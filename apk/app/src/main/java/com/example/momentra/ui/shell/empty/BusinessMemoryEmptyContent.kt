package com.example.momentra.ui.shell.empty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma: memory-empty-b (657:10208) */
@Composable
fun BusinessMemoryEmptyContent(
    onStartCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        "Spending Patterns",
        "Revenue Forecasts",
        "Operational Trends",
    )

    BusinessEmptyScrollColumn(modifier = modifier) {
        BusinessEmptyPill("MEMORY")
        BusinessEmptyHeadline(
            title = "Intelligence That Compounds",
            body = "AI-powered pattern recognition across spending, performance, and operations.",
        )

        rows.forEach { title ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(14.dp))
                    .background(BusinessEmptyTokens.CardFill)
                    .padding(14.dp),
            ) {
                BusinessEmptyIcon(R.drawable.ic_business_empty_memory_dot, 6.dp)
                Text(
                    text = title,
                    color = BusinessEmptyTokens.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                BusinessEmptyImage(
                    resId = R.drawable.ic_business_empty_sparkline,
                    width = 60.dp,
                    height = 20.dp,
                )
            }
        }

        Text(
            text = "“An enterprise with a memory is an enterprise with an unfair advantage.”",
            color = BusinessEmptyTokens.TextSecondary,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        BusinessEmptyCta("Activate Memory →", onStartCta)
    }
}
