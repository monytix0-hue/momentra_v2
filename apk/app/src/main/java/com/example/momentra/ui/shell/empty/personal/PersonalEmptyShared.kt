package com.example.momentra.ui.shell.empty.personal

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.theme.PlusJakartaSans

internal val PeBg = Color(0xFF14121B)
internal val PeText = Color(0xFFE5E0EE)
internal val PeTextMuted = Color(0xFF938EA1)
internal val PeSubtle = Color(0xFF938EA1)
internal val PeSecondary = Color(0xFFC9C4D8)
internal val PeMuted = Color(0xFFB8B0C8)
internal val PePurple = Color(0xFF7C5CFC)
internal val PeGreen = Color(0xFF10B981)
internal val PeAmber = Color(0xFFF59E0B)
internal val PePink = Color(0xFFE91E63)
internal val PeBlue = Color(0xFF3B82F6)
internal val PeCard = Color(0xFF1A1726)

internal val PeCtaBrush = Brush.horizontalGradient(listOf(PePurple, PePink))

@Composable
internal fun PeAppear(
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        shown = true
    }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "peAppear",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = appear
            translationY = (1f - appear) * 16f
            scaleX = 0.985f + (0.015f * appear)
            scaleY = 0.985f + (0.015f * appear)
        },
    ) {
        content()
    }
}

@Composable
internal fun PeIconCircle(
    glyph: String,
    accent: Color,
    deep: Color,
    size: Dp = 32.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent, deep))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = Color.White,
            fontSize = (size.value * 0.44f).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
internal fun PeGradientPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(PeCtaBrush)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            fontFamily = PlusJakartaSans,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun PeQuoteCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                ),
            )
            .border(1.dp, PePurple.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PePurple.copy(alpha = 0.6f)),
        )
        Text(
            text,
            color = PeText,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = PlusJakartaSans,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun PersonalGradientCta(label: String, onClick: () -> Unit) {
    PeGradientPrimaryButton(label = label, onClick = onClick)
}

@Composable
internal fun PersonalHistoryBlock(title: String, history: List<MomentSummary>) {
    if (history.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            title,
            color = PeText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(modifier = Modifier.height(8.dp))
        history.forEach { moment ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(14.dp),
            ) {
                Text(
                    moment.title,
                    color = PeText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    moment.status.replaceFirstChar { it.titlecase() },
                    color = PeSubtle,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
internal fun PersonalSectionLabel(text: String, color: Color = PePurple) {
    Text(
        text = text.uppercase(),
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
internal fun PersonalHeroTitle(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            color = PeText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            body,
            color = PeMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}
