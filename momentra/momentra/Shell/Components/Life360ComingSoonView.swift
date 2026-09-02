import SwiftUI

/// Figma `1075:7637` Life 360 Coming Soon — static local UI only.
/// Does not call GET /life360 or read projection.life360.
struct Life360ComingSoonView: View {
    let onClose: () -> Void
    @State private var notifyAck = false

    private var theme: GlobalSurfaceTheme.Life360 { GlobalSurfaceTheme.life360 }

    var body: some View {
        NativeSheetScaffold(
            title: "Life 360",
            onClose: onClose,
            background: theme.comingSoonBackground
        ) {
            ScrollView {
                VStack(alignment: .center, spacing: 24) {
                    VStack(spacing: 12) {
                        Text("COMING SOON")
                            .font(.system(size: 12, weight: .heavy))
                            .tracking(1.5)
                            .foregroundStyle(theme.comingSoonBackground)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(
                                LinearGradient(
                                    colors: [theme.gold, theme.goldEnd],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .clipShape(Capsule())

                        Text("Life 360")
                            .font(.system(size: 32, weight: .heavy))
                            .foregroundStyle(theme.textPrimary)

                        Text("Your complete life intelligence is on the way. We're building something meaningful.")
                            .font(.system(size: 15))
                            .foregroundStyle(theme.textSecondary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                    }

                    Life360RadarIllustration()
                        .frame(width: 220, height: 220)

                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(theme.gold)
                                    .frame(width: 32, height: 32)
                                Image(systemName: "sparkles")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(theme.comingSoonBackground)
                            }
                            Text("Your Life Map is waiting to form")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(theme.textPrimary)
                        }
                        Text("We're currently building this feature. Life 360 will unify your money, people, work, and growth signals into one powerful view once it's ready.")
                            .font(.system(size: 14))
                            .foregroundStyle(theme.textSecondary)
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(theme.gold.opacity(0.3), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(alignment: .leading, spacing: 12) {
                        Text("WHAT LIFE 360 WILL REVEAL")
                            .font(.system(size: 12, weight: .bold))
                            .tracking(1.2)
                            .foregroundStyle(theme.gold.opacity(0.6))

                        HStack(alignment: .top, spacing: 8) {
                            FeatureCard(title: "Life Alignment", systemImage: "target")
                            FeatureCard(title: "Life Energy", systemImage: "bolt.fill")
                            FeatureCard(title: "Life Balance", systemImage: "scalemass.fill")
                            FeatureCard(title: "Life Momentum", systemImage: "airplane.departure")
                        }
                    }

                    VStack(spacing: 8) {
                        HStack {
                            Text("DEVELOPMENT PROGRESS")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(theme.gold)
                            Spacer()
                            Text("65%")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(theme.gold)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(theme.card)
                                Capsule()
                                    .fill(theme.gold)
                                    .frame(width: geo.size.width * theme.decorativeProgressFraction)
                            }
                        }
                        .frame(height: 8)
                    }

                    HStack(alignment: .top, spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(theme.gold.opacity(0.1))
                                .frame(width: 40, height: 40)
                            Image(systemName: "info.circle")
                                .foregroundStyle(theme.gold)
                        }
                        Text("As we build this feature, we'll keep you updated. Life 360 will be your unified view of everything that matters.")
                            .font(.system(size: 13))
                            .foregroundStyle(theme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(20)
                    .background(Color(hex: 0x1C1B1B).opacity(0.4))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(theme.gold.opacity(0.3), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 20)
            }
        } footer: {
            Button {
                notifyAck = true
            } label: {
                HStack {
                    Image(systemName: "bell.fill")
                    Text("Notify Me When Ready")
                        .font(.system(size: 16, weight: .bold))
                    Spacer()
                    Image(systemName: "arrow.right")
                }
                .foregroundStyle(theme.comingSoonBackground)
                .padding(.horizontal, 24)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(
                    LinearGradient(
                        colors: [theme.gold, theme.goldEnd],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .accessibilityLabel("Notify me when Life 360 is ready")
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
            .background(theme.comingSoonBackground)
        }
        .accessibilityIdentifier("life360.coming_soon")
        .alert("You're on the list", isPresented: $notifyAck) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("We'll notify you when Life 360 is ready.")
        }
    }
}

private struct FeatureCard: View {
    let title: String
    let systemImage: String
    private var theme: GlobalSurfaceTheme.Life360 { GlobalSurfaceTheme.life360 }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [theme.gold, theme.goldEnd],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: 40, height: 40)
                Image(systemName: systemImage)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(theme.comingSoonBackground)
            }
            Text(title)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(theme.textPrimary)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.card)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(theme.gold.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct Life360RadarIllustration: View {
    private var theme: GlobalSurfaceTheme.Life360 { GlobalSurfaceTheme.life360 }

    var body: some View {
        Canvas { context, size in
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            let maxR = min(size.width, size.height) * 0.42
            for f in [0.35, 0.55, 0.75, 1.0] {
                var path = Path()
                path.addEllipse(in: CGRect(
                    x: c.x - maxR * f,
                    y: c.y - maxR * f,
                    width: maxR * f * 2,
                    height: maxR * f * 2
                ))
                context.stroke(
                    path,
                    with: .color(theme.online.opacity(0.18 + (1 - f) * 0.08)),
                    lineWidth: 2
                )
            }
            var ray = Path()
            ray.move(to: c)
            ray.addLine(to: CGPoint(x: c.x + maxR * 0.72, y: c.y - maxR * 0.18))
            context.stroke(ray, with: .color(theme.gold.opacity(0.55)), lineWidth: 2)
            context.fill(
                Path(ellipseIn: CGRect(x: c.x - 10, y: c.y - 10, width: 20, height: 20)),
                with: .color(theme.online)
            )
            let tip = CGPoint(x: c.x + maxR * 0.72, y: c.y - maxR * 0.18)
            context.fill(
                Path(ellipseIn: CGRect(x: tip.x - 5, y: tip.y - 5, width: 10, height: 10)),
                with: .color(theme.gold)
            )
        }
    }
}
