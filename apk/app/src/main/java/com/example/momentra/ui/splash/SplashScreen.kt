package com.example.momentra.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.MomentraBrandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val dotsVisible = remember { mutableStateListOf(false, false, false, false, false) }
    val ghostOp = remember { Animatable(0f) }
    val peakProgress = remember { Animatable(0f) }
    val arcOp = remember { Animatable(0f) }
    val sparkScale = remember { Animatable(0f) }
    val sparkOp = remember { Animatable(0f) }
    val wordOp = remember { Animatable(0f) }
    val wordY = remember { Animatable(12f) }
    val fdotOp = remember { Animatable(0f) }
    val fdotScale = remember { Animatable(0f) }
    val tagOp = remember { Animatable(0f) }
    val orb1Op = remember { Animatable(0f) }
    val orb2Op = remember { Animatable(0f) }

    val sparkPulse = rememberInfiniteTransition(label = "pulse")
    val pulse by sparkPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkPulse",
    )

    LaunchedEffect(Unit) {
        launch { orb1Op.animateTo(1f, tween(700, delayMillis = 100)) }
        launch { orb2Op.animateTo(1f, tween(700, delayMillis = 300)) }

        listOf(280L, 440L, 600L, 760L, 920L).forEachIndexed { i, d ->
            launch {
                delay(d)
                dotsVisible[i] = true
            }
        }
        delay(1080)

        launch { ghostOp.animateTo(1f, tween(300)) }
        delay(320)

        launch { peakProgress.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        delay(540)

        launch { arcOp.animateTo(1f, tween(200)) }
        delay(220)

        launch { sparkOp.animateTo(1f, tween(80)) }
        launch { sparkScale.animateTo(1f, tween(350)) }
        delay(450)

        launch { wordOp.animateTo(1f, tween(440)) }
        launch { wordY.animateTo(0f, tween(440, easing = FastOutSlowInEasing)) }
        delay(240)

        launch { fdotOp.animateTo(1f, tween(180)) }
        launch { fdotScale.animateTo(1f, tween(350)) }
        delay(180)

        launch { tagOp.animateTo(1f, tween(500)) }
        delay(600)

        onFinish()
    }

    Box(
        Modifier.fillMaxSize().background(MomentraBrandColors.Brand),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(260.dp)
                .offset(x = 110.dp, y = (-190).dp)
                .graphicsLayer { alpha = orb1Op.value }
                .background(MomentraBrandColors.Cta.copy(alpha = 0.18f), CircleShape),
        )
        Box(
            Modifier
                .size(200.dp)
                .offset(x = (-110).dp, y = 210.dp)
                .graphicsLayer { alpha = orb2Op.value }
                .background(MomentraBrandColors.Progress.copy(alpha = 0.12f), CircleShape),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Canvas(Modifier.size(120.dp)) {
                val w = size.width
                fun p(v: Float) = v * w / 120f

                drawPath(
                    path = Path().apply {
                        moveTo(p(14f), p(100f))
                        lineTo(p(14f), p(50f))
                        lineTo(p(34f), p(74f))
                        lineTo(p(54f), p(24f))
                        lineTo(p(54f), p(100f))
                    },
                    color = MomentraBrandColors.TextOnDark.copy(alpha = ghostOp.value * 0.15f),
                    style = Stroke(p(8f), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                val dotPts = listOf(
                    Offset(p(14f), p(100f)), Offset(p(14f), p(62f)),
                    Offset(p(34f), p(74f)), Offset(p(54f), p(32f)),
                    Offset(p(54f), p(100f)),
                )
                dotsVisible.forEachIndexed { i, vis ->
                    if (vis) drawCircle(MomentraBrandColors.TextOnDark, p(6f), dotPts[i])
                }

                if (peakProgress.value > 0f) {
                    val pts = listOf(
                        Offset(p(54f), p(100f)), Offset(p(54f), p(32f)),
                        Offset(p(74f), p(74f)), Offset(p(94f), p(32f)),
                        Offset(p(96f), p(100f)),
                    )
                    val total = pts.size - 1
                    val drawn = (peakProgress.value * total).toInt()
                    for (i in 0 until minOf(drawn + 1, total)) {
                        val t = i.toFloat() / total
                        val col = lerp(MomentraBrandColors.Cta, MomentraBrandColors.Progress, t)
                        drawLine(col, pts[i], pts[i + 1], p(8f), StrokeCap.Round)
                    }
                }

                if (arcOp.value > 0f) {
                    val arc = Path().apply {
                        moveTo(p(94f), p(32f))
                        quadraticBezierTo(p(98f), p(20f), p(104f), p(16f))
                    }
                    drawPath(
                        arc,
                        MomentraBrandColors.Progress.copy(alpha = arcOp.value * 0.7f),
                        style = Stroke(p(2.5f), cap = StrokeCap.Round),
                    )
                }

                if (sparkOp.value > 0f) {
                    val sc = sparkScale.value * pulse
                    drawCircle(
                        MomentraBrandColors.Progress.copy(alpha = sparkOp.value),
                        p(10f) * sc,
                        Offset(p(105f), p(18f)),
                    )
                    drawCircle(
                        MomentraBrandColors.Cta.copy(alpha = sparkOp.value),
                        p(5.5f) * sc,
                        Offset(p(105f), p(18f)),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = wordOp.value
                    translationY = wordY.value
                },
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = MomentraBrandColors.TextOnDark)) { append("momentr") }
                            withStyle(SpanStyle(color = MomentraBrandColors.Cta)) { append("a") }
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.width(2.dp))
                    Box(
                        Modifier
                            .size(7.dp)
                            .offset(y = (-4).dp)
                            .graphicsLayer {
                                alpha = fdotOp.value
                                scaleX = fdotScale.value
                                scaleY = fdotScale.value
                            }
                            .background(MomentraBrandColors.Progress, CircleShape),
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    "TOGETHER · FORWARD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 3.sp,
                    color = MomentraBrandColors.TextOnDark.copy(alpha = 0.38f * tagOp.value),
                )
            }
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
)
