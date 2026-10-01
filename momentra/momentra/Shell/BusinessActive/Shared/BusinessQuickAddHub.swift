import SwiftUI

/// Figma Action Center hubs — Team Ops / Runway / Ops (B01–B03).
struct BusinessQuickAddHub: View {
    let hasActiveMoment: Bool
    let hasCompany: Bool
    var capabilityCodes: [String]? = nil
    var momentId: String? = nil
    var momentTypeCode: String? = nil
    var companyId: String? = nil
    var onClose: () -> Void
    var onTile: (BusinessQuickAddKind) -> Void
    var onNewMoment: () -> Void = {}
    var onOpenCompanySettings: () -> Void = {}
    /// Legacy callbacks kept for AppShell wiring of live finance sheets.
    var onExpense: () -> Void = {}
    var onRevenue: () -> Void = {}
    var onInvoice: () -> Void = {}
    var onMembers: () -> Void = {}

    @State private var search = ""
    @State private var smallShop = false
    @State private var hubHint: String?

    private var theme: BusinessActiveTheme { .forTypeCode(momentTypeCode) }
    private var moduleEnabled: Bool {
        CompanyModules.isEnabled(companyId: companyId, key: CompanyModules.moduleKey(for: theme))
    }
    private var isRunway: Bool {
        theme.typeLabel == BusinessActiveTheme.businessRunway.typeLabel
    }
    private let columns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

    private var spec: BusinessMomentFamilySpec? {
        BusinessMomentFamilyConfig.quickAddSpec(momentTypeCode, smallShop: smallShop)
    }

