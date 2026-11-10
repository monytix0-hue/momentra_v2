import SwiftUI

/// TRD-STORY-STATE / MTO-STORY-STATE kit — empty, failed, unauthorized, loading, saved.
/// Keep in lockstep with Android `StoryStateKit.kt` (Figma `2657:29510`+).

enum StoryStateKind {
    case memoryEmpty
    case configNoActivity
    case generating
    case generatingFailed
    case conflict
    case unauthorized
    case loadingSkeleton
    case partialPreview
}

struct StoryStateSpec {
    var kind: StoryStateKind
    var title: String
    var body: String
    var primaryLabel: String?
    var secondaryLabel: String?
    var errorCode: String?
    var rows: [(String, String)] = []
}

enum StoryStateSpecs {
    static let night = Color(hex: "#090D16")
    static let ink = Color(hex: "#F8F9FD")
    static let muted = Color(hex: "#F8F9FD").opacity(0.70)
    static let ember = Color(hex: "#E8621A")

    static func memoryEmpty() -> StoryStateSpec {
        StoryStateSpec(
            kind: .memoryEmpty,
            title: "No stories yet",
            body: "Turn this moment’s verified activity into an 8-slide Story you can relive and share.",
            primaryLabel: "GENERATE STORY"
        )
    }

    static func generating() -> StoryStateSpec {
        StoryStateSpec(
            kind: .generating,
            title: "Generating your Moment Story…",
            body: "Building eight slides from verified activity. Nothing is shared until this finishes."
        )
    }

    static func generatingFailed(_ detail: String?) -> StoryStateSpec {
        StoryStateSpec(
            kind: .generatingFailed,
            title: "Story couldn’t finish",
            body: (detail?.isEmpty == false) ? detail! : "Something broke while building the story. Nothing was saved and your data is untouched.",
            primaryLabel: "RETRY",
            secondaryLabel: "Cancel",
            errorCode: "GENERATION_FAILED"
        )
    }

    static func unauthorized() -> StoryStateSpec {
        StoryStateSpec(
            kind: .unauthorized,
            title: "This story isn’t available to you",
            body: "Ask the moment owner for access. We’re not showing any details from it.",
            primaryLabel: "Go back",
            errorCode: "ACCESS_DENIED"
        )
    }

    /// Requires backend reconciliation signals — intentionally not wired in the viewer yet.
    static func conflict() -> StoryStateSpec {
        StoryStateSpec(
            kind: .conflict,
            title: "Numbers need a check before we share",
            body: "Two sources disagree for this period, so totals are held back until they reconcile.",
            primaryLabel: "REVIEW SOURCES",
            secondaryLabel: "Retry check",
            errorCode: "RECONCILIATION_REQUIRED",
            rows: [
                ("Revenue total", "Held · conflict"),
                ("Export & share", "Blocked"),
            ]
        )
    }

    static func loading() -> StoryStateSpec {
        StoryStateSpec(kind: .loadingSkeleton, title: "Loading Story…", body: "")
    }

    static func loadError(_ detail: String?) -> StoryStateSpec {
        StoryStateSpec(
            kind: .generatingFailed,
            title: "Couldn’t load Story",
            body: (detail?.isEmpty == false) ? detail! : "Check your connection and try again.",
            primaryLabel: "RETRY",
            secondaryLabel: "Cancel",
            errorCode: "STORY_LOAD_FAILED"
        )
    }

    static func notStarted(completed: Bool) -> StoryStateSpec {
        StoryStateSpec(
            kind: .memoryEmpty,
            title: completed ? "Story is not ready yet" : "No Story yet",
            body: completed
                ? "Tap Generate to create it so every member can view it."
                : "Complete the moment first, then generate the Story.",
            primaryLabel: "GENERATE STORY"
        )
    }

    static func configNoActivity(period: String) -> StoryStateSpec {
        StoryStateSpec(
            kind: .configNoActivity,
            title: "No verified activity in this period",
            body: "We found no reconciled records for \(period). We won’t show zeros for data we don’t have.",
            primaryLabel: "CHANGE PERIOD",
            secondaryLabel: "Generate · needs activity",
            rows: [
                ("Activity", "Not recorded"),
                ("Money", "Not recorded"),
                ("Network", "Not recorded"),
            ]
        )
    }
}

struct StoryStateBody: View {
    let spec: StoryStateSpec
    var accent: Color = StoryStateSpecs.ember
    var onPrimary: (() -> Void)?
    var onSecondary: (() -> Void)?

