import SwiftUI
import Combine

enum OnboardingMode {
    case firstRun
    case replay
}

private let sceneIds = ["onboarding_1", "onboarding_2", "onboarding_3"]
private let particleCount = 80
private let scene1End: TimeInterval = 2.0
private let scene2End: TimeInterval = 5.0
private let ctaAt: TimeInterval = 7.2
private let midnight = Color(red: 5 / 255, green: 8 / 255, blue: 22 / 255)

private struct Particle {
    var x: CGFloat
    var y: CGFloat
    var z: CGFloat
    var vx: CGFloat
    var vy: CGFloat
    var ox: CGFloat
    var oy: CGFloat
    var tx: CGFloat
    var ty: CGFloat
    var r: CGFloat
    var opacity: CGFloat
}

private func easeInOut(_ t: CGFloat) -> CGFloat {
    t < 0.5 ? 2 * t * t : 1 - pow(-2 * t + 2, 2) / 2
}

private func sampleMTargets(count: Int, size: CGSize) -> [CGPoint] {
    let path: [CGPoint] = [
        .init(x: 14, y: 100), .init(x: 14, y: 50), .init(x: 34, y: 74),
        .init(x: 54, y: 24), .init(x: 54, y: 100), .init(x: 54, y: 24),
        .init(x: 74, y: 74), .init(x: 94, y: 50), .init(x: 94, y: 100),
    ]
    let scale = min(size.width, size.height) * 0.42
    let cx = size.width * 0.5
    let cy = size.height * 0.38
    return (0..<count).map { i in
        let t = CGFloat(i) / CGFloat(count) * CGFloat(path.count - 1)
        let i0 = min(Int(t), path.count - 2)
        let i1 = i0 + 1
        let f = t - CGFloat(i0)
        let x = path[i0].x + (path[i1].x - path[i0].x) * f
        let y = path[i0].y + (path[i1].y - path[i0].y) * f
        return CGPoint(
            x: cx + ((x - 54) / 120) * scale * 2.2,
            y: cy + ((y - 62) / 120) * scale * 2.0
        )
    }
}

private func createParticles(size: CGSize, reduceMotion: Bool) -> [Particle] {
    let targets = sampleMTargets(count: particleCount, size: size)
    return (0..<particleCount).map { i in
        let x = reduceMotion ? targets[i].x : CGFloat.random(in: 0...max(size.width, 1))
        let y = reduceMotion ? targets[i].y : CGFloat.random(in: 0...max(size.height, 1))
        return Particle(
            x: x, y: y,
            z: CGFloat.random(in: 0.4...1.0),
            vx: CGFloat.random(in: -0.075...0.075),
            vy: CGFloat.random(in: -0.075...0.075),
            ox: x, oy: y,
            tx: targets[i].x, ty: targets[i].y,
            r: CGFloat.random(in: 1.2...3.4),
            opacity: CGFloat.random(in: 0.25...0.8)
        )
    }
}

/// First-run / replay onboarding:
/// 1) Figma product carousel
/// 2) Existing cinematic particle welcome
struct OnboardingView: View {
    let mode: OnboardingMode
    let onFinished: () -> Void

    @State private var stage: OnboardingStage = .product

    private enum OnboardingStage {
        case product
        case cinematic
    }

    var body: some View {
        Group {
            switch stage {
            case .product:
                ProductOnboardingView {
                    stage = .cinematic
                }
            case .cinematic:
                CinematicOnboardingView(mode: mode, onFinished: onFinished)
            }
        }
    }
}

/// Cinematic welcome — particle network morphs into Momentra M.
struct CinematicOnboardingView: View {
    let mode: OnboardingMode
    let onFinished: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var scene = 0
    @State private var showCta = false
    @State private var exiting = false
    @State private var exitProgress: CGFloat = 0
    @State private var startDate = Date()
    @State private var particles: [Particle] = []
    @State private var canvasSize: CGSize = .zero
    @State private var finished = false
    @State private var tick: Int = 0

    private let timer = Timer.publish(every: 1.0 / 60.0, on: .main, in: .common).autoconnect()

