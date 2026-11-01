package com.example.momentra.ui.shell.business.appstate

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The eight screens covered by the shared Figma app-state kit. */
enum class BusinessAppStateScreen {
    Pulse,
    Moments,
    Life,
    Memory,
    Hub,
    ViewAll,
    NoCompany,
    Offline,
    ;

    /** Bottom-bar destination this screen belongs to, if any. */
    val destinationLabel: String?
        get() = when (this) {
            Pulse -> "Pulse"
            Moments -> "Moments"
            Life -> "Life"
            Memory -> "Memory"
            Hub -> "Quick adds"
            ViewAll, NoCompany, Offline -> null
        }
}

/** The four states each screen can be in. */
enum class BusinessAppStatePhase {
    Empty,
    Loading,
    Error,
    Setup,
}

/** How a row's value should be toned. */
enum class ValueTone {
    /** Normal white value. */
    Normal,

    /** Amber — "Not recorded" / "Hidden until online". Never renders 0. */
    Warning,
}

data class AppStateRow(
    val label: String,
    val value: String,
    val tone: ValueTone = ValueTone.Normal,
)

/** The lucide glyph Figma places inside the hero badge (18dp, white stroke). */
enum class AppStateIcon {
    Sparkle,
    Moments,
    Sun,
    Memory,
    Plus,
    Check,
    Alert,
    Offline,
    ;

    /** Material Icons Extended equivalent, tinted white at 18dp inside the badge. */
    val imageVector: ImageVector
        get() = when (this) {
            Sparkle -> Icons.Outlined.AutoAwesome
            Moments -> Icons.Outlined.Bolt
            Sun -> Icons.Outlined.WbSunny
            Memory -> Icons.Outlined.History
            Plus -> Icons.Outlined.Add
            Check -> Icons.Outlined.Check
            Alert -> Icons.Outlined.WarningAmber
            Offline -> Icons.Outlined.CloudOff
        }
}

data class AppStateSpec(
    val screen: BusinessAppStateScreen,
    val phase: BusinessAppStatePhase,
    val icon: AppStateIcon,
    val title: String,
    val body: String,
    val rows: List<AppStateRow> = emptyList(),
    val eyebrow: String? = null,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    val errorCode: String? = null,
    val footnote: String? = null,
)

/**
 * Copy and row data for every state in the Figma kit (node 2666:32397).
 *
 * The kit's governing rule: a missing figure is never rendered as 0. Rows either say
 * "Not recorded" / "None yet" / "Quiet" or the row is omitted entirely.
 */
object BusinessAppStateSpecs {

    private const val FOOTNOTE = "Nothing was lost. Values stay hidden until they reload — never shown as 0."

    /** Hub tile labels are per-vertical text overrides (Figma "Hub label overrides"). */
    fun hubTileLabels(accent: BusinessAppStateAccent): List<Pair<AppStateIcon, String>> = when (accent) {
        BusinessAppStateAccent.Services -> listOf(
            AppStateIcon.Check to "Invoice",
            AppStateIcon.Plus to "Purchase",
            AppStateIcon.Moments to "Collection",
            AppStateIcon.Sparkle to "Payment",
            AppStateIcon.Memory to "Expense",
            AppStateIcon.Sun to "Appointment",
        )
        BusinessAppStateAccent.Marketing -> listOf(
            AppStateIcon.Check to "Client invoice",
            AppStateIcon.Plus to "Vendor bill",
            AppStateIcon.Moments to "Collection",
            AppStateIcon.Sparkle to "Payment",
            AppStateIcon.Memory to "Expense",
            AppStateIcon.Sun to "Campaign",
        )
        else -> listOf(
            AppStateIcon.Check to "Sale",
            AppStateIcon.Plus to "Purchase",
            AppStateIcon.Moments to "Collection",
            AppStateIcon.Sparkle to "Payment",
            AppStateIcon.Memory to "Expense",
            AppStateIcon.Sun to "Stock",
        )
    }

