package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. Recreate saved_orders with String primary key
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `saved_orders_new` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `orderNumber` TEXT NOT NULL,
                `clientName` TEXT NOT NULL,
                `clientPhone` TEXT NOT NULL,
                `deceasedName` TEXT NOT NULL DEFAULT '',
                `cemeteryName` TEXT NOT NULL DEFAULT '',
                `plotNumber` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'DRAFT',
                `subtotalAmount` REAL NOT NULL,
                `discountPercent` REAL NOT NULL DEFAULT 0.0,
                `discountAmount` REAL NOT NULL DEFAULT 0.0,
                `totalAmount` REAL NOT NULL,
                `prepaymentAmount` REAL NOT NULL DEFAULT 0.0,
                `remainingAmount` REAL NOT NULL DEFAULT 0.0,
                `itemsJson` TEXT NOT NULL,
                `notes` TEXT NOT NULL DEFAULT '',
                `syncStatus` TEXT NOT NULL DEFAULT 'PENDING_PUSH',
                `isDeleted` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `saved_orders_new` (
                id, orderNumber, clientName, clientPhone, deceasedName, cemeteryName, plotNumber,
                createdAt, updatedAt, status, subtotalAmount, discountPercent, discountAmount,
                totalAmount, prepaymentAmount, remainingAmount, itemsJson, notes, syncStatus, isDeleted
            )
            SELECT
                CAST(id AS TEXT), orderNumber, clientName, clientPhone, deceasedName, cemeteryName, plotNumber,
                createdAt, updatedAt, status, subtotalAmount, discountPercent, discountAmount,
                totalAmount, prepaymentAmount, remainingAmount, itemsJson, notes, 'SYNCED', 0
            FROM `saved_orders`
        """.trimIndent())

        db.execSQL("DROP TABLE `saved_orders` ")
        db.execSQL("ALTER TABLE `saved_orders_new` RENAME TO `saved_orders` ")

        // 2. Add columns to price_items
        db.execSQL("ALTER TABLE `price_items` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE `price_items` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE `price_items` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

        // 3. Add columns to stone_materials
        db.execSQL("ALTER TABLE `stone_materials` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE `stone_materials` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE `stone_materials` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

        // 4. Add columns to monument_size_presets
        db.execSQL("ALTER TABLE `monument_size_presets` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE `monument_size_presets` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE `monument_size_presets` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")

        // 5. Add columns to constructor_service_prices
        db.execSQL("ALTER TABLE `constructor_service_prices` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT $now")
        db.execSQL("ALTER TABLE `constructor_service_prices` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE `constructor_service_prices` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        PriceItem::class,
        SavedOrder::class,
        StoneMaterialItem::class,
        MonumentSizePresetItem::class,
        ConstructorServicePriceItem::class,
        EngravingFontItem::class,
        EngravingDrawingItem::class,
        PhotoSizeItem::class,
        PhotoFrameItem::class,
        VaseItem::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao
    abstract fun orderDao(): OrderDao
    abstract fun stoneMaterialDao(): StoneMaterialDao
    abstract fun monumentSizePresetDao(): MonumentSizePresetDao
    abstract fun constructorServicePriceDao(): ConstructorServicePriceDao
    abstract fun engravingFontDao(): EngravingFontDao
    abstract fun engravingDrawingDao(): EngravingDrawingDao
    abstract fun photoSizeDao(): PhotoSizeDao
    abstract fun photoFrameDao(): PhotoFrameDao
    abstract fun vaseDao(): VaseDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ritual_calc_database.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class RitualRepository(
    private val priceDao: PriceDao,
    private val orderDao: OrderDao,
    private val stoneMaterialDao: StoneMaterialDao,
    private val monumentSizePresetDao: MonumentSizePresetDao,
    private val constructorServicePriceDao: ConstructorServicePriceDao,
    private val engravingFontDao: EngravingFontDao,
    private val engravingDrawingDao: EngravingDrawingDao,
    private val photoSizeDao: PhotoSizeDao,
    private val photoFrameDao: PhotoFrameDao,
    private val vaseDao: VaseDao
) {
    val allPriceItems: Flow<List<PriceItem>> = priceDao.getAllPriceItems()
    val enabledPriceItems: Flow<List<PriceItem>> = priceDao.getEnabledPriceItems()
    val allOrders: Flow<List<SavedOrder>> = orderDao.getAllOrders()

    val allStoneMaterials: Flow<List<StoneMaterialItem>> = stoneMaterialDao.getAllMaterials()
    val enabledStoneMaterials: Flow<List<StoneMaterialItem>> = stoneMaterialDao.getEnabledMaterials()

    val allSizePresets: Flow<List<MonumentSizePresetItem>> = monumentSizePresetDao.getAllPresets()
    val enabledSizePresets: Flow<List<MonumentSizePresetItem>> = monumentSizePresetDao.getEnabledPresets()

    val allConstructorServicePrices: Flow<List<ConstructorServicePriceItem>> = constructorServicePriceDao.getAllServicePrices()

    val allEngravingFonts: Flow<List<EngravingFontItem>> = engravingFontDao.getAllFonts()
    val enabledEngravingFonts: Flow<List<EngravingFontItem>> = engravingFontDao.getEnabledFonts()

    val allEngravingDrawings: Flow<List<EngravingDrawingItem>> = engravingDrawingDao.getAllDrawings()
    val enabledEngravingDrawings: Flow<List<EngravingDrawingItem>> = engravingDrawingDao.getEnabledDrawings()

    val allPhotoSizes: Flow<List<PhotoSizeItem>> = photoSizeDao.getAllPhotoSizes()
    val enabledPhotoSizes: Flow<List<PhotoSizeItem>> = photoSizeDao.getEnabledPhotoSizes()

    val allPhotoFrames: Flow<List<PhotoFrameItem>> = photoFrameDao.getAllPhotoFrames()
    val enabledPhotoFrames: Flow<List<PhotoFrameItem>> = photoFrameDao.getEnabledPhotoFrames()

    val allVases: Flow<List<VaseItem>> = vaseDao.getAllVases()
    val enabledVases: Flow<List<VaseItem>> = vaseDao.getEnabledVases()

    suspend fun ensureDefaultDataLoaded() {
        priceDao.purgeCleaningServiceItems()
        deduplicateLocalPriceItems()
        try {
            val existingPrices = constructorServicePriceDao.getAllServicePricesSync()
            val existingKeys = existingPrices.map { it.key }.toSet()
            val defaults = DefaultCatalog.getDefaultConstructorServicePrices()
            val missing = defaults.filter { it.key !in existingKeys }
            if (missing.isNotEmpty()) {
                constructorServicePriceDao.insertServicePrices(missing.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            }
        } catch (_: Exception) {}

        try {
            val existingFonts = engravingFontDao.getAllFontsSync()
            if (existingFonts.isEmpty()) {
                val defaultFonts = DefaultCatalog.getDefaultEngravingFonts()
                engravingFontDao.insertFonts(defaultFonts.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            }
        } catch (_: Exception) {}

        try {
            val existingDrawings = engravingDrawingDao.getAllDrawingsSync()
            if (existingDrawings.isEmpty()) {
                val defaultDrawings = DefaultCatalog.getDefaultEngravingDrawings()
                engravingDrawingDao.insertDrawings(defaultDrawings.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            }
        } catch (_: Exception) {}

        try {
            val existingPhotoSizes = photoSizeDao.getAllPhotoSizesSync()
            if (existingPhotoSizes.isEmpty()) {
                val defaultPhotoSizes = DefaultCatalog.getDefaultPhotoSizes()
                photoSizeDao.insertPhotoSizes(defaultPhotoSizes.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            }
        } catch (_: Exception) {}

        try {
            val existingPhotoFrames = photoFrameDao.getAllPhotoFramesSync()
            if (existingPhotoFrames.isEmpty()) {
                val defaultPhotoFrames = DefaultCatalog.getDefaultPhotoFrames()
                photoFrameDao.insertPhotoFrames(defaultPhotoFrames.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            }
        } catch (_: Exception) {}

        try {
            val existingVases = vaseDao.getAllVasesSync()
            if (existingVases.isEmpty()) {
                val defaultVases = DefaultCatalog.getDefaultVases()
                vaseDao.insertVases(defaultVases.map { it.copy(syncStatus = SyncStatus.SYNCED) })
            }
        } catch (_: Exception) {}
    }



    suspend fun deduplicateLocalPriceItems() {
        try {
            val allPrices = priceDao.getAllPriceItemsSync()
            val groupedByName = allPrices.groupBy { it.name.trim().lowercase() }
            for ((_, items) in groupedByName) {
                if (items.size > 1) {
                    val primaryItem = items.find { it.id in 1L..44L } ?: items.minByOrNull { it.id } ?: items.first()
                    for (item in items) {
                        if (item.id != primaryItem.id) {
                            priceDao.hardDeleteItemById(item.id)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // --- Direct Local Cache Ops (Used by Realtime Firestore Listeners & Cloud Engine) ---
    suspend fun saveOrderToLocalCacheOnly(order: SavedOrder) = orderDao.insertOrder(order)
    suspend fun savePriceItemToLocalCacheOnly(item: PriceItem): Long {
        if (item.id <= 0) return 0L
        try {
            val trimmedName = item.name.trim().lowercase()
            val allCurrent = priceDao.getAllPriceItemsSync()
            for (existing in allCurrent) {
                if (existing.id != item.id && existing.name.trim().lowercase() == trimmedName) {
                    priceDao.hardDeleteItemById(existing.id)
                }
            }
        } catch (_: Exception) {}
        return priceDao.insertItem(item)
    }
    suspend fun saveStoneMaterialToLocalCacheOnly(material: StoneMaterialItem) = stoneMaterialDao.insertMaterial(material)
    suspend fun saveSizePresetToLocalCacheOnly(preset: MonumentSizePresetItem) = monumentSizePresetDao.insertPreset(preset)
    suspend fun saveServicePriceToLocalCacheOnly(item: ConstructorServicePriceItem) = constructorServicePriceDao.insertServicePrice(item)
    suspend fun saveFontToLocalCacheOnly(font: EngravingFontItem) = engravingFontDao.insertFont(font)
    suspend fun saveDrawingToLocalCacheOnly(drawing: EngravingDrawingItem) = engravingDrawingDao.insertDrawing(drawing)
    suspend fun savePhotoSizeToLocalCacheOnly(size: PhotoSizeItem) = photoSizeDao.insertPhotoSize(size)
    suspend fun savePhotoFrameToLocalCacheOnly(frame: PhotoFrameItem) = photoFrameDao.insertPhotoFrame(frame)
    suspend fun saveVaseToLocalCacheOnly(vase: VaseItem) = vaseDao.insertVase(vase)

    // --- Bulk Sync & Pending Operations ---
    suspend fun getAllPriceItemsSync(): List<PriceItem> = priceDao.getAllPriceItemsSync()
    suspend fun getAllOrdersSync(): List<SavedOrder> = orderDao.getAllOrdersSync()
    suspend fun getAllMaterialsSync(): List<StoneMaterialItem> = stoneMaterialDao.getAllMaterialsSync()
    suspend fun getAllPresetsSync(): List<MonumentSizePresetItem> = monumentSizePresetDao.getAllPresetsSync()
    suspend fun getAllServicePricesSync(): List<ConstructorServicePriceItem> = constructorServicePriceDao.getAllServicePricesSync()
    suspend fun getAllFontsSync(): List<EngravingFontItem> = engravingFontDao.getAllFontsSync()
    suspend fun getAllDrawingsSync(): List<EngravingDrawingItem> = engravingDrawingDao.getAllDrawingsSync()
    suspend fun getAllPhotoSizesSync(): List<PhotoSizeItem> = photoSizeDao.getAllPhotoSizesSync()
    suspend fun getAllPhotoFramesSync(): List<PhotoFrameItem> = photoFrameDao.getAllPhotoFramesSync()
    suspend fun getAllVasesSync(): List<VaseItem> = vaseDao.getAllVasesSync()

    suspend fun getPendingPushOrders(): List<SavedOrder> = orderDao.getPendingPushOrders()
    suspend fun getPendingDeleteOrders(): List<SavedOrder> = orderDao.getPendingDeleteOrders()

    suspend fun getPendingPushPrices(): List<PriceItem> = priceDao.getPendingPushItems()
    suspend fun getPendingDeletePrices(): List<PriceItem> = priceDao.getPendingDeleteItems()

    suspend fun getPendingPushMaterials(): List<StoneMaterialItem> = stoneMaterialDao.getPendingPushMaterials()
    suspend fun getPendingDeleteMaterials(): List<StoneMaterialItem> = stoneMaterialDao.getPendingDeleteMaterials()

    suspend fun getPendingPushPresets(): List<MonumentSizePresetItem> = monumentSizePresetDao.getPendingPushPresets()
    suspend fun getPendingDeletePresets(): List<MonumentSizePresetItem> = monumentSizePresetDao.getPendingDeletePresets()

    suspend fun getPendingPushServicePrices(): List<ConstructorServicePriceItem> = constructorServicePriceDao.getPendingPushServicePrices()
    suspend fun getPendingDeleteServicePrices(): List<ConstructorServicePriceItem> = constructorServicePriceDao.getPendingDeleteServicePrices()

    suspend fun getPendingPushFonts(): List<EngravingFontItem> = engravingFontDao.getPendingPushFonts()
    suspend fun getPendingDeleteFonts(): List<EngravingFontItem> = engravingFontDao.getPendingDeleteFonts()

    suspend fun getPendingPushDrawings(): List<EngravingDrawingItem> = engravingDrawingDao.getPendingPushDrawings()
    suspend fun getPendingDeleteDrawings(): List<EngravingDrawingItem> = engravingDrawingDao.getPendingDeleteDrawings()

    suspend fun getPendingPushPhotoSizes(): List<PhotoSizeItem> = photoSizeDao.getPendingPushPhotoSizes()
    suspend fun getPendingDeletePhotoSizes(): List<PhotoSizeItem> = photoSizeDao.getPendingDeletePhotoSizes()

    suspend fun getPendingPushPhotoFrames(): List<PhotoFrameItem> = photoFrameDao.getPendingPushPhotoFrames()
    suspend fun getPendingDeletePhotoFrames(): List<PhotoFrameItem> = photoFrameDao.getPendingDeletePhotoFrames()

    suspend fun getPendingPushVases(): List<VaseItem> = vaseDao.getPendingPushVases()
    suspend fun getPendingDeleteVases(): List<VaseItem> = vaseDao.getPendingDeleteVases()

    suspend fun getPriceItemById(id: Long): PriceItem? = priceDao.getItemById(id)
    suspend fun getPriceItemByIdIncludingDeleted(id: Long): PriceItem? = priceDao.getItemByIdIncludingDeleted(id)

    suspend fun getMaterialById(id: String): StoneMaterialItem? = stoneMaterialDao.getMaterialById(id)
    suspend fun getMaterialByIdIncludingDeleted(id: String): StoneMaterialItem? = stoneMaterialDao.getMaterialByIdIncludingDeleted(id)

    suspend fun getPresetById(id: String): MonumentSizePresetItem? = monumentSizePresetDao.getPresetById(id)
    suspend fun getPresetByIdIncludingDeleted(id: String): MonumentSizePresetItem? = monumentSizePresetDao.getPresetByIdIncludingDeleted(id)

    suspend fun getServicePriceByKey(key: String): ConstructorServicePriceItem? = constructorServicePriceDao.getServicePriceByKey(key)
    suspend fun getServicePriceByKeyIncludingDeleted(key: String): ConstructorServicePriceItem? = constructorServicePriceDao.getServicePriceByKeyIncludingDeleted(key)

    suspend fun getFontById(id: String): EngravingFontItem? = engravingFontDao.getFontById(id)
    suspend fun getFontByIdIncludingDeleted(id: String): EngravingFontItem? = engravingFontDao.getFontByIdIncludingDeleted(id)

    suspend fun getDrawingById(id: String): EngravingDrawingItem? = engravingDrawingDao.getDrawingById(id)
    suspend fun getDrawingByIdIncludingDeleted(id: String): EngravingDrawingItem? = engravingDrawingDao.getDrawingByIdIncludingDeleted(id)

    suspend fun getPhotoSizeById(id: String): PhotoSizeItem? = photoSizeDao.getPhotoSizeById(id)
    suspend fun getPhotoSizeByIdIncludingDeleted(id: String): PhotoSizeItem? = photoSizeDao.getPhotoSizeByIdIncludingDeleted(id)

    suspend fun getPhotoFrameById(id: String): PhotoFrameItem? = photoFrameDao.getPhotoFrameById(id)
    suspend fun getPhotoFrameByIdIncludingDeleted(id: String): PhotoFrameItem? = photoFrameDao.getPhotoFrameByIdIncludingDeleted(id)

    suspend fun getVaseById(id: String): VaseItem? = vaseDao.getVaseById(id)
    suspend fun getVaseByIdIncludingDeleted(id: String): VaseItem? = vaseDao.getVaseByIdIncludingDeleted(id)

    // --- Price Items ---
    suspend fun insertPriceItem(item: PriceItem): Long = priceDao.insertItem(item)
    suspend fun updatePriceItem(item: PriceItem) = priceDao.updateItem(item)
    suspend fun deletePriceItem(item: PriceItem) = priceDao.softDeleteItemById(item.id)
    suspend fun deletePriceItem(itemId: Long) = priceDao.softDeleteItemById(itemId)
    suspend fun softDeletePriceItem(itemId: Long) = priceDao.softDeleteItemById(itemId)
    suspend fun hardDeletePriceItem(itemId: Long) = priceDao.hardDeleteItemById(itemId)
    suspend fun resetToDefaultPrices() {
        priceDao.clearDefaultItems()
        priceDao.insertItems(DefaultCatalog.getDefaultItems().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Stone Materials ---
    suspend fun insertStoneMaterial(material: StoneMaterialItem) = stoneMaterialDao.insertMaterial(material)
    suspend fun updateStoneMaterial(material: StoneMaterialItem) = stoneMaterialDao.updateMaterial(material)
    suspend fun deleteStoneMaterial(material: StoneMaterialItem) = stoneMaterialDao.softDeleteMaterialById(material.id)
    suspend fun deleteStoneMaterial(id: String) = stoneMaterialDao.softDeleteMaterialById(id)
    suspend fun softDeleteStoneMaterial(id: String) = stoneMaterialDao.softDeleteMaterialById(id)
    suspend fun hardDeleteStoneMaterial(id: String) = stoneMaterialDao.hardDeleteMaterialById(id)
    suspend fun resetStoneMaterials() {
        stoneMaterialDao.clearAllMaterials()
        stoneMaterialDao.insertMaterials(DefaultCatalog.getDefaultStoneMaterials().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Size Presets ---
    suspend fun insertSizePreset(preset: MonumentSizePresetItem) = monumentSizePresetDao.insertPreset(preset)
    suspend fun updateSizePreset(preset: MonumentSizePresetItem) = monumentSizePresetDao.updatePreset(preset)
    suspend fun deleteSizePreset(preset: MonumentSizePresetItem) = monumentSizePresetDao.softDeletePresetById(preset.id)
    suspend fun deleteSizePreset(id: String) = monumentSizePresetDao.softDeletePresetById(id)
    suspend fun softDeleteSizePreset(id: String) = monumentSizePresetDao.softDeletePresetById(id)
    suspend fun hardDeleteSizePreset(id: String) = monumentSizePresetDao.hardDeletePresetById(id)
    suspend fun resetSizePresets() {
        monumentSizePresetDao.clearAllPresets()
        monumentSizePresetDao.insertPresets(DefaultCatalog.getDefaultSizePresets().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Constructor Service Rates ---
    suspend fun insertServicePrice(item: ConstructorServicePriceItem) = constructorServicePriceDao.insertServicePrice(item)
    suspend fun updateServicePrice(item: ConstructorServicePriceItem) = constructorServicePriceDao.updateServicePrice(item)
    suspend fun deleteServicePrice(item: ConstructorServicePriceItem) = constructorServicePriceDao.softDeleteServicePriceByKey(item.key)
    suspend fun deleteServicePriceByKey(key: String) = constructorServicePriceDao.softDeleteServicePriceByKey(key)
    suspend fun softDeleteServicePriceByKey(key: String) = constructorServicePriceDao.softDeleteServicePriceByKey(key)
    suspend fun hardDeleteServicePriceByKey(key: String) = constructorServicePriceDao.hardDeleteServicePriceByKey(key)
    suspend fun resetConstructorServicePrices() {
        constructorServicePriceDao.clearAllServicePrices()
        constructorServicePriceDao.insertServicePrices(DefaultCatalog.getDefaultConstructorServicePrices().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Engraving Fonts ---
    suspend fun insertFont(font: EngravingFontItem) = engravingFontDao.insertFont(font)
    suspend fun updateFont(font: EngravingFontItem) = engravingFontDao.updateFont(font)
    suspend fun deleteFont(font: EngravingFontItem) = engravingFontDao.softDeleteFontById(font.id)
    suspend fun deleteFont(id: String) = engravingFontDao.softDeleteFontById(id)
    suspend fun softDeleteFont(id: String) = engravingFontDao.softDeleteFontById(id)
    suspend fun hardDeleteFont(id: String) = engravingFontDao.hardDeleteFontById(id)
    suspend fun resetEngravingFonts() {
        engravingFontDao.clearAllFonts()
        engravingFontDao.insertFonts(DefaultCatalog.getDefaultEngravingFonts().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Engraving Drawings ---
    suspend fun insertDrawing(drawing: EngravingDrawingItem) = engravingDrawingDao.insertDrawing(drawing)
    suspend fun updateDrawing(drawing: EngravingDrawingItem) = engravingDrawingDao.updateDrawing(drawing)
    suspend fun deleteDrawing(drawing: EngravingDrawingItem) = engravingDrawingDao.softDeleteDrawingById(drawing.id)
    suspend fun deleteDrawing(id: String) = engravingDrawingDao.softDeleteDrawingById(id)
    suspend fun softDeleteDrawing(id: String) = engravingDrawingDao.softDeleteDrawingById(id)
    suspend fun hardDeleteDrawing(id: String) = engravingDrawingDao.hardDeleteDrawingById(id)
    suspend fun resetEngravingDrawings() {
        engravingDrawingDao.clearAllDrawings()
        engravingDrawingDao.insertDrawings(DefaultCatalog.getDefaultEngravingDrawings().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Photo Sizes ---
    suspend fun insertPhotoSize(item: PhotoSizeItem) = photoSizeDao.insertPhotoSize(item)
    suspend fun updatePhotoSize(item: PhotoSizeItem) = photoSizeDao.updatePhotoSize(item)
    suspend fun deletePhotoSize(item: PhotoSizeItem) = photoSizeDao.softDeletePhotoSizeById(item.id)
    suspend fun deletePhotoSize(id: String) = photoSizeDao.softDeletePhotoSizeById(id)
    suspend fun softDeletePhotoSize(id: String) = photoSizeDao.softDeletePhotoSizeById(id)
    suspend fun hardDeletePhotoSize(id: String) = photoSizeDao.hardDeletePhotoSizeById(id)
    suspend fun resetPhotoSizes() {
        photoSizeDao.clearAllPhotoSizes()
        photoSizeDao.insertPhotoSizes(DefaultCatalog.getDefaultPhotoSizes().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Photo Frames ---
    suspend fun insertPhotoFrame(item: PhotoFrameItem) = photoFrameDao.insertPhotoFrame(item)
    suspend fun updatePhotoFrame(item: PhotoFrameItem) = photoFrameDao.updatePhotoFrame(item)
    suspend fun deletePhotoFrame(item: PhotoFrameItem) = photoFrameDao.softDeletePhotoFrameById(item.id)
    suspend fun deletePhotoFrame(id: String) = photoFrameDao.softDeletePhotoFrameById(id)
    suspend fun softDeletePhotoFrame(id: String) = photoFrameDao.softDeletePhotoFrameById(id)
    suspend fun hardDeletePhotoFrame(id: String) = photoFrameDao.hardDeletePhotoFrameById(id)
    suspend fun resetPhotoFrames() {
        photoFrameDao.clearAllPhotoFrames()
        photoFrameDao.insertPhotoFrames(DefaultCatalog.getDefaultPhotoFrames().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Vases ---
    suspend fun insertVase(item: VaseItem) = vaseDao.insertVase(item)
    suspend fun updateVase(item: VaseItem) = vaseDao.updateVase(item)
    suspend fun deleteVase(item: VaseItem) = vaseDao.softDeleteVaseById(item.id)
    suspend fun deleteVase(id: String) = vaseDao.softDeleteVaseById(id)
    suspend fun softDeleteVase(id: String) = vaseDao.softDeleteVaseById(id)
    suspend fun hardDeleteVase(id: String) = vaseDao.hardDeleteVaseById(id)
    suspend fun resetVases() {
        vaseDao.clearAllVases()
        vaseDao.insertVases(DefaultCatalog.getDefaultVases().map { it.copy(syncStatus = SyncStatus.SYNCED) })
    }

    // --- Orders ---

    suspend fun getOrderById(id: String): SavedOrder? = orderDao.getOrderById(id)
    suspend fun getOrderByIdIncludingDeleted(id: String): SavedOrder? = orderDao.getOrderByIdIncludingDeleted(id)
    suspend fun saveOrder(order: SavedOrder) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: SavedOrder) = orderDao.updateOrder(order)
    suspend fun deleteOrder(order: SavedOrder) = orderDao.softDeleteOrderById(order.id)
    suspend fun deleteOrder(orderId: String) = orderDao.softDeleteOrderById(orderId)
    suspend fun softDeleteOrder(orderId: String) = orderDao.softDeleteOrderById(orderId)
    suspend fun hardDeleteOrder(orderId: String) = orderDao.hardDeleteOrderById(orderId)

    suspend fun clearAllRoomData() {
        orderDao.clearAllOrders()
        priceDao.clearAllPriceItems()
        stoneMaterialDao.clearAllMaterials()
        monumentSizePresetDao.clearAllPresets()
        constructorServicePriceDao.clearAllServicePrices()
        engravingFontDao.clearAllFonts()
        engravingDrawingDao.clearAllDrawings()
        photoSizeDao.clearAllPhotoSizes()
        photoFrameDao.clearAllPhotoFrames()
        vaseDao.clearAllVases()
    }
}

