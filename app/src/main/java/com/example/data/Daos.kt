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
    @Query("SELECT * FROM price_items ORDER BY sortOrder ASC, id ASC")
    fun getAllPriceItems(): Flow<List<PriceItem>>

    @Query("SELECT * FROM price_items WHERE isEnabled = 1 ORDER BY sortOrder ASC, id ASC")
    fun getEnabledPriceItems(): Flow<List<PriceItem>>

    @Query("SELECT * FROM price_items WHERE category = :category ORDER BY sortOrder ASC, id ASC")
    fun getItemsByCategory(category: String): Flow<List<PriceItem>>

    @Query("SELECT * FROM price_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): PriceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PriceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PriceItem>)

    @Update
    suspend fun updateItem(item: PriceItem)

    @Delete
    suspend fun deleteItem(item: PriceItem)

    @Query("DELETE FROM price_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM price_items WHERE isCustom = 0")
    suspend fun clearDefaultItems()

    @Query("SELECT COUNT(*) FROM price_items")
    suspend fun getItemCount(): Int
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM saved_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<SavedOrder>>

    @Query("SELECT * FROM saved_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): SavedOrder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SavedOrder): Long

    @Update
    suspend fun updateOrder(order: SavedOrder)

    @Delete
    suspend fun deleteOrder(order: SavedOrder)

    @Query("DELETE FROM saved_orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_orders")
    suspend fun getOrderCount(): Int
}

@Dao
interface StoneMaterialDao {
    @Query("SELECT * FROM stone_materials ORDER BY sortOrder ASC, name ASC")
    fun getAllMaterials(): Flow<List<StoneMaterialItem>>

    @Query("SELECT * FROM stone_materials WHERE isEnabled = 1 ORDER BY sortOrder ASC, name ASC")
    fun getEnabledMaterials(): Flow<List<StoneMaterialItem>>

    @Query("SELECT * FROM stone_materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialById(id: String): StoneMaterialItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(item: StoneMaterialItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(items: List<StoneMaterialItem>)

    @Update
    suspend fun updateMaterial(item: StoneMaterialItem)

    @Delete
    suspend fun deleteMaterial(item: StoneMaterialItem)

    @Query("DELETE FROM stone_materials WHERE id = :id")
    suspend fun deleteMaterialById(id: String)

    @Query("DELETE FROM stone_materials")
    suspend fun clearAllMaterials()

    @Query("SELECT COUNT(*) FROM stone_materials")
    suspend fun getMaterialsCount(): Int
}

@Dao
interface MonumentSizePresetDao {
    @Query("SELECT * FROM monument_size_presets ORDER BY sortOrder ASC, heightCm ASC, widthCm ASC")
    fun getAllPresets(): Flow<List<MonumentSizePresetItem>>

    @Query("SELECT * FROM monument_size_presets WHERE isEnabled = 1 ORDER BY sortOrder ASC, heightCm ASC, widthCm ASC")
    fun getEnabledPresets(): Flow<List<MonumentSizePresetItem>>

    @Query("SELECT * FROM monument_size_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): MonumentSizePresetItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(item: MonumentSizePresetItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(items: List<MonumentSizePresetItem>)

    @Update
    suspend fun updatePreset(item: MonumentSizePresetItem)

    @Delete
    suspend fun deletePreset(item: MonumentSizePresetItem)

    @Query("DELETE FROM monument_size_presets WHERE id = :id")
    suspend fun deletePresetById(id: String)

    @Query("DELETE FROM monument_size_presets")
    suspend fun clearAllPresets()

    @Query("SELECT COUNT(*) FROM monument_size_presets")
    suspend fun getPresetsCount(): Int
}

@Dao
interface ConstructorServicePriceDao {
    @Query("SELECT * FROM constructor_service_prices ORDER BY sortOrder ASC, key ASC")
    fun getAllServicePrices(): Flow<List<ConstructorServicePriceItem>>

    @Query("SELECT * FROM constructor_service_prices WHERE `key` = :key LIMIT 1")
    suspend fun getServicePriceByKey(key: String): ConstructorServicePriceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicePrice(item: ConstructorServicePriceItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServicePrices(items: List<ConstructorServicePriceItem>)

    @Update
    suspend fun updateServicePrice(item: ConstructorServicePriceItem)

    @Delete
    suspend fun deleteServicePrice(item: ConstructorServicePriceItem)

    @Query("DELETE FROM constructor_service_prices WHERE `key` = :key")
    suspend fun deleteServicePriceByKey(key: String)

    @Query("DELETE FROM constructor_service_prices")
    suspend fun clearAllServicePrices()

    @Query("SELECT COUNT(*) FROM constructor_service_prices")
    suspend fun getPricesCount(): Int
}

