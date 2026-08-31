package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SetupPreviewBar(
    val label: String,
    val progress: Float,
    val color: Color,
)

@Composable
fun SetupPreviewCard(
    title: String,
    subtitle: String? = null,
    bullets: List<String> = emptyList(),
    bars: List<SetupPreviewBar> = emptyList(),
    badge: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SetupTokens.SurfaceCard)
            .border(1.dp, SetupTokens.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = SetupTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (badge != null) {
                Text(
                    badge,
                    color = SetupTokens.BrandPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SetupTokens.AccentPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        if (subtitle != null) {
            Text(subtitle, color = SetupTokens.TextSecondary, fontSize = 12.sp)
        }
        bars.forEach { bar ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(bar.label, color = SetupTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bar.progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(bar.color),
                    )
                }
            }
        }
        bullets.forEach { bullet ->
            Text("• $bullet", color = SetupTokens.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun SetupMissionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SetupTokens.SurfaceCard)
            .border(1.dp, SetupTokens.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = SetupTokens.BrandPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(body, color = SetupTokens.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun SetupFieldCard(
    label: String,
    value: String,
    icon: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SetupTokens.SurfaceCard)
            .border(1.dp, SetupTokens.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Text(icon, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SetupTokens.TextSecondary, fontSize = 11.sp)
            Text(value.ifBlank { "—" }, color = SetupTokens.TextPrimary, fontSize = 14.sp)
        }
    }
}
