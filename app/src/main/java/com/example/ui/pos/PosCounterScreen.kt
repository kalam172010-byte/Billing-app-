package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ScannerRoom
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.components.MonoCodeBadge
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoLarge
import com.example.ui.theme.TabularMonoMedium
import com.example.ui.theme.TabularMonoSmall
import com.example.util.FormatUtils

@Composable
fun PosCounterScreen(
    store: Store?,
    products: List<Product>,
    cartItems: List<CartItem>,
    discountAmount: Double,
    paymentMethod: PaymentMethod,
    scannerRoom: ScannerRoom?,
    isBusy: Boolean,
    onQuickCodeSubmit: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onDiscountChanged: (Double) -> Unit,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onOpenCameraScanner: () -> Unit,
    onOpenWirelessRoom: () -> Unit,
    onDisconnectRemoteScanner: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onGenerateBill: () -> Unit
) {
    val currency = store?.currencySymbol ?: "₹"
    var searchQuery by remember { mutableStateOf("") }
    var quickCodeInput by remember { mutableStateOf("") }
    var discountInput by remember(discountAmount) {
        mutableStateOf(if (discountAmount == 0.0) "" else discountAmount.toString())
    }
    var mobileTab by remember { mutableStateOf(0) } // 0 = Catalog, 1 = Cart

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

    val subtotal = remember(cartItems) { cartItems.sumOf { it.taxableAmount } }
    val gstTotal = remember(cartItems) { cartItems.sumOf { it.gstAmount } }
    val grandTotal = remember(subtotal, gstTotal, discountAmount) {
        (subtotal + gstTotal - discountAmount).coerceAtLeast(0.0)
    }
    val totalQty = remember(cartItems) { cartItems.sumOf { it.quantity } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        val isWide = maxWidth >= 840.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Live Wireless Scanner Connected Banner
            if (scannerRoom?.scannerConnected == true) {
                Surface(
                    color = Emerald50,
                    border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiTethering,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Live Wireless Scanner: ${scannerRoom.connectedScannerName.ifBlank { "Handheld Phone" }} · Room #${scannerRoom.roomCode}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Emerald700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PureWhite,
                            border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onDisconnectRemoteScanner)
                        ) {
                            Text(
                                text = "Disconnect",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate900,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (isWide) {
                // Tablet / Desktop Split Workspace: Left 58% Quick Entry + Product Grid, Right 42% Live Cart
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PosFastInputBar(
                            quickCodeInput = quickCodeInput,
                            onQuickCodeChange = { quickCodeInput = it },
                            onSubmitQuickCode = {
                                if (quickCodeInput.isNotBlank()) {
                                    onQuickCodeSubmit(quickCodeInput)
                                    quickCodeInput = ""
                                }
                            },
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onOpenCameraScanner = onOpenCameraScanner,
                            onOpenWirelessRoom = onOpenWirelessRoom
                        )
                        PosCatalogPane(
                            products = filteredProducts,
                            allProductCount = products.size,
                            currency = currency,
                            onAddToCart = onAddToCart,
                            onNavigateToProducts = onNavigateToProducts,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PosCartPanel(
                        cartItems = cartItems,
                        currency = currency,
                        subtotal = subtotal,
                        gstTotal = gstTotal,
                        discountInput = discountInput,
                        grandTotal = grandTotal,
                        paymentMethod = paymentMethod,
                        isBusy = isBusy,
                        onUpdateQty = onUpdateQty,
                        onRemoveFromCart = onRemoveFromCart,
                        onClearCart = onClearCart,
                        onDiscountInputChanged = { text ->
                            discountInput = text
                            onDiscountChanged(text.toDoubleOrNull() ?: 0.0)
                        },
                        onPaymentMethodSelected = onPaymentMethodSelected,
                        onGenerateBill = onGenerateBill,
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // Compact Mobile Workspace: Fast Bar + Switcher (Catalog vs Active Cart) + Sticky Checkout Strip
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PosFastInputBar(
                        quickCodeInput = quickCodeInput,
                        onQuickCodeChange = { quickCodeInput = it },
                        onSubmitQuickCode = {
                            if (quickCodeInput.isNotBlank()) {
                                onQuickCodeSubmit(quickCodeInput)
                                quickCodeInput = ""
                            }
                        },
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onOpenCameraScanner = onOpenCameraScanner,
                        onOpenWirelessRoom = onOpenWirelessRoom
                    )

                    // Compact Segmented Switcher: Quick Catalog | Current Bill (N)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (mobileTab == 0) Slate900 else PureWhite,
                            border = BorderStroke(1.dp, if (mobileTab == 0) Slate900 else Slate200),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { mobileTab = 0 }
                                .testTag("pos_tab_catalog")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Products (${products.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (mobileTab == 0) PureWhite else Slate900
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (mobileTab == 1) Slate900 else PureWhite,
                            border = BorderStroke(1.dp, if (mobileTab == 1) Slate900 else Slate200),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { mobileTab = 1 }
                                .testTag("pos_tab_cart")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Active Bill ($totalQty)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (mobileTab == 1) PureWhite else Slate900
                                )
                                if (grandTotal > 0.0) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = FormatUtils.formatMoney(grandTotal, currency),
                                        style = TabularMonoSmall,
                                        color = if (mobileTab == 1) Emerald600 else Emerald700
                                    )
                                }
                            }
                        }
                    }

                    if (mobileTab == 0) {
                        PosCatalogPane(
                            products = filteredProducts,
                            allProductCount = products.size,
                            currency = currency,
                            onAddToCart = onAddToCart,
                            onNavigateToProducts = onNavigateToProducts,
                            modifier = Modifier.weight(1f)
                        )
                        // Quick Sticky Checkout Bar when items are in cart
                        if (cartItems.isNotEmpty()) {
                            EditorialCard(
                                containerColor = Slate900,
                                borderColor = Slate900,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .clickable { mobileTab = 1 }
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "$totalQty items · View Cart",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate300
                                        )
                                        Text(
                                            text = FormatUtils.formatMoney(grandTotal, currency),
                                            style = TabularMonoLarge,
                                            color = PureWhite
                                        )
                                    }
                                    Button(
                                        onClick = onGenerateBill,
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Emerald600,
                                            contentColor = PureWhite
                                        ),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("generate_bill_quick_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ReceiptLong,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.action_generate_bill),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        PosCartPanel(
                            cartItems = cartItems,
                            currency = currency,
                            subtotal = subtotal,
                            gstTotal = gstTotal,
                            discountInput = discountInput,
                            grandTotal = grandTotal,
                            paymentMethod = paymentMethod,
                            isBusy = isBusy,
                            onUpdateQty = onUpdateQty,
                            onRemoveFromCart = onRemoveFromCart,
                            onClearCart = onClearCart,
                            onDiscountInputChanged = { text ->
                                discountInput = text
                                onDiscountChanged(text.toDoubleOrNull() ?: 0.0)
                            },
                            onPaymentMethodSelected = onPaymentMethodSelected,
                            onGenerateBill = onGenerateBill,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosFastInputBar(
    quickCodeInput: String,
    onQuickCodeChange: (String) -> Unit,
    onSubmitQuickCode: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenCameraScanner: () -> Unit,
    onOpenWirelessRoom: () -> Unit
) {
    EditorialCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: 4-Digit Short Code Instant Entry + Camera QR Button + Wireless Hub Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickCodeInput,
                    onValueChange = onQuickCodeChange,
                    placeholder = {
                        Text(
                            text = "4-digit code (e.g. 1001)",
                            style = TabularMonoSmall,
                            color = Slate500
                        )
                    },
                    textStyle = TabularMonoMedium.copy(color = Slate900),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmitQuickCode() }),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Slate900,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_code_input")
                )

                Button(
                    onClick = onSubmitQuickCode,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("quick_code_add_button")
                ) {
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald600,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenCameraScanner)
                        .testTag("open_camera_qr_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = stringResource(R.string.cd_scan_qr),
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Row 2: Product Search + Wireless Scanner Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "Search products by name, SKU, or category...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Slate900,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_product_search")
                )
                OutlinedButton(
                    onClick = onOpenWirelessRoom,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate200),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiTethering,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Pair", style = MaterialTheme.typography.labelLarge, color = Slate900)
                }
            }
        }
    }
}

