package com.example.momentra.ui.shell.business.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.business.appstate.AppStateGlow
import com.example.momentra.ui.shell.business.appstate.BusinessAppStateTokens
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessIcons

/**
 * Figma `2:1514` — guided business type selection for Create Moment.
 *
 * Keep in lockstep with `BusinessGuidedCreateView` on iOS.
 */
@Composable
fun BusinessGuidedCreateContent(
    onBack: () -> Unit,
    onContinue: (BusinessGuidedCreateOption) -> Unit,
    submitting: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf(BusinessGuidedCreateOption.Manufacturing.id) }
    val selected = BusinessGuidedCreateSpecs.option(selectedId)
    val emerald = BusinessAppStateTokens.Emerald
    val scroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BusinessAppStateTokens.Page),
    ) {
        // Dual-orb ambient mesh — same as Pulse Empty / AppState bodies (Figma 2:221).
        AppStateGlow(
            accent = emerald,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 24.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(10.dp),
            )
        }

        Text(
            text = "${BusinessGuidedCreateSpecs.QUESTION_LINE_1}\n${BusinessGuidedCreateSpecs.QUESTION_LINE_2}",
            color = Color(0xFFF1F5F9),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = BusinessGuidedCreateSpecs.SUBTITLE,
            color = Color(0xFFA7A7AE),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(20.dp))

        BusinessGuidedCreateSpecs.options.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { option ->
                    BusinessChoiceCard(
                        option = option,
                        selected = option.id == selectedId,
                        emerald = emerald,
                        onClick = { selectedId = option.id },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        PreviewCard(option = selected, emerald = emerald)
        Spacer(Modifier.height(24.dp))

        BusinessContinueButton(
            label = BusinessGuidedCreateSpecs.CONTINUE_LABEL,
            enabled = !submitting,
            onClick = { onContinue(selected) },
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = BusinessAppStateTokens.ErrorRing, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = BusinessGuidedCreateSpecs.FOOTNOTE,
            color = Color(0xFFA7A7AE),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            fontFamily = PlusJakartaSans,
        )
        Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun BusinessContinueButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) BusinessAppStateTokens.Emerald else BusinessAppStateTokens.Emerald.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(0xFF0E0E0E),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun BusinessChoiceCard(
    option: BusinessGuidedCreateOption,
    selected: Boolean,
    emerald: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(126.dp)
            .clip(shape)
            .background(if (selected) emerald.copy(alpha = 0.07f) else Color(0xFF1B1B1E))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) emerald else Color.White.copy(alpha = 0.1f),
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val iconRes = BusinessIcons.res(option.iconKey)
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
            Icon(
                imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) emerald else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = "${option.titleLine1}\n${option.titleLine2}",
            color = Color(0xFFF1F5F9),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = option.meaning,
            color = Color(0xFFA7A7AE),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontFamily = PlusJakartaSans,
        )
    }
}

@Composable
private fun PreviewCard(option: BusinessGuidedCreateOption, emerald: Color) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF18181B))
            .border(1.dp, Color(0xFF053B29), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = option.previewEyebrow,
            color = emerald,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = option.previewHeadline,
            color = Color(0xFFF1F5F9),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PlusJakartaSans,
        )
        Text(
            text = option.previewBody,
            color = Color(0xFFA7A7AE),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = PlusJakartaSans,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f)),
        )
        option.capabilities.chunked(2).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pair.forEach { (icon, label) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        val iconRes = BusinessIcons.res(icon)
                        if (iconRes != null) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = emerald,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = label,
                            color = Color(0xFFF1F5F9),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
