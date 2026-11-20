package com.example.ui.pos

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Sale
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoLarge
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils
import com.example.util.QrCodeUtils

/**
 * Shown immediately after clicking "Generate Bill". NEVER auto-prints.
 * Provides explicit buttons for Print Receipt, Download PDF Bill, Download Receipt Image,
 * Instant Customer Bill QR Code + Digital Receipt Preview, and Tap NFC to Send Bill.
 */
@Composable
fun BillGeneratedDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onPrintReceipt: (Sale) -> Unit,
    onDownloadPdf: (Sale) -> Unit,
    onDownloadImage: (Sale) -> Unit,
    onOpenDigitalBillPreview: (Sale) -> Unit,
    onOpenNfcShare: (Sale) -> Unit
) {
    val qrBitmap: Bitmap? = remember(sale.digitalBillUrl) {
        QrCodeUtils.generateQrBitmap(sale.digitalBillUrl, 420)
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald50
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald600,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Bill Generated Successfully",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate900
                            )
                            Text(
                                text = "${sale.invoiceNumber} · ${sale.paymentMethod}",
                                style = TabularMonoSmall,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Slate600)
                    }
                }

                // Grand Total Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL BILLED (${FormatUtils.formatQty(sale.totalUnits)} units)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                            Text(
                                text = FormatUtils.formatMoney(sale.grandTotal, sale.currencySymbol),
                                style = TabularMonoLarge,
                                color = Emerald700
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MonoCodeBadge(code = "/?b=${sale.shortBillId}", accent = true)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "GST ${FormatUtils.formatMoney(sale.gstTotal, sale.currencySymbol)}",
                                style = TabularMonoSmall,
                                color = Slate600
                            )
                        }
                    }
                }

                // Instant Customer Bill QR Code
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Instant Customer Bill QR Code",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate900
                        )
                        Text(
                            text = "Customer scans with any phone camera to view & download PDF bill with zero login.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PureWhite,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_qr_code_image),
                                    modifier = Modifier
                                        .size(180.dp)
                                        .padding(10.dp)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onOpenDigitalBillPreview(sale) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("preview_digital_bill_button")
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate900)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_preview_digital_bill), color = Slate900)
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Explicit Action Buttons (No auto-print)
                Button(
                    onClick = { onPrintReceipt(sale) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("bill_modal_print_button")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${stringResource(R.string.action_print_receipt)} (${sale.thermalWidthMm}mm / A4)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onOpenNfcShare(sale) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("bill_modal_nfc_button")
                ) {
                    Icon(Icons.Filled.Contactless, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_tap_nfc),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDownloadPdf(sale) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("bill_modal_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF Bill", color = Slate900, style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = { onDownloadImage(sale) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("bill_modal_image_button")
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Image PNG", color = Slate900, style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("bill_modal_new_sale_button")
                ) {
                    Text("Start Next Sale", color = Slate900, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
