import SwiftUI

/// Lifestyle Moments populated body — teal family (`#0EA5A4`), adapted from Future Moments.
struct PersonalLifestyleMomentsActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    let momentTitle: String?
    var onOpenQuickAdd: () -> Void
    var onAddExpense: () -> Void

    @State private var pulse: APIClient.PersonalPulsePayload?
    @State private var activities: [APIClient.ActivityItemPayload] = []
    @State private var loading = true
    @State private var error: String?

    private let accent = Color(hex: "#0EA5A4")
    private let accentSoft = Color(hex: "#2DD4BF")

    var body: some View {
        Group {
            if loading && pulse == nil {
                ProgressView().tint(accent)
            } else {
                ZStack(alignment: .bottomTrailing) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            if let error {
                                Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                            }
                            if let momentTitle, !momentTitle.isEmpty {
                                Text(momentTitle)
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundStyle(Color(hex: "#C9C4D8"))
                            }
                            heroCard
                            journeyTimeline
                            spendJourney
                            bestAndTurning
                            insightsCard
                            captureCta
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .padding(.bottom, 56)
                    }
                    Button(action: onAddExpense) {
                        Text("₹+")
                            .font(.system(size: 16, weight: .heavy))
                            .foregroundStyle(.white)
                            .frame(width: 52, height: 52)
                            .background(accent)
                            .clipShape(Circle())
                            .shadow(color: accent.opacity(0.4), radius: 10, y: 4)
                    }
                    .disabled(momentId == nil)
                    .opacity(momentId == nil ? 0.45 : 1)
                    .padding(.trailing, 20)
                    .padding(.bottom, 16)
                }
            }
        }
        .background(Color(hex: "#14121B"))
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var vitality: String { PersonalLifestyleDerived.vitalityIndexDisplay(pulse: pulse) }
    private var axes: PersonalLifestyleDerived.AxisScores { PersonalLifestyleDerived.axisScores(pulse: pulse) }
    private var stage: String { PersonalLifestyleDerived.networkStability(pulse: pulse) }

    private var spendPairs: [(String, String)] { PersonalLifestyleDerived.spendPairs(pulse: pulse) }

    private var experienceCount: Int { PersonalLifestyleDerived.experienceCount(pulse: pulse) }

    private var insightLine: String {
        vitality == "—"
            ? "Log experiences and wellbeing to reveal your Vitality Index."
            : "Your lifestyle rhythm is forming through lived moments."
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Lifestyle Journey")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Text(insightLine)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }
                Spacer(minLength: 8)
                VStack(spacing: 2) {
                    Text(vitality)
                        .font(.system(size: 28, weight: .heavy))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                    Text("VITALITY")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(Color(hex: "#C9C4D8"))
                }
                .frame(width: 88, height: 88)
                .background(
                    Circle().stroke(
                        vitality == "—" ? Color.white.opacity(0.15) : accent,
                        lineWidth: 6
                    )
                )
            }
            Text(stage == "Thriving" ? "Flourishing" : (vitality == "—" ? "Building" : "Stabilizing"))
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white)
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(LinearGradient(colors: [accent, accentSoft], startPoint: .leading, endPoint: .trailing))
                .clipShape(Capsule())
            HStack(spacing: 8) {
                miniStat("✨", experienceCount > 0 ? "\(experienceCount)" : "—", "Experiences")
                miniStat("📝", activities.isEmpty ? "—" : "\(activities.count)", "Logs")
                miniStat("💰", spendPairs.isEmpty ? "—" : "Logged", "Spend")
                miniStat("🌿", PersonalLifeOpsDerived.displayScore(pulse?.recoveryScore), "Recovery")
            }
        }
        .padding(24)
        .background(Color(hex: "#121A1C"))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func miniStat(_ emoji: String, _ value: String, _ label: String) -> some View {
        VStack(spacing: 8) {
            Text(emoji).font(.system(size: 16))
            Text(value).font(.system(size: 16, weight: .heavy)).foregroundStyle(.white)
                .lineLimit(1).minimumScaleFactor(0.7)
            Text(label).font(.system(size: 9, weight: .bold)).foregroundStyle(.white)
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(accent.opacity(0.85))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var journeyTimeline: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Journey Timeline")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            if activities.isEmpty {
                Text("No journey entries yet. Capture an experience or wellbeing log to start.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white.opacity(0.05))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
            } else {
                ForEach(activities.prefix(8)) { item in
                    HStack(alignment: .top, spacing: 12) {
                        Text("◇")
                            .font(.system(size: 18))
                            .frame(width: 40, height: 40)
                            .background(accent)
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.title)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Color(hex: "#E5E0EE"))
                            Text(PersonalLifeOpsDerived.relativeTime(item.occurredAt))
                                .font(.system(size: 11))
                                .foregroundStyle(Color(hex: "#C9C4D8"))
                        }
                    }
                    .padding(16)
                    .background(accent.opacity(0.1))
                    .overlay(RoundedRectangle(cornerRadius: 20).stroke(accent.opacity(0.4), lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                }
            }
        }
    }

    private var spendJourney: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Lifestyle Spend")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            if spendPairs.isEmpty {
                Text("No lifestyle spend recorded for this moment yet.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white.opacity(0.05))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(spendPairs, id: \.0) { currency, amount in
                    HStack {
                        Text(currency).foregroundStyle(Color(hex: "#E5E0EE"))
                        Spacer()
                        Text(amount).fontWeight(.semibold).foregroundStyle(Color(hex: "#E5E0EE"))
                    }
                    .font(.system(size: 13))
                }
                .padding(16)
                .background(Color.white.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
    }

    private var bestAndTurning: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Best Moments")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            HStack(spacing: 10) {
                bestCard(
                    title: experienceCount > 0 ? "Experiences logged" : "Experiences",
                    detail: experienceCount > 0 ? "\(experienceCount) on this moment" : "Building…",
                    emoji: "🏆"
                )
                bestCard(
                    title: spendPairs.isEmpty ? "Spend" : "Spend logged",
                    detail: spendPairs.isEmpty ? "Building…" : "Lifestyle spend present",
                    emoji: "💰"
                )
            }
        }
    }

    private func bestCard(title: String, detail: String, emoji: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(emoji).font(.system(size: 18))
            Text(title).font(.system(size: 13, weight: .bold)).foregroundStyle(Color(hex: "#E5E0EE"))
            Text(detail).font(.system(size: 12, weight: .semibold)).foregroundStyle(accent)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(accent.opacity(0.1))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(accent.opacity(0.45), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var insightsCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text("AI Insights").font(.system(size: 14, weight: .bold)).foregroundStyle(Color(hex: "#E5E0EE"))
                Text("Coming Soon")
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundStyle(accentSoft)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(accent.opacity(0.2))
                    .clipShape(Capsule())
            }
            Text("Patterns across experiences, wellbeing, and discovery will surface here.")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var captureCta: some View {
        VStack(spacing: 10) {
            Text("Capture a new moment")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
            Text("Add an experience, wellbeing check, or lifestyle log")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.85))
            Button(action: onOpenQuickAdd) {
                Text("+ Open Quick Add")
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.white.opacity(0.55), lineWidth: 1.5))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(LinearGradient(colors: [accent, Color(hex: "#7C5CFC")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func load() async {
        error = nil
        if let cached = PersonalTabDataCache.peekPulse(momentId: momentId) {
            pulse = cached.pulse
            activities = cached.activities
            loading = false
        } else {
            loading = true
        }
        do {
            let data = try await PersonalTabLoad.loadPulseTab(momentId: momentId)
            pulse = data.pulse
            activities = data.activities
        } catch {
            self.error = error.localizedDescription
        }
        loading = false
    }
}
