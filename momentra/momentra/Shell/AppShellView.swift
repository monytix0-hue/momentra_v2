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
    @State private var groupMomentDirectoryOpen = false
    @State private var businessGap: BusinessGapPage?
    @State private var businessLocationName: String?
    @State private var reopenCompanySettings = false
    @State private var pendingActivation: PendingBusinessActivation?
    @State private var groupDirectoryPreferCompleted = false
    @State private var groupViewerReadOnly = false
    @State private var groupCollabKind: GroupCollabKind? = nil
    @State private var groupFinancePresented = false
    @State private var groupSplitsPresented = false
    @State private var weddingGapQa: WeddingQuickAddKind? = nil
    @State private var experienceGapQa: ExperienceQuickAddKind? = nil
    @State private var purchaseGapQa: PurchaseQuickAddKind? = nil
    @State private var livingGapQa: LivingQuickAddKind? = nil
    @State private var businessExpenseSheetPresented = false
    @State private var businessKhataSheetPresented = false
    @State private var businessRevenueSheetPresented = false
    @State private var businessInvoiceSheetPresented = false
    @State private var businessMembersSheetPresented = false
    @State private var businessQuickAddPresented = false
    @State private var businessGapQa: BusinessQuickAddKind? = nil
    @State private var lifeOpsQa: LifeOpsQuickAddKind? = nil
    @State private var futureQa: FutureQuickAddKind? = nil
    @State private var lifestyleQa: LifestyleQuickAddKind? = nil
    @State private var relationshipsQa: RelationshipsQuickAddKind? = nil
    @State private var personalQaMomentId: String? = nil
    @State private var personalSetupSystem: PersonalSetupSystem? = nil
    @State private var personalSetupChooserOpen = false
    @State private var relationshipsActivityOpen = false
    @State private var recentActivityOpen = false
    @State private var groupRecentActivityOpen = false
    @State private var teamRecentActivityOpen = false
    @State private var businessRecentActivityOpen = false
    @State private var businessRecentActivityAccent = Color(hex: "#818CF8")
    @State private var businessRecentActivitySubtitle = "Updates, spend, memories, and other events."
    @State private var newMomentOpen = false
    @State private var groupCreatePhase: GroupCreatePhase = .chooser
    @State private var showManageMoment = false
    @State private var storyMomentId: String? = nil
    @State private var editSetupTarget: EditMomentSetupTarget? = nil
    @State private var showJoinQrScanner = false
    @State private var showReferComingSoon = false
    @State private var pendingGroupJoin: PendingGroupJoin?
    @State private var pendingCompanyJoin: PendingCompanyJoin?
    @State private var companyMenuOpen = false
    @State private var joinFeedbackMessage: String?
    @State private var habitRewardMessage: String?
    @State private var inboxOpen = false
    @StateObject private var inboxBadge = NotificationInboxBadge.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        shellPage
            .overlay(alignment: .bottom) {
                if let habitRewardMessage {
                    Text(habitRewardMessage)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#2D1F5E"))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.bottom, 88)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .onAppear {
                model.applyPreferredPersonalLock()
                model.bindIdentity(identity)
                if let pending = JoinInviteStore.shared.consume() {
                    pendingGroupJoin = PendingGroupJoin(id: pending)
                }
                if let companyCode = JoinCompanyInviteStore.shared.consume() {
                    pendingCompanyJoin = PendingCompanyJoin(id: companyCode)
                }
                handlePendingPushDeepLink()
                Task { await inboxBadge.refresh() }
            }
            .onChange(of: model.moments.map(\.momentId)) { _, _ in
                model.applyPreferredPersonalLock()
            }
            .onChange(of: model.selectedContext) { _, _ in
                model.applyPreferredPersonalLock()
            }
            .onReceive(JoinInviteStore.shared.$pendingCode) { code in
                guard let code, !code.isEmpty else { return }
                guard pendingGroupJoin == nil else { return }
                if let pending = JoinInviteStore.shared.consume() {
                    pendingGroupJoin = PendingGroupJoin(id: pending)
                }
            }
            .onReceive(JoinCompanyInviteStore.shared.$pendingCode) { code in
                guard let code, !code.isEmpty else { return }
                guard pendingCompanyJoin == nil else { return }
                if let pending = JoinCompanyInviteStore.shared.consume() {
                    pendingCompanyJoin = PendingCompanyJoin(id: pending)
                }
            }
            .onReceive(PushDeepLinkStore.shared.$pendingLink) { link in
                guard let link, !link.isEmpty else { return }
                handlePendingPushDeepLink()
            }
            .onChange(of: model.moments.map(\.momentId)) { _, _ in
                handlePendingPushDeepLink()
            }
            .onChange(of: identity.userId) { _, _ in
                model.bindIdentity(identity)
            }
            .task(id: "\(model.selectedMomentId ?? "")-\(identity.userId)") {
                await refreshGroupViewerReadOnly()
            }
            .onChange(of: model.selectedContext) { _, _ in
                newMomentOpen = false
                groupMomentDirectoryOpen = false
                groupCreatePhase = .chooser
            }
            .onChange(of: model.bottomDestination) { _, destination in
                newMomentOpen = false
                groupMomentDirectoryOpen = false
                if destination == .create, model.selectedContext == .group {
                    // Keep phase when advancing from Pulse type cards; reset only when tapping Create tab from chooser path is handled by openNewMoment / tab setter.
                }
            }
            .onChange(of: scenePhase) { _, phase in
                guard phase == .active else { return }
                model.flushOfflineQueue()
                Task { await inboxBadge.refresh() }
                if model.selectedContext == .group {
                    model.refreshVisibleGroupTab()
                }
            }
    }

    private var shellPage: some View {
        NativeShellTabView(
            selection: bottomTabSelection,
            accent: momentAccent,
            context: model.selectedContext,
            content: { tabNavigationRoot }
        )
        .background(Color(hex: "#14121B").ignoresSafeArea())
        .fullScreenCover(isPresented: $showManageMoment) {
            if let momentId = model.selectedMomentId {
                ManageMomentFlowSheet(
                    momentId: momentId,
                    momentTitle: model.selectedMomentTitle ?? "Moment",
                    domain: model.selectedContext,
                    currentUserId: identity.userId,
                    companyId: model.selectedCompany?.companyId
                        ?? model.moments.first(where: { $0.momentId == momentId })?.companyId,
                    isPresented: $showManageMoment,
                    onEditSetup: {
                        editSetupTarget = EditMomentSetupTarget.resolve(
                            context: model.selectedContext,
                            momentTypeCode: model.selectedMomentTypeCode
                        )
                    },
                    onLifecycleChanged: {
                        model.reloadCurrentContext()
                    },
                    onCompleted: {
                        let completedId = model.selectedMomentId
                        showManageMoment = false
                        storyMomentId = completedId
                        model.reloadCurrentContext()
                    },
                    onLeft: {
                        showManageMoment = false
                        model.clearSelectedMomentAfterLeave()
                    },
                    onDuplicated: { newId, title in
                        showManageMoment = false
                        model.onMomentCreated(momentId: newId, title: title)
                    }
                )
                .preferredColorScheme(.dark)
            }
        }
        .fullScreenCover(item: $personalSetupSystem) { system in
            PersonalSetupWizardView(
                system: system,
                onBack: { personalSetupSystem = nil },
                onCreated: { momentId, title, momentTypeCode, status in
                    personalSetupSystem = nil
                    model.onMomentCreated(
                        momentId: momentId,
                        title: title,
                        momentTypeCode: momentTypeCode,
                        status: status
                    )
                }
            )
            .preferredColorScheme(.dark)
        }
        .sheet(isPresented: $personalSetupChooserOpen) {
            NavigationStack {
                List {
                    ForEach(PersonalUnified.missingSetupSystems(model.moments)) { system in
                        Button(system.setupTitle) {
                            personalSetupChooserOpen = false
                            personalSetupSystem = system
                        }
                    }
                }
                .navigationTitle("Set up another life area")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { personalSetupChooserOpen = false }
                    }
                }
            }
            .preferredColorScheme(.dark)
        }
        .fullScreenCover(isPresented: Binding(
            get: { storyMomentId != nil },
            set: { if !$0 { storyMomentId = nil } }
        )) {
            if let momentId = storyMomentId {
                MomentStoryViewerView(momentId: momentId) {
                    storyMomentId = nil
                }
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
                        if let mid = model.selectedMomentId {
                            GroupTabDataCache.invalidateMoment(mid)
                        }
                        model.refreshVisibleGroupTab(forcePrefetch: true)
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
            if let momentId = personalQaMomentId ?? model.selectedMomentId {
                switch kind {
                case .masterExpense:
                    PersonalMasterExpenseSheet(
                        momentId: momentId,
                        pulseFamily: PersonalPulseFamily.forTypeCode(model.selectedMomentTypeCode),
                        onClose: {
                            moneyQa = nil
                            personalQaMomentId = nil
                        },
                        onSaved: {
                            moneyQa = nil
                            personalQaMomentId = nil
                            model.refreshVisiblePersonalTab()
                            showHabitReward("Saved · counts toward your day")
                            PersonalEveningNudgeScheduler.onTodaySave()
                        }
                    )
                case .income, .transfer, .savings:
                    PersonalMoneyQuickAddSheet(
                        kind: kind,
                        momentId: momentId,
                        onClose: {
                            moneyQa = nil
                            personalQaMomentId = nil
                        },
                        onSaved: {
                            moneyQa = nil
                            personalQaMomentId = nil
                            model.refreshVisiblePersonalTab()
                            if kind == .income {
                                showHabitReward("Saved · counts toward your day")
                            }
                        }
                    )
                }
            }
        }
        .sheet(isPresented: $groupExpenseSheetPresented) {
            if groupViewerReadOnly {
                Text("Viewers can view but not add expenses.")
                    .padding(24)
                    .onAppear { groupExpenseSheetPresented = false }
            } else if let momentId = model.selectedMomentId {
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
            if groupViewerReadOnly {
                Text("Viewers can view but not add contributions.")
                    .padding(24)
                    .onAppear { groupContributionSheetPresented = false }
            } else if let momentId = model.selectedMomentId {
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
                    currentUserId: identity.userId,
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
                    momentTypeCode: model.selectedMomentTypeCode,
                    isPresented: Binding(
                        get: { groupCollabKind != nil },
                        set: { if !$0 { groupCollabKind = nil } }
                    ),
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
        .sheet(isPresented: $companyMenuOpen) {
            CompanyFlowSheet(
                companies: model.companies,
                selectedCompanyId: model.selectedCompany?.companyId,
                startOnSettings: reopenCompanySettings,
                onClose: {
                    reopenCompanySettings = false
                    companyMenuOpen = false
                },
                onSelect: { model.selectCompany($0) },
                onCreated: { model.onCompanyCreated($0) },
                onOpenLocations: {
                    reopenCompanySettings = true
                    companyMenuOpen = false
                    businessGap = .locationPicker
                },
                onCompaniesChanged: {
                    model.clearSelectedMomentAfterLeave()
                }
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(item: $pendingCompanyJoin) { pending in
            CompanyJoinConfirmSheet(
                code: pending.code,
                onClose: { pendingCompanyJoin = nil },
                onGoToCompany: { company in
                    pendingCompanyJoin = nil
                    model.onCompanyCreated(company)
                    if model.selectedContext != .business {
                        model.selectContext(.business)
                    }
                }
            )
        }
        .sheet(item: $pendingGroupJoin) { pending in
            GroupJoinConfirmSheet(
                code: pending.code,
                onClose: { pendingGroupJoin = nil },
                onJoin: {
                    let code = pending.code
                    let result = try await model.redeemJoinCode(code, using: createModel)
                    pendingGroupJoin = nil
                    newMomentOpen = false
                    groupCreatePhase = .chooser
                    if result.alreadyMember == true {
                        joinFeedbackMessage = "Already a member"
                    } else if result.momentId == nil || result.momentId?.isEmpty == true {
                        joinFeedbackMessage =
                            "Invite claimed — you’ll join when the organizer finishes creating the group."
                    } else {
                        joinFeedbackMessage = "Joined group"
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
                    pendingCompanyJoin = PendingCompanyJoin(id: code)
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
        .sheet(isPresented: $businessKhataSheetPresented) {
            if let momentId = model.selectedMomentId {
                let companyId = model.selectedCompany?.companyId
                    ?? model.moments.first(where: { $0.momentId == momentId })?.companyId
                    ?? ""
                BusinessKhataHomeView(
                    momentId: momentId,
                    companyId: companyId,
                    shopName: model.selectedCompany?.displayName ?? model.selectedMomentTitle ?? "",
                    isPresented: $businessKhataSheetPresented
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
                    shopName: model.selectedCompany?.displayName
                        ?? model.selectedMomentTitle
                        ?? "",
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
                momentId: model.selectedMomentId,
                momentTypeCode: model.selectedMomentTypeCode,
                companyId: model.selectedCompany?.companyId,
                onClose: { businessQuickAddPresented = false },
                onTile: { kind in
                    businessQuickAddPresented = false
                    switch kind {
                    case .khata:
                        businessKhataSheetPresented = true
                    case .expense, .spendEntry:
                        businessExpenseSheetPresented = true
                    case .revenue:
                        businessRevenueSheetPresented = true
                    case .invoice:
                        businessInvoiceSheetPresented = true
                    default:
                        businessGapQa = kind
                    }
                },
                            onNewMoment: {
                    businessQuickAddPresented = false
                    model.selectBottomDestination(.create)
                },
                onOpenCompanySettings: {
                    businessQuickAddPresented = false
                    reopenCompanySettings = true
                    companyMenuOpen = true
                },
                onExpense: {
                    businessQuickAddPresented = false
                    businessExpenseSheetPresented = true
                },
                onRevenue: {
                    businessQuickAddPresented = false
                    businessRevenueSheetPresented = true
                },
                onInvoice: {
                    businessQuickAddPresented = false
                    businessInvoiceSheetPresented = true
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
                if kind == .expense || kind == .spendEntry {
                    Color.clear
                        .onAppear {
                            businessGapQa = nil
                            businessExpenseSheetPresented = true
                        }
                } else if code.contains("RUNWAY") {
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
                            businessRevenueSheetPresented = true
                        },
                        onInvoice: {
                            let code = (model.selectedMomentTypeCode ?? "").uppercased()
                            businessInvoiceSheetPresented = true
                        }
                    )
                }
            }
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .sheet(item: $lifeOpsQa) { kind in
            if let momentId = personalQaMomentId ?? model.selectedMomentId {
                PersonalLifeOpsQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: {
                        lifeOpsQa = nil
                        personalQaMomentId = nil
                    },
                    onSaved: {
                        lifeOpsQa = nil
                        personalQaMomentId = nil
                        model.refreshVisiblePersonalTab()
                        if kind == .mood || kind == .recovery {
                            showHabitReward("Logged · your day is updating")
                            PersonalEveningNudgeScheduler.onTodaySave()
                        }
                    }
                )
            }
        }
        .sheet(item: $futureQa) { kind in
            if let momentId = personalQaMomentId ?? model.selectedMomentId {
                PersonalFutureQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: {
                        futureQa = nil
                        personalQaMomentId = nil
                    },
                    onSaved: {
                        futureQa = nil
                        personalQaMomentId = nil
                        model.refreshVisiblePersonalTab()
                    }
                )
            }
        }
        .sheet(item: $lifestyleQa) { kind in
            if let momentId = personalQaMomentId ?? model.selectedMomentId {
                PersonalLifestyleQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: {
                        lifestyleQa = nil
                        personalQaMomentId = nil
                    },
                    onSaved: {
                        lifestyleQa = nil
                        personalQaMomentId = nil
                        model.refreshVisiblePersonalTab()
                    }
                )
            }
        }
        .sheet(item: $relationshipsQa) { kind in
            if let momentId = personalQaMomentId ?? model.selectedMomentId {
                PersonalRelationshipsQuickAddSheet(
                    kind: kind,
                    momentId: momentId,
                    onClose: {
                        relationshipsQa = nil
                        personalQaMomentId = nil
                    },
                    onSaved: {
                        relationshipsQa = nil
                        personalQaMomentId = nil
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
        .sheet(isPresented: $teamRecentActivityOpen) {
            if let momentId = model.selectedMomentId {
                BusinessRecentActivityFlow(
                    momentId: momentId,
                    isPresented: $teamRecentActivityOpen,
                    accent: Color(hex: "#818CF8"),
                    subtitle: "Team updates, polls, memories, and other events."
                )
            }
        }
        .sheet(isPresented: $businessRecentActivityOpen) {
            if let momentId = model.selectedMomentId {
                BusinessRecentActivityFlow(
                    momentId: momentId,
                    isPresented: $businessRecentActivityOpen,
                    accent: businessRecentActivityAccent,
                    subtitle: businessRecentActivitySubtitle
                )
            }
        }
        .sheet(isPresented: $groupRecentActivityOpen) {
            if let momentId = model.selectedMomentId {
                GroupRecentActivityFlow(
                    momentId: momentId,
                    momentTypeCode: model.selectedMomentTypeCode
                        ?? model.moments.first(where: { $0.momentId == momentId })?.momentTypeCode,
                    isPresented: $groupRecentActivityOpen,
                    onChanged: { model.refreshVisibleGroupTab(forcePrefetch: true) }
                )
            }
        }
        .sheet(isPresented: Binding(
            get: { model.life360Open },
            set: { model.openLife360($0) }
        )) {
            Life360ComingSoonView(onClose: { model.openLife360(false) })
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $inboxOpen) {
            NotificationInboxView(
                onOpenMoment: { momentId in
                    if model.openMomentFromDeepLink(momentId: momentId) { return }
                    PushDeepLinkStore.shared.offer("momentra://moment/\(momentId)")
                },
                onClose: { inboxOpen = false }
            )
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
            model.selectBottomDestination(.create)
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

    /// Peek pending push link; only consume after successful open (retry when inventory arrives).
    private func showHabitReward(_ message: String) {
        habitRewardMessage = message
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            if habitRewardMessage == message {
                habitRewardMessage = nil
            }
        }
    }

    private func handlePendingPushDeepLink() {
        guard let pending = PushDeepLinkStore.shared.peek() else { return }
        if PushDeepLinkStore.isInboxLink(pending.link) {
            _ = PushDeepLinkStore.shared.consume()
            inboxOpen = true
            Task {
                if let id = pending.userNotificationId {
                    _ = try? await APIClient.shared.markMyNotificationsRead(notificationIds: [id])
                    await inboxBadge.refresh()
                }
            }
            return
        }
        guard let momentId = PushDeepLinkStore.parseMomentId(pending.link) else { return }
        let openStory = PushDeepLinkStore.isStoryLink(pending.link)
        if openStory {
            // Completed moments leave active inventory — open Story by id without selection gate.
            _ = model.openMomentFromDeepLink(momentId: momentId)
            storyMomentId = momentId
            _ = PushDeepLinkStore.shared.consume()
            Task {
                if let id = pending.userNotificationId {
                    _ = try? await APIClient.shared.markMyNotificationsRead(notificationIds: [id])
                    await inboxBadge.refresh()
                }
            }
            return
        }
        guard model.openMomentFromDeepLink(momentId: momentId) else { return }
        _ = PushDeepLinkStore.shared.consume()
        Task {
            if let id = pending.userNotificationId {
                _ = try? await APIClient.shared.markMyNotificationsRead(notificationIds: [id])
                await inboxBadge.refresh()
            }
        }
    }

    private func redeemJoinCode(_ code: String) {
        pendingGroupJoin = PendingGroupJoin(id: code)
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
                groupMomentDirectoryOpen = false
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
        [.pulse, .moments, .life, .memory, .create].contains(model.bottomDestination)
    }

    private var shellNavigationTitle: String {
        if newMomentOpen { return "New Moment" }
        if model.bottomDestination == .create { return "" }
        return "\(model.selectedContext.label) · \(model.bottomDestination.label)"
    }

    private var momentSwitcherIsEmpty: Bool {
        model.moments.filter { $0.isActiveStatus || $0.isCompletedStatus }.isEmpty
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
        let base = model.moments
            .filter { $0.isActiveStatus || $0.momentId == model.selectedMomentId }
        if model.selectedContext == .personal {
            // Unified Personal — no family chip list (chrome is PersonalUnifiedChromeView).
            return []
        }
        return base.map { ($0.momentId, $0.title) }
    }

    private func openMoneyQa(_ kind: MoneyQuickAddKind) {
        guard let m = PersonalUnified.resolveFamilyTarget(
            moments: model.moments,
            family: .lifeOperations,
            currentSelectedId: model.selectedMomentId
        ) else {
            personalSetupSystem = .lifeOperations
            return
        }
        personalQaMomentId = m.momentId
        moneyQa = kind
    }

    private func openLifeOpsQa(_ kind: LifeOpsQuickAddKind) {
        guard let m = PersonalUnified.resolveFamilyTarget(
            moments: model.moments,
            family: .lifeOperations,
            currentSelectedId: model.selectedMomentId
        ) else {
            personalSetupSystem = .lifeOperations
            return
        }
        personalQaMomentId = m.momentId
        lifeOpsQa = kind
    }

    private func openFutureQa(_ kind: FutureQuickAddKind) {
        guard let m = PersonalUnified.resolveFamilyTarget(
            moments: model.moments,
            family: .futureBuilding,
            currentSelectedId: model.selectedMomentId
        ) else { return }
        personalQaMomentId = m.momentId
        futureQa = kind
    }

    private func openLifestyleQa(_ kind: LifestyleQuickAddKind) {
        guard let m = PersonalUnified.resolveFamilyTarget(
            moments: model.moments,
            family: .lifestyle,
            currentSelectedId: model.selectedMomentId
        ) else { return }
        personalQaMomentId = m.momentId
        lifestyleQa = kind
    }

    private func openRelationshipsQa(_ kind: RelationshipsQuickAddKind) {
        guard let m = PersonalUnified.resolveFamilyTarget(
            moments: model.moments,
            family: .relationships,
            currentSelectedId: model.selectedMomentId
        ) else { return }
        personalQaMomentId = m.momentId
        relationshipsQa = kind
    }

    private var personalSwitcherTitle: String? {
        if model.selectedContext == .personal {
            return PersonalPulseFamily.forTypeCode(model.selectedMomentTypeCode).switcherLabel
        }
        return model.selectedMomentTitle
    }

    private var selectedMomentIsCompleted: Bool {
        model.moments.first(where: { $0.momentId == model.selectedMomentId })?.isCompletedStatus == true
    }

    @ViewBuilder
    private var tabNavigationRoot: some View {
        NavigationStack {
            ZStack {
                destinationBodyWithFab
                if groupMomentDirectoryOpen && model.selectedContext == .group {
                    GroupActiveMomentsDirectoryView(
                        moments: model.moments,
                        selectedMomentId: model.selectedMomentId,
                        onDismiss: {
                            withAnimation(.easeInOut(duration: 0.25)) {
                                groupMomentDirectoryOpen = false
                                groupDirectoryPreferCompleted = false
                            }
                        },
                        onSelectMoment: { momentId in
                            model.selectMoment(id: momentId)
                            withAnimation(.easeInOut(duration: 0.25)) {
                                groupMomentDirectoryOpen = false
                                groupDirectoryPreferCompleted = false
                            }
                        },
                        onSelectCompletedMoment: { moment in
                            model.selectCompletedGroupMoment(moment)
                            withAnimation(.easeInOut(duration: 0.25)) {
                                groupMomentDirectoryOpen = false
                                groupDirectoryPreferCompleted = false
                            }
                        },
                        onOpenStory: { momentId in
                            storyMomentId = momentId
                            withAnimation(.easeInOut(duration: 0.25)) {
                                groupMomentDirectoryOpen = false
                                groupDirectoryPreferCompleted = false
                            }
                        },
                        onCreateMoment: {
                            withAnimation(.easeInOut(duration: 0.25)) {
                                groupMomentDirectoryOpen = false
                                groupDirectoryPreferCompleted = false
                            }
                            openNewMoment()
                        },
                        initialCompletedTab: groupDirectoryPreferCompleted
                    )
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .move(edge: .bottom)),
                        removal: .opacity.combined(with: .move(edge: .bottom))
                    ))
                    .zIndex(1)
                }
                if let businessGap, model.selectedContext == .business {
                    BusinessGapHost(
                        page: businessGap,
                        companyId: model.selectedCompany?.companyId,
                        companyName: model.selectedCompany?.displayName ?? "",
                        moments: model.moments,
                        locationName: $businessLocationName,
                        onSelectMoment: { moment in
                            model.selectMoment(id: moment.momentId)
                            model.selectBottomDestination(.pulse)
                            self.businessGap = nil
                        },
                        onCreateMoment: {
                            self.businessGap = nil
                            openNewMoment()
                        },
                        onClose: { self.businessGap = nil },
                        onPage: { self.businessGap = $0 },
                        onBackToSettings: {
                            self.businessGap = nil
                            reopenCompanySettings = true
                            companyMenuOpen = true
                        }
                    )
                    .zIndex(2)
                }
                if let pendingActivation {
                    BusinessActivationSuccess(title: pendingActivation.title) {
                        newMomentOpen = false
                        model.onMomentCreated(
                            momentId: pendingActivation.momentId,
                            title: pendingActivation.title,
                            momentTypeCode: pendingActivation.momentTypeCode,
                            status: pendingActivation.status
                        )
                        self.pendingActivation = nil
                    }
                    .zIndex(3)
                }
            }
            .animation(.easeInOut(duration: 0.28), value: groupMomentDirectoryOpen)
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .safeAreaInset(edge: .top, spacing: 0) {
                if !(groupMomentDirectoryOpen && model.selectedContext == .group) {
                    shellTopChrome
                }
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
            if let notice = model.offlineNotice {
                Text(notice)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color(hex: "#3A2A12"))
            }

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
                onInbox: { inboxOpen = true },
                onAvatar: { model.openProfile(true) },
                unreadNotificationCount: inboxBadge.unreadCount,
                locationName: businessLocationName
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
            if shouldShowMomentSwitcher && !newMomentOpen && !groupMomentDirectoryOpen {
                if model.selectedContext == .personal {
                    PersonalUnifiedChromeView(
                        accent: momentAccent,
                        showManage: model.selectedMomentId != nil,
                        showSetUpEveryday: PersonalUnified.activeMoments(model.moments).allSatisfy {
                            PersonalPulseFamily.forTypeCode($0.momentTypeCode) != .lifeOperations
                        } && !PersonalUnified.activeMoments(model.moments).isEmpty,
                        onManage: { showManageMoment = true },
                        onSetUpEveryday: { personalSetupSystem = .lifeOperations }
                    )
                } else {
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
                        onInvite: model.selectedContext == .group ? { groupInviteSheetPresented = true } : nil,
                        useDirectorySelector: model.selectedContext == .group || model.selectedContext == .business,
                        onOpenDirectory: {
                            if model.selectedContext == .business {
                                businessGap = .moments
                            } else {
                                groupDirectoryPreferCompleted = false
                                withAnimation(.easeInOut(duration: 0.28)) {
                                    groupMomentDirectoryOpen = true
                                }
                            }
                        },
                        selectedIsCompleted: selectedMomentIsCompleted,
                        startExpanded: false
                    )
                }
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
                        PersonalExpenseFab {
                            openMoneyQa(.masterExpense)
                        }
                    }
                    .padding(.trailing, 16)
                    .padding(.bottom, 8)
                }
            }
    }

    @ViewBuilder
    private var destinationBody: some View {
        if newMomentOpen, model.selectedContext == .group {
            groupCreateBody
        } else if newMomentOpen, model.selectedContext == .business {
            if let companyId = model.selectedCompany?.companyId {
                BusinessCreateFlowView(
                    createModel: createModel,
                    companyId: companyId,
                    onBack: { newMomentOpen = false },
                    onCreated: { outcome in
                        pendingActivation = PendingBusinessActivation(
                            momentId: outcome.momentId,
                            title: outcome.title,
                            momentTypeCode: outcome.momentTypeCode,
                            status: outcome.status
                        )
                    }
                )
            } else {
                    CompanyFlowSheet(
                        companies: model.companies,
                        selectedCompanyId: nil,
                        startOnCreate: true,
                        onClose: { newMomentOpen = false },
                        onSelect: { model.selectCompany($0) },
                        onCreated: { model.onCompanyCreated($0) }
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
                            momentTypeCode: outcome.momentTypeCode,
                            status: outcome.status
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
                    onJoinCode: redeemJoinCode,
                    onOpenCompletedMoments: {
                        groupDirectoryPreferCompleted = true
                        withAnimation(.easeInOut(duration: 0.28)) {
                            groupMomentDirectoryOpen = true
                        }
                    }
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
                            momentTypeCode: groupTypeCode,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in weddingGapQa = kind },
                            onViewAllActivity: { groupRecentActivityOpen = true }
                        )
                    } else if isExperience {
                        ExperiencePulseActiveView(
                            theme: experienceTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            momentTypeCode: groupTypeCode,
                            viewerReadOnly: groupViewerReadOnly,
                            onAddExpense: {
                                guard !groupViewerReadOnly else { return }
                                groupExpenseSheetPresented = true
                            },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in
                                guard !groupViewerReadOnly else { return }
                                if kind == .participant {
                                    groupInviteSheetPresented = true
                                } else {
                                    experienceGapQa = kind
                                }
                            },
                            onViewAllActivity: { groupRecentActivityOpen = true }
                        )
                    } else if isPurchase {
                        PurchasePulseActiveView(
                            theme: purchaseTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            momentTypeCode: groupTypeCode,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in
                                if kind == .contributor {
                                    groupInviteSheetPresented = true
                                } else {
                                    purchaseGapQa = kind
                                }
                            },
                            onViewAllActivity: { groupRecentActivityOpen = true }
                        )
                    } else if isLiving {
                        LivingPulseActiveView(
                            theme: livingTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            momentTypeCode: groupTypeCode,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onOpenQuickAdd: { model.selectBottomDestination(.create) },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onQuickAddKind: { kind in
                                if kind == .resident {
                                    groupInviteSheetPresented = true
                                } else {
                                    livingGapQa = kind
                                }
                            },
                            onViewAllActivity: { groupRecentActivityOpen = true }
                        )
                    } else {
                        GroupPulseActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            momentTypeCode: groupTypeCode,
                            onAddExpense: { groupExpenseSheetPresented = true },
                            onViewSplits: { groupSplitsPresented = true },
                            onOpenFinance: { groupFinancePresented = true },
                            onOpenMemory: { groupCollabKind = .memory },
                            onOpenChat: { groupCollabKind = .update },
                            onOpenItinerary: { groupCollabKind = .planning },
                            onViewAllActivity: { groupRecentActivityOpen = true }
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
                            momentTypeCode: groupTypeCode,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else if isPurchase {
                        PurchaseMomentsActiveView(
                            theme: purchaseTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            momentTypeCode: groupTypeCode,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else if isLiving {
                        LivingMomentsActiveView(
                            theme: livingTheme,
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            momentTypeCode: groupTypeCode,
                            onOpenQuickAdd: { model.selectBottomDestination(.create) }
                        )
                    } else {
                        GroupMomentsActiveView(
                            refreshToken: model.groupTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            momentTypeCode: groupTypeCode,
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
                            case .experience:
                                if isWedding { weddingGapQa = .planning }
                                else if isExperience { experienceGapQa = .planning }
                                else if isPurchase { purchaseGapQa = .purchaseItem }
                                else if isLiving { livingGapQa = .task }
                                else { groupCollabKind = .planning }
                            case .goal, .community:
                                // Goal / Community moments are Coming Soon — do not open unrelated sheets.
                                joinFeedbackMessage = action == .goal
                                    ? "Goal moments are coming soon."
                                    : "Community moments are coming soon."
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
                                else if isLiving { groupInviteSheetPresented = true }
                                else { groupCollabKind = .booking }
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
                            viewerReadOnly: groupViewerReadOnly,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                guard !groupViewerReadOnly else { return }
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
                            viewerReadOnly: groupViewerReadOnly,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                if groupViewerReadOnly { return }
                                if kind == .participant {
                                    groupInviteSheetPresented = true
                                } else {
                                    experienceGapQa = kind
                                }
                            },
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
                            viewerReadOnly: groupViewerReadOnly,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                guard !groupViewerReadOnly else { return }
                                if kind == .contributor {
                                    groupInviteSheetPresented = true
                                } else {
                                    purchaseGapQa = kind
                                }
                            },
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
                            viewerReadOnly: groupViewerReadOnly,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                guard !groupViewerReadOnly else { return }
                                if kind == .resident {
                                    groupInviteSheetPresented = true
                                } else {
                                    livingGapQa = kind
                                }
                            },
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
                            viewerReadOnly: groupViewerReadOnly,
                            onClose: { model.exitCreateDestination() },
                            onExpense: {
                                guard !groupViewerReadOnly else { return }
                                groupExpenseSheetPresented = true
                            },
                            onContribution: {
                                guard !groupViewerReadOnly else { return }
                                groupContributionSheetPresented = true
                            },
                            onSettle: {
                                guard !groupViewerReadOnly else { return }
                                groupSettlementSheetPresented = true
                            },
                            onParticipants: { groupParticipantsSheetPresented = true },
                            onInvite: {
                                guard !groupViewerReadOnly else { return }
                                groupInviteSheetPresented = true
                            },
                            onBudget: {
                                guard !groupViewerReadOnly else { return }
                                groupBudgetSheetPresented = true
                            },
                            onPlanning: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .planning
                            },
                            onChecklist: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .checklist
                            },
                            onBooking: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .booking
                            },
                            onPoll: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .poll
                            },
                            onUpdate: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .update
                            },
                            onMemory: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .memory
                            },
                            onPurchaseItem: {
                                guard !groupViewerReadOnly else { return }
                                groupCollabKind = .purchaseItem
                            },
                            onResident: {
                                guard !groupViewerReadOnly else { return }
                                groupInviteSheetPresented = true
                            },
                            onNewMoment: {
                                groupCreatePhase = .chooser
                                newMomentOpen = true
                            },
                            onJoinCode: redeemJoinCode
                        )
                    }
                } else if model.selectedContext == .personal, model.bottomDestination == .pulse {
                    PersonalPulseActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentTitle: model.selectedMomentTitle,
                        momentId: model.selectedMomentId,
                        momentTypeCode: personalTypeCode,
                        onAddExpense: { openMoneyQa(.masterExpense) },
                        onLifeOpsQuickAdd: { openLifeOpsQa($0) },
                        onFutureQuickAdd: { openFutureQa($0) },
                        onLifestyleQuickAdd: { openLifestyleQa($0) },
                        onRelationshipsQuickAdd: { openRelationshipsQa($0) },
                        onViewAllActivity: {
                            if isRelationships {
                                relationshipsActivityOpen = true
                            } else {
                                recentActivityOpen = true
                            }
                        },
                        forceCollapsed: true
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
                        onProtectMilestone: { openFutureQa(.milestone) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isLifestyle {
                    PersonalLifestyleMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectRitual: { openLifestyleQa(.experience) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isRelationships {
                    PersonalRelationshipsMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectConnection: { openRelationshipsQa(.connection) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .memory, isLifeOps {
                    PersonalLifeOpsMemoryActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        momentId: model.selectedMomentId,
                        onProtectRecovery: { openLifeOpsQa(.recovery) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .life {
                    PersonalLifeActiveView(
                        refreshToken: model.personalTabRefreshToken,
                        onLogRecovery: { openLifeOpsQa(.recovery) },
                        onLogSpend: { openMoneyQa(.masterExpense) },
                        onOpenAdd: { model.selectBottomDestination(.create) }
                    )
                } else if model.selectedContext == .personal, model.bottomDestination == .create {
                    PersonalQuickAddHubView(
                        hasActiveMoment: model.selectedMomentId != nil,
                        momentTypeCode: personalTypeCode,
                        capabilityCodes: model.capabilities,
                        simpleMode: false,
                        unifiedCatalog: true,
                        presentFamilies: PersonalUnified.presentFamilies(model.moments),
                        onSetupMissing: { personalSetupChooserOpen = true },
                        onClose: { model.exitCreateDestination() },
                        onSpend: { openMoneyQa(.masterExpense) },
                        onIncome: { openMoneyQa(.income) },
                        onRecovery: { openLifeOpsQa(.recovery) },
                        onMood: { openLifeOpsQa(.mood) },
                        onAttention: { openLifeOpsQa(.attention) },
                        onAdjust: { openLifeOpsQa(.adjust) },
                        onTransfer: { openMoneyQa(.transfer) },
                        onSavings: { openMoneyQa(.savings) },
                        onFutureQuickAdd: { openFutureQa($0) },
                        onLifestyleQuickAdd: { openLifestyleQa($0) },
                        onRelationshipsQuickAdd: { openRelationshipsQa($0) }
                    )
                } else if model.selectedContext == .business, model.bottomDestination == .pulse {
                    let code = (model.selectedMomentTypeCode ?? "").uppercased()
                    if code.contains("RUNWAY") {
                        RunwayPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogExpense: { businessExpenseSheetPresented = true },
                            onOpenQuickAdd: { businessQuickAddPresented = true },
                            onOpenMoments: { model.selectBottomDestination(.moments) },
                            onViewAllActivity: {
                                businessRecentActivityAccent = Color(hex: "#F59E0B")
                                businessRecentActivitySubtitle = "Spend, revenue, and financial events."
                                businessRecentActivityOpen = true
                            }
                        )
                    } else if code.contains("OPERATIONS") && !code.contains("TEAM") {
                        OpsPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogSpend: { businessExpenseSheetPresented = true },
                            onOpenQuickAdd: { businessQuickAddPresented = true },
                            onOpenMoments: { model.selectBottomDestination(.moments) },
                            onViewAllActivity: {
                                businessRecentActivityAccent = Color(hex: "#818CF8")
                                businessRecentActivitySubtitle = "Deliveries, vendors, issues, and ops events."
                                businessRecentActivityOpen = true
                            }
                        )
                    } else if code.contains("TEAM_OPERATIONS") {
                        TeamOpsPulseActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogDelivery: { businessGapQa = .teamUpdate },
                            onOpenQuickAdd: { businessQuickAddPresented = true },
                            onViewAllActivity: { teamRecentActivityOpen = true },
                            onAddExpense: { businessExpenseSheetPresented = true }
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
                            onLogExpense: { businessExpenseSheetPresented = true },
                            onOpenQuickAdd: { businessQuickAddPresented = true },
                            onOpenMoments: { model.selectBottomDestination(.moments) },
                            onViewAllActivity: {
                                businessRecentActivityAccent = Color(hex: "#F59E0B")
                                businessRecentActivitySubtitle = "Spend, revenue, and financial events."
                                businessRecentActivityOpen = true
                            }
                        )
                    } else if code.contains("OPERATIONS") && !code.contains("TEAM") {
                        OpsMomentsActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentId: model.selectedMomentId,
                            momentTitle: model.selectedMomentTitle,
                            onLogSpend: { businessExpenseSheetPresented = true },
                            onOpenQuickAdd: { businessQuickAddPresented = true },
                            onOpenMoments: { model.selectBottomDestination(.moments) },
                            onViewAllActivity: {
                                businessRecentActivityAccent = Color(hex: "#818CF8")
                                businessRecentActivitySubtitle = "Deliveries, vendors, issues, and ops events."
                                businessRecentActivityOpen = true
                            }
                        )
                    } else if code.contains("TEAM_OPERATIONS") {
                        TeamOpsMomentsActiveView(
                            refreshToken: model.businessTabRefreshToken,
                            momentTitle: model.selectedMomentTitle,
                            momentId: model.selectedMomentId,
                            onLogWin: { businessGapQa = .teamUpdate },
                            onOpenQuickAdd: { businessQuickAddPresented = true },
                            onViewAllActivity: { teamRecentActivityOpen = true }
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
                        onViewReport: { businessGap = .finance },
                        onOpenFinance: { businessGap = .finance },
                        onOpenVendor: { businessGap = .vendor }
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
                            onOpenQuickAdd: { businessGapQa = .teamUpdate }
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
                            momentId: model.selectedMomentId,
                            momentTypeCode: model.selectedMomentTypeCode,
                            companyId: model.selectedCompany?.companyId,
                            onClose: { model.exitCreateDestination() },
                            onTile: { kind in
                                switch kind {
                                case .khata:
                                    businessKhataSheetPresented = true
                                case .expense, .spendEntry:
                                    businessExpenseSheetPresented = true
                                case .revenue:
                                    businessRevenueSheetPresented = true
                                case .invoice:
                                    businessInvoiceSheetPresented = true
                                default:
                                    businessGapQa = kind
                                }
                            },
                            onNewMoment: { newMomentOpen = true },
                            onOpenCompanySettings: {
                                reopenCompanySettings = true
                                companyMenuOpen = true
                            },
                            onExpense: { businessExpenseSheetPresented = true },
                            onRevenue: {
                                businessRevenueSheetPresented = true
                            },
                            onInvoice: {
                                businessInvoiceSheetPresented = true
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
                            momentTypeCode: outcome.momentTypeCode,
                            status: outcome.status
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
                            momentTypeCode: outcome.momentTypeCode,
                            status: outcome.status
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
                            momentTypeCode: outcome.momentTypeCode,
                            status: outcome.status
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
                            momentTypeCode: outcome.momentTypeCode,
                            status: outcome.status
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

    @MainActor
    private func refreshGroupViewerReadOnly() async {
        guard model.selectedContext == .group,
              let momentId = model.selectedMomentId,
              !momentId.isEmpty
        else {
            groupViewerReadOnly = false
            return
        }
        do {
            let participants = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            groupViewerReadOnly = GroupViewerAccess.isViewer(
                participants: participants,
                currentUserId: identity.userId
            )
        } catch {
            // Keep prior value on transient failures.
        }
    }
}
