package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.cloud.SyncState
import com.example.ui.RitualViewModel
import com.example.ui.components.AuthDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncSettingsScreen(viewModel: RitualViewModel) {
    val syncState by viewModel.syncState.collectAsState()
    val usdRate by viewModel.usdExchangeRate.collectAsState()
    var usdRateInput by remember(usdRate) { mutableStateOf(usdRate.toString()) }
    var showAuthDialog by remember { mutableStateOf(false) }

    val savedOrders by viewModel.savedOrders.collectAsState()
    val allPrices by viewModel.allPrices.collectAsState()
    val stoneMaterials by viewModel.stoneMaterials.collectAsState()
    val sizePresets by viewModel.sizePresets.collectAsState()
    val constructorPrices by viewModel.constructorServicePrices.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var showClearPasswordDialog by remember { mutableStateOf(false) }
    var clearPasswordInput by remember { mutableStateOf("") }
    var clearPasswordError by remember { mutableStateOf<String?>(null) }

    if (showClearPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearPasswordDialog = false
                clearPasswordInput = ""
                clearPasswordError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Подтверждение очистки")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Внимание! Данное действие безвозвратно удалит данные из Room и Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Введите пароль для подтверждения:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = clearPasswordInput,
                        onValueChange = {
                            clearPasswordInput = it
                            clearPasswordError = null
                        },
                        label = { Text("Пароль") },
                        singleLine = true,
                        isError = clearPasswordError != null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_password_input")
                    )
                    if (clearPasswordError != null) {
                        Text(
                            text = clearPasswordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clearPasswordInput.trim() == "2101264") {
                            viewModel.clearRoomAndFirestore()
                            showClearPasswordDialog = false
                            clearPasswordInput = ""
                            clearPasswordError = null
                        } else {
                            clearPasswordError = "Неверный пароль"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_btn")
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearPasswordDialog = false
                        clearPasswordInput = ""
                        clearPasswordError = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("📥 Импорт базы данных (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Вставьте текст резервной копии JSON ниже:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\"version\": 1, \"orders\": [...], ...}") },
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importDatabaseBackup(importJsonText.trim())
                            showImportDialog = false
                            importJsonText = ""
                        }
                    },
                    enabled = importJsonText.isNotBlank()
                ) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            syncManager = viewModel.syncManager,
            onDismiss = { showAuthDialog = false },
            onSuccess = { userEmail ->
                viewModel.showUserMessage("Успешная авторизация: $userEmail")
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. USD CURRENCY CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Курс доллара США (USD / BYN)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Используется для расчёта стоимости кубометра гранита",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = usdRateInput,
                        onValueChange = {
                            usdRateInput = it
                            it.toDoubleOrNull()?.let { rate ->
                                if (rate > 0) viewModel.updateUsdExchangeRate(rate)
                            }
                        },
                        label = { Text("Курс USD (BYN)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("usd_rate_input")
                    )

                    Button(
                        onClick = { viewModel.refreshNbrbUsdRate(showUserFeedback = true) },
                        modifier = Modifier.height(54.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Курс НБРБ")
                    }
                }
            }
        }

        // --- 2. CLOUD FIRESTORE SYNC CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (syncState) {
                    is SyncState.Connected -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    is SyncState.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (syncState) {
                            is SyncState.Connected -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            is SyncState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (syncState) {
                                    is SyncState.Connected -> Icons.Default.CloudDone
                                    is SyncState.Syncing -> Icons.Default.CloudSync
                                    is SyncState.Error -> Icons.Default.CloudOff
                                    else -> Icons.Default.CloudQueue
                                },
                                contentDescription = null,
                                tint = when (syncState) {
                                    is SyncState.Connected -> MaterialTheme.colorScheme.tertiary
                                    is SyncState.Error -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Синхронизация с облаком (Firestore)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (val s = syncState) {
                                is SyncState.Connected -> "🟢 Подключено: ${s.userEmail ?: "Общая база"}"
                                is SyncState.Syncing -> "🔄 Передача пакета данных..."
                                is SyncState.NotAuthenticated -> "🟡 Готово к работе"
                                is SyncState.Disabled -> "⚙️ Локальный режим (Room DB)"
                                is SyncState.Error -> "🔴 ${s.message}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (syncState) {
                                is SyncState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (syncState is SyncState.Syncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (syncState is SyncState.Error && ((syncState as SyncState.Error).message.contains("404") || (syncState as SyncState.Error).message.contains("datastore/setup") || (syncState as SyncState.Error).message.contains("does not exist"))) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🛠️ Активация Firestore в Google Cloud (1 шаг):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Google требует один раз нажать «Создать базу данных» по ссылке ниже (выберите режим «Firestore Native»):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://console.cloud.google.com/datastore/setup?project=dwa-angeld")
                                    )
                                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Открыть консоль активации 🌐")
                            }
                        }
                    }
                }

                Text(
                    text = "💡 Архитектура: Firestore является основным хранилищем данных. Room служит локальным кэшем для работы в офлайне.\n• Обновление кэша происходит автоматически при подключении к сети.\n• Изменение и сохранение данных выполняется напрямую в Firestore при наличии сети.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.forceUploadLocalDataToFirestore() },
                        enabled = syncState !is SyncState.Syncing,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Выгрузить в Firestore ☁️", style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedButton(
                        onClick = { viewModel.refreshLocalCacheFromFirestore() },
                        enabled = syncState !is SyncState.Syncing,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Обновить кэш 🔄", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testCloudConnection() },
                        enabled = syncState !is SyncState.Syncing,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Тест связи 📡", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (syncState is SyncState.Connected && viewModel.syncManager.getCurrentUserEmail() != null) {
                        OutlinedButton(
                            onClick = { viewModel.syncManager.signOut() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Text("Выйти из аккаунта", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showAuthDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Вход / Аккаунт", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Button(
                    onClick = {
                        clearPasswordInput = ""
                        clearPasswordError = null
                        showClearPasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("clear_db_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Очистить Room и Firestore 🗑️")
                }
            }
        }

        // --- 3. BACKUP & FILE EXCHANGE ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        text = "💾 Резервная копия и передача базы (JSON)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Сохраните всю базу (сметы, каталог, гранит и тарифы) в файл или отправьте коллеге через Telegram, Viber, WhatsApp или почту.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportDatabaseBackup(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Экспорт файла 📤", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Импорт 📥", maxLines = 1)
                    }
                }
            }
        }

        // --- 4. DATABASE STATISTICS ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📊 Локальная база данных (на этом телефоне)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Сохранённые сметы клиентов:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${savedOrders.size} шт.", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Позиций в прайс-каталоге:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${allPrices.size} шт.", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Видов гранита и материалов:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${stoneMaterials.size} шт.", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Типоразмеров стел и комплектов:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${sizePresets.size} шт.", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Тарифов конструктора услуг:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${constructorPrices.size} шт.", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 4. FIRESTORE RULES GUIDE ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Справка: Настройка базы и локация США",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "🇺🇸 Местоположение США (nam5 / us-central1):\n" +
                            "Это стандартный регион серверов Google по умолчанию. Он полностью подходит для работы и НЕ является причиной ошибки.\n\n" +
                            "🛠️ Почему может возникать таймаут:\n" +
                            "1. В консоли Firebase в меню «Firestore Database» нужно один раз нажать «Создать базу данных» (Create database), если она ещё не создана.\n" +
                            "2. Во вкладке «Rules» (Правила) нужно вставить allow read, write: if true; и обязательно нажать синюю кнопку «Publish» (Опубликовать).\n" +
                            "3. Если оператор связи блокирует соединение, нажмите кнопку «Тест связи 📡» — приложение покажет точный статус.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
