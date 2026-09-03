import Combine
import FirebaseAuth
import FirebaseCore
import Foundation
import UIKit
#if os(iOS)
import AuthenticationServices
import CryptoKit
import GoogleSignIn
import Security
#endif

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var phase: AuthPhase = .launching
    @Published var identity: ShellIdentity?
    @Published var error: String?
    @Published var phoneCodeSent = false

    var isLoggedIn: Bool { phase == .authenticated && identity != nil }
    var isLoading: Bool { phase == .authenticating || phase == .authenticatedBootstrapping }
    var isRestoringSession: Bool { phase == .restoringSession || phase == .authenticatedBootstrapping }

    private let api = APIClient.shared
    private var phoneVerificationID: String?
#if os(iOS)
    private var appleSignInNonce: String?
#endif

    init() {
        Task { await restoreSessionIfPossible() }
    }

    func restoreSessionIfPossible() async {
        guard Auth.auth().currentUser != nil else {
            phase = .signedOut
            identity = nil
            return
        }
        phase = .restoringSession
        do {
            let me = try await api.bootstrapMe()
            let firebaseUid = Auth.auth().currentUser?.uid
            identity = ShellIdentity(
                userId: me.userId,
                displayName: me.displayName,
                email: me.email,
                firebaseUid: firebaseUid
            )
            if let firebaseUid {
                MomentraIdentityCache.save(
                    firebaseUid: firebaseUid,
                    userId: me.userId,
                    displayName: me.displayName,
                    email: me.email
                )
            }
            MomentraAnalytics.shared.syncUserDemographics(
                user: Auth.auth().currentUser,
                profileDisplayName: me.displayName,
                profileEmail: me.email
            )
            phase = .authenticated
        } catch let err as APIErrorKind {
            switch err {
            case .network:
                // Never substitute Firebase UID for Momentra userId.
                if let firebaseUid = Auth.auth().currentUser?.uid,
                   let cached = MomentraIdentityCache.load(firebaseUid: firebaseUid) {
                    identity = cached
                    phase = .authenticated
                    error = "NETWORK_UNAVAILABLE"
                } else if Auth.auth().currentUser != nil {
                    identity = nil
                    phase = .restoringSession
                    error = "NETWORK_UNAVAILABLE"
                } else {
                    failRestore()
                }
            case .unauthenticated:
                try? Auth.auth().signOut()
                #if os(iOS)
                GIDSignIn.sharedInstance.signOut()
                #endif
                phase = .sessionExpired
                identity = nil
            default:
                failRestore()
            }
        } catch {
            failRestore()
        }
    }

    private func failRestore() {
        try? Auth.auth().signOut()
        #if os(iOS)
        GIDSignIn.sharedInstance.signOut()
        #endif
        phase = .signedOut
        identity = nil
    }

    func signInWithEmailPassword(email: String, password: String) {
        phase = .authenticating
        error = nil
        Task {
            do {
                let result = try await Auth.auth().signIn(
                    withEmail: email.lowercased(),
                    password: password
                )
                try await finishBootstrap(user: result.user, method: "email_sign_in")
            } catch {
                recoverFromFailure(error, method: "email_sign_in")
            }
        }
    }

    func registerWithEmailPassword(email: String, password: String) {
        phase = .authenticating
        error = nil
        Task {
            do {
                let result = try await Auth.auth().createUser(
                    withEmail: email.lowercased(),
                    password: password
                )
                try await finishBootstrap(user: result.user, method: "email_register")
            } catch {
                recoverFromFailure(error, method: "email_register")
            }
        }
    }

    func sendPhoneCode(phone: String) {
        phase = .authenticating
        error = nil
        guard let e164 = Self.normalizedE164(phone) else {
            phase = .authError
            error = "Enter a phone number in E.164 format, e.g. +919876543210."
            return
        }
        Task {
            do {
                let verificationID = try await PhoneAuthProvider.provider().verifyPhoneNumber(e164, uiDelegate: nil)
                phoneVerificationID = verificationID
                phoneCodeSent = true
                phase = .signedOut
                MomentraAnalytics.shared.trackAuthResult(method: "phone_sms_sent", success: true)
            } catch {
                recoverFromFailure(error, method: "phone_sms_sent")
            }
        }
    }

    func confirmPhoneCode(code: String) {
        let trimmed = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let verificationID = phoneVerificationID, trimmed.count >= 6 else {
            error = "Enter the 6-digit code from SMS."
            return
        }
        phase = .authenticating
        error = nil
        Task {
            do {
                let credential = PhoneAuthProvider.provider().credential(
                    withVerificationID: verificationID,
                    verificationCode: trimmed
                )
                let result = try await Auth.auth().signIn(with: credential)
                phoneCodeSent = false
                phoneVerificationID = nil
                try await finishBootstrap(user: result.user, method: "phone_verify")
            } catch {
                recoverFromFailure(error, method: "phone_verify")
            }
        }
    }

    func resetPhoneFlow() {
        phoneCodeSent = false
        phoneVerificationID = nil
        error = nil
        phase = .signedOut
    }

    private static func normalizedE164(_ raw: String) -> String? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.hasPrefix("+") {
            let digits = trimmed.dropFirst().filter(\.isNumber)
            return digits.count >= 8 ? "+\(digits)" : nil
        }
        let digits = trimmed.filter(\.isNumber)
        guard digits.count >= 10 else { return nil }
        if digits.count == 10 { return "+91\(digits)" }
        return "+\(digits)"
    }

    #if os(iOS)
    func prepareAppleSignInRequest(_ request: ASAuthorizationAppleIDRequest) {
        let nonce = Self.randomNonceString()
        appleSignInNonce = nonce
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
        phase = .authenticating
        error = nil
    }

    func handleAppleSignInCompletion(_ result: Result<ASAuthorization, Error>) {
        switch result {
        case .failure(let error):
            if let authError = error as? ASAuthorizationError, authError.code == .canceled {
                phase = .signedOut
                self.error = nil
                appleSignInNonce = nil
                return
            }
            appleSignInNonce = nil
            recoverFromFailure(error, method: "apple")
        case .success(let authorization):
            guard
                let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
                let nonce = appleSignInNonce,
                let tokenData = appleIDCredential.identityToken,
                let idToken = String(data: tokenData, encoding: .utf8)
            else {
                appleSignInNonce = nil
                recoverFromFailure(
                    NSError(
                        domain: "MomentraAuth",
                        code: -4,
                        userInfo: [NSLocalizedDescriptionKey: "Apple Sign In returned an incomplete credential."]
                    ),
                    method: "apple"
                )
                return
            }
            appleSignInNonce = nil
            Task {
                do {
                    let credential = OAuthProvider.appleCredential(
                        withIDToken: idToken,
                        rawNonce: nonce,
                        fullName: appleIDCredential.fullName
                    )
                    let authResult = try await Auth.auth().signIn(with: credential)
                    if let fullName = appleIDCredential.fullName {
                        let display = PersonNameComponentsFormatter().string(from: fullName)
                            .trimmingCharacters(in: .whitespacesAndNewlines)
                        if !display.isEmpty, authResult.user.displayName == nil {
                            let change = authResult.user.createProfileChangeRequest()
                            change.displayName = display
                            try? await change.commitChanges()
                        }
                    }
                    try await finishBootstrap(user: authResult.user, method: "apple")
                } catch {
                    recoverFromFailure(error, method: "apple")
                }
            }
        }
    }

    func signInWithGoogle() {
        phase = .authenticating
        error = nil
        Task {
            do {
                guard let clientID = FirebaseApp.app()?.options.clientID else {
                    throw NSError(
                        domain: "MomentraAuth",
                        code: -1,
                        userInfo: [NSLocalizedDescriptionKey: "Missing Firebase client ID"]
                    )
                }
                GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
                guard let presenter = UIApplication.topViewController() else {
                    throw NSError(
                        domain: "MomentraAuth",
                        code: -2,
                        userInfo: [NSLocalizedDescriptionKey: "Could not present Google sign-in"]
                    )
                }
                let signInResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
                guard let idToken = signInResult.user.idToken?.tokenString else {
                    throw NSError(
                        domain: "MomentraAuth",
                        code: -3,
                        userInfo: [NSLocalizedDescriptionKey: "Missing Google ID token"]
                    )
                }
                let credential = GoogleAuthProvider.credential(
                    withIDToken: idToken,
                    accessToken: signInResult.user.accessToken.tokenString
                )
                let authResult = try await Auth.auth().signIn(with: credential)
                try await finishBootstrap(user: authResult.user, method: "google")
            } catch {
                recoverFromFailure(error, method: "google")
            }
        }
    }

    private static func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remaining = length
        while remaining > 0 {
            let randoms: [UInt8] = (0 ..< 16).map { _ in
                var random: UInt8 = 0
                let status = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                if status != errSecSuccess {
                    fatalError("Unable to generate nonce. SecRandomCopyBytes failed with OSStatus \(status)")
                }
                return random
            }
            for random in randoms where remaining > 0 {
                if random < charset.count {
                    result.append(charset[Int(random)])
                    remaining -= 1
                }
            }
        }
        return result
    }

    private static func sha256(_ input: String) -> String {
        let data = Data(input.utf8)
        let hash = SHA256.hash(data: data)
        return hash.map { String(format: "%02x", $0) }.joined()
    }
    #endif

    private func finishBootstrap(user: User, method: String) async throws {
        phase = .authenticatedBootstrapping
        let me = try await api.bootstrapMe()
        identity = ShellIdentity(
            userId: me.userId,
            displayName: me.displayName ?? user.displayName,
            email: me.email ?? user.email,
            firebaseUid: user.uid
        )
        MomentraIdentityCache.save(
            firebaseUid: user.uid,
            userId: me.userId,
            displayName: me.displayName ?? user.displayName,
            email: me.email ?? user.email
        )
        MomentraAnalytics.shared.syncUserDemographics(
            user: Auth.auth().currentUser,
            profileDisplayName: me.displayName ?? user.displayName,
            profileEmail: me.email ?? user.email
        )
        MomentraAnalytics.shared.trackAuthResult(method: method, success: true)
        phase = .authenticated
        error = nil
        APIClient.shared.warmAuthToken()
        PushNotifications.requestPermissionAndRegister()
        Task { await PushNotifications.syncDeviceWithBackend() }
    }

    private func recoverFromFailure(_ error: Error, method: String) {
        phase = .authError
        self.error = Self.userFacingAuthError(error)
        MomentraAnalytics.shared.trackAuthResult(
            method: method,
            success: false,
            errorCode: error.localizedDescription
        )
    }

    private static func userFacingAuthError(_ error: Error) -> String {
        if let kind = error as? APIErrorKind {
            switch kind {
            case .network(let message):
                return message
            case .unauthenticated:
                return "Session expired. Sign in again."
            case .server:
                return "The API is temporarily unavailable. Try again."
            default:
                return String(describing: kind)
            }
        }
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            return """
            Cannot reach the API at \(APIConfig.baseURLDescription). \
            Set Info.plist MomentraAPIBaseURL to your computer’s LAN IP \
            (same Wi‑Fi), matching Android API_BASE_URL. Backend must be running.
            """
        }
        // Firebase / Apple often localize this as “Could not connect to the server.”
        let text = error.localizedDescription
        if text.localizedCaseInsensitiveContains("could not connect")
            || text.localizedCaseInsensitiveContains("can't connect")
            || text.localizedCaseInsensitiveContains("network error") {
            return """
            Network error during sign-in. If Firebase succeeded but bootstrap failed, \
            check MomentraAPIBaseURL (\(APIConfig.baseURLDescription)) and that the \
            backend is listening on 0.0.0.0:3000.
            """
        }
        return text
    }

    func sendPasswordReset(email: String) {
        let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            error = "Enter your email to reset password."
            return
        }
        phase = .authenticating
        error = nil
        Auth.auth().sendPasswordReset(withEmail: trimmed) { err in
            Task { @MainActor in
                if let err {
                    self.phase = .authError
                    self.error = err.localizedDescription
                } else {
                    self.phase = .signedOut
                    self.error = "Password reset email sent. Check your inbox."
                }
            }
        }
    }

    func signOut() {
        MomentraIdentityCache.clear(firebaseUid: Auth.auth().currentUser?.uid)
        BootstrapCacheStore.clear(userId: identity?.userId)
        UserDefaults.standard.removeObject(forKey: "momentra_hide_balances")
        try? Auth.auth().signOut()
        #if os(iOS)
        GIDSignIn.sharedInstance.signOut()
        #endif
        identity = nil
        phase = .signedOut
    }

    func onSessionExpired() {
        MomentraIdentityCache.clear(firebaseUid: Auth.auth().currentUser?.uid)
        BootstrapCacheStore.clear(userId: identity?.userId)
        try? Auth.auth().signOut()
        #if os(iOS)
        GIDSignIn.sharedInstance.signOut()
        #endif
        identity = nil
        phase = .sessionExpired
        error = "Session expired"
    }
}

#if os(iOS)
extension UIApplication {
    static func topViewController(
        base: UIViewController? = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first?.rootViewController
    ) -> UIViewController? {
        if let nav = base as? UINavigationController {
            return topViewController(base: nav.visibleViewController)
        }
        if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return topViewController(base: selected)
        }
        if let presented = base?.presentedViewController {
            return topViewController(base: presented)
        }
        return base
    }
}
#endif
