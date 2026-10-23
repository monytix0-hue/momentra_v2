package com.example.momentra.ui.shell.group.directory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.MeRepository
import com.example.momentra.domain.MomentSummary
import com.example.momentra.domain.isActiveStatus
import com.example.momentra.ui.shell.group.shared.GroupExperienceFamily
import com.example.momentra.ui.shell.group.shared.groupExperienceFamilyFor
import com.example.momentra.ui.shell.group.shared.isThemedLiving
import com.example.momentra.ui.shell.group.shared.isThemedPurchase
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ShellTokens
import java.util.Locale

enum class GroupMomentDirectoryBucket(val label: String) {
    CELEBRATIONS("Celebrations"),
    GATHERINGS("Gatherings"),
    TRIPS("Trips"),
    PLANS("Plans"),
}

fun groupMomentDirectoryBucket(momentTypeCode: String?): GroupMomentDirectoryBucket {
    val code = momentTypeCode?.uppercase(Locale.US).orEmpty()
    val family = groupExperienceFamilyFor(momentTypeCode)
    return when {
        code.contains("TRIP") || code.contains("TRAVEL") || code.contains("HIKE") ||
            code.contains("WEEKEND") || code.contains("WATER") -> GroupMomentDirectoryBucket.TRIPS
        family == GroupExperienceFamily.WEDDING ||
            family == GroupExperienceFamily.HOUSE_PARTY ||
            code.contains("BIRTHDAY") ||
            code.contains("CELEBRAT") -> GroupMomentDirectoryBucket.CELEBRATIONS
        family == GroupExperienceFamily.OFFICE_OUTING ||
            family.isThemedLiving() ||
            family.isThemedPurchase() -> GroupMomentDirectoryBucket.PLANS
        else -> GroupMomentDirectoryBucket.GATHERINGS
    }
}

private fun bucketPaletteList(bucket: GroupMomentDirectoryBucket): List<List<Color>> = when (bucket) {
        GroupMomentDirectoryBucket.CELEBRATIONS -> listOf(
            listOf(Color(0xFFE879A8), Color(0xFFC084FC)),
            listOf(Color(0xFF818CF8), Color(0xFFA78BFA)),
            listOf(Color(0xFFF472B6), Color(0xFFFB7185)),
        )
        GroupMomentDirectoryBucket.GATHERINGS -> listOf(
            listOf(Color(0xFFFB923C), Color(0xFFF97316)),
            listOf(Color(0xFFA78BFA), Color(0xFF6366F1)),
            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
            listOf(Color(0xFFFB7185), Color(0xFFF97316)),
        )
        GroupMomentDirectoryBucket.TRIPS -> listOf(
            listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)),
            listOf(Color(0xFF6366F1), Color(0xFF3B82F6)),
            listOf(Color(0xFF2DD4BF), Color(0xFF14B8A6)),
            listOf(Color(0xFF22D3EE), Color(0xFF06B6D4)),
        )
        GroupMomentDirectoryBucket.PLANS -> listOf(
            listOf(Color(0xFF4ADE80), Color(0xFF22C55E)),
            listOf(Color(0xFFA3E635), Color(0xFF84CC16)),
        )
    }

private fun bucketSingularLabel(bucket: GroupMomentDirectoryBucket): String = when (bucket) {
    GroupMomentDirectoryBucket.CELEBRATIONS -> "Celebration"
    GroupMomentDirectoryBucket.GATHERINGS -> "Gathering"
    GroupMomentDirectoryBucket.TRIPS -> "Trip"
    GroupMomentDirectoryBucket.PLANS -> "Plan"
}

enum class MomentDirectoryKind { GROUP, BUSINESS }

private data class DirectoryBucket(
    val id: String,
    val label: String,
    val singular: String,
    val palettes: List<List<Color>>,
)

private fun DirectoryBucket.gradientAt(index: Int): List<Color> = palettes[index % palettes.size]

private data class MomentDirectorySpec(
    val background: Color,
    val chrome: Color,
    val summaryFill: Color,
    val summaryValue: Color,
    val summaryLabel: Color,
    val tabSelectedFill: Color,
    val tabSelectedText: Color,
    val createGradient: List<Color>,
    val footerDetail: String,
    val emptyOngoing: String,
    val testTag: String,
    val buckets: List<DirectoryBucket>,
    val classify: (String?) -> DirectoryBucket,
    val loadCompleted: suspend (MeRepository) -> Result<List<MomentSummary>>,
)

