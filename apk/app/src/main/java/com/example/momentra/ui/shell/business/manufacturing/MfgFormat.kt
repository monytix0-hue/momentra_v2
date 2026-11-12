package com.example.momentra.ui.shell.business.manufacturing

import com.example.momentra.ui.shell.business.retail.RetailFormat

/** Money / date helpers for MFG sheets — same compact/exact rules as retail. */
object MfgFormat {
    fun exact(currency: String?, raw: String?, visible: Boolean = true) =
        RetailFormat.exact(currency, raw, visible)
    fun compact(currency: String?, raw: String?, visible: Boolean = true) =
        RetailFormat.compact(currency, raw, visible)
    fun day(iso: String?) = RetailFormat.day(iso)
    fun todayLine() = RetailFormat.todayLine()
    fun month(iso: String?) = RetailFormat.month(iso)
}
