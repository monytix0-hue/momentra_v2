import SwiftUI

enum MoneyQuickAddKind: String, Identifiable {
    case masterExpense
    case income
    case transfer
    case savings

    var id: String { rawValue }
}

private struct SavingsGoal: Identifiable {
    let id: String
    let emoji: String
    let name: String
    let targetLabel: String
}

private let savingsGoals: [SavingsGoal] = [
    SavingsGoal(id: "house", emoji: "🏠", name: "House Fund", targetLabel: "₹5,00,000 target"),
    SavingsGoal(id: "travel", emoji: "✈️", name: "Travel Fund", targetLabel: "₹1,50,000 target"),
    SavingsGoal(id: "education", emoji: "🎓", name: "Education", targetLabel: "₹2,00,000 target"),
]

private enum MoneySheetTokens {
    static let bg = Color(hex: "#14121B")
    static let surface = Color(hex: "#201E28")
    static let text = Color(hex: "#E5E0EE")
    static let secondary = Color(hex: "#C9C4D8")
    static let brand = Color(hex: "#C9BFFF")
    static let green = Color(hex: "#10B981")
    static let blue = Color(hex: "#3B82F6")
    static let teal = Color(hex: "#14B8A6")
    static let border = Color(hex: "#938EA1")
    static let cardBorder = Color.white.opacity(0.08)

    static func accent(for kind: MoneyQuickAddKind) -> Color {
        switch kind {
        case .income: return green
        case .transfer: return blue
        case .savings: return teal
        case .masterExpense: return green
        }
    }
}

private enum AccountPickerTarget { case from, to }

/// Figma money quick-add sheets — Income `482:18697`, Transfer `520:29924`, Savings `520:30019`.
struct PersonalMoneyQuickAddSheet: View {
    let kind: MoneyQuickAddKind
    let momentId: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var activeKind: MoneyQuickAddKind
    @State private var showAccountPicker = false
    @State private var accountPickerTarget: AccountPickerTarget = .from

    @State private var accounts: [APIClient.FinancialAccount] = []
    @State private var fromAccountId: String?
    @State private var toAccountId: String?
    @State private var amount = ""
    @State private var note = ""
    @State private var title = ""
    @State private var category = PersonalExpenseCategoryCatalog.masterCategories.first!.code
    @State private var subcategory = PersonalExpenseCategoryCatalog.masterCategories.first!.subcategories.first!.code
    @State private var financialImpact = "Essential"
    @State private var transferType = "One-time"
    @State private var frequency = "One-time"
    @State private var selectedGoalId = savingsGoals.first!.id
    @State private var whenCode = "Now"
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let incomeRepository = IncomeCreateRepository()
    private let movementRepository = MovementCreateRepository()

    init(
        kind: MoneyQuickAddKind,
        momentId: String,
        onClose: @escaping () -> Void,
        onSaved: @escaping () -> Void
    ) {
        self.kind = kind
        self.momentId = momentId
        self.onClose = onClose
        self.onSaved = onSaved
        _activeKind = State(initialValue: kind)
    }