private fun groupMomentDirectorySpec(): MomentDirectorySpec {
    val buckets = GroupMomentDirectoryBucket.entries.map { bucket ->
        DirectoryBucket(
            id = bucket.name,
            label = bucket.label,
            singular = bucketSingularLabel(bucket),
            palettes = bucketPaletteList(bucket),
        )
    }
    return MomentDirectorySpec(
        background = ShellTokens.SurfaceContent,
        chrome = Color(0xFF1C1A24),
        summaryFill = Color(0xFFE8E2D6),
        summaryValue = Color(0xFF1A1625),
        summaryLabel = Color(0xFF5C5668),
        tabSelectedFill = Color(0xFFE8E2D6),
        tabSelectedText = Color(0xFF1A1625),
        createGradient = listOf(Color(0xFFE8621A), Color(0xFFFDBA74)),
        footerDetail = "Spin up another shared experience when you're ready.",
        emptyOngoing = "Create a group moment to get started",
        testTag = "group.moment.directory",
        buckets = buckets,
        classify = { code -> buckets.first { it.id == groupMomentDirectoryBucket(code).name } },
        loadCompleted = { repo -> repo.listGroupMoments(limit = 50, lifecycle = "completed") },
    )
}

private fun businessMomentDirectorySpec(): MomentDirectorySpec {
    fun bucket(id: String, label: String, palettes: List<List<Color>>) =
        DirectoryBucket(id = id, label = label, singular = label, palettes = palettes)
    val buckets = listOf(
        bucket("marketing", "Marketing", listOf(
            listOf(Color(0xFF818CF8), Color(0xFF6366F1)),
            listOf(Color(0xFFA78BFA), Color(0xFF818CF8)),
        )),
        bucket("trading", "Trading", listOf(
            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
            listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
        )),
        bucket("retail", "Retail", listOf(
            listOf(Color(0xFF34D399), Color(0xFF10B981)),
            listOf(Color(0xFF6EE7B7), Color(0xFF34D399)),
        )),
        bucket("manufacturing", "Manufacturing", listOf(
            listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)),
            listOf(Color(0xFF22D3EE), Color(0xFF0284C7)),
        )),
        bucket("work", "Work", listOf(
            listOf(Color(0xFF94A3B8), Color(0xFF64748B)),
            listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)),
        )),
    )
    return MomentDirectorySpec(
        background = Color(0xFF0C0F15),
        chrome = Color(0xFF161B26),
        summaryFill = Color(0xFF161B26),
        summaryValue = Color(0xFFF1F5F9),
        summaryLabel = Color(0xFF94A3B8),
        tabSelectedFill = Color(0xFFE0E7FF),
        tabSelectedText = Color(0xFF1E1B4B),
        createGradient = listOf(Color(0xFF818CF8), Color(0xFFA5B4FC)),
        footerDetail = "Spin up another business moment when you're ready.",
        emptyOngoing = "Create a business moment to get started",
        testTag = "business.moment.directory",
        buckets = buckets,
        classify = { code ->
            val c = code?.uppercase(Locale.US).orEmpty()
            val id = when {
                c.contains("MARKETING") -> "marketing"
                c.contains("TRADING") || c.contains("DISTRIBUTION") -> "trading"
                c.contains("RETAIL") || c.contains("RESTAURANT") || c.contains("CONSUMER") || c.contains("FOOD") -> "retail"
                c.contains("MANUFACTURING") || c.contains("PRODUCTION") || c.contains("MADE_TO_ORDER") -> "manufacturing"
                else -> "work"
            }
            buckets.first { it.id == id }
        },
        loadCompleted = { repo -> repo.listBusinessMoments(limit = 50, lifecycle = "completed") },
    )
}

private fun momentDirectorySpec(kind: MomentDirectoryKind): MomentDirectorySpec = when (kind) {
    MomentDirectoryKind.GROUP -> groupMomentDirectorySpec()
    MomentDirectoryKind.BUSINESS -> businessMomentDirectorySpec()
}

