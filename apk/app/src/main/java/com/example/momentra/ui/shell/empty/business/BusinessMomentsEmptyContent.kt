package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma: moments-empty-b (1687:20652) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessMomentsEmptyContent(
    onStartCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeline = listOf(
        "You bought supplies" to "Today, 10:42 AM",
        "A customer paid" to "Oct 14, 2024",
        "You saved a receipt" to "Sep 01, 2024",
    )
    val chips = listOf("Purchases", "Expenses", "Receipts", "Activity")

    BusinessEmptyScrollColumn(modifier = modifier) {
        BusinessEmptyPill("MOMENTS")
        BusinessEmptyHeadline(
            title = "See what happened in your business",
            body = "Keep purchases, expenses, receipts and important activity together.",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(20.dp))
                .background(BusinessEmptyTokens.CardFill)
                .padding(20.dp),
        ) {
            timeline.forEachIndexed { index, (title, time) ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BusinessEmptyIcon(R.drawable.ic_business_empty_timeline_dot, 10.dp)
                        if (index < timeline.lastIndex) {
                            Spacer(modifier = Modifier.height(4.dp))
                            BusinessEmptyImage(
                                resId = R.drawable.ic_business_empty_timeline_line,
                                width = 2.dp,
                                height = 44.dp,
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = if (index < timeline.lastIndex) 16.dp else 0.dp),
                    ) {
                        Text(title, color = BusinessEmptyTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(time, color = BusinessEmptyTokens.TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            chips.forEach { Chip(it) }
        }

        BusinessEmptyCta("Add your first moment →", onStartCta)
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        color = BusinessEmptyTokens.TextSecondary,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(100.dp))
            .background(BusinessEmptyTokens.CardFill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
