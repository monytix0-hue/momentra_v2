import SwiftUI

struct DistributionActiveView: View {
    let model: OperatingProjectionPayload
    let tab: OperatingTab
    var onOpenQuickAdd: () -> Void = {}
    var onRecord: (DistributionEventDraft) async throws -> Void = { _ in }
    var onDecide: (String, String) async throws -> Void = { _, _ in }

    @State private var activityOpen = false
    @State private var gstDetailsOpen = false

    var body: some View {
        Group {
            if tab == .quickAdd {
                DistributionQuickAddHub(model: model, onRecord: onRecord, onDecide: onDecide)
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
        .background(DistributionChrome.bg)
        .sheet(isPresented: $activityOpen) { activitySheet }
    }

    private var pulse: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Pulse", "Your sales plan, distribution commitments and ready money.")
            if model.financeVisible == true {
                sectionTitle("Target & Budget")
                card {
                    Text("Recorded sales").font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                    Text(money(recordedSales)).font(.plusJakarta(size: 28, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
                    if model.target?.configured == true {
                        Text("of \(money(model.target?.value)) sales target").font(.plusJakarta(size: 13)).foregroundStyle(DistributionChrome.secondary)
                        meter("Sales progress", fraction(recordedSales, model.target?.value), DistributionChrome.brand)
                    } else {
                        Text("No sales target set for this period.").font(.plusJakarta(size: 13)).foregroundStyle(DistributionChrome.secondary)
                    }
                    if model.budget?.configured == true {
                        meter("Budget used", fraction(model.budget?.spent, model.budget?.amount), DistributionChrome.success)
                        line("Budget remaining", money(model.budget?.remaining), DistributionChrome.success)
                    } else {
                        Text("No period budget set.").font(.plusJakarta(size: 13)).foregroundStyle(DistributionChrome.secondary)
                    }
                }
                sectionTitle("Needs Attention")
                if (model.attention ?? []).isEmpty {
                    card { Text("Nothing is waiting.").foregroundStyle(DistributionChrome.secondary) }
                }
                ForEach(model.attention ?? []) { item in
                    attentionCard(item)
                }
                sectionTitle("Payment Readiness")
                readiness("To collect", (model.paymentReadiness ?? []).filter { $0.relevance == "RECEIVABLE" })
                readiness("To pay", (model.paymentReadiness ?? []).filter { $0.relevance != "RECEIVABLE" })
                sectionTitle("Finance")
                card {
                    line("Recorded sales", money(model.finance?.sales), DistributionChrome.accent)
                    line("Received", money(model.finance?.received), DistributionChrome.success)
                    line("Still to collect", money(model.finance?.outstanding), Color(hex: "#FBBF24"))
                    line("Eligible cost", money(model.finance?.eligibleCost), DistributionChrome.ink)
                    line("Business cash paid", money(model.finance?.cashPaid), DistributionChrome.ink)
                    line("Still to pay", money(model.finance?.payable), Color(hex: "#FBBF24"))
                }
                if let position = model.gstPosition {
                    sectionTitle("GST Position")
                    card {
                        Text("Estimated GST from recorded sales and purchases").font(.plusJakarta(size: 13)).foregroundStyle(DistributionChrome.secondary)
                        Text(position.asOfMonth ?? "").font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                        if (position.sources ?? []).isEmpty {
                            Text("No GST transactions recorded for this period.").foregroundStyle(DistributionChrome.ink)
                        } else {
                            line("GST on sales", money(position.outputGst), DistributionChrome.accent)
                            line("GST on purchases", money(position.inputGst), DistributionChrome.ink)
                            let negative = (position.difference ?? "").hasPrefix("-")
                            Text(negative
                                 ? "Estimated excess purchase GST: \(money(position.difference?.replacingOccurrences(of: "-", with: "")))"
                                 : "Estimated difference: \(money(position.difference)) payable before adjustments")
                                .foregroundStyle(DistributionChrome.ink)
                            Button(gstDetailsOpen ? "Hide GST details" : "View GST details") { gstDetailsOpen.toggle() }
                                .font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(DistributionChrome.accent)
                            if gstDetailsOpen {
                                line("CGST on sales", money(position.cgstSales), DistributionChrome.secondary)
                                line("SGST on sales", money(position.sgstSales), DistributionChrome.secondary)
                                line("IGST on sales", money(position.igstSales), DistributionChrome.secondary)
                                line("CGST on purchases", money(position.cgstPurchases), DistributionChrome.secondary)
                                line("SGST on purchases", money(position.sgstPurchases), DistributionChrome.secondary)
                                line("IGST on purchases", money(position.igstPurchases), DistributionChrome.secondary)
                                Text("Recorded purchase GST is not eligible input tax credit.")
                                    .font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                                ForEach(position.sources ?? []) { source in
                                    Text("\(source.title ?? "") · \(source.eventType ?? "") · \(money(source.gstTotal))")
                                        .font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                                }
                            }
                        }
                        Text("Indicative GST position — not a GST return or verified amount payable")
                            .font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                    }
                }
            } else {
                card {
                    Text("Finance totals stay with Finance, Approver, and Owner.")
                        .foregroundStyle(DistributionChrome.ink)
                }
                ForEach(model.attention ?? []) { item in
                    attentionCard(item)
                }
            }
            sectionTitle("Activity")
            ForEach(Array((model.activity ?? []).prefix(5))) { row in activityRow(row) }
            if (model.activity ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(DistributionChrome.secondary) }
            }
            Button("View all activity") { activityOpen = true }
                .font(.plusJakarta(size: 15, weight: .semibold))
                .foregroundStyle(DistributionChrome.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(DistributionChrome.brand, lineWidth: 1))
        }
    }

    private var moments: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Moments", "Dispatches, stock, and the distribution this month.")
            card {
                Text(period).font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.accent)
                Text("This month, in motion").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
                HStack {
                    metric("Sales", money(model.finance?.sales ?? model.target?.progress))
                    metric("Target", model.target?.configured == true ? money(model.target?.value) : "—")
                }
                HStack {
                    metric("Spent", model.budget?.configured == true ? money(model.budget?.spent) : "—")
                    metric("Remaining", model.budget?.configured == true ? money(model.budget?.remaining) : "—")
                }
                if model.target?.configured == true {
                    meter("Sales toward target", fraction(recordedSales, model.target?.value), DistributionChrome.brand)
                }
            }
            ForEach(model.boards?.moments ?? [], id: \.code) { section in
                board(section)
            }
            card {
                Text("Create the next distribution moment").font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
                Button("Open Quick Add", action: onOpenQuickAdd)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(DistributionChrome.bg)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(DistributionChrome.brand)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var life: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Life", "How the distribution is moving today.")
            ForEach(model.boards?.life ?? []) { board($0) }
            if (model.boards?.life ?? []).isEmpty {
                card { Text("Nothing recorded yet.").foregroundStyle(DistributionChrome.secondary) }
            }
        }
    }

    private var memory: some View {
        VStack(alignment: .leading, spacing: 16) {
            header("Memory", "What this distribution keeps from the month.")
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
                Text("All activity").font(.plusJakarta(size: 22, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
                Text(model.title ?? "Distribution").foregroundStyle(DistributionChrome.secondary)
                ForEach(model.activity ?? []) { activityRow($0) }
                if (model.activity ?? []).isEmpty {
                    Text("Nothing recorded yet.").foregroundStyle(DistributionChrome.secondary)
                }
            }
            .padding(18)
        }
        .background(DistributionChrome.bg)
    }

    private func header(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.plusJakarta(size: 32, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
                Spacer()
                pill(roleLabel(model.role))
            }
            Text(body).font(.plusJakarta(size: 14)).foregroundStyle(DistributionChrome.secondary)
            Text(snapshot).font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
        }
    }

    private func attentionCard(_ item: OperatingAttentionPayload) -> some View {
        card {
            Text(item.kind).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(DistributionChrome.accent)
            Text(item.title).foregroundStyle(DistributionChrome.ink)
            let meta = [
                item.counterpartyName,
                item.dueAt.map { String($0.prefix(10)) },
                model.financeVisible == true ? money(item.amount) : nil,
            ].compactMap { $0 }.filter { !$0.isEmpty && $0 != "—" }
            if !meta.isEmpty {
                Text(meta.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
            }
            if item.cta != nil || item.id.hasPrefix("open-") || item.sourceEventId != nil {
                Button(operatingAttentionCtaLabel(item)) {
                    openOperatingAttention(item, model: model, onOpenQuickAdd: onOpenQuickAdd)
                }
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(DistributionChrome.accent)
            }
        }
    }

    private func board(_ section: OperatingBoardSectionPayload) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionTitle(section.title)
            card {
                Text(section.detail.isEmpty ? "Nothing recorded yet." : formatOperatingDetail(section.detail, currency: model.currency))
                    .font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                if let coverage = section.coverage, !coverage.isEmpty {
                    Text(coverage).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(DistributionChrome.accent)
                }
                ForEach(section.rows ?? []) { row in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.title).foregroundStyle(DistributionChrome.ink)
                        let extra = [row.channelName, row.counterpartyName, row.paymentStatus].compactMap { $0 }.filter { $0 != "NONE" }
                        if !extra.isEmpty {
                            Text(extra.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                        }
                        if model.financeVisible == true, let amount = row.amount, !amount.isEmpty {
                            Text(money(amount)).font(.plusJakarta(size: 13, weight: .semibold)).foregroundStyle(DistributionChrome.accent)
                        }
                    }
                }
            }
        }
    }

    private func activityRow(_ row: OperatingActivityPayload) -> some View {
        card {
            Text(row.eventType.replacingOccurrences(of: "_", with: " "))
                .font(.plusJakarta(size: 11)).foregroundStyle(DistributionChrome.accent)
            Text(row.title).foregroundStyle(DistributionChrome.ink)
            let qty = [row.quantity, row.unit].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " ")
            let bits = [row.counterpartyName, qty.isEmpty ? nil : qty, row.paymentStatus == "NONE" ? nil : row.paymentStatus, String(row.occurredAt.prefix(10)), model.financeVisible == true ? money(row.amount) : nil].compactMap { $0 }.filter { !$0.isEmpty && $0 != "—" }
            if !bits.isEmpty {
                Text(bits.joined(separator: " · ")).font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
            }
            Button("Edit") { OperatingChromeState.shared.editing = row }
                .font(.plusJakarta(size: 13, weight: .semibold))
        }
    }

    @ViewBuilder private func readiness(_ label: String, _ rows: [OperatingPaymentPayload]) -> some View {
        if !rows.isEmpty {
            card {
                Text(label).font(.plusJakarta(size: 12, weight: .semibold)).foregroundStyle(DistributionChrome.accent)
                ForEach(rows) { item in
                    Button { OperatingChromeState.shared.settling = item } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            line(item.title, money(item.amount), DistributionChrome.ink)
                            let bucket: String? = {
                                switch item.dueBucket {
                                case "OVERDUE": return "Overdue"
                                case "DUE_SOON": return "Due soon"
                                case "OPEN": return item.dueAt == nil ? "Open · no due date" : "Open"
                                default: return nil
                                }
                            }()
                            Text([item.counterpartyName, item.dueAt.map { String($0.prefix(10)) }, bucket].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "))
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
            .background(DistributionChrome.card)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(DistributionChrome.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 18))
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text).font(.plusJakarta(size: 20, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
    }

    private func pill(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .semibold))
            .foregroundStyle(DistributionChrome.accent)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(DistributionChrome.notice)
            .clipShape(Capsule())
    }

    private func metric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.plusJakarta(size: 11)).foregroundStyle(DistributionChrome.secondary)
            Text(value).font(.plusJakarta(size: 16, weight: .semibold)).foregroundStyle(DistributionChrome.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(DistributionChrome.notice)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func meter(_ label: String, _ fraction: Double, _ color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label).font(.plusJakarta(size: 12)).foregroundStyle(DistributionChrome.secondary)
                Spacer()
                Text("\(Int(fraction * 100))%").font(.plusJakarta(size: 12)).foregroundStyle(color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(DistributionChrome.border)
                    Capsule().fill(color).frame(width: geo.size.width * min(max(fraction, 0), 1))
                }
            }
            .frame(height: 6)
        }
    }

    private func line(_ label: String, _ value: String, _ color: Color) -> some View {
        HStack {
            Text(label).foregroundStyle(DistributionChrome.ink)
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

    private var recordedSales: String? {
        let metric = model.target?.metric.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if metric.isEmpty || metric.uppercased() == "REVENUE" {
            return model.finance?.sales ?? model.target?.progress
        }
        return model.target?.progress
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
        OperatingMomentCatalog.roles.first { $0.0 == code }?.1 ?? "Distribution"
    }
}

enum DistributionChrome {
    static let bg = Color(hex: "#071014")
    static let card = Color(hex: "#0C171B")
    static let notice = Color(hex: "#15262C")
    static let field = Color(hex: "#0C171B")
    static let border = Color(hex: "#345058")
    static let brand = Color(hex: "#FBBF24")
    static let accent = Color(hex: "#FBBF24")
    static let secondary = Color(hex: "#66C7A4")
    static let ink = Color(hex: "#EFF5F2")
    static let success = Color(hex: "#66C7A4")
}

struct DistributionEventDraft {
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
    var conversionToBase: String? = nil
    var dueAt: String? = nil
    var contract: OperatingContractInput? = nil
    var lineItems: [(itemName: String, quantity: String?, unit: String?, unitPrice: String?, masterId: String?)]? = nil
    var masterGstApplicable: String? = nil
    var masterGstClassification: String? = nil
    var masterGstRate: String? = nil
    var masterPriceIncludesGst: String? = nil
    var masterSalePriceIncludesGst: String? = nil
    var masterPurchasePriceIncludesGst: String? = nil
    var masterGstEffectiveFrom: String? = nil
    var masterHsn: String? = nil
    var masterGstRegistration: String? = nil
    var masterBusinessState: String? = nil
    var masterBillingState: String? = nil
    var masterDeliveryState: String? = nil
    var masterGstin: String? = nil
}

extension OperatingCaptureDraft {
    func toDistribution() -> DistributionEventDraft {
        DistributionEventDraft(
            eventType: eventType,
            title: title,
            amount: amount,
            note: note,
            channelName: channelName,
            counterpartyName: counterpartyName,
            paymentStatus: paymentStatus,
            occurredAt: occurredAt,
            quantity: quantity,
            unit: unit,
            unitPrice: unitPrice,
            itemName: itemName,
            parentEventId: parentEventId,
            revisesEventId: revisesEventId,
            masterKind: masterKind,
            conversionToBase: conversionToBase,
            dueAt: dueAt,
            contract: contract,
            lineItems: lineItems,
            masterGstApplicable: masterGstApplicable,
            masterGstClassification: masterGstClassification,
            masterGstRate: masterGstRate,
            masterPriceIncludesGst: masterPriceIncludesGst,
            masterSalePriceIncludesGst: masterSalePriceIncludesGst,
            masterPurchasePriceIncludesGst: masterPurchasePriceIncludesGst,
            masterGstEffectiveFrom: masterGstEffectiveFrom,
            masterHsn: masterHsn,
            masterGstRegistration: masterGstRegistration,
            masterBusinessState: masterBusinessState,
            masterBillingState: masterBillingState,
            masterDeliveryState: masterDeliveryState,
            masterGstin: masterGstin
        )
    }
}
