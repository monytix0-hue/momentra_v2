package com.example.momentra.ui.shell.business.shared

/**
 * The canonical India States & Union Territories registry, mirroring the backend table in
 * `backend/typescript/src/modules/business/operating-gst-states.ts`.
 *
 * GST decides between CGST+SGST and IGST by comparing the business's state with the place of
 * supply. Comparing typed names is what let "MH" and "Maharashtra" read as two different states
 * and charge IGST on an intra-state sale, so every state field stores the canonical name and
 * every jurisdiction decision compares the two-digit code.
 *
 * The list is bundled rather than fetched so the picker opens instantly offline. The backend
 * parity test asserts this file, the iOS copy, and the server table carry the same 37 pairs.
 */
data class IndiaState(
    /** Two-digit GST state code, zero-padded ("01".."38", plus "97"). */
    val code: String,
    /** The name written onto the preference and shown in the picker. */
    val name: String,
    val isUnionTerritory: Boolean,
)

object IndiaStateCatalog {

    /**
     * 28 states and 8 union territories in GST code order, plus "97 Other Territory".
     *
     * Code 25 (Daman & Diu) is deliberately absent: the territory merged into 26, and
     * [canonical] maps the old code forward rather than reviving a retired jurisdiction.
     */
    val all: List<IndiaState> = listOf(
        IndiaState("01", "Jammu & Kashmir", true),
        IndiaState("02", "Himachal Pradesh", false),
        IndiaState("03", "Punjab", false),
        IndiaState("04", "Chandigarh", true),
        IndiaState("05", "Uttarakhand", false),
        IndiaState("06", "Haryana", false),
        IndiaState("07", "Delhi", true),
        IndiaState("08", "Rajasthan", false),
        IndiaState("09", "Uttar Pradesh", false),
        IndiaState("10", "Bihar", false),
        IndiaState("11", "Sikkim", false),
        IndiaState("12", "Arunachal Pradesh", false),
        IndiaState("13", "Nagaland", false),
        IndiaState("14", "Manipur", false),
        IndiaState("15", "Mizoram", false),
        IndiaState("16", "Tripura", false),
        IndiaState("17", "Meghalaya", false),
        IndiaState("18", "Assam", false),
        IndiaState("19", "West Bengal", false),
        IndiaState("20", "Jharkhand", false),
        IndiaState("21", "Odisha", false),
        IndiaState("22", "Chhattisgarh", false),
        IndiaState("23", "Madhya Pradesh", false),
        IndiaState("24", "Gujarat", false),
        IndiaState("26", "Dadra & Nagar Haveli and Daman & Diu", true),
        IndiaState("27", "Maharashtra", false),
        IndiaState("29", "Karnataka", false),
        IndiaState("30", "Goa", false),
        IndiaState("31", "Lakshadweep", true),
        IndiaState("32", "Kerala", false),
        IndiaState("33", "Tamil Nadu", false),
        IndiaState("34", "Puducherry", true),
        IndiaState("35", "Andaman & Nicobar Islands", true),
        IndiaState("36", "Telangana", false),
        IndiaState("37", "Andhra Pradesh", false),
        IndiaState("38", "Ladakh", true),
        IndiaState("97", "Other Territory", true),
    )

    val states: List<IndiaState> = all.filterNot { it.isUnionTerritory }
    val unionTerritories: List<IndiaState> = all.filter { it.isUnionTerritory }

    private val byCode: Map<String, IndiaState> = all.associateBy { it.code }
    private val byName: Map<String, IndiaState> = all.associateBy { normalize(it.name) }

