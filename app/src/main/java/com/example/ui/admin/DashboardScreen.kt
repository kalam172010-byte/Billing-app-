package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber700
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
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils
import java.util.Calendar
import java.util.Date

@Composable
fun DashboardScreen(
    store: Store?,
    products: List<Product>,
    sales: List<Sale>,
    onExportExcel: () -> Unit,
    onNavigateProducts: () -> Unit,
    onNavigateReports: () -> Unit
) {
    val currency = store?.currencySymbol ?: "₹"
    var rangeDays by remember { mutableIntStateOf(7) } // 7, 30, or 0 (All)

    val filteredSales = remember(sales, rangeDays) {
        if (rangeDays == 0) sales
        else {
            val cutoff = System.currentTimeMillis() - rangeDays * 24L * 3600_000L
            sales.filter { (it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= cutoff }
        }
    }

    val totalRevenue = remember(filteredSales) { filteredSales.sumOf { it.grandTotal } }
    val totalProfit = remember(filteredSales) { filteredSales.sumOf { it.estimatedProfit } }
    val totalGst = remember(filteredSales) { filteredSales.sumOf { it.gstTotal } }
    val totalUnits = remember(filteredSales) { filteredSales.sumOf { it.totalUnits } }
    val lowStockProducts = remember(products) { products.filter { it.stockQty <= it.minStockAlert } }

    // Daily bar chart buckets (last 7 days)
    val chartData = remember(sales) {
        val cal = Calendar.getInstance()
        val buckets = (6 downTo 0).map { daysAgo ->
            val c = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = c.timeInMillis
            val end = start + 24L * 3600_000L
            val label = FormatUtils.formatShortDate(Date(start))
            val daySales = sales.filter {
                val t = it.createdAt?.toDate()?.time ?: 0L
                t in start until end
            }
            Triple(label, daySales.sumOf { it.grandTotal }, daySales.sumOf { it.estimatedProfit })
        }
        buckets
    }

    // GST Slab Breakdown
    val gstSlabs = remember(filteredSales) {
        val map = sortedMapOf<Double, Pair<Double, Double>>() // gst% -> (taxable, tax)
        filteredSales.forEach { sale ->
            sale.items.forEach { item ->
                val prev = map[item.gstPercent] ?: (0.0 to 0.0)
                map[item.gstPercent] = (prev.first + item.taxableAmount) to (prev.second + item.gstAmount)
            }
        }
        map.toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Business Overview", style = MaterialTheme.typography.headlineMedium, color = Slate900)
                    Text(
                        text = "${store?.name ?: "Active Branch"} · Live GST & Profit Metrics",
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
                        .testTag("dashboard_export_excel_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                    Spacer(Modifier.width(6.dp))
                    Text("Export .xlsx", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Date Range Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "Last 7 Days", 30 to "Last 30 Days", 0 to "All Time").forEach { (days, label) ->
                    FilterChip(
                        selected = rangeDays == days,
                        onClick = { rangeDays = days },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate900,
                            selectedLabelColor = PureWhite
                        )
                    )
                }
            }
        }

        // 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Total Revenue",
                        value = FormatUtils.formatMoney(totalRevenue, currency),
                        subtitle = "${filteredSales.size} bills generated",
                        icon = Icons.Filled.AttachMoney,
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Est. Net Profit",
                        value = FormatUtils.formatMoney(totalProfit, currency),
                        subtitle = "After purchase cost",
                        icon = Icons.Filled.TrendingUp,
                        accent = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "GST Collected",
                        value = FormatUtils.formatMoney(totalGst, currency),
                        subtitle = "${FormatUtils.formatQty(totalUnits)} units sold",
                        icon = Icons.Filled.ReceiptLong,
                        accent = false,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Low Stock Alerts",
                        value = lowStockProducts.size.toString(),
                        subtitle = "${products.size} total SKUs",
                        icon = Icons.Filled.WarningAmber,
                        accent = false,
                        warning = lowStockProducts.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateProducts)
                    )
                }
            }
        }

        // Interactive 7-Day Revenue & Profit Bar Chart
        item {
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
                            Text("7-Day Revenue & Profit Trend", style = MaterialTheme.typography.titleMedium, color = Slate900)
                            Text("Dark bar = Gross Revenue · Emerald = Net Profit", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                        MonoCodeBadge(code = currency, accent = true)
                    }

                    val maxVal = chartData.maxOfOrNull { it.second }?.coerceAtLeast(100.0) ?: 100.0
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    ) {
                        val chartH = size.height - 20.dp.toPx()
                        val slotW = size.width / chartData.size.coerceAtLeast(1)
                        val barW = (slotW * 0.28f).coerceAtLeast(8.dp.toPx())

                        // Subtle horizontal guidelines
                        for (g in 0..3) {
                            val y = chartH * (g / 3f)
                            drawLine(
                                color = Slate200,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        chartData.forEachIndexed { idx, (_, rev, prof) ->
                            val centerX = slotW * idx + slotW / 2f
                            val revH = ((rev / maxVal) * (chartH - 12.dp.toPx())).toFloat().coerceAtLeast(4f)
                            val profH = ((prof / maxVal) * (chartH - 12.dp.toPx())).toFloat().coerceAtLeast(2f)

                            drawRoundRect(
                                color = Slate900,
                                topLeft = Offset(centerX - barW - 2f, chartH - revH),
                                size = Size(barW, revH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            drawRoundRect(
                                color = Emerald600,
                                topLeft = Offset(centerX + 2f, chartH - profH),
                                size = Size(barW, profH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        chartData.forEach { (label, _, _) ->
                            Text(label.take(6), style = TabularMonoSmall, color = Slate500)
                        }
                    }
                }
            }
        }

        // GST Slab Breakdown Card
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GST Tax Breakdown (CGST + SGST)", style = MaterialTheme.typography.titleMedium, color = Slate900)
                        Text(
                            text = "All Invoices →",
                            style = MaterialTheme.typography.labelLarge,
                            color = Emerald700,
                            modifier = Modifier.clickable(onClick = onNavigateReports)
                        )
                    }
                    if (gstSlabs.isEmpty()) {
                        Text(
                            text = "Generate POS bills to see automatic GST slab totals here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    } else {
                        gstSlabs.forEach { (rate, pair) ->
                            val (taxable, tax) = pair
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate50,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MonoCodeBadge(code = "${FormatUtils.formatQty(rate)}% GST", accent = true)
                                        Text(
                                            text = "Taxable: ${FormatUtils.formatMoney(taxable, currency)}",
                                            style = TabularMonoSmall,
                                            color = Slate600
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FormatUtils.formatMoney(tax, currency),
                                            style = TabularMonoMedium,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "CGST ${FormatUtils.formatMoney(tax / 2, currency)} · SGST ${FormatUtils.formatMoney(tax / 2, currency)}",
                                            style = TabularMonoSmall,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Low-Stock Alerts Section
        if (lowStockProducts.isNotEmpty()) {
            item {
                EditorialCard(
                    borderColor = Amber700.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Amber700)
                            Text("Low-Stock Alerts (${lowStockProducts.size})", style = MaterialTheme.typography.titleMedium, color = Slate900)
                        }
                        HorizontalDivider(color = Slate200)
                        lowStockProducts.take(8).forEach { prod ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MonoCodeBadge(code = "#${prod.shortCode}")
                                    Text(prod.name, style = MaterialTheme.typography.bodyMedium, color = Slate900)
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Amber50) {
                                    Text(
                                        text = "${FormatUtils.formatQty(prod.stockQty)} / min ${FormatUtils.formatQty(prod.minStockAlert)}",
                                        style = TabularMonoSmall,
                                        color = Amber700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
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
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Boolean,
    warning: Boolean = false,
    modifier: Modifier = Modifier
) {
    EditorialCard(
        borderColor = if (warning) Amber700.copy(alpha = 0.45f) else Slate200,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Slate600)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        warning -> Amber50
                        accent -> Emerald50
                        else -> Slate100
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = when {
                            warning -> Amber700
                            accent -> Emerald700
                            else -> Slate900
                        },
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = TabularMonoLarge,
                color = if (warning) Amber700 else Slate900,
                maxLines = 1
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Slate500)
        }
    }
}
