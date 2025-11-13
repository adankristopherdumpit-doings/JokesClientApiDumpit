package ph.edu.comteq.jokesclientapi

// AndroidX Lifecycle Imports
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

// Kotlin Coroutines Flow Imports
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Your Project's Data Class (assuming 'Joke.kt' is in this package)
import ph.edu.comteq.jokesclientapi.Joke

// Java Exception Import (for the catch block)
import java.lang.Exception

sealed class JokesUIState {
    object Idle : JokesUIState()
    object Loading : JokesUIState()
    data class Success(val jokes: List<Joke>) : JokesUIState()
    data class Error(val message: String) : JokesUIState()
}

class JokesViewModel : ViewModel() {
    // This line assumes 'retroFitInstance' is an object or is accessible from this class
    private val api = retroFitInstance.jokeAPI

    private val _uistate = MutableStateFlow<JokesUIState>(JokesUIState.Idle)
    val uiState: StateFlow<JokesUIState> = _uistate.asStateFlow()

    fun get_Jokes() {
        // viewModelScope is now resolved
        viewModelScope.launch {
            _uistate.value = JokesUIState.Loading
            try {
                // This suspend function is now correctly called from a coroutine
                val jokes = api.getJokes()
                _uistate.value = JokesUIState.Success(jokes)
            } catch (e: Exception) {
                _uistate.value = JokesUIState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addJoke(setup: String, punchline: String) {
        viewModelScope.launch {
            try {
                val newJoke = Joke(setup = setup, punchline = punchline)
                api.addJokes(newJoke)
                // After successfully adding, refresh the jokes list
                get_Jokes()
            } catch (e: Exception) {
                _uistate.value = JokesUIState.Error(
                    e.message ?: "Unknown error while adding joke"
                )
            }
        }
    }

    // --- FIX: Moved deleteJoke inside the ViewModel ---
    fun deleteJoke(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteJokes(id)
                // After successfully deleting, refresh the jokes list
                get_Jokes()
            } catch (e: Exception) {
                _uistate.value = JokesUIState.Error(
                    e.message ?: "Unknown error while deleting joke"
                )
            }
        }
    }

    fun updateJoke(id: Int, setup: String, punchline: String) {
        viewModelScope.launch {
            try {
                val updatedJoke = Joke(id = id, setup = setup, punchline = punchline)
                api.updateJoke(id, updatedJoke)
                get_Jokes() // Refresh the list to show the updated joke
            } catch (e: Exception) {
                _uistate.value = JokesUIState.Error(
                    e.message ?: "Unknown error while updating joke"
                )
            }
        }
    }
}
