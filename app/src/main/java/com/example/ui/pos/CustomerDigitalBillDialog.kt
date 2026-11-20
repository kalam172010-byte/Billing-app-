package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Sale
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoLarge
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils

/**
 * Clean, login-free Customer Digital Receipt view (/?b=XXXXXX) showing store details,
 * itemized list, GST, totals, and 1-click Download PDF Bill button.
 */
@Composable
fun CustomerDigitalBillDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onDownloadPdf: (Sale) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald50
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                            Text("Digital Tax Receipt", style = MaterialTheme.typography.labelMedium, color = Emerald700)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Close, contentDescription = "Close receipt", tint = Slate600)
                    }
                }

                // Store Identity Block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (sale.receiptHeader.isNotBlank()) {
                        Text(sale.receiptHeader, style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                    Text(
                        text = sale.storeName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    if (sale.storeAddress.isNotBlank()) {
                        Text(
                            text = sale.storeAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600,
                            textAlign = TextAlign.Center
                        )
                    }
                    val meta = listOfNotNull(
                        sale.storePhone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
                        sale.storeGstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" }
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = TabularMonoSmall, color = Slate600, textAlign = TextAlign.Center)
                    }
                }

                HorizontalDivider(color = Slate200)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("INVOICE NO.", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Text(sale.invoiceNumber, style = TabularMonoMedium, color = Slate900)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DATE & TIME", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Text(
                            FormatUtils.formatDateTime(sale.createdAt?.toDate()),
                            style = TabularMonoSmall,
                            color = Slate900
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoCodeBadge(code = "/?b=${sale.shortBillId}", accent = true)
                    MonoCodeBadge(code = "PAID · ${sale.paymentMethod}")
                }

                HorizontalDivider(color = Slate200)

                // Itemized Table
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ITEM", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text("AMOUNT", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                        HorizontalDivider(color = Slate200)
                        sale.items.forEach { li ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(li.name, style = MaterialTheme.typography.titleSmall, color = Slate900)
                                    Text(
                                        text = "${FormatUtils.formatQty(li.quantity)} ${li.unit} × ${FormatUtils.formatMoney(li.unitPrice, sale.currencySymbol)} (GST ${FormatUtils.formatQty(li.gstPercent)}%)",
                                        style = TabularMonoSmall,
                                        color = Slate500
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatMoney(li.lineTotal, sale.currencySymbol),
                                    style = TabularMonoMedium,
                                    color = Slate900
                                )
                            }
                        }
                    }
                }

                // Totals Block
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taxable Subtotal", style = MaterialTheme.typography.bodyMedium, color = Slate600)
                        Text(FormatUtils.formatMoney(sale.subtotal, sale.currencySymbol), style = TabularMonoMedium, color = Slate900)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total GST", style = MaterialTheme.typography.bodyMedium, color = Slate600)
                        Text(FormatUtils.formatMoney(sale.gstTotal, sale.currencySymbol), style = TabularMonoMedium, color = Slate900)
                    }
                    if (sale.discountAmount > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount", style = MaterialTheme.typography.bodyMedium, color = Emerald700)
                            Text("-${FormatUtils.formatMoney(sale.discountAmount, sale.currencySymbol)}", style = TabularMonoMedium, color = Emerald700)
                        }
                    }
                    HorizontalDivider(color = Slate200)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total", style = MaterialTheme.typography.titleMedium, color = Slate900)
                        Text(
                            text = FormatUtils.formatMoney(sale.grandTotal, sale.currencySymbol),
                            style = TabularMonoLarge,
                            color = Emerald700
                        )
                    }
                }

                if (sale.storeUpiId.isNotBlank()) {
                    Text(
                        text = "Merchant UPI: ${sale.storeUpiId}",
                        style = TabularMonoSmall,
                        color = Slate600,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                if (sale.receiptFooter.isNotBlank()) {
                    Text(
                        text = sale.receiptFooter,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onDownloadPdf(sale) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("digital_bill_download_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download PDF Bill", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
