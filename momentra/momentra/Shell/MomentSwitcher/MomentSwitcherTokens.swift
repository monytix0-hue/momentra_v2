import SwiftUI

/// Tokens for the Figma `MomentSwitcher` frame (`2678:37791`) — the Group and Business moment
/// switcher overlay.
///
/// Read from the five state frames: `2678:37481` Ongoing, `2678:37561` Completed, `2678:37634`
/// Empty, `2678:37715` SearchEmpty, `2678:37790` LoadError.
///
/// Deliberately *not* folded into `BusinessAppStateTokens`: that kit backs a different Figma family
/// (`2852:39645`) whose values are asserted by `BusinessAppStateSpecsTests`, and the switcher now
/// serves Group too.
///
/// Keep in lockstep with `MomentSwitcherTokens.kt` on Android.
enum MomentSwitcherTokens {
    // MARK: Canvas
    /// Frame root fill — `#12151C`.
    static let page = Color(hex: 0x12151C)
    /// The `wash` ellipse: emerald at 12%, 360pt, gaussian blur 50, pegged to (-120, -80).
    static let wash = Color(hex: 0x10B981)
    static let washAlpha: Double = 0.12
    static let washSize: CGFloat = 360
    static let washBlur: CGFloat = 50
    static let washOffset = CGSize(width: -120, height: -80)

    // MARK: Glass
    /// White @ 8% — state cards, metric tiles, search field.
    static let glassFill = Color.white.opacity(0.08)
    /// White @ 14% — state card / search stroke.
    static let glassStroke = Color.white.opacity(0.14)
    /// White @ 6% — tab container fill.
    static let sheetFill = Color.white.opacity(0.06)
    /// White @ 12% — tab container stroke.
    static let sheetStroke = Color.white.opacity(0.12)
    /// White @ 10% fill / 20% stroke — the 40pt close button.
    static let iconButtonFill = Color.white.opacity(0.10)
    static let iconButtonStroke = Color.white.opacity(0.20)

    // MARK: Accent
    /// `appstate/accent` collapsed to emerald.
    static let accent = Color(hex: 0x10B981)
    /// Selected tab gradient: emerald @10% -> violet @33%.
    static let tabStart = Color(hex: 0x10B981).opacity(0.10)
    static let tabEnd = Color(hex: 0x8B5CF6).opacity(0.33)
    /// `#00E599` — the LIVE NOW dot and label.
    static let liveDot = Color(hex: 0x00E599)
    /// `#F5A623` — the Categories metric and the Story pill ring.
    static let warm = Color(hex: 0xF5A623)
    /// Amber @ 28% — the Categories metric stroke.
    static let warmStroke = Color(hex: 0xF5A623).opacity(0.28)
    /// `#FF5C38` — the error card stroke and badge.
    static let error = Color(hex: 0xFF5C38)
    static let errorStroke = Color(hex: 0xFF5C38).opacity(0.30)
    static let errorPillFill = Color(hex: 0xFF5C38).opacity(0.14)
    static let errorPillStroke = Color(hex: 0xFF5C38).opacity(0.40)

    // MARK: Gradients
    /// `chip · All` and every primary CTA: `#E8621A` -> `#F5A623`.
    static let emberStart = Color(hex: 0xE8621A)
    static let emberEnd = Color(hex: 0xF5A623)
    /// `Live card` fill: `#312E81` -> `rgba(14,124,102,0.3)` @55% -> `rgba(16,185,129,0.3)`.
    static let liveCardStops: [Gradient.Stop] = [
        .init(color: Color(hex: 0x312E81), location: 0),
        .init(color: Color(hex: 0x0E7C66).opacity(0.30), location: 0.55),
        .init(color: Color(hex: 0x10B981).opacity(0.30), location: 1),
    ]
    /// The same three stops as plain colours, for buckets that carry their gradient as a list.
    static let liveCardColors: [Color] = liveCardStops.map(\.color)
    /// Unselected bucket chip: white @ 12% fill, white @ 22% stroke.
    static let chipFill = Color.white.opacity(0.12)
    static let chipStroke = Color.white.opacity(0.22)
    /// LIVE NOW chip: black @ 25% fill, white @ 20% stroke.
    static let liveChipFill = Color.black.opacity(0.25)
    static let liveChipStroke = Color.white.opacity(0.20)
    /// Story pill: white @ 16% fill.
    static let storyPillFill = Color.white.opacity(0.16)
    /// Clear-search pill: white @ 14% fill.
    static let clearPillFill = Color.white.opacity(0.14)

