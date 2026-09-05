package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupPollItemDto
import com.example.momentra.ui.theme.PlusJakartaSans
import java.util.Locale
import kotlin.math.roundToInt

private enum class PollsListFilter(val label: String) {
    All("All"),
    Active("Active"),
    Closed("Closed"),
}

private val pollAvatarColors = listOf(
    Color(0xFFFDBA74),
    Color(0xFF86EFAC),
    Color(0xFF93C5FD),
    Color(0xFFC4B5FD),
)

/** Figma 1604:16811 — Polls list (View all). Live APIs only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPollsListSheet(
    visible: Boolean,
    momentTitle: String?,
    chrome: MomentsChrome,
    polls: List<GroupPollItemDto>,
    onDismiss: () -> Unit,
    onChanged: () -> Unit = {},
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = chrome.bg,
    ) {
        GroupPollsListBody(
            momentTitle = momentTitle,
            chrome = chrome,
            polls = polls,
            onDismiss = onDismiss,
            onChanged = onChanged,
        )
    }
}

@Composable
fun GroupPollsListBody(
    momentTitle: String?,
    chrome: MomentsChrome,
    polls: List<GroupPollItemDto>,
    onDismiss: () -> Unit,
    onChanged: () -> Unit = {},
) {
    var filter by remember { mutableStateOf(PollsListFilter.All) }
    var selectedPollId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(polls, filter) {
        when (filter) {
            PollsListFilter.All -> polls
            PollsListFilter.Active -> polls.filter { it.status.equals("OPEN", ignoreCase = true) }
            PollsListFilter.Closed -> polls.filter {
                val s = it.status.orEmpty().uppercase(Locale.US)
                s == "CLOSED" || s == "CANCELLED"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .background(chrome.bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(chrome.card)
                    .border(1.dp, chrome.border, CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = chrome.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Polls",
                    color = chrome.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    momentTitle ?: "Shared moment",
                    color = chrome.secondary,
                    fontSize = 12.sp,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PollsListFilter.entries.forEach { tab ->
                val selected = filter == tab
                Text(
                    tab.label,
                    color = if (selected) chrome.darkText else chrome.secondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) chrome.accent else chrome.card)
                        .then(
                            if (selected) Modifier else Modifier.border(1.dp, chrome.border, RoundedCornerShape(999.dp)),
                        )
                        .clickable { filter = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (filtered.isEmpty()) {
                val (title, detail) = when (filter) {
                    PollsListFilter.All -> "No polls yet" to "Create a poll from Quick Add — nothing is invented."
                    PollsListFilter.Active -> "No active polls" to "Open polls will show here."
                    PollsListFilter.Closed -> "No closed polls" to "Closed polls will show here."
                }
                GroupEmptySection(title, detail)
            } else {
                filtered.forEachIndexed { index, poll ->
                    PollsListCard(
                        poll = poll,
                        index = index,
                        chrome = chrome,
                        onVote = { poll.pollId?.let { selectedPollId = it } },
                        onOpen = { poll.pollId?.let { selectedPollId = it } },
                    )
                }
            }
        }
    }

    selectedPollId?.let { pollId ->
        PollDetailSheet(
            pollId = pollId,
            visible = true,
            onDismiss = { selectedPollId = null },
            onSaved = {
                onChanged()
            },
        )
    }
}

@Composable
private fun PollsListCard(
    poll: GroupPollItemDto,
    index: Int,
    chrome: MomentsChrome,
    onVote: () -> Unit,
    onOpen: () -> Unit,
) {
    val isOpen = poll.status.equals("OPEN", ignoreCase = true)
    val options = poll.options
    val total = maxOf(poll.totalVotes ?: options.sumOf { it.voteCount ?: 0 }, 0)
    val denom = maxOf(total, 1)
    val creator = poll.createdByDisplayName ?: "Member"
    val endsTag = formatPollEndsTag(poll.closesAt, poll.status)
    val winningCount = options.maxOfOrNull { it.voteCount ?: 0 } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(chrome.card)
            .border(1.dp, chrome.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(pollAvatarColors[index % pollAvatarColors.size]),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initialsFromName(creator),
                        color = chrome.darkText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Text(
                    creator,
                    color = chrome.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                endsTag,
                color = if (isOpen) chrome.accent else chrome.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isOpen) chrome.brandSoft else chrome.card)
                    .then(
                        if (isOpen) Modifier else Modifier.border(1.dp, chrome.border, RoundedCornerShape(6.dp)),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Text(
            poll.question ?: "Poll",
            color = chrome.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val votes = option.voteCount ?: 0
                val pct = ((votes.toDouble() / denom) * 100).roundToInt()
                val isWinner = !isOpen && winningCount > 0 && votes == winningCount
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (isWinner) {
                                Text("✓", color = chrome.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                option.text ?: "Option",
                                color = chrome.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        Text(
                            "$votes votes ($pct%)",
                            color = chrome.secondary,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF252332)),
                    ) {
                        val fraction = if (total == 0) 0f else (votes.toFloat() / denom).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceAtLeast(if (total == 0) 0f else 0.02f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(chrome.accent),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (total == 1) "1 total vote" else "$total total votes",
                color = chrome.secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            if (isOpen) {
                Text(
                    "Vote",
                    color = chrome.darkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chrome.accent)
                        .clickable(onClick = onVote)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}
