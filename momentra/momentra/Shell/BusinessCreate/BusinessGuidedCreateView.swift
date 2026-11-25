import SwiftUI

/// Figma `2:1514` — guided business type selection for Create Moment.
/// Keep in lockstep with `BusinessGuidedCreateContent` on Android.
struct BusinessGuidedCreateView: View {
    var onBack: () -> Void = {}
    var onContinue: (BusinessGuidedCreateOption) -> Void = { _ in }
    var submitting: Bool = false
    var error: String? = nil

    @State private var selectedId = BusinessGuidedCreateSpecs.options[3].id

    private var selected: BusinessGuidedCreateOption {
        BusinessGuidedCreateSpecs.option(id: selectedId)
    }

    private let emerald = BusinessAppStateTokens.emerald

    var body: some View {
        ZStack {
            BusinessAppStateTokens.page.ignoresSafeArea()
            // Dual-orb ambient mesh — same as Pulse Empty / AppState bodies (Figma 2:221).
            AppStateGlow(accent: emerald)
                .allowsHitTesting(false)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // Figma "Brand and dismiss" (`2:1521`) — logo left, close right.
                    HStack {
                        Image("ShellTopbarLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 118, height: 40)
                        Spacer()
                        Button(action: onBack) {
                            Image(systemName: "xmark")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(.white)
                                .frame(width: 44, height: 44)
                        }
                        .buttonStyle(.plain)
                    }
                    .frame(height: 52)
                    .padding(.top, 4)

                    Text("\(BusinessGuidedCreateSpecs.questionLine1)\n\(BusinessGuidedCreateSpecs.questionLine2)")
                        .font(.system(size: 32, weight: .semibold))
                        .foregroundStyle(Color(hex: "#F1F5F9"))
                        .lineSpacing(8)
                        .padding(.top, 20)

                    Text(BusinessGuidedCreateSpecs.subtitle)
                        .font(.system(size: 14))
                        .foregroundStyle(Color(hex: "#A7A7AE"))
                        .padding(.top, 12)

                    LazyVGrid(
                        columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)],
                        spacing: 10
                    ) {
                        ForEach(BusinessGuidedCreateSpecs.options) { option in
                            choiceCard(option)
                        }
                    }
                    .padding(.top, 20)

                    previewCard
                        .padding(.top, 18)

                    BusinessContinueButton(
                        label: BusinessGuidedCreateSpecs.continueLabel,
                        enabled: !submitting
                    ) {
                        onContinue(selected)
                    }
                    .padding(.top, 24)

                    if let error {
                        Text(error)
                            .font(.system(size: 12))
                            .foregroundStyle(BusinessAppStateTokens.errorRing)
                            .padding(.top, 8)
                    }

                    Text(BusinessGuidedCreateSpecs.footnote)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: "#A7A7AE"))
                        .frame(maxWidth: .infinity)
                        .padding(.top, 12)
                        .padding(.bottom, 24)
                }
                .padding(.horizontal, 24)
            }
        }
    }

    private func choiceCard(_ option: BusinessGuidedCreateOption) -> some View {
        let selected = option.id == selectedId
        return Button {
            selectedId = option.id
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    businessIcon(option.iconKey)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 19, height: 19)
                        .foregroundStyle(.white)
                    Spacer()
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(selected ? emerald : Color.white.opacity(0.35))
                }
                Text("\(option.titleLine1)\n\(option.titleLine2)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: "#F1F5F9"))
                    .multilineTextAlignment(.leading)
                Text(option.meaning)
                    .font(.system(size: 11))
                    .foregroundStyle(Color(hex: "#A7A7AE"))
            }
            .padding(14)
            .frame(maxWidth: .infinity, minHeight: 126, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(selected ? emerald.opacity(0.07) : Color(hex: "#1B1B1E"))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(selected ? emerald : Color.white.opacity(0.1), lineWidth: selected ? 1.5 : 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var previewCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(selected.previewEyebrow)
                .font(.system(size: 10, weight: .semibold))
                .tracking(1)
                .foregroundStyle(emerald)
            Text(selected.previewHeadline)
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color(hex: "#F1F5F9"))
            Text(selected.previewBody)
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#A7A7AE"))
            Rectangle()
                .fill(Color.white.opacity(0.1))
                .frame(height: 1)
            LazyVGrid(
                columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)],
                spacing: 8
            ) {
                ForEach(Array(selected.capabilities.enumerated()), id: \.offset) { _, cap in
                    HStack(alignment: .top, spacing: 8) {
                        businessIcon(cap.iconKey)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 16, height: 16)
                            .foregroundStyle(emerald)
                        Text(cap.label)
                            .font(.system(size: 12))
                            .foregroundStyle(Color(hex: "#F1F5F9"))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color(hex: "#18181B"))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "#053B29"), lineWidth: 1)
        )
    }

    private func businessIcon(_ key: String) -> Image {
        if let icon = BusinessIcon(rawValue: key) {
            return icon.image
        }
        return Image(systemName: "circle")
    }
}

/// Solid emerald Continue CTA — Code Connect target for Figma `Continue` (`2:32`).
struct BusinessContinueButton: View {
    var label: String = BusinessGuidedCreateSpecs.continueLabel
    var enabled: Bool = true
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Color(hex: "#0E0E0E"))
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(BusinessAppStateTokens.emerald.opacity(enabled ? 1 : 0.4))
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
