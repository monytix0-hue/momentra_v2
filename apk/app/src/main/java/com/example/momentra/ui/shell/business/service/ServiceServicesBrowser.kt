package com.example.momentra.ui.shell.business.service

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.api.OperatingMasterDto
import com.example.momentra.data.api.OperatingProjectionDto
import com.example.momentra.ui.shell.business.shared.OperatingBrowserKind
import com.example.momentra.ui.shell.business.shared.OperatingBrowserSelection
import com.example.momentra.ui.shell.business.shared.OperatingChromeState
import com.example.momentra.ui.shell.business.shared.OperatingSheetScrim
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.ServicesTeal

/**
 * SVC-SHEET · Services browser — Figma `2533:29628`.
 *
 * Compact picker (restaurant menu-browser analogue) used from Booking / Finalize Charge
 * to Add a saved service or package into the parent draft. `+ New service` / `+ New package`
 * deep-link into Phase 2 master forms.
 */
private val BrowserCategories = listOf("Hair", "Skin", "Spa", "Wellness", "Nails")

data class ServiceBrowserItem(
    val id: String,
    val name: String,
    val meta: String,
    val priceLabel: String,
    val unitPrice: String?,
    val category: String,
    val sac: String? = null,
    val masterKind: String? = null,
    val unit: String? = null,
    val isPackage: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServiceServicesBrowserSheet(
    model: OperatingProjectionDto,
    momentId: String?,
    captureHint: String,
    onAdd: (OperatingBrowserSelection) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BrowserCategories.first()) }
    var formKind by remember { mutableStateOf<ServiceMasterFormKind?>(null) }

    val live = remember(model.masters) {
        model.masters.orEmpty()
            .filter {
                it.kind.uppercase() in setOf("PRODUCT", "ITEM", "SERVICE", "PACKAGE", "CONSUMABLE")
            }
            .map { it.toBrowserItem() }
            .sortedBy { it.name.lowercase() }
    }
    val source = live.ifEmpty { brightCareBrowserFixtures() }
    val usingFixtures = live.isEmpty()

    val filtered = source.filter { item ->
        val q = query.trim()
        val matchesQuery = q.isEmpty() ||
            item.name.contains(q, ignoreCase = true) ||
            item.meta.contains(q, ignoreCase = true) ||
            item.sac?.contains(q, ignoreCase = true) == true
        // Figma Hair selection still shows the mixed BrightCare fixture set.
        val matchesCat = usingFixtures ||
            item.category.equals(category, ignoreCase = true) ||
            item.category.isBlank()
        matchesQuery && matchesCat
    }

    val sheetShape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .clip(sheetShape)
            .background(ServicesTeal.card)
            .border(1.dp, ServicesTeal.sheetBorder, sheetShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Services",
                style = ServicesTeal.rowTitle().copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Tap Add · service or package",
                style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp),
                maxLines = 1,
            )
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ServicesTeal.chip)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✕",
                    color = ServicesTeal.grab,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ServicesTeal.chip)
                .border(1.dp, ServicesTeal.teal, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = ServicesTeal.textPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                cursorBrush = SolidColor(ServicesTeal.teal),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search service, package or SAC",
                            color = ServicesTeal.grab,
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                        )
                    }
                    inner()
                },
            )
            Text(
                "Filter",
                color = ServicesTeal.whyBody,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BrowserCategories.forEach { chip ->
                TealModeChip(
                    text = chip,
                    selected = chip == category,
                    limeSelected = true,
                    onClick = { category = chip },
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                Text(
                    "No offerings yet — create a service or package below. Empty catalogue never invents a price.",
                    style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 12.sp),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                filtered.forEach { item ->
                    ServiceBrowserRow(
                        item = item,
                        onAdd = {
                            onAdd(
                                OperatingBrowserSelection(
                                    id = item.id,
                                    name = item.name,
                                    unit = item.unit,
                                    unitPrice = item.unitPrice,
                                    masterKind = item.masterKind,
                                    code = item.sac,
                                    browserKind = OperatingBrowserKind.Services,
                                    captureHint = captureHint,
                                ),
                            )
                        },
                    )
                }
            }
        }

        CreateMasterRow(
            label = "+ New service",
            hint = "Name · SAC · GST · duration",
            onClick = { formKind = ServiceMasterFormKind.Service },
        )
        CreateMasterRow(
            label = "+ New package",
            hint = "Name · SAC · GST · duration",
            onClick = { formKind = ServiceMasterFormKind.Package },
        )
    }

    formKind?.let { kind ->
        OperatingSheetScrim(
            onDismiss = { formKind = null },
            accent = ServicesTeal.teal,
            surface = ServicesTeal.sheet,
            border = ServicesTeal.sheetBorder,
        ) {
            ServiceMasterFormSheet(
                kind = kind,
                model = model,
                momentId = momentId,
                existing = null,
                onDismiss = { formKind = null },
                onSaved = {
                    formKind = null
                    OperatingChromeState.refresh += 1
                },
                onError = onError,
            )
        }
    }
}

