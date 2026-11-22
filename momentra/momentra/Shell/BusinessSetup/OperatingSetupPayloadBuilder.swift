import Foundation

/// Maps Figma Business Setup form values → `OperatingPreferences` (+ master seeds).
/// Lockstep with Android `OperatingSetupPayloadBuilder` and backend `applyOperatingSetupAliases`.
enum OperatingSetupPayloadBuilder {
    struct MasterSeed: Equatable {
        let kind: String
        let name: String
    }

    struct Result: Equatable {
        let preferences: [String: Any]
        let masterSeeds: [MasterSeed]
        let validationError: String?
        let fieldErrors: [String: String]

        init(
            preferences: [String: Any],
            masterSeeds: [MasterSeed],
            validationError: String?,
            fieldErrors: [String: String] = [:]
        ) {
            self.preferences = preferences
            self.masterSeeds = masterSeeds
            self.validationError = validationError
            self.fieldErrors = fieldErrors
        }

        static func == (lhs: Result, rhs: Result) -> Bool {
            NSDictionary(dictionary: lhs.preferences).isEqual(to: rhs.preferences)
                && lhs.masterSeeds == rhs.masterSeeds
                && lhs.validationError == rhs.validationError
                && lhs.fieldErrors == rhs.fieldErrors
        }
    }

    private static let dualWriteBudgetTarget: Set<String> = [
        "TRADING_WHOLESALE",
        "DISTRIBUTION_DEALERSHIP",
        "STANDARD_PRODUCTION",
        "MADE_TO_ORDER",
    ]

    private static let retailChannelsRequired: Set<String> = [
        "RETAIL_STORE",
        "RESTAURANT_FOOD",
        "CONSUMER_SERVICE",
    ]

    private static let retailSubtypes: Set<String> = [
        "RETAIL_STORE",
        "RESTAURANT_FOOD",
        "CONSUMER_SERVICE",
    ]

    private static let marketingSubtypes: Set<String> = [
        "CLIENT_CAMPAIGN",
        "OWN_BUSINESS_CAMPAIGN",
    ]

    private static let tradingOrMfg: Set<String> = [
        "TRADING_WHOLESALE",
        "DISTRIBUTION_DEALERSHIP",
        "STANDARD_PRODUCTION",
        "MADE_TO_ORDER",
    ]

    private static let roleAliases: [String: String] = [
        "owner": "OWNER_ADMIN",
        "owners": "OWNER_ADMIN",
        "admin": "OWNER_ADMIN",
        "owner / admin": "OWNER_ADMIN",
        "owner/admin": "OWNER_ADMIN",
        "manager": "OPERATIONS_LEAD",
        "plant manager": "OPERATIONS_LEAD",
        "business manager": "OPERATIONS_LEAD",
        "operations lead": "OPERATIONS_LEAD",
        "operations": "OPERATIONS_LEAD",
        "campaign lead": "OPERATIONS_LEAD",
        "commercial": "OPERATIONS_LEAD",
        "marketer": "OPERATIONS_LEAD",
        "executive": "OPERATIONS_LEAD",
        "delivery": "OPERATIONS_LEAD",
        "route lead": "OPERATIONS_LEAD",
        "warehouse": "OPERATIONS_LEAD",
        "sales exec": "OPERATIONS_LEAD",
        "sales executive": "OPERATIONS_LEAD",
        "cashier": "TEAM_MEMBER",
        "floor": "TEAM_MEMBER",
        "stock": "TEAM_MEMBER",
        "kitchen": "TEAM_MEMBER",
        "waiter": "TEAM_MEMBER",
        "service": "TEAM_MEMBER",
        "specialist": "TEAM_MEMBER",
        "stylist": "TEAM_MEMBER",
        "therapist": "TEAM_MEMBER",
        "front desk": "TEAM_MEMBER",
        "salesman": "TEAM_MEMBER",
        "sales rep": "TEAM_MEMBER",
        "sales": "TEAM_MEMBER",
        "supervisor": "OPERATIONS_LEAD",
        "operator": "TEAM_MEMBER",
        "estimator": "CONTRIBUTOR",
        "shop floor": "TEAM_MEMBER",
        "accounts": "FINANCE",
        "finance": "FINANCE",
        "approver": "APPROVER",
        "contributor": "CONTRIBUTOR",
        "team member": "TEAM_MEMBER",
        "viewer": "OBSERVER",
        "viewer / auditor": "OBSERVER",
        "observer": "OBSERVER",
        "audit": "OBSERVER",
        "auditor": "OBSERVER",
    ]

