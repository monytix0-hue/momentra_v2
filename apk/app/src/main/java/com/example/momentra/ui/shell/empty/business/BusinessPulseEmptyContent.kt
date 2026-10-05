package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma: pulse-empty-b (1687:20594) */
@Composable
fun BusinessPulseEmptyContent(
    onStartCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = listOf("Money Spend", "Purchases tracked", "Expenses")
    val features = listOf(
        Triple(R.drawable.ic_business_empty_activity, "See every purchase", "Track what your business buys and how much it costs."),
        Triple(R.drawable.ic_business_empty_bell, "Keep expenses in check", "Know when spending is higher than usual."),
        Triple(R.drawable.ic_business_empty_trending_up, "See spending clearly", "See where your business spends the most."),
    )

    BusinessEmptyScrollColumn(modifier) {
        BusinessEmptyPill("PULSE")
        BusinessEmptyHeadline(
            title = "See Where Your Business Money Goes",
            body = "Track purchases, expenses and spending across your business.",
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(16.dp))
                .background(BusinessEmptyTokens.CardFill)
                .padding(16.dp),
        ) {
            metrics.forEach { label ->
                ZeroMetricBar(label = label)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            features.forEach { (icon, title, body) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(12.dp))
                        .background(BusinessEmptyTokens.CardFill)
                        .padding(12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BusinessEmptyTokens.IconWell),
                    ) {
                        BusinessEmptyIcon(icon, 16.dp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(title, color = BusinessEmptyTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(body, color = BusinessEmptyTokens.TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        BusinessEmptyCta("Start Tracking →", onStartCta)
    }
}

@Composable
private fun ZeroMetricBar(label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = BusinessEmptyTokens.TextSecondary, fontSize = 11.sp)
            Text(
                text = BusinessNoMomentEmptyCopy.ABSENT,
                color = BusinessEmptyTokens.Accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        )
    }
}
