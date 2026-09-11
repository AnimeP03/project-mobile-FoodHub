package com.example.foodhub.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.foodhub.ApiQuotaManager
import com.example.foodhub.TokenManager
import com.example.foodhub.data.local.PreferencesDao
import com.example.foodhub.data.local.PreferencesEntity
import com.example.foodhub.data.model.Allergies
import com.example.foodhub.data.model.Cousine
import com.example.foodhub.data.model.DietType
import com.example.foodhub.data.model.DietaryRegime
import com.example.foodhub.data.model.IngredientStatus
import com.example.foodhub.data.model.PantryUiState
import com.example.foodhub.data.model.PianoUiState
import com.example.foodhub.data.model.TimePreparation
import com.example.foodhub.ui.theme.ArancioneVibrante
import com.example.foodhub.viewmodel.PantryViewModel
import com.example.foodhub.viewmodel.PianoViewModel
import com.example.foodhub.viewmodel.PreferencesViewModel
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    tokenManager: TokenManager,
    pianoViewModel: PianoViewModel,
    pantryViewModel: PantryViewModel,
    preferencesViewModel: PreferencesViewModel,
    preferencesDao: PreferencesDao,
    apiQuotaManager: ApiQuotaManager
) {
    val username = tokenManager.getUsername() ?: "Chef"
    val token = tokenManager.getToken() ?: ""
    val userId = tokenManager.getUserIdFromToken(token)
    val pianoUiState by pianoViewModel.uiState.collectAsStateWithLifecycle()
    val pantryUiState by pantryViewModel.uiState.collectAsStateWithLifecycle()


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { HomeTopBar(username = username, onLogout = onLogout, userId, preferencesViewModel, preferencesDao) }
        item { ApiQuota(apiQuotaManager) }
        item { StatoGiornalieroSection(pianoUiState = pianoUiState) }
        item { AlertInventarioSection(pantryUiState = pantryUiState) }
    }
}


@Composable
fun ApiQuota(
    quotaManager: ApiQuotaManager,
    modifier: Modifier = Modifier
) {
    val requestPoints = remember { quotaManager.getQuotaRequest() }
    val usedPoints = remember { quotaManager.getQuotaUsed() }
    val leftPoints = remember { quotaManager.getQuotaLeft() }

    ApiQuotaContent(
        requestPoints = requestPoints,
        usedPoints = usedPoints,
        leftPoints = leftPoints,
        modifier = modifier
    )
}

