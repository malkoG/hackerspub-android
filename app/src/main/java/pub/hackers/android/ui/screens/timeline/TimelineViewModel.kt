package pub.hackers.android.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.Pager
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.paging.CursorPagingSource
import pub.hackers.android.data.paging.PostOverlayStore
import pub.hackers.android.data.paging.applyOverlays
import pub.hackers.android.data.paging.distinctByEffectiveId
import pub.hackers.android.data.paging.personalTimelinePage
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import javax.inject.Inject

data class TimelineUiState(
    val error: String? = null,
    val reactionPickerPostId: String? = null,
    val draftCount: Int = 0,
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    val preferencesManager: PreferencesManager,
    val refreshTrigger: TimelineRefreshTrigger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val overlayStore = PostOverlayStore()

    private var currentPagingSource: PagingSource<String, Post>? = null

    val posts: Flow<PagingData<Post>> = combine(
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
                initialLoadSize = 20,
            ),
            pagingSourceFactory = {
                CursorPagingSource<Post> { after -> repository.personalTimelinePage(after) }
                    .also { currentPagingSource = it }
            },
        ).flow
            .distinctByEffectiveId()
            .cachedIn(viewModelScope),
        overlayStore.overlays,
    ) { paging, overlays ->
        paging.map { post -> post.applyOverlays(overlays) }
    }.cachedIn(viewModelScope)

    init {
        loadDraftCount()
        viewModelScope.launch {
            refreshTrigger.refreshAt.drop(1).collect {
                currentPagingSource?.invalidate()
            }
        }
    }

    fun loadDraftCount() {
        viewModelScope.launch {
            repository.getArticleDrafts()
                .onSuccess { drafts ->
                    _uiState.update { it.copy(draftCount = drafts.size) }
                }
        }
    }

    fun sharePost(postId: String) {
        overlayStore.mutate(postId) {
            it.copy(viewerHasShared = true, shareDelta = it.shareDelta + 1)
        }
        viewModelScope.launch {
            repository.sharePost(postId).onFailure {
                overlayStore.mutate(postId) { prev ->
                    prev.copy(
                        viewerHasShared = false,
                        shareDelta = prev.shareDelta - 1,
                    )
                }
            }
        }
    }

    fun unsharePost(postId: String) {
        overlayStore.mutate(postId) {
            it.copy(viewerHasShared = false, shareDelta = it.shareDelta - 1)
        }
        viewModelScope.launch {
            repository.unsharePost(postId).onFailure {
                overlayStore.mutate(postId) { prev ->
                    prev.copy(
                        viewerHasShared = true,
                        shareDelta = prev.shareDelta + 1,
                    )
                }
            }
        }
    }

    fun toggleFavourite(post: Post) {
        toggleReaction(post, "❤️")
    }

    fun toggleBookmark(post: Post) {
        val target = post.sharedPost ?: post
        val willBookmark = !target.viewerHasBookmarked

        overlayStore.mutate(target.id) {
            it.copy(viewerHasBookmarked = willBookmark)
        }

        viewModelScope.launch {
            val result = if (willBookmark) {
                repository.bookmarkPost(target.id)
            } else {
                repository.unbookmarkPost(target.id)
            }
            result.onFailure {
                overlayStore.mutate(target.id) { prev ->
                    prev.copy(viewerHasBookmarked = !willBookmark)
                }
            }
        }
    }

    /**
     * Optimistically toggle a reaction on [post] (or its sharedPost target).
     * The overlay is computed from the post's current reactionGroups (possibly
     * already overlaid) and persisted in [overlayStore] until the server
     * confirms. On failure, the overlay for that post is cleared so the
     * server-authoritative state wins on the next emission.
     */
    fun toggleReaction(post: Post, emoji: String) {
        val target = post.sharedPost ?: post
        val existing = target.reactionGroups.find { it.emoji == emoji }
        val wasReacted = existing?.viewerHasReacted == true

        val updatedGroups = computeToggledReactionGroups(target.reactionGroups, emoji, wasReacted)

        overlayStore.mutate(target.id) { prev ->
            prev.copy(
                reactionOverride = updatedGroups,
                reactionCountOverride = updatedGroups.sumOf { it.count },
            )
        }
        _uiState.update { it.copy(reactionPickerPostId = null) }

        viewModelScope.launch {
            val result = if (wasReacted) {
                repository.removeReactionFromPost(target.id, emoji)
            } else {
                repository.addReactionToPost(target.id, emoji)
            }
            result.onFailure {
                // Revert by clearing overlay; server-fetched post data will win
                overlayStore.clear(target.id)
            }
        }
    }

    fun showReactionPicker(postId: String) {
        _uiState.update { it.copy(reactionPickerPostId = postId) }
    }

    fun hideReactionPicker() {
        _uiState.update { it.copy(reactionPickerPostId = null) }
    }

    private fun computeToggledReactionGroups(
        groups: List<ReactionGroup>,
        emoji: String,
        wasReacted: Boolean,
    ): List<ReactionGroup> = if (wasReacted) {
        groups.map { g ->
            if (g.emoji == emoji) g.copy(count = maxOf(0, g.count - 1), viewerHasReacted = false)
            else g
        }.filter { it.count > 0 || it.viewerHasReacted }
    } else {
        val existing = groups.find { it.emoji == emoji }
        if (existing != null) {
            groups.map { g ->
                if (g.emoji == emoji) g.copy(count = g.count + 1, viewerHasReacted = true)
                else g
            }
        } else {
            groups + ReactionGroup(
                emoji = emoji, customEmoji = null,
                count = 1, reactors = emptyList(), viewerHasReacted = true,
            )
        }
    }
}