@Composable
private fun ServiceBrowserRow(
    item: ServiceBrowserItem,
    onAdd: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ServicesTeal.chip)
            .border(1.dp, ServicesTeal.lime.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .clickable(onClick = onAdd)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.name,
                style = ServicesTeal.rowTitle().copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.meta,
                style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            item.priceLabel,
            style = ServicesTeal.rowValue(ServicesTeal.whyBody).copy(fontSize = 13.sp),
            maxLines = 1,
        )
        Box(
            Modifier
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(10.dp),
                    clip = false,
                    ambientColor = ServicesTeal.lime.copy(alpha = 0.40f),
                    spotColor = ServicesTeal.lime.copy(alpha = 0.40f),
                )
                .clip(RoundedCornerShape(10.dp))
                .background(ServicesTeal.lime)
                .clickable(onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "Add",
                color = ServicesTeal.inkOnTeal,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun CreateMasterRow(
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ServicesTeal.lime.copy(alpha = 0.08f))
            .border(1.dp, ServicesTeal.lime.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = ServicesTeal.rowValue(ServicesTeal.whyBody).copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Text(
            hint,
            style = ServicesTeal.rowCaption(ServicesTeal.grab).copy(fontSize = 11.sp),
            maxLines = 1,
        )
    }
}

private fun OperatingMasterDto.toBrowserItem(): ServiceBrowserItem {
    val isPackage = kind.equals("PACKAGE", ignoreCase = true) ||
        name.contains("package", ignoreCase = true)
    val category = dimension?.takeIf { it.isNotBlank() }
        ?: BrowserCategories.firstOrNull { cat ->
            name.contains(cat, ignoreCase = true) || code.equals(cat, ignoreCase = true)
        }
        ?: "Hair"
    val sac = hsn?.takeIf { it.isNotBlank() } ?: code?.takeIf { it.isNotBlank() }
    val gst = gstRate?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("%")) it else "$it%" }
    val duration = unit?.takeIf { it.isNotBlank() }
    val meta = when {
        isPackage -> listOfNotNull(
            "Package",
            duration,
            gst?.let { "$it per session" },
        ).joinToString(" · ")
        else -> listOfNotNull(
            duration,
            sac?.let { "SAC $it" },
            gst,
        ).joinToString(" · ")
    }.ifBlank { if (isPackage) "Package" else "Service" }
    val rawPrice = sellPrice?.removePrefix("₹")?.trim()?.takeIf { it.isNotBlank() }
    return ServiceBrowserItem(
        id = id,
        name = name.ifBlank { if (isPackage) "Unnamed package" else "Unnamed service" },
        meta = meta,
        priceLabel = rawPrice?.let { "₹$it" } ?: "—",
        unitPrice = rawPrice,
        category = category,
        sac = sac,
        masterKind = kind.takeIf { it.isNotBlank() } ?: if (isPackage) "PACKAGE" else "SERVICE",
        unit = duration,
        isPackage = isPackage,
    )
}

/** BrightCare fixtures for visual QA when projection masters are empty (Figma `2533:29628`). */
internal fun brightCareBrowserFixtures(): List<ServiceBrowserItem> = listOf(
    ServiceBrowserItem(
        id = "seed-svc-haircut",
        name = "Haircut & style",
        meta = "45 min · SAC 999721 · 18%",
        priceLabel = "₹899",
        unitPrice = "899",
        category = "Hair",
        sac = "999721",
        masterKind = "SERVICE",
        unit = "45 min",
    ),
    ServiceBrowserItem(
        id = "seed-svc-colour",
        name = "Hair colour (global)",
        meta = "90 min · SAC 999721 · 18%",
        priceLabel = "₹2,499",
        unitPrice = "2499",
        category = "Hair",
        sac = "999721",
        masterKind = "SERVICE",
        unit = "90 min",
    ),
    ServiceBrowserItem(
        id = "seed-svc-facial",
        name = "Deep-cleanse facial",
        meta = "60 min · SAC 999721 · 18%",
        priceLabel = "₹1,499",
        unitPrice = "1499",
        category = "Skin",
        sac = "999721",
        masterKind = "SERVICE",
        unit = "60 min",
    ),
    ServiceBrowserItem(
        id = "seed-svc-keratin",
        name = "Keratin package · 3 sessions",
        meta = "Package · 180 days · 18% per session",
        priceLabel = "₹7,999",
        unitPrice = "7999",
        category = "Hair",
        sac = null,
        masterKind = "PACKAGE",
        unit = "180 days",
        isPackage = true,
    ),
    ServiceBrowserItem(
        id = "seed-svc-massage",
        name = "Swedish massage",
        meta = "60 min · SAC 999722 · 18%",
        priceLabel = "₹2,200",
        unitPrice = "2200",
        category = "Spa",
        sac = "999722",
        masterKind = "SERVICE",
        unit = "60 min",
    ),
)
