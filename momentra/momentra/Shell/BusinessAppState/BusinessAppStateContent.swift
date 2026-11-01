import SwiftUI

/// Body-only screen bodies for the Figma "App States · Shared (all Business verticals)" kit.
///
/// Every body renders inside the existing shell chrome (top bar, context switcher, moment
/// selector, bottom nav). Only the "State body" region and the glow behind it live here.
///
/// Keep in lockstep with `BusinessAppStateContent.kt` on Android.

// MARK: - Standard body

/// The default state body: glow, hero ring, title, copy, optional glass card, actions.
///
/// Figma centres this body vertically (`justify-center`) and only overflows on the tall
/// variants, so the content is centred whenever it fits and scrolls past that.
struct BusinessAppStateBody: View {
    let spec: BusinessAppStateSpec
    let accent: BusinessAppStateAccent
    let onPrimary: () -> Void
    let onSecondary: () -> Void

    private var accentColor: Color { accent.color }
    private var ringAccent: Color {
        spec.phase == .error ? BusinessAppStateTokens.errorRing : accentColor
    }

    var body: some View {
        ZStack {
            AppStateGlow(accent: accentColor)
                .offset(y: 24)

            GeometryReader { geo in
            ScrollView(showsIndicators: false) {
                VStack(spacing: BusinessAppStateTokens.bodyGap) {
                    if spec.screen == .offline {
                        offlineBanner
                    }

                    AppStateMarkRing(
                        accent: ringAccent,
                        icon: spec.icon,
                        size: spec.screen == .noCompany ? 132 : 120
                    )
                    .padding(.vertical, 2)

                    AppStateTitle(text: spec.title)

                    if !spec.body.isEmpty {
                        AppStateBodyText(text: spec.body)
                    }

                    if let code = spec.errorCode {
                        AppStateErrorCodePill(code: code, accent: BusinessAppStateTokens.errorRing)
                    }

                    if !spec.rows.isEmpty, let eyebrow = spec.eyebrow {
                        AppStateGlassCard(accent: accentColor) {
                            AppStateEyebrow(text: eyebrow, accent: accentColor)
                            ForEach(spec.rows.indices, id: \.self) { i in
                                AppStateGlassRow(
                                    label: spec.rows[i].label,
                                    value: spec.rows[i].value,
                                    valueColor: spec.rows[i].tone == .warning
                                        ? BusinessAppStateTokens.warning
                                        : BusinessAppStateTokens.text
                                )
                            }
                        }
                    }

                    if spec.primaryLabel != nil || spec.secondaryLabel != nil {
                        Spacer().frame(height: 2)
                    }

                    if let label = spec.primaryLabel {
                        AppStatePrimaryButton(label: label, action: onPrimary)
                    }
                    if let label = spec.secondaryLabel {
                        AppStateSecondaryButton(label: label, action: onSecondary)
                    }
                    if let note = spec.footnote {
                        AppStateFootnote(text: note)
                    }
                }
                .frame(maxWidth: .infinity)
                // Figma centres the State body and only overflows on the tall variants.
                .frame(
                    minHeight: max(0, geo.size.height - 48),
                    alignment: .center
                )
                .padding(.horizontal, BusinessAppStateTokens.screenHorizontalPadding)
                .padding(.vertical, 24)
            }
            .scrollBounceBehavior(.basedOnSize)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Amber banner shown above the ring on the Offline state.
    private var offlineBanner: some View {
        HStack(spacing: 10) {
            Image(systemName: AppStateIcon.offline.systemName)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(BusinessAppStateTokens.warning)
            Text(BusinessAppStateSpecs.offlineBanner)
                .font(.plusJakarta(size: 13, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(BusinessAppStateTokens.warningSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(BusinessAppStateTokens.warningBorder, lineWidth: 1)
        )
    }
}

// MARK: - Loading body

/// Loading body: label row with a pulsing accent dot, then two skeleton tiles and three
/// skeleton cards. Figma aligns this body to the top rather than centring it.
struct BusinessAppStateLoadingBody: View {
    let label: String
    let accent: BusinessAppStateAccent

    private var accentColor: Color { accent.color }

    var body: some View {
        ZStack {
            AppStateGlow(accent: accentColor)
                .offset(y: 24)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: BusinessAppStateTokens.bodyGap) {
                    HStack(spacing: 8) {
                        AppStateSpinnerDot(accent: accentColor)
                        Text(label)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(Color.white.opacity(0.75))
                    }

                    HStack(spacing: 10) {
                        ForEach(0..<2) { _ in
                            AppStateSkeletonBox(accent: accentColor) {
                                AppStateSkeletonBar(width: 70, height: 26, radius: 8)
                                AppStateSkeletonBar(width: 90, height: 10, radius: 5)
                            }
                        }
                    }

                    ForEach(0..<3) { _ in
                        AppStateSkeletonBox(accent: accentColor) {
                            AppStateSkeletonBar(width: 120, height: 12)
                            AppStateSkeletonBar(width: 220, height: 28, radius: 8)
                            AppStateSkeletonBar(width: 300, height: 12)
                            AppStateSkeletonBar(width: 240, height: 12)
                        }
                    }
                }
                .padding(.horizontal, BusinessAppStateTokens.screenHorizontalPadding)
                .padding(.vertical, 28)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Hub body

/// Hub empty body: six capture tiles in a two-column grid. The first tile carries the accent
/// treatment, the rest are muted until the first capture lands.
struct BusinessAppStateHubBody: View {
    let accent: BusinessAppStateAccent
    let onPrimary: () -> Void
    let onSecondary: () -> Void

    private var spec: BusinessAppStateSpec {
        BusinessAppStateSpecs.spec(.hub, .empty)
    }

    private var tiles: [HubTileSpec] {
        BusinessAppStateSpecs.hubTileLabels(accent: accent)
    }

    private var accentColor: Color { accent.color }

    var body: some View {
        ZStack {
            AppStateGlow(accent: accentColor)
                .offset(y: 24)

            GeometryReader { geo in
            ScrollView(showsIndicators: false) {
                VStack(spacing: BusinessAppStateTokens.bodyGap) {
                    AppStateMarkRing(accent: accentColor, icon: spec.icon, size: 120)
                        .padding(.vertical, 2)
                    AppStateTitle(text: spec.title)
                    AppStateBodyText(text: spec.body)

                    LazyVGrid(
                        columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)],
                        spacing: 10
                    ) {
                        ForEach(Array(tiles.enumerated()), id: \.offset) { index, tile in
                            HubTile(
                                icon: tile.icon,
                                label: tile.label,
                                accent: accentColor,
                                highlighted: index == 0,
                                action: onPrimary
                            )
                        }
                    }

                    if let note = spec.footnote {
                        AppStateFootnote(text: note)
                    }
                    Spacer().frame(height: 16)
                }
                .frame(maxWidth: .infinity)
                .frame(
                    minHeight: max(0, geo.size.height - 48),
                    alignment: .center
                )
                .padding(.horizontal, BusinessAppStateTokens.screenHorizontalPadding)
                .padding(.vertical, 24)
            }
            .scrollBounceBehavior(.basedOnSize)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct HubTile: View {
    let icon: AppStateIcon
    let label: String
    let accent: Color
    let highlighted: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon.systemName)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.white)
                Text(label)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 73)
            .background(
                RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                    .fill(Color.white.opacity(highlighted ? 0.12 : 0.05))
            )
            .overlay(
                RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                    .stroke(
                        highlighted ? accent.opacity(0.60) : Color.white.opacity(0.12),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

// MARK: - View-all sheet

/// Sheet variant of the empty state, used by the Pulse "Needs Attention" view-all sheet.
/// Figma: sheet fills from the bottom with a scrim behind it, radius 20, accent stroke.
struct BusinessAppStateViewAllSheet: View {
    let accent: BusinessAppStateAccent
    @Binding var selectedFilter: String
    let onClose: () -> Void
    let onShowAll: () -> Void

    private var spec: BusinessAppStateSpec {
        BusinessAppStateSpecs.spec(.viewAll, .empty)
    }

    private var accentColor: Color { accent.color }
    private let filters = ["All", "Open", "Due soon", "Closed"]

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.55)
                .ignoresSafeArea()
                .onTapGesture(perform: onClose)

            VStack(spacing: 12) {
                Capsule()
                    .fill(Color.white.opacity(0.30))
                    .frame(width: 40, height: 4)

                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 2) {
                        if let eyebrow = spec.eyebrow {
                            AppStateEyebrow(text: eyebrow, accent: accentColor)
                        }
                        Text("Needs Attention")
                            .font(.plusJakarta(size: 20, weight: .heavy))
                            .foregroundStyle(.white)
                    }
                    Spacer()
                    AppStateIconButton(
                        systemName: "xmark",
                        accessibilityLabel: "Close",
                        action: onClose
                    )
                }

                HStack(spacing: 8) {
                    ForEach(filters, id: \.self) { filter in
                        Button {
                            selectedFilter = filter
                        } label: {
                            Text(filter)
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 9)
                                .background(
                                    Capsule()
                                        .fill(
                                            filter == selectedFilter
                                                ? AnyShapeStyle(
                                                    LinearGradient(
                                                        colors: [
                                                            BusinessAppStateTokens.ctaStart,
                                                            BusinessAppStateTokens.ctaEnd,
                                                        ],
                                                        startPoint: .leading,
                                                        endPoint: .trailing
                                                    )
                                                )
                                                : AnyShapeStyle(Color.white.opacity(0.10))
                                        )
                                )
                                .overlay(
                                    Capsule().stroke(
                                        filter == selectedFilter
                                            ? Color.clear
                                            : Color.white.opacity(0.22),
                                        lineWidth: 1
                                    )
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }

                VStack(spacing: BusinessAppStateTokens.bodyGap) {
                    AppStateMarkRing(accent: accentColor, icon: spec.icon, size: 112)
                    AppStateTitle(text: spec.title)
                    AppStateBodyText(text: spec.body)
                    if let label = spec.secondaryLabel {
                        AppStateSecondaryButton(label: label, action: onShowAll)
                    }
                    if let note = spec.footnote {
                        AppStateFootnote(text: note)
                    }
                }
                .padding(.top, 4)

                Spacer().frame(height: 12)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(
                UnevenRoundedRectangle(
                    topLeadingRadius: BusinessAppStateTokens.cardRadius,
                    topTrailingRadius: BusinessAppStateTokens.cardRadius
                )
                .fill(BusinessAppStateTokens.sheet)
            )
            .overlay(
                UnevenRoundedRectangle(
                    topLeadingRadius: BusinessAppStateTokens.cardRadius,
                    topTrailingRadius: BusinessAppStateTokens.cardRadius
                )
                .stroke(accentColor.opacity(0.35), lineWidth: 1)
            )
        }
    }
}
