import SwiftUI

/// Shared chrome for Group Finance / Expense Splits (Figma 1257:9021 / 1257:8866).
struct GroupFinanceChrome {
    let bg: Color
    let card: Color
    let border: Color
    let text: Color
    let secondary: Color
    let accent: Color
    let accentLight: Color
    let accentSoft: Color
    let green: Color
    let orange: Color
    let heroGradient: LinearGradient

    static func wedding() -> GroupFinanceChrome {
        .init(
            bg: WeddingActiveTheme.bg,
            card: WeddingActiveTheme.card,
            border: WeddingActiveTheme.border,
            text: WeddingActiveTheme.text,
            secondary: WeddingActiveTheme.secondary,
            accent: WeddingActiveTheme.accent,
            accentLight: WeddingActiveTheme.accentLight,
            accentSoft: WeddingActiveTheme.accentSoft,
            green: Color(hex: "#4ADE80"),
            orange: Color(hex: "#FF7A3D"),
            heroGradient: WeddingActiveTheme.heroGradient
        )
    }

    static func generic() -> GroupFinanceChrome {
        .init(
            bg: GroupActiveTheme.bg,
            card: GroupActiveTheme.card,
            border: GroupActiveTheme.border,
            text: GroupActiveTheme.text,
            secondary: GroupActiveTheme.secondary,
            accent: GroupActiveTheme.accentOrange,
            accentLight: TripSheetTokens.accentEnd,
            accentSoft: GroupActiveTheme.accentOrange.opacity(0.2),
            green: Color(hex: "#4ADE80"),
            orange: TripSheetTokens.accent,
            heroGradient: LinearGradient(
                colors: [TripSheetTokens.accent, TripSheetTokens.accentEnd],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
    }

    static func houseParty() -> GroupFinanceChrome {
        let t = ExperienceActiveTheme.houseParty
        return .init(
            bg: t.bg,
            card: t.card,
            border: t.border,
            text: t.text,
            secondary: t.secondary,
            accent: t.accent,
            accentLight: t.accentLight,
            accentSoft: t.accentSoft,
            green: Color(hex: "#4ADE80"),
            orange: Color(hex: "#FF7A3D"),
            heroGradient: t.heroGradient
        )
    }

    static func officeOuting() -> GroupFinanceChrome {
        let t = ExperienceActiveTheme.officeOuting
        return .init(
            bg: t.bg,
            card: t.card,
            border: t.border,
            text: t.text,
            secondary: t.secondary,
            accent: t.accent,
            accentLight: t.accentLight,
            accentSoft: t.accentSoft,
            green: Color(hex: "#4ADE80"),
            orange: Color(hex: "#FF7A3D"),
            heroGradient: t.heroGradient
        )
    }

    static func forFamily(_ family: GroupExperienceFamily) -> GroupFinanceChrome {
        switch family {
        case .wedding: return .wedding()
        case .houseParty: return .houseParty()
        case .officeOuting: return .officeOuting()
        case .giftPool, .groupPurchase, .sharedAsset, .customPurchase:
            return .purchase(PurchaseActiveTheme.forFamily(family))
        case .flatmates, .familyHousehold, .coLiving, .customLiving:
            return .living(LivingActiveTheme.forFamily(family))
        case .sharedGeneric: return .generic()
        }
    }

    static func purchase(_ t: PurchaseActiveTheme) -> GroupFinanceChrome {
        .init(
            bg: t.bg,
            card: t.card,
            border: t.border,
            text: t.text,
            secondary: t.secondary,
            accent: t.accent,
            accentLight: t.accentLight,
            accentSoft: t.accentSoft,
            green: Color(hex: "#4ADE80"),
            orange: Color(hex: "#FF7A3D"),
            heroGradient: t.heroGradient
        )
    }

    static func living(_ t: LivingActiveTheme) -> GroupFinanceChrome {
        .init(
            bg: t.bg,
            card: t.card,
            border: t.border,
            text: t.text,
            secondary: t.secondary,
            accent: t.accent,
            accentLight: t.accentLight,
            accentSoft: t.accentSoft,
            green: Color(hex: "#4ADE80"),
            orange: Color(hex: "#FF7A3D"),
            heroGradient: t.heroGradient
        )
    }
}

/// Figma 1257:9021 / 1260:9424 / 1265:10466 — Group Finance.
struct GroupFinanceDetailView: View {
    let momentId: String
    let momentTitle: String?
    var isWedding: Bool = false
    var experienceFamily: GroupExperienceFamily = .sharedGeneric
    var onClose: () -> Void
    var onOpenSplits: () -> Void = {}
    var onSettle: () -> Void = {}

    @State private var finance: APIClient.GroupFinancePayload?
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    private var family: GroupExperienceFamily {
        if experienceFamily != .sharedGeneric { return experienceFamily }
        return isWedding ? .wedding : .sharedGeneric
    }

    private var chrome: GroupFinanceChrome { .forFamily(family) }
    private var isWeddingChrome: Bool { family.isWedding }
    private var financeScreenTitle: String {
        if family.isThemedLiving {
            return LivingActiveTheme.forFamily(family).financeTitle
        }
        return "Group Finance"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }
                    HStack(spacing: 8) {
                        Text("💕 \(title ?? momentTitle ?? "Group Moment")")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(chrome.accent)
                        if isWeddingChrome {
                            Text("18 Oct 2026")
                                .font(.plusJakarta(size: 10, weight: .semibold))
                                .foregroundStyle(chrome.accent)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(chrome.accentSoft)
                                .clipShape(Capsule())
                        }
                        Text("· \(participants.count) members")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(chrome.accent)
                    }

                    if loading && finance == nil {
                        ProgressView().tint(chrome.accent).frame(maxWidth: .infinity).padding(.vertical, 40)
                    } else {
                        financeSummary
                        positionsSection
                    }
                }
                .padding(16)
            }
            .background(chrome.bg)
            .navigationTitle(financeScreenTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onClose) {
                        Image(systemName: "chevron.left").foregroundStyle(chrome.accent)
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(action: onSettle) {
                    VStack(spacing: 4) {
                        Text("Settle Outstanding")
                            .font(.plusJakarta(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                        Text("Record a settlement against open balances")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(Color.white.opacity(0.8))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(chrome.heroGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
        }
        .task { await load() }
    }

    private var financeSummary: some View {
        let total = finance?.totals?.first
        let currency = total?.currencyCode ?? "INR"
        let utilization = GroupFinanceFormat.utilizationPercent(
            expenseTotal: total?.expenseTotal,
            budgetTotal: total?.budgetTotal
        )
        return VStack(alignment: .leading, spacing: 14) {
            Text("TOTAL EXPENSES")
                .font(.plusJakarta(size: 11, weight: .semibold))
                .foregroundStyle(chrome.secondary)
            Text(GroupFinanceFormat.formatMoney(total?.expenseTotal, currencyCode: currency))
                .font(.plusJakarta(size: 28, weight: .heavy))
                .foregroundStyle(chrome.text)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                summaryTile("Budget", GroupFinanceFormat.formatMoney(total?.budgetTotal, currencyCode: currency), "\(utilization)% Utilized", chrome.accent)
                summaryTile("Contributions", GroupFinanceFormat.formatMoney(total?.contributionTotal, currencyCode: currency), "Collected", chrome.green)
                summaryTile("Settled", GroupFinanceFormat.formatMoney(total?.settledTotal, currencyCode: currency), "Recorded", chrome.green)
                summaryTile("Outstanding", GroupFinanceFormat.formatMoney(total?.outstandingTotal, currencyCode: currency), "Live total", chrome.orange)
            }
            GroupProgressBar(percent: utilization)
            Button("View Splits →", action: onOpenSplits)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(chrome.accentLight)
        }
        .padding(16)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(chrome.border))
    }

    private func summaryTile(_ label: String, _ value: String, _ hint: String, _ hintColor: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                if label == "Budget" {
                    Image(systemName: "dollarsign.circle")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.secondary)
                } else if label == "Contributions" {
                    Image(systemName: "arrow.down.circle")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.secondary)
                } else if label == "Settled" {
                    Image(systemName: "checkmark.circle")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.secondary)
                } else if label == "Outstanding" {
                    Image(systemName: "exclamationmark.circle")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(chrome.secondary)
                }
                Text(label).font(.plusJakarta(size: 11)).foregroundStyle(chrome.secondary)
            }
            Text(value).font(.plusJakarta(size: 16, weight: .bold)).foregroundStyle(chrome.text)
            Text(hint).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(hintColor)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(chrome.bg)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(chrome.border))
    }

    private var positionsSection: some View {
        let positions = finance?.positions ?? []
        let nameById = Dictionary(uniqueKeysWithValues: participants.map { ($0.participantId, $0.displayName ?? String($0.participantId.prefix(8))) })
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        return VStack(alignment: .leading, spacing: 10) {
            Text("👥 Participant Positions")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(chrome.text)
            if positions.isEmpty {
                Text("Positions appear after shared expenses are recorded.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(chrome.secondary)
            } else {
                ForEach(positions) { pos in
                    let name = nameById[pos.participantId] ?? String(pos.participantId.prefix(8))
                    positionCard(name: name, pos: pos, currency: currency)
                }
            }
        }
    }

    private func positionCard(name: String, pos: APIClient.GroupFinancePositionPayload, currency: String) -> some View {
        let net = GroupFinanceFormat.parseAmount(pos.netPosition)
        let getsBack = net >= 0
        let roleLabel = isWeddingChrome ? "(Party Member)" : ""
        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(String(name.prefix(1)).uppercased())
                    .font(.plusJakarta(size: 13, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(getsBack ? chrome.accent : Color.white.opacity(0.1))
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(name).font(.plusJakarta(size: 14, weight: .bold)).foregroundStyle(chrome.text)
                    if isWeddingChrome && !roleLabel.isEmpty {
                        Text(roleLabel).font(.plusJakarta(size: 11)).foregroundStyle(chrome.secondary)
                    }
                }
                Spacer()
                Text(getsBack
                     ? "+\(GroupFinanceFormat.formatMoney(pos.netPosition, currencyCode: currency)) Gets back"
                     : "-\(GroupFinanceFormat.formatMoney(decimalString(absDecimal(net)), currencyCode: currency)) Owes")
                    .font(.plusJakarta(size: 12, weight: .bold))
                    .foregroundStyle(getsBack ? chrome.green : chrome.orange)
            }
            HStack {
                metric("Paid", GroupFinanceFormat.formatMoney(pos.paidTotal, currencyCode: currency))
                metric("Share", GroupFinanceFormat.formatMoney(pos.allocatedTotal, currencyCode: currency))
                metric("Payable", GroupFinanceFormat.formatMoney(pos.payableTotal, currencyCode: currency), chrome.orange)
                metric("Receivable", GroupFinanceFormat.formatMoney(pos.receivableTotal, currencyCode: currency), chrome.green)
            }
        }
        .padding(14)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
    }

    private func metric(_ label: String, _ value: String, _ tint: Color? = nil) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.plusJakarta(size: 10)).foregroundStyle(chrome.secondary)
            Text(value).font(.plusJakarta(size: 11, weight: .semibold)).foregroundStyle(tint ?? chrome.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func load() async {
        error = nil
        if let cached = GroupTabDataCache.peekPulse(momentId) {
            finance = cached.finance
            title = cached.title
            loading = false
        }
        do {
            async let fin = APIClient.shared.getGroupFinance(momentId: momentId)
            async let people = APIClient.shared.listGroupParticipants(momentId: momentId)
            let facet = try await fin
            title = facet.title ?? title
            finance = facet.payload
            participants = try await people
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

/// Figma 1257:8866 / 1260:9267 / 1265:10310 — Expense Splits / View Splits.
struct GroupExpenseSplitsView: View {
    let momentId: String
    let momentTitle: String?
    var isWedding: Bool = false
    var experienceFamily: GroupExperienceFamily = .sharedGeneric
    var onClose: () -> Void
    var onOpenFinance: () -> Void = {}

    @State private var finance: APIClient.GroupFinancePayload?
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var title: String?
    @State private var loading = true
    @State private var error: String?

    private var family: GroupExperienceFamily {
        if experienceFamily != .sharedGeneric { return experienceFamily }
        return isWedding ? .wedding : .sharedGeneric
    }

    private var chrome: GroupFinanceChrome { .forFamily(family) }
    private var isWeddingChrome: Bool { family.isWedding }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }
                    if loading && finance == nil {
                        ProgressView().tint(chrome.accent).frame(maxWidth: .infinity).padding(.vertical, 40)
                    } else {
                        summaryCard
                        viewerBalance
                        whoOwes
                        breakdown
                    }
                }
                .padding(16)
            }
            .background(chrome.bg)
            .navigationTitle("Expense Splits")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onClose) {
                        Image(systemName: "chevron.left").foregroundStyle(chrome.accent)
                    }
                }
            }
        }
        .task { await load() }
    }

    private var summaryCard: some View {
        let total = finance?.totals?.first
        let currency = total?.currencyCode ?? "INR"
        let expenseTotal = GroupFinanceFormat.parseAmount(total?.expenseTotal)
        let budgetTotal = GroupFinanceFormat.parseAmount(total?.budgetTotal)
        let categoryFraction: CGFloat = budgetTotal > 0 ? CGFloat(truncating: (expenseTotal as NSDecimalNumber).dividing(by: budgetTotal as NSDecimalNumber)) : 0
        
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("💕 \(title ?? momentTitle ?? "Group Moment")")
                    .font(.plusJakarta(size: 16, weight: .heavy))
                    .foregroundStyle(chrome.text)
                Spacer()
                Text(GroupFinanceFormat.formatMoney(total?.expenseTotal, currencyCode: currency))
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(chrome.accent)
            }
            
            if family == .wedding || family.isThemedExperience {
                Text("Spend vs budget")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(chrome.secondary)
                GeometryReader { geo in
                    HStack(spacing: 0) {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(chrome.accent)
                            .frame(width: geo.size.width * min(max(categoryFraction, 0.05), 0.95))
                        RoundedRectangle(cornerRadius: 8)
                            .fill(family == .wedding ? Color(hex: "#F5E6D3") : chrome.accentSoft)
                    }
                }
                .frame(height: 12)
            } else {
                Text("Category split chart requires expense categories API.")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(chrome.secondary)
            }
            
            Button("Tap for Group Finance →", action: onOpenFinance)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(chrome.accentLight)
        }
        .padding(16)
        .background(chrome.card)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(chrome.border))
    }

    private var viewerBalance: some View {
        let viewer = finance?.viewerPosition
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        let net = GroupFinanceFormat.parseAmount(viewer?.netPosition)
        let headline: String = {
            guard viewer != nil else { return "No balance yet" }
            if net < 0 { return "You owe \(GroupFinanceFormat.formatMoney(decimalString(absDecimal(net)), currencyCode: currency))" }
            if net > 0 { return "You get back \(GroupFinanceFormat.formatMoney(viewer?.netPosition, currencyCode: currency))" }
            return "You're settled up"
        }()
        return VStack(alignment: .leading, spacing: 12) {
            Text("YOUR BALANCE")
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(Color.white.opacity(0.85))
            Text(headline)
                .font(.plusJakarta(size: 24, weight: .heavy))
                .foregroundStyle(.white)
            HStack {
                Text("Keep group coordination high by settling up.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color.white.opacity(0.9))
                Spacer()
                Text("Settle Up")
                    .font(.plusJakarta(size: 12, weight: .heavy))
                    .foregroundStyle(chrome.accent)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.white)
                    .clipShape(Capsule())
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(chrome.heroGradient)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var whoOwes: some View {
        let positions = finance?.positions ?? []
        let nameById = Dictionary(uniqueKeysWithValues: participants.map { ($0.participantId, $0.displayName ?? String($0.participantId.prefix(8))) })
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        return VStack(alignment: .leading, spacing: 10) {
            Text("🤝 Who Owes Whom")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(chrome.text)
            Text("Net positions from live finance — pairwise settlement graph is not available yet.")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(chrome.secondary)
            ForEach(positions.filter { abs(GroupFinanceFormat.parseAmount($0.netPosition)) > 0 }) { pos in
                let name = nameById[pos.participantId] ?? String(pos.participantId.prefix(8))
                let net = GroupFinanceFormat.parseAmount(pos.netPosition)
                let owes = net < 0
                HStack(spacing: 12) {
                    Text(String(name.prefix(1)).uppercased())
                        .font(.plusJakarta(size: 13, weight: .heavy))
                        .foregroundStyle(owes ? chrome.text : .white)
                        .frame(width: 36, height: 36)
                        .background(owes ? Color.white.opacity(0.1) : chrome.accent)
                        .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(owes ? "\(name) owes (net)" : "\(name) gets back (net)")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(chrome.text)
                        Text(owes ? "Pending settlement" : "Receivable outstanding")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(chrome.secondary)
                    }
                    Spacer()
                    Text(GroupFinanceFormat.formatMoney(decimalString(absDecimal(net)), currencyCode: currency))
                        .font(.plusJakarta(size: 14, weight: .heavy))
                        .foregroundStyle(owes ? chrome.orange : chrome.green)
                }
                .padding(12)
                .background(chrome.card)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
            }
        }
    }

    private var breakdown: some View {
        let positions = finance?.positions ?? []
        let nameById = Dictionary(uniqueKeysWithValues: participants.map { ($0.participantId, $0.displayName ?? String($0.participantId.prefix(8))) })
        let currency = finance?.totals?.first?.currencyCode ?? "INR"
        return VStack(alignment: .leading, spacing: 10) {
            Text("📊 Expense Breakdown")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(chrome.text)
            ForEach(positions) { pos in
                let name = nameById[pos.participantId] ?? String(pos.participantId.prefix(8))
                let net = GroupFinanceFormat.parseAmount(pos.netPosition)
                let positive = net >= 0
                HStack(spacing: 12) {
                    Text(String(name.prefix(1)).uppercased())
                        .font(.plusJakarta(size: 14, weight: .heavy))
                        .foregroundStyle(positive ? .white : chrome.text)
                        .frame(width: 36, height: 36)
                        .background(positive ? chrome.accent : Color.white.opacity(0.1))
                        .clipShape(Circle())
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(name).font(.plusJakarta(size: 14, weight: .bold)).foregroundStyle(chrome.text)
                            Spacer()
                            Text("Paid \(GroupFinanceFormat.compactMoney(pos.paidTotal, currencyCode: currency)) · Share \(GroupFinanceFormat.compactMoney(pos.allocatedTotal, currencyCode: currency))")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(chrome.secondary)
                        }
                        HStack {
                            GroupProgressBar(percent: sharePercent(pos))
                            Text(positive
                                 ? "+\(GroupFinanceFormat.formatMoney(pos.netPosition, currencyCode: currency))"
                                 : "-\(GroupFinanceFormat.formatMoney(decimalString(absDecimal(net)), currencyCode: currency))")
                                .font(.plusJakarta(size: 11, weight: .bold))
                                .foregroundStyle(positive ? chrome.green : chrome.orange)
                        }
                    }
                }
                .padding(12)
                .background(chrome.card)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(chrome.border))
            }
        }
    }

    private func sharePercent(_ pos: APIClient.GroupFinancePositionPayload) -> Int {
        let paid = GroupFinanceFormat.parseAmount(pos.paidTotal)
        let share = GroupFinanceFormat.parseAmount(pos.allocatedTotal)
        guard share > 0 else { return 0 }
        let pct = (paid as NSDecimalNumber).doubleValue / (share as NSDecimalNumber).doubleValue * 100
        return min(100, Int(pct.rounded()))
    }

    private func load() async {
        error = nil
        if let cached = GroupTabDataCache.peekPulse(momentId) {
            finance = cached.finance
            title = cached.title
            loading = false
        }
        do {
            async let fin = APIClient.shared.getGroupFinance(momentId: momentId)
            async let people = APIClient.shared.listGroupParticipants(momentId: momentId)
            let facet = try await fin
            title = facet.title ?? title
            finance = facet.payload
            participants = try await people
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

private func absDecimal(_ value: Decimal) -> Decimal {
    value < 0 ? -value : value
}

private func decimalString(_ value: Decimal) -> String {
    (value as NSDecimalNumber).stringValue
}
