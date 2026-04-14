package com.vine.navigation

import android.net.Uri

object ZaikoRoute {
    const val LOGIN = "login"

    const val HT_HOME = "ht/home"
    const val HT_STOCK_LIST = "ht/stock/list"
    const val HT_STOCK_HISTORY = "ht/stock/history"
    const val HT_INBOUND = "ht/inbound"
    const val HT_OUTBOUND = "ht/outbound"
    const val HT_MOVE = "ht/move"
    const val HT_STOCKTAKE = "ht/stocktake"
    const val HT_ADJUSTMENT = "ht/adjustment"

    const val RESULT_MESSAGE_ARG = "message"
    const val HT_RESULT = "ht/result/{$RESULT_MESSAGE_ARG}"

    fun htResult(message: String): String {
        return "ht/result/${Uri.encode(message)}"
    }
}