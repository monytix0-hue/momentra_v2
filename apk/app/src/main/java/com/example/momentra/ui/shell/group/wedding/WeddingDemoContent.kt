package com.example.momentra.ui.shell.group.wedding

/**
 * QUARANTINED — static Figma copy only. Must never be wired into production UI paths.
 * Live Wedding screens use APIs + honest empties. Kept for isolated previews/tests.
 */
@Deprecated("Do not use in production UI — quarantine for Figma layout reference only")
object WeddingDemoContent {
    const val HEALTH_SCORE = 82
    const val HEALTH_SUBTITLE = "Coordination is moving smoothly."
    const val HEALTH_DETAIL = "Planning momentum is strong. Keep the team aligned!"
    const val HEALTH_UPDATED = "Updated 2 hours ago"
    const val HERO_DATE_LABEL = "18 Oct 2026"
    const val HERO_COUNTDOWN = "2 months away!"
    const val HERO_TYPE_PILL = "Wedding • 18 Oct 2026"

    data class StatusPill(val label: String, val emoji: String)
    val healthPills = listOf(
        StatusPill("74% Activity", "💖"),
        StatusPill("8 Replies", "👥"),
        StatusPill("Trending Up", "✨"),
    )

    data class AttentionItem(val emoji: String, val title: String, val detail: String, val cta: String)
    val attentionItems = listOf(
        AttentionItem("🏛️", "Venue deposit due", "Finalize payment by Oct 1", "Pay Now"),
        AttentionItem("📋", "Guest list finalization", "Confirm headcount for catering", "Review"),
        AttentionItem("🍽️", "Catering menu selection", "Finalize appetizers and desserts", "Choose"),
    )

    const val PROGRESS_PERCENT = 75
    const val TASKS_DONE = 12
    const val TASKS_TOTAL = 16
    const val DEMO_RSVP_COUNT = "74"
    const val DEMO_RSVP_PERCENT = "87%"
    const val DEMO_TASKS = "18"
    const val SEGMENT_FILLED = 6

    data class PartyMember(val name: String, val role: String, val percent: Int, val featured: Boolean = false)
    val partyMembers = listOf(
        PartyMember("Sarah", "Bride", 92, featured = true),
        PartyMember("Mike", "Groom", 78),
        PartyMember("Priya", "Maid of Honor", 72),
        PartyMember("Arjun", "Best Man", 48),
        PartyMember("Meera", "Planner", 55),
    )

    data class BudgetCategory(val label: String, val amountLabel: String, val fraction: Float)
    val budgetCategories = listOf(
        BudgetCategory("Venue & Catering", "₹2,80,000", 0.85f),
        BudgetCategory("Decor & Photography", "₹1,20,000", 0.45f),
    )
    const val DEMO_BALANCE_OWE = "You owe ₹15,000"
    const val DEMO_TOTAL_POOL = "₹5,20,000"

    data class ActivityDemo(val emoji: String, val title: String, val whenLabel: String)
    val activityFeed = listOf(
        ActivityDemo("📸", "Sarah added venue photos", "Today, 9:42 AM"),
        ActivityDemo("🎵", "Mike confirmed DJ booking", "Today, 8:15 AM"),
        ActivityDemo("👗", "Priya shared outfit ideas", "Yesterday, 11:23 PM"),
        ActivityDemo("🎁", "Meera added registry items", "Yesterday, 6:00 PM"),
    )

    data class Insight(val emoji: String, val title: String, val body: String)
    val insights = listOf(
        Insight("👥", "Guest coordination", "Guest coordination is 85% complete — Finalize RSVP this week."),
        Insight("📷", "Venue momentum", "Venue photos boosted planning momentum — Share more venue details to keep it up."),
    )

    data class TimelineDay(val title: String, val date: String, val detail: String, val time: String, val emoji: String)
    val momentsTimeline = listOf(
        TimelineDay("MEHNDI", "17 OCT", "Mehndi ceremony & cocktails", "4:00 PM", "☀️"),
        TimelineDay("WEDDING", "18 OCT", "Main ceremony & reception", "11:00 AM", "🛕"),
        TimelineDay("RECEPTION", "18 OCT", "Grand reception & dinner", "7:00 PM", "🌙"),
    )

    data class GalleryTile(val title: String, val countLabel: String? = null)
    val galleryTiles = listOf(
        GalleryTile("Venue decor", "12"),
        GalleryTile("Mehndi setup", "8"),
        GalleryTile("Outfits", "6"),
    )

    data class UpcomingEvent(val title: String, val detail: String, val badge: String?, val emoji: String)
    val upcomingEvents = listOf(
        UpcomingEvent("Venue walkthrough", "Final check • 12 Oct", "THIS SATURDAY", "🏠"),
        UpcomingEvent("Outfit fitting", "Next week • Bride & bridesmaids", null, "📅"),
    )

    data class MemoryMilestone(val title: String, val date: String, val emoji: String)
    val memoryTimeline = listOf(
        MemoryMilestone("The group was created", "02 Aug 2026", "⭐"),
        MemoryMilestone("Venue confirmed and booked", "18 Sep 2026", "📍"),
        MemoryMilestone("First dress fitting captured", "03 Oct 2026", "👜"),
        MemoryMilestone("Mehndi night memories", "13 Dec 2026", "📷"),
    )

    val memoryFeatureCards = listOf(
        "First plan confirmed",
        "Everyone contributed",
        "50 RSVPs received",
    )

    val memoryInsights = listOf(
        Pair("Strongest memory signal", "Mehndi night was the most emotional shared moment."),
        Pair("Participation pattern", "Photos were captured by all active members."),
    )
}
