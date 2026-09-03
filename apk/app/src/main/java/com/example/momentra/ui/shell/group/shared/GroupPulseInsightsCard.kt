package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.AnalyticsInsightItemDto
import com.example.momentra.ui.theme.PlusJakartaSans

@Composable
fun GroupPulseInsightsHeroCard(
    headerTitle: String,
    insights: List<AnalyticsInsightItemDto>,
    gradient: Brush,
    titleColor: Color = Color.White,
    bodyColor: Color = Color.White.copy(alpha = 0.9f),
    footerLabel: String? = null,
    onFooterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            headerTitle,
            color = titleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        if (insights.isEmpty()) {
            Text(
                "No insights yet",
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Insights appear when there's enough group activity — nothing is invented.",
                color = bodyColor,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
        } else {
            insights.take(3).forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.title?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = titleColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    item.body?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = bodyColor,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }
        if (footerLabel != null && onFooterClick != null) {
            Text(
                footerLabel,
                color = Color(0xFF131313),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = onFooterClick)
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
fun GroupPulseInsightsSectionCard(
    insights: List<AnalyticsInsightItemDto>,
    modifier: Modifier = Modifier,
) {
    GroupSectionCard(title = "Momentra Insights", modifier = modifier) {
        if (insights.isEmpty()) {
            GroupEmptySection(
                message = "No insights yet",
                detail = "Insights appear when there's enough group activity — nothing is invented.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                insights.take(3).forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.title?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                color = GroupActiveTheme.Text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        item.body?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                color = GroupActiveTheme.Secondary,
                                fontSize = 12.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }
        }
    }
}
