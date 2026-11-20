package com.example.nfc

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.cardemulation.HostApduService
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

data class NfcDiagnosticResult(
    val hardwarePresent: Boolean,
    val nfcEnabled: Boolean,
    val hceSupported: Boolean,
    val activeMode: String,
    val latencyMs: Long,
    val statusSummary: String
)

data class NfcTapEvent(
    val url: String,
    val transport: String,
    val timestampMs: Long
)

/**
 * Shared runtime bridge between Compose UI and native Android HostApduService (HCE)
 * + NFC Tag Writer (NfcAdapter.enableReaderMode).
 */
object NfcHceBridge {
    private val _armedBillUrl = MutableStateFlow<String?>(null)
    val armedBillUrl: StateFlow<String?> = _armedBillUrl.asStateFlow()

    private val _tapSuccessEvent = MutableStateFlow<NfcTapEvent?>(null)
    val tapSuccessEvent: StateFlow<NfcTapEvent?> = _tapSuccessEvent.asStateFlow()

    private val _lastTagWriteError = MutableStateFlow<String?>(null)
    val lastTagWriteError: StateFlow<String?> = _lastTagWriteError.asStateFlow()

    fun armBillForNfcTap(billUrl: String) {
        _lastTagWriteError.value = null
        _tapSuccessEvent.value = null
        _armedBillUrl.value = billUrl
    }

    fun disarmNfcTap() {
        _armedBillUrl.value = null
        _lastTagWriteError.value = null
    }

    fun notifyTapSuccess(url: String, transport: String) {
        _tapSuccessEvent.value = NfcTapEvent(
            url = url,
            transport = transport,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun runTransmitterDiagnostics(context: Context): NfcDiagnosticResult {
        val start = SystemClock.elapsedRealtimeNanos()
        val pm = context.packageManager
        val hasNfcFeature = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
        val hasHceFeature = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        val adapter = NfcAdapter.getDefaultAdapter(context)
        val enabled = adapter?.isEnabled == true
        val elapsedMs = ((SystemClock.elapsedRealtimeNanos() - start) / 1_000_000L).coerceAtLeast(1L)

        val mode = when {
            !hasNfcFeature || adapter == null -> "Unavailable (No NFC Hardware)"
            !enabled -> "NFC Radio Disabled in System Settings"
            hasHceFeature -> "Native Android HCE (Type 4 NDEF) + Tag Writer"
            else -> "NFC Tag Writer (Reader/Writer Mode)"
        }
        val summary = when {
            !hasNfcFeature || adapter == null -> "This device/emulator does not expose an NFC radio chip. Use the QR code or run on an NFC-equipped Android phone."
            !enabled -> "NFC radio is turned off. Enable NFC in Android Settings -> Connected devices -> Connection preferences."
            else -> "NFC transmitter ready. Hold customer's phone back-to-back (1-3 cm) or tap an NTAG213/215 sticker."
        }
        return NfcDiagnosticResult(
            hardwarePresent = hasNfcFeature && adapter != null,
            nfcEnabled = enabled,
            hceSupported = hasHceFeature,
            activeMode = mode,
            latencyMs = elapsedMs,
            statusSummary = summary
        )
    }

    fun writeArmedUrlToTag(tag: Tag) {
        val url = _armedBillUrl.value ?: return
        try {
            val ndefRecord = NdefRecord.createUri(url)
            val ndefMessage = NdefMessage(arrayOf(ndefRecord))
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    _lastTagWriteError.value = "NFC tag is read-only."
                    ndef.close()
                    return
                }
                if (ndef.maxSize < ndefMessage.toByteArray().size) {
                    _lastTagWriteError.value = "NFC tag capacity too small (${ndef.maxSize} bytes)."
                    ndef.close()
                    return
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                notifyTapSuccess(url, "NFC Tag Written")
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    notifyTapSuccess(url, "NFC Tag Formatted & Written")
                } else {
                    _lastTagWriteError.value = "Tag does not support NDEF format."
                }
            }
        } catch (e: Exception) {
            _lastTagWriteError.value = e.localizedMessage ?: "Failed to write NFC tag."
        }
    }

