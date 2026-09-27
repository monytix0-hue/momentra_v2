import SwiftUI

struct PendingCompanyJoin: Identifiable {
    let id: String
    var code: String { id }
}

private enum CompanyJoinPhase {
    case preview
    case joining
    case welcome
}

/// Preview a company invite, then joining, then welcome.
struct CompanyJoinConfirmSheet: View {
    let code: String
    var onClose: () -> Void
    var onGoToCompany: (CompanySummary) -> Void

    @StateObject private var createModel = MomentCreateModel()
    @State private var preview: CompanyInvite?
    @State private var phase: CompanyJoinPhase = .preview
    @State private var loading = true
    @State private var error: String?
    @State private var joined: CompanySummary?
    @State private var roleLabel: String?

    private let bg = Color(hex: "#161B26")
    private let accent = Color(hex: "#818CF8")

    var body: some View {
        VStack(spacing: 16) {
            Capsule()
                .fill(Color(hex: "#625E70"))
                .frame(width: 36, height: 4)
                .padding(.top, 12)
            switch phase {
            case .preview:
                previewBody
            case .joining:
                ProgressView().tint(accent)
                Text(preview?.title.isEmpty == false ? "Joining \(preview?.title ?? "")..." : "Joining…")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
            case .welcome:
                welcomeBody
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(bg.ignoresSafeArea())
        .task(id: code) { await loadPreview() }
        .task(id: phase) { await redeemIfNeeded() }
    }

    @ViewBuilder
    private var previewBody: some View {
        if loading {
            ProgressView().tint(accent)
        } else if let preview {
            HStack(spacing: 12) {
                initialsBubble(preview.title)
                Text(preview.title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
            }
            if !preview.membershipType.isEmpty {
                Text("You join as \(prettyRole(preview.membershipType))")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(accent.opacity(0.15), in: Capsule())
            }
        }
        if let error {
            Text(error)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color(hex: "#F87171"))
        }
        Button {
            guard preview != nil else { return }
            phase = .joining
            error = nil
        } label: {
            Text("Join Company")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color(hex: "#F1F5F9"))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(accent.opacity(preview == nil ? 0.35 : 0.9), in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(preview == nil)
        Button("Decline", action: onClose)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(Color(hex: "#94A3B8"))
        Text("You can leave anytime from company settings")
            .font(.system(size: 12))
            .foregroundStyle(Color(hex: "#94A3B8"))
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var welcomeBody: some View {
        let name = joined?.displayName ?? ""
        Text(name.isEmpty ? "Welcome" : "Welcome to \(name)!")
            .font(.system(size: 22, weight: .bold))
            .foregroundStyle(.white)
            .multilineTextAlignment(.center)
        if let roleLabel, !roleLabel.isEmpty {
            Text("You've successfully joined as a \(prettyRole(roleLabel))")
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: "#94A3B8"))
                .multilineTextAlignment(.center)
        }
        if !name.isEmpty {
            HStack(spacing: 12) {
                initialsBubble(name)
                Text(name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                Spacer()
            }
            .padding(12)
            .background(Color(hex: "#252230"), in: RoundedRectangle(cornerRadius: 12))
        }
        Button {
            if let joined { onGoToCompany(joined) }
        } label: {
            Text("Go to Company")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Color(hex: "#F1F5F9"))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(accent.opacity(0.9), in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        Button("Stay Here", action: onClose)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(Color(hex: "#94A3B8"))
    }

    private func initialsBubble(_ name: String) -> some View {
        Text(companyInitials(name))
            .font(.system(size: 13, weight: .bold))
            .foregroundStyle(Color(hex: "#818CF8"))
            .frame(width: 40, height: 40)
            .background(Color(hex: "#818CF8").opacity(0.2), in: Circle())
    }

    private func prettyRole(_ raw: String) -> String {
        let lower = raw.lowercased()
        return lower.prefix(1).uppercased() + lower.dropFirst()
    }

    private func loadPreview() async {
        loading = true
        error = nil
        preview = await createModel.previewCompanyInvite(code: code)
        if preview == nil {
            error = "Invite not found or no longer valid."
        }
        loading = false
    }

    private func redeemIfNeeded() async {
        guard phase == .joining, let preview else { return }
        if let result = await createModel.redeemCompanyInvite(code: code) {
            roleLabel = result.membershipType ?? preview.membershipType
            let title = preview.title.isEmpty ? "Company" : preview.title
            joined = CompanySummary(
                companyId: result.companyId.isEmpty ? preview.companyId : result.companyId,
                displayName: title
            )
            phase = .welcome
        } else {
            error = "Could not join company"
            phase = .preview
        }
    }
}