/**
 * Figma Active Moments directory — Group moment selector (1–4 large cards vs 5+ horizontal grids).
 */
@Composable
fun GroupActiveMomentsDirectory(
    moments: List<MomentSummary>,
    selectedMomentId: String?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelectMoment: (String) -> Unit,
    onSelectCompletedMoment: (MomentSummary) -> Unit = {},
    onOpenStory: (String) -> Unit = {},
    onCreateMoment: () -> Unit,
    /** Open on Completed lifecycle tab (empty Group with only completed history). */
    initialCompletedTab: Boolean = false,
    /** Home mode has no Close — dismissing would return to the create-empty screen. */
    allowDismiss: Boolean = true,
    kind: MomentDirectoryKind = MomentDirectoryKind.GROUP,
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
        GroupActiveMomentsDirectoryBody(
            moments = moments,
            selectedMomentId = selectedMomentId,
            onDismiss = onDismiss,
            onSelectMoment = onSelectMoment,
            onSelectCompletedMoment = onSelectCompletedMoment,
            onOpenStory = onOpenStory,
            onCreateMoment = onCreateMoment,
            initialCompletedTab = initialCompletedTab,
            allowDismiss = allowDismiss,
            kind = kind,
        )
    }
}

private enum class DirectoryLifecycleTab { ONGOING, COMPLETED }

