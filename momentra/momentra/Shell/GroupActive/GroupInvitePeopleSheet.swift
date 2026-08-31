import SwiftUI
import UIKit

/// Trip Quick Add — mint invite link + add participant (Figma 575:15497 chrome).
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
    private let fieldBg = TripSheetTokens.field
    private let border = TripSheetTokens.border
    private let muted = TripSheetTokens.muted

    private var displayPath: String? {
        inviteCode.map { GroupInviteLink.displayPath(code: $0) }
    }

    private var copyText: String? {
        inviteCode.map { GroupInviteLink.copyText(code: $0) }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Text("👥")
                        .font(.system(size: 16))
                        .frame(width: 36, height: 36)
                        .background(TripSheetTokens.accent.opacity(0.18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(TripSheetTokens.accent.opacity(0.35)))
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Invite people")
                            .font(.plusJakarta(size: 18, weight: .heavy))
                            .foregroundStyle(TripSheetTokens.text)
                        Text("Share a link or add someone to this trip")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(muted)
                    }
                }

                    inviteLinkBlock

                    field("Name", text: $name, placeholder: "Display name")
                    field("Email (optional)", text: $email, placeholder: "name@email.com")

                    VStack(alignment: .leading, spacing: 8) {
                        Text("ROLE")
                            .font(.plusJakarta(size: 11, weight: .bold))
                            .foregroundStyle(muted)
                        HStack(spacing: 8) {
                            roleChip("PARTICIPANT", label: "Guest")
                            roleChip("ORGANIZER", label: "Organizer")
                        }
                    }

                    if let formError {
                        Text(formError)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#F87171"))
                    }

                    Button {
                        Task { await addParticipant() }
                    } label: {
                        if submitting {
                            ProgressView().tint(.white).frame(maxWidth: .infinity).padding(.vertical, 16)
                        } else {
                            Text("Add participant")
                                .font(.plusJakarta(size: 15, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                        }
                    }
                    .background(
                        LinearGradient(
                            colors: [TripSheetTokens.accent, TripSheetTokens.accentEnd],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .disabled(submitting || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    .opacity(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.55 : 1)

                    if !participants.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("ON THIS TRIP")
                                .font(.plusJakarta(size: 11, weight: .bold))
                                .foregroundStyle(muted)
                            ForEach(participants) { p in
                                HStack {
                                    Text(p.displayName?.isEmpty == false ? p.displayName! : "Participant")
                                        .font(.plusJakarta(size: 14))
                                        .foregroundStyle(.white)
                                        .lineLimit(1)
                                    Spacer()
                                    Text((p.roleCode ?? "PARTICIPANT").capitalized)
                                        .font(.plusJakarta(size: 11))
                                        .foregroundStyle(muted)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .background(fieldBg)
                                .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                        }
                    }
                }
                .padding(24)
            }
            .background(sheetBg)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(Color(hex: "#A855F7"))
                }
            }
        }
        .presentationDetents([.large])
        .task { await bootstrap() }
    }

    @ViewBuilder
    private var inviteLinkBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("INVITE LINK")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(muted)
            if minting {
                HStack(spacing: 10) {
                    ProgressView().tint(Color(hex: "#A855F7"))
                    Text("Minting invite…")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .background(fieldBg)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            } else if let displayPath, let copyText {
                Button {
                    UIPasteboard.general.string = copyText
                    copied = true
                } label: {
                    HStack {
                        Text(displayPath)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                        Spacer()
                        Text(copied ? "Copied" : "Copy")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(Color(hex: "#A855F7"))
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .background(fieldBg)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            } else {
                Text(mintError ?? "Invite unavailable")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }
        }
    }

    private func field(_ label: String, text: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label.uppercased())
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(muted)
            TextField(placeholder, text: text)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(.white)
                .padding(12)
                .background(fieldBg)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    private func roleChip(_ code: String, label: String) -> some View {
        let selected = role == code
        return Text(label)
            .font(.plusJakarta(size: 12, weight: .semibold))
            .foregroundStyle(selected ? .white : muted)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? TripSheetTokens.accent : fieldBg)
            .overlay(Capsule().stroke(selected ? TripSheetTokens.accent : border))
            .clipShape(Capsule())
            .onTapGesture { role = code }
    }

    private func bootstrap() async {
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
            let emailTrimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
            _ = try await APIClient.shared.addGroupParticipant(
                momentId: momentId,
                displayName: trimmed,
                roleCode: role,
                email: emailTrimmed.isEmpty ? nil : emailTrimmed
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
