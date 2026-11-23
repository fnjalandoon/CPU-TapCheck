package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CheckInOutcome
import com.example.ui.CheckInUiState
import com.example.ui.theme.AlreadyBlue
import com.example.ui.theme.AlreadyContainer
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LateAmber
import com.example.ui.theme.LateContainer
import com.example.ui.theme.OfflineContainer
import com.example.ui.theme.OfflineSlate
import com.example.ui.theme.OnAlreadyContainer
import com.example.ui.theme.OnErrorContainer
import com.example.ui.theme.OnLateContainer
import com.example.ui.theme.OnOfflineContainer
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentGreen

@Composable
fun CheckInResultCard(
    checkInState: CheckInUiState,
    onDone: () -> Unit,
    onUpdateId: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (checkInState !is CheckInUiState.Idle) {
        BackHandler(onBack = onDone)
    }

    when (checkInState) {
        CheckInUiState.Idle -> Unit
        is CheckInUiState.Loading -> {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("checkin_loading_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = stringResource(R.string.checking_in_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(
                            R.string.checking_in_subtitle,
                            checkInState.classCode
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        CheckInUiState.InvalidTag -> {
            ResultLayout(
                accentColor = ErrorCrimson,
                containerColor = ErrorContainer,
                onContainerColor = OnErrorContainer,
                icon = Icons.Filled.ErrorOutline,
                headline = stringResource(R.string.result_invalid_tag_title),
                greeting = null,
                subject = null,
                dateLabel = null,
                timeLabel = null,
                classCode = null,
                detailMessage = stringResource(R.string.result_invalid_tag_message),
                showUpdateIdAction = false,
                onDone = onDone,
                onUpdateId = onUpdateId,
                modifier = modifier
            )
        }
        is CheckInUiState.Result -> {
            when (val outcome = checkInState.outcome) {
                is CheckInOutcome.Present -> {
                    ResultLayout(
                        accentColor = PresentGreen,
                        containerColor = PresentContainer,
                        onContainerColor = OnPresentContainer,
                        icon = Icons.Filled.CheckCircle,
                        headline = stringResource(R.string.result_present_title),
                        greeting = outcome.given.takeIf { it.isNotBlank() }?.let {
                            stringResource(R.string.result_greeting, it)
                        },
                        subject = outcome.subject.takeIf { it.isNotBlank() },
                        dateLabel = outcome.dateLabel.takeIf { it.isNotBlank() },
                        timeLabel = outcome.time.takeIf { it.isNotBlank() },
                        classCode = outcome.classCode,
                        detailMessage = null,
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.Late -> {
                    ResultLayout(
                        accentColor = LateAmber,
                        containerColor = LateContainer,
                        onContainerColor = OnLateContainer,
                        icon = Icons.Filled.AccessTime,
                        headline = stringResource(R.string.result_late_title),
                        greeting = outcome.given.takeIf { it.isNotBlank() }?.let {
                            stringResource(R.string.result_greeting, it)
                        },
                        subject = outcome.subject.takeIf { it.isNotBlank() },
                        dateLabel = outcome.dateLabel.takeIf { it.isNotBlank() },
                        timeLabel = outcome.time.takeIf { it.isNotBlank() },
                        classCode = outcome.classCode,
                        detailMessage = null,
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.AlreadyCheckedIn -> {
                    ResultLayout(
                        accentColor = AlreadyBlue,
                        containerColor = AlreadyContainer,
                        onContainerColor = OnAlreadyContainer,
                        icon = Icons.Filled.Verified,
                        headline = stringResource(R.string.result_already_title),
                        greeting = outcome.given.takeIf { it.isNotBlank() }?.let {
                            stringResource(R.string.result_greeting, it)
                        },
                        subject = outcome.subject.takeIf { it.isNotBlank() },
                        dateLabel = outcome.dateLabel.takeIf { it.isNotBlank() },
                        timeLabel = outcome.time.takeIf { it.isNotBlank() }?.let {
                            stringResource(R.string.result_first_scan_at, it)
                        },
                        classCode = outcome.classCode,
                        detailMessage = "Recorded status: ${outcome.status}",
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.Closed -> {
                    ResultLayout(
                        accentColor = OfflineSlate,
                        containerColor = OfflineContainer,
                        onContainerColor = OnOfflineContainer,
                        icon = Icons.Filled.LockClock,
                        headline = stringResource(R.string.result_closed_title),
                        greeting = null,
                        subject = outcome.subject.takeIf { it.isNotBlank() },
                        dateLabel = null,
                        timeLabel = null,
                        classCode = outcome.classCode,
                        detailMessage = outcome.message,
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.NoClass -> {
                    ResultLayout(
                        accentColor = OfflineSlate,
                        containerColor = OfflineContainer,
                        onContainerColor = OnOfflineContainer,
                        icon = Icons.Filled.EventBusy,
                        headline = stringResource(R.string.result_noclass_title),
                        greeting = null,
                        subject = outcome.subject.takeIf { it.isNotBlank() },
                        dateLabel = null,
                        timeLabel = null,
                        classCode = outcome.classCode,
                        detailMessage = outcome.message,
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.NotFound -> {
                    ResultLayout(
                        accentColor = ErrorCrimson,
                        containerColor = ErrorContainer,
                        onContainerColor = OnErrorContainer,
                        icon = Icons.Filled.PersonOff,
                        headline = stringResource(R.string.result_notfound_title),
                        greeting = null,
                        subject = outcome.subject.takeIf { it.isNotBlank() },
                        dateLabel = null,
                        timeLabel = null,
                        classCode = outcome.classCode,
                        detailMessage = outcome.message,
                        showUpdateIdAction = true,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.Offline -> {
                    ResultLayout(
                        accentColor = OfflineSlate,
                        containerColor = OfflineContainer,
                        onContainerColor = OnOfflineContainer,
                        icon = Icons.Filled.WifiOff,
                        headline = stringResource(R.string.result_offline_title),
                        greeting = null,
                        subject = null,
                        dateLabel = null,
                        timeLabel = null,
                        classCode = outcome.classCode,
                        detailMessage = "${stringResource(R.string.result_offline_message)}\n${stringResource(R.string.retry_hint)}",
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.ServerEndpointNotReady -> {
                    ResultLayout(
                        accentColor = ErrorCrimson,
                        containerColor = ErrorContainer,
                        onContainerColor = OnErrorContainer,
                        icon = Icons.Filled.ErrorOutline,
                        headline = stringResource(R.string.result_error_title),
                        greeting = null,
                        subject = null,
                        dateLabel = null,
                        timeLabel = null,
                        classCode = outcome.classCode,
                        detailMessage = stringResource(R.string.result_server_html_message),
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
                is CheckInOutcome.Error -> {
                    ResultLayout(
                        accentColor = ErrorCrimson,
                        containerColor = ErrorContainer,
                        onContainerColor = OnErrorContainer,
                        icon = Icons.Filled.ErrorOutline,
                        headline = stringResource(R.string.result_error_title),
                        greeting = null,
                        subject = null,
                        dateLabel = null,
                        timeLabel = null,
                        classCode = outcome.classCode,
                        detailMessage = outcome.message,
                        showUpdateIdAction = false,
                        onDone = onDone,
                        onUpdateId = onUpdateId,
                        modifier = modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultLayout(
    accentColor: Color,
    containerColor: Color,
    onContainerColor: Color,
    icon: ImageVector,
    headline: String,
    greeting: String?,
    subject: String?,
    dateLabel: String?,
    timeLabel: String?,
    classCode: String?,
    detailMessage: String?,
    showUpdateIdAction: Boolean,
    onDone: () -> Unit,
    onUpdateId: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("checkin_result_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = accentColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = headline,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = headline,
                style = MaterialTheme.typography.displaySmall,
                color = onContainerColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("checkin_result_headline")
            )

            if (!greeting.isNullOrBlank()) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = onContainerColor,
                    textAlign = TextAlign.Center
                )
            }

            if (!subject.isNullOrBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.72f),
                    contentColor = onContainerColor,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!dateLabel.isNullOrBlank()) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!timeLabel.isNullOrBlank()) {
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (!timeLabel.isNullOrBlank() || !dateLabel.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(dateLabel, timeLabel).joinToString(" | "),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = onContainerColor,
                    textAlign = TextAlign.Center
                )
            }

            if (!detailMessage.isNullOrBlank()) {
                Text(
                    text = detailMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainerColor,
                    textAlign = TextAlign.Center
                )
            }

            if (!classCode.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.result_class_code_label, classCode),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = onContainerColor.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(4.dp))

            if (showUpdateIdAction) {
                OutlinedButton(
                    onClick = onUpdateId,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = onContainerColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("result_update_id_button")
                ) {
                    Text(stringResource(R.string.update_id_action))
                }
            }

            Button(
                onClick = onDone,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("done_button")
            ) {
                Text(
                    text = stringResource(R.string.done_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
