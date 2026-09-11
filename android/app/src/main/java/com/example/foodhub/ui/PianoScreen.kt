package com.example.foodhub.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.foodhub.data.local.MealEntity
import com.example.foodhub.data.local.PreferencesEntity
import com.example.foodhub.data.model.Cousine
import com.example.foodhub.data.model.DayPlan
import com.example.foodhub.data.model.DietType
import com.example.foodhub.data.model.DietaryRegime
import com.example.foodhub.data.model.PianoUiState
import com.example.foodhub.viewmodel.PianoViewModel

@Composable
fun PianoScreen(viewModel: PianoViewModel) {


    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val selectedMeal by viewModel.selectedMeal.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            PreferencesSummarySection(prefs)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.generatePlan() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Genera Piano Settimanale")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is PianoUiState.Idle -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Clicca su genera per creare il tuo piano pasti personalizzato", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is PianoUiState.Loading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PianoUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.plan.days) { day ->
                            DayPlanItem(
                                day = day,
                                onToggle = { viewModel.toggleDayExpansion(day.dayName) },
                                onMealClick = { viewModel.selectMeal(it) },
                                onRegenerate = { viewModel.regenerateDay(day.dayName) }
                            )
                        }
                    }
                }
                is PianoUiState.Error -> {
                    Text("Errore: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedMeal != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedMeal?.let { MealDetailScreen(
                meal = it,
                onBack = { viewModel.selectMeal(null) }) }
        }
    }
}

@Composable
fun PreferencesSummarySection(prefs: PreferencesEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Le tue preferenze:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            if (prefs != null) {
                val scrollState = rememberScrollState()
                val regimes = prefs.dietaryRegime.split(",").filter { it.isNotBlank() }
                val types = prefs.dietType.split(",").filter { it.isNotBlank() }
                val cousines = prefs.cousine.split(",").filter { it.isNotBlank() && it != "[]" }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    regimes.forEach {
                        SuggestionChip(onClick = {}, label = { Text(DietaryRegime.valueOf(it).uiLabel) })
                    }
                    types.forEach {
                        SuggestionChip(onClick = {}, label = { Text(DietType.valueOf(it).uiLabel) })
                    }
                    if (cousines.isNotEmpty()) {
                        cousines.forEach {
                            SuggestionChip(onClick = {}, label = { Text(Cousine.valueOf(it).uiLabel) })
                        }
                    }else {
                        SuggestionChip(onClick = {}, label = { Text("Tutte") })
                    }
                }
                if (scrollState.maxValue > 0) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
                    ) {
                        val progress = scrollState.value.toFloat() / scrollState.maxValue

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.4f)
                                .graphicsLayer {
                                    translationX = progress * (40.dp.toPx() * 0.6f)
                                }
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                        )
                    }
                }
            } else {
                Text("Caricamento preferenze...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DayPlanItem(day: DayPlan, onToggle: () -> Unit, onMealClick: (MealEntity) -> Unit, onRegenerate: () -> Unit) {
    ElevatedCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = day.dayName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (day.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (!day.isExpanded) {
                Text(
                    text = day.meals.joinToString(", ") { it.title },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            AnimatedVisibility(visible = day.isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    day.meals.forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onMealClick(meal) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = meal.image,
                                contentDescription = meal.title,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = meal.mealType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(text = meal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                                Text(text = "⏳ ${meal.readyInMinutes} min • 🍽 ${meal.servings} porzioni", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        if (meal != day.meals.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        FilledTonalButton(onClick = onRegenerate) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rigenera giorno")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealDetailScreen(meal: MealEntity, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(meal.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Indietro") }
                }
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    AsyncImage(
                        model = meal.image,
                        contentDescription = "Immagine di ${meal.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = meal.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoChip(icon = Icons.Default.Schedule, text = "${meal.readyInMinutes} min")
                            InfoChip(icon = Icons.Default.Person, text = "${meal.servings} porzioni")
                            InfoChip(icon = Icons.Default.Star, text = "${meal.spoonacularScore.toInt()} Score")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (meal.vegetarian) TagChip("Vegetariano", MaterialTheme.colorScheme.tertiaryContainer)
                            if (meal.vegan) TagChip("Vegano", MaterialTheme.colorScheme.tertiaryContainer)
                            if (meal.glutenFree) TagChip("Senza Glutine", MaterialTheme.colorScheme.secondaryContainer)
                            if (meal.dairyFree) TagChip("Senza Lattosio", MaterialTheme.colorScheme.secondaryContainer)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Valori Nutrizionali", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val calories = meal.nutrition.nutrients.find { it.name == "Calories" }?.amount ?: 0.0
                            val protein = meal.nutrition.nutrients.find { it.name == "Protein" }?.amount ?: 0.0
                            val carbs = meal.nutrition.nutrients.find { it.name == "Carbohydrates" }?.amount ?: 0.0
                            val fat = meal.nutrition.nutrients.find { it.name == "Fat" }?.amount ?: 0.0

                            NutrientBadge("Calorie", "${calories.toInt()} kcal")
                            NutrientBadge("Pro", "${protein.toInt()}g")
                            NutrientBadge("Carb", "${carbs.toInt()}g")
                            NutrientBadge("Grassi", "${fat.toInt()}g")
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("Proteine: ${meal.nutrition.caloricBreakdown.percentProtein}%", style = MaterialTheme.typography.labelMedium)
                                Text("Grassi: ${meal.nutrition.caloricBreakdown.percentFat}%", style = MaterialTheme.typography.labelMedium)
                                Text("Carb: ${meal.nutrition.caloricBreakdown.percentCarbs}%", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    Text("Ingredienti", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(meal.extendedIngredients) { ing ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imageUrl = "https://spoonacular.com/cdn/ingredients_100x100/${ing.image}"
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(ing.originalName.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text("${ing.measures.amount} ${ing.measures.unitShort}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparazione", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val stepsList = meal.analyzedInstructions.firstOrNull()?.steps ?: emptyList()
                if (stepsList.isEmpty()) {
                    item { Text("Istruzioni non disponibili.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) }
                } else {
                    items(stepsList) { step ->
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.number}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = step.step, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TagChip(text: String, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NutrientBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit) {
    CenterAlignedTopAppBar(title = title, navigationIcon = navigationIcon)
}
