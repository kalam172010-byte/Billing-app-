package com.example

import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.KalamRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOutUser
import com.example.data.model.Sale
import com.example.nfc.NfcHceBridge
import com.example.ui.admin.DashboardScreen
import com.example.ui.admin.ProductsScreen
import com.example.ui.admin.QrStudioScreen
import com.example.ui.admin.ReportsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.components.KalamNavigationDrawerSheet
import com.example.ui.components.KalamTopAppBar
import com.example.ui.components.OfflineFloatingBanner
import com.example.ui.nfc.NfcBillShareDialog
import com.example.ui.pos.BillGeneratedDialog
import com.example.ui.pos.CustomerDigitalBillDialog
import com.example.ui.pos.PosCounterScreen
import com.example.ui.scanner.CameraQrScannerDialog
import com.example.ui.scanner.HandheldScannerScreen
import com.example.ui.scanner.WirelessTerminalRoomDialog
import com.example.ui.settings.OfflineSyncDialog
import com.example.ui.settings.StoreSettingsScreen
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.KalamBillingTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate900
import com.example.util.NetworkMonitor
import com.example.util.ReceiptPrintAndExport
import com.example.viewmodel.AppSection
import com.example.viewmodel.PosViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KalamBillingTheme {
                KalamAppNavigation(activity = this)
            }
        }
    }
}

@Composable
private fun KalamAppNavigation(activity: ComponentActivity) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener transitions automatically */ })
    } else {
        key(user.uid) {
            AuthenticatedPosShell(
                activity = activity,
                currentUserId = user.uid,
                userEmail = user.email.orEmpty()
            )
        }
    }
}

