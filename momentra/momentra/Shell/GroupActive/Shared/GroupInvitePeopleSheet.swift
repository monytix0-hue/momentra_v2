import SwiftUI
import UIKit

/// Trip Quick Add — mint invite link + add participant (Figma 581:13699).
struct GroupInvitePeopleSheet: View {
    let momentId: String
    let momentTitle: String
    let momentTypeCode: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void = {}

    @State private var inviteCode: String?
    @State private var minting = true
    @State private var mintError: String?
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var name = ""
    @State private var email = ""
    @State private var role = "PARTICIPANT"
    @State private var submitting = false
    @State private var formError: String?
    @State private var copied = false

    private let sheetBg = TripSheetTokens.bg

    private var displayPath: String? {
        inviteCode.map { GroupInviteLink.displayPath(code: $0) }
    }

    private var copyText: String? {
        inviteCode.map { GroupInviteLink.copyText(code: $0) }
    }

    private var qrPayload: String? {
        inviteCode.map { GroupInviteLink.qrPayload(code: $0) }
    }

    private var invitePeopleSubtitle: String {
        GroupExperienceFamily.forTypeCode(momentTypeCode).invitePeopleSubtitle
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
                        subtitle: invitePeopleSubtitle,
                        accent: TripForm.accent
                    )

                    TripInviteShareSection(
                        minting: minting,
                        displayPath: displayPath,
                        mintError: mintError,
                        copied: $copied,
                        copyText: copyText,
                        qrPayload: qrPayload
                    )

                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 6) {
                            TripFieldLabel(text: "Name")
                            TripSheetField(value: $name, placeholder: "Aarav Mehta")
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            TripFieldLabel(text: "Email/Phone")
                            TripSheetField(value: $email, placeholder: "aarav@email.com", keyboardType: .emailAddress)
                        }
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        TripFieldLabel(text: "Assigned Role")
                        HStack(spacing: 8) {
                            roleChip("PARTICIPANT", label: "Member")
                            roleChip("ORGANIZER", label: "Organizer")
                            roleChip("VIEWER", label: "Viewer")
                        }
                    }

                    if !participants.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            TripFieldLabel(text: "Already Invited (\(participants.count))")
                            ForEach(participants) { p in
                                HStack {
                                    Circle()
                                        .fill(TripForm.accent.opacity(0.2))
                                        .frame(width: 28, height: 28)
                                        .overlay(
                                            Text(tripInitials(p.displayName ?? "?"))
                                                .font(.plusJakarta(size: 10, weight: .bold))
                                                .foregroundStyle(TripForm.accent)
                                        )
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(p.displayName ?? "Participant")
                                            .font(.plusJakarta(size: 13, weight: .semibold))
                                            .foregroundStyle(TripForm.text)
                                        Text(p.status ?? "Member")
                                            .font(.plusJakarta(size: 11))
                                            .foregroundStyle(TripForm.muted)
                                    }
                                    Spacer()
                                    Text((p.status ?? "Pending").capitalized)
                                        .font(.plusJakarta(size: 11, weight: .semibold))
                                        .foregroundStyle(TripForm.green)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(TripForm.green.opacity(0.12))
                                        .clipShape(Capsule())
                                }
                                .padding(8)
                                .background(TripForm.field)
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                        }
                    }

                    if let formError {
                        Text(formError)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#F87171"))
                    }
                }
                .padding(24)
            }
        } footer: {
            TripPrimaryCta(
                label: "Send Invite",
                enabled: !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !submitting,
                loading: submitting,
                footer: "Share the invite link so they can join",
                colors: [TripForm.accent, TripSheetTokens.accentEnd],
                onTap: { Task { await addParticipant() } }
            )
            .padding(.horizontal, 24)
            .padding(.bottom, 8)
            .background(sheetBg)
        }
        .presentationDetents([.large])
        .task(id: "\(momentId)|\(momentTitle)|\(momentTypeCode)") {
            await bootstrap()
        }
    }

    private func roleChip(_ code: String, label: String) -> some View {
        let selected = role == code
        return Text(label)
            .font(.plusJakarta(size: 12, weight: .semibold))
            .foregroundStyle(selected ? TripForm.text : TripForm.muted)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
            .background(selected ? TripForm.accent.opacity(0.2) : TripForm.field)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(selected ? TripForm.accent : TripForm.border)
            )
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .onTapGesture { role = code }
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

    private func addParticipant() async {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        submitting = true
        formError = nil
        do {
            var emailTrimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
            var phone: String? = nil
            if !emailTrimmed.isEmpty {
                let looksLikePhone = emailTrimmed.contains(where: { $0.isNumber })
                    && !emailTrimmed.contains("@")
                if looksLikePhone {
                    phone = emailTrimmed
                    emailTrimmed = ""
                }
            }
            _ = try await APIClient.shared.addGroupParticipant(
                momentId: momentId,
                displayName: trimmed,
                roleCode: role,
                email: emailTrimmed.isEmpty ? nil : emailTrimmed,
                phone: phone
            )
            name = ""
            email = ""
            await refreshParticipants()
            onSaved()
        } catch {
            formError = error.localizedDescription
        }
        submitting = false
    }
}
