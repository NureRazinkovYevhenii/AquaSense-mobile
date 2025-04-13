package com.example.aquasense.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sharedPref = context.getSharedPreferences("AquaPrefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("AUTH_TOKEN", null)
        val originalRequest = chain.request()
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(newRequest)
    }
}