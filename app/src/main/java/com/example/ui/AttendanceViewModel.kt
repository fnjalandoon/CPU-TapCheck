package com.example.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceApi
import com.example.data.CheckInOutcome
import com.example.data.FeedbackType
import com.example.data.LookupOutcome
import com.example.data.NfcTagParser
import com.example.data.SavedStudentProfile
import com.example.data.StudentPreferencesRepository
import com.example.data.TagParseResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class NfcHardwareStatus {
    Ready,
    Disabled,
    Unsupported
}

sealed interface CheckInUiState {
    data object Idle : CheckInUiState
    data class Loading(val classCode: String) : CheckInUiState
    data class Result(val outcome: CheckInOutcome) : CheckInUiState
    data object InvalidTag : CheckInUiState
}

sealed interface LookupUiState {
    data object Idle : LookupUiState
    data object Loading : LookupUiState
    data class Found(
        val studentId: String,
        val displayName: String,
        val givenName: String,
        val subject: String
    ) : LookupUiState
    data class Message(val text: String, val isOffline: Boolean = false) : LookupUiState
}

data class AttendanceUiState(
    val isProfileLoaded: Boolean = false,
    val profile: SavedStudentProfile = SavedStudentProfile(),
    val todayDateKey: String = manilaDateKey(),
    val isEditingId: Boolean = false,
    val idInput: String = "",
    val idValidationError: String? = null,
    val classCodeInput: String = "",
    val lookupState: LookupUiState = LookupUiState.Idle,
    val nfcStatus: NfcHardwareStatus = NfcHardwareStatus.Unsupported,
    val pendingClassCode: String? = null,
    val checkInState: CheckInUiState = CheckInUiState.Idle
) {
    val hasCheckedInToday: Boolean
        get() = profile.lastCheckIn.isValid && profile.lastCheckIn.dateKey == todayDateKey
}

private fun manilaDateKey(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("Asia/Manila")
    return fmt.format(Date())
}

