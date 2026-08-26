package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.AppScreen
import com.example.ui.RitualViewModel
import kotlinx.coroutines.flow.collectLatest

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            AppScreen.CALCULATOR -> "Ритуальный калькулятор"
                            AppScreen.PRICE_SETTINGS -> "Прайс-лист и расценки"
                            AppScreen.SAVED_ORDERS -> "Сохраненные сметы"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
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
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            }
        }
    }
}
