package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.util.FontHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonumentBuilderDialog(
    materials: List<StoneMaterial> = emptyList(),
    sizePresets: List<MonumentSizePreset> = emptyList(),
    servicePrices: Map<String, Double> = emptyMap(),
    constructorServicePrices: List<ConstructorServicePriceItem> = emptyList(),
    fonts: List<EngravingFontItem> = emptyList(),
    drawings: List<EngravingDrawingItem> = emptyList(),
    photoSizes: List<PhotoSizeItem> = emptyList(),
    photoFrames: List<PhotoFrameItem> = emptyList(),
    vases: List<VaseItem> = emptyList(),
    usdRate: Double = 3.25,
    eurRate: Double = 3.55,
    initialFioText: String = "",
    initialEpitaphText: String = "",
    onUpdateFactoryDetails: ((
        deceasedLastName: String,
        deceasedFirstName: String,
        deceasedMiddleName: String,
        birthDate: String,
        deathDate: String,
        crossInfo: String,
        photoVignetteInfo: String,
        frameInfo: String,
        epitaphText: String,
        plateDecoration: String,
        additionsInfo: String,
        monumentMaterial: String,
        obeliskInfo: String,
        plinthInfo: String,
        flowerbedInfo: String,
        slabInfo: String,
        vaseInfo: String,
        dismantlingInfo: String
    ) -> Unit)? = null,
    onDismiss: () -> Unit,
    onAddSingleItem: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double, note: String) -> Unit,
    onAddMultipleItems: (List<CalculatedItemData>) -> Unit
) {
    val scrollState = rememberScrollState()

    val actualMaterials = materials
    val actualPresets = sizePresets
    val actualFonts = fonts
    val actualDrawings = drawings
    val actualPhotoSizes = remember(photoSizes) {
        photoSizes.ifEmpty { com.example.data.DefaultCatalog.getDefaultPhotoSizes() }
    }
    val actualPhotoFrames = remember(photoFrames) {
        photoFrames.ifEmpty { com.example.data.DefaultCatalog.getDefaultPhotoFrames() }
    }
    val actualVases = remember(vases) {
        vases.ifEmpty { com.example.data.DefaultCatalog.getDefaultVases() }
    }


    val actualServicePrices = remember(servicePrices, constructorServicePrices) {
        val map = servicePrices.toMutableMap()
        constructorServicePrices.forEach { item ->
            map[item.key] = item.price
        }
        map
    }

    val actualEurRate = remember(actualServicePrices, eurRate) {
        actualServicePrices["eur_exchange_rate"] ?: eurRate
    }

    val builtInKeys = remember {
        setOf(
            "carving_wave", "carving_cross", "carving_flowers", "carving_angel_tree", "carving_rock", "carving_exclusive",
            "chamfer_polished_10mm", "chamfer_round", "plinth_high", "flowerbed_half_slab", "flowerbed_solid_slab", "flowerbed_double",
            "portrait_laser_a4", "portrait_hand_retouch", "portrait_full_height", "portrait_ceramic_13x18", "portrait_triplex",
            "letter_standard", "letter_painted", "letter_gold", "letter_gold_large", "letter_gold_small", "letter_epitaph",
            "usd_exchange_rate", "eur_exchange_rate",
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
        mutableStateOf(actualMaterials.firstOrNull())
    }

    // 2. Dimensions & Volume Calculations
    // Stele Dimensions (cm)
    var steleHeightStr by remember { mutableStateOf("100") }
    var steleWidthStr by remember { mutableStateOf("50") }
    var steleThicknessStr by remember { mutableStateOf("8") }

    // Annex / Pristavka to Stele (cm)
    var includeAnnex by remember { mutableStateOf(false) }
    var annexUseDifferentStone by remember { mutableStateOf(true) }
    var selectedAnnexStone by remember(actualMaterials) {
        mutableStateOf(if (actualMaterials.size > 1) actualMaterials[1] else actualMaterials.firstOrNull())
    }
    var annexHeightStr by remember { mutableStateOf("80") }
    var annexWidthStr by remember { mutableStateOf("25") }
    var annexThicknessStr by remember { mutableStateOf("8") }

    // Shelf under Stele / Полка под стелу (cm) - from main stone
    var includeShelf by remember { mutableStateOf(false) }
    var shelfLengthStr by remember { mutableStateOf("70") }
    var shelfWidthStr by remember { mutableStateOf("15") }
    var shelfThicknessStr by remember { mutableStateOf("5") }

    // Plinth (Тумба) Dimensions (cm) & Two-part configuration
    var includePlinth by remember { mutableStateOf(true) }
    var plinthHasTwoParts by remember { mutableStateOf(false) }

    // Plinth Part 1 (Основная / нижняя / левая)
    var plinthLengthStr by remember { mutableStateOf("60") }
    var plinthWidthStr by remember { mutableStateOf("20") }
    var plinthHeightStr by remember { mutableStateOf("15") }
    var plinth1UseDifferentStone by remember { mutableStateOf(false) }
    var selectedPlinth1Stone by remember(actualMaterials) {
        mutableStateOf(actualMaterials.firstOrNull())
    }

    // Plinth Part 2 (Дополнительная / верхняя / правая)
    var plinth2LengthStr by remember { mutableStateOf("40") }
    var plinth2WidthStr by remember { mutableStateOf("20") }
    var plinth2HeightStr by remember { mutableStateOf("15") }
    var plinth2UseDifferentStone by remember { mutableStateOf(true) }
    var selectedPlinth2Stone by remember(actualMaterials) {
        mutableStateOf(if (actualMaterials.size > 1) actualMaterials[1] else actualMaterials.firstOrNull())
    }

    // Flowerbed (Цветник открытый из брусьев) Dimensions (cm)
    var includeFlowerbed by remember { mutableStateOf(true) }
    // Modes: "OPEN_JOINT" (со стыками - 3 бруса), "OPEN_SUM" (без вычитания стыков - 3 бруса), "CLOSED_4" (закрытый 4 бруса)
    var flowerbedMode by remember { mutableStateOf("OPEN_JOINT") }
    var flowerbedLengthStr by remember { mutableStateOf("100") }
    var flowerbedWidthStr by remember { mutableStateOf("60") }
    var flowerbedThicknessStr by remember { mutableStateOf("8") }
    var flowerbedBarWidthStr by remember { mutableStateOf("5") }
    var flowerbedUseDifferentStone by remember { mutableStateOf(false) }
    var selectedFlowerbedStone by remember(actualMaterials) {
        mutableStateOf(actualMaterials.firstOrNull())
    }

    // Tombstone Slab (Надгробная плита) Dimensions (cm) & Two-part configuration
    var includeSlab by remember { mutableStateOf(false) }
    var slabHasTwoParts by remember { mutableStateOf(false) }

    // Slab Part 1 (Основная / полуплита 1)
    var slabLengthStr by remember { mutableStateOf("100") }
    var slabWidthStr by remember { mutableStateOf("60") }
    var slabThicknessStr by remember { mutableStateOf("5") }
    var slab1UseDifferentStone by remember { mutableStateOf(false) }
    var selectedSlab1Stone by remember(actualMaterials) {
        mutableStateOf(actualMaterials.firstOrNull())
    }

    // Slab Part 2 (Дополнительная / полуплита 2)
    var slab2LengthStr by remember { mutableStateOf("100") }
    var slab2WidthStr by remember { mutableStateOf("30") }
    var slab2ThicknessStr by remember { mutableStateOf("5") }
    var slab2UseDifferentStone by remember { mutableStateOf(true) }
    var selectedSlab2Stone by remember(actualMaterials) {
        mutableStateOf(if (actualMaterials.size > 1) actualMaterials[1] else actualMaterials.firstOrNull())
    }

    // --- CALCULATIONS ---

    // Granite density ~ 2900 kg/m³
    val stoneDensityKgM3 = 2900.0
    val stonePricePerM3 = selectedStone?.pricePerM3 ?: 0.0

    // 1) Stele Calculations
    val steleH = steleHeightStr.toDoubleOrNull() ?: 100.0
    val steleW = steleWidthStr.toDoubleOrNull() ?: 50.0
    val steleT = steleThicknessStr.toDoubleOrNull() ?: 8.0
    val steleVolumeM3 = (steleH * steleW * steleT) / 1_000_000.0
    val steleMassKg = steleVolumeM3 * stoneDensityKgM3
    val stelePrice = steleVolumeM3 * stonePricePerM3

    // 2) Annex (Приставка к стеле) Calculations
    val effectiveAnnexStone = if (annexUseDifferentStone && selectedAnnexStone != null) selectedAnnexStone else selectedStone
    val annexStonePricePerM3 = effectiveAnnexStone?.pricePerM3 ?: 0.0
    val annexH = annexHeightStr.toDoubleOrNull() ?: 80.0
    val annexW = annexWidthStr.toDoubleOrNull() ?: 25.0
    val annexT = annexThicknessStr.toDoubleOrNull() ?: 8.0
    val annexVolumeM3 = if (includeAnnex) (annexH * annexW * annexT) / 1_000_000.0 else 0.0
    val annexMassKg = annexVolumeM3 * stoneDensityKgM3
    val annexPrice = annexVolumeM3 * annexStonePricePerM3
    val annexSizeDesc = "${annexH.toInt()}×${annexW.toInt()}×${annexT.toInt()} см"
    val annexStoneName = effectiveAnnexStone?.name ?: (selectedStone?.name ?: "Камень")

    // 3) Shelf under Stele (Полка под стелу) Calculations - always from main stone
    val shelfL = shelfLengthStr.toDoubleOrNull() ?: 70.0
    val shelfW = shelfWidthStr.toDoubleOrNull() ?: 15.0
    val shelfT = shelfThicknessStr.toDoubleOrNull() ?: 5.0
    val shelfVolumeM3 = if (includeShelf) (shelfL * shelfW * shelfT) / 1_000_000.0 else 0.0
    val shelfMassKg = shelfVolumeM3 * stoneDensityKgM3
    val shelfPrice = shelfVolumeM3 * stonePricePerM3
    val shelfSizeDesc = "${shelfL.toInt()}×${shelfW.toInt()}×${shelfT.toInt()} см"

    // 4) Plinth (Тумба) Calculations
    // Part 1 (Основная)
    val effectivePlinth1Stone = if (plinth1UseDifferentStone && selectedPlinth1Stone != null) selectedPlinth1Stone else selectedStone
    val plinth1StonePricePerM3 = effectivePlinth1Stone?.pricePerM3 ?: 0.0
    val plinth1L = plinthLengthStr.toDoubleOrNull() ?: 60.0
    val plinth1W = plinthWidthStr.toDoubleOrNull() ?: 20.0
    val plinth1H = plinthHeightStr.toDoubleOrNull() ?: 15.0
    val plinth1VolumeM3 = if (includePlinth) (plinth1L * plinth1W * plinth1H) / 1_000_000.0 else 0.0
    val plinth1MassKg = plinth1VolumeM3 * stoneDensityKgM3
    val plinth1Price = plinth1VolumeM3 * plinth1StonePricePerM3
    val plinth1SizeDesc = "${plinth1L.toInt()}×${plinth1W.toInt()}×${plinth1H.toInt()} см"
    val plinth1StoneName = effectivePlinth1Stone?.name ?: (selectedStone?.name ?: "Камень")

    // Part 2 (Дополнительная - if 2 parts enabled)
    val effectivePlinth2Stone = if (plinth2UseDifferentStone && selectedPlinth2Stone != null) selectedPlinth2Stone else selectedStone
    val plinth2StonePricePerM3 = effectivePlinth2Stone?.pricePerM3 ?: 0.0
    val plinth2L = plinth2LengthStr.toDoubleOrNull() ?: 40.0
    val plinth2W = plinth2WidthStr.toDoubleOrNull() ?: 20.0
    val plinth2H = plinth2HeightStr.toDoubleOrNull() ?: 15.0
    val plinth2VolumeM3 = if (includePlinth && plinthHasTwoParts) (plinth2L * plinth2W * plinth2H) / 1_000_000.0 else 0.0
    val plinth2MassKg = plinth2VolumeM3 * stoneDensityKgM3
    val plinth2Price = plinth2VolumeM3 * plinth2StonePricePerM3
    val plinth2SizeDesc = "${plinth2L.toInt()}×${plinth2W.toInt()}×${plinth2H.toInt()} см"
    val plinth2StoneName = effectivePlinth2Stone?.name ?: (selectedStone?.name ?: "Камень")

    val plinthVolumeM3 = plinth1VolumeM3 + plinth2VolumeM3
    val plinthMassKg = plinth1MassKg + plinth2MassKg
    val plinthPrice = plinth1Price + plinth2Price
    val plinthSizeDesc = if (plinthHasTwoParts) {
        "ч.1: $plinth1SizeDesc, ч.2: $plinth2SizeDesc"
    } else {
        plinth1SizeDesc
    }

    // 5) Flowerbed (Цветник) Calculations
    val effectiveFlowerbedStone = if (flowerbedUseDifferentStone && selectedFlowerbedStone != null) selectedFlowerbedStone else selectedStone
    val flowerbedStonePricePerM3 = effectiveFlowerbedStone?.pricePerM3 ?: 0.0
    val flowerbedStoneName = effectiveFlowerbedStone?.name ?: (selectedStone?.name ?: "Камень")
    val fbL = flowerbedLengthStr.toDoubleOrNull() ?: 100.0
    val fbW = flowerbedWidthStr.toDoubleOrNull() ?: 60.0
    val fbT = flowerbedThicknessStr.toDoubleOrNull() ?: 8.0
    val fbBarW = flowerbedBarWidthStr.toDoubleOrNull() ?: 5.0

    val flowerbedVolumeM3: Double
    val flowerbedMathDesc: String

    if (!includeFlowerbed || includeSlab) {
        flowerbedVolumeM3 = 0.0
        flowerbedMathDesc = if (includeSlab) "Без цветника (установлена надгробная плита)" else "Без цветника"
    } else {
        when (flowerbedMode) {
            "CLOSED_4" -> {
                val longBarsCm3 = 2.0 * fbL * fbT * fbBarW
                val innerCrossW = (fbW - 2.0 * fbBarW).coerceAtLeast(0.0)
                val crossBarsCm3 = 2.0 * innerCrossW * fbT * fbBarW
                val totalCm3 = longBarsCm3 + crossBarsCm3
                flowerbedVolumeM3 = totalCm3 / 1_000_000.0
                flowerbedMathDesc = "4 бруса закрытый: 2 бруса ${fbL.toInt()}×${fbBarW.toInt()}×${fbT.toInt()} + 2 перемычки ${innerCrossW.toInt()}×${fbBarW.toInt()}×${fbT.toInt()} см = ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³"
            }
            "OPEN_SUM" -> {
                // Direct sum L+L+W without corner deduction
                val longBarsCm3 = 2.0 * fbL * fbT * fbBarW
                val crossBarCm3 = fbW * fbT * fbBarW
                val totalCm3 = longBarsCm3 + crossBarCm3
                flowerbedVolumeM3 = totalCm3 / 1_000_000.0
                flowerbedMathDesc = "3 бруса (сумма длин): 2×(${fbL.toInt()}×${fbBarW.toInt()}×${fbT.toInt()}) + 1×(${fbW.toInt()}×${fbBarW.toInt()}×${fbT.toInt()}) см = ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³"
            }
            else -> {
                // "OPEN_JOINT" - Accurate physical joint accounting (2 long bars L + 1 cross bar W - 2*BarW)
                val longBarsCm3 = 2.0 * fbL * fbT * fbBarW
                val innerCrossW = (fbW - 2.0 * fbBarW).coerceAtLeast(0.0)
                val crossBarCm3 = innerCrossW * fbT * fbBarW
                val totalCm3 = longBarsCm3 + crossBarCm3
                flowerbedVolumeM3 = totalCm3 / 1_000_000.0
                flowerbedMathDesc = "3 бруса со стыком: 2 бруса ${fbL.toInt()}×${fbBarW.toInt()}×${fbT.toInt()} + 1 перемычка ${innerCrossW.toInt()}×${fbBarW.toInt()}×${fbT.toInt()} см = ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³"
            }
        }
    }
    val flowerbedMassKg = flowerbedVolumeM3 * stoneDensityKgM3
    val flowerbedPrice = flowerbedVolumeM3 * flowerbedStonePricePerM3

    // 6) Tombstone Slab (Надгробная плита) Calculations
    // Part 1 (Основная / полуплита 1)
    val effectiveSlab1Stone = if (slab1UseDifferentStone && selectedSlab1Stone != null) selectedSlab1Stone else selectedStone
    val slab1StonePricePerM3 = effectiveSlab1Stone?.pricePerM3 ?: 0.0
    val slab1L = slabLengthStr.toDoubleOrNull() ?: 100.0
    val slab1W = slabWidthStr.toDoubleOrNull() ?: 60.0
    val slab1T = slabThicknessStr.toDoubleOrNull() ?: 5.0
    val slab1VolumeM3 = if (includeSlab) (slab1L * slab1W * slab1T) / 1_000_000.0 else 0.0
    val slab1MassKg = slab1VolumeM3 * stoneDensityKgM3
    val slab1Price = slab1VolumeM3 * slab1StonePricePerM3
    val slab1SizeDesc = "${slab1L.toInt()}×${slab1W.toInt()}×${slab1T.toInt()} см"
    val slab1StoneName = effectiveSlab1Stone?.name ?: (selectedStone?.name ?: "Камень")

    // Part 2 (Дополнительная / полуплита 2 - if 2 parts enabled)
    val effectiveSlab2Stone = if (slab2UseDifferentStone && selectedSlab2Stone != null) selectedSlab2Stone else selectedStone
    val slab2StonePricePerM3 = effectiveSlab2Stone?.pricePerM3 ?: 0.0
    val slab2L = slab2LengthStr.toDoubleOrNull() ?: 100.0
    val slab2W = slab2WidthStr.toDoubleOrNull() ?: 30.0
    val slab2T = slab2ThicknessStr.toDoubleOrNull() ?: 5.0
    val slab2VolumeM3 = if (includeSlab && slabHasTwoParts) (slab2L * slab2W * slab2T) / 1_000_000.0 else 0.0
    val slab2MassKg = slab2VolumeM3 * stoneDensityKgM3
    val slab2Price = slab2VolumeM3 * slab2StonePricePerM3
    val slab2SizeDesc = "${slab2L.toInt()}×${slab2W.toInt()}×${slab2T.toInt()} см"
    val slab2StoneName = effectiveSlab2Stone?.name ?: (selectedStone?.name ?: "Камень")

    val slabVolumeM3 = slab1VolumeM3 + slab2VolumeM3
    val slabMassKg = slab1MassKg + slab2MassKg
    val slabPrice = slab1Price + slab2Price
    val slabSizeDesc = if (slabHasTwoParts) {
        "ч.1: $slab1SizeDesc, ч.2: $slab2SizeDesc"
    } else {
        slab1SizeDesc
    }

    // 3. Engraving (ФИО и эпитафия)
    var fioText by remember(initialFioText) {
        mutableStateOf(
            if (initialFioText.isNotBlank()) initialFioText
            else "Иванов Иван Иванович\n15.05.1950 — 20.08.2024"
        )
    }
    var fioLetteringMode by remember { mutableStateOf("STANDARD") } // "STANDARD", "PAINTED", "GOLD"
    val isGoldFio = (fioLetteringMode == "GOLD")
    val isPaintedFio = (fioLetteringMode == "PAINTED")

    var selectedFontId by remember(actualFonts) {
        mutableStateOf(actualFonts.firstOrNull()?.id)
    }
    val selectedFont = remember(actualFonts, selectedFontId) {
        actualFonts.find { it.id == selectedFontId } ?: actualFonts.firstOrNull()
    }
    var isFontSelectorExpanded by remember { mutableStateOf(false) }

    var showManualFioCounts by remember { mutableStateOf(false) }
    var manualLargeLettersStr by remember { mutableStateOf("") }
    var manualSmallLettersStr by remember { mutableStateOf("") }
    var manualDigitsStr by remember { mutableStateOf("") }

    var epitaphText by remember(initialEpitaphText) {
        mutableStateOf(
            if (initialEpitaphText.isNotBlank()) initialEpitaphText
            else "Помним, любим, скорбим..."
        )
    }
    var epitaphLetteringMode by remember { mutableStateOf("STANDARD") } // "STANDARD", "PAINTED", "GOLD"
    val isGoldEpitaph = (epitaphLetteringMode == "GOLD")
    val isPaintedEpitaph = (epitaphLetteringMode == "PAINTED")

    // Lettering tariffs: Standard (BYN), Painted (BYN 10.00), Gold (EUR 8€ / 7€)
    val goldLargeEurPrice = getServicePrice("letter_gold_large", 8.00)
    val goldSmallEurPrice = getServicePrice("letter_gold_small", 7.00)
    val standardLetterPrice = getServicePrice("letter_standard", 1.60)
    val paintedLetterPrice = getServicePrice("letter_painted", 10.00)
    val epitaphSignPrice = getServicePrice("letter_epitaph", 1.50)

    val autoLargeCount = remember(fioText) { fioText.count { it.isLetter() && it.isUpperCase() } }
    val autoSmallCount = remember(fioText) { fioText.count { it.isLetter() && !it.isUpperCase() } }
    val autoDigitsCount = remember(fioText) { fioText.count { it.isDigit() } }

    val fioLargeLetters = if (showManualFioCounts && manualLargeLettersStr.isNotBlank()) {
        manualLargeLettersStr.toIntOrNull() ?: autoLargeCount
    } else {
        autoLargeCount
    }

    val fioSmallLetters = if (showManualFioCounts && manualSmallLettersStr.isNotBlank()) {
        manualSmallLettersStr.toIntOrNull() ?: autoSmallCount
    } else {
        autoSmallCount
    }

    val fioDigits = if (showManualFioCounts && manualDigitsStr.isNotBlank()) {
        manualDigitsStr.toIntOrNull() ?: autoDigitsCount
    } else {
        autoDigitsCount
    }

    val countedFioLetters = fioLargeLetters + fioSmallLetters + fioDigits

    // FIO Calculations
    val fioGoldTotalEur = (fioLargeLetters * goldLargeEurPrice) + ((fioSmallLetters + fioDigits) * goldSmallEurPrice)
    val fioGoldPriceByn = fioGoldTotalEur * actualEurRate
    val fioPaintedPriceByn = countedFioLetters * paintedLetterPrice
    val fioStandardPriceByn = countedFioLetters * standardLetterPrice

    val fioPrice = when (fioLetteringMode) {
        "PAINTED" -> fioPaintedPriceByn
        "GOLD" -> fioGoldPriceByn
        else -> fioStandardPriceByn
    }
    val pricePerSign = when (fioLetteringMode) {
        "PAINTED" -> paintedLetterPrice
        "GOLD" -> if (countedFioLetters > 0) fioPrice / countedFioLetters else (goldSmallEurPrice * actualEurRate)
        else -> standardLetterPrice
    }

    // Epitaph counts & pricing
    var showManualEpitaphCounts by remember { mutableStateOf(false) }
    var manualEpitaphLargeStr by remember { mutableStateOf("") }
    var manualEpitaphSmallStr by remember { mutableStateOf("") }
    var manualEpitaphDigitsStr by remember { mutableStateOf("") }

    val autoEpitaphLarge = remember(epitaphText) { epitaphText.count { it.isLetter() && it.isUpperCase() } }
    val autoEpitaphSmall = remember(epitaphText) { epitaphText.count { it.isLetter() && !it.isUpperCase() } }
    val autoEpitaphDigits = remember(epitaphText) { epitaphText.count { it.isDigit() } }
    val autoEpitaphAllNonSpace = remember(epitaphText) { epitaphText.count { !it.isWhitespace() } }

    val epitaphLargeLetters = if (showManualEpitaphCounts && manualEpitaphLargeStr.isNotBlank()) {
        manualEpitaphLargeStr.toIntOrNull() ?: autoEpitaphLarge
    } else {
        autoEpitaphLarge
    }

    val epitaphSmallLetters = if (showManualEpitaphCounts && manualEpitaphSmallStr.isNotBlank()) {
        manualEpitaphSmallStr.toIntOrNull() ?: autoEpitaphSmall
    } else {
        autoEpitaphSmall
    }

    val epitaphDigits = if (showManualEpitaphCounts && manualEpitaphDigitsStr.isNotBlank()) {
        manualEpitaphDigitsStr.toIntOrNull() ?: autoEpitaphDigits
    } else {
        autoEpitaphDigits
    }

    val countedEpitaphLetters = if (showManualEpitaphCounts) {
        epitaphLargeLetters + epitaphSmallLetters + epitaphDigits
    } else if (epitaphLetteringMode == "GOLD") {
        autoEpitaphLarge + autoEpitaphSmall + autoEpitaphDigits
    } else {
        autoEpitaphAllNonSpace
    }

    val epitaphGoldTotalEur = (epitaphLargeLetters * goldLargeEurPrice) + ((epitaphSmallLetters + epitaphDigits) * goldSmallEurPrice)
    val epitaphGoldPriceByn = epitaphGoldTotalEur * actualEurRate
    val epitaphPaintedPriceByn = countedEpitaphLetters * paintedLetterPrice
    val epitaphStandardPriceByn = countedEpitaphLetters * epitaphSignPrice

    val epitaphPrice = when (epitaphLetteringMode) {
        "PAINTED" -> epitaphPaintedPriceByn
        "GOLD" -> epitaphGoldPriceByn
        else -> epitaphStandardPriceByn
    }

    // 4. Drawings & Decor (Крест, Цветок, Иконы, Ангелы, Свечи)
    var selectedDrawingIds by remember { mutableStateOf(setOf<String>()) }
    var drawingCategoryFilter by remember { mutableStateOf("Все") }
    var isDrawingPickerOpen by remember { mutableStateOf(false) }

    val selectedDrawingsList = remember(actualDrawings, selectedDrawingIds) {
        actualDrawings.filter { it.id in selectedDrawingIds }
    }
    val drawingsTotal = selectedDrawingsList.sumOf { it.price }

    // 5. Photo / Portrait & Frame (Фотокерамика, Металлокерамика, Стекло, Рамки)
    var includePhoto by remember { mutableStateOf(false) }
    var selectedPhotoSize by remember(actualPhotoSizes) { mutableStateOf(actualPhotoSizes.firstOrNull()) }
    var selectedPhotoFrame by remember(actualPhotoFrames) { mutableStateOf(actualPhotoFrames.firstOrNull()) }
    var isPhotoSizeDropdownExpanded by remember { mutableStateOf(false) }
    var isPhotoFrameDropdownExpanded by remember { mutableStateOf(false) }

    val photoSizePrice = if (includePhoto) (selectedPhotoSize?.price ?: 0.0) else 0.0
    val photoFramePrice = if (includePhoto) (selectedPhotoFrame?.price ?: 0.0) else 0.0
    val photoTotal = photoSizePrice + photoFramePrice

    // 6. Vases & Accessories (Вазы гранитные и кованые)
    var includeVase by remember { mutableStateOf(false) }
    var selectedVase by remember(actualVases) { mutableStateOf(actualVases.firstOrNull()) }
    var vaseQuantity by remember { mutableIntStateOf(1) }
    var isVaseDropdownExpanded by remember { mutableStateOf(false) }

    val vaseUnitPrice = if (includeVase) (selectedVase?.price ?: 0.0) else 0.0
    val vaseTotal = vaseUnitPrice * vaseQuantity

    // 7. Works: Installation and Dismantling (Монтаж и демонтаж)
    var includeInstallation by remember { mutableStateOf(false) }
    val defaultInstallPrice = getServicePrice("installation_base_price", 500.0)
    var installationPriceStr by remember { mutableStateOf(defaultInstallPrice.toInt().toString()) }
    val installationPrice = if (includeInstallation) (installationPriceStr.toDoubleOrNull() ?: defaultInstallPrice) else 0.0

    var includeDemontazh by remember { mutableStateOf(false) }
    val defaultDemontazhPrice = getServicePrice("demontazh_base_price", 150.0)
    var demontazhPriceStr by remember { mutableStateOf(defaultDemontazhPrice.toInt().toString()) }
    var demontazhNote by remember { mutableStateOf("Демонтаж старого надгробия / креста") }
    val demontazhPrice = if (includeDemontazh) (demontazhPriceStr.toDoubleOrNull() ?: defaultDemontazhPrice) else 0.0

    val worksTotal = installationPrice + demontazhPrice

    val totalEstimatedWeightKg = (steleMassKg + annexMassKg + shelfMassKg + plinthMassKg + flowerbedMassKg + slabMassKg).toInt()

    // Totals
    val stoneTotal = stelePrice + annexPrice + shelfPrice + plinthPrice + flowerbedPrice + slabPrice
    val engravingTotal = fioPrice + epitaphPrice
    val monumentGrandTotal = stoneTotal + engravingTotal + drawingsTotal + photoTotal + vaseTotal + worksTotal



    val steleSizeDesc = "${steleH.toInt()}×${steleW.toInt()}×${steleT.toInt()} см"
    val fbSizeDesc = "${fbL.toInt()}×${fbW.toInt()}×${fbT.toInt()} см"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .widthIn(max = 920.dp)
                    .fillMaxHeight(0.94f),
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
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Стела (памятник)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = PriceFormatter.formatWithUsd(stelePrice, usdRate),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = steleHeightStr,
                                    onValueChange = { steleHeightStr = it },
                                    label = { Text("Высота H (см)", style = MaterialTheme.typography.labelSmall) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("custom_height_input")
                                )
                                OutlinedTextField(
                                    value = steleWidthStr,
                                    onValueChange = { steleWidthStr = it },
                                    label = { Text("Ширина W (см)", style = MaterialTheme.typography.labelSmall) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("custom_width_input")
                                )
                                OutlinedTextField(
                                    value = steleThicknessStr,
                                    onValueChange = { steleThicknessStr = it },
                                    label = { Text("Толщина (см)", style = MaterialTheme.typography.labelSmall) },
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Объём: ${"%.4f".format(java.util.Locale.US, steleVolumeM3)} м³  •  Вес: ~${steleMassKg.toInt()} кг",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "V × Цена камня",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // --- 2.2 ANNEX (ПРИСТАВКА К СТЕЛЕ) CARD ---
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (includeAnnex) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = if (includeAnnex) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includeAnnex,
                                        onCheckedChange = { includeAnnex = it },
                                        modifier = Modifier.testTag("include_annex_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Приставка к стеле", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }

                                Text(
                                    text = if (includeAnnex) PriceFormatter.formatWithUsd(annexPrice, usdRate) else "0 BYN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeAnnex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }

                            if (includeAnnex) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = annexHeightStr,
                                        onValueChange = { annexHeightStr = it },
                                        label = { Text("H (см)", style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("annex_height_input")
                                    )
                                    OutlinedTextField(
                                        value = annexWidthStr,
                                        onValueChange = { annexWidthStr = it },
                                        label = { Text("W (см)", style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("annex_width_input")
                                    )
                                    OutlinedTextField(
                                        value = annexThicknessStr,
                                        onValueChange = { annexThicknessStr = it },
                                        label = { Text("Толщ. (см)", style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("annex_thickness_input")
                                    )
                                }

                                ElementStoneSelector(
                                    title = "Камень приставки:",
                                    useDifferentStone = annexUseDifferentStone,
                                    onUseDifferentStoneChange = { annexUseDifferentStone = it },
                                    selectedStone = effectiveAnnexStone,
                                    onSelectStone = { selectedAnnexStone = it },
                                    materials = actualMaterials,
                                    usdRate = usdRate,
                                    mainStoneName = selectedStone?.name ?: "—",
                                    mainStonePrice = stonePricePerM3,
                                    switchTestTag = "annex_different_stone_switch"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${"%.4f".format(java.util.Locale.US, annexVolumeM3)} м³ • ~${annexMassKg.toInt()} кг",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "V × ${PriceFormatter.formatRub(annexStonePricePerM3)} BYN/м³",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // --- 2.3 SHELF UNDER STELE (ПОЛКА ПОД СТЕЛУ) CARD ---
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (includeShelf) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = if (includeShelf) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includeShelf,
                                        onCheckedChange = { includeShelf = it },
                                        modifier = Modifier.testTag("include_shelf_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Полка под стелу", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }

                                Text(
                                    text = if (includeShelf) PriceFormatter.formatWithUsd(shelfPrice, usdRate) else "0 BYN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeShelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }

                            if (includeShelf) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = shelfLengthStr,
                                        onValueChange = { shelfLengthStr = it },
                                        label = { Text("L (см)", style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("shelf_length_input")
                                    )
                                    OutlinedTextField(
                                        value = shelfWidthStr,
                                        onValueChange = { shelfWidthStr = it },
                                        label = { Text("W (см)", style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("shelf_width_input")
                                    )
                                    OutlinedTextField(
                                        value = shelfThicknessStr,
                                        onValueChange = { shelfThicknessStr = it },
                                        label = { Text("Толщ. (см)", style = MaterialTheme.typography.labelSmall) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("shelf_thickness_input")
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${"%.4f".format(java.util.Locale.US, shelfVolumeM3)} м³ • ~${shelfMassKg.toInt()} кг",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "V × Цена камня",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // --- 2.4 PLINTH CARD ---
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (includePlinth) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = if (includePlinth) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includePlinth,
                                        onCheckedChange = { includePlinth = it },
                                        modifier = Modifier.testTag("include_plinth_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Тумба (подставка)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }

                                Text(
                                    text = if (includePlinth) PriceFormatter.formatWithUsd(plinthPrice, usdRate) else "0 BYN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (includePlinth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }

                            if (includePlinth) {
                                // Switch for two parts
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Состоит из 2 частей (составная)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = plinthHasTwoParts,
                                            onCheckedChange = { plinthHasTwoParts = it },
                                            modifier = Modifier.testTag("plinth_two_parts_switch")
                                        )
                                    }
                                }

                                if (!plinthHasTwoParts) {
                                    // Single Plinth
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = plinthLengthStr,
                                            onValueChange = { plinthLengthStr = it },
                                            label = { Text("L (см)", style = MaterialTheme.typography.labelSmall) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("plinth_length_input")
                                        )
                                        OutlinedTextField(
                                            value = plinthWidthStr,
                                            onValueChange = { plinthWidthStr = it },
                                            label = { Text("W (см)", style = MaterialTheme.typography.labelSmall) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("plinth_width_input")
                                        )
                                        OutlinedTextField(
                                            value = plinthHeightStr,
                                            onValueChange = { plinthHeightStr = it },
                                            label = { Text("H (см)", style = MaterialTheme.typography.labelSmall) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("plinth_height_input")
                                        )
                                    }

                                    ElementStoneSelector(
                                        title = "Камень тумбы:",
                                        useDifferentStone = plinth1UseDifferentStone,
                                        onUseDifferentStoneChange = { plinth1UseDifferentStone = it },
                                        selectedStone = effectivePlinth1Stone,
                                        onSelectStone = { selectedPlinth1Stone = it },
                                        materials = actualMaterials,
                                        usdRate = usdRate,
                                        mainStoneName = selectedStone?.name ?: "—",
                                        mainStonePrice = stonePricePerM3,
                                        switchTestTag = "plinth_different_stone_switch"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${"%.4f".format(java.util.Locale.US, plinth1VolumeM3)} м³ • ~${plinth1MassKg.toInt()} кг",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "V × ${PriceFormatter.formatRub(plinth1StonePricePerM3)} BYN/м³",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    // Plinth Part 1
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Часть 1 (Нижняя / левая)",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Text(
                                                    text = PriceFormatter.formatWithUsd(plinth1Price, usdRate),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = plinthLengthStr,
                                                    onValueChange = { plinthLengthStr = it },
                                                    label = { Text("L (см)", style = MaterialTheme.typography.labelSmall) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("plinth1_length_input")
                                                )
                                                OutlinedTextField(
                                                    value = plinthWidthStr,
                                                    onValueChange = { plinthWidthStr = it },
                                                    label = { Text("W (см)", style = MaterialTheme.typography.labelSmall) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("plinth1_width_input")
                                                )
                                                OutlinedTextField(
                                                    value = plinthHeightStr,
                                                    onValueChange = { plinthHeightStr = it },
                                                    label = { Text("H (см)", style = MaterialTheme.typography.labelSmall) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("plinth1_height_input")
                                                )
                                            }

                                            ElementStoneSelector(
                                                title = "Камень ч.1:",
                                                useDifferentStone = plinth1UseDifferentStone,
                                                onUseDifferentStoneChange = { plinth1UseDifferentStone = it },
                                                selectedStone = effectivePlinth1Stone,
                                                onSelectStone = { selectedPlinth1Stone = it },
                                                materials = actualMaterials,
                                                usdRate = usdRate,
                                                mainStoneName = selectedStone?.name ?: "—",
                                                mainStonePrice = stonePricePerM3,
                                                switchTestTag = "plinth1_different_stone_switch"
                                            )

                                            Text(
                                                text = "Объём ч.1: ${"%.4f".format(java.util.Locale.US, plinth1VolumeM3)} м³  •  Вес: ~${plinth1MassKg.toInt()} кг  •  Камень: $plinth1StoneName",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Plinth Part 2
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Часть 2 (Верхняя / правая)",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Text(
                                                    text = PriceFormatter.formatWithUsd(plinth2Price, usdRate),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = plinth2LengthStr,
                                                    onValueChange = { plinth2LengthStr = it },
                                                    label = { Text("L (см)", style = MaterialTheme.typography.labelSmall) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("plinth2_length_input")
                                                )
                                                OutlinedTextField(
                                                    value = plinth2WidthStr,
                                                    onValueChange = { plinth2WidthStr = it },
                                                    label = { Text("W (см)", style = MaterialTheme.typography.labelSmall) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("plinth2_width_input")
                                                )
                                                OutlinedTextField(
                                                    value = plinth2HeightStr,
                                                    onValueChange = { plinth2HeightStr = it },
                                                    label = { Text("H (см)", style = MaterialTheme.typography.labelSmall) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).testTag("plinth2_height_input")
                                                )
                                            }

                                            ElementStoneSelector(
                                                title = "Камень ч.2:",
                                                useDifferentStone = plinth2UseDifferentStone,
                                                onUseDifferentStoneChange = { plinth2UseDifferentStone = it },
                                                selectedStone = effectivePlinth2Stone,
                                                onSelectStone = { selectedPlinth2Stone = it },
                                                materials = actualMaterials,
                                                usdRate = usdRate,
                                                mainStoneName = selectedStone?.name ?: "—",
                                                mainStonePrice = stonePricePerM3,
                                                switchTestTag = "plinth2_different_stone_switch"
                                            )

                                            Text(
                                                text = "Объём ч.2: ${"%.4f".format(java.util.Locale.US, plinth2VolumeM3)} м³  •  Вес: ~${plinth2MassKg.toInt()} кг  •  Камень: $plinth2StoneName",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Всего тумба (2 части): ${"%.4f".format(java.util.Locale.US, plinthVolumeM3)} м³  •  ~${plinthMassKg.toInt()} кг",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = PriceFormatter.formatWithUsd(plinthPrice, usdRate),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(start = 8.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 2.5 FLOWERBED CARD ---
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
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includeFlowerbed,
                                        onCheckedChange = { checked ->
                                            includeFlowerbed = checked
                                            if (checked) {
                                                includeSlab = false
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (includeSlab) "Цветник (заменен плитой)" else "Цветник (открытый из брусьев)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (includeSlab) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = flowerbedMode == "OPEN_JOINT",
                                        onClick = { flowerbedMode = "OPEN_JOINT" },
                                        label = { Text("3 бруса со стыком", style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = flowerbedMode == "OPEN_SUM",
                                        onClick = { flowerbedMode = "OPEN_SUM" },
                                        label = { Text("3 бруса (сумма)", style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = flowerbedMode == "CLOSED_4",
                                        onClick = { flowerbedMode = "CLOSED_4" },
                                        label = { Text("4 бруса (закрытый)", style = MaterialTheme.typography.bodySmall) },
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
                                        label = { Text("Длина L (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = flowerbedWidthStr,
                                        onValueChange = { flowerbedWidthStr = it },
                                        label = { Text("Ширина W (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = flowerbedThicknessStr,
                                        onValueChange = { flowerbedThicknessStr = it },
                                        label = { Text("Высота H (см)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = flowerbedBarWidthStr,
                                    onValueChange = { flowerbedBarWidthStr = it },
                                    label = { Text("Ширина сечения бруса (см)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                ElementStoneSelector(
                                    title = "Камень для цветника:",
                                    useDifferentStone = flowerbedUseDifferentStone,
                                    onUseDifferentStoneChange = { flowerbedUseDifferentStone = it },
                                    selectedStone = effectiveFlowerbedStone,
                                    onSelectStone = { selectedFlowerbedStone = it },
                                    materials = actualMaterials,
                                    usdRate = usdRate,
                                    mainStoneName = selectedStone?.name ?: "—",
                                    mainStonePrice = stonePricePerM3,
                                    switchTestTag = "flowerbed_different_stone_switch"
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "📐 $flowerbedMathDesc",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Объём: ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³  •  Вес: ~${flowerbedMassKg.toInt()} кг  •  Стоимость: ${PriceFormatter.formatWithUsd(flowerbedPrice, usdRate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- 2.6 TOMBSTONE SLAB (НАДГРОБНАЯ ПЛИТА) CARD ---
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
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includeSlab,
                                        onCheckedChange = { checked ->
                                            includeSlab = checked
                                            if (checked) {
                                                includeFlowerbed = false
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (includeFlowerbed) "Надгробная плита (заменена цветником)" else "Надгробная плита",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (includeFlowerbed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = if (includeSlab) PriceFormatter.formatWithUsd(slabPrice, usdRate) else "0 BYN",
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeSlab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (includeSlab) {
                                // 1 part vs 2 parts switch
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Конфигурация плиты:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (slabHasTwoParts) "2 части (составная / 2 полуплиты)" else "1 сплошная плита",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (slabHasTwoParts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (slabHasTwoParts) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }

                                        Switch(
                                            checked = slabHasTwoParts,
                                            onCheckedChange = { slabHasTwoParts = it },
                                            modifier = Modifier.testTag("slab_two_parts_switch")
                                        )
                                    }
                                }

                                if (!slabHasTwoParts) {
                                    // Single Slab Configuration
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = slabLengthStr,
                                            onValueChange = { slabLengthStr = it },
                                            label = { Text("Длина L (см)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = slabWidthStr,
                                            onValueChange = { slabWidthStr = it },
                                            label = { Text("Ширина W (см)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = slabThicknessStr,
                                            onValueChange = { slabThicknessStr = it },
                                            label = { Text("Толщина (см)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    ElementStoneSelector(
                                        title = "Камень для надгробной плиты:",
                                        useDifferentStone = slab1UseDifferentStone,
                                        onUseDifferentStoneChange = { slab1UseDifferentStone = it },
                                        selectedStone = effectiveSlab1Stone,
                                        onSelectStone = { selectedSlab1Stone = it },
                                        materials = actualMaterials,
                                        usdRate = usdRate,
                                        mainStoneName = selectedStone?.name ?: "—",
                                        mainStonePrice = stonePricePerM3,
                                        switchTestTag = "slab1_different_stone_switch"
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Объём: ${"%.4f".format(java.util.Locale.US, slab1VolumeM3)} м³  •  Вес: ~${slab1MassKg.toInt()} кг",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = PriceFormatter.formatWithUsd(slabPrice, usdRate),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    // 2 Parts Slab Configuration
                                    // --- PART 1 (Основная / полуплита 1) ---
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Часть 1 (полуплита 1)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                                Text(
                                                    text = PriceFormatter.formatWithUsd(slab1Price, usdRate),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = slabLengthStr,
                                                    onValueChange = { slabLengthStr = it },
                                                    label = { Text("Длина (см)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = slabWidthStr,
                                                    onValueChange = { slabWidthStr = it },
                                                    label = { Text("Ширина (см)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = slabThicknessStr,
                                                    onValueChange = { slabThicknessStr = it },
                                                    label = { Text("Толщина (см)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            ElementStoneSelector(
                                                title = "Камень для части 1:",
                                                useDifferentStone = slab1UseDifferentStone,
                                                onUseDifferentStoneChange = { slab1UseDifferentStone = it },
                                                selectedStone = effectiveSlab1Stone,
                                                onSelectStone = { selectedSlab1Stone = it },
                                                materials = actualMaterials,
                                                usdRate = usdRate,
                                                mainStoneName = selectedStone?.name ?: "—",
                                                mainStonePrice = stonePricePerM3,
                                                switchTestTag = "slab1_part_different_stone_switch"
                                            )

                                            Text(
                                                text = "Объём ч.1: ${"%.4f".format(java.util.Locale.US, slab1VolumeM3)} м³  •  Вес: ~${slab1MassKg.toInt()} кг",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // --- PART 2 (Дополнительная / полуплита 2) ---
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Часть 2 (полуплита 2)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                                Text(
                                                    text = PriceFormatter.formatWithUsd(slab2Price, usdRate),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = slab2LengthStr,
                                                    onValueChange = { slab2LengthStr = it },
                                                    label = { Text("Длина (см)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = slab2WidthStr,
                                                    onValueChange = { slab2WidthStr = it },
                                                    label = { Text("Ширина (см)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = slab2ThicknessStr,
                                                    onValueChange = { slab2ThicknessStr = it },
                                                    label = { Text("Толщина (см)") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            ElementStoneSelector(
                                                title = "Камень для части 2:",
                                                useDifferentStone = slab2UseDifferentStone,
                                                onUseDifferentStoneChange = { slab2UseDifferentStone = it },
                                                selectedStone = effectiveSlab2Stone,
                                                onSelectStone = { selectedSlab2Stone = it },
                                                materials = actualMaterials,
                                                usdRate = usdRate,
                                                mainStoneName = selectedStone?.name ?: "—",
                                                mainStonePrice = stonePricePerM3,
                                                switchTestTag = "slab2_part_different_stone_switch"
                                            )

                                            Text(
                                                text = "Объём ч.2: ${"%.4f".format(java.util.Locale.US, slab2VolumeM3)} м³  •  Вес: ~${slab2MassKg.toInt()} кг",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Total 2-part Slab summary bar
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Всего плита (2 части): ${"%.4f".format(java.util.Locale.US, slabVolumeM3)} м³  •  ~${slabMassKg.toInt()} кг",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = PriceFormatter.formatWithUsd(slabPrice, usdRate),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
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

                    // Font Selection Selector & Live Visual Stone Plaque
                    if (actualFonts.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FontDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Шрифт гравировки:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        color = if ((selectedFont?.price ?: 0.0) <= 0.0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if ((selectedFont?.price ?: 0.0) <= 0.0) "Бесплатно" else PriceFormatter.formatRub(selectedFont?.price ?: 0.0),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if ((selectedFont?.price ?: 0.0) <= 0.0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = isFontSelectorExpanded,
                                    onExpandedChange = { isFontSelectorExpanded = !isFontSelectorExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = "${selectedFont?.name ?: "По умолчанию"} (${FontHelper.getStyleDisplayName(selectedFont?.styleKey ?: "SERIF")})",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFontSelectorExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isFontSelectorExpanded,
                                        onDismissRequest = { isFontSelectorExpanded = false }
                                    ) {
                                        actualFonts.forEach { fontItem ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = fontItem.name,
                                                                fontWeight = if (fontItem.id == selectedFont?.id) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (fontItem.id == selectedFont?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Пример: Иванов И.И. 1950 — 2024",
                                                                fontFamily = FontHelper.getFontFamily(fontItem.styleKey),
                                                                fontWeight = FontHelper.getFontWeight(fontItem.styleKey),
                                                                fontStyle = FontHelper.getFontStyle(fontItem.styleKey),
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (fontItem.price > 0.0) {
                                                            Text(
                                                                text = PriceFormatter.formatRub(fontItem.price),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "Бесплатно",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedFontId = fontItem.id
                                                    isFontSelectorExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Live visual rendering of text on simulated stone plaque
                                if (fioText.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1B1E22), shape = RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ОБРАЗЕЦ НАДПИСИ НА КАМНЕ:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF8E9297),
                                                    fontSize = 9.sp
                                                )
                                                Text(
                                                    text = selectedFont?.name ?: "Шрифт",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFA5B4FC),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = fioText.trim(),
                                                fontFamily = FontHelper.getFontFamily(selectedFont?.styleKey ?: "SERIF"),
                                                fontWeight = FontHelper.getFontWeight(selectedFont?.styleKey ?: "SERIF"),
                                                fontStyle = FontHelper.getFontStyle(selectedFont?.styleKey ?: "SERIF"),
                                                fontSize = 16.sp,
                                                lineHeight = 22.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                color = when (fioLetteringMode) {
                                                    "GOLD" -> Color(0xFFFACC15)
                                                    "PAINTED" -> Color(0xFFFFFFFF)
                                                    else -> Color(0xFFE2E8F0)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (epitaphText.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = epitaphText.trim(),
                                                    fontFamily = FontHelper.getFontFamily(selectedFont?.styleKey ?: "SERIF"),
                                                    fontWeight = FontWeight.Normal,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    fontSize = 13.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    color = when (epitaphLetteringMode) {
                                                        "GOLD" -> Color(0xFFFACC15).copy(alpha = 0.9f)
                                                        "PAINTED" -> Color(0xFFFFFFFF).copy(alpha = 0.9f)
                                                        else -> Color(0xFFCBD5E1)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Engraving Mode Selection
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (fioLetteringMode) {
                                "GOLD" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                "PAINTED" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (fioLetteringMode) {
                                            "GOLD" -> Icons.Default.AutoAwesome
                                            "PAINTED" -> Icons.Default.Brush
                                            else -> Icons.Default.FormatQuote
                                        },
                                        contentDescription = null,
                                        tint = when (fioLetteringMode) {
                                            "GOLD" -> MaterialTheme.colorScheme.tertiary
                                            "PAINTED" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = when (fioLetteringMode) {
                                                "GOLD" -> "Сусальное золото (8€ / 7€)"
                                                "PAINTED" -> "Покраска букв (10.00 BYN / знак)"
                                                else -> "Классическая гравировка"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (fioLetteringMode) {
                                                "GOLD" -> "Большие 8 €, маленькие и цифры 7 € (1 € = ${"%.2f".format(java.util.Locale.US, actualEurRate)} BYN)"
                                                "PAINTED" -> "Окраска букв и цифр: ${"%.2f".format(java.util.Locale.US, paintedLetterPrice)} BYN / знак"
                                                else -> "Стандартный шрифт: ${"%.2f".format(java.util.Locale.US, standardLetterPrice)} BYN / знак"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 3-way mode selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = fioLetteringMode == "STANDARD",
                                    onClick = { fioLetteringMode = "STANDARD" },
                                    label = { Text("Классика", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = fioLetteringMode == "PAINTED",
                                    onClick = { fioLetteringMode = "PAINTED" },
                                    label = { Text("Покраска (10р)", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1.1f)
                                )
                                FilterChip(
                                    selected = fioLetteringMode == "GOLD",
                                    onClick = { fioLetteringMode = "GOLD" },
                                    label = { Text("Золото (8€/7€)", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            if (isGoldFio) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                                // Breakdown Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Заглавные (8 €)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$fioLargeLetters шт. × 8€", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("${fioLargeLetters * 8} €", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Строчные (7 €)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$fioSmallLetters шт. × 7€", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("${fioSmallLetters * 7} €", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Цифры (7 €)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$fioDigits шт. × 7€", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("${fioDigits * 7} €", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Итого золото: $countedFioLetters зн. = ${"%.2f".format(java.util.Locale.US, fioGoldTotalEur)} €",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = PriceFormatter.formatWithUsd(fioGoldPriceByn, usdRate),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Toggle manual count adjustments
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { showManualFioCounts = !showManualFioCounts },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showManualFioCounts) Icons.Default.ExpandLess else Icons.Default.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (showManualFioCounts) "Скрыть ручную правку" else "Уточнить знаки вручную",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                if (showManualFioCounts) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = manualLargeLettersStr,
                                            onValueChange = { manualLargeLettersStr = it },
                                            label = { Text("Больших", style = MaterialTheme.typography.labelSmall) },
                                            placeholder = { Text("$autoLargeCount") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = manualSmallLettersStr,
                                            onValueChange = { manualSmallLettersStr = it },
                                            label = { Text("Маленьких", style = MaterialTheme.typography.labelSmall) },
                                            placeholder = { Text("$autoSmallCount") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = manualDigitsStr,
                                            onValueChange = { manualDigitsStr = it },
                                            label = { Text("Цифр", style = MaterialTheme.typography.labelSmall) },
                                            placeholder = { Text("$autoDigitsCount") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPaintedFio) "Покраска: $countedFioLetters зн. × ${"%.2f".format(java.util.Locale.US, paintedLetterPrice)} BYN"
                                        else "Знаков: $countedFioLetters шт. × ${"%.2f".format(java.util.Locale.US, standardLetterPrice)} BYN",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = PriceFormatter.formatWithUsd(fioPrice, usdRate),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Epitaph
                    Text("Эпитафия (памятная надпись):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = epitaphText,
                        onValueChange = { epitaphText = it },
                        label = { Text("Текст эпитафии (оставьте пустым, если нет)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1
                    )
                    if (countedEpitaphLetters > 0 || epitaphText.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Оформление эпитафии: $countedEpitaphLetters зн.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = PriceFormatter.formatWithUsd(epitaphPrice, usdRate),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = epitaphLetteringMode == "STANDARD",
                                    onClick = { epitaphLetteringMode = "STANDARD" },
                                    label = { Text("Обычный", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = epitaphLetteringMode == "PAINTED",
                                    onClick = { epitaphLetteringMode = "PAINTED" },
                                    label = { Text("Покраска (10р)", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1.1f)
                                )
                                FilterChip(
                                    selected = epitaphLetteringMode == "GOLD",
                                    onClick = { epitaphLetteringMode = "GOLD" },
                                    label = { Text("Золото (8€/7€)", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            if (epitaphLetteringMode == "GOLD") {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Сусальное золото (курс 1€ = ${"%.2f".format(java.util.Locale.US, actualEurRate)} BYN)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            TextButton(
                                                onClick = { showManualEpitaphCounts = !showManualEpitaphCounts },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                            ) {
                                                Text(
                                                    text = if (showManualEpitaphCounts) "Авто-подсчет" else "Править знаки",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }

                                        Text(
                                            text = "$epitaphLargeLetters бол. × ${"%.0f".format(java.util.Locale.US, goldLargeEurPrice)}€ + ${epitaphSmallLetters + epitaphDigits} мал./знаков × ${"%.0f".format(java.util.Locale.US, goldSmallEurPrice)}€ = ${"%.2f".format(java.util.Locale.US, epitaphGoldTotalEur)} € (${"%.2f".format(java.util.Locale.US, epitaphGoldPriceByn)} BYN)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )

                                        if (showManualEpitaphCounts) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = manualEpitaphLargeStr,
                                                    onValueChange = { manualEpitaphLargeStr = it },
                                                    label = { Text("Больших", style = MaterialTheme.typography.labelSmall) },
                                                    placeholder = { Text("$autoEpitaphLarge") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = manualEpitaphSmallStr,
                                                    onValueChange = { manualEpitaphSmallStr = it },
                                                    label = { Text("Маленьких", style = MaterialTheme.typography.labelSmall) },
                                                    placeholder = { Text("$autoEpitaphSmall") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = manualEpitaphDigitsStr,
                                                    onValueChange = { manualEpitaphDigitsStr = it },
                                                    label = { Text("Знаков/цифр", style = MaterialTheme.typography.labelSmall) },
                                                    placeholder = { Text("$autoEpitaphDigits") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // === SECTION: DRAWINGS AND DECORATIVE ENGRAVING ===
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Рисунки и декор на камне",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (selectedDrawingsList.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${selectedDrawingsList.size} шт (${PriceFormatter.formatRub(drawingsTotal)})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (selectedDrawingsList.isNotEmpty()) {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    selectedDrawingsList.forEach { drawing ->
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedDrawingIds = selectedDrawingIds - drawing.id },
                                            label = { Text("${drawing.code} ${drawing.name} (${PriceFormatter.formatRub(drawing.price)})") },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Удалить",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Рисунки не выбраны. Выберите кресты, цветы, иконы или свечи из каталога.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { isDrawingPickerOpen = true },
                                modifier = Modifier.fillMaxWidth().testTag("open_drawing_picker_btn")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (selectedDrawingIds.isEmpty()) "Выбрать рисунки и декор из каталога"
                                    else "Изменить выбранные рисунки (${selectedDrawingIds.size} шт.)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // === SECTION: PHOTO & FRAME / PORTRAIT ===
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Портрет / Фотокерамика / Рамка",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Switch(
                                    checked = includePhoto,
                                    onCheckedChange = { includePhoto = it },
                                    modifier = Modifier.testTag("include_photo_switch")
                                )
                            }

                            if (includePhoto) {
                                Text(
                                    text = "Выберите размер/тип фото (фотокерамика, фото на стекле, гравированный портрет) и вариант оформления (рамка/врезка):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Dropdown 1: Photo Size
                                ExposedDropdownMenuBox(
                                    expanded = isPhotoSizeDropdownExpanded,
                                    onExpandedChange = { isPhotoSizeDropdownExpanded = !isPhotoSizeDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedPhotoSize?.let { "${it.category} — ${it.sizeName} (${PriceFormatter.formatRub(it.price)})" } ?: "Выберите вариант...",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Размер и категория портрета") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPhotoSizeDropdownExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isPhotoSizeDropdownExpanded,
                                        onDismissRequest = { isPhotoSizeDropdownExpanded = false }
                                    ) {
                                        val grouped = actualPhotoSizes.groupBy { it.category }
                                        grouped.forEach { (cat, items) ->
                                            DropdownMenuItem(
                                                text = { Text("— $cat —", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                                onClick = {},
                                                enabled = false
                                            )
                                            items.forEach { sizeItem ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${sizeItem.sizeName} ${if (sizeItem.description.isNotBlank()) "(${sizeItem.description})" else ""}")
                                                            Text(PriceFormatter.formatRub(sizeItem.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedPhotoSize = sizeItem
                                                        isPhotoSizeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Dropdown 2: Photo Frame
                                ExposedDropdownMenuBox(
                                    expanded = isPhotoFrameDropdownExpanded,
                                    onExpandedChange = { isPhotoFrameDropdownExpanded = !isPhotoFrameDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedPhotoFrame?.let { "${it.name} [${it.materialType}] (${PriceFormatter.formatRub(it.price)})" } ?: "Без рамки",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Рамка / Врезка / Оформление") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPhotoFrameDropdownExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isPhotoFrameDropdownExpanded,
                                        onDismissRequest = { isPhotoFrameDropdownExpanded = false }
                                    ) {
                                        actualPhotoFrames.forEach { frameItem ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("${frameItem.name} (${frameItem.materialType})")
                                                        Text(PriceFormatter.formatRub(frameItem.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                },
                                                onClick = {
                                                    selectedPhotoFrame = frameItem
                                                    isPhotoFrameDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Итого за фото и оформление:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = PriceFormatter.formatWithUsd(photoTotal, usdRate),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // === SECTION: VASES & ACCESSORIES ===
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFlorist,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ваза (Гранитная / Кованая)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Switch(
                                    checked = includeVase,
                                    onCheckedChange = { includeVase = it },
                                    modifier = Modifier.testTag("include_vase_switch")
                                )
                            }

                            if (includeVase) {
                                Text(
                                    text = "Выберите тип и модель вазы из каталога (гранитные полувазы, кованые вазы):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isVaseDropdownExpanded,
                                    onExpandedChange = { isVaseDropdownExpanded = !isVaseDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedVase?.let { "${it.name} (${it.materialType}, ${it.sizeCm}) — ${PriceFormatter.formatRub(it.price)}" } ?: "Выберите вазу...",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Модель вазы") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVaseDropdownExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isVaseDropdownExpanded,
                                        onDismissRequest = { isVaseDropdownExpanded = false }
                                    ) {
                                        val grouped = actualVases.groupBy { it.materialType }
                                        grouped.forEach { (matType, items) ->
                                            DropdownMenuItem(
                                                text = { Text("— $matType —", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                                onClick = {},
                                                enabled = false
                                            )
                                            items.forEach { vaseItem ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${vaseItem.name} (${vaseItem.sizeCm})")
                                                            Text(PriceFormatter.formatRub(vaseItem.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedVase = vaseItem
                                                        isVaseDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Количество: ", style = MaterialTheme.typography.bodyMedium)
                                        IconButton(
                                            onClick = { if (vaseQuantity > 1) vaseQuantity-- },
                                            enabled = vaseQuantity > 1
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Меньше")
                                        }
                                        Text(
                                            text = "$vaseQuantity шт.",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(onClick = { vaseQuantity++ }) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Больше")
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Итого за вазу:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = PriceFormatter.formatWithUsd(vaseTotal, usdRate),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(10.dp))

                    // === SECTION: INSTALLATION AND DISMANTLING (МОНТАЖ И ДЕМОНТАЖ) ===
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Монтаж и демонтаж на кладбище", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                if (worksTotal > 0) {
                                    Text(
                                        text = PriceFormatter.formatWithUsd(worksTotal, usdRate),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }

                            // --- Монтаж (установка) ---
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (includeInstallation) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = includeInstallation,
                                                onCheckedChange = { includeInstallation = it }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Монтаж (установка комплекта)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = if (includeInstallation) PriceFormatter.formatWithUsd(installationPrice, usdRate) else "0 BYN",
                                            fontWeight = FontWeight.Bold,
                                            color = if (includeInstallation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (includeInstallation) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Slider(
                                                value = (installationPriceStr.toFloatOrNull() ?: 500f).coerceIn(0f, 4000f),
                                                onValueChange = { installationPriceStr = it.toInt().toString() },
                                                valueRange = 0f..4000f,
                                                steps = 79,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = installationPriceStr,
                                                onValueChange = { installationPriceStr = it },
                                                label = { Text("Сумма (BYN)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(130.dp)
                                            )
                                        }

                                        // Quick presets
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(400, 500, 800, 1000, 1200, 1500, 2000).forEach { preset ->
                                                FilterChip(
                                                    selected = (installationPriceStr.toIntOrNull() == preset),
                                                    onClick = { installationPriceStr = preset.toString() },
                                                    label = { Text("${preset}р", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // --- Демонтаж ---
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (includeDemontazh) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = includeDemontazh,
                                                onCheckedChange = { includeDemontazh = it }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Демонтаж старого надгробия / креста", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = if (includeDemontazh) PriceFormatter.formatWithUsd(demontazhPrice, usdRate) else "0 BYN",
                                            fontWeight = FontWeight.Bold,
                                            color = if (includeDemontazh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (includeDemontazh) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Slider(
                                                value = (demontazhPriceStr.toFloatOrNull() ?: 150f).coerceIn(0f, 1000f),
                                                onValueChange = { demontazhPriceStr = it.toInt().toString() },
                                                valueRange = 0f..1000f,
                                                steps = 39,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = demontazhPriceStr,
                                                onValueChange = { demontazhPriceStr = it },
                                                label = { Text("Сумма (BYN)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.width(130.dp)
                                            )
                                        }

                                        // Quick presets & description
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(50, 100, 150, 200, 300, 400).forEach { preset ->
                                                FilterChip(
                                                    selected = (demontazhPriceStr.toIntOrNull() == preset),
                                                    onClick = { demontazhPriceStr = preset.toString() },
                                                    label = { Text("${preset}р", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = demontazhNote,
                                            onValueChange = { demontazhNote = it },
                                            label = { Text("Описание демонтажа (для завода и сметы)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // === SUMMARY COST BREAKDOWN CARD ===
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Сводная смета комплекта памятника:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            CostRow("Камень комплекта:", selectedStone?.name ?: "— (не выбран)")
                            CostRow("Стела ($steleSizeDesc, ${"%.3f".format(java.util.Locale.US, steleVolumeM3)} м³):", PriceFormatter.formatWithUsd(stelePrice, usdRate))
                            if (includeAnnex) {
                                CostRow("Приставка ($annexSizeDesc, ${"%.3f".format(java.util.Locale.US, annexVolumeM3)} м³, $annexStoneName):", PriceFormatter.formatWithUsd(annexPrice, usdRate))
                            }
                            if (includeShelf) {
                                CostRow("Полка ($shelfSizeDesc, ${"%.3f".format(java.util.Locale.US, shelfVolumeM3)} м³):", PriceFormatter.formatWithUsd(shelfPrice, usdRate))
                            }
                            if (includePlinth) {
                                if (plinthHasTwoParts) {
                                    CostRow("Тумба ч.1 ($plinth1SizeDesc, ${"%.3f".format(java.util.Locale.US, plinth1VolumeM3)} м³, $plinth1StoneName):", PriceFormatter.formatWithUsd(plinth1Price, usdRate))
                                    CostRow("Тумба ч.2 ($plinth2SizeDesc, ${"%.3f".format(java.util.Locale.US, plinth2VolumeM3)} м³, $plinth2StoneName):", PriceFormatter.formatWithUsd(plinth2Price, usdRate))
                                } else {
                                    CostRow("Тумба ($plinth1SizeDesc, ${"%.3f".format(java.util.Locale.US, plinth1VolumeM3)} м³, $plinth1StoneName):", PriceFormatter.formatWithUsd(plinthPrice, usdRate))
                                }
                            }
                            if (includeFlowerbed) {
                                CostRow("Цветник ($fbSizeDesc, ${"%.3f".format(java.util.Locale.US, flowerbedVolumeM3)} м³, $flowerbedStoneName):", PriceFormatter.formatWithUsd(flowerbedPrice, usdRate))
                            }
                            if (includeSlab) {
                                if (slabHasTwoParts) {
                                    CostRow("Плита ч.1 ($slab1SizeDesc, ${"%.3f".format(java.util.Locale.US, slab1VolumeM3)} м³, $slab1StoneName):", PriceFormatter.formatWithUsd(slab1Price, usdRate))
                                    CostRow("Плита ч.2 ($slab2SizeDesc, ${"%.3f".format(java.util.Locale.US, slab2VolumeM3)} м³, $slab2StoneName):", PriceFormatter.formatWithUsd(slab2Price, usdRate))
                                } else {
                                    CostRow("Надгробная плита ($slab1SizeDesc, ${"%.3f".format(java.util.Locale.US, slabVolumeM3)} м³, $slab1StoneName):", PriceFormatter.formatWithUsd(slabPrice, usdRate))
                                }
                            }
                            if (fioPrice > 0) {
                                val fioTypeLabel = when (fioLetteringMode) {
                                    "PAINTED" -> "ФИО (покраска, $countedFioLetters зн.):"
                                    "GOLD" -> "ФИО (золото, $countedFioLetters зн.):"
                                    else -> "ФИО (гравировка, $countedFioLetters зн.):"
                                }
                                CostRow(fioTypeLabel, PriceFormatter.formatWithUsd(fioPrice, usdRate))
                            }
                            if (epitaphPrice > 0) {
                                val epTypeLabel = when (epitaphLetteringMode) {
                                    "PAINTED" -> "Эпитафия (покраска, $countedEpitaphLetters зн.):"
                                    "GOLD" -> "Эпитафия (золото, $countedEpitaphLetters зн.):"
                                    else -> "Эпитафия (гравировка, $countedEpitaphLetters зн.):"
                                }
                                CostRow(epTypeLabel, PriceFormatter.formatWithUsd(epitaphPrice, usdRate))
                            }
                            if (drawingsTotal > 0) {
                                val drawingsSummaryStr = selectedDrawingsList.joinToString(", ") { "${it.code} ${it.name}" }
                                CostRow("Рисунки и декор (${selectedDrawingsList.size} шт: $drawingsSummaryStr):", PriceFormatter.formatWithUsd(drawingsTotal, usdRate))
                            }
                            if (includePhoto && photoTotal > 0) {
                                val photoSizeDesc = selectedPhotoSize?.let { "${it.category} ${it.sizeName}" } ?: "Портрет"
                                val frameDesc = selectedPhotoFrame?.let { ", ${it.name}" } ?: ""
                                CostRow("Фото и рамка ($photoSizeDesc$frameDesc):", PriceFormatter.formatWithUsd(photoTotal, usdRate))
                            }
                            if (includeVase && vaseTotal > 0) {
                                val vaseDesc = selectedVase?.let { "${it.name} (${it.materialType}, ${it.sizeCm})" } ?: "Ваза"
                                CostRow("Ваза ($vaseDesc × $vaseQuantity шт):", PriceFormatter.formatWithUsd(vaseTotal, usdRate))
                            }
                            if (includeInstallation && installationPrice > 0) {
                                CostRow("Монтаж (установка комплекта):", PriceFormatter.formatWithUsd(installationPrice, usdRate))
                            }
                            if (includeDemontazh && demontazhPrice > 0) {
                                CostRow("Демонтаж ($demontazhNote):", PriceFormatter.formatWithUsd(demontazhPrice, usdRate))
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
                        val stoneNameStr = selectedStone?.name ?: "Без породы"
                        OutlinedButton(
                            onClick = {
                                // Add as detailed multiple items
                                val items = mutableListOf<CalculatedItemData>()

                                items.add(
                                    CalculatedItemData(
                                        category = ItemCategory.MONUMENTS.displayName,
                                        name = "Стела $steleSizeDesc ($stoneNameStr)",
                                        unit = "шт",
                                        unitPrice = stelePrice,
                                        quantity = 1.0,
                                        totalPrice = stelePrice,
                                        customNote = "Объём: ${"%.4f".format(java.util.Locale.US, steleVolumeM3)} м³, камень: $stoneNameStr"
                                    )
                                )

                                if (includeAnnex) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.MONUMENTS.displayName,
                                            name = "Приставка к стеле $annexSizeDesc ($annexStoneName)",
                                            unit = "шт",
                                            unitPrice = annexPrice,
                                            quantity = 1.0,
                                            totalPrice = annexPrice,
                                            customNote = "Объём: ${"%.4f".format(java.util.Locale.US, annexVolumeM3)} м³, камень: $annexStoneName"
                                        )
                                    )
                                }

                                if (includeShelf) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.MONUMENTS.displayName,
                                            name = "Полка под стелу $shelfSizeDesc ($stoneNameStr)",
                                            unit = "шт",
                                            unitPrice = shelfPrice,
                                            quantity = 1.0,
                                            totalPrice = shelfPrice,
                                            customNote = "Объём: ${"%.4f".format(java.util.Locale.US, shelfVolumeM3)} м³, материал: $stoneNameStr"
                                        )
                                    )
                                }

                                if (includePlinth) {
                                    if (plinthHasTwoParts) {
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.MONUMENTS.displayName,
                                                name = "Тумба ч.1 (основная) $plinth1SizeDesc ($plinth1StoneName)",
                                                unit = "шт",
                                                unitPrice = plinth1Price,
                                                quantity = 1.0,
                                                totalPrice = plinth1Price,
                                                customNote = "Объём: ${"%.4f".format(java.util.Locale.US, plinth1VolumeM3)} м³, камень: $plinth1StoneName"
                                            )
                                        )
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.MONUMENTS.displayName,
                                                name = "Тумба ч.2 (дополнительная) $plinth2SizeDesc ($plinth2StoneName)",
                                                unit = "шт",
                                                unitPrice = plinth2Price,
                                                quantity = 1.0,
                                                totalPrice = plinth2Price,
                                                customNote = "Объём: ${"%.4f".format(java.util.Locale.US, plinth2VolumeM3)} м³, камень: $plinth2StoneName"
                                            )
                                        )
                                    } else {
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.MONUMENTS.displayName,
                                                name = "Тумба (подставка) $plinth1SizeDesc ($plinth1StoneName)",
                                                unit = "шт",
                                                unitPrice = plinthPrice,
                                                quantity = 1.0,
                                                totalPrice = plinthPrice,
                                                customNote = "Объём: ${"%.4f".format(java.util.Locale.US, plinthVolumeM3)} м³, камень: $plinth1StoneName"
                                            )
                                        )
                                    }
                                }

                                if (includeFlowerbed) {
                                    val fbTypeStr = when (flowerbedMode) {
                                        "CLOSED_4" -> "Цветник (4 бруса закрытый)"
                                        "OPEN_SUM" -> "Цветник (3 бруса без вычитания стыка)"
                                        else -> "Цветник (3 бруса со стыком)"
                                    }
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.MONUMENTS.displayName,
                                            name = "$fbTypeStr $fbSizeDesc ($flowerbedStoneName)",
                                            unit = "компл",
                                            unitPrice = flowerbedPrice,
                                            quantity = 1.0,
                                            totalPrice = flowerbedPrice,
                                            customNote = "Объём: ${"%.4f".format(java.util.Locale.US, flowerbedVolumeM3)} м³, камень: $flowerbedStoneName"
                                        )
                                    )
                                }

                                if (includeSlab) {
                                    if (slabHasTwoParts) {
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.MONUMENTS.displayName,
                                                name = "Надгробная плита ч.1 $slab1SizeDesc ($slab1StoneName)",
                                                unit = "шт",
                                                unitPrice = slab1Price,
                                                quantity = 1.0,
                                                totalPrice = slab1Price,
                                                customNote = "Объём: ${"%.4f".format(java.util.Locale.US, slab1VolumeM3)} м³, камень: $slab1StoneName"
                                            )
                                        )
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.MONUMENTS.displayName,
                                                name = "Надгробная плита ч.2 $slab2SizeDesc ($slab2StoneName)",
                                                unit = "шт",
                                                unitPrice = slab2Price,
                                                quantity = 1.0,
                                                totalPrice = slab2Price,
                                                customNote = "Объём: ${"%.4f".format(java.util.Locale.US, slab2VolumeM3)} м³, камень: $slab2StoneName"
                                            )
                                        )
                                    } else {
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.MONUMENTS.displayName,
                                                name = "Надгробная плита $slab1SizeDesc ($slab1StoneName)",
                                                unit = "шт",
                                                unitPrice = slabPrice,
                                                quantity = 1.0,
                                                totalPrice = slabPrice,
                                                customNote = "Объём: ${"%.4f".format(java.util.Locale.US, slabVolumeM3)} м³, камень: $slab1StoneName"
                                            )
                                        )
                                    }
                                }

                                if (fioText.isNotBlank() || fioPrice > 0) {
                                    val fontNameSuffix = selectedFont?.let { " [Шрифт: ${it.name}]" } ?: ""
                                    val fioEngravingTitle = when (fioLetteringMode) {
                                        "PAINTED" -> "Покраска букв ФИО и дат$fontNameSuffix"
                                        "GOLD" -> "Сусальное золото ФИО и дат$fontNameSuffix"
                                        else -> "Гравировка ФИО и дат$fontNameSuffix"
                                    }
                                    val fioNote = fioText.trim()
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.ENGRAVING.displayName,
                                            name = fioEngravingTitle,
                                            unit = "компл",
                                            unitPrice = fioPrice,
                                            quantity = 1.0,
                                            totalPrice = fioPrice,
                                            customNote = fioNote
                                        )
                                    )
                                }

                                if (epitaphText.isNotBlank() || epitaphPrice > 0) {
                                    val epitaphEngravingTitle = when (epitaphLetteringMode) {
                                        "PAINTED" -> "Покраска эпитафии"
                                        "GOLD" -> "Сусальное золото эпитафии"
                                        else -> "Гравировка эпитафии"
                                    }
                                    val epNote = epitaphText.trim()
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.ENGRAVING.displayName,
                                            name = epitaphEngravingTitle,
                                            unit = "компл",
                                            unitPrice = epitaphPrice,
                                            quantity = 1.0,
                                            totalPrice = epitaphPrice,
                                            customNote = epNote
                                        )
                                    )
                                }

                                // Add selected drawings
                                selectedDrawingsList.forEach { drawing ->
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.ENGRAVING.displayName,
                                            name = "Гравировка рисунка ${drawing.code} «${drawing.name}»",
                                            unit = "шт",
                                            unitPrice = drawing.price,
                                            quantity = 1.0,
                                            totalPrice = drawing.price,
                                            customNote = "Категория: ${drawing.category}, код: ${drawing.code}"
                                        )
                                    )
                                }

                                // Add photo & frame if included
                                if (includePhoto) {
                                    if (selectedPhotoSize != null) {
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.ENGRAVING.displayName,
                                                name = "Портрет / Фотокерамика (${selectedPhotoSize?.category ?: "Фото"} ${selectedPhotoSize?.sizeName ?: ""})",
                                                unit = "шт",
                                                unitPrice = selectedPhotoSize?.price ?: 0.0,
                                                quantity = 1.0,
                                                totalPrice = selectedPhotoSize?.price ?: 0.0,
                                                customNote = "Категория: ${selectedPhotoSize?.category ?: ""}, размер: ${selectedPhotoSize?.sizeName ?: ""}"
                                            )
                                        )
                                    }
                                    if (selectedPhotoFrame != null && selectedPhotoFrame!!.price > 0) {
                                        items.add(
                                            CalculatedItemData(
                                                category = ItemCategory.ENGRAVING.displayName,
                                                name = "Рамка / врезка фото (${selectedPhotoFrame!!.name})",
                                                unit = "шт",
                                                unitPrice = selectedPhotoFrame!!.price,
                                                quantity = 1.0,
                                                totalPrice = selectedPhotoFrame!!.price,
                                                customNote = "Тип/материал: ${selectedPhotoFrame!!.materialType}"
                                            )
                                        )
                                    }
                                }

                                // Add vase if included
                                if (includeVase && selectedVase != null) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.OTHER.displayName,
                                            name = "Ваза ${selectedVase!!.name} (${selectedVase!!.materialType}, ${selectedVase!!.sizeCm})",
                                            unit = "шт",
                                            unitPrice = selectedVase!!.price,
                                            quantity = vaseQuantity.toDouble(),
                                            totalPrice = vaseTotal,
                                            customNote = "Материал: ${selectedVase!!.materialType}, размер: ${selectedVase!!.sizeCm}"
                                        )
                                    )
                                }

                                // Add Installation if included
                                if (includeInstallation && installationPrice > 0) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.OTHER.displayName,
                                            name = "Монтаж (установка) памятника",
                                            unit = "компл",
                                            unitPrice = installationPrice,
                                            quantity = 1.0,
                                            totalPrice = installationPrice,
                                            customNote = "Установка комплекта на кладбище"
                                        )
                                    )
                                }

                                // Add Demontazh if included
                                if (includeDemontazh && demontazhPrice > 0) {
                                    items.add(
                                        CalculatedItemData(
                                            category = ItemCategory.OTHER.displayName,
                                            name = "Демонтаж",
                                            unit = "усл",
                                            unitPrice = demontazhPrice,
                                            quantity = 1.0,
                                            totalPrice = demontazhPrice,
                                            customNote = demontazhNote
                                        )
                                    )
                                }

                                val sendFactoryDetailsToState = {
                                    val (lName, fName, mName) = com.example.data.FactoryOrderDocumentData.splitFio(fioText)
                                    val (bDate, dDate) = com.example.data.FactoryOrderDocumentData.extractDatesFromText(fioText)

                                    val crossDrawings = selectedDrawingsList.filter {
                                        it.category.contains("Крест", ignoreCase = true) || it.name.contains("Крест", ignoreCase = true) || it.code.startsWith("КР", ignoreCase = true)
                                    }.joinToString("; ") { "${it.code} ${it.name}" }

                                    val vigDrawings = selectedDrawingsList.filter {
                                        it.category.contains("Виньет", ignoreCase = true) || it.name.contains("Виньет", ignoreCase = true) || it.code.startsWith("В-", ignoreCase = true)
                                    }.joinToString("; ") { "${it.code} ${it.name}" }

                                    val photoDesc = if (includePhoto && selectedPhotoSize != null) {
                                        "${selectedPhotoSize?.category} ${selectedPhotoSize?.sizeName}"
                                    } else ""

                                    val fullPhotoVignette = buildString {
                                        if (photoDesc.isNotBlank()) append(photoDesc)
                                        if (vigDrawings.isNotBlank()) {
                                            if (isNotEmpty()) append(", ")
                                            append(vigDrawings)
                                        }
                                    }

                                    val frameDesc = if (includePhoto && selectedPhotoFrame != null) selectedPhotoFrame!!.name else ""

                                    val otherDrawings = selectedDrawingsList.filter {
                                        !it.category.contains("Крест", ignoreCase = true) && !it.name.contains("Крест", ignoreCase = true) && !it.code.startsWith("КР", ignoreCase = true) &&
                                        !it.category.contains("Виньет", ignoreCase = true) && !it.name.contains("Виньет", ignoreCase = true) && !it.code.startsWith("В-", ignoreCase = true)
                                    }.joinToString("; ") { "${it.code} ${it.name}" }

                                    val obeliskDesc = buildString {
                                        append(steleSizeDesc)
                                        if (includeAnnex) append(" + приставка $annexSizeDesc ($annexStoneName)")
                                        if (includeShelf) append(" + полка $shelfSizeDesc")
                                    }

                                    val plinthDesc = if (includePlinth) {
                                        if (plinthHasTwoParts) "ч.1: $plinth1SizeDesc ($plinth1StoneName), ч.2: $plinth2SizeDesc ($plinth2StoneName)"
                                        else "$plinth1SizeDesc ($plinth1StoneName)"
                                    } else ""

                                    val flowerbedDesc = if (includeFlowerbed) "$fbSizeDesc ($flowerbedStoneName)" else ""

                                    val slabDesc = if (includeSlab) {
                                        if (slabHasTwoParts) "ч.1: $slab1SizeDesc ($slab1StoneName), ч.2: $slab2SizeDesc ($slab2StoneName)"
                                        else "$slab1SizeDesc ($slab1StoneName)"
                                    } else ""

                                    val vaseDesc = if (includeVase && selectedVase != null) "${selectedVase!!.name} (${selectedVase!!.sizeCm}) × $vaseQuantity шт" else ""
                                    val dismantlingDesc = if (includeDemontazh) demontazhNote else ""

                                    onUpdateFactoryDetails?.invoke(
                                        lName,
                                        fName,
                                        mName,
                                        bDate,
                                        dDate,
                                        crossDrawings,
                                        fullPhotoVignette,
                                        frameDesc,
                                        epitaphText.trim(),
                                        "",
                                        otherDrawings,
                                        stoneNameStr,
                                        obeliskDesc,
                                        plinthDesc,
                                        flowerbedDesc,
                                        slabDesc,
                                        vaseDesc,
                                        dismantlingDesc
                                    )
                                }

                                sendFactoryDetailsToState()
                                onAddMultipleItems(items)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_monument_detailed_btn")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Добавить комплект в расчёт", maxLines = 1, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    }
                }
            }
        }

        if (isDrawingPickerOpen) {
            DrawingPickerDialog(
                drawings = actualDrawings,
                selectedDrawingIds = selectedDrawingIds,
                onSelectionChanged = { newSet -> selectedDrawingIds = newSet },
                onDismiss = { isDrawingPickerOpen = false }
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun StoneMaterialSelector(
    materials: List<StoneMaterial>,
    selected: StoneMaterial?,
    onSelect: (StoneMaterial) -> Unit,
    usdRate: Double
) {
    if (materials.isEmpty()) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "База данных материалов пуста. Добавьте породы камня в настройках расценок или восстановите начальный прайс-лист в настройках.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            materials.forEach { mat ->
                val isSelected = selected?.id == mat.id
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
                                Text(text = mat.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElementStoneSelector(
    title: String,
    useDifferentStone: Boolean,
    onUseDifferentStoneChange: (Boolean) -> Unit,
    selectedStone: StoneMaterial?,
    onSelectStone: (StoneMaterial) -> Unit,
    materials: List<StoneMaterial>,
    usdRate: Double,
    mainStoneName: String,
    mainStonePrice: Double,
    switchTestTag: String = ""
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = if (useDifferentStone) "Инд. камень" else "Как у стелы",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (useDifferentStone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (useDifferentStone) FontWeight.Bold else FontWeight.Normal
                    )
                    Switch(
                        checked = useDifferentStone,
                        onCheckedChange = onUseDifferentStoneChange,
                        modifier = Modifier
                            .scale(0.8f)
                            .then(if (switchTestTag.isNotEmpty()) Modifier.testTag(switchTestTag) else Modifier)
                    )
                }
            }

            if (!useDifferentStone) {
                Text(
                    text = "Камень: $mainStoneName (${PriceFormatter.formatWithUsd(mainStonePrice, usdRate)}/м³)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var expandedStoneDropdown by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedStoneDropdown,
                    onExpandedChange = { expandedStoneDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${selectedStone?.name ?: "Выберите камень"} (${PriceFormatter.formatWithUsd(selectedStone?.pricePerM3 ?: 0.0, usdRate)}/м³)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Порода камня", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStoneDropdown) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedStoneDropdown,
                        onDismissRequest = { expandedStoneDropdown = false }
                    ) {
                        materials.forEach { mat ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(mat.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${mat.colorName} • ${PriceFormatter.formatWithUsd(mat.pricePerM3, usdRate)}/м³",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectStone(mat)
                                    expandedStoneDropdown = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingPickerDialog(
    drawings: List<EngravingDrawingItem>,
    selectedDrawingIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("Все") }

    val categories = remember(drawings) {
        val base = listOf("Все", "Крест", "Цветок", "Иконы", "Ангелы", "Свечи")
        val custom = drawings.map { it.category }.distinct().filter { it !in base }
        base + custom
    }

    val filteredDrawings = remember(drawings, searchQuery, categoryFilter) {
        drawings.filter { item ->
            val matchesCategory = (categoryFilter == "Все") || item.category.equals(categoryFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.code.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Каталог рисунков и декора",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Выбрано: ${selectedDrawingIds.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по коду или названию...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (filteredDrawings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ничего не найдено",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredDrawings, key = { it.id }) { drawing ->
                            val isSelected = drawing.id in selectedDrawingIds
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = if (isSelected) selectedDrawingIds - drawing.id else selectedDrawingIds + drawing.id
                                        onSelectionChanged(newSet)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                val newSet = if (checked) selectedDrawingIds + drawing.id else selectedDrawingIds - drawing.id
                                                onSelectionChanged(newSet)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = drawing.code,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = drawing.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = drawing.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = PriceFormatter.formatRub(drawing.price),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

