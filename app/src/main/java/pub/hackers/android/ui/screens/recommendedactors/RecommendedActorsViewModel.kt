package pub.hackers.android.ui.screens.recommendedactors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.Actor
import javax.inject.Inject

data class RecommendedActorsUiState(
    val displayedActors: List<Actor> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecommendedActorsViewModel @Inject constructor(
    private val repository: HackersPubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendedActorsUiState())
    val uiState: StateFlow<RecommendedActorsUiState> = _uiState.asStateFlow()

    private val shownActors = mutableListOf<Actor>()
    private val hiddenActors = mutableListOf<Actor>()

    init {
        loadActors()
    }

    private fun loadActors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getRecommendedActors(limit = FETCH_SIZE).fold(
                onSuccess = { actors ->
                    shownActors.clear()
                    shownActors.addAll(actors.take(WINDOW_SIZE))
                    hiddenActors.clear()
                    hiddenActors.addAll(actors.drop(WINDOW_SIZE))
                    _uiState.update {
                        it.copy(isLoading = false, displayedActors = shownActors.toList())
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun dismissActor(actorId: String) {
        val index = shownActors.indexOfFirst { it.id == actorId }
        if (index < 0) return
        replaceActorAt(index)
    }

    fun followActor(actorId: String) {
        val index = shownActors.indexOfFirst { it.id == actorId }
        if (index < 0) return
        viewModelScope.launch {
            repository.followActor(actorId).fold(
                onSuccess = { replaceActorAt(index) },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    private fun replaceActorAt(index: Int) {
        if (hiddenActors.isNotEmpty()) {
            shownActors[index] = hiddenActors.removeFirst()
        } else {
            shownActors.removeAt(index)
        }
        _uiState.update { it.copy(displayedActors = shownActors.toList()) }
    }

    companion object {
        private const val WINDOW_SIZE = 6
        private const val FETCH_SIZE = 50
    }
}
