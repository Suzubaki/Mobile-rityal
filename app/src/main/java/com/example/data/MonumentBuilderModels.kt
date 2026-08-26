package com.example.data

data class StoneMaterial(
    val id: String,
    val name: String,
    val colorName: String,
    val pricePerM3: Double,
    val origin: String,
    val description: String,
    val suitableForDirectEngraving: Boolean = true,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true
)

fun StoneMaterialItem.toModel(): StoneMaterial = StoneMaterial(
    id = id,
    name = name,
    colorName = colorName,
    pricePerM3 = pricePerM3,
    origin = origin,
    description = description,
    suitableForDirectEngraving = suitableForDirectEngraving,
    isCustom = isCustom,
    isEnabled = isEnabled
)

fun StoneMaterial.toEntity(sortOrder: Int = 0): StoneMaterialItem = StoneMaterialItem(
    id = id,
    name = name,
    colorName = colorName,
    pricePerM3 = pricePerM3,
    origin = origin,
    description = description,
    suitableForDirectEngraving = suitableForDirectEngraving,
    isCustom = isCustom,
    isEnabled = isEnabled,
    sortOrder = sortOrder
)

data class MonumentSizePreset(
    val id: String,
    val name: String,
    val heightCm: Int,
    val widthCm: Int,
    val thicknessCm: Int,
    val steleBasePrice: Double,
    val plinthBasePrice: Double,
    val flowerbedBasePrice: Double,
    val plinthDimensions: String,
    val flowerbedDimensions: String,
    val isFamily: Boolean = false,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true
)

fun MonumentSizePresetItem.toModel(): MonumentSizePreset = MonumentSizePreset(
    id = id,
    name = name,
    heightCm = heightCm,
    widthCm = widthCm,
    thicknessCm = thicknessCm,
    steleBasePrice = steleBasePrice,
    plinthBasePrice = plinthBasePrice,
    flowerbedBasePrice = flowerbedBasePrice,
    plinthDimensions = plinthDimensions,
    flowerbedDimensions = flowerbedDimensions,
    isFamily = isFamily,
    isCustom = isCustom,
    isEnabled = isEnabled
)

fun MonumentSizePreset.toEntity(sortOrder: Int = 0): MonumentSizePresetItem = MonumentSizePresetItem(
    id = id,
    name = name,
    heightCm = heightCm,
    widthCm = widthCm,
    thicknessCm = thicknessCm,
    steleBasePrice = steleBasePrice,
    plinthBasePrice = plinthBasePrice,
    flowerbedBasePrice = flowerbedBasePrice,
    plinthDimensions = plinthDimensions,
    flowerbedDimensions = flowerbedDimensions,
    isFamily = isFamily,
    isCustom = isCustom,
    isEnabled = isEnabled,
    sortOrder = sortOrder
)

enum class PolishingType(val displayName: String, val multiplier: Double, val description: String) {
    FACE_ONLY("Лицевая (1 сторона)", 0.90, "Полировка только лицевой части, бока и зад пиленые"),
    FACE_AND_SIDES("Лицевая + торцы (3 стороны)", 1.00, "Полировка лицевой стороны и боковых граней"),
    FULL_5_SIDES("Круговая полировка (5 сторон)", 1.15, "Зеркальная полировка всех сторон, торцов и верха")
}

enum class SteleShape(val key: String, val displayName: String, val baseCarvingCost: Double, val description: String) {
    RECTANGULAR("carving_rectangular", "Прямоугольный (базовый)", 0.0, "Классическая строгая прямоугольная стела"),
    WAVE_SHOULDERS("carving_wave", "Фигурная 'Волна / Плечики / Арка'", 150.0, "Плавные изгибы, фасонные скосы по верхней грани"),
    WITH_CROSS("carving_cross", "С резным крестом", 290.0, "Глубокая барельефная резьба православного/католического креста"),
    WITH_FLOWERS("carving_flowers", "С резными розами / цветами", 340.0, "Объемная скульптурная резьба цветов на стеле"),
    WITH_ANGEL_TREE("carving_angel_tree", "С резным ангелом / березкой", 460.0, "Художественная барельефная резьба ангела или дерева"),
    ROCK_CRUST("carving_rock", "Скала / Корка (колотый скол)", 190.0, "Необработанный природный колотый край гранита"),
    EXCLUSIVE_CUSTOM("carving_exclusive", "Авторская эксклюзивная резка", 550.0, "Сложный индивидуальный резной проект любой формы")
}