    static func build(
        preferences: [String: Any],
        momentTypeCode: String,
        title: String,
        activating: Bool = true
    ) -> Result {
        var prefs = stringKeyed(preferences)
        let subtype = momentTypeCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        prefs["subtype"] = subtype

        let identity = firstNonEmpty(
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
            title
        )
        if prefs["outletName"] == nil, let restaurantName = prefs["restaurantName"] as? String,
           !restaurantName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            prefs["outletName"] = restaurantName
        }
        if !identity.isEmpty { prefs["identityName"] = identity }

        let party: String
        if retailSubtypes.contains(subtype) {
            party = firstNonEmpty(prefs["partyName"], prefs["companyName"], identity)
        } else if subtype == "CLIENT_CAMPAIGN" {
            party = firstNonEmpty(
                prefs["partyName"],
                prefs["clientName"],
                prefs["brandName"],
                prefs["principalBrand"],
                prefs["companyName"],
                prefs["customerName"]
            )
        } else {
            party = firstNonEmpty(
                prefs["partyName"],
                prefs["clientName"],
                prefs["brandName"],
                prefs["principalBrand"],
                prefs["companyName"],
                prefs["customerName"],
                prefs["format"],
                prefs["serviceStyle"],
                prefs["bookingStyle"],
                identity
            )
        }
        if !party.isEmpty { prefs["partyName"] = party }

        if stringValue(prefs["location"]).isEmpty {
            let location = firstNonEmpty(prefs["location"], prefs["locality"])
            if !location.isEmpty { prefs["location"] = location }
        }

        if prefs["startDate"] == nil, let planMonth = prefs["planMonth"] {
            let month = stringValue(planMonth)
            if month.range(of: #"^\d{4}-\d{2}$"#, options: .regularExpression) != nil {
                prefs["startDate"] = "\(month)-01"
            } else if let coerced = coerceSetupDate(month) {
                prefs["startDate"] = coerced
            }
        }

        if let saleIncludes = mapSalePriceIncludesGst(prefs["salePriceIncludesGst"]) {
            prefs["salePriceIncludesGst"] = saleIncludes
        }

        if positiveAmount(prefs["monthlyBudget"]) == nil {
            let budget = firstNonEmpty(
                prefs["monthlyBudget"],
                prefs["monthlyExpense"],
                prefs["marketingBudget"],
                prefs["targetSales"],
                prefs["coverTarget"],
                prefs["avgTicket"]
            )
            if positiveAmount(budget) != nil { prefs["monthlyBudget"] = budget }
        }
        if positiveAmount(prefs["monthlyTarget"]) == nil {
            let target = firstNonEmpty(
                prefs["monthlyTarget"],
                prefs["monthlySalesTarget"],
                prefs["monthlyChargeTarget"],
                prefs["targetSales"],
                prefs["leadGoal"],
                prefs["conversionGoal"],
                prefs["monthlyOutput"],
                prefs["monthlyJobs"],
                prefs["coverTarget"]
            )
            if positiveAmount(target) != nil { prefs["monthlyTarget"] = target }
        }
        if dualWriteBudgetTarget.contains(subtype) {
            if positiveAmount(prefs["monthlyBudget"]) == nil, let t = positiveAmount(prefs["monthlyTarget"]) {
                prefs["monthlyBudget"] = t
            }
            if positiveAmount(prefs["monthlyTarget"]) == nil, let b = positiveAmount(prefs["monthlyBudget"]) {
                prefs["monthlyTarget"] = b
            }
        }
        if let budget = positiveAmount(prefs["monthlyBudget"]) { prefs["monthlyBudget"] = budget }
        if let target = positiveAmount(prefs["monthlyTarget"]) { prefs["monthlyTarget"] = target }

