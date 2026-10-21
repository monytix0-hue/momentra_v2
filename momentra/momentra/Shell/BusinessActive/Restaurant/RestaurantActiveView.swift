import SwiftUI

struct RestaurantActiveView: View {
    let model: OperatingProjectionPayload
    let tab: OperatingTab
    var onOpenQuickAdd: () -> Void = {}
    var onRecord: (RestaurantEventDraft) async throws -> Void = { _ in }
    var onDecide: (String, String) async throws -> Void = { _, _ in }

    @State private var activityOpen = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                switch tab {
                case .pulse: pulse
                case .moments: moments
                case .life: life
                case .memory: memory
                case .quickAdd: RestaurantQuickAddHub(model: model, onRecord: onRecord, onDecide: onDecide)
                }
            }
            .padding(.horizontal, 17)
            .padding(.vertical, 20)
        }
        .background(RestaurantChrome.bg)
        .sheet(isPresented: $activityOpen) { activitySheet }
    }

    private var pulse: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Pulse", "Your sales plan, restaurant commitments and ready money.")
            if model.financeVisible == true {
                sectionTitle("Target & Budget")
                card {
                    Text("Recorded sales").font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
                    Text(money(model.target?.progress)).font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
                    if model.target?.configured == true {
                        Text("of \(money(model.target?.value)) sales target").font(.plusJakarta(size: 13)).foregroundStyle(RestaurantChrome.secondary)
                        meter("Sales progress", fraction(model.target?.progress, model.target?.value), RestaurantChrome.brand)
                    } else {
                        Text("No sales target set for this period.").font(.plusJakarta(size: 13)).foregroundStyle(RestaurantChrome.secondary)
                    }
                    if model.budget?.configured == true {
                        meter("Budget used", fraction(model.budget?.spent, model.budget?.amount), RestaurantChrome.success)
                        line("Budget remaining", money(model.budget?.remaining), RestaurantChrome.success)
                    } else {
                        Text("No period budget set.").font(.plusJakarta(size: 13)).foregroundStyle(RestaurantChrome.secondary)
                    }
                }
                sectionTitle("Needs Attention")
                if (model.attention ?? []).isEmpty {
                    card { Text("Nothing is waiting.").foregroundStyle(RestaurantChrome.secondary) }
                }
                ForEach(model.attention ?? []) { item in
                    attentionCard(item)
                }
                sectionTitle("Payment Readiness")
                readiness("To collect", (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" })
                readiness("To pay", (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" })
                sectionTitle("Finance")
                card {
                    line("Recorded sales", money(model.finance?.sales), RestaurantChrome.accent)
                    line("Received", money(model.finance?.received), RestaurantChrome.success)
                    line("Still to collect", money(model.finance?.outstanding), Color(hex: "#FBBF24"))
                    line("Eligible cost", money(model.finance?.eligibleCost), RestaurantChrome.ink)
                    line("Business cash paid", money(model.finance?.cashPaid), RestaurantChrome.ink)
                    line("Still to pay", money(model.finance?.payable), Color(hex: "#FBBF24"))
                }
            } else {
                card {
                    Text("Finance totals stay with Finance, Approver, and Owner.")
                        .foregroundStyle(RestaurantChrome.ink)
                }
                ForEach(model.attention ?? []) { item in
                    attentionCard(item)
                }
            }
            sectionTitle("Activity")
            ForEach(Array((model.activity ?? []).prefix(5))) { row in activityRow(row) }
            if (model.activity ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(RestaurantChrome.secondary) }
            }
            Button("View all activity") { activityOpen = true }
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(RestaurantChrome.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(RestaurantChrome.brand, lineWidth: 1))
        }
    }

    private var moments: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Moments", "Orders, stock, and the restaurant this month.")
            card {
                Text(period).font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.accent)
                Text("This month, in motion").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
                HStack {
                    metric("Sales", money(model.finance?.sales ?? model.target?.progress))
                    metric("Target", model.target?.configured == true ? money(model.target?.value) : "—")
                }
                HStack {
                    metric("Spent", model.budget?.configured == true ? money(model.budget?.spent) : "—")
                    metric("Remaining", model.budget?.configured == true ? money(model.budget?.remaining) : "—")
                }
                if model.target?.configured == true {
                    meter("Sales toward target", fraction(model.target?.progress, model.target?.value), RestaurantChrome.brand)
                }
            }
            ForEach(model.boards?.moments ?? []) { section in
                board(section)
            }
            card {
                Text("Create the next restaurant moment").font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
                Button("Open Quick Add", action: onOpenQuickAdd)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(RestaurantChrome.bg)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(RestaurantChrome.brand)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var life: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Life", "How the restaurant is moving today.")
            ForEach(model.boards?.life ?? []) { board($0) }
            if (model.boards?.life ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(RestaurantChrome.secondary) }
            }
        }
    }

    private var memory: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Memory", "What this restaurant keeps from the month.")
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
                Text("All activity").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
                Text(model.title ?? "Restaurant").foregroundStyle(RestaurantChrome.secondary)
                ForEach(model.activity ?? []) { activityRow($0) }
                if (model.activity ?? []).isEmpty {
                    Text("Nothing recorded yet.").foregroundStyle(RestaurantChrome.secondary)
                }
            }
            .padding(18)
        }
        .background(RestaurantChrome.bg)
    }

    private func header(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.plusJakarta(size: 32, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
                Spacer()
                pill(roleLabel(model.role))
            }
            Text(body).font(.plusJakarta(size: 14)).foregroundStyle(RestaurantChrome.secondary)
            Text(snapshot).font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
        }
    }

    private func attentionCard(_ item: OperatingAttentionPayload) -> some View {
        card {
            Text(item.kind).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(RestaurantChrome.accent)
            Text(item.title).foregroundStyle(RestaurantChrome.ink)
            let meta = [
                item.counterpartyName,
                item.dueAt.map { String($0.prefix(10)) },
                model.financeVisible == true ? money(item.amount) : nil,
            ].compactMap { $0 }.filter { !$0.isEmpty && $0 != "—" }
            if !meta.isEmpty {
                Text(meta.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
            }
            if item.cta != nil || item.id.hasPrefix("open-") || item.sourceEventId != nil {
                Button(operatingAttentionCtaLabel(item)) {
                    openOperatingAttention(item, model: model, onOpenQuickAdd: onOpenQuickAdd)
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(RestaurantChrome.accent)
            }
        }
    }

    private func board(_ section: OperatingBoardSectionPayload) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle(section.title)
            card {
                Text(section.detail.isEmpty ? "Nothing recorded yet." : formatOperatingDetail(section.detail, currency: model.currency))
                    .font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
                if let coverage = section.coverage, !coverage.isEmpty {
                    Text(coverage).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(RestaurantChrome.accent)
                }
                ForEach(section.rows ?? []) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).foregroundStyle(RestaurantChrome.ink)
                        let extra = [row.channelName, row.counterpartyName, row.paymentStatus].compactMap { $0 }.filter { $0 != "NONE" }
                        if !extra.isEmpty {
                            Text(extra.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
                        }
                        if model.financeVisible == true, let amount = row.amount, !amount.isEmpty {
                            Text(money(amount)).font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(RestaurantChrome.accent)
                        }
                    }
                }
            }
        }
    }

    private func activityRow(_ row: OperatingActivityPayload) -> some View {
        card {
            Text(row.eventType.replacingOccurrences(of: "_", with: " "))
                .font(.plusJakarta(size: 11)).foregroundStyle(RestaurantChrome.accent)
            Text(row.title).foregroundStyle(RestaurantChrome.ink)
            let qty = [row.quantity, row.unit].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " ")
            let bits = [row.counterpartyName, qty.isEmpty ? nil : qty, row.paymentStatus == "NONE" ? nil : row.paymentStatus, String(row.occurredAt.prefix(10)), model.financeVisible == true ? money(row.amount) : nil].compactMap { $0 }.filter { !$0.isEmpty && $0 != "—" }
            if !bits.isEmpty {
                Text(bits.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
            }
            Button("Edit") { OperatingChromeState.shared.editing = row }
                .font(.plusJakarta(size: 13, weight: .semibold))
        }
    }

    @ViewBuilder private func readiness(_ label: String, _ rows: [OperatingPaymentPayload]) -> some View {
        if !rows.isEmpty {
            card {
                Text(label).font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(RestaurantChrome.accent)
                ForEach(rows) { item in
                    Button { OperatingChromeState.shared.settling = item } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            line(item.title, money(item.amount), RestaurantChrome.ink)
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
            .background(RestaurantChrome.card)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(RestaurantChrome.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text).font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
    }

    private func pill(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(RestaurantChrome.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(RestaurantChrome.notice)
            .clipShape(Capsule())
    }

    private func metric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.plusJakarta(size: 11)).foregroundStyle(RestaurantChrome.secondary)
            Text(value).font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(RestaurantChrome.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(RestaurantChrome.notice)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func meter(_ label: String, _ fraction: Double, _ color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(RestaurantChrome.secondary)
                Spacer()
                Text("\(Int(fraction * 100))%").font(.plusJakarta(size: 12)).foregroundStyle(color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(RestaurantChrome.border)
                    Capsule().fill(color).frame(width: geo.size.width * min(max(fraction, 0), 1))
                }
            }
            .frame(height: 6)
        }
    }

    private func line(_ label: String, _ value: String, _ color: Color) -> some View {
        HStack {
            Text(label).foregroundStyle(RestaurantChrome.ink)
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
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? "Restaurant"
    }
}

enum RestaurantChrome {
    static let bg = Color(hex: "#101412")
    static let card = Color(hex: "#202A24")
    static let notice = Color(hex: "#2A352E")
    static let field = Color(hex: "#1A211D")
    static let border = Color(hex: "#344238")
    static let brand = Color(hex: "#F2B544")
    static let accent = Color(hex: "#F2B544")
    static let secondary = Color(hex: "#B9C5BA")
    static let ink = Color(hex: "#F4F3EB")
    static let success = Color(hex: "#8FBF7A")
}

struct RestaurantEventDraft {
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
}