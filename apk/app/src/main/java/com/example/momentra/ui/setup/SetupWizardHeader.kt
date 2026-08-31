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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SetupWizardHeader(
    title: String,
    durationLabel: String = "About 3 minutes",
    onClose: () -> Unit,
    onSummary: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SetupTokens.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(durationLabel, color = SetupTokens.TextSecondary, fontSize = 12.sp)
        }
        if (onSummary != null) {
            Column(
                modifier = Modifier
                    .clickable(enabled = enabled, onClick = onSummary)
                    .padding(8.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Summary"
                    },
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(SetupTokens.TextPrimary),
                    )
                }
            }
        }
        Text(
            "×",
            color = SetupTokens.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClose)
                .padding(8.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Close setup"
                },
        )
    }
}
