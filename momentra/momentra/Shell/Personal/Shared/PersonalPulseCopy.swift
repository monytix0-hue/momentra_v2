import Foundation

/// Human-language Pulse copy helpers — intelligence stays under the labels.
enum PersonalPulseCopy {
    struct SignalLine: Equatable {
        let direction: Direction
        let label: String
        enum Direction: String { case up, down, neutral }
    }

    static func statusBand(wellbeingScore: String?) -> String {
        guard let n = PersonalLifeOpsDerived.scoreNumber(wellbeingScore) else {
            return "Getting started"
        }
        if n >= 75 { return "Feeling strong" }
        if n >= 55 { return "Feeling steady" }
        if n >= 35 { return "Needs a little care" }
        return "Feeling strained"
    }

    static func heroSentence(
        family: PersonalPulseFamily,
        recoveryScore: String?,
        wellbeingScore: String?,
        moodState: String?,
        spendPairs: [(String, String)],
        todayLogCount: Int
    ) -> String {
        let recovery = PersonalLifeOpsDerived.scoreNumber(recoveryScore)
        let wellbeing = PersonalLifeOpsDerived.scoreNumber(wellbeingScore)
        let mood = moodState?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        switch family {
        case .lifeOperations:
            var parts: [String] = []
            if let recovery {
                parts.append(recovery >= 60 ? "Recovery is good." : "Recovery needs a little attention.")
            }
            if !spendPairs.isEmpty {
                parts.append("Spending is on the board.")
            } else if todayLogCount == 0 {
                parts.append("Nothing logged yet today.")
            } else if mood.isEmpty {
                parts.append("A quick mood check-in would help.")
            }
            if parts.isEmpty {
                parts.append(wellbeing.map { $0 >= 55 ? "Your day is taking shape." : "Start with one small log." } ?? "Start with spend, mood, or recovery.")
            }
            return parts.prefix(2).joined(separator: " ")
        case .futureBuilding:
            if todayLogCount == 0 { return "Log a milestone, progress, or learning to move the needle." }
            if let wellbeing, wellbeing >= 60 { return "Momentum is building. Keep the progress logs coming." }
            return "A small progress log today compounds."
        case .lifestyle:
            if todayLogCount == 0 { return "An experience or wellbeing check-in keeps lifestyle alive." }
            if let recovery, recovery >= 60 { return "Vitality looks solid. Protect a ritual today." }
            return "A quieter stretch — log one experience outside the usual."
        case .relationships:
            if todayLogCount == 0 { return "A quick connection log keeps people close." }
            return "Presence compounds. Log a connection or shared moment."
        }
    }

    static func shapedTodaySignals(
        family: PersonalPulseFamily,
        recoveryScore: String?,
        wellbeingScore: String?,
        rhythmScore: String?,
        attentionCount: Int?,
        moodState: String?,
        spendPairs: [(String, String)],
        helpingLabels: [String],
        hurtingLabels: [String]
    ) -> [SignalLine] {
        var lines: [SignalLine] = []
        let recovery = PersonalLifeOpsDerived.scoreNumber(recoveryScore)
        let wellbeing = PersonalLifeOpsDerived.scoreNumber(wellbeingScore)
        let rhythm = PersonalLifeOpsDerived.scoreNumber(rhythmScore)

        switch family {
        case .lifeOperations:
            if let recovery {
                lines.append(SignalLine(direction: recovery >= 55 ? .up : .down, label: "Recovery"))
            }
            if let rhythm {
                lines.append(SignalLine(direction: rhythm >= 55 ? .up : .down, label: "Discipline"))
            }
            if !spendPairs.isEmpty {
                lines.append(SignalLine(direction: .down, label: "Spending pressure"))
            } else if helpingLabels.contains(where: { $0.localizedCaseInsensitiveContains("spend") }) {
                lines.append(SignalLine(direction: .neutral, label: "Spending"))
            }
        case .futureBuilding:
            if let wellbeing {
                lines.append(SignalLine(direction: wellbeing >= 55 ? .up : .down, label: "Vision"))
            }
            if let recovery {
                lines.append(SignalLine(direction: recovery >= 55 ? .up : .down, label: "Growth"))
            }
            if let rhythm {
                lines.append(SignalLine(direction: rhythm >= 55 ? .up : .neutral, label: "Momentum"))
            }
        case .lifestyle:
            if let recovery {
                lines.append(SignalLine(direction: recovery >= 55 ? .up : .down, label: "Joy"))
            }
            if let wellbeing {
                lines.append(SignalLine(direction: wellbeing >= 55 ? .up : .down, label: "Fulfillment"))
            }
            if (attentionCount ?? 0) == 0 {
                lines.append(SignalLine(direction: .down, label: "Exploration"))
            } else {
                lines.append(SignalLine(direction: .up, label: "Exploration"))
            }
        case .relationships:
            if let recovery {
                lines.append(SignalLine(direction: recovery >= 55 ? .up : .down, label: "Care"))
            }
            if let wellbeing {
                lines.append(SignalLine(direction: wellbeing >= 55 ? .up : .down, label: "Trust"))
            }
            if let rhythm {
                lines.append(SignalLine(direction: rhythm >= 55 ? .up : .neutral, label: "Presence"))
            }
        }

        if lines.isEmpty {
            if !helpingLabels.isEmpty {
                lines.append(SignalLine(direction: .up, label: helpingLabels[0]))
            }
            if !hurtingLabels.isEmpty {
                lines.append(SignalLine(direction: .down, label: hurtingLabels[0]))
            }
        }
        if lines.isEmpty {
            let mood = moodState?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !mood.isEmpty {
                lines.append(SignalLine(direction: .neutral, label: "Mood · \(mood)"))
            } else {
                lines.append(SignalLine(direction: .neutral, label: "Waiting on today's first logs"))
            }
        }
        return Array(lines.prefix(4))
    }

    static func moneySpentLine(spendPairs: [(String, String)], format: (String) -> String) -> String {
        guard !spendPairs.isEmpty else { return "No spend logged" }
        return spendPairs.map { "\(format($0.1)) \($0.0)" }.joined(separator: " · ") + " spent"
    }
}
