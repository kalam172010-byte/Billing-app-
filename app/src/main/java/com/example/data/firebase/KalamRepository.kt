package com.example.data.firebase

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleLineItem
import com.example.data.model.ScannerRoom
import com.example.data.model.StockLog
import com.example.data.model.StockLogReason
import com.example.data.model.Store
import com.example.util.QrCodeUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

data class CollectionSnapshot<T>(
    val items: List<T>,
    val hasPendingWrites: Boolean,
    val fromCache: Boolean
)

class KalamRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    companion object {
        @Volatile
        private var configured = false

        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(databaseId)
            if (!configured) {
                synchronized(this) {
                    if (!configured) {
                        try {
                            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                                .setLocalCacheSettings(
                                    PersistentCacheSettings.newBuilder()
                                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                        .build()
                                )
                                .build()
                        } catch (_: IllegalStateException) {
                            // Settings already applied for this Firestore instance
                        }
                        configured = true
                    }
                }
            }
            return firestore
        }
    }

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("Signed-in user changed; refusing cross-user access.")
        }
        return uid
    }

    private fun userRoot(userId: String) = db.collection("users").document(userId)

    // --- REALTIME OBSERVATION FLOWS ---

    fun observeStores(userId: String): Flow<CollectionSnapshot<Store>> =
        userRoot(userId).collection("stores")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.toObjects(
                    Store::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                CollectionSnapshot(
                    items = list,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    fromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                logFlowError(error, userId, "users/$userId/stores")
                throw error
            }

    fun observeProducts(userId: String): Flow<CollectionSnapshot<Product>> =
        userRoot(userId).collection("products")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.shortCode }
                CollectionSnapshot(
                    items = list,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    fromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                logFlowError(error, userId, "users/$userId/products")
                throw error
            }

    fun observeSales(userId: String): Flow<CollectionSnapshot<Sale>> =
        userRoot(userId).collection("sales")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.toObjects(
                    Sale::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                CollectionSnapshot(
                    items = list,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    fromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                logFlowError(error, userId, "users/$userId/sales")
                throw error
            }

    fun observeStockLogs(userId: String): Flow<CollectionSnapshot<StockLog>> =
        userRoot(userId).collection("stockLogs")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val list = snapshot.toObjects(
                    StockLog::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                CollectionSnapshot(
                    items = list,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    fromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                logFlowError(error, userId, "users/$userId/stockLogs")
                throw error
            }

    fun observeScannerRoom(userId: String, roomCode: String): Flow<ScannerRoom?> =
        userRoot(userId).collection("scannerRooms").document(roomCode)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        ScannerRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowError(error, userId, "users/$userId/scannerRooms/$roomCode")
                throw error
            }

    private fun logFlowError(error: Throwable, expectedUid: String, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(ex, OperationType.LIST, path)
        }
    }

    // --- STORE MUTATIONS ---

    suspend fun createStore(userId: String, store: Store): Result<String> {
        var path = "users/$userId/stores"
        return try {
            val uid = requireUserId(userId)
            require(store.name.trim().isNotEmpty()) { "Store name cannot be empty." }
            val docRef = userRoot(uid).collection("stores").document()
            path = docRef.path
            docRef.set(store.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStore(userId: String, store: Store): Result<Unit> {
        var path = "users/$userId/stores/${store.id}"
        return try {
            val uid = requireUserId(userId)
            require(store.id.isNotBlank()) { "Store ID is required." }
            require(store.name.trim().isNotEmpty()) { "Store name cannot be empty." }
            val docRef = userRoot(uid).collection("stores").document(store.id)
            path = docRef.path
            docRef.update(store.toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStore(userId: String, storeId: String): Result<Unit> {
        val path = "users/$userId/stores/$storeId"
        return try {
            val uid = requireUserId(userId)
            require(storeId.isNotBlank()) { "Store ID is required." }
            userRoot(uid).collection("stores").document(storeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PRODUCT MUTATIONS ---

    suspend fun createProduct(userId: String, product: Product): Result<String> {
        var path = "users/$userId/products"
        return try {
            val uid = requireUserId(userId)
            require(product.storeId.isNotBlank()) { "Active store is required." }
            require(product.name.trim().isNotEmpty()) { "Product name is required." }
            require(product.shortCode.trim().isNotEmpty()) { "4-digit product code is required." }
            val docRef = userRoot(uid).collection("products").document()
            path = docRef.path

            val batch = db.batch()
            batch.set(docRef, product.toCreateMap(uid, docRef.id))

            if (product.stockQty != 0.0) {
                val logRef = userRoot(uid).collection("stockLogs").document()
                val initialLog = StockLog(
                    storeId = product.storeId,
                    productId = docRef.id,
                    productName = product.name.trim(),
                    shortCode = product.shortCode.trim(),
                    deltaQty = product.stockQty,
                    previousStock = 0.0,
                    newStock = product.stockQty,
                    reason = StockLogReason.INITIAL.name,
                    referenceInvoice = "Opening Stock"
                )
                batch.set(logRef, initialLog.toCreateMap(uid))
            }
            batch.commit().await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(userId: String, product: Product): Result<Unit> {
        val path = "users/$userId/products/${product.id}"
        return try {
            val uid = requireUserId(userId)
            require(product.id.isNotBlank()) { "Product ID is required." }
            require(product.name.trim().isNotEmpty()) { "Product name is required." }
            userRoot(uid).collection("products").document(product.id)
                .update(product.toUpdateMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustProductStock(
        userId: String,
        product: Product,
        deltaQty: Double,
        reason: StockLogReason,
        note: String
    ): Result<Unit> {
        val path = "users/$userId/products/${product.id}"
        return try {
            val uid = requireUserId(userId)
            val newStock = (product.stockQty + deltaQty).coerceIn(-1_000_000.0, 100_000_000.0)
            val batch = db.batch()
            val prodRef = userRoot(uid).collection("products").document(product.id)
            batch.update(
                prodRef,
                mapOf(
                    "stockQty" to newStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val logRef = userRoot(uid).collection("stockLogs").document()
            val log = StockLog(
                storeId = product.storeId,
                productId = product.id,
                productName = product.name,
                shortCode = product.shortCode,
                deltaQty = deltaQty,
                previousStock = product.stockQty,
                newStock = newStock,
                reason = reason.name,
                referenceInvoice = note.trim().take(60)
            )
            batch.set(logRef, log.toCreateMap(uid))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(userId: String, productId: String): Result<Unit> {
        val path = "users/$userId/products/$productId"
        return try {
            val uid = requireUserId(userId)
            require(productId.isNotBlank()) { "Product ID is required." }
            userRoot(uid).collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- POS CHECKOUT (ATOMIC SALE + STOCK DEDUCTION + STOCK LOGS) ---

    suspend fun completeSale(
        userId: String,
        store: Store,
        cartItems: List<CartItem>,
        discountAmount: Double,
        paymentMethod: PaymentMethod,
        existingSalesCount: Int
    ): Result<Sale> {
        var path = "users/$userId/sales"
        return try {
            val uid = requireUserId(userId)
            require(cartItems.isNotEmpty()) { "Cart is empty." }

            val saleRef = userRoot(uid).collection("sales").document()
            path = saleRef.path

            val shortBillId = generateShortBillId()
            val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val seq = (existingSalesCount + 1).toString().padStart(3, '0')
            val invoiceNumber = "${store.invoicePrefix.ifBlank { "KBP" }}-$dateStamp-$seq"

            val lineItems = cartItems.map { item ->
                val taxable = round2(item.taxableAmount)
                val gst = round2(item.gstAmount)
                SaleLineItem(
                    productId = item.product.id,
                    shortCode = item.product.shortCode,
                    name = item.product.name,
                    unit = item.product.unit,
                    quantity = item.quantity.toDouble(),
                    unitPrice = item.product.sellingPrice,
                    purchasePrice = item.product.purchasePrice,
                    gstPercent = item.product.gstPercent,
                    taxableAmount = taxable,
                    gstAmount = gst,
                    lineTotal = round2(taxable + gst)
                )
            }

            val subtotal = round2(lineItems.sumOf { it.taxableAmount })
            val gstTotal = round2(lineItems.sumOf { it.gstAmount })
            val clampedDiscount = round2(discountAmount.coerceIn(0.0, subtotal + gstTotal))
            val grandTotal = round2((subtotal + gstTotal - clampedDiscount).coerceAtLeast(0.0))
            val totalCost = round2(cartItems.sumOf { it.costTotal })
            val totalUnits = cartItems.sumOf { it.quantity }.toDouble()

            val draftSale = Sale(
                id = saleRef.id,
                userId = uid,
                storeId = store.id,
                storeName = store.name,
                storeAddress = store.address,
                storePhone = store.phone,
                storeGstin = store.gstin,
                storeUpiId = store.upiId,
                currencySymbol = store.currencySymbol,
                receiptHeader = store.receiptHeader,
                receiptFooter = store.receiptFooter,
                thermalWidthMm = store.thermalWidthMm,
                shortBillId = shortBillId,
                invoiceNumber = invoiceNumber,
                paymentMethod = paymentMethod.name,
                items = lineItems,
                subtotal = subtotal,
                gstTotal = gstTotal,
                discountAmount = clampedDiscount,
                grandTotal = grandTotal,
                totalCost = totalCost,
                totalUnits = totalUnits
            )

            val digitalBillUrl = QrCodeUtils.buildSelfContainedBillUrl(draftSale)
            val finalSale = draftSale.copy(digitalBillUrl = digitalBillUrl)

            val batch = db.batch()
            batch.set(saleRef, finalSale.toCreateMap(uid))

            for (cartItem in cartItems) {
                val product = cartItem.product
                val previousStock = product.stockQty
                val newStock = (previousStock - cartItem.quantity)
                    .let { if (store.allowNegativeStock) it else it.coerceAtLeast(0.0) }
                    .coerceIn(-1_000_000.0, 100_000_000.0)

                val prodRef = userRoot(uid).collection("products").document(product.id)
                batch.update(
                    prodRef,
                    mapOf(
                        "stockQty" to newStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val logRef = userRoot(uid).collection("stockLogs").document()
                val log = StockLog(
                    storeId = store.id,
                    productId = product.id,
                    productName = product.name,
                    shortCode = product.shortCode,
                    deltaQty = -cartItem.quantity.toDouble(),
                    previousStock = previousStock,
                    newStock = newStock,
                    reason = StockLogReason.SALE.name,
                    referenceInvoice = invoiceNumber
                )
                batch.set(logRef, log.toCreateMap(uid))
            }

            batch.commit().await()
            Result.success(finalSale)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSale(userId: String, saleId: String): Result<Unit> {
        val path = "users/$userId/sales/$saleId"
        return try {
            val uid = requireUserId(userId)
            userRoot(uid).collection("sales").document(saleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- WIRELESS PHONE-TO-TERMINAL SCANNER ROOM ---

    suspend fun ensureScannerRoom(userId: String, roomCode: String, storeId: String): Result<Unit> {
        val path = "users/$userId/scannerRooms/$roomCode"
        return try {
            val uid = requireUserId(userId)
            require(roomCode.length in 4..8) { "Invalid room code." }
            require(storeId.isNotBlank()) { "Active store is required." }
            val docRef = userRoot(uid).collection("scannerRooms").document(roomCode)
            val existing = docRef.get().await()
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80).ifBlank { "POS Terminal" }
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "storeId" to storeId,
                        "hostDeviceName" to deviceName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "roomCode" to roomCode,
                        "storeId" to storeId,
                        "hostDeviceName" to deviceName,
                        "connectedScannerName" to "",
                        "scannerConnected" to false,
                        "lastScanCode" to "",
                        "lastScanNonce" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun connectHandheldScanner(userId: String, roomCode: String, scannerName: String): Result<ScannerRoom> {
        val path = "users/$userId/scannerRooms/$roomCode"
        return try {
            val uid = requireUserId(userId)
            val docRef = userRoot(uid).collection("scannerRooms").document(roomCode)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("Terminal Room #$roomCode was not found."))
            }
            docRef.update(
                mapOf(
                    "connectedScannerName" to scannerName.trim().ifBlank { "Handheld Scanner" }.take(100),
                    "scannerConnected" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val room = snap.toObject(ScannerRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: ScannerRoom(id = roomCode, roomCode = roomCode)
            Result.success(room.copy(connectedScannerName = scannerName, scannerConnected = true))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun disconnectHandheldScanner(userId: String, roomCode: String): Result<Unit> {
        val path = "users/$userId/scannerRooms/$roomCode"
        return try {
            val uid = requireUserId(userId)
            userRoot(uid).collection("scannerRooms").document(roomCode).update(
                mapOf(
                    "connectedScannerName" to "",
                    "scannerConnected" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishScanCode(
        userId: String,
        roomCode: String,
        code: String,
        scannerName: String
    ): Result<Unit> {
        val path = "users/$userId/scannerRooms/$roomCode"
        return try {
            val uid = requireUserId(userId)
            val docRef = userRoot(uid).collection("scannerRooms").document(roomCode)
            docRef.update(
                mapOf(
                    "lastScanCode" to code.trim().take(200),
                    "lastScanNonce" to FieldValue.increment(1L),
                    "connectedScannerName" to scannerName.trim().ifBlank { "Handheld Scanner" }.take(100),
                    "scannerConnected" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- OFFLINE MODE, SYNC & JSON BACKUP / RESTORE ---

    suspend fun setOfflineNetworkEnabled(enabled: Boolean): Result<Unit> {
        return try {
            if (enabled) db.enableNetwork().await() else db.disableNetwork().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPendingWrites(): Result<Unit> {
        return try {
            db.enableNetwork().await()
            db.waitForPendingWrites().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportBackupJson(
        stores: List<Store>,
        products: List<Product>,
        sales: List<Sale>,
        stockLogs: List<StockLog>
    ): String {
        val root = JSONObject()
        root.put("app", "KALAM BILLING PRO")
        root.put("schemaVersion", 1)
        root.put("exportedAtIso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        val storesArr = JSONArray()
        stores.forEach { s ->
            storesArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("category", s.category)
                    put("address", s.address)
                    put("phone", s.phone)
                    put("gstin", s.gstin)
                    put("upiId", s.upiId)
                    put("currencySymbol", s.currencySymbol)
                    put("invoicePrefix", s.invoicePrefix)
                    put("thermalWidthMm", s.thermalWidthMm)
                    put("receiptHeader", s.receiptHeader)
                    put("receiptFooter", s.receiptFooter)
                    put("allowNegativeStock", s.allowNegativeStock)
                }
            )
        }
        root.put("stores", storesArr)

        val productsArr = JSONArray()
        products.forEach { p ->
            productsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("storeId", p.storeId)
                    put("shortCode", p.shortCode)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("purchasePrice", p.purchasePrice)
                    put("sellingPrice", p.sellingPrice)
                    put("gstPercent", p.gstPercent)
                    put("stockQty", p.stockQty)
                    put("minStockAlert", p.minStockAlert)
                    put("unit", p.unit)
                }
            )
        }
        root.put("products", productsArr)

        val salesArr = JSONArray()
        sales.forEach { sale ->
            val itemsArr = JSONArray()
            sale.items.forEach { li ->
                itemsArr.put(
                    JSONObject().apply {
                        put("productId", li.productId)
                        put("shortCode", li.shortCode)
                        put("name", li.name)
                        put("unit", li.unit)
                        put("quantity", li.quantity)
                        put("unitPrice", li.unitPrice)
                        put("purchasePrice", li.purchasePrice)
                        put("gstPercent", li.gstPercent)
                        put("taxableAmount", li.taxableAmount)
                        put("gstAmount", li.gstAmount)
                        put("lineTotal", li.lineTotal)
                    }
                )
            }
            salesArr.put(
                JSONObject().apply {
                    put("id", sale.id)
                    put("storeId", sale.storeId)
                    put("storeName", sale.storeName)
                    put("storeAddress", sale.storeAddress)
                    put("storePhone", sale.storePhone)
                    put("storeGstin", sale.storeGstin)
                    put("storeUpiId", sale.storeUpiId)
                    put("currencySymbol", sale.currencySymbol)
                    put("receiptHeader", sale.receiptHeader)
                    put("receiptFooter", sale.receiptFooter)
                    put("thermalWidthMm", sale.thermalWidthMm)
                    put("shortBillId", sale.shortBillId)
                    put("invoiceNumber", sale.invoiceNumber)
                    put("paymentMethod", sale.paymentMethod)
                    put("items", itemsArr)
                    put("subtotal", sale.subtotal)
                    put("gstTotal", sale.gstTotal)
                    put("discountAmount", sale.discountAmount)
                    put("grandTotal", sale.grandTotal)
                    put("totalCost", sale.totalCost)
                    put("totalUnits", sale.totalUnits)
                    put("digitalBillUrl", sale.digitalBillUrl)
                }
            )
        }
        root.put("sales", salesArr)

        val logsArr = JSONArray()
        stockLogs.forEach { log ->
            logsArr.put(
                JSONObject().apply {
                    put("id", log.id)
                    put("storeId", log.storeId)
                    put("productId", log.productId)
                    put("productName", log.productName)
                    put("shortCode", log.shortCode)
                    put("deltaQty", log.deltaQty)
                    put("previousStock", log.previousStock)
                    put("newStock", log.newStock)
                    put("reason", log.reason)
                    put("referenceInvoice", log.referenceInvoice)
                }
            )
        }
        root.put("stockLogs", logsArr)
        return root.toString(2)
    }

    suspend fun restoreBackupJson(userId: String, jsonText: String): Result<String> {
        var path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val root = JSONObject(jsonText)
            val storesArr = root.optJSONArray("stores") ?: JSONArray()
            val productsArr = root.optJSONArray("products") ?: JSONArray()
            val salesArr = root.optJSONArray("sales") ?: JSONArray()

            val storeIdMap = mutableMapOf<String, String>()
            var storesRestored = 0
            for (i in 0 until storesArr.length()) {
                val obj = storesArr.getJSONObject(i)
                val oldId = obj.optString("id")
                val docRef = userRoot(uid).collection("stores").document()
                path = docRef.path
                val store = Store(
                    name = obj.optString("name", "Restored Store"),
                    category = obj.optString("category", "Retail"),
                    address = obj.optString("address", ""),
                    phone = obj.optString("phone", ""),
                    gstin = obj.optString("gstin", ""),
                    upiId = obj.optString("upiId", ""),
                    currencySymbol = obj.optString("currencySymbol", "₹"),
                    invoicePrefix = obj.optString("invoicePrefix", "KBP"),
                    thermalWidthMm = obj.optInt("thermalWidthMm", 80),
                    receiptHeader = obj.optString("receiptHeader", ""),
                    receiptFooter = obj.optString("receiptFooter", "Thank you!"),
                    allowNegativeStock = obj.optBoolean("allowNegativeStock", false)
                )
                docRef.set(store.toCreateMap(uid)).await()
                if (oldId.isNotBlank()) storeIdMap[oldId] = docRef.id
                storesRestored++
            }

            var productsRestored = 0
            for (i in 0 until productsArr.length()) {
                val obj = productsArr.getJSONObject(i)
                val mappedStoreId = storeIdMap[obj.optString("storeId")] ?: continue
                val docRef = userRoot(uid).collection("products").document()
                path = docRef.path
                val prod = Product(
                    storeId = mappedStoreId,
                    shortCode = obj.optString("shortCode", (1001 + i).toString()),
                    name = obj.optString("name", "Item ${i + 1}"),
                    sku = obj.optString("sku", ""),
                    category = obj.optString("category", ""),
                    purchasePrice = obj.optDouble("purchasePrice", 0.0),
                    sellingPrice = obj.optDouble("sellingPrice", 0.0),
                    gstPercent = obj.optDouble("gstPercent", 0.0),
                    stockQty = obj.optDouble("stockQty", 0.0),
                    minStockAlert = obj.optDouble("minStockAlert", 5.0),
                    unit = obj.optString("unit", "pcs")
                )
                docRef.set(prod.toCreateMap(uid, docRef.id)).await()
                productsRestored++
            }

            var salesRestored = 0
            for (i in 0 until salesArr.length()) {
                val obj = salesArr.getJSONObject(i)
                val mappedStoreId = storeIdMap[obj.optString("storeId")] ?: continue
                val itemsArr = obj.optJSONArray("items") ?: continue
                if (itemsArr.length() == 0) continue
                val lineItems = mutableListOf<SaleLineItem>()
                for (j in 0 until itemsArr.length()) {
                    val li = itemsArr.getJSONObject(j)
                    lineItems.add(
                        SaleLineItem(
                            productId = li.optString("productId", "restored_$j"),
                            shortCode = li.optString("shortCode", "1001"),
                            name = li.optString("name", "Item"),
                            unit = li.optString("unit", "pcs"),
                            quantity = li.optDouble("quantity", 1.0),
                            unitPrice = li.optDouble("unitPrice", 0.0),
                            purchasePrice = li.optDouble("purchasePrice", 0.0),
                            gstPercent = li.optDouble("gstPercent", 0.0),
                            taxableAmount = li.optDouble("taxableAmount", 0.0),
                            gstAmount = li.optDouble("gstAmount", 0.0),
                            lineTotal = li.optDouble("lineTotal", 0.0)
                        )
                    )
                }
                val docRef = userRoot(uid).collection("sales").document()
                path = docRef.path
                val sale = Sale(
                    storeId = mappedStoreId,
                    storeName = obj.optString("storeName", "Store"),
                    storeAddress = obj.optString("storeAddress", ""),
                    storePhone = obj.optString("storePhone", ""),
                    storeGstin = obj.optString("storeGstin", ""),
                    storeUpiId = obj.optString("storeUpiId", ""),
                    currencySymbol = obj.optString("currencySymbol", "₹"),
                    receiptHeader = obj.optString("receiptHeader", ""),
                    receiptFooter = obj.optString("receiptFooter", ""),
                    thermalWidthMm = obj.optInt("thermalWidthMm", 80),
                    shortBillId = obj.optString("shortBillId", generateShortBillId()),
                    invoiceNumber = obj.optString("invoiceNumber", "RESTORED-${i + 1}"),
                    paymentMethod = obj.optString("paymentMethod", PaymentMethod.CASH.name),
                    items = lineItems,
                    subtotal = obj.optDouble("subtotal", 0.0),
                    gstTotal = obj.optDouble("gstTotal", 0.0),
                    discountAmount = obj.optDouble("discountAmount", 0.0),
                    grandTotal = obj.optDouble("grandTotal", 0.0),
                    totalCost = obj.optDouble("totalCost", 0.0),
                    totalUnits = obj.optDouble("totalUnits", 1.0),
                    digitalBillUrl = obj.optString("digitalBillUrl", "https://kalambilling.app/b/RESTOR")
                )
                docRef.set(sale.toCreateMap(uid)).await()
                salesRestored++
            }

            Result.success("Restored $storesRestored stores, $productsRestored products, and $salesRestored bills.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun generateShortBillId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