    fun buildExportableBridgeCode(sampleUrl: String): String = """
// =========================================================================
// KALAM BILLING PRO — Native Android NFC Host Card Emulation (HCE) Bridge
// Pre-configured Bill URL: $sampleUrl
// =========================================================================
//
// 1. AndroidManifest.xml:
// <uses-permission android:name="android.permission.NFC" />
// <uses-feature android:name="android.hardware.nfc.hce" android:required="false" />
// <service android:name=".nfc.KalamNfcHceService"
//     android:exported="true"
//     android:permission="android.permission.BIND_NFC_SERVICE">
//     <intent-filter>
//         <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
//     </intent-filter>
//     <meta-data android:name="android.nfc.cardemulation.host_apdu_service"
//         android:resource="@xml/aid_list" />
// </service>
//
// 2. res/xml/aid_list.xml:
// <host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
//     android:description="@string/nfc_hce_service_desc"
//     android:requireDeviceUnlock="false">
//     <aid-group android:category="other" android:description="@string/nfc_hce_aid_group_desc">
//         <aid-filter android:name="D2760000850101" />
//     </aid-group>
// </host-apdu-service>
//
// 3. KalamNfcHceService.kt:
// Implements NFC Forum Type 4 Tag NDEF emulation serving NDEF URI records
// directly to Android and iPhone NFC readers with zero share-sheet fallback.
    """.trimIndent()
}

/**
 * Native Android Host Card Emulation (HCE) Service implementing NFC Forum Type 4 Tag
 * NDEF specification (AID D2760000850101) to beam digital bill URIs phone-to-phone.
 */
class KalamNfcHceService : HostApduService() {

    private var selectedFile = 0

    companion object {
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SELECT_NDEF_APP = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
            0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01, 0x00
        )
        private val SELECT_CC_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x03
        )
        private val SELECT_NDEF_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04
        )
        // NFC Forum Type 4 Capability Container (15 bytes)
        private val CAPABILITY_CONTAINER = byteArrayOf(
            0x00, 0x0F, // CCLEN = 15
            0x20,       // Mapping Version 2.0
            0x00, 0xFF.toByte(), // MLe max R-APDU
            0x00, 0xFF.toByte(), // MLc max C-APDU
            0x04, 0x06, // NDEF File Control TLV
            0xE1.toByte(), 0x04, // File ID E104
            0x0F, 0xFF.toByte(), // Max NDEF size 4095 bytes
            0x00,       // Read access granted
            0xFF.toByte() // Write access denied (read-only tag)
        )
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_NOT_FOUND
        val armedUrl = NfcHceBridge.armedBillUrl.value ?: return SW_NOT_FOUND

        // SELECT NDEF Tag Application
        if (apdu.size >= 12 && matchesPrefix(apdu, SELECT_NDEF_APP, 12)) {
            selectedFile = 0
            return SW_OK
        }
        // SELECT Capability Container (E103)
        if (apdu.contentEquals(SELECT_CC_FILE)) {
            selectedFile = 1
            return SW_OK
        }
        // SELECT NDEF File (E104)
        if (apdu.contentEquals(SELECT_NDEF_FILE)) {
            selectedFile = 2
            return SW_OK
        }
        // READ BINARY (CLA=00, INS=B0)
        if (apdu.size >= 5 && apdu[0] == 0x00.toByte() && apdu[1] == 0xB0.toByte()) {
            val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
            val le = apdu[4].toInt() and 0xFF
            val dataSource = when (selectedFile) {
                1 -> CAPABILITY_CONTAINER
                2 -> buildNdefFileBytes(armedUrl)
                else -> return SW_NOT_FOUND
            }
            if (offset >= dataSource.size) return SW_OK
            val length = minOf(if (le == 0) 255 else le, dataSource.size - offset)
            val slice = dataSource.copyOfRange(offset, offset + length)
            if (selectedFile == 2 && offset + length >= dataSource.size) {
                NfcHceBridge.notifyTapSuccess(armedUrl, "Android HCE Phone-to-Phone Tap")
            }
            return slice + SW_OK
        }
        return SW_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = 0
    }

    private fun buildNdefFileBytes(url: String): ByteArray {
        val record = NdefRecord.createUri(url)
        val messageBytes = NdefMessage(arrayOf(record)).toByteArray()
        val len = messageBytes.size
        val nlen = byteArrayOf(((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte())
        return nlen + messageBytes
    }

    private fun matchesPrefix(a: ByteArray, b: ByteArray, len: Int): Boolean {
        for (i in 0 until len) {
            if (a[i] != b[i]) return false
        }
        return true
    }
}
