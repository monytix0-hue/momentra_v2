import SwiftUI

/// Figma: memory-empty-b (1687:20818)
struct BusinessMemoryEmptyView: View {
    var onStartCta: () -> Void

    private let rows = [
        "Old receipts",
        "Past expenses",
        "Business records",
    ]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "MEMORY")
                    BusinessEmptyHeadline(
                        title: "Remember What Happened in Your Business",
                        bodyText: "Find past activity and let Momentra learn from your business over time."
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

                    Text("\"Everything stays here so you can find it later.\"")
                        .font(.system(size: 13).italic())
                        .foregroundStyle(BusinessEmptyTokens.textSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)

                    BusinessEmptyCTA(label: "Start Building Business Memory →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }
}
