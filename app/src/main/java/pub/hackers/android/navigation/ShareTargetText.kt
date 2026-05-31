package pub.hackers.android.navigation

internal object ShareTargetText {
    fun format(subject: CharSequence?, text: CharSequence?): String? {
        val sharedText = text?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val sharedSubject = subject?.toString()?.takeIf { it.isNotBlank() }

        return if (sharedSubject != null) {
            "$sharedSubject\n\n$sharedText"
        } else {
            sharedText
        }
    }
}
