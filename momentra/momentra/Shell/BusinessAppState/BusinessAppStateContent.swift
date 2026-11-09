import SwiftUI

/// Body-only screen bodies for the Figma "App States · Shared (all Business verticals)" kit.
///
/// Every body renders inside the existing shell chrome (top bar, context switcher, moment
/// selector, bottom nav). Only the "State body" region and the glow mesh behind it live here.
///
/// Rebuilt against Figma "Obsidian & Neon Refraction" (`2852:39645`).
///
/// Keep in lockstep with `BusinessAppStateContent.kt` on Android.

// MARK: - Standard body

/// The default state body: glow mesh, hero badge, optional error pill, title, copy, refraction
/// glass card, actions.
///
/// Figma centres this body vertically (`justify-center`) and only overflows on the tall
/// variants, so the content is centred whenever it fits and scrolls past that.
struct BusinessAppStateBody: View {
    let spec: BusinessAppStateSpec
    let accent: BusinessAppStateAccent
    let onPrimary: () -> Void
    let onSecondary: () -> Void

    private var accentColor: Color { accent.color }
    private var tone: AppStateTone {
        spec.phase == .error ? .error : .normal
    }

    /// Figma: the 120pt hero maps onto the 112pt indicator badge; No-company uses the 132 → 124.
    private var heroSize: CGFloat {
        spec.screen == .noCompany ? 124 : BusinessAppStateTokens.heroSize
    }

    /// The Offline frame re-tints the eyebrow to `#94A4B8`.
    private var eyebrowColor: Color {
        spec.screen == .offline ? BusinessAppStateTokens.textMuted : accentColor
    }

