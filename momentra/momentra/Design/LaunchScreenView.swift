import SwiftUI
import UIKit

struct LaunchScreenView: View {
    var onFinish: (() -> Void)? = nil

    @State private var dotsVisible = [false, false, false, false, false]
    @State private var ghostOpacity: Double = 0
    @State private var peakProgress: CGFloat = 0
    @State private var arcOpacity: Double = 0
    @State private var sparkScale: CGFloat = 0
    @State private var sparkOpacity: Double = 0
    @State private var sparkPulse: CGFloat = 1
    @State private var wordOpacity: Double = 0
    @State private var wordOffset: CGFloat = 12
    @State private var fdotOpacity: Double = 0
    @State private var fdotScale: CGFloat = 0
    @State private var tagOpacity: Double = 0
    @State private var orb1Opacity: Double = 0
    @State private var orb2Opacity: Double = 0

    private let indigo = MomentraBrandTokens.brand
    private let ember = MomentraBrandTokens.cta
    private let amber = MomentraBrandTokens.progress
    private let soft = MomentraBrandTokens.textOnDark

    var body: some View {
        ZStack {
            indigo.ignoresSafeArea()

            Circle().fill(ember.opacity(0.18))
                .frame(width: 260, height: 260)
                .offset(x: 110, y: -190)
                .opacity(orb1Opacity)

            Circle().fill(amber.opacity(0.12))
                .frame(width: 200, height: 200)
                .offset(x: -110, y: 210)
                .opacity(orb2Opacity)

            VStack(spacing: 20) {
                Spacer()

                MarkCanvas(
                    dotsVisible: dotsVisible,
                    ghostOpacity: ghostOpacity,
                    peakProgress: peakProgress,
                    arcOpacity: arcOpacity,
                    sparkScale: sparkScale * sparkPulse,
                    sparkOpacity: sparkOpacity,
                    soft: soft, ember: ember, amber: amber
                )
                .frame(width: 120, height: 120)

                VStack(spacing: 5) {
                    HStack(alignment: .top, spacing: 0) {
                        Text("momentr")
                            .font(.system(size: 32, weight: .medium))
                            .foregroundColor(soft)
                            .kerning(-0.5)

                        ZStack(alignment: .topTrailing) {
                            Text("a")
                                .font(.system(size: 32, weight: .medium))
                                .foregroundColor(ember)
                                .kerning(-0.5)

                            Circle()
                                .fill(amber)
                                .frame(width: 7, height: 7)
                                .offset(x: 2, y: -10)
                                .opacity(fdotOpacity)
                                .scaleEffect(fdotScale)
                        }
                    }

                    Text("TOGETHER · FORWARD")
                        .font(.system(size: 9, weight: .regular))
                        .tracking(3)
                        .foregroundColor(soft.opacity(0.38 * tagOpacity))
                }
                .opacity(wordOpacity)
                .offset(y: wordOffset)

                Spacer()
            }
        }
        .onAppear { runAnimation() }
    }

    func runAnimation() {
        func schedule(delay: Double, dur: Double, spring: Bool = false, block: @escaping () -> Void) {
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                if spring {
                    withAnimation(.interpolatingSpring(stiffness: 350, damping: 13)) { block() }
                } else {
                    withAnimation(.easeOut(duration: dur)) { block() }
                }
            }
        }

        schedule(delay: 0.10, dur: 0.8) { orb1Opacity = 1 }
        schedule(delay: 0.30, dur: 0.8) { orb2Opacity = 1 }

        let dotDelays = [0.28, 0.44, 0.60, 0.76, 0.92]
        dotDelays.enumerated().forEach { i, d in
            DispatchQueue.main.asyncAfter(deadline: .now() + d) {
                withAnimation(.interpolatingSpring(stiffness: 380, damping: 12)) {
                    dotsVisible[i] = true
                }
            }
        }

