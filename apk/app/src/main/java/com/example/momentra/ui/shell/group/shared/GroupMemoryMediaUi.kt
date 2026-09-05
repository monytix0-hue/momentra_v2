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
    showMediaCountBadge: Boolean = false,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    data class Tile(val url: String?, val count: Int)
    val tiles = remember(items, showMediaCountBadge) {
        if (showMediaCountBadge) {
            items.mapNotNull { item ->
                val count = if (item.mediaCount > 0) item.mediaCount else item.media.size
                val url = item.media.firstOrNull { !it.downloadUrl.isNullOrBlank() }?.downloadUrl
                when {
                    !url.isNullOrBlank() -> Tile(url, maxOf(count, 1))
                    count > 0 -> Tile(null, count)
                    else -> null
                }
            }
        } else {
            memoryGalleryUrls(items).map { Tile(it, 0) }
        }
    }
    if (tiles.isEmpty()) {
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tiles.forEach { tile ->
            Box(
                modifier = Modifier
                    .size(width = if (showMediaCountBadge) 110.dp else tileSize, height = if (showMediaCountBadge) 140.dp else tileSize)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0x40E88A4F), RoundedCornerShape(16.dp))
                    .background(field),
            ) {
                RemoteMemoryImage(
                    url = tile.url,
                    modifier = Modifier.fillMaxSize(),
                )
                if (showMediaCountBadge && tile.count > 0) {
                    Text(
                        "${tile.count}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
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
