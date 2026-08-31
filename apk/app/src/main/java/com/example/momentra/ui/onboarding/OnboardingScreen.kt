package com.example.momentra.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.AnalyticsWidgets
import com.example.momentra.analytics.TrackScreen
import com.example.momentra.analytics.trackWidget
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.MomentraBrandColors
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.isActive

private val SCENE_IDS = listOf("onboarding_1", "onboarding_2", "onboarding_3")
private const val PARTICLE_COUNT = 80
private const val SCENE1_END = 2000L
private const val SCENE2_END = 5000L
private const val SCENE3_END = 7000L
private const val CTA_AT = 7200L
private val Midnight = Color(0xFF050816)

enum class OnboardingMode {
    FirstRun,
    Replay,
}

private data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var ox: Float,
    var oy: Float,
    var tx: Float,
    var ty: Float,
    var r: Float,
    var opacity: Float,
)

private fun easeInOut(t: Float): Float =
    if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).let { it * it } / 2f

private fun sampleMTargets(count: Int, w: Float, h: Float): List<Offset> {
    val path = listOf(
        Offset(14f, 100f), Offset(14f, 50f), Offset(34f, 74f), Offset(54f, 24f),
        Offset(54f, 100f), Offset(54f, 24f), Offset(74f, 74f), Offset(94f, 50f), Offset(94f, 100f),
    )
    val scale = minOf(w, h) * 0.42f
    val cx = w * 0.5f
    val cy = h * 0.38f
    return List(count) { i ->
        val t = (i.toFloat() / count) * (path.lastIndex)
        val i0 = t.toInt().coerceAtMost(path.lastIndex - 1)
        val i1 = (i0 + 1).coerceAtMost(path.lastIndex)
        val f = t - i0
        val x = path[i0].x + (path[i1].x - path[i0].x) * f
        val y = path[i0].y + (path[i1].y - path[i0].y) * f
        Offset(
            cx + ((x - 54f) / 120f) * scale * 2.2f,
            cy + ((y - 62f) / 120f) * scale * 2.0f,
        )
    }
}

private fun createParticles(w: Float, h: Float, reduceMotion: Boolean): MutableList<Particle> {
    val targets = sampleMTargets(PARTICLE_COUNT, w, h)
    val rnd = Random(42)
    return MutableList(PARTICLE_COUNT) { i ->
        val x = if (reduceMotion) targets[i].x else rnd.nextFloat() * w
        val y = if (reduceMotion) targets[i].y else rnd.nextFloat() * h
        Particle(
            x = x,
            y = y,
            z = 0.4f + rnd.nextFloat() * 0.6f,
            vx = (rnd.nextFloat() - 0.5f) * 0.15f,
            vy = (rnd.nextFloat() - 0.5f) * 0.15f,
            ox = x,
            oy = y,
            tx = targets[i].x,
            ty = targets[i].y,
            r = 1.2f + rnd.nextFloat() * 2.2f,
            opacity = 0.25f + rnd.nextFloat() * 0.55f,
        )
    }
}

/**
 * First-run / replay onboarding:
 * 1) Figma product carousel (Pulse → Moments → Life+Memory)
 * 2) Existing cinematic particle welcome
 */
@Composable
fun OnboardingScreen(
    mode: OnboardingMode = OnboardingMode.FirstRun,
    onFinished: () -> Unit,
) {
    var stage by remember { mutableStateOf(OnboardingStage.Product) }

    when (stage) {
        OnboardingStage.Product -> ProductOnboardingScreen(
            onContinueToCinematic = { stage = OnboardingStage.Cinematic },
        )
        OnboardingStage.Cinematic -> CinematicOnboardingScreen(
            mode = mode,
            onFinished = onFinished,
        )
    }
}

private enum class OnboardingStage { Product, Cinematic }

/**
 * Cinematic welcome — particle network morphs into Momentra M.
 * Gate/prefs/replay contract preserved via [OnboardingMode] + [onFinished].
 */
