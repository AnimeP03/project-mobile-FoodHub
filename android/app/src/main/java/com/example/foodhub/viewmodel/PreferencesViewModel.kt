package com.example.foodhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhub.TokenManager
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.local.CachedMealDao
import com.example.foodhub.data.local.PreferencesDao
import com.example.foodhub.data.local.PreferencesEntity
import com.example.foodhub.data.model.Allergies
import com.example.foodhub.data.model.Cousine
import com.example.foodhub.data.model.DietType
import com.example.foodhub.data.model.DietaryRegime
import com.example.foodhub.data.model.TimePreparation
import com.example.foodhub.data.model.UserPreferencesRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreferencesViewModel(private val apiService: ApiService,private val cachedMealDao: CachedMealDao, private val preferencesDao: PreferencesDao, private val tokenManager: TokenManager) : ViewModel() {

    private val _isLoading= MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError(){
        _errorMessage.value = null
    }

    fun resetState() {
        _currentStep.value = 1
        _regimeSelected.value = ""
        _allergieSelected.value = emptySet()
        _typeSelected.value = emptySet()
        _timeSelected.value = ""
        _cousineSelected.value = emptySet()
        _errorMessage.value = null
        _isLoading.value = false
    }


    private val _maxStep = MutableStateFlow(5)
    val maxStep: StateFlow<Int> =  _maxStep.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _regimeSelected = MutableStateFlow<String>("")
    val regimeSelected: StateFlow<String?> = _regimeSelected.asStateFlow()

    private val _allergieSelected = MutableStateFlow<Set<String>>(emptySet())
    val allergieSelected: StateFlow<Set<String>> = _allergieSelected.asStateFlow()

    private val _typeSelected = MutableStateFlow<Set<String>>(emptySet())
    val typeSelected: StateFlow<Set<String>> = _typeSelected.asStateFlow()

    private val _timeSelected = MutableStateFlow<String>("")
    val timeSelected: StateFlow<String?> = _timeSelected.asStateFlow()

    private val _cousineSelected = MutableStateFlow<Set<String>>(emptySet())
    val cousineSelected: StateFlow<Set<String>> = _cousineSelected.asStateFlow()




    fun setRegime(regime: DietaryRegime) {
        _regimeSelected.value = regime.name
    }

    fun setTime(time: TimePreparation) {
        _timeSelected.value = time.name
    }


    fun toggleAllergia(allergia: Allergies) {
        _allergieSelected.update { currentSelection ->
            if (currentSelection.contains(allergia.name)) {
                currentSelection - allergia.name
            }
            else {
                currentSelection + allergia.name
            }
        }
    }

    fun toggleCuisine(cousine: Cousine){
        _cousineSelected.update { currentSelection ->
            if (currentSelection.contains(cousine.name)) {
                currentSelection - cousine.name
            }
            else {
                currentSelection + cousine.name
            }
        }
    }


    fun toggleType(type: DietType) {
        _typeSelected.update { currentSelection ->
            when (type) {
                DietType.BILANCIATA, DietType.NESSUNA -> {
                    if (currentSelection.contains(type.name)) {
                        emptySet()
                    } else {
                        setOf(type.name)
                    }
                }


                else -> {
                    val exclusive = currentSelection - DietType.BILANCIATA.name - DietType.NESSUNA.name

                    if (exclusive.contains(type.name)) {
                        exclusive - type.name
                    } else {
                        exclusive + type.name
                    }
                }
            }
        }
    }


    fun nextStep(onSuccess: () -> Unit = {}) {
        if (_currentStep.value < _maxStep.value) {
            _currentStep.value += 1
        } else {
            savePreferences(onSuccess)
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value -= 1
        }
    }




    private fun savePreferences(onSuccess: () -> Unit) {
        val token = tokenManager.getToken()

        if (token == null) {
            _errorMessage.value = "Sessione scaduta"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try{
                val apiRequest = UserPreferencesRequest(
                    dietaryRegime = _regimeSelected.value,
                    allergies = _allergieSelected.value.toList(),
                    dietType = _typeSelected.value.toList(),
                    timePreparation = _timeSelected.value,
                    cousine = _cousineSelected.value.toList()
                )
                val response = apiService.savePreferences("Bearer $token", apiRequest)

                if (response.isSuccessful){
                    val currentUserId = tokenManager.getUserIdFromToken(token) ?: "1"
                    val roomEntity = PreferencesEntity(
                        userid = currentUserId,
                        dietaryRegime = _regimeSelected.value,
                        allergies = _allergieSelected.value.joinToString(","),
                        dietType = _typeSelected.value.joinToString(","),
                        timePreparation = _timeSelected.value,
                        cousine = _cousineSelected.value.joinToString(",")
                    )

                    withContext(Dispatchers.IO){
                        preferencesDao.savePreferences(roomEntity)
                        cachedMealDao.clearAllCache()
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
                else{
                    val statusCode = response.code()
                    val errorDetail = response.errorBody()?.string() ?: "Nessun dettaglio dal server"
                    _errorMessage.value = "Errore $statusCode: $errorDetail"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Errore di connessione: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}