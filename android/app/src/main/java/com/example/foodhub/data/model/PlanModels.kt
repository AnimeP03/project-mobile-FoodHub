package com.example.foodhub.data.model

import com.example.foodhub.data.local.CustomMealEntity
import com.example.foodhub.data.local.MealEntity
import com.squareup.moshi.JsonClass

sealed class ScanResultState {
    object Hidden : ScanResultState()
    object Loading : ScanResultState()
    data class ShowForm(
        val id: Long,
        val userid: String,
        val originalName: String = "",
        val image: String? = null,
        val amountInStock: Float = 0f,
        val unit: String = "",
        val isError: Boolean = false,
        val errorMessage: String? = null
    ) : ScanResultState()
}

sealed class CustomRecipesUiState {
    object Loading : CustomRecipesUiState()
    data class Success(val recipes: List<CustomMealEntity>) : CustomRecipesUiState()
    data class Error(val message: String) : CustomRecipesUiState()
}

sealed class PianoUiState {
    object Idle : PianoUiState()
    object Loading : PianoUiState()
    data class Success(val plan: WeeklyPlan) : PianoUiState()
    data class Error(val message: String) : PianoUiState()
}

sealed class PantryUiState {
    object Idle : PantryUiState()
    object Loading : PantryUiState()
    data class Success(val items: List<PantryUiItem>) : PantryUiState()
    data class Error(val message: String) : PantryUiState()
}

@JsonClass(generateAdapter = true)
data class Metric(
    val amount: Double,
    val unitShort: String,
    val unitLong: String
)
@JsonClass(generateAdapter = true)
data class Ingredient(
    val id: Long?,
    val image: String?,
    val originalName: String,
    val nameClean: String,
    val measures: Metric
)



@JsonClass(generateAdapter = true)
data class Nutrition(
    val nutrients: List<Nutrient>,
    val caloricBreakdown: CaloricBreakdown
)

@JsonClass(generateAdapter = true)
data class Nutrient(
    val name: String,
    val amount: Double,
    val unit: String
)
@JsonClass(generateAdapter = true)
data class CaloricBreakdown(
    val percentProtein: Double,
    val percentFat: Double,
    val percentCarbs: Double
)
@JsonClass(generateAdapter = true)
data class Step(
    val number: Int,
    val step: String
)
@JsonClass(generateAdapter = true)
data class Instruction(
    val steps: List<Step>
)

data class Barcode(
    val barcode: String
)

data class Id(
    val id: Long
)


data class DayPlan(
    val dayName: String,
    val meals: List<MealEntity>,
    var isExpanded: Boolean = false
)

data class WeeklyPlan(
    val id: String,
    val days: List<DayPlan>
)
