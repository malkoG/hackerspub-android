package pub.hackers.android.data.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import pub.hackers.android.data.repository.HackersPubRepository

/**
 * Shape common to every cursor-based paginated endpoint in this codebase.
 * Repository adapter functions below translate result types into this.
 */
data class CursorPage<T>(
    val items: List<T>,
    val endCursor: String?,
    val hasNextPage: Boolean,
)

/**
 * Generic, repository-agnostic PagingSource for cursor-based APIs.
 * Callers provide a `fetch` lambda that takes a cursor and returns a page.
 */
class CursorPagingSource<T : Any>(
    private val fetch: suspend (after: String?) -> Result<CursorPage<T>>,
) : PagingSource<String, T>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        val after = params.key // null on first page
        return fetch(after).fold(
            onSuccess = { page ->
                LoadResult.Page(
                    data = page.items,
                    prevKey = null, // forward-only feed
                    nextKey = if (page.hasNextPage) page.endCursor else null,
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    // Forward-only feeds always refresh from head; we never seek into the middle.
    override fun getRefreshKey(state: PagingState<String, T>): String? = null
}

/**
 * Convenience factory — wraps [CursorPagingSource] with our standard [PagingConfig].
 *
 * Defaults:
 * - `pageSize = 20` matches the server's GraphQL `first: 20` (we can't request
 *   more per page regardless, so this is the practical ceiling).
 * - `prefetchDistance = 5` triggers the next page fetch only once the user is
 *   within ~half a viewport of the loaded window's end. The previous value of
 *   20 (one full page ahead) caused cascading prefetches right after the first
 *   render: with only 20 items loaded and `distance-from-end < prefetchDistance`
 *   satisfied after any small scroll, page 2, 3, 4… fired back-to-back.
 * - `initialLoadSize = 20` equals `pageSize` and equals the server's fixed
 *   first-page size, so Paging's "desired vs delivered" check doesn't
 *   immediately schedule an append. (The previous 30 was wishful — server
 *   only returns 20, and asking for 30 made Paging think the window was
 *   under-filled on first load.)
 * - `enablePlaceholders = false` — existing UI has no skeleton support.
 */
fun <T : Any> cursorPager(
    pageSize: Int = 20,
    prefetchDistance: Int = 5,
    initialLoadSize: Int = 20,
    fetch: suspend (String?) -> Result<CursorPage<T>>,
): Pager<String, T> = Pager(
    config = PagingConfig(
        pageSize = pageSize,
        prefetchDistance = prefetchDistance,
        enablePlaceholders = false,
        initialLoadSize = initialLoadSize,
    ),
    pagingSourceFactory = { CursorPagingSource(fetch) },
)

// region Repository adapters
// Each paginated endpoint gets a one-liner converter so ViewModels can call
// `cursorPager { repository.fooPage(it) }` without repeating the boilerplate.
// Note: the repository's own `.distinctBy { it.id }` (outer-id) is preserved
// as a defensive dedup; see PR #83. The paging layer adds an extra pass via
// `Flow<PagingData<Post>>.distinctByEffectiveId()` in PostOverlay.kt that
// keys on the displayed post id (sharedPost.id ?: id) to handle repost pairs.

suspend fun HackersPubRepository.notificationsPage(after: String?) =
    getNotifications(after = after, refresh = (after == null))
        .map { CursorPage(it.notifications, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.personalTimelinePage(after: String?) =
    getPersonalTimeline(after = after, refresh = (after == null))
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.publicTimelinePage(after: String?) =
    getPublicTimeline(after = after, refresh = (after == null))
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.localTimelinePage(after: String?) =
    getLocalTimeline(after = after, refresh = (after == null))
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.postRepliesPage(postId: String, after: String?) =
    getPostReplies(postId, after)
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.actorPostsPage(handle: String, after: String?) =
    getActorPosts(handle, after)
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.actorNotesPage(handle: String, after: String?) =
    getActorNotes(handle, after)
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.actorArticlesPage(handle: String, after: String?) =
    getActorArticles(handle, after)
        .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

suspend fun HackersPubRepository.bookmarksPage(
    after: String?,
    postType: pub.hackers.android.graphql.type.PostType?,
) = getBookmarks(after, postType)
    .map { CursorPage(it.posts, it.endCursor, it.hasNextPage) }

// endregion
