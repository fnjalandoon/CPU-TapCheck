package com.example

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceApiClient
import com.example.data.FeedbackHelper
import com.example.data.StudentPreferencesRepository
import com.example.data.TeacherApiClient
import com.example.data.TeacherPreferencesRepository
import com.example.ui.AttendanceScreen
import com.example.ui.AttendanceViewModel
import com.example.ui.NfcHardwareStatus
import com.example.ui.TeacherViewModel
import com.example.ui.theme.CpuTapCheckTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val feedbackHelper by lazy { FeedbackHelper(applicationContext) }

    private val viewModel: AttendanceViewModel by viewModels {
        AttendanceViewModel.Factory(
            preferencesRepository = StudentPreferencesRepository(applicationContext),
            api = AttendanceApiClient(applicationContext)
        )
    }

    private val teacherViewModel: TeacherViewModel by viewModels {
        TeacherViewModel.Factory(
            preferencesRepository = TeacherPreferencesRepository(applicationContext),
            api = TeacherApiClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        refreshNfcStatus()

        if (savedInstanceState == null) {
            viewModel.handleIncomingIntent(intent)
        }

        setContent {
            CpuTapCheckTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val teacherUiState by teacherViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    launch {
                        viewModel.feedbackEvents.collect { feedbackHelper.play(it) }
                    }
                    launch {
                        teacherViewModel.feedbackEvents.collect { feedbackHelper.play(it) }
                    }
                }

                AttendanceScreen(
                    uiState = uiState,
                    teacherUiState = teacherUiState,
                    onIdChanged = viewModel::onIdInputChanged,
                    onClassCodeChanged = viewModel::onClassCodeInputChanged,
                    onVerifyLookup = viewModel::verifyIdWithClassCode,
                    onRejectLookup = viewModel::rejectLookupResult,
                    onSaveId = viewModel::saveStudentId,
                    onStartEditId = viewModel::startEditingId,
                    onCancelEditId = viewModel::cancelEditingId,
                    onOpenNfcSettings = ::openNfcSettings,
                    onDismissResult = viewModel::dismissCheckInResult,
                    onUpdateIdFromNotFound = viewModel::openEditFromNotFound,
                    onOpenTeacherPortal = teacherViewModel::openPortal,
                    onCloseTeacherPortal = teacherViewModel::closePortal,
                    onTeacherLogin = teacherViewModel::login,
                    onTeacherChangeKey = teacherViewModel::changeKey,
                    onTeacherSignOut = teacherViewModel::logout,
                    onSelectTeacherTab = teacherViewModel::selectTab,
                    onRefreshTeacherDashboard = teacherViewModel::loadDashboard,
                    onOpenAddClass = teacherViewModel::openAddClassForm,
                    onOpenEditClass = teacherViewModel::openEditClassForm,
                    onCloseClassForm = teacherViewModel::closeClassForm,
                    onSaveClassForm = teacherViewModel::saveClassForm,
                    onOpenClassDetail = teacherViewModel::openClassDetail,
                    onCloseClassDetail = teacherViewModel::closeClassDetail,
                    onSelectClassDetailTab = teacherViewModel::selectClassDetailTab,
                    onRefreshToday = {
                        teacherViewModel.uiState.value.selectedClass?.code?.let { code ->
                            teacherViewModel.loadToday(code)
                        }
                    },
                    onManualMark = teacherViewModel::manualMarkPresent,
                    onRefreshRoster = {
                        teacherViewModel.uiState.value.selectedClass?.code?.let { code ->
                            teacherViewModel.loadRoster(code)
                        }
                    },
                    onAddStudents = teacherViewModel::addStudentsToClass,
                    onRemoveStudent = teacherViewModel::removeStudentFromClass,
                    onDismissRosterBanner = teacherViewModel::dismissRosterBanner,
                    onWritePayloadChanged = teacherViewModel::setWritePayloadCode,
                    onWriteFormatChanged = teacherViewModel::setWriteFormat,
                    onLockReadOnlyChanged = teacherViewModel::setLockReadOnly,
                    onArmWriter = teacherViewModel::armNfcWriter,
                    onAddTeacher = teacherViewModel::addTeacher,
                    onResetTeacherKey = teacherViewModel::resetTeacherKey,
                    onToggleTeacherActive = teacherViewModel::toggleTeacherActive,
                    onSaveAdminSettings = teacherViewModel::saveAdminSettings,
                    onDismissKeyBanner = teacherViewModel::dismissGeneratedKeyBanner
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNfcStatus()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        disableNfcForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (teacherViewModel.uiState.value.isWritingNfcNow) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                teacherViewModel.onTagDetectedForWrite(tag)
            }
        } else {
            viewModel.handleIncomingIntent(intent)
        }
    }

    private fun refreshNfcStatus() {
        val adapter = nfcAdapter
        val status = when {
            adapter == null -> NfcHardwareStatus.Unsupported
            adapter.isEnabled -> NfcHardwareStatus.Ready
            else -> NfcHardwareStatus.Disabled
        }
        viewModel.updateNfcStatus(status)
    }

    private fun enableNfcForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        runCatching {
            adapter.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    private fun disableNfcForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        runCatching {
            adapter.disableForegroundDispatch(this)
        }
    }

    private fun openNfcSettings() {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
    }
}
