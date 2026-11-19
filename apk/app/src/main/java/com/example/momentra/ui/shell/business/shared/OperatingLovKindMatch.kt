package com.example.momentra.ui.shell.business.shared

object OperatingLovKindMatch {

    private val productKinds = setOf("PRODUCT", "SKU", "BATCH", "PART", "MATERIAL", "RAW", "BOM")

    private val buyerKinds = setOf("BUYER", "CUSTOMER", "PRINCIPAL", "DEALER", "PARTY")

    private val supplierKinds = setOf("SUPPLIER", "VENDOR", "PRINCIPAL", "DEALER", "PARTY")

    private val partyKindsForSupplySet =
        setOf("CUSTOMER", "BUYER", "SUPPLIER", "PRINCIPAL", "DEALER", "VENDOR")

    fun masterMatchesLovKind(pickerKind: String, masterKind: String): Boolean {
        val picker = pickerKind.uppercase()
        val master = masterKind.uppercase()
        return when (picker) {
            "PRODUCT" -> master in productKinds || master.isBlank()
            "UOM" -> master in productKinds
            "BUYER" -> master in buyerKinds
            "SUPPLIER" -> master in supplierKinds
            else -> master == picker
        }
    }

    fun partyKindsForSupply(): Set<String> = partyKindsForSupplySet
}