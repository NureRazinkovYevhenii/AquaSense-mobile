package com.example.aquasense

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aquasense.network.ApiService
import com.example.aquasense.network.RetrofitClient

class ToggleLightWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val aquariumId = inputData.getLong("aquariumId", -1L)
        if (aquariumId == -1L) return Result.failure()

        Log.d("ToggleLightWorker", "Executing light toggle for aquarium $aquariumId at time: ${System.currentTimeMillis()}")
        return try {
            val apiService = RetrofitClient.createService(applicationContext, ApiService::class.java)
            val response = apiService.turnLight(aquariumId)
            if (response.isSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("ToggleLightWorker", "Error executing light toggle", e)
            Result.retry()
        }
    }
}