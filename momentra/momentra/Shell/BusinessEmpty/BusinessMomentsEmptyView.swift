import SwiftUI

/// Figma: moments-empty-b (1687:20652)
struct BusinessMomentsEmptyView: View {
    var onStartCta: () -> Void

    private let timeline = BusinessNoMomentEmptyCopy.momentsSampleRows

    private let chips = ["Purchases", "Expenses", "Receipts", "Activity"]

    var body: some View {
        NativeDashboardScaffold(background: BusinessSheetTheme.bg) {
            NativeListSection(insets: EdgeInsets(top: 24, leading: 24, bottom: 40, trailing: 24)) {
                VStack(spacing: 24) {
                    BusinessEmptyPill(label: "MOMENTS")
                    BusinessEmptyHeadline(
                        title: "See what happened in your business",
                        bodyText: "Keep purchases, expenses, receipts and important activity together."
                    )

                    if !timeline.isEmpty {
                    VStack(alignment: .leading, spacing: 0) {
                        ForEach(Array(timeline.enumerated()), id: \.offset) { index, item in
                            HStack(alignment: .top, spacing: 16) {
                                VStack(spacing: 0) {
                                    BusinessEmptyAssetIcon(name: "business_empty_timeline_dot", size: 10)
                                    if index < timeline.count - 1 {
                                        // Figma export is 44×1 horizontal; rotate for vertical rail.
                                        BusinessEmptyAssetImage(name: "business_empty_timeline_line", width: 44, height: 1)
                                            .rotationEffect(.degrees(90))
                                            .frame(width: 1, height: 44)
                                    }
                                }
                                .frame(width: 10)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.title)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundStyle(BusinessEmptyTokens.textPrimary)
                                    Text(item.time)
                                        .font(.system(size: 11, design: .monospaced))
                                        .foregroundStyle(BusinessEmptyTokens.textMuted)
                                }
                                .padding(.bottom, index < timeline.count - 1 ? 16 : 0)
                                Spacer(minLength: 0)
                            }
                        }
                    }
                    .padding(20)
                    .background(BusinessEmptyTokens.cardFill)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    }

                    HStack(spacing: 6) {
                        ForEach(chips, id: \.self, content: chipView)
                    }
                    .frame(maxWidth: .infinity)

                    BusinessEmptyCTA(label: "Add your first moment →", action: onStartCta)
                }
            }
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }

    private func chipView(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .medium))
            .foregroundStyle(BusinessEmptyTokens.textSecondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(BusinessEmptyTokens.cardFill)
            .overlay(Capsule().stroke(BusinessEmptyTokens.cardStroke, lineWidth: 1))
            .clipShape(Capsule())
    }
}
