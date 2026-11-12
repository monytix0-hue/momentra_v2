import SwiftUI

/// Figma SVC-QA-01…10 field layouts — mirrors Android `ServiceCaptureLayouts.kt`.
/// Used by `OperatingCaptureSheet` when `theme.vertical == .consumerService`.

struct TealAmountCard: View {
    let theme: OperatingTheme
    let label: String
    let currency: String
    @Binding var amount: String
    var hint: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                Spacer(minLength: 8)
                Text(currency.isEmpty ? "INR" : currency)
                    .font(.plusJakarta(size: 11, weight: .bold))
                    .foregroundStyle(TealSheetChrome.lime)
            }
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("₹")
                    .font(.plusJakarta(size: 28, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                TextField("0", text: $amount)
                    .font(.plusJakarta(size: 30, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                    .keyboardType(.decimalPad)
            }
            Text(hint)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(OperatingV12.subtitle)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.inset)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.55), lineWidth: 1)
        )
    }
}

struct TealLabeledField: View {
    let theme: OperatingTheme
    let label: String
    @Binding var text: String
    var placeholder: String = ""
    var chevron: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(TealSheetChrome.label)
            HStack {
                TextField(placeholder, text: $text)
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                if chevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(OperatingV12.subtitle)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(theme.inset)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(TealSheetChrome.chipBorder.opacity(0.7), lineWidth: 1)
            )
        }
    }
}

struct TealReadRow: View {
    let theme: OperatingTheme
    let label: String
    let value: String
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(TealSheetChrome.label)
            Button {
                action?()
            } label: {
                HStack {
                    Text(value)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(OperatingV12.title)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if action != nil {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(OperatingV12.subtitle)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 12)
                .background(theme.inset)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(TealSheetChrome.chipBorder.opacity(0.7), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .disabled(action == nil)
        }
    }
}

struct TealMoreDetailsRow: View {
    let theme: OperatingTheme
    @Binding var open: Bool

    var body: some View {
        Button {
            open.toggle()
        } label: {
            HStack(spacing: 8) {
                Text("More details")
                    .font(.plusJakarta(size: 13, weight: .bold))
                    .foregroundStyle(TealSheetChrome.lime)
                Text(ServiceCaptureCopy.moreDetailsHint)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(OperatingV12.subtitle)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .lineLimit(1)
                Image(systemName: open ? "chevron.up" : "chevron.down")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(TealSheetChrome.lime)
            }
            .padding(12)
            .background(theme.inset.opacity(0.001))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.4), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

struct ServiceCaptureBody: View {
    let theme: OperatingTheme
    let mode: String
    @ObservedObject var model: BusinessOperatingModel
    @Binding var draft: OperatingCaptureSheet.Draft
    @Binding var moreDetailsOpen: Bool

    var body: some View {
        let upper = mode.uppercased()
        VStack(alignment: .leading, spacing: 12) {
            switch upper {
            case "SERVICE": finalizeCharge
            case "PURCHASE", "VENDOR": purchase
            case "COLLECTION": collection
            case "PAYMENT", "REIMBURSE": payment
            case "SPEND": expense
            case "STOCK": stock
            case "APPOINTMENT", "BOOKING": booking
            case "STATUS": status
            case "BUDGET", "TARGET": budget
            case "ISSUE", "UPDATE", "APPROVAL": issue
            default: EmptyView()
            }
            TealWhyBox(theme: theme, text: ServiceCaptureCopy.why(mode))
        }
    }

    // MARK: - QA-01 Finalize Charge

    private var finalizeCharge: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("SERVICE"),
                currency: model.currency,
                amount: $draft.amount,
                hint: draft.itemName.isEmpty ? ServiceCaptureCopy.amountHint("SERVICE") : draft.itemName
            )
            TealLabeledField(theme: theme, label: "Client", text: $draft.counterparty, placeholder: "Named client")
            TealLabeledField(theme: theme, label: "Service", text: $draft.itemName, placeholder: "Pick from Services")
            labeledChips(
                "Payment state",
                ["Collect now", "Part collect", "Pay later"],
                selection: paymentChoiceBinding(default: "Collect now")
            )
            if draft.paymentChoice.localizedCaseInsensitiveContains("Part") {
                TealLabeledField(theme: theme, label: "Collect now", text: $draft.amountPaidNow, placeholder: "Optional partial", chevron: false)
            }
            TealReadRow(
                theme: theme,
                label: "GST",
                value: "Unavailable · stamped on charge invoice only"
            )
            moreBlock
            TealReadRow(theme: theme, label: "Charged on", value: occurredLabel)
        }
    }

