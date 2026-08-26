package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.EstimateDocumentData
import com.example.data.PriceFormatter
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxGenerator {

    fun generateDocx(context: Context, data: EstimateDocumentData): File {
        val safeFileName = buildString {
            append("Смета_№")
            append(data.documentNumber.replace("[^a-zA-Z0-9А-Яа-я—_-]".toRegex(), "_"))
            if (data.clientName.isNotBlank()) {
                append("_")
                append(data.clientName.trim().replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9А-Яа-я—_-]".toRegex(), ""))
            }
            append("_")
            append(data.date.replace(".", "-"))
            append(".docx")
        }

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "docx_estimates")
        outputDir.mkdirs()
        val outputFile = File(outputDir, safeFileName)

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(getContentTypesXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(getRelsXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(getWordRelsXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. word/styles.xml
            zos.putNextEntry(ZipEntry("word/styles.xml"))
            zos.write(getStylesXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. word/settings.xml
            zos.putNextEntry(ZipEntry("word/settings.xml"))
            zos.write(getSettingsXml().toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 6. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(buildDocumentXml(data).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        return outputFile
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("/\\", "")
            .replace("\\/", "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>
</Types>"""
    }

    private fun getRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun getWordRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
</Relationships>"""
    }

    private fun getStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
        <w:lang w:val="ru-RU"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>"""
    }

    private fun getSettingsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:zoom w:percent="100"/>
  <w:defaultTabStop w:val="720"/>
  <w:characterSpacingControl w:val="doNotCompress"/>
</w:settings>"""
    }

    private fun buildDocumentXml(data: EstimateDocumentData): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("<w:body>")

        // --- 1. DOCUMENT TITLE ---
        sb.append(makeParagraph("СМЕТНО-ДОГОВОРНОЙ ЗАКАЗ № ${data.documentNumber}", align = "center", bold = true, sizeHalfPoints = 28, spaceBefore = 100, spaceAfter = 60))
        sb.append(makeParagraph("от ${data.date} г.", align = "center", italic = true, sizeHalfPoints = 22, spaceAfter = 200))

        // --- 2. CUSTOMER & ORDER INFO TABLE ---
        val infoTableRows = listOf(
            listOf("Дата заказа:", data.date, "Ф.И.О. Заказчика:", data.clientName.ifBlank { "_________________________________" }),
            listOf("Телефон:", data.clientPhone.ifBlank { "____________________" }, "Адрес установки:", data.installationAddress.ifBlank { "_________________________________" })
        )
        sb.append(makeTable(infoTableRows, widthsDxa = listOf(2200, 2400, 2400, 2600), showBorders = true, headerBg = null))

        sb.append(makeParagraph("", spaceAfter = 100))

        // --- 3. ENGRAVING & DECEASED DETAILS TABLE ---
        val engravingTableRows = listOf(
            listOf("Ф.И.О. усопшего:", data.deceasedName.ifBlank { "_________________________________" }),
            listOf("Гравировка ФИО (даты рождения/смерти):", data.engravingInfo.ifBlank { "_________________________________" }),
            listOf("Эпитафия:", data.epitaphText.ifBlank { "_________________________________" })
        )
        sb.append(makeTable(engravingTableRows, widthsDxa = listOf(3800, 5800), showBorders = true, headerBg = null))

        sb.append(makeParagraph("", spaceAfter = 160))

        // --- 3. ESTIMATE ITEMS TABLE ---
        sb.append(makeParagraph("Перечень товаров и выполняемых работ:", bold = true, sizeHalfPoints = 24, spaceAfter = 100))

        val itemsTableData = mutableListOf<List<String>>()
        // Header
        itemsTableData.add(listOf("№", "Наименование позиций", "Ед.", "Кол-во", "Цена (BYN)", "Сумма (BYN)"))

        // Rows
        data.items.forEach { item ->
            itemsTableData.add(
                listOf(
                    item.number.toString(),
                    if (item.customNote.isNotBlank() && item.customNote != item.name) "${item.name}\n(${item.customNote})" else item.name,
                    item.unit,
                    PriceFormatter.formatNumber(item.quantity),
                    PriceFormatter.formatNumber(item.unitPrice),
                    PriceFormatter.formatNumber(item.totalPrice)
                )
            )
        }

        sb.append(makeTable(itemsTableData, widthsDxa = listOf(600, 4400, 800, 1000, 1400, 1400), showBorders = true, headerBg = "EFEFEF"))

        sb.append(makeParagraph("", spaceAfter = 160))

        // --- 4. SUMMARY / TOTALS TABLE ---
        val summaryRows = mutableListOf<List<String>>()
        summaryRows.add(listOf("Сумма по смете:", PriceFormatter.formatRub(data.subtotal)))
        if (data.discountPercent > 0) {
            summaryRows.add(listOf("Скидка (${PriceFormatter.formatNumber(data.discountPercent)}%):", "-${PriceFormatter.formatRub(data.discountAmount)}"))
        }
        summaryRows.add(listOf("ИТОГО К ОПЛАТЕ:", PriceFormatter.formatRub(data.grandTotal)))
        if (data.prepayment > 0) {
            summaryRows.add(listOf("Внесена предоплата:", PriceFormatter.formatRub(data.prepayment)))
            summaryRows.add(listOf("Остаток к доплате:", PriceFormatter.formatRub(data.remainingAmount)))
        }

        val rightAlignedSummaryTable = makeSummaryTable(summaryRows)
        sb.append(rightAlignedSummaryTable)

        if (data.notes.isNotBlank()) {
            sb.append(makeParagraph("", spaceAfter = 100))
            sb.append(makeParagraph("Примечание к заказу: ${data.notes}", italic = true, sizeHalfPoints = 20, spaceAfter = 140))
        } else {
            sb.append(makeParagraph("", spaceAfter = 180))
        }

        // --- 5. AGREEMENT & SIGNATURES WITH EXECUTIVE INFO AT BOTTOM ---
        sb.append(makeParagraph("С оформлением, комплектацией и стоимостью заказа согласен.", italic = true, sizeHalfPoints = 20, spaceAfter = 160))

        val signatureTable = listOf(
            listOf(
                "ЗАКАЗЧИК:\n\n_____________________ (${data.clientName.ifBlank { "Ф.И.О." }})",
                "ИСПОЛНИТЕЛЬ:\n${data.organizationName}\n${data.organizationContacts}\n${data.organizationAddress}\n\n_____________________ (М.П.)"
            )
        )
        sb.append(makeTable(signatureTable, widthsDxa = listOf(4800, 4800), showBorders = false, headerBg = null))

        // Page properties (A4 with 1.5 cm margins)
        sb.append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="850" w:right="850" w:bottom="850" w:left="850"/></w:sectPr>""")
        sb.append("</w:body></w:document>")

        return sb.toString()
    }

    private fun makeParagraph(
        text: String,
        align: String = "left",
        bold: Boolean = false,
        italic: Boolean = false,
        sizeHalfPoints: Int = 22,
        spaceBefore: Int = 0,
        spaceAfter: Int = 0
    ): String {
        val escaped = escapeXml(text)
        val jcVal = when (align.lowercase()) {
            "center" -> "center"
            "right" -> "right"
            else -> "left"
        }

        val lines = escaped.split("\n")
        val runsXml = lines.mapIndexed { index, line ->
            val breakTag = if (index > 0) "<w:br/>" else ""
            """<w:r><w:rPr>${if (bold) "<w:b/>" else ""}${if (italic) "<w:i/>" else ""}<w:sz w:val="$sizeHalfPoints"/><w:szCs w:val="$sizeHalfPoints"/></w:rPr>$breakTag<w:t xml:space="preserve">$line</w:t></w:r>"""
        }.joinToString("")

        return """<w:p><w:pPr><w:jc w:val="$jcVal"/><w:spacing w:before="$spaceBefore" w:after="$spaceAfter"/></w:pPr>$runsXml</w:p>"""
    }

    private fun makeTable(
        tableData: List<List<String>>,
        widthsDxa: List<Int>,
        showBorders: Boolean = true,
        headerBg: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("<w:tbl>")
        sb.append("""<w:tblPr><w:tblW w:w="0" w:type="auto"/>""")

        if (showBorders) {
            sb.append("""<w:tblBorders>
                <w:top w:val="single" w:sz="4" w:space="0" w:color="B0C4DE"/>
                <w:left w:val="single" w:sz="4" w:space="0" w:color="B0C4DE"/>
                <w:bottom w:val="single" w:sz="4" w:space="0" w:color="B0C4DE"/>
                <w:right w:val="single" w:sz="4" w:space="0" w:color="B0C4DE"/>
                <w:insideH w:val="single" w:sz="4" w:space="0" w:color="D3D3D3"/>
                <w:insideV w:val="single" w:sz="4" w:space="0" w:color="D3D3D3"/>
            </w:tblBorders>""")
        } else {
            sb.append("<w:tblBorders><w:top w:val=\"none\"/><w:left w:val=\"none\"/><w:bottom w:val=\"none\"/><w:right w:val=\"none\"/></w:tblBorders>")
        }

        sb.append("""<w:tblCellMar><w:top w:w="120" w:type="dxa"/><w:bottom w:w="120" w:type="dxa"/><w:left w:w="160" w:type="dxa"/><w:right w:w="160" w:type="dxa"/></w:tblCellMar>""")
        sb.append("</w:tblPr>")

        tableData.forEachIndexed { rowIndex, row ->
            val isHeader = (rowIndex == 0 && headerBg != null)
            sb.append("<w:tr>")
            if (isHeader) sb.append("<w:trPr><w:tblHeader/><w:cantSplit/></w:trPr>")
            else sb.append("<w:trPr><w:cantSplit/></w:trPr>")

            row.forEachIndexed { colIndex, cellText ->
                val colWidth = widthsDxa.getOrElse(colIndex) { 1500 }
                val isBold = isHeader || (colIndex == 0 && row.size == 2 && showBorders)
                val bgColor = if (isHeader && headerBg != null) headerBg else null

                sb.append("<w:tc>")
                sb.append("<w:tcPr>")
                sb.append("""<w:tcW w:w="$colWidth" w:type="dxa"/>""")
                if (bgColor != null) {
                    sb.append("""<w:shd w:val="clear" w:color="auto" w:fill="$bgColor"/>""")
                }
                sb.append("</w:tcPr>")

                val align = if (colIndex >= 3 && row.size >= 5) "right" else "left"
                sb.append(makeParagraph(cellText, align = align, bold = isBold, sizeHalfPoints = if (isHeader) 22 else 20, spaceAfter = 20))
                sb.append("</w:tc>")
            }
            sb.append("</w:tr>")
        }

        sb.append("</w:tbl>")
        return sb.toString()
    }

    private fun makeSummaryTable(summaryRows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("<w:tbl>")
        sb.append("""<w:tblPr><w:tblW w:w="5000" w:type="dxa"/><w:jc w:val="right"/>""")
        sb.append("""<w:tblBorders><w:top w:val="none"/><w:left w:val="none"/><w:bottom w:val="single" w:sz="4" w:color="B0C4DE"/><w:right w:val="none"/></w:tblBorders>""")
        sb.append("""<w:tblCellMar><w:top w:w="80" w:type="dxa"/><w:bottom w:w="80" w:type="dxa"/><w:left w:w="120" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tblCellMar>""")
        sb.append("</w:tblPr>")

        summaryRows.forEach { row ->
            val label = row[0]
            val value = row[1]
            val isTotalRow = label.contains("ИТОГО", ignoreCase = true)

            sb.append("<w:tr><w:trPr><w:cantSplit/></w:trPr>")

            // Label cell
            sb.append("""<w:tc><w:tcPr><w:tcW w:w="3000" w:type="dxa"/></w:tcPr>""")
            sb.append(makeParagraph(label, align = "right", bold = isTotalRow, sizeHalfPoints = if (isTotalRow) 24 else 22, spaceAfter = 20))
            sb.append("</w:tc>")

            // Value cell
            sb.append("""<w:tc><w:tcPr><w:tcW w:w="2000" w:type="dxa"/></w:tcPr>""")
            sb.append(makeParagraph(value, align = "right", bold = isTotalRow, sizeHalfPoints = if (isTotalRow) 26 else 22, spaceAfter = 20))
            sb.append("</w:tc>")

            sb.append("</w:tr>")
        }

        sb.append("</w:tbl>")
        return sb.toString()
    }
}

