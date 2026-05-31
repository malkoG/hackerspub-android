package pub.hackers.android.ui.components

import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility

fun Post.canPinToViewerProfile(): Boolean {
    return actor.isViewer &&
        sharedPost == null &&
        (visibility == PostVisibility.PUBLIC || visibility == PostVisibility.UNLISTED)
}

fun Post.canEditNote(): Boolean {
    return actor.isViewer &&
        sharedPost == null &&
        typename == "Note" &&
        rawContent != null
}
