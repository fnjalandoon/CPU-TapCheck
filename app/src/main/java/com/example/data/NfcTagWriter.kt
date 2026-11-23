package com.example.data

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

enum class NfcWriteFormat {
    CompactText,
    WebUrl
}

sealed interface NfcWriteResult {
    data class Success(val payloadDescription: String, val lockedReadOnly: Boolean) : NfcWriteResult
    data class Failure(val message: String) : NfcWriteResult
}

object NfcTagWriter {

    fun buildPayloadString(subjectCode: String, format: NfcWriteFormat): String {
        val clean = subjectCode.trim().replace(Regex("\\s+"), " ")
        return when (format) {
            NfcWriteFormat.CompactText -> clean
            NfcWriteFormat.WebUrl -> Uri.parse(AttendanceConfig.WEB_APP_BASE_URL)
                .buildUpon()
                .appendQueryParameter("code", clean)
                .build()
                .toString()
        }
    }

    fun write(
        tag: Tag,
        subjectCode: String,
        format: NfcWriteFormat,
        lockReadOnly: Boolean
    ): NfcWriteResult {
        val clean = subjectCode.trim().replace(Regex("\\s+"), " ")
        if (clean.length < 2) {
            return NfcWriteResult.Failure("Subject code is too short to write.")
        }

        val payloadText = buildPayloadString(clean, format)
        val record = when (format) {
            NfcWriteFormat.CompactText -> NdefRecord.createTextRecord("en", payloadText)
            NfcWriteFormat.WebUrl -> NdefRecord.createUri(payloadText)
        }
        val message = NdefMessage(arrayOf(record))
        val messageBytes = message.toByteArray().size

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable) {
                    return NfcWriteResult.Failure("This NFC card is already locked as read-only.")
                }
                if (ndef.maxSize < messageBytes) {
                    return NfcWriteResult.Failure(
                        "Card memory is too small (${ndef.maxSize} bytes, needs $messageBytes bytes). Try Compact Text format instead."
                    )
                }
                ndef.writeNdefMessage(message)
                var locked = false
                if (lockReadOnly && ndef.canMakeReadOnly()) {
                    locked = runCatching { ndef.makeReadOnly() }.getOrDefault(false)
                }
                NfcWriteResult.Success(payloadText, locked)
            } catch (e: Exception) {
                NfcWriteResult.Failure("Could not write to card. Hold your phone steady and try again.")
            } finally {
                runCatching { ndef.close() }
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return try {
                formatable.connect()
                if (lockReadOnly) {
                    formatable.formatReadOnly(message)
                    NfcWriteResult.Success(payloadText, true)
                } else {
                    formatable.format(message)
                    NfcWriteResult.Success(payloadText, false)
                }
            } catch (e: Exception) {
                NfcWriteResult.Failure("Could not format this NFC card. Hold steady and try again.")
            } finally {
                runCatching { formatable.close() }
            }
        }

        return NfcWriteResult.Failure("This NFC card does not support NDEF writing.")
    }
}
