package com.example.foodhub.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.foodhub.data.model.Ingredient
import com.example.foodhub.data.model.Instruction
import com.example.foodhub.data.model.MealTypeConverters
import com.example.foodhub.data.model.Nutrition
import kotlinx.coroutines.flow.Flow

@Database(entities = [PreferencesEntity::class, MealEntity::class, CachedMealEntity::class, PantryEntity::class, CustomMealEntity::class], version = 1)
@TypeConverters(MealTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun preferencesDao(): PreferencesDao
    abstract fun cachedMealDao(): CachedMealDao
    abstract fun weeklyPlanDao(): WeeklyPlanDao
    abstract fun pantryDao(): PantryDao
    abstract fun customRecipeDao(): CustomRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "OSdb"
                ).fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Entity(tableName = "user_preferences")
data class PreferencesEntity(
    @PrimaryKey
    val userid: String,
    val dietaryRegime: String,
    val allergies: String,
    val dietType: String,
    val timePreparation: String,
    val cousine: String
)

@Entity(tableName = "meal_entity")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userid: String,

    val spoonacularId: Long,
    val image: String,
    val title: String,
    val readyInMinutes: Int,
    val servings: Int,
    val vegetarian: Boolean,
    val vegan: Boolean,
    val glutenFree: Boolean,
    val dairyFree: Boolean,
    val summary: String,
    val spoonacularScore: Double,

    val extendedIngredients: List<Ingredient>,
    val nutrition: Nutrition,
    val cuisines: List<String>,
    val dishTypes: List<String>,
    val analyzedInstructions: List<Instruction>,

    val dayOfWeek: String,
    val mealType: String,

    val fetchedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_meal_entity")
data class CustomMealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userid: String,

    val spoonacularId: Long?,
    val image: String?,
    val title: String,
    val readyInMinutes: Int,
    val servings: Int,
    val summary: String,
    val spoonacularScore: Double?,

    val extendedIngredients: List<Ingredient>,
    val nutrition: Nutrition?,
    val cuisines: List<String>?,
    val dishTypes: List<String>,
    val analyzedInstructions: List<Instruction>,
)

@Entity(tableName = "cached_meals")
data class CachedMealEntity(
    @PrimaryKey val spoonacularId: Long,
    val image: String,
    val title: String,
    val readyInMinutes: Int,
    val servings: Int,
    val vegetarian: Boolean,
    val vegan: Boolean,
    val glutenFree: Boolean,
    val dairyFree: Boolean,
    val summary: String,
    val spoonacularScore: Double,

    val extendedIngredients: List<Ingredient>,
    val nutrition: Nutrition,
    val cuisines: List<String>,
    val dishTypes: List<String>,
    val analyzedInstructions: List<Instruction>,

    val fetchedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pantry")
data class PantryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val userid: String,
    val originalName: String,
    val image: String?,
    val amountInStock: Float,
    val unit: String
)

@Dao
interface PreferencesDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: PreferencesEntity)

    @Query("SELECT * FROM user_preferences WHERE userid = :userId")
    suspend fun getPreferences(userId: String): PreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE userid = :userId")
    fun getPreferencesFlow(userId: String): Flow<PreferencesEntity?>

    @Query("DELETE FROM user_preferences WHERE userid = :userId")
    suspend fun clearUserPreferences(userId: String)
}

@Dao
interface CachedMealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMeals(meals: List<CachedMealEntity>)

    @Query("""
        DELETE FROM cached_meals 
        WHERE spoonacularId NOT IN (
            SELECT spoonacularId FROM cached_meals 
            ORDER BY fetchedTimestamp DESC 
            LIMIT 300
        )
    """)
    suspend fun keepOnlyRecentCache()


    @RawQuery
    suspend fun getMealsByMultipleIngredients(query: SupportSQLiteQuery): List<CachedMealEntity>

    @Query("DELETE FROM cached_meals")
    suspend fun clearAllCache()


}

@Dao
interface WeeklyPlanDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlannedMeal(meal: MealEntity)

    @Delete
    suspend fun deletePlannedMeal(meal: MealEntity)

    @Query("DELETE FROM meal_entity WHERE userId = :userid")
    suspend fun clearEntireWeek(userid: String)

    @Query("SELECT * FROM meal_entity WHERE userId = :userId ORDER BY fetchedTimestamp ASC")
    fun getAllPlannedMeals(userId: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meal_entity WHERE dayOfWeek = :day")
    fun getMealsForDay(day: String): Flow<List<MealEntity>>
}

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry WHERE userid = :userId")
    suspend fun getAllIngredients(userId: String): List<PantryEntity>

    @Query("SELECT * FROM pantry WHERE id = :id AND userid = :userId LIMIT 1")
    suspend fun getIngredient(id: Long, userId: String): PantryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredient(ingredient: PantryEntity)

    @Transaction
    suspend fun addOrUpdateIngredient(ingredient: PantryEntity) {
        val existing = getIngredient(ingredient.id, ingredient.userid)

        if (existing != null) {
            val updatedQuantity = existing.amountInStock + ingredient.amountInStock
            updateIngredient(existing.copy(amountInStock = updatedQuantity))
        } else {
            insertIngredient(ingredient)
        }
    }

    @Update
    suspend fun updateIngredient(ingredient: PantryEntity)

    @Query("DELETE FROM pantry WHERE userid = :userId")
    suspend fun clearPantryForUser(userId: String)
}

@Dao
interface CustomRecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: CustomMealEntity): Long


    @Query("DELETE FROM custom_meal_entity WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)

    @Query("SELECT * FROM custom_meal_entity WHERE userid = :userId")
    suspend fun getRecipesByUserId(userId: String): List<CustomMealEntity>

    @Query("SELECT * FROM custom_meal_entity WHERE id = :id")
    suspend fun getRecipeById(id: Long): CustomMealEntity

}