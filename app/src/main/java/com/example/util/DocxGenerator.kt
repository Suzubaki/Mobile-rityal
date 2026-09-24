package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.EstimateDocumentData
import com.example.data.FactoryOrderDocumentData
import com.example.data.PriceFormatter
import java.io.File
import java.io.FileInputStream
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

    /**
     * Copies generated .docx to standard public Downloads folder so it is immediately
     * accessible from Windows PC host when using BlueStacks 5.
     */
    fun saveDocxToPublicDownloads(context: Context, data: EstimateDocumentData): File {
        val file = generateDocx(context, data)
        return try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads != null) {
                publicDownloads.mkdirs()
                val targetFile = File(publicDownloads, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else {
                file
            }
        } catch (e: Exception) {
            file
        }
    }

    /**
     * Создание файла Word (.docx) бланка наряда для завода / цеха (БЕЗ ЦЕННИКОВ).
     */
    fun generateFactoryDocx(context: Context, data: FactoryOrderDocumentData): File {
        val safeFileName = buildString {
            append("Заказ_Завод_№")
            append(data.orderNumber.replace("[^a-zA-Z0-9А-Яа-я—_-]".toRegex(), "_"))
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
            zos.write(buildFactoryDocumentXml(data).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }

        return outputFile
    }

    /**
     * Сохранение бланка для завода в общедоступную папку Загрузки (Downloads) для хост-ПК (BlueStacks).
     */
    fun saveFactoryDocxToPublicDownloads(context: Context, data: FactoryOrderDocumentData): File {
        val file = generateFactoryDocx(context, data)
        return try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads != null) {
                publicDownloads.mkdirs()
                val targetFile = File(publicDownloads, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else {
                file
            }
        } catch (e: Exception) {
            file
        }
    }

    /**
     * Launches external Word application or Android document viewer for the .docx file.
     */
    fun openDocxFile(context: Context, file: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Открыть смету в Word (${file.name})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareDocxFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Поделиться сметным заказом (${file.name})")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun getOrExtractPcHtmlFile(context: Context): File {
        val targetFile = File(context.cacheDir, "pc_orders.html")
        try {
            context.assets.open("pc_orders.html").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.w("DocxGenerator", "Failed to extract pc_orders.html from assets: ${e.message}")
        }
        return targetFile
    }

    fun sharePcHtmlApp(context: Context) {
        val file = getOrExtractPcHtmlFile(context)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ПК-версия для печати заказов (Два Ангела)")
            putExtra(Intent.EXTRA_TEXT, "Файл приложения для ПК. Откройте его на компьютере двойным кликом в браузере (Chrome / Яндекс / Edge) для просмотра заказов и прямой печати договоров на принтер.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Отправить файл ПК-версии (pc_orders.html)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun savePcHtmlToDownloads(context: Context): File? {
        val sourceFile = getOrExtractPcHtmlFile(context)
        return try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads != null) {
                publicDownloads.mkdirs()
                val targetFile = File(publicDownloads, "pc_orders.html")
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else {
                sourceFile
            }
        } catch (e: Exception) {
            sourceFile
        }
    }

    /**
     * Formats estimate as human-readable plain text for clipboard / messaging.
     */
    fun generatePlainText(data: EstimateDocumentData): String {
        return buildString {
            appendLine("══════════════════════════════════════════")
            appendLine("СМЕТНО-ДОГОВОРНОЙ ЗАКАЗ № ${data.documentNumber}")
            appendLine("Дата: ${data.date}")
            appendLine("Исполнитель: ${data.organizationName}")
            appendLine("Контакты: ${data.organizationContacts}")
            appendLine("Адрес: ${data.organizationAddress}")
            appendLine("──────────────────────────────────────────")
            if (data.clientName.isNotBlank()) appendLine("Заказчик: ${data.clientName}")
            if (data.clientPhone.isNotBlank()) appendLine("Телефон: ${data.clientPhone}")
            if (data.installationAddress.isNotBlank()) appendLine("Адрес установки / Кладбище: ${data.installationAddress}")
            if (data.deceasedName.isNotBlank()) appendLine("Усопший: ${data.deceasedName}")
            if (data.epitaphText.isNotBlank()) appendLine("Эпитафия: «${data.epitaphText}»")
            appendLine("──────────────────────────────────────────")
            appendLine("ПЕРЕЧЕНЬ ПОЗИЦИЙ:")
            data.items.forEach { item ->
                appendLine("${item.number}. ${item.name}")
                if (item.customNote.isNotBlank() && item.customNote != item.name) {
                    appendLine("   (${item.customNote})")
                }
                appendLine("   ${PriceFormatter.formatNumber(item.quantity)} ${item.unit} × ${PriceFormatter.formatRub(item.unitPrice)} = ${PriceFormatter.formatRub(item.totalPrice)}")
            }
            appendLine("──────────────────────────────────────────")
            appendLine("ИТОГО: ${PriceFormatter.formatRub(data.subtotal)}")
            if (data.discountPercent > 0) {
                appendLine("Скидка (${PriceFormatter.formatNumber(data.discountPercent)}%): -${PriceFormatter.formatRub(data.discountAmount)}")
            }
            appendLine("К ОПЛАТЕ: ${PriceFormatter.formatRub(data.grandTotal)}")
            if (data.prepayment > 0) {
                appendLine("Предоплата: ${PriceFormatter.formatRub(data.prepayment)}")
                appendLine("Остаток: ${PriceFormatter.formatRub(data.remainingAmount)}")
            }
            if (data.notes.isNotBlank()) {
                appendLine("──────────────────────────────────────────")
                appendLine("Примечание: ${data.notes}")
            }
            appendLine("══════════════════════════════════════════")
        }
    }

    /**
     * Generates printable HTML for webview printing / direct PDF export.
     */
    fun generateHtml(data: EstimateDocumentData): String {
        val rowsHtml = StringBuilder()
        data.items.forEach { item ->
            val noteHtml = if (item.customNote.isNotBlank() && item.customNote != item.name) {
                "<div style='font-size: 10px; color: #555; margin-top: 1px;'>${escapeXml(item.customNote)}</div>"
            } else ""
            rowsHtml.append("""
                <tr>
                    <td style='text-align: center;'>${item.number}</td>
                    <td><strong>${escapeXml(item.name)}</strong>$noteHtml</td>
                    <td style='text-align: center;'>${escapeXml(item.unit)}</td>
                    <td style='text-align: right;'>${PriceFormatter.formatNumber(item.quantity)}</td>
                    <td style='text-align: right;'>${PriceFormatter.formatNumber(item.unitPrice)}</td>
                    <td style='text-align: right;'><strong>${PriceFormatter.formatNumber(item.totalPrice)}</strong></td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Смета № ${escapeXml(data.documentNumber)}</title>
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 8mm 10mm 8mm 10mm;
                    }
                    * { box-sizing: border-box; }
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; font-size: 11px; color: #111; padding: 0; margin: 0; line-height: 1.25; }
                    .header { text-align: center; border-bottom: 2px solid #2b5797; padding-bottom: 4px; margin-bottom: 8px; }
                    .header h1 { font-size: 16px; margin: 0 0 2px 0; color: #2b5797; text-transform: uppercase; }
                    .header .date { font-size: 11px; color: #555; font-style: italic; }
                    .info-grid { display: flex; flex-wrap: wrap; margin-bottom: 8px; border: 1px solid #ddd; background: #fdfdfd; border-radius: 4px; padding: 6px; }
                    .info-col { flex: 1 1 45%; margin: 2px 6px; }
                    .info-label { font-size: 9px; text-transform: uppercase; color: #777; font-weight: bold; }
                    .info-value { font-size: 11px; font-weight: 500; }
                    table { width: 100%; border-collapse: collapse; margin-top: 6px; margin-bottom: 8px; page-break-inside: auto; }
                    tr { page-break-inside: avoid; page-break-after: auto; }
                    th, td { border: 1px solid #ccc; padding: 4px 6px; font-size: 10px; }
                    th { background-color: #f2f5f9; color: #2b5797; font-weight: bold; }
                    .totals-box { width: 280px; margin-left: auto; margin-top: 6px; margin-bottom: 8px; page-break-inside: avoid; }
                    .total-row { display: flex; justify-content: space-between; padding: 2px 0; font-size: 11px; border-bottom: 1px dashed #eee; }
                    .grand-total { font-size: 13px; font-weight: bold; color: #2b5797; border-top: 2px solid #2b5797; border-bottom: 2px solid #2b5797; padding: 4px 0; }
                    .signatures { display: flex; justify-content: space-between; margin-top: 12px; padding-top: 8px; border-top: 1px solid #ccc; page-break-inside: avoid; }
                    .sig-col { flex: 1; margin: 0 12px; font-size: 10px; }
                    .sig-line { margin-top: 18px; border-bottom: 1px solid #333; }
                    @media print {
                        html, body {
                            width: 100%;
                            height: 100%;
                            overflow: hidden;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>СМЕТНО-ДОГОВОРНОЙ ЗАКАЗ № ${escapeXml(data.documentNumber)}</h1>
                    <div class="date">от ${escapeXml(data.date)} г.</div>
                </div>

                <div class="info-grid">
                    <div class="info-col">
                        <div class="info-label">Исполнитель</div>
                        <div class="info-value">${escapeXml(data.organizationName)}<br>${escapeXml(data.organizationContacts)}<br>${escapeXml(data.organizationAddress)}</div>
                    </div>
                    <div class="info-col">
                        <div class="info-label">Заказчик</div>
                        <div class="info-value"><strong>${escapeXml(data.clientName.ifBlank { "—" })}</strong><br>Тел: ${escapeXml(data.clientPhone.ifBlank { "—" })}</div>
                    </div>
                    ${if (data.deceasedName.isNotBlank()) """
                    <div class="info-col" style="flex: 1 1 100%;">
                        <div class="info-label">ФИО Усопшего</div>
                        <div class="info-value"><strong>${escapeXml(data.deceasedName)}</strong></div>
                    </div>
                    """.trimIndent() else ""}
                    ${if (data.installationAddress.isNotBlank()) """
                    <div class="info-col" style="flex: 1 1 100%;">
                        <div class="info-label">Адрес установки / Кладбище</div>
                        <div class="info-value">${escapeXml(data.installationAddress)}</div>
                    </div>
                    """.trimIndent() else ""}
                </div>

                <table>
                    <thead>
                        <tr>
                            <th style="width: 30px;">№</th>
                            <th>Наименование позиций</th>
                            <th style="width: 50px;">Ед.</th>
                            <th style="width: 60px;">Кол-во</th>
                            <th style="width: 80px;">Цена (BYN)</th>
                            <th style="width: 90px;">Сумма (BYN)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>

                <div class="totals-box">
                    <div class="total-row"><span>Сумма по смете:</span><span>${PriceFormatter.formatRub(data.subtotal)}</span></div>
                    ${if (data.discountPercent > 0) """<div class="total-row"><span>Скидка (${PriceFormatter.formatNumber(data.discountPercent)}%):</span><span>-${PriceFormatter.formatRub(data.discountAmount)}</span></div>""" else ""}
                    <div class="total-row grand-total"><span>ИТОГО К ОПЛАТЕ:</span><span>${PriceFormatter.formatRub(data.grandTotal)}</span></div>
                    ${if (data.prepayment > 0) """
                    <div class="total-row"><span>Внесена предоплата:</span><span>${PriceFormatter.formatRub(data.prepayment)}</span></div>
                    <div class="total-row" style="font-weight: bold; color: #b71c1c;"><span>Остаток к доплате:</span><span>${PriceFormatter.formatRub(data.remainingAmount)}</span></div>
                    """.trimIndent() else ""}
                </div>

                ${if (data.notes.isNotBlank()) """
                <div style="font-size: 10px; font-style: italic; color: #444; margin-bottom: 8px; background: #fafafa; padding: 6px; border-left: 3px solid #2b5797;">
                    <strong>Примечание:</strong> ${escapeXml(data.notes)}
                </div>
                """.trimIndent() else ""}

                <div class="signatures">
                    <div class="sig-col">
                        <strong>ЗАКАЗЧИК:</strong>
                        <div class="sig-line"></div>
                        <div style="margin-top: 2px; color: #666; font-size: 10px;">(подпись / ${escapeXml(data.clientName.ifBlank { "Ф.И.О." })})</div>
                    </div>
                    <div class="sig-col">
                        <strong>ИСПОЛНИТЕЛЬ:</strong>
                        <div class="sig-line"></div>
                        <div style="margin-top: 2px; color: #666; font-size: 10px;">(подпись / М.П. ${escapeXml(data.organizationName)})</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Prints document or exports to PDF via Android Print Framework.
     */
    fun printDocument(context: Context, data: EstimateDocumentData) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Смета_№${data.documentNumber}")
                val jobName = "Смета №${data.documentNumber} - ${data.clientName}"
                printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        val html = generateHtml(data)
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    /**
     * Текстовая версия бланка для завода (без ценников).
     */
    fun generateFactoryPlainText(data: FactoryOrderDocumentData): String {
        return buildString {
            appendLine("Дата: ${data.date.ifBlank { "__________" }}. Ф. И О заказчика: ${data.clientName.ifBlank { "____________________" }}  Срок: ${data.deadline.ifBlank { "__________" }}")
            appendLine("Телефон: ${data.clientPhone.ifBlank { "_______________" }}  адрес уст-ки: ${data.installationAddress.ifBlank { "____________________" }}")
            appendLine("")
            appendLine("Фамилия.ум.: ${data.deceasedLastName.ifBlank { "________________" }}  Имя ум.: ${data.deceasedFirstName.ifBlank { "________________" }}")
            appendLine("Отчество ум.: ${data.deceasedMiddleName.ifBlank { "________________" }}")
            appendLine("Ч.М.Г.рождения: ${data.birthDate.ifBlank { "__________" }}  Ч.М.Г.смерти: ${data.deathDate.ifBlank { "__________" }}")
            appendLine("Крест: ${data.cross.ifBlank { "__________" }}  виньетка(фото): ${data.vignettePhoto.ifBlank { "__________" }}  рамка: ${data.frame.ifBlank { "__________" }}")
            appendLine("Эпитафия.: ${data.epitaph.ifBlank { "________________________________________________" }}")
            appendLine("_________________________________________________________________")
            appendLine("Оформление")
            appendLine("плиты: ${data.plateDecoration.ifBlank { "__________________________________________________" }}")
            appendLine("_________________________________________________________________")
            appendLine("Дополнения: ${data.additions.ifBlank { "_____________________________________________" }}")
            appendLine("_________________________________________________________________")
            appendLine("─────────────────────────────────────────────────────────────────")
            appendLine(".                     ФОРМА И РАЗМЕРЫ П-КА")
            appendLine("Материал: ${data.material.ifBlank { "____________________" }}")
            appendLine("Обелиск: ${data.obelisk.ifBlank { "____________________" }}")
            appendLine("Приставка: ${data.annex.ifBlank { "____________________" }}")
            appendLine("Полка: ${data.shelf.ifBlank { "____________________" }}")
            appendLine("Тумба: ${data.tumba.ifBlank { "____________________" }}")
            appendLine("Цветник: ${data.cvetnik.ifBlank { "____________________" }}")
            appendLine("Плита: ${data.plita.ifBlank { "____________________" }}")
            appendLine("Отмостка верх: ${data.otmostkaTop.ifBlank { "____________________" }}")
            appendLine("Ваза: ${data.vase.ifBlank { "__________" }}  Лампада: ${data.lampada.ifBlank { "__________" }}")
            appendLine("Демонтаж: ${data.demontazh.ifBlank { "____________________" }}")
            appendLine("Вид плитки_ и размер: ${data.tileTypeAndSize.ifBlank { "________________" }}  бордюр: ${data.bordyur.ifBlank { "__________" }}")
            appendLine("")
            appendLine("ЗАКАЗЧИК с оформлением и комплектацией памятника.")
            appendLine("согласен ____________ Ф. И. О")
            appendLine("")
            appendLine("ИП Ступак О.О    Viber, Telegram    тел..+375295421809. +375297273637")
            appendLine("<<ДВА АНГЕЛА>> Ольшаны рынок БРОДОК павильон 35")
        }
    }

    private fun formatVal(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.isBlank()) "&nbsp;" else escapeXml(trimmed)
    }

    /**
     * HTML представление бланка для завода (без ценников).
     */
    fun generateFactoryHtml(data: FactoryOrderDocumentData): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Заказ на производство № ${escapeXml(data.orderNumber)}</title>
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 6mm 10mm 6mm 10mm;
                    }
                    * { box-sizing: border-box; }
                    body { font-family: "Times New Roman", Times, serif; font-size: 12px; color: #000; padding: 0; margin: 0; line-height: 1.3; }
                    .row { margin-bottom: 4px; display: flex; flex-wrap: wrap; align-items: baseline; }
                    .label { font-weight: normal; white-space: nowrap; }
                    .value { font-weight: bold; border-bottom: 1px solid #000; display: inline-block; padding: 0 4px; min-height: 16px; }
                    .divider { border-bottom: 2px solid #000; margin: 8px 0 6px 0; }
                    .section-title { font-weight: bold; text-align: center; margin: 6px 0 8px 0; font-size: 13px; letter-spacing: 0.5px; }
                    .blank-line { border-bottom: 1px solid #444; height: 16px; margin-bottom: 2px; }
                    .footer { margin-top: 12px; font-weight: bold; font-size: 11px; page-break-inside: avoid; }
                    @media print {
                        html, body {
                            width: 100%;
                            height: 100%;
                            overflow: hidden;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="row">
                    <span class="label">Дата</span> <span class="value" style="min-width: 90px;">${formatVal(data.date)}</span>.
                    <span class="label">&nbsp;&nbsp;Ф. И О заказчика.</span> <span class="value" style="min-width: 240px;">${formatVal(data.clientName)}</span>
                    <span class="label">&nbsp;&nbsp;Срок</span> <span class="value" style="min-width: 110px;">${formatVal(data.deadline)}</span>
                </div>

                <div class="row">
                    <span class="label">Телефон</span> <span class="value" style="min-width: 180px;">${formatVal(data.clientPhone)}</span>
                    <span class="label">&nbsp;&nbsp;адрес уст-ки</span> <span class="value" style="min-width: 280px;">${formatVal(data.installationAddress)}</span>
                </div>

                <div style="height: 6px;"></div>

                <div class="row">
                    <span class="label">Фамилия.ум.</span> <span class="value" style="min-width: 220px;">${formatVal(data.deceasedLastName)}</span>
                    <span class="label">&nbsp;&nbsp;Имя ум.</span> <span class="value" style="min-width: 220px;">${formatVal(data.deceasedFirstName)}</span>
                </div>

                <div class="row">
                    <span class="label">Отчество ум.</span> <span class="value" style="min-width: 280px;">${formatVal(data.deceasedMiddleName)}</span>
                </div>

                <div class="row">
                    <span class="label">Ч.М.Г.рождения</span> <span class="value" style="min-width: 170px;">${formatVal(data.birthDate)}</span>
                    <span class="label">&nbsp;&nbsp;Ч.М.Г.смерти</span> <span class="value" style="min-width: 170px;">${formatVal(data.deathDate)}</span>
                </div>

                <div class="row">
                    <span class="label">Крест</span> <span class="value" style="min-width: 110px;">${formatVal(data.cross)}</span>
                    <span class="label">&nbsp;&nbsp;виньетка(фото)</span> <span class="value" style="min-width: 150px;">${formatVal(data.vignettePhoto)}</span>
                    <span class="label">&nbsp;&nbsp;рамка</span> <span class="value" style="min-width: 110px;">${formatVal(data.frame)}</span>
                </div>

                <div class="row">
                    <span class="label">Эпитафия.</span> <span class="value" style="min-width: 520px;">${formatVal(data.epitaph)}</span>
                </div>
                <div class="blank-line"></div>

                <div class="row">
                    <span class="label">Оформление плиты</span> <span class="value" style="min-width: 500px;">${formatVal(data.plateDecoration)}</span>
                </div>
                <div class="blank-line"></div>

                <div class="row">
                    <span class="label">Дополнения</span> <span class="value" style="min-width: 510px;">${formatVal(data.additions)}</span>
                </div>
                <div class="blank-line"></div>

                <div class="divider"></div>

                <div class="section-title">. &nbsp; &nbsp; ФОРМА И РАЗМЕРЫ П-КА</div>

                <div class="row">
                    <span class="label">Материал</span> <span class="value" style="min-width: 480px;">${formatVal(data.material)}</span>
                </div>

                <div class="row">
                    <span class="label">Обелиск</span> <span class="value" style="min-width: 490px;">${formatVal(data.obelisk)}</span>
                </div>

                <div class="row">
                    <span class="label">Приставка</span> <span class="value" style="min-width: 480px;">${formatVal(data.annex)}</span>
                </div>

                <div class="row">
                    <span class="label">Полка</span> <span class="value" style="min-width: 505px;">${formatVal(data.shelf)}</span>
                </div>

                <div class="row">
                    <span class="label">Тумба</span> <span class="value" style="min-width: 505px;">${formatVal(data.tumba)}</span>
                </div>

                <div class="row">
                    <span class="label">Цветник</span> <span class="value" style="min-width: 490px;">${formatVal(data.cvetnik)}</span>
                </div>

                <div class="row">
                    <span class="label">Плита</span> <span class="value" style="min-width: 505px;">${formatVal(data.plita)}</span>
                </div>

                <div class="row">
                    <span class="label">Отмостка верх</span> <span class="value" style="min-width: 440px;">${formatVal(data.otmostkaTop)}</span>
                </div>

                <div class="row">
                    <span class="label">Ваза</span> <span class="value" style="min-width: 170px;">${formatVal(data.vase)}</span>
                    <span class="label">&nbsp;&nbsp;Лампада</span> <span class="value" style="min-width: 170px;">${formatVal(data.lampada)}</span>
                </div>

                <div class="row">
                    <span class="label">Демонтаж</span> <span class="value" style="min-width: 470px;">${formatVal(data.demontazh)}</span>
                </div>

                <div class="row">
                    <span class="label">Вид плитки_ и размер</span> <span class="value" style="min-width: 220px;">${formatVal(data.tileTypeAndSize)}</span>
                    <span class="label">&nbsp;&nbsp;бордюр</span> <span class="value" style="min-width: 130px;">${formatVal(data.bordyur)}</span>
                </div>

                <div class="footer">
                    <div style="font-weight: bold; margin-bottom: 2px;">ЗАКАЗЧИК с оформлением и комплектацией памятника.</div>
                    <div style="margin-bottom: 6px;">согласен ___________________________________ Ф. И. О</div>
                    <div style="font-size: 9pt; color: #333; margin-top: 8px; border-top: 1px dashed #aaa; padding-top: 4px;">
                        <strong>ИП Ступак О.О</strong> &nbsp;&nbsp;&nbsp;&nbsp; Viber, Telegram &nbsp;&nbsp; тел.: +375295421809, +375297273637<br>
                        &lt;&lt;ДВА АНГЕЛА&gt;&gt; Ольшаны рынок БРОДОК павильон 35
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Печать или экспорт в PDF бланка для завода через Android Print Manager.
     */
    fun printFactoryDocument(context: Context, data: FactoryOrderDocumentData) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Заказ_Завод_№${data.orderNumber}")
                val jobName = "Заказ Завод №${data.orderNumber} - ${data.clientName}"
                printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        val html = generateFactoryHtml(data)
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    /**
     * Построение Word XML (word/document.xml) для бланка завода в точности по предоставленной форме.
     */
    private fun buildFactoryDocumentXml(data: FactoryOrderDocumentData): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("<w:body>")

        // Line 1: Дата ... Ф. И О заказчика. ... Срок ...
        sb.append(makeFormLine(listOf(
            "Дата " to data.date.ifBlank { "__________" },
            ".   Ф. И О заказчика. " to data.clientName.ifBlank { "____________________" },
            "   Срок " to data.deadline.ifBlank { "__________" }
        ), spaceAfter = 120))

        // Line 2: Телефон ... адрес уст-ки ...
        sb.append(makeFormLine(listOf(
            "Телефон " to data.clientPhone.ifBlank { "_______________" },
            "   адрес уст-ки " to data.installationAddress.ifBlank { "____________________" }
        ), spaceAfter = 160))

        // Line 3: Фамилия.ум. ... Имя ум. ...
        sb.append(makeFormLine(listOf(
            "Фамилия.ум. " to data.deceasedLastName.ifBlank { "________________" },
            "   Имя ум. " to data.deceasedFirstName.ifBlank { "________________" }
        ), spaceAfter = 120))

        // Line 4: Отчество ум. ...
        sb.append(makeFormLine(listOf(
            "Отчество ум. " to data.deceasedMiddleName.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 5: Ч.М.Г.рождения ... Ч.М.Г.смерти ...
        sb.append(makeFormLine(listOf(
            "Ч.М.Г.рождения " to data.birthDate.ifBlank { "__________" },
            "   Ч.М.Г.смерти " to data.deathDate.ifBlank { "__________" }
        ), spaceAfter = 120))

        // Line 6: Крест ... виньетка(фото) ... рамка ...
        sb.append(makeFormLine(listOf(
            "Крест " to data.cross.ifBlank { "__________" },
            "   виньетка(фото) " to data.vignettePhoto.ifBlank { "__________" },
            "   рамка " to data.frame.ifBlank { "__________" }
        ), spaceAfter = 120))

        // Line 7: Эпитафия. ...
        sb.append(makeFormLine(listOf(
            "Эпитафия. " to data.epitaph.ifBlank { "________________________________________________" }
        ), spaceAfter = 80))
        sb.append(makeBlankLine())
        sb.append(makeBlankLine())

        // Line 10: Оформление плиты ...
        sb.append(makeParagraph("Оформление", sizeHalfPoints = 22, spaceAfter = 20))
        sb.append(makeFormLine(listOf(
            "плиты " to data.plateDecoration.ifBlank { "__________________________________________________" }
        ), spaceAfter = 80))
        sb.append(makeBlankLine())

        // Line 12: Дополнения ...
        sb.append(makeFormLine(listOf(
            "Дополнения " to data.additions.ifBlank { "_____________________________________________" }
        ), spaceAfter = 80))
        sb.append(makeBlankLine())

        // Horizontal separator line
        sb.append("""<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="12" w:space="4" w:color="000000"/></w:pBdr><w:spacing w:before="120" w:after="160"/></w:pPr></w:p>""")

        // Line 14: .   ФОРМА И РАЗМЕРЫ П-КА
        sb.append(makeParagraph(".   ФОРМА И РАЗМЕРЫ П-КА", align = "center", bold = true, sizeHalfPoints = 24, spaceBefore = 60, spaceAfter = 160))

        // Line 15: Материал ...
        sb.append(makeFormLine(listOf(
            "Материал " to data.material.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 16: Обелиск ...
        sb.append(makeFormLine(listOf(
            "Обелиск " to data.obelisk.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 17: Тумба ...
        sb.append(makeFormLine(listOf(
            "Тумба " to data.tumba.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 18: Цветник ...
        sb.append(makeFormLine(listOf(
            "Цветник " to data.cvetnik.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 19: Плита ...
        sb.append(makeFormLine(listOf(
            "Плита " to data.plita.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 20: Отмостка верх ...
        sb.append(makeFormLine(listOf(
            "Отмостка верх " to data.otmostkaTop.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 21: Ваза ... Лампада ...
        sb.append(makeFormLine(listOf(
            "Ваза " to data.vase.ifBlank { "__________" },
            "   Лампада " to data.lampada.ifBlank { "__________" }
        ), spaceAfter = 120))

        // Line 22: Демонтаж ...
        sb.append(makeFormLine(listOf(
            "Демонтаж " to data.demontazh.ifBlank { "____________________" }
        ), spaceAfter = 120))

        // Line 23: Вид плитки_ и размер ... бордюр ...
        sb.append(makeFormLine(listOf(
            "Вид плитки_ и размер " to data.tileTypeAndSize.ifBlank { "________________" },
            "   бордюр " to data.bordyur.ifBlank { "__________" }
        ), spaceAfter = 200))

        // Bottom signature and company info
        sb.append(makeParagraph("ЗАКАЗЧИК с оформлением и комплектацией памятника.", bold = true, sizeHalfPoints = 20, spaceBefore = 200, spaceAfter = 40))
        sb.append(makeParagraph("согласен ___________________________________ Ф. И. О", bold = false, sizeHalfPoints = 20, spaceBefore = 0, spaceAfter = 140))
        sb.append(makeParagraph("ИП Ступак О.О    Viber, Telegram    тел.: +375295421809. +375297273637", bold = true, sizeHalfPoints = 18, spaceBefore = 60, spaceAfter = 20))
        sb.append(makeParagraph("<<ДВА АНГЕЛА>> Ольшаны рынок БРОДОК павильон 35", bold = false, sizeHalfPoints = 18, spaceBefore = 0, spaceAfter = 60))

        // Page setup: A4 portrait (1cm margins)
        sb.append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="567" w:right="567" w:bottom="567" w:left="567"/></w:sectPr>""")
        sb.append("</w:body></w:document>")

        return sb.toString()
    }

    private fun makeFormLine(
        items: List<Pair<String, String>>,
        spaceBefore: Int = 0,
        spaceAfter: Int = 100,
        sizeHalfPoints: Int = 22
    ): String {
        val runs = StringBuilder()
        items.forEach { (label, value) ->
            val escLabel = escapeXml(label)
            val escValue = escapeXml(value)
            runs.append("""<w:r><w:rPr><w:sz w:val="$sizeHalfPoints"/><w:szCs w:val="$sizeHalfPoints"/></w:rPr><w:t xml:space="preserve">$escLabel</w:t></w:r>""")
            val isBlankUnderline = escValue.startsWith("____")
            runs.append("""<w:r><w:rPr><w:b/><w:u w:val="single"/><w:sz w:val="$sizeHalfPoints"/><w:szCs w:val="$sizeHalfPoints"/></w:rPr><w:t xml:space="preserve">${if (isBlankUnderline) escValue else " $escValue "}</w:t></w:r>""")
        }
        return """<w:p><w:pPr><w:spacing w:before="$spaceBefore" w:after="$spaceAfter"/></w:pPr>$runs</w:p>"""
    }

    private fun makeBlankLine(): String {
        return """<w:p><w:pPr><w:spacing w:before="0" w:after="80"/></w:pPr><w:r><w:rPr><w:u w:val="single"/><w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr><w:t xml:space="preserve">                                                                                                                                                            </w:t></w:r></w:p>"""
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
        val engravingTableRows = mutableListOf(
            listOf("Ф.И.О. усопшего:", data.deceasedName.ifBlank { "_________________________________" })
        )
        if (data.epitaphText.isNotBlank()) {
            engravingTableRows.add(listOf("Эпитафия:", data.epitaphText))
        }
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

        // Page properties (A4 with 1cm margins)
        sb.append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="567" w:right="567" w:bottom="567" w:left="567"/></w:sectPr>""")
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

