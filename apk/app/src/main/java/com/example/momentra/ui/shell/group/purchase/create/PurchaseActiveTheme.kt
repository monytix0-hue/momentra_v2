package com.example.momentra.ui.shell.group.purchase.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.group.shared.GroupExperienceFamily
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma Gift Pool 601:* / Group Purchase 605:* / Shared Asset 605:* / Custom Purchase 617:*. */
data class PurchaseActiveTheme(
    val bg: Color,
    val accent: Color,
    val accentLight: Color,
    val accentSoft: Color,
    val accentSolid: Color,
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val card: Color,
    val border: Color,
    val darkText: Color,
    val typeLabel: String,
    val pulseTitle: String,
    val contributionsTitle: String,
    val budgetTitle: String,
    val insightsTitle: String,
    val healthLabel: String,
    val hubHeroRes: Int,
    val heroEmoji: String,
    val participantRoles: List<String>,
    val participantSubtitle: String,
    val heroGradient: Brush,
    val pulseHeroGradient: Brush,
    val includesVendor: Boolean,
    val includesOwnership: Boolean,
    val includesDelivery: Boolean,
    val includesContributor: Boolean,
    val includesBudget: Boolean,
    val quickChips: List<Triple<String, String, PurchaseQuickAddKind>>,
    val statGradients: List<Brush>,
) {
    companion object {
        val GiftPool = PurchaseActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFFEC4899),
            accentLight = Color(0xFFF472B6),
            accentSoft = Color(0x33EC4899),
            accentSolid = Color(0xFFDB2777),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Gift Pool",
            pulseTitle = "Gift Pool Pulse",
            contributionsTitle = "Member Contributions",
            budgetTitle = "Pool Budget",
            insightsTitle = "Gift Pool Insights",
            healthLabel = "Pool Health",
            hubHeroRes = com.example.momentra.R.drawable.gift_pool_hub_hero,
            heroEmoji = "🎁",
            participantRoles = listOf("Organizer", "Contributor", "Recipient"),
            participantSubtitle = "Invite contributors to the gift pool",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFFF472B6),
                    0.75f to Color(0xFFDB2777).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesVendor = false,
            includesOwnership = false,
            includesDelivery = true,
            includesContributor = true,
            includesBudget = true,
            quickChips = listOf(
                Triple("🎁", "Contribute", PurchaseQuickAddKind.CONTRIBUTION),
                Triple("📷", "Photos", PurchaseQuickAddKind.MEMORY),
                Triple("💰", "Budget", PurchaseQuickAddKind.BUDGET),
                Triple("🛒", "Items", PurchaseQuickAddKind.PURCHASE_ITEM),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFFDB2777), Color(0xFFBE185D))),
                Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFDB2777))),
                Brush.horizontalGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899))),
                Brush.horizontalGradient(listOf(Color(0xFFF9A8D4), Color(0xFFEC4899))),
            ),
        )

        val GroupPurchase = PurchaseActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFFFF7A3D),
            accentLight = Color(0xFFFB923C),
            accentSoft = Color(0x33FF7A3D),
            accentSolid = Color(0xFFEA580C),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Group Purchase",
            pulseTitle = "Purchase Pulse",
            contributionsTitle = "Member Contributions",
            budgetTitle = "Purchase Budget",
            insightsTitle = "Purchase Insights",
            healthLabel = "Purchase Health",
            hubHeroRes = com.example.momentra.R.drawable.group_purchase_hub_hero,
            heroEmoji = "🛒",
            participantRoles = listOf("Organizer", "Buyer", "Member"),
            participantSubtitle = "Invite people joining this purchase",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFFFB923C), Color(0xFFFF7A3D))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFFFB923C),
                    0.75f to Color(0xFFEA580C).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesVendor = true,
            includesOwnership = false,
            includesDelivery = true,
            includesContributor = true,
            includesBudget = true,
            quickChips = listOf(
                Triple("🛒", "Items", PurchaseQuickAddKind.PURCHASE_ITEM),
                Triple("💳", "Expense", PurchaseQuickAddKind.EXPENSE),
                Triple("🏪", "Vendor", PurchaseQuickAddKind.VENDOR),
                Triple("📦", "Delivery", PurchaseQuickAddKind.DELIVERY),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C))),
                Brush.horizontalGradient(listOf(Color(0xFFFF7A3D), Color(0xFFEA580C))),
                Brush.horizontalGradient(listOf(Color(0xFFFB923C), Color(0xFFFF7A3D))),
                Brush.horizontalGradient(listOf(Color(0xFFFDBA74), Color(0xFFFF7A3D))),
            ),
        )

        val SharedAsset = PurchaseActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFF8B5CF6),
            accentLight = Color(0xFFA78BFA),
            accentSoft = Color(0x338B5CF6),
            accentSolid = Color(0xFF7C3AED),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Shared Asset",
            pulseTitle = "Asset Pulse",
            contributionsTitle = "Member Contributions",
            budgetTitle = "Asset Budget",
            insightsTitle = "Asset Insights",
            healthLabel = "Asset Health",
            hubHeroRes = com.example.momentra.R.drawable.shared_asset_hub_hero,
            heroEmoji = "📷",
            participantRoles = listOf("Owner", "Co-owner", "Member"),
            participantSubtitle = "Invite co-owners for this shared asset",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFFA78BFA),
                    0.75f to Color(0xFF7C3AED).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesVendor = true,
            includesOwnership = true,
            includesDelivery = true,
            includesContributor = true,
            includesBudget = true,
            quickChips = listOf(
                Triple("🎁", "Contribute", PurchaseQuickAddKind.CONTRIBUTION),
                Triple("🔑", "Ownership", PurchaseQuickAddKind.OWNERSHIP),
                Triple("🏪", "Vendor", PurchaseQuickAddKind.VENDOR),
                Triple("📷", "Photos", PurchaseQuickAddKind.MEMORY),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))),
                Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))),
                Brush.horizontalGradient(listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6))),
                Brush.horizontalGradient(listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6))),
            ),
        )

        val CustomPurchase = PurchaseActiveTheme(
            bg = Color(0xFF14121B),
            accent = Color(0xFFF59E0B),
            accentLight = Color(0xFFFBBF24),
            accentSoft = Color(0x33F59E0B),
            accentSolid = Color(0xFFD97706),
            text = Color(0xFFE5E2E1),
            secondary = Color(0xFF9CA3AF),
            muted = Color(0xFFA8A19E),
            card = Color(0xFF1C1926),
            border = Color(0xFF2A2538),
            darkText = Color(0xFF14121B),
            typeLabel = "Custom Purchase",
            pulseTitle = "Purchase Pulse",
            contributionsTitle = "Member Contributions",
            budgetTitle = "Purchase Budget",
            insightsTitle = "Purchase Insights",
            healthLabel = "Purchase Health",
            hubHeroRes = com.example.momentra.R.drawable.custom_purchase_hub_hero,
            heroEmoji = "✨",
            participantRoles = listOf("Organizer", "Contributor", "Member"),
            participantSubtitle = "Invite people to this custom purchase",
            heroGradient = Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
            pulseHeroGradient = Brush.linearGradient(
                colorStops = arrayOf(
                    0.25f to Color(0xFFFBBF24),
                    0.75f to Color(0xFFD97706).copy(alpha = 0.9f),
                ),
                start = Offset(40f, 0f),
                end = Offset(360f, 400f),
            ),
            includesVendor = true,
            includesOwnership = true,
            includesDelivery = true,
            includesContributor = false,
            includesBudget = false,
            quickChips = listOf(
                Triple("🛒", "Items", PurchaseQuickAddKind.PURCHASE_ITEM),
                Triple("💳", "Expense", PurchaseQuickAddKind.EXPENSE),
                Triple("📦", "Delivery", PurchaseQuickAddKind.DELIVERY),
                Triple("🔑", "Ownership", PurchaseQuickAddKind.OWNERSHIP),
            ),
            statGradients = listOf(
                Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFB45309))),
                Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))),
                Brush.horizontalGradient(listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))),
            ),
        )

        fun forFamily(family: GroupExperienceFamily): PurchaseActiveTheme =
            when (family) {
                GroupExperienceFamily.GROUP_PURCHASE -> GroupPurchase
                GroupExperienceFamily.SHARED_ASSET -> SharedAsset
                GroupExperienceFamily.CUSTOM_PURCHASE -> CustomPurchase
                else -> GiftPool
            }
    }
}

