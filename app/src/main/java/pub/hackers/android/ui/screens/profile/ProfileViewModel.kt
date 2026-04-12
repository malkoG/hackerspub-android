package pub.hackers.android.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.AccountLink
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.ActorField
import pub.hackers.android.domain.model.Post
import javax.inject.Inject

enum class ProfileTab {
    POSTS, NOTES, ARTICLES
}

data class TabState(
    val posts: List<Post> = emptyList(),
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val isLoaded: Boolean = false
)

data class ProfileUiState(
    val actor: Actor? = null,
    val bio: String? = null,
    val fields: List<ActorField> = emptyList(),
    val accountLinks: List<AccountLink> = emptyList(),
    val selectedTab: ProfileTab = ProfileTab.POSTS,
    val tabStates: Map<ProfileTab, TabState> = ProfileTab.entries.associateWith { TabState() },
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val isViewer: Boolean = false,
    val viewerFollows: Boolean = false,
    val followsViewer: Boolean = false,
    val viewerBlocks: Boolean = false,
    val isPerformingAction: Boolean = false,
    val actionError: String? = null
) {
    val currentTabState: TabState get() = tabStates[selectedTab] ?: TabState()
    val posts: List<Post> get() = currentTabState.posts
    val hasNextPage: Boolean get() = currentTabState.hasNextPage
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val handle: String = checkNotNull(savedStateHandle["handle"])

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile(handle)
    }

    fun loadProfile(handle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getProfile(handle)
                .onSuccess { result ->
                    _uiState.update {
                        val newTabStates = it.tabStates.toMutableMap()
                        newTabStates[ProfileTab.POSTS] = TabState(
                            posts = result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isLoaded = true
                        )
                        it.copy(
                            actor = result.actor,
                            bio = result.bio,
                            fields = result.fields,
                            accountLinks = result.accountLinks,
                            tabStates = newTabStates,
                            isViewer = result.isViewer,
                            viewerFollows = result.viewerFollows,
                            followsViewer = result.followsViewer,
                            viewerBlocks = result.viewerBlocks,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            repository.getProfile(handle)
                .onSuccess { result ->
                    _uiState.update {
                        // Reset all tab states on refresh
                        val newTabStates = ProfileTab.entries.associateWith { TabState() }.toMutableMap()
                        newTabStates[ProfileTab.POSTS] = TabState(
                            posts = result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isLoaded = true
                        )
                        it.copy(
                            actor = result.actor,
                            bio = result.bio,
                            fields = result.fields,
                            accountLinks = result.accountLinks,
                            tabStates = newTabStates,
                            isViewer = result.isViewer,
                            viewerFollows = result.viewerFollows,
                            followsViewer = result.followsViewer,
                            viewerBlocks = result.viewerBlocks,
                            isRefreshing = false
                        )
                    }
                    // Reload current tab if not POSTS
                    val currentTab = _uiState.value.selectedTab
                    if (currentTab != ProfileTab.POSTS) {
                        loadTabData(currentTab)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        val tabState = _uiState.value.tabStates[tab]
        if (tabState == null || !tabState.isLoaded) {
            loadTabData(tab)
        }
    }

    private fun loadTabData(tab: ProfileTab) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = when (tab) {
                ProfileTab.POSTS -> repository.getProfile(handle).map {
                    Triple(it.posts, it.hasNextPage, it.endCursor)
                }
                ProfileTab.NOTES -> repository.getActorNotes(handle).map {
                    Triple(it.posts, it.hasNextPage, it.endCursor)
                }
                ProfileTab.ARTICLES -> repository.getActorArticles(handle).map {
                    Triple(it.posts, it.hasNextPage, it.endCursor)
                }
            }

            result
                .onSuccess { (posts, hasNextPage, endCursor) ->
                    _uiState.update {
                        val newTabStates = it.tabStates.toMutableMap()
                        newTabStates[tab] = TabState(
                            posts = posts,
                            hasNextPage = hasNextPage,
                            endCursor = endCursor,
                            isLoaded = true
                        )
                        it.copy(tabStates = newTabStates, isLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        val tabState = currentState.currentTabState
        if (!tabState.hasNextPage || currentState.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val tab = currentState.selectedTab
            val result = when (tab) {
                ProfileTab.POSTS -> repository.getProfile(handle, postsAfter = tabState.endCursor).map {
                    Triple(it.posts, it.hasNextPage, it.endCursor)
                }
                ProfileTab.NOTES -> repository.getActorNotes(handle, after = tabState.endCursor).map {
                    Triple(it.posts, it.hasNextPage, it.endCursor)
                }
                ProfileTab.ARTICLES -> repository.getActorArticles(handle, after = tabState.endCursor).map {
                    Triple(it.posts, it.hasNextPage, it.endCursor)
                }
            }

            result
                .onSuccess { (posts, hasNextPage, endCursor) ->
                    _uiState.update {
                        val newTabStates = it.tabStates.toMutableMap()
                        val existing = newTabStates[tab] ?: TabState()
                        newTabStates[tab] = existing.copy(
                            posts = existing.posts + posts,
                            hasNextPage = hasNextPage,
                            endCursor = endCursor
                        )
                        it.copy(tabStates = newTabStates, isLoadingMore = false)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun followActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPerformingAction = true, actionError = null) }

            repository.followActor(actorId)
                .onSuccess {
                    refresh()
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun unfollowActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPerformingAction = true, actionError = null) }

            repository.unfollowActor(actorId)
                .onSuccess {
                    refresh()
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun blockActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPerformingAction = true, actionError = null) }

            repository.blockActor(actorId)
                .onSuccess {
                    refresh()
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun unblockActor() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPerformingAction = true, actionError = null) }

            repository.unblockActor(actorId)
                .onSuccess {
                    refresh()
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun removeFollower() {
        val actorId = _uiState.value.actor?.id ?: return
        if (_uiState.value.isPerformingAction) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPerformingAction = true, actionError = null) }

            repository.removeFollower(actorId)
                .onSuccess {
                    refresh()
                    _uiState.update { it.copy(isPerformingAction = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isPerformingAction = false, actionError = error.message)
                    }
                }
        }
    }

    fun dismissActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    private fun updatePostInAllTabs(postId: String, transform: (Post) -> Post) {
        _uiState.update { state ->
            val newTabStates = state.tabStates.mapValues { (_, tabState) ->
                tabState.copy(
                    posts = tabState.posts.map { post ->
                        if (post.id == postId || post.sharedPost?.id == postId) {
                            transform(post)
                        } else post
                    }
                )
            }
            state.copy(tabStates = newTabStates)
        }
    }

    fun sharePost(postId: String) {
        viewModelScope.launch {
            repository.sharePost(postId)
                .onSuccess {
                    updatePostInAllTabs(postId) { post ->
                        post.copy(
                            viewerHasShared = true,
                            engagementStats = post.engagementStats.copy(
                                shares = post.engagementStats.shares + 1
                            )
                        )
                    }
                }
        }
    }

    fun unsharePost(postId: String) {
        viewModelScope.launch {
            repository.unsharePost(postId)
                .onSuccess {
                    updatePostInAllTabs(postId) { post ->
                        post.copy(
                            viewerHasShared = false,
                            engagementStats = post.engagementStats.copy(
                                shares = maxOf(0, post.engagementStats.shares - 1)
                            )
                        )
                    }
                }
        }
    }
}
