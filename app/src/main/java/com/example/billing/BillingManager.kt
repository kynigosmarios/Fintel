package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    private val context: Context,
    private val onSubscriptionActivated: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "FintelBilling"
        const val SUB_MONTHLY = "fintel_pro_monthly"
        const val SUB_YEARLY = "fintel_pro_yearly"
        const val INAPP_LIFETIME = "fintel_pro_lifetime"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _monthlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val monthlyProductDetails: StateFlow<ProductDetails?> = _monthlyProductDetails.asStateFlow()

    private val _yearlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val yearlyProductDetails: StateFlow<ProductDetails?> = _yearlyProductDetails.asStateFlow()

    private val _lifetimeProductDetails = MutableStateFlow<ProductDetails?>(null)
    val lifetimeProductDetails: StateFlow<ProductDetails?> = _lifetimeProductDetails.asStateFlow()

    fun startConnection() {
        if (billingClient.isReady) {
            _isBillingReady.value = true
            queryAllProducts()
            queryActivePurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingReady.value = true
                    queryAllProducts()
                    queryActivePurchases()
                } else {
                    _isBillingReady.value = false
                    Log.i(TAG, "Billing service not available on current environment (code: ${billingResult.responseCode}): ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingReady.value = false
                Log.d(TAG, "Billing service disconnected")
            }
        })
    }

    fun queryAllProducts() {
        if (!billingClient.isReady) return

        // 1. Query Subscription products (Monthly & Yearly)
        val subsList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val subsParams = QueryProductDetailsParams.newBuilder().setProductList(subsList).build()
        billingClient.queryProductDetailsAsync(subsParams) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = queryResult.productDetailsList ?: emptyList()
                for (details in list) {
                    when (details.productId) {
                        SUB_MONTHLY -> _monthlyProductDetails.value = details
                        SUB_YEARLY -> _yearlyProductDetails.value = details
                    }
                    Log.d(TAG, "Found subscription: ${details.productId} - ${details.title}")
                }
            } else {
                Log.d(TAG, "Query subscriptions status: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }

        // 2. Query In-App One-Time product (Lifetime)
        val inappList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(INAPP_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val inappParams = QueryProductDetailsParams.newBuilder().setProductList(inappList).build()
        billingClient.queryProductDetailsAsync(inappParams) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = queryResult.productDetailsList ?: emptyList()
                for (details in list) {
                    if (details.productId == INAPP_LIFETIME) {
                        _lifetimeProductDetails.value = details
                        Log.d(TAG, "Found in-app product: ${details.productId} - ${details.title}")
                    }
                }
            } else {
                Log.d(TAG, "Query in-app status: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String = SUB_YEARLY): Boolean {
        if (!billingClient.isReady) {
            startConnection()
            return false
        }

        if (productId == INAPP_LIFETIME) {
            val details = _lifetimeProductDetails.value
            if (details == null) {
                Log.w(TAG, "No product details available for $productId.")
                return false
            }
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()
            val result = billingClient.launchBillingFlow(activity, billingFlowParams)
            return result.responseCode == BillingClient.BillingResponseCode.OK
        } else {
            val details = if (productId == SUB_MONTHLY) _monthlyProductDetails.value else _yearlyProductDetails.value
            val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (details == null || offerToken == null) {
                Log.w(TAG, "No product details or offer token available for $productId.")
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
    }

    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        // 1. Query Subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchasesList(purchases)
            }
        }

        // 2. Query In-App purchases
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(inappParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchasesList(purchases)
            }
        }
    }

    private fun handlePurchasesList(purchases: List<Purchase>?) {
        val hasActive = purchases?.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } == true
        if (hasActive) {
            Log.d(TAG, "Found active purchase from Google Play.")
            onSubscriptionActivated(true)
        }
        purchases?.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                handlePurchasesList(purchases)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled Google Play purchase flow.")
            }
            else -> {
                Log.w(TAG, "Purchase update: ${billingResult.responseCode} - ${billingResult.debugMessage}")
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
                Log.w(TAG, "Purchase acknowledge note: ${result.debugMessage}")
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
