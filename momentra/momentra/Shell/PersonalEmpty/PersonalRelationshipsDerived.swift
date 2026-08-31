import Foundation

/// Relationships Bond Index + axis scores from pulse + widgetPayload (Figma 505:11793).
enum PersonalRelationshipsDerived {
    struct BondAxes {
        let trust: String
        let care: String
        let support: String
        let presence: String
    }

    static func payloadNumber(_ payload: [String: AnyDecodable]?, key: String) -> Double? {
        guard let raw = payload?[key]?.value else { return nil }
        if let n = raw as? Double { return n }
        if let n = raw as? Int { return Double(n) }
        if let s = raw as? String { return PersonalLifeOpsDerived.scoreNumber(s) }
        if let n = raw as? NSNumber { return n.doubleValue }
        return nil
    }

    static func displayFromPayloadOrFallback(
        payload: [String: AnyDecodable]?,
        payloadKey: String,
        fallback: String?
    ) -> String {
        if let n = payloadNumber(payload, key: payloadKey) {
            return String(Int(n.rounded()))
        }
        return PersonalLifeOpsDerived.displayScore(fallback)
    }

    static func bondIndex(pulse: APIClient.PersonalPulsePayload?) -> Int? {
        if let w = PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore) { return Int(w.rounded()) }
        if let r = PersonalLifeOpsDerived.scoreNumber(pulse?.rhythmScore) { return Int(r.rounded()) }
        let keys = ["bondIndex", "trustScore", "careScore", "supportScore", "presenceScore"]
        let nums = keys.compactMap { payloadNumber(pulse?.widgetPayload, key: $0) }
        guard !nums.isEmpty else { return nil }
        return Int((nums.reduce(0, +) / Double(nums.count)).rounded())
    }

    static func bondIndexDisplay(pulse: APIClient.PersonalPulsePayload?) -> String {
        bondIndex(pulse: pulse).map(String.init) ?? "—"
    }

    static func bondAxes(pulse: APIClient.PersonalPulsePayload?) -> BondAxes {
        let payload = pulse?.widgetPayload
        return BondAxes(
            trust: displayFromPayloadOrFallback(payload: payload, payloadKey: "trustScore", fallback: nil),
            care: displayFromPayloadOrFallback(payload: payload, payloadKey: "careScore", fallback: nil),
            support: displayFromPayloadOrFallback(payload: payload, payloadKey: "supportScore", fallback: nil),
            presence: displayFromPayloadOrFallback(payload: payload, payloadKey: "presenceScore", fallback: nil)
        )
    }

    static func bondSubtitle(pulse: APIClient.PersonalPulsePayload?) -> String {
        bondIndex(pulse: pulse) != nil ? "Bond signal present" : "Awaiting bond signals"
    }

    static func spendPairs(pulse: APIClient.PersonalPulsePayload?) -> [(String, String)] {
        guard let map = pulse?.widgetPayload?["spendByCurrency"]?.value as? [String: Any] else { return [] }
        return map.compactMap { key, value in
            let v = "\(value)"
            guard !key.isEmpty, !v.isEmpty else { return nil }
            return (key, v)
        }
    }
}
