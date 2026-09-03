package com.example.momentra.ui.shell.personal.relationships.create

import com.example.momentra.data.api.ActivityItemDto
import java.util.UUID

/** Relationships activity row — Figma `1036:7697` / Pulse preview. */
data class RelationshipsActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val whenLabel: String,
    val impact: String,
    val emoji: String,
    val relationship: String = "Partner",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val filter: String = "Partner",
)

fun mapActivityDtosToRelationships(items: List<ActivityItemDto>): List<RelationshipsActivityItem> {
    if (items.isEmpty()) return emptyList()
    return items.map { dto ->
        val meta = relationshipsActivityVisual(dto.activityCode, dto.title)
        RelationshipsActivityItem(
            id = dto.occurredAt + dto.title,
            title = dto.title.ifBlank { meta.title },
            whenLabel = formatRelationshipsOccurredAt(dto.occurredAt),
            impact = meta.impact,
            emoji = meta.emoji,
            relationship = meta.filter,
            filter = meta.filter,
        )
    }
}

private data class RelVisual(val title: String, val emoji: String, val impact: String, val filter: String)

private fun relationshipsActivityVisual(code: String, title: String): RelVisual {
    val c = code.uppercase()
    return when {
        c.contains("CONNECT") || title.contains("connection", true) ->
            RelVisual(title.ifBlank { "Connection" }, "💬", "Connection", "Partner")
        c.contains("SUPPORT") || title.contains("support", true) ->
            RelVisual(title.ifBlank { "Support" }, "🫶", "Support", "Partner")
        c.contains("SHARED") || title.contains("shared", true) ->
            RelVisual(title.ifBlank { "Shared experience" }, "🤝", "Shared", "Partner")
        c.contains("INVEST") ->
            RelVisual(title.ifBlank { "Investment" }, "💝", "Investment", "Partner")
        c.contains("FAMILY") || title.contains("family", true) ->
            RelVisual(title.ifBlank { "Family" }, "📞", "Family", "Family")
        c.contains("FRIEND") ->
            RelVisual(title.ifBlank { "Friend time" }, "🤝", "Friends", "Friends")
        c.contains("SELF") || c.contains("REFLECT") || c.contains("INTERACTION") ->
            RelVisual(title.ifBlank { "Interaction" }, "🪞", "Interaction", "Self")
        else -> RelVisual(title.ifBlank { "Activity" }, "❤️", "Logged", "Partner")
    }
}

private fun formatRelationshipsOccurredAt(iso: String): String = try {
    val instant = java.time.Instant.parse(iso)
    java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a", java.util.Locale.getDefault())
        .withZone(java.time.ZoneId.systemDefault())
        .format(instant)
} catch (_: Exception) {
    iso
}
