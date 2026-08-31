import SwiftUI

struct PersonalCreateEmptyView: View {
    var history: [MomentSummary] = []
    var onMomentCreated: (String, String, String?) -> Void = { _, _, _ in }

    @State private var wizard: PersonalSetupSystem?

    var body: some View {
        chooser
            .sheet(item: $wizard) { system in
                PersonalSetupWizardView(
                    system: system,
                    onBack: { wizard = nil },
                    onCreated: { id, title, typeCode in
                        wizard = nil
                        onMomentCreated(id, title, typeCode)
                    }
                )
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(24)
                .preferredColorScheme(.dark)
            }
    }

    private var chooser: some View {
        ScrollView {
            VStack(spacing: 32) {
                VStack(spacing: 8) {
                    Text("Create a Moment")
                        .font(.system(size: 22, weight: .heavy))
                        .foregroundStyle(PersonalEmptyTokens.text)
                        .frame(maxWidth: .infinity)
                    Text("Choose a life system to begin")
                        .font(.system(size: 13))
                        .foregroundStyle(PersonalEmptyTokens.subtle)
                        .frame(maxWidth: .infinity)
                }

                VStack(spacing: 12) {
                    HStack(spacing: 12) {
                        lifeCard(
                            .lifeOperations,
                            subtitle: "Daily commitments & money",
                            glyph: "▣",
                            accent: PersonalEmptyTokens.purple,
                            deep: Color(hex: "#4F46E5"),
                            thumb: "personal_create_thumb_life_ops"
                        )
                        lifeCard(
                            .futureBuilding,
                            subtitle: "Goals, growth & progress",
                            glyph: "↗",
                            accent: PersonalEmptyTokens.green,
                            deep: Color(hex: "#0F766E"),
                            thumb: "personal_create_thumb_future"
                        )
                    }
                    HStack(spacing: 12) {
                        lifeCard(
                            .lifestyle,
                            subtitle: "Experiences & wellbeing",
                            glyph: "◈",
                            accent: PersonalEmptyTokens.amber,
                            deep: Color(hex: "#EA580C"),
                            thumb: "personal_create_thumb_lifestyle"
                        )
                        lifeCard(
                            .relationships,
                            subtitle: "Care & shared moments",
                            glyph: "♡",
                            accent: Color(hex: "#E91E63"),
                            deep: Color(hex: "#BE185D"),
                            thumb: "personal_create_thumb_relationships"
                        )
                    }
                }

                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 8) {
                        Text("⚡").foregroundStyle(PersonalEmptyTokens.purple)
                        Text("Quick Start")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(PersonalEmptyTokens.text)
                    }
                    quickRow("☀️", "Morning check-in", "How are you feeling today?", "Log", PersonalEmptyTokens.purple, .lifeOperations)
                    Divider().overlay(Color.white.opacity(0.04))
                    quickRow("💰", "Track expense", "Record a transaction", "Add", PersonalEmptyTokens.green, .lifeOperations)
                    Divider().overlay(Color.white.opacity(0.04))
                    quickRow("🤝", "Log connection", "Capture a shared moment", "Start", PersonalEmptyTokens.amber, .relationships)
                }

                HStack(alignment: .top, spacing: 8) {
                    Text("✦")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 24, height: 24)
                        .background(
                            RadialGradient(
                                colors: [PersonalEmptyTokens.purple, Color(hex: "#4F46E5")],
                                center: .center,
                                startRadius: 0,
                                endRadius: 16
                            ),
                            in: Circle()
                        )
                    Text("Tip: Start with what's on your mind right now.\nMomentra adapts to you.")
                        .font(.system(size: 15).italic())
                        .foregroundStyle(PersonalEmptyTokens.secondary)
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.white.opacity(0.04))
                .overlay(RoundedRectangle(cornerRadius: 20).stroke(PersonalEmptyTokens.purple, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 20))

                if !history.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Past moments")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(PersonalEmptyTokens.text)
                        ForEach(history) { moment in
                            Text(moment.title)
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(PersonalEmptyTokens.text)
                                .padding(14)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.white.opacity(0.06))
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 34)
        }
        .background(PersonalEmptyTokens.bg.ignoresSafeArea())
        .personalAppear()
        .trackScreen(AnalyticsScreens.personalCreate)
    }

    private func lifeCard(
        _ system: PersonalSetupSystem,
        subtitle: String,
        glyph: String,
        accent: Color,
        deep: Color,
        thumb: String
    ) -> some View {
        Button { wizard = system } label: {
            VStack(spacing: 0) {
                Image(thumb)
                    .resizable()
                    .scaledToFill()
                    .frame(height: 80)
                    .frame(maxWidth: .infinity)
                    .clipped()
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 8) {
                        Text(glyph)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(width: 32, height: 32)
                            .background(
                                RadialGradient(colors: [accent, deep], center: .center, startRadius: 0, endRadius: 20),
                                in: Circle()
                            )
                        Text(system.label)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(PersonalEmptyTokens.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.85)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    Text(subtitle)
                        .font(.system(size: 11))
                        .foregroundStyle(PersonalEmptyTokens.subtle)
                        .lineLimit(1)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 12)
                .padding(.top, 10)
                .padding(.bottom, 8)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                accent.frame(height: 3)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 160)
            .background(PersonalEmptyTokens.card)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.06), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private func quickRow(
        _ emoji: String,
        _ title: String,
        _ subtitle: String,
        _ cta: String,
        _ color: Color,
        _ system: PersonalSetupSystem
    ) -> some View {
        Button { wizard = system } label: {
            HStack {
                HStack(spacing: 12) {
                    Text(emoji).font(.system(size: 18))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(PersonalEmptyTokens.text)
                        Text(subtitle)
                            .font(.system(size: 11))
                            .foregroundStyle(PersonalEmptyTokens.subtle)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                Spacer()
                Text(cta)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(color, in: Capsule())
            }
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
    }
}
