package com.example.momentra.ui.shell.empty.business

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.shell.business.shared.BusinessAudience
import com.example.momentra.ui.shell.business.shared.CompanyModules
import com.example.momentra.ui.shell.maestro.MaestroIds

private val Bg = Color(0xFF0C0F15)
private val Accent = Color(0xFF818CF8)
private val Border = Color(0xFF1E293B)
private val Card = Color(0xFF161B26)
private val Muted = Color(0xFF94A3B8)
private val Dim = Color(0xFF64748B)

private enum class BizCreateTab { Moment, Memory }

private data class BizCategory(
    val title: String,
    val body: String,
    @DrawableRes val icon: Int,
    @DrawableRes val card: Int,
    val comingSoon: Boolean,
    val setupKind: BusinessSetupKind? = null,
    val testTag: String? = null,
)

private data class BizMemory(
    val title: String,
    val body: String,
    val tag: String,
    val date: String,
    val dot: Color,
)

/** Figma 658:9451 + 658:9573 — Business Create / Choose Moment & Memory */
@Composable
fun BusinessCreateMomentContent(
    onBack: () -> Unit,
    onSelectSetup: (BusinessSetupKind) -> Unit,
    companyId: String? = null,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(BizCreateTab.Moment) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val context = LocalContext.current
    val showRetailTagline = remember(companyId) {
        BusinessAudience.isSmallShop(BusinessAudience.forCompany(context, companyId))
    }

    val categories = remember(showRetailTagline, companyId) {
        val live = listOf(
            BizCategory(
                "Team & Work",
                if (showRetailTagline) "Staff updates, attendance, and shop team notes"
                else "Manage hiring, attendance, performance & team growth",
                R.drawable.ic_biz_create_users,
                R.drawable.ic_biz_create_card_team,
                false,
                BusinessSetupKind.TEAM_OPERATIONS,
                MaestroIds.BUSINESS_SETUP_TEAM,
            ),
            BizCategory(
                "Money & Cash Flow",
                if (showRetailTagline) "Track cash, sales, and shop spends"
                else "Monitor cash and spending.",
                R.drawable.ic_biz_create_trending,
                R.drawable.ic_biz_create_card_runway,
                false,
                BusinessSetupKind.BUSINESS_RUNWAY,
                MaestroIds.BUSINESS_SETUP_RUNWAY,
            ),
            BizCategory(
                "Daily Business",
                if (showRetailTagline) "Suppliers, day-to-day spends, and shop issues"
                else "Organize departments, processes and workflows.",
                R.drawable.ic_biz_create_credit_card,
                R.drawable.ic_biz_create_card_ops,
                false,
                BusinessSetupKind.BUSINESS_OPERATIONS,
                MaestroIds.BUSINESS_SETUP_OPS,
            ),
        )
        val all = if (showRetailTagline) {
            live
        } else {
            live + listOf(
                BizCategory("Project Operations", "Organize tasks, milestones, sprints & deliverables", R.drawable.ic_biz_create_layers, R.drawable.ic_biz_create_card_project, true),
                BizCategory("Event Operations", "Organize events from planning through execution.", R.drawable.ic_biz_create_wallet, R.drawable.ic_biz_create_card_event, true),
                BizCategory("Vendor Operations", "Manage suppliers, contracts, procurement & partnerships", R.drawable.ic_biz_create_briefcase, R.drawable.ic_biz_create_card_vendor, true),
            )
        }
        all.filter { cat ->
            val kind = cat.setupKind ?: return@filter true
            val key = CompanyModules.moduleKeyForSetupKind(kind) ?: return@filter true
            CompanyModules.isEnabled(context, companyId, key)
        }
    }
    val memories = remember { emptyList<BizMemory>() }
    val filters = listOf("All", "Revenue", "Team", "Strategy", "Projects", "Expenses")
    val filtered = memories.filter {
        (filter == "All" || it.tag == filter) &&
            (search.isBlank() || it.title.contains(search, true) || it.body.contains(search, true))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (tab == BizCreateTab.Moment) "Choose a Moment" else "Choose a Memory",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (tab == BizCreateTab.Moment) Border else Card)
                        .clickable(onClick = onBack)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_biz_create_x),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (showRetailTagline) {
                Text(
                    text = "Built for shops and small teams in India.",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (!showRetailTagline) {
            TabSwitcher(tab = tab, onSelect = { tab = it })
        }

        if (tab == BizCreateTab.Moment || showRetailTagline) {
            MomentBody(
                categories = categories,
                onSelect = onSelectSetup,
                hideAiSuggest = showRetailTagline,
            )
        } else {
            MemoryBody(
                search = search,
                onSearch = { search = it },
                filter = filter,
                filters = filters,
                onFilter = { filter = it },
                memories = filtered,
            )
        }
    }
}

