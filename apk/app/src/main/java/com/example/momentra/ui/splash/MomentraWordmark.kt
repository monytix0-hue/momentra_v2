package com.example.momentra.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.MomentraBrandColors

@Composable
fun MomentraWordmark(
    modifier: Modifier = Modifier,
    showTagline: Boolean = false,
    titleSizeSp: Float = 32f,
    taglineSizeSp: Float = 9f,
    alignStart: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MomentraBrandColors.TextOnDark)) { append("momentr") }
                    withStyle(SpanStyle(color = MomentraBrandColors.Cta)) { append("a") }
                },
                fontSize = titleSizeSp.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
            Spacer(Modifier.width(2.dp))
            Box(
                Modifier
                    .size(maxOf(5.dp, (titleSizeSp * 0.22f).dp))
                    .offset(y = (-(titleSizeSp * 0.12f)).dp)
                    .background(MomentraBrandColors.Progress, CircleShape),
            )
        }

        if (showTagline) {
            Text(
                "TOGETHER · FORWARD",
                fontSize = taglineSizeSp.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                color = MomentraBrandColors.TextOnDark.copy(alpha = 0.38f),
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
        }
    }
}
