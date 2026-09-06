import SwiftUI

extension TourStep {
    static let createMoment = TourStep(
        accessibilityID: "topbar.new_moment",
        title: "Create a Moment",
        message: "Tap the 'New' button in the top bar to start capturing a new moment — a spend, feeling, task, or memory.",
        action: nil,
        arrowPosition: .top
    )
    
    static let activateMoment = TourStep(
        accessibilityID: "bottom.quickadd",
        title: "Activate a Moment",
        message: "After creating, tap 'Activate' in the setup catalog to make your moment active and start tracking.",
        action: nil,
        arrowPosition: .bottom
    )
    
    static let quickaddsTab = TourStep(
        accessibilityID: "bottom.quickadd",
        title: "Quickadds",
        message: "Tap the 'Quickadds' tab at the bottom to quickly add expenses, income, tasks, or memories for your moment.",
        action: nil,
        arrowPosition: .top
    )
    
    static let personalQuickadds = TourStep(
        accessibilityID: "bottom.quickadd",
        title: "Personal Quickadds",
        message: "Personal moments show money Q&A — master expense, income/transfer, and savings quickadds.",
        action: nil,
        arrowPosition: .bottom
    )
    
    static let businessQuickadds = TourStep(
        accessibilityID: "bottom.quickadd",
        title: "Business Quickadds",
        message: "Business moments have expense, revenue, invoice, and member quickadds to track team operations.",
        action: nil,
        arrowPosition: .bottom
    )
    
    static let groupQuickadds = TourStep(
        accessibilityID: "bottom.quickadd",
        title: "Group Quickadds",
        message: "Group moments have living, purchase, experience, and wedding quickadds for shared tracking.",
        action: nil,
        arrowPosition: .bottom
    )
}