package com.example.foodhub.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodhub.ApiQuotaManager
import com.example.foodhub.TokenManager
import com.example.foodhub.data.api.ApiService
import com.example.foodhub.data.local.PantryDao
import com.example.foodhub.data.local.PantryEntity
import com.example.foodhub.data.local.WeeklyPlanDao
import com.example.foodhub.data.model.IngredientStatus
import com.example.foodhub.data.model.PantryUiItem
import com.example.foodhub.data.model.PantryUiState
import com.example.foodhub.data.model.ScanResultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.time.LocalDate
import kotlin.text.toFloatOrNull
import com.example.foodhub.data.model.Barcode


class PantryViewModel(
	private val pantryDao: PantryDao,
	private val weeklyPlanDao: WeeklyPlanDao,
	private val tokenManager: TokenManager,
	private val apiService: ApiService,
	private val apiQuotaManager: ApiQuotaManager
) : ViewModel() {

	private val _uiState = MutableStateFlow<PantryUiState>(PantryUiState.Idle)
	val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

	private val _scanState = MutableStateFlow<ScanResultState>(ScanResultState.Hidden)
	val scanState: StateFlow<ScanResultState> = _scanState.asStateFlow()


	fun refresh() {
		viewModelScope.launch {
			_uiState.value = PantryUiState.Loading
			try {

				val token = tokenManager.getToken()
				val userId = token?.let { tokenManager.getUserIdFromToken(it) }

				val pantry = if (userId != null) pantryDao.getAllIngredients(userId) else emptyList()
				val meals = if (userId != null) {
					weeklyPlanDao.getAllPlannedMeals(userId).first()
				} else emptyList()

				val currentDayValue = LocalDate.now().dayOfWeek.value

				val upcomingMeals = meals.filter { meal ->
					val mealDayValue = mapDayOfWeekToInt(meal.dayOfWeek)

					mealDayValue >= currentDayValue
				}

				val requiredIngredients = mutableMapOf<String, PantryUiItem>()

				upcomingMeals.flatMap { it.extendedIngredients }.forEach { ing ->
					val name = (ing.nameClean ?: ing.originalName).lowercase()
					val existing = requiredIngredients[name]

					if (existing != null) {
						requiredIngredients[name] = existing.copy(amount = existing.amount + ing.measures.amount.toFloat())
					} else {
						requiredIngredients[name] = PantryUiItem(
							id = -1L,
							name = ing.nameClean ?: ing.originalName,
							image = ing.image ?: "",
							amount = ing.measures.amount.toFloat(),
							unit = ing.measures.unitLong,
							status = IngredientStatus.TO_BUY
						)
					}
				}

				val finalUiItems = mutableListOf<PantryUiItem>()

				pantry.forEach { p ->
					val lowerName = p.originalName.lowercase()
					val required = requiredIngredients[lowerName]

					if (required != null) {
						if (p.amountInStock >= required.amount) {
							finalUiItems.add(
								PantryUiItem(p.id, p.originalName, p.image, p.amountInStock, p.unit, IngredientStatus.IN_USE)
							)
							requiredIngredients.remove(lowerName)
						} else {
							if (p.amountInStock > 0) {
								finalUiItems.add(
									PantryUiItem(p.id, p.originalName, p.image, p.amountInStock, p.unit, IngredientStatus.IN_USE)
								)
							}
							val missingAmount = required.amount - p.amountInStock
							requiredIngredients[lowerName] = required.copy(amount = missingAmount)
						}
					} else {
						val status = if (p.amountInStock <= 0.0) IngredientStatus.TO_BUY else IngredientStatus.UNUSED
						finalUiItems.add(
							PantryUiItem(
								p.id,
								p.originalName,
								p.image,
								p.amountInStock,
								p.unit,
								status)
						)
					}
				}

				finalUiItems.addAll(requiredIngredients.values)

				_uiState.value = PantryUiState.Success(finalUiItems)

			} catch (e: Exception) {
				_uiState.value = PantryUiState.Error(e.message ?: "Errore inatteso")
			}
		}
	}

	private fun mapDayOfWeekToInt(day: String): Int {
		return when (day.lowercase().trim()) {
			"lunedi", "lunedì" -> 1
			"martedi", "martedì" -> 2
			"mercoledi", "mercoledì" -> 3
			"giovedi", "giovedì" -> 4
			"venerdi", "venerdì" -> 5
			"sabato"           -> 6
			"domenica"         -> 7
			else               -> 1
		}
	}

	fun processScannedBarcode(barcodeVal: String) {
		Log.e("KitchenOS_Logs", "Codice ricevuto: $barcodeVal")
		viewModelScope.launch {
			_scanState.value = ScanResultState.Loading

			try {
				val token = tokenManager.getToken()
				val userId = token?.let { tokenManager.getUserIdFromToken(it) }

				if (userId == null) {
					_scanState.value = ScanResultState.Hidden
					return@launch
				}
				Log.e("KitchenOS_Logs", "UserId: ${userId}")
				val response = apiService.verifyProduct("Bearer $token", Barcode(barcodeVal))


				val requestQuota = response.headers()["X-API-Quota-Request"]?.toFloatOrNull() ?: 0f
				val usedQuota = response.headers()["X-API-Quota-Used"]?.toFloatOrNull() ?: 0f
				val leftQuota = response.headers()["X-API-Quota-Left"]?.toFloatOrNull() ?: -1f

				if (leftQuota != -1f) {
					apiQuotaManager.saveQuota(requestQuota, usedQuota, leftQuota)
					Log.d("KitchenOS_Logs", "Quota aggiornata: Rimasti $leftQuota crediti")
				}

				if (response.isSuccessful) {
					val product = response.body()

					if (product != null) {
						_scanState.value = ScanResultState.ShowForm(
							id = product.id,
							userid = product.userid,
							originalName = product.originalName,
							image = product.image,
							amountInStock = product.amountInStock,
							unit = product.unit,
							isError = false
						)
					}
				} else {
					val isNotFound = response.code() == 404
					_scanState.value = ScanResultState.ShowForm(
						id = System.currentTimeMillis(),
						userid = userId,
						originalName = "",
						image = null,
						amountInStock = 0f,
						unit = "",
						isError = true,
						errorMessage = if (isNotFound)
							"Non abbiamo trovato questo prodotto nel nostro database. Inserisci i dettagli manualmente."
						else
							"Errore server (${response.code()}). Inserisci i dettagli manualmente."
					)
				}

			} catch (e: Exception) {
				Log.e("KitchenOS_Logs", "Errore di rete: ${e.message}")
				_scanState.value = ScanResultState.ShowForm(
					id = System.currentTimeMillis(),
					userid = tokenManager.getToken()?.let { tokenManager.getUserIdFromToken(it) } ?: "",
					originalName = "",
					image = null,
					amountInStock = 0f,
					unit = "",
					isError = true,
					errorMessage = "Errore di connessione. Inserisci i dettagli manualmente."
				)
			}
		}
	}

	fun confirmAndSaveIngredient(id: Long, userid: String, name: String, amount: Float, unit: String, imageUrl: String?) {
		viewModelScope.launch {
			val newIngredient = PantryEntity(
				id = id,
				userid = userid,
				originalName = name,
				image = imageUrl,
				amountInStock = amount,
				unit = unit
			)

			pantryDao.addOrUpdateIngredient(newIngredient)

			_scanState.value = ScanResultState.Hidden
			refresh()
		}
	}

	fun dismissScanDialog() {
		_scanState.value = ScanResultState.Hidden
	}


	fun processImageLabels(labels: List<String>) {
		Log.e("KitchenOS_Logs", "Etichette AI ricevute: $labels")
		if (labels.isEmpty()) return
		viewModelScope.launch {
			_scanState.value = ScanResultState.Loading

			try {
				val token = tokenManager.getToken()
				val userId = token?.let { tokenManager.getUserIdFromToken(it) }

				if (userId == null) {
					_scanState.value = ScanResultState.Hidden
					return@launch
				}
				Log.e("KitchenOS_Logs", "UserId: $userId")

				val bestLabel = labels.first()

				_scanState.value = ScanResultState.ShowForm(
					id = System.currentTimeMillis(),
					userid = userId,
					originalName = bestLabel,
					image = null,
					amountInStock = 0f,
					unit = "",
					isError = false,
					errorMessage = "Oggetto rilevato dall'AI. Completa i dettagli per salvarlo."
				)

			} catch (e: Exception) {
				Log.e("KitchenOS_Logs", "Errore processImageLabels: ${e.message}")
				_scanState.value = ScanResultState.ShowForm(
					id = System.currentTimeMillis(),
					userid = tokenManager.getToken()?.let { tokenManager.getUserIdFromToken(it) } ?: "",
					originalName = "",
					image = null,
					amountInStock = 0f,
					unit = "",
					isError = true,
					errorMessage = "Errore durante l'elaborazione AI. Inserisci i dettagli manualmente."
				)
			}
		}
	}
}


