import SwiftUI
import UIKit

/// Invite link/QR + active members manage (organizer role edit / remove).
struct GroupInvitePeopleSheet: View {
    let momentId: String
    let momentTitle: String
    let momentTypeCode: String
    let currentUserId: String?
    @Binding var isPresented: Bool
    var onSaved: () -> Void = {}

    @State private var inviteCode: String?
    @State private var minting = true
    @State private var mintError: String?
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var actionError: String?
    @State private var busyParticipantId: String?
    @State private var removeTarget: APIClient.GroupParticipantPayload?
    @State private var copied = false
    @State private var guestName = ""
    @State private var addingGuest = false

    private let sheetBg = TripSheetTokens.bg
    private let roleOptions: [(code: String, label: String)] = [
        ("PARTICIPANT", "Member"),
        ("ORGANIZER", "Organizer"),
        ("OBSERVER", "Viewer"),
    ]

    private var displayPath: String? {
        inviteCode.map { GroupInviteLink.displayPath(code: $0) }
    }

    private var copyText: String? {
        inviteCode.map { GroupInviteLink.copyText(code: $0) }
    }

    private var qrPayload: String? {
        inviteCode.map { GroupInviteLink.qrPayload(code: $0) }
    }

    private var activeMembers: [APIClient.GroupParticipantPayload] {
        participants.filter { ($0.status ?? "").uppercased() == "ACTIVE" }
    }

    private var viewerIsOrganizer: Bool {
        guard let currentUserId else { return false }
        return activeMembers.contains { p in
            p.userId == currentUserId &&
            ["ORGANIZER", "CO_ORGANIZER"].contains((p.roleCode ?? "").uppercased())
        }
    }

