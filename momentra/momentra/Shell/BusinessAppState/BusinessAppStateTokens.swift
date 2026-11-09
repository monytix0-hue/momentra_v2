import SwiftUI

/// Tokens for the Figma "App States · Shared (all Business verticals)" kit.
///
/// Rebuilt against Figma "Business v3 · International Obsidian · Emerald Theme" (node 2852:39645).
/// That frame replaces the flat dark grey surface with a deep `#05070B` obsidian canvas, a
/// volumetric dual-orb glow mesh, high-refraction glass cards and floating neon indicator badges.
///
/// The kit collapses `appstate/accent` to one emerald value shared by every Business vertical, so
/// the whole kit reads as a single accent system. These are the exact Figma values and are the
/// source of truth for this layer — they intentionally do not follow `BusinessActiveTheme`.
///
/// Keep in lockstep with `BusinessAppStateTokens.kt` on Android.
enum BusinessAppStateAccent: String, CaseIterable {
    /// App States · Accent / Emerald — #10B981 (unified across every Business vertical)
    case emerald

    var color: Color {
        Color(hex: "#10B981")
    }

    /// Resolve the accent for an operating moment subtype code.
    ///
    /// The accent is unified, so every code resolves to emerald. The *vertical* still matters for
    /// copy overrides — see `BusinessAppStateVertical`.
    static func forTypeCode(_ momentTypeCode: String?) -> BusinessAppStateAccent {
        .emerald
    }
}

/// The business vertical a moment belongs to.
///
/// The accent is unified across verticals, but the kit still carries per-vertical *text* overrides
/// (Figma "Hub label overrides"), so the vertical stays a first-class discriminator even though the
/// colour no longer varies.
///
/// Keep in lockstep with `BusinessAppStateVertical.kt` on Android.
enum BusinessAppStateVertical: String, CaseIterable {
    case trading
    case retail
    case rst
    case services
    case mfg
    case marketing
    case distribution

    /// Resolve the vertical for an operating moment subtype code.
    ///
    /// Mirrors the Figma mode set exactly, so the same moment is classified identically on every
    /// platform.
    static func forTypeCode(_ momentTypeCode: String?) -> BusinessAppStateVertical {
        switch momentTypeCode?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() {
        case "TRADING_WHOLESALE": return .trading
        case "DISTRIBUTION_DEALERSHIP": return .distribution
        case "RETAIL_STORE": return .retail
        case "RESTAURANT_FOOD": return .rst
        case "CONSUMER_SERVICE": return .services
        case "STANDARD_PRODUCTION", "MADE_TO_ORDER": return .mfg
        case "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN": return .marketing
        default: return .trading
        }
    }
}

/// Surface ramp shared by every app-state variant.
///
/// Figma "Obsidian & Neon Refraction": a deep `#05070B` spatial canvas, a volumetric dual-orb glow
/// mesh, `rgba(16,22,35,0.72)` refraction glass at a 1.25pt `rgba(16,185,129,0.35)` border, and the
/// emerald `#059669 → #34D399` primary CTA ramp. Every value here is read from the frame nodes
/// directly (node 2852:39645).
enum BusinessAppStateTokens {
    // MARK: Palette primitives
    /// Figma emerald 500 — the kit's single accent.
    static let emerald = Color(hex: "#10B981")
    /// Figma emerald 600 — CTA start and the warm glow orb.
    static let emeraldDeep = Color(hex: "#059669")
    /// Figma emerald 400 — CTA end.
    static let emeraldBright = Color(hex: "#34D399")
    /// Figma error / diagnostic red.
    static let errorRing = Color(hex: "#FF5C38")
    /// Figma amber, used for "Not recorded" / "Hidden until online" values inline in the rows.
    static let warning = Color(hex: "#F5A624")

    // MARK: Surfaces
    /// Figma root background for every Business app-state frame (`#05070B`).
    static let page = Color(hex: "#05070B")
    /// Hero badge core fill (`#0F141F`).
    static let sheet = Color(hex: "#0F141F")
    static let surfaceRaised = Color(hex: "#151D2E")
    static let border = Color(hex: "#1E293B")

