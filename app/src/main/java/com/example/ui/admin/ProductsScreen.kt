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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.StockLog
import com.example.data.model.StockLogReason
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Rose700
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils

@Composable
fun ProductsScreen(
    store: Store?,
    products: List<Product>,
    stockLogs: List<StockLog>,
    nextAutoShortCode: () -> String,
    onSaveProduct: (Product, Boolean, () -> Unit) -> Unit,
    onAdjustStock: (Product, Double, StockLogReason, String) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    val currency = store?.currencySymbol ?: "₹"
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Products, 1 = Stock Movement Logs
    var editorProduct by remember { mutableStateOf<Product?>(null) }
    var isEditingExisting by remember { mutableStateOf(false) }
    var stockAdjustProduct by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) products
        else products.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.shortCode.contains(q, ignoreCase = true) ||
                it.sku.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true)
        }
    }

    if (editorProduct != null) {
        ProductEditorDialog(
            initial = editorProduct!!,
            isEdit = isEditingExisting,
            currency = currency,
            onDismiss = { editorProduct = null },
            onSave = { updated ->
                onSaveProduct(updated, isEditingExisting) { editorProduct = null }
            }
        )
    }

    if (stockAdjustProduct != null) {
        StockAdjustDialog(
            product = stockAdjustProduct!!,
            onDismiss = { stockAdjustProduct = null },
            onConfirm = { delta, reason, note ->
                onAdjustStock(stockAdjustProduct!!, delta, reason, note)
                stockAdjustProduct = null
            }
        )
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
                Text("Products & Inventory", style = MaterialTheme.typography.headlineMedium, color = Slate900)
                Text(
                    text = "${products.size} SKUs in ${store?.name ?: "Branch"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
            Button(
                onClick = {
                    isEditingExisting = false
                    editorProduct = Product(
                        storeId = store?.id.orEmpty(),
                        shortCode = nextAutoShortCode(),
                        gstPercent = 0.0,
                        minStockAlert = 5.0,
                        unit = "pcs"
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_product_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                Spacer(Modifier.width(6.dp))
                Text("Add Product", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Tab switcher: Catalog | Stock Audit Logs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Inventory (${products.size})") },
                leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Slate900,
                    selectedLabelColor = PureWhite,
                    selectedLeadingIconColor = Emerald600
                )
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Stock Logs (${stockLogs.size})") },
                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Slate900,
                    selectedLabelColor = PureWhite,
                    selectedLeadingIconColor = Emerald600
                )
            )
        }

        if (activeTab == 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter by product name, 4-digit code, SKU, or category...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Slate500) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("products_search_input")
            )

            if (products.isEmpty()) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Slate500, modifier = Modifier.size(36.dp))
                        Text("Zero Products Yet", style = MaterialTheme.typography.titleMedium, color = Slate900)
                        Text(
                            text = "Kalam Billing Pro starts 100% clean with zero dummy products. Tap 'Add Product' to create your first item with auto-generated 4-digit code 1001.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isLow = product.stockQty <= product.minStockAlert
                        EditorialCard(
                            borderColor = if (isLow) Amber700.copy(alpha = 0.45f) else Slate200,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            MonoCodeBadge(code = "#${product.shortCode}", accent = true)
                                            if (product.category.isNotBlank()) {
                                                MonoCodeBadge(code = product.category)
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(product.name, style = MaterialTheme.typography.titleMedium, color = Slate900)
                                        if (product.sku.isNotBlank()) {
                                            Text("SKU: ${product.sku}", style = TabularMonoSmall, color = Slate500)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = FormatUtils.formatMoney(product.sellingPrice, currency),
                                            style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Cost ${FormatUtils.formatMoney(product.purchasePrice, currency)} · GST ${FormatUtils.formatQty(product.gstPercent)}%",
                                            style = TabularMonoSmall,
                                            color = Slate500
                                        )
                                    }
                                }

                                HorizontalDivider(color = Slate200)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isLow) Amber50 else Emerald50
                                    ) {
                                        Text(
                                            text = "Stock: ${FormatUtils.formatQty(product.stockQty)} ${product.unit} (Alert ≤ ${FormatUtils.formatQty(product.minStockAlert)})",
                                            style = TabularMonoSmall,
                                            color = if (isLow) Amber700 else Emerald700,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { stockAdjustProduct = product },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.Tune, contentDescription = "Adjust stock", tint = Emerald700)
                                        }
                                        IconButton(
                                            onClick = {
                                                isEditingExisting = true
                                                editorProduct = product
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit product", tint = Slate900)
                                        }
                                        IconButton(
                                            onClick = { onDeleteProduct(product) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete product", tint = Rose600)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Stock Movement Audit Logs
            if (stockLogs.isEmpty()) {
                EditorialCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No stock movement logs yet. Sales and restocks appear here automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(stockLogs, key = { it.id }) { log ->
                        val positive = log.deltaQty >= 0
                        EditorialCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MonoCodeBadge(code = "#${log.shortCode}")
                                        Text(log.productName, style = MaterialTheme.typography.titleSmall, color = Slate900)
                                    }
                                    Text(
                                        text = "${StockLogReason.fromRaw(log.reason).label} · ${log.referenceInvoice.ifBlank { "Manual" }} · ${FormatUtils.formatDateTime(log.createdAt?.toDate())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (positive) Emerald50 else Rose50
                                    ) {
                                        Text(
                                            text = "${if (positive) "+" else ""}${FormatUtils.formatQty(log.deltaQty)}",
                                            style = TabularMonoMedium,
                                            color = if (positive) Emerald700 else Rose700,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "${FormatUtils.formatQty(log.previousStock)} → ${FormatUtils.formatQty(log.newStock)}",
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
}

@Composable
private fun ProductEditorDialog(
    initial: Product,
    isEdit: Boolean,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var shortCode by remember { mutableStateOf(initial.shortCode) }
    var sku by remember { mutableStateOf(initial.sku) }
    var category by remember { mutableStateOf(initial.category) }
    var purchasePrice by remember { mutableStateOf(if (initial.purchasePrice == 0.0 && !isEdit) "" else initial.purchasePrice.toString()) }
    var sellingPrice by remember { mutableStateOf(if (initial.sellingPrice == 0.0 && !isEdit) "" else initial.sellingPrice.toString()) }
    var gstPercent by remember { mutableStateOf(initial.gstPercent.toString()) }
    var stockQty by remember { mutableStateOf(if (initial.stockQty == 0.0 && !isEdit) "10" else initial.stockQty.toString()) }
    var minStockAlert by remember { mutableStateOf(initial.minStockAlert.toString()) }
    var unit by remember { mutableStateOf(initial.unit.ifBlank { "pcs" }) }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Edit Product" else "Add New Product",
                        style = MaterialTheme.typography.titleLarge,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Slate600)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shortCode,
                        onValueChange = { shortCode = it.take(12) },
                        label = { Text("4-Digit Code *") },
                        textStyle = TabularMonoMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(24) },
                        label = { Text("Unit (pcs, kg)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it.take(60) },
                        label = { Text("SKU / Barcode") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it.take(60) },
                        label = { Text("Category") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Purchase ($currency)") },
                        textStyle = TabularMonoMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling ($currency) *") },
                        textStyle = TabularMonoMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_selling_price_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gstPercent,
                        onValueChange = { gstPercent = it },
                        label = { Text("GST %") },
                        textStyle = TabularMonoMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = { Text("Stock Qty") },
                        textStyle = TabularMonoMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockAlert,
                        onValueChange = { minStockAlert = it },
                        label = { Text("Min Alert") },
                        textStyle = TabularMonoMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick GST slab selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("0", "5", "12", "18", "28").forEach { rate ->
                        FilterChip(
                            selected = gstPercent.toDoubleOrNull() == rate.toDouble(),
                            onClick = { gstPercent = rate },
                            label = { Text("$rate%", style = TabularMonoSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", color = Slate900)
                    }
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty() && shortCode.trim().isNotEmpty()) {
                                onSave(
                                    initial.copy(
                                        name = name.trim(),
                                        shortCode = shortCode.trim(),
                                        sku = sku.trim(),
                                        category = category.trim(),
                                        purchasePrice = (purchasePrice.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                        sellingPrice = (sellingPrice.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                                        gstPercent = (gstPercent.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                        stockQty = stockQty.toDoubleOrNull() ?: 0.0,
                                        minStockAlert = (minStockAlert.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.0),
                                        unit = unit.trim().ifBlank { "pcs" }
                                    )
                                )
                            }
                        },
                        enabled = name.trim().isNotEmpty() && shortCode.trim().isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_product_button")
                    ) {
                        Text(if (isEdit) "Update Product" else "Create Product")
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAdjustDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double, StockLogReason, String) -> Unit
) {
    var qtyText by remember { mutableStateOf("10") }
    var selectedReason by remember { mutableStateOf(StockLogReason.RESTOCK) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Adjust Stock · ${product.name}", style = MaterialTheme.typography.titleMedium, color = Slate900)
                Text(
                    text = "Current stock: ${FormatUtils.formatQty(product.stockQty)} ${product.unit}",
                    style = TabularMonoSmall,
                    color = Slate500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(StockLogReason.RESTOCK, StockLogReason.ADJUSTMENT, StockLogReason.DAMAGE).forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = {
                        Text(
                            if (selectedReason == StockLogReason.DAMAGE) "Quantity to Deduct"
                            else "Quantity Change (+ or -)"
                        )
                    },
                    textStyle = TabularMonoMedium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(60) },
                    label = { Text("Reference / Audit Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Cancel", color = Slate900)
                    }
                    Button(
                        onClick = {
                            val raw = qtyText.toDoubleOrNull() ?: 0.0
                            val delta = if (selectedReason == StockLogReason.DAMAGE && raw > 0) -raw else raw
                            if (delta != 0.0) {
                                onConfirm(delta, selectedReason, note.ifBlank { selectedReason.label })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
