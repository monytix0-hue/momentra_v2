import SwiftUI

/// Figma 546:30147 / 353:22147 Manage Moment + rename + delete + leave sheets.
enum ManageMomentSubsheet: String, Identifiable {
    case rename
    case delete
    case leaveTransfer
    case leaveConfirm
    var id: String { rawValue }
}

private enum ManageMomentTokens {
    static let sheetBg = Color(hex: "#161B26")
    static let rowBg = Color(hex: "#14121B")
    static let text = Color(hex: "#F5F2FC")
    static let muted = Color(hex: "#9CA3AF")
    static let purple = Color(hex: "#7C5CFC")
    static let blue = Color(hex: "#3B82F6")
    static let green = Color(hex: "#10B981")
    static let red = Color(hex: "#EF4444")
    static let redDark = Color(hex: "#DC2626")
    static let redText = Color(hex: "#FF5961")
    static let destructiveBg = Color(hex: "#2A1A1A")
    static let border = Color(hex: "#1E293B")
}

private struct LeaveCandidate: Identifiable {
    var id: String { userId }
    let userId: String
    let displayName: String
    let roleLabel: String
}

struct ManageMomentFlowSheet: View {
    let momentId: String
    let momentTitle: String
    let domain: AppContextKind
    let currentUserId: String
    var companyId: String? = nil
    @Binding var isPresented: Bool
    var onEditSetup: () -> Void
    var onLifecycleChanged: () -> Void
    var onLeft: () -> Void = {}

    @State private var subsheet: ManageMomentSubsheet?
    @State private var busy = false
    @State private var errorText: String?
    @State private var confirmPause = false
    @State private var confirmComplete = false
    @State private var viewerIsLeader = false
    @State private var candidates: [LeaveCandidate] = []
    @State private var transferUserId: String?

    private var supportsLeave: Bool { domain == .group || domain == .business }
    private var leaveNoun: String { domain == .business ? "Company" : "Group" }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture { isPresented = false }

