import SwiftUI

/// Figma 453:9376 Master Expense — personal expense create premium layout.
struct PersonalMasterExpenseSheet: View {
    let momentId: String
    var pulseFamily: PersonalPulseFamily = .lifeOperations
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var purpose = ""
    @State private var amount = ""
    @State private var categoryCode = PersonalExpenseCategoryCatalog.masterCategories.first?.code ?? "FOOD"
    @State private var paidFrom = "Primary"
    @State private var selectedAccountId: String?
    @State private var showAccountPicker = false
    @State private var accounts: [APIClient.FinancialAccount] = []
    @State private var whenCode = "Today"
    @State private var showWhenPicker = false
    @State private var showDetails = true
    @State private var selectedFeelings: Set<String> = []
    @State private var meaningfulness = "Medium"
    @State private var memorability = "High"
    @State private var sharedExperience = true
    @State private var sharedWith: Set<String> = []
    @State private var relationshipImpact: Set<String> = []
    @State private var reasoning: Set<String> = []
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?
    @State private var draftKey = UUID().uuidString

    private let expenseRepository = ExpenseCreateRepository()

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                meHeader
                subtitleBlock
                smartBanner
                purposeField
                amountField
                categoryGrid
                paidFromSection
                whenSection
                moreDetailsSection
                notesField
                if let error { Text(error).font(.system(size: 12)).foregroundStyle(PersonalMasterExpenseTheme.error) }
                footerButtons
                privacyLine
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(PersonalMasterExpenseTheme.bg)
        .task {
            accounts = (try? await APIClient.shared.listFinancialAccounts()) ?? []
            selectedAccountId = accounts.first?.financialAccountId
            paidFrom = accounts.first?.accountName ?? "Primary"
        }
        .sheet(isPresented: $showAccountPicker) {
            PersonalAccountPickerSheet(
                selectedAccountId: selectedAccountId,
                onSelect: { account in
                    if !accounts.contains(where: { $0.financialAccountId == account.financialAccountId }) {
                        accounts.append(account)
                    }
                    selectedAccountId = account.financialAccountId
                    paidFrom = account.accountName
                },
                onClose: { showAccountPicker = false }
            )
            .presentationDetents([.large])
        }
        .confirmationDialog("When", isPresented: $showWhenPicker, titleVisibility: .visible) {
            ForEach(PersonalMasterExpenseTheme.whenOptions, id: \.self) { opt in
                Button(opt) { whenCode = opt }
            }
        }
    }

    private var meHeader: some View {
        HStack {
            Button(action: onClose) {
                MeIcon(
                    symbol: PersonalMasterExpenseIcons.Chrome.back.rawValue,
                    tint: PersonalMasterExpenseTheme.accent,
                    size: 22,
                    accessibilityLabel: "Back"
                )
                .frame(width: 36, height: 36)
                .background(PersonalMasterExpenseTheme.surfaceSolid.opacity(0.7))
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(PersonalMasterExpenseTheme.border))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
            .buttonStyle(.plain)
            HStack(spacing: 8) {
                MeIcon(
                    symbol: PersonalMasterExpenseIcons.Chrome.header.rawValue,
                    tint: PersonalMasterExpenseTheme.accent,
                    size: 16
                )
                .padding(8)
                .background(PersonalMasterExpenseTheme.accent.opacity(0.08))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(PersonalMasterExpenseTheme.accent))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                Text("Master Expense")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
            }
            Spacer()
            Button(action: clearAll) {
                Text("Clear All")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(PersonalMasterExpenseTheme.accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(PersonalMasterExpenseTheme.accent.opacity(0.08))
                    .overlay(Capsule().stroke(PersonalMasterExpenseTheme.accent))
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("master_expense_clear_all")
        }
    }

    private var subtitleBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("One expense. Impact across your life.")
                .font(.system(size: 14))
                .foregroundStyle(PersonalMasterExpenseTheme.muted)
            LinearGradient(
                colors: [PersonalMasterExpenseTheme.accent, PersonalMasterExpenseTheme.accentLight],
                startPoint: .leading,
                endPoint: .trailing
            )
            .frame(height: 2)
            .clipShape(RoundedRectangle(cornerRadius: 1))
        }
    }

    private var smartBanner: some View {
        HStack(alignment: .top, spacing: 12) {
            MeIcon(
                symbol: PersonalMasterExpenseIcons.Chrome.info.rawValue,
                tint: PersonalMasterExpenseTheme.accent,
                size: 18
            )
            Text("This entry can update Life operations, Lifestyle and Relationships.")
                .font(.system(size: 14))
                .foregroundStyle(.white)
                .lineSpacing(4)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PersonalMasterExpenseTheme.accent.opacity(0.08))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalMasterExpenseTheme.accent))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var purposeField: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("What did you spend on?")
            HStack(spacing: 10) {
                MeIcon(
                    symbol: PersonalMasterExpenseIcons.Chrome.edit.rawValue,
                    tint: PersonalMasterExpenseTheme.accent,
                    size: 18
                )
                TextField("Dinner with friends", text: $purpose)
                    .foregroundStyle(PersonalMasterExpenseTheme.textMain)
                    .font(.system(size: 15, weight: .medium))
            }
            .padding(18)
            .background(PersonalMasterExpenseTheme.surfaceSolid.opacity(0.7))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalMasterExpenseTheme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var amountField: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("Amount")
            HStack(spacing: 12) {
                MeIcon(
                    symbol: PersonalMasterExpenseIcons.Chrome.amount.rawValue,
                    tint: .white,
                    size: 18
                )
                .frame(width: 32, height: 32)
                .background(PersonalMasterExpenseTheme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                TextField("0.00", text: $amount)
                    .font(.system(size: 40, weight: .heavy))
                    .foregroundStyle(.white)
                    .keyboardType(.decimalPad)
            }
            .padding(20)
            .background(PersonalMasterExpenseTheme.surfaceSolid.opacity(0.7))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalMasterExpenseTheme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var categoryGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel("Category")
            let categories = PersonalExpenseCategoryCatalog.masterCategories
            ForEach(Array(stride(from: 0, to: categories.count, by: 2)), id: \.self) { index in
                HStack(spacing: 12) {
                    categoryTile(categories[index])
                    if index + 1 < categories.count {
                        categoryTile(categories[index + 1])
                    } else {
                        Spacer(minLength: 0)
                    }
                }
            }
        }
    }

    private func categoryTile(_ cat: PersonalExpenseCategoryCatalog.Category) -> some View {
        let selected = categoryCode == cat.code
        return Button {
            categoryCode = cat.code
        } label: {
            VStack(spacing: 4) {
                MeIcon(
                    symbol: PersonalMasterExpenseIcons.categorySymbol(code: cat.code),
                    tint: .white,
                    size: 24,
                    accessibilityLabel: cat.label
                )
                Text(cat.label)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 72)
            .background(selected ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.categoryUnselected)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(selected ? PersonalMasterExpenseTheme.accentLight : PersonalMasterExpenseTheme.categoryBorder)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private var paidFromSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("Paid From")
            rowCard(
                symbol: accounts.first(where: { $0.financialAccountId == selectedAccountId }).map {
                    PersonalFinancialAccountUi.nativeSymbolForType($0.accountType)
                } ?? PersonalFinancialAccountUi.nativeSymbolForType("BANK"),
                label: accounts.first(where: { $0.financialAccountId == selectedAccountId })?.accountName ?? paidFrom,
                onChange: { showAccountPicker = true }
            )
        }
    }

    private var whenSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("When")
            rowCard(
                symbol: PersonalMasterExpenseIcons.Chrome.calendar.rawValue,
                label: formatWhenLabel(whenCode),
                onChange: { showWhenPicker = true }
            )
        }
    }

    private var moreDetailsSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Button { showDetails.toggle() } label: {
                HStack {
                    HStack(spacing: 8) {
                        MeIcon(
                            symbol: PersonalMasterExpenseIcons.Chrome.folder.rawValue,
                            tint: PersonalMasterExpenseTheme.accent,
                            size: 16
                        )
                        Text("More details")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(PersonalMasterExpenseTheme.textMain)
                    }
                    Spacer()
                    MeIcon(
                        symbol: showDetails
                            ? PersonalMasterExpenseIcons.Chrome.expandUp.rawValue
                            : PersonalMasterExpenseIcons.Chrome.expandDown.rawValue,
                        tint: PersonalMasterExpenseTheme.muted,
                        size: 12
                    )
                }
            }
            .buttonStyle(.plain)

            if showDetails {
                Text("How did this make you feel?")
                    .font(.system(size: 12))
                    .foregroundStyle(PersonalMasterExpenseTheme.muted)
                FlowLayout(spacing: 8) {
                    ForEach(PersonalMasterExpenseTheme.emotionalOptions) { opt in
                        emotionalChip(opt)
                    }
                }
                segmentControl(
                    label: "How meaningful was this experience?",
                    selected: meaningfulness,
                    onSelect: { meaningfulness = $0 }
                )
                segmentControl(
                    label: "How memorable was this?",
                    selected: memorability,
                    onSelect: { memorability = $0 }
                )
                HStack {
                    HStack(spacing: 8) {
                        Text("✨")
                        Text("Shared experience")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(PersonalMasterExpenseTheme.textMain)
                    }
                    Spacer()
                    Text(sharedExperience ? "ON" : "OFF")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(sharedExperience ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.categoryUnselected)
                        .clipShape(Capsule())
                        .onTapGesture { sharedExperience.toggle() }
                }
                if sharedExperience {
                    Text("Shared with").font(.system(size: 12)).foregroundStyle(PersonalMasterExpenseTheme.muted)
                    FlowLayout(spacing: 8) {
                        ForEach(PersonalMasterExpenseTheme.sharedWithOptions, id: \.self) { label in
                            meChip(label, selected: sharedWith.contains(label)) {
                                toggleSet(label, in: &sharedWith)
                            }
                        }
                    }
                    Text("What was the impact on this relationship?")
                        .font(.system(size: 12))
                        .foregroundStyle(PersonalMasterExpenseTheme.muted)
                    ForEach(PersonalMasterExpenseTheme.relationshipImpactOptions) { opt in
                        relationshipCard(opt)
                    }
                    Text("Why did this happen?")
                        .font(.system(size: 12))
                        .foregroundStyle(PersonalMasterExpenseTheme.muted)
                    FlowLayout(spacing: 8) {
                        ForEach(PersonalMasterExpenseTheme.reasoningOptions, id: \.self) { label in
                            meChip(label, selected: reasoning.contains(label)) {
                                toggleSet(label, in: &reasoning)
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(PersonalMasterExpenseTheme.surfaceSolid.opacity(0.5))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalMasterExpenseTheme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var notesField: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("Notes")
            VStack(alignment: .trailing, spacing: 8) {
                HStack(alignment: .top, spacing: 10) {
                    MeIcon(
                        symbol: PersonalMasterExpenseIcons.Chrome.edit.rawValue,
                        tint: PersonalMasterExpenseTheme.accent,
                        size: 18
                    )
                    TextField("Add any additional notes...", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                        .foregroundStyle(PersonalMasterExpenseTheme.textMain)
                        .onChange(of: notes) { _, newValue in
                            if newValue.count > 200 { notes = String(newValue.prefix(200)) }
                        }
                }
                Text("\(notes.count)/200")
                    .font(.system(size: 12))
                    .foregroundStyle(PersonalMasterExpenseTheme.muted)
            }
            .padding(18)
            .background(PersonalMasterExpenseTheme.surfaceSolid.opacity(0.7))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalMasterExpenseTheme.border))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private var footerButtons: some View {
        HStack(spacing: 12) {
            Button(action: onClose) {
                Text("Cancel")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(PersonalMasterExpenseTheme.textMain)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.plain)
            Button(action: submit) {
                Text(submitting ? "Saving…" : "Confirm Expense ✓")
                    .font(.system(size: 15, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(PersonalMasterExpenseTheme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(amount.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || submitting)
            .opacity(amount.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || submitting ? 0.5 : 1)
            .accessibilityIdentifier("master_expense_confirm")
        }
    }

    private var privacyLine: some View {
        HStack(spacing: 6) {
            Text("🔒").font(.system(size: 12))
            Text("Your details private and secure.")
                .font(.system(size: 12))
                .foregroundStyle(PersonalMasterExpenseTheme.muted)
        }
        .frame(maxWidth: .infinity)
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.system(size: 11, weight: .bold))
            .kerning(1.2)
            .foregroundStyle(PersonalMasterExpenseTheme.accent)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func rowCard(symbol: String, label: String, onChange: @escaping () -> Void) -> some View {
        HStack {
            HStack(spacing: 12) {
                MeIcon(symbol: symbol, tint: PersonalMasterExpenseTheme.accent, size: 20)
                Text(label)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(PersonalMasterExpenseTheme.textMain)
                    .lineLimit(1)
            }
            Spacer()
            Button("Change", action: onChange)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(PersonalMasterExpenseTheme.accent)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(PersonalMasterExpenseTheme.accent.opacity(0.08))
                .overlay(Capsule().stroke(PersonalMasterExpenseTheme.accent))
                .clipShape(Capsule())
                .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(PersonalMasterExpenseTheme.surfaceSolid.opacity(0.7))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PersonalMasterExpenseTheme.border))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func emotionalChip(_ opt: PersonalMasterExpenseTheme.EmotionalOption) -> some View {
        let selected = selectedFeelings.contains(opt.label)
        return Button {
            toggleSet(opt.label, in: &selectedFeelings)
        } label: {
            VStack(spacing: 4) {
                Text(opt.emoji).font(.system(size: 22))
                Text(opt.label)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(selected ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.muted)
                    .lineLimit(1)
            }
            .frame(width: 72)
            .padding(.vertical, 8)
            .background(selected ? PersonalMasterExpenseTheme.accent.opacity(0.15) : PersonalMasterExpenseTheme.categoryUnselected)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(selected ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.categoryBorder)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func meChip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Text(label)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selected ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.categoryUnselected)
            .overlay(
                Capsule().stroke(selected ? PersonalMasterExpenseTheme.accentLight : PersonalMasterExpenseTheme.categoryBorder)
            )
            .clipShape(Capsule())
            .onTapGesture(perform: action)
    }

    private func relationshipCard(_ opt: PersonalMasterExpenseTheme.RelationshipImpactOption) -> some View {
        let selected = relationshipImpact.contains(opt.label)
        return Button {
            toggleSet(opt.label, in: &relationshipImpact)
        } label: {
            HStack(spacing: 12) {
                Text(opt.emoji).font(.system(size: 20))
                Text(opt.label)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(selected ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.textMain)
                Spacer()
            }
            .padding(14)
            .background(selected ? PersonalMasterExpenseTheme.accent.opacity(0.12) : PersonalMasterExpenseTheme.categoryUnselected)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(selected ? PersonalMasterExpenseTheme.accent : PersonalMasterExpenseTheme.categoryBorder)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    private func segmentControl(label: String, selected: String, onSelect: @escaping (String) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label).font(.system(size: 12)).foregroundStyle(PersonalMasterExpenseTheme.muted)
            HStack(spacing: 4) {
                ForEach(PersonalMasterExpenseTheme.segmentOptions, id: \.self) { opt in
                    Text(opt)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(selected == opt ? .white : PersonalMasterExpenseTheme.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(selected == opt ? PersonalMasterExpenseTheme.accent : Color.clear)
                        .clipShape(Capsule())
                        .onTapGesture { onSelect(opt) }
                }
            }
            .padding(4)
            .background(PersonalMasterExpenseTheme.categoryUnselected)
            .overlay(Capsule().stroke(PersonalMasterExpenseTheme.categoryBorder))
            .clipShape(Capsule())
        }
    }

    private func formatWhenLabel(_ whenCode: String) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        let time = formatter.string(from: Date())
        switch whenCode {
        case "Yesterday": return "Yesterday \(time)"
        case "Today": return "Today \(time)"
        default: return "Now"
        }
    }

    private func toggleSet(_ value: String, in set: inout Set<String>) {
        if set.contains(value) { set.remove(value) } else { set.insert(value) }
    }

    private func clearAll() {
        purpose = ""
        amount = ""
        categoryCode = PersonalExpenseCategoryCatalog.masterCategories.first?.code ?? "FOOD"
        notes = ""
        selectedFeelings = []
        relationshipImpact = []
        sharedWith = []
        reasoning = []
        whenCode = "Today"
        meaningfulness = "Medium"
        memorability = "High"
        sharedExperience = true
    }

    private func effectiveAtIso() -> String? {
        let now = Date()
        let cal = Calendar.current
        let date: Date
        switch whenCode {
        case "Yesterday": date = cal.date(byAdding: .day, value: -1, to: now) ?? now
        case "Today", "Now": date = now
        default: date = now
        }
        return ISO8601DateFormatter().string(from: date)
    }

    private func submit() {
        guard !submitting else { return }
        submitting = true
        error = nil
        let description = buildDescription()
        Task {
            do {
                _ = try await expenseRepository.createExpense(
                    draftKey: draftKey,
                    momentId: momentId,
                    amount: amount.trimmingCharacters(in: .whitespacesAndNewlines),
                    currencyCode: "INR",
                    description: description,
                    merchantName: purpose.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : purpose.trimmingCharacters(in: .whitespacesAndNewlines),
                    categoryCode: categoryCode,
                    financialAccountId: selectedAccountId,
                    paymentMethodCode: nil,
                    effectiveAt: effectiveAtIso()
                )
                await MainActor.run {
                    submitting = false
                    onSaved()
                }
            } catch {
                await MainActor.run {
                    submitting = false
                    self.error = error.localizedDescription
                }
            }
        }
    }

    private func buildDescription() -> String? {
        var parts = [String]()
        let trimmedNotes = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedNotes.isEmpty { parts.append(trimmedNotes) }
        if !selectedFeelings.isEmpty { parts.append("Feelings: \(selectedFeelings.sorted().joined(separator: ", "))") }
        parts.append("Meaning: \(meaningfulness) · Memory: \(memorability)")
        if sharedExperience {
            parts.append("Shared experience")
            if !sharedWith.isEmpty { parts.append("Shared with: \(sharedWith.sorted().joined(separator: ", "))") }
        }
        if !relationshipImpact.isEmpty { parts.append("Relationship impact: \(relationshipImpact.sorted().joined(separator: ", "))") }
        if !reasoning.isEmpty { parts.append("Reason: \(reasoning.sorted().joined(separator: ", "))") }
        parts.append("When: \(whenCode) · Paid from: \(paidFrom)")
        return parts.joined(separator: " · ")
    }
}
