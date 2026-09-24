package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AppScreen
import com.example.ui.RitualViewModel
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: RitualViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val calcState by viewModel.calcState.collectAsState()
    val allPrices by viewModel.allPrices.collectAsState()
    val activePrices by viewModel.activePrices.collectAsState()
    val savedOrders by viewModel.savedOrders.collectAsState()
    val priceSearchQuery by viewModel.priceSearchQuery.collectAsState()
    val selectedPriceCategory by viewModel.selectedPriceCategory.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val isOnline by viewModel.isOnline.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    header = {
                        Spacer(modifier = Modifier.padding(top = 12.dp))
                    }
                ) {
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.CALCULATOR,
                        onClick = { viewModel.setScreen(AppScreen.CALCULATOR) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (calcState.selectedItems.isNotEmpty()) {
                                        Badge { Text(calcState.selectedItems.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = "Калькулятор")
                            }
                        },
                        label = { Text("Калькулятор") },
                        modifier = Modifier.testTag("nav_calculator")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.PRICE_SETTINGS,
                        onClick = { viewModel.setScreen(AppScreen.PRICE_SETTINGS) },
                        icon = {
                            Icon(Icons.Default.PriceChange, contentDescription = "Прайс-лист")
                        },
                        label = { Text("Расценки") },
                        modifier = Modifier.testTag("nav_price_settings")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.SAVED_ORDERS,
                        onClick = { viewModel.setScreen(AppScreen.SAVED_ORDERS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (savedOrders.isNotEmpty()) {
                                        Badge { Text(savedOrders.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Сметы")
                            }
                        },
                        label = { Text("Сметы") },
                        modifier = Modifier.testTag("nav_saved_orders")
                    )

                    NavigationRailItem(
                        selected = currentScreen == AppScreen.CLOUD_SETTINGS,
                        onClick = { viewModel.setScreen(AppScreen.CLOUD_SETTINGS) },
                        icon = {
                            Icon(Icons.Default.CloudSync, contentDescription = "Облако и курс")
                        },
                        label = { Text("Облако") },
                        modifier = Modifier.testTag("nav_cloud_settings")
                    )
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentScreen) {
                                    AppScreen.CALCULATOR -> "Ритуальный калькулятор"
                                    AppScreen.PRICE_SETTINGS -> "Прайс-лист и расценки"
                                    AppScreen.SAVED_ORDERS -> "Сохраненные сметы"
                                    AppScreen.CLOUD_SETTINGS -> "Облачная база и курс"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            AssistChip(
                                onClick = { viewModel.setScreen(AppScreen.CLOUD_SETTINGS) },
                                label = {
                                    Text(
                                        if (isOnline) "Онлайн ☁️" else "Офлайн 📱",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isOnline)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.CALCULATOR,
                                onClick = { viewModel.setScreen(AppScreen.CALCULATOR) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (calcState.selectedItems.isNotEmpty()) {
                                                Badge { Text(calcState.selectedItems.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Calculate, contentDescription = "Калькулятор")
                                    }
                                },
                                label = { Text("Калькулятор") },
                                modifier = Modifier.testTag("nav_calculator")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PRICE_SETTINGS,
                                onClick = { viewModel.setScreen(AppScreen.PRICE_SETTINGS) },
                                icon = {
                                    Icon(Icons.Default.PriceChange, contentDescription = "Прайс-лист")
                                },
                                label = { Text("Расценки") },
                                modifier = Modifier.testTag("nav_price_settings")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SAVED_ORDERS,
                                onClick = { viewModel.setScreen(AppScreen.SAVED_ORDERS) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (savedOrders.isNotEmpty()) {
                                                Badge { Text(savedOrders.size.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = "Сметы")
                                    }
                                },
                                label = { Text("Сметы") },
                                modifier = Modifier.testTag("nav_saved_orders")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.CLOUD_SETTINGS,
                                onClick = { viewModel.setScreen(AppScreen.CLOUD_SETTINGS) },
                                icon = {
                                    Icon(Icons.Default.CloudSync, contentDescription = "Облако и курс")
                                },
                                label = { Text("Облако") },
                                modifier = Modifier.testTag("nav_cloud_settings")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1200.dp)
                    ) {
                        when (currentScreen) {
                            AppScreen.CALCULATOR -> {
                                CalculatorScreen(
                                    viewModel = viewModel,
                                    calcState = calcState,
                                    activePrices = activePrices
                                )
                            }
                            AppScreen.PRICE_SETTINGS -> {
                                PriceSettingsScreen(
                                    viewModel = viewModel,
                                    allPrices = allPrices,
                                    searchQuery = priceSearchQuery,
                                    selectedCategory = selectedPriceCategory
                                )
                            }
                            AppScreen.SAVED_ORDERS -> {
                                SavedOrdersScreen(
                                    viewModel = viewModel,
                                    orders = savedOrders
                                )
                            }
                            AppScreen.CLOUD_SETTINGS -> {
                                CloudSyncSettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
