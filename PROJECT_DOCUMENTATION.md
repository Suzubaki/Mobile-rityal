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

### 8.1. Конструктор памятника (`ui/components/MonumentBuilderDialog.kt`, 3698 строк)
Главный Composable: `MonumentBuilderDialog(...)`.
В файле определены ровно 5 вспомогательных Composable-компонентов:
- `SectionHeader(title, icon)` — заголовок раздела конструктора;
- `CostRow(label, amount)` — строка итоговой детализации;
- `StoneMaterialSelector(...)` — выбор единого основного камня комплекта (список `RadioButton`, при пустом списке пород выводит красное предупреждение);
- `ElementStoneSelector(...)` — компактный блок переключения индивидуального камня для отдельного элемента (`Switch` «Как у стелы» / «Инд. камень» + выпадающий список `ExposedDropdownMenuBox` для выбора другой породы);
- `DrawingPickerDialog(...)` — модальный диалог множественного выбора рисунков гравировки с поиском и фильтром категорий.
*(Выбор размеров фото, рамок, ваз и шрифта реализован через встроенные выпадающие списки `ExposedDropdownMenuBox`, без отдельных диалогов).*

#### 1) Входные параметры и инициализация данных
`CalculatorScreen` передаёт в конструктор активные справочники из `RitualViewModel`:
- **Камни (`materials`)**: активные `StoneMaterialEntity.toModel()`. Внутри конструктора fallback отсутствует — при пустом справочнике `StoneMaterialSelector` показывает ошибку конфигурации.
- **Тарифы и услуги (`servicePrices` + `constructorServicePrices`)**: объединяются в общую карту `actualServicePrices: Map<String, Double>`; поиск цены идёт через `getServicePrice(code, fallback)`. Курс евро вычисляется как `actualEurRate = actualServicePrices["eur_exchange_rate"] ?: eurRate` (по умолчанию `3.55`).
- **Фото, рамки, вазы**: при пустых списках из БД автоматически подставляются дефолтные каталоги `DefaultCatalog.getDefaultPhotoSizes()`, `getDefaultPhotoFrames()`, `getDefaultVases()`.
- **Предзаполнение ФИО и эпитафии**:
  - `CalculatorScreen` собирает `initialFioText` из `calcState.deceasedName` (или `deceasedLastName / FirstName / MiddleName`) + перенос строки + `deceasedBirthDate — deceasedDeathDate`. Если строка пуста, конструктор подставляет шаблон `"Иванов Иван Иванович\n15.05.1950 — 20.08.2024"`.
  - Если `initialEpitaphText` пуст, подставляется `"Помним, любим, скорбим..."`.
- **Зарезервированные поля в коде (не выводятся в UI):** `actualPresets` (`sizePresets`), `builtInKeys` / `customServices` / `selectedCustomServiceKeys`, а также параметр `onAddSingleItem` (активная нижняя кнопка добавления использует `onAddMultipleItems` + `onUpdateFactoryDetails`).

#### 2) Базовая физика и расчёт камня
- **Плотность гранита:** `stoneDensityKgM3 = 2900.0` кг/м³.
- **Единицы ввода:** размеры задаются в сантиметрах (строковые состояния парсятся через `toDoubleOrNull() ?: 0.0`).
- **Объём элемента (м³):**
  $$V = \frac{A_{\text{см}} \times B_{\text{см}} \times C_{\text{см}}}{1\,000\,000}$$
- **Масса элемента (кг):** $M = V \times 2900.0$.
- **Стоимость элемента (BYN):** $\text{Цена} = V \times \text{pricePerM3}$, где используется либо цена основного камня `selectedStone`, либо цена выбранной в `ElementStoneSelector` породы.

#### 3) Разделы 1 и 2: Каменные элементы комплекта
1. **Основной камень (`selectedStone`, Раздел 1):** выбирается в `StoneMaterialSelector`. Применяется ко всей конструкции по умолчанию (для Стелы и Полки — всегда, для остальных элементов — если не включён индивидуальный камень).
2. **Стела / Обелиск (`stele`):** включена всегда. Размеры по умолчанию: `H = 100`, `W = 50`, `T = 8` см. Всегда считается по цене основного `selectedStone`.
3. **Приставка к стеле (`includeAnnex`, по умолч. `false`):**
   - Размеры по умолч.: `H = 80`, `W = 25`, `T = 8` см.
   - Имеет `ElementStoneSelector`: флаг `annexUseDifferentStone` по умолчанию `true` (подставляется `actualMaterials[1]` или `[0]` для контрастной вставки).
