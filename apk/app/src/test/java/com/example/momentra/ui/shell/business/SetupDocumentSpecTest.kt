package com.example.momentra.ui.shell.business

import com.example.momentra.ui.shell.business.shared.BusinessInviteRoles
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.empty.business.SetupDocState
import com.example.momentra.ui.shell.empty.business.SetupDocumentSpec
import com.example.momentra.ui.shell.empty.business.SetupInvitee
import com.example.momentra.ui.shell.empty.business.SetupMasterCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The six-section `TRD-SETUP-v6` / `DST-SETUP-v6` document, asserted as data: section order,
 * per-vertical channel and role sets, §04 count subtitles, and the preference keys it emits.
 * No Compose, no network.
 */
class SetupDocumentSpecTest {

    @Test
    fun bothVerticalsCoverTheSameSixSectionsInOrder() {
        val trading = SetupDocumentSpec.sections(SetupDocumentSpec.TRADING)
        val distribution = SetupDocumentSpec.sections(SetupDocumentSpec.DISTRIBUTION)

        assertEquals(listOf("01", "02", "03", "04", "05", "06"), trading.map { it.number })
        assertEquals(trading.map { it.number }, distribution.map { it.number })
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "Catalogue, Warehouses & Network",
                "GST & Tax Details",
                "Review your setup",
            ),
            trading.map { it.title },
        )
        // Every section but the review carries an explanatory subtitle.
        assertTrue(trading.dropLast(1).all { it.subtitle.isNotBlank() })
    }

    @Test
    fun tradingAndDistributionChannelsDiffer() {
        val trading = SetupDocumentSpec.channels(SetupDocumentSpec.TRADING)
        val distribution = SetupDocumentSpec.channels(SetupDocumentSpec.DISTRIBUTION)

        assertEquals(listOf("Wholesale", "Retail", "B2B Direct", "Online", "Distributor", "Other"), trading)
        assertTrue(distribution.contains("General Trade"))
        assertTrue(distribution.contains("Van Sales"))
        assertFalse(trading.contains("Van Sales"))
        assertFalse(trading.contains("HoReCa"))
        assertFalse(distribution.contains("HoReCa"))
        assertTrue(trading.contains("Wholesale"))
        assertTrue(distribution.contains("Wholesale"))
    }

    @Test
    fun inviteRolesAreExactlyTheFourMintableCodes() {
        val expected = listOf("FINANCE", "APPROVER", "OPERATIONS_LEAD", "CONTRIBUTOR")
        assertEquals(expected, SetupDocumentSpec.inviteRoles(SetupDocumentSpec.TRADING).map { it.code })
        assertEquals(expected, SetupDocumentSpec.inviteRoles(SetupDocumentSpec.DISTRIBUTION).map { it.code })
        assertEquals(
            listOf("Finance", "Approver", "Operations", "Contributor"),
            SetupDocumentSpec.inviteRoles(SetupDocumentSpec.TRADING).map { it.label },
        )
        assertEquals(
            listOf("Manager", "Warehouse", "Route lead", "Salesman"),
            SetupDocumentSpec.inviteRoles(SetupDocumentSpec.DISTRIBUTION).map { it.label },
        )
        // The catalog's chip set and the sheet's own list must never drift apart.
        assertEquals(
            BusinessInviteRoles.map { it.first }.sorted(),
            SetupDocumentSpec.inviteRoles(SetupDocumentSpec.TRADING).map { it.code }.sorted(),
        )
    }

    @Test
    fun roleCodeRoundsTripsThroughTheLabel() {
        SetupDocumentSpec.inviteRoles(SetupDocumentSpec.TRADING).forEach { role ->
            assertEquals(role.code, SetupDocumentSpec.roleCode(role.label))
            assertEquals(role.label, SetupDocumentSpec.roleLabel(role.code))
        }
        assertEquals("OPERATIONS_LEAD", SetupDocumentSpec.roleCode("Not a role"))
    }

    @Test
    fun membershipTypeMatchesTheBackendSplit() {
        assertEquals("ADMIN", SetupDocumentSpec.membershipType("FINANCE"))
        assertEquals("ADMIN", SetupDocumentSpec.membershipType("APPROVER"))
        assertEquals("MEMBER", SetupDocumentSpec.membershipType("OPERATIONS_LEAD"))
        assertEquals("MEMBER", SetupDocumentSpec.membershipType("CONTRIBUTOR"))
    }

    @Test
    fun everyEmittedPreferenceKeyIsAcceptedByTheCatalog() {
        val emitted = SetupDocumentSpec.emittedPreferenceKeys()
        assertTrue("The document must emit something", emitted.isNotEmpty())
        val unknown = emitted - OperatingMomentCatalog.preferenceKeys
        assertTrue("Keys the catalog would strip: $unknown", unknown.isEmpty())
    }

    @Test
    fun preferencesUseTheWireEnumsNotTheOperatorWords() {
        val prefs = SetupDocumentSpec.preferences(
            subtypeCode = SetupDocumentSpec.TRADING,
            identityName = "  Northstar Trading  ",
            partyName = "  Northstar Pvt Ltd ",
            currency = "INR",
            startDate = "2026-10-12",
            channels = listOf("Wholesale", "Online"),
            monthlyBudget = "500000",
            monthlyTarget = "1200000",
            targetMetric = "REVENUE",
            participants = listOf(mapOf("displayName" to "Kiran Rao", "role" to "OWNER_ADMIN")),
            gstin = " 29abcde4321t1z5 ",
            gstState = "Maharashtra",
            gstRegistration = "REGULAR",
            gstTracking = "YES",
            salePriceIncludesGst = "NO",
        )

        assertEquals("Northstar Trading", prefs["identityName"])
        assertEquals("Northstar Pvt Ltd", prefs["partyName"])
        // A GSTIN is stored uppercased; a lower-case one would fail matching downstream.
        assertEquals("29ABCDE4321T1Z5", prefs["gstin"])
        assertEquals("REVENUE", prefs["targetMetric"])
        // Purchases inherit the sales decision unless told otherwise.
        assertEquals(prefs["salePriceIncludesGst"], prefs["purchasePriceIncludesGst"])
        assertEquals(listOf("Wholesale", "Online"), prefs["channels"])
        @Suppress("UNCHECKED_CAST")
        val participants = prefs["participants"] as List<Map<String, String>>
        assertEquals("Kiran Rao", participants.single()["displayName"])
    }

    @Test
    fun gstStateRoundTripsAsACanonicalNamePlusItsCode() {
        fun prefsFor(state: String): Map<String, Any?> = SetupDocumentSpec.preferences(
            subtypeCode = SetupDocumentSpec.TRADING,
            identityName = "Northstar Trading",
            partyName = "Northstar Pvt Ltd",
            currency = "INR",
            startDate = "2026-10-12",
            channels = listOf("Wholesale"),
            monthlyBudget = "500000",
            monthlyTarget = "1200000",
            targetMetric = "REVENUE",
            participants = listOf(mapOf("displayName" to "Kiran Rao", "role" to "OWNER_ADMIN")),
            gstState = state,
            gstRegistration = "REGULAR",
            gstTracking = "YES",
            salePriceIncludesGst = "NO",
        )

        // A picked name, a typed abbreviation, and a bare code all land on the same pair.
        for (spelling in listOf("Maharashtra", "maharashtra", "MH", "27", "  Maharashtra  ")) {
            val prefs = prefsFor(spelling)
            assertEquals("canonical name for \"$spelling\"", "Maharashtra", prefs["gstState"])
            assertEquals("code for \"$spelling\"", "27", prefs["gstStateCode"])
        }

        // The document emits the code, so the backend never has to re-derive it.
        assertTrue(SetupDocumentSpec.emittedPreferenceKeys().contains("gstStateCode"))

        // An unresolvable value is kept as typed so the operator can see and fix it; the code is
        // left empty rather than guessed.
        val unknown = prefsFor("Atlantis")
        assertEquals("Atlantis", unknown["gstState"])
        assertEquals("", unknown["gstStateCode"])
    }

    @Test
    fun metricAndGstLabelsRoundTripThroughTheirCodes() {
        assertEquals("REVENUE", SetupDocumentSpec.metricCode("Revenue"))
        assertEquals("ORDERS", SetupDocumentSpec.metricCode("Orders"))
        assertEquals("CUSTOM_COUNT", SetupDocumentSpec.metricCode("Count"))
        assertEquals("Revenue", SetupDocumentSpec.metricLabel("REVENUE"))
        assertEquals("Count", SetupDocumentSpec.metricLabel("CUSTOM_COUNT"))
        // An unknown code prints as a word rather than a raw enum.
        assertEquals("Revenue", SetupDocumentSpec.metricLabel(""))

        assertEquals("YES", SetupDocumentSpec.gstPricingCode("Prices include GST"))
        assertEquals("NO", SetupDocumentSpec.gstPricingCode("GST is added separately"))
        assertEquals("REGULAR", SetupDocumentSpec.gstRegistrationCode("Regular"))
        assertEquals("NOT_SURE", SetupDocumentSpec.gstRegistrationCode("Not sure"))
        assertEquals("YES", SetupDocumentSpec.gstTrackingCode("Yes"))
        assertEquals("NOT_NOW", SetupDocumentSpec.gstTrackingCode("Not now"))
    }

    @Test
    fun currencyLabelsCarryTheirIsoCode() {
        assertEquals("INR", SetupDocumentSpec.currencyCode("INR · Indian Rupee (₹)"))
        assertEquals("USD", SetupDocumentSpec.currencyCode("USD · US Dollar"))
        // A blank choice falls back to the frame's default rather than an empty currency.
        assertEquals("INR", SetupDocumentSpec.currencyCode(""))
    }

    @Test
    fun anEmptyCatalogueNeverPrintsZeroSkus() {
        val empty = SetupDocumentSpec.catalogueSubtitle(SetupMasterCounts())
        assertEquals(SetupDocumentSpec.CATALOGUE_INVITE, empty)
        assertFalse(empty.contains("0 SKU"))

        val filled = SetupDocumentSpec.catalogueSubtitle(SetupMasterCounts(skus = 4, priceLists = 2))
        assertTrue(filled.startsWith("4 SKUs"))
        assertTrue(filled.contains("2 price lists"))
        assertTrue(filled.contains("HSN + GST"))

        // One SKU reads singular.
        assertEquals("1 SKU · HSN + GST on each", SetupDocumentSpec.catalogueSubtitle(SetupMasterCounts(skus = 1)))
    }

    @Test
    fun warehousesPrintNamesBeforeCounts() {
        assertEquals(
            SetupDocumentSpec.WAREHOUSE_INVITE,
            SetupDocumentSpec.warehouseSubtitle(emptyList(), SetupMasterCounts()),
        )
        assertEquals(
            "Pune DC · Nashik DC · 3 routes",
            SetupDocumentSpec.warehouseSubtitle(
                listOf("Pune DC", "Nashik DC"),
                SetupMasterCounts(warehouses = 2, routes = 3),
            ),
        )
        assertEquals(
            "1 warehouse",
            SetupDocumentSpec.warehouseSubtitle(emptyList(), SetupMasterCounts(warehouses = 1)),
        )
    }

    @Test
    fun networkRowNamesTheVertical() {
        assertEquals(
            SetupDocumentSpec.NETWORK_INVITE,
            SetupDocumentSpec.networkSubtitle(SetupDocumentSpec.TRADING, SetupMasterCounts()),
        )
        val trading = SetupDocumentSpec.networkSubtitle(
            SetupDocumentSpec.TRADING,
            SetupMasterCounts(principals = 2, dealers = 5),
        )
        assertTrue(trading.contains("2 principals"))
        assertTrue(trading.contains("5 dealers"))
        val distribution = SetupDocumentSpec.networkSubtitle(
            SetupDocumentSpec.DISTRIBUTION,
            SetupMasterCounts(principals = 1, dealers = 3),
        )
        assertTrue(distribution.contains("1 supplier"))
        assertTrue(distribution.contains("3 buyers"))

        assertEquals("Business contacts", SetupDocumentSpec.networkLabel(SetupDocumentSpec.TRADING))
        assertEquals("Suppliers & customers", SetupDocumentSpec.networkLabel(SetupDocumentSpec.DISTRIBUTION))
    }

    @Test
    fun theFootnoteNamesTheRightCarrier() {
        assertTrue(SetupDocumentSpec.networkFootnote(SetupDocumentSpec.TRADING).contains("principals carry GSTIN"))
        assertTrue(SetupDocumentSpec.networkFootnote(SetupDocumentSpec.DISTRIBUTION).contains("buyers carry GSTIN"))
        listOf(SetupDocumentSpec.TRADING, SetupDocumentSpec.DISTRIBUTION).forEach { code ->
            assertTrue(SetupDocumentSpec.networkFootnote(code).contains("never on SO / PO save"))
        }
    }

    @Test
    fun initialsMatchTheAvatarRule() {
        assertEquals("KR", SetupDocumentSpec.initials("Kiran Rao"))
        assertEquals("K", SetupDocumentSpec.initials("Kiran"))
        assertEquals("SM", SetupDocumentSpec.initials("  Suresh   Mehta  "))
        assertEquals("", SetupDocumentSpec.initials("   "))
    }

    @Test
    fun monthAndDateLabelsReadLikeTheFrame() {
        assertEquals("October 2026", SetupDocumentSpec.monthLabel("2026-10"))
        assertEquals("12 October 2026", SetupDocumentSpec.setupDateLabel("2026-10-12"))
        // Unparseable input is passed through rather than crashing a review panel.
        assertEquals("soon", SetupDocumentSpec.setupDateLabel("soon"))
        assertEquals("This month", SetupDocumentSpec.monthLabel(""))
    }

    @Test
    fun momentLineReadsAsKindCurrencyAndWindow() {
        assertEquals(
            "Trading · INR · From 12 October 2026 · Still open",
            SetupDocumentSpec.momentLine(SetupDocumentSpec.TRADING, "INR", "2026-10-12", ""),
        )
        assertEquals(
            "Distribution · INR · From 1 November 2026 · Until 30 November 2026",
            SetupDocumentSpec.momentLine(
                SetupDocumentSpec.DISTRIBUTION,
                "INR",
                "2026-11-01",
                "2026-11-30",
            ),
        )
    }

    @Test
    fun channelHintCountsSelection() {
        assertEquals(
            "3 selected. These are the places you sell or serve. They are not a different kind of business.",
            SetupDocumentSpec.channelHint(3),
        )
    }

    @Test
    fun defaultRoleNoteNamesTheDefaultMintableRole() {
        assertEquals(
            "Default Operations · sales, stock, purchases and orders. Role applies when they join.",
            SetupDocumentSpec.defaultRoleNote(SetupDocumentSpec.TRADING),
        )
        assertEquals(
            SetupDocumentSpec.defaultRoleNote(SetupDocumentSpec.TRADING),
            SetupDocumentSpec.defaultRoleNote(SetupDocumentSpec.DISTRIBUTION),
        )
    }

    @Test
    fun inviteAccessNoteDescribesTheSelectedRole() {
        assertEquals("Access: money, payments and finance boards.", SetupDocumentSpec.inviteAccessNote("FINANCE"))
        assertEquals(
            "Access: approves money and stock adjustments.",
            SetupDocumentSpec.inviteAccessNote("APPROVER"),
        )
        assertEquals(
            "Access: sales, stock, purchases and orders.",
            SetupDocumentSpec.inviteAccessNote("OPERATIONS_LEAD"),
        )
        assertEquals(
            "Access: records activity; money waits for approval.",
            SetupDocumentSpec.inviteAccessNote("CONTRIBUTOR"),
        )
    }

    @Test
    fun teamMetaVerbFollowsStatus() {
        assertEquals("Invite sent · Operations", SetupDocumentSpec.teamMeta("PENDING", "Operations"))
        assertEquals("Added · Finance", SetupDocumentSpec.teamMeta("ACTIVE", "Finance"))
    }

    @Test
    fun inviteesRideTheParticipantsPreferenceAfterTheOwner() {
        val invitees = listOf(
            SetupInvitee(id = "a", name = "Kiran Rao", roleCode = "OPERATIONS_LEAD"),
            SetupInvitee(id = "b", name = "  Asha Menon  ", roleCode = "FINANCE", phone = "+91 98765 43210"),
            SetupInvitee(id = "c", name = "   ", roleCode = "APPROVER"),
        )
        val rows = SetupDocumentSpec.participants("Owner Name", invitees)

        assertEquals(3, rows.size)
        assertEquals("Owner Name", rows[0]["displayName"])
        assertEquals("OWNER_ADMIN", rows[0]["role"])
        assertEquals("Kiran Rao", rows[1]["displayName"])
        assertEquals("OPERATIONS_LEAD", rows[1]["role"])
        assertEquals("Asha Menon", rows[2]["displayName"])
        assertEquals("FINANCE", rows[2]["role"])
        // A blank name is not a person and must never reach the wire.
        assertTrue(rows.none { it["displayName"].isNullOrBlank() })
    }

    @Test
    fun savedParticipantsRoundTripBackIntoInviteeRows() {
        val prefs = mapOf(
            "participants" to listOf(
                mapOf("displayName" to "Owner Name", "role" to "OWNER_ADMIN"),
                mapOf("displayName" to "Kiran Rao", "role" to "OPERATIONS_LEAD"),
                mapOf("displayName" to "Asha Menon", "role" to "FINANCE"),
            ),
        )
        val invitees = SetupDocumentSpec.invitees(prefs)

        assertEquals(listOf("Kiran Rao", "Asha Menon"), invitees.map { it.name })
        assertEquals(listOf("OPERATIONS_LEAD", "FINANCE"), invitees.map { it.roleCode })
        assertEquals(listOf("Operations", "Finance"), invitees.map { it.roleLabel })
    }

    @Test
    fun anInviteeRoleOutsideTheChipSetRoundTripsUnchanged() {
        // A saved draft can carry a role the chip set no longer offers; the row must survive it
        // rather than silently degrading to the default operations role.
        val prefs = mapOf(
            "participants" to listOf(
                mapOf("displayName" to "Owner Name", "role" to "OWNER_ADMIN"),
                mapOf("displayName" to "Kiran Rao", "role" to "OBSERVER"),
            ),
        )
        val invitees = SetupDocumentSpec.invitees(prefs)

        assertEquals(listOf("OBSERVER"), invitees.map { it.roleCode })
        assertEquals("OBSERVER", SetupDocumentSpec.participants("Owner Name", invitees)[1]["role"])
    }

    @Test
    fun isDistributionOnlyMatchesTheDealershipSubtype() {
        assertTrue(SetupDocumentSpec.isDistribution(SetupDocumentSpec.DISTRIBUTION))
        assertTrue(SetupDocumentSpec.isDistribution("distribution_dealership"))
        assertFalse(SetupDocumentSpec.isDistribution(SetupDocumentSpec.TRADING))
        assertFalse(SetupDocumentSpec.isDistribution(""))
    }

    @Test
    fun unknownMetricLabelFallsBackToAWord() {
        // The frame never shows a raw enum, so an unexpected code must print as a word.
        assertEquals("Count", SetupDocumentSpec.metricLabel("SOMETHING_ELSE"))
        assertEquals("Count", SetupDocumentSpec.metricLabel("custom_count"))
        assertEquals("Revenue", SetupDocumentSpec.metricLabel(""))
        assertEquals("", SetupDocumentSpec.gstTrackingCode("Maybe"))
    }

    // MARK: Draft prefill

    @Test
    fun aDraftRoundTripsThroughTheDocumentState() {
        val saved = SetupDocState(SetupDocumentSpec.DISTRIBUTION, initialName = "Metro Distribution")
        saved.description = "Dealership for the western belt"
        saved.location = "Pune"
        saved.gstin = "27AABCM1234C1ZX"
        saved.gstState = "Maharashtra"
        saved.gstTracking = "Not now"
        saved.gstRegistration = "Composition"
        saved.gstPricing = "Prices include GST"
        saved.channels = setOf("General Trade")
        saved.budget = "2,50,000"
        saved.target = "9,00,000"
        saved.month = "2026-11"
        saved.startDate = "2026-11-01"
        // The selected chip is transient; the durable record is the per-invitee role.
        saved.invitees = listOf(SetupInvitee(id = "a", name = "Asha Menon", roleCode = "FINANCE"))

        val reopened = SetupDocState(SetupDocumentSpec.TRADING)
        reopened.applyPrefill(saved.preferences("Kiran Rao"))

        assertEquals(SetupDocumentSpec.DISTRIBUTION, reopened.subtype)
        assertEquals("Metro Distribution", reopened.businessName)
        assertEquals("Dealership for the western belt", reopened.description)
        assertEquals("Pune", reopened.location)
        assertEquals("27AABCM1234C1ZX", reopened.gstin)
        assertEquals("Maharashtra", reopened.gstState)
        assertEquals("Not now", reopened.gstTracking)
        assertEquals("Composition", reopened.gstRegistration)
        assertEquals("Prices include GST", reopened.gstPricing)
        assertEquals(setOf("General Trade"), reopened.channels)
        // Grouped money comes back grouped, not as raw digits in the field.
        assertEquals("2,50,000", reopened.budget)
        assertEquals("9,00,000", reopened.target)
        assertEquals("2026-11-01", reopened.startDate)
        assertEquals("INR · Indian Rupee (₹)", reopened.currency)
        assertEquals(SetupDocumentSpec.DEFAULT_INVITE_ROLE, reopened.inviteRole)
        assertEquals(listOf("Asha Menon"), reopened.invitees.map { it.name })
        assertEquals(listOf("Finance"), reopened.invitees.map { it.roleLabel })
        // The reopened document is ready to start without retyping anything.
        assertTrue(reopened.canStart)
    }

    @Test
    fun prefillDropsChannelsTheSubtypeNoLongerOffers() {
        val reopened = SetupDocState(SetupDocumentSpec.TRADING)
        reopened.applyPrefill(
            mapOf(
                "subtype" to SetupDocumentSpec.DISTRIBUTION,
                "channels" to listOf("General Trade", "Van Sales"),
            ),
        )

        assertEquals(SetupDocumentSpec.DISTRIBUTION, reopened.subtype)
        assertEquals(setOf("General Trade", "Van Sales"), reopened.channels)

        // Switching back to trading must not keep a dealership-only chip selected.
        reopened.switchSubtype(SetupDocumentSpec.TRADING)
        assertTrue(reopened.channels.isEmpty())
    }

    @Test
    fun prefillIgnoresAnUnknownSubtypeAndBlankFields() {
        val reopened = SetupDocState(SetupDocumentSpec.TRADING, initialName = "Northstar Trading")
        reopened.applyPrefill(
            mapOf(
                "subtype" to "SOMETHING_ELSE",
                "identityName" to "   ",
                "channels" to emptyList<String>(),
            ),
        )

        assertEquals(SetupDocumentSpec.TRADING, reopened.subtype)
        assertEquals("Northstar Trading", reopened.businessName)
        assertTrue(reopened.channels.isEmpty())
    }

    // MARK: Status

    @Test
    fun onlyADraftMayBeDiscardedOrPromoted() {
        assertTrue(SetupDocumentSpec.isDiscardable("DRAFT"))
        assertFalse(SetupDocumentSpec.isDiscardable("ACTIVE"))
        assertTrue(SetupDocumentSpec.isPromotable("draft"))
        assertFalse(SetupDocumentSpec.isPromotable("ACTIVE"))
    }

    @Test
    fun theActivateCopyFollowsTheStatus() {
        // A live moment must not offer to `Start` again — it is saved in place.
        assertEquals("Start this business", SetupDocumentSpec.activateLabel("DRAFT"))
        assertEquals("Save changes", SetupDocumentSpec.activateLabel("ACTIVE"))
        assertEquals("Starting…", SetupDocumentSpec.activatingLabel("DRAFT"))
        assertEquals("Saving…", SetupDocumentSpec.activatingLabel("ACTIVE"))
        assertEquals("Setup in progress", SetupDocumentSpec.statusPillLabel("DRAFT"))
        assertEquals("Business is live", SetupDocumentSpec.statusPillLabel("ACTIVE"))
    }
}
