package ph.edu.comteq.jokesclientapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ph.edu.comteq.jokesclientapi.ui.theme.JokesclientapiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JokesclientapiTheme {
                val jokesViewModel: JokesViewModel = viewModel()
                JokesApp(viewModel = jokesViewModel)
            }
        }
    }
}

@Composable
fun JokesApp(viewModel: JokesViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Joke")
            }
        }
    ) { innerPadding ->
        JokesScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )

        if (showAddDialog) {
            AddJokeDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { setup, punchline ->
                    viewModel.addJoke(setup, punchline)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun JokesScreen(viewModel: JokesViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.get_Jokes()
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is JokesUIState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No Jokes To Show")
                }
            }
            is JokesUIState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is JokesUIState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for the FAB
                ) {
                    items(state.jokes, key = { it.id ?: it.hashCode() }) { joke ->
                        JokeItem(
                            joke = joke,
                            viewModel = viewModel
                        )
                    }
                }
            }
            is JokesUIState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(modifier = Modifier.padding(16.dp), text = state.message)
                }
            }
        }
    }
}

@Composable
fun JokeItem(joke: joke, viewModel: JokesViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var jokeToEdit by remember { mutableStateOf<joke?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = joke.setup)
                Text(text = joke.punchline)
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Joke",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = { jokeToEdit = joke }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Joke"
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Joke") },
            text = { Text("Are you sure you want to delete this joke?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        joke.id?.let { viewModel.deleteJoke(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    jokeToEdit?.let { currentJoke ->
        EditJokeDialog(
            joke = currentJoke,
            onDismiss = { jokeToEdit = null },
            onConfirm = { id, setup, punchline ->
                viewModel.updateJoke(id, setup, punchline)
                jokeToEdit = null
            }
        )
    }
}


@Composable
fun AddJokeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var setup by remember { mutableStateOf("") }
    var punchline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add New Joke") },
        text = {
            Column {
                OutlinedTextField(
                    value = setup,
                    onValueChange = { setup = it },
                    label = { Text("Setup") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = punchline,
                    onValueChange = { punchline = it },
                    label = { Text("Punchline") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (setup.isNotBlank() && punchline.isNotBlank()) {
                        onConfirm(setup, punchline)
                    }
                },
                enabled = setup.isNotBlank() && punchline.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditJokeDialog(
    joke: joke,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var setup by remember { mutableStateOf(joke.setup) }
    var punchline by remember { mutableStateOf(joke.punchline) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Joke") },
        text = {
            Column {
                OutlinedTextField(
                    value = setup,
                    onValueChange = { setup = it },
                    label = { Text("Setup") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = punchline,
                    onValueChange = { punchline = it },
                    label = { Text("Punchline") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (setup.isNotBlank() && punchline.isNotBlank()) {
                        onConfirm(joke.id ?: 0, setup, punchline)
                    }
                },
                enabled = setup.isNotBlank() && punchline.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
