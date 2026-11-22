import Foundation
import Testing
@testable import momentra

struct BusinessSetupFieldModelTests {

    @Test func fieldKindsRoundTripInMinimalSpec() {
        let fields: [BusinessSetupFieldSpec] = [
            BusinessSetupFieldSpec(key: "t", label: "T", placeholder: "p", kind: .text, helper: "hint"),
            BusinessSetupFieldSpec(key: "c", label: "C", placeholder: "", kind: .choice(["A", "B"])),
            BusinessSetupFieldSpec(key: "m", label: "M", placeholder: "", kind: .multiChoice(["X", "Y"])),
            BusinessSetupFieldSpec(key: "g", label: "G", placeholder: "", kind: .toggle),
            BusinessSetupFieldSpec(key: "d", label: "D", placeholder: "date", kind: .date),
            BusinessSetupFieldSpec(key: "r", label: "R", placeholder: "Add", kind: .rowAction("Add")),
            BusinessSetupFieldSpec(key: "mem", label: "Mem", placeholder: "", kind: .memberList(["You"])),
            BusinessSetupFieldSpec(
                key: "mod",
                label: "Mod",
                placeholder: "",
                kind: .moduleList([
                    BusinessSetupModuleRow("A", subtitle: "sub-a"),
                    BusinessSetupModuleRow("B"),
                ])
            ),
            BusinessSetupFieldSpec(key: "rev", label: "Rev", placeholder: "", kind: .reviewSummary(["t", "c"])),
            BusinessSetupFieldSpec(key: "ph", label: "Identity", placeholder: "", kind: .panelHeader("Identity")),
        ]
        let section = BusinessSetupSectionSpec(id: "s", title: "S", subtitle: "", fields: fields)
        let spec = BusinessSetupDocumentSpec(
            typeCode: "TEST",
            familyTitle: "T",
            eyebrow: "e",
            heroTitle: "h",
            heroSubtitle: "sub",
            activateLabel: "Go",
            defaultTitleKey: "t",
            sections: [section]
        )
        let prefs = spec.defaultPreferences
        #expect(prefs["t"] == "")
        #expect(prefs["c"] == "A")
        #expect(prefs["m"] == "X")
        #expect(prefs["g"] == "Yes")
        #expect(prefs["d"] == "")
        #expect(prefs["r"] == "")
        #expect(prefs["mem"] == "You")
        #expect(prefs["mod"] == "")
        #expect(prefs["rev"] == "")
        #expect(prefs["ph"] == "")
        #expect(fields[0].helper == "hint")
        #expect(fields.count == 10)
        if case .moduleList(let modules) = fields[7].kind {
            #expect(modules[0].subtitle == "sub-a")
        } else {
            Issue.record("expected moduleList")
        }
        #expect(BusinessSetupFieldChrome.hasError("bad"))
        #expect(!BusinessSetupFieldChrome.hasError(nil))
        #expect(!BusinessSetupFieldChrome.hasError(""))
    }

