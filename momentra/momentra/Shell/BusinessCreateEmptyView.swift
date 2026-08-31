import SwiftUI

/// Figma: create-empty-b (657:10100)
struct BusinessCreateEmptyView: View {
    var onStartCta: () -> Void

    private let tiles: [(String, String, String)] = [
        ("business_empty_file_text", "Create Invoice", "Bill clients professionally"),
        ("business_empty_dollar", "Log Expense", "Track every rupee spent"),
        ("business_empty_users", "Add Team Member", "Grow your operations team"),
        ("business_empty_folder", "New Project", "Organize work by project"),
        ("business_empty_truck", "Add Vendor", "Manage supply chain"),
        ("business_empty_bar_chart", "Generate Report", "Data-driven business insights"),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                BusinessEmptyPill(label: "CREATE")
                BusinessEmptyHeadline(
                    title: "Your Command Center",
                    bodyText: "Invoices, expenses, vendors, projects - every business action in one place."
                )

                VStack(spacing: 12) {
                    ForEach(0..<3, id: \.self) { row in
                        HStack(spacing: 12) {
                            tileView(tiles[row * 2])
                            tileView(tiles[row * 2 + 1])
                        }
                    }
                }

                Text("From solo founders to scaling teams")
                    .font(.system(size: 12))
                    .foregroundStyle(BusinessEmptyTokens.textMuted)
                    .multilineTextAlignment(.center)

                BusinessEmptyCTA(label: "First Action →", action: onStartCta)
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 40)
        }
        .businessEmptyAppear()
    }

    private func tileView(_ tile: (String, String, String)) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(BusinessEmptyTokens.iconWell)
                    .frame(width: 36, height: 32)
                BusinessEmptyAssetIcon(name: tile.0, size: 18)
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(tile.1)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(BusinessEmptyTokens.textPrimary)
                Text(tile.2)
                    .font(.system(size: 11))
                    .foregroundStyle(BusinessEmptyTokens.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: 110)
        .background(BusinessEmptyTokens.cardFill)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
