package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemCategory(val displayName: String, val iconName: String) {
    MONUMENTS("Памятники", "monument"),
    ENGRAVING("Гравировка", "palette"),
    FENCES_GROUND("Ограды и плитка", "fence"),
    BURIAL_SERVICES("Захоронение", "funeral"),
    OTHER("Прочее", "more")
}

@Entity(tableName = "price_items")
data class PriceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // from ItemCategory
    val subcategory: String = "",
    val name: String,
    val unit: String = "шт", // шт, компл, м.п., м², знак, рейс, услуга, час
    val defaultPrice: Double,
    val currentPrice: Double,
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "saved_orders")
data class SavedOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val clientName: String,
    val clientPhone: String,
    val deceasedName: String = "",
    val cemeteryName: String = "",
    val plotNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // DRAFT, IN_PROGRESS, READY, PAID, CANCELLED
    val subtotalAmount: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val prepaymentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val itemsJson: String, // serialized List<CalculatedItemData>
    val notes: String = ""
)

@Entity(tableName = "stone_materials")
data class StoneMaterialItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorName: String,
    val pricePerM3: Double, // Price per 1 cubic meter in BYN
    val origin: String,
    val description: String = "",
    val suitableForDirectEngraving: Boolean = true,
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "monument_size_presets")
data class MonumentSizePresetItem(
    @PrimaryKey
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
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "constructor_service_prices")
data class ConstructorServicePriceItem(
    @PrimaryKey
    val key: String,
    val groupName: String, // "Резка и форма", "Фаска и обработка", "Подставки и цветники", "Портреты", "Гравировка и символика", "Монтаж"
    val title: String,
    val unit: String = "BYN",
    val price: Double,
    val description: String = "",
    val sortOrder: Int = 0
)

