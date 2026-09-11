package com.example.foodhub

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.local.CachedMealDao
import com.example.foodhub.data.local.CachedMealEntity
import com.example.foodhub.data.local.MealEntity
import com.example.foodhub.data.local.PantryDao
import com.example.foodhub.data.local.PreferencesDao
import com.example.foodhub.data.local.PreferencesEntity
import com.example.foodhub.data.local.WeeklyPlanDao
import com.example.foodhub.data.model.DayPlan
import com.example.foodhub.data.model.UserPreferencesRequest
import com.example.foodhub.data.model.WeeklyPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class CacheRepository(
    private val apiService : ApiService,
    private val cacheDao: CachedMealDao,
    private val weekDao: WeeklyPlanDao,
    private val preferencesDao: PreferencesDao,
    private val pantryDao: PantryDao,
    private val tokenManager: TokenManager,
    private val ApiQuotaManager: ApiQuotaManager
) {

    private val seven_days = 7L * 24 * 60 * 60 * 1000

    suspend fun generateWeek(): WeeklyPlan {

        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: throw Exception("Token non trovato.")
            val currentUserId = tokenManager.getUserIdFromToken(token) ?: throw Exception("User ID non trovato.")


            val cutoffTime = System.currentTimeMillis() - seven_days

            var queryString = "SELECT * FROM cached_meals WHERE fetchedTimestamp >= ?"
            val bindArgs = mutableListOf<Any>(cutoffTime)

            val currentMeals = weekDao.getAllPlannedMeals(currentUserId).first()
            val currentWeekRecipeIds = currentMeals.map { it.spoonacularId }

            val query = SimpleSQLiteQuery(queryString, bindArgs.toTypedArray())
            var recipePool = cacheDao.getMealsByMultipleIngredients(query).toMutableList()

            val breakfastsPool = recipePool.filter { it.dishTypes.contains("breakfast") }
            val mainsPool = recipePool.filter { it.dishTypes.contains("main course") }

            if (breakfastsPool.size < 10 || mainsPool.size < 20) {
                fetchAndCacheBulk(token)
                val freshQuery = SimpleSQLiteQuery("SELECT * FROM cached_meals WHERE fetchedTimestamp >= ?", arrayOf(cutoffTime))
                recipePool = cacheDao.getMealsByMultipleIngredients(freshQuery).toMutableList()
            }

            val pantryList = pantryDao.getAllIngredients(currentUserId)
            val virtualPantry = pantryList.associate { it.originalName.lowercase() to it.amountInStock.toFloat() }.toMutableMap()
            val calendarList = mutableListOf<MealEntity>()
            val workingPool = recipePool.toMutableList()

            val daysOfWeek = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")

            for (day in daysOfWeek) {
                val breakfastWinner = selectBestMeal(workingPool, "breakfast", calendarList, virtualPantry, day, "Colazione", currentUserId, currentWeekRecipeIds)
                calendarList.add(breakfastWinner)
                workingPool.removeAll { it.spoonacularId == breakfastWinner.spoonacularId }
                deductFromVirtualPantry(breakfastWinner, virtualPantry)

                val lunchWinner = selectBestMeal(workingPool, "main course", calendarList, virtualPantry, day, "Pranzo", currentUserId, currentWeekRecipeIds)
                calendarList.add(lunchWinner)
                workingPool.removeAll { it.spoonacularId == lunchWinner.spoonacularId }
                deductFromVirtualPantry(lunchWinner, virtualPantry)

                val dinnerWinner = selectBestMeal(workingPool, "main course", calendarList, virtualPantry, day, "Cena", currentUserId, currentWeekRecipeIds)
                calendarList.add(dinnerWinner)
                workingPool.removeAll { it.spoonacularId == dinnerWinner.spoonacularId }
                deductFromVirtualPantry(dinnerWinner, virtualPantry)
            }

            val dayPlans = daysOfWeek.map { dayName ->
                DayPlan(dayName, calendarList.filter { it.dayOfWeek == dayName })
            }
            val newWeeklyPlan = WeeklyPlan(id = "week_${System.currentTimeMillis()}", days = dayPlans)

            weekDao.clearEntireWeek(currentUserId)
            calendarList.forEach { weekDao.insertPlannedMeal(it) }
            apiService.saveCurrentPlan("Bearer $token", calendarList)


            return@withContext newWeeklyPlan
        }
    }

    private suspend fun fetchAndCacheBulk(token: String) = coroutineScope {
        val authHeader = "Bearer $token"
        val tempB = async { apiService.getRecipesByType(authHeader, "breakfast", 30) }
        val tempM = async { apiService.getRecipesByType(authHeader, "main course", 60) }

        val resB = tempB.await()
        val resM = tempM.await()

        Log.e("KitchenOS_Logs", "Dimensione Breakfast: ${resB.body()?.size}")
        Log.e("KitchenOS_Logs", "Dimensione Main Course: ${resM.body()?.size}")

        if (resB.isSuccessful && resM.isSuccessful) {
            val toCache = (resB.body() ?: emptyList()) + (resM.body() ?: emptyList())
            cacheDao.insertCachedMeals(toCache)
            cacheDao.keepOnlyRecentCache()

            val leftQuotaB = resB.headers()["X-API-Quota-Left"]?.toFloatOrNull() ?: -1f
            val leftQuotaM = resM.headers()["X-API-Quota-Left"]?.toFloatOrNull() ?: -1f
            val latestResponse = if (leftQuotaM != -1f && (leftQuotaB == -1f || leftQuotaM < leftQuotaB)) {
                resM
            } else {
                resB
            }

            val finalRequestQuota = latestResponse.headers()["X-API-Quota-Request"]?.toFloatOrNull() ?: 0f
            val finalUsedQuota = latestResponse.headers()["X-API-Quota-Used"]?.toFloatOrNull() ?: 0f
            val finalLeftQuota = latestResponse.headers()["X-API-Quota-Left"]?.toFloatOrNull() ?: -1f

            if (finalLeftQuota != -1f) {
                ApiQuotaManager.saveQuota(finalRequestQuota, finalUsedQuota, finalLeftQuota)
                Log.d("KitchenOS_Logs", "Quota aggiornata: Rimasti $finalLeftQuota crediti")
            }


        }
    }

    private fun selectBestMeal(
        pool: List<CachedMealEntity>,
        dishType: String,
        history: List<MealEntity>,
        pantry: Map<String, Float>,
        day: String,
        mealTypeLabel: String,
        userId: String,
        seenRecipeIds: List<Long>
    ): MealEntity {
        var candidates = pool.filter { it.dishTypes.contains(dishType) && it.spoonacularId !in seenRecipeIds }

        if (candidates.isEmpty()) {
            val fallbackIncludingSeen = pool.filter { it.dishTypes.contains(dishType) }
            if (fallbackIncludingSeen.isNotEmpty()) {
                Log.w("KitchenOS_Logs", "Nessun candidato disponibile escludendo ricette già in settimana: userò candidati includendo quelle già pianificate per $day/$mealTypeLabel")
                candidates = fallbackIncludingSeen
            } else {
                val broadNotSeen = pool.filter { it.spoonacularId !in seenRecipeIds }
                if (broadNotSeen.isNotEmpty()) {
                    Log.w("KitchenOS_Logs", "Nessun candidato del tipo $dishType: uso ricette di altri tipi non ancora pianificate per $day/$mealTypeLabel")
                    candidates = broadNotSeen
                } else if (pool.isNotEmpty()) {
                    Log.w("KitchenOS_Logs", "Ultimo fallback: userò qualsiasi ricetta disponibile per $day/$mealTypeLabel")
                    candidates = pool
                }
            }
        }

        val lastMeals = history.takeLast(3)
        val recentIngredients = lastMeals.flatMap { it.extendedIngredients.map { ing -> ing.nameClean.lowercase() } }.toSet()

        val scoredCandidates = candidates.map { recipe ->
            var score = 0
            val recipeIngredients = recipe.extendedIngredients.map { it.nameClean.lowercase() }

            if (recipeIngredients.any { it in recentIngredients }) {
                score -= 20
            }

            recipe.extendedIngredients.forEach { ing ->
                val pantryAmount = pantry[ing.nameClean.lowercase()] ?: 0f
                if (pantryAmount > 0) {
                    score += 10
                    if (pantryAmount >= ing.measures.amount) {
                        score += 5
                    }
                }
            }
            recipe to score
        }.sortedByDescending { it.second }

        val winner = scoredCandidates.firstOrNull()?.first ?: candidates.shuffled().firstOrNull()

        if (winner == null) {
            Log.e("KitchenOS_Logs", "Impossibile scegliere un pasto per $day/$mealTypeLabel: pool ricette vuoto")
            throw Exception("Nessuna ricetta disponibile per $day/$mealTypeLabel")
        }

        return winner.toMealEntity(day, mealTypeLabel, userId)
    }

    private fun deductFromVirtualPantry(meal: MealEntity, pantry: MutableMap<String, Float>) {
        meal.extendedIngredients.forEach { ing ->
            val name = ing.nameClean.lowercase()
            val current = pantry[name] ?: 0.0f
            pantry[name] = current - ing.measures.amount.toFloat()
        }
    }

    fun CachedMealEntity.toMealEntity(dayName: String, assignedMealType: String, userId: String): MealEntity {
        return MealEntity(
            userid = userId,
            spoonacularId = this.spoonacularId,
            image = this.image,
            title = this.title,
            readyInMinutes = this.readyInMinutes,
            servings = this.servings,
            vegetarian = this.vegetarian,
            vegan = this.vegan,
            glutenFree = this.glutenFree,
            dairyFree = this.dairyFree,
            summary = this.summary,
            spoonacularScore = this.spoonacularScore,
            extendedIngredients = this.extendedIngredients,
            nutrition = this.nutrition,
            cuisines = this.cuisines,
            dishTypes = this.dishTypes,
            analyzedInstructions = this.analyzedInstructions,
            dayOfWeek = dayName,
            mealType = assignedMealType
        )
    }

    fun UserPreferencesRequest.toPreferencesEntity(userId: String): PreferencesEntity {
        return PreferencesEntity(
            userid = userId,
            dietaryRegime = this.dietaryRegime ?: "ONNIVORO",
            allergies = this.allergies?.joinToString(",") ?: "",
            dietType = this.dietType?.joinToString(",") ?: "",
            timePreparation = this.timePreparation ?: "PIU_DI_60",
            cousine = this.cousine?.joinToString(",") ?: ""
        )
    }

    suspend fun regenerateDay(dayName: String) {
        return withContext(Dispatchers.IO) {
            val token = tokenManager.getToken() ?: throw Exception("Token non trovato.")
            val currentUserId = tokenManager.getUserIdFromToken(token) ?: throw Exception("User ID non trovato.")

            val cutoffTime = System.currentTimeMillis() - seven_days

            var queryString = "SELECT * FROM cached_meals WHERE fetchedTimestamp >= ?"
            val bindArgs = mutableListOf<Any>(cutoffTime)

            val query = SimpleSQLiteQuery(queryString, bindArgs.toTypedArray())
            var recipePool = cacheDao.getMealsByMultipleIngredients(query).toMutableList()

            val breakfastsPool = recipePool.filter { it.dishTypes.contains("breakfast") }
            val mainsPool = recipePool.filter { it.dishTypes.contains("main course") }

            if (breakfastsPool.size < 10 || mainsPool.size < 20) {
                fetchAndCacheBulk(token)
                val freshQuery = SimpleSQLiteQuery("SELECT * FROM cached_meals WHERE fetchedTimestamp >= ?", arrayOf(cutoffTime))
                recipePool = cacheDao.getMealsByMultipleIngredients(freshQuery).toMutableList()
            }

            val pantryList = pantryDao.getAllIngredients(currentUserId)
            val virtualPantry = pantryList.associate { it.originalName.lowercase() to it.amountInStock.toFloat() }.toMutableMap()

            val daysOfWeek = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica")
            val calendarList = mutableListOf<MealEntity>()
            for (d in daysOfWeek) {
                if (d == dayName) continue
                val meals = weekDao.getMealsForDay(d).first()
                calendarList.addAll(meals)
            }

            val workingPool = recipePool.toMutableList()

            val currentMeals = weekDao.getAllPlannedMeals(currentUserId).first()
            val currentWeekRecipeIds = currentMeals.map { it.spoonacularId }

            val breakfastWinner = selectBestMeal(workingPool, "breakfast", calendarList, virtualPantry, dayName, "Colazione", currentUserId, currentWeekRecipeIds)
            calendarList.add(breakfastWinner)
            workingPool.removeAll { it.spoonacularId == breakfastWinner.spoonacularId }
            deductFromVirtualPantry(breakfastWinner, virtualPantry)

            val lunchWinner = selectBestMeal(workingPool, "main course", calendarList, virtualPantry, dayName, "Pranzo", currentUserId, currentWeekRecipeIds)
            calendarList.add(lunchWinner)
            workingPool.removeAll { it.spoonacularId == lunchWinner.spoonacularId }
            deductFromVirtualPantry(lunchWinner, virtualPantry)

            val dinnerWinner = selectBestMeal(workingPool, "main course", calendarList, virtualPantry, dayName, "Cena", currentUserId, currentWeekRecipeIds)
            calendarList.add(dinnerWinner)
            workingPool.removeAll { it.spoonacularId == dinnerWinner.spoonacularId }
            deductFromVirtualPantry(dinnerWinner, virtualPantry)

            val existingDayMeals = weekDao.getMealsForDay(dayName).first()
            existingDayMeals.forEach { weekDao.deletePlannedMeal(it) }

            weekDao.insertPlannedMeal(breakfastWinner)
            weekDao.insertPlannedMeal(lunchWinner)
            weekDao.insertPlannedMeal(dinnerWinner)

            apiService.saveCurrentPlan("Bearer $token", calendarList)
        }
    }

     suspend fun syncPreferences(token: String) {
        val currentUserId = tokenManager.getUserIdFromToken(token) ?: return
        val localPrefs = preferencesDao.getPreferences(currentUserId)

        if (localPrefs == null) {
            try {
                val response = apiService.getUserPreferences("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val remotePrefs = response.body()!!
                    preferencesDao.savePreferences(remotePrefs.toPreferencesEntity(currentUserId))

                    cacheDao.clearAllCache()
                    Log.d("KitchenOS_Logs", "Cache cleared for new user: $currentUserId")
                }
            } catch (e: Exception) {
                Log.e("KitchenOS_Logs", "Errore sync: ${e.message}")
            }
        } else if (localPrefs.userid == currentUserId) {
            Log.d("KitchenOS_Logs", "Same user $currentUserId logging back in - cache preserved")
        }
    }

    suspend fun syncWeeklyPlan(token: String) {
        val currentUserId = tokenManager.getUserIdFromToken(token) ?: return
        val localMeals = weekDao.getAllPlannedMeals(currentUserId).first()

        if (localMeals.isEmpty()) {
            try {
                val response = apiService.getCurrentPlan("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val remoteMeals = response.body()!!
                    weekDao.clearEntireWeek(currentUserId)
                    remoteMeals.forEach { meal ->
                        val mealToInsert = meal.copy(id = 0, userid = currentUserId)
                        weekDao.insertPlannedMeal(mealToInsert)
                    }
                }
            } catch (e: Exception) {
                Log.e("KitchenOS_Logs", "Errore sync: ${e.message}")
            }
        } else {
            Log.d("KitchenOS_Logs", "Piano già presente in Room, salto sync backend.")
        }
    }
}
