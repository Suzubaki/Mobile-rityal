# Техническая документация проекта «Ритуал Калькулятор»

Данный документ содержит полное и точное описание архитектуры, конфигурации, модулей, классов, функций, математических формул и бизнес-логики Android-приложения **«Ритуал Калькулятор»**.

---

## Оглавление
1. [Общие сведения и конфигурация проекта](#1-общие-сведения-и-конфигурация-проекта)
2. [Архитектура, точка входа, тема и навигация](#2-архитектура-точка-входа-тема-и-навигация)
3. [Слой данных (Room, сущности, DAO, репозиторий, каталог по умолчанию)](#3-слой-данных-room-сущности-dao-репозиторий-каталог-по-умолчанию)
4. [Модели расчётов и конструктора памятников](#4-модели-расчётов-и-конструктора-памятников)
5. [Управление состоянием и бизнес-логика (`RitualViewModel`)](#5-управление-состоянием-и-бизнес-логика-ritualviewmodel)
6. [Сетевой слой и мониторинг подключения](#6-сетевой-слой-и-мониторинг-подключения)
7. [Облачная синхронизация и аутентификация (`FirebaseSyncManager`, `GoogleSignInHelper`)](#7-облачная-синхронизация-и-аутентификация)
8. [Математическая и конструкторская логика (`MonumentBuilderDialog`, `DimensionCalculatorsDialog`)](#8-математическая-и-конструкторская-логика)
9. [Формирование документов и экспорт (Договор клиента и Наряд для завода)](#9-формирование-документов-и-экспорт)
10. [Пользовательский интерфейс: экраны и диалоги](#10-пользовательский-интерфейс-экраны-и-диалоги)

---

## 1. Общие сведения и конфигурация проекта

### 1.1. Идентификаторы и параметры сборки (`app/build.gradle.kts`, `gradle/libs.versions.toml`)
- **Название приложения (`strings.xml`, `settings.gradle.kts`):** `Ритуал Калькулятор` (`rootProject.name = "RitualCalc"`).
- **Application ID:** `com.aistudio.ritualcalc.mvmord`
- **Namespace:** `com.example`
- **SDK:** `minSdk = 24`, `compileSdk = 36` (`minorApiLevel = 1`), `targetSdk = 36`.
- **Java / JVM Target:** Java 11.
- **Основные плагины:**
  - Android Gradle Plugin `9.1.1`, Kotlin `2.2.10`, KSP `2.3.5`
  - Compose Compiler (`kotlin-compose`), `kotlin-serialization`
  - `com.google.gms.google-services` (`4.5.0`)
  - `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` (`2.0.1`, читает `.env` / `.env.example`)
- **Ключевые зависимости:**
  - **UI:** Jetpack Compose BOM `2024.09.00`, Material 3, `material-icons-extended`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, `navigation-compose` (`2.8.9`).
  - **Локальная БД:** Room `2.7.0` (`room-runtime`, `room-ktx`, `room-compiler` через KSP).
  - **Сеть и сериализация:** OkHttp `4.10.0`, Retrofit `2.12.0`, Moshi `1.15.2` (`moshi-kotlin`, `converter-moshi`), `kotlinx-serialization-json` `1.8.0`.
  - **Firebase и авторизация:** Firebase BOM `34.17.0` (`firebase-firestore`, `firebase-auth`, `firebase-ai`, `firebase-appcheck-recaptcha`), Jetpack Credential Manager `1.5.0` (`credentials`, `credentials-play-services-auth`, `googleid` `1.1.1`), `kotlinx-coroutines-play-services`.

### 1.2. Манифест и файловый провайдер (`AndroidManifest.xml`, `res/xml/file_paths.xml`)
- **Разрешения:** `android.permission.INTERNET`.
- **Сетевой режим:** `android:usesCleartextTraffic="true"`.
- **Стартовая активность:** `com.example.MainActivity` (`windowSoftInputMode="adjustResize"`, `launchMode="singleTop"`).
- **FileProvider (`${applicationId}.fileprovider`):** предоставляет доступ к внутреннему кэшу (`cacheDir`, путь `"shared_files/"`) и внешнему кэшу (`external-cache-path`) для открытия и отправки `.docx` и `.json` файлов.

---

## 2. Архитектура, точка входа, тема и навигация

Приложение построено по паттерну **MVVM** (Jetpack Compose UI -> `RitualViewModel` -> `RitualRepository` -> `AppDatabase` (Room) + `FirebaseSyncManager` (Firestore) + `NbrbCurrencyFetcher`).

### 2.1. Точка входа (`MainActivity.kt`, `MainScreen.kt`)
- `MainActivity : ComponentActivity`:
  - Вызывает `enableEdgeToEdge()`.
  - Создаёт `RitualViewModel` через `by viewModels()`.
  - Оборачивает контент в `RitualCalculatorTheme { Surface { MainAppScreen(viewModel = viewModel) } }`.
- `MainScreen()` (`ui/MainScreen.kt`) — вспомогательный Composable-обёртка, делегирующий отображение в `MainAppScreen()`.

### 2.2. Адаптивная оболочка навигации (`ui/screens/MainAppScreen.kt`)
- **`enum class AppScreen(val title: String, val icon: ImageVector)`:**
  1. `CALCULATOR` — «Калькулятор»
  2. `PRICE_SETTINGS` — «Прайс-лист»
  3. `SAVED_ORDERS` — «Сметы»
  4. `CLOUD_SETTINGS` — «Настройки»
- **`MainAppScreen(viewModel: RitualViewModel)`:**
  - Подписывается на `currentScreen`, `calculationState`, `activePrices`, `allPrices`, `savedOrders`, `priceSearchQuery`, `selectedPriceCategory`, `isOnline`, `syncState`, `userMessage`.
  - Через `LaunchedEffect(Unit)` отслеживает `viewModel.userMessage` и выводит `SnackbarHostState.showSnackbar(...)`.
  - Использует `BoxWithConstraints` для адаптивности: при `maxWidth >= 720.dp` (`isWideScreen`) отображает боковую панель `NavigationRail`, на узких экранах — нижний `NavigationBar`. Основной контент ограничивается `Modifier.widthIn(max = 1200.dp)` по центру.
  - В `TopAppBar` выводит название активного экрана, индикатор облачной синхронизации (`SyncStatusIcon`: зелёное облако / анимация / ошибка) и кликабельный чип статуса сети (`"Онлайн ☁️"` / `"Офлайн 📱"`), нажатие на который вызывает `viewModel.syncPendingChanges()`.
- **`SyncStatusIcon(syncState: SyncState)`:** приватный Composable индикации состояния `SyncState` в верхней панели.

### 2.3. Тема и оформление (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`)
- **`Color.kt`:** палитра «Гранит и сусальное золото»: тёмный фон `SurfaceDark` (`#13161A`), `SurfaceVariantDark` (`#1E232A`), светлый фон `SurfaceLight` (`#F6F8FA`), акцентное золото `GoldAccent` (`#D4AF37`), `GoldLight` (`#F3D570`), `GoldDark` (`#A6841A`), светлый primary `#8B6508`, а также семантические цвета статусов (`StatusGreen`, `StatusOrange`, `StatusRed`).
- **`Theme.kt`:** `RitualCalculatorTheme` (и псевдоним `MyApplicationTheme`) на базе Material 3 (`LightColorScheme` / `DarkColorScheme`), настраивает цвет `statusBarColor` и флаг `isAppearanceLightStatusBars`.
- **`Type.kt`:** типографика Material 3 (`Typography`).

---

## 3. Слой данных (Room, сущности, DAO, репозиторий, каталог по умолчанию)

### 3.1. Сущности и перечисления (`data/Entities.kt`)

#### Перечисления
- **`enum class SyncStatus`:** `SYNCED`, `PENDING_PUSH`, `PENDING_DELETE`, `CONFLICT`.
- **`enum class ItemCategory(val displayName: String)`:**
  - `MONUMENTS` («Памятники»)
  - `ENGRAVING` («Гравировка»)
  - `FENCES_GROUND` («Ограды и плитка»)
  - `BURIAL_SERVICES` («Захоронение»)
  - `OTHER` («Прочее»)

#### Таблицы Room
Все синхронизируемые таблицы содержат поля `syncStatus: String = SyncStatus.SYNCED.name` и `updatedAt: Long = System.currentTimeMillis()`.

1. **`PriceItem` (`@Entity("price_items")`)** — прайс-лист товаров/услуг:
   - `id: Int` (autoGenerate), `category`, `subcategory`, `name`, `unit`, `defaultPrice: Double`, `currentPrice: Double`, `description`, `isCustom: Boolean`, `isEnabled: Boolean`.
2. **`SavedOrder` (`@Entity("saved_orders")`)** — сохранённая смета/заказ:
   - Клиентские и итоговые поля: `id: Int`, `orderNumber`, `createdAt`, `clientName`, `clientPhone`, `deceasedName`, `deceasedBirthDate`, `deceasedDeathDate`, `cemeteryName`, `plotNumber`, `itemsJson` (JSON списка `CalculatedItemData`), `subtotalAmount`, `discountPercent`, `discountAmount`, `totalAmount`, `prepaymentAmount`, `remainingAmount`, `notes`, `status` (`DRAFT`, `IN_PROGRESS`, `READY`, `PAID`, `CANCELLED`).
   - Поля для наряда завода: `deceasedLastName`, `deceasedFirstName`, `deceasedMiddleName`, `crossInfo`, `photoVignetteInfo`, `frameInfo`, `epitaphText`, `plateDecoration`, `additionsInfo`, `monumentMaterial`, `obeliskInfo`, `plinthInfo`, `flowerbedInfo`, `slabInfo`, `vaseInfo`, `dismantlingInfo`.
3. **`StoneMaterialItem` (`@Entity("stone_materials")`)** — породы камня для конструктора:
   - `id: String` (PK), `name`, `colorName`, `pricePerM3: Double` (цена 1 м³ в BYN), `origin`, `description`, `suitableForDirectEngraving: Boolean`, `isCustom`, `isEnabled`, `sortOrder`.
4. **`MonumentSizePresetItem` (`@Entity("monument_size_presets")`)** — типоразмеры комплектов:
   - `id: String` (PK), `categoryName`, `name`, `steleH`, `steleW`, `steleT`, `plinthL`, `plinthW`, `plinthH`, `flowerbedL`, `flowerbedW`, `flowerbedT`, `description`, `isCustom`, `isEnabled`, `sortOrder`.
5. **`ConstructorServicePriceItem` (`@Entity("constructor_service_prices")`)** — тарифы конструктора и курсы валют:
   - `key: String` (PK), `title`, `price: Double`, `unit`, `description`.
6. **`EngravingFontItem` (`@Entity("engraving_fonts")`)** — шрифты гравировки:
   - `id: String` (PK), `name`, `styleKey`, `price: Double`, `sampleText`, `description`, `isEnabled`, `sortOrder`.
7. **`EngravingDrawingItem` (`@Entity("engraving_drawings")`)** — рисунки и декор:
   - `id: String` (PK), `category` («Крест», «Цветок», «Иконы», «Ангелы», «Свечи» и др.), `code` (`К-1`, `Ц-1`...), `name`, `price: Double`, `description`, `isEnabled`, `sortOrder`.
8. **`PhotoSizeItem` (`@Entity("photo_size_items")`)** — размеры и виды фото/портретов:
   - `id: String` (PK), `category`, `sizeName`, `price: Double`, `description`, `isEnabled`, `sortOrder`.
9. **`PhotoFrameItem` (`@Entity("photo_frame_items")`)** — рамки и врезка:
   - `id: String` (PK), `name`, `materialType`, `price: Double`, `description`, `isEnabled`, `sortOrder`.
10. **`VaseItem` (`@Entity("vase_items")`)** — вазы и аксессуары:
    - `id: String` (PK), `name`, `materialType`, `sizeCm`, `price: Double`, `description`, `isEnabled`, `sortOrder`.

### 3.2. DAO-интерфейсы (`data/Daos.kt`)
Для каждой сущности определён DAO:
- `PriceDao`: `getAllPrices()`, `getEnabledPrices()`, `getPriceById()`, `getPendingPushPrices()`, `getPendingDeletePrices()`, `insertPrice()`, `insertPrices()`, `updatePrice()`, `deletePrice()`, `deleteById()`, `getCount()`, `resetAllPricesToDefault()`, `deleteUnlinkedDuplicateServiceItems()`, `deleteAllPrices()`.
- `OrderDao`: `getAllOrders()` (сортировка `ORDER BY createdAt DESC`), `getOrderById()`, `getOrderByNumber()`, `getPendingPushOrders()`, `getPendingDeleteOrders()`, `insertOrder()`, `updateOrder()`, `deleteOrder()`, `deleteOrderById()`, `deleteAllOrders()`.
- `StoneMaterialDao`, `MonumentSizePresetDao`, `ConstructorServicePriceDao`, `EngravingFontDao`, `EngravingDrawingDao`, `PhotoSizeDao`, `PhotoFrameDao`, `VaseDao`: предоставляют `Flow` активных и всех записей, методы чтения ожидающих синхронизации (`getPendingPush*`, `getPendingDelete*`), `insert*` (`OnConflictStrategy.REPLACE`), `update*`, `delete*`, `deleteById`, `getCount`, `deleteAll*`.

### 3.3. База данных и репозиторий (`data/AppDatabase.kt`)
- **`AppDatabase : RoomDatabase` (`version = 8`, имя БД `ritual_calc_database.db`):**
  - Содержит `MIGRATION_3_4` (добавляет колонки `syncStatus`, `updatedAt` и таблицу `constructor_service_prices`) и `.fallbackToDestructiveMigration()` для старших миграций.
  - Singleton `getDatabase(context)` с `synchronized`.
- **`RitualRepository`:**
  - Агрегирует 10 DAO и предоставляет реактивные `Flow` (`allPrices`, `activePrices`, `allOrders`, `allStoneMaterials`, `activeStoneMaterials`, `allSizePresets`, `activeSizePresets`, `allConstructorServicePrices`, `allEngravingFonts`, `activeEngravingFonts`, `allEngravingDrawings`, `activeEngravingDrawings`, `allPhotoSizes`, `activePhotoSizes`, `allPhotoFrames`, `activePhotoFrames`, `allVases`, `activeVases`).
  - **`ensureDefaultDataLoaded()`:**
    1. Очищает устаревшие дубли услуг уборки (`deleteUnlinkedDuplicateServiceItems()`).
    2. Запускает `deduplicateLocalPriceItems()` — группирует `price_items` по `name.trim().lowercase()`, оставляет запись с `id in 1..44` или наименьшим `id`, остальные удаляет.
    3. Заполняет отсутствующие ключи из `DefaultCatalog.getDefaultConstructorServicePrices()`.
    4. Если таблицы `engraving_fonts`, `engraving_drawings`, `photo_size_items`, `photo_frame_items`, `vase_items` пусты — заполняет их из `DefaultCatalog`.
  - Предоставляет методы CRUD, локального кэширования из Firestore (`save*ToLocalCacheOnly` — записывает со статусом `SYNCED`), выборки несинхронизированных записей (`getPendingPush*`, `getPendingDelete*`), сброса (`resetPricesToDefaults`, `resetMaterialsToDefaults`, `resetPresetsToDefaults`, `resetServicePricesToDefaults`) и полной очистки (`clearAllRoomData`).

### 3.4. Начальный каталог (`data/DefaultCatalog.kt`)
`object DefaultCatalog` (и `typealias DefaultPriceCatalog = DefaultCatalog`):
- `getItems()`, `getDefaultItems()`, `getDefaultStoneMaterials()`, `getDefaultSizePresets()` возвращают `emptyList()` (каталог товаров, камней и пресетов наполняется пользователем или синхронизируется из Firestore).
- `getDefaultEngravingFonts()`: 7 шрифтов (`Академический (Засечки)`, `Стандартный Рубленый`, `Прописной Курсив`, `Рукописный Каллиграфический`, `Монументальный Полужирный`, `Славянский / Стилизованный`, `Тонкий Классический`) по `0.0 BYN`.
- `getDefaultEngravingDrawings()`: 25 позиций по 5 категориям с ценами от `10.0` до `150.0 BYN`: Кресты (`К-1..К-5`), Цветы (`Ц-1..Ц-5`), Иконы (`И-1..И-5`), Ангелы (`А-1..А-5`), Свечи (`С-1..С-5`).
- `getDefaultConstructorServicePrices()`: 15 тарифов:
  - `usd_exchange_rate` = `3.25`
  - `eur_exchange_rate` = `3.55`
  - `letter_standard` = `1.60` BYN/знак
  - `letter_painted` = `10.00` BYN/знак
  - `letter_gold_large` = `8.00` EUR/знак
  - `letter_gold_small` = `7.00` EUR/знак
  - `letter_gold` = `5.50` BYN/знак (устаревший базовый ключ, оставлен для совместимости)
  - `letter_epitaph` = `1.50` BYN/знак
  - `measure_delivery_base_price` = `60.0` BYN
  - `measure_delivery_base_km` = `15.0` км
  - `measure_delivery_km_price` = `1.50` BYN/км
  - `measure_fence_price_pm` = `45.0` BYN/м.п.
  - `measure_tile_price_sqm` = `85.0` BYN/м²
  - `installation_base_price` = `500.0` BYN
  - `demontazh_base_price` = `150.0` BYN
- `getDefaultPhotoSizes()`: 14 форматов (Фотокерамика, Металлокерамика, Фото на стекле, Гравировка портрета) с ценами `95.0..650.0 BYN`.
- `getDefaultPhotoFrames()`: 5 вариантов (`Без рамки` 0, `Бронзовая рамка (Caggiati)` 180, `Алюминиевая рамка (Золото/Серебро)` 90, `Гранитная рамка (объёмная)` 220, `Врезка (ниша в стеле с фаской)` 120).
- `getDefaultVases()`: 9 ваз (`Гранит` 120..220 BYN и `Кованый металл` 140..190 BYN).

---

## 4. Модели расчётов и конструктора памятников

### 4.1. Модели сметы и форматирования (`data/CalculationModels.kt`)
- **`@JsonClass(generateAdapter = true) data class CalculatedItemData`:**
  - Поля: `priceItemId`, `category`, `name`, `unit`, `unitPrice`, `quantity`, `totalPrice`, `customNote`.
  - Вычисляемое свойство `val subtotal: Double get() = unitPrice * quantity`.
- **`object FormatUtils`:** использует локаль `ru-RU`, `DecimalFormat("#,##0.##")`:
  - `formatPrice(price)` / `formatRub(price)` / `formatByn(price)` -> `"1 250,5 BYN"`
  - `formatBynAndUsd(priceByn, usdRate)` -> `"325 BYN (100 $)"` (при `usdRate > 0`)
  - `formatEur(priceEur)` -> `"16 €"`
  - `formatBynAndEur(priceEur, eurRate)` -> `"16 € (56,80 BYN)"`
  - `formatNumber(number)`, `formatDate(timestamp)` (`dd.MM.yyyy HH:mm`), `formatDateShort(timestamp)` (`dd.MM.yyyy`).
- **`object PriceFormatter`:** обёртка над `FormatUtils` (`formatRub`, `formatWithUsd`, `formatNumber`).
- **`object OrderJsonAdapter`:** сериализация/десериализация `List<CalculatedItemData>` через `Moshi` + `KotlinJsonAdapterFactory`.

### 4.2. Модели конструктора (`data/MonumentBuilderModels.kt`)
- **`data class StoneMaterial`** и **`data class MonumentSizePreset`** с расширениями конвертации `.toModel()` и `.toEntity()` между Room-сущностями и UI-моделями.
- **Справочные перечисления (`PolishingType`, `SteleShape`, `ChamferType`, `FlowerbedType`, `PlinthType`, `PortraitType`, `InstallationType`):** определяют варианты обработки, форм и комплектации (в текущем UI конструктора расчёт идёт напрямую по объёму/тарифу без мультипликаторов этих enum).
- **`object MonumentCatalogData`:** справочные списки материалов, пресетов и цен допов, синхронизируемые через БД (`emptyList()` по умолчанию для камней/пресетов).

---

## 5. Управление состоянием и бизнес-логика (`RitualViewModel`)

Файл: `app/src/main/java/com/example/ui/RitualViewModel.kt`.

### 5.1. Состояние расчёта `CalculationState`
Содержит:
- Список позиций `selectedItems: List<CalculatedItemData>`.
- Данные клиента и заказа: `orderNumber` (`"РК-yyMMdd-HHmm-XXX"`), `clientName`, `clientPhone`, `deceasedName`, `deceasedBirthDate`, `deceasedDeathDate`, `cemeteryName`, `plotNumber`, `notes`.
- Финансовые параметры: `discountPercent`, `prepayment`.
- 23 поля наряда для завода: `deceasedLastName`, `deceasedFirstName`, `deceasedMiddleName`, `crossInfo`, `photoVignetteInfo`, `frameInfo`, `epitaphText`, `plateDecoration`, `additionsInfo`, `monumentMaterial`, `obeliskInfo`, `plinthInfo`, `flowerbedInfo`, `slabInfo`, `vaseInfo`, `dismantlingInfo`.
- Вычисляемые свойства:
  - `subtotal = selectedItems.sumOf { it.totalPrice }`
  - `discountAmount = subtotal * (discountPercent / 100.0)`
  - `total = (subtotal - discountAmount).coerceAtLeast(0.0)`
  - `remainingAmount = (total - prepayment).coerceAtLeast(0.0)`

### 5.2. Инициализация (`init`) и жизненный цикл (`onCleared`)
1. Создаёт `AppDatabase`, `RitualRepository`, `FirebaseSyncManager`, `NetworkMonitor`, `NbrbCurrencyFetcher`.
2. В `viewModelScope.launch`:
   - Вызывает `repository.ensureDefaultDataLoaded()`.
   - Запускает `autoPullFromCloudOnStartup()`: через 600 мс (или мгновенно при наличии интернета) выполняет `refreshLocalCacheFromFirestore()` и запускает realtime-слушатели Firestore (`startRealtimeListeners()`).
   - Загружает актуальные курсы USD и EUR НБРБ (`refreshNbrbRates(showUserFeedback = false)`).
   - Подписывается на `networkMonitor.isOnline`: при восстановлении интернета запускает `syncPendingOfflineChanges()` и повторно запускает слушатели.
3. `onCleared()` останавливает слушатели Firestore (`syncManager.stopListeners()`) и `networkMonitor.stop()`.

### 5.3. Ключевые методы `RitualViewModel`
- **Навигация и сообщения:** `selectScreen(screen)`, `setPriceSearchQuery(query)`, `setPriceCategory(category)`, `showMessage(msg)`, `showUserMessage(msg)`, `clearMessage()`.
- **Калькулятор сметы:**
  - `addItemToCalculation(item, quantity = 1.0)` — если позиция с таким `priceItemId > 0` и `unitPrice` уже есть в списке, увеличивает количество; иначе добавляет новую.
  - `addCustomItemToCalculation(name, category, unit, unitPrice, quantity, customNote)`
  - `addMultipleItemsToCalculation(items)`
  - `updateItemQuantity(index, newQuantity)` — пересчитывает `totalPrice = item.unitPrice * newQuantity` (при `<= 0` удаляет).
  - `updateItemPrice(index, newPrice)` — пересчитывает `totalPrice = newPrice * item.quantity`.
  - `removeItemFromCalculation(index)`
  - `updateClientDetails(...)` — обновляет поля заказчика, усопшего, скидки и предоплаты.
  - `updateFactoryDetails(...)` — сохраняет детальные поля наряда завода (и синхронизирует `deceasedName` / даты при необходимости).
  - `clearCalculation()` — сбрасывает `CalculationState` с новым номером заказа.
- **Операции с БД и облаком (с проверкой интернета `isOnline`):**
  - *Обязателен онлайн (при офлайне показывают сообщение):* `saveCurrentOrder(status)`, `savePriceItem(item)`, `updatePriceItemCost(itemId, newPrice)`, `saveStoneMaterial(item)`, `saveSizePreset(item)`, `updateUsdExchangeRate(rate)`, `updateEurExchangeRate(rate)`.
  - *Гибридный режим (онлайн -> Firestore + Room `SYNCED`; офлайн -> Room `PENDING_PUSH` / `PENDING_DELETE` с автодогрузкой при появлении сети):* `deleteSavedOrder`, `loadOrderIntoCalculator`, `togglePriceItemEnabled`, `deletePriceItem`, `toggleStoneMaterialEnabled`, `deleteStoneMaterial`, `toggleSizePresetEnabled`, `deleteSizePreset`, `updateConstructorPriceByKey`, `saveConstructorServicePrice`, `saveFont`/`toggleFontEnabled`/`deleteFont`, `saveDrawing`/`toggleDrawingEnabled`/`deleteDrawing`, `savePhotoSize`/`togglePhotoSizeEnabled`/`deletePhotoSize`, `savePhotoFrame`/`togglePhotoFrameEnabled`/`deletePhotoFrame`, `saveVase`/`toggleVaseEnabled`/`deleteVase`.
- **Курсы валют и синхронизация:**
  - `refreshNbrbRates(showUserFeedback)` и `refreshNbrbUsdRate(showUserFeedback)`: запрашивают курсы USD и EUR, при успехе сохраняют их в `constructor_service_prices` (и в облако, если онлайн).
  - `testCloudConnection()`, `forceUploadLocalDataToFirestore()`, `refreshLocalCacheFromFirestore()`, `syncPendingChanges()`, `resetAllPricesToDefaults()`, `clearRoomAndFirestore()`.
- **Экспорт и резервное копирование:**
  - `shareEstimate(context)`: генерирует текстовую смету для мессенджеров и запускает `Intent.ACTION_SEND`.
  - `exportDatabaseBackup(context)`: собирает JSON (`version = 1`, `appName = "RitualCalc"`) со всеми 10 коллекциями, сохраняет во временный файл `ritual_backup_*.json` и открывает системный диалог отправки через `FileProvider`.
  - `importDatabaseBackup(jsonString)`: парсит JSON-бэкап, сохраняет записи в Room (и при наличии сети отправляет в Firestore через `uploadLocalToFirestore()`).

---

## 6. Сетевой слой и мониторинг подключения

### 6.1. `network/NbrbCurrencyFetcher.kt`
- **`data class NbrbCurrencyRates(val usdRate: Double?, val eurRate: Double?, val source: String)`**
- **`NbrbCurrencyFetcher`:** клиент `OkHttpClient` (таймауты 10 с / 12 с), загружает курсы USD и EUR к BYN по трёхступенчатой отказоустойчивой схеме (`Dispatchers.IO`):
  1. `fetchFromNbrbHtmlPage()`: парсит официальную страницу `https://www.nbrb.by/statistics/rates/ratesdaily`, ищет строки `USD`/`431` и `EUR`/`451`, извлекает числа regex `(\d{1,2}[.,]\d{2,4})` в допустимом диапазоне `(1.5..15.0)`.
  2. `fetchRateFromEndpoints()`: опрашивает официальные JSON API НБРБ (`api.nbrb.by/exrates/rates/{431|451}` и `www.nbrb.by/api/...`), парсит поле `Cur_OfficialRate`.
  3. `fetchFromFallbackApi()`: запасные публичные API (`open.er-api.com/v6/latest/{USD|EUR}` и `cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{usd|eur}.json`), извлекает курс `BYN`.
- Метод `fetchUsdToBynRate()` оставлен как обёртка, возвращающая `usdRate`.

### 6.2. `util/NetworkMonitor.kt`
- **`NetworkMonitor(context: Context)`:** использует `ConnectivityManager` и `NetworkCallback` с фильтром `NetworkCapabilities.NET_CAPABILITY_INTERNET`.
- Предоставляет `val isOnline: StateFlow<Boolean>` для мгновенного реагирования ViewModel и UI на потерю или восстановление сети.
- Метод `stop()` отменяет регистрацию callback.

### 6.3. `util/FontHelper.kt`
- **`object FontHelper`:** сопоставляет `styleKey` шрифтов гравировки (`SERIF`, `SANS`, `CURSIVE`, `MONOSPACE`, `BOLD_SERIF`, `BOLD_SANS`, `SLAVIC`) с Compose `FontFamily`, `FontWeight`, `FontStyle` и отображаемыми названиями (`getStyleDisplayName`, `getFontFamily`, `getFontWeight`, `getFontStyle`, `availableStyles`).

---

## 7. Облачная синхронизация и аутентификация

### 7.1. `data/cloud/FirebaseSyncManager.kt` (~3027 строк)
Управляет двусторонней синхронизацией между локальной базой Room и Cloud Firestore проекта `"dwa-angeld"`.

- **`sealed class SyncState`:** `Disabled`, `NotAuthenticated`, `Syncing`, `Connected(userEmail: String?)`, `Error(message: String)`.
- **Коллекции Firestore (10 шт.):**
  `orders`, `price_catalog`, `stone_materials`, `monument_size_presets`, `constructor_prices`, `engraving_fonts`, `engraving_drawings`, `photo_sizes`, `photo_frames`, `vases`.
- **Выбор ID базы данных Firestore:** в `SharedPreferences("firebase_sync_prefs")` хранится `preferred_db_id` (`"default"` или `"(default)"`). При получении HTTP 404 метод `testConnection()` и загрузчики автоматически пробуют альтернативный `databaseId`.
- **Архитектура чтения/записи:**
  - **Realtime-слушатели (`startRealtimeListeners()`):** подключает 10 `SnapshotListener` (для `orders`, `price_catalog`, `stone_materials`, `monument_size_presets`, `constructor_prices`, `engraving_fonts`, `engraving_drawings`, `photo_sizes`, `photo_frames`, `vases`). Каждое обновление облака конвертируется в Room-сущность и сохраняется через `save*ToLocalCacheOnly` (если запись не помечена локально как `PENDING_DELETE`), причём в `price_items` работает защита от возврата дубликатов «уборка и мытье».
  - **Запись (`pushOrderToFirestore`, `pushPriceToFirestore`, `pushStoneMaterialToFirestore`, `pushSizePresetToFirestore`, `pushConstructorPriceToFirestore`, `pushFontToFirestore`, `pushDrawingToFirestore`, `pushPhotoSizeToFirestore`, `pushPhotoFrameToFirestore`, `pushVaseToFirestore`):** приоритетно использует прямой HTTP REST API (`PATCH .../{collection}/{docId}`) с `Bearer` токеном текущего пользователя `FirebaseAuth` (если авторизован), а при ошибке или отказе — `FirebaseFirestore` SDK.
  - **Удаление (`deleteOrderFromFirestore`, `deletePriceFromFirestore`, и т.д.):** выполняет удаление документа сразу в обеих базах (`default` и `(default)`) через REST и SDK для гарантированного удаления.
  - **Пакетная выгрузка (`uploadLocalToFirestore()`, `forceUploadAllLocalData()`):** сначала удаляет из облака записи со статусом `PENDING_DELETE`, затем отправляет пакеты документов через REST `:commit` (пачками по 200 операций) с fallback на SDK `WriteBatch` (до 400 операций).
  - **Загрузка кэша (`refreshLocalCacheFromFirestore()`):** принудительно забирает все 10 коллекций из Firestore (REST API с fallback на SDK) и синхронизирует локальный кэш Room; также автоматически выгружает стартовые каталоги по умолчанию, если в облаке коллекции пустые.
  - **Синхронизация отложенных офлайн-изменений (`syncPendingOfflineChanges()`):** находит все локальные сущности со статусом `PENDING_PUSH` и `PENDING_DELETE` и доотправляет их в Firestore при восстановлении интернета.
  - **Очистка (`clearBothRoomAndFirestore()`):** удаляет все документы из всех 10 коллекций в базах `default` и `(default)` и полностью очищает Room (`clearAllRoomData()`).
  - **Аутентификация Email/Password:** `signInWithEmail`, `registerWithEmail`, `signOut`, `getCurrentUserEmail`, `getCurrentUserId`.

### 7.2. `util/GoogleSignInHelper.kt`
- Содержит реализацию входа через Google с помощью Jetpack `CredentialManager` (`GetSignInWithGoogleOption`, `GetCredentialRequest`) и `FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(...))`.
- *Примечание:* в UI (`AuthDialog.kt`) используется диалог входа/регистрации по **Email/Password** и режим общей базы Firestore без входа, а `GoogleSignInHelper` предоставлен как готовый модуль для Google Sign-In.

---

## 8. Математическая и конструкторская логика

### 8.1. Конструктор памятника (`ui/components/MonumentBuilderDialog.kt`)
Главный Composable: `MonumentBuilderDialog(...)` (включая вспомогательные диалоги `ElementStoneSelector`, `StonePickerSheet`, `DimensionChip`, `DrawingPickerDialog`, `PhotoSizePickerDialog`, `PhotoFramePickerDialog`, `VasePickerDialog`).

#### Базовая физика и стоимость камня
- **Плотность гранита:** `stoneDensityKgM3 = 2900.0` кг/м³.
- **Перевод размеров (см -> м³):**
  $$\text{Объём (м}^3\text{)} = \frac{A \times B \times C}{1\,000\,000}$$
- **Масса (кг):** $\text{Масса} = \text{Объём} \times 2900$.
- **Стоимость элемента (BYN):** $\text{Цена} = \text{Объём} \times \text{pricePerM3}$ (где для каждого элемента можно выбрать либо основной камень `selectedStone`, либо отдельную породу через `ElementStoneSelector`).

#### Расчёт элементов комплекта
1. **Стела (`steleH × steleW × steleT` см):** всегда рассчитывается из `selectedStone`.
2. **Приставка (`includeAnnex`, по умолч. `80×25×8` см):** опциональная порода `selectedAnnexStone`.
3. **Полка под стелу (`includeShelf`, по умолч. `70×15×5` см):** рассчитывается из основного `selectedStone`.
4. **Тумба / Подставка (`includePlinth`):**
   - Режим 1 части (`plinthL × plinthW × plinthH`, по умолч. `60×20×15` см).
   - Режим 2 частей (`plinthHasTwoParts = true`): ч.1 (`60×20×15` см) + ч.2 (`40×20×15` см), для каждой можно задать отдельную породу камня.
5. **Цветник (`includeFlowerbed`, автоматически отключается при включении надгробной плиты):**
   - Поддерживает 3 формулы расчёта объёма (`FlowerbedCalcMode`):
     - `OPEN_JOINT` (Открытый по стыкам — боковые длинные + короткая внутренняя):
       $$V = \frac{2 \cdot L \cdot T \cdot B_w + (W - 2 B_w) \cdot T \cdot B_w}{1\,000\,000}$$
     - `OPEN_SUM` (Открытый по полной сумме брусков — без вычета толщины):
       $$V = \frac{2 \cdot L \cdot T \cdot B_w + W \cdot T \cdot B_w}{1\,000\,000}$$
     - `CLOSED_4` (Закрытый 4 балки — замкнутый контур):
       $$V = \frac{2 \cdot L \cdot T \cdot B_w + 2 \cdot (W - 2 B_w) \cdot T \cdot B_w}{1\,000\,000}$$
     *(где $L$ — длина, $W$ — ширина, $T$ — высота, $B_w$ — сечение балки).*
   - Может использовать отдельную породу `selectedFlowerbedStone`.
6. **Надгробная плита (`includeSlab`):**
   - 1 часть (по умолч. `100×60×5` см) или 2 части (`slabHasTwoParts = true`, ч.2 по умолч. `100×30×5` см) с выбором породы для каждой части.

#### Расчёт гравировки и сусального золота
Поддерживаются 3 технологии нанесения (`EngravingTechnique`: `STANDARD`, `PAINTED`, `GOLD`) раздельно для ФИО/дат и для эпитафии:
- Автоподсчёт символов (`!it.isWhitespace()`) и ручная корректировка для сусального золота: подсчитываются заглавные буквы (`isUpperCase()`) и прочие/строчные/цифры (`!isUpperCase()`).
- **`STANDARD` (обычная гравировка):** $\text{Количество} \times \text{letter\_standard}$ (`1.60 BYN/зн.` для ФИО/дат, `1.50 BYN/зн.` для эпитафии).
- **`PAINTED` (покраска букв):** $\text{Количество} \times \text{letter\_painted}$ (`10.00 BYN/зн.`).
- **`GOLD` (сусальное золото в EUR):**
  $$\text{Стоимость BYN} = \left( N_{\text{загл.}} \times P_{\text{gold\_large}} + (N_{\text{строчн.}} + N_{\text{цифр}}) \times P_{\text{gold\_small}} \right) \times \text{eurRate}$$
  где по умолчанию $P_{\text{gold\_large}} = 8.0\,\text{€}$, $P_{\text{gold\_small}} = 7.0\,\text{€}$.
- **Предпросмотр шрифта:** рендерится с помощью `FontHelper` на тёмной гранитной подложке.

#### Декор, фото, аксессуары и работы
- **Рисунки (`DrawingPickerDialog`):** выбор из активных `EngravingDrawingItem` (крест, цветы, икона, ангел, свечи). Поддерживается разбиение на категории в `onUpdateFactoryDetails` (`crossInfo`, `photoVignetteInfo` с виньетками, `plateDecoration`).
- **Фото и рамка (`PhotoSizePickerDialog`, `PhotoFramePickerDialog`):** выбор размера фото (`PhotoSizeItem`) и рамки (`PhotoFrameItem`).
- **Вазы (`VasePickerDialog`):** выбор вазы (`VaseItem`) и количества `vaseQty`.
- **Монтаж и демонтаж:** редактируемые поля на базе `installation_base_price` (500 BYN) и `demontazh_base_price` (150 BYN), плюс текстовое поле заметок по демонтажу.

#### Передача в смету
Кнопка «Добавить комплект в расчёт»:
1. Формирует `List<CalculatedItemData>` по каждому включённому компоненту (Стела, Приставка, Полка, Тумба ч.1/ч.2, Цветник, Надгробная плита ч.1/ч.2, Гравировка ФИО/эпитафии с пометкой «Сусальное золото» при `GOLD`, Рисунки, Фото, Рамка, Вазы, Монтаж, Демонтаж) и передаёт в `onAddMultipleItems`.
2. Разбирает ФИО на Фамилию / Имя / Отчество (`split`), даты жизни (`deceasedBirthDate`, `deceasedDeathDate`) и вызывает `onUpdateFactoryDetails(...)` для мгновенного заполнения наряда завода.

*(Примечание: в коде также вычисляются `builtInKeys` и `customServices` из `constructorServicePrices`, которые зарезервированы на будущее и не рендерятся как отдельный UI-блок).*

### 8.2. Калькуляторы замеров (`ui/components/DimensionCalculatorsDialog.kt`)
Содержит 4 вкладки с возможностью подставлять товары из каталога и сохранять тариф обратно в прайс (`onSaveServicePrice`):
1. **`FencePerimeterCalculator` (Ограда / Периметр):**
   - Периметр $P = (L + W) \times 2$ (пог. м); Стоимость $= P \times \text{Цена за м.п.}$ (тариф `measure_fence_price_pm`, по умолч. `45.0`).
2. **`AreaTileCalculator` (Плитка / Площадь):**
   - Площадь $S = L \times W$ (м²); Стоимость $= S \times \text{Цена за м}^2$ (тариф `measure_tile_price_sqm`, по умолч. `85.0`).
3. **`EngravingLettersCalculator` (Гравировка):**
   - Подсчёт символов без пробелов (`textInput.count { !it.isWhitespace() }`); Стоимость $= N \times \text{letter\_standard}$.
4. **`DeliveryDistanceCalculator` (Доставка):**
   - Доп. км $= \max(0, \text{totalDist} - \text{baseKm})$; Стоимость $= \text{basePrice} + \text{Доп. км} \times \text{extraKmPrice}$ (`60.0 + max(0, km - 15.0) * 1.50`).

---

## 9. Формирование документов и экспорт

### 9.1. Документ клиента (`data/EstimateDocumentData.kt`)
Модель `EstimateDocumentData` хранит шапку организации:
- `companyName = "ИП Ступак О.О."`
- `companyPhone = "Viber, Telegram тел.: +375295421809, +375297273637"`
- `companyAddress = "«ДВА АНГЕЛА» Ольшаны рынок БРОДОК павильон 35"`
- А также поля заказчика, усопшего, сметных позиций `items`, `subtotalAmount`, `discountPercent`, `discountAmount`, `totalAmount`, `prepaymentAmount`, `remainingAmount`, даты приёма и исполнения.

Функции очистки и построения:
- **`cleanEngravingCalculation(raw: String): String`:** удаляет расчётные формулы и цены в скобках `(...)` / `[...]` (например, `12 зн. × 1.60 BYN`, суммы в EUR/BYN), а если обнаружено «сусальн»/«золот», обязательно сохраняет/добавляет пометку `(Сусальное золото)` без дублирования.
- **`extractCleanDeceasedName(rawDeceasedName, items): String`:** извлекает чистое ФИО усопшего из поля заказа или из позиции гравировки.
- **`extractBundleField(items, prefix): String`:** вытаскивает описание поля из комплектных записей (если они заданы в `customNote`).
- **`fromCalculationState(state)`** и **`fromSavedOrder(order, items)`:** собирают объект `EstimateDocumentData`.

### 9.2. Производственный наряд для завода (`data/FactoryOrderDocumentData.kt`)
Модель `FactoryOrderDocumentData` содержит:
- Шапку организации, номер и дату заказа, данные заказчика.
- Раздел «Сведения об усопшем»: `deceasedLastName`, `deceasedFirstName`, `deceasedMiddleName`, `deceasedBirthDate`, `deceasedDeathDate`.
- Гравировка и оформление: `crossInfo`, `photoInfo`, `frameInfo`, `epitaphText`, `goldLeafInfo`, `plateDecoration`, `additionsInfo`.
- Комплектация камня (с поддержкой первых и вторых частей): `monumentMaterial`, `obeliskInfo`, `annexInfo` (Приставка), `shelfInfo` (Полка), `plinthInfo`, `plinthPart2Info`, `flowerbedInfo`, `slabInfo`, `slabPart2Info`, `vaseInfo`, `dismantlingInfo`.

Ключевые функции очистки и нормализации текста:
- **`fixParentheses(input: String): String`:** балансирует и исправляет незакрытые/лишние круглые скобки `(` и `)`, удаляет пустые `()`, добавляет пробел перед `(` при необходимости.
- **`cleanWorkshopText(raw: String): String`:** убирает английские/ценовые приставки (`BYN`, `USD`, `EUR`), формулы расчёта и лишние технические хвосты.
- **`cleanPhotoText(raw: String): String`:** убирает префиксы `Категория:`, `размер:`, слово `Портрет` и все скобки, оставляя чистый вид и размер фото.
- **`cleanFrameText(raw: String): String`:** для врезки возвращает `"врезка"`, для `"Без рамки"` — пустую строку `""`, для рамок — только материал (например, `"бронза"`, `"алюминий"`, `"гранит"`).
- **`splitFio(fullFio: String)`** и **`extractDatesFromText(raw: String)`:** раскладывают ФИО на 3 компонента и вытягивают даты жизни.
- **`fromCalculationState(state)`**, **`fromSavedOrder(order, items)`**, **`fromEstimateDocumentData(...)`:** формируют наряд из `CalculationState`, `SavedOrder` или `EstimateDocumentData`, определяя использование сусального золота в ФИО и эпитафии (маркировка `(СУСАЛЬНОЕ ЗОЛОТО)` / `Сусальное золото`), разбирая размеры стелы, приставки, полки, 1-й/2-й частей тумбы и надгробной плиты, цветника и ваз, и применяя `fixParentheses` ко всем каменным элементам.

### 9.3. Генератор Word, HTML, печати и текста (`util/DocxGenerator.kt`)
`object DocxGenerator` (1057 строк):
- **A4 и стили OOXML:** страница `11906 × 16838` twips (A4), поля по `1 см` (`567` twips). Собирает валидный `.docx` ZIP-архив из 6 XML-частей: `[Content_Types].xml`, `_rels/.rels`, `word/_rels/document.xml.rels`, `word/styles.xml`, `word/settings.xml`, `word/document.xml`.
- **Функции для Договора клиента:**
  - `generateDocxFile(context, data)` -> `Result<File>` в `cacheDir/shared_files`.
  - `buildDocumentXml(data)` -> OOXML разметка таблицы позиций, реквизитов и подписи заказчика.
  - `generateEstimateHtml(data)` -> HTML с `@page { size: A4 portrait; margin: 7mm 10mm }` и сжатием `@media print { height: 100%; overflow: hidden }` для гарантированного размещения на 1 листе А4 при печати/PDF.
  - `generatePlainText(data)` -> текстовая версия сметы для мессенджеров.
  - `saveDocxToPublicDownloads(context, data)` -> сохраняет в `MediaStore.Downloads` (API 29+) или `Environment.DIRECTORY_DOWNLOADS` (API < 29).
- **Функции для Наряда завода:**
  - `generateFactoryDocxFile(context, data)` -> `Result<File>` (`.docx`).
  - `buildFactoryDocumentXml(data)` -> OOXML-таблицы наряда *(примечание: в отличие от HTML, PlainText и UI-предпросмотра, где строки `annexInfo` «Приставка» и `shelfInfo` «Полка» выводятся, в текущей реализации `buildFactoryDocumentXml` эти две строки отдельно в XML-таблицу `.docx` не добавлены)*.
  - `generateFactoryHtml(data)` -> HTML-форма А4 на 1 лист, включает строки «Приставка» и «Полка».
  - `generateFactoryPlainText(data)` -> текстовый бланк наряда, включает «Приставка» и «Полка».
  - `saveFactoryDocxToPublicDownloads(context, data)` -> сохранение `.docx` наряда в Загрузки.
- **Системное взаимодействие:**
  - `printEstimateViaWebView(context, htmlContent, jobName)` — рендерит HTML в `WebView` и вызывает системный `PrintManager` (печать или сохранение в PDF).
  - `openDocxFile(context, file)` и `shareDocxFile(context, file)` — открывают/отправляют `.docx` через `FileProvider`.
  - Вспомогательные XML/HTML билдеры: `escapeXml`, `escapeHtml`, `cellXml`, `factoryRowXml`, `factorySectionRowXml`.

### 9.4. Диалог предпросмотра документов (`ui/components/EstimatePreviewDialog.kt`)
- **`EstimatePreviewDialog`:** полноэкранный диалог с 2 вкладками:
  1. **«Договор клиенту» (`ClientEstimateSheet`)** — бланк заказа-договора с шапкой ИП, таблицей позиций, финансовым блоком и подписями сторон. В нижней панели: `Копировать`, `Печать / PDF`, `В Загрузки (Word)`, `Отправить (.docx)`.
  2. **«Наряд завода» (`FactoryOrderSheet`)** — бланк цеха/производства с таблицей ФИО/дат, гравировки/оформления (с пометкой сусального золота), всех элементов памятника (включая Приставку, Полку, 2-е части тумбы и плиты) и допусловиями.
- Включает вспомогательные Composable: `ClientFieldRow`, `FactoryDataRow`, `FactorySectionTitle`, `SummaryRow`.

---

## 10. Пользовательский интерфейс: экраны и диалоги

### 10.1. Экраны (`ui/screens/`)
1. **`CalculatorScreen.kt`:**
   - Карточка номера сметы и раскрываемая форма данных клиента/усопшего (ФИО, телефон, даты рождения/смерти, кладбище, участок, примечание) + кнопка быстрого обновления курса НБРБ (`$ = ...`).
   - Кнопки запуска: «Конструктор памятника», «Каталог расценок», «Замеры».
   - Список добавленных позиций (`CalculatedItemCard`) со степперами количества `- / +`, быстрым редактированием цены/количества (`AlertDialog`) и удалением.
   - Нижняя сводная панель: подытог, скидка, «ИТОГО», предоплата/остаток, кнопки «Договор клиенту», «Наряд завода», «Скидка», «Сохранить» (с выбором статуса `DRAFT`/`IN_PROGRESS`/`PAID`), «Текст» (отправка в мессенджер).
2. **`PriceSettingsScreen.kt` (3691 строка, 7 вкладок):**
   - Вкладка 1 **`GeneralCatalogTab`:** поиск, фильтрация по категориям, карточки `PriceItemManagementCard`, быстрое переключение видимости, быстрый пересчёт цены BYN ↔ USD, `AddEditPriceDialog`.
   - Вкладка 2 **`StoneMaterialsTab`:** управление породами камня (`StoneMaterialCard`, `AddEditStoneMaterialDialog` с 10 популярными пресетами гранита/мрамора, флагом прямой гравировки и двойным вводом BYN ↔ USD / м³).
   - Вкладка 3 **`MeasurementPricingTab`:** панель курсов USD/EUR НБРБ и редактирование секций (`CatalogItemsSectionCard`, `MeasurementSectionCard`, `AddEditMeasurementTariffDialog`) для оград, плитки, тарифов гравировки (включая сусальное золото в EUR), доставки, монтажа и демонтажа.
   - Вкладка 4 **`FontsPricingTab`:** поиск, переключатель активности и карточки `FontItemCard` с предпросмотром на тёмной гранитной подложке + `AddEditFontDialog`.
   - Вкладка 5 **`DrawingsPricingTab`:** фильтр по категориям декора, поиск по коду/названию, карточки `DrawingItemCard` и `AddEditDrawingDialog`.
   - Вкладка 6 **`PhotoSizesAndFramesTab`:** 2 подвкладки («Размеры фото и портретов» и «Рамки и врезка») с `AddEditPhotoSizeDialog` и `AddEditPhotoFrameDialog`.
   - Вкладка 7 **`VasesPricingTab`:** фильтр по материалу, управление вазами и `AddEditVaseDialog`.
3. **`SavedOrdersScreen.kt`:**
   - Список `SavedOrderCard` с бейджем статуса `StatusBadge`.
   - Полноразмерный диалог просмотра состава сметы с кнопками «В калькулятор», «Договор / Печать» (открывает `EstimatePreviewDialog`) и «Удалить».
4. **`CloudSyncSettingsScreen.kt`:**
   - Управление курсом USD и кнопкой обновления НБРБ.
   - Карточка облачной синхронизации Firestore (`Выгрузить в Firestore`, `Обновить кэш`, `Тест связи`, кнопка активации БД в Google Cloud при ошибке 404, вход/выход `AuthDialog`).
   - Кнопка полной очистки Room и Firestore с защитой паролем (`"2101264"`).
   - Экспорт и импорт резервной копии JSON (`exportDatabaseBackup`, `importDatabaseBackup`).
   - Сводная статистика локальных таблиц и справка по настройке правил Firestore.

### 10.2. Диалоги (`ui/components/`)
- **`ItemCatalogSheet.kt`:** `ModalBottomSheet` с поиском, фильтром категорий и группировкой позиций прайса (`CatalogItemRow`) для быстрого добавления в расчёт.
- **`AddCustomItemDialog.kt`:** создание произвольной позиции с одновременным пересчётом BYN ↔ USD, категорией, количеством и выбором единицы измерения.
- **`AddEditPriceDialog.kt`:** создание и редактирование позиции прайс-листа (`PriceItem`) с конвертацией BYN ↔ USD.
- **`DiscountPrepaymentDialog.kt`:** настройка скидки (пресеты `3%, 5%, 10%, 15%` или ручной ввод) и предоплаты (пресеты `30%, 50%, 70%` от суммы или ручной ввод) с расчётом остатка.
- **`AuthDialog.kt`:** вход и регистрация в Firebase Authentication по Email/Password с расшифровкой ошибок или переключением в режим общей базы Firestore без входа.
