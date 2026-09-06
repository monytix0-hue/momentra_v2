package com.example.momentra.ui.shell.tour

import com.example.momentra.domain.BottomDestination

object TourScripts {

    fun personal(hasActiveMoment: Boolean): List<TourStep> {
        val chrome = listOf(
            TourStep(
                id = "personal_context",
                title = "Your life contexts",
                body = "Switch between Personal, Group, and Business. Each has its own Moments and Quick Adds.",
                target = TourTargetId.CONTEXT_SWITCHER,
            ),
            TourStep(
                id = "personal_tabs",
                title = "Navigate your Moment",
                body = "Pulse is right now. Moments is your timeline. Life and Memory go deeper. Create opens Quick Adds once a Moment is active.",
                target = TourTargetId.BOTTOM_NAV,
            ),
        )
        val createPath = if (hasActiveMoment) {
            listOf(
                TourStep(
                    id = "personal_create_qa",
                    title = "Create = Quick Add",
                    body = "With a Moment selected, the Create tab opens Quick Add — log money, mood, milestones, and more in seconds.",
                    target = TourTargetId.BOTTOM_CREATE,
                    advanceOn = setOf(TourSignal.OPENED_CREATE_TAB),
                ),
                TourStep(
                    id = "personal_qa_tile",
                    title = "Tap a Quick Add",
                    body = "Pick any tile to capture something. You’ll see where it shows up after you save.",
                    target = TourTargetId.QA_HUB_TILE,
                    advanceOn = setOf(TourSignal.QUICKADD_SAVED),
                ),
            )
        } else {
            listOf(
                TourStep(
                    id = "personal_start_moment",
                    title = "Start a Moment",
                    body = "Tap Create to choose a life system — Life Operations, Future, Lifestyle, or Relationships.",
                    target = TourTargetId.BOTTOM_CREATE,
                    advanceOn = setOf(TourSignal.OPENED_CREATE_TAB),
                ),
                TourStep(
                    id = "personal_chooser",
                    title = "Pick a life system",
                    body = "Each system is a Moment. Choose one to open setup — you can refine everything later.",
                    target = TourTargetId.CREATE_CHOOSER,
                    advanceOn = setOf(TourSignal.SETUP_SHEET_OPEN),
                ),
                TourStep(
                    id = "personal_activate",
                    title = "Activate or save draft",
                    body = "Activate makes the Moment live and unlocks Pulse + Quick Adds. Save draft keeps progress — drafts already unlock the shell like active Moments.",
                    target = TourTargetId.SETUP_ACTIVATE,
                    advanceOn = setOf(TourSignal.MOMENT_CREATED),
                ),
                TourStep(
                    id = "personal_create_qa_after",
                    title = "Create is now Quick Add",
                    body = "With your Moment selected, Create opens Quick Add instead of the chooser. Log something anytime.",
                    target = TourTargetId.BOTTOM_CREATE,
                    advanceOn = setOf(TourSignal.OPENED_CREATE_TAB),
                ),
                TourStep(
                    id = "personal_qa_tile",
                    title = "Try a Quick Add",
                    body = "Tap a tile to capture an expense, mood, milestone, and more.",
                    target = TourTargetId.QA_HUB_TILE,
                    advanceOn = setOf(TourSignal.QUICKADD_SAVED),
                ),
            )
        }
        val wrap = listOf(
            TourStep(
                id = "personal_where",
                title = "Where to look",
                body = "After a Quick Add, check Pulse for live status, Moments for the timeline, and Life for deeper detail.",
                target = TourTargetId.TAB_PULSE,
                goToDestination = BottomDestination.PULSE,
                nextLabel = "Go to Pulse",
            ),
            TourStep(
                id = "personal_switcher",
                title = "Switch Moments",
                body = "When you have more than one Moment, use the switcher to jump between them or manage setup.",
                target = TourTargetId.MOMENT_SWITCHER,
                nextLabel = "Got it",
            ),
        )
        return chrome + createPath + wrap
    }

    fun groupMini(): List<TourStep> = listOf(
        TourStep(
            id = "group_create_vs_join",
            title = "Create or join",
            body = "Start a new Group Moment from Create, or join one with a code / QR from the top bar.",
            target = TourTargetId.TOP_QR,
        ),
        TourStep(
            id = "group_create_tab",
            title = "Create & Quick Add",
            body = "With a Group Moment selected, Create opens Quick Add for expenses, people, planning, and more.",
            target = TourTargetId.BOTTOM_CREATE,
            advanceOn = setOf(TourSignal.OPENED_CREATE_TAB),
        ),
        TourStep(
            id = "group_where",
            title = "Where group activity shows",
            body = "Look on Pulse for the live board, Moments for the story, and Life for money & planning depth.",
            target = TourTargetId.BOTTOM_NAV,
            goToDestination = BottomDestination.PULSE,
            nextLabel = "Got it",
        ),
    )

    fun businessMini(): List<TourStep> = listOf(
        TourStep(
            id = "biz_company_first",
            title = "Company first",
            body = "Business Moments need a company. Set one up from the company menu before creating a Moment.",
            target = TourTargetId.COMPANY_CHIP,
        ),
        TourStep(
            id = "biz_create_moment",
            title = "Then create a Moment",
            body = "Pick Team Ops, Runway, or Operations — Activate unlocks tracking and Quick Adds.",
            target = TourTargetId.BOTTOM_CREATE,
            advanceOn = setOf(TourSignal.OPENED_CREATE_TAB, TourSignal.MOMENT_CREATED),
        ),
        TourStep(
            id = "biz_where",
            title = "Where business data shows",
            body = "Expenses, revenue, and ops land on Pulse and Moments. Use Create for Quick Adds anytime.",
            target = TourTargetId.BOTTOM_NAV,
            goToDestination = BottomDestination.PULSE,
            nextLabel = "Got it",
        ),
    )
}
