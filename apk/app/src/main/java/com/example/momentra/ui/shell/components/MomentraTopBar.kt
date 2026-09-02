package com.example.momentra.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.splash.MomentraWordmark
import com.example.momentra.ui.theme.ShellTokens
import com.example.momentra.ui.theme.shell.GlobalSurfaceTheme
import com.example.momentra.ui.theme.shell.GlobalTheme

/**
 * Shell topbar variants (Figma):
 * - Personal `763:12256` — no QR
 * - Group `772:11972` — QR
 * - Business setup `1522:12255` — QR, no company chip
 * - Business activated `692:34971` — company chip + Moments label
 */
data class MomentraTopBarConfig(
    val context: AppContext,
    val displayName: String?,
    val companies: List<CompanySummary> = emptyList(),
    val selectedCompany: CompanySummary? = null,
    val companyMenuOpen: Boolean = false,
    val life360Available: Boolean = true,
    val globalCreateAvailable: Boolean = true,
    val qrScanAvailable: Boolean = false,
    val referAvailable: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentraTopBar(
    config: MomentraTopBarConfig,
    onCompanyMenuToggle: (Boolean) -> Unit = {},
    onCompanySelected: (CompanySummary) -> Unit = {},
    onQrScan: () -> Unit = {},
    onLife360: () -> Unit = {},
    onNewMoment: () -> Unit = {},
    onRefer: () -> Unit = {},
    onAvatar: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val showCompanyChip =
        config.context == AppContext.BUSINESS && config.selectedCompany != null
    val showQr = config.qrScanAvailable &&
        (config.context == AppContext.GROUP || config.context == AppContext.BUSINESS)
    val createLabel = if (showCompanyChip) "Moments" else "New"
    val actionBg = Color(0xFF1C233D)
    val labelMuted = Color(0xFFABA3BA)

    TopAppBar(
        modifier = modifier
            .testTag(MaestroIds.TOPBAR),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MomentraWordmark(
                    showTagline = true,
                    titleSizeSp = 16f,
                    taglineSizeSp = 5.5f,
                    alignStart = true,
                )
                if (showCompanyChip) {
                    CompanySwitcher(
                        companies = config.companies,
                        selected = config.selectedCompany,
                        menuOpen = config.companyMenuOpen,
                        onToggle = onCompanyMenuToggle,
                        onSelected = onCompanySelected,
                    )
                }
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (showQr) {
                    LabeledTopBarAction(
                        label = "QR",
                        background = actionBg,
                        onClick = onQrScan,
                        contentDescription = "Scan QR to join",
                        testTag = MaestroIds.TOPBAR_QR,
                        labelColor = Color.White.copy(alpha = 0.86f),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_shell_qr),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                if (config.life360Available) {
                    LabeledTopBarAction(
                        label = "360",
                        background = GlobalSurfaceTheme.life360.action,
                        onClick = onLife360,
                        contentDescription = "Open Life360",
                        testTag = MaestroIds.TOPBAR_LIFE360,
                        labelColor = Color.White.copy(alpha = 0.86f),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_shell_radar),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                if (config.globalCreateAvailable) {
                    LabeledTopBarAction(
                        label = createLabel,
                        background = GlobalTheme.createMomentCta,
                        onClick = onNewMoment,
                        contentDescription = if (showCompanyChip) "Open moments" else "Create moment",
                        testTag = MaestroIds.TOPBAR_NEW_MOMENT,
                        labelColor = Color.White.copy(alpha = 0.92f),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_shell_plus),
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
                if (config.referAvailable) {
                    LabeledTopBarAction(
                        label = "Refer",
                        background = actionBg,
                        onClick = onRefer,
                        contentDescription = "Refer a friend",
                        testTag = MaestroIds.TOPBAR_REFER,
                        labelColor = labelMuted,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_shell_gift),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                AvatarChip(
                    initials = initialsOf(config.displayName),
                    onClick = onAvatar,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GlobalTheme.topBarBackground,
        )
    )
}

@Composable
private fun LabeledTopBarAction(
    label: String,
    background: Color,
    onClick: () -> Unit,
    contentDescription: String,
    testTag: String,
    labelColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(32.dp)
            .height(50.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Text(
            text = label,
            color = labelColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.1.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AvatarChip(initials: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .testTag(MaestroIds.TOPBAR_PROFILE)
            .semantics { contentDescription = "Open profile" },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(ShellTokens.ActionCircle)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_shell_status_dot),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(8.dp),
        )
    }
}

private fun initialsOf(name: String?): String {
    val parts = name?.trim()?.split(Regex("\\s+")).orEmpty().filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "M"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
