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

    val core = if (theme.isManufacturing) {
        listOf(
            HubTile("PRODUCTION", "Production", if (theme.isMadeToOrder) "Against job · accepted qty" else "Output · batch · scrap", R.drawable.qa_clipboard),
            HubTile("SALE", "Sale", "Invoice · buyer · GST", R.drawable.qa_receipt),
            HubTile("PURCHASE", "Purchase", "Material · GRN", R.drawable.qa_wallet),
            HubTile("CUSTOMER_ORDER", if (theme.isMadeToOrder) "Job order" else "Customer order", "Confirm · accept", R.drawable.qa_clipboard),
            HubTile("DELIVERY", "Delivery", "Dispatch · POD", R.drawable.qa_shipment),
            HubTile("COLLECTION", "Collection", "Allocate open AR", R.drawable.qa_arrow_left_right),
            HubTile("PAYMENT", "Payment", "Settle open AP", R.drawable.qa_credit_card),
            HubTile("STOCK", "Stock", "Material · WIP · FG", R.drawable.qa_package),
            HubTile("BUDGET", "Budget", "Versioned revise", R.drawable.qa_budget),
        )
    } else {
        listOf(
            HubTile("SALE", "Sale", "Invoice · buyer · GST", R.drawable.qa_receipt),
            HubTile("PURCHASE", "Purchase", "Supplier · GRN", R.drawable.qa_wallet),
            HubTile("COLLECTION", "Collection", "Allocate open AR", R.drawable.qa_arrow_left_right),
            HubTile("PAYMENT", "Payment", "Settle open AP", R.drawable.qa_credit_card),
            HubTile("SPEND", "Expense", "Route · van · depot", R.drawable.qa_budget),
            HubTile("STOCK", "Stock", "Adjust · count · batch", R.drawable.qa_package),
            HubTile("ORDER", "Order", "SO · PO commitment", R.drawable.qa_clipboard),
            HubTile("SHIPMENT", "Delivery", "Dispatch · POD", R.drawable.qa_shipment),
            HubTile("BUDGET", "Budget", "Versioned revise", R.drawable.qa_budget),
        )
    }
    val support = listOf(
        HubTile(
            "PRINCIPALS",
            when {
                theme.isManufacturing -> "Vendors & buyers"
                theme.isTrading -> "Suppliers & buyers"
                else -> "Principals & dealers"
            },
            when {
                theme.isManufacturing -> "Vendors · customers"
                theme.isTrading -> "Suppliers · retailers"
                else -> "Principals · dealers"
            },
            R.drawable.qa_users,
            master = "Principals",
        ),
        HubTile(
            "CATALOGUE",
            if (theme.isManufacturing) "Parts & BOM" else "Catalogue & SKU",
            if (theme.isManufacturing) "Part · SKU · BOM" else "SKU · batch · price",
            R.drawable.qa_package,
            master = "Catalogue",
        ),
        HubTile(
            "LOCATIONS",
            when {
                theme.isManufacturing -> "Lines & stores"
                theme.isTrading -> "Godowns & branches"
                else -> "Warehouses & routes"
            },
            if (theme.isManufacturing) "Lines · WIP · FG" else "Depots · beats",
            R.drawable.qa_truck,
            master = "Locations",
        ),
        HubTile("INVITE", "Invite & QR", "Join · people · roles", R.drawable.qa_qr, master = "Invite"),
        HubTile("UPDATE", "Issue", "Update · blocker · risk", R.drawable.qa_alert),
    )

    fun filtered(tiles: List<HubTile>): List<HubTile> {
        val q = query.trim()
        if (q.isEmpty()) return tiles
        return tiles.filter {
            it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        hubHero(theme, identity, onCapture)
        searchBar(theme, query) { query = it }
        tileGrid("Core actions · ${core.size}", "Tap to record", filtered(core), theme) { tile ->
            onCapture(tile.id)
        }
        tileGrid("Support & management · ${support.size}", "Role-gated", filtered(support), theme) { tile ->
            if (tile.master != null) onOpenMasters(tile.master) else onCapture(tile.id)
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
)

@Composable
private fun hubHero(
    theme: OperatingV12Theme,
    identity: String,
    onCapture: (String) -> Unit,
) {
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
        hubButton("Dispatch", R.drawable.qa_shipment, filled = true, theme) { onCapture("SHIPMENT") }
        hubButton("Record Sale", R.drawable.qa_receipt, filled = false, theme) { onCapture("SALE") }
        Text(
            "Dispatch moves goods on a route; Record Sale raises the ${theme.partyNoun.lowercase().dropLast(1)} invoice.",
            color = OperatingV12.subtitle,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun hubButton(
    title: String,
    icon: Int,
    filled: Boolean,
    theme: OperatingV12Theme,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (filled) 20.dp else 0.dp, RoundedCornerShape(14.dp), ambientColor = theme.accent.copy(alpha = 0.5f), spotColor = theme.accent.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) theme.accent else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (filled) Color.Transparent else theme.accent.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (filled) Color.White else theme.accent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            title,
            color = if (filled) Color.White else OperatingV12.title,
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
                        OperatingV12ActionTile(
                            theme = theme,
                            title = tile.title,
                            subtitle = tile.subtitle,
                            icon = tile.icon,
                            onClick = { onOpen(tile) },
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