    var body: some View {
        ZStack {
            BusinessAppStateTokens.page
                .ignoresSafeArea()

            AppStateGlow(accent: accentColor, tone: tone)
                .offset(y: 24)

            GeometryReader { geo in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: BusinessAppStateTokens.bodyGap) {
                        if spec.screen == .offline {
                            AppStateOfflineBanner(text: BusinessAppStateSpecs.offlineBanner)
                        }

                        AppStateMarkRing(
                            accent: accentColor,
                            icon: spec.icon,
                            size: heroSize,
                            tone: tone
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
                            AppStateGlassCard {
                                AppStateEyebrow(text: eyebrow, accent: eyebrowColor)
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
}

// MARK: - Loading body

/// Loading body: hero badge, title, then a single refraction glass skeleton box holding three
/// shimmering placeholder bars, followed by a 110pt trailing spacer.
///
/// Figma removed the pulsing-dot label row and the 2-tile + 3-card grid, so both are gone here.
struct BusinessAppStateLoadingBody: View {
    let label: String
    let accent: BusinessAppStateAccent

    private var accentColor: Color { accent.color }

    var body: some View {
        ZStack {
            BusinessAppStateTokens.page
                .ignoresSafeArea()

            AppStateGlow(accent: accentColor)
                .offset(y: 24)

            GeometryReader { geo in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: BusinessAppStateTokens.bodyGap) {
                        AppStateMarkRing(accent: accentColor, icon: .sparkle)
                            .padding(.vertical, 2)

                        AppStateTitle(text: label)

                        AppStateSkeletonBox {
                            AppStateSkeletonBar(width: 160)
                            AppStateSkeletonBar(width: 300)
                            AppStateSkeletonBar(width: 220)
                        }
                        .frame(height: BusinessAppStateTokens.skeletonBoxHeight)

                        Spacer().frame(height: BusinessAppStateTokens.skeletonTrailingSpace)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: max(0, geo.size.height - 48), alignment: .center)
                    .padding(.horizontal, BusinessAppStateTokens.screenHorizontalPadding)
                    .padding(.vertical, 24)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Hub body

/// Hub empty body: six capture tiles in a three-column glass grid. Every tile carries the same
/// weight in the Obsidian frame — the amber highlight and the 60% muting are gone.
struct BusinessAppStateHubBody: View {
    let accent: BusinessAppStateAccent
    let vertical: BusinessAppStateVertical
    let onPrimary: () -> Void
    let onSecondary: () -> Void

    private var spec: BusinessAppStateSpec {
        BusinessAppStateSpecs.spec(.hub, .empty)
    }

    private var tiles: [HubTileSpec] {
        BusinessAppStateSpecs.hubTileLabels(vertical: vertical)
    }

    private var accentColor: Color { accent.color }

    /// Figma: `3*111 + 2*10 = 353`, so the three columns are fixed inside the 354 body width.
    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.fixed(BusinessAppStateTokens.hubTileWidth), spacing: BusinessAppStateTokens.hubColumnGap),
            count: 3
        )
    }

    var body: some View {
        ZStack {
            BusinessAppStateTokens.page
                .ignoresSafeArea()

            AppStateGlow(accent: accentColor)
                .offset(y: 24)

            GeometryReader { geo in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: BusinessAppStateTokens.bodyGap) {
                        AppStateMarkRing(
                            accent: accentColor,
                            icon: spec.icon
                        )
                        .padding(.vertical, 2)
                        AppStateTitle(text: spec.title)
                        AppStateBodyText(text: spec.body)

                        LazyVGrid(columns: columns, spacing: BusinessAppStateTokens.hubRowGap) {
                            ForEach(Array(tiles.enumerated()), id: \.offset) { _, tile in
                                HubTile(
                                    icon: tile.icon,
                                    label: tile.label,
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
                    .frame(minHeight: max(0, geo.size.height - 48), alignment: .center)
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
    let icon: BusinessIcon
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: BusinessAppStateTokens.hubTileLabelGap) {
                icon.image
                    .resizable()
                    .scaledToFit()
                    .frame(
                        width: BusinessAppStateTokens.hubIconSize,
                        height: BusinessAppStateTokens.hubIconSize
                    )
                    .foregroundStyle(BusinessAppStateTokens.emerald)
                Text(label)
                    .font(.plusJakarta(size: BusinessAppStateTokens.hubLabelSize, weight: .semibold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            // Figma: tile is `111x67` with 14pt vertical padding; the label is 15pt tall.
            .frame(
                width: BusinessAppStateTokens.hubTileWidth,
                height: BusinessAppStateTokens.hubTileHeight
            )
            // Figma draws the tile as a flat `white 5%` wash — every tile is equal weight.
            .background(
                RoundedRectangle(cornerRadius: BusinessAppStateTokens.hubTileRadius)
                    .fill(BusinessAppStateTokens.hubTileFill)
            )
            .overlay(
                RoundedRectangle(cornerRadius: BusinessAppStateTokens.hubTileRadius)
                    .stroke(BusinessAppStateTokens.hubTileStroke, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

// MARK: - View-all sheet

/// Sheet variant of the empty state, used by the Pulse "Needs Attention" view-all sheet.
/// Figma: sheet fills from the bottom with a scrim behind it, radius 22, emerald stroke.
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
                    .fill(BusinessAppStateTokens.emerald.opacity(0.30))
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
                                .foregroundStyle(
                                    filter == selectedFilter
                                        ? BusinessAppStateTokens.emerald
                                        : BusinessAppStateTokens.textMuted
                                )
                                .padding(.horizontal, 16)
                                .padding(.vertical, 9)
                                .background(
                                    Capsule().fill(
                                        filter == selectedFilter
                                            ? BusinessAppStateTokens.emerald.opacity(0.20)
                                            : Color.white.opacity(0.06)
                                    )
                                )
                                .overlay(
                                    Capsule().stroke(
                                        filter == selectedFilter
                                            ? BusinessAppStateTokens.emerald.opacity(0.80)
                                            : BusinessAppStateTokens.secondaryButtonStroke,
                                        lineWidth: 1
                                    )
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }

                VStack(spacing: BusinessAppStateTokens.bodyGap) {
                    AppStateMarkRing(accent: accentColor, icon: spec.icon)
                    AppStateTitle(text: spec.title)
                    AppStateBodyText(text: spec.body)
                    if let label = spec.secondaryLabel {
                        AppStateSecondaryButton(label: label, action: onShowAll, filled: true)
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
                .fill(BusinessAppStateTokens.glassCardFill)
            )
            .overlay(
                UnevenRoundedRectangle(
                    topLeadingRadius: BusinessAppStateTokens.cardRadius,
                    topTrailingRadius: BusinessAppStateTokens.cardRadius
                )
                .stroke(
                    BusinessAppStateTokens.glassCardStroke,
                    lineWidth: BusinessAppStateTokens.glassCardStrokeWidth
                )
            )
        }
    }
}
