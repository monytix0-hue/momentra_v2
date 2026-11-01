package com.example.momentra.ui.shell.business.appstate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isCompletedStatus
import com.example.momentra.ui.theme.PlusJakartaSans
import java.util.Locale

/** Lifecycle tab of the business moments directory. */
enum class BusinessDirectoryTab { ONGOING, COMPLETED }

/**
 * The five states the Figma `MomentSwitcher` covers (node 2678:37791).
 */
enum class BusinessDirectoryState {
    Ongoing,
    Completed,
    Empty,
    SearchEmpty,
    LoadError,
}

/** Accent bucket used to group business moments. Presentation only — classification is unchanged. */
internal data class BusinessDirectoryBucket(
    val id: String,
    val label: String,
    val accent: BusinessAppStateAccent,
)

internal fun businessDirectoryBucketFor(momentTypeCode: String?): BusinessDirectoryBucket {
    val code = momentTypeCode?.uppercase(Locale.US).orEmpty()
    return when {
        code.contains("MARKETING") || code.contains("CAMPAIGN") ->
            BusinessDirectoryBucket("marketing", "Marketing", BusinessAppStateAccent.Marketing)
        code.contains("TRADING") || code.contains("WHOLESALE") ->
            BusinessDirectoryBucket("trading", "Trading", BusinessAppStateAccent.Trading)
        code.contains("DISTRIBUTION") || code.contains("DEALERSHIP") ->
            BusinessDirectoryBucket("distribution", "Distribution", BusinessAppStateAccent.Distribution)
        code.contains("RETAIL") || code.contains("STORE") ->
            BusinessDirectoryBucket("retail", "Retail", BusinessAppStateAccent.Retail)
        code.contains("RESTAURANT") || code.contains("FOOD") ->
            BusinessDirectoryBucket("food", "Food", BusinessAppStateAccent.Rst)
        code.contains("CONSUMER") || code.contains("SERVICE") ->
            BusinessDirectoryBucket("service", "Service", BusinessAppStateAccent.Services)
        code.contains("MANUFACTURING") || code.contains("PRODUCTION") || code.contains("MADE_TO_ORDER") ->
            BusinessDirectoryBucket("manufacturing", "Manufacturing", BusinessAppStateAccent.Mfg)
        else -> BusinessDirectoryBucket("trading", "Business", BusinessAppStateAccent.Trading)
    }
}

/**
 * Business Active Moments directory, rebuilt against Figma `MomentSwitcher` (node 2678:37791).
 *
 * Distinct from the older gap-flow `BusinessMomentsDirectory` in `gap/BusinessGapScreens.kt`,
 * which serves a different entry point and is left untouched.
 */
@Composable
fun BusinessActiveMomentsDirectory(
    moments: List<MomentSummary>,
    selectedMomentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelectMoment: (String) -> Unit,
    onSelectCompletedMoment: (MomentSummary) -> Unit = {},
    onOpenStory: (String) -> Unit = {},
    onCreateMoment: () -> Unit,
    initialCompletedTab: Boolean = false,
    /** Completed moments loaded by the caller; null means not yet loaded. */
    completedMoments: List<MomentSummary>? = null,
    completedLoading: Boolean = false,
    completedError: String? = null,
    onRetryCompleted: () -> Unit = {},
    companyName: String = "Your business",
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 8 },
        ),
        exit = fadeOut(tween(180)) + slideOutVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            targetOffsetY = { it / 10 },
        ),
        modifier = modifier,
    ) {
        BusinessActiveMomentsDirectoryBody(
            moments = moments,
            selectedMomentId = selectedMomentId,
            onDismiss = onDismiss,
            onSelectMoment = onSelectMoment,
            onSelectCompletedMoment = onSelectCompletedMoment,
            onOpenStory = onOpenStory,
            onCreateMoment = onCreateMoment,
            initialCompletedTab = initialCompletedTab,
            completedMoments = completedMoments,
            completedLoading = completedLoading,
            completedError = completedError,
            onRetryCompleted = onRetryCompleted,
            companyName = companyName,
        )
    }
}

