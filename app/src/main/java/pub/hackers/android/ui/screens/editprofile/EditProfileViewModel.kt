package pub.hackers.android.ui.screens.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import pub.hackers.android.domain.model.EditableAccountLink
import javax.inject.Inject

data class EditProfileUiState(
    val id: String = "",
    val handle: String = "",
    val name: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val pendingAvatarDataUrl: String? = null,
    val links: List<EditableAccountLink> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
)

sealed interface EditProfileEvent {
    data object Saved : EditProfileEvent
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: HackersPubRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            repository.getEditableAccount()
                .onSuccess { account ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            id = account.id,
                            handle = account.handle,
                            name = account.name,
                            bio = account.bio,
                            avatarUrl = account.avatarUrl,
                            pendingAvatarDataUrl = null,
                            links = account.links,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, loadError = error.message)
                    }
                }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bio = value) }
    }

    fun onAvatarPicked(dataUrl: String) {
        _uiState.update { it.copy(pendingAvatarDataUrl = dataUrl) }
    }

    fun onLinkAdd() {
        _uiState.update { it.copy(links = it.links + EditableAccountLink(name = "", url = "")) }
    }

    fun onLinkChange(index: Int, name: String, url: String) {
        _uiState.update {
            val updated = it.links.toMutableList()
            if (index in updated.indices) {
                updated[index] = EditableAccountLink(name = name, url = url)
            }
            it.copy(links = updated)
        }
    }

    fun onLinkRemove(index: Int) {
        _uiState.update {
            val updated = it.links.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            it.copy(links = updated)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.id.isEmpty()) return

        val cleanedLinks = state.links
            .map { it.copy(name = it.name.trim(), url = it.url.trim()) }
            .filter { it.name.isNotEmpty() && it.url.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            repository.updateAccount(
                id = state.id,
                name = state.name,
                bio = state.bio,
                avatarUrl = state.pendingAvatarDataUrl,
                links = cleanedLinks,
            )
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(EditProfileEvent.Saved)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, saveError = error.message)
                    }
                }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
