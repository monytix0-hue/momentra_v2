package com.example.momentra.ui.shell.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/** Figma 1035:7757 — Personal expense FAB (₹+). */
@Composable
fun PersonalExpenseFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = PersonalMasterExpenseTheme.Accent.copy(alpha = 0.4f),
                spotColor = PersonalMasterExpenseTheme.Accent.copy(alpha = 0.4f),
            )
            .clip(CircleShape)
            .background(PersonalMasterExpenseTheme.Accent)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Add expense"
                role = Role.Button
            }
            .testTag("personal_expense_fab"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "₹+",
            color = PersonalMasterExpenseTheme.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}