    @Test func consumerServiceMatchesFigma71162() {
        let spec = BusinessSetupDocuments.spec(for: "CONSUMER_SERVICE")
        #expect(spec.heroTitle == "BrightCare Setup")
        #expect(spec.activateLabel == "Start this business")
        #expect(spec.defaultTitleKey == "serviceName")
        #expect(spec.sectionTitles == [
            "Business Basics",
            "People & Roles",
            "Channels & Monthly Plan",
            "GST & Tax Details",
            "Review your setup",
        ])
        #expect(spec.sections.allSatisfy { $0.id != "network" })
        let keys = Set(spec.sections.flatMap { $0.fields.map(\.key) })
        for key in [
            "serviceName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyChargeTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        ] {
            #expect(keys.contains(key))
        }
        #expect(!keys.contains("modules"))
        let channelsField = spec.sections.flatMap(\.fields).first { $0.key == "channels" }
        if case .multiChoice(let options) = channelsField?.kind {
            #expect(options == ["Studio", "Home", "Online", "Corporate", "Events"])
        } else {
            Issue.record("channels missing")
        }
        let rolesField = spec.sections.flatMap(\.fields).first { $0.key == "roles" }
        if case .multiChoice(let options) = rolesField?.kind {
            #expect(options == ["Owner", "Manager", "Stylist", "Front desk", "Therapist"])
        } else {
            Issue.record("roles missing")
        }
    }

    @Test func restaurantMatchesFigma90612() {
        let spec = BusinessSetupDocuments.spec(for: "RESTAURANT_FOOD")
        #expect(spec.heroTitle == "Spice Route Setup")
        #expect(spec.activateLabel == "Start this restaurant")
        #expect(spec.defaultTitleKey == "outletName")
        #expect(spec.sectionTitles == [
            "Business Basics",
            "People & Roles",
            "Channels & Monthly Plan",
            "Kitchen, Menu & Network",
            "GST & Tax Details",
            "Review your setup",
        ])
        let keys = Set(spec.sections.flatMap { $0.fields.map(\.key) })
        for key in [
            "outletName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        ] {
            #expect(keys.contains(key))
        }
        let channelsField = spec.sections.flatMap(\.fields).first { $0.key == "channels" }
        if case .multiChoice(let options) = channelsField?.kind {
            #expect(options == ["Dine-in", "Direct", "Zomato", "Takeaway", "Swiggy"])
        } else {
            Issue.record("channels missing")
        }
        let rolesField = spec.sections.flatMap(\.fields).first { $0.key == "roles" }
        if case .multiChoice(let options) = rolesField?.kind {
            #expect(options == ["Owner", "Manager", "Cashier", "Kitchen", "Waiter"])
        } else {
            Issue.record("roles missing")
        }
        let modulesField = spec.sections.flatMap(\.fields).first { $0.key == "modules" }
        if case .moduleList(let modules) = modulesField?.kind {
            #expect(modules.count == 4)
            #expect(modules[0].title == "Menu")
            #expect(modules[1].title == "Stations")
            #expect(modules[2].title == "Vendors")
        } else {
            Issue.record("modules missing")
        }
    }

    @Test func emptyValuesMapDoesNotCrashDefaultPreferences() {
        for family in BusinessSetupFamily.allCases {
            for chip in family.chips {
                let prefs = BusinessSetupDocuments.spec(for: chip.typeCode).defaultPreferences
                #expect(!prefs.isEmpty)
            }
        }
    }

    @Test func clientCampaignHasV6SectionsAndActivateLabel() {
        let spec = BusinessSetupDocuments.spec(for: "CLIENT_CAMPAIGN")
        #expect(spec.activateLabel == "Start this business")
        #expect(spec.sectionTitles == [
            "Business Basics",
            "People & Roles",
            "Network / Counterparties",
            "Channels & Monthly Plan",
            "GST & Tax Details",
            "Review your setup",
        ])
        #expect(spec.eyebrow == "Client campaign")
        #expect(spec.heroTitle == "Set up a client campaign")
    }

    @Test func ownMarketingMatchesMarketingPair() {
        let spec = BusinessSetupDocuments.spec(for: "OWN_BUSINESS_CAMPAIGN")
        #expect(spec.activateLabel == "Start this business")
        #expect(spec.sectionTitles.contains("Business Basics"))
        #expect(spec.sectionTitles.contains("People & Roles"))
        #expect(spec.sectionTitles.contains("GST & Tax Details"))
        #expect(spec.sectionTitles.contains("Review your setup"))
    }

    @Test func tradingActivateLabelIsStartTheBusiness() {
        let trading = BusinessSetupDocuments.spec(for: "TRADING_WHOLESALE")
        let distribution = BusinessSetupDocuments.spec(for: "DISTRIBUTION_DEALERSHIP")
        #expect(trading.activateLabel == "Start this business")
        #expect(distribution.activateLabel == "Start the business!")
        #expect(trading.sectionTitles.contains("Catalogue, Warehouses & Network"))
        #expect(BusinessSetupTheme.forTypeCode("TRADING_WHOLESALE").domain == .trading)
    }

    @Test func tradingMatchesFigma18833() {
        let spec = BusinessSetupDocuments.spec(for: "TRADING_WHOLESALE")
        #expect(spec.heroTitle == "Northstar Trading Setup")
        #expect(spec.activateLabel == "Start this business")
        #expect(spec.defaultTitleKey == "businessName")
        #expect(spec.sectionTitles == [
            "Business Basics",
            "People & Roles",
            "Channels & Monthly Plan",
            "Catalogue, Warehouses & Network",
            "GST & Tax Details",
            "Review your setup",
        ])
        let keys = Set(spec.sections.flatMap { $0.fields.map(\.key) })
        for key in [
            "businessName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        ] {
            #expect(keys.contains(key))
        }
        let channelsField = spec.sections.flatMap(\.fields).first { $0.key == "channels" }
        if case .multiChoice(let options) = channelsField?.kind {
            #expect(options == ["Retail buyers", "Wholesale", "Modern trade", "HoReCa", "Online"])
        } else {
            Issue.record("channels missing")
        }
        let rolesField = spec.sections.flatMap(\.fields).first { $0.key == "roles" }
        if case .multiChoice(let options) = rolesField?.kind {
            #expect(options == ["Owner", "Manager", "Sales exec", "Warehouse", "Sales rep"])
        } else {
            Issue.record("roles missing")
        }
        let modulesField = spec.sections.flatMap(\.fields).first { $0.key == "modules" }
        if case .moduleList(let modules) = modulesField?.kind {
            #expect(modules.count == 4)
            #expect(modules[0].title == "Catalogue & SKU")
            #expect(modules[1].title == "Godowns & branches")
            #expect(modules[2].title == "Suppliers & buyers")
            #expect(modules[3].title == "Team invite & QR")
        } else {
            Issue.record("modules missing")
        }
    }

    @Test func retailStoreMatchesFigma80797FiveSections() {
        let spec = BusinessSetupDocuments.spec(for: "RETAIL_STORE")
        #expect(spec.heroTitle == "Harbor Mart Setup")
        #expect(spec.activateLabel == "Start this store")
        #expect(spec.defaultTitleKey == "storeName")
        #expect(spec.sectionTitles == [
            "Business Basics",
            "People & Roles",
            "Channels & Monthly Plan",
            "GST & Tax Details",
            "Review your setup",
        ])
        #expect(spec.sections.allSatisfy { $0.id != "network" })
        let keys = Set(spec.sections.flatMap { $0.fields.map(\.key) })
        for key in [
            "storeName", "companyName", "gstin", "currency", "startDate", "endDate",
            "description", "location", "roles", "channels", "planMonth",
            "monthlyBudget", "monthlyTarget", "targetMetric",
            "gstTracking", "gstRegistration", "gstState", "salePriceIncludesGst",
        ] {
            #expect(keys.contains(key))
        }
        #expect(!keys.contains("modules"))
        #expect(!keys.contains("posStyle"))
        #expect(!keys.contains("locality"))
        let channelsField = spec.sections.flatMap(\.fields).first { $0.key == "channels" }
        if case .multiChoice(let options) = channelsField?.kind {
            #expect(options == ["Counter", "Home", "Online", "B2B", "Market"])
        } else {
            Issue.record("channels missing")
        }
    }

    @Test func retailTrioSectionParity() {
        let expectedLabels = [
            "RETAIL_STORE": "Start this store",
            "RESTAURANT_FOOD": "Start this restaurant",
            "CONSUMER_SERVICE": "Start this business",
        ]
        for (code, label) in expectedLabels {
            let spec = BusinessSetupDocuments.spec(for: code)
            #expect(spec.activateLabel == label)
            #expect(spec.sectionTitles.contains("Business Basics"))
            #expect(spec.sectionTitles.contains("People & Roles"))
            #expect(spec.sectionTitles.contains("GST & Tax Details"))
            #expect(spec.sectionTitles.contains("Review your setup"))
        }
    }

    @Test func manufacturingPairSectionParity() {
        for code in ["STANDARD_PRODUCTION", "MADE_TO_ORDER"] {
            let spec = BusinessSetupDocuments.spec(for: code)
            #expect(spec.activateLabel == "Start this business")
            #expect(spec.sectionTitles.contains("Business Basics"))
            #expect(spec.sectionTitles.contains("People & Roles"))
            #expect(spec.sectionTitles.contains("GST & Tax Details"))
            #expect(spec.sectionTitles.contains("Review your setup"))
        }
    }

    @Test func requiredTitleKeyPresentOnEveryDocument() {
        for family in BusinessSetupFamily.allCases {
            for chip in family.chips {
                let spec = BusinessSetupDocuments.spec(for: chip.typeCode)
                let keys = Set(spec.sections.flatMap { $0.fields.map(\.key) })
                #expect(keys.contains(spec.defaultTitleKey))
            }
        }
    }
}
