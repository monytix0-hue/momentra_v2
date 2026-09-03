package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CompanyMemberDto
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.PlusJakartaSans

private val Bg = Color(0xFF14121B)
private val Card = Color(0xFF201E28)
private val TextPrimary = Color(0xFFE5E0EE)
private val TextSecondary = Color(0xFFC9C4D8)
private val Accent = Color(0xFF818CF8)
private val Red = Color(0xFFF87171)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessMembersSheet(
    companyId: String,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var members by remember { mutableStateOf<List<CompanyMemberDto>>(emptyList()) }
    var showInvite by remember { mutableStateOf(false) }

    LaunchedEffect(companyId, visible) {
        if (!visible) return@LaunchedEffect
        loading = true
        error = null
        runCatching {
            ApiClient.apiService.listCompanyMembers(companyId).data.members
        }.fold(
            onSuccess = {
                members = it
                loading = false
            },
            onFailure = {
                error = it.message ?: "Could not load members"
                loading = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Bg,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "People",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "Invite",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clickable { showInvite = true }
                        .padding(4.dp),
                )
            }
            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
                error != null -> {
                    Text(error!!, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
                }
                members.isEmpty() -> {
                    Text(
                        "No members on this company yet.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                }
                else -> {
                    members.forEach { m ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Card)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                m.displayName ?: m.userId.take(8),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                "${m.membershipType} · ${m.status}",
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

    com.example.momentra.ui.shell.empty.business.CompanyInviteShareSheet(
        companyId = companyId,
        visible = showInvite,
        onDismiss = { showInvite = false },
    )
}
