package pub.hackers.android.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.NotificationStateManager
import pub.hackers.android.data.paging.cursorPager
import pub.hackers.android.data.paging.notificationsPage
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Notification
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    private val notificationStateManager: NotificationStateManager,
) : ViewModel() {

    val notifications: Flow<PagingData<Notification>> =
        cursorPager { after -> repository.notificationsPage(after) }
            .flow
            .cachedIn(viewModelScope)

    fun markAsSeen() {
        viewModelScope.launch { notificationStateManager.markAsSeen() }
    }
}
