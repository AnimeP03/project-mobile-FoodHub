package com.example.foodhub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhub.TokenManager
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.local.CustomMealEntity
import com.example.foodhub.data.local.CustomRecipeDao
import com.example.foodhub.data.model.CustomRecipesUiState
import com.example.foodhub.data.model.Id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class RecipeViewModel(
    private val customRecipeDao: CustomRecipeDao,
    private val apiService : ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CustomRecipesUiState>(CustomRecipesUiState.Loading)
    val uiState: StateFlow<CustomRecipesUiState> = _uiState.asStateFlow()


    fun loadCustomRecipes() {
        viewModelScope.launch {
            _uiState.value = CustomRecipesUiState.Loading
            try {
                val token = tokenManager.getToken()  ?: return@launch
                val userId = token?.let { tokenManager.getUserIdFromToken(it) }  ?: return@launch

                var cachedRecipes = customRecipeDao.getRecipesByUserId(userId)

                if (cachedRecipes.isEmpty()) {
                    try {
                        val remoteRecipes = apiService.getRemoteRecipes("Bearer $token")

                        if (remoteRecipes.isSuccessful && remoteRecipes.body() != null) {
                            val remoteMeals = remoteRecipes.body()!!
                            remoteMeals.forEach { meal ->
                                customRecipeDao.insertRecipe(meal)
                            }
                        }

                    } catch (e: Exception) {
                        throw e
                    }
                }

                val recipes = customRecipeDao.getRecipesByUserId(userId)
                _uiState.value = CustomRecipesUiState.Success(recipes)
            } catch (e: Exception) {
                _uiState.value = CustomRecipesUiState.Error("Errore nel caricamento delle ricette")
            }
        }
    }

    fun deleteRecipe(id: Long) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken()  ?: return@launch
                apiService.deleteRemoteRecipe("Bearer $token", Id(id))
                customRecipeDao.deleteRecipeById(id)
                loadCustomRecipes()
            } catch (e: Exception) {
                _uiState.value = CustomRecipesUiState.Error("Errore nel cancellare delle ricette")
            }
        }
    }

    fun addRecipe(recipe : CustomMealEntity){
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken()  ?: return@launch
                val userId = token?.let { tokenManager.getUserIdFromToken(it) }  ?: return@launch

                val finalrecipe = recipe.copy(userid = userId)

                apiService.addRemoteRecipe("Bearer $token", finalrecipe)

                customRecipeDao.insertRecipe(finalrecipe)
                loadCustomRecipes()
            } catch (e: Exception) {
                _uiState.value = CustomRecipesUiState.Error("Errore nell'aggiunta della ricetta")
            }
        }
    }
}