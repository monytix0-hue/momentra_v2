package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.wedding.create.WeddingActiveTheme
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

private val Bg = Color(0xFF14121B)
private val Card = Color(0xFF201E28)
private val TextPrimary = Color(0xFFE5E0EE)
private val TextSecondary = Color(0xFFC9C4D8)
private val Accent = Color(0xFF3B82F6)
private val Red = Color(0xFFF87171)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupParticipantsSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    isWedding: Boolean = false,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bg = if (isWedding) WeddingActiveTheme.Bg else Bg
    val accent = if (isWedding) WeddingActiveTheme.Accent else Accent
    val textPrimary = if (isWedding) WeddingActiveTheme.Text else TextPrimary
    val card = if (isWedding) WeddingActiveTheme.Card else Card
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        error = null
        repository.getParticipants(momentId).fold(
            onSuccess = {
                participants = it.participants
                loading = false
            },
            onFailure = {
                error = it.message ?: "Could not load participants"
                loading = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 24.dp)
                .testTag(MaestroIds.QA_TILE_PEOPLE),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (isWedding) "Add Participant" else "People",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                error != null -> {
                    Text(error!!, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
                participants.isEmpty() -> {
                    Text(
                        "No participants on this moment yet.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                else -> {
                    participants.forEach { p ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(card)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                p.displayName ?: p.participantId.take(8),
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                "${p.roleCode} · ${p.status}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }
        }
    }
}
