package com.example.data

object DefaultCatalog {
    fun getItems(): List<PriceItem> = emptyList()
    fun getDefaultItems(): List<PriceItem> = emptyList()
    fun getDefaultStoneMaterials(): List<StoneMaterialItem> = emptyList()
    fun getDefaultSizePresets(): List<MonumentSizePresetItem> = emptyList()
    fun getDefaultEngravingFonts(): List<EngravingFontItem> = listOf(
        EngravingFontItem(
            id = "font_academic",
            name = "Академический (Классика)",
            styleKey = "SERIF",
            price = 0.0,
            sampleText = "Иванов Иван Иванович\n1950 — 2024",
            description = "Традиционный строгий шрифт с засечками для гранитных памятников",
            sortOrder = 0
        ),
        EngravingFontItem(
            id = "font_brusok",
            name = "Брусок (Рубленый прямой)",
            styleKey = "SANS_SERIF_BOLD",
            price = 0.0,
            sampleText = "ИВАНОВ ИВАН ИВАНОВИЧ\n1950 — 2024",
            description = "Четкий, хорошо читаемый прямой шрифт повышенной контрастности",
            sortOrder = 1
        ),
        EngravingFontItem(
            id = "font_old_slavic",
            name = "Старославянский (Церковный)",
            styleKey = "OLD_SLAVIC",
            price = 0.0,
            sampleText = "Ивановъ Иванъ Ивановичъ\n1950 — 2024",
            description = "Торжественный шрифт в древнерусском стиле с акцентированными засечками",
            sortOrder = 2
        ),
        EngravingFontItem(
            id = "font_calligraphy",
            name = "Курсив / Каллиграфия",
            styleKey = "CURSIVE",
            price = 0.0,
            sampleText = "Иванов Иван Иванович\n1950 — 2024",
            description = "Изящный рукописный наклонный шрифт",
            sortOrder = 3
        ),
        EngravingFontItem(
            id = "font_antiqua",
            name = "Антиква / Модерн",
            styleKey = "ANTIQUE",
            price = 0.0,
            sampleText = "Иванов Иван Иванович\n1950 — 2024",
            description = "Элегантный европейский шрифт с тонкими линиями",
            sortOrder = 4
        ),
        EngravingFontItem(
            id = "font_sans",
            name = "Современный гротеск",
            styleKey = "SANS_SERIF",
            price = 0.0,
            sampleText = "Иванов Иван Иванович\n1950 — 2024",
            description = "Минималистичный ровный шрифт без засечек",
            sortOrder = 5
        ),
        EngravingFontItem(
            id = "font_gothic",
            name = "Монументальный (Технический)",
            styleKey = "MONOSPACE",
            price = 0.0,
            sampleText = "ИВАНОВ ИВАН ИВАНОВИЧ\n1950 — 2024",
            description = "Шрифт фиксированной ширины для четких геометрических надписей",
            sortOrder = 6
        )
    )

