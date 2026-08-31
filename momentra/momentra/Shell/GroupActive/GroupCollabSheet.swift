import PhotosUI
import SwiftUI
import UIKit

enum GroupCollabKind: String, Identifiable {
    case planning, booking, poll, update, memory, purchaseItem, resident
    var id: String { rawValue }
}

/// Figma 575:15497 Trip Quick Add sheets — live APIs + ISO date/time fields.
struct GroupCollabSheet: View {
    let kind: GroupCollabKind
    let momentId: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void = {}

    @State private var primary = ""
    @State private var secondary = ""
    @State private var optionA = ""
    @State private var optionB = ""
    @State private var optionC = ""
    @State private var anonymous = true
    @State private var multi = false
    @State private var date = Date()
    @State private var time = Date()
    @State private var useDate = false
    @State private var useTime = false
    @State private var memoryType = "Photo"
    @State private var busy = false
    @State private var error: String?
    @State private var showSourcePicker = false
    @State private var showCamera = false
    @State private var showLibrary = false
    @State private var pickedItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var selectedImageData: Data?

    private let sheetBg = TripSheetTokens.bg
    private let fieldBg = TripSheetTokens.field
    private let border = TripSheetTokens.border
    private let muted = TripSheetTokens.muted

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    header
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
                    Button {
                        Task { await save() }
                    } label: {
                        if busy {
                            ProgressView().tint(.white).frame(maxWidth: .infinity).padding(.vertical, 16)
                        } else {
                            Text(ctaLabel)
                                .font(.plusJakarta(size: 15, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                        }
                    }
                    .background(ctaGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .disabled(busy || !canSubmit)
                    .opacity(canSubmit ? 1 : 0.55)
                }
                .padding(24)
            }
            .background(sheetBg)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { isPresented = false }
                        .foregroundStyle(Color(hex: "#A855F7"))
                }
            }
        }
        .presentationDetents([.large])
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

    private var header: some View {
        HStack(spacing: 12) {
            Text(headerGlyph)
                .font(.system(size: 16))
                .frame(width: 36, height: 36)
                .background(headerAccent.opacity(0.18))
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(headerAccent.opacity(0.35)))
                .clipShape(RoundedRectangle(cornerRadius: 18))
            VStack(alignment: .leading, spacing: 2) {
                Text(titleText)
                    .font(.plusJakarta(size: 18, weight: .heavy))
                    .foregroundStyle(TripSheetTokens.text)
                Text(subtitleText)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(muted)
            }
        }
    }

    private var headerGlyph: String {
        switch kind {
        case .planning: return "📍"
        case .booking: return "🏨"
        case .poll: return "📊"
        case .update: return "✏️"
        case .memory: return "📷"
        case .purchaseItem: return "🛒"
        case .resident: return "🏠"
        }
    }

    private var headerAccent: Color {
        switch kind {
        case .planning: return Color(hex: "#14B8A6")
        case .booking: return TripSheetTokens.accent
        case .poll: return Color(hex: "#A855F7")
        case .update: return Color(hex: "#3B82F6")
        case .memory: return Color(hex: "#FF8E63")
        default: return TripSheetTokens.accent
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
        case .booking: return "Reserve stays, rides, or tickets"
        case .poll: return "Vote on activities with your travel group"
        case .update: return "Share a status with your travel group"
        case .memory: return "Save a snippet of your trip for the shared journal"
        case .purchaseItem: return "Track something the group is buying"
        case .resident: return "Add someone to the household roster"
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

    private var ctaGradient: LinearGradient {
        let colors: [Color]
        switch kind {
        case .planning: colors = [Color(hex: "#14B8A6"), Color(hex: "#0F766E")]
        case .booking: colors = [TripSheetTokens.accent, TripSheetTokens.accentEnd]
        case .poll: colors = [Color(hex: "#A855F7"), Color(hex: "#C084FC")]
        case .update: colors = [Color(hex: "#3B82F6"), Color(hex: "#1D4ED8")]
        case .memory: colors = [Color(hex: "#FF8E63"), Color(hex: "#E8744F")]
        default: colors = [TripSheetTokens.accent, TripSheetTokens.accentEnd]
        }
        return LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing)
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
        VStack(alignment: .leading, spacing: 12) {
            labeledField("Plan Title", text: $primary, placeholder: "Dolphin Watching & Sunset Cruise")
            HStack(spacing: 12) {
                dateToggleField
                timeToggleField
            }
            // Location UI is local-only — not submitted.
            labeledField("Location", text: $secondary, placeholder: "Coco Beach, Nerul")
        }
    }

    private var bookingFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            labeledField("Booking Title", text: $primary, placeholder: "Hotel / Flight / Activity")
            HStack(spacing: 12) {
                dateToggleField
                timeToggleField
            }
        }
    }

    private var pollFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            labeledField("Poll Question", text: $primary, placeholder: "Where should we eat on Day 2?")
            labeledField("Option 1", text: $optionA, placeholder: "Option A")
            labeledField("Option 2", text: $optionB, placeholder: "Option B")
            labeledField("Option 3 (optional)", text: $optionC, placeholder: "Option C")
            toggleRow("Anonymous Voting", "Hide voters' names in results", $anonymous)
            toggleRow("Allow Multiple Choice", "Co-travelers can select multiple options", $multi)
            Text("POLL DEADLINE")
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(muted)
            HStack(spacing: 12) {
                dateToggleField
                timeToggleField
            }
        }
    }

    private var updateFields: some View {
        labeledField("Update", text: $primary, placeholder: "What's happening?", axis: .vertical)
    }

    private var memoryFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(["Photo", "Milestone", "Lesson", "Reflection"], id: \.self) { chip in
                        Text(chip)
                            .font(.plusJakarta(size: 12, weight: .semibold))
                            .foregroundStyle(memoryType == chip ? .white : muted)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(memoryType == chip ? Color(hex: "#FF8E63") : fieldBg)
                            .clipShape(Capsule())
                            .onTapGesture { memoryType = chip }
                    }
                }
            }
            Button { showSourcePicker = true } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(fieldBg)
                        .frame(height: 120)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(border))
                    if let selectedImage {
                        Image(uiImage: selectedImage)
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    } else {
                        Text("＋  Camera / gallery")
                            .font(.plusJakarta(size: 14))
                            .foregroundStyle(muted)
                    }
                }
            }
            .buttonStyle(.plain)
            if selectedImage != nil {
                Text("Photo attached · tap to change")
                    .font(.plusJakarta(size: 11))
                    .foregroundStyle(Color(hex: "#A855F7"))
            }
            labeledField("Caption", text: $primary, placeholder: "What made this special?", axis: .vertical)
        }
    }

    private var purchaseFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            labeledField("Label", text: $primary, placeholder: "Item name")
            labeledField("Amount (optional)", text: $secondary, placeholder: "0.00")
        }
    }

    private var residentFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            labeledField("Name", text: $primary, placeholder: "Display name")
            labeledField("Role (optional)", text: $secondary, placeholder: "Roommate / Owner")
        }
    }

    private var dateToggleField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Toggle("Date", isOn: $useDate)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(muted)
                .tint(Color(hex: "#A855F7"))
            if useDate {
                DatePicker("", selection: $date, displayedComponents: .date)
                    .labelsHidden()
                    .colorScheme(.dark)
                    .padding(8)
                    .background(fieldBg)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var timeToggleField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Toggle("Time", isOn: $useTime)
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(muted)
                .tint(Color(hex: "#A855F7"))
            if useTime {
                DatePicker("", selection: $time, displayedComponents: .hourAndMinute)
                    .labelsHidden()
                    .colorScheme(.dark)
                    .padding(8)
                    .background(fieldBg)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func labeledField(_ label: String, text: Binding<String>, placeholder: String, axis: Axis = .horizontal) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label.uppercased())
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(muted)
            TextField(placeholder, text: text, axis: axis)
                .font(.plusJakarta(size: 14))
                .foregroundStyle(.white)
                .padding(12)
                .background(fieldBg)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(border))
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    private func toggleRow(_ title: String, _ subtitle: String, _ value: Binding<Bool>) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.plusJakarta(size: 14, weight: .semibold)).foregroundStyle(.white)
                Text(subtitle).font(.plusJakarta(size: 11)).foregroundStyle(muted)
            }
            Spacer()
            Toggle("", isOn: value).labelsHidden().tint(Color(hex: "#A855F7"))
        }
    }

    /// Combine date + time pickers into ISO-8601 with offset for Zod `.datetime()`.
    private func combinedIso() -> String? {
        guard useDate || useTime else { return nil }
        let cal = Calendar.current
        var comps = DateComponents()
        let daySource = useDate ? date : Date()
        let day = cal.dateComponents([.year, .month, .day], from: daySource)
        comps.year = day.year
        comps.month = day.month
        comps.day = day.day
        if useTime {
            let t = cal.dateComponents([.hour, .minute], from: time)
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
                _ = try await APIClient.shared.createPlanningItem(
                    momentId: momentId,
                    title: trimmed,
                    dueAt: combinedIso()
                )
            case .booking:
                _ = try await APIClient.shared.createBooking(
                    momentId: momentId,
                    title: trimmed,
                    bookedAt: combinedIso()
                )
            case .poll:
                // Anonymous is UI-only — do not prefix [anon] into the question.
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
                _ = try await APIClient.shared.postGroupUpdate(momentId: momentId, message: trimmed)
            case .memory:
                let title: String
                if trimmed.isEmpty {
                    title = memoryType
                } else {
                    title = "[\(memoryType)] \(trimmed)"
                }
                let created = try await APIClient.shared.createGroupMemory(
                    momentId: momentId,
                    title: title,
                    capturedAt: nowIso()
                )
                if let memoryId = created.memoryId, let bytes = selectedImageData {
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
