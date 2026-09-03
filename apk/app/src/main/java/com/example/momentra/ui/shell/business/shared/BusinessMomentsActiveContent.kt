package com.example.momentra.ui.shell.business.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.ui.theme.PlusJakartaSans

private val Red = Color(0xFFF87171)

/** Themed Business Moments — activity list from live getActivity. */
@Composable
fun BusinessMomentsActiveContent(
    momentId: String?,
    momentTitle: String?,
    momentTypeCode: String? = null,
    refreshToken: Long = 0L,
    onOpenQuickAdd: () -> Unit = {},
    repository: BusinessSliceRepository = remember { BusinessSliceRepository() },
    modifier: Modifier = Modifier,
) {
    val theme = BusinessActiveTheme.forTypeCode(momentTypeCode)
    var loading by remember { mutableStateOf(true) }
    var activities by remember { mutableStateOf<List<ActivityItemDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken, momentId) {
        if (momentId.isNullOrBlank()) {
            loading = false
            activities = emptyList()
            error = "Select a Business Moment."
            return@LaunchedEffect
        }
        loading = activities.isEmpty()
        error = null
        repository.getActivity(momentId).fold(
            onSuccess = { activities = it.items },
            onFailure = { error = it.message },
        )
        loading = false
    }

    if (loading && activities.isEmpty()) {
        Box(modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize().background(theme.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 72.dp),
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
            Text(
                theme.momentsTitle,
                color = theme.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
            if (activities.isEmpty()) {
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
                        "Nothing recorded yet",
                        color = theme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Text(
                        "Activity appears here after live writes. Empty stays empty.",
                        color = theme.secondary,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(theme.accent)
                            .clickable(onClick = onOpenQuickAdd)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            "Open Action Center",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            } else {
                activities.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.card)
                            .border(1.dp, theme.border, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            item.title.ifBlank { item.activityCode },
                            color = theme.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            item.occurredAt,
                            color = theme.secondary,
                            fontSize = 12.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(theme.accent)
                .clickable(onClick = onOpenQuickAdd),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
