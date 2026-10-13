package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages Google Play In-App Subscriptions for Fintel Pro.
 * Follows official Google Play Billing 7.x guidelines:
 * - Queries subscription products (fintel_pro_monthly, fintel_pro_yearly)
 * - Launches native Google Play payment sheet
 * - Acknowledges purchases within 3 days (immediately upon confirmation)
 * - Restores purchases across devices
 */
class BillingManager(
    private val context: Context,
    private val onSubscriptionActivated: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "FintelBilling"
        const val SUB_MONTHLY = "fintel_pro_monthly"
        const val SUB_YEARLY = "fintel_pro_yearly"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady = _isBillingReady.asStateFlow()

    private val _monthlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val monthlyProductDetails = _monthlyProductDetails.asStateFlow()

    private val _yearlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val yearlyProductDetails = _yearlyProductDetails.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun startConnection() {
        if (billingClient.isReady) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client successfully connected to Google Play.")
                    _isBillingReady.value = true
                    querySubscriptions()
                    queryActivePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _isBillingReady.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Will reconnect on next request.")
                _isBillingReady.value = false
            }
        })
    }

    /**
     * Queries Google Play Console for active subscription product details.
     */
    fun querySubscriptions() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in productDetailsList) {
                    when (details.productId) {
                        SUB_MONTHLY -> _monthlyProductDetails.value = details
                        SUB_YEARLY -> _yearlyProductDetails.value = details
                    }
                    Log.d(TAG, "Found subscription: ${details.productId} - ${details.title}")
                }
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launches the native Google Play purchase flow sheet.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String = SUB_MONTHLY): Boolean {
        if (!billingClient.isReady) {
            startConnection()
            return false
        }

        val details = if (productId == SUB_YEARLY) _yearlyProductDetails.value else _monthlyProductDetails.value
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken

        if (details == null || offerToken == null) {
            Log.w(TAG, "No product details or offer token available for $productId. Ensure product is created in Play Console.")
            return false
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Checks if user already has an active subscription in Google Play.
     */
    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActive = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasActive) {
                    Log.d(TAG, "Found active subscription from Google Play.")
                    onSubscriptionActivated(true)
                }

                // Acknowledge any unacknowledged purchases
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    /**
     * Callback from Google Play when purchase flow completes or updates.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        acknowledgePurchase(purchase)
                        onSubscriptionActivated(true)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled Google Play purchase flow.")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
                onSubscriptionActivated(true)
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
