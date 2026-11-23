package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.teacherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cpu_tapcheck_teacher_session"
)

data class TeacherSession(
    val token: String = "",
    val user: TeacherUser? = null
) {
    val isLoggedIn: Boolean get() = token.isNotBlank() && user != null
}

class TeacherPreferencesRepository(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USERNAME = stringPreferencesKey("username")
        val NAME = stringPreferencesKey("name")
        val ROLE = stringPreferencesKey("role")
        val MUST_CHANGE = booleanPreferencesKey("must_change")
    }

    val sessionFlow: Flow<TeacherSession> = context.teacherDataStore.data.map { prefs ->
        val token = prefs[Keys.TOKEN].orEmpty()
        val username = prefs[Keys.USERNAME].orEmpty()
        if (token.isBlank() || username.isBlank()) {
            TeacherSession()
        } else {
            TeacherSession(
                token = token,
                user = TeacherUser(
                    username = username,
                    name = prefs[Keys.NAME].orEmpty(),
                    role = prefs[Keys.ROLE].orEmpty().ifBlank { "teacher" },
                    mustChange = prefs[Keys.MUST_CHANGE] ?: false
                )
            )
        }
    }

    suspend fun saveSession(token: String, user: TeacherUser) {
        context.teacherDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USERNAME] = user.username
            prefs[Keys.NAME] = user.name
            prefs[Keys.ROLE] = user.role
            prefs[Keys.MUST_CHANGE] = user.mustChange
        }
    }

    suspend fun clearSession() {
        context.teacherDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
