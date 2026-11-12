import AVFoundation
import PhotosUI
import SwiftUI
import UIKit
import Vision

/// Retail invoice / SKU barcode + QR scanner. Returns the raw payload for catalogue match
/// or receive-line fill. Distinct from `GroupJoinQrScanner` (invite-link only).
struct RetailBarcodeScanner: View {
    var title: String = "Scan barcode"
    var subtitle: String = "Point at a product barcode, QR, or supplier invoice code"
    var onCode: (String) -> Void
    var onDismiss: () -> Void

    @State private var accepted = false
    @State private var cameraDenied = false
    @State private var cameraReady = false
    @State private var photoItem: PhotosPickerItem?
    @State private var scanError: String?
    @State private var decodingUpload = false

    private static let retailSymbologies: [VNBarcodeSymbology] = [
        .qr, .ean13, .ean8, .upce, .code128, .code39, .itf14, .dataMatrix,
    ]

    var body: some View {
        ZStack {
            if cameraReady, !cameraDenied {
                ScannerCameraView(onRaw: { raw in acceptRaw(raw) }, symbologies: Self.retailSymbologies)
                    .ignoresSafeArea()
            }

            VStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(title)
                        .font(.plusJakarta(size: 20, weight: .bold))
                        .foregroundStyle(.white)
                    Text(banner)
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(.white.opacity(0.7))
                    if let scanError {
                        Text(scanError)
                            .font(.plusJakarta(size: 13))
                            .foregroundStyle(Color(hex: "#F87171"))
                            .padding(.top, 4)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                .safeAreaPadding(.top)
                Spacer()
                RoundedRectangle(cornerRadius: 24)
                    .stroke(Color(hex: "#F43F5E"), lineWidth: 2)
                    .frame(width: 240, height: 240)
                Spacer()
                HStack(spacing: 12) {
                    PhotosPicker(
                        selection: $photoItem,
                        matching: .images,
                        photoLibrary: .shared()
                    ) {
                        Text("Upload")
                            .font(.plusJakarta(size: 15, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 28)
                            .padding(.vertical, 12)
                            .background(Color(hex: "#F43F5E").opacity(0.9), in: Capsule())
                    }
                    .disabled(decodingUpload)

                    Button("Cancel", action: onDismiss)
                        .font(.plusJakarta(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 28)
                        .padding(.vertical, 12)
                        .background(Color.white.opacity(0.12), in: Capsule())
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
                .safeAreaPadding(.bottom)
            }
        }
        .background(Color(hex: "#131313").ignoresSafeArea())
        .task {
            let granted = await AVCaptureDevice.requestAccess(for: .video)
            cameraDenied = !granted
            cameraReady = granted
        }
        .onChange(of: photoItem) { _, item in
            guard let item else { return }
            Task { await decodeUploadedPhoto(item) }
        }
    }

    private var banner: String {
        if decodingUpload { return "Reading code from photo…" }
        if cameraDenied { return "Camera access is off — upload a barcode photo instead." }
        return subtitle
    }

    private func acceptRaw(_ raw: String) {
        let code = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !accepted, !code.isEmpty else { return }
        accepted = true
        onCode(code)
    }

    private func decodeUploadedPhoto(_ item: PhotosPickerItem) async {
        decodingUpload = true
        scanError = nil
        defer {
            decodingUpload = false
            photoItem = nil
        }
        guard let data = try? await item.loadTransferable(type: Data.self),
              let uiImage = UIImage(data: data),
              let cgImage = uiImage.cgImage else {
            scanError = "Could not read that photo."
            return
        }
        let request = VNDetectBarcodesRequest()
        request.symbologies = Self.retailSymbologies
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        do {
            try handler.perform([request])
            guard let raw = request.results?.compactMap({ $0.payloadStringValue }).first else {
                scanError = "No barcode found in that image."
                return
            }
            acceptRaw(raw)
        } catch {
            scanError = "No barcode found in that image."
        }
    }
}
