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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

private data class CreateTileData(
    @DrawableRes val icon: Int,
    val title: String,
    val body: String,
)

/** Figma: create-empty-b (657:10100) */
@Composable
fun BusinessCreateEmptyContent(
    onStartCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = listOf(
        CreateTileData(R.drawable.ic_business_empty_file_text, "Create Invoice", "Bill clients professionally"),
        CreateTileData(R.drawable.ic_business_empty_dollar, "Log Expense", "Track every rupee spent"),
        CreateTileData(R.drawable.ic_business_empty_users, "Add Team Member", "Grow your operations team"),
        CreateTileData(R.drawable.ic_business_empty_folder, "New Project", "Organize work by project"),
        CreateTileData(R.drawable.ic_business_empty_truck, "Add Vendor", "Manage supply chain"),
        CreateTileData(R.drawable.ic_business_empty_bar_chart, "Generate Report", "Data-driven business insights"),
    )

    BusinessEmptyScrollColumn(modifier = modifier) {
        BusinessEmptyPill("CREATE")
        BusinessEmptyHeadline(
            title = "Your Command Center",
            body = "Invoices, expenses, vendors, projects - every business action in one place.",
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    CreateTileCard(tiles[row * 2], Modifier.weight(1f))
                    CreateTileCard(tiles[row * 2 + 1], Modifier.weight(1f))
                }
            }
        }

        Text(
            text = "From solo founders to scaling teams",
            color = BusinessEmptyTokens.TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        BusinessEmptyCta("First Action →", onStartCta)
    }
}

@Composable
private fun CreateTileCard(
    tile: CreateTileData,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BusinessEmptyTokens.CardStroke, RoundedCornerShape(16.dp))
            .background(BusinessEmptyTokens.CardFill)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 36.dp, height = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BusinessEmptyTokens.IconWell),
        ) {
            BusinessEmptyIcon(tile.icon, 18.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = tile.title,
                color = BusinessEmptyTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Text(
                text = tile.body,
                color = BusinessEmptyTokens.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
