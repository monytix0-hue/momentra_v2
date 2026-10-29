import SwiftUI

/// Figma 658:9451 (Choose a Moment) + 658:9573 (Choose a Memory).
struct BusinessCreateMomentView: View {
    var onBack: () -> Void
    var onSelectSetup: (BusinessSetupKind) -> Void
    var companyId: String? = nil

    enum Tab: String, CaseIterable {
        case createMoment = "Create a Moment"
        case chooseMemory = "Choose a Memory"
    }

    @State private var tab: Tab = .createMoment
    @State private var search = ""
    @State private var filter = "All"

    private var showRetailTagline: Bool {
        BusinessAudience.isSmallShop(BusinessAudience.forCompany(companyId: companyId))
    }

    private struct Category: Identifiable {
        let id = UUID()
        let title: String
        let body: String
        let icon: String
        let card: String
        let comingSoon: Bool
        let setupKind: BusinessSetupKind?
        let testTag: String?
    }

    private struct MemoryItem: Identifiable {
        let id = UUID()
        let title: String
        let body: String
        let tag: String
        let date: String
        let dotAsset: String
    }

    private let categories: [Category] = BusinessSetupKind.allCases.map { kind in
        Category(title: kind.title, body: kind.body, icon: "biz_create_layers", card: "biz_create_card_project", comingSoon: false, setupKind: kind, testTag: kind.maestroTag)
    }

    private var visibleCategories: [Category] {
        return categories.filter { cat in
            guard let kind = cat.setupKind else { return true }
            guard let key = CompanyModules.moduleKey(for: kind) else { return true }
            return CompanyModules.isEnabled(companyId: companyId, key: key)
        }
    }

    private let memories: [MemoryItem] = []

    private let filters = ["All", "Revenue", "Team", "Strategy", "Projects", "Expenses"]

