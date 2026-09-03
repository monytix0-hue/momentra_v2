import SwiftUI

/// QUARANTINED — static Figma demo copy only. Never wire into production UI paths.
/// Live Wedding screens use APIs + honest empties. Kept for isolated previews/tests.
@available(*, deprecated, message: "Do not use in production UI — quarantine for Figma layout reference only")
enum WeddingDemoContent {
    static let healthScore = 82
    static let healthSubtitle = "Coordination is moving smoothly."
    static let healthDetail = "Planning momentum is strong. Keep the team aligned!"
    static let healthUpdated = "Updated 2 hours ago"
    static let heroDateLabel = "18 Oct 2026"
    static let heroCountdown = "🎉 2 months away!"
    static let heroTypePill = "Wedding • 18 Oct 2026"

    static let healthPills = ["74% Activity", "8 Replies", "Trending Up"]
    
    static let segmentFilled = 6
    static let segmentTotal = 8
    static let demoRsvpCount = "74"
    static let demoTasksDone = "12"
    static let demoTasksTotal = "16"

    struct AttentionItem {
        let emoji: String
        let title: String
        let detail: String
        let cta: String
    }

    static let attentionItems: [AttentionItem] = [
        .init(emoji: "💰", title: "Venue deposit due", detail: "Finalize payment by Oct 1", cta: "Pay Now"),
        .init(emoji: "👥", title: "Guest list finalization", detail: "Confirm headcount for catering", cta: "Review"),
        .init(emoji: "🍽️", title: "Catering menu selection", detail: "Finalize appetizers and desserts", cta: "Choose"),
    ]

    static let progressPercent = 75
    static let tasksDone = 12
    static let tasksTotal = 16
    static let demoRsvpPercent = "87%"
    static let demoTasks = "18"

    struct PartyMember {
        let name: String
        let role: String
        let percent: Int
        let featured: Bool
    }

    static let partyMembers: [PartyMember] = [
        .init(name: "Sarah", role: "Bride", percent: 92, featured: true),
        .init(name: "Mike", role: "Groom", percent: 78, featured: false),
        .init(name: "Priya", role: "Maid of Honor", percent: 72, featured: false),
        .init(name: "Arjun", role: "Best Man", percent: 48, featured: false),
        .init(name: "Meera", role: "Planner", percent: 55, featured: false),
    ]

    struct BudgetCategory {
        let label: String
        let amountLabel: String
        let fraction: CGFloat
    }

    static let budgetCategories: [BudgetCategory] = [
        .init(label: "Venue & Catering", amountLabel: "₹2,80,000", fraction: 0.85),
        .init(label: "Decor & Photography", amountLabel: "₹1,20,000", fraction: 0.45),
    ]

    struct ActivityDemo {
        let emoji: String
        let title: String
        let whenLabel: String
    }

    static let activityFeed: [ActivityDemo] = [
        .init(emoji: "📷", title: "Sarah added venue photos", whenLabel: "Today, 9:42 AM"),
        .init(emoji: "🎵", title: "Mike confirmed DJ booking", whenLabel: "Today, 8:15 AM"),
        .init(emoji: "👗", title: "Priya shared outfit ideas", whenLabel: "Yesterday, 11:23 PM"),
        .init(emoji: "🎁", title: "Meera added registry items", whenLabel: "Yesterday, 6:00 PM"),
    ]

    static let insights: [(emoji: String, title: String, body: String)] = [
        (emoji: "✨", title: "Guest coordination", body: "85% complete — Finalize RSVP this week."),
        (emoji: "📸", title: "Venue momentum", body: "Photos boosted planning momentum — Share more venue details to keep it up."),
    ]

    struct TimelineDay {
        let emoji: String
        let title: String
        let date: String
        let detail: String
        let time: String
    }

    static let momentsTimeline: [TimelineDay] = [
        .init(emoji: "🎨", title: "MEHNDI", date: "17 OCT", detail: "Mehndi ceremony & cocktails", time: "4:00 PM"),
        .init(emoji: "💒", title: "WEDDING", date: "18 OCT", detail: "Main ceremony & reception", time: "11:00 AM"),
        .init(emoji: "🥂", title: "RECEPTION", date: "18 OCT", detail: "Grand reception & dinner", time: "7:00 PM"),
    ]

    struct GalleryTile {
        let title: String
        let countLabel: String?
    }

    static let galleryTiles: [GalleryTile] = [
        .init(title: "Venue decor", countLabel: "12"),
        .init(title: "Mehndi setup", countLabel: "8"),
        .init(title: "Outfits", countLabel: "6"),
    ]

    struct UpcomingEvent {
        let title: String
        let detail: String
        let badge: String?
    }

    static let upcomingEvents: [UpcomingEvent] = [
        .init(title: "Venue walkthrough", detail: "Final check • 12 Oct", badge: "THIS SATURDAY"),
        .init(title: "Outfit fitting", detail: "Next week • Bride & bridesmaids", badge: nil),
    ]

    struct MemoryMilestone {
        let title: String
        let date: String
    }

    static let memoryTimeline: [MemoryMilestone] = [
        .init(title: "The group was created", date: "02 Aug 2026"),
        .init(title: "Venue confirmed and booked", date: "18 Sep 2026"),
        .init(title: "First dress fitting captured", date: "03 Oct 2026"),
        .init(title: "Mehndi night memories", date: "13 Dec 2026"),
    ]

    static let memoryFeatureCards = [
        "First plan confirmed",
        "Everyone contributed",
        "50 RSVPs received",
    ]

    static let memoryInsights: [(String, String)] = [
        ("Strongest memory signal", "Mehndi night was the most emotional shared moment."),
        ("Participation pattern", "Photos were captured by all active members."),
    ]
}