        schedule(delay: 1.08, dur: 0.3) { ghostOpacity = 1 }
        withAnimation(.easeInOut(duration: 0.52).delay(1.4)) { peakProgress = 1 }
        schedule(delay: 1.92, dur: 0.2) { arcOpacity = 1 }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.13) {
            withAnimation(.easeOut(duration: 0.08)) { sparkOpacity = 1 }
            withAnimation(.interpolatingSpring(stiffness: 420, damping: 10)) { sparkScale = 1 }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
            withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                sparkPulse = 1.2
            }
        }

        schedule(delay: 2.33, dur: 0.45) { wordOpacity = 1; wordOffset = 0 }
        schedule(delay: 2.57, dur: 0.2, spring: true) { fdotOpacity = 1; fdotScale = 1 }
        schedule(delay: 2.72, dur: 0.5) { tagOpacity = 1 }

        DispatchQueue.main.asyncAfter(deadline: .now() + 3.3) {
            onFinish?()
        }
    }
}

struct MarkCanvas: View {
    let dotsVisible: [Bool]
    let ghostOpacity: Double
    let peakProgress: CGFloat
    let arcOpacity: Double
    let sparkScale: CGFloat
    let sparkOpacity: Double
    let soft: Color
    let ember: Color
    let amber: Color

    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 120
            func p(_ v: CGFloat) -> CGFloat { v * s }

            let ghostPath = Path {
                $0.move(to: .init(x: p(14), y: p(100)))
                $0.addLine(to: .init(x: p(14), y: p(50)))
                $0.addLine(to: .init(x: p(34), y: p(74)))
                $0.addLine(to: .init(x: p(54), y: p(24)))
                $0.addLine(to: .init(x: p(54), y: p(100)))
            }
            ctx.stroke(ghostPath,
                with: .color(soft.opacity(ghostOpacity * 0.15)),
                style: .init(lineWidth: p(8), lineCap: .round, lineJoin: .round))

            let dotPts: [(CGFloat, CGFloat)] = [
                (14, 100), (14, 62), (34, 74), (54, 32), (54, 100),
            ]
            for (i, pt) in dotPts.enumerated() {
                guard dotsVisible[i] else { continue }
                let (cx, cy) = pt
                let r = p(6)
                ctx.fill(Path(ellipseIn: .init(x: p(cx) - r, y: p(cy) - r, width: r * 2, height: r * 2)),
                         with: .color(soft))
            }

            if peakProgress > 0 {
                let pts: [(CGFloat, CGFloat)] = [
                    (54, 100), (54, 32), (74, 74), (94, 32), (96, 100),
                ]
                let total = pts.count - 1
                let drawn = Int(peakProgress * CGFloat(total))
                for i in 0..<min(drawn + 1, total) {
                    let t = CGFloat(i) / CGFloat(total)
                    let col = lerpColor(ember, amber, t: t)
                    var seg = Path()
                    seg.move(to: .init(x: p(pts[i].0), y: p(pts[i].1)))
                    seg.addLine(to: .init(x: p(pts[i + 1].0), y: p(pts[i + 1].1)))
                    ctx.stroke(seg, with: .color(col),
                               style: .init(lineWidth: p(8), lineCap: .round, lineJoin: .round))
                }
            }

            if arcOpacity > 0 {
                var arc = Path()
                arc.move(to: .init(x: p(94), y: p(32)))
                arc.addQuadCurve(
                    to: .init(x: p(104), y: p(16)),
                    control: .init(x: p(98), y: p(20)))
                ctx.stroke(arc,
                    with: .color(amber.opacity(arcOpacity * 0.7)),
                    style: .init(lineWidth: p(2.5), lineCap: .round))
            }

            if sparkOpacity > 0 {
                let sc = sparkScale
                let sx = p(105), sy = p(18)
                let ro = p(10) * sc
                ctx.fill(Path(ellipseIn: .init(x: sx - ro, y: sy - ro, width: ro * 2, height: ro * 2)),
                         with: .color(amber.opacity(sparkOpacity)))
                let ri = p(5.5) * sc
                ctx.fill(Path(ellipseIn: .init(x: sx - ri, y: sy - ri, width: ri * 2, height: ri * 2)),
                         with: .color(ember.opacity(sparkOpacity)))
            }
        }
    }
}

private func lerpColor(_ a: Color, _ b: Color, t: CGFloat) -> Color {
    let ca = UIColor(a).cgColor.components ?? [0, 0, 0, 1]
    let cb = UIColor(b).cgColor.components ?? [0, 0, 0, 1]
    return Color(red: Double(ca[0] + (cb[0] - ca[0]) * t),
                 green: Double(ca[1] + (cb[1] - ca[1]) * t),
                 blue: Double(ca[2] + (cb[2] - ca[2]) * t))
}

#Preview { LaunchScreenView() }
