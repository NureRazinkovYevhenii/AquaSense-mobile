package com.example.aquasense


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType.Companion.Text
import androidx.compose.ui.unit.dp
import com.example.aquasense.network.ApiService
import com.example.aquasense.network.Measurement
import com.example.aquasense.network.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aquariumId = intent.getLongExtra("aquariumId", -1)

        setContent {
            MaterialTheme {
                HistoryScreen(aquariumId = aquariumId)
            }
        }
    }
}

@Composable
fun HistoryScreen(aquariumId: Long) {
    var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    var diffMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiService = RetrofitClient.createService(context, ApiService::class.java)

    LaunchedEffect(aquariumId) {
        try {

            val response = apiService.getAquariumMeasurements(aquariumId)
            if (response.isSuccessful && response.body() != null) {
                measurements = response.body()!!
                diffMessages = computeDifferences(measurements)
            } else {
                errorMessage = "Error loading history"
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (errorMessage.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(diffMessages) { message ->
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}



fun computeDifferences(measurements: List<Measurement>): List<String> {
    if (measurements.isEmpty()) return emptyList()

    // Сортируем по времени (предполагается, что timestamp в формате ISO_LOCAL_DATE_TIME)
    val sorted = measurements.sortedBy { it.timestamp }

    val differences = mutableListOf<String>()
    // Для первой записи можно показать исходное состояние
    differences.add("${formatTimestamp(sorted.first().timestamp)} Initial state: " +
            "Temperature ${sorted.first().temperature}°C, " +
            "Light: ${if (sorted.first().lightStatus) "is on" else "is off"}")

    for (i in 1 until sorted.size) {
        val prev = sorted[i - 1]
        val cur = sorted[i]
        val changes = mutableListOf<String>()
        if (cur.temperature != prev.temperature) {
            changes.add("Temperature ${cur.temperature}°C")
        }
        // Сравнение времени кормления (если отличается — то было кормление)
        if (cur.lastFeedTime != prev.lastFeedTime) {
            changes.add("Feed")
        }
        if (cur.lightStatus != prev.lightStatus) {
            changes.add(if (cur.lightStatus) "Light is on" else "Light is off")
        }
        if (changes.isNotEmpty()) {
            differences.add("${formatTimestamp(cur.timestamp)} ${changes.joinToString(", ")}")
        }
    }
    return differences
}

fun formatTimestamp(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        isoString
    }
}