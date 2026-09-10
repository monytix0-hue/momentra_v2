import SwiftUI

typealias GroupMemoryItem = APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem
typealias GroupMemoryMedia = APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem.GroupMemoryMedia

func memoryGalleryUrls(from items: [GroupMemoryItem]) -> [URL] {
    items.flatMap { item in
        (item.media ?? []).compactMap { media -> URL? in
            guard let raw = media.downloadUrl, !raw.isEmpty else { return nil }
            return URL(string: raw)
        }
    }
}

/// Remote memory photo via `AsyncImage` (signed download URLs from list/facet payloads).
struct RemoteMemoryImage: View {
    let url: URL?
    var placeholderColor: Color = Color(hex: "#322E40")
    var contentMode: ContentMode = .fill

    var body: some View {
        Group {
            if let url {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: contentMode)
                    case .failure:
                        Text("📷")
                            .font(.system(size: 18))
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(placeholderColor)
                    case .empty:
                        ProgressView()
                            .tint(.white.opacity(0.5))
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(placeholderColor)
                    @unknown default:
                        placeholderColor
                    }
                }
            } else {
                Text("📷")
                    .font(.system(size: 18))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(placeholderColor)
            }
        }
    }
}

private struct MemoryPhotoViewerState: Identifiable {
    let id = UUID()
    let urls: [URL]
    let initialIndex: Int
}

/// Full-screen lightbox with pinch / double-tap zoom.
struct MemoryPhotoFullscreenViewer: View {
    let urls: [URL]
    var initialIndex: Int = 0
    var onDismiss: () -> Void

    @State private var index: Int
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var dragOffset: CGFloat = 0

    init(urls: [URL], initialIndex: Int = 0, onDismiss: @escaping () -> Void) {
        self.urls = urls
        self.initialIndex = initialIndex
        self.onDismiss = onDismiss
        _index = State(initialValue: min(max(0, initialIndex), max(urls.count - 1, 0)))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if urls.isEmpty {
                Text("📷")
                    .font(.system(size: 40))
                    .foregroundStyle(.white.opacity(0.6))
            } else if urls.count == 1 {
                zoomablePage(url: urls[0])
            } else {
                TabView(selection: $index) {
                    ForEach(Array(urls.enumerated()), id: \.offset) { i, url in
                        zoomablePage(url: url)
                            .tag(i)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))
                .onChange(of: index) { _, _ in
                    resetZoom()
                }
            }

            VStack {
                HStack {
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 28))
                            .symbolRenderingMode(.hierarchical)
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel("Close")
                }
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
        }
        .offset(y: dragOffset)
        .opacity(1 - min(abs(dragOffset) / 400, 0.5))
        .gesture(dismissDrag)
        .statusBarHidden(true)
    }

    private var dismissDrag: some Gesture {
        DragGesture()
            .onChanged { value in
                guard scale <= 1.05 else { return }
                dragOffset = value.translation.height
            }
            .onEnded { value in
                guard scale <= 1.05 else {
                    dragOffset = 0
                    return
                }
                if abs(value.translation.height) > 120 {
                    onDismiss()
                } else {
                    withAnimation(.easeOut(duration: 0.2)) { dragOffset = 0 }
                }
            }
    }

    @ViewBuilder
    private func zoomablePage(url: URL) -> some View {
        RemoteMemoryImage(url: url, placeholderColor: .black, contentMode: .fit)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .scaleEffect(scale)
            .offset(offset)
            .gesture(magnifyGesture)
            .onTapGesture(count: 2) {
                withAnimation(.easeInOut(duration: 0.2)) {
                    if scale > 1.1 {
                        resetZoom()
                    } else {
                        scale = 2.5
                        lastScale = 2.5
                    }
                }
            }
    }

    private var magnifyGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                scale = min(max(lastScale * value, 1), 4)
            }
            .onEnded { _ in
                lastScale = scale
                if scale < 1.05 {
                    withAnimation(.easeOut(duration: 0.15)) { resetZoom() }
                }
            }
            .simultaneously(with:
                DragGesture()
                    .onChanged { value in
                        guard scale > 1.05 else { return }
                        offset = value.translation
                    }
                    .onEnded { _ in
                        if scale <= 1.05 {
                            withAnimation(.easeOut(duration: 0.15)) { offset = .zero }
                        }
                    }
            )
    }

    private func resetZoom() {
        scale = 1
        lastScale = 1
        offset = .zero
    }
}