@Composable
private fun BusinessActiveMomentsDirectoryBody(
    moments: List<MomentSummary>,
    selectedMomentId: String?,
    onDismiss: () -> Unit,
    onSelectMoment: (String) -> Unit,
    onSelectCompletedMoment: (MomentSummary) -> Unit,
    onOpenStory: (String) -> Unit,
    onCreateMoment: () -> Unit,
    initialCompletedTab: Boolean,
    completedMoments: List<MomentSummary>?,
    completedLoading: Boolean,
    completedError: String?,
    onRetryCompleted: () -> Unit,
    companyName: String,
) {
    var tab by remember { mutableStateOf(if (initialCompletedTab) BusinessDirectoryTab.COMPLETED else BusinessDirectoryTab.ONGOING) }
    var query by remember { mutableStateOf("") }
    var bucketFilter by remember { mutableStateOf<String?>(null) }

    val activeMoments = remember(moments) { moments.filter { !it.isCompletedStatus() } }
    val source = if (tab == BusinessDirectoryTab.COMPLETED) completedMoments.orEmpty() else activeMoments

    val buckets = remember(source) {
        source.map { businessDirectoryBucketFor(it.momentTypeCode) }
            .distinctBy { it.id }
    }

    val filtered = remember(source, query, bucketFilter, tab) {
        source.filter { moment ->
            val matchesQuery = query.isBlank() ||
                moment.title.contains(query, ignoreCase = true)
            val matchesBucket = bucketFilter == null ||
                businessDirectoryBucketFor(moment.momentTypeCode).id == bucketFilter
            matchesQuery && matchesBucket
        }
    }

    val state = when {
        completedError != null && tab == BusinessDirectoryTab.COMPLETED -> BusinessDirectoryState.LoadError
        completedLoading && tab == BusinessDirectoryTab.COMPLETED && completedMoments.isNullOrEmpty() ->
            BusinessDirectoryState.Completed
        source.isEmpty() -> BusinessDirectoryState.Empty
        filtered.isEmpty() -> BusinessDirectoryState.SearchEmpty
        tab == BusinessDirectoryTab.COMPLETED -> BusinessDirectoryState.Completed
        else -> BusinessDirectoryState.Ongoing
    }

    val primaryAccent = buckets.firstOrNull()?.accent ?: BusinessAppStateAccent.Trading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BusinessAppStateTokens.Page),
    ) {
        // Ambient wash — Figma draws four large blurred ellipses behind the content.
        DirectoryWash(primaryAccent.color, Modifier.align(Alignment.TopCenter).padding(top = 0.dp))
        DirectoryWash(
            BusinessAppStateTokens.Warning.copy(alpha = 0.18f),
            Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
        ) {
            DirectoryHeader(
                companyName = companyName,
                subtitle = when (tab) {
                    BusinessDirectoryTab.ONGOING -> "${activeMoments.size} active moment${if (activeMoments.size == 1) "" else "s"}"
                    BusinessDirectoryTab.COMPLETED -> "${completedMoments.orEmpty().size} completed moments"
                },
                accent = primaryAccent.color,
                onDismiss = onDismiss,
            )

            DirectoryTabs(
                tab = tab,
                accent = primaryAccent.color,
                onSelect = {
                    tab = it
                    bucketFilter = null
                    query = ""
                },
            )

            DirectoryMetrics(
                activeCount = activeMoments.size,
                categoryCount = buckets.size,
                accent = primaryAccent.color,
            )

            DirectorySearch(
                query = query,
                onQueryChange = { query = it },
            )

            if (buckets.isNotEmpty()) {
                DirectoryChips(
                    buckets = buckets,
                    selected = bucketFilter,
                    onSelect = { bucketFilter = it },
                )
            }

            when (state) {
                BusinessDirectoryState.Empty -> DirectoryEmptyCard(
                    accent = primaryAccent,
                    tab = tab,
                    onCreateMoment = onCreateMoment,
                )
                BusinessDirectoryState.SearchEmpty -> DirectorySearchEmptyCard(
                    accent = primaryAccent,
                    query = query,
                    tab = tab,
                    onClear = {
                        query = ""
                        bucketFilter = null
                    },
                )
                BusinessDirectoryState.LoadError -> DirectoryLoadErrorCard(
                    accent = primaryAccent,
                    error = completedError,
                    onRetry = onRetryCompleted,
                )
                BusinessDirectoryState.Ongoing,
                BusinessDirectoryState.Completed,
                -> {
                    filtered.groupBy { businessDirectoryBucketFor(it.momentTypeCode) }
                        .forEach { (bucket, items) ->
                            Text(
                                text = "${bucket.label.uppercase(Locale.US)} · ${items.size}",
                                color = bucket.accent.color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                fontFamily = PlusJakartaSans,
                            )
                            items.forEach { moment ->
                                DirectoryMomentCard(
                                    moment = moment,
                                    bucket = bucket,
                                    selected = moment.momentId == selectedMomentId,
                                    onClick = {
                                        if (moment.isCompletedStatus()) {
                                            onSelectCompletedMoment(moment)
                                        } else {
                                            onSelectMoment(moment.momentId)
                                        }
                                    },
                                    onOpenStory = { onOpenStory(moment.momentId) },
                                )
                            }
                        }

                    if (tab == BusinessDirectoryTab.ONGOING) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "That's everything live",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PlusJakartaSans,
                            )
                            Text(
                                text = "Spin up another business moment when you're ready.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                        AppStatePrimaryButton(label = "Create another moment", onClick = onCreateMoment)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DirectoryWash(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(360.dp)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.16f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun DirectoryHeader(
    companyName: String,
    subtitle: String,
    accent: Color,
    onDismiss: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF14121B))
                .border(2.dp, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "M",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = companyName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                text = subtitle,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            )
        }
        AppStateIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = "Close",
            onClick = onDismiss,
        )
    }
}

