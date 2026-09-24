package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncSummary(
    val ordersCount: Int,
    val pricesCount: Int,
    val materialsCount: Int,
    val presetsCount: Int,
    val servicePricesCount: Int,
    val fontsCount: Int = 0,
    val drawingsCount: Int = 0,
    val photoSizesCount: Int = 0,
    val photoFramesCount: Int = 0,
    val vasesCount: Int = 0,
    val executionTimeMs: Long = 0
)

sealed class SyncState {
    object Disabled : SyncState()
    object NotAuthenticated : SyncState()
    object Syncing : SyncState()
    data class Connected(val userEmail: String?, val lastSyncTime: Long) : SyncState()
    data class Error(val message: String, val isPermissionError: Boolean = false) : SyncState()
}

class FirebaseSyncManager(
    private val context: Context,
    private val repository: RitualRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    private val prefs = context.getSharedPreferences("firebase_sync_prefs", Context.MODE_PRIVATE)
    private val projectId = "dwa-angeld"
    private val apiKey = "AIzaSyBOzkw_mMXjES9dMcBT70EwQRLD7ehWiUc"
    private var activeDbName: String = (prefs.getString("firestore_db_name", "default") ?: "default")
        .replace("(", "").replace(")", "").trim().ifBlank { "default" }

    fun getActiveDatabaseId(): String = activeDbName

    fun setDatabaseId(newId: String) {
        val sanitized = newId.replace("(", "").replace(")", "").trim().ifBlank { "default" }
        activeDbName = sanitized
        prefs.edit().putString("firestore_db_name", sanitized).apply()
        reinitFirestoreSdk()
    }

    private fun reinitFirestoreSdk() {
        runCatching {
            val app = FirebaseApp.getInstance()
            firestore = try {
                FirebaseFirestore.getInstance(app, activeDbName)
            } catch (_: Exception) {
                FirebaseFirestore.getInstance(app)
            }.apply {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            }
        }
    }

    private fun getRestBaseUrl(dbName: String = activeDbName): String =
        "https://firestore.googleapis.com/v1/projects/$projectId/databases/$dbName/documents"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Disabled)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var ordersListener: ListenerRegistration? = null
    private var priceListener: ListenerRegistration? = null
    private var stoneListener: ListenerRegistration? = null
    private var sizePresetsListener: ListenerRegistration? = null
    private var constructorPricesListener: ListenerRegistration? = null
    private var fontsListener: ListenerRegistration? = null
    private var drawingsListener: ListenerRegistration? = null
    private var photoSizesListener: ListenerRegistration? = null
    private var photoFramesListener: ListenerRegistration? = null
    private var vasesListener: ListenerRegistration? = null

    init {
        initFirebaseIfAvailable()
    }

    private fun initFirebaseIfAvailable() {
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val app = FirebaseApp.getInstance()
            auth = FirebaseAuth.getInstance()
            firestore = runCatching {
                FirebaseFirestore.getInstance(app, activeDbName)
            }.getOrElse {
                FirebaseFirestore.getInstance(app)
            }.apply {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            }

            val user = auth?.currentUser
            val initialLabel = if (user != null) (user.email ?: "Авторизован") else "Общая база (Firestore)"
            _syncState.value = SyncState.Connected(initialLabel, System.currentTimeMillis())

            auth?.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    _syncState.value = SyncState.Connected(currentUser.email ?: "Авторизован", System.currentTimeMillis())
                } else {
                    _syncState.value = SyncState.Connected("Общая база (Firestore)", System.currentTimeMillis())
                }
            }

            // Attach Firestore Realtime Cache Listeners
            startRealtimeListeners()
        }.onFailure { e ->
            Log.w(TAG, "Firebase SDK warning: ${e.message}. Falling back to REST Cloud.")
            _syncState.value = SyncState.Connected("Общая база (REST Cloud)", System.currentTimeMillis())
        }
    }

    fun isFirebaseConfigured(): Boolean = true

    fun getCurrentUserEmail(): String? = auth?.currentUser?.email

    fun signOut() {
        auth?.signOut()
        _syncState.value = SyncState.Connected("Общая база (Firestore)", System.currentTimeMillis())
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<String> {
        val a = auth ?: return Result.failure(IllegalStateException("Firebase Auth не инициализирован"))
        return runCatching {
            val res = a.signInWithEmailAndPassword(email.trim(), pass).await()
            val userEmail = res.user?.email ?: email
            _syncState.value = SyncState.Connected(userEmail, System.currentTimeMillis())
            startRealtimeListeners()
            userEmail
        }
    }

    suspend fun registerWithEmail(email: String, pass: String): Result<String> {
        val a = auth ?: return Result.failure(IllegalStateException("Firebase Auth не инициализирован"))
        return runCatching {
            val res = a.createUserWithEmailAndPassword(email.trim(), pass).await()
            val userEmail = res.user?.email ?: email
            _syncState.value = SyncState.Connected(userEmail, System.currentTimeMillis())
            startRealtimeListeners()
            userEmail
        }
    }

    // =========================================================================
    // REALTIME FIRESTORE LISTENERS -> ROOM CACHE UPDATES (NO ECHO WRITES TO CLOUD)
    // =========================================================================

    fun startRealtimeListeners() {
        val db = firestore ?: return

        // 1. Orders Listener
        runCatching {
            ordersListener?.remove()
            ordersListener = db.collection("orders")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} orders from Firestore")
                        val cloudOrderIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudOrderIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", "createdAt", default = System.currentTimeMillis())
                                val existing = repository.getOrderByIdIncludingDeleted(id)

                                val cloudOrder = SavedOrder(
                                    id = id,
                                    orderNumber = doc.extractString("orderNumber", default = existing?.orderNumber ?: ""),
                                    clientName = doc.extractString("clientName", default = existing?.clientName ?: ""),
                                    clientPhone = doc.extractString("clientPhone", default = existing?.clientPhone ?: ""),
                                    deceasedName = doc.extractString("deceasedName", default = existing?.deceasedName ?: ""),
                                    cemeteryName = doc.extractString("cemeteryName", default = existing?.cemeteryName ?: ""),
                                    plotNumber = doc.extractString("plotNumber", default = existing?.plotNumber ?: ""),
                                    createdAt = doc.extractLong("createdAt", default = existing?.createdAt ?: System.currentTimeMillis()),
                                    updatedAt = cloudUpdatedAt,
                                    status = doc.extractString("status", default = existing?.status ?: "DRAFT"),
                                    subtotalAmount = doc.extractDouble("subtotalAmount", default = existing?.subtotalAmount ?: 0.0),
                                    discountPercent = doc.extractDouble("discountPercent", default = existing?.discountPercent ?: 0.0),
                                    discountAmount = doc.extractDouble("discountAmount", default = existing?.discountAmount ?: 0.0),
                                    totalAmount = doc.extractDouble("totalAmount", default = existing?.totalAmount ?: 0.0),
                                    prepaymentAmount = doc.extractDouble("prepaymentAmount", default = existing?.prepaymentAmount ?: 0.0),
                                    remainingAmount = doc.extractDouble("remainingAmount", default = existing?.remainingAmount ?: 0.0),
                                    itemsJson = doc.extractString("itemsJson", default = existing?.itemsJson ?: "[]"),
                                    notes = doc.extractString("notes", default = existing?.notes ?: ""),
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveOrderToLocalCacheOnly(cloudOrder)
                            }
                        }

                        val allLocalOrders = repository.getAllOrdersSync()
                        for (localOrder in allLocalOrders) {
                            if (!cloudOrderIds.contains(localOrder.id) && localOrder.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteOrder(localOrder.id)
                            }
                        }
                        updateConnectedState()
                    }
                }
        }

        // 2. Price Items Catalog Listener
        runCatching {
            priceListener?.remove()
            priceListener = db.collection("price_catalog")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} price items from Firestore")
                        val cloudIds = mutableSetOf<Long>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                if (doc.id == "0") {
                                    deleteSinglePriceInternal(0L)
                                    return@runCatching
                                }
                                var id = doc.extractLong("id").takeIf { it > 0 } ?: doc.id.toLongOrNull()?.takeIf { it > 0 } ?: 0L
                                val itemName = doc.extractString("name", "title", default = "Позиция #$id")
                                if (itemName.contains("уборка", ignoreCase = true) || itemName.contains("мытье", ignoreCase = true)) {
                                    if (id > 0) deleteSinglePriceInternal(id)
                                    if (id > 0) repository.hardDeletePriceItem(id)
                                    return@runCatching
                                }
                                if (id <= 0L) {
                                    val defaultMatch = DefaultCatalog.getDefaultItems().find { it.name.trim().equals(itemName.trim(), ignoreCase = true) }
                                    id = defaultMatch?.id ?: return@runCatching
                                }
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getPriceItemByIdIncludingDeleted(id)

                                val defaultP = doc.extractDouble("defaultPrice", "default_price", "price", "currentPrice", "cost", default = existing?.defaultPrice ?: 0.0)
                                val currentP = doc.extractDouble("currentPrice", "current_price", "price", "cost", "priceByn", "price_byn", "defaultPrice", default = existing?.currentPrice ?: defaultP)
                                val item = PriceItem(
                                    id = id,
                                    category = doc.extractString("category", default = existing?.category ?: ItemCategory.MONUMENTS.name),
                                    subcategory = doc.extractString("subcategory", "subCategory", default = existing?.subcategory ?: ""),
                                    name = itemName,
                                    unit = doc.extractString("unit", default = existing?.unit ?: "шт"),
                                    defaultPrice = defaultP,
                                    currentPrice = currentP,
                                    description = doc.extractString("description", "desc", default = existing?.description ?: ""),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.savePriceItemToLocalCacheOnly(item)
                            }
                        }

                        repository.deduplicateLocalPriceItems()

                        val allLocalPrices = repository.getAllPriceItemsSync()
                        for (local in allLocalPrices) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeletePriceItem(local.id)
                            }
                        }
                        updateConnectedState()
                    }
                }
        }

        // 3. Stone Materials Listener
        runCatching {
            stoneListener?.remove()
            stoneListener = db.collection("stone_materials")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} stone materials from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getMaterialByIdIncludingDeleted(id)

                                val item = StoneMaterialItem(
                                    id = id,
                                    name = doc.extractString("name", "title", default = existing?.name ?: id),
                                    colorName = doc.extractString("colorName", "color", default = existing?.colorName ?: ""),
                                    pricePerM3 = doc.extractDouble("pricePerM3", "price_per_m3", "price", default = existing?.pricePerM3 ?: 0.0),
                                    origin = doc.extractString("origin", default = existing?.origin ?: ""),
                                    description = doc.extractString("description", "desc", default = existing?.description ?: ""),
                                    suitableForDirectEngraving = doc.extractBool("suitableForDirectEngraving", "engraving", default = existing?.suitableForDirectEngraving ?: true),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveStoneMaterialToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllMaterialsSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteStoneMaterial(local.id)
                            }
                        }
                    }
                }
        }

        // 4. Monument Size Presets Listener
        runCatching {
            sizePresetsListener?.remove()
            sizePresetsListener = db.collection("monument_size_presets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} size presets from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getPresetByIdIncludingDeleted(id)

                                val item = MonumentSizePresetItem(
                                    id = id,
                                    name = doc.extractString("name", "title", default = existing?.name ?: id),
                                    heightCm = doc.extractInt("heightCm", "height", default = existing?.heightCm ?: 100),
                                    widthCm = doc.extractInt("widthCm", "width", default = existing?.widthCm ?: 50),
                                    thicknessCm = doc.extractInt("thicknessCm", "thickness", default = existing?.thicknessCm ?: 8),
                                    steleBasePrice = doc.extractDouble("steleBasePrice", "stelePrice", default = existing?.steleBasePrice ?: 0.0),
                                    plinthBasePrice = doc.extractDouble("plinthBasePrice", "plinthPrice", default = existing?.plinthBasePrice ?: 0.0),
                                    flowerbedBasePrice = doc.extractDouble("flowerbedBasePrice", "flowerbedPrice", default = existing?.flowerbedBasePrice ?: 0.0),
                                    plinthDimensions = doc.extractString("plinthDimensions", default = existing?.plinthDimensions ?: ""),
                                    flowerbedDimensions = doc.extractString("flowerbedDimensions", default = existing?.flowerbedDimensions ?: ""),
                                    isFamily = doc.extractBool("isFamily", default = existing?.isFamily ?: false),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveSizePresetToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllPresetsSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteSizePreset(local.id)
                            }
                        }
                    }
                }
        }

        // 5. Constructor Service Rates Listener
        runCatching {
            constructorPricesListener?.remove()
            constructorPricesListener = db.collection("constructor_prices")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} service rates from Firestore")
                        val cloudKeys = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val key = doc.extractString("key").ifBlank { doc.id }
                                if (key.isBlank()) return@runCatching
                                cloudKeys.add(key)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getServicePriceByKeyIncludingDeleted(key)

                                val item = ConstructorServicePriceItem(
                                    key = key,
                                    groupName = doc.extractString("groupName", "group", "category", default = existing?.groupName ?: ""),
                                    title = doc.extractString("title", "name", default = existing?.title ?: key),
                                    unit = doc.extractString("unit", default = existing?.unit ?: "BYN"),
                                    price = doc.extractDouble("price", "currentPrice", "cost", "value", default = existing?.price ?: 0.0),
                                    description = doc.extractString("description", "desc", default = existing?.description ?: ""),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveServicePriceToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllServicePricesSync()
                        for (local in allLocal) {
                            if (!cloudKeys.contains(local.key) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteServicePriceByKey(local.key)
                            }
                        }
                    }
                }
        }

        // 6. Engraving Fonts Listener
        runCatching {
            fontsListener?.remove()
            fontsListener = db.collection("engraving_fonts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} fonts from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getFontByIdIncludingDeleted(id)

                                val item = EngravingFontItem(
                                    id = id,
                                    name = doc.extractString("name", "title", default = existing?.name ?: id),
                                    styleKey = doc.extractString("styleKey", default = existing?.styleKey ?: "SERIF"),
                                    price = doc.extractDouble("price", default = existing?.price ?: 0.0),
                                    sampleText = doc.extractString("sampleText", default = existing?.sampleText ?: "Иванов Иван Иванович\n1950 — 2024"),
                                    description = doc.extractString("description", default = existing?.description ?: ""),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveFontToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllFontsSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteFont(local.id)
                            }
                        }
                    }
                }
        }

        // 7. Engraving Drawings Listener
        runCatching {
            drawingsListener?.remove()
            drawingsListener = db.collection("engraving_drawings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} drawings from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getDrawingByIdIncludingDeleted(id)

                                val item = EngravingDrawingItem(
                                    id = id,
                                    code = doc.extractString("code", default = existing?.code ?: id),
                                    category = doc.extractString("category", default = existing?.category ?: "Крест"),
                                    name = doc.extractString("name", "title", default = existing?.name ?: ""),
                                    price = doc.extractDouble("price", "standardPrice", default = existing?.price ?: 35.0),
                                    description = doc.extractString("description", default = existing?.description ?: ""),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveDrawingToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllDrawingsSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteDrawing(local.id)
                            }
                        }
                    }
                }
        }

        // 8. Photo Sizes Listener
        runCatching {
            photoSizesListener?.remove()
            photoSizesListener = db.collection("photo_sizes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} photo sizes from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getPhotoSizeByIdIncludingDeleted(id)

                                val item = PhotoSizeItem(
                                    id = id,
                                    category = doc.extractString("category", default = existing?.category ?: "Фотокерамика"),
                                    sizeName = doc.extractString("sizeName", "name", default = existing?.sizeName ?: id),
                                    price = doc.extractDouble("price", default = existing?.price ?: 150.0),
                                    description = doc.extractString("description", default = existing?.description ?: ""),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.savePhotoSizeToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllPhotoSizesSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeletePhotoSize(local.id)
                            }
                        }
                    }
                }
        }

        // 9. Photo Frames Listener
        runCatching {
            photoFramesListener?.remove()
            photoFramesListener = db.collection("photo_frames")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} photo frames from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getPhotoFrameByIdIncludingDeleted(id)

                                val item = PhotoFrameItem(
                                    id = id,
                                    name = doc.extractString("name", "title", default = existing?.name ?: id),
                                    materialType = doc.extractString("materialType", "material", default = existing?.materialType ?: "Бронза"),
                                    price = doc.extractDouble("price", default = existing?.price ?: 80.0),
                                    description = doc.extractString("description", default = existing?.description ?: ""),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.savePhotoFrameToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllPhotoFramesSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeletePhotoFrame(local.id)
                            }
                        }
                    }
                }
        }

        // 10. Vases Listener
        runCatching {
            vasesListener?.remove()
            vasesListener = db.collection("vases")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        Log.d(TAG, "[Realtime] Received ${snapshot.documents.size} vases from Firestore")
                        val cloudIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            runCatching {
                                val id = doc.extractString("id").ifBlank { doc.id }
                                if (id.isBlank()) return@runCatching
                                cloudIds.add(id)

                                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                                val existing = repository.getVaseByIdIncludingDeleted(id)

                                val item = VaseItem(
                                    id = id,
                                    name = doc.extractString("name", "title", default = existing?.name ?: id),
                                    materialType = doc.extractString("materialType", "material", default = existing?.materialType ?: "Гранит"),
                                    sizeCm = doc.extractString("sizeCm", default = existing?.sizeCm ?: "30 см"),
                                    price = doc.extractDouble("price", default = existing?.price ?: 120.0),
                                    description = doc.extractString("description", default = existing?.description ?: ""),
                                    isCustom = doc.extractBool("isCustom", default = existing?.isCustom ?: false),
                                    isEnabled = doc.extractBool("isEnabled", default = existing?.isEnabled ?: true),
                                    sortOrder = doc.extractInt("sortOrder", "order", default = existing?.sortOrder ?: 0),
                                    updatedAt = cloudUpdatedAt,
                                    syncStatus = SyncStatus.SYNCED,
                                    isDeleted = false
                                )
                                repository.saveVaseToLocalCacheOnly(item)
                            }
                        }

                        val allLocal = repository.getAllVasesSync()
                        for (local in allLocal) {
                            if (!cloudIds.contains(local.id) && local.syncStatus != SyncStatus.PENDING_PUSH) {
                                repository.hardDeleteVase(local.id)
                            }
                        }
                    }
                }
        }
    }

    private fun updateConnectedState() {
        val user = auth?.currentUser
        val label = if (user != null) (user.email ?: "Авторизован") else "Общая база (Firestore)"
        _syncState.value = SyncState.Connected(label, System.currentTimeMillis())
    }

    // =========================================================================
    // ONLINE DIRECT MUTATIONS (FIRESTORE PRIMARY -> LOCAL ROOM CACHE UPDATE)
    // =========================================================================

    suspend fun saveOrderOnline(order: SavedOrder): Boolean {
        Log.d(TAG, "Executing saveOrderOnline for order ID=${order.id}")
        val pushed = pushSingleOrderInternal(order)
        if (pushed) {
            repository.saveOrderToLocalCacheOnly(order.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteOrderOnline(orderId: String): Boolean {
        Log.d(TAG, "Executing deleteOrderOnline for order ID=$orderId")
        repository.hardDeleteOrder(orderId)
        val deleted = deleteSingleOrderInternal(orderId)
        return deleted
    }

    suspend fun savePriceItemOnline(item: PriceItem): Boolean {
        Log.d(TAG, "Executing savePriceItemOnline for price ID=${item.id}")
        val pushed = pushSinglePriceInternal(item)
        if (pushed) {
            repository.savePriceItemToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deletePriceItemOnline(itemId: Long, itemName: String? = null): Boolean {
        Log.d(TAG, "Executing deletePriceItemOnline for price ID=$itemId, name=$itemName")
        repository.hardDeletePriceItem(itemId)
        val deleted = deleteSinglePriceInternal(itemId, itemName)
        return deleted
    }

    suspend fun saveStoneMaterialOnline(material: StoneMaterialItem): Boolean {
        Log.d(TAG, "Executing saveStoneMaterialOnline for material ID=${material.id}")
        val pushed = pushSingleMaterialInternal(material)
        if (pushed) {
            repository.saveStoneMaterialToLocalCacheOnly(material.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteStoneMaterialOnline(materialId: String, materialName: String? = null): Boolean {
        Log.d(TAG, "Executing deleteStoneMaterialOnline for material ID=$materialId, name=$materialName")
        repository.hardDeleteStoneMaterial(materialId)
        val deleted = deleteSingleMaterialInternal(materialId, materialName)
        return deleted
    }

    suspend fun saveSizePresetOnline(preset: MonumentSizePresetItem): Boolean {
        val pushed = pushSinglePresetInternal(preset)
        if (pushed) {
            repository.saveSizePresetToLocalCacheOnly(preset.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteSizePresetOnline(presetId: String, presetName: String? = null): Boolean {
        repository.hardDeleteSizePreset(presetId)
        val deleted = deleteSinglePresetInternal(presetId, presetName)
        return deleted
    }

    suspend fun saveServicePriceOnline(item: ConstructorServicePriceItem): Boolean {
        val pushed = pushSingleServicePriceInternal(item)
        if (pushed) {
            repository.saveServicePriceToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteServicePriceOnline(key: String, title: String? = null): Boolean {
        repository.hardDeleteServicePriceByKey(key)
        val deleted = deleteSingleServicePriceInternal(key, title)
        return deleted
    }

    suspend fun saveFontOnline(font: EngravingFontItem): Boolean {
        Log.d(TAG, "Executing saveFontOnline for font ID=${font.id}")
        val pushed = pushSingleFontInternal(font)
        if (pushed) {
            repository.saveFontToLocalCacheOnly(font.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteFontOnline(fontId: String, fontName: String? = null): Boolean {
        Log.d(TAG, "Executing deleteFontOnline for font ID=$fontId, name=$fontName")
        repository.hardDeleteFont(fontId)
        val deleted = deleteSingleFontInternal(fontId, fontName)
        return deleted
    }

    suspend fun saveDrawingOnline(drawing: EngravingDrawingItem): Boolean {
        Log.d(TAG, "Executing saveDrawingOnline for drawing ID=${drawing.id}")
        val pushed = pushSingleDrawingInternal(drawing)
        if (pushed) {
            repository.saveDrawingToLocalCacheOnly(drawing.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteDrawingOnline(drawingId: String, drawingName: String? = null): Boolean {
        Log.d(TAG, "Executing deleteDrawingOnline for drawing ID=$drawingId, name=$drawingName")
        repository.hardDeleteDrawing(drawingId)
        val deleted = deleteSingleDrawingInternal(drawingId, drawingName)
        return deleted
    }

    suspend fun savePhotoSizeOnline(item: PhotoSizeItem): Boolean {
        Log.d(TAG, "Executing savePhotoSizeOnline for photo size ID=${item.id}")
        val pushed = pushSinglePhotoSizeInternal(item)
        if (pushed) {
            repository.savePhotoSizeToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deletePhotoSizeOnline(itemId: String, sizeName: String? = null): Boolean {
        Log.d(TAG, "Executing deletePhotoSizeOnline for photo size ID=$itemId, name=$sizeName")
        repository.hardDeletePhotoSize(itemId)
        val deleted = deleteSinglePhotoSizeInternal(itemId, sizeName)
        return deleted
    }

    suspend fun savePhotoFrameOnline(item: PhotoFrameItem): Boolean {
        Log.d(TAG, "Executing savePhotoFrameOnline for photo frame ID=${item.id}")
        val pushed = pushSinglePhotoFrameInternal(item)
        if (pushed) {
            repository.savePhotoFrameToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deletePhotoFrameOnline(itemId: String, frameName: String? = null): Boolean {
        Log.d(TAG, "Executing deletePhotoFrameOnline for photo frame ID=$itemId, name=$frameName")
        repository.hardDeletePhotoFrame(itemId)
        val deleted = deleteSinglePhotoFrameInternal(itemId, frameName)
        return deleted
    }

    suspend fun saveVaseOnline(vase: VaseItem): Boolean {
        Log.d(TAG, "Executing saveVaseOnline for vase ID=${vase.id}")
        val pushed = pushSingleVaseInternal(vase)
        if (pushed) {
            repository.saveVaseToLocalCacheOnly(vase.copy(syncStatus = SyncStatus.SYNCED))
        }
        return pushed
    }

    suspend fun deleteVaseOnline(vaseId: String, vaseName: String? = null): Boolean {
        Log.d(TAG, "Executing deleteVaseOnline for vase ID=$vaseId, name=$vaseName")
        repository.hardDeleteVase(vaseId)
        val deleted = deleteSingleVaseInternal(vaseId, vaseName)
        return deleted
    }

    suspend fun resetPricesToDefaultOnline(): Boolean = true

    suspend fun resetStoneMaterialsOnline(): Boolean = true

    suspend fun resetSizePresetsOnline(): Boolean = true

    suspend fun resetConstructorServicePricesOnline(): Boolean = true

    suspend fun resetFontsOnline(): Boolean = true

    suspend fun resetDrawingsOnline(): Boolean = true

    suspend fun resetPhotoSizesOnline(): Boolean = true

    suspend fun resetPhotoFramesOnline(): Boolean = true

    suspend fun resetVasesOnline(): Boolean = true

    // =========================================================================
    // SEED INITIAL CLOUD DATA & FULL UPLOAD TO FIRESTORE (BATCHED REST + SDK)
    // =========================================================================

    private suspend fun getAuthHeader(): Pair<String, String>? {
        val user = auth?.currentUser ?: return null
        return runCatching {
            val token = user.getIdToken(false).await().token
            if (!token.isNullOrBlank()) {
                "Authorization" to "Bearer $token"
            } else null
        }.getOrNull()
    }

    private suspend fun createRestRequestBuilder(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
        val authHeader = getAuthHeader()
        if (authHeader != null) {
            builder.addHeader(authHeader.first, authHeader.second)
        }
        return builder
    }

    suspend fun seedFirestoreIfEmpty(): Boolean = withContext(Dispatchers.IO) {
        return@withContext false
    }

    fun formatFirestoreError(e: Throwable): String {
        val msg = e.localizedMessage ?: e.message ?: ""
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("Missing or insufficient permissions", ignoreCase = true) || msg.contains("403", ignoreCase = true) ->
                "🚫 Ошибка доступа (PERMISSION_DENIED / 403): В Firebase Console откройте Firestore Database -> вкладка 'Security Rules' (Правила) и установите: allow read, write: if true; (и нажмите Publish)"
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("network", ignoreCase = true) ->
                "📡 Ошибка сети: Нет связи с серверами Firebase Firestore. Проверьте подключение к интернету."
            else ->
                "Ошибка Firestore: ${msg.ifBlank { "Неизвестная ошибка" }}"
        }
    }

    private suspend fun commitRestBatchWrites(writesList: List<JSONObject>): Result<Unit> = withContext(Dispatchers.IO) {
        if (writesList.isEmpty()) return@withContext Result.success(Unit)
        runCatching {
            val commitUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/$activeDbName/documents:commit?key=$apiKey"
            for (chunk in writesList.chunked(200)) {
                val rootJson = JSONObject()
                val writesArr = JSONArray()
                for (w in chunk) {
                    writesArr.put(w)
                }
                rootJson.put("writes", writesArr)

                val req = createRestRequestBuilder(commitUrl)
                    .post(rootJson.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        throw IllegalStateException("REST commit error HTTP ${resp.code}: $body")
                    }
                }
            }
            Unit
        }
    }

    suspend fun uploadAllDataToFirestore(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Starting full batch upload to Firestore...")

        runCatching {
            val now = System.currentTimeMillis()
            repository.deduplicateLocalPriceItems()
            var orders = repository.getAllOrdersSync()
            var prices = repository.getAllPriceItemsSync()
            var materials = repository.getAllMaterialsSync()
            var presets = repository.getAllPresetsSync()
            var services = repository.getAllServicePricesSync()
            var fonts = repository.getAllFontsSync()
            var drawings = repository.getAllDrawingsSync()
            var photoSizes = repository.getAllPhotoSizesSync()
            var photoFrames = repository.getAllPhotoFramesSync()
            var vases = repository.getAllVasesSync()

            prices = prices.filter { !it.name.contains("уборка", ignoreCase = true) && !it.name.contains("мытье", ignoreCase = true) }
                .map { item ->
                    if (item.id <= 0L) {
                        item.copy(id = (System.currentTimeMillis() % 1000000 + 1000))
                    } else {
                        item
                    }
                }

            val restWrites = mutableListOf<JSONObject>()

            // 1. Price Catalog writes
            for (item in prices) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/price_catalog/${item.id}")
                updateObj.put("fields", priceToRestDoc(item).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 2. Stone Materials writes
            for (mat in materials) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/stone_materials/${mat.id}")
                updateObj.put("fields", stoneToRestDoc(mat).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 3. Presets writes
            for (preset in presets) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/monument_size_presets/${preset.id}")
                updateObj.put("fields", presetToRestDoc(preset).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 4. Services writes
            for (service in services) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/constructor_prices/${service.key}")
                updateObj.put("fields", constructorToRestDoc(service).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 5. Orders writes
            for (order in orders) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/orders/${order.id}")
                updateObj.put("fields", orderToRestDoc(order).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 6. Fonts writes
            for (font in fonts) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/engraving_fonts/${font.id}")
                updateObj.put("fields", fontToRestDoc(font).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 7. Drawings writes
            for (drawing in drawings) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/engraving_drawings/${drawing.id}")
                updateObj.put("fields", drawingToRestDoc(drawing).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 8. Photo Sizes writes
            for (ps in photoSizes) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/photo_sizes/${ps.id}")
                updateObj.put("fields", photoSizeToRestDoc(ps).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 9. Photo Frames writes
            for (pf in photoFrames) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/photo_frames/${pf.id}")
                updateObj.put("fields", photoFrameToRestDoc(pf).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // 10. Vases writes
            for (vase in vases) {
                val writeObj = JSONObject()
                val updateObj = JSONObject()
                updateObj.put("name", "projects/$projectId/databases/$activeDbName/documents/vases/${vase.id}")
                updateObj.put("fields", vaseToRestDoc(vase).getJSONObject("fields"))
                writeObj.put("update", updateObj)
                restWrites.add(writeObj)
            }

            // Execute REST commit directly to Google Cloud Firestore
            val restResult = commitRestBatchWrites(restWrites)
            if (restResult.isFailure) {
                val ex = restResult.exceptionOrNull()!!
                Log.w(TAG, "REST Batch upload failed: ${ex.message}. Trying Firestore SDK batch...")
                
                val db = firestore ?: throw ex
                for (chunk in prices.chunked(400)) {
                    val batch = db.batch()
                    for (item in chunk) {
                        val docRef = db.collection("price_catalog").document(item.id.toString())
                        batch.set(docRef, mapOf(
                            "id" to item.id,
                            "category" to item.category,
                            "subcategory" to item.subcategory,
                            "name" to item.name,
                            "unit" to item.unit,
                            "defaultPrice" to item.defaultPrice,
                            "currentPrice" to item.currentPrice,
                            "description" to item.description,
                            "isCustom" to item.isCustom,
                            "isEnabled" to item.isEnabled,
                            "sortOrder" to item.sortOrder,
                            "updatedAt" to (if (item.updatedAt > 0) item.updatedAt else now)
                        ), SetOptions.merge())
                    }
                    batch.commit().await()
                }

                if (materials.isNotEmpty()) {
                    for (chunk in materials.chunked(400)) {
                        val batch = db.batch()
                        for (mat in chunk) {
                            val docRef = db.collection("stone_materials").document(mat.id)
                            batch.set(docRef, mapOf(
                                "id" to mat.id,
                                "name" to mat.name,
                                "colorName" to mat.colorName,
                                "pricePerM3" to mat.pricePerM3,
                                "origin" to mat.origin,
                                "description" to mat.description,
                                "suitableForDirectEngraving" to mat.suitableForDirectEngraving,
                                "isCustom" to mat.isCustom,
                                "isEnabled" to mat.isEnabled,
                                "sortOrder" to mat.sortOrder,
                                "updatedAt" to (if (mat.updatedAt > 0) mat.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (presets.isNotEmpty()) {
                    for (chunk in presets.chunked(400)) {
                        val batch = db.batch()
                        for (preset in chunk) {
                            val docRef = db.collection("monument_size_presets").document(preset.id)
                            batch.set(docRef, mapOf(
                                "id" to preset.id,
                                "name" to preset.name,
                                "heightCm" to preset.heightCm,
                                "widthCm" to preset.widthCm,
                                "thicknessCm" to preset.thicknessCm,
                                "steleBasePrice" to preset.steleBasePrice,
                                "plinthBasePrice" to preset.plinthBasePrice,
                                "flowerbedBasePrice" to preset.flowerbedBasePrice,
                                "plinthDimensions" to preset.plinthDimensions,
                                "flowerbedDimensions" to preset.flowerbedDimensions,
                                "isFamily" to preset.isFamily,
                                "isCustom" to preset.isCustom,
                                "isEnabled" to preset.isEnabled,
                                "sortOrder" to preset.sortOrder,
                                "updatedAt" to (if (preset.updatedAt > 0) preset.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (services.isNotEmpty()) {
                    for (chunk in services.chunked(400)) {
                        val batch = db.batch()
                        for (service in chunk) {
                            val docRef = db.collection("constructor_prices").document(service.key)
                            batch.set(docRef, mapOf(
                                "key" to service.key,
                                "groupName" to service.groupName,
                                "title" to service.title,
                                "unit" to service.unit,
                                "price" to service.price,
                                "description" to service.description,
                                "sortOrder" to service.sortOrder,
                                "updatedAt" to (if (service.updatedAt > 0) service.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (orders.isNotEmpty()) {
                    for (chunk in orders.chunked(400)) {
                        val batch = db.batch()
                        for (order in chunk) {
                            val docRef = db.collection("orders").document(order.id)
                            batch.set(docRef, mapOf(
                                "id" to order.id,
                                "orderNumber" to order.orderNumber,
                                "clientName" to order.clientName,
                                "clientPhone" to order.clientPhone,
                                "deceasedName" to order.deceasedName,
                                "cemeteryName" to order.cemeteryName,
                                "plotNumber" to order.plotNumber,
                                "createdAt" to order.createdAt,
                                "updatedAt" to (if (order.updatedAt > 0) order.updatedAt else now),
                                "status" to order.status,
                                "subtotalAmount" to order.subtotalAmount,
                                "discountPercent" to order.discountPercent,
                                "discountAmount" to order.discountAmount,
                                "totalAmount" to order.totalAmount,
                                "prepaymentAmount" to order.prepaymentAmount,
                                "remainingAmount" to order.remainingAmount,
                                "itemsJson" to order.itemsJson,
                                "notes" to order.notes
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (fonts.isNotEmpty()) {
                    for (chunk in fonts.chunked(400)) {
                        val batch = db.batch()
                        for (font in chunk) {
                            val docRef = db.collection("engraving_fonts").document(font.id)
                            batch.set(docRef, mapOf(
                                "id" to font.id,
                                "name" to font.name,
                                "styleKey" to font.styleKey,
                                "price" to font.price,
                                "sampleText" to font.sampleText,
                                "description" to font.description,
                                "isCustom" to font.isCustom,
                                "isEnabled" to font.isEnabled,
                                "sortOrder" to font.sortOrder,
                                "updatedAt" to (if (font.updatedAt > 0) font.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (drawings.isNotEmpty()) {
                    for (chunk in drawings.chunked(400)) {
                        val batch = db.batch()
                        for (drawing in chunk) {
                            val docRef = db.collection("engraving_drawings").document(drawing.id)
                            batch.set(docRef, mapOf(
                                "id" to drawing.id,
                                "category" to drawing.category,
                                "code" to drawing.code,
                                "name" to drawing.name,
                                "price" to drawing.price,
                                "description" to drawing.description,
                                "isCustom" to drawing.isCustom,
                                "isEnabled" to drawing.isEnabled,
                                "sortOrder" to drawing.sortOrder,
                                "updatedAt" to (if (drawing.updatedAt > 0) drawing.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (photoSizes.isNotEmpty()) {
                    for (chunk in photoSizes.chunked(400)) {
                        val batch = db.batch()
                        for (ps in chunk) {
                            val docRef = db.collection("photo_sizes").document(ps.id)
                            batch.set(docRef, mapOf(
                                "id" to ps.id,
                                "category" to ps.category,
                                "sizeName" to ps.sizeName,
                                "price" to ps.price,
                                "description" to ps.description,
                                "isCustom" to ps.isCustom,
                                "isEnabled" to ps.isEnabled,
                                "sortOrder" to ps.sortOrder,
                                "updatedAt" to (if (ps.updatedAt > 0) ps.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (photoFrames.isNotEmpty()) {
                    for (chunk in photoFrames.chunked(400)) {
                        val batch = db.batch()
                        for (pf in chunk) {
                            val docRef = db.collection("photo_frames").document(pf.id)
                            batch.set(docRef, mapOf(
                                "id" to pf.id,
                                "name" to pf.name,
                                "materialType" to pf.materialType,
                                "price" to pf.price,
                                "description" to pf.description,
                                "isCustom" to pf.isCustom,
                                "isEnabled" to pf.isEnabled,
                                "sortOrder" to pf.sortOrder,
                                "updatedAt" to (if (pf.updatedAt > 0) pf.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }

                if (vases.isNotEmpty()) {
                    for (chunk in vases.chunked(400)) {
                        val batch = db.batch()
                        for (vase in chunk) {
                            val docRef = db.collection("vases").document(vase.id)
                            batch.set(docRef, mapOf(
                                "id" to vase.id,
                                "name" to vase.name,
                                "materialType" to vase.materialType,
                                "sizeCm" to vase.sizeCm,
                                "price" to vase.price,
                                "description" to vase.description,
                                "isCustom" to vase.isCustom,
                                "isEnabled" to vase.isEnabled,
                                "sortOrder" to vase.sortOrder,
                                "updatedAt" to (if (vase.updatedAt > 0) vase.updatedAt else now)
                            ), SetOptions.merge())
                        }
                        batch.commit().await()
                    }
                }
            }

            Log.d(TAG, "Successfully uploaded all data to Firestore.")
            updateConnectedState()

            SyncSummary(
                ordersCount = orders.size,
                pricesCount = prices.size,
                materialsCount = materials.size,
                presetsCount = presets.size,
                servicePricesCount = services.size,
                fontsCount = fonts.size,
                drawingsCount = drawings.size,
                photoSizesCount = photoSizes.size,
                photoFramesCount = photoFrames.size,
                vasesCount = vases.size,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }.onFailure { e ->
            Log.e(TAG, "Failed to upload all data to Firestore: ${e.message}", e)
            val msg = formatFirestoreError(e)
            _syncState.value = SyncState.Error(msg)
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val userEmail = auth?.currentUser?.email ?: "Не авторизован"
        runCatching {
            var targetDb = activeDbName
            var url = "${getRestBaseUrl(targetDb)}/price_catalog?pageSize=10&key=$apiKey"
            var req = createRestRequestBuilder(url).get().build()
            var response = httpClient.newCall(req).execute()
            var code = response.code
            var body = response.body?.string() ?: ""
            response.close()

            // If 404 occurred with "default", try fallback "(default)" or vice versa
            if (code == 404) {
                val alternateDb = if (targetDb == "default") "(default)" else "default"
                val altUrl = "${getRestBaseUrl(alternateDb)}/price_catalog?pageSize=10&key=$apiKey"
                val altReq = createRestRequestBuilder(altUrl).get().build()
                val altResp = httpClient.newCall(altReq).execute()
                if (altResp.code == 200) {
                    targetDb = alternateDb
                    setDatabaseId(targetDb)
                    code = altResp.code
                    body = altResp.body?.string() ?: ""
                }
                altResp.close()
            }

            if (code == 200) {
                val json = JSONObject(body)
                val docsCount = if (json.has("documents")) json.getJSONArray("documents").length() else 0
                val msg = "🟢 Firestore подключен (HTTP 200 OK)! База: '$targetDb', Пользователь: $userEmail. В облаке найдено позиций: $docsCount"
                Log.d(TAG, msg)
                _syncState.value = SyncState.Connected(userEmail, System.currentTimeMillis())
                msg
            } else {
                Log.e(TAG, "Firestore test failed: HTTP $code, $body")
                val isPerm = code == 403 || body.contains("PERMISSION_DENIED", ignoreCase = true)
                val errMsg = if (isPerm) {
                    "🚫 Ошибка доступа 403 (PERMISSION_DENIED). В Firebase Console откройте Firestore Database -> вкладка Rules (Правила) и разрешите запись: allow read, write: if true; (и нажмите Publish)"
                } else {
                    "🔴 Ошибка Firestore (HTTP $code, база '$targetDb'): ${body.take(150)}"
                }
                throw IllegalStateException(errMsg)
            }
        }.onFailure { e ->
            Log.e(TAG, "Connection test failed: ${e.message}", e)
            val msg = formatFirestoreError(e)
            _syncState.value = SyncState.Error(msg)
        }
    }

    private suspend fun fetchOrderFromCloud(orderId: String): DocumentSnapshot? {
        val db = firestore ?: return null
        return runCatching {
            db.collection("orders").document(orderId).get().await()
        }.getOrNull()
    }

    suspend fun syncPendingOfflineChanges() = withContext(Dispatchers.IO) {
        if (firestore == null) return@withContext
        runCatching {
            // Pending Delete Orders
            val pendingDeleteOrders = repository.getPendingDeleteOrders()
            for (order in pendingDeleteOrders) {
                deleteSingleOrderInternal(order.id)
                repository.hardDeleteOrder(order.id)
            }
            // Pending Push Orders
            val pendingPushOrders = repository.getPendingPushOrders()
            for (order in pendingPushOrders) {
                val pushed = pushSingleOrderInternal(order)
                if (pushed) {
                    repository.saveOrderToLocalCacheOnly(order.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            // Pending Delete Service Prices
            val pendingDeleteServicePrices = repository.getPendingDeleteServicePrices()
            for (item in pendingDeleteServicePrices) {
                deleteSingleServicePriceInternal(item.key, item.title)
                repository.hardDeleteServicePriceByKey(item.key)
            }
            // Pending Push Service Prices
            val pendingPushServicePrices = repository.getPendingPushServicePrices()
            for (item in pendingPushServicePrices) {
                val pushed = pushSingleServicePriceInternal(item)
                if (pushed) {
                    repository.saveServicePriceToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            // Pending Delete Fonts
            val pendingDeleteFonts = repository.getPendingDeleteFonts()
            for (item in pendingDeleteFonts) {
                deleteSingleFontInternal(item.id, item.name)
                repository.hardDeleteFont(item.id)
            }
            // Pending Push Fonts
            val pendingPushFonts = repository.getPendingPushFonts()
            for (item in pendingPushFonts) {
                val pushed = pushSingleFontInternal(item)
                if (pushed) {
                    repository.saveFontToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            // Pending Delete Drawings
            val pendingDeleteDrawings = repository.getPendingDeleteDrawings()
            for (item in pendingDeleteDrawings) {
                deleteSingleDrawingInternal(item.id, item.name)
                repository.hardDeleteDrawing(item.id)
            }
            // Pending Push Drawings
            val pendingPushDrawings = repository.getPendingPushDrawings()
            for (item in pendingPushDrawings) {
                val pushed = pushSingleDrawingInternal(item)
                if (pushed) {
                    repository.saveDrawingToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            // Pending Delete Photo Sizes
            val pendingDeletePhotoSizes = repository.getPendingDeletePhotoSizes()
            for (item in pendingDeletePhotoSizes) {
                deleteSinglePhotoSizeInternal(item.id, item.sizeName)
                repository.hardDeletePhotoSize(item.id)
            }
            // Pending Push Photo Sizes
            val pendingPushPhotoSizes = repository.getPendingPushPhotoSizes()
            for (item in pendingPushPhotoSizes) {
                val pushed = pushSinglePhotoSizeInternal(item)
                if (pushed) {
                    repository.savePhotoSizeToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            // Pending Delete Photo Frames
            val pendingDeletePhotoFrames = repository.getPendingDeletePhotoFrames()
            for (item in pendingDeletePhotoFrames) {
                deleteSinglePhotoFrameInternal(item.id, item.name)
                repository.hardDeletePhotoFrame(item.id)
            }
            // Pending Push Photo Frames
            val pendingPushPhotoFrames = repository.getPendingPushPhotoFrames()
            for (item in pendingPushPhotoFrames) {
                val pushed = pushSinglePhotoFrameInternal(item)
                if (pushed) {
                    repository.savePhotoFrameToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            // Pending Delete Vases
            val pendingDeleteVases = repository.getPendingDeleteVases()
            for (item in pendingDeleteVases) {
                deleteSingleVaseInternal(item.id, item.name)
                repository.hardDeleteVase(item.id)
            }
            // Pending Push Vases
            val pendingPushVases = repository.getPendingPushVases()
            for (item in pendingPushVases) {
                val pushed = pushSingleVaseInternal(item)
                if (pushed) {
                    repository.saveVaseToLocalCacheOnly(item.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
        }
    }

    // =========================================================================
    // REFRESH LOCAL CACHE FROM CLOUD
    // =========================================================================

    suspend fun refreshLocalCacheFromFirestore(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Refreshing local Room cache from Firestore...")
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore не доступен"))

        runCatching {
            syncPendingOfflineChanges()

            val ordersSnapshot = db.collection("orders").get().await()
            var ordersCount = 0
            val cloudOrderIds = mutableSetOf<String>()
            for (doc in ordersSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudOrderIds.add(id)
                val cloudUpdatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis())
                val cloudOrder = SavedOrder(
                    id = id,
                    orderNumber = doc.extractString("orderNumber", default = ""),
                    clientName = doc.extractString("clientName", default = ""),
                    clientPhone = doc.extractString("clientPhone", default = ""),
                    deceasedName = doc.extractString("deceasedName", default = ""),
                    cemeteryName = doc.extractString("cemeteryName", default = ""),
                    plotNumber = doc.extractString("plotNumber", default = ""),
                    createdAt = doc.extractLong("createdAt", default = System.currentTimeMillis()),
                    updatedAt = cloudUpdatedAt,
                    status = doc.extractString("status", default = "DRAFT"),
                    subtotalAmount = doc.extractDouble("subtotalAmount", default = 0.0),
                    discountPercent = doc.extractDouble("discountPercent", default = 0.0),
                    discountAmount = doc.extractDouble("discountAmount", default = 0.0),
                    totalAmount = doc.extractDouble("totalAmount", default = 0.0),
                    prepaymentAmount = doc.extractDouble("prepaymentAmount", default = 0.0),
                    remainingAmount = doc.extractDouble("remainingAmount", default = 0.0),
                    itemsJson = doc.extractString("itemsJson", default = "[]"),
                    notes = doc.extractString("notes", default = ""),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveOrderToLocalCacheOnly(cloudOrder)
                ordersCount++
            }
            val allLocalOrders = repository.getAllOrdersSync()
            for (localOrder in allLocalOrders) {
                if (!cloudOrderIds.contains(localOrder.id) && localOrder.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteOrder(localOrder.id)
                }
            }

            val priceSnapshot = db.collection("price_catalog").get().await()
            var pricesCount = 0
            val cloudPriceIds = mutableSetOf<Long>()
            for (doc in priceSnapshot.documents) {
                if (doc.id == "0") {
                    deleteSinglePriceInternal(0L)
                    continue
                }
                var id = doc.extractLong("id").takeIf { it > 0 } ?: doc.id.toLongOrNull()?.takeIf { it > 0 } ?: 0L
                val itemName = doc.extractString("name", "title", default = "Позиция #$id")
                if (itemName.contains("уборка", ignoreCase = true) || itemName.contains("мытье", ignoreCase = true)) {
                    if (id > 0) deleteSinglePriceInternal(id)
                    if (id > 0) repository.hardDeletePriceItem(id)
                    continue
                }
                if (id <= 0L) {
                    continue
                }
                cloudPriceIds.add(id)
                val item = PriceItem(
                    id = id,
                    category = doc.extractString("category", default = ItemCategory.MONUMENTS.name),
                    subcategory = doc.extractString("subcategory", default = ""),
                    name = itemName,
                    unit = doc.extractString("unit", default = "шт"),
                    defaultPrice = doc.extractDouble("defaultPrice", default = 0.0),
                    currentPrice = doc.extractDouble("currentPrice", default = 0.0),
                    description = doc.extractString("description", default = ""),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.savePriceItemToLocalCacheOnly(item)
                pricesCount++
            }
            repository.deduplicateLocalPriceItems()
            val allLocalPrices = repository.getAllPriceItemsSync()
            for (localPrice in allLocalPrices) {
                if (!cloudPriceIds.contains(localPrice.id) && localPrice.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeletePriceItem(localPrice.id)
                }
            }

            val materialsSnapshot = db.collection("stone_materials").get().await()
            var materialsCount = 0
            val cloudMaterialIds = mutableSetOf<String>()
            for (doc in materialsSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudMaterialIds.add(id)
                val mat = StoneMaterialItem(
                    id = id,
                    name = doc.extractString("name", "title", default = id),
                    colorName = doc.extractString("colorName", default = ""),
                    pricePerM3 = doc.extractDouble("pricePerM3", default = 0.0),
                    origin = doc.extractString("origin", default = ""),
                    description = doc.extractString("description", default = ""),
                    suitableForDirectEngraving = doc.extractBool("suitableForDirectEngraving", default = true),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveStoneMaterialToLocalCacheOnly(mat)
                materialsCount++
            }
            val allLocalMaterials = repository.getAllMaterialsSync()
            for (localMat in allLocalMaterials) {
                if (!cloudMaterialIds.contains(localMat.id) && localMat.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteStoneMaterial(localMat.id)
                }
            }

            val presetsSnapshot = db.collection("monument_size_presets").get().await()
            var presetsCount = 0
            val cloudPresetIds = mutableSetOf<String>()
            for (doc in presetsSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudPresetIds.add(id)
                val preset = MonumentSizePresetItem(
                    id = id,
                    name = doc.extractString("name", default = id),
                    heightCm = doc.extractInt("heightCm", default = 100),
                    widthCm = doc.extractInt("widthCm", default = 50),
                    thicknessCm = doc.extractInt("thicknessCm", default = 8),
                    steleBasePrice = doc.extractDouble("steleBasePrice", default = 0.0),
                    plinthBasePrice = doc.extractDouble("plinthBasePrice", default = 0.0),
                    flowerbedBasePrice = doc.extractDouble("flowerbedBasePrice", default = 0.0),
                    plinthDimensions = doc.extractString("plinthDimensions", default = ""),
                    flowerbedDimensions = doc.extractString("flowerbedDimensions", default = ""),
                    isFamily = doc.extractBool("isFamily", default = false),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveSizePresetToLocalCacheOnly(preset)
                presetsCount++
            }
            val allLocalPresets = repository.getAllPresetsSync()
            for (localPreset in allLocalPresets) {
                if (!cloudPresetIds.contains(localPreset.id) && localPreset.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteSizePreset(localPreset.id)
                }
            }

            val servicePricesSnapshot = db.collection("constructor_prices").get().await()
            var servicePricesCount = 0
            val cloudServiceKeys = mutableSetOf<String>()
            for (doc in servicePricesSnapshot.documents) {
                val key = doc.extractString("key").ifBlank { doc.id }
                if (key.isBlank()) continue
                cloudServiceKeys.add(key)
                val sp = ConstructorServicePriceItem(
                    key = key,
                    groupName = doc.extractString("groupName", default = ""),
                    title = doc.extractString("title", default = key),
                    unit = doc.extractString("unit", default = "BYN"),
                    price = doc.extractDouble("price", default = 0.0),
                    description = doc.extractString("description", default = ""),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveServicePriceToLocalCacheOnly(sp)
                servicePricesCount++
            }
            val allLocalServicePrices = repository.getAllServicePricesSync()
            for (localService in allLocalServicePrices) {
                if (!cloudServiceKeys.contains(localService.key) && localService.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteServicePriceByKey(localService.key)
                }
            }

            // Engraving Fonts
            val fontsSnapshot = db.collection("engraving_fonts").get().await()
            var fontsCount = 0
            val cloudFontIds = mutableSetOf<String>()
            for (doc in fontsSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudFontIds.add(id)
                val font = EngravingFontItem(
                    id = id,
                    name = doc.extractString("name", "title", default = id),
                    styleKey = doc.extractString("styleKey", default = "SERIF"),
                    price = doc.extractDouble("price", default = 0.0),
                    sampleText = doc.extractString("sampleText", default = "Иванов Иван Иванович\n1950 — 2024"),
                    description = doc.extractString("description", default = ""),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveFontToLocalCacheOnly(font)
                fontsCount++
            }
            val allLocalFonts = repository.getAllFontsSync()
            for (localFont in allLocalFonts) {
                if (!cloudFontIds.contains(localFont.id) && localFont.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteFont(localFont.id)
                }
            }

            // Engraving Drawings
            val drawingsSnapshot = db.collection("engraving_drawings").get().await()
            var drawingsCount = 0
            val cloudDrawingIds = mutableSetOf<String>()
            for (doc in drawingsSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudDrawingIds.add(id)
                val drawing = EngravingDrawingItem(
                    id = id,
                    category = doc.extractString("category", default = "Крест"),
                    code = doc.extractString("code", default = id),
                    name = doc.extractString("name", "title", default = ""),
                    price = doc.extractDouble("price", "standardPrice", default = 35.0),
                    description = doc.extractString("description", default = ""),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveDrawingToLocalCacheOnly(drawing)
                drawingsCount++
            }
            val allLocalDrawings = repository.getAllDrawingsSync()
            for (localDrawing in allLocalDrawings) {
                if (!cloudDrawingIds.contains(localDrawing.id) && localDrawing.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteDrawing(localDrawing.id)
                }
            }

            // Photo Sizes
            val photoSizesSnapshot = db.collection("photo_sizes").get().await()
            var photoSizesCount = 0
            val cloudPhotoSizeIds = mutableSetOf<String>()
            for (doc in photoSizesSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudPhotoSizeIds.add(id)
                val ps = PhotoSizeItem(
                    id = id,
                    category = doc.extractString("category", default = "Фотокерамика"),
                    sizeName = doc.extractString("sizeName", default = id),
                    price = doc.extractDouble("price", default = 150.0),
                    description = doc.extractString("description", default = ""),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.savePhotoSizeToLocalCacheOnly(ps)
                photoSizesCount++
            }
            val allLocalPhotoSizes = repository.getAllPhotoSizesSync()
            for (localPs in allLocalPhotoSizes) {
                if (!cloudPhotoSizeIds.contains(localPs.id) && localPs.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeletePhotoSize(localPs.id)
                }
            }

            // Photo Frames
            val photoFramesSnapshot = db.collection("photo_frames").get().await()
            var photoFramesCount = 0
            val cloudPhotoFrameIds = mutableSetOf<String>()
            for (doc in photoFramesSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudPhotoFrameIds.add(id)
                val pf = PhotoFrameItem(
                    id = id,
                    name = doc.extractString("name", "title", default = id),
                    materialType = doc.extractString("materialType", "material", default = "Бронза"),
                    price = doc.extractDouble("price", default = 80.0),
                    description = doc.extractString("description", default = ""),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.savePhotoFrameToLocalCacheOnly(pf)
                photoFramesCount++
            }
            val allLocalPhotoFrames = repository.getAllPhotoFramesSync()
            for (localPf in allLocalPhotoFrames) {
                if (!cloudPhotoFrameIds.contains(localPf.id) && localPf.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeletePhotoFrame(localPf.id)
                }
            }

            // Vases
            val vasesSnapshot = db.collection("vases").get().await()
            var vasesCount = 0
            val cloudVaseIds = mutableSetOf<String>()
            for (doc in vasesSnapshot.documents) {
                val id = doc.extractString("id").ifBlank { doc.id }
                if (id.isBlank()) continue
                cloudVaseIds.add(id)
                val vase = VaseItem(
                    id = id,
                    name = doc.extractString("name", "title", default = id),
                    materialType = doc.extractString("materialType", "material", default = "Гранит"),
                    sizeCm = doc.extractString("sizeCm", default = "30 см"),
                    price = doc.extractDouble("price", default = 120.0),
                    description = doc.extractString("description", default = ""),
                    isCustom = doc.extractBool("isCustom", default = false),
                    isEnabled = doc.extractBool("isEnabled", default = true),
                    sortOrder = doc.extractInt("sortOrder", default = 0),
                    updatedAt = doc.extractLong("updatedAt", default = System.currentTimeMillis()),
                    syncStatus = SyncStatus.SYNCED,
                    isDeleted = false
                )
                repository.saveVaseToLocalCacheOnly(vase)
                vasesCount++
            }
            val allLocalVases = repository.getAllVasesSync()
            for (localVase in allLocalVases) {
                if (!cloudVaseIds.contains(localVase.id) && localVase.syncStatus != SyncStatus.PENDING_PUSH) {
                    repository.hardDeleteVase(localVase.id)
                }
            }

            val summary = SyncSummary(
                ordersCount = ordersCount,
                pricesCount = pricesCount,
                materialsCount = materialsCount,
                presetsCount = presetsCount,
                servicePricesCount = servicePricesCount,
                fontsCount = fontsCount,
                drawingsCount = drawingsCount,
                photoSizesCount = photoSizesCount,
                photoFramesCount = photoFramesCount,
                vasesCount = vasesCount,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
            summary
        }
    }

    suspend fun clearBothRoomAndFirestore(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Clearing both Firestore and Room databases...")
            val db = firestore
            val collections = listOf(
                "orders",
                "price_catalog",
                "stone_materials",
                "monument_size_presets",
                "constructor_prices",
                "engraving_fonts",
                "engraving_drawings",
                "photo_sizes",
                "photo_frames",
                "vases"
            )

            if (db != null) {
                for (collName in collections) {
                    val snapshot = db.collection(collName).get().await()
                    for (doc in snapshot.documents) {
                        doc.reference.delete().await()
                    }
                }
            } else {
                for (collName in collections) {
                    val url = "${getRestBaseUrl()}/$collName?key=$apiKey"
                    val request = Request.Builder().url(url).get().build()
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val json = JSONObject(body)
                            if (json.has("documents")) {
                                val docs = json.getJSONArray("documents")
                                for (i in 0 until docs.length()) {
                                    val docObj = docs.getJSONObject(i)
                                    val docName = docObj.getString("name")
                                    val deleteUrl = "https://firestore.googleapis.com/v1/$docName?key=$apiKey"
                                    val delReq = Request.Builder().url(deleteUrl).delete().build()
                                    httpClient.newCall(delReq).execute().close()
                                }
                            }
                        }
                    }
                }
            }

            repository.clearAllRoomData()
            Log.d(TAG, "Successfully cleared both Firestore and Room databases.")
            Unit
        }
    }

    // =========================================================================
    // LOW-LEVEL SINGLE ITEM FIRESTORE WRITES & DELETES
    // =========================================================================

    private suspend fun pushSingleOrderInternal(order: SavedOrder): Boolean {
        // Try REST first with Auth token for immediate cloud persistence
        try {
            val url = "${getRestBaseUrl()}/orders/${order.id}?key=$apiKey"
            val doc = orderToRestDoc(order)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                // Also write to local SDK cache
                try {
                    firestore?.collection("orders")?.document(order.id)?.set(mapOf(
                        "id" to order.id,
                        "orderNumber" to order.orderNumber,
                        "clientName" to order.clientName,
                        "clientPhone" to order.clientPhone,
                        "deceasedName" to order.deceasedName,
                        "cemeteryName" to order.cemeteryName,
                        "plotNumber" to order.plotNumber,
                        "createdAt" to order.createdAt,
                        "updatedAt" to order.updatedAt,
                        "status" to order.status,
                        "subtotalAmount" to order.subtotalAmount,
                        "discountPercent" to order.discountPercent,
                        "discountAmount" to order.discountAmount,
                        "totalAmount" to order.totalAmount,
                        "prepaymentAmount" to order.prepaymentAmount,
                        "remainingAmount" to order.remainingAmount,
                        "itemsJson" to order.itemsJson,
                        "notes" to order.notes
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write order failed: ${ex.message}")
        }

        return try {
            firestore?.collection("orders")?.document(order.id)
                ?.set(mapOf(
                    "id" to order.id,
                    "orderNumber" to order.orderNumber,
                    "clientName" to order.clientName,
                    "clientPhone" to order.clientPhone,
                    "deceasedName" to order.deceasedName,
                    "cemeteryName" to order.cemeteryName,
                    "plotNumber" to order.plotNumber,
                    "createdAt" to order.createdAt,
                    "updatedAt" to order.updatedAt,
                    "status" to order.status,
                    "subtotalAmount" to order.subtotalAmount,
                    "discountPercent" to order.discountPercent,
                    "discountAmount" to order.discountAmount,
                    "totalAmount" to order.totalAmount,
                    "prepaymentAmount" to order.prepaymentAmount,
                    "remainingAmount" to order.remainingAmount,
                    "itemsJson" to order.itemsJson,
                    "notes" to order.notes
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push order ${order.id} via SDK: ${e.message}")
            false
        }
    }

    private suspend fun deleteSingleOrderInternal(orderId: String): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/orders/$orderId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete orders/$orderId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete order error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("orders")?.document(orderId)?.delete()?.await()
        } catch (e: Exception) {
            Log.w(TAG, "SDK delete orders/$orderId failed: ${e.message}")
        }

        try {
            val snapshot = firestore?.collection("orders")?.whereEqualTo("id", orderId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        return true
    }

    private suspend fun pushSinglePriceInternal(item: PriceItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/price_catalog/${item.id}?key=$apiKey"
            val doc = priceToRestDoc(item)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("price_catalog")?.document(item.id.toString())?.set(mapOf(
                        "id" to item.id,
                        "category" to item.category,
                        "subcategory" to item.subcategory,
                        "name" to item.name,
                        "unit" to item.unit,
                        "defaultPrice" to item.defaultPrice,
                        "currentPrice" to item.currentPrice,
                        "description" to item.description,
                        "isCustom" to item.isCustom,
                        "isEnabled" to item.isEnabled,
                        "sortOrder" to item.sortOrder,
                        "updatedAt" to item.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write price failed: ${ex.message}")
        }

        return try {
            firestore?.collection("price_catalog")?.document(item.id.toString())
                ?.set(mapOf(
                    "id" to item.id,
                    "category" to item.category,
                    "subcategory" to item.subcategory,
                    "name" to item.name,
                    "unit" to item.unit,
                    "defaultPrice" to item.defaultPrice,
                    "currentPrice" to item.currentPrice,
                    "description" to item.description,
                    "isCustom" to item.isCustom,
                    "isEnabled" to item.isEnabled,
                    "sortOrder" to item.sortOrder,
                    "updatedAt" to item.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSinglePriceInternal(itemId: Long, itemName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/price_catalog/$itemId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete price_catalog/$itemId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete price error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("price_catalog")?.document(itemId.toString())?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("price_catalog")?.whereEqualTo("id", itemId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!itemName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("price_catalog")?.whereEqualTo("name", itemName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSingleMaterialInternal(material: StoneMaterialItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/stone_materials/${material.id}?key=$apiKey"
            val doc = stoneToRestDoc(material)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("stone_materials")?.document(material.id)?.set(mapOf(
                        "id" to material.id,
                        "name" to material.name,
                        "colorName" to material.colorName,
                        "pricePerM3" to material.pricePerM3,
                        "origin" to material.origin,
                        "description" to material.description,
                        "suitableForDirectEngraving" to material.suitableForDirectEngraving,
                        "isCustom" to material.isCustom,
                        "isEnabled" to material.isEnabled,
                        "sortOrder" to material.sortOrder,
                        "updatedAt" to material.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write stone material failed: ${ex.message}")
        }

        return try {
            firestore?.collection("stone_materials")?.document(material.id)
                ?.set(mapOf(
                    "id" to material.id,
                    "name" to material.name,
                    "colorName" to material.colorName,
                    "pricePerM3" to material.pricePerM3,
                    "origin" to material.origin,
                    "description" to material.description,
                    "suitableForDirectEngraving" to material.suitableForDirectEngraving,
                    "isCustom" to material.isCustom,
                    "isEnabled" to material.isEnabled,
                    "sortOrder" to material.sortOrder,
                    "updatedAt" to material.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSingleMaterialInternal(materialId: String, materialName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/stone_materials/$materialId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete stone_materials/$materialId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete stone error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("stone_materials")?.document(materialId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("stone_materials")?.whereEqualTo("id", materialId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!materialName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("stone_materials")?.whereEqualTo("name", materialName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSinglePresetInternal(preset: MonumentSizePresetItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/monument_size_presets/${preset.id}?key=$apiKey"
            val doc = presetToRestDoc(preset)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("monument_size_presets")?.document(preset.id)?.set(mapOf(
                        "id" to preset.id,
                        "name" to preset.name,
                        "heightCm" to preset.heightCm,
                        "widthCm" to preset.widthCm,
                        "thicknessCm" to preset.thicknessCm,
                        "steleBasePrice" to preset.steleBasePrice,
                        "plinthBasePrice" to preset.plinthBasePrice,
                        "flowerbedBasePrice" to preset.flowerbedBasePrice,
                        "plinthDimensions" to preset.plinthDimensions,
                        "flowerbedDimensions" to preset.flowerbedDimensions,
                        "isFamily" to preset.isFamily,
                        "isCustom" to preset.isCustom,
                        "isEnabled" to preset.isEnabled,
                        "sortOrder" to preset.sortOrder,
                        "updatedAt" to preset.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write preset failed: ${ex.message}")
        }

        return try {
            firestore?.collection("monument_size_presets")?.document(preset.id)
                ?.set(mapOf(
                    "id" to preset.id,
                    "name" to preset.name,
                    "heightCm" to preset.heightCm,
                    "widthCm" to preset.widthCm,
                    "thicknessCm" to preset.thicknessCm,
                    "steleBasePrice" to preset.steleBasePrice,
                    "plinthBasePrice" to preset.plinthBasePrice,
                    "flowerbedBasePrice" to preset.flowerbedBasePrice,
                    "plinthDimensions" to preset.plinthDimensions,
                    "flowerbedDimensions" to preset.flowerbedDimensions,
                    "isFamily" to preset.isFamily,
                    "isCustom" to preset.isCustom,
                    "isEnabled" to preset.isEnabled,
                    "sortOrder" to preset.sortOrder,
                    "updatedAt" to preset.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSinglePresetInternal(presetId: String, presetName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/monument_size_presets/$presetId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete monument_size_presets/$presetId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete preset error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("monument_size_presets")?.document(presetId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("monument_size_presets")?.whereEqualTo("id", presetId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!presetName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("monument_size_presets")?.whereEqualTo("name", presetName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSingleServicePriceInternal(item: ConstructorServicePriceItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/constructor_prices/${item.key}?key=$apiKey"
            val doc = constructorToRestDoc(item)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("constructor_prices")?.document(item.key)?.set(mapOf(
                        "key" to item.key,
                        "groupName" to item.groupName,
                        "title" to item.title,
                        "unit" to item.unit,
                        "price" to item.price,
                        "description" to item.description,
                        "sortOrder" to item.sortOrder,
                        "updatedAt" to item.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write service price failed: ${ex.message}")
        }

        return try {
            firestore?.collection("constructor_prices")?.document(item.key)
                ?.set(mapOf(
                    "key" to item.key,
                    "groupName" to item.groupName,
                    "title" to item.title,
                    "unit" to item.unit,
                    "price" to item.price,
                    "description" to item.description,
                    "sortOrder" to item.sortOrder,
                    "updatedAt" to item.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSingleServicePriceInternal(key: String, title: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/constructor_prices/$key?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete constructor_prices/$key ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete service price error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("constructor_prices")?.document(key)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("constructor_prices")?.whereEqualTo("key", key)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!title.isNullOrBlank()) {
            try {
                val titleSnapshot = firestore?.collection("constructor_prices")?.whereEqualTo("title", title)?.get()?.await()
                titleSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSingleFontInternal(font: EngravingFontItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/engraving_fonts/${font.id}?key=$apiKey"
            val doc = fontToRestDoc(font)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("engraving_fonts")?.document(font.id)?.set(mapOf(
                        "id" to font.id,
                        "name" to font.name,
                        "styleKey" to font.styleKey,
                        "price" to font.price,
                        "sampleText" to font.sampleText,
                        "description" to font.description,
                        "isCustom" to font.isCustom,
                        "isEnabled" to font.isEnabled,
                        "sortOrder" to font.sortOrder,
                        "updatedAt" to font.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write font failed: ${ex.message}")
        }

        return try {
            firestore?.collection("engraving_fonts")?.document(font.id)
                ?.set(mapOf(
                    "id" to font.id,
                    "name" to font.name,
                    "styleKey" to font.styleKey,
                    "price" to font.price,
                    "sampleText" to font.sampleText,
                    "description" to font.description,
                    "isCustom" to font.isCustom,
                    "isEnabled" to font.isEnabled,
                    "sortOrder" to font.sortOrder,
                    "updatedAt" to font.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSingleFontInternal(fontId: String, fontName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/engraving_fonts/$fontId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete engraving_fonts/$fontId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete font error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("engraving_fonts")?.document(fontId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("engraving_fonts")?.whereEqualTo("id", fontId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!fontName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("engraving_fonts")?.whereEqualTo("name", fontName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSingleDrawingInternal(drawing: EngravingDrawingItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/engraving_drawings/${drawing.id}?key=$apiKey"
            val doc = drawingToRestDoc(drawing)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("engraving_drawings")?.document(drawing.id)?.set(mapOf(
                        "id" to drawing.id,
                        "category" to drawing.category,
                        "code" to drawing.code,
                        "name" to drawing.name,
                        "price" to drawing.price,
                        "description" to drawing.description,
                        "isCustom" to drawing.isCustom,
                        "isEnabled" to drawing.isEnabled,
                        "sortOrder" to drawing.sortOrder,
                        "updatedAt" to drawing.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write drawing failed: ${ex.message}")
        }

        return try {
            firestore?.collection("engraving_drawings")?.document(drawing.id)
                ?.set(mapOf(
                    "id" to drawing.id,
                    "category" to drawing.category,
                    "code" to drawing.code,
                    "name" to drawing.name,
                    "price" to drawing.price,
                    "description" to drawing.description,
                    "isCustom" to drawing.isCustom,
                    "isEnabled" to drawing.isEnabled,
                    "sortOrder" to drawing.sortOrder,
                    "updatedAt" to drawing.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSingleDrawingInternal(drawingId: String, drawingName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/engraving_drawings/$drawingId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete engraving_drawings/$drawingId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete drawing error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("engraving_drawings")?.document(drawingId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("engraving_drawings")?.whereEqualTo("id", drawingId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!drawingName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("engraving_drawings")?.whereEqualTo("name", drawingName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSinglePhotoSizeInternal(item: PhotoSizeItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/photo_sizes/${item.id}?key=$apiKey"
            val doc = photoSizeToRestDoc(item)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("photo_sizes")?.document(item.id)?.set(mapOf(
                        "id" to item.id,
                        "category" to item.category,
                        "sizeName" to item.sizeName,
                        "price" to item.price,
                        "description" to item.description,
                        "isCustom" to item.isCustom,
                        "isEnabled" to item.isEnabled,
                        "sortOrder" to item.sortOrder,
                        "updatedAt" to item.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write photo size failed: ${ex.message}")
        }

        return try {
            firestore?.collection("photo_sizes")?.document(item.id)
                ?.set(mapOf(
                    "id" to item.id,
                    "category" to item.category,
                    "sizeName" to item.sizeName,
                    "price" to item.price,
                    "description" to item.description,
                    "isCustom" to item.isCustom,
                    "isEnabled" to item.isEnabled,
                    "sortOrder" to item.sortOrder,
                    "updatedAt" to item.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSinglePhotoSizeInternal(itemId: String, sizeName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/photo_sizes/$itemId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete photo_sizes/$itemId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete photo size error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("photo_sizes")?.document(itemId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("photo_sizes")?.whereEqualTo("id", itemId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!sizeName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("photo_sizes")?.whereEqualTo("sizeName", sizeName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSinglePhotoFrameInternal(item: PhotoFrameItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/photo_frames/${item.id}?key=$apiKey"
            val doc = photoFrameToRestDoc(item)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("photo_frames")?.document(item.id)?.set(mapOf(
                        "id" to item.id,
                        "name" to item.name,
                        "materialType" to item.materialType,
                        "price" to item.price,
                        "description" to item.description,
                        "isCustom" to item.isCustom,
                        "isEnabled" to item.isEnabled,
                        "sortOrder" to item.sortOrder,
                        "updatedAt" to item.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write photo frame failed: ${ex.message}")
        }

        return try {
            firestore?.collection("photo_frames")?.document(item.id)
                ?.set(mapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "materialType" to item.materialType,
                    "price" to item.price,
                    "description" to item.description,
                    "isCustom" to item.isCustom,
                    "isEnabled" to item.isEnabled,
                    "sortOrder" to item.sortOrder,
                    "updatedAt" to item.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSinglePhotoFrameInternal(itemId: String, frameName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/photo_frames/$itemId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete photo_frames/$itemId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete photo frame error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("photo_frames")?.document(itemId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("photo_frames")?.whereEqualTo("id", itemId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!frameName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("photo_frames")?.whereEqualTo("name", frameName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    private suspend fun pushSingleVaseInternal(vase: VaseItem): Boolean {
        try {
            val url = "${getRestBaseUrl()}/vases/${vase.id}?key=$apiKey"
            val doc = vaseToRestDoc(vase)
            val request = createRestRequestBuilder(url).patch(doc.toString().toRequestBody(jsonMediaType)).build()
            val ok = httpClient.newCall(request).execute().use { resp -> resp.isSuccessful }
            if (ok) {
                try {
                    firestore?.collection("vases")?.document(vase.id)?.set(mapOf(
                        "id" to vase.id,
                        "name" to vase.name,
                        "materialType" to vase.materialType,
                        "sizeCm" to vase.sizeCm,
                        "price" to vase.price,
                        "description" to vase.description,
                        "isCustom" to vase.isCustom,
                        "isEnabled" to vase.isEnabled,
                        "sortOrder" to vase.sortOrder,
                        "updatedAt" to vase.updatedAt
                    ), SetOptions.merge())
                } catch (_: Exception) {}
                return true
            }
        } catch (ex: Exception) {
            Log.w(TAG, "REST write vase failed: ${ex.message}")
        }

        return try {
            firestore?.collection("vases")?.document(vase.id)
                ?.set(mapOf(
                    "id" to vase.id,
                    "name" to vase.name,
                    "materialType" to vase.materialType,
                    "sizeCm" to vase.sizeCm,
                    "price" to vase.price,
                    "description" to vase.description,
                    "isCustom" to vase.isCustom,
                    "isEnabled" to vase.isEnabled,
                    "sortOrder" to vase.sortOrder,
                    "updatedAt" to vase.updatedAt
                ), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteSingleVaseInternal(vaseId: String, vaseName: String? = null): Boolean {
        val databasesToTry = listOf(activeDbName, if (activeDbName == "(default)") "default" else "(default)").distinct()
        for (dbName in databasesToTry) {
            try {
                val url = "${getRestBaseUrl(dbName)}/vases/$vaseId?key=$apiKey"
                val request = createRestRequestBuilder(url).delete().build()
                httpClient.newCall(request).execute().use { resp ->
                    Log.d(TAG, "REST delete vases/$vaseId ($dbName) returned HTTP ${resp.code}")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "REST delete vase error ($dbName): ${ex.message}")
            }
        }

        try {
            firestore?.collection("vases")?.document(vaseId)?.delete()?.await()
        } catch (_: Exception) {}

        try {
            val snapshot = firestore?.collection("vases")?.whereEqualTo("id", vaseId)?.get()?.await()
            snapshot?.documents?.forEach { doc ->
                runCatching { doc.reference.delete().await() }
            }
        } catch (_: Exception) {}

        if (!vaseName.isNullOrBlank()) {
            try {
                val nameSnapshot = firestore?.collection("vases")?.whereEqualTo("name", vaseName)?.get()?.await()
                nameSnapshot?.documents?.forEach { doc ->
                    runCatching { doc.reference.delete().await() }
                }
            } catch (_: Exception) {}
        }

        return true
    }

    // REST serialisers
    private fun String?.toFsString(): JSONObject = JSONObject().put("stringValue", this ?: "")
    private fun Double?.toFsDouble(): JSONObject = JSONObject().put("doubleValue", this ?: 0.0)
    private fun Long?.toFsInt(): JSONObject = JSONObject().put("integerValue", (this ?: 0L).toString())
    private fun Int?.toFsInt(): JSONObject = JSONObject().put("integerValue", (this ?: 0).toString())
    private fun Boolean?.toFsBool(): JSONObject = JSONObject().put("booleanValue", this ?: false)

    private fun orderToRestDoc(order: SavedOrder): JSONObject {
        val fields = JSONObject()
        fields.put("id", order.id.toFsString())
        fields.put("orderNumber", order.orderNumber.toFsString())
        fields.put("clientName", order.clientName.toFsString())
        fields.put("clientPhone", order.clientPhone.toFsString())
        fields.put("deceasedName", order.deceasedName.toFsString())
        fields.put("cemeteryName", order.cemeteryName.toFsString())
        fields.put("plotNumber", order.plotNumber.toFsString())
        fields.put("createdAt", order.createdAt.toFsInt())
        fields.put("updatedAt", order.updatedAt.toFsInt())
        fields.put("status", order.status.toFsString())
        fields.put("subtotalAmount", order.subtotalAmount.toFsDouble())
        fields.put("discountPercent", order.discountPercent.toFsDouble())
        fields.put("discountAmount", order.discountAmount.toFsDouble())
        fields.put("totalAmount", order.totalAmount.toFsDouble())
        fields.put("prepaymentAmount", order.prepaymentAmount.toFsDouble())
        fields.put("remainingAmount", order.remainingAmount.toFsDouble())
        fields.put("itemsJson", order.itemsJson.toFsString())
        fields.put("notes", order.notes.toFsString())
        return JSONObject().put("fields", fields)
    }

    private fun priceToRestDoc(item: PriceItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", item.id.toFsInt())
        fields.put("category", item.category.toFsString())
        fields.put("subcategory", item.subcategory.toFsString())
        fields.put("name", item.name.toFsString())
        fields.put("unit", item.unit.toFsString())
        fields.put("defaultPrice", item.defaultPrice.toFsDouble())
        fields.put("currentPrice", item.currentPrice.toFsDouble())
        fields.put("description", item.description.toFsString())
        fields.put("isCustom", item.isCustom.toFsBool())
        fields.put("isEnabled", item.isEnabled.toFsBool())
        fields.put("sortOrder", item.sortOrder.toFsInt())
        fields.put("updatedAt", item.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun stoneToRestDoc(mat: StoneMaterialItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", mat.id.toFsString())
        fields.put("name", mat.name.toFsString())
        fields.put("colorName", mat.colorName.toFsString())
        fields.put("pricePerM3", mat.pricePerM3.toFsDouble())
        fields.put("origin", mat.origin.toFsString())
        fields.put("description", mat.description.toFsString())
        fields.put("suitableForDirectEngraving", mat.suitableForDirectEngraving.toFsBool())
        fields.put("isCustom", mat.isCustom.toFsBool())
        fields.put("isEnabled", mat.isEnabled.toFsBool())
        fields.put("sortOrder", mat.sortOrder.toFsInt())
        fields.put("updatedAt", mat.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun presetToRestDoc(p: MonumentSizePresetItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", p.id.toFsString())
        fields.put("name", p.name.toFsString())
        fields.put("heightCm", p.heightCm.toFsInt())
        fields.put("widthCm", p.widthCm.toFsInt())
        fields.put("thicknessCm", p.thicknessCm.toFsInt())
        fields.put("steleBasePrice", p.steleBasePrice.toFsDouble())
        fields.put("plinthBasePrice", p.plinthBasePrice.toFsDouble())
        fields.put("flowerbedBasePrice", p.flowerbedBasePrice.toFsDouble())
        fields.put("plinthDimensions", p.plinthDimensions.toFsString())
        fields.put("flowerbedDimensions", p.flowerbedDimensions.toFsString())
        fields.put("isFamily", p.isFamily.toFsBool())
        fields.put("isCustom", p.isCustom.toFsBool())
        fields.put("isEnabled", p.isEnabled.toFsBool())
        fields.put("sortOrder", p.sortOrder.toFsInt())
        fields.put("updatedAt", p.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun constructorToRestDoc(item: ConstructorServicePriceItem): JSONObject {
        val fields = JSONObject()
        fields.put("key", item.key.toFsString())
        fields.put("groupName", item.groupName.toFsString())
        fields.put("title", item.title.toFsString())
        fields.put("unit", item.unit.toFsString())
        fields.put("price", item.price.toFsDouble())
        fields.put("description", item.description.toFsString())
        fields.put("sortOrder", item.sortOrder.toFsInt())
        fields.put("updatedAt", item.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun fontToRestDoc(font: EngravingFontItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", font.id.toFsString())
        fields.put("name", font.name.toFsString())
        fields.put("styleKey", font.styleKey.toFsString())
        fields.put("price", font.price.toFsDouble())
        fields.put("sampleText", font.sampleText.toFsString())
        fields.put("description", font.description.toFsString())
        fields.put("isCustom", font.isCustom.toFsBool())
        fields.put("isEnabled", font.isEnabled.toFsBool())
        fields.put("sortOrder", font.sortOrder.toFsInt())
        fields.put("updatedAt", font.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun drawingToRestDoc(drawing: EngravingDrawingItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", drawing.id.toFsString())
        fields.put("category", drawing.category.toFsString())
        fields.put("code", drawing.code.toFsString())
        fields.put("name", drawing.name.toFsString())
        fields.put("price", drawing.price.toFsDouble())
        fields.put("description", drawing.description.toFsString())
        fields.put("isCustom", drawing.isCustom.toFsBool())
        fields.put("isEnabled", drawing.isEnabled.toFsBool())
        fields.put("sortOrder", drawing.sortOrder.toFsInt())
        fields.put("updatedAt", drawing.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun photoSizeToRestDoc(item: PhotoSizeItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", item.id.toFsString())
        fields.put("category", item.category.toFsString())
        fields.put("sizeName", item.sizeName.toFsString())
        fields.put("price", item.price.toFsDouble())
        fields.put("description", item.description.toFsString())
        fields.put("isCustom", item.isCustom.toFsBool())
        fields.put("isEnabled", item.isEnabled.toFsBool())
        fields.put("sortOrder", item.sortOrder.toFsInt())
        fields.put("updatedAt", item.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun photoFrameToRestDoc(item: PhotoFrameItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", item.id.toFsString())
        fields.put("name", item.name.toFsString())
        fields.put("materialType", item.materialType.toFsString())
        fields.put("price", item.price.toFsDouble())
        fields.put("description", item.description.toFsString())
        fields.put("isCustom", item.isCustom.toFsBool())
        fields.put("isEnabled", item.isEnabled.toFsBool())
        fields.put("sortOrder", item.sortOrder.toFsInt())
        fields.put("updatedAt", item.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    private fun vaseToRestDoc(vase: VaseItem): JSONObject {
        val fields = JSONObject()
        fields.put("id", vase.id.toFsString())
        fields.put("name", vase.name.toFsString())
        fields.put("materialType", vase.materialType.toFsString())
        fields.put("sizeCm", vase.sizeCm.toFsString())
        fields.put("price", vase.price.toFsDouble())
        fields.put("description", vase.description.toFsString())
        fields.put("isCustom", vase.isCustom.toFsBool())
        fields.put("isEnabled", vase.isEnabled.toFsBool())
        fields.put("sortOrder", vase.sortOrder.toFsInt())
        fields.put("updatedAt", vase.updatedAt.toFsInt())
        return JSONObject().put("fields", fields)
    }

    companion object {
        private const val TAG = "FirebaseSyncManager"
    }
}

// Helpers for extracting field values from DocumentSnapshot
private fun DocumentSnapshot.extractString(vararg keys: String, default: String = ""): String {
    for (k in keys) {
        if (contains(k)) {
            val v = getString(k)
            if (v != null) return v
        }
    }
    return default
}

private fun DocumentSnapshot.extractDouble(vararg keys: String, default: Double = 0.0): Double {
    for (k in keys) {
        if (contains(k)) {
            val d = getDouble(k)
            if (d != null) return d
            val l = getLong(k)
            if (l != null) return l.toDouble()
            val s = getString(k)
            if (s != null) {
                val parsed = s.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }
    }
    return default
}

private fun DocumentSnapshot.extractLong(vararg keys: String, default: Long = 0L): Long {
    for (k in keys) {
        if (contains(k)) {
            val l = getLong(k)
            if (l != null) return l
            val s = getString(k)
            if (s != null) {
                val parsed = s.toLongOrNull()
                if (parsed != null) return parsed
            }
        }
    }
    return default
}

private fun DocumentSnapshot.extractInt(vararg keys: String, default: Int = 0): Int {
    for (k in keys) {
        if (contains(k)) {
            val l = getLong(k)
            if (l != null) return l.toInt()
            val s = getString(k)
            if (s != null) {
                val parsed = s.toIntOrNull()
                if (parsed != null) return parsed
            }
        }
    }
    return default
}

private fun DocumentSnapshot.extractBool(vararg keys: String, default: Boolean = false): Boolean {
    for (k in keys) {
        if (contains(k)) {
            val b = getBoolean(k)
            if (b != null) return b
        }
    }
    return default
}
