package com.example.momentra.ui.shell.momentswitcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.data.repository.MeRepository
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isCompletedStatus
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The shared Group + Business moment switcher, built against Figma `2678:37791`.
 *
 * States (`2678:37481` Ongoing, `2678:37561` Completed, `2678:37634` Empty,
 * `2678:37715` SearchEmpty, `2678:37790` LoadError) are resolved by
 * [resolveMomentSwitcherState]; everything that differs between Group and Business arrives through
 * [MomentSwitcherSpec].
 *
 * Keep in lockstep with `MomentSwitcherDirectoryView.swift` on iOS.
 *
 * Two intentional corrections to the Figma source are marked `Figma deviation` below.
 */
@Composable
fun MomentSwitcherDirectory(
    spec: MomentSwitcherSpec,
    moments: List<MomentSummary>,
    selectedMomentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelectMoment: (String) -> Unit,
    onSelectCompletedMoment: (MomentSummary) -> Unit,
    onOpenStory: (String) -> Unit,
    onCreateMoment: () -> Unit,
    initialCompletedTab: Boolean = false,
    /** Test/gallery seam: when non-null the caller owns Completed-tab loading. */
    completedMoments: List<MomentSummary>? = null,
    completedLoading: Boolean? = null,
    completedError: String? = null,
    /** Test/gallery seam: seeds the search field so SearchEmpty is reachable without typing. */
    initialQuery: String = "",
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
        MomentSwitcherDirectoryBody(
            spec = spec,
            moments = moments,
            selectedMomentId = selectedMomentId,
            onDismiss = onDismiss,
            onSelectMoment = onSelectMoment,
            onSelectCompletedMoment = onSelectCompletedMoment,
            onOpenStory = onOpenStory,
            onCreateMoment = onCreateMoment,
            initialCompletedTab = initialCompletedTab,
            completedMomentsOverride = completedMoments,
            completedLoadingOverride = completedLoading,
            completedErrorOverride = completedError,
            initialQuery = initialQuery,
        )
    }
}