    /** "OCTOBER 2026" — Memory eyebrow. */
    fun monthLabel(now: Date = Date()): String =
        SimpleDateFormat("MMMM yyyy", Locale.US).format(now).uppercase(Locale.US)

    /** "TODAY · 31 OCT" — Life eyebrow. */
    fun todayLabel(now: Date = Date()): String {
        val day = SimpleDateFormat("d MMM", Locale.US).format(now).uppercase(Locale.US)
        return "TODAY · $day"
    }

    fun spec(
        screen: BusinessAppStateScreen,
        phase: BusinessAppStatePhase,
        now: Date = Date(),
    ): AppStateSpec = when (screen) {
        BusinessAppStateScreen.Pulse -> pulse(phase)
        BusinessAppStateScreen.Moments -> moments(phase)
        BusinessAppStateScreen.Life -> life(phase, now)
        BusinessAppStateScreen.Memory -> memory(phase, now)
        BusinessAppStateScreen.Hub -> hub(phase)
        BusinessAppStateScreen.ViewAll -> viewAll(phase)
        BusinessAppStateScreen.NoCompany -> noCompany(phase)
        BusinessAppStateScreen.Offline -> offline(phase)
    }

    private fun pulse(phase: BusinessAppStatePhase): AppStateSpec = when (phase) {
        BusinessAppStatePhase.Empty -> AppStateSpec(
            screen = BusinessAppStateScreen.Pulse,
            phase = phase,
            icon = AppStateIcon.Plus,
            title = "No pulse yet",
            body = "Nothing has been recorded for your business this period. Pulse fills in as sales, collections and stock land.",
            eyebrow = "WHAT SHOWS UP HERE",
            rows = listOf(
                AppStateRow("Sales & collections", "Not recorded", ValueTone.Warning),
                AppStateRow("Needs attention", "Nothing open"),
                AppStateRow("GST position", "Not recorded", ValueTone.Warning),
            ),
            primaryLabel = "Record first sale",
            secondaryLabel = "Open Quick adds",
        )
        BusinessAppStatePhase.Loading -> AppStateSpec(
            screen = BusinessAppStateScreen.Pulse,
            phase = phase,
            icon = AppStateIcon.Sparkle,
            title = "Loading Pulse…",
            body = "",
        )
        BusinessAppStatePhase.Error -> AppStateSpec(
            screen = BusinessAppStateScreen.Pulse,
            phase = phase,
            icon = AppStateIcon.Alert,
            title = "Couldn't load Pulse",
            body = "We couldn't reach your business data. Check your connection and try again.",
            errorCode = "PULSE_FETCH_FAILED",
            primaryLabel = "Retry",
            secondaryLabel = "Open Quick adds",
            footnote = FOOTNOTE,
        )
        BusinessAppStatePhase.Setup -> AppStateSpec(
            screen = BusinessAppStateScreen.Pulse,
            phase = BusinessAppStatePhase.Empty,
            icon = AppStateIcon.Plus,
            title = "No pulse yet",
            body = "Nothing has been recorded for your business this period.",
        )
    }

    private fun moments(phase: BusinessAppStatePhase): AppStateSpec = when (phase) {
        BusinessAppStatePhase.Empty -> AppStateSpec(
            screen = BusinessAppStateScreen.Moments,
            phase = phase,
            icon = AppStateIcon.Moments,
            title = "No moment details yet",
            body = "Your moment is live, but nothing is attached yet. Orders, stock and people appear here as you add them.",
            eyebrow = "A MOMENT HOLDS",
            rows = listOf(
                AppStateRow("Orders & commitments", "None yet"),
                AppStateRow("Stock & products", "None yet"),
                AppStateRow("People & network", "Just you"),
            ),
            primaryLabel = "Add first order",
            secondaryLabel = "Invite your team",
        )
        BusinessAppStatePhase.Loading -> AppStateSpec(
            screen = BusinessAppStateScreen.Moments,
            phase = phase,
            icon = AppStateIcon.Sparkle,
            title = "Loading Moments…",
            body = "",
        )
        BusinessAppStatePhase.Error -> AppStateSpec(
            screen = BusinessAppStateScreen.Moments,
            phase = phase,
            icon = AppStateIcon.Alert,
            title = "Couldn't load Moments",
            body = "The moment is safe — we just couldn't fetch its details right now.",
            errorCode = "MOMENT_DETAIL_FAILED",
            primaryLabel = "Retry",
            secondaryLabel = "Switch moment",
            footnote = FOOTNOTE,
        )
        BusinessAppStatePhase.Setup -> AppStateSpec(
            screen = BusinessAppStateScreen.Moments,
            phase = BusinessAppStatePhase.Empty,
            icon = AppStateIcon.Moments,
            title = "No moment details yet",
            body = "Your moment is live, but nothing is attached yet.",
        )
    }

