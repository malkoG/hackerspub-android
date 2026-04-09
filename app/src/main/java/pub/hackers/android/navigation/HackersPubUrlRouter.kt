package pub.hackers.android.navigation

import android.util.Base64
import pub.hackers.android.ui.DetailScreen
import pub.hackers.android.ui.Screen
import java.net.URI

sealed class HackersPubRoute {
    data class Profile(val handle: String) : HackersPubRoute()
    data class NoteDetail(val globalId: String) : HackersPubRoute()
    data class SignInVerification(val token: String, val code: String) : HackersPubRoute()
    data class TagSearch(val tag: String) : HackersPubRoute()
}

object HackersPubUrlRouter {

    private const val HOST = "hackers.pub"
    private val UUID_REGEX = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )

    fun resolve(url: String): HackersPubRoute? {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }

        if (uri.host != HOST) return null
        if (uri.scheme != "https" && uri.scheme != "http") return null

        val path = uri.path?.trimEnd('/') ?: return null
        val segments = path.split('/').filter { it.isNotEmpty() }

        return when {
            // /tags/<tag>
            segments.size == 2 && segments[0] == "tags" -> {
                HackersPubRoute.TagSearch(segments[1])
            }

            // /sign/in/<token>?code=<code>
            segments.size >= 3 && segments[0] == "sign" && segments[1] == "in" -> {
                val token = segments[2]
                val query = uri.query ?: return null
                val code = parseQueryParam(query, "code") ?: return null
                HackersPubRoute.SignInVerification(token, code)
            }

            // /@<handle>/<noteId> where noteId is a UUID
            segments.size == 2 && segments[0].startsWith("@") && UUID_REGEX.matches(segments[1]) -> {
                val handle = segments[0].removePrefix("@")
                val noteId = segments[1]
                val globalId = encodeRelayId("Note", noteId)
                HackersPubRoute.NoteDetail(globalId)
            }

            // /@<handle>
            segments.size == 1 && segments[0].startsWith("@") -> {
                val username = segments[0].removePrefix("@")
                val handle = if ("@" in username) username else "$username@$HOST"
                HackersPubRoute.Profile(handle)
            }

            else -> null
        }
    }

    fun isHackersPubUrl(url: String): Boolean {
        return try {
            URI(url).host == HOST
        } catch (_: Exception) {
            false
        }
    }

    private fun encodeRelayId(typeName: String, rawId: String): String {
        val raw = "$typeName:$rawId"
        return Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun parseQueryParam(query: String, key: String): String? {
        return query.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it[0] == key }
            ?.getOrNull(1)
    }
}

fun HackersPubRoute.toNavRoute(): String {
    return when (this) {
        is HackersPubRoute.Profile -> DetailScreen.Profile.createRoute(handle)
        is HackersPubRoute.NoteDetail -> DetailScreen.PostDetail.createRoute(globalId)
        is HackersPubRoute.SignInVerification -> DetailScreen.SignIn.createRoute(token, code)
        is HackersPubRoute.TagSearch -> Screen.Search.createRoute(tag)
    }
}
