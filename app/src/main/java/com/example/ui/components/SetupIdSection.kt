package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.LookupUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer
import com.example.ui.theme.PresentGreen

@Composable
fun SetupIdSection(
    idInput: String,
    idValidationError: String?,
    classCodeInput: String,
    pendingClassCode: String?,
    lookupState: LookupUiState,
    canCancel: Boolean,
    onIdChanged: (String) -> Unit,
    onClassCodeChanged: (String) -> Unit,
    onVerifyLookup: () -> Unit,
    onRejectLookup: () -> Unit,
    onSaveId: (displayName: String, givenName: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (canCancel) {
        BackHandler(onBack = onCancel)
    }

    var showLookupSection by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!pendingClassCode.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.setup_pending_tag_banner, pendingClassCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.setup_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.setup_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = idInput,
                    onValueChange = onIdChanged,
                    label = { Text(stringResource(R.string.student_id_label)) },
                    placeholder = { Text(stringResource(R.string.student_id_placeholder)) },
                    supportingText = {
                        Text(
                            text = idValidationError ?: stringResource(R.string.student_id_helper),
                            color = if (idValidationError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    isError = idValidationError != null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSaveId("", "") }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_id_input")
                )

                Button(
                    onClick = { onSaveId("", "") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("save_id_button")
                ) {
                    Text(
                        text = stringResource(R.string.save_id_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("cancel_edit_id_button")
                    ) {
                        Text(stringResource(R.string.cancel_edit_button))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )

                TextButton(
                    onClick = { showLookupSection = !showLookupSection },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_verify_lookup_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.verify_section_title),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showLookupSection) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = showLookupSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.verify_section_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = classCodeInput,
                            onValueChange = onClassCodeChanged,
                            label = { Text(stringResource(R.string.class_code_label)) },
                            placeholder = { Text(stringResource(R.string.class_code_placeholder)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { onVerifyLookup() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lookup_class_code_input")
                        )

                        OutlinedButton(
                            onClick = onVerifyLookup,
                            enabled = lookupState !is LookupUiState.Loading,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("lookup_verify_button")
                        ) {
                            if (lookupState is LookupUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.verifying_button))
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.verify_button))
                            }
                        }

                        when (lookupState) {
                            is LookupUiState.Found -> {
                                Surface(
                                    color = PresentContainer,
                                    contentColor = OnPresentContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, PresentGreen.copy(alpha = 0.4f)),
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
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = PresentGreen
                                            )
                                            Text(
                                                text = stringResource(R.string.verify_confirm_title),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        Text(
                                            text = lookupState.displayName,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        if (lookupState.subject.isNotBlank()) {
                                            Text(
                                                text = lookupState.subject,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = onRejectLookup,
                                                border = BorderStroke(1.dp, PresentGreen),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = OnPresentContainer
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("lookup_reject_button")
                                            ) {
                                                Text(stringResource(R.string.verify_confirm_no))
                                            }
                                            Button(
                                                onClick = {
                                                    onSaveId(
                                                        lookupState.displayName,
                                                        lookupState.givenName
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = PresentGreen,
                                                    contentColor = androidx.compose.ui.graphics.Color.White
                                                ),
                                                modifier = Modifier
                                                    .weight(1.4f)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("lookup_confirm_save_button")
                                            ) {
                                                Text(stringResource(R.string.verify_confirm_yes))
                                            }
                                        }
                                    }
                                }
                            }
                            is LookupUiState.Message -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lookupState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
