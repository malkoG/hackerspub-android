package pub.hackers.android.ui.components

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.navigation.HackersPubUrlRouter
import pub.hackers.android.navigation.toNavRoute

class InAppBrowserUriHandler(
    private val context: Context,
    private val useInAppBrowser: Boolean,
    private val onInternalNavigate: ((route: String) -> Unit)? = null
) : UriHandler {
    override fun openUri(uri: String) {
        val route = HackersPubUrlRouter.resolve(uri)
        if (route != null && onInternalNavigate != null) {
            onInternalNavigate.invoke(route.toNavRoute())
            return
        }
        openUrl(context, uri, useInAppBrowser)
    }
}

@Composable
fun ProvideInAppBrowserUriHandler(
    preferencesManager: PreferencesManager,
    onInternalNavigate: ((route: String) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val useInAppBrowser by preferencesManager.useInAppBrowser.collectAsState(initial = true)
    val context = LocalContext.current
    val uriHandler = InAppBrowserUriHandler(context, useInAppBrowser, onInternalNavigate)

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        content()
    }
}
