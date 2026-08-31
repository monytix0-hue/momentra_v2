import Foundation

/// Lossy JSON decode helper for widget payloads.
struct AnyDecodable: Decodable {
    let value: Any

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let dict = try? container.decode([String: AnyDecodable].self) {
            value = dict.mapValues { $0.value }
        } else if let arr = try? container.decode([AnyDecodable].self) {
            value = arr.map { $0.value }
        } else if let s = try? container.decode(String.self) {
            value = s
        } else if let i = try? container.decode(Int.self) {
            value = i
        } else if let d = try? container.decode(Double.self) {
            value = d
        } else if let b = try? container.decode(Bool.self) {
            value = b
        } else {
            value = NSNull()
        }
    }
}