@Composable
private fun MomentSwitcherDirectoryBody(
    spec: MomentSwitcherSpec,
    moments: List<MomentSummary>,
    selectedMomentId: String?,
    onDismiss: () -> Unit,
    onSelectMoment: (String) -> Unit,
    onSelectCompletedMoment: (MomentSummary) -> Unit,
    onOpenStory: (String) -> Unit,
    onCreateMoment: () -> Unit,
    initialCompletedTab: Boolean,
    completedMomentsOverride: List<MomentSummary>?,
    completedLoadingOverride: Boolean?,
    completedErrorOverride: String?,
    initialQuery: String,
) {
    val tokens = MomentSwitcherTokens
    val scope = rememberCoroutineScope()
    val repository = remember { MeRepository() }

    var tab by remember(initialCompletedTab) {
        mutableStateOf(
            if (initialCompletedTab) MomentSwitcherTab.COMPLETED else MomentSwitcherTab.ONGOING,
        )
    }
    var query by remember { mutableStateOf(initialQuery) }
    var bucketFilter by remember { mutableStateOf<String?>(null) }
    var fetchedCompleted by remember { mutableStateOf<List<MomentSummary>>(emptyList()) }
    var fetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    val active = remember(moments, selectedMomentId) {
        ongoingSwitcherMoments(moments, selectedMomentId)
    }

    // Real completed-moment loading. Pre-existing behaviour left this permanently empty and made
    // Retry a no-op; the spec now carries the fetch so Group and Business both get real history.
    LaunchedEffect(tab, reloadToken, completedMomentsOverride) {
        if (tab != MomentSwitcherTab.COMPLETED || completedMomentsOverride != null) return@LaunchedEffect
        fetching = true
        fetchError = null
        spec.loadCompleted(repository)
            .onSuccess { fetchedCompleted = completedSwitcherMoments(it) }
            .onFailure { fetchError = it.message ?: "Could not load completed moments" }
        fetching = false
    }

    val completed = completedMomentsOverride ?: fetchedCompleted
    val source = if (tab == MomentSwitcherTab.COMPLETED) completed else active
    val filtered = remember(source, query, bucketFilter, spec) {
        filterSwitcherMoments(source, query, bucketFilter, spec)
    }
    val presentBuckets = remember(source, spec) {
        source.map { spec.classify(it.momentTypeCode) }.distinctBy { it.id }
    }
    val state = resolveMomentSwitcherState(
        tab = tab,
        sourceCount = source.size,
        filteredCount = filtered.size,
        completedLoading = completedLoadingOverride ?: fetching,
        completedError = completedErrorOverride ?: fetchError,
    )
    val headline = momentSwitcherHeadline(
        count = if (tab == MomentSwitcherTab.COMPLETED) completed.size else active.size,
        tab = tab,
    )
    val hasData = state == MomentSwitcherState.Ongoing || state == MomentSwitcherState.Completed
    val chromeAlpha = when (state) {
        MomentSwitcherState.Empty -> tokens.MutedEmpty
        MomentSwitcherState.LoadError -> tokens.MutedError
        else -> 1f
    }
    val chromeEnabled = hasData || state == MomentSwitcherState.SearchEmpty

    fun resetFilters() {
        query = ""
        bucketFilter = null
    }

    fun retry() {
        if (completedMomentsOverride != null) return
        reloadToken += 1
    }

    fun openMoment(moment: MomentSummary) {
        if (moment.isCompletedStatus()) {
            onSelectCompletedMoment(moment)
        } else {
            onSelectMoment(moment.momentId)
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.Page)
            .testTag(spec.testTag),
    ) {
        MomentSwitcherWash()

        Column(
            verticalArrangement = Arrangement.spacedBy(tokens.BodyGap),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.ScreenPadding)
                .padding(top = 16.dp, bottom = 32.dp),
        ) {
            MomentSwitcherHeader(
                headline = if (state == MomentSwitcherState.LoadError) "Moments" else headline,
                subtitle = momentSwitcherSubtitle(spec, state),
                allowDismiss = spec.allowDismiss,
                onDismiss = onDismiss,
            )

            // Figma deviation: the source mutes tabs + search (55% in Empty, 45% in LoadError) while
            // leaving them hit-testable. We mute *and* disable interaction in those states.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .stateAlpha(chromeAlpha),
            ) {
                MomentSwitcherTabs(
                    tab = tab,
                    enabled = chromeEnabled,
                    onSelect = {
                        tab = it
                        resetFilters()
                    },
                )
                MomentSwitcherSearch(
                    query = query,
                    enabled = chromeEnabled,
                    onQueryChange = { query = it },
                    onClear = { resetFilters() },
                )
            }

            if (hasData) {
                MomentSwitcherMetrics(
                    activeCount = active.size,
                    categoryCount = presentBuckets.size,
                )
                if (presentBuckets.isNotEmpty()) {
                    MomentSwitcherChips(
                        buckets = presentBuckets,
                        selected = bucketFilter,
                        onSelect = { bucketFilter = it },
                    )
                }
            }

            when (state) {
                MomentSwitcherState.Ongoing, MomentSwitcherState.Completed -> {
                    filtered.groupBy { spec.classify(it.momentTypeCode).id }.forEach { (bucketId, items) ->
                        val bucket = presentBuckets.firstOrNull { it.id == bucketId }
                            ?: spec.classify(items.firstOrNull()?.momentTypeCode)
                        Text(
                            text = "${bucket.label.uppercase(Locale.US)} · ${items.size}",
                            color = tokens.Accent,
                            fontSize = tokens.EyebrowSize.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = tokens.EyebrowTracking.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        items.forEach { moment ->
                            MomentSwitcherLiveCard(
                                moment = moment,
                                meta = spec.memberLabel(moment, bucket),
                                completed = moment.isCompletedStatus(),
                                onClick = { openMoment(moment) },
                                onOpenStory = { onOpenStory(moment.momentId) },
                            )
                        }
                    }

                    if (tab == MomentSwitcherTab.ONGOING) {
                        MomentSwitcherFooter(
                            title = spec.footerTitle,
                            body = spec.footerBody,
                            cta = spec.footerCta,
                            onClick = onCreateMoment,
                        )
                    }
                }

                MomentSwitcherState.Empty -> MomentSwitcherStateCard(
                    glow = tokens.Warm,
                    ring = tokens.Warm,
                    badgeColor = tokens.Warm,
                    badgeGlyph = "+",
                    title = spec.emptyTitle,
                    body = spec.emptyBody,
                    primaryLabel = spec.emptyCta,
                    onPrimary = onCreateMoment,
                )

                MomentSwitcherState.SearchEmpty -> MomentSwitcherStateCard(
                    glow = tokens.Accent,
                    ring = tokens.Accent,
                    badgeColor = tokens.Accent,
                    badgeGlyph = null,
                    title = "No moments match",
                    body = "Nothing in " +
                        (if (tab == MomentSwitcherTab.COMPLETED) "Completed" else "Ongoing") +
                        " matches \u201C${query.trim()}\u201D. Try another word or clear filters.",
                    primaryLabel = "Clear search",
                    onPrimary = { resetFilters() },
                    heroSize = tokens.HeroSearchSize,
                    secondary = true,
                )

                MomentSwitcherState.LoadError -> MomentSwitcherStateCard(
                    glow = tokens.Error,
                    ring = tokens.Error,
                    badgeColor = tokens.Error,
                    badgeGlyph = "!",
                    title = spec.errorTitle,
                    body = spec.errorBody,
                    primaryLabel = "Retry",
                    onPrimary = { retry() },
                    errorCode = spec.errorCode,
                )

                MomentSwitcherState.CompletedEmpty -> MomentSwitcherStateCard(
                    glow = tokens.Accent,
                    ring = tokens.Accent,
                    badgeColor = tokens.Accent,
                    badgeGlyph = "✓",
                    title = spec.completedEmptyTitle,
                    body = spec.completedEmptyBody,
                    primaryLabel = spec.completedEmptyCta,
                    onPrimary = onCreateMoment,
                )

                MomentSwitcherState.Loading -> MomentSwitcherLoadingCard()
            }
        }
    }
}