    var body: some View {
        ZStack {
            StoryStateSpecs.night.ignoresSafeArea()
            Group {
                if spec.kind == .loadingSkeleton || spec.kind == .generating {
                    VStack(spacing: 12) {
                        ProgressView().tint(accent)
                        Text(spec.title)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(StoryStateSpecs.ink)
                            .multilineTextAlignment(.center)
                        if !spec.body.isEmpty {
                            Text(spec.body)
                                .font(.system(size: 13))
                                .foregroundStyle(StoryStateSpecs.muted)
                                .multilineTextAlignment(.center)
                        }
                    }
                    .padding(24)
                } else {
                    VStack(spacing: 14) {
                        mark
                        Text(spec.title)
                            .font(.system(size: 22, weight: .heavy))
                            .foregroundStyle(StoryStateSpecs.ink)
                            .multilineTextAlignment(.center)
                        if !spec.body.isEmpty {
                            Text(spec.body)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(StoryStateSpecs.muted)
                                .multilineTextAlignment(.center)
                        }
                        if let code = spec.errorCode {
                            Text(code)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(accent)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(accent.opacity(0.12))
                                .clipShape(Capsule())
                        }
                        ForEach(spec.rows, id: \.0) { row in
                            HStack {
                                Text(row.0).foregroundStyle(StoryStateSpecs.muted)
                                Spacer()
                                Text(row.1).foregroundStyle(Color(hex: "#F5A623"))
                            }
                            .font(.system(size: 13))
                        }
                        if let label = spec.primaryLabel {
                            let enabled = onPrimary != nil &&
                                !(spec.kind == .configNoActivity && label.hasPrefix("Generate"))
                            Button(action: { onPrimary?() }) {
                                Text(label)
                                    .font(.system(size: 14, weight: .heavy))
                                    .tracking(1)
                                    .foregroundStyle(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
                                    .background(
                                        LinearGradient(
                                            colors: enabled
                                                ? [accent, Color(hex: "#F5A623")]
                                                : [Color.white.opacity(0.12), Color.white.opacity(0.08)],
                                            startPoint: .top,
                                            endPoint: .bottom
                                        )
                                    )
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .disabled(!enabled)
                        }
                        if let label = spec.secondaryLabel,
                           onSecondary != nil || spec.kind == .configNoActivity {
                            let secondaryEnabled = onSecondary != nil &&
                                !(spec.kind == .configNoActivity && label.hasPrefix("Generate"))
                            Button(action: { onSecondary?() }) {
                                Text(label)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(secondaryEnabled ? StoryStateSpecs.ink : StoryStateSpecs.muted)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(Color.white.opacity(0.10))
                                    .overlay(
                                        Capsule().stroke(Color.white.opacity(0.22), lineWidth: 1)
                                    )
                                    .clipShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .disabled(!secondaryEnabled)
                        }
                    }
                    .padding(22)
                    .background(Color.white.opacity(0.08))
                    .overlay(
                        RoundedRectangle(cornerRadius: 32, style: .continuous)
                            .stroke(Color.white.opacity(0.14), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                    .padding(.horizontal, 24)
                }
            }
        }
    }

    private var mark: some View {
        ZStack(alignment: .bottomTrailing) {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [accent.opacity(0.28), .clear],
                        center: .center,
                        startRadius: 10,
                        endRadius: 70
                    )
                )
                .frame(width: 120, height: 120)
            Circle()
                .fill(Color.white.opacity(0.06))
                .overlay(Circle().stroke(Color.white.opacity(0.18), lineWidth: 1.5))
                .frame(width: 88, height: 88)
                .overlay(
                    Text("M")
                        .font(.system(size: 28, weight: .black))
                        .foregroundStyle(StoryStateSpecs.ink)
                )
            Circle()
                .fill(accent)
                .overlay(Circle().stroke(StoryStateSpecs.night, lineWidth: 3))
                .frame(width: 36, height: 36)
                .overlay(
                    Text(glyph)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                )
                .offset(x: 4, y: 4)
        }
        .frame(width: 120, height: 120)
    }

    private var glyph: String {
        switch spec.kind {
        case .unauthorized: return "⊘"
        case .conflict: return "⇄"
        case .generatingFailed, .configNoActivity, .partialPreview: return "!"
        default: return "✦"
        }
    }
}

struct StorySavedPanel: View {
    let versionLabel: String
    var accent: Color = StoryStateSpecs.ember
    var onShare: () -> Void
    var onBack: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Spacer()
            ZStack {
                Circle()
                    .fill(accent.opacity(0.12))
                    .frame(width: 48, height: 48)
                Text("✓")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(accent)
            }
            Text("Story saved to Memory")
                .font(.system(size: 26, weight: .heavy))
                .foregroundStyle(StoryStateSpecs.ink)
                .multilineTextAlignment(.center)
            Text(versionLabel)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(StoryStateSpecs.ink.opacity(0.65))
                .multilineTextAlignment(.center)
            Text("New or backdated entries create Version 2 with a changed-facts badge.")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(StoryStateSpecs.ink.opacity(0.45))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            Spacer().frame(height: 24)
            Button(action: onShare) {
                Text("Share story")
                    .font(.system(size: 16, weight: .heavy))
                    .foregroundStyle(StoryStateSpecs.night)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(accent.opacity(0.92))
                    .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
            }
            .buttonStyle(.plain)
            Button(action: onBack) {
                Text("Back to Memory")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(StoryStateSpecs.ink)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(Color.white.opacity(0.10))
                    .overlay(
                        RoundedRectangle(cornerRadius: 24, style: .continuous)
                            .stroke(Color.white.opacity(0.22), lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(StoryStateSpecs.night.ignoresSafeArea())
    }
}