    fun getDefaultEngravingDrawings(): List<EngravingDrawingItem> = listOf(
        // --- Крест ---
        EngravingDrawingItem(
            id = "draw_cross_1",
            category = "Крест",
            code = "К-1",
            name = "Крест православный (восьмиконечный классический)",
            price = 35.0,
            description = "Традиционный православный крест",
            sortOrder = 0
        ),
        EngravingDrawingItem(
            id = "draw_cross_2",
            category = "Крест",
            code = "К-2",
            name = "Крест католический (латинский четырехконечный)",
            price = 35.0,
            description = "Строгий католический крест",
            sortOrder = 1
        ),
        EngravingDrawingItem(
            id = "draw_cross_3",
            category = "Крест",
            code = "К-3",
            name = "Крест резной трилистник с распятием",
            price = 45.0,
            description = "Фигурный крест с лучами и рельефным распятием",
            sortOrder = 2
        ),
        EngravingDrawingItem(
            id = "draw_cross_4",
            category = "Крест",
            code = "К-4",
            name = "Крест с плащаницей / драпировкой",
            price = 40.0,
            description = "Крест, обвитый траурным покрывалом",
            sortOrder = 3
        ),
        EngravingDrawingItem(
            id = "draw_cross_5",
            category = "Крест",
            code = "К-5",
            name = "Крест с терновым венцом",
            price = 40.0,
            description = "Символ мученичества и вечной памяти",
            sortOrder = 4
        ),

        // --- Цветок ---
        EngravingDrawingItem(
            id = "draw_flower_1",
            category = "Цветок",
            code = "Ц-1",
            name = "Две розы с лентой",
            price = 35.0,
            description = "Пара роз, перевязанных траурной лентой",
            sortOrder = 0
        ),
        EngravingDrawingItem(
            id = "draw_flower_2",
            category = "Цветок",
            code = "Ц-2",
            name = "Гвоздики (2 шт.)",
            price = 30.0,
            description = "Классические гвоздики памяти",
            sortOrder = 1
        ),
        EngravingDrawingItem(
            id = "draw_flower_3",
            category = "Цветок",
            code = "Ц-3",
            name = "Ветка роз с листьями и бутонами",
            price = 35.0,
            description = "Художественная ветка цветущих роз",
            sortOrder = 2
        ),
        EngravingDrawingItem(
            id = "draw_flower_4",
            category = "Цветок",
            code = "Ц-4",
            name = "Тюльпаны (букет)",
            price = 30.0,
            description = "Нежный весенний букет тюльпанов",
            sortOrder = 3
        ),
        EngravingDrawingItem(
            id = "draw_flower_5",
            category = "Цветок",
            code = "Ц-5",
            name = "Ветка березы / Лилии",
            price = 35.0,
            description = "Символ чистоты и скорби",
            sortOrder = 4
        ),

        // --- Иконы ---
        EngravingDrawingItem(
            id = "draw_icon_1",
            category = "Иконы",
            code = "И-1",
            name = "Божия Матерь Казанская",
            price = 65.0,
            description = "Почитаемый образ Богородицы",
            sortOrder = 0
        ),
        EngravingDrawingItem(
            id = "draw_icon_2",
            category = "Иконы",
            code = "И-2",
            name = "Спас Нерукотворный (Иисус Христос)",
            price = 65.0,
            description = "Канонический лик Спасителя",
            sortOrder = 1
        ),
        EngravingDrawingItem(
            id = "draw_icon_3",
            category = "Иконы",
            code = "И-3",
            name = "Святой Николай Чудотворец",
            price = 65.0,
            description = "Икона святителя Николая",
            sortOrder = 2
        ),
        EngravingDrawingItem(
            id = "draw_icon_4",
            category = "Иконы",
            code = "И-4",
            name = "Семистрельная Богородица (Умягчение злых сердец)",
            price = 65.0,
            description = "Икона Богоматери",
            sortOrder = 3
        ),
        EngravingDrawingItem(
            id = "draw_icon_5",
            category = "Иконы",
            code = "И-5",
            name = "Икона Умиление / Владимирская",
            price = 65.0,
            description = "Образ Пресвятой Девы с Младенцем",
            sortOrder = 4
        ),

        // --- Ангелы ---
        EngravingDrawingItem(
            id = "draw_angel_1",
            category = "Ангелы",
            code = "А-1",
            name = "Ангел скорбящий (склоненный)",
            price = 55.0,
            description = "Скорбящий ангел у надгробия",
            sortOrder = 0
        ),
        EngravingDrawingItem(
            id = "draw_angel_2",
            category = "Ангелы",
            code = "А-2",
            name = "Ангел со свечой памяти",
            price = 55.0,
            description = "Хранитель, держащий зажженную свечу",
            sortOrder = 1
        ),
        EngravingDrawingItem(
            id = "draw_angel_3",
            category = "Ангелы",
            code = "А-3",
            name = "Ангел-хранитель с раскрытыми крыльями",
            price = 60.0,
            description = "Величественный ангел с распростертыми крыльями",
            sortOrder = 2
        ),
        EngravingDrawingItem(
            id = "draw_angel_4",
            category = "Ангелы",
            code = "А-4",
            name = "Маленький спящий ангелочек (детский)",
            price = 50.0,
            description = "Нежный образ маленького херувима",
            sortOrder = 3
        ),
        EngravingDrawingItem(
            id = "draw_angel_5",
            category = "Ангелы",
            code = "А-5",
            name = "Молящийся ангел на облаке",
            price = 55.0,
            description = "Коленопреклоненный ангел в молитве",
            sortOrder = 4
        ),

        // --- Свечи ---
        EngravingDrawingItem(
            id = "draw_candle_1",
            category = "Свечи",
            code = "С-1",
            name = "Свеча с розой",
            price = 30.0,
            description = "Горящая свеча у подножия бутона розы",
            sortOrder = 0
        ),
        EngravingDrawingItem(
            id = "draw_candle_2",
            category = "Свечи",
            code = "С-2",
            name = "Две свечи скорби и вечной памяти",
            price = 35.0,
            description = "Пара зажженных свечей разной высоты",
            sortOrder = 1
        ),
        EngravingDrawingItem(
            id = "draw_candle_3",
            category = "Свечи",
            code = "С-3",
            name = "Свеча в молитвенных ладонях",
            price = 40.0,
            description = "Руки, оберегающие пламя свечи",
            sortOrder = 2
        ),
        EngravingDrawingItem(
            id = "draw_candle_4",
            category = "Свечи",
            code = "С-4",
            name = "Одиночная свеча с драпировкой",
            price = 30.0,
            description = "Строгая вертикальная свеча с лентой",
            sortOrder = 3
        ),
        EngravingDrawingItem(
            id = "draw_candle_5",
            category = "Свечи",
            code = "С-5",
            name = "Лампада с горящим огоньком",
            price = 35.0,
            description = "Резная лампада с ярким пламенем",
            sortOrder = 4
        )
    )
    fun getDefaultConstructorServicePrices(): List<ConstructorServicePriceItem> = listOf(
        ConstructorServicePriceItem(
            key = "usd_exchange_rate",
            groupName = "Настройки",
            title = "Курс USD к BYN",
            price = 3.25,
            description = "Курс доллара для расчетов и отображения",
            sortOrder = 0
        ),
        ConstructorServicePriceItem(
            key = "eur_exchange_rate",
            groupName = "Настройки",
            title = "Курс EUR к BYN",
            price = 3.55,
            description = "Курс евро для расчетов сусального золота и отображения",
            sortOrder = 1
        ),
        ConstructorServicePriceItem(
            key = "letter_standard",
            groupName = "Гравировка",
            title = "Гравировка 1 знака ФИО/дат (обычный шрифт)",
            price = 1.60,
            description = "Цена за 1 знак букв/цифр ФИО в Конструкторе памятника и Замерах (BYN)",
            sortOrder = 2
        ),
        ConstructorServicePriceItem(
            key = "letter_painted",
            groupName = "Гравировка",
            title = "Покраска букв (1 знак / буква)",
            price = 10.00,
            description = "Стоимость покраски 1 знака (буквы/цифры) в Конструкторе памятника (BYN)",
            sortOrder = 3
        ),
        ConstructorServicePriceItem(
            key = "letter_gold_large",
            groupName = "Гравировка",
            title = "Сусальное золото: большие буквы (EUR)",
            price = 8.00,
            description = "Тариф в евро (€) за 1 большую (заглавную) букву",
            sortOrder = 4
        ),
        ConstructorServicePriceItem(
            key = "letter_gold_small",
            groupName = "Гравировка",
            title = "Сусальное золото: маленькие буквы и цифры (EUR)",
            price = 7.00,
            description = "Тариф в евро (€) за 1 строчную букву или цифру (цифры считаются за маленькие)",
            sortOrder = 5
        ),
        ConstructorServicePriceItem(
            key = "letter_gold",
            groupName = "Гравировка",
            title = "Гравировка 1 знака с сусальным золотом (базовая)",
            price = 5.50,
            description = "Базовый тариф сусального золота",
            sortOrder = 5
        ),
        ConstructorServicePriceItem(
            key = "letter_epitaph",
            groupName = "Гравировка",
            title = "Гравировка 1 знака эпитафии",
            price = 1.50,
            description = "Цена за 1 знак эпитафии в Конструкторе памятника (BYN)",
            sortOrder = 6
        ),
        ConstructorServicePriceItem(
            key = "measure_delivery_base_price",
            groupName = "Доставка",
            title = "Базовая доставка спецтранспортом",
            price = 60.0,
            description = "Фиксированная стоимость рейса с погрузкой/разгрузкой",
            sortOrder = 4
        ),
        ConstructorServicePriceItem(
            key = "measure_delivery_base_km",
            groupName = "Доставка",
            title = "Включенное расстояние в базовый тариф",
            price = 15.0,
            description = "Километров от базы/мастерской без доплаты",
            sortOrder = 5
        ),
        ConstructorServicePriceItem(
            key = "measure_delivery_km_price",
            groupName = "Доставка",
            title = "Доплата за каждый километр свыше нормы",
            price = 1.50,
            description = "Тариф за выезд за пределы включенного расстояния",
            sortOrder = 6
        ),
        ConstructorServicePriceItem(
            key = "measure_fence_price_pm",
            groupName = "Замеры",
            title = "Базовая стоимость оград (замер)",
            price = 45.0,
            description = "Стоимость за погонный метр в калькуляторе замеров оград",
            sortOrder = 7
        ),
        ConstructorServicePriceItem(
            key = "measure_tile_price_sqm",
            groupName = "Замеры",
            title = "Базовая стоимость укладки плитки (замер)",
            price = 85.0,
            description = "Стоимость за 1 м² в калькуляторе замеров плитки",
            sortOrder = 8
        ),
        ConstructorServicePriceItem(
            key = "installation_base_price",
            groupName = "Установка и монтаж",
            title = "Монтаж (установка) памятника",
            price = 500.0,
            description = "Базовая стоимость установки комплекта памятника на кладбище (BYN)",
            sortOrder = 9
        ),
        ConstructorServicePriceItem(
            key = "demontazh_base_price",
            groupName = "Установка и монтаж",
            title = "Демонтаж старого памятника",
            price = 150.0,
            description = "Стоимость демонтажа старого памятника / креста (BYN)",
            sortOrder = 10
        )
    )

