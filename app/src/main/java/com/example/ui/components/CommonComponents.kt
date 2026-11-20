package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ScannerRoom
import com.example.data.model.Store
import com.example.data.model.SyncMetrics
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
import com.example.ui.theme.TabularMonoSmall
import com.example.viewmodel.AppSection

@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PureWhite,
    borderColor: Color = Slate200,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun MonoCodeBadge(
    code: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = if (accent) Emerald50 else Slate100,
        border = BorderStroke(1.dp, if (accent) Emerald600.copy(alpha = 0.3f) else Slate200)
    ) {
        Text(
            text = code,
            style = TabularMonoSmall.copy(fontWeight = FontWeight.Bold),
            color = if (accent) Emerald700 else Slate900,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * Single-row sticky top header (56dp mobile / 64dp tablet) with 3 clean zones:
 * Zone 1: Menu drawer trigger + contextual Back button + Brand wordmark
 * Zone 2: Wide-screen desktop navigation links (POS Counter, Dashboard, Products, QR Sheet, Reports)
 * Zone 3: Quick actions (Offline Ready / Offline Mode, Wireless Room #XXXX, Admin Panel / POS toggle)
 */
@Composable
fun KalamTopAppBar(
    currentSection: AppSection,
    activeStore: Store?,
    roomCode: String,
    scannerRoom: ScannerRoom?,
    syncMetrics: SyncMetrics,
    isExpandedWidth: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateSection: (AppSection) -> Unit,
    onOpenOfflineModal: () -> Unit,
    onOpenWirelessRoomModal: () -> Unit,
    onToggleAdminPosMode: () -> Unit
) {
    val isOffline = syncMetrics.forceOffline || !syncMetrics.networkAvailable
    Surface(
        color = PureWhite,
        border = BorderStroke(0.dp, Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpandedWidth) 64.dp else 56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Zone 1: Menu + Back + Brand Wordmark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("menu_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_open_menu),
                            tint = Slate900
                        )
                    }
                    if (currentSection != AppSection.POS_COUNTER) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back),
                                tint = Slate900
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = 2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "KALAM BILLING",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Emerald600
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (activeStore != null) {
                            Text(
                                text = activeStore.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Zone 2: Wide-Screen Navigation Links
                if (isExpandedWidth) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val links = listOf(
                            AppSection.POS_COUNTER to stringResource(R.string.nav_pos_counter),
                            AppSection.DASHBOARD to stringResource(R.string.nav_dashboard),
                            AppSection.PRODUCTS to stringResource(R.string.nav_products),
                            AppSection.QR_STUDIO to stringResource(R.string.nav_qr_sheet),
                            AppSection.REPORTS to stringResource(R.string.nav_reports)
                        )
                        links.forEach { (section, label) ->
                            val selected = currentSection == section
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Slate900 else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateSection(section) }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) PureWhite else Slate600,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Zone 3: Quick Actions (Offline, Room #XXXX, Admin/POS Mode Toggle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Offline Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOffline) Amber50 else Emerald50,
                        border = BorderStroke(1.dp, if (isOffline) Amber700.copy(alpha = 0.35f) else Emerald600.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenOfflineModal)
                            .testTag("offline_status_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.CloudQueue,
                                contentDescription = stringResource(R.string.cd_offline_status),
                                tint = if (isOffline) Amber700 else Emerald700,
                                modifier = Modifier.size(16.dp)
                            )
                            if (isExpandedWidth) {
                                Text(
                                    text = if (isOffline) "Offline Mode" else "Offline Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isOffline) Amber700 else Emerald700
                                )
                            }
                        }
                    }

                    // Wireless Scanner Room Pill (#XXXX)
                    val scannerConnected = scannerRoom?.scannerConnected == true
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (scannerConnected) Emerald50 else Slate100,
                        border = BorderStroke(
                            1.dp,
                            if (scannerConnected) Emerald600 else Slate200
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenWirelessRoomModal)
                            .testTag("wireless_room_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiTethering,
                                contentDescription = stringResource(R.string.cd_wireless_scanner),
                                tint = if (scannerConnected) Emerald700 else Slate600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "#$roomCode",
                                style = TabularMonoSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (scannerConnected) Emerald700 else Slate900
                            )
                        }
                    }

                    // Admin Panel / POS Counter Mode Toggle
                    val inPos = currentSection == AppSection.POS_COUNTER
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleAdminPosMode)
                            .testTag("mode_toggle_button")
                    ) {
                        Text(
                            text = if (inPos) "Admin" else "POS",
                            style = MaterialTheme.typography.labelLarge,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = Slate200, thickness = 1.dp)
        }
    }
}

