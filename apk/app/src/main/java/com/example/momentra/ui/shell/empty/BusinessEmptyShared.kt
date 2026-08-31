package com.example.momentra.ui.shell.empty

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

internal object BusinessEmptyTokens {
    val Accent = Color(0xFF818CF8)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF64748B)
    val CardFill = Color.White.copy(alpha = 0.08f)
    val CardStroke = Color.White.copy(alpha = 0.10f)
    val IconWell = Accent.copy(alpha = 0.10f)
}

@Composable
internal fun BusinessEmptyScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "businessEmptyAppear",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = appear
                scaleX = 0.98f + (0.02f * appear)
                scaleY = 0.98f + (0.02f * appear)
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}

@Composable
internal fun BusinessEmptyPill(label: String) {
    Text(
        text = label.uppercase(),
        color = BusinessEmptyTokens.Accent,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, BusinessEmptyTokens.Accent, RoundedCornerShape(100.dp))
            .background(BusinessEmptyTokens.Accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun BusinessEmptyHeadline(title: String, body: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            color = BusinessEmptyTokens.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = body,
            color = BusinessEmptyTokens.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun BusinessEmptyCta(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, BusinessEmptyTokens.Accent, RoundedCornerShape(12.dp))
            .background(BusinessEmptyTokens.Accent.copy(alpha = 0.15f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = BusinessEmptyTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
internal fun BusinessEmptyIcon(
    @DrawableRes resId: Int,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

@Composable
internal fun BusinessEmptyImage(
    @DrawableRes resId: Int,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.size(width = width, height = height),
    )
}
