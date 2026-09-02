package com.example.momentra.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.AnalyticsWidgets
import com.example.momentra.analytics.TrackScreen
import com.example.momentra.analytics.trackWidget
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.splash.MomentraWordmark
import com.example.momentra.ui.theme.MomentraBrandColors

private enum class AuthMode { SignIn, Register, Phone }

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoggedIn: () -> Unit,
) {
    val state by authViewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    val handleGoogleSignIn: () -> Unit = signIn@{
        if (activity == null) {
            authViewModel.onAuthError("Could not start Google sign-in")
            return@signIn
        }
        trackWidget(AnalyticsScreens.LOGIN, AnalyticsWidgets.LOGIN_BTN_GOOGLE, "tap")
        authViewModel.signInWithGoogle(activity)
    }

    fun submitEmailAuth() {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            authViewModel.onAuthError("Enter your email.")
            return
        }
        if (password.length < 6) {
            authViewModel.onAuthError("Password must be at least 6 characters.")
            return
        }
        if (mode == AuthMode.Register) {
            authViewModel.registerWithEmail(trimmed, password, confirmPassword)
        } else {
            authViewModel.signInWithEmail(trimmed, password)
        }
    }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoggedIn()
    }

    val loginScreen = when (mode) {
        AuthMode.SignIn -> AnalyticsScreens.LOGIN_SIGN_IN
        AuthMode.Register -> AnalyticsScreens.LOGIN_REGISTER
        AuthMode.Phone -> AnalyticsScreens.LOGIN_PHONE
    }
    TrackScreen(loginScreen)

    BrandAuthScreen {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MomentraWordmark()

            Spacer(modifier = Modifier.height(28.dp))

            TabRow(
                selectedTabIndex = mode.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(4.dp),
                containerColor = Color.Transparent,
                indicator = {},
                divider = {},
            ) {
                AuthMode.entries.forEach { option ->
                    val selected = mode == option
                    val label = when (option) {
                        AuthMode.SignIn -> "Sign in"
                        AuthMode.Register -> "Register"
                        AuthMode.Phone -> "Phone"
                    }
                    Tab(
                        selected = selected,
                        onClick = {
                            mode = option
                            authViewModel.clearError()
                            val widget = when (option) {
                                AuthMode.SignIn -> AnalyticsWidgets.LOGIN_TAB_SIGN_IN
                                AuthMode.Register -> AnalyticsWidgets.LOGIN_TAB_REGISTER
                                AuthMode.Phone -> AnalyticsWidgets.LOGIN_TAB_PHONE
                            }
                            trackWidget(AnalyticsScreens.LOGIN, widget, "tap")
                            if (option != AuthMode.Phone) {
                                authViewModel.resetPhoneFlow()
                                smsCode = ""
                            }
                            if (option == AuthMode.SignIn) confirmPassword = ""
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                color = if (selected) {
                                    MomentraBrandColors.Indigo500
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(100.dp),
                            ),
                        text = {
                            Text(
                                text = label,
                                color = MomentraBrandColors.TextOnDark,
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (mode == AuthMode.Phone) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column {
                        BrandFieldLabel("Phone")
                        BrandTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = "+919876543210",
                        )
                    }
                    if (state.phoneCodeSent) {
                        Column {
                            BrandFieldLabel("SMS code")
                            BrandTextField(
                                value = smsCode,
                                onValueChange = { smsCode = it },
                                placeholder = "6-digit code",
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (state.isLoading) {
                    CircularProgressIndicator(color = MomentraBrandColors.Cta)
                } else {
                    BrandPrimaryButton(
                        text = if (state.phoneCodeSent) "Verify code" else "Send SMS code",
                        onClick = {
                            trackWidget(
                                AnalyticsScreens.LOGIN_PHONE,
                                if (state.phoneCodeSent) {
                                    AnalyticsWidgets.LOGIN_BTN_PHONE_VERIFY
                                } else {
                                    AnalyticsWidgets.LOGIN_BTN_PHONE_SEND
                                },
                            )
                            if (activity == null) {
                                authViewModel.onAuthError("Could not start phone sign-in")
                            } else if (state.phoneCodeSent) {
                                authViewModel.confirmPhoneCode(smsCode)
                            } else {
                                authViewModel.sendPhoneCode(activity, phone)
                            }
                        },
                    )
                    if (state.phoneCodeSent) {
                        Spacer(modifier = Modifier.height(12.dp))
                        BrandSecondaryButton(
                            text = "Use a different number",
                            onClick = {
                                trackWidget(AnalyticsScreens.LOGIN_PHONE, AnalyticsWidgets.LOGIN_BTN_PHONE_CHANGE, "tap")
                                smsCode = ""
                                authViewModel.resetPhoneFlow()
                            },
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column {
                        BrandFieldLabel("Email")
                        BrandTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "you@example.com",
                            modifier = Modifier.testTag(MaestroIds.LOGIN_EMAIL),
                        )
                    }

                    Column {
                        BrandFieldLabel("Password")
                        BrandTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.testTag(MaestroIds.LOGIN_PASSWORD),
                        )
                    }

                    if (mode == AuthMode.SignIn) {
                        Text(
                            text = "Forgot password?",
                            color = MomentraBrandColors.Cta,
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag(MaestroIds.LOGIN_FORGOT)
                                .clickable {
                                    authViewModel.sendPasswordReset(email)
                                }
                                .padding(vertical = 4.dp),
                        )
                    }

                    if (mode == AuthMode.Register) {
                        Column {
                            BrandFieldLabel("Confirm password")
                            BrandTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = "Confirm password",
                                visualTransformation = PasswordVisualTransformation(),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (state.isLoading) {
                    CircularProgressIndicator(color = MomentraBrandColors.Cta)
                } else {
                    BrandPrimaryButton(
                        text = if (mode == AuthMode.SignIn) "Sign in" else "Create account",
                        onClick = {
                            trackWidget(AnalyticsScreens.LOGIN, AnalyticsWidgets.LOGIN_BTN_EMAIL_SUBMIT, "tap")
                            submitEmailAuth()
                        },
                        modifier = Modifier.testTag(MaestroIds.LOGIN_SUBMIT),
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            BrandOrDivider()
            Spacer(modifier = Modifier.height(22.dp))

            if (!state.isLoading) {
                BrandSecondaryButton(
                    text = "Continue with Google",
                    onClick = handleGoogleSignIn,
                    modifier = Modifier.testTag(MaestroIds.LOGIN_GOOGLE),
                )
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MomentraBrandColors.Ember300,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(MaestroIds.LOGIN_ERROR),
                )
            }
        }
    }
}
