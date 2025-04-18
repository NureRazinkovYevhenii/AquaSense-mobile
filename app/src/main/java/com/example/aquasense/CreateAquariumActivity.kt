package com.example.aquasense

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.aquasense.network.ApiService
import com.example.aquasense.network.RetrofitClient
import kotlinx.coroutines.launch

data class AquariumRequest(
    val name: String,
    val description: String?
)

class CreateAquariumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var loading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf("") }

            CreateAquariumScreen(
                loading = loading,
                errorMessage = errorMessage,
                onCreate = { name, description ->
                    lifecycleScope.launch {
                        loading = true
                        errorMessage = ""
                        try {
                            val request = AquariumRequest(name, description.takeIf { it.isNotBlank() })

                            val apiService = RetrofitClient.createService(this@CreateAquariumActivity, ApiService::class.java)
                            val response = apiService.createAquarium(request)
                            if (response.isSuccessful && response.body() != null) {
                                Toast.makeText(
                                    this@CreateAquariumActivity,
                                    "Aquarium created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            } else {
                                val error = response.errorBody()?.string() ?: response.message()
                                errorMessage = "Failed to create aquarium: $error"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.localizedMessage}"
                        } finally {
                            loading = false
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAquariumScreen(
    loading: Boolean,
    errorMessage: String,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create Aquarium") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            // Можно добавить уведомление о некорректном вводе
                        } else {
                            onCreate(name, description)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    Text(text = if (loading) "Creating..." else "Create")
                }
            }
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}