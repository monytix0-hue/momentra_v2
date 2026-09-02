package com.example.momentra.ui.shell.group.wedding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.shell.empty.group.GroupJoinQrScanner
import com.example.momentra.ui.shell.group.GroupActionRegistry
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

enum class WeddingQuickAddKind {
    PARTICIPANT,
    PLANNING,
    EXPENSE,
    BUDGET,
    CONTRIBUTION,
    SETTLE,
    VENDOR,
    ATTENDANCE,
    UPDATE,
    POLL,
    MEMORY,
}

data class WeddingHubTile(
    val kind: WeddingQuickAddKind,
    val label: String,
    val icon: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val live: Boolean,
)

private val weddingHubTiles = listOf(
    WeddingHubTile(WeddingQuickAddKind.PARTICIPANT, "Participant", "👤", Color(0xFFFA7387), Color(0xFFE01C4D), live = true),
    WeddingHubTile(WeddingQuickAddKind.PLANNING, "Planning Item", "📋", Color(0xFFF573B5), Color(0xFFDB2675), live = true),
    WeddingHubTile(WeddingQuickAddKind.EXPENSE, "Expense", "💳", Color(0xFFBF26D4), Color(0xFF871A8F), live = true),
    WeddingHubTile(WeddingQuickAddKind.BUDGET, "Budget", "🪙", Color(0xFFFC7085), Color(0xFFE83359), live = true),
    WeddingHubTile(WeddingQuickAddKind.CONTRIBUTION, "Contribution", "🎁", Color(0xFFD945F0), Color(0xFFA31CB0), live = true),
    WeddingHubTile(WeddingQuickAddKind.VENDOR, "Vendor", "🏪", Color(0xFFED8CB8), Color(0xFFD14D85), live = true),
    WeddingHubTile(WeddingQuickAddKind.ATTENDANCE, "Attendance", "✅", Color(0xFFBD175C), Color(0xFF820F42), live = true),
    WeddingHubTile(WeddingQuickAddKind.UPDATE, "Update", "📢", Color(0xFFE878FA), Color(0xFFBF26D4), live = true),
    WeddingHubTile(WeddingQuickAddKind.POLL, "Poll", "📊", Color(0xFFA854F7), Color(0xFF7D3BED), live = true),
    WeddingHubTile(WeddingQuickAddKind.MEMORY, "Memory", "📷", Color(0xFFF53D5E), Color(0xFFC71F40), live = true),
)

/** Figma 584:16938 — Wedding Quick Add hub exact. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeddingQuickAddHub(
    momentTitle: String?,
    hasActiveMoment: Boolean,
    onClose: () -> Unit,
    onTile: (WeddingQuickAddKind) -> Unit,
    onCreateMoment: () -> Unit = {},
    onJoinCode: (String) -> Unit = {},
    capabilities: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    val tiles = weddingHubTiles.filter {
        search.isBlank() || it.label.contains(search, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090A))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Quick Add", color = Color(0xFFF7F5F2), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text("Bring your experience to life", color = WeddingActiveTheme.Muted, fontSize = 10.sp, fontFamily = PlusJakartaSans)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF171618))
                    .border(1.dp, WeddingActiveTheme.PeachChip.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = WeddingActiveTheme.PeachChip, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContextChip(momentTitle ?: "Wedding", solid = true, solidColor = WeddingActiveTheme.PeachChip)
            ContextChip("Shared Experience", solid = false, tint = WeddingActiveTheme.TealChip)
            ContextChip("Planning Stage", solid = false, tint = WeddingActiveTheme.PurpleChip)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(WeddingActiveTheme.HubHeroGradient)
                .border(1.dp, WeddingActiveTheme.PeachChip.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Bring your experience to life",
                    color = WeddingActiveTheme.DarkText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    if (hasActiveMoment) "Add people, plans, money, memories and decisions."
                    else "Select or create a moment first.",
                    color = WeddingActiveTheme.DarkText,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Image(
                    painter = painterResource(R.drawable.wedding_hub_cake),
                    contentDescription = null,
                    modifier = Modifier
                        .width(180.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF171618))
                    .border(1.dp, WeddingActiveTheme.PeachChip.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .testTag(MaestroIds.QA_SEARCH),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = WeddingActiveTheme.Text,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                ),
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text("Search actions...", color = WeddingActiveTheme.Muted, fontSize = 13.sp, fontFamily = PlusJakartaSans)
                    }
                    inner()
                },
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF171618))
                    .border(1.dp, WeddingActiveTheme.PeachChip.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .semantics {
                        contentDescription = "Scan QR code"
                        role = Role.Button
                    }
                    .clickable { showScanner = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = WeddingActiveTheme.PeachChip,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 3,
        ) {
            tiles.forEach { tile ->
                val capabilityOk = when (tile.kind) {
                    WeddingQuickAddKind.EXPENSE ->
                        GroupActionRegistry.isDestinationEnabled(capabilities, GroupActionRegistry.Destination.EXPENSE)
                    WeddingQuickAddKind.CONTRIBUTION ->
                        GroupActionRegistry.isDestinationEnabled(capabilities, GroupActionRegistry.Destination.CONTRIBUTION)
                    WeddingQuickAddKind.PARTICIPANT ->
                        GroupActionRegistry.isDestinationEnabled(capabilities, GroupActionRegistry.Destination.PARTICIPANTS) ||
                            GroupActionRegistry.isDestinationEnabled(capabilities, GroupActionRegistry.Destination.INVITE)
                    WeddingQuickAddKind.BUDGET -> hasActiveMoment
                    else -> true
                }
                WeddingHubTileCard(
                    tile = tile,
                    enabled = hasActiveMoment && capabilityOk,
                    onClick = { onTile(tile.kind) },
                )
            }
        }

        Text(
            "Create another Group Moment",
            color = WeddingActiveTheme.AccentLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCreateMoment)
                .padding(vertical = 8.dp),
        )
    }
        if (showScanner) {
            GroupJoinQrScanner(
                onCode = { code ->
                    showScanner = false
                    onJoinCode(code)
                },
                onDismiss = { showScanner = false },
            )
        }
    }
}

@Composable
private fun ContextChip(label: String, solid: Boolean, tint: Color = WeddingActiveTheme.Accent, solidColor: Color = WeddingActiveTheme.Accent) {
    Text(
        label,
        color = if (solid) WeddingActiveTheme.DarkText else tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) solidColor else tint.copy(alpha = 0.12f))
            .border(1.dp, if (solid) solidColor.copy(alpha = 0.5f) else tint.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun WeddingHubTileCard(
    tile: WeddingHubTile,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val testTag = when (tile.kind) {
        WeddingQuickAddKind.EXPENSE -> MaestroIds.QA_TILE_EXPENSE
        WeddingQuickAddKind.CONTRIBUTION -> MaestroIds.QA_TILE_CONTRIBUTE
        WeddingQuickAddKind.SETTLE -> MaestroIds.QA_TILE_SETTLE
        WeddingQuickAddKind.PARTICIPANT -> MaestroIds.QA_TILE_PEOPLE
        WeddingQuickAddKind.BUDGET -> MaestroIds.QA_TILE_BUDGET
        else -> "qa.tile.wedding.${tile.kind.name.lowercase()}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(0.31f)
            .testTag(testTag)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(tile.gradientStart.copy(alpha = 0.9f), tile.gradientEnd)))
            .border(1.dp, tile.gradientStart.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(tile.icon, fontSize = 22.sp)
        Text(tile.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = PlusJakartaSans)
    }
}
