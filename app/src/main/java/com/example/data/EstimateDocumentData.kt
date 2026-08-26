package com.example.data

import com.example.ui.CalculationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EstimateDocumentItem(
    val number: Int,
    val name: String,
    val category: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val customNote: String = ""
)

data class EstimateDocumentData(
    val documentNumber: String,
    val date: String,
    val organizationName: String = "ИП Ступак О.О.",
    val organizationContacts: String = "Viber, Telegram тел.: +375295421809, +375297273637",
    val organizationAddress: String = "«ДВА АНГЕЛА» Ольшаны рынок БРОДОК павильон 35",
    val clientName: String = "",
    val clientPhone: String = "",
    val installationAddress: String = "",
    val deceasedName: String = "",
    val deceasedBirthDate: String = "",
    val deceasedDeathDate: String = "",
    val items: List<EstimateDocumentItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val prepayment: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val notes: String = "",
    val monumentMaterial: String = "",
    val steleDimensions: String = "",
    val plinthDimensions: String = "",
    val flowerbedDimensions: String = "",
    val engravingInfo: String = "",
    val epitaphText: String = ""
) {
    companion object {
        private fun extractBundleField(note: String, label: String, nextLabels: List<String>): String? {
            val options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            // 1. Bracket format: "label: [content]"
            val bracketMatch = Regex("""$label:\s*\[(.*?)\]""", options).find(note)
            if (bracketMatch != null && bracketMatch.groupValues[1].isNotBlank()) {
                return bracketMatch.groupValues[1].trim()
            }

            // 2. Pipe-separated format: "label: content |"
            val pipeMatch = Regex("""$label:\s*(.*?)(?:\s*\|\s*|$)""", options).find(note)
            if (pipeMatch != null && pipeMatch.groupValues[1].isNotBlank()) {
                val cand = pipeMatch.groupValues[1].trim()
                if (!nextLabels.any { cand.contains(it, ignoreCase = true) }) {
                    return cand
                }
            }

            // 3. Fallback lookahead before next section keyword or end of string (DO NOT truncate on dots)
            val lookahead = if (nextLabels.isNotEmpty()) {
                val escaped = nextLabels.joinToString("|") { Regex.escape(it) }
                """$label:\s*(.*?)(?=(?:$escaped)|\s*Вес:|\s*$)"""
            } else {
                """$label:\s*(.*?)(?=\s*Вес:|\s*$)"""
            }
            val match = Regex(lookahead, options).find(note)
            if (match != null) {
                val res = match.groupValues[1].trim().trimEnd('|', ';', ',')
                if (res.isNotBlank()) return res
            }
            return null
        }

        private fun extractCleanDeceasedName(rawText: String): String {
            if (rawText.isBlank()) return ""
            // Get non-empty lines
            val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
            for (line in lines) {
                // Remove date formats like 12.05.1950, 1950-2023, 12.05.1950 - 24.11.2023, (р. 1950), etc.
                val clean = line
                    .replace(Regex("""\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b"""), "")
                    .replace(Regex("""\b\d{4}\s*[-–—]\s*\d{4}\b"""), "")
                    .replace(Regex("""\b\d{4}\b"""), "")
                    .replace(Regex("""[()«»"–—\-]"""), " ")
                    .replace(Regex("""\b(р|ум|г|гг|г\.|р\.|ум\.)\b""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                if (clean.any { it.isLetter() }) {
                    return clean
                }
            }
            return rawText.trim()
        }

        fun fromCalculationState(
            state: CalculationState,
            currentDate: Date = Date()
        ): EstimateDocumentData {
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(currentDate)

            var stele = ""
            var plinth = ""
            var flowerbed = ""
            var epitaph = ""
            var fioEngraving = ""
            val otherEngraving = mutableListOf<String>()

            state.selectedItems.forEach { item ->
                val note = item.customNote.trim()
                when {
                    item.name.contains("Стела", ignoreCase = true) -> stele = note.ifBlank { item.name }
                    item.name.contains("Тумба", ignoreCase = true) -> plinth = note.ifBlank { item.name }
                    item.name.contains("Цветник", ignoreCase = true) || item.name.contains("Плита", ignoreCase = true) -> flowerbed = note.ifBlank { item.name }
                    item.name.contains("ФИО", ignoreCase = true) -> {
                        fioEngraving = note.ifBlank { item.name }
                    }
                    item.name.contains("Эпитафия", ignoreCase = true) -> {
                        epitaph = note.ifBlank { item.name }
                    }
                    item.category.contains("Гравировка", ignoreCase = true) || item.category.contains("Художественное", ignoreCase = true) -> {
                        val textWithNote = if (note.isNotBlank() && note != item.name) "${item.name} ($note)" else item.name
                        otherEngraving.add(textWithNote)
                    }
                    // Extract from bundle note if present
                    item.customNote.contains("Гравировка ФИО:", ignoreCase = true) || item.customNote.contains("Эпитафия:", ignoreCase = true) -> {
                        if (fioEngraving.isBlank()) {
                            val extractedFio = extractBundleField(item.customNote, "Гравировка ФИО", listOf("Эпитафия:", "Вес:"))
                            if (!extractedFio.isNullOrBlank()) fioEngraving = extractedFio
                        }
                        if (epitaph.isBlank()) {
                            val extractedEp = extractBundleField(item.customNote, "Эпитафия", listOf("Вес:"))
                            if (!extractedEp.isNullOrBlank()) epitaph = extractedEp
                        }
                    }
                }
            }

            val finalEngravingInfo = buildString {
                if (fioEngraving.isNotBlank()) {
                    append(fioEngraving)
                } else if (state.deceasedName.isNotBlank()) {
                    append(state.deceasedName)
                }
                if (otherEngraving.isNotEmpty()) {
                    if (isNotEmpty()) append("; ")
                    append(otherEngraving.joinToString("; "))
                }
            }

            val docItems = state.selectedItems.mapIndexed { index, item ->
                EstimateDocumentItem(
                    number = index + 1,
                    name = item.name,
                    category = item.category,
                    unit = item.unit,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    customNote = item.customNote
                )
            }

            val addressStr = buildString {
                if (state.cemeteryName.isNotBlank()) append(state.cemeteryName)
                if (state.plotNumber.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append("уч. ").append(state.plotNumber)
                }
            }

            var finalDeceasedName = state.deceasedName.trim()
            if (finalDeceasedName.isBlank() && fioEngraving.isNotBlank()) {
                finalDeceasedName = extractCleanDeceasedName(fioEngraving)
            }

            return EstimateDocumentData(
                documentNumber = state.orderNumber,
                date = dateStr,
                clientName = state.clientName,
                clientPhone = state.clientPhone,
                installationAddress = addressStr,
                deceasedName = finalDeceasedName,
                items = docItems,
                subtotal = state.subtotal,
                discountPercent = state.discountPercent,
                discountAmount = state.discountAmount,
                grandTotal = state.total,
                prepayment = state.prepayment,
                remainingAmount = state.remainingAmount,
                notes = state.notes,
                steleDimensions = stele,
                plinthDimensions = plinth,
                flowerbedDimensions = flowerbed,
                engravingInfo = finalEngravingInfo,
                epitaphText = epitaph
            )
        }

        fun fromSavedOrder(
            order: SavedOrder,
            items: List<CalculatedItemData>
        ): EstimateDocumentData {
            val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(order.createdAt))

            var stele = ""
            var plinth = ""
            var flowerbed = ""
            var epitaph = ""
            var fioEngraving = ""
            val otherEngraving = mutableListOf<String>()

            items.forEach { item ->
                val note = item.customNote.trim()
                when {
                    item.name.contains("Стела", ignoreCase = true) -> stele = note.ifBlank { item.name }
                    item.name.contains("Тумба", ignoreCase = true) -> plinth = note.ifBlank { item.name }
                    item.name.contains("Цветник", ignoreCase = true) || item.name.contains("Плита", ignoreCase = true) -> flowerbed = note.ifBlank { item.name }
                    item.name.contains("ФИО", ignoreCase = true) -> {
                        fioEngraving = note.ifBlank { item.name }
                    }
                    item.name.contains("Эпитафия", ignoreCase = true) -> {
                        epitaph = note.ifBlank { item.name }
                    }
                    item.category.contains("Гравировка", ignoreCase = true) || item.category.contains("Художественное", ignoreCase = true) -> {
                        val textWithNote = if (note.isNotBlank() && note != item.name) "${item.name} ($note)" else item.name
                        otherEngraving.add(textWithNote)
                    }
                    item.customNote.contains("Гравировка ФИО:", ignoreCase = true) || item.customNote.contains("Эпитафия:", ignoreCase = true) -> {
                        if (fioEngraving.isBlank()) {
                            val extractedFio = extractBundleField(item.customNote, "Гравировка ФИО", listOf("Эпитафия:", "Вес:"))
                            if (!extractedFio.isNullOrBlank()) fioEngraving = extractedFio
                        }
                        if (epitaph.isBlank()) {
                            val extractedEp = extractBundleField(item.customNote, "Эпитафия", listOf("Вес:"))
                            if (!extractedEp.isNullOrBlank()) epitaph = extractedEp
                        }
                    }
                }
            }

            val finalEngravingInfo = buildString {
                if (fioEngraving.isNotBlank()) {
                    append(fioEngraving)
                } else if (order.deceasedName.isNotBlank()) {
                    append(order.deceasedName)
                }
                if (otherEngraving.isNotEmpty()) {
                    if (isNotEmpty()) append("; ")
                    append(otherEngraving.joinToString("; "))
                }
            }

            val docItems = items.mapIndexed { index, item ->
                EstimateDocumentItem(
                    number = index + 1,
                    name = item.name,
                    category = item.category,
                    unit = item.unit,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    customNote = item.customNote
                )
            }

            val addressStr = buildString {
                if (order.cemeteryName.isNotBlank()) append(order.cemeteryName)
                if (order.plotNumber.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append("уч. ").append(order.plotNumber)
                }
            }

            var finalDeceasedName = order.deceasedName.trim()
            if (finalDeceasedName.isBlank() && fioEngraving.isNotBlank()) {
                finalDeceasedName = extractCleanDeceasedName(fioEngraving)
            }

            return EstimateDocumentData(
                documentNumber = order.orderNumber,
                date = dateStr,
                clientName = order.clientName,
                clientPhone = order.clientPhone,
                installationAddress = addressStr,
                deceasedName = finalDeceasedName,
                items = docItems,
                subtotal = order.subtotalAmount,
                discountPercent = order.discountPercent,
                discountAmount = order.discountAmount,
                grandTotal = order.totalAmount,
                prepayment = order.prepaymentAmount,
                remainingAmount = order.remainingAmount,
                notes = order.notes,
                steleDimensions = stele,
                plinthDimensions = plinth,
                flowerbedDimensions = flowerbed,
                engravingInfo = finalEngravingInfo,
                epitaphText = epitaph
            )
        }
    }
}