enum class ChamferType(val key: String, val displayName: String, val cost: Double) {
    TECH_2MM("chamfer_tech", "Техническая фаска 2 мм", 0.0),
    POLISHED_10MM("chamfer_polished_10mm", "Широкая полированная фаска 10 мм", 50.0),
    FIGURED_ROUND("chamfer_round", "Фигурная радиусная фаска (валик)", 85.0)
}

enum class FlowerbedType(val key: String, val displayName: String, val costOffset: Double, val description: String) {
    OPEN_3_BARS("flowerbed_open", "Открытый цветник (3 балки)", 0.0, "Классический открытый цветник для высадки цветов"),
    HALF_COVERED("flowerbed_half_slab", "Полузакрытый (с малой плитой)", 120.0, "Цветник с гранитной надгробной плитой на 1/2 длины"),
    FULL_SOLID_SLAB("flowerbed_solid_slab", "Сплошная надгробная плита (закрытый)", 260.0, "Полностью закрытая полированная гранитная плита"),
    DOUBLE_FAMILY("flowerbed_double", "Двойной семейный цветник", 100.0, "Широкий цветник на два захоронения"),
    NONE("flowerbed_none", "Без цветника", -140.0, "Без элементов цветника")
}

enum class PlinthType(val key: String, val displayName: String, val costOffset: Double) {
    STANDARD("plinth_std", "Стандартная тумба (под размер)", 0.0),
    HIGH_PLINTH("plinth_high", "Увеличенная высокая тумба", 85.0),
    NONE("plinth_none", "Без подставки (только стела)", -140.0)
}

enum class PortraitType(val key: String, val displayName: String, val cost: Double, val description: String) {
    NONE("portrait_none", "Без портрета", 0.0, ""),
    LASER_A4("portrait_laser_a4", "Лазерная/станочная гравировка (А4)", 150.0, "Высокоточная гравировка с ретушью фото"),
    MACHINE_PLUS_HAND("portrait_hand_retouch", "Станочная + ручная доработка художником", 260.0, "Глубокая прорисовка полутонов и взгляда художником"),
    FULL_HEIGHT("portrait_full_height", "Портрет в полный рост", 420.0, "Гравировка человека в полный рост"),
    CERAMIC_13x18("portrait_ceramic_13x18", "Фотокерамика 13×18 см (с врезкой в нишу)", 120.0, "Запекание на керамической пластине, врезка в камень"),
    TRIPLEX_GLASS("portrait_triplex", "Фото на осветленном стекле 20×30 см", 290.0, "Премиальный триплекс с полированными фасками")
}

enum class InstallationType(val key: String, val displayName: String, val cost: Double, val description: String) {
    NONE("install_none", "Без установки (самовывоз)", 0.0, "Самовывоз со склада мастерской"),
    ON_BEAMS("install_beams", "Монтаж на ж/б балки перекрытия (в грунт)", 260.0, "Установка на балки перекрытия, заливка раствора по уровню"),
    ON_CONCRETE_PAD("install_concrete_pad", "Монтаж на сплошной армированный фундамент", 470.0, "Заливка монолитной плиты с армированием"),
    ON_EXISTING_BASE("install_existing_base", "Монтаж на готовый цоколь / плитку", 210.0, "Монтаж стелы и цветника на существующий фундамент")
}

object MonumentCatalogData {
    val materials: List<StoneMaterial>
        get() = DefaultCatalog.getDefaultStoneMaterials().map { it.toModel() }

    val sizePresets: List<MonumentSizePreset>
        get() = DefaultCatalog.getDefaultSizePresets().map { it.toModel() }

    val servicePrices: Map<String, Double>
        get() = DefaultCatalog.getDefaultConstructorServicePrices().associate { it.key to it.price }
}
