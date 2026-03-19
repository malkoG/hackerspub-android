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

class InAppBrowserUriHandler(
    private val context: Context,
    private val useInAppBrowser: Boolean
) : UriHandler {
    override fun openUri(uri: String) {
        openUrl(context, uri, useInAppBrowser)
    }
}

@Composable
fun ProvideInAppBrowserUriHandler(
    preferencesManager: PreferencesManager,
    content: @Composable () -> Unit
) {
    val useInAppBrowser by preferencesManager.useInAppBrowser.collectAsState(initial = true)
    val context = LocalContext.current
    val uriHandler = InAppBrowserUriHandler(context, useInAppBrowser)

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        content()
    }
}
