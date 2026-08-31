import SwiftUI

/// Figma 546:30147 / 353:22147 Manage Moment + rename + delete sheets.
enum ManageMomentSubsheet: String, Identifiable {
    case rename
    case delete
    var id: String { rawValue }
}

private enum ManageMomentTokens {
    static let sheetBg = Color(hex: "#1A1628")
    static let rowBg = Color(hex: "#14121B")
    static let text = Color(hex: "#F5F2FC")
    static let muted = Color(hex: "#ABA3BA")
    static let purple = Color(hex: "#7C5CFC")
    static let blue = Color(hex: "#3B82F6")
    static let green = Color(hex: "#10B981")
    static let red = Color(hex: "#EF4444")
    static let redText = Color(hex: "#FF5961")
    static let destructiveBg = Color(hex: "#2A1A1A")
}

struct ManageMomentFlowSheet: View {
    let momentId: String
    let momentTitle: String
    @Binding var isPresented: Bool
    var onEditSetup: () -> Void
    var onLifecycleChanged: () -> Void

    @State private var subsheet: ManageMomentSubsheet?
    @State private var busy = false
    @State private var errorText: String?
    @State private var confirmPause = false
    @State private var confirmComplete = false

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture { isPresented = false }

            VStack(spacing: 0) {
                Capsule()
                    .fill(Color.white.opacity(0.2))
                    .frame(width: 40, height: 4)
                    .padding(.top, 12)
                    .padding(.bottom, 16)

                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Manage Moment")
                            .font(.system(size: 19, weight: .bold))
                            .foregroundStyle(ManageMomentTokens.text)
                        Text(momentTitle)
                            .font(.system(size: 10))
                            .foregroundStyle(ManageMomentTokens.muted)
                    }
                    Spacer()
                    Button { isPresented = false } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(ManageMomentTokens.purple)
                            .frame(width: 24, height: 24)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)

                VStack(spacing: 4) {
                    manageRow(
                        icon: "gearshape.fill",
                        well: ManageMomentTokens.purple,
                        title: "Edit setup",
                        subtitle: "Revisit priorities and configuration"
                    ) {
                        isPresented = false
                        onEditSetup()
                    }
                    manageRow(
                        icon: "square.and.pencil",
                        well: ManageMomentTokens.blue,
                        title: "Edit moment name",
                        subtitle: "Rename how this moment appears"
                    ) {
                        subsheet = .rename
                    }
                    manageRow(
                        icon: "pause.fill",
                        well: ManageMomentTokens.blue,
                        title: "Pause rhythm",
                        subtitle: "Pause tracking without losing your data"
                    ) {
                        confirmPause = true
                    }
                    manageRow(
                        icon: "checkmark",
                        well: ManageMomentTokens.green,
                        title: "Complete Chapter",
                        subtitle: "Mark this moment as finished"
                    ) {
                        confirmComplete = true
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)

                Spacer().frame(height: 8)

                Button {
                    subsheet = .delete
                } label: {
                    HStack(spacing: 12) {
                        iconWell(systemName: "trash.fill", well: ManageMomentTokens.red)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Delete permanently")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(ManageMomentTokens.redText)
                            Text("Removes this moment; analytics kept")
                                .font(.system(size: 10))
                                .foregroundStyle(ManageMomentTokens.muted)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(ManageMomentTokens.muted)
                    }
                    .padding(12)
                    .background(ManageMomentTokens.destructiveBg, in: RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(ManageMomentTokens.red, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.bottom, 24)

                if let errorText {
                    Text(errorText)
                        .font(.system(size: 12))
                        .foregroundStyle(ManageMomentTokens.redText)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                }
            }
            .frame(maxWidth: .infinity)
            .background(ManageMomentTokens.sheetBg)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: .black.opacity(0.5), radius: 12, y: -4)
        }
        .sheet(item: $subsheet) { kind in
            switch kind {
            case .rename:
                ManageMomentRenameSheet(
                    momentId: momentId,
                    momentTitle: momentTitle,
                    isPresented: Binding(
                        get: { subsheet != nil },
                        set: { if !$0 { subsheet = nil } }
                    ),
                    onSaved: {
                        subsheet = nil
                        isPresented = false
                        onLifecycleChanged()
                    }
                )
                .presentationDetents([.height(320)])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(20)
                .preferredColorScheme(.dark)
            case .delete:
                ManageMomentDeleteSheet(
                    momentId: momentId,
                    momentTitle: momentTitle,
                    isPresented: Binding(
                        get: { subsheet != nil },
                        set: { if !$0 { subsheet = nil } }
                    ),
                    onDeleted: {
                        subsheet = nil
                        isPresented = false
                        onLifecycleChanged()
                    }
                )
                .presentationDetents([.height(360)])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(20)
                .preferredColorScheme(.dark)
            }
        }
        .confirmationDialog("Pause rhythm?", isPresented: $confirmPause, titleVisibility: .visible) {
            Button("Pause", role: .destructive) {
                Task { await runLifecycle(.archive) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Pause tracking without losing your data.")
        }
        .confirmationDialog("Complete Chapter?", isPresented: $confirmComplete, titleVisibility: .visible) {
            Button("Complete", role: .destructive) {
                Task { await runLifecycle(.cancel) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Mark this moment as finished.")
        }
        .disabled(busy)
    }

    private enum LifecycleAction { case archive, cancel }

    private func runLifecycle(_ action: LifecycleAction) async {
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
            switch action {
            case .archive:
                _ = try await APIClient.shared.archiveMoment(momentId: momentId, expectedVersion: detail.version)
            case .cancel:
                _ = try await APIClient.shared.cancelMoment(momentId: momentId, expectedVersion: detail.version)
            }
            isPresented = false
            onLifecycleChanged()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }

    private func manageRow(
        icon: String,
        well: Color,
        title: String,
        subtitle: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                iconWell(systemName: icon, well: well)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(ManageMomentTokens.text)
                    Text(subtitle)
                        .font(.system(size: 10))
                        .foregroundStyle(ManageMomentTokens.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ManageMomentTokens.muted)
            }
            .padding(12)
            .background(ManageMomentTokens.rowBg, in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func iconWell(systemName: String, well: Color) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 32, height: 32)
            .background(well, in: Circle())
    }
}

struct ManageMomentRenameSheet: View {
    let momentId: String
    let momentTitle: String
    @Binding var isPresented: Bool
    var onSaved: () -> Void

    @State private var name: String = ""
    @State private var busy = false
    @State private var errorText: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Edit Moment Name")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.text)
                    Text(momentTitle)
                        .font(.system(size: 10))
                        .foregroundStyle(ManageMomentTokens.muted)
                }
                Spacer()
                Button { isPresented = false } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.purple)
                }
                .buttonStyle(.plain)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("MOMENT NAME")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(ManageMomentTokens.muted)
                HStack(spacing: 12) {
                    Image(systemName: "square.and.pencil")
                        .foregroundStyle(ManageMomentTokens.blue)
                    TextField("Moment name", text: $name)
                        .foregroundStyle(ManageMomentTokens.text)
                }
                .padding(12)
                .background(ManageMomentTokens.rowBg, in: RoundedRectangle(cornerRadius: 12))
            }

            if let errorText {
                Text(errorText)
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.redText)
            }

            Button {
                Task { await save() }
            } label: {
                if busy {
                    ProgressView().tint(.white).frame(maxWidth: .infinity).padding(.vertical, 12)
                } else {
                    Text("Save")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
            }
            .background(ManageMomentTokens.purple, in: RoundedRectangle(cornerRadius: 12))
            .disabled(busy || name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            Button("Cancel") { isPresented = false }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(ManageMomentTokens.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            Spacer(minLength: 0)
        }
        .padding(16)
        .background(ManageMomentTokens.sheetBg.ignoresSafeArea())
        .onAppear { name = momentTitle }
    }

    private func save() async {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
            _ = try await APIClient.shared.updateMoment(
                momentId: momentId,
                title: trimmed,
                expectedVersion: detail.version
            )
            onSaved()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}

struct ManageMomentDeleteSheet: View {
    let momentId: String
    let momentTitle: String
    @Binding var isPresented: Bool
    var onDeleted: () -> Void

    @State private var busy = false
    @State private var errorText: String?

    var body: some View {
        VStack(spacing: 16) {
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 40, height: 4)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Delete Permanently?")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.text)
                    Text(momentTitle)
                        .font(.system(size: 10))
                        .foregroundStyle(ManageMomentTokens.muted)
                }
                Spacer()
                Button { isPresented = false } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(ManageMomentTokens.purple)
                }
                .buttonStyle(.plain)
            }

            Image(systemName: "trash.fill")
                .font(.system(size: 22))
                .foregroundStyle(.white)
                .frame(width: 48, height: 48)
                .background(ManageMomentTokens.red, in: Circle())

            Text("This cannot be undone. The moment and operational data will be removed; analytics are kept.")
                .font(.system(size: 13))
                .foregroundStyle(ManageMomentTokens.muted)
                .multilineTextAlignment(.center)

            if let errorText {
                Text(errorText)
                    .font(.system(size: 12))
                    .foregroundStyle(ManageMomentTokens.redText)
            }

            Button {
                Task { await delete() }
            } label: {
                if busy {
                    ProgressView().tint(.white).frame(maxWidth: .infinity).padding(.vertical, 12)
                } else {
                    Text("Delete")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
            }
            .background(ManageMomentTokens.red, in: RoundedRectangle(cornerRadius: 12))
            .disabled(busy)

            Button("Cancel") { isPresented = false }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(ManageMomentTokens.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            Spacer(minLength: 0)
        }
        .padding(16)
        .background(ManageMomentTokens.sheetBg.ignoresSafeArea())
    }

    private func delete() async {
        busy = true
        errorText = nil
        defer { busy = false }
        do {
            let detail = try await APIClient.shared.getMomentDetail(momentId: momentId)
            _ = try await APIClient.shared.deleteMoment(momentId: momentId, expectedVersion: detail.version)
            onDeleted()
        } catch {
            errorText = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
