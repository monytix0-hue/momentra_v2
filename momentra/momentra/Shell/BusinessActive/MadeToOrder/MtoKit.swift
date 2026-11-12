import SwiftUI

/// Made-to-Order QuickAdds kit — soft-amber chrome sheets (D2: keep Core·9 hub IA).
enum MtoKit {
    static let tagline = "Quote. Confirm. Make. Invoice."
    static let eyebrow = "MAKE TO ORDER"
}

func mtoMasterTab(from tab: OperatingMasterTab) -> MtoMasterListTab {
    switch tab {
    case .invite: return .invite
    case .principals: return .vendors
    case .catalogue: return .productsBom
    case .locations: return .linesStores
    case .priceLists: return .productsBom
    }
}

func mtoPartCategory(_ master: OperatingMasterPayload) -> String {
    let kind = master.kind.uppercased()
    let name = master.name.lowercased()
    if kind.contains("WIP") || name.contains("wip") { return "WIP" }
    if kind.contains("RAW") || name.contains("raw") { return "Raw" }
    if kind.contains("CONSUM") || name.contains("consum") { return "Consumables" }
    if kind.contains("SPARE") || name.contains("spare") { return "Spares" }
    return "Finished"
}
