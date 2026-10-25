import SwiftUI

struct ProductionActiveView: View {
    let model: OperatingProjectionPayload
    let tab: OperatingTab
    var onOpenQuickAdd: () -> Void = {}
    var onRecord: (ProductionEventDraft) async throws -> Void = { _ in }
    var onDecide: (String, String) async throws -> Void = { _, _ in }
    var momentId: String? = nil

    @State private var activityOpen = false
    private var chrome: ProductionChrome { model.subtype == "MADE_TO_ORDER" ? .job : .standard }

    var body: some View {
        Group {
            if tab == .quickAdd {
                ProductionQuickAddHub(model: model, onRecord: onRecord, onDecide: onDecide)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        switch tab {
                        case .pulse: pulse
                        case .moments: moments
                        case .life: life
                        case .memory: memory
                        case .quickAdd: EmptyView()
                        }
                    }
                    .padding(.horizontal, 17)
                    .padding(.vertical, 20)
                }
            }
        }
        .background(chrome.bg)
        .environment(\.productionChrome, chrome)
        .sheet(isPresented: $activityOpen) { activitySheet }
    }

    private var pulse: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Pulse", model.subtype == "MADE_TO_ORDER"
                   ? "Revenue qualifying delivered/billed facts, approved spend and settlement readiness."
                   : "Sales, spend, collections and approved manufacturing sources.")
            if model.financeVisible == true {
                sectionTitle("Target & Budget")
                card {
                    Text("Toward the monthly target").font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
                    Text(money(model.target?.progress)).font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(chrome.ink)
                    Text("of \(money(model.target?.value)) target").font(.plusJakarta(size: 13)).foregroundStyle(chrome.secondary)
                    if targetOpen {
                        meter("Target progress", fraction(model.target?.progress, model.target?.value), chrome.brand)
                    } else {
                        Text(model.target?.coverage ?? "not recorded").font(.plusJakarta(size: 13)).foregroundStyle(chrome.secondary)
                    }
                    meter("Budget used", fraction(model.budget?.spent, model.budget?.amount), chrome.success)
                    line("Budget remaining", money(model.budget?.remaining), chrome.success)
                    Text("Validated sales count toward a revenue target. Collections do not. Production, orders, and deliveries count only for the measure you chose.")
                        .font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
                }
                sectionTitle("Needs Attention")
                if (model.attention ?? []).isEmpty {
                    card { Text("Nothing is waiting.").foregroundStyle(chrome.secondary) }
                }
                ForEach(model.attention ?? []) { item in
                    card {
                        Text(item.kind).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(chrome.accent)
                        Text(item.title).foregroundStyle(chrome.ink)
                        if item.id.hasPrefix("open-"), let payment = (model.paymentReadiness ?? []).first(where: { $0.id == String(item.id.dropFirst(5)) }) {
                            Button("Open") { OperatingChromeState.shared.settling = payment }
                                .font(.plusJakarta(size: 13, weight: .semibold))
                        }
                        if item.kind == "APPROVAL" && (model.role == "OWNER_ADMIN" || model.role == "APPROVER") {
                            HStack {
                                Button("Approve") { Task { try? await onDecide(item.id, "APPROVED") } }
                                    .foregroundStyle(chrome.success)
                                Button("Reject") { Task { try? await onDecide(item.id, "REJECTED") } }
                                    .foregroundStyle(Color(hex: "#F87171"))
                            }
                        }
                    }
                }
                sectionTitle("Payment Readiness")
                readiness("To collect", (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" })
                readiness("To pay", (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" })
                sectionTitle("Finance")
                card {
                    line("Net sales", money(model.finance?.sales), chrome.accent)
                    line("Received", money(model.finance?.received), chrome.success)
                    line("Still to collect", money(model.finance?.outstanding), Color(hex: "#FBBF24"))
                    line("Eligible cost", money(model.finance?.eligibleCost), chrome.ink)
                    line("Business cash paid", money(model.finance?.cashPaid), chrome.ink)
                    line("Still to pay", money(model.finance?.payable), Color(hex: "#FBBF24"))
                }
            } else {
                Text("Production status, with the work you're allowed to see.")
                    .font(.plusJakarta(size: 14)).foregroundStyle(chrome.secondary)
                card {
                    Text(model.title ?? "Factory").font(.plusJakarta(size: 18, weight: .semibold)).foregroundStyle(chrome.ink)
                    Text((model.role ?? "Assigned work").replacingOccurrences(of: "_", with: " "))
                        .font(.plusJakarta(size: 13)).foregroundStyle(chrome.secondary)
                }
                ForEach(model.attention ?? []) { item in
                    card { Text(item.title).foregroundStyle(chrome.ink) }
                }
                card {
                    Text("Finance access restricted").font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(chrome.ink)
                    Text("Financial records, amounts, and budget history are not available in your current scope.")
                        .font(.plusJakarta(size: 13)).foregroundStyle(chrome.secondary)
                }
            }
            sectionTitle("Activity")
            ForEach(Array((model.activity ?? []).prefix(5))) { row in activityRow(row) }
            if (model.activity ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(chrome.secondary) }
            }
            Button("View all activity") { activityOpen = true }
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(chrome.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(chrome.brand, lineWidth: 1))
        }
    }

    private var moments: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Moments", "Production, dispatch, and the factory this month.")
            card {
                Text(period).font(.plusJakarta(size: 12)).foregroundStyle(chrome.accent)
                Text("This month, in motion").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(chrome.ink)
                HStack {
                    metric("Progress", money(model.target?.progress))
                    metric("Target", money(model.target?.value))
                }
                HStack {
                    metric("Spent", money(model.budget?.spent))
                    metric("Remaining", money(model.budget?.remaining))
                }
                if targetOpen {
                    meter("Toward the monthly target", fraction(model.target?.progress, model.target?.value), chrome.brand)
                } else {
                    Text(model.target?.coverage ?? "not recorded").font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
                }
            }
            ForEach(model.boards?.moments ?? [], id: \.code) { section in
                board(section)
            }
            card {
                Text("Capture the next manufacturing event").font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(chrome.ink)
                Button("Open Quick Add", action: onOpenQuickAdd)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(chrome.bg)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(chrome.brand)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var life: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Life", "How production is moving today.")
            ForEach(model.boards?.life ?? []) { board($0) }
            if (model.boards?.life ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(chrome.secondary) }
            }
        }
    }

    private var memory: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Memory", "What this factory keeps from the month.")
            HStack {
                pill(period)
                pill("Current")
            }
            ForEach(model.boards?.memory ?? []) { board($0) }
            Button("Preview story") {
                if let momentId { OperatingChromeState.shared.previewStoryId = momentId }
            }
            .font(.plusJakarta(size: 15, weight: .semibold))
            .foregroundStyle(chrome.bg)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(chrome.brand)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    private var activitySheet: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("All activity").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(chrome.ink)
                Text(model.title ?? "Factory").foregroundStyle(chrome.secondary)
                ForEach(model.activity ?? []) { activityRow($0) }
                if (model.activity ?? []).isEmpty {
                    Text("Nothing recorded yet.").foregroundStyle(chrome.secondary)
                }
            }
            .padding(18)
        }
        .background(chrome.bg)
    }

    private func header(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.plusJakarta(size: 32, weight: .semibold)).foregroundStyle(chrome.ink)
                Spacer()
                pill(roleLabel(model.role))
            }
            Text(body).font(.plusJakarta(size: 14)).foregroundStyle(chrome.secondary)
            Text(snapshot).font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
        }
    }

    private func board(_ section: OperatingBoardSectionPayload) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle(section.title)
            card {
                Text(section.detail.isEmpty ? "Nothing recorded yet." : formatOperatingDetail(section.detail, currency: model.currency))
                    .font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
                if let coverage = section.coverage, !coverage.isEmpty {
                    Text(coverage).font(.plusJakarta(size: 12)).foregroundStyle(chrome.accent)
                }
                ForEach(section.rows ?? []) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).foregroundStyle(chrome.ink)
                        let extra = [row.channelName, row.counterpartyName, row.paymentStatus].compactMap { $0 }.filter { $0 != "NONE" }
                        if !extra.isEmpty {
                            Text(extra.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
                        }
                        if model.financeVisible == true, let amount = row.amount, !amount.isEmpty {
                            Text(money(amount)).font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(chrome.accent)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder private func channelTotals(_ title: String, _ rows: [OperatingActivityPayload]) -> some View {
        let grouped = Dictionary(grouping: rows.filter { $0.approvalStatus != "REJECTED" && $0.approvalStatus != "PENDING" }) {
            ($0.channelName?.isEmpty == false ? $0.channelName : nil) ?? "Unassigned"
        }
        if !grouped.isEmpty {
            Text(title).font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
            ForEach(grouped.keys.sorted(), id: \.self) { channel in
                let total = (grouped[channel] ?? []).compactMap { Double($0.amount ?? "") }.reduce(0, +)
                line(channel, money(String(total)), chrome.ink)
            }
        }
    }

    private func activityRow(_ row: OperatingActivityPayload) -> some View {
        card {
            Text(row.eventType.replacingOccurrences(of: "_", with: " "))
                .font(.plusJakarta(size: 11)).foregroundStyle(chrome.accent)
            Text(row.title).foregroundStyle(chrome.ink)
            let qty = [row.quantity, row.unit].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " ")
            let bits = [row.counterpartyName, qty.isEmpty ? nil : qty, row.paymentStatus == "NONE" ? nil : row.paymentStatus, String(row.occurredAt.prefix(10)), model.financeVisible == true ? money(row.amount) : nil].compactMap { $0 }.filter { !$0.isEmpty && $0 != "—" }
            if !bits.isEmpty {
                Text(bits.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
            }
            Button("Edit") { OperatingChromeState.shared.editing = row }
                .font(.plusJakarta(size: 13, weight: .semibold))
        }
    }

    @ViewBuilder private func readiness(_ label: String, _ rows: [OperatingPaymentPayload]) -> some View {
        if !rows.isEmpty {
            card {
                Text(label).font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(chrome.accent)
                ForEach(rows) { item in
                    Button { OperatingChromeState.shared.settling = item } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            line(item.title, money(item.amount), chrome.ink)
                            Text([item.counterpartyName, item.dueAt.map { String($0.prefix(10)) }].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "))
                                .font(.plusJakarta(size: 12))
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8, content: content)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(chrome.card)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(chrome.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text).font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(chrome.ink)
    }

    private func pill(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(chrome.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(chrome.notice)
            .clipShape(Capsule())
    }

    private func metric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.plusJakarta(size: 11)).foregroundStyle(chrome.secondary)
            Text(value).font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(chrome.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(chrome.notice)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func meter(_ label: String, _ fraction: Double, _ color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(chrome.secondary)
                Spacer()
                Text("\(Int(fraction * 100))%").font(.plusJakarta(size: 12)).foregroundStyle(color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(chrome.border)
                    Capsule().fill(color).frame(width: geo.size.width * min(max(fraction, 0), 1))
                }
            }
            .frame(height: 6)
        }
    }

    private func line(_ label: String, _ value: String, _ color: Color) -> some View {
        HStack {
            Text(label).foregroundStyle(chrome.ink)
            Spacer()
            Text(value).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(color)
        }
    }

    private var targetOpen: Bool {
        let coverage = model.target?.coverage ?? ""
        return coverage.isEmpty || coverage == "null"
    }

    private var period: String {
        let raw = model.startDate ?? ""
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let date = formatter.date(from: String(raw.prefix(10))) ?? Date()
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: date)
    }

    private var snapshot: String {
        let today = DateFormatter()
        today.locale = Locale(identifier: "en_US_POSIX")
        today.dateFormat = "dd MMM yyyy"
        let from = model.startDate?.prefix(10).description
        let until = model.endDate?.prefix(10).description
        if let until, !until.isEmpty { return "Snapshot · \(from?.isEmpty == false ? from! : today.string(from: Date())) – \(until)" }
        return "Snapshot · from \(from?.isEmpty == false ? from! : today.string(from: Date())) · \(today.string(from: Date()))"
    }

    private func money(_ raw: String?) -> String {
        formatOperatingAmount(raw, currency: model.currency, visible: model.financeVisible == true)
    }

    private func fraction(_ part: String?, _ whole: String?) -> Double {
        let p = Double(part ?? "") ?? 0
        let w = Double(whole ?? "") ?? 0
        return w > 0 ? p / w : 0
    }

    private func roleLabel(_ code: String?) -> String {
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? "Production"
    }
}

struct ProductionChrome {
    let bg: Color
    let card: Color
    let notice: Color
    let field: Color
    let border: Color
    let brand: Color
    let accent: Color
    let secondary: Color
    let ink: Color
    let success: Color

    static let standard = ProductionChrome(
        bg: Color(hex: "#09121F"), card: Color(hex: "#122235"), notice: Color(hex: "#16405C"),
        field: Color(hex: "#09121F"), border: Color(hex: "#2B4B63"), brand: Color(hex: "#6DDCFF"),
        accent: Color(hex: "#6DDCFF"), secondary: Color(hex: "#A5BBCE"), ink: Color(hex: "#E5E0EE"),
        success: Color(hex: "#8FD6FF")
    )
    static let job = ProductionChrome(
        bg: Color(hex: "#071014"), card: Color(hex: "#15262C"), notice: Color(hex: "#15262C"),
        field: Color(hex: "#071014"), border: Color(hex: "#345058"), brand: Color(hex: "#66C7A4"),
        accent: Color(hex: "#66C7A4"), secondary: Color(hex: "#AFC1C3"), ink: Color(hex: "#EFF5F2"),
        success: Color(hex: "#66C7A4")
    )
}

private struct ProductionChromeKey: EnvironmentKey {
    static let defaultValue = ProductionChrome.standard
}

extension EnvironmentValues {
    var productionChrome: ProductionChrome {
        get { self[ProductionChromeKey.self] }
        set { self[ProductionChromeKey.self] = newValue }
    }
}

struct ProductionEventDraft {
    var eventType: String
    var title: String
    var amount: String?
    var note: String?
    var channelName: String?
    var counterpartyName: String?
    var paymentStatus: String?
    var occurredAt: String? = nil
    var quantity: String? = nil
    var unit: String? = nil
    var unitPrice: String? = nil
    var itemName: String? = nil
    var parentEventId: String? = nil
    var revisesEventId: String? = nil
        var masterKind: String? = nil
    var dueAt: String? = nil
    var contract: OperatingContractInput? = nil
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil
    var rejectedQuantity: String? = nil
}