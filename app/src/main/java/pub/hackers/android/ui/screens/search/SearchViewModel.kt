package pub.hackers.android.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class SearchUiState(
    val query: String = "",
    val actors: List<Actor> = emptyList(),
    val posts: List<Post> = emptyList(),
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

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasSearched = true, resolvedObjectUrl = null, actors = emptyList()) }

            // Save to recent searches
            preferencesManager.addRecentSearch(query)

            // Try searchObject first for URL/handle resolution
            repository.searchObject(query)
                .onSuccess { url ->
                    if (url != null) {
                        _uiState.update { it.copy(resolvedObjectUrl = url) }
                    }
                }

            // Search actors if query looks like a handle
            if (query.startsWith("@") || query.contains("@")) {
                val handleQuery = query.removePrefix("@")
                repository.searchActorsByHandle(handleQuery, limit = 5)
                    .onSuccess { actors ->
                        _uiState.update { it.copy(actors = actors) }
                    }
            }

            // Always search posts too
            repository.searchPosts(query)
                .onSuccess { posts ->
                    _uiState.update {
                        it.copy(
                            posts = posts,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun consumeResolvedUrl() {
        _uiState.update { it.copy(resolvedObjectUrl = null) }
    }

    fun clearSearch() {
        _uiState.update {
            SearchUiState(recentSearches = it.recentSearches)
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
