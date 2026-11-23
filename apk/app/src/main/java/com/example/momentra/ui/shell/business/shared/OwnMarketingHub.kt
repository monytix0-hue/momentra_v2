package com.example.momentra.ui.shell.business.shared

/**
 * Own Business Campaign helpers. Full Own hub is out of scope for CLIENT_CAMPAIGN work;
 * keep [isOwn] for shell branching so Client and Distribution paths stay reachable.
 */
object OwnMarketingHub {
    const val TYPE_CODE: String = "OWN_BUSINESS_CAMPAIGN"

    fun isOwn(typeCode: String?): Boolean =
        typeCode?.trim()?.uppercase() == TYPE_CODE
}
