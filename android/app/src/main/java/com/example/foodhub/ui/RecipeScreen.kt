package com.example.foodhub.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.foodhub.data.local.CustomMealEntity
import com.example.foodhub.data.model.CustomRecipesUiState
import com.example.foodhub.data.model.Ingredient
import com.example.foodhub.data.model.Instruction
import com.example.foodhub.data.model.Metric
import com.example.foodhub.data.model.Step
import com.example.foodhub.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel
){
    LaunchedEffect(Unit) {
        viewModel.loadCustomRecipes()
    }


    var showAddRecipeDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Le Mie Ricette", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddRecipeDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crea Nuova Ricetta")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showAddRecipeDialog) {
                AddRecipeDialog(
                    onClose = { showAddRecipeDialog = false },
                    onAddRecipe = { recipe ->
                        viewModel.addRecipe(recipe)
                        showAddRecipeDialog = false
                    }
                )
            }
            when (val state = uiState) {
                is CustomRecipesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CustomRecipesUiState.Error -> Text("Errore: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is CustomRecipesUiState.Success -> {
                    val recipes = state.recipes
                    if (recipes.isEmpty()) {
                        Text("Nessuna ricetta personalizzata." + "", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                        ) {
                            items(
                                items = recipes,
                                key = { meal -> meal.id }
                            ) { meal ->
                                SwipeableRecipeItem(
                                    meal = meal,
                                    onDelete = { id -> viewModel.deleteRecipe(id) }
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRecipeItem(
    meal: CustomMealEntity,
    onDelete: (Long) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete(meal.id)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color.Transparent
                }, label = "swipeColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            ExpandableCustomRecipeCard(meal = meal)
        }
    )
}

@Composable
fun ExpandableCustomRecipeCard(meal: CustomMealEntity) {

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val imageUrl = meal.image ?: "https://via.placeholder.com/150?text=No+Image"
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Immagine di ${meal.title}",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = "Tempo", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${meal.readyInMinutes} min", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Porzioni", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${meal.servings} porz.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Riduci" else "Espandi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                meal.nutrition?.let { nutrition ->
                    Text("Valori Nutrizionali", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val calories = nutrition.nutrients.find { it.name == "Calories" }?.amount ?: 0.0
                    val protein = nutrition.nutrients.find { it.name == "Protein" }?.amount ?: 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NutrientBadge2("Calorie", "${calories.toInt()} kcal")
                        NutrientBadge2("Proteine", "${protein.toInt()}g")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (meal.extendedIngredients.isNotEmpty()) {
                    Text("Ingredienti", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    meal.extendedIngredients.forEach { ing ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("• ", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${ing.originalName.replaceFirstChar { it.uppercase() }} - ${ing.measures.amount} ${ing.measures.unitShort}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val steps = meal.analyzedInstructions.firstOrNull()?.steps ?: emptyList()
                if (steps.isNotEmpty()) {
                    Text("Preparazione", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    steps.forEach { step ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("${step.number}. ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = step.step, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                        }
                    }
                } else if (meal.summary.isNotEmpty()) {
                    Text("Descrizione", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = meal.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun NutrientBadge2(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeDialog(
    onClose: () -> Unit,
    onAddRecipe: (CustomMealEntity) -> Unit
) {

    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var readyInMinutes by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("") }
    var dishType by remember { mutableStateOf("") }

    val ingredients = remember { mutableStateListOf<Ingredient>() }
    val steps = remember { mutableStateListOf<Step>() }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
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
                        text = "Aggiungere Nuova Ricetta",
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
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = readyInMinutes, onValueChange = { readyInMinutes = it }, label = { Text("Minuti") }, modifier = Modifier.weight(1f),keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(value = servings, onValueChange = { servings = it }, label = { Text("Porzioni") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }

                    OutlinedTextField(value = dishType, onValueChange = { dishType = it }, label = { Text("Tipo Piatto (Breakfast o Main Course") }, modifier = Modifier.fillMaxWidth())

                    Text("Ingredienti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ingredients.forEachIndexed { index, ingredient ->
                        IngredientInputRow(
                            ingredient = ingredient,
                            onUpdate = { newIngredient -> ingredients[index] = newIngredient },
                            onDelete = { ingredients.removeAt(index) }
                        )
                    }
                    Button(onClick = { ingredients.add(Ingredient(null, null,"","", Metric(0.0, "", ""))) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Aggiungi Ingrediente")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Preparazione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    steps.forEachIndexed { index, step ->
                        StepInputRow(
                            stepText = step.step,
                            onUpdate = { newText -> steps[index] = step.copy(step = newText, number = index + 1) },
                            onDelete = { steps.removeAt(index) }
                        )
                    }
                    Button(onClick = { steps.add(Step(number = steps.size + 1, step = "")) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Aggiungi Step")
                    }
                }

                Button(
                    onClick = {
                        val newMeal = CustomMealEntity(
                            userid = "",
                            title = title,
                            readyInMinutes = readyInMinutes.toIntOrNull() ?: 0,
                            servings = servings.toIntOrNull() ?: 0,
                            summary = summary,
                            dishTypes = dishType.split(",").map { it.trim().lowercase() },
                            extendedIngredients = ingredients.toList(),
                            analyzedInstructions = listOf(Instruction(steps = steps.toList())),
                            spoonacularId = null, image = null, spoonacularScore = null, nutrition = null, cuisines = null
                        )
                        onAddRecipe(newMeal)
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Salva Ricetta")
                }
            }
        }
    }
}

@Composable
fun IngredientInputRow(ingredient: Ingredient, onUpdate: (Ingredient) -> Unit, onDelete: () -> Unit) {
    var amountText by remember { mutableStateOf(ingredient.measures.amount.toString().removeSuffix(".0")) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(value = ingredient.originalName, onValueChange = { onUpdate(ingredient.copy(originalName = it)) }, label = { Text("Nome") })
            Row {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' }) {
                            amountText = newValue
                            val newAmount = newValue.toDoubleOrNull() ?: 0.0
                            onUpdate(ingredient.copy(measures = ingredient.measures.copy(amount = newAmount)))
                        }
                    },
                    label = { Text("Q.tà") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = ingredient.measures.unitShort,
                    onValueChange = { onUpdate(ingredient.copy(measures = ingredient.measures.copy(unitShort = it))) },
                    label = { Text("Unità") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun StepInputRow(stepText: String, onUpdate: (String) -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = stepText,
            onValueChange = onUpdate,
            label = { Text("Descrizione step") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error) }
    }
}