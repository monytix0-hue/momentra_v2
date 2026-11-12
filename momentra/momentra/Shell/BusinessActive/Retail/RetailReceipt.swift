import PhotosUI
import SwiftUI
import UIKit

import UniformTypeIdentifiers

/// Harbor Mart thermal receipt — on-screen paper preview + WhatsApp PDF/JPG share.
///
/// Style: narrow cream slip, charcoal tabular type, dashed rules, rose only on the
/// duplicate banner. Refs come from real SALE fields (`clientKey` / event id) — never
/// invent fiscal `HM-B-*` numbers. Client re-render until a receipt.pdf API exists.
///
/// Logo is device-local branding keyed by company/moment until a store-branding API exists.
/// Primary upload lives in Retail Locations masters (Store branding); reprint / post&print
/// keep a lighter Replace that also enforces JPEG-only.

let RetailLogoJpegOnlyMessage = "JPEG only — PNG, HEIC and WebP are not allowed"

enum RetailLogoPickResult {
    case saved(UIImage)
    case rejected(String)
}

let HarborMartDemoGstin = "36ADUFS9403L1Z9"

enum RetailReceiptPaperSize: String, CaseIterable, Identifiable, Equatable {
    case thermal2
    case thermal3
    case thermal4
    case thermal6
    case a4

    var id: String { rawValue }

    static let `default`: RetailReceiptPaperSize = .thermal3

    var shortLabel: String {
        switch self {
        case .thermal2: return "2\""
        case .thermal3: return "3\""
        case .thermal4: return "4\""
        case .thermal6: return "6\""
        case .a4: return "A4"
        }
    }

    var label: String {
        switch self {
        case .thermal2: return "2 inch · 58mm"
        case .thermal3: return "3 inch · 80mm"
        case .thermal4: return "4 inch · 112mm"
        case .thermal6: return "6 inch · 152mm"
        case .a4: return "A4 · 210mm"
        }
    }

    /// Typical printable width in millimetres.
    var widthMm: CGFloat {
        switch self {
        case .thermal2: return 58
        case .thermal3: return 80
        case .thermal4: return 112
        case .thermal6: return 152
        case .a4: return 210
        }
    }

    var previewContentWidth: CGFloat {
        switch self {
        case .thermal2: return 200
        case .thermal3: return 280
        case .thermal4: return 340
        case .thermal6: return 400
        case .a4: return 320
        }
    }

    var previewFrameWidth: CGFloat {
        switch self {
        case .a4: return 460
        default: return previewContentWidth
        }
    }

    var pageSizePoints: CGSize {
        let width = (widthMm / 25.4) * 72
        switch self {
        case .a4:
            return CGSize(width: width, height: (297 / 25.4) * 72)
        default:
            // Tall enough for a typical receipt; renderer clips to content.
            return CGSize(width: width, height: width * 2.8)
        }
    }
}

enum RetailReceiptShareFormat: String, CaseIterable, Identifiable {
    case jpg
    case pdf

    var id: String { rawValue }
    var label: String { rawValue.uppercased() }
}

struct RetailReceiptLine: Equatable {
    var name: String
    var qty: String?
    var rate: String?
    var amount: String?
    var meta: String?
}

struct RetailReceiptModel: Equatable {
    var storeName: String
    var tagline: String = "Stock. Sell. Settle."
    var locationLine: String?
    /// Store GSTIN when known; omit line when nil.
    var gstin: String?
    var occurredAt: String?
    var refLabel: String
    var duplicate: Bool
    var lines: [RetailReceiptLine]
    var amountOnly: Bool
    var amountOnlyTitle: String?
    var subtotal: String?
    var gstEstimate: String?
    var tender: String?
    var total: String?
    var currency: String
    var reason: String?
    var footerHonesty: String = "Client duplicate · receipt.pdf API not allocated yet"
}

enum RetailReceiptPaper {
    static let cream = Color(hex: "#FFFBF5")
    static let ink = Color(hex: "#1C1917")
    static let muted = Color(hex: "#57534E")
    static let rule = Color(hex: "#A8A29E")
}

