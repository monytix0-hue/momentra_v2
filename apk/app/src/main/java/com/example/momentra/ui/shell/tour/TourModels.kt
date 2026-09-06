package com.example.momentra.ui.shell.tour

import com.example.momentra.domain.BottomDestination

enum class TourId {
    PERSONAL,
    GROUP_MINI,
    BUSINESS_MINI,
}

enum class TourTargetId {
    CONTEXT_SWITCHER,
    BOTTOM_NAV,
    BOTTOM_CREATE,
    TAB_PULSE,
    TAB_MOMENTS,
    TAB_LIFE,
    TAB_MEMORY,
    MOMENT_SWITCHER,
    CREATE_CHOOSER,
    SETUP_ACTIVATE,
    SETUP_DRAFT,
    QA_HUB_TILE,
    TOP_QR,
    COMPANY_CHIP,
}

enum class TourSignal {
    OPENED_CREATE_TAB,
    MOMENT_CREATED,
    SETUP_SHEET_OPEN,
    QUICKADD_SAVED,
    CONTEXT_GROUP,
    CONTEXT_BUSINESS,
}

data class TourStep(
    val id: String,
    val title: String,
    val body: String,
    val target: TourTargetId? = null,
    /** Optional primary CTA that navigates after Next (e.g. open a tab). */
    val goToDestination: BottomDestination? = null,
    val advanceOn: Set<TourSignal> = emptySet(),
    val nextLabel: String = "Next",
)

data class WhereToLookHint(
    val title: String,
    val body: String,
    val goTo: BottomDestination,
)
