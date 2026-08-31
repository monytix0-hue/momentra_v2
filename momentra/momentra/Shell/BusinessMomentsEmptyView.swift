import SwiftUI

/// Figma: moments-empty-b (657:10043)
struct BusinessMomentsEmptyView: View {
    var onStartCta: () -> Void

    private let timeline: [(String, String)] = [
        ("Operational Milestone Reached", "Today, 10:42 AM"),
        ("Strategic Seed Round Confirmed", "Oct 14, 2024"),
        ("Inception & Core Architecture Setup", "Sep 01, 2024"),
    ]

    private let chips = ["Decisions", "Revenue", "Partnerships", "Team", "Growth"]

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                BusinessEmptyPill(label: "MOMENTS")
                BusinessEmptyHeadline(
                    title: "Every Decision. Documented.",
                    bodyText: "Capture milestones, wins, and pivotal moments that define your business story."
                )

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
                                Text(item.0)
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(BusinessEmptyTokens.textPrimary)
                                Text(item.1)
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

                VStack(spacing: 6) {
                    HStack(spacing: 6) {
                        ForEach(chips.prefix(4), id: \.self, content: chipView)
                    }
                    chipView(chips[4])
                }
                .frame(maxWidth: .infinity)

                BusinessEmptyCTA(label: "Record First Moment →", action: onStartCta)
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .padding(.bottom, 40)
        }
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
