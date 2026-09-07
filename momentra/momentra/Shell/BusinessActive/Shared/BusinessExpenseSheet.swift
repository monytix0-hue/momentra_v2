import SwiftUI
import PhotosUI
import UIKit

/// Figma `1620:12158` — Add Expense bottom sheet (Team Ops / business).
struct BusinessExpenseSheet: View {
    let momentId: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var amountDisplay = ""
    @State private var currencyCode = "INR"
    @State private var preferredCurrencyCodes: [String] = ["INR"]
    @State private var descriptionText = ""
    @State private var categoryLabel = "Software"
    @State private var paidBy = "You"
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var receiptItem: PhotosPickerItem?
    @State private var receiptBytes: Data?
    @State private var receiptName: String?
    @State private var receiptContentType = "image/jpeg"
    @State private var submitting = false
    @State private var error: String?
    @State private var pendingApprovalId: String?

    private let accent = TeamOpsSheetAccent.indigo
    private let categoryLabels = ["Software", "Travel", "Office", "Equipment", "Services", "Other"]
    private let paidByOptions = ["You"]

    private func categoryCode(_ label: String) -> String { label.uppercased() }

    var body: some View {
        NativeSheetScaffold(
            title: "Add Expense",
            onClose: { isPresented = false },
            background: TeamOpsSheetTokens.sheetBg
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    TeamOpsSheetHeader(
                        emoji: "💳",
                        title: "Add Expense",
                        explanation: "Record team spend against this moment",
                        accent: accent,
                        onClose: { isPresented = false }
                    )

                    TeamOpsFieldLabel(text: "Amount")
                    HStack(alignment: .bottom, spacing: 8) {
                        TravelCurrencyPicker(
                            selectedCode: $currencyCode,
                            preferredCodes: preferredCurrencyCodes,
                            accentColor: accent.accent
                        )
                        TextField("0.00", text: Binding(
                            get: { amountDisplay },
                            set: { raw in
                                let stripped = TeamOpsAmountFormat.strip(raw)
                                amountDisplay = TeamOpsAmountFormat.display(from: stripped)
                            }
                        ))
                        .keyboardType(.decimalPad)
                        .font(.plusJakarta(size: 36, weight: .heavy))
                        .foregroundStyle(TeamOpsSheetTokens.text)
                    }

                    TeamOpsFieldLabel(text: "Description")
                    TeamOpsTextField(value: $descriptionText, placeholder: "AWS, ads, supplies…", minHeight: 44)

                    TeamOpsFieldLabel(text: "Category")
                    TeamOpsChipRow(options: categoryLabels, selected: $categoryLabel, accent: accent)

                    HStack(spacing: 14) {
                        VStack(alignment: .leading, spacing: 8) {
                            TeamOpsFieldLabel(text: "Paid By")
                            TeamOpsDropdownField(
                                value: paidBy,
                                options: paidByOptions,
                                onSelect: { paidBy = $0 },
                                placeholder: "You"
                            )
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        VStack(alignment: .leading, spacing: 8) {
                            TeamOpsFieldLabel(text: "Date")
                            TeamOpsDateField(isoDate: $isoDate)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }

                    TeamOpsFieldLabel(text: "Receipt")
                    PhotosPicker(selection: $receiptItem, matching: .images) {
                        HStack {
                            Text(receiptName ?? "Attach PDF/Img")
                                .font(.plusJakarta(size: 14, weight: .medium))
                                .foregroundStyle(receiptName != nil ? TeamOpsSheetTokens.text : TeamOpsSheetTokens.muted)
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .frame(height: 44)
                        .background(TeamOpsSheetTokens.field)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(TeamOpsSheetTokens.border))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    .onChange(of: receiptItem) { _, item in
                        Task { await loadReceipt(item) }
                    }

                    if let pendingApprovalId {
                        approvalCard(pendingApprovalId)
                    }
                    if let error {
                        Text(error)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(TeamOpsSheetTokens.error)
                    }

                    TeamOpsPrimaryCta(
                        label: submitting ? "Saving…" : "Add Expense",
                        enabled: canSubmit && !submitting && pendingApprovalId == nil,
                        loading: submitting,
                        footerHint: "Team will be notified",
                        accent: accent
                    ) {
                        Task { await save() }
                    }
                }
                .padding(16)
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .task {
            let ctx = await MomentCurrencyContextLoader.loadBusiness(momentId: momentId)
            preferredCurrencyCodes = ctx.preferred
            currencyCode = ctx.primary
        }
    }

    private var canSubmit: Bool {
        let amt = TeamOpsAmountFormat.strip(amountDisplay)
        guard let value = Double(amt), value > 0 else { return false }
        return currencyCode.count == 3
    }

    private func approvalCard(_ id: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Approval required")
                .font(.plusJakarta(size: 14, weight: .heavy))
                .foregroundStyle(Color(hex: "#FBBF24"))
            Text("This expense is DRAFT pending approval — burn updates after approve.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(TeamOpsSheetTokens.muted)
            HStack(spacing: 10) {
                Button {
                    Task { await decide(id, decision: "APPROVE") }
                } label: {
                    Text("Approve")
                        .font(.plusJakarta(size: 13, weight: .heavy))
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
                        .font(.plusJakarta(size: 13, weight: .heavy))
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
        .background(TeamOpsSheetTokens.field)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func loadReceipt(_ item: PhotosPickerItem?) async {
        guard let item else {
            receiptBytes = nil
            receiptName = nil
            return
        }
        do {
            if let data = try await item.loadTransferable(type: Data.self) {
                receiptBytes = data
                receiptName = "Receipt.jpg"
                receiptContentType = "image/jpeg"
            }
        } catch {
            self.error = "Could not load receipt"
        }
    }

    private func save() async {
        let amt = TeamOpsAmountFormat.strip(amountDisplay).trimmingCharacters(in: .whitespacesAndNewlines)
        guard !amt.isEmpty, amt != "0", amt != "0.00" else {
            error = "Enter an amount"
            return
        }
        submitting = true
        error = nil
        do {
            let result = try await APIClient.shared.createBusinessExpense(
                momentId: momentId,
                amount: amt,
                currencyCode: currencyCode.uppercased(),
                description: descriptionText.isEmpty ? nil : descriptionText,
                categoryCode: categoryCode(categoryLabel),
                paidBy: paidBy.isEmpty ? nil : paidBy,
                effectiveAt: "\(isoDate)T12:00:00.000Z"
            )
            if let bytes = receiptBytes, !bytes.isEmpty {
                _ = try? await APIClient.shared.uploadAndAttachExpenseMedia(
                    momentId: momentId,
                    expenseId: result.expenseId,
                    bytes: bytes,
                    contentType: receiptContentType
                )
            }
            onSaved()
            if let approvalId = result.approvalRequestId, result.isDraft {
                pendingApprovalId = approvalId
                error = "Pending approval — burn updates after approve"
            } else if result.isDraft {
                error = "Pending approval — burn updates after approve"
            } else {
                isPresented = false
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
    @State private var preferredCurrencyCodes: [String] = ["INR"]
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
                TravelCurrencyPicker(
                    selectedCode: $currencyCode,
                    preferredCodes: preferredCurrencyCodes,
                    accentColor: accent
                )
                TextField("0.00", text: $amount)
                    .keyboardType(.decimalPad)
                    .font(.system(size: 26, weight: .heavy))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
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
                        Text("Share this link via Messages or WhatsApp. New members join as MEMBER.")
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(Color(hex: "#64748B"))
                        InviteSendChannelButtons(
                            enabled: invitePath != nil,
                            accent: accent,
                            onMessages: {
                                if let path = invitePath {
                                    InviteOutboundShare.sendSms(
                                        phone: nil,
                                        message: InviteOutboundShare.inviteMessage(title: "company", url: path)
                                    )
                                }
                            },
                            onWhatsApp: {
                                if let path = invitePath {
                                    InviteOutboundShare.sendWhatsApp(
                                        phone: nil,
                                        message: InviteOutboundShare.inviteMessage(title: "company", url: path)
                                    )
                                }
                            }
                        )
                        HStack(spacing: 16) {
                            Button("Copy link") {
                                UIPasteboard.general.string = invitePath
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(accent)
                            Button("Share…") {
                                if let path = invitePath {
                                    InviteOutboundShare.presentSystemShare(items: [path])
                                }
                            }
                            .buttonStyle(.plain)
                            .foregroundStyle(accent)
                        }
                        .font(.plusJakarta(size: 14, weight: .bold))
                    }
                    Spacer()
                    Button("Done") { showInvite = false }
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(accent)
                }
                .padding(20)
                .presentationDetents([.medium, .large])
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
