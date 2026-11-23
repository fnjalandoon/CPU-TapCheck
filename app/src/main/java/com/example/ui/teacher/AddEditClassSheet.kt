package com.example.ui.teacher

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ClassInfoPayload
import com.example.data.ClassSummary
import com.example.data.CsvRosterParser
import com.example.data.ParsedRosterRow
import com.example.data.RosterFileParseResult
import com.example.data.RosterRowPayload

private val ALL_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditClassSheet(
    editingClass: ClassSummary?,
    isSaving: Boolean,
    error: String?,
    onSave: (ClassInfoPayload, List<RosterRowPayload>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancel)

    val context = LocalContext.current
    var subject by rememberSaveable { mutableStateOf(editingClass?.subject.orEmpty()) }
    var selectedDays by rememberSaveable {
        mutableStateOf(
            if (editingClass != null) {
                ALL_DAYS.filter { day ->
                    editingClass.days.contains(day, ignoreCase = true)
                }.toSet()
            } else {
                setOf("Mon", "Wed", "Fri")
            }
        )
    }
    var startTime by rememberSaveable { mutableStateOf(editingClass?.start?.ifBlank { "09:00" } ?: "09:00") }
    var endTime by rememberSaveable { mutableStateOf(editingClass?.end?.ifBlank { "10:30" } ?: "10:30") }
    var openBefore by rememberSaveable { mutableStateOf((editingClass?.openBefore ?: 30).toString()) }
    var lateAfter by rememberSaveable { mutableStateOf((editingClass?.lateAfter ?: 15).toString()) }
    var extraDates by rememberSaveable { mutableStateOf(editingClass?.extra.orEmpty()) }
    var skipDates by rememberSaveable { mutableStateOf(editingClass?.skip.orEmpty()) }
    var rosterText by rememberSaveable { mutableStateOf("") }
    var importedFileRows by remember { mutableStateOf<List<ParsedRosterRow>>(emptyList()) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val parsedFromText = remember(rosterText) { CsvRosterParser.parse(rosterText) }
    val parsedRoster = remember(importedFileRows, parsedFromText) {
        if (parsedFromText.isNotEmpty()) parsedFromText else importedFileRows
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            }.onSuccess { bytes ->
                when (val result = CsvRosterParser.parseFileBytes(bytes)) {
                    is RosterFileParseResult.Success -> {
                        importedFileRows = result.rows
                        rosterText = result.rows.joinToString("\n") { row ->
                            if (row.course.isNotBlank()) "${row.id}\t${row.name}\t${row.course}"
                            else "${row.id}\t${row.name}"
                        }
                        localError = null
                    }
                    is RosterFileParseResult.Error -> {
                        localError = result.message
                    }
                }
            }.onFailure {
                localError = "Could not read that file. Try a .csv or .xlsx Excel file."
            }
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editingClass == null) "Add New Class / Subject" else "Edit Class (${editingClass.code})",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it; localError = null },
                label = { Text("Subject Code & Name") },
                placeholder = { Text("e.g. BUS 2101: Good Governance") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("class_subject_input")
            )

            Text(
                text = "Class Days",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ALL_DAYS.forEach { day ->
                    val isSelected = day in selectedDays
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                            localError = null
                        },
                        label = { Text(day) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it; localError = null },
                    label = { Text("Start (e.g. 09:00)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it; localError = null },
                    label = { Text("End (e.g. 10:30)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = openBefore,
                    onValueChange = { openBefore = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Open before (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lateAfter,
                    onValueChange = { lateAfter = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Late after (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = extraDates,
                onValueChange = { extraDates = it },
                label = { Text("Extra Dates (optional, YYYY-MM-DD)") },
                placeholder = { Text("e.g. 2026-12-08") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = skipDates,
                onValueChange = { skipDates = it },
                label = { Text("Skip Dates (optional, YYYY-MM-DD)") },
                placeholder = { Text("e.g. 2026-12-25") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (editingClass == null) {
                Text(
                    text = "Student Roster",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = {
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
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("pick_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Import CSV or Excel (.xlsx)")
                }

                OutlinedTextField(
                    value = rosterText,
                    onValueChange = {
                        rosterText = it
                        importedFileRows = emptyList()
                        localError = null
                    },
                    label = { Text("Or Paste Roster (ID, Name, Course)") },
                    placeholder = { Text("25-0227-24, \"Santos, Maria\", BSBA 2") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roster_paste_input")
                )

                Text(
                    text = "${parsedRoster.size} valid student(s) ready to enroll",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val displayError = localError ?: error
            if (!displayError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (subject.isBlank()) {
                            localError = "Enter a subject code and name."
                            return@Button
                        }
                        if (selectedDays.isEmpty()) {
                            localError = "Select at least one class day."
                            return@Button
                        }
                        val info = ClassInfoPayload(
                            subject = subject.trim(),
                            days = selectedDays.joinToString(", "),
                            start = startTime.trim(),
                            end = endTime.trim(),
                            openBefore = openBefore.toIntOrNull() ?: 30,
                            lateAfter = lateAfter.toIntOrNull() ?: 15,
                            extra = extraDates.trim(),
                            skip = skipDates.trim()
                        )
                        val rows = parsedRoster.map {
                            RosterRowPayload(id = it.id, name = it.name, course = it.course)
                        }
                        onSave(info, rows)
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("save_class_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (editingClass == null) "Create Class" else "Save Changes")
                }
            }
        }
    }
}
