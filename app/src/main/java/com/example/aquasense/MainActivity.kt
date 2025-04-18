package com.example.aquasense


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.aquasense.models.Aquarium
import com.example.aquasense.network.ApiService
import com.example.aquasense.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }

    private fun formatLastFeedTime(lastFeedTime: String): String {
        return try {
            val dateTime = try {
                java.time.ZonedDateTime.parse(lastFeedTime)
            } catch (e: Exception) {
                java.time.LocalDateTime.parse(lastFeedTime, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(java.time.ZoneId.systemDefault())
            }
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM, HH:mm")
            dateTime.format(formatter)
        } catch (e: Exception) {
            lastFeedTime
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        var aquariums by remember { mutableStateOf<List<Aquarium>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            loadAquariums(
                onSuccess = { result ->
                    aquariums = result
                    isLoading = false
                },
                onError = { error ->
                    errorMessage = error
                    isLoading = false
                }
            )
        }

        LaunchedEffect(Unit) {
            while (true) {
                loadAquariums(
                    onSuccess = { result ->
                        aquariums = result
                        isLoading = false
                    },
                    onError = { error ->
                        errorMessage = error
                        isLoading = false
                    }
                )
                delay(5000)
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    loadAquariums(
                        onSuccess = { result ->
                            aquariums = result
                            isLoading = false
                        },
                        onError = { error ->
                            errorMessage = error
                            isLoading = false
                        }
                    )
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Scaffold(
            topBar = {
                androidx.compose.material3.TopAppBar(
                    title = { Text("aquaSense") },
                    actions = {
                        IconButton(onClick = { logout() }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                androidx.compose.material3.FloatingActionButton(onClick = { createAquarium() }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Aquarium"
                    )
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    aquariums.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No aquariums found.",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(aquariums) { aquarium ->
                                AquariumItem(
                                    aquarium = aquarium,
                                    onDelete = { deleteAquarium(aquarium.id) },
                                    onFeed = { feedFish(aquarium.id) },
                                    onToggleLight = { toggleLight(aquarium.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AquariumItem(
        aquarium: Aquarium,
        onDelete: () -> Unit,
        onFeed: () -> Unit,
        onToggleLight: () -> Unit
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clickable {
                    val intent = Intent(context, ScheduleSettingsActivity::class.java)
                    intent.putExtra("aquariumId", aquarium.id)
                    context.startActivity(intent)
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = aquarium.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = aquarium.description ?: "No description",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Temp: ${aquarium.temperature}°C",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Light: ${if (aquarium.isLightOn) "On" else "Off"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last Feed: ${formatLastFeedTime(aquarium.lastFeedTime)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onFeed) {
                            Text("Feed")
                        }
                        Button(onClick = onToggleLight) {
                            Text(if (aquarium.isLightOn) "Turn Off Light" else "Turn On Light")
                        }
                        Button(onClick = onDelete) {
                            Text("Delete")
                        }
                    }
                }
                TextButton(
                    onClick = {
                        val intent = Intent(context, HistoryActivity::class.java)
                        intent.putExtra("aquariumId", aquarium.id)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(text = "History")
                }
            }
        }
    }

    private fun loadAquariums(
        onSuccess: (List<Aquarium>) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.createService(this@MainActivity, ApiService::class.java)
                val response = apiService.getAquariums()
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else if (response.code() == 404) {
                    onSuccess(emptyList())
                } else {
                    val error = response.message() ?: "Failed to load aquariums"
                    Log.e("MainActivity", error)
                    onError(error)
                }
            } catch (e: Exception) {
                val error = "Error loading aquariums: ${e.message}"
                Log.e("MainActivity", error, e)
                onError(error)
            }
        }
    }

    private fun deleteAquarium(id: Long) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.createService(this@MainActivity, ApiService::class.java)
                val response = apiService.deleteAquarium(id)
                if (response.isSuccessful) {
                    showToast("Aquarium deleted")
                    loadAquariums(
                        onSuccess = { },
                        onError = { }
                    )
                    recreate()
                } else {
                    showToast("Failed to delete aquarium")
                }
            } catch (e: Exception) {
                showToast("Error deleting aquarium")
            }
        }
    }

    private fun feedFish(id: Long) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.createService(this@MainActivity, ApiService::class.java)
                val response = apiService.feedFish(id)
                if (response.isSuccessful) {
                    showToast("Feed command sent")
                    loadAquariums(
                        onSuccess = {},
                        onError = { }
                    )
                } else {
                    showToast("Failed to send feed command")
                }
            } catch (e: Exception) {
                showToast("Error sending feed command")
            }
        }
    }

    private fun toggleLight(id: Long) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.createService(this@MainActivity, ApiService::class.java)
                val response = apiService.turnLight(id)
                if (response.isSuccessful) {
                    showToast("Light command sent")
                    loadAquariums(
                        onSuccess = { },
                        onError = { }
                    )
                } else {
                    showToast("Failed to send light command")
                }
            } catch (e: Exception) {
                showToast("Error sending light command")
            }
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("AquaPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("AUTH_TOKEN").apply()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createAquarium() {
        val intent = Intent(this, CreateAquariumActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}