package pub.hackers.android.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionManager: SessionManager,
    val preferencesManager: PreferencesManager
) : ViewModel() {
    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn
}
