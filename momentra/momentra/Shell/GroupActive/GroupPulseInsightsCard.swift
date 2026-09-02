import SwiftUI

/// Live group pulse insights from `GET /v1/analytics/insights` (scopeType=MOMENT).
struct GroupPulseInsightsHeroCard: View {
    let headerTitle: String
    let insights: [AnalyticsInsightItemPayload]
    let gradient: LinearGradient
    var titleColor: Color = .white
    var bodyColor: Color = Color.white.opacity(0.9)
    var footerLabel: String?
    var footerAction: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(headerTitle)
                .font(.plusJakarta(size: 18, weight: .heavy))
                .foregroundStyle(titleColor)

            if insights.isEmpty {
                Text("No insights yet")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(titleColor)
                Text("Insights appear when there's enough group activity — nothing is invented.")
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(bodyColor)
            } else {
                ForEach(Array(insights.prefix(3)), id: \.insightId) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        if let title = item.title, !title.isEmpty {
                            Text(title)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(titleColor)
                        }
                        if let body = item.body, !body.isEmpty {
                            Text(body)
                                .font(.plusJakarta(size: 12))
                                .foregroundStyle(bodyColor)
                        }
                    }
                }
            }

            if let footerLabel, let footerAction {
                Button(action: footerAction) {
                    Text(footerLabel)
                        .font(.plusJakarta(size: 14, weight: .bold))
                        .foregroundStyle(Color(hex: "#131313"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(gradient)
        .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

struct GroupPulseInsightsSectionCard: View {
    let insights: [AnalyticsInsightItemPayload]

    var body: some View {
        GroupSectionCard(title: "Momentra Insights") {
            if insights.isEmpty {
                GroupEmptySection(
                    message: "No insights yet",
                    detail: "Insights appear when there's enough group activity — nothing is invented."
                )
            } else {
                VStack(alignment: .leading, spacing: 12) {
                    ForEach(Array(insights.prefix(3)), id: \.insightId) { item in
                        VStack(alignment: .leading, spacing: 4) {
                            if let title = item.title, !title.isEmpty {
                                Text(title)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(GroupActiveTheme.text)
                            }
                            if let body = item.body, !body.isEmpty {
                                Text(body)
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(GroupActiveTheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
