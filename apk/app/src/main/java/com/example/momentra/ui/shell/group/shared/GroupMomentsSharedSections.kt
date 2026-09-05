package com.example.momentra.ui.shell.group.shared

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupExpenseListItemDto
import com.example.momentra.data.api.GroupFinancePayloadDto
import com.example.momentra.data.api.GroupLifeBookingDto
import com.example.momentra.data.api.GroupLifePlanningItemDto
import com.example.momentra.data.api.GroupLifeUpdateDto
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.ui.shell.group.shared.formatBookingDay
import com.example.momentra.ui.shell.group.shared.formatBookingDayTime
import com.example.momentra.ui.shell.group.experience.create.ExperienceActiveTheme
import com.example.momentra.ui.shell.group.living.create.LivingActiveTheme
import com.example.momentra.ui.shell.group.purchase.create.PurchaseActiveTheme
import com.example.momentra.ui.shell.group.wedding.create.WeddingActiveTheme
import com.example.momentra.ui.theme.PlusJakartaSans
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Theme tokens for Moments shared sections (Trip / Wedding / Experience / Purchase / Living). */
data class MomentsChrome(
    val bg: Color,
    val text: Color,
    val secondary: Color,
    val card: Color,
    val border: Color,
    val accent: Color,
    val accentAlt: Color,
    val darkText: Color,
    val brandSoft: Color,
) {
    companion object {
        val Trip = MomentsChrome(
            bg = GroupActiveTheme.Bg,
            text = GroupActiveTheme.Text,
            secondary = GroupActiveTheme.Secondary,
            card = GroupActiveTheme.Card,
            border = GroupActiveTheme.Border,
            accent = GroupActiveTheme.Brand,
            accentAlt = GroupActiveTheme.AccentOrange,
            darkText = Color(0xFF14121B),
            brandSoft = GroupActiveTheme.Brand.copy(alpha = 0.1f),
        )

        val Wedding = MomentsChrome(
            bg = WeddingActiveTheme.Bg,
            text = WeddingActiveTheme.Text,
            secondary = WeddingActiveTheme.Secondary,
            card = WeddingActiveTheme.Card,
            border = WeddingActiveTheme.Border,
            accent = WeddingActiveTheme.Accent,
            accentAlt = WeddingActiveTheme.AccentLight,
            darkText = WeddingActiveTheme.DarkText,
            brandSoft = WeddingActiveTheme.AccentSoft,
        )

        fun experience(theme: ExperienceActiveTheme) = MomentsChrome(
            bg = theme.bg,
            text = theme.text,
            secondary = theme.secondary,
            card = theme.card,
            border = theme.border,
            accent = theme.accent,
            accentAlt = theme.accentLight,
            darkText = theme.darkText,
            brandSoft = theme.accentSoft,
        )

        fun purchase(theme: PurchaseActiveTheme) = MomentsChrome(
            bg = theme.bg,
            text = theme.text,
            secondary = theme.secondary,
            card = theme.card,
            border = theme.border,
            accent = theme.accent,
            accentAlt = theme.accentLight,
            darkText = theme.darkText,
            brandSoft = theme.accentSoft,
        )

        fun living(theme: LivingActiveTheme) = MomentsChrome(
            bg = theme.bg,
            text = theme.text,
            secondary = theme.secondary,
            card = theme.card,
            border = theme.border,
            accent = theme.accent,
            accentAlt = theme.accentLight,
            darkText = theme.darkText,
            brandSoft = theme.accentSoft,
        )
    }
}

private val momentsAvatarColors = listOf(
    Color(0xFFFDBA74), Color(0xFF86EFAC), Color(0xFF93C5FD), Color(0xFFC4B5FD),
)
private val momentsItineraryAccents = listOf(
    Color(0xFF14B8A6), Color(0xFFE88A4F), Color(0xFFA855F7),
)

@Composable
fun MomentsSectionHeader(
    title: String,
    chrome: MomentsChrome,
    onViewAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = chrome.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            "View all",
            color = if (onViewAll != null) chrome.accent else chrome.accent.copy(alpha = 0.45f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            modifier = if (onViewAll != null) Modifier.clickable(onClick = onViewAll) else Modifier,
        )
    }
}

