import Combine
import SwiftUI

final class TourGuide: ObservableObject {
    @Published var steps: [TourStep] = []
    @Published var currentIndex: Int = 0
    @Published var isPresented: Bool = false

    var currentStep: TourStep? {
        guard currentIndex < steps.count else { return nil }
        return steps[currentIndex]
    }

    var isFinished: Bool {
        currentIndex >= steps.count
    }

    func next() {
        guard currentIndex < steps.count - 1 else {
            isPresented = false
            return
        }
        currentIndex += 1
    }

    func skip() {
        isPresented = false
        currentIndex = steps.count
    }
}
