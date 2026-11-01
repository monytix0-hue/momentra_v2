import SwiftUI

/// Create Business Story — Figma `RST-STORY-CONFIG` `2671:32352`.
///
/// Ported from `RetailStoryConfigSheet` onto the ember palette. `POST /v1/moments/:momentId/stories`
/// takes no body, so period / detail / GST controls cannot be honoured. They stay visible
/// (the frame shows them) but disabled with the reason stated, instead of silently doing nothing.
struct RestaurantStoryConfigSheet: View {
    let model: OperatingProjectionPayload
    let momentId: String
    var onDismiss: () -> Void

    @State private var generating = false
    @State private var errorText: String?

    private var venueName: String {
        let title = (model.title ?? "").trimmingCharacters(in: .whitespaces)
        return title.isEmpty ? "Restaurant" : title
    }

    private var monthLabel: String { RestaurantFormat.month(model.startDate) }

    private var netSales: String {
        RestaurantFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
    }

    var body: some View {
        EmberScreenBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Spacer(minLength: 0)
                        Button { onDismiss() } label: {
                            Text("✕")
                                .font(.plusJakarta(size: 14, weight: .bold))
                                .foregroundStyle(RestaurantEmber.textBody)
                                .frame(width: 36, height: 36)
                                .background(RestaurantEmber.chip)
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Close")
                    }

                    previewThumb

                    Text("Your \(monthLabel.components(separatedBy: " ").first ?? monthLabel) story is ready to compose")
                        .font(.plusJakarta(size: 15, weight: .medium))
                        .foregroundStyle(RestaurantEmber.textBody)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity)

                    sheetBody
                    Spacer(minLength: 12)
                }
                .padding(.horizontal, RestaurantEmber.gutter)
                .padding(.vertical, 12)
            }
        }
        .presentationDragIndicator(.hidden)
    }

    /// 140 × 300 story thumb: 8-segment progress, cover eyebrow, hero metric, caption.
    private var previewThumb: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient(
                colors: [Color(hex: "#3A1A14"), RestaurantEmber.screenBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 3) {
                    ForEach(0..<8, id: \.self) { index in
                        Capsule()
                            .fill(index == 0 ? RestaurantEmber.ember : Color.white.opacity(0.22))
                            .frame(height: 2)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.top, 10)

                Spacer(minLength: 0)

                VStack(alignment: .leading, spacing: 2) {
                    Text("THE BUSINESS MOMENT")
                        .font(.plusJakarta(size: 6, weight: .semibold))
                        .tracking(1)
                        .foregroundStyle(RestaurantEmber.gold)
                        .lineLimit(1)
                    Text(netSales)
                        .font(.plusJakarta(size: 34, weight: .bold))
                        .foregroundStyle(RestaurantEmber.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text("net sales · \(monthLabel)")
                        .font(.plusJakarta(size: 7, weight: .medium))
                        .foregroundStyle(RestaurantEmber.textBody)
                        .lineLimit(1)
                }
                .padding(.horizontal, 8)

                Spacer(minLength: 0)

                Capsule()
                    .fill(Color.white.opacity(0.6))
                    .frame(width: 46, height: 6)
                    .padding(.horizontal, 8)
                    .padding(.bottom, 12)
            }
        }
        .frame(width: 140, height: 300)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(RestaurantEmber.ember.opacity(0.35), lineWidth: 1)
        )
        .shadow(color: RestaurantEmber.ember.opacity(0.35), radius: 24, x: 0, y: 12)
        .frame(maxWidth: .infinity)
    }

    private var sheetBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            Capsule()
                .fill(RestaurantEmber.ember.opacity(0.55))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: 2) {
                Text("Create Business Story")
                    .font(.plusJakarta(size: 20, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                Text("\(venueName) · 8 slides")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RestaurantEmber.textBody)
            }

            // Period — the story always covers the moment's recorded window.
            VStack(alignment: .leading, spacing: 8) {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 64), spacing: 8)], alignment: .leading, spacing: 8) {
                    ForEach(Self.periods, id: \.self) { label in
                        let selected = label == monthLabel
                        Text(label)
                            .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? Color.white : RestaurantEmber.textFaint)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? RestaurantEmber.ember : RestaurantEmber.chip)
                            .clipShape(Capsule())
                    }
                }
                Text("Period follows the moment's recorded window — regenerate covers that range only.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RestaurantEmber.textFaint)
                    .fixedSize(horizontal: false, vertical: true)
            }

            // Detail level — one composition is produced today.
            HStack(spacing: 4) {
                segment("Summary", selected: true)
                segment("Detailed", selected: false)
            }
            .padding(4)
            .background(RestaurantEmber.field)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            if model.gstTracking == "YES" {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Include GST snapshot")
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(RestaurantEmber.textFaint)
                        Text("Recorded GST is not part of this story build")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(RestaurantEmber.textFaint)
                    }
                    Spacer(minLength: 0)
                    Capsule()
                        .fill(Color.white.opacity(0.14))
                        .frame(width: 44, height: 24)
                        .overlay(alignment: .leading) {
                            Circle()
                                .fill(RestaurantEmber.textFaint)
                                .frame(width: 18, height: 18)
                                .padding(3)
                        }
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Include GST snapshot, not available")
            }

            Button { Task { await generate() } } label: {
                HStack(spacing: 8) {
                    if generating {
                        ProgressView().tint(.white).scaleEffect(0.8)
                    }
                    Image(RestaurantTileIcons.plus)
                        .renderingMode(.template)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                        .foregroundStyle(.white)
                    Text(generating ? "Composing…" : "Generate story")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(RestaurantEmber.ember)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: RestaurantEmber.ember.opacity(0.45), radius: 14, x: 0, y: 8)
            }
            .buttonStyle(.plain)
            .disabled(generating)

            if let errorText {
                Text(errorText)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RestaurantEmber.negative)
                    .fixedSize(horizontal: false, vertical: true)
            }

            EmberWhyNote(
                body: "Every slide is built from recorded activity. Nothing is estimated, and an empty section says so.",
                tinted: true
            )
        }
        .padding(16)
        .background(RestaurantEmber.card)
        .clipShape(UnevenRoundedRectangle(
            topLeadingRadius: 22,
            bottomLeadingRadius: 0,
            bottomTrailingRadius: 0,
            topTrailingRadius: 22,
            style: .continuous
        ))
    }

    private func segment(_ label: String, selected: Bool) -> some View {
        Text(label)
            .font(.plusJakarta(size: 14, weight: selected ? .bold : .medium))
            .foregroundStyle(selected ? RestaurantEmber.textPrimary : RestaurantEmber.textFaint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(selected ? RestaurantEmber.chip : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private static let periods = ["This week", "This month", "Last month", "Quarter", "Year", "Custom"]

    private func generate() async {
        generating = true
        errorText = nil
        do {
            try await APIClient.shared.createMomentStory(momentId: momentId)
            OperatingChromeState.shared.previewStoryId = momentId
            onDismiss()
        } catch {
            errorText = error.localizedDescription
        }
        generating = false
    }
}