    private var sceneScreen: String {
        switch scene {
        case 0: AnalyticsScreens.onboardingScene1
        case 1: AnalyticsScreens.onboardingScene2
        default: AnalyticsScreens.onboardingScene3
        }
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                midnight.ignoresSafeArea()

                Canvas { context, size in
                    drawParticles(context: &context, size: size)
                }
                .ignoresSafeArea()
                .onAppear {
                    canvasSize = proxy.size
                    particles = createParticles(size: proxy.size, reduceMotion: reduceMotion)
                    startDate = Date()
                    if reduceMotion {
                        scene = 2
                        showCta = true
                    }
                }

                VStack {
                    HStack {
                        Spacer()
                        Button("Skip") {
                            trackWidget(screenName: AnalyticsScreens.onboarding, widgetName: AnalyticsWidgets.onboardingSkip)
                            finish(reason: "skip")
                        }
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.4))
                            .padding(.trailing, 16)
                            .padding(.top, 8)
                    }
                    Spacer()
                }
                .padding(.top, proxy.safeAreaInsets.top)

                copyOverlay
                    .allowsHitTesting(false)

                if showCta && !exiting {
                    VStack {
                        Spacer()
                        Button(action: beginExit) {
                            Text("Step Inside")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(.white)
                                .frame(maxWidth: 320)
                                .frame(height: 52)
                                .background(
                                    LinearGradient(
                                        colors: [MomentraBrandTokens.ember500, MomentraBrandTokens.amber500],
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    ),
                                    in: Capsule()
                                )
                        }
                        .padding(.horizontal, 36)
                        .padding(.bottom, max(proxy.safeAreaInsets.bottom, 12) + 24)
                    }
                }
            }
            .preferredColorScheme(.dark)
            .id(sceneScreen)
            .trackScreen(sceneScreen)
            .onReceive(timer) { now in
                guard !finished else { return }
                if canvasSize != proxy.size && proxy.size.width > 0 {
                    canvasSize = proxy.size
                    if particles.isEmpty {
                        particles = createParticles(size: proxy.size, reduceMotion: reduceMotion)
                    }
                }
                stepSimulation(now: now, size: proxy.size)
                tick &+= 1
            }
        }
    }

    @ViewBuilder
    private var copyOverlay: some View {
        VStack(spacing: 10) {
            if scene == 0 {
                Text("Life is already unfolding.")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 28)
            } else if scene == 1 {
                Text("Keep what matters together.")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 28)
            } else {
                Spacer().frame(height: 112)
                Text("Momentra")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundStyle(MomentraBrandTokens.textOnDark)
                Text("One place for every moment.")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.55))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.9), value: scene)
    }

    private func stepSimulation(now: Date, size: CGSize) {
        guard size.width > 0, size.height > 0 else { return }
        let elapsed = reduceMotion ? 7.5 : now.timeIntervalSince(startDate)
        let morph: CGFloat
        if reduceMotion {
            morph = 1
        } else if elapsed >= scene2End {
            morph = easeInOut(min(1, CGFloat((elapsed - scene2End) / 2.0)))
        } else {
            morph = 0
        }

        let newScene: Int
        if reduceMotion { newScene = 2 }
        else if elapsed < scene1End { newScene = 0 }
        else if elapsed < scene2End { newScene = 1 }
        else { newScene = 2 }
        if newScene != scene { scene = newScene }
        if !reduceMotion && elapsed >= ctaAt && !showCta && !exiting { showCta = true }

        let w = size.width
        let h = size.height
        let exitT = exitProgress

        for i in particles.indices {
            if exiting {
                let dx = particles[i].x - w / 2
                let dy = particles[i].y - h / 2
                particles[i].x += dx * 0.02 * (1 + exitT * 3)
                particles[i].y += dy * 0.02 * (1 + exitT * 3)
                particles[i].z += 0.02
                particles[i].opacity *= 0.985
            } else if morph > 0.001 {
                particles[i].ox += particles[i].vx * (1 - morph)
                particles[i].oy += particles[i].vy * (1 - morph)
                if particles[i].ox < 0 || particles[i].ox > w { particles[i].vx *= -1 }
                if particles[i].oy < 0 || particles[i].oy > h { particles[i].vy *= -1 }
                particles[i].x = particles[i].ox + (particles[i].tx - particles[i].ox) * morph
                particles[i].y = particles[i].oy + (particles[i].ty - particles[i].oy) * morph
            } else {
                particles[i].x += particles[i].vx
                particles[i].y += particles[i].vy
                if particles[i].x < 0 || particles[i].x > w { particles[i].vx *= -1 }
                if particles[i].y < 0 || particles[i].y > h { particles[i].vy *= -1 }
                particles[i].ox = particles[i].x
                particles[i].oy = particles[i].y
            }
        }
    }

    private func drawParticles(context: inout GraphicsContext, size: CGSize) {
        let elapsed = reduceMotion ? 7.5 : Date().timeIntervalSince(startDate)
        let morph: CGFloat
        let linkAlpha: CGFloat
        if reduceMotion {
            morph = 1
            linkAlpha = 0.15
        } else if elapsed >= scene2End {
            morph = easeInOut(min(1, CGFloat((elapsed - scene2End) / 2.0)))
            linkAlpha = 1 - morph * 0.85
        } else if elapsed >= scene1End {
            morph = 0
            linkAlpha = easeInOut(min(1, CGFloat((elapsed - scene1End) / 0.8)))
        } else {
            morph = 0
            linkAlpha = 0
        }

        context.fill(
            Path(CGRect(origin: .zero, size: size)),
            with: .radialGradient(
                Gradient(colors: [
                    Color(red: 45 / 255, green: 31 / 255, blue: 94 / 255).opacity(0.35),
                    .clear,
                ]),
                center: CGPoint(x: size.width * 0.7, y: size.height * 0.2),
                startRadius: 0,
                endRadius: size.width * 0.45
            )
        )

        let exitT = exitProgress
        let w = size.width
        let h = size.height

        if linkAlpha > 0.02 && !exiting {
            let maxDist = min(w, h) * 0.12
            var edges = 0
            for i in particles.indices {
                if edges >= 90 { break }
                for j in (i + 1)..<particles.count {
                    if edges >= 90 { break }
                    let dx = particles[i].x - particles[j].x
                    let dy = particles[i].y - particles[j].y
                    let d = sqrt(dx * dx + dy * dy)
                    if d < maxDist {
                        let alpha = (1 - d / maxDist) * linkAlpha * 0.45
                        var path = Path()
                        path.move(to: CGPoint(x: particles[i].x, y: particles[i].y))
                        path.addLine(to: CGPoint(x: particles[j].x, y: particles[j].y))
                        context.stroke(
                            path,
                            with: .color(Color(red: 0.7, green: 0.63, blue: 1).opacity(alpha)),
                            lineWidth: 0.8
                        )
                        edges += 1
                    }
                }
            }
        }

        for p in particles {
            let glow = 6 + p.z * 6
            let base = morph > 0.5 ? MomentraBrandTokens.ember500 : Color(red: 0.78, green: 0.74, blue: 1)
            let op = p.opacity * (exiting ? (1 - exitT * 0.3) : 1)
            let center = CGPoint(x: p.x, y: p.y)
            context.fill(
                Path(ellipseIn: CGRect(x: p.x - glow, y: p.y - glow, width: glow * 2, height: glow * 2)),
                with: .radialGradient(
                    Gradient(colors: [base.opacity(op), base.opacity(0)]),
                    center: center,
                    startRadius: 0,
                    endRadius: glow
                )
            )
            let coreR = p.r * p.z * (1 + exitT * 2)
            context.fill(
                Path(ellipseIn: CGRect(x: p.x - coreR, y: p.y - coreR, width: coreR * 2, height: coreR * 2)),
                with: .color(MomentraBrandTokens.textOnDark.opacity(op * 0.9))
            )
        }

        if exitT > 0 {
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(MomentraBrandTokens.textOnDark.opacity(exitT * 0.55)))
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(MomentraBrandTokens.indigo700.opacity(exitT * 0.25)))
        }
    }

    private func beginExit() {
        guard !exiting else { return }
        trackWidget(screenName: AnalyticsScreens.onboarding, widgetName: AnalyticsWidgets.onboardingStepInside)
        exiting = true
        withAnimation(.linear(duration: 1.4)) {
            exitProgress = 1
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            finish(reason: "complete")
        }
    }

    private func finish(reason: String) {
        guard !finished else { return }
        finished = true
        if mode == .firstRun {
            OnboardingPrefs.markSeen()
        }
        onFinished()
    }
}
