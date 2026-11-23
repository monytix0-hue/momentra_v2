import AuthenticationServices
import SwiftUI
import UIKit

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
        Form {
            Section {
                MomentraWordmark()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            }

            Section {
                Picker("", selection: $mode) {
                    ForEach(AuthMode.allCases, id: \.self) { m in
                        Text(m.rawValue).tag(m)
                    }
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .accessibilityIdentifier("login_mode_picker")
            }

            if mode == .phone {
                phoneFormSection
            } else {
                emailFormSection
            }

            #if os(iOS)
            Section {
                SignInWithAppleButton(.continue) { request in
                    trackWidget(screenName: loginScreen, widgetName: AnalyticsWidgets.loginBtnApple)
                    viewModel.prepareAppleSignInRequest(request)
                } onCompletion: { result in
                    viewModel.handleAppleSignInCompletion(result)
                }
                .signInWithAppleButtonStyle(.white)
                .frame(height: BrandSpacing.appleButtonHeight)
                .clipShape(Capsule())
                .disabled(viewModel.isLoading)
                .listRowBackground(Color.clear)
                .accessibilityIdentifier("login.apple")

                Button {
                    trackWidget(screenName: loginScreen, widgetName: AnalyticsWidgets.loginBtnGoogle)
                    viewModel.signInWithGoogle()
                } label: {
                    Label("Continue with Google", systemImage: "g.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(BrandSecondaryButtonStyle())
                .disabled(viewModel.isLoading)
                .listRowBackground(Color.clear)
                .accessibilityIdentifier("login.google")
            }
            #endif

            if let error = viewModel.error {
                Section {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(MomentraBrandTokens.ember300)
                        .multilineTextAlignment(.center)
                        .listRowBackground(Color.clear)
                        .accessibilityIdentifier("login.error")
                }
            }

            Section {
                HStack {
                    Button("Privacy Policy") {
                        if let url = URL(string: "https://momentra.tech/privacy") {
                            UIApplication.shared.open(url)
                        }
                    }
                    .accessibilityHint("Opens in Safari")
                    Button("Terms") {
                        if let url = URL(string: "https://momentra.tech/terms") {
                            UIApplication.shared.open(url)
                        }
                    }
                    .accessibilityHint("Opens in Safari")
                }
                .font(.system(size: 12))
                .listRowBackground(Color.clear)
            }
        }
        .scrollContentBackground(.hidden)
        // No `.id(loginScreen)` here. Changing the id rebuilt the whole form and reset every
        // `@State`, so switching Sign in → Register → Sign in silently discarded the credentials
        // the person had already typed. Screen attribution lives in `.trackScreen` and the mode
        // change below, neither of which needs the view to be recreated.
        .trackScreen(loginScreen)
        .brandAuthScreen()
        .accessibilityIdentifier("login.screen")
        .onChange(of: mode) { _, newMode in
            if newMode != .phone { viewModel.resetPhoneFlow() }
            smsCode = ""
            password = ""
            confirmPassword = ""
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

    @ViewBuilder
    private var emailFormSection: some View {
        Section {
            TextField("you@example.com", text: $email)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                .autocorrectionDisabled()
                .brandTextField()
                .accessibilityIdentifier("login.email")

            SecureField("Password", text: $password)
                .textContentType(mode == .signIn ? .password : .newPassword)
                .brandTextField()
                .accessibilityIdentifier("login.password")

            if mode == .register {
                SecureField("Confirm password", text: $confirmPassword)
                    .textContentType(.newPassword)
                    .brandTextField()
            }
        } header: {
            Text(mode == .signIn ? "Sign in with email" : "Create account")
        }

        Section {
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
            .listRowBackground(Color.clear)
            .accessibilityIdentifier("login.submit")

            if mode == .signIn {
                Button("Forgot password?") {
                    viewModel.sendPasswordReset(email: email)
                }
                .font(.system(size: 13))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.75))
                .frame(maxWidth: .infinity)
                .disabled(viewModel.isLoading)
                .listRowBackground(Color.clear)
                .accessibilityIdentifier("login.forgot")
            }
        }
    }

    @ViewBuilder
    private var phoneFormSection: some View {
        Section {
            TextField("+919876543210", text: $phone)
                .keyboardType(.phonePad)
                .textContentType(.telephoneNumber)
                .brandTextField()
                .disabled(viewModel.phoneCodeSent)

            if viewModel.phoneCodeSent {
                TextField("6-digit code", text: $smsCode)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .brandTextField()
            }
        } header: {
            Text("Phone sign in")
        }

        Section {
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
            .listRowBackground(Color.clear)

            if viewModel.phoneCodeSent {
                Button("Use a different number") {
                    trackWidget(screenName: AnalyticsScreens.loginPhone, widgetName: AnalyticsWidgets.loginBtnPhoneChange)
                    smsCode = ""
                    viewModel.resetPhoneFlow()
                }
                .font(.system(size: 13))
                .foregroundStyle(MomentraBrandTokens.textOnDark.opacity(0.75))
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
        }
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
}
