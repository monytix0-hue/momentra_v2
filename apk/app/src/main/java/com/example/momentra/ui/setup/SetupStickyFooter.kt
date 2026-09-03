package com.example.momentra.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SetupStickyFooter(
    tagline: String,
    ctaLabel: String,
    onCta: () -> Unit,
    submitting: Boolean = false,
    saved: Boolean = true,
    onPreview: (() -> Unit)? = null,
    accentBrush: Brush = Brush.horizontalGradient(listOf(Color(0xFF7C5CFC), Color(0xFFE91E63))),
    ctaTestTag: String? = null,
    backgroundColor: Color = SetupTokens.BgPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            tagline,
            color = SetupTokens.BrandPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (saved) {
                    Text("✓", color = SetupTokens.SavedGreen, fontSize = 14.sp)
                    Text("Saved", color = SetupTokens.TextSecondary, fontSize = 12.sp)
                }
            }
            if (onPreview != null) {
                Text(
                    "Preview",
                    color = SetupTokens.BrandPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onPreview)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Preview setup"
                        },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(accentBrush)
                .clickable(enabled = !submitting, onClick = onCta)
                .then(if (ctaTestTag != null) Modifier.testTag(ctaTestTag) else Modifier)
                .semantics {
                    role = Role.Button
                    contentDescription = ctaLabel
                    if (ctaTestTag != null) {
                        testTag = ctaTestTag
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(ctaLabel, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}
