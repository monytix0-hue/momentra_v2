package com.example.momentra.ui.shell.business.setup

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens

/**
 * Chrome + subtype chips + active setup document. Activate → `submitBusinessSetup`.
 * Keep in lockstep with `BusinessSetupShell.swift`.
 */
@Composable
fun BusinessSetupShell(
    family: BusinessSetupFamily,
    createViewModel: MomentCreateViewModel,
    onBack: () -> Unit,
    onCreated: (CreateMomentOutcome) -> Unit,
    modifier: Modifier = Modifier,
    initialTypeCode: String? = null,
    companyId: String? = null,
    editingMomentId: String? = null,
    editingMomentStatus: String? = null,
    initialTitle: String? = null,
) {
    var typeCode by remember {
        mutableStateOf((initialTypeCode ?: family.defaultTypeCode).uppercase())
    }
    val values = remember { mutableStateMapOf<String, String>() }
    var didSeed by remember { mutableStateOf(false) }
    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    var savingDraft by remember { mutableStateOf(false) }
    var draftMomentId by remember { mutableStateOf(editingMomentId) }
    var draftMomentStatus by remember { mutableStateOf(editingMomentStatus) }
    var draftSavedMessage by remember { mutableStateOf<String?>(null) }
    val createState by createViewModel.state.collectAsState()

    val theme = remember(typeCode) { BusinessSetupTheme.forTypeCode(typeCode) }
    val spec = remember(typeCode) { BusinessSetupDocuments.spec(typeCode) }

    fun reseed(code: String, priorTitle: String? = null) {
        val next = BusinessSetupDocuments.spec(code)
        val seeded = next.defaultPreferences()
        val carried = priorTitle?.trim().orEmpty()
        when {
            carried.isNotBlank() -> seeded[next.defaultTitleKey] = carried
            !initialTitle.isNullOrBlank() -> seeded[next.defaultTitleKey] = initialTitle
        }
        values.forEach { (key, value) ->
            if (value.isNotBlank() && seeded.containsKey(key)) seeded[key] = value
        }
        values.clear()
        values.putAll(seeded)
    }

    LaunchedEffect(Unit) {
        if (!didSeed) {
            reseed(typeCode)
            if (!initialTitle.isNullOrBlank() && values[spec.defaultTitleKey].isNullOrBlank()) {
                values[spec.defaultTitleKey] = initialTitle
            }
            didSeed = true
            val loadId = editingMomentId
            if (loadId != null) {
                createViewModel.getDomainSetupPrefill(loadId)?.preferences?.forEach { (key, raw) ->
                    when (raw) {
                        is String -> if (raw.isNotBlank()) values[key] = raw
                        is List<*> -> {
                            val joined = raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                                .joinToString("|")
                            if (joined.isNotBlank()) values[key] = joined
                        }
                        is Number, is Boolean -> values[key] = raw.toString()
                    }
                }
            }
        }
    }

    LaunchedEffect(createState.submitting, createState.error) {
        if (!createState.submitting && createState.error != null) {
            savingDraft = false
        }
    }

    val titleValue = run {
        val raw = values[spec.defaultTitleKey]?.trim().orEmpty()
        when {
            raw.isNotEmpty() -> raw
            !initialTitle.isNullOrBlank() -> initialTitle.trim()
            else -> spec.heroTitle
        }
    }
    val canActivate = values[spec.defaultTitleKey]?.trim()?.isNotEmpty() == true ||
        !initialTitle.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(theme.canvasTop.copy(alpha = 0.85f), theme.canvas, theme.canvas),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = BusinessTokens.Colors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onBack)
                        .padding(top = 4.dp),
                )
                Text(
                    text = family.title,
                    color = BusinessTokens.Colors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                if (draftMomentStatus.equals("DRAFT", ignoreCase = true) && draftMomentId != null) {
                    Text(
                        text = "Discard draft",
                        color = BusinessTokens.Colors.SignalDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clickable(enabled = !createState.submitting) {
                                val id = draftMomentId ?: return@clickable
                                createViewModel.discardMomentDraft(id) {
                                    draftMomentId = null
                                    draftMomentStatus = null
                                    onBack()
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }

            BusinessSetupDocumentKit.HeroCard(
                theme = theme,
                eyebrow = spec.eyebrow,
                title = spec.heroTitle,
                subtitle = spec.heroSubtitle,
            )

            if (draftMomentStatus.equals("DRAFT", ignoreCase = true)) {
                Text(
                    text = "Setup in progress",
                    color = theme.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            BusinessSetupDocumentKit.SubtypeChips(
                theme = theme,
                chips = family.chips,
                selected = typeCode,
                onSelect = { code ->
                    val priorTitle = values[spec.defaultTitleKey]
                    typeCode = code
                    reseed(code, priorTitle)
                },
            )

            val labelIndex = remember(spec) {
                spec.sections.flatMap { s -> s.fields.map { it.key to it.label } }.toMap()
            }
            val gstRegistered = values["gstRegistered"] ?: values["gstTracking"] ?: "Yes"
            val gstOff = gstRegistered == "No" ||
                gstRegistered.equals("false", ignoreCase = true) ||
                gstRegistered.equals("Not now", ignoreCase = true)
            val gstDependent = setOf(
                "gstType", "gstState", "gstin", "filingFrequency",
                "gstRegistration", "salePriceIncludesGst", "gstEffectiveFrom",
            )
            spec.sections.forEachIndexed { index, section ->
                BusinessSetupDocumentKit.SectionCard(
                    theme = theme,
                    index = index + 1,
                    title = section.title,
                    subtitle = section.subtitle,
                ) {
                    section.fields.forEach { field ->
                        val hideGst = field.key in gstDependent && gstOff
                        if (hideGst) return@forEach
                        when (val kind = field.kind) {
                            is BusinessSetupFieldSpec.Kind.PanelHeader -> {
                                BusinessSetupDocumentKit.PanelHeader(theme = theme, title = kind.title)
                            }
                            is BusinessSetupFieldSpec.Kind.Text -> {
                                BusinessSetupDocumentKit.FieldRow(
                                    theme = theme,
                                    label = field.label,
                                    placeholder = field.placeholder,
                                    value = values[field.key].orEmpty(),
                                    onValueChange = {
                                        values[field.key] = it
                                        fieldErrors.remove(field.key)
                                    },
                                    helper = field.helper,
                                    error = fieldErrors[field.key],
                                )
                            }
                            is BusinessSetupFieldSpec.Kind.Choice -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = field.label,
                                        color = theme.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    BusinessSetupDocumentKit.ChoiceChips(
                                        theme = theme,
                                        options = kind.options,
                                        selected = values[field.key].orEmpty(),
                                        onSelect = {
                                            values[field.key] = it
                                            fieldErrors.remove(field.key)
                                        },
                                    )
                                    if (field.helper.isNotBlank() && fieldErrors[field.key].isNullOrBlank()) {
                                        Text(
                                            text = field.helper,
                                            color = theme.hint,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                            is BusinessSetupFieldSpec.Kind.MultiChoice -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = field.label,
                                        color = theme.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    BusinessSetupDocumentKit.MultiChoiceChips(
                                        theme = theme,
                                        options = kind.options,
                                        selectedPipe = values[field.key].orEmpty(),
                                        onChange = {
                                            values[field.key] = it
                                            fieldErrors.remove(field.key)
                                        },
                                        error = fieldErrors[field.key],
                                        helper = field.helper,
                                    )
                                }
                            }
                            is BusinessSetupFieldSpec.Kind.Toggle -> {
                                BusinessSetupDocumentKit.ToggleRow(
                                    theme = theme,
                                    label = field.label,
                                    value = values[field.key].orEmpty().ifEmpty { "Yes" },
                                    onChange = { values[field.key] = it },
                                )
                            }
                            is BusinessSetupFieldSpec.Kind.Date -> {
                                BusinessSetupDocumentKit.DateRow(
                                    theme = theme,
                                    label = field.label,
                                    placeholder = field.placeholder,
                                    value = values[field.key].orEmpty(),
                                    onValueChange = {
                                        values[field.key] = it
                                        fieldErrors.remove(field.key)
                                    },
                                    helper = field.helper,
                                    error = fieldErrors[field.key],
                                )
                            }
                            is BusinessSetupFieldSpec.Kind.RowAction -> {
                                BusinessSetupDocumentKit.RowAction(
                                    theme = theme,
                                    label = field.label,
                                    action = kind.action,
                                )
                            }
                            is BusinessSetupFieldSpec.Kind.MemberList -> {
                                val stored = values[field.key].orEmpty()
                                val list = if (stored.isBlank()) {
                                    kind.members
                                } else {
                                    stored.split("|").filter { it.isNotBlank() }
                                }
                                BusinessSetupDocumentKit.MemberList(theme = theme, members = list)
                            }
                            is BusinessSetupFieldSpec.Kind.ModuleList -> {
                                BusinessSetupDocumentKit.ModuleList(
                                    theme = theme,
                                    modules = kind.modules,
                                    helper = field.helper,
                                )
                            }
                            is BusinessSetupFieldSpec.Kind.ReviewSummary -> {
                                BusinessSetupDocumentKit.ReviewSummary(
                                    theme = theme,
                                    keys = kind.keys,
                                    values = values.toMap(),
                                    labelsByKey = labelIndex,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (section.id == "people") {
                        BusinessSetupInviteSection(
                            theme = theme,
                            companyId = companyId,
                            mintInvite = { id -> createViewModel.mintCompanyInvite(id) },
                            embedded = true,
                        )
                    }
                    if (
                        section.id == "channels" &&
                        typeCode in setOf("RETAIL_STORE", "DISTRIBUTION_DEALERSHIP", "TRADING_WHOLESALE")
                    ) {
                        val status = OperatingSetupPayloadBuilder.planStatusMessage(values.toMap())
                        Text(
                            text = "●  ${status.second}",
                            color = if (status.first) {
                                BusinessTokens.Colors.SignalPositive
                            } else {
                                theme.accent
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (status.first) {
                                        BusinessTokens.Colors.SignalPositive.copy(alpha = 0.14f)
                                    } else {
                                        theme.accent.copy(alpha = 0.08f)
                                    },
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        fun preferencesPayload(): Map<String, Any> = values
            .mapNotNull { (k, v) ->
                val trimmed = v.trim()
                if (trimmed.isEmpty()) null else k to (trimmed as Any)
            }
            .toMap()

        BusinessSetupDocumentKit.StickyActivateBar(
            theme = theme,
            label = spec.activateLabel,
            enabled = canActivate,
            submitting = createState.submitting,
            onActivate = {
                fieldErrors.clear()
                createViewModel.submitBusinessSetup(
                    familyCode = family.familyCode,
                    companyId = companyId,
                    preferences = preferencesPayload(),
                    title = titleValue,
                    subtype = typeCode,
                    editingMomentId = draftMomentId,
                    editingMomentStatus = draftMomentStatus,
                    status = "ACTIVE",
                    companyName = if (companyId.isNullOrBlank()) titleValue else null,
                    onSuccess = onCreated,
                    onFieldErrors = { errors ->
                        fieldErrors.clear()
                        fieldErrors.putAll(errors)
                    },
                )
            },
            draftEnabled = canActivate,
            savingDraft = savingDraft,
            onSaveDraft = {
                savingDraft = true
                draftSavedMessage = null
                fieldErrors.clear()
                val existingId = draftMomentId
                if (existingId != null && draftMomentStatus.equals("DRAFT", ignoreCase = true)) {
                    createViewModel.saveBusinessSetupDraft(
                        momentId = existingId,
                        preferences = preferencesPayload(),
                        title = titleValue,
                        onComplete = { ok ->
                            savingDraft = false
                            draftSavedMessage = if (ok) "Draft saved" else "Could not save draft"
                        },
                    )
                } else {
                    createViewModel.submitBusinessSetup(
                        familyCode = family.familyCode,
                        companyId = companyId,
                        preferences = preferencesPayload(),
                        title = titleValue,
                        subtype = typeCode,
                        editingMomentId = draftMomentId,
                        editingMomentStatus = draftMomentStatus,
                        status = "DRAFT",
                        companyName = if (companyId.isNullOrBlank()) titleValue else null,
                        onSuccess = { outcome ->
                            savingDraft = false
                            draftMomentId = outcome.momentId
                            draftMomentStatus = "DRAFT"
                            draftSavedMessage = "Draft saved"
                        },
                        onFieldErrors = { /* draft is lenient */ },
                    )
                }
            },
            onScheduleLater = onBack,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        val banner = draftSavedMessage ?: createState.error
        banner?.let { err ->
            Text(
                text = err,
                color = if (draftSavedMessage != null) {
                    theme.accent
                } else {
                    BusinessTokens.Colors.SignalDanger
                },
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .padding(horizontal = 20.dp),
            )
        }
    }
}
