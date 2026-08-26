package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ItemCategory
import com.example.data.PriceFormatter
import com.example.data.PriceItem

data class CatalogPresetOption(
    val name: String,
    val price: Double,
    val unit: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimensionCalculatorsDialog(
    activePrices: List<PriceItem> = emptyList(),
    servicePrices: Map<String, Double> = emptyMap(),
    usdRate: Double = 3.25,
    onDismiss: () -> Unit,
    onSaveServicePrice: ((key: String, price: Double) -> Unit)? = null,
    onAddCalculatedItem: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Ограда / Периметр", "Плитка / Площадь", "Гравировка", "Доставка")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Замеры и расчёты",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, maxLines = 1, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> FencePerimeterCalculator(
                        activePrices = activePrices,
                        servicePrices = servicePrices,
                        usdRate = usdRate,
                        onSavePrice = onSaveServicePrice,
                        onAdd = onAddCalculatedItem,
                        onDismiss = onDismiss
                    )
                    1 -> AreaTileCalculator(
                        activePrices = activePrices,
                        servicePrices = servicePrices,
                        usdRate = usdRate,
                        onSavePrice = onSaveServicePrice,
                        onAdd = onAddCalculatedItem,
                        onDismiss = onDismiss
                    )
                    2 -> EngravingLettersCalculator(
                        servicePrices = servicePrices,
                        usdRate = usdRate,
                        onSavePrice = onSaveServicePrice,
                        onAdd = onAddCalculatedItem,
                        onDismiss = onDismiss
                    )
                    3 -> DeliveryDistanceCalculator(
                        servicePrices = servicePrices,
                        usdRate = usdRate,
                        onSavePrice = onSaveServicePrice,
                        onAdd = onAddCalculatedItem,
                        onDismiss = onDismiss
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FencePerimeterCalculator(
    activePrices: List<PriceItem>,
    servicePrices: Map<String, Double>,
    usdRate: Double,
    onSavePrice: ((key: String, price: Double) -> Unit)?,
    onAdd: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPrice = servicePrices["measure_fence_price_pm"] ?: 45.0

    var lengthStr by remember { mutableStateOf("2.0") }
    var widthStr by remember { mutableStateOf("2.0") }
    var pricePerMeterStr by remember(defaultPrice) {
        mutableStateOf(if (defaultPrice % 1.0 == 0.0) defaultPrice.toLong().toString() else defaultPrice.toString())
    }
    var fenceName by remember { mutableStateOf("Ограда металлическая профильная сварная") }
    var priceSavedFeedback by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val fenceOptions = remember(activePrices) {
        val fromCatalog = activePrices.filter { item ->
            item.isEnabled && item.category == ItemCategory.FENCES_GROUND.displayName && item.unit != "м²"
        }.map { item ->
            CatalogPresetOption(item.name, item.currentPrice, item.unit)
        }
        if (fromCatalog.isNotEmpty()) {
            fromCatalog.distinctBy { it.name }
        } else {
            listOf(
                CatalogPresetOption("Ограда металлическая профильная сварная", 45.0, "м.п."),
                CatalogPresetOption("Ограда кованая с элементами декора", 110.0, "м.п."),
                CatalogPresetOption("Ограда из нержавеющей стали", 140.0, "м.п."),
                CatalogPresetOption("Бетонный армированный цоколь (ленточный)", 95.0, "м.п."),
                CatalogPresetOption("Гранитный цоколь с шарами и столбиками", 280.0, "м.п."),
                CatalogPresetOption("Комбинированная ограда (металл + гранит)", 180.0, "м.п.")
            )
        }
    }

    val length = lengthStr.toDoubleOrNull() ?: 0.0
    val width = widthStr.toDoubleOrNull() ?: 0.0
    val perimeter = (length + width) * 2
    val pricePerMeter = pricePerMeterStr.toDoubleOrNull() ?: 0.0
    val total = perimeter * pricePerMeter

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Быстрый расчет ограды и цоколя по размерам участка:", style = MaterialTheme.typography.bodyMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = lengthStr,
                onValueChange = { lengthStr = it },
                label = { Text("Длина (м)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("fence_calc_length")
            )
            OutlinedTextField(
                value = widthStr,
                onValueChange = { widthStr = it },
                label = { Text("Ширина (м)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("fence_calc_width")
            )
        }

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = fenceName,
                onValueChange = {
                    fenceName = it
                    isDropdownExpanded = true
                },
                label = { Text("Тип ограды (выберите или введите)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("fence_name_combobox")
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                fenceOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${PriceFormatter.formatRub(option.price)} / м.п.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            fenceName = option.name
                            pricePerMeterStr = if (option.price % 1.0 == 0.0) option.price.toLong().toString() else option.price.toString()
                            isDropdownExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pricePerMeterStr,
                onValueChange = {
                    pricePerMeterStr = it
                    priceSavedFeedback = false
                },
                label = { Text("Цена за 1 пог. м (BYN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).testTag("fence_calc_price")
            )

            if (onSavePrice != null && pricePerMeter > 0) {
                OutlinedButton(
                    onClick = {
                        onSavePrice("measure_fence_price_pm", pricePerMeter)
                        priceSavedFeedback = true
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        if (priceSavedFeedback) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (priceSavedFeedback) "Сохранено!" else "В прайс", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Периметр: ${PriceFormatter.formatNumber(perimeter)} пог. м", fontWeight = FontWeight.Bold)
                Text(
                    text = "Итого за ограду: ${PriceFormatter.formatWithUsd(total, usdRate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = {
                if (perimeter > 0 && pricePerMeter > 0) {
                    onAdd(
                        "$fenceName (${lengthStr}×${widthStr} м)",
                        ItemCategory.FENCES_GROUND.displayName,
                        "м.п.",
                        pricePerMeter,
                        perimeter
                    )
                    onDismiss()
                }
            },
            enabled = perimeter > 0 && pricePerMeter > 0,
            modifier = Modifier.fillMaxWidth().testTag("add_fence_to_calc_btn")
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить в смету")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaTileCalculator(
    activePrices: List<PriceItem>,
    servicePrices: Map<String, Double>,
    usdRate: Double,
    onSavePrice: ((key: String, price: Double) -> Unit)?,
    onAdd: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPrice = servicePrices["measure_tile_price_sqm"] ?: 85.0

    var lengthStr by remember { mutableStateOf("2.0") }
    var widthStr by remember { mutableStateOf("2.0") }
    var pricePerSqMeterStr by remember(defaultPrice) {
        mutableStateOf(if (defaultPrice % 1.0 == 0.0) defaultPrice.toLong().toString() else defaultPrice.toString())
    }
    var workName by remember { mutableStateOf("Укладка тротуарной плитки с бордюром «под ключ»") }
    var priceSavedFeedback by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val tileOptions = remember(activePrices) {
        val fromCatalog = activePrices.filter { item ->
            item.isEnabled && (item.category == ItemCategory.FENCES_GROUND.displayName || item.name.contains("плитк", ignoreCase = true) || item.name.contains("газон", ignoreCase = true) || item.name.contains("щебень", ignoreCase = true)) && item.unit == "м²"
        }.map { item ->
            CatalogPresetOption(item.name, item.currentPrice, item.unit)
        }
        if (fromCatalog.isNotEmpty()) {
            fromCatalog.distinctBy { it.name }
        } else {
            listOf(
                CatalogPresetOption("Укладка тротуарной плитки с бордюром «под ключ»", 85.0, "м²"),
                CatalogPresetOption("Облицовка гранитной плиткой (30×30 или 60×30)", 190.0, "м²"),
                CatalogPresetOption("Облицовка керамогранитной плиткой", 120.0, "м²"),
                CatalogPresetOption("Отсыпка декоративным щебнем / мраморной крошкой", 35.0, "м²"),
                CatalogPresetOption("Укладка искусственного газона", 48.0, "м²"),
                CatalogPresetOption("Заливка бетонного основания (стяжка)", 65.0, "м²")
            )
        }
    }

    val length = lengthStr.toDoubleOrNull() ?: 0.0
    val width = widthStr.toDoubleOrNull() ?: 0.0
    val area = length * width
    val pricePerSqMeter = pricePerSqMeterStr.toDoubleOrNull() ?: 0.0
    val total = area * pricePerSqMeter

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Расчет площади благоустройства (плитка, брусчатка, крошка):", style = MaterialTheme.typography.bodyMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = lengthStr,
                onValueChange = { lengthStr = it },
                label = { Text("Длина (м)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = widthStr,
                onValueChange = { widthStr = it },
                label = { Text("Ширина (м)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = workName,
                onValueChange = {
                    workName = it
                    isDropdownExpanded = true
                },
                label = { Text("Тип покрытия / плитки (выберите или введите)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("tile_name_combobox")
            )

            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                tileOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${PriceFormatter.formatRub(option.price)} / м²",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            workName = option.name
                            pricePerSqMeterStr = if (option.price % 1.0 == 0.0) option.price.toLong().toString() else option.price.toString()
                            isDropdownExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pricePerSqMeterStr,
                onValueChange = {
                    pricePerSqMeterStr = it
                    priceSavedFeedback = false
                },
                label = { Text("Цена за 1 м² (BYN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            if (onSavePrice != null && pricePerSqMeter > 0) {
                OutlinedButton(
                    onClick = {
                        onSavePrice("measure_tile_price_sqm", pricePerSqMeter)
                        priceSavedFeedback = true
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        if (priceSavedFeedback) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (priceSavedFeedback) "Сохранено!" else "В прайс", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Площадь: ${PriceFormatter.formatNumber(area)} м²", fontWeight = FontWeight.Bold)
                Text(
                    text = "Итого: ${PriceFormatter.formatWithUsd(total, usdRate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = {
                if (area > 0 && pricePerSqMeter > 0) {
                    onAdd(
                        "$workName (${PriceFormatter.formatNumber(area)} м²)",
                        ItemCategory.FENCES_GROUND.displayName,
                        "м²",
                        pricePerSqMeter,
                        area
                    )
                    onDismiss()
                }
            },
            enabled = area > 0 && pricePerSqMeter > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить в смету")
        }
    }
}

@Composable
private fun EngravingLettersCalculator(
    servicePrices: Map<String, Double>,
    usdRate: Double,
    onSavePrice: ((key: String, price: Double) -> Unit)?,
    onAdd: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPrice = servicePrices["letter_standard"] ?: 1.60

    var textInput by remember { mutableStateOf("") }
    var pricePerCharStr by remember(defaultPrice) {
        mutableStateOf(if (defaultPrice % 1.0 == 0.0) defaultPrice.toLong().toString() else "%.2f".format(java.util.Locale.US, defaultPrice))
    }
    var priceSavedFeedback by remember { mutableStateOf(false) }

    val charCount = remember(textInput) {
        textInput.count { !it.isWhitespace() }
    }
    val pricePerChar = pricePerCharStr.toDoubleOrNull() ?: 0.0
    val total = charCount * pricePerChar

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Подсчет символов эпитафии или надписи:", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Текст эпитафии / надписи") },
            placeholder = { Text("Например: Помним, любим, скорбим...") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth().testTag("engraving_text_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pricePerCharStr,
                onValueChange = {
                    pricePerCharStr = it
                    priceSavedFeedback = false
                },
                label = { Text("Цена за 1 знак (BYN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            if (onSavePrice != null && pricePerChar > 0) {
                OutlinedButton(
                    onClick = {
                        onSavePrice("letter_standard", pricePerChar)
                        priceSavedFeedback = true
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        if (priceSavedFeedback) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (priceSavedFeedback) "Сохранено!" else "В прайс", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Знаков без пробелов: $charCount шт", fontWeight = FontWeight.Bold)
                Text(
                    text = "Стоимость гравировки: ${PriceFormatter.formatWithUsd(total, usdRate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = {
                if (charCount > 0 && pricePerChar > 0) {
                    val notePreview = if (textInput.length > 30) "${textInput.take(30)}..." else textInput
                    onAdd(
                        "Гравировка надписи: \"$notePreview\" ($charCount зн.)",
                        ItemCategory.ENGRAVING.displayName,
                        "знак",
                        pricePerChar,
                        charCount.toDouble()
                    )
                    onDismiss()
                }
            },
            enabled = charCount > 0 && pricePerChar > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить в смету")
        }
    }
}

@Composable
private fun DeliveryDistanceCalculator(
    servicePrices: Map<String, Double>,
    usdRate: Double,
    onSavePrice: ((key: String, price: Double) -> Unit)?,
    onAdd: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultBasePrice = servicePrices["measure_delivery_base_price"] ?: 60.0
    val defaultBaseKm = servicePrices["measure_delivery_base_km"] ?: 15.0
    val defaultKmPrice = servicePrices["measure_delivery_km_price"] ?: 1.50

    var baseKmStr by remember(defaultBaseKm) {
        mutableStateOf(if (defaultBaseKm % 1.0 == 0.0) defaultBaseKm.toLong().toString() else defaultBaseKm.toString())
    }
    var basePriceStr by remember(defaultBasePrice) {
        mutableStateOf(if (defaultBasePrice % 1.0 == 0.0) defaultBasePrice.toLong().toString() else defaultBasePrice.toString())
    }
    var totalDistanceStr by remember { mutableStateOf("35") }
    var extraKmPriceStr by remember(defaultKmPrice) {
        mutableStateOf(if (defaultKmPrice % 1.0 == 0.0) defaultKmPrice.toLong().toString() else defaultKmPrice.toString())
    }
    var priceSavedFeedback by remember { mutableStateOf(false) }

    val baseKm = baseKmStr.toDoubleOrNull() ?: 0.0
    val basePrice = basePriceStr.toDoubleOrNull() ?: 0.0
    val totalDist = totalDistanceStr.toDoubleOrNull() ?: 0.0
    val extraKmPrice = extraKmPriceStr.toDoubleOrNull() ?: 0.0

    val extraKm = (totalDist - baseKm).coerceAtLeast(0.0)
    val extraCost = extraKm * extraKmPrice
    val totalCost = basePrice + extraCost

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Расчет доставки с учетом выезда за пределы города:", style = MaterialTheme.typography.bodyMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = totalDistanceStr,
                onValueChange = { totalDistanceStr = it },
                label = { Text("Общий путь (км)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = basePriceStr,
                onValueChange = {
                    basePriceStr = it
                    priceSavedFeedback = false
                },
                label = { Text("Базовая доставка (BYN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = baseKmStr,
                onValueChange = {
                    baseKmStr = it
                    priceSavedFeedback = false
                },
                label = { Text("Включено км") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = extraKmPriceStr,
                onValueChange = {
                    extraKmPriceStr = it
                    priceSavedFeedback = false
                },
                label = { Text("Цена за доп. км (BYN)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        if (onSavePrice != null && (basePrice > 0 || extraKmPrice > 0)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = {
                        if (basePrice > 0) onSavePrice("measure_delivery_base_price", basePrice)
                        if (baseKm > 0) onSavePrice("measure_delivery_base_km", baseKm)
                        if (extraKmPrice > 0) onSavePrice("measure_delivery_km_price", extraKmPrice)
                        priceSavedFeedback = true
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        if (priceSavedFeedback) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (priceSavedFeedback) "Тарифы сохранены в прайс!" else "Сохранить тарифы в прайс", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (extraKm > 0) {
                    Text("Доп. километраж: ${PriceFormatter.formatNumber(extraKm)} км × ${PriceFormatter.formatRub(extraKmPrice)} = ${PriceFormatter.formatRub(extraCost)}")
                }
                Text(
                    text = "Итого доставка: ${PriceFormatter.formatWithUsd(totalCost, usdRate)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = {
                if (totalCost > 0) {
                    onAdd(
                        "Доставка на кладбище ($totalDistanceStr км)",
                        ItemCategory.OTHER.displayName,
                        "рейс",
                        totalCost,
                        1.0
                    )
                    onDismiss()
                }
            },
            enabled = totalCost > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить в смету")
        }
    }
}
