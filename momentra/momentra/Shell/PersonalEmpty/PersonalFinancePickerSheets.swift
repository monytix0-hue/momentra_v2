import SwiftUI

/// Figma 417:8863 + inferred category picker sheets.
struct PersonalCategoryPickerSheet: View {
    enum Mode { case category, subcategory }

    let mode: Mode
    let selectedCategoryCode: String
    let selectedSubcategoryCode: String?
    var onSelect: (String, String?) -> Void
    var onClose: () -> Void

    @State private var step: String
    @State private var activeCategory: String

    init(
        mode: Mode,
        selectedCategoryCode: String,
        selectedSubcategoryCode: String?,
        onSelect: @escaping (String, String?) -> Void,
        onClose: @escaping () -> Void
    ) {
        self.mode = mode
        self.selectedCategoryCode = selectedCategoryCode
        self.selectedSubcategoryCode = selectedSubcategoryCode
        self.onSelect = onSelect
        self.onClose = onClose
        _step = State(initialValue: mode == .subcategory ? "sub" : "cat")
        _activeCategory = State(initialValue: selectedCategoryCode)
    }

    var body: some View {
        VStack(spacing: 12) {
            Capsule().fill(Color(hex: "#C9C4D8").opacity(0.3)).frame(width: 48, height: 4)
            HStack {
                Text(step == "sub" ? "Sub-category" : "Category")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
                Spacer()
                if step == "sub" {
                    Button("Back") { step = "cat" }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#7C5CFC"))
                }
            }
            ScrollView {
                VStack(spacing: 8) {
                    if step == "cat" {
                        ForEach(PersonalExpenseCategoryCatalog.masterCategories) { cat in
                            let selected = cat.code == selectedCategoryCode
                            Button {
                                activeCategory = cat.code
                                if mode == .subcategory {
                                    step = "sub"
                                } else {
                                    onSelect(cat.code, cat.subcategories.first?.code)
                                    onClose()
                                }
                            } label: {
                                HStack(spacing: 12) {
                                    Text(cat.emoji)
                                    Text(cat.label)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Color(hex: "#E5E0EE"))
                                    Spacer()
                                }
                                .padding(16)
                                .background(selected ? Color(hex: "#7C5CFC").opacity(0.15) : Color(hex: "#201E28"))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(selected ? Color(hex: "#7C5CFC") : Color.white.opacity(0.08)))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .buttonStyle(.plain)
                        }
                    } else if let cat = PersonalExpenseCategoryCatalog.masterCategories.first(where: { $0.code == activeCategory }) {
                        ForEach(cat.subcategories) { sub in
                            let selected = sub.code == selectedSubcategoryCode
                            Button {
                                onSelect(cat.code, sub.code)
                                onClose()
                            } label: {
                                Text(sub.label)
                                    .font(.system(size: 15))
                                    .foregroundStyle(Color(hex: "#E5E0EE"))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(16)
                                    .background(selected ? Color(hex: "#7C5CFC").opacity(0.15) : Color(hex: "#201E28"))
                                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(selected ? Color(hex: "#7C5CFC") : Color.white.opacity(0.08)))
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
        .padding(20)
        .background(Color(hex: "#14121B"))
    }
}

struct PersonalUploadAttachmentSheet: View {
    var onUpload: (String) -> Void
    var onClose: () -> Void
    var uploadAction: (String) async throws -> String

    @State private var uploading = false
    @State private var error: String?

    var body: some View {
        VStack(spacing: 16) {
            LinearGradient(colors: [Color(hex: "#7C5CFC"), Color(hex: "#FF9E74"), Color(hex: "#10B981")], startPoint: .leading, endPoint: .trailing)
                .frame(height: 4)
            Text("Upload Attachment")
                .font(.system(size: 22, weight: .heavy))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text("Pick a source — upload persists when complete.")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
            if let error {
                Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
            }
            ForEach([
                ("📸", "Take Photo", "image/jpeg"),
                ("🖼️", "Photo Library", "image/jpeg"),
                ("📁", "Browse Files", "application/pdf"),
            ], id: \.1) { emoji, title, contentType in
                Button {
                    guard !uploading else { return }
                    uploading = true
                    error = nil
                    Task {
                        do {
                            let uploadId = try await uploadAction(contentType)
                            await MainActor.run {
                                uploading = false
                                onUpload(uploadId)
                                onClose()
                            }
                        } catch {
                            await MainActor.run {
                                uploading = false
                                self.error = error.localizedDescription
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 16) {
                        Text(emoji).frame(width: 48, height: 48).background(Color(hex: "#3B82F6").opacity(0.2)).clipShape(Circle())
                        VStack(alignment: .leading) {
                            Text(title).font(.system(size: 15, weight: .semibold)).foregroundStyle(Color(hex: "#E5E0EE"))
                            Text(uploading ? "Uploading…" : contentType).font(.system(size: 12)).foregroundStyle(Color(hex: "#C9C4D8"))
                        }
                        Spacer()
                    }
                    .padding(20)
                    .background(Color(hex: "#3B82F6").opacity(0.1))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(hex: "#3B82F6").opacity(0.2)))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                .disabled(uploading)
            }
        }
        .padding(24)
        .background(Color(hex: "#14121B"))
    }
}

// MARK: - Financial account helpers (Figma 353:21778 / 353:21863)

enum PersonalFinancialAccountUi {
    static let typeOptions: [(label: String, api: String)] = [
        ("Bank", "BANK"), ("Cash", "CASH"), ("Credit", "CARD"),
    ]
    static let currencyOptions = ["INR", "USD", "EUR"]

    static func emojiForType(_ accountType: String) -> String {
        switch accountType.uppercased() {
        case "CASH": return "💵"
        case "CARD", "CREDIT", "CREDIT_CARD": return "💳"
        case "WALLET": return "👛"
        default: return "🏦"
        }
    }

    static func emojiForLabel(_ label: String) -> String {
        switch label {
        case "Cash": return "💵"
        case "Credit": return "💳"
        default: return "🏦"
        }
    }

    static func nativeSymbolForType(_ accountType: String) -> String {
        switch accountType.uppercased() {
        case "CASH": return "banknote.fill"
        case "CARD", "CREDIT", "CREDIT_CARD": return "creditcard.fill"
        case "WALLET": return "wallet.pass.fill"
        default: return "building.columns.fill"
        }
    }

    static func nativeSymbolForLabel(_ label: String) -> String {
        switch label {
        case "Cash": return "banknote.fill"
        case "Credit": return "creditcard.fill"
        default: return "building.columns.fill"
        }
    }

    static func apiTypeForLabel(_ label: String) -> String {
        typeOptions.first(where: { $0.label == label })?.api ?? "BANK"
    }

    static func balanceLabel(_ account: APIClient.FinancialAccount) -> String {
        if let inst = account.institutionName?.trimmingCharacters(in: .whitespacesAndNewlines), !inst.isEmpty {
            return inst
        }
        return "\(account.accountType.replacingOccurrences(of: "_", with: " ")) · \(account.currencyCode)"
    }

    static func paymentMethodForAccountType(_ accountType: String) -> String {
        switch accountType.uppercased() {
        case "CARD": return "CARD"
        case "BANK": return "BANK_TRANSFER"
        case "WALLET": return "WALLET"
        default: return "CASH"
        }
    }
}

/// Figma 353:21778 — Add Account sheet.
struct PersonalAddAccountSheet: View {
    var onSaved: (APIClient.FinancialAccount) -> Void
    var onClose: () -> Void

    @State private var accountTypeLabel = "Bank"
    @State private var accountName = ""
    @State private var openingBalance = ""
    @State private var currency = "INR"
    @State private var setDefault = true
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let purple = Color(hex: "#7C5CFC")
    private let green = Color(hex: "#10B981")
    private let subtle = Color(hex: "#ABA3BA")

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 8) {
                    Button(action: onClose) {
                        Text("‹").font(.system(size: 22, weight: .bold)).foregroundStyle(Color(hex: "#E5E0EE"))
                    }
                    .buttonStyle(.plain)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Add Account")
                            .font(.system(size: 19, weight: .bold))
                            .foregroundStyle(Color(hex: "#F5F2FC"))
                        Text("Life Operations")
                            .font(.system(size: 10))
                            .foregroundStyle(subtle)
                    }
                }

                VStack(alignment: .leading, spacing: 16) {
                    accountFieldLabel("ACCOUNT TYPE")
                    accountTypeChips
                    accountFieldLabel("ACCOUNT NAME")
                    HStack(spacing: 8) {
                        Text(PersonalFinancialAccountUi.emojiForLabel(accountTypeLabel))
                        TextField("HDFC Savings", text: $accountName)
                            .font(.system(size: 13))
                            .foregroundStyle(Color(hex: "#F5F2FC"))
                            .accessibilityIdentifier("personal.account.add.name")
                    }
                    .padding(12)
                    .background(Color(hex: "#302E39"))
                    .overlay(RoundedRectangle(cornerRadius: 11).stroke(Color(hex: "#938EA1").opacity(0.2)))
                    .clipShape(RoundedRectangle(cornerRadius: 11))

                    accountFieldLabel("OPENING BALANCE (OPTIONAL)")
                    HStack {
                        if openingBalance.isEmpty {
                            Text("₹").font(.system(size: 26, weight: .bold)).foregroundStyle(green)
                        }
                        TextField("", text: $openingBalance)
                            .keyboardType(.decimalPad)
                            .font(.system(size: 26, weight: .bold))
                            .foregroundStyle(green)
                            .onChange(of: openingBalance) { _, v in
                                openingBalance = v.filter { $0.isNumber || $0 == "." || $0 == "," }
                            }
                    }
                    .padding(12)
                    .background(Color(hex: "#302E39"))
                    .overlay(RoundedRectangle(cornerRadius: 11).stroke(Color(hex: "#938EA1").opacity(0.2)))
                    .clipShape(RoundedRectangle(cornerRadius: 11))
                    Text("Balance tracking coming soon.")
                        .font(.system(size: 10))
                        .foregroundStyle(subtle)

                    accountFieldLabel("CURRENCY")
                    currencyChips

                    Toggle(isOn: $setDefault) {
                        Text("Set as Default Account")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color(hex: "#F5F2FC"))
                    }
                    .tint(green)
                }
                .padding(14)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#575266")))
                .clipShape(RoundedRectangle(cornerRadius: 14))

                Button(action: submit) {
                    Group {
                        if submitting {
                            ProgressView().tint(.white)
                        } else {
                            Text("Add Account")
                                .font(.system(size: 13, weight: .bold))
                        }
                    }
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(LinearGradient(colors: [purple, Color(hex: "#A78BFA")], startPoint: .leading, endPoint: .trailing))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .disabled(accountName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || submitting)
                .opacity(accountName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.5 : 1)
                .accessibilityIdentifier("personal.account.add.submit")

                HStack(alignment: .top, spacing: 6) {
                    Text("ℹ️").font(.system(size: 10))
                    Text("The account will become available immediately in Quick Add and Master Expense.")
                        .font(.system(size: 10))
                        .foregroundStyle(subtle)
                }

                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
        }
        .background(Color(hex: "#14121B"))
    }

    private func accountFieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 9))
            .tracking(0.8)
            .foregroundStyle(subtle)
    }

    private var accountTypeChips: some View {
        FlowLayout(spacing: 7) {
            ForEach(PersonalFinancialAccountUi.typeOptions, id: \.label) { opt in
                let selected = accountTypeLabel == opt.label
                let color: Color = {
                    switch opt.label {
                    case "Cash": return green
                    case "Credit": return Color(hex: "#F59E0B")
                    default: return Color(hex: "#3B82F6")
                    }
                }()
                let testId: String = {
                    switch opt.label {
                    case "Bank": return "personal.account.add.type.bank"
                    case "Cash": return "personal.account.add.type.cash"
                    default: return "personal.account.add.type.credit"
                    }
                }()
                Text(opt.label)
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(selected ? color : color.opacity(0.35))
                    .clipShape(Capsule())
                    .onTapGesture { accountTypeLabel = opt.label }
                    .accessibilityIdentifier(testId)
            }
        }
    }

    private var currencyChips: some View {
        FlowLayout(spacing: 7) {
            ForEach(PersonalFinancialAccountUi.currencyOptions, id: \.self) { code in
                let selected = currency == code
                let color: Color = {
                    switch code {
                    case "USD": return Color(hex: "#3B82F6")
                    case "EUR": return green
                    default: return purple
                    }
                }()
                Text(code)
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(selected ? color : color.opacity(0.35))
                    .clipShape(Capsule())
                    .onTapGesture { currency = code }
            }
        }
    }

    private func submit() {
        guard !submitting else { return }
        submitting = true
        error = nil
        Task {
            do {
                let account = try await APIClient.shared.createFinancialAccount(
                    accountType: PersonalFinancialAccountUi.apiTypeForLabel(accountTypeLabel),
                    accountName: accountName.trimmingCharacters(in: .whitespacesAndNewlines),
                    currencyCode: currency,
                    institutionName: nil,
                    idempotencyKey: draftKey
                )
                await MainActor.run {
                    submitting = false
                    onSaved(account)
                    onClose()
                }
            } catch {
                await MainActor.run {
                    submitting = false
                    self.error = error.localizedDescription
                }
            }
        }
    }
}

