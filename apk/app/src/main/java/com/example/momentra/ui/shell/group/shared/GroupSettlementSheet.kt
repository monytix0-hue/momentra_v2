package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.example.momentra.domain.AppContext
import com.example.momentra.data.api.GroupParticipantDto
import com.example.momentra.data.repository.GroupSliceRepository
import com.example.momentra.ui.shell.empty.group.GeBg
import com.example.momentra.ui.shell.empty.group.GeBorder
import com.example.momentra.ui.shell.empty.group.GeCard
import com.example.momentra.ui.shell.empty.group.GeSecondary
import com.example.momentra.ui.shell.empty.group.GeText
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.shell.MomentThemes
import kotlinx.coroutines.launch
import java.math.BigDecimal
import com.example.momentra.ui.shell.group.wedding.create.FieldLabel
import com.example.momentra.ui.shell.group.wedding.create.SheetField

private val Red = Color(0xFFF87171)

/**
 * Shared Group settlement sheet for all G01–G12 subtypes (QH-W).
 * Payer = debtor settling; payee = creditor receiving.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupSettlementSheet(
    momentId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    momentTypeCode: String? = null,
    repository: GroupSliceRepository = remember { GroupSliceRepository() },
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MomentThemes.resolve(
        AppContext.GROUP,
        momentTypeCode,
    ).primary
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var participants by remember { mutableStateOf<List<GroupParticipantDto>>(emptyList()) }
    var payerId by remember { mutableStateOf<String?>(null) }
    var payeeId by remember { mutableStateOf<String?>(null) }
    /** LOCAL_ONLY — how settlement happened; not sent (no schema column). */
    var paymentMethod by remember { mutableStateOf("EXTERNAL") }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(momentId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        error = null
        repository.getParticipants(momentId).fold(
            onSuccess = { dto ->
                val active = dto.participants.filter {
                    it.status.equals("ACTIVE", ignoreCase = true) ||
                        it.status.equals("INVITED", ignoreCase = true)
                }.ifEmpty { dto.participants }
                participants = active
                payerId = active.getOrNull(1)?.participantId ?: active.firstOrNull()?.participantId
                payeeId = active.firstOrNull()?.participantId
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GeBg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 24.dp)
                .testTag(MaestroIds.QA_TILE_SETTLE + ".sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Settle",
                color = GeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                "Record a ledger settlement from the debtor (payer) to the creditor (payee). Payment rails (UPI/bank) are not processed — method is local-only.",
                color = GeSecondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            if (loading) {
                CircularProgressIndicator(color = accent, modifier = Modifier.padding(8.dp))
            } else {
                FieldLabel("Payer (settling debt)")
                ParticipantChips(
                    participants = participants,
                    selectedId = payerId,
                    onSelect = { payerId = it },
                    accent = accent,
                )
                FieldLabel("Payee (receiving)")
                ParticipantChips(
                    participants = participants,
                    selectedId = payeeId,
                    onSelect = { payeeId = it },
                    accent = accent,
                )
                FieldLabel("Amount")
                SheetField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                )
                FieldLabel("Currency")
                SheetField(
                    value = currency,
                    onValueChange = { currency = it.uppercase().take(3) },
                    placeholder = "INR",
                )
                FieldLabel("How paid (local only — not sent)")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "UPI" to "UPI",
                        "BANK" to "Bank Transfer",
                        "EXTERNAL" to "Mark as Paid Externally",
                    ).forEach { (code, label) ->
                        val selected = paymentMethod == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) accent else GeCard)
                                .border(1.dp, if (selected) accent else GeBorder, RoundedCornerShape(999.dp))
                                .clickable { paymentMethod = code }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White else GeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                }
            }
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
            val canSubmit = !submitting && !loading &&
                payerId != null && payeeId != null && payerId != payeeId &&
                amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canSubmit) accent else GeCard)
                    .clickable(enabled = canSubmit) {
                        val payer = payerId ?: return@clickable
                        val payee = payeeId ?: return@clickable
                        if (payer == payee) {
                            error = "Payer and payee must differ"
                            return@clickable
                        }
                        scope.launch {
                            submitting = true
                            error = null
                            repository.createSettlement(
                                momentId = momentId,
                                payerParticipantId = payer,
                                payeeParticipantId = payee,
                                amount = amount,
                                currencyCode = currency,
                            ).fold(
                                onSuccess = {
                                    submitting = false
                                    onSaved()
                                    onDismiss()
                                },
                                onFailure = {
                                    submitting = false
                                    error = it.message
                                },
                            )
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (submitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(4.dp))
                } else {
                    Text(
                        "Record settlement",
                        color = if (canSubmit) Color.White else GeSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantChips(
    participants: List<GroupParticipantDto>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        participants.forEach { p ->
            val selected = p.participantId == selectedId
            val label = p.displayName?.takeIf { it.isNotBlank() } ?: p.roleCode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) accent.copy(alpha = 0.25f) else GeCard)
                    .border(1.dp, if (selected) accent else GeBorder, RoundedCornerShape(20.dp))
                    .clickable { onSelect(p.participantId) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(label, color = GeText, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = GeSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GeCard)
            .border(1.dp, GeBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = GeSecondary.copy(alpha = 0.6f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = GeText,
                fontSize = 14.sp,
                fontFamily = PlusJakartaSans,
            ),
            cursorBrush = SolidColor(Color(0xFF10B981)),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