enum class PurchaseQuickAddKind {
    CONTRIBUTOR,
    CONTRIBUTION,
    EXPENSE,
    BUDGET,
    VENDOR,
    POLL,
    UPDATE,
    MEMORY,
    PURCHASE_ITEM,
    DELIVERY,
    OWNERSHIP,
}

fun PurchaseQuickAddKind.label(): String = when (this) {
    PurchaseQuickAddKind.CONTRIBUTOR -> "Contributor"
    PurchaseQuickAddKind.CONTRIBUTION -> "Contribution"
    PurchaseQuickAddKind.EXPENSE -> "Expense"
    PurchaseQuickAddKind.BUDGET -> "Budget"
    PurchaseQuickAddKind.VENDOR -> "Vendor"
    PurchaseQuickAddKind.POLL -> "Poll"
    PurchaseQuickAddKind.UPDATE -> "Update"
    PurchaseQuickAddKind.MEMORY -> "Memory"
    PurchaseQuickAddKind.PURCHASE_ITEM -> "Purchase Item"
    PurchaseQuickAddKind.DELIVERY -> "Delivery"
    PurchaseQuickAddKind.OWNERSHIP -> "Ownership"
}

fun PurchaseQuickAddKind.emoji(): String = when (this) {
    PurchaseQuickAddKind.CONTRIBUTOR -> "👤"
    PurchaseQuickAddKind.CONTRIBUTION -> "🎁"
    PurchaseQuickAddKind.EXPENSE -> "💳"
    PurchaseQuickAddKind.BUDGET -> "💰"
    PurchaseQuickAddKind.VENDOR -> "🏪"
    PurchaseQuickAddKind.POLL -> "📊"
    PurchaseQuickAddKind.UPDATE -> "📢"
    PurchaseQuickAddKind.MEMORY -> "📷"
    PurchaseQuickAddKind.PURCHASE_ITEM -> "🛒"
    PurchaseQuickAddKind.DELIVERY -> "📦"
    PurchaseQuickAddKind.OWNERSHIP -> "🔑"
}

