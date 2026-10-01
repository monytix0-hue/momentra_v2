import Foundation

/// Presentation-shaped Memory contract (M4). Views render only this model.
struct PersonalMemorySummaryModel: Equatable {
    struct Hero: Equatable {
        let periodLabel: String
        let sentence: String?
        let memoryCount: Int
        let activityCount: Int
        let highlightCount: Int
    }

    struct HighlightItem: Equatable, Identifiable {
        var id: String { "\(title)-\(occurredAt)" }
        let title: String
        let occurredAt: String
    }

    struct Highlights: Equatable {
        let heading: String
        let items: [HighlightItem]
    }

    struct Pattern: Equatable {
        let title: String
        let body: String
    }

    struct PatternWhyItem: Equatable, Identifiable {
        var id: String { "\(kind)-\(label)" }
        let kind: String
        let label: String
        let occurredAt: String?
    }

    struct ReturnBehaviour: Equatable, Identifiable {
        var id: String { label }
        let label: String
        let strengthLabel: String?
    }

    struct Evolution: Equatable {
        let thenLabel: String
        let nowLabel: String
        let summary: String
    }

    struct EvolutionDetail: Equatable {
        let thenSummary: String
        let nowSummary: String
        let notes: [String]
    }

    struct ReliveMediaItem: Equatable, Identifiable {
        var id: String { "\(memoryId)-\(downloadUrl)" }
        let memoryId: String
        let title: String?
        let downloadUrl: String
    }

    let hero: Hero
    let highlights: Highlights
    let pattern: Pattern?
    let patternWhy: [PatternWhyItem]?
    let returnBehaviours: [ReturnBehaviour]
    let evolution: Evolution?
    let evolutionDetail: EvolutionDetail?
    let reliveMedia: [ReliveMediaItem]

    /// Map payload → presentation. Respects sectionQuality; never rescues empty sections.
    static func from(_ payload: APIClient.PersonalMemoryPayload) -> PersonalMemorySummaryModel {
        let sq = payload.sectionQuality ?? [:]
        func real(_ key: String) -> Bool {
            (sq[key] ?? "").uppercased() == "REAL_DATA"
        }

        let periodLabel = (payload.periodLabel?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap {
            $0.isEmpty ? nil : $0
        } ?? "This month"

        let counts = payload.counts
        let heroSentence: String? = {
            guard real("hero") else { return nil }
            let s = payload.heroSentence?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return s.isEmpty ? nil : s
        }()

        let hero = Hero(
            periodLabel: periodLabel,
            sentence: heroSentence,
            memoryCount: counts?.memories ?? payload.memoryCount ?? payload.items?.count ?? 0,
            activityCount: counts?.activities ?? 0,
            highlightCount: counts?.highlights ?? payload.highlights?.count ?? 0
        )

        let heading: String = {
            switch (payload.highlightsSource ?? "").uppercased() {
            case "MEMORY": return "Remember this?"
            case "MIXED": return "Moments worth revisiting"
            default: return "From your month"
            }
        }()

        let highlightItems: [HighlightItem] = {
            guard real("highlights") else { return [] }
            return (payload.highlights ?? []).prefix(5).compactMap { h in
                let t = h.title.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !t.isEmpty else { return nil }
                return HighlightItem(title: t, occurredAt: h.occurredAt)
            }
        }()

        let pattern: Pattern? = {
            guard real("pattern"), let p = payload.primaryPattern else { return nil }
            let title = p.title.trimmingCharacters(in: .whitespacesAndNewlines)
            let body = p.body.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !title.isEmpty else { return nil }
            return Pattern(title: title, body: body.isEmpty ? title : body)
        }()

        let patternWhy: [PatternWhyItem]? = {
            guard real("patternWhy"), let why = payload.patternWhy, !why.isEmpty else { return nil }
            let items = why.compactMap { w -> PatternWhyItem? in
                let label = w.label.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !label.isEmpty else { return nil }
                return PatternWhyItem(kind: w.kind, label: label, occurredAt: w.occurredAt)
            }
            return items.isEmpty ? nil : items
        }()

        let returns: [ReturnBehaviour] = {
            guard real("returnBehaviours") else { return [] }
            return (payload.returnBehaviours ?? []).prefix(4).compactMap { r in
                let label = r.label.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !label.isEmpty else { return nil }
                return ReturnBehaviour(label: label, strengthLabel: r.strengthLabel)
            }
        }()

        let evolution: Evolution? = {
            guard real("evolution"), let e = payload.evolution else { return nil }
            return Evolution(thenLabel: e.thenLabel, nowLabel: e.nowLabel, summary: e.summary)
        }()

        let evolutionDetail: EvolutionDetail? = {
            guard real("evolutionDetail"), let d = payload.evolutionDetail else { return nil }
            return EvolutionDetail(
                thenSummary: d.thenSummary,
                nowSummary: d.nowSummary,
                notes: d.notes ?? []
            )
        }()

        let reliveMedia: [ReliveMediaItem] = {
            guard real("relive") else { return [] }
            return (payload.reliveMedia ?? []).compactMap { m in
                let url = m.downloadUrl.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !url.isEmpty else { return nil }
                return ReliveMediaItem(memoryId: m.memoryId, title: m.title, downloadUrl: url)
            }
        }()

        return PersonalMemorySummaryModel(
            hero: hero,
            highlights: Highlights(heading: heading, items: highlightItems),
            pattern: pattern,
            patternWhy: patternWhy,
            returnBehaviours: returns,
            evolution: evolution,
            evolutionDetail: evolutionDetail,
            reliveMedia: reliveMedia
        )
    }
}
