import AuthenticationServices
import SwiftUI

struct LoginView: View {
    @ObservedObject var viewModel: AuthViewModel
    var onLoggedIn: () -> Void

    @State private var mode: AuthMode = .signIn
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var phone = ""
    @State private var smsCode = ""

    private enum AuthMode: String, CaseIterable {
        case signIn = "Sign in"
        case register = "Register"
        case phone = "Phone"
    }

    private var loginScreen: String {
        switch mode {
        case .signIn: AnalyticsScreens.loginSignIn
        case .register: AnalyticsScreens.loginRegister
        case .phone: AnalyticsScreens.loginPhone
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                MomentraWordmark()
                    .padding(.bottom, BrandSpacing.xl - 4)

                Picker("", selection: $mode) {
                    ForEach(AuthMode.allCases, id: \.self) { m in
                        Text(m.rawValue).tag(m)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, BrandSpacing.screenHorizontal)
                .accessibilityIdentifier("login_mode_picker")

                if mode == .phone {
                    phoneFields
                } else {
                    emailFields
                }

                orDivider.padding(.vertical, 22)

                #if os(iOS)
                VStack(spacing: 12) {
                    SignInWithAppleButton(.continue) { request in
                        trackWidget(screenName: AnalyticsScreens.login, widgetName: AnalyticsWidgets.loginBtnApple)
                        viewModel.prepareAppleSignInRequest(request)
                    } onCompletion: { result in
                        viewModel.handleAppleSignInCompletion(result)
                    }
                    .signInWithAppleButtonStyle(.white)
                    .frame(height: BrandSpacing.appleButtonHeight)
                    .clipShape(Capsule())
                    .disabled(viewModel.isLoading)
                    .accessibilityIdentifier("login.apple")

                    Button {
                        trackWidget(screenName: AnalyticsScreens.login, widgetName: AnalyticsWidgets.loginBtnGoogle)
                        viewModel.signInWithGoogle()
                    } label: {
                        Label("Continue with Google", systemImage: "g.circle.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(BrandSecondaryButtonStyle())
                    .disabled(viewModel.isLoading)
                    .accessibilityIdentifier("login.google")
                }
                .padding(.horizontal, BrandSpacing.screenHorizontal)
                #endif

                if let error = viewModel.error {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(MomentraBrandTokens.ember300)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, BrandSpacing.screenHorizontal)
                        .padding(.top, 16)
                        .accessibilityIdentifier("login.error")
                }
            }
            .padding(.vertical, 24)
        }
        .id(loginScreen)
        .trackScreen(loginScreen)
        .brandAuthScreen()
        .accessibilityIdentifier("login.screen")
        .onChange(of: mode) { _, newMode in
            if newMode != .phone { viewModel.resetPhoneFlow() }
            smsCode = ""
            let widget: String
            switch newMode {
            case .signIn: widget = AnalyticsWidgets.loginTabSignIn
            case .register: widget = AnalyticsWidgets.loginTabRegister
            case .phone: widget = AnalyticsWidgets.loginTabPhone
            }
            trackWidget(screenName: AnalyticsScreens.login, widgetName: widget)
        }
        .onChange(of: viewModel.isLoggedIn) { _, loggedIn in
            if loggedIn { onLoggedIn() }
        }
    }

    private var emailFields: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 14) {
                labeledField(title: "Email") {
                    TextField("you@example.com", text: $email)
                        .textContentType(.username)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                        .brandTextField()
                        .accessibilityIdentifier("login.email")
                }

                labeledField(title: "Password") {
                    SecureField("Password", text: $password)
                        .textContentType(mode == .signIn ? .password : .newPassword)
                        .brandTextField()
                        .accessibilityIdentifier("login.password")
                }

                if mode == .register {
                    labeledField(title: "Confirm password") {
                        SecureField("Confirm password", text: $confirmPassword)
                            .textContentType(.newPassword)
                            .brandTextField()
                    }
                }
            }
            .padding(.horizontal, BrandSpacing.screenHorizontal)
            .padding(.top, 22)

