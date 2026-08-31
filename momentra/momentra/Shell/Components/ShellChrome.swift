import SwiftUI

struct MomentraTopBar: View {
    let context: AppContextKind
    let displayName: String?
    let companies: [CompanySummary]
    let selectedCompany: CompanySummary?
    @Binding var companyMenuOpen: Bool
    var onCompanySelected: (CompanySummary) -> Void
    var onQrScan: (() -> Void)? = nil
    var onLife360: () -> Void = {}
    var onNewMoment: () -> Void = {}
    var onAvatar: () -> Void

    private var showsQrScan: Bool {
        (context == .group || context == .business) && onQrScan != nil
    }

    var body: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                MomentraWordmark(
                    showTagline: false,
                    titleSize: 18,
                    taglineSize: 6,
                    alignStart: true
                )

                if context == .business {
                    companyChip
                }
            }

            Spacer(minLength: 8)

            HStack(spacing: 6) {
                if showsQrScan, let onQrScan {
                    roundAction(
                        bg: Color(hex: "#1C233D"),
                        label: "Scan QR to join",
                        action: onQrScan
                    ) {
                        Image("ShellQr")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 12, height: 12)
                    }
                }
                roundAction(bg: GlobalSurfaceTheme.life360.action, label: "Open Life360", action: onLife360) {
                    Image("ShellRadar")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                }
                roundAction(
                    bg: GlobalTheme.createMomentCta,
                    label: "Create moment",
                    hint: "Creates a new moment",
                    action: onNewMoment
                ) {
                    Image("ShellPlus")
                        .resizable()
                        .renderingMode(.template)
                        .foregroundStyle(.white)
                        .frame(width: 12, height: 12)
                }
                Button(action: onAvatar) {
                    ZStack(alignment: .bottomTrailing) {
                        Text(initials)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 32, height: 32)
                            .background(GlobalTheme.actionCircle, in: Circle())
                        Image("ShellStatusDot")
                            .resizable()
                            .frame(width: 7, height: 7)
                            .offset(x: 1, y: 1)
                    }
                }
                .accessibilityLabel("Open profile")
                .accessibilityIdentifier("topbar.profile")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
        .frame(minHeight: 48)
        .background(GlobalTheme.topBarBackground)
        .accessibilityIdentifier("topbar.root")
    }

    private var companyChip: some View {
        Menu {
            ForEach(companies) { company in
                Button(company.displayName) {
                    onCompanySelected(company)
                }
            }
        } label: {
            HStack(spacing: 4) {
                Text(selectedCompany?.displayName ?? (companies.isEmpty ? "No companies" : "Select company"))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .lineLimit(1)
                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 5)
            .background(Color(hex: "#1A2030"), in: RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color(hex: "#3A4258"), lineWidth: 1)
            )
        }
        .disabled(companies.isEmpty)
        .accessibilityLabel(
            selectedCompany.map { "Selected company \($0.displayName)" } ?? "Company selector"
        )
        .accessibilityIdentifier("company.switcher")
    }

    private func roundAction<Content: View>(
        bg: Color,
        label: String,
        hint: String? = nil,
        action: @escaping () -> Void = {},
        @ViewBuilder content: () -> Content
    ) -> some View {
        Button(action: action) {
            content()
                .frame(width: 32, height: 32)
                .background(bg, in: Circle())
        }
        .accessibilityLabel(label)
        .accessibilityIdentifier(
            label == "Open Life360" ? "topbar.life360"
            : label == "Create moment" ? "topbar.new_moment"
            : label == "Scan QR to join" ? "topbar.qr"
            : "topbar.\(label.lowercased().replacingOccurrences(of: " ", with: "_"))"
        )
        .accessibilityHint(hint ?? "")
    }

    private var initials: String {
        let parts = (displayName ?? "")
            .split(whereSeparator: \.isWhitespace)
            .map(String.init)
        if parts.isEmpty { return "M" }
        if parts.count == 1 { return String(parts[0].prefix(2)).uppercased() }
        return "\(parts.first!.prefix(1))\(parts.last!.prefix(1))".uppercased()
    }
}

struct CompactShellChrome: View {
    var onExpand: () -> Void
    var onNewMoment: () -> Void
    var onAvatar: () -> Void

