package pub.hackers.android.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.local.PreferencesManager
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.domain.model.Post
import javax.inject.Inject

enum class SearchMode { ALL, PEOPLE, POSTS, TAGS }

data class SearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.ALL,
    val actors: List<Actor> = emptyList(),
    val posts: List<Post> = emptyList(),
    val taggedPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
    val resolvedObjectUrl: String? = null,
    val recentSearches: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.recentSearches.collect { searches ->
                _uiState.update { it.copy(recentSearches = searches) }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setMode(mode: SearchMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun search() {
        val rawQuery = _uiState.value.query.trim()
        if (rawQuery.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    hasSearched = true,
                    resolvedObjectUrl = null,
                    actors = emptyList(),
                    posts = emptyList(),
                    taggedPosts = emptyList()
                )
            }

            preferencesManager.addRecentSearch(rawQuery)

            val handleQuery = rawQuery.removePrefix("@")
            val tagQuery = if (rawQuery.startsWith("#")) rawQuery else "#$rawQuery"

            val objectDeferred = async { repository.searchObject(rawQuery) }
            val actorsDeferred = async { repository.searchActorsByHandle(handleQuery, limit = 30) }
            val postsDeferred = async { repository.searchPosts(rawQuery) }
            val tagsDeferred = async { repository.searchPosts(tagQuery) }

            val objectResult = objectDeferred.await()
            val actorsResult = actorsDeferred.await()
            val postsResult = postsDeferred.await()
            val tagsResult = tagsDeferred.await()

            val resolvedUrl = objectResult.getOrNull()
            val actors = actorsResult.getOrDefault(emptyList())
            val posts = postsResult.getOrDefault(emptyList())
            val taggedPosts = tagsResult.getOrDefault(emptyList())

            val anySuccess =
                actorsResult.isSuccess || postsResult.isSuccess || tagsResult.isSuccess
            val firstError = listOf(actorsResult, postsResult, tagsResult)
                .firstNotNullOfOrNull { it.exceptionOrNull() }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    resolvedObjectUrl = resolvedUrl,
                    actors = actors,
                    posts = posts,
                    taggedPosts = taggedPosts,
                    error = if (!anySuccess) firstError?.message else null
                )
            }
        }
    }

    fun consumeResolvedUrl() {
        _uiState.update { it.copy(resolvedObjectUrl = null) }
    }

    fun clearSearch() {
        _uiState.update {
            SearchUiState(recentSearches = it.recentSearches, mode = it.mode)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            preferencesManager.removeRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferencesManager.clearRecentSearches()
        }
    }

    fun selectRecentSearch(query: String) {
        _uiState.update { it.copy(query = query) }
        search()
    }
}
