package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ItemCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomItemDialog(
    usdRate: Double = 3.25,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, unit: String, unitPrice: Double, quantity: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ItemCategory.MONUMENTS.displayName) }
    var unit by remember { mutableStateOf("шт") }
    var unitPriceBynStr by remember { mutableStateOf("") }
    var unitPriceUsdStr by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = remember { ItemCategory.values().map { it.displayName } }
    val units = remember { listOf("шт", "компл", "м.п.", "м²", "знак", "рейс", "услуга", "час") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить произвольную позицию", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Наименование товара или работы *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_item_name_input")
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitPriceBynStr,
                        onValueChange = { input ->
                            unitPriceBynStr = input
                            val byn = input.toDoubleOrNull()
                            if (byn != null && usdRate > 0) {
                                val usd = byn / usdRate
                                unitPriceUsdStr = if (usd % 1.0 == 0.0) usd.toLong().toString() else "%.2f".format(java.util.Locale.US, usd)
                            } else if (input.isBlank()) {
                                unitPriceUsdStr = ""
                            }
                        },
                        label = { Text("Цена (BYN) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.1f).testTag("custom_item_price_input")
                    )

                    OutlinedTextField(
                        value = unitPriceUsdStr,
                        onValueChange = { input ->
                            unitPriceUsdStr = input
                            val usd = input.toDoubleOrNull()
                            if (usd != null && usdRate > 0) {
                                val byn = usd * usdRate
                                unitPriceBynStr = if (byn % 1.0 == 0.0) byn.toLong().toString() else "%.2f".format(java.util.Locale.US, byn)
                            } else if (input.isBlank()) {
                                unitPriceBynStr = ""
                            }
                        },
                        label = { Text("Цена ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Кол-во *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("custom_item_qty_input")
                    )
                    Text(
                        text = "1$ = $usdRate BYN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text("Единица измерения:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    units.take(4).forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = unitPriceBynStr.toDoubleOrNull() ?: 0.0
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank() && price >= 0) {
                        onConfirm(name.trim(), category, unit, price, qty)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank() && unitPriceBynStr.isNotBlank(),
                modifier = Modifier.testTag("submit_custom_item_btn")
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
