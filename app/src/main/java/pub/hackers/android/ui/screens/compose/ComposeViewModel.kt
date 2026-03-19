package pub.hackers.android.ui.screens.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.icu.util.ULocale
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject

data class ComposeUiState(
    val content: String = "",
    val cursorPosition: Int = 0,
    val language: String = java.util.Locale.getDefault().language,
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val replyToId: String? = null,
    val replyTargetPost: Post? = null,
    val isLoadingReplyTarget: Boolean = false,
    val quotedPostId: String? = null,
    val quotedPost: Post? = null,
    val isLoadingQuotedPost: Boolean = false,
    val quotedPostLoadFailed: Boolean = false,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val error: String? = null,
    // Mention autocomplete state
    val mentionQuery: String? = null,
    val mentionStartIndex: Int = -1,
    val mentionSuggestions: List<Actor> = emptyList(),
    val isLoadingMentions: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private var viewerHandle: String? = null

    // Regex to detect @mention at cursor position
    // Matches: @handle or @user@domain patterns that are incomplete
    private val mentionRegex = Regex("""@([^\s@]+(?:@[^\s@]*)?)$""")

    private val mentionQueryFlow = MutableSharedFlow<String>(replay = 0)

    init {
        viewModelScope.launch {
            repository.getViewer().onSuccess { viewer ->
                viewerHandle = viewer?.handle
            }
        }

        // Setup debounced mention search
        viewModelScope.launch {
            mentionQueryFlow
                .debounce(300L)
                .collectLatest { query ->
                    if (query.isNotEmpty()) {
                        searchMentions(query)
                    }
                }
        }
    }

    private suspend fun searchMentions(query: String) {
        _uiState.update { it.copy(isLoadingMentions = true) }

        repository.searchActorsByHandle(query, limit = 10)
            .onSuccess { actors ->
                _uiState.update {
                    it.copy(
                        mentionSuggestions = actors,
                        isLoadingMentions = false
                    )
                }
            }
            .onFailure {
                _uiState.update {
                    it.copy(
                        mentionSuggestions = emptyList(),
                        isLoadingMentions = false
                    )
                }
            }
    }

    private fun detectMentionTrigger(content: String, cursorPosition: Int) {
        // Get text before cursor
        val textBeforeCursor = content.take(cursorPosition)

        // Find mention pattern at cursor position
        val match = mentionRegex.find(textBeforeCursor)

        if (match != null) {
            val mentionQuery = match.groupValues[1]
            val mentionStartIndex = match.range.first

            _uiState.update {
                it.copy(
                    mentionQuery = mentionQuery,
                    mentionStartIndex = mentionStartIndex
                )
            }

            // Trigger debounced search
            viewModelScope.launch {
                mentionQueryFlow.emit(mentionQuery)
            }
        } else {
            // Clear mention state
            clearMentionState()
        }
    }

    private fun clearMentionState() {
        _uiState.update {
            it.copy(
                mentionQuery = null,
                mentionStartIndex = -1,
                mentionSuggestions = emptyList(),
                isLoadingMentions = false
            )
        }
    }

    fun selectMention(actor: Actor): Pair<String, Int> {
        val state = _uiState.value
        val content = state.content
        val startIndex = state.mentionStartIndex

        if (startIndex >= 0) {
            // Replace @partial with @full_handle
            val beforeMention = content.take(startIndex)
            val textAfterMentionStart = content.drop(startIndex)

            // Find where the partial mention ends (next space, newline, or end of string)
            val mentionEndOffset = textAfterMentionStart.indexOfFirst { it == ' ' || it == '\n' }
            val afterMention = if (mentionEndOffset >= 0) {
                textAfterMentionStart.drop(mentionEndOffset)
            } else {
                " " // Add space after mention if at end
            }

            val normalizedHandle = actor.handle.removePrefix("@")
            val insertedHandle = "@$normalizedHandle "
            val newContent = "$beforeMention$insertedHandle${afterMention.trimStart()}"
            val newCursorPosition = beforeMention.length + insertedHandle.length

            _uiState.update {
                it.copy(
                    content = newContent,
                    cursorPosition = newCursorPosition,
                    mentionQuery = null,
                    mentionStartIndex = -1,
                    mentionSuggestions = emptyList(),
                    isLoadingMentions = false
                )
            }

            return Pair(newContent, newCursorPosition)
        }

        return Pair(content, state.cursorPosition)
    }

    fun dismissMentionSuggestions() {
        clearMentionState()
    }

    fun setQuotedPost(postId: String) {
        _uiState.update { it.copy(quotedPostId = postId, isLoadingQuotedPost = true, quotedPostLoadFailed = false) }
        viewModelScope.launch {
            repository.getPostDetail(postId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            quotedPost = result.post,
                            isLoadingQuotedPost = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingQuotedPost = false, quotedPostLoadFailed = true) }
                }
        }
    }

    fun setReplyTarget(postId: String) {
        _uiState.update { it.copy(replyToId = postId, isLoadingReplyTarget = true) }
        viewModelScope.launch {
            repository.getPostDetail(postId)
                .onSuccess { result ->
                    val post = result.post
                    val mentionPrefix = buildMentionPrefix(post)
                    _uiState.update {
                        it.copy(
                            replyTargetPost = post,
                            isLoadingReplyTarget = false,
                            content = mentionPrefix
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingReplyTarget = false) }
                }
        }
    }

    private fun buildMentionPrefix(post: Post): String {
        val mentions = mutableSetOf<String>()

        // Add the post author (normalize by removing leading @)
        mentions.add(post.actor.handle.removePrefix("@"))

        // Add existing mentions from the post (normalize)
        mentions.addAll(post.mentions.map { it.removePrefix("@") })

        // Remove viewer's own handle if present (normalize for comparison)
        viewerHandle?.let { mentions.remove(it.removePrefix("@")) }

        return if (mentions.isNotEmpty()) {
            mentions.joinToString(" ") { "@${it.removePrefix("@")}" } + " "
        } else {
            ""
        }
    }

    fun updateContent(content: String, cursorPosition: Int = content.length) {
        _uiState.update { it.copy(content = content, cursorPosition = cursorPosition) }

        // Check for mention trigger
        detectMentionTrigger(content, cursorPosition)

        // Detect language
        detectLanguage(content)
    }

    private fun detectLanguage(text: String) {
        if (text.isBlank() || text.length < 20) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val tcm = context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE) as? TextClassificationManager
                val classifier = tcm?.textClassifier ?: return
                val request = TextLanguage.Request.Builder(text).build()
                val result = classifier.detectLanguage(request)
                if (result.localeHypothesisCount > 0) {
                    val topLocale = result.getLocale(0)
                    val lang = topLocale.language
                    if (lang.isNotEmpty()) {
                        _uiState.update { it.copy(language = lang) }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback: keep current language
        }
    }

    fun updateVisibility(visibility: PostVisibility) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun post() {
        val state = _uiState.value
        if (state.content.isBlank() || state.isPosting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }

            repository.createNote(
                content = state.content,
                language = state.language,
                visibility = state.visibility,
                replyTargetId = state.replyToId,
                quotedPostId = state.quotedPostId
            )
                .onSuccess {
                    _uiState.update { it.copy(isPosting = false, isPosted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isPosting = false
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