    // MARK: Text
    static let text = Color.white
    /// Body copy and muted subtitles (`#94A4B8`).
    static let textMuted = Color(hex: "#94A4B8")
    /// Footnoted microcopy (`#738099`).
    static let textSubtle = Color(hex: "#738099")
    /// Glass-card row labels (`#C7D1E0`).
    static let rowLabelColor = Color(hex: "#C7D1E0")

    // MARK: Glass (backdrop-blurred chrome: icon buttons, dividers)
    static let glassFill = Color.white.opacity(0.08)
    static let glassStroke = Color.white.opacity(0.22)
    static let glassStrokeSoft = Color.white.opacity(0.14)
    /// Figma `backdrop-blur` on every glass surface.
    static let blurRadius: CGFloat = 24

    /// Refraction glass card fill: `rgba(16,22,35,0.72)`.
    static let glassCardFill = Color(red: 16 / 255, green: 22 / 255, blue: 35 / 255).opacity(0.72)
    /// Refraction glass card stroke: emerald @35%.
    static let glassCardStroke = emerald.opacity(0.35)
    /// Figma draws the card border at `1.25px`.
    static let glassCardStrokeWidth: CGFloat = 1.25

    /// Small emerald hairline used by chips and tab containers.
    static let hairlineStroke = emerald.opacity(0.18)

    // MARK: Primary CTA — the emerald gradient
    static let ctaStart = Color(hex: "#059669")
    static let ctaEnd = Color(hex: "#34D399")
    /// Emerald glow borrowed from Figma's `quick-bubble` shadow so the CTA lifts off the canvas.
    static let ctaShadow = emerald.opacity(0.35)
    static let ctaShadowRadius: CGFloat = 12
    static let ctaShadowY: CGFloat = 4

    /// Secondary CTA: no fill on screen bodies, a 6% white wash inside the switcher sheet.
    static let secondaryButtonFill = Color.clear
    static let secondaryButtonStroke = emerald.opacity(0.18)

    // MARK: Ambient glow mesh
    /// Atmospheric accent orb behind the state body (`#10B981`).
    static let glowAccent = emerald
    static let glowAccentSize: CGFloat = 360
    /// Stop 0 alpha; the mesh fades to 8% at 60% and 0 at the rim.
    static let glowAccentAlpha: Double = 0.28
    static let glowAccentMidAlpha: Double = 0.08
    static let glowAccentMidStop: CGFloat = 0.60
    /// Warm secondary orb (`#059669`).
    static let glowWarm = emeraldDeep
    static let glowWarmSize: CGFloat = 280
    static let glowWarmAlpha: Double = 0.15
    /// Error states swap the accent orb for `#FF5C38`.
    static let glowError = errorRing
    static let glowErrorAlpha: Double = 0.25

    // MARK: Hero indicator badge (Figma "Hero Indicator Badge", 112pt)
    static let heroSize: CGFloat = 112
    /// Outer halo: 96 inset 8, emerald @8% fill, emerald @45% 2pt ring.
    static let heroRingSize: CGFloat = 96
    static let heroRingInset: CGFloat = 8
    static let heroRingStroke: CGFloat = 2
    static let heroRingFillAlpha: Double = 0.08
    static let heroRingStrokeAlpha: Double = 0.45
    /// Inner core: 76 inset 18, `#0F141F` fill, emerald @18% 1.5pt ring.
    static let heroCoreSize: CGFloat = 76
    static let heroCoreInset: CGFloat = 18
    static let heroCoreStroke: CGFloat = 1.5
    static let heroCoreFill = Color(hex: "#0F141F")
    static let heroCoreStrokeAlpha: Double = 0.18
    /// Floating status dot: 36 at (72,72), state-coloured fill, emerald 3.5pt ring.
    static let heroDotSize: CGFloat = 36
    static let heroDotInset: CGFloat = 72
    static let heroDotStroke: CGFloat = 3.5
    /// Hero glyph inside the core, and the glyph inside the status dot.
    static let heroGlyphSize: CGFloat = 28
    static let heroDotGlyphSize: CGFloat = 17
    static let heroDotGlyphColor = Color(hex: "#080A0F")

