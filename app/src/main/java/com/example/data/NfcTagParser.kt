package com.example.data

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import java.nio.charset.Charset

sealed interface TagParseResult {
    data class ValidCode(val classCode: String) : TagParseResult
    data object InvalidOrEmptyTag : TagParseResult
    data object NotAnNfcIntent : TagParseResult
}

object NfcTagParser {

    private val validCodeRegex = Regex("^[A-Za-z0-9 _.()/-]{2,50}$")

    fun extractClassCode(intent: Intent?): TagParseResult {
        if (intent == null) return TagParseResult.NotAnNfcIntent
        val action = intent.action

        val isNfcAction = action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED

        if (!isNfcAction && action != Intent.ACTION_VIEW) {
            return TagParseResult.NotAnNfcIntent
        }

        // 1. Check NDEF messages attached to the intent
        val ndefMessages = readNdefMessagesFromIntent(intent)
        for (message in ndefMessages) {
            extractFromNdefMessage(message)?.let { code ->
                return TagParseResult.ValidCode(code)
            }
        }

        // 2. Fallback: read cached NDEF directly from Tag if present
        val tag = readTagFromIntent(intent)
        if (tag != null) {
            val cached = runCatching { Ndef.get(tag)?.cachedNdefMessage }.getOrNull()
            if (cached != null) {
                extractFromNdefMessage(cached)?.let { code ->
                    return TagParseResult.ValidCode(code)
                }
            }
        }

        // 3. Check Intent URI data (both ACTION_VIEW and NDEF URI launches)
        intent.data?.let { uri ->
            extractCodeFromUri(uri)?.let { code ->
                return TagParseResult.ValidCode(code)
            }
        }

        return if (isNfcAction) {
            TagParseResult.InvalidOrEmptyTag
        } else {
            TagParseResult.NotAnNfcIntent
        }
    }

    fun extractFromNdefMessage(message: NdefMessage): String? {
        for (record in message.records) {
            extractFromNdefRecord(record)?.let { return it }
        }
        return null
    }

    fun extractFromNdefRecord(record: NdefRecord): String? {
        when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    record.toUri()?.let { uri ->
                        extractCodeFromUri(uri)?.let { return it }
                    }
                } else if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val text = decodeTextPayload(record.payload)
                    if (!text.isNullOrBlank()) {
                        parseClassCodeString(text)?.let { return it }
                    }
                }
            }
            NdefRecord.TNF_ABSOLUTE_URI -> {
                val uri = record.toUri() ?: runCatching {
                    Uri.parse(String(record.payload, Charsets.UTF_8))
                }.getOrNull()
                if (uri != null) {
                    extractCodeFromUri(uri)?.let { return it }
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                val text = runCatching { String(record.payload, Charsets.UTF_8) }.getOrNull()
                if (!text.isNullOrBlank()) {
                    parseClassCodeString(text)?.let { return it }
                }
            }
            NdefRecord.TNF_EXTERNAL_TYPE -> {
                val text = runCatching { String(record.payload, Charsets.UTF_8) }.getOrNull()
                if (!text.isNullOrBlank()) {
                    parseClassCodeString(text)?.let { return it }
                }
            }
        }
        return null
    }

    fun parseClassCodeString(raw: String): String? {
        val trimmed = raw.trim()
            .replace("\u0000", "")
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
            return uri?.let { extractCodeFromUri(it) }
        }

        if (trimmed.startsWith("code=", ignoreCase = true)) {
            val candidate = trimmed.substringAfter("=", "").trim()
            return candidate.takeIf { validCodeRegex.matches(it) }
        }

        return trimmed.takeIf { validCodeRegex.matches(it) }
    }

    fun extractCodeFromUri(uri: Uri): String? {
        val codeParam = runCatching { uri.getQueryParameter("code") }.getOrNull()
            ?.trim()
            ?.replace(Regex(" {2,}"), " ")
        return codeParam?.takeIf { validCodeRegex.matches(it) }
    }

    internal fun decodeTextPayload(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val statusByte = payload[0].toInt()
        val isUtf16 = (statusByte and 0x80) != 0
        val langLength = statusByte and 0x3F
        val textStart = 1 + langLength
        if (textStart > payload.size) return null
        val charset: Charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
        return String(payload, textStart, payload.size - textStart, charset)
    }

    @Suppress("DEPRECATION")
    private fun readNdefMessagesFromIntent(intent: Intent): List<NdefMessage> {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES,
                NdefMessage::class.java
            )
        } else {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        } ?: return emptyList()

        return raw.mapNotNull { it as? NdefMessage }
    }

    @Suppress("DEPRECATION")
    private fun readTagFromIntent(intent: Intent): Tag? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }
}
