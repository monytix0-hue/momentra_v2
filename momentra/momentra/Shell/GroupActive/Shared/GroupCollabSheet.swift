import PhotosUI
import SwiftUI
import UIKit

enum GroupCollabKind: String, Identifiable {
    case planning, booking, poll, update, memory, purchaseItem, resident
    var id: String { rawValue }
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
    @State private var bookedById: String?
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
        case .booking: return "Add Booking"
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
        case .booking: return "Add Booking"
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
        VStack(alignment: .leading, spacing: 12) {
            TripChipRow(
                options: ["Hotel", "Flight", "Transport", "Activity", "Restaurant"],
                selected: $bookingType,
                accent: TripForm.accent
            )
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Booking Name")
                TripSheetField(value: $primary, placeholder: "MIMARU Kyoto Stay")
            }
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Confirmation #")
                    TripSheetField(value: $confirmationNumber, placeholder: "MMR-98402X")
                }
                VStack(alignment: .leading, spacing: 6) {
                    TripFieldLabel(text: "Cost (₹)")
                    TripSheetField(value: $bookingCost, placeholder: "42,500", keyboardType: .decimalPad)
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                TripFieldLabel(text: "Date Range (Check-In / Check-Out)")
                TripDateRangeField(start: $dateIso, end: $endDateIso)
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
        do {
            let list = try await APIClient.shared.listGroupParticipants(momentId: momentId)
            participants = list.filter {
                ($0.status ?? "ACTIVE").uppercased() == "ACTIVE" || ($0.status ?? "").uppercased() == "INVITED"
            }
            assignedIds = Set(participants.map(\.participantId))
            taggedIds = Set(participants.prefix(3).map(\.participantId))
            if bookedById == nil {
                bookedById = participants.first?.participantId
            }
        } catch {
            // best-effort
        }
    }

    private func save() async {
        let trimmed = primary.trimmingCharacters(in: .whitespacesAndNewlines)
        if kind != .memory {
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
                _ = try await APIClient.shared.createBooking(
                    momentId: momentId,
                    title: trimmed,
                    bookedAt: combinedIso() ?? SetupDateTimeUtils.isoDateToStartInstant(dateIso)
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
