import AVFoundation
import SwiftUI
import Vision

struct GroupJoinQrScanner: View {
    var onCode: (String) -> Void
    var onCompanyCode: ((String) -> Void)? = nil
    var onDismiss: () -> Void

    @State private var accepted = false
    @State private var cameraDenied = false
    @State private var cameraReady = false

    var body: some View {
        ZStack {
            if cameraReady, !cameraDenied {
                ScannerCameraView { raw in
                    guard !accepted else { return }
                    if let company = CompanyJoinLink.parse(raw), let onCompanyCode {
                        accepted = true
                        onCompanyCode(company)
                        return
                    }
                    guard let code = GroupJoinLink.parse(raw) else { return }
                    accepted = true
                    onCode(code)
                }
                .ignoresSafeArea()
            }

            VStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Scan to join")
                        .font(.plusJakarta(size: 20, weight: .bold))
                        .foregroundStyle(.white)
                    Text(cameraDenied
                         ? "Camera access is needed to scan an invite QR."
                         : "Point the camera at a Momentra invite QR.")
                        .font(.plusJakarta(size: 14))
                        .foregroundStyle(.white.opacity(0.7))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                Spacer()
                RoundedRectangle(cornerRadius: 24)
                    .stroke(Color(hex: "#FF7A3D"), lineWidth: 2)
                    .frame(width: 240, height: 240)
                Spacer()
                Button("Cancel", action: onDismiss)
                    .font(.plusJakarta(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.12), in: Capsule())
                    .padding(.bottom, 48)
            }
        }
        .background(Color(hex: "#131313").ignoresSafeArea())
        .task {
            let granted = await AVCaptureDevice.requestAccess(for: .video)
            cameraDenied = !granted
            cameraReady = granted
        }
    }
}

private struct ScannerCameraView: UIViewControllerRepresentable {
    var onRaw: (String) -> Void

    func makeUIViewController(context: Context) -> ScannerCameraController {
        let controller = ScannerCameraController()
        controller.onRaw = onRaw
        return controller
    }

    func updateUIViewController(_ uiViewController: ScannerCameraController, context: Context) {
        uiViewController.onRaw = onRaw
    }
}

final class ScannerCameraController: UIViewController, AVCaptureVideoDataOutputSampleBufferDelegate {
    var onRaw: ((String) -> Void)?
    private let session = AVCaptureSession()
    private let output = AVCaptureVideoDataOutput()
    private var preview: AVCaptureVideoPreviewLayer?
    private var handling = false
    private let request = VNDetectBarcodesRequest()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        request.symbologies = [.qr]
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else { return }
        session.addInput(input)
        output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "momentra.qr"))
        if session.canAddOutput(output) {
            session.addOutput(output)
        }
        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        layer.frame = view.bounds
        view.layer.addSublayer(layer)
        preview = layer
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.session.startRunning()
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        preview?.frame = view.bounds
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        session.stopRunning()
    }

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard !handling, let pixel = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let handler = VNImageRequestHandler(cvPixelBuffer: pixel, orientation: .right, options: [:])
        try? handler.perform([request])
        guard let payload = request.results?.compactMap({ $0.payloadStringValue }).first else { return }
        handling = true
        DispatchQueue.main.async { [weak self] in
            self?.onRaw?(payload)
        }
    }
}
