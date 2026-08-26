package com.example.data

object DefaultCatalog {
    fun getItems(): List<PriceItem> = getDefaultItems()

    fun getDefaultItems(): List<PriceItem> = listOf(
        // === ПАМЯТНИКИ (MONUMENTS) ===
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Комплекты Габбро-Диабаз (Карелия)",
            name = "Памятник 80×40×5 см (стела, тумба, цветник)",
            unit = "компл",
            defaultPrice = 650.0,
            currentPrice = 650.0,
            description = "Стела 80x40x5, подставка 50x20x15, цветник 80x50x5. Полировка круговая 5 сторон.",
            sortOrder = 1
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Комплекты Габбро-Диабаз (Карелия)",
            name = "Памятник 100×50×8 см (стела, тумба, цветник)",
            unit = "компл",
            defaultPrice = 960.0,
            currentPrice = 960.0,
            description = "Стела 100x50x8, подставка 60x20x15, цветник 100x60x8. Круговая полировка.",
            sortOrder = 2
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Комплекты Габбро-Диабаз (Карелия)",
            name = "Памятник 120×60×8 см (стела, тумба, цветник)",
            unit = "компл",
            defaultPrice = 1340.0,
            currentPrice = 1340.0,
            description = "Стела 120x60x8, подставка 70x20x15, цветник 120x70x8. Классический прямоугольный.",
            sortOrder = 3
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Фигурные памятники",
            name = "Фигурная резка стелы (Волна / Плечики / Арка)",
            unit = "шт",
            defaultPrice = 150.0,
            currentPrice = 150.0,
            description = "Художественная обработка фаски и верхней грани гранитной плиты.",
            sortOrder = 4
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Фигурные памятники",
            name = "Стела с резным крестом / березкой / ангелом",
            unit = "компл",
            defaultPrice = 1650.0,
            currentPrice = 1650.0,
            description = "Глубокая барельефная резьба по граниту Габбро-Диабаз 100x50x8.",
            sortOrder = 5
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Семейные (двойные) памятники",
            name = "Горизонтальный памятник 100×60×8 см на двоих",
            unit = "компл",
            defaultPrice = 1240.0,
            currentPrice = 1240.0,
            description = "Стела 100x60x8 горизонт, подставка 110x20x15, цветник 100x80x8.",
            sortOrder = 6
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Установка памятников",
            name = "Монтаж памятника на железобетонные балки",
            unit = "компл",
            defaultPrice = 260.0,
            currentPrice = 260.0,
            description = "Включает швеллеры/балки перекрытия, заливку раствора, установку по уровню.",
            sortOrder = 7
        ),
        PriceItem(
            category = ItemCategory.MONUMENTS.displayName,
            subcategory = "Установка памятников",
            name = "Демонтаж старого металлического/бетонного памятника",
            unit = "услуга",
            defaultPrice = 90.0,
            currentPrice = 90.0,
            description = "Разборка старого надгробия, вынос к месту сбора мусора.",
            sortOrder = 8
        ),

