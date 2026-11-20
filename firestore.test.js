const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_owner_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validStorePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    name: "Kalam Menswear HQ",
    category: "Menswear",
    address: "42 Anna Salai, Chennai",
    phone: "+91 98400 12345",
    gstin: "33AABCK1234F1Z5",
    upiId: "kalammenswear@upi",
    currencySymbol: "₹",
    invoicePrefix: "KBP",
    thermalWidthMm: 80,
    receiptHeader: "Welcome to Kalam Menswear",
    receiptFooter: "Thank you for shopping! Goods once sold cannot be returned.",
    allowNegativeStock: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validProductPayload(uid, storeId, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    storeId,
    shortCode: "1001",
    name: "Linen Formal Shirt - Ivory",
    sku: "KLM-SH-1001",
    category: "Shirts",
    purchasePrice: 650.0,
    sellingPrice: 1299.0,
    gstPercent: 12.0,
    stockQty: 25.0,
    minStockAlert: 5.0,
    unit: "pcs",
    qrPayload: "KBP-PROD:1001:prod_1",
    createdAt: now,
    updatedAt: now,
  };
}

function validSalePayload(uid, storeId, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    storeId,
    storeName: "Kalam Menswear HQ",
    storeAddress: "42 Anna Salai, Chennai",
    storePhone: "+91 98400 12345",
    storeGstin: "33AABCK1234F1Z5",
    storeUpiId: "kalammenswear@upi",
    currencySymbol: "₹",
    receiptHeader: "Welcome",
    receiptFooter: "Thank you!",
    thermalWidthMm: 80,
    shortBillId: "KBP84X",
    invoiceNumber: "KBP-20261120-001",
    paymentMethod: "UPI",
    items: [
      {
        productId: "prod_1",
        shortCode: "1001",
        name: "Linen Formal Shirt - Ivory",
        unit: "pcs",
        quantity: 2.0,
        unitPrice: 1299.0,
        purchasePrice: 650.0,
        gstPercent: 12.0,
        taxableAmount: 2598.0,
        gstAmount: 311.76,
        lineTotal: 2909.76,
      },
    ],
    subtotal: 2598.0,
    gstTotal: 311.76,
    discountAmount: 0.0,
    grandTotal: 2909.76,
    totalCost: 1300.0,
    totalUnits: 2.0,
    digitalBillUrl: "https://kalambilling.app/b/KBP84X#d=eyJpbnYiOiJLQlAtMDAxIn0",
    createdAt: now,
    updatedAt: now,
  };
}

function validStockLogPayload(uid, storeId, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    storeId,
    productId: "prod_1",
    productName: "Linen Formal Shirt - Ivory",
    shortCode: "1001",
    deltaQty: -2.0,
    previousStock: 25.0,
    newStock: 23.0,
    reason: "SALE",
    referenceInvoice: "KBP-20261120-001",
    createdAt: now,
  };
}

function validScannerRoomPayload(uid, storeId, roomCode = "4829", now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    roomCode,
    storeId,
    hostDeviceName: "Counter Terminal 1",
    connectedScannerName: "",
    scannerConnected: false,
    lastScanCode: "",
    lastScanNonce: 0,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied all reads and writes across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("stores").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1").set(validStorePayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sales").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("stockLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("scannerRooms").get());
});

test("Owner can CRUD stores; cross-user, ghost fields, and immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const storeRef = aliceDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1");

  await assertSucceeds(storeRef.set(validStorePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("stores").get());

  // Valid partial update
  await assertSucceeds(
    storeRef.update({
      name: "Kalam Menswear Flagship",
      thermalWidthMm: 58,
      allowNegativeStock: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId 'id' leakage
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_2").set({
      ...validStorePayload(ALICE_UID),
      id: "store_2",
    })
  );

  // Reject invalid thermalWidthMm
  await assertFails(
    storeRef.update({
      thermalWidthMm: 120,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    storeRef.update({
      createdAt: new Date(Date.now() - 300),
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_3").set(
      validStorePayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Reject cross-user read & write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("Products require existing parent store, support metadata & stock updates, and block cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Orphan product without parent store must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1")
      .set(validProductPayload(ALICE_UID, "missing_store"))
  );

  // Create store first
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1")
      .set(validStorePayload(ALICE_UID))
  );

  // Create product
  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1");
  await assertSucceeds(prodRef.set(validProductPayload(ALICE_UID, "store_1")));

  // Stock deduction update
  await assertSucceeds(
    prodRef.update({
      stockQty: 23.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Query list by storeId (mirrors ProductRepository)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").where("storeId", "==", "store_1").get()
  );

  // Reject storeId mutation
  await assertFails(
    prodRef.update({
      storeId: "other_store",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Sales and StockLogs enforce schema, parent store existence, immutability, and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1")
      .set(validStorePayload(ALICE_UID))
  );

  const saleRef = aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1");
  await assertSucceeds(saleRef.set(validSalePayload(ALICE_UID, "store_1")));

  // Sales are immutable (updates rejected)
  await assertFails(
    saleRef.update({
      grandTotal: 1.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid payment method rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_2").set({
      ...validSalePayload(ALICE_UID, "store_1"),
      paymentMethod: "CRYPTO",
    })
  );

  // StockLog create & list succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stockLogs").doc("log_1")
      .set(validStockLogPayload(ALICE_UID, "store_1"))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stockLogs").where("storeId", "==", "store_1").get()
  );

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("stockLogs").doc("log_1").get());
});

test("Wireless ScannerRoom allows owner pairing and live scan streaming while enforcing schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stores").doc("store_1")
      .set(validStorePayload(ALICE_UID))
  );

  const roomRef = aliceDb.collection("users").doc(ALICE_UID).collection("scannerRooms").doc("4829");
  await assertSucceeds(roomRef.set(validScannerRoomPayload(ALICE_UID, "store_1", "4829")));

  // Handheld connects
  await assertSucceeds(
    roomRef.update({
      connectedScannerName: "Pixel 9 Handheld",
      scannerConnected: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Handheld streams scanned QR code
  await assertSucceeds(
    roomRef.update({
      lastScanCode: "KBP-PROD:1001:prod_1",
      lastScanNonce: 1,
      connectedScannerName: "Pixel 9 Handheld",
      scannerConnected: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Mismatched roomCode in doc vs path rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scannerRooms").doc("9999")
      .set(validScannerRoomPayload(ALICE_UID, "store_1", "1111"))
  );

  // Cross-user rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("scannerRooms").doc("4829").get());
});