/// Figma 353:21863 — Account picker (empty + populated).
struct PersonalAccountPickerSheet: View {
    let selectedAccountId: String?
    var excludeAccountId: String? = nil
    var onSelect: (APIClient.FinancialAccount) -> Void
    var onClose: () -> Void

    @State private var accounts: [APIClient.FinancialAccount] = []
    @State private var loading = true
    @State private var showAddAccount = false
    @State private var error: String?

    private let purple = Color(hex: "#7C5CFC")
    private let subtle = Color(hex: "#ABA3BA")

    var body: some View {
        Group {
            if showAddAccount {
                PersonalAddAccountSheet(
                    onSaved: { created in
                        accounts.append(created)
                        onSelect(created)
                        showAddAccount = false
                        onClose()
                    },
                    onClose: { showAddAccount = false }
                )
            } else {
                pickerContent
            }
        }
        .accessibilityIdentifier("personal.account.picker")
        .task { await reload() }
    }

    private var filtered: [APIClient.FinancialAccount] {
        accounts.filter { $0.financialAccountId != excludeAccountId }
    }

    @ViewBuilder
    private var pickerContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Button(action: onClose) {
                    Text("‹").font(.system(size: 22, weight: .bold)).foregroundStyle(Color(hex: "#E5E0EE"))
                }
                .buttonStyle(.plain)
                Text("Select Account")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundStyle(Color(hex: "#F5F2FC"))
                Spacer()
                Button(action: onClose) {
                    Text("Close")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(purple)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("personal.account.picker.close")
            }

