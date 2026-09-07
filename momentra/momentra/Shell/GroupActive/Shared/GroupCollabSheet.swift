import PhotosUI
import SwiftUI
import UIKit
import UniformTypeIdentifiers

enum GroupCollabKind: String, Identifiable {
    case planning, booking, poll, update, memory, purchaseItem, resident
    var id: String { rawValue }
}

private struct HotelStayDraft: Identifiable {
    let id = UUID()
    var hotelName = ""
    var referenceCode = ""
    var amount = ""
    var startDate = ""
    var endDate = ""
}

private struct FlightSegmentDraft: Identifiable {
    let id = UUID()
    var legLabel = "OUTBOUND"
    var airline = ""
    var flightNumber = ""
    var originCode = ""
    var destinationCode = ""
    var seatClass = "Economy"
    var seatNumber = ""
    var departDate = ""
    var departTime = ""
    var arriveDate = ""
    var arriveTime = ""
}

/// Figma Trip Quick Add sheets — native pickers + full Figma field layout.
struct GroupCollabSheet: View {
    let kind: GroupCollabKind
    let momentId: String
    var momentTypeCode: String? = nil
    @Binding var isPresented: Bool
    var onSaved: () -> Void = {}

    @State private var primary = ""
    @State private var secondary = ""
    @State private var notes = ""
    @State private var optionA = ""
    @State private var optionB = ""
    @State private var optionC = ""
    @State private var showOption3 = false
    @State private var anonymous = true
    @State private var multi = false
    @State private var dateIso = ""
    @State private var timeIso = ""
    @State private var endDateIso = ""
    @State private var memoryType = "Photo"
    @State private var updateType = "Announcement"
    @State private var bookingType = "Hotel"
    @State private var confirmationNumber = ""
    @State private var bookingCost = ""
    @State private var bookingCurrency = "INR"
    @State private var preferredCurrencies: [String] = ["INR", "JPY", "USD"]
    @State private var bookedById: String?
    @State private var paidById: String?
    @State private var linkExpense = true
    @State private var splitStrategy = "EQUAL"
    @State private var splitIds: Set<String> = []
    @State private var splitValues: [String: String] = [:]
    @State private var bookingPlaces: [GroupSetupPlacePrefill] = []
    @State private var selectedPlaceIds: Set<String> = []
    @State private var hotelStays: [HotelStayDraft] = [HotelStayDraft()]
    @State private var flightSegments: [FlightSegmentDraft] = [
        FlightSegmentDraft(legLabel: "OUTBOUND"),
        FlightSegmentDraft(legLabel: "RETURN"),
    ]
    @State private var attachmentUploadIds: [String] = []
    @State private var attachmentNames: [String] = []
    @State private var showDocImporter = false
    @State private var uploadingDoc = false
    @State private var priority = "Medium"
    @State private var planCategory = ""
    @State private var mood = "🍁"
    @State private var notifyAll = true
    @State private var bookingConfirmed = true
    @State private var taggedIds: Set<String> = []
    @State private var assignedIds: Set<String> = []
    @State private var participants: [APIClient.GroupParticipantPayload] = []
    @State private var busy = false
    @State private var error: String?
    @State private var showSourcePicker = false
    @State private var showCamera = false
    @State private var showLibrary = false
    @State private var pickedItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var selectedImageData: Data?