@Composable
private fun TabSwitcher(tab: BizCreateTab, onSelect: (BizCreateTab) -> Unit) {
    val pill = tab == BizCreateTab.Moment
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(if (pill) 100.dp else 12.dp))
            .border(1.dp, Border, RoundedCornerShape(if (pill) 100.dp else 12.dp))
            .background(if (pill) Color(0xFF111520) else Card)
            .padding(4.dp),
    ) {
        listOf(BizCreateTab.Moment to "Create a Moment", BizCreateTab.Memory to "Choose a Memory").forEach { (value, label) ->
            val selected = tab == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (pill) 100.dp else 8.dp))
                    .background(if (selected) Accent else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = when {
                        selected && pill -> Bg
                        selected -> Color.White
                        else -> Muted
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun MomentBody(
    categories: List<BizCategory>,
    onSelect: (BusinessSetupKind) -> Unit,
    hideAiSuggest: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Border, RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Accent.copy(alpha = 0.145f), Card.copy(alpha = 0f), Card),
                    ),
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Run every part of your business with clarity.",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 31.sp,
            )
            Text(
                "Create moments to track operations, revenue, strategy, and growth — all in one place.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(24.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Accent))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Accent.copy(alpha = 0.35f)))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Choose the part of the business you want to manage.",
                color = Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            categories.forEach { CategoryCard(it, onSelect = onSelect) }
        }

        if (!hideAiSuggest) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Not sure where to start? ", color = Muted, fontSize = 14.sp)
                Text(
                    "Let AI suggest →",
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    style = TextStyle(textDecoration = TextDecoration.Underline),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(item: BizCategory, onSelect: (BusinessSetupKind) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .then(if (item.comingSoon) Modifier.alpha(0.6f) else Modifier)
            .then(
                if (!item.comingSoon && item.setupKind != null) {
                    val kind = item.setupKind
                    Modifier
                        .then(item.testTag?.let { Modifier.testTag(it) } ?: Modifier)
                        .clickable { onSelect(kind) }
                } else Modifier
            ),
    ) {
        Image(
            painter = painterResource(item.card),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Card, Card.copy(alpha = 0.93f), Card.copy(alpha = 0.4f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Accent.copy(alpha = 0.13f))
                        .border(1.dp, Accent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(item.icon), null, Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(item.body, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_biz_create_chevron), null, Modifier.size(16.dp))
            }
        }
        if (item.comingSoon) {
            Text(
                text = "Coming Soon",
                color = Color(0xFF98A3B8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x994D4D59))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MemoryBody(
    search: String,
    onSearch: (String) -> Unit,
    filter: String,
    filters: List<String>,
    onFilter: (String) -> Unit,
    memories: List<BizMemory>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .background(Card)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(painterResource(R.drawable.ic_biz_memory_search), null, Modifier.size(18.dp))
            BasicTextField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text("Search memories...", color = Dim, fontSize = 14.sp)
                    }
                    inner()
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { chip ->
                val selected = filter == chip
                Text(
                    text = chip,
                    color = if (selected) Accent else Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (selected) Modifier else Modifier.background(Card))
                        .border(1.dp, if (selected) Accent else Border, RoundedCornerShape(20.dp))
                        .clickable { onFilter(chip) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Text("RECENT MEMORIES", color = Dim, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp)

        if (memories.isEmpty()) {
            Text(
                "No memories yet",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Card)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            )
        } else {
            memories.forEach { MemoryCard(it) }
        }
    }
}

@Composable
private fun MemoryCard(item: BizMemory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(item.dot))
            Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(item.body, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.tag.uppercase(),
                    color = Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Border)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(item.date, color = Dim, fontSize = 12.sp)
            }
            Image(painterResource(R.drawable.ic_biz_memory_bookmark), null, Modifier.size(16.dp))
        }
    }
}
