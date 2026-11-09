import SwiftUI

/// Tone of an app-state surface. Error frames swap the emerald accent orb for the diagnostic red
/// while keeping every emerald structural stroke, which is exactly what Figma draws.
///
/// Keep in lockstep with `AppStateTone` on Android.
enum AppStateTone {
    case normal
    case error
}

/// Body-only primitives for the Figma "App States · Shared (all Business verticals)" kit.
///
/// The kit draws a full 402x874 screen, but the shell already owns the status bar, top bar,
/// context switcher, moment selector and bottom nav. Only the "State body" region and the glow
/// mesh behind it are rendered here, mounted inside existing chrome.
///
/// Rebuilt against Figma "Obsidian & Neon Refraction" (`2852:39645`).
///
/// Keep in lockstep with `BusinessAppStateKit.kt` on Android.

// MARK: - Glow

/// Volumetric dual-orb mesh behind the state body.
///
/// Figma: an accent orb (`#10B981`, 360pt, 28% → 8% @60% → 0) with a warm `#059669` orb (280pt,
/// 15% → 0) drifting off its lower-right shoulder. Error frames drop the warm orb and paint the
/// accent orb `#FF5C38` at 25%.
struct AppStateGlow: View {
    let accent: Color
    var tone: AppStateTone = .normal
    var size: CGFloat = BusinessAppStateTokens.glowAccentSize

    private var accentOrbColor: Color {
        tone == .error ? BusinessAppStateTokens.glowError : accent
    }

    private var accentOrbAlpha: Double {
        tone == .error ? BusinessAppStateTokens.glowErrorAlpha : BusinessAppStateTokens.glowAccentAlpha
    }

    private var accentOrbMidAlpha: Double {
        tone == .error
            ? BusinessAppStateTokens.glowErrorAlpha * 0.34
            : BusinessAppStateTokens.glowAccentMidAlpha
    }

    var body: some View {
        ZStack {
            Orb(
                color: accentOrbColor,
                coreAlpha: accentOrbAlpha,
                midAlpha: accentOrbMidAlpha,
                size: size
            )
            .offset(y: -size * 0.10)

            if tone == .normal {
                Orb(
                    color: BusinessAppStateTokens.glowWarm,
                    coreAlpha: BusinessAppStateTokens.glowWarmAlpha,
                    midAlpha: 0,
                    size: BusinessAppStateTokens.glowWarmSize
                )
                .offset(x: size * 0.24, y: size * 0.20)
            }
        }
        .frame(width: size, height: size)
        .allowsHitTesting(false)
    }
}

/// One soft radial orb in the mesh.
private struct Orb: View {
    let color: Color
    let coreAlpha: Double
    let midAlpha: Double
    let size: CGFloat

    var body: some View {
        Circle()
            .fill(
                RadialGradient(
                    stops: [
                        .init(color: color.opacity(coreAlpha), location: 0),
                        .init(color: color.opacity(max(midAlpha, 0)), location: BusinessAppStateTokens.glowAccentMidStop),
                        .init(color: color.opacity(0), location: 1),
                    ],
                    center: .center,
                    startRadius: 0,
                    endRadius: size / 2
                )
            )
            .frame(width: size, height: size)
            .blur(radius: size * 0.06)
    }
}

// MARK: - Hero indicator badge

/// Floating hero indicator badge: outer halo ring, obsidian core with the state glyph, and a
/// state-coloured status dot riding the lower-right corner.
///
/// Figma geometry at the 112pt reference: halo 96 inset 8 (emerald @8% fill, emerald @45% 2pt
/// ring), core 76 inset 18 (`#0F141F` fill, emerald @18% 1.5pt ring, 28pt white glyph), status dot
/// 36 at (72,72) with a 3.5pt emerald ring and a 17pt `#080A0F` glyph. Everything below scales off
/// `size / 112`.
struct AppStateMarkRing: View {
    let accent: Color
    let icon: AppStateIcon
    var size: CGFloat = BusinessAppStateTokens.heroSize
    var tone: AppStateTone = .normal

    private var scale: CGFloat { size / BusinessAppStateTokens.heroSize }

    /// The status dot keeps the emerald ring in both tones; only its fill flips.
    private var dotFill: Color {
        tone == .error ? BusinessAppStateTokens.errorRing : accent
    }

