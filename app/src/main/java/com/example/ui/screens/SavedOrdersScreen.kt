package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EstimateDocumentData
import com.example.data.FactoryOrderDocumentData
import com.example.data.OrderJsonAdapter
import com.example.data.PriceFormatter
import com.example.data.SavedOrder
import com.example.ui.RitualViewModel
import com.example.ui.components.EstimatePreviewDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SavedOrdersScreen(
    viewModel: RitualViewModel,
    orders: List<SavedOrder>
) {
    val context = LocalContext.current
    var orderToDelete by remember { mutableStateOf<SavedOrder?>(null) }
    var orderToView by remember { mutableStateOf<SavedOrder?>(null) }
    var orderForPreview by remember { mutableStateOf<SavedOrder?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "История сохраненных расчетов (${orders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Нет сохраненных смет",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Создайте расчет во вкладке «Калькулятор» и нажмите «Сохранить», чтобы зафиксировать смету или заказ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    SavedOrderCard(
                        order = order,
                        onOpenInCalculator = { viewModel.loadOrderIntoCalculator(order) },
                        onPreview = { orderForPreview = order },
                        onViewDetails = { orderToView = order },
                        onDelete = { orderToDelete = order }
                    )
                }
            }
        }
    }

    // Full Document Preview Dialog for Saved Order
    orderForPreview?.let { order ->
        val items = remember(order) { OrderJsonAdapter.fromJson(order.itemsJson) }
        val docData = remember(order, items) {
            EstimateDocumentData.fromSavedOrder(order, items)
        }
        val factoryData = remember(order, items) {
            FactoryOrderDocumentData.fromSavedOrder(order, items)
        }
        EstimatePreviewDialog(
            data = docData,
            factoryData = factoryData,
            onDismissRequest = { orderForPreview = null },
            onShowMessage = { msg -> viewModel.showMessage(msg) }
        )
    }

    // View Order Details Modal Dialog
    orderToView?.let { order ->
        val items = remember(order) { OrderJsonAdapter.fromJson(order.itemsJson) }
        val dateStr = remember(order.createdAt) {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))
        }

        Dialog(
            onDismissRequest = { orderToView = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = order.orderNumber,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatusBadge(status = order.status)
                            }
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { orderToView = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Client information card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (order.clientName.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Заказчик: ${order.clientName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (order.clientPhone.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Телефон: ${order.clientPhone}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (order.deceasedName.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("В память: ${order.deceasedName}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (order.cemeteryName.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Кладбище: ${order.cemeteryName} ${if (order.plotNumber.isNotBlank()) "(уч. ${order.plotNumber})" else ""}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // Order Items List
                        Text("Состав сметы (${items.size} поз.):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items.forEachIndexed { i, itm ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${i + 1}. ${itm.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = PriceFormatter.formatRub(itm.totalPrice),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${PriceFormatter.formatNumber(itm.quantity)} ${itm.unit} × ${PriceFormatter.formatRub(itm.unitPrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (i < items.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }

                        // Summary Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Подытог:", style = MaterialTheme.typography.bodySmall)
                                    Text(PriceFormatter.formatRub(order.subtotalAmount), style = MaterialTheme.typography.bodySmall)
                                }
                                if (order.discountPercent > 0 || order.discountAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Скидка (${order.discountPercent.toInt()}%):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        Text("-${PriceFormatter.formatRub(order.discountAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("ИТОГО К ОПЛАТЕ:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(PriceFormatter.formatRub(order.totalAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                if (order.prepaymentAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Предоплата:", style = MaterialTheme.typography.bodySmall)
                                        Text(PriceFormatter.formatRub(order.prepaymentAmount), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Остаток к доплате:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                        Text(PriceFormatter.formatRub(order.remainingAmount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        if (order.notes.isNotBlank()) {
                            Text("Примечания: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Bottom Action Controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Main Action: Load into calculator
                        Button(
                            onClick = {
                                viewModel.loadOrderIntoCalculator(order)
                                orderToView = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("В калькулятор", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        // Secondary actions: Preview + Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val target = order
                                    orderToView = null
                                    orderForPreview = target
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Договор / Печать", maxLines = 1, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val target = order
                                    orderToView = null
                                    orderToDelete = target
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Удалить", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            },
            title = { Text("Удалить смету ${orderToDelete?.orderNumber}?") },
            text = { Text("Смета и все сохраненные позиции будут удалены из приложения и облака Firestore.") },
            confirmButton = {
                Button(
                    onClick = {
                        orderToDelete?.let { viewModel.deleteSavedOrder(it) }
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить смету")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SavedOrderCard(
    order: SavedOrder,
    onOpenInCalculator: () -> Unit,
    onPreview: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("saved_order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (order.clientName.isNotBlank()) {
                Text(
                    text = "Заказчик: ${order.clientName} ${if (order.clientPhone.isNotBlank()) "(${order.clientPhone})" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (order.deceasedName.isNotBlank()) {
                Text(
                    text = "В память: ${order.deceasedName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сумма: ${PriceFormatter.formatRub(order.totalAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (order.prepaymentAmount > 0) {
                        Text(
                            text = "Остаток: ${PriceFormatter.formatRub(order.remainingAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPreview,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("saved_order_preview_btn_${order.id}")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Договор")
                    }

                    FilledTonalButton(
                        onClick = onOpenInCalculator,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, bgColor, textColor) = when (status) {
        "IN_PROGRESS" -> Triple("В работе", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        "PAID" -> Triple("Оплачен", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        "READY" -> Triple("Готов", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> Triple("Черновик", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
