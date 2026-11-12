import SwiftUI

/// Kitchen ticket — Figma `RST-QA-KOT · Send` `2449:29655`, `· Fire` `2449:29769`,
/// `· Reprint` `2449:29859`. Send posts `ORDER` (PLACED); Fire PATCHes the same event
/// (`revisesEventId`); Reprint shares a client re-render and never allocates a new KOT id.
struct RestaurantKotSheet: View {
    let theme: OperatingTheme
    @ObservedObject var model: BusinessOperatingModel

    @Environment(\.dismiss) private var dismiss

    private enum KotTab: String, CaseIterable, Identifiable {
        case send = "Send"
        case fire = "Fire"
        case reprint = "Reprint"
        var id: String { rawValue }
    }

    private enum KotChannel: String, CaseIterable, Identifiable {
        case inDining = "In-dining"
        case cloud = "Cloud"
        case takeaway = "Takeaway"
        var id: String { rawValue }
    }

    @State private var tab: KotTab = .send
    @State private var channel: KotChannel = .inDining
    @State private var ticketId: String?
    @State private var stationFilter = "All stations"
    @State private var dishQuery = ""
    @State private var guest = ""
    @State private var note = ""
    @State private var tableNumber = ""
    @State private var covers = ""
    @State private var platform = "Zomato"
    @State private var platformOrderId = ""
    @State private var reprintReason = "Kitchen printer jam"
    @State private var assignee = ""
    @State private var lines: [KotLine] = []
    @State private var saving = false
    @State private var moreOpen = false
    @State private var draftKotKey = "KOT-\(Int(Date().timeIntervalSince1970) % 100_000)"

    private var masterSource: [OperatingMasterPayload] {
        model.masters.isEmpty ? (model.projection?.masters ?? []) : model.masters
    }

