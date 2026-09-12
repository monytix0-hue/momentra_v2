package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.api.ActivityItemDto
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class GroupActivityTreeNode(
    val item: ActivityItemDto,
    val children: List<ActivityItemDto> = emptyList(),
)

fun groupActivityAmountLabel(item: ActivityItemDto): String? {
    val raw = item.activityPayload?.amount ?: return null
    val value = raw.toDoubleOrNull() ?: return null
    val currency = item.activityPayload?.currencyCode ?: "INR"
    val symbol = if (currency.equals("INR", ignoreCase = true)) "₹" else "$currency "
    val rounded = value.roundToInt()
    return if (abs(value - rounded) < 0.001) {
        "$symbol$rounded"
    } else {
        String.format(Locale.getDefault(), "%s%.2f", symbol, value)
    }
}

fun groupActivityGroupKey(item: ActivityItemDto): String? {
    item.activityPayload?.expenseId?.trim()?.takeIf { it.isNotEmpty() }?.let { return "expense:$it" }
    item.activityPayload?.contributionId?.trim()?.takeIf { it.isNotEmpty() }?.let { return "contrib:$it" }
    return null
}

fun isGroupActivityVoided(activityCode: String): Boolean =
    activityCode.uppercase(Locale.US).contains("VOIDED")

fun isGroupActivityUpdated(activityCode: String): Boolean =
    activityCode.uppercase(Locale.US).contains("_UPDATED")

fun groupActivityDeletedTitle(item: ActivityItemDto): String {
    val actor = item.actorDisplayName?.trim()?.takeIf { it.isNotEmpty() }
    val firstName = actor?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotEmpty() }
    return if (firstName != null) "$firstName deleted this activity" else "Deleted activity"
}

fun groupActivityRowTitle(item: ActivityItemDto, isChild: Boolean): String {
    if (isChild && isGroupActivityVoided(item.activityCode)) {
        return groupActivityDeletedTitle(item)
    }
    return groupActivityDisplayTitle(item)
}

/**
 * Nest UPDATED / VOIDED lifecycle rows under the original expense or contribution.
 * Unkeyed rows stay top-level. Parents ordered by occurredAt descending.
 */
fun groupActivityTree(items: List<ActivityItemDto>): List<GroupActivityTreeNode> {
    if (items.isEmpty()) return emptyList()

    val keyed = linkedMapOf<String, MutableList<ActivityItemDto>>()
    val unkeyed = mutableListOf<ActivityItemDto>()
    for (item in items) {
        val key = groupActivityGroupKey(item)
        if (key == null) {
            unkeyed.add(item)
        } else {
            keyed.getOrPut(key) { mutableListOf() }.add(item)
        }
    }

    val nodes = mutableListOf<GroupActivityTreeNode>()
    for ((_, group) in keyed) {
        nodes.add(buildGroupActivityNode(group))
    }
    for (item in unkeyed) {
        nodes.add(GroupActivityTreeNode(item))
    }

    return nodes.sortedByDescending { parseGroupActivityOccurredAtMillis(it.item.occurredAt) }
}

fun groupActivityNodeHasVoidChild(node: GroupActivityTreeNode): Boolean =
    node.children.any { isGroupActivityVoided(it.activityCode) } ||
        isGroupActivityVoided(node.item.activityCode)

private fun buildGroupActivityNode(group: List<ActivityItemDto>): GroupActivityTreeNode {
    val sorted = group.sortedBy { parseGroupActivityOccurredAtMillis(it.occurredAt) }
    val parent = sorted.firstOrNull {
        it.activityCode.uppercase(Locale.US).contains("RECORDED") && !isGroupActivityVoided(it.activityCode)
    } ?: sorted.firstOrNull { !isGroupActivityVoided(it.activityCode) }
        ?: sorted.first()

    val children = sorted.filter { item ->
        if (item === parent) return@filter false
        isGroupActivityVoided(item.activityCode) || isGroupActivityUpdated(item.activityCode)
    }
    return GroupActivityTreeNode(parent, children)
}

private fun parseGroupActivityOccurredAtMillis(raw: String): Long =
    runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
