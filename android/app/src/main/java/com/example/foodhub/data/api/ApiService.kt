package com.example.foodhub.data.api

import com.example.foodhub.data.local.CachedMealEntity
import com.example.foodhub.data.local.CustomMealEntity
import com.example.foodhub.data.local.MealEntity
import com.example.foodhub.data.local.PantryEntity
import com.example.foodhub.data.model.AuthResponse
import com.example.foodhub.data.model.Barcode
import com.example.foodhub.data.model.GenericResponse
import com.example.foodhub.data.model.Id
import com.example.foodhub.data.model.LoginRequest
import com.example.foodhub.data.model.RegisterRequest
import com.example.foodhub.data.model.UserPreferencesRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @GET("user/preferences")
    suspend fun getUserPreferences(
        @Header("Authorization") token: String
    ): Response<UserPreferencesRequest>

    @POST("user/preferences")
    suspend fun savePreferences(
        @Header("Authorization") token: String,
        @Body request: UserPreferencesRequest
    ): Response<GenericResponse>

    // TODO: cambiare da testGenerate a generate
    @POST("plan/generate")
    suspend fun getRecipesByType(
        @Header("Authorization") token: String,
        @Query("type") type: String,
        @Query("count") count: Int
    ): Response<List<CachedMealEntity>>

    @GET("plan/current")
    suspend fun getCurrentPlan(
        @Header("Authorization") token: String
    ): Response<List<MealEntity>>

    @POST("plan/current")
    suspend fun saveCurrentPlan(
        @Header("Authorization") token: String,
        @Body request: List<MealEntity>
    ): Response<GenericResponse>

    @POST("user/verify")
    suspend fun verifyProduct(
        @Header("Authorization") token: String,
        @Body request: Barcode
    ): Response<PantryEntity>

    @POST("user/getCustom")
    suspend fun getRemoteRecipes(
        @Header("Authorization") token: String,
    ): Response<List<CustomMealEntity>>


    @POST("user/addCustom")
    suspend fun addRemoteRecipe(
        @Header("Authorization") token: String,
        @Body request: CustomMealEntity
    ): Response<GenericResponse>

    @POST("user/deleteCustom")
    suspend fun deleteRemoteRecipe(
        @Header("Authorization") token: String,
        @Body request: Id
    ): Response<GenericResponse>
}
