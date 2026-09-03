import Foundation

/// Lifestyle Vitality Index + axis scores from pulse + widgetPayload (Figma 505:12365).
enum PersonalLifestyleDerived {
    struct AxisScores {
        let joy: String
        let fulfillment: String
        let vitality: String
        let exploration: String
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

    static func axisScores(pulse: APIClient.PersonalPulsePayload?) -> AxisScores {
        let payload = pulse?.widgetPayload
        return AxisScores(
            joy: displayFromPayloadOrFallback(payload: payload, payloadKey: "joyScore", fallback: pulse?.recoveryScore),
            fulfillment: displayFromPayloadOrFallback(payload: payload, payloadKey: "fulfillmentScore", fallback: pulse?.wellbeingScore),
            vitality: displayFromPayloadOrFallback(payload: payload, payloadKey: "vitalityScore", fallback: pulse?.rhythmScore),
            exploration: displayFromPayloadOrFallback(
                payload: payload,
                payloadKey: "explorationScore",
                fallback: PersonalLifeOpsDerived.attentionDisplay(count: pulse?.attentionCount)
            )
        )
    }

    static func vitalityIndex(pulse: APIClient.PersonalPulsePayload?) -> Int? {
        let axes = axisScores(pulse: pulse)
        let nums = [axes.joy, axes.fulfillment, axes.vitality, axes.exploration]
            .compactMap { PersonalLifeOpsDerived.scoreNumber($0) }
        if nums.isEmpty {
            return PersonalLifeOpsDerived.scoreNumber(pulse?.wellbeingScore).map { Int($0.rounded()) }
        }
        return Int((nums.reduce(0, +) / Double(nums.count)).rounded())
    }

    static func vitalityIndexDisplay(pulse: APIClient.PersonalPulsePayload?) -> String {
        vitalityIndex(pulse: pulse).map(String.init) ?? "—"
    }

    static func networkStability(pulse: APIClient.PersonalPulsePayload?) -> String {
        guard let index = vitalityIndex(pulse: pulse) else { return "Awaiting first signals" }
        if index >= 75 { return "Flourishing" }
        if index >= 50 { return "Growing" }
        return "Building"
    }

    static func experienceCount(pulse: APIClient.PersonalPulsePayload?) -> Int {
        Int(payloadNumber(pulse?.widgetPayload, key: "experienceCount") ?? 0)
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