    private var filteredMemories: [MemoryItem] {
        memories.filter { item in
            (filter == "All" || item.tag == filter) &&
                (search.isEmpty || item.title.localizedCaseInsensitiveContains(search) || item.body.localizedCaseInsensitiveContains(search))
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                header
                if !showRetailTagline {
                    tabSwitcher
                }
                if tab == .createMoment || showRetailTagline {
                    momentContent
                } else {
                    memoryContent
                }
            }
            .padding(.top, 12)
            .padding(.bottom, 40)
        }
        .background(BusinessSheetTheme.bg.ignoresSafeArea())
        .businessEmptyAppear()
    }

    private var header: some View {
        VStack(spacing: 16) {
            Capsule()
                .fill(BusinessSheetTheme.border)
                .frame(width: 40, height: 4)
            HStack {
                Text(tab == .createMoment ? "Choose a Moment" : "Choose a Memory")
                    .font(.plusJakarta(size: 20, weight: .bold))
                    .foregroundStyle(.white)
                Spacer(minLength: 0)
                Button(action: onBack) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(tab == .createMoment ? BusinessSheetTheme.border : BusinessSheetTheme.card)
                            .frame(width: 32, height: 32)
                        Image("biz_create_x")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 16, height: 16)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }
            if showRetailTagline {
                Text("Built for shops and small teams in India.")
                    .font(.plusJakarta(size: 13, weight: .medium))
                    .foregroundStyle(Color(hex: "#94A3B8"))
            }
        }
        .padding(.horizontal, 20)
    }

    private var tabSwitcher: some View {
        HStack(spacing: 4) {
            ForEach(Tab.allCases, id: \.self) { item in
                Button {
                    tab = item
                } label: {
                    Text(item.rawValue)
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(tabForeground(item))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(tab == item ? BusinessEmptyTokens.accent : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: tab == .createMoment ? 100 : 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(tab == .createMoment ? Color(hex: "#111520") : BusinessSheetTheme.card)
        .overlay(
            RoundedRectangle(cornerRadius: tab == .createMoment ? 100 : 12)
                .stroke(BusinessSheetTheme.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: tab == .createMoment ? 100 : 12))
        .padding(.horizontal, 20)
    }

    private func tabForeground(_ item: Tab) -> Color {
        if tab == item {
            return tab == .createMoment ? BusinessSheetTheme.bg : .white
        }
        return Color(hex: "#94A3B8")
    }

    private var momentContent: some View {
        VStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Run every part of your business with clarity.")
                        .font(.plusJakarta(size: 24, weight: .bold))
                        .foregroundStyle(.white)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("Create moments to track operations, revenue, strategy, and growth — all in one place.")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(Color(hex: "#94A3B8"))
                        .fixedSize(horizontal: false, vertical: true)
                }
                HStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 1)
                        .fill(BusinessEmptyTokens.accent)
                        .frame(width: 24, height: 2)
                    Circle()
                        .fill(BusinessEmptyTokens.accent.opacity(0.35))
                        .frame(width: 6, height: 6)
                }
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                LinearGradient(
                    colors: [BusinessEmptyTokens.accent.opacity(0.145), Color(hex: "#161B26").opacity(0)],
                    startPoint: .top,
                    endPoint: .center
                )
                .background(Color(hex: "#161B26"))
            )
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(BusinessSheetTheme.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .padding(.horizontal, 20)

            VStack(alignment: .leading, spacing: 16) {
                Text("Choose the part of the business you want to manage.")
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(Color(hex: "#94A3B8"))
                ForEach(visibleCategories) { category in
                    Button {
                        if let kind = category.setupKind, !category.comingSoon {
                            onSelectSetup(kind)
                        }
                    } label: {
                        categoryCard(category)
                    }
                    .buttonStyle(.plain)
                    .disabled(category.comingSoon || category.setupKind == nil)
                }
            }
            .padding(.horizontal, 20)

            if !showRetailTagline {
                HStack(spacing: 6) {
                    Text("Not sure where to start?")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(Color(hex: "#94A3B8"))
                    Text("Let AI suggest →")
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(BusinessEmptyTokens.accent)
                        .underline()
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
        }
    }

    private var memoryContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 10) {
                Image("biz_memory_search")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                TextField("", text: $search, prompt: Text("Search memories...").foregroundStyle(Color(hex: "#64748B")))
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(.white)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(BusinessSheetTheme.card, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(BusinessSheetTheme.border, lineWidth: 1))

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(filters, id: \.self) { chip in
                        Button {
                            filter = chip
                        } label: {
                            Text(chip)
                                .font(.plusJakarta(size: 13))
                                .foregroundStyle(filter == chip ? BusinessEmptyTokens.accent : Color(hex: "#94A3B8"))
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(filter == chip ? Color.clear : BusinessSheetTheme.card, in: Capsule())
                                .overlay(Capsule().stroke(filter == chip ? BusinessEmptyTokens.accent : BusinessSheetTheme.border, lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Text("RECENT MEMORIES")
                .font(.plusJakarta(size: 13, weight: .semibold))
                .tracking(0.8)
                .foregroundStyle(Color(hex: "#64748B"))

            if filteredMemories.isEmpty {
                Text("No memories yet")
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(Color(hex: "#94A3B8"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(BusinessSheetTheme.card, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(BusinessSheetTheme.border, lineWidth: 1))
            } else {
                ForEach(filteredMemories) { memoryCard($0) }
            }
        }
        .padding(.horizontal, 20)
    }

    private func categoryCard(_ item: Category) -> some View {
        ZStack(alignment: .topTrailing) {
            HStack(spacing: 0) {
                HStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10)
                            .fill(BusinessEmptyTokens.accent.opacity(0.13))
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(BusinessEmptyTokens.accent, lineWidth: 1))
                            .frame(width: 40, height: 40)
                        Image(item.icon)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 20, height: 20)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.title)
                            .font(.plusJakarta(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(item.body)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(Color(hex: "#94A3B8"))
                            .lineLimit(2)
                    }
                    Spacer(minLength: 0)
                }
                .padding(16)
                Image("biz_create_chevron")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
                    .frame(width: 44)
            }
            .frame(height: 110)
            .background(
                Image(item.card)
                    .resizable()
                    .scaledToFill()
                    .overlay(
                        LinearGradient(
                            colors: [Color(hex: "#161B26"), Color(hex: "#161B26").opacity(0.93), Color(hex: "#161B26").opacity(0.4)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
            )
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(BusinessSheetTheme.border, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .opacity(item.comingSoon ? 0.6 : 1)
            .accessibilityIdentifier(item.testTag ?? "")

            if item.comingSoon {
                Text("Coming Soon")
                    .font(.plusJakarta(size: 9, weight: .semibold))
                    .tracking(0.5)
                    .foregroundStyle(Color(hex: "#98A3B8"))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(hex: "#4D4D59").opacity(0.6))
                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.white.opacity(0.1), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                    .padding(.top, 9)
                    .padding(.trailing, 12)
            }
        }
    }

    private func memoryCard(_ item: MemoryItem) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(item.dotAsset)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 8, height: 8)
                Text(item.title)
                    .font(.plusJakarta(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
            }
            Text(item.body)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(Color(hex: "#94A3B8"))
                .lineLimit(2)
            Image("biz_memory_line")
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
            HStack {
                HStack(spacing: 8) {
                    Text(item.tag.uppercased())
                        .font(.plusJakarta(size: 11, weight: .semibold))
                        .foregroundStyle(Color(hex: "#94A3B8"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(BusinessSheetTheme.border, in: RoundedRectangle(cornerRadius: 6))
                    Text(item.date)
                        .font(.plusJakarta(size: 12))
                        .foregroundStyle(Color(hex: "#64748B"))
                }
                Spacer()
                Image("biz_memory_bookmark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
            }
        }
        .padding(16)
        .background(BusinessSheetTheme.card, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(BusinessSheetTheme.border, lineWidth: 1))
    }
}