/// Horizontal strip of memory photos; empty copy when no downloadable media.
struct MemoryPhotoGalleryStrip: View {
    let items: [GroupMemoryItem]
    var emptyMessage: String
    var emptyDetail: String
    var text: Color
    var muted: Color
    var field: Color
    var border: Color
    var tileSize: CGFloat = 96
    var showMediaCountBadge: Bool = false

    @State private var viewer: MemoryPhotoViewerState?

    private var tiles: [(url: URL?, count: Int)] {
        if showMediaCountBadge {
            return items.compactMap { item -> (URL?, Int)? in
                let count = item.mediaCount ?? item.media?.count ?? 0
                guard let url = item.primaryDownloadUrl.flatMap({ URL(string: $0) }) else {
                    return count > 0 ? (nil, count) : nil
                }
                return (url, max(count, 1))
            }
        }
        return memoryGalleryUrls(from: items).map { ($0, 0) }
    }

    private var openableUrls: [URL] {
        tiles.compactMap(\.url)
    }

    var body: some View {
        if tiles.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                Text(emptyMessage)
                    .font(.plusJakarta(size: 13, weight: .semibold))
                    .foregroundStyle(text)
                Text(emptyDetail)
                    .font(.plusJakarta(size: 12))
                    .foregroundStyle(muted)
            }
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(Array(tiles.enumerated()), id: \.offset) { idx, tile in
                        ZStack(alignment: .topTrailing) {
                            RemoteMemoryImage(url: tile.url, placeholderColor: field)
                                .frame(
                                    width: showMediaCountBadge ? nil : tileSize,
                                    height: showMediaCountBadge ? 140 : tileSize
                                )
                                .frame(maxWidth: showMediaCountBadge ? .infinity : nil)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                            if showMediaCountBadge, tile.count > 0 {
                                Text("\(tile.count)")
                                    .font(.plusJakarta(size: 10, weight: .bold))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.black.opacity(0.5))
                                    .clipShape(Capsule())
                                    .padding(6)
                            }
                        }
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color(hex: "#40E88A4F"), lineWidth: 1.5)
                        )
                        .frame(width: showMediaCountBadge ? 110 : tileSize)
                        .contentShape(RoundedRectangle(cornerRadius: 16))
                        .onTapGesture {
                            guard let url = tile.url else { return }
                            let urls = openableUrls
                            let start = urls.firstIndex(of: url) ?? min(idx, max(urls.count - 1, 0))
                            guard !urls.isEmpty else { return }
                            viewer = MemoryPhotoViewerState(urls: urls, initialIndex: start)
                        }
                    }
                }
            }
            .fullScreenCover(item: $viewer) { state in
                MemoryPhotoFullscreenViewer(
                    urls: state.urls,
                    initialIndex: state.initialIndex,
                    onDismiss: { viewer = nil }
                )
            }
        }
    }
}

/// Optional thumbnail for timeline / list rows when `primaryDownloadUrl` is present.
struct MemoryMediaThumb: View {
    let urlString: String?
    var size: CGFloat = 44
    var border: Color = Color(hex: "#322E40")
    var field: Color = Color(hex: "#252230")

    @State private var viewer: MemoryPhotoViewerState?

    private var url: URL? {
        urlString.flatMap { URL(string: $0) }
    }

    var body: some View {
        RemoteMemoryImage(
            url: url,
            placeholderColor: field
        )
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(border, lineWidth: 1)
        )
        .background(field)
        .contentShape(RoundedRectangle(cornerRadius: 10))
        .onTapGesture {
            guard let url else { return }
            viewer = MemoryPhotoViewerState(urls: [url], initialIndex: 0)
        }
        .fullScreenCover(item: $viewer) { state in
            MemoryPhotoFullscreenViewer(
                urls: state.urls,
                initialIndex: state.initialIndex,
                onDismiss: { viewer = nil }
            )
        }
    }
}
