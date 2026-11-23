package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LookupResponse(
    val ok: Boolean = false,
    val display: String? = null,
    val given: String? = null,
    val subject: String? = null,
    val msg: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckInResponse(
    val ok: Boolean = false,
    val state: String? = null,
    val given: String? = null,
    val display: String? = null,
    val subject: String? = null,
    val dateLabel: String? = null,
    val status: String? = null,
    val time: String? = null,
    val already: Boolean = false,
    val msg: String? = null
)

sealed interface LookupOutcome {
    data class Found(
        val display: String,
        val given: String,
        val subject: String
    ) : LookupOutcome

    data class NotFound(val message: String) : LookupOutcome
    data object Offline : LookupOutcome
    data object ServerEndpointNotReady : LookupOutcome
    data class Error(val message: String) : LookupOutcome
}

sealed interface CheckInOutcome {
    data class Present(
        val classCode: String,
        val given: String,
        val display: String,
        val subject: String,
        val dateLabel: String,
        val time: String
    ) : CheckInOutcome

    data class Late(
        val classCode: String,
        val given: String,
        val display: String,
        val subject: String,
        val dateLabel: String,
        val time: String
    ) : CheckInOutcome

    data class AlreadyCheckedIn(
        val classCode: String,
        val given: String,
        val display: String,
        val subject: String,
        val dateLabel: String,
        val status: String,
        val time: String
    ) : CheckInOutcome

    data class Closed(
        val classCode: String,
        val subject: String,
        val message: String
    ) : CheckInOutcome

    data class NoClass(
        val classCode: String,
        val subject: String,
        val message: String
    ) : CheckInOutcome

    data class NotFound(
        val classCode: String,
        val subject: String,
        val message: String
    ) : CheckInOutcome

    data class Offline(val classCode: String) : CheckInOutcome

    data class ServerEndpointNotReady(val classCode: String) : CheckInOutcome

    data class Error(
        val classCode: String,
        val message: String
    ) : CheckInOutcome
}
