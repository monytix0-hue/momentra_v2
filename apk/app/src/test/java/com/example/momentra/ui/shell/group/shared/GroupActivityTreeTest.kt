package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.api.ActivityItemDto
import com.example.momentra.data.api.ActivityPayloadDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupActivityTreeTest {
    @Test
    fun amountLabelFormatsInr() {
        val item = activity(
            code = "GROUP_EXPENSE_RECORDED",
            amount = "250.0000",
            currency = "INR",
            expenseId = "e1",
        )
        assertEquals("₹250", groupActivityAmountLabel(item))
    }

    @Test
    fun nestsVoidedUnderRecorded() {
        val recorded = activity(
            code = "GROUP_EXPENSE_RECORDED",
            title = "Lunch",
            occurredAt = "2026-09-10T10:00:00Z",
            amount = "100.0000",
            expenseId = "e1",
        )
        val voided = activity(
            code = "GROUP_EXPENSE_VOIDED",
            title = "Lunch",
            occurredAt = "2026-09-11T12:00:00Z",
            amount = "100.0000",
            expenseId = "e1",
            actor = "Sam",
        )
        val other = activity(
            code = "GROUP_POLL_CREATED",
            title = "Poll",
            occurredAt = "2026-09-12T09:00:00Z",
        )

        val tree = groupActivityTree(listOf(voided, other, recorded))
        assertEquals(2, tree.size)
        val expenseNode = tree.first { it.item.activityPayload?.expenseId == "e1" }
        assertEquals("GROUP_EXPENSE_RECORDED", expenseNode.item.activityCode)
        assertEquals(1, expenseNode.children.size)
        assertEquals("GROUP_EXPENSE_VOIDED", expenseNode.children[0].activityCode)
        assertTrue(groupActivityNodeHasVoidChild(expenseNode))
        assertEquals("Sam deleted this activity", groupActivityRowTitle(expenseNode.children[0], isChild = true))
        assertFalse(tree.any { it.item.activityCode == "GROUP_EXPENSE_VOIDED" })
    }

    private fun activity(
        code: String,
        title: String = "Item",
        occurredAt: String = "2026-09-10T10:00:00Z",
        amount: String? = null,
        currency: String? = "INR",
        expenseId: String? = null,
        actor: String? = null,
    ) = ActivityItemDto(
        activityCode = code,
        title = title,
        occurredAt = occurredAt,
        activityPayload = if (amount != null || expenseId != null) {
            ActivityPayloadDto(
                expenseId = expenseId,
                amount = amount,
                currencyCode = currency,
            )
        } else {
            null
        },
        actorDisplayName = actor,
    )
}
