package com.example

import com.example.data.ClassSummary
import com.example.data.CsvRosterParser
import com.example.data.RosterFileParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CsvRosterParserUnitTest {

    @Test
    fun parsesCsvWithHeaderAndQuotedLastFirstNames() {
        val csv = """
            ID Number, Student Name, Course & Year
            2023-1234, "ACOSTA, Princess Dian", BSED 3
            93056169, "Jalando-On, Francis Neil", MDiv 2
            bad, Missing Digits, AB 1
        """.trimIndent()

        val rows = CsvRosterParser.parse(csv)
        assertEquals(2, rows.size)
        assertEquals("2023-1234", rows[0].id)
        assertEquals("ACOSTA, Princess Dian", rows[0].name)
        assertEquals("BSED 3", rows[0].course)
        assertEquals("93056169", rows[1].id)
        assertEquals("Jalando-On, Francis Neil", rows[1].name)
    }

    @Test
    fun parsesTabSeparatedPasteWithoutHeaders() {
        val tsv = "20240011\tSantos, Maria\tBSBA 2\n20240022\tReyes, Juan"
        val rows = CsvRosterParser.parse(tsv)
        assertEquals(2, rows.size)
        assertEquals("20240011", rows[0].id)
        assertEquals("Santos, Maria", rows[0].name)
        assertEquals("BSBA 2", rows[0].course)
        assertEquals("20240022", rows[1].id)
        assertEquals("", rows[1].course)
    }

    @Test
    fun parsesXlsxWorkbookWithSharedStringsAndNumericIds() {
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="4" uniqueCount="4">
              <si><t>Student ID</t></si>
              <si><t>Full Name</t></si>
              <si><t>Course</t></si>
              <si><t>Jalando-On, Francis Neil</t></si>
            </sst>
        """.trimIndent()

        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                  <c r="C1" t="s"><v>2</v></c>
                </row>
                <row r="2">
                  <c r="A2"><v>93056169.0</v></c>
                  <c r="B2" t="s"><v>3</v></c>
                  <c r="C2" t="inlineStr"><is><t>MDiv 2</t></is></c>
                </row>
                <row r="3">
                  <c r="A3"><v>2.0241234E7</v></c>
                  <c r="B3" t="inlineStr"><is><t>Santos, Maria</t></is></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheetXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val result = CsvRosterParser.parseFileBytes(baos.toByteArray())
        assertTrue(result is RosterFileParseResult.Success)
        val rows = (result as RosterFileParseResult.Success).rows
        assertEquals(2, rows.size)
        assertEquals("93056169", rows[0].id)
        assertEquals("Jalando-On, Francis Neil", rows[0].name)
        assertEquals("MDiv 2", rows[0].course)
        assertEquals("20241234", rows[1].id)
        assertEquals("Santos, Maria", rows[1].name)
    }

    @Test
    fun rejectsLegacyBinaryXlsWithClearMessage() {
        val fakeXls = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(), 0x00)
        val result = CsvRosterParser.parseFileBytes(fakeXls)
        assertTrue(result is RosterFileParseResult.Error)
        assertTrue((result as RosterFileParseResult.Error).message.contains(".xlsx"))
    }

    @Test
    fun extractsShortSubjectCodeCleanly() {
        assertEquals(
            "BUS 2101",
            ClassSummary(code = "1521", subject = "BUS 2101: Good Governance and Social Responsibility").shortSubjectCode
        )
        assertEquals(
            "HS 3202",
            ClassSummary(code = "8UHK22", subject = "HS 3202 [Modern and Post-Modern Church History]").shortSubjectCode
        )
        assertEquals(
            "CE Tech 1",
            ClassSummary(code = "72PGMX", subject = "CE Tech 1").shortSubjectCode
        )
    }
}
