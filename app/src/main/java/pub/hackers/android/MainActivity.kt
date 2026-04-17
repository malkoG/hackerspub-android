package pub.hackers.android

import android.Manifest
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.local.SessionManager
import pub.hackers.android.navigation.HackersPubRoute
import pub.hackers.android.navigation.HackersPubUrlRouter
import pub.hackers.android.navigation.toNavRoute
import pub.hackers.android.ui.HackersPubApp
import pub.hackers.android.ui.theme.HackersPubTheme
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.ThemeMode
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

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Permission result — no action needed, worker checks at post time
    }

    // Material You / dynamic-color recomposition does not happen automatically
    // when the system wallpaper changes — dynamicLightColorScheme(context) reads
    // resources at call time. Listen for color changes and recreate the Activity
    // so the new palette is picked up. Available on API 27+.
    private val wallpaperColorsListener =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager.OnColorsChangedListener { _, which ->
                if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                    recreate()
                }
            }
        } else null

    private var wallpaperListenerRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleDeepLink(intent)
            handleNavigationIntent(intent)
        }
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            LaunchedEffect(themeMode) {
                if (themeMode == ThemeMode.DYNAMIC) {
                    registerWallpaperColorsListener()
                } else {
                    unregisterWallpaperColorsListener()
                }
            }
            HackersPubTheme(themeMode = themeMode) {
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

    private fun registerWallpaperColorsListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        if (wallpaperListenerRegistered) return
        val listener = wallpaperColorsListener ?: return
        WallpaperManager.getInstance(this)
            .addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        wallpaperListenerRegistered = true
    }

    private fun unregisterWallpaperColorsListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        if (!wallpaperListenerRegistered) return
        wallpaperColorsListener?.let {
            WallpaperManager.getInstance(this).removeOnColorsChangedListener(it)
        }
        wallpaperListenerRegistered = false
    }

    override fun onDestroy() {
        unregisterWallpaperColorsListener()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                val isLoggedIn = sessionManager.isLoggedIn.first()
                if (isLoggedIn && ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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
            is HackersPubRoute.PostByUrl,
            is HackersPubRoute.TagSearch -> {
                navigationIntent = NavigationIntent(route = route.toNavRoute())
            }
            null -> {
                // Unrecognized hackers.pub URL — open in browser
                val browserIntent = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_BROWSER
                ).apply {
                    setData(data)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(browserIntent)
                } catch (_: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, data))
                }
                if (isTaskRoot) finish()
            }
        }
    }
}
