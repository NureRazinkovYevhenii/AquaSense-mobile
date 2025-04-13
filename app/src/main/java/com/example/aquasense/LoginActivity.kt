package com.example.aquasense

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.aquasense.network.ApiService
import com.example.aquasense.network.AuthRequest
import com.example.aquasense.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверка сохраненного токена
        val sharedPref = getSharedPreferences("AquaPrefs", Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("AUTH_TOKEN", null)
        if (savedToken != null) {
            navigateToMainScreen()
            return
        }

        setContent {
            LoginScreen(
                onLogin = { username, password ->
                    handleAuth(username, password, isLogin = true)
                },
                onSignUp = { username, password ->
                    handleAuth(username, password, isLogin = false)
                }
            )
        }
    }

    private fun handleAuth(username: String, password: String, isLogin: Boolean) {
        lifecycleScope.launch {
            try {
                // Получаем ApiService с настроенным AuthInterceptor
                val apiService = RetrofitClient.createService(this@LoginActivity, ApiService::class.java)
                val response = if (isLogin) {
                    Log.d("LoginActivity", "Отправка запроса на логин с username: $username")
                    apiService.login(AuthRequest(username, password))
                } else {
                    Log.d("LoginActivity", "Отправка запроса на регистрацию с username: $username")
                    apiService.register(AuthRequest(username, password))
                }

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    Log.d("LoginActivity", "Успешный ответ: $token")
                    val sharedPref = getSharedPreferences("AquaPrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("AUTH_TOKEN", token)
                        apply()
                    }
                    showToast("Успешно!")
                    navigateToMainScreen()
                } else {
                    Log.e("LoginActivity", "Ошибка авторизации: ${response.code()} - ${response.message()}")
                    showToast("Ошибка авторизации")
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Ошибка сети: ${e.message}", e)
                showToast("Ошибка сети")
            }
        }
    }

    private fun navigateToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (username: String, password: String) -> Unit,
    onSignUp: (username: String, password: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isLogin) {
                        onLogin(username, password)
                    } else {
                        onSignUp(username, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLogin) "Login" else "Sign Up")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { isLogin = !isLogin }) {
                Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login")
            }
        }
    }
}