package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TeacherUser(
    val username: String = "",
    val name: String = "",
    val role: String = "teacher",
    val mustChange: Boolean = false
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class ClassSummary(
    val code: String = "",
    val subject: String = "",
    val schedule: String = "",
    val owner: String = "",
    val ownerName: String = "",
    val students: Int = 0,
    val days: String = "",
    val start: String = "",
    val end: String = "",
    val openBefore: Int = 30,
    val lateAfter: Int = 15,
    val extra: String = "",
    val skip: String = ""
) {
    /** Extracts the short subject code prefix (e.g. "BUS 2101" from "BUS 2101: Good Governance"). */
    val shortSubjectCode: String
        get() {
            val trimmed = subject.trim()
            val colonIdx = trimmed.indexOf(':')
            val bracketIdx = trimmed.indexOf('[')
            val cutIdx = listOf(colonIdx, bracketIdx).filter { it > 0 }.minOrNull()
            return if (cutIdx != null) trimmed.substring(0, cutIdx).trim() else trimmed
        }
}

@JsonClass(generateAdapter = true)
data class TeacherAuthResponse(
    val ok: Boolean = false,
    val token: String? = null,
    val user: TeacherUser? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class TeacherInitResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val mustChange: Boolean = false,
    val user: TeacherUser? = null,
    val baseUrl: String? = null,
    val threshold: Int = 80,
    val classes: List<ClassSummary>? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class ClassesListResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val classes: List<ClassSummary>? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class ClassInfoPayload(
    val subject: String,
    val days: String,
    val start: String,
    val end: String,
    val openBefore: Int,
    val lateAfter: Int,
    val extra: String,
    val skip: String
)

@JsonClass(generateAdapter = true)
data class RosterRowPayload(
    val id: String,
    val name: String,
    val course: String = ""
)

@JsonClass(generateAdapter = true)
data class AddClassResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val code: String? = null,
    val added: Int = 0,
    val skipped: List<String>? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class SimpleApiResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class TodayPresentRow(
    val id: String = "",
    val name: String = "",
    val time: String = "",
    val status: String = "",
    val method: String = ""
)

@JsonClass(generateAdapter = true)
data class TodayAbsentRow(
    val id: String = "",
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class TodayCounts(
    val total: Int = 0,
    val present: Int = 0,
    val late: Int = 0,
    val absent: Int = 0
)

@JsonClass(generateAdapter = true)
data class TodayResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val subject: String = "",
    val dateLabel: String = "",
    val isDay: Boolean = false,
    val open: Boolean = false,
    val present: List<TodayPresentRow> = emptyList(),
    val absent: List<TodayAbsentRow> = emptyList(),
    val counts: TodayCounts = TodayCounts(),
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class TotalsRow(
    val id: String = "",
    val name: String = "",
    val attended: Int = 0,
    val pct: Double? = null
)

@JsonClass(generateAdapter = true)
data class TotalsResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val held: Int = 0,
    val threshold: Int = 80,
    val rows: List<TotalsRow> = emptyList(),
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class TeacherAccountItem(
    val username: String = "",
    val name: String = "",
    val role: String = "teacher",
    val active: Boolean = true,
    val mustChange: Boolean = false,
    val classes: Int = 0,
    val created: String = ""
)

@JsonClass(generateAdapter = true)
data class TeachersListResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val teachers: List<TeacherAccountItem> = emptyList(),
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class TeacherKeyResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val username: String? = null,
    val name: String? = null,
    val tempKey: String? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminSettingsResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val threshold: Int = 80,
    val webAppUrl: String = "",
    val msg: String? = null
)

// Request envelopes for Code.gs doPost
@JsonClass(generateAdapter = true)
data class PostLoginRequest(val action: String = "login", val username: String, val key: String)

@JsonClass(generateAdapter = true)
data class PostTokenRequest(val action: String, val token: String)

@JsonClass(generateAdapter = true)
data class PostChangeKeyRequest(
    val action: String = "changeKey",
    val token: String,
    val oldKey: String,
    val newKey: String
)

@JsonClass(generateAdapter = true)
data class PostClassCodeRequest(
    val action: String,
    val token: String,
    val code: String
)

@JsonClass(generateAdapter = true)
data class PostAddClassRequest(
    val action: String = "addClass",
    val token: String,
    val info: ClassInfoPayload,
    val rows: List<RosterRowPayload>
)

@JsonClass(generateAdapter = true)
data class PostUpdateClassRequest(
    val action: String = "updateClass",
    val token: String,
    val code: String,
    val info: ClassInfoPayload
)

@JsonClass(generateAdapter = true)
data class PostManualMarkRequest(
    val action: String = "manualMark",
    val token: String,
    val code: String,
    val id: String
)

@JsonClass(generateAdapter = true)
data class PostAddTeacherRequest(
    val action: String = "addTeacher",
    val token: String,
    val username: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class PostResetKeyRequest(
    val action: String = "resetKey",
    val token: String,
    val username: String
)

@JsonClass(generateAdapter = true)
data class PostSetActiveRequest(
    val action: String = "setActive",
    val token: String,
    val username: String,
    val active: Boolean
)

@JsonClass(generateAdapter = true)
data class RosterListResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val rows: List<RosterRowPayload> = emptyList(),
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class RosterAddResponse(
    val ok: Boolean = false,
    val auth: Boolean = false,
    val added: Int = 0,
    val skipped: List<String>? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class PostSaveSettingsRequest(
    val action: String = "saveSettings",
    val token: String,
    val threshold: Int,
    val webAppUrl: String
)

@JsonClass(generateAdapter = true)
data class PostRosterAddRequest(
    val action: String = "rosterAdd",
    val token: String,
    val code: String,
    val rows: List<RosterRowPayload>
)

@JsonClass(generateAdapter = true)
data class PostRosterRemoveRequest(
    val action: String = "rosterRemove",
    val token: String,
    val code: String,
    val id: String
)
