import SwiftUI
import UIKit

/// Log a company expense against the active Business Moment.
struct BusinessExpenseSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currencyCode = "INR"
    @State private var descriptionText = ""
    @State private var categoryCode = "PURCHASE"
    @State private var submitting = false
    @State private var error: String?
    @State private var pendingApprovalId: String?

    private let accent = Color(red: 0.506, green: 0.549, blue: 0.973)
    private let categories = ["PURCHASE", "OPS", "SOFTWARE", "TRAVEL", "OTHER"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    header
                    formCard
                    if let pendingApprovalId {
                        approvalCard(pendingApprovalId)
                    }
                    if let error {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(Color(hex: "#F87171"))
                    }
                    saveButton
                }
                .padding(16)
            }
            .background(Color(hex: "#14121B"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accent)
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        HStack(spacing: 10) {
            Image(systemName: "creditcard")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(accent)
                .frame(width: 40, height: 40)
                .background(Color.white.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: 20))
            VStack(alignment: .leading, spacing: 2) {
                Text("Log business expense")
                    .font(.system(size: 18, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Text("Posted to company finance for this Moment.")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            }
        }
    }

    private var formCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            fieldLabel("AMOUNT")
            HStack(spacing: 10) {
                TextField("INR", text: $currencyCode)
                    .textInputAutocapitalization(.characters)
                    .frame(width: 56)
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                TextField("0.00", text: $amount)
                    .keyboardType(.decimalPad)
                    .font(.system(size: 26, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
            }
            .padding(12)
            .background(Color(hex: "#201E28"))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            fieldLabel("DESCRIPTION")
            TextField("AWS, ads, supplies…", text: $descriptionText)
                .foregroundStyle(Color(hex: "#E5E0EE"))
                .padding(12)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "#938EA1"), lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            fieldLabel("CATEGORY")
            FlowLayout(spacing: 8) {
                ForEach(categories, id: \.self) { code in
                    let on = categoryCode == code
                    Button { categoryCode = code } label: {
                        Text(code.capitalized)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(on ? .white : Color(hex: "#C9C4D8"))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(on ? accent : Color(hex: "#201E28"))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func approvalCard(_ id: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Approval required")
                .font(.system(size: 14, weight: .heavy))
                .foregroundStyle(Color(hex: "#FBBF24"))
            Text("This expense is DRAFT pending approval.")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
            HStack(spacing: 10) {
                Button {
                    Task { await decide(id, decision: "APPROVE") }
                } label: {
                    Text("Approve")
                        .font(.system(size: 13, weight: .heavy))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(hex: "#14B8A6"))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                Button {
                    Task { await decide(id, decision: "REJECT") }
                } label: {
                    Text("Reject")
                        .font(.system(size: 13, weight: .heavy))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(hex: "#F87171"))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .background(Color(hex: "#201E28"))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var saveButton: some View {
        Button {
            Task { await save() }
        } label: {
            HStack(spacing: 8) {
                if submitting {
                    ProgressView().tint(.white)
                } else {
                    Text("Save Expense")
                        .font(.system(size: 15, weight: .heavy))
                    Image(systemName: "checkmark")
                        .font(.system(size: 13, weight: .bold))
                }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(accent)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!canSubmit || submitting)
        .opacity(!canSubmit ? 0.55 : 1)
    }

    private var canSubmit: Bool {
        !amount.isEmpty && currencyCode.count == 3 && pendingApprovalId == nil
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .bold))
            .tracking(0.8)
            .foregroundStyle(accent)
    }

    private func save() async {
        submitting = true
        error = nil
        do {
            let result = try await APIClient.shared.createBusinessExpense(
                momentId: momentId,
                amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                currencyCode: currencyCode.uppercased(),
                description: descriptionText.isEmpty ? nil : descriptionText,
                categoryCode: categoryCode
            )
            if let approvalId = result.approvalRequestId, result.status.uppercased() == "DRAFT" {
                pendingApprovalId = approvalId
            } else {
                isPresented = false
                onSaved()
            }
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }

    private func decide(_ approvalId: String, decision: String) async {
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.decideApproval(
                approvalRequestId: approvalId,
                decision: decision
            )
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

/// Record company revenue.
struct BusinessRevenueSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var amount = ""
    @State private var currencyCode = "INR"
    @State private var descriptionText = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = Color(hex: "#14B8A6")

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 14) {
                Text("Log revenue")
                    .font(.system(size: 18, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                HStack {
                    TextField("INR", text: $currencyCode)
                        .textInputAutocapitalization(.characters)
                        .frame(width: 56)
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    TextField("0.00", text: $amount)
                        .keyboardType(.decimalPad)
                        .font(.system(size: 26, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                }
                .padding(12)
                .background(Color(hex: "#201E28"))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                TextField("Description (optional)", text: $descriptionText)
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                    .padding(12)
                    .background(Color(hex: "#201E28"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                if let error {
                    Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    if submitting {
                        ProgressView().tint(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    } else {
                        Text("Save Revenue")
                            .font(.system(size: 15, weight: .heavy))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                }
                .background(accent)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .disabled(amount.isEmpty || submitting || currencyCode.count != 3)
                .opacity(amount.isEmpty ? 0.55 : 1)
                Spacer()
            }
            .padding(16)
            .background(Color(hex: "#14121B"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accent)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func save() async {
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createBusinessRevenue(
                momentId: momentId,
                amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                currencyCode: currencyCode.uppercased(),
                description: descriptionText.isEmpty ? nil : descriptionText
            )
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

/// Track a simple single-line invoice.
struct BusinessInvoiceSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var invoiceNumber = ""
    @State private var currencyCode = "INR"
    @State private var lineDescription = ""
    @State private var quantity = "1"
    @State private var unitPrice = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = Color(hex: "#3B82F6")

    private var today: String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Track invoice")
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    TextField("Invoice #", text: $invoiceNumber)
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                        .padding(12)
                        .background(Color(hex: "#201E28"))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    TextField("Line description", text: $lineDescription)
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                        .padding(12)
                        .background(Color(hex: "#201E28"))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    HStack {
                        TextField("Qty", text: $quantity)
                            .keyboardType(.decimalPad)
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        TextField(currencyCode, text: $currencyCode)
                            .textInputAutocapitalization(.characters)
                            .frame(width: 56)
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                        TextField("Unit price", text: $unitPrice)
                            .keyboardType(.decimalPad)
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                    }
                    .padding(12)
                    .background(Color(hex: "#201E28"))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    Text("Tax is server-authoritative (omit or 0).")
                        .font(.system(size: 11))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }
                    Button {
                        Task { await save() }
                    } label: {
                        if submitting {
                            ProgressView().tint(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        } else {
                            Text("Save Invoice")
                                .font(.system(size: 15, weight: .heavy))
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                        }
                    }
                    .background(accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .disabled(!canSubmit || submitting)
                    .opacity(!canSubmit ? 0.55 : 1)
                }
                .padding(16)
            }
            .background(Color(hex: "#14121B"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(accent)
                }
            }
        }
        .presentationDetents([.large])
    }

    private var canSubmit: Bool {
        !invoiceNumber.isEmpty
            && !lineDescription.isEmpty
            && !quantity.isEmpty
            && !unitPrice.isEmpty
            && currencyCode.count == 3
    }

    private func save() async {
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createBusinessInvoice(
                momentId: momentId,
                invoiceNumber: invoiceNumber.trimmingCharacters(in: .whitespacesAndNewlines),
                invoiceDate: today,
                currencyCode: currencyCode.uppercased(),
                lines: [
                    APIClient.BusinessInvoiceLineInput(
                        description: lineDescription,
                        quantity: quantity.trimmingCharacters(in: .whitespacesAndNewlines),
                        unitPrice: unitPrice.trimmingCharacters(in: .whitespacesAndNewlines),
                        taxAmount: nil
                    ),
                ]
            )
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

/// Company members list + invite mint.
struct BusinessMembersSheet: View {
    let companyId: String
    @Binding var isPresented: Bool

    @State private var members: [APIClient.CompanyMemberPayload] = []
    @State private var loading = true
    @State private var error: String?
    @State private var showInvite = false
    @State private var invitePath: String?
    @State private var inviteCode: String?
    @State private var inviteError: String?
    @State private var minting = false

    private let accent = Color(red: 0.506, green: 0.549, blue: 0.973)

    var body: some View {
        NavigationStack {
            Group {
                if loading {
                    ProgressView().tint(accent)
                } else if let error {
                    Text(error).foregroundStyle(Color(hex: "#F87171")).padding()
                } else if members.isEmpty {
                    Text("No members on this company yet.")
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                        .padding()
                } else {
                    List(members) { m in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(m.displayName ?? String(m.userId.prefix(8)))
                                .font(.headline)
                            Text("\(m.membershipType) · \(m.status)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color(hex: "#14121B"))
            .navigationTitle("People")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button("Invite") { mintInvite() }
                        .disabled(minting)
                }
            }
            .sheet(isPresented: $showInvite) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Invite to company")
                        .font(.plusJakarta(size: 20, weight: .heavy))
                    if let inviteError {
                        Text(inviteError).foregroundStyle(Color(hex: "#F87171"))
                    } else {
                        if let code = inviteCode,
                           let qr = GroupQRCode.image(from: CompanyJoinLink.qrPayload(code: code), size: 160) {
                            Image(uiImage: qr)
                                .interpolation(.none)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 144, height: 144)
                                .padding(8)
                                .background(Color.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .frame(maxWidth: .infinity)
                        }
                        Text(invitePath ?? "")
                            .font(.plusJakarta(size: 14, weight: .semibold))
                            .foregroundStyle(accent)
                        Text("Code: \(inviteCode ?? "")")
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(Color(hex: "#9E9AA8"))
                        Text("Share this link or QR. New members join as MEMBER.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#64748B"))
                    }
                    Spacer()
                    Button("Done") { showInvite = false }
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(accent)
                }
                .padding(20)
                .presentationDetents([.medium])
            }
        }
        .presentationDetents([.medium, .large])
        .task {
            loading = true
            do {
                members = try await APIClient.shared.listCompanyMembers(companyId: companyId)
            } catch {
                self.error = error.localizedDescription
            }
            loading = false
        }
    }

    private func mintInvite() {
        guard !minting else { return }
        minting = true
        inviteError = nil
        Task {
            do {
                let invite = try await APIClient.shared.mintCompanyInvite(
                    companyId: companyId,
                    membershipType: "MEMBER",
                    idempotencyKey: UUID().uuidString
                )
                await MainActor.run {
                    inviteCode = invite.inviteCode
                    invitePath = invite.invitePath.isEmpty
                        ? CompanyJoinLink.displayPath(code: invite.inviteCode)
                        : invite.invitePath
                    minting = false
                    showInvite = true
                }
            } catch {
                await MainActor.run {
                    minting = false
                    inviteError = error.localizedDescription
                    showInvite = true
                }
            }
        }
    }
}
