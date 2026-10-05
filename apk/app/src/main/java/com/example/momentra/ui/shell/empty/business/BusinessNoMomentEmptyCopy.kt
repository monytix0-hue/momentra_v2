package com.example.momentra.ui.shell.empty.business

/**
 * Copy for a company that has no active Business moment.
 * Sample activity and numeric placeholders must not look like live data.
 */
object BusinessNoMomentEmptyCopy {
    val momentsSampleRows: List<Pair<String, String>> = emptyList()

    const val ABSENT = "Not set up"

    val forbiddenSampleTitles = listOf(
        "You bought supplies",
        "A customer paid",
        "You saved a receipt",
    )
}
