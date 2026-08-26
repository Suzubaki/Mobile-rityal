package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.DocxGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen(val title: String) {
    CALCULATOR("Калькулятор смет"),
    PRICE_SETTINGS("Прайс и настройки"),
    SAVED_ORDERS("Сохраненные сметы")
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
    val editingOrderId: Long? = null
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
            return "РК-${df.format(Date())}"
        }
    }
}

class RitualViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RitualRepository

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

    init {
        val db = AppDatabase.getInstance(application)
        repository = RitualRepository(
            db.priceDao(),
            db.orderDao(),
            db.stoneMaterialDao(),
            db.monumentSizePresetDao(),
            db.constructorServicePriceDao()
        )

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

        viewModelScope.launch {
            repository.ensureDefaultDataLoaded()
            refreshNbrbUsdRate(showUserFeedback = false)
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
                cemeteryName = cemeteryName,
                plotNumber = plotNumber,
                notes = notes,
                discountPercent = discountPercent,
                prepayment = prepayment
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

        viewModelScope.launch {
            val order = SavedOrder(
                id = state.editingOrderId ?: 0L,
                orderNumber = state.orderNumber,
                clientName = state.clientName.ifBlank { "Без имени" },
                clientPhone = state.clientPhone,
                deceasedName = state.deceasedName,
                cemeteryName = state.cemeteryName,
                plotNumber = state.plotNumber,
                status = status,
                subtotalAmount = state.subtotal,
                discountPercent = state.discountPercent,
                discountAmount = state.discountAmount,
                totalAmount = state.total,
                prepaymentAmount = state.prepayment,
                remainingAmount = state.remainingAmount,
                itemsJson = OrderJsonAdapter.toJson(state.selectedItems),
                notes = state.notes
            )

            if (state.editingOrderId != null) {
                repository.updateOrder(order)
                _userMessage.tryEmit("Смета ${order.orderNumber} успешно обновлена")
            } else {
                val insertedId = repository.saveOrder(order)
                _calcState.update { it.copy(editingOrderId = insertedId) }
                _userMessage.tryEmit("Смета ${order.orderNumber} сохранена")
            }
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
            repository.deleteOrder(order)
            if (_calcState.value.editingOrderId == order.id) {
                _calcState.update { it.copy(editingOrderId = null) }
            }
            _userMessage.tryEmit("Смета ${order.orderNumber} удалена")
        }
    }

    // --- Price Management Operations ---

    fun savePriceItem(item: PriceItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.insertPriceItem(item)
                _userMessage.tryEmit("Добавлена расценка: ${item.name}")
            } else {
                repository.updatePriceItem(item)
                _userMessage.tryEmit("Обновлена расценка: ${item.name}")
            }
        }
    }

    fun updatePriceItemCost(id: Long, newPrice: Double) {
        viewModelScope.launch {
            val item = allPrices.value.find { it.id == id }
            if (item != null) {
                repository.updatePriceItem(item.copy(currentPrice = newPrice))
                _userMessage.tryEmit("Цена «${item.name}» изменена на ${PriceFormatter.formatRub(newPrice)}")
            }
        }
    }

    fun togglePriceItemEnabled(item: PriceItem) {
        viewModelScope.launch {
            repository.updatePriceItem(item.copy(isEnabled = !item.isEnabled))
        }
    }

    fun deletePriceItem(item: PriceItem) {
        viewModelScope.launch {
            repository.deletePriceItem(item.id)
            _userMessage.tryEmit("Удалена позиция из прайса: ${item.name}")
        }
    }

    fun resetPricesToDefault() {
        viewModelScope.launch {
            repository.resetToDefaultPrices()
            _userMessage.tryEmit("Прайс-лист сброшен к базовым расценкам")
        }
    }

    // --- Stone Materials Operations ---

    fun saveStoneMaterial(material: StoneMaterialItem) {
        viewModelScope.launch {
            repository.insertStoneMaterial(material)
            _userMessage.tryEmit("Материал сохранен: ${material.name}")
        }
    }

    fun toggleStoneMaterialEnabled(material: StoneMaterialItem) {
        viewModelScope.launch {
            repository.updateStoneMaterial(material.copy(isEnabled = !material.isEnabled))
        }
    }

    fun deleteStoneMaterial(material: StoneMaterialItem) {
        viewModelScope.launch {
            repository.deleteStoneMaterial(material)
            _userMessage.tryEmit("Материал «${material.name}» удален")
        }
    }

    fun resetStoneMaterials() {
        viewModelScope.launch {
            repository.resetStoneMaterials()
            _userMessage.tryEmit("Список материалов сброшен к заводским")
        }
    }

    // --- Monument Size Presets Operations ---

    fun saveSizePreset(preset: MonumentSizePresetItem) {
        viewModelScope.launch {
            repository.insertSizePreset(preset)
            _userMessage.tryEmit("Типоразмер сохранен: ${preset.name}")
        }
    }

    fun toggleSizePresetEnabled(preset: MonumentSizePresetItem) {
        viewModelScope.launch {
            repository.updateSizePreset(preset.copy(isEnabled = !preset.isEnabled))
        }
    }

    fun deleteSizePreset(preset: MonumentSizePresetItem) {
        viewModelScope.launch {
            repository.deleteSizePreset(preset)
            _userMessage.tryEmit("Размер «${preset.name}» удален")
        }
    }

    fun resetSizePresets() {
        viewModelScope.launch {
            repository.resetSizePresets()
            _userMessage.tryEmit("Типовые размеры сброшены к базовым")
        }
    }

    // --- Constructor Service Prices Operations ---

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

    fun updateUsdExchangeRate(newRate: Double, showFeedback: Boolean = true, sourceLabel: String? = null) {
        viewModelScope.launch {
            val existing = constructorServicePrices.value.find { it.key == "usd_exchange_rate" }
            if (existing != null) {
                repository.updateServicePrice(existing.copy(price = newRate))
            } else {
                repository.insertServicePrice(
                    ConstructorServicePriceItem(
                        key = "usd_exchange_rate",
                        groupName = "Настройки",
                        title = "Курс USD к BYN",
                        price = newRate,
                        description = "Курс доллара для расчетов и отображения",
                        sortOrder = -1
                    )
                )
            }
            if (showFeedback) {
                val labelMsg = if (sourceLabel != null) " от $sourceLabel" else ""
                _userMessage.tryEmit("Курс USD$labelMsg обновлен: $newRate BYN")
            }
        }
    }

    fun saveConstructorServicePrice(item: ConstructorServicePriceItem) {
        viewModelScope.launch {
            repository.insertServicePrice(item)
            _userMessage.tryEmit("Тариф «${item.title}» сохранен")
        }
    }

    fun deleteConstructorServicePrice(item: ConstructorServicePriceItem) {
        viewModelScope.launch {
            repository.deleteServicePrice(item)
            _userMessage.tryEmit("Удалена услуга: ${item.title}")
        }
    }

    fun updateConstructorPriceByKey(key: String, newPrice: Double) {
        viewModelScope.launch {
            val item = constructorServicePrices.value.find { it.key == key }
            if (item != null) {
                repository.insertServicePrice(item.copy(price = newPrice))
                _userMessage.tryEmit("Тариф «${item.title}» изменен на ${PriceFormatter.formatRub(newPrice)}")
            }
        }
    }

    fun resetConstructorServicePrices() {
        viewModelScope.launch {
            repository.resetConstructorServicePrices()
            _userMessage.tryEmit("Тарифы конструктора сброшены к базовым")
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

    fun generateAndShareDocx(context: Context) {
        val state = _calcState.value
        if (state.selectedItems.isEmpty()) {
            _userMessage.tryEmit("Смета пуста! Добавьте товары или услуги")
            return
        }

        viewModelScope.launch {
            try {
                // Auto-save current estimate to database so it is retained in history and not lost
                val order = SavedOrder(
                    id = state.editingOrderId ?: 0L,
                    orderNumber = state.orderNumber,
                    clientName = state.clientName.ifBlank { "Заказчик" },
                    clientPhone = state.clientPhone,
                    deceasedName = state.deceasedName,
                    cemeteryName = state.cemeteryName,
                    plotNumber = state.plotNumber,
                    status = "DRAFT",
                    subtotalAmount = state.subtotal,
                    discountPercent = state.discountPercent,
                    discountAmount = state.discountAmount,
                    totalAmount = state.total,
                    prepaymentAmount = state.prepayment,
                    remainingAmount = state.remainingAmount,
                    itemsJson = OrderJsonAdapter.toJson(state.selectedItems),
                    notes = state.notes
                )

                if (state.editingOrderId != null && state.editingOrderId != 0L) {
                    repository.updateOrder(order)
                } else {
                    val insertedId = repository.saveOrder(order)
                    _calcState.update { it.copy(editingOrderId = insertedId) }
                }

                val currentState = _calcState.value
                val docData = EstimateDocumentData.fromCalculationState(currentState)
                val docxFile = DocxGenerator.generateDocx(context, docData)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    docxFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Сформировать Word (${docxFile.name})")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                _userMessage.tryEmit("Смета сохранена и сформирована в Word: ${docxFile.name}")
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

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                docxFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Сформировать Word (${docxFile.name})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            _userMessage.tryEmit("Документ Word сформирован: ${docxFile.name}")
        } catch (e: Exception) {
            _userMessage.tryEmit("Ошибка формирования Word: ${e.message}")
        }
    }
}
