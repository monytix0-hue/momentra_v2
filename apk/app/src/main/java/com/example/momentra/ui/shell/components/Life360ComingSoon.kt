package com.example.momentra.ui.shell.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.shell.GlobalSurfaceTheme
import kotlinx.coroutines.launch

/**
 * Figma `1075:7637` Life 360 Coming Soon — static local UI only.
 * Does not call GET /life360 or read projection.life360.
 */
@Composable
fun Life360GlobalSurface(onClose: () -> Unit) {
    val theme = GlobalSurfaceTheme.life360
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val goldBrush = Brush.horizontalGradient(listOf(theme.gold, theme.goldEnd))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.comingSoonBackground)
            .semantics { contentDescription = "Life360 Coming Soon" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClose) {
                    Text("Close", color = theme.textSecondary)
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(goldBrush)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "COMING SOON",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        color = theme.comingSoonBackground,
                    )
                }
                Text(
                    text = "Life 360",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = theme.textPrimary,
                )
                Text(
                    text = "Your complete life intelligence is on the way. We're building something meaningful.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = theme.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Life360RadarIllustration(
                modifier = Modifier.size(220.dp),
                gold = theme.gold,
                online = theme.online,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, theme.gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .background(theme.card)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.gold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = theme.comingSoonBackground,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = "Your Life Map is waiting to form",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = theme.textPrimary,
                    )
                }
                Text(
                    text = "We're currently building this feature. Life 360 will unify your money, people, work, and growth signals into one powerful view once it's ready.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = theme.textSecondary,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "WHAT LIFE 360 WILL REVEAL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = theme.gold.copy(alpha = 0.6f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeaturePreviewCard("Life Alignment", Icons.Filled.TrackChanges, theme, goldBrush, Modifier.weight(1f))
                    FeaturePreviewCard("Life Energy", Icons.Filled.Bolt, theme, goldBrush, Modifier.weight(1f))
                    FeaturePreviewCard("Life Balance", Icons.Filled.Balance, theme, goldBrush, Modifier.weight(1f))
                    FeaturePreviewCard("Life Momentum", Icons.Filled.RocketLaunch, theme, goldBrush, Modifier.weight(1f))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "DEVELOPMENT PROGRESS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = theme.gold,
                    )
                    Text(
                        text = "65%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = theme.gold,
                    )
                }
                LinearProgressIndicator(
                    progress = { theme.decorativeProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = theme.gold,
                    trackColor = theme.card,
                    strokeCap = StrokeCap.Round,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        snackbar.showSnackbar("We'll notify you when Life 360 is ready.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Notify me when Life 360 is ready" },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = theme.comingSoonBackground),
                contentPadding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(goldBrush)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = theme.comingSoonBackground)
                        Text(
                            text = "Notify Me When Ready",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = theme.comingSoonBackground,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = theme.comingSoonBackground,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, theme.gold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .background(Color(0x661C1B1B))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.gold.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = theme.gold, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "As we build this feature, we'll keep you updated. Life 360 will be your unified view of everything that matters.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = theme.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun FeaturePreviewCard(
    label: String,
    icon: ImageVector,
    theme: GlobalSurfaceTheme.Life360,
    goldBrush: Brush,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, theme.gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(theme.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(goldBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = theme.comingSoonBackground, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = theme.textPrimary,
            maxLines = 2,
        )
    }
}

@Composable
private fun Life360RadarIllustration(
    modifier: Modifier = Modifier,
    gold: Color,
    online: Color,
) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension * 0.42f
        listOf(0.35f, 0.55f, 0.75f, 1f).forEach { f ->
            drawCircle(
                color = online.copy(alpha = 0.18f + (1f - f) * 0.08f),
                radius = maxR * f,
                center = c,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        drawLine(
            color = gold.copy(alpha = 0.55f),
            start = c,
            end = Offset(c.x + maxR * 0.72f, c.y - maxR * 0.18f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = online, radius = 10.dp.toPx(), center = c)
        drawCircle(color = gold, radius = 5.dp.toPx(), center = Offset(c.x + maxR * 0.72f, c.y - maxR * 0.18f))
        listOf(
            Offset(c.x - maxR * 0.55f, c.y - maxR * 0.4f),
            Offset(c.x + maxR * 0.5f, c.y + maxR * 0.45f),
            Offset(c.x - maxR * 0.2f, c.y + maxR * 0.6f),
        ).forEach { p ->
            drawCircle(color = gold.copy(alpha = 0.85f), radius = 3.dp.toPx(), center = p)
        }
    }
}