@Composable
fun MomentsHeroHeader(
    eyebrow: String,
    title: String,
    status: String,
    stats: List<Triple<String, String, List<Color>>>,
    chrome: MomentsChrome,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    eyebrow,
                    color = chrome.secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    title,
                    color = chrome.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                status.uppercase(Locale.US),
                color = chrome.darkText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chrome.accent)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        stats.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value, colors) ->
                    MomentsStatCard(
                        label = label,
                        value = value,
                        colors = colors,
                        chrome = chrome,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MomentsStatCard(
    label: String,
    value: String,
    colors: List<Color>,
    chrome: MomentsChrome,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(colors.ifEmpty { listOf(chrome.accent, chrome.accentAlt) }))
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = chrome.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(value, color = chrome.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

@Composable
fun MomentsPollPreviewCard(
    poll: GroupPollItemDto,
    chrome: MomentsChrome,
    onClick: () -> Unit,
) {
    val options = poll.options.take(2)
    val total = maxOf(poll.totalVotes ?: options.sumOf { it.voteCount ?: 0 }, 1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    poll.question ?: "Poll",
                    color = chrome.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    formatPollClosesMeta(poll.closesAt, poll.totalVotes),
                    color = chrome.secondary,
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            Text(
                (poll.status ?: "OPEN").uppercase(Locale.US),
                color = chrome.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(chrome.brandSoft)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        options.forEachIndexed { index, option ->
            val votes = option.voteCount ?: 0
            val fraction = votes.toFloat() / total.toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        option.text ?: "Option",
                        color = chrome.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text("$votes votes", color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF252332)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (index == 0) chrome.accent else chrome.accentAlt),
                    )
                }
            }
        }
    }
}

