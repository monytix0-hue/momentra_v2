import SwiftUI

/// Business Story v3 entry — Figma TRD-STORY flow (Android `OperatingStoryConfigSheet` + `TradingStoryV3Flow`).
struct OperatingStoryConfigSheet: View {
    let model: OperatingProjectionPayload
    let momentId: String
    let theme: OperatingSheetTheme
    let noun: String
    var onDismiss: () -> Void

    var body: some View {
        TradingStoryExperience(
            model: model,
            momentId: momentId,
            theme: theme,
            noun: noun,
            onDismiss: onDismiss
        )
    }
}

/// Figma `TRD-STORY-STATE · Memory empty stories` (5:22560) — Memory Stories tab empty.
struct TradingStoryMemoryEmpty: View {
    let businessName: String
    let accent: Color
    var onGenerate: () -> Void
    var onDismiss: (() -> Void)? = nil

    var body: some View {
        ZStack {
            TradingStoryColors.night.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Circle().fill(accent).frame(width: 40, height: 40)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Memory")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(TradingStoryColors.ink)
                        Text("\(businessName) · Stories")
                            .font(.system(size: 13))
                            .foregroundStyle(TradingStoryColors.muted)
                    }
                    Spacer()
                    if let onDismiss {
                        Button(action: onDismiss) {
                            Image(systemName: "xmark")
                                .foregroundStyle(TradingStoryColors.ink)
                                .frame(width: 36, height: 36)
                                .background(TradingStoryColors.glass)
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                    }
                }
                HStack(spacing: 8) {
                    ForEach(["Stories", "Photos", "Notes"], id: \.self) { chip in
                        Text(chip)
                            .font(.system(size: 13, weight: chip == "Stories" ? .bold : .medium))
                            .foregroundStyle(chip == "Stories" ? TradingStoryColors.night : TradingStoryColors.ink)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(chip == "Stories" ? accent : TradingStoryColors.glass)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                VStack(spacing: 14) {
                    ZStack {
                        Circle().stroke(accent.opacity(0.35), lineWidth: 3).frame(width: 140, height: 140)
                        Circle().fill(accent.opacity(0.9)).frame(width: 100, height: 100)
                        Image(systemName: "sparkles")
                            .foregroundStyle(.white)
                            .offset(x: 40, y: 40)
                    }
                    Text("No stories yet")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(TradingStoryColors.ink)
                    Text("Turn this moment’s verified activity into an 8-slide Story you can relive and share.")
                        .font(.system(size: 14))
                        .foregroundStyle(TradingStoryColors.muted)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 12)
                    Button(action: onGenerate) {
                        Text("GENERATE STORY")
                            .font(.system(size: 14, weight: .bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .foregroundStyle(TradingStoryColors.night)
                            .background(accent)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(TradingStoryColors.glass)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                Spacer()
            }
            .padding(20)
        }
    }
}