fun purchaseHubTiles(theme: PurchaseActiveTheme): List<PurchaseQuickAddKind> {
    val tiles = mutableListOf<PurchaseQuickAddKind>()
    if (theme.includesContributor) tiles.add(PurchaseQuickAddKind.CONTRIBUTOR)
    tiles.add(PurchaseQuickAddKind.CONTRIBUTION)
    tiles.add(PurchaseQuickAddKind.EXPENSE)
    if (theme.includesBudget) tiles.add(PurchaseQuickAddKind.BUDGET)
    tiles.add(PurchaseQuickAddKind.PURCHASE_ITEM)
    if (theme.includesVendor) tiles.add(PurchaseQuickAddKind.VENDOR)
    if (theme.includesDelivery) tiles.add(PurchaseQuickAddKind.DELIVERY)
    if (theme.includesOwnership) tiles.add(PurchaseQuickAddKind.OWNERSHIP)
    tiles.addAll(
        listOf(
            PurchaseQuickAddKind.POLL,
            PurchaseQuickAddKind.UPDATE,
            PurchaseQuickAddKind.MEMORY,
        ),
    )
    return tiles
}

@Composable
fun PurchaseSectionCard(
    theme: PurchaseActiveTheme,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.card)
            .border(1.dp, theme.border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = theme.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PlusJakartaSans)
            trailing?.invoke()
        }
        content()
    }
}

@Composable
fun PurchaseEmptyBlock(theme: PurchaseActiveTheme, message: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(message, color = theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(detail, color = theme.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun PurchaseStatCard(label: String, value: String, gradient: Brush, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(value, color = Color.White.copy(alpha = 0.95f), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun PurchaseEmojiChip(
    theme: PurchaseActiveTheme,
    label: String,
    emoji: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.card)
                .border(1.dp, theme.border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Text(label, color = theme.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun PurchaseCrewRow(
    theme: PurchaseActiveTheme,
    name: String,
    role: String,
    amountLabel: String,
    featured: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(theme.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), color = theme.accentLight, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$name ($role)${if (featured) " ★" else ""}",
                color = theme.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(if (featured) "Top contributor" else "Contributor", color = theme.muted, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.accentSoft),
            )
        }
        Text(amountLabel, color = theme.accentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}
