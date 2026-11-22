package com.example.momentra.ui.shell.business.setup

/**
 * Maps Figma Business Setup form values → OperatingPreferences (+ master seeds).
 * Lockstep with iOS `OperatingSetupPayloadBuilder` and backend `applyOperatingSetupAliases`.
 */
object OperatingSetupPayloadBuilder {
    data class MasterSeed(val kind: String, val name: String)

    data class Result(
        val preferences: Map<String, Any>,
        val masterSeeds: List<MasterSeed>,
        val validationError: String?,
        val fieldErrors: Map<String, String> = emptyMap(),
    )

    private val dualWriteBudgetTarget = setOf(
        "TRADING_WHOLESALE",
        "DISTRIBUTION_DEALERSHIP",
        "STANDARD_PRODUCTION",
        "MADE_TO_ORDER",
    )

    private val retailChannelsRequired = setOf(
        "RETAIL_STORE",
        "RESTAURANT_FOOD",
        "CONSUMER_SERVICE",
    )

    private val retailSubtypes = setOf(
        "RETAIL_STORE",
        "RESTAURANT_FOOD",
        "CONSUMER_SERVICE",
    )

    private val marketingSubtypes = setOf(
        "CLIENT_CAMPAIGN",
        "OWN_BUSINESS_CAMPAIGN",
    )

    private val tradingOrMfg = setOf(
        "TRADING_WHOLESALE",
        "DISTRIBUTION_DEALERSHIP",
        "STANDARD_PRODUCTION",
        "MADE_TO_ORDER",
    )

    private val roleAliases = mapOf(
        "owner" to "OWNER_ADMIN",
        "owners" to "OWNER_ADMIN",
        "admin" to "OWNER_ADMIN",
        "owner / admin" to "OWNER_ADMIN",
        "owner/admin" to "OWNER_ADMIN",
        "manager" to "OPERATIONS_LEAD",
        "plant manager" to "OPERATIONS_LEAD",
        "business manager" to "OPERATIONS_LEAD",
        "operations lead" to "OPERATIONS_LEAD",
        "operations" to "OPERATIONS_LEAD",
        "campaign lead" to "OPERATIONS_LEAD",
        "commercial" to "OPERATIONS_LEAD",
        "marketer" to "OPERATIONS_LEAD",
        "executive" to "OPERATIONS_LEAD",
        "delivery" to "OPERATIONS_LEAD",
        "warehouse" to "OPERATIONS_LEAD",
        "sales exec" to "OPERATIONS_LEAD",
        "sales executive" to "OPERATIONS_LEAD",
        "cashier" to "TEAM_MEMBER",
        "floor" to "TEAM_MEMBER",
        "stock" to "TEAM_MEMBER",
        "kitchen" to "TEAM_MEMBER",
        "waiter" to "TEAM_MEMBER",
        "service" to "TEAM_MEMBER",
        "specialist" to "TEAM_MEMBER",
        "stylist" to "TEAM_MEMBER",
        "therapist" to "TEAM_MEMBER",
        "front desk" to "TEAM_MEMBER",
        "salesman" to "TEAM_MEMBER",
        "sales rep" to "TEAM_MEMBER",
        "sales" to "TEAM_MEMBER",
        "supervisor" to "OPERATIONS_LEAD",
        "operator" to "TEAM_MEMBER",
        "estimator" to "CONTRIBUTOR",
        "shop floor" to "TEAM_MEMBER",
        "accounts" to "FINANCE",
        "finance" to "FINANCE",
        "approver" to "APPROVER",
        "contributor" to "CONTRIBUTOR",
        "team member" to "TEAM_MEMBER",
        "viewer" to "OBSERVER",
        "viewer / auditor" to "OBSERVER",
        "observer" to "OBSERVER",
        "audit" to "OBSERVER",
        "auditor" to "OBSERVER",
    )

