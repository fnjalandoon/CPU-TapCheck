package com.example.ui

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdminSettingsResponse
import com.example.data.ApiCallResult
import com.example.data.ClassInfoPayload
import com.example.data.ClassSummary
import com.example.data.FeedbackType
import com.example.data.NfcTagWriter
import com.example.data.NfcWriteFormat
import com.example.data.NfcWriteResult
import com.example.data.RosterRowPayload
import com.example.data.TeacherAccountItem
import com.example.data.TeacherApiClient
import com.example.data.TeacherPreferencesRepository
import com.example.data.TeacherSession
import com.example.data.TodayResponse
import com.example.data.TotalsResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TeacherTab {
    Classes,
    Admin
}

enum class ClassDetailTab {
    Today,
    Roster,
    Totals,
    WriteNfc
}

data class TeacherUiState(
    val isPortalOpen: Boolean = false,
    val session: TeacherSession = TeacherSession(),
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val isLoadingInit: Boolean = false,
    val dashboardMessage: String? = null,
    val activeTab: TeacherTab = TeacherTab.Classes,
    val classes: List<ClassSummary> = emptyList(),
    val threshold: Int = 80,
    // Selected class detail
    val selectedClass: ClassSummary? = null,
    val classDetailTab: ClassDetailTab = ClassDetailTab.Today,
    val isLoadingToday: Boolean = false,
    val todayData: TodayResponse? = null,
    val todayError: String? = null,
    val markingStudentId: String? = null,
    val isLoadingRoster: Boolean = false,
    val rosterRows: List<RosterRowPayload> = emptyList(),
    val rosterError: String? = null,
    val rosterBanner: String? = null,
    val isSavingRoster: Boolean = false,
    val removingStudentId: String? = null,
    val isLoadingTotals: Boolean = false,
    val totalsData: TotalsResponse? = null,
    val totalsError: String? = null,
    // NFC tag writer
    val isWaitingToWriteNfc: Boolean = false,
    val writeFormat: NfcWriteFormat = NfcWriteFormat.CompactText,
    val lockReadOnly: Boolean = false,
    val writePayloadCode: String = "",
    val nfcWriteResult: NfcWriteResult? = null,
    // Add / Edit class modal
    val showClassForm: Boolean = false,
    val editingClass: ClassSummary? = null,
    val isSavingClass: Boolean = false,
    val classFormError: String? = null,
    // Admin teachers & settings
    val isLoadingAdmin: Boolean = false,
    val teachers: List<TeacherAccountItem> = emptyList(),
    val generatedTempKeyBanner: String? = null,
    val adminSettings: AdminSettingsResponse? = null,
    val adminMessage: String? = null,
    val isSavingAdmin: Boolean = false
) {
    val isWritingNfcNow: Boolean
        get() = isPortalOpen && selectedClass != null && classDetailTab == ClassDetailTab.WriteNfc && isWaitingToWriteNfc
}

