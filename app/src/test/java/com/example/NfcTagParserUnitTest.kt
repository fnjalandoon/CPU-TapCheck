package com.example

import com.example.data.NfcTagParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NfcTagParserUnitTest {

    @Test
    fun `parses subject codes with spaces and hyphens`() {
        assertEquals("BUS 2101", NfcTagParser.parseClassCodeString("BUS 2101"))
        assertEquals("THEO 101-A", NfcTagParser.parseClassCodeString("  THEO  101-A \n"))
        assertEquals("1521", NfcTagParser.parseClassCodeString("1521"))
    }

    @Test
    fun `parses key-value code prefix with subject code`() {
        assertEquals("BUS 2101", NfcTagParser.parseClassCodeString("code=BUS 2101"))
    }

    @Test
    fun `rejects invalid or empty text`() {
        assertNull(NfcTagParser.parseClassCodeString("   "))
        assertNull(NfcTagParser.parseClassCodeString("a"))
        assertNull(NfcTagParser.parseClassCodeString("hello! @#\$% invalid"))
    }

    @Test
    fun `decodes NDEF UTF-8 text record payload`() {
        val lang = "en".toByteArray(Charsets.US_ASCII)
        val text = "BUS 2101".toByteArray(Charsets.UTF_8)
        val payload = byteArrayOf(lang.size.toByte()) + lang + text
        assertEquals("BUS 2101", NfcTagParser.decodeTextPayload(payload))
    }
}
