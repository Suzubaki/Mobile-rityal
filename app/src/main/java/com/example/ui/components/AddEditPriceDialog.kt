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
import com.example.data.PriceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPriceDialog(
    itemToEdit: PriceItem?,
    usdRate: Double = 3.25,
    onDismiss: () -> Unit,
    onSave: (PriceItem) -> Unit,
    onDelete: ((PriceItem) -> Unit)? = null
) {
    val isEdit = itemToEdit != null
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: ItemCategory.MONUMENTS.displayName) }
    var subcategory by remember { mutableStateOf(itemToEdit?.subcategory ?: "") }
    var unit by remember { mutableStateOf(itemToEdit?.unit ?: "шт") }

    val initialByn = itemToEdit?.currentPrice
    var priceBynStr by remember {
        mutableStateOf(
            initialByn?.let { if (it % 1.0 == 0.0) it.toLong().toString() else "%.2f".format(java.util.Locale.US, it) } ?: ""
        )
    }
    var priceUsdStr by remember {
        mutableStateOf(
            initialByn?.let {
                val usd = if (usdRate > 0) it / usdRate else 0.0
                if (usd % 1.0 == 0.0) usd.toLong().toString() else "%.2f".format(java.util.Locale.US, usd)
            } ?: ""
        )
    }

    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = remember { ItemCategory.values().map { it.displayName } }
    val commonUnits = remember { listOf("шт", "компл", "м.п.", "м²", "знак", "рейс", "услуга") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Редактировать расценку" else "Добавить в прайс-лист",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название позиции / услуги *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("price_dialog_name")
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

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Подкатегория / материал (опционально)") },
                    placeholder = { Text("Например: Гранит Габбро-Диабаз") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Currency Dual Input: BYN (primary) and USD ($)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceBynStr,
                        onValueChange = { input ->
                            priceBynStr = input
                            val byn = input.toDoubleOrNull()
                            if (byn != null && usdRate > 0) {
                                val usd = byn / usdRate
                                priceUsdStr = if (usd % 1.0 == 0.0) usd.toLong().toString() else "%.2f".format(java.util.Locale.US, usd)
                            } else if (input.isBlank()) {
                                priceUsdStr = ""
                            }
                        },
                        label = { Text("Цена (BYN) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.1f).testTag("price_dialog_cost")
                    )

                    OutlinedTextField(
                        value = priceUsdStr,
                        onValueChange = { input ->
                            priceUsdStr = input
                            val usd = input.toDoubleOrNull()
                            if (usd != null && usdRate > 0) {
                                val byn = usd * usdRate
                                priceBynStr = if (byn % 1.0 == 0.0) byn.toLong().toString() else "%.2f".format(java.util.Locale.US, byn)
                            } else if (input.isBlank()) {
                                priceBynStr = ""
                            }
                        },
                        label = { Text("Цена ($ USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Ед. изм. *") },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Курс: 1$ = $usdRate BYN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commonUnits.take(4).forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание / комплектация") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceBynStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && price >= 0) {
                        val newItem = itemToEdit?.copy(
                            name = name.trim(),
                            category = category,
                            subcategory = subcategory.trim(),
                            unit = unit.trim(),
                            currentPrice = price,
                            description = description.trim()
                        ) ?: PriceItem(
                            id = 0,
                            category = category,
                            subcategory = subcategory.trim(),
                            name = name.trim(),
                            unit = unit.trim(),
                            defaultPrice = price,
                            currentPrice = price,
                            description = description.trim(),
                            isCustom = true,
                            isEnabled = true
                        )
                        onSave(newItem)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank() && priceBynStr.isNotBlank(),
                modifier = Modifier.testTag("save_price_dialog_btn")
            ) {
                Text(if (isEdit) "Сохранить" else "Добавить")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEdit && onDelete != null && itemToEdit != null) {
                    TextButton(
                        onClick = {
                            onDelete(itemToEdit)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}
