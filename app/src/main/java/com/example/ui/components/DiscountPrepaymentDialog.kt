package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.PriceFormatter

@Composable
fun DiscountPrepaymentDialog(
    initialDiscountPercent: Double,
    initialPrepayment: Double,
    subtotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (discountPercent: Double, prepayment: Double) -> Unit
) {
    var discountPercentStr by remember {
        mutableStateOf(if (initialDiscountPercent > 0) initialDiscountPercent.toString() else "")
    }
    var prepaymentStr by remember {
        mutableStateOf(if (initialPrepayment > 0) initialPrepayment.toString() else "")
    }

    val discount = discountPercentStr.toDoubleOrNull() ?: 0.0
    val discountAmount = subtotal * (discount / 100.0)
    val total = (subtotal - discountAmount).coerceAtLeast(0.0)
    val prepayment = prepaymentStr.toDoubleOrNull() ?: 0.0
    val remaining = (total - prepayment).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Скидка и предоплата", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Сумма по смете без скидки: ${PriceFormatter.formatRub(subtotal)}", fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = discountPercentStr,
                    onValueChange = { discountPercentStr = it },
                    label = { Text("Процент скидки (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("discount_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 5, 10, 15).forEach { pct ->
                        FilterChip(
                            selected = discount == pct.toDouble(),
                            onClick = { discountPercentStr = pct.toString() },
                            label = { Text("$pct%") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = prepaymentStr,
                    onValueChange = { prepaymentStr = it },
                    label = { Text("Внесенная предоплата (BYN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("prepayment_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30, 50, 70).forEach { pct ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val amount = (total * (pct / 100.0)).toInt()
                                prepaymentStr = amount.toString()
                            },
                            label = { Text("$pct%") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (discount > 0) {
                        Text("Скидка: -${PriceFormatter.formatRub(discountAmount)}", color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Итого к оплате: ${PriceFormatter.formatRub(total)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (prepayment > 0) {
                        Text("Остаток к доплате: ${PriceFormatter.formatRub(remaining)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        (discountPercentStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                        (prepaymentStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_discount_prepayment_btn")
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
