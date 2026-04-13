package pub.hackers.android.ui.screens.drafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.ArticleDraft
import javax.inject.Inject

data class DraftsUiState(
    val drafts: List<ArticleDraft> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val deletingDraftId: String? = null
)

@HiltViewModel
class DraftsViewModel @Inject constructor(
    private val repository: HackersPubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftsUiState())
    val uiState: StateFlow<DraftsUiState> = _uiState.asStateFlow()

    init {
        loadDrafts()
    }

    fun loadDrafts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getArticleDrafts()
                .onSuccess { drafts ->
                    _uiState.update {
                        it.copy(
                            drafts = drafts,
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

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingDraftId = draftId) }

            repository.deleteArticleDraft(draftId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            drafts = state.drafts.filter { it.id != draftId },
                            deletingDraftId = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            deletingDraftId = null
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
