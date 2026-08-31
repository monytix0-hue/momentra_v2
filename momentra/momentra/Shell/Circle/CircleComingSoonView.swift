import SwiftUI

/// Figma `1075:7556` Circle Coming Soon — static local UI in Circle context.
/// Does not call GET /life360 or Circle CRUD.
struct CircleComingSoonView: View {
    @State private var notifyAck = false

    var body: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(
                colors: [CircleComingSoonTheme.pageStart, CircleComingSoonTheme.pageEnd],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 28) {
                    CircleNetworkIllustration()
                        .frame(height: 220)
                        .frame(maxWidth: .infinity)

                    VStack(spacing: 12) {
                        Text("COMING SOON")
                            .font(.system(size: 12, weight: .heavy))
                            .tracking(1.5)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(
                                LinearGradient(
                                    colors: [CircleComingSoonTheme.accent, CircleComingSoonTheme.accentEnd],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .clipShape(Capsule())

                        Text("Circle")
                            .font(.system(size: 36, weight: .heavy))
                            .foregroundStyle(CircleComingSoonTheme.textPrimary)

                        Text("Your people network is being crafted. A new way to see how your connections shape your life.")
                            .font(.system(size: 16))
                            .foregroundStyle(CircleComingSoonTheme.textSecondary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                    }

                    VStack(alignment: .leading, spacing: 16) {
                        Text("WHAT CIRCLE WILL REVEAL")
                            .font(.system(size: 12, weight: .heavy))
                            .tracking(1.5)
                            .foregroundStyle(CircleComingSoonTheme.accent.opacity(0.6))

                        HStack(spacing: 16) {
                            FeatureCard(
                                title: "Relationship Map",
                                systemImage: "point.3.connected.trianglepath.dotted",
                                start: CircleComingSoonTheme.accent,
                                end: CircleComingSoonTheme.accentEnd,
                                border: CircleComingSoonTheme.accent
                            )
                            FeatureCard(
                                title: "Shared Moments",
                                systemImage: "heart.fill",
                                start: CircleComingSoonTheme.lavender,
                                end: CircleComingSoonTheme.accent,
                                border: CircleComingSoonTheme.lavender
                            )
                        }
                        HStack(spacing: 16) {
                            FeatureCard(
                                title: "Life Balance",
                                systemImage: "scalemass.fill",
                                start: CircleComingSoonTheme.accentEnd,
                                end: CircleComingSoonTheme.peach,
                                border: CircleComingSoonTheme.accentEnd
                            )
                            FeatureCard(
                                title: "Momentum",
                                systemImage: "bolt.fill",
                                start: CircleComingSoonTheme.peach,
                                end: CircleComingSoonTheme.accent,
                                border: CircleComingSoonTheme.peach
                            )
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(CircleComingSoonTheme.accent)
                                    .frame(width: 32, height: 32)
                                Image(systemName: "sparkles")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundStyle(CircleComingSoonTheme.pageStart)
                            }
                            Text("We're building Circle to map your connections automatically")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(CircleComingSoonTheme.textPrimary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        Text("We're currently building this feature. Circle will unify your people, plans, and money signals into one powerful view once it's ready.")
                            .font(.system(size: 14))
                            .foregroundStyle(CircleComingSoonTheme.textSecondary)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(CircleComingSoonTheme.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(CircleComingSoonTheme.accent.opacity(0.3), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(spacing: 8) {
                        HStack {
                            Text("DEVELOPMENT PROGRESS")
                                .font(.system(size: 12, weight: .heavy))
                                .foregroundStyle(CircleComingSoonTheme.accent)
                            Spacer()
                            Text("45%")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(CircleComingSoonTheme.accent)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule().fill(CircleComingSoonTheme.card)
                                Capsule()
                                    .fill(
                                        LinearGradient(
                                            colors: [CircleComingSoonTheme.accent, CircleComingSoonTheme.accentEnd],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .frame(width: geo.size.width * CircleComingSoonTheme.decorativeProgressFraction)
                            }
                        }
                        .frame(height: 8)
                    }

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
                        .foregroundStyle(CircleComingSoonTheme.pageStart)
                        .padding(.horizontal, 24)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            LinearGradient(
                                colors: [CircleComingSoonTheme.accent, CircleComingSoonTheme.accentEnd],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .accessibilityLabel("Notify me when Circle is ready")

                    HStack(alignment: .top, spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(CircleComingSoonTheme.accent.opacity(0.05))
                                .frame(width: 40, height: 40)
                            Image(systemName: "info.circle")
                                .foregroundStyle(CircleComingSoonTheme.accent)
                        }
                        Text("As we build this feature, we'll keep you updated. Circle will be your unified view of everything that matters.")
                            .font(.system(size: 13))
                            .foregroundStyle(CircleComingSoonTheme.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(24)
                    .background(CircleComingSoonTheme.accent.opacity(0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(CircleComingSoonTheme.accent.opacity(0.3), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    Spacer(minLength: 24)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 24)
            }
        }
        .accessibilityIdentifier("circle.coming_soon")
        .alert("You're on the list", isPresented: $notifyAck) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("We'll notify you when Circle is ready.")
        }
    }
}

private struct FeatureCard: View {
    let title: String
    let systemImage: String
    let start: Color
    let end: Color
    let border: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ZStack {
                Circle()
                    .fill(LinearGradient(colors: [start, end], startPoint: .leading, endPoint: .trailing))
                    .frame(width: 48, height: 48)
                Image(systemName: systemImage)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
            }
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(CircleComingSoonTheme.textPrimary)
                .lineLimit(2)
                .minimumScaleFactor(0.85)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CircleComingSoonTheme.cardAlt.opacity(0.95))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(border.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct CircleNetworkIllustration: View {
    var body: some View {
        Canvas { context, size in
            let c = CGPoint(x: size.width / 2, y: size.height / 2)
            let maxR = min(size.width, size.height) * 0.42
            for f in [0.55, 0.78, 1.0] {
                var path = Path()
                path.addEllipse(in: CGRect(
                    x: c.x - maxR * f,
                    y: c.y - maxR * f,
                    width: maxR * f * 2,
                    height: maxR * f * 2
                ))
                context.stroke(
                    path,
                    with: .color(CircleComingSoonTheme.lavender.opacity(0.12 + (1 - f) * 0.08)),
                    lineWidth: 2
                )
            }
            let nodes: [CGPoint] = [
                CGPoint(x: c.x - maxR * 0.45, y: c.y - maxR * 0.35),
                CGPoint(x: c.x + maxR * 0.45, y: c.y - maxR * 0.35),
                CGPoint(x: c.x - maxR * 0.45, y: c.y + maxR * 0.4),
                CGPoint(x: c.x + maxR * 0.45, y: c.y + maxR * 0.4),
            ]
            for n in nodes {
                var ray = Path()
                ray.move(to: c)
                ray.addLine(to: n)
                context.stroke(ray, with: .color(CircleComingSoonTheme.accent.opacity(0.55)), lineWidth: 2)
            }
            context.fill(
                Path(ellipseIn: CGRect(x: c.x - 14, y: c.y - 14, width: 28, height: 28)),
                with: .color(CircleComingSoonTheme.accent)
            )
            for (i, n) in nodes.enumerated() {
                let r: CGFloat = CGFloat(5 + i)
                context.fill(
                    Path(ellipseIn: CGRect(x: n.x - r, y: n.y - r, width: r * 2, height: r * 2)),
                    with: .color(i % 2 == 0 ? CircleComingSoonTheme.accent : CircleComingSoonTheme.lavender)
                )
            }
        }
    }
}