    private var dotGlyph: String {
        tone == .error ? "exclamationmark" : "checkmark"
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Outer halo ring.
            Circle()
                .fill(BusinessAppStateTokens.emerald.opacity(BusinessAppStateTokens.heroRingFillAlpha))
                .overlay(
                    Circle().stroke(
                        BusinessAppStateTokens.emerald.opacity(BusinessAppStateTokens.heroRingStrokeAlpha),
                        lineWidth: BusinessAppStateTokens.heroRingStroke * scale
                    )
                )
                .frame(
                    width: BusinessAppStateTokens.heroRingSize * scale,
                    height: BusinessAppStateTokens.heroRingSize * scale
                )
                .offset(
                    x: BusinessAppStateTokens.heroRingInset * scale,
                    y: BusinessAppStateTokens.heroRingInset * scale
                )

            // Obsidian core with the state glyph.
            Circle()
                .fill(BusinessAppStateTokens.heroCoreFill)
                .overlay(
                    Circle().stroke(
                        BusinessAppStateTokens.emerald.opacity(BusinessAppStateTokens.heroCoreStrokeAlpha),
                        lineWidth: BusinessAppStateTokens.heroCoreStroke * scale
                    )
                )
                .overlay(
                    Image(systemName: icon.systemName)
                        .font(.system(size: BusinessAppStateTokens.heroGlyphSize * scale, weight: .bold))
                        .foregroundStyle(.white)
                )
                .frame(
                    width: BusinessAppStateTokens.heroCoreSize * scale,
                    height: BusinessAppStateTokens.heroCoreSize * scale
                )
                .offset(
                    x: BusinessAppStateTokens.heroCoreInset * scale,
                    y: BusinessAppStateTokens.heroCoreInset * scale
                )

            // Floating status dot.
            Circle()
                .fill(dotFill)
                .overlay(
                    Circle().stroke(
                        BusinessAppStateTokens.emerald,
                        lineWidth: BusinessAppStateTokens.heroDotStroke * scale
                    )
                )
                .overlay(
                    Image(systemName: dotGlyph)
                        .font(.system(size: BusinessAppStateTokens.heroDotGlyphSize * scale, weight: .heavy))
                        .foregroundStyle(BusinessAppStateTokens.heroDotGlyphColor)
                )
                .frame(
                    width: BusinessAppStateTokens.heroDotSize * scale,
                    height: BusinessAppStateTokens.heroDotSize * scale
                )
                .offset(
                    x: BusinessAppStateTokens.heroDotInset * scale,
                    y: BusinessAppStateTokens.heroDotInset * scale
                )
        }
        .frame(width: size, height: size, alignment: .topLeading)
        .accessibilityHidden(true)
    }
}

// MARK: - Text

/// Section eyebrow inside a glass card. Figma: 11pt ExtraBold, tracking 1.5, emerald.
/// The Offline frame re-tints this to `#94A4B8`.
struct AppStateEyebrow: View {
    let text: String
    let accent: Color

    var body: some View {
        Text(text.uppercased())
            .font(.plusJakarta(size: BusinessAppStateTokens.eyebrowSize, weight: .heavy))
            .tracking(BusinessAppStateTokens.eyebrowTracking)
            .foregroundStyle(accent)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Title. Figma: 24pt Bold, line-height 30, white.
struct AppStateTitle: View {
    let text: String
    var alignment: TextAlignment = .center

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.titleSize, weight: .heavy))
            .foregroundStyle(.white)
            .multilineTextAlignment(alignment)
            .lineSpacing(3)
            .frame(maxWidth: .infinity, alignment: alignment == .center ? .center : .leading)
    }
}

/// Body copy. Figma: 13.5pt Medium, `#94A4B8`, line-height 19.
struct AppStateBodyText: View {
    let text: String
    var alignment: TextAlignment = .center

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.bodySize, weight: .medium))
            .foregroundStyle(BusinessAppStateTokens.textMuted)
            .multilineTextAlignment(alignment)
            // Figma body copy is `leading-[1.35]`; SwiftUI spacing adds to the font's own
            // line height, so this lands near the 19pt Android uses.
            .lineSpacing(BusinessAppStateTokens.bodyLineSpacing)
            .frame(maxWidth: .infinity, alignment: alignment == .center ? .center : .leading)
    }
}

