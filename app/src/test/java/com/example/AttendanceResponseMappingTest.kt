package com.example

import com.example.data.AttendanceApiClient
import com.example.data.CheckInOutcome
import com.example.data.CheckInResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceResponseMappingTest {

    @Test
    fun `maps Present response`() {
        val outcome = AttendanceApiClient.mapCheckInResponse(
            "1521",
            CheckInResponse(
                ok = true,
                given = "Maria",
                display = "Maria Santos",
                subject = "BUS 2101",
                dateLabel = "Nov 23, 2026",
                status = "Present",
                time = "8:55 AM",
                already = false
            )
        )
        assertTrue(outcome is CheckInOutcome.Present)
        val present = outcome as CheckInOutcome.Present
        assertEquals("Maria", present.given)
        assertEquals("BUS 2101", present.subject)
        assertEquals("8:55 AM", present.time)
    }

    @Test
    fun `maps Late response`() {
        val outcome = AttendanceApiClient.mapCheckInResponse(
            "1521",
            CheckInResponse(
                ok = true,
                given = "Juan",
                display = "Juan Cruz",
                subject = "BUS 2101",
                dateLabel = "Nov 23, 2026",
                status = "Late",
                time = "9:16 AM",
                already = false
            )
        )
        assertTrue(outcome is CheckInOutcome.Late)
    }

    @Test
    fun `maps Already Checked In response`() {
        val outcome = AttendanceApiClient.mapCheckInResponse(
            "1521",
            CheckInResponse(
                ok = true,
                given = "Maria",
                display = "Maria Santos",
                subject = "BUS 2101",
                dateLabel = "Nov 23, 2026",
                status = "Present",
                time = "8:55 AM",
                already = true
            )
        )
        assertTrue(outcome is CheckInOutcome.AlreadyCheckedIn)
    }

    @Test
    fun `maps closed noclass and notfound server states`() {
        assertTrue(
            AttendanceApiClient.mapCheckInResponse(
                "1521",
                CheckInResponse(ok = false, state = "closed")
            ) is CheckInOutcome.Closed
        )
        assertTrue(
            AttendanceApiClient.mapCheckInResponse(
                "1521",
                CheckInResponse(ok = false, state = "noclass")
            ) is CheckInOutcome.NoClass
        )
        assertTrue(
            AttendanceApiClient.mapCheckInResponse(
                "1521",
                CheckInResponse(ok = false, state = "notfound")
            ) is CheckInOutcome.NotFound
        )
    }

    @Test
    fun `TodayResponse resolves and sorts recorded dates descending`() {
        val resp = com.example.data.TodayResponse(
            ok = true,
            dates = listOf("2026-11-20", "2026-11-26", "2026-11-20", "2026-11-24")
        )
        assertEquals(listOf("2026-11-26", "2026-11-24", "2026-11-20"), resp.resolvedDates)

        val fallbackResp = com.example.data.TodayResponse(
            ok = true,
            recordedDates = listOf("2026-11-17", "2026-11-19")
        )
        assertEquals(listOf("2026-11-19", "2026-11-17"), fallbackResp.resolvedDates)
    }

    @Test
    fun `TodayDateHelpers UTC round-trip preserves exact calendar date`() {
        val key = "2026-11-24"
        val utcMillis = com.example.ui.teacher.TodayDateHelpers.dateKeyToUtcMillis(key)
        assertTrue(utcMillis != null)
        assertEquals(key, com.example.ui.teacher.TodayDateHelpers.utcMillisToDateKey(utcMillis!!))
        assertEquals("Nov 24, 2026", com.example.ui.teacher.TodayDateHelpers.formatShortDate(key))
    }
}
