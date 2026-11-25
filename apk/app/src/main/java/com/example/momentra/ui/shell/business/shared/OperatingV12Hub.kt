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
    // Empty capabilities ⇒ leave tiles open (projection still loading). Same honesty as Retail/MTO hubs.
    val allowed = model.quickAdds.map { it.code.uppercase() }.toSet()

    // MFG-HUB-v3 `2396:29892` — plant journey only. MTO mounts MadeToOrderQuickAddHub, never this grid.
    val plantHub = theme.verticalKey == "manufacturing"
    val distHub = theme.verticalKey == "distribution"

    // DST-HUB-v11 Figma groups: Money / Operations / People / Utilities.
    val money = if (distHub) {
        listOf(
            HubTile("SALE", "Sale", "Invoice · dealer · GST", TradingChromeIcons.hubDrawable("SALE"), requires = listOf("SALE")),
            HubTile("PURCHASE", "Purchase", "Supplier · GRN", TradingChromeIcons.hubDrawable("PURCHASE"), requires = listOf("PURCHASE")),
            HubTile("COLLECTION", "Collection", "Allocate open AR", TradingChromeIcons.hubDrawable("COLLECTION"), requires = listOf("COLLECTION")),
            HubTile("PAYMENT", "Payment", "Settle open AP", TradingChromeIcons.hubDrawable("PAYMENT"), requires = listOf("PAYMENT")),
            HubTile("SPEND", "Expense", "Route · van · depot", TradingChromeIcons.hubDrawable("SPEND"), requires = listOf("SPEND")),
        )
    } else emptyList()
    val operations = if (distHub) {
        listOf(
            HubTile("STOCK", "Stock", "Adjust · count · batch", TradingChromeIcons.hubDrawable("STOCK"), requires = listOf("STOCK")),
            HubTile("RETURN", "Return", "Credit · goods outcome", TradingChromeIcons.hubDrawable("RETURN"), requires = listOf("RETURN")),
            HubTile("ORDER", "Order", "SO · PO commitment", TradingChromeIcons.hubDrawable("ORDER"), requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("SHIPMENT", "Delivery", "Dispatch · POD", TradingChromeIcons.hubDrawable("SHIPMENT"), requires = listOf("SHIPMENT", "DELIVERY")),
            HubTile("BUDGET", "Budget", "Versioned revise", TradingChromeIcons.hubDrawable("BUDGET"), requires = listOf("BUDGET")),
        )
    } else emptyList()
    val people = if (distHub) {
        listOf(
            HubTile("PRINCIPALS", "Principals & dealers", "Principals · dealers · channels", TradingChromeIcons.hubDrawable("PRINCIPALS"), master = "Principals"),
            HubTile("CATALOGUE", "Catalogue & SKU", "SKU · batch · price", TradingChromeIcons.hubDrawable("CATALOGUE"), master = "Catalogue"),
            HubTile("LOCATIONS", "Warehouses & routes", "Depots · beats", TradingChromeIcons.hubDrawable("LOCATIONS"), master = "Locations"),
            HubTile("INVITE", "Invite & QR", "Join · people · roles", TradingChromeIcons.hubDrawable("INVITE"), master = "Invite"),
            HubTile("UPDATE", "Update / Issue", "Update · blocker · risk", TradingChromeIcons.hubDrawable("UPDATE"), requires = listOf("UPDATE", "ISSUE")),
        )
    } else emptyList()

    val core = if (plantHub) {
        listOf(
            HubTile("SALE", "Sale", "Invoice · buyer · GST", TradingChromeIcons.hubDrawable("SALE"), requires = listOf("SALE")),
            HubTile("PURCHASE", "Purchase", "Material · GRN", TradingChromeIcons.hubDrawable("PURCHASE"), requires = listOf("PURCHASE")),
            HubTile("COLLECTION", "Collection", "Allocate open AR", TradingChromeIcons.hubDrawable("COLLECTION"), requires = listOf("COLLECTION")),
            HubTile("PAYMENT", "Payment", "Settle open AP", TradingChromeIcons.hubDrawable("PAYMENT"), requires = listOf("PAYMENT")),
            HubTile("SPEND", "Expense", "Running plant cost", TradingChromeIcons.hubDrawable("SPEND"), requires = listOf("SPEND")),
            HubTile("STOCK", "Stock", "Material · WIP · FG", TradingChromeIcons.hubDrawable("STOCK"), requires = listOf("STOCK")),
            HubTile("ORDER", "Order", "Customer · commitment", TradingChromeIcons.hubDrawable("ORDER"), requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("PRODUCTION", "Production", "Output · batch · scrap", TradingChromeIcons.hubDrawable("PRODUCTION"), requires = listOf("PRODUCTION")),
            HubTile("BUDGET", "Budget", "Versioned revise", TradingChromeIcons.hubDrawable("BUDGET"), requires = listOf("BUDGET")),
        )
    } else if (theme.isTrading) {
        // TRD-QA-01…12 hub titles — match Figma capture sheet names.
        TradingChromeIcons.tradingHubCoreIds.map { id ->
            val (title, subtitle, requires) = when (id) {
                "SALE" -> Triple("Sale", "TRD-QA-01 · amount · buyer", listOf("SALE"))
                "PURCHASE" -> Triple("Purchase", "TRD-QA-02 · supplier · goods", listOf("PURCHASE"))
                "COLLECTION" -> Triple("Collection", "TRD-QA-03 · allocate AR", listOf("COLLECTION"))
                "PAYMENT" -> Triple("Payment", "TRD-QA-04 · settle AP", listOf("PAYMENT"))
                "SPEND" -> Triple("Expense", "TRD-QA-05 · paid / later", listOf("SPEND"))
                "STOCK" -> Triple("Stock", "TRD-QA-06 · set · add · reduce", listOf("STOCK"))
                "RETURN" -> Triple("Return", "TRD-QA-07 · credit note", listOf("RETURN"))
                "BUDGET" -> Triple("Budget", "TRD-QA-08 · versioned revise", listOf("BUDGET"))
                "ORDER" -> Triple("Order", "TRD-QA-11 · commitment only", listOf("ORDER", "CUSTOMER_ORDER"))
                "SHIPMENT" -> Triple("Delivery", "TRD-QA-12 · goods move", listOf("SHIPMENT", "DELIVERY"))
                else -> error("Unknown trading hub core id: $id")
            }
            HubTile(id, title, subtitle, TradingChromeIcons.hubDrawable(id), requires = requires)
        }
    } else emptyList()
    val support = if (distHub) emptyList() else listOf(
        HubTile(
            "PRINCIPALS",
            when {
                plantHub -> "Vendors & buyers"
                theme.isTrading -> "Suppliers & buyers"
                else -> "Business contacts"
            },
            when {
                plantHub -> "Vendors · customers"
                theme.isTrading -> "Suppliers · retailers"
                else -> "Principals · dealers"
            },
            TradingChromeIcons.hubDrawable("PRINCIPALS"),
            master = "Principals",
        ),
        HubTile(
            "CATALOGUE",
            if (plantHub) "Parts & BOM" else "Catalogue & SKU",
            if (plantHub) "Part · SKU · BOM" else "SKU · batch · price",
            TradingChromeIcons.hubDrawable("CATALOGUE"),
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
            TradingChromeIcons.hubDrawable("LOCATIONS"),
            master = "Locations",
        ),
        HubTile("INVITE", "Invite & QR", "Join · people · roles", TradingChromeIcons.hubDrawable("INVITE"), master = "Invite"),
        HubTile(
            "UPDATE",
            if (theme.isTrading) "Update / Issue" else "Issue",
            if (theme.isTrading) "TRD-QA-09 · ops signal" else "Update · blocker · risk",
            TradingChromeIcons.hubDrawable("UPDATE"),
            requires = listOf("UPDATE", "ISSUE"),
        ),
        if (theme.isTrading) {
            HubTile(
                "APPROVAL",
                "Approval",
                "TRD-QA-10 · decide · reimburse",
                TradingChromeIcons.hubDrawable("APPROVAL"),
                requires = listOf("UPDATE", "ISSUE", "PAYMENT"),
            )
        } else {
            null
        },
    ).filterNotNull()
    val utilities = when {
        plantHub -> listOf(
            HubTile("REWORK", "Rework / Scrap", "Rework · scrap · job work", TradingChromeIcons.hubDrawable("UPDATE"), requires = listOf("STOCK")),
            HubTile("HOLD", "Hold", "Park work order", TradingChromeIcons.hubDrawable("HOLD"), requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("TRANSFER", "WIP transfer", "Between lines · stores", TradingChromeIcons.hubDrawable("TRANSFER"), requires = listOf("STOCK")),
            HubTile("REPRINT", "Reprint", "Invoice · job card", TradingChromeIcons.hubDrawable("REPRINT"), requires = listOf("SALE")),
            HubTile("RETURN", "Return", "Credit · goods outcome", TradingChromeIcons.hubDrawable("RETURN"), requires = listOf("RETURN")),
            HubTile("DELIVERY", "Delivery", "Dispatch · POD", TradingChromeIcons.hubDrawable("DELIVERY"), requires = listOf("DELIVERY", "SHIPMENT")),
        )
        theme.isTrading || distHub -> listOf(
            HubTile("HOLD", "Hold", "Freeze open order or DO", TradingChromeIcons.hubDrawable("HOLD"), requires = listOf("ORDER", "CUSTOMER_ORDER")),
            HubTile("TRANSFER", "Transfer", "Move stock between locations", TradingChromeIcons.hubDrawable("TRANSFER"), requires = listOf("STOCK")),
            HubTile("REPRINT", "Reprint", "Invoice · duplicate copy", TradingChromeIcons.hubDrawable("REPRINT"), requires = listOf("SALE")),
        )
        else -> emptyList()
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
        // Figma Hub Hero `5:16497` — Trading Quick Adds only.
        if (theme.isTrading) {
            OperatingV12HubHero(model = model, theme = theme)
        }
        searchBar(theme, query) { query = it }
        if (distHub) {
            tileGrid("Money · ${filtered(money).size}", "Sale · settle · spend", filtered(money), theme, ::lockedReason) { tile ->
                onCapture(tile.id)
            }
            tileGrid("Operations · ${filtered(operations).size}", "Stock · order · delivery", filtered(operations), theme, ::lockedReason) { tile ->
                onCapture(tile.id)
            }
            tileGrid("People · ${filtered(people).size}", "Principals · catalogue · routes", filtered(people), theme, ::lockedReason) { tile ->
                if (tile.master != null) onOpenMasters(tile.master) else onCapture(tile.id)
            }
            tileGrid("Utilities · ${filtered(utilities).size}", "Hold · transfer · reprint", filtered(utilities), theme, ::lockedReason) { tile ->
                onCapture(tile.id)
            }
        } else {
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
        }
        whyCard(distribution = distHub)
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
        Icon(painterResource(TradingChromeIcons.hubDrawable("SEARCH")), contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
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
private fun whyCard(distribution: Boolean = false) {
    val rules = listOf(
        "Sale ≠ receipt — cash lands only via Collection",
        "Order = commitment only, no stock or ledger move",
        "Delivery = physical movement only",
        "Submit ≠ approval ≠ payment",
        "Stock changes only on receipt / dispatch",
        if (distribution) {
            "Supplier stock in → dealer dispatch out → route settles collections"
        } else {
            "Supplier stock in → buyer dispatch out → route settles collections"
        },
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