    var body: some View {
        NativeSheetScaffold(
            title: titleText,
            onClose: { isPresented = false },
            background: TripForm.bg
        ) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TripSheetHeader(iconAsset: headerIcon, title: titleText, subtitle: subtitleText, accent: headerAccent)
                    switch kind {
                    case .planning: planningFields
                    case .booking: bookingFields
                    case .poll: pollFields
                    case .update: updateFields
                    case .memory: memoryFields
                    case .purchaseItem: purchaseFields
                    case .resident: residentFields
                    }
                    if let error {
                        Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                    }
                }
                .padding(24)
            }
        } footer: {
            TripPrimaryCta(
                label: ctaLabel,
                enabled: canSubmit,
                loading: busy,
                footer: ctaFooter,
                colors: ctaColors,
                onTap: { Task { await save() } }
            )
            .padding(.horizontal, 24)
            .padding(.bottom, 8)
            .background(TripForm.bg)
        }
        .presentationDetents([.large])
        .task { await loadParticipants() }
        .confirmationDialog("Add photo", isPresented: $showSourcePicker, titleVisibility: .visible) {
            Button("Camera") { showCamera = true }
            Button("Photo Library") { showLibrary = true }
            Button("Cancel", role: .cancel) {}
        }
        .photosPicker(isPresented: $showLibrary, selection: $pickedItem, matching: .images)
        .onChange(of: pickedItem) { _, item in
            guard let item else { return }
            Task {
                do {
                    if let data = try await item.loadTransferable(type: TripPickedImageData.self) {
                        selectedImageData = data.data
                        if let image = UIImage(data: data.data) {
                            selectedImage = image
                            error = nil
                        } else {
                            error = "Could not open that photo"
                        }
                    } else {
                        error = "Could not open that photo"
                    }
                } catch {
                    self.error = error.localizedDescription
                }
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            TripCameraPicker(image: $selectedImage, imageData: $selectedImageData, onCancel: { showCamera = false })
                .ignoresSafeArea()
        }
    }

    private var titleText: String {
        switch kind {
        case .planning: return "Add Plan"
        case .booking: return bookingType == "Flight" ? "Add Flight" : "Add Booking"
        case .poll: return "Create Poll"
        case .update: return "Post Update"
        case .memory: return "Capture Memory"
        case .purchaseItem: return "Add purchase item"
        case .resident: return "Add resident"
        }
    }

    private var subtitleText: String {
        switch kind {
        case .planning: return "Schedule an activity for your trip"
        case .booking: return "Attach reservations to your Kyoto timeline"
        case .poll: return "Vote on activities with your travel group"
        case .update: return "Share a status with your travel group"
        case .memory: return "Save a snippet of your trip for the shared journal"
        case .purchaseItem: return "Track something the group is buying"
        case .resident: return "Add someone to the household roster"
        }
    }

    private var headerIcon: String {
        switch kind {
        case .planning: return "GroupQaCalendar"
        case .booking: return "GroupQaTicket"
        case .poll: return "GroupQaVote"
        case .update: return "GroupQaMegaphone"
        case .memory: return "GroupQaCamera"
        case .purchaseItem: return "GroupQaChartBar"
        case .resident: return "GroupQaUserPlus"
        }
    }

    private var headerAccent: Color {
        switch kind {
        case .planning: return TripForm.teal
        case .booking: return TripForm.accent
        case .poll: return TripForm.purple
        case .update: return TripForm.blue
        case .memory: return TripForm.pink
        default: return TripForm.accent
        }
    }

    private var ctaLabel: String {
        switch kind {
        case .planning: return "Add Plan"
        case .booking: return bookingType == "Flight" ? "Add Flight" : "Add Booking"
        case .poll: return "Create Poll"
        case .update: return "Post Update"
        case .memory: return "Save Memory"
        default: return "Save"
        }
    }

    private var ctaFooter: String? {
        switch kind {
        case .planning: return "Added to group itinerary"
        case .booking: return nil
        case .update: return "Visible in group feed"
        default: return nil
        }
    }

    private var ctaColors: [Color] {
        switch kind {
        case .planning: return [TripForm.teal, Color(hex: "#0F766E")]
        case .booking: return [TripForm.accent, TripForm.accentEnd]
        case .poll: return [TripForm.purple, Color(hex: "#C084FC")]
        case .update: return [TripForm.blue, Color(hex: "#1D4ED8")]
        case .memory: return [TripForm.pink, Color(hex: "#F472B6")]
        default: return [TripForm.accent, TripForm.accentEnd]
        }
    }

    private var canSubmit: Bool {
        let trimmed = primary.trimmingCharacters(in: .whitespacesAndNewlines)
        switch kind {
        case .poll:
            return !trimmed.isEmpty
                && !optionA.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                && !optionB.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .memory:
            return !trimmed.isEmpty || selectedImageData != nil
        case .booking:
            if !trimmed.isEmpty { return true }
            if bookingType == "Hotel" { return hotelStays.contains { !$0.hotelName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty } }
            if bookingType == "Flight" {
                return flightSegments.contains {
                    !$0.airline.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        || !$0.flightNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                }
            }
            return false
        default:
            return !trimmed.isEmpty
        }
    }

    private var planningFields: some View {
        let categoryOptions = GroupPlanningCategoryCatalog.labels(for: momentTypeCode)
        return VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Category")
                TripChipRow(
                    options: categoryOptions,
                    selected: Binding(
                        get: {
                            planCategory.isEmpty
                                ? GroupPlanningCategoryCatalog.defaultLabel(for: momentTypeCode)
                                : planCategory
                        },
                        set: { planCategory = $0 }
                    ),
                    accent: TripForm.teal
                )
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Plan Title")
                TripSheetField(value: $primary, placeholder: "Dolphin Watching & Sunset Cruise")
            }
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Date")
                    TripDatePickField(value: $dateIso, accent: TripForm.teal)
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Time")
                    TripTimePickField(value: $timeIso, accent: TripForm.teal)
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Location")
                TripSheetField(value: $secondary, placeholder: "Coco Beach, Nerul", leadingIcon: "mappin.and.ellipse")
            }
            if !participants.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Assign To")
                    TripParticipantPicker(participants: participants, selectedIds: $assignedIds, accent: TripForm.teal)
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Priority")
                TripSegmentedControl(options: ["Low", "Medium", "High"], selected: $priority, accent: TripForm.teal)
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Add Notes")
                TripSheetField(
                    value: $notes,
                    placeholder: "Carry sunglasses and camera. Boat leaves sharp at 3:45.",
                    singleLine: false,
                    minHeight: 80
                )
            }
        }
    }

    private var bookingFields: some View {
        let seatClasses = ["Economy", "Premium Economy", "Business", "First"]
        let livingTypes: Set<String> = ["FAMILY_HOUSEHOLD", "FLATMATES", "CO_LIVING", "SHARED_LIVING", "COMMUNITY_LIVING"]
        let supportsPooled = livingTypes.contains((momentTypeCode ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased())
        var splitLabels: [(String, String)] = [("Equal", "EQUAL"), ("Custom", "EXACT"), ("% Percent", "PERCENTAGE")]
        if supportsPooled { splitLabels.append(("Pooled", "POOLED")) }
        return VStack(alignment: .leading, spacing: 12) {
            TripChipRow(
                options: ["Hotel", "Flight", "Transport", "Activity", "Restaurant"],
                selected: $bookingType,
                accent: TripForm.accent
            )
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: bookingType == "Flight" ? "Trip / Booking Name" : "Booking Name")
                TripSheetField(
                    value: $primary,
                    placeholder: bookingType == "Flight" ? "DEL → KIX Round Trip" : "MIMARU Kyoto Stay"
                )
            }

            if bookingType == "Hotel" {
                ForEach(Array(hotelStays.indices), id: \.self) { index in
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Stay \(index + 1)")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(TripForm.muted)
                        VStack(alignment: .leading, spacing: 6) {
                            TripFieldLabel(text: "Hotel Name")
                            TripSheetField(value: $hotelStays[index].hotelName, placeholder: "MIMARU Kyoto")
                        }
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "Confirmation #")
                                TripSheetField(value: $hotelStays[index].referenceCode, placeholder: "MMR-98402X")
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "Cost (\(TravelCurrencyCatalog.symbol(bookingCurrency)))")
                                TripSheetField(value: $hotelStays[index].amount, placeholder: "42,500", keyboardType: .decimalPad)
                            }
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            TripFieldLabel(text: "Check-In / Check-Out")
                            TripDateRangeField(start: $hotelStays[index].startDate, end: $hotelStays[index].endDate)
                        }
                    }
                    .padding(12)
                    .background(TripForm.field)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(TripForm.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                Button { hotelStays.append(HotelStayDraft()) } label: {
                    Text("+ Add Stay")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(TripForm.accent)
                }
                .buttonStyle(.plain)
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Total Cost (\(TravelCurrencyCatalog.symbol(bookingCurrency)))")
                        TripSheetField(value: $bookingCost, placeholder: "42,500", keyboardType: .decimalPad)
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Currency")
                        TripCurrencyMenuField(code: $bookingCurrency, preferred: preferredCurrencies)
                    }
                }
            } else if bookingType == "Flight" {
                ForEach(Array(flightSegments.indices), id: \.self) { index in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(flightSegments[index].legLabel == "RETURN" ? "Return" : flightSegments[index].legLabel == "CONNECTING" ? "Connecting" : "Outbound")
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(TripForm.muted)
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "Airline")
                                TripSheetField(value: $flightSegments[index].airline, placeholder: "IndiGo")
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "Flight #")
                                TripSheetField(value: $flightSegments[index].flightNumber, placeholder: "6E 214")
                            }
                        }
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "From")
                                TripSheetField(value: $flightSegments[index].originCode, placeholder: "DEL")
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "To")
                                TripSheetField(value: $flightSegments[index].destinationCode, placeholder: "KIX")
                            }
                        }
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "Class")
                                Menu {
                                    ForEach(seatClasses, id: \.self) { c in
                                        Button(c) { flightSegments[index].seatClass = c }
                                    }
                                } label: {
                                    HStack {
                                        Text(flightSegments[index].seatClass)
                                            .font(.plusJakarta(size: 14))
                                            .foregroundStyle(TripForm.text)
                                        Spacer()
                                        Image(systemName: "chevron.down")
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundStyle(TripForm.muted)
                                    }
                                    .frame(minHeight: 44)
                                    .padding(.horizontal, 16)
                                    .background(TripForm.field.opacity(0.5))
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(TripForm.border))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                }
                            }
                            VStack(alignment: .leading, spacing: 6) {
                                TripFieldLabel(text: "Seat")
                                TripSheetField(value: $flightSegments[index].seatNumber, placeholder: "12A")
                            }
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            TripFieldLabel(text: "Departure")
                            TripDateTimePickField(date: $flightSegments[index].departDate, time: $flightSegments[index].departTime, placeholder: "Depart")
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            TripFieldLabel(text: "Arrival")
                            TripDateTimePickField(date: $flightSegments[index].arriveDate, time: $flightSegments[index].arriveTime, placeholder: "Arrive")
                        }
                    }
                    .padding(12)
                    .background(TripForm.field)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(TripForm.border))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                Button {
                    flightSegments.append(FlightSegmentDraft(legLabel: flightSegments.count == 1 ? "RETURN" : "CONNECTING"))
                } label: {
                    Text("+ Add Segment")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(TripForm.accent)
                }
                .buttonStyle(.plain)
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Confirmation #")
                        TripSheetField(value: $confirmationNumber, placeholder: "6E-CONF-4421")
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Total Cost (\(TravelCurrencyCatalog.symbol(bookingCurrency)))")
                        TripSheetField(value: $bookingCost, placeholder: "28,400", keyboardType: .decimalPad)
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Currency")
                    TripCurrencyMenuField(code: $bookingCurrency, preferred: preferredCurrencies)
                }
            } else {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Confirmation #")
                        TripSheetField(value: $confirmationNumber, placeholder: "REF-12345")
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: "Cost (\(TravelCurrencyCatalog.symbol(bookingCurrency)))")
                        TripSheetField(value: $bookingCost, placeholder: "5,000", keyboardType: .decimalPad)
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Currency")
                    TripCurrencyMenuField(code: $bookingCurrency, preferred: preferredCurrencies)
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Date Range")
                    TripDateRangeField(start: $dateIso, end: $endDateIso)
                }
            }

            if !bookingPlaces.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Places")
                    FlowLayout(spacing: 8) {
                        ForEach(bookingPlaces.filter { $0.placeId != nil }, id: \.placeId) { place in
                            let id = place.placeId!
                            let on = selectedPlaceIds.contains(id)
                            Button {
                                if on { selectedPlaceIds.remove(id) } else { selectedPlaceIds.insert(id) }
                            } label: {
                                Text(place.label ?? String(id.prefix(8)))
                                    .font(.plusJakarta(size: 12, weight: .semibold))
                                    .foregroundStyle(on ? TripForm.accent : TripForm.muted)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(on ? TripForm.accent.opacity(0.15) : TripForm.field)
                                    .overlay(Capsule().stroke(on ? TripForm.accent : TripForm.border))
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }

            if !participants.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Booked By")
                    Menu {
                        ForEach(participants, id: \.participantId) { p in
                            Button(p.displayName ?? String(p.participantId.prefix(8))) {
                                bookedById = p.participantId
                            }
                        }
                    } label: {
                        HStack {
                            Text(
                                participants.first(where: { $0.participantId == bookedById })?.displayName
                                    ?? participants.first?.displayName
                                    ?? "You"
                            )
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(TripForm.text)
                            Spacer()
                            Image(systemName: "chevron.down")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(TripForm.muted)
                        }
                        .padding(12)
                        .background(TripForm.field)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(TripForm.border))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }

            TripToggleRow(
                title: "Status: Confirmed",
                subtitle: "Mark booking immediately as secured",
                isOn: $bookingConfirmed,
                accent: TripForm.accent
            )
            TripToggleRow(
                title: "Link as group expense",
                subtitle: "Split the booking cost with members",
                isOn: $linkExpense,
                accent: TripForm.accent
            )
            if linkExpense && !participants.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Paid By")
                    Menu {
                        ForEach(participants, id: \.participantId) { p in
                            Button(p.displayName ?? String(p.participantId.prefix(8))) {
                                paidById = p.participantId
                            }
                        }
                    } label: {
                        HStack {
                            Text(
                                participants.first(where: { $0.participantId == paidById })?.displayName
                                    ?? participants.first?.displayName
                                    ?? "You"
                            )
                            .font(.plusJakarta(size: 13, weight: .semibold))
                            .foregroundStyle(TripForm.text)
                            Spacer()
                            Image(systemName: "chevron.down")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(TripForm.muted)
                        }
                        .padding(12)
                        .background(TripForm.field)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(TripForm.border))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Split Type")
                    HStack(spacing: 4) {
                        ForEach(splitLabels, id: \.1) { item in
                            let on = splitStrategy == item.1
                            Button {
                                splitStrategy = item.1
                                let ids = Array(splitIds).sorted()
                                switch item.1 {
                                case "PERCENTAGE":
                                    if !ids.isEmpty {
                                        let even = String(format: "%.2f", 100.0 / Double(ids.count))
                                        splitValues = Dictionary(uniqueKeysWithValues: ids.map { ($0, even) })
                                    }
                                case "EXACT":
                                    if let total = Decimal(string: bookingCost.replacingOccurrences(of: ",", with: "")),
                                       !ids.isEmpty, total > 0 {
                                        let n = Decimal(ids.count)
                                        let base = (total / n as NSDecimalNumber).rounding(accordingToBehavior: NSDecimalNumberHandler(
                                            roundingMode: .down, scale: 2,
                                            raiseOnExactness: false, raiseOnOverflow: false,
                                            raiseOnUnderflow: false, raiseOnDivideByZero: false
                                        )).decimalValue
                                        var map: [String: String] = [:]
                                        var allocated = Decimal(0)
                                        for (i, id) in ids.enumerated() {
                                            if i == ids.count - 1 {
                                                map[id] = "\(total - allocated)"
                                            } else {
                                                map[id] = "\(base)"
                                                allocated += base
                                            }
                                        }
                                        splitValues = map
                                    } else {
                                        splitValues = Dictionary(uniqueKeysWithValues: ids.map { ($0, "") })
                                    }
                                default:
                                    splitValues = [:]
                                }
                            } label: {
                                Text(item.0)
                                    .font(.plusJakarta(size: 12, weight: .semibold))
                                    .foregroundStyle(on ? .white : TripForm.muted)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(on ? TripForm.accent : Color.clear)
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(4)
                    .background(TripForm.field)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                if splitStrategy == "POOLED" {
                    Text("Household spend — no per-member split.")
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(TripForm.muted)
                } else {
                    TripParticipantPicker(participants: participants, selectedIds: $splitIds, accent: TripForm.accent)
                }
                if splitStrategy != "EQUAL" && splitStrategy != "POOLED" && !splitIds.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        TripFieldLabel(text: splitStrategy == "PERCENTAGE" ? "Percent (must sum to 100)" : "Exact amount per person")
                        ForEach(Array(splitIds).sorted(), id: \.self) { id in
                            let name = participants.first(where: { $0.participantId == id })?.displayName ?? String(id.prefix(8))
                            HStack {
                                Text(name)
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(TripForm.text)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                TripSheetField(
                                    value: Binding(
                                        get: { splitValues[id] ?? "" },
                                        set: { splitValues[id] = $0 }
                                    ),
                                    placeholder: splitStrategy == "PERCENTAGE" ? "%" : "0.00",
                                    keyboardType: .decimalPad
                                )
                                .frame(maxWidth: .infinity)
                            }
                        }
                    }
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Documents")
                Button { showDocImporter = true } label: {
                    HStack {
                        Text(
                            uploadingDoc
                                ? "Uploading…"
                                : (attachmentNames.isEmpty ? "Upload confirmation / ticket" : attachmentNames.joined(separator: ", "))
                        )
                        .font(.plusJakarta(size: 13))
                        .foregroundStyle(TripForm.muted)
                        Spacer()
                        Text("+ Add")
                            .font(.plusJakarta(size: 12, weight: .bold))
                            .foregroundStyle(TripForm.accent)
                    }
                    .padding(12)
                    .background(TripForm.field)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(TripForm.border))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .disabled(uploadingDoc)
            }
        }
        .fileImporter(isPresented: $showDocImporter, allowedContentTypes: [.item], allowsMultipleSelection: false) { result in
            guard case .success(let urls) = result, let url = urls.first else { return }
            Task { await uploadBookingDocument(url: url) }
        }
    }

    private var pollFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Poll Question")
                TripSheetField(value: $primary, placeholder: "Where should we eat on Day 2?")
            }
            VStack(alignment: .leading, spacing: 8) {
                TripFieldLabel(text: "Options")
                TripSheetField(value: $optionA, placeholder: "🍣 Gyoza ChaoChao Restaurant")
                TripSheetField(value: $optionB, placeholder: "🍜 Nishiki Market Street Food")
                if showOption3 {
                    TripSheetField(value: $optionC, placeholder: "Option 3 (optional)")
                }
                Button {
                    showOption3 = true
                } label: {
                    Text("+ Add Option")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(TripForm.purple)
                }
                .buttonStyle(.plain)
            }
            Divider().overlay(TripForm.border)
            TripToggleRow(title: "Anonymous Voting", subtitle: "Hide voters' names in results", isOn: $anonymous)
            TripToggleRow(title: "Allow Multiple Choice", subtitle: "Co-travelers can select multiple options", isOn: $multi)
            Divider().overlay(TripForm.border)
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Poll Deadline")
                TripDeadlineField(date: $dateIso, time: $timeIso, accent: TripForm.purple)
            }
        }
    }

    private var updateFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            TripChipRow(
                options: ["Announcement", "Status", "Question", "Reminder"],
                selected: $updateType,
                accent: TripForm.blue
            )
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Update Message")
                TripSheetField(
                    value: $primary,
                    placeholder: "Road closure on our route…",
                    singleLine: false,
                    minHeight: 100
                )
            }
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Attach Media")
                    HStack(spacing: 8) {
                        Button { showSourcePicker = true } label: {
                            Image(systemName: "photo")
                                .foregroundStyle(TripForm.text)
                                .frame(width: 40, height: 40)
                                .background(TripForm.field)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                        Button { secondary = "link" } label: {
                            Image(systemName: "link")
                                .foregroundStyle(TripForm.text)
                                .frame(width: 40, height: 40)
                                .background(TripForm.field)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Priority")
                    TripSegmentedControl(options: ["Normal", "Urgent"], selected: Binding(
                        get: { priority == "High" ? "Urgent" : "Normal" },
                        set: { priority = $0 == "Urgent" ? "High" : "Medium" }
                    ), accent: Color(hex: "#EF4444"))
                }
            }
            TripToggleRow(
                title: "Notify all members",
                subtitle: "Sends push notifications instantly",
                isOn: $notifyAll,
                accent: TripForm.blue
            )
        }
    }

    private var memoryFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            TripChipRow(
                options: ["Photo", "Milestone", "Lesson", "Reflection"],
                selected: $memoryType,
                accent: TripForm.pink
            )
            Button { showSourcePicker = true } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(TripForm.field.opacity(0.25))
                        .frame(height: 100)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(TripForm.pink, style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                        )
                    if let selectedImage {
                        Image(uiImage: selectedImage)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 100)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "icloud.and.arrow.up")
                                .font(.system(size: 24))
                                .foregroundStyle(TripForm.pink)
                            Text("Upload Media")
                                .font(.plusJakarta(size: 13, weight: .semibold))
                                .foregroundStyle(TripForm.text)
                            Text("Drag & drop or tap to choose files (Max 20MB)")
                                .font(.plusJakarta(size: 11))
                                .foregroundStyle(TripForm.muted)
                        }
                    }
                }
            }
            .buttonStyle(.plain)
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Caption")
                TripSheetField(value: $primary, placeholder: "Incredible golden autumn leaves at Kiyomizudera!")
            }
            if !participants.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Tag People")
                    TripParticipantPicker(participants: participants, selectedIds: $taggedIds, accent: TripForm.pink)
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Location")
                TripSheetField(value: $secondary, placeholder: "Kiyomizu-dera Temple, Kyoto", leadingIcon: "mappin.and.ellipse")
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Mood")
                TripMoodRow(moods: ["🍁", "✨", "📸", "🍜", "🏯", "🙌"], selected: $mood, accent: TripForm.pink)
            }
        }
    }

    private var purchaseFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Label")
                TripSheetField(value: $primary, placeholder: "Item name")
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Amount (optional)")
                TripSheetField(value: $secondary, placeholder: "0.00", keyboardType: .decimalPad)
            }
        }
    }

    private var residentFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Name")
                TripSheetField(value: $primary, placeholder: "Display name")
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Role (optional)")
                TripSheetField(value: $secondary, placeholder: "Roommate / Owner")
            }
        }
    }

    private func combinedIso() -> String? {
        guard !dateIso.isEmpty || !timeIso.isEmpty else { return nil }
        let cal = Calendar.current
        var comps = DateComponents()
        let daySource = !dateIso.isEmpty ? SetupDateTimeUtils.dateFromIso(dateIso) : Date()
        let day = cal.dateComponents([.year, .month, .day], from: daySource)
        comps.year = day.year
        comps.month = day.month
        comps.day = day.day
        if !timeIso.isEmpty {
            let t = cal.dateComponents([.hour, .minute], from: SetupDateTimeUtils.timeFromIso(timeIso))
            comps.hour = t.hour
            comps.minute = t.minute
        } else {
            comps.hour = 0
            comps.minute = 0
        }
        comps.second = 0
        guard let combined = cal.date(from: comps) else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        formatter.timeZone = .current
        return formatter.string(from: combined)
    }

    private func nowIso() -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        formatter.timeZone = .current
        return formatter.string(from: Date())
    }

    private func loadParticipants() async {
        let currencyCtx = await MomentCurrencyContextLoader.loadGroup(momentId: momentId)
        bookingCurrency = currencyCtx.primary
        preferredCurrencies = {
            var seen = Set<String>()
            return ([currencyCtx.primary] + currencyCtx.preferred).filter { seen.insert($0).inserted }
        }()
        do {
            let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            participants = list.filter {
                ($0.status ?? "ACTIVE").uppercased() == "ACTIVE" || ($0.status ?? "").uppercased() == "INVITED"
            }
            assignedIds = Set(participants.map(\.participantId))
            taggedIds = Set(participants.prefix(3).map(\.participantId))
            splitIds = Set(participants.map(\.participantId))
            if bookedById == nil {
                bookedById = participants.first?.participantId
            }
            if paidById == nil {
                paidById = participants.first?.participantId
            }
        } catch {
            // best-effort
        }
        if let prefill = try? await APIClient.shared.getGroupSetupPrefill(momentId: momentId) {
            bookingPlaces = (prefill.places ?? []).filter { $0.placeId != nil }
        }
    }

    private func uploadBookingDocument(url: URL) async {
        uploadingDoc = true
        defer { uploadingDoc = false }
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        do {
            let data = try Data(contentsOf: url)
            let mime = "application/octet-stream"
            let uploadId = try await APIClient.shared.uploadBookingMedia(momentId: momentId, bytes: data, contentType: mime)
            attachmentUploadIds.append(uploadId)
            attachmentNames.append(url.lastPathComponent)
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func normalizeMoney(_ raw: String) -> String? {
        let cleaned = raw.replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else { return nil }
        guard cleaned.range(of: #"^\d+(\.\d{1,4})?$"#, options: .regularExpression) != nil else { return nil }
        return cleaned
    }

    private func bookingTypeCode() -> String {
        switch bookingType {
        case "Hotel": return "HOTEL"
        case "Flight": return "FLIGHT"
        case "Transport": return "TRANSPORT"
        case "Activity": return "ACTIVITY"
        case "Restaurant": return "RESTAURANT"
        default: return "OTHER"
        }
    }

    private func dateTimeIso(date: String, time: String) -> String? {
        guard !date.isEmpty || !time.isEmpty else { return nil }
        let cal = Calendar.current
        var comps = DateComponents()
        let daySource = !date.isEmpty ? SetupDateTimeUtils.dateFromIso(date) : Date()
        let day = cal.dateComponents([.year, .month, .day], from: daySource)
        comps.year = day.year
        comps.month = day.month
        comps.day = day.day
        if !time.isEmpty {
            let t = cal.dateComponents([.hour, .minute], from: SetupDateTimeUtils.timeFromIso(time))
            comps.hour = t.hour
            comps.minute = t.minute
        } else {
            comps.hour = 0
            comps.minute = 0
        }
        comps.second = 0
        guard let combined = cal.date(from: comps) else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        formatter.timeZone = .current
        return formatter.string(from: combined)
    }

    private func save() async {
        let trimmed = primary.trimmingCharacters(in: .whitespacesAndNewlines)
        if kind == .booking {
            guard canSubmit else {
                error = "Required"
                return
            }
        } else if kind != .memory {
            guard !trimmed.isEmpty else {
                error = "Required"
                return
            }
        } else if trimmed.isEmpty && selectedImageData == nil {
            error = "Required"
            return
        }
        busy = true
        error = nil
        do {
            switch kind {
            case .planning:
                let categoryLabel = planCategory.isEmpty
                    ? GroupPlanningCategoryCatalog.defaultLabel(for: momentTypeCode)
                    : planCategory
                let location = secondary.trimmingCharacters(in: .whitespacesAndNewlines)
                let note = notes.trimmingCharacters(in: .whitespacesAndNewlines)
                _ = try await APIClient.shared.createPlanningItem(
                    momentId: momentId,
                    title: trimmed,
                    dueAt: combinedIso(),
                    categoryCode: GroupPlanningCategoryCatalog.code(forLabel: categoryLabel),
                    location: location.isEmpty ? nil : location,
                    priorityCode: GroupPlanningCategoryCatalog.priorityCode(for: priority),
                    description: note.isEmpty ? nil : note
                )
            case .booking:
                let stayBodies: [APIClient.BookingStayBody] = bookingType == "Hotel"
                    ? hotelStays.compactMap { s in
                        let name = s.hotelName.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !name.isEmpty else { return nil }
                        return APIClient.BookingStayBody(
                            hotelName: name,
                            referenceCode: s.referenceCode.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                            amount: normalizeMoney(s.amount),
                            currencyCode: bookingCurrency,
                            startAt: dateTimeIso(date: s.startDate, time: ""),
                            endAt: dateTimeIso(date: s.endDate, time: "")
                        )
                    }
                    : []
                let segmentBodies: [APIClient.BookingFlightSegmentBody] = bookingType == "Flight"
                    ? flightSegments.map { s in
                        APIClient.BookingFlightSegmentBody(
                            legLabel: s.legLabel,
                            airline: s.airline.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                            flightNumber: s.flightNumber.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                            originCode: s.originCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().nilIfEmpty,
                            destinationCode: s.destinationCode.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().nilIfEmpty,
                            seatClass: s.seatClass.nilIfEmpty,
                            seatNumber: s.seatNumber.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty,
                            departAt: dateTimeIso(date: s.departDate, time: s.departTime),
                            arriveAt: dateTimeIso(date: s.arriveDate, time: s.arriveTime)
                        )
                    }
                    : []
                let amount = normalizeMoney(bookingCost) ?? stayBodies.compactMap(\.amount).first
                if linkExpense, let amount {
                    let ids = Array(splitIds).sorted()
                    switch splitStrategy {
                    case "PERCENTAGE":
                        let sum = ids.reduce(0.0) { $0 + (Double(splitValues[$1] ?? "0") ?? 0) }
                        if abs(sum - 100) > 0.01 {
                            error = "Percents must sum to 100 (now \(sum))"
                            busy = false
                            return
                        }
                    case "EXACT":
                        let sum = ids.reduce(Decimal.zero) { acc, id in
                            acc + (Decimal(string: splitValues[id] ?? "0") ?? 0)
                        }
                        let total = Decimal(string: amount) ?? -1
                        if sum != total {
                            error = "Exact amounts must equal booking cost"
                            busy = false
                            return
                        }
                    default:
                        break
                    }
                    if splitStrategy != "POOLED" && ids.isEmpty {
                        error = "Select at least one member for the split"
                        busy = false
                        return
                    }
                    if paidById == nil {
                        error = "Select who paid"
                        busy = false
                        return
                    }
                }
                let resolvedTitle = trimmed.isEmpty
                    ? (stayBodies.first?.hotelName
                        ?? [segmentBodies.first?.airline, segmentBodies.first?.flightNumber].compactMap { $0 }.joined(separator: " ")
                        .nilIfEmpty
                        ?? bookingType)
                    : trimmed
                let headerStart: String? = {
                    switch bookingType {
                    case "Hotel": return stayBodies.first?.startAt
                    case "Flight": return segmentBodies.first?.departAt
                    default: return dateTimeIso(date: dateIso, time: "")
                    }
                }()
                let headerEnd: String? = {
                    switch bookingType {
                    case "Hotel": return stayBodies.last?.endAt
                    case "Flight": return segmentBodies.last?.arriveAt
                    default: return dateTimeIso(date: endDateIso, time: "")
                    }
                }()
                let ref = confirmationNumber.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
                    ?? stayBodies.first?.referenceCode
                let ids = Array(splitIds).sorted()
                let splitInputs: [APIClient.GroupSplitInput]? = {
                    guard linkExpense, amount != nil else { return nil }
                    switch splitStrategy {
                    case "POOLED":
                        return []
                    case "EQUAL":
                        return GroupActionRegistry.equalSplitInputs(participantIds: ids)
                    case "PERCENTAGE":
                        return ids.map { APIClient.GroupSplitInput(participantId: $0, percent: splitValues[$0]) }
                    case "EXACT":
                        return ids.map { APIClient.GroupSplitInput(participantId: $0, amount: splitValues[$0]) }
                    default:
                        return ids.map { APIClient.GroupSplitInput(participantId: $0) }
                    }
                }()
                _ = try await APIClient.shared.createBooking(
                    momentId: momentId,
                    title: resolvedTitle,
                    bookingType: bookingTypeCode(),
                    referenceCode: ref,
                    amount: amount,
                    currencyCode: amount == nil ? nil : bookingCurrency,
                    startAt: headerStart,
                    endAt: headerEnd,
                    bookedAt: headerStart,
                    status: bookingConfirmed ? "CONFIRMED" : "PLANNED",
                    bookedByParticipantId: bookedById,
                    paidByParticipantId: (linkExpense && amount != nil) ? paidById : nil,
                    placeIds: selectedPlaceIds.isEmpty ? nil : Array(selectedPlaceIds),
                    stays: stayBodies.isEmpty ? nil : stayBodies,
                    flightSegments: segmentBodies.isEmpty ? nil : segmentBodies,
                    linkExpense: linkExpense && amount != nil,
                    splitStrategy: (linkExpense && amount != nil) ? splitStrategy : nil,
                    splitInputs: splitInputs,
                    attachmentUploadIds: attachmentUploadIds.isEmpty ? nil : attachmentUploadIds
                )
            case .poll:
                var opts = [
                    optionA.trimmingCharacters(in: .whitespacesAndNewlines),
                    optionB.trimmingCharacters(in: .whitespacesAndNewlines),
                ]
                let c = optionC.trimmingCharacters(in: .whitespacesAndNewlines)
                if !c.isEmpty { opts.append(c) }
                _ = try await APIClient.shared.createPoll(
                    momentId: momentId,
                    question: trimmed,
                    options: opts,
                    closesAt: combinedIso(),
                    pollType: multi ? "MULTI_CHOICE" : "SINGLE_CHOICE"
                )
            case .update:
                _ = try await APIClient.shared.postGroupUpdate(
                    momentId: momentId,
                    message: trimmed,
                    notifyMembers: notifyAll,
                    urgencyCode: GroupPlanningCategoryCatalog.urgencyCode(for: priority == "High" ? "Urgent" : "Normal")
                )
            case .memory:
                let title: String
                if trimmed.isEmpty {
                    title = memoryType
                } else {
                    title = "[\(memoryType)] \(trimmed)"
                }
                if memoryType == "Photo" && selectedImageData == nil {
                    error = "Add a photo before saving"
                    busy = false
                    return
                }
                let created = try await APIClient.shared.createGroupMemory(
                    momentId: momentId,
                    title: title,
                    capturedAt: nowIso()
                )
                let wantsPhoto = selectedImageData != nil || memoryType == "Photo"
                if wantsPhoto {
                    guard let memoryId = created.memoryId else {
                        throw NSError(domain: "Momentra", code: 1, userInfo: [NSLocalizedDescriptionKey: "Memory saved but id missing — photo not attached"])
                    }
                    guard let bytes = selectedImageData else {
                        throw NSError(domain: "Momentra", code: 2, userInfo: [NSLocalizedDescriptionKey: "Could not read the selected photo. Try picking it again."])
                    }
                    _ = try await APIClient.shared.uploadAndAttachMemoryMedia(
                        momentId: momentId,
                        memoryId: memoryId,
                        bytes: bytes,
                        contentType: "image/jpeg"
                    )
                }
            case .purchaseItem:
                let amount = secondary.trimmingCharacters(in: .whitespacesAndNewlines)
                _ = try await APIClient.shared.createPurchaseItem(
                    momentId: momentId,
                    label: trimmed,
                    amount: amount.isEmpty ? nil : amount
                )
            case .resident:
                let role = secondary.trimmingCharacters(in: .whitespacesAndNewlines)
                _ = try await APIClient.shared.addResident(
                    momentId: momentId,
                    name: trimmed,
                    roleCode: role.isEmpty ? nil : role
                )
            }
            isPresented = false
            onSaved()
        } catch {
            self.error = error.localizedDescription
        }
        busy = false
    }
}

private struct TripPickedImageData: Transferable {
    let data: Data

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(importedContentType: .image) { data in
            TripPickedImageData(data: data)
        }
    }
}

private struct TripCameraPicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    @Binding var imageData: Data?
    var onCancel: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        picker.delegate = context.coordinator
        picker.allowsEditing = false
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: TripCameraPicker
        init(_ parent: TripCameraPicker) { self.parent = parent }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onCancel()
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let img = info[.originalImage] as? UIImage {
                parent.image = img
                parent.imageData = img.jpegData(compressionQuality: 0.85)
            }
            parent.onCancel()
        }
    }
}
