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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupPollDetailDto
import com.example.momentra.data.api.GroupPollOptionDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.wedding.create.PrimaryCta
import com.example.momentra.ui.shell.group.wedding.create.PurpleAccent
import com.example.momentra.ui.shell.group.wedding.create.SheetHeader
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollDetailSheet(
    pollId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GroupActiveTheme.Bg,
    ) {
        PollDetailBody(
            pollId = pollId,
            onDismiss = onDismiss,
            onSaved = onSaved,
            repository = repository,
        )
    }
}

@Composable
fun PollDetailBody(
    pollId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    var poll by remember { mutableStateOf<GroupPollDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = poll == null
            error = null
            repository.getPoll(pollId).fold(
                onSuccess = { poll = it; loading = false },
                onFailure = { e -> error = e.message; loading = false },
            )
        }
    }

    LaunchedEffect(pollId) { reload() }

    val isOpen = poll?.status?.equals("OPEN", ignoreCase = true) == true
    val totalVotes = poll?.options?.sumOf { it.voteCount ?: 0 } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        error?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (loading && poll == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GroupActiveTheme.Brand)
            }
        } else if (poll != null) {
            SheetHeader(
                iconRes = com.example.momentra.R.drawable.ic_qa_activity,
                title = poll?.question ?: "Poll",
                subtitle = poll?.status?.replaceFirstChar { c -> c.uppercase() },
                accent = PurpleAccent,
            )
            poll?.options.orEmpty().forEach { option ->
                PollOptionRow(
                    option = option,
                    totalVotes = totalVotes,
                    enabled = isOpen && !submitting,
                    onClick = {
                        val optionId = option.pollOptionId ?: return@PollOptionRow
                        submitting = true
                        scope.launch {
                            repository.votePoll(pollId, optionId).fold(
                                onSuccess = {
                                    submitting = false
                                    reload()
                                    onSaved()
                                },
                                onFailure = { e ->
                                    submitting = false
                                    error = e.message
                                },
                            )
                        }
                    },
                )
            }
            if (isOpen) {
                PrimaryCta(
                    label = "Close poll",
                    enabled = !submitting,
                    accent = PurpleAccent,
                    loading = submitting,
                    lightLabel = true,
                    onClick = {
                        submitting = true
                        scope.launch {
                            repository.closePoll(pollId).fold(
                                onSuccess = {
                                    submitting = false
                                    reload()
                                    onSaved()
                                },
                                onFailure = { e ->
                                    submitting = false
                                    error = e.message
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PollOptionRow(
    option: GroupPollOptionDto,
    totalVotes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val count = option.voteCount ?: 0
    val pct = if (totalVotes > 0) count.toFloat() / totalVotes.toFloat() else 0f
    val voted = option.votedByMe == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (voted) PurpleAccent.soft else Color(0xFF181716))
            .border(
                1.dp,
                if (voted) PurpleAccent.accent.copy(alpha = 0.5f) else GroupActiveTheme.Border,
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                option.text ?: "Option",
                color = GroupActiveTheme.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$count",
                color = GroupActiveTheme.Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF2A2826)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (voted) PurpleAccent.accent else PurpleAccent.accent.copy(alpha = 0.55f),
                    ),
            )
        }
    }
}
