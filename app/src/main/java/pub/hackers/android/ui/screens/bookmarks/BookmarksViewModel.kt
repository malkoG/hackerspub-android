package pub.hackers.android.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.paging.PostOverlayStore
import pub.hackers.android.data.paging.applyOverlays
import pub.hackers.android.data.paging.bookmarksPage
import pub.hackers.android.data.paging.cursorPager
import pub.hackers.android.data.paging.distinctByEffectiveId
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.graphql.type.PostType as GqlPostType
import javax.inject.Inject

enum class BookmarkTab {
    ALL, ARTICLES, NOTES
}

data class BookmarksUiState(
    val reactionPickerPostId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: HackersPubRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(BookmarkTab.ALL)
    val selectedTab: StateFlow<BookmarkTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    private val overlayStore = PostOverlayStore()

    val posts: Flow<PagingData<Post>> = _selectedTab
        .flatMapLatest { tab ->
            cursorPager { after ->
                repository.bookmarksPage(after, tab.toGraphqlPostType())
            }.flow.distinctByEffectiveId().cachedIn(viewModelScope)
        }
        .combine(overlayStore.overlays) { paging, overlays ->
            paging
                .map { post -> post.applyOverlays(overlays) }
                .filter { post -> (post.sharedPost ?: post).viewerHasBookmarked }
        }
        .cachedIn(viewModelScope)

    fun selectTab(tab: BookmarkTab) {
        if (_selectedTab.value != tab) _selectedTab.value = tab
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
    ): List<ReactionGroup> {
        return if (wasReacted) {
            groups.map { group ->
                if (group.emoji == emoji) {
                    group.copy(count = maxOf(0, group.count - 1), viewerHasReacted = false)
                } else group
            }.filter { it.count > 0 || it.viewerHasReacted }
        } else {
            val existing = groups.find { it.emoji == emoji }
            if (existing != null) {
                groups.map { group ->
                    if (group.emoji == emoji) {
                        group.copy(count = group.count + 1, viewerHasReacted = true)
                    } else group
                }
            } else {
                groups + ReactionGroup(
                    emoji = emoji,
                    customEmoji = null,
                    count = 1,
                    reactors = emptyList(),
                    viewerHasReacted = true,
                )
            }
        }
    }

    private fun BookmarkTab.toGraphqlPostType(): GqlPostType? = when (this) {
        BookmarkTab.ALL -> null
        BookmarkTab.ARTICLES -> GqlPostType.ARTICLE
        BookmarkTab.NOTES -> GqlPostType.NOTE
    }
}
