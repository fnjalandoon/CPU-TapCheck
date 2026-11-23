package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface AttendanceApi {
    suspend fun lookup(classCode: String, studentId: String): LookupOutcome
    suspend fun checkIn(classCode: String, studentId: String): CheckInOutcome
}

class AttendanceApiClient(
    private val context: Context,
    private val baseUrl: String = AttendanceConfig.WEB_APP_BASE_URL,
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val moshi: Moshi = Moshi.Builder().build()
) : AttendanceApi {

    private val lookupAdapter = moshi.adapter(LookupResponse::class.java)
    private val checkInAdapter = moshi.adapter(CheckInResponse::class.java)

    override suspend fun lookup(classCode: String, studentId: String): LookupOutcome =
        withContext(Dispatchers.IO) {
            if (!isNetworkAvailable()) {
                return@withContext LookupOutcome.Offline
            }

            val url = baseUrl.toHttpUrl().newBuilder()
                .addQueryParameter("api", "lookup")
                .addQueryParameter("code", classCode.trim())
                .addQueryParameter("id", studentId.trim())
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()?.trim().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext LookupOutcome.Error(
                            "Server returned status ${response.code}."
                        )
                    }
                    if (body.startsWith("<")) {
                        return@withContext LookupOutcome.ServerEndpointNotReady
                    }
                    val parsed = runCatching { lookupAdapter.fromJson(body) }.getOrNull()
                        ?: return@withContext LookupOutcome.Error("Unexpected response from server.")

                    if (parsed.ok) {
                        LookupOutcome.Found(
                            display = parsed.display.orEmpty().ifBlank { studentId.trim() },
                            given = parsed.given.orEmpty().ifBlank {
                                parsed.display.orEmpty().ifBlank { studentId.trim() }
                            },
                            subject = parsed.subject.orEmpty()
                        )
                    } else {
                        LookupOutcome.NotFound(
                            parsed.msg?.takeIf { it.isNotBlank() }
                                ?: "Student ID was not found for class ${classCode.trim()}."
                        )
                    }
                }
            } catch (_: IOException) {
                LookupOutcome.Offline
            }
        }

    override suspend fun checkIn(classCode: String, studentId: String): CheckInOutcome =
        withContext(Dispatchers.IO) {
            val cleanCode = classCode.trim()
            if (!isNetworkAvailable()) {
                return@withContext CheckInOutcome.Offline(cleanCode)
            }

            val url = baseUrl.toHttpUrl().newBuilder()
                .addQueryParameter("api", "checkin")
                .addQueryParameter("code", cleanCode)
                .addQueryParameter("id", studentId.trim())
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()?.trim().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext CheckInOutcome.Error(
                            classCode = cleanCode,
                            message = "Server returned status ${response.code}."
                        )
                    }
                    if (body.startsWith("<")) {
                        return@withContext CheckInOutcome.ServerEndpointNotReady(cleanCode)
                    }
                    val parsed = runCatching { checkInAdapter.fromJson(body) }.getOrNull()
                        ?: return@withContext CheckInOutcome.Error(
                            classCode = cleanCode,
                            message = "Unexpected response from server."
                        )

                    mapCheckInResponse(cleanCode, parsed)
                }
            } catch (_: IOException) {
                CheckInOutcome.Offline(cleanCode)
            }
        }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        fun mapCheckInResponse(classCode: String, response: CheckInResponse): CheckInOutcome {
            val given = response.given.orEmpty().ifBlank { response.display.orEmpty() }
            val display = response.display.orEmpty().ifBlank { given }
            val subject = response.subject.orEmpty()
            val dateLabel = response.dateLabel.orEmpty()
            val time = response.time.orEmpty()
            val status = response.status.orEmpty()
            val state = response.state.orEmpty().lowercase()

            if (response.ok) {
                return when {
                    response.already || state == "already" -> CheckInOutcome.AlreadyCheckedIn(
                        classCode = classCode,
                        given = given,
                        display = display,
                        subject = subject,
                        dateLabel = dateLabel,
                        status = status.ifBlank { "Present" },
                        time = time
                    )
                    status.equals("Late", ignoreCase = true) -> CheckInOutcome.Late(
                        classCode = classCode,
                        given = given,
                        display = display,
                        subject = subject,
                        dateLabel = dateLabel,
                        time = time
                    )
                    else -> CheckInOutcome.Present(
                        classCode = classCode,
                        given = given,
                        display = display,
                        subject = subject,
                        dateLabel = dateLabel,
                        time = time
                    )
                }
            }

            return when (state) {
                "already" -> CheckInOutcome.AlreadyCheckedIn(
                    classCode = classCode,
                    given = given,
                    display = display,
                    subject = subject,
                    dateLabel = dateLabel,
                    status = status.ifBlank { "Present" },
                    time = time
                )
                "closed" -> CheckInOutcome.Closed(
                    classCode = classCode,
                    subject = subject,
                    message = response.msg?.takeIf { it.isNotBlank() }
                        ?: "Attendance is currently closed for this class."
                )
                "noclass" -> CheckInOutcome.NoClass(
                    classCode = classCode,
                    subject = subject,
                    message = response.msg?.takeIf { it.isNotBlank() }
                        ?: "There is no scheduled class today."
                )
                "notfound" -> CheckInOutcome.NotFound(
                    classCode = classCode,
                    subject = subject,
                    message = response.msg?.takeIf { it.isNotBlank() }
                        ?: "Your Student ID is not on this class roster."
                )
                else -> CheckInOutcome.Error(
                    classCode = classCode,
                    message = response.msg?.takeIf { it.isNotBlank() }
                        ?: "Could not complete check-in. Please tap again."
                )
            }
        }

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