class AttendanceViewModel(
    private val preferencesRepository: StudentPreferencesRepository,
    private val api: AttendanceApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<FeedbackType>(extraBufferCapacity = 4)
    val feedbackEvents: SharedFlow<FeedbackType> = _feedbackEvents.asSharedFlow()

    private var queuedClassCode: String? = null

    init {
        viewModelScope.launch {
            preferencesRepository.studentProfileFlow.collect { profile ->
                _uiState.update { current ->
                    val firstLoad = !current.isProfileLoaded
                    val nextIdInput = if (firstLoad) profile.studentId else current.idInput
                    current.copy(
                        isProfileLoaded = true,
                        profile = profile,
                        todayDateKey = manilaDateKey(),
                        idInput = nextIdInput
                    )
                }
                onProfileLoadedCheckPending(profile)
            }
        }
    }

    fun updateNfcStatus(status: NfcHardwareStatus) {
        _uiState.update { it.copy(nfcStatus = status, todayDateKey = manilaDateKey()) }
    }

    fun handleIncomingIntent(intent: Intent?) {
        when (val result = NfcTagParser.extractClassCode(intent)) {
            is TagParseResult.ValidCode -> {
                handleScannedClassCode(result.classCode)
            }
            TagParseResult.InvalidOrEmptyTag -> {
                _feedbackEvents.tryEmit(FeedbackType.Error)
                _uiState.update {
                    it.copy(
                        pendingClassCode = null,
                        checkInState = CheckInUiState.InvalidTag
                    )
                }
            }
            TagParseResult.NotAnNfcIntent -> Unit
        }
    }

    fun handleScannedClassCode(classCode: String) {
        val state = _uiState.value
        if (!state.isProfileLoaded) {
            queuedClassCode = classCode
            _uiState.update { it.copy(pendingClassCode = classCode) }
            return
        }
        if (!state.profile.hasSavedId || state.isEditingId) {
            queuedClassCode = classCode
            _uiState.update {
                it.copy(
                    pendingClassCode = classCode,
                    checkInState = CheckInUiState.Idle
                )
            }
            return
        }
        performCheckIn(classCode, state.profile.studentId)
    }

    fun onIdInputChanged(value: String) {
        _uiState.update {
            it.copy(
                idInput = value,
                idValidationError = null,
                lookupState = LookupUiState.Idle
            )
        }
    }

    fun onClassCodeInputChanged(value: String) {
        _uiState.update {
            it.copy(
                classCodeInput = value,
                lookupState = LookupUiState.Idle
            )
        }
    }

    fun startEditingId() {
        _uiState.update {
            it.copy(
                isEditingId = true,
                idInput = it.profile.studentId,
                idValidationError = null,
                lookupState = LookupUiState.Idle
            )
        }
    }

    fun cancelEditingId() {
        _uiState.update {
            it.copy(
                isEditingId = false,
                idInput = it.profile.studentId,
                idValidationError = null,
                lookupState = LookupUiState.Idle
            )
        }
    }

    fun verifyIdWithClassCode() {
        val state = _uiState.value
        val idError = validateStudentId(state.idInput)
        if (idError != null) {
            _uiState.update { it.copy(idValidationError = idError) }
            return
        }
        val code = state.classCodeInput.trim()
        if (code.length < 2) {
            _uiState.update {
                it.copy(lookupState = LookupUiState.Message("Enter a subject code to look up your name."))
            }
            return
        }

        val cleanId = state.idInput.trim()
        _uiState.update { it.copy(lookupState = LookupUiState.Loading) }

        viewModelScope.launch {
            val outcome = api.lookup(code, cleanId)
            _uiState.update { current ->
                val nextLookup = when (outcome) {
                    is LookupOutcome.Found -> LookupUiState.Found(
                        studentId = cleanId,
                        displayName = outcome.display,
                        givenName = outcome.given,
                        subject = outcome.subject
                    )
                    is LookupOutcome.NotFound -> LookupUiState.Message(outcome.message)
                    LookupOutcome.Offline -> LookupUiState.Message(
                        "No connection, tap again when online.",
                        isOffline = true
                    )
                    LookupOutcome.ServerEndpointNotReady -> LookupUiState.Message(
                        "Server lookup endpoint is not ready yet. You can still save your ID directly."
                    )
                    is LookupOutcome.Error -> LookupUiState.Message(outcome.message)
                }
                current.copy(lookupState = nextLookup)
            }
        }
    }

    fun rejectLookupResult() {
        _uiState.update { it.copy(lookupState = LookupUiState.Idle) }
    }

    fun saveStudentId(verifiedDisplayName: String = "", verifiedGivenName: String = "") {
        val state = _uiState.value
        val idError = validateStudentId(state.idInput)
        if (idError != null) {
            _uiState.update { it.copy(idValidationError = idError) }
            return
        }

        val cleanId = state.idInput.trim()
        val pendingCode = state.pendingClassCode ?: queuedClassCode

        viewModelScope.launch {
            preferencesRepository.saveStudentProfile(
                studentId = cleanId,
                verifiedDisplayName = verifiedDisplayName,
                verifiedGivenName = verifiedGivenName
            )
            queuedClassCode = null
            _uiState.update {
                it.copy(
                    isEditingId = false,
                    idValidationError = null,
                    lookupState = LookupUiState.Idle,
                    pendingClassCode = null
                )
            }
            if (!pendingCode.isNullOrBlank()) {
                performCheckIn(pendingCode, cleanId)
            }
        }
    }

    fun dismissCheckInResult() {
        _uiState.update { it.copy(checkInState = CheckInUiState.Idle) }
    }

    fun openEditFromNotFound() {
        _uiState.update {
            it.copy(
                checkInState = CheckInUiState.Idle,
                isEditingId = true,
                idInput = it.profile.studentId,
                idValidationError = null,
                lookupState = LookupUiState.Idle
            )
        }
    }

    internal fun onProfileLoadedCheckPending(profile: SavedStudentProfile) {
        val pending = queuedClassCode
        if (profile.hasSavedId && !pending.isNullOrBlank() && !_uiState.value.isEditingId) {
            queuedClassCode = null
            _uiState.update { it.copy(pendingClassCode = null) }
            performCheckIn(pending, profile.studentId)
        }
    }

    private fun performCheckIn(classCode: String, studentId: String) {
        _uiState.update {
            it.copy(checkInState = CheckInUiState.Loading(classCode))
        }
        viewModelScope.launch {
            val outcome = api.checkIn(classCode, studentId)
            val dateKey = manilaDateKey()
            when (outcome) {
                is CheckInOutcome.Present -> {
                    preferencesRepository.updateVerifiedNameIfEmpty(outcome.display, outcome.given)
                    preferencesRepository.saveLastCheckIn(
                        subject = outcome.subject.ifBlank { classCode },
                        status = "Present",
                        time = outcome.time,
                        dateKey = dateKey
                    )
                    _feedbackEvents.tryEmit(FeedbackType.Success)
                }
                is CheckInOutcome.Late -> {
                    preferencesRepository.updateVerifiedNameIfEmpty(outcome.display, outcome.given)
                    preferencesRepository.saveLastCheckIn(
                        subject = outcome.subject.ifBlank { classCode },
                        status = "Late",
                        time = outcome.time,
                        dateKey = dateKey
                    )
                    _feedbackEvents.tryEmit(FeedbackType.Warning)
                }
                is CheckInOutcome.AlreadyCheckedIn -> {
                    preferencesRepository.updateVerifiedNameIfEmpty(outcome.display, outcome.given)
                    preferencesRepository.saveLastCheckIn(
                        subject = outcome.subject.ifBlank { classCode },
                        status = outcome.status,
                        time = outcome.time,
                        dateKey = dateKey
                    )
                    _feedbackEvents.tryEmit(FeedbackType.Success)
                }
                is CheckInOutcome.Closed,
                is CheckInOutcome.NoClass -> _feedbackEvents.tryEmit(FeedbackType.Warning)
                is CheckInOutcome.NotFound,
                is CheckInOutcome.Offline,
                is CheckInOutcome.ServerEndpointNotReady,
                is CheckInOutcome.Error -> _feedbackEvents.tryEmit(FeedbackType.Error)
            }
            _uiState.update {
                it.copy(
                    todayDateKey = dateKey,
                    checkInState = CheckInUiState.Result(outcome)
                )
            }
        }
    }

    private fun validateStudentId(rawId: String): String? {
        val trimmed = rawId.trim()
        if (trimmed.isEmpty()) {
            return "Please enter your Student ID number."
        }
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 4) {
            return "Enter at least 4 digits for your Student ID."
        }
        return null
    }

    class Factory(
        private val preferencesRepository: StudentPreferencesRepository,
        private val api: AttendanceApi
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AttendanceViewModel(preferencesRepository, api) as T
        }
    }
}
