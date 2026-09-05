package com.example.momentra.ui.shell.group.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

@Composable
fun QuickAddDraftActions(
    submitLabel: String,
    submitEnabled: Boolean,
    ctaBrush: Brush,
    loading: Boolean = false,
    footer: String? = null,
    lightLabel: Boolean = false,
    onSubmit: () -> Unit,
    onSaveDraft: (() -> Unit)? = null,
    draftEnabled: Boolean = false,
) {
    val ink = Color(0xFF0F172A)
    val lightInk = TripSheetTokens.Text
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ctaBrush)
                .then(if (submitEnabled && !loading) Modifier.clickable(onClick = onSubmit) else Modifier)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = if (lightLabel) lightInk else ink,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    submitLabel,
                    color = if (lightLabel) lightInk else ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        if (onSaveDraft != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, TripSheetTokens.Border, RoundedCornerShape(14.dp))
                    .then(if (draftEnabled && !loading) Modifier.clickable(onClick = onSaveDraft) else Modifier)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Save draft",
                    color = if (draftEnabled && !loading) TripSheetTokens.Text else TripSheetTokens.Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
        footer?.let {
            Text(it, color = Color(0xFF64748B), fontSize = 12.sp, fontFamily = PlusJakartaSans)
        }
    }
}
