import SwiftUI

struct OperatingSubtype: Identifiable {
    let code: String
    let label: String
    let partyLabel: String
    let targetMetrics: [String]
    let requiresChannel: Bool
    let quickAdds: [String]
    var id: String { code }
}

enum OperatingMomentCatalog {
    static let roles: [(String, String)] = [
        ("OWNER_ADMIN", "Owner / Admin"),
        ("OPERATIONS_LEAD", "Operations Lead"),
        ("FINANCE", "Finance"),
        ("APPROVER", "Approver"),
        ("CONTRIBUTOR", "Contributor"),
        ("TEAM_MEMBER", "Team Member"),
        ("OBSERVER", "Observer"),
    ]

    private static let marketingAdds = [
        "ACTIVITY", "PROGRESS", "SPEND", "PAYMENT", "REVISION", "APPROVAL", "UPDATE", "ISSUE", "RESULT",
    ]

    static func subtypes(_ kind: BusinessSetupKind) -> [OperatingSubtype] {
        switch kind {
        case .marketingCampaigns:
            return [
                OperatingSubtype(code: "CLIENT_CAMPAIGN", label: "Client Campaign", partyLabel: "Client", targetMetrics: ["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT", "CLIENT_BILLING"], requiresChannel: false, quickAdds: marketingAdds + ["INVOICE", "COLLECTION"]),
                OperatingSubtype(code: "OWN_BUSINESS_CAMPAIGN", label: "Own Business Campaign", partyLabel: "Brand", targetMetrics: ["LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"], requiresChannel: false, quickAdds: marketingAdds),
            ]
        case .tradingDistribution:
            return [
                OperatingSubtype(code: "TRADING_WHOLESALE", label: "Trading / Wholesale", partyLabel: "Business", targetMetrics: ["REVENUE", "ORDERS", "CUSTOM_COUNT"], requiresChannel: false, quickAdds: ["SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER", "SHIPMENT"]),
                OperatingSubtype(code: "DISTRIBUTION_DEALERSHIP", label: "Distribution / Dealership", partyLabel: "Principal", targetMetrics: ["REVENUE", "ORDERS", "CUSTOM_COUNT"], requiresChannel: false, quickAdds: ["SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER", "SHIPMENT"]),
            ]
        case .retailConsumer:
            return [
                OperatingSubtype(code: "RETAIL_STORE", label: "Retail Store", partyLabel: "Store", targetMetrics: ["REVENUE", "ORDERS", "CUSTOM_COUNT"], requiresChannel: true, quickAdds: ["SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER"]),
                OperatingSubtype(code: "RESTAURANT_FOOD", label: "Restaurant / Food", partyLabel: "Outlet", targetMetrics: ["REVENUE", "ORDERS", "CUSTOM_COUNT"], requiresChannel: true, quickAdds: ["SALE", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK", "ORDER"]),
                OperatingSubtype(code: "CONSUMER_SERVICE", label: "Consumer Service", partyLabel: "Service", targetMetrics: ["REVENUE", "ORDERS", "CUSTOM_COUNT"], requiresChannel: true, quickAdds: ["SERVICE", "APPOINTMENT", "PURCHASE", "SPEND", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK"]),
            ]
        case .manufacturingProduction:
            return [
                OperatingSubtype(code: "STANDARD_PRODUCTION", label: "Standard Production", partyLabel: "Plant", targetMetrics: ["REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"], requiresChannel: false, quickAdds: ["PRODUCTION", "PURCHASE", "SPEND", "SALE", "CUSTOMER_ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK"]),
                OperatingSubtype(code: "MADE_TO_ORDER", label: "Made-to-Order", partyLabel: "Workshop", targetMetrics: ["REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"], requiresChannel: false, quickAdds: ["PRODUCTION", "PURCHASE", "SPEND", "SALE", "CUSTOMER_ORDER", "DELIVERY", "PAYMENT", "COLLECTION", "BUDGET", "TARGET", "UPDATE", "ISSUE", "APPROVAL", "STOCK"]),
            ]
        }
    }

    static func kind(for code: String?) -> BusinessSetupKind? {
        let c = (code ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if c.isEmpty { return nil }
        if let kind = BusinessSetupKind(rawValue: c) { return kind }
        return BusinessSetupKind.allCases.first { subtypes($0).contains { $0.code == c } }
    }

    static func isRetired(_ code: String?) -> Bool {
        let c = (code ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if c.isEmpty || kind(for: c) != nil { return false }
        return c.contains("TEAM_OPERATIONS") || c.contains("RUNWAY") || (c.contains("OPERATIONS") && !c.contains("TEAM"))
    }

    static let preferenceKeys: Set<String> = [
        "subtype", "identityName", "partyName", "description", "currency", "startDate", "location", "activationPolicy",
        "participants", "counterparties", "channels", "tradeReach", "monthlyBudget", "monthlyTarget", "targetMetric",
    ]

    static func label(_ code: String) -> String {
        switch code {
        case "SPEND": return "Add Spend"
        case "RESULT": return "Add Result"
        case "PAYMENT": return "Add Payment"
        case "COLLECTION": return "Add Collection"
        case "BUDGET": return "Revise Budget"
        case "TARGET": return "Revise Target"
        case "UPDATE": return "Add Update"
        case "ISSUE": return "Add Issue"
        case "APPROVAL": return "Request Approval"
        case "DELIVERABLE": return "Add Deliverable"
        case "VENDOR_COST": return "Add Vendor Cost"
        case "COMPLETE": return "Mark Complete"
        case "SALE": return "Record Sale"
        case "PURCHASE": return "Record Purchase"
        case "STOCK": return "Stock Update"
        case "ORDER": return "Order Update"
        case "SHIPMENT": return "Shipment Update"
        case "SERVICE": return "Record Service"
        case "APPOINTMENT": return "Appointment / Job"
        case "PRODUCTION": return "Record Production"
        case "CUSTOMER_ORDER": return "Customer Order"
        case "DELIVERY": return "Record Delivery"
        default: return code
        }
    }
}

enum OperatingTab {
    case pulse, moments, life, memory, quickAdd
}

/// Last operating projection for a moment. Paint this before the ledger replay returns.
enum OperatingProjectionCache {
    private static var stored: [String: OperatingProjectionPayload] = [:]

    static func peek(_ momentId: String) -> OperatingProjectionPayload? {
        stored[momentId]
    }

    static func put(_ momentId: String, _ payload: OperatingProjectionPayload) {
        stored[momentId] = payload
    }
}

struct OperatingMomentPage: View {
    let momentId: String?
    let momentTypeCode: String?
    let momentTitle: String?
    let tab: OperatingTab
    var refreshToken: Int = 0
    var onCreateMoment: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var projection: OperatingProjectionPayload?
    @State private var errorText: String?
    @State private var tick = 0
    @ObservedObject private var chrome = OperatingChromeState.shared

    var body: some View {
        Group {
            if OperatingMomentCatalog.isRetired(momentTypeCode) || momentId == nil {
                retired
            } else if projection?.retired == true {
                retired
            } else {
                content
            }
        }
        .task(id: "\(momentId ?? "")-\(refreshToken)-\(tick)") { await load() }
        .sheet(item: Binding(get: { chrome.editing }, set: { chrome.editing = $0 })) { row in
            if let projection {
                OperatingCaptureSheet(
                    title: "Edit activity",
                    icon: "qa_message",
                    eventType: row.eventType,
                    mode: row.eventType,
                    model: projection,
                    seed: row,
                    onSubmit: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: row.id
                        )
                        chrome.editing = nil
                        tick += 1
                    }
                )
            }
        }
        .background {
            Color.clear.sheet(item: Binding(get: { chrome.settling }, set: { chrome.settling = $0 })) { row in
            if let projection {
                let collect = row.relevance == "RECEIVABLE"
                OperatingCaptureSheet(
                    title: collect ? "Record Collection" : "Record Payment",
                    icon: "qa_shield",
                    eventType: collect ? "COLLECTION" : "PAYMENT",
                    mode: collect ? "COLLECTION" : "PAYMENT",
                    model: projection,
                    seed: OperatingActivityPayload(
                        id: "",
                        title: row.title,
                        eventType: collect ? "COLLECTION" : "PAYMENT",
                        occurredAt: "",
                        amount: row.amount,
                        channelName: nil,
                        counterpartyName: row.counterpartyName,
                        paymentStatus: nil,
                        approvalStatus: nil,
                        note: nil,
                        quantity: nil,
                        unit: nil,
                        parentEventId: row.id,
                        status: nil,
                        orderSide: nil,
                        estimatedAmount: nil
                    ),
                    onSubmit: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId ?? row.id,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: nil
                        )
                        chrome.settling = nil
                        tick += 1
                    }
                )
            }
            }
        }
    }

    private var retired: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(momentTitle ?? "Business moment").font(.title2.weight(.semibold)).foregroundStyle(.white)
            Text("Team & Work, Money & Cash Flow, and Daily Business are retired. Create a Marketing, Trading, Retail, or Manufacturing moment.")
                .foregroundStyle(Color(hex: "#94A3B8"))
            Button("Create a moment", action: onCreateMoment)
                .buttonStyle(.borderedProminent)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Color(hex: "#0C0F15"))
    }

