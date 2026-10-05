import Foundation

/// Copy for a company that has no active Business moment.
/// Sample activity and numeric placeholders must not look like live data.
enum BusinessNoMomentEmptyCopy {
    static let momentsSampleRows: [(title: String, time: String)] = []
    static let absent = "Not set up"

    static let forbiddenSampleTitles = [
        "You bought supplies",
        "A customer paid",
        "You saved a receipt",
    ]
}