@Composable
private fun GroupActiveMomentsDirectoryBody(
    moments: List<MomentSummary>,
    selectedMomentId: String?,
    onDismiss: () -> Unit,
    onSelectMoment: (String) -> Unit,
    onSelectCompletedMoment: (MomentSummary) -> Unit,
    onOpenStory: (String) -> Unit,
    onCreateMoment: () -> Unit,
    initialCompletedTab: Boolean = false,
    allowDismiss: Boolean = true,
    kind: MomentDirectoryKind = MomentDirectoryKind.GROUP,
) {
    val spec = momentDirectorySpec(kind)
    val meRepository = remember { MeRepository() }
    val active = remember(moments) { moments.filter { it.isActiveStatus() } }
    var lifecycleTab by remember(initialCompletedTab) {
        mutableStateOf(
            if (initialCompletedTab) DirectoryLifecycleTab.COMPLETED else DirectoryLifecycleTab.ONGOING,
        )
    }
    var completedMoments by remember { mutableStateOf<List<MomentSummary>>(emptyList()) }
    var loadingCompleted by remember { mutableStateOf(false) }
    var completedError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedBucket by remember { mutableStateOf<DirectoryBucket?>(null) }
    val isCompletedTab = lifecycleTab == DirectoryLifecycleTab.COMPLETED

    LaunchedEffect(lifecycleTab) {
        if (lifecycleTab != DirectoryLifecycleTab.COMPLETED) return@LaunchedEffect
        loadingCompleted = true
        completedError = null
        spec.loadCompleted(meRepository)
            .onSuccess { completedMoments = it }
            .onFailure { completedError = it.message ?: "Could not load completed moments" }
        loadingCompleted = false
    }

    val source = if (isCompletedTab) completedMoments else active
    val filtered = remember(source, query, selectedBucket) {
        source.filter { m ->
            val matchesQuery = query.isBlank() ||
                m.title.contains(query.trim(), ignoreCase = true)
            val bucket = spec.classify(m.momentTypeCode)
            val matchesBucket = selectedBucket == null || bucket.id == selectedBucket?.id
            matchesQuery && matchesBucket
        }
    }
    val grouped = remember(filtered) {
        spec.buckets.mapNotNull { bucket ->
            val items = filtered.filter { spec.classify(it.momentTypeCode).id == bucket.id }
            if (items.isEmpty()) null else bucket to items
        }
    }
    val categoryCount = grouped.size
    val sparse = filtered.size in 1..4
    val headline = when {
        isCompletedTab && filtered.isEmpty() -> "No completed moments"
        isCompletedTab && filtered.size == 1 -> "1 completed moment"
        isCompletedTab -> "${filtered.size} completed moments"
        filtered.isEmpty() -> "No active moments"
        filtered.size == 1 -> "1 active moment"
        else -> "${filtered.size} active moments"
    }
    val subtitle = when {
        isCompletedTab && filtered.isEmpty() -> "Complete a moment to reopen it or relive its Story"
        isCompletedTab -> "Open to settle expenses · Story on each card"
        filtered.isEmpty() -> spec.emptyOngoing
        sparse -> "Your live moment · ready to open"
        else -> "Visual directory · all live"
    }

    fun openMoment(moment: MomentSummary) {
        if (isCompletedTab) {
            onSelectCompletedMoment(moment)
            onDismiss()
        } else {
            onSelectMoment(moment.momentId)
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(spec.background)
            .testTag(spec.testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headline,
                    color = MomentraBrandColors.TextOnDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    subtitle,
                    color = ShellTokens.EmptyBody,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (allowDismiss) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = MomentraBrandColors.TextOnDark.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(6.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(spec.chrome)
                .padding(4.dp),
        ) {
            listOf(
                DirectoryLifecycleTab.ONGOING to "Ongoing",
                DirectoryLifecycleTab.COMPLETED to "Completed",
            ).forEach { (tab, label) ->
                val selected = lifecycleTab == tab
                Text(
                    label,
                    color = if (selected) spec.tabSelectedText else MomentraBrandColors.TextOnDark.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) spec.tabSelectedFill else Color.Transparent)
                        .clickable {
                            lifecycleTab = tab
                            selectedBucket = null
                        }
                        .padding(vertical = 10.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DirectorySummaryTile(
                value = "${filtered.size}",
                label = if (isCompletedTab) "Completed" else "Active now",
                fill = spec.summaryFill,
                valueColor = spec.summaryValue,
                labelColor = spec.summaryLabel,
                modifier = Modifier.weight(1f),
            )
            DirectorySummaryTile(
                value = "$categoryCount",
                label = "Categories",
                fill = spec.summaryFill,
                valueColor = spec.summaryValue,
                labelColor = spec.summaryLabel,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(spec.chrome)
                    .border(1.dp, Color(0xFF322E40), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = ShellTokens.EmptyBody, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MomentraBrandColors.TextOnDark,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                    ),
                    cursorBrush = SolidColor(MomentraBrandColors.Cta),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Find a moment", color = ShellTokens.EmptyBody.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = PlusJakartaSans)
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(spec.chrome)
                    .border(1.dp, Color(0xFF322E40), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = "Filter", tint = ShellTokens.EmptyBody, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(
                label = "All",
                selected = selectedBucket == null,
                chrome = spec.chrome,
                onClick = { selectedBucket = null },
            )
            spec.buckets.forEach { bucket ->
                val count = source.count { spec.classify(it.momentTypeCode).id == bucket.id }
                if (count > 0) {
                    CategoryChip(
                        label = bucket.label,
                        selected = selectedBucket?.id == bucket.id,
                        chrome = spec.chrome,
                        onClick = {
                            selectedBucket = if (selectedBucket?.id == bucket.id) null else bucket
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (loadingCompleted && isCompletedTab) {
                item {
                    Text(
                        "Loading…",
                        color = ShellTokens.EmptyBody,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (completedError != null && isCompletedTab) {
                item {
                    Text(
                        completedError ?: "",
                        color = Color(0xFFF87171),
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(spec.chrome)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            if (isCompletedTab) "No completed moments yet" else "Nothing matches",
                            color = MomentraBrandColors.TextOnDark,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            if (isCompletedTab) "Complete a moment to relive its Story here."
                            else "Try another search or create a new group moment.",
                            color = ShellTokens.EmptyBody,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        if (!isCompletedTab) {
                            CreateAnotherCta(onClick = onCreateMoment, gradient = spec.createGradient)
                        }
                    }
                }
            } else if (sparse) {
                grouped.forEach { (bucket, items) ->
                    item(key = "hdr-${bucket.id}") {
                        SectionHeader(bucket.label.uppercase(Locale.US), items.size)
                    }
                    items(items, key = { it.momentId }) { moment ->
                        val idx = items.indexOf(moment)
                        LargeMomentCard(
                            moment = moment,
                            bucket = bucket,
                            gradientIndex = idx,
                            selected = moment.momentId == selectedMomentId,
                            completed = isCompletedTab,
                            onClick = { openMoment(moment) },
                            onOpenStory = {
                                onOpenStory(moment.momentId)
                                onDismiss()
                            },
                        )
                    }
                }
                if (!isCompletedTab) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(spec.chrome)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "That's everything live",
                            color = MomentraBrandColors.TextOnDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            spec.footerDetail,
                            color = ShellTokens.EmptyBody,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaSans,
                        )
                        CreateAnotherCta(onClick = onCreateMoment, gradient = spec.createGradient)
                    }
                }
                }
            } else {
                grouped.forEach { (bucket, items) ->
                    item(key = "hdr-${bucket.id}") {
                        SectionHeader(bucket.label.uppercase(Locale.US), items.size)
                    }
                    item(key = "row-${bucket.id}") {
                        if (items.size == 1) {
                            LargeMomentCard(
                                moment = items.first(),
                                bucket = bucket,
                                gradientIndex = 0,
                                selected = items.first().momentId == selectedMomentId,
                                completed = isCompletedTab,
                                height = 120.dp,
                                onClick = { openMoment(items.first()) },
                                onOpenStory = {
                                    onOpenStory(items.first().momentId)
                                    onDismiss()
                                },
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(end = 8.dp),
                            ) {
                                items(items, key = { it.momentId }) { moment ->
                                    val idx = items.indexOf(moment)
                                    CompactMomentCard(
                                        moment = moment,
                                        bucket = bucket,
                                        gradientIndex = idx,
                                        selected = moment.momentId == selectedMomentId,
                                        completed = isCompletedTab,
                                        onClick = { openMoment(moment) },
                                        onOpenStory = {
                                            onOpenStory(moment.momentId)
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectorySummaryTile(
    value: String,
    label: String,
    fill: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(value, color = valueColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text(label, color = labelColor, fontSize = 13.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, chrome: Color, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.Black else MomentraBrandColors.TextOnDark,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PlusJakartaSans,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White else chrome)
            .border(1.dp, if (selected) Color.White else Color(0xFF3A3648), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = ShellTokens.EmptyBody, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
        Text("$count", color = ShellTokens.EmptyBody.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun LiveNowBadge(completed: Boolean = false) {
    if (completed) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF94A3B8)),
            )
            Text(
                "COMPLETED",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    } else {
        val pulse = rememberInfiniteTransition(label = "live")
        val alpha by pulse.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "liveAlpha",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80).copy(alpha = alpha)),
            )
            Text(
                "LIVE NOW",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
private fun LargeMomentCard(
    moment: MomentSummary,
    bucket: DirectoryBucket,
    gradientIndex: Int,
    selected: Boolean,
    onClick: () -> Unit,
    completed: Boolean = false,
    onOpenStory: (() -> Unit)? = null,
    height: Dp = 148.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed) 0.98f else 1f
    val members = moment.participantCount.coerceAtLeast(0)
    val meta = "$members member${if (members == 1) "" else "s"} · ${bucket.singular}"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(bucket.gradientAt(gradientIndex)))
            .then(
                if (selected) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
                else Modifier,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveNowBadge(completed = completed)
                if (onOpenStory != null) {
                    Text(
                        "Story",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(onClick = onOpenStory)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    moment.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(meta, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontFamily = PlusJakartaSans)
            }
        }
    }
}

@Composable
private fun CompactMomentCard(
    moment: MomentSummary,
    bucket: DirectoryBucket,
    gradientIndex: Int,
    selected: Boolean,
    onClick: () -> Unit,
    completed: Boolean = false,
    onOpenStory: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = if (pressed) 0.97f else 1f
    val members = moment.participantCount.coerceAtLeast(0)
    Box(
        modifier = Modifier
            .width(148.dp)
            .height(132.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(bucket.gradientAt(gradientIndex)))
            .then(
                if (selected) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                else Modifier,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveNowBadge(completed = completed)
                if (onOpenStory != null) {
                    Text(
                        "Story",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(onClick = onOpenStory)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    moment.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$members members",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

@Composable
private fun CreateAnotherCta(onClick: () -> Unit, gradient: List<Color>) {
    Text(
        "CREATE ANOTHER MOMENT",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        fontFamily = PlusJakartaSans,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    )
}
