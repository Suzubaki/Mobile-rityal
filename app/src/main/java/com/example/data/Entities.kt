package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_PUSH = "PENDING_PUSH"
    const val PENDING_DELETE = "PENDING_DELETE"
    const val CONFLICT = "CONFLICT"
}

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
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)

@Entity(tableName = "saved_orders")
data class SavedOrder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val orderNumber: String,
    val clientName: String,
    val clientPhone: String,
    val deceasedName: String = "",
    val cemeteryName: String = "",
    val plotNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // DRAFT, IN_PROGRESS, READY, PAID, CANCELLED
    val subtotalAmount: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val prepaymentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val itemsJson: String, // serialized List<CalculatedItemData>
    val notes: String = "",
    val syncStatus: String = SyncStatus.PENDING_PUSH,
    val isDeleted: Boolean = false
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
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
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
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
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
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)

@Entity(tableName = "engraving_fonts")
data class EngravingFontItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val styleKey: String = "SERIF", // SERIF, SANS_SERIF, SANS_SERIF_BOLD, OLD_SLAVIC, CURSIVE, ANTIQUE, MONOSPACE
    val price: Double = 0.0, // Free (0 BYN) for now by default
    val sampleText: String = "Иванов Иван Иванович\n1950 — 2024",
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)

@Entity(tableName = "engraving_drawings")
data class EngravingDrawingItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "Крест", "Цветок", "Иконы", "Ангелы", "Свечи"
    val code: String,     // "К-1", "Ц-1", "И-1", "А-1", "С-1", etc.
    val name: String,     // e.g. "Свеча с розой", "Православный крест"
    val price: Double = 35.0, // BYN (editable in prices)
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)

@Entity(tableName = "photo_size_items")
data class PhotoSizeItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String = "Фотокерамика", // "Фотокерамика", "Металлокерамика", "Фото на стекле", "Гравировка портрета"
    val sizeName: String, // "13×18 см", "18×24 см", "20×30 см", "24×30 см", "30×40 см", "40×60 см"
    val price: Double = 150.0, // BYN
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)

@Entity(tableName = "photo_frame_items")
data class PhotoFrameItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String, // "Бронза Caggiati", "Алюминиевая рамка", "Гранитная рамка", "Врезка / Ниша", "Без рамки"
    val materialType: String = "Бронза", // "Бронза", "Алюминий", "Гранит", "Врезка", "Без рамки"
    val price: Double = 80.0, // BYN
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)

@Entity(tableName = "vase_items")
data class VaseItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String, // e.g. "Ваза гранитная 30 см (Габбро)"
    val materialType: String = "Гранит", // "Гранит", "Кованый металл", "Полимер"
    val sizeCm: String = "30 см",
    val price: Double = 120.0, // BYN
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)





