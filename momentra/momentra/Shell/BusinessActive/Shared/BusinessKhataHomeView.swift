import SwiftUI

private let KhataAccent = Color(hex: "#818CF8")
private let KhataPaid = Color(hex: "#34D399")
private let KhataDue = Color(hex: "#F59E0B")


/// Dark-sheet text field: the native `.roundedBorder` style renders a light gray box
/// that breaks the Business sheet surface.
private struct KhataFieldModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .font(.plusJakarta(size: 15))
            .foregroundStyle(BusinessSheetTokens.text)
            .padding(.horizontal, 14)
            .frame(height: 48)
            .background(BusinessSheetTokens.field)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(BusinessSheetTokens.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private extension View {
    func khataField() -> some View { modifier(KhataFieldModifier()) }
}

struct BusinessKhataHomeView: View {
    let momentId: String
    let companyId: String
    let shopName: String
    @Binding var isPresented: Bool
    var onEntrySaved: () -> Void = {}

    @State private var tab = "CUSTOMER"
    @State private var items: [APIClient.KhataPartyItem] = []
    @State private var loading = true
    @State private var error: String?
    @State private var showAdd = false
    @State private var entryParty: APIClient.KhataPartyItem?
    @State private var entryType = "CREDIT"
    @State private var cashSaleParty: APIClient.KhataPartyItem?
    @State private var historyParty: APIClient.KhataPartyItem?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Who owes you — and who you owe")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(BusinessSheetTokens.muted)

                    HStack(spacing: 8) {
                        tabChip("Customers", key: "CUSTOMER")
                        tabChip("Suppliers", key: "SUPPLIER")
                    }

                    if let error {
                        Text(error).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                    }
                    if loading {
                        ProgressView().tint(KhataAccent)
                    } else if items.isEmpty {
                        BusinessEmptyState(
                            title: tab == "SUPPLIER" ? "No suppliers yet" : "No customers yet",
                            message: tab == "CUSTOMER"
                                ? "Add a customer, then log udhaar when they take goods on credit."
                                : "Add a supplier, then log credit purchases and payouts.",
                            accent: KhataAccent,
                            glyph: tab == "SUPPLIER" ? "📦" : "👤",
                            actionLabel: tab == "SUPPLIER" ? "Add supplier" : "Add customer",
                            onAction: { showAdd = true }
                        )
                    } else {
                        ForEach(items) { party in
                            partyCard(party)
                        }
                    }

                    Button {
                        showAdd = true
                    } label: {
                        Text(tab == "SUPPLIER" ? "Add supplier" : "Add customer")
                            .font(.plusJakarta(size: 15, weight: .semibold))
                            .foregroundStyle(Color(hex: "#14121B"))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(
                                LinearGradient(
                                    colors: [KhataAccent, Color(hex: "#A5B4FC")],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .shadow(color: KhataAccent.opacity(0.4), radius: 10, y: 4)
                    }
                    .buttonStyle(.plain)
                }
                .padding(16)
            }
            .background(BusinessSheetTokens.bg.ignoresSafeArea())
            .navigationTitle("Khata")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                }
            }
            .task(id: "\(companyId)-\(tab)") { await reload() }
            .sheet(isPresented: $showAdd) {
                BusinessKhataAddPartyView(companyId: companyId, partyKind: tab) {
                    showAdd = false
                    Task { await reload() }
                }
            }
            .sheet(item: $entryParty) { party in
                BusinessKhataEntryView(
                    momentId: momentId,
                    party: party,
                    entryType: entryType
                ) {
                    entryParty = nil
                    Task { await reload() }
                    onEntrySaved()
                }
            }
            .sheet(item: $cashSaleParty) { party in
                BusinessKhataCashSaleView(momentId: momentId, party: party) {
                    cashSaleParty = nil
                    Task { await reload() }
                    onEntrySaved()
                }
            }
            .sheet(item: $historyParty) { party in
                BusinessKhataHistoryView(companyId: companyId, party: party)
            }
        }
    }

    private func tabChip(_ label: String, key: String) -> some View {
        let selected = tab == key
        return Button {
            tab = key
        } label: {
            Text(label)
                .font(.plusJakarta(size: 13, weight: .semibold))
                .foregroundStyle(selected ? Color.white : BusinessSheetTokens.muted)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? KhataAccent : BusinessSheetTokens.field)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private func partyCard(_ party: APIClient.KhataPartyItem) -> some View {
        let due = Double(party.balanceDue) ?? 0
        let supplier = (party.partyKind ?? "").uppercased() == "SUPPLIER"
        return VStack(alignment: .leading, spacing: 10) {
            Button {
                historyParty = party
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(party.name)
                            .font(.plusJakarta(size: 15, weight: .bold))
                            .foregroundStyle(BusinessSheetTokens.text)
                        let meta = [party.phone, party.overdue == true && due > 0 ? "Overdue" : nil]
                            .compactMap { $0 }
                            .joined(separator: " · ")
                        if !meta.isEmpty {
                            Text(meta)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(BusinessSheetTokens.muted)
                        }
                    }
                    Spacer()
                    Text(formatDue(party.balanceDue))
                        .font(.plusJakarta(size: 16, weight: .heavy))
                        .foregroundStyle(due > 0 ? KhataDue : BusinessSheetTokens.text)
                    Text("›")
                        .font(.plusJakarta(size: 18, weight: .medium))
                        .foregroundStyle(BusinessSheetTokens.muted)
                }
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    miniBtn(supplier ? "Udhaar liya" : "Udhaar diya") {
                        entryType = "CREDIT"
                        entryParty = party
                    }
                    miniBtn(supplier ? "Paisa diya" : "Paisa mila") {
                        entryType = "PAYMENT"
                        entryParty = party
                    }
                    if !supplier {
                        miniBtn("Cash sale") {
                            cashSaleParty = party
                        }
                    }
                }
                if let phone = party.phone, !phone.isEmpty, due > 0 {
                    HStack(spacing: 8) {
                        miniBtn("Remind") {
                            let msg = khataReminderMessage(
                                partyName: party.name,
                                amountDue: party.balanceDue,
                                shopName: shopName
                            )
                            InviteOutboundShare.sendWhatsApp(phone: phone, message: msg)
                        }
                    }
                }
            }
        }
        .padding(14)
        .background(BusinessSheetTokens.field)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(BusinessSheetTokens.border))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func miniBtn(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(KhataAccent)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(KhataAccent.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }

    private func reload() async {
        guard !companyId.isEmpty else { return }
        loading = true
        error = nil
        do {
            let list = try await APIClient.shared.listKhataParties(companyId: companyId, partyKind: tab)
            items = list.items
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}

private func formatDue(_ raw: String) -> String {
    let cleaned = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if let d = Double(cleaned) {
        return String(format: "₹%.2f", d)
    }
    return "₹\(cleaned)"
}

func khataReminderMessage(partyName: String, amountDue: String, shopName: String) -> String {
    let amt = formatDue(amountDue)
    let shop = shopName.isEmpty ? "our shop" : shopName
    return """
    Namaste \(partyName),

    Aapka khata balance \(amt) hai (\(shop)).
    Your credit balance is \(amt).

    Please settle when convenient. Dhanyavaad!
    """
}

private struct BusinessKhataAddPartyView: View {
    let companyId: String
    let partyKind: String
    var onSaved: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var phone = ""
    @State private var submitting = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                TextField(partyKind == "SUPPLIER" ? "Supplier name" : "Customer name", text: $name)
                    .khataField()
                TextField("Phone (for WhatsApp remind)", text: $phone)
                    .keyboardType(.phonePad)
                    .khataField()
                if let error {
                    Text(error).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(submitting ? "Saving…" : "Save")
                        .font(.plusJakarta(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: "#14121B"))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(
                            LinearGradient(
                                colors: [KhataAccent, Color(hex: "#A5B4FC")],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: KhataAccent.opacity(0.4), radius: 10, y: 4)
                }
                .buttonStyle(.plain)
                .disabled(submitting || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                Spacer()
            }
            .padding(16)
            .navigationTitle(partyKind == "SUPPLIER" ? "Add supplier" : "Add customer")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func save() async {
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createKhataParty(
                companyId: companyId,
                name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                partyKind: partyKind,
                phone: phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : phone.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            onSaved()
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

private struct BusinessKhataEntryView: View {
    let momentId: String
    let party: APIClient.KhataPartyItem
    let entryType: String
    var onSaved: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var amount = ""
    @State private var note = ""
    @State private var payment = "Cash"
    @State private var submitting = false
    @State private var error: String?

    private var isPayment: Bool { entryType == "PAYMENT" }
    private var isSupplier: Bool { (party.partyKind ?? "").uppercased() == "SUPPLIER" }

    private var title: String {
        switch (isPayment, isSupplier) {
        case (true, true): return "Paisa diya · Collection"
        case (true, false): return "Paisa mila · Collection"
        case (false, true): return "Udhaar liya · Credit purchase"
        case (false, false): return "Udhaar diya · Credit sale"
        }
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                Text(party.name).font(.plusJakarta(size: 13)).foregroundStyle(BusinessSheetTokens.muted)
                TextField("Amount", text: $amount)
                    .keyboardType(.decimalPad)
                    .khataField()
                if isPayment {
                    Picker("Payment", selection: $payment) {
                        Text("Cash").tag("Cash")
                        Text("UPI").tag("UPI")
                        Text("Card").tag("Card")
                    }
                    .pickerStyle(.segmented)
                }
                TextField("Note (optional)", text: $note)
                    .khataField()
                if let error {
                    Text(error).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(submitting ? "Saving…" : "Save")
                        .font(.plusJakarta(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: "#14121B"))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(
                            LinearGradient(
                                colors: [KhataAccent, Color(hex: "#A5B4FC")],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: KhataAccent.opacity(0.4), radius: 10, y: 4)
                }
                .buttonStyle(.plain)
                .disabled(submitting)
                Spacer()
            }
            .padding(16)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func save() async {
        let amt = amount.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !amt.isEmpty, amt != "0" else {
            error = "Enter an amount"
            return
        }
        submitting = true
        error = nil
        let method: String? = isPayment ? (payment == "UPI" ? "UPI" : payment == "Card" ? "CARD" : "CASH") : nil
        do {
            _ = try await APIClient.shared.createKhataEntry(
                momentId: momentId,
                partyId: party.partyId,
                entryType: entryType,
                amount: amt,
                currencyCode: party.currencyCode ?? "INR",
                note: note.isEmpty ? nil : note,
                paymentMethodCode: method
            )
            onSaved()
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

private struct BusinessKhataCashSaleView: View {
    let momentId: String
    let party: APIClient.KhataPartyItem
    var onSaved: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var amount = ""
    @State private var note = ""
    @State private var payment = "Cash"
    @State private var submitting = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 12) {
                Text(party.name).font(.plusJakarta(size: 13)).foregroundStyle(BusinessSheetTokens.muted)
                TextField("Amount", text: $amount)
                    .keyboardType(.decimalPad)
                    .khataField()
                Picker("Payment", selection: $payment) {
                    Text("Cash").tag("Cash")
                    Text("UPI").tag("UPI")
                    Text("Card").tag("Card")
                }
                .pickerStyle(.segmented)
                TextField("Note (optional)", text: $note)
                    .khataField()
                Text("Does not change udhaar balance")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(BusinessSheetTokens.muted)
                if let error {
                    Text(error).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(submitting ? "Saving…" : "Save cash sale")
                        .font(.plusJakarta(size: 15, weight: .semibold))
                        .foregroundStyle(Color(hex: "#14121B"))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(
                            LinearGradient(
                                colors: [KhataAccent, Color(hex: "#A5B4FC")],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: KhataAccent.opacity(0.4), radius: 10, y: 4)
                }
                .buttonStyle(.plain)
                .disabled(submitting)
                Spacer()
            }
            .padding(16)
            .navigationTitle("Cash sale · Naqd becha")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func save() async {
        let amt = amount.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !amt.isEmpty, amt != "0" else {
            error = "Enter an amount"
            return
        }
        submitting = true
        error = nil
        let method = payment == "UPI" ? "UPI" : payment == "Card" ? "CARD" : "CASH"
        do {
            _ = try await APIClient.shared.createBusinessRevenue(
                momentId: momentId,
                amount: amt,
                currencyCode: (party.currencyCode ?? "INR").uppercased(),
                description: note.isEmpty ? "Cash sale · \(party.name)" : note,
                partyId: party.partyId,
                paymentMethodCode: method
            )
            onSaved()
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

private struct BusinessKhataHistoryView: View {
    let companyId: String
    let party: APIClient.KhataPartyItem

    @Environment(\.dismiss) private var dismiss
    @State private var loading = true
    @State private var error: String?
    @State private var entries: [APIClient.KhataEntryItem] = []
    @State private var balanceDue: String

    init(companyId: String, party: APIClient.KhataPartyItem) {
        self.companyId = companyId
        self.party = party
        _balanceDue = State(initialValue: party.balanceDue)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Due \(formatDue(balanceDue))")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(KhataDue)
                    if let error {
                        Text(error).font(.plusJakarta(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                    }
                    if loading {
                        ProgressView().tint(KhataAccent)
                    } else if entries.isEmpty {
                        Text("No entries yet")
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(BusinessSheetTokens.muted)
                    } else {
                        ForEach(entries) { entry in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(entry.entryType.uppercased() == "PAYMENT" ? "Payment" : "Credit")
                                        .font(.plusJakarta(size: 14, weight: .semibold))
                                        .foregroundStyle(BusinessSheetTokens.text)
                                    if let note = entry.note, !note.isEmpty {
                                        Text(note)
                                            .font(.plusJakarta(size: 12))
                                            .foregroundStyle(BusinessSheetTokens.muted)
                                    }
                                    if let at = entry.effectiveAt, !at.isEmpty {
                                        Text(String(at.prefix(10)))
                                            .font(.plusJakarta(size: 11))
                                            .foregroundStyle(BusinessSheetTokens.muted)
                                    }
                                }
                                Spacer()
                                Text(formatDue(entry.amount))
                                    .font(.plusJakarta(size: 14, weight: .bold))
                                    .foregroundStyle(
                                        entry.entryType.uppercased() == "PAYMENT"
                                            ? KhataPaid
                                            : KhataDue
                                    )
                            }
                            .padding(12)
                            .background(BusinessSheetTokens.field)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }
                .padding(16)
            }
            .background(BusinessSheetTokens.bg.ignoresSafeArea())
            .navigationTitle(party.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .task {
                loading = true
                error = nil
                do {
                    let result = try await APIClient.shared.listKhataPartyEntries(
                        companyId: companyId,
                        partyId: party.partyId
                    )
                    entries = result.items
                    if let due = result.balanceDue {
                        balanceDue = due
                    }
                } catch {
                    self.error = error.localizedDescription
                }
                loading = false
            }
        }
        .presentationDetents([.medium, .large])
    }
}
