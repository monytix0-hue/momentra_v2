package com.example.momentra.ui.shell.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.momentra.data.api.BusinessMemoryPayloadDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val Red = Color(0xFFF87171)

/** Themed Business Memory — live getMemory only. */
@Composable
fun BusinessMemoryActiveContent(
    momentId: String?,
    momentTitle: String?,
    refreshToken: Long,
    momentTypeCode: String? = null,
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.forTypeCode(momentTypeCode)
    var loading by remember { mutableStateOf(true) }
    var payload by remember { mutableStateOf<BusinessMemoryPayloadDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            payload = null
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = payload == null
        error = null
        repository.getMemory(momentId).fold(
            onSuccess = { payload = it.payload },
            onFailure = { error = it.message },
        )
        loading = false
    }

    if (loading && payload == null) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    val items = payload?.items.orEmpty()
    val count = payload?.memoryCount ?: items.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        error?.let {
            Text(it, color = Red, fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
        if (!momentTitle.isNullOrBlank()) {
            Text(
                momentTitle,
                color = theme.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                theme.memoryTitle,
                color = theme.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Save",
                color = theme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.clickable(onClick = onOpenQuickAdd),
            )
        }

        if (items.isEmpty() || count == 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.card)
                    .border(1.dp, theme.border, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "No memories yet",
                    color = theme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    "Memories appear when the Memory write path saves live items. count: $count",
                    color = theme.secondary,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        } else {
            Text(
                "$count memories",
                color = theme.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = PlusJakartaSans,
            )
            items.forEach { item ->
                Text(
                    item["title"]?.toString() ?: "Memory",
                    color = theme.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.card)
                        .border(1.dp, theme.border, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                )
            }
        }
    }
}
