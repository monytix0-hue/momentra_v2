package com.example.momentra.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.ShellTokens

/** Minimal consent gate (FIGMA_GAP) before login — analytics/AI purposes refined in Account hub. */
@Composable
fun ConsentGateScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ShellTokens.TopBarBackground)
            .padding(24.dp)
            .testTag(MaestroIds.CONSENT_GATE),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Privacy & consent", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Momentra uses account data to run your moments. You can grant or withdraw analytics and AI consents anytime in Account → Privacy.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(MaestroIds.CONSENT_CONTINUE),
        ) {
            Text("Continue")
        }
    }
}