    private var selectedCategory: PersonalExpenseCategoryCatalog.Category {
        PersonalExpenseCategoryCatalog.masterCategories.first { $0.code == category }
            ?? PersonalExpenseCategoryCatalog.masterCategories.first!
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ambientHeader
                mainContent
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
            }
        }
        .background(MoneySheetTokens.bg)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .task(id: momentId) {
            await loadAccounts()
        }
        .sheet(isPresented: $showAccountPicker) {
            PersonalAccountPickerSheet(
                selectedAccountId: accountPickerTarget == .from ? fromAccountId : toAccountId,
                excludeAccountId: accountPickerTarget == .to ? fromAccountId : nil,
                onSelect: { account in
                    if !accounts.contains(where: { $0.financialAccountId == account.financialAccountId }) {
                        accounts.append(account)
                    }
                    switch accountPickerTarget {
                    case .from:
                        fromAccountId = account.financialAccountId
                        if toAccountId == account.financialAccountId {
                            toAccountId = accounts.first(where: { $0.financialAccountId != fromAccountId })?.financialAccountId
                        }
                    case .to:
                        toAccountId = account.financialAccountId
                    }
                },
                onClose: { showAccountPicker = false }
            )
            .presentationDetents([.large])
        }
    }

    @MainActor
    private func loadAccounts() async {
        do {
            let list = try await APIClient.shared.listFinancialAccounts()
            accounts = list
            fromAccountId = list.first?.financialAccountId
            toAccountId = list.dropFirst().first?.financialAccountId ?? list.first?.financialAccountId
        } catch {
            // Keep empty; user can still save without account selection.
        }
    }

    private var ambientHeader: some View {
        LinearGradient(
            colors: [MoneySheetTokens.accent(for: activeKind).opacity(0.18), .clear],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: 120)
        .frame(maxWidth: .infinity)
    }

    private var mainContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            intelligenceHeader
            moneyTabBar
            Group {
                switch activeKind {
                case .income:
                    incomeTab
                case .transfer:
                    transferTab
                case .savings:
                    savingsTab
                case .masterExpense:
                    EmptyView()
                }
            }
            if let error {
                Text(error)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Color(hex: "#F87171"))
            }
        }
        .padding(.top, -88)
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: activeKind)
    }

    private var intelligenceHeader: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Intelligence OS")
                    .font(.plusJakarta(size: 20, weight: .heavy))
                    .foregroundStyle(MoneySheetTokens.text)
                Text("Record what shapes how your day runs.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(MoneySheetTokens.secondary)
            }
            Spacer(minLength: 8)
            Text("2 Entries Today")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(MoneySheetTokens.secondary)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(MoneySheetTokens.surface)
                .clipShape(Capsule())
        }
    }

    private var moneyTabBar: some View {
        HStack(spacing: 8) {
            tabButton(kind: .income, emoji: "📈", label: "Income")
            tabButton(kind: .transfer, emoji: "💸", label: "Transfer")
            tabButton(kind: .savings, emoji: "🌱", label: "Savings")
        }
    }

    private func tabButton(kind tabKind: MoneyQuickAddKind, emoji: String, label: String) -> some View {
        let selected = activeKind == tabKind
        let accent = MoneySheetTokens.accent(for: tabKind)
        return Button {
            activeKind = tabKind
        } label: {
            HStack(spacing: 4) {
                Text(emoji)
                    .font(.plusJakarta(size: 14))
                Text(label)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(selected ? MoneySheetTokens.text : MoneySheetTokens.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(selected ? accent.opacity(0.2) : MoneySheetTokens.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(selected ? accent : MoneySheetTokens.cardBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private var incomeTab: some View {
        let accent = MoneySheetTokens.green
        return VStack(alignment: .leading, spacing: 12) {
            tabHeading(title: "Income", subtitle: "Track money coming in and financial relief.")
            titleField(value: $title, placeholder: "Salary deposit")
            amountField(value: $amount, accent: accent, testTag: "personal.income.amount")
            PersonalAccountSelectRow(
                label: "PAID FROM",
                account: accounts.first(where: { $0.financialAccountId == fromAccountId }),
                onClick: {
                    accountPickerTarget = .from
                    showAccountPicker = true
                },
                testTag: "personal.income.account"
            )
            categoryChips(accent: accent, testTag: "personal.income.category")
            subcategoryChips(accent: accent, testTag: "personal.income.subcategory")
            fieldLabel("FINANCIAL IMPACT")
            simpleChips(options: ["Essential", "Planned", "Unplanned"], selected: $financialImpact, accent: accent)
            whenChips(selected: $whenCode, accent: accent)
            saveButton(
                label: "Save Income ✓",
                accent: accent,
                enabled: !amount.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                submitting: submitting,
                testTag: "personal.income.submit",
                action: saveIncome
            )
        }
    }

    private var transferTab: some View {
        let accent = MoneySheetTokens.blue
        return VStack(alignment: .leading, spacing: 12) {
            tabHeading(title: "Transfer", subtitle: "Move money between your accounts.")
            PersonalAccountSelectRow(
                label: "FROM",
                account: accounts.first(where: { $0.financialAccountId == fromAccountId }),
                onClick: {
                    accountPickerTarget = .from
                    showAccountPicker = true
                }
            )
            PersonalAccountSelectRow(
                label: "TO",
                account: accounts.first(where: { $0.financialAccountId == toAccountId }),
                onClick: {
                    accountPickerTarget = .to
                    showAccountPicker = true
                }
            )
            amountField(value: $amount, accent: accent, testTag: "personal.money.transfer.amount")
            noteField(value: $note, testTag: "personal.money.transfer.note")
            fieldLabel("TRANSFER TYPE")
            simpleChips(options: ["One-time", "Recurring"], selected: $transferType, accent: accent)
            whenChips(selected: $whenCode, accent: accent)
            saveButton(
                label: "Transfer Now ✓",
                accent: accent,
                enabled: !amount.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                submitting: submitting,
                testTag: "personal.money.transfer.submit",
                action: saveTransfer
            )
        }
    }

    private var savingsTab: some View {
        let accent = MoneySheetTokens.teal
        return VStack(alignment: .leading, spacing: 12) {
            tabHeading(title: "Savings", subtitle: "Move money toward your goals.")
            fieldLabel("SAVINGS GOAL")
            ForEach(savingsGoals) { goal in
                savingsGoalCard(goal: goal, selected: selectedGoalId == goal.id, accent: accent) {
                    selectedGoalId = goal.id
                }
            }
            fieldLabel("DEPOSIT AMOUNT")
            amountField(value: $amount, accent: accent, testTag: "personal.money.savings.amount")
            PersonalAccountSelectRow(
                label: "DEPOSIT FROM",
                account: accounts.first(where: { $0.financialAccountId == fromAccountId }),
                onClick: {
                    accountPickerTarget = .from
                    showAccountPicker = true
                },
                testTag: "personal.money.savings.account"
            )
            fieldLabel("FREQUENCY")
            simpleChips(options: ["One-time", "Weekly", "Monthly"], selected: $frequency, accent: accent)
            whenChips(selected: $whenCode, accent: accent)
            saveButton(
                label: "Save Now ✓",
                accent: accent,
                enabled: !amount.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                submitting: submitting,
                testTag: "personal.money.savings.submit",
                action: saveSavings
            )
        }
    }

    private func saveIncome() {
        guard !submitting else { return }
        submitting = true
        error = nil
        let trimmedAmount = amount.trimmingCharacters(in: .whitespaces)
        let description = buildIncomeDescription()
        Task { @MainActor in
            do {
                _ = try await incomeRepository.createIncome(
                    draftKey: draftKey,
                    momentId: momentId,
                    amount: trimmedAmount,
                    currencyCode: "INR",
                    description: description,
                    merchantName: title.trimmingCharacters(in: .whitespaces).isEmpty ? nil : title.trimmingCharacters(in: .whitespaces),
                    categoryCode: category,
                    financialAccountId: fromAccountId,
                    paymentMethodCode: nil,
                    effectiveAt: nil
                )
                finishSave()
            } catch {
                failSave(error)
            }
        }
    }

    private func buildIncomeDescription() -> String {
        var parts: [String] = []
        let trimmedTitle = title.trimmingCharacters(in: .whitespaces)
        if !trimmedTitle.isEmpty { parts.append(trimmedTitle) }
        let trimmedNote = note.trimmingCharacters(in: .whitespaces)
        if !trimmedNote.isEmpty { parts.append(trimmedNote) }
        parts.append(financialImpact)
        parts.append(PersonalExpenseCategoryCatalog.labelForCode(subcategory))
        parts.append(whenCode)
        return parts.joined(separator: " · ")
    }

    private func saveTransfer() {
        guard !submitting else { return }
        submitting = true
        error = nil
        let trimmedAmount = amount.trimmingCharacters(in: .whitespaces)
        let description = note.trimmingCharacters(in: .whitespaces).isEmpty
            ? "Transfer · \(transferType) · \(whenCode)"
            : note.trimmingCharacters(in: .whitespaces)
        Task { @MainActor in
            do {
                _ = try await movementRepository.createMovement(
                    draftKey: draftKey,
                    momentId: momentId,
                    movementType: "TRANSFER",
                    amount: trimmedAmount,
                    currencyCode: "INR",
                    accountId: fromAccountId,
                    goalId: nil,
                    description: description,
                    effectiveAt: nil
                )
                finishSave()
            } catch {
                failSave(error)
            }
        }
    }

    private func saveSavings() {
        guard !submitting else { return }
        submitting = true
        error = nil
        let trimmedAmount = amount.trimmingCharacters(in: .whitespaces)
        let goalName = savingsGoals.first { $0.id == selectedGoalId }?.name ?? "Savings"
        let description = note.trimmingCharacters(in: .whitespaces).isEmpty
            ? "\(goalName) · \(frequency) · \(whenCode)"
            : note.trimmingCharacters(in: .whitespaces)
        Task { @MainActor in
            do {
                _ = try await movementRepository.createMovement(
                    draftKey: draftKey,
                    momentId: momentId,
                    movementType: "SAVINGS_DEPOSIT",
                    amount: trimmedAmount,
                    currencyCode: "INR",
                    accountId: fromAccountId,
                    goalId: nil,
                    description: description,
                    effectiveAt: nil
                )
                finishSave()
            } catch {
                failSave(error)
            }
        }
    }

    private func tabHeading(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.plusJakarta(size: 20, weight: .bold))
                .foregroundStyle(MoneySheetTokens.text)
            Text(subtitle)
                .font(.plusJakarta(size: 12))
                .foregroundStyle(MoneySheetTokens.secondary)
        }
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.plusJakarta(size: 11, weight: .bold))
            .tracking(0.8)
            .foregroundStyle(MoneySheetTokens.brand)
    }

    private func titleField(value: Binding<String>, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("TITLE")
            ZStack(alignment: .leading) {
                if value.wrappedValue.isEmpty {
                    Text(placeholder)
                        .font(.plusJakarta(size: 15))
                        .foregroundStyle(MoneySheetTokens.secondary)
                }
                TextField("", text: value)
                    .font(.plusJakarta(size: 15))
                    .foregroundStyle(MoneySheetTokens.text)
            }
            .padding(14)
            .background(MoneySheetTokens.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(MoneySheetTokens.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    private func amountField(value: Binding<String>, accent: Color, testTag: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("AMOUNT")
            HStack(spacing: 8) {
                Text("₹")
                    .font(.plusJakarta(size: 22, weight: .bold))
                    .foregroundStyle(MoneySheetTokens.secondary)
                TextField("", text: value)
                    .keyboardType(.decimalPad)
                    .font(.plusJakarta(size: 28, weight: .heavy))
                    .foregroundStyle(MoneySheetTokens.text)
                    .onChange(of: value.wrappedValue) { _, newValue in
                        value.wrappedValue = newValue.filter { $0.isNumber || $0 == "." }
                    }
                if !value.wrappedValue.isEmpty {
                    Button {
                        value.wrappedValue = String(value.wrappedValue.dropLast())
                    } label: {
                        Image(systemName: "delete.left")
                            .font(.system(size: 18))
                            .foregroundStyle(MoneySheetTokens.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(14)
            .background(MoneySheetTokens.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(MoneySheetTokens.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .accessibilityIdentifier(testTag)
        }
    }

    private func noteField(value: Binding<String>, testTag: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("NOTE")
            ZStack(alignment: .leading) {
                if value.wrappedValue.isEmpty {
                    Text("Add a note...")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(MoneySheetTokens.secondary)
                }
                TextField("", text: value, axis: .vertical)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(MoneySheetTokens.text)
                    .lineLimit(3...6)
            }
            .padding(14)
            .background(MoneySheetTokens.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(MoneySheetTokens.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .accessibilityIdentifier(testTag)
        }
    }


    private func categoryChips(accent: Color, testTag: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("CATEGORY")
            FlowLayout(spacing: 8) {
                ForEach(PersonalExpenseCategoryCatalog.masterCategories) { cat in
                    let selected = category == cat.code
                    Button {
                        category = cat.code
                        subcategory = cat.subcategories.first?.code ?? subcategory
                    } label: {
                        Text("\(cat.emoji) \(cat.label)")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(selected ? .white : MoneySheetTokens.text)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? accent : MoneySheetTokens.surface)
                            .overlay(
                                Capsule().stroke(selected ? accent : MoneySheetTokens.cardBorder, lineWidth: 1)
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier(testTag)
                }
            }
        }
    }

    private func subcategoryChips(accent: Color, testTag: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("SUBCATEGORY")
            FlowLayout(spacing: 8) {
                ForEach(selectedCategory.subcategories) { sub in
                    let selected = subcategory == sub.code
                    Button {
                        subcategory = sub.code
                    } label: {
                        Text(sub.label)
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(selected ? .white : MoneySheetTokens.text)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? accent : MoneySheetTokens.surface)
                            .overlay(
                                Capsule().stroke(selected ? accent : MoneySheetTokens.cardBorder, lineWidth: 1)
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier(testTag)
                }
            }
        }
    }

    private func whenChips(selected: Binding<String>, accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            fieldLabel("WHEN")
            simpleChips(options: ["Now", "Today", "Yesterday", "Change"], selected: selected, accent: accent)
        }
    }

    private func simpleChips(options: [String], selected: Binding<String>, accent: Color) -> some View {
        FlowLayout(spacing: 8) {
            ForEach(options, id: \.self) { opt in
                let isOn = selected.wrappedValue == opt
                Button {
                    selected.wrappedValue = opt
                } label: {
                    Text(opt)
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(isOn ? .white : MoneySheetTokens.text)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(isOn ? accent : MoneySheetTokens.surface)
                        .overlay(
                            Capsule().stroke(isOn ? accent : MoneySheetTokens.cardBorder, lineWidth: 1)
                        )
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func savingsGoalCard(goal: SavingsGoal, selected: Bool, accent: Color, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            HStack {
                HStack(spacing: 12) {
                    Text(goal.emoji)
                        .font(.plusJakarta(size: 20))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(goal.name)
                            .font(.plusJakarta(size: 14, weight: .semibold))
                            .foregroundStyle(MoneySheetTokens.text)
                        Text(goal.targetLabel)
                            .font(.plusJakarta(size: 12))
                            .foregroundStyle(MoneySheetTokens.secondary)
                    }
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 20, height: 20)
                        .background(accent)
                        .clipShape(Circle())
                } else {
                    Circle()
                        .stroke(MoneySheetTokens.border, lineWidth: 2)
                        .frame(width: 20, height: 20)
                }
            }
            .padding(14)
            .background(selected ? MoneySheetTokens.surface : MoneySheetTokens.bg)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(selected ? accent : MoneySheetTokens.border, lineWidth: selected ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .padding(.vertical, 4)
    }

    private func saveButton(
        label: String,
        accent: Color,
        enabled: Bool,
        submitting: Bool,
        testTag: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Group {
                if submitting {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text(label)
                        .font(.plusJakarta(size: 15, weight: .heavy))
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(accent.opacity(enabled ? 1 : 0.5))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityIdentifier(testTag)
    }

    @MainActor
    private func finishSave() {
        submitting = false
        onSaved()
        onClose()
    }

    @MainActor
    private func failSave(_ error: Error) {
        submitting = false
        self.error = error.localizedDescription
    }
}

/// Simple wrapping layout for chips (shared by money quick-add sheets).
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: proposal, subviews: subviews)
        for (point, subview) in zip(result.positions, subviews) {
            subview.place(at: CGPoint(x: bounds.minX + point.x, y: bounds.minY + point.y), proposal: .unspecified)
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var width: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            width = max(width, x - spacing)
        }
        return (CGSize(width: width, height: y + rowHeight), positions)
    }
}

private struct OptionalAccessibilityIdentifier: ViewModifier {
    let tag: String?

    func body(content: Content) -> some View {
        if let tag, !tag.isEmpty {
            content.accessibilityIdentifier(tag)
        } else {
            content
        }
    }
}
