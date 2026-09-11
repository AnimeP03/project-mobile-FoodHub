package com.example.foodhub.data.model



data class UserPreferencesRequest(
    val dietaryRegime: String? = null,
    val allergies: List<String>? = emptyList() ,
    val dietType: List<String>? = emptyList(),
    val timePreparation: String? = null,
    val cousine: List<String>? = emptyList()
)
data class GenericResponse(
    val success: Boolean,
    val message: String
)
