package com.example.momentra.ui.shell.business.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bundled India States & UTs registry. Plain JVM, no Compose: the catalog is the table every
 * GST decision reads on device, so its shape and its resolution rules are asserted as data.
 *
 * The backend `india-states-parity.test.ts` asserts these same rows match the server table and
 * the iOS copy, so this file only has to prove the Android behaviour.
 */
class IndiaStateCatalogTest {

    @Test
    fun carriesAllStatesAndUnionTerritories() {
        assertEquals(37, IndiaStateCatalog.all.size)
        assertEquals(28, IndiaStateCatalog.states.size)
        assertEquals(9, IndiaStateCatalog.unionTerritories.size)
        assertEquals(IndiaStateCatalog.all.size, IndiaStateCatalog.states.size + IndiaStateCatalog.unionTerritories.size)
    }

    @Test
    fun codesAreUniqueTwoDigitStrings() {
        val codes = IndiaStateCatalog.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        codes.forEach { code -> assertTrue("$code must be two digits", code.matches(Regex("\\d{2}"))) }
    }

    @Test
    fun keepsTheRetiredDamanAndDiuCodeOutOfTheList() {
        // 25 was Daman & Diu before the merger. It resolves forward to 26, never to itself.
        assertTrue(IndiaStateCatalog.all.none { it.code == "25" })
        assertEquals("26", IndiaStateCatalog.resolve("25")?.code)
        assertEquals("26", IndiaStateCatalog.codeOf("25"))
        assertEquals("Dadra & Nagar Haveli and Daman & Diu", IndiaStateCatalog.canonical("25"))
    }

    @Test
    fun resolvesFullNamesInAnyCasingAndWithPadding() {
        assertEquals("27", IndiaStateCatalog.codeOf("Maharashtra"))
        assertEquals("27", IndiaStateCatalog.codeOf("  maharashtra  "))
        assertEquals("27", IndiaStateCatalog.codeOf("MAHARASHTRA"))
        assertEquals("Karnataka", IndiaStateCatalog.canonical("karnataka"))
        assertEquals("Tamil Nadu", IndiaStateCatalog.canonical("tamil nadu"))
    }

    @Test
    fun resolvesTwoDigitCodes() {
        assertEquals("Maharashtra", IndiaStateCatalog.canonical("27"))
        assertEquals("Goa", IndiaStateCatalog.canonical("30"))
        assertEquals("Other Territory", IndiaStateCatalog.canonical("97"))
    }

    @Test
    fun resolvesPostalAbbreviationsAndLegacySpellings() {
        assertEquals("27", IndiaStateCatalog.codeOf("MH"))
        assertEquals("29", IndiaStateCatalog.codeOf("KA"))
        assertEquals("01", IndiaStateCatalog.codeOf("J&K"))
        assertEquals("21", IndiaStateCatalog.codeOf("Orissa"))
        assertEquals("34", IndiaStateCatalog.codeOf("Pondicherry"))
        assertEquals("05", IndiaStateCatalog.codeOf("Uttaranchal"))
        assertEquals("29", IndiaStateCatalog.codeOf("Bangalore"))
        assertEquals("36", IndiaStateCatalog.codeOf("Hyderabad"))
        assertEquals("07", IndiaStateCatalog.codeOf("New Delhi"))
    }

    @Test
    fun leavesUnresolvableTextAlone() {
        assertNull(IndiaStateCatalog.resolve(""))
        assertNull(IndiaStateCatalog.resolve(null))
        assertNull(IndiaStateCatalog.resolve("Atlantis"))
        assertEquals("", IndiaStateCatalog.codeOf("Atlantis"))
        assertEquals("", IndiaStateCatalog.canonical("Atlantis"))
        // Display falls back to the raw text so an old row never renders blank.
        assertEquals("Atlantis", IndiaStateCatalog.displayName("Atlantis"))
        assertEquals("Maharashtra", IndiaStateCatalog.displayName("mh"))
    }

    @Test
    fun jurisdictionComparesCodesNotSpellings() {
        assertEquals("INTRA", IndiaStateCatalog.jurisdiction("Maharashtra", "MH"))
        assertEquals("INTRA", IndiaStateCatalog.jurisdiction("maharashtra", "Maharashtra"))
        assertEquals("INTER", IndiaStateCatalog.jurisdiction("Maharashtra", "KA"))
        assertEquals("INTER", IndiaStateCatalog.jurisdiction("27", "29"))
        // Unresolvable on both sides but identical text: still intra-state.
        assertEquals("INTRA", IndiaStateCatalog.jurisdiction("Atlantis", "Atlantis"))
        // Unresolvable and different: inter-state, matching the old string compare.
        assertEquals("INTER", IndiaStateCatalog.jurisdiction("Atlantis", "El Dorado"))
        assertNull(IndiaStateCatalog.jurisdiction("", "Maharashtra"))
        assertNull(IndiaStateCatalog.jurisdiction("Maharashtra", null))
    }

    @Test
    fun gstinHintExplainsThePrefix() {
        val matching = IndiaStateCatalog.gstinStateHint("27AAPFU0939F1ZV", "Maharashtra")
        assertNotNull(matching)
        assertEquals("GSTIN starts with 27, which is Maharashtra.", matching)

        val mismatched = IndiaStateCatalog.gstinStateHint("29AAGCB7383J1Z4", "Maharashtra")
        assertEquals("GSTIN starts with 29 (Karnataka) but the state is Maharashtra.", mismatched)

        // Too short, non-numeric, or no state selected says nothing rather than guessing.
        assertNull(IndiaStateCatalog.gstinStateHint("2", "Maharashtra"))
        assertNull(IndiaStateCatalog.gstinStateHint("", "Maharashtra"))
        assertNull(IndiaStateCatalog.gstinStateHint("XXAAAAA0000A1Z5", "Maharashtra"))
        assertNull(IndiaStateCatalog.gstinStateHint("27AAPFU0939F1ZV", ""))
        // A prefix that names no Indian jurisdiction is not a hint we can give.
        assertNull(IndiaStateCatalog.gstinStateHint("99AAAAA0000A1Z5", "Maharashtra"))
    }

    @Test
    fun searchGroupsStatesBeforeUnionTerritories() {
        val all = IndiaStateCatalog.search(null)
        assertEquals(IndiaStateCatalog.all.size, all.size)
        val firstTerritory = all.indexOfFirst { it.isUnionTerritory }
        assertTrue("states must come first", firstTerritory >= IndiaStateCatalog.states.size)
        assertTrue(all.drop(firstTerritory).all { it.isUnionTerritory })

        assertEquals(listOf("Maharashtra"), IndiaStateCatalog.search("mahar").map { it.name })
        assertEquals(listOf("Maharashtra"), IndiaStateCatalog.search("27").map { it.name })
        assertTrue(IndiaStateCatalog.search("Atlantis").isEmpty())
    }
}