enum RetailReceiptLogoStore {
    private static let defaults = UserDefaults.standard

    private static func scopeKey(companyId: String?, momentId: String?) -> String {
        let company = (companyId?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap { $0.isEmpty ? nil : $0 } ?? "_"
        let moment = (momentId?.trimmingCharacters(in: .whitespacesAndNewlines)).flatMap { $0.isEmpty ? nil : $0 } ?? "_session"
        return "retail.receipt.logo.\(company).\(moment)"
    }

    private static func fileURL(companyId: String?, momentId: String?) -> URL {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            .appendingPathComponent("receipt_logos", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(scopeKey(companyId: companyId, momentId: momentId)).jpg")
    }

    static func loadImage(companyId: String?, momentId: String?) -> UIImage? {
        let url = fileURL(companyId: companyId, momentId: momentId)
        guard FileManager.default.fileExists(atPath: url.path),
              let data = try? Data(contentsOf: url) else { return nil }
        return UIImage(data: data)
    }

    static func isJpegMagic(_ data: Data) -> Bool {
        guard data.count >= 3 else { return false }
        return data[data.startIndex] == 0xFF
            && data[data.startIndex.advanced(by: 1)] == 0xD8
            && data[data.startIndex.advanced(by: 2)] == 0xFF
    }

    /// True when the picker item declares JPEG (or declares nothing). Rejects HEIC/PNG/WebP.
    static func itemLooksLikeJpeg(_ item: PhotosPickerItem) -> Bool {
        let types = item.supportedContentTypes
        if types.isEmpty { return true }
        if types.contains(where: { $0.conforms(to: .jpeg) }) { return true }
        let blocked: [UTType] = [.png, .webP, .heic, .heif, .gif, .tiff]
        if types.contains(where: { type in blocked.contains(where: { type.conforms(to: $0) }) }) {
            return false
        }
        return false
    }

    /// Accept only real JPEG bytes — do not convert PNG/HEIC/WebP.
    static func saveJpegData(_ data: Data, companyId: String?, momentId: String?) -> RetailLogoPickResult {
        guard isJpegMagic(data), let image = UIImage(data: data) else {
            return .rejected(RetailLogoJpegOnlyMessage)
        }
        let url = fileURL(companyId: companyId, momentId: momentId)
        let sized = scale(image, maxEdge: 512)
        guard let out = sized.jpegData(compressionQuality: 0.88) else {
            return .rejected("Could not open that photo")
        }
        do {
            try out.write(to: url, options: .atomic)
            defaults.set(url.path, forKey: scopeKey(companyId: companyId, momentId: momentId))
            return .saved(sized)
        } catch {
            return .rejected(error.localizedDescription)
        }
    }

    static func clear(companyId: String?, momentId: String?) {
        let url = fileURL(companyId: companyId, momentId: momentId)
        try? FileManager.default.removeItem(at: url)
        defaults.removeObject(forKey: scopeKey(companyId: companyId, momentId: momentId))
    }

    private static func scale(_ image: UIImage, maxEdge: CGFloat) -> UIImage {
        let size = image.size
        let longest = max(size.width, size.height)
        guard longest > maxEdge else { return image }
        let scale = maxEdge / longest
        let target = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: target)
        return renderer.image { _ in image.draw(in: CGRect(origin: .zero, size: target)) }
    }
}

enum RetailReceiptBuilder {
    static func storeName(from model: BusinessOperatingModel) -> String {
        let title = model.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return title.isEmpty ? "Harbor Mart" : title
    }

    static func locationLine(from model: BusinessOperatingModel) -> String {
        if let location = model.projection?.location?.trimmingCharacters(in: .whitespacesAndNewlines),
           !location.isEmpty {
            return location
        }
        if let andheri = model.projection?.life?.channels.first(where: {
            $0.localizedCaseInsensitiveContains("Andheri")
        }) {
            return andheri
        }
        return "Harbor Mart Andheri"
    }

