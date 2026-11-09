import SwiftUI

struct AppShellView: View {
    let identity: ShellIdentity
    @ObservedObject var model: AppShellModel
    @ObservedObject private var operatingChrome = OperatingChromeState.shared
    var onSignOut: () -> Void
    var onSessionExpired: () -> Void = {}
    var onDisplayNameSaved: (String) -> Void = { _ in }

    @StateObject private var createModel = MomentCreateModel()
    @State private var moneyQa: MoneyQuickAddKind? = nil
    @State private var groupExpenseSheetPresented = false
    @State private var groupContributionSheetPresented = false
    @State private var groupSettlementSheetPresented = false
    @State private var groupBudgetSheetPresented = false
    @State private var groupParticipantsSheetPresented = false
    @State private var groupInviteSheetPresented = false
    @State private var groupMomentDirectoryOpen = false
    @State private var businessMomentDirectoryOpen = false
    @State private var businessGap: BusinessGapPage?
    @State private var pendingActivation: PendingBusinessActivation?
    @State private var groupDirectoryPreferCompleted = false
    @State private var businessDirectoryPreferCompleted = false
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
    @State private var businessGapQa: BusinessQuickAddKind? = nil
    @State private var businessAttentionViewAllOpen = false
    @State private var businessAttentionFilter = "Open"
    @State private var lifeOpsQa: LifeOpsQuickAddKind? = nil
    @State private var personalQaMomentId: String? = nil
    @State private var personalSetupSystem: PersonalSetupSystem? = nil
    @State private var personalSetupChooserOpen = false
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
    @State private var businessSetupOpen = false
    @State private var businessJoinOpen = false
    @State private var joinFeedbackMessage: String?
    @State private var habitRewardMessage: String?
    @State private var inboxOpen = false
    @StateObject private var inboxBadge = NotificationInboxBadge.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        attachEventHandlers(to: shellPageWithOverlay)
    }

    private var shellPageWithOverlay: some View {
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
    }

    @ViewBuilder
    private func attachEventHandlers<V: View>(to content: V) -> some View {
        content
            .onAppear(perform: handleOnAppear)
            .onChange(of: model.moments.map(\.momentId)) { _, _ in
                model.applyPreferredPersonalLock()
                handlePendingPushDeepLink()
            }
            .onChange(of: model.selectedContext) { _, _ in
                model.applyPreferredPersonalLock()
                newMomentOpen = false
                groupMomentDirectoryOpen = false
                businessMomentDirectoryOpen = false
                groupCreatePhase = .chooser
            }
            .onReceive(JoinInviteStore.shared.$pendingCode, perform: handleGroupPendingCode)
            .onReceive(JoinCompanyInviteStore.shared.$pendingCode, perform: handleCompanyPendingCode)
            .onReceive(PushDeepLinkStore.shared.$pendingLink, perform: handlePushDeepLink)
            .onChange(of: identity.userId) { _, _ in
                model.bindIdentity(identity)
            }
            .task(id: "\(model.selectedMomentId ?? "")-\(identity.userId)") {
                await refreshGroupViewerReadOnly()
            }
            .onChange(of: model.bottomDestination) { _, _ in
                newMomentOpen = false
                groupMomentDirectoryOpen = false
                businessMomentDirectoryOpen = false
            }
            .onChange(of: scenePhase, handleScenePhaseChange)
    }

    private func handleOnAppear() {
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

    private func handleGroupPendingCode(_ code: String?) {
        guard let code, !code.isEmpty else { return }
        guard pendingGroupJoin == nil else { return }
        if let pending = JoinInviteStore.shared.consume() {
            pendingGroupJoin = PendingGroupJoin(id: pending)
        }
    }

    private func handleCompanyPendingCode(_ code: String?) {
        guard let code, !code.isEmpty else { return }
        guard pendingCompanyJoin == nil else { return }
        if let pending = JoinCompanyInviteStore.shared.consume() {
            pendingCompanyJoin = PendingCompanyJoin(id: pending)
        }
    }

    private func handlePushDeepLink(_ link: String?) {
        guard let link, !link.isEmpty else { return }
        handlePendingPushDeepLink()
    }

    private func handleScenePhaseChange(_: ScenePhase, _ phase: ScenePhase) {
        guard phase == .active else { return }
        model.flushOfflineQueue()
        Task { await inboxBadge.refresh() }
        if model.selectedContext == .group {
            model.refreshVisibleGroupTab()
        }
    }

    private var isStoryPresented: Binding<Bool> {
        Binding<Bool>(
            get: { storyMomentId != nil },
            set: { if !$0 { storyMomentId = nil } }
        )
    }

    private var isGroupCollabPresented: Binding<Bool> {
        Binding<Bool>(
            get: { groupCollabKind != nil },
            set: { if !$0 { groupCollabKind = nil } }
        )
    }

    private var isJoinFeedbackPresented: Binding<Bool> {
        Binding<Bool>(
            get: { joinFeedbackMessage != nil },
            set: { if !$0 { joinFeedbackMessage = nil } }
        )
    }

    private var isLife360Presented: Binding<Bool> {
        Binding<Bool>(
            get: { model.life360Open },
            set: { model.openLife360($0) }
        )
    }

    private var isProfilePresented: Binding<Bool> {
        Binding<Bool>(
            get: { model.profileOpen },
            set: { model.openProfile($0) }
        )
    }

    private var shellPage: some View {
        let base = NativeShellTabView(
            selection: bottomTabSelection,
            accent: momentAccent,
            context: model.selectedContext,
            content: { tabNavigationRoot }
        )
        .background(Color(hex: "#14121B").ignoresSafeArea())

        let b0 = base.fullScreenCover(isPresented: businessCreatePresented) {
            BusinessCreateFlowView(
                createModel: createModel,
                companyId: model.selectedCompany?.companyId,
                ownerName: model.identity?.displayName ?? "",
                onBack: { newMomentOpen = false },
                onCreated: { outcome in
                    newMomentOpen = false
                    model.onMomentCreated(
                        momentId: outcome.momentId,
                        title: outcome.title,
                        momentTypeCode: outcome.momentTypeCode,
                        status: outcome.status
                    )
                }
            )
            .preferredColorScheme(.dark)
        }

        let b1 = attachSetupAndPersonalCovers(to: b0)
        let b2 = attachGroupPrimarySheets(to: b1)
        let b3 = attachGapAndFinanceSheets(to: b2)
        return attachBusinessAndSystemSheets(to: b3)
    }

    /// The Business "+" opens the Figma guided selection as a full-screen page (no tab bar, no shell bar).
    private var businessCreatePresented: Binding<Bool> {
        Binding(
            get: { newMomentOpen && model.selectedContext == .business },
            set: { newMomentOpen = $0 }
        )
    }

    @ViewBuilder
    private func attachSetupAndPersonalCovers<V: View>(to content: V) -> some View {
        content
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
        .fullScreenCover(isPresented: isStoryPresented) {
            if let momentId = storyMomentId {
                MomentStoryViewerView(momentId: momentId) {
                    storyMomentId = nil
                }
                .preferredColorScheme(.dark)
            }
        }
        .onChange(of: operatingChrome.previewStoryId) { _, id in
            if let id {
                storyMomentId = id
                operatingChrome.previewStoryId = nil
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
    }

    @ViewBuilder
    private func attachGroupPrimarySheets<V: View>(to content: V) -> some View {
        content
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
                    isPresented: isGroupCollabPresented,
                    onSaved: { model.refreshVisibleGroupTab() }
                )
            }
        }
    }

    @ViewBuilder
    private func attachGapAndFinanceSheets<V: View>(to content: V) -> some View {
        content
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
    }

    @ViewBuilder
    private func attachBusinessAndSystemSheets<V: View>(to content: V) -> some View {
        content
            .sheet(isPresented: $businessSetupOpen) {
                CompanySetupFlowView(
                    onClose: { businessSetupOpen = false },
                    onActivated: { company in
                        businessSetupOpen = false
                        model.onCompanyCreated(company)
                        if model.selectedContext != .business {
                            model.selectContext(.business)
                        }
                    }
                )
            }
            .sheet(isPresented: $businessJoinOpen) {
                NavigationStack {
                    VStack(spacing: 16) {
                        Text("Join Business")
                            .font(.headline)
                        Text("Enter an invite link or join code to join a business.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { businessJoinOpen = false }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
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
            .alert("Group invite", isPresented: isJoinFeedbackPresented) {
                Button("OK", role: .cancel) { joinFeedbackMessage = nil }
            } message: {
                Text(joinFeedbackMessage ?? "")
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
            .sheet(isPresented: $recentActivityOpen) {
                PersonalRecentActivityFlow(
                    momentId: model.selectedMomentId,
                    isPresented: $recentActivityOpen,
                    onChanged: { model.refreshVisiblePersonalTab() }
                )
            }
            .sheet(isPresented: isLife360Presented) {
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
            .sheet(isPresented: $businessAttentionViewAllOpen) {
                BusinessAppStateViewAllSheet(
                    accent: BusinessAppStateAccent.forTypeCode(model.selectedMomentTypeCode),
                    selectedFilter: $businessAttentionFilter,
                    onClose: { businessAttentionViewAllOpen = false },
                    onShowAll: {
                        businessAttentionViewAllOpen = false
                        businessAttentionFilter = "All"
                    }
                )
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationBackground(BusinessAppStateTokens.sheet)
            }
            .sheet(isPresented: isProfilePresented) {
                AccountHubView(
                    identity: model.identity ?? identity,
                    onDisplayNameSaved: { name in
                        model.applyDisplayName(name)
                        onDisplayNameSaved(name)
                    },
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

    /// The switcher is open for whichever context owns it — Group and Business used to run separate
    /// overlays, now one view is mounted for both.
    private var momentSwitcherDirectoryOpen: Bool {
        (groupMomentDirectoryOpen && model.selectedContext == .group)
            || (businessMomentDirectoryOpen && model.selectedContext == .business)
    }

    private var momentSwitcherIsBusiness: Bool {
        model.selectedContext == .business
    }

    private var momentSwitcherPrefersCompleted: Bool {
        momentSwitcherIsBusiness ? businessDirectoryPreferCompleted : groupDirectoryPreferCompleted
    }

    private func closeMomentSwitcher() {
        withAnimation(.easeInOut(duration: 0.25)) {
            groupMomentDirectoryOpen = false
            groupDirectoryPreferCompleted = false
            businessMomentDirectoryOpen = false
            businessDirectoryPreferCompleted = false
        }
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
                businessMomentDirectoryOpen = false
                if next == .create, model.selectedContext == .group, model.bottomDestination != .create {
                    groupCreatePhase = .chooser
                }
                model.selectBottomDestination(next)
            }
        )
    }

    private var showPersonalExpenseFab: Bool {
        false
    }

    private var shellNavigationTitle: String {
        if model.selectedContext == .personal { return "" }
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
                // One spec-driven switcher serves both contexts (Figma 2678:37791).
                if momentSwitcherDirectoryOpen {
                    MomentSwitcherDirectoryView(
                        spec: momentSwitcherIsBusiness
                            ? businessMomentSwitcherSpec(
                                companyName: model.selectedCompany?.displayName ?? "Your business"
                            )
                            : groupMomentSwitcherSpec(),
                        moments: model.moments,
                        selectedMomentId: model.selectedMomentId,
                        onDismiss: closeMomentSwitcher,
                        onSelectMoment: { momentId in
                            model.selectMoment(id: momentId)
                            closeMomentSwitcher()
                        },
                        onSelectCompletedMoment: { moment in
                            if momentSwitcherIsBusiness {
                                model.selectCompletedBusinessMoment(moment)
                            } else {
                                model.selectCompletedGroupMoment(moment)
                            }
                            closeMomentSwitcher()
                        },
                        onOpenStory: { momentId in
                            storyMomentId = momentId
                            closeMomentSwitcher()
                        },
                        onCreateMoment: {
                            closeMomentSwitcher()
                            openNewMoment()
                        },
                        initialCompletedTab: momentSwitcherPrefersCompleted
                    )
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .move(edge: .bottom)),
                        removal: .opacity.combined(with: .move(edge: .bottom))
                    ))
                    .zIndex(1)
                }
            }
            .animation(.easeInOut(duration: 0.28), value: groupMomentDirectoryOpen)
            .animation(.easeInOut(duration: 0.28), value: businessMomentDirectoryOpen)
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
            .safeAreaInset(edge: .top, spacing: 0) {
                if !momentSwitcherDirectoryOpen {
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
                onQrScan: (model.selectedContext == .group || model.selectedContext == .business)
                    ? { showJoinQrScanner = true }
                    : nil,
                onLife360: { model.openLife360(true) },
                onNewMoment: openNewMoment,
                onRefer: { showReferComingSoon = true },
                onInbox: { inboxOpen = true },
                onAvatar: { model.openProfile(true) },
                unreadNotificationCount: inboxBadge.unreadCount
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
            if shouldShowMomentSwitcher && model.selectedContext != .personal && !newMomentOpen && !groupMomentDirectoryOpen && !businessMomentDirectoryOpen {
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
                                businessDirectoryPreferCompleted = false
                                withAnimation(.easeInOut(duration: 0.28)) {
                                    businessMomentDirectoryOpen = true
                                }
                            } else {
                                groupDirectoryPreferCompleted = false
                                withAnimation(.easeInOut(duration: 0.28)) {
                                    groupMomentDirectoryOpen = true
                                }
                            }
                        },
                        selectedIsCompleted: selectedMomentIsCompleted,
                        startExpanded: false,
                        participantNames: []
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
        } else {
            destinationContent
        }
    }

    @ViewBuilder
    private var destinationContent: some View {
        switch model.contextContent {
        case .idle, .loading:
            if model.selectedContext == .business {
                BusinessAppStateLoadingBody(
                    label: "\(model.bottomDestination.label) · loading",
                    accent: BusinessAppStateAccent.forTypeCode(model.selectedMomentTypeCode)
                )
            } else {
                ProgressView()
                    .tint(MomentraBrandTokens.cta)
            }
        case .offline:
            if model.selectedContext == .business {
                BusinessAppStateBody(
                    spec: BusinessAppStateSpecs.spec(.offline, .error),
                    accent: BusinessAppStateAccent.forTypeCode(model.selectedMomentTypeCode),
                    onPrimary: { model.selectContext(model.selectedContext) },
                    onSecondary: { model.selectBottomDestination(.create) }
                )
            } else {
                emptyPanel(
                    title: "You're offline",
                    body: "Check your connection and try again.",
                    action: "Retry"
                ) {
                    model.selectContext(model.selectedContext)
                }
            }
        case .error(let code, let message):
            if code == "UNAUTHORIZED" {
                Color.clear.onAppear(perform: onSessionExpired)
            }
            if model.selectedContext == .business {
                let accent = BusinessAppStateAccent.forTypeCode(model.selectedMomentTypeCode)
                BusinessAppStateBody(
                    spec: BusinessAppStateSpecs.spec(errorScreen(for: model.bottomDestination), .error),
                    accent: accent,
                    onPrimary: { model.selectContext(model.selectedContext) },
                    onSecondary: { model.selectBottomDestination(.create) }
                )
            } else {
                emptyPanel(title: "We couldn't load your moments", body: message, action: "Retry") {
                    model.selectContext(model.selectedContext)
                }
            }
        case .forbidden:
            emptyPanel(
                title: "No access",
                body: "You no longer have access to this \(model.selectedContext.label.lowercased()) resource. Your session stays signed in."
            )
        case .deferred:
            CircleComingSoonView()
        case .empty:
            emptyDestinationContent
        case .ready(let detail):
            readyDestinationBody(detail: detail)
        }
    }

    @ViewBuilder
    private var emptyDestinationContent: some View {
        ContextEmptyExperienceView(
            createModel: createModel,
            context: model.selectedContext,
            destination: model.bottomDestination,
            experience: model.momentExperience,
            moments: model.moments,
            hasCompany: model.selectedCompany != nil,
            selectedCompanyId: model.selectedCompany?.companyId,
            momentTypeCode: model.selectedMomentTypeCode,
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
            },
            onCreateBusiness: { businessSetupOpen = true },
            onJoinCompany: { businessJoinOpen = true }
        )
    }

    @ViewBuilder
    private func readyDestinationBody(detail: String?) -> some View {
        if model.selectedContext == .group {
            groupReadyDestinationBody
        } else if model.selectedContext == .personal,
                  [.pulse, .moments, .memory, .life, .create].contains(model.bottomDestination) {
            PersonalExperienceHost(
                destination: model.bottomDestination,
                onOpenLife: { model.selectBottomDestination(.life) },
                onOpenPulse: { model.selectBottomDestination(.pulse) },
                onOpenMoments: { model.selectBottomDestination(.moments) }
            )
        } else if model.selectedContext == .business,
                  let momentId = model.selectedMomentId,
                  BusinessOperatingVertical.forSubtype(
                      model.selectedMomentTypeCode
                          ?? model.moments.first(where: { $0.momentId == momentId })?.momentTypeCode
                  ) != nil,
                  [.pulse, .moments, .life, .memory, .create].contains(model.bottomDestination) {
            OperatingExperienceHost(
                momentId: momentId,
                companyId: model.selectedCompany?.companyId
                    ?? model.moments.first(where: { $0.momentId == momentId })?.companyId,
                subtype: model.selectedMomentTypeCode
                    ?? model.moments.first(where: { $0.momentId == momentId })?.momentTypeCode,
                destination: model.bottomDestination,
                refreshToken: model.businessTabRefreshToken,
                onStoryReady: { id in storyMomentId = id }
            )
            .id(momentId)
        } else if model.selectedContext == .business,
                  [.pulse, .moments, .life, .memory, .create].contains(model.bottomDestination),
                  model.selectedMomentId == nil {
            businessEmptyExperience
        } else {
            emptyPanel(
                title: "\(model.selectedContext.label) · \(model.bottomDestination.label)",
                body: detail ?? "Active Moment ready. Product features arrive in later phases."
            )
        }
    }

    @ViewBuilder
    private var businessEmptyExperience: some View {
        ContextEmptyExperienceView(
            createModel: createModel,
            context: model.selectedContext,
            destination: model.bottomDestination,
            experience: model.momentExperience,
            moments: model.moments,
            hasCompany: model.selectedCompany != nil,
            selectedCompanyId: model.selectedCompany?.companyId,
            momentTypeCode: model.selectedMomentTypeCode,
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
            },
            onCreateBusiness: { businessSetupOpen = true },
            onJoinCompany: { businessJoinOpen = true }
        )
    }

    @ViewBuilder
    private var groupReadyDestinationBody: some View {
        let groupTypeCode = model.selectedMomentTypeCode
            ?? model.moments.first(where: { $0.momentId == model.selectedMomentId })?.momentTypeCode
        let groupFamily = GroupExperienceFamily.forTypeCode(groupTypeCode)
        let experienceTheme = ExperienceActiveTheme.forFamily(groupFamily)
        let purchaseTheme = PurchaseActiveTheme.forFamily(groupFamily)
        let livingTheme = LivingActiveTheme.forFamily(groupFamily)

        switch model.bottomDestination {
        case .pulse:
            groupPulseActive(
                family: groupFamily,
                typeCode: groupTypeCode,
                experienceTheme: experienceTheme,
                purchaseTheme: purchaseTheme,
                livingTheme: livingTheme
            )
        case .moments:
            groupMomentsActive(
                family: groupFamily,
                typeCode: groupTypeCode,
                experienceTheme: experienceTheme,
                purchaseTheme: purchaseTheme,
                livingTheme: livingTheme
            )
        case .life:
            groupLifeActive(
                family: groupFamily,
                experienceTheme: experienceTheme,
                purchaseTheme: purchaseTheme,
                livingTheme: livingTheme
            )
        case .memory:
            groupMemoryActive(
                family: groupFamily,
                experienceTheme: experienceTheme,
                purchaseTheme: purchaseTheme,
                livingTheme: livingTheme
            )
        case .create:
            groupQuickAddActive(
                family: groupFamily,
                typeCode: groupTypeCode,
                experienceTheme: experienceTheme,
                purchaseTheme: purchaseTheme,
                livingTheme: livingTheme
            )
        }
    }

    @ViewBuilder
    private func groupPulseActive(
        family: GroupExperienceFamily,
        typeCode: String?,
        experienceTheme: ExperienceActiveTheme,
        purchaseTheme: PurchaseActiveTheme,
        livingTheme: LivingActiveTheme
    ) -> some View {
        if family.isWedding {
            WeddingPulseActiveView(
                refreshToken: model.groupTabRefreshToken,
                momentTitle: model.selectedMomentTitle,
                momentId: model.selectedMomentId,
                momentTypeCode: typeCode,
                onAddExpense: { groupExpenseSheetPresented = true },
                onOpenQuickAdd: { model.selectBottomDestination(.create) },
                onViewSplits: { groupSplitsPresented = true },
                onOpenFinance: { groupFinancePresented = true },
                onQuickAddKind: { kind in weddingGapQa = kind },
                onViewAllActivity: { groupRecentActivityOpen = true }
            )
        } else if family.isThemedExperience {
            ExperiencePulseActiveView(
                theme: experienceTheme,
                refreshToken: model.groupTabRefreshToken,
                momentTitle: model.selectedMomentTitle,
                momentId: model.selectedMomentId,
                momentTypeCode: typeCode,
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
        } else if family.isThemedPurchase {
            PurchasePulseActiveView(
                theme: purchaseTheme,
                refreshToken: model.groupTabRefreshToken,
                momentTitle: model.selectedMomentTitle,
                momentId: model.selectedMomentId,
                momentTypeCode: typeCode,
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
        } else if family.isThemedLiving {
            LivingPulseActiveView(
                theme: livingTheme,
                refreshToken: model.groupTabRefreshToken,
                momentTitle: model.selectedMomentTitle,
                momentId: model.selectedMomentId,
                momentTypeCode: typeCode,
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
                momentTypeCode: typeCode,
                onAddExpense: { groupExpenseSheetPresented = true },
                onViewSplits: { groupSplitsPresented = true },
                onOpenFinance: { groupFinancePresented = true },
                onOpenMemory: { groupCollabKind = .memory },
                onOpenChat: { groupCollabKind = .update },
                onOpenItinerary: { groupCollabKind = .planning },
                onViewAllActivity: { groupRecentActivityOpen = true }
            )
        }
    }

    @ViewBuilder
    private func groupMomentsActive(
        family: GroupExperienceFamily,
        typeCode: String?,
        experienceTheme: ExperienceActiveTheme,
        purchaseTheme: PurchaseActiveTheme,
        livingTheme: LivingActiveTheme
    ) -> some View {
        if family.isWedding {
            WeddingMomentsActiveView(
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                onOpenQuickAdd: { model.selectBottomDestination(.create) }
            )
        } else if family.isThemedExperience {
            ExperienceMomentsActiveView(
                theme: experienceTheme,
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                momentTypeCode: typeCode,
                onOpenQuickAdd: { model.selectBottomDestination(.create) }
            )
        } else if family.isThemedPurchase {
            PurchaseMomentsActiveView(
                theme: purchaseTheme,
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                momentTypeCode: typeCode,
                onOpenQuickAdd: { model.selectBottomDestination(.create) }
            )
        } else if family.isThemedLiving {
            LivingMomentsActiveView(
                theme: livingTheme,
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                momentTypeCode: typeCode,
                onOpenQuickAdd: { model.selectBottomDestination(.create) }
            )
        } else {
            GroupMomentsActiveView(
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                momentTypeCode: typeCode,
                onCreateMoment: {
                    groupCreatePhase = .chooser
                    newMomentOpen = true
                }
            )
        }
    }

    @ViewBuilder
    private func groupLifeActive(
        family: GroupExperienceFamily,
        experienceTheme: ExperienceActiveTheme,
        purchaseTheme: PurchaseActiveTheme,
        livingTheme: LivingActiveTheme
    ) -> some View {
        let isWedding = family.isWedding
        let isExperience = family.isThemedExperience
        let isPurchase = family.isThemedPurchase
        let isLiving = family.isThemedLiving
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
    }

    @ViewBuilder
    private func groupMemoryActive(
        family: GroupExperienceFamily,
        experienceTheme: ExperienceActiveTheme,
        purchaseTheme: PurchaseActiveTheme,
        livingTheme: LivingActiveTheme
    ) -> some View {
        if family.isWedding {
            WeddingMemoryActiveView(
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                onOpenQuickAdd: { weddingGapQa = .memory }
            )
        } else if family.isThemedExperience {
            ExperienceMemoryActiveView(
                theme: experienceTheme,
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                onOpenQuickAdd: { experienceGapQa = .memory }
            )
        } else if family.isThemedPurchase {
            PurchaseMemoryActiveView(
                theme: purchaseTheme,
                refreshToken: model.groupTabRefreshToken,
                momentId: model.selectedMomentId,
                momentTitle: model.selectedMomentTitle,
                onOpenQuickAdd: { purchaseGapQa = .memory }
            )
        } else if family.isThemedLiving {
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
    }

    @ViewBuilder
    private func groupQuickAddActive(
        family: GroupExperienceFamily,
        typeCode: String?,
        experienceTheme: ExperienceActiveTheme,
        purchaseTheme: PurchaseActiveTheme,
        livingTheme: LivingActiveTheme
    ) -> some View {
        if family.isWedding {
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
        } else if family.isThemedExperience {
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
        } else if family.isThemedPurchase {
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
        } else if family.isThemedLiving {
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

    /// Which kit screen the current bottom destination maps to, for error and loading bodies.
    private func errorScreen(for destination: BottomDestination) -> BusinessAppStateScreen {
        switch destination {
        case .pulse: return .pulse
        case .moments: return .moments
        case .life: return .life
        case .memory: return .memory
        case .create: return .hub
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
