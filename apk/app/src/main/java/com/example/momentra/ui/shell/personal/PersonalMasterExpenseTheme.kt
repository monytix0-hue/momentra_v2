package com.example.momentra.ui.shell.personal

import androidx.compose.ui.graphics.Color

/** Figma 453:9376 Master Expense premium tokens — fixed purple accent. */
object PersonalMasterExpenseTheme {
    val Bg = Color(0xFF191622)
    val Surface = Color(0xB3181424)
    val SurfaceSolid = Color(0xFF181424)
    val Border = Color(0xFF342C44)
    val Accent = Color(0xFF7C5CFC)
    val AccentLight = Color(0xFFC9BFFF)
    val Text = Color(0xFFFFFFFF)
    val TextMain = Color(0xFFE5E0EE)
    val Muted = Color(0xFF9E9AA7)
    val CategoryUnselected = Color(0xFF14131A)
    val CategoryBorder = Color.White.copy(alpha = 0.1f)
    val Error = Color(0xFFF87171)

    data class EmotionalOption(val emoji: String, val label: String)
    data class RelationshipImpactOption(val emoji: String, val label: String)

    val emotionalOptions = listOf(
        EmotionalOption("😌", "Relieved"),
        EmotionalOption("😊", "Happy"),
        EmotionalOption("🤝", "Connected"),
        EmotionalOption("😎", "Proud"),
        EmotionalOption("😐", "Neutral"),
        EmotionalOption("🤔", "Unsure"),
        EmotionalOption("😔", "Guilty"),
        EmotionalOption("🙏", "Blessed"),
        EmotionalOption("😞", "Disappointed"),
    )

    val sharedWithOptions = listOf("Spouse", "Parents", "Family", "Friends", "Custom")

    val relationshipImpactOptions = listOf(
        RelationshipImpactOption("💪", "Strengthened Connection"),
        RelationshipImpactOption("🎉", "Celebration Together"),
        RelationshipImpactOption("🤲", "Support Given"),
    )

    val reasoningOptions = listOf("Celebration", "Daily Need", "Gift", "Travel", "Other")

    val segmentOptions = listOf("Low", "Medium", "High")
    val whenOptions = listOf("Now", "Today", "Yesterday")
}
