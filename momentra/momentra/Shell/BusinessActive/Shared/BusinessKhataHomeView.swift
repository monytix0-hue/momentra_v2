import SwiftUI

struct BusinessKhataHomeView: View {
    let momentId: String
    let companyId: String
    let shopName: String
    @Binding var isPresented: Bool

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
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#94A3B8"))

                    HStack(spacing: 8) {
                        tabChip("Customers", key: "CUSTOMER")
                        tabChip("Suppliers", key: "SUPPLIER")
                    }

                    if let error {
                        Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                    }
                    if loading {
                        ProgressView().tint(Color(hex: "#818CF8"))
                    } else if items.isEmpty {
                        Text(
                            tab == "CUSTOMER"
                                ? "Add a customer, then log udhaar when they take goods on credit"
                                : "Add a supplier, then log credit purchases and payouts"
                        )
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#64748B"))
                    } else {
                        ForEach(items) { party in
                            partyCard(party)
                        }
                    }

                    Button {
                        showAdd = true
                    } label: {
                        Text(tab == "SUPPLIER" ? "Add supplier" : "Add customer")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color(hex: "#F1F5F9"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color(hex: "#818CF8"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
                .padding(16)
            }
            .background(Color(hex: "#0F172A").ignoresSafeArea())
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
                }
            }
            .sheet(item: $cashSaleParty) { party in
                BusinessKhataCashSaleView(momentId: momentId, party: party) {
                    cashSaleParty = nil
                    Task { await reload() }
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
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(selected ? Color.white : Color(hex: "#64748B"))
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? Color(hex: "#818CF8") : Color(hex: "#1E293B"))
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
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(Color(hex: "#F1F5F9"))
                        let meta = [party.phone, party.overdue == true && due > 0 ? "Overdue" : nil]
                            .compactMap { $0 }
                            .joined(separator: " · ")
                        if !meta.isEmpty {
                            Text(meta)
                                .font(.system(size: 12))
                                .foregroundStyle(Color(hex: "#64748B"))
                        }
                    }
                    Spacer()
                    Text(formatDue(party.balanceDue))
                        .font(.system(size: 16, weight: .heavy))
                        .foregroundStyle(due > 0 ? Color(hex: "#F59E0B") : Color(hex: "#F1F5F9"))
                    Text("›")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(Color(hex: "#64748B"))
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
        .background(Color(hex: "#1E293B"))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "#334155")))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func miniBtn(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color(hex: "#818CF8"))
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(Color(hex: "#818CF8").opacity(0.15))
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
                    .textFieldStyle(.roundedBorder)
                TextField("Phone (for WhatsApp remind)", text: $phone)
                    .keyboardType(.phonePad)
                    .textFieldStyle(.roundedBorder)
                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(submitting ? "Saving…" : "Save")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#818CF8"))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
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
                Text(party.name).font(.system(size: 13)).foregroundStyle(Color(hex: "#94A3B8"))
                TextField("Amount", text: $amount)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                if isPayment {
                    Picker("Payment", selection: $payment) {
                        Text("Cash").tag("Cash")
                        Text("UPI").tag("UPI")
                        Text("Card").tag("Card")
                    }
                    .pickerStyle(.segmented)
                }
                TextField("Note (optional)", text: $note)
                    .textFieldStyle(.roundedBorder)
                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(submitting ? "Saving…" : "Save")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#818CF8"))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
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
                Text(party.name).font(.system(size: 13)).foregroundStyle(Color(hex: "#94A3B8"))
                TextField("Amount", text: $amount)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                Picker("Payment", selection: $payment) {
                    Text("Cash").tag("Cash")
                    Text("UPI").tag("UPI")
                    Text("Card").tag("Card")
                }
                .pickerStyle(.segmented)
                TextField("Note (optional)", text: $note)
                    .textFieldStyle(.roundedBorder)
                Text("Does not change udhaar balance")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: "#64748B"))
                if let error {
                    Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                }
                Button {
                    Task { await save() }
                } label: {
                    Text(submitting ? "Saving…" : "Save cash sale")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#818CF8"))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
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
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F59E0B"))
                    if let error {
                        Text(error).font(.system(size: 12)).foregroundStyle(Color(hex: "#F87171"))
                    }
                    if loading {
                        ProgressView().tint(Color(hex: "#818CF8"))
                    } else if entries.isEmpty {
                        Text("No entries yet")
                            .font(.system(size: 14))
                            .foregroundStyle(Color(hex: "#64748B"))
                    } else {
                        ForEach(entries) { entry in
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(entry.entryType.uppercased() == "PAYMENT" ? "Payment" : "Credit")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(Color(hex: "#F1F5F9"))
                                    if let note = entry.note, !note.isEmpty {
                                        Text(note)
                                            .font(.system(size: 12))
                                            .foregroundStyle(Color(hex: "#64748B"))
                                    }
                                    if let at = entry.effectiveAt, !at.isEmpty {
                                        Text(String(at.prefix(10)))
                                            .font(.system(size: 11))
                                            .foregroundStyle(Color(hex: "#64748B"))
                                    }
                                }
                                Spacer()
                                Text(formatDue(entry.amount))
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(
                                        entry.entryType.uppercased() == "PAYMENT"
                                            ? Color(hex: "#34D399")
                                            : Color(hex: "#F59E0B")
                                    )
                            }
                            .padding(12)
                            .background(Color(hex: "#1E293B"))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }
                .padding(16)
            }
            .background(Color(hex: "#0F172A").ignoresSafeArea())
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