    fun build(
        preferences: Map<String, Any>,
        momentTypeCode: String,
        title: String,
        activating: Boolean = true,
    ): Result {
        val prefs = stringKeyed(preferences).toMutableMap()
        val subtype = momentTypeCode.trim().uppercase()
        prefs["subtype"] = subtype

        val identity = firstNonEmpty(
            prefs["identityName"],
            prefs["title"],
            prefs["campaignName"],
            prefs["businessName"],
            prefs["storeName"],
            prefs["outletName"],
            prefs["restaurantName"],
            prefs["serviceName"],
            prefs["plantName"],
            prefs["shopName"],
            title,
        )
        if (prefs["outletName"] == null) {
            val restaurantName = stringValue(prefs["restaurantName"])
            if (restaurantName.isNotEmpty()) prefs["outletName"] = restaurantName
        }
        if (identity.isNotEmpty()) prefs["identityName"] = identity

        val party = if (subtype in retailSubtypes) {
            firstNonEmpty(prefs["partyName"], prefs["companyName"], identity)
        } else if (subtype == "CLIENT_CAMPAIGN") {
            firstNonEmpty(
                prefs["partyName"],
                prefs["clientName"],
                prefs["brandName"],
                prefs["principalBrand"],
                prefs["companyName"],
                prefs["customerName"],
            )
        } else {
            firstNonEmpty(
                prefs["partyName"],
                prefs["clientName"],
                prefs["brandName"],
                prefs["principalBrand"],
                prefs["companyName"],
                prefs["customerName"],
                prefs["format"],
                prefs["serviceStyle"],
                prefs["bookingStyle"],
                identity,
            )
        }
        if (party.isNotEmpty()) prefs["partyName"] = party

        if (stringValue(prefs["location"]).isEmpty()) {
            val location = firstNonEmpty(prefs["location"], prefs["locality"])
            if (location.isNotEmpty()) prefs["location"] = location
        }

        if (prefs["startDate"] == null && prefs["planMonth"] != null) {
            val month = stringValue(prefs["planMonth"])
            when {
                Regex("""^\d{4}-\d{2}$""").matches(month) -> prefs["startDate"] = "$month-01"
                else -> coerceSetupDate(month)?.let { prefs["startDate"] = it }
            }
        }

        mapSalePriceIncludesGst(prefs["salePriceIncludesGst"])?.let {
            prefs["salePriceIncludesGst"] = it
        }

        if (positiveAmount(prefs["monthlyBudget"]) == null) {
            val budget = firstNonEmpty(
                prefs["monthlyBudget"],
                prefs["monthlyExpense"],
                prefs["marketingBudget"],
                prefs["targetSales"],
                prefs["coverTarget"],
                prefs["avgTicket"],
            )
            if (positiveAmount(budget) != null) prefs["monthlyBudget"] = budget
        }
        if (positiveAmount(prefs["monthlyTarget"]) == null) {
            val target = firstNonEmpty(
                prefs["monthlyTarget"],
                prefs["monthlySalesTarget"],
                prefs["monthlyChargeTarget"],
                prefs["targetSales"],
                prefs["leadGoal"],
                prefs["conversionGoal"],
                prefs["monthlyOutput"],
                prefs["monthlyJobs"],
                prefs["coverTarget"],
            )
            if (positiveAmount(target) != null) prefs["monthlyTarget"] = target
        }
        if (subtype in dualWriteBudgetTarget) {
            if (positiveAmount(prefs["monthlyBudget"]) == null) {
                positiveAmount(prefs["monthlyTarget"])?.let { prefs["monthlyBudget"] = it }
            }
            if (positiveAmount(prefs["monthlyTarget"]) == null) {
                positiveAmount(prefs["monthlyBudget"])?.let { prefs["monthlyTarget"] = it }
            }
        }
        positiveAmount(prefs["monthlyBudget"])?.let { prefs["monthlyBudget"] = it }
        positiveAmount(prefs["monthlyTarget"])?.let { prefs["monthlyTarget"] = it }

        val tracking = mapGstTracking(prefs["gstTracking"] ?: prefs["gstRegistered"])
        if (tracking.isNotEmpty()) prefs["gstTracking"] = tracking
        val registration = mapGstRegistration(prefs["gstRegistration"] ?: prefs["gstType"])
        if (registration.isNotEmpty()) prefs["gstRegistration"] = registration

        var channels = splitList(prefs["channels"])
        if (channels.isEmpty()) {
            channels = splitList(
                firstNonEmpty(
                    prefs["posStyle"],
                    prefs["bookingStyle"],
                    prefs["orderChannels"],
                    prefs["territory"],
                    prefs["routeStyle"],
                ),
            )
        }
        if (channels.isNotEmpty()) prefs["channels"] = channels

        (coerceSetupDate(prefs["startDate"]) ?: coerceSetupDate(prefs["startedOn"]))?.let {
            prefs["startDate"] = it
        }
        coerceSetupDate(prefs["endDate"])?.let { prefs["endDate"] = it }

        val currency = stringValue(prefs["currency"]).uppercase()
        prefs["currency"] = currency.ifEmpty { "INR" }

        if (prefs["gstin"] != null) {
            prefs["gstin"] = stringValue(prefs["gstin"]).uppercase()
        }

        if (prefs["participants"] == null) {
            val roles = splitList(prefs["roles"])
            val members = splitList(prefs["teamMembers"])
            val count = maxOf(roles.size, members.size)
            if (count > 0) {
                val rows = mutableListOf<Map<String, String>>()
                for (i in 0 until count) {
                    val role = mapRole(roles.getOrNull(i) ?: roles.firstOrNull() ?: "TEAM_MEMBER")
                    val name = members.getOrNull(i) ?: roles.getOrNull(i) ?: "Member ${i + 1}"
                    rows += mapOf("displayName" to name, "role" to role)
                }
                prefs["participants"] = rows
            }
        }

        val counterparties = mutableListOf<Map<String, String>>()
        @Suppress("UNCHECKED_CAST")
        val existing = prefs["counterparties"] as? List<*>
        existing?.forEach { row ->
            val map = row as? Map<*, *> ?: return@forEach
            val name = stringValue(map["name"])
            if (name.isEmpty()) return@forEach
            val kind = stringValue(map["kind"]).ifEmpty { "OTHER" }
            counterparties += mapOf("name" to name, "kind" to kind)
        }
        val networkSeeds = listOf(
            "supplierName" to "SUPPLIER",
            "customerName" to "CUSTOMER",
            "buyerName" to "BUYER",
            "vendorName" to "SUPPLIER",
            "principalBrand" to "PRINCIPAL",
            "clientName" to "CLIENT",
        )
        for ((key, kind) in networkSeeds) {
            val name = stringValue(prefs[key])
            if (name.isEmpty()) continue
            if (counterparties.none { it["name"].equals(name, ignoreCase = true) }) {
                counterparties += mapOf("name" to name, "kind" to kind)
            }
        }
        if (counterparties.isNotEmpty()) prefs["counterparties"] = counterparties

        val masters = mutableListOf<MasterSeed>()
        for (row in counterparties) {
            val name = row["name"] ?: continue
            val kind = row["kind"] ?: continue
            masters += MasterSeed(kind = kind, name = name)
        }
        for (channel in channels) {
            masters += MasterSeed(kind = "CHANNEL", name = channel)
        }

        if (!activating) {
            return Result(preferences = prefs, masterSeeds = masters, validationError = null)
        }
        val fieldErrors = validationFieldErrors(subtype, prefs, channels)
        val error = fieldErrors.values.firstOrNull()
        return Result(
            preferences = prefs,
            masterSeeds = masters,
            validationError = error,
            fieldErrors = fieldErrors,
        )
    }

