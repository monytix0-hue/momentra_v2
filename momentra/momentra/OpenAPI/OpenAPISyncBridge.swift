import Foundation
#if canImport(MomentraAPI)
import MomentraAPI

enum OpenAPISyncBridge {
    static let contractVersion = "momentra-v1-generated"

    static var isLinked: Bool { true }
}
#else
enum OpenAPISyncBridge {
    static let contractVersion = "momentra-v1-generated"

    static var isLinked: Bool { false }
}
#endif
