package com.example.data

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedRosterRow(
    val id: String,
    val name: String,
    val course: String = ""
)

sealed interface RosterFileParseResult {
    data class Success(val rows: List<ParsedRosterRow>) : RosterFileParseResult
    data class Error(val message: String) : RosterFileParseResult
}

object CsvRosterParser {

    fun parse(rawText: String): List<ParsedRosterRow> {
        val lines = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()
        val grid = lines.map { splitCsvOrTsvLine(it) }
        return parseGrid(grid)
    }

    fun parseFileBytes(bytes: ByteArray): RosterFileParseResult {
        if (bytes.isEmpty()) {
            return RosterFileParseResult.Error("The selected file is empty.")
        }

        // Legacy binary .xls (OLE2 compound document: D0 CF 11 E0)
        if (bytes.size >= 4 &&
            bytes[0] == 0xD0.toByte() &&
            bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() &&
            bytes[3] == 0xE0.toByte()
        ) {
            return RosterFileParseResult.Error(
                "Old .xls Excel format is not supported. Save the file as Excel Workbook (.xlsx) or CSV (.csv) and try again."
            )
        }

        // Modern .xlsx (ZIP archive starting with 'PK')
        if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            return parseXlsxBytes(bytes)
        }

        // Otherwise treat as UTF-8 CSV / TSV / text
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val rows = parse(text)
        return if (rows.isEmpty()) {
            RosterFileParseResult.Error("No valid students found in that CSV/text file. Check that each row has an ID number and Name.")
        } else {
            RosterFileParseResult.Success(rows)
        }
    }

    fun parseXlsxBytes(bytes: ByteArray): RosterFileParseResult {
        return try {
            var sharedStringsXml: ByteArray? = null
            val worksheetEntries = mutableListOf<Pair<String, ByteArray>>()

            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        when {
                            name == "xl/sharedstrings.xml" -> {
                                sharedStringsXml = zis.readBytes()
                            }
                            name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml") -> {
                                worksheetEntries.add(entry.name to zis.readBytes())
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (worksheetEntries.isEmpty()) {
                return RosterFileParseResult.Error("Could not find a worksheet inside that Excel file.")
            }

            // Prefer sheet1.xml or lowest sheet number
            val firstSheetBytes = worksheetEntries
                .minByOrNull { (name, _) ->
                    Regex("sheet(\\d+)\\.xml", RegexOption.IGNORE_CASE)
                        .find(name)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                }!!.second

            val sharedStrings = sharedStringsXml?.let(::parseSharedStringsXml).orEmpty()
            val grid = parseWorksheetXml(firstSheetBytes, sharedStrings)
            val rows = parseGrid(grid)

            if (rows.isEmpty()) {
                RosterFileParseResult.Error(
                    "No valid students found on the first Excel sheet. Check that ID Number and Name columns are populated."
                )
            } else {
                RosterFileParseResult.Success(rows)
            }
        } catch (_: Exception) {
            RosterFileParseResult.Error(
                "Could not read that Excel file. Try exporting it as .xlsx (without a password) or .csv."
            )
        }
    }

    internal fun parseGrid(grid: List<List<String>>): List<ParsedRosterRow> {
        val nonEmpty = grid.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmpty.isEmpty()) return emptyList()

        val out = mutableListOf<ParsedRosterRow>()
        var idCol = 0
        var nameCol = 1
        var courseCol = 2
        var startIdx = 0

        val firstCells = nonEmpty[0]
        if (firstCells.size >= 2 && looksLikeHeader(firstCells)) {
            firstCells.forEachIndexed { idx, cell ->
                val lower = cell.lowercase().replace(Regex("[^a-z]"), "")
                when {
                    lower == "id" || lower.contains("studentid") || lower.contains("idno") || lower.contains("idnumber") -> idCol = idx
                    lower == "name" || lower.contains("studentname") || lower.contains("fullname") -> nameCol = idx
                    lower.contains("course") || lower.contains("year") || lower.contains("program") -> courseCol = idx
                }
            }
            startIdx = 1
        }

        for (i in startIdx until nonEmpty.size) {
            val cells = nonEmpty[i]
            if (cells.size < 2) continue
            val rawId = cells.getOrNull(idCol)?.trim().orEmpty()
            val rawName = cells.getOrNull(nameCol)?.trim().orEmpty()
            val rawCourse = cells.getOrNull(courseCol)?.trim().orEmpty()
            if (rawId.isBlank() || rawName.isBlank()) continue
            if (rawId.count { it.isDigit() } < 4) continue
            out.add(ParsedRosterRow(id = rawId, name = rawName, course = rawCourse))
        }
        return out
    }

    private fun parseSharedStringsXml(xmlBytes: ByteArray): List<String> {
        val doc = secureDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        val siNodes = doc.getElementsByTagName("si")
        val list = ArrayList<String>(siNodes.length)
        for (i in 0 until siNodes.length) {
            val si = siNodes.item(i) as? Element ?: continue
            val tNodes = si.getElementsByTagName("t")
            val sb = StringBuilder()
            for (j in 0 until tNodes.length) {
                sb.append(tNodes.item(j).textContent.orEmpty())
            }
            list.add(sb.toString().trim())
        }
        return list
    }

    private fun parseWorksheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val doc = secureDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        val rowNodes = doc.getElementsByTagName("row")
        val grid = ArrayList<List<String>>(rowNodes.length)

        for (rIdx in 0 until rowNodes.length) {
            val rowEl = rowNodes.item(rIdx) as? Element ?: continue
            val cellNodes = rowEl.getElementsByTagName("c")
            if (cellNodes.length == 0) continue

            val rowValues = mutableListOf<String>()
            var fallbackCol = 0

            for (cIdx in 0 until cellNodes.length) {
                val cellEl = cellNodes.item(cIdx) as? Element ?: continue
                val ref = cellEl.getAttribute("r")
                val colIndex = if (ref.isNotBlank()) columnIndexFromRef(ref) else fallbackCol
                fallbackCol = colIndex + 1

                while (rowValues.size <= colIndex) {
                    rowValues.add("")
                }

                val cellType = cellEl.getAttribute("t")
                val value = when (cellType) {
                    "s" -> {
                        val idx = firstTagText(cellEl, "v").toIntOrNull()
                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                    }
                    "inlineStr" -> {
                        val tNodes = cellEl.getElementsByTagName("t")
                        val sb = StringBuilder()
                        for (k in 0 until tNodes.length) {
                            sb.append(tNodes.item(k).textContent.orEmpty())
                        }
                        sb.toString().trim()
                    }
                    "str", "b", "e" -> {
                        firstTagText(cellEl, "v").trim()
                    }
                    else -> {
                        normalizeNumericCell(firstTagText(cellEl, "v").trim())
                    }
                }

                rowValues[colIndex] = value
            }

            if (rowValues.any { it.isNotBlank() }) {
                grid.add(rowValues)
            }
        }
        return grid
    }

    private fun firstTagText(parent: Element, tagName: String): String {
        val list = parent.getElementsByTagName(tagName)
        return if (list.length > 0) list.item(0).textContent.orEmpty() else ""
    }

    private fun normalizeNumericCell(raw: String): String {
        if (raw.isBlank()) return ""
        val num = raw.toBigDecimalOrNull() ?: return raw
        return try {
            val stripped = num.stripTrailingZeros()
            if (stripped.scale() <= 0) stripped.toPlainString() else BigDecimal(raw).toPlainString()
        } catch (_: Exception) {
            raw
        }
    }

    internal fun columnIndexFromRef(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch in 'A'..'Z' || ch in 'a'..'z') {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun secureDocumentBuilder() = DocumentBuilderFactory.newInstance().apply {
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        isExpandEntityReferences = false
    }.newDocumentBuilder()

    private fun looksLikeHeader(cells: List<String>): Boolean {
        val joined = cells.joinToString(" ").lowercase()
        val hasDigits = cells.any { it.count { ch -> ch.isDigit() } >= 5 }
        if (hasDigits) return false
        return joined.contains("id") || joined.contains("name") || joined.contains("student")
    }

    internal fun splitCsvOrTsvLine(line: String): List<String> {
        if (line.contains("\t")) {
            return line.split("\t").map { it.trim().trim('"') }
        }
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
