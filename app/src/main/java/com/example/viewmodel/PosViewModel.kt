package com.example.viewmodel

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CollectionSnapshot
import com.example.data.firebase.KalamRepository
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.ScannerRoom
import com.example.data.model.StockLog
import com.example.data.model.StockLogReason
import com.example.data.model.Store
import com.example.data.model.SyncMetrics
import com.example.data.model.UiState
import com.example.util.NetworkMonitor
import com.example.util.QrCodeUtils
import com.example.util.SoundFeedback
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppSection(val title: String) {
    POS_COUNTER("POS Counter"),
    DASHBOARD("Dashboard"),
    PRODUCTS("Products"),
    QR_STUDIO("QR Sheet"),
    REPORTS("Reports"),
    STORE_SETTINGS("Store Settings"),
    HANDHELD_SCANNER("Handheld Scanner")
}

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val repository: KalamRepository,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedStoreId = MutableStateFlow<String?>(null)
    val selectedStoreId: StateFlow<String?> = _selectedStoreId.asStateFlow()

    private val _currentSection = MutableStateFlow(AppSection.POS_COUNTER)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val paymentMethod: StateFlow<PaymentMethod> = _paymentMethod.asStateFlow()

    private val _generatedSaleModal = MutableStateFlow<Sale?>(null)
    val generatedSaleModal: StateFlow<Sale?> = _generatedSaleModal.asStateFlow()

    private val _digitalBillPreview = MutableStateFlow<Sale?>(null)
    val digitalBillPreview: StateFlow<Sale?> = _digitalBillPreview.asStateFlow()

    private val _nfcShareSale = MutableStateFlow<Sale?>(null)
    val nfcShareSale: StateFlow<Sale?> = _nfcShareSale.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _forceOffline = MutableStateFlow(false)
    val forceOffline: StateFlow<Boolean> = _forceOffline.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Persistent 4-digit Wireless Terminal Room Code (#XXXX)
    private val _terminalRoomCode = MutableStateFlow(
        Random.nextInt(1000, 9999).toString()
    )
    val terminalRoomCode: StateFlow<String> = _terminalRoomCode.asStateFlow()

    // Handheld Scanner paired room code when this phone acts as a wireless scanner
    private val _pairedHandheldRoom = MutableStateFlow<ScannerRoom?>(null)
    val pairedHandheldRoom: StateFlow<ScannerRoom?> = _pairedHandheldRoom.asStateFlow()

    private var lastProcessedScanNonce: Long = -1L

    // --- FIRESTORE STREAMS WITH TWO-TIER ERROR UNWRAPPING ---

    private fun <T> observeSafe(
        source: kotlinx.coroutines.flow.Flow<CollectionSnapshot<T>>,
        label: String
    ): StateFlow<UiState<CollectionSnapshot<T>>> = source
        .map<CollectionSnapshot<T>, UiState<CollectionSnapshot<T>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing $label", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load $label"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val storesState: StateFlow<UiState<CollectionSnapshot<Store>>> =
        observeSafe(repository.observeStores(currentUserId), "stores")

    val allProductsState: StateFlow<UiState<CollectionSnapshot<Product>>> =
        observeSafe(repository.observeProducts(currentUserId), "products")

    val allSalesState: StateFlow<UiState<CollectionSnapshot<Sale>>> =
        observeSafe(repository.observeSales(currentUserId), "sales")

    val allStockLogsState: StateFlow<UiState<CollectionSnapshot<StockLog>>> =
        observeSafe(repository.observeStockLogs(currentUserId), "stock logs")

    val networkOnline: StateFlow<Boolean> = networkMonitor.observeOnlineStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val stores: StateFlow<List<Store>> = storesState
        .map { (it as? UiState.Success)?.data?.items.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeStore: StateFlow<Store?> = combine(stores, _selectedStoreId) { list, chosenId ->
        list.firstOrNull { it.id == chosenId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeProducts: StateFlow<List<Product>> = combine(allProductsState, activeStore) { state, store ->
        val items = (state as? UiState.Success)?.data?.items.orEmpty()
        if (store == null) emptyList() else items.filter { it.storeId == store.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeSales: StateFlow<List<Sale>> = combine(allSalesState, activeStore) { state, store ->
        val items = (state as? UiState.Success)?.data?.items.orEmpty()
        if (store == null) emptyList() else items.filter { it.storeId == store.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeStockLogs: StateFlow<List<StockLog>> = combine(allStockLogsState, activeStore) { state, store ->
        val items = (state as? UiState.Success)?.data?.items.orEmpty()
        if (store == null) emptyList() else items.filter { it.storeId == store.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val hostScannerRoom: StateFlow<ScannerRoom?> = _terminalRoomCode
        .flatMapLatest { code ->
            if (code.isBlank()) flowOf(null)
            else repository.observeScannerRoom(currentUserId, code)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    emit(null)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val syncMetrics: StateFlow<SyncMetrics> = combine(
        storesState,
        allProductsState,
        allSalesState,
        allStockLogsState,
        combine(_forceOffline, networkOnline) { forced, online -> forced to online }
    ) { st, pr, sa, lg, net ->
        val sSnap = (st as? UiState.Success)?.data
        val pSnap = (pr as? UiState.Success)?.data
        val saSnap = (sa as? UiState.Success)?.data
        val lgSnap = (lg as? UiState.Success)?.data

        val sCount = sSnap?.items?.size ?: 0
        val pCount = pSnap?.items?.size ?: 0
        val saCount = saSnap?.items?.size ?: 0
        val lgCount = lgSnap?.items?.size ?: 0

        val pendingFlags = listOfNotNull(
            sSnap?.hasPendingWrites,
            pSnap?.hasPendingWrites,
            saSnap?.hasPendingWrites,
            lgSnap?.hasPendingWrites
        ).count { it }

        val estKb = ((sCount * 0.6) + (pCount * 0.5) + (saCount * 1.4) + (lgCount * 0.35))
        SyncMetrics(
            storeCount = sCount,
            productCount = pCount,
            saleCount = saCount,
            stockLogCount = lgCount,
            estimatedCacheKb = estKb,
            pendingWritesCount = pendingFlags,
            forceOffline = net.first,
            networkAvailable = net.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SyncMetrics())

    init {
        // Auto-provision default initial business branch if owner has zero stores yet
        viewModelScope.launch {
            storesState.collect { state ->
                if (state is UiState.Success && state.data.items.isEmpty() && !state.data.fromCache) {
                    val defaultStore = Store(
                        name = "Main Counter Store",
                        category = "Retail",
                        currencySymbol = "₹",
                        invoicePrefix = "KBP",
                        thermalWidthMm = 80,
                        receiptHeader = "TAX INVOICE",
                        receiptFooter = "Thank you for shopping with us!",
                        allowNegativeStock = false
                    )
                    val created = repository.createStore(currentUserId, defaultStore)
                    created.onSuccess { id -> _selectedStoreId.value = id }
                } else if (state is UiState.Success && state.data.items.isNotEmpty() && _selectedStoreId.value == null) {
                    _selectedStoreId.value = state.data.items.first().id
                }
            }
        }

        // Keep Wireless Terminal Room registered for the active store
        viewModelScope.launch {
            combine(activeStore, _terminalRoomCode) { store, code -> store to code }
                .collect { (store, code) ->
                    if (store != null && code.isNotBlank()) {
                        repository.ensureScannerRoom(currentUserId, code, store.id)
                    }
                }
        }

        // Listen for incoming remote wireless phone scans and push into live POS cart
        viewModelScope.launch {
            hostScannerRoom.collect { room ->
                if (room != null && room.lastScanNonce > 0L) {
                    if (lastProcessedScanNonce == -1L) {
                        lastProcessedScanNonce = room.lastScanNonce
                    } else if (room.lastScanNonce > lastProcessedScanNonce && room.lastScanCode.isNotBlank()) {
                        lastProcessedScanNonce = room.lastScanNonce
                        handleScannedOrEnteredCode(room.lastScanCode, fromWireless = true)
                    }
                }
            }
        }
    }

    // --- NAVIGATION & MODE CONTROLS ---

    fun navigateTo(section: AppSection) {
        _currentSection.value = section
    }

    fun toggleAdminPosMode() {
        _currentSection.value = if (_currentSection.value == AppSection.POS_COUNTER) {
            AppSection.DASHBOARD
        } else {
            AppSection.POS_COUNTER
        }
    }

    fun selectStore(storeId: String) {
        if (_selectedStoreId.value != storeId) {
            _selectedStoreId.value = storeId
            _cartItems.value = emptyList()
            _discountAmount.value = 0.0
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- CART & FAST BILLING ACTIONS ---

    fun handleScannedOrEnteredCode(rawInput: String, fromWireless: Boolean = false): Boolean {
        val code = QrCodeUtils.extractProductCode(rawInput)
        if (code.isBlank()) return false
        val products = activeProducts.value
        val matched = products.firstOrNull {
            it.shortCode.equals(code, ignoreCase = true) ||
                it.sku.equals(code, ignoreCase = true) ||
                it.id == code ||
                it.qrPayload.equals(rawInput.trim(), ignoreCase = true)
        }
        return if (matched != null) {
            val added = addToCart(matched)
            if (added) {
                SoundFeedback.playScanBeep()
                if (fromWireless) {
                    showMessage("Wireless scan added: ${matched.name} (#${matched.shortCode})")
                }
            }
            added
        } else {
            showMessage("No product found for code '$code'")
            false
        }
    }

    fun addToCart(product: Product): Boolean {
        val store = activeStore.value
        val allowNeg = store?.allowNegativeStock == true
        val currentList = _cartItems.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.product.id == product.id }
        val nextQty = if (existingIdx >= 0) currentList[existingIdx].quantity + 1 else 1

        if (!allowNeg && nextQty > product.stockQty) {
            showMessage("Insufficient stock for ${product.name} (Available: ${product.stockQty.toInt()} ${product.unit})")
            return false
        }
        if (existingIdx >= 0) {
            currentList[existingIdx] = currentList[existingIdx].copy(product = product, quantity = nextQty)
        } else {
            currentList.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = currentList
        return true
    }

    fun updateCartQuantity(productId: String, delta: Int) {
        val store = activeStore.value
        val allowNeg = store?.allowNegativeStock == true
        val currentList = _cartItems.value.toMutableList()
        val idx = currentList.indexOfFirst { it.product.id == productId }
        if (idx < 0) return
        val item = currentList[idx]
        val newQty = item.quantity + delta
        if (newQty <= 0) {
            currentList.removeAt(idx)
        } else {
            if (!allowNeg && newQty > item.product.stockQty) {
                showMessage("Only ${item.product.stockQty.toInt()} ${item.product.unit} in stock for ${item.product.name}")
                return
            }
            currentList[idx] = item.copy(quantity = newQty)
        }
        _cartItems.value = currentList
    }

    fun removeFromCart(productId: String) {
        _cartItems.update { list -> list.filterNot { it.product.id == productId } }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _discountAmount.value = 0.0
    }

    fun setDiscountAmount(amount: Double) {
        _discountAmount.value = amount.coerceAtLeast(0.0)
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
    }

    fun generateBill() {
        val store = activeStore.value
        val items = _cartItems.value
        if (store == null) {
            showMessage("Please create or select a store first.")
            return
        }
        if (items.isEmpty()) {
            showMessage("Add at least one item to generate a bill.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.completeSale(
                userId = currentUserId,
                store = store,
                cartItems = items,
                discountAmount = _discountAmount.value,
                paymentMethod = _paymentMethod.value,
                existingSalesCount = activeSales.value.size
            )
            _isBusy.value = false
            result.onSuccess { sale ->
                SoundFeedback.playCheckoutSuccess()
                clearCart()
                // Opens Bill Generated Successfully modal FIRST — NEVER auto-prints!
                _generatedSaleModal.value = sale
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Failed to generate bill.")
            }
        }
    }

    fun dismissGeneratedSaleModal() {
        _generatedSaleModal.value = null
    }

    fun openDigitalBillPreview(sale: Sale) {
        _digitalBillPreview.value = sale
    }

    fun dismissDigitalBillPreview() {
        _digitalBillPreview.value = null
    }

    fun openNfcShareModal(sale: Sale) {
        _nfcShareSale.value = sale
    }

    fun dismissNfcShareModal() {
        _nfcShareSale.value = null
    }

    // --- PRODUCT & INVENTORY CRUD ---

    fun nextAutoShortCode(): String {
        val codes = activeProducts.value.mapNotNull { it.shortCode.toIntOrNull() }
        val maxCode = codes.maxOrNull() ?: 1000
        return (maxCode + 1).coerceAtLeast(1001).toString()
    }

    fun saveProduct(product: Product, isEdit: Boolean, onDone: () -> Unit = {}) {
        val store = activeStore.value
        if (store == null) {
            showMessage("Select a business branch first.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = if (isEdit) {
                repository.updateProduct(currentUserId, product)
            } else {
                repository.createProduct(currentUserId, product.copy(storeId = store.id))
            }
            _isBusy.value = false
            res.onSuccess {
                showMessage(if (isEdit) "Product updated" else "Product added (#${product.shortCode})")
                onDone()
            }.onFailure {
                showMessage(it.localizedMessage ?: "Unable to save product.")
            }
        }
    }

    fun adjustStock(product: Product, deltaQty: Double, reason: StockLogReason, note: String) {
        viewModelScope.launch {
            val res = repository.adjustProductStock(currentUserId, product, deltaQty, reason, note)
            res.onSuccess {
                showMessage("Stock updated for ${product.name}")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to adjust stock.")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val res = repository.deleteProduct(currentUserId, product.id)
            res.onSuccess {
                showMessage("Deleted ${product.name}")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to delete product.")
            }
        }
    }

    // --- STORE SETTINGS & MULTI-BUSINESS ---

    fun saveStore(store: Store, isNew: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            if (isNew) {
                repository.createStore(currentUserId, store).onSuccess { newId ->
                    _selectedStoreId.value = newId
                    showMessage("Created branch: ${store.name}")
                    onDone()
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Failed to create store.")
                }
            } else {
                repository.updateStore(currentUserId, store).onSuccess {
                    showMessage("Saved settings for ${store.name}")
                    onDone()
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Failed to update store.")
                }
            }
            _isBusy.value = false
        }
    }

    fun deleteStore(store: Store) {
        if (stores.value.size <= 1) {
            showMessage("At least one active store branch is required.")
            return
        }
        viewModelScope.launch {
            repository.deleteStore(currentUserId, store.id).onSuccess {
                showMessage("Removed branch: ${store.name}")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to delete branch.")
            }
        }
    }

    // --- WIRELESS PHONE-TO-TERMINAL SCANNER ---

    fun disconnectRemoteScanner() {
        viewModelScope.launch {
            repository.disconnectHandheldScanner(currentUserId, _terminalRoomCode.value)
                .onSuccess { showMessage("Disconnected wireless scanner") }
                .onFailure { showMessage(it.localizedMessage ?: "Disconnect failed") }
        }
    }

    fun connectAsHandheldScanner(roomCode: String) {
        val cleaned = QrCodeUtils.extractScannerRoomCode(roomCode) ?: roomCode.trim()
        if (cleaned.length !in 4..8) {
            showMessage("Enter a valid 4-digit Terminal Room Code.")
            return
        }
        val scannerName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Handheld Phone" }
        viewModelScope.launch {
            _isBusy.value = true
            repository.connectHandheldScanner(currentUserId, cleaned, scannerName)
                .onSuccess { room ->
                    _pairedHandheldRoom.value = room
                    if (room.storeId.isNotBlank()) {
                        _selectedStoreId.value = room.storeId
                    }
                    SoundFeedback.playScanBeep()
                    showMessage("Paired with Terminal #$cleaned")
                }
                .onFailure {
                    showMessage(it.localizedMessage ?: "Could not pair with Room #$cleaned")
                }
            _isBusy.value = false
        }
    }

    fun disconnectAsHandheldScanner() {
        val room = _pairedHandheldRoom.value ?: return
        viewModelScope.launch {
            repository.disconnectHandheldScanner(currentUserId, room.roomCode)
            _pairedHandheldRoom.value = null
            showMessage("Disconnected from Terminal #${room.roomCode}")
        }
    }

    fun sendCodeFromHandheld(rawCode: String) {
        val room = _pairedHandheldRoom.value
        if (room == null) {
            showMessage("Pair with a Terminal Room (#XXXX) first.")
            return
        }
        val scannerName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Handheld Phone" }
        viewModelScope.launch {
            repository.publishScanCode(currentUserId, room.roomCode, rawCode, scannerName)
                .onSuccess {
                    SoundFeedback.playScanBeep()
                    showMessage("Sent '${QrCodeUtils.extractProductCode(rawCode)}' to Terminal #${room.roomCode}")
                }
                .onFailure {
                    showMessage(it.localizedMessage ?: "Failed to stream scan code")
                }
        }
    }

    // --- OFFLINE MODE, SYNC & JSON BACKUP / RESTORE ---

    fun toggleForceOffline(force: Boolean) {
        _forceOffline.value = force
        viewModelScope.launch {
            repository.setOfflineNetworkEnabled(!force).onSuccess {
                showMessage(if (force) "Force Offline Mode enabled" else "Online Cloud Sync resumed")
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _forceOffline.value = false
            _isBusy.value = true
            repository.syncPendingWrites().onSuccess {
                showMessage("All local records synced to Cloud Firestore")
            }.onFailure {
                showMessage(it.localizedMessage ?: "Sync will complete automatically when online")
            }
            _isBusy.value = false
        }
    }

    fun buildBackupJson(): String {
        val allStores = (storesState.value as? UiState.Success)?.data?.items.orEmpty()
        val allProds = (allProductsState.value as? UiState.Success)?.data?.items.orEmpty()
        val allSales = (allSalesState.value as? UiState.Success)?.data?.items.orEmpty()
        val allLogs = (allStockLogsState.value as? UiState.Success)?.data?.items.orEmpty()
        return repository.exportBackupJson(allStores, allProds, allSales, allLogs)
    }

    fun restoreFromBackupJson(jsonText: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.restoreBackupJson(currentUserId, jsonText)
                .onSuccess { summary -> showMessage(summary) }
                .onFailure { err -> showMessage("Restore failed: ${err.localizedMessage}") }
            _isBusy.value = false
        }
    }

    private companion object {
        const val TAG = "PosViewModel"
    }
}
