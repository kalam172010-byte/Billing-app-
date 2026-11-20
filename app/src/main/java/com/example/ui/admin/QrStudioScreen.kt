package com.example.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.QrLabelSize
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils
import com.example.util.QrCodeUtils

/**
 * QR Studio (A4 Print Sheet): Select products, choose sticker size (38x25mm, 50x25mm, 70x35mm),
 * configure copies per product, preview scannable QR stickers (KBP-PROD:<4-digit>:<id>),
 * and print or download as A4 PDF.
 */
@Composable
fun QrStudioScreen(
    store: Store?,
    products: List<Product>,
    onPrintQrSheet: (List<Pair<Product, Int>>, QrLabelSize) -> Unit,
    onDownloadQrPdf: (List<Pair<Product, Int>>, QrLabelSize) -> Unit
) {
    val currency = store?.currencySymbol ?: "₹"
    var selectedSize by remember { mutableStateOf(QrLabelSize.MEDIUM_50X25) }
    val copyCounts = remember { mutableStateMapOf<String, Int>() }

    val selectedPairs = products.mapNotNull { p ->
        val count = copyCounts[p.id] ?: 0
        if (count > 0) p to count else null
    }
    val totalLabels = selectedPairs.sumOf { it.second }
    val previewProduct = selectedPairs.firstOrNull()?.first ?: products.firstOrNull()

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
                Text("QR Studio · A4 Print Sheet", style = MaterialTheme.typography.headlineMedium, color = Slate900)
                Text(
                    text = "Generate scannable KBP-PROD sticker sheets ($totalLabels stickers selected)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }

        // Sticker Size Picker (38x25mm, 50x25mm, 70x35mm)
        EditorialCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("STICKER LABEL DIMENSIONS", style = MaterialTheme.typography.labelSmall, color = Slate500)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QrLabelSize.entries.forEach { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { Text(size.label, style = TabularMonoSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Live Sticker Preview Card
                if (previewProduct != null) {
                    HorizontalDivider(color = Slate200)
                    Text("LIVE STICKER PREVIEW (${selectedSize.label})", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    StickerPreviewTile(product = previewProduct, currency = currency)
                }

                // Print & Download A4 PDF Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPrintQrSheet(selectedPairs, selectedSize) },
                        enabled = totalLabels > 0,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_studio_print_button")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                        Spacer(Modifier.width(6.dp))
                        Text("Print A4 Sheet")
                    }
                    OutlinedButton(
                        onClick = { onDownloadQrPdf(selectedPairs, selectedSize) },
                        enabled = totalLabels > 0,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("qr_studio_download_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = Slate900)
                        Spacer(Modifier.width(6.dp))
                        Text("A4 PDF", color = Slate900)
                    }
                }
            }
        }

        // Product Selection & Copies List
        if (products.isEmpty()) {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, tint = Slate500, modifier = Modifier.size(32.dp))
                    Text("No products available for QR stickers", style = MaterialTheme.typography.titleMedium, color = Slate900)
                    Text("Add products in Inventory first to generate scannable QR labels.", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val copies = copyCounts[product.id] ?: 0
                    EditorialCard(
                        borderColor = if (copies > 0) Emerald600 else Slate200,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MonoCodeBadge(code = "#${product.shortCode}", accent = copies > 0)
                                    Text(
                                        text = FormatUtils.formatMoney(product.sellingPrice, currency),
                                        style = TabularMonoSmall,
                                        color = Slate600
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (copies > 0) copyCounts[product.id] = copies - 1
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease copies", modifier = Modifier.size(16.dp), tint = Slate900)
                                    }
                                }
                                Text(
                                    text = copies.toString(),
                                    style = TabularMonoMedium,
                                    color = Slate900,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate900,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            copyCounts[product.id] = (copies + 1).coerceAtMost(100)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase copies", modifier = Modifier.size(16.dp), tint = PureWhite)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            copyCounts[product.id] = (copies + 10).coerceAtMost(100)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Text("+10", style = TabularMonoSmall, color = Slate900)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerPreviewTile(product: Product, currency: String) {
    val payload = product.qrPayload.ifBlank { "KBP-PROD:${product.shortCode}:${product.id}" }
    val qrBitmap: Bitmap? = remember(payload) { QrCodeUtils.generateQrBitmap(payload, 200) }
    DisposableEffect(qrBitmap) {
        onDispose { qrBitmap?.recycle() }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Preview QR sticker",
                    modifier = Modifier.size(72.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${product.shortCode}",
                    style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold),
                    color = Emerald700
                )
                Text(
                    text = FormatUtils.formatMoney(product.sellingPrice, currency),
                    style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
                Text(
                    text = payload,
                    style = TabularMonoSmall,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
