package pub.hackers.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.paging.CursorPage
import pub.hackers.android.data.paging.PostOverlayStore
import pub.hackers.android.data.paging.actorArticlesPage
import pub.hackers.android.data.paging.actorNotesPage
import pub.hackers.android.data.paging.actorPostsPage
import pub.hackers.android.data.paging.applyOverlays
import pub.hackers.android.data.paging.cursorPager
import pub.hackers.android.data.paging.distinctByEffectiveId
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.AccountLink
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.ActorField
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import pub.hackers.android.ui.bookmark.BookmarkMutationCoordinator
import javax.inject.Inject

enum class ProfileTab {
    POSTS, NOTES, ARTICLES
}

data class ProfileUiState(
    val actor: Actor? = null,
    val bio: String? = null,
    val fields: List<ActorField> = emptyList(),
    val accountLinks: List<AccountLink> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isViewer: Boolean = false,
    val viewerFollows: Boolean = false,
    val followsViewer: Boolean = false,
    val viewerBlocks: Boolean = false,
    val isPerformingAction: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val handle: String = checkNotNull(savedStateHandle["handle"])

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(ProfileTab.POSTS)
    val selectedTab: StateFlow<ProfileTab> = _selectedTab.asStateFlow()

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
            overlayStore.mutate(postId) { prev ->
                prev.copy(viewerHasBookmarked = !attemptedState)
            }
        },
    )

    // Each tab has its own pager. Screen side only collects the active tab's
    // flow, so inactive tabs don't fetch until the user switches to them.
    // The shared overlayStore means optimistic mutations (share, reaction)
    // propagate across all three tabs automatically.
    val postsTab: Flow<PagingData<Post>> = overlaidPostsFlow { after ->
        repository.actorPostsPage(handle, after)
    }

    val notesTab: Flow<PagingData<Post>> = overlaidPostsFlow { after ->
        repository.actorNotesPage(handle, after)
    }

    val articlesTab: Flow<PagingData<Post>> = overlaidPostsFlow { after ->
        repository.actorArticlesPage(handle, after)
    }

    private fun overlaidPostsFlow(
        fetch: suspend (after: String?) -> Result<CursorPage<Post>>,
    ): Flow<PagingData<Post>> = combine(
        cursorPager { after -> fetch(after) }
            .flow
            .distinctByEffectiveId()
            .cachedIn(viewModelScope),
        overlayStore.overlays,
    ) { paging, overlays ->
        paging.map { post -> post.applyOverlays(overlays) }
    }.cachedIn(viewModelScope)

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getProfile(handle)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            actor = profile.actor,
                            bio = profile.bio,
                            fields = profile.fields,
                            accountLinks = profile.accountLinks,
                            isViewer = profile.isViewer,
                            viewerFollows = profile.viewerFollows,
                            followsViewer = profile.followsViewer,
                            viewerBlocks = profile.viewerBlocks,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            repository.getProfile(handle, refresh = true)
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            actor = profile.actor,
                            bio = profile.bio,
                            fields = profile.fields,
                            accountLinks = profile.accountLinks,
                            isViewer = profile.isViewer,
                            viewerFollows = profile.viewerFollows,
                            followsViewer = profile.followsViewer,
                            viewerBlocks = profile.viewerBlocks,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _selectedTab.value = tab
    }

    fun followActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return
        _uiState.update { it.copy(viewerFollows = true, isPerformingAction = true, actionError = null) }
        viewModelScope.launch {
            repository.followActor(actorId)
                .onSuccess {
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(viewerFollows = false, isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun unfollowActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return
        _uiState.update { it.copy(viewerFollows = false, isPerformingAction = true, actionError = null) }
        viewModelScope.launch {
            repository.unfollowActor(actorId)
                .onSuccess {
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(viewerFollows = true, isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }
    fun blockActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return
        _uiState.update { it.copy(viewerBlocks = true, isPerformingAction = true, actionError = null) }
        viewModelScope.launch {
            repository.blockActor(actorId)
                .onSuccess {
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(viewerBlocks = false, isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun unblockActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return
        _uiState.update { it.copy(viewerBlocks = false, isPerformingAction = true, actionError = null) }
        viewModelScope.launch {
            repository.unblockActor(actorId)
                .onSuccess {
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(viewerBlocks = true, isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun removeFollower() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return
        _uiState.update { it.copy(followsViewer = false, isPerformingAction = true, actionError = null) }
        viewModelScope.launch {
            repository.removeFollower(actorId)
                .onSuccess {
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(followsViewer = true, isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun dismissActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun sharePost(postId: String) {
        overlayStore.mutate(postId) {
            it.copy(viewerHasShared = true, shareDelta = it.shareDelta + 1)
        }
        viewModelScope.launch {
            repository.sharePost(postId).onFailure {
                overlayStore.mutate(postId) { prev ->
                    prev.copy(viewerHasShared = false, shareDelta = prev.shareDelta - 1)
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
                    prev.copy(viewerHasShared = true, shareDelta = prev.shareDelta + 1)
                }
            }
        }
    }

    fun toggleBookmark(post: Post) {
        val target = post.sharedPost ?: post
        bookmarkCoordinator.toggle(target.id, target.viewerHasBookmarked)
    }

    @Suppress("unused")
    fun toggleReaction(post: Post, emoji: String) {
        val target = post.sharedPost ?: post
        val existing = target.reactionGroups.find { it.emoji == emoji }
        val wasReacted = existing?.viewerHasReacted == true

        val updatedGroups = if (wasReacted) {
            target.reactionGroups.map { g ->
                if (g.emoji == emoji) g.copy(
                    count = maxOf(0, g.count - 1),
                    viewerHasReacted = false,
                )
                else g
            }.filter { it.count > 0 || it.viewerHasReacted }
        } else if (existing != null) {
            target.reactionGroups.map { g ->
                if (g.emoji == emoji) g.copy(count = g.count + 1, viewerHasReacted = true)
                else g
            }
        } else {
            target.reactionGroups + ReactionGroup(
                emoji = emoji,
                customEmoji = null,
                count = 1,
                reactors = emptyList(),
                viewerHasReacted = true,
            )
        }

        overlayStore.mutate(target.id) { prev ->
            prev.copy(
                reactionOverride = updatedGroups,
                reactionCountOverride = updatedGroups.sumOf { it.count },
            )
        }

        viewModelScope.launch {
            val result = if (wasReacted) {
                repository.removeReactionFromPost(target.id, emoji)
            } else {
                repository.addReactionToPost(target.id, emoji)
            }
            result.onFailure { overlayStore.clear(target.id) }
        }
    }
}
