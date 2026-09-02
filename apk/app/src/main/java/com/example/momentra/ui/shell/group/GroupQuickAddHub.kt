package com.example.momentra.ui.shell.group

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
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
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * Figma 575:14655 — Group Trip Action Center Quick Add hub.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupQuickAddHub(
    hasActiveMoment: Boolean,
    onClose: () -> Unit,
    onExpense: () -> Unit,
    onContribution: () -> Unit,
    onSettle: () -> Unit = {},
    onParticipants: () -> Unit = {},
    onInvite: () -> Unit = {},
    onBudget: () -> Unit = {},
    onPlanning: () -> Unit = {},
    onBooking: () -> Unit = {},
    onPoll: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onMemory: () -> Unit = {},
    onPurchaseItem: () -> Unit = {},
    onResident: () -> Unit = {},
    onCreateMoment: () -> Unit = {},
    onJoinCode: (String) -> Unit = {},
    momentTitle: String? = null,
    momentTypeCode: String? = null,
    capabilities: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    val tiles = GroupActionRegistry.figmaTripHubTiles.filter {
        search.isBlank() || it.label.contains(search, ignoreCase = true)
    }
    val titleChip = momentTitle?.takeIf { it.isNotBlank() } ?: "Trip"

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
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Quick Add",
                        color = Color(0xFFF7F5F2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Bring your experience to life",
                        color = Color(0xFFA8A19E),
                        fontSize = 10.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF171618))
                        .border(1.dp, Color(0xFF403C40), RoundedCornerShape(16.dp))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_qa_close),
                        contentDescription = "Close",
                        tint = Color(0xFFE5E0EE),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HubContextChip(titleChip, solid = true, solidColor = Color(0xFFFFB598), textOnSolid = Color(0xFF591D00))
                HubContextChip("Shared Experience", solid = false, tint = Color(0xFF14B8A6))
                HubContextChip("Planning Stage", solid = false, tint = Color(0xFFA855F7))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(22.dp), ambientColor = Color(0xFFFFB598).copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFB598), Color(0xFFE8621A))))
                    .border(1.dp, Color(0xFFFFB598).copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Bring your experience to life",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        if (hasActiveMoment) "Add people, plans, money, memories and decisions."
                        else "Select or create a moment first.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Image(
                        painter = painterResource(R.drawable.trip_hub_hero),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 180.dp, height = 120.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = Color(0xFFFFB598).copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF171618))
                        .border(1.dp, Color(0xFFFFB598).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_group_qa_search),
                        contentDescription = null,
                        tint = Color(0xFFA8A19E),
                        modifier = Modifier.size(18.dp),
                    )
                    BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(MaestroIds.QA_SEARCH),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFFF7F5F2),
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        ),
                        decorationBox = { inner ->
                            if (search.isEmpty()) {
                                Text(
                                    "Search actions...",
                                    color = Color(0xFFA8A19E),
                                    fontSize = 13.sp,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                            inner()
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF171618))
                        .border(1.dp, Color(0xFFFFB598).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .semantics {
                            contentDescription = "Scan QR code"
                            role = Role.Button
                        }
                        .clickable { showScanner = true }
                        .testTag("qa.tile.qr"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFFFFB598),
                        modifier = Modifier.size(18.dp),
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
                    val enabled = GroupActionRegistry.hubTileEnabled(hasActiveMoment, capabilities, tile)
                    HubFigmaTile(
                        tile = tile,
                        enabled = enabled,
                        onClick = {
                            when (tile.destination) {
                                GroupActionRegistry.Destination.EXPENSE -> onExpense()
                                GroupActionRegistry.Destination.CONTRIBUTION -> onContribution()
                                GroupActionRegistry.Destination.SETTLEMENT -> onSettle()
                                GroupActionRegistry.Destination.INVITE -> onInvite()
                                GroupActionRegistry.Destination.PARTICIPANTS -> onParticipants()
                                GroupActionRegistry.Destination.BUDGET -> onBudget()
                                GroupActionRegistry.Destination.PLANNING -> onPlanning()
                                GroupActionRegistry.Destination.BOOKING -> onBooking()
                                GroupActionRegistry.Destination.POLL -> onPoll()
                                GroupActionRegistry.Destination.UPDATE -> onUpdate()
                                GroupActionRegistry.Destination.MEMORY -> onMemory()
                                GroupActionRegistry.Destination.PURCHASE_ITEM -> onPurchaseItem()
                                GroupActionRegistry.Destination.RESIDENT -> onResident()
                            }
                        },
                    )
                }
            }

            Text(
                "Create another Group Moment",
                color = Color(0xFFE8621A),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateMoment)
                    .padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))
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
private fun HubContextChip(
    label: String,
    solid: Boolean,
    tint: Color = Color(0xFFE8621A),
    solidColor: Color = Color(0xFFFFB598),
    textOnSolid: Color = Color(0xFF591D00),
) {
    Text(
        label,
        color = if (solid) textOnSolid else tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        maxLines = 1,
        modifier = Modifier
            .shadow(10.dp, RoundedCornerShape(999.dp), ambientColor = tint.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(999.dp))
            .background(if (solid) solidColor else tint.copy(alpha = 0.12f))
            .border(
                1.dp,
                tint.copy(alpha = 0.3f),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun HubFigmaTile(
    tile: GroupActionRegistry.HubTileSpec,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val testTag = when (tile.id) {
        "expense" -> MaestroIds.QA_TILE_EXPENSE
        "contribution" -> MaestroIds.QA_TILE_CONTRIBUTE
        "settle" -> MaestroIds.QA_TILE_SETTLE
        "invite" -> MaestroIds.QA_TILE_PEOPLE
        "budget" -> MaestroIds.QA_TILE_BUDGET
        else -> "qa.tile.group.${tile.id}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(0.31f)
            .height(104.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = tile.gradientStart.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(tile.gradientStart, tile.gradientEnd)))
            .border(1.dp, tile.gradientStart.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(testTag)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(tile.iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        Text(
            tile.label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
        )
    }
}