4. **Полка под стелу (`includeShelf`, по умолч. `false`):**
   - Размеры по умолч.: `L = 70`, `W = 15`, `T = 5` см.
   - Всегда рассчитывается из основного камня `selectedStone` (без `ElementStoneSelector`).
5. **Тумба / Подставка (`includePlinth`, по умолч. `true`):**
   - Переключатель `plinthHasTwoParts` (по умолч. `false`) включает составную тумбу из 2 частей.
   - **Часть 1:** по умолч. `L = 60`, `W = 20`, `H = 15` см, `plinth1UseDifferentStone = false`.
   - **Часть 2** *(активна только при `plinthHasTwoParts = true`)*: по умолч. `L = 40`, `W = 20`, `H = 15` см, `plinth2UseDifferentStone = true` (порода `actualMaterials[1]` или `[0]`).
   - Итоговые `plinthVolumeM3`, `plinthMassKg`, `plinthPrice` суммируют часть 1 и часть 2.
6. **Цветник (`includeFlowerbed`, по умолч. `true`):**
   - **Взаимоисключение с надгробной плитой:** включение цветника автоматически устанавливает `includeSlab = false`, и наоборот. Если `!includeFlowerbed || includeSlab`, объём и стоимость цветника равны `0.0`.
   - Размеры по умолч.: длина `L = 100`, ширина `W = 60`, высота/толщина `T = 8`, ширина бруса `BarW = 5` см. Опциональный `ElementStoneSelector` (`flowerbedUseDifferentStone = false`).
   - **3 формулы расчёта (`flowerbedMode`, по умолч. `"OPEN_JOINT"`):**
     - `"OPEN_JOINT"` *(3 бруса со стыком)*: 2 длинных бруса по `L` и 1 поперечный длиной `max(0, W - 2*BarW)`:
       $$V = \frac{2 \cdot L \cdot T \cdot B_w + \max(0, W - 2 B_w) \cdot T \cdot B_w}{1\,000\,000}$$
     - `"OPEN_SUM"` *(3 бруса — сумма)*: 2 длинных бруса по `L` и 1 поперечный полной ширины `W` без вычета стыков:
       $$V = \frac{2 \cdot L \cdot T \cdot B_w + W \cdot T \cdot B_w}{1\,000\,000}$$
     - `"CLOSED_4"` *(4 бруса — закрытый контур)*: 2 длинных по `L` и 2 поперечных со стыком `max(0, W - 2*BarW)`:
       $$V = \frac{2 \cdot L \cdot T \cdot B_w + 2 \cdot \max(0, W - 2 B_w) \cdot T \cdot B_w}{1\,000\,000}$$
7. **Надгробная плита (`includeSlab`, по умолч. `false`):**
   - Переключатель `slabHasTwoParts` (по умолч. `false`).
   - **Часть 1:** по умолч. `L = 100`, `W = 60`, `T = 5` см (`slab1UseDifferentStone = false`).
   - **Часть 2** *(активна при `slabHasTwoParts = true`)*: по умолч. `L = 100`, `W = 30`, `T = 5` см (`slab2UseDifferentStone = true`, порода `actualMaterials[1]` или `[0]`).
- **Сводка по камню:** `stoneTotal` суммирует стоимости всех включённых элементов; `totalEstimatedWeightKg` складывает их массы и округляет до целых кг (`.toInt()`).

#### 4) Раздел 3: Гравировка, шрифты, предпросмотр и сусальное золото
- **Используемые тарифы из `actualServicePrices`:**
  - `letter_standard` — `1.60 BYN` (обычный знак ФИО/дат);
  - `letter_painted` — `10.00 BYN` (покрашенная буква/знак);
  - `letter_gold_large` — `8.00 EUR` (заглавная буква сусальным золотом);
  - `letter_gold_small` — `7.00 EUR` (строчная буква или цифра сусальным золотом);
  - `letter_epitaph` — `1.50 BYN` (обычный знак эпитафии).
- **Выбор шрифта и живой предпросмотр:**
  - В `ExposedDropdownMenuBox` выбирается `selectedFont` (`EngravingFontItem`). Рядом отображается бейдж стоимости из каталога шрифтов (`"Бесплатно"` или цена), однако в итоговую сумму `engravingTotal` стоимость самого шрифта отдельно не добавляется — расчёт ведётся исключительно по знакам `fioPrice + epitaphPrice`.
  - Ниже рендерится имитация чёрной гранитной стелы (`#1B1E22`), где текст `fioText` и `epitaphText` отображается реальным шрифтом из `FontHelper` и окрашивается под выбранную технологию (`"GOLD"` -> золотой `#FACC15`, `"PAINTED"` -> белый `#FFFFFF`, `"STANDARD"` -> светло-серый `#E2E8F0` / `#CBD5E1`).
