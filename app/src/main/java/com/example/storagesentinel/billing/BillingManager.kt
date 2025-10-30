package com.example.storagesentinel.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "billing")

data class BillingState(
    val isConnected: Boolean = false,
    val isProVersion: Boolean = false,
    val purchaseState: PurchaseState = PurchaseState.IDLE,
    val availableProducts: List<ProductDetails> = emptyList(),
    val errorMessage: String? = null
)

enum class PurchaseState {
    IDLE, PURCHASING, SUCCESS, ERROR
}

class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener {
    
    companion object {
        private const val TAG = "BillingManager"
        private const val PRO_VERSION_SKU = "storage_sentinel_pro"
        private val PRO_VERSION_KEY = booleanPreferencesKey("pro_version")
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _billingState = MutableStateFlow(BillingState())
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()
    
    private lateinit var billingClient: BillingClient
    
    val isProVersion: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PRO_VERSION_KEY] ?: false }
    
    init {
        initializeBillingClient()
    }
    
    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        connectToBilling()
    }
    
    private fun connectToBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected successfully")
                    _billingState.value = _billingState.value.copy(
                        isConnected = true,
                        errorMessage = null
                    )
                    
                    // Query existing purchases
                    queryExistingPurchases()
                    
                    // Load available products
                    loadAvailableProducts()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingState.value = _billingState.value.copy(
                        isConnected = false,
                        errorMessage = "Billing setup failed: ${billingResult.debugMessage}"
                    )
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _billingState.value = _billingState.value.copy(isConnected = false)
            }
        })
    }
    
    private fun queryExistingPurchases() {
        scope.launch {
            val purchasesResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            
            if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchases = purchasesResult.purchasesList
                val hasProPurchase = purchases?.any { purchase ->
                    purchase.products.contains(PRO_VERSION_SKU) && 
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                } ?: false
                
                if (hasProPurchase) {
                    setProVersion(true)
                }
                
                // Acknowledge any unacknowledged purchases
                purchases?.forEach { purchase ->
                    if (!purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }
    
    private fun loadAvailableProducts() {
        scope.launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_VERSION_SKU)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            
            val productDetailsResult = billingClient.queryProductDetails(params)
            
            if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = productDetailsResult.productDetailsList ?: emptyList()
                _billingState.value = _billingState.value.copy(
                    availableProducts = productDetailsList
                )
                Log.d(TAG, "Loaded ${productDetailsList.size} products")
            } else {
                Log.e(TAG, "Failed to load products: ${productDetailsResult.billingResult.debugMessage}")
            }
        }
    }
    
    fun purchaseProVersion(activity: Activity) {
        val proProduct = _billingState.value.availableProducts.find { 
            it.productId == PRO_VERSION_SKU 
        }
        
        if (proProduct == null) {
            Log.w(TAG, "Pro product not available, using simulation mode for testing")
            _billingState.value = _billingState.value.copy(
                purchaseState = PurchaseState.PURCHASING
            )
            
            // Simulate purchase success after a brief delay for testing
            scope.launch {
                kotlinx.coroutines.delay(1500) // Simulate processing time
                simulateProPurchase()
            }
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(proProduct)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        _billingState.value = _billingState.value.copy(purchaseState = PurchaseState.PURCHASING)
        
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult.debugMessage}")
            _billingState.value = _billingState.value.copy(
                purchaseState = PurchaseState.ERROR,
                errorMessage = "Failed to start purchase: ${billingResult.debugMessage}"
            )
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "Purchase successful, processing ${purchases.size} purchases")
            
            purchases.forEach { purchase ->
                if (purchase.products.contains(PRO_VERSION_SKU)) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        // Verify and acknowledge the purchase
                        acknowledgePurchase(purchase)
                        setProVersion(true)
                        
                        _billingState.value = _billingState.value.copy(
                            purchaseState = PurchaseState.SUCCESS,
                            errorMessage = null
                        )
                    }
                }
            }
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            _billingState.value = _billingState.value.copy(
                purchaseState = PurchaseState.ERROR,
                errorMessage = when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.USER_CANCELED -> "Purchase canceled"
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this item"
                    else -> "Purchase failed: ${billingResult.debugMessage}"
                }
            )
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        scope.launch {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                val ackResult = billingClient.acknowledgePurchase(acknowledgeParams)
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                }
            }
        }
    }
    
    private fun setProVersion(isPro: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PRO_VERSION_KEY] = isPro
            }
            _billingState.value = _billingState.value.copy(isProVersion = isPro)
        }
    }
    
    fun clearPurchaseState() {
        _billingState.value = _billingState.value.copy(
            purchaseState = PurchaseState.IDLE,
            errorMessage = null
        )
    }
    
    fun disconnect() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
    
    // Developer testing methods
    fun simulateProPurchase() {
        Log.d(TAG, "Simulating pro purchase for testing")
        setProVersion(true)
        _billingState.value = _billingState.value.copy(purchaseState = PurchaseState.SUCCESS)
    }
    
    fun resetProVersion() {
        Log.d(TAG, "Resetting pro version for testing")
        setProVersion(false)
    }
}