    private func filtered(_ kinds: [BusinessQuickAddKind]) -> [BusinessQuickAddKind] {
        let visible = BusinessMomentFamilyConfig.visibleActions(
            kinds,
            capabilities: capabilityCodes,
            momentTypeCode: momentTypeCode
        )
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return visible }
        return visible.filter {
            $0.label(smallShop: smallShop).lowercased().contains(q) || $0.subtitle.lowercased().contains(q)
        }
    }

    private var primaryTiles: [BusinessQuickAddKind] { filtered(spec?.primary ?? []) }
    private var secondaryTiles: [BusinessQuickAddKind] { filtered(spec?.secondary ?? []) }

    private var hubSubtitleText: String {
        if smallShop, isRunway, let hubHint, !hubHint.isEmpty {
            return hubHint
        }
        return BusinessQuickAddKind.hubSubtitle(theme: theme, smallShop: smallShop)
    }

    private var filterChips: [String] {
        BusinessQuickAddKind.hubFilterChips(theme: theme, smallShop: smallShop)
    }

    private var disabledReasonText: String? {
        let financeKinds: [BusinessQuickAddKind] = [.revenue, .invoice, .expense, .spendEntry]
        let visible = primaryTiles + secondaryTiles
        let anyDisabled = financeKinds.contains { kind in
            visible.contains(kind) && !(
                (hasActiveMoment || kind == .memory)
                    && BusinessActionRegistry.isKindEnabled(
                        kind,
                        capabilities: capabilityCodes,
                        momentTypeCode: momentTypeCode
                    )
            )
        }
        guard anyDisabled else { return nil }
        return BusinessActionRegistry.disabledReason(
            hasActiveMoment: hasActiveMoment,
            momentTypeCode: momentTypeCode
        )
    }

    var body: some View {
        List {
            Section {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Quick Add")
                            .font(.plusJakarta(size: 24, weight: .heavy))
                            .foregroundStyle(.white)
                        Text(hubSubtitleText)
                            .font(.plusJakarta(size: 13, weight: .medium))
                            .foregroundStyle(theme.secondary)
                    }
                    Spacer()
                    Button(action: onClose) {
                        Image("TeamOpsQaClose")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 10, height: 10)
                            .frame(width: 24, height: 24)
                            .background(Color(hex: "#818CF8").opacity(0.9))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }

                if moduleEnabled, !filterChips.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip(theme.typeLabel, selected: true)
                            ForEach(filterChips, id: \.self) { chip($0, selected: false) }
                        }
                    }
                }
            }
            .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 4, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            if !moduleEnabled {
                Section {
                    VStack(spacing: 16) {
                        Text("This module is off for your company")
                            .font(.plusJakarta(size: 15, weight: .medium))
                            .foregroundStyle(theme.secondary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                        Button {
                            onClose()
                            onOpenCompanySettings()
                        } label: {
                            Text("Open company settings")
                                .font(.plusJakarta(size: 14, weight: .semibold))
                                .foregroundStyle(theme.accent)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(24)
                    .background(theme.card)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 16, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            } else {
            Section {
                HStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(theme.hubHeroTitle)
                            .font(.plusJakarta(size: 18, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(theme.hubHeroDetail)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color.white.opacity(0.7))
                    }
                    Spacer(minLength: 0)
                    Image(theme.hubHeroAssetName)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 100, height: 100)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(20)
                .frame(maxWidth: .infinity, minHeight: 140)
                .background(theme.heroGradient)
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.border))
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)

            Section {
                HStack(spacing: 10) {
                    Image("TeamOpsQaSearch")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                    TextField("Search actions...", text: $search)
                        .font(.plusJakarta(size: 13, weight: .medium))
                        .foregroundStyle(theme.text)
                }
                .padding(12)
                .background(theme.card)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(theme.border))
                .clipShape(RoundedRectangle(cornerRadius: 24))

                if let disabledReasonText {
                    Text(disabledReasonText)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(theme.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                actionGrid("Primary", kinds: primaryTiles)
                actionGrid("More", kinds: secondaryTiles)

                if primaryTiles.isEmpty && secondaryTiles.isEmpty {
                    Text("No actions match this search.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(theme.secondary)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(theme.card)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.border))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
            .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 16, trailing: 16))
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .safeAreaInset(edge: .bottom) {
            if moduleEnabled {
                Button(action: onNewMoment) {
                    Text("Create another Business Moment")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(theme.accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
                .background(theme.bg.opacity(0.92))
            }
        }
        .background(theme.bg.ignoresSafeArea())
        .task(id: "\(momentId ?? "")-\(companyId ?? "")") {
            if let companyId {
                smallShop = BusinessAudience.isSmallShop(BusinessAudience.forCompany(companyId: companyId))
                hubHint = IndustryTemplateCatalog.hubHintForCompany(companyId: companyId)
            }
            smallShop = await BusinessAudience.isSmallShopMoment(
                momentId: momentId,
                fallbackCompanyId: companyId
            )
            hubHint = IndustryTemplateCatalog.hubHintForCompany(companyId: companyId)
        }
    }

    @ViewBuilder
    private func actionGrid(_ title: String, kinds: [BusinessQuickAddKind]) -> some View {
        if !kinds.isEmpty {
            Text(title)
                .font(.plusJakarta(size: 14, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(kinds) { kind in
                    let capOk = BusinessActionRegistry.isKindEnabled(
                        kind,
                        capabilities: capabilityCodes,
                        momentTypeCode: momentTypeCode
                    )
                    let momentOk = hasActiveMoment || kind == .memory
                    Button { handle(kind) } label: {
                        let tall = kind == .activityLog || kind == .poll || kind == .memory
                        VStack(spacing: 8) {
                            if let icon = kind.teamOpsHubIconName {
                                Image(icon)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 40, height: 40)
                            } else {
                                Text(kind.emoji)
                                    .font(.system(size: 22))
                                    .frame(width: 40, height: 40)
                            }
                            Text(kind.label(smallShop: smallShop))
                                .font(.plusJakarta(size: 11, weight: .semibold))
                                .foregroundStyle(kind.stripeColor)
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .minimumScaleFactor(0.8)
                            Text(kind.subtitle)
                                .font(.plusJakarta(size: 9))
                                .foregroundStyle(theme.muted)
                                .lineLimit(1)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: tall ? 120 : 100)
                        .padding(8)
                        .background(
                            LinearGradient(
                                colors: [kind.stripeColor.opacity(0.12), .clear],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                            .background(theme.card)
                        )
                        .overlay(alignment: .leading) {
                            Rectangle()
                                .fill(kind.stripeColor)
                                .frame(width: 3)
                        }
                        .overlay(RoundedRectangle(cornerRadius: tall ? 20 : 16).stroke(theme.border))
                        .clipShape(RoundedRectangle(cornerRadius: tall ? 20 : 16))
                        .opacity(momentOk && capOk ? 1 : 0.45)
                    }
                    .buttonStyle(.plain)
                    .disabled(!(momentOk && capOk))
                }
            }
        }
    }

    private func chip(_ label: String, selected: Bool) -> some View {
        Text(label)
            .font(.plusJakarta(size: 10, weight: .bold))
            .foregroundStyle(selected ? Color.white : Color(hex: "#818CF8"))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(selected ? Color(hex: "#6366F1") : theme.card)
            .overlay(Capsule().stroke(Color(hex: "#6366F1").opacity(0.3)))
            .clipShape(Capsule())
    }

    private func handle(_ kind: BusinessQuickAddKind) {
        onTile(kind)
    }
}
