package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.RitualViewModel
import com.example.ui.components.AddEditPriceDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceSettingsScreen(
    viewModel: RitualViewModel,
    allPrices: List<PriceItem>,
    searchQuery: String,
    selectedCategory: String?
) {
    var mainTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Каталог", "Камни", "Замеры и гравировка")

    val stoneMaterials by viewModel.stoneMaterials.collectAsState()
    val sizePresets by viewModel.sizePresets.collectAsState()
    val constructorPrices by viewModel.constructorServicePrices.collectAsState()
    val usdRate by viewModel.usdExchangeRate.collectAsState()
    var usdRateInput by remember(usdRate) { mutableStateOf(usdRate.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // USD Exchange Rate Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Курс доллара (USD / BYN)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Курс Нацбанка РБ (авто-загрузка). Можно отредактировать вручную.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = { viewModel.refreshNbrbUsdRate(showUserFeedback = true) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Обновить курс с сайта Нацбанка РБ",
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = usdRateInput,
                    onValueChange = {
                        usdRateInput = it
                        it.toDoubleOrNull()?.let { rate ->
                            if (rate > 0) viewModel.updateUsdExchangeRate(rate)
                        }
                    },
                    label = { Text("Курс USD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(105.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top Navigation Tabs
        PrimaryTabRow(
            selectedTabIndex = mainTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = mainTab == index,
                    onClick = { mainTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (mainTab == index) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (mainTab) {
            0 -> GeneralCatalogTab(
                viewModel = viewModel,
                allPrices = allPrices,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                usdRate = usdRate
            )
            1 -> StoneMaterialsTab(
                viewModel = viewModel,
                materials = stoneMaterials,
                usdRate = usdRate
            )
            2 -> MeasurementPricingTab(
                viewModel = viewModel,
                allPrices = allPrices,
                servicePrices = constructorPrices,
                usdRate = usdRate
            )
        }
    }
}

// =========================================================================
// TAB 1: GENERAL CATALOG TAB
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralCatalogTab(
    viewModel: RitualViewModel,
    allPrices: List<PriceItem>,
    searchQuery: String,
    selectedCategory: String?,
    usdRate: Double
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PriceItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PriceItem?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    // Quick inline price editor
    var quickEditItem by remember { mutableStateOf<PriceItem?>(null) }
    var quickEditCostStr by remember { mutableStateOf("") }

    val categories = remember { ItemCategory.values().map { it.displayName } }

    val filteredPrices = remember(allPrices, selectedCategory, searchQuery) {
        allPrices.filter { item ->
            val matchCat = selectedCategory == null || item.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true) ||
                    item.subcategory.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar & Reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setPriceSearchQuery(it) },
                placeholder = { Text("Поиск по расценкам...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setPriceSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("price_search_input")
            )

            FilledTonalIconButton(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.testTag("reset_prices_btn")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Сброс к базовым ценам")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.setPriceCategory(null) },
                    label = { Text("Все (${allPrices.size})") }
                )
            }
            items(categories) { cat ->
                val countInCat = remember(allPrices) { allPrices.count { it.category == cat } }
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        viewModel.setPriceCategory(if (selectedCategory == cat) null else cat)
                    },
                    label = { Text("$cat ($countInCat)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top info bar with Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Найдено позиций: ${filteredPrices.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_new_price_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить позицию", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (filteredPrices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Позиции не найдены",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredPrices, key = { it.id }) { item ->
                    PriceItemManagementCard(
                        item = item,
                        usdRate = usdRate,
                        onQuickPriceEdit = {
                            quickEditItem = item
                            quickEditCostStr = item.currentPrice.toString()
                        },
                        onEditFull = { itemToEdit = item },
                        onToggleEnabled = { viewModel.togglePriceItemEnabled(item) },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddEditPriceDialog(
            itemToEdit = null,
            usdRate = usdRate,
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                viewModel.savePriceItem(newItem)
                showAddDialog = false
            }
        )
    }

    itemToEdit?.let { item ->
        AddEditPriceDialog(
            itemToEdit = item,
            usdRate = usdRate,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                viewModel.savePriceItem(updatedItem)
                itemToEdit = null
            }
        )
    }

    quickEditItem?.let { item ->
        var editByn by remember { mutableStateOf(if (item.currentPrice % 1.0 == 0.0) item.currentPrice.toLong().toString() else "%.2f".format(java.util.Locale.US, item.currentPrice)) }
        var editUsd by remember {
            val usd = if (usdRate > 0) item.currentPrice / usdRate else 0.0
            mutableStateOf(if (usd % 1.0 == 0.0) usd.toLong().toString() else "%.2f".format(java.util.Locale.US, usd))
        }

        AlertDialog(
            onDismissRequest = { quickEditItem = null },
            title = { Text("Быстрое изменение цены") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editByn,
                            onValueChange = { input ->
                                editByn = input
                                val byn = input.toDoubleOrNull()
                                if (byn != null && usdRate > 0) {
                                    val usd = byn / usdRate
                                    editUsd = if (usd % 1.0 == 0.0) usd.toLong().toString() else "%.2f".format(java.util.Locale.US, usd)
                                } else if (input.isBlank()) {
                                    editUsd = ""
                                }
                            },
                            label = { Text("Цена (${item.unit}) BYN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.1f)
                        )

                        OutlinedTextField(
                            value = editUsd,
                            onValueChange = { input ->
                                editUsd = input
                                val usd = input.toDoubleOrNull()
                                if (usd != null && usdRate > 0) {
                                    val byn = usd * usdRate
                                    editByn = if (byn % 1.0 == 0.0) byn.toLong().toString() else "%.2f".format(java.util.Locale.US, byn)
                                } else if (input.isBlank()) {
                                    editByn = ""
                                }
                            },
                            label = { Text("Цена ($ USD)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Text(
                        text = "1$ = $usdRate BYN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPrice = editByn.toDoubleOrNull()
                        if (newPrice != null && newPrice >= 0) {
                            viewModel.updatePriceItemCost(item.id, newPrice)
                            quickEditItem = null
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { quickEditItem = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Удалить позицию?") },
            text = { Text("Вы действительно хотите удалить «${item.name}» из прайс-листа?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePriceItem(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Сброс прайс-листа") },
            text = { Text("Сбросить все расценки каталога к базовым заводским значениям в BYN?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPricesToDefault()
                        showResetConfirmation = false
                    }
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun PriceItemManagementCard(
    item: PriceItem,
    usdRate: Double,
    onQuickPriceEdit: () -> Unit,
    onEditFull: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("price_item_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Category tag & Status + Quick action icons (Edit, Toggle, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.subcategory.ifBlank { item.category },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!item.isEnabled) {
                        Surface(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Отключено",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Action buttons right at top - ALWAYS clearly visible & never squished!
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditFull,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Редактировать позицию",
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (item.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (item.isEnabled) "Скрыть" else "Показать",
                            modifier = Modifier.size(17.dp),
                            tint = if (item.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_price_item_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Удалить позицию",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Name & Description
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Price bar at the bottom with quick edit
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickPriceEdit() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Default.PriceChange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Цена (за ${item.unit}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = PriceFormatter.formatWithUsd(item.currentPrice, usdRate),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Быстро изменить цену",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: STONE MATERIALS MANAGEMENT
// =========================================================================

@Composable
private fun StoneMaterialsTab(
    viewModel: RitualViewModel,
    materials: List<StoneMaterialItem>,
    usdRate: Double
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var materialToEdit by remember { mutableStateOf<StoneMaterialItem?>(null) }
    var materialToDelete by remember { mutableStateOf<StoneMaterialItem?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Породы камня для конструктора",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Стоимость за 1 м³ для точного расчёта стелы, тумбы и цветника",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = { showResetConfirmation = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Сброс к базовым материалам")
                }
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_stone_material_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Камень", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (materials.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.Terrain,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Список пород камня пуст",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Добавьте новые виды гранита и мрамора или восстановите стандартный набор",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Создать породу камня")
                        }
                        OutlinedButton(onClick = { viewModel.resetStoneMaterials() }) {
                            Text("Восстановить базовые")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(materials, key = { it.id }) { mat ->
                    StoneMaterialCard(
                        material = mat,
                        usdRate = usdRate,
                        onEdit = { materialToEdit = mat },
                        onToggleEnabled = { viewModel.toggleStoneMaterialEnabled(mat) },
                        onDelete = { materialToDelete = mat }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditStoneMaterialDialog(
            initial = null,
            usdRate = usdRate,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveStoneMaterial(it)
                showAddDialog = false
            }
        )
    }

    materialToEdit?.let { mat ->
        AddEditStoneMaterialDialog(
            initial = mat,
            usdRate = usdRate,
            onDismiss = { materialToEdit = null },
            onSave = {
                viewModel.saveStoneMaterial(it)
                materialToEdit = null
            }
        )
    }

    materialToDelete?.let { mat ->
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            title = { Text("Удалить материал?") },
            text = { Text("Удалить породу камня «${mat.name}» из конструктора?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStoneMaterial(mat)
                        materialToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Сброс материалов") },
            text = { Text("Восстановить заводской список пород гранита и мрамора?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetStoneMaterials()
                        showResetConfirmation = false
                    }
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun StoneMaterialCard(
    material: StoneMaterialItem,
    usdRate: Double,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    val priceText = "${PriceFormatter.formatWithUsd(material.pricePerM3, usdRate)} / м³"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (material.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("stone_card_${material.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Stone Name, Origin, and Price Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = material.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (material.isCustom) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Свой",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${material.colorName} • ${material.origin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            if (material.description.isNotEmpty()) {
                Text(
                    text = material.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Engraving suitability badge on its own row so it NEVER crowds buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    if (material.suitableForDirectEngraving) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (material.suitableForDirectEngraving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (material.suitableForDirectEngraving) "Прямая гравировка портрета: ДА" else "Портрет: на медальоне / стекле",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (material.suitableForDirectEngraving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // DEDICATED ACTION BAR (ALWAYS 100% VISIBLE ON EVERY SCREEN)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visibility toggle chip/button
                OutlinedButton(
                    onClick = onToggleEnabled,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        if (material.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (material.isEnabled) "Вкл" else "Выкл",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Edit button
                    FilledTonalButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Изменить", style = MaterialTheme.typography.labelSmall)
                    }

                    // Delete button (PROMINENT RED, NEVER CUT OFF)
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).testTag("delete_stone_btn_${material.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditStoneMaterialDialog(
    initial: StoneMaterialItem?,
    usdRate: Double = 3.25,
    onDismiss: () -> Unit,
    onSave: (StoneMaterialItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var colorName by remember { mutableStateOf(initial?.colorName ?: "") }
    var origin by remember { mutableStateOf(initial?.origin ?: "") }

    val initialM3 = initial?.pricePerM3 ?: 14000.0
    var priceBynStr by remember {
        mutableStateOf(if (initialM3 % 1.0 == 0.0) initialM3.toLong().toString() else "%.2f".format(java.util.Locale.US, initialM3))
    }
    var priceUsdStr by remember {
        val usd = if (usdRate > 0) initialM3 / usdRate else 0.0
        mutableStateOf(if (usd % 1.0 == 0.0) usd.toLong().toString() else "%.2f".format(java.util.Locale.US, usd))
    }

    var suitableForDirectEngraving by remember { mutableStateOf(initial?.suitableForDirectEngraving ?: true) }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    // Popular stone templates for quick 1-tap fill
    val popularPresets = listOf(
        Triple("Карельский Габбро-Диабаз", "Черный глубокий", "Карелия, Россия"),
        Triple("Дымовский (Балтийский)", "Коричнево-бордовый", "Ленинградская обл., Россия"),
        Triple("Покостовский гранит", "Светло-серый крапчатый", "Житомирская обл., Украина"),
        Triple("Лезниковский гранит", "Насыщенный красный", "Украина"),
        Triple("Мансуровский гранит", "Бело-серый мелкозернистый", "Урал, Россия"),
        Triple("Лабрадорит Volga Blue", "Черный с синей иризацией", "Украина"),
        Triple("Балтик Грин (Baltic Green)", "Изумрудно-зеленый", "Финляндия"),
        Triple("Масловский гранит (Verde Oliva)", "Оливково-зеленый", "Украина"),
        Triple("Коелга мрамор", "Белый с серыми прожилками", "Урал, Россия"),
        Triple("Шокшинский кварцит", "Малиновый благородный", "Карелия, Россия")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Terrain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(if (initial == null) "Создание породы камня" else "Редактирование материала")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initial == null) {
                    Text(
                        text = "Быстрый выбор из популярных пород:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(popularPresets) { preset ->
                            SuggestionChip(
                                onClick = {
                                    name = preset.first
                                    colorName = preset.second
                                    origin = preset.third
                                    if (preset.first.contains("Габбро") || preset.first.contains("черный", ignoreCase = true)) {
                                        suitableForDirectEngraving = true
                                    } else {
                                        suitableForDirectEngraving = false
                                    }
                                },
                                label = { Text(preset.first, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название породы камня *") },
                    placeholder = { Text("например: Балтик Грин, Гранит") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colorName,
                        onValueChange = { colorName = it },
                        label = { Text("Цвет / Оттенок") },
                        placeholder = { Text("Черный, Серый...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Месторождение") },
                        placeholder = { Text("Карелия, Урал...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Базовая цена за 1 кубический метр (1 м³):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                        label = { Text("Цена 1 м³ (BYN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
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
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { suitableForDirectEngraving = !suitableForDirectEngraving }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = suitableForDirectEngraving,
                            onCheckedChange = { suitableForDirectEngraving = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Прямая гравировка портрета",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (suitableForDirectEngraving) "Подходит для нанесения портрета станком/лазером"
                                else "Требуется медальон, триплекс или керамогранитная вставка",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (зернистость, фактура)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val pM3 = priceBynStr.toDoubleOrNull() ?: 14000.0
                        val id = initial?.id ?: "stone_${System.currentTimeMillis()}"
                        onSave(
                            StoneMaterialItem(
                                id = id,
                                name = name.trim(),
                                colorName = colorName.trim().ifBlank { "Натуральный камень" },
                                pricePerM3 = pM3,
                                origin = origin.trim().ifBlank { "Карьер" },
                                description = description.trim(),
                                suitableForDirectEngraving = suitableForDirectEngraving,
                                isCustom = true,
                                isEnabled = initial?.isEnabled ?: true,
                                sortOrder = initial?.sortOrder ?: 99
                            )
                        )
                    }
                }
            ) {
                Text(if (initial == null) "Создать породу" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// =========================================================================
// TAB 3: MEASUREMENT & FIELD RATES TAB (ЗАМЕРЫ)
// =========================================================================

@Composable
private fun MeasurementPricingTab(
    viewModel: RitualViewModel,
    allPrices: List<PriceItem>,
    servicePrices: List<ConstructorServicePriceItem>,
    usdRate: Double
) {
    val fenceItems = remember(allPrices) {
        allPrices.filter { item ->
            item.category == ItemCategory.FENCES_GROUND.displayName && item.unit != "м²"
        }
    }

    val tileItems = remember(allPrices) {
        allPrices.filter { item ->
            item.category == ItemCategory.FENCES_GROUND.displayName && item.unit == "м²"
        }
    }

    val pricesMap = remember(servicePrices) {
        servicePrices.associate { it.key to it.price }
    }

    var showAddFenceDialog by remember { mutableStateOf(false) }
    var showAddTileDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PriceItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PriceItem?>(null) }

    var editingTariffKey by remember { mutableStateOf<String?>(null) }
    var editingTariffTitle by remember { mutableStateOf("") }
    var editingTariffUnit by remember { mutableStateOf("BYN") }
    var editingTariffPrice by remember { mutableDoubleStateOf(0.0) }
    var editingTariffDesc by remember { mutableStateOf("") }

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.SquareFoot,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Расценки замеров и гравировки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Типы оград, плитки, доставки и гравировка за 1 знак. Создавайте, редактируйте и удаляйте позиции — они синхронизируются с калькулятором замеров.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 1: Fences & Ground Perimeter
        CatalogItemsSectionCard(
            title = "1. Типы оград и цоколей (периметр)",
            icon = Icons.Default.Fence,
            description = "Формула калькулятора: (Длина + Ширина) × 2 × Цена за метр. Позиции подтягиваются в combobox замеров.",
            usdRate = usdRate,
            items = fenceItems,
            onAddClick = { showAddFenceDialog = true },
            onEditItem = { itemToEdit = it },
            onToggleItem = { viewModel.togglePriceItemEnabled(it) },
            onDeleteItem = { itemToDelete = it }
        )

        // Section 2: Tiling & Area
        CatalogItemsSectionCard(
            title = "2. Типы покрытий и плитки (площадь)",
            icon = Icons.Default.GridOn,
            description = "Формула калькулятора: Длина × Ширина × Цена за 1 м². Позиции подтягиваются в combobox замеров.",
            usdRate = usdRate,
            items = tileItems,
            onAddClick = { showAddTileDialog = true },
            onEditItem = { itemToEdit = it },
            onToggleItem = { viewModel.togglePriceItemEnabled(it) },
            onDeleteItem = { itemToDelete = it }
        )

        // Section 3: Engraving Letters & Epitaphs
        MeasurementSectionCard(
            title = "3. Гравировка надписей (Конструктор памятника)",
            icon = Icons.Default.EditNote,
            description = "Тарифы за 1 знак. Связаны с расчётом ФИО, сусального золота и эпитафии в Конструкторе памятника",
            usdRate = usdRate,
            items = listOf(
                MeasurementTariffDisplay(
                    key = "letter_standard",
                    title = "Гравировка 1 знака ФИО/дат (обычный шрифт)",
                    unit = "знак",
                    price = pricesMap["letter_standard"] ?: 1.60,
                    description = "Цена за 1 знак букв/цифр ФИО в Конструкторе памятника и Замерах"
                ),
                MeasurementTariffDisplay(
                    key = "letter_gold",
                    title = "Гравировка 1 знака с сусальным золотом",
                    unit = "знак",
                    price = pricesMap["letter_gold"] ?: 5.50,
                    description = "Цена за 1 знак в Конструкторе памятника при выборе тумблера «Сусальное золото»"
                ),
                MeasurementTariffDisplay(
                    key = "letter_epitaph",
                    title = "Гравировка 1 знака эпитафии",
                    unit = "знак",
                    price = pricesMap["letter_epitaph"] ?: 1.50,
                    description = "Цена за 1 знак эпитафии в Конструкторе памятника"
                )
            ),
            onEdit = { key, title, unit, price, desc ->
                editingTariffKey = key
                editingTariffTitle = title
                editingTariffUnit = unit
                editingTariffPrice = price
                editingTariffDesc = desc
            }
        )

        // Section 4: Delivery
        MeasurementSectionCard(
            title = "4. Доставка на кладбище",
            icon = Icons.Default.LocalShipping,
            description = "Формула: Базовый тариф + (Доп. км × Тариф за км)",
            usdRate = usdRate,
            items = listOf(
                MeasurementTariffDisplay(
                    key = "measure_delivery_base_price",
                    title = "Базовая доставка спецтранспортом",
                    unit = "рейс",
                    price = pricesMap["measure_delivery_base_price"] ?: 60.0,
                    description = "Фиксированная стоимость рейса с погрузкой/разгрузкой"
                ),
                MeasurementTariffDisplay(
                    key = "measure_delivery_base_km",
                    title = "Включенное расстояние в базовый тариф",
                    unit = "км",
                    price = pricesMap["measure_delivery_base_km"] ?: 15.0,
                    description = "Километров от базы/мастерской без доплаты"
                ),
                MeasurementTariffDisplay(
                    key = "measure_delivery_km_price",
                    title = "Доплата за каждый километр свыше нормы",
                    unit = "BYN / км",
                    price = pricesMap["measure_delivery_km_price"] ?: 1.50,
                    description = "Тариф за выезд за пределы включенного расстояния"
                )
            ),
            onEdit = { key, title, unit, price, desc ->
                editingTariffKey = key
                editingTariffTitle = title
                editingTariffUnit = unit
                editingTariffPrice = price
                editingTariffDesc = desc
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Reset Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сбросить расценки замеров к заводским")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Add Fence Dialog
    if (showAddFenceDialog) {
        AddEditPriceDialog(
            itemToEdit = PriceItem(
                category = ItemCategory.FENCES_GROUND.displayName,
                subcategory = "Металлические ограды",
                name = "",
                unit = "м.п.",
                defaultPrice = 50.0,
                currentPrice = 50.0,
                description = "Погонные метры ограды или цоколя",
                isCustom = true
            ),
            usdRate = usdRate,
            onDismiss = { showAddFenceDialog = false },
            onSave = { newItem ->
                viewModel.savePriceItem(newItem)
                showAddFenceDialog = false
            }
        )
    }

    // Add Tile Dialog
    if (showAddTileDialog) {
        AddEditPriceDialog(
            itemToEdit = PriceItem(
                category = ItemCategory.FENCES_GROUND.displayName,
                subcategory = "Благоустройство участка",
                name = "",
                unit = "м²",
                defaultPrice = 85.0,
                currentPrice = 85.0,
                description = "Квадратные метры плитки / благоустройства",
                isCustom = true
            ),
            usdRate = usdRate,
            onDismiss = { showAddTileDialog = false },
            onSave = { newItem ->
                viewModel.savePriceItem(newItem)
                showAddTileDialog = false
            }
        )
    }

    // Edit Item Dialog
    itemToEdit?.let { item ->
        AddEditPriceDialog(
            itemToEdit = item,
            usdRate = usdRate,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                viewModel.savePriceItem(updatedItem)
                itemToEdit = null
            },
            onDelete = { itemToDelete ->
                viewModel.deletePriceItem(itemToDelete)
                itemToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Удалить позицию замеров?") },
            text = { Text("Вы действительно хотите удалить «${item.name}» из списка замеров?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePriceItem(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Edit Tariff Dialog
    editingTariffKey?.let { key ->
        AddEditMeasurementTariffDialog(
            title = editingTariffTitle,
            unit = editingTariffUnit,
            initialPrice = editingTariffPrice,
            description = editingTariffDesc,
            usdRate = usdRate,
            onDismiss = { editingTariffKey = null },
            onSave = { newPrice ->
                viewModel.updateConstructorPriceByKey(key, newPrice)
                editingTariffKey = null
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить расценки замеров?") },
            text = { Text("Все тарифы оград, плитки, гравировки знаков и доставки будут возвращены к исходным значениям.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateConstructorPriceByKey("measure_fence_price_pm", 45.0)
                        viewModel.updateConstructorPriceByKey("measure_tile_price_sqm", 85.0)
                        viewModel.updateConstructorPriceByKey("letter_standard", 1.60)
                        viewModel.updateConstructorPriceByKey("letter_gold", 5.50)
                        viewModel.updateConstructorPriceByKey("letter_epitaph", 1.50)
                        viewModel.updateConstructorPriceByKey("measure_delivery_base_price", 60.0)
                        viewModel.updateConstructorPriceByKey("measure_delivery_base_km", 15.0)
                        viewModel.updateConstructorPriceByKey("measure_delivery_km_price", 1.50)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private data class MeasurementTariffDisplay(
    val key: String,
    val title: String,
    val unit: String,
    val price: Double,
    val description: String
)

@Composable
private fun CatalogItemsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    usdRate: Double,
    items: List<PriceItem>,
    onAddClick: () -> Unit,
    onEditItem: (PriceItem) -> Unit,
    onToggleItem: (PriceItem) -> Unit,
    onDeleteItem: (PriceItem) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = onAddClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить", style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (items.isEmpty()) {
                Text(
                    text = "Список пуст. Нажмите «Добавить» для создания новой позиции.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        PriceItemManagementCard(
                            item = item,
                            usdRate = usdRate,
                            onQuickPriceEdit = { onEditItem(item) },
                            onEditFull = { onEditItem(item) },
                            onToggleEnabled = { onToggleItem(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    usdRate: Double,
    items: List<MeasurementTariffDisplay>,
    onEdit: (key: String, title: String, unit: String, price: Double, desc: String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            items.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                onEdit(item.key, item.title, item.unit, item.price, item.description)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val formattedPrice = if (item.unit == "км") {
                                        "${item.price.toInt()} км"
                                    } else {
                                        "${PriceFormatter.formatRub(item.price)} / ${item.unit}"
                                    }
                                    Text(
                                        text = formattedPrice,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (item.unit != "км" && usdRate > 0) {
                                        Text(
                                            text = "(${"%.2f".format(java.util.Locale.US, item.price / usdRate)} $)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Изменить цену",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditMeasurementTariffDialog(
    title: String,
    unit: String,
    initialPrice: Double,
    description: String,
    usdRate: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var priceBynStr by remember {
        mutableStateOf(
            if (initialPrice % 1.0 == 0.0) initialPrice.toLong().toString()
            else "%.2f".format(java.util.Locale.US, initialPrice)
        )
    }
    var priceUsdStr by remember {
        val usd = if (usdRate > 0) initialPrice / usdRate else 0.0
        mutableStateOf(
            if (usd % 1.0 == 0.0) usd.toLong().toString()
            else "%.2f".format(java.util.Locale.US, usd)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Изменить расценку", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (unit == "км") {
                    OutlinedTextField(
                        value = priceBynStr,
                        onValueChange = { priceBynStr = it },
                        label = { Text("Значение ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            label = { Text("Цена BYN / $unit *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.1f)
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
                            label = { Text("Цена $ USD") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    Text(
                        text = "Текущий курс: 1$ = $usdRate BYN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceBynStr.toDoubleOrNull()
                    if (p != null && p >= 0) {
                        onSave(p)
                    }
                },
                enabled = priceBynStr.toDoubleOrNull() != null
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

