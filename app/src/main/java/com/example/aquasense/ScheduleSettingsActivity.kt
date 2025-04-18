package com.example.aquasense

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ScheduleSettingsActivity : ComponentActivity() {

    private var aquariumId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aquariumId = intent.getLongExtra("aquariumId", -1L)
        if (aquariumId == -1L) {
            finish()
            return
        }

        val sharedPref = getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)
        val storedFeedInterval = sharedPref.getInt("feed_interval", 0)
        val storedLightStart = sharedPref.getString("light_start", "") ?: ""
        val storedLightEnd = sharedPref.getString("light_end", "") ?: ""

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                ScheduleSettingsScreen(
                    initialFeedInterval = if (storedFeedInterval > 0) storedFeedInterval.toString() else "",
                    initialLightStart = storedLightStart,
                    initialLightEnd = storedLightEnd
                ) { feedIntervalHours, lightStartTime, lightEndTime ->
                    scheduleFeedWorker(feedIntervalHours, aquariumId)
                    scheduleLightWorkers(lightStartTime, lightEndTime, aquariumId)

                    sharedPref.edit().apply {
                        putInt("feed_interval", feedIntervalHours)
                        putString("light_start", lightStartTime)
                        putString("light_end", lightEndTime)
                        apply()
                    }
                    finish()
                }
            }
        }
    }

    private fun scheduleFeedWorker(feedIntervalHours: Int, aquariumId: Long) {
        val data = Data.Builder()
            .putLong("aquariumId", aquariumId)
            .build()

        val initialDelay = TimeUnit.HOURS.toMillis(feedIntervalHours.toLong())
        Log.d("ScheduleSettings", "Scheduling feed worker with initial delay: $initialDelay ms for aquarium $aquariumId")

        val workRequest = PeriodicWorkRequestBuilder<FeedFishWorker>(
            feedIntervalHours.toLong(), TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "FeedFishWorker_$aquariumId",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleLightWorkers(lightStartTime: String, lightEndTime: String, aquariumId: Long) {
        scheduleToggleLightWorker(lightStartTime, "ToggleLightStartWorker_$aquariumId", aquariumId)
        scheduleToggleLightWorker(lightEndTime, "ToggleLightEndWorker_$aquariumId", aquariumId)
    }

    private fun scheduleToggleLightWorker(targetTime: String, uniqueName: String, aquariumId: Long) {
        val data = Data.Builder()
            .putLong("aquariumId", aquariumId)
            .build()

        val initialDelay = calculateInitialDelay(targetTime)
        Log.d("ScheduleSettings", "Scheduling light worker ($uniqueName) with initial delay: $initialDelay ms for target time $targetTime")

        val workRequest = PeriodicWorkRequestBuilder<ToggleLightWorker>(
            24, TimeUnit.HOURS,
            10000, TimeUnit.MILLISECONDS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    private fun calculateInitialDelay(targetTime: String): Long {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val target = LocalTime.parse(targetTime, formatter)
            val now = LocalTime.now()
            var delay = Duration.between(now, target).toMillis()
            if (delay < 0) {
                delay += Duration.ofDays(1).toMillis()
            }
            delay
        } catch (e: Exception) {
            Log.e("ScheduleSettings", "Error calculating delay for targetTime: $targetTime", e)
            0L
        }
    }
}

@androidx.compose.runtime.Composable
fun ScheduleSettingsScreen(
    initialFeedInterval: String,
    initialLightStart: String,
    initialLightEnd: String,
    onSave: (feedIntervalHours: Int, lightStartTime: String, lightEndTime: String) -> Unit
) {
    var feedInterval by remember { mutableStateOf(initialFeedInterval) }
    var lightStartTime by remember { mutableStateOf(initialLightStart) }
    var lightEnd by remember { mutableStateOf(initialLightEnd) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Настройка расписания для кормления и света", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = feedInterval,
            onValueChange = { feedInterval = it },
            label = { Text("Интервал кормления (в часах)") }
        )
        OutlinedTextField(
            value = lightStartTime,
            onValueChange = { lightStartTime = it },
            label = { Text("Начало света (HH:mm)") }
        )
        OutlinedTextField(
            value = lightEnd,
            onValueChange = { lightEnd = it },
            label = { Text("Окончание света (HH:mm)") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val interval = feedInterval.toIntOrNull() ?: 0
            if (interval > 0 && lightStartTime.isNotBlank() && lightEnd.isNotBlank()) {
                onSave(interval, lightStartTime, lightEnd)
            } else {
                // Например, можно вывести Toast с сообщением об ошибке ввода.
            }
        }) {
            Text("Сохранить расписание")
        }
    }
}