/// Muted footnote under the actions. Figma: 11pt Medium, `#738099`, line-height 15.
struct AppStateFootnote: View {
    let text: String
    /// Multiplies `textSubtle`; Figma draws footnotes solid, so this stays at 1.
    var opacity: CGFloat = BusinessAppStateTokens.footnoteMutedOpacity
    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.footnoteSize, weight: .medium))
            .foregroundStyle(BusinessAppStateTokens.textSubtle.opacity(Double(opacity)))
            .multilineTextAlignment(.center)
            .lineSpacing(2)
            .frame(maxWidth: .infinity)
    }
}

// MARK: - Glass card

/// Refraction glass card: `rgba(16,22,35,0.72)` over the glow, 1.25pt emerald @35% border,
/// radius 22, 20/18 padding, 12pt row gap. The flat translucent fill *is* the Figma effect — there
/// is no sheen overlay to reproduce.
struct AppStateGlassCard<Content: View>: View {
    /// Multiplies the emerald card border; 1 matches Figma exactly.
    var strokeAlpha: Double = 1
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: BusinessAppStateTokens.cardRowGap) {
            content()
        }
        .padding(.horizontal, BusinessAppStateTokens.cardPaddingHorizontal)
        .padding(.vertical, BusinessAppStateTokens.cardPaddingVertical)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            // Figma `backdrop-blur-[12px]`: the material blurs the glow behind the card, then the
            // translucent `rgba(16,22,35,0.72)` fill sits on top of it.
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                        .fill(BusinessAppStateTokens.glassCardFill)
                )
        )
        .overlay(
            RoundedRectangle(cornerRadius: BusinessAppStateTokens.cardRadius)
                .stroke(
                    BusinessAppStateTokens.glassCardStroke.opacity(strokeAlpha),
                    lineWidth: BusinessAppStateTokens.glassCardStrokeWidth
                )
        )
    }
}

/// One label/value line inside a glass card.
struct AppStateGlassRow: View {
    let label: String
    let value: String
    var valueColor: Color = BusinessAppStateTokens.text

    var body: some View {
        HStack(spacing: 10) {
            Text(label)
                .font(.plusJakarta(size: BusinessAppStateTokens.rowLabelSize, weight: .medium))
                .foregroundStyle(BusinessAppStateTokens.rowLabelColor)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(value)
                .font(.plusJakarta(size: BusinessAppStateTokens.rowValueSize, weight: .semibold))
                .foregroundStyle(valueColor)
                .fixedSize(horizontal: true, vertical: false)
        }
    }
}

// MARK: - Buttons

