package com.example.ui.teacher

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.CsvRosterParser
import com.example.data.ParsedRosterRow
import com.example.data.RosterFileParseResult
import com.example.data.RosterRowPayload
import com.example.ui.TeacherUiState
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnErrorContainer
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentGreen

@Composable
fun RosterTabContent(
    uiState: TeacherUiState,
    onRefreshRoster: () -> Unit,
    onAddStudents: (List<RosterRowPayload>) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showSingleAddDialog by rememberSaveable { mutableStateOf(false) }
    var showPastePanel by rememberSaveable { mutableStateOf(false) }
    var pasteText by rememberSaveable { mutableStateOf("") }
    var pendingImportRows by remember { mutableStateOf<List<ParsedRosterRow>?>(null) }
    var localFileError by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingDeleteStudent by remember { mutableStateOf<RosterRowPayload?>(null) }

    val parsedFromPaste = remember(pasteText) { CsvRosterParser.parse(pasteText) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            }.onSuccess { bytes ->
                when (val result = CsvRosterParser.parseFileBytes(bytes)) {
                    is RosterFileParseResult.Success -> {
                        localFileError = null
                        pendingImportRows = result.rows
                    }
                    is RosterFileParseResult.Error -> {
                        localFileError = result.message
                    }
                }
            }.onFailure {
                localFileError = "Could not open that file. Try a .csv or .xlsx Excel file."
            }
        }
    }

    if (showSingleAddDialog) {
        AddSingleStudentDialog(
            onDismiss = { showSingleAddDialog = false },
            onConfirm = { student ->
                showSingleAddDialog = false
                onAddStudents(listOf(student))
            }
        )
    }

    pendingImportRows?.let { imported ->
        AlertDialog(
            onDismissRequest = { pendingImportRows = null },
            title = { Text("Import ${imported.size} Student(s)?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ready to enroll ${imported.size} student(s). Any ID already in this class will be skipped automatically.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            imported.take(5).forEach { row ->
                                Text(
                                    text = "${row.id} | ${row.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (imported.size > 5) {
                                Text(
                                    text = "...and ${imported.size - 5} more",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val payload = imported.map {
                        RosterRowPayload(id = it.id, name = it.name, course = it.course)
                    }
                    pendingImportRows = null
                    onAddStudents(payload)
                }) {
                    Text("Add Students")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportRows = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingDeleteStudent?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteStudent = null },
            title = { Text("Remove Student?") },
            text = {
                Text("Remove ${target.name} (${target.id}) from this class roster? Past attendance records stay in the Attendance sheet.")
            },
            confirmButton = {
                Button(onClick = {
                    val id = target.id
                    pendingDeleteStudent = null
                    onRemoveStudent(id)
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteStudent = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Students Actions Card
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Students to Class",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add a late enrollee one by one, import a CSV or Excel (.xlsx) list, or paste rows from Excel or Google Sheets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        localFileError = null
                        onDismissBanner()
                        showSingleAddDialog = true
                    },
                    enabled = !uiState.isSavingRoster,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("roster_add_single_button")
                ) {
                    if (uiState.isSavingRoster) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Student")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            localFileError = null
                            onDismissBanner()
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        enabled = !uiState.isSavingRoster,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp)
                            .testTag("roster_import_file_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CSV / Excel")
                    }

                    OutlinedButton(
                        onClick = { showPastePanel = !showPastePanel },
                        enabled = !uiState.isSavingRoster,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("roster_toggle_paste_button")
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paste List")
                    }
                }

                AnimatedVisibility(visible = showPastePanel) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it; localFileError = null },
                            label = { Text("Paste ID, Name, Course (from Excel or CSV)") },
                            placeholder = { Text("25-0227-24\tSantos, Maria\tBSBA 2") },
                            minLines = 3,
                            maxLines = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("roster_paste_batch_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${parsedFromPaste.size} student(s) detected",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = {
                                    val payload = parsedFromPaste.map {
                                        RosterRowPayload(id = it.id, name = it.name, course = it.course)
                                    }
                                    pasteText = ""
                                    showPastePanel = false
                                    onAddStudents(payload)
                                },
                                enabled = parsedFromPaste.isNotEmpty() && !uiState.isSavingRoster,
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Enroll ${parsedFromPaste.size}")
                            }
                        }
                    }
                }
            }
        }

        if (!uiState.rosterBanner.isNullOrBlank()) {
            Surface(
                color = PresentContainer,
                contentColor = OnPresentContainer,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, PresentGreen.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PresentGreen)
                    Text(
                        text = uiState.rosterBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissBanner) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        val errorMsg = localFileError ?: uiState.rosterError
        if (!errorMsg.isNullOrBlank()) {
            Surface(
                color = ErrorContainer,
                contentColor = OnErrorContainer,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorCrimson)
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        localFileError = null
                        onDismissBanner()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        // Enrolled Roster Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enrolled Students (${uiState.rosterRows.size})",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(
                onClick = onRefreshRoster,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh")
            }
        }

        if (uiState.isLoadingRoster) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.rosterRows.isEmpty()) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No students on this roster yet. Use the buttons above to add students.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            if (uiState.rosterRows.size > 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name or ID...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val filtered = remember(uiState.rosterRows, searchQuery) {
                val q = searchQuery.trim().lowercase()
                if (q.isEmpty()) uiState.rosterRows
                else uiState.rosterRows.filter {
                    it.name.lowercase().contains(q) || it.id.lowercase().contains(q)
                }
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filtered.forEachIndexed { idx, row ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                val meta = if (row.course.isNotBlank()) "${row.id} | ${row.course}" else row.id
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (uiState.removingStudentId == row.id) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { pendingDeleteStudent = row },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove ${row.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSingleStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (RosterRowPayload) -> Unit
) {
    var idInput by rememberSaveable { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var courseInput by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it; validationError = null },
                    label = { Text("Student ID") },
                    placeholder = { Text("e.g. 25-0227-24 or 93056169") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("single_student_id_input")
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; validationError = null },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Santos, Maria") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("single_student_name_input")
                )
                OutlinedTextField(
                    value = courseInput,
                    onValueChange = { courseInput = it },
                    label = { Text("Course & Year (optional)") },
                    placeholder = { Text("e.g. BSED 3") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorCrimson
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanId = idInput.trim()
                    val cleanName = nameInput.trim()
                    val digitsCount = cleanId.count { it.isDigit() }
                    when {
                        digitsCount < 6 -> validationError = "Student ID must have at least 6 digits."
                        cleanName.isBlank() -> validationError = "Please enter the student's name."
                        else -> onConfirm(
                            RosterRowPayload(
                                id = cleanId,
                                name = cleanName,
                                course = courseInput.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_single_student")
            ) {
                Text("Add Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
