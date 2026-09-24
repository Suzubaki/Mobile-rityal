package com.example.data

import com.example.ui.CalculationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Бланк заказа для завода (производства) без ценников.
 * В точности воспроизводит структуру реального бланка цеха.
 */
data class FactoryOrderDocumentData(
    val orderNumber: String,
    val date: String = "",
    val clientName: String = "",
    val deadline: String = "",
    val clientPhone: String = "",
    val installationAddress: String = "",
    // Данные усопшего и оформление
    val deceasedLastName: String = "",
    val deceasedFirstName: String = "",
    val deceasedMiddleName: String = "",
    val birthDate: String = "",
    val deathDate: String = "",
    val cross: String = "",
    val vignettePhoto: String = "",
    val frame: String = "",
    val epitaph: String = "",
    val plateDecoration: String = "",
    val additions: String = "",
    // ФОРМА И РАЗМЕРЫ П-КА
    val material: String = "",
    val obelisk: String = "",
    val annex: String = "",
    val shelf: String = "",
    val tumba: String = "",
    val cvetnik: String = "",
    val plita: String = "",
    val otmostkaTop: String = "",
    val vase: String = "",
    val lampada: String = "",
    val demontazh: String = "",
    val tileTypeAndSize: String = "",
    val bordyur: String = ""
) {
    companion object {

        fun fixParentheses(raw: String): String {
            val text = raw.trim()
            if (text.isBlank()) return ""
            var openCount = 0
            val sb = StringBuilder()
            for (ch in text) {
                if (ch == '(') {
                    openCount++
                    sb.append(ch)
                } else if (ch == ')') {
                    if (openCount > 0) {
                        openCount--
                        sb.append(ch)
                    }
                } else {
                    sb.append(ch)
                }
            }
            repeat(openCount) {
                sb.append(')')
            }
            return sb.toString()
                .replace(Regex("""\(\s*\)"""), "")
                .replace(Regex("""\s+"""), " ")
                .trim(' ', ',', ';', ':', '-', '|')
        }

        fun cleanWorkshopText(raw: String): String {
            if (raw.isBlank()) return ""
            var text = raw
                // Remove currency & price tags like (45.00 руб.), (120.00 BYN), (30.00 €), ($50)
                .replace(Regex("""\(\s*[\d.,]+\s*(?:BYN|EUR|USD|руб\.?|р\.?|€|\$)\s*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\[[^\]]*?(?:BYN|EUR|USD|руб|€|\$|\d+\s*зн|\d+\s*бол|\d+\s*мал)[^\]]*?\]""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\[\s*Шрифт:[^\]]+\]""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(?\s*Объём:\s*[\d.,]+\s*м³\s*[,]?\s*\)?""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(?\s*камень:\s*[^,);]+[,]?\s*\)?""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(?\s*материал:\_?[^,);]+[,]?\s*\)?""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(\s*Категория:[^\)]*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(\s*размер:[^\)]*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(\s*Тип[^\)]*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(\s*основная\s*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(\s*дополнительная\s*\)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^Гравировка рисунка\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^Гравировка\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^Покраска букв\s+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^Портрет\s*/\s*Фотокерамика\s*\((.*?)\)$""", RegexOption.IGNORE_CASE), "$1")
                .replace(Regex("""^Рамка\s*/\s*врезка фото\s*\((.*?)\)$""", RegexOption.IGNORE_CASE), "$1")

            text = text.replace(Regex("""\(([^)]+)\)\s*\(\1\)""", RegexOption.IGNORE_CASE), "($1)")
            text = text.replace(Regex("""\(\s*\)"""), "")
            text = text.replace(Regex("""\[\s*\]"""), "")
            text = text.replace(Regex("""\s+"""), " ")
            text = text.trim(' ', ',', ';', '[', ']', '|', ':', '-')
            return fixParentheses(text)
        }

        fun cleanPhotoText(raw: String): String {
            if (raw.isBlank()) return ""
            var text = cleanWorkshopText(raw)

            text = text.replace(Regex("""^\d+\s*шт:?\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Портрет\s*/\s*Фотокерамика\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Портрет\s*/\s*Фото\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Портрет\s+""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Фотокерамика\s*/\s*""", RegexOption.IGNORE_CASE), "")

            text = text.replace(Regex("""\bКатегория:\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bкатегория\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bразмер:\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bи\s+т\.?д?\.?\b""", RegexOption.IGNORE_CASE), "")

            text = text.replace(Regex("""\(\s*\)"""), "").replace(Regex("""\s+"""), " ").trim(' ', ',', ';', ':', '-', '/')

            if (text.startsWith("(") && text.endsWith(")") && text.count { it == '(' } == 1) {
                text = text.substring(1, text.length - 1).trim()
            }

            return fixParentheses(text)
        }

        fun cleanFrameText(raw: String): String {
            if (raw.isBlank() || raw.equals("Без рамки", ignoreCase = true) || raw.contains("без рамки", ignoreCase = true)) {
                return ""
            }
            val lower = raw.lowercase()

            val isVrezka = lower.contains("врезка") || lower.contains("ниша") || lower.contains("recessed")
            val hasSpecificMaterial = lower.contains("бронз") || lower.contains("латунь") || lower.contains("алюмин") || lower.contains("полимер") || lower.contains("гранит")

            if (isVrezka && !hasSpecificMaterial) {
                return "врезка"
            }

            var text = cleanWorkshopText(raw)
            text = text.replace(Regex("""^\d+\s*шт:?\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Рамка\s*/\s*врезка\s*(?:фото)?\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Рамка\s*/\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Врезка\s+рамки\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""^Рамка\s+""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bрамка\b""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bфаска\b""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bТип/материал:\s*""", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("""\bМатериал:\s*""", RegexOption.IGNORE_CASE), "")

            text = text.replace(Regex("""\(\s*\)"""), "").replace(Regex("""\s+"""), " ").trim(' ', ',', ';', ':', '-', '/', '\\')

            if (text.startsWith("(") && text.endsWith(")") && text.count { it == '(' } == 1) {
                text = text.substring(1, text.length - 1).trim()
            }

            if (text.isBlank() || text.equals("врезка", ignoreCase = true)) {
                return "врезка"
            }

            return fixParentheses(text)
        }

        private fun extractBundleField(note: String, label: String, nextLabels: List<String>): String? {
            val options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            val bracketMatch = Regex("""$label:\s*\[(.*?)\]""", options).find(note)
            if (bracketMatch != null && bracketMatch.groupValues[1].isNotBlank()) {
                return bracketMatch.groupValues[1].trim()
            }
            val pipeMatch = Regex("""$label:\s*(.*?)(?:\s*\|\s*|$)""", options).find(note)
            if (pipeMatch != null && pipeMatch.groupValues[1].isNotBlank()) {
                val cand = pipeMatch.groupValues[1].trim()
                if (!nextLabels.any { cand.contains(it, ignoreCase = true) }) {
                    return cand
                }
            }
            val lookahead = if (nextLabels.isNotEmpty()) {
                val escaped = nextLabels.joinToString("|") { Regex.escape(it) }
                """$label:\s*(.*?)(?=(?:$escaped)|\s*$)"""
            } else {
                """$label:\s*(.*?)(?=\s*$)"""
            }
            val match = Regex(lookahead, options).find(note)
            if (match != null) {
                val res = match.groupValues[1].trim().trimEnd('|', ';', ',')
                if (res.isNotBlank()) return res
            }
            return null
        }

        fun splitFio(fullName: String): Triple<String, String, String> {
            val lines = fullName.lines().map { it.trim() }.filter { it.isNotBlank() }
            val firstLine = lines.firstOrNull() ?: fullName.trim()
            val clean = firstLine.replace(Regex("""\s+"""), " ")
            if (clean.isBlank()) return Triple("", "", "")
            val parts = clean.split(" ")
            return when {
                parts.size >= 3 -> Triple(parts[0], parts[1], parts.subList(2, parts.size).joinToString(" "))
                parts.size == 2 -> Triple(parts[0], parts[1], "")
                else -> Triple(parts[0], "", "")
            }
        }

        fun extractDatesFromText(text: String): Pair<String, String> {
            if (text.isBlank()) return Pair("", "")
            // Format: 15.05.1950 — 20.08.2024 or 1950 - 2024 or 15/05/1950 - 20/08/2024
            val dateRegex = Regex("""(\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4})\s*[-–—/]\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4})""")
            val match = dateRegex.find(text)
            if (match != null) {
                return Pair(match.groupValues[1].trim(), match.groupValues[2].trim())
            }
            // Fallback: look for individual date occurrences
            val singleDates = Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b""").findAll(text).map { it.value }.toList()
            if (singleDates.size >= 2) {
                return Pair(singleDates[0], singleDates[1])
            } else if (singleDates.size == 1) {
                return Pair(singleDates[0], "")
            }
            return Pair("", "")
        }

        fun fromCalculationState(
            state: CalculationState,
            currentDate: Date = Date()
        ): FactoryOrderDocumentData {
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(currentDate)

            // Smart extraction from items and constructor data
            var extractedMaterial = state.monumentMaterial.trim()
            val extractedObelisks = mutableListOf<String>()
            val extractedAnnexes = mutableListOf<String>()
            val extractedShelves = mutableListOf<String>()
            val extractedTumbas = mutableListOf<String>()
            val extractedCvetniks = mutableListOf<String>()
            val extractedPlitas = mutableListOf<String>()
            var extractedOtmostka = state.otmostkaInfo.trim()
            val extractedVases = if (state.vaseInfo.isNotBlank()) mutableListOf(state.vaseInfo.trim()) else mutableListOf()
            val extractedLampadas = if (state.lampadaInfo.isNotBlank()) mutableListOf(state.lampadaInfo.trim()) else mutableListOf()
            val extractedDemontazh = if (state.dismantlingInfo.isNotBlank()) mutableListOf(state.dismantlingInfo.trim()) else mutableListOf()
            val extractedTiles = if (state.tileInfo.isNotBlank()) mutableListOf(state.tileInfo.trim()) else mutableListOf()
            val extractedBorders = if (state.borderInfo.isNotBlank()) mutableListOf(state.borderInfo.trim()) else mutableListOf()
            val extractedCrosses = if (state.crossInfo.isNotBlank()) mutableListOf(state.crossInfo.trim()) else mutableListOf()
            val extractedPhotos = if (state.photoVignetteInfo.isNotBlank()) mutableListOf(state.photoVignetteInfo.trim()) else mutableListOf()
            val extractedFrames = if (state.frameInfo.isNotBlank()) mutableListOf(state.frameInfo.trim()) else mutableListOf()
            var extractedEpitaph = state.epitaphText.trim()
            val extractedPlateDecor = if (state.plateDecoration.isNotBlank()) mutableListOf(state.plateDecoration.trim()) else mutableListOf()
            val extractedAdditions = if (state.additionsInfo.isNotBlank()) mutableListOf(state.additionsInfo.trim()) else mutableListOf()

            var rawFioFromEngraving = ""

            state.selectedItems.forEach { item ->
                val name = item.name.trim()
                val note = item.customNote.trim()
                val cleanItemName = cleanWorkshopText(name)
                val cleanItemNote = cleanWorkshopText(note)
                val desc = when {
                    cleanItemNote.isBlank() || cleanItemNote.equals(cleanItemName, ignoreCase = true) -> cleanItemName
                    cleanItemNote.startsWith(cleanItemName, ignoreCase = true) -> cleanItemNote
                    cleanItemName.startsWith(cleanItemNote, ignoreCase = true) -> cleanItemName
                    else -> {
                        val cleanNoteNoParens = cleanItemNote.removePrefix(cleanItemName).trim(' ', '(', ')', '-')
                        if (cleanNoteNoParens.isNotBlank()) "$cleanItemName $cleanNoteNoParens" else cleanItemName
                    }
                }

                // Check for single bundle format
                if (name.contains("Комплект памятника", ignoreCase = true) || note.contains("Камень:", ignoreCase = true)) {
                    val mat = extractBundleField(note, "Камень", listOf("Стела", "Размер", "Модель", "Полировка"))
                    if (!mat.isNullOrBlank() && extractedMaterial.isBlank()) {
                        extractedMaterial = cleanWorkshopText(mat)
                    }
                    val st = extractBundleField(note, "Стела", listOf("Приставка", "Полка", "Тумба", "Цветник", "Надгробная плита"))
                    val annex = extractBundleField(note, "Приставка", listOf("Полка", "Тумба", "Цветник", "Надгробная плита"))
                    val shelf = extractBundleField(note, "Полка под стелу", listOf("Тумба", "Цветник", "Надгробная плита"))
                        ?: extractBundleField(note, "Полка", listOf("Тумба", "Цветник", "Надгробная плита"))
                    if (!st.isNullOrBlank() && extractedObelisks.isEmpty()) {
                        extractedObelisks.add(cleanWorkshopText(st))
                    }
                    if (!annex.isNullOrBlank() && extractedAnnexes.isEmpty()) {
                        extractedAnnexes.add(cleanWorkshopText(annex))
                    }
                    if (!shelf.isNullOrBlank() && extractedShelves.isEmpty()) {
                        extractedShelves.add(cleanWorkshopText(shelf))
                    }
                    val tb = extractBundleField(note, "Тумба", listOf("Цветник", "Надгробная плита", "Покраска", "Гравировка ФИО"))
                    if (!tb.isNullOrBlank() && extractedTumbas.isEmpty()) {
                        extractedTumbas.add(cleanWorkshopText(tb))
                    }
                    val fb = extractBundleField(note, "Цветник", listOf("Надгробная плита", "Покраска", "Гравировка ФИО"))
                    if (!fb.isNullOrBlank() && extractedCvetniks.isEmpty()) {
                        extractedCvetniks.add(cleanWorkshopText(fb))
                    }
                    val sl = extractBundleField(note, "Надгробная плита", listOf("Покраска", "Гравировка ФИО", "Эпитафия"))
                    if (!sl.isNullOrBlank() && extractedPlitas.isEmpty()) {
                        extractedPlitas.add(cleanWorkshopText(sl))
                    }
                    val fioB = extractBundleField(note, "Гравировка ФИО", listOf("Эпитафия", "Рисунки", "Фото/Портрет"))
                        ?: extractBundleField(note, "Покраска букв ФИО", listOf("Эпитафия", "Рисунки", "Фото/Портрет"))
                    if (!fioB.isNullOrBlank() && rawFioFromEngraving.isBlank()) {
                        rawFioFromEngraving = fioB.trim()
                    }
                    val epB = extractBundleField(note, "Эпитафия", listOf("Рисунки", "Фото/Портрет", "Ваза"))
                        ?: extractBundleField(note, "Покраска эпитафии", listOf("Рисунки", "Фото/Портрет", "Ваза"))
                    if (!epB.isNullOrBlank() && extractedEpitaph.isBlank()) {
                        extractedEpitaph = cleanWorkshopText(epB)
                    }
                    val decorB = extractBundleField(note, "Рисунки и декор", listOf("Фото/Портрет", "Ваза", "Вес"))
                    if (!decorB.isNullOrBlank()) {
                        val decorItems = decorB.split(",").map { cleanWorkshopText(it) }.filter { it.isNotBlank() }
                        decorItems.forEach { dec ->
                            when {
                                dec.contains("Крест", ignoreCase = true) || dec.contains("КР-", ignoreCase = true) || dec.contains("распят", ignoreCase = true) -> {
                                    if (!extractedCrosses.contains(dec)) extractedCrosses.add(dec)
                                }
                                dec.contains("Виньет", ignoreCase = true) || dec.contains("В-", ignoreCase = true) -> {
                                    if (!extractedPhotos.contains(dec)) extractedPhotos.add(dec)
                                }
                                else -> {
                                    if (!extractedAdditions.contains(dec)) extractedAdditions.add(dec)
                                }
                            }
                        }
                    }
                    val photoB = extractBundleField(note, "Фото/Портрет", listOf("Ваза", "Вес"))
                    if (!photoB.isNullOrBlank()) {
                        val cleanPh = cleanWorkshopText(photoB)
                        if (cleanPh.contains("рамка:", ignoreCase = true)) {
                            val phParts = cleanPh.split(Regex("""рамка:\s*""", RegexOption.IGNORE_CASE))
                            val p1 = cleanWorkshopText(phParts[0])
                            val p2 = if (phParts.size > 1) cleanWorkshopText(phParts[1]) else ""
                            if (p1.isNotBlank() && !extractedPhotos.contains(p1)) extractedPhotos.add(p1)
                            if (p2.isNotBlank() && !extractedFrames.contains(p2)) extractedFrames.add(p2)
                        } else {
                            if (!extractedPhotos.contains(cleanPh)) extractedPhotos.add(cleanPh)
                        }
                    }
                    val vaseB = extractBundleField(note, "Ваза", listOf("Вес", "Доставка"))
                    if (!vaseB.isNullOrBlank()) {
                        val cleanV = cleanWorkshopText(vaseB)
                        if (!extractedVases.contains(cleanV)) extractedVases.add(cleanV)
                    }
                }

                // Individual line items
                when {
                    name.contains("Приставка", ignoreCase = true) -> {
                        if (!extractedAnnexes.contains(desc)) extractedAnnexes.add(desc)
                    }
                    name.contains("Полка", ignoreCase = true) -> {
                        if (!extractedShelves.contains(desc)) extractedShelves.add(desc)
                    }
                    name.contains("Стела", ignoreCase = true) || name.contains("Обелиск", ignoreCase = true) -> {
                        if (!extractedObelisks.contains(desc)) extractedObelisks.add(desc)
                        if (extractedMaterial.isBlank()) {
                            val matMatch = Regex("""\((.*?)\)""").find(name)
                            if (matMatch != null && !matMatch.groupValues[1].contains("х", ignoreCase = true)) {
                                extractedMaterial = matMatch.groupValues[1].trim()
                            }
                        }
                    }
                    name.contains("Тумба", ignoreCase = true) || name.contains("Подставка", ignoreCase = true) -> {
                        if (!extractedTumbas.contains(desc)) extractedTumbas.add(desc)
                    }
                    name.contains("Цветник", ignoreCase = true) -> {
                        if (!extractedCvetniks.contains(desc)) extractedCvetniks.add(desc)
                    }
                    name.contains("Плита", ignoreCase = true) && !name.contains("плитк", ignoreCase = true) -> {
                        if (!extractedPlitas.contains(desc)) extractedPlitas.add(desc)
                    }
                    name.contains("Отмостка", ignoreCase = true) || name.contains("Фундамент", ignoreCase = true) || name.contains("Бетонир", ignoreCase = true) || name.contains("Балки", ignoreCase = true) -> {
                        if (extractedOtmostka.isBlank()) extractedOtmostka = desc
                    }
                    name.contains("Ваза", ignoreCase = true) -> {
                        val qtyStr = if (item.quantity > 1) "${PriceFormatter.formatNumber(item.quantity)} шт: " else ""
                        val fullVase = "$qtyStr$desc"
                        if (!extractedVases.contains(fullVase)) extractedVases.add(fullVase)
                    }
                    name.contains("Лампада", ignoreCase = true) -> {
                        val qtyStr = if (item.quantity > 1) "${PriceFormatter.formatNumber(item.quantity)} шт: " else ""
                        val fullL = "$qtyStr$desc"
                        if (!extractedLampadas.contains(fullL)) extractedLampadas.add(fullL)
                    }
                    name.contains("Демонтаж", ignoreCase = true) -> {
                        if (!extractedDemontazh.contains(desc)) extractedDemontazh.add(desc)
                    }
                    name.contains("Плитк", ignoreCase = true) || name.contains("Керамогранит", ignoreCase = true) || name.contains("Брусчатк", ignoreCase = true) -> {
                        if (!extractedTiles.contains(desc)) extractedTiles.add(desc)
                    }
                    name.contains("Бордюр", ignoreCase = true) || name.contains("Поребрик", ignoreCase = true) -> {
                        if (!extractedBorders.contains(desc)) extractedBorders.add(desc)
                    }
                    name.contains("Крест", ignoreCase = true) || name.contains("КР-", ignoreCase = true) || name.contains("Распяти", ignoreCase = true) -> {
                        if (!extractedCrosses.contains(desc)) extractedCrosses.add(desc)
                    }
                    name.contains("Фото", ignoreCase = true) || name.contains("Медальон", ignoreCase = true) ||
                    name.contains("Керамик", ignoreCase = true) || name.contains("Стекл", ignoreCase = true) ||
                    name.contains("Портрет", ignoreCase = true) || name.contains("Виньетк", ignoreCase = true) ||
                    name.contains("В-", ignoreCase = true) -> {
                        if (!extractedPhotos.contains(desc)) extractedPhotos.add(desc)
                    }
                    name.contains("Рамка", ignoreCase = true) || name.contains("Врезка рамки", ignoreCase = true) -> {
                        if (!extractedFrames.contains(desc)) extractedFrames.add(desc)
                    }
                    name.contains("Эпитафия", ignoreCase = true) -> {
                        if (extractedEpitaph.isBlank()) extractedEpitaph = cleanWorkshopText(note.ifBlank { name })
                    }
                    name.contains("ФИО", ignoreCase = true) || name.contains("Гравировка ФИО", ignoreCase = true) -> {
                        if (rawFioFromEngraving.isBlank()) rawFioFromEngraving = note.ifBlank { name }
                    }
                    name.contains("Фаска", ignoreCase = true) || name.contains("Оформление плиты", ignoreCase = true) || name.contains("Полировка", ignoreCase = true) -> {
                        if (!extractedPlateDecor.contains(desc)) extractedPlateDecor.add(desc)
                    }
                    else -> {
                        if (name.contains("Столб", ignoreCase = true) || name.contains("Скамь", ignoreCase = true) ||
                            name.contains("Стол", ignoreCase = true) || name.contains("Оград", ignoreCase = true) ||
                            name.contains("Антидождь", ignoreCase = true) || name.contains("Рисунок", ignoreCase = true) ||
                            item.category.contains("Благоустройство", ignoreCase = true) ||
                            item.category.contains("Ограды", ignoreCase = true)
                        ) {
                            if (!extractedAdditions.contains(desc)) extractedAdditions.add(desc)
                        }
                    }
                }
            }

            // Parse FIO and Dates
            val baseFio = when {
                state.deceasedName.isNotBlank() -> state.deceasedName
                rawFioFromEngraving.isNotBlank() -> rawFioFromEngraving
                else -> ""
            }

            val (lName, fName, mName) = splitFio(baseFio)
            val (datesBirth, datesDeath) = extractDatesFromText(rawFioFromEngraving.ifBlank { baseFio })

            val finalLastName = state.deceasedLastName.ifBlank { lName }
            val finalFirstName = state.deceasedFirstName.ifBlank { fName }
            val finalMiddleName = state.deceasedMiddleName.ifBlank { mName }
            val finalBirthDate = state.deceasedBirthDate.ifBlank { datesBirth }
            val finalDeathDate = state.deceasedDeathDate.ifBlank { datesDeath }

            val addressStr = buildString {
                if (state.cemeteryName.isNotBlank()) append(state.cemeteryName)
                if (state.plotNumber.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append("уч. ").append(state.plotNumber)
                }
            }

            val finalAdditions = buildString {
                if (state.additionsInfo.isNotBlank()) {
                    append(state.additionsInfo)
                } else {
                    if (extractedAdditions.isNotEmpty()) {
                        append(extractedAdditions.joinToString("; "))
                    }
                    if (state.notes.isNotBlank()) {
                        if (isNotEmpty()) append("; ")
                        append(state.notes)
                    }
                }
            }

            val photoVal = if (state.photoVignetteInfo.isNotBlank()) cleanPhotoText(state.photoVignetteInfo)
            else extractedPhotos.map { cleanPhotoText(it) }.filter { it.isNotBlank() }.distinct().joinToString("; ")

            val frameVal = if (state.frameInfo.isNotBlank()) cleanFrameText(state.frameInfo)
            else extractedFrames.map { cleanFrameText(it) }.filter { it.isNotBlank() }.distinct().joinToString("; ")

            return FactoryOrderDocumentData(
                orderNumber = state.orderNumber,
                date = dateStr,
                clientName = state.clientName,
                deadline = state.orderTerm,
                clientPhone = state.clientPhone,
                installationAddress = addressStr,
                deceasedLastName = finalLastName,
                deceasedFirstName = finalFirstName,
                deceasedMiddleName = finalMiddleName,
                birthDate = finalBirthDate,
                deathDate = finalDeathDate,
                cross = fixParentheses(state.crossInfo.ifBlank { extractedCrosses.joinToString("; ") }),
                vignettePhoto = photoVal,
                frame = frameVal,
                epitaph = fixParentheses(state.epitaphText.ifBlank { extractedEpitaph }),
                plateDecoration = fixParentheses(state.plateDecoration.ifBlank { extractedPlateDecor.joinToString("; ") }),
                additions = fixParentheses(finalAdditions),
                material = fixParentheses(state.monumentMaterial.ifBlank { extractedMaterial }),
                obelisk = fixParentheses(state.obeliskInfo.ifBlank { extractedObelisks.joinToString(", ") }),
                annex = fixParentheses(extractedAnnexes.joinToString(", ")),
                shelf = fixParentheses(extractedShelves.joinToString(", ")),
                tumba = fixParentheses(state.plinthInfo.ifBlank { extractedTumbas.joinToString(", ") }),
                cvetnik = fixParentheses(state.flowerbedInfo.ifBlank { extractedCvetniks.joinToString(", ") }),
                plita = fixParentheses(state.slabInfo.ifBlank { extractedPlitas.joinToString(", ") }),
                otmostkaTop = fixParentheses(state.otmostkaInfo.ifBlank { extractedOtmostka }),
                vase = fixParentheses(state.vaseInfo.ifBlank { extractedVases.joinToString("; ") }),
                lampada = fixParentheses(state.lampadaInfo.ifBlank { extractedLampadas.joinToString("; ") }),
                demontazh = fixParentheses(state.dismantlingInfo.ifBlank { extractedDemontazh.joinToString("; ") }),
                tileTypeAndSize = fixParentheses(state.tileInfo.ifBlank { extractedTiles.joinToString("; ") }),
                bordyur = fixParentheses(state.borderInfo.ifBlank { extractedBorders.joinToString("; ") })
            )
        }

        fun fromSavedOrder(
            order: SavedOrder,
            items: List<CalculatedItemData>
        ): FactoryOrderDocumentData {
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(order.createdAt))

            var extractedMaterial = ""
            val extractedObelisks = mutableListOf<String>()
            val extractedAnnexes = mutableListOf<String>()
            val extractedShelves = mutableListOf<String>()
            val extractedTumbas = mutableListOf<String>()
            val extractedCvetniks = mutableListOf<String>()
            val extractedPlitas = mutableListOf<String>()
            var extractedOtmostka = ""
            val extractedVases = mutableListOf<String>()
            val extractedLampadas = mutableListOf<String>()
            val extractedDemontazh = mutableListOf<String>()
            val extractedTiles = mutableListOf<String>()
            val extractedBorders = mutableListOf<String>()
            val extractedCrosses = mutableListOf<String>()
            val extractedPhotos = mutableListOf<String>()
            val extractedFrames = mutableListOf<String>()
            var extractedEpitaph = ""
            val extractedPlateDecor = mutableListOf<String>()
            val extractedAdditions = mutableListOf<String>()
            var rawFioFromEngraving = ""

            items.forEach { item ->
                val name = item.name.trim()
                val note = item.customNote.trim()
                val cleanItemName = cleanWorkshopText(name)
                val cleanItemNote = cleanWorkshopText(note)
                val desc = when {
                    cleanItemNote.isBlank() || cleanItemNote.equals(cleanItemName, ignoreCase = true) -> cleanItemName
                    cleanItemNote.startsWith(cleanItemName, ignoreCase = true) -> cleanItemNote
                    cleanItemName.startsWith(cleanItemNote, ignoreCase = true) -> cleanItemName
                    else -> {
                        val cleanNoteNoParens = cleanItemNote.removePrefix(cleanItemName).trim(' ', '(', ')', '-')
                        if (cleanNoteNoParens.isNotBlank()) "$cleanItemName $cleanNoteNoParens" else cleanItemName
                    }
                }

                if (name.contains("Комплект памятника", ignoreCase = true) || note.contains("Камень:", ignoreCase = true)) {
                    val mat = extractBundleField(note, "Камень", listOf("Стела", "Размер", "Модель", "Полировка"))
                    if (!mat.isNullOrBlank() && extractedMaterial.isBlank()) {
                        extractedMaterial = cleanWorkshopText(mat)
                    }
                    val st = extractBundleField(note, "Стела", listOf("Приставка", "Полка", "Тумба", "Цветник", "Надгробная плита"))
                    val annex = extractBundleField(note, "Приставка", listOf("Полка", "Тумба", "Цветник", "Надгробная плита"))
                    val shelf = extractBundleField(note, "Полка под стелу", listOf("Тумба", "Цветник", "Надгробная плита"))
                        ?: extractBundleField(note, "Полка", listOf("Тумба", "Цветник", "Надгробная плита"))
                    if (!st.isNullOrBlank() && extractedObelisks.isEmpty()) {
                        extractedObelisks.add(cleanWorkshopText(st))
                    }
                    if (!annex.isNullOrBlank() && extractedAnnexes.isEmpty()) {
                        extractedAnnexes.add(cleanWorkshopText(annex))
                    }
                    if (!shelf.isNullOrBlank() && extractedShelves.isEmpty()) {
                        extractedShelves.add(cleanWorkshopText(shelf))
                    }
                    val tb = extractBundleField(note, "Тумба", listOf("Цветник", "Надгробная плита", "Покраска", "Гравировка ФИО"))
                    if (!tb.isNullOrBlank() && extractedTumbas.isEmpty()) {
                        extractedTumbas.add(cleanWorkshopText(tb))
                    }
                    val fb = extractBundleField(note, "Цветник", listOf("Надгробная плита", "Покраска", "Гравировка ФИО"))
                    if (!fb.isNullOrBlank() && extractedCvetniks.isEmpty()) {
                        extractedCvetniks.add(cleanWorkshopText(fb))
                    }
                    val sl = extractBundleField(note, "Надгробная плита", listOf("Покраска", "Гравировка ФИО", "Эпитафия"))
                    if (!sl.isNullOrBlank() && extractedPlitas.isEmpty()) {
                        extractedPlitas.add(cleanWorkshopText(sl))
                    }
                    val fioB = extractBundleField(note, "Гравировка ФИО", listOf("Эпитафия", "Рисунки", "Фото/Портрет"))
                        ?: extractBundleField(note, "Покраска букв ФИО", listOf("Эпитафия", "Рисунки", "Фото/Портрет"))
                    if (!fioB.isNullOrBlank() && rawFioFromEngraving.isBlank()) {
                        rawFioFromEngraving = fioB.trim()
                    }
                    val epB = extractBundleField(note, "Эпитафия", listOf("Рисунки", "Фото/Портрет", "Ваза"))
                        ?: extractBundleField(note, "Покраска эпитафии", listOf("Рисунки", "Фото/Портрет", "Ваза"))
                    if (!epB.isNullOrBlank() && extractedEpitaph.isBlank()) {
                        extractedEpitaph = cleanWorkshopText(epB)
                    }
                    val decorB = extractBundleField(note, "Рисунки и декор", listOf("Фото/Портрет", "Ваза", "Вес"))
                    if (!decorB.isNullOrBlank()) {
                        val decorItems = decorB.split(",").map { cleanWorkshopText(it) }.filter { it.isNotBlank() }
                        decorItems.forEach { dec ->
                            when {
                                dec.contains("Крест", ignoreCase = true) || dec.contains("КР-", ignoreCase = true) || dec.contains("распят", ignoreCase = true) -> {
                                    if (!extractedCrosses.contains(dec)) extractedCrosses.add(dec)
                                }
                                dec.contains("Виньет", ignoreCase = true) || dec.contains("В-", ignoreCase = true) -> {
                                    if (!extractedPhotos.contains(dec)) extractedPhotos.add(dec)
                                }
                                else -> {
                                    if (!extractedAdditions.contains(dec)) extractedAdditions.add(dec)
                                }
                            }
                        }
                    }
                    val photoB = extractBundleField(note, "Фото/Портрет", listOf("Ваза", "Вес"))
                    if (!photoB.isNullOrBlank()) {
                        val cleanPh = cleanWorkshopText(photoB)
                        if (cleanPh.contains("рамка:", ignoreCase = true)) {
                            val phParts = cleanPh.split(Regex("""рамка:\s*""", RegexOption.IGNORE_CASE))
                            val p1 = cleanWorkshopText(phParts[0])
                            val p2 = if (phParts.size > 1) cleanWorkshopText(phParts[1]) else ""
                            if (p1.isNotBlank() && !extractedPhotos.contains(p1)) extractedPhotos.add(p1)
                            if (p2.isNotBlank() && !extractedFrames.contains(p2)) extractedFrames.add(p2)
                        } else {
                            if (!extractedPhotos.contains(cleanPh)) extractedPhotos.add(cleanPh)
                        }
                    }
                    val vaseB = extractBundleField(note, "Ваза", listOf("Вес", "Доставка"))
                    if (!vaseB.isNullOrBlank()) {
                        val cleanV = cleanWorkshopText(vaseB)
                        if (!extractedVases.contains(cleanV)) extractedVases.add(cleanV)
                    }
                }

                when {
                    name.contains("Приставка", ignoreCase = true) -> {
                        if (!extractedAnnexes.contains(desc)) extractedAnnexes.add(desc)
                    }
                    name.contains("Полка", ignoreCase = true) -> {
                        if (!extractedShelves.contains(desc)) extractedShelves.add(desc)
                    }
                    name.contains("Стела", ignoreCase = true) || name.contains("Обелиск", ignoreCase = true) -> {
                        if (!extractedObelisks.contains(desc)) extractedObelisks.add(desc)
                        if (extractedMaterial.isBlank()) {
                            val matMatch = Regex("""\((.*?)\)""").find(name)
                            if (matMatch != null && !matMatch.groupValues[1].contains("х", ignoreCase = true)) {
                                extractedMaterial = matMatch.groupValues[1].trim()
                            }
                        }
                    }
                    name.contains("Тумба", ignoreCase = true) || name.contains("Подставка", ignoreCase = true) -> {
                        if (!extractedTumbas.contains(desc)) extractedTumbas.add(desc)
                    }
                    name.contains("Цветник", ignoreCase = true) -> {
                        if (!extractedCvetniks.contains(desc)) extractedCvetniks.add(desc)
                    }
                    name.contains("Плита", ignoreCase = true) && !name.contains("плитк", ignoreCase = true) -> {
                        if (!extractedPlitas.contains(desc)) extractedPlitas.add(desc)
                    }
                    name.contains("Отмостка", ignoreCase = true) || name.contains("Фундамент", ignoreCase = true) || name.contains("Бетонир", ignoreCase = true) || name.contains("Балки", ignoreCase = true) -> {
                        if (extractedOtmostka.isBlank()) extractedOtmostka = desc
                    }
                    name.contains("Ваза", ignoreCase = true) -> {
                        val qtyStr = if (item.quantity > 1) "${PriceFormatter.formatNumber(item.quantity)} шт: " else ""
                        val fullV = "$qtyStr$desc"
                        if (!extractedVases.contains(fullV)) extractedVases.add(fullV)
                    }
                    name.contains("Лампада", ignoreCase = true) -> {
                        val qtyStr = if (item.quantity > 1) "${PriceFormatter.formatNumber(item.quantity)} шт: " else ""
                        val fullL = "$qtyStr$desc"
                        if (!extractedLampadas.contains(fullL)) extractedLampadas.add(fullL)
                    }
                    name.contains("Демонтаж", ignoreCase = true) -> {
                        if (!extractedDemontazh.contains(desc)) extractedDemontazh.add(desc)
                    }
                    name.contains("Плитк", ignoreCase = true) || name.contains("Керамогранит", ignoreCase = true) || name.contains("Брусчатк", ignoreCase = true) -> {
                        if (!extractedTiles.contains(desc)) extractedTiles.add(desc)
                    }
                    name.contains("Бордюр", ignoreCase = true) || name.contains("Поребрик", ignoreCase = true) -> {
                        if (!extractedBorders.contains(desc)) extractedBorders.add(desc)
                    }
                    name.contains("Крест", ignoreCase = true) || name.contains("КР-", ignoreCase = true) || name.contains("Распяти", ignoreCase = true) -> {
                        if (!extractedCrosses.contains(desc)) extractedCrosses.add(desc)
                    }
                    name.contains("Фото", ignoreCase = true) || name.contains("Медальон", ignoreCase = true) ||
                    name.contains("Керамик", ignoreCase = true) || name.contains("Стекл", ignoreCase = true) ||
                    name.contains("Портрет", ignoreCase = true) || name.contains("Виньетк", ignoreCase = true) ||
                    name.contains("В-", ignoreCase = true) -> {
                        if (!extractedPhotos.contains(desc)) extractedPhotos.add(desc)
                    }
                    name.contains("Рамка", ignoreCase = true) || name.contains("Врезка рамки", ignoreCase = true) -> {
                        if (!extractedFrames.contains(desc)) extractedFrames.add(desc)
                    }
                    name.contains("Эпитафия", ignoreCase = true) -> {
                        if (extractedEpitaph.isBlank()) extractedEpitaph = cleanWorkshopText(note.ifBlank { name })
                    }
                    name.contains("ФИО", ignoreCase = true) || name.contains("Гравировка ФИО", ignoreCase = true) -> {
                        if (rawFioFromEngraving.isBlank()) rawFioFromEngraving = note.ifBlank { name }
                    }
                    name.contains("Фаска", ignoreCase = true) || name.contains("Оформление плиты", ignoreCase = true) || name.contains("Полировка", ignoreCase = true) -> {
                        if (!extractedPlateDecor.contains(desc)) extractedPlateDecor.add(desc)
                    }
                    else -> {
                        if (name.contains("Столб", ignoreCase = true) || name.contains("Скамь", ignoreCase = true) ||
                            name.contains("Стол", ignoreCase = true) || name.contains("Оград", ignoreCase = true) ||
                            name.contains("Антидождь", ignoreCase = true) || name.contains("Рисунок", ignoreCase = true) ||
                            item.category.contains("Благоустройство", ignoreCase = true) ||
                            item.category.contains("Ограды", ignoreCase = true)
                        ) {
                            if (!extractedAdditions.contains(desc)) extractedAdditions.add(desc)
                        }
                    }
                }
            }

            val baseFio = order.deceasedName.ifBlank { rawFioFromEngraving }
            val (lName, fName, mName) = splitFio(baseFio)
            val (datesBirth, datesDeath) = extractDatesFromText(rawFioFromEngraving.ifBlank { baseFio })

            val addressStr = buildString {
                if (order.cemeteryName.isNotBlank()) append(order.cemeteryName)
                if (order.plotNumber.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append("уч. ").append(order.plotNumber)
                }
            }

            val finalAdditions = buildString {
                if (extractedAdditions.isNotEmpty()) {
                    append(extractedAdditions.joinToString("; "))
                }
                if (order.notes.isNotBlank()) {
                    if (isNotEmpty()) append("; ")
                    append(order.notes)
                }
            }

            val photoVal = extractedPhotos.map { cleanPhotoText(it) }.filter { it.isNotBlank() }.distinct().joinToString("; ")
            val frameVal = extractedFrames.map { cleanFrameText(it) }.filter { it.isNotBlank() }.distinct().joinToString("; ")

            return FactoryOrderDocumentData(
                orderNumber = order.orderNumber,
                date = dateStr,
                clientName = order.clientName,
                deadline = "",
                clientPhone = order.clientPhone,
                installationAddress = addressStr,
                deceasedLastName = lName,
                deceasedFirstName = fName,
                deceasedMiddleName = mName,
                birthDate = datesBirth,
                deathDate = datesDeath,
                cross = fixParentheses(extractedCrosses.joinToString("; ")),
                vignettePhoto = photoVal,
                frame = frameVal,
                epitaph = fixParentheses(extractedEpitaph),
                plateDecoration = fixParentheses(extractedPlateDecor.joinToString("; ")),
                additions = fixParentheses(finalAdditions),
                material = fixParentheses(extractedMaterial),
                obelisk = fixParentheses(extractedObelisks.joinToString(", ")),
                annex = fixParentheses(extractedAnnexes.joinToString(", ")),
                shelf = fixParentheses(extractedShelves.joinToString(", ")),
                tumba = fixParentheses(extractedTumbas.joinToString(", ")),
                cvetnik = fixParentheses(extractedCvetniks.joinToString(", ")),
                plita = fixParentheses(extractedPlitas.joinToString(", ")),
                otmostkaTop = fixParentheses(extractedOtmostka),
                vase = fixParentheses(extractedVases.joinToString("; ")),
                lampada = fixParentheses(extractedLampadas.joinToString("; ")),
                demontazh = fixParentheses(extractedDemontazh.joinToString("; ")),
                tileTypeAndSize = fixParentheses(extractedTiles.joinToString("; ")),
                bordyur = fixParentheses(extractedBorders.joinToString("; "))
            )
        }

        fun fromEstimateDocumentData(docData: EstimateDocumentData): FactoryOrderDocumentData {
            val (lName, fName, mName) = splitFio(docData.deceasedName)
            val (birth, death) = extractDatesFromText(docData.engravingInfo)

            var extractedMaterial = ""
            var extractedObelisk = docData.steleDimensions
            var extractedTumba = docData.plinthDimensions
            var extractedCvetnik = docData.flowerbedDimensions
            var extractedPlita = ""
            var extractedOtmostka = ""
            val extractedVases = mutableListOf<String>()
            val extractedLampadas = mutableListOf<String>()
            val extractedDemontazh = mutableListOf<String>()
            val extractedTiles = mutableListOf<String>()
            val extractedBorders = mutableListOf<String>()
            val extractedCrosses = mutableListOf<String>()
            val extractedPhotos = mutableListOf<String>()
            val extractedFrames = mutableListOf<String>()
            val extractedPlateDecor = mutableListOf<String>()
            val extractedAdditions = mutableListOf<String>()

            docData.items.forEach { item ->
                val name = item.name.trim()
                val note = item.customNote.trim()
                val cleanItemName = cleanWorkshopText(name)
                val cleanItemNote = cleanWorkshopText(note)
                val desc = if (cleanItemNote.isNotBlank() && cleanItemNote != cleanItemName) {
                    "$cleanItemName ($cleanItemNote)"
                } else {
                    cleanItemName
                }

                when {
                    name.contains("Стела", ignoreCase = true) || name.contains("Обелиск", ignoreCase = true) -> {
                        if (extractedObelisk.isBlank()) extractedObelisk = desc
                    }
                    name.contains("Тумба", ignoreCase = true) -> {
                        if (extractedTumba.isBlank()) extractedTumba = desc
                    }
                    name.contains("Цветник", ignoreCase = true) -> {
                        if (extractedCvetnik.isBlank()) extractedCvetnik = desc
                    }
                    name.contains("Плита", ignoreCase = true) && !name.contains("плитк", ignoreCase = true) -> {
                        extractedPlita = desc
                    }
                    name.contains("Отмостка", ignoreCase = true) || name.contains("Фундамент", ignoreCase = true) -> {
                        extractedOtmostka = desc
                    }
                    name.contains("Ваза", ignoreCase = true) -> extractedVases.add("${PriceFormatter.formatNumber(item.quantity)} шт: $desc")
                    name.contains("Лампада", ignoreCase = true) -> extractedLampadas.add("${PriceFormatter.formatNumber(item.quantity)} шт: $desc")
                    name.contains("Демонтаж", ignoreCase = true) -> extractedDemontazh.add(desc)
                    name.contains("Плитк", ignoreCase = true) || name.contains("Керамогранит", ignoreCase = true) -> extractedTiles.add(desc)
                    name.contains("Бордюр", ignoreCase = true) -> extractedBorders.add(desc)
                    name.contains("Крест", ignoreCase = true) || name.contains("КР-", ignoreCase = true) -> extractedCrosses.add(desc)
                    name.contains("Фото", ignoreCase = true) || name.contains("Медальон", ignoreCase = true) ||
                    name.contains("Керамик", ignoreCase = true) || name.contains("Виньет", ignoreCase = true) -> extractedPhotos.add(desc)
                    name.contains("Рамка", ignoreCase = true) -> extractedFrames.add(desc)
                    name.contains("Оформление плиты", ignoreCase = true) -> extractedPlateDecor.add(desc)
                    else -> {
                        if (name.contains("Столб", ignoreCase = true) || name.contains("Скамь", ignoreCase = true) ||
                            name.contains("Стол", ignoreCase = true) || name.contains("Оград", ignoreCase = true)
                        ) {
                            extractedAdditions.add(desc)
                        }
                    }
                }
            }

            val hasGoldFio = docData.items.any { item ->
                val text = "${item.name} ${item.customNote}"
                (text.contains("золот", ignoreCase = true) || text.contains("сусальн", ignoreCase = true)) &&
                        (text.contains("ФИО", ignoreCase = true) || text.contains("букв", ignoreCase = true) || text.contains("дат", ignoreCase = true))
            }
            val hasGoldEpitaph = docData.items.any { item ->
                val text = "${item.name} ${item.customNote}"
                (text.contains("золот", ignoreCase = true) || text.contains("сусальн", ignoreCase = true)) &&
                        text.contains("эпитаф", ignoreCase = true)
            }
            val hasAnyGold = docData.items.any { item ->
                val text = "${item.name} ${item.customNote}"
                text.contains("золот", ignoreCase = true) || text.contains("сусальн", ignoreCase = true)
            } || docData.engravingInfo.contains("золот", ignoreCase = true) || docData.engravingInfo.contains("сусальн", ignoreCase = true)

            val finalEpitaph = buildString {
                append(docData.epitaphText)
                if (hasGoldEpitaph && !docData.epitaphText.contains("золот", ignoreCase = true) && !docData.epitaphText.contains("сусальн", ignoreCase = true)) {
                    if (isNotEmpty()) append(" ")
                    append("(СУСАЛЬНОЕ ЗОЛОТО)")
                }
            }

            if (hasGoldFio && extractedPlateDecor.none { it.contains("золот", ignoreCase = true) || it.contains("сусальн", ignoreCase = true) }) {
                extractedPlateDecor.add(0, "Сусальное золото (ФИО и даты)")
            } else if (hasAnyGold && extractedPlateDecor.none { it.contains("золот", ignoreCase = true) || it.contains("сусальн", ignoreCase = true) }) {
                extractedPlateDecor.add(0, "Сусальное золото")
            }

            val finalCross = if (extractedCrosses.isNotEmpty()) extractedCrosses.joinToString("; ")
            else if (docData.engravingInfo.contains("крест", ignoreCase = true)) "Крест на стеле" else ""
            val finalAdditions = buildString {
                if (extractedAdditions.isNotEmpty()) append(extractedAdditions.joinToString("; "))
                if (docData.notes.isNotBlank()) {
                    if (isNotEmpty()) append("; ")
                    append(docData.notes)
                }
            }

            val photoVal = extractedPhotos.map { cleanPhotoText(it) }.filter { it.isNotBlank() }.distinct().joinToString("; ")
            val frameVal = extractedFrames.map { cleanFrameText(it) }.filter { it.isNotBlank() }.distinct().joinToString("; ")

            return FactoryOrderDocumentData(
                orderNumber = docData.documentNumber,
                date = docData.date,
                clientName = docData.clientName,
                deadline = "",
                clientPhone = docData.clientPhone,
                installationAddress = docData.installationAddress,
                deceasedLastName = lName,
                deceasedFirstName = fName,
                deceasedMiddleName = mName,
                birthDate = birth,
                deathDate = death,
                cross = fixParentheses(finalCross),
                vignettePhoto = photoVal,
                frame = frameVal,
                epitaph = fixParentheses(finalEpitaph),
                plateDecoration = fixParentheses(extractedPlateDecor.joinToString("; ")),
                additions = fixParentheses(finalAdditions),
                material = fixParentheses(extractedMaterial),
                obelisk = fixParentheses(extractedObelisk),
                tumba = fixParentheses(extractedTumba),
                cvetnik = fixParentheses(extractedCvetnik),
                plita = fixParentheses(extractedPlita),
                otmostkaTop = fixParentheses(extractedOtmostka),
                vase = fixParentheses(extractedVases.joinToString("; ")),
                lampada = fixParentheses(extractedLampadas.joinToString("; ")),
                demontazh = fixParentheses(extractedDemontazh.joinToString("; ")),
                tileTypeAndSize = fixParentheses(extractedTiles.joinToString("; ")),
                bordyur = fixParentheses(extractedBorders.joinToString("; "))
            )
        }
    }
}