    /// Prefer a real 15-char GSTIN; Harbor Mart demo fixtures get `HarborMartDemoGstin`.
    static func gstin(from model: BusinessOperatingModel) -> String? {
        if let reg = model.projection?.gstRegistration?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
           looksLikeGstin(reg) {
            return reg
        }
        if let fromMaster = (model.masters.isEmpty ? model.projection?.masters : model.masters)?
            .compactMap({ $0.gstin?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() })
            .first(where: { looksLikeGstin($0) }) {
            return fromMaster
        }
        let display = storeName(from: model)
        let realLocation = model.projection?.location?.trimmingCharacters(in: .whitespacesAndNewlines)
        let isHarborDemo = display.localizedCaseInsensitiveContains("Harbor Mart")
            || (realLocation?.localizedCaseInsensitiveContains("Harbor Mart") == true)
        return isHarborDemo ? HarborMartDemoGstin : nil
    }

    private static func looksLikeGstin(_ value: String) -> Bool {
        value.count == 15
            && value.allSatisfy { $0.isLetter || $0.isNumber }
            && value.prefix(2).allSatisfy(\.isNumber)
    }

    static func refLabel(clientKey: String?, eventId: String?) -> String {
        if let key = clientKey?.trimmingCharacters(in: .whitespacesAndNewlines), !key.isEmpty {
            return key
        }
        if let id = eventId?.trimmingCharacters(in: .whitespacesAndNewlines), !id.isEmpty {
            return "Sale · \(id)"
        }
        return "Posted sale · no bill no."
    }

    static func fromSale(
        model: BusinessOperatingModel,
        sale: OperatingActivityPayload,
        document: OperatingDocumentPayload? = nil,
        duplicate: Bool = true,
        reason: String? = nil,
        tenderOverride: String? = nil
    ) -> RetailReceiptModel {
        let currency = model.currency
        let docLines = document?.lines ?? []
        let lines: [RetailReceiptLine] = docLines.map { line in
            let qty = line.quantity
            let rate = line.unitPrice
            let amount: String? = {
                guard let q = Decimal(string: qty ?? ""), let r = Decimal(string: rate ?? "") else {
                    return rate.map { OperatingV12Money.amount($0, currency: currency) }
                }
                let product = NSDecimalNumber(decimal: q * r).stringValue
                return OperatingV12Money.amount(product, currency: currency)
            }()
            let meta = [
                line.unit,
                line.gstRate.map { "GST \($0)%" },
            ].compactMap { $0?.nilIfBlank }.joined(separator: " · ")
            return RetailReceiptLine(
                name: line.itemName?.nilIfBlank ?? line.masterName?.nilIfBlank ?? "Line",
                qty: qty,
                rate: rate.map { OperatingV12Money.amount($0, currency: currency) },
                amount: amount,
                meta: meta.nilIfBlank
            )
        }
        let amountOnly = lines.isEmpty
        let totalRaw = document?.grandTotal?.nilIfBlank ?? sale.amount
        let subtotalRaw = document?.subTotal?.nilIfBlank ?? (amountOnly ? nil : totalRaw)
        let gstRaw: String? = {
            if let total = document?.tax?.total?.nilIfBlank { return total }
            let cgst = Decimal(string: document?.tax?.cgst ?? "") ?? 0
            let sgst = Decimal(string: document?.tax?.sgst ?? "") ?? 0
            let sum = cgst + sgst
            return sum > 0 ? NSDecimalNumber(decimal: sum).stringValue : nil
        }()
        let tender = tenderOverride
            ?? sale.paymentStatus?.nilIfBlank
            ?? sale.note.flatMap { note -> String? in
                guard note.contains("Tender:") else { return nil }
                let after = note.components(separatedBy: "Tender:").last?
                    .components(separatedBy: "·")
                    .first?
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                return after?.nilIfBlank
            }

        return RetailReceiptModel(
            storeName: storeName(from: model),
            locationLine: locationLine(from: model),
            gstin: gstin(from: model),
            occurredAt: sale.occurredAt.nilIfBlank,
            refLabel: refLabel(clientKey: sale.clientKey, eventId: sale.id),
            duplicate: duplicate,
            lines: lines,
            amountOnly: amountOnly,
            amountOnlyTitle: amountOnly ? (sale.title.nilIfBlank ?? "Quick amount sale") : nil,
            subtotal: subtotalRaw.map { OperatingV12Money.amount($0, currency: currency) },
            gstEstimate: gstRaw.map { "GST est. \(OperatingV12Money.amount($0, currency: currency))" },
            tender: tender,
            total: totalRaw.map { OperatingV12Money.amount($0, currency: currency) },
            currency: currency,
            reason: reason
        )
    }

