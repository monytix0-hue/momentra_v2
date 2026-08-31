import SwiftUI

/// Join an existing company via typed code or QR (`momentra://c/{code}`).
struct CompanyJoinCodeSheet: View {
    var onClose: () -> Void
    var onJoined: (CompanySummary) -> Void

    @State private var code = ""
    @State private var error: String?
    @State private var submitting = false
    @State private var showScanner = false
    @StateObject private var createModel = MomentCreateModel()

    private let bg = Color(hex: "#161B26")
    private let field = Color(hex: "#252230")
    private let border = Color(hex: "#322E40")
    private let accent = Color(hex: "#818CF8")

    var body: some View {
        ZStack(alignment: .top) {
            bg.ignoresSafeArea()
            VStack(spacing: 0) {
                Capsule()
                    .fill(Color(hex: "#625E70"))
                    .frame(width: 48, height: 4)
                    .padding(.top, 12)
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("Join with company code")
                            .font(.plusJakarta(size: 20, weight: .heavy))
                            .foregroundStyle(.white)
                        Spacer()
                        Button("Close", action: onClose)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(accent)
                    }
                    Text("Enter an invite code or scan a company QR.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(Color(hex: "#9E9AA8"))

                    HStack(spacing: 10) {
                        TextField("Invite code", text: $code)
                            .font(.plusJakarta(size: 15, weight: .medium))
                            .foregroundStyle(.white)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        Button {
                            showScanner = true
                        } label: {
                            Image("ShellQr")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 18, height: 18)
                                .frame(width: 36, height: 36)
                                .background(Color(hex: "#1C233D"), in: Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Scan company QR")
                    }
                    .padding(14)
                    .background(field)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(border))
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    if let error {
                        Text(error)
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(Color(hex: "#F87171"))
                    }

                    Button(action: submit) {
                        Text(submitting ? "Joining…" : "Join company")
                            .font(.plusJakarta(size: 15, weight: .heavy))
                            .foregroundStyle(Color(hex: "#14121B"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(
                                LinearGradient(
                                    colors: [accent, Color(hex: "#6366F1")],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                ),
                                in: RoundedRectangle(cornerRadius: 14)
                            )
                    }
                    .buttonStyle(.plain)
                    .disabled(submitting || CompanyJoinLink.parseTyped(code) == nil)
                    .opacity(CompanyJoinLink.parseTyped(code) == nil ? 0.45 : 1)
                }
                .padding(20)
                Spacer()
            }
        }
        .fullScreenCover(isPresented: $showScanner) {
            GroupJoinQrScanner(
                onCode: { _ in
                    showScanner = false
                    error = "That looks like a group invite. Use a company QR (momentra://c/…)."
                },
                onCompanyCode: { scanned in
                    showScanner = false
                    code = scanned
                    submit()
                },
                onDismiss: { showScanner = false }
            )
        }
    }

    private func submit() {
        guard let parsed = CompanyJoinLink.parseTyped(code), !submitting else { return }
        submitting = true
        error = nil
        Task {
            guard let result = await createModel.redeemCompanyInvite(code: parsed) else {
                await MainActor.run {
                    submitting = false
                    error = "Could not join. Check the code and try again."
                }
                return
            }
            let companies = await createModel.listCompanies()
            let summary = companies.first(where: { $0.companyId == result.companyId })
                ?? CompanySummary(companyId: result.companyId, displayName: "Company")
            await MainActor.run {
                submitting = false
                onJoined(summary)
            }
        }
    }
}