@Composable
fun ApiQuotaContent(
    requestPoints: Float,
    usedPoints: Float,
    leftPoints: Float,
    modifier: Modifier = Modifier
) {val totalPoints = usedPoints + if (leftPoints > 0) leftPoints else 0f
    val progress = if (totalPoints > 0f) usedPoints / totalPoints else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "QuotaProgressAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Stato Quota API (Spoonacular)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Utilizzato",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.1f".format(progress * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuotaIndicator(
                    label = "Ultima chiamata",
                    value = if (requestPoints >= 0) "${requestPoints}" else "N/D",
                    valueColor = MaterialTheme.colorScheme.secondary
                )

                QuotaIndicator(
                    label = "Usati oggi",
                    value = "${usedPoints}",
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QuotaIndicator(
                    label = "Rimasti",
                    value = if (leftPoints >= 0) "${leftPoints.toInt()}" else "N/D",
                    valueColor = if (leftPoints < 20f && leftPoints >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RowScope.QuotaIndicator(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(username: String, onLogout: () -> Unit , userId: String?, preferencesViewModel: PreferencesViewModel, preferencesDao: PreferencesDao) {
    var showPreferences by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profilo",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Ciao, $username!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { showPreferences = true }) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Impostazioni",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = "Logout",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (showPreferences) {
            Dialog(
                onDismissRequest = {
                    showPreferences = false
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .fillMaxHeight(1f),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 6.dp
                ) {
                    PreferencesModalManager(
                        preferencesDao = preferencesDao,
                        viewModel = preferencesViewModel,
                        userId = userId,
                        onClose = { showPreferences = false }
                    )
                }
            }
        }
    }
}

@Composable
fun StatoGiornalieroSection(pianoUiState: PianoUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cosa mangiare oggi 🍽️",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (pianoUiState) {
                is PianoUiState.Success -> {
                    val todayMeals = pianoUiState.plan.days.firstOrNull()?.meals ?: emptyList()
                    val breakfastMeal = todayMeals.find { it.mealType.lowercase() == "colazione" }
                    val lunchMeal = todayMeals.find { it.mealType.lowercase() == "pranzo" }
                    val dinnerMeal = todayMeals.find { it.mealType.lowercase() == "cena" }

                    if (breakfastMeal != null) {
                        Text(
                            text = "Colazione: ${breakfastMeal.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Collazione: Non pianificata",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (lunchMeal != null) {
                        Text(
                            text = "Pranzo: ${lunchMeal.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Pranzo: Non pianificato",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (dinnerMeal != null) {
                        Text(
                            text = "Cena: ${dinnerMeal.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Cena: Non pianificata",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Riepilogo Nutrienti Stimati",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    var totalCalories = 0.0
                    var totalProteins = 0.0
                    var totalCarbs = 0.0
                    var totalFats = 0.0

                    todayMeals.forEach { meal ->
                        meal.nutrition.nutrients.forEach { nutrient ->
                            when {
                                nutrient.name.contains("Calories", ignoreCase = true) -> totalCalories += nutrient.amount
                                nutrient.name.contains("Protein", ignoreCase = true) -> totalProteins += nutrient.amount
                                nutrient.name.contains("Carbohydrates", ignoreCase = true) -> totalCarbs += nutrient.amount
                                nutrient.name.contains("Fat", ignoreCase = true) && !nutrient.name.contains("Saturated", ignoreCase = true) -> totalFats += nutrient.amount
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NutrientItem("Cal", totalCalories.toInt().toString())
                        NutrientItem("Pro", "${totalProteins.toInt()}g")
                        NutrientItem("Carb", "${totalCarbs.toInt()}g")
                        NutrientItem("Fat", "${totalFats.toInt()}g")
                    }
                }
                is PianoUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is PianoUiState.Error -> {
                    Text(
                        text = "Errore nel caricamento del piano: ${pianoUiState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is PianoUiState.Idle -> {
                    Text(
                        text = "Piano non ancora generato. Accedi alla sezione Piano per crearlo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NutrientItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AlertInventarioSection(pantryUiState: PantryUiState) {
    when (pantryUiState) {
        is PantryUiState.Success -> {
            val missingIngredients = pantryUiState.items.filter { it.status == IngredientStatus.TO_BUY }

            if (missingIngredients.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Attenzione",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "In Esaurimento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Questi ingredienti ti servono per le ricette di questa settimana:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val limit = 3
                        val displayIn = missingIngredients.take(limit)
                        val remainingCount = missingIngredients.size - limit

                        displayIn.forEach { ingredient ->
                            MissingIngredientItem(ingredient.name)
                        }

                        if (remainingCount > 0) {
                            Text(
                                text = "+ altri $remainingCount ingredienti",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        is PantryUiState.Loading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Caricamento dispensa...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        is PantryUiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Errore nel caricamento della dispensa: ${pantryUiState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        is PantryUiState.Idle -> {
        }
    }
}

@Composable
fun MissingIngredientItem(name: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun PreferencesModalManager(
    preferencesDao: PreferencesDao,
    viewModel: PreferencesViewModel,
    userId: String?,
    onClose: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    if (isEditing) {
        PreferencesScreen(
            viewModel = viewModel,
            onPreferencesSaved = {
                isEditing = false
            }
        )
    } else {
        CurrentPreferencesSummary(
            preferencesDao = preferencesDao,
            userId = userId,
            onEditClick = { isEditing = true },
            onClose = onClose
        )
    }
}

@Composable
fun CurrentPreferencesSummary(
    preferencesDao: PreferencesDao,
    userId: String?,
    onEditClick: () -> Unit,
    onClose: () -> Unit
) {
    var userPreferences by remember { mutableStateOf<PreferencesEntity?>(null) }

    LaunchedEffect(userId) {
        if (userId != null) {
            userPreferences = preferencesDao.getPreferences(userId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Le tue Preferenze",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Chiudi",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            userPreferences?.let { prefs ->
                SummaryItem(title = "Regime Base", value = DietaryRegime.valueOf(prefs.dietaryRegime).uiLabel)

                SummaryItem(
                    title = "Allergie/Intolleranze",
                    value = if (prefs.allergies.isEmpty()) {
                        "Nessuno"
                    } else {
                        prefs.allergies
                            .split(",")
                            .filter { it.isNotBlank() }
                            .map { Allergies.valueOf(it.trim()).uiLabel }
                            .joinToString(", ")
                    }
                )

                SummaryItem(
                    title = "Obiettivo Dieta",
                    value = if (prefs.dietType.isEmpty()) {
                        "Nessuno"
                    } else {
                        prefs.dietType
                            .split(",")
                            .filter { it.isNotBlank() }
                            .map { DietType.valueOf(it.trim()).uiLabel }
                            .joinToString(", ")
                    }
                )

                SummaryItem(
                    title = "Cucine Preferite",
                    value = if (prefs.cousine.isEmpty()) {
                        "Tutte"
                    } else {
                        prefs.cousine
                            .split(",")
                            .filter { it.isNotBlank() }
                            .map { Cousine.valueOf(it.trim()).uiLabel }
                            .joinToString(", ")
                    }
                )

                SummaryItem(
                    title = "Tempo a disposizione",
                    value = TimePreparation.valueOf(prefs.timePreparation).uiLabel
                )
            }

            Button(
                onClick = onEditClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modifica Preferenze", fontSize = 18.sp)
            }

        }
    }
}

@Composable
fun SummaryItem(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
