import Foundation

enum FutureQuickAddKind: String, CaseIterable, Identifiable {
    case milestone, investment, goal, opportunity, pivot, progress, learning
    var id: String { rawValue }
}

enum LifestyleQuickAddKind: String, CaseIterable, Identifiable {
    case habit, routine, wellness, experience, wellbeing, discovery, expression, adjust
    var id: String { rawValue }
}

enum RelationshipsQuickAddKind: String, CaseIterable, Identifiable {
    case note, gift, meeting, connection, shared, investment, support, adjust
    var id: String { rawValue }
}
