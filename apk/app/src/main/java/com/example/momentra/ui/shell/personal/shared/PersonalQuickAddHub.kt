package com.example.momentra.ui.shell.personal.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.momentra.ui.theme.generated.PersonalTokens
import com.example.momentra.ui.theme.PlusJakartaSans

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalQuickAddHub(
    hasActiveMoment: Boolean,
    momentTypeCode: String? = null,
    capabilityCodes: List<String>? = null,
    simpleMode: Boolean = false,
    onSpend: () -> Unit = {},
    onIncome: () -> Unit,
    onRecovery: () -> Unit = {},
    onMood: () -> Unit = {},
    onAttention: () -> Unit = {},
    onAdjust: () -> Unit = {},
    onTransfer: () -> Unit = {},
    onSavings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    val family = personalPulseFamilyFor(momentTypeCode)
    val tiles = PersonalActionRegistry.tiles(
        family = PersonalPulseFamily.LIFE_OPERATIONS,
        hasActiveMoment = hasActiveMoment,
        capabilityCodes = capabilityCodes,
        simpleMode = simpleMode,
    ).filter { tile ->
        search.isBlank() || tile.label.contains(search, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Quick Add",
                    color = PersonalTokens.Colors.TextPrimary,
                    style = PersonalTokens.Type.Hero,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Simplify your day and align focus",
                    color = PersonalTokens.Colors.TextSecondary,
                    style = PersonalTokens.Type.Caption,
                )
            }
        }

        Spacer(Modifier.height(PersonalTokens.Space.Md))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.24f),
                            Color(0xFF6C4EF2).copy(alpha = 0.12f),
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Text(
                if (family == PersonalPulseFamily.LIFE_OPERATIONS) "Add to Everyday" else family.switcherLabel().let { "Add to $it" },
                color = PersonalTokens.Colors.TextPrimary,
                style = PersonalTokens.Type.Hero,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Spend, mood, recovery — and more when you need it.",
                color = PersonalTokens.Colors.TextSecondary,
                style = PersonalTokens.Type.Caption,
            )
        }

        Spacer(Modifier.height(PersonalTokens.Space.Md))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PersonalTokens.Colors.SurfaceContainer)
                .border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(com.example.momentra.R.drawable.ic_pers_compass),
                contentDescription = null,
                modifier = Modifier.size(15.dp).alpha(0.7f),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = PersonalTokens.Type.Body.copy(color = PersonalTokens.Colors.TextPrimary, fontFamily = PlusJakartaSans),
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text(
                            "Search personal actions...",
                            color = PersonalTokens.Colors.TextSecondary,
                            style = PersonalTokens.Type.Caption,
                        )
                    }
                    inner()
                },
            )
        }

        Spacer(Modifier.height(PersonalTokens.Space.Md))

        if (family == PersonalPulseFamily.LIFE_OPERATIONS) {
            Text(
                family.switcherLabel(),
                color = PersonalTokens.Colors.BrandPrimary,
                style = PersonalTokens.Type.Metadata,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(PersonalTokens.Space.Sm))
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 3,
        ) {
            tiles.forEach { tile ->
                PersonalQaTile(
                    tile = tile,
                    enabled = tile.tappable,
                    dimmed = !hasActiveMoment || !tile.enabledWhenMomentActive,
                    onClick = { dispatchTile(tile.label, onSpend, onIncome, onRecovery, onMood, onAttention, onAdjust, onTransfer, onSavings) },
                )
            }
        }

        if (!hasActiveMoment) {
            Spacer(Modifier.height(PersonalTokens.Space.Md))
            Text(
                "Tap a tile to set up Life Operations, then log spend, mood, and money.",
                color = PersonalTokens.Colors.TextSecondary,
                style = PersonalTokens.Type.Caption,
            )
        }

        Spacer(Modifier.height(PersonalTokens.Space.Inner))
    }
}

private fun dispatchTile(
    label: String,
    onSpend: () -> Unit,
    onIncome: () -> Unit,
    onRecovery: () -> Unit,
    onMood: () -> Unit,
    onAttention: () -> Unit,
    onAdjust: () -> Unit,
    onTransfer: () -> Unit,
    onSavings: () -> Unit,
) {
    when (label) {
        "Spend", "Expense" -> onSpend()
        "Income" -> onIncome()
        "Recovery" -> onRecovery()
        "Mood" -> onMood()
        "Attention" -> onAttention()
        "Adjust" -> onAdjust()
        "Transfer" -> onTransfer()
        "Savings" -> onSavings()
    }
}

@Composable
private fun PersonalQaTile(
    tile: PersonalActionTile,
    enabled: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (dimmed) 0.55f else 1f
    Column(
        modifier = Modifier
            .width(104.dp)
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(tile.gradientStart, tile.gradientEnd)))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(tile.iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp).alpha(alpha),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            tile.label,
            color = Color.White.copy(alpha = alpha),
            style = PersonalTokens.Type.Body,
            fontWeight = FontWeight.Bold,
        )
    }
}