/** The 360dp emerald wash, blurred 50 and pegged to (-120, -80). */
@Composable
private fun MomentSwitcherWash() {
    val tokens = MomentSwitcherTokens
    Box(
        modifier = Modifier
            .offset(x = tokens.WashOffsetX, y = tokens.WashOffsetY)
            .size(tokens.WashSize)
            .blur(tokens.WashBlur)
            .background(
                Brush.radialGradient(
                    listOf(tokens.Wash.copy(alpha = tokens.WashAlpha), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun MomentSwitcherHeader(
    headline: String,
    subtitle: String,
    allowDismiss: Boolean,
    onDismiss: () -> Unit,
) {
    val tokens = MomentSwitcherTokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(R.drawable.momentra_topbar_logo),
            contentDescription = null,
            modifier = Modifier
                .size(tokens.AvatarSize)
                .clip(CircleShape),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = headline,
                color = tokens.Text,
                fontSize = tokens.HeadlineSize.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = tokens.TextMuted,
                fontSize = tokens.SubtitleSize.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (allowDismiss) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(tokens.CloseSize)
                    .clip(CircleShape)
                    .background(tokens.IconButtonFill)
                    .border(1.dp, tokens.IconButtonStroke, CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = tokens.Text,
                    modifier = Modifier.size(tokens.CloseGlyphSize),
                )
            }
        }
    }
}

@Composable
private fun MomentSwitcherTabs(
    tab: MomentSwitcherTab,
    enabled: Boolean,
    onSelect: (MomentSwitcherTab) -> Unit,
) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.PillRadius)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.SheetFill)
            .border(1.dp, tokens.SheetStroke, shape)
            .padding(4.dp),
    ) {
        MomentSwitcherTab.entries.forEach { entry ->
            val selected = entry == tab
            val label = when (entry) {
                MomentSwitcherTab.ONGOING -> "Ongoing"
                MomentSwitcherTab.COMPLETED -> "Completed"
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .then(
                        if (selected) {
                            Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(tokens.TabStart, tokens.TabEnd),
                                    ),
                                )
                                .border(1.dp, tokens.Accent, shape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(enabled = enabled, role = Role.Tab) { onSelect(entry) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = label,
                    color = if (selected) tokens.Text else tokens.TabIdle,
                    fontSize = tokens.TabLabelSize.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun MomentSwitcherMetrics(activeCount: Int, categoryCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MomentSwitcherMetric(
            value = activeCount.toString(),
            label = "Active now",
            valueColor = MomentSwitcherTokens.Accent,
            stroke = MomentSwitcherTokens.Accent,
            modifier = Modifier.weight(1f),
        )
        MomentSwitcherMetric(
            value = categoryCount.toString(),
            label = "Categories",
            valueColor = MomentSwitcherTokens.Warm,
            stroke = MomentSwitcherTokens.WarmStroke,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MomentSwitcherMetric(
    value: String,
    label: String,
    valueColor: Color,
    stroke: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.MetricRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(shape)
            .background(tokens.GlassFill)
            .border(1.dp, stroke, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = tokens.MetricValueSize.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = label,
            color = tokens.TextMuted,
            fontSize = tokens.MetricLabelSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun MomentSwitcherSearch(
    query: String,
    enabled: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.SearchRadius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.GlassFill)
            .border(1.dp, tokens.GlassStroke, shape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = tokens.TextSubtle,
            modifier = Modifier.size(tokens.SearchIconSize),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Find a moment",
                    color = tokens.TextSubtle,
                    fontSize = tokens.SearchTextSize.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = tokens.Text,
                    fontSize = tokens.SearchTextSize.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                ),
                cursorBrush = SolidColor(tokens.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotBlank()) {
            // SearchEmpty frame `2678:37662`.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.PillRadius))
                    .background(tokens.ClearPillFill)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "✕ Clear",
                    color = tokens.Text,
                    fontSize = tokens.ClearPillSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        } else {
            // Figma deviation: the source draws the Filter pill with an emerald fill *and* an
            // emerald label/icon, which renders invisible. White label on emerald.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.PillRadius))
                    .background(tokens.Accent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(tokens.FilterIconSize),
                    )
                    Text(
                        text = "Filter",
                        color = Color.White,
                        fontSize = tokens.FilterLabelSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MomentSwitcherChips(
    buckets: List<MomentSwitcherBucket>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MomentSwitcherChip(
            label = "All",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        buckets.forEach { bucket ->
            MomentSwitcherChip(
                label = bucket.label,
                selected = selected == bucket.id,
                onClick = { onSelect(bucket.id) },
            )
        }
    }
}

@Composable
private fun MomentSwitcherChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.PillRadius)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(Brush.verticalGradient(tokens.EmberGradient))
                } else {
                    Modifier
                        .background(tokens.ChipFill)
                        .border(1.dp, tokens.ChipStroke, shape)
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = tokens.Text,
            fontSize = tokens.ChipLabelSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/**
 * Full-width live card (`2678:37433`): radius 28, violet→emerald fill, accent border, emerald
 * drop shadow, LIVE NOW chip, amber-ringed Story pill, 26sp title.
 */
@Composable
private fun MomentSwitcherLiveCard(
    moment: MomentSummary,
    meta: String,
    completed: Boolean,
    onClick: () -> Unit,
    onOpenStory: () -> Unit,
) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.LiveCardRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val spread = tokens.LiveCardShadowRadius.toPx()
                val drop = tokens.LiveCardShadowY.toPx()
                repeat(4) { i ->
                    val t = (i + 1) / 4f
                    drawRoundRect(
                        color = tokens.Accent.copy(alpha = 0.25f / (i + 1)),
                        topLeft = Offset(-spread * t, drop - spread * t),
                        size = Size(size.width + spread * 2 * t, size.height + spread * 2 * t),
                        cornerRadius = CornerRadius(size.height / 4f + spread * t),
                    )
                }
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    *tokens.LiveCardGradient
                        .zip(tokens.LiveCardStops)
                        .map { (color, stop) -> stop to color }
                        .toTypedArray(),
                ),
            )
            .border(1.dp, tokens.Accent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MomentSwitcherStatusChip(completed = completed)
            MomentSwitcherStoryPill(onClick = onOpenStory)
        }
        Text(
            text = moment.title,
            color = tokens.Text,
            fontSize = tokens.CardTitleSize.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = meta,
            color = tokens.TextOnCard,
            fontSize = tokens.CardMetaSize.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (completed) "Tap to relive this moment" else "Tap Story to relive this moment",
            color = tokens.TextOnCard,
            fontSize = tokens.CardMicroSize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun MomentSwitcherStatusChip(completed: Boolean) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.PillRadius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(shape)
            .background(tokens.LiveChipFill)
            .border(1.dp, tokens.LiveChipStroke, shape)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(tokens.LiveDotSize)
                .background(if (completed) tokens.Warm else tokens.LiveDot, CircleShape),
        )
        Text(
            text = if (completed) "WRAPPED" else "LIVE NOW",
            color = if (completed) tokens.Warm else tokens.LiveDot,
            fontSize = tokens.LiveChipSize.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = tokens.LiveChipTracking.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun MomentSwitcherStoryPill(onClick: () -> Unit) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.PillRadius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(shape)
            .background(tokens.StoryPillFill)
            .border(2.dp, tokens.Warm, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(tokens.StoryAvatarSize)
                .clip(CircleShape),
        ) {
            Image(
                painter = painterResource(R.drawable.momentra_topbar_logo),
                contentDescription = null,
                modifier = Modifier.size(tokens.StoryAvatarSize),
            )
        }
        Text(
            text = "Story",
            color = tokens.Text,
            fontSize = tokens.StoryLabelSize.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = "▶",
            color = tokens.Warm,
            fontSize = tokens.StoryGlyphSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun MomentSwitcherFooter(
    title: String,
    body: String,
    cta: String,
    onClick: () -> Unit,
) {
    val tokens = MomentSwitcherTokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        Text(
            text = title,
            color = tokens.Text,
            fontSize = tokens.FooterTitleSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = body,
            color = tokens.Text.copy(alpha = 0.6f),
            fontSize = tokens.FooterBodySize.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans,
        )
    }
    MomentSwitcherPrimaryButton(label = cta, onClick = onClick)
}

@Composable
private fun MomentSwitcherPrimaryButton(label: String, onClick: () -> Unit) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.PillRadius)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val spread = tokens.CtaShadowRadius.toPx()
                val drop = tokens.CtaShadowY.toPx()
                repeat(4) { i ->
                    val t = (i + 1) / 4f
                    drawRoundRect(
                        color = MomentSwitcherTokens.EmberGradient.first().copy(alpha = 0.33f / (i + 1)),
                        topLeft = Offset(-spread * t, drop - spread * t),
                        size = Size(size.width + spread * 2 * t, size.height + spread * 2 * t),
                        cornerRadius = CornerRadius(size.height / 2f + spread * t),
                    )
                }
            }
            .clip(shape)
            .background(Brush.verticalGradient(tokens.EmberGradient))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = tokens.Text,
            fontSize = tokens.CtaLabelSize.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = tokens.CtaTracking.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun MomentSwitcherStateCard(
    glow: Color,
    ring: Color,
    badgeColor: Color,
    badgeGlyph: String?,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    heroSize: Dp = MomentSwitcherTokens.HeroSize,
    errorCode: String? = null,
    secondary: Boolean = false,
) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.StateCardRadius)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.GlassFill)
            .border(
                1.dp,
                if (errorCode != null) tokens.ErrorStroke else tokens.GlassStroke,
                shape,
            )
            .padding(horizontal = 22.dp, vertical = 26.dp),
    ) {
        MomentSwitcherHeroMark(
            glow = glow,
            ring = ring,
            badgeColor = badgeColor,
            badgeGlyph = badgeGlyph,
            size = heroSize,
        )
        Text(
            text = title,
            color = tokens.Text,
            fontSize = tokens.StateTitleSize.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = body,
            color = tokens.TextMuted,
            fontSize = tokens.StateBodySize.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = (tokens.StateBodySize * tokens.StateBodyLineHeight).sp,
            textAlign = TextAlign.Center,
            fontFamily = PlusJakartaSans,
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorCode != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.PillRadius))
                    .background(tokens.ErrorPillFill)
                    .border(1.dp, tokens.ErrorPillStroke, RoundedCornerShape(tokens.PillRadius))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = errorCode,
                    color = tokens.Error,
                    fontSize = tokens.ErrorCodeSize.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = tokens.ErrorCodeTracking.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        if (secondary) {
            MomentSwitcherSecondaryButton(label = primaryLabel, onClick = onPrimary)
        } else {
            MomentSwitcherPrimaryButton(label = primaryLabel, onClick = onPrimary)
        }
    }
}

/** Secondary CTA: no fill, white 22% border, radius 999 (SearchEmpty `2678:37682`). */
@Composable
private fun MomentSwitcherSecondaryButton(label: String, onClick: () -> Unit) {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.PillRadius)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.GlassFill)
            .border(1.dp, MomentSwitcherTokens.ChipStroke, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(
            text = label,
            color = tokens.Text,
            fontSize = tokens.CtaLabelSize.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/**
 * Hero mark: 136dp (124 in SearchEmpty) with an amber/emerald/red halo, a 3dp ring, the 96dp brand
 * mark and a 40dp badge. Figma `hero · mark ring` in all four state cards.
 */
@Composable
private fun MomentSwitcherHeroMark(
    glow: Color,
    ring: Color,
    badgeColor: Color,
    badgeGlyph: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val tokens = MomentSwitcherTokens
    val search = size == tokens.HeroSearchSize
    val ringSize = if (search) tokens.HeroSearchRingSize else tokens.HeroRingSize
    val markSize = if (search) tokens.HeroSearchMarkSize else tokens.HeroMarkSize
    val badgeInset = if (search) tokens.HeroSearchBadgeInset else tokens.HeroBadgeInset

    Box(modifier = modifier.size(size)) {
        // Halo.
        Box(
            modifier = Modifier
                .size(size)
                .blur(18.dp)
                .background(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.45f), Color.Transparent),
                    ),
                ),
        )
        // Ring.
        Box(
            modifier = Modifier
                .offset(x = tokens.HeroInset, y = tokens.HeroInset)
                .size(ringSize)
                .border(3.dp, ring.copy(alpha = 0.85f), CircleShape),
        )
        // Brand mark.
        Image(
            painter = painterResource(R.drawable.momentra_topbar_logo),
            contentDescription = null,
            modifier = Modifier
                .offset(x = tokens.HeroMarkInset, y = tokens.HeroMarkInset)
                .size(markSize)
                .clip(CircleShape),
        )
        // Badge.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = badgeInset, y = badgeInset)
                .size(tokens.HeroBadgeSize)
                .clip(CircleShape)
                .background(badgeColor)
                .border(tokens.HeroBadgeBorder, MomentSwitcherTokens.Page, CircleShape),
        ) {
            if (badgeGlyph != null) {
                Text(
                    text = badgeGlyph,
                    color = Color.White,
                    fontSize = tokens.HeroBadgeGlyphSize.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlusJakartaSans,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun MomentSwitcherLoadingCard() {
    val tokens = MomentSwitcherTokens
    val shape = RoundedCornerShape(tokens.StateCardRadius)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.GlassFill)
            .border(1.dp, tokens.GlassStroke, shape)
            .padding(horizontal = 22.dp, vertical = 32.dp),
    ) {
        CircularProgressIndicator(
            color = tokens.Accent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = "Loading moments…",
            color = tokens.TextMuted,
            fontSize = tokens.StateBodySize.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
    }
}

/** `Modifier.alpha` guard so full-opacity states don't allocate an extra graphics layer. */
private fun Modifier.stateAlpha(value: Float): Modifier =
    if (value >= 1f) this else this.alpha(value)
