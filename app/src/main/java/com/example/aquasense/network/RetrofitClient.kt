package com.example.aquasense.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.google.gson.GsonBuilder

object RetrofitClient {
    private const val BASE_URL = "http://aquasense.runasp.net/api/"

    @Volatile
    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(AuthInterceptor(context.applicationContext))
                        .build()
                )
                // Сначала обрабатываем скалярные (текстовые) ответы
                .addConverterFactory(ScalarsConverterFactory.create())
                // Затем JSON-ответы с использованием Gson с включенным lenient режимом
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
                .build()
                .also { retrofit = it }
        }
    }

    fun <T> createService(context: Context, service: Class<T>): T {
        return getInstance(context).create(service)
    }
}