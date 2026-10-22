import SwiftUI

struct MarketingActiveView: View {
    let model: OperatingProjectionPayload
    let tab: OperatingTab
    var onOpenQuickAdd: () -> Void = {}
    var onRecord: (MarketingEventDraft) async throws -> Void = { _ in }
    var onDecide: (String, String) async throws -> Void = { _, _ in }
    var momentId: String? = nil

    @State private var activityOpen = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                switch tab {
                case .pulse: pulse
                case .moments: moments
                case .life: life
                case .memory: memory
                case .quickAdd: MarketingQuickAddHub(model: model, onRecord: onRecord, onDecide: onDecide)
                }
            }
            .padding(.horizontal, 17)
            .padding(.vertical, 20)
        }
        .background(MarketingChrome.bg)
        .sheet(isPresented: $activityOpen) { activitySheet }
    }

    private var pulse: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Pulse", model.subtype == "OWN_BUSINESS_CAMPAIGN"
                   ? "Your campaign plan, approved obligations and ready money."
                   : "Your campaign revenue plan, approved spend and ready money.")
            if model.financeVisible == true {
                sectionTitle("Target & Budget")
                card {
                    Text("Accepted results").font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                    let measured = (model.activity ?? []).contains { $0.eventType == "RESULT" || $0.eventType == "PROGRESS" }
                    Text(measured ? money(model.target?.progress) : "N/A").font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                    Text("of \(money(model.target?.value)) target").font(.plusJakarta(size: 13)).foregroundStyle(MarketingChrome.secondary)
                    if measured {
                        meter("Results toward target", fraction(model.target?.progress, model.target?.value), MarketingChrome.brand)
                    }
                    channelTotals("Results by channel", (model.activity ?? []).filter { $0.eventType == "RESULT" || $0.eventType == "PROGRESS" })
                    meter("Budget used", fraction(model.budget?.spent, model.budget?.amount), MarketingChrome.success)
                    channelTotals("Spend by channel", (model.activity ?? []).filter { $0.eventType == "SPEND" || $0.eventType == "VENDOR_COST" })
                    line("Budget remaining", money(model.budget?.remaining), MarketingChrome.success)
                    Text("Accepted results count toward the target. Money collected does not. Spend and vendor costs count once toward the budget.")
                        .font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                }
                sectionTitle("Needs Attention")
                if (model.attention ?? []).isEmpty {
                    card { Text("Nothing is waiting.").foregroundStyle(MarketingChrome.secondary) }
                }
                ForEach(model.attention ?? []) { item in
                    card {
                        Text(item.kind).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(MarketingChrome.accent)
                        Text(item.title).foregroundStyle(MarketingChrome.ink)
                        if item.id.hasPrefix("open-"), let payment = (model.paymentReadiness ?? []).first(where: { $0.id == String(item.id.dropFirst(5)) }) {
                            Button("Open") { OperatingChromeState.shared.settling = payment }
                                .font(.plusJakarta(size: 13, weight: .semibold))
                        } else if item.kind == "ATT-03" || item.kind == "APPROVAL" || item.cta == "approval", model.role == "OWNER_ADMIN" || model.role == "APPROVER" {
                            HStack {
                                Button("Approve") { Task { try? await onDecide(item.id, "APPROVED") } }
                                    .foregroundStyle(MarketingChrome.success)
                                Button("Reject") { Task { try? await onDecide(item.id, "REJECTED") } }
                                    .foregroundStyle(Color(hex: "#F87171"))
                            }
                        } else if item.cta != nil {
                            Button(operatingAttentionCtaLabel(item)) {
                                openOperatingAttention(item, model: model, onOpenQuickAdd: onOpenQuickAdd)
                            }
                            .font(.plusJakarta(size: 13, weight: .semibold))
                        }
                    }
                }
                sectionTitle("Payment Readiness")
                if model.subtype == "CLIENT_CAMPAIGN" {
                    readiness("To collect", (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" })
                }
                readiness("To pay", (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" })
                sectionTitle("Finance")
                card {
                    let measured = (model.activity ?? []).contains { $0.eventType == "RESULT" || $0.eventType == "PROGRESS" }
                    line("Accepted results", measured ? money(model.target?.progress) : "N/A", MarketingChrome.accent)
                    if model.subtype == "CLIENT_CAMPAIGN" {
                        line("Client invoices", money(model.finance?.sales), MarketingChrome.accent)
                        line("Received", money(model.finance?.received), MarketingChrome.success)
                        line("Still to collect", money(model.finance?.outstanding), Color(hex: "#FBBF24"))
                    }
                    line("Eligible cost", money(model.finance?.eligibleCost), MarketingChrome.ink)
                    line("Business cash paid", money(model.finance?.cashPaid), MarketingChrome.ink)
                    line("Still to pay", money(model.finance?.payable), Color(hex: "#FBBF24"))
                }
            } else {
                Text("The campaign today, with the work you're allowed to see.")
                    .font(.plusJakarta(size: 14)).foregroundStyle(MarketingChrome.secondary)
                card {
                    Text(model.title ?? "Campaign").font(.plusJakarta(size: 18, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                    let channels = (model.life?.channels ?? []).joined(separator: " · ")
                    if !channels.isEmpty {
                        Text("Channels · \(channels)").font(.plusJakarta(size: 13)).foregroundStyle(MarketingChrome.secondary)
                    }
                    Text("Financial totals stay hidden in this view.")
                        .font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                }
                ForEach(model.attention ?? []) { item in
                    card { Text(item.title).foregroundStyle(MarketingChrome.ink) }
                }
                card {
                    Text("Finance access restricted").font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                    Text("Financial records, amounts, and budget history are not available in your current scope.")
                        .font(.plusJakarta(size: 13)).foregroundStyle(MarketingChrome.secondary)
                }
            }
            sectionTitle("Activity")
            ForEach(Array((model.activity ?? []).prefix(5))) { row in activityRow(row) }
            if (model.activity ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(MarketingChrome.secondary) }
            }
            Button("View all activity") { activityOpen = true }
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(MarketingChrome.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(MarketingChrome.brand, lineWidth: 1))
        }
    }

    private var moments: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Moments", "Deliverables, results, and the campaign this month.")
            card {
                Text(period).font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.accent)
                Text("This month, in motion").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                HStack {
                    metric("Results", money(model.target?.progress))
                    metric("Target", money(model.target?.value))
                }
                HStack {
                    metric("Spent", money(model.budget?.spent))
                    metric("Remaining", money(model.budget?.remaining))
                }
                meter("Results toward target", fraction(model.target?.progress, model.target?.value), MarketingChrome.brand)
            }
            ForEach(model.boards?.moments ?? [], id: \.code) { section in
                board(section)
            }
            card {
                Text("Create the next campaign moment").font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                Button("Open Quick Add", action: onOpenQuickAdd)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(MarketingChrome.bg)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(MarketingChrome.brand)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var life: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Life", "How the campaign is moving today.")
            ForEach(model.boards?.life ?? []) { board($0) }
            if (model.boards?.life ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(MarketingChrome.secondary) }
            }
        }
    }

    private var memory: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Memory", "What this campaign keeps from the month.")
            Button("Preview story") {
                if let momentId { OperatingChromeState.shared.previewStoryId = momentId }
            }
            .font(.plusJakarta(size: 14, weight: .semibold))
            .foregroundStyle(MarketingChrome.bg)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(MarketingChrome.brand)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            HStack {
                pill(period)
                pill("Current")
            }
            ForEach(model.boards?.memory ?? []) { board($0) }
        }
    }

    private var activitySheet: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("All activity").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                Text(model.title ?? "Campaign").foregroundStyle(MarketingChrome.secondary)
                ForEach(model.activity ?? []) { activityRow($0) }
                if (model.activity ?? []).isEmpty {
                    Text("Nothing recorded yet.").foregroundStyle(MarketingChrome.secondary)
                }
            }
            .padding(18)
        }
        .background(MarketingChrome.bg)
    }

    private func header(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.plusJakarta(size: 32, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
                Spacer()
                pill(roleLabel(model.role))
            }
            Text(body).font(.plusJakarta(size: 14)).foregroundStyle(MarketingChrome.secondary)
            Text(snapshot).font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
        }
    }

    private func board(_ section: OperatingBoardSectionPayload) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle(section.title)
            card {
                Text(section.detail.isEmpty ? "Nothing recorded yet." : formatOperatingDetail(section.detail, currency: model.currency))
                    .font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                if let coverage = section.coverage, !coverage.isEmpty {
                    Text(coverage).font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(MarketingChrome.accent)
                }
                ForEach(section.rows ?? []) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).foregroundStyle(MarketingChrome.ink)
                        let extra = [row.channelName, row.counterpartyName, row.paymentStatus].compactMap { $0 }.filter { $0 != "NONE" }
                        if !extra.isEmpty {
                            Text(extra.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                        }
                        if model.financeVisible == true, let amount = row.amount, !amount.isEmpty {
                            Text(money(amount)).font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(MarketingChrome.accent)
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
            Text(title).font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
            ForEach(grouped.keys.sorted(), id: \.self) { channel in
                let total = (grouped[channel] ?? []).compactMap { Double($0.amount ?? "") }.reduce(0, +)
                line(channel, money(String(total)), MarketingChrome.ink)
            }
        }
    }

    private func activityRow(_ row: OperatingActivityPayload) -> some View {
        card {
            Text(row.eventType.replacingOccurrences(of: "_", with: " "))
                .font(.plusJakarta(size: 11)).foregroundStyle(MarketingChrome.accent)
            Text(row.title).foregroundStyle(MarketingChrome.ink)
            let qty = [row.quantity, row.unit].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " ")
            let bits = [row.counterpartyName, qty.isEmpty ? nil : qty, row.paymentStatus == "NONE" ? nil : row.paymentStatus, String(row.occurredAt.prefix(10)), model.financeVisible == true ? money(row.amount) : nil].compactMap { $0 }.filter { !$0.isEmpty && $0 != "—" }
            if !bits.isEmpty {
                Text(bits.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
            }
            Button("Edit") { OperatingChromeState.shared.editing = row }
                .font(.plusJakarta(size: 13, weight: .semibold))
        }
    }

    @ViewBuilder private func readiness(_ label: String, _ rows: [OperatingPaymentPayload]) -> some View {
        if !rows.isEmpty {
            card {
                Text(label).font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(MarketingChrome.accent)
                ForEach(rows) { item in
                    Button { OperatingChromeState.shared.settling = item } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            line(item.title, money(item.amount), MarketingChrome.ink)
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
            .background(MarketingChrome.card)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(MarketingChrome.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text).font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
    }

    private func pill(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(MarketingChrome.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(MarketingChrome.notice)
            .clipShape(Capsule())
    }

    private func metric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.plusJakarta(size: 11)).foregroundStyle(MarketingChrome.secondary)
            Text(value).font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(MarketingChrome.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(MarketingChrome.notice)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func meter(_ label: String, _ fraction: Double, _ color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(MarketingChrome.secondary)
                Spacer()
                Text("\(Int(fraction * 100))%").font(.plusJakarta(size: 12)).foregroundStyle(color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(MarketingChrome.border)
                    Capsule().fill(color).frame(width: geo.size.width * min(max(fraction, 0), 1))
                }
            }
            .frame(height: 6)
        }
    }

    private func line(_ label: String, _ value: String, _ color: Color) -> some View {
        HStack {
            Text(label).foregroundStyle(MarketingChrome.ink)
            Spacer()
            Text(value).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(color)
        }
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
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? "Marketing"
    }
}

enum MarketingChrome {
    static let bg = Color(hex: "#09121F")
    static let card = Color(hex: "#122235")
    static let notice = Color(hex: "#16405C")
    static let field = Color(hex: "#09121F")
    static let border = Color(hex: "#2B4B63")
    static let brand = Color(hex: "#6DDCFF")
    static let accent = Color(hex: "#6DDCFF")
    static let secondary = Color(hex: "#A5BBCE")
    static let ink = Color(hex: "#E5E0EE")
    static let success = Color(hex: "#8FD6FF")
}

struct MarketingEventDraft {
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
    var reportingMode: String? = nil
    var metricCode: String? = nil
    var budgetCategory: String? = nil
    var activityStatus: String? = nil
    var attributedSales: String? = nil
    var revisionKind: String? = nil
}