    // MARK: Semantic
    static let danger = Color(hex: "#EF4444")
    /// Diagnostic pill: red @15% fill, emerald @45% stroke, `#FF5C38` label.
    static let errorPillFill = errorRing.opacity(0.15)
    static let errorPillStroke = emerald.opacity(0.45)
    /// Offline status pill: `rgba(38,46,61,0.9)` fill, emerald @40% stroke.
    static let offlineBannerFill = Color(red: 38 / 255, green: 46 / 255, blue: 61 / 255).opacity(0.90)
    static let offlineBannerStroke = emerald.opacity(0.40)
    static let offlineBannerHeight: CGFloat = 34
    static let offlineGrey = Color(hex: "#94A4B8")

    // MARK: Geometry
    static let screenHorizontalPadding: CGFloat = 24
    static let bodyWidth: CGFloat = 354
    static let bodyGap: CGFloat = 12
    static let cardRadius: CGFloat = 22
    static let cardPaddingHorizontal: CGFloat = 20
    static let cardPaddingVertical: CGFloat = 18
    static let cardRowGap: CGFloat = 12
    static let buttonRadius: CGFloat = 999
    static let primaryButtonHeight: CGFloat = 50
    static let secondaryButtonHeight: CGFloat = 46

    // MARK: Hub capture grid (Figma "Quick Adds Glass Grid")
    /// Six `111x67` tiles across the `354` body width: `3*111 + 2*10 = 353`.
    static let hubTileWidth: CGFloat = 111
    static let hubTileHeight: CGFloat = 67
    static let hubColumnGap: CGFloat = 10
    static let hubRowGap: CGFloat = 10
    static let hubTileRadius: CGFloat = 16
    static let hubTileFill = Color.white.opacity(0.05)
    static let hubTileStroke = emerald.opacity(0.25)
    static let hubIconSize: CGFloat = 18
    static let hubLabelSize: CGFloat = 11.5
    static let hubTileLabelGap: CGFloat = 6
    static let hubTileVerticalPadding: CGFloat = 14

    // MARK: Loading skeleton (Figma "Loading Glass Skeleton Box")
    static let skeletonBoxHeight: CGFloat = 150
    static let skeletonPadHorizontal: CGFloat = 20
    static let skeletonPadVertical: CGFloat = 22
    static let skeletonGap: CGFloat = 14
    static let skeletonBarHeight: CGFloat = 16
    static let skeletonBarRadius: CGFloat = 8
    /// Shimmer ramp: base → 18% at 50% → base.
    static let skeletonBarBase = Color.white.opacity(0.04)
    static let skeletonBarHighlight = Color.white.opacity(0.18)
    static let skeletonTrailingSpace: CGFloat = 110

    // MARK: Type scale
    static let titleSize: CGFloat = 24
    static let bodySize: CGFloat = 13.5
    /// Figma body `leading-[1.35]`: SwiftUI spacing stacks on the font's intrinsic leading.
    static let bodyLineSpacing: CGFloat = 3
    static let eyebrowSize: CGFloat = 11
    static let rowLabelSize: CGFloat = 13
    static let rowValueSize: CGFloat = 13
    static let buttonLabelSize: CGFloat = 13.5
    static let errorCodeSize: CGFloat = 11
    static let footnoteSize: CGFloat = 11
    static let offlineBannerLabelSize: CGFloat = 11.5

    static let eyebrowTracking: CGFloat = 1.5
    static let buttonTracking: CGFloat = 1
    static let errorCodeTracking: CGFloat = 1.5

    // MARK: Alphas still referenced by shared chrome
    /// Footnote alpha over `textSubtle` — the footnote is solid `#738099` on every screen.
    static let footnoteMutedOpacity: CGFloat = 1
}
