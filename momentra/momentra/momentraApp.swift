import FirebaseAuth
import FirebaseCore
import FirebaseMessaging
import SwiftUI
import UIKit
import UserNotifications
#if os(iOS)
import GoogleSignIn
#endif

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        SentryBootstrap.initIfConfigured()
        FirebaseApp.configure()
        _ = MomentraAnalytics.shared
        PushNotifications.configure(delegate: self)
        PushNotifications.requestPermissionAndRegister()
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        MomentraAnalytics.shared.onAppForeground()
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        MomentraAnalytics.shared.onAppBackground()
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        PushNotifications.handleApnsToken(deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        NSLog("APNs registration failed: \(error.localizedDescription)")
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.newData)
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        if Auth.auth().canHandle(url) {
            return true
        }
        #if os(iOS)
        return GIDSignIn.sharedInstance.handle(url)
        #else
        return false
        #endif
    }

    // MARK: - UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        PushDeepLinkStore.shared.offer(userInfo: response.notification.request.content.userInfo)
        completionHandler()
    }

    // MARK: - MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken, !fcmToken.isEmpty else { return }
        Task { await PushNotifications.syncDeviceWithBackend(explicitToken: fcmToken) }
    }
}

@main
struct momentraApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var authViewModel = AuthViewModel()
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                AuthOnlyView(viewModel: authViewModel)
                    .preferredColorScheme(.dark)
                    #if os(iOS)
                    .onOpenURL { url in
                        if Auth.auth().canHandle(url) { return }
                        if GIDSignIn.sharedInstance.handle(url) { return }
                        if let code = GroupJoinLink.parse(url) {
                            JoinInviteStore.shared.offer(code)
                        }
                    }
                    .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                        guard let url = activity.webpageURL else { return }
                        if let code = GroupJoinLink.parse(url) {
                            JoinInviteStore.shared.offer(code)
                        }
                    }
                    #endif

                if showSplash {
                    LaunchScreenView(onFinish: {
                        trackWidget(screenName: AnalyticsScreens.splash, widgetName: AnalyticsWidgets.splashComplete, action: "auto")
                        showSplash = false
                    })
                    .trackScreen(AnalyticsScreens.splash)
                    .transition(.opacity)
                    .zIndex(1)
                }
            }
            .animation(.easeOut(duration: 0.3), value: showSplash)
        }
    }
}

struct AuthOnlyView: View {
    @ObservedObject var viewModel: AuthViewModel
    @StateObject private var shellModel = AppShellModel()
    @State private var onboardingSeen = OnboardingPrefs.isSeen
    @State private var consentAck = OnboardingPrefs.isConsentGateSeen
    @State private var lockTick = 0
    @Environment(\.scenePhase) private var scenePhase

    private var needsLock: Bool {
        viewModel.isLoggedIn &&
            AppLockStore.isPinEnabled &&
            !AppLockSession.unlocked
    }

    var body: some View {
        Group {
            if viewModel.isLoggedIn, viewModel.identity != nil, needsLock {
                let _ = lockTick
                AppLockGateView(onUnlocked: { lockTick += 1 })
            } else if viewModel.isLoggedIn, let identity = viewModel.identity {
                AppShellView(
                    identity: identity,
                    model: shellModel,
                    onSignOut: {
                        shellModel.clearForLogout()
                        AppLockSession.unlocked = false
                        viewModel.signOut()
                    },
                    onSessionExpired: {
                        shellModel.clearForLogout()
                        viewModel.onSessionExpired()
                    }
                )
                .trackScreen(AnalyticsScreens.home)
            } else if viewModel.isRestoringSession {
                VStack {
                    ProgressView("Restoring session…")
                        .tint(MomentraBrandTokens.cta)
                }
                .brandAuthScreen()
                .trackScreen(AnalyticsScreens.sessionRestore)
            } else if !onboardingSeen {
                OnboardingView(mode: .firstRun) {
                    onboardingSeen = true
                }
            } else if !consentAck {
                ConsentGateView {
                    OnboardingPrefs.markConsentGateSeen()
                    consentAck = true
                }
            } else {
                LoginView(viewModel: viewModel, onLoggedIn: {})
            }
        }
        .animation(.easeInOut(duration: 0.25), value: viewModel.isLoggedIn)
        .animation(.easeInOut(duration: 0.25), value: viewModel.isRestoringSession)
        .animation(.easeInOut(duration: 0.25), value: onboardingSeen)
        .animation(.easeInOut(duration: 0.25), value: consentAck)
        .onChange(of: viewModel.isLoggedIn) { _, loggedIn in
            if loggedIn {
                PushNotifications.requestPermissionAndRegister()
                Task { await PushNotifications.syncDeviceWithBackend() }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .background:
                AppLockSession.lastBackground = Date()
            case .active:
                if AppLockStore.isPinEnabled {
                    let timeout = TimeInterval(AppLockStore.autoLockSeconds)
                    let elapsed = Date().timeIntervalSince(AppLockSession.lastBackground ?? Date.distantPast)
                    if AppLockSession.lastBackground != nil && elapsed >= timeout {
                        AppLockSession.unlocked = false
                        lockTick += 1
                    }
                }
            default:
                break
            }
        }
    }
}
