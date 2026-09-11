package com.example.foodhub.ui

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodhub.ApiQuotaManager
import com.example.foodhub.CacheRepository
import com.example.foodhub.TokenManager
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.local.PreferencesDao
import com.example.foodhub.data.local.WeeklyPlanDao
import com.example.foodhub.data.local.AppDatabase
import com.example.foodhub.data.local.CustomRecipeDao
import com.example.foodhub.data.local.PantryDao
import com.example.foodhub.viewmodel.PantryViewModel
import com.example.foodhub.viewmodel.PianoViewModel
import com.example.foodhub.viewmodel.PreferencesViewModel
import com.example.foodhub.viewmodel.RecipeViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    object Home : BottomNavItem("home_content", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Dispensa : BottomNavItem("dispensa", "Dispensa", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List)
    object Piano : BottomNavItem("piano", "Piano", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    object Ricette : BottomNavItem("ricette", "Ricette", Icons.Filled.Star, Icons.Outlined.Star)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBar(
    apiService: ApiService,
    preferencesDao: PreferencesDao,
    tokenManager: TokenManager,
    weeklyPlanDao: WeeklyPlanDao,
    pantryDao: PantryDao,
    cacheRepository: CacheRepository,
    preferencesViewModel: PreferencesViewModel,
    apiQuotaManager: ApiQuotaManager,
    customRecipeDao: CustomRecipeDao,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    val pianoViewModel: PianoViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PianoViewModel( preferencesDao, tokenManager, weeklyPlanDao, cacheRepository) as T
            }
        }
    )

    val pantryViewModel: PantryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PantryViewModel(pantryDao, weeklyPlanDao, tokenManager, apiService, apiQuotaManager) as T
            }
        }
    )

    val recipeViewModel: RecipeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecipeViewModel(customRecipeDao, apiService, tokenManager) as T
            }
        }
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                tonalElevation = 0.dp) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Dispensa,
                    BottomNavItem.Piano,
                    BottomNavItem.Ricette
                )

                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        modifier = Modifier
                            .padding(horizontal = 0.1.dp)
                            .clip(
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                )
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            ),
                        icon = { Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        ) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,

                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { 
                HomeScreen(
                    onLogout = onLogout,
                    tokenManager = tokenManager,
                    pianoViewModel = pianoViewModel,
                    pantryViewModel = pantryViewModel,
                    preferencesViewModel = preferencesViewModel,
                    preferencesDao = preferencesDao,
                    apiQuotaManager = apiQuotaManager
                )
            }
            composable(BottomNavItem.Dispensa.route) {
                PantryScreen(viewModel = pantryViewModel)
            }
            composable(BottomNavItem.Piano.route) {
                PianoScreen(viewModel = pianoViewModel)
            }
            composable(BottomNavItem.Ricette.route) {
                RecipeScreen(viewModel = recipeViewModel)
            }
        }
    }
}

