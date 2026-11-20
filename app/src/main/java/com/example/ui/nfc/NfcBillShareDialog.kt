package com.example.ui.nfc

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Sale
import com.example.nfc.NfcDiagnosticResult
import com.example.nfc.NfcHceBridge
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose700
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils

/**
 * Dedicated NFC Bill Share Modal (HCE Phone-to-Phone + NDEF Tag Writer).
 * Strictly NO OS Share Sheet / WhatsApp / Quick Share fallback.
 * Includes live HCE arming, Test NFC Connection diagnostic tool (ms latency),
 * Phone Positioning Guide (Android center-back vs iPhone top-rear edge),
 * and 1-click Export Android NFC HCE Bridge Code.
 */
@Composable
fun NfcBillShareDialog(
    sale: Sale,
    onExportBridgeCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tapEvent by NfcHceBridge.tapSuccessEvent.collectAsStateWithLifecycle()
    val tagWriteError by NfcHceBridge.lastTagWriteError.collectAsStateWithLifecycle()
    var diagnostic by remember { mutableStateOf<NfcDiagnosticResult?>(null) }

    DisposableEffect(sale.digitalBillUrl) {
        NfcHceBridge.armBillForNfcTap(sale.digitalBillUrl)
        diagnostic = NfcHceBridge.runTransmitterDiagnostics(context)
        onDispose {
            NfcHceBridge.disarmNfcTap()
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "nfcPulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Contactless, contentDescription = null, tint = Emerald600)
                        Column {
                            Text("Tap NFC to Send Bill", style = MaterialTheme.typography.titleLarge, color = Slate900)
                            Text("Native Android HCE + NDEF Tag Writer", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Close, contentDescription = "Close NFC modal", tint = Slate600)
                    }
                }

                // Active HCE Transmitter Visual Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (tapEvent != null) Emerald50 else Slate900,
                    border = BorderStroke(1.dp, if (tapEvent != null) Emerald600 else Slate900),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .scale(if (tapEvent != null) 1f else pulseScale)
                                .background(
                                    if (tapEvent != null) Emerald600 else Emerald600.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tapEvent != null) Icons.Filled.CheckCircle else Icons.Filled.Contactless,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = if (tapEvent != null) "NFC Tap Transmitted!" else "NFC HCE Armed & Broadcasting",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (tapEvent != null) Emerald700 else PureWhite
                        )
                        Text(
                            text = if (tapEvent != null) {
                                "${tapEvent!!.transport} · Bill #${sale.shortBillId} delivered"
                            } else {
                                "Invoice ${sale.invoiceNumber} · ${FormatUtils.formatMoney(sale.grandTotal, sale.currencySymbol)}"
                            },
                            style = TabularMonoSmall,
                            color = if (tapEvent != null) Emerald700 else Slate200
                        )
                        MonoCodeBadge(code = "AID D2760000850101 · /?b=${sale.shortBillId}", accent = tapEvent != null)
                    }
                }

                if (tagWriteError != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Rose50,
                        border = BorderStroke(1.dp, Rose700.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Rose700, modifier = Modifier.size(18.dp))
                            Text(tagWriteError!!, style = MaterialTheme.typography.bodySmall, color = Rose700)
                        }
                    }
                }

                // Test NFC Connection Diagnostic Tool
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NFC Transmitter Diagnostics", style = MaterialTheme.typography.titleSmall, color = Slate900)
                            OutlinedButton(
                                onClick = { diagnostic = NfcHceBridge.runTransmitterDiagnostics(context) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.testTag("test_nfc_connection_button")
                            ) {
                                Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate900)
                                Spacer(Modifier.width(4.dp))
                                Text("Test NFC Connection", style = MaterialTheme.typography.labelMedium, color = Slate900)
                            }
                        }

                        diagnostic?.let { d ->
                            DiagnosticRow("NFC Radio Hardware", if (d.hardwarePresent) "Detected" else "Not Present", d.hardwarePresent)
                            DiagnosticRow("Radio Power State", if (d.nfcEnabled) "Enabled" else "Off / Unavailable", d.nfcEnabled)
                            DiagnosticRow("HCE Card Emulation", if (d.hceSupported) "Supported" else "Unavailable", d.hceSupported)
                            DiagnosticRow("Active Mode", d.activeMode, d.nfcEnabled)
                            DiagnosticRow("Response Latency", "${d.latencyMs} ms", true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (d.nfcEnabled) Emerald50 else Amber50,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = d.statusSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (d.nfcEnabled) Emerald700 else Amber700,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Phone Positioning Guide
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Phone Positioning Guide (1–3 cm · Hold 1–2s)", style = MaterialTheme.typography.titleSmall, color = Slate900)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate50,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = Emerald600)
                                    Text("Android Phones", style = MaterialTheme.typography.labelLarge, color = Slate900)
                                    Text(
                                        "Align center-back to center-back within 1–3 cm. Hold steady 1–2 seconds.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate50,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.PhoneIphone, contentDescription = null, tint = Slate900)
                                    Text("iPhone Customers", style = MaterialTheme.typography.labelLarge, color = Slate900)
                                    Text(
                                        "Tap customer's top-rear camera edge to your phone's NFC sweet spot.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Export Native Android APK NFC Bridge Code Button
                OutlinedButton(
                    onClick = { onExportBridgeCode(NfcHceBridge.buildExportableBridgeCode(sale.digitalBillUrl)) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_nfc_bridge_code_button")
                ) {
                    Icon(Icons.Filled.Code, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download Android APK NFC Bridge Code", color = Slate900, style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, healthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate600)
        Text(
            text = value,
            style = TabularMonoSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (healthy) Emerald700 else Amber700
        )
    }
}
