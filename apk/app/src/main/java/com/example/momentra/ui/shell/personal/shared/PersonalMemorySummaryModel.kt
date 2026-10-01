package com.example.momentra.ui.shell.personal.shared

import com.example.momentra.data.api.PersonalMemoryDto

/**
 * Presentation-shaped Memory contract (M4). Views render only this model.
 * Mapper respects sectionQuality and never rescues empty server sections.
 */
data class PersonalMemorySummaryModel(
    val hero: Hero,
    val highlights: Highlights,
    val pattern: Pattern?,
    val patternWhy: List<PatternWhyItem>?,
    val returnBehaviours: List<ReturnBehaviour>,
    val evolution: Evolution?,
    val evolutionDetail: EvolutionDetail?,
    val reliveMedia: List<ReliveMediaItem> = emptyList(),
) {
    data class Hero(
        val periodLabel: String,
        val sentence: String?,
        val memoryCount: Int,
        val activityCount: Int,
        val highlightCount: Int,
    )

    data class HighlightItem(
        val title: String,
        val occurredAt: String,
    )

    data class Highlights(
        val heading: String,
        val items: List<HighlightItem>,
    )

    data class Pattern(
        val title: String,
        val body: String,
    )

    data class PatternWhyItem(
        val kind: String,
        val label: String,
        val occurredAt: String?,
    )

    data class ReturnBehaviour(
        val label: String,
        val strengthLabel: String?,
    )

    data class Evolution(
        val thenLabel: String,
        val nowLabel: String,
        val summary: String,
    )

    data class EvolutionDetail(
        val thenSummary: String,
        val nowSummary: String,
        val notes: List<String>,
    )

    data class ReliveMediaItem(
        val memoryId: String,
        val title: String?,
        val downloadUrl: String,
    )

    companion object {
        fun from(payload: PersonalMemoryDto): PersonalMemorySummaryModel {
            val sq = payload.sectionQuality
            fun real(key: String): Boolean =
                sq[key].equals("REAL_DATA", ignoreCase = true)

            val periodLabel = payload.periodLabel.trim().ifEmpty { "This month" }
            val heroSentence = if (real("hero")) {
                payload.heroSentence?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }

            val hero = Hero(
                periodLabel = periodLabel,
                sentence = heroSentence,
                memoryCount = payload.counts?.memories ?: payload.memoryCount,
                activityCount = payload.counts?.activities ?: 0,
                highlightCount = payload.counts?.highlights ?: payload.highlights.size,
            )

            val heading = when (payload.highlightsSource.uppercase()) {
                "MEMORY" -> "Remember this?"
                "MIXED" -> "Moments worth revisiting"
                else -> "From your month"
            }

            val highlightItems = if (real("highlights")) {
                payload.highlights
                    .asSequence()
                    .mapNotNull { h ->
                        val t = h.title.trim()
                        if (t.isEmpty()) null else HighlightItem(t, h.occurredAt)
                    }
                    .take(5)
                    .toList()
            } else {
                emptyList()
            }

            val pattern = if (real("pattern")) {
                payload.primaryPattern?.let { p ->
                    val title = p.title.trim()
                    if (title.isEmpty()) null
                    else Pattern(title = title, body = p.body.trim().ifEmpty { title })
                }
            } else {
                null
            }

            val patternWhy = if (real("patternWhy")) {
                payload.patternWhy
                    ?.mapNotNull { w ->
                        val label = w.label.trim()
                        if (label.isEmpty()) null
                        else PatternWhyItem(w.kind, label, w.occurredAt)
                    }
                    ?.takeIf { it.isNotEmpty() }
            } else {
                null
            }

            val returns = if (real("returnBehaviours")) {
                payload.returnBehaviours
                    .mapNotNull { r ->
                        val label = r.label.trim()
                        if (label.isEmpty()) null
                        else ReturnBehaviour(label, r.strengthLabel)
                    }
                    .take(4)
            } else {
                emptyList()
            }

            val evolution = if (real("evolution")) {
                payload.evolution?.let {
                    Evolution(it.thenLabel, it.nowLabel, it.summary)
                }
            } else {
                null
            }

            val evolutionDetail = if (real("evolutionDetail")) {
                payload.evolutionDetail?.let {
                    EvolutionDetail(it.thenSummary, it.nowSummary, it.notes)
                }
            } else {
                null
            }

            val reliveMedia = if (real("relive")) {
                payload.reliveMedia.mapNotNull { m ->
                    val url = m.downloadUrl.trim()
                    if (url.isEmpty()) null
                    else ReliveMediaItem(m.memoryId, m.title, url)
                }
            } else {
                emptyList()
            }

            return PersonalMemorySummaryModel(
                hero = hero,
                highlights = Highlights(heading, highlightItems),
                pattern = pattern,
                patternWhy = patternWhy,
                returnBehaviours = returns,
                evolution = evolution,
                evolutionDetail = evolutionDetail,
                reliveMedia = reliveMedia,
            )
        }
    }
}
