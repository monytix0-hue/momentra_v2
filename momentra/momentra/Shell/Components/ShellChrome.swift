import SwiftUI
import UIKit

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
    var onRefer: () -> Void = {}
    var onAvatar: () -> Void
    var referAvailable: Bool = true

    /// Figma: company chip only when Business + selected company (`692:34971`).
    private var showCompanyChip: Bool {
        context == .business && selectedCompany != nil
    }

    private var showsQrScan: Bool {
        (context == .group || context == .business) && onQrScan != nil
    }

    private var createLabel: String {
        showCompanyChip ? "Moments" : "New"
    }

    private let actionBg = Color(hex: "#1C233D")
    private let labelMuted = Color(hex: "#ABA3BA")

    var body: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                MomentraWordmark(
                    showTagline: true,
                    titleSize: 16,
                    taglineSize: 5.5,
                    alignStart: true
                )

                if showCompanyChip {
                    companyChip
                }
            }

            Spacer(minLength: 8)

            HStack(spacing: 5) {
                if showsQrScan, let onQrScan {
                    labeledAction(
                        caption: "QR",
                        bg: actionBg,
                        a11y: "Scan QR to join",
                        id: "topbar.qr",
                        captionColor: .white.opacity(0.86),
                        action: onQrScan
                    ) {
                        Image("ShellQr")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 12, height: 12)
                    }
                }
                labeledAction(
                    caption: "360",
                    bg: GlobalSurfaceTheme.life360.action,
                    a11y: "Open Life360",
                    id: "topbar.life360",
                    captionColor: .white.opacity(0.86),
                    action: onLife360
                ) {
                    Image("ShellRadar")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 12, height: 12)
                }
                labeledAction(
                    caption: createLabel,
                    bg: GlobalTheme.createMomentCta,
                    a11y: showCompanyChip ? "Open moments" : "Create moment",
                    id: "topbar.new_moment",
                    captionColor: .white.opacity(0.92),
                    action: onNewMoment
                ) {
                    Image("ShellPlus")
                        .resizable()
                        .renderingMode(.template)
                        .foregroundStyle(.white)
                        .frame(width: 10, height: 10)
                }
                if referAvailable {
                    labeledAction(
                        caption: "Refer",
                        bg: actionBg,
                        a11y: "Refer a friend",
                        id: "topbar.refer",
                        captionColor: labelMuted,
                        action: onRefer
                    ) {
                        Image("ShellGift")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 12, height: 12)
                    }
                }
                Button(action: onAvatar) {
                    ZStack(alignment: .bottomTrailing) {
                        Text(initials)
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 36, height: 36)
                            .background(GlobalTheme.actionCircle, in: Circle())
                        Image("ShellStatusDot")
                            .resizable()
                            .frame(width: 8, height: 8)
                            .offset(x: 1, y: 1)
                    }
                }
                .accessibilityLabel("Open profile")
                .accessibilityIdentifier("topbar.profile")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .frame(minHeight: 56)
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
            HStack(spacing: 6) {
                Text(selectedCompany?.displayName ?? "Company")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .lineLimit(1)
                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.8))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color(hex: "#1E293B"), in: RoundedRectangle(cornerRadius: 8))
        }
        .accessibilityLabel(
            selectedCompany.map { "Selected company \($0.displayName)" } ?? "Company selector"
        )
        .accessibilityIdentifier("company.switcher")
    }

    private func labeledAction<Content: View>(
        caption: String,
        bg: Color,
        a11y: String,
        id: String,
        captionColor: Color,
        action: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                content()
                    .frame(width: 28, height: 28)
                    .background(bg, in: Circle())
                Text(caption)
                    .font(.system(size: 8.5, weight: .medium))
                    .foregroundStyle(captionColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(width: 32, height: 50, alignment: .top)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(a11y)
        .accessibilityIdentifier(id)
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
    var onInvite: (() -> Void)? = nil

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

                if let onInvite {
                    Button(action: onInvite) {
                        Image("GroupQaUserPlus")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 16, height: 16)
                            .foregroundStyle(accent)
                            .frame(width: 28, height: 28)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .disabled(!canOpenSettings)
                    .opacity(canOpenSettings ? 1 : 0.4)
                    .accessibilityLabel("Invite people")
                    .accessibilityIdentifier("moment.switcher.invite")
                }

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

// MARK: - Native NavigationStack toolbar (replaces stacked top chrome)

struct ShellToolbarContent: ToolbarContent {
    let context: AppContextKind
    let displayName: String?
    let companies: [CompanySummary]
    let selectedCompany: CompanySummary?
    let showCompanyMenu: Bool
    var onCompanySelect: (CompanySummary) -> Void
    var onQrScan: (() -> Void)?
    var onLife360: () -> Void
    var onNewMoment: () -> Void
    var onRefer: () -> Void
    var onAvatar: () -> Void

    private var showsQrScan: Bool {
        (context == .group || context == .business) && onQrScan != nil
    }

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            MomentraWordmark(showTagline: false, titleSize: 16, taglineSize: 6, alignStart: true)
        }

        ToolbarItem(placement: .principal) {
            if showCompanyMenu, let selectedCompany {
                Menu {
                    ForEach(companies) { company in
                        Button(company.displayName) { onCompanySelect(company) }
                    }
                } label: {
                    Label(selectedCompany.displayName, systemImage: "building.2")
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                }
                .accessibilityIdentifier("company.switcher")
            }
        }

        ToolbarItemGroup(placement: .topBarTrailing) {
            if showsQrScan, let onQrScan {
                Button(action: onQrScan) {
                    Image("ShellQr")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 18, height: 18)
                }
                .accessibilityLabel("Scan QR to join")
                .accessibilityIdentifier("topbar.qr")
            }
            Button(action: onLife360) {
                Image("ShellRadar")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
            .accessibilityLabel("Open Life360")
            .accessibilityIdentifier("topbar.life360")

            Button(action: onNewMoment) {
                Image("ShellPlus")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
            }
            .accessibilityLabel(context == .business ? "Open moments" : "Create moment")
            .accessibilityIdentifier("topbar.new_moment")

            Button(action: onRefer) {
                Image("ShellGift")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
            }
            .accessibilityLabel("Refer a friend")
            .accessibilityIdentifier("topbar.refer")

            Button(action: onAvatar) {
                Text(profileInitials)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 32, height: 32)
                    .background(GlobalTheme.actionCircle, in: Circle())
            }
            .accessibilityLabel("Open profile")
            .accessibilityIdentifier("topbar.profile")
        }
    }

    private var profileInitials: String {
        let parts = (displayName ?? "")
            .split(whereSeparator: \.isWhitespace)
            .map(String.init)
        if parts.isEmpty { return "M" }
        if parts.count == 1 { return String(parts[0].prefix(2)).uppercased() }
        return "\(parts.first!.prefix(1))\(parts.last!.prefix(1))".uppercased()
    }
}

