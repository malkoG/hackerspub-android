package pub.hackers.android.data.paging

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import androidx.paging.filter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup

/**
 * Transient client-side state layered on top of server-fetched posts.
 *
 * When the user performs an optimistic action (share, reaction), we record
 * the delta here rather than mutating the [Post] stored in [PagingData].
 * The overlay is merged into items at collection time via [Post.applyOverlay].
 * This avoids invalidating the [PagingSource] on every mutation.
 */
@Immutable
data class PostOverlay(
    val viewerHasShared: Boolean? = null,      // null = no override
    val shareDelta: Int = 0,                    // added to engagementStats.shares
    val reactionOverride: List<ReactionGroup>? = null, // full replacement when we touched reactions
    val reactionCountOverride: Int? = null,     // engagementStats.reactions override
)

/**
 * Apply [overlay] to this [Post]. Returns the same instance when overlay is null.
 */
fun Post.applyOverlay(overlay: PostOverlay?): Post {
    if (overlay == null) return this
    return copy(
        viewerHasShared = overlay.viewerHasShared ?: viewerHasShared,
        engagementStats = engagementStats.copy(
            shares = maxOf(0, engagementStats.shares + overlay.shareDelta),
            reactions = overlay.reactionCountOverride ?: engagementStats.reactions,
        ),
        reactionGroups = overlay.reactionOverride ?: reactionGroups,
    )
}

/**
 * Apply direct and shared-post overlays to a [Post] in one pass.
 * Used inside [PagingData.map] transformers so both a top-level post and its
 * shared/reposted inner post can be optimistically updated.
 */
fun Post.applyOverlays(overlays: Map<String, PostOverlay>): Post {
    val direct = overlays[id]
    val shared = sharedPost?.id?.let(overlays::get)
    val afterDirect = applyOverlay(direct)
    if (shared == null || afterDirect.sharedPost == null) return afterDirect
    return afterDirect.copy(sharedPost = afterDirect.sharedPost.applyOverlay(shared))
}

/**
 * Thread-safe, ViewModel-scoped store for [PostOverlay] values keyed by post id.
 * Exposes a [StateFlow] so it can be combined with a [PagingData] flow.
 */
class PostOverlayStore {
    private val _overlays = MutableStateFlow<Map<String, PostOverlay>>(emptyMap())
    val overlays: StateFlow<Map<String, PostOverlay>> = _overlays

    fun mutate(postId: String, block: (PostOverlay) -> PostOverlay) {
        _overlays.update { current ->
            current + (postId to block(current[postId] ?: PostOverlay()))
        }
    }

    fun clear(postId: String) {
        _overlays.update { it - postId }
    }
}

/**
 * Effective-id dedup: drop duplicates across pages keyed by displayed post id
 * (`sharedPost.id` for reposts, else `id`).
 *
 * Fixes two classes of bugs exposed after moving to Paging 3:
 * 1. LazyColumn duplicate-key crash when the server returns the same outer
 *    post id in multiple pages.
 * 2. Same-content-twice rendering when the server returns both an original
 *    post and a repost wrapping it in the same feed.
 *
 * This layers *on top of* the repository's outer-id dedup
 * (`.distinctBy { it.id }` added in PR #83) — the repository filter catches
 * duplicate outer ids at the response boundary; this filter catches repost
 * pairs (different outer ids, same sharedPost.id) at the paging boundary.
 *
 * A fresh [HashSet] is allocated per new [PagingData] emission, so refresh
 * resets the seen set correctly.
 */
fun Flow<PagingData<Post>>.distinctByEffectiveId(): Flow<PagingData<Post>> = map { pagingData ->
    val seen = HashSet<String>()
    pagingData.filter { post ->
        seen.add(post.sharedPost?.id ?: post.id)
    }
}