    static func fromPosCart(
        model: BusinessOperatingModel,
        title: String,
        lines: [RetailPosLine],
        totalPlain: String,
        tender: String,
        customer: String?,
        quickAmountOnly: Bool,
        gstEstimatePlain: String? = nil
    ) -> RetailReceiptModel {
        let currency = model.currency
        let receiptLines: [RetailReceiptLine] = (quickAmountOnly || lines.isEmpty)
            ? []
            : lines.map { line in
                RetailReceiptLine(
                    name: line.name,
                    qty: "\(line.quantity)",
                    rate: OperatingV12Money.amount(line.unitPrice, currency: currency),
                    amount: OperatingV12Money.amount(
                        NSDecimalNumber(decimal: line.amount).stringValue,
                        currency: currency
                    ),
                    meta: line.gstRate?.nilIfBlank.map { "GST \($0)" }
                )
            }
        return RetailReceiptModel(
            storeName: storeName(from: model),
            locationLine: locationLine(from: model),
            gstin: gstin(from: model),
            occurredAt: nil,
            refLabel: "Just posted · awaiting bill key",
            duplicate: false,
            lines: receiptLines,
            amountOnly: receiptLines.isEmpty,
            amountOnlyTitle: receiptLines.isEmpty ? title : nil,
            subtotal: receiptLines.isEmpty ? nil : OperatingV12Money.amount(totalPlain, currency: currency),
            gstEstimate: gstEstimatePlain.map { "GST est. \(OperatingV12Money.amount($0, currency: currency))" },
            tender: tender.nilIfBlank,
            total: OperatingV12Money.amount(totalPlain, currency: currency),
            currency: currency,
            footerHonesty: [
                customer?.nilIfBlank.map { "Customer · \($0)" },
                "Client copy · receipt.pdf API not allocated yet",
            ].compactMap { $0 }.joined(separator: " · ")
        )
    }

    static func shareText(_ model: RetailReceiptModel) -> String {
        let rule = String(repeating: "-", count: 32)
        var lines: [String] = [
            model.storeName.uppercased(),
            model.tagline,
        ]
        if let location = model.locationLine { lines.append(location) }
        if let gstin = model.gstin { lines.append("GSTIN : \(gstin)") }
        lines.append(rule)
        lines.append(model.duplicate ? "DUPLICATE COPY" : "CUSTOMER COPY")
        lines.append(rule)
        lines.append("Ref \(model.refLabel)")
        if let when = model.occurredAt { lines.append("When \(when)") }
        lines.append(rule)
        if model.amountOnly {
            lines.append(model.amountOnlyTitle ?? "Amount sale")
            lines.append("(No SKU lines · amount only)")
            if let total = model.total { lines.append("TOTAL".padding(toLength: 20, withPad: " ", startingAt: 0) + total) }
        } else {
            lines.append(padCols("ITEM", "QTY", "AMT"))
            lines.append(rule)
            for line in model.lines {
                lines.append(padCols(String(line.name.prefix(18)), line.qty ?? "—", line.amount ?? "—"))
                if let meta = line.meta { lines.append("  \(meta)") }
            }
            lines.append(rule)
            if let sub = model.subtotal { lines.append(padCols("Subtotal", "", sub)) }
            if let gst = model.gstEstimate { lines.append(" \(gst)") }
            if let tender = model.tender { lines.append("Tender \(tender)") }
            if let total = model.total { lines.append(padCols("TOTAL", "", total)) }
        }
        if let reason = model.reason?.nilIfBlank {
            lines.append(rule)
            lines.append("Reprint \(reason)")
        }
        lines.append(rule)
        lines.append("Thank you for shopping with Harbor Mart")
        lines.append(model.footerHonesty)
        return lines.joined(separator: "\n")
    }

