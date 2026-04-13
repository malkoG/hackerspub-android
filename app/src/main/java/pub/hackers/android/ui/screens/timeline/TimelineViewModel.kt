package pub.hackers.android.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.ReactionGroup
import java.time.Instant
import javax.inject.Inject

data class TimelineUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null,
    val reactionPickerPostId: String? = null,
    val draftCount: Int = 0
)

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private var lastLoadTime: Instant? = null
    private val staleThresholdSeconds = 60L

    init {
        loadTimeline()
        loadDraftCount()
    }

    fun loadDraftCount() {
        viewModelScope.launch {
            repository.getArticleDrafts()
                .onSuccess { drafts ->
                    _uiState.update { it.copy(draftCount = drafts.size) }
                }
        }
    }

    fun refreshIfStale() {
        val last = lastLoadTime ?: return
        if (Instant.now().epochSecond - last.epochSecond > staleThresholdSeconds) {
            refresh()
        }
    }

    private fun loadTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getPersonalTimeline(refresh = true)
                .onSuccess { result ->
                    lastLoadTime = Instant.now()
                    _uiState.update {
                        it.copy(
                            posts = result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
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

            repository.getPersonalTimeline(refresh = true)
                .onSuccess { result ->
                    lastLoadTime = Instant.now()
                    _uiState.update {
                        it.copy(
                            posts = result.posts,
                            hasNextPage = result.hasNextPage,
                            endCursor = result.endCursor,
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

            repository.getPersonalTimeline(after = currentState.endCursor)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            posts = (it.posts + result.posts).distinctBy { p -> p.id },
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

    fun sharePost(postId: String) {
        // Optimistic update
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
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
        viewModelScope.launch {
            repository.sharePost(postId)
                .onFailure {
                    // Revert on failure
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == postId) {
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

    fun unsharePost(postId: String) {
        // Optimistic update
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
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
        viewModelScope.launch {
            repository.unsharePost(postId)
                .onFailure {
                    // Revert on failure
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == postId) {
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

    fun toggleFavourite(postId: String) {
        toggleReaction(postId, "❤️")
    }

    fun toggleReaction(postId: String, emoji: String) {
        val post = _uiState.value.posts.find {
            it.id == postId || it.sharedPost?.id == postId
        } ?: return
        val targetPost = post.sharedPost ?: post
        val existingGroup = targetPost.reactionGroups.find { it.emoji == emoji }
        val viewerHasReacted = existingGroup?.viewerHasReacted == true

        // Optimistic update
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { p ->
                    val target = p.sharedPost ?: p
                    if (target.id == targetPost.id) {
                        val updatedGroups = updateReactionGroups(
                            target.reactionGroups, emoji, viewerHasReacted
                        )
                        val totalReactions = updatedGroups.sumOf { it.count }
                        val updatedTarget = target.copy(
                            reactionGroups = updatedGroups,
                            engagementStats = target.engagementStats.copy(
                                reactions = totalReactions
                            )
                        )
                        if (p.sharedPost != null) p.copy(sharedPost = updatedTarget)
                        else updatedTarget
                    } else p
                },
                reactionPickerPostId = null
            )
        }

        viewModelScope.launch {
            val result = if (viewerHasReacted) {
                repository.removeReactionFromPost(targetPost.id, emoji)
            } else {
                repository.addReactionToPost(targetPost.id, emoji)
            }

            result.onFailure {
                // Revert on failure
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.map { p ->
                            val target = p.sharedPost ?: p
                            if (target.id == targetPost.id) {
                                val revertedGroups = updateReactionGroups(
                                    target.reactionGroups, emoji, !viewerHasReacted
                                )
                                val totalReactions = revertedGroups.sumOf { it.count }
                                val revertedTarget = target.copy(
                                    reactionGroups = revertedGroups,
                                    engagementStats = target.engagementStats.copy(
                                        reactions = totalReactions
                                    )
                                )
                                if (p.sharedPost != null) p.copy(sharedPost = revertedTarget)
                                else revertedTarget
                            } else p
                        }
                    )
                }
            }
        }
    }

    fun showReactionPicker(postId: String) {
        _uiState.update { it.copy(reactionPickerPostId = postId) }
    }

    fun hideReactionPicker() {
        _uiState.update { it.copy(reactionPickerPostId = null) }
    }

    private fun updateReactionGroups(
        groups: List<ReactionGroup>,
        emoji: String,
        wasReacted: Boolean
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
                    count = 1, reactors = emptyList(), viewerHasReacted = true
                )
            }
        }
    }
}
