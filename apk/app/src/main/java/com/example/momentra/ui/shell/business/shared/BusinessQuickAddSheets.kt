package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.SheetAccent
import com.example.momentra.ui.shell.group.wedding.create.SheetField
import com.example.momentra.ui.shell.group.wedding.create.WeddingMemorySheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingPollSheetBody
import com.example.momentra.ui.shell.group.wedding.create.WeddingUpdateSheetBody
import com.example.momentra.ui.theme.PlusJakartaSans

private val SheetBg = Color(0xFF161B26)
private val Handle = Color(0xFF475569)

private fun BusinessActiveTheme.toSheetAccent(): SheetAccent =
    SheetAccent(accent = accent, accentEnd = accentSolid, soft = accentSoft)

/** Business Quick Add sheets — theme accent; GAP kinds use disabled Coming soon CTA. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessGapQuickAddSheet(
    theme: BusinessActiveTheme,
    kind: BusinessQuickAddKind,
    visible: Boolean,
    momentId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {},
    onExpense: () -> Unit = {},
    onRevenue: () -> Unit = {},
    onInvoice: () -> Unit = {},
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return

    val accent = theme.toSheetAccent()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Handle),
            )
            when (kind) {
                BusinessQuickAddKind.EXPENSE, BusinessQuickAddKind.SPEND_ENTRY ->
                    RedirectLiveBody(
                        theme = theme,
                        kind = kind,
                        message = "Expense is available from the finance sheet.",
                        onContinue = {
                            onDismiss()
                            onExpense()
                        },
                    )
                BusinessQuickAddKind.REVENUE ->
                    RedirectLiveBody(
                        theme = theme,
                        kind = kind,
                        message = "Revenue is available from the finance sheet.",
                        onContinue = {
                            onDismiss()
                            onRevenue()
                        },
                    )
                BusinessQuickAddKind.INVOICE ->
                    RedirectLiveBody(
                        theme = theme,
                        kind = kind,
                        message = "Invoice is available from the finance sheet.",
                        onContinue = {
                            onDismiss()
                            onInvoice()
                        },
                    )
                BusinessQuickAddKind.POLL ->
                    WeddingPollSheetBody(momentId, repository, onDismiss, onSaved, accent)
                BusinessQuickAddKind.MEMORY ->
                    WeddingMemorySheetBody(momentId, repository, onDismiss, onSaved, accent)
                BusinessQuickAddKind.TEAM_UPDATE, BusinessQuickAddKind.GENERAL_UPDATE ->
                    WeddingUpdateSheetBody(momentId, repository, onDismiss, onSaved, accent)
                else ->
                    BusinessGapBody(theme = theme, kind = kind)
            }
        }
    }
}

@Composable
private fun RedirectLiveBody(
    theme: BusinessActiveTheme,
    kind: BusinessQuickAddKind,
    message: String,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            kind.label(),
            color = theme.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            message,
            color = theme.secondary,
            fontSize = 13.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent)
                .clickable(onClick = onContinue)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Continue",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun BusinessGapBody(
    theme: BusinessActiveTheme,
    kind: BusinessQuickAddKind,
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(kind.emoji(), fontSize = 28.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    kind.label(),
                    color = theme.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    kind.subtitle(),
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Title")
            SheetField(title, { title = it }, "Title", minHeight = 42)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Notes")
            SheetField(notes, { notes = it }, "Notes", singleLine = false, minHeight = 80)
        }
        Text(
            "API not wired for this Action Center command yet — Coming soon.",
            color = theme.muted,
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.accent.copy(alpha = 0.35f))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Coming soon",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}