    // MARK: - QA-02 Purchase

    private var purchase: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("PURCHASE"),
                currency: model.currency,
                amount: $draft.amount,
                hint: draft.counterparty.isEmpty ? ServiceCaptureCopy.amountHint("PURCHASE") : draft.counterparty
            )
            TealLabeledField(theme: theme, label: "Supplier", text: $draft.counterparty, placeholder: "Required")
            labeledChips("Receive now", ["Full", "Partial", "Later"], selection: goodsBinding(default: "Full"))
            labeledChips("Payment state", ["Paid now", "Part paid", "Pay later"], selection: paymentChoiceBinding(default: "Pay later"))
            moreBlock
            TealReadRow(theme: theme, label: "Expected on", value: occurredLabel)
        }
    }

    // MARK: - QA-03 Collection

    private var collection: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("COLLECTION"),
                currency: model.currency,
                amount: $draft.amount,
                hint: draft.title.isEmpty ? ServiceCaptureCopy.amountHint("COLLECTION") : draft.title
            )
            TealLabeledField(theme: theme, label: "Client", text: $draft.counterparty, placeholder: "Named client")
            TealLabeledField(theme: theme, label: "Source", text: $draft.title, placeholder: "Open charge / package")
            labeledChips("Method", ["UPI", "Cash", "Card", "Bank transfer"], selection: methodBinding(default: "UPI"))
            moreBlock
            TealReadRow(theme: theme, label: "Received on", value: occurredLabel)
        }
    }

    // MARK: - QA-04 Payment

    private var payment: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("PAYMENT"),
                currency: model.currency,
                amount: $draft.amount,
                hint: draft.counterparty.isEmpty ? ServiceCaptureCopy.amountHint("PAYMENT") : draft.counterparty
            )
            TealLabeledField(theme: theme, label: "Payee", text: $draft.counterparty, placeholder: "Supplier / payee")
            TealLabeledField(theme: theme, label: "Source", text: $draft.title, placeholder: "Supplier invoice")
            labeledChips("Method", ["Bank transfer", "UPI", "Cash", "Card"], selection: methodBinding(default: "Bank transfer"))
            moreBlock
            TealReadRow(theme: theme, label: "Paid on", value: occurredLabel)
        }
    }

    // MARK: - QA-05 Expense

    private var expense: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("SPEND"),
                currency: model.currency,
                amount: $draft.amount,
                hint: draft.title.isEmpty ? ServiceCaptureCopy.amountHint("SPEND") : draft.title
            )
            TealLabeledField(theme: theme, label: "Type", text: $draft.title, placeholder: "Studio › category")
            TealLabeledField(theme: theme, label: "Payee", text: $draft.counterparty, placeholder: "Optional")
            labeledChips("Payment state", ["Paid now", "Part paid", "Pay later"], selection: paymentChoiceBinding(default: "Paid now"))
            moreBlock
            TealReadRow(theme: theme, label: "Period", value: occurredLabel)
        }
    }

    // MARK: - QA-06 Stock

    private var stock: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealLabeledField(theme: theme, label: "Supply", text: $draft.title, placeholder: "Consumable / SKU")
            TealLabeledField(theme: theme, label: "Location", text: $draft.channel, placeholder: "Room / store")
            TealLabeledField(theme: theme, label: "Quantity", text: $draft.quantity, placeholder: "Required", chevron: false)
            labeledChips(
                "Reason",
                ["Usage / count", "Stocktake", "Damage", "Waste", "Correction"],
                selection: Binding(
                    get: { draft.stockReason.isEmpty ? "Usage / count" : draft.stockReason },
                    set: { draft.stockReason = $0 }
                )
            )
            moreBlock
        }
    }

    // MARK: - QA-07 Booking

    private var booking: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("APPOINTMENT"),
                currency: model.currency,
                amount: $draft.amount,
                hint: ServiceCaptureCopy.amountHint("APPOINTMENT")
            )
            TealLabeledField(theme: theme, label: "Client", text: $draft.counterparty, placeholder: "Named client")
            TealLabeledField(theme: theme, label: "Service", text: $draft.itemName, placeholder: "Pick from Services")
            TealReadRow(theme: theme, label: "Slot", value: occurredLabel)
            TealLabeledField(theme: theme, label: "Pro", text: $draft.sku, placeholder: "Staff")
            labeledChips(
                "Status",
                ["Booked", "Confirmed", "Arrived", "No-show"],
                selection: paymentChoiceBinding(default: "Booked")
            )
            moreBlock
        }
    }

    // MARK: - QA-08 Status

    private var status: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealLabeledField(theme: theme, label: "Booking", text: $draft.title, placeholder: "Booking ref")
            labeledChips(
                "Status",
                ["In progress", "Completed", "Rescheduled", "No-show", "Cancelled"],
                selection: Binding(
                    get: {
                        if draft.goodsStatus.isEmpty {
                            switch draft.paymentChoice {
                            case "Complete": return "Completed"
                            case "Reschedule": return "Rescheduled"
                            default: return "In progress"
                            }
                        }
                        return draft.goodsStatus
                    },
                    set: { draft.goodsStatus = $0 }
                )
            )
            TealLabeledField(theme: theme, label: "Pro", text: $draft.sku, placeholder: "Staff")
            TealLabeledField(theme: theme, label: "Room", text: $draft.channel, placeholder: "Room / chair")
            moreBlock
            TealReadRow(theme: theme, label: "Due", value: occurredLabel)
        }
    }

    // MARK: - QA-09 Budget

    private var budget: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealAmountCard(
                theme: theme,
                label: ServiceCaptureCopy.amountLabel("BUDGET"),
                currency: model.currency,
                amount: $draft.amount,
                hint: draft.title.isEmpty ? ServiceCaptureCopy.amountHint("BUDGET") : draft.title
            )
            TealReadRow(theme: theme, label: "Period", value: occurredLabel)
            if draft.paymentChoice == "Sessions" {
                TealLabeledField(theme: theme, label: "Sessions target", text: $draft.quantity, placeholder: "Count", chevron: false)
            }
            TealLabeledField(theme: theme, label: "Previous", text: $draft.revisionKind, placeholder: "Prior version")
            moreBlock
        }
    }

    // MARK: - QA-10 Issue

    private var issue: some View {
        VStack(alignment: .leading, spacing: 12) {
            TealLabeledField(theme: theme, label: "Title", text: $draft.title, placeholder: "What needs attention")
            if draft.paymentChoice == "Issue" || draft.paymentChoice.isEmpty {
                labeledChips(
                    "Severity",
                    ["Info", "Alert", "Critical"],
                    selection: Binding(
                        get: { draft.goodsStatus.isEmpty ? "Alert" : draft.goodsStatus },
                        set: { draft.goodsStatus = $0 }
                    )
                )
            }
            TealLabeledField(theme: theme, label: "Owner", text: $draft.counterparty, placeholder: "Owner")
            TealLabeledField(theme: theme, label: "Link", text: $draft.itemName, placeholder: "Bookings / charge / purchase")
            moreBlock
        }
    }

    // MARK: - Shared

    private var moreBlock: some View {
        VStack(alignment: .leading, spacing: 10) {
            TealMoreDetailsRow(theme: theme, open: $moreDetailsOpen)
            if moreDetailsOpen {
                TealLabeledField(theme: theme, label: "Note", text: $draft.note, placeholder: "Optional", chevron: false)
            }
        }
    }

    private var occurredLabel: String {
        guard let raw = draft.occurredAt, !raw.isEmpty else { return "Pick date" }
        return raw
            .replacingOccurrences(of: "T", with: " · ")
            .prefix(16)
            .description
    }

    private func labeledChips(_ label: String, _ options: [String], selection: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.plusJakarta(size: 11, weight: .medium))
                .foregroundStyle(TealSheetChrome.label)
            TealChipRow(theme: theme, options: options, selection: selection)
        }
    }

    private func paymentChoiceBinding(default value: String) -> Binding<String> {
        Binding(
            get: { draft.paymentChoice.isEmpty ? value : draft.paymentChoice },
            set: { draft.paymentChoice = $0 }
        )
    }

    private func goodsBinding(default value: String) -> Binding<String> {
        Binding(
            get: { draft.goodsStatus.isEmpty ? value : draft.goodsStatus },
            set: { draft.goodsStatus = $0 }
        )
    }

    private func methodBinding(default value: String) -> Binding<String> {
        Binding(
            get: { draft.channel.isEmpty ? value : draft.channel },
            set: { draft.channel = $0 }
        )
    }
}
