import SwiftUI
import UniformTypeIdentifiers

/// Shared receipt/bill pick + preview for group expense and contribution flows.
struct GroupReceiptPickControl: View {
    var label: String = "Receipt"
    var muted: Color
    var field: Color
    var border: Color
    var enabled: Bool
    var uploading: Bool
    var fileName: String?
    var onPick: () -> Void
    var onClear: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label.uppercased())
                .font(.system(size: 10, weight: .bold))
                .foregroundStyle(muted)
            Button(action: onPick) {
                HStack {
                    Text(
                        uploading
                            ? "Uploading…"
                            : (fileName?.isEmpty == false ? fileName! : "📎 Attach PDF/Img")
                    )
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(muted)
                    .lineLimit(1)
                    Spacer(minLength: 4)
                    if fileName?.isEmpty == false {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 14))
                            .foregroundStyle(muted)
                            .onTapGesture(perform: onClear)
                    }
                }
                .padding(.horizontal, 12)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
                .background(field)
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(style: StrokeStyle(lineWidth: 1, dash: [4]))
                        .foregroundStyle(border)
                )
            }
            .buttonStyle(.plain)
            .disabled(!enabled || uploading)
        }
    }
}

struct GroupReceiptAttachmentRow: View {
    let contentType: String?
    let downloadUrl: String?
    let fallbackLabel: String
    var accent: Color
    var text: Color
    var muted: Color

    private var isImage: Bool {
        (contentType ?? "").lowercased().hasPrefix("image/")
    }

    var body: some View {
        Group {
            if isImage, let raw = downloadUrl, let url = URL(string: raw) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(height: 120)
                            .frame(maxWidth: .infinity)
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    case .failure:
                        fileChip
                    default:
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .frame(height: 80)
                    }
                }
                .onTapGesture {
                    UIApplication.shared.open(url)
                }
            } else {
                fileChip
            }
        }
    }

    private var fileChip: some View {
        Button {
            if let raw = downloadUrl, let url = URL(string: raw) {
                UIApplication.shared.open(url)
            }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "doc.fill")
                    .foregroundStyle(accent)
                Text(fallbackLabel)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(text)
                    .lineLimit(1)
                Spacer()
                if downloadUrl != nil {
                    Text("Open")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(accent)
                }
            }
            .padding(12)
            .background(Color.white.opacity(0.06))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(downloadUrl == nil)
    }
}

struct ContributionReceiptSheet: View {
    let momentId: String
    let item: APIClient.GroupContributionItem
    var chrome: MomentsChrome
    var onDismiss: () -> Void

    @State private var attachments: [APIClient.ContributionAttachment] = []
    @State private var loading = true
    @State private var error: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(item.displayName ?? "Member")
                        .font(.plusJakarta(size: 16, weight: .bold))
                        .foregroundStyle(chrome.text)
                    Text(GroupFinanceFormat.formatMoney(item.amount, currencyCode: item.currencyCode ?? "INR"))
                        .font(.plusJakarta(size: 14, weight: .semibold))
                        .foregroundStyle(chrome.secondary)

                    if loading {
                        ProgressView().tint(chrome.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 24)
                    } else if let error {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(Color(hex: "#F87171"))
                    } else if attachments.isEmpty {
                        Text("No receipt attached.")
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(chrome.secondary)
                            .padding(.vertical, 16)
                    } else {
                        ForEach(Array(attachments.enumerated()), id: \.element.id) { idx, att in
                            GroupReceiptAttachmentRow(
                                contentType: att.contentType,
                                downloadUrl: att.downloadUrl,
                                fallbackLabel: "Receipt \(idx + 1)",
                                accent: chrome.accent,
                                text: chrome.text,
                                muted: chrome.secondary
                            )
                        }
                    }
                }
                .padding(20)
            }
            .background(chrome.bg)
            .navigationTitle("Receipt")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onDismiss)
                        .foregroundStyle(chrome.accent)
                }
            }
        }
        .task {
            loading = true
            error = nil
            do {
                attachments = try await APIClient.shared.listContributionAttachments(
                    momentId: momentId,
                    contributionId: item.contributionId
                )
            } catch {
                self.error = error.localizedDescription
                attachments = []
            }
            loading = false
        }
        .presentationDetents([.medium, .large])
    }
}

enum GroupReceiptUpload {
    static func readFile(url: URL) throws -> (Data, String, String) {
        let accessed = url.startAccessingSecurityScopedResource()
        defer { if accessed { url.stopAccessingSecurityScopedResource() } }
        let data = try Data(contentsOf: url)
        let ext = url.pathExtension.lowercased()
        let mime: String
        switch ext {
        case "pdf": mime = "application/pdf"
        case "png": mime = "image/png"
        case "jpg", "jpeg": mime = "image/jpeg"
        case "heic", "heif": mime = "image/heic"
        default: mime = "application/octet-stream"
        }
        return (data, mime, url.lastPathComponent)
    }
}
