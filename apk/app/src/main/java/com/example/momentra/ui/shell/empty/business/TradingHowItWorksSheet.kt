package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

data class TradingHowItWorksCopy(
    val context: String,
    val intro: String,
    val cards: List<Triple<String, String, String>>,
    val snapshotTitle: String,
    val metrics: List<String>,
    val closing: String,
    val cta: String,
) {
    companion object {
        fun forSubtype(code: String): TradingHowItWorksCopy? = when (code) {
            "TRADING_WHOLESALE" -> TradingHowItWorksCopy(
                context = "Bring your orders, stock, deliveries and payments together.",
                intro = "Imagine you buy products from suppliers and sell them to customers. Some goods arrive today, others later. Some customers pay now, others after delivery.",
                cards = listOf(
                    Triple("You order", "You order 100 boxes from a supplier.", "100 boxes expected."),
                    Triple("Goods arrive", "Only 60 boxes arrive.", "40 boxes still to receive."),
                    Triple("You sell", "A customer orders 40 boxes.", "40 boxes committed to the customer."),
                    Triple("You deliver", "You deliver 25 boxes.", "15 boxes still to deliver."),
                    Triple("You collect", "Your customer pays ₹10,000 now.", "The remaining amount stays visible until collected."),
                    Triple("You pay", "You make a payment to your supplier.", "What you still owe updates."),
                ),
                snapshotTitle = "Your trading at a glance",
                metrics = listOf("40 boxes to receive", "15 boxes to deliver", "Collections to follow up"),
                closing = "Know what is bought, sold, delivered and still pending.",
                cta = "Create my Trading Moment",
            )
            "DISTRIBUTION_DEALERSHIP" -> TradingHowItWorksCopy(
                context = "Keep your retailer orders, stock, deliveries and collections connected.",
                intro = "Imagine you supply products to several retailers. Orders come from different shops, deliveries happen in parts, and payments arrive on different days.",
                cards = listOf(
                    Triple("You order", "You order 500 cases from a supplier.", "500 cases expected."),
                    Triple("Stock arrives", "400 cases arrive first.", "100 cases still to receive."),
                    Triple("Retailers order", "Three retailers order 300 cases in total.", "Orders committed across your retailers."),
                    Triple("You dispatch", "You deliver 200 cases against those sales.", "100 cases still to deliver."),
                    Triple("Collections arrive", "Two retailers make part payments.", "Outstanding collections by retailer."),
                    Triple("Supplier payment", "A supplier payment is due next week.", "The upcoming payment needs attention."),
                ),
                snapshotTitle = "Your distribution at a glance",
                metrics = listOf("100 cases to receive", "100 cases to deliver", "3 retailer orders"),
                closing = "Know what is moving, what is pending and who needs attention.",
                cta = "Create my Distribution Moment",
            )
            else -> null
        }
    }
}

private val places = listOf(
    "Pulse" to "What needs your attention",
    "Moments" to "Your orders, stock and commitments",
    "Life" to "What is happening across your business",
    "Memory" to "How your business has progressed",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingHowItWorksSheet(
    copy: TradingHowItWorksCopy,
    accent: Color,
    ink: Color,
    actionInk: Color,
    onExpand: () -> Unit,
    onCreate: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expanded by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF122235),
    ) {
        val sheetCap = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
        Column(Modifier.fillMaxWidth().heightIn(max = sheetCap)) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "See how your business comes together",
                    color = ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "An order comes in. Stock arrives. Goods go out. Payments follow. Momentra helps you see what happened and what still needs attention.",
                    color = ink.copy(alpha = 0.72f),
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Text(
                    "Example only · Not your business data",
                    color = accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Text(copy.intro, color = ink.copy(alpha = 0.84f), fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 20.sp)
                Text("A simple example", color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                copy.cards.take(if (expanded) 6 else 4).forEachIndexed { index, card ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(12.dp)
                            .semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("${index + 1} · ${card.first}", color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(card.second, color = ink.copy(alpha = 0.84f), fontFamily = PlusJakartaSans, fontSize = 13.sp, lineHeight = 18.sp)
                        Text(card.third, color = accent, fontFamily = PlusJakartaSans, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
                TextButton(onClick = {
                    expanded = !expanded
                    if (expanded) onExpand()
                }) {
                    Text(
                        if (expanded) "Show less" else "See the complete journey",
                        color = accent,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
                Text(copy.snapshotTitle, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(copy.metrics.joinToString("  ·  "), color = ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 20.sp)
                Text(
                    "Sample figures for illustration. Your actual Moment will show your own business activity.",
                    color = ink.copy(alpha = 0.64f),
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    "You record what happens. Momentra connects what comes next.",
                    color = ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Text(
                    "Start with any activity. You do not need an order before recording a sale or purchase.",
                    color = ink.copy(alpha = 0.72f),
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Text(copy.closing, color = ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 20.sp)
                Text(
                    "Once you start, Momentra brings the picture together.",
                    color = ink,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                places.forEach { (name, line) ->
                    Column(Modifier.semantics(mergeDescendants = true) {}) {
                        Text(name, color = ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(line, color = ink.copy(alpha = 0.72f), fontFamily = PlusJakartaSans, fontSize = 13.sp)
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent)
                        .semantics { contentDescription = copy.cta }
                        .clickable(onClick = onCreate),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        copy.cta,
                        color = actionInk,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", color = ink.copy(alpha = 0.72f), fontFamily = PlusJakartaSans, fontSize = 14.sp)
                }
            }
        }
    }
}
