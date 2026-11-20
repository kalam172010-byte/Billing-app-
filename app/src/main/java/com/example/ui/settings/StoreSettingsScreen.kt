package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Store
import com.example.ui.components.EditorialCard
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TabularMonoSmall

private val BusinessCategories = listOf(
    "Retail", "Menswear", "Supermarket", "Grocery", "Mobile Shop", "Electronics", "Pharmacy", "Cafe"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreSettingsScreen(
    stores: List<Store>,
    activeStore: Store?,
    onSelectStore: (String) -> Unit,
    onSaveStore: (Store, Boolean) -> Unit,
    onDeleteStore: (Store) -> Unit
) {
    var creatingNewBranch by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(activeStore?.name ?: "") }
    var category by remember { mutableStateOf(activeStore?.category ?: "Retail") }
    var address by remember { mutableStateOf(activeStore?.address ?: "") }
    var phone by remember { mutableStateOf(activeStore?.phone ?: "") }
    var gstin by remember { mutableStateOf(activeStore?.gstin ?: "") }
    var upiId by remember { mutableStateOf(activeStore?.upiId ?: "") }
    var currencySymbol by remember { mutableStateOf(activeStore?.currencySymbol ?: "₹") }
    var invoicePrefix by remember { mutableStateOf(activeStore?.invoicePrefix ?: "KBP") }
    var thermalWidthMm by remember { mutableIntStateOf(activeStore?.thermalWidthMm ?: 80) }
    var receiptHeader by remember { mutableStateOf(activeStore?.receiptHeader ?: "") }
    var receiptFooter by remember { mutableStateOf(activeStore?.receiptFooter ?: "Thank you for shopping with us!") }
    var allowNegativeStock by remember { mutableStateOf(activeStore?.allowNegativeStock ?: false) }

    LaunchedEffect(activeStore?.id, creatingNewBranch) {
        if (creatingNewBranch) {
            name = ""
            category = "Retail"
            address = ""
            phone = ""
            gstin = ""
            upiId = ""
            currencySymbol = "₹"
            invoicePrefix = "KBP"
            thermalWidthMm = 80
            receiptHeader = "TAX INVOICE"
            receiptFooter = "Thank you for visiting!"
            allowNegativeStock = false
        } else if (activeStore != null) {
            name = activeStore.name
            category = activeStore.category
            address = activeStore.address
            phone = activeStore.phone
            gstin = activeStore.gstin
            upiId = activeStore.upiId
            currencySymbol = activeStore.currencySymbol
            invoicePrefix = activeStore.invoicePrefix
            thermalWidthMm = activeStore.thermalWidthMm
            receiptHeader = activeStore.receiptHeader
            receiptFooter = activeStore.receiptFooter
            allowNegativeStock = activeStore.allowNegativeStock
        }
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
                    Text("Multi-Business & Receipt Settings", style = MaterialTheme.typography.headlineMedium, color = Slate900)
                    Text("Switch branches or configure GSTIN, UPI, and thermal roll layout", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                OutlinedButton(
                    onClick = { creatingNewBranch = !creatingNewBranch },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Emerald600),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("new_business_branch_button")
                ) {
                    Icon(Icons.Filled.AddBusiness, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (creatingNewBranch) "Edit Active" else "New Branch", color = Emerald700)
                }
            }
        }

        // Business Branches Switcher
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("YOUR BUSINESS BRANCHES (${stores.size})", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    stores.forEach { store ->
                        val selected = !creatingNewBranch && store.id == activeStore?.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Emerald50 else Slate50,
                            border = BorderStroke(1.dp, if (selected) Emerald600 else Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    creatingNewBranch = false
                                    onSelectStore(store.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = if (selected) Emerald700 else Slate600)
                                    Column {
                                        Text(store.name, style = MaterialTheme.typography.titleSmall, color = Slate900)
                                        Text(
                                            text = "${store.category} · ${store.invoicePrefix} · ${store.thermalWidthMm}mm",
                                            style = TabularMonoSmall,
                                            color = Slate500
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selected) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = Emerald600, modifier = Modifier.size(20.dp))
                                    }
                                    if (stores.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteStore(store) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete branch", tint = Rose600)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Branch & Receipt Configuration Form
        item {
            EditorialCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (creatingNewBranch) "Create New Business Branch" else "Edit Active Branch & Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(100) },
                        label = { Text("Store / Business Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_name_input")
                    )

                    Text("Business Category", style = MaterialTheme.typography.labelMedium, color = Slate600)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BusinessCategories.forEach { cat ->
                            FilterChip(
                                selected = category.equals(cat, ignoreCase = true),
                                onClick = { category = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate900,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it.take(250) },
                        label = { Text("Store Address") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.take(30) },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it.uppercase().take(30) },
                            label = { Text("GSTIN") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.take(100) },
                        label = { Text("Merchant UPI ID (e.g. store@upi)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it.take(8) },
                            label = { Text("Currency Symbol") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it.uppercase().take(16) },
                            label = { Text("Invoice Prefix") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Slate200)

                    Text("Thermal Receipt Paper Width", style = MaterialTheme.typography.labelMedium, color = Slate600)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(58, 80).forEach { widthMm ->
                            FilterChip(
                                selected = thermalWidthMm == widthMm,
                                onClick = { thermalWidthMm = widthMm },
                                label = { Text("${widthMm}mm Roll", style = TabularMonoSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Slate900,
                                    selectedLabelColor = PureWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = receiptHeader,
                        onValueChange = { receiptHeader = it.take(200) },
                        label = { Text("Custom Receipt Header") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it.take(200) },
                        label = { Text("Custom Receipt Footer") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow Negative Stock Billing", style = MaterialTheme.typography.titleSmall, color = Slate900)
                            Text("Permit POS checkout even when inventory reaches zero", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                        Switch(
                            checked = allowNegativeStock,
                            onCheckedChange = { allowNegativeStock = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Emerald600)
                        )
                    }

                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                val target = (if (creatingNewBranch) Store() else (activeStore ?: Store())).copy(
                                    name = name.trim(),
                                    category = category.trim().ifBlank { "Retail" },
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    gstin = gstin.trim().uppercase(),
                                    upiId = upiId.trim(),
                                    currencySymbol = currencySymbol.trim().ifBlank { "₹" },
                                    invoicePrefix = invoicePrefix.trim().uppercase().ifBlank { "KBP" },
                                    thermalWidthMm = thermalWidthMm,
                                    receiptHeader = receiptHeader.trim(),
                                    receiptFooter = receiptFooter.trim(),
                                    allowNegativeStock = allowNegativeStock
                                )
                                onSaveStore(target, creatingNewBranch)
                                creatingNewBranch = false
                            }
                        },
                        enabled = name.trim().isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = PureWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_store_settings_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp), tint = Emerald600)
                        Spacer(Modifier.width(8.dp))
                        Text(if (creatingNewBranch) "Create Business Branch" else "Save Branch Settings")
                    }
                }
            }
        }
    }
}