            VStack(spacing: 0) {
                Capsule()
                    .fill(ManageMomentTokens.border)
                    .frame(width: 36, height: 4)
                    .padding(.top, 12)
                    .padding(.bottom, 16)

                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Manage Moment")
                            .font(.system(size: 19, weight: .bold))
                            .foregroundStyle(ManageMomentTokens.text)
                        Text(momentTitle)
                            .font(.system(size: 10))
                            .foregroundStyle(ManageMomentTokens.muted)
                    }
                    Spacer()
                    Button { isPresented = false } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(ManageMomentTokens.purple)
                            .frame(width: 24, height: 24)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)

                VStack(spacing: 4) {
                    manageRow(
                        icon: "gearshape.fill",
                        well: ManageMomentTokens.purple,
                        title: "Edit setup",
                        subtitle: "Revisit priorities and configuration"
                    ) {
                        isPresented = false
                        onEditSetup()
                    }
                    manageRow(
                        icon: "square.and.pencil",
                        well: ManageMomentTokens.blue,
                        title: "Edit moment name",
                        subtitle: "Rename how this moment appears"
                    ) {
                        subsheet = .rename
                    }
                    if !supportsLeave || viewerIsLeader {
                        manageRow(
                            icon: "pause.fill",
                            well: ManageMomentTokens.blue,
                            title: "Pause rhythm",
                            subtitle: "Pause tracking without losing your data"
                        ) {
                            confirmPause = true
                        }
                        manageRow(
                            icon: "checkmark",
                            well: ManageMomentTokens.green,
                            title: "Complete Chapter",
                            subtitle: "Mark this moment as finished"
                        ) {
                            confirmComplete = true
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)

                if !supportsLeave || viewerIsLeader {
                    Spacer().frame(height: 8)
                    Button { subsheet = .delete } label: {
                        HStack(spacing: 12) {
                            iconWell(systemName: "trash.fill", well: ManageMomentTokens.red)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Delete permanently")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(ManageMomentTokens.redText)
                                Text("Removes this moment; analytics kept")
                                    .font(.system(size: 10))
                                    .foregroundStyle(ManageMomentTokens.muted)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(ManageMomentTokens.muted)
                        }
                        .padding(12)
                        .background(ManageMomentTokens.destructiveBg, in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(ManageMomentTokens.red, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, 16)
                }

                if supportsLeave {
                    Spacer().frame(height: 8)
                    let leaveEnabled = !(viewerIsLeader && candidates.isEmpty)
                    Button {
                        transferUserId = nil
                        errorText = nil
                        subsheet = viewerIsLeader ? .leaveTransfer : .leaveConfirm
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(leaveEnabled ? ManageMomentTokens.red : ManageMomentTokens.muted)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color(hex: "#EF4444").opacity(0.1), in: RoundedRectangle(cornerRadius: 8))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color(hex: "#EF4444").opacity(0.2), lineWidth: 1)
                                )
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Leave \(leaveNoun)")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(leaveEnabled ? ManageMomentTokens.redText : ManageMomentTokens.muted)
                                Text(
                                    viewerIsLeader && candidates.isEmpty
                                        ? "Invite someone else before you can leave"
                                        : "You will lose access to shared content"
                                )
                                .font(.system(size: 10))
                                .foregroundStyle(ManageMomentTokens.muted)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(ManageMomentTokens.muted)
                        }
                        .padding(12)
                        .background(Color(hex: "#EF4444").opacity(leaveEnabled ? 0.1 : 0.04), in: RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color(hex: "#EF4444").opacity(leaveEnabled ? 0.2 : 0.1), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(!leaveEnabled)
                    .padding(.horizontal, 16)
                }

                if let errorText {
                    Text(errorText)
                        .font(.system(size: 12))
                        .foregroundStyle(ManageMomentTokens.redText)
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                }

                Spacer().frame(height: 24)
            }
            .frame(maxWidth: .infinity)
            .background(ManageMomentTokens.sheetBg)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: .black.opacity(0.5), radius: 12, y: -4)
        }
        .task { await loadLeaveContext() }
        .sheet(item: $subsheet) { kind in
            switch kind {
            case .rename:
                ManageMomentRenameSheet(
                    momentId: momentId,
                    momentTitle: momentTitle,
                    isPresented: Binding(
                        get: { subsheet != nil },
                        set: { if !$0 { subsheet = nil } }
                    ),
                    onSaved: {
                        subsheet = nil
                        isPresented = false
                        onLifecycleChanged()
                    }
                )
                .presentationDetents([.height(320)])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(20)
                .preferredColorScheme(.dark)
            case .delete:
                ManageMomentDeleteSheet(
                    momentId: momentId,
                    momentTitle: momentTitle,
                    isPresented: Binding(
                        get: { subsheet != nil },
                        set: { if !$0 { subsheet = nil } }
                    ),
                    onDeleted: {
                        subsheet = nil
                        isPresented = false
                        onLifecycleChanged()
                    }
                )
                .presentationDetents([.height(360)])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(20)
                .preferredColorScheme(.dark)
            case .leaveTransfer:
                LeaveTransferSheet(
                    leaveNoun: leaveNoun,
                    candidates: candidates,
                    selectedUserId: $transferUserId,
                    errorText: errorText,
                    onContinue: {
                        if transferUserId == nil {
                            errorText = "Select a member to become the new \(domain == .business ? "admin" : "organizer")."
                        } else {
                            errorText = nil
                            subsheet = .leaveConfirm
                        }
                    },
                    onClose: { subsheet = nil }
                )
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(24)
                .preferredColorScheme(.dark)
            case .leaveConfirm:
                LeaveConfirmSheet(
                    momentTitle: momentTitle,
                    leaveNoun: leaveNoun,
                    busy: busy,
                    errorText: errorText,
                    onConfirm: { Task { await runLeave() } },
                    onClose: {
                        subsheet = viewerIsLeader ? .leaveTransfer : nil
                    }
                )
                .presentationDetents([.height(420)])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(24)
                .preferredColorScheme(.dark)
            }
        }
        .confirmationDialog("Pause rhythm?", isPresented: $confirmPause, titleVisibility: .visible) {
            Button("Pause", role: .destructive) {
                Task { await runLifecycle(.archive) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Pause tracking without losing your data.")
        }
        .confirmationDialog("Complete Chapter?", isPresented: $confirmComplete, titleVisibility: .visible) {
            Button("Complete", role: .destructive) {
                Task { await runLifecycle(.cancel) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Mark this moment as finished.")
        }
        .disabled(busy)
    }

    private enum LifecycleAction { case archive, cancel }

    private func loadLeaveContext() async {
        switch domain {
        case .group:
            do {
                let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
                let active = list.filter { ($0.status ?? "") == "ACTIVE" && ($0.userId?.isEmpty == false) }
                let me = active.first { $0.userId == currentUserId }
                viewerIsLeader = me?.roleCode == "ORGANIZER" || me?.roleCode == "CO_ORGANIZER"
                candidates = active.compactMap { p in
                    guard let uid = p.userId, uid != currentUserId else { return nil }
                    return LeaveCandidate(
                        userId: uid,
                        displayName: (p.displayName?.isEmpty == false) ? (p.displayName ?? "Member") : "Member",
                        roleLabel: p.roleCode ?? "PARTICIPANT"
                    )
                }
            } catch {
                viewerIsLeader = false
                candidates = []
            }
        case .business:
            guard let companyId, !companyId.isEmpty else {
                viewerIsLeader = false
                candidates = []
                return
            }
            do {
                let list = try await APIClient.shared.listCompanyMembers(companyId: companyId)
                let active = list.filter { $0.status == "ACTIVE" }
                let me = active.first { $0.userId == currentUserId }
                viewerIsLeader = me?.membershipType == "OWNER" || me?.membershipType == "ADMIN"
                candidates = active.compactMap { m in
                    guard m.userId != currentUserId else { return nil }
                    return LeaveCandidate(
                        userId: m.userId,
                        displayName: (m.displayName?.isEmpty == false) ? (m.displayName ?? "Member") : "Member",
                        roleLabel: m.membershipType
                    )
                }
            } catch {
                viewerIsLeader = false
                candidates = []
            }
        default:
            viewerIsLeader = true
            candidates = []
        }
    }

    private func runLeave() async {
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            switch domain {
            case .group:
                _ = try await APIClient.shared.leaveGroupMoment(momentId: momentId, transferUserId: transferUserId)
            case .business:
                guard let companyId else { throw URLError(.badURL) }
                _ = try await APIClient.shared.leaveCompany(companyId: companyId, transferUserId: transferUserId)
            default:
                return
            }
            subsheet = nil
            isPresented = false
            onLeft()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func runLifecycle(_ action: LifecycleAction) async {
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
            switch action {
            case .archive:
                _ = try await APIClient.shared.archiveMoment(momentId: momentId, expectedVersion: detail.version)
            case .cancel:
                _ = try await APIClient.shared.cancelMoment(momentId: momentId, expectedVersion: detail.version)
            }
            isPresented = false
            onLifecycleChanged()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func manageRow(
        icon: String,
        well: Color,
        title: String,
        subtitle: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                iconWell(systemName: icon, well: well)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(ManageMomentTokens.text)
                    Text(subtitle)
                        .font(.system(size: 10))
                        .foregroundStyle(ManageMomentTokens.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ManageMomentTokens.muted)
            }
            .padding(12)
            .background(ManageMomentTokens.rowBg, in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func iconWell(systemName: String, well: Color) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 32, height: 32)
            .background(well, in: Circle())
    }
}

private struct LeaveTransferSheet: View {
    let leaveNoun: String
    let candidates: [LeaveCandidate]
    @Binding var selectedUserId: String?
    var errorText: String?
    var onContinue: () -> Void
    var onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Capsule()
                .fill(ManageMomentTokens.border)
                .frame(width: 36, height: 4)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)

            Text(leaveNoun == "Company" ? "Choose a new admin" : "Choose a new organizer")
                .font(.system(size: 19, weight: .bold))
                .foregroundStyle(ManageMomentTokens.text)
            Text("They will manage this \(leaveNoun) after you leave")
                .font(.system(size: 12))
                .foregroundStyle(ManageMomentTokens.muted)

            ScrollView {
                VStack(spacing: 8) {
                    ForEach(candidates) { c in
                        let selected = c.userId == selectedUserId
                        Button { selectedUserId = c.userId } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(c.displayName)
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(ManageMomentTokens.text)
                                    Text(c.roleLabel)
                                        .font(.system(size: 11))
                                        .foregroundStyle(ManageMomentTokens.muted)
                                }
                                Spacer()
                                if selected {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(ManageMomentTokens.red)
                                }
                            }
                            .padding(12)
                            .background(
                                selected ? Color(hex: "#EF4444").opacity(0.1) : ManageMomentTokens.rowBg,
                                in: RoundedRectangle(cornerRadius: 12)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(selected ? ManageMomentTokens.red : ManageMomentTokens.border, lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if let errorText {
                Text(errorText)
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.redText)
            }

            Button(action: onContinue) {
                Text("Continue")
                    .font(.system(size: 15, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(ManageMomentTokens.red, in: RoundedRectangle(cornerRadius: 16))
            }
            .buttonStyle(.plain)
            .disabled(selectedUserId == nil)

            Button("Cancel", action: onClose)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(ManageMomentTokens.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
        }
        .padding(16)
        .background(ManageMomentTokens.sheetBg.ignoresSafeArea())
    }
}

private struct LeaveConfirmSheet: View {
    let momentTitle: String
    let leaveNoun: String
    let busy: Bool
    var errorText: String?
    var onConfirm: () -> Void
    var onClose: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Capsule()
                .fill(ManageMomentTokens.border)
                .frame(width: 36, height: 4)
                .padding(.top, 8)

            Image(systemName: "rectangle.portrait.and.arrow.right")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(ManageMomentTokens.red)
                .frame(width: 52, height: 52)
                .background(Color(hex: "#EF4444").opacity(0.1), in: Circle())
                .overlay(Circle().stroke(Color(hex: "#EF4444").opacity(0.2), lineWidth: 1))

            Text("Leave \(momentTitle)?")
                .font(.system(size: 20, weight: .heavy))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)

            Text("You will lose access to shared moments, schedules, and household data. This action cannot be undone.")
                .font(.system(size: 14))
                .foregroundStyle(ManageMomentTokens.muted)
                .multilineTextAlignment(.center)
                .lineSpacing(4)

            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.red)
                Text("This will permanently remove you from all shared content.")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ManageMomentTokens.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color(hex: "#EF4444").opacity(0.05), in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#EF4444").opacity(0.2), lineWidth: 1))

            if let errorText {
                Text(errorText)
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.redText)
            }

            HStack(spacing: 12) {
                Button(action: onClose) {
                    Text("Cancel")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(ManageMomentTokens.border, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .disabled(busy)

                Button(action: onConfirm) {
                    Group {
                        if busy {
                            ProgressView().tint(.white)
                        } else {
                            Text("Leave \(leaveNoun)")
                                .font(.system(size: 15, weight: .heavy))
                                .foregroundStyle(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        LinearGradient(
                            colors: [ManageMomentTokens.redDark, ManageMomentTokens.red],
                            startPoint: .leading,
                            endPoint: .trailing
                        ),
                        in: RoundedRectangle(cornerRadius: 16)
                    )
                }
                .buttonStyle(.plain)
                .disabled(busy)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(ManageMomentTokens.sheetBg.ignoresSafeArea())
    }
}

struct ManageMomentRenameSheet: View {
    let momentId: String
    let momentTitle: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var name: String = ""
    @State private var busy = false
    @State private var errorText: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Edit Moment Name")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.text)
                    Text(momentTitle)
                        .font(.system(size: 10))
                        .foregroundStyle(ManageMomentTokens.muted)
                }
                Spacer()
                Button { isPresented = false } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.purple)
                }
                .buttonStyle(.plain)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("MOMENT NAME")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(ManageMomentTokens.muted)
                HStack(spacing: 12) {
                    Image(systemName: "square.and.pencil")
                        .foregroundStyle(ManageMomentTokens.blue)
                    TextField("Moment name", text: $name)
                        .foregroundStyle(ManageMomentTokens.text)
                }
                .padding(12)
                .background(ManageMomentTokens.rowBg, in: RoundedRectangle(cornerRadius: 12))
            }

            if let errorText {
                Text(errorText)
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.redText)
            }

            Button {
                Task { await save() }
            } label: {
                if busy {
                    ProgressView().tint(.white).frame(maxWidth: .infinity).padding(.vertical, 12)
                } else {
                    Text("Save")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
            }
            .background(ManageMomentTokens.purple, in: RoundedRectangle(cornerRadius: 12))
            .disabled(busy || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            Button("Cancel") { isPresented = false }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(ManageMomentTokens.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            Spacer(minLength: 0)
        }
        .padding(16)
        .background(ManageMomentTokens.sheetBg.ignoresSafeArea())
        .onAppear { name = momentTitle }
    }

    private func save() async {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
            _ = try await APIClient.shared.updateMoment(
                momentId: momentId,
                title: trimmed,
                expectedVersion: detail.version
            )
            onSaved()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

struct ManageMomentDeleteSheet: View {
    let momentId: String
    let momentTitle: String
    @Binding var isPresented: Bool
    var onDeleted: () -> Void

    @State private var busy = false
    @State private var errorText: String?

    var body: some View {
        VStack(spacing: 16) {
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 40, height: 4)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Delete Permanently?")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.text)
                    Text(momentTitle)
                        .font(.system(size: 10))
                        .foregroundStyle(ManageMomentTokens.muted)
                }
                Spacer()
                Button { isPresented = false } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.purple)
                }
                .buttonStyle(.plain)
            }

            Image(systemName: "trash.fill")
                .font(.system(size: 22))
                .foregroundStyle(.white)
                .frame(width: 48, height: 48)
                .background(ManageMomentTokens.red, in: Circle())

            Text("This cannot be undone. The moment and operational data will be removed; analytics are kept.")
                .font(.system(size: 13))
                .foregroundStyle(ManageMomentTokens.muted)
                .multilineTextAlignment(.center)

            if let errorText {
                Text(errorText)
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.redText)
            }

            Button {
                Task { await delete() }
            } label: {
                if busy {
                    ProgressView().tint(.white).frame(maxWidth: .infinity).padding(.vertical, 12)
                } else {
                    Text("Delete")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
            }
            .background(ManageMomentTokens.red, in: RoundedRectangle(cornerRadius: 12))
            .disabled(busy)

            Button("Cancel") { isPresented = false }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(ManageMomentTokens.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            Spacer(minLength: 0)
        }
        .padding(16)
        .background(ManageMomentTokens.sheetBg.ignoresSafeArea())
    }

    private func delete() async {
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
            _ = try await APIClient.shared.deleteMoment(momentId: momentId, expectedVersion: detail.version)
            onDeleted()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