@Composable
private fun CinematicOnboardingScreen(
    mode: OnboardingMode = OnboardingMode.FirstRun,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val reduceMotion = false

    var scene by remember { mutableIntStateOf(if (reduceMotion) 2 else 0) }
    var showCta by remember { mutableStateOf(reduceMotion) }
    var exiting by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableFloatStateOf(if (reduceMotion) CTA_AT.toFloat() else 0f) }
    var exitT by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val particles = remember { mutableStateOf<MutableList<Particle>>(mutableListOf()) }
    val finished = remember { mutableStateOf(false) }
    val latestOnFinished by rememberUpdatedState(onFinished)
    val frameTick = remember { mutableIntStateOf(0) }

    fun finish(reason: String) {
        if (finished.value) return
        finished.value = true
        if (mode == OnboardingMode.FirstRun) {
            prefs.setOnboardingSeen(true)
        }
        latestOnFinished()
    }

    LaunchedEffect(canvasSize, reduceMotion) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        particles.value = createParticles(w, h, reduceMotion)
        if (reduceMotion) return@LaunchedEffect

        val start = withFrameMillis { it }
        while (isActive && !finished.value) {
            withFrameMillis { now ->
                val elapsed = (now - start).toFloat()
                elapsedMs = elapsed
                val si = when {
                    elapsed < SCENE1_END -> 0
                    elapsed < SCENE2_END -> 1
                    else -> 2
                }
                if (si != scene) scene = si
                if (elapsed >= CTA_AT && !showCta && !exiting) showCta = true

                val morph = when {
                    elapsed >= SCENE2_END -> easeInOut(((elapsed - SCENE2_END) / 2000f).coerceIn(0f, 1f))
                    else -> 0f
                }
                val list = particles.value
                for (p in list) {
                    if (exiting) {
                        val dx = p.x - w / 2f
                        val dy = p.y - h / 2f
                        p.x += dx * 0.02f * (1f + exitT * 3f)
                        p.y += dy * 0.02f * (1f + exitT * 3f)
                        p.z += 0.02f
                        p.opacity *= 0.985f
                    } else if (morph > 0.001f) {
                        p.ox += p.vx * (1f - morph)
                        p.oy += p.vy * (1f - morph)
                        if (p.ox < 0 || p.ox > w) p.vx *= -1
                        if (p.oy < 0 || p.oy > h) p.vy *= -1
                        p.x = p.ox + (p.tx - p.ox) * morph
                        p.y = p.oy + (p.ty - p.oy) * morph
                    } else {
                        p.x += p.vx
                        p.y += p.vy
                        if (p.x < 0 || p.x > w) p.vx *= -1
                        if (p.y < 0 || p.y > h) p.vy *= -1
                        p.ox = p.x
                        p.oy = p.y
                    }
                }
                frameTick.intValue = frameTick.intValue + 1
            }
        }
    }

    LaunchedEffect(exiting) {
        if (!exiting) return@LaunchedEffect
        val exitAnim = Animatable(0f)
        exitAnim.animateTo(1f, animationSpec = tween(1400, easing = LinearEasing)) {
            exitT = value
        }
        finish("complete")
    }

    BackHandler(enabled = true) {
        if (!exiting) {
            trackWidget(AnalyticsScreens.ONBOARDING, AnalyticsWidgets.ONBOARDING_SKIP, "back")
            finish("skip")
        }
    }

    val sceneScreen = when (scene) {
        0 -> AnalyticsScreens.ONBOARDING_SCENE_1
        1 -> AnalyticsScreens.ONBOARDING_SCENE_2
        else -> AnalyticsScreens.ONBOARDING_SCENE_3
    }
    TrackScreen(sceneScreen)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Midnight),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it },
        ) {
            // Read frameTick so Compose redraws each frame
            frameTick.intValue
            val w = size.width
            val h = size.height
            val elapsed = elapsedMs
            val morph = if (reduceMotion) 1f
            else if (elapsed >= SCENE2_END) easeInOut(((elapsed - SCENE2_END) / 2000f).coerceIn(0f, 1f))
            else 0f
            val linkAlpha = when {
                reduceMotion -> 0.15f
                elapsed >= SCENE2_END -> 1f - morph * 0.85f
                elapsed >= SCENE1_END -> easeInOut(((elapsed - SCENE1_END) / 800f).coerceIn(0f, 1f))
                else -> 0f
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x592D1F5E), Color.Transparent),
                    center = Offset(w * 0.7f, h * 0.2f),
                    radius = w * 0.45f,
                ),
            )

            val list = particles.value
            if (linkAlpha > 0.02f && !exiting) {
                val maxDist = minOf(w, h) * 0.12f
                val maxDist2 = maxDist * maxDist
                var edges = 0
                for (i in list.indices) {
                    if (edges >= 90) break
                    for (j in i + 1 until list.size) {
                        if (edges >= 90) break
                        val a = list[i]
                        val b = list[j]
                        val dx = a.x - b.x
                        val dy = a.y - b.y
                        val d2 = dx * dx + dy * dy
                        if (d2 < maxDist2) {
                            val alpha = (1f - sqrt(d2) / maxDist) * linkAlpha * 0.45f
                            drawLine(
                                color = Color(0xFFB4A0FF).copy(alpha = alpha),
                                start = Offset(a.x, a.y),
                                end = Offset(b.x, b.y),
                                strokeWidth = 1.2f,
                                cap = StrokeCap.Round,
                            )
                            edges++
                        }
                    }
                }
            }

            for (p in list) {
                val glow = 6f + p.z * 6f
                val base = if (morph > 0.5f) MomentraBrandColors.Ember500 else Color(0xFFC8BEFF)
                val op = p.opacity * if (exiting) (1f - exitT * 0.3f) else 1f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(base.copy(alpha = op), base.copy(alpha = 0f)),
                        center = Offset(p.x, p.y),
                        radius = glow,
                    ),
                    radius = glow,
                    center = Offset(p.x, p.y),
                )
                drawCircle(
                    color = MomentraBrandColors.TextOnDark.copy(alpha = op * 0.9f),
                    radius = p.r * p.z * (1f + exitT * 2f),
                    center = Offset(p.x, p.y),
                )
            }

            if (exitT > 0f) {
                drawRect(MomentraBrandColors.TextOnDark.copy(alpha = exitT * 0.55f))
                drawRect(MomentraBrandColors.Indigo700.copy(alpha = exitT * 0.25f))
            }
        }

        TextButton(
            onClick = {
                trackWidget(AnalyticsScreens.ONBOARDING, AnalyticsWidgets.ONBOARDING_SKIP, "tap")
                finish("skip")
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
                .testTag(MaestroIds.ONBOARDING_SKIP),
        ) {
            Text(
                text = "Skip",
                color = MomentraBrandColors.TextOnDark.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (scene) {
                0 -> WelcomeSentence("Life is already unfolding.")
                1 -> WelcomeSentence("Keep what matters together.")
                else -> {
                    Spacer(Modifier.height(112.dp))
                    Text(
                        text = "Momentra",
                        color = MomentraBrandColors.TextOnDark,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "One place for every moment.",
                        color = MomentraBrandColors.TextOnDark.copy(alpha = 0.55f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (showCta && !exiting) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.78f)
                    .height(52.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(MomentraBrandColors.Ember500, MomentraBrandColors.Amber500),
                        ),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Step Inside"
                    }
                    .clickable {
                        trackWidget(AnalyticsScreens.ONBOARDING, AnalyticsWidgets.ONBOARDING_STEP_INSIDE, "tap")
                        exiting = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Step Inside",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun WelcomeSentence(text: String) {
    Text(
        text = text,
        color = MomentraBrandColors.TextOnDark,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        lineHeight = 34.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}
