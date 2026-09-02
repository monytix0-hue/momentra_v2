import SwiftUI

// MARK: - Dashboard list

/// Native iOS dashboard container: plain `List` on a themed background with clear section rows.
struct NativeDashboardScaffold<Content: View>: View {
    var background: Color
    @ViewBuilder var content: () -> Content

    var body: some View {
        List {
            content()
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(background)
    }
}

/// Standard list section styling for branded cards on dark dashboards.
struct NativeListSection<Content: View>: View {
    var insets: EdgeInsets = EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16)
    @ViewBuilder var content: () -> Content

    var body: some View {
        Section {
            content()
        }
        .listRowInsets(insets)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }
}

// MARK: - Sticky footer

struct NativeStickyFooter<Content: View>: View {
    var background: Color
    var horizontalPadding: CGFloat = 16
    var bottomPadding: CGFloat = 8
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(.horizontal, horizontalPadding)
            .padding(.bottom, bottomPadding)
            .frame(maxWidth: .infinity)
            .background(background)
    }
}

extension View {
    /// Pin a primary/secondary CTA above the home indicator and tab bar.
    func nativeStickyFooter<Footer: View>(
        background: Color,
        @ViewBuilder footer: () -> Footer
    ) -> some View {
        safeAreaInset(edge: .bottom, spacing: 0) {
            footer()
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
                .frame(maxWidth: .infinity)
                .background(background)
        }
    }
}

// MARK: - Sheet

/// NavigationStack sheet with inline title, Close, and optional pinned footer.
struct NativeSheetScaffold<Content: View, Footer: View>: View {
    let title: String
    var closeLabel: String = "Close"
    var onClose: () -> Void
    var background: Color = GlobalTheme.surfaceContent
    @ViewBuilder var content: () -> Content
    @ViewBuilder var footer: () -> Footer

    var body: some View {
        NavigationStack {
            content()
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(closeLabel, action: onClose)
                    }
                }
                .safeAreaInset(edge: .bottom) {
                    footer()
                }
        }
        .background(background.ignoresSafeArea())
    }
}

extension NativeSheetScaffold where Footer == EmptyView {
    init(
        title: String,
        closeLabel: String = "Close",
        onClose: @escaping () -> Void,
        background: Color = GlobalTheme.surfaceContent,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.closeLabel = closeLabel
        self.onClose = onClose
        self.background = background
        self.content = content
        self.footer = { EmptyView() }
    }
}
