import SwiftUI

/// Figma 2943:9075 — Create a moment · Guided business selection.
///
/// The Business top bar "+" opens this screen: pick a kind, read what the moment will include,
/// then Continue into the four-step setup wizard for that kind.
struct BusinessCreateMomentView: View {
    var onBack: () -> Void
    var onSelectSetup: (BusinessSetupKind) -> Void
    var companyId: String? = nil

    @State private var selected: BusinessSetupKind = .manufacturing

    private static let page = Color(hex: "#111113")
    private static let cardFill = Color(hex: "#1B1B1E")
    private static let cardStroke = Color.white.opacity(0.10)
    private static let emerald = Color(hex: "#10B981")
    private static let heroFill = Color(hex: "#18181B")
    private static let heroStroke = Color(hex: "#053B29")
    private static let textPrimary = Color(hex: "#F1F5F9")
    private static let textSecondary = Color(hex: "#A7A7AE")
    private static let ctaInk = Color(hex: "#0E0E0E")

    private struct Choice: Identifiable {
        var id: String { kind.rawValue }
        let kind: BusinessSetupKind
        let title: String
        let caption: String
        let icon: BusinessIcon
        let eyebrow: String
        let promise: String
        let description: String
        let capabilities: [(BusinessIcon, String)]
    }

    private static let choices: [Choice] = [
        Choice(
            kind: .marketing,
            title: "Marketing & Campaigns",
            caption: "Ads, promotions & leads",
            icon: .megaphone,
            eyebrow: "YOUR MARKETING MOMENT",
            promise: "Built for the way you promote.",
            description: "Plan campaigns, track leads, and keep every approval in one place.",
            capabilities: [
                (.wallet, "Campaign budgets"),
                (.target, "Lead tracking"),
                (.users, "Client retainers"),
                (.badgeCheck, "Creative approvals"),
            ]
        ),
        Choice(
            kind: .trading,
            title: "Trading & Distribution",
            caption: "Bulk buying & selling",
            icon: .truck,
            eyebrow: "YOUR TRADING MOMENT",
            promise: "Built for the way you trade.",
            description: "Buy in bulk, sell across counters, and keep stock, dispatch and collections in step.",
            capabilities: [
                (.package, "Purchase & stock"),
                (.truck, "Sales & dispatch"),
                (.wallet, "Collections & ledger"),
                (.route, "Shipments & tracking"),
            ]
        ),
        Choice(
            kind: .retail,
            title: "Retail & Consumer",
            caption: "Shop & counter sales",
            icon: .store,
            eyebrow: "YOUR RETAIL MOMENT",
            promise: "Built for the way you sell.",
            description: "Run counter sales, keep stock honest, and close the day with cash that matches.",
            capabilities: [
                (.store, "POS / counter sales"),
                (.package, "Inventory & stock"),
                (.users, "Customer tabs"),
                (.receipt, "Daily cash close"),
            ]
        ),
        Choice(
            kind: .manufacturing,
            title: "Manufacturing & Production",
            caption: "Batch & job work",
            icon: .factory,
            eyebrow: "YOUR MANUFACTURING MOMENT",
            promise: "Built for the way you make.",
            description: "Make products in batches from raw materials or manage made-to-order job work.",
            capabilities: [
                (.clipboardList, "Quotes & job orders"),
                (.factory, "Production & WIP"),
                (.layers, "BOM & materials"),
                (.receiptText, "Invoices against orders"),
            ]
        ),
    ]

    private var choice: Choice {
        Self.choices.first { $0.kind == selected } ?? Self.choices[3]
    }

