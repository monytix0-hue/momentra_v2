package com.example.momentra.ui.shell.empty

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.MomentSummary
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.ShellTokens

data class MomentEmptyConfig(
    val eyebrow: String? = null,
    val title: String,
    val body: String,
    val primaryLabel: String? = null,
    val onPrimary: (() -> Unit)? = null,
    val secondaryLabel: String? = null,
    val onSecondary: (() -> Unit)? = null,
    val historyTitle: String? = null,
    val history: List<MomentSummary> = emptyList(),
    val accent: Color = Color(0xFF7C5CFC),
)

@Composable
fun MomentEmptyState(
    config: MomentEmptyConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        config.eyebrow?.let { eyebrow ->
            Text(
                text = eyebrow.uppercase(),
                color = config.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, config.accent, RoundedCornerShape(999.dp))
                    .background(config.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        Text(
            text = config.title,
            color = MomentraBrandColors.TextOnDark,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = config.body,
            color = ShellTokens.EmptyBody,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        if (config.history.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = config.historyTitle ?: "Past moments",
                color = MomentraBrandColors.TextOnDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            config.history.forEach { moment ->
                HistoryRow(moment = moment, accent = config.accent)
            }
        }

        Spacer(Modifier.height(8.dp))
        if (config.primaryLabel != null && config.onPrimary != null) {
            PrimaryCta(
                label = config.primaryLabel,
                accent = config.accent,
                onClick = config.onPrimary,
            )
        }
        if (config.secondaryLabel != null && config.onSecondary != null) {
            Text(
                text = config.secondaryLabel,
                color = config.accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .semantics {
                        role = Role.Button
                        contentDescription = config.secondaryLabel
                    }
                    .clickable(onClick = config.onSecondary)
                    .padding(8.dp),
            )
        }
        Text(
            text = "Or tap + in the top bar to create a Moment",
            color = ShellTokens.EmptyBody.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryRow(moment: MomentSummary, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = moment.title,
                color = MomentraBrandColors.TextOnDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                text = moment.status.replaceFirstChar { it.titlecase() },
                color = ShellTokens.EmptyBody,
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(moment.status.take(1), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PrimaryCta(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(1.5.dp, accent, RoundedCornerShape(14.dp))
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MomentraBrandColors.TextOnDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}
