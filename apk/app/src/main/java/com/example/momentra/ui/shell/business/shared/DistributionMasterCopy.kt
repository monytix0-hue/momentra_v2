package com.example.momentra.ui.shell.business.shared

/**
 * Dist-specific masters / picker / Switch Moment copy (DST-MASTER / DST-PICKER / DST-SHEET).
 * Mirrored on iOS `DistributionMasterCopy.swift`. Trading keeps generic OperatingMasterTab titles.
 */
object DistributionMasterCopy {

    fun tabTitle(tab: OperatingMasterTab): String = when (tab) {
        OperatingMasterTab.Principals -> "Principals & dealers"
        OperatingMasterTab.Catalogue -> "Catalogue & SKU"
        OperatingMasterTab.Locations -> "Warehouses & routes"
        OperatingMasterTab.PriceLists -> "Price lists"
        OperatingMasterTab.Invite -> "Invite & QR"
    }

    fun tabSubtitle(tab: OperatingMasterTab): String = when (tab) {
        OperatingMasterTab.Principals ->
            "Principal & dealer master · GSTIN drives ITC on GRN and place of supply on invoice"
        OperatingMasterTab.Catalogue ->
            "SKU master · principal, HSN, GST, MRP + case size before it appears in SO / PO"
        OperatingMasterTab.Locations ->
            "Warehouse & route master · stock moves between them are transfers, not sales"
        OperatingMasterTab.PriceLists ->
            "Effective-dated dealer price · scheme override never rewrites a posted invoice"
        OperatingMasterTab.Invite ->
            "Invite grants a role and its access. Joining ≠ approval ≠ payment."
    }

    fun tabNote(tab: OperatingMasterTab): String = when (tab) {
        OperatingMasterTab.Principals ->
            "Principal/dealer is a master write. GSTIN drives ITC on GRN and output GST on invoice — PO/SO save never posts GST."
        OperatingMasterTab.Catalogue ->
            "Create SKU is a master write. SO and PO only pick saved SKUs; GST posts on invoice / GRN, not on save."
        OperatingMasterTab.Locations ->
            "A route belongs to a warehouse. Moving stock between them is a transfer, not a sale."
        OperatingMasterTab.PriceLists ->
            "A price list is effective-dated. Two lists may overlap in name but never in a day."
        OperatingMasterTab.Invite ->
            "Invite grants a role and its access. Joining ≠ approval ≠ payment."
    }

    fun newCta(tab: OperatingMasterTab): String = when (tab) {
        OperatingMasterTab.Principals -> "+ New principal / dealer"
        OperatingMasterTab.Catalogue -> "+ New SKU"
        OperatingMasterTab.Locations -> "+ New warehouse or route"
        OperatingMasterTab.PriceLists -> "+ New price list"
        OperatingMasterTab.Invite -> "Done"
    }

    fun formTitle(tab: OperatingMasterTab, editingName: String?): String = when {
        editingName != null -> "Edit $editingName"
        tab == OperatingMasterTab.Principals -> "New / Edit Principal or Dealer"
        tab == OperatingMasterTab.Catalogue -> "Create SKU"
        tab == OperatingMasterTab.Locations -> "Add / Edit Warehouse or Route"
        tab == OperatingMasterTab.PriceLists -> "Create price list"
        else -> "New ${tab.singular}"
    }

    fun saveCta(tab: OperatingMasterTab, editing: Boolean): String = when (tab) {
        OperatingMasterTab.Principals -> "Save party"
        OperatingMasterTab.Catalogue -> "Save SKU"
        OperatingMasterTab.Locations -> if (editing) "Save warehouse / route" else "Save warehouse / route"
        OperatingMasterTab.PriceLists -> "Save price list"
        OperatingMasterTab.Invite -> "Done"
    }

    fun saveFootnote(tab: OperatingMasterTab): String = when (tab) {
        OperatingMasterTab.Principals -> "Party saved · available in PO / SO"
        OperatingMasterTab.Catalogue -> "Master saved · available in browser on next open"
        OperatingMasterTab.Locations -> "Location saved · available on stock / transfer"
        OperatingMasterTab.PriceLists -> "Price list saved · effective from valid-from"
        OperatingMasterTab.Invite -> "3 invites pending · roles apply on join"
    }

    const val SWITCH_TITLE = "Switch Moment"
    const val SWITCH_SUBTITLE = "Your Business moments · tap to open"
    const val SWITCH_NEW = "+ New Business moment"

    const val SKU_BROWSER_TITLE = "Catalogue"
    const val SKU_BROWSER_HINT = "Tap Add · qty per line"
    const val SKU_BROWSER_SEARCH = "Search SKU, name or barcode"
    const val SKU_BROWSER_CREATE = "+ New SKU"
    const val SKU_BROWSER_CREATE_HINT = "Name · HSN · GST · MRP"

    val skuCategories = listOf("Staples", "Beverages", "Personal care", "Home care", "Snacks")

    const val PICKER_DAY_SUBTITLE = "tap a day"
    const val PICKER_MONTH_TITLE = "Pick a month"
    const val PICKER_DATETIME_TITLE = "Pick a day and time"
}
