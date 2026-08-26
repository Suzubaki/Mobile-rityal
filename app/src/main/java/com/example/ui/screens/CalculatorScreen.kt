package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculatedItemData
import com.example.data.PriceFormatter
import com.example.data.PriceItem
import com.example.data.toModel
import com.example.ui.CalculationState
import com.example.ui.RitualViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: RitualViewModel,
    calcState: CalculationState,
    activePrices: List<PriceItem>
) {
    val context = LocalContext.current
    val stoneMaterials by viewModel.stoneMaterials.collectAsStateWithLifecycle()
    val sizePresets by viewModel.sizePresets.collectAsStateWithLifecycle()
    val constructorServicePrices by viewModel.constructorServicePrices.collectAsStateWithLifecycle()
    val constructorPricesMap = remember(constructorServicePrices) {
        constructorServicePrices.associate { it.key to it.price }
    }
    val usdRate by viewModel.usdExchangeRate.collectAsStateWithLifecycle()

    var showCatalogSheet by remember { mutableStateOf(false) }
    var showMonumentBuilder by remember { mutableStateOf(false) }
    var showDimensionCalculators by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showClientDetailsCard by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showSaveOrderDialog by remember { mutableStateOf(false) }

    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var editingQuantityText by remember { mutableStateOf("") }
    var editingPriceText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // --- Client Info Card ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = calcState.orderNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val clientDisplay = if (calcState.clientName.isNotBlank()) {
                            calcState.clientName + if (calcState.clientPhone.isNotBlank()) " (${calcState.clientPhone})" else ""
                        } else {
                            "Данные клиента не заполнены"
                        }
                        Text(
                            text = clientDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (calcState.clientName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            onClick = { viewModel.refreshNbrbUsdRate(showUserFeedback = true) },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = "Курс НБ РБ",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$ = $usdRate",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        IconButton(
                            onClick = { showClientDetailsCard = !showClientDetailsCard },
                            modifier = Modifier.testTag("toggle_client_details_btn")
                        ) {
                            Icon(
                                imageVector = if (showClientDetailsCard) Icons.Default.ExpandLess else Icons.Default.PersonOutline,
                                contentDescription = "Данные заказчика"
                            )
                        }

                        if (calcState.selectedItems.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearConfirmation = true },
                                modifier = Modifier.testTag("clear_calc_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Очистить расчет",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Expandable Client Details Form
                AnimatedVisibility(visible = showClientDetailsCard) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider()

                        OutlinedTextField(
                            value = calcState.clientName,
                            onValueChange = { viewModel.updateClientDetails(clientName = it) },
                            label = { Text("ФИО Заказчика") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_client_name")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = calcState.clientPhone,
                                onValueChange = { viewModel.updateClientDetails(clientPhone = it) },
                                label = { Text("Телефон") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_client_phone")
                            )
                            OutlinedTextField(
                                value = calcState.deceasedName,
                                onValueChange = { viewModel.updateClientDetails(deceasedName = it) },
                                label = { Text("ФИО Усопшего") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("input_deceased_name")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = calcState.cemeteryName,
                                onValueChange = { viewModel.updateClientDetails(cemeteryName = it) },
                                label = { Text("Кладбище") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f).testTag("input_cemetery")
                            )
                            OutlinedTextField(
                                value = calcState.plotNumber,
                                onValueChange = { viewModel.updateClientDetails(plotNumber = it) },
                                label = { Text("Участок/Ряд") },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f).testTag("input_plot")
                            )
                        }

                        OutlinedTextField(
                            value = calcState.notes,
                            onValueChange = { viewModel.updateClientDetails(notes = it) },
                            label = { Text("Примечание к заказу") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("input_notes")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Action Buttons: Monument Builder, Catalog, Dimensions Helper, Custom Item ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { showMonumentBuilder = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_monument_builder_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Terrain, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Конструктор памятника (камни, размеры, резка)", fontWeight = FontWeight.Bold, maxLines = 1)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showCatalogSheet = true },
                    modifier = Modifier.weight(1f).testTag("open_catalog_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Каталог расценок", maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showDimensionCalculators = true },
                    modifier = Modifier.weight(1f).testTag("open_dimension_calc_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.SquareFoot, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Замеры", maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- List of Added Items in Calculation ---
        if (calcState.selectedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Смета пока пуста",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Используйте «Конструктор памятника» для расчета стелы, тумбы и цветника из выбранной породы камня, либо выберите позиции из каталога расценок.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                itemsIndexed(calcState.selectedItems) { index, item ->
                    CalculatedItemCard(
                        item = item,
                        index = index,
                        onQuantityChange = { newQty -> viewModel.updateItemQuantity(index, newQty) },
                        onRemove = { viewModel.removeItemFromCalculation(index) },
                        onEdit = {
                            editingItemIndex = index
                            editingQuantityText = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else item.quantity.toString()
                            editingPriceText = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toLong().toString() else item.unitPrice.toString()
                        }
                    )
                }
            }
        }

        // --- Bottom Calculation Summary Card ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Позиций: ${calcState.selectedItems.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Сумма: ${PriceFormatter.formatRub(calcState.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (calcState.discountPercent > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Скидка (${PriceFormatter.formatNumber(calcState.discountPercent)}%):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "-${PriceFormatter.formatRub(calcState.discountAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ИТОГО:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = PriceFormatter.formatRub(calcState.total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("total_amount_text")
                    )
                }

                if (calcState.prepayment > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Предоплата: ${PriceFormatter.formatRub(calcState.prepayment)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Остаток: ${PriceFormatter.formatRub(calcState.remainingAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                // Primary Action: Generate Word Document
                Button(
                    onClick = { viewModel.generateAndShareDocx(context) },
                    enabled = calcState.selectedItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_word_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сформировать Word (.docx)", fontWeight = FontWeight.Bold)
                }

                // Secondary Action Buttons: Discount, Save, Share Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDiscountDialog = true },
                        modifier = Modifier.weight(1f).testTag("discount_btn"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (calcState.discountPercent > 0) "${PriceFormatter.formatNumber(calcState.discountPercent)}%" else "Скидка", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { showSaveOrderDialog = true },
                        enabled = calcState.selectedItems.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("save_order_btn"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { viewModel.shareEstimate(context) },
                        enabled = calcState.selectedItems.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("share_estimate_btn"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Текст", maxLines = 1)
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showMonumentBuilder) {
        MonumentBuilderDialog(
            materials = stoneMaterials.map { it.toModel() },
            sizePresets = sizePresets.map { it.toModel() },
            servicePrices = constructorServicePrices.associate { it.key to it.price },
            constructorServicePrices = constructorServicePrices,
            usdRate = usdRate,
            onDismiss = { showMonumentBuilder = false },
            onAddSingleItem = { name, cat, unit, price, qty, note ->
                viewModel.addCustomItemToCalculation(name, cat, unit, price, qty, note)
            },
            onAddMultipleItems = { items ->
                viewModel.addMultipleItemsToCalculation(items)
            }
        )
    }

    if (showCatalogSheet) {
        ItemCatalogSheet(
            activePrices = activePrices,
            onDismiss = { showCatalogSheet = false },
            onSelectItem = { item: PriceItem, qty: Double ->
                viewModel.addItemToCalculation(item, qty)
            }
        )
    }

    if (showDimensionCalculators) {
        DimensionCalculatorsDialog(
            activePrices = activePrices,
            servicePrices = constructorPricesMap,
            usdRate = usdRate,
            onDismiss = { showDimensionCalculators = false },
            onSaveServicePrice = { key, price ->
                viewModel.updateConstructorPriceByKey(key, price)
            },
            onAddCalculatedItem = { name: String, cat: String, unit: String, price: Double, qty: Double ->
                viewModel.addCustomItemToCalculation(name, cat, unit, price, qty)
            }
        )
    }

    if (showDiscountDialog) {
        DiscountPrepaymentDialog(
            initialDiscountPercent = calcState.discountPercent,
            initialPrepayment = calcState.prepayment,
            subtotal = calcState.subtotal,
            onDismiss = { showDiscountDialog = false },
            onConfirm = { discountPct: Double, prepay: Double ->
                viewModel.updateClientDetails(
                    discountPercent = discountPct,
                    prepayment = prepay
                )
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Очистить расчет?") },
            text = { Text("Все добавленные позиции будут удалены из текущего калькулятора.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCalculation()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showSaveOrderDialog) {
        var selectedStatus by remember { mutableStateOf("DRAFT") }
        AlertDialog(
            onDismissRequest = { showSaveOrderDialog = false },
            title = { Text("Сохранить смету ${calcState.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Заказчик: ${calcState.clientName.ifBlank { "Без имени" }}")
                    Text("Итоговая сумма: ${PriceFormatter.formatRub(calcState.total)}", fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Статус сметы / заказа:", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "DRAFT" to "Черновик",
                            "IN_PROGRESS" to "В работе",
                            "PAID" to "Оплачен"
                        ).forEach { (st, label) ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentOrder(status = selectedStatus)
                        showSaveOrderDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_order_btn")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveOrderDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Quick Item Edit Dialog (Quantity & Price)
    editingItemIndex?.let { index ->
        if (index in calcState.selectedItems.indices) {
            val item = calcState.selectedItems[index]
            AlertDialog(
                onDismissRequest = { editingItemIndex = null },
                title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editingQuantityText,
                            onValueChange = { editingQuantityText = it },
                            label = { Text("Количество (${item.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editingPriceText,
                            onValueChange = { editingPriceText = it },
                            label = { Text("Цена за единицу (BYN)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newQty = editingQuantityText.toDoubleOrNull() ?: item.quantity
                            val newPrice = editingPriceText.toDoubleOrNull() ?: item.unitPrice
                            viewModel.updateItemPrice(index, newPrice)
                            viewModel.updateItemQuantity(index, newQty)
                            editingItemIndex = null
                        }
                    ) {
                        Text("Применить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingItemIndex = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
private fun CalculatedItemCard(
    item: CalculatedItemData,
    index: Int,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calc_item_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Category, Name & Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalIconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(32.dp).testTag("remove_item_btn_$index")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Удалить позицию",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom controls: Price / unit on the left, Qty stepper + Total on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price & Unit chip clickable to edit
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { onEdit() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${PriceFormatter.formatRub(item.unitPrice)}/${item.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quantity Stepper: - 1 +
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onQuantityChange(item.quantity - 1.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Меньше", modifier = Modifier.size(13.dp))
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onEdit() }
                        ) {
                            Text(
                                text = PriceFormatter.formatNumber(item.quantity),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { onQuantityChange(item.quantity + 1.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Больше", modifier = Modifier.size(13.dp))
                        }
                    }

                    // Subtotal for item
                    Text(
                        text = PriceFormatter.formatRub(item.totalPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
