import SwiftUI

struct AppShellView: View {
    let identity: ShellIdentity
    @ObservedObject var model: AppShellModel
    var onSignOut: () -> Void
    var onSessionExpired: () -> Void = {}

    @StateObject private var createModel = MomentCreateModel()
    @State private var moneyQa: MoneyQuickAddKind? = nil
    @State private var groupExpenseSheetPresented = false
    @State private var groupContributionSheetPresented = false
    @State private var groupSettlementSheetPresented = false
    @State private var groupBudgetSheetPresented = false
    @State private var groupParticipantsSheetPresented = false
    @State private var groupInviteSheetPresented = false
    @State private var groupCollabKind: GroupCollabKind? = nil
    @State private var groupFinancePresented = false
    @State private var groupSplitsPresented = false
    @State private var weddingGapQa: WeddingQuickAddKind? = nil
    @State private var experienceGapQa: ExperienceQuickAddKind? = nil
    @State private var purchaseGapQa: PurchaseQuickAddKind? = nil
    @State private var livingGapQa: LivingQuickAddKind? = nil
    @State private var businessExpenseSheetPresented = false
    @State private var businessRevenueSheetPresented = false
    @State private var businessInvoiceSheetPresented = false
    @State private var businessMembersSheetPresented = false
    @State private var businessQuickAddPresented = false
    @State private var businessGapQa: BusinessQuickAddKind? = nil
    @State private var lifeOpsQa: LifeOpsQuickAddKind? = nil
    @State private var futureQa: FutureQuickAddKind? = nil
    @State private var lifestyleQa: LifestyleQuickAddKind? = nil
    @State private var relationshipsQa: RelationshipsQuickAddKind? = nil
    @State private var relationshipsActivityOpen = false
    @State private var recentActivityOpen = false
    @State private var newMomentOpen = false
    @State private var groupCreatePhase: GroupCreatePhase = .chooser
    @State private var showManageMoment = false
    @State private var editSetupTarget: EditMomentSetupTarget? = nil
    @State private var showJoinQrScanner = false
    @State private var showReferComingSoon = false
    @State private var pendingGroupJoin: PendingGroupJoin?
    @State private var companyMenuOpen = false
    @State private var joinFeedbackMessage: String?
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        shellPage
            .onAppear {
                model.bindIdentity(identity)
                if let pending = JoinInviteStore.shared.consume() {
                    pendingGroupJoin = PendingGroupJoin(id: pending)
                }
            }
            .onReceive(JoinInviteStore.shared.$pendingCode) { code in
                guard let code, !code.isEmpty else { return }
                guard pendingGroupJoin == nil else { return }
                if let pending = JoinInviteStore.shared.consume() {
                    pendingGroupJoin = PendingGroupJoin(id: pending)
                }
            }
            .onChange(of: identity.userId) { _, _ in
                model.bindIdentity(identity)
            }
            .onChange(of: model.selectedContext) { _, _ in
                newMomentOpen = false
                groupCreatePhase = .chooser
            }
            .onChange(of: model.bottomDestination) { _, destination in
                newMomentOpen = false
                if destination == .create, model.selectedContext == .group {
                    // Keep phase when advancing from Pulse type cards; reset only when tapping Create tab from chooser path is handled by openNewMoment / tab setter.
                }
            }
            .onChange(of: scenePhase) { _, phase in
                guard phase == .active else { return }
                if model.selectedContext == .group {
                    model.refreshVisibleGroupTab()
                }
            }
    }

    private var shellPage: some View {
        NativeShellTabView(
            selection: bottomTabSelection,
            accent: momentAccent,
            content: { tabNavigationRoot }
        )
        .background(Color(hex: "#14121B").ignoresSafeArea())
        .fullScreenCover(isPresented: $showManageMoment) {
            if let momentId = model.selectedMomentId {
                ManageMomentFlowSheet(
                    momentId: momentId,
                    momentTitle: model.selectedMomentTitle ?? "Moment",
                    isPresented: $showManageMoment,
                    onEditSetup: {
                        editSetupTarget = EditMomentSetupTarget.resolve(
                            context: model.selectedContext,
                            momentTypeCode: model.selectedMomentTypeCode
                        )
                    },
                    onLifecycleChanged: {
                        model.reloadCurrentContext()
                    }
                )
                .preferredColorScheme(.dark)
            }
        }
        .sheet(item: $editSetupTarget) { target in
            if let momentId = model.selectedMomentId {
                EditMomentSetupHost(
                    target: target,
                    momentId: momentId,
                    momentTitle: model.selectedMomentTitle ?? "",
                    momentTypeCode: model.selectedMomentTypeCode,
                    companyId: model.moments.first(where: { $0.momentId == momentId })?.companyId,
                    createModel: createModel,
                    onClose: { editSetupTarget = nil },
                    onSaved: {
                        editSetupTarget = nil
                        model.reloadCurrentContext()
                    }
                )
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(24)
                .preferredColorScheme(.dark)
            }
        }
        .sheet(item: $moneyQa) { kind in
            if let momentId = model.selectedMomentId {
                switch kind {
                case .masterExpense:
                    PersonalMasterExpenseSheet(
                        momentId: momentId,
                        pulseFamily: PersonalPulseFamily.forTypeCode(model.selectedMomentTypeCode),
                        onClose: { moneyQa = nil },
                        onSaved: {
                            moneyQa = nil
                            model.refreshVisiblePersonalTab()
                        }
                    )
                case .income, .transfer, .savings:
                    PersonalMoneyQuickAddSheet(
                        kind: kind,
                        momentId: momentId,
                        onClose: { moneyQa = nil },
                        onSaved: {
                            moneyQa = nil
                            model.refreshVisiblePersonalTab()
                        }
                    )
                }
            }
        }
        .sheet(isPresented: $groupExpenseSheetPresented) {
            if let momentId = model.selectedMomentId {
                GroupExpenseSheet(
                    momentId: momentId,
                    isPresented: $groupExpenseSheetPresented,
                    isWedding: GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode).isWedding,
                    momentTypeCode: model.selectedMomentTypeCode,
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(isPresented: $groupContributionSheetPresented) {
            if let momentId = model.selectedMomentId {
                GroupContributionSheet(
                    momentId: momentId,
                    isPresented: $groupContributionSheetPresented,
                    isWedding: GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode).isWedding,
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(isPresented: $groupSettlementSheetPresented) {
            if let momentId = model.selectedMomentId {
                GroupSettlementSheet(
                    momentId: momentId,
                    momentTypeCode: model.selectedMomentTypeCode,
                    isPresented: $groupSettlementSheetPresented,
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(isPresented: $groupBudgetSheetPresented) {
            if let momentId = model.selectedMomentId {
                GroupBudgetSheet(
                    momentId: momentId,
                    isPresented: $groupBudgetSheetPresented,
                    isWedding: GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode).isWedding,
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(isPresented: $groupParticipantsSheetPresented) {
            if let momentId = model.selectedMomentId {
                GroupParticipantsSheet(
                    momentId: momentId,
                    isPresented: $groupParticipantsSheetPresented,
                    isWedding: GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode).isWedding
                )
            }
        }
        .sheet(isPresented: $groupInviteSheetPresented) {
            if let momentId = model.selectedMomentId {
                let inviteTypeCode = model.selectedMomentTypeCode
                    ?? model.moments.first(where: { $0.momentId == momentId })?.momentTypeCode
                    ?? "TRIP"
                GroupInvitePeopleSheet(
                    momentId: momentId,
                    momentTitle: model.selectedMomentTitle ?? "Trip",
                    momentTypeCode: inviteTypeCode,
                    isPresented: $groupInviteSheetPresented,
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(item: $groupCollabKind) { kind in
            if let momentId = model.selectedMomentId {
                GroupCollabSheet(
                    kind: kind,
                    momentId: momentId,
                    isPresented: Binding(
                        get: { groupCollabKind != nil },
                        set: { if !$0 { groupCollabKind = nil } }
                    ),
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(item: $pendingGroupJoin) { pending in
            GroupJoinConfirmSheet(
                code: pending.code,
                onClose: { pendingGroupJoin = nil },
                onJoin: {
                    let code = pending.code
                    Task {
                        let result = await model.redeemJoinCode(code, using: createModel)
                        pendingGroupJoin = nil
                        newMomentOpen = false
                        groupCreatePhase = .chooser
                        if let result {
                            if result.alreadyMember == true {
                                joinFeedbackMessage = "Already a member"
                            } else if result.momentId == nil || result.momentId?.isEmpty == true {
                                joinFeedbackMessage =
                                    "Invite claimed — you’ll join when the organizer finishes creating the group."
                            } else {
                                joinFeedbackMessage = "Joined group"
                            }
                        }
                    }
                }
            )
        }
        .sheet(item: $weddingGapQa) { kind in
            WeddingGapQuickAddSheet(
                kind: kind,
                momentId: model.selectedMomentId,
                onClose: { weddingGapQa = nil },
                onSaved: { model.refreshVisibleGroupTab() }
            )
        }
        .sheet(item: $experienceGapQa) { kind in
            let family = GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode)
            ExperienceGapQuickAddSheet(
                theme: ExperienceActiveTheme.forFamily(family),
                kind: kind,
                momentId: model.selectedMomentId,
                momentTypeCode: model.selectedMomentTypeCode,
                onClose: { experienceGapQa = nil },
                onSaved: { model.refreshVisibleGroupTab() },
                onBooking: { groupCollabKind = .booking }
            )
        }
        .sheet(item: $purchaseGapQa) { kind in
            let family = GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode)
            PurchaseGapQuickAddSheet(
                theme: PurchaseActiveTheme.forFamily(family),
                kind: kind,
                momentId: model.selectedMomentId,
                momentTypeCode: model.selectedMomentTypeCode,
                onClose: { purchaseGapQa = nil },
                onSaved: { model.refreshVisibleGroupTab() }
            )
        }
        .sheet(item: $livingGapQa) { kind in
            let family = GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode)
            LivingGapQuickAddSheet(
                theme: LivingActiveTheme.forFamily(family),
                kind: kind,
                momentId: model.selectedMomentId,
                momentTypeCode: model.selectedMomentTypeCode,
                onClose: { livingGapQa = nil },
                onSaved: { model.refreshVisibleGroupTab() }
            )
        }
        .fullScreenCover(isPresented: $showJoinQrScanner) {
            GroupJoinQrScanner(
                onCode: { code in
                    showJoinQrScanner = false
                    redeemJoinCode(code)
                },
                onCompanyCode: { code in
                    showJoinQrScanner = false
                    redeemCompanyInviteCode(code)
                },
                onDismiss: { showJoinQrScanner = false }
            )
        }
        .alert("Referrals coming soon", isPresented: $showReferComingSoon) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Invite sharing will be available in a future release.")
        }
        .alert(
            "Group invite",
            isPresented: Binding(
                get: { joinFeedbackMessage != nil },
                set: { if !$0 { joinFeedbackMessage = nil } }
            )
        ) {
            Button("OK", role: .cancel) { joinFeedbackMessage = nil }
        } message: {
            Text(joinFeedbackMessage ?? "")
        }
        .fullScreenCover(isPresented: $groupFinancePresented) {
            if let momentId = model.selectedMomentId {
                let family = GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode)
                GroupFinanceDetailView(
                    momentId: momentId,
                    momentTitle: model.selectedMomentTitle,
                    isWedding: family.isWedding,
                    experienceFamily: family,
                    onClose: { groupFinancePresented = false },
                    onOpenSplits: {
                        groupFinancePresented = false
                        groupSplitsPresented = true
                    },
                    onSettle: {
                        groupFinancePresented = false
                        groupSettlementSheetPresented = true
                    }
                )
            }
        }
        .fullScreenCover(isPresented: $groupSplitsPresented) {
            if let momentId = model.selectedMomentId {
                let family = GroupExperienceFamily.forTypeCode(model.selectedMomentTypeCode)
                GroupExpenseSplitsView(
                    momentId: momentId,
                    momentTitle: model.selectedMomentTitle,
                    isWedding: family.isWedding,
                    experienceFamily: family,
                    onClose: { groupSplitsPresented = false },
                    onOpenFinance: {
                        groupSplitsPresented = false
                        groupFinancePresented = true
                    }
                )
            }
        }
        .sheet(isPresented: $businessExpenseSheetPresented) {
            if let momentId = model.selectedMomentId {
                BusinessExpenseSheet(
                    momentId: momentId,
                    isPresented: $businessExpenseSheetPresented,
                    onSaved: { model.refreshVisibleBusinessTab() }
                )
            }
        }
        .sheet(isPresented: $businessRevenueSheetPresented) {
            if let momentId = model.selectedMomentId {
                BusinessRevenueSheet(
                    momentId: momentId,
                    isPresented: $businessRevenueSheetPresented,
                    onSaved: { model.refreshVisibleBusinessTab() }
                )
            }
        }
        .sheet(isPresented: $businessInvoiceSheetPresented) {
            if let momentId = model.selectedMomentId {
                BusinessInvoiceSheet(
                    momentId: momentId,
                    isPresented: $businessInvoiceSheetPresented,
                    onSaved: { model.refreshVisibleBusinessTab() }
                )
            }
        }
        .sheet(isPresented: $businessMembersSheetPresented) {
            if let companyId = model.selectedCompany?.companyId {
                BusinessMembersSheet(
                    companyId: companyId,
                    isPresented: $businessMembersSheetPresented
                )
            }
        }
        .sheet(isPresented: $businessQuickAddPresented) {
            BusinessQuickAddHub(
                hasActiveMoment: model.selectedMomentId != nil,
                hasCompany: model.selectedCompany != nil,
                capabilityCodes: model.capabilities,
                momentTypeCode: model.selectedMomentTypeCode,
                onClose: { businessQuickAddPresented = false },
                onTile: { kind in
                    businessQuickAddPresented = false
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    let isOps = code.contains("OPERATIONS") && !code.contains("TEAM")
                    switch kind {
                    case .expense, .spendEntry:
                        if isOps {
                            businessGapQa = .spendEntry
                        } else {
                            businessExpenseSheetPresented = true
                        }
                    case .revenue:
                        if code.contains("RUNWAY") { businessRevenueSheetPresented = true }
                    case .invoice:
                        if code.contains("RUNWAY") { businessInvoiceSheetPresented = true }
                    default:
                        businessGapQa = kind
                    }
                },
                onNewMoment: {
                    businessQuickAddPresented = false
                    model.selectBottomDestination(.create)
                },
                onExpense: {
                    businessQuickAddPresented = false
                    businessExpenseSheetPresented = true
                },
                onRevenue: {
                    businessQuickAddPresented = false
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    if code.contains("RUNWAY") { businessRevenueSheetPresented = true }
                },
                onInvoice: {
                    businessQuickAddPresented = false
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    if code.contains("RUNWAY") { businessInvoiceSheetPresented = true }
                },
                onMembers: {
                    businessQuickAddPresented = false
                    businessMembersSheetPresented = true
                }
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(item: $businessGapQa) { kind in
            let code = (model.selectedMomentTypeCode ?? "").uppercased()
            Group {
                if code.contains("RUNWAY") {
                    RunwayQuickAddSheet(
                        kind: kind,
                        momentId: model.selectedMomentId,
                        onClose: { businessGapQa = nil },
                        onSaved: { model.refreshVisibleBusinessTab() }
                    )
                } else if code.contains("OPERATIONS") && !code.contains("TEAM"), OpsQuickAddSheets.isOpsKind(kind) {
                    OpsQuickAddSheet(
                        kind: kind,
                        momentId: model.selectedMomentId,
                        companyId: model.selectedCompany?.companyId,
                        momentTitle: model.selectedMomentTitle,
                        onClose: { businessGapQa = nil },
                        onSaved: { model.refreshVisibleBusinessTab() }
                    )
                } else if code.contains("TEAM_OPERATIONS"), TeamOpsQuickAddSheets.isTeamOpsKind(kind) {
                    TeamOpsGapQuickAddSheet(
                        kind: kind,
                        momentId: model.selectedMomentId,
                        onClose: { businessGapQa = nil },
                        onSaved: { model.refreshVisibleBusinessTab() }
                    )
                } else {
                    BusinessGapQuickAddSheet(
                        theme: BusinessActiveTheme.forTypeCode(model.selectedMomentTypeCode),
                        kind: kind,
                        momentId: model.selectedMomentId,
                        onClose: { businessGapQa = nil },
                        onSaved: { model.refreshVisibleBusinessTab() },
                        onExpense: { businessExpenseSheetPresented = true },
                        onRevenue: {
                            let code = (model.selectedMomentTypeCode ?? "").uppercased()
                            if code.contains("RUNWAY") { businessRevenueSheetPresented = true }
                        },
                        onInvoice: {
                            let code = (model.selectedMomentTypeCode ?? "").uppercased()
                            if code.contains("RUNWAY") { businessInvoiceSheetPresented = true }
                        }
                    )
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(item: $lifeOpsQa) { kind in
            if let momentId = model.selectedMomentId {
                PersonalLifeOpsQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: { lifeOpsQa = nil },
                    onSaved: {
                        lifeOpsQa = nil
                        model.refreshVisiblePersonalTab()
                    }
                )
            }
        }
        .sheet(item: $futureQa) { kind in
            if let momentId = model.selectedMomentId {
                PersonalFutureQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: { futureQa = nil },
                    onSaved: {
                        futureQa = nil
                        model.refreshVisiblePersonalTab()
                    }
                )
            }
        }
        .sheet(item: $lifestyleQa) { kind in
            if let momentId = model.selectedMomentId {
                PersonalLifestyleQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: { lifestyleQa = nil },
                    onSaved: {
                        lifestyleQa = nil
                        model.refreshVisiblePersonalTab()
                    }
                )
            }
        }
        .sheet(item: $relationshipsQa) { kind in
            if let momentId = model.selectedMomentId {
                PersonalRelationshipsQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: { relationshipsQa = nil },
                    onSaved: {
                        relationshipsQa = nil
                        model.refreshVisiblePersonalTab()
                    }
                )
            }
        }
        .sheet(isPresented: $relationshipsActivityOpen) {
            PersonalRelationshipsActivityFlow(
                momentId: model.selectedMomentId,
                isPresented: $relationshipsActivityOpen,
                onChanged: { model.refreshVisiblePersonalTab() }
            )
        }
        .sheet(isPresented: $recentActivityOpen) {
            PersonalRecentActivityFlow(
                momentId: model.selectedMomentId,
                isPresented: $recentActivityOpen,
                onChanged: { model.refreshVisiblePersonalTab() }
            )
        }
        .sheet(isPresented: Binding(
            get: { model.life360Open },
            set: { model.openLife360($0) }
        )) {
            Life360ComingSoonView(onClose: { model.openLife360(false) })
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: Binding(
            get: { model.profileOpen },
            set: { model.openProfile($0) }
        )) {
            AccountHubView(
                identity: model.identity ?? identity,
                onSignOut: {
                    model.openProfile(false)
                    onSignOut()
                },
                onClose: { model.openProfile(false) },
                onAccountDeleted: {
                    model.openProfile(false)
                    onSignOut()
                }
            )
        }
    }

    private func openNewMoment() {
        switch model.selectedContext {
        case .personal:
            if model.selectedMomentId != nil, case .ready = model.contextContent {
                newMomentOpen = true
            } else {
                model.selectBottomDestination(.create)
            }
        case .group:
            groupCreatePhase = .chooser
            if model.selectedMomentId != nil, case .ready = model.contextContent {
                newMomentOpen = true
            } else {
                model.selectBottomDestination(.create)
            }
        case .business:
            newMomentOpen = true
        default:
            model.selectBottomDestination(.create)
        }
    }

    private func redeemJoinCode(_ code: String) {
        pendingGroupJoin = PendingGroupJoin(id: code)
    }

    private func redeemCompanyInviteCode(_ code: String) {
        Task {
            await model.redeemCompanyInviteCode(code, using: createModel)
            newMomentOpen = false
        }
    }

    private var shellAccent: Color {
        switch model.selectedContext {
        case .personal:
            return Color(hex: "#7C5CFC")
        case .circle:
            return CircleComingSoonTheme.selectedTab
        case .group:
            return Color(hex: "#E8621A")
        case .business:
            return Color(hex: "#818CF8")
        }
    }

    /// Moment-type accent (Wedding pink, Trip peach). Used for tab bar + moment chrome.
    private var momentAccent: Color {
        MomentThemes.resolve(
            context: model.selectedContext,
            momentTypeCode: model.selectedMomentTypeCode
        ).primary
    }

    private var shouldShowMomentSwitcher: Bool {
        if model.bottomDestination == .create { return false }
        switch model.selectedContext {
        case .circle:
            return false
        case .personal, .group, .business:
            if model.contextContent == .empty { return false }
            if model.contextContent == .loading || model.contextContent == .idle { return false }
            return model.showMomentSwitcher
        }
    }

    private var bottomTabSelection: Binding<BottomDestination> {
        Binding(
            get: { model.bottomDestination },
            set: { next in
                newMomentOpen = false
                if next == .create, model.selectedContext == .group, model.bottomDestination != .create {
                    groupCreatePhase = .chooser
                }
                model.selectBottomDestination(next)
            }
        )
    }

    private var showPersonalExpenseFab: Bool {
        model.selectedContext == .personal &&
        model.selectedMomentId != nil &&
        !newMomentOpen &&
        [.pulse, .moments, .life, .memory].contains(model.bottomDestination)
    }

    private var shellNavigationTitle: String {
        if newMomentOpen { return "New Moment" }
        if model.bottomDestination == .create { return "" }
        return "\(model.selectedContext.label) · \(model.bottomDestination.label)"
    }

    private var momentSwitcherIsEmpty: Bool {
        model.moments.filter(\.isActiveStatus).isEmpty
    }

    private var momentSwitcherIsLoading: Bool {
        switch model.contextContent {
        case .loading, .idle:
            return true
        default:
            return false
        }
    }

    private var activeMomentPairs: [(String, String)] {
        model.moments.filter(\.isActiveStatus).map { ($0.momentId, $0.title) }
    }

    @ViewBuilder
    private var tabNavigationRoot: some View {
        NavigationStack {
            destinationBodyWithFab
                .navigationTitle("")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar(.hidden, for: .navigationBar)
                .safeAreaInset(edge: .top, spacing: 0) {
                    shellTopChrome
                }
        }
    }

    @ViewBuilder
    private var shellTopChrome: some View {
        if model.bottomDestination == .create {
            EmptyView()
        } else {
            shellTopChromeContent
        }
    }

    private var shellTopChromeContent: some View {
        VStack(spacing: 0) {
            MomentraTopBar(
                context: model.selectedContext,
                displayName: identity.displayName,
                companies: model.companies,
                selectedCompany: model.selectedCompany,
                companyMenuOpen: $companyMenuOpen,
                onCompanySelected: model.selectCompany,
                onQrScan: (model.selectedContext == .group || model.selectedContext == .business)
                    ? { showJoinQrScanner = true }
                    : nil,
                onLife360: { model.openLife360(true) },
                onNewMoment: openNewMoment,
                onRefer: { showReferComingSoon = true },
                onAvatar: { model.openProfile(true) }
            )

            if !shellNavigationTitle.isEmpty {
                Text(shellNavigationTitle)
                    .font(.system(size: 34, weight: .bold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.top, 4)
                    .padding(.bottom, 8)
            }

            if model.bottomDestination != .create {
                ShellContextInset(
                    selected: model.selectedContext,
                    supportedContexts: model.supportedContexts,
                    onSelect: model.selectContext
                )
            }
            if shouldShowMomentSwitcher && !newMomentOpen {
                MomentSwitcherView(
                    selectedTitle: model.selectedMomentTitle,
                    selectedMomentId: model.selectedMomentId,
                    activeMoments: activeMomentPairs,
                    isEmpty: momentSwitcherIsEmpty,
                    isLoading: momentSwitcherIsLoading,
                    accent: momentAccent,
                    onSelectMoment: model.selectMoment,
                    onSettings: {
                        guard model.selectedMomentId != nil else { return }
                        showManageMoment = true
                    },
                    onInvite: model.selectedContext == .group ? { groupInviteSheetPresented = true } : nil
                )
            }
        }
        .background(GlobalTheme.topBarBackground)
    }

    @ViewBuilder
    private var destinationBodyWithFab: some View {
        destinationBody
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if showPersonalExpenseFab {
                    HStack {
                        Spacer()
                        PersonalExpenseFab { moneyQa = .masterExpense }
                    }
                    .padding(.trailing, 16)
                    .padding(.bottom, 8)
                }
            }
    }

    @ViewBuilder
    private var destinationBody: some View {
        if newMomentOpen, model.selectedContext == .personal {
            PersonalCreateEmptyView(
                history: model.moments,
                onMomentCreated: { id, title, typeCode in
                    newMomentOpen = false
                    model.onMomentCreated(momentId: id, title: title, momentTypeCode: typeCode)
                }
            )
        } else if newMomentOpen, model.selectedContext == .group {
            groupCreateBody
        } else if newMomentOpen, model.selectedContext == .business {
            if let companyId = model.selectedCompany?.companyId {
                BusinessCreateFlowView(
                    createModel: createModel,
                    companyId: companyId,
                    onBack: { newMomentOpen = false },
                    onCreated: { outcome in
                        newMomentOpen = false
                        model.onMomentCreated(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode
                        )
                    }
                )
            } else {
                CompanySetupFlowView(
                    onClose: { newMomentOpen = false },
                    onActivated: { model.onCompanyCreated($0) }
                )
            }
        } else {
            switch model.contextContent {
            case .idle, .loading:
                ProgressView()
                    .tint(MomentraBrandTokens.cta)
            case .offline:
                emptyPanel(
                    title: "You're offline",
                    body: "Check your connection and try again.",
                    action: "Retry"
                ) {
                    model.selectContext(model.selectedContext)
                }
            case .error(let code, let message):
                if code == "UNAUTHORIZED" {
                    Color.clear.onAppear(perform: onSessionExpired)
                }
                emptyPanel(title: "We couldn't load your moments", body: message, action: "Retry") {
                    model.selectContext(model.selectedContext)
                }
            case .forbidden:
                emptyPanel(
                    title: "No access",
                    body: "You no longer have access to this \(model.selectedContext.label.lowercased()) resource. Your session stays signed in."
                )
            case .deferred:
                CircleComingSoonView()
            case .empty:
                ContextEmptyExperienceView(
                    createModel: createModel,
                    context: model.selectedContext,
                    destination: model.bottomDestination,
                    experience: model.momentExperience,
                    moments: model.moments,
                    hasCompany: model.selectedCompany != nil,
                    selectedCompanyId: model.selectedCompany?.companyId,
                    onCreateMoment: openNewMoment,
                    onCreateBack: {
                        if model.selectedContext == .group, groupCreatePhase != .chooser {
                            groupCreatePhase = .chooser
                        } else {
                            groupCreatePhase = .chooser
                            model.exitCreateDestination()
                        }
                    },
                    onCompanyActivated: { model.onCompanyCreated($0) },
                    onMomentCreated: { outcome in
                        groupCreatePhase = .chooser
                        model.onMomentCreated(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode
                        )
                    },
                    groupCreatePhase: groupCreatePhase,
                    onSelectExperience: {
                        groupCreatePhase = .experienceSetup
                        model.selectBottomDestination(.create)
                    },
                    onSelectPurchase: {
                        groupCreatePhase = .purchaseSetup
                        model.selectBottomDestination(.create)
                    },
                    onSelectLiving: {
                        groupCreatePhase = .livingSetup
                        model.selectBottomDestination(.create)
                    },
                    onExitGroupSetup: { groupCreatePhase = .chooser },
                    onJoinCode: redeemJoinCode
                )
            case .ready(let detail):
                let personalTypeCode = model.selectedMomentTypeCode
                    ?? model.moments.first(where: { $0.momentId == model.selectedMomentId })?.momentTypeCode
                let personalFamily = PersonalPulseFamily.forTypeCode(personalTypeCode)
                let isLifeOps = personalFamily == .lifeOperations
                let isFutureBuilding = personalFamily == .futureBuilding
                let isLifestyle = personalFamily == .lifestyle
                let isRelationships = personalFamily == .relationships
                let groupTypeCode = model.selectedMomentTypeCode
                    ?? model.moments.first(where: { $0.momentId == model.selectedMomentId })?.momentTypeCode
                let groupFamily = GroupExperienceFamily.forTypeCode(groupTypeCode)
                let isWedding = groupFamily.isWedding
                let isExperience = groupFamily.isThemedExperience
                let isPurchase = groupFamily.isThemedPurchase
                let isLiving = groupFamily.isThemedLiving
                let experienceTheme = ExperienceActiveTheme.forFamily(groupFamily)
                let purchaseTheme = PurchaseActiveTheme.forFamily(groupFamily)
                let livingTheme = LivingActiveTheme.forFamily(groupFamily)
                if model.selectedContext == .group, model.bottomDestination == .pulse {
                    if isWedding {
                        WeddingPulseActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in weddingGapQa = kind }
                        )
                    } else if isExperience {
                        ExperiencePulseActiveView(
                            theme: experienceTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in experienceGapQa = kind }
                        )
                    } else if isPurchase {
                        PurchasePulseActiveView(
                            theme: purchaseTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in purchaseGapQa = kind }
                        )
                    } else if isLiving {
                        LivingPulseActiveView(
                            theme: livingTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in livingGapQa = kind }
                        )
                    } else {
                        GroupPulseActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onOpenMemory: { groupCollabKind = .memory },
                            onOpenChat: { groupCollabKind = .update },
                            onOpenItinerary: { groupCollabKind = .planning }
                        )
                    }
                } else if model.selectedContext == .group, model.bottomDestination == .moments {
                    if isWedding {
                        WeddingMomentsActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else if isExperience {
                        ExperienceMomentsActiveView(
                            theme: experienceTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else if isPurchase {
                        PurchaseMomentsActiveView(
                            theme: purchaseTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else if isLiving {
                        LivingMomentsActiveView(
                            theme: livingTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else {
                        GroupMomentsActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onCreateMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            }
                        )
                    }
                } else if model.selectedContext == .group, model.bottomDestination == .life {
                    GroupLifeActiveView(
                        refreshToken: model.groupTabRefreshToken,
                        momentId: model.selectedMomentId,
                        momentTitle: model.selectedMomentTitle,
                        onQuickAction: { action in
                            switch action {
                            case .experience, .goal:
                                if isWedding { weddingGapQa = .planning }
                                else if isExperience { experienceGapQa = .planning }
                                else if isPurchase { purchaseGapQa = .purchaseItem }
                                else if isLiving { livingGapQa = .task }
                                else { groupCollabKind = .planning }
                            case .purchase:
                                if isWedding { weddingGapQa = .expense }
                                else if isExperience { experienceGapQa = .expense }
                                else if isPurchase { purchaseGapQa = .expense }
                                else if isLiving { livingGapQa = .expense }
                                else { groupExpenseSheetPresented = true }
                            case .living:
                                if isWedding { weddingGapQa = .vendor }
                                else if isExperience { experienceGapQa = experienceTheme.includesVendor ? .vendor : .booking }
                                else if isPurchase { purchaseGapQa = purchaseTheme.includesVendor ? .vendor : .contribution }
                                else if isLiving { livingGapQa = .resident }
                                else { groupCollabKind = .booking }
                            case .community:
                                if isWedding { weddingGapQa = .update }
                                else if isExperience { experienceGapQa = .update }
                                else if isPurchase { purchaseGapQa = .update }
                                else if isLiving { livingGapQa = .update }
                                else { groupCollabKind = .update }
                            }
                        }
                    )
                } else if model.selectedContext == .group, model.bottomDestination == .memory {
                    if isWedding {
                        WeddingMemoryActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { weddingGapQa = .memory }
                        )
                    } else if isExperience {
                        ExperienceMemoryActiveView(
                            theme: experienceTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { experienceGapQa = .memory }
                        )
                    } else if isPurchase {
                        PurchaseMemoryActiveView(
                            theme: purchaseTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { purchaseGapQa = .memory }
                        )
                    } else if isLiving {
                        LivingMemoryActiveView(
                            theme: livingTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { livingGapQa = .memory }
                        )
                    } else {
                        GroupMemoryActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onOpenQuickAdd: { groupCollabKind = .memory }
                        )
                    }
                } else if model.selectedContext == .group, model.bottomDestination == .create {
                    if isWedding {
                        WeddingQuickAddHubView(
                            momentTitle: model.selectedMomentTitle,
                            hasActiveMoment: model.selectedMomentId != nil,
                            capabilityCodes: model.capabilities,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                weddingGapQa = kind
                            },
                            onNewMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            },
                            onJoinCode: redeemJoinCode
                        )
                    } else if isExperience {
                        ExperienceQuickAddHubView(
                            theme: experienceTheme,
                            momentTitle: model.selectedMomentTitle,
                            hasActiveMoment: model.selectedMomentId != nil,
                            capabilityCodes: model.capabilities,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in experienceGapQa = kind },
                            onNewMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            },
                            onJoinCode: redeemJoinCode
                        )
                    } else if isPurchase {
                        PurchaseQuickAddHubView(
                            theme: purchaseTheme,
                            momentTitle: model.selectedMomentTitle,
                            hasActiveMoment: model.selectedMomentId != nil,
                            capabilityCodes: model.capabilities,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in purchaseGapQa = kind },
                            onNewMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            },
                            onJoinCode: redeemJoinCode
                        )
                    } else if isLiving {
                        LivingQuickAddHubView(
                            theme: livingTheme,
                            momentTitle: model.selectedMomentTitle,
                            hasActiveMoment: model.selectedMomentId != nil,
                            capabilityCodes: model.capabilities,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in livingGapQa = kind },
                            onNewMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            },
                            onJoinCode: redeemJoinCode
                        )
                    } else {
                        GroupQuickAddHubView(
                            hasActiveMoment: model.selectedMomentId != nil,
                            capabilityCodes: model.capabilities,
                            momentTypeCode: model.selectedMomentTypeCode,
                            momentTitle: model.selectedMomentTitle,
                            onClose: { model.exitCreateDestination() },
                            onExpense: { groupExpenseSheetPresented = true },
                            onContribution: { groupContributionSheetPresented = true },
                            onSettle: { groupSettlementSheetPresented = true },
                            onParticipants: { groupParticipantsSheetPresented = true },
                            onInvite: { groupInviteSheetPresented = true },
                            onBudget: { groupBudgetSheetPresented = true },
                            onPlanning: { groupCollabKind = .planning },
                            onBooking: { groupCollabKind = .booking },
                            onPoll: { groupCollabKind = .poll },
                            onUpdate: { groupCollabKind = .update },
                            onMemory: { groupCollabKind = .memory },
                            onPurchaseItem: { groupCollabKind = .purchaseItem },
                            onResident: { groupCollabKind = .resident },
                            onNewMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            },
                            onJoinCode: redeemJoinCode
                        )
                    }
                } else if model.selectedContext == .personal, model.bottomDestination == .pulse, isRelationships {
                    PersonalRelationshipsPulseActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentTitle: model.selectedMomentTitle,
                        momentId: model.selectedMomentId,
                        onAddExpense: { moneyQa = .masterExpense },
                        onRelationshipsQuickAdd: { relationshipsQa = $0 },
                        onOpenRecentActivity: { relationshipsActivityOpen = true }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .pulse, isLifestyle {
                    PersonalLifestylePulseActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentTitle: model.selectedMomentTitle,
                        momentId: model.selectedMomentId,
                        onAddExpense: { moneyQa = .masterExpense },
                        onLifestyleQuickAdd: { lifestyleQa = $0 },
                        onViewAllActivity: { recentActivityOpen = true }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .pulse {
                    PersonalPulseActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentTitle: model.selectedMomentTitle,
                        momentId: model.selectedMomentId,
                        momentTypeCode: personalTypeCode,
                        onAddExpense: { moneyQa = .masterExpense },
                        onLifeOpsQuickAdd: { lifeOpsQa = $0 },
                        onFutureQuickAdd: { futureQa = $0 },
                        onLifestyleQuickAdd: { lifestyleQa = $0 },
                        onViewAllActivity: { recentActivityOpen = true }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .moments, isFutureBuilding {
                    PersonalFutureMomentsActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        momentTitle: model.selectedMomentTitle,
                        onOpenQuickAdd: { model.selectBottomDestination(.create) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .moments, isLifestyle {
                    PersonalLifestyleMomentsActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        momentTitle: model.selectedMomentTitle,
                        onOpenQuickAdd: { model.selectBottomDestination(.create) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .moments, isRelationships {
                    PersonalRelationshipsMomentsActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        momentTitle: model.selectedMomentTitle,
                        onOpenQuickAdd: { model.selectBottomDestination(.create) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .moments, isLifeOps {
                    PersonalLifeOpsMomentsActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        momentTitle: model.selectedMomentTitle,
                        onOpenQuickAdd: { model.selectBottomDestination(.create) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isFutureBuilding {
                    PersonalFutureMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectMilestone: { futureQa = .milestone }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isLifestyle {
                    PersonalLifestyleMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectRitual: { lifestyleQa = .experience }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isRelationships {
                    PersonalRelationshipsMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectConnection: { relationshipsQa = .connection }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isLifeOps {
                    PersonalLifeOpsMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectRecovery: { lifeOpsQa = .recovery }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .life {
                    PersonalLifeActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        onLogRecovery: { lifeOpsQa = .recovery }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .create {
                    PersonalQuickAddHubView(
                        hasActiveMoment: model.selectedMomentId != nil,
                        momentTypeCode: personalTypeCode,
                        capabilityCodes: model.capabilities,
                        onClose: { model.exitCreateDestination() },
                        onIncome: { moneyQa = .income },
                        onRecovery: { lifeOpsQa = .recovery },
                        onMood: { lifeOpsQa = .mood },
                        onAttention: { lifeOpsQa = .attention },
                        onAdjust: { lifeOpsQa = .adjust },
                        onTransfer: { moneyQa = .transfer },
                        onSavings: { moneyQa = .savings },
                        onFutureQuickAdd: { futureQa = $0 },
                        onLifestyleQuickAdd: { lifestyleQa = $0 },
                        onRelationshipsQuickAdd: { relationshipsQa = $0 }
                    )
                } else if model.selectedContext == .business, model.bottomDestination == .pulse {
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    if code.contains("RUNWAY") {
                        RunwayPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogExpense: { businessGapQa = .expense },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    } else if code.contains("OPERATIONS") && !code.contains("TEAM") {
                        OpsPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogSpend: { businessGapQa = .spendEntry },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    } else if code.contains("TEAM_OPERATIONS") {
                        TeamOpsPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogDelivery: { businessGapQa = .teamUpdate },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    } else {
                        BusinessPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            momentTypeCode: model.selectedMomentTypeCode,
                            onAddExpense: { businessExpenseSheetPresented = true },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    }
                } else if model.selectedContext == .business, model.bottomDestination == .moments {
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    if code.contains("RUNWAY") {
                        RunwayMomentsActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogExpense: { businessGapQa = .expense },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    } else if code.contains("OPERATIONS") && !code.contains("TEAM") {
                        OpsMomentsActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onLogSpend: { businessGapQa = .spendEntry },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    } else if code.contains("TEAM_OPERATIONS") {
                        TeamOpsMomentsActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogWin: { businessGapQa = .teamUpdate },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    } else {
                        BusinessMomentsActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            momentTypeCode: model.selectedMomentTypeCode,
                            onAddExpense: { businessExpenseSheetPresented = true },
                            onOpenQuickAdd: { businessQuickAddPresented = true }
                        )
                    }
                } else if model.selectedContext == .business, model.bottomDestination == .life {
                    BusinessLifeActiveView(
                        refreshToken: model.businessTabRefreshToken,
                        momentId: model.selectedMomentId,
                        momentTitle: model.selectedMomentTitle,
                        momentTypeCode: model.selectedMomentTypeCode,
                        onViewReport: { model.selectBottomDestination(.pulse) }
                    )
                } else if model.selectedContext == .business, model.bottomDestination == .memory {
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    if code.contains("RUNWAY") {
                        RunwayMemoryActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onRecordLearning: { businessGapQa = .memory }
                        )
                    } else if code.contains("OPERATIONS") && !code.contains("TEAM") {
                        OpsMemoryActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onRecordLearning: { businessGapQa = .memory }
                        )
                    } else if code.contains("TEAM_OPERATIONS") {
                        TeamOpsMemoryActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onRecordLearning: { businessGapQa = .memory },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else {
                        BusinessMemoryActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            momentTypeCode: model.selectedMomentTypeCode,
                            onOpenQuickAdd: { businessGapQa = .memory }
                        )
                    }
                } else if model.selectedContext == .business, model.bottomDestination == .create {
                    if model.selectedMomentId != nil, model.selectedCompany != nil {
                        BusinessQuickAddHub(
                            hasActiveMoment: true,
                            hasCompany: true,
                            capabilityCodes: model.capabilities,
                            momentTypeCode: model.selectedMomentTypeCode,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                let code = (model.selectedMomentTypeCode ?? "").uppercased()
                                if code.contains("RUNWAY")
                                    || (code.contains("OPERATIONS") && !code.contains("TEAM"))
                                {
                                    businessGapQa = kind
                                } else {
                                    switch kind {
                                    case .expense, .spendEntry:
                                        businessExpenseSheetPresented = true
                                    case .revenue:
                                        if code.contains("RUNWAY") { businessRevenueSheetPresented = true }
                                    case .invoice:
                                        if code.contains("RUNWAY") { businessInvoiceSheetPresented = true }
                                    default:
                                        businessGapQa = kind
                                    }
                                }
                            },
                            onNewMoment: { newMomentOpen = true },
                            onExpense: { businessExpenseSheetPresented = true },
                            onRevenue: {
                                let code = (model.selectedMomentTypeCode ?? "").uppercased()
                                if code.contains("RUNWAY") { businessRevenueSheetPresented = true }
                            },
                            onInvoice: {
                                let code = (model.selectedMomentTypeCode ?? "").uppercased()
                                if code.contains("RUNWAY") { businessInvoiceSheetPresented = true }
                            },
                            onMembers: { businessMembersSheetPresented = true }
                        )
                    } else {
                        ContextEmptyExperienceView(
                            createModel: createModel,
                            context: model.selectedContext,
                            destination: model.bottomDestination,
                            experience: model.momentExperience,
                            moments: model.moments,
                            hasCompany: model.selectedCompany != nil,
                            selectedCompanyId: model.selectedCompany?.companyId,
                            onCreateMoment: { model.selectBottomDestination(.create) },
                            onCreateBack: { model.exitCreateDestination() },
                            onCompanyActivated: { model.onCompanyCreated($0) },
                            onMomentCreated: { outcome in
                                model.onMomentCreated(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode
                        )
                            }
                        )
                    }
                } else {
                    emptyPanel(
                        title: "\(model.selectedContext.label) · \(model.bottomDestination.label)",
                        body: detail ?? "Active Moment ready. Product features arrive in later phases."
                    )
                }
            }
        }
    }

    @ViewBuilder
    private var groupCreateBody: some View {
        GroupCreateMomentView(
            onBack: {
                groupCreatePhase = .chooser
                if newMomentOpen {
                    newMomentOpen = false
                } else {
                    model.exitCreateDestination()
                }
            },
            onSelectExperience: { groupCreatePhase = .experienceSetup },
            onSelectPurchase: { groupCreatePhase = .purchaseSetup },
            onSelectLiving: { groupCreatePhase = .livingSetup },
            onJoinCode: redeemJoinCode
        )
        .sheet(isPresented: Binding(
            get: {
                switch groupCreatePhase {
                case .experienceSetup, .purchaseSetup, .livingSetup: return true
                case .chooser: return false
                }
            },
            set: { if !$0 { groupCreatePhase = .chooser } }
        )) {
            Group {
                switch groupCreatePhase {
                case .experienceSetup:
                    GroupExperienceSetupView(
                        createModel: createModel,
                        onBack: { groupCreatePhase = .chooser },
                        onCreated: { outcome in
                            groupCreatePhase = .chooser
                            newMomentOpen = false
                            model.onMomentCreated(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode
                        )
                        }
                    )
                case .purchaseSetup:
                    GroupPurchaseSetupView(
                        createModel: createModel,
                        onBack: { groupCreatePhase = .chooser },
                        onCreated: { outcome in
                            groupCreatePhase = .chooser
                            newMomentOpen = false
                            model.onMomentCreated(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode
                        )
                        }
                    )
                case .livingSetup:
                    GroupLivingSetupView(
                        createModel: createModel,
                        onBack: { groupCreatePhase = .chooser },
                        onCreated: { outcome in
                            groupCreatePhase = .chooser
                            newMomentOpen = false
                            model.onMomentCreated(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode
                        )
                        }
                    )
                case .chooser:
                    EmptyView()
                }
            }
            .presentationDetents([.fraction(0.92)])
            .presentationCornerRadius(24)
            .presentationBackground(GroupSetupTheme.card)
            .presentationDragIndicator(.visible)
        }
    }

    private func emptyPanel(
        title: String,
        body: String,
        action: String? = nil,
        onAction: (() -> Void)? = nil
    ) -> some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundStyle(MomentraBrandTokens.textOnDark)
                .multilineTextAlignment(.center)
            Text(body)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "#C9C4D8"))
                .multilineTextAlignment(.center)
            if let action, let onAction {
                Button(action, action: onAction)
                    .padding(.top, 8)
            }
        }
        .padding(24)
    }
}
