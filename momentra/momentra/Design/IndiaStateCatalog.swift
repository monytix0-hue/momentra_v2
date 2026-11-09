import Foundation

/// One Indian state or union territory, keyed by its two-digit GST state code.
struct IndiaState: Identifiable, Hashable {
    /// Two-digit GST state code, zero-padded ("01"…"38", plus "97").
    let code: String
    /// The name written onto the preference and shown in the picker.
    let name: String
    let isUnionTerritory: Bool

    var id: String { code }
}

/// The canonical India States & Union Territories registry, mirroring the backend table in
/// `backend/typescript/src/modules/business/operating-gst-states.ts`.
///
/// GST decides between CGST+SGST and IGST by comparing the business's state with the place of
/// supply. Comparing typed names is what let "MH" and "Maharashtra" read as two different states
/// and charge IGST on an intra-state sale, so every state field stores the canonical name and
/// every jurisdiction decision compares the two-digit code.
///
/// The list is bundled rather than fetched so the picker opens instantly offline. The backend
/// parity test asserts this file, the Android copy, and the server table carry the same 37 pairs.
enum IndiaStateCatalog {

    /// 28 states and 8 union territories in GST code order, plus "97 Other Territory".
    ///
    /// Code 25 (Daman & Diu) is deliberately absent: the territory merged into 26, and
    /// `canonical(_:)` maps the old code forward rather than reviving a retired jurisdiction.
    static let all: [IndiaState] = [
        IndiaState(code: "01", name: "Jammu & Kashmir", isUnionTerritory: true),
        IndiaState(code: "02", name: "Himachal Pradesh", isUnionTerritory: false),
        IndiaState(code: "03", name: "Punjab", isUnionTerritory: false),
        IndiaState(code: "04", name: "Chandigarh", isUnionTerritory: true),
        IndiaState(code: "05", name: "Uttarakhand", isUnionTerritory: false),
        IndiaState(code: "06", name: "Haryana", isUnionTerritory: false),
        IndiaState(code: "07", name: "Delhi", isUnionTerritory: true),
        IndiaState(code: "08", name: "Rajasthan", isUnionTerritory: false),
        IndiaState(code: "09", name: "Uttar Pradesh", isUnionTerritory: false),
        IndiaState(code: "10", name: "Bihar", isUnionTerritory: false),
        IndiaState(code: "11", name: "Sikkim", isUnionTerritory: false),
        IndiaState(code: "12", name: "Arunachal Pradesh", isUnionTerritory: false),
        IndiaState(code: "13", name: "Nagaland", isUnionTerritory: false),
        IndiaState(code: "14", name: "Manipur", isUnionTerritory: false),
        IndiaState(code: "15", name: "Mizoram", isUnionTerritory: false),
        IndiaState(code: "16", name: "Tripura", isUnionTerritory: false),
        IndiaState(code: "17", name: "Meghalaya", isUnionTerritory: false),
        IndiaState(code: "18", name: "Assam", isUnionTerritory: false),
        IndiaState(code: "19", name: "West Bengal", isUnionTerritory: false),
        IndiaState(code: "20", name: "Jharkhand", isUnionTerritory: false),
        IndiaState(code: "21", name: "Odisha", isUnionTerritory: false),
        IndiaState(code: "22", name: "Chhattisgarh", isUnionTerritory: false),
        IndiaState(code: "23", name: "Madhya Pradesh", isUnionTerritory: false),
        IndiaState(code: "24", name: "Gujarat", isUnionTerritory: false),
        IndiaState(code: "26", name: "Dadra & Nagar Haveli and Daman & Diu", isUnionTerritory: true),
        IndiaState(code: "27", name: "Maharashtra", isUnionTerritory: false),
        IndiaState(code: "29", name: "Karnataka", isUnionTerritory: false),
        IndiaState(code: "30", name: "Goa", isUnionTerritory: false),
        IndiaState(code: "31", name: "Lakshadweep", isUnionTerritory: true),
        IndiaState(code: "32", name: "Kerala", isUnionTerritory: false),
        IndiaState(code: "33", name: "Tamil Nadu", isUnionTerritory: false),
        IndiaState(code: "34", name: "Puducherry", isUnionTerritory: true),
        IndiaState(code: "35", name: "Andaman & Nicobar Islands", isUnionTerritory: true),
        IndiaState(code: "36", name: "Telangana", isUnionTerritory: false),
        IndiaState(code: "37", name: "Andhra Pradesh", isUnionTerritory: false),
        IndiaState(code: "38", name: "Ladakh", isUnionTerritory: true),
        IndiaState(code: "97", name: "Other Territory", isUnionTerritory: true),
    ]

    static var states: [IndiaState] { all.filter { !$0.isUnionTerritory } }
    static var unionTerritories: [IndiaState] { all.filter(\.isUnionTerritory) }

    private static let byCode: [String: IndiaState] = Dictionary(uniqueKeysWithValues: all.map { ($0.code, $0) })
    private static let byName: [String: IndiaState] = Dictionary(uniqueKeysWithValues: all.map { (normalize($0.name), $0) })

