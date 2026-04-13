package pub.hackers.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.navigation.HackersPubRoute
import pub.hackers.android.navigation.HackersPubUrlRouter
import pub.hackers.android.navigation.toNavRoute
import pub.hackers.android.ui.HackersPubApp
import pub.hackers.android.ui.theme.HackersPubTheme
import pub.hackers.android.ui.theme.LocalAppColors
import javax.inject.Inject

data class DeepLinkData(
    val token: String,
    val code: String
)

data class NavigationIntent(
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLinkData by mutableStateOf<DeepLinkData?>(null)
    private var navigationIntent by mutableStateOf<NavigationIntent?>(null)

    @Inject
    lateinit var sessionManager: SessionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Permission result — no action needed, worker checks at post time
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleDeepLink(intent)
            handleNavigationIntent(intent)
        }
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            HackersPubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAppColors.current.background
                ) {
                    HackersPubApp(
                        deepLinkData = deepLinkData,
                        navigationIntent = navigationIntent,
                        onDeepLinkConsumed = { deepLinkData = null },
                        onNavigationIntentConsumed = { navigationIntent = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
        handleNavigationIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isLoggedIn = runBlocking { sessionManager.isLoggedIn.first() }
            if (isLoggedIn && ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val route = intent?.getStringExtra("navigate_to") ?: return
        navigationIntent = NavigationIntent(route = route)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return

        // Handle legacy custom scheme
        if (data.scheme == "hackerspub" && data.host == "verify") {
            val token = data.getQueryParameter("token")
            val code = data.getQueryParameter("code")
            if (token != null && code != null) {
                deepLinkData = DeepLinkData(token = token, code = code)
            }
            return
        }

        // Handle HTTPS web links
        val url = data.toString()
        when (val route = HackersPubUrlRouter.resolve(url)) {
            is HackersPubRoute.SignInVerification -> {
                deepLinkData = DeepLinkData(token = route.token, code = route.code)
            }
            is HackersPubRoute.Profile, is HackersPubRoute.NoteDetail,
            is HackersPubRoute.TagSearch, is HackersPubRoute.Notifications -> {
                navigationIntent = NavigationIntent(route = route.toNavRoute())
            }
            null -> { /* Not a recognized URL, ignore */ }
        }
    }
}
