package com.example.ui.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.TeacherUiState
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LateContainer
import com.example.ui.theme.OnErrorContainer
import com.example.ui.theme.OnLateContainer
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object TodayDateHelpers {
    fun currentManilaDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return sdf.format(Date())
    }

    fun dateKeyToUtcMillis(dateKey: String): Long? {
        if (dateKey.isBlank()) return null
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            sdf.parse(dateKey.trim())?.time
        }.getOrNull()
    }

    fun utcMillisToDateKey(utcMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(utcMillis))
    }

    fun formatShortDate(dateKey: String): String {
        val millis = dateKeyToUtcMillis(dateKey) ?: return dateKey
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    fun formatFriendlyDate(dateKey: String): String {
        val millis = dateKeyToUtcMillis(dateKey) ?: return dateKey
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayTabContent(
    uiState: TeacherUiState,
    onRefresh: () -> Unit,
    onSelectDate: (String) -> Unit,
    onResetToToday: () -> Unit,
    onStepOlder: () -> Unit,
    onStepNewer: () -> Unit,
    onManualMark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = uiState.todayData
    val manilaToday = today?.today?.takeIf { it.isNotBlank() } ?: remember { TodayDateHelpers.currentManilaDateKey() }
    val effectiveDate = uiState.selectedTodayDate.ifBlank {
        today?.date?.takeIf { it.isNotBlank() } ?: manilaToday
    }
    val isViewingPastDate = when {
        today?.isPast != null -> today.isPast
        today?.isToday != null -> !today.isToday
        else -> effectiveDate < manilaToday
    }

    val olderTarget = remember(uiState.recordedClassDates, effectiveDate) {
        uiState.recordedClassDates.firstOrNull { it < effectiveDate }
    }
    val hasNewerTarget = effectiveDate < manilaToday

    var showCalendarPicker by remember { mutableStateOf(false) }
    var showDatesDropdown by remember { mutableStateOf(false) }
    var confirmingStudentId by remember(effectiveDate, today?.absent) { mutableStateOf<String?>(null) }

    if (showCalendarPicker) {
        val initialMillis = TodayDateHelpers.dateKeyToUtcMillis(effectiveDate)
            ?: TodayDateHelpers.dateKeyToUtcMillis(manilaToday)
        val maxYear = manilaToday.substring(0, 4).toIntOrNull() ?: 9999
        val selectableDates = remember(manilaToday) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return TodayDateHelpers.utcMillisToDateKey(utcTimeMillis) <= manilaToday
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= maxYear
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showCalendarPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            val pickedKey = TodayDateHelpers.utcMillisToDateKey(pickedMillis)
                            showCalendarPicker = false
                            if (pickedKey == manilaToday) {
                                onResetToToday()
                            } else {
                                onSelectDate(pickedKey)
                            }
                        } else {
                            showCalendarPicker = false
                        }
                    }
                ) {
                    Text("View Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date picker + recorded class dates + Older / Today / Newer navigation
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Pick a date (calendar)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(
                            onClick = { showCalendarPicker = true },
                            enabled = !uiState.isLoadingToday,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("today_calendar_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = TodayDateHelpers.formatShortDate(effectiveDate),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Class dates with records",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box {
                            OutlinedButton(
                                onClick = { showDatesDropdown = true },
                                enabled = !uiState.isLoadingToday,
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("today_recorded_dates_dropdown")
                            ) {
                                Text(
                                    text = if (!isViewingPastDate) {
                                        "Today"
                                    } else {
                                        TodayDateHelpers.formatShortDate(effectiveDate)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Choose a class date with records"
                                )
                            }

                            DropdownMenu(
                                expanded = showDatesDropdown,
                                onDismissRequest = { showDatesDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Today (${TodayDateHelpers.formatShortDate(manilaToday)})") },
                                    trailingIcon = if (!isViewingPastDate) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        showDatesDropdown = false
                                        onResetToToday()
                                    }
                                )

                                if (uiState.recordedClassDates.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "No past recorded dates yet",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        enabled = false,
                                        onClick = {}
                                    )
                                } else {
                                    HorizontalDivider()
                                    uiState.recordedClassDates.forEach { recordedDate ->
                                        val isSelected = effectiveDate == recordedDate
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = TodayDateHelpers.formatShortDate(recordedDate) +
                                                        if (recordedDate == manilaToday) " (Today)" else ""
                                                )
                                            },
                                            trailingIcon = if (isSelected) {
                                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                            } else {
                                                null
                                            },
                                            onClick = {
                                                showDatesDropdown = false
                                                onSelectDate(recordedDate)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onStepOlder,
                        enabled = olderTarget != null && !uiState.isLoadingToday,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("today_step_older_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Older", maxLines = 1)
                    }

                    if (isViewingPastDate) {
                        OutlinedButton(
                            onClick = onResetToToday,
                            enabled = !uiState.isLoadingToday,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .heightIn(min = 48.dp)
                                .testTag("today_jump_today_button")
                        ) {
                            Text("Today", maxLines = 1)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onRefresh,
                            enabled = !uiState.isLoadingToday,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .heightIn(min = 48.dp)
                                .testTag("today_jump_today_button")
                        ) {
                            Text("Today", maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onStepNewer,
                        enabled = hasNewerTarget && !uiState.isLoadingToday,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("today_step_newer_button")
                    ) {
                        Text("Newer", maxLines = 1)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Date header + scheduled day status
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val heading = today?.dateLabel?.takeIf { it.isNotBlank() }
                            ?: TodayDateHelpers.formatFriendlyDate(effectiveDate)
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isViewingPastDate) {
                                Surface(
                                    color = LateContainer,
                                    contentColor = OnLateContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "(past date)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (today != null) {
                                val statusIcon = if (today.isDay) {
                                    Icons.Filled.EventAvailable
                                } else {
                                    Icons.Filled.EventBusy
                                }
                                val statusText = when {
                                    today.isDay && isViewingPastDate -> "Regular class day"
                                    today.isDay && !isViewingPastDate -> "Regular class day today"
                                    !today.isDay && isViewingPastDate -> "There was no class scheduled on that day."
                                    else -> "No class scheduled today."
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isLoadingToday,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("today_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh date attendance"
                        )
                    }
                }
            }
        }

        if (uiState.isLoadingToday) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!uiState.todayError.isNullOrBlank()) {
            Surface(
                color = ErrorContainer,
                contentColor = OnErrorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.todayError,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else if (today != null) {
            val enrolledTotal = today.counts.total.takeIf { it > 0 }
                ?: (today.present.size + today.absent.size)

            // Responsive 4-stat grid (2x2 on narrow phones, 4-across on wider screens)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 380.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountStatChip("Present", today.counts.present, PresentContainer, OnPresentContainer, Modifier.weight(1f))
                            CountStatChip("Late", today.counts.late, LateContainer, OnLateContainer, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CountStatChip("Absent", today.counts.absent, ErrorContainer, OnErrorContainer, Modifier.weight(1f))
                            CountStatChip("Enrolled", enrolledTotal, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountStatChip("Present", today.counts.present, PresentContainer, OnPresentContainer, Modifier.weight(1f))
                        CountStatChip("Late", today.counts.late, LateContainer, OnLateContainer, Modifier.weight(1f))
                        CountStatChip("Absent", today.counts.absent, ErrorContainer, OnErrorContainer, Modifier.weight(1f))
                        CountStatChip("Enrolled", enrolledTotal, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    }
                }
            }

            if (today.present.isNotEmpty()) {
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
                            text = "Checked In (${today.present.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        today.present.forEachIndexed { idx, row ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${row.id} | ${row.time} (${row.method})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val isLate = row.status.equals("Late", ignoreCase = true)
                                Surface(
                                    color = if (isLate) LateContainer else PresentContainer,
                                    contentColor = if (isLate) OnLateContainer else OnPresentContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = row.status,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (today.absent.isNotEmpty()) {
                val secondListTitle = if (isViewingPastDate) {
                    "Absent (${today.absent.size})"
                } else {
                    "Not yet in (${today.absent.size})"
                }

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
                            text = secondListTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isViewingPastDate) {
                            Text(
                                text = "Correcting a past date: tap Mark Present twice to confirm. Records are stamped at class start time and labeled Manual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        today.absent.forEachIndexed { idx, row ->
                            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            val isMarking = uiState.markingStudentId == row.id
                            val isConfirming = isViewingPastDate && confirmingStudentId == row.id

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(row.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = row.id,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isConfirming) {
                                    Button(
                                        onClick = {
                                            confirmingStudentId = null
                                            onManualMark(row.id)
                                        },
                                        enabled = uiState.markingStudentId == null,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LateContainer,
                                            contentColor = OnLateContainer
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("confirm_mark_present_${row.id}")
                                    ) {
                                        if (isMarking) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Tap to Confirm")
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            if (isViewingPastDate) {
                                                confirmingStudentId = row.id
                                            } else {
                                                onManualMark(row.id)
                                            }
                                        },
                                        enabled = uiState.markingStudentId == null,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("mark_present_${row.id}")
                                    ) {
                                        if (isMarking) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Mark Present")
                                        }
                                    }
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
private fun CountStatChip(
    label: String,
    value: Int,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}
