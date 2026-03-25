package pub.hackers.android.data.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pub.hackers.android.HackersPubApplication
import pub.hackers.android.MainActivity
import pub.hackers.android.R
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Notification

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: HackersPubRepository,
    private val sessionManager: SessionManager,
    private val notificationStateManager: NotificationStateManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val NOTIFICATION_GROUP = "pub.hackers.android.NOTIFICATIONS"
        const val SUMMARY_NOTIFICATION_ID = 0
    }

    override suspend fun doWork(): Result {
        val isLoggedIn = sessionManager.isLoggedIn.first()
        if (!isLoggedIn) return Result.success()

        val result = repository.getNotifications(refresh = true)

        return result.fold(
            onSuccess = { notificationsResult ->
                val notifications = notificationsResult.notifications
                if (notifications.isEmpty()) return@fold Result.success()

                val newestId = notifications.first().id
                val previousId = notificationStateManager.getLastPolledId()

                if (newestId != previousId) {
                    notificationStateManager.updateLastPolledId(newestId)
                    if (hasNotificationPermission()) {
                        showNotifications(notifications)
                    }
                }

                Result.success()
            },
            onFailure = { error ->
                // Auth errors — don't retry
                val message = error.message ?: ""
                if (message.contains("401") || message.contains("403") || message.contains("unauthorized", ignoreCase = true)) {
                    return@fold Result.success()
                }
                // Network errors — retry with exponential backoff
                Result.retry()
            }
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showNotifications(notifications: List<Notification>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = HackersPubApplication.NOTIFICATION_CHANNEL_ID

        // Show individual notifications (up to 5)
        notifications.take(5).forEachIndexed { index, notification ->
            val text = buildNotificationText(notification)
            val builder = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Hackers' Pub")
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(NOTIFICATION_GROUP)

            notificationManager.notify(index + 1, builder.build())
        }

        // Summary notification for grouping
        if (notifications.size > 1) {
            val summary = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Hackers' Pub")
                .setContentText("${notifications.size} new notifications")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .build()
            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summary)
        }
    }

    private fun buildNotificationText(notification: Notification): String {
        val actorName = notification.actors.firstOrNull()?.name ?: "Someone"
        return when (notification) {
            is Notification.Follow -> "$actorName followed you"
            is Notification.Mention -> "$actorName mentioned you"
            is Notification.Reply -> "$actorName replied to your post"
            is Notification.Quote -> "$actorName quoted your post"
            is Notification.Share -> "$actorName shared your post"
            is Notification.React -> "$actorName reacted to your post"
        }
    }
}
