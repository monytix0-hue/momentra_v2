package com.example.momentra.ui.shell.group.shared

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * TRD-STORY-STATE / MTO-STORY-STATE kit — empty, failed, unauthorized, conflict,
 * loading, and config-no-activity surfaces from Figma `2657:29510`+.
 *
 * Shared across business verticals; accent follows the vertical ember/amber/emerald.
 */

internal val StoryStateNight = Color(0xFF090D16)
internal val StoryStateInk = Color(0xFFF8F9FD)
internal val StoryStateMuted = Color(0xFFF8F9FD).copy(alpha = 0.70f)
internal val StoryStateGlass = Color.White.copy(alpha = 0.08f)
internal val StoryStateGlassBorder = Color.White.copy(alpha = 0.14f)
internal val StoryStateEmber = Color(0xFFE8621A)
internal val StoryStateEmberSoft = Color(0xFFF5A623)

enum class StoryStateKind {
    MemoryEmpty,
    ConfigNoActivity,
    Generating,
    GeneratingFailed,
    Conflict,
    Unauthorized,
    LoadingSkeleton,
    PartialPreview,
}

data class StoryStateSpec(
    val kind: StoryStateKind,
    val title: String,
    val body: String,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    val errorCode: String? = null,
    val rows: List<Pair<String, String>> = emptyList(),
    val eyebrow: String? = null,
)

object StoryStateSpecs {
    fun memoryEmpty(businessLabel: String = "this business"): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.MemoryEmpty,
        title = "No stories yet",
        body = "Turn this moment’s verified activity into an 8-slide Story you can relive and share.",
        primaryLabel = "GENERATE STORY",
        eyebrow = "$businessLabel · Stories",
    )

    fun configNoActivity(periodLabel: String = "this period"): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.ConfigNoActivity,
        title = "No verified activity in this period",
        body = "We found no reconciled trades, invoices or payouts for $periodLabel. We won’t show zeros for data we don’t have.",
        primaryLabel = "CHANGE PERIOD",
        secondaryLabel = "Generate · needs activity",
        rows = listOf(
            "Trades" to "Not recorded",
            "Revenue" to "Not recorded",
            "Payouts" to "Not recorded",
        ),
    )

    fun generating(): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.Generating,
        title = "Generating your Moment Story…",
        body = "Building eight slides from verified activity. Nothing is shared until this finishes.",
    )

    fun generatingFailed(detail: String? = null): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.GeneratingFailed,
        title = "Story couldn’t finish",
        body = detail?.takeIf { it.isNotBlank() }
            ?: "Something broke while building the story. Nothing was saved and your data is untouched.",
        primaryLabel = "RETRY",
        secondaryLabel = "Cancel",
        errorCode = "GENERATION_FAILED",
    )

    fun conflict(): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.Conflict,
        title = "Numbers need a check before we share",
        body = "Two sources disagree for this period, so totals are held back until they reconcile.",
        primaryLabel = "REVIEW SOURCES",
        secondaryLabel = "Retry check",
        errorCode = "RECONCILIATION_REQUIRED",
        rows = listOf(
            "Revenue total" to "Held · conflict",
            "Export & share" to "Blocked",
        ),
    )

    fun unauthorized(): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.Unauthorized,
        title = "This story isn’t available to you",
        body = "Ask the moment owner for access. We’re not showing any details from it.",
        primaryLabel = "Go back",
        errorCode = "ACCESS_DENIED",
    )

    fun loading(): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.LoadingSkeleton,
        title = "Loading Story…",
        body = "",
    )

    fun loadError(detail: String?): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.GeneratingFailed,
        title = "Couldn’t load Story",
        body = detail?.takeIf { it.isNotBlank() } ?: "Check your connection and try again.",
        primaryLabel = "RETRY",
        secondaryLabel = "Cancel",
        errorCode = "STORY_LOAD_FAILED",
    )

    fun notStarted(completed: Boolean): StoryStateSpec = StoryStateSpec(
        kind = StoryStateKind.MemoryEmpty,
        title = if (completed) "Story is not ready yet" else "No Story yet",
        body = if (completed) {
            "Tap Generate to create it so every member can view it."
        } else {
            "Complete the moment first, then generate the Story."
        },
        primaryLabel = "GENERATE STORY",
    )
}

@Composable
fun StoryStateBody(
    spec: StoryStateSpec,
    accent: Color = StoryStateEmber,
    onPrimary: (() -> Unit)? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .fillMaxSize()
            .background(StoryStateNight),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (spec.kind) {
                StoryStateKind.LoadingSkeleton, StoryStateKind.Generating -> {
                    CircularProgressIndicator(color = accent)
                    if (spec.title.isNotBlank()) {
                        Text(
                            spec.title,
                            color = StoryStateInk,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (spec.body.isNotBlank()) {
                        Text(
                            spec.body,
                            color = StoryStateMuted,
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(StoryStateGlass)
                            .border(1.dp, StoryStateGlassBorder, shape)
                            .padding(horizontal = 22.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        StoryStateMark(accent = accent, kind = spec.kind)
                        Text(
                            spec.title,
                            color = StoryStateInk,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                        )
                        if (spec.body.isNotBlank()) {
                            Text(
                                spec.body,
                                color = StoryStateMuted,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                            )
                        }
                        spec.errorCode?.let { code ->
                            Text(
                                code,
                                color = accent,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accent.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        if (spec.rows.isNotEmpty()) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                spec.rows.forEach { (label, value) ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(label, color = StoryStateMuted, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                                        Text(value, color = Color(0xFFF5A623), fontFamily = PlusJakartaSans, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        spec.primaryLabel?.let { label ->
                            val primaryEnabled = onPrimary != null &&
                                !(spec.kind == StoryStateKind.ConfigNoActivity && label.startsWith("Generate"))
                            StoryStatePrimaryButton(
                                label = label,
                                accent = accent,
                                enabled = primaryEnabled,
                                onClick = { onPrimary?.invoke() },
                            )
                        }
                        spec.secondaryLabel?.let { label ->
                            if (onSecondary != null || spec.kind == StoryStateKind.ConfigNoActivity) {
                                StoryStateSecondaryButton(
                                    label = label,
                                    enabled = onSecondary != null &&
                                        !(spec.kind == StoryStateKind.ConfigNoActivity && label.startsWith("Generate")),
                                    onClick = { onSecondary?.invoke() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryStateMark(accent: Color, kind: StoryStateKind) {
    val glyph = when (kind) {
        StoryStateKind.Unauthorized -> "⊘"
        StoryStateKind.Conflict -> "⇄"
        StoryStateKind.GeneratingFailed, StoryStateKind.ConfigNoActivity -> "!"
        else -> "✦"
    }
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("M", color = StoryStateInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(accent)
                .border(3.dp, StoryStateNight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, color = Color.White, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StoryStatePrimaryButton(
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(accent, StoryStateEmberSoft))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.08f)))
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Color.White else StoryStateMuted,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun StoryStateSecondaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) StoryStateInk else StoryStateMuted,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun StorySavedPanel(
    versionLabel: String,
    footnote: String = "New or backdated entries create Version 2 with a changed-facts badge.",
    accent: Color = StoryStateEmber,
    onShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(StoryStateNight)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(3.dp, Color(0xFF16181F), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Text(
            "Story saved to Memory",
            color = StoryStateInk,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            versionLabel,
            color = StoryStateInk.copy(alpha = 0.65f),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            footnote,
            color = StoryStateInk.copy(alpha = 0.45f),
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.92f))
                .clickable(onClick = onShare),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Share story",
                color = StoryStateNight,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Back to Memory",
                color = StoryStateInk,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
