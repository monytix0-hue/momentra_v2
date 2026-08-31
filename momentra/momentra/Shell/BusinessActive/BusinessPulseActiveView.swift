import SwiftUI

/// Themed Business Pulse — live pulse/finance only (B01–B03).
struct BusinessPulseActiveView: View {
    let refreshToken: UInt64
    let momentTitle: String?
    let momentId: String?
    var momentTypeCode: String? = nil
    var onAddExpense: () -> Void = {}
    var onOpenQuickAdd: () -> Void = {}

    @State private var pulse: APIClient.BusinessPulsePayload?
    @State private var financeOverlay: APIClient.BusinessFinancePayload?
    @State private var loading = true
    @State private var error: String?

    private var theme: BusinessActiveTheme { .forTypeCode(momentTypeCode) }
    private var finance: APIClient.BusinessFinancePayload? { financeOverlay ?? pulse?.payload?.finance }
    private var isEmpty: Bool {
        let q = (pulse?.payload?.dataQuality ?? finance?.dataQuality ?? "EMPTY").uppercased()
        return q == "EMPTY" || pulse?.status?.uppercased() == "EMPTY"
    }

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(theme.accent)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                        }
                        if let momentTitle, !momentTitle.isEmpty {
                            Text(momentTitle)
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(theme.secondary)
                        }
                        heroCard
                        kpiRow
                        if isEmpty {
                            emptyFinanceCard
                        } else {
                            totalsCard
                        }
                        Button(action: onAddExpense) {
                            HStack {
                                Image(systemName: "plus.circle.fill")
                                Text(primaryCtaLabel)
                                    .font(.plusJakarta(size: 15, weight: .heavy))
                            }
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(theme.accent)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .buttonStyle(.plain)
                        .disabled(momentId == nil)
                        .opacity(momentId == nil ? 0.5 : 1)

                        Button(action: onOpenQuickAdd) {
                            Text("Open Action Center")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(theme.accent)
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
            }
        }
        .background(theme.bg)
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var primaryCtaLabel: String {
        switch theme.typeLabel {
        case "Business Runway": return "Log Expense"
        case "Business Operations": return "Log Spend"
        default: return "Log Team Update"
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(theme.pulseTitle.uppercased())
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(.white.opacity(0.8))
            HStack(alignment: .firstTextBaseline) {
                Text("\(pulse?.payload?.attentionCount ?? 0)")
                    .font(.plusJakarta(size: 36, weight: .heavy))
                    .foregroundStyle(.white)
                Text("attention")
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.75))
                Spacer()
                Text(isEmpty ? "No signal yet" : "Live")
                    .font(.plusJakarta(size: 11, weight: .heavy))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Color.white.opacity(0.12))
                    .clipShape(Capsule())
            }
            if let health = pulse?.payload?.financialHealthScore, !health.isEmpty {
                Text("Health \(health)")
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.85))
            }
        }
        .padding(14)
        .background(theme.heroGradient)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var kpiRow: some View {
        HStack(spacing: 8) {
            metricChip("Moments", "\(pulse?.payload?.activeMomentCount ?? 0)")
            metricChip("Family", pulse?.businessFamily?.replacingOccurrences(of: "_", with: " ") ?? theme.typeLabel)
            metricChip(runwayLabel, pulse?.payload?.runwayMonths ?? "—")
        }
    }

    private var runwayLabel: String {
        switch theme.typeLabel {
        case "Business Runway": return "Runway"
        case "Business Operations": return "Ops"
        default: return "Capacity"
        }
    }

    private func metricChip(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.plusJakarta(size: 9, weight: .semibold))
                .foregroundStyle(theme.secondary)
            Text(value)
                .font(.plusJakarta(size: 13, weight: .heavy))
                .foregroundStyle(theme.text)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var emptyFinanceCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("No finance signal yet")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(theme.text)
            Text("Live KPIs appear when pulse/finance projections have data. Missing metrics stay empty.")
                .font(.plusJakarta(size: 13))
                .foregroundStyle(theme.secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var totalsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Totals")
                .font(.plusJakarta(size: 16, weight: .heavy))
                .foregroundStyle(theme.text)
            let totals = finance?.totals ?? []
            if totals.isEmpty {
                Text("No currency totals yet.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(theme.secondary)
            } else {
                ForEach(Array(totals.enumerated()), id: \.offset) { _, row in
                    HStack {
                        Text(row.currencyCode)
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(theme.text)
                        Spacer()
                        VStack(alignment: .trailing, spacing: 2) {
                            Text("Spent \(maskedMoney(row.expenseTotal ?? "0"))")
                                .font(.plusJakarta(size: 12, weight: .semibold))
                                .foregroundStyle(theme.text)
                            Text("Revenue \(maskedMoney(row.revenueTotal ?? "0"))")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(theme.secondary)
                            Text("Invoices \(maskedMoney(row.invoiceOutstandingTotal ?? "0"))")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(theme.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .padding(14)
        .background(theme.card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func load() async {
        guard let momentId else {
            loading = false
            error = "Select a Business Moment."
            return
        }
        error = nil
        if let cached = BusinessTabDataCache.peekPulse(momentId) {
            pulse = cached.pulse
            financeOverlay = cached.finance
            loading = false
        } else {
            loading = true
        }
        do {
            let tab = try await BusinessTabLoad.loadPulseTab(momentId: momentId)
            pulse = tab.pulse
            financeOverlay = tab.finance
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    private func maskedMoney(_ value: String) -> String {
        UserDefaults.standard.bool(forKey: "momentra_hide_balances") ? "••••" : value
    }
}
