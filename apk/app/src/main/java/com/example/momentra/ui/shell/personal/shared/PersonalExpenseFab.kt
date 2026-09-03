package com.example.momentra.ui.shell.personal.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp

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
        Icon(
            imageVector = Icons.Outlined.CurrencyRupee,
            contentDescription = null,
            tint = PersonalMasterExpenseTheme.Text,
            modifier = Modifier.size(24.dp),
        )
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = PersonalMasterExpenseTheme.Text,
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
        )
    }
}