    private var content: some View {
        Group {
            if let projection, projection.subtype == "RETAIL_STORE" {
                RetailStoreActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    },
                    momentId: momentId
                )
            } else if let projection, projection.subtype == "RESTAURANT_FOOD" {
                RestaurantActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    }
                )
            } else if let projection, projection.subtype == "CLIENT_CAMPAIGN" || projection.subtype == "OWN_BUSINESS_CAMPAIGN" {
                MarketingActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems,
                            reportingMode: draft.reportingMode,
                            metricCode: draft.metricCode,
                            budgetCategory: draft.budgetCategory,
                            activityStatus: draft.activityStatus,
                            attributedSales: draft.attributedSales,
                            revisionKind: draft.revisionKind
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    },
                    momentId: momentId
                )
            } else if let projection, projection.subtype == "STANDARD_PRODUCTION" || projection.subtype == "MADE_TO_ORDER" {
                ProductionActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems,
                            rejectedQuantity: draft.rejectedQuantity
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    },
                    momentId: momentId
                )
            } else if let projection, projection.subtype == "TRADING_WHOLESALE" {
                TradingActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            conversionToBase: draft.conversionToBase,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    }
                )
            } else if let projection, projection.subtype == "DISTRIBUTION_DEALERSHIP" {
                DistributionActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            conversionToBase: draft.conversionToBase,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    }
                )
            } else if let projection, projection.subtype == "CONSUMER_SERVICE" {
                ConsumerServiceActiveView(
                    model: projection,
                    tab: tab,
                    onOpenQuickAdd: onOpenQuickAdd,
                    onRecord: { draft in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.recordOperatingEvent(
                            momentId: momentId,
                            eventType: draft.eventType,
                            title: draft.title,
                            amount: draft.amount,
                            note: draft.note,
                            channelName: draft.channelName,
                            counterpartyName: draft.counterpartyName,
                            paymentStatus: draft.paymentStatus,
                            occurredAt: draft.occurredAt,
                            parentEventId: draft.parentEventId,
                            quantity: draft.quantity,
                            unit: draft.unit,
                            itemName: draft.itemName,
                            unitPrice: draft.unitPrice,
                            masterKind: draft.masterKind,
                            revisesEventId: draft.revisesEventId,
                            dueAt: draft.dueAt,
                            contract: draft.contract,
                            lineItems: draft.lineItems
                        )
                        tick += 1
                    },
                    onDecide: { eventId, decision in
                        guard let momentId else { return }
                        _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: decision)
                        tick += 1
                    },
                    momentId: momentId
                )
            } else {
                generic
            }
        }
    }

    private var generic: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(familyKicker(projection?.familyCode))
                    .font(.caption.weight(.medium)).foregroundStyle(familyAccent(projection?.familyCode))
                Text(projection?.subtypeLabel ?? momentTitle ?? "Business moment")
                    .font(.title2.weight(.semibold)).foregroundStyle(.white)
                if let party = projection?.partyName, !party.isEmpty {
                    Text(party).foregroundStyle(Color(hex: "#94A3B8"))
                }
                if let errorText, projection == nil {
                    Text(errorText).foregroundStyle(Color(hex: "#F87171"))
                }
                switch tab {
                case .pulse: pulse
                case .moments: board(projection?.boards?.moments, momentsHeading(projection?.familyCode))
                case .life: board(projection?.boards?.life, "How this moment connects")
                case .memory: board(projection?.boards?.memory, "What stays")
                case .quickAdd: quickAdd
                }
            }
            .padding(20)
        }
        .background(Color(hex: "#0C0F15"))
    }

    @ViewBuilder private var pulse: some View {
        if projection?.financeVisible == true, let budget = projection?.budget {
            card("Budget", "\(budget.remaining) left of \(budget.amount)")
            if let target = projection?.target {
                card("Target", "\(target.progress) of \(target.value) \(target.metric)")
            }
            ForEach(projection?.paymentReadiness ?? [], id: \.id) { item in
                card("Payment Readiness", "\(item.title) · \(item.amount) · \(item.status)")
            }
        } else {
            card("Your view", "Finance totals stay with Finance, Approver, and Owner.")
        }
        if (projection?.attention ?? []).isEmpty {
            card("Needs Attention", "Nothing is waiting.")
        }
        ForEach(projection?.attention ?? [], id: \.id) { item in
            card(item.kind, item.title)
        }
        let recent = Array((projection?.activity ?? []).prefix(5))
        if !recent.isEmpty {
            Text("Activity").font(.headline).foregroundStyle(.white)
            ForEach(recent) { row in
                card(row.eventType, row.title + (row.amount.map { " · \($0)" } ?? ""))
            }
        }
    }

    @ViewBuilder private func board(_ sections: [OperatingBoardSectionPayload]?, _ heading: String) -> some View {
        Text(heading).font(.headline).foregroundStyle(.white)
        if sections?.isEmpty != false {
            card("This moment", "Nothing recorded yet.")
        }
        ForEach(sections ?? []) { section in
            card(section.title, section.detail.isEmpty ? "Nothing recorded yet." : section.detail)
            ForEach(section.rows ?? []) { row in
                card(row.eventType.replacingOccurrences(of: "_", with: " "), row.title + (row.amount.map { " · \($0)" } ?? ""))
            }
        }
    }

    @ViewBuilder private var quickAdd: some View {
        OperatingQuickAddForm(
            adds: projection?.quickAdds ?? [],
            pending: (projection?.attention ?? []).filter { $0.kind == "APPROVAL" },
            onSubmit: { code, title, amount in
                guard let momentId else { return }
                _ = try await APIClient.shared.recordOperatingEvent(momentId: momentId, eventType: code, title: title, amount: amount)
                tick += 1
            },
            onDecide: { eventId in
                guard let momentId else { return }
                _ = try await APIClient.shared.decideOperatingEvent(momentId: momentId, eventId: eventId, decision: "APPROVED")
                tick += 1
            }
        )
    }

    private func card(_ label: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(Color(hex: "#94A3B8"))
            Text(body).foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Color(hex: "#161B26"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func familyAccent(_ family: String?) -> Color {
        switch family {
        case "TRADING_DISTRIBUTION": return Color(hex: "#FBBF24")
        case "MARKETING_CAMPAIGNS": return Color(hex: "#818CF8")
        case "MANUFACTURING_PRODUCTION": return Color(hex: "#38BDF8")
        default: return Color(hex: "#34D399")
        }
    }

    private func familyKicker(_ family: String?) -> String {
        switch family {
        case "TRADING_DISTRIBUTION": return "TRADING & DISTRIBUTION"
        case "MARKETING_CAMPAIGNS": return "MARKETING & CAMPAIGNS"
        case "MANUFACTURING_PRODUCTION": return "MANUFACTURING & PRODUCTION"
        default: return "RETAIL & CONSUMER"
        }
    }

    private func momentsHeading(_ family: String?) -> String {
        switch family {
        case "TRADING_DISTRIBUTION": return "Sales, purchases, and movement"
        case "MARKETING_CAMPAIGNS": return "Spend, results, and delivery"
        case "MANUFACTURING_PRODUCTION": return "Production, orders, and delivery"
        default: return "Sales, stock, and service"
        }
    }

    private func load() async {
        guard let momentId, !OperatingMomentCatalog.isRetired(momentTypeCode) else { return }
        if projection == nil, let cached = OperatingProjectionCache.peek(momentId) {
            projection = cached
            chrome.participantNames = cached.participants ?? []
        }
        do {
            let fresh = try await APIClient.shared.getOperatingMoment(momentId: momentId)
            OperatingProjectionCache.put(momentId, fresh)
            projection = fresh
            chrome.participantNames = fresh.participants ?? []
            errorText = nil
        } catch is CancellationError {
            return
        } catch {
            if projection == nil {
                errorText = error.localizedDescription
            }
        }
    }
}

private struct OperatingQuickAddForm: View {
    let adds: [OperatingQuickAddPayload]
    let pending: [OperatingAttentionPayload]
    let onSubmit: (String, String, String?) async throws -> Void
    let onDecide: (String) async throws -> Void
    @State private var selected = ""
    @State private var title = ""
    @State private var amount = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Quick Add").foregroundStyle(.white).font(.headline)
            ForEach(adds, id: \.code) { add in
                Button(add.label) { selected = add.code }
                    .buttonStyle(.bordered)
            }
            TextField("What happened", text: $title).textFieldStyle(.roundedBorder)
            if adds.first(where: { $0.code == selected })?.needsAmount == true {
                TextField("Amount", text: $amount).textFieldStyle(.roundedBorder)
            }
            Button("Add") {
                let code = selected.isEmpty ? (adds.first?.code ?? "") : selected
                let label = title
                let value = amount.isEmpty ? nil : amount
                Task {
                    try? await onSubmit(code, label, value)
                    title = ""
                    amount = ""
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
            ForEach(pending) { item in
                Button("Approve \(item.title)") {
                    Task { try? await onDecide(item.id) }
                }
                .buttonStyle(.bordered)
            }
        }
    }
}
