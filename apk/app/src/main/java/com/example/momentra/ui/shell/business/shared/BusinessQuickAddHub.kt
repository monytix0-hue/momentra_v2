package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.shell.business.shared.teamOpsHubIconRes

@Composable
fun BusinessQuickAddHub(
    hasActiveMoment: Boolean,
    hasCompany: Boolean,
    onClose: () -> Unit,
    onExpense: () -> Unit = {},
    onRevenue: () -> Unit = {},
    onInvoice: () -> Unit = {},
    onMembers: () -> Unit = {},
    onCreateMoment: () -> Unit = {},
    onTile: (BusinessQuickAddKind) -> Unit = {},
    momentTypeCode: String? = null,
    capabilities: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.forTypeCode(momentTypeCode)
    val isRunway = theme.typeLabel == BusinessActiveTheme.BusinessRunway.typeLabel
    val isOps = theme.typeLabel == BusinessActiveTheme.BusinessOperations.typeLabel
    val useLegacyExpenseShortcuts = !isRunway && !isOps
    var search by remember { mutableStateOf("") }
    val tiles = remember(theme, search) {
        val all = businessHubTiles(theme)
        val q = search.trim().lowercase()
        if (q.isEmpty()) all else all.filter {
            it.label().lowercase().contains(q) || it.subtitle().lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Quick Add",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    theme.hubSubtitle,
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6818CF8))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_teamops_qa_close),
                    contentDescription = "Close",
                    modifier = Modifier.size(10.dp),
                )
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HubChip(theme.typeLabel, selected = true, theme = theme)
            theme.filterChips.forEach { HubChip(it, selected = false, theme = theme) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.heroGradient)
                .border(1.dp, theme.border, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    theme.hubHeroTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    theme.hubHeroDetail,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Image(
                painter = painterResource(theme.hubHeroRes),
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(24.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_teamops_qa_search),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                textStyle = TextStyle(color = theme.text, fontSize = 13.sp, fontFamily = PlusJakartaSans),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text("Search actions...", color = theme.muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    }
                    inner()
                },
            )
        }

        tiles.chunked(3).forEach { chunk ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                chunk.forEach { kind ->
                    val capOk = kind.isCapabilityEnabled(capabilities, momentTypeCode)
                    ActionTile(
                        kind = kind,
                        theme = theme,
                        enabled = (hasActiveMoment || kind == BusinessQuickAddKind.MEMORY) && capOk,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (kind) {
                                BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.SPEND_ENTRY -> {
                                    if (useLegacyExpenseShortcuts) onExpense()
                                    onTile(kind)
                                }
                                BusinessQuickAddKind.REVENUE -> {
                                    if (useLegacyExpenseShortcuts) onRevenue()
                                    onTile(kind)
                                }
                                BusinessQuickAddKind.INVOICE -> {
                                    if (useLegacyExpenseShortcuts) onInvoice()
                                    onTile(kind)
                                }
                                else -> onTile(kind)
                            }
                        },
                    )
                }
                repeat(3 - chunk.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        if (tiles.isEmpty()) {
            Text(
                "No actions match this search.",
                color = theme.secondary,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            )
        }

        Text(
            "Create another Business Moment",
            color = theme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCreateMoment)
                .padding(vertical = 10.dp)
                .testTag(MaestroIds.QA_TILE_EXPENSE),
        )
    }
}

@Composable
private fun HubChip(label: String, selected: Boolean, theme: BusinessActiveTheme) {
    Text(
        label,
        color = if (selected) Color.White else Color(0xFF818CF8),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFF6366F1) else theme.card)
            .border(1.dp, Color(0x4D6366F1), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActionTile(
    kind: BusinessQuickAddKind,
    theme: BusinessActiveTheme,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stripe = kind.stripeColor()
    val iconRes = kind.teamOpsHubIconRes()
    val tileHeight = if (
        kind == BusinessQuickAddKind.ACTIVITY_LOG ||
        kind == BusinessQuickAddKind.POLL ||
        kind == BusinessQuickAddKind.MEMORY
    ) 120.dp else 100.dp
    Box(
        modifier = modifier
            .height(tileHeight)
            .clip(
                if (tileHeight == 120.dp) {
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                } else {
                    RoundedCornerShape(16.dp)
                },
            )
            .background(theme.card)
            .background(
                Brush.horizontalGradient(
                    listOf(stripe.copy(alpha = 0.12f), Color.Transparent),
                ),
            )
            .border(1.dp, theme.border, RoundedCornerShape(16.dp))
            .then(kind.maestroTileId()?.let { Modifier.testTag(it) } ?: Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(tileHeight)
                .background(stripe),
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                Text(kind.emoji(), fontSize = 22.sp)
            }
            Text(
                kind.label(),
                color = stripe,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                kind.subtitle(),
                color = theme.muted,
                fontSize = 9.sp,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
            )
        }
    }
}