    private static func padCols(_ a: String, _ b: String, _ c: String) -> String {
        let left = a.padding(toLength: 18, withPad: " ", startingAt: 0)
        let mid = String(repeating: " ", count: max(0, 4 - b.count)) + b
        let right = String(repeating: " ", count: max(0, 10 - c.count)) + c
        return "\(left) \(mid) \(right)"
    }
}

struct RetailReceiptPreview: View {
    let model: RetailReceiptModel
    var paper: RetailReceiptPaperSize = .default
    var logo: UIImage? = nil

    var body: some View {
        VStack(alignment: .center, spacing: 0) {
            VStack(alignment: .center, spacing: 6) {
                if let logo {
                    Image(uiImage: logo)
                        .resizable()
                        .scaledToFit()
                        .frame(
                            width: paper == .thermal2 ? 56 : (paper == .thermal3 ? 72 : 88),
                            height: paper == .thermal2 ? 40 : (paper == .thermal3 ? 52 : 64)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                }
                Text(model.storeName.uppercased())
                    .font(.system(size: paper == .thermal2 ? 13 : 15, weight: .bold, design: .monospaced))
                    .tracking(1.2)
                    .foregroundStyle(RetailReceiptPaper.ink)
                    .multilineTextAlignment(.center)
                Text(model.tagline)
                    .font(.system(size: 10, weight: .semibold, design: .monospaced))
                    .foregroundStyle(Color(hex: "#F43F5E"))
                    .multilineTextAlignment(.center)
                if let location = model.locationLine {
                    Text(location)
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(RetailReceiptPaper.muted)
                        .multilineTextAlignment(.center)
                }
                if let gstin = model.gstin {
                    Text("GSTIN : \(gstin)")
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundStyle(RetailReceiptPaper.muted)
                        .multilineTextAlignment(.center)
                }
                dash
                Text(model.duplicate ? "DUPLICATE COPY" : "CUSTOMER COPY")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .tracking(1.4)
                    .foregroundStyle(model.duplicate ? Color(hex: "#F43F5E") : RetailReceiptPaper.ink)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 5)
                    .padding(.horizontal, 8)
                    .background(
                        (model.duplicate ? Color(hex: "#F43F5E").opacity(0.14) : Color(hex: "#FBBF24").opacity(0.18))
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                dash
                kv("Ref", model.refLabel)
                if let when = model.occurredAt {
                    kv("When", String(when.prefix(16)))
                }
                dash
                if model.amountOnly {
                    Text(model.amountOnlyTitle ?? "Amount sale")
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundStyle(RetailReceiptPaper.ink)
                        .frame(maxWidth: .infinity)
                    Text("No SKU lines · amount only")
                        .font(.system(size: 9, design: .monospaced))
                        .foregroundStyle(RetailReceiptPaper.muted)
                        .frame(maxWidth: .infinity)
                    if let total = model.total {
                        kv("TOTAL", total, emphatic: true)
                    }
                } else {
                    HStack {
                        Text("ITEM").font(.system(size: 9, design: .monospaced)).foregroundStyle(RetailReceiptPaper.muted)
                        Spacer()
                        Text("QTY").font(.system(size: 9, design: .monospaced)).foregroundStyle(RetailReceiptPaper.muted)
                        Text("AMT").font(.system(size: 9, design: .monospaced)).foregroundStyle(RetailReceiptPaper.muted)
                    }
                    dash
                    ForEach(Array(model.lines.enumerated()), id: \.offset) { _, line in
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(line.name)
                                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                                    .foregroundStyle(RetailReceiptPaper.ink)
                                    .lineLimit(2)
                                if let meta = line.meta {
                                    Text(meta)
                                        .font(.system(size: 9, design: .monospaced))
                                        .foregroundStyle(RetailReceiptPaper.muted)
                                }
                            }
                            Spacer(minLength: 8)
                            Text(line.qty ?? "—")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundStyle(RetailReceiptPaper.ink)
                            Text(line.amount ?? "—")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundStyle(RetailReceiptPaper.ink)
                                .frame(minWidth: 56, alignment: .trailing)
                        }
                    }
                    dash
                    if let sub = model.subtotal { kv("Subtotal", sub) }
                    if let gst = model.gstEstimate {
                        Text(gst)
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundStyle(RetailReceiptPaper.muted)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }
                    if let tender = model.tender { kv("Tender", tender) }
                    if let total = model.total { kv("TOTAL", total, emphatic: true) }
                }
                if let reason = model.reason?.nilIfBlank {
                    dash
                    kv("Reprint", reason)
                }
                dash
                Text("Thank you for shopping with Harbor Mart")
                    .font(.system(size: 10, design: .monospaced))
                    .foregroundStyle(RetailReceiptPaper.ink)
                    .multilineTextAlignment(.center)
                Text(model.footerHonesty)
                    .font(.system(size: 8, design: .monospaced))
                    .foregroundStyle(RetailReceiptPaper.muted)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 16)
            .frame(maxWidth: paper.previewContentWidth)
        }
        .padding(paper == .a4 ? 18 : 0)
        .frame(maxWidth: paper.previewFrameWidth)
        .background(RetailReceiptPaper.cream)
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(Color(hex: "#F43F5E").opacity(0.28), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var dash: some View {
        GeometryReader { geo in
            Path { path in
                path.move(to: CGPoint(x: 0, y: 0.5))
                path.addLine(to: CGPoint(x: geo.size.width, y: 0.5))
            }
            .stroke(style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            .foregroundStyle(RetailReceiptPaper.rule)
        }
        .frame(height: 1)
        .padding(.vertical, 2)
    }

    private func kv(_ label: String, _ value: String, emphatic: Bool = false) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label)
                .font(.system(size: emphatic ? 12 : 10, weight: emphatic ? .bold : .regular, design: .monospaced))
                .foregroundStyle(emphatic ? RetailReceiptPaper.ink : RetailReceiptPaper.muted)
            Spacer(minLength: 8)
            Text(value)
                .font(.system(size: emphatic ? 13 : 10, weight: emphatic ? .bold : .semibold, design: .monospaced))
                .foregroundStyle(RetailReceiptPaper.ink)
                .multilineTextAlignment(.trailing)
                .lineLimit(2)
        }
    }
}

