package pub.hackers.android.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
import pub.hackers.android.data.paging.cursorPager
import pub.hackers.android.data.paging.distinctByEffectiveId
import pub.hackers.android.data.paging.localTimelinePage
import pub.hackers.android.data.paging.publicTimelinePage
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.ui.bookmark.BookmarkMutationCoordinator
import javax.inject.Inject

enum class ExploreTab {
    LOCAL, GLOBAL
}

data class ExploreUiState(
    val reactionPickerPostId: String? = null,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: HackersPubRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ExploreTab.LOCAL)
    val selectedTab: StateFlow<ExploreTab> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val overlayStore = PostOverlayStore()
    private val bookmarkCoordinator = BookmarkMutationCoordinator(
        scope = viewModelScope,
        requestMutation = { postId, shouldBookmark ->
            if (shouldBookmark) repository.bookmarkPost(postId)
            else repository.unbookmarkPost(postId)
        },
        applyDesiredState = { postId, isBookmarked ->
            overlayStore.mutate(postId) {
                it.copy(viewerHasBookmarked = isBookmarked)
            }
        },
        revertFailedState = { postId, attemptedState ->
            overlayStore.mutate(postId) {
                it.copy(viewerHasBookmarked = !attemptedState)
            }
        },
    )

    val posts: Flow<PagingData<Post>> = _selectedTab
        .flatMapLatest { tab ->
            cursorPager { after ->
                when (tab) {
                    ExploreTab.LOCAL -> repository.localTimelinePage(after)
                    ExploreTab.GLOBAL -> repository.publicTimelinePage(after)
                }
            }.flow.distinctByEffectiveId().cachedIn(viewModelScope)
        }
        .combine(overlayStore.overlays) { paging, overlays ->
            paging.map { post -> post.applyOverlays(overlays) }
        }
        .cachedIn(viewModelScope)

    fun selectTab(tab: ExploreTab) {
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
        bookmarkCoordinator.toggle(target.id, target.viewerHasBookmarked)
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
                    emoji = emoji, customEmoji = null,
                    count = 1, reactors = emptyList(), viewerHasReacted = true,
                )
            }
        }
    }
}
