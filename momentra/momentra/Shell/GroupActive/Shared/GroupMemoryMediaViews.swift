import SwiftUI

typealias GroupMemoryItem = APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem
typealias GroupMemoryMedia = APIClient.GroupMemoryPayload.MemoryInner.GroupMemoryItem.GroupMemoryMedia

/// Maps Quick Add memory chips → `memory.memory.memory_type` enum.
func groupMemoryTypeCode(forChip label: String) -> String {
    switch label.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
    case "milestone": return "MILESTONE"
    case "lesson": return "LEARNING"
    case "reflection": return "EXPERIENCE"
    default: return "GENERAL"
    }
}

func memoryPhotoTitle(_ raw: String?) -> String? {
    let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    return trimmed.isEmpty ? nil : trimmed
}

struct MemoryGalleryPhoto {
    let url: URL
    let title: String?
}

func memoryGalleryPhotos(from items: [GroupMemoryItem]) -> [MemoryGalleryPhoto] {
    items.flatMap { item in
        let title = memoryPhotoTitle(item.title)
        return (item.media ?? []).compactMap { media -> MemoryGalleryPhoto? in
            guard let raw = media.downloadUrl, !raw.isEmpty, let url = URL(string: raw) else { return nil }
            return MemoryGalleryPhoto(url: url, title: title)
        }
    }
}

func memoryGalleryUrls(from items: [GroupMemoryItem]) -> [URL] {
    memoryGalleryPhotos(from: items).map(\.url)
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
    let titles: [String]
    let initialIndex: Int
}

/// Full-screen lightbox with pinch / double-tap zoom.
struct MemoryPhotoFullscreenViewer: View {
    let urls: [URL]
    var titles: [String] = []
    var initialIndex: Int = 0
    var onDismiss: () -> Void

    @State private var index: Int
    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var dragOffset: CGFloat = 0

    init(urls: [URL], titles: [String] = [], initialIndex: Int = 0, onDismiss: @escaping () -> Void) {
        self.urls = urls
        self.titles = titles
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
                .padding(.horizontal, 20)
                Spacer()
                if urls.indices.contains(index), index < titles.count, !titles[index].isEmpty {
                    Text(titles[index])
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 16)
                        .background(Color.black.opacity(0.55))
                }
            }
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

    private struct GalleryTile {
        let url: URL?
        let count: Int
        let title: String?
    }

    private var tiles: [GalleryTile] {
        if showMediaCountBadge {
            return items.compactMap { item in
                let count = item.mediaCount ?? item.media?.count ?? 0
                let title = memoryPhotoTitle(item.title)
                guard let url = item.primaryDownloadUrl.flatMap({ URL(string: $0) }) else {
                    return count > 0 ? GalleryTile(url: nil, count: count, title: title) : nil
                }
                return GalleryTile(url: url, count: max(count, 1), title: title)
            }
        }
        return memoryGalleryPhotos(from: items).map { GalleryTile(url: $0.url, count: 0, title: $0.title) }
    }

    private var openable: [GalleryTile] {
        tiles.filter { $0.url != nil }
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
                        ZStack(alignment: .bottomLeading) {
                            ZStack(alignment: .topTrailing) {
                                RemoteMemoryImage(url: tile.url, placeholderColor: field)
                                    .frame(
                                        width: showMediaCountBadge ? nil : tileSize,
                                        height: showMediaCountBadge ? 140 : tileSize
                                    )
                                    .frame(maxWidth: showMediaCountBadge ? .infinity : nil)
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
                            if let title = tile.title {
                                Text(title)
                                    .font(.plusJakarta(size: 10, weight: .semibold))
                                    .foregroundStyle(.white)
                                    .lineLimit(2)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 4)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(Color.black.opacity(0.55))
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color(hex: "#40E88A4F"), lineWidth: 1.5)
                        )
                        .frame(width: showMediaCountBadge ? 110 : tileSize)
                        .contentShape(RoundedRectangle(cornerRadius: 16))
                        .onTapGesture {
                            guard let url = tile.url else { return }
                            let photos = openable
                            let urls = photos.compactMap(\.url)
                            let start = urls.firstIndex(of: url) ?? min(idx, max(urls.count - 1, 0))
                            guard !urls.isEmpty else { return }
                            viewer = MemoryPhotoViewerState(
                                urls: urls,
                                titles: photos.map { $0.title ?? "" },
                                initialIndex: start
                            )
                        }
                    }
                }
            }
            .fullScreenCover(item: $viewer) { state in
                MemoryPhotoFullscreenViewer(
                    urls: state.urls,
                    titles: state.titles,
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
            viewer = MemoryPhotoViewerState(urls: [url], titles: [], initialIndex: 0)
        }
        .fullScreenCover(item: $viewer) { state in
            MemoryPhotoFullscreenViewer(
                urls: state.urls,
                titles: state.titles,
                initialIndex: state.initialIndex,
                onDismiss: { viewer = nil }
            )
        }
    }
}

/// Moments Shared Gallery "View all" — grid of every downloadable photo.
struct MemoryGalleryListSheet: View {
    let items: [GroupMemoryItem]
    var chrome: MomentsChrome
    var onDismiss: () -> Void

    @State private var viewer: MemoryPhotoViewerState?

    private var photos: [MemoryGalleryPhoto] { memoryGalleryPhotos(from: items) }

    private let columns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                Group {
                    if photos.isEmpty {
                        GroupEmptySection(message: "No photos yet", detail: "Add a memory with a photo from Quick Add.")
                            .padding(.top, 24)
                    } else {
                        LazyVGrid(columns: columns, spacing: 8) {
                            ForEach(Array(photos.enumerated()), id: \.offset) { index, photo in
                                ZStack(alignment: .bottomLeading) {
                                    RemoteMemoryImage(url: photo.url, placeholderColor: chrome.card)
                                        .aspectRatio(1, contentMode: .fill)
                                        .frame(maxWidth: .infinity)
                                    if let title = photo.title {
                                        Text(title)
                                            .font(.plusJakarta(size: 10, weight: .semibold))
                                            .foregroundStyle(.white)
                                            .lineLimit(2)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 4)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .background(Color.black.opacity(0.55))
                                    }
                                }
                                .clipped()
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(chrome.border, lineWidth: 1)
                                )
                                .contentShape(RoundedRectangle(cornerRadius: 12))
                                .onTapGesture {
                                    viewer = MemoryPhotoViewerState(
                                        urls: photos.map(\.url),
                                        titles: photos.map { $0.title ?? "" },
                                        initialIndex: index
                                    )
                                }
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(chrome.bg)
            .navigationTitle("Shared Gallery")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onDismiss)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .fullScreenCover(item: $viewer) { state in
            MemoryPhotoFullscreenViewer(
                urls: state.urls,
                titles: state.titles,
                initialIndex: state.initialIndex,
                onDismiss: { viewer = nil }
            )
        }
    }
}
