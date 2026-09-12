package com.example.momentra.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.NotificationInboxItemDto
import com.example.momentra.data.local.PendingDeepLink
import com.example.momentra.data.repository.AccountRepository
import kotlinx.coroutines.launch

/**
 * In-app notification inbox — shows group activity even when OS push is denied
 * or undelivered. Backed by `GET /v1/me/notifications`.
 */
@Composable
fun NotificationInboxSheet(
    onOpenMoment: (String) -> Unit,
    onClose: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit = {},
    repository: AccountRepository = remember { AccountRepository() },
) {
    var notifications by remember { mutableStateOf<List<NotificationInboxItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        errorText = null
        repository.listNotifications(limit = 50)
            .onSuccess {
                notifications = it.items
                onUnreadCountChanged(it.unreadCount)
            }
            .onFailure { errorText = it.message ?: "Couldn't load notifications" }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("inbox.sheet"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Notifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            TextButton(
                onClick = {
                    scope.launch {
                        repository.markNotificationsRead(all = true)
                        load()
                    }
                },
                enabled = notifications.any { it.readAt == null },
                modifier = Modifier.testTag("inbox.mark_all_read"),
            ) {
                Text("Mark all read")
            }
        }

        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .testTag("inbox.loading"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            errorText != null -> EmptyState(
                title = "Couldn't load notifications",
                detail = errorText.orEmpty(),
                tag = "inbox.error",
            )

            notifications.isEmpty() -> EmptyState(
                title = "You're all caught up",
                detail = "Group updates, polls, bookings, and expenses will show up here.",
                tag = "inbox.empty",
            )

            else -> {
                val grouped = notifications.groupBy { it.threadKey ?: it.momentId ?: "other" }
                val keys = grouped.keys.sortedByDescending { k ->
                    grouped[k]?.firstOrNull()?.createdAt.orEmpty()
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inbox.list"),
                ) {
                    keys.forEach { key ->
                        val sectionItems = grouped[key].orEmpty()
                        item(key = "hdr-$key") {
                            Text(
                                text = sectionItems.firstOrNull()?.momentTitle ?: "Updates",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }
                        items(sectionItems, key = { it.notificationId }) { item ->
                            InboxRow(item) {
                                scope.launch {
                                    repository.markNotificationsRead(listOf(item.notificationId))
                                }
                                val momentId = item.deepLink
                                    ?.let { PendingDeepLink.parseMomentId(it) }
                                    ?: item.momentId
                                onClose()
                                if (!momentId.isNullOrBlank()) onOpenMoment(momentId)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxRow(item: NotificationInboxItemDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("inbox.item"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(if (item.readAt == null) Color(0xFFF43F5E) else Color.Transparent),
        )
        Column {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = item.body,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String, tag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(
            text = detail,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
