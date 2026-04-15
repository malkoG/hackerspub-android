package pub.hackers.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.data.worker.NotificationWorker
import pub.hackers.android.ui.screens.timeline.TimelineRefreshTrigger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    val preferencesManager: PreferencesManager,
    private val notificationStateManager: NotificationStateManager,
    private val workManager: WorkManager,
    private val repository: HackersPubRepository,
    val timelineRefreshTrigger: TimelineRefreshTrigger,
) : ViewModel() {

    companion object {
        const val NOTIFICATION_WORK_NAME = "notification_poll"
        const val FOREGROUND_POLL_INTERVAL_MS = 60_000L // 1 minute
    }

    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    val hasUnread: StateFlow<Boolean> = notificationStateManager.hasUnread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var foregroundPollJob: Job? = null

    init {
        ensureNotificationPolling()
    }

    private fun ensureNotificationPolling() {
        viewModelScope.launch {
            val loggedIn = sessionManager.isLoggedIn.first()
            if (loggedIn) {
                enqueueNotificationPolling()
                startForegroundPolling()
            }
        }
    }

    fun enqueueNotificationPolling() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun startForegroundPolling() {
        if (foregroundPollJob?.isActive == true) return
        foregroundPollJob = viewModelScope.launch {
            while (isActive) {
                delay(FOREGROUND_POLL_INTERVAL_MS)
                pollNotifications()
            }
        }
    }

    fun stopForegroundPolling() {
        foregroundPollJob?.cancel()
        foregroundPollJob = null
    }

    private suspend fun pollNotifications() {
        val loggedIn = sessionManager.isLoggedIn.first()
        if (!loggedIn) return

        repository.getNotifications(refresh = true)
            .onSuccess { result ->
                val notifications = result.notifications
                if (notifications.isNotEmpty()) {
                    val newestId = notifications.first().id
                    val previousId = notificationStateManager.getLastPolledId()
                    if (newestId != previousId) {
                        notificationStateManager.updateLastPolledId(newestId)
                    }
                }
            }
    }
}
