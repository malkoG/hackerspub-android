package pub.hackers.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStateManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val LAST_SEEN_NOTIFICATION_ID = stringPreferencesKey("last_seen_notification_id")
        private val LAST_POLLED_NOTIFICATION_ID = stringPreferencesKey("last_polled_notification_id")
    }

    val hasUnread: Flow<Boolean> = dataStore.data.map { prefs ->
        val lastSeen = prefs[LAST_SEEN_NOTIFICATION_ID]
        val lastPolled = prefs[LAST_POLLED_NOTIFICATION_ID]
        lastPolled != null && lastPolled != lastSeen
    }

    suspend fun getLastPolledId(): String? {
        return dataStore.data.map { it[LAST_POLLED_NOTIFICATION_ID] }.first()
    }

    suspend fun updateLastPolledId(id: String) {
        dataStore.edit { prefs ->
            prefs[LAST_POLLED_NOTIFICATION_ID] = id
        }
    }

    suspend fun markAsSeen() {
        dataStore.edit { prefs ->
            val lastPolled = prefs[LAST_POLLED_NOTIFICATION_ID]
            if (lastPolled != null) {
                prefs[LAST_SEEN_NOTIFICATION_ID] = lastPolled
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(LAST_SEEN_NOTIFICATION_ID)
            prefs.remove(LAST_POLLED_NOTIFICATION_ID)
        }
    }
}
