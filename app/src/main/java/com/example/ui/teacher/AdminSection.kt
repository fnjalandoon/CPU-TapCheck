package com.example.ui.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.TeacherUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentGreen

@Composable
fun AdminSection(
    uiState: TeacherUiState,
    onAddTeacher: (username: String, name: String) -> Unit,
    onResetKey: (username: String) -> Unit,
    onToggleActive: (username: String, active: Boolean) -> Unit,
    onSaveSettings: (threshold: Int, webAppUrl: String) -> Unit,
    onDismissKeyBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showAddTeacherDialog by remember { mutableStateOf(false) }

    var thresholdInput by remember { mutableStateOf("80") }
    var webAppUrlInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.adminSettings) {
        uiState.adminSettings?.let { s ->
            thresholdInput = s.threshold.toString()
            webAppUrlInput = s.webAppUrl
        }
    }

    if (showAddTeacherDialog) {
        var newUsername by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTeacherDialog = false },
            title = { Text("Add Teacher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. maria.santos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Dr. Maria Santos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddTeacher(newUsername.trim(), newName.trim())
                        showAddTeacherDialog = false
                    },
                    enabled = newUsername.isNotBlank() && newName.isNotBlank()
                ) {
                    Text("Create Teacher")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTeacherDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.generatedTempKeyBanner.isNullOrBlank()) {
            Surface(
                color = PresentContainer,
                contentColor = OnPresentContainer,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, PresentGreen.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.generatedTempKeyBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(uiState.generatedTempKeyBanner))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy key")
                    }
                    TextButton(onClick = onDismissKeyBanner) {
                        Text("OK")
                    }
                }
            }
        }

        if (!uiState.adminMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.adminMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (uiState.isLoadingAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Teachers (${uiState.teachers.size})", style = MaterialTheme.typography.titleLarge)
                        Button(
                            onClick = { showAddTeacherDialog = true },
                            enabled = !uiState.isSavingAdmin,
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Teacher")
                        }
                    }

                    uiState.teachers.forEachIndexed { idx, teacher ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(teacher.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${teacher.username} (${teacher.role}) | ${teacher.classes} classes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onResetKey(teacher.username) },
                                    enabled = !uiState.isSavingAdmin,
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text("Reset Key")
                                }
                                if (!teacher.role.equals("admin", ignoreCase = true)) {
                                    Switch(
                                        checked = teacher.active,
                                        onCheckedChange = { active -> onToggleActive(teacher.username, active) },
                                        enabled = !uiState.isSavingAdmin
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Attendance Settings", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Required Attendance (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = webAppUrlInput,
                        onValueChange = { webAppUrlInput = it },
                        label = { Text("Public Web App URL") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                thresholdInput.toIntOrNull() ?: 80,
                                webAppUrlInput.trim()
                            )
                        },
                        enabled = !uiState.isSavingAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }
    }
}