            Button {
                trackWidget(screenName: loginScreen, widgetName: AnalyticsWidgets.loginBtnEmailSubmit)
                submitEmailAuth()
            } label: {
                Group {
                    if viewModel.isLoading {
                        ProgressView()
                            .tint(MomentraBrandTokens.textOnEmber)
                    } else {
                        Text(mode == .signIn ? "Sign in" : "Create account")
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(BrandPrimaryButtonStyle())
            .disabled(viewModel.isLoading)
            .accessibilityIdentifier("login.submit")
            .padding(.horizontal, BrandSpacing.screenHorizontal)
            .padding(.top, 20)

            if mode == .signIn {
                Button("Forgot password?") {
                    viewModel.sendPasswordReset(email: email)
                }
                .font(.system(size: 13))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.75))
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
                .disabled(viewModel.isLoading)
                .accessibilityIdentifier("login.forgot")
            }
        }
    }

    private var phoneFields: some View {
        VStack(alignment: .leading, spacing: 14) {
            labeledField(title: "Phone") {
                TextField("+919876543210", text: $phone)
                    .keyboardType(.phonePad)
                    .textContentType(.telephoneNumber)
                    .brandTextField()
                    .disabled(viewModel.phoneCodeSent)
            }

            if viewModel.phoneCodeSent {
                labeledField(title: "SMS code") {
                    TextField("6-digit code", text: $smsCode)
                        .keyboardType(.numberPad)
                        .textContentType(.oneTimeCode)
                        .brandTextField()
                }
            }

            Button {
                trackWidget(
                    screenName: AnalyticsScreens.loginPhone,
                    widgetName: viewModel.phoneCodeSent
                        ? AnalyticsWidgets.loginBtnPhoneVerify
                        : AnalyticsWidgets.loginBtnPhoneSend
                )
                submitPhoneAuth()
            } label: {
                Group {
                    if viewModel.isLoading {
                        ProgressView()
                            .tint(MomentraBrandTokens.textOnEmber)
                    } else {
                        Text(viewModel.phoneCodeSent ? "Verify code" : "Send SMS code")
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(BrandPrimaryButtonStyle())
            .disabled(viewModel.isLoading)

            if viewModel.phoneCodeSent {
                Button("Use a different number") {
                    trackWidget(screenName: AnalyticsScreens.loginPhone, widgetName: AnalyticsWidgets.loginBtnPhoneChange)
                    smsCode = ""
                    viewModel.resetPhoneFlow()
                }
                .font(.system(size: 13))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.75))
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, BrandSpacing.screenHorizontal)
        .padding(.top, 22)
    }

    private func submitPhoneAuth() {
        if viewModel.phoneCodeSent {
            viewModel.confirmPhoneCode(code: smsCode)
        } else {
            viewModel.sendPhoneCode(phone: phone)
        }
    }

    private func submitEmailAuth() {
        let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            viewModel.error = "Enter your email."
            return
        }
        guard password.count >= 6 else {
            viewModel.error = "Password must be at least 6 characters."
            return
        }
        if mode == .register {
            guard password == confirmPassword else {
                viewModel.error = "Passwords do not match."
                return
            }
            viewModel.registerWithEmailPassword(email: trimmed, password: password)
        } else {
            viewModel.signInWithEmailPassword(email: trimmed, password: password)
        }
    }

    @ViewBuilder
    private func labeledField<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.7))
            content()
        }
    }

    private var orDivider: some View {
        ZStack {
            Rectangle()
                .fill(Color.white.opacity(0.15))
                .frame(height: 1)
                .padding(.horizontal, 24)
            Text("or")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.55))
                .padding(.horizontal, 8)
                .background(MomentraBrandTokens.brand)
        }
        .padding(.horizontal, BrandSpacing.screenHorizontal)
    }
}
