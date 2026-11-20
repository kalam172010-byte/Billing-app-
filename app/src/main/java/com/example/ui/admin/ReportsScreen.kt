package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Sale
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils

@Composable
fun ReportsScreen(
    store: Store?,
    sales: List<Sale>,
    onViewDigitalBill: (Sale) -> Unit,
    onPrintReceipt: (Sale) -> Unit,
    onDownloadPdf: (Sale) -> Unit,
    onOpenNfcShare: (Sale) -> Unit,
    onExportExcel: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredSales = remember(sales, query) {
        val q = query.trim()
        if (q.isEmpty()) sales
        else sales.filter {
            it.invoiceNumber.contains(q, ignoreCase = true) ||
                it.shortBillId.contains(q, ignoreCase = true) ||
                it.paymentMethod.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Invoice History & GST Reports", style = MaterialTheme.typography.headlineMedium, color = Slate900)
                Text(
                    text = "${sales.size} invoices in ${store?.name ?: "Branch"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
            Button(
                onClick = onExportExcel,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("reports_export_excel_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                Spacer(Modifier.width(6.dp))
                Text("Excel .xlsx")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search invoice number, short bill ID, or payment mode...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Slate500) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredSales.isEmpty()) {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Slate500, modifier = Modifier.size(34.dp))
                    Text("No invoices recorded yet", style = MaterialTheme.typography.titleMedium, color = Slate900)
                    Text("Completed bills from the POS Counter appear here with reprint, PDF, and NFC actions.", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredSales, key = { it.id }) { sale ->
                    EditorialCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(sale.invoiceNumber, style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold), color = Slate900)
                                        MonoCodeBadge(code = "#${sale.shortBillId}", accent = true)
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "${FormatUtils.formatDateTime(sale.createdAt?.toDate())} · ${sale.paymentMethod} · ${FormatUtils.formatQty(sale.totalUnits)} units",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FormatUtils.formatMoney(sale.grandTotal, sale.currencySymbol),
                                        style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Emerald700
                                    )
                                    Text(
                                        text = "GST ${FormatUtils.formatMoney(sale.gstTotal, sale.currencySymbol)}",
                                        style = TabularMonoSmall,
                                        color = Slate500
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate200)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onViewDigitalBill(sale) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Slate200),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate900)
                                    Spacer(Modifier.width(4.dp))
                                    Text("View", style = MaterialTheme.typography.labelMedium, color = Slate900)
                                }
                                OutlinedButton(
                                    onClick = { onPrintReceipt(sale) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Slate200),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate900)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Print", style = MaterialTheme.typography.labelMedium, color = Slate900)
                                }
                                OutlinedButton(
                                    onClick = { onDownloadPdf(sale) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Slate200),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate900)
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF", style = MaterialTheme.typography.labelMedium, color = Slate900)
                                }
                                OutlinedButton(
                                    onClick = { onOpenNfcShare(sale) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Filled.Contactless, contentDescription = null, modifier = Modifier.size(15.dp), tint = Emerald700)
                                    Spacer(Modifier.width(4.dp))
                                    Text("NFC", style = MaterialTheme.typography.labelMedium, color = Emerald700)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
