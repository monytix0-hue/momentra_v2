package com.example.momentra.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.momentra.R
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.splash.MomentraWordmark
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.ShellTokens
import com.example.momentra.ui.theme.shell.GlobalSurfaceTheme
import com.example.momentra.ui.theme.shell.GlobalTheme

data class MomentraTopBarConfig(
    val context: AppContext,
    val displayName: String?,
    val companies: List<CompanySummary> = emptyList(),
    val selectedCompany: CompanySummary? = null,
    val companyMenuOpen: Boolean = false,
    val life360Available: Boolean = true,
    val globalCreateAvailable: Boolean = true,
    val qrScanAvailable: Boolean = false,
)

@Composable
fun MomentraTopBar(
    config: MomentraTopBarConfig,
    onCompanyMenuToggle: (Boolean) -> Unit = {},
    onCompanySelected: (CompanySummary) -> Unit = {},
    onQrScan: () -> Unit = {},
    onLife360: () -> Unit = {},
    onNewMoment: () -> Unit = {},
    onAvatar: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val showQr = config.qrScanAvailable &&
        (config.context == AppContext.GROUP || config.context == AppContext.BUSINESS)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ShellTokens.TopBarHeight)
            .background(GlobalTheme.topBarBackground)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(MaestroIds.TOPBAR),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            MomentraWordmark(
                showTagline = false,
                titleSizeSp = 18f,
                taglineSizeSp = 6f,
                alignStart = true,
            )
            if (config.context == AppContext.BUSINESS) {
                CompanySwitcher(
                    companies = config.companies,
                    selected = config.selectedCompany,
                    menuOpen = config.companyMenuOpen,
                    onToggle = onCompanyMenuToggle,
                    onSelected = onCompanySelected,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showQr) {
                RoundAction(
                    background = Color(0xFF1C233D),
                    onClick = onQrScan,
                    contentDescription = "Scan QR to join",
                    testTag = MaestroIds.TOPBAR_QR,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_shell_qr),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            if (config.life360Available) {
                RoundAction(
                    background = GlobalSurfaceTheme.life360.action,
                    onClick = onLife360,
                    contentDescription = "Open Life360",
                    testTag = MaestroIds.TOPBAR_LIFE360,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_shell_radar),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (config.globalCreateAvailable) {
                RoundAction(
                    background = GlobalTheme.createMomentCta,
                    onClick = onNewMoment,
                    contentDescription = "Create moment",
                    testTag = MaestroIds.TOPBAR_NEW_MOMENT,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_shell_plus),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            AvatarChip(
                initials = initialsOf(config.displayName),
                onClick = onAvatar,
            )
        }
    }
}

@Composable
private fun RoundAction(
    background: Color,
    onClick: () -> Unit,
    contentDescription: String,
    testTag: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ShellTokens.IconTap)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun AvatarChip(initials: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ShellTokens.AvatarSize)
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
