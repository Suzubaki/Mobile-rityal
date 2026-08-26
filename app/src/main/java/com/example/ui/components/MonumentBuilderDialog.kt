package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonumentBuilderDialog(
    materials: List<StoneMaterial> = emptyList(),
    sizePresets: List<MonumentSizePreset> = emptyList(),
    servicePrices: Map<String, Double> = emptyMap(),
    constructorServicePrices: List<ConstructorServicePriceItem> = emptyList(),
    usdRate: Double = 3.25,
    onDismiss: () -> Unit,
    onAddSingleItem: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double, note: String) -> Unit,
    onAddMultipleItems: (List<CalculatedItemData>) -> Unit
) {
    val scrollState = rememberScrollState()

    val actualMaterials = remember(materials) {
        if (materials.isNotEmpty()) materials else MonumentCatalogData.materials
    }
    val actualPresets = remember(sizePresets) {
        if (sizePresets.isNotEmpty()) sizePresets else MonumentCatalogData.sizePresets
    }
    val actualServicePrices = remember(servicePrices, constructorServicePrices) {
        val map = servicePrices.toMutableMap()
        constructorServicePrices.forEach { item ->
            map[item.key] = item.price
        }
        if (map.isNotEmpty()) map else MonumentCatalogData.servicePrices
    }

    val builtInKeys = remember {
        setOf(
            "carving_wave", "carving_cross", "carving_flowers", "carving_angel_tree", "carving_rock", "carving_exclusive",
            "chamfer_polished_10mm", "chamfer_round", "plinth_high", "flowerbed_half_slab", "flowerbed_solid_slab", "flowerbed_double",
            "portrait_laser_a4", "portrait_hand_retouch", "portrait_full_height", "portrait_ceramic_13x18", "portrait_triplex",
            "letter_standard", "letter_gold", "letter_epitaph",
            "art_cross", "art_flowers", "art_backside", "art_antirain",
            "install_beams", "install_concrete_pad", "install_existing_base",
            "measure_fence_price_pm", "measure_tile_price_sqm", "measure_delivery_base_price", "measure_delivery_base_km", "measure_delivery_km_price"
        )
    }

    val customServices = remember(constructorServicePrices) {
        constructorServicePrices.filter { it.key !in builtInKeys }
    }

    var selectedCustomServiceKeys by remember { mutableStateOf(setOf<String>()) }

    fun getServicePrice(code: String, fallback: Double): Double {
        return actualServicePrices[code] ?: fallback
    }

    // 1. Single unified Stone material for stele, plinth, and flowerbed
    var selectedStone by remember(actualMaterials) {
        mutableStateOf(actualMaterials.firstOrNull() ?: MonumentCatalogData.materials.first())
    }

    // 2. Dimensions & Volume Calculations
    // Stele Dimensions (cm)
    var steleHeightStr by remember { mutableStateOf("100") }
    var steleWidthStr by remember { mutableStateOf("50") }
    var steleThicknessStr by remember { mutableStateOf("8") }

    // Plinth (Тумба) Dimensions (cm)
    var includePlinth by remember { mutableStateOf(true) }
    var plinthLengthStr by remember { mutableStateOf("60") }
    var plinthWidthStr by remember { mutableStateOf("20") }
    var plinthHeightStr by remember { mutableStateOf("15") }

    // Flowerbed (Цветник) Dimensions (cm)
    var includeFlowerbed by remember { mutableStateOf(true) }
    var isSolidSlabFlowerbed by remember { mutableStateOf(false) }
    var flowerbedLengthStr by remember { mutableStateOf("100") }
    var flowerbedWidthStr by remember { mutableStateOf("60") }
    var flowerbedThicknessStr by remember { mutableStateOf("8") }
    var flowerbedBarWidthStr by remember { mutableStateOf("5") }

    // --- CALCULATIONS ---

    // Granite density ~ 2900 kg/m³
    val stoneDensityKgM3 = 2900.0

    // 1) Stele Calculations
    val steleH = steleHeightStr.toDoubleOrNull() ?: 100.0
    val steleW = steleWidthStr.toDoubleOrNull() ?: 50.0
    val steleT = steleThicknessStr.toDoubleOrNull() ?: 8.0
    val steleVolumeM3 = (steleH * steleW * steleT) / 1_000_000.0
    val steleMassKg = steleVolumeM3 * stoneDensityKgM3
    val stelePrice = steleVolumeM3 * selectedStone.pricePerM3

    // 2) Plinth Calculations
    val plinthL = plinthLengthStr.toDoubleOrNull() ?: 60.0
    val plinthW = plinthWidthStr.toDoubleOrNull() ?: 20.0
    val plinthH = plinthHeightStr.toDoubleOrNull() ?: 15.0
    val plinthVolumeM3 = if (includePlinth) (plinthL * plinthW * plinthH) / 1_000_000.0 else 0.0
    val plinthMassKg = plinthVolumeM3 * stoneDensityKgM3
    val plinthPrice = plinthVolumeM3 * selectedStone.pricePerM3

    // 3) Flowerbed Calculations
    val fbL = flowerbedLengthStr.toDoubleOrNull() ?: 100.0
    val fbW = flowerbedWidthStr.toDoubleOrNull() ?: 60.0
    val fbT = flowerbedThicknessStr.toDoubleOrNull() ?: 8.0
    val fbBarW = flowerbedBarWidthStr.toDoubleOrNull() ?: 5.0

    val flowerbedVolumeM3 = if (includeFlowerbed) {
        if (isSolidSlabFlowerbed) {
            // Solid full slab: L * W * T
            (fbL * fbW * fbT) / 1_000_000.0
        } else {
            // 3 bars (2 longitudinal bars + 1 transverse bar)
            val totalBarLength = (2 * fbL) + fbW
            (totalBarLength * fbBarW * fbT) / 1_000_000.0
        }
    } else 0.0
    val flowerbedMassKg = flowerbedVolumeM3 * stoneDensityKgM3
    val flowerbedPrice = flowerbedVolumeM3 * selectedStone.pricePerM3

    // 3. Engraving (ФИО и эпитафия)
    var fioText by remember { mutableStateOf("Иванов Иван Иванович\n15.05.1950 — 20.08.2024") }
    var fioLettersCountStr by remember { mutableStateOf("") }
    var isGoldFio by remember { mutableStateOf(false) }

    var epitaphText by remember { mutableStateOf("Помним, любим, скорбим...") }

    val countedFioLetters = remember(fioText, fioLettersCountStr) {
        if (fioLettersCountStr.isNotBlank()) {
            fioLettersCountStr.toIntOrNull() ?: 0
        } else {
            fioText.count { it.isLetterOrDigit() }
        }
    }
    val pricePerSign = if (isGoldFio) {
        getServicePrice("letter_gold", 5.50)
    } else {
        getServicePrice("letter_standard", 1.60)
    }
    val fioPrice = (countedFioLetters * pricePerSign)

    val countedEpitaphLetters = remember(epitaphText) {
        epitaphText.count { it.isLetterOrDigit() }
    }
    val epitaphSignPrice = getServicePrice("letter_epitaph", 1.50)
    val epitaphPrice = (countedEpitaphLetters * epitaphSignPrice)

    val totalEstimatedWeightKg = (steleMassKg + plinthMassKg + flowerbedMassKg).toInt()

    // Totals
    val stoneTotal = stelePrice + plinthPrice + flowerbedPrice
    val engravingTotal = fioPrice + epitaphPrice
    val monumentGrandTotal = stoneTotal + engravingTotal

    val steleSizeDesc = "${steleH.toInt()}×${steleW.toInt()}×${steleT.toInt()} см"
    val plinthSizeDesc = "${plinthL.toInt()}×${plinthW.toInt()}×${plinthH.toInt()} см"
    val fbSizeDesc = "${fbL.toInt()}×${fbW.toInt()}×${fbT.toInt()} см"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Dialog Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Конструктор памятника",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Единый камень для стелы, тумбы и цветника • Расчёт по объёму",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Main Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // === SECTION 1: UNIFIED STONE SELECTION ===
                    SectionHeader(title = "1. Выбор камня (единый для всего комплекта)", icon = Icons.Default.Terrain)

                    Text(
                        text = "Выбранный материал применяется для расчёта стоимости стелы, тумбы и цветника по их объёму:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StoneMaterialSelector(
                        materials = actualMaterials,
                        selected = selectedStone,
                        onSelect = { selectedStone = it },
                        usdRate = usdRate
                    )

                    HorizontalDivider()

                    // === SECTION 2: DIMENSIONS & VOLUME CALCULATIONS ===
                    SectionHeader(title = "2. Размеры и объёмы элементов", icon = Icons.Default.Straighten)

                    // --- 2.1 STELE CARD ---
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Стела (памятник)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = PriceFormatter.formatWithUsd(stelePrice, usdRate),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = steleHeightStr,
                                    onValueChange = { steleHeightStr = it },
                                    label = { Text("Высота (см)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("custom_height_input")
                                )
                                OutlinedTextField(
                                    value = steleWidthStr,
                                    onValueChange = { steleWidthStr = it },
                                    label = { Text("Ширина (см)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("custom_width_input")
                                )
                                OutlinedTextField(
                                    value = steleThicknessStr,
                                    onValueChange = { steleThicknessStr = it },
                                    label = { Text("Толщина (см)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("custom_thickness_input")
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Объём: ${"%.4f".format(java.util.Locale.US, steleVolumeM3)} м³  •  Вес: ~${steleMassKg.toInt()} кг",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "V × Цена камня",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // --- 2.2 PLINTH CARD ---
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = includePlinth,
                                        onCheckedChange = { includePlinth = it }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Тумба (подставка)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }

                                Text(
                                    text = if (includePlinth) PriceFormatter.formatWithUsd(plinthPrice, usdRate) else "0 BYN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (includePlinth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (includePlinth) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = plinthLengthStr,
                                        onValueChange = { plinthLengthStr = it },
                                        label = { Text("Длина (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = plinthWidthStr,
                                        onValueChange = { plinthWidthStr = it },
                                        label = { Text("Ширина (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = plinthHeightStr,
                                        onValueChange = { plinthHeightStr = it },
                                        label = { Text("Высота (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Объём: ${"%.4f".format(java.util.Locale.US, plinthVolumeM3)} м³  •  Вес: ~${plinthMassKg.toInt()} кг",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "V × Цена камня",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- 2.3 FLOWERBED CARD ---
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = includeFlowerbed,
                                        onCheckedChange = { includeFlowerbed = it }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Цветник / надгробие", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }

                                Text(
                                    text = if (includeFlowerbed) PriceFormatter.formatWithUsd(flowerbedPrice, usdRate) else "0 BYN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeFlowerbed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (includeFlowerbed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !isSolidSlabFlowerbed,
                                        onClick = { isSolidSlabFlowerbed = false },
                                        label = { Text("Открытый (3 бруса)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = isSolidSlabFlowerbed,
                                        onClick = { isSolidSlabFlowerbed = true },
                                        label = { Text("Сплошная плита") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = flowerbedLengthStr,
                                        onValueChange = { flowerbedLengthStr = it },
                                        label = { Text("Длина (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = flowerbedWidthStr,
                                        onValueChange = { flowerbedWidthStr = it },
                                        label = { Text("Ширина (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = flowerbedThicknessStr,
                                        onValueChange = { flowerbedThicknessStr = it },
                                        label = { Text("Толщина (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!isSolidSlabFlowerbed) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = flowerbedBarWidthStr,
                                            onValueChange = { flowerbedBarWidthStr = it },
                                            label = { Text("Ширина бруса (см)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "2 длинных + 1 поперечный брус",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Объём: ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³  •  Вес: ~${flowerbedMassKg.toInt()} кг",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "V × Цена камня",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // === SECTION 3: ENGRAVING ===
                    SectionHeader(title = "3. Гравировка ФИО и эпитафии", icon = Icons.Default.Edit)

                    // Text & Dates
                    Text("Гравировка ФИО и дат:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = fioText,
                        onValueChange = { fioText = it },
                        label = { Text("Текст ФИО и дат") },
                        modifier = Modifier.fillMaxWidth().testTag("fio_text_input"),
                        minLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Знаков: $countedFioLetters (${PriceFormatter.formatWithUsd(fioPrice, usdRate)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Сусальное золото (${PriceFormatter.formatWithUsd(getServicePrice("letter_gold", 5.50), usdRate)}/зн)", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = isGoldFio,
                                onCheckedChange = { isGoldFio = it },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Epitaph
                    Text("Эпитафия (памятная надпись):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = epitaphText,
                        onValueChange = { epitaphText = it },
                        label = { Text("Текст эпитафии (оставьте пустым, если нет)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1
                    )
                    if (countedEpitaphLetters > 0) {
                        Text(
                            text = "Знаков эпитафии: $countedEpitaphLetters (${PriceFormatter.formatWithUsd(epitaphPrice, usdRate)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(10.dp))

                    // === SUMMARY COST BREAKDOWN CARD ===
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Сводная смета комплекта памятника:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            CostRow("Камень комплекта:", selectedStone.name)
                            CostRow("Стела ($steleSizeDesc, ${"%.3f".format(java.util.Locale.US, steleVolumeM3)} м³):", PriceFormatter.formatWithUsd(stelePrice, usdRate))
                            if (includePlinth) {
                                CostRow("Тумба ($plinthSizeDesc, ${"%.3f".format(java.util.Locale.US, plinthVolumeM3)} м³):", PriceFormatter.formatWithUsd(plinthPrice, usdRate))
                            }
                            if (includeFlowerbed) {
                                CostRow("Цветник ($fbSizeDesc, ${"%.3f".format(java.util.Locale.US, flowerbedVolumeM3)} м³):", PriceFormatter.formatWithUsd(flowerbedPrice, usdRate))
                            }
                            if (fioPrice > 0) {
                                CostRow("ФИО ($countedFioLetters зн.):", PriceFormatter.formatWithUsd(fioPrice, usdRate))
                            }
                            if (epitaphPrice > 0) {
                                CostRow("Эпитафия ($countedEpitaphLetters зн.):", PriceFormatter.formatWithUsd(epitaphPrice, usdRate))
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ИТОГО ЗА ПАМЯТНИК:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Примерный общий вес: ~$totalEstimatedWeightKg кг", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = PriceFormatter.formatWithUsd(monumentGrandTotal, usdRate),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Bottom Action Buttons: Single Complete Set OR Detailed Items
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Add as detailed multiple items
                                val items = mutableListOf<CalculatedItemData>()

                                items.add(
                                    CalculatedItemData(
                                        category = ItemCategory.MONUMENTS.displayName,
                                        name = "Стела $steleSizeDesc (${selectedStone.name})",
                                        unit = "шт",
                                        unitPrice = stelePrice,
                                        quantity = 1.0,
                                        totalPrice = stelePrice,
                                        customNote = "Объём: ${"%.4f".format(java.util.Locale.US, steleVolumeM3)} м³, камень: ${selectedStone.name}"
                                    )
                                )

                                if (includePlinth) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.MONUMENTS.displayName,
                                            name = "Тумба (подставка) $plinthSizeDesc (${selectedStone.name})",
                                            unit = "шт",
                                            unitPrice = plinthPrice,
                                            quantity = 1.0,
                                            totalPrice = plinthPrice,
                                            customNote = "Объём: ${"%.4f".format(java.util.Locale.US, plinthVolumeM3)} м³"
                                        )
                                    )
                                }

                                if (includeFlowerbed) {
                                    val fbTypeStr = if (isSolidSlabFlowerbed) "Надгробная плита" else "Цветник (3 бруса)"
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.MONUMENTS.displayName,
                                            name = "$fbTypeStr $fbSizeDesc (${selectedStone.name})",
                                            unit = "компл",
                                            unitPrice = flowerbedPrice,
                                            quantity = 1.0,
                                            totalPrice = flowerbedPrice,
                                            customNote = "Объём: ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³"
                                        )
                                    )
                                }

                                if (fioText.isNotBlank() || fioPrice > 0) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.ENGRAVING.displayName,
                                            name = "Гравировка ФИО и дат (${if (isGoldFio) "Золото" else "Классика"})",
                                            unit = "знак",
                                            unitPrice = pricePerSign,
                                            quantity = (if (countedFioLetters > 0) countedFioLetters else 1).toDouble(),
                                            totalPrice = fioPrice,
                                            customNote = fioText.trim()
                                        )
                                    )
                                }

                                if (epitaphText.isNotBlank() || epitaphPrice > 0) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.ENGRAVING.displayName,
                                            name = "Гравировка эпитафии",
                                            unit = "знак",
                                            unitPrice = epitaphSignPrice,
                                            quantity = (if (countedEpitaphLetters > 0) countedEpitaphLetters else 1).toDouble(),
                                            totalPrice = epitaphPrice,
                                            customNote = epitaphText.trim()
                                        )
                                    )
                                }

                                onAddMultipleItems(items)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("add_monument_detailed_btn")
                        ) {
                            Text("По позициям", maxLines = 1)
                        }

                        Button(
                            onClick = {
                                // Add as single complete bundle item
                                val fullDescription = buildString {
                                    append("Камень: ${selectedStone.name}. ")
                                    append("Стела: $steleSizeDesc (${"%.3f".format(java.util.Locale.US, steleVolumeM3)} м³). ")
                                    if (includePlinth) append("Тумба: $plinthSizeDesc (${"%.3f".format(java.util.Locale.US, plinthVolumeM3)} м³). ")
                                    if (includeFlowerbed) append("Цветник: $fbSizeDesc (${"%.3f".format(java.util.Locale.US, flowerbedVolumeM3)} м³). ")
                                    if (fioText.isNotBlank()) append("Гравировка ФИО: [${fioText.trim()}] | ")
                                    if (epitaphText.isNotBlank()) append("Эпитафия: [${epitaphText.trim()}] | ")
                                    append("Вес: ~$totalEstimatedWeightKg кг.")
                                }

                                val title = "Комплект памятника $steleSizeDesc (${selectedStone.name})"
                                onAddSingleItem(
                                    title,
                                    ItemCategory.MONUMENTS.displayName,
                                    "компл",
                                    monumentGrandTotal,
                                    1.0,
                                    fullDescription
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.3f).testTag("add_monument_single_btn")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В смету (комплект)", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CostRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StoneMaterialSelector(
    materials: List<StoneMaterial>,
    selected: StoneMaterial,
    onSelect: (StoneMaterial) -> Unit,
    usdRate: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        materials.forEach { mat ->
            val isSelected = selected.id == mat.id
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(mat) }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(mat) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = mat.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${PriceFormatter.formatWithUsd(mat.pricePerM3, usdRate)} / м³",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${mat.colorName} • ${mat.origin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
