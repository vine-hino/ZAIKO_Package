package com.vine.navigation

import android.net.Uri

object ZaikoRoute {
    const val LOGIN = "login"

    const val HT_HOME = "ht/home"
    const val HT_STOCK_LIST = "ht/stock/list"
    const val HT_STOCK_HISTORY = "ht/stock/history"
    const val HT_INBOUND = "ht/inbound"
    const val HT_OUTBOUND = "ht/outbound"
    const val HT_STOCKTAKE = "ht/stocktake"
    const val HT_ADJUSTMENT_BASE = "ht/adjustment"
    const val HT_ADJUSTMENT_PRODUCT_ARG = "productCode"
    const val HT_ADJUSTMENT_LOCATION_ARG = "locationCode"
    const val HT_ADJUSTMENT = "$HT_ADJUSTMENT_BASE?${HT_ADJUSTMENT_PRODUCT_ARG}={${HT_ADJUSTMENT_PRODUCT_ARG}}&${HT_ADJUSTMENT_LOCATION_ARG}={${HT_ADJUSTMENT_LOCATION_ARG}}"

    const val PREPARING_LABEL_ARG = "label"
    const val HT_PREPARING = "ht/preparing/{$PREPARING_LABEL_ARG}"

    const val RESULT_MESSAGE_ARG = "message"
    const val HT_RESULT = "ht/result/{$RESULT_MESSAGE_ARG}"

    fun htPreparing(label: String): String {
        return "ht/preparing/${Uri.encode(label)}"
    }

    fun htResult(message: String): String {
        return "ht/result/${Uri.encode(message)}"
    }

    fun htAdjustment(
        productCode: String? = null,
        locationCode: String? = null,
    ): String {
        val product = Uri.encode(productCode.orEmpty())
        val location = Uri.encode(locationCode.orEmpty())
        return "$HT_ADJUSTMENT_BASE?$HT_ADJUSTMENT_PRODUCT_ARG=$product&$HT_ADJUSTMENT_LOCATION_ARG=$location"
    }
}