    var body: some View {
        VStack(spacing: 0) {
            header

            ScrollView {
                VStack(spacing: 0) {
                    question
                    preview
                }
            }

            footer
        }
        .background(Self.page.ignoresSafeArea())
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 0) {
            MomentraWordmark(showTagline: false, titleSize: 21, alignStart: true)
            Spacer(minLength: 0)
            Button(action: onBack) {
                Image("biz_create_x")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 12, height: 12)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close creation")
        }
        .frame(height: 52)
        .padding(.horizontal, 24)
    }

    // MARK: - Question + grid

    private var question: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 12) {
                Text("What kind of\nbusiness do you run?")
                    .font(.plusJakarta(size: 32, weight: .semibold))
                    .foregroundStyle(Self.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text("Choose a type. We’ll tailor your moment.")
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(Self.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(spacing: 10) {
                ForEach(Array(choiceRows.enumerated()), id: \.offset) { pair in
                    HStack(spacing: 10) {
                        ForEach(pair.element) { item in
                            choiceCard(item)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .padding(.bottom, 20)
    }

    /// The design pairs the four kinds 2×2: marketing/trading, then retail/manufacturing.
    private var choiceRows: [[Choice]] {
        stride(from: 0, to: Self.choices.count, by: 2).map {
            Array(Self.choices[$0..<min($0 + 2, Self.choices.count)])
        }
    }

    private func choiceCard(_ item: Choice) -> some View {
        let isSelected = item.kind == selected
        return Button {
            selected = item.kind
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 0) {
                    item.icon.image
                        .resizable()
                        .scaledToFit()
                        .frame(width: 19, height: 19)
                        .foregroundStyle(isSelected ? Self.emerald : Self.textSecondary)
                    Spacer(minLength: 0)
                    SelectionIndicator(isSelected: isSelected)
                }
                .frame(height: 20)

                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(Self.textPrimary)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(item.caption)
                        .font(.plusJakarta(size: 11))
                        .foregroundStyle(Self.textSecondary)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .frame(maxWidth: .infinity, minHeight: 126, maxHeight: 126, alignment: .topLeading)
            .background(isSelected ? Self.emerald.opacity(0.07) : Self.cardFill)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(isSelected ? Self.emerald : Self.cardStroke, lineWidth: isSelected ? 1.5 : 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(item.kind.maestroTag)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
        .accessibilityLabel("\(item.title). \(item.caption)")
    }

    // MARK: - Preview

    private var preview: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 8) {
                Text(choice.eyebrow)
                    .font(.plusJakarta(size: 10, weight: .semibold))
                    .tracking(1)
                    .foregroundStyle(Self.emerald)
                Text(choice.promise)
                    .font(.plusJakarta(size: 20, weight: .semibold))
                    .foregroundStyle(Self.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(choice.description)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(Self.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Rectangle()
                .fill(Color.white.opacity(0.10))
                .frame(height: 1)

            VStack(spacing: 8) {
                ForEach(0..<2, id: \.self) { row in
                    HStack(spacing: 16) {
                        ForEach(0..<2, id: \.self) { column in
                            let capability = choice.capabilities[row * 2 + column]
                            HStack(alignment: .top, spacing: 8) {
                                capability.0.image
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 16, height: 16)
                                    .foregroundStyle(Self.emerald)
                                Text(capability.1)
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(Self.textPrimary)
                                    .fixedSize(horizontal: false, vertical: true)
                                Spacer(minLength: 0)
                            }
                            .frame(maxWidth: .infinity, minHeight: 40, alignment: .leading)
                        }
                    }
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Self.heroFill)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Self.heroStroke, lineWidth: 1))
        .padding(.horizontal, 24)
    }

    // MARK: - Footer

    private var footer: some View {
        VStack(spacing: 12) {
            Button {
                onSelectSetup(choice.kind)
            } label: {
                Text("Continue")
                    .font(.plusJakarta(size: 17, weight: .semibold))
                    .foregroundStyle(Self.ctaInk)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Self.emerald)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Continue")

            Text("You can change this later in Manage moment.")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(Self.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .padding(.bottom, 10)
    }
}

/// Unselected is a 17pt hairline ring; selected is an 18pt emerald disc with a dark check.
private struct SelectionIndicator: View {
    let isSelected: Bool

    var body: some View {
        if isSelected {
            ZStack {
                Circle()
                    .fill(Color(hex: "#10B981"))
                Image(systemName: "checkmark")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(Color(hex: "#0E0E0E"))
            }
            .frame(width: 18, height: 18)
        } else {
            Circle()
                .stroke(Color.white.opacity(0.35), lineWidth: 1.5)
                .frame(width: 17, height: 17)
        }
    }
}
