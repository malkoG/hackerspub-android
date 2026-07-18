package pub.hackers.android.ui.screens.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.net.Uri
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import androidx.compose.runtime.Immutable
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.NoteMediumAttachment
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility
import pub.hackers.android.domain.model.QuotePolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.util.UUID
import java.time.Instant
import javax.inject.Inject

private const val MAX_NOTE_MEDIA = 20
private const val MIN_POLL_OPTIONS = 2
private const val MAX_POLL_OPTIONS = 20
private const val MAX_POLL_TITLE_LENGTH = 200

@Immutable
data class ComposeMediaAttachment(
    val localId: String,
    val uri: Uri,
    val altText: String = "",
    val uploadProgress: Int = 0,
    val uploadedMediumId: String? = null,
    val uploadedMediumRelayId: String? = null,
    val isUploading: Boolean = true,
    val isGeneratingAltText: Boolean = false,
    val error: String? = null,
)

data class ComposeUiState(
    val content: String = "",
    val cursorPosition: Int = 0,
    val language: String = java.util.Locale.getDefault().language,
    val isLanguageManuallySet: Boolean = false,
    val suggestedLanguages: List<String> = emptyList(),
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val quotePolicy: QuotePolicy = QuotePolicy.EVERYONE,
    val replyToId: String? = null,
    val replyTargetPost: Post? = null,
    val isLoadingReplyTarget: Boolean = false,
    val quotedPostId: String? = null,
    val quotedPost: Post? = null,
    val isLoadingQuotedPost: Boolean = false,
    val quotedPostLoadFailed: Boolean = false,
    val editPostId: String? = null,
    val isLoadingEditTarget: Boolean = false,
    val mediaAttachments: List<ComposeMediaAttachment> = emptyList(),
    // Poll draft state
    val pollEnabled: Boolean = false,
    val pollTitle: String = "",
    val pollOptions: List<String> = listOf("", ""),
    val pollMultiple: Boolean = false,
    val pollDurationMinutes: Long = 24 * 60,
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
    @param:ApplicationContext private val context: Context,
    private val replyPostedSignal: ReplyPostedSignal,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private var viewerHandle: String? = null
    private val uploadJobs = mutableMapOf<String, Job>()

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

        loadSuggestedLanguages()

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

    private fun loadSuggestedLanguages() {
        viewModelScope.launch {
            runCatching {
                repository.getSuggestedFilterLanguages().onSuccess { languages ->
                    _uiState.update { it.copy(suggestedLanguages = languages) }
                }
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
                    val post = result.post
                    if (post.viewerCanQuote) {
                        _uiState.update {
                            it.copy(
                                quotedPost = post,
                                quotedPostId = post.id,
                                isLoadingQuotedPost = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                quotedPostId = null,
                                quotedPost = null,
                                isLoadingQuotedPost = false,
                                quotedPostLoadFailed = false,
                                error = "You cannot quote this post"
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingQuotedPost = false, quotedPostLoadFailed = true) }
                }
        }
    }

    fun setEditTarget(postId: String) {
        val currentState = _uiState.value
        if (currentState.editPostId == postId && !currentState.isLoadingEditTarget) return

        _uiState.update {
            it.copy(
                editPostId = postId,
                isLoadingEditTarget = true,
                error = null,
            )
        }
        viewModelScope.launch {
            repository.getPostDetail(postId)
                .onSuccess { result ->
                    val post = result.post
                    val rawContent = post.rawContent
                    if (post.typename != "Note" || rawContent.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                editPostId = null,
                                isLoadingEditTarget = false,
                                error = "This note cannot be edited"
                            )
                        }
                        return@onSuccess
                    }

                    _uiState.update {
                        it.copy(
                            content = rawContent,
                            cursorPosition = rawContent.length,
                            language = post.language ?: it.language,
                            visibility = post.visibility,
                            quotePolicy = post.quotePolicy,
                            editPostId = post.id,
                            isLoadingEditTarget = false,
                            mediaAttachments = emptyList(),
                            mentionQuery = null,
                            mentionStartIndex = -1,
                            mentionSuggestions = emptyList(),
                            isLoadingMentions = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            editPostId = null,
                            isLoadingEditTarget = false,
                            error = error.message ?: "Unable to load note"
                        )
                    }
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
                            content = if (it.content.isBlank()) mentionPrefix else it.content,
                            cursorPosition = if (it.content.isBlank()) mentionPrefix.length else it.cursorPosition
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

    fun setInitialContent(content: String) {
        if (content.isBlank()) return

        _uiState.update {
            if (it.content.isBlank()) {
                it.copy(
                    content = content,
                    cursorPosition = content.length,
                )
            } else {
                it
            }
        }
        detectLanguage(content)
    }

    fun updateContent(content: String, cursorPosition: Int = content.length) {
        _uiState.update { it.copy(content = content, cursorPosition = cursorPosition) }

        // Check for mention trigger
        detectMentionTrigger(content, cursorPosition)

        // Detect language
        detectLanguage(content)
    }

    fun updateLanguage(language: String) {
        _uiState.update {
            it.copy(
                language = language.trim().replace('_', '-'),
                isLanguageManuallySet = true,
            )
        }
    }

    private fun detectLanguage(text: String) {
        if (_uiState.value.isLanguageManuallySet) return
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

    fun updateQuotePolicy(quotePolicy: QuotePolicy) {
        _uiState.update { it.copy(quotePolicy = quotePolicy) }
    }

    fun togglePoll() {
        _uiState.update { it.copy(pollEnabled = !it.pollEnabled) }
    }

    fun updatePollTitle(title: String) {
        _uiState.update { it.copy(pollTitle = title.take(MAX_POLL_TITLE_LENGTH)) }
    }

    fun updatePollOption(index: Int, value: String) {
        _uiState.update { state ->
            if (index !in state.pollOptions.indices) return@update state
            state.copy(
                pollOptions = state.pollOptions.toMutableList().also { it[index] = value }
            )
        }
    }

    fun addPollOption() {
        _uiState.update { state ->
            if (state.pollOptions.size >= MAX_POLL_OPTIONS) state
            else state.copy(pollOptions = state.pollOptions + "")
        }
    }

    fun removePollOption(index: Int) {
        _uiState.update { state ->
            if (state.pollOptions.size <= MIN_POLL_OPTIONS || index !in state.pollOptions.indices) {
                state
            } else {
                state.copy(pollOptions = state.pollOptions.filterIndexed { i, _ -> i != index })
            }
        }
    }

    fun setPollMultiple(multiple: Boolean) {
        _uiState.update { it.copy(pollMultiple = multiple) }
    }

    fun updatePollDurationMinutes(minutes: Long) {
        _uiState.update { it.copy(pollDurationMinutes = minutes) }
    }

    fun addMediaUris(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val currentCount = _uiState.value.mediaAttachments.size
        val remainingSlots = MAX_NOTE_MEDIA - currentCount
        if (remainingSlots <= 0) {
            _uiState.update { it.copy(error = "You can attach up to $MAX_NOTE_MEDIA images") }
            return
        }

        val acceptedUris = uris.take(remainingSlots)
        if (acceptedUris.size < uris.size) {
            _uiState.update { it.copy(error = "Some images were skipped because the limit is $MAX_NOTE_MEDIA") }
        }

        val attachments = acceptedUris.map { uri ->
            ComposeMediaAttachment(
                localId = UUID.randomUUID().toString(),
                uri = uri,
            )
        }

        _uiState.update { state ->
            state.copy(mediaAttachments = state.mediaAttachments + attachments)
        }

        attachments.forEach { attachment ->
            uploadMediaAttachment(attachment.localId, attachment.uri)
        }
    }

    fun removeMediaAttachment(localId: String) {
        uploadJobs.remove(localId)?.cancel()
        _uiState.update { state ->
            state.copy(mediaAttachments = state.mediaAttachments.filterNot { it.localId == localId })
        }
    }

    fun updateMediaAltText(localId: String, altText: String) {
        updateMediaAttachment(localId) { it.copy(altText = altText) }
    }

    fun generateAltText(localId: String) {
        val attachment = _uiState.value.mediaAttachments.firstOrNull { it.localId == localId }
        val mediumRelayId = attachment?.uploadedMediumRelayId ?: return
        if (attachment.isGeneratingAltText) return

        updateMediaAttachment(localId) { it.copy(isGeneratingAltText = true, error = null) }
        viewModelScope.launch {
            repository.generateMediumAltText(
                mediumRelayId = mediumRelayId,
                language = _uiState.value.language,
                context = _uiState.value.content,
            )
                .onSuccess { altText ->
                    updateMediaAttachment(localId) {
                        it.copy(
                            altText = altText,
                            isGeneratingAltText = false,
                        )
                    }
                }
                .onFailure { error ->
                    updateMediaAttachment(localId) {
                        it.copy(
                            isGeneratingAltText = false,
                            error = error.message,
                        )
                    }
                    _uiState.update { it.copy(error = error.message ?: "Alt text generation failed") }
                }
        }
    }

    private fun uploadMediaAttachment(localId: String, uri: Uri) {
        uploadJobs[localId]?.cancel()
        uploadJobs[localId] = viewModelScope.launch {
            try {
                repository.uploadMedium(uri) { progress ->
                    updateMediaAttachment(localId) { it.copy(uploadProgress = progress) }
                }
                    .onSuccess { medium ->
                        updateMediaAttachment(localId) {
                            it.copy(
                                uploadProgress = 100,
                                uploadedMediumId = medium.uuid,
                                uploadedMediumRelayId = medium.relayId,
                                isUploading = false,
                            )
                        }
                    }
                    .onFailure { error ->
                        updateMediaAttachment(localId) {
                            it.copy(
                                isUploading = false,
                                error = error.message,
                            )
                        }
                        _uiState.update { it.copy(error = error.message ?: "Image upload failed") }
                    }
            } finally {
                uploadJobs.remove(localId)
            }
        }
    }

    private fun updateMediaAttachment(
        localId: String,
        transform: (ComposeMediaAttachment) -> ComposeMediaAttachment,
    ) {
        _uiState.update { state ->
            state.copy(
                mediaAttachments = state.mediaAttachments.map { attachment ->
                    if (attachment.localId == localId) transform(attachment) else attachment
                }
            )
        }
    }

    fun post() {
        val state = _uiState.value
        if (state.content.isBlank() || state.isPosting) return
        val editPostId = state.editPostId
        if (editPostId != null) {
            updateExistingNote(state, editPostId)
            return
        }

        val media = state.mediaAttachments.map { attachment ->
            val mediumId = attachment.uploadedMediumId
            when {
                attachment.isUploading || mediumId == null -> {
                    _uiState.update { it.copy(error = "Wait for image uploads to finish") }
                    return
                }
                attachment.altText.isBlank() -> {
                    _uiState.update { it.copy(error = "Alt text is required for every image") }
                    return
                }
                attachment.error != null -> {
                    _uiState.update { it.copy(error = attachment.error) }
                    return
                }
                else -> NoteMediumAttachment(
                    mediumId = mediumId,
                    alt = attachment.altText.trim(),
                )
            }
        }

        if (state.pollEnabled) {
            val trimmedTitle = state.pollTitle.trim()
            val trimmedOptions = state.pollOptions.map { it.trim() }.filter { it.isNotEmpty() }
            when {
                trimmedTitle.isEmpty() -> {
                    _uiState.update { it.copy(error = "Poll title is required") }
                    return
                }
                trimmedOptions.size < MIN_POLL_OPTIONS -> {
                    _uiState.update { it.copy(error = "Add at least $MIN_POLL_OPTIONS poll options") }
                    return
                }
                trimmedOptions.size != trimmedOptions.distinct().size -> {
                    _uiState.update { it.copy(error = "Poll options must be unique") }
                    return
                }
            }
            postQuestion(state, trimmedTitle, trimmedOptions, media)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }

            repository.createNote(
                content = state.content,
                language = state.language,
                visibility = state.visibility,
                quotePolicy = state.effectiveQuotePolicy(),
                replyTargetId = state.replyToId,
                quotedPostId = state.quotedPostId,
                media = media,
            )
                .onSuccess { newPost ->
                    state.replyToId?.let { replyTargetId ->
                        replyPostedSignal.emit(ReplyPostedEvent(replyTargetId, newPost))
                    }
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

    private fun postQuestion(
        state: ComposeUiState,
        title: String,
        options: List<String>,
        media: List<NoteMediumAttachment>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }

            val ends = Instant.now().plusSeconds(state.pollDurationMinutes * 60)
            repository.createQuestion(
                content = state.content,
                pollTitle = title,
                pollOptions = options,
                pollMultiple = state.pollMultiple,
                pollEnds = ends,
                language = state.language,
                visibility = state.visibility,
                quotePolicy = state.effectiveQuotePolicy(),
                replyTargetId = state.replyToId,
                quotedPostId = state.quotedPostId,
                media = media,
            )
                .onSuccess { newPost ->
                    state.replyToId?.let { replyTargetId ->
                        replyPostedSignal.emit(ReplyPostedEvent(replyTargetId, newPost))
                    }
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

    private fun updateExistingNote(state: ComposeUiState, editPostId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }

            repository.updateNote(
                noteId = editPostId,
                content = state.content,
                language = state.language,
                quotePolicy = state.effectiveQuotePolicy(),
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

    private fun ComposeUiState.effectiveQuotePolicy(): QuotePolicy {
        return if (visibility == PostVisibility.PUBLIC || visibility == PostVisibility.UNLISTED) {
            quotePolicy
        } else {
            QuotePolicy.SELF
        }
    }

    override fun onCleared() {
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
        super.onCleared()
    }
}
