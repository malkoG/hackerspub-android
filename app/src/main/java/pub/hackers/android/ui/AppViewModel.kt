package pub.hackers.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.messaging.FcmTokenManager
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
    val timelineRefreshTrigger: TimelineRefreshTrigger,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {

    companion object {
        const val NOTIFICATION_WORK_NAME = "notification_poll"
    }

    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    val hasUnread: StateFlow<Boolean> = notificationStateManager.hasUnread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        ensureNotificationPolling()
    }

    private fun ensureNotificationPolling() {
        viewModelScope.launch {
            val loggedIn = sessionManager.isLoggedIn.first()
            if (loggedIn) {
                enqueueNotificationPolling()
            }
        }
    }

    fun registerFcmToken() {
        viewModelScope.launch {
            fcmTokenManager.registerCurrentToken()
        }
    }

    fun unregisterFcmToken() {
        viewModelScope.launch {
            fcmTokenManager.unregisterCurrentToken()
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
}
