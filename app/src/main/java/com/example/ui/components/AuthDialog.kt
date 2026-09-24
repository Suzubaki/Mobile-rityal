package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.cloud.FirebaseSyncManager
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    syncManager: FirebaseSyncManager,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isRegisterMode) "Регистрация аккаунта" else "Вход в синхронизацию",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Авторизуйтесь или используйте общую базу Firestore, чтобы сметы и прайс-листы были одинаковыми на всех смартфонах.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email (логин)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Пароль (от 6 знаков)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isRegisterMode = !isRegisterMode; errorMessage = null }) {
                        Text(
                            text = if (isRegisterMode) "Уже есть аккаунт? Войти" else "Создать новый аккаунт",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = {
                        syncManager.startRealtimeListeners()
                        onSuccess("Общая база (Firestore)")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Использовать общий Firestore без входа", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        errorMessage = "Введите валидный Email и пароль (мин. 6 символов)"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = if (isRegisterMode) {
                            syncManager.registerWithEmail(email.trim(), password)
                        } else {
                            syncManager.signInWithEmail(email.trim(), password)
                        }
                        isLoading = false
                        result.onSuccess { userEmail ->
                            onSuccess(userEmail)
                            onDismiss()
                        }.onFailure { err ->
                            val msg = err.localizedMessage ?: "Ошибка авторизации"
                            errorMessage = if (msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                                "В Firebase Console проекта еще не включен метод входа Email/Password. Включите его в Console -> Authentication -> Sign-in method, либо нажмите кнопку ниже «Использовать общий Firestore без входа»."
                            } else if (msg.contains("EMAIL_EXISTS", ignoreCase = true) || msg.contains("email address is already in use", ignoreCase = true)) {
                                "Этот Email уже зарегистрирован! Переключитесь в режим «Войти»."
                            } else if (msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) || msg.contains("wrong password", ignoreCase = true)) {
                                "Неверный Email или пароль. Проверьте правильность ввода."
                            } else {
                                msg
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (isRegisterMode) "Создать аккаунт" else "Войти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
