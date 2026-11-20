package com.example.ui.scanner

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.ScannerRoom
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.components.MonoCodeBadge
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
import com.example.ui.theme.TabularMonoLarge
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils
import com.example.util.QrCodeUtils

/**
 * Host Terminal Pairing Modal showing persistent 4-digit Room Code (#XXXX),
 * pairing QR code (KBP-PAIR:XXXX:storeId), connected device banner, and remote disconnect.
 */
@Composable
fun WirelessTerminalRoomDialog(
    roomCode: String,
    store: Store?,
    scannerRoom: ScannerRoom?,
    onDisconnectRemote: () -> Unit,
    onSwitchToHandheldMode: () -> Unit,
    onDismiss: () -> Unit
) {
    val pairingPayload = "KBP-PAIR:$roomCode:${store?.id.orEmpty()}"
    val qrBitmap: Bitmap? = remember(pairingPayload) {
        QrCodeUtils.generateQrBitmap(pairingPayload, 420)
    }
    DisposableEffect(qrBitmap) {
        onDispose { qrBitmap?.recycle() }
    }

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
                        Icon(Icons.Filled.WifiTethering, contentDescription = null, tint = Emerald600)
                        Column {
                            Text("Wireless Scanner Hub", style = MaterialTheme.typography.titleLarge, color = Slate900)
                            Text("Real-time Phone-to-Terminal QR Sync", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Slate600)
                    }
                }

                // Terminal Code Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("TERMINAL ROOM CODE", style = MaterialTheme.typography.labelSmall, color = Slate200)
                        Text("#$roomCode", style = TabularMonoLarge.copy(fontWeight = FontWeight.Bold), color = Emerald600)
                        Text(
                            text = "Branch: ${store?.name ?: "Default Store"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate200
                        )
                    }
                }

                // Pairing QR Code
                if (qrBitmap != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PureWhite,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Wireless scanner pairing QR code",
                                modifier = Modifier
                                    .size(188.dp)
                                    .padding(12.dp)
                            )
                        }
                        Text(
                            text = "Open Kalam Billing Pro on a second phone → Handheld Scanner Mode → scan this QR or enter #$roomCode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }

                // Connection Status
                val isConnected = scannerRoom?.scannerConnected == true
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isConnected) Emerald50 else Slate50,
                    border = BorderStroke(1.dp, if (isConnected) Emerald600 else Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isConnected) Emerald700 else Slate500
                            )
                            Column {
                                Text(
                                    text = if (isConnected) "Connected: ${scannerRoom?.connectedScannerName}" else "Waiting for Handheld Scanner...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate900
                                )
                                if (!scannerRoom?.lastScanCode.isNullOrBlank()) {
                                    Text(
                                        text = "Last scan: ${scannerRoom?.lastScanCode}",
                                        style = TabularMonoSmall,
                                        color = Emerald700
                                    )
                                }
                            }
                        }
                        if (isConnected) {
                            OutlinedButton(
                                onClick = onDisconnectRemote,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Rose700.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Filled.LinkOff, contentDescription = null, tint = Rose700, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Disconnect", color = Rose700, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                Button(
                    onClick = {
                        onDismiss()
                        onSwitchToHandheldMode()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("switch_to_handheld_scanner_button")
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use This Phone as Handheld Scanner")
                }
            }
        }
    }
}

/**
 * Handheld Phone Scanner Mode: Pair with a terminal room (#XXXX) via QR or 4-digit code,
 * view the synced product catalog, and stream scanned QR codes or quick taps straight to the POS cart.
 */
@Composable
fun HandheldScannerScreen(
    pairedRoom: ScannerRoom?,
    store: Store?,
    products: List<Product>,
    onConnectRoom: (String) -> Unit,
    onDisconnectRoom: () -> Unit,
    onSendCodeToTerminal: (String) -> Unit
) {
    var roomCodeInput by remember { mutableStateOf("") }
    var quickCodeInput by remember { mutableStateOf("") }
    var showPairQrScanner by remember { mutableStateOf(false) }
    var showProductQrScanner by remember { mutableStateOf(false) }
    val currency = store?.currencySymbol ?: "₹"

    if (showPairQrScanner) {
        CameraQrScannerDialog(
            title = "Scan Terminal Pairing QR",
            subtitle = "Point at the Host Terminal's Wireless Scanner Hub QR",
            onCodeScanned = { payload ->
                showPairQrScanner = false
                onConnectRoom(payload)
            },
            onDismiss = { showPairQrScanner = false }
        )
    }

    if (showProductQrScanner) {
        CameraQrScannerDialog(
            title = "Handheld Product Scanner",
            subtitle = "Every scanned QR streams directly into Room #${pairedRoom?.roomCode}",
            onCodeScanned = { payload ->
                onSendCodeToTerminal(payload)
            },
            onDismiss = { showProductQrScanner = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Handheld Scanner Mode", style = MaterialTheme.typography.titleLarge, color = Slate900)
                        Text(
                            text = if (pairedRoom != null) "Paired with Terminal #${pairedRoom.roomCode}" else "Connect to a POS Terminal Room",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pairedRoom != null) Emerald700 else Slate500
                        )
                    }
                    if (pairedRoom != null) {
                        OutlinedButton(
                            onClick = onDisconnectRoom,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Rose700.copy(alpha = 0.4f))
                        ) {
                            Text("Unpair", color = Rose700, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (pairedRoom == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.filter { c -> c.isDigit() }.take(6) },
                            placeholder = { Text("4-digit room code (e.g. 4829)", style = TabularMonoSmall) },
                            textStyle = TabularMonoMedium,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("handheld_room_code_input")
                        )
                        Button(
                            onClick = { onConnectRoom(roomCodeInput) },
                            enabled = roomCodeInput.length >= 4,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Pair")
                        }
                    }
                    OutlinedButton(
                        onClick = { showPairQrScanner = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Emerald600),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Emerald700)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Terminal Pairing QR", color = Emerald700, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = { showProductQrScanner = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = PureWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("handheld_launch_camera_button")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Continuous QR Camera", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickCodeInput,
                            onValueChange = { quickCodeInput = it },
                            placeholder = { Text("Send 4-digit product code...", style = TabularMonoSmall) },
                            textStyle = TabularMonoMedium,
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (quickCodeInput.isNotBlank()) {
                                    onSendCodeToTerminal(quickCodeInput)
                                    quickCodeInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send code", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Synced Product Catalog Quick-Send List
        Text(
            text = "SYNCED STORE CATALOG (${products.size} items) · Tap to stream to POS Cart",
            style = MaterialTheme.typography.labelSmall,
            color = Slate500,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(products, key = { it.id }) { product ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = pairedRoom != null) {
                            onSendCodeToTerminal(product.qrPayload.ifBlank { product.shortCode })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            MonoCodeBadge(code = "#${product.shortCode}", accent = true)
                            Column {
                                Text(product.name, style = MaterialTheme.typography.titleSmall, color = Slate900)
                                Text(
                                    text = FormatUtils.formatMoney(product.sellingPrice, currency),
                                    style = TabularMonoSmall,
                                    color = Slate500
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Stream to POS",
                            tint = if (pairedRoom != null) Emerald600 else Slate200,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