@Composable
private fun DirectoryTabs(
    tab: BusinessDirectoryTab,
    accent: Color,
    onSelect: (BusinessDirectoryTab) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
            .padding(5.dp),
    ) {
        BusinessDirectoryTab.entries.forEach { entry ->
            val selected = entry == tab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
                    .background(
                        if (selected) {
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.55f)),
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        },
                    )
                    .border(
                        1.dp,
                        if (selected) accent else Color.Transparent,
                        RoundedCornerShape(BusinessAppStateTokens.ButtonRadius),
                    )
                    .clickable(role = Role.Button) { onSelect(entry) },
            ) {
                Text(
                    text = when (entry) {
                        BusinessDirectoryTab.ONGOING -> "Ongoing"
                        BusinessDirectoryTab.COMPLETED -> "Completed"
                    },
                    color = if (selected) Color(0xFF0E0E0E) else Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun DirectoryMetrics(activeCount: Int, categoryCount: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        DirectoryMetric(
            value = activeCount.toString(),
            label = "Active now",
            accent = accent,
            strokeAlpha = 1f,
            modifier = Modifier.weight(1f),
        )
        DirectoryMetric(
            value = categoryCount.toString(),
            label = "Categories",
            accent = BusinessAppStateTokens.Warning,
            strokeAlpha = 0.28f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DirectoryMetric(
    value: String,
    label: String,
    accent: Color,
    strokeAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(BusinessAppStateTokens.CardRadius))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = strokeAlpha), RoundedCornerShape(BusinessAppStateTokens.CardRadius))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun DirectorySearch(query: String, onQueryChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BusinessAppStateTokens.CardRadius))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(BusinessAppStateTokens.CardRadius))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Find a moment",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = PlusJakartaSans,
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DirectoryChips(
    buckets: List<BusinessDirectoryBucket>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        DirectoryChip(
            label = "All",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        buckets.forEach { bucket ->
            DirectoryChip(
                label = bucket.label,
                selected = selected == bucket.id,
                onClick = { onSelect(bucket.id) },
            )
        }
    }
}

