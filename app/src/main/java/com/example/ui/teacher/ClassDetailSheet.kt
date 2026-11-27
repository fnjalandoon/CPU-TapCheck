package com.example.ui.teacher

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ClassSummary
import com.example.data.NfcTagWriter
import com.example.data.NfcWriteFormat
import com.example.data.NfcWriteResult
import com.example.data.RosterRowPayload
import com.example.ui.ClassDetailTab
import com.example.ui.NfcHardwareStatus
import com.example.ui.TeacherUiState
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LateAmber
import com.example.ui.theme.LateContainer
import com.example.ui.theme.OnErrorContainer
import com.example.ui.theme.OnLateContainer
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailSheet(
    cls: ClassSummary,
    uiState: TeacherUiState,
    nfcStatus: NfcHardwareStatus,
    onBack: () -> Unit,
    onSelectTab: (ClassDetailTab) -> Unit,
    onRefreshToday: () -> Unit,
    onSelectTodayDate: (String) -> Unit,
    onResetTodayToCurrent: () -> Unit,
    onStepTodayOlder: () -> Unit,
    onStepTodayNewer: () -> Unit,
    onManualMark: (String) -> Unit,
    onRefreshRoster: () -> Unit,
    onAddStudents: (List<RosterRowPayload>) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onDismissRosterBanner: () -> Unit,
    onEditClass: (ClassSummary) -> Unit,
    onWritePayloadChanged: (String) -> Unit,
    onWriteFormatChanged: (NfcWriteFormat) -> Unit,
    onLockReadOnlyChanged: (Boolean) -> Unit,
    onArmWriter: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showLockConfirmDialog by remember { mutableStateOf(false) }

    if (showLockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLockConfirmDialog = false },
            title = { Text("Lock NFC Card Permanently?") },
            text = {
                Text("Locking makes the NFC card read-only forever so nobody can overwrite it. You will not be able to reuse that card for another subject.")
            },
            confirmButton = {
                Button(onClick = {
                    onLockReadOnlyChanged(true)
                    showLockConfirmDialog = false
                }) {
                    Text("Enable Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to classes"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cls.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Code: ${cls.code} | ${cls.schedule}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onSelectTab(ClassDetailTab.Roster) }) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Manage roster and add students"
                )
            }
            IconButton(onClick = { onEditClass(cls) }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit class"
                )
            }
        }

        val tabs = listOf(
            ClassDetailTab.Today to "Today",
            ClassDetailTab.Roster to "Roster",
            ClassDetailTab.Totals to "Totals",
            ClassDetailTab.WriteNfc to "NFC"
        )
        PrimaryTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == uiState.classDetailTab }.coerceAtLeast(0)
        ) {
            tabs.forEach { (tab, label) ->
                Tab(
                    selected = uiState.classDetailTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = { Text(label) }
                )
            }
        }

        when (uiState.classDetailTab) {
            ClassDetailTab.Today -> TodayTabContent(
                uiState = uiState,
                onRefresh = onRefreshToday,
                onSelectDate = onSelectTodayDate,
                onResetToToday = onResetTodayToCurrent,
                onStepOlder = onStepTodayOlder,
                onStepNewer = onStepTodayNewer,
                onManualMark = onManualMark
            )
            ClassDetailTab.Roster -> RosterTabContent(
                uiState = uiState,
                onRefreshRoster = onRefreshRoster,
                onAddStudents = onAddStudents,
                onRemoveStudent = onRemoveStudent,
                onDismissBanner = onDismissRosterBanner
            )
            ClassDetailTab.Totals -> TotalsTabContent(uiState = uiState)
            ClassDetailTab.WriteNfc -> WriteNfcTabContent(
                uiState = uiState,
                nfcStatus = nfcStatus,
                onWritePayloadChanged = onWritePayloadChanged,
                onWriteFormatChanged = onWriteFormatChanged,
                onRequestLockToggle = { desired ->
                    if (desired) {
                        showLockConfirmDialog = true
                    } else {
                        onLockReadOnlyChanged(false)
                    }
                },
                onArmWriter = onArmWriter
            )
        }
    }
}

@Composable
private fun TotalsTabContent(uiState: TeacherUiState) {
    val totals = uiState.totalsData
    if (uiState.isLoadingTotals) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (!uiState.totalsError.isNullOrBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = uiState.totalsError,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp)
            )
        }
    } else if (totals != null) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Meetings held: ${totals.held} | Requirement: ${totals.threshold}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                totals.rows.forEachIndexed { idx, row ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    val pct = row.pct
                    val belowThreshold = pct != null && pct < totals.threshold
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${row.id} | ${row.attended}/${totals.held} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (pct == null) "N/A" else "${pct}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = if (belowThreshold) ErrorCrimson else PresentGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteNfcTabContent(
    uiState: TeacherUiState,
    nfcStatus: NfcHardwareStatus,
    onWritePayloadChanged: (String) -> Unit,
    onWriteFormatChanged: (NfcWriteFormat) -> Unit,
    onRequestLockToggle: (Boolean) -> Unit,
    onArmWriter: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Write Classroom NFC Card",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (nfcStatus != NfcHardwareStatus.Ready) {
                Surface(
                    color = LateContainer,
                    contentColor = OnLateContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (nfcStatus == NfcHardwareStatus.Disabled) {
                            "NFC is turned off on this phone. Turn on NFC in Settings to write cards."
                        } else {
                            "This device does not have NFC hardware to write cards."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.writePayloadCode,
                onValueChange = onWritePayloadChanged,
                label = { Text("Code to Write on Card") },
                supportingText = { Text("Use the Subject Code (e.g. HS 3202) or Sheet Code.") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nfc_write_code_input")
            )

            Text(
                text = "Card Data Format",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = uiState.writeFormat == NfcWriteFormat.CompactText,
                    onClick = { onWriteFormatChanged(NfcWriteFormat.CompactText) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Compact Subject Code (Recommended)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Fits easily on 144-byte NTAG213 cards. Opens CPU TapCheck directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = uiState.writeFormat == NfcWriteFormat.WebUrl,
                    onClick = { onWriteFormatChanged(NfcWriteFormat.WebUrl) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Web Link + App Launch", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Works with CPU TapCheck or opens the web attendance page on iPhones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val preview = NfcTagWriter.buildPayloadString(uiState.writePayloadCode, uiState.writeFormat)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Will write: $preview",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lock Card Read-Only", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Permanent. Prevents anyone from erasing the card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.lockReadOnly,
                    onCheckedChange = onRequestLockToggle
                )
            }

            when (val res = uiState.nfcWriteResult) {
                is NfcWriteResult.Success -> {
                    Surface(
                        color = PresentContainer,
                        contentColor = OnPresentContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, PresentGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PresentGreen)
                                Text(
                                    text = if (res.lockedReadOnly) "NFC Card Written and Locked!" else "NFC Card Written!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Saved: ${res.payloadDescription}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = onArmWriter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Write Another Card")
                            }
                        }
                    }
                }
                is NfcWriteResult.Failure -> {
                    Surface(
                        color = ErrorContainer,
                        contentColor = OnErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorCrimson)
                            Text(res.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                null -> Unit
            }

            if (uiState.isWaitingToWriteNfc && nfcStatus == NfcHardwareStatus.Ready) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Nfc, contentDescription = null, modifier = Modifier.size(28.dp))
                        Column {
                            Text(
                                text = "Ready to Write",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap a blank or rewritable NFC card on the back of your phone now.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
