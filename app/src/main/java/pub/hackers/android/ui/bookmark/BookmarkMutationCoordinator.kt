package pub.hackers.android.ui.bookmark

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Serializes bookmark mutations per post while preserving optimistic UI state.
 *
 * The UI is updated immediately to the user's latest desired state. Network
 * requests are sent one at a time per post id; if the user toggles again while
 * a request is in flight, we remember the last desired state and enqueue a
 * follow-up request after the current one completes.
 */
class BookmarkMutationCoordinator(
    private val scope: CoroutineScope,
    private val requestMutation: suspend (postId: String, shouldBookmark: Boolean) -> Result<Unit>,
    private val applyDesiredState: (postId: String, isBookmarked: Boolean) -> Unit,
    private val revertFailedState: (postId: String, attemptedState: Boolean) -> Unit,
) {
    private val inFlight = mutableSetOf<String>()
    private val lastDesired = mutableMapOf<String, Boolean>()

    fun toggle(postId: String, currentIsBookmarked: Boolean) {
        val desiredState = !currentIsBookmarked
        lastDesired[postId] = desiredState
        applyDesiredState(postId, desiredState)

        if (!inFlight.add(postId)) return
        sync(postId, desiredState)
    }

    private fun sync(postId: String, desiredState: Boolean) {
        scope.launch {
            val result = requestMutation(postId, desiredState)
            val latestDesired = lastDesired[postId]

            if (result.isFailure && latestDesired == desiredState) {
                revertFailedState(postId, desiredState)
                lastDesired.remove(postId)
            } else if (result.isSuccess && latestDesired == desiredState) {
                lastDesired.remove(postId)
            }

            inFlight.remove(postId)

            val nextDesired = lastDesired[postId]
            if (nextDesired != null && nextDesired != desiredState && inFlight.add(postId)) {
                sync(postId, nextDesired)
            }
        }
    }
}