/// Primary store-logo setup — Retail Locations masters (Store branding). JPEG only.
struct RetailStoreBrandingCard: View {
    let theme: OperatingTheme
    let companyId: String?
    let momentId: String?
    var onMessage: (String) -> Void = { _ in }

    @State private var logo: UIImage?
    @State private var logoItem: PhotosPickerItem?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Store branding")
                .font(.plusJakarta(size: 14, weight: .semibold))
                .foregroundStyle(OperatingV12.title)
            Text("Receipt logo · JPEG only · stays on this device")
                .font(.plusJakarta(size: 12))
                .foregroundStyle(RoseSheetChrome.textMuted)
            HStack(alignment: .center, spacing: 12) {
                PhotosPicker(
                    selection: $logoItem,
                    matching: .images,
                    preferredItemEncoding: .compatible
                ) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(RoseSheetChrome.chip)
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .strokeBorder(RoseSheetChrome.chipBorder, lineWidth: 1)
                        if let logo {
                            Image(uiImage: logo)
                                .resizable()
                                .scaledToFill()
                                .frame(width: 64, height: 64)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        } else {
                            Text("JPEG")
                                .font(.plusJakarta(size: 11, weight: .medium))
                                .foregroundStyle(RoseSheetChrome.textMuted)
                        }
                    }
                    .frame(width: 64, height: 64)
                }
                VStack(alignment: .leading, spacing: 6) {
                    PhotosPicker(
                        selection: $logoItem,
                        matching: .images,
                        preferredItemEncoding: .compatible
                    ) {
                        Text(logo == nil ? "Upload JPEG logo" : "Replace logo")
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(RoseSheetChrome.textMuted)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(RoseSheetChrome.chip)
                            .overlay(Capsule().strokeBorder(RoseSheetChrome.chipBorder, lineWidth: 1))
                            .clipShape(Capsule())
                    }
                    if logo != nil {
                        RoseModeChip(
                            theme: theme,
                            text: "Remove logo",
                            selected: false,
                            style: .ticket
                        ) {
                            RetailReceiptLogoStore.clear(companyId: companyId, momentId: momentId)
                            logo = nil
                            onMessage("Receipt logo removed")
                        }
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(RoseSheetChrome.chip)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(theme.accent.opacity(0.35), lineWidth: 1)
        )
        .onAppear {
            logo = RetailReceiptLogoStore.loadImage(companyId: companyId, momentId: momentId)
        }
        .onChange(of: logoItem) { _, item in
            guard let item else { return }
            Task { await applyPickedLogo(item) }
        }
    }

    @MainActor
    private func applyPickedLogo(_ item: PhotosPickerItem) async {
        guard RetailReceiptLogoStore.itemLooksLikeJpeg(item) else {
            onMessage(RetailLogoJpegOnlyMessage)
            logoItem = nil
            return
        }
        guard let data = try? await item.loadTransferable(type: Data.self) else {
            onMessage("Could not open that photo")
            logoItem = nil
            return
        }
        switch RetailReceiptLogoStore.saveJpegData(data, companyId: companyId, momentId: momentId) {
        case .saved(let image):
            logo = image
            onMessage("Receipt logo saved · JPEG")
        case .rejected(let message):
            onMessage(message)
        }
        logoItem = nil
    }
}

