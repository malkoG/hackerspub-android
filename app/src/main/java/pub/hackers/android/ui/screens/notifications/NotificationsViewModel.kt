package pub.hackers.android.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Notification
import java.time.Instant
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    private val notificationStateManager: NotificationStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var lastLoadTime: Instant? = null
    private val staleThresholdSeconds = 60L

    init {
        loadNotifications()
    }

    fun refreshIfStale() {
        val last = lastLoadTime ?: return
        if (Instant.now().epochSecond - last.epochSecond > staleThresholdSeconds) {
            refresh()
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getNotifications()
            result
                .onSuccess { notificationsResult ->
                    lastLoadTime = Instant.now()
                    _uiState.update {
                        it.copy(
                            notifications = notificationsResult.notifications,
                            hasNextPage = notificationsResult.hasNextPage,
                            endCursor = notificationsResult.endCursor,
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
            if (result.isSuccess) {
                notificationStateManager.markAsSeen()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            val result = repository.getNotifications(refresh = true)
            result
                .onSuccess { notificationsResult ->
                    lastLoadTime = Instant.now()
                    _uiState.update {
                        it.copy(
                            notifications = notificationsResult.notifications,
                            hasNextPage = notificationsResult.hasNextPage,
                            endCursor = notificationsResult.endCursor,
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
            if (result.isSuccess) {
                notificationStateManager.markAsSeen()
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (!currentState.hasNextPage || currentState.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            repository.getNotifications(after = currentState.endCursor)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications + result.notifications,
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
}
