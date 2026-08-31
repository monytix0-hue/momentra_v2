package com.example.momentra.ui.shell.empty

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma: life-empty-b (657:10151) */
@Composable
fun BusinessLifeEmptyContent(
    onStartCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = listOf("FTE Nodes", "Burn Rate", "Unit Margin")

    BusinessEmptyScrollColumn(modifier = modifier) {
        BusinessEmptyPill("LIFE")
        BusinessEmptyHeadline(
            title = "See the Full Picture",
            body = "People, finances, operations — how every thread of your business weaves together.",
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(24.dp))
                .background(BusinessEmptyTokens.CardFill)
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LifeNode(R.drawable.ic_business_empty_users, "People")
                Spacer(modifier = Modifier.width(16.dp))
                BusinessEmptyImage(
                    resId = R.drawable.ic_business_empty_connector_h,
                    width = 24.dp,
                    height = 2.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
                LifeNode(R.drawable.ic_business_empty_dollar, "Finances")
            }

            BusinessEmptyImage(
                resId = R.drawable.ic_business_empty_connector_v,
                width = 2.dp,
                height = 16.dp,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LifeNode(R.drawable.ic_business_empty_cpu, "Operations")
                Spacer(modifier = Modifier.width(16.dp))
                BusinessEmptyImage(
                    resId = R.drawable.ic_business_empty_connector_h,
                    width = 24.dp,
                    height = 2.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
                LifeNode(R.drawable.ic_business_empty_activity, "Growth")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            stats.forEach { label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(12.dp))
                        .background(BusinessEmptyTokens.CardFill)
                        .padding(10.dp),
                ) {
                    Text(
                        text = "—",
                        color = BusinessEmptyTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(label, color = BusinessEmptyTokens.TextMuted, fontSize = 10.sp)
                }
            }
        }

        BusinessEmptyCta("Start Journey →", onStartCta)
    }
}

@Composable
private fun LifeNode(@DrawableRes icon: Int, title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, BusinessEmptyTokens.Accent, CircleShape)
                .background(BusinessEmptyTokens.Accent.copy(alpha = 0.15f)),
        ) {
            BusinessEmptyIcon(icon, 20.dp)
        }
        Text(
            text = title,
            color = BusinessEmptyTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}
