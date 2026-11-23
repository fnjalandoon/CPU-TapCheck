package com.example.ui.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ClassInfoPayload
import com.example.data.ClassSummary
import com.example.data.NfcWriteFormat
import com.example.data.RosterRowPayload
import com.example.ui.ClassDetailTab
import com.example.ui.NfcHardwareStatus
import com.example.ui.TeacherTab
import com.example.ui.TeacherUiState
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPortalScreen(
    uiState: TeacherUiState,
    nfcStatus: NfcHardwareStatus,
    onClosePortal: () -> Unit,
    onLogin: (String, String) -> Unit,
    onChangeKey: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSelectTab: (TeacherTab) -> Unit,
    onRefreshDashboard: () -> Unit,
    onOpenAddClass: () -> Unit,
    onOpenEditClass: (ClassSummary) -> Unit,
    onCloseClassForm: () -> Unit,
    onSaveClassForm: (ClassInfoPayload, List<RosterRowPayload>) -> Unit,
    onOpenClassDetail: (ClassSummary) -> Unit,
    onCloseClassDetail: () -> Unit,
    onSelectClassDetailTab: (ClassDetailTab) -> Unit,
    onRefreshToday: () -> Unit,
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
    onResetKey: (String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onSaveSettings: (Int, String) -> Unit,
    onDismissKeyBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClosePortal)

    val user = uiState.session.user

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onClosePortal) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Student Tap"
                    )
                }
                Column {
                    Text(
                        text = if (user != null) user.name else "Teacher / Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (user != null) {
                            "${user.username} (${user.role})"
                        } else {
                            "Student check-in resumes when you exit"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.session.isLoggedIn) {
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out"
                    )
                }
            }
        }

        when {
            !uiState.session.isLoggedIn -> {
                TeacherLoginSection(
                    isLoading = uiState.isLoggingIn,
                    error = uiState.loginError,
                    onLogin = onLogin
                )
            }
            user?.mustChange == true -> {
                ChangeKeySection(
                    username = user.username,
                    isLoading = uiState.isLoggingIn,
                    error = uiState.loginError,
                    onChangeKey = onChangeKey,
                    onSignOut = onSignOut
                )
            }
            uiState.showClassForm -> {
                AddEditClassSheet(
                    editingClass = uiState.editingClass,
                    isSaving = uiState.isSavingClass,
                    error = uiState.classFormError,
                    onSave = onSaveClassForm,
                    onCancel = onCloseClassForm
                )
            }
            uiState.selectedClass != null -> {
                ClassDetailSheet(
                    cls = uiState.selectedClass,
                    uiState = uiState,
                    nfcStatus = nfcStatus,
                    onBack = onCloseClassDetail,
                    onSelectTab = onSelectClassDetailTab,
                    onRefreshToday = onRefreshToday,
                    onManualMark = onManualMark,
                    onRefreshRoster = onRefreshRoster,
                    onAddStudents = onAddStudents,
                    onRemoveStudent = onRemoveStudent,
                    onDismissRosterBanner = onDismissRosterBanner,
                    onEditClass = onOpenEditClass,
                    onWritePayloadChanged = onWritePayloadChanged,
                    onWriteFormatChanged = onWriteFormatChanged,
                    onLockReadOnlyChanged = onLockReadOnlyChanged,
                    onArmWriter = onArmWriter
                )
            }
            else -> {
                if (user?.isAdmin == true) {
                    val tabs = listOf(TeacherTab.Classes to "Classes", TeacherTab.Admin to "Admin")
                    PrimaryTabRow(
                        selectedTabIndex = tabs.indexOfFirst { it.first == uiState.activeTab }.coerceAtLeast(0)
                    ) {
                        tabs.forEach { (tab, label) ->
                            Tab(
                                selected = uiState.activeTab == tab,
                                onClick = { onSelectTab(tab) },
                                text = { Text(label) }
                            )
                        }
                    }
                }

                when (uiState.activeTab) {
                    TeacherTab.Classes -> ClassesDashboardSection(
                        uiState = uiState,
                        onRefresh = onRefreshDashboard,
                        onOpenAddClass = onOpenAddClass,
                        onOpenClassDetail = onOpenClassDetail
                    )
                    TeacherTab.Admin -> AdminSection(
                        uiState = uiState,
                        onAddTeacher = onAddTeacher,
                        onResetKey = onResetKey,
                        onToggleActive = onToggleActive,
                        onSaveSettings = onSaveSettings,
                        onDismissKeyBanner = onDismissKeyBanner
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassesDashboardSection(
    uiState: TeacherUiState,
    onRefresh: () -> Unit,
    onOpenAddClass: () -> Unit,
    onOpenClassDetail: (ClassSummary) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenAddClass,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("add_class_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Class")
            }
            OutlinedButton(
                onClick = onRefresh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh classes", modifier = Modifier.size(18.dp))
            }
        }

        if (!uiState.dashboardMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.dashboardMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (uiState.isLoadingInit) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.classes.isEmpty()) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No Classes Yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Tap Add Class above to create your first subject and import a CSV student roster.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            uiState.classes.forEach { cls ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenClassDetail(cls) }
                        .testTag("class_card_${cls.code}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cls.subject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cls.schedule,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Code: ${cls.code} | ${cls.students} students",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Open class",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