class TeacherViewModel(
    private val preferencesRepository: TeacherPreferencesRepository,
    private val api: TeacherApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<FeedbackType>(extraBufferCapacity = 4)
    val feedbackEvents: SharedFlow<FeedbackType> = _feedbackEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.sessionFlow.collect { session ->
                val wasLoggedIn = _uiState.value.session.isLoggedIn
                _uiState.update { it.copy(session = session) }
                if (session.isLoggedIn && !wasLoggedIn && _uiState.value.isPortalOpen) {
                    loadDashboard()
                }
            }
        }
    }

    fun openPortal() {
        _uiState.update {
            it.copy(
                isPortalOpen = true,
                loginError = null,
                dashboardMessage = null
            )
        }
        if (_uiState.value.session.isLoggedIn) {
            loadDashboard()
        }
    }

    fun closePortal() {
        _uiState.update {
            it.copy(
                isPortalOpen = false,
                selectedClass = null,
                isWaitingToWriteNfc = false,
                nfcWriteResult = null,
                showClassForm = false,
                editingClass = null
            )
        }
    }

    fun login(username: String, key: String) {
        if (username.isBlank() || key.isBlank()) {
            _uiState.update { it.copy(loginError = "Enter your username and key.") }
            return
        }
        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
        viewModelScope.launch {
            when (val res = api.login(username, key)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.ok && !data.token.isNullOrBlank() && data.user != null) {
                        preferencesRepository.saveSession(data.token, data.user)
                        _uiState.update {
                            it.copy(
                                isLoggingIn = false,
                                loginError = null,
                                activeTab = TeacherTab.Classes
                            )
                        }
                        if (!data.user.mustChange) {
                            loadDashboard()
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoggingIn = false,
                                loginError = data.msg ?: "Could not log in."
                            )
                        }
                    }
                }
            }
        }
    }

    fun changeKey(oldKey: String, newKey: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        if (newKey.length < 6) {
            _uiState.update { it.copy(loginError = "New key must be at least 6 characters.") }
            return
        }
        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
        viewModelScope.launch {
            when (val res = api.changeKey(token, oldKey, newKey)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.ok && !data.token.isNullOrBlank() && data.user != null) {
                        preferencesRepository.saveSession(data.token, data.user)
                        _uiState.update { it.copy(isLoggingIn = false, loginError = null) }
                        loadDashboard()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoggingIn = false,
                                loginError = data.msg ?: "Could not update key."
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesRepository.clearSession()
            _uiState.update {
                TeacherUiState(isPortalOpen = it.isPortalOpen)
            }
        }
    }

    fun selectTab(tab: TeacherTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == TeacherTab.Admin && _uiState.value.teachers.isEmpty()) {
            loadAdminData()
        }
    }

    fun loadDashboard() {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isLoadingInit = true, dashboardMessage = null) }
        viewModelScope.launch {
            when (val res = api.init(token)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isLoadingInit = false, dashboardMessage = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.auth) {
                        preferencesRepository.clearSession()
                        _uiState.update {
                            it.copy(isLoadingInit = false, loginError = data.msg ?: "Session expired. Please log in again.")
                        }
                        return@launch
                    }
                    if (data.ok) {
                        if (data.user != null) {
                            preferencesRepository.saveSession(token, data.user)
                        }
                        val updatedClasses = data.classes.orEmpty()
                        _uiState.update { state ->
                            val refreshedSelected = state.selectedClass?.let { sel ->
                                updatedClasses.find { it.code == sel.code } ?: sel
                            }
                            state.copy(
                                isLoadingInit = false,
                                classes = updatedClasses,
                                selectedClass = refreshedSelected,
                                threshold = data.threshold,
                                dashboardMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoadingInit = false, dashboardMessage = data.msg ?: "Could not load classes.")
                        }
                    }
                }
            }
        }
    }

    fun openClassDetail(cls: ClassSummary) {
        _uiState.update {
            it.copy(
                selectedClass = cls,
                classDetailTab = ClassDetailTab.Today,
                todayData = null,
                todayError = null,
                rosterRows = emptyList(),
                rosterError = null,
                rosterBanner = null,
                totalsData = null,
                totalsError = null,
                isWaitingToWriteNfc = false,
                writePayloadCode = cls.shortSubjectCode.ifBlank { cls.code },
                nfcWriteResult = null
            )
        }
        loadToday(cls.code)
    }

    fun closeClassDetail() {
        _uiState.update {
            it.copy(
                selectedClass = null,
                isWaitingToWriteNfc = false,
                nfcWriteResult = null
            )
        }
    }

    fun selectClassDetailTab(tab: ClassDetailTab) {
        val cls = _uiState.value.selectedClass ?: return
        _uiState.update {
            it.copy(
                classDetailTab = tab,
                isWaitingToWriteNfc = tab == ClassDetailTab.WriteNfc,
                nfcWriteResult = if (tab == ClassDetailTab.WriteNfc) null else it.nfcWriteResult
            )
        }
        when (tab) {
            ClassDetailTab.Today -> loadToday(cls.code)
            ClassDetailTab.Roster -> loadRoster(cls.code)
            ClassDetailTab.Totals -> loadTotals(cls.code)
            ClassDetailTab.WriteNfc -> Unit
        }
    }

    fun loadToday(code: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isLoadingToday = true, todayError = null) }
        viewModelScope.launch {
            when (val res = api.today(token, code)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isLoadingToday = false, todayError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.auth) {
                        preferencesRepository.clearSession()
                        _uiState.update { it.copy(isLoadingToday = false, loginError = data.msg) }
                    } else if (data.ok) {
                        _uiState.update { it.copy(isLoadingToday = false, todayData = data) }
                    } else {
                        _uiState.update {
                            it.copy(isLoadingToday = false, todayError = data.msg ?: "Could not load today's attendance.")
                        }
                    }
                }
            }
        }
    }

    fun manualMarkPresent(studentId: String) {
        val token = _uiState.value.session.token
        val cls = _uiState.value.selectedClass ?: return
        if (token.isBlank()) return
        _uiState.update { it.copy(markingStudentId = studentId) }
        viewModelScope.launch {
            when (val res = api.manualMark(token, cls.code, studentId)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(markingStudentId = null, todayError = res.message) }
                }
                is ApiCallResult.Success -> {
                    _uiState.update { it.copy(markingStudentId = null) }
                    if (res.data.ok) {
                        _feedbackEvents.tryEmit(FeedbackType.Success)
                        loadToday(cls.code)
                    } else {
                        _uiState.update { it.copy(todayError = res.data.msg ?: "Could not mark student.") }
                    }
                }
            }
        }
    }

    fun loadRoster(code: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isLoadingRoster = true, rosterError = null) }
        viewModelScope.launch {
            when (val res = api.roster(token, code)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isLoadingRoster = false, rosterError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.auth) {
                        preferencesRepository.clearSession()
                        _uiState.update { it.copy(isLoadingRoster = false, loginError = data.msg) }
                    } else if (data.ok) {
                        _uiState.update { it.copy(isLoadingRoster = false, rosterRows = data.rows) }
                    } else {
                        _uiState.update {
                            it.copy(isLoadingRoster = false, rosterError = data.msg ?: "Could not load class roster.")
                        }
                    }
                }
            }
        }
    }

    fun addStudentsToClass(rows: List<RosterRowPayload>) {
        val token = _uiState.value.session.token
        val cls = _uiState.value.selectedClass ?: return
        if (token.isBlank()) return
        if (rows.isEmpty()) {
            _uiState.update { it.copy(rosterError = "Add at least one student with an ID number and Name.") }
            return
        }
        _uiState.update { it.copy(isSavingRoster = true, rosterError = null, rosterBanner = null) }
        viewModelScope.launch {
            when (val res = api.rosterAdd(token, cls.code, rows)) {
                is ApiCallResult.Failure -> {
                    _feedbackEvents.tryEmit(FeedbackType.Error)
                    _uiState.update { it.copy(isSavingRoster = false, rosterError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.auth) {
                        preferencesRepository.clearSession()
                        _uiState.update { it.copy(isSavingRoster = false, loginError = data.msg) }
                    } else if (data.ok) {
                        _feedbackEvents.tryEmit(FeedbackType.Success)
                        val skippedNote = data.skipped.orEmpty()
                            .takeIf { it.isNotEmpty() }
                            ?.let { " Skipped (${it.size}): ${it.take(3).joinToString("; ")}" }
                            .orEmpty()
                        _uiState.update {
                            it.copy(
                                isSavingRoster = false,
                                rosterBanner = "Added ${data.added} student(s) to ${cls.subject}.$skippedNote"
                            )
                        }
                        loadRoster(cls.code)
                        loadDashboard()
                    } else {
                        _feedbackEvents.tryEmit(FeedbackType.Error)
                        _uiState.update {
                            it.copy(isSavingRoster = false, rosterError = data.msg ?: "Could not add students.")
                        }
                    }
                }
            }
        }
    }

    fun removeStudentFromClass(studentId: String) {
        val token = _uiState.value.session.token
        val cls = _uiState.value.selectedClass ?: return
        if (token.isBlank()) return
        _uiState.update { it.copy(removingStudentId = studentId, rosterError = null) }
        viewModelScope.launch {
            when (val res = api.rosterRemove(token, cls.code, studentId)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(removingStudentId = null, rosterError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    _uiState.update { it.copy(removingStudentId = null) }
                    if (data.auth) {
                        preferencesRepository.clearSession()
                        _uiState.update { it.copy(loginError = data.msg) }
                    } else if (data.ok) {
                        _uiState.update { it.copy(rosterBanner = "Removed student ($studentId) from roster.") }
                        loadRoster(cls.code)
                        loadDashboard()
                    } else {
                        _uiState.update { it.copy(rosterError = data.msg ?: "Could not remove student.") }
                    }
                }
            }
        }
    }

    fun dismissRosterBanner() {
        _uiState.update { it.copy(rosterBanner = null, rosterError = null) }
    }

    fun loadTotals(code: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isLoadingTotals = true, totalsError = null) }
        viewModelScope.launch {
            when (val res = api.totals(token, code)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isLoadingTotals = false, totalsError = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.auth) {
                        preferencesRepository.clearSession()
                        _uiState.update { it.copy(isLoadingTotals = false, loginError = data.msg) }
                    } else if (data.ok) {
                        _uiState.update { it.copy(isLoadingTotals = false, totalsData = data) }
                    } else {
                        _uiState.update {
                            it.copy(isLoadingTotals = false, totalsError = data.msg ?: "Could not load totals.")
                        }
                    }
                }
            }
        }
    }

    // NFC Writer controls
    fun setWriteFormat(format: NfcWriteFormat) {
        _uiState.update {
            it.copy(
                writeFormat = format,
                isWaitingToWriteNfc = true,
                nfcWriteResult = null
            )
        }
    }

    fun setWritePayloadCode(code: String) {
        _uiState.update {
            it.copy(
                writePayloadCode = code,
                isWaitingToWriteNfc = true,
                nfcWriteResult = null
            )
        }
    }

    fun setLockReadOnly(lock: Boolean) {
        _uiState.update { it.copy(lockReadOnly = lock) }
    }

    fun armNfcWriter() {
        _uiState.update { it.copy(isWaitingToWriteNfc = true, nfcWriteResult = null) }
    }

    fun onTagDetectedForWrite(tag: Tag) {
        val state = _uiState.value
        if (!state.isWritingNfcNow) return
        val result = NfcTagWriter.write(
            tag = tag,
            subjectCode = state.writePayloadCode,
            format = state.writeFormat,
            lockReadOnly = state.lockReadOnly
        )
        when (result) {
            is NfcWriteResult.Success -> _feedbackEvents.tryEmit(FeedbackType.Success)
            is NfcWriteResult.Failure -> _feedbackEvents.tryEmit(FeedbackType.Error)
        }
        _uiState.update {
            it.copy(
                isWaitingToWriteNfc = result is NfcWriteResult.Failure,
                nfcWriteResult = result
            )
        }
    }

    // Add / Edit class
    fun openAddClassForm() {
        _uiState.update {
            it.copy(
                showClassForm = true,
                editingClass = null,
                classFormError = null
            )
        }
    }

    fun openEditClassForm(cls: ClassSummary) {
        _uiState.update {
            it.copy(
                showClassForm = true,
                editingClass = cls,
                classFormError = null
            )
        }
    }

    fun closeClassForm() {
        _uiState.update {
            it.copy(
                showClassForm = false,
                editingClass = null,
                classFormError = null
            )
        }
    }

    fun saveClassForm(
        info: ClassInfoPayload,
        rosterRows: List<RosterRowPayload>
    ) {
        val token = _uiState.value.session.token
        val editing = _uiState.value.editingClass
        if (token.isBlank()) return

        _uiState.update { it.copy(isSavingClass = true, classFormError = null) }
        viewModelScope.launch {
            if (editing == null) {
                if (rosterRows.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isSavingClass = false,
                            classFormError = "Add at least one student (ID and Name) via CSV file or paste."
                        )
                    }
                    return@launch
                }
                when (val res = api.addClass(token, info, rosterRows)) {
                    is ApiCallResult.Failure -> {
                        _uiState.update { it.copy(isSavingClass = false, classFormError = res.message) }
                    }
                    is ApiCallResult.Success -> {
                        if (res.data.ok) {
                            _feedbackEvents.tryEmit(FeedbackType.Success)
                            _uiState.update {
                                it.copy(
                                    isSavingClass = false,
                                    showClassForm = false,
                                    dashboardMessage = "Class added (${res.data.code}). ${res.data.added} students enrolled."
                                )
                            }
                            loadDashboard()
                        } else {
                            _uiState.update {
                                it.copy(
                                    isSavingClass = false,
                                    classFormError = res.data.msg ?: "Could not add class."
                                )
                            }
                        }
                    }
                }
            } else {
                when (val res = api.updateClass(token, editing.code, info)) {
                    is ApiCallResult.Failure -> {
                        _uiState.update { it.copy(isSavingClass = false, classFormError = res.message) }
                    }
                    is ApiCallResult.Success -> {
                        if (res.data.ok) {
                            _feedbackEvents.tryEmit(FeedbackType.Success)
                            _uiState.update {
                                it.copy(
                                    isSavingClass = false,
                                    showClassForm = false,
                                    editingClass = null,
                                    dashboardMessage = "Updated ${info.subject}."
                                )
                            }
                            loadDashboard()
                        } else {
                            _uiState.update {
                                it.copy(
                                    isSavingClass = false,
                                    classFormError = res.data.msg ?: "Could not update class."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Admin Teachers & Settings
    fun loadAdminData() {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isLoadingAdmin = true, adminMessage = null) }
        viewModelScope.launch {
            val tRes = api.teachers(token)
            val sRes = api.getSettings(token)
            _uiState.update { current ->
                var msg = current.adminMessage
                val teachers = when (tRes) {
                    is ApiCallResult.Success -> if (tRes.data.ok) tRes.data.teachers else {
                        msg = tRes.data.msg
                        current.teachers
                    }
                    is ApiCallResult.Failure -> {
                        msg = tRes.message
                        current.teachers
                    }
                }
                val settings = when (sRes) {
                    is ApiCallResult.Success -> if (sRes.data.ok) sRes.data else current.adminSettings
                    is ApiCallResult.Failure -> current.adminSettings
                }
                current.copy(
                    isLoadingAdmin = false,
                    teachers = teachers,
                    adminSettings = settings,
                    adminMessage = msg
                )
            }
        }
    }

    fun addTeacher(username: String, name: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isSavingAdmin = true, adminMessage = null, generatedTempKeyBanner = null) }
        viewModelScope.launch {
            when (val res = api.addTeacher(token, username, name)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isSavingAdmin = false, adminMessage = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.ok) {
                        _feedbackEvents.tryEmit(FeedbackType.Success)
                        _uiState.update {
                            it.copy(
                                isSavingAdmin = false,
                                generatedTempKeyBanner = "Teacher ${data.name} (${data.username}) created. Temporary key: ${data.tempKey}"
                            )
                        }
                        loadAdminData()
                    } else {
                        _uiState.update {
                            it.copy(isSavingAdmin = false, adminMessage = data.msg ?: "Could not add teacher.")
                        }
                    }
                }
            }
        }
    }

    fun resetTeacherKey(username: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isSavingAdmin = true, adminMessage = null, generatedTempKeyBanner = null) }
        viewModelScope.launch {
            when (val res = api.resetKey(token, username)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isSavingAdmin = false, adminMessage = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    if (data.ok) {
                        _uiState.update {
                            it.copy(
                                isSavingAdmin = false,
                                generatedTempKeyBanner = "New temporary key for ${data.username}: ${data.tempKey}"
                            )
                        }
                        loadAdminData()
                    } else {
                        _uiState.update { it.copy(isSavingAdmin = false, adminMessage = data.msg) }
                    }
                }
            }
        }
    }

    fun toggleTeacherActive(username: String, active: Boolean) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isSavingAdmin = true, adminMessage = null) }
        viewModelScope.launch {
            when (val res = api.setActive(token, username, active)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isSavingAdmin = false, adminMessage = res.message) }
                }
                is ApiCallResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingAdmin = false,
                            adminMessage = if (!res.data.ok) res.data.msg else null
                        )
                    }
                    if (res.data.ok) loadAdminData()
                }
            }
        }
    }

    fun saveAdminSettings(threshold: Int, webAppUrl: String) {
        val token = _uiState.value.session.token
        if (token.isBlank()) return
        _uiState.update { it.copy(isSavingAdmin = true, adminMessage = null) }
        viewModelScope.launch {
            when (val res = api.saveSettings(token, threshold, webAppUrl)) {
                is ApiCallResult.Failure -> {
                    _uiState.update { it.copy(isSavingAdmin = false, adminMessage = res.message) }
                }
                is ApiCallResult.Success -> {
                    val data = res.data
                    _uiState.update {
                        it.copy(
                            isSavingAdmin = false,
                            adminMessage = if (data.ok) "Settings saved." else (data.msg ?: "Could not save settings.")
                        )
                    }
                    if (data.ok) loadAdminData()
                }
            }
        }
    }

    fun dismissGeneratedKeyBanner() {
        _uiState.update { it.copy(generatedTempKeyBanner = null) }
    }

    class Factory(
        private val preferencesRepository: TeacherPreferencesRepository,
        private val api: TeacherApiClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TeacherViewModel(preferencesRepository, api) as T
        }
    }
}