    private fun life(phase: BusinessAppStatePhase, now: Date): AppStateSpec = when (phase) {
        BusinessAppStatePhase.Empty -> AppStateSpec(
            screen = BusinessAppStateScreen.Life,
            phase = phase,
            icon = AppStateIcon.Sun,
            title = "Nothing moving today",
            body = "A calm day. No dispatches, collections or people activity yet — Life updates the moment something moves.",
            eyebrow = todayLabel(now),
            rows = listOf(
                AppStateRow("Money moving", "Nothing yet"),
                AppStateRow("Dealers & channels", "Quiet"),
                AppStateRow("People & activity", "Quiet"),
            ),
            primaryLabel = "Open Quick adds",
            secondaryLabel = "Back to Pulse",
        )
        BusinessAppStatePhase.Loading -> AppStateSpec(
            screen = BusinessAppStateScreen.Life,
            phase = phase,
            icon = AppStateIcon.Sparkle,
            title = "Loading Life…",
            body = "",
        )
        BusinessAppStatePhase.Error -> AppStateSpec(
            screen = BusinessAppStateScreen.Life,
            phase = phase,
            icon = AppStateIcon.Alert,
            title = "Couldn't load Life",
            body = "Today's activity couldn't be fetched. Your records are safe.",
            errorCode = "LIFE_FEED_FAILED",
            primaryLabel = "Retry",
            secondaryLabel = "Back to Pulse",
            footnote = FOOTNOTE,
        )
        BusinessAppStatePhase.Setup -> AppStateSpec(
            screen = BusinessAppStateScreen.Life,
            phase = BusinessAppStatePhase.Empty,
            icon = AppStateIcon.Sun,
            title = "Nothing moving today",
            body = "A calm day. Life updates the moment something moves.",
            eyebrow = todayLabel(now),
        )
    }

    private fun memory(phase: BusinessAppStatePhase, now: Date): AppStateSpec = when (phase) {
        BusinessAppStatePhase.Empty -> AppStateSpec(
            screen = BusinessAppStateScreen.Memory,
            phase = phase,
            icon = AppStateIcon.Memory,
            title = "No Memory yet",
            body = "No closed activity for ${monthLabel(now)} yet. Memory writes itself as days close — nothing is estimated.",
            eyebrow = monthLabel(now),
            rows = listOf(
                AppStateRow("Days closed", "None yet", ValueTone.Warning),
                AppStateRow("Money memory", "Not recorded", ValueTone.Warning),
                AppStateRow("Story", "Needs closed days"),
            ),
            primaryLabel = "Pick another month",
            secondaryLabel = "Back to Pulse",
        )
        BusinessAppStatePhase.Loading -> AppStateSpec(
            screen = BusinessAppStateScreen.Memory,
            phase = phase,
            icon = AppStateIcon.Sparkle,
            title = "Loading Memory…",
            body = "",
        )
        BusinessAppStatePhase.Error -> AppStateSpec(
            screen = BusinessAppStateScreen.Memory,
            phase = phase,
            icon = AppStateIcon.Alert,
            title = "Couldn't load Memory",
            body = "Your history is intact — this month just didn't load.",
            errorCode = "MEMORY_FETCH_FAILED",
            primaryLabel = "Retry",
            secondaryLabel = "Back to Pulse",
            footnote = FOOTNOTE,
        )
        BusinessAppStatePhase.Setup -> AppStateSpec(
            screen = BusinessAppStateScreen.Memory,
            phase = BusinessAppStatePhase.Empty,
            icon = AppStateIcon.Memory,
            title = "No Memory yet",
            body = "Memory writes itself as days close — nothing is estimated.",
            eyebrow = monthLabel(now),
        )
    }

