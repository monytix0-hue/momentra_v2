package com.example.momentra.ui.shell.business.life

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.business.life.components.CompanyLifeColors
import com.example.momentra.ui.shell.business.shared.BUSINESS_LIFE_FINANCE_SCOPE
import com.example.momentra.ui.shell.business.shared.BusinessLifeActivityFact
import com.example.momentra.ui.shell.business.shared.BusinessLifeCompanyState
import com.example.momentra.ui.shell.business.shared.BusinessLifeLens
import com.example.momentra.ui.shell.business.shared.BusinessLifeMovementTab
import com.example.momentra.ui.theme.PlusJakartaSans

@Composable
fun BusinessLifeLensChips(
    selected: BusinessLifeLens,
    onSelect: (BusinessLifeLens) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BusinessLifeLens.entries.forEach { lens ->
            val on = lens == selected
            Text(
                lens.label(),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) CompanyLifeColors.IndigoSolid else CompanyLifeColors.Card)
                    .clickable { onSelect(lens) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (on) CompanyLifeColors.Text else CompanyLifeColors.Secondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
        }
    }
}

@Composable
fun BusinessLifeBlocks(
    state: BusinessLifeCompanyState,
    movement: BusinessLifeMovementTab,
    onMovement: (BusinessLifeMovementTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        LifeBlock("Company overview") {
            state.overview.forEach { line ->
                LifePair(line.title, line.state)
            }
        }
        LifeBlock("This week") {
            Text(state.thisWeek, color = CompanyLifeColors.Text, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        }
        LifeBlock("Where the business moved") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementChip("Activity", movement == BusinessLifeMovementTab.ACTIVITY) {
                    onMovement(BusinessLifeMovementTab.ACTIVITY)
                }
                MovementChip("Money", movement == BusinessLifeMovementTab.MONEY) {
                    onMovement(BusinessLifeMovementTab.MONEY)
                }
            }
            if (movement == BusinessLifeMovementTab.ACTIVITY) {
                if (state.activity.isEmpty()) {
                    Muted("Nothing recorded yet.")
                } else {
                    state.activity.forEach { ActivityRow(it) }
                }
            } else if (state.moneyMovement.isEmpty()) {
                Muted("No money figures recorded.")
            } else {
                state.moneyMovement.forEach { LifePair(it.label, it.value) }
            }
        }
        LifeBlock("Needs attention") {
            if (state.needsAttention.isEmpty()) Muted("Nothing needs attention.")
            else state.needsAttention.forEach { Muted(it, CompanyLifeColors.Text) }
        }
        LifeBlock("What's working") {
            if (state.working.isEmpty()) Muted("Nothing marked as working.")
            else state.working.forEach { Muted(it, CompanyLifeColors.Text) }
        }
        LifeBlock("Financial position") {
            Text(
                BUSINESS_LIFE_FINANCE_SCOPE,
                color = CompanyLifeColors.Secondary,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
            )
            if (state.financialPosition.isEmpty()) Muted("No company totals yet.")
            else state.financialPosition.forEach { LifePair(it.label, it.value) }
        }
    }
}

@Composable
private fun LifeBlock(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CompanyLifeColors.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = CompanyLifeColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = PlusJakartaSans)
        content()
    }
}

@Composable
private fun LifePair(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = CompanyLifeColors.Secondary, fontSize = 14.sp, fontFamily = PlusJakartaSans)
        Text(value, color = CompanyLifeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = PlusJakartaSans)
    }
}

@Composable
private fun MovementChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CompanyLifeColors.Indigo else CompanyLifeColors.Bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = CompanyLifeColors.Text,
        fontSize = 12.sp,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
private fun ActivityRow(item: BusinessLifeActivityFact) {
    Text(
        item.title.ifBlank { "Activity" },
        color = CompanyLifeColors.Text,
        fontSize = 14.sp,
        fontFamily = PlusJakartaSans,
    )
}

@Composable
private fun Muted(text: String, color: androidx.compose.ui.graphics.Color = CompanyLifeColors.Secondary) {
    Text(text, color = color, fontSize = 14.sp, fontFamily = PlusJakartaSans)
}

private fun BusinessLifeLens.label(): String = when (this) {
    BusinessLifeLens.OVERVIEW -> "Overview"
    BusinessLifeLens.MONEY -> "Money"
    BusinessLifeLens.DAILY -> "Daily"
    BusinessLifeLens.TEAM -> "Team"
}
