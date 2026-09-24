package com.example.momentra.ui.shell.group.shared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.momentra.data.api.GroupMemoryItemDto
import com.example.momentra.data.api.GroupMemoryMediaDto
import com.example.momentra.ui.shell.components.MomentraModalBottomSheet
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Maps Quick Add memory chips → `memory.memory.memory_type` enum. */
fun groupMemoryTypeCode(chipLabel: String): String =
    when (chipLabel.trim().lowercase(Locale.US)) {
        "milestone" -> "MILESTONE"
        "lesson" -> "LEARNING"
        "reflection" -> "EXPERIENCE"
        else -> "GENERAL"
    }

data class MemoryGalleryPhoto(val url: String, val title: String?)

fun memoryGalleryPhotos(items: List<GroupMemoryItemDto>): List<MemoryGalleryPhoto> =
    items.flatMap { item ->
        val title = item.title?.trim()?.takeIf { it.isNotEmpty() }
        item.media.mapNotNull { media ->
            media.downloadUrl?.takeIf { it.isNotBlank() }?.let { MemoryGalleryPhoto(it, title) }
        }
    }

fun memoryGalleryUrls(items: List<GroupMemoryItemDto>): List<String> =
    memoryGalleryPhotos(items).map { it.url }

fun GroupMemoryItemDto.primaryDownloadUrl(): String? =
    media.firstOrNull { !it.downloadUrl.isNullOrBlank() }?.downloadUrl

@Composable
fun RemoteMemoryImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholderColor: Color = Color(0xFF322E40),
    contentScale: ContentScale = ContentScale.Crop,
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
                contentScale = contentScale,
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
fun MemoryPhotoFullscreenDialog(
    urls: List<String>,
    titles: List<String?> = emptyList(),
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
) {
    if (urls.isEmpty()) return
    val start = initialIndex.coerceIn(0, urls.lastIndex)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        val pagerState = rememberPagerState(initialPage = start, pageCount = { urls.size })
        val caption = titles.getOrNull(if (urls.size == 1) 0 else pagerState.currentPage)
            ?.takeIf { it.isNotBlank() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (urls.size == 1) {
                ZoomableMemoryPhoto(url = urls[0], modifier = Modifier.fillMaxSize())
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ZoomableMemoryPhoto(url = urls[page], modifier = Modifier.fillMaxSize())
                }
            }
            caption?.let { label ->
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
            Text(
                "✕",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onDismiss)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun ZoomableMemoryPhoto(
    url: String,
    modifier: Modifier = Modifier,
) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 4f)
                    scale = next
                    offset = if (next > 1.05f) offset + pan else Offset.Zero
                }
            }
            .pointerInput(url) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        RemoteMemoryImage(
            url = url,
            contentScale = ContentScale.Fit,
            placeholderColor = Color.Black,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
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
    data class Tile(val url: String?, val count: Int, val title: String?)
    val tiles = remember(items, showMediaCountBadge) {
        if (showMediaCountBadge) {
            items.mapNotNull { item ->
                val count = if (item.mediaCount > 0) item.mediaCount else item.media.size
                val url = item.media.firstOrNull { !it.downloadUrl.isNullOrBlank() }?.downloadUrl
                val title = item.title?.trim()?.takeIf { it.isNotEmpty() }
                when {
                    !url.isNullOrBlank() -> Tile(url, maxOf(count, 1), title)
                    count > 0 -> Tile(null, count, title)
                    else -> null
                }
            }
        } else {
            memoryGalleryPhotos(items).map { Tile(it.url, 0, it.title) }
        }
    }
    val openable = remember(tiles) { tiles.filter { !it.url.isNullOrBlank() } }
    val openableUrls = remember(openable) { openable.mapNotNull { it.url } }
    val openableTitles = remember(openable) { openable.map { it.title } }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

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
        tiles.forEachIndexed { index, tile ->
            Box(
                modifier = Modifier
                    .size(width = if (showMediaCountBadge) 110.dp else tileSize, height = if (showMediaCountBadge) 140.dp else tileSize)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0x40E88A4F), RoundedCornerShape(16.dp))
                    .background(field)
                    .then(
                        if (!tile.url.isNullOrBlank()) {
                            Modifier.clickable {
                                val start = openableUrls.indexOf(tile.url).takeIf { it >= 0 } ?: index.coerceAtMost(openableUrls.lastIndex)
                                if (openableUrls.isNotEmpty()) viewerIndex = start
                            }
                        } else {
                            Modifier
                        },
                    ),
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
                tile.title?.takeIf { it.isNotBlank() }?.let { label ->
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    viewerIndex?.let { start ->
        MemoryPhotoFullscreenDialog(
            urls = openableUrls,
            titles = openableTitles,
            initialIndex = start,
            onDismiss = { viewerIndex = null },
        )
    }
}

@Composable
fun MemoryMediaThumb(
    url: String?,
    modifier: Modifier = Modifier,
    border: Color = Color(0xFF322E40),
    field: Color = Color(0xFF252230),
) {
    var showViewer by remember { mutableStateOf(false) }
    RemoteMemoryImage(
        url = url,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .background(field)
            .then(
                if (!url.isNullOrBlank()) {
                    Modifier.clickable { showViewer = true }
                } else {
                    Modifier
                },
            ),
    )
    if (showViewer && !url.isNullOrBlank()) {
        MemoryPhotoFullscreenDialog(
            urls = listOf(url),
            onDismiss = { showViewer = false },
        )
    }
}

@Composable
fun MemoryMediaThumb(
    media: GroupMemoryMediaDto?,
    modifier: Modifier = Modifier,
    border: Color = Color(0xFF322E40),
    field: Color = Color(0xFF252230),
) {
    MemoryMediaThumb(
        url = media?.downloadUrl,
        modifier = modifier,
        border = border,
        field = field,
    )
}

/** Moments Shared Gallery "View all" — grid of every downloadable photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryGalleryListSheet(
    items: List<GroupMemoryItemDto>,
    visible: Boolean,
    onDismiss: () -> Unit,
    chrome: MomentsChrome,
) {
    if (!visible) return
    val photos = remember(items) { memoryGalleryPhotos(items) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    MomentraModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = chrome.bg,
        skipPartiallyExpanded = true,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Shared Gallery",
                color = chrome.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            if (photos.isEmpty()) {
                GroupEmptySection("No photos yet", "Add a memory with a photo from Quick Add.")
            } else {
                val columns = 3
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.chunked(columns).forEachIndexed { rowIndex, rowPhotos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPhotos.forEachIndexed { colIndex, photo ->
                                val startIndex = rowIndex * columns + colIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, chrome.border, RoundedCornerShape(12.dp))
                                        .background(chrome.card)
                                        .clickable { viewerIndex = startIndex },
                                ) {
                                    RemoteMemoryImage(
                                        url = photo.url,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    photo.title?.takeIf { it.isNotBlank() }?.let { label ->
                                        Text(
                                            label,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = PlusJakartaSans,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                            repeat(columns - rowUrls.size) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    viewerIndex?.let { start ->
        MemoryPhotoFullscreenDialog(
            urls = photos.map { it.url },
            titles = photos.map { it.title },
            initialIndex = start,
            onDismiss = { viewerIndex = null },
        )
    }
}
