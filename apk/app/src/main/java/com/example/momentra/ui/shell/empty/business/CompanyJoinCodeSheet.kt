package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.MintCompanyInviteBody
import com.example.momentra.data.repository.MeRepository
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.empty.group.GroupJoinQrScanner
import com.example.momentra.ui.shell.empty.group.generateInviteQrBitmap
import com.example.momentra.ui.theme.PlusJakartaSans
import java.util.UUID
import kotlinx.coroutines.launch

private val SheetBg = Color(0xFF161B26)
private val FieldBg = Color(0xFF252230)
private val FieldBorder = Color(0xFF322E40)
private val Accent = Color(0xFF818CF8)
private val Red = Color(0xFFF87171)
private val CtaText = Color(0xFF14121B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyJoinCodeSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onJoined: (CompanySummary) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val parsed = CompanyJoinLink.parseTyped(code)

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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Join with company code",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Close",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
            }
            Text(
                "Enter an invite code or scan a company QR.",
                color = Color(0xFF9E9AA8),
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(FieldBg)
                    .border(1.dp, FieldBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = code,
                    onValueChange = { code = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    ),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (code.isEmpty()) {
                            Text("Invite code", color = Color(0xFF64748B), fontSize = 15.sp, fontFamily = PlusJakartaSans)
                        }
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C233D))
                        .clickable { showScanner = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_shell_qr),
                        contentDescription = "Scan company QR",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            error?.let {
                Text(it, color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Accent, Color(0xFF6366F1))))
                    .then(if (parsed == null || submitting) Modifier else Modifier)
                    .clickable(enabled = parsed != null && !submitting) {
                        val token = parsed ?: return@clickable
                        submitting = true
                        error = null
                        scope.launch {
                            runCatching {
                                ApiClient.apiService.redeemCompanyInvite(
                                    code = token,
                                    idempotencyKey = UUID.randomUUID().toString(),
                                ).data
                            }.fold(
                                onSuccess = { result ->
                                    val companies = MeRepository().listCompanies().getOrElse { emptyList() }
                                    val summary = companies.firstOrNull { it.companyId == result.companyId }
                                        ?: CompanySummary(result.companyId, "Company")
                                    submitting = false
                                    onJoined(summary)
                                },
                                onFailure = {
                                    submitting = false
                                    error = it.message ?: "Could not join. Check the code and try again."
                                },
                            )
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (submitting) "Joining…" else "Join company",
                    color = CtaText.copy(alpha = if (parsed == null) 0.5f else 1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }

    if (showScanner) {
        GroupJoinQrScanner(
            onCode = {
                showScanner = false
                error = "That looks like a group invite. Use a company QR (momentra://c/…)."
            },
            onCompanyCode = { scanned ->
                showScanner = false
                code = scanned
            },
            onDismiss = { showScanner = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyInviteShareSheet(
    companyId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var invitePath by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(companyId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        error = null
        runCatching {
            ApiClient.apiService.mintCompanyInvite(
                idempotencyKey = UUID.randomUUID().toString(),
                body = MintCompanyInviteBody(companyId = companyId),
            ).data
        }.fold(
            onSuccess = {
                inviteCode = it.inviteCode
                invitePath = it.invitePath.ifBlank { CompanyJoinLink.displayPath(it.inviteCode) }
                loading = false
            },
            onFailure = {
                error = it.message ?: "Could not mint invite"
                loading = false
            },
        )
    }

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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Invite to company",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.fillMaxWidth(),
            )
            when {
                loading -> Text("Minting invite…", color = Color(0xFF9E9AA8), fontFamily = PlusJakartaSans)
                error != null -> Text(error!!, color = Red, fontFamily = PlusJakartaSans)
                else -> {
                    val code = inviteCode.orEmpty()
                    val qrSizePx = with(LocalDensity.current) { 160.dp.roundToPx() }
                    val qrBitmap = remember(code, qrSizePx) {
                        code.takeIf { it.isNotBlank() }?.let {
                            generateInviteQrBitmap(CompanyJoinLink.qrPayload(it), qrSizePx)
                        }
                    }
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Company invite QR",
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                        )
                    }
                    Text(
                        invitePath.orEmpty(),
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Code: $code",
                        color = Color(0xFF9E9AA8),
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Share this link or QR. New members join as MEMBER.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text(
                "Done",
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(onClick = onDismiss)
                    .padding(8.dp),
            )
        }
    }
}
