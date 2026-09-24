package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EstimateDocumentData
import com.example.data.FactoryOrderDocumentData
import com.example.data.PriceFormatter
import com.example.util.DocxGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimatePreviewDialog(
    data: EstimateDocumentData,
    factoryData: FactoryOrderDocumentData? = null,
    initialTab: Int = 0,
    onDismissRequest: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val resolvedFactoryData = remember(data, factoryData) {
        factoryData ?: FactoryOrderDocumentData.fromEstimateDocumentData(data)
    }
    var selectedDocTab by remember { mutableStateOf(initialTab) } // 0 = Договор клиенту, 1 = Бланк завода

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .widthIn(max = 880.dp)
                    .fillMaxHeight(0.94f)
                    .clip(RoundedCornerShape(16.dp)),
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        if (selectedDocTab == 0) "Договор-смета (Клиент)" else "Бланк заказа цеха (Завод)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (selectedDocTab == 0)
                                            "Смета № ${data.documentNumber} от ${data.date}"
                                        else
                                            "Заказ № ${resolvedFactoryData.orderNumber} (без ценников)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismissRequest) {
                                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        val text = if (selectedDocTab == 0) {
                                            DocxGenerator.generatePlainText(data)
                                        } else {
                                            DocxGenerator.generateFactoryPlainText(resolvedFactoryData)
                                        }
                                        val title = if (selectedDocTab == 0) "Смета №${data.documentNumber}" else "Заказ цеха №${resolvedFactoryData.orderNumber}"
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = ClipData.newPlainText(title, text)
                                        cm?.setPrimaryClip(clip)
                                        onShowMessage(if (selectedDocTab == 0) "Текст сметы скопирован 📋" else "Бланк для завода скопирован 📋")
                                    },
                                    modifier = Modifier.testTag("preview_copy_btn")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать текст")
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            if (selectedDocTab == 0) {
                                                DocxGenerator.printDocument(context, data)
                                            } else {
                                                DocxGenerator.printFactoryDocument(context, resolvedFactoryData)
                                            }
                                        } catch (e: Exception) {
                                            onShowMessage("Ошибка печати: ${e.message}")
                                        }
                                    },
                                    modifier = Modifier.testTag("preview_print_btn")
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Печать / PDF")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )

                        PrimaryTabRow(
                            selectedTabIndex = selectedDocTab,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Tab(
                                selected = selectedDocTab == 0,
                                onClick = { selectedDocTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Договор клиенту (со сметой)", fontWeight = if (selectedDocTab == 0) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                modifier = Modifier.testTag("tab_client_contract")
                            )
                            Tab(
                                selected = selectedDocTab == 1,
                                onClick = { selectedDocTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Бланк для завода (без цен)", fontWeight = if (selectedDocTab == 1) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                modifier = Modifier.testTag("tab_factory_order")
                            )
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedDocTab == 0) {
                                // Client Doc Actions - Primary is Print / PDF
                                Button(
                                    onClick = {
                                        try {
                                            DocxGenerator.printDocument(context, data)
                                        } catch (e: Exception) {
                                            onShowMessage("Ошибка печати: ${e.message}")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("preview_print_main_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Печать договора / Сохранить в PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val savedFile = DocxGenerator.saveDocxToPublicDownloads(context, data)
                                                onShowMessage("💾 Файл сохранен в Загрузки для ПК:\n${savedFile.name}")
                                            } catch (e: Exception) {
                                                onShowMessage("Ошибка сохранения: ${e.message}")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("preview_save_downloads_btn")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("В Загрузки (Word/PC)", maxLines = 1)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            try {
                                                val file = DocxGenerator.generateDocx(context, data)
                                                DocxGenerator.shareDocxFile(context, file)
                                            } catch (e: Exception) {
                                                onShowMessage("Ошибка отправки: ${e.message}")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("preview_share_btn")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Поделиться", maxLines = 1)
                                    }
                                }
                            } else {
                                // Factory Doc Actions
                                Button(
                                    onClick = {
                                        try {
                                            DocxGenerator.printFactoryDocument(context, resolvedFactoryData)
                                        } catch (e: Exception) {
                                            onShowMessage("Ошибка печати бланка: ${e.message}")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("preview_print_factory_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Печать бланка завода / PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val savedFile = DocxGenerator.saveFactoryDocxToPublicDownloads(context, resolvedFactoryData)
                                                onShowMessage("💾 Бланк завода сохранен в Загрузки для ПК:\n${savedFile.name}")
                                            } catch (e: Exception) {
                                                onShowMessage("Ошибка сохранения: ${e.message}")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("preview_save_factory_downloads_btn")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("В Загрузки (PC)", maxLines = 1)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            try {
                                                val file = DocxGenerator.generateFactoryDocx(context, resolvedFactoryData)
                                                DocxGenerator.shareDocxFile(context, file)
                                            } catch (e: Exception) {
                                                onShowMessage("Ошибка отправки: ${e.message}")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("preview_share_factory_btn")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Поделиться", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                val horizontalScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .verticalScroll(scrollState)
                        .horizontalScroll(horizontalScrollState),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .widthIn(min = 540.dp, max = 780.dp)
                    ) {
                        if (selectedDocTab == 0) {
                            // Client Document Sheet
                            ClientEstimateSheet(data = data)
                        } else {
                            // Factory Order Sheet (Без ценников)
                            FactoryOrderSheet(data = resolvedFactoryData)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientEstimateSheet(data: EstimateDocumentData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .widthIn(max = 760.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 1. Header & Organization Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        data.organizationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        data.organizationAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        data.organizationContacts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "№ ${data.documentNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Document Title
            Text(
                "СМЕТА-СПЕЦИФИКАЦИЯ К ЗАКАЗУ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "от ${data.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Client & Monument Information Table
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow(label = "Заказчик:", value = data.clientName.ifBlank { "—" })
                    if (data.clientPhone.isNotBlank()) {
                        DetailRow(label = "Телефон:", value = data.clientPhone)
                    }
                    if (data.deceasedName.isNotBlank()) {
                        DetailRow(label = "Усопший:", value = data.deceasedName)
                    }
                    if (data.installationAddress.isNotBlank()) {
                        DetailRow(label = "Место установки:", value = data.installationAddress)
                    }

                    // Monument dimensions if available
                    if (data.steleDimensions.isNotBlank() || data.plinthDimensions.isNotBlank() || data.flowerbedDimensions.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Параметры памятника:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (data.steleDimensions.isNotBlank()) {
                            DetailRow(label = "Стела:", value = data.steleDimensions)
                        }
                        if (data.plinthDimensions.isNotBlank()) {
                            DetailRow(label = "Тумба:", value = data.plinthDimensions)
                        }
                        if (data.flowerbedDimensions.isNotBlank()) {
                            DetailRow(label = "Цветник:", value = data.flowerbedDimensions)
                        }
                    }

                    if (data.epitaphText.isNotBlank()) {
                        DetailRow(label = "Эпитафия:", value = "\"${data.epitaphText}\"")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Specification Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("№", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Наименование товара / услуги", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Кол-во", modifier = Modifier.width(60.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Цена", modifier = Modifier.width(75.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Сумма", modifier = Modifier.width(85.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            // Items List
            data.items.forEachIndexed { index, item ->
                val bg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLowest
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${item.number}", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        if (item.customNote.isNotBlank() && item.customNote != item.name) {
                            Text(
                                item.customNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "${PriceFormatter.formatNumber(item.quantity)} ${item.unit}",
                        modifier = Modifier.width(60.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        PriceFormatter.formatRub(item.unitPrice),
                        modifier = Modifier.width(75.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        PriceFormatter.formatRub(item.totalPrice),
                        modifier = Modifier.width(85.dp),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Totals Block
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Сумма по смете:", style = MaterialTheme.typography.bodyMedium)
                        Text(PriceFormatter.formatRub(data.subtotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    if (data.discountPercent > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Скидка (${PriceFormatter.formatNumber(data.discountPercent)}%):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Text("-${PriceFormatter.formatRub(data.discountAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ИТОГО К ОПЛАТЕ:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(PriceFormatter.formatRub(data.grandTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    if (data.prepayment > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Внесенный аванс (предоплата):", style = MaterialTheme.typography.bodyMedium)
                            Text(PriceFormatter.formatRub(data.prepayment), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Остаток к доплате:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(PriceFormatter.formatRub(data.remainingAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // 5. Notes if available
            if (data.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Примечание к заказу:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(data.notes, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Signatures Area
            Text(
                "С оформлением, комплектацией и стоимостью заказа согласен.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ЗАКАЗЧИК:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        "(подпись / ${data.clientName.ifBlank { "Ф.И.О." }})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("ИСПОЛНИТЕЛЬ:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        "(подпись / М.П. ${data.organizationName})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Бланк заказа для завода (производства) БЕЗ ЦЕННИКОВ.
 * В точности воспроизводит структуру реального бланка из мастерской (с фото).
 */
@Composable
private fun FactoryOrderSheet(data: FactoryOrderDocumentData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .widthIn(max = 760.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Line 1: Дата ... Ф. И О заказчика. ... Срок ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Дата", value = data.date, minWidth = 80.dp)
                Text(".", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                FormFieldText(label = "Ф. И О заказчика.", value = data.clientName, minWidth = 180.dp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                FormFieldText(label = "Срок", value = data.deadline, minWidth = 90.dp)
            }

            // Line 2: Телефон ... адрес уст-ки ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Телефон", value = data.clientPhone, minWidth = 140.dp)
                Spacer(modifier = Modifier.width(12.dp))
                FormFieldText(label = "адрес уст-ки", value = data.installationAddress, minWidth = 200.dp, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Line 3: Фамилия.ум. ... Имя ум. ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Фамилия.ум.", value = data.deceasedLastName, minWidth = 150.dp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                FormFieldText(label = "Имя ум.", value = data.deceasedFirstName, minWidth = 150.dp, modifier = Modifier.weight(1f))
            }

            // Line 4: Отчество ум. ...
            FormFieldText(
                label = "Отчество ум.",
                value = data.deceasedMiddleName,
                minWidth = 260.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 5: Ч.М.Г.рождения ... Ч.М.Г.смерти ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Ч.М.Г.рождения", value = data.birthDate, minWidth = 120.dp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                FormFieldText(label = "Ч.М.Г.смерти", value = data.deathDate, minWidth = 120.dp, modifier = Modifier.weight(1f))
            }

            // Line 6: Крест ... виньетка(фото) ... рамка ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Крест", value = data.cross, minWidth = 80.dp, modifier = Modifier.weight(0.9f))
                Spacer(modifier = Modifier.width(8.dp))
                FormFieldText(label = "виньетка(фото)", value = data.vignettePhoto, minWidth = 110.dp, modifier = Modifier.weight(1.3f))
                Spacer(modifier = Modifier.width(8.dp))
                FormFieldText(label = "рамка", value = data.frame, minWidth = 80.dp, modifier = Modifier.weight(0.8f))
            }

            // Line 7: Эпитафия. ...
            FormFieldText(
                label = "Эпитафия.",
                value = data.epitaph,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )
            FormUnderlineRow()
            FormUnderlineRow()

            // Line 10: Оформление плиты ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Оформление", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                    Text("плиты", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            val y = size.height - strokeWidth / 2
                            drawLine(Color.Black.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        data.plateDecoration.ifBlank { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
            FormUnderlineRow()

            // Line 12: Дополнения ...
            FormFieldText(
                label = "Дополнения",
                value = data.additions,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )
            FormUnderlineRow()

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal separator line
            HorizontalDivider(thickness = 2.dp, color = Color.Black)

            Spacer(modifier = Modifier.height(4.dp))

            // Title: ФОРМА И РАЗМЕРЫ П-КА
            Text(
                ".                     ФОРМА И РАЗМЕРЫ П-КА",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Line 15: Материал ...
            FormFieldText(
                label = "Материал",
                value = data.material,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 16: Обелиск ...
            FormFieldText(
                label = "Обелиск",
                value = data.obelisk,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 16a: Приставка ...
            FormFieldText(
                label = "Приставка",
                value = data.annex,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 16b: Полка ...
            FormFieldText(
                label = "Полка",
                value = data.shelf,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 17: Тумба ...
            FormFieldText(
                label = "Тумба",
                value = data.tumba,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 18: Цветник ...
            FormFieldText(
                label = "Цветник",
                value = data.cvetnik,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 19: Плита ...
            FormFieldText(
                label = "Плита",
                value = data.plita,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 20: Отмостка верх ...
            FormFieldText(
                label = "Отмостка верх",
                value = data.otmostkaTop,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 21: Ваза ... Лампада ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Ваза", value = data.vase, minWidth = 110.dp, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                FormFieldText(label = "Лампада", value = data.lampada, minWidth = 110.dp, modifier = Modifier.weight(1f))
            }

            // Line 22: Демонтаж ...
            FormFieldText(
                label = "Демонтаж",
                value = data.demontazh,
                minWidth = 280.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Line 23: Вид плитки_ и размер ... бордюр ...
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                FormFieldText(label = "Вид плитки_ и размер", value = data.tileTypeAndSize, minWidth = 150.dp, modifier = Modifier.weight(1.3f))
                Spacer(modifier = Modifier.width(12.dp))
                FormFieldText(label = "бордюр", value = data.bordyur, minWidth = 90.dp, modifier = Modifier.weight(0.7f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom signature and company info
            Text(
                "ЗАКАЗЧИК с оформлением и комплектацией памятника.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                "согласен ___________________________________ Ф. И. О",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "ИП Ступак О.О    Viber, Telegram    тел.: +375295421809, +375297273637",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                "<<ДВА АНГЕЛА>> Ольшаны рынок БРОДОК павильон 35",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun FormFieldText(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    minWidth: Dp = 80.dp
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .widthIn(min = minWidth)
                .weight(1f, fill = false)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color.Black.copy(alpha = 0.65f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = value.ifBlank { " " },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun FormUnderlineRow(
    text: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
