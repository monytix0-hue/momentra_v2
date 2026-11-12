import SwiftUI

/// Amber-themed centered Date picker modal — Figma `2549:29336`.
struct MfgDatePickerModal: View {
    let theme: OperatingTheme
    @Binding var isoDate: String
    @Binding var isPresented: Bool

    @State private var cursor: Date = Date()
    @State private var picked: Date = Date()

    private let cal = Calendar(identifier: .gregorian)

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
                .onTapGesture { isPresented = false }
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Select date")
                            .font(.plusJakarta(size: 18, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                        Text("Forge & Form · tap a day")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(MfgSheetChrome.textBody)
                    }
                    Spacer()
                    Button { isPresented = false } label: {
                        Text("✕")
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(width: 32, height: 32)
                            .background(theme.inset, in: Circle())
                    }
                    .buttonStyle(.plain)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        quickChip("Today", days: 0)
                        quickChip("Tomorrow", days: 1)
                        quickChip("+7 days", days: 7)
                        Button {
                            picked = Date()
                            cursor = Date()
                        } label: {
                            Text("Clear")
                                .font(.plusJakarta(size: 12, weight: .medium))
                                .foregroundStyle(MfgSheetChrome.textBody)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(theme.inset, in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }
                HStack {
                    Text(monthTitle)
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                    Spacer()
                    Button {
                        cursor = cal.date(byAdding: .month, value: -1, to: cursor) ?? cursor
                    } label: {
                        Text("‹").font(.plusJakarta(size: 16, weight: .bold)).frame(width: 32, height: 32)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    Button {
                        cursor = cal.date(byAdding: .month, value: 1, to: cursor) ?? cursor
                    } label: {
                        Text("›").font(.plusJakarta(size: 16, weight: .bold)).frame(width: 32, height: 32)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
                calendarGrid
                Text("Selected: \(selectedLabel)")
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(theme.accentSoft)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.accent.opacity(0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .strokeBorder(theme.accent.opacity(0.5), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                HStack(spacing: 8) {
                    Button { isPresented = false } label: {
                        Text("Cancel")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    Button {
                        isoDate = Self.iso(picked)
                        isPresented = false
                    } label: {
                        Text("Confirm")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(theme.accent, in: RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .background(theme.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: theme.accent.opacity(0.25), radius: 20, y: 8)
            .padding(.horizontal, 24)
        }
        .onAppear {
            picked = Self.parse(isoDate) ?? Date()
            cursor = picked
        }
    }

    private func quickChip(_ label: String, days: Int) -> some View {
        let date = cal.date(byAdding: .day, value: days, to: Date()) ?? Date()
        let on = cal.isDate(picked, inSameDayAs: date)
        return Button {
            picked = date
            cursor = date
        } label: {
            Text(label)
                .font(.plusJakarta(size: 12, weight: on ? .bold : .medium))
                .foregroundStyle(on ? Color(hex: "#18181B") : MfgSheetChrome.textBody)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(on ? theme.accent : theme.inset, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    private var monthTitle: String {
        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: cursor)
    }

    private var selectedLabel: String {
        let f = DateFormatter()
        f.dateFormat = "EEE, dd MMM yyyy"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: picked)
    }

    private var calendarGrid: some View {
        let comps = cal.dateComponents([.year, .month], from: cursor)
        let first = cal.date(from: comps) ?? cursor
        let weekday = cal.component(.weekday, from: first) // 1=Sun
        let monStartBlank = (weekday + 5) % 7
        let daysInMonth = cal.range(of: .day, in: .month, for: cursor)?.count ?? 30
        let today = Date()

        return VStack(spacing: 4) {
            HStack {
                ForEach(["M", "T", "W", "T", "F", "S", "S"], id: \.self) { d in
                    Text(d)
                        .font(.plusJakarta(size: 11, weight: .bold))
                        .foregroundStyle(MfgSheetChrome.textBody)
                        .frame(maxWidth: .infinity)
                }
            }
            ForEach(0..<6, id: \.self) { week in
                HStack(spacing: 4) {
                    ForEach(0..<7, id: \.self) { col in
                        let idx = week * 7 + col
                        let day = idx - monStartBlank + 1
                        if day < 1 || day > daysInMonth {
                            Color.clear.frame(height: 36).frame(maxWidth: .infinity)
                        } else {
                            let date = cal.date(byAdding: .day, value: day - 1, to: first) ?? first
                            let isSelected = cal.isDate(date, inSameDayAs: picked)
                            let isToday = cal.isDate(date, inSameDayAs: today)
                            Button {
                                picked = date
                            } label: {
                                Text("\(day)")
                                    .font(.plusJakarta(size: 13, weight: isSelected ? .bold : .medium))
                                    .foregroundStyle(
                                        isSelected ? OperatingV12.title
                                            : isToday ? theme.accentSoft : OperatingV12.title
                                    )
                                    .frame(maxWidth: .infinity, minHeight: 36)
                                    .background(isSelected ? theme.accent : Color.clear)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .strokeBorder(isToday && !isSelected ? theme.accentSoft : Color.clear, lineWidth: 1)
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
        .padding(12)
        .background(theme.canvas)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(theme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    static func iso(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }

    static func parse(_ iso: String) -> Date? {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f.date(from: String(iso.prefix(10)))
    }
}

/// Amber-themed date / month / datetime quick picks — Figma MFG-PICKER frames.
struct MfgDateQuickPicker: View {
    let theme: OperatingTheme
    @Binding var isoDate: String

    private var today: Date { Date() }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            MfgFieldLabel(text: "Quick pick")
            HStack(spacing: 6) {
                ForEach(quickOptions, id: \.0) { label, date in
                    MfgModeChip(
                        theme: theme,
                        text: label,
                        selected: isoDate.prefix(10) == Self.iso(date)
                    ) {
                        isoDate = Self.iso(date)
                    }
                }
            }
            Text(display)
                .font(.plusJakarta(size: 16, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(MfgSheetChrome.sheet)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private var quickOptions: [(String, Date)] {
        [
            ("Today", today),
            ("Tomorrow", Calendar.current.date(byAdding: .day, value: 1, to: today) ?? today),
            ("+3 days", Calendar.current.date(byAdding: .day, value: 3, to: today) ?? today),
            ("+1 week", Calendar.current.date(byAdding: .day, value: 7, to: today) ?? today),
        ]
    }

    private var display: String {
        let f = DateFormatter()
        f.dateFormat = "dd MMM yyyy"
        f.locale = Locale(identifier: "en_US_POSIX")
        if let d = Self.parse(isoDate) { return f.string(from: d) }
        return isoDate
    }

    static func iso(_ date: Date) -> String { MfgDatePickerModal.iso(date) }
    static func parse(_ iso: String) -> Date? { MfgDatePickerModal.parse(iso) }
}

struct MfgMonthPicker: View {
    let theme: OperatingTheme
    @Binding var yearMonth: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            MfgFieldLabel(text: "Period")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(months, id: \.self) { ym in
                        MfgModeChip(
                            theme: theme,
                            text: Self.label(ym),
                            selected: yearMonth.prefix(7) == ym
                        ) {
                            yearMonth = ym
                        }
                    }
                }
            }
        }
    }

    private var months: [String] {
        let cal = Calendar.current
        let now = Date()
        return (-1...2).compactMap { offset -> String? in
            guard let d = cal.date(byAdding: .month, value: offset, to: now) else { return nil }
            let f = DateFormatter()
            f.dateFormat = "yyyy-MM"
            return f.string(from: d)
        }
    }

    static func label(_ ym: String) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        guard let d = f.date(from: String(ym.prefix(7))) else { return ym }
        let out = DateFormatter()
        out.dateFormat = "MMM yyyy"
        out.locale = Locale(identifier: "en_US_POSIX")
        return out.string(from: d)
    }
}

/// Centered Month / budget period modal — Figma `2549:29460` year + 3×4 month grid.
struct MfgMonthPickerModal: View {
    let theme: OperatingTheme
    @Binding var yearMonth: String
    @Binding var isPresented: Bool

    @State private var year: Int = Calendar.current.component(.year, from: Date())
    @State private var pickedMonth: Int = Calendar.current.component(.month, from: Date())

    private let monthLabels = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    private let cal = Calendar(identifier: .gregorian)

    private var nowYm: (year: Int, month: Int) {
        let now = Date()
        return (cal.component(.year, from: now), cal.component(.month, from: now))
    }

    private var pickedKey: String {
        String(format: "%04d-%02d", year, pickedMonth)
    }

    private var selectedSummary: String {
        guard let start = cal.date(from: DateComponents(year: year, month: pickedMonth, day: 1)),
              let range = cal.range(of: .day, in: .month, for: start),
              let end = cal.date(byAdding: .day, value: range.count - 1, to: start)
        else { return "Selected: \(pickedKey)" }
        let monthFmt = DateFormatter()
        monthFmt.locale = Locale(identifier: "en_US_POSIX")
        monthFmt.dateFormat = "MMMM yyyy"
        let dayFmt = DateFormatter()
        dayFmt.locale = Locale(identifier: "en_US_POSIX")
        dayFmt.dateFormat = "dd MMM"
        return "Selected: \(monthFmt.string(from: start)) · \(dayFmt.string(from: start))–\(dayFmt.string(from: end))"
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
                .onTapGesture { isPresented = false }
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Select month")
                            .font(.plusJakarta(size: 18, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                        Text("Budget, target & reporting period")
                            .font(.plusJakarta(size: 11))
                            .foregroundStyle(MfgSheetChrome.textBody)
                    }
                    Spacer()
                    Button { isPresented = false } label: {
                        Text("✕")
                            .font(.plusJakarta(size: 13, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(width: 32, height: 32)
                            .background(theme.inset, in: Circle())
                            .overlay(Circle().strokeBorder(theme.accent, lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        quickChip("This month", year: nowYm.year, month: nowYm.month)
                        let last = cal.date(byAdding: .month, value: -1, to: Date()) ?? Date()
                        quickChip(
                            "Last month",
                            year: cal.component(.year, from: last),
                            month: cal.component(.month, from: last)
                        )
                        Text("Custom range ›")
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(MfgSheetChrome.textBody)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(theme.inset, in: Capsule())
                    }
                }
                HStack {
                    Text("\(year)")
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(OperatingV12.title)
                    Spacer()
                    Button { year -= 1 } label: {
                        Text("‹").font(.plusJakarta(size: 16, weight: .bold)).frame(width: 32, height: 32)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10).strokeBorder(theme.accent, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                    Button { year += 1 } label: {
                        Text("›").font(.plusJakarta(size: 16, weight: .bold)).frame(width: 32, height: 32)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10).strokeBorder(theme.accent, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                }
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(1...12, id: \.self) { month in
                        let selected = pickedMonth == month
                        let isCurrent = year == nowYm.year && month == nowYm.month
                        Button {
                            pickedMonth = month
                        } label: {
                            Text(monthLabels[month - 1])
                                .font(.plusJakarta(size: 14, weight: selected ? .bold : .medium))
                                .foregroundStyle(selected ? OperatingV12.title : (isCurrent ? theme.accentSoft : OperatingV12.title))
                                .frame(maxWidth: .infinity, minHeight: 48)
                                .background(selected ? theme.accent : theme.inset)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .strokeBorder(isCurrent && !selected ? theme.accentSoft : Color.clear, lineWidth: 1)
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(12)
                .background(theme.canvas)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(theme.accent, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

                Text(selectedSummary)
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(theme.accentSoft)
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.accent.opacity(0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .strokeBorder(theme.accent.opacity(0.5), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                HStack(spacing: 8) {
                    Button { isPresented = false } label: {
                        Text("Cancel")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12).strokeBorder(theme.accent, lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)
                    Button {
                        yearMonth = pickedKey
                        isPresented = false
                    } label: {
                        Text("Confirm")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .background(theme.accent, in: RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .background(theme.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: theme.accent.opacity(0.25), radius: 20, y: 8)
            .padding(.horizontal, 24)
        }
        .onAppear {
            if let d = Self.parse(yearMonth) {
                year = cal.component(.year, from: d)
                pickedMonth = cal.component(.month, from: d)
            }
        }
    }

    private func quickChip(_ label: String, year y: Int, month m: Int) -> some View {
        let on = year == y && pickedMonth == m
        return Button {
            year = y
            pickedMonth = m
        } label: {
            Text(label)
                .font(.plusJakarta(size: 12, weight: on ? .bold : .medium))
                .foregroundStyle(on ? Color(hex: "#18181B") : MfgSheetChrome.textBody)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(on ? theme.accent : theme.inset, in: Capsule())
                .overlay(
                    Capsule().strokeBorder(on ? theme.accent : theme.accent.opacity(0.6), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
    }

    static func ym(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        return f.string(from: date)
    }

    static func parse(_ ym: String) -> Date? {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        return f.date(from: String(ym.prefix(7)))
    }
}

struct MfgDateTimeSlots: View {
    let theme: OperatingTheme
    @Binding var slot: String

    private let slots = ["Morning · 09:00", "Noon · 12:00", "Afternoon · 15:00", "Evening · 18:00"]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            MfgFieldLabel(text: "Dispatch / due slot")
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 6) {
                ForEach(slots, id: \.self) { s in
                    MfgModeChip(theme: theme, text: s, selected: slot == s) { slot = s }
                }
            }
        }
    }
}

/// Centered Date & time modal — Figma `2549:29517`.
struct MfgDateTimePickerModal: View {
    let theme: OperatingTheme
    @Binding var isoDate: String
    @Binding var slot: String
    @Binding var isPresented: Bool

    @State private var pickedDate: Date = Date()
    @State private var pickedSlot: String = "Morning · 09:00"

    private let slots = ["Morning · 09:00", "Noon · 12:00", "Afternoon · 15:00", "Evening · 18:00"]
    private let cal = Calendar(identifier: .gregorian)

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
                .onTapGesture { isPresented = false }
            VStack(alignment: .leading, spacing: 14) {
                Text("Select date & time")
                    .font(.plusJakarta(size: 18, weight: .bold))
                    .foregroundStyle(OperatingV12.title)
                Text("Dispatch · due · decide-by")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(MfgSheetChrome.textBody)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        quick("Today", days: 0)
                        quick("Tomorrow", days: 1)
                        quick("+3 days", days: 3)
                    }
                }
                Text(dayLabel)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(OperatingV12.title)
                MfgFieldLabel(text: "Time slot")
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                    ForEach(slots, id: \.self) { s in
                        MfgModeChip(theme: theme, text: s, selected: pickedSlot == s) { pickedSlot = s }
                    }
                }
                HStack(spacing: 8) {
                    Button { isPresented = false } label: {
                        Text("Cancel")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(OperatingV12.title)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(theme.inset, in: RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                    Button {
                        isoDate = MfgDatePickerModal.iso(pickedDate)
                        slot = pickedSlot
                        isPresented = false
                    } label: {
                        Text("Confirm")
                            .font(.plusJakarta(size: 14, weight: .bold))
                            .foregroundStyle(Color(hex: "#18181B"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(theme.accent, in: RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
            .background(theme.surface)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(theme.accent.opacity(0.28), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 24)
        }
        .onAppear {
            pickedDate = MfgDatePickerModal.parse(isoDate) ?? Date()
            pickedSlot = slot.isEmpty ? "Morning · 09:00" : slot
        }
    }

    private var dayLabel: String {
        let f = DateFormatter()
        f.dateFormat = "EEE, dd MMM yyyy"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: pickedDate)
    }

    private func quick(_ title: String, days: Int) -> some View {
        let date = cal.date(byAdding: .day, value: days, to: Date()) ?? Date()
        return MfgModeChip(
            theme: theme,
            text: title,
            selected: cal.isDate(pickedDate, inSameDayAs: date)
        ) { pickedDate = date }
    }
}