@Composable
private fun AuthenticatedPosShell(
    activity: ComponentActivity,
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: PosViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                PosViewModel(
                    repository = KalamRepository(app),
                    networkMonitor = NetworkMonitor(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val stores by viewModel.stores.collectAsStateWithLifecycle()
    val activeStore by viewModel.activeStore.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val sales by viewModel.activeSales.collectAsStateWithLifecycle()
    val stockLogs by viewModel.activeStockLogs.collectAsStateWithLifecycle()
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val discountAmount by viewModel.discountAmount.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val generatedSale by viewModel.generatedSaleModal.collectAsStateWithLifecycle()
    val digitalBillPreview by viewModel.digitalBillPreview.collectAsStateWithLifecycle()
    val nfcShareSale by viewModel.nfcShareSale.collectAsStateWithLifecycle()
    val roomCode by viewModel.terminalRoomCode.collectAsStateWithLifecycle()
    val scannerRoom by viewModel.hostScannerRoom.collectAsStateWithLifecycle()
    val pairedHandheldRoom by viewModel.pairedHandheldRoom.collectAsStateWithLifecycle()
    val syncMetrics by viewModel.syncMetrics.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    var showCameraQrScanner by remember { mutableStateOf(false) }
    var showWirelessRoomDialog by remember { mutableStateOf(false) }
    var showOfflineDialog by remember { mutableStateOf(false) }

    // Pending binary payloads for SAF CreateDocument exports (zero storage permissions!)
    var pendingSaveBytes by remember { mutableStateOf<ByteArray?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val bytes = pendingSaveBytes
        pendingSaveBytes = null
        if (uri != null && bytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                viewModel.showMessage("File exported successfully")
            } catch (e: Exception) {
                viewModel.showMessage("Export failed: ${e.localizedMessage}")
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonText = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                if (!jsonText.isNullOrBlank()) {
                    viewModel.restoreFromBackupJson(jsonText)
                }
            } catch (e: Exception) {
                viewModel.showMessage("Failed to read backup file: ${e.localizedMessage}")
            }
        }
    }

    fun exportBytes(fileName: String, bytes: ByteArray) {
        pendingSaveBytes = bytes
        createDocumentLauncher.launch(fileName)
    }

    fun printOrFallbackPdf(sale: Sale) {
        val pdfBytes = ReceiptPrintAndExport.generateInvoicePdfBytes(sale, thermalMode = true)
        val launched = ReceiptPrintAndExport.printPdfBytes(context, "Bill-${sale.invoiceNumber}", pdfBytes)
        if (!launched) {
            viewModel.showMessage("Printer service unavailable — saving PDF bill instead")
            exportBytes("Bill-${sale.invoiceNumber}.pdf", pdfBytes)
        }
    }

    // Enable Android NFC ReaderMode (for writing NDEF tags) whenever NFC Bill Share Modal is open
    DisposableEffect(nfcShareSale) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcShareSale != null && adapter != null && adapter.isEnabled) {
            try {
                adapter.enableReaderMode(
                    activity,
                    { tag -> NfcHceBridge.writeArmedUrlToTag(tag) },
                    NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V,
                    null
                )
            } catch (_: Exception) {
            }
        }
        onDispose {
            try {
                adapter?.disableReaderMode(activity)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // BackHandler for secondary screens returns smoothly to POS Counter
    BackHandler(enabled = currentSection != AppSection.POS_COUNTER) {
        viewModel.navigateTo(AppSection.POS_COUNTER)
    }

    // --- MODALS ---
    if (showCameraQrScanner) {
        CameraQrScannerDialog(
            onCodeScanned = { decoded ->
                val matched = viewModel.handleScannedOrEnteredCode(decoded)
                if (matched) showCameraQrScanner = false
            },
            onDismiss = { showCameraQrScanner = false }
        )
    }

    if (showWirelessRoomDialog) {
        WirelessTerminalRoomDialog(
            roomCode = roomCode,
            store = activeStore,
            scannerRoom = scannerRoom,
            onDisconnectRemote = { viewModel.disconnectRemoteScanner() },
            onSwitchToHandheldMode = { viewModel.navigateTo(AppSection.HANDHELD_SCANNER) },
            onDismiss = { showWirelessRoomDialog = false }
        )
    }

    if (showOfflineDialog) {
        OfflineSyncDialog(
            metrics = syncMetrics,
            isBusy = isBusy,
            onToggleForceOffline = { viewModel.toggleForceOffline(it) },
            onSyncNow = { viewModel.syncNow() },
            onBackupJson = {
                val json = viewModel.buildBackupJson()
                exportBytes("kalam-billing-backup.json", json.toByteArray(Charsets.UTF_8))
            },
            onRestoreJson = {
                openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = { showOfflineDialog = false }
        )
    }

    generatedSale?.let { sale ->
        BillGeneratedDialog(
            sale = sale,
            onDismiss = { viewModel.dismissGeneratedSaleModal() },
            onPrintReceipt = { printOrFallbackPdf(it) },
            onDownloadPdf = {
                val pdf = ReceiptPrintAndExport.generateInvoicePdfBytes(it, thermalMode = true)
                exportBytes("Bill-${it.invoiceNumber}.pdf", pdf)
            },
            onDownloadImage = {
                val png = ReceiptPrintAndExport.generateReceiptPngBytes(it)
                exportBytes("Receipt-${it.invoiceNumber}.png", png)
            },
            onOpenDigitalBillPreview = { viewModel.openDigitalBillPreview(it) },
            onOpenNfcShare = { viewModel.openNfcShareModal(it) }
        )
    }

    digitalBillPreview?.let { sale ->
        CustomerDigitalBillDialog(
            sale = sale,
            onDismiss = { viewModel.dismissDigitalBillPreview() },
            onDownloadPdf = {
                val pdf = ReceiptPrintAndExport.generateInvoicePdfBytes(it, thermalMode = false)
                exportBytes("Invoice-${it.invoiceNumber}.pdf", pdf)
            }
        )
    }

    nfcShareSale?.let { sale ->
        NfcBillShareDialog(
            sale = sale,
            onExportBridgeCode = { kotlinSource ->
                exportBytes("KalamNfcHceBridge.kt", kotlinSource.toByteArray(Charsets.UTF_8))
            },
            onDismiss = { viewModel.dismissNfcShareModal() }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KalamNavigationDrawerSheet(
                stores = stores,
                activeStore = activeStore,
                currentSection = currentSection,
                userEmail = userEmail,
                roomCode = roomCode,
                syncMetrics = syncMetrics,
                onSelectStore = { storeId ->
                    viewModel.selectStore(storeId)
                    scope.launch { drawerState.close() }
                },
                onNavigate = { section ->
                    viewModel.navigateTo(section)
                    scope.launch { drawerState.close() }
                },
                onOpenWirelessRoom = {
                    scope.launch { drawerState.close() }
                    showWirelessRoomDialog = true
                },
                onOpenOfflineModal = {
                    scope.launch { drawerState.close() }
                    showOfflineDialog = true
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    signOutUser(credentialManager, scope)
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedWidth = maxWidth >= 840.dp

            Scaffold(
                containerColor = Slate50,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.navigationBarsPadding()
                    )
                },
                topBar = {
                    Column {
                        KalamTopAppBar(
                            currentSection = currentSection,
                            activeStore = activeStore,
                            roomCode = roomCode,
                            scannerRoom = scannerRoom,
                            syncMetrics = syncMetrics,
                            isExpandedWidth = isExpandedWidth,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateBack = { viewModel.navigateTo(AppSection.POS_COUNTER) },
                            onNavigateSection = { viewModel.navigateTo(it) },
                            onOpenOfflineModal = { showOfflineDialog = true },
                            onOpenWirelessRoomModal = { showWirelessRoomDialog = true },
                            onToggleAdminPosMode = { viewModel.toggleAdminPosMode() }
                        )
                        OfflineFloatingBanner(
                            syncMetrics = syncMetrics,
                            onOpenOfflineModal = { showOfflineDialog = true }
                        )
                    }
                },
                bottomBar = {
                    if (!isExpandedWidth) {
                        NavigationBar(
                            containerColor = PureWhite,
                            tonalElevation = 0.dp
                        ) {
                            val navItems = listOf(
                                Triple(AppSection.POS_COUNTER, "POS", Icons.Filled.PointOfSale),
                                Triple(AppSection.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
                                Triple(AppSection.PRODUCTS, "Products", Icons.Filled.Inventory2),
                                Triple(AppSection.QR_STUDIO, "QR Sheet", Icons.Filled.QrCode2),
                                Triple(AppSection.REPORTS, "Reports", Icons.Filled.Assessment)
                            )
                            navItems.forEach { (section, label, icon) ->
                                NavigationBarItem(
                                    selected = currentSection == section,
                                    onClick = { viewModel.navigateTo(section) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Emerald700,
                                        selectedTextColor = Slate900,
                                        indicatorColor = Emerald50,
                                        unselectedIconColor = Slate500,
                                        unselectedTextColor = Slate500
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentSection) {
                        AppSection.POS_COUNTER -> PosCounterScreen(
                            store = activeStore,
                            products = products,
                            cartItems = cartItems,
                            discountAmount = discountAmount,
                            paymentMethod = paymentMethod,
                            scannerRoom = scannerRoom,
                            isBusy = isBusy,
                            onQuickCodeSubmit = { viewModel.handleScannedOrEnteredCode(it) },
                            onAddToCart = { viewModel.addToCart(it) },
                            onUpdateQty = { id, delta -> viewModel.updateCartQuantity(id, delta) },
                            onRemoveFromCart = { viewModel.removeFromCart(it) },
                            onClearCart = { viewModel.clearCart() },
                            onDiscountChanged = { viewModel.setDiscountAmount(it) },
                            onPaymentMethodSelected = { viewModel.setPaymentMethod(it) },
                            onOpenCameraScanner = { showCameraQrScanner = true },
                            onOpenWirelessRoom = { showWirelessRoomDialog = true },
                            onDisconnectRemoteScanner = { viewModel.disconnectRemoteScanner() },
                            onNavigateToProducts = { viewModel.navigateTo(AppSection.PRODUCTS) },
                            onGenerateBill = { viewModel.generateBill() }
                        )

                        AppSection.DASHBOARD -> DashboardScreen(
                            store = activeStore,
                            products = products,
                            sales = sales,
                            onExportExcel = {
                                val storeObj = activeStore ?: return@DashboardScreen
                                val xlsx = ReceiptPrintAndExport.generateExcelXlsxBytes(storeObj, sales, products)
                                exportBytes("${storeObj.invoicePrefix}-Sales-GST-Report.xlsx", xlsx)
                            },
                            onNavigateProducts = { viewModel.navigateTo(AppSection.PRODUCTS) },
                            onNavigateReports = { viewModel.navigateTo(AppSection.REPORTS) }
                        )

                        AppSection.PRODUCTS -> ProductsScreen(
                            store = activeStore,
                            products = products,
                            stockLogs = stockLogs,
                            nextAutoShortCode = { viewModel.nextAutoShortCode() },
                            onSaveProduct = { prod, isEdit, done -> viewModel.saveProduct(prod, isEdit, done) },
                            onAdjustStock = { prod, delta, reason, note -> viewModel.adjustStock(prod, delta, reason, note) },
                            onDeleteProduct = { viewModel.deleteProduct(it) }
                        )

                        AppSection.QR_STUDIO -> QrStudioScreen(
                            store = activeStore,
                            products = products,
                            onPrintQrSheet = { selections, size ->
                                val storeObj = activeStore ?: return@QrStudioScreen
                                val pdf = ReceiptPrintAndExport.generateQrStickerSheetPdfBytes(storeObj, selections, size)
                                val ok = ReceiptPrintAndExport.printPdfBytes(context, "QR-Sheet-${storeObj.invoicePrefix}", pdf)
                                if (!ok) {
                                    viewModel.showMessage("Print unavailable — saving A4 QR PDF")
                                    exportBytes("QR-Sheet-${storeObj.invoicePrefix}.pdf", pdf)
                                }
                            },
                            onDownloadQrPdf = { selections, size ->
                                val storeObj = activeStore ?: return@QrStudioScreen
                                val pdf = ReceiptPrintAndExport.generateQrStickerSheetPdfBytes(storeObj, selections, size)
                                exportBytes("QR-Sheet-${storeObj.invoicePrefix}.pdf", pdf)
                            }
                        )

                        AppSection.REPORTS -> ReportsScreen(
                            store = activeStore,
                            sales = sales,
                            onViewDigitalBill = { viewModel.openDigitalBillPreview(it) },
                            onPrintReceipt = { printOrFallbackPdf(it) },
                            onDownloadPdf = {
                                val pdf = ReceiptPrintAndExport.generateInvoicePdfBytes(it, thermalMode = true)
                                exportBytes("Bill-${it.invoiceNumber}.pdf", pdf)
                            },
                            onOpenNfcShare = { viewModel.openNfcShareModal(it) },
                            onExportExcel = {
                                val storeObj = activeStore ?: return@ReportsScreen
                                val xlsx = ReceiptPrintAndExport.generateExcelXlsxBytes(storeObj, sales, products)
                                exportBytes("${storeObj.invoicePrefix}-Sales-GST-Report.xlsx", xlsx)
                            }
                        )

                        AppSection.STORE_SETTINGS -> StoreSettingsScreen(
                            stores = stores,
                            activeStore = activeStore,
                            onSelectStore = { viewModel.selectStore(it) },
                            onSaveStore = { storeObj, isNew -> viewModel.saveStore(storeObj, isNew) },
                            onDeleteStore = { viewModel.deleteStore(it) }
                        )

                        AppSection.HANDHELD_SCANNER -> HandheldScannerScreen(
                            pairedRoom = pairedHandheldRoom,
                            store = activeStore,
                            products = products,
                            onConnectRoom = { viewModel.connectAsHandheldScanner(it) },
                            onDisconnectRoom = { viewModel.disconnectAsHandheldScanner() },
                            onSendCodeToTerminal = { viewModel.sendCodeFromHandheld(it) }
                        )
                    }
                }
            }
        }
    }
}
