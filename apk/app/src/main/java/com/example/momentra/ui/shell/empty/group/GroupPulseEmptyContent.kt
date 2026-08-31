package com.example.momentra.ui.shell.empty.group

/** Figma Group Pulse empty (`575:8552` cluster) — S3-B: educational UI only; real emptiness from bootstrap inventory. */
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma 575:8967 — Group / Pulse empty */
@Composable
fun GroupPulseEmptyContent(
    onCreateMoment: () -> Unit,
    onJoinCode: (String) -> Unit = {},
    onSelectExperience: () -> Unit = onCreateMoment,
    onSelectPurchase: () -> Unit = onCreateMoment,
    onSelectLiving: () -> Unit = onCreateMoment,
    modifier: Modifier = Modifier,
) {
    var showScanner by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GeBg)
                .verticalScroll(rememberScrollState()),
        ) {
            GeAppear {
                GeFigmaHeroExport(
                    resId = R.drawable.group_pulse_hero,
                    aspectRatio = 402f / 560f,
                    onCta = onCreateMoment,
                    ctaLabel = "Begin Story",
                )
            }

            GeAppear(delayMillis = 90) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GeChapterLabel("Chapter 02 / The Matrix")
                    Text(
                        "Select Your Arena",
                        color = GeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                    )
                    GeMomentTypeGrid(
                        onSelectExperience = onSelectExperience,
                        onSelectPurchase = onSelectPurchase,
                        onSelectLiving = onSelectLiving,
                    )
                    GeScanJoinButton(onClick = { showScanner = true })
                }
            }

            GeAppear(delayMillis = 180) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GeCard)
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    GeChapterLabel("Chapter 03 / Why Momentra")
                    Text(
                        "Why Groups Use Momentra",
                        color = GeText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                    )
                    listOf(
                        "Coordinate Together" to "Keep people, plans and money aligned in real-time.",
                        "Manage Shared Money" to "Track contributions, spending and settlements effortlessly.",
                        "Stay Organized" to "Plans, tasks and updates all in one unified dashboard.",
                        "Remember Together" to "Capture milestones, updates and memories as they happen.",
                    ).forEach { (title, body) ->
                        GeFeatureRow(
                            iconRes = R.drawable.group_pulse_feature_icon,
                            title = title,
                            body = body,
                        )
                    }
                }
            }
        }
        if (showScanner) {
            GroupJoinQrScanner(
                onCode = { code ->
                    showScanner = false
                    onJoinCode(code)
                },
                onDismiss = { showScanner = false },
            )
        }
    }
}
