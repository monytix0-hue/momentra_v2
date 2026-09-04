package com.example.momentra.ui.shell.group.shared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupMemoryMediaDto
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

fun memoryGalleryUrls(items: List<GroupMemoryItemDto>): List<String> =
    items.flatMap { item ->
        item.media.mapNotNull { m -> m.downloadUrl?.takeIf { it.isNotBlank() } }
    }

fun GroupMemoryItemDto.primaryDownloadUrl(): String? =
    media.firstOrNull { !it.downloadUrl.isNullOrBlank() }?.downloadUrl

@Composable
fun RemoteMemoryImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholderColor: Color = Color(0xFF322E40),
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        bitmap = null
        failed = false
        if (url.isNullOrBlank()) {
            failed = true
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 12_000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        if (bitmap == null) failed = true
    }

    Box(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            failed -> Text("📷", fontSize = 18.sp)
            else -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun MemoryPhotoGalleryStrip(
    items: List<GroupMemoryItemDto>,
    emptyMessage: String,
    emptyDetail: String,
    text: Color,
    muted: Color,
    field: Color,
    border: Color,
    tileSize: Dp = 96.dp,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    val urls = remember(items) { memoryGalleryUrls(items) }
    if (urls.isEmpty()) {
        if (emptyContent != null) {
            emptyContent()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(emptyMessage, color = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
                Text(emptyDetail, color = muted, fontSize = 12.sp, fontFamily = PlusJakartaSans)
            }
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        urls.forEach { url ->
            RemoteMemoryImage(
                url = url,
                modifier = Modifier
                    .size(tileSize)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, border, RoundedCornerShape(12.dp))
                    .background(field),
            )
        }
    }
}

@Composable
fun MemoryMediaThumb(
    media: GroupMemoryMediaDto?,
    modifier: Modifier = Modifier,
    border: Color = Color(0xFF322E40),
    field: Color = Color(0xFF252230),
) {
    RemoteMemoryImage(
        url = media?.downloadUrl,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .background(field),
    )
}