    var body: some View {
        HStack {
            Button(action: onExpand) {
                MomentraWordmark(showTagline: false, titleSize: 16, taglineSize: 6, alignStart: true)
            }
            .buttonStyle(.plain)
            Spacer()
            Button(action: onNewMoment) {
                Text("+")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 32, height: 32)
                    .background(MomentraBrandTokens.cta, in: Circle())
            }
            .buttonStyle(.plain)
            Button(action: onAvatar) {
                Text("··")
                    .font(.system(size: 11))
                    .foregroundStyle(.white)
                    .frame(width: 32, height: 32)
                    .background(Color(hex: "#1E293B"), in: Circle())
            }
            .buttonStyle(.plain)
            Button(action: onExpand) {
                Image(systemName: "chevron.down")
                    .foregroundStyle(.white.opacity(0.7))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Expand top bar")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
        .background(Color(hex: "#0C0F15"))
    }
}

struct ContextSwitcherView: View {
    let selected: AppContextKind
    let supportedContexts: [AppContextKind]
    var onSelect: (AppContextKind) -> Void

    var body: some View {
        let contexts = supportedContexts.isEmpty ? [AppContextKind.personal] : supportedContexts
        HStack(spacing: 0) {
            ForEach(contexts) { context in
                let isSelected = context == selected
                let accent = ContextTheme.of(context).contextAccent
                Button {
                    onSelect(context)
                } label: {
                    Text(context.label)
                        .font(.system(size: 12, weight: isSelected ? .semibold : .medium))
                        .foregroundStyle(isSelected ? Color.white : GlobalTheme.contextUnselected)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                        .background(
                            isSelected ? accent : Color.clear,
                            in: RoundedRectangle(cornerRadius: 8)
                        )
                }
                .accessibilityLabel("Switch to \(context.label)")
                .accessibilityIdentifier("context.\(context.rawValue.lowercased())")
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
        .frame(height: 36)
        .background(GlobalTheme.topBarBackground)
        .accessibilityIdentifier("context.switcher")
    }
}

struct MomentSwitcherView: View {
    let selectedTitle: String?
    let selectedMomentId: String?
    let activeMoments: [(String, String)]
    let isEmpty: Bool
    let isLoading: Bool
    var accent: Color = Color(hex: "#E8621A")
    var onSelectMoment: (String) -> Void = { _ in }
    var onSettings: () -> Void = {}

    @State private var expanded = false

    var body: some View {
        let title: String = {
            if isLoading { return "Loading moments…" }
            if isEmpty { return "No moments yet" }
            if let selectedTitle, !selectedTitle.isEmpty { return selectedTitle }
            return "Select moment"
        }()
        let pills: [(String, String)] = {
            if isLoading || isEmpty { return [] }
            if !activeMoments.isEmpty { return activeMoments }
            if let id = selectedMomentId, let title = selectedTitle, !title.isEmpty {
                return [(id, title)]
            }
            return []
        }()
        let canExpand = pills.count > 1
        let canOpenSettings = selectedMomentId != nil && !isEmpty && !isLoading

        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Button {
                    guard canExpand else { return }
                    withAnimation(.easeInOut(duration: 0.2)) { expanded.toggle() }
                } label: {
                    HStack(spacing: 8) {
                        Circle().fill(accent).frame(width: 7, height: 7)
                        Text(title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(MomentraBrandTokens.textOnDark)
                            .lineLimit(1)
                        Spacer(minLength: 0)
                    }
                }
                .buttonStyle(.plain)

                Button(action: onSettings) {
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(accent)
                        .frame(width: 28, height: 28)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(!canOpenSettings)
                .opacity(canOpenSettings ? 1 : 0.4)
                .accessibilityLabel("Moment settings")
                .accessibilityIdentifier("moment.switcher.settings")

                if canExpand {
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { expanded.toggle() }
                    } label: {
                        Image(systemName: expanded ? "chevron.up" : "chevron.down")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(MomentraBrandTokens.textOnDark)
                            .frame(width: 24, height: 28)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)

            if expanded && !pills.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("When Module.")
                        .font(.system(size: 11))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(pills, id: \.0) { momentId, pill in
                                let selected = momentId == selectedMomentId
                                Button {
                                    onSelectMoment(momentId)
                                } label: {
                                    Text(pill)
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundStyle(selected ? .black : accent)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(
                                            selected ? accent : Color.clear,
                                            in: Capsule()
                                        )
                                        .overlay(Capsule().stroke(accent, lineWidth: 1))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 12)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#161B26"), in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
        .background(Color(hex: "#0C0F15"))
        .accessibilityLabel("Moment switcher: \(title)")
        .accessibilityIdentifier("moment.switcher")
    }
}
