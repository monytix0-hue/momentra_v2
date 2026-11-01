import SwiftUI

/// Create Business Story — Figma `RTL-STORY-CONFIG · IG · Create story` `2670:33783`.
///
/// The story endpoint regenerates the moment's recorded window and has no request body,
/// so period / detail / GST controls cannot be honoured. They stay visible (the frame
/// shows them) but disabled with the reason stated, instead of silently doing nothing.
struct RetailStoryConfigSheet: View {
    let model: OperatingProjectionPayload
    let momentId: String
    var onDismiss: () -> Void

    @State private var generating = false
    @State private var errorText: String?

    private var storeName: String {
        let title = (model.title ?? "").trimmingCharacters(in: .whitespaces)
        return title.isEmpty ? "Retail Store" : title
    }

    private var monthLabel: String { RetailFormat.month(model.startDate) }

    private var netSales: String {
        RetailFormat.compact(model.finance?.sales, currency: model.currency, visible: model.financeVisible == true)
    }

    var body: some View {
        RoseScreenBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Spacer()
                        Button { onDismiss() } label: {
                            Text("✕")
                                .font(.plusJakarta(size: 14, weight: .bold))
                                .foregroundStyle(RetailRose.textBody)
                                .frame(width: 36, height: 36)
                                .background(RetailRose.chip)
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Close")
                    }

                    previewThumb

                    Text("Your \(monthLabel.components(separatedBy: " ").first ?? monthLabel) story is ready to compose")
                        .font(.plusJakarta(size: 15, weight: .medium))
                        .foregroundStyle(RetailRose.textBody)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity)

                    sheetBody
                    Spacer(minLength: 12)
                }
                .padding(.horizontal, RetailRose.gutter)
                .padding(.vertical, 12)
            }
        }
        .presentationDragIndicator(.hidden)
    }

    /// 140 × 300 story thumb: 8-segment progress, cover eyebrow, hero metric, caption.
    private var previewThumb: some View {
        ZStack(alignment: .topLeading) {
            LinearGradient(
                colors: [Color(hex: "#2A1A1F"), Color(hex: "#0B0A1F")],
                startPoint: .top,
                endPoint: .bottom
            )
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 3) {
                    ForEach(0..<8, id: \.self) { index in
                        Capsule()
                            .fill(index == 0 ? RetailRose.rose : Color.white.opacity(0.22))
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
                        .foregroundStyle(RetailRose.rose)
                        .lineLimit(1)
                    Text(netSales)
                        .font(.plusJakarta(size: 34, weight: .bold))
                        .foregroundStyle(RetailRose.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text("net sales · \(monthLabel)")
                        .font(.plusJakarta(size: 7, weight: .medium))
                        .foregroundStyle(RetailRose.textBody)
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
                .stroke(RetailRose.rose.opacity(0.35), lineWidth: 1)
        )
        .shadow(color: RetailRose.rose.opacity(0.35), radius: 24, x: 0, y: 12)
        .frame(maxWidth: .infinity)
    }

    private var sheetBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            Capsule()
                .fill(RetailRose.roseMuted)
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)

            VStack(alignment: .leading, spacing: 2) {
                Text("Create Business Story")
                    .font(.plusJakarta(size: 20, weight: .bold))
                    .foregroundStyle(RetailRose.textPrimary)
                Text("\(storeName) · 8 slides")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RetailRose.textBody)
            }

            // Period — the story always covers the moment's recorded window.
            VStack(alignment: .leading, spacing: 8) {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 64), spacing: 8)], alignment: .leading, spacing: 8) {
                    ForEach(Self.periods, id: \.self) { label in
                        let selected = label == monthLabel
                        Text(label)
                            .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? Color.white : RetailRose.textFaint)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selected ? RetailRose.rose : RetailRose.chip)
                            .clipShape(Capsule())
                    }
                }
                Text("Period follows the moment's recorded window — regenerate covers that range only.")
                    .font(.plusJakarta(size: 10))
                    .foregroundStyle(RetailRose.textFaint)
                    .fixedSize(horizontal: false, vertical: true)
            }

            // Detail level — one composition is produced today.
            HStack(spacing: 4) {
                segment("Summary", selected: true)
                segment("Detailed", selected: false)
            }
            .padding(4)
            .background(RetailRose.field)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            if model.gstTracking == "YES" {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Include GST snapshot")
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(RetailRose.textFaint)
                        Text("Recorded GST is not part of this story build")
                            .font(.plusJakarta(size: 10))
                            .foregroundStyle(RetailRose.textFaint)
                    }
                    Spacer(minLength: 0)
                    Capsule()
                        .fill(Color.white.opacity(0.14))
                        .frame(width: 44, height: 24)
                        .overlay(alignment: .leading) {
                            Circle()
                                .fill(RetailRose.textFaint)
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
                    Image("RetailPlus")
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
                .background(RetailRose.rose)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: RetailRose.rose.opacity(0.45), radius: 14, x: 0, y: 8)
            }
            .buttonStyle(.plain)
            .disabled(generating)

            if let errorText {
                Text(errorText)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(RetailRose.negative)
                    .fixedSize(horizontal: false, vertical: true)
            }

            RoseWhyNote(
                body: "Every slide is built from recorded activity. Nothing is estimated, and an empty section says so.",
                tinted: true
            )
        }
        .padding(16)
        .background(RetailRose.cardSlate)
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
            .foregroundStyle(selected ? RetailRose.textPrimary : RetailRose.textFaint)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(selected ? RetailRose.chip : Color.clear)
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
