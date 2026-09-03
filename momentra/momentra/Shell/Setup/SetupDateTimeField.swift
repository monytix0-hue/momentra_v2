import SwiftUI

struct SetupDateField: View {
    let label: String
    @Binding var isoValue: String?
    var testTag: String = "setup.date"

    @State private var showPicker = false
    @State private var draftDate = Date()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SetupTokens.textPrimary)
            Button {
                draftDate = SetupDateTimeUtils.dateFromIso(isoValue)
                showPicker = true
            } label: {
                HStack {
                    Text(SetupDateTimeUtils.formatDateDisplay(isoValue))
                        .font(.system(size: 15))
                        .foregroundStyle(isoValue == nil ? SetupTokens.textSecondary : .white)
                    Spacer()
                    Text("▼")
                        .font(.system(size: 12))
                        .foregroundStyle(SetupTokens.textSecondary)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(SetupTokens.bizCard, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: "#1E293B"), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier(testTag)
        }
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("", selection: $draftDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle(label)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showPicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                isoValue = SetupDateTimeUtils.localDateString(from: draftDate)
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium])
        }
    }
}

struct SetupDateRangeField: View {
    let label: String
    @Binding var startIso: String?
    @Binding var endIso: String?
    var testTag: String = "setup.date.range"

    var body: some View {
        VStack(spacing: 12) {
            SetupDateField(
                label: "\(label) (start)",
                isoValue: $startIso,
                testTag: "\(testTag).start"
            )
            SetupDateField(
                label: "\(label) (end)",
                isoValue: $endIso,
                testTag: "\(testTag).end"
            )
        }
        .accessibilityIdentifier(testTag)
    }
}

struct SetupDateTimeField: View {
    let label: String
    @Binding var isoValue: String?
    var testTag: String = "setup.datetime"

    @State private var showPicker = false
    @State private var draftDate = Date()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(SetupTokens.textPrimary)
            Button {
                showPicker = true
            } label: {
                HStack {
                    Text(SetupDateTimeUtils.formatDateTimeDisplay(isoValue))
                        .font(.system(size: 15))
                        .foregroundStyle(isoValue == nil ? SetupTokens.textSecondary : .white)
                    Spacer()
                    Text("▼")
                        .font(.system(size: 12))
                        .foregroundStyle(SetupTokens.textSecondary)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(SetupTokens.bizCard, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color(hex: "#1E293B"), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier(testTag)
        }
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("", selection: $draftDate, displayedComponents: [.date, .hourAndMinute])
                    .datePickerStyle(.graphical)
                    .labelsHidden()
                    .padding()
                    .navigationTitle(label)
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showPicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("OK") {
                                isoValue = SetupDateTimeUtils.localDateTimeString(from: draftDate)
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
        .onAppear {
            if let isoValue {
                draftDate = SetupDateTimeUtils.dateFromIso(isoValue)
            }
        }
    }
}