    // MARK: Text
    static let text = Color.white
    /// Headline body at 70% — subtitles, metric labels, empty-card body.
    static let textMuted = Color.white.opacity(0.70)
    /// Placeholder at 55%.
    static let textSubtle = Color.white.opacity(0.55)
    /// Unselected tab label at 55%.
    static let tabIdle = Color.white.opacity(0.55)
    /// Card microcopy — `rgba(255,255,255,0.85)`.
    static let textOnCard = Color.white.opacity(0.85)

    // MARK: Geometry
    static let screenPadding: CGFloat = 24
    static let bodyGap: CGFloat = 16
    static let cardRadius: CGFloat = 22
    static let stateCardRadius: CGFloat = 32
    static let liveCardRadius: CGFloat = 28
    static let metricRadius: CGFloat = 20
    static let searchRadius: CGFloat = 20
    static let closeSize: CGFloat = 40
    static let closeGlyphSize: CGFloat = 17
    static let avatarSize: CGFloat = 40
    static let storyAvatarSize: CGFloat = 28
    static let filterIconSize: CGFloat = 12
    static let searchIconSize: CGFloat = 19
    static let liveDotSize: CGFloat = 8
    static let heroSize: CGFloat = 136
    static let heroSearchSize: CGFloat = 124
    static let heroRingSize: CGFloat = 108
    static let heroSearchRingSize: CGFloat = 96
    static let heroInset: CGFloat = 14
    static let heroMarkSize: CGFloat = 96
    static let heroSearchMarkSize: CGFloat = 84
    static let heroMarkInset: CGFloat = 20
    static let heroBadgeSize: CGFloat = 40
    static let heroBadgeInset: CGFloat = 92
    static let heroSearchBadgeInset: CGFloat = 80
    static let heroBadgeBorder: CGFloat = 3
    static let liveCardShadowRadius: CGFloat = 32
    static let liveCardShadowY: CGFloat = 10
    static let ctaShadowRadius: CGFloat = 20
    static let ctaShadowY: CGFloat = 6

    // MARK: Type scale
    static let headlineSize: CGFloat = 22
    static let subtitleSize: CGFloat = 13
    static let tabLabelSize: CGFloat = 14
    static let metricValueSize: CGFloat = 32
    static let metricLabelSize: CGFloat = 12
    static let searchTextSize: CGFloat = 14
    static let chipLabelSize: CGFloat = 13
    static let eyebrowSize: CGFloat = 12
    static let eyebrowTracking: CGFloat = 2
    static let cardTitleSize: CGFloat = 26
    static let cardMetaSize: CGFloat = 13
    static let cardMicroSize: CGFloat = 12
    static let liveChipSize: CGFloat = 11
    static let liveChipTracking: CGFloat = 1.5
    static let storyLabelSize: CGFloat = 14
    static let storyGlyphSize: CGFloat = 10
    static let stateTitleSize: CGFloat = 24
    static let stateTitleSmallSize: CGFloat = 17
    static let stateBodySize: CGFloat = 14
    static let stateBodyLineSpacing: CGFloat = 4
    static let footerTitleSize: CGFloat = 15
    static let footerBodySize: CGFloat = 12
    static let ctaLabelSize: CGFloat = 14
    static let ctaTracking: CGFloat = 1
    static let errorCodeSize: CGFloat = 11
    static let errorCodeTracking: CGFloat = 1.5
    static let clearPillSize: CGFloat = 12
    static let filterLabelSize: CGFloat = 12
    static let heroBadgeGlyphSize: CGFloat = 18

    // MARK: State muting (Figma `muted` group opacity)
    /// `Empty`: tabs + search muted to 55%.
    static let mutedEmpty: Double = 0.55
    /// `LoadError`: tabs + search muted to 45%.
    static let mutedError: Double = 0.45
}
