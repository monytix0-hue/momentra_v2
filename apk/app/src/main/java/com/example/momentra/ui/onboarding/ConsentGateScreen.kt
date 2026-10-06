package com.example.momentra.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.ShellTokens

/** Consent + age gate before login — analytics/AI purposes refined in Account hub. */
@Composable
fun ConsentGateScreen(
    onContinue: () -> Unit,
    prefs: AppPreferences,
) {
    val context = LocalContext.current
    var confirmedAge13Plus by remember { mutableStateOf(prefs.isAgeGateAccepted()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShellTokens.TopBarBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag(MaestroIds.CONSENT_GATE),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Privacy & consent", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Momentra uses your account and moment data to run the product. You can manage these purposes anytime in Account → Privacy:",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        PurposeBullet("Account & moments — operate your Personal, Group, and Business moments")
        PurposeBullet("Analytics — optional product analytics (Personal / Business)")
        PurposeBullet("AI insights — optional AI suggestions and recommendations")
        PurposeBullet("Memory patterns — optional pattern analysis on your memories")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "By continuing you agree to our Privacy Policy and Terms of Service.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
        )
        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://momentra.tech/privacy")),
                )
            }
        }) {
            Text("Privacy Policy", color = Color.White)
        }
        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://momentra.tech/terms")),
                )
            }
        }) {
            Text("Terms of Service", color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("consent.age_gate"),
        ) {
            Checkbox(
                checked = confirmedAge13Plus,
                onCheckedChange = { confirmedAge13Plus = it },
            )
            Text(
                "I confirm I am 13 years of age or older",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                prefs.setAgeGateAccepted(true)
                onContinue()
            },
            enabled = confirmedAge13Plus,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MaestroIds.CONSENT_CONTINUE),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun PurposeBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("•  ", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Text(text, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
    }
}
