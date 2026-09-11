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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupContributionItemDto
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val ContributionAvatarColors = listOf(
    Color(0xFFFDBA74), Color(0xFF86EFAC), Color(0xFFF9A8D4), Color(0xFF93C5FD),
)

fun contributionPaymentMethodLabel(code: String?): String = when (code?.trim()?.uppercase(Locale.US)) {
    "UPI" -> "UPI"
    "BANK_TRANSFER" -> "Bank Transfer"
    "CASH" -> "Cash"
    "CARD" -> "Card"
    else -> code?.takeIf { it.isNotBlank() } ?: "—"
}

private fun parseContributionInstant(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
}

fun contributionRelativeTime(iso: String?): String {
    val instant = parseContributionInstant(iso) ?: return ""
    val seconds = ChronoUnit.SECONDS.between(instant, Instant.now()).toInt()
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3600}h ago"
        seconds < 172_800 -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("d MMM", Locale.US)
            .format(instant.atZone(ZoneId.systemDefault()))
    }
}

fun contributionDayKey(iso: String?): LocalDate? {
    val instant = parseContributionInstant(iso) ?: return null
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}

fun contributionDayHeader(day: LocalDate, today: LocalDate = LocalDate.now()): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.US).format(day)
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts[1].first()}".uppercase(Locale.US)
    } else {
        name.take(2).uppercase(Locale.US)
    }
}

private fun amountLabel(amount: String?, currency: String?): String {
    val code = currency?.uppercase(Locale.US) ?: "INR"
    val symbol = when (code) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$code "
    }
    return "$symbol${amount ?: "0"}"
}

@Composable
fun MomentsContributionCard(
    item: GroupContributionItemDto,
    chrome: MomentsChrome,
    avatarIndex: Int = 0,
    onTap: (() -> Unit)? = null,
) {
    val name = item.displayName?.takeIf { it.isNotBlank() } ?: "Member"
    val pool = item.label?.takeIf { it.isNotBlank() } ?: "Pool"
    val time = contributionRelativeTime(item.contributedAt)
    val meta = if (time.isBlank()) pool else "$time · $pool"
    val status = if (item.status.equals("PENDING", true)) "PENDING" else "PAID"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .then(
                if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier,
            ),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(104.dp)
                    .background(chrome.accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ContributionAvatarColors[avatarIndex % ContributionAvatarColors.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            initials(name),
                            color = Color(0xFF14121B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name, color = chrome.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                        Text(meta, color = chrome.secondary, fontSize = 11.sp, fontFamily = PlusJakartaSans)
                    }
                    if (item.hasAttachment) {
                        AttachmentPaperclipIcon(tint = chrome.accent)
                    }
                    Text(
                        status,
                        color = chrome.text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(chrome.accent.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        amountLabel(item.amount, item.currencyCode),
                        color = chrome.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        contributionPaymentMethodLabel(item.paymentMethodCode),
                        color = chrome.secondary,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@Composable
fun MomentsContributionDetailsSection(
    items: List<GroupContributionItemDto>,
    chrome: MomentsChrome,
    momentId: String? = null,
    onViewAll: (() -> Unit)? = null,
    onEdit: ((GroupContributionItemDto) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MomentsSectionHeader(title = "Contribution Details", chrome = chrome, onViewAll = onViewAll)
        if (items.isEmpty()) {
            GroupEmptySection(
                message = "No contributions yet",
                detail = "Record a contribution from Quick Add — nothing is invented.",
            )
        } else {
            items.take(3).forEachIndexed { idx, item ->
                MomentsContributionCard(
                    item = item,
                    chrome = chrome,
                    avatarIndex = idx,
                    onTap = if (momentId != null && onEdit != null) {
                        { onEdit(item) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionsListSheet(
    items: List<GroupContributionItemDto>,
    visible: Boolean,
    onDismiss: () -> Unit,
    chrome: MomentsChrome,
    momentId: String? = null,
    onEdit: ((GroupContributionItemDto) -> Unit)? = null,
) {
    if (!visible) return
    val grouped = remember(items) {
        val today = LocalDate.now()
        val map = linkedMapOf<LocalDate, MutableList<GroupContributionItemDto>>()
        items.forEach { item ->
            val day = contributionDayKey(item.contributedAt) ?: today
            map.getOrPut(day) { mutableListOf() }.add(item)
        }
        map.entries.sortedByDescending { it.key }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = chrome.bg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Contributions",
                color = chrome.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            if (grouped.isEmpty()) {
                GroupEmptySection(
                    message = "No contributions yet",
                    detail = "Record a contribution from Quick Add — nothing is invented.",
                )
            } else {
                grouped.forEach { (day, dayItems) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            contributionDayHeader(day),
                            color = chrome.secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                        dayItems.forEachIndexed { idx, item ->
                            MomentsContributionCard(
                                item = item,
                                chrome = chrome,
                                avatarIndex = idx,
                                onTap = if (momentId != null && onEdit != null) {
                                    { onEdit(item) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
