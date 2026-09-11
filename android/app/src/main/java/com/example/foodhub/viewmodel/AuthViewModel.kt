package com.example.foodhub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhub.CacheRepository
import com.example.foodhub.TokenManager
import com.example.foodhub.data.model.RegisterRequest
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val apiService: ApiService, private val cacheRepository: CacheRepository, private val tokenManager: TokenManager) : ViewModel(){
    private val _isLoading= MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun isEmailValid(email: String): Boolean{
        return email.contains("@")
    }

    fun isPasswordValid(pass: String): Boolean{
        return pass.length >= 7
    }

    fun isUsernameValid(user: String): Boolean{
        return user.length >= 3
    }

    fun clearError(){
        _errorMessage.value = null
    }

    fun register(email: String, username: String, pass: String, onSuccess: (String) -> Unit) {
        if(!isEmailValid(email)){
            _errorMessage.value = "L'email deve contenere la @"
            return
        }

        if(!isPasswordValid(pass)){
            _errorMessage.value = "La password deve avere almeno 7 caratteri"
            return
        }

        if(!isUsernameValid(username)){
            _errorMessage.value = "Il username deve avere almeno 3 caratteri"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = RegisterRequest(email, username, pass)
                val response = apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    val responseUser = response.body()!!.user
                    tokenManager.saveToken(token)
                    tokenManager.saveUsername(responseUser.username)
                    onSuccess(token)
                } else {
                    _errorMessage.value = "Errore durante la registrazione: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore di connessione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = LoginRequest(email, pass)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token
                    val responseUser = response.body()!!.user
                    tokenManager.saveToken(token)
                    tokenManager.saveUsername(responseUser.username)
                    try {
                        cacheRepository.syncPreferences(token)
                        cacheRepository.syncWeeklyPlan(token)
                    } catch (e: Exception) {
                        _errorMessage.value = "Errore durante la sincronizzazione delle preferenze: ${e.message}"
                    }
                    onSuccess(token)
                } else {
                    _errorMessage.value = "Credenziali non valide."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore di connessione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}