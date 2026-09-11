package com.example.foodhub.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodhub.CacheRepository
import com.example.foodhub.TokenManager
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.local.MealEntity
import com.example.foodhub.data.local.PreferencesDao
import com.example.foodhub.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.foodhub.data.local.PreferencesEntity
import com.example.foodhub.data.local.WeeklyPlanDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class PianoViewModel(
    private val preferencesDao: PreferencesDao,
    private val tokenManager: TokenManager,
    private val weeklyPlanDao: WeeklyPlanDao,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PianoUiState>(PianoUiState.Idle)
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    private val _userPreferences = MutableStateFlow<PreferencesEntity?>(null)
    val userPreferences: StateFlow<PreferencesEntity?> = _userPreferences.asStateFlow()


    private val _selectedMeal = MutableStateFlow<MealEntity?>(null)
    val selectedMeal: StateFlow<MealEntity?> = _selectedMeal.asStateFlow()

    init {
        loadPreferences()
        loadWeeklyPlan()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            val token = tokenManager.getToken()
            if (token != null) {
                val userId = tokenManager.getUserIdFromToken(token)
                if (userId != null) {
                    preferencesDao.getPreferencesFlow(userId).collect { updatedPrefs ->
                        _userPreferences.value = updatedPrefs
                    }
                }
            }
        }
    }

    private fun loadWeeklyPlan(){
        viewModelScope.launch {
            val token = tokenManager.getToken() ?: return@launch
            val userId = tokenManager.getUserIdFromToken(token) ?: return@launch
            _uiState.value = PianoUiState.Loading
            delay(500)
            weeklyPlanDao.getAllPlannedMeals(userId).collect { mealsList ->

                if (mealsList.isEmpty()) {
                    _uiState.value = PianoUiState.Idle
                } else {
                    val mealsByDay: Map<String, List<MealEntity>> =
                        mealsList.groupBy { it.dayOfWeek }
                    val daysOfWeek = listOf(
                        "Lunedì",
                        "Martedì",
                        "Mercoledì",
                        "Giovedì",
                        "Venerdì",
                        "Sabato",
                        "Domenica"
                    )
                    val dayPlans = daysOfWeek.map { dayName ->
                        DayPlan(
                            dayName = dayName,
                            meals = mealsByDay[dayName] ?: emptyList(),
                            isExpanded = false
                        )
                    }

                    val weeklyPlan = WeeklyPlan(
                        id = "week_1",
                        days = dayPlans
                    )

                    _uiState.value = PianoUiState.Success(weeklyPlan)
                }
            }
        }
    }

    fun generatePlan() {
        viewModelScope.launch {
            _uiState.value = PianoUiState.Loading

            try {
                cacheRepository.generateWeek()
            } catch (e: Exception) {
                _uiState.value = PianoUiState.Error("Errore durante la generazione del piano: ${e.message}")
            }

        }
    }

    fun regenerateDay(dayName: String) {
        viewModelScope.launch {
            _uiState.value = PianoUiState.Loading
            try {
                cacheRepository.regenerateDay(dayName)
            } catch (e: Exception) {
                _uiState.value = PianoUiState.Error("Errore durante la rigenerazione del giorno: ${e.message}")
            }
        }
    }

    fun toggleDayExpansion(dayName: String) {
        val currentState = _uiState.value
        if (currentState is PianoUiState.Success) {
            val updatedDays = currentState.plan.days.map {
                if (it.dayName == dayName) it.copy(isExpanded = !it.isExpanded) else it
            }
            _uiState.value = PianoUiState.Success(currentState.plan.copy(days = updatedDays))
        }
    }

    fun selectMeal(meal: MealEntity?) {
        _selectedMeal.value = meal
    }
}
