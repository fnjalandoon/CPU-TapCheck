package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ClassInfoPayload
import com.example.data.ClassSummary
import com.example.data.NfcWriteFormat
import com.example.data.RosterRowPayload
import com.example.ui.components.CheckInResultCard
import com.example.ui.components.ReadyToTapSection
import com.example.ui.components.SetupIdSection
import com.example.ui.teacher.TeacherPortalScreen
import com.example.ui.theme.LateContainer
import com.example.ui.theme.OnLateContainer
import com.example.ui.theme.OnPresentContainer
import com.example.ui.theme.PresentContainer

private enum class ActivePane {
    TeacherPortal,
    LoadingProfile,
    SetupOrEditId,
    CheckInResult,
    ReadyToTap
}

@Composable
fun AttendanceScreen(
    uiState: AttendanceUiState,
    teacherUiState: TeacherUiState,
    onIdChanged: (String) -> Unit,
    onClassCodeChanged: (String) -> Unit,
    onVerifyLookup: () -> Unit,
    onRejectLookup: () -> Unit,
    onSaveId: (displayName: String, givenName: String) -> Unit,
    onStartEditId: () -> Unit,
    onCancelEditId: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onDismissResult: () -> Unit,
    onUpdateIdFromNotFound: () -> Unit,
    onOpenTeacherPortal: () -> Unit,
    onCloseTeacherPortal: () -> Unit,
    onTeacherLogin: (String, String) -> Unit,
    onTeacherChangeKey: (String, String) -> Unit,
    onTeacherSignOut: () -> Unit,
    onSelectTeacherTab: (TeacherTab) -> Unit,
    onRefreshTeacherDashboard: () -> Unit,
    onOpenAddClass: () -> Unit,
    onOpenEditClass: (ClassSummary) -> Unit,
    onCloseClassForm: () -> Unit,
    onSaveClassForm: (ClassInfoPayload, List<RosterRowPayload>) -> Unit,
    onOpenClassDetail: (ClassSummary) -> Unit,
    onCloseClassDetail: () -> Unit,
    onSelectClassDetailTab: (ClassDetailTab) -> Unit,
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
    onWritePayloadChanged: (String) -> Unit,
    onWriteFormatChanged: (NfcWriteFormat) -> Unit,
    onLockReadOnlyChanged: (Boolean) -> Unit,
    onArmWriter: () -> Unit,
    onAddTeacher: (String, String) -> Unit,
    onResetTeacherKey: (String) -> Unit,
    onToggleTeacherActive: (String, Boolean) -> Unit,
    onSaveAdminSettings: (Int, String) -> Unit,
    onDismissKeyBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePane = when {
        teacherUiState.isPortalOpen -> ActivePane.TeacherPortal
        !uiState.isProfileLoaded -> ActivePane.LoadingProfile
        !uiState.profile.hasSavedId || uiState.isEditingId -> ActivePane.SetupOrEditId
        uiState.checkInState !is CheckInUiState.Idle -> ActivePane.CheckInResult
        else -> ActivePane.ReadyToTap
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TopBrandHeader(
                        nfcStatus = uiState.nfcStatus,
                        showStaffButton = !teacherUiState.isPortalOpen,
                        onOpenTeacherPortal = onOpenTeacherPortal
                    )

                    AnimatedContent(
                        targetState = activePane,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                        },
                        label = "attendance_pane_transition"
                    ) { pane ->
                        when (pane) {
                            ActivePane.TeacherPortal -> {
                                TeacherPortalScreen(
                                    uiState = teacherUiState,
                                    nfcStatus = uiState.nfcStatus,
                                    onClosePortal = onCloseTeacherPortal,
                                    onLogin = onTeacherLogin,
                                    onChangeKey = onTeacherChangeKey,
                                    onSignOut = onTeacherSignOut,
                                    onSelectTab = onSelectTeacherTab,
                                    onRefreshDashboard = onRefreshTeacherDashboard,
                                    onOpenAddClass = onOpenAddClass,
                                    onOpenEditClass = onOpenEditClass,
                                    onCloseClassForm = onCloseClassForm,
                                    onSaveClassForm = onSaveClassForm,
                                    onOpenClassDetail = onOpenClassDetail,
                                    onCloseClassDetail = onCloseClassDetail,
                                    onSelectClassDetailTab = onSelectClassDetailTab,
                                    onRefreshToday = onRefreshToday,
                                    onSelectTodayDate = onSelectTodayDate,
                                    onResetTodayToCurrent = onResetTodayToCurrent,
                                    onStepTodayOlder = onStepTodayOlder,
                                    onStepTodayNewer = onStepTodayNewer,
                                    onManualMark = onManualMark,
                                    onRefreshRoster = onRefreshRoster,
                                    onAddStudents = onAddStudents,
                                    onRemoveStudent = onRemoveStudent,
                                    onDismissRosterBanner = onDismissRosterBanner,
                                    onWritePayloadChanged = onWritePayloadChanged,
                                    onWriteFormatChanged = onWriteFormatChanged,
                                    onLockReadOnlyChanged = onLockReadOnlyChanged,
                                    onArmWriter = onArmWriter,
                                    onAddTeacher = onAddTeacher,
                                    onResetKey = onResetTeacherKey,
                                    onToggleActive = onToggleTeacherActive,
                                    onSaveSettings = onSaveAdminSettings,
                                    onDismissKeyBanner = onDismissKeyBanner
                                )
                            }
                            ActivePane.LoadingProfile -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            ActivePane.SetupOrEditId -> {
                                SetupIdSection(
                                    idInput = uiState.idInput,
                                    idValidationError = uiState.idValidationError,
                                    classCodeInput = uiState.classCodeInput,
                                    pendingClassCode = uiState.pendingClassCode,
                                    lookupState = uiState.lookupState,
                                    canCancel = uiState.profile.hasSavedId && uiState.isEditingId,
                                    onIdChanged = onIdChanged,
                                    onClassCodeChanged = onClassCodeChanged,
                                    onVerifyLookup = onVerifyLookup,
                                    onRejectLookup = onRejectLookup,
                                    onSaveId = onSaveId,
                                    onCancel = onCancelEditId
                                )
                            }
                            ActivePane.CheckInResult -> {
                                CheckInResultCard(
                                    checkInState = uiState.checkInState,
                                    onDone = onDismissResult,
                                    onUpdateId = onUpdateIdFromNotFound
                                )
                            }
                            ActivePane.ReadyToTap -> {
                                ReadyToTapSection(
                                    profile = uiState.profile,
                                    hasCheckedInToday = uiState.hasCheckedInToday,
                                    nfcStatus = uiState.nfcStatus,
                                    onEditId = onStartEditId,
                                    onOpenNfcSettings = onOpenNfcSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBrandHeader(
    nfcStatus: NfcHardwareStatus,
    showStaffButton: Boolean,
    onOpenTeacherPortal: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.brand_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val (badgeContainer, badgeTextColor, badgeLabel) = when (nfcStatus) {
            NfcHardwareStatus.Ready -> Triple(
                PresentContainer,
                OnPresentContainer,
                stringResource(R.string.nfc_status_ready)
            )
            NfcHardwareStatus.Disabled -> Triple(
                LateContainer,
                OnLateContainer,
                stringResource(R.string.nfc_status_off)
            )
            NfcHardwareStatus.Unsupported -> Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                stringResource(R.string.nfc_status_unavailable)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = badgeContainer,
                contentColor = badgeTextColor,
                shape = CircleShape,
                modifier = Modifier.testTag("nfc_status_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Nfc,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showStaffButton) {
                IconButton(
                    onClick = onOpenTeacherPortal,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("teacher_portal_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Teacher or Admin sign in",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