    private fun hub(phase: BusinessAppStatePhase): AppStateSpec = when (phase) {
        BusinessAppStatePhase.Empty -> AppStateSpec(
            screen = BusinessAppStateScreen.Hub,
            phase = phase,
            icon = AppStateIcon.Plus,
            title = "Start with a capture",
            body = "Nothing recorded yet. One sale, purchase or stock update is enough to light up Pulse.",
            footnote = "Tap Sale to begin — the rest unlock as you go.",
        )
        BusinessAppStatePhase.Error -> AppStateSpec(
            screen = BusinessAppStateScreen.Hub,
            phase = phase,
            icon = AppStateIcon.Alert,
            title = "Couldn't load Quick adds",
            body = "Capture forms couldn't load. Nothing you entered was lost.",
            errorCode = "HUB_FETCH_FAILED",
            primaryLabel = "Retry",
            secondaryLabel = "Back to Pulse",
            footnote = FOOTNOTE,
        )
        BusinessAppStatePhase.Loading, BusinessAppStatePhase.Setup -> AppStateSpec(
            screen = BusinessAppStateScreen.Hub,
            phase = BusinessAppStatePhase.Empty,
            icon = AppStateIcon.Plus,
            title = "Start with a capture",
            body = "Nothing recorded yet. One sale, purchase or stock update is enough to light up Pulse.",
        )
    }

    private fun viewAll(phase: BusinessAppStatePhase): AppStateSpec = AppStateSpec(
        screen = BusinessAppStateScreen.ViewAll,
        phase = if (phase == BusinessAppStatePhase.Loading || phase == BusinessAppStatePhase.Error) {
            BusinessAppStatePhase.Empty
        } else {
            phase
        },
        icon = AppStateIcon.Check,
        title = "Nothing here",
        body = "No open items under \u201COpen\u201D. Everything that needed attention is handled — or nothing has been recorded yet.",
        eyebrow = "PULSE · VIEW ALL",
        primaryLabel = null,
        secondaryLabel = "Show all items",
        footnote = "Filter: Open · Memory keeps closed items",
    )

    private fun noCompany(phase: BusinessAppStatePhase): AppStateSpec = AppStateSpec(
        screen = BusinessAppStateScreen.NoCompany,
        phase = BusinessAppStatePhase.Setup,
        icon = AppStateIcon.Plus,
        title = "Add your business to open Pulse",
        body = "Pulse, Moments, Life and Memory need a business first. Takes about two minutes — GSTIN optional.",
        eyebrow = "YOU'LL SET UP",
        rows = listOf(
            AppStateRow("Business name & type", "Step 1"),
            AppStateRow("Financial year & currency", "Step 2"),
            AppStateRow("Invite team (optional)", "Step 3"),
        ),
        primaryLabel = "Create business",
        secondaryLabel = "Join with invite code",
    )

    private fun offline(phase: BusinessAppStatePhase): AppStateSpec = AppStateSpec(
        screen = BusinessAppStateScreen.Offline,
        phase = BusinessAppStatePhase.Error,
        icon = AppStateIcon.Offline,
        title = "Waiting for connection",
        body = "Last-known Pulse is shown read-only. Captures queue on this device and sync when you're back.",
        eyebrow = "LAST KNOWN",
        rows = listOf(
            AppStateRow("Last synced", "—"),
            AppStateRow("Queued captures", "None"),
            AppStateRow("Live figures", "Hidden until online", ValueTone.Warning),
        ),
        primaryLabel = "Retry",
    )

    /** Banner text shown above the ring on the offline state. */
    const val OFFLINE_BANNER = "You're offline · showing last known"
}
