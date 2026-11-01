import SwiftUI

/// The three picker frames under `2487:37611`:
/// `RST-PICKER · Date` `2521:29334`, `· Month` `2521:29458`, `· Due date & time` `2521:29515`.
///
/// One surface with three modes so the POS, budget and KOT sheets share the same chrome.
/// Every chip and grid is real: `Today / Tomorrow / +7 days` resolve against the device
/// calendar, the month grid follows the real calendar, and `Now / End of shift` resolve to
/// actual clock times. Nothing is a placeholder.
struct EmberPickerSheet: View {
    enum Mode: String, Identifiable {
        case date, month, dateTime
        var id: String { rawValue }

        var title: String {
            switch self {
            case .date: return "Select date"
            case .month: return "Select month"
            case .dateTime: return "Due date & time"
            }
        }

        var subtitle: String {
            switch self {
            case .date: return "Tap a day"
            case .month: return "Budget, target & reporting period"
            case .dateTime: return "Prep, service or delivery slot"
            }
        }
    }

    let mode: Mode
    /// Short venue name shown in the subtitle (`Spice Route · tap a day`).
    var venue: String = ""
    @Binding var date: Date
    var onDismiss: () -> Void

    @State private var cursor: Date = Date()
    @State private var hour = 18
    @State private var minute = 0

    private var calendar: Calendar {
        var calendar = Calendar.current
        calendar.firstWeekday = 1
        return calendar
    }

    var body: some View {
        EmberScreenBackground {
            VStack(alignment: .leading, spacing: 12) {
                EmberSheetGrabber()
                EmberSheetHeader(
                    title: mode.title,
                    subtitle: venue.isEmpty ? mode.subtitle : "\(venue) · \(mode.subtitle)",
                    onClose: onDismiss
                )

                switch mode {
                case .date: dayPicker
                case .month: monthPicker
                case .dateTime: dateTimePicker
                }

                selectionLine

                HStack(spacing: 10) {
                    Button(action: onDismiss) {
                        Text("Cancel")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(RestaurantEmber.textBody)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(RestaurantEmber.chip)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)

                    Button {
                        commit()
                        onDismiss()
                    } label: {
                        Text("Confirm")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(RestaurantEmber.ember)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }

                Spacer(minLength: 16)
            }
            .padding(.horizontal, RestaurantEmber.gutter)
            .padding(.vertical, 12)
        }
        .presentationDragIndicator(.hidden)
        .onAppear {
            cursor = date
            let parts = calendar.dateComponents([.hour, .minute], from: date)
            hour = parts.hour ?? 18
            minute = parts.minute ?? 0
        }
    }

    // MARK: - Day `2521:29334`

    private var dayPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                ForEach(Self.dayShortcuts, id: \.label) { shortcut in
                    EmberModeChip(
                        text: shortcut.label,
                        selected: calendar.isDate(date, inSameDayAs: shortcut.resolve())
                    ) {
                        date = shortcut.resolve()
                        cursor = date
                    }
                }
                Spacer(minLength: 0)
            }

