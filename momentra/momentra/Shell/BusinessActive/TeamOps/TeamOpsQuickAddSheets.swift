import SwiftUI

enum TeamOpsQuickAddSheets {
    static func isTeamOpsKind(_ kind: BusinessQuickAddKind) -> Bool {
        switch kind {
        case .teamUpdate, .decision, .blocker, .meeting, .recognition, .approval,
             .milestone, .retrospective, .riskFlag, .activityLog, .poll, .memory:
            return true
        default:
            return false
        }
    }
}

struct TeamOpsGapQuickAddSheet: View {
    let kind: BusinessQuickAddKind
    var momentId: String? = nil
    var onClose: () -> Void
    var onSaved: () -> Void = {}

    var body: some View {
        ZStack(alignment: .top) {
            TeamOpsSheetTokens.sheetBg.ignoresSafeArea()
            VStack(spacing: 0) {
                Capsule()
                    .fill(TeamOpsSheetTokens.handle)
                    .frame(width: 48, height: 4)
                    .padding(.top, 12)
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        switch kind {
                        case .teamUpdate:
                            TeamOpsTeamUpdateForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .decision:
                            TeamOpsDecisionForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .blocker:
                            TeamOpsIssueForm(
                                momentId: momentId,
                                kind: .blocker,
                                accent: .red,
                                sheetTitle: "Flag Blocker",
                                explanation: "Surface a delivery blocker",
                                titleLabel: "Blocker",
                                titlePlaceholder: "Blocker title",
                                ctaLabel: "Flag Blocker",
                                footerHint: "Team will be notified",
                                errorFallback: "Could not flag blocker",
                                onClose: onClose,
                                onSaved: onSaved
                            )
                        case .meeting:
                            TeamOpsMeetingForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .recognition:
                            TeamOpsRecognitionForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .approval:
                            TeamOpsApprovalForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .milestone:
                            TeamOpsMilestoneForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .retrospective:
                            TeamOpsRetroForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .riskFlag:
                            TeamOpsRiskForm(
                                momentId: momentId,
                                onClose: onClose,
                                onSaved: onSaved
                            )
                        case .activityLog:
                            TeamOpsActivityForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .poll:
                            TeamOpsPollForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        case .memory:
                            TeamOpsMemoryForm(momentId: momentId, onClose: onClose, onSaved: onSaved)
                        default:
                            Text("Unsupported Team Ops command")
                                .font(.plusJakarta(size: 14))
                                .foregroundStyle(TeamOpsSheetTokens.muted)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 28)
                }
            }
        }
    }
}

// MARK: - Helpers

private struct TeamOpsFieldBlock<Content: View>: View {
    let label: String
    @ViewBuilder var content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            TeamOpsFieldLabel(text: label)
            content
        }
    }
}

private func teamOpsEmoji(for kind: BusinessQuickAddKind) -> String {
    kind.emoji
}

private func teamOpsHasMoment(_ momentId: String?) -> Bool {
    guard let momentId else { return false }
    return !momentId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
}

// MARK: - Team Update

private struct TeamOpsTeamUpdateForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var updateType = "Progress"
    @State private var title = ""
    @State private var details = ""
    @State private var visibility = "Team"
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo
    private let updateTypes = ["Progress", "Announcement", "Blocker Resolved"]
    private let visibilities = ["Team", "Company", "Private"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .teamUpdate),
                title: "Team Update",
                explanation: "Share progress with the team",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Update Type") {
                TeamOpsSegmentedControl(options: updateTypes, selected: $updateType, accent: accent)
            }
            TeamOpsFieldBlock(label: "Title") {
                TeamOpsTextField(value: $title, placeholder: "Update title")
            }
            TeamOpsFieldBlock(label: "Details") {
                TeamOpsTextField(value: $details, placeholder: "What should the team know…", minHeight: 96, singleLine: false)
            }
            TeamOpsFieldBlock(label: "Visibility") {
                TeamOpsChipRow(options: visibilities, selected: $visibility, accent: accent)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Share Update",
                enabled: teamOpsHasMoment(momentId) && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Update will be shared",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        var body = "Type: \(updateType)\nVisibility: \(visibility)\n"
        let trimmed = details.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { body += trimmed }
        do {
            _ = try await APIClient.shared.createBusinessUpdate(
                momentId: momentId,
                body: body,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Decision

private struct TeamOpsDecisionForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var decision = ""
    @State private var context = ""
    @State private var decidedBy = "You"
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var impact = "Engineering"
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.lavender
    private let decidedByOptions = ["You", "Lead", "Team"]
    private let impactAreas = ["Engineering", "Design", "Operations", "All"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .decision),
                title: "Log Decision",
                explanation: "Record a choice and its impact",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Decision") {
                TeamOpsTextField(value: $decision, placeholder: "What was decided")
            }
            TeamOpsFieldBlock(label: "Context") {
                TeamOpsTextField(value: $context, placeholder: "Why this decision was made…", minHeight: 80, singleLine: false)
            }
            TeamOpsFieldBlock(label: "Decided By") {
                TeamOpsDropdownField(value: decidedBy, options: decidedByOptions, onSelect: { decidedBy = $0 }, placeholder: "Select")
            }
            TeamOpsFieldBlock(label: "Date") {
                TeamOpsDateField(isoDate: $isoDate)
            }
            TeamOpsFieldBlock(label: "Impact") {
                TeamOpsChipRow(options: impactAreas, selected: $impact, accent: accent)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Log Decision",
                enabled: teamOpsHasMoment(momentId) && !decision.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Decision will be logged",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        var rationale = "Decided by: \(decidedBy)\nDate: \(isoDate)\nImpact: \(impact)\n"
        let trimmed = context.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { rationale += trimmed }
        do {
            _ = try await APIClient.shared.createDecision(
                momentId: momentId,
                title: decision.trimmingCharacters(in: .whitespacesAndNewlines),
                decisionText: trimmed.isEmpty ? decision.trimmingCharacters(in: .whitespacesAndNewlines) : trimmed,
                rationale: rationale
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Blocker / Risk

private struct TeamOpsIssueForm: View {
    var momentId: String?
    let kind: BusinessQuickAddKind
    let accent: TeamOpsSheetAccent
    let sheetTitle: String
    let explanation: String
    let titleLabel: String
    let titlePlaceholder: String
    let ctaLabel: String
    let footerHint: String
    let errorFallback: String
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var severity = "High"
    @State private var blockedItem = "Feature"
    @State private var owner = "You"
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var details = ""
    @State private var submitting = false
    @State private var error: String?

    private let severities = ["Critical", "High", "Medium", "Low"]
    private let blockedItems = ["Feature", "Release", "Dependency", "Sprint goal", "Other"]
    private let ownerOptions = ["You", "Lead", "Unassigned"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: kind),
                title: sheetTitle,
                explanation: explanation,
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: titleLabel) {
                TeamOpsTextField(value: $title, placeholder: titlePlaceholder)
            }
            TeamOpsFieldBlock(label: "Severity") {
                TeamOpsChipRow(options: severities, selected: $severity, accent: accent)
            }
            TeamOpsFieldBlock(label: "Blocked Item") {
                TeamOpsDropdownField(value: blockedItem, options: blockedItems, onSelect: { blockedItem = $0 }, placeholder: "Select item")
            }
            TeamOpsFieldBlock(label: "Owner") {
                TeamOpsDropdownField(value: owner, options: ownerOptions, onSelect: { owner = $0 }, placeholder: "Select owner")
            }
            TeamOpsFieldBlock(label: "Due date") {
                TeamOpsDateField(isoDate: $isoDate)
            }
            TeamOpsFieldBlock(label: "Details") {
                TeamOpsTextField(value: $details, placeholder: "Describe impact…", minHeight: 80, singleLine: false)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : ctaLabel,
                enabled: teamOpsHasMoment(momentId) && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: footerHint,
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        var parts = [
            "Blocked item: \(blockedItem)",
            "Owner: \(owner)",
            "Due: \(isoDate)",
        ]
        let trimmed = details.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { parts.append(trimmed) }
        do {
            _ = try await APIClient.shared.createBusinessIssue(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                description: parts.joined(separator: " · "),
                severity: severity.uppercased()
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription.isEmpty ? errorFallback : error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Meeting

private struct TeamOpsMeetingForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var timeHm: String = {
        let cal = Calendar.current
        let now = Date()
        return String(format: "%02d:%02d", cal.component(.hour, from: now), cal.component(.minute, from: now))
    }()
    @State private var notes = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .meeting),
                title: "Log Meeting",
                explanation: "Capture notes and next steps",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Title") {
                TeamOpsTextField(value: $title, placeholder: "Sprint review")
            }
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    TeamOpsFieldLabel(text: "Date")
                    TeamOpsDateField(isoDate: $isoDate)
                }
                .frame(maxWidth: .infinity)
                VStack(alignment: .leading, spacing: 8) {
                    TeamOpsFieldLabel(text: "Time")
                    TeamOpsTimeField(timeHm: $timeHm)
                }
                .frame(maxWidth: .infinity)
            }
            TeamOpsFieldBlock(label: "Notes") {
                TeamOpsTextField(value: $notes, placeholder: "Decisions and next steps…", minHeight: 96, singleLine: false)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Log Meeting",
                enabled: teamOpsHasMoment(momentId) && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Meeting will be logged",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let meetingAt = "\(isoDate)T\(timeHm):00.000Z"
        let trimmed = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            _ = try await APIClient.shared.createMeetingRecord(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                meetingAt: meetingAt,
                notes: trimmed.isEmpty ? nil : trimmed
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Recognition

private struct TeamOpsRecognitionForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var recipient = ""
    @State private var why = ""
    @State private var type = "Kudos"
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo
    private let recognitionTypes = ["Kudos", "Shoutout", "Award"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .recognition),
                title: "Recognition",
                explanation: "Celebrate a teammate win",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Recipient") {
                TeamOpsTextField(value: $recipient, placeholder: "Teammate name")
            }
            TeamOpsFieldBlock(label: "Why") {
                TeamOpsTextField(value: $why, placeholder: "What they did…", minHeight: 80, singleLine: false)
            }
            TeamOpsFieldBlock(label: "Type") {
                TeamOpsChipRow(options: recognitionTypes, selected: $type, accent: accent)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Give Recognition",
                enabled: teamOpsHasMoment(momentId)
                    && !recipient.trimmingCharacters(in: .whitespaces).isEmpty
                    && !why.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    && !submitting,
                loading: submitting,
                footerHint: "Recognition will be shared",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let who = recipient.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            _ = try await APIClient.shared.createRecognition(
                momentId: momentId,
                recipientName: who,
                recognitionType: type.uppercased(),
                whyText: why.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Approval

private struct TeamOpsApprovalForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var amountDisplay = ""
    @State private var urgency = "Normal"
    @State private var note = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo
    private let urgencies = ["Normal", "High", "Urgent"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .approval),
                title: "Request Approval",
                explanation: "Route a request for sign-off",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Request title") {
                TeamOpsTextField(value: $title, placeholder: "What needs sign-off")
            }
            TeamOpsFieldBlock(label: "Amount") {
                TeamOpsAmountField(displayValue: $amountDisplay)
            }
            TeamOpsFieldBlock(label: "Urgency") {
                TeamOpsChipRow(options: urgencies, selected: $urgency, accent: accent)
            }
            TeamOpsFieldBlock(label: "Note") {
                TeamOpsTextField(value: $note, placeholder: "Optional context", minHeight: 72, singleLine: false)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Request Approval",
                enabled: teamOpsHasMoment(momentId) && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Stakeholders will be notified",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let amountRaw = TeamOpsAmountFormat.strip(amountDisplay)
        let amount = amountRaw.isEmpty ? nil : amountRaw
        var noteParts = ["Urgency: \(urgency)"]
        let trimmedNote = note.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedNote.isEmpty { noteParts.append(trimmedNote) }
        do {
            _ = try await APIClient.shared.createBusinessApprovalRequest(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                amount: amount,
                currencyCode: "INR",
                note: noteParts.joined(separator: " · ")
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Milestone

private struct TeamOpsMilestoneForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var name = ""
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var status = "Planned"
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo
    private let statuses = ["Planned", "In Progress", "Done"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .milestone),
                title: "Add Milestone",
                explanation: "Mark a delivery checkpoint",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Milestone name") {
                TeamOpsTextField(value: $name, placeholder: "Ship v1.2")
            }
            TeamOpsFieldBlock(label: "Due date") {
                TeamOpsDateField(isoDate: $isoDate)
            }
            TeamOpsFieldBlock(label: "Status") {
                TeamOpsChipRow(options: statuses, selected: $status, accent: accent)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Add Milestone",
                enabled: teamOpsHasMoment(momentId) && !name.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Milestone will be tracked",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let apiStatus: String
        switch status {
        case "Planned": apiStatus = "PLANNED"
        case "In Progress": apiStatus = "ACTIVE"
        case "Done": apiStatus = "COMPLETED"
        default: apiStatus = "PLANNED"
        }
        let targetAt = "\(isoDate)T12:00:00.000Z"
        do {
            _ = try await APIClient.shared.createMilestone(
                momentId: momentId,
                title: name.trimmingCharacters(in: .whitespacesAndNewlines),
                targetAt: targetAt,
                status: apiStatus
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Retrospective

private struct TeamOpsRetroForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var wentWell = ""
    @State private var improve = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .retrospective),
                title: "Retrospective",
                explanation: "Review wins and improvements",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "What went well") {
                TeamOpsTextField(value: $wentWell, placeholder: "Wins…", minHeight: 80, singleLine: false)
            }
            TeamOpsFieldBlock(label: "Improve next") {
                TeamOpsTextField(value: $improve, placeholder: "What to improve…", minHeight: 80, singleLine: false)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Save Retrospective",
                enabled: teamOpsHasMoment(momentId)
                    && (!wentWell.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        || !improve.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    && !submitting,
                loading: submitting,
                footerHint: "Retrospective will be saved",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let wins = wentWell.trimmingCharacters(in: .whitespacesAndNewlines)
        let next = improve.trimmingCharacters(in: .whitespacesAndNewlines)
        do {
            _ = try await APIClient.shared.createRetrospective(
                momentId: momentId,
                wentWell: wins.isEmpty ? nil : wins,
                improveNext: next.isEmpty ? nil : next
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Activity Log

private struct TeamOpsActivityForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var owner = "You"
    @State private var category = "Delivery"
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo
    private let owners = ["You", "Lead", "Team"]
    private let categories = ["Delivery", "Decision", "Ops", "Other"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .activityLog),
                title: "Activity Log",
                explanation: "Track a team action",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Activity title") {
                TeamOpsTextField(value: $title, placeholder: "What happened")
            }
            TeamOpsFieldBlock(label: "Owner") {
                TeamOpsDropdownField(value: owner, options: owners, onSelect: { owner = $0 }, placeholder: "Select owner")
            }
            TeamOpsFieldBlock(label: "Category") {
                TeamOpsChipRow(options: categories, selected: $category, accent: accent)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Log Activity",
                enabled: teamOpsHasMoment(momentId) && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Activity will be logged",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createActivityLogEntry(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                ownerLabel: owner,
                categoryCode: category.uppercased()
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Poll

private struct TeamOpsPollForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var question = ""
    @State private var optionA = ""
    @State private var optionB = ""
    @State private var optionC = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .poll),
                title: "Create Poll",
                explanation: "Gather quick team input",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Question") {
                TeamOpsTextField(value: $question, placeholder: "What should we decide?")
            }
            TeamOpsFieldBlock(label: "Option A") {
                TeamOpsTextField(value: $optionA, placeholder: "First option")
            }
            TeamOpsFieldBlock(label: "Option B") {
                TeamOpsTextField(value: $optionB, placeholder: "Second option")
            }
            TeamOpsFieldBlock(label: "Option C") {
                TeamOpsTextField(value: $optionC, placeholder: "Optional")
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Create Poll",
                enabled: teamOpsHasMoment(momentId)
                    && !question.trimmingCharacters(in: .whitespaces).isEmpty
                    && !optionA.trimmingCharacters(in: .whitespaces).isEmpty
                    && !optionB.trimmingCharacters(in: .whitespaces).isEmpty
                    && !submitting,
                loading: submitting,
                footerHint: "Poll will go live",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let options = [optionA, optionB, optionC]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        do {
            _ = try await APIClient.shared.createPoll(
                momentId: momentId,
                question: question.trimmingCharacters(in: .whitespacesAndNewlines),
                options: options
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Memory

private struct TeamOpsMemoryForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var bodyText = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.indigo

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .memory),
                title: "Save to Memory",
                explanation: "Capture a learning for the playbook",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Title") {
                TeamOpsTextField(value: $title, placeholder: "Memory title")
            }
            TeamOpsFieldBlock(label: "Body") {
                TeamOpsTextField(value: $bodyText, placeholder: "What should we remember?", minHeight: 96, singleLine: false)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Save to Memory",
                enabled: teamOpsHasMoment(momentId)
                    && !title.trimmingCharacters(in: .whitespaces).isEmpty
                    && !bodyText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    && !submitting,
                loading: submitting,
                footerHint: "Logged under team memory",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        do {
            _ = try await APIClient.shared.createBusinessMemory(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                body: bodyText.trimmingCharacters(in: .whitespacesAndNewlines)
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription
        }
        submitting = false
    }
}

// MARK: - Risk Flag

private struct TeamOpsRiskForm: View {
    var momentId: String?
    var onClose: () -> Void
    var onSaved: () -> Void

    @State private var title = ""
    @State private var severity = "High"
    @State private var blockedItem = "Feature"
    @State private var owner = "You"
    @State private var isoDate = SetupDateTimeUtils.localDateString(from: Date())
    @State private var details = ""
    @State private var submitting = false
    @State private var error: String?

    private let accent = TeamOpsSheetAccent.red
    private let severities = ["Critical", "High", "Medium", "Low"]
    private let blockedItems = ["Feature", "Release", "Dependency", "Sprint goal", "Other"]
    private let ownerOptions = ["You", "Lead", "Unassigned"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            TeamOpsSheetHeader(
                emoji: teamOpsEmoji(for: .riskFlag),
                title: "Risk Flag",
                explanation: "Raise a delivery risk early",
                accent: accent,
                onClose: onClose
            )
            TeamOpsFieldBlock(label: "Risk") {
                TeamOpsTextField(value: $title, placeholder: "Risk title")
            }
            TeamOpsFieldBlock(label: "Severity") {
                TeamOpsChipRow(options: severities, selected: $severity, accent: accent)
            }
            TeamOpsFieldBlock(label: "Blocked Item") {
                TeamOpsDropdownField(value: blockedItem, options: blockedItems, onSelect: { blockedItem = $0 }, placeholder: "Select item")
            }
            TeamOpsFieldBlock(label: "Owner") {
                TeamOpsDropdownField(value: owner, options: ownerOptions, onSelect: { owner = $0 }, placeholder: "Select owner")
            }
            TeamOpsFieldBlock(label: "Due date") {
                TeamOpsDateField(isoDate: $isoDate)
            }
            TeamOpsFieldBlock(label: "Details") {
                TeamOpsTextField(value: $details, placeholder: "Describe impact…", minHeight: 80, singleLine: false)
            }
            if let error {
                Text(error).font(.plusJakarta(size: 12)).foregroundStyle(TeamOpsSheetTokens.error)
            }
            TeamOpsPrimaryCta(
                label: submitting ? "Saving…" : "Flag Risk",
                enabled: teamOpsHasMoment(momentId) && !title.trimmingCharacters(in: .whitespaces).isEmpty && !submitting,
                loading: submitting,
                footerHint: "Risk will be flagged",
                accent: accent
            ) { Task { await submit() } }
        }
    }

    private func submit() async {
        guard let momentId else { return }
        submitting = true
        error = nil
        let impactApi: String
        switch severity {
        case "Critical": impactApi = "CRITICAL"
        case "High": impactApi = "HIGH"
        case "Medium": impactApi = "MEDIUM"
        default: impactApi = "LOW"
        }
        var parts = [
            "Blocked item: \(blockedItem)",
            "Owner: \(owner)",
            "Due: \(isoDate)",
        ]
        let trimmed = details.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { parts.append(trimmed) }
        do {
            _ = try await APIClient.shared.createRisk(
                momentId: momentId,
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                description: parts.joined(separator: " · "),
                likelihood: "MEDIUM",
                impact: impactApi
            )
            onSaved(); onClose()
        } catch {
            self.error = error.localizedDescription.isEmpty ? "Could not flag risk" : error.localizedDescription
        }
        submitting = false
    }
}
