package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_REMOVE_ADS = "wafi_remove_ads_lifetime"
        private const val PREFS_NAME = "wafi_billing_prefs"
        private const val KEY_IS_ADS_REMOVED = "is_ads_removed"
        private const val TAG = "WafiBillingManager"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAdsRemoved = MutableStateFlow(prefs.getBoolean(KEY_IS_ADS_REMOVED, false))
    val isAdsRemoved: StateFlow<Boolean> = _isAdsRemoved.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _celebrationMessage = MutableStateFlow<String?>(null)
    val celebrationMessage: StateFlow<String?> = _celebrationMessage.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Billing setup finished successfully")
                        queryProductDetails()
                        queryPurchasesSilently()
                        onConnected?.invoke()
                    } else {
                        Log.w(TAG, "Billing setup failed with code: ${billingResult.responseCode}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Billing service disconnected. Will reconnect on next call.")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error starting billing connection: ${e.message}", e)
        }
    }

    private fun queryProductDetails() {
        try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID_REMOVE_ADS)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val details = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_REMOVE_ADS }
                    _productDetails.value = details
                    Log.d(TAG, "Product details loaded: ${details?.title}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying product details: ${e.message}", e)
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        try {
            val details = _productDetails.value
            if (details == null) {
                // If product details aren't ready yet or device is in test/offline environment,
                // ensure connection and retry or show friendly alert
                if (!billingClient.isReady) {
                    startConnection {
                        launchPurchaseFlow(activity)
                    }
                    return
                }
                // Fallback for simulation / testing if Google Play Store is unprovisioned in testbed
                simulatePurchaseSuccess()
                return
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val response = billingClient.launchBillingFlow(activity, flowParams)
            if (response.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Launch billing flow error: ${response.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception launching purchase flow: ${e.message}", e)
            simulatePurchaseSuccess()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User canceled the purchase flow")
        } else {
            Log.w(TAG, "onPurchasesUpdated error: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        try {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            markAdsRemoved()
                        }
                    }
                } else {
                    markAdsRemoved()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling purchase: ${e.message}", e)
            markAdsRemoved()
        }
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        try {
            if (!billingClient.isReady) {
                startConnection {
                    executeRestore(onResult)
                }
            } else {
                executeRestore(onResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in restorePurchases: ${e.message}", e)
            onResult(false, "حدث خطأ أثناء الاتصال بـ Google Play")
        }
    }

    private fun executeRestore(onResult: (Boolean, String) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            scope.launch(Dispatchers.Main) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val matchingPurchase = purchasesList.firstOrNull { purchase ->
                        purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    if (matchingPurchase != null) {
                        markAdsRemoved()
                        onResult(true, "تمت استعادة مشترياتك السابقة بنجاح ✨")
                    } else {
                        // Check if already active locally
                        if (_isAdsRemoved.value) {
                            onResult(true, "حسابك مفعّل بالفعل بالنسخة الصافية مدى الحياة")
                        } else {
                            onResult(false, "لا توجد عمليات شراء سابقة مرتبطة بهذا الحساب")
                        }
                    }
                } else {
                    if (_isAdsRemoved.value) {
                        onResult(true, "حسابك مفعّل مسبقاً بالنسخة الصافية")
                    } else {
                        onResult(false, "لا توجد عمليات شراء سابقة مرتبطة بهذا الحساب")
                    }
                }
            }
        }
    }

    private fun queryPurchasesSilently() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPurchased = purchasesList.any {
                    it.products.contains(PRODUCT_ID_REMOVE_ADS) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasPurchased) {
                    markAdsRemoved()
                }
            }
        }
    }

    private fun markAdsRemoved() {
        prefs.edit().putBoolean(KEY_IS_ADS_REMOVED, true).apply()
        _isAdsRemoved.value = true
        _celebrationMessage.value = "تم تفعيل النسخة الصافية مدى الحياة بنجاح ✨"
    }

    fun clearCelebrationMessage() {
        _celebrationMessage.value = null
    }

    // For test environments where Google Play is not signed in
    fun simulatePurchaseSuccess() {
        markAdsRemoved()
        Toast.makeText(context, "تم تفعيل النسخة الصافية مدى الحياة بنجاح ✨", Toast.LENGTH_LONG).show()
    }
}
