package com.example.momentra.ui.shell.business.appstate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Body-only primitives for the Figma "App States · Shared (all Business verticals)" kit.
 *
 * The kit draws a full 402x874 screen, but the shell already owns the status bar, top bar,
 * context switcher, moment selector and bottom nav. Only the "State body" region and the glow
 * ellipse behind it are rendered here, mounted inside existing chrome.
 */

private val BodyShape = RoundedCornerShape(BusinessAppStateTokens.CardRadius)

/** Soft accent wash behind the state body. Figma: 440dp ellipse, accent @12%, layer blur 160. */
@Composable
fun AppStateGlow(
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = BusinessAppStateTokens.GlowSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = BusinessAppStateTokens.GlowAlpha),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.toPx() / 2f,
                    ),
                )
            },
    )
}

/**
 * Hero mark ring: blurred halo, accent ring, brand-mark core, accent badge with a glyph.
 *
 * Figma geometry for the 120dp variant: halo 120, ring 92 inset 14 with 3dp stroke,
 * mark 80 inset 20, badge 40 at bottom-right with a 3dp page-coloured border.
 */
@Composable
fun AppStateMarkRing(
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    markRes: Int = R.drawable.momentra_official_logo,
) {
    val scale = size.value / 120f
    val ringSize = (92f * scale).dp
    val ringInset = (14f * scale).dp
    val markSize = (80f * scale).dp
    val markInset = (20f * scale).dp
    val badgeSize = (40f * scale).dp
    val badgeInset = (76f * scale).dp
    val badgeBorder = (3f * scale).dp

    Box(modifier = modifier.size(size)) {
        // halo
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                            center = center,
                            radius = this.size.minDimension / 2f,
                        ),
                    )
                },
        )
        // ring
        Box(
            modifier = Modifier
                .offset(x = ringInset, y = ringInset)
                .size(ringSize)
                .border(badgeBorder, accent, CircleShape),
        )
        // brand mark core
        Box(
            modifier = Modifier
                .offset(x = markInset, y = markInset)
                .size(markSize)
                .clip(CircleShape)
                .background(Color(0xFF14121B))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(markRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(markSize * 0.62f),
            )
        }
        // badge
        Box(
            modifier = Modifier
                .offset(x = badgeInset, y = badgeInset)
                .size(badgeSize)
                .clip(CircleShape)
                .background(accent)
                .border(badgeBorder, BusinessAppStateTokens.Page, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((18f * scale).dp),
            )
        }
    }
}

/** Section eyebrow inside a glass card. Figma: 12sp ExtraBold, tracking 2, accent. */
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

/** Glass card: 8% white fill, 1dp accent stroke, radius 20, 16/18 padding. */
@Composable
fun AppStateGlassCard(
    accent: Color,
    modifier: Modifier = Modifier,
    strokeAlpha: Float = 1f,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(BodyShape)
            .background(BusinessAppStateTokens.GlassFill)
            .border(1.dp, accent.copy(alpha = strokeAlpha), BodyShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
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
            color = Color.White.copy(alpha = 0.75f),
            fontSize = BusinessAppStateTokens.RowLabelSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.weight(1f, fill = true),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = BusinessAppStateTokens.RowValueSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/** Primary CTA. Figma: orange ramp, radius 999, height 50, 14sp ExtraBold tracking 1. */
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
            .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
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

/** Secondary CTA. Figma: glass 8% fill + white 22% stroke, radius 999, height 48. */
@Composable
fun AppStateSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BusinessAppStateTokens.SecondaryButtonHeight)
            .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
            .background(BusinessAppStateTokens.GlassFill)
            .border(
                1.dp,
                BusinessAppStateTokens.GlassStroke,
                RoundedCornerShape(BusinessAppStateTokens.ButtonRadius),
            )
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

/** Diagnostic pill on error states. Figma: accent @14% fill, accent @40% stroke, 11sp ExtraBold. */
@Composable
fun AppStateErrorCodePill(
    code: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
            .padding(horizontal = 13.dp, vertical = 7.dp),
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

/** Muted footnote under the actions. Figma: 11sp SemiBold, white @50%. */
@Composable
fun AppStateFootnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.50f),
        fontSize = BusinessAppStateTokens.FootnoteSize.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 15.sp,
        fontFamily = PlusJakartaSans,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Title. Figma: 24sp ExtraBold, centred. */
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

/** Body copy. Figma: 14sp Medium, white @70%, 1.35 line height. */
@Composable
fun AppStateBodyText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.70f),
        fontSize = BusinessAppStateTokens.BodySize.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 19.sp,
        fontFamily = PlusJakartaSans,
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Shimmering placeholder bar used by the loading skeletons. */
@Composable
fun AppStateSkeletonBar(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp,
) {
    val transition = rememberInfiniteTransition(label = "appStateSkeleton")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "appStateSkeletonShift",
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 1f),
                    ),
                    start = Offset(shift * 220f, 0f),
                    end = Offset(shift * 220f + 120f, 0f),
                ),
            ),
    )
}

/** Glass skeleton container used by the loading body. */
@Composable
fun AppStateSkeletonBox(
    accent: Color,
    modifier: Modifier = Modifier,
    radius: Dp = BusinessAppStateTokens.CardRadius,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(BusinessAppStateTokens.GlassFill)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(radius))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

/** Small circular spinner dot with an accent glow, for the loading label. */
@Composable
fun AppStateSpinnerDot(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "appStateSpinner")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "appStateSpinnerAlpha",
    )
    Box(
        modifier = modifier
            .size(10.dp)
            .drawBehind {
                drawCircle(color = accent.copy(alpha = 0.35f * alpha), radius = size.minDimension)
            }
            .background(accent, CircleShape),
    )
}

/** Divider used by list-style shells. */
@Composable
fun AppStateDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f)),
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
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
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
