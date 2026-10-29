package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Shared Business empty state, matching the Group pattern
 * (`GroupRecentActivityScreen`): a title that names the surface, one muted line that says what
 * will appear here, and an optional next action. Card-free — it sits on the sheet/tab background.
 *
 * Replaces the 70 inline "Nothing recorded yet." / "Nothing is waiting." strings that were
 * previously bare `Text` calls inside per-module cards.
 */
@Composable
fun BusinessEmptyState(
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier,
    glyph: String? = null,
    iconRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 12.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        if (glyph != null || iconRes != null) {
            Box(
                modifier = Modifier
                    .size(if (compact) 40.dp else 56.dp)
                    .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(if (compact) 10.dp else 14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(if (compact) 18.dp else 24.dp),
                    )
                } else {
                    Text(glyph.orEmpty(), fontSize = if (compact) 16.sp else 22.sp, fontFamily = PlusJakartaSans)
                }
            }
        }
        Text(
            title,
            color = BusinessSheetTokens.text,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 13.sp else 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            body,
            color = BusinessSheetTokens.muted,
            fontFamily = PlusJakartaSans,
            fontSize = if (compact) 12.sp else 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.3f), shape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    actionLabel,
                    color = accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