    var body: some View {
        NativeSheetScaffold(
            title: "Invite People",
            onClose: { isPresented = false },
            background: sheetBg
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TripSheetHeader(
                        iconAsset: "GroupQaUserPlus",
                        title: "Invite People",
                        subtitle: "Share a link or QR so people can join",
                        accent: TripForm.accent
                    )

                    TripInviteShareSection(
                        minting: minting,
                        displayPath: displayPath,
                        mintError: mintError,
                        copied: $copied,
                        copyText: copyText,
                        qrPayload: qrPayload,
                        momentTitle: momentTitle
                    )

                    if viewerIsOrganizer {
                        VStack(alignment: .leading, spacing: 8) {
                            TripFieldLabel(text: "Add guest")
                            Text("Guests have no account — organizers can add them to expenses.")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(TripForm.muted)
                            HStack(spacing: 8) {
                                TextField("Guest name", text: $guestName)
                                    .font(.plusJakarta(size: 13))
                                    .foregroundStyle(TripForm.text)
                                    .padding(10)
                                    .background(TripForm.field)
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                    .accessibilityIdentifier("group.invite.guestName")
                                Button {
                                    Task { await addGuest() }
                                } label: {
                                    Text(addingGuest ? "…" : "Add")
                                        .font(.plusJakarta(size: 13, weight: .semibold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 10)
                                        .background(TripForm.accent)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                                .buttonStyle(.plain)
                                .disabled(addingGuest || guestName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                                .accessibilityIdentifier("group.invite.addGuest")
                            }
                        }
                    }

                    if !activeMembers.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            TripFieldLabel(text: "Active members (\(activeMembers.count))")
                            ForEach(activeMembers) { p in
                                memberRow(p)
                            }
                        }
                    }

                    if let actionError {
                        Text(actionError)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#F87171"))
                    }

                    Text("Share via Messages or WhatsApp so they can join")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(TripForm.muted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(24)
            }
        } footer: {
            EmptyView()
        }
        .presentationDetents([.large])
        .task(id: "\(momentId)|\(momentTitle)|\(momentTypeCode)") {
            await bootstrap()
        }
        .confirmationDialog(
            "Remove member?",
            isPresented: Binding(
                get: { removeTarget != nil },
                set: { if !$0 { removeTarget = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("Remove", role: .destructive) {
                if let target = removeTarget {
                    removeTarget = nil
                    Task { await removeMember(target) }
                }
            }
            Button("Cancel", role: .cancel) { removeTarget = nil }
        } message: {
            Text("Remove \(removeTarget?.displayName ?? "this member") from the group?")
        }
    }

    @ViewBuilder
    private func memberRow(_ p: APIClient.GroupParticipantPayload) -> some View {
        let name = (p.displayName?.isEmpty == false) ? (p.displayName ?? "Member") : "Member"
        let busy = busyParticipantId == p.participantId
        let selected = uiRoleCode(p.roleCode)
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Circle()
                    .fill(TripForm.accent.opacity(0.2))
                    .frame(width: 28, height: 28)
                    .overlay(
                        Text(tripInitials(name))
                            .font(.plusJakarta(size: 10, weight: .bold))
                            .foregroundStyle(TripForm.accent)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 6) {
                        Text(name)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(TripForm.text)
                        if p.guest {
                            Text("Guest")
                                .font(.plusJakarta(size: 10, weight: .semibold))
                                .foregroundStyle(TripForm.accent)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(TripForm.accent.opacity(0.15))
                                .clipShape(Capsule())
                        }
                    }
                    Text(p.roleLabel ?? displayRoleLabel(p.roleCode))
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(TripForm.muted)
                }
                Spacer()
                Text("Active")
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(TripForm.green)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(TripForm.green.opacity(0.12))
                    .clipShape(Capsule())
                if viewerIsOrganizer {
                    Button {
                        removeTarget = p
                    } label: {
                        Text("Remove")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(busy ? TripForm.muted : Color(hex: "#EF4444"))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color(hex: "#EF4444").opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                    .disabled(busy)
                    .accessibilityLabel("Remove \(p.displayName ?? "member")")
                }
            }
            if viewerIsOrganizer && !p.guest {
                HStack(spacing: 8) {
                    ForEach(roleOptions, id: \.code) { opt in
                        let isSelected = selected == opt.code
                        Text(opt.label)
                            .font(.plusJakarta(size: 11, weight: .semibold))
                            .foregroundStyle(isSelected ? TripForm.text : TripForm.muted)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(isSelected ? TripForm.accent.opacity(0.25) : TripForm.field)
                            .overlay(
                                Capsule().stroke(isSelected ? TripForm.accent : TripForm.border)
                            )
                            .clipShape(Capsule())
                            .onTapGesture {
                                guard !busy, !isSelected else { return }
                                Task { await changeRole(p, to: opt.code) }
                            }
                    }
                }
            }
        }
        .padding(10)
        .background(TripForm.field)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func displayRoleLabel(_ roleCode: String?) -> String {
        switch (roleCode ?? "").uppercased() {
        case "ORGANIZER", "CO_ORGANIZER": return "Organizer"
        case "OBSERVER", "VIEWER": return "Viewer"
        case "RESIDENT": return "Resident"
        case "CONTRIBUTOR": return "Contributor"
        default: return "Member"
        }
    }

    private func uiRoleCode(_ roleCode: String?) -> String {
        switch (roleCode ?? "").uppercased() {
        case "ORGANIZER", "CO_ORGANIZER": return "ORGANIZER"
        case "OBSERVER", "VIEWER": return "OBSERVER"
        default: return "PARTICIPANT"
        }
    }

    private func bootstrap() async {
        inviteCode = nil
        minting = true
        mintError = nil
        do {
            let invite = try await APIClient.shared.mintGroupInvite(
                title: momentTitle.isEmpty ? "Trip" : momentTitle,
                momentTypeCode: momentTypeCode.isEmpty ? "TRIP" : momentTypeCode,
                momentId: momentId,
                idempotencyKey: UUID().uuidString
            )
            inviteCode = invite.inviteCode
            minting = false
        } catch {
            mintError = error.localizedDescription
            minting = false
        }
        await refreshParticipants()
    }

    private func refreshParticipants() async {
        do {
            participants = try await APIClient.shared.listGroupParticipants(momentId: momentId)
        } catch {
            // best-effort list
        }
    }

    private func addGuest() async {
        let name = guestName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return }
        addingGuest = true
        actionError = nil
        defer { addingGuest = false }
        do {
            _ = try await APIClient.shared.addGroupParticipant(momentId: momentId, displayName: name)
            guestName = ""
            await refreshParticipants()
            onSaved()
        } catch {
            actionError = error.localizedDescription
        }
    }

    private func changeRole(_ p: APIClient.GroupParticipantPayload, to roleCode: String) async {
        busyParticipantId = p.participantId
        actionError = nil
        do {
            _ = try await APIClient.shared.updateGroupParticipantRole(
                momentId: momentId,
                participantId: p.participantId,
                roleCode: roleCode
            )
            await refreshParticipants()
            onSaved()
        } catch {
            actionError = error.localizedDescription
        }
        busyParticipantId = nil
    }

    private func removeMember(_ p: APIClient.GroupParticipantPayload) async {
        busyParticipantId = p.participantId
        actionError = nil
        do {
            _ = try await APIClient.shared.removeGroupParticipant(
                momentId: momentId,
                participantId: p.participantId
            )
            await refreshParticipants()
            onSaved()
        } catch {
            actionError = error.localizedDescription
        }
        busyParticipantId = nil
    }
}
