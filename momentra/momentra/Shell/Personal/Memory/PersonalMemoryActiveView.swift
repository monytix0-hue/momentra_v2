import SwiftUI

/// M4 Memory — reflection over time (five honest blocks via PersonalMemorySummaryModel).
struct PersonalMemoryActiveView: View {
    let refreshToken: UInt64

    @State private var payload: APIClient.PersonalMemoryPayload?
    @State private var loading = true
    @State private var error: String?
    @State private var showPatternWhy = false
    @State private var showEvolutionDetail = false
    @State private var showRelive = false

    private let bg = Color(red: 0.078, green: 0.071, blue: 0.106)
    private let card = Color(red: 0.110, green: 0.106, blue: 0.180)
    private let cardAlt = Color(red: 0.086, green: 0.106, blue: 0.149)
    private let text = Color(red: 0.898, green: 0.878, blue: 0.933)
    private let muted = Color(red: 0.788, green: 0.769, blue: 0.847)
    private let dim = Color(red: 0.549, green: 0.549, blue: 0.620)
    private let purple = Color(red: 0.486, green: 0.361, blue: 0.988)
    private let green = Color(red: 0.063, green: 0.725, blue: 0.506)
    private let red = Color(red: 0.937, green: 0.267, blue: 0.267)
    private let amber = Color(red: 0.961, green: 0.620, blue: 0.043)

    private var summary: PersonalMemorySummaryModel? {
        guard let payload else { return nil }
        return PersonalMemorySummaryModel.from(payload)
    }

    var body: some View {
        Group {
            if loading && payload == nil {
                ProgressView().tint(purple).frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NativeDashboardScaffold(background: bg) {
                    NativeListSection {
                        if let error {
                            Text(error).font(.plusJakarta(size: 12)).foregroundStyle(red)
                        }
                        if let summary {
                            heroBlock(summary.hero)
                            if !summary.reliveMedia.isEmpty {
                                reliveBlock(summary.reliveMedia)
                            }
                            if !summary.highlights.items.isEmpty {
                                highlightsBlock(summary.highlights)
                            }
                            if let pattern = summary.pattern {
                                patternBlock(pattern, hasWhy: summary.patternWhy != nil)
                            }
                            if !summary.returnBehaviours.isEmpty {
                                returnBlock(summary.returnBehaviours)
                            }
                            if let evolution = summary.evolution {
                                evolutionBlock(evolution, hasDetail: summary.evolutionDetail != nil)
                            }
                            if summary.highlights.items.isEmpty
                                && summary.pattern == nil
                                && summary.returnBehaviours.isEmpty
                                && summary.evolution == nil
                                && summary.hero.memoryCount == 0
                                && summary.hero.activityCount == 0 {
                                emptyHint
                            }
                        }
                    }
                }
            }
        }
        .task(id: refreshToken) { await load() }
        .sheet(isPresented: $showPatternWhy) {
            if let why = summary?.patternWhy {
                disclosureSheet(title: "See why", items: why.map(\.label))
            }
        }
        .sheet(isPresented: $showEvolutionDetail) {
            if let detail = summary?.evolutionDetail {
                evolutionDisclosureSheet(detail)
            }
        }
        .sheet(isPresented: $showRelive) {
            if let media = summary?.reliveMedia, !media.isEmpty {
                reliveSheet(media)
            }
        }
    }

