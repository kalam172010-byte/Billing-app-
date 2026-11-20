package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class StockLogReason(val label: String) {
    SALE("POS Sale"),
    RESTOCK("Restock"),
    ADJUSTMENT("Adjustment"),
    INITIAL("Opening Stock"),
    DAMAGE("Damage / Loss");

    companion object {
        fun fromRaw(raw: String): StockLogReason =
            entries.firstOrNull { it.name == raw } ?: ADJUSTMENT
    }
}

enum class QrLabelSize(
    val label: String,
    val widthMm: Float,
    val heightMm: Float,
    val columnsOnA4: Int
) {
    SMALL_38X25("38 × 25 mm", 38f, 25f, 5),
    MEDIUM_50X25("50 × 25 mm", 50f, 25f, 4),
    LARGE_70X35("70 × 35 mm", 70f, 35f, 2)
}

@Keep
@IgnoreExtraProperties
data class Store(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Retail",
    val address: String = "",
    val phone: String = "",
    val gstin: String = "",
    val upiId: String = "",
    val currencySymbol: String = "₹",
    val invoicePrefix: String = "KBP",
    val thermalWidthMm: Int = 80,
    val receiptHeader: String = "",
    val receiptFooter: String = "Thank you for your business!",
    val allowNegativeStock: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name.trim().take(100),
        "category" to category.trim().ifBlank { "Retail" }.take(60),
        "address" to address.trim().take(250),
        "phone" to phone.trim().take(30),
        "gstin" to gstin.trim().uppercase().take(30),
        "upiId" to upiId.trim().take(100),
        "currencySymbol" to currencySymbol.trim().ifBlank { "₹" }.take(8),
        "invoicePrefix" to invoicePrefix.trim().uppercase().ifBlank { "KBP" }.take(16),
        "thermalWidthMm" to if (thermalWidthMm == 58) 58 else 80,
        "receiptHeader" to receiptHeader.trim().take(200),
        "receiptFooter" to receiptFooter.trim().take(200),
        "allowNegativeStock" to allowNegativeStock,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(100),
        "category" to category.trim().ifBlank { "Retail" }.take(60),
        "address" to address.trim().take(250),
        "phone" to phone.trim().take(30),
        "gstin" to gstin.trim().uppercase().take(30),
        "upiId" to upiId.trim().take(100),
        "currencySymbol" to currencySymbol.trim().ifBlank { "₹" }.take(8),
        "invoicePrefix" to invoicePrefix.trim().uppercase().ifBlank { "KBP" }.take(16),
        "thermalWidthMm" to if (thermalWidthMm == 58) 58 else 80,
        "receiptHeader" to receiptHeader.trim().take(200),
        "receiptFooter" to receiptFooter.trim().take(200),
        "allowNegativeStock" to allowNegativeStock,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val shortCode: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val gstPercent: Double = 0.0,
    val stockQty: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val unit: String = "pcs",
    val qrPayload: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String, productDocId: String): Map<String, Any> {
        val code = shortCode.trim().take(12)
        val qr = "KBP-PROD:$code:$productDocId"
        return mapOf(
            "userId" to ownerUid,
            "storeId" to storeId,
            "shortCode" to code,
            "name" to name.trim().take(120),
            "sku" to sku.trim().take(60),
            "category" to category.trim().take(60),
            "purchasePrice" to purchasePrice.coerceIn(0.0, 100_000_000.0),
            "sellingPrice" to sellingPrice.coerceIn(0.0, 100_000_000.0),
            "gstPercent" to gstPercent.coerceIn(0.0, 100.0),
            "stockQty" to stockQty.coerceIn(-1_000_000.0, 100_000_000.0),
            "minStockAlert" to minStockAlert.coerceIn(0.0, 100_000_000.0),
            "unit" to unit.trim().ifBlank { "pcs" }.take(24),
            "qrPayload" to qr.take(200),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        val code = shortCode.trim().take(12)
        val qr = "KBP-PROD:$code:$id"
        return mapOf(
            "shortCode" to code,
            "name" to name.trim().take(120),
            "sku" to sku.trim().take(60),
            "category" to category.trim().take(60),
            "purchasePrice" to purchasePrice.coerceIn(0.0, 100_000_000.0),
            "sellingPrice" to sellingPrice.coerceIn(0.0, 100_000_000.0),
            "gstPercent" to gstPercent.coerceIn(0.0, 100.0),
            "stockQty" to stockQty.coerceIn(-1_000_000.0, 100_000_000.0),
            "minStockAlert" to minStockAlert.coerceIn(0.0, 100_000_000.0),
            "unit" to unit.trim().ifBlank { "pcs" }.take(24),
            "qrPayload" to qr.take(200),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SaleLineItem(
    val productId: String = "",
    val shortCode: String = "",
    val name: String = "",
    val unit: String = "pcs",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val gstPercent: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val lineTotal: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "shortCode" to shortCode,
        "name" to name,
        "unit" to unit,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "purchasePrice" to purchasePrice,
        "gstPercent" to gstPercent,
        "taxableAmount" to taxableAmount,
        "gstAmount" to gstAmount,
        "lineTotal" to lineTotal
    )
}

@Keep
@IgnoreExtraProperties
data class Sale(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val storeGstin: String = "",
    val storeUpiId: String = "",
    val currencySymbol: String = "₹",
    val receiptHeader: String = "",
    val receiptFooter: String = "",
    val thermalWidthMm: Int = 80,
    val shortBillId: String = "",
    val invoiceNumber: String = "",
    val paymentMethod: String = PaymentMethod.CASH.name,
    val items: List<SaleLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val gstTotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalUnits: Double = 0.0,
    val digitalBillUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val estimatedProfit: Double
        get() = (subtotal - discountAmount - totalCost).coerceAtLeast(0.0)

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "storeId" to storeId,
        "storeName" to storeName.trim().ifBlank { "Store" }.take(100),
        "storeAddress" to storeAddress.trim().take(250),
        "storePhone" to storePhone.trim().take(30),
        "storeGstin" to storeGstin.trim().take(30),
        "storeUpiId" to storeUpiId.trim().take(100),
        "currencySymbol" to currencySymbol.ifBlank { "₹" }.take(8),
        "receiptHeader" to receiptHeader.take(200),
        "receiptFooter" to receiptFooter.take(200),
        "thermalWidthMm" to if (thermalWidthMm == 58) 58 else 80,
        "shortBillId" to shortBillId.take(24),
        "invoiceNumber" to invoiceNumber.take(60),
        "paymentMethod" to PaymentMethod.fromRaw(paymentMethod).name,
        "items" to items.take(100).map { it.toMap() },
        "subtotal" to subtotal.coerceAtLeast(0.0),
        "gstTotal" to gstTotal.coerceAtLeast(0.0),
        "discountAmount" to discountAmount.coerceAtLeast(0.0),
        "grandTotal" to grandTotal.coerceAtLeast(0.0),
        "totalCost" to totalCost.coerceAtLeast(0.0),
        "totalUnits" to totalUnits.coerceAtLeast(0.0),
        "digitalBillUrl" to digitalBillUrl.take(4000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class StockLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val productId: String = "",
    val productName: String = "",
    val shortCode: String = "",
    val deltaQty: Double = 0.0,
    val previousStock: Double = 0.0,
    val newStock: Double = 0.0,
    val reason: String = StockLogReason.ADJUSTMENT.name,
    val referenceInvoice: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "storeId" to storeId,
        "productId" to productId,
        "productName" to productName.trim().ifBlank { "Product" }.take(120),
        "shortCode" to shortCode.trim().ifBlank { "0000" }.take(12),
        "deltaQty" to deltaQty,
        "previousStock" to previousStock,
        "newStock" to newStock,
        "reason" to StockLogReason.fromRaw(reason).name,
        "referenceInvoice" to referenceInvoice.take(60),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ScannerRoom(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomCode: String = "",
    val storeId: String = "",
    val hostDeviceName: String = "POS Counter",
    val connectedScannerName: String = "",
    val scannerConnected: Boolean = false,
    val lastScanCode: String = "",
    val lastScanNonce: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val taxableAmount: Double get() = product.sellingPrice * quantity
    val gstAmount: Double get() = taxableAmount * (product.gstPercent / 100.0)
    val lineTotal: Double get() = taxableAmount + gstAmount
    val costTotal: Double get() = product.purchasePrice * quantity
}

data class SyncMetrics(
    val storeCount: Int = 0,
    val productCount: Int = 0,
    val saleCount: Int = 0,
    val stockLogCount: Int = 0,
    val estimatedCacheKb: Double = 0.0,
    val pendingWritesCount: Int = 0,
    val forceOffline: Boolean = false,
    val networkAvailable: Boolean = true
)
