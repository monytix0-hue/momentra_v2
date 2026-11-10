package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * `TRD-HUB-v11` / `DST-HUB-v11`.
 *
 * Nine core captures, five support tiles, search, and the save-rules why card.
 */
@Composable
fun OperatingV12HubScreen(
    model: OperatingProjectionDto,
    theme: OperatingV12Theme,
    onCapture: (String) -> Unit,
    onOpenMasters: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val title = model.title.orEmpty().ifBlank { theme.label }
    val gstOn = model.gstTracking.equals("YES", ignoreCase = true)
    val identity = listOf(
        title,
        OperatingV12Date.fiscalYear(model.startDate),
        model.currency ?: "INR",
        if (gstOn) "GST on" else "GST off",
    ).joinToString(" · ")
    // Empty capabilities ⇒ leave tiles open (projection still loading). Same honesty as Retail/MTO hubs.
    val allowed = model.quickAdds.map { it.code.uppercase() }.toSet()

    // MFG-HUB-v3 `2396:29892` — plant journey only. MTO mounts MadeToOrderQuickAddHub, never this grid.
    val plantHub = theme.verticalKey == "manufacturing"
    val core = if (plantHub) {
        listOf(
            HubTile("SALE", "Sale", "Invoice · buyer · GST", R.drawable.qa_receipt, requires = listOf("SALE")),
            HubTile("PURCHASE", "Purchase", "Material · GRN", R.drawable.qa_wallet, requires = listOf("PURCHASE")),
            HubTile("COLLECTION", "Collection", "Allocate open AR", R.drawable.qa_arrow_left_right, requires = listOf("COLLECTION")),
            HubTile("PAYMENT", "Payment", "Settle open AP", R.drawable.qa_credit_card, requires = listOf("PAYMENT")),
            HubTile("SPEND", "Expense", "Running plant cost", R.drawable.qa_budget, requires = listOf("SPEND")),
            HubTile("STOCK", "Stock", "Material · WIP · FG", R.drawable.qa_package, requires = listOf("STOCK")),
            HubTile("ORDER", "Order", "Customer · commitment", R.drawable.qa_clipboard, requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("PRODUCTION", "Production", "Output · batch · scrap", R.drawable.qa_factory, requires = listOf("PRODUCTION")),
            HubTile("BUDGET", "Budget", "Versioned revise", R.drawable.qa_budget, requires = listOf("BUDGET")),
        )
    } else {
        listOf(
            HubTile("SALE", "Sale", "Invoice · buyer · GST", R.drawable.qa_receipt, requires = listOf("SALE")),
            HubTile("PURCHASE", "Purchase", "Supplier · GRN", R.drawable.qa_wallet, requires = listOf("PURCHASE")),
            HubTile("COLLECTION", "Collection", "Allocate open AR", R.drawable.qa_arrow_left_right, requires = listOf("COLLECTION")),
            HubTile("PAYMENT", "Payment", "Settle open AP", R.drawable.qa_credit_card, requires = listOf("PAYMENT")),
            HubTile("SPEND", "Expense", "Route · van · depot", R.drawable.qa_budget, requires = listOf("SPEND")),
            HubTile("STOCK", "Stock", "Adjust · count · batch", R.drawable.qa_package, requires = listOf("STOCK")),
            HubTile("ORDER", "Order", "SO · PO commitment", R.drawable.qa_clipboard, requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("SHIPMENT", "Delivery", "Dispatch · POD", R.drawable.qa_shipment, requires = listOf("SHIPMENT", "DELIVERY")),
            HubTile("BUDGET", "Budget", "Versioned revise", R.drawable.qa_budget, requires = listOf("BUDGET")),
        )
    }
    val support = listOf(
        HubTile(
            "PRINCIPALS",
            when {
                plantHub -> "Vendors & buyers"
                theme.isTrading -> "Suppliers & buyers"
                else -> "Principals & dealers"
            },
            when {
                plantHub -> "Vendors · customers"
                theme.isTrading -> "Suppliers · retailers"
                else -> "Principals · dealers"
            },
            R.drawable.qa_users,
            master = "Principals",
        ),
        HubTile(
            "CATALOGUE",
            if (plantHub) "Parts & BOM" else "Catalogue & SKU",
            if (plantHub) "Part · SKU · BOM" else "SKU · batch · price",
            R.drawable.qa_package,
            master = "Catalogue",
        ),
        HubTile(
            "LOCATIONS",
            when {
                plantHub -> "Lines & stores"
                theme.isTrading -> "Godowns & branches"
                else -> "Warehouses & routes"
            },
            if (plantHub) "Lines · WIP · FG" else "Depots · beats",
            R.drawable.qa_truck,
            master = "Locations",
        ),
        HubTile("INVITE", "Invite & QR", "Join · people · roles", R.drawable.qa_qr, master = "Invite"),
        HubTile("UPDATE", "Issue", "Update · blocker · risk", R.drawable.qa_alert, requires = listOf("UPDATE", "ISSUE")),
    )
    val utilities = if (plantHub) {
        listOf(
            HubTile("REWORK", "Rework / Scrap", "Rework · scrap · job work", R.drawable.qa_alert, requires = listOf("STOCK")),
            HubTile("HOLD", "Hold", "Park work order", R.drawable.qa_shield, requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("TRANSFER", "WIP transfer", "Between lines · stores", R.drawable.qa_shipment, requires = listOf("STOCK")),
            HubTile("REPRINT", "Reprint", "Invoice · job card", R.drawable.qa_receipt, requires = listOf("SALE")),
            HubTile("RETURN", "Return", "Credit · goods outcome", R.drawable.qa_arrow_left_right, requires = listOf("RETURN")),
            HubTile("DELIVERY", "Delivery", "Dispatch · POD", R.drawable.qa_shipment, requires = listOf("DELIVERY", "SHIPMENT")),
        )
    } else {
        emptyList()
    }

    fun filtered(tiles: List<HubTile>): List<HubTile> {
        val q = query.trim()
        if (q.isEmpty()) return tiles
        return tiles.filter {
            it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }
    }

    fun lockedReason(tile: HubTile): String? {
        if (tile.master != null || tile.requires.isEmpty() || allowed.isEmpty()) return null
        if (tile.requires.any { it in allowed }) return null
        return if (tile.id == "RETURN") "Unavailable" else "Role locked"
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        hubHero(theme, identity, allowed, onCapture)
        searchBar(theme, query) { query = it }
        tileGrid("Core actions · ${core.size}", "Tap to record", filtered(core), theme, ::lockedReason) { tile ->
            onCapture(tile.id)
        }
        tileGrid("Support & management · ${support.size}", "Role-gated", filtered(support), theme, ::lockedReason) { tile ->
            if (tile.master != null) onOpenMasters(tile.master) else onCapture(tile.id)
        }
        if (utilities.isNotEmpty()) {
            tileGrid("Utilities · ${utilities.size}", "Hold · rework · reprint", filtered(utilities), theme, ::lockedReason) { tile ->
                onCapture(tile.id)
            }
        }
        whyCard()
    }
}

private data class HubTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: Int,
    val master: String? = null,
    val requires: List<String> = emptyList(),
)

@Composable
private fun hubHero(
    theme: OperatingV12Theme,
    identity: String,
    allowed: Set<String>,
    onCapture: (String) -> Unit,
) {
    fun heroLocked(vararg codes: String): String? {
        if (allowed.isEmpty()) return null
        return if (codes.any { it in allowed }) null else "Role locked"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = theme.accent.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(24.dp))
            .background(OperatingV12.surface)
            .border(1.dp, OperatingV12.cyan.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "QUICK ADDS · v11.1",
            color = OperatingV12.cyan,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
        )
        Text("Quick Adds", color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        Text(theme.motionLine, color = theme.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(identity, color = OperatingV12.subtitle, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        if (theme.verticalKey == "manufacturing") {
            hubButton("Record Production", R.drawable.qa_factory, filled = true, theme, locked = heroLocked("PRODUCTION")) {
                onCapture("PRODUCTION")
            }
            hubButton("Record Sale", R.drawable.qa_receipt, filled = false, theme, locked = heroLocked("SALE")) {
                onCapture("SALE")
            }
            Text(
                "Accepted production posts finished stock once. Sale raises the ${theme.partyNoun.lowercase().dropLast(1)} invoice — scrap never becomes sellable qty.",
                color = OperatingV12.subtitle,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        } else {
            hubButton(
                "Dispatch",
                R.drawable.qa_shipment,
                filled = true,
                theme,
                locked = heroLocked("SHIPMENT", "DELIVERY"),
            ) { onCapture("SHIPMENT") }
            hubButton("Record Sale", R.drawable.qa_receipt, filled = false, theme, locked = heroLocked("SALE")) {
                onCapture("SALE")
            }
            Text(
                "Dispatch moves goods on a route; Record Sale raises the ${theme.partyNoun.lowercase().dropLast(1)} invoice.",
                color = OperatingV12.subtitle,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun hubButton(
    title: String,
    icon: Int,
    filled: Boolean,
    theme: OperatingV12Theme,
    locked: String? = null,
    onClick: () -> Unit,
) {
    val enabled = locked == null
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                if (filled && enabled) 20.dp else 0.dp,
                RoundedCornerShape(14.dp),
                ambientColor = theme.accent.copy(alpha = 0.5f),
                spotColor = theme.accent.copy(alpha = 0.5f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    filled -> theme.accent
                    else -> Color.White.copy(alpha = 0.06f)
                },
            )
            .border(
                1.dp,
                when {
                    !enabled -> theme.accent.copy(alpha = 0.15f)
                    filled -> Color.Transparent
                    else -> theme.accent.copy(alpha = 0.4f)
                },
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = when {
                !enabled -> OperatingV12.subtitle
                filled -> Color.White
                else -> theme.accent
            },
            modifier = Modifier.size(16.dp),
        )
        Text(
            locked ?: title,
            color = when {
                !enabled -> OperatingV12.subtitle
                filled -> Color.White
                else -> OperatingV12.title
            },
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun searchBar(theme: OperatingV12Theme, query: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.qa_search), contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            textStyle = TextStyle(color = OperatingV12.title, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
            cursorBrush = SolidColor(theme.accent),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Search actions...", color = Color(0xFF64748B), fontFamily = PlusJakartaSans, fontSize = 14.sp)
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun tileGrid(
    title: String,
    trailing: String,
    tiles: List<HubTile>,
    theme: OperatingV12Theme,
    lockedReason: (HubTile) -> String?,
    onOpen: (HubTile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OperatingV12SectionHead(title, trailing, OperatingV12.cyan)
        if (tiles.isEmpty()) {
            Text("No action matches.", color = OperatingV12.subtitle, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        } else {
            tiles.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { tile ->
                        val locked = lockedReason(tile)
                        OperatingV12ActionTile(
                            theme = theme,
                            title = tile.title,
                            subtitle = tile.subtitle,
                            icon = tile.icon,
                            locked = locked,
                            onClick = { if (locked == null) onOpen(tile) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun whyCard() {
    val rules = listOf(
        "Sale ≠ receipt — cash lands only via Collection",
        "Order = commitment only, no stock or ledger move",
        "Delivery = physical movement only",
        "Submit ≠ approval ≠ payment",
        "Stock changes only on receipt / dispatch",
        "Supplier stock in → buyer dispatch out → route settles collections",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(16.dp), ambientColor = OperatingV12.cyan.copy(alpha = 0.25f), spotColor = OperatingV12.cyan.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(16.dp))
            .background(OperatingV12.cyan.copy(alpha = 0.08f))
            .border(1.dp, OperatingV12.cyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Why these tiles save differently",
            color = OperatingV12.cyan,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        rules.forEach { rule ->
            Text(
                "• $rule",
                color = Color(0xFFCBD5E1),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}
