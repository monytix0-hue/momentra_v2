package com.example.momentra.ui.shell.business.appstate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Tone of an app-state surface. Error frames swap the emerald accent orb for the diagnostic red
 * while keeping every emerald structural stroke, which is exactly what Figma draws.
 *
 * Keep in lockstep with `AppStateTone` on iOS.
 */
enum class AppStateTone {
    Normal,
    Error,
    ;

    val isError: Boolean get() = this == Error
}

/**
 * Body-only primitives for the Figma "App States · Shared (all Business verticals)" kit.
 *
 * The kit draws a full 402x874 screen, but the shell already owns the status bar, top bar,
 * context switcher, moment selector and bottom nav. Only the "State body" region and the glow
 * mesh behind it are rendered here, mounted inside existing chrome.
 *
 * Rebuilt against Figma "Obsidian & Neon Refraction" (`2852:39645`).
 *
 * Keep in lockstep with `BusinessAppStateKit.swift` on iOS.
 */

private val CardShape = RoundedCornerShape(BusinessAppStateTokens.CardRadius)
private val ButtonShape = RoundedCornerShape(BusinessAppStateTokens.ButtonRadius)

/**
 * Volumetric dual-orb mesh behind the state body.
 *
 * Figma: an accent orb (`#10B981`, 360dp, 28% → 8% @60% → 0) with a warm `#059669` orb (280dp,
 * 15% → 0) drifting off its lower-right shoulder. Error frames drop the warm orb and paint the
 * accent orb `#FF5C38` at 25%.
 */
@Composable
fun AppStateGlow(
    accent: Color,
    modifier: Modifier = Modifier,
    tone: AppStateTone = AppStateTone.Normal,
    size: Dp = BusinessAppStateTokens.GlowAccentSize,
) {
    val accentColor = if (tone.isError) BusinessAppStateTokens.GlowError else accent
    val accentCore = if (tone.isError) BusinessAppStateTokens.GlowErrorAlpha else BusinessAppStateTokens.GlowAccentAlpha
    val accentMid = if (tone.isError) {
        BusinessAppStateTokens.GlowErrorAlpha * 0.34f
    } else {
        BusinessAppStateTokens.GlowAccentMidAlpha
    }

    Box(modifier = modifier.size(size)) {
        GlowOrb(
            color = accentColor,
            coreAlpha = accentCore,
            midAlpha = accentMid,
            size = size,
            modifier = Modifier.offset(y = -(size * 0.10f)),
        )
        if (!tone.isError) {
            GlowOrb(
                color = BusinessAppStateTokens.GlowWarm,
                coreAlpha = BusinessAppStateTokens.GlowWarmAlpha,
                midAlpha = 0f,
                size = BusinessAppStateTokens.GlowWarmSize,
                modifier = Modifier.offset(x = size * 0.24f, y = size * 0.20f),
            )
        }
    }
}

/** One soft radial orb in the mesh. */
@Composable
private fun GlowOrb(
    color: Color,
    coreAlpha: Float,
    midAlpha: Float,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .blur(radius = size * 0.06f),
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = coreAlpha),
                BusinessAppStateTokens.GlowAccentMidStop to color.copy(alpha = midAlpha),
                1f to Color.Transparent,
                center = center,
                radius = this.size.minDimension / 2f,
            ),
        )
    }
}

/**
 * Floating hero indicator badge: outer halo ring, obsidian core with the state glyph, and a
 * state-coloured status dot riding the lower-right corner.
 *
 * Figma geometry at the 112dp reference: halo 96 inset 8 (emerald @8% fill, emerald @45% 2dp
 * ring), core 76 inset 18 (`#0F141F` fill, emerald @18% 1.5dp ring, 28dp white glyph), status dot
 * 36 at (72,72) with a 3.5dp emerald ring and a 17sp `#080A0F` glyph. Everything below scales off
 * `size / 112`.
 */