        // === ХУДОЖЕСТВЕННОЕ ОФОРМЛЕНИЕ И ГРАВИРОВКА (ENGRAVING) ===
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Портреты на граните",
            name = "Гравировка портрета станочная (лазер/ударная)",
            unit = "шт",
            defaultPrice = 150.0,
            currentPrice = 150.0,
            description = "Формат А4 (до 30x40 см) с ретушью и цветокоррекцией фото.",
            sortOrder = 10
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Портреты на граните",
            name = "Ручная доработка портрета художником",
            unit = "шт",
            defaultPrice = 110.0,
            currentPrice = 110.0,
            description = "Углубление полутонов, прорисовка волос и глаз профессиональным художником.",
            sortOrder = 11
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Портреты на граните",
            name = "Фотокерамика на памятник (овал/прямоугольник)",
            unit = "шт",
            defaultPrice = 120.0,
            currentPrice = 120.0,
            description = "Запекание при 900°C, гарантия от выгорания 30 лет. Размер 13x18 см.",
            sortOrder = 12
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Портреты на граните",
            name = "Фото на осветленном стекле триплекс (20×30 см)",
            unit = "шт",
            defaultPrice = 290.0,
            currentPrice = 290.0,
            description = "Премиальное изображение высокой четкости внутри каленого стекла.",
            sortOrder = 13
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Надписи и знаки",
            name = "Гравировка знака (буква, цифра ФИО и дат)",
            unit = "знак",
            defaultPrice = 1.60,
            currentPrice = 1.60,
            description = "Стандартный академический или курсивный шрифт на полированном граните.",
            sortOrder = 14
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Надписи и знаки",
            name = "Покрытие надписей сусальным золотом / краской",
            unit = "знак",
            defaultPrice = 4.50,
            currentPrice = 4.50,
            description = "Глубокая рубка знаков 'в ус' с покрытием золотой краской/сусалью.",
            sortOrder = 15
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Символика и эпитафии",
            name = "Гравировка креста / полумесяца / звезды",
            unit = "шт",
            defaultPrice = 30.0,
            currentPrice = 30.0,
            description = "Контурный или объемный религиозный символ 15-20 см.",
            sortOrder = 16
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Символика и эпитафии",
            name = "Гравировка цветов (розы, гвоздики 2 шт)",
            unit = "шт",
            defaultPrice = 45.0,
            currentPrice = 45.0,
            description = "Художественное изображение цветов у подножия стелы.",
            sortOrder = 17
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Символика и эпитафии",
            name = "Гравировка пейзажа / иконы на обратной стороне",
            unit = "шт",
            defaultPrice = 190.0,
            currentPrice = 190.0,
            description = "Храм, природа, скорбящая Богородица во весь рост на тыльной стороне.",
            sortOrder = 18
        ),
        PriceItem(
            category = ItemCategory.ENGRAVING.displayName,
            subcategory = "Символика и эпитафии",
            name = "Гидрофобное защитное покрытие «Антидождь»",
            unit = "услуга",
            defaultPrice = 50.0,
            currentPrice = 50.0,
            description = "Портрет и надписи остаются яркими и контрастными во время дождя.",
            sortOrder = 19
        ),

        // === ОГРАДЫ, ЦОКОЛИ И БЛАГОУСТРОЙСТВО (FENCES_GROUND) ===
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Металлические ограды",
            name = "Ограда профильная сварная (высота 40-50 см)",
            unit = "м.п.",
            defaultPrice = 45.0,
            currentPrice = 45.0,
            description = "Труба профильная 20x20, столб 30x30, полимерно-порошковая окраска.",
            sortOrder = 20
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Металлические ограды",
            name = "Ограда кованая с элементами декора",
            unit = "м.п.",
            defaultPrice = 110.0,
            currentPrice = 110.0,
            description = "Кованый прут 12 мм, пики, волюты, патинирование под медь/бронзу.",
            sortOrder = 21
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Монтаж оград и цоколей",
            name = "Установка металлической ограды (бетонирование столбов)",
            unit = "точка",
            defaultPrice = 25.0,
            currentPrice = 25.0,
            description = "Бурение лунок, заливка бетоном опорных столбов с калиткой.",
            sortOrder = 22
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Монтаж оград и цоколей",
            name = "Бетонный армированный цоколь (ленточный фундамент)",
            unit = "м.п.",
            defaultPrice = 95.0,
            currentPrice = 95.0,
            description = "Опалубка, армирование 10 мм арматурой, заливка бетона М300 20x20 см.",
            sortOrder = 23
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Монтаж оград и цоколей",
            name = "Гранитный цоколь с шарами и столбиками",
            unit = "м.п.",
            defaultPrice = 280.0,
            currentPrice = 280.0,
            description = "Брус гранитный 15x10 см Габбро-Диабаз, столбики 15x15 с шарами.",
            sortOrder = 24
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Благоустройство участка",
            name = "Укладка тротуарной плитки с бордюром 'под ключ'",
            unit = "м²",
            defaultPrice = 85.0,
            currentPrice = 85.0,
            description = "Выемка грунта, геотекстиль, песчано-щебеночная подушка, плитка и бордюр.",
            sortOrder = 25
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Благоустройство участка",
            name = "Облицовка гранитной плиткой (30×30 или 60×30)",
            unit = "м²",
            defaultPrice = 190.0,
            currentPrice = 190.0,
            description = "Полированный гранит Габбро, морозостойкий двухкомпонентный клей.",
            sortOrder = 26
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Благоустройство участка",
            name = "Отсыпка декоративным щебнем / мраморной крошкой",
            unit = "м²",
            defaultPrice = 35.0,
            currentPrice = 35.0,
            description = "Плотный геотекстиль, слой щебня или крошки толщиной 4-5 см.",
            sortOrder = 27
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Благоустройство участка",
            name = "Укладка искусственного газона",
            unit = "м²",
            defaultPrice = 48.0,
            currentPrice = 48.0,
            description = "Синтетическое покрытие с дренажем, устойчивое к УФ и осадкам.",
            sortOrder = 28
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Благоустройство участка",
            name = "Столик и лавочка металлические (комплект)",
            unit = "компл",
            defaultPrice = 210.0,
            currentPrice = 210.0,
            description = "Сварной каркас, деревянный брус со спецпропиткой, монтаж в грунт.",
            sortOrder = 29
        ),
        PriceItem(
            category = ItemCategory.FENCES_GROUND.displayName,
            subcategory = "Благоустройство участка",
            name = "Столик и лавочка гранитные (Габбро)",
            unit = "компл",
            defaultPrice = 750.0,
            currentPrice = 750.0,
            description = "Столешница 50x50, лавка 80x25, гранитные ножки-опоры.",
            sortOrder = 30
        ),

        // === РИТУАЛЬНЫЕ УСЛУГИ И ЗАХОРОНЕНИЕ (BURIAL_SERVICES) ===
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Копка могилы и подготовка",
            name = "Рытье могилы стандартное ручное (лето)",
            unit = "услуга",
            defaultPrice = 280.0,
            currentPrice = 280.0,
            description = "Глубина 1.8-2.0 м, зачистка дна, оформление лапником, закопка и холм.",
            sortOrder = 40
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Копка могилы и подготовка",
            name = "Рытье могилы в зимний период / сложный грунт",
            unit = "услуга",
            defaultPrice = 390.0,
            currentPrice = 390.0,
            description = "Прогрев грунта, долбление мерзлоты/корней, формирование могильного холма.",
            sortOrder = 41
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Копка могилы и подготовка",
            name = "Драпировка могилы тканью + сингуматор (лифт)",
            unit = "услуга",
            defaultPrice = 210.0,
            currentPrice = 210.0,
            description = "Тканевая драпировка стен могилы, ритуальный механический лифт опускания.",
            sortOrder = 42
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Бригада сопровождения",
            name = "Бригада сопровождения и выноса (4 человека)",
            unit = "услуга",
            defaultPrice = 200.0,
            currentPrice = 200.0,
            description = "Вынос гроба из морга/дома, погрузка, сопровождение, опускание в могилу.",
            sortOrder = 43
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Автокатафалк",
            name = "Автокатафалк (Морг — Зал прощания — Кладбище)",
            unit = "рейс",
            defaultPrice = 180.0,
            currentPrice = 180.0,
            description = "Специализированный микроавтобус с подиумом и местами для родственников (до 3 ч).",
            sortOrder = 44
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Автокатафалк",
            name = "Дополнительный час работы катафалка",
            unit = "час",
            defaultPrice = 40.0,
            currentPrice = 40.0,
            description = "Ожидание или дополнительный маршрут к месту поминальной трапезы.",
            sortOrder = 45
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Ритуальные принадлежности",
            name = "Гроб обитый тканью (шелк/бархат) стандарт",
            unit = "шт",
            defaultPrice = 160.0,
            currentPrice = 160.0,
            description = "В комплекте постель, подушка, покрывало, ручки, закрутки.",
            sortOrder = 46
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Ритуальные принадлежности",
            name = "Гроб полированный деревянный (сосна/дуб)",
            unit = "шт",
            defaultPrice = 580.0,
            currentPrice = 580.0,
            description = "Четырехгранный или шестигранный лакированный, премиум фурнитура.",
            sortOrder = 47
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Ритуальные принадлежности",
            name = "Крест на могилу деревянный временный (сосна)",
            unit = "шт",
            defaultPrice = 65.0,
            currentPrice = 65.0,
            description = "Восьмиконечный лакированный крест с табличкой ФИО/даты.",
            sortOrder = 48
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Ритуальные принадлежности",
            name = "Крест металлический с порошковой покраской",
            unit = "шт",
            defaultPrice = 110.0,
            currentPrice = 110.0,
            description = "Прочная сварная конструкция с коваными узорами и табличкой.",
            sortOrder = 49
        ),
        PriceItem(
            category = ItemCategory.BURIAL_SERVICES.displayName,
            subcategory = "Ритуальные принадлежности",
            name = "Венок ритуальный заказной с лентой 120 см",
            unit = "шт",
            defaultPrice = 75.0,
            currentPrice = 75.0,
            description = "Качественные искусственные цветы, хвоя, атласная траурная лента с надписью.",
            sortOrder = 50
        ),

        // === ПРОЧЕЕ И АКСЕССУАРЫ (OTHER) ===
        PriceItem(
            category = ItemCategory.OTHER.displayName,
            subcategory = "Аксессуары из гранита и полимергранита",
            name = "Ваза гранитная 25-30 см (Габбро-Диабаз)",
            unit = "шт",
            defaultPrice = 130.0,
            currentPrice = 130.0,
            description = "Точеная гранитная ваза с дренажным отверстием для стока воды.",
            sortOrder = 60
        ),
        PriceItem(
            category = ItemCategory.OTHER.displayName,
            subcategory = "Аксессуары из гранита и полимергранита",
            name = "Лампада гранитная со стеклом и металлом",
            unit = "шт",
            defaultPrice = 150.0,
            currentPrice = 150.0,
            description = "Защищенная лампада для свечей, монтаж на двухкомпонентный клей.",
            sortOrder = 61
        ),
        PriceItem(
            category = ItemCategory.OTHER.displayName,
            subcategory = "Транспорт и доставка",
            name = "Доставка памятника/комплекта на кладбище (город)",
            unit = "рейс",
            defaultPrice = 60.0,
            currentPrice = 60.0,
            description = "Транспортировка на кладбище в черте города и разгрузка манипулятором.",
            sortOrder = 62
        ),
        PriceItem(
            category = ItemCategory.OTHER.displayName,
            subcategory = "Транспорт и доставка",
            name = "Доставка за город (межгород)",
            unit = "км",
            defaultPrice = 1.50,
            currentPrice = 1.50,
            description = "Тариф за каждый километр от черты города до кладбища.",
            sortOrder = 63
        ),
        PriceItem(
            category = ItemCategory.OTHER.displayName,
            subcategory = "Уход за захоронением",
            name = "Разовая уборка и мытье памятника",
            unit = "услуга",
            defaultPrice = 50.0,
            currentPrice = 50.0,
            description = "Очистка от листвы/сорняков, мойка камня спецсредствами, фотоотчет заказчику.",
            sortOrder = 64
        )
    )

    fun getDefaultStoneMaterials(): List<StoneMaterialItem> = listOf(
        StoneMaterialItem(
            id = "gabbro",
            name = "Габбро-Диабаз (Карелия)",
            colorName = "Черный глубокий",
            pricePerM3 = 4200.0,
            origin = "Карелия",
            description = "Классический черный гранит, идеален для портретов и контрастной гравировки.",
            suitableForDirectEngraving = true,
            sortOrder = 1
        ),
        StoneMaterialItem(
            id = "dymovsky",
            name = "Дымовский / Балтийский",
            colorName = "Коричнево-бордовый",
            pricePerM3 = 5600.0,
            origin = "Ленинградская обл.",
            description = "Благородный гранит теплого шоколадно-красноватого оттенка.",
            suitableForDirectEngraving = false,
            sortOrder = 2
        ),
        StoneMaterialItem(
            id = "mansurovsky",
            name = "Мансуровский",
            colorName = "Светло-серый / белый",
            pricePerM3 = 5200.0,
            origin = "Башкортостан, Урал",
            description = "Самый светлый гранит. Смотрится монументально и воздушно.",
            suitableForDirectEngraving = false,
            sortOrder = 3
        ),
        StoneMaterialItem(
            id = "pokostovsky",
            name = "Покостовский",
            colorName = "Серый крапчатый",
            pricePerM3 = 4800.0,
            origin = "Житомирская обл.",
            description = "Прочный серый гранит с равномерной зернистой структурой.",
            suitableForDirectEngraving = false,
            sortOrder = 4
        ),
        StoneMaterialItem(
            id = "leznikovsky",
            name = "Лезниковский (малиновый)",
            colorName = "Красный / малиновый",
            pricePerM3 = 7800.0,
            origin = "Житомирская обл.",
            description = "Элитный высокопрочный красный гранит высшей категории.",
            suitableForDirectEngraving = false,
            sortOrder = 5
        ),
        StoneMaterialItem(
            id = "serpentinite",
            name = "Змеевик (Серпентинит)",
            colorName = "Темно-зеленый",
            pricePerM3 = 6500.0,
            origin = "Урал, Россия",
            description = "Уникальный природный зеленый камень с шелковистым узором.",
            suitableForDirectEngraving = false,
            sortOrder = 6
        ),
        StoneMaterialItem(
            id = "baltic_green",
            name = "Балтик Грин (Baltic Green)",
            colorName = "Крупнозернистый зеленый",
            pricePerM3 = 7000.0,
            origin = "Финляндия / Карелия",
            description = "Премиальный финский зеленый гранит с неповторимой текстурой.",
            suitableForDirectEngraving = false,
            sortOrder = 7
        ),
        StoneMaterialItem(
            id = "labradorite",
            name = "Лабрадорит (Иризирующий)",
            colorName = "Черно-синий с переливом",
            pricePerM3 = 7200.0,
            origin = "Украина / Житомир",
            description = "Глубокий камень с сине-голубыми переливающимися кристаллами.",
            suitableForDirectEngraving = false,
            sortOrder = 8
        ),
        StoneMaterialItem(
            id = "koelga",
            name = "Мрамор Коелга",
            colorName = "Белый мрамор",
            pricePerM3 = 3800.0,
            origin = "Челябинская обл., Урал",
            description = "Натуральный белый мрамор с нежными серыми прожилками.",
            suitableForDirectEngraving = false,
            sortOrder = 9
        ),
        StoneMaterialItem(
            id = "polymer",
            name = "Литьевой Гранитополимер",
            colorName = "Черный глянец",
            pricePerM3 = 2200.0,
            origin = "Фабричное литье",
            description = "Легкий полимерный композит, бюджетная альтернатива камню.",
            suitableForDirectEngraving = true,
            sortOrder = 10
        )
    )

    fun getDefaultSizePresets(): List<MonumentSizePresetItem> = listOf(
        MonumentSizePresetItem(
            id = "80x40x5",
            name = "80 × 40 × 5 см (Эконом)",
            heightCm = 80,
            widthCm = 40,
            thicknessCm = 5,
            steleBasePrice = 380.0,
            plinthBasePrice = 140.0,
            flowerbedBasePrice = 120.0,
            plinthDimensions = "50×20×15 см",
            flowerbedDimensions = "80×50×5 см",
            sortOrder = 1
        ),
        MonumentSizePresetItem(
            id = "100x50x8",
            name = "100 × 50 × 8 см (Стандарт)",
            heightCm = 100,
            widthCm = 50,
            thicknessCm = 8,
            steleBasePrice = 620.0,
            plinthBasePrice = 170.0,
            flowerbedBasePrice = 170.0,
            plinthDimensions = "60×20×15 см",
            flowerbedDimensions = "100×60×8 см",
            sortOrder = 2
        ),
        MonumentSizePresetItem(
            id = "100x50x10",
            name = "100 × 50 × 10 см (Стандарт массив)",
            heightCm = 100,
            widthCm = 50,
            thicknessCm = 10,
            steleBasePrice = 760.0,
            plinthBasePrice = 210.0,
            flowerbedBasePrice = 170.0,
            plinthDimensions = "60×20×20 см",
            flowerbedDimensions = "100×60×8 см",
            sortOrder = 3
        ),
        MonumentSizePresetItem(
            id = "110x50x8",
            name = "110 × 50 × 8 см (Увеличенный)",
            heightCm = 110,
            widthCm = 50,
            thicknessCm = 8,
            steleBasePrice = 720.0,
            plinthBasePrice = 170.0,
            flowerbedBasePrice = 170.0,
            plinthDimensions = "60×20×15 см",
            flowerbedDimensions = "100×60×8 см",
            sortOrder = 4
        ),
        MonumentSizePresetItem(
            id = "120x60x8",
            name = "120 × 60 × 8 см (Престиж)",
            heightCm = 120,
            widthCm = 60,
            thicknessCm = 8,
            steleBasePrice = 900.0,
            plinthBasePrice = 220.0,
            flowerbedBasePrice = 220.0,
            plinthDimensions = "70×20×15 см",
            flowerbedDimensions = "120×70×8 см",
            sortOrder = 5
        ),
        MonumentSizePresetItem(
            id = "120x60x10",
            name = "120 × 60 × 10 см (Престиж плюс)",
            heightCm = 120,
            widthCm = 60,
            thicknessCm = 10,
            steleBasePrice = 1080.0,
            plinthBasePrice = 260.0,
            flowerbedBasePrice = 220.0,
            plinthDimensions = "70×20×20 см",
            flowerbedDimensions = "120×70×8 см",
            sortOrder = 6
        ),
        MonumentSizePresetItem(
            id = "140x70x10",
            name = "140 × 70 × 10 см (Монумент элит)",
            heightCm = 140,
            widthCm = 70,
            thicknessCm = 10,
            steleBasePrice = 1500.0,
            plinthBasePrice = 320.0,
            flowerbedBasePrice = 300.0,
            plinthDimensions = "80×25×20 см",
            flowerbedDimensions = "140×80×10 см",
            sortOrder = 7
        ),
        MonumentSizePresetItem(
            id = "family_100x60x8",
            name = "100 × 60 × 8 см (Горизонтальный на 2 персоны)",
            heightCm = 60,
            widthCm = 100,
            thicknessCm = 8,
            steleBasePrice = 760.0,
            plinthBasePrice = 260.0,
            flowerbedBasePrice = 220.0,
            plinthDimensions = "110×20×15 см",
            flowerbedDimensions = "100×80×8 см",
            isFamily = true,
            sortOrder = 8
        ),
        MonumentSizePresetItem(
            id = "family_120x70x8",
            name = "120 × 70 × 8 см (Горизонтальный большой)",
            heightCm = 70,
            widthCm = 120,
            thicknessCm = 8,
            steleBasePrice = 1050.0,
            plinthBasePrice = 310.0,
            flowerbedBasePrice = 280.0,
            plinthDimensions = "130×20×15 см",
            flowerbedDimensions = "120×90×8 см",
            isFamily = true,
            sortOrder = 9
        )
    )

    fun getDefaultConstructorServicePrices(): List<ConstructorServicePriceItem> = listOf(
        // Художественное оформление и резка
        ConstructorServicePriceItem(
            key = "carving_wave",
            groupName = "Художественное оформление",
            title = "Фигурная резка 'Волна / Плечики / Арка'",
            price = 150.0,
            description = "Плавные изгибы, фасонные скосы по верхней грани",
            sortOrder = 1
        ),
        ConstructorServicePriceItem(
            key = "carving_cross",
            groupName = "Художественное оформление",
            title = "С резным барельефным крестом",
            price = 290.0,
            description = "Глубокая барельефная резьба православного/католического креста",
            sortOrder = 2
        ),
        ConstructorServicePriceItem(
            key = "carving_flowers",
            groupName = "Художественное оформление",
            title = "С резными розами / цветами",
            price = 340.0,
            description = "Объемная скульптурная резьба цветов на стеле",
            sortOrder = 3
        ),
        ConstructorServicePriceItem(
            key = "carving_angel_tree",
            groupName = "Художественное оформление",
            title = "С резным ангелом / березкой",
            price = 460.0,
            description = "Художественная барельефная резьба ангела или дерева",
            sortOrder = 4
        ),
        ConstructorServicePriceItem(
            key = "carving_rock",
            groupName = "Художественное оформление",
            title = "Скала / Корка (колотый скол)",
            price = 190.0,
            description = "Необработанный природный колотый край гранита",
            sortOrder = 5
        ),
        ConstructorServicePriceItem(
            key = "carving_exclusive",
            groupName = "Художественное оформление",
            title = "Авторская эксклюзивная резка",
            price = 550.0,
            description = "Сложный индивидуальный резной проект любой формы",
            sortOrder = 6
        ),
        ConstructorServicePriceItem(
            key = "chamfer_polished_10mm",
            groupName = "Художественное оформление",
            title = "Широкая полированная фаска 10 мм",
            price = 50.0,
            description = "Снятие широкой фаски с зеркальной полировкой",
            sortOrder = 10
        ),
        ConstructorServicePriceItem(
            key = "chamfer_round",
            groupName = "Художественное оформление",
            title = "Фигурная радиусная фаска (валик)",
            price = 85.0,
            description = "Округление граней стелы валиком с полировкой",
            sortOrder = 11
        ),
        ConstructorServicePriceItem(
            key = "plinth_high",
            groupName = "Художественное оформление",
            title = "Увеличенная высокая тумба",
            price = 85.0,
            description = "Подставка увеличенной высоты",
            sortOrder = 20
        ),
        ConstructorServicePriceItem(
            key = "flowerbed_half_slab",
            groupName = "Художественное оформление",
            title = "Цветник с малой плитой (1/2 длины)",
            price = 120.0,
            description = "Гранитная накладная плита на половину цветника",
            sortOrder = 21
        ),
        ConstructorServicePriceItem(
            key = "flowerbed_solid_slab",
            groupName = "Художественное оформление",
            title = "Сплошная полированная надгробная плита",
            price = 260.0,
            description = "Полностью закрытая полированная гранитная плита",
            sortOrder = 22
        ),
        ConstructorServicePriceItem(
            key = "flowerbed_double",
            groupName = "Художественное оформление",
            title = "Двойной семейный цветник",
            price = 100.0,
            description = "Увеличенный цветник на два захоронения",
            sortOrder = 23
        ),
        ConstructorServicePriceItem(
            key = "portrait_laser_a4",
            groupName = "Художественное оформление",
            title = "Лазерная/станочная гравировка (А4)",
            price = 150.0,
            description = "Высокоточная гравировка с ретушью фото",
            sortOrder = 30
        ),
        ConstructorServicePriceItem(
            key = "portrait_hand_retouch",
            groupName = "Художественное оформление",
            title = "Станочная + ручная доработка художником",
            price = 260.0,
            description = "Глубокая прорисовка полутонов и взгляда художником",
            sortOrder = 31
        ),
        ConstructorServicePriceItem(
            key = "portrait_full_height",
            groupName = "Художественное оформление",
            title = "Портрет в полный рост",
            price = 420.0,
            description = "Гравировка человека в полный рост",
            sortOrder = 32
        ),
        ConstructorServicePriceItem(
            key = "portrait_ceramic_13x18",
            groupName = "Художественное оформление",
            title = "Фотокерамика 13×18 см (врезка в нишу)",
            price = 120.0,
            description = "Запекание на керамической пластине, врезка в камень",
            sortOrder = 33
        ),
        ConstructorServicePriceItem(
            key = "portrait_triplex",
            groupName = "Художественное оформление",
            title = "Фото на осветленном стекле триплекс 20×30 см",
            price = 290.0,
            description = "Премиальный триплекс с полированными фасками",
            sortOrder = 34
        ),
        ConstructorServicePriceItem(
            key = "letter_standard",
            groupName = "Художественное оформление",
            title = "Гравировка 1 знака ФИО/дат (станок)",
            unit = "знак",
            price = 1.60,
            description = "Стандартный шрифт на полированном граните",
            sortOrder = 40
        ),
        ConstructorServicePriceItem(
            key = "letter_gold",
            groupName = "Художественное оформление",
            title = "Гравировка 1 знака с сусальным золотом / краской",
            unit = "знак",
            price = 5.50,
            description = "Глубокая рубка с золочением или покраской",
            sortOrder = 41
        ),
        ConstructorServicePriceItem(
            key = "letter_epitaph",
            groupName = "Художественное оформление",
            title = "Гравировка 1 знака эпитафии",
            unit = "знак",
            price = 1.50,
            description = "Текст эпитафии курсивом или академическим шрифтом",
            sortOrder = 42
        ),
        ConstructorServicePriceItem(
            key = "art_cross",
            groupName = "Символика и до опции",
            title = "Гравировка креста / полумесяца",
            price = 30.0,
            description = "Религиозный символ 15-20 см",
            sortOrder = 50
        ),
        ConstructorServicePriceItem(
            key = "art_flowers",
            groupName = "Символика и до опции",
            title = "Гравировка цветов (розы / гвоздики)",
            price = 45.0,
            description = "Цветы у подножия стелы",
            sortOrder = 51
        ),
        ConstructorServicePriceItem(
            key = "art_backside",
            groupName = "Символика и до опции",
            title = "Гравировка обратной стороны (икона / пейзаж)",
            price = 190.0,
            description = "Храм, природа или икона на тыльной стороне стелы",
            sortOrder = 52
        ),
        ConstructorServicePriceItem(
            key = "art_antirain",
            groupName = "Символика и до опции",
            title = "Покрытие «Антидождь»",
            price = 50.0,
            description = "Водоотталкивающее нанопокрытие для портрета и надписей",
            sortOrder = 53
        ),
        ConstructorServicePriceItem(
            key = "install_beams",
            groupName = "Установка и монтаж",
            title = "Монтаж на ж/б балки перекрытия (в грунт)",
            price = 260.0,
            description = "Установка на балки перекрытия, заливка раствора по уровню",
            sortOrder = 60
        ),
        ConstructorServicePriceItem(
            key = "install_concrete_pad",
            groupName = "Установка и монтаж",
            title = "Монтаж на сплошной армированный фундамент",
            price = 470.0,
            description = "Заливка монолитной плиты с армированием",
            sortOrder = 61
        ),
        ConstructorServicePriceItem(
            key = "install_existing_base",
            groupName = "Установка и монтаж",
            title = "Монтаж на готовый цоколь / плитку",
            price = 210.0,
            description = "Монтаж стелы и цветника на существующий фундамент",
            sortOrder = 62
        ),
        ConstructorServicePriceItem(
            key = "measure_fence_price_pm",
            groupName = "Замеры и расчеты",
            title = "Ограда / цоколь (за 1 пог. м)",
            unit = "м.п.",
            price = 45.0,
            description = "Базовая цена за 1 погонный метр ограды или цоколя",
            sortOrder = 70
        ),
        ConstructorServicePriceItem(
            key = "measure_tile_price_sqm",
            groupName = "Замеры и расчеты",
            title = "Благоустройство / плитка (за 1 м²)",
            unit = "м²",
            price = 85.0,
            description = "Укладка плитки, брусчатки, бордюра и отсыпка за 1 кв. м",
            sortOrder = 71
        ),
        ConstructorServicePriceItem(
            key = "measure_delivery_base_price",
            groupName = "Замеры и расчеты",
            title = "Доставка (базовый тариф)",
            unit = "рейс",
            price = 60.0,
            description = "Базовая стоимость подачи спецтранспорта",
            sortOrder = 72
        ),
        ConstructorServicePriceItem(
            key = "measure_delivery_base_km",
            groupName = "Замеры и расчеты",
            title = "Доставка (включенный километраж)",
            unit = "км",
            price = 15.0,
            description = "Количество километров, включенных в базовый тариф доставки",
            sortOrder = 73
        ),
        ConstructorServicePriceItem(
            key = "measure_delivery_km_price",
            groupName = "Замеры и расчеты",
            title = "Доставка за доп. км",
            unit = "км",
            price = 1.50,
            description = "Доплата за каждый километр свыше включенного лимита",
            sortOrder = 74
        )
    )
}

typealias DefaultPriceCatalog = DefaultCatalog