        let tracking = mapGstTracking(prefs["gstTracking"] ?? prefs["gstRegistered"])
        if !tracking.isEmpty { prefs["gstTracking"] = tracking }
        let registration = mapGstRegistration(prefs["gstRegistration"] ?? prefs["gstType"])
        if !registration.isEmpty { prefs["gstRegistration"] = registration }

        var channels = splitList(prefs["channels"])
        if channels.isEmpty {
            channels = splitList(firstNonEmpty(
                prefs["posStyle"],
                prefs["bookingStyle"],
                prefs["orderChannels"],
                prefs["territory"],
                prefs["routeStyle"]
            ))
        }
        if !channels.isEmpty { prefs["channels"] = channels }

        if let start = coerceSetupDate(prefs["startDate"]) ?? coerceSetupDate(prefs["startedOn"]) {
            prefs["startDate"] = start
        }
        if let end = coerceSetupDate(prefs["endDate"]) {
            prefs["endDate"] = end
        }

        let currency = (prefs["currency"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        prefs["currency"] = (currency?.isEmpty == false) ? currency! : "INR"

        if let gstin = prefs["gstin"] as? String {
            prefs["gstin"] = gstin.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        }

        if prefs["participants"] == nil {
            let roles = splitList(prefs["roles"])
            let members = splitList(prefs["teamMembers"])
            let count = max(roles.count, members.count)
            if count > 0 {
                var rows: [[String: String]] = []
                for i in 0..<count {
                    let role = mapRole(roles[safe: i] ?? roles.first ?? "TEAM_MEMBER")
                    let name = members[safe: i] ?? roles[safe: i] ?? "Member \(i + 1)"
                    rows.append(["displayName": name, "role": role])
                }
                prefs["participants"] = rows
            }
        }

        var counterparties: [[String: String]] = []
        if let existing = prefs["counterparties"] as? [[String: Any]] {
            for row in existing {
                let name = stringValue(row["name"])
                guard !name.isEmpty else { continue }
                counterparties.append([
                    "name": name,
                    "kind": stringValue(row["kind"]).isEmpty ? "OTHER" : stringValue(row["kind"]),
                ])
            }
        }
        let networkSeeds: [(String, String)] = [
            ("supplierName", "SUPPLIER"),
            ("customerName", "CUSTOMER"),
            ("buyerName", "BUYER"),
            ("vendorName", "SUPPLIER"),
            ("principalBrand", "PRINCIPAL"),
            ("clientName", "CLIENT"),
        ]
        for (key, kind) in networkSeeds {
            let name = stringValue(prefs[key])
            guard !name.isEmpty else { continue }
            if !counterparties.contains(where: { $0["name"]?.caseInsensitiveCompare(name) == .orderedSame }) {
                counterparties.append(["name": name, "kind": kind])
            }
        }
        if !counterparties.isEmpty { prefs["counterparties"] = counterparties }

        var masters: [MasterSeed] = []
        for row in counterparties {
            if let name = row["name"], let kind = row["kind"], !name.isEmpty {
                masters.append(MasterSeed(kind: kind, name: name))
            }
        }
        for channel in channels {
            masters.append(MasterSeed(kind: "CHANNEL", name: channel))
        }

        if !activating {
            return Result(preferences: prefs, masterSeeds: masters, validationError: nil)
        }
        let fieldErrors = validationFieldErrors(subtype: subtype, prefs: prefs, channels: channels)
        return Result(
            preferences: prefs,
            masterSeeds: masters,
            validationError: fieldErrors.values.first,
            fieldErrors: fieldErrors
        )
    }

    // MARK: - Coercion

    static func positiveAmount(_ value: Any?) -> String? {
        if let n = value as? NSNumber, n.doubleValue > 0 {
            return String(format: "%.4f", n.doubleValue)
        }
        var raw = stringValue(value)
        if raw.isEmpty { return nil }
        if let re = try? NSRegularExpression(pattern: #"^(₹|rs\.?|inr)\s*"#, options: .caseInsensitive) {
            raw = re.stringByReplacingMatches(in: raw, range: NSRange(raw.startIndex..., in: raw), withTemplate: "")
        }
        raw = raw.replacingOccurrences(of: "₹", with: "")
            .replacingOccurrences(of: ",", with: "")
            .replacingOccurrences(of: " ", with: "")
        guard let re = try? NSRegularExpression(pattern: #"^\d+(\.\d{1,4})?$"#),
              re.firstMatch(in: raw, range: NSRange(raw.startIndex..., in: raw)) != nil,
              let n = Double(raw), n > 0 else { return nil }
        return String(format: "%.4f", n)
    }

    static func coerceSetupDate(_ value: Any?) -> String? {
        let raw = stringValue(value)
        if raw.isEmpty { return nil }
        if raw.range(of: #"^\d{4}-\d{2}-\d{2}"#, options: .regularExpression) != nil {
            return String(raw.prefix(10))
        }
        let months: [String: Int] = [
            "jan": 1, "january": 1, "feb": 2, "february": 2, "mar": 3, "march": 3,
            "apr": 4, "april": 4, "may": 5, "jun": 6, "june": 6, "jul": 7, "july": 7,
            "aug": 8, "august": 8, "sep": 9, "sept": 9, "september": 9,
            "oct": 10, "october": 10, "nov": 11, "november": 11, "dec": 12, "december": 12,
        ]
        let parts = raw.split(separator: " ").map(String.init)
        if parts.count == 3, let day = Int(parts[0]), let year = Int(parts[2]),
           let month = months[parts[1].lowercased()], day >= 1, day <= 31 {
            return String(format: "%04d-%02d-%02d", year, month, day)
        }
        return nil
    }

    static func splitList(_ value: Any?) -> [String] {
        if let arr = value as? [Any] {
            return arr.map { stringValue($0) }.filter { !$0.isEmpty }
        }
        let raw = stringValue(value)
        if raw.isEmpty { return [] }
        return raw.split(whereSeparator: { $0 == "|" || $0 == "," })
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
    }

    static func mapGstTracking(_ value: Any?) -> String {
        let raw = stringValue(value).lowercased()
        if raw.isEmpty { return "" }
        if ["yes", "y", "true", "1", "registered"].contains(raw) { return "YES" }
        if ["no", "n", "false", "0", "not now", "not_now", "later"].contains(raw) { return "NOT_NOW" }
        let upper = raw.uppercased()
        return ["YES", "NOT_NOW"].contains(upper) ? upper : ""
    }

    static func mapGstRegistration(_ value: Any?) -> String {
        let raw = stringValue(value).lowercased().replacingOccurrences(of: " ", with: "_")
        if raw.isEmpty { return "" }
        switch raw {
        case "regular": return "REGULAR"
        case "composition", "composition_scheme": return "COMPOSITION"
        case "unregistered": return "UNREGISTERED"
        case "not_sure", "notsure", "unsure": return "NOT_SURE"
        default:
            let upper = raw.uppercased()
            return ["REGULAR", "COMPOSITION", "UNREGISTERED", "NOT_SURE"].contains(upper) ? upper : ""
        }
    }

    static func mapSalePriceIncludesGst(_ value: Any?) -> String? {
        let raw = stringValue(value).lowercased()
        if raw.isEmpty { return nil }
        if ["yes", "y", "true", "prices include gst", "include", "included"].contains(raw) { return "YES" }
        if ["no", "n", "false", "gst is added separately", "added separately", "exclude", "excluded"].contains(raw) {
            return "NO"
        }
        let upper = raw.uppercased()
        return ["YES", "NO"].contains(upper) ? upper : nil
    }

    /// Soft Channels status copy — informational, never blocks activate for retail.
    static func planStatusMessage(preferences: [String: Any]) -> (ok: Bool, message: String) {
        let budgetOk = positiveAmount(preferences["monthlyBudget"]) != nil
        let targetOk = positiveAmount(
            preferences["monthlyTarget"] ?? preferences["monthlyChargeTarget"] ?? preferences["targetSales"]
        ) != nil
        switch (budgetOk, targetOk) {
        case (true, true): return (true, "Budget and sales target look good")
        case (false, true): return (false, "Add a monthly budget so the plan stays complete.")
        case (true, false): return (false, "Add a monthly sales target so the plan stays complete.")
        default: return (false, "Budget and sales target are optional until you start.")
        }
    }

    static func mapRole(_ value: Any?) -> String {
        let raw = stringValue(value)
        if raw.isEmpty { return "TEAM_MEMBER" }
        let code = raw.uppercased().replacingOccurrences(of: " ", with: "_")
        let known = ["OWNER_ADMIN", "OPERATIONS_LEAD", "FINANCE", "APPROVER", "CONTRIBUTOR", "TEAM_MEMBER", "OBSERVER"]
        if known.contains(code) { return code }
        return roleAliases[raw.lowercased()] ?? "TEAM_MEMBER"
    }

    // MARK: - Helpers

    private static func validationFieldErrors(
        subtype: String,
        prefs: [String: Any],
        channels: [String]
    ) -> [String: String] {
        var errors: [String: String] = [:]
        if stringValue(prefs["identityName"]).isEmpty {
            let titleKey: String
            switch subtype {
            case "CONSUMER_SERVICE": titleKey = "serviceName"
            case "RETAIL_STORE": titleKey = "storeName"
            case "RESTAURANT_FOOD": titleKey = "outletName"
            case "STANDARD_PRODUCTION": titleKey = "plantName"
            case "MADE_TO_ORDER": titleKey = "shopName"
            case "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN": titleKey = "campaignName"
            default: titleKey = "businessName"
            }
            errors[titleKey] = "Moment name is required."
        }
        if stringValue(prefs["partyName"]).isEmpty {
            errors["companyName"] = "Business party name is required."
        }
        if marketingSubtypes.contains(subtype) {
            if positiveAmount(prefs["monthlyBudget"]) == nil {
                errors["monthlyBudget"] = "Monthly budget must be a positive amount."
            }
            let start = stringValue(prefs["startDate"])
            if start.range(of: #"^\d{4}-\d{2}-\d{2}$"#, options: .regularExpression) == nil,
               coerceSetupDate(prefs["startDate"]) == nil {
                errors["startDate"] = "Campaign start date is required."
            }
        } else if tradingOrMfg.contains(subtype) {
            if positiveAmount(prefs["monthlyBudget"]) == nil {
                errors["monthlyBudget"] = "Monthly budget must be a positive amount."
            }
            if positiveAmount(prefs["monthlyTarget"]) == nil {
                errors["monthlyTarget"] = "Monthly target must be a positive amount."
            }
        }
        if retailChannelsRequired.contains(subtype), channels.isEmpty {
            errors["channels"] = "Select at least one channel."
        }
        let tracking = stringValue(prefs["gstTracking"])
        let gstin = stringValue(prefs["gstin"])
        if tracking == "YES", !gstin.isEmpty, !isValidGstin(gstin) {
            errors["gstin"] = "Enter a valid 15-digit GSTIN."
        }
        return errors
    }

    static func isValidGstin(_ value: String) -> Bool {
        let raw = value.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        return raw.range(
            of: #"^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$"#,
            options: .regularExpression
        ) != nil
    }

    private static func stringKeyed(_ preferences: [String: Any]) -> [String: Any] {
        var out: [String: Any] = [:]
        for (key, value) in preferences {
            if let s = value as? String {
                let trimmed = s.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty { out[key] = trimmed }
            } else {
                out[key] = value
            }
        }
        return out
    }

    private static func firstNonEmpty(_ values: Any?...) -> String {
        for value in values {
            let text = stringValue(value)
            if !text.isEmpty { return text }
        }
        return ""
    }

    private static func stringValue(_ value: Any?) -> String {
        guard let value else { return "" }
        if let s = value as? String { return s.trimmingCharacters(in: .whitespacesAndNewlines) }
        if let n = value as? NSNumber { return n.stringValue }
        return String(describing: value).trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
