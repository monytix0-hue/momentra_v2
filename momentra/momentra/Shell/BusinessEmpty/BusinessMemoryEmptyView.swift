import SwiftUI

/// Figma: memory-empty-b (657:10208)
struct BusinessMemoryEmptyView: View {
    var onStartCta: () -> Void

    private let rows = [
        "Spending Patterns",
        "Revenue Forecasts",
        "Operational Trends",
    ]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "MEMORY")
                    BusinessEmptyHeadline(
                        title: "Intelligence That Compounds",
                        bodyText: "AI-powered pattern recognition across spending, performance, and operations."
                    )

                    VStack(spacing: 8) {
                        ForEach(rows, id: \.self) { title in
                            HStack {
                                HStack(spacing: 8) {
                                    BusinessEmptyAssetIcon(name: "business_empty_memory_dot", size: 6)
                                    Text(title)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundStyle(BusinessEmptyTokens.textPrimary)
                                }
                                Spacer()
                                BusinessEmptyAssetImage(name: "business_empty_sparkline", width: 60, height: 20)
                            }
                            .padding(14)
                            .background(BusinessEmptyTokens.cardFill)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    Text("“An enterprise with a memory is an enterprise with an unfair advantage.”")
                        .font(.system(size: 13).italic())
                        .foregroundStyle(BusinessEmptyTokens.textSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)

                    BusinessEmptyCTA(label: "Activate Memory →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }
}
