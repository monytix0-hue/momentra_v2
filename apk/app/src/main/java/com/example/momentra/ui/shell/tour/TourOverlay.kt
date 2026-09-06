package com.example.momentra.ui.shell.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlin.math.roundToInt

private val ScrimColor = Color.Black.copy(alpha = 0.42f)
private val BubbleBg = Color(0xFF1A2030)
private val BubbleBorder = Color(0xFF3A4258)
private val BubbleAccent = Color(0xFFC9BFFF)
private val BubbleText = Color(0xFFE5E0EE)
private val BubbleMuted = Color(0xFFC9C4D8)

@Composable
fun TourHost(
    controller: TourController,
    registry: TourTargetRegistry,
    onNavigate: (com.example.momentra.domain.BottomDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTourTargetRegistry provides registry,
        LocalTourController provides controller,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            TourOverlay(
                controller = controller,
                registry = registry,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
fun TourOverlay(
    controller: TourController,
    registry: TourTargetRegistry,
    onNavigate: (com.example.momentra.domain.BottomDestination) -> Unit,
) {
    val hint = controller.oneOffHint
    val step = if (hint == null) controller.currentStep else null
    if (hint == null && step == null) return

    val title = hint?.title ?: step!!.title
    val body = hint?.body ?: step!!.body
    val nextLabel = when {
        hint != null -> "Go to ${hint.goTo.labelShort()}"
        else -> step!!.nextLabel
    }
    val targetId = step?.target
    val hole = targetId?.let { registry.get(it) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        // Soft highlight only — no pointer handlers, so taps pass through to chrome/sheets.
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (hole != null) {
                val full = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                val cut = Path().apply {
                    val pad = 8.dp.toPx()
                    addRoundRect(
                        RoundRect(
                            left = (hole.left - pad).coerceAtLeast(0f),
                            top = (hole.top - pad).coerceAtLeast(0f),
                            right = (hole.right + pad).coerceAtMost(size.width),
                            bottom = (hole.bottom + pad).coerceAtMost(size.height),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                        ),
                    )
                }
                clipPath(cut, clipOp = ClipOp.Difference) {
                    drawPath(full, ScrimColor)
                }
                val pad = 8.dp.toPx()
                drawRoundRect(
                    color = BubbleAccent.copy(alpha = 0.9f),
                    topLeft = Offset(hole.left - pad, hole.top - pad),
                    size = Size(hole.width + pad * 2, hole.height + pad * 2),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        val bubbleMaxWidth = with(density) { 320.dp.toPx() }
        val bubbleApproxHeight = with(density) { 168.dp.toPx() }
        val (bx, by) = bubbleOffset(
            hole = hole,
            screenW = screenW,
            screenH = screenH,
            bubbleW = bubbleMaxWidth,
            bubbleH = bubbleApproxHeight,
        )

        Popup(
            offset = IntOffset(bx.roundToInt(), by.roundToInt()),
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = false,
            ),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .heightIn(min = 120.dp)
                    .background(BubbleBg, RoundedCornerShape(16.dp))
                    .border(1.dp, BubbleBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    title,
                    color = BubbleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    body,
                    color = BubbleMuted,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        if (hint != null) controller.dismissOneOff() else controller.skip()
                    }) {
                        Text("Skip", color = BubbleMuted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    }
                    TextButton(onClick = {
                        if (hint != null) {
                            onNavigate(hint.goTo)
                            controller.dismissOneOff()
                        } else {
                            controller.next(onNavigate)
                        }
                    }) {
                        Text(
                            nextLabel,
                            color = BubbleAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
    }
}

private fun bubbleOffset(
    hole: Rect?,
    screenW: Float,
    screenH: Float,
    bubbleW: Float,
    bubbleH: Float,
): Pair<Float, Float> {
    val margin = 24f
    if (hole == null) {
        return (screenW - bubbleW) / 2f to (screenH - bubbleH) / 2f
    }
    val below = hole.bottom + 16f
    val above = hole.top - bubbleH - 16f
    val y = when {
        below + bubbleH < screenH - margin -> below
        above > margin -> above
        else -> (screenH - bubbleH) / 2f
    }
    val x = (hole.center.x - bubbleW / 2f).coerceIn(margin, (screenW - bubbleW - margin).coerceAtLeast(margin))
    return x to y
}

private fun com.example.momentra.domain.BottomDestination.labelShort(): String = when (this) {
    com.example.momentra.domain.BottomDestination.PULSE -> "Pulse"
    com.example.momentra.domain.BottomDestination.MOMENTS -> "Moments"
    com.example.momentra.domain.BottomDestination.CREATE -> "Create"
    com.example.momentra.domain.BottomDestination.LIFE -> "Life"
    com.example.momentra.domain.BottomDestination.MEMORY -> "Memory"
}