/// Primary CTA. Figma: `#059669 → #34D399` gradient, radius 999, height 50, 13.5pt ExtraBold
/// tracking 1, emerald glow shadow.
struct AppStatePrimaryButton: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label.uppercased())
                .font(.plusJakarta(size: BusinessAppStateTokens.buttonLabelSize, weight: .heavy))
                .tracking(BusinessAppStateTokens.buttonTracking)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: BusinessAppStateTokens.primaryButtonHeight)
                .background(
                    LinearGradient(
                        colors: [BusinessAppStateTokens.ctaStart, BusinessAppStateTokens.ctaEnd],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(Capsule())
                .shadow(
                    color: BusinessAppStateTokens.ctaShadow,
                    radius: BusinessAppStateTokens.ctaShadowRadius,
                    x: 0,
                    y: BusinessAppStateTokens.ctaShadowY
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

/// Secondary CTA. Figma: no fill, 1pt emerald @18% border, radius 999, height 46, 13.5pt Bold.
struct AppStateSecondaryButton: View {
    let label: String
    let action: () -> Void
    /// The switcher sheet variant paints a 6% white wash behind the border.
    var filled: Bool = false

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.plusJakarta(size: BusinessAppStateTokens.buttonLabelSize, weight: .bold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: BusinessAppStateTokens.secondaryButtonHeight)
                .background(
                    Capsule().fill(filled ? Color.white.opacity(0.06) : Color.clear)
                )
                .overlay(
                    Capsule().stroke(BusinessAppStateTokens.secondaryButtonStroke, lineWidth: 1)
                )
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

// MARK: - Pills

/// Diagnostic pill on error states. Figma: `#FF5C38` @15% fill, emerald @45% border, 11pt
/// ExtraBold tracking 1.5 label in `#FF5C38`.
struct AppStateErrorCodePill: View {
    let code: String
    let accent: Color

    var body: some View {
        Text(code)
            .font(.plusJakarta(size: BusinessAppStateTokens.errorCodeSize, weight: .heavy))
            .tracking(BusinessAppStateTokens.errorCodeTracking)
            .foregroundStyle(accent)
            .padding(.horizontal, 14)
            .padding(.vertical, 6)
            .background(Capsule().fill(BusinessAppStateTokens.errorPillFill))
            .overlay(Capsule().stroke(BusinessAppStateTokens.errorPillStroke, lineWidth: 1))
    }
}

/// Centred offline status pill. Figma: `rgba(38,46,61,0.9)` fill, emerald @40% border, height 34,
/// 11.5pt SemiBold white. No icon, no amber.
struct AppStateOfflineBanner: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.plusJakarta(size: BusinessAppStateTokens.offlineBannerLabelSize, weight: .semibold))
            .foregroundStyle(.white)
            .multilineTextAlignment(.center)
            .padding(.horizontal, 16)
            .frame(height: BusinessAppStateTokens.offlineBannerHeight)
            .background(Capsule().fill(BusinessAppStateTokens.offlineBannerFill))
            .overlay(Capsule().stroke(BusinessAppStateTokens.offlineBannerStroke, lineWidth: 1))
    }
}

// MARK: - Skeletons

/// Shimmering placeholder bar used by the loading skeleton.
struct AppStateSkeletonBar: View {
    var width: CGFloat
    var height: CGFloat = BusinessAppStateTokens.skeletonBarHeight
    var radius: CGFloat = BusinessAppStateTokens.skeletonBarRadius
    @State private var shift: CGFloat = -1

    var body: some View {
        RoundedRectangle(cornerRadius: radius)
            .fill(BusinessAppStateTokens.skeletonBarBase)
            .overlay(
                GeometryReader { geo in
                    LinearGradient(
                        colors: [
                            BusinessAppStateTokens.skeletonBarBase,
                            BusinessAppStateTokens.skeletonBarHighlight,
                            BusinessAppStateTokens.skeletonBarBase,
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: geo.size.width * 0.55)
                    .offset(x: shift * geo.size.width)
                }
                .clipShape(RoundedRectangle(cornerRadius: radius))
            )
            .frame(width: width, height: height)
            .onAppear {
                withAnimation(.linear(duration: 1.4).repeatForever(autoreverses: false)) {
                    shift = 1.8
                }
            }
    }
}

/// Glass skeleton container used by the loading body. Figma: w354 h150, 20/22 padding, 14pt gap.
struct AppStateSkeletonBox<Content: View>: View {
    var radius: CGFloat = BusinessAppStateTokens.cardRadius
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: BusinessAppStateTokens.skeletonGap) {
            content()
        }
        .padding(.horizontal, BusinessAppStateTokens.skeletonPadHorizontal)
        .padding(.vertical, BusinessAppStateTokens.skeletonPadVertical)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: radius)
                .fill(BusinessAppStateTokens.glassCardFill)
        )
        .overlay(
            RoundedRectangle(cornerRadius: radius)
                .stroke(BusinessAppStateTokens.glassCardStroke, lineWidth: BusinessAppStateTokens.glassCardStrokeWidth)
        )
    }
}

// MARK: - Shared chrome bits

/// Divider used by list-style shells.
struct AppStateDivider: View {
    var body: some View {
        Rectangle()
            .fill(BusinessAppStateTokens.emerald.opacity(0.14))
            .frame(height: 1)
    }
}

/// Icon button used for close / settings affordances.
struct AppStateIconButton: View {
    let systemName: String
    let accessibilityLabel: String
    let action: () -> Void
    var size: CGFloat = 40

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: size * 0.4, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: size, height: size)
                .background(Circle().fill(Color.white.opacity(0.06)))
                .overlay(Circle().stroke(BusinessAppStateTokens.emerald.opacity(0.18), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}
