package pub.hackers.android.ui.screens.postdetail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import pub.hackers.android.data.repository.HackersPubRepository
import javax.inject.Inject

@HiltViewModel
class PostByUrlResolverViewModel @Inject constructor(
    private val repository: HackersPubRepository,
) : ViewModel() {

    suspend fun resolve(url: String): String? {
        return repository.resolvePostIdByUrl(url).getOrNull()
    }
}