            if loading {
                ProgressView().frame(maxWidth: .infinity).padding(.vertical, 32)
            } else if filtered.isEmpty {
                emptyState
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(filtered) { account in
                            accountRow(account)
                        }
                        createAccountButton
                    }
                }
            }

            if let error {
                Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(Color(hex: "#14121B"))
    }

    private var emptyState: some View {
        VStack(spacing: 24) {
            Text("🏦")
                .font(.system(size: 48))
                .frame(width: 120, height: 120)
                .background(LinearGradient(colors: [purple, Color(hex: "#A78BFA")], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(Circle())
            VStack(spacing: 8) {
                Text("No accounts available")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(Color(hex: "#F5F2FC"))
                Text("You'll return here automatically after the account is created.")
                    .font(.system(size: 14))
                    .foregroundStyle(subtle)
                    .multilineTextAlignment(.center)
            }
            createAccountButton
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    private var createAccountButton: some View {
        Button { showAddAccount = true } label: {
            HStack(spacing: 8) {
                Text("➕")
                Text("Create Account")
                    .font(.system(size: 14, weight: .bold))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(LinearGradient(colors: [purple, Color(hex: "#A78BFA")], startPoint: .leading, endPoint: .trailing))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("personal.account.picker.create")
    }

    private func accountRow(_ account: APIClient.FinancialAccount) -> some View {
        let selected = account.financialAccountId == selectedAccountId
        return Button {
            onSelect(account)
            onClose()
        } label: {
            HStack {
                HStack(spacing: 12) {
                    Text(PersonalFinancialAccountUi.emojiForType(account.accountType))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(account.accountName)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color(hex: "#E5E0EE"))
                        Text(PersonalFinancialAccountUi.balanceLabel(account))
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: "#C9C4D8"))
                    }
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 20, height: 20)
                        .background(purple)
                        .clipShape(Circle())
                }
            }
            .padding(14)
            .background(selected ? Color(hex: "#201E28") : Color(hex: "#14121B"))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(selected ? purple : Color(hex: "#938EA1"), lineWidth: selected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    @MainActor
    private func reload() async {
        loading = true
        do {
            accounts = try await APIClient.shared.listFinancialAccounts()
            error = nil
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

/// Chevron row for account selection in money/expense/edit surfaces.
struct PersonalAccountSelectRow: View {
    let label: String
    let account: APIClient.FinancialAccount?
    var placeholder: String = "Select account"
    var onClick: () -> Void
    var testTag: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .tracking(0.8)
                .foregroundStyle(Color(hex: "#ABA3BA"))
            Button(action: onClick) {
                HStack {
                    if let account {
                        Text(PersonalFinancialAccountUi.emojiForType(account.accountType))
                    }
                    Text(account?.accountName ?? placeholder)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Spacer()
                    Text("›").foregroundStyle(Color(hex: "#C9C4D8"))
                }
                .padding(14)
                .background(Color(hex: "#201E28"))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#938EA1")))
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .optionalAccessibilityIdentifier(testTag)
        }
    }
}

private extension View {
    @ViewBuilder
    func optionalAccessibilityIdentifier(_ id: String?) -> some View {
        if let id {
            accessibilityIdentifier(id)
        } else {
            self
        }
    }
}