    fun getDefaultPhotoSizes(): List<PhotoSizeItem> = listOf(
        // Фотокерамика
        PhotoSizeItem(id = "ps_ceram_13x18", category = "Фотокерамика", sizeName = "13×18 см", price = 120.0, description = "Классический овал/прямоугольник керамика", sortOrder = 0),
        PhotoSizeItem(id = "ps_ceram_18x24", category = "Фотокерамика", sizeName = "18×24 см", price = 160.0, description = "Средний размер портрета керамика", sortOrder = 1),
        PhotoSizeItem(id = "ps_ceram_20x30", category = "Фотокерамика", sizeName = "20×30 см", price = 210.0, description = "Увеличенный формат фотокерамика", sortOrder = 2),
        PhotoSizeItem(id = "ps_ceram_24x30", category = "Фотокерамика", sizeName = "24×30 см", price = 260.0, description = "Большой портретный овал керамика", sortOrder = 3),
        PhotoSizeItem(id = "ps_ceram_30x40", category = "Фотокерамика", sizeName = "30×40 см", price = 340.0, description = "Парадный портрет керамика", sortOrder = 4),

        // Металлокерамика
        PhotoSizeItem(id = "ps_metal_13x18", category = "Металлокерамика", sizeName = "13×18 см", price = 90.0, description = "Металлокерамическая табличка 13x18", sortOrder = 0),
        PhotoSizeItem(id = "ps_metal_18x24", category = "Металлокерамика", sizeName = "18×24 см", price = 120.0, description = "Металлокерамика 18x24", sortOrder = 1),
        PhotoSizeItem(id = "ps_metal_20x30", category = "Металлокерамика", sizeName = "20×30 см", price = 170.0, description = "Металлокерамика 20x30", sortOrder = 2),

        // Фото на стекле
        PhotoSizeItem(id = "ps_glass_18x24", category = "Фото на стекле", sizeName = "18×24 см", price = 280.0, description = "Закаленное триплекс стекло с уф-печатью", sortOrder = 0),
        PhotoSizeItem(id = "ps_glass_20x30", category = "Фото на стекле", sizeName = "20×30 см", price = 360.0, description = "Стекло с высокой четкостью 20x30", sortOrder = 1),
        PhotoSizeItem(id = "ps_glass_30x40", category = "Фото на стекле", sizeName = "30×40 см", price = 480.0, description = "Формат 30x40 премиум фото на стекле", sortOrder = 2),
        PhotoSizeItem(id = "ps_glass_40x60", category = "Фото на стекле", sizeName = "40×60 см", price = 750.0, description = "Большой ростовой/панорамный портрет на стекле", sortOrder = 3),

        // Гравировка портрета на граните
        PhotoSizeItem(id = "ps_engr_30x40", category = "Гравировка портрета", sizeName = "30×40 см (погрудный)", price = 200.0, description = "Художественная ручная/компьютерная гравировка портрета", sortOrder = 0),
        PhotoSizeItem(id = "ps_engr_full_height", category = "Гравировка портрета", sizeName = "В полный рост", price = 450.0, description = "Гравировка человека во весь рост на стеле", sortOrder = 1)
    )