@Composable
private fun PosCatalogPane(
    products: List<Product>,
    allProductCount: Int,
    currency: String,
    onAddToCart: (Product) -> Unit,
    onNavigateToProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (allProductCount == 0) {
        EditorialCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Slate100
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Your Inventory Is Clean & Ready",
                    style = MaterialTheme.typography.titleLarge,
                    color = Slate900
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Zero dummy products loaded. Add your first product with an automatic 4-digit quick code (1001+) to begin counter billing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToProducts,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_pos_add_product_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Products in Inventory", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    } else if (products.isEmpty()) {
        EditorialCard(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching products found for your search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }
    } else {
        // Photo-free, high-speed editorial product tap cards
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            items(products, key = { it.id }) { product ->
                val isLowStock = product.stockQty <= product.minStockAlert
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, if (isLowStock) Amber700.copy(alpha = 0.4f) else Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAddToCart(product) }
                        .testTag("pos_product_card_${product.shortCode}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MonoCodeBadge(code = "#${product.shortCode}", accent = true)
                            Text(
                                text = "${FormatUtils.formatQty(product.stockQty)} ${product.unit}",
                                style = TabularMonoSmall,
                                color = if (isLowStock) Amber700 else Slate500
                            )
                        }
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate900,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(36.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = FormatUtils.formatMoney(product.sellingPrice, currency),
                                style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate900
                            )
                            Text(
                                text = "GST ${FormatUtils.formatQty(product.gstPercent)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCartPanel(
    cartItems: List<CartItem>,
    currency: String,
    subtotal: Double,
    gstTotal: Double,
    discountInput: String,
    grandTotal: Double,
    paymentMethod: PaymentMethod,
    isBusy: Boolean,
    onUpdateQty: (String, Int) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onDiscountInputChanged: (String) -> Unit,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onGenerateBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    EditorialCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cart Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PointOfSale,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Current Bill",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900
                    )
                    MonoCodeBadge(code = "${cartItems.sumOf { it.quantity }} units")
                }
                if (cartItems.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Rose600,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClearCart)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
            HorizontalDivider(color = Slate200)

            // Cart Items List
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Cart is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate600
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap any product, enter a 4-digit code, or scan a QR sticker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "#${item.product.shortCode} · ${FormatUtils.formatMoney(item.product.sellingPrice, currency)} + ${FormatUtils.formatQty(item.product.gstPercent)}% GST",
                                        style = TabularMonoSmall,
                                        color = Slate500
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PureWhite,
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUpdateQty(item.product.id, -1) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Remove,
                                                contentDescription = "Decrease quantity",
                                                tint = Slate900,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.quantity.toString(),
                                        style = TabularMonoMedium,
                                        color = Slate900,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUpdateQty(item.product.id, 1) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Increase quantity",
                                                tint = PureWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = FormatUtils.formatMoney(item.lineTotal, currency),
                                        style = TabularMonoMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Slate900
                                    )
                                    IconButton(
                                        onClick = { onRemoveFromCart(item.product.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove item",
                                            tint = Slate500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate200)

            // Payment Method + Discount + Totals + Customer-Free Fast Checkout Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Payment Mode Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { onPaymentMethodSelected(method) },
                            label = {
                                Text(
                                    text = method.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate900,
                                selectedLabelColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Bill Discount Input
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = onDiscountInputChanged,
                    label = { Text("Bill Discount ($currency)", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    textStyle = TabularMonoMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bill_discount_input")
                )

                // Summary Lines
                SummaryRow("Taxable Subtotal", FormatUtils.formatMoney(subtotal, currency))
                SummaryRow("GST Total", FormatUtils.formatMoney(gstTotal, currency))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grand Total",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900
                    )
                    Text(
                        text = FormatUtils.formatMoney(grandTotal, currency),
                        style = TabularMonoLarge,
                        color = Emerald700
                    )
                }

                Button(
                    onClick = onGenerateBill,
                    enabled = cartItems.isNotEmpty() && !isBusy,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_bill_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_generate_bill),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate600)
        Text(text = value, style = TabularMonoMedium, color = Slate900)
    }
}
