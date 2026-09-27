import SwiftUI

/// Figma: create-empty-b (1687:20701). Receipt tile uses the people glyph from the design.
struct BusinessCreateEmptyView: View {
    var onStartCta: () -> Void

    private let tiles: [(String, String, String)] = [
        ("business_empty_file_text", "Add Purchase", "Something you bought for your business"),
        ("business_empty_dollar", "Add Expense", "Money spent on daily things"),
        ("business_empty_users", "Add Receipt", "Save the paper slip"),
        ("business_empty_folder", "Add Bill", "Money you need to pay"),
        ("business_empty_truck", "Add Supplier", "The person you buy from"),
        ("business_empty_bar_chart", "View Summary", "See how much you spent"),
    ]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "CREATE")
                    BusinessEmptyHeadline(
                        title: "Add a Purchase or Expense",
                        bodyText: "Keep track of money in and out. Add a receipt, bill, or anything you bought for your business."
                    )

                    VStack(spacing: 12) {
                        ForEach(0..<3, id: \.self) { row in
                            HStack(spacing: 12) {
                                tileView(tiles[row * 2])
                                tileView(tiles[row * 2 + 1])
                            }
                        }
                    }

                    Text("Built for every business")
                        .font(.system(size: 14))
                        .foregroundStyle(BusinessEmptyTokens.textMuted)
                        .multilineTextAlignment(.center)

                    BusinessEmptyCTA(label: "Add Your First Activity →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
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
