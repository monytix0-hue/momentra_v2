import SwiftUI

struct TourOverlay<Content: View>: View {
    let isHighlighted: Bool
    let arrowPosition: Edge
    let content: Content
    
    init(isHighlighted: Bool, arrowPosition: Edge, content: Content) {
        self.isHighlighted = isHighlighted
        self.arrowPosition = arrowPosition
        self.content = content
    }
    
    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .center) {
                content
                
                if isHighlighted {
                    // Semi-transparent overlay covering everything except the target area
                    Color.black.opacity(0.4)
                        .ignoresSafeArea()
                        .mask(
                            Group {
                                switch arrowPosition {
                                case .top:
                                    // Cutout at top for arrow
                                    Rectangle()
                                        .frame(height: 80)
                                        .offset(y: -geometry.safeAreaInsets.top)
                                    
                                case .bottom:
                                    Rectangle()
                                        .frame(height: 80)
                                        .offset(y: geometry.safeAreaInsets.top + 80)
                                    
                                case .leading:
                                    Rectangle()
                                        .frame(width: 80)
                                        .offset(x: -geometry.safeAreaInsets.leading)
                                    
                                case .trailing:
                                    Rectangle()
                                        .frame(width: 80)
                                        .offset(x: -geometry.safeAreaInsets.leading - geometry.size.width + 80)
                                    
                                @unknown default:
                                    Color.clear
                                }
                            }
                        )
                    
                    // Arrow pointer
                    TourArrowTriangle()
                        .fill(Color.white)
                        .frame(width: 20, height: 20)
                        .offset(arrowOffset(for: arrowPosition))
                }
            }
        }
    }

    private func arrowOffset(for edge: Edge) -> CGSize {
        switch edge {
        case .top: return CGSize(width: 0, height: -10)
        case .bottom: return CGSize(width: 0, height: 10)
        case .leading: return CGSize(width: -10, height: 0)
        case .trailing: return CGSize(width: 10, height: 0)
        @unknown default: return .zero
        }
    }
}

struct TourArrowTriangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}

struct TourStepView: View {
    let step: TourStep
    @Binding var isPresented: Bool
    @Namespace private var namespace
    
    @State private var targetID: String?
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Spacer()
                Button("Done") {
                    isPresented = false
                }
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.gray.opacity(0.3))
                .cornerRadius(20)
            }
            
            VStack(spacing: 20) {
                Text(step.title)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                
                Text(step.message)
                    .font(.system(size: 15, weight: .regular))
                    .foregroundStyle(.white.opacity(0.85))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 24)
                
                if let action = step.action {
                    Button("Let's Go") {
                        action()
                        isPresented = false
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 200, height: 44)
                    .background(MomentraBrandTokens.ember500, in: Capsule())
                    .padding(.top, 8)
                }
            }
            .padding(24)
            .background(Color(hex: "#1C233D"))
            .cornerRadius(16)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .overlay(alignment: .topTrailing) {
                TourOverlay(isHighlighted: true, arrowPosition: step.arrowPosition, content: EmptyView())
            }
        }
        .ignoresSafeArea()
        .background(Color.black.opacity(isPresented ? 0.3 : 0))
        .onAppear { targetID = step.accessibilityID }
    }
}