package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {
    @Query("SELECT * FROM price_items WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getAllPriceItems(): Flow<List<PriceItem>>

    @Query("SELECT * FROM price_items WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getEnabledPriceItems(): Flow<List<PriceItem>>

    @Query("SELECT * FROM price_items WHERE category = :category AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getItemsByCategory(category: String): Flow<List<PriceItem>>

    @Query("SELECT * FROM price_items WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getItemById(id: Long): PriceItem?

    @Query("SELECT * FROM price_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND isDeleted = 0")
    suspend fun getItemsByName(name: String): List<PriceItem>

    @Query("SELECT * FROM price_items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdIncludingDeleted(id: Long): PriceItem?

    @Query("SELECT * FROM price_items WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushItems(): List<PriceItem>

    @Query("SELECT * FROM price_items WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteItems(): List<PriceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PriceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PriceItem>)

    @Update
    suspend fun updateItem(item: PriceItem)

    @Delete
    suspend fun deleteItem(item: PriceItem)

    @Query("UPDATE price_items SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteItemById(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM price_items WHERE id = :id")
    suspend fun hardDeleteItemById(id: Long)

    @Query("DELETE FROM price_items WHERE name LIKE '%уборка и мытье%' OR name LIKE '%Разовая уборка%'")
    suspend fun purgeCleaningServiceItems()

    @Query("DELETE FROM price_items")
    suspend fun clearAllPriceItems()

    @Query("DELETE FROM price_items WHERE isCustom = 0")
    suspend fun clearDefaultItems()

    @Query("SELECT COUNT(*) FROM price_items WHERE isDeleted = 0")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM price_items WHERE isDeleted = 0")
    suspend fun getAllPriceItemsSync(): List<PriceItem>
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM saved_orders WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<SavedOrder>>

    @Query("SELECT * FROM saved_orders WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getOrderById(id: String): SavedOrder?

    @Query("SELECT * FROM saved_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderByIdIncludingDeleted(id: String): SavedOrder?

    @Query("SELECT * FROM saved_orders WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushOrders(): List<SavedOrder>

    @Query("SELECT * FROM saved_orders WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteOrders(): List<SavedOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SavedOrder)

    @Update
    suspend fun updateOrder(order: SavedOrder)

    @Delete
    suspend fun deleteOrder(order: SavedOrder)

    @Query("UPDATE saved_orders SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteOrderById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM saved_orders WHERE id = :id")
    suspend fun hardDeleteOrderById(id: String)

    @Query("DELETE FROM saved_orders")
    suspend fun clearAllOrders()

    @Query("SELECT COUNT(*) FROM saved_orders WHERE isDeleted = 0")
    suspend fun getOrderCount(): Int

    @Query("SELECT * FROM saved_orders WHERE isDeleted = 0")
    suspend fun getAllOrdersSync(): List<SavedOrder>
}

@Dao
interface StoneMaterialDao {
    @Query("SELECT * FROM stone_materials WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAllMaterials(): Flow<List<StoneMaterialItem>>

    @Query("SELECT * FROM stone_materials WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getEnabledMaterials(): Flow<List<StoneMaterialItem>>

    @Query("SELECT * FROM stone_materials WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getMaterialById(id: String): StoneMaterialItem?

    @Query("SELECT * FROM stone_materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialByIdIncludingDeleted(id: String): StoneMaterialItem?

    @Query("SELECT * FROM stone_materials WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushMaterials(): List<StoneMaterialItem>

    @Query("SELECT * FROM stone_materials WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteMaterials(): List<StoneMaterialItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(item: StoneMaterialItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(items: List<StoneMaterialItem>)

    @Update
    suspend fun updateMaterial(item: StoneMaterialItem)

    @Delete
    suspend fun deleteMaterial(item: StoneMaterialItem)

    @Query("UPDATE stone_materials SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteMaterialById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM stone_materials WHERE id = :id")
    suspend fun hardDeleteMaterialById(id: String)

    @Query("DELETE FROM stone_materials")
    suspend fun clearAllMaterials()

    @Query("SELECT COUNT(*) FROM stone_materials WHERE isDeleted = 0")
    suspend fun getMaterialsCount(): Int

    @Query("SELECT * FROM stone_materials WHERE isDeleted = 0")
    suspend fun getAllMaterialsSync(): List<StoneMaterialItem>
}

@Dao
interface MonumentSizePresetDao {
    @Query("SELECT * FROM monument_size_presets WHERE isDeleted = 0 ORDER BY sortOrder ASC, heightCm ASC, widthCm ASC")
    fun getAllPresets(): Flow<List<MonumentSizePresetItem>>

    @Query("SELECT * FROM monument_size_presets WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, heightCm ASC, widthCm ASC")
    fun getEnabledPresets(): Flow<List<MonumentSizePresetItem>>

    @Query("SELECT * FROM monument_size_presets WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getPresetById(id: String): MonumentSizePresetItem?

    @Query("SELECT * FROM monument_size_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetByIdIncludingDeleted(id: String): MonumentSizePresetItem?

    @Query("SELECT * FROM monument_size_presets WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushPresets(): List<MonumentSizePresetItem>

    @Query("SELECT * FROM monument_size_presets WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletePresets(): List<MonumentSizePresetItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(item: MonumentSizePresetItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(items: List<MonumentSizePresetItem>)

    @Update
    suspend fun updatePreset(item: MonumentSizePresetItem)

    @Delete
    suspend fun deletePreset(item: MonumentSizePresetItem)

    @Query("UPDATE monument_size_presets SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePresetById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM monument_size_presets WHERE id = :id")
    suspend fun hardDeletePresetById(id: String)

    @Query("DELETE FROM monument_size_presets")
    suspend fun clearAllPresets()

    @Query("SELECT COUNT(*) FROM monument_size_presets WHERE isDeleted = 0")
    suspend fun getPresetsCount(): Int

    @Query("SELECT * FROM monument_size_presets WHERE isDeleted = 0")
    suspend fun getAllPresetsSync(): List<MonumentSizePresetItem>
}

@Dao
interface ConstructorServicePriceDao {
    @Query("SELECT * FROM constructor_service_prices WHERE isDeleted = 0 ORDER BY sortOrder ASC, key ASC")
    fun getAllServicePrices(): Flow<List<ConstructorServicePriceItem>>

    @Query("SELECT * FROM constructor_service_prices WHERE `key` = :key AND isDeleted = 0 LIMIT 1")
    suspend fun getServicePriceByKey(key: String): ConstructorServicePriceItem?

    @Query("SELECT * FROM constructor_service_prices WHERE `key` = :key LIMIT 1")
    suspend fun getServicePriceByKeyIncludingDeleted(key: String): ConstructorServicePriceItem?

    @Query("SELECT * FROM constructor_service_prices WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushServicePrices(): List<ConstructorServicePriceItem>

    @Query("SELECT * FROM constructor_service_prices WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteServicePrices(): List<ConstructorServicePriceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicePrice(item: ConstructorServicePriceItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicePrices(items: List<ConstructorServicePriceItem>)

    @Update
    suspend fun updateServicePrice(item: ConstructorServicePriceItem)

    @Delete
    suspend fun deleteServicePrice(item: ConstructorServicePriceItem)

    @Query("UPDATE constructor_service_prices SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE `key` = :key")
    suspend fun softDeleteServicePriceByKey(key: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM constructor_service_prices WHERE `key` = :key")
    suspend fun hardDeleteServicePriceByKey(key: String)

    @Query("DELETE FROM constructor_service_prices")
    suspend fun clearAllServicePrices()

    @Query("SELECT COUNT(*) FROM constructor_service_prices WHERE isDeleted = 0")
    suspend fun getPricesCount(): Int

    @Query("SELECT * FROM constructor_service_prices WHERE isDeleted = 0")
    suspend fun getAllServicePricesSync(): List<ConstructorServicePriceItem>
}

@Dao
interface EngravingFontDao {
    @Query("SELECT * FROM engraving_fonts WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getAllFonts(): Flow<List<EngravingFontItem>>

    @Query("SELECT * FROM engraving_fonts WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getEnabledFonts(): Flow<List<EngravingFontItem>>

    @Query("SELECT * FROM engraving_fonts WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getFontById(id: String): EngravingFontItem?

    @Query("SELECT * FROM engraving_fonts WHERE id = :id LIMIT 1")
    suspend fun getFontByIdIncludingDeleted(id: String): EngravingFontItem?

    @Query("SELECT * FROM engraving_fonts WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushFonts(): List<EngravingFontItem>

    @Query("SELECT * FROM engraving_fonts WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteFonts(): List<EngravingFontItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFont(item: EngravingFontItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFonts(items: List<EngravingFontItem>)

    @Update
    suspend fun updateFont(item: EngravingFontItem)

    @Delete
    suspend fun deleteFont(item: EngravingFontItem)

    @Query("UPDATE engraving_fonts SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteFontById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM engraving_fonts WHERE id = :id")
    suspend fun hardDeleteFontById(id: String)

    @Query("DELETE FROM engraving_fonts")
    suspend fun clearAllFonts()

    @Query("SELECT COUNT(*) FROM engraving_fonts WHERE isDeleted = 0")
    suspend fun getFontsCount(): Int

    @Query("SELECT * FROM engraving_fonts WHERE isDeleted = 0")
    suspend fun getAllFontsSync(): List<EngravingFontItem>
}

@Dao
interface EngravingDrawingDao {
    @Query("SELECT * FROM engraving_drawings WHERE isDeleted = 0 ORDER BY category ASC, sortOrder ASC, code ASC")
    fun getAllDrawings(): Flow<List<EngravingDrawingItem>>

    @Query("SELECT * FROM engraving_drawings WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY category ASC, sortOrder ASC, code ASC")
    fun getEnabledDrawings(): Flow<List<EngravingDrawingItem>>

    @Query("SELECT * FROM engraving_drawings WHERE category = :category AND isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, code ASC")
    fun getEnabledDrawingsByCategory(category: String): Flow<List<EngravingDrawingItem>>

    @Query("SELECT * FROM engraving_drawings WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getDrawingById(id: String): EngravingDrawingItem?

    @Query("SELECT * FROM engraving_drawings WHERE id = :id LIMIT 1")
    suspend fun getDrawingByIdIncludingDeleted(id: String): EngravingDrawingItem?

    @Query("SELECT * FROM engraving_drawings WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushDrawings(): List<EngravingDrawingItem>

    @Query("SELECT * FROM engraving_drawings WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteDrawings(): List<EngravingDrawingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(item: EngravingDrawingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawings(items: List<EngravingDrawingItem>)

    @Update
    suspend fun updateDrawing(item: EngravingDrawingItem)

    @Delete
    suspend fun deleteDrawing(item: EngravingDrawingItem)

    @Query("UPDATE engraving_drawings SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteDrawingById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM engraving_drawings WHERE id = :id")
    suspend fun hardDeleteDrawingById(id: String)

    @Query("DELETE FROM engraving_drawings")
    suspend fun clearAllDrawings()

    @Query("SELECT COUNT(*) FROM engraving_drawings WHERE isDeleted = 0")
    suspend fun getDrawingsCount(): Int

    @Query("SELECT * FROM engraving_drawings WHERE isDeleted = 0")
    suspend fun getAllDrawingsSync(): List<EngravingDrawingItem>
}

@Dao
interface PhotoSizeDao {
    @Query("SELECT * FROM photo_size_items WHERE isDeleted = 0 ORDER BY category ASC, sortOrder ASC")
    fun getAllPhotoSizes(): Flow<List<PhotoSizeItem>>

    @Query("SELECT * FROM photo_size_items WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY category ASC, sortOrder ASC")
    fun getEnabledPhotoSizes(): Flow<List<PhotoSizeItem>>

    @Query("SELECT * FROM photo_size_items WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getPhotoSizeById(id: String): PhotoSizeItem?

    @Query("SELECT * FROM photo_size_items WHERE id = :id LIMIT 1")
    suspend fun getPhotoSizeByIdIncludingDeleted(id: String): PhotoSizeItem?

    @Query("SELECT * FROM photo_size_items WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushPhotoSizes(): List<PhotoSizeItem>

    @Query("SELECT * FROM photo_size_items WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletePhotoSizes(): List<PhotoSizeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoSize(item: PhotoSizeItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoSizes(items: List<PhotoSizeItem>)

    @Update
    suspend fun updatePhotoSize(item: PhotoSizeItem)

    @Delete
    suspend fun deletePhotoSize(item: PhotoSizeItem)

    @Query("UPDATE photo_size_items SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePhotoSizeById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM photo_size_items WHERE id = :id")
    suspend fun hardDeletePhotoSizeById(id: String)

    @Query("DELETE FROM photo_size_items")
    suspend fun clearAllPhotoSizes()

    @Query("SELECT COUNT(*) FROM photo_size_items WHERE isDeleted = 0")
    suspend fun getPhotoSizesCount(): Int

    @Query("SELECT * FROM photo_size_items WHERE isDeleted = 0")
    suspend fun getAllPhotoSizesSync(): List<PhotoSizeItem>
}

@Dao
interface PhotoFrameDao {
    @Query("SELECT * FROM photo_frame_items WHERE isDeleted = 0 ORDER BY sortOrder ASC")
    fun getAllPhotoFrames(): Flow<List<PhotoFrameItem>>

    @Query("SELECT * FROM photo_frame_items WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC")
    fun getEnabledPhotoFrames(): Flow<List<PhotoFrameItem>>

    @Query("SELECT * FROM photo_frame_items WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getPhotoFrameById(id: String): PhotoFrameItem?

    @Query("SELECT * FROM photo_frame_items WHERE id = :id LIMIT 1")
    suspend fun getPhotoFrameByIdIncludingDeleted(id: String): PhotoFrameItem?

    @Query("SELECT * FROM photo_frame_items WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushPhotoFrames(): List<PhotoFrameItem>

    @Query("SELECT * FROM photo_frame_items WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletePhotoFrames(): List<PhotoFrameItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoFrame(item: PhotoFrameItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoFrames(items: List<PhotoFrameItem>)

    @Update
    suspend fun updatePhotoFrame(item: PhotoFrameItem)

    @Delete
    suspend fun deletePhotoFrame(item: PhotoFrameItem)

    @Query("UPDATE photo_frame_items SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePhotoFrameById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM photo_frame_items WHERE id = :id")
    suspend fun hardDeletePhotoFrameById(id: String)

    @Query("DELETE FROM photo_frame_items")
    suspend fun clearAllPhotoFrames()

    @Query("SELECT COUNT(*) FROM photo_frame_items WHERE isDeleted = 0")
    suspend fun getPhotoFramesCount(): Int

    @Query("SELECT * FROM photo_frame_items WHERE isDeleted = 0")
    suspend fun getAllPhotoFramesSync(): List<PhotoFrameItem>
}

@Dao
interface VaseDao {
    @Query("SELECT * FROM vase_items WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getAllVases(): Flow<List<VaseItem>>

    @Query("SELECT * FROM vase_items WHERE isEnabled = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getEnabledVases(): Flow<List<VaseItem>>

    @Query("SELECT * FROM vase_items WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getVaseById(id: String): VaseItem?

    @Query("SELECT * FROM vase_items WHERE id = :id LIMIT 1")
    suspend fun getVaseByIdIncludingDeleted(id: String): VaseItem?

    @Query("SELECT * FROM vase_items WHERE syncStatus = 'PENDING_PUSH' AND isDeleted = 0")
    suspend fun getPendingPushVases(): List<VaseItem>

    @Query("SELECT * FROM vase_items WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteVases(): List<VaseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVase(item: VaseItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVases(items: List<VaseItem>)

    @Update
    suspend fun updateVase(item: VaseItem)

    @Delete
    suspend fun deleteVase(item: VaseItem)

    @Query("UPDATE vase_items SET isDeleted = 1, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteVaseById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM vase_items WHERE id = :id")
    suspend fun hardDeleteVaseById(id: String)

    @Query("DELETE FROM vase_items")
    suspend fun clearAllVases()

    @Query("SELECT COUNT(*) FROM vase_items WHERE isDeleted = 0")
    suspend fun getVasesCount(): Int

    @Query("SELECT * FROM vase_items WHERE isDeleted = 0")
    suspend fun getAllVasesSync(): List<VaseItem>
}