    fun getDefaultPhotoFrames(): List<PhotoFrameItem> = listOf(
        PhotoFrameItem(id = "pf_none", name = "Без рамки", materialType = "Без рамки", price = 0.0, description = "Монтаж портрета без декоративной рамки", sortOrder = 0),
        PhotoFrameItem(id = "pf_bronze", name = "Бронза (Caggiati)", materialType = "Бронза", price = 180.0, description = "Итальянская бронзовая рамка для фото", sortOrder = 1),
        PhotoFrameItem(id = "pf_aluminum", name = "Алюминий", materialType = "Алюминий", price = 90.0, description = "Легкая алюминиевая рамка с защитным покрытием", sortOrder = 2),
        PhotoFrameItem(id = "pf_granite", name = "Гранитная рамка / фаска", materialType = "Гранит", price = 120.0, description = "Вырезанная рамка из гранита по периметру фото", sortOrder = 3),
        PhotoFrameItem(id = "pf_recessed", name = "Врезка / Ниша в камне", materialType = "Врезка", price = 60.0, description = "Подготовка глубокой ниши в граните под вклейку фото", sortOrder = 4)
    )

    fun getDefaultVases(): List<VaseItem> = listOf(
        // Гранитные вазы
        VaseItem(id = "v_granite_25", name = "Ваза гранитная 25 см (Габбро)", materialType = "Гранит", sizeCm = "25 см", price = 110.0, description = "Черный гранит Габбро-Диабаз 25 см", sortOrder = 0),
        VaseItem(id = "v_granite_30", name = "Ваза гранитная 30 см (Габбро)", materialType = "Гранит", sizeCm = "30 см", price = 140.0, description = "Черный гранит Габбро-Диабаз 30 см", sortOrder = 1),
        VaseItem(id = "v_granite_35", name = "Ваза гранитная 35 см (Габбро)", materialType = "Гранит", sizeCm = "35 см", price = 180.0, description = "Черный гранит Габбро-Диабаз 35 см", sortOrder = 2),
        VaseItem(id = "v_granite_red_30", name = "Ваза гранитная 30 см (Лезниковский)", materialType = "Гранит", sizeCm = "30 см", price = 170.0, description = "Красный гранит Лезники 30 см", sortOrder = 3),
        VaseItem(id = "v_granite_gray_30", name = "Ваза гранитная 30 см (Мансуровский)", materialType = "Гранит", sizeCm = "30 см", price = 160.0, description = "Светлый гранит 30 см", sortOrder = 4),

        // Кованые вазы
        VaseItem(id = "v_forged_25", name = "Ваза кованая 25 см (Черная)", materialType = "Кованый металл", sizeCm = "25 см", price = 85.0, description = "Кованый металл с антикоррозийным покрытием 25 см", sortOrder = 5),
        VaseItem(id = "v_forged_30", name = "Ваза кованая 30 см (С золото-патиной)", materialType = "Кованый металл", sizeCm = "30 см", price = 115.0, description = "Кованая ажурная ваза с ручной золотой патиной 30 см", sortOrder = 6),
        VaseItem(id = "v_forged_bronze_30", name = "Ваза кованая 30 см (Бронзовая патина)", materialType = "Кованый металл", sizeCm = "30 см", price = 120.0, description = "Кованый металл с бронзовым отблеском 30 см", sortOrder = 7),

        // Полуваза
        VaseItem(id = "v_half_granite_30", name = "Полуваза гранитная накладная 30 см", materialType = "Гранит", sizeCm = "30 см", price = 95.0, description = "Накладная полуваза из гранита для установки на стелу", sortOrder = 8)
    )
}


typealias DefaultPriceCatalog = DefaultCatalog
