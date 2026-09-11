package com.example.foodhub.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class MealTypeConverters {

    private val moshi = Moshi.Builder().build()

    private val ingredientListType = Types.newParameterizedType(List::class.java, Ingredient::class.java)
    private val ingredientAdapter = moshi.adapter<List<Ingredient>>(ingredientListType)

    private val nutritionAdapter = moshi.adapter(Nutrition::class.java)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringAdapter = moshi.adapter<List<String>>(stringListType)

    private val instructionListType = Types.newParameterizedType(List::class.java, Instruction::class.java)
    private val instructionAdapter = moshi.adapter<List<Instruction>>(instructionListType)

    // 1. Converter per List<Ingredient>
    @TypeConverter
    fun fromIngredientList(value: List<Ingredient>?): String? {
        return value?.let { ingredientAdapter.toJson(it) }
    }

    @TypeConverter
    fun toIngredientList(value: String?): List<Ingredient> {
        return value?.let { ingredientAdapter.fromJson(it) } ?: emptyList()
    }


    // 2. Converter per Nutrition (Oggetto singolo)
    @TypeConverter
    fun fromNutrition(value: Nutrition?): String? {
        return value?.let { nutritionAdapter.toJson(it) }
    }

    @TypeConverter
    fun toNutrition(value: String?): Nutrition? {
        return value?.let { nutritionAdapter.fromJson(it) }
    }


    // 3. Converter per List<String> (Serve per cuisines e dishTypes)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { stringAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.let { stringAdapter.fromJson(it) } ?: emptyList()
    }


    // 4. Converter per List<Instruction>
    @TypeConverter
    fun fromInstructionList(value: List<Instruction>?): String? {
        return value?.let { instructionAdapter.toJson(it) }
    }

    @TypeConverter
    fun toInstructionList(value: String?): List<Instruction> {
        return value?.let { instructionAdapter.fromJson(it) } ?: emptyList()
    }
}