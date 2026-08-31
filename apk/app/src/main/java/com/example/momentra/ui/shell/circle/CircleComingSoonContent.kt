package com.example.momentra.ui.shell.circle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.momentra.ui.theme.shell.CircleComingSoonTheme
import kotlinx.coroutines.launch

/**
 * Figma `1075:7556` Circle Coming Soon — static local UI in Circle context.
 * Does not call GET /life360 or Circle CRUD.
 */
@Composable
fun CircleComingSoonContent(modifier: Modifier = Modifier) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pinkBrush = Brush.horizontalGradient(
        listOf(CircleComingSoonTheme.accent, CircleComingSoonTheme.accentEnd),
    )
    val pageBrush = Brush.verticalGradient(
        listOf(CircleComingSoonTheme.pageStart, CircleComingSoonTheme.pageEnd),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBrush)
            .semantics { contentDescription = "Circle Coming Soon" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            CircleNetworkIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(pinkBrush)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "COMING SOON",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        color = Color.White,
                    )
                }
                Text(
                    text = "Circle",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    color = CircleComingSoonTheme.textPrimary,
                )
                Text(
                    text = "Your people network is being crafted. A new way to see how your connections shape your life.",
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = CircleComingSoonTheme.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "WHAT CIRCLE WILL REVEAL",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = CircleComingSoonTheme.accent.copy(alpha = 0.6f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FeatureCard(
                        "Relationship Map",
                        Icons.Filled.AccountTree,
                        CircleComingSoonTheme.accent,
                        CircleComingSoonTheme.accentEnd,
                        CircleComingSoonTheme.accent,
                        Modifier.weight(1f),
                    )
                    FeatureCard(
                        "Shared Moments",
                        Icons.Filled.Favorite,
                        CircleComingSoonTheme.lavender,
                        CircleComingSoonTheme.accent,
                        CircleComingSoonTheme.lavender,
                        Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FeatureCard(
                        "Life Balance",
                        Icons.Filled.Balance,
                        CircleComingSoonTheme.accentEnd,
                        CircleComingSoonTheme.peach,
                        CircleComingSoonTheme.accentEnd,
                        Modifier.weight(1f),
                    )
                    FeatureCard(
                        "Momentum",
                        Icons.Filled.Bolt,
                        CircleComingSoonTheme.peach,
                        CircleComingSoonTheme.accent,
                        CircleComingSoonTheme.peach,
                        Modifier.weight(1f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, CircleComingSoonTheme.accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .background(CircleComingSoonTheme.card)
                    .padding(24.dp),
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
                            .background(CircleComingSoonTheme.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = CircleComingSoonTheme.pageStart,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = "We're building Circle to map your connections automatically",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CircleComingSoonTheme.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "We're currently building this feature. Circle will unify your people, plans, and money signals into one powerful view once it's ready.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = CircleComingSoonTheme.textSecondary,
                )
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
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = CircleComingSoonTheme.accent,
                    )
                    Text(
                        text = "45%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CircleComingSoonTheme.accent,
                    )
                }
                LinearProgressIndicator(
                    progress = { CircleComingSoonTheme.decorativeProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CircleComingSoonTheme.accent,
                    trackColor = CircleComingSoonTheme.card,
                    strokeCap = StrokeCap.Round,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        snackbar.showSnackbar("We'll notify you when Circle is ready.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Notify me when Circle is ready" },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CircleComingSoonTheme.pageStart,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pinkBrush)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = CircleComingSoonTheme.pageStart,
                        )
                        Text(
                            text = "Notify Me When Ready",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = CircleComingSoonTheme.pageStart,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = CircleComingSoonTheme.pageStart,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, CircleComingSoonTheme.accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .background(CircleComingSoonTheme.accent.copy(alpha = 0.12f))
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CircleComingSoonTheme.accent.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = CircleComingSoonTheme.accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "As we build this feature, we'll keep you updated. Circle will be your unified view of everything that matters.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = CircleComingSoonTheme.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
private fun FeatureCard(
    label: String,
    icon: ImageVector,
    start: Color,
    end: Color,
    border: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, border.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .background(CircleComingSoonTheme.cardAlt.copy(alpha = 0.95f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(start, end))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CircleComingSoonTheme.textPrimary,
        )
    }
}

@Composable
private fun CircleNetworkIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension * 0.42f
        val pink = CircleComingSoonTheme.accent
        val lav = CircleComingSoonTheme.lavender
        listOf(0.55f, 0.78f, 1f).forEach { f ->
            drawCircle(
                color = lav.copy(alpha = 0.12f + (1f - f) * 0.08f),
                radius = maxR * f,
                center = c,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        val nodes = listOf(
            Offset(c.x - maxR * 0.45f, c.y - maxR * 0.35f),
            Offset(c.x + maxR * 0.45f, c.y - maxR * 0.35f),
            Offset(c.x - maxR * 0.45f, c.y + maxR * 0.4f),
            Offset(c.x + maxR * 0.45f, c.y + maxR * 0.4f),
        )
        nodes.forEach { n ->
            drawLine(pink.copy(alpha = 0.55f), c, n, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(color = pink, radius = 14.dp.toPx(), center = c)
        nodes.forEachIndexed { i, n ->
            drawCircle(
                color = if (i % 2 == 0) pink else lav,
                radius = (5 + i).dp.toPx(),
                center = n,
            )
        }
    }
}
