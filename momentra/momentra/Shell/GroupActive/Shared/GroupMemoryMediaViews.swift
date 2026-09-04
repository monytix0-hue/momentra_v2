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

    var body: some View {
        Group {
            if let url {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
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

    private var urls: [URL] {
        memoryGalleryUrls(from: items)
    }

    var body: some View {
        if urls.isEmpty {
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
                HStack(spacing: 8) {
                    ForEach(Array(urls.enumerated()), id: \.offset) { _, url in
                        RemoteMemoryImage(url: url, placeholderColor: field)
                            .frame(width: tileSize, height: tileSize)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(border, lineWidth: 1)
                            )
                            .background(field)
                    }
                }
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

    var body: some View {
        RemoteMemoryImage(
            url: urlString.flatMap { URL(string: $0) },
            placeholderColor: field
        )
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(border, lineWidth: 1)
        )
        .background(field)
    }
}
