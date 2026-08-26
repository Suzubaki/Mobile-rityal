package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.OrderJsonAdapter
import com.example.data.PriceFormatter
import com.example.data.SavedOrder
import com.example.ui.RitualViewModel
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        onGenerateWord = { viewModel.generateAndShareDocxForOrder(context, order) },
                        onViewDetails = { orderToView = order },
                        onDelete = { orderToDelete = order }
                    )
                }
            }
        }
    }

    // View Order Details Modal Dialog
    orderToView?.let { order ->
        val items = remember(order) { OrderJsonAdapter.fromJson(order.itemsJson) }
        val dateStr = remember(order.createdAt) {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))
        }

        AlertDialog(
            onDismissRequest = { orderToView = null },
            title = {
                Column {
                    Text(order.orderNumber, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (order.clientName.isNotBlank()) Text("Заказчик: ${order.clientName}")
                    if (order.clientPhone.isNotBlank()) Text("Телефон: ${order.clientPhone}")
                    if (order.deceasedName.isNotBlank()) Text("Усопший: ${order.deceasedName}")
                    if (order.cemeteryName.isNotBlank()) Text("Кладбище: ${order.cemeteryName} ${if (order.plotNumber.isNotBlank()) "(уч. ${order.plotNumber})" else ""}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Состав сметы (${items.size} поз.):", fontWeight = FontWeight.Bold)
                    items.forEachIndexed { i, itm ->
                        Text(
                            text = "${i + 1}. ${itm.name} — ${PriceFormatter.formatNumber(itm.quantity)} ${itm.unit} × ${PriceFormatter.formatRub(itm.unitPrice)} = ${PriceFormatter.formatRub(itm.totalPrice)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Итого:")
                        Text(PriceFormatter.formatRub(order.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (order.prepaymentAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Остаток:")
                            Text(PriceFormatter.formatRub(order.remainingAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.generateAndShareDocxForOrder(context, order)
                        }
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сформировать Word")
                    }
                    Button(
                        onClick = {
                            viewModel.loadOrderIntoCalculator(order)
                            orderToView = null
                        }
                    ) {
                        Text("В калькулятор")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToView = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Удалить смету ${orderToDelete?.orderNumber}?") },
            text = { Text("Смета и все сохраненные данные будут удалены без возможности восстановления.") },
            confirmButton = {
                Button(
                    onClick = {
                        orderToDelete?.let { viewModel.deleteSavedOrder(it) }
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
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
    onGenerateWord: () -> Unit,
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
                Column {
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

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onGenerateWord,
                        modifier = Modifier.testTag("saved_order_word_btn_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Сформировать Word",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenInCalculator,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("В калькулятор")
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
