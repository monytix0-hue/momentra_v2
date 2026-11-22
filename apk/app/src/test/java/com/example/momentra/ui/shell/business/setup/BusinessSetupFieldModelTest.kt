package com.example.momentra.ui.shell.business.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupFieldModelTest {

    @Test
    fun fieldKindsRoundTripInMinimalSpec() {
        val fields = listOf(
            BusinessSetupFieldSpec("t", "T", "p", BusinessSetupFieldSpec.Kind.Text, helper = "hint"),
            BusinessSetupFieldSpec("c", "C", kind = BusinessSetupFieldSpec.Kind.Choice(listOf("A", "B"))),
            BusinessSetupFieldSpec("m", "M", kind = BusinessSetupFieldSpec.Kind.MultiChoice(listOf("X", "Y"))),
            BusinessSetupFieldSpec("g", "G", kind = BusinessSetupFieldSpec.Kind.Toggle),
            BusinessSetupFieldSpec("d", "D", "date", BusinessSetupFieldSpec.Kind.Date),
            BusinessSetupFieldSpec("r", "R", "Add", BusinessSetupFieldSpec.Kind.RowAction("Add")),
            BusinessSetupFieldSpec("mem", "Mem", kind = BusinessSetupFieldSpec.Kind.MemberList(listOf("You"))),
            BusinessSetupFieldSpec(
                "mod",
                "Mod",
                kind = BusinessSetupFieldSpec.Kind.ModuleList(
                    listOf(BusinessSetupModuleRow("A", "sub-a"), BusinessSetupModuleRow("B")),
                ),
            ),
            BusinessSetupFieldSpec("rev", "Rev", kind = BusinessSetupFieldSpec.Kind.ReviewSummary(listOf("t", "c"))),
            BusinessSetupFieldSpec("ph", "Identity", kind = BusinessSetupFieldSpec.Kind.PanelHeader("Identity")),
        )
        val section = BusinessSetupSectionSpec("s", "S", "", fields)
        val spec = BusinessSetupDocumentSpec(
            typeCode = "TEST",
            familyTitle = "T",
            eyebrow = "e",
            heroTitle = "h",
            heroSubtitle = "sub",
            activateLabel = "Go",
            defaultTitleKey = "t",
            sections = listOf(section),
        )
        val prefs = spec.defaultPreferences()
        assertEquals("", prefs["t"])
        assertEquals("A", prefs["c"])
        assertEquals("X", prefs["m"])
        assertEquals("Yes", prefs["g"])
        assertEquals("", prefs["d"])
        assertEquals("", prefs["r"])
        assertEquals("You", prefs["mem"])
        assertEquals("", prefs["mod"])
        assertEquals("", prefs["rev"])
        assertEquals("", prefs["ph"])
        assertEquals("hint", fields[0].helper)
        assertEquals(10, fields.size)
        val moduleKind = fields[7].kind as BusinessSetupFieldSpec.Kind.ModuleList
        assertEquals("sub-a", moduleKind.modules[0].subtitle)
        assertTrue(BusinessSetupFieldChrome.hasError("bad"))
        assertTrue(!BusinessSetupFieldChrome.hasError(null))
        assertTrue(!BusinessSetupFieldChrome.hasError(""))
    }

    @Test
    fun consumerServiceMatchesFigma71162() {
        val spec = BusinessSetupDocuments.spec("CONSUMER_SERVICE")
        assertEquals("BrightCare Setup", spec.heroTitle)
        assertEquals("Start this business", spec.activateLabel)
        assertEquals("serviceName", spec.defaultTitleKey)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(keys.containsAll(listOf(
            "serviceName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyChargeTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        )))
        assertTrue(!keys.contains("modules"))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Studio", "Home", "Online", "Corporate", "Events"), channels.options)
        val roles = spec.sections
            .flatMap { it.fields }
            .first { it.key == "roles" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Owner", "Manager", "Stylist", "Front desk", "Therapist"), roles.options)
    }

    @Test
    fun restaurantMatchesFigma90612() {
        val spec = BusinessSetupDocuments.spec("RESTAURANT_FOOD")
        assertEquals("Spice Route Setup", spec.heroTitle)
        assertEquals("Start this restaurant", spec.activateLabel)
        assertEquals("outletName", spec.defaultTitleKey)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "Kitchen, Menu & Network",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(keys.containsAll(listOf(
            "outletName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        )))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Dine-in", "Direct", "Zomato", "Takeaway", "Swiggy"), channels.options)
        val roles = spec.sections
            .flatMap { it.fields }
            .first { it.key == "roles" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Owner", "Manager", "Cashier", "Kitchen", "Waiter"), roles.options)
        val modules = spec.sections
            .flatMap { it.fields }
            .first { it.key == "modules" }
            .kind as BusinessSetupFieldSpec.Kind.ModuleList
        assertEquals(4, modules.modules.size)
        assertEquals("Menu", modules.modules[0].title)
        assertEquals("Stations", modules.modules[1].title)
        assertEquals("Vendors", modules.modules[2].title)
    }

    @Test
    fun emptyValuesMapDoesNotCrashDefaultPreferences() {
        BusinessSetupFamily.entries.forEach { family ->
            family.chips.forEach { chip ->
                val prefs = BusinessSetupDocuments.spec(chip.typeCode).defaultPreferences()
                assertTrue(prefs.isNotEmpty())
            }
        }
    }

    @Test
    fun clientCampaignMatchesFigma33148FiveSections() {
        val spec = BusinessSetupDocuments.spec("CLIENT_CAMPAIGN")
        assertEquals("Brightline Setup", spec.heroTitle)
        assertEquals(
            "Bring your business, people, and monthly plan together before you start.",
            spec.heroSubtitle,
        )
        assertEquals("Start this business", spec.activateLabel)
        assertEquals("campaignName", spec.defaultTitleKey)
        assertEquals("Client campaign", spec.eyebrow)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(
            keys.containsAll(
                listOf(
                    "campaignName", "companyName", "clientName", "currency", "startDate", "endDate",
                    "gstin", "campaignPurpose", "roles", "channels", "clientFee",
                    "monthlyBudget", "monthlyTarget", "targetMetric",
                    "gstTracking", "gstRegistration",
                ),
            ),
        )
        assertTrue(!keys.contains("modules"))
        assertTrue(!keys.contains("leadGoal"))
        assertTrue(!keys.contains("billingFrequency"))
        assertTrue(!keys.contains("addPeople"))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(
            listOf("Paid social", "Search", "Print", "Influencer", "Events"),
            channels.options,
        )
        val roles = spec.sections
            .flatMap { it.fields }
            .first { it.key == "roles" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(
            listOf("Owner", "Admin", "Campaign Lead", "Finance", "Approver"),
            roles.options,
        )
    }

    @Test
    fun ownMarketingMatchesMarketingPair() {
        val spec = BusinessSetupDocuments.spec("OWN_BUSINESS_CAMPAIGN")
        assertEquals("Start this business", spec.activateLabel)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
    }

    @Test
    fun ownMarketingMatchesFigma38325() {
        val spec = BusinessSetupDocuments.spec("OWN_BUSINESS_CAMPAIGN")
        assertEquals("Harbor & Loom Setup", spec.heroTitle)
        assertTrue(spec.eyebrow.contains("Own"))
        assertEquals("Start this business", spec.activateLabel)
        assertEquals("campaignName", spec.defaultTitleKey)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(
            keys.containsAll(
                listOf(
                    "brandName", "endDate", "campaignPurpose", "offerType", "promotedProducts",
                    "location", "description", "channels", "marketingBudget", "planMonth",
                    "monthlyBudget", "conversionGoal", "targetMetric", "gstTracking",
                    "gstRegistration", "salePriceIncludesGst",
                ),
            ),
        )
        assertTrue(!keys.contains("modules"))
        assertTrue(!keys.contains("addPeople"))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(
            listOf("Paid social", "Search", "Print", "Influencer", "Events", "Store window"),
            channels.options,
        )
        val roles = spec.sections
            .flatMap { it.fields }
            .first { it.key == "roles" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(
            listOf("Owner", "Admin", "Campaign Lead", "Finance", "Approver"),
            roles.options,
        )
    }

    @Test
    fun tradingActivateLabelIsStartTheBusiness() {
        val trading = BusinessSetupDocuments.spec("TRADING_WHOLESALE")
        val distribution = BusinessSetupDocuments.spec("DISTRIBUTION_DEALERSHIP")
        assertEquals("Start this business", trading.activateLabel)
        assertEquals("Start this business", distribution.activateLabel)
        assertTrue(!trading.sectionTitles.contains("Catalogue, Warehouses & Network"))
        assertEquals(
            com.example.momentra.ui.theme.generated.BusinessDomain.TRADING,
            BusinessSetupTheme.forTypeCode("TRADING_WHOLESALE").domain,
        )
    }

    @Test
    fun distributionMatchesFigma8637FiveSections() {
        val spec = BusinessSetupDocuments.spec("DISTRIBUTION_DEALERSHIP")
        assertEquals("Metro Distribution Setup", spec.heroTitle)
        assertEquals(
            "Bring your business, people, and monthly plan together before you start.",
            spec.heroSubtitle,
        )
        assertEquals("Start this business", spec.activateLabel)
        assertEquals("businessName", spec.defaultTitleKey)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(
            keys.containsAll(
                listOf(
                    "businessName", "companyName", "gstin", "currency", "startDate", "endDate",
                    "description", "location", "roles", "channels", "planMonth",
                    "monthlyBudget", "monthlyTarget", "targetMetric",
                    "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
                ),
            ),
        )
        assertTrue(!keys.contains("principalBrand"))
        assertTrue(!keys.contains("territory"))
        assertTrue(!keys.contains("routeStyle"))
        assertTrue(!keys.contains("dealerCount"))
        assertTrue(!keys.contains("targetSales"))
        assertTrue(!keys.contains("modules"))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(
            listOf("Retail dealers", "Wholesale", "Modern trade", "HoReCa", "Van sales"),
            channels.options,
        )
        val roles = spec.sections
            .flatMap { it.fields }
            .first { it.key == "roles" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(
            listOf("Owner", "Manager", "Route lead", "Warehouse", "Salesman"),
            roles.options,
        )
    }

    @Test
    fun tradingMatchesFigma18833() {
        val spec = BusinessSetupDocuments.spec("TRADING_WHOLESALE")
        assertEquals("Northstar Trading Setup", spec.heroTitle)
        assertEquals("Start this business", spec.activateLabel)
        assertEquals("businessName", spec.defaultTitleKey)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(keys.containsAll(listOf(
            "businessName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        )))
        assertTrue(!keys.contains("modules"))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Retail buyers", "Wholesale", "Modern trade", "HoReCa", "Online"), channels.options)
        val roles = spec.sections
            .flatMap { it.fields }
            .first { it.key == "roles" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Owner", "Manager", "Sales exec", "Warehouse", "Sales rep"), roles.options)
    }

    @Test
    fun retailStoreMatchesFigma80797FiveSections() {
        val spec = BusinessSetupDocuments.spec("RETAIL_STORE")
        assertEquals("Harbor Mart Setup", spec.heroTitle)
        assertEquals("Start this store", spec.activateLabel)
        assertEquals("storeName", spec.defaultTitleKey)
        assertEquals(
            listOf(
                "Business Basics",
                "People & Roles",
                "Channels & Monthly Plan",
                "GST & Tax Details",
                "Review your setup",
            ),
            spec.sectionTitles,
        )
        assertTrue(spec.sections.none { it.id == "network" })
        val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
        assertTrue(
            keys.containsAll(
                listOf(
                    "storeName", "companyName", "gstin", "currency", "startDate", "endDate",
                    "description", "location", "roles", "channels", "planMonth",
                    "monthlyBudget", "monthlyTarget", "targetMetric",
                    "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
                ),
            ),
        )
        assertTrue(!keys.contains("modules"))
        assertTrue(!keys.contains("posStyle"))
        assertTrue(!keys.contains("locality"))
        val channels = spec.sections
            .flatMap { it.fields }
            .first { it.key == "channels" }
            .kind as BusinessSetupFieldSpec.Kind.MultiChoice
        assertEquals(listOf("Counter", "Home", "Online", "B2B", "Market"), channels.options)
    }

    @Test
    fun retailTrioSectionParity() {
        val expectedLabels = mapOf(
            "RETAIL_STORE" to "Start this store",
            "RESTAURANT_FOOD" to "Start this restaurant",
            "CONSUMER_SERVICE" to "Start this business",
        )
        expectedLabels.forEach { (code, label) ->
            val spec = BusinessSetupDocuments.spec(code)
            assertEquals(label, spec.activateLabel)
            assertTrue(spec.sectionTitles.contains("Business Basics"))
            assertTrue(spec.sectionTitles.contains("People & Roles"))
            assertTrue(spec.sectionTitles.contains("GST & Tax Details"))
            assertTrue(spec.sectionTitles.contains("Review your setup"))
        }
    }

    @Test
    fun manufacturingPairSectionParity() {
        listOf("STANDARD_PRODUCTION", "MADE_TO_ORDER").forEach { code ->
            val spec = BusinessSetupDocuments.spec(code)
            assertEquals("Start this business", spec.activateLabel)
            assertTrue(spec.sectionTitles.contains("Business Basics"))
            assertTrue(spec.sectionTitles.contains("People & Roles"))
            assertTrue(spec.sectionTitles.contains("GST & Tax Details"))
            assertTrue(spec.sectionTitles.contains("Review your setup"))
        }
    }

    @Test
    fun requiredTitleKeyPresentOnEveryDocument() {
        BusinessSetupFamily.entries.forEach { family ->
            family.chips.forEach { chip ->
                val spec = BusinessSetupDocuments.spec(chip.typeCode)
                val keys = spec.sections.flatMap { it.fields.map { f -> f.key } }.toSet()
                assertTrue(keys.contains(spec.defaultTitleKey))
            }
        }
    }
}