    /**
     * Spellings that appear in older saved rows and in free text typed before the picker existed.
     * Keys are normalized (lowercased, punctuation collapsed); values are GST state codes.
     */
    private val aliases: Map<String, String> = mapOf(
        "ap" to "37",
        "ar" to "12",
        "as" to "18",
        "br" to "10",
        "cg" to "22",
        "ch" to "04",
        "dd" to "26",
        "dl" to "07",
        "dn" to "26",
        "ga" to "30",
        "gj" to "24",
        "hp" to "02",
        "hr" to "06",
        "jh" to "20",
        "jk" to "01",
        "j k" to "01",
        "ka" to "29",
        "kl" to "32",
        "la" to "38",
        "ld" to "31",
        "mh" to "27",
        "ml" to "17",
        "mn" to "14",
        "mp" to "23",
        "mz" to "15",
        "nl" to "13",
        "or" to "21",
        "pb" to "03",
        "py" to "34",
        "rj" to "08",
        "sk" to "11",
        "tg" to "36",
        "tn" to "33",
        "tr" to "16",
        "ts" to "36",
        "uk" to "05",
        "up" to "09",
        "wb" to "19",
        "bangalore" to "29",
        "bengaluru" to "29",
        "bombay" to "27",
        "calcutta" to "19",
        "chennai" to "33",
        "cochin" to "32",
        "ernakulam" to "32",
        "gurgaon" to "06",
        "gurugram" to "06",
        "hyderabad" to "36",
        "kolkata" to "19",
        "mumbai" to "27",
        "mysore" to "29",
        "mysuru" to "29",
        "ncr" to "07",
        "noida" to "09",
        "pune" to "27",
        "trivandrum" to "32",
        "visakhapatnam" to "37",
        "vizag" to "37",
        "daman" to "26",
        "diu" to "26",
        "daman and diu" to "26",
        "dadra and nagar haveli" to "26",
        "nagar haveli" to "26",
        "andaman and nicobar islands" to "35",
        "andaman" to "35",
        "nicobar" to "35",
        "andaman nicobar" to "35",
        "jammu and kashmir" to "01",
        "jammu" to "01",
        "kashmir" to "01",
        "orissa" to "21",
        "pondicherry" to "34",
        "uttaranchal" to "05",
        "new delhi" to "07",
        "nct" to "07",
        "nct of delhi" to "07",
        // The retired Daman & Diu code maps forward to the merged territory.
        "25" to "26",
    )

    private fun normalize(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    /**
     * Resolve any spelling of an Indian state to its canonical record, or `null`.
     *
     * Accepts the full name in any casing, the two-digit code, a two-letter postal abbreviation,
     * and the legacy / city spellings in [aliases].
     */
    fun resolve(raw: String?): IndiaState? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        if (text.matches(Regex("\\d{2}"))) {
            return byCode[text] ?: aliases[text]?.let { byCode[it] }
        }

        val key = normalize(text)
        if (key.isEmpty()) return null

        byName[key]?.let { return it }
        aliases[key]?.let { code -> byCode[code]?.let { return it } }

        if (key.matches(Regex("\\d{1,2}"))) {
            return byCode[key.padStart(2, '0')]
        }
        return null
    }

    /** The canonical full name for any spelling, or `""` when it cannot be resolved. */
    fun canonical(raw: String?): String = resolve(raw)?.name.orEmpty()

    /** The two-digit GST code for any spelling, or `""` when it cannot be resolved. */
    fun codeOf(raw: String?): String = resolve(raw)?.code.orEmpty()

    /** What to show for a stored value: the canonical name when known, otherwise the raw text. */
    fun displayName(raw: String?): String {
        val text = raw?.trim().orEmpty()
        return resolve(text)?.name ?: text
    }

    /**
     * `INTRA` when both sides resolve to the same code, `INTER` when both resolve and differ,
     * and `null` when neither can be resolved. Falls back to a case-insensitive name compare so
     * a legacy row written identically on both sides still reads as intra-state.
     */
    fun jurisdiction(businessState: String?, supplyState: String?): String? {
        val business = resolve(businessState)
        val supply = resolve(supplyState)
        if (business != null && supply != null) {
            return if (business.code == supply.code) "INTRA" else "INTER"
        }
        val left = businessState?.trim().orEmpty().lowercase()
        val right = supplyState?.trim().orEmpty().lowercase()
        if (left.isEmpty() || right.isEmpty()) return null
        return if (left == right) "INTRA" else "INTER"
    }

    /**
     * The hint under a GSTIN field: whether its first two characters name the selected state.
     *
     * Returns `null` when the GSTIN is too short to judge, or when nothing useful can be said.
     */
    fun gstinStateHint(gstin: String?, state: String?): String? {
        val text = gstin?.trim().orEmpty().uppercase()
        if (text.length < 2 || !text.take(2).matches(Regex("\\d{2}"))) return null
        val declared = byCode[text.take(2)] ?: return null
        val selected = resolve(state) ?: return null
        return if (declared.code == selected.code) {
            "GSTIN starts with ${declared.code}, which is ${declared.name}."
        } else {
            "GSTIN starts with ${declared.code} (${declared.name}) but the state is ${selected.name}."
        }
    }

    /** States and union territories matching a picker query, states first. */
    fun search(query: String?): List<IndiaState> {
        val q = query?.trim().orEmpty().lowercase()
        if (q.isEmpty()) return states + unionTerritories
        return (states + unionTerritories).filter { state ->
            state.name.lowercase().contains(q) || state.code == q
        }
    }
}