    fun positiveAmount(value: Any?): String? {
        when (value) {
            is Number -> if (value.toDouble() > 0) return "%.4f".format(value.toDouble())
        }
        var raw = stringValue(value)
        if (raw.isEmpty()) return null
        raw = raw.replace(Regex("""^(₹|rs\.?|inr)\s*""", RegexOption.IGNORE_CASE), "")
        raw = raw.replace("₹", "").replace(",", "").replace(" ", "")
        if (!Regex("""^\d+(\.\d{1,4})?$""").matches(raw)) return null
        val n = raw.toDoubleOrNull() ?: return null
        if (n <= 0) return null
        return "%.4f".format(n)
    }

    fun coerceSetupDate(value: Any?): String? {
        val raw = stringValue(value)
        if (raw.isEmpty()) return null
        if (Regex("""^\d{4}-\d{2}-\d{2}""").containsMatchIn(raw)) return raw.take(10)
        val months = mapOf(
            "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4, "may" to 5, "jun" to 6, "june" to 6, "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8, "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10, "nov" to 11, "november" to 11, "dec" to 12, "december" to 12,
        )
        val parts = raw.split(Regex("""\s+"""))
        if (parts.size == 3) {
            val day = parts[0].toIntOrNull()
            val month = months[parts[1].lowercase()]
            val year = parts[2].toIntOrNull()
            if (day != null && month != null && year != null && day in 1..31) {
                return "%04d-%02d-%02d".format(year, month, day)
            }
        }
        return null
    }

