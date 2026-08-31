package com.example.momentra.ui.shell.empty.group

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay

internal val GeBg = Color(0xFF131313)
internal val GeText = Color(0xFFE5E2E1)
internal val GeSecondary = Color(0xFFDFC0B4)
internal val GeOrange = Color(0xFFFF7A3D)
internal val GeOrangeSoft = Color(0xFFFFB598)
internal val GeCtaText = Color(0xFF591C00)
internal val GeCard = Color(0xFF201F1F)
internal val GeBorder = Color.White.copy(alpha = 0.10f)
internal val GeSurfaceHigh = Color(0xFF2A2A2A)

internal val GeCtaBrush = Brush.linearGradient(
    listOf(Color(0xFFFF7A3D), Color(0xFFFFB598)),
)

@Composable
internal fun GeScanJoinButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, GeOrange, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "Scan QR to join"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Scan QR to join",
            color = GeOrange,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
        )
    }
}

@Composable
internal fun GeChapterLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = GeOrange,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
    )
}

@Composable
internal fun GeOrangeCta(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GeCtaBrush)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = GeCtaText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Pixel-faithful Figma hero export (copy + CTA baked in).
 * [ctaFractionFromBottom] is the tappable CTA band height as a fraction of the image.
 */
@Composable
internal fun GeFigmaHeroExport(
    resId: Int,
    aspectRatio: Float,
    onCta: () -> Unit,
    ctaLabel: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
        // Transparent CTA hit target over the baked-in button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .height(56.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = ctaLabel
                }
                .clickable(onClick = onCta),
        )
    }
}

@Composable
internal fun GeHeroImage(
    resId: Int,
    height: Dp,
    contentScale: ContentScale = ContentScale.Crop,
    overlayAlpha: Float = 0.5f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF14121B).copy(alpha = overlayAlpha)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun GeAppear(
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        shown = true
    }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "geAppear",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = appear
            translationY = (1f - appear) * 18f
        },
    ) {
        content()
    }
}

@Composable
internal fun GeShimmerBar(accent: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "geShimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "geShimmerShift",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(accent.copy(alpha = 0.22f)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationX = size.width * shift }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
internal fun GeTypeCard(
    imageRes: Int,
    badgeRes: Int? = R.drawable.group_moments_type_badge,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 192.dp,
    comingSoon: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (comingSoon) 22.dp else 24.dp))
            .border(1.dp, GeBorder, RoundedCornerShape(if (comingSoon) 22.dp else 24.dp))
            .background(GeCard)
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics {
                            role = Role.Button
                            contentDescription = title
                        }
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (comingSoon) 170.dp else imageHeight)
                .then(if (comingSoon) Modifier.alpha(0.5f) else Modifier),
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            if (badgeRes != null && !comingSoon) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GeOrange.copy(alpha = 0.2f))
                        .border(1.dp, GeBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(badgeRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(if (comingSoon) 20.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    title,
                    color = GeText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                if (comingSoon) {
                    Text(
                        "Coming Soon",
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
            Text(body, color = GeSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

/** Shared Figma type grid used by Pulse, Moments, and Create. */
@Composable
internal fun GeMomentTypeGrid(
    onSelectExperience: (() -> Unit)? = null,
    onSelectPurchase: (() -> Unit)? = null,
    onSelectLiving: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GeTypeCard(
                imageRes = R.drawable.group_moments_type_experience,
                title = "Experience",
                body = "Trips, weddings, celebrations, outings and events.",
                modifier = Modifier.weight(1f),
                onClick = onSelectExperience,
            )
            GeTypeCard(
                imageRes = R.drawable.group_moments_type_purchase,
                title = "Purchase",
                body = "Plan, fund and track something together.",
                modifier = Modifier.weight(1f),
                onClick = onSelectPurchase,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GeTypeCard(
                imageRes = R.drawable.group_moments_type_living,
                title = "Living",
                body = "Coordinate a home, routine or shared space.",
                modifier = Modifier.weight(1f),
                onClick = onSelectLiving,
            )
            GeTypeCard(
                imageRes = R.drawable.group_moments_type_goal,
                badgeRes = null,
                title = "Goal",
                body = "Turn a shared ambition into steady progress.",
                modifier = Modifier.weight(1f),
                comingSoon = true,
            )
        }
        GeTypeCard(
            imageRes = R.drawable.group_moments_type_community,
            title = "Community",
            body = "Bring a wider circle around a common purpose.",
            modifier = Modifier.fillMaxWidth(),
            comingSoon = true,
        )
    }
}

/** Full Figma card export (image + copy baked in). */
@Composable
internal fun GeFigmaCardExport(
    resId: Int,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
internal fun GeFeatureRow(
    iconRes: Int,
    title: String,
    body: String,
    tintIcon: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GeBorder, RoundedCornerShape(16.dp))
            .background(GeCard)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GeOrange.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = if (tintIcon) ColorFilter.tint(GeOrange) else null,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title.uppercase(),
                color = GeText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
            )
            Text(body, color = GeSecondary, fontSize = 16.sp, lineHeight = 23.sp)
        }
    }
}

@Composable
internal fun GeSectionSpacer(height: Dp = 8.dp) {
    Spacer(modifier = Modifier.height(height))
}
