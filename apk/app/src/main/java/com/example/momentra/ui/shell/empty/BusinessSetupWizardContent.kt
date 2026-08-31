package com.example.momentra.ui.shell.empty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.setup.SetupPrefField
import com.example.momentra.ui.setup.SetupPreviewCard
import com.example.momentra.ui.setup.SetupPreferenceFilter
import com.example.momentra.ui.setup.SetupStickyFooter
import com.example.momentra.ui.setup.SetupTitleField
import com.example.momentra.ui.setup.SetupTokens
import com.example.momentra.ui.setup.SetupWizardHeader
import com.example.momentra.ui.setup.SetupWizardScaffold
import com.example.momentra.ui.shell.maestro.MaestroIds

/** Interactive Business setup wizard — Figma 692:34736+ with shared setup shell. */
@Composable
fun BusinessSetupWizardContent(
    kind: BusinessSetupKind,
    companyId: String,
    createViewModel: MomentCreateViewModel,
    submitting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onCreated: (CreateMomentOutcome) -> Unit,
    modifier: Modifier = Modifier,
    editingMomentId: String? = null,
    initialTitle: String? = null,
) {
    val catalog = remember(kind) { BusinessSetupCatalog.forKind(kind) }
    val selections = remember(kind) {
        mutableStateMapOf<String, Any>().apply {
            catalog.defaultPreferences.forEach { (k, v) -> put(k, v) }
        }
    }
    var momentTitle by remember(kind) { mutableStateOf(initialTitle?.takeIf { it.isNotBlank() } ?: catalog.defaultTitle) }

    DisposableEffect(kind) {
        MomentraAnalytics.get().onScreenEnter(kind.analyticsScreen)
        onDispose { MomentraAnalytics.get().onScreenExit(kind.analyticsScreen) }
    }

    val ctaBrush = when (kind) {
        BusinessSetupKind.TEAM_OPERATIONS -> Brush.horizontalGradient(
            listOf(Color(0xFF10B981), Color(0xFF34D399)),
        )
        BusinessSetupKind.BUSINESS_RUNWAY -> Brush.horizontalGradient(
            listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
        )
        BusinessSetupKind.BUSINESS_OPERATIONS -> Brush.horizontalGradient(
            listOf(SetupTokens.BizAccent, SetupTokens.AccentPurple),
        )
    }

    SetupWizardScaffold(
        modifier = modifier.testTag(kind.maestroTag),
        backgroundColor = SetupTokens.BizBg,
        footer = {
            SetupStickyFooter(
                tagline = catalog.footerTagline,
                ctaLabel = catalog.activateLabel,
                submitting = submitting,
                accentBrush = ctaBrush,
                ctaTestTag = MaestroIds.BUSINESS_SETUP_SUBMIT,
                backgroundColor = SetupTokens.BizBg,
                onCta = {
                    val apiPrefs = SetupPreferenceFilter.filterToCatalogKeys(
                        selections.toMap(),
                        BusinessSetupCatalog.allowedKeys(kind),
                    )
                    createViewModel.submitBusinessSetup(
                        kind = kind,
                        companyId = companyId,
                        preferences = apiPrefs,
                        title = momentTitle.trim().ifBlank { catalog.defaultTitle },
                        editingMomentId = editingMomentId,
                        onSuccess = onCreated,
                    )
                },
            )
        },
    ) {
        SetupWizardHeader(
            title = kind.title,
            durationLabel = catalog.subtitle,
            onClose = onClose,
            enabled = !submitting,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SetupTitleField(
                label = "Moment title",
                value = momentTitle,
                onValueChange = { momentTitle = it },
                placeholder = catalog.defaultTitle,
            )

            catalog.sections.forEachIndexed { index, section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SetupTokens.BizCard)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "${(index + 1).toString().padStart(2, '0')} · ${section.title.uppercase()}",
                        color = SetupTokens.BizAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    if (section.subtitle != null) {
                        Text(section.subtitle, color = SetupTokens.TextSecondary, fontSize = 12.sp)
                    }
                    section.fields.forEach { field ->
                        SetupPrefField(field, selections, selectedChipColor = kind.activateColor)
                    }
                }
            }

            businessPreview(kind, selections)

            if (error != null) {
                Text(error, color = SetupTokens.Error, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun businessPreview(kind: BusinessSetupKind, selections: Map<String, Any>) {
    when (kind) {
        BusinessSetupKind.TEAM_OPERATIONS -> SetupPreviewCard(
            title = "Team Ops Preview",
            subtitle = "${selections["teamName"]} · ${selections["workMode"]} · ${selections["size"]}",
            bullets = listOf(
                "Reviews: ${selections["reviewCycle"]}",
                "Approval threshold: ${selections["approvalThreshold"]}",
            ),
        )
        BusinessSetupKind.BUSINESS_RUNWAY -> SetupPreviewCard(
            title = "Runway Preview",
            subtitle = "Cash ${selections["availableCash"]} · Spend ${selections["monthlySpending"]}/mo",
            bullets = listOf(
                "Revenue: ${selections["monthlyRevenue"]} (${selections["revenueStage"]})",
                "Warning at ${selections["warningThreshold"]} runway",
            ),
        )
        BusinessSetupKind.BUSINESS_OPERATIONS -> SetupPreviewCard(
            title = "Ops Monitoring Preview",
            subtitle = "${selections["scope"]} · ${selections["model"]} · ${selections["cadence"]} cadence",
            bullets = listOf(
                "Budget: ${selections["monthlyBudget"]}",
                "Alarm threshold: ${selections["approvalAlarm"]}",
            ),
        )
    }
}
