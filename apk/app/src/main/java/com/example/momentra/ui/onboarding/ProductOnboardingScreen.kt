package com.example.momentra.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.momentra.ui.shell.maestro.MaestroIds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.AnalyticsWidgets
import com.example.momentra.analytics.TrackScreen
import com.example.momentra.analytics.trackWidget
import com.example.momentra.ui.theme.MomentraBrandColors

/** Figma frame Android / Onboarding / Product * — 402 × 874 */
private const val DesignW = 402f
private const val DesignH = 874f

private val ProductMidnight = Color(0xFF050816)
private val CtaOrange = Color(0xFFFF7A3D)
private val SkipColor = Color(0xFFF5F0FF).copy(alpha = 0.58f)
private val BodyColor = Color(0xFFF5F0FF).copy(alpha = 0.62f)

private data class ProductPage(
    val illustrationRes: Int,
    val illustrationTop: Float,
    val illustrationSize: Float,
    val copyTop: Float,
    val eyebrow: String,
    val eyebrowColor: Color,
    val title: String,
    val body: String,
    val analyticsScreen: String,
    val cta: String,
)

private val ProductPages = listOf(
    ProductPage(
        illustrationRes = R.drawable.onboarding_ill_pulse,
        illustrationTop = 118f,
        illustrationSize = 340f,
        copyTop = 492f,
        eyebrow = "YOUR DAILY PULSE",
        eyebrowColor = MomentraBrandColors.Amber500,
        title = "Everything important,\nat a glance.",
        body = "Money, people, goals, and work come together in one calm view.",
        analyticsScreen = AnalyticsScreens.ONBOARDING_PRODUCT_1,
        cta = "Next",
    ),
    ProductPage(
        illustrationRes = R.drawable.onboarding_ill_moments,
        illustrationTop = 112f,
        illustrationSize = 340f,
        copyTop = 488f,
        eyebrow = "CAPTURE THE MOMENT",
        eyebrowColor = MomentraBrandColors.Teal500,
        title = "Add it once.\nRemember it better.",
        body = "A spend, a feeling, a task, or a memory—Momentra keeps the context with it.",
        analyticsScreen = AnalyticsScreens.ONBOARDING_PRODUCT_2,
        cta = "Next",
    ),
    ProductPage(
        illustrationRes = R.drawable.onboarding_ill_life_memory,
        illustrationTop = 106f,
        illustrationSize = 350f,
        copyTop = 488f,
        eyebrow = "SEE WHAT'S NEXT",
        eyebrowColor = MomentraBrandColors.Amber500,
        title = "Your story\nbecomes insight.",
        body = "Life shows the shape of today. Memory learns the patterns that help tomorrow.",
        analyticsScreen = AnalyticsScreens.ONBOARDING_PRODUCT_3,
        cta = "Get Started",
    ),
)

/**
 * Figma product education carousel (848:11634–11636).
 * Uses exported Figma illustration PNGs and scales the 402×874 frame to the device.
 */
@Composable
fun ProductOnboardingScreen(
    onContinueToCinematic: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    val current = ProductPages[page]
    TrackScreen(current.analyticsScreen)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductMidnight),
    ) {
        val scale = minOf(maxWidth / DesignW.dp, maxHeight / DesignH.dp)
        val frameW = DesignW.dp * scale
        val frameH = DesignH.dp * scale
        fun dx(v: Float): Dp = frameW * (v / DesignW)
        fun dy(v: Float): Dp = frameH * (v / DesignH)
        fun spScaled(v: Float) = (v * scale).sp

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(frameW)
                .height(frameH),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = dy(current.illustrationTop - 20f))
                    .size(dx(300f))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MomentraBrandColors.Indigo700.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )

            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "productPage",
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                val p = ProductPages[index]
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(p.illustrationRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = dy(p.illustrationTop))
                            .size(dx(p.illustrationSize)),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = dy(p.copyTop))
                            .width(dx(354f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dy(10f)),
                    ) {
                        Text(
                            text = p.eyebrow,
                            color = p.eyebrowColor,
                            fontSize = spScaled(11f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (1.4f * scale).sp,
                        )
                        Text(
                            text = p.title,
                            color = MomentraBrandColors.TextOnDark,
                            fontSize = spScaled(30f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = spScaled(36f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = p.body,
                            color = BodyColor,
                            fontSize = spScaled(15f),
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = spScaled(22f),
                            modifier = Modifier.width(dx(330f)),
                        )
                    }
                }
            }

            Image(
                painter = painterResource(R.drawable.onboarding_brand_mark),
                contentDescription = "Momentra",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = dx(24f), top = dy(40f))
                    .size(dx(28f)),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = "Skip",
                color = SkipColor,
                fontSize = spScaled(13f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = dy(43f), end = dx(24f))
                    .testTag(MaestroIds.ONBOARDING_SKIP)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Skip"
                    }
                    .clickable {
                        trackWidget(AnalyticsScreens.ONBOARDING, AnalyticsWidgets.ONBOARDING_SKIP, "tap")
                        onContinueToCinematic()
                    },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = dy(746f))
                    .width(dx(362f))
                    .height(dy(88f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dy(16f), Alignment.CenterVertically),
            ) {
                PageDots(
                    activeIndex = page,
                    count = ProductPages.size,
                    dot = dx(8f),
                    activeWidth = dx(22f),
                )
                Box(
                    modifier = Modifier
                        .width(dx(320f))
                        .height(dy(52f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(CtaOrange)
                        .semantics {
                            role = Role.Button
                            contentDescription = current.cta
                        }
                        .clickable {
                            trackWidget(
                                AnalyticsScreens.ONBOARDING,
                                if (page == ProductPages.lastIndex) {
                                    AnalyticsWidgets.ONBOARDING_GET_STARTED
                                } else {
                                    AnalyticsWidgets.ONBOARDING_NEXT
                                },
                                "tap",
                            )
                            if (page < ProductPages.lastIndex) {
                                page += 1
                            } else {
                                onContinueToCinematic()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = current.cta,
                        color = Color.White,
                        fontSize = spScaled(17f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(
    activeIndex: Int,
    count: Int,
    dot: Dp,
    activeWidth: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dot),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(dot),
    ) {
        repeat(count) { i ->
            if (i == activeIndex) {
                Box(
                    modifier = Modifier
                        .width(activeWidth)
                        .height(dot)
                        .clip(RoundedCornerShape(999.dp))
                        .background(CtaOrange),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(dot)
                        .clip(CircleShape)
                        .background(Color(0xFF3A3558)),
                )
            }
        }
    }
}