    private func load() async {
        error = nil
        if payload != nil {
            loading = false
        } else {
            loading = true
        }
        do {
            payload = try await APIClient.shared.getPersonalMemory()
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }

    // MARK: - Blocks

    private func heroBlock(_ hero: PersonalMemorySummaryModel.Hero) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(hero.periodLabel.uppercased())
                .font(.plusJakarta(size: 11, weight: .bold))
                .foregroundStyle(purple)
            if let sentence = hero.sentence {
                Text(sentence)
                    .font(.plusJakarta(size: 16, weight: .semibold))
                    .foregroundStyle(text)
            }
            HStack(spacing: 14) {
                countChip("\(hero.memoryCount)", "memories")
                countChip("\(hero.activityCount)", "activities")
                if hero.highlightCount > 0 {
                    countChip("\(hero.highlightCount)", "highlights")
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func countChip(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.plusJakarta(size: 15, weight: .bold))
                .foregroundStyle(text)
            Text(label)
                .font(.plusJakarta(size: 10))
                .foregroundStyle(dim)
        }
    }

    private func reliveBlock(_ media: [PersonalMemorySummaryModel.ReliveMediaItem]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Relive")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(media.prefix(6)) { item in
                        AsyncImage(url: URL(string: item.downloadUrl)) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            default:
                                RoundedRectangle(cornerRadius: 10).fill(cardAlt)
                            }
                        }
                        .frame(width: 72, height: 72)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }
            Button { showRelive = true } label: {
                Text("Open Relive")
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(purple)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func highlightsBlock(_ highlights: PersonalMemorySummaryModel.Highlights) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(highlights.heading)
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            ForEach(highlights.items) { item in
                Text(item.title)
                    .font(.plusJakarta(size: 14, weight: .medium))
                    .foregroundStyle(text)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(cardAlt)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func patternBlock(_ pattern: PersonalMemorySummaryModel.Pattern, hasWhy: Bool) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Patterns worth knowing")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            Text(pattern.title)
                .font(.plusJakarta(size: 16, weight: .bold))
                .foregroundStyle(text)
            Text(pattern.body)
                .font(.plusJakarta(size: 13))
                .foregroundStyle(muted)
            if hasWhy {
                Button { showPatternWhy = true } label: {
                    Text("See why")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(purple)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func returnBlock(_ items: [PersonalMemorySummaryModel.ReturnBehaviour]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Worth doing again")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            ForEach(items) { item in
                HStack {
                    Text(item.label)
                        .font(.plusJakarta(size: 14, weight: .medium))
                        .foregroundStyle(text)
                    Spacer()
                    if let strength = item.strengthLabel, !strength.isEmpty {
                        Text(strength)
                            .font(.plusJakarta(size: 11, weight: .semibold))
                            .foregroundStyle(green)
                    }
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 12)
                .background(cardAlt)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func evolutionBlock(_ evolution: PersonalMemorySummaryModel.Evolution, hasDetail: Bool) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Then → Now")
                .font(.plusJakarta(size: 12, weight: .bold))
                .foregroundStyle(purple)
            HStack {
                Text(evolution.thenLabel)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(dim)
                Image(systemName: "arrow.right")
                    .font(.system(size: 10))
                    .foregroundStyle(dim)
                Text(evolution.nowLabel)
                    .font(.plusJakarta(size: 12, weight: .semibold))
                    .foregroundStyle(text)
            }
            Text(evolution.summary)
                .font(.plusJakarta(size: 14, weight: .medium))
                .foregroundStyle(muted)
            if hasDetail {
                Button { showEvolutionDetail = true } label: {
                    Text("See your evolution")
                        .font(.plusJakarta(size: 13, weight: .semibold))
                        .foregroundStyle(purple)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(card)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var emptyHint: some View {
        Text("As you log life and save memories, this space will help you revisit what mattered.")
            .font(.plusJakarta(size: 13))
            .foregroundStyle(muted)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08)))
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func disclosureSheet(title: String, items: [String]) -> some View {
        NavigationStack {
            List {
                ForEach(Array(items.enumerated()), id: \.offset) { _, label in
                    Text(label)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(text)
                }
            }
            .scrollContentBackground(.hidden)
            .background(bg)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { showPatternWhy = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func evolutionDisclosureSheet(_ detail: PersonalMemorySummaryModel.EvolutionDetail) -> some View {
        NavigationStack {
            List {
                Section("Then") {
                    Text(detail.thenSummary).font(.plusJakarta(size: 14)).foregroundStyle(text)
                }
                Section("Now") {
                    Text(detail.nowSummary).font(.plusJakarta(size: 14)).foregroundStyle(text)
                }
                if !detail.notes.isEmpty {
                    Section("Notes") {
                        ForEach(detail.notes, id: \.self) { note in
                            Text(note).font(.plusJakarta(size: 14)).foregroundStyle(muted)
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(bg)
            .navigationTitle("Your evolution")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { showEvolutionDetail = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func reliveSheet(_ media: [PersonalMemorySummaryModel.ReliveMediaItem]) -> some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(media) { item in
                        VStack(alignment: .leading, spacing: 6) {
                            AsyncImage(url: URL(string: item.downloadUrl)) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    RoundedRectangle(cornerRadius: 12).fill(cardAlt)
                                }
                            }
                            .frame(height: 140)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            if let title = item.title, !title.isEmpty {
                                Text(title)
                                    .font(.plusJakarta(size: 12))
                                    .foregroundStyle(muted)
                                    .lineLimit(2)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(bg)
            .navigationTitle("Relive")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { showRelive = false }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
