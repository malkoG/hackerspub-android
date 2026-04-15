package pub.hackers.android.ui.screens.timeline

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineRefreshTrigger @Inject constructor() {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests = _requests.asSharedFlow()

    fun requestRefresh() {
        _requests.tryEmit(Unit)
    }
}