    private var stations: [OperatingMasterPayload] {
        masterSource
            .filter { ["STATION", "LOCATION", "KITCHEN_STATION"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var menuDishes: [OperatingMasterPayload] {
        masterSource
            .filter { ["MENU_DISH", "PRODUCT", "ITEM", "SKU", "DISH", "MENU_ITEM"].contains($0.kind.uppercased()) }
            .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    private var tickets: [OperatingActivityPayload] {
        (model.projection?.activity ?? [])
            .filter { $0.eventType.uppercased() == "ORDER" }
            .filter {
                let status = ($0.status ?? "").uppercased()
                return status != "DONE" && status != "CANCELLED" && status != "COMPLETED"
            }
            .sorted { $0.occurredAt > $1.occurredAt }
    }

    private var selectedTicket: OperatingActivityPayload? {
        tickets.first { $0.id == ticketId }
    }

    private var fireTarget: String? {
        nextFireStatus(selectedTicket?.status)
    }

    private var stationChipOptions: [String] {
        switch tab {
        case .send:
            var names = ["All stations"]
            let fromMasters = stations.map(\.name)
            if fromMasters.isEmpty {
                names += ["Tandoor", "Hot", "Cold", "Bar"]
            } else {
                names += fromMasters
            }
            if channel == .cloud, !names.contains("Cloud queue") {
                names.append("Cloud queue")
            }
            if channel == .takeaway || channel == .cloud, !names.contains("Pack") {
                names.append("Pack")
            }
            return names
        case .fire:
            var names = ["Held only"]
            let fromMasters = stations.map(\.name)
            if fromMasters.isEmpty {
                names += ["Hot", "Tandoor"]
            } else {
                names += Array(fromMasters.prefix(4))
            }
            return names
        case .reprint:
            switch channel {
            case .cloud:
                return ["Cloud queue", "Hot", "Pack"]
            case .takeaway:
                return ["Pack", "Hot", "Bar"]
            case .inDining:
                var names = ["All stations"]
                let fromMasters = stations.map(\.name)
                names += fromMasters.isEmpty ? ["Tandoor", "Hot", "Bar"] : fromMasters
                return names
            }
        }
    }

    private var visibleLines: [KotLine] {
        let source: [KotLine]
        switch tab {
        case .send:
            source = lines
        case .fire:
            let heldDraft = lines.filter { $0.course == .hold }
            source = heldDraft.isEmpty
                ? parseTicketLines(selectedTicket).filter { $0.course == .hold }
                : heldDraft
        case .reprint:
            if !lines.isEmpty {
                source = lines.map { line in
                    var copy = line
                    copy.course = .now
                    return copy
                }
            } else {
                source = parseTicketLines(selectedTicket).map { line in
                    var copy = line
                    copy.course = .now
                    return copy
                }
            }
        }
        switch stationFilter {
        case "All stations", "Held only", "Cloud queue":
            return source
        default:
            return source.filter {
                $0.station.caseInsensitiveCompare(stationFilter) == .orderedSame
            }
        }
    }

    private var canSubmit: Bool {
        switch tab {
        case .send:
            return !lines.isEmpty
        case .fire:
            return selectedTicket != nil && fireTarget != nil
        case .reprint:
            return selectedTicket != nil || !lines.isEmpty
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                EmberGrabber()
                EmberSheetHeader(
                    title: headerTitle,
                    subtitle: headerSubtitle,
                    onClose: { dismiss() }
                )

                EmberChipRow(
                    options: KotTab.allCases.map(\.rawValue),
                    selection: Binding(
                        get: { tab.rawValue },
                        set: { raw in
                            if let next = KotTab(rawValue: raw) {
                                applyTabChange(next)
                            }
                        }
                    )
                )

                EmberChipRow(
                    options: KotChannel.allCases.map(\.rawValue),
                    selection: Binding(
                        get: { channel.rawValue },
                        set: { raw in
                            if let next = KotChannel(rawValue: raw) {
                                applyChannelChange(next)
                            }
                        }
                    ),
                    style: .channel
                )

                contextChips

                EmberTicketHero(
                    eyebrow: heroEyebrow,
                    ticketId: heroKotId,
                    headline: heroHeadline,
                    detail: heroDetail
                )

                EmberChipRow(
                    options: stationChipOptions,
                    selection: $stationFilter
                )

                if tab == .send {
                    sendComposer
                } else {
                    ticketPicker
                }

                linesSection

                EmberMetaRows(rows: metaRows)

                EmberMoreDetails(hint: moreHint, open: $moreOpen) {
                    moreFields
                }

                EmberWhyBox(text: whyCopy)

                EmberStickyFooter(
                    title: footerTitle,
                    caption: footerCaption,
                    disabled: !canSubmit,
                    loading: saving,
                    action: { Task { await submit() } }
                )
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 28)
        }
        .background(EmberSheetChrome.sheet)
        .task {
            if ticketId == nil { ticketId = tickets.first?.id }
            if stationFilter.isEmpty || !stationChipOptions.contains(stationFilter) {
                stationFilter = stationChipOptions.first ?? "All stations"
            }
            if model.masters.isEmpty { await model.loadMasters() }
        }
        .onChange(of: tab) { _, _ in
            if !stationChipOptions.contains(stationFilter) {
                stationFilter = stationChipOptions.first ?? "All stations"
            }
        }
        .onChange(of: channel) { _, _ in
            if !stationChipOptions.contains(stationFilter) {
                stationFilter = stationChipOptions.first ?? "All stations"
            }
        }
    }

    // MARK: - Copy

    private var headerTitle: String {
        switch tab {
        case .send: return "Send KOT"
        case .fire: return "Fire course"
        case .reprint: return "Reprint KOT"
        }
    }

    private var headerSubtitle: String {
        switch tab {
        case .send: return "Kitchen ticket · no revenue"
        case .fire: return "Release held lines to kitchen"
        case .reprint: return "Re-print kitchen ticket · no new ticket id"
        }
    }

    private var whyCopy: String {
        switch tab {
        case .send:
            return "Send KOT never posts revenue or stock. Held lines stay on ticket until Fire. Recipe deduction excluded in v5."
        case .fire:
            return "Course fire is operational state. Does not create revenue, collection, or stock movement."
        case .reprint:
            return "Reprint and cloud ticket sync are operational. Revenue only when bill posts; platform settle via Collection."
        }
    }

    private var footerTitle: String {
        if saving {
            switch tab {
            case .send: return "Sending…"
            case .fire: return "Firing…"
            case .reprint: return "Sharing…"
            }
        }
        switch tab {
        case .send: return "Send to kitchen"
        case .fire: return "Fire to kitchen"
        case .reprint: return "Reprint ticket"
        }
    }

    private var footerCaption: String {
        switch tab {
        case .send: return "KOT is operational only — bill posts revenue later"
        case .fire: return "Fire updates ticket state only — never posts a sale"
        case .reprint: return "Reprint keeps same KOT id — never duplicates the order"
        }
    }

    private var moreHint: String {
        switch tab {
        case .send: return "Guest · Note · Assignee · Print"
        case .fire: return "Assignee · Station alert"
        case .reprint: return "Reprint reason · Settlement"
        }
    }

    // MARK: - Hero / context

    private var heroEyebrow: String {
        switch (tab, channel) {
        case (.reprint, .cloud): return "Cloud ticket"
        default: return "Ticket"
        }
    }

    private var heroKotId: String {
        if let key = selectedTicket?.clientKey?.trimmingCharacters(in: .whitespaces), !key.isEmpty {
            return key
        }
        return draftKotKey
    }

    private var heroHeadline: String {
        switch tab {
        case .send:
            switch channel {
            case .inDining:
                let table = tableNumber.trimmingCharacters(in: .whitespaces)
                return table.isEmpty ? "New ticket" : "Table \(table)"
            case .cloud:
                let id = platformOrderId.trimmingCharacters(in: .whitespaces)
                return id.isEmpty ? platform : id
            case .takeaway:
                return "Pickup"
            }
        case .fire:
            let table = (selectedTicket?.tableNumber ?? tableNumber)
                .trimmingCharacters(in: .whitespaces)
            return table.isEmpty ? "Fire course" : "Fire · Table \(table)"
        case .reprint:
            switch channel {
            case .cloud:
                if let ticket = selectedTicket {
                    if let platformRef = extractPlatformRef(ticket.note ?? "") {
                        return platformRef
                    }
                    let counter = ticket.counterpartyName?.trimmingCharacters(in: .whitespaces) ?? ""
                    if !counter.isEmpty, counter != platform { return counter }
                }
                let id = platformOrderId.trimmingCharacters(in: .whitespaces)
                return id.isEmpty ? heroKotId : id
            case .inDining:
                let table = (selectedTicket?.tableNumber ?? tableNumber)
                    .trimmingCharacters(in: .whitespaces)
                return table.isEmpty ? heroKotId : "Table \(table)"
            case .takeaway:
                if let title = selectedTicket?.title, !title.isEmpty { return title }
                return "Pickup ticket"
            }
        }
    }

    private var heroDetail: String {
        switch tab {
        case .send:
            switch channel {
            case .inDining:
                let cover = covers.trimmingCharacters(in: .whitespaces)
                let coverBit = cover.isEmpty ? nil : "\(cover) covers"
                let stationBit = stationFilter == "All stations" ? "Hot pass + Tandoor" : stationFilter
                return [coverBit, "Dinner", stationBit].compactMap { $0 }.joined(separator: " · ")
            case .cloud:
                let count = lines.isEmpty ? nil : "\(lines.count) item\(lines.count == 1 ? "" : "s")"
                return [platform, count, "Pack for rider"].compactMap { $0 }.joined(separator: " · ")
            case .takeaway:
                let count = lines.isEmpty ? nil : "\(lines.count) item\(lines.count == 1 ? "" : "s")"
                return [count, "Ready desk"].compactMap { $0 }.joined(separator: " · ")
            }
        case .fire:
            let held = visibleLines.count
            let station = selectedTicket?.locationName ?? "Hot pass"
            return "\(held) held line\(held == 1 ? "" : "s") ready · \(station)"
        case .reprint:
            if let ticket = selectedTicket {
                let bits = [
                    ticket.channelName,
                    ticket.locationName,
                    ticket.status,
                ].compactMap { $0?.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
                return bits.isEmpty ? "Same KOT id · kitchen reprint" : bits.joined(separator: " · ")
            }
            return channel == .cloud
                ? "\(platform) · Pack for rider"
                : "Same KOT id · kitchen reprint"
        }
    }

    @ViewBuilder
    private var contextChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                switch channel {
                case .inDining:
                    EmberModeChip(
                        text: tableCoversLabel,
                        selected: true,
                        style: .channel
                    ) {}
                    EmberModeChip(
                        text: tab == .fire ? "Held → Now" : "Course · Now + Hold",
                        selected: false
                    ) {}
                case .cloud:
                    EmberModeChip(
                        text: cloudContextLabel,
                        selected: true,
                        style: .channel
                    ) {}
                    EmberModeChip(text: "Platform ticket", selected: false) {}
                case .takeaway:
                    EmberModeChip(text: "Pickup desk", selected: true, style: .channel) {}
                    EmberModeChip(text: "Ready by", selected: false, locked: true) {}
                }
                EmberModeChip(text: "GST on", selected: false, locked: true) {}
            }
        }
    }

    private var tableCoversLabel: String {
        let table = tableNumber.trimmingCharacters(in: .whitespaces)
        let cover = covers.trimmingCharacters(in: .whitespaces)
        if table.isEmpty && cover.isEmpty { return "Table · covers" }
        if cover.isEmpty { return "Table \(table)" }
        if table.isEmpty { return "\(cover) covers" }
        return "Table \(table) · \(cover) covers"
    }

    private var cloudContextLabel: String {
        let order = platformOrderId.trimmingCharacters(in: .whitespaces)
        if order.isEmpty { return platform }
        return "\(platform) · \(order)"
    }

    // MARK: - Composer / picker

    private var sendComposer: some View {
        VStack(alignment: .leading, spacing: 8) {
            EmberFieldLabel(text: "Add from menu")
            EmberScanField(
                placeholder: menuDishes.isEmpty
                    ? "No saved dish — add under Menu"
                    : "Butter chicken · search menu",
                query: $dishQuery,
                disabled: menuDishes.isEmpty,
                action: addDish
            )
            if !dishQuery.trimmingCharacters(in: .whitespaces).isEmpty {
                dishMatches
            }
            if menuDishes.isEmpty {
                EmberEmptyCard(
                    title: "Menu not set up",
                    bodyText: "Add dishes under Menu first. Send KOT posts ORDER lines — never a sale."
                )
            }
        }
    }

    private var ticketPicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            EmberFieldLabel(text: tab == .fire ? "Open tickets" : "Reprint from")
            if tickets.isEmpty {
                EmberEmptyCard(
                    title: "No open tickets",
                    bodyText: tab == .fire
                        ? "Send a kitchen ticket first. Fire only revises an existing ORDER."
                        : "Send a kitchen ticket first. Reprint keeps that same KOT id."
                )
            } else {
                ForEach(tickets) { ticket in
                    Button {
                        ticketId = ticket.id
                        hydrateFromTicket(ticket)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(ticket.title.isEmpty ? "Kitchen ticket" : ticket.title)
                                    .font(.plusJakarta(size: 13, weight: .semibold))
                                    .foregroundStyle(EmberSheetChrome.textPrimary)
                                    .lineLimit(1)
                                Text(ticketMeta(ticket))
                                    .font(.plusJakarta(size: 11))
                                    .foregroundStyle(EmberSheetChrome.textBody)
                                    .lineLimit(1)
                            }
                            Spacer()
                            if ticket.id == ticketId {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(EmberSheetChrome.ember)
                            }
                        }
                        .padding(12)
                        .background(
                            ticket.id == ticketId
                                ? EmberSheetChrome.ember.opacity(0.12)
                                : EmberSheetChrome.chip
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(
                                    ticket.id == ticketId
                                        ? EmberSheetChrome.ember.opacity(0.5)
                                        : EmberSheetChrome.hairline,
                                    lineWidth: 1
                                )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            if tab == .fire, let selectedTicket, fireTarget == nil {
                EmberEmptyCard(
                    title: "Already advanced",
                    bodyText: "This ticket is \(selectedTicket.status ?? "ready"). Fire only moves Placed → Preparing → Ready."
                )
            }
        }
    }

    @ViewBuilder
    private var dishMatches: some View {
        let matches = menuDishes
            .filter { $0.name.localizedCaseInsensitiveContains(dishQuery.trimmingCharacters(in: .whitespaces)) }
            .prefix(4)
        if !matches.isEmpty {
            VStack(spacing: 6) {
                ForEach(Array(matches)) { dish in
                    Button {
                        add(dish)
                        dishQuery = ""
                    } label: {
                        Text(dish.name)
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(EmberSheetChrome.textPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 9)
                            .background(EmberSheetChrome.chip)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Lines

    @ViewBuilder
    private var linesSection: some View {
        let rows = visibleLines
        if rows.isEmpty {
            EmberEmptyCard(
                title: emptyLinesTitle,
                bodyText: emptyLinesBody
            )
        } else {
            VStack(spacing: 8) {
                ForEach(rows) { line in
                    kotLineCard(line)
                }
            }
        }
    }

    private var emptyLinesTitle: String {
        switch tab {
        case .send: return "Nothing on the ticket"
        case .fire: return "No held lines"
        case .reprint: return "No lines to reprint"
        }
    }

    private var emptyLinesBody: String {
        switch tab {
        case .send: return "Add dishes, set Now / Hold, then send to kitchen. This is not a bill."
        case .fire: return "Held lines appear here after Send. Fire releases them — never posts revenue."
        case .reprint: return "Pick an open ticket above. Reprint keeps the same KOT id."
        }
    }

    private func kotLineCard(_ line: KotLine) -> some View {
        let isHold = line.course == .hold
        let courseLabel: String = {
            switch tab {
            case .send: return line.course.rawValue
            case .fire: return "Fire now"
            case .reprint: return "Sent"
            }
        }()
        let courseStyle: EmberChipStyle = {
            switch tab {
            case .reprint: return .courseNow
            case .fire: return .courseHold
            case .send: return line.course == .now ? .courseNow : .courseHold
            }
        }()
        return VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text(line.name)
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("×\(line.quantity)")
                    .font(.plusJakarta(size: 14, weight: .bold))
                    .foregroundStyle(EmberSheetChrome.gold)
                if tab == .send {
                    Button {
                        removeLine(line.id)
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(EmberSheetChrome.textBody)
                    }
                    .buttonStyle(.plain)
                }
            }
            HStack(alignment: .center, spacing: 6) {
                Text(line.meta.isEmpty ? defaultMeta(for: line) : line.meta)
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(EmberSheetChrome.textBody)
                    .frame(maxWidth: .infinity, alignment: .leading)
                EmberModeChip(text: line.station, selected: false) {
                    if tab == .send { cycleStation(line.id) }
                }
                EmberModeChip(
                    text: courseLabel,
                    selected: true,
                    style: courseStyle
                ) {
                    if tab == .send { toggleCourse(line.id) }
                }
            }
            if tab == .send {
                EmberStepper(
                    quantity: line.quantity,
                    onDecrement: { bumpQuantity(line.id, by: -1) },
                    onIncrement: { bumpQuantity(line.id, by: 1) }
                )
            }
        }
        .padding(12)
        .background(EmberSheetChrome.chip)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    isHold
                        ? EmberSheetChrome.amber.opacity(0.40)
                        : EmberSheetChrome.ember.opacity(0.30),
                    lineWidth: 1
                )
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(
            color: isHold ? EmberSheetChrome.amber.opacity(0.20) : .clear,
            radius: 6,
            y: 2
        )
    }

    private func defaultMeta(for line: KotLine) -> String {
        switch tab {
        case .send:
            return line.course == .hold ? "Hold until guest ready" : "Send with ticket"
        case .fire:
            return "Was Hold · fire with mains"
        case .reprint:
            return "Sent · reprint same id"
        }
    }

    // MARK: - Meta / more

    private var metaRows: [(label: String, value: String)] {
        switch tab {
        case .send:
            return [
                ("Print to", printTarget),
                ("Assignee", assignee.isEmpty ? "—" : assignee),
                ("Linked bill", "Open · not posted"),
            ]
        case .fire:
            let sent = lines.filter { $0.course == .now }.map(\.name)
            let sentLabel = sent.isEmpty
                ? (selectedTicket?.title.isEmpty == false ? selectedTicket!.title : "Earlier courses")
                : sent.joined(separator: " · ")
            return [
                ("Already sent", sentLabel),
                ("Station alert", "Hot pass buzz"),
                ("Linked bill", "Still open"),
            ]
        case .reprint:
            return [
                ("Original sent", originalSentLabel),
                ("Reprint reason", reprintReason.isEmpty ? "Kitchen printer jam" : reprintReason),
                (
                    "Settlement",
                    channel == .cloud
                        ? "Platform AR · Collection later"
                        : "Bill posts revenue separately"
                ),
            ]
        }
    }

    private var printTarget: String {
        if stationFilter == "All stations" || stationFilter == "Held only" {
            return "Kitchen printer · Hot pass"
        }
        if stationFilter == "Cloud queue" || stationFilter == "Pack" {
            return "Cloud queue · Pack"
        }
        return "Kitchen printer · \(stationFilter)"
    }

    private var originalSentLabel: String {
        if let ticket = selectedTicket {
            let time = String(ticket.occurredAt.prefix(16)).replacingOccurrences(of: "T", with: " ")
            let chef = assignee.isEmpty ? "Kitchen" : assignee
            return "\(time) · \(chef)"
        }
        return assignee.isEmpty ? "—" : assignee
    }

    @ViewBuilder
    private var moreFields: some View {
        VStack(alignment: .leading, spacing: 10) {
            if tab == .send {
                if channel == .inDining {
                    HStack(spacing: 8) {
                        VStack(alignment: .leading, spacing: 6) {
                            EmberFieldLabel(text: "Table")
                            EmberTextField(placeholder: "12", text: $tableNumber, keyboard: .numbersAndPunctuation)
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            EmberFieldLabel(text: "Covers")
                            EmberTextField(placeholder: "4", text: $covers, keyboard: .numberPad)
                        }
                    }
                }
                if channel == .cloud {
                    EmberFieldLabel(text: "Platform")
                    EmberChipRow(
                        options: ["Zomato", "Swiggy", "Other"],
                        selection: $platform,
                        style: .channel
                    )
                    EmberFieldLabel(text: "Platform order id")
                    EmberTextField(placeholder: "ZOM-90821", text: $platformOrderId)
                }
                EmberFieldLabel(text: "Guest")
                EmberTextField(placeholder: "Table guest or name", text: $guest)
                EmberFieldLabel(text: "Note")
                EmberTextField(placeholder: "Allergies, course, fire note", text: $note)
            }
            EmberFieldLabel(text: "Assignee")
            EmberTextField(placeholder: "Chef Ama", text: $assignee)
            if tab == .reprint {
                EmberFieldLabel(text: "Reprint reason")
                EmberTextField(placeholder: "Kitchen printer jam", text: $reprintReason)
            }
        }
    }

    // MARK: - Actions

    private func applyTabChange(_ next: KotTab) {
        tab = next
        stationFilter = stationChipOptions.first ?? "All stations"
        if next != .send, let ticket = selectedTicket ?? tickets.first {
            ticketId = ticket.id
            hydrateFromTicket(ticket)
        }
    }

    private func applyChannelChange(_ next: KotChannel) {
        channel = next
        stationFilter = stationChipOptions.first ?? "All stations"
        if next == .cloud, platformOrderId.isEmpty {
            platformOrderId = "ZOM-90821"
        }
    }

    private func hydrateFromTicket(_ ticket: OperatingActivityPayload) {
        if let key = ticket.clientKey, !key.isEmpty { draftKotKey = key }
        if let table = ticket.tableNumber, !table.isEmpty { tableNumber = table }
        if let cover = ticket.covers, !cover.isEmpty { covers = cover }
        if let loc = ticket.locationName, !loc.isEmpty, stationChipOptions.contains(loc) {
            stationFilter = loc
        }
        if let ch = ticket.channelName {
            if let match = KotChannel(rawValue: ch) {
                channel = match
            } else if ch.localizedCaseInsensitiveContains("cloud") {
                channel = .cloud
            } else if ch.localizedCaseInsensitiveContains("take") {
                channel = .takeaway
            }
        }
        if tab != .send {
            lines = parseTicketLines(ticket)
        }
    }

    private func parseTicketLines(_ ticket: OperatingActivityPayload?) -> [KotLine] {
        guard let ticket else { return [] }
        let note = ticket.note ?? ""
        let heldNames: Set<String> = {
            guard let range = note.range(of: "Hold:", options: .caseInsensitive) else { return [] }
            let after = note[range.upperBound...]
            let segment = after.split(separator: "·", maxSplits: 1, omittingEmptySubsequences: false).first ?? after.prefix(0)
            return Set(
                segment.split(separator: ",")
                    .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
                    .filter { !$0.isEmpty }
            )
        }()
        let posted = (ticket.lines ?? []).filter { !$0.itemName.trimmingCharacters(in: .whitespaces).isEmpty }
        if !posted.isEmpty {
            return posted.map { row in
                let name = row.itemName
                let isHold = heldNames.contains(name.trimmingCharacters(in: .whitespaces).lowercased())
                return KotLine(
                    id: row.masterId ?? "\(ticket.id)-\(name)",
                    masterId: row.masterId,
                    name: name,
                    unit: row.unit ?? ticket.unit ?? "pc",
                    quantity: max(1, min(99, Int(row.quantity ?? "1") ?? 1)),
                    station: ticket.locationName?.nilIfBlank ?? "Hot",
                    course: isHold ? .hold : .now,
                    meta: isHold ? "Hold until guest ready" : ""
                )
            }
        }
        // Legacy fallback when activity projection omitted lines.
        let name = ticket.title.isEmpty ? "Kitchen line" : ticket.title
        let station = ticket.locationName?.trimmingCharacters(in: .whitespaces)
        let isHold = note.localizedCaseInsensitiveContains("hold")
        return [
            KotLine(
                id: ticket.id,
                masterId: nil,
                name: name,
                unit: ticket.unit ?? "pc",
                quantity: max(1, Int(ticket.quantity ?? "1") ?? 1),
                station: (station?.isEmpty == false ? station! : "Hot"),
                course: isHold ? .hold : .now,
                meta: note.isEmpty ? "" : String(note.prefix(48))
            )
        ]
    }

    private func addDish() {
        let typed = dishQuery.trimmingCharacters(in: .whitespaces)
        guard !menuDishes.isEmpty else { return }
        let match = menuDishes.first { $0.name.caseInsensitiveCompare(typed) == .orderedSame }
            ?? menuDishes.first { !typed.isEmpty && $0.name.localizedCaseInsensitiveContains(typed) }
            ?? menuDishes.first
        if let match { add(match) }
        dishQuery = ""
    }

    private func add(_ dish: OperatingMasterPayload) {
        if let index = lines.firstIndex(where: { $0.masterId == dish.id }) {
            lines[index].quantity += 1
            return
        }
        let defaultStation: String = {
            if stationFilter != "All stations",
               stationFilter != "Held only",
               stationFilter != "Cloud queue" {
                return stationFilter
            }
            return stations.first?.name ?? "Hot"
        }()
        lines.append(KotLine.of(dish, station: defaultStation))
    }

    private func removeLine(_ id: String) {
        lines.removeAll { $0.id == id }
    }

    private func bumpQuantity(_ id: String, by delta: Int) {
        guard let index = lines.firstIndex(where: { $0.id == id }) else { return }
        let next = lines[index].quantity + delta
        if next <= 0 {
            lines.remove(at: index)
        } else {
            lines[index].quantity = min(99, next)
        }
    }

    private func toggleCourse(_ id: String) {
        guard let index = lines.firstIndex(where: { $0.id == id }) else { return }
        lines[index].course = lines[index].course == .now ? .hold : .now
        if lines[index].course == .hold, lines[index].meta.isEmpty {
            lines[index].meta = "Hold until guest ready"
        }
    }

    private func cycleStation(_ id: String) {
        guard let index = lines.firstIndex(where: { $0.id == id }) else { return }
        let options = stations.map(\.name)
        let pool = options.isEmpty ? ["Tandoor", "Hot", "Cold", "Bar", "Pack"] : options
        let current = lines[index].station
        let nextIndex = ((pool.firstIndex(of: current) ?? -1) + 1) % pool.count
        lines[index].station = pool[nextIndex]
    }

    private func nextFireStatus(_ status: String?) -> String? {
        switch (status ?? "").uppercased() {
        case "", "PLACED": return "PREPARING"
        case "PREPARING": return "READY"
        default: return nil
        }
    }

    private func ticketMeta(_ ticket: OperatingActivityPayload) -> String {
        [
            ticket.clientKey.flatMap { $0.isEmpty ? nil : $0 },
            ticket.tableNumber.flatMap { $0.isEmpty ? nil : "Table \($0)" },
            ticket.covers.flatMap { $0.isEmpty ? nil : "\($0) covers" },
            ticket.locationName.flatMap { $0.isEmpty ? nil : $0 },
            ticket.channelName.flatMap { $0.isEmpty ? nil : $0 },
            ticket.status.flatMap { $0.isEmpty ? nil : $0 },
        ]
        .compactMap { $0 }
        .joined(separator: " · ")
    }

    private func extractPlatformRef(_ note: String) -> String? {
        let tokens = note.components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { !$0.isEmpty }
        return tokens.first {
            $0.uppercased().hasPrefix("ZOM") || $0.uppercased().hasPrefix("SWIG")
        }
    }

    private func submit() async {
        guard canSubmit else { return }
        saving = true
        defer { saving = false }
        switch tab {
        case .send:
            await sendTicket()
        case .fire:
            await fireTicket()
        case .reprint:
            reprintTicket()
        }
    }

    private func sendTicket() async {
        var contract = OperatingContractInput()
        contract.orderStatus = "PLACED"
        contract.taxMode = "NOT_CONFIGURED"
        let table = tableNumber.trimmingCharacters(in: .whitespaces)
        let coverCount = covers.trimmingCharacters(in: .whitespaces)
        let primaryStation: String = {
            if stationFilter != "All stations",
               stationFilter != "Held only",
               stationFilter != "Cloud queue" {
                return stationFilter
            }
            return lines.first?.station ?? stations.first?.name ?? ""
        }()
        contract.locationName = primaryStation.isEmpty ? nil : primaryStation
        if channel == .inDining {
            contract.tableNumber = table.isEmpty ? nil : table
            contract.covers = coverCount.isEmpty ? nil : coverCount
        }
        contract.clientKey = draftKotKey

        let held = lines.filter { $0.course == .hold }.map(\.name)
        let noteBits = [
            primaryStation.isEmpty ? nil : "Station: \(primaryStation)",
            held.isEmpty ? nil : "Hold: \(held.joined(separator: " · "))",
            channel == .cloud && !platformOrderId.trimmingCharacters(in: .whitespaces).isEmpty
                ? "\(platform) · \(platformOrderId.trimmingCharacters(in: .whitespaces))"
                : nil,
            note.trimmingCharacters(in: .whitespaces).isEmpty ? nil : note,
            assignee.trimmingCharacters(in: .whitespaces).isEmpty ? nil : "Assignee: \(assignee)",
        ].compactMap { $0 }

        let guestName = guest.trimmingCharacters(in: .whitespaces)
        let title: String = {
            switch channel {
            case .inDining:
                if !table.isEmpty { return "Table \(table)" }
            case .cloud:
                let id = platformOrderId.trimmingCharacters(in: .whitespaces)
                if !id.isEmpty { return id }
                return "\(platform) ticket"
            case .takeaway:
                return "Pickup · \(lines.count) item\(lines.count == 1 ? "" : "s")"
            }
            if !guestName.isEmpty { return "Ticket · \(guestName)" }
            return "Ticket · \(lines.count) item\(lines.count == 1 ? "" : "s")"
        }()

        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "ORDER",
                title: title,
                note: noteBits.isEmpty ? nil : noteBits.joined(separator: " · "),
                channelName: channel.rawValue,
                counterpartyName: guestName.isEmpty
                    ? (channel == .cloud ? platform : nil)
                    : guestName,
                quantity: coverCount.isEmpty ? nil : coverCount,
                lineItems: lines.map {
                    .init(
                        itemName: $0.name,
                        quantity: "\($0.quantity)",
                        unit: $0.unit,
                        unitPrice: nil,
                        masterId: $0.masterId
                    )
                },
                contract: contract,
                paymentStatus: "NONE"
            )
        )
        if ok { dismiss() }
    }

    private func fireTicket() async {
        guard let ticket = selectedTicket, let next = fireTarget else { return }
        var contract = OperatingContractInput()
        contract.orderStatus = next
        contract.tableNumber = ticket.tableNumber
        contract.covers = ticket.covers
        contract.locationName = ticket.locationName
        contract.clientKey = ticket.clientKey
        let cleanedNote: String? = {
            let base = ticket.note ?? ""
            let suffix = "Fired · held → Now"
            if base.isEmpty { return suffix }
            let withoutHold = base
                .components(separatedBy: "Hold:")
                .first?
                .trimmingCharacters(in: CharacterSet(charactersIn: " ·"))
            if let withoutHold, !withoutHold.isEmpty {
                return "\(withoutHold) · \(suffix)"
            }
            return suffix
        }()
        let ok = await model.record(
            OperatingEventInput(
                momentId: model.momentId,
                eventType: "ORDER",
                title: ticket.title.isEmpty ? "Kitchen ticket" : ticket.title,
                amount: ticket.amount,
                note: cleanedNote,
                channelName: ticket.channelName ?? channel.rawValue,
                counterpartyName: ticket.counterpartyName,
                quantity: ticket.quantity,
                contract: contract,
                paymentStatus: "NONE",
                revisesEventId: ticket.id
            )
        )
        if ok { dismiss() }
    }

    private func reprintTicket() {
        let kot = heroKotId
        let headline = heroHeadline
        let rows = visibleLines.isEmpty ? parseTicketLines(selectedTicket) : visibleLines
        let lineText: String = {
            if rows.isEmpty {
                return selectedTicket?.title ?? "Kitchen ticket"
            }
            return rows.map { "\($0.name) ×\($0.quantity) · \($0.station)" }.joined(separator: "\n")
        }()
        let reason = reprintReason.trimmingCharacters(in: .whitespaces)
        let body = [
            "Reprint KOT · \(kot)",
            headline,
            lineText,
            reason.isEmpty ? nil : "Reason: \(reason)",
            "Same KOT id — never duplicates the order",
        ]
        .compactMap { $0 }
        .joined(separator: "\n")
        InviteOutboundShare.presentSystemShare(items: [body])
    }
}

// MARK: - Line model

private struct KotLine: Identifiable, Equatable {
    enum Course: String, Equatable {
        case now = "Now"
        case hold = "Hold"
    }

    let id: String
    let masterId: String?
    let name: String
    let unit: String?
    var quantity: Int
    var station: String
    var course: Course
    var meta: String

    static func of(_ dish: OperatingMasterPayload, station: String) -> KotLine {
        KotLine(
            id: dish.id + "-\(UUID().uuidString.prefix(6))",
            masterId: dish.id,
            name: dish.name,
            unit: dish.unit ?? "pc",
            quantity: 1,
            station: station,
            course: .now,
            meta: ""
        )
    }
}