@Composable
fun OfflineFloatingBanner(
    syncMetrics: SyncMetrics,
    onOpenOfflineModal: () -> Unit
) {
    val isOffline = syncMetrics.forceOffline || !syncMetrics.networkAvailable
    if (!isOffline) return
    Surface(
        color = Amber50,
        border = BorderStroke(1.dp, Amber700.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenOfflineModal)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = Amber700,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (syncMetrics.forceOffline) "Offline Mode Active (Manual) · Billing saved locally"
                    else "No Internet · Offline billing active",
                    style = MaterialTheme.typography.labelMedium,
                    color = Amber700
                )
            }
            Text(
                text = "Manage",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate900
            )
        }
    }
}

@Composable
fun KalamNavigationDrawerSheet(
    stores: List<Store>,
    activeStore: Store?,
    currentSection: AppSection,
    userEmail: String,
    roomCode: String,
    syncMetrics: SyncMetrics,
    onSelectStore: (String) -> Unit,
    onNavigate: (AppSection) -> Unit,
    onOpenWirelessRoom: () -> Unit,
    onOpenOfflineModal: () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = PureWhite,
        modifier = Modifier.width(304.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Brand Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = Slate900
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "K",
                            style = MaterialTheme.typography.titleMedium,
                            color = Emerald600
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.brand_wordmark),
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900
                    )
                    Text(
                        text = userEmail.ifBlank { "Cloud Owner" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))

            // Active Store Switcher
            Text(
                text = "ACTIVE BUSINESS BRANCH",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            stores.forEach { store ->
                val isSelected = store.id == activeStore?.id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Emerald50 else Slate50,
                    border = BorderStroke(1.dp, if (isSelected) Emerald600 else Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectStore(store.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = if (isSelected) Emerald700 else Slate600,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${store.category} · ${store.thermalWidthMm}mm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Active store",
                                tint = Emerald600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))

            // Billing & Scanning Section
            Text(
                text = "BILLING & SCANNING",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_pos_counter),
                icon = Icons.Filled.PointOfSale,
                selected = currentSection == AppSection.POS_COUNTER,
                onClick = { onNavigate(AppSection.POS_COUNTER) }
            )
            DrawerNavRow(
                label = "${stringResource(R.string.nav_wireless_room)} (#$roomCode)",
                icon = Icons.Filled.WifiTethering,
                selected = false,
                onClick = onOpenWirelessRoom
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_handheld_scanner),
                icon = Icons.Filled.QrCodeScanner,
                selected = currentSection == AppSection.HANDHELD_SCANNER,
                onClick = { onNavigate(AppSection.HANDHELD_SCANNER) }
            )

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))

            // Admin Management Section
            Text(
                text = "ADMIN MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_dashboard),
                icon = Icons.Filled.Dashboard,
                selected = currentSection == AppSection.DASHBOARD,
                onClick = { onNavigate(AppSection.DASHBOARD) }
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_products),
                icon = Icons.Filled.Inventory2,
                selected = currentSection == AppSection.PRODUCTS,
                onClick = { onNavigate(AppSection.PRODUCTS) }
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_qr_sheet),
                icon = Icons.Filled.QrCode2,
                selected = currentSection == AppSection.QR_STUDIO,
                onClick = { onNavigate(AppSection.QR_STUDIO) }
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_reports),
                icon = Icons.Filled.Assessment,
                selected = currentSection == AppSection.REPORTS,
                onClick = { onNavigate(AppSection.REPORTS) }
            )

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 8.dp))

            // Configuration & Sync Section
            Text(
                text = "SETTINGS & CLOUD SYNC",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            DrawerNavRow(
                label = stringResource(R.string.nav_store_settings),
                icon = Icons.Filled.Settings,
                selected = currentSection == AppSection.STORE_SETTINGS,
                onClick = { onNavigate(AppSection.STORE_SETTINGS) }
            )
            val isOffline = syncMetrics.forceOffline || !syncMetrics.networkAvailable
            DrawerNavRow(
                label = if (isOffline) "Offline Mode (Local Cache)" else stringResource(R.string.nav_offline_sync),
                icon = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.CloudQueue,
                selected = false,
                onClick = onOpenOfflineModal
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Slate200)
            Spacer(Modifier.height(4.dp))

            DrawerNavRow(
                label = stringResource(R.string.action_sign_out),
                icon = Icons.AutoMirrored.Filled.Logout,
                selected = false,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun DrawerNavRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Slate900,
            selectedTextColor = PureWhite,
            selectedIconColor = Emerald600,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = Slate900,
            unselectedIconColor = Slate600
        ),
        modifier = Modifier.height(48.dp)
    )
}