@Composable
fun AppStateMarkRing(
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = BusinessAppStateTokens.HeroSize,
    tone: AppStateTone = AppStateTone.Normal,
    dotGlyph: String = "✓",
) {
    val t = BusinessAppStateTokens
    val scale = size.value / t.HeroSize.value
    val dotFill = if (tone.isError) t.ErrorRing else accent

    Box(modifier = modifier.size(size)) {
        // Outer halo ring.
        Box(
            modifier = Modifier
                .offset(x = t.HeroRingInset * scale, y = t.HeroRingInset * scale)
                .size(t.HeroRingSize * scale)
                .background(t.Emerald.copy(alpha = t.HeroRingFillAlpha), CircleShape)
                .border(t.HeroRingStroke * scale, t.Emerald.copy(alpha = t.HeroRingStrokeAlpha), CircleShape),
        )
        // Obsidian core with the state glyph.
        Box(
            modifier = Modifier
                .offset(x = t.HeroCoreInset * scale, y = t.HeroCoreInset * scale)
                .size(t.HeroCoreSize * scale)
                .clip(CircleShape)
                .background(t.HeroCoreFill)
                .border(t.HeroCoreStroke * scale, t.Emerald.copy(alpha = t.HeroCoreStrokeAlpha), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(t.HeroGlyphSize * scale),
            )
        }
        // Floating status dot — emerald ring in both tones, only the fill flips.
        Box(
            modifier = Modifier
                .offset(x = t.HeroDotInset * scale, y = t.HeroDotInset * scale)
                .size(t.HeroDotSize * scale)
                .clip(CircleShape)
                .background(dotFill)
                .border(t.HeroDotStroke * scale, t.Emerald, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (tone.isError) "!" else dotGlyph,
                color = t.HeroDotGlyphColor,
                fontSize = (t.HeroDotGlyphSize.value * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

/** Section eyebrow inside a glass card. Figma: 11sp ExtraBold, tracking 1.5, emerald. */
@Composable
fun AppStateEyebrow(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        color = accent,
        fontSize = BusinessAppStateTokens.EyebrowSize.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = BusinessAppStateTokens.EyebrowTracking.sp,
        fontFamily = PlusJakartaSans,
        modifier = modifier,
    )
}

/**
 * Refraction glass card: `rgba(16,22,35,0.72)` over the glow, 1.25dp emerald @35% border,
 * radius 22, 20/18 padding, 12dp row gap. The flat translucent fill *is* the Figma effect — the
 * old white linear-gradient sheen is gone on purpose.
 */
@Composable
fun AppStateGlassCard(
    modifier: Modifier = Modifier,
    /** Border hue. Error diagnostics swap the emerald stroke for the diagnostic red. */
    accent: Color = BusinessAppStateTokens.GlassCardStroke,
    strokeAlpha: Float = 1f,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.CardRowGap),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            // Figma's effect is the flat translucent `rgba(16,22,35,0.72)` fill over the glow —
            // there is no white sheen overlay, so none is drawn.
            .background(BusinessAppStateTokens.GlassCardFill)
            .border(
                BusinessAppStateTokens.GlassCardStrokeWidth,
                accent.copy(alpha = accent.alpha * strokeAlpha),
                CardShape,
            )
            .padding(
                horizontal = BusinessAppStateTokens.CardPaddingHorizontal,
                vertical = BusinessAppStateTokens.CardPaddingVertical,
            ),
        content = content,
    )
}

/** One label/value line inside a glass card. */
@Composable
fun AppStateGlassRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = BusinessAppStateTokens.Text,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = BusinessAppStateTokens.RowLabelColor,
            fontSize = BusinessAppStateTokens.RowLabelSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f, fill = true),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = BusinessAppStateTokens.RowValueSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/**
 * Primary CTA. Figma: `#059669 → #34D399` gradient, radius 999, height 50, 13.5sp ExtraBold
 * tracking 1, emerald glow shadow.
 */
@Composable
fun AppStatePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BusinessAppStateTokens.PrimaryButtonHeight)
            // Figma `0 4px 12px rgba(16,185,129,0.35)`. Coloured shadows are API 28+, so the
            // emerald halo is drawn explicitly to keep the look on minSdk 24.
            .drawBehind {
                val spread = BusinessAppStateTokens.CtaShadowRadius.toPx()
                val drop = BusinessAppStateTokens.CtaShadowY.toPx()
                repeat(4) { i ->
                    val t = (i + 1) / 4f
                    drawRoundRect(
                        color = BusinessAppStateTokens.CtaShadow.copy(alpha = 0.35f / (i + 1)),
                        topLeft = Offset(-spread * t, drop - spread * t),
                        size = androidx.compose.ui.geometry.Size(
                            width = size.width + spread * 2 * t,
                            height = size.height + spread * 2 * t,
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f + spread * t),
                    )
                }
            }
            .clip(ButtonShape)
            .background(
                Brush.horizontalGradient(
                    listOf(BusinessAppStateTokens.CtaStart, BusinessAppStateTokens.CtaEnd),
                ),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White,
            fontSize = BusinessAppStateTokens.ButtonLabelSize.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = BusinessAppStateTokens.ButtonTracking.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

/** Secondary CTA. Figma: no fill, 1dp emerald @18% border, radius 999, height 46, 13.5sp Bold. */
@Composable
fun AppStateSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BusinessAppStateTokens.SecondaryButtonHeight)
            .clip(ButtonShape)
            .background(if (filled) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .border(1.dp, BusinessAppStateTokens.SecondaryButtonStroke, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = BusinessAppStateTokens.ButtonLabelSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/**
 * Diagnostic pill on error states. Figma: `#FF5C38` @15% fill, emerald @45% border, 11sp
 * ExtraBold tracking 1.5 label in `#FF5C38`.
 */
@Composable
fun AppStateErrorCodePill(
    code: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(ButtonShape)
            .background(BusinessAppStateTokens.ErrorPillFill)
            .border(1.dp, BusinessAppStateTokens.ErrorPillStroke, ButtonShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = code,
            color = accent,
            fontSize = BusinessAppStateTokens.ErrorCodeSize.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = BusinessAppStateTokens.ErrorCodeTracking.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

/**
 * Centred offline status pill. Figma: `rgba(38,46,61,0.9)` fill, emerald @40% border, height 34,
 * 11.5sp SemiBold white. No icon, no amber.
 */
@Composable
fun AppStateOfflineBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(ButtonShape)
            .background(BusinessAppStateTokens.OfflineBannerFill)
            .border(1.dp, BusinessAppStateTokens.OfflineBannerStroke, ButtonShape)
            .height(BusinessAppStateTokens.OfflineBannerHeight)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = BusinessAppStateTokens.OfflineBannerLabelSize.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 15.sp,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
    }
}

/** Muted footnote under the actions. Figma: 11sp Medium, `#738099`, line-height 15. */
@Composable
fun AppStateFootnote(
    text: String,
    modifier: Modifier = Modifier,
    opacity: Float = BusinessAppStateTokens.FootnoteMutedOpacity,
) {
    Text(
        text = text,
        color = BusinessAppStateTokens.TextSubtle.copy(
            alpha = BusinessAppStateTokens.TextSubtle.alpha * opacity,
        ),
        fontSize = BusinessAppStateTokens.FootnoteSize.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 15.sp,
        fontFamily = PlusJakartaSans,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Title. Figma: 24sp Bold, line-height 30, white. */
@Composable
fun AppStateTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        color = Color.White,
        fontSize = BusinessAppStateTokens.TitleSize.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 30.sp,
        fontFamily = PlusJakartaSans,
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Body copy. Figma: 13.5sp Medium, `#94A4B8`, line-height 19. */
@Composable
fun AppStateBodyText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        color = BusinessAppStateTokens.TextMuted,
        fontSize = BusinessAppStateTokens.BodySize.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = BusinessAppStateTokens.BodyLineHeight.sp,
        fontFamily = PlusJakartaSans,
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Shimmering placeholder bar used by the loading skeleton. Base 4% white → 18% at 50%. */
@Composable
fun AppStateSkeletonBar(
    width: Dp,
    height: Dp = BusinessAppStateTokens.SkeletonBarHeight,
    modifier: Modifier = Modifier,
    radius: Dp = BusinessAppStateTokens.SkeletonBarRadius,
) {
    val transition = rememberInfiniteTransition(label = "appStateSkeleton")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "appStateSkeletonShift",
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(BusinessAppStateTokens.SkeletonBarBase)
            .drawBehind {
                val band = size.width * 0.55f
                val start = shift * size.width
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BusinessAppStateTokens.SkeletonBarBase,
                            BusinessAppStateTokens.SkeletonBarHighlight,
                            BusinessAppStateTokens.SkeletonBarBase,
                        ),
                        startX = start,
                        endX = start + band,
                    ),
                )
            },
    )
}

/** Glass skeleton container used by the loading body. Figma: w354 h150, 20/22 padding, 14dp gap. */
@Composable
fun AppStateSkeletonBox(
    modifier: Modifier = Modifier,
    radius: Dp = BusinessAppStateTokens.CardRadius,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.SkeletonGap),
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(BusinessAppStateTokens.GlassCardFill)
            .border(
                BusinessAppStateTokens.GlassCardStrokeWidth,
                BusinessAppStateTokens.GlassCardStroke,
                RoundedCornerShape(radius),
            )
            .padding(
                horizontal = BusinessAppStateTokens.SkeletonPadHorizontal,
                vertical = BusinessAppStateTokens.SkeletonPadVertical,
            ),
        content = content,
    )
}

/** Divider used by list-style shells. */
@Composable
fun AppStateDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BusinessAppStateTokens.Emerald.copy(alpha = 0.14f)),
    )
}

/** Icon button used for close / settings affordances. */
@Composable
fun AppStateIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, BusinessAppStateTokens.HairlineStroke, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.4f),
        )
    }
}