/// Size / format / logo / WhatsApp controls for reprint + post&print flows.
/// Lighter Replace remains; primary upload is `RetailStoreBrandingCard` in Locations setup.
struct RetailReceiptShareControls: View {
    let theme: OperatingTheme
    let model: RetailReceiptModel
    let companyId: String?
    let momentId: String?
    @Binding var paper: RetailReceiptPaperSize
    @Binding var format: RetailReceiptShareFormat
    @Binding var logo: UIImage?
    var onShareItems: ([Any]) -> Void

    @State private var logoItem: PhotosPickerItem?
    @State private var logoError: String?

    private var whatsappInstalled: Bool {
        UIApplication.shared.canOpenURL(URL(string: "whatsapp://")!)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Paper size")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(RoseSheetChrome.textMuted)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(RetailReceiptPaperSize.allCases) { size in
                        RoseModeChip(
                            theme: theme,
                            text: size.shortLabel,
                            selected: paper == size,
                            style: .ticket
                        ) { paper = size }
                    }
                }
            }
            Text("Share as")
                .font(.plusJakarta(size: 11))
                .foregroundStyle(RoseSheetChrome.textMuted)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(RetailReceiptShareFormat.allCases) { fmt in
                        RoseModeChip(
                            theme: theme,
                            text: fmt.label,
                            selected: format == fmt,
                            style: .ticket
                        ) { format = fmt }
                    }
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    PhotosPicker(
                        selection: $logoItem,
                        matching: .images,
                        preferredItemEncoding: .compatible
                    ) {
                        Text(logo == nil ? "Add store logo" : "Replace logo")
                            .font(.plusJakarta(size: 12, weight: .medium))
                            .foregroundStyle(RoseSheetChrome.textMuted)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(RoseSheetChrome.chip)
                            .overlay(Capsule().strokeBorder(RoseSheetChrome.chipBorder, lineWidth: 1))
                            .clipShape(Capsule())
                    }
                    if logo != nil {
                        RoseModeChip(
                            theme: theme,
                            text: "Remove logo",
                            selected: false,
                            style: .ticket
                        ) {
                            RetailReceiptLogoStore.clear(companyId: companyId, momentId: momentId)
                            logo = nil
                            logoError = nil
                        }
                    }
                    RoseModeChip(
                        theme: theme,
                        text: "Share on WhatsApp",
                        selected: false,
                        style: .ticket
                    ) {
                        share(preferWhatsApp: true)
                    }
                    RoseModeChip(
                        theme: theme,
                        text: "Share / print",
                        selected: false,
                        style: .ticket
                    ) {
                        share(preferWhatsApp: false)
                    }
                }
            }
            Text("JPEG logo · set in Locations · \(paper.label) · \(format.label)")
                .font(.plusJakarta(size: 10))
                .foregroundStyle(RoseSheetChrome.textMuted)
            if let logoError {
                Text(logoError)
                    .font(.plusJakarta(size: 11, weight: .medium))
                    .foregroundStyle(theme.warning)
            }
        }
        .onChange(of: logoItem) { _, item in
            guard let item else { return }
            Task { await applyPickedLogo(item) }
        }
    }

    @MainActor
    private func applyPickedLogo(_ item: PhotosPickerItem) async {
        guard RetailReceiptLogoStore.itemLooksLikeJpeg(item) else {
            logoError = RetailLogoJpegOnlyMessage
            logoItem = nil
            return
        }
        guard let data = try? await item.loadTransferable(type: Data.self) else {
            logoError = "Could not open that photo"
            logoItem = nil
            return
        }
        switch RetailReceiptLogoStore.saveJpegData(data, companyId: companyId, momentId: momentId) {
        case .saved(let image):
            logo = image
            logoError = nil
        case .rejected(let message):
            logoError = message
        }
        logoItem = nil
    }

    private func share(preferWhatsApp: Bool) {
        let items = RetailReceiptShare.items(
            for: model,
            paper: paper,
            format: format,
            logo: logo
        )
        if preferWhatsApp, whatsappInstalled {
            // Explicit WhatsApp path still goes through the share sheet with file items;
            // UIActivityViewController is the supported way to attach PDF/JPG to WhatsApp.
            onShareItems(items)
        } else {
            onShareItems(items)
        }
    }
}