    fun splitList(value: Any?): List<String> {
        when (value) {
            is List<*> -> return value.map { stringValue(it) }.filter { it.isNotEmpty() }
        }
        val raw = stringValue(value)
        if (raw.isEmpty()) return emptyList()
        return raw.split('|', ',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun mapGstTracking(value: Any?): String {
        val raw = stringValue(value).lowercase()
        if (raw.isEmpty()) return ""
        if (raw in setOf("yes", "y", "true", "1", "registered")) return "YES"
        if (raw in setOf("no", "n", "false", "0", "not now", "not_now", "later")) return "NOT_NOW"
        val upper = raw.uppercase()
        return if (upper in setOf("YES", "NOT_NOW")) upper else ""
    }

    fun mapGstRegistration(value: Any?): String {
        val raw = stringValue(value).lowercase().replace(" ", "_")
        if (raw.isEmpty()) return ""
        return when (raw) {
            "regular" -> "REGULAR"
            "composition", "composition_scheme" -> "COMPOSITION"
            "unregistered" -> "UNREGISTERED"
            "not_sure", "notsure", "unsure" -> "NOT_SURE"
            else -> {
                val upper = raw.uppercase()
                if (upper in setOf("REGULAR", "COMPOSITION", "UNREGISTERED", "NOT_SURE")) upper else ""
            }
        }
    }

    fun mapSalePriceIncludesGst(value: Any?): String? {
        val raw = stringValue(value).lowercase()
        if (raw.isEmpty()) return null
        if (raw in setOf("yes", "y", "true", "prices include gst", "include", "included")) return "YES"
        if (raw in setOf("no", "n", "false", "gst is added separately", "added separately", "exclude", "excluded")) {
            return "NO"
        }
        val upper = raw.uppercase()
        return if (upper in setOf("YES", "NO")) upper else null
    }

    /** Soft Channels status copy — informational, never blocks activate for retail. */
    fun planStatusMessage(preferences: Map<String, Any>): Pair<Boolean, String> {
        val budgetOk = positiveAmount(preferences["monthlyBudget"]) != null
        val targetOk = positiveAmount(
            preferences["monthlyTarget"] ?: preferences["monthlyChargeTarget"] ?: preferences["targetSales"],
        ) != null
        return when {
            budgetOk && targetOk -> true to "Budget and sales target look good"
            targetOk && !budgetOk -> false to "Add a monthly budget so the plan stays complete."
            budgetOk && !targetOk -> false to "Add a monthly sales target so the plan stays complete."
            else -> false to "Budget and sales target are optional until you start."
        }
    }

    fun mapRole(value: Any?): String {
        val raw = stringValue(value)
        if (raw.isEmpty()) return "TEAM_MEMBER"
        val code = raw.uppercase().replace(" ", "_")
        val known = setOf(
            "OWNER_ADMIN", "OPERATIONS_LEAD", "FINANCE", "APPROVER",
            "CONTRIBUTOR", "TEAM_MEMBER", "OBSERVER",
        )
        if (code in known) return code
        return roleAliases[raw.lowercase()] ?: "TEAM_MEMBER"
    }

    private fun validationFieldErrors(
        subtype: String,
        prefs: Map<String, Any>,
        channels: List<String>,
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (stringValue(prefs["identityName"]).isEmpty()) {
            val titleKey = when (subtype) {
                "CONSUMER_SERVICE" -> "serviceName"
                "RETAIL_STORE" -> "storeName"
                "RESTAURANT_FOOD" -> "outletName"
                "STANDARD_PRODUCTION" -> "plantName"
                "MADE_TO_ORDER" -> "shopName"
                "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN" -> "campaignName"
                else -> "businessName"
            }
            errors[titleKey] = "Moment name is required."
        }
        if (stringValue(prefs["partyName"]).isEmpty()) {
            errors["companyName"] = "Business party name is required."
        }
        if (subtype in marketingSubtypes) {
            if (positiveAmount(prefs["monthlyBudget"]) == null) {
                errors["monthlyBudget"] = "Monthly budget must be a positive amount."
            }
            val start = stringValue(prefs["startDate"])
            if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(start) && coerceSetupDate(prefs["startDate"]) == null) {
                errors["startDate"] = "Campaign start date is required."
            }
        } else if (subtype in tradingOrMfg) {
            if (positiveAmount(prefs["monthlyBudget"]) == null) {
                errors["monthlyBudget"] = "Monthly budget must be a positive amount."
            }
            if (positiveAmount(prefs["monthlyTarget"]) == null) {
                errors["monthlyTarget"] = "Monthly target must be a positive amount."
            }
        }
        if (subtype in retailChannelsRequired && channels.isEmpty()) {
            errors["channels"] = "Select at least one channel."
        }
        val tracking = stringValue(prefs["gstTracking"])
        val gstin = stringValue(prefs["gstin"])
        if (tracking == "YES" && gstin.isNotEmpty() && !isValidGstin(gstin)) {
            errors["gstin"] = "Enter a valid 15-digit GSTIN."
        }
        return errors
    }

    fun isValidGstin(value: String): Boolean {
        val raw = value.trim().uppercase()
        return Regex("""^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$""").matches(raw)
    }

    private fun stringKeyed(preferences: Map<String, Any>): Map<String, Any> {
        val out = mutableMapOf<String, Any>()
        for ((key, value) in preferences) {
            if (value is String) {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty()) out[key] = trimmed
            } else {
                out[key] = value
            }
        }
        return out
    }

    private fun firstNonEmpty(vararg values: Any?): String {
        for (value in values) {
            val text = stringValue(value)
            if (text.isNotEmpty()) return text
        }
        return ""
    }

    private fun stringValue(value: Any?): String = when (value) {
        null -> ""
        is String -> value.trim()
        is Number -> value.toString()
        else -> value.toString().trim()
    }
}
