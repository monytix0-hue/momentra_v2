import SwiftUI

/// Figma 658:9451 (Choose a Moment) + 658:9573 (Choose a Memory).
struct BusinessCreateMomentView: View {
    var onBack: () -> Void
    var onSelectSetup: (BusinessSetupKind) -> Void

    enum Tab: String, CaseIterable {
        case createMoment = "Create a Moment"
        case chooseMemory = "Choose a Memory"
    }

    @State private var tab: Tab = .createMoment
    @State private var search = ""
    @State private var filter = "All"

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

    private let categories: [Category] = [
        Category(title: "Team Operations", body: "Manage hiring, attendance, performance & team growth", icon: "biz_create_users", card: "biz_create_card_team", comingSoon: false, setupKind: .teamOperations, testTag: "business.setup.team_operations"),
        Category(title: "Business Runway", body: "Monitor cash flow, spending and runway health.", icon: "biz_create_trending", card: "biz_create_card_runway", comingSoon: false, setupKind: .businessRunway, testTag: "business.setup.business_runway"),
        Category(title: "Business Operations", body: "Organize departments, processes and workflows.", icon: "biz_create_credit_card", card: "biz_create_card_ops", comingSoon: false, setupKind: .businessOperations, testTag: "business.setup.business_operations"),
        Category(title: "Project Operations", body: "Organize tasks, milestones, sprints & deliverables", icon: "biz_create_layers", card: "biz_create_card_project", comingSoon: true, setupKind: nil, testTag: nil),
        Category(title: "Event Operations", body: "Organize events from planning through execution.", icon: "biz_create_wallet", card: "biz_create_card_event", comingSoon: true, setupKind: nil, testTag: nil),
        Category(title: "Vendor Operations", body: "Manage suppliers, contracts, procurement & partnerships", icon: "biz_create_briefcase", card: "biz_create_card_vendor", comingSoon: true, setupKind: nil, testTag: nil),
    ]

    private let memories: [MemoryItem] = [
        MemoryItem(title: "Q2 Revenue Milestone", body: "Crossed ₹50L monthly recurring revenue for the first time", tag: "Revenue", date: "Jul 14, 2026", dotAsset: "biz_memory_dot_green"),
        MemoryItem(title: "New CTO Onboarded", body: "Ravi Mehta joined as CTO, bringing 12 years enterprise experience", tag: "Team", date: "Jun 28, 2026", dotAsset: "biz_memory_dot_blue"),
        MemoryItem(title: "Series A Strategy Pivot", body: "Shifted focus from B2C to B2B SaaS after market analysis", tag: "Strategy", date: "Jun 15, 2026", dotAsset: "biz_memory_dot_purple"),
        MemoryItem(title: "Product V2 Launch", body: "Released redesigned dashboard with AI-powered insights", tag: "Projects", date: "May 30, 2026", dotAsset: "biz_memory_dot_orange"),
        MemoryItem(title: "Cost Optimization Win", body: "Reduced cloud infrastructure costs by 34% through migration", tag: "Expenses", date: "May 18, 2026", dotAsset: "biz_memory_dot_green"),
        MemoryItem(title: "First Enterprise Client", body: "Signed 3-year contract with TechCorp India worth ₹2.4Cr", tag: "Revenue", date: "May 2, 2026", dotAsset: "biz_memory_dot_blue"),
    ]

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
                tabSwitcher
                if tab == .createMoment {
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
                    .font(.system(size: 20, weight: .bold))
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
                        .font(.system(size: 14, weight: .semibold))
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
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(.white)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("Create moments to track operations, revenue, strategy, and growth — all in one place.")
                        .font(.system(size: 14))
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
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color(hex: "#94A3B8"))
                ForEach(categories) { category in
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

            HStack(spacing: 6) {
                Text("Not sure where to start?")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: "#94A3B8"))
                Text("Let AI suggest →")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(BusinessEmptyTokens.accent)
                    .underline()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
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
                    .font(.system(size: 14))
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
                                .font(.system(size: 13))
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
                .font(.system(size: 13, weight: .semibold))
                .tracking(0.8)
                .foregroundStyle(Color(hex: "#64748B"))

            ForEach(filteredMemories) { memoryCard($0) }

            VStack(spacing: 8) {
                Image("biz_memory_line_footer")
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
                Text("Showing \(filteredMemories.count) of 24 memories")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#64748B"))
                Text("View All Memories →")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(BusinessEmptyTokens.accent)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 4)
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
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(item.body)
                            .font(.system(size: 13))
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
                    .font(.system(size: 9, weight: .semibold))
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
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
            }
            Text(item.body)
                .font(.system(size: 13))
                .foregroundStyle(Color(hex: "#94A3B8"))
                .lineLimit(2)
            Image("biz_memory_line")
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
            HStack {
                HStack(spacing: 8) {
                    Text(item.tag.uppercased())
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color(hex: "#94A3B8"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(BusinessSheetTheme.border, in: RoundedRectangle(cornerRadius: 6))
                    Text(item.date)
                        .font(.system(size: 12))
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
