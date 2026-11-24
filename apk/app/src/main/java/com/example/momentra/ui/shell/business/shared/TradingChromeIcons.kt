package com.example.momentra.ui.shell.business.shared

import com.example.momentra.R

/**
 * Locked TRD-v12 pulse + Quick Add hub icon catalog (Figma chrome parity).
 * Keep in sync with iOS `TradingChromeIcons`.
 */
object TradingChromeIcons {
    /** Pulse quick-action row — Sale / Purchase / Collection / Payment. */
    val pulseActionCodes: List<String> = listOf("SALE", "PURCHASE", "COLLECTION", "PAYMENT")

    fun pulseActionDrawable(code: String): Int = when (code.uppercase()) {
        "SALE" -> R.drawable.ic_rest_tile_sale
        "PURCHASE" -> R.drawable.ic_rest_tile_purchase
        "COLLECTION" -> R.drawable.ic_rest_tile_collection
        "PAYMENT" -> R.drawable.ic_rest_tile_payment
        else -> error("Unknown Trading pulse action icon code: $code")
    }

    /** Hub tile id → `qa_*` drawable. Masters / captures share this map. */
    fun hubDrawable(tileId: String): Int = when (tileId.uppercase()) {
        "SALE", "REPRINT" -> R.drawable.qa_receipt
        "PURCHASE" -> R.drawable.qa_wallet
        "COLLECTION", "RETURN" -> R.drawable.qa_arrow_left_right
        "PAYMENT" -> R.drawable.qa_credit_card
        "SPEND", "BUDGET" -> R.drawable.qa_budget
        "STOCK", "CATALOGUE" -> R.drawable.qa_package
        "ORDER" -> R.drawable.qa_clipboard
        "SHIPMENT", "DELIVERY", "TRANSFER" -> R.drawable.qa_shipment
        "PRINCIPALS" -> R.drawable.qa_users
        "LOCATIONS" -> R.drawable.qa_truck
        "INVITE" -> R.drawable.qa_qr
        "UPDATE" -> R.drawable.qa_alert
        "APPROVAL", "HOLD" -> R.drawable.qa_shield
        "PRODUCTION" -> R.drawable.qa_factory
        "SEARCH" -> R.drawable.qa_search
        else -> error("Unknown Trading hub icon tile id: $tileId")
    }

    val tradingHubCoreIds: List<String> = listOf(
        "SALE", "PURCHASE", "COLLECTION", "PAYMENT", "SPEND",
        "STOCK", "RETURN", "BUDGET", "ORDER", "SHIPMENT",
    )

    val tradingHubSupportIds: List<String> = listOf(
        "PRINCIPALS", "CATALOGUE", "LOCATIONS", "INVITE", "UPDATE", "APPROVAL",
    )
}