            monthHeader
            weekdayHeader
            dayGrid { day in
                date = day
            }
        }
    }

    // MARK: - Month `2521:29458`

    private var monthPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                ForEach(["This month", "Last month"], id: \.self) { label in
                    let target = label == "This month"
                        ? Date()
                        : calendar.date(byAdding: .month, value: -1, to: Date()) ?? Date()
                    EmberModeChip(
                        text: label,
                        selected: calendar.isDate(date, equalTo: target, toGranularity: .month)
                    ) {
                        date = target
                        cursor = target
                    }
                }
                EmberModeChip(text: "Custom range ›", selected: false, locked: "Not a field", onTap: nil)
                Spacer(minLength: 0)
            }

            HStack {
                Button { shiftYear(-1) } label: { chevron("‹") }.buttonStyle(.plain)
                Text("\(yearText)")
                    .font(.plusJakarta(size: 16, weight: .bold))
                    .foregroundStyle(RestaurantEmber.textPrimary)
                    .frame(maxWidth: .infinity)
                Button { shiftYear(1) } label: { chevron("›") }.buttonStyle(.plain)
            }

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 4), spacing: 6) {
                ForEach(0..<12, id: \.self) { index in
                    let selected = calendar.component(.month, from: date) == index + 1
                        && calendar.component(.year, from: date) == calendar.component(.year, from: cursor)
                    Button {
                        var parts = calendar.dateComponents([.year], from: cursor)
                        parts.month = index + 1
                        parts.day = 1
                        if let built = calendar.date(from: parts) {
                            date = built
                            cursor = built
                        }
                    } label: {
                        Text(Self.monthNames[index])
                            .font(.plusJakarta(size: 13, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? Color.white : RestaurantEmber.textBody)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                            .background(selected ? RestaurantEmber.ember : RestaurantEmber.chip)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Date & time `2521:29515`

    private var dateTimePicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            monthHeader
            weekdayHeader
            dayGrid { day in date = day }

            EmberFieldLabel("Time")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 5), spacing: 6) {
                ForEach(Self.timeSlots, id: \.self) { slot in
                    let selected = slot.hour == hour && slot.minute == minute
                    Button {
                        hour = slot.hour
                        minute = slot.minute
                    } label: {
                        Text(slot.label)
                            .font(.plusJakarta(size: 12, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? Color.white : RestaurantEmber.textBody)
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(selected ? RestaurantEmber.ember : RestaurantEmber.chip)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            HStack(spacing: 6) {
                EmberModeChip(text: "Now", selected: false) {
                    let now = Date()
                    let parts = calendar.dateComponents([.hour, .minute], from: now)
                    hour = parts.hour ?? 18
                    minute = parts.minute ?? 0
                    date = now
                }
                EmberModeChip(text: "End of shift", selected: false) {
                    hour = 23
                    minute = 0
                    date = Date()
                }
                Spacer(minLength: 0)
            }
        }
    }

    // MARK: - Shared pieces

    private var monthHeader: some View {
        HStack {
            Button { shiftMonth(-1) } label: { chevron("‹") }.buttonStyle(.plain)
            Text("\(Self.monthNames[calendar.component(.month, from: cursor) - 1]) \(yearText)")
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(RestaurantEmber.textPrimary)
                .frame(maxWidth: .infinity)
            Button { shiftMonth(1) } label: { chevron("›") }.buttonStyle(.plain)
        }
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Array(["M", "T", "W", "T", "F", "S", "S"].enumerated()), id: \.offset) { _, label in
                Text(label)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(RestaurantEmber.textFaint)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func dayGrid(onPick: @escaping (Date) -> Void) -> some View {
        let days = gridDays()
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 4) {
            ForEach(Array(days.enumerated()), id: \.offset) { _, day in
                if let day {
                    let selected = calendar.isDate(day, inSameDayAs: date)
                    let isToday = calendar.isDateInToday(day)
                    Button { onPick(day) } label: {
                        Text("\(calendar.component(.day, from: day))")
                            .font(.plusJakarta(size: 13, weight: selected ? .bold : .medium))
                            .foregroundStyle(selected ? Color.white : (isToday ? RestaurantEmber.gold : RestaurantEmber.textBody))
                            .frame(maxWidth: .infinity)
                            .frame(height: 38)
                            .background(selected ? RestaurantEmber.ember : Color.clear)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                } else {
                    Color.clear.frame(height: 38)
                }
            }
        }
    }

    private var selectionLine: some View {
        Text("Selected: \(selectionText)")
            .font(.plusJakarta(size: 12, weight: .medium))
            .foregroundStyle(RestaurantEmber.gold)
            .lineLimit(1)
    }

    private var selectionText: String {
        switch mode {
        case .date:
            return format("EEE, dd MMM yyyy")
        case .month:
            let start = calendar.date(from: calendar.dateComponents([.year, .month], from: date)) ?? date
            let days = calendar.range(of: .day, in: .month, for: start)?.count ?? 30
            return "\(format("MMMM yyyy")) · 01–\(days) \(format("MMM"))"
        case .dateTime:
            var merged = calendar.dateComponents([.year, .month, .day], from: date)
            merged.hour = hour
            merged.minute = minute
            let built = calendar.date(from: merged) ?? date
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.dateFormat = "EEE, dd MMM · h:mm a"
            return formatter.string(from: built)
        }
    }

    private func format(_ pattern: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = pattern
        return formatter.string(from: date)
    }

    private func chevron(_ glyph: String) -> some View {
        Text(glyph)
            .font(.plusJakarta(size: 16, weight: .bold))
            .foregroundStyle(RestaurantEmber.textBody)
            .frame(width: 34, height: 34)
            .background(RestaurantEmber.chip)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var yearText: String { "\(calendar.component(.year, from: cursor))" }

    private func shiftMonth(_ delta: Int) {
        cursor = calendar.date(byAdding: .month, value: delta, to: cursor) ?? cursor
    }

    private func shiftYear(_ delta: Int) {
        cursor = calendar.date(byAdding: .year, value: delta, to: cursor) ?? cursor
    }

    /// Real calendar grid: leading blanks so day 1 lands under its weekday column.
    private func gridDays() -> [Date?] {
        guard let range = calendar.range(of: .day, in: .month, for: cursor),
              let first = calendar.date(from: calendar.dateComponents([.year, .month], from: cursor))
        else { return [] }
        // `weekday` is 1=Sunday; shift so Monday is column 0.
        let leading = (calendar.component(.weekday, from: first) + 5) % 7
        var cells: [Date?] = Array(repeating: nil, count: leading)
        for day in range {
            cells.append(calendar.date(byAdding: .day, value: day - 1, to: first))
        }
        return cells
    }

    private func commit() {
        switch mode {
        case .date, .dateTime:
            var parts = calendar.dateComponents([.year, .month, .day], from: date)
            if mode == .dateTime {
                parts.hour = hour
                parts.minute = minute
            }
            date = calendar.date(from: parts) ?? date
        case .month:
            var parts = calendar.dateComponents([.year, .month], from: date)
            parts.day = 1
            date = calendar.date(from: parts) ?? date
        }
    }

    private struct DayShortcut {
        let label: String
        let offset: Int
        func resolve() -> Date {
            Calendar.current.date(byAdding: .day, value: offset, to: Date()) ?? Date()
        }
    }

    private static let dayShortcuts: [DayShortcut] = [
        DayShortcut(label: "Today", offset: 0),
        DayShortcut(label: "Tomorrow", offset: 1),
        DayShortcut(label: "+7 days", offset: 7),
    ]

    private static let monthNames = [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    ]

    private struct TimeSlot: Hashable {
        let hour: Int
        let minute: Int
        var label: String {
            let suffix = hour >= 12 ? "pm" : "am"
            let display = hour % 12 == 0 ? 12 : hour % 12
            return "\(display):\(String(format: "%02d", minute)) \(suffix)"
        }
    }

    private static let timeSlots: [TimeSlot] = [
        TimeSlot(hour: 9, minute: 0), TimeSlot(hour: 10, minute: 30),
        TimeSlot(hour: 12, minute: 0), TimeSlot(hour: 13, minute: 30),
        TimeSlot(hour: 15, minute: 0), TimeSlot(hour: 16, minute: 30),
        TimeSlot(hour: 18, minute: 0), TimeSlot(hour: 19, minute: 30),
        TimeSlot(hour: 21, minute: 0),
    ]
}