enum RetailReceiptShare {
    /// Prefer a rendered JPG/PDF of the paper slip; always include formatted text.
    @MainActor
    static func items(
        for model: RetailReceiptModel,
        paper: RetailReceiptPaperSize = .default,
        format: RetailReceiptShareFormat = .jpg,
        logo: UIImage? = nil
    ) -> [Any] {
        let text = RetailReceiptBuilder.shareText(model)
        let preview = RetailReceiptPreview(model: model, paper: paper, logo: logo)
            .padding(8)
            .background(RetailReceiptPaper.cream)
        let renderer = ImageRenderer(content: preview)
        renderer.scale = UIScreen.main.scale
        guard let image = renderer.uiImage else { return [text] }

        switch format {
        case .jpg:
            if let data = image.jpegData(compressionQuality: 0.92) {
                let url = FileManager.default.temporaryDirectory
                    .appendingPathComponent("harbor-mart-receipt-\(UUID().uuidString).jpg")
                try? data.write(to: url, options: .atomic)
                return [url, text]
            }
            return [image, text]
        case .pdf:
            if let url = writePdf(image: image, paper: paper) {
                return [url, text]
            }
            return [image, text]
        }
    }

    /// Backward-compatible helper used by older call sites.
    @MainActor
    static func items(for model: RetailReceiptModel) -> [Any] {
        items(for: model, paper: .default, format: .jpg, logo: nil)
    }

    private static func writePdf(image: UIImage, paper: RetailReceiptPaperSize) -> URL? {
        let page = paper.pageSizePoints
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("harbor-mart-receipt-\(UUID().uuidString).pdf")
        let bounds = CGRect(origin: .zero, size: page)
        let renderer = UIGraphicsPDFRenderer(bounds: bounds)
        do {
            try renderer.writePDF(to: url) { ctx in
                ctx.beginPage()
                let cream = UIColor(red: 1, green: 251 / 255, blue: 245 / 255, alpha: 1)
                cream.setFill()
                ctx.cgContext.fill(bounds)
                let maxW = paper == .a4 ? page.width * 0.62 : page.width
                let scale = maxW / max(image.size.width, 1)
                let drawW = image.size.width * scale
                let drawH = image.size.height * scale
                let origin = CGPoint(
                    x: (page.width - drawW) / 2,
                    y: paper == .a4 ? 36 : 0
                )
                image.draw(in: CGRect(origin: origin, size: CGSize(width: drawW, height: drawH)))
            }
            return url
        } catch {
            return nil
        }
    }
}

private extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        guard let value = self?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else {
            return nil
        }
        return value
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
