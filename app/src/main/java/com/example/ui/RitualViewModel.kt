package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.cloud.FirebaseSyncManager
import com.example.data.cloud.SyncState
import com.example.util.DocxGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen(val title: String) {
    CALCULATOR("Ритуальный калькулятор"),
    PRICE_SETTINGS("Прайс-лист и расценки"),
    SAVED_ORDERS("Сохраненные сметы"),
    CLOUD_SETTINGS("Облачная база и курс")
}

data class CalculationState(
    val orderNumber: String = generateOrderNumber(),
    val clientName: String = "",
    val clientPhone: String = "",
    val deceasedName: String = "",
    val cemeteryName: String = "",
    val plotNumber: String = "",
    val notes: String = "",
    val selectedItems: List<CalculatedItemData> = emptyList(),
    val discountPercent: Double = 0.0,
    val prepayment: Double = 0.0,
    val editingOrderId: String? = null,
    // Бланк заказа для завода (без ценников)
    val orderTerm: String = "",
    val deceasedLastName: String = "",
    val deceasedFirstName: String = "",
    val deceasedMiddleName: String = "",
    val deceasedBirthDate: String = "",
    val deceasedDeathDate: String = "",
    val crossInfo: String = "",
    val photoVignetteInfo: String = "",
    val frameInfo: String = "",
    val epitaphText: String = "",
    val plateDecoration: String = "",
    val additionsInfo: String = "",
    val monumentMaterial: String = "",
    val obeliskInfo: String = "",
    val plinthInfo: String = "",
    val flowerbedInfo: String = "",
    val slabInfo: String = "",
    val otmostkaInfo: String = "",
    val vaseInfo: String = "",
    val lampadaInfo: String = "",
    val dismantlingInfo: String = "",
    val tileInfo: String = "",
    val borderInfo: String = ""
) {
    val subtotal: Double
        get() = selectedItems.sumOf { it.totalPrice }

    val discountAmount: Double
        get() = subtotal * (discountPercent / 100.0)

    val total: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)

    val remainingAmount: Double
        get() = (total - prepayment).coerceAtLeast(0.0)

    companion object {
        fun generateOrderNumber(): String {
            val df = SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault())
            val randomSuffix = kotlin.random.Random.nextInt(100, 999)
            return "РК-${df.format(Date())}-$randomSuffix"
        }
    }
}

class RitualViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RitualRepository
    val syncManager: FirebaseSyncManager
    val syncState: StateFlow<SyncState>

    val networkMonitor = com.example.util.NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _currentScreen = MutableStateFlow(AppScreen.CALCULATOR)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _calcState = MutableStateFlow(CalculationState())
    val calcState: StateFlow<CalculationState> = _calcState.asStateFlow()

    private val _priceSearchQuery = MutableStateFlow("")
    val priceSearchQuery: StateFlow<String> = _priceSearchQuery.asStateFlow()

    private val _selectedPriceCategory = MutableStateFlow<String?>(null)
    val selectedPriceCategory: StateFlow<String?> = _selectedPriceCategory.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun showUserMessage(message: String) {
        _userMessage.tryEmit(message)
    }

    val allPrices: StateFlow<List<PriceItem>>
    val activePrices: StateFlow<List<PriceItem>>
    val savedOrders: StateFlow<List<SavedOrder>>

    val stoneMaterials: StateFlow<List<StoneMaterialItem>>
    val activeStoneMaterials: StateFlow<List<StoneMaterialItem>>

    val sizePresets: StateFlow<List<MonumentSizePresetItem>>
    val activeSizePresets: StateFlow<List<MonumentSizePresetItem>>

    val constructorServicePrices: StateFlow<List<ConstructorServicePriceItem>>
    val constructorPricesMap: StateFlow<Map<String, Double>>
    val usdExchangeRate: StateFlow<Double>
    val eurExchangeRate: StateFlow<Double>

    val engravingFonts: StateFlow<List<EngravingFontItem>>
    val activeEngravingFonts: StateFlow<List<EngravingFontItem>>

    val engravingDrawings: StateFlow<List<EngravingDrawingItem>>
    val activeEngravingDrawings: StateFlow<List<EngravingDrawingItem>>

    val photoSizes: StateFlow<List<PhotoSizeItem>>
    val activePhotoSizes: StateFlow<List<PhotoSizeItem>>

    val photoFrames: StateFlow<List<PhotoFrameItem>>
    val activePhotoFrames: StateFlow<List<PhotoFrameItem>>

    val vases: StateFlow<List<VaseItem>>
    val activeVases: StateFlow<List<VaseItem>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = RitualRepository(
            db.priceDao(),
            db.orderDao(),
            db.stoneMaterialDao(),
            db.monumentSizePresetDao(),
            db.constructorServicePriceDao(),
            db.engravingFontDao(),
            db.engravingDrawingDao(),
            db.photoSizeDao(),
            db.photoFrameDao(),
            db.vaseDao()
        )


        syncManager = FirebaseSyncManager(application, repository)
        syncState = syncManager.syncState

        allPrices = repository.allPriceItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activePrices = repository.enabledPriceItems
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savedOrders = repository.allOrders
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        stoneMaterials = repository.allStoneMaterials
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeStoneMaterials = repository.enabledStoneMaterials
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        sizePresets = repository.allSizePresets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeSizePresets = repository.enabledSizePresets
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        constructorServicePrices = repository.allConstructorServicePrices
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        constructorPricesMap = repository.allConstructorServicePrices
            .map { list -> list.associate { it.key to it.price } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        usdExchangeRate = constructorServicePrices
            .map { list -> list.find { it.key == "usd_exchange_rate" }?.price ?: 3.25 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3.25)

        eurExchangeRate = constructorServicePrices
            .map { list -> list.find { it.key == "eur_exchange_rate" }?.price ?: 3.55 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3.55)

        engravingFonts = repository.allEngravingFonts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeEngravingFonts = repository.enabledEngravingFonts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        engravingDrawings = repository.allEngravingDrawings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeEngravingDrawings = repository.enabledEngravingDrawings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        photoSizes = repository.allPhotoSizes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activePhotoSizes = repository.enabledPhotoSizes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        photoFrames = repository.allPhotoFrames
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activePhotoFrames = repository.enabledPhotoFrames
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        vases = repository.allVases
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeVases = repository.enabledVases
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



        viewModelScope.launch {
            repository.ensureDefaultDataLoaded()
            autoPullFromCloudOnStartup()
            refreshNbrbRates(showUserFeedback = false)

            var wasOnline = isOnline.value
            isOnline.collect { online ->
                if (online && !wasOnline) {
                    _userMessage.tryEmit("Подключение к сети восстановлено. Синхронизация с Firestore ☁️")
                    syncManager.refreshLocalCacheFromFirestore()
                }
                wasOnline = online
            }
        }
    }

    private fun autoPullFromCloudOnStartup() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            if (isOnline.value) {
                syncManager.refreshLocalCacheFromFirestore()
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setPriceSearchQuery(query: String) {
        _priceSearchQuery.value = query
    }

    fun setPriceCategory(category: String?) {
        _selectedPriceCategory.value = category
    }

    // --- Calculator Operations ---

    fun updateClientDetails(
        clientName: String = _calcState.value.clientName,
        clientPhone: String = _calcState.value.clientPhone,
        deceasedName: String = _calcState.value.deceasedName,
        deceasedBirthDate: String = _calcState.value.deceasedBirthDate,
        deceasedDeathDate: String = _calcState.value.deceasedDeathDate,
        cemeteryName: String = _calcState.value.cemeteryName,
        plotNumber: String = _calcState.value.plotNumber,
        notes: String = _calcState.value.notes,
        discountPercent: Double = _calcState.value.discountPercent,
        prepayment: Double = _calcState.value.prepayment
    ) {
        _calcState.update {
            it.copy(
                clientName = clientName,
                clientPhone = clientPhone,
                deceasedName = deceasedName,
                deceasedBirthDate = deceasedBirthDate,
                deceasedDeathDate = deceasedDeathDate,
                cemeteryName = cemeteryName,
                plotNumber = plotNumber,
                notes = notes,
                discountPercent = discountPercent,
                prepayment = prepayment
            )
        }
    }

    fun updateFactoryDetails(
        orderTerm: String = _calcState.value.orderTerm,
        deceasedLastName: String = _calcState.value.deceasedLastName,
        deceasedFirstName: String = _calcState.value.deceasedFirstName,
        deceasedMiddleName: String = _calcState.value.deceasedMiddleName,
        deceasedBirthDate: String = _calcState.value.deceasedBirthDate,
        deceasedDeathDate: String = _calcState.value.deceasedDeathDate,
        crossInfo: String = _calcState.value.crossInfo,
        photoVignetteInfo: String = _calcState.value.photoVignetteInfo,
        frameInfo: String = _calcState.value.frameInfo,
        epitaphText: String = _calcState.value.epitaphText,
        plateDecoration: String = _calcState.value.plateDecoration,
        additionsInfo: String = _calcState.value.additionsInfo,
        monumentMaterial: String = _calcState.value.monumentMaterial,
        obeliskInfo: String = _calcState.value.obeliskInfo,
        plinthInfo: String = _calcState.value.plinthInfo,
        flowerbedInfo: String = _calcState.value.flowerbedInfo,
        slabInfo: String = _calcState.value.slabInfo,
        otmostkaInfo: String = _calcState.value.otmostkaInfo,
        vaseInfo: String = _calcState.value.vaseInfo,
        lampadaInfo: String = _calcState.value.lampadaInfo,
        dismantlingInfo: String = _calcState.value.dismantlingInfo,
        tileInfo: String = _calcState.value.tileInfo,
        borderInfo: String = _calcState.value.borderInfo
    ) {
        _calcState.update {
            it.copy(
                orderTerm = orderTerm,
                deceasedLastName = deceasedLastName,
                deceasedFirstName = deceasedFirstName,
                deceasedMiddleName = deceasedMiddleName,
                deceasedBirthDate = deceasedBirthDate,
                deceasedDeathDate = deceasedDeathDate,
                crossInfo = crossInfo,
                photoVignetteInfo = photoVignetteInfo,
                frameInfo = frameInfo,
                epitaphText = epitaphText,
                plateDecoration = plateDecoration,
                additionsInfo = additionsInfo,
                monumentMaterial = monumentMaterial,
                obeliskInfo = obeliskInfo,
                plinthInfo = plinthInfo,
                flowerbedInfo = flowerbedInfo,
                slabInfo = slabInfo,
                otmostkaInfo = otmostkaInfo,
                vaseInfo = vaseInfo,
                lampadaInfo = lampadaInfo,
                dismantlingInfo = dismantlingInfo,
                tileInfo = tileInfo,
                borderInfo = borderInfo
            )
        }
    }

    fun addItemToCalculation(priceItem: PriceItem, quantity: Double = 1.0) {
        _calcState.update { state ->
            val existingIndex = state.selectedItems.indexOfFirst { it.priceItemId == priceItem.id }
            val newItems = state.selectedItems.toMutableList()

            if (existingIndex >= 0) {
                val existing = newItems[existingIndex]
                val newQty = existing.quantity + quantity
                newItems[existingIndex] = existing.copy(
                    quantity = newQty,
                    totalPrice = newQty * existing.unitPrice
                )
            } else {
                newItems.add(
                    CalculatedItemData(
                        priceItemId = priceItem.id,
                        category = priceItem.category,
                        name = priceItem.name,
                        unit = priceItem.unit,
                        unitPrice = priceItem.currentPrice,
                        quantity = quantity,
                        totalPrice = priceItem.currentPrice * quantity,
                        customNote = priceItem.description
                    )
                )
            }
            state.copy(selectedItems = newItems)
        }
        _userMessage.tryEmit("Добавлено: ${priceItem.name}")
    }

    fun addCustomItemToCalculation(
        name: String,
        category: String,
        unit: String,
        unitPrice: Double,
        quantity: Double,
        customNote: String = ""
    ) {
        _calcState.update { state ->
            val newItems = state.selectedItems.toMutableList()
            newItems.add(
                CalculatedItemData(
                    priceItemId = null,
                    category = category,
                    name = name,
                    unit = unit,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    totalPrice = unitPrice * quantity,
                    customNote = customNote
                )
            )
            state.copy(selectedItems = newItems)
        }
        _userMessage.tryEmit("Добавлена позиция: $name")
    }

    fun addMultipleItemsToCalculation(items: List<CalculatedItemData>) {
        if (items.isEmpty()) return
        _calcState.update { state ->
            val newItems = state.selectedItems.toMutableList()
            newItems.addAll(items)
            state.copy(selectedItems = newItems)
        }
        _userMessage.tryEmit("Добавлено позиций из конструктора: ${items.size}")
    }

    fun updateCalculatedItemQuantity(index: Int, newQuantity: Double) {
        _calcState.update { state ->
            if (index in state.selectedItems.indices) {
                val newItems = state.selectedItems.toMutableList()
                if (newQuantity <= 0.0) {
                    newItems.removeAt(index)
                } else {
                    val item = newItems[index]
                    newItems[index] = item.copy(
                        quantity = newQuantity,
                        totalPrice = newQuantity * item.unitPrice
                    )
                }
                state.copy(selectedItems = newItems)
            } else state
        }
    }

    fun updateItemQuantity(index: Int, newQuantity: Double) = updateCalculatedItemQuantity(index, newQuantity)

    fun updateCalculatedItemPrice(index: Int, newPrice: Double) {
        _calcState.update { state ->
            if (index in state.selectedItems.indices) {
                val newItems = state.selectedItems.toMutableList()
                val item = newItems[index]
                newItems[index] = item.copy(
                    unitPrice = newPrice,
                    totalPrice = item.quantity * newPrice
                )
                state.copy(selectedItems = newItems)
            } else state
        }
    }

    fun updateItemPrice(index: Int, newPrice: Double) = updateCalculatedItemPrice(index, newPrice)

    fun removeCalculatedItem(index: Int) {
        _calcState.update { state ->
            if (index in state.selectedItems.indices) {
                val newItems = state.selectedItems.toMutableList()
                newItems.removeAt(index)
                state.copy(selectedItems = newItems)
            } else state
        }
    }

    fun removeItemFromCalculation(index: Int) = removeCalculatedItem(index)

    fun clearCalculation() {
        _calcState.value = CalculationState()
        _userMessage.tryEmit("Калькулятор сброшен")
    }

    // --- Save & Order Management ---

    fun saveCurrentOrder(status: String = "DRAFT") {
        val state = _calcState.value
        if (state.selectedItems.isEmpty()) {
            _userMessage.tryEmit("Смета пуста! Добавьте товары или услуги")
            return
        }

        if (!isOnline.value) {
            _userMessage.tryEmit("⚠️ Нет подключения к интернету. Сохранение сметы доступно только онлайн в Firestore.")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val isNewOrder = state.editingOrderId == null
            val existing = if (!isNewOrder) repository.getOrderById(state.editingOrderId!!) else null
            val orderId = state.editingOrderId ?: java.util.UUID.randomUUID().toString()
            val createdAt = existing?.createdAt ?: now

            val order = SavedOrder(
                id = orderId,
                orderNumber = state.orderNumber,
                clientName = state.clientName.ifBlank { "Без имени" },
                clientPhone = state.clientPhone,
                deceasedName = state.deceasedName,
                cemeteryName = state.cemeteryName,
                plotNumber = state.plotNumber,
                createdAt = createdAt,
                updatedAt = now,
                status = status,
                subtotalAmount = state.subtotal,
                discountPercent = state.discountPercent,
                discountAmount = state.discountAmount,
                totalAmount = state.total,
                prepaymentAmount = state.prepayment,
                remainingAmount = state.remainingAmount,
                itemsJson = OrderJsonAdapter.toJson(state.selectedItems),
                notes = state.notes,
                syncStatus = SyncStatus.SYNCED,
                isDeleted = false
            )

            val saved = syncManager.saveOrderOnline(order)
            if (saved) {
                _userMessage.tryEmit("Смета ${order.orderNumber} сохранена в Firestore ☁️")
            } else {
                repository.saveOrder(order.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Смета ${order.orderNumber} сохранена 💾")
            }
            // Clear calculator for the next order
            _calcState.value = CalculationState()
        }
    }

    fun loadOrderToCalculator(order: SavedOrder) {
        val parsedItems = OrderJsonAdapter.fromJson(order.itemsJson)
        _calcState.value = CalculationState(
            orderNumber = order.orderNumber,
            clientName = order.clientName,
            clientPhone = order.clientPhone,
            deceasedName = order.deceasedName,
            cemeteryName = order.cemeteryName,
            plotNumber = order.plotNumber,
            notes = order.notes,
            selectedItems = parsedItems,
            discountPercent = order.discountPercent,
            prepayment = order.prepaymentAmount,
            editingOrderId = order.id
        )
        _currentScreen.value = AppScreen.CALCULATOR
        _userMessage.tryEmit("Смета ${order.orderNumber} загружена в калькулятор")
    }

    fun loadOrderIntoCalculator(order: SavedOrder) = loadOrderToCalculator(order)

    fun deleteSavedOrder(order: SavedOrder) {
        viewModelScope.launch {
            if (isOnline.value) {
                repository.hardDeleteOrder(order.id)
                syncManager.deleteOrderOnline(order.id)
            } else {
                repository.softDeleteOrder(order.id)
            }
            if (_calcState.value.editingOrderId == order.id) {
                _calcState.update { it.copy(editingOrderId = null) }
            }
            _userMessage.tryEmit("Смета ${order.orderNumber} удалена 🗑️")
        }
    }

    // --- Price Management Operations ---

    fun savePriceItem(item: PriceItem) {
        if (!isOnline.value) {
            _userMessage.tryEmit("⚠️ Нет подключения к интернету. Изменение расценок доступно только онлайн в Firestore.")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val itemToSave = if (item.id == 0L) {
                val generatedId = System.currentTimeMillis()
                item.copy(id = generatedId, updatedAt = now)
            } else {
                item.copy(updatedAt = now)
            }

            val saved = syncManager.savePriceItemOnline(itemToSave)
            if (saved) {
                _userMessage.tryEmit("Расценка «${itemToSave.name}» сохранена в Firestore ☁️")
            } else {
                _userMessage.tryEmit("⚠️ Не удалось сохранить расценку в Firestore.")
            }
        }
    }

    fun updatePriceItemCost(id: Long, newPrice: Double) {
        if (!isOnline.value) {
            _userMessage.tryEmit("⚠️ Нет подключения к интернету. Изменение цен доступно только онлайн в Firestore.")
            return
        }
        viewModelScope.launch {
            val item = allPrices.value.find { it.id == id }
            if (item != null) {
                val updated = item.copy(currentPrice = newPrice, updatedAt = System.currentTimeMillis())
                val saved = syncManager.savePriceItemOnline(updated)
                if (saved) {
                    _userMessage.tryEmit("Цена «${item.name}» изменена на ${PriceFormatter.formatRub(newPrice)} (Firestore)")
                } else {
                    _userMessage.tryEmit("⚠️ Не удалось обновить цену в Firestore.")
                }
            }
        }
    }

    fun togglePriceItemEnabled(item: PriceItem) {
        viewModelScope.launch {
            val updated = item.copy(isEnabled = !item.isEnabled, updatedAt = System.currentTimeMillis())
            repository.savePriceItemToLocalCacheOnly(updated)
            syncManager.savePriceItemOnline(updated)
            _userMessage.tryEmit(if (updated.isEnabled) "«${item.name}» включена в каталог 👁️" else "«${item.name}» скрыта из каталога 🙈")
        }
    }

    fun deletePriceItem(item: PriceItem) {
        viewModelScope.launch {
            repository.hardDeletePriceItem(item.id)
            _userMessage.tryEmit("Позиция «${item.name}» удалена 🗑️")
            syncManager.deletePriceItemOnline(item.id, item.name)
        }
    }

    // --- Stone Materials Operations ---

    fun saveStoneMaterial(material: StoneMaterialItem) {
        if (!isOnline.value) {
            _userMessage.tryEmit("⚠️ Нет подключения к интернету. Сохранение материалов доступно только онлайн.")
            return
        }
        viewModelScope.launch {
            val matToSave = material.copy(updatedAt = System.currentTimeMillis())
            val saved = syncManager.saveStoneMaterialOnline(matToSave)
            if (saved) {
                _userMessage.tryEmit("Материал «${matToSave.name}» сохранен в Firestore ☁️")
            } else {
                _userMessage.tryEmit("⚠️ Ошибка сохранения материала в Firestore.")
            }
        }
    }

    fun toggleStoneMaterialEnabled(material: StoneMaterialItem) {
        viewModelScope.launch {
            val updated = material.copy(isEnabled = !material.isEnabled, updatedAt = System.currentTimeMillis())
            repository.saveStoneMaterialToLocalCacheOnly(updated)
            syncManager.saveStoneMaterialOnline(updated)
            _userMessage.tryEmit(if (updated.isEnabled) "Камень «${material.name}» включен в конструктор 👁️" else "Камень «${material.name}» скрыт из конструктора 🙈")
        }
    }

    fun deleteStoneMaterial(material: StoneMaterialItem) {
        viewModelScope.launch {
            repository.hardDeleteStoneMaterial(material.id)
            _userMessage.tryEmit("Материал «${material.name}» удален 🗑️")
            syncManager.deleteStoneMaterialOnline(material.id, material.name)
        }
    }

    // --- Monument Size Presets Operations ---

    fun saveSizePreset(preset: MonumentSizePresetItem) {
        if (!isOnline.value) {
            _userMessage.tryEmit("⚠️ Нет подключения к интернету. Сохранение размеров доступно только онлайн.")
            return
        }
        viewModelScope.launch {
            val presetToSave = preset.copy(updatedAt = System.currentTimeMillis())
            val saved = syncManager.saveSizePresetOnline(presetToSave)
            if (saved) {
                _userMessage.tryEmit("Типоразмер «${presetToSave.name}» сохранен в Firestore ☁️")
            } else {
                _userMessage.tryEmit("⚠️ Ошибка сохранения типоразмера в Firestore.")
            }
        }
    }

    fun toggleSizePresetEnabled(preset: MonumentSizePresetItem) {
        viewModelScope.launch {
            val updated = preset.copy(isEnabled = !preset.isEnabled, updatedAt = System.currentTimeMillis())
            repository.saveSizePresetToLocalCacheOnly(updated)
            syncManager.saveSizePresetOnline(updated)
            _userMessage.tryEmit(if (updated.isEnabled) "Размер «${preset.name}» включен 👁️" else "Размер «${preset.name}» скрыт 🙈")
        }
    }

    fun deleteSizePreset(preset: MonumentSizePresetItem) {
        viewModelScope.launch {
            repository.hardDeleteSizePreset(preset.id)
            _userMessage.tryEmit("Размер «${preset.name}» удален 🗑️")
            syncManager.deleteSizePresetOnline(preset.id, preset.name)
        }
    }

    // --- Constructor Service Prices Operations ---

    fun refreshNbrbRates(showUserFeedback: Boolean = true) {
        viewModelScope.launch {
            val usdResult = com.example.network.NbrbCurrencyFetcher.fetchUsdRateWithDetails()
            val eurResult = com.example.network.NbrbCurrencyFetcher.fetchEurRateWithDetails()
            
            var updatedAny = false
            if (usdResult != null && usdResult.rate > 0) {
                val formattedRate = "%.4f".format(java.util.Locale.US, usdResult.rate).toDoubleOrNull() ?: usdResult.rate
                updateUsdExchangeRate(formattedRate, showFeedback = false, sourceLabel = usdResult.source)
                updatedAny = true
            }
            if (eurResult != null && eurResult.rate > 0) {
                val formattedRate = "%.4f".format(java.util.Locale.US, eurResult.rate).toDoubleOrNull() ?: eurResult.rate
                updateEurExchangeRate(formattedRate, showFeedback = false, sourceLabel = eurResult.source)
                updatedAny = true
            }

            if (showUserFeedback) {
                if (updatedAny) {
                    val usdStr = usdResult?.let { "%.2f".format(it.rate) } ?: "—"
                    val eurStr = eurResult?.let { "%.2f".format(it.rate) } ?: "—"
                    _userMessage.tryEmit("Курсы НБ РБ обновлены: 1$ = $usdStr BYN, 1€ = $eurStr BYN ☁️")
                } else {
                    _userMessage.tryEmit("Не удалось подключиться к серверу Нацбанка РБ. Проверьте интернет-соединение.")
                }
            }
        }
    }

    fun refreshNbrbUsdRate(showUserFeedback: Boolean = true) {
        viewModelScope.launch {
            val result = com.example.network.NbrbCurrencyFetcher.fetchUsdRateWithDetails()
            if (result != null && result.rate > 0) {
                val formattedRate = "%.4f".format(java.util.Locale.US, result.rate).toDoubleOrNull() ?: result.rate
                updateUsdExchangeRate(formattedRate, showFeedback = showUserFeedback, sourceLabel = result.source)
            } else if (showUserFeedback) {
                _userMessage.tryEmit("Не удалось подключиться к серверу Нацбанка РБ. Проверьте интернет-соединение.")
            }
        }
    }

    fun refreshNbrbEurRate(showUserFeedback: Boolean = true) {
        viewModelScope.launch {
            val result = com.example.network.NbrbCurrencyFetcher.fetchEurRateWithDetails()
            if (result != null && result.rate > 0) {
                val formattedRate = "%.4f".format(java.util.Locale.US, result.rate).toDoubleOrNull() ?: result.rate
                updateEurExchangeRate(formattedRate, showFeedback = showUserFeedback, sourceLabel = result.source)
            } else if (showUserFeedback) {
                _userMessage.tryEmit("Не удалось подключиться к серверу Нацбанка РБ. Проверьте интернет-соединение.")
            }
        }
    }

    fun updateUsdExchangeRate(newRate: Double, showFeedback: Boolean = true, sourceLabel: String? = null) {
        if (!isOnline.value) {
            if (showFeedback) {
                _userMessage.tryEmit("⚠️ Нет подключения к интернету. Обновление курса USD доступно только онлайн.")
            }
            return
        }
        viewModelScope.launch {
            val existing = constructorServicePrices.value.find { it.key == "usd_exchange_rate" }
            val itemToSave = if (existing != null) {
                existing.copy(price = newRate, updatedAt = System.currentTimeMillis())
            } else {
                ConstructorServicePriceItem(
                    key = "usd_exchange_rate",
                    groupName = "Настройки",
                    title = "Курс USD к BYN",
                    price = newRate,
                    description = "Курс доллара для расчетов и отображения",
                    sortOrder = 0,
                    updatedAt = System.currentTimeMillis()
                )
            }
            val saved = syncManager.saveServicePriceOnline(itemToSave)
            if (showFeedback && saved) {
                val labelMsg = if (sourceLabel != null) " от $sourceLabel" else ""
                _userMessage.tryEmit("Курс USD$labelMsg обновлен: $newRate BYN (Firestore) ☁️")
            }
        }
    }

    fun updateEurExchangeRate(newRate: Double, showFeedback: Boolean = true, sourceLabel: String? = null) {
        if (!isOnline.value) {
            if (showFeedback) {
                _userMessage.tryEmit("⚠️ Нет подключения к интернету. Обновление курса EUR доступно только онлайн.")
            }
            return
        }
        viewModelScope.launch {
            val existing = constructorServicePrices.value.find { it.key == "eur_exchange_rate" }
            val itemToSave = if (existing != null) {
                existing.copy(price = newRate, updatedAt = System.currentTimeMillis())
            } else {
                ConstructorServicePriceItem(
                    key = "eur_exchange_rate",
                    groupName = "Настройки",
                    title = "Курс EUR к BYN",
                    price = newRate,
                    description = "Курс евро для расчетов сусального золота и отображения",
                    sortOrder = 1,
                    updatedAt = System.currentTimeMillis()
                )
            }
            val saved = syncManager.saveServicePriceOnline(itemToSave)
            if (showFeedback && saved) {
                val labelMsg = if (sourceLabel != null) " от $sourceLabel" else ""
                _userMessage.tryEmit("Курс EUR$labelMsg обновлен: $newRate BYN (Firestore) ☁️")
            }
        }
    }

    fun saveConstructorServicePrice(item: ConstructorServicePriceItem) {
        viewModelScope.launch {
            val itemToSave = item.copy(updatedAt = System.currentTimeMillis())
            if (isOnline.value) {
                val saved = syncManager.saveServicePriceOnline(itemToSave)
                if (saved) {
                    _userMessage.tryEmit("Тариф «${itemToSave.title}» сохранен в Firestore ☁️")
                } else {
                    repository.saveServicePriceToLocalCacheOnly(itemToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Тариф «${itemToSave.title}» сохранен в Room 📱")
                }
            } else {
                repository.saveServicePriceToLocalCacheOnly(itemToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Тариф «${itemToSave.title}» сохранен в Room (Офлайн) 📱")
            }
        }
    }

    fun deleteConstructorServicePrice(item: ConstructorServicePriceItem) {
        viewModelScope.launch {
            repository.hardDeleteServicePriceByKey(item.key)
            _userMessage.tryEmit("Тариф «${item.title}» удален 🗑️")
            if (isOnline.value) {
                syncManager.deleteServicePriceOnline(item.key, item.title)
            }
        }
    }

    fun updateConstructorPriceByKey(key: String, newPrice: Double) {
        viewModelScope.launch {
            val existingInMemory = constructorServicePrices.value.find { it.key == key }
            val existingInDb = repository.getServicePriceByKey(key)
            val itemToUpdate = existingInMemory ?: existingInDb

            val itemToSave = if (itemToUpdate != null) {
                itemToUpdate.copy(price = newPrice, updatedAt = System.currentTimeMillis())
            } else {
                val defaultItem = DefaultCatalog.getDefaultConstructorServicePrices().find { it.key == key }
                ConstructorServicePriceItem(
                    key = key,
                    groupName = defaultItem?.groupName ?: "Тарифы",
                    title = defaultItem?.title ?: key,
                    price = newPrice,
                    description = defaultItem?.description ?: "",
                    sortOrder = defaultItem?.sortOrder ?: 99,
                    updatedAt = System.currentTimeMillis()
                )
            }

            if (isOnline.value) {
                val saved = syncManager.saveServicePriceOnline(itemToSave)
                if (saved) {
                    _userMessage.tryEmit("Тариф «${itemToSave.title}» изменен на ${PriceFormatter.formatRub(newPrice)} (Firestore) ☁️")
                } else {
                    repository.saveServicePriceToLocalCacheOnly(itemToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Тариф «${itemToSave.title}» изменен на ${PriceFormatter.formatRub(newPrice)} (Room) 📱")
                }
            } else {
                repository.saveServicePriceToLocalCacheOnly(itemToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Тариф «${itemToSave.title}» изменен на ${PriceFormatter.formatRub(newPrice)} (Офлайн) 📱")
            }
        }
    }

    // --- Engraving Fonts Operations ---

    fun saveFont(font: EngravingFontItem) {
        viewModelScope.launch {
            val fontToSave = font.copy(updatedAt = System.currentTimeMillis())
            if (isOnline.value) {
                val saved = syncManager.saveFontOnline(fontToSave)
                if (saved) {
                    _userMessage.tryEmit("Шрифт «${fontToSave.name}» сохранен в Firestore ☁️")
                } else {
                    repository.saveFontToLocalCacheOnly(fontToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Шрифт «${fontToSave.name}» сохранен в Room 📱")
                }
            } else {
                repository.saveFontToLocalCacheOnly(fontToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Шрифт «${fontToSave.name}» сохранен в Room (Офлайн) 📱")
            }
        }
    }

    fun toggleFontEnabled(font: EngravingFontItem) {
        viewModelScope.launch {
            val updated = font.copy(isEnabled = !font.isEnabled, updatedAt = System.currentTimeMillis())
            repository.saveFontToLocalCacheOnly(updated)
            if (isOnline.value) {
                syncManager.saveFontOnline(updated)
            }
            _userMessage.tryEmit(if (updated.isEnabled) "Шрифт «${font.name}» включен 👁️" else "Шрифт «${font.name}» скрыт 🙈")
        }
    }

    fun deleteFont(font: EngravingFontItem) {
        viewModelScope.launch {
            repository.hardDeleteFont(font.id)
            _userMessage.tryEmit("Шрифт «${font.name}» удален 🗑️")
            if (isOnline.value) {
                syncManager.deleteFontOnline(font.id, font.name)
            }
        }
    }

    fun resetFontsToDefault() {
        viewModelScope.launch {
            repository.resetEngravingFonts()
            _userMessage.tryEmit("Список шрифтов сброшен к заводским настройкам 🔄")
        }
    }

    // --- Engraving Drawings Operations ---

    fun saveDrawing(drawing: EngravingDrawingItem) {
        viewModelScope.launch {
            val drawingToSave = drawing.copy(updatedAt = System.currentTimeMillis())
            if (isOnline.value) {
                val saved = syncManager.saveDrawingOnline(drawingToSave)
                if (saved) {
                    _userMessage.tryEmit("Рисунок «${drawingToSave.code} ${drawingToSave.name}» сохранен в Firestore ☁️")
                } else {
                    repository.saveDrawingToLocalCacheOnly(drawingToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Рисунок «${drawingToSave.code} ${drawingToSave.name}» сохранен в Room 📱")
                }
            } else {
                repository.saveDrawingToLocalCacheOnly(drawingToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Рисунок «${drawingToSave.code} ${drawingToSave.name}» сохранен в Room (Офлайн) 📱")
            }
        }
    }

    fun toggleDrawingEnabled(drawing: EngravingDrawingItem) {
        viewModelScope.launch {
            val updated = drawing.copy(isEnabled = !drawing.isEnabled, updatedAt = System.currentTimeMillis())
            repository.saveDrawingToLocalCacheOnly(updated)
            if (isOnline.value) {
                syncManager.saveDrawingOnline(updated)
            }
            _userMessage.tryEmit(if (updated.isEnabled) "Рисунок «${drawing.code} ${drawing.name}» включен 👁️" else "Рисунок «${drawing.code} ${drawing.name}» скрыт 🙈")
        }
    }

    fun deleteDrawing(drawing: EngravingDrawingItem) {
        viewModelScope.launch {
            repository.hardDeleteDrawing(drawing.id)
            _userMessage.tryEmit("Рисунок «${drawing.code} ${drawing.name}» удален 🗑️")
            if (isOnline.value) {
                syncManager.deleteDrawingOnline(drawing.id, drawing.name)
            }
        }
    }

    fun resetDrawingsToDefault() {
        viewModelScope.launch {
            repository.resetEngravingDrawings()
            _userMessage.tryEmit("Каталог рисунков сброшен к заводским настройкам 🔄")
        }
    }

    // --- Photo Sizes Operations ---

    fun savePhotoSize(photoSize: PhotoSizeItem) {
        viewModelScope.launch {
            val sizeToSave = photoSize.copy(updatedAt = System.currentTimeMillis())
            if (isOnline.value) {
                val saved = syncManager.savePhotoSizeOnline(sizeToSave)
                if (saved) {
                    _userMessage.tryEmit("Размер фото «${sizeToSave.category} ${sizeToSave.sizeName}» сохранен в Firestore ☁️")
                } else {
                    repository.savePhotoSizeToLocalCacheOnly(sizeToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Размер фото «${sizeToSave.category} ${sizeToSave.sizeName}» сохранен в Room 📱")
                }
            } else {
                repository.savePhotoSizeToLocalCacheOnly(sizeToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Размер фото «${sizeToSave.category} ${sizeToSave.sizeName}» сохранен в Room (Офлайн) 📱")
            }
        }
    }

    fun togglePhotoSizeEnabled(photoSize: PhotoSizeItem) {
        viewModelScope.launch {
            val updated = photoSize.copy(isEnabled = !photoSize.isEnabled, updatedAt = System.currentTimeMillis())
            repository.savePhotoSizeToLocalCacheOnly(updated)
            if (isOnline.value) {
                syncManager.savePhotoSizeOnline(updated)
            }
            _userMessage.tryEmit(if (updated.isEnabled) "Размер «${photoSize.sizeName}» включен 👁️" else "Размер «${photoSize.sizeName}» скрыт 🙈")
        }
    }

    fun deletePhotoSize(photoSize: PhotoSizeItem) {
        viewModelScope.launch {
            repository.hardDeletePhotoSize(photoSize.id)
            _userMessage.tryEmit("Размер фото «${photoSize.sizeName}» удален 🗑️")
            if (isOnline.value) {
                syncManager.deletePhotoSizeOnline(photoSize.id, photoSize.sizeName)
            }
        }
    }

    fun resetPhotoSizesToDefault() {
        viewModelScope.launch {
            repository.resetPhotoSizes()
            _userMessage.tryEmit("Размеры фото сброшены к заводским настройкам 🔄")
        }
    }

    // --- Photo Frames Operations ---

    fun savePhotoFrame(frame: PhotoFrameItem) {
        viewModelScope.launch {
            val frameToSave = frame.copy(updatedAt = System.currentTimeMillis())
            if (isOnline.value) {
                val saved = syncManager.savePhotoFrameOnline(frameToSave)
                if (saved) {
                    _userMessage.tryEmit("Рамка «${frameToSave.name}» сохранена в Firestore ☁️")
                } else {
                    repository.savePhotoFrameToLocalCacheOnly(frameToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Рамка «${frameToSave.name}» сохранена в Room 📱")
                }
            } else {
                repository.savePhotoFrameToLocalCacheOnly(frameToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Рамка «${frameToSave.name}» сохранена в Room (Офлайн) 📱")
            }
        }
    }

    fun togglePhotoFrameEnabled(frame: PhotoFrameItem) {
        viewModelScope.launch {
            val updated = frame.copy(isEnabled = !frame.isEnabled, updatedAt = System.currentTimeMillis())
            repository.savePhotoFrameToLocalCacheOnly(updated)
            if (isOnline.value) {
                syncManager.savePhotoFrameOnline(updated)
            }
            _userMessage.tryEmit(if (updated.isEnabled) "Рамка «${frame.name}» включена 👁️" else "Рамка «${frame.name}» скрыта 🙈")
        }
    }

    fun deletePhotoFrame(frame: PhotoFrameItem) {
        viewModelScope.launch {
            repository.hardDeletePhotoFrame(frame.id)
            _userMessage.tryEmit("Рамка/оформление «${frame.name}» удалена 🗑️")
            if (isOnline.value) {
                syncManager.deletePhotoFrameOnline(frame.id, frame.name)
            }
        }
    }

    fun resetPhotoFramesToDefault() {
        viewModelScope.launch {
            repository.resetPhotoFrames()
            _userMessage.tryEmit("Рамки сброшены к заводским настройкам 🔄")
        }
    }

    // --- Vases Operations ---

    fun saveVase(vase: VaseItem) {
        viewModelScope.launch {
            val vaseToSave = vase.copy(updatedAt = System.currentTimeMillis())
            if (isOnline.value) {
                val saved = syncManager.saveVaseOnline(vaseToSave)
                if (saved) {
                    _userMessage.tryEmit("Ваза «${vaseToSave.name}» сохранена в Firestore ☁️")
                } else {
                    repository.saveVaseToLocalCacheOnly(vaseToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                    _userMessage.tryEmit("Ваза «${vaseToSave.name}» сохранена в Room 📱")
                }
            } else {
                repository.saveVaseToLocalCacheOnly(vaseToSave.copy(syncStatus = SyncStatus.PENDING_PUSH))
                _userMessage.tryEmit("Ваза «${vaseToSave.name}» сохранена в Room (Офлайн) 📱")
            }
        }
    }

    fun toggleVaseEnabled(vase: VaseItem) {
        viewModelScope.launch {
            val updated = vase.copy(isEnabled = !vase.isEnabled, updatedAt = System.currentTimeMillis())
            repository.saveVaseToLocalCacheOnly(updated)
            if (isOnline.value) {
                syncManager.saveVaseOnline(updated)
            }
            _userMessage.tryEmit(if (updated.isEnabled) "Ваза «${vase.name}» включена 👁️" else "Ваза «${vase.name}» скрыта 🙈")
        }
    }

    fun deleteVase(vase: VaseItem) {
        viewModelScope.launch {
            repository.hardDeleteVase(vase.id)
            _userMessage.tryEmit("Ваза «${vase.name}» удалена 🗑️")
            if (isOnline.value) {
                syncManager.deleteVaseOnline(vase.id, vase.name)
            }
        }
    }

    fun resetVasesToDefault() {
        viewModelScope.launch {
            repository.resetVases()
            _userMessage.tryEmit("Каталог ваз сброшен к заводским настройкам 🔄")
        }
    }




    fun refreshLocalCacheFromFirestore() {
        viewModelScope.launch {
            if (!isOnline.value) {
                _userMessage.tryEmit("Нет подключения к сети для обновления кэша из Firestore")
                return@launch
            }
            val res = syncManager.refreshLocalCacheFromFirestore()
            res.onSuccess { summary ->
                _userMessage.tryEmit("Обновлен локальный кэш из Firestore: ${summary.ordersCount} смет, ${summary.pricesCount} расценок, ${summary.materialsCount} камней, ${summary.fontsCount} шрифтов, ${summary.drawingsCount} рисунков, ${summary.photoSizesCount} фото, ${summary.photoFramesCount} рамок, ${summary.vasesCount} ваз ☁️")
            }.onFailure { err ->
                _userMessage.tryEmit("Ошибка загрузки из Firestore: ${err.localizedMessage}")
            }
        }
    }

    fun forceUploadLocalDataToFirestore() {
        viewModelScope.launch {
            if (!isOnline.value) {
                _userMessage.tryEmit("⚠️ Нет подключения к интернету для выгрузки в Firestore")
                return@launch
            }
            _userMessage.tryEmit("Выгрузка базы данных в Firestore... ☁️")
            val res = syncManager.uploadAllDataToFirestore()
            res.onSuccess { summary ->
                _userMessage.tryEmit("Успешно выгружено в Firestore: ${summary.pricesCount} расценок, ${summary.materialsCount} материалов, ${summary.fontsCount} шрифтов, ${summary.drawingsCount} рисунков, ${summary.photoSizesCount} фото, ${summary.photoFramesCount} рамок, ${summary.vasesCount} ваз, ${summary.ordersCount} смет ☁️")
            }.onFailure { err ->
                _userMessage.tryEmit("Ошибка выгрузки в Firestore: ${err.localizedMessage ?: err.message}")
            }
        }
    }

    fun testCloudConnection() {
        viewModelScope.launch {
            if (!isOnline.value) {
                _userMessage.tryEmit("Устройство находится в офлайн-режиме (работает через Room кэш) 🔴")
                return@launch
            }
            _userMessage.tryEmit("Проверка связи с Firestore... 📡")
            val res = syncManager.testConnection()
            res.onSuccess { msg ->
                _userMessage.tryEmit(msg)
            }.onFailure { err ->
                _userMessage.tryEmit("🔴 Ошибка Firestore: ${err.localizedMessage ?: err.message}")
            }
        }
    }

    fun clearRoomAndFirestore() {
        viewModelScope.launch {
            _userMessage.tryEmit("Очистка Room и Firestore... 🗑️")
            val res = syncManager.clearBothRoomAndFirestore()
            res.onSuccess {
                _userMessage.tryEmit("Базы данных Room и Firestore полностью очищены 🗑️")
            }.onFailure { err ->
                _userMessage.tryEmit("Ошибка при очистке баз данных: ${err.localizedMessage}")
            }
        }
    }

    val databaseIdFlow: StateFlow<String> = MutableStateFlow(syncManager.getActiveDatabaseId()).asStateFlow()

    fun updateDatabaseId(newId: String) {
        syncManager.setDatabaseId(newId)
        (databaseIdFlow as? MutableStateFlow)?.value = syncManager.getActiveDatabaseId()
        _userMessage.tryEmit("Идентификатор базы установлен: ${syncManager.getActiveDatabaseId()}")
        testCloudConnection()
    }

    // --- JSON Database Backup & Restore ---

    fun exportDatabaseBackup(context: Context) {
        viewModelScope.launch {
            try {
                val orders = repository.getAllOrdersSync()
                val prices = repository.getAllPriceItemsSync()
                val stones = repository.getAllMaterialsSync()
                val presets = repository.getAllPresetsSync()
                val services = repository.getAllServicePricesSync()
                val fonts = repository.getAllFontsSync()
                val drawings = repository.getAllDrawingsSync()
                val photoSizes = repository.getAllPhotoSizesSync()
                val photoFrames = repository.getAllPhotoFramesSync()
                val vases = repository.getAllVasesSync()

                val root = org.json.JSONObject()
                root.put("version", 1)
                root.put("timestamp", System.currentTimeMillis())
                root.put("appName", "RitualCalc")

                // Orders
                val ordersArr = org.json.JSONArray()
                for (o in orders) {
                    val jo = org.json.JSONObject()
                    jo.put("id", o.id)
                    jo.put("orderNumber", o.orderNumber)
                    jo.put("clientName", o.clientName)
                    jo.put("clientPhone", o.clientPhone)
                    jo.put("deceasedName", o.deceasedName)
                    jo.put("cemeteryName", o.cemeteryName)
                    jo.put("plotNumber", o.plotNumber)
                    jo.put("createdAt", o.createdAt)
                    jo.put("status", o.status)
                    jo.put("subtotalAmount", o.subtotalAmount)
                    jo.put("discountPercent", o.discountPercent)
                    jo.put("discountAmount", o.discountAmount)
                    jo.put("totalAmount", o.totalAmount)
                    jo.put("prepaymentAmount", o.prepaymentAmount)
                    jo.put("remainingAmount", o.remainingAmount)
                    jo.put("itemsJson", o.itemsJson)
                    jo.put("notes", o.notes)
                    ordersArr.put(jo)
                }
                root.put("orders", ordersArr)

                // Prices
                val pricesArr = org.json.JSONArray()
                for (p in prices) {
                    val jp = org.json.JSONObject()
                    jp.put("id", p.id)
                    jp.put("category", p.category)
                    jp.put("subcategory", p.subcategory)
                    jp.put("name", p.name)
                    jp.put("unit", p.unit)
                    jp.put("defaultPrice", p.defaultPrice)
                    jp.put("currentPrice", p.currentPrice)
                    jp.put("description", p.description)
                    jp.put("isCustom", p.isCustom)
                    jp.put("isEnabled", p.isEnabled)
                    jp.put("sortOrder", p.sortOrder)
                    pricesArr.put(jp)
                }
                root.put("prices", pricesArr)

                // Stones
                val stonesArr = org.json.JSONArray()
                for (s in stones) {
                    val js = org.json.JSONObject()
                    js.put("id", s.id)
                    js.put("name", s.name)
                    js.put("colorName", s.colorName)
                    js.put("pricePerM3", s.pricePerM3)
                    js.put("origin", s.origin)
                    js.put("description", s.description)
                    js.put("suitableForDirectEngraving", s.suitableForDirectEngraving)
                    js.put("isCustom", s.isCustom)
                    js.put("isEnabled", s.isEnabled)
                    js.put("sortOrder", s.sortOrder)
                    stonesArr.put(js)
                }
                root.put("stones", stonesArr)

                // Presets
                val presetsArr = org.json.JSONArray()
                for (pr in presets) {
                    val jpr = org.json.JSONObject()
                    jpr.put("id", pr.id)
                    jpr.put("name", pr.name)
                    jpr.put("heightCm", pr.heightCm)
                    jpr.put("widthCm", pr.widthCm)
                    jpr.put("thicknessCm", pr.thicknessCm)
                    jpr.put("steleBasePrice", pr.steleBasePrice)
                    jpr.put("plinthBasePrice", pr.plinthBasePrice)
                    jpr.put("flowerbedBasePrice", pr.flowerbedBasePrice)
                    jpr.put("plinthDimensions", pr.plinthDimensions)
                    jpr.put("flowerbedDimensions", pr.flowerbedDimensions)
                    jpr.put("isFamily", pr.isFamily)
                    jpr.put("isCustom", pr.isCustom)
                    jpr.put("isEnabled", pr.isEnabled)
                    jpr.put("sortOrder", pr.sortOrder)
                    presetsArr.put(jpr)
                }
                root.put("presets", presetsArr)

                // Services
                val servArr = org.json.JSONArray()
                for (srv in services) {
                    val jsrv = org.json.JSONObject()
                    jsrv.put("key", srv.key)
                    jsrv.put("groupName", srv.groupName)
                    jsrv.put("title", srv.title)
                    jsrv.put("unit", srv.unit)
                    jsrv.put("price", srv.price)
                    jsrv.put("description", srv.description)
                    jsrv.put("sortOrder", srv.sortOrder)
                    servArr.put(jsrv)
                }
                root.put("servicePrices", servArr)

                // Fonts
                val fontsArr = org.json.JSONArray()
                for (f in fonts) {
                    val jf = org.json.JSONObject()
                    jf.put("id", f.id)
                    jf.put("name", f.name)
                    jf.put("styleKey", f.styleKey)
                    jf.put("price", f.price)
                    jf.put("sampleText", f.sampleText)
                    jf.put("description", f.description)
                    jf.put("isCustom", f.isCustom)
                    jf.put("isEnabled", f.isEnabled)
                    jf.put("sortOrder", f.sortOrder)
                    fontsArr.put(jf)
                }
                root.put("fonts", fontsArr)

                // Drawings
                val drawingsArr = org.json.JSONArray()
                for (d in drawings) {
                    val jd = org.json.JSONObject()
                    jd.put("id", d.id)
                    jd.put("category", d.category)
                    jd.put("code", d.code)
                    jd.put("name", d.name)
                    jd.put("price", d.price)
                    jd.put("description", d.description)
                    jd.put("isCustom", d.isCustom)
                    jd.put("isEnabled", d.isEnabled)
                    jd.put("sortOrder", d.sortOrder)
                    drawingsArr.put(jd)
                }
                root.put("drawings", drawingsArr)

                // Photo sizes
                val photoSizesArr = org.json.JSONArray()
                for (ps in photoSizes) {
                    val jps = org.json.JSONObject()
                    jps.put("id", ps.id)
                    jps.put("category", ps.category)
                    jps.put("sizeName", ps.sizeName)
                    jps.put("price", ps.price)
                    jps.put("description", ps.description)
                    jps.put("isCustom", ps.isCustom)
                    jps.put("isEnabled", ps.isEnabled)
                    jps.put("sortOrder", ps.sortOrder)
                    photoSizesArr.put(jps)
                }
                root.put("photoSizes", photoSizesArr)

                // Photo frames
                val photoFramesArr = org.json.JSONArray()
                for (pf in photoFrames) {
                    val jpf = org.json.JSONObject()
                    jpf.put("id", pf.id)
                    jpf.put("name", pf.name)
                    jpf.put("materialType", pf.materialType)
                    jpf.put("price", pf.price)
                    jpf.put("description", pf.description)
                    jpf.put("isCustom", pf.isCustom)
                    jpf.put("isEnabled", pf.isEnabled)
                    jpf.put("sortOrder", pf.sortOrder)
                    photoFramesArr.put(jpf)
                }
                root.put("photoFrames", photoFramesArr)

                // Vases
                val vasesArr = org.json.JSONArray()
                for (v in vases) {
                    val jv = org.json.JSONObject()
                    jv.put("id", v.id)
                    jv.put("name", v.name)
                    jv.put("materialType", v.materialType)
                    jv.put("sizeCm", v.sizeCm)
                    jv.put("price", v.price)
                    jv.put("description", v.description)
                    jv.put("isCustom", v.isCustom)
                    jv.put("isEnabled", v.isEnabled)
                    jv.put("sortOrder", v.sortOrder)
                    vasesArr.put(jv)
                }
                root.put("vases", vasesArr)

                val jsonContent = root.toString(2)
                val backupFile = java.io.File(context.cacheDir, "ritual_backup_${System.currentTimeMillis()}.json")
                backupFile.writeText(jsonContent, Charsets.UTF_8)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, jsonContent)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Экспорт базы (${orders.size} смет, ${prices.size} товаров, ${fonts.size} шрифтов, ${drawings.size} рисунков, ${photoSizes.size} фото, ${photoFrames.size} рамок, ${vases.size} ваз)")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                _userMessage.tryEmit("Файл резервной копии сформирован (${orders.size} смет, ${prices.size} товаров, ${fonts.size} шрифтов, ${drawings.size} рисунков, ${photoSizes.size} фото, ${photoFrames.size} рамок, ${vases.size} ваз)")
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка экспорта резервной копии: ${e.message}")
            }
        }
    }

    fun importDatabaseBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonString)
                var ordersCount = 0
                var pricesCount = 0
                var stonesCount = 0
                var presetsCount = 0
                var servicesCount = 0
                var fontsCount = 0
                var drawingsCount = 0
                var photoSizesCount = 0
                var photoFramesCount = 0
                var vasesCount = 0

                val ordersArr = root.optJSONArray("orders")
                if (ordersArr != null) {
                    for (i in 0 until ordersArr.length()) {
                        val jo = ordersArr.getJSONObject(i)
                        val idStr = jo.optString("id").ifBlank {
                            jo.optLong("id", 0L).takeIf { it > 0 }?.toString() ?: java.util.UUID.randomUUID().toString()
                        }
                        val order = SavedOrder(
                            id = idStr,
                            orderNumber = jo.optString("orderNumber"),
                            clientName = jo.optString("clientName"),
                            clientPhone = jo.optString("clientPhone"),
                            deceasedName = jo.optString("deceasedName"),
                            cemeteryName = jo.optString("cemeteryName"),
                            plotNumber = jo.optString("plotNumber"),
                            createdAt = jo.optLong("createdAt", System.currentTimeMillis()),
                            status = jo.optString("status", "DRAFT"),
                            subtotalAmount = jo.optDouble("subtotalAmount"),
                            discountPercent = jo.optDouble("discountPercent"),
                            discountAmount = jo.optDouble("discountAmount"),
                            totalAmount = jo.optDouble("totalAmount"),
                            prepaymentAmount = jo.optDouble("prepaymentAmount"),
                            remainingAmount = jo.optDouble("remainingAmount"),
                            itemsJson = jo.optString("itemsJson", "[]"),
                            notes = jo.optString("notes")
                        )
                        repository.saveOrder(order)
                        ordersCount++
                    }
                }

                val pricesArr = root.optJSONArray("prices")
                if (pricesArr != null) {
                    for (i in 0 until pricesArr.length()) {
                        val jp = pricesArr.getJSONObject(i)
                        val item = PriceItem(
                            id = jp.optLong("id"),
                            category = jp.optString("category", ItemCategory.MONUMENTS.name),
                            subcategory = jp.optString("subcategory"),
                            name = jp.optString("name"),
                            unit = jp.optString("unit", "шт"),
                            defaultPrice = jp.optDouble("defaultPrice"),
                            currentPrice = jp.optDouble("currentPrice"),
                            description = jp.optString("description"),
                            isCustom = jp.optBoolean("isCustom"),
                            isEnabled = jp.optBoolean("isEnabled", true),
                            sortOrder = jp.optInt("sortOrder")
                        )
                        repository.insertPriceItem(item)
                        pricesCount++
                    }
                }

                val stonesArr = root.optJSONArray("stones")
                if (stonesArr != null) {
                    for (i in 0 until stonesArr.length()) {
                        val js = stonesArr.getJSONObject(i)
                        val mat = StoneMaterialItem(
                            id = js.optString("id"),
                            name = js.optString("name"),
                            colorName = js.optString("colorName"),
                            pricePerM3 = js.optDouble("pricePerM3"),
                            origin = js.optString("origin"),
                            description = js.optString("description"),
                            suitableForDirectEngraving = js.optBoolean("suitableForDirectEngraving", true),
                            isCustom = js.optBoolean("isCustom"),
                            isEnabled = js.optBoolean("isEnabled", true),
                            sortOrder = js.optInt("sortOrder")
                        )
                        repository.insertStoneMaterial(mat)
                        stonesCount++
                    }
                }

                val presetsArr = root.optJSONArray("presets")
                if (presetsArr != null) {
                    for (i in 0 until presetsArr.length()) {
                        val jpr = presetsArr.getJSONObject(i)
                        val preset = MonumentSizePresetItem(
                            id = jpr.optString("id"),
                            name = jpr.optString("name"),
                            heightCm = jpr.optInt("heightCm"),
                            widthCm = jpr.optInt("widthCm"),
                            thicknessCm = jpr.optInt("thicknessCm"),
                            steleBasePrice = jpr.optDouble("steleBasePrice"),
                            plinthBasePrice = jpr.optDouble("plinthBasePrice"),
                            flowerbedBasePrice = jpr.optDouble("flowerbedBasePrice"),
                            plinthDimensions = jpr.optString("plinthDimensions"),
                            flowerbedDimensions = jpr.optString("flowerbedDimensions"),
                            isFamily = jpr.optBoolean("isFamily"),
                            isCustom = jpr.optBoolean("isCustom"),
                            isEnabled = jpr.optBoolean("isEnabled", true),
                            sortOrder = jpr.optInt("sortOrder")
                        )
                        repository.insertSizePreset(preset)
                        presetsCount++
                    }
                }

                val servArr = root.optJSONArray("servicePrices")
                if (servArr != null) {
                    for (i in 0 until servArr.length()) {
                        val jsrv = servArr.getJSONObject(i)
                        val item = ConstructorServicePriceItem(
                            key = jsrv.optString("key"),
                            groupName = jsrv.optString("groupName"),
                            title = jsrv.optString("title"),
                            unit = jsrv.optString("unit", "BYN"),
                            price = jsrv.optDouble("price"),
                            description = jsrv.optString("description"),
                            sortOrder = jsrv.optInt("sortOrder")
                        )
                        repository.insertServicePrice(item)
                        servicesCount++
                    }
                }

                val fontsArr = root.optJSONArray("fonts")
                if (fontsArr != null) {
                    for (i in 0 until fontsArr.length()) {
                        val jf = fontsArr.getJSONObject(i)
                        val font = EngravingFontItem(
                            id = jf.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = jf.optString("name"),
                            styleKey = jf.optString("styleKey", "SERIF"),
                            price = jf.optDouble("price", 0.0),
                            sampleText = jf.optString("sampleText", "Иванов Иван Иванович\n1950 — 2024"),
                            description = jf.optString("description", ""),
                            isCustom = jf.optBoolean("isCustom"),
                            isEnabled = jf.optBoolean("isEnabled", true),
                            sortOrder = jf.optInt("sortOrder")
                        )
                        repository.insertFont(font)
                        fontsCount++
                    }
                }

                val drawingsArr = root.optJSONArray("drawings")
                if (drawingsArr != null) {
                    for (i in 0 until drawingsArr.length()) {
                        val jd = drawingsArr.getJSONObject(i)
                        val drawing = EngravingDrawingItem(
                            id = jd.optString("id").ifBlank { UUID.randomUUID().toString() },
                            category = jd.optString("category", "Крест"),
                            code = jd.optString("code"),
                            name = jd.optString("name"),
                            price = jd.optDouble("price", 35.0),
                            description = jd.optString("description", ""),
                            isCustom = jd.optBoolean("isCustom"),
                            isEnabled = jd.optBoolean("isEnabled", true),
                            sortOrder = jd.optInt("sortOrder")
                        )
                        repository.insertDrawing(drawing)
                        drawingsCount++
                    }
                }

                val photoSizesArr = root.optJSONArray("photoSizes")
                if (photoSizesArr != null) {
                    for (i in 0 until photoSizesArr.length()) {
                        val jps = photoSizesArr.getJSONObject(i)
                        val ps = PhotoSizeItem(
                            id = jps.optString("id").ifBlank { UUID.randomUUID().toString() },
                            category = jps.optString("category", "Фотокерамика"),
                            sizeName = jps.optString("sizeName"),
                            price = jps.optDouble("price", 150.0),
                            description = jps.optString("description", ""),
                            isCustom = jps.optBoolean("isCustom"),
                            isEnabled = jps.optBoolean("isEnabled", true),
                            sortOrder = jps.optInt("sortOrder")
                        )
                        repository.insertPhotoSize(ps)
                        photoSizesCount++
                    }
                }

                val photoFramesArr = root.optJSONArray("photoFrames")
                if (photoFramesArr != null) {
                    for (i in 0 until photoFramesArr.length()) {
                        val jpf = photoFramesArr.getJSONObject(i)
                        val pf = PhotoFrameItem(
                            id = jpf.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = jpf.optString("name"),
                            materialType = jpf.optString("materialType", "Бронза"),
                            price = jpf.optDouble("price", 80.0),
                            description = jpf.optString("description", ""),
                            isCustom = jpf.optBoolean("isCustom"),
                            isEnabled = jpf.optBoolean("isEnabled", true),
                            sortOrder = jpf.optInt("sortOrder")
                        )
                        repository.insertPhotoFrame(pf)
                        photoFramesCount++
                    }
                }

                val vasesArr = root.optJSONArray("vases")
                if (vasesArr != null) {
                    for (i in 0 until vasesArr.length()) {
                        val jv = vasesArr.getJSONObject(i)
                        val vase = VaseItem(
                            id = jv.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = jv.optString("name"),
                            materialType = jv.optString("materialType", "Гранит"),
                            sizeCm = jv.optString("sizeCm", "30 см"),
                            price = jv.optDouble("price", 120.0),
                            description = jv.optString("description", ""),
                            isCustom = jv.optBoolean("isCustom"),
                            isEnabled = jv.optBoolean("isEnabled", true),
                            sortOrder = jv.optInt("sortOrder")
                        )
                        repository.insertVase(vase)
                        vasesCount++
                    }
                }

                _userMessage.tryEmit("Успешно импортировано: $ordersCount смет, $pricesCount товаров, $stonesCount материалов, $presetsCount размеров, $servicesCount тарифов, $fontsCount шрифтов, $drawingsCount рисунков, $photoSizesCount фото, $photoFramesCount рамок, $vasesCount ваз")
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка импорта: ${e.message}")
            }
        }
    }

    // --- Sharing Estimate Text in BYN ---

    fun shareEstimate(context: Context) {
        val state = _calcState.value
        if (state.selectedItems.isEmpty()) {
            _userMessage.tryEmit("Смета пуста")
            return
        }

        val sb = StringBuilder()
        sb.append("📋 РАСЧЕТ СМЕТЫ: ${state.orderNumber}\n")
        sb.append("═══════════════════════════\n")
        if (state.clientName.isNotBlank()) sb.append("Заказчик: ${state.clientName}\n")
        if (state.clientPhone.isNotBlank()) sb.append("Телефон: ${state.clientPhone}\n")
        if (state.deceasedName.isNotBlank()) sb.append("Усопший: ${state.deceasedName}\n")
        if (state.cemeteryName.isNotBlank()) sb.append("Кладбище: ${state.cemeteryName} ${if (state.plotNumber.isNotBlank()) "(уч. ${state.plotNumber})" else ""}\n")
        sb.append("───────────────────────────\n")
        sb.append("ПОЗИЦИИ ЗАКАЗА:\n")

        val grouped = state.selectedItems.groupBy { it.category }
        grouped.forEach { (category, items) ->
            sb.append("\n[$category]\n")
            items.forEachIndexed { i, itm ->
                sb.append("${i + 1}. ${itm.name}\n")
                sb.append("   ${PriceFormatter.formatNumber(itm.quantity)} ${itm.unit} × ${PriceFormatter.formatRub(itm.unitPrice)} = ${PriceFormatter.formatRub(itm.totalPrice)}\n")
            }
        }

        sb.append("\n───────────────────────────\n")
        sb.append("Сумма без скидки: ${PriceFormatter.formatRub(state.subtotal)}\n")
        if (state.discountPercent > 0) {
            sb.append("Скидка (${PriceFormatter.formatNumber(state.discountPercent)}%): -${PriceFormatter.formatRub(state.discountAmount)}\n")
        }
        sb.append("ИТОГО К ОПЛАТЕ: ${PriceFormatter.formatRub(state.total)}\n")

        if (state.prepayment > 0) {
            sb.append("Внесена предоплата: ${PriceFormatter.formatRub(state.prepayment)}\n")
            sb.append("Остаток к доплате: ${PriceFormatter.formatRub(state.remainingAmount)}\n")
        }

        if (state.notes.isNotBlank()) {
            sb.append("Примечание: ${state.notes}\n")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Отправить смету клиенту")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    // --- Generate Word (.docx) Document ---

    fun showMessage(message: String) {
        _userMessage.tryEmit(message)
    }

    // --- Generate & Open Word (.docx) Document ---

    fun openDocxInWord(context: Context, data: EstimateDocumentData) {
        if (data.items.isEmpty()) {
            _userMessage.tryEmit("Смета пуста! Добавьте товары или услуги")
            return
        }
        viewModelScope.launch {
            try {
                val file = DocxGenerator.generateDocx(context, data)
                val opened = DocxGenerator.openDocxFile(context, file)
                if (opened) {
                    _userMessage.tryEmit("Открытие сметы в Word: ${file.name} 📄")
                } else {
                    _userMessage.tryEmit("Файл Word создан: ${file.name}")
                }
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка создания Word: ${e.message}")
            }
        }
    }

    fun saveDocxToDownloads(context: Context, data: EstimateDocumentData) {
        if (data.items.isEmpty()) {
            _userMessage.tryEmit("Смета пуста!")
            return
        }
        viewModelScope.launch {
            try {
                val file = DocxGenerator.saveDocxToPublicDownloads(context, data)
                _userMessage.tryEmit("💾 Файл Word сохранен в папку Загрузки (PC):\n${file.name}")
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка сохранения в Загрузки: ${e.message}")
            }
        }
    }

    fun generateAndShareDocx(context: Context) {
        val state = _calcState.value
        if (state.selectedItems.isEmpty()) {
            _userMessage.tryEmit("Смета пуста! Добавьте товары или услуги")
            return
        }

        viewModelScope.launch {
            try {
                // Auto-save current estimate to database so it is retained in history and not lost
                val now = System.currentTimeMillis()
                val orderId = state.editingOrderId ?: java.util.UUID.randomUUID().toString()
                val order = SavedOrder(
                    id = orderId,
                    orderNumber = state.orderNumber,
                    clientName = state.clientName.ifBlank { "Заказчик" },
                    clientPhone = state.clientPhone,
                    deceasedName = state.deceasedName,
                    cemeteryName = state.cemeteryName,
                    plotNumber = state.plotNumber,
                    createdAt = now,
                    updatedAt = now,
                    status = "DRAFT",
                    subtotalAmount = state.subtotal,
                    discountPercent = state.discountPercent,
                    discountAmount = state.discountAmount,
                    totalAmount = state.total,
                    prepaymentAmount = state.prepayment,
                    remainingAmount = state.remainingAmount,
                    itemsJson = OrderJsonAdapter.toJson(state.selectedItems),
                    notes = state.notes,
                    syncStatus = if (isOnline.value) SyncStatus.SYNCED else SyncStatus.PENDING_PUSH,
                    isDeleted = false
                )

                if (isOnline.value) {
                    syncManager.saveOrderOnline(order)
                } else {
                    repository.saveOrder(order)
                }
                if (state.editingOrderId == null) {
                    _calcState.update { it.copy(editingOrderId = orderId) }
                }

                val currentState = _calcState.value
                val docData = EstimateDocumentData.fromCalculationState(currentState)
                val docxFile = DocxGenerator.generateDocx(context, docData)
                DocxGenerator.openDocxFile(context, docxFile)

                _userMessage.tryEmit("Смета сохранена и сформирована в Word: ${docxFile.name} 📄")
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка формирования Word: ${e.message}")
            }
        }
    }

    fun generateAndShareDocxForOrder(context: Context, order: SavedOrder) {
        val items = OrderJsonAdapter.fromJson(order.itemsJson)
        if (items.isEmpty()) {
            _userMessage.tryEmit("Смета пуста!")
            return
        }

        try {
            val docData = EstimateDocumentData.fromSavedOrder(order, items)
            val docxFile = DocxGenerator.generateDocx(context, docData)
            DocxGenerator.openDocxFile(context, docxFile)

            _userMessage.tryEmit("Документ Word сформирован: ${docxFile.name} 📄")
        } catch (e: Exception) {
            _userMessage.tryEmit("Ошибка формирования Word: ${e.message}")
        }
    }

    fun generateAndShareFactoryDocx(context: Context) {
        val state = _calcState.value
        if (state.selectedItems.isEmpty()) {
            _userMessage.tryEmit("Заказ пуст! Добавьте параметры памятника")
            return
        }
        viewModelScope.launch {
            try {
                val factoryData = FactoryOrderDocumentData.fromCalculationState(state)
                val docxFile = DocxGenerator.generateFactoryDocx(context, factoryData)
                DocxGenerator.openDocxFile(context, docxFile)
                _userMessage.tryEmit("Бланк для завода сформирован в Word: ${docxFile.name} 🏭")
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка формирования Word для завода: ${e.message}")
            }
        }
    }

    fun generateAndShareFactoryDocxForOrder(context: Context, order: SavedOrder) {
        val items = OrderJsonAdapter.fromJson(order.itemsJson)
        try {
            val factoryData = FactoryOrderDocumentData.fromSavedOrder(order, items)
            val docxFile = DocxGenerator.generateFactoryDocx(context, factoryData)
            DocxGenerator.openDocxFile(context, docxFile)
            _userMessage.tryEmit("Бланк для завода сформирован в Word: ${docxFile.name} 🏭")
        } catch (e: Exception) {
            _userMessage.tryEmit("Ошибка формирования Word для завода: ${e.message}")
        }
    }

    fun saveFactoryDocxToDownloads(context: Context, data: FactoryOrderDocumentData) {
        viewModelScope.launch {
            try {
                val file = DocxGenerator.saveFactoryDocxToPublicDownloads(context, data)
                _userMessage.tryEmit("💾 Бланк завода сохранен в Загрузки (PC):\n${file.name}")
            } catch (e: Exception) {
                _userMessage.tryEmit("Ошибка сохранения в Загрузки: ${e.message}")
            }
        }
    }
}
