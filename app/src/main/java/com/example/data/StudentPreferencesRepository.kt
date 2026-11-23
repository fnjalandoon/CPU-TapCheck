package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cpu_tapcheck_student_prefs"
)

data class LastCheckInRecord(
    val subject: String = "",
    val status: String = "",
    val time: String = "",
    val dateKey: String = ""
) {
    val isValid: Boolean get() = subject.isNotBlank() && dateKey.isNotBlank()
}

data class SavedStudentProfile(
    val studentId: String = "",
    val verifiedDisplayName: String = "",
    val verifiedGivenName: String = "",
    val lastCheckIn: LastCheckInRecord = LastCheckInRecord()
) {
    val hasSavedId: Boolean get() = studentId.isNotBlank()
}

class StudentPreferencesRepository(private val context: Context) {

    private object Keys {
        val STUDENT_ID = stringPreferencesKey("student_id")
        val VERIFIED_DISPLAY_NAME = stringPreferencesKey("verified_display_name")
        val VERIFIED_GIVEN_NAME = stringPreferencesKey("verified_given_name")
        val LAST_SUBJECT = stringPreferencesKey("last_checkin_subject")
        val LAST_STATUS = stringPreferencesKey("last_checkin_status")
        val LAST_TIME = stringPreferencesKey("last_checkin_time")
        val LAST_DATE_KEY = stringPreferencesKey("last_checkin_date_key")
    }

    val studentProfileFlow: Flow<SavedStudentProfile> = context.studentDataStore.data.map { prefs ->
        SavedStudentProfile(
            studentId = prefs[Keys.STUDENT_ID].orEmpty(),
            verifiedDisplayName = prefs[Keys.VERIFIED_DISPLAY_NAME].orEmpty(),
            verifiedGivenName = prefs[Keys.VERIFIED_GIVEN_NAME].orEmpty(),
            lastCheckIn = LastCheckInRecord(
                subject = prefs[Keys.LAST_SUBJECT].orEmpty(),
                status = prefs[Keys.LAST_STATUS].orEmpty(),
                time = prefs[Keys.LAST_TIME].orEmpty(),
                dateKey = prefs[Keys.LAST_DATE_KEY].orEmpty()
            )
        )
    }

    suspend fun saveStudentProfile(
        studentId: String,
        verifiedDisplayName: String = "",
        verifiedGivenName: String = ""
    ) {
        val cleanId = studentId.trim()
        context.studentDataStore.edit { prefs ->
            val previousId = prefs[Keys.STUDENT_ID].orEmpty()
            prefs[Keys.STUDENT_ID] = cleanId
            if (verifiedDisplayName.isNotBlank()) {
                prefs[Keys.VERIFIED_DISPLAY_NAME] = verifiedDisplayName.trim()
            } else if (previousId != cleanId) {
                prefs.remove(Keys.VERIFIED_DISPLAY_NAME)
            }
            if (verifiedGivenName.isNotBlank()) {
                prefs[Keys.VERIFIED_GIVEN_NAME] = verifiedGivenName.trim()
            } else if (previousId != cleanId) {
                prefs.remove(Keys.VERIFIED_GIVEN_NAME)
            }
            if (previousId != cleanId) {
                prefs.remove(Keys.LAST_SUBJECT)
                prefs.remove(Keys.LAST_STATUS)
                prefs.remove(Keys.LAST_TIME)
                prefs.remove(Keys.LAST_DATE_KEY)
            }
        }
    }

    suspend fun updateVerifiedNameIfEmpty(displayName: String, givenName: String) {
        if (displayName.isBlank() && givenName.isBlank()) return
        context.studentDataStore.edit { prefs ->
            if (prefs[Keys.VERIFIED_DISPLAY_NAME].isNullOrBlank() && displayName.isNotBlank()) {
                prefs[Keys.VERIFIED_DISPLAY_NAME] = displayName.trim()
            }
            if (prefs[Keys.VERIFIED_GIVEN_NAME].isNullOrBlank() && givenName.isNotBlank()) {
                prefs[Keys.VERIFIED_GIVEN_NAME] = givenName.trim()
            }
        }
    }

    suspend fun saveLastCheckIn(subject: String, status: String, time: String, dateKey: String) {
        if (subject.isBlank() || dateKey.isBlank()) return
        context.studentDataStore.edit { prefs ->
            prefs[Keys.LAST_SUBJECT] = subject.trim()
            prefs[Keys.LAST_STATUS] = status.trim()
            prefs[Keys.LAST_TIME] = time.trim()
            prefs[Keys.LAST_DATE_KEY] = dateKey.trim()
        }
    }

    suspend fun clearStudentProfile() {
        context.studentDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
