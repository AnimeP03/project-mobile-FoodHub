package com.example.foodhub.ui

import android.R.attr.enabled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.toSpannable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodhub.data.model.Allergies
import com.example.foodhub.data.model.Cousine
import com.example.foodhub.data.model.DietType
import com.example.foodhub.data.model.DietaryRegime
import com.example.foodhub.data.model.TimePreparation
import com.example.foodhub.viewmodel.PreferencesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PreferencesScreen(viewModel: PreferencesViewModel, onPreferencesSaved: () -> Unit) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val maxStep by viewModel.maxStep.collectAsStateWithLifecycle()

    // per usare il tasto indietro solo se step maggiore di 1
    BackHandler(enabled = currentStep > 1) {
        viewModel.previousStep()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step $currentStep di $maxStep",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 32.dp, bottom = 32.dp)
        )

        when (currentStep) {
            1 -> StepDietaryRegime(
                viewModel = viewModel,
                onRegimeSelected = { viewModel.nextStep() }
            )
            2 -> StepAllergies(
                viewModel = viewModel,
                onContinuaClick = { viewModel.nextStep() }
            )
            3 -> StepDietType(
                viewModel = viewModel,
                onContinuaClick = { viewModel.nextStep() }
            )
            4 -> StepCousine(
                viewModel = viewModel,
                onContinuaClick = { viewModel.nextStep() }
            )
            5 -> StepTime(
                viewModel = viewModel,
                onTimeSelected = { viewModel.nextStep(onSuccess = onPreferencesSaved) }
            )
        }
    }
}


@Composable
fun StepDietaryRegime(viewModel: PreferencesViewModel, onRegimeSelected: () -> Unit) {
    // per asspetare prima di cambiare
    val coroutineScope = rememberCoroutineScope()
    var midChange by remember { mutableStateOf(false) }

    val regimeSelected by viewModel.regimeSelected.collectAsState()

    Text(
        text = "Qual è il tuo regime alimentare base?",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 40.dp)
    )

    DietaryRegime.entries.forEach { regime ->
        val bgColor = if (regimeSelected == regime.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        val txtColor = if (regimeSelected == regime.name) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        Button(
            onClick = {
                if (!midChange) {
                    midChange = true

                    viewModel.setRegime(regime)
                    coroutineScope.launch {
                        delay(350)
                        onRegimeSelected()
                        midChange = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(bottom = 12.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = bgColor,
                contentColor = txtColor,
                disabledContainerColor = bgColor,
                disabledContentColor = txtColor
            ),
            enabled = !midChange
        ) {
            Text(text = regime.uiLabel, fontSize = 16.sp)
        }
    }
}

@Composable
fun StepAllergies(viewModel: PreferencesViewModel, onContinuaClick: () -> Unit) {
    val allergieSelected by viewModel.allergieSelected.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hai qualche allergia o intolleranza?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Allergies.entries.forEach { allergia ->
                val isSelected = allergieSelected.contains(allergia.name)

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.toggleAllergia(allergia) },
                    modifier = Modifier.height(54.dp),
                    label = { Text(text = allergia.uiLabel) },

                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.primary,

                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinuaClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continua", fontSize = 18.sp)
        }
    }
}

@Composable
fun StepDietType(viewModel: PreferencesViewModel, onContinuaClick: () -> Unit) {
    val typeSelected by viewModel.typeSelected.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Che dieta nutrizionale preferisci?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        )
        Column (
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)

        ) {
            DietType.entries.forEach { type ->
                val isSelected = typeSelected.contains(type.name)

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.toggleType(type) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = type.uiLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = type.desc,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp,
                                modifier = Modifier.alpha(0.8f)
                            )
                        }
                    },

                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.primary,

                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinuaClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = typeSelected.isNotEmpty()
        ) {
            Text("Continua", fontSize = 18.sp)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCousine(viewModel: PreferencesViewModel, onContinuaClick: () -> Unit){
    val selectedCousine by viewModel.cousineSelected.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Quali cucine preferisci?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Scegline quante ne vuoi",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    .border(
                        width = 1.dp,
                        color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    if (selectedCousine.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedCousine.forEach { couisine ->
                                var current = Cousine.valueOf(couisine)
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.toggleCuisine(current) },
                                    label = { Text(current.uiLabel) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Rimuovi",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCousine.isEmpty()) "Seleziona cucine..." else "Aggiungi cucina",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )

                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Cousine.entries.forEach { couisine ->
                    if (!selectedCousine.contains(couisine.name)) {
                        DropdownMenuItem(
                            text = { Text(couisine.uiLabel) },
                            onClick = {
                                viewModel.toggleCuisine(couisine)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (selectedCousine.isEmpty()) {
            Text(
                text = "Se non selezioni nulla, includeremo tutti i tipi di cucina nei tuoi menù.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            )
        }

        Button(
            onClick = onContinuaClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Continua", fontSize = 18.sp)
        }
    }
}

@Composable
fun StepTime(viewModel: PreferencesViewModel, onTimeSelected: () -> Unit){
    val coroutineScope = rememberCoroutineScope()
    var midChange by remember { mutableStateOf(false) }

    val selectedTime by viewModel.timeSelected.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Quanto tempo hai per cucinare?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )

        TimePreparation.entries.forEach { time ->
            val bgColor =
                if (selectedTime == time.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            val txtColor =
                if (selectedTime == time.name) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            Button(
                onClick = {
                    if (!midChange) {
                        midChange = true

                        viewModel.setTime(time)
                        coroutineScope.launch {
                            delay(350)
                            viewModel.clearError()
                            onTimeSelected()
                            midChange = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(bottom = 12.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = bgColor,
                    contentColor = txtColor,
                    disabledContainerColor = bgColor,
                    disabledContentColor = txtColor
                ),
                enabled = !midChange || !isLoading
            ) {
                Text(text = time.uiLabel, fontSize = 16.sp)
            }

        }
        Spacer(modifier = Modifier.weight(1f))

        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))


        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}