@Composable
private fun DirectoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BusinessAppStateTokens.ButtonRadius))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(BusinessAppStateTokens.CtaStart, BusinessAppStateTokens.CtaEnd),
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
                },
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else Color.White.copy(alpha = 0.22f),
                RoundedCornerShape(BusinessAppStateTokens.ButtonRadius),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun DirectoryMomentCard(
    moment: MomentSummary,
    bucket: BusinessDirectoryBucket,
    selected: Boolean,
    onClick: () -> Unit,
    onOpenStory: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF312E81),
                        Color(0xFF0E7C66),
                        bucket.accent.color,
                    ),
                ),
            )
            .border(
                1.dp,
                bucket.accent.color.copy(alpha = if (selected) 1f else 0.6f),
                RoundedCornerShape(28.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(19.dp),
    ) {
        Text(
            text = moment.title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = buildString {
                append("${moment.participantCount.coerceAtLeast(1)} member")
                if (moment.participantCount != 1) append("s")
                append(" · ")
                append(bucket.label)
            },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = if (moment.isCompletedStatus()) "Tap to relive this moment" else "Tap Story to relive this moment",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.clickable(role = Role.Button, onClick = onOpenStory),
        )
    }
}

@Composable
private fun DirectoryEmptyCard(
    accent: BusinessAppStateAccent,
    tab: BusinessDirectoryTab,
    onCreateMoment: () -> Unit,
) {
    DirectoryStateCard(accent = accent) {
        AppStateMarkRing(accent = accent.color, icon = AppStateIcon.Moments.imageVector, size = 136.dp)
        AppStateTitle(
            if (tab == BusinessDirectoryTab.COMPLETED) "No completed moments" else "No live business moments",
        )
        AppStateBodyText(
            if (tab == BusinessDirectoryTab.COMPLETED) {
                "Wrapped moments and their Stories appear here once a moment is completed."
            } else {
                "Start a moment for your business and its Story will build from verified activity."
            },
        )
        AppStatePrimaryButton(
            label = if (tab == BusinessDirectoryTab.COMPLETED) "Start a moment" else "Create first moment",
            onClick = onCreateMoment,
        )
    }
}

@Composable
private fun DirectorySearchEmptyCard(
    accent: BusinessAppStateAccent,
    query: String,
    tab: BusinessDirectoryTab,
    onClear: () -> Unit,
) {
    DirectoryStateCard(accent = accent) {
        AppStateMarkRing(accent = accent.color, icon = Icons.Outlined.Search, size = 124.dp)
        AppStateTitle("No moments match")
        AppStateBodyText(
            "Nothing in ${if (tab == BusinessDirectoryTab.COMPLETED) "Completed" else "Ongoing"} matches " +
                "\u201C$query\u201D. Try another word or clear filters.",
        )
        AppStateSecondaryButton(label = "Clear search", onClick = onClear)
    }
}

@Composable
private fun DirectoryLoadErrorCard(
    accent: BusinessAppStateAccent,
    error: String?,
    onRetry: () -> Unit,
) {
    DirectoryStateCard(accent = accent, error = true) {
        AppStateMarkRing(accent = BusinessAppStateTokens.ErrorRing, icon = AppStateIcon.Alert.imageVector, size = 136.dp)
        AppStateTitle("Couldn't load moments")
        AppStateBodyText(
            "Your moments are safe — we just couldn't reach them. Check your connection and try again.",
        )
        AppStateErrorCodePill(code = "MOMENTS_FETCH_FAILED", accent = BusinessAppStateTokens.ErrorRing)
        AppStatePrimaryButton(label = "Retry", onClick = onRetry)
    }
}

@Composable
private fun DirectoryStateCard(
    accent: BusinessAppStateAccent,
    error: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BusinessAppStateTokens.BodyGap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (error) BusinessAppStateTokens.ErrorRing.copy(alpha = 0.5f) else accent.color.copy(alpha = 0.35f),
                RoundedCornerShape(32.dp),
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        content = content,
    )
}
