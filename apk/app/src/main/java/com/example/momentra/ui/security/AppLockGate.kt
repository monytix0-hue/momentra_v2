package com.example.momentra.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.momentra.data.security.AppLockSession
import com.example.momentra.data.security.AppLockStore
import com.example.momentra.ui.theme.ShellTokens

@Composable
fun AppLockGate(
    lockStore: AppLockStore,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun unlock() {
        AppLockSession.markUnlocked()
        onUnlocked()
    }

    fun launchBiometric() {
        val act = activity ?: return
        val manager = BiometricManager.from(context)
        val can = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            error = "Biometrics unavailable"
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            act,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        error = errString.toString()
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Momentra")
            .setSubtitle("Confirm it's you")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(lockStore.biometricsEnabled()) {
        if (lockStore.biometricsEnabled()) {
            launchBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShellTokens.TopBarBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("App Locked", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
        Text("Enter your local PIN to continue.", color = Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = Color(0xFFFF8A80)) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (lockStore.verifyPin(pin)) {
                    unlock()
                } else {
                    error = "Incorrect PIN"
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Unlock") }
        if (lockStore.biometricsEnabled()) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { launchBiometric() }) {
                Text("Use biometrics")
            }
        }
    }
}