@Composable
fun MomentsUpdateFeedRow(
    item: GroupLifeUpdateDto,
    index: Int,
    chrome: MomentsChrome,
) {
    val name = item.authorDisplayName ?: "Member"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(momentsAvatarColors[index % momentsAvatarColors.size]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initialsFromName(name),
                color = chrome.darkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                Text(formatRelativeShort(item.createdAt), color = chrome.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
            Text(item.message.orEmpty(), color = chrome.text, fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun MomentsItineraryDayCard(
    dayIndex: Int,
    day: LocalDate,
    title: String,
    timeLabel: String,
    chrome: MomentsChrome,
) {
    val accent = momentsItineraryAccents[(dayIndex - 1).coerceAtLeast(0) % momentsItineraryAccents.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(52.dp)
                .background(accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                formatItineraryDayLabel(dayIndex, day),
                color = chrome.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(title, color = chrome.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            Text(timeLabel, color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun MomentsExpensesCard(
    spent: String?,
    currency: String,
    peopleCount: Int,
    expenses: List<GroupExpenseListItemDto>,
    chrome: MomentsChrome,
) {
    val people = maxOf(peopleCount, 1)
    val spentDecimal = GroupFinanceFormat.parseAmount(spent)
    val perPerson = if (spentDecimal > BigDecimal.ZERO) {
        GroupFinanceFormat.formatMoney(
            spentDecimal.divide(BigDecimal(people), 2, RoundingMode.HALF_UP).toPlainString(),
            currency,
        )
    } else {
        "—"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MomentsExpenseSummaryTile("Total spent", GroupFinanceFormat.formatMoney(spent, currency), chrome, Modifier.weight(1f))
            MomentsExpenseSummaryTile("Per-person split", perPerson, chrome, Modifier.weight(1f))
        }
        if (expenses.isEmpty()) {
            GroupEmptySection("No expenses yet", "Add a group expense from Quick Add.")
        } else {
            Text(
                "RECENT EXPENSES",
                color = chrome.secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            expenses.take(3).forEach { expense ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            expense.description ?: "Expense",
                            color = chrome.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            (expense.categoryCode ?: "General").replace('_', ' ').replaceFirstChar { it.titlecase(Locale.US) },
                            color = chrome.secondary,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Text(
                        GroupFinanceFormat.formatMoney(expense.amount, expense.currencyCode ?: currency),
                        color = chrome.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        expense.paidByDisplayName ?: "—",
                        color = chrome.secondary,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentsExpenseSummaryTile(
    label: String,
    value: String,
    chrome: MomentsChrome,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = chrome.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        Text(value, color = chrome.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
    }
}

data class MomentsUpcomingEvent(
    val title: String,
    val detail: String,
    val badge: String?,
    val glyph: String,
)

fun buildMomentsUpcomingEvents(
    bookings: List<GroupLifeBookingDto>,
    planningItems: List<GroupLifePlanningItemDto>,
    finance: GroupFinancePayloadDto?,
    limit: Int = 3,
): List<MomentsUpcomingEvent> {
    val out = mutableListOf<MomentsUpcomingEvent>()
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val zone = ZoneId.systemDefault()

    for (booking in bookings.take(6)) {
        val startMillis = parseInstantMillis(booking.startAt ?: booking.bookedAt) ?: continue
        if (startMillis < now) continue
        val day = java.time.Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val badge = when (day) {
            tomorrow -> "TOMORROW"
            today -> "TODAY"
            else -> null
        }
        val type = (booking.bookingType ?: "Booking").replace('_', ' ').replaceFirstChar { it.titlecase(Locale.US) }
        out += MomentsUpcomingEvent(
            title = booking.title ?: "Booking",
            detail = "$type · ${formatBookingDay(booking.startAt ?: booking.bookedAt).orEmpty()}",
            badge = badge,
            glyph = "🏠",
        )
        if (out.size >= 2) break
    }
    if (out.size < 2) {
        for (plan in recentOpenPlanningItems(planningItems, limit = 8)) {
            val dueMillis = parseInstantMillis(plan.dueAt) ?: continue
            if (dueMillis < now) continue
            val day = java.time.Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalDate()
            val badge = when (day) {
                tomorrow -> "TOMORROW"
                today -> "TODAY"
                else -> null
            }
            out += MomentsUpcomingEvent(
                title = plan.title ?: "Plan",
                detail = "Plan · ${formatBookingDay(plan.dueAt).orEmpty()}",
                badge = badge,
                glyph = "📅",
            )
            if (out.size >= 2) break
        }
    }
    val currency = finance?.totals?.firstOrNull()?.currencyCode ?: "INR"
    val budget = GroupFinanceFormat.parseAmount(finance?.totals?.firstOrNull()?.budgetTotal)
    val spent = GroupFinanceFormat.parseAmount(finance?.totals?.firstOrNull()?.expenseTotal)
    val remaining = budget.subtract(spent)
    if (remaining > BigDecimal.ZERO && budget > BigDecimal.ZERO && out.size < limit) {
        out += MomentsUpcomingEvent(
            title = "Budget remaining",
            detail = GroupFinanceFormat.formatMoney(remaining.toPlainString(), currency),
            badge = null,
            glyph = "📅",
        )
    }
    return out.take(limit)
}

@Composable
fun MomentsUpcomingEventCard(
    event: MomentsUpcomingEvent,
    highlight: Boolean,
    chrome: MomentsChrome,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) Color(0x14E85940) else Color(0x10E88A4F))
            .border(1.dp, if (highlight) Color(0x4DE85940) else Color(0x33E88A4F), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(chrome.card),
            contentAlignment = Alignment.Center,
        ) { Text(event.glyph, fontSize = 18.sp) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.title, color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                event.badge?.let {
                    Text(
                        it,
                        color = chrome.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(chrome.brandSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(event.detail, color = chrome.secondary, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun MomentsQuickAddCta(
    chrome: MomentsChrome,
    onClick: () -> Unit,
    title: String = "Create the next shared moment",
    subtitle: String = "Add a plan, expense, memory, poll or update.",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(chrome.accent, chrome.accentAlt)))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ Open Quick Add", color = chrome.darkText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        }
    }
}

@Composable
fun MomentsSimpleRowCard(
    title: String,
    chrome: MomentsChrome,
    meta: String? = null,
    status: String? = null,
    statusColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
            if (!meta.isNullOrBlank()) {
                Text(meta, color = chrome.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            }
        }
        status?.let {
            val color = statusColor ?: chrome.accent
            Text(
                it.uppercase(Locale.US),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Figma 575:14327 — booking card in Moments / Experience tabs. */
@Composable
fun MomentsBookingCard(item: GroupLifeBookingDto, chrome: MomentsChrome) {
    val status = (item.status ?: "PLANNED").uppercase(Locale.US)
    val confirmed = status == "CONFIRMED" || status == "BOOKED" || status == "COMPLETED"
    val typeLabel = (item.bookingType ?: "Booking").replace('_', ' ').replaceFirstChar { it.titlecase(Locale.US) }
    val day = formatBookingDay(item.startAt ?: item.bookedAt)
    val meta = listOfNotNull(typeLabel, day).joinToString(" · ")
    val whenLabel = formatBookingDayTime(item.startAt) ?: formatBookingDay(item.bookedAt) ?: "—"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(chrome.card),
                    contentAlignment = Alignment.Center,
                ) { Text(if (confirmed) "🏨" else "🎟️", fontSize = 18.sp) }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title ?: item.bookingId.orEmpty(), color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                    Text(meta, color = chrome.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                }
            }
            Text(
                status,
                color = if (confirmed) Color(0xFF22C55E) else chrome.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background((if (confirmed) Color(0xFF22C55E) else chrome.accent).copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (confirmed) "Check-in" else "Start time", color = chrome.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
            Text(whenLabel, color = chrome.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        }
    }
}
