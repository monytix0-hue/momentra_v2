import SwiftUI

struct GroupSetupStep: Identifiable {
    let id: String
    let iconName: String
    let label: String
}

struct GroupSetupStepper: View {
    let activeStep: Int
    var palette: GroupTypePalette = GroupSetupTheme.tripPalette

    var body: some View {
        HStack(spacing: 12) {
            ForEach(Array(Self.experienceSteps.enumerated()), id: \.offset) { index, step in
                stepItem(step, index: index)
            }
        }
    }

    private func stepItem(_ step: GroupSetupStep, index: Int) -> some View {
        let state: StepVisual = {
            if index < activeStep { return .completed }
            if index == activeStep { return .current }
            return .upcoming
        }()
        return VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(state.fill(palette: palette))
                    .frame(width: 44, height: 44)
                    .overlay(Circle().stroke(state.border(palette: palette), lineWidth: 2))
                    .shadow(color: state == .current ? palette.stepGlow : .clear, radius: 7)
                Image(step.iconName)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(state.icon(palette: palette, upcoming: upcomingIcon(for: index)))
            }
            Text(step.label)
                .font(.plusJakarta(size: 12, weight: .semibold))
                .foregroundStyle(state.label(palette: palette))
        }
        .frame(maxWidth: .infinity)
    }

    private func upcomingIcon(for index: Int) -> Color {
        index == 3 ? GroupSetupTheme.textSecondary : palette.accent
    }

    private enum StepVisual {
        case current, completed, upcoming

        func fill(palette: GroupTypePalette) -> Color {
            switch self {
            case .current: return palette.accent
            case .completed: return palette.accent.opacity(0.2)
            case .upcoming: return GroupSetupTheme.stepInactiveBg
            }
        }

        func border(palette: GroupTypePalette) -> Color {
            switch self {
            case .current, .completed: return palette.accent
            case .upcoming: return GroupSetupTheme.border
            }
        }

        func icon(palette: GroupTypePalette, upcoming: Color) -> Color {
            switch self {
            case .current: return GroupSetupTheme.ctaText
            case .completed: return palette.accent
            case .upcoming: return upcoming
            }
        }

        func label(palette: GroupTypePalette) -> Color {
            switch self {
            case .current, .completed: return palette.accent
            case .upcoming: return GroupSetupTheme.textSecondary
            }
        }
    }

    static let experienceSteps: [GroupSetupStep] = [
        .init(id: "type", iconName: "ges_step_compass", label: "Type"),
        .init(id: "details", iconName: "ges_step_check", label: "Details"),
        .init(id: "people", iconName: "ges_step_check", label: "People"),
        .init(id: "activate", iconName: "ges_step_rocket", label: "Activate"),
    ]
}

struct GroupSetupSectionHeader: View {
    let step: String
    let title: String
    var bodyText: String? = nil
    var palette: GroupTypePalette = GroupSetupTheme.tripPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .center, spacing: 12) {
                Text(step)
                    .font(.plusJakarta(size: step.count <= 2 ? 40 : 11, weight: .heavy))
                    .foregroundStyle(palette.accent.opacity(0.12))
                Text(title.uppercased())
                    .font(.plusJakarta(size: 11, weight: .semibold))
                    .foregroundStyle(palette.accent)
            }
            if let bodyText {
                Text(bodyText)
                    .font(.plusJakarta(size: 13))
                    .foregroundStyle(GroupSetupTheme.textSecondary)
            }
        }
    }
}

struct GroupSetupHero: View {
    let title: String
    let subtitle: String
    let accent: Color
    let iconName: String

    var body: some View {
        VStack(spacing: 28) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [accent.opacity(0.22), .clear],
                            center: .center,
                            startRadius: 10,
                            endRadius: 140
                        )
                    )
                    .frame(width: 280, height: 280)
                RoundedRectangle(cornerRadius: 56)
                    .fill(Color(hex: "#161B26"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 56)
                            .stroke(Color(hex: "#FFE1BF").opacity(0.2), lineWidth: 1)
                    )
                    .frame(width: 112, height: 112)
                    .overlay {
                        Image(iconName)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 44, height: 44)
                    }
            }
            VStack(spacing: 12) {
                Text(title)
                    .font(.plusJakarta(size: 28, weight: .bold))
                    .foregroundStyle(accent)
                    .multilineTextAlignment(.center)
                Text(subtitle)
                    .font(.plusJakarta(size: 14))
                    .foregroundStyle(accent.opacity(0.42))
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }
}