struct ShellContextInset: View {
    let selected: AppContextKind
    let supportedContexts: [AppContextKind]
    var onSelect: (AppContextKind) -> Void

    var body: some View {
        ContextSwitcherView(
            selected: selected,
            supportedContexts: supportedContexts,
            onSelect: onSelect
        )
    }
}

// MARK: - Bottom navigation (native UITabBar via SwiftUI TabView)

private let shellTabOrder: [BottomDestination] = [.pulse, .moments, .create, .life, .memory]

struct NativeShellTabView<Content: View>: View {
    @Binding var selection: BottomDestination
    let accent: Color
    @ViewBuilder var content: () -> Content

    var body: some View {
        TabView(selection: $selection) {
            ForEach(shellTabOrder) { destination in
                tabRoot(for: destination)
                    .tag(destination)
                    .tabItem { tabLabel(for: destination) }
            }
        }
        .tint(accent)
        .toolbarBackground(GlobalTheme.bottomBarBackground, for: .tabBar)
        .toolbarBackground(.visible, for: .tabBar)
        .onAppear(perform: Self.configureTabBarAppearance)
        .accessibilityIdentifier("bottom.nav")
    }

    @ViewBuilder
    private func tabRoot(for destination: BottomDestination) -> some View {
        if selection == destination {
            content()
        } else {
            Color.clear
        }
    }

    @ViewBuilder
    private func tabLabel(for destination: BottomDestination) -> some View {
        if destination == .create {
            Label(destination.shellNavLabel, systemImage: destination.systemImage)
        } else {
            Label(destination.shellNavLabel, image: destination.tabAssetName)
        }
    }

    static func configureTabBarAppearance() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(GlobalTheme.bottomBarBackground)
        let unselected = UIColor(GlobalTheme.bottomUnselected)
        let normal = appearance.stackedLayoutAppearance.normal
        normal.iconColor = unselected
        normal.titleTextAttributes = [.foregroundColor: unselected]
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }
}

/// Legacy custom bar kept for reference / previews; app shell uses `NativeShellTabView`.
struct ShellBottomNavigationView: View {
    let selected: BottomDestination
    let accent: Color
    var onSelect: (BottomDestination) -> Void

    private let barHeight: CGFloat = 72
    private let iconSize: CGFloat = 22
    private let fabSize: CGFloat = 36
    private let plusIconSize: CGFloat = 16

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                bottomTab(.pulse)
                bottomTab(.moments)
                createFab
                bottomTab(.life)
                bottomTab(.memory)
            }
            .padding(.horizontal, 12)
            .padding(.top, 4)
            .padding(.bottom, 8)
            .frame(height: barHeight)
        }
        .frame(maxWidth: .infinity)
        .background(GlobalTheme.bottomBarBackground)
        .background(GlobalTheme.bottomBarBackground.ignoresSafeArea(edges: .bottom))
        .accessibilityIdentifier("bottom.nav")
    }

    private func bottomTab(_ destination: BottomDestination) -> some View {
        let isSelected = selected == destination
        let tint = isSelected ? accent : GlobalTheme.bottomUnselected
        return Button {
            onSelect(destination)
        } label: {
            VStack(spacing: 4) {
                Image(destination.tabAssetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize)
                    .foregroundStyle(tint)
                Text(destination.shellNavLabel)
                    .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                    .foregroundStyle(tint)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityLabel(destination.shellNavLabel)
        .accessibilityIdentifier(bottomA11yId(destination))
    }

    private var createFab: some View {
        let isSelected = selected == .create
        return Button {
            onSelect(.create)
        } label: {
            VStack(spacing: 0) {
                ZStack {
                    Circle()
                        .fill(accent)
                        .frame(width: fabSize, height: fabSize)
                    Image("ShellPlus")
                        .resizable()
                        .scaledToFit()
                        .frame(width: plusIconSize, height: plusIconSize)
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Quickadds")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityIdentifier("bottom.quickadd")
    }

    private func bottomA11yId(_ destination: BottomDestination) -> String {
        switch destination {
        case .pulse: return "bottom.pulse"
        case .moments: return "bottom.moments"
        case .create: return "bottom.quickadd"
        case .life: return "bottom.life"
        case .memory: return "bottom.memory"
        }
    }
}