    /// Spellings seen in older saved rows and in free text typed before the picker existed.
    /// Keys are normalized (lowercased, punctuation collapsed); values are GST state codes.
    private static let aliases: [String: String] = [
        "ap": "37", "ar": "12", "as": "18", "br": "10", "cg": "22", "ch": "04",
        "dd": "26", "dl": "07", "dn": "26", "ga": "30", "gj": "24", "hp": "02",
        "hr": "06", "jh": "20", "jk": "01", "j k": "01", "ka": "29", "kl": "32", "la": "38",
        "ld": "31", "mh": "27", "ml": "17", "mn": "14", "mp": "23", "mz": "15",
        "nl": "13", "or": "21", "pb": "03", "py": "34", "rj": "08", "sk": "11",
        "tg": "36", "tn": "33", "tr": "16", "ts": "36", "uk": "05", "up": "09",
        "wb": "19",
        "bangalore": "29", "bengaluru": "29", "bombay": "27", "calcutta": "19",
        "chennai": "33", "cochin": "32", "ernakulam": "32", "gurgaon": "06",
        "gurugram": "06", "hyderabad": "36", "kolkata": "19", "mumbai": "27",
        "mysore": "29", "mysuru": "29", "ncr": "07", "noida": "09", "pune": "27",
        "trivandrum": "32", "visakhapatnam": "37", "vizag": "37",
        "daman": "26", "diu": "26", "daman and diu": "26",
        "dadra and nagar haveli": "26", "nagar haveli": "26",
        "andaman and nicobar islands": "35", "andaman": "35", "nicobar": "35",
        "andaman nicobar": "35",
        "jammu and kashmir": "01", "jammu": "01", "kashmir": "01",
        "orissa": "21", "pondicherry": "34", "uttaranchal": "05",
        "new delhi": "07", "nct": "07", "nct of delhi": "07",
        // The retired Daman & Diu code maps forward to the merged territory.
        "25": "26",
    ]

    private static func normalize(_ raw: String) -> String {
        raw.lowercased()
            .replacingOccurrences(of: "[^a-z0-9]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
    }

    /// Resolve any spelling of an Indian state to its canonical record, or `nil`.
    ///
    /// Accepts the full name in any casing, the two-digit code, a two-letter postal abbreviation,
    /// and the legacy / city spellings in `aliases`.
    static func resolve(_ raw: String?) -> IndiaState? {
        guard let text = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty else {
            return nil
        }
        if text.range(of: "^\\d{2}$", options: .regularExpression) != nil {
            if let byCode = byCode[text] { return byCode }
            if let alias = aliases[text], let mapped = byCode[alias] { return mapped }
        }
        let key = normalize(text)
        guard !key.isEmpty else { return nil }
        if let named = byName[key] { return named }
        if let alias = aliases[key], let mapped = byCode[alias] { return mapped }
        if key.range(of: "^\\d{1,2}$", options: .regularExpression) != nil {
            return byCode[key.leftPadded(to: 2)]
        }
        return nil
    }

    /// The canonical full name for any spelling, or `""` when it cannot be resolved.
    static func canonical(_ raw: String?) -> String {
        resolve(raw)?.name ?? ""
    }

    /// The two-digit GST code for any spelling, or `""` when it cannot be resolved.
    static func codeOf(_ raw: String?) -> String {
        resolve(raw)?.code ?? ""
    }

    /// What to show for a stored value: the canonical name when known, otherwise the raw text.
    static func displayName(_ raw: String?) -> String {
        let text = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return resolve(text)?.name ?? text
    }

    /// `INTRA` when both sides resolve to the same code, `INTER` when both resolve and differ,
    /// and `nil` when neither can be resolved.
    static func jurisdiction(businessState: String?, supplyState: String?) -> String? {
        let business = resolve(businessState)
        let supply = resolve(supplyState)
        if let business, let supply {
            return business.code == supply.code ? "INTRA" : "INTER"
        }
        let left = (businessState ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let right = (supplyState ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !left.isEmpty, !right.isEmpty else { return nil }
        return left == right ? "INTRA" : "INTER"
    }

    /// The hint under a GSTIN field: whether its first two characters name the selected state.
    ///
    /// `nil` when the GSTIN is too short to judge or the state is not yet known.
    static func gstinStateHint(gstin: String?, state: String?) -> String? {
        let text = (gstin ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        guard text.count >= 2 else { return nil }
        let prefix = String(text.prefix(2))
        guard prefix.range(of: "^\\d{2}$", options: .regularExpression) != nil else { return nil }
        guard let declared = byCode[prefix] else { return nil }
        guard let selected = resolve(state) else { return nil }
        return declared.code == selected.code
            ? "GSTIN starts with \(declared.code), which is \(declared.name)."
            : "GSTIN starts with \(declared.code) (\(declared.name)) but the state is \(selected.name)."
    }

    /// States and union territories matching a picker query, states first.
    static func search(_ query: String?) -> [IndiaState] {
        let q = (query ?? "").trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return states + unionTerritories }
        return (states + unionTerritories).filter {
            $0.name.lowercased().contains(q) || $0.code == q
        }
    }
}

private extension String {
    func leftPadded(to width: Int) -> String {
        count >= width ? self : String(repeating: "0", count: width - count) + self
    }
}
