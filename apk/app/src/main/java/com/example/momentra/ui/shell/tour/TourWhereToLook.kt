package com.example.momentra.ui.shell.tour

import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.shell.personal.future.create.FutureQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.LifeOpsQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.MoneyQuickAddKind
import com.example.momentra.ui.shell.personal.shared.LifestyleQuickAddKind
import com.example.momentra.ui.shell.personal.shared.RelationshipsQuickAddKind

object TourWhereToLook {

    fun forMoney(kind: MoneyQuickAddKind): WhereToLookHint = when (kind) {
        MoneyQuickAddKind.MASTER_EXPENSE -> WhereToLookHint(
            title = "Expense saved",
            body = "Find it on Moments (timeline) and Life (money detail). Pulse reflects recent activity.",
            goTo = BottomDestination.MOMENTS,
        )
        MoneyQuickAddKind.INCOME -> WhereToLookHint(
            title = "Income saved",
            body = "Income shows in Life money views and on your Moments timeline.",
            goTo = BottomDestination.LIFE,
        )
        MoneyQuickAddKind.TRANSFER -> WhereToLookHint(
            title = "Transfer saved",
            body = "Transfers appear in Life account flows and Moments activity.",
            goTo = BottomDestination.LIFE,
        )
        MoneyQuickAddKind.SAVINGS -> WhereToLookHint(
            title = "Savings logged",
            body = "Check Life for savings goals and Moments for the timeline entry.",
            goTo = BottomDestination.LIFE,
        )
    }

    fun forLifeOps(kind: LifeOpsQuickAddKind): WhereToLookHint = when (kind) {
        LifeOpsQuickAddKind.RECOVERY -> WhereToLookHint(
            title = "Recovery saved",
            body = "Check Pulse for how recovery shows in your rhythm, and Life for deeper history.",
            goTo = BottomDestination.PULSE,
        )
        LifeOpsQuickAddKind.MOOD -> WhereToLookHint(
            title = "Mood logged",
            body = "Your reflection appears on Pulse. Open Moments to see it on the timeline.",
            goTo = BottomDestination.PULSE,
        )
        LifeOpsQuickAddKind.ATTENTION, LifeOpsQuickAddKind.ADJUST -> WhereToLookHint(
            title = "Rhythm updated",
            body = "Attention and adjustments surface on Pulse first, then in Life.",
            goTo = BottomDestination.PULSE,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun forFuture(kind: FutureQuickAddKind): WhereToLookHint = WhereToLookHint(
        title = "Future action saved",
        body = "Milestones and progress show on Pulse and build your Moments timeline.",
        goTo = BottomDestination.PULSE,
    )

    @Suppress("UNUSED_PARAMETER")
    fun forLifestyle(kind: LifestyleQuickAddKind): WhereToLookHint = WhereToLookHint(
        title = "Lifestyle logged",
        body = "Look on Pulse for the latest, and Moments for the story of your experiences.",
        goTo = BottomDestination.PULSE,
    )

    @Suppress("UNUSED_PARAMETER")
    fun forRelationships(kind: RelationshipsQuickAddKind): WhereToLookHint = WhereToLookHint(
        title = "Connection saved",
        body = "Relationship activity shows on Pulse. Open Moments for the shared timeline.",
        goTo = BottomDestination.PULSE,
    )

    fun forGroupGeneric(label: String = "Saved"): WhereToLookHint = WhereToLookHint(
        title = label,
        body = "Group updates show on Pulse. Check Moments for the story and Life for money & planning.",
        goTo = BottomDestination.PULSE,
    )

    fun forBusinessGeneric(label: String = "Saved"): WhereToLookHint = WhereToLookHint(
        title = label,
        body = "Business activity lands on Pulse and Moments. Use Life for deeper ops and finance.",
        goTo = BottomDestination.PULSE,
    )
}
