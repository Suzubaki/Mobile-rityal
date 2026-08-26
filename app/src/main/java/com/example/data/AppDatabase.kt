package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Database(
    entities = [
        PriceItem::class,
        SavedOrder::class,
        StoneMaterialItem::class,
        MonumentSizePresetItem::class,
        ConstructorServicePriceItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceDao(): PriceDao
    abstract fun orderDao(): OrderDao
    abstract fun stoneMaterialDao(): StoneMaterialDao
    abstract fun monumentSizePresetDao(): MonumentSizePresetDao
    abstract fun constructorServicePriceDao(): ConstructorServicePriceDao

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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.priceDao().insertItems(DefaultCatalog.getDefaultItems())
                                database.stoneMaterialDao().insertMaterials(DefaultCatalog.getDefaultStoneMaterials())
                                database.monumentSizePresetDao().insertPresets(DefaultCatalog.getDefaultSizePresets())
                                database.constructorServicePriceDao().insertServicePrices(DefaultCatalog.getDefaultConstructorServicePrices())
                            }
                        }
                    }).build()
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
    private val constructorServicePriceDao: ConstructorServicePriceDao
) {
    val allPriceItems: Flow<List<PriceItem>> = priceDao.getAllPriceItems()
    val enabledPriceItems: Flow<List<PriceItem>> = priceDao.getEnabledPriceItems()
    val allOrders: Flow<List<SavedOrder>> = orderDao.getAllOrders()

    val allStoneMaterials: Flow<List<StoneMaterialItem>> = stoneMaterialDao.getAllMaterials()
    val enabledStoneMaterials: Flow<List<StoneMaterialItem>> = stoneMaterialDao.getEnabledMaterials()

    val allSizePresets: Flow<List<MonumentSizePresetItem>> = monumentSizePresetDao.getAllPresets()
    val enabledSizePresets: Flow<List<MonumentSizePresetItem>> = monumentSizePresetDao.getEnabledPresets()

    val allConstructorServicePrices: Flow<List<ConstructorServicePriceItem>> = constructorServicePriceDao.getAllServicePrices()

    suspend fun ensureDefaultDataLoaded() {
        if (priceDao.getItemCount() == 0) {
            priceDao.insertItems(DefaultCatalog.getDefaultItems())
        }
        if (stoneMaterialDao.getMaterialsCount() == 0) {
            stoneMaterialDao.insertMaterials(DefaultCatalog.getDefaultStoneMaterials())
        }
        if (monumentSizePresetDao.getPresetsCount() == 0) {
            monumentSizePresetDao.insertPresets(DefaultCatalog.getDefaultSizePresets())
        }
        if (constructorServicePriceDao.getPricesCount() == 0) {
            constructorServicePriceDao.insertServicePrices(DefaultCatalog.getDefaultConstructorServicePrices())
        }
    }

    // --- Price Items ---
    suspend fun insertPriceItem(item: PriceItem): Long = priceDao.insertItem(item)
    suspend fun updatePriceItem(item: PriceItem) = priceDao.updateItem(item)
    suspend fun deletePriceItem(item: PriceItem) = priceDao.deleteItem(item)
    suspend fun deletePriceItem(itemId: Long) = priceDao.deleteItemById(itemId)
    suspend fun resetToDefaultPrices() {
        priceDao.clearDefaultItems()
        priceDao.insertItems(DefaultCatalog.getDefaultItems())
    }

    // --- Stone Materials ---
    suspend fun insertStoneMaterial(material: StoneMaterialItem) = stoneMaterialDao.insertMaterial(material)
    suspend fun updateStoneMaterial(material: StoneMaterialItem) = stoneMaterialDao.updateMaterial(material)
    suspend fun deleteStoneMaterial(material: StoneMaterialItem) = stoneMaterialDao.deleteMaterial(material)
    suspend fun resetStoneMaterials() {
        stoneMaterialDao.clearAllMaterials()
        stoneMaterialDao.insertMaterials(DefaultCatalog.getDefaultStoneMaterials())
    }

    // --- Size Presets ---
    suspend fun insertSizePreset(preset: MonumentSizePresetItem) = monumentSizePresetDao.insertPreset(preset)
    suspend fun updateSizePreset(preset: MonumentSizePresetItem) = monumentSizePresetDao.updatePreset(preset)
    suspend fun deleteSizePreset(preset: MonumentSizePresetItem) = monumentSizePresetDao.deletePreset(preset)
    suspend fun resetSizePresets() {
        monumentSizePresetDao.clearAllPresets()
        monumentSizePresetDao.insertPresets(DefaultCatalog.getDefaultSizePresets())
    }

    // --- Constructor Service Rates ---
    suspend fun insertServicePrice(item: ConstructorServicePriceItem) = constructorServicePriceDao.insertServicePrice(item)
    suspend fun updateServicePrice(item: ConstructorServicePriceItem) = constructorServicePriceDao.updateServicePrice(item)
    suspend fun deleteServicePrice(item: ConstructorServicePriceItem) = constructorServicePriceDao.deleteServicePrice(item)
    suspend fun deleteServicePriceByKey(key: String) = constructorServicePriceDao.deleteServicePriceByKey(key)
    suspend fun resetConstructorServicePrices() {
        constructorServicePriceDao.clearAllServicePrices()
        constructorServicePriceDao.insertServicePrices(DefaultCatalog.getDefaultConstructorServicePrices())
    }

    // --- Orders ---
    suspend fun saveOrder(order: SavedOrder): Long = orderDao.insertOrder(order)
    suspend fun updateOrder(order: SavedOrder) = orderDao.updateOrder(order)
    suspend fun deleteOrder(order: SavedOrder) = orderDao.deleteOrder(order)
    suspend fun deleteOrder(orderId: Long) = orderDao.deleteOrderById(orderId)
}
