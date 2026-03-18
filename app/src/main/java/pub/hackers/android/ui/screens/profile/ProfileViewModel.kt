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
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.Post
import javax.inject.Inject

data class ProfileUiState(
    val actor: Actor? = null,
    val bio: String? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null,
    val isViewer: Boolean = false,
    val viewerFollows: Boolean = false,
    val followsViewer: Boolean = false,
    val viewerBlocks: Boolean = false,
    val isPerformingAction: Boolean = false,
    val actionError: String? = null
)

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
                        it.copy(
                            actor = result.actor,
                            bio = result.bio,
                            posts = result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
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
                        it.copy(
                            actor = result.actor,
                            bio = result.bio,
                            posts = result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isViewer = result.isViewer,
                            viewerFollows = result.viewerFollows,
                            followsViewer = result.followsViewer,
                            viewerBlocks = result.viewerBlocks,
                            isRefreshing = false
                        )
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

    fun loadMore() {
        val currentState = _uiState.value
        if (!currentState.hasNextPage || currentState.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            repository.getProfile(handle, postsAfter = currentState.endCursor)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            posts = it.posts + result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
                            isLoadingMore = false
                        )
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

    fun sharePost(postId: String) {
        viewModelScope.launch {
            repository.sharePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == postId || post.sharedPost?.id == postId) {
                                    post.copy(
                                        viewerHasShared = true,
                                        engagementStats = post.engagementStats.copy(
                                            shares = post.engagementStats.shares + 1
                                        )
                                    )
                                } else post
                            }
                        )
                    }
                }
        }
    }

    fun unsharePost(postId: String) {
        viewModelScope.launch {
            repository.unsharePost(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == postId || post.sharedPost?.id == postId) {
                                    post.copy(
                                        viewerHasShared = false,
                                        engagementStats = post.engagementStats.copy(
                                            shares = maxOf(0, post.engagementStats.shares - 1)
                                        )
                                    )
                                } else post
                            }
                        )
                    }
                }
        }
    }
}
