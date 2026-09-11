package com.example.foodhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.api.RetrofitClient
import com.example.foodhub.data.local.AppDatabase
import com.example.foodhub.data.local.CachedMealDao
import com.example.foodhub.data.local.CustomRecipeDao
import com.example.foodhub.data.local.PantryDao
import com.example.foodhub.data.local.PreferencesDao
import com.example.foodhub.data.local.WeeklyPlanDao
import com.example.foodhub.ui.LoginScreen
import com.example.foodhub.ui.NavBar
import com.example.foodhub.ui.PreferencesScreen
import com.example.foodhub.ui.RegisterScreen
import com.example.foodhub.ui.theme.FoodHubTheme
import com.example.foodhub.viewmodel.AuthViewModel
import com.example.foodhub.viewmodel.PreferencesViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // per il jwt (token)
        val tokenManager = TokenManager(this)

        // retrofit per le api
        val apiService = RetrofitClient.apiService

        val database = AppDatabase.getDatabase(this)
        val preferencesDao = database.preferencesDao()
        val cachedMealDao = database.cachedMealDao()
        val weeklyPlanDao = database.weeklyPlanDao()
        val pantryDao = database.pantryDao()
        val customRecipeDao = database.customRecipeDao()
        val apiQuota = ApiQuotaManager(this)
        val cacheRepository = CacheRepository(apiService, cachedMealDao, weeklyPlanDao, preferencesDao, pantryDao, tokenManager, apiQuota)


        // gia loggato
        val tokenSaved = tokenManager.getToken()

        val firstScreen = if (tokenSaved != null) "home" else "login"


        setContent {
            FoodHubTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        apiService = apiService,
                        tokenManager = tokenManager,
                        startDestination = firstScreen,
                        preferencesDao = preferencesDao,
                        cachedMealDao = cachedMealDao,
                        weeklyPlanDao = weeklyPlanDao,
                        pantryDao = pantryDao,
                        cacheRepository = cacheRepository,
                        apiQuotaManager = apiQuota,
                        customRecipedao = customRecipeDao
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    apiService: ApiService,
    tokenManager: TokenManager,
    startDestination: String,
    preferencesDao: PreferencesDao,
    cachedMealDao: CachedMealDao,
    weeklyPlanDao: WeeklyPlanDao,
    pantryDao: PantryDao,
    cacheRepository: CacheRepository,
    apiQuotaManager: ApiQuotaManager,
    customRecipedao: CustomRecipeDao
) {

    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(apiService, cacheRepository, tokenManager) as T
            }
        }
    )

    val preferencesViewModel: PreferencesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PreferencesViewModel(apiService, cachedMealDao ,preferencesDao,tokenManager) as T
            }
        }
    )

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
    ) {


        // --- ROTTA LOGIN ---
        composable("login") {
            LaunchedEffect(Unit) {
                authViewModel.clearError()
            }
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { token ->
                    tokenManager.saveToken(token)
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // --- ROTTA REGISTRAZIONE ---
        composable("register") {
            LaunchedEffect(Unit) {
                authViewModel.clearError()
            }
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = { token ->
                    tokenManager.saveToken(token)
                    navController.navigate("preferences") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- ROTTA PREFERENZE  ---
        composable("preferences") {

            LaunchedEffect(Unit) {
                preferencesViewModel.resetState()
            }
            PreferencesScreen(
                viewModel = preferencesViewModel,
                onPreferencesSaved = {
                    navController.navigate("home") {
                        popUpTo("preferences") { inclusive = true }
                    }
                }
            )
        }

        // --- ROTTA HOME ---
        composable("home") {
            NavBar(
                apiService = apiService,
                preferencesDao = preferencesDao,
                tokenManager = tokenManager,
                weeklyPlanDao = weeklyPlanDao,
                pantryDao = pantryDao,
                cacheRepository = cacheRepository,
                preferencesViewModel = preferencesViewModel,
                apiQuotaManager = apiQuotaManager,
                customRecipeDao = customRecipedao,
                onLogout = {
                    scope.launch {
                        val token = tokenManager.getToken()
                        if (token != null) {
                            val userId = tokenManager.getUserIdFromToken(token)
                            if (userId != null) {
                                weeklyPlanDao.clearEntireWeek(userId)
                            }
                            tokenManager.clearToken()
                            tokenManager.clearUsername()
                        }
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}