- **Алгоритм подсчёта знаков ФИО и дат (`fioLetteringMode`):**
  - В отличие от эпитафии, в ФИО считаются **только буквы и цифры** (пробелы, дефисы `-`, тире `—`, точки `.` и запятые игнорируются):
    - `autoLargeCount = fioText.count { it.isLetter() && it.isUpperCase() }`
    - `autoSmallCount = fioText.count { it.isLetter() && !it.isUpperCase() }`
    - `autoDigitsCount = fioText.count { it.isDigit() }`
  - В режиме `"GOLD"` появляется кнопка ручной правки `showManualFioCounts` для отдельной корректировки заглавных, строчных и цифр, а также 3 карточки разбивки (`Заглавные 8€`, `Строчные 7€`, `Цифры 7€`).
  - Расчёт:
    - `"GOLD"`: `fioGoldTotalEur = fioLargeLetters * goldLargeEurPrice + (fioSmallLetters + fioDigits) * goldSmallEurPrice`; `fioPrice = fioGoldTotalEur * actualEurRate` (BYN).
    - `"PAINTED"`: `fioPrice = (fioLarge + fioSmall + fioDigits) * paintedLetterPrice`.
    - `"STANDARD"`: `fioPrice = (fioLarge + fioSmall + fioDigits) * standardLetterPrice`.
- **Алгоритм подсчёта эпитафии (`epitaphLetteringMode`):**
  - В режимах `"STANDARD"` и `"PAINTED"` (когда не включена ручная правка `showManualEpitaphCounts`) считается `epitaphText.count { !it.isWhitespace() }` — то есть **все непробельные символы, включая знаки препинания**.
  - В режиме `"GOLD"` (или при ручной правке) считаются только буквы и цифры (`isLetter()` с разбивкой по регистру и `isDigit()`).
  - Расчёт:
    - `"GOLD"`: `(large * goldLargeEurPrice + (small + digits) * goldSmallEurPrice) * actualEurRate`.
    - `"PAINTED"`: `countedEpitaph * paintedLetterPrice`.
    - `"STANDARD"`: `countedEpitaph * epitaphSignPrice` (`1.50 BYN`).

#### 5) Разделы 4–7: Декор, фото, вазы и работы
- **Раздел 4 — Рисунки (`selectedDrawingIds`):**
  - Кнопка открывает `DrawingPickerDialog` с поиском по `name`, `code`, `category` и фильтр-чипами (`"Все"`, `"Крест"`, `"Цветок"`, `"Иконы"`, `"Ангелы"`, `"Свечи"` + кастомные категории из БД).
  - Выбранные рисунки отображаются в конструкторе в виде `InputChip` с кнопкой удаления; `drawingsTotal = selectedDrawingsList.sumOf { it.price }`.
- **Раздел 5 — Портрет / фото и рамка (`includePhoto`, по умолч. `false`):**
  - Два выпадающих списка `ExposedDropdownMenuBox`: размер фото (`actualPhotoSizes`, сгруппированы по `category`) и рамка/врезка (`actualPhotoFrames`, с типом `materialType` и ценой).
  - `photoTotal = photoSizePrice + photoFramePrice`.
- **Раздел 6 — Вазы (`includeVase`, по умолч. `false`):**
  - `ExposedDropdownMenuBox` по `actualVases` (группировка по `materialType`) + степпер количества `vaseQuantity` (`>= 1`).
  - `vaseTotal = vaseUnitPrice * vaseQuantity`.
- **Раздел 7 — Монтаж и демонтаж:**
  - **Монтаж (`includeInstallation`, по умолч. `false`):** базовое значение берётся из `installation_base_price` (`500 BYN`), регулируется через `Slider` (`0..4000`, `steps = 79`), текстовое поле и 7 быстрых чипов (`400, 500, 800, 1000, 1200, 1500, 2000`).
  - **Демонтаж (`includeDemontazh`, по умолч. `false`):** базовое значение из `demontazh_base_price` (`150 BYN`), регулируется через `Slider` (`0..1000`, `steps = 39`), поле ввода, 6 быстрых чипов (`50, 100, 150, 200, 300, 400`) и текстовое примечание `demontazhNote` (по умолч. `"Демонтаж старого надгробия / креста"`).
  - `worksTotal = (installation если вкл.) + (demontazh если вкл.)`.

#### 6) Итоговая карточка и алгоритм кнопки «Добавить комплект в расчёт» (`testTag = "add_monument_detailed_btn"`)
Итоговая сумма конструктора:
`monumentGrandTotal = stoneTotal + engravingTotal + drawingsTotal + photoTotal + vaseTotal + worksTotal`.

По нажатию кнопки выполняются два шага:
1. **Генерация позиций расчёта (`items: MutableList<CalculatedItemData>`) -> `onAddMultipleItems(items)`:**
   - Категория `"Памятники"`:
     - `Стела <размер> (<камень>)`
     - `Приставка к стеле <размер> (<камень>)` *(если `includeAnnex`)*
     - `Полка под стелу <размер> (<камень>)` *(если `includeShelf`)*
     - `Тумба (подставка) <размер> (<камень>)` либо при `plinthHasTwoParts`: две отдельные позиции `Тумба ч.1 (основная)...` и `Тумба ч.2 (дополнительная)...`
     - `Цветник (4 бруса закрытый | 3 бруса без вычитания стыка | 3 бруса со стыком) <размер> (<камень>)` *(если `includeFlowerbed && !includeSlab`)*
     - `Надгробная плита <размер> (<камень>)` либо при `slabHasTwoParts`: `Надгробная плита ч.1...` и `Надгробная плита ч.2...`
   - Категория `"Гравировка"`:
     - ФИО и даты: `"Покраска букв ФИО и дат [Шрифт: ...]"` / `"Сусальное золото ФИО и дат [Шрифт: ...]"` / `"Гравировка ФИО и дат [Шрифт: ...]"`, `customNote = fioText.trim()`
     - Эпитафия: `"Покраска эпитафии"` / `"Сусальное золото эпитафии"` / `"Гравировка эпитафии"`, `customNote = epitaphText.trim()`
     - Каждый выбранный рисунок: `"Гравировка рисунка ${code} «${name}»"`, `customNote = "Категория: ${category}, код: ${code}"`
     - Фото (`includePhoto`): `"Портрет / Фотокерамика (${photo.name})"` (`customNote = "Категория: ${category}, размер: ${dimensions}"`)
     - Рамка (`includePhoto && selectedPhotoFrame.price > 0`): `"Рамка / врезка фото (${frame.name})"` (`customNote = "Тип/материал: ${materialType}"`). *Примечание: бесплатные варианты с ценой `0` (например, `"Без рамки"`) как отдельная сметная строка не добавляются, а передаются только в наряд.*
   - Категория `"Прочее"`:
     - `Ваза ${vase.name}` (`quantity = vaseQuantity`)
     - `Монтаж (установка) памятника` *(если `includeInstallation && installationPrice > 0`)*
     - `Демонтаж` *(если `includeDemontazh && demontazhPrice > 0`, `customNote = demontazhNote`)*
2. **Обновление полей производственного наряда (`sendFactoryDetailsToState()` -> `onUpdateFactoryDetails`):**
   - Разбирает `fioText` через `FactoryOrderDocumentData.splitFio(...)` и `extractDatesFromText(...)` на Фамилию, Имя, Отчество и даты рождения/смерти.
   - Автоматически распределяет выбранные рисунки по категориям для наряда:
     - Кресты (`category` или `name` содержит `"Крест"`, или `code` начинается с `"КР"`) -> `crossInfo`;
     - Виньетки (`"Виньет"` или `code` начинается с `"В-"`) -> `photoVignetteInfo`;
     - Все остальные рисунки -> `additionsInfo`.
   - Формирует строковые описания для наряда: `obeliskInfo` (стела, при необходимости через `+ приставка ... + полка ...`), `plinthInfo`, `flowerbedInfo`, `slabInfo`, `vaseInfo`, `dismantlingInfo`, `epitaphText`, `monumentMaterial`, `photoInfo`, `frameInfo`. В `RitualViewModel.updateFactoryDetails` эти поля записываются в `CalculationState`.

### 8.2. Калькуляторы замеров (`ui/components/DimensionCalculatorsDialog.kt`)
Содержит 4 вкладки с возможностью подставлять товары из каталога и сохранять обновлённый тариф обратно в прайс (`onSaveServicePrice`):
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

Ключевые функции очистки и построения:
- **`cleanEngravingCalculation(raw: String): String`:** удаляет расчётные формулы, количество знаков, курсы и цены в скобках `(...)` / `[...]` (например, `12 зн. × 1.60 BYN`), оставляя понятное описание позиции. Если в названии/примечании обнаружено «сусальн»/«золот», обязательно сохраняет/добавляет пометку `(Сусальное золото)` без дублирования.
- **`extractCleanDeceasedName(rawDeceasedName, items): String`:** извлекает чистое ФИО усопшего из поля заказа или из позиции гравировки.
- **`extractBundleField(items, prefix): String`:** вытаскивает описание поля из комплектных записей, если оно задано в `customNote`.
- **`fromCalculationState(state)`** и **`fromSavedOrder(order, items)`:** собирают итоговый `EstimateDocumentData`, исключая технический расчётный шум и сохраняя маркеры сусального золота.

### 9.2. Производственный наряд для завода (`data/FactoryOrderDocumentData.kt`)
Модель `FactoryOrderDocumentData` содержит:
- Шапку организации, номер и дату заказа, данные заказчика.
- Раздел «Сведения об усопшем»: `deceasedLastName`, `deceasedFirstName`, `deceasedMiddleName`, `deceasedBirthDate`, `deceasedDeathDate`.
- Гравировка и оформление: `crossInfo`, `photoInfo`, `frameInfo`, `epitaphText`, `goldLeafInfo`, `plateDecoration`, `additionsInfo`.
- Комплектация камня (с поддержкой приставки, полки и первых/вторых частей): `monumentMaterial`, `obeliskInfo`, `annexInfo` (Приставка), `shelfInfo` (Полка), `plinthInfo`, `plinthPart2Info`, `flowerbedInfo`, `slabInfo`, `slabPart2Info`, `vaseInfo`, `dismantlingInfo`.

Ключевые функции очистки и нормализации текста:
- **`fixParentheses(input: String): String`:** балансирует незакрытые/лишние круглые скобки `(` и `)`, удаляет пустые `()`, добавляет пробел перед `(` при необходимости (применяется ко всем каменным элементам).
- **`cleanWorkshopText(raw: String): String`:** удаляет английские/ценовые приставки (`BYN`, `USD`, `EUR`), формулы расчёта знаков и технические хвосты, оставляя только цеховые параметры.
- **`cleanPhotoText(raw: String): String`:** убирает префиксы `Категория:`, `размер:`, слово `Портрет` и все скобки, оставляя чистый вид и размер фото.
- **`cleanFrameText(raw: String): String`:** для врезки возвращает `"врезка"`, для `"Без рамки"` — пустую строку `""`, для рамок — только материал (например, `"бронза"`, `"алюминий"`, `"гранит"`).
- **`splitFio(fullFio: String)`** и **`extractDatesFromText(raw: String)`:** раскладывают ФИО на 3 компонента и вытягивают даты жизни.
- **`fromCalculationState(state)`**, **`fromSavedOrder(order, items)`** и **`fromEstimateDocumentData(...)`:**
  - `fromCalculationState` и `fromSavedOrder` берут подготовленные поля `state`/`order` (`obeliskInfo`, `plinthInfo`, `epitaphText` и т. д.), дополнительно извлекают **Приставку (`annexInfo`)**, **Полку (`shelfInfo`)**, 2-е части тумбы (`plinthPart2Info`) и надгробной плиты (`slabPart2Info`) из `selectedItems`, очищают фото/рамки/цеховой текст и балансируют скобки через `fixParentheses`.
  - `fromEstimateDocumentData` дополнительно определяет наличие сусального золота в позициях ФИО и эпитафии и добавляет маркировку `(СУСАЛЬНОЕ ЗОЛОТО)` / `Сусальное золото (ФИО и даты)` в наряд.

### 9.3. Генератор Word, HTML, печати и текста (`util/DocxGenerator.kt`)
`object DocxGenerator` (1057 строк):
- **A4 и стили OOXML:** страница `11906 × 16838` twips (A4), поля по `1 см` (`567` twips). Собирает валидный `.docx` ZIP-архив из 6 XML-частей: `[Content_Types].xml`, `_rels/.rels`, `word/_rels/document.xml.rels`, `word/styles.xml`, `word/settings.xml`, `word/document.xml`.
- **Функции для Договора клиента:**
  - `generateDocxFile(context, data)` -> `Result<File>` в `cacheDir/shared_files`.
  - `buildDocumentXml(data)` -> OOXML-разметка шапки ИП, таблицы позиций, финансовых итогов и подписей заказчика.
  - `generateEstimateHtml(data)` -> HTML с `@page { size: A4 portrait; margin: 7mm 10mm }` и правилами `@media print { height: 100%; overflow: hidden }` для гарантированного размещения на 1 листе А4 при печати/PDF.
  - `generatePlainText(data)` -> текстовая версия сметы для отправки в мессенджеры.
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
  1. **«Договор клиенту» (`ClientEstimateSheet`)** — бланк заказа-договора с шапкой ИП, таблицей позиций (без лишних формул расчёта знаков), финансовым блоком и подписями сторон. В нижней панели: `Копировать`, `Печать / PDF`, `В Загрузки (Word)`, `Отправить (.docx)`.
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
