package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

sealed interface ApiCallResult<out T> {
    data class Success<T>(val data: T) : ApiCallResult<T>
    data class Failure(val message: String, val authExpired: Boolean = false) : ApiCallResult<Nothing>
}

class TeacherApiClient(
    private val context: Context,
    private val baseUrl: String = AttendanceConfig.WEB_APP_BASE_URL,
    private val httpClient: OkHttpClient = AttendanceApiClient.defaultHttpClient(),
    private val moshi: Moshi = Moshi.Builder().build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(username: String, key: String): ApiCallResult<TeacherAuthResponse> =
        post(
            PostLoginRequest(username = username.trim(), key = key),
            moshi.adapter(PostLoginRequest::class.java),
            moshi.adapter(TeacherAuthResponse::class.java)
        )

    suspend fun init(token: String): ApiCallResult<TeacherInitResponse> =
        post(
            PostTokenRequest(action = "init", token = token),
            moshi.adapter(PostTokenRequest::class.java),
            moshi.adapter(TeacherInitResponse::class.java)
        )

    suspend fun changeKey(
        token: String,
        oldKey: String,
        newKey: String
    ): ApiCallResult<TeacherAuthResponse> =
        post(
            PostChangeKeyRequest(token = token, oldKey = oldKey, newKey = newKey),
            moshi.adapter(PostChangeKeyRequest::class.java),
            moshi.adapter(TeacherAuthResponse::class.java)
        )

    suspend fun addClass(
        token: String,
        info: ClassInfoPayload,
        rows: List<RosterRowPayload>
    ): ApiCallResult<AddClassResponse> =
        post(
            PostAddClassRequest(token = token, info = info, rows = rows),
            moshi.adapter(PostAddClassRequest::class.java),
            moshi.adapter(AddClassResponse::class.java)
        )

    suspend fun updateClass(
        token: String,
        code: String,
        info: ClassInfoPayload
    ): ApiCallResult<SimpleApiResponse> =
        post(
            PostUpdateClassRequest(token = token, code = code, info = info),
            moshi.adapter(PostUpdateClassRequest::class.java),
            moshi.adapter(SimpleApiResponse::class.java)
        )

    suspend fun today(
        token: String,
        code: String,
        date: String? = null
    ): ApiCallResult<TodayResponse> =
        post(
            PostClassCodeRequest(
                action = "today",
                token = token,
                code = code,
                date = date?.takeIf { it.isNotBlank() }
            ),
            moshi.adapter(PostClassCodeRequest::class.java),
            moshi.adapter(TodayResponse::class.java)
        )

    suspend fun reportDates(token: String, code: String): ApiCallResult<ReportDatesResponse> =
        post(
            PostClassCodeRequest(action = "report", token = token, code = code),
            moshi.adapter(PostClassCodeRequest::class.java),
            moshi.adapter(ReportDatesResponse::class.java)
        )

    suspend fun totals(token: String, code: String): ApiCallResult<TotalsResponse> =
        post(
            PostClassCodeRequest(action = "totals", token = token, code = code),
            moshi.adapter(PostClassCodeRequest::class.java),
            moshi.adapter(TotalsResponse::class.java)
        )

    suspend fun manualMark(
        token: String,
        code: String,
        studentId: String,
        date: String? = null
    ): ApiCallResult<CheckInResponse> =
        post(
            PostManualMarkRequest(
                token = token,
                code = code,
                id = studentId,
                date = date?.takeIf { it.isNotBlank() }
            ),
            moshi.adapter(PostManualMarkRequest::class.java),
            moshi.adapter(CheckInResponse::class.java)
        )

    suspend fun roster(token: String, code: String): ApiCallResult<RosterListResponse> =
        post(
            PostClassCodeRequest(action = "roster", token = token, code = code),
            moshi.adapter(PostClassCodeRequest::class.java),
            moshi.adapter(RosterListResponse::class.java)
        )

    suspend fun rosterAdd(
        token: String,
        code: String,
        rows: List<RosterRowPayload>
    ): ApiCallResult<RosterAddResponse> =
        post(
            PostRosterAddRequest(token = token, code = code, rows = rows),
            moshi.adapter(PostRosterAddRequest::class.java),
            moshi.adapter(RosterAddResponse::class.java)
        )

    suspend fun rosterRemove(
        token: String,
        code: String,
        studentId: String
    ): ApiCallResult<SimpleApiResponse> =
        post(
            PostRosterRemoveRequest(token = token, code = code, id = studentId),
            moshi.adapter(PostRosterRemoveRequest::class.java),
            moshi.adapter(SimpleApiResponse::class.java)
        )

    suspend fun teachers(token: String): ApiCallResult<TeachersListResponse> =
        post(
            PostTokenRequest(action = "teachers", token = token),
            moshi.adapter(PostTokenRequest::class.java),
            moshi.adapter(TeachersListResponse::class.java)
        )

    suspend fun addTeacher(
        token: String,
        username: String,
        name: String
    ): ApiCallResult<TeacherKeyResponse> =
        post(
            PostAddTeacherRequest(token = token, username = username, name = name),
            moshi.adapter(PostAddTeacherRequest::class.java),
            moshi.adapter(TeacherKeyResponse::class.java)
        )

    suspend fun resetKey(token: String, username: String): ApiCallResult<TeacherKeyResponse> =
        post(
            PostResetKeyRequest(token = token, username = username),
            moshi.adapter(PostResetKeyRequest::class.java),
            moshi.adapter(TeacherKeyResponse::class.java)
        )

    suspend fun setActive(
        token: String,
        username: String,
        active: Boolean
    ): ApiCallResult<SimpleApiResponse> =
        post(
            PostSetActiveRequest(token = token, username = username, active = active),
            moshi.adapter(PostSetActiveRequest::class.java),
            moshi.adapter(SimpleApiResponse::class.java)
        )

    suspend fun getSettings(token: String): ApiCallResult<AdminSettingsResponse> =
        post(
            PostTokenRequest(action = "getSettings", token = token),
            moshi.adapter(PostTokenRequest::class.java),
            moshi.adapter(AdminSettingsResponse::class.java)
        )

    suspend fun saveSettings(
        token: String,
        threshold: Int,
        webAppUrl: String
    ): ApiCallResult<SimpleApiResponse> =
        post(
            PostSaveSettingsRequest(token = token, threshold = threshold, webAppUrl = webAppUrl),
            moshi.adapter(PostSaveSettingsRequest::class.java),
            moshi.adapter(SimpleApiResponse::class.java)
        )

    private suspend fun <Req, Res> post(
        bodyObj: Req,
        reqAdapter: JsonAdapter<Req>,
        resAdapter: JsonAdapter<Res>
    ): ApiCallResult<Res> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext ApiCallResult.Failure("No internet connection. Please try again.")
        }

        val json = reqAdapter.toJson(bodyObj)
        val request = Request.Builder()
            .url(baseUrl)
            .header("Accept", "application/json")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string()?.trim().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiCallResult.Failure("Server returned status ${response.code}.")
                }
                if (raw.startsWith("<")) {
                    return@withContext ApiCallResult.Failure(
                        "Teacher API is not deployed in Code.gs yet. Update Code.gs and deploy a New version."
                    )
                }
                val parsed = runCatching { resAdapter.fromJson(raw) }.getOrNull()
                    ?: return@withContext ApiCallResult.Failure("Unexpected response from server.")
                ApiCallResult.Success(parsed)
            }
        } catch (_: IOException) {
            ApiCallResult.Failure("Connection error. Please try again.")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
