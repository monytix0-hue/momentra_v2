import SwiftUI

struct TradingHowItWorksCopy {
    let context: String
    let intro: String
    let cards: [(title: String, happened: String, shows: String)]
    let snapshotTitle: String
    let metrics: [String]
    let closing: String
    let cta: String

    static func forSubtype(_ code: String) -> TradingHowItWorksCopy? {
        switch code {
        case "TRADING_WHOLESALE":
            return TradingHowItWorksCopy(
                context: "Bring your orders, stock, deliveries and payments together.",
                intro: "Imagine you buy products from suppliers and sell them to customers. Some goods arrive today, others later. Some customers pay now, others after delivery.",
                cards: [
                    ("You order", "You order 100 boxes from a supplier.", "100 boxes expected."),
                    ("Goods arrive", "Only 60 boxes arrive.", "40 boxes still to receive."),
                    ("You sell", "A customer orders 40 boxes.", "40 boxes committed to the customer."),
                    ("You deliver", "You deliver 25 boxes.", "15 boxes still to deliver."),
                    ("You collect", "Your customer pays ₹10,000 now.", "The remaining amount stays visible until collected."),
                    ("You pay", "You make a payment to your supplier.", "What you still owe updates."),
                ],
                snapshotTitle: "Your trading at a glance",
                metrics: ["40 boxes to receive", "15 boxes to deliver", "Collections to follow up"],
                closing: "Know what is bought, sold, delivered and still pending.",
                cta: "Create my Trading Moment"
            )
        case "DISTRIBUTION_DEALERSHIP":
            return TradingHowItWorksCopy(
                context: "Keep your retailer orders, stock, deliveries and collections connected.",
                intro: "Imagine you supply products to several retailers. Orders come from different shops, deliveries happen in parts, and payments arrive on different days.",
                cards: [
                    ("You order", "You order 500 cases from a supplier.", "500 cases expected."),
                    ("Stock arrives", "400 cases arrive first.", "100 cases still to receive."),
                    ("Retailers order", "Three retailers order 300 cases in total.", "Orders committed across your retailers."),
                    ("You dispatch", "You deliver 200 cases against those sales.", "100 cases still to deliver."),
                    ("Collections arrive", "Two retailers make part payments.", "Outstanding collections by retailer."),
                    ("Supplier payment", "A supplier payment is due next week.", "The upcoming payment needs attention."),
                ],
                snapshotTitle: "Your distribution at a glance",
                metrics: ["100 cases to receive", "100 cases to deliver", "3 retailer orders"],
                closing: "Know what is moving, what is pending and who needs attention.",
                cta: "Create my Distribution Moment"
            )
        default:
            return nil
        }
    }
}

struct TradingHowItWorksSheet: View {
    let copy: TradingHowItWorksCopy
    let accent: Color
    let ink: Color
    let actionInk: Color
    var onExpand: () -> Void
    var onCreate: () -> Void
    var onClose: () -> Void

    @State private var expanded = false

    private let places: [(String, String)] = [
        ("Pulse", "What needs your attention"),
        ("Moments", "Your orders, stock and commitments"),
        ("Life", "What is happening across your business"),
        ("Memory", "How your business has progressed"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("See how your business comes together")
                        .font(.plusJakarta(size: 22, weight: .semibold))
                        .foregroundStyle(ink)
                        .accessibilityAddTraits(.isHeader)
                    Text("An order comes in. Stock arrives. Goods go out. Payments follow. Momentra helps you see what happened and what still needs attention.")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(ink.opacity(0.72))
                        .fixedSize(horizontal: false, vertical: true)
                    Text("Example only · Not your business data")
                        .font(.plusJakarta(size: 12, weight: .semibold))
                        .foregroundStyle(accent)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(accent.opacity(0.16))
                        .clipShape(Capsule())
                    Text(copy.intro)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(ink.opacity(0.84))
                        .fixedSize(horizontal: false, vertical: true)
                    Text("A simple example")
                        .font(.plusJakarta(size: 16, weight: .semibold))
                        .foregroundStyle(ink)
                    ForEach(Array(copy.cards.prefix(expanded ? 6 : 4).enumerated()), id: \.offset) { index, card in
                        journeyCard(index + 1, card)
                    }
                    Button(expanded ? "Show less" : "See the complete journey") {
                        expanded.toggle()
                        if expanded { onExpand() }
                    }
                    .font(.plusJakarta(size: 14, weight: .semibold))
                    .foregroundStyle(accent)
                    .accessibilityLabel(expanded ? "Show less" : "See the complete journey")
                    Text(copy.snapshotTitle)
                        .font(.plusJakarta(size: 16, weight: .semibold))
                        .foregroundStyle(ink)
                    Text(copy.metrics.joined(separator: "  ·  "))
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(ink)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("Sample figures for illustration. Your actual Moment will show your own business activity.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(ink.opacity(0.64))
                        .fixedSize(horizontal: false, vertical: true)
                    Text("You record what happens. Momentra connects what comes next.")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(ink)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("Start with any activity. You do not need an order before recording a sale or purchase.")
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(ink.opacity(0.72))
                        .fixedSize(horizontal: false, vertical: true)
                    Text(copy.closing)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(ink)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("Once you start, Momentra brings the picture together.")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(ink)
                    ForEach(places, id: \.0) { place in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(place.0).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(ink)
                            Text(place.1).font(.plusJakarta(size: 13)).foregroundStyle(ink.opacity(0.72))
                        }
                        .accessibilityElement(children: .combine)
                    }
                }
                .padding(20)
            }
            VStack(spacing: 8) {
                Button(copy.cta, action: onCreate)
                    .font(.plusJakarta(size: 16, weight: .semibold))
                    .foregroundStyle(actionInk)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                    .accessibilityLabel(copy.cta)
                Button("Close", action: onClose)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(ink.opacity(0.72))
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: 44)
                    .accessibilityLabel("Close")
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
        .background(Color(hex: "#122235"))
    }

    private func journeyCard(_ number: Int, _ card: (title: String, happened: String, shows: String)) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("\(number) · \(card.title)")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(ink)
            Text(card.happened)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(ink.opacity(0.84))
                .fixedSize(horizontal: false, vertical: true)
            Text(card.shows)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(accent)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color.white.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .combine)
    }
}
