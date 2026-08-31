package com.example.momentra.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.shell.GlobalTheme

/**
 * Independent Business CompanySwitcher (Context → Company → Moment).
 * Never infers company from a Moment.
 */
@Composable
fun CompanySwitcher(
    companies: List<CompanySummary>,
    selected: CompanySummary?,
    menuOpen: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelected: (CompanySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (companies.isEmpty()) return
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .widthIn(max = 140.dp)
                .heightIn(min = 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GlobalTheme.companyChipBackground)
                .border(1.dp, GlobalTheme.companyChipBorder, RoundedCornerShape(8.dp))
                .clickable { onToggle(!menuOpen) }
                .testTag(MaestroIds.COMPANY_SWITCHER)
                .semantics { contentDescription = "Switch company" }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selected?.displayName ?: "Company",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onToggle(false) }) {
            companies.forEach { company ->
                DropdownMenuItem(
                    text = { Text(company.displayName) },
                    onClick = {
                        onSelected(company)
                        onToggle(false)
                    },
                )
            }
        }
    }
}
