import SwiftUI

/// Lifestyle Memory populated body — teal family; Memory GET honest empty (S2 G4).
struct PersonalLifestyleMemoryActiveView: View {
    let refreshToken: UInt64
    let momentId: String?
    var onProtectRitual: () -> Void

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
                NativeDashboardScaffold(background: Color(hex: "#14121B")) {

                    NativeListSection {

                    VStack(alignment: .leading, spacing: 16) {
                        if let error {
                            Text(error).font(.caption).foregroundStyle(Color(hex: "#F87171"))
                        }
                        identitySnapshot
                        memoryProjection
                        corePattern
                        activitySignals
                        growthEdge
                        insightsCard
                    }
                

                    }

                }
            }
        }
        .background(Color(hex: "#14121B"))
        .task(id: "\(refreshToken)-\(momentId ?? "")") { await load() }
    }

    private var thinData: Bool {
        activities.count < 2 && PersonalLifestyleDerived.vitalityIndex(pulse: pulse) == nil
    }

    private var axes: PersonalLifestyleDerived.AxisScores { PersonalLifestyleDerived.axisScores(pulse: pulse) }

    private var identitySnapshot: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("1 IDENTITY SNAPSHOT")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            Text(thinData ? "Explorer in Motion" : "Vitality Builder")
                .font(.system(size: 28, weight: .heavy))
                .foregroundStyle(Color(hex: "#E5E0EE"))
            Text(
                thinData
                    ? "Log experiences and wellbeing to reveal your lifestyle identity."
                    : "You thrive when experiences and recovery stay in rhythm."
            )
            .font(.system(size: 14))
            .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#121A1C"))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var memoryProjection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("2 AXIS SNAPSHOT")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            axisRow("Joy", axes.joy)
            axisRow("Fulfillment", axes.fulfillment)
            axisRow("Vitality", axes.vitality)
            axisRow("Exploration", axes.exploration)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.08), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func axisRow(_ label: String, _ score: String) -> some View {
        HStack {
            Text(label).foregroundStyle(Color(hex: "#E5E0EE"))
            Spacer()
            Text(score).fontWeight(.semibold).foregroundStyle(accentSoft)
        }
        .font(.system(size: 13))
    }

    private var corePattern: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("3 CORE PATTERN")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            if thinData {
                Text("Building… Need a few more logs to show Experience → Recovery → Vitality.")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                Text("Experience → Recovery → Vitality")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: "#E5E0EE"))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "#121A1C"))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.1), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var activitySignals: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("4 ACTIVITY SIGNALS")
                .font(.system(size: 12, weight: .bold))
                .tracking(1.2)
                .foregroundStyle(Color(hex: "#C9C4D8"))
            if activities.isEmpty {
                Text("Building…")
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: "#C9C4D8"))
            } else {
                ForEach(activities.prefix(4)) { item in
                    Text(item.title)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color(hex: "#E5E0EE"))
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var growthEdge: some View {
        VStack(spacing: 10) {
            Text("Protect one weekly lifestyle ritual")
                .font(.system(size: 16, weight: .heavy))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
            Text("Schedule it before the week fills up.")
                .font(.system(size: 13))
                .foregroundStyle(.white.opacity(0.85))
                .multilineTextAlignment(.center)
            Button(action: onProtectRitual) {
                Text("Log Experience Now")
                    .font(.system(size: 14, weight: .heavy))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.plain)
            .disabled(momentId == nil)
            .opacity(momentId == nil ? 0.5 : 1)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(LinearGradient(colors: [accent, Color(hex: "#7C5CFC")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private var insightsCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("AI Insights").font(.system(size: 14, weight: .bold)).foregroundStyle(Color(hex: "#E5E0EE"))
                Spacer()
                Text("Coming Soon")
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundStyle(accentSoft)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(accent.opacity(0.2))
                    .clipShape(Capsule())
            }
            Text("Recurring themes across your Lifestyle journey will appear here.")
                .font(.system(size: 12))
                .foregroundStyle(Color(hex: "#C9C4D8"))
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
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
