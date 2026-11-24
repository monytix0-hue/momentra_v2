package com.example.momentra.ui.shell.business.setup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.create.MomentCreateViewModel
import com.example.momentra.ui.shell.business.shared.IndiaStateCatalog
import com.example.momentra.ui.shell.business.shared.IndiaStateSheet
import com.example.momentra.ui.shell.business.shared.OperatingSheetTheme
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * Chrome + subtype chips + active setup document. Activate → `submitBusinessSetup`.
 * Keep in lockstep with `BusinessSetupShell.swift`.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    // Keys the user has edited through a control. Draft prefill must never clobber these.
    val userEditedKeys = remember { mutableSetOf<String>() }
    var didSeed by remember { mutableStateOf(false) }
    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    var savingDraft by remember { mutableStateOf(false) }
    var draftMomentId by remember { mutableStateOf(editingMomentId) }
    var draftMomentStatus by remember { mutableStateOf(editingMomentStatus) }
    var draftSavedMessage by remember { mutableStateOf<String?>(null) }
    var logoPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var logoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var logoUploading by remember { mutableStateOf(false) }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    var logoError by remember { mutableStateOf<String?>(null) }
    var linkedCompanyId by remember { mutableStateOf(companyId) }
    var showGstStatePicker by remember { mutableStateOf(false) }
    val createState by createViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme = remember(typeCode) { BusinessSetupTheme.forTypeCode(typeCode) }
    val spec = remember(typeCode) { BusinessSetupDocuments.spec(typeCode) }
    // Shell post-pick upload wiring — callers: HeroCard onLogoClick → logoPicker.
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = decodeSetupLogo(context, uri)
        if (picked == null) {
            logoError = "Could not open that photo"
            return@rememberLauncherForActivityResult
        }
        logoBytes = picked.bytes
        logoPreview = picked.preview
        logoError = null
        scope.launch {
            logoUploading = true
            try {
                val ensured = ensureDraftMomentForLogo(
                    draftMomentId = draftMomentId,
                    linkedCompanyId = linkedCompanyId,
                    setDraft = { id, company ->
                        draftMomentId = id
                        draftMomentStatus = "DRAFT"
                        if (!company.isNullOrBlank()) linkedCompanyId = company
                    },
                    createViewModel = createViewModel,
                    family = family,
                    preferences = {
                        values.mapNotNull { (k, v) ->
                            val trimmed = v.trim()
                            if (trimmed.isEmpty()) null else k to (trimmed as Any)
                        }.toMap()
                    },
                    titleValue = {
                        val raw = values[spec.defaultTitleKey]?.trim().orEmpty()
                        when {
                            raw.isNotEmpty() -> raw
                            !initialTitle.isNullOrBlank() -> initialTitle.trim()
                            else -> spec.heroTitle
                        }
                    },
                    typeCode = typeCode,
                    draftMomentStatus = draftMomentStatus,
                )
                if (ensured == null) {
                    logoError = "Could not upload logo"
                    return@launch
                }
                if (!ensured.companyId.isNullOrBlank()) {
                    linkedCompanyId = ensured.companyId
                }
                val companyName = values["companyName"]?.trim().orEmpty().ifEmpty {
                    values[spec.defaultTitleKey]?.trim().orEmpty().ifEmpty {
                        initialTitle?.trim().orEmpty().ifEmpty { spec.heroTitle }
                    }
                }
                createViewModel.uploadSetupLogo(
                    companyId = linkedCompanyId ?: ensured.companyId,
                    momentId = ensured.momentId,
                    companyName = companyName,
                    bytes = picked.bytes,
                    contentType = "image/jpeg",
                ).fold(
                    onSuccess = { result ->
                        linkedCompanyId = result.companyId
                        values["logoMediaId"] = result.mediaId
                        logoUrl = result.downloadUrl
                        logoError = null
                    },
                    onFailure = {
                        logoError = "Could not upload logo"
                    },
                )
            } finally {
                logoUploading = false
            }
        }
    }

    // Record a user edit so async draft prefill cannot overwrite the field.
    fun editField(key: String, value: String) {
        values[key] = value
        userEditedKeys.add(key)
    }

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
        values["logoMediaId"]?.takeIf { it.isNotBlank() }?.let { seeded["logoMediaId"] = it }
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
                    val loaded: String? = when (raw) {
                        is String -> raw
                        is List<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                            .joinToString("|")
                        is Number, is Boolean -> raw.toString()
                        else -> null
                    }
                    // Guard: never clobber a key the user has already typed into, and only fill
                    // fields that are still empty. logoMediaId keeps its existing preservation path.
                    if (BusinessSetupPrefill.shouldApply(
                            key = key,
                            dirty = userEditedKeys,
                            loaded = loaded,
                            current = values[key],
                        )
                    ) {
                        loaded?.let { values[key] = it }
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
                .imeNestedScroll()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

            val heroTitleLive = BusinessSetupHeroTitle.format(
                name = values[spec.defaultTitleKey]?.trim().orEmpty()
                    .ifEmpty { initialTitle?.trim().orEmpty() },
                placeholder = spec.heroTitle,
            )
            BusinessSetupDocumentKit.HeroCard(
                theme = theme,
                eyebrow = spec.eyebrow,
                title = heroTitleLive,
                subtitle = spec.heroSubtitle,
                logoBitmap = logoPreview,
                logoUrl = logoUrl,
                logoUploading = logoUploading,
                onLogoClick = {
                    logoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
            if (!logoError.isNullOrBlank()) {
                Text(
                    text = logoError.orEmpty(),
                    color = BusinessTokens.Colors.SignalDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            }

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
                                if (field.key == "gstState") {
                                    val selected = values[field.key].orEmpty()
                                    val code = IndiaStateCatalog.codeOf(selected)
                                    val display = when {
                                        selected.isBlank() -> ""
                                        code.isNotBlank() ->
                                            "${IndiaStateCatalog.displayName(selected)} · $code"
                                        else -> selected
                                    }
                                    BusinessSetupDocumentKit.PickerRow(
                                        theme = theme,
                                        label = field.label,
                                        value = display,
                                        placeholder = field.placeholder.ifBlank { "Select state" },
                                        helper = field.helper,
                                        error = fieldErrors[field.key],
                                        onClick = { showGstStatePicker = true },
                                    )
                                } else {
                                    BusinessSetupDocumentKit.FieldRow(
                                        theme = theme,
                                        label = field.label,
                                        placeholder = field.placeholder,
                                        value = values[field.key].orEmpty(),
                                        onValueChange = {
                                            editField(field.key, it)
                                            fieldErrors.remove(field.key)
                                        },
                                        helper = field.helper,
                                        error = fieldErrors[field.key],
                                        forceUppercase = field.key == "gstin",
                                    )
                                }
                            }
                            is BusinessSetupFieldSpec.Kind.Choice -> {
                                if (field.key == "currency") {
                                    BusinessSetupDocumentKit.CurrencyDropdown(
                                        theme = theme,
                                        label = field.label,
                                        options = kind.options,
                                        selected = values[field.key].orEmpty().ifEmpty {
                                            kind.options.firstOrNull().orEmpty()
                                        },
                                        onSelect = {
                                            editField(field.key, it)
                                            fieldErrors.remove(field.key)
                                        },
                                        helper = field.helper,
                                        error = fieldErrors[field.key],
                                    )
                                } else {
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
                                                editField(field.key, it)
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
                                            editField(field.key, it)
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
                                    onChange = { editField(field.key, it) },
                                )
                            }
                            is BusinessSetupFieldSpec.Kind.Date -> {
                                BusinessSetupDocumentKit.DateRow(
                                    theme = theme,
                                    label = field.label,
                                    placeholder = field.placeholder,
                                    value = values[field.key].orEmpty(),
                                    onValueChange = {
                                        editField(field.key, it)
                                        fieldErrors.remove(field.key)
                                    },
                                    helper = field.helper,
                                    error = fieldErrors[field.key],
                                    fieldKey = field.key,
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
                    }
                    if (section.id == "people") {
                        BusinessSetupInviteSection(
                            theme = theme,
                            companyId = linkedCompanyId,
                            mintInvite = { id -> createViewModel.mintCompanyInvite(id) },
                            embedded = true,
                            inviteTitle = titleValue,
                            resolveCompanyId = {
                                val ensured = ensureDraftMomentForLogo(
                                    draftMomentId = draftMomentId,
                                    linkedCompanyId = linkedCompanyId,
                                    setDraft = { id, company ->
                                        draftMomentId = id
                                        draftMomentStatus = "DRAFT"
                                        if (!company.isNullOrBlank()) linkedCompanyId = company
                                    },
                                    createViewModel = createViewModel,
                                    family = family,
                                    preferences = {
                                        values.mapNotNull { (k, v) ->
                                            val trimmed = v.trim()
                                            if (trimmed.isEmpty()) null else k to (trimmed as Any)
                                        }.toMap()
                                    },
                                    titleValue = { titleValue },
                                    typeCode = typeCode,
                                    draftMomentStatus = draftMomentStatus,
                                )
                                val company = ensured?.companyId?.takeIf { it.isNotBlank() }
                                    ?: linkedCompanyId?.takeIf { it.isNotBlank() }
                                if (!company.isNullOrBlank()) linkedCompanyId = company
                                company
                            },
                        )
                    }
                    if (
                        section.id == "channels" &&
                        typeCode in setOf(
                            "RETAIL_STORE",
                            "DISTRIBUTION_DEALERSHIP",
                            "TRADING_WHOLESALE",
                            "CLIENT_CAMPAIGN",
                        )
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
                    companyId = linkedCompanyId,
                    preferences = preferencesPayload(),
                    title = titleValue,
                    subtype = typeCode,
                    editingMomentId = draftMomentId,
                    editingMomentStatus = draftMomentStatus,
                    status = "ACTIVE",
                    companyName = if (linkedCompanyId.isNullOrBlank()) titleValue else null,
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
                        companyId = linkedCompanyId,
                        preferences = preferencesPayload(),
                        title = titleValue,
                        subtype = typeCode,
                        editingMomentId = draftMomentId,
                        editingMomentStatus = draftMomentStatus,
                        status = "DRAFT",
                        companyName = if (linkedCompanyId.isNullOrBlank()) titleValue else null,
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

        if (showGstStatePicker) {
            val sheetTheme = OperatingSheetTheme(
                accent = theme.accent,
                accentSoft = theme.accentSoft,
                canvas = theme.canvas,
                surface = theme.surface,
                inset = theme.inset,
                border = Color(0xFF1E293B),
                ink = BusinessTokens.Colors.TextPrimary,
                body = theme.label,
                warning = Color(0xFFF59E0B),
                negative = Color(0xFFF87171),
            )
            IndiaStateSheet(
                selected = values["gstState"].orEmpty(),
                theme = sheetTheme,
                onDismiss = { showGstStatePicker = false },
                onPick = { state ->
                    editField("gstState", state.name)
                    fieldErrors.remove("gstState")
                    showGstStatePicker = false
                },
            )
        }
    }
}

private data class DraftEnsureForLogo(
    val momentId: String,
    val companyId: String?,
)

/**
 * Prefer in-session DRAFT create via submitBusinessSetup(status=DRAFT) when draftMomentId is null
 * so upload can complete without forcing Save draft first.
 * Resolves the moment's linked companyId into Shell so uploadSetupLogo never creates a second company.
 * Fail-fast when create is already submitting. Wait for submitting/callback before any
 * long ceiling — do not treat "never submitted yet" as failure at ~2s (viewModelScope race).
 */
private suspend fun ensureDraftMomentForLogo(
    draftMomentId: String?,
    linkedCompanyId: String?,
    setDraft: (momentId: String, companyId: String?) -> Unit,
    createViewModel: MomentCreateViewModel,
    family: BusinessSetupFamily,
    preferences: () -> Map<String, Any>,
    titleValue: () -> String,
    typeCode: String,
    draftMomentStatus: String?,
): DraftEnsureForLogo? {
    draftMomentId?.takeIf { it.isNotBlank() }?.let { existing ->
        val company = SetupLogoCompanyResolver.preferExisting(
            linkedCompanyId,
            createViewModel.getDomainSetupPrefill(existing)?.companyId,
        )
        return DraftEnsureForLogo(existing, company)
    }
    // Fail fast: submitBusinessSetup early-returns when already submitting (no callbacks).
    if (createViewModel.state.value.submitting) return null

    val deferred = CompletableDeferred<String?>()
    createViewModel.submitBusinessSetup(
        familyCode = family.familyCode,
        companyId = linkedCompanyId,
        preferences = preferences(),
        title = titleValue(),
        subtype = typeCode,
        editingMomentId = null,
        editingMomentStatus = draftMomentStatus,
        status = "DRAFT",
        companyName = if (linkedCompanyId.isNullOrBlank()) titleValue() else null,
        onSuccess = { outcome ->
            deferred.complete(outcome.momentId)
        },
        onFieldErrors = {
            deferred.complete(null)
        },
    )
    // Phase 1: wait for viewModelScope to start (submitting=true) OR callback/error.
    // submitting is set only after async build inside the launch — do not false-fail at ~2s.
    val launchStarted = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
        while (!deferred.isCompleted) {
            val st = createViewModel.state.value
            if (st.submitting) return@withTimeoutOrNull true
            if (st.error != null) {
                deferred.complete(null)
                return@withTimeoutOrNull false
            }
            kotlinx.coroutines.delay(20)
        }
        // Completed via onSuccess/onFieldErrors (e.g. validation) before submitting flipped.
        false
    }
    if (launchStarted == null && !deferred.isCompleted) {
        // Launch never observed and no callback — true hung / silent no-op.
        deferred.complete(null)
    }
    // Phase 2: longer ceiling only after submitting was observed (or already completed).
    if (!deferred.isCompleted && launchStarted == true) {
        val timedOut = kotlinx.coroutines.withTimeoutOrNull(30_000L) {
            while (!deferred.isCompleted) {
                kotlinx.coroutines.delay(50)
                val st = createViewModel.state.value
                if (!st.submitting && st.error != null) {
                    deferred.complete(null)
                }
            }
            true
        }
        if (timedOut == null) deferred.complete(null)
    }
    val momentId = deferred.await() ?: return null
    val company = createViewModel.getDomainSetupPrefill(momentId)?.companyId?.takeIf { it.isNotBlank() }
    setDraft(momentId, company)
    return DraftEnsureForLogo(momentId, company)
}

private data class SetupLogoPick(val preview: ImageBitmap, val bytes: ByteArray)

/** Mirror of PersonalExperienceHost.decodePhoto — JPEG/PNG bytes + preview bitmap. */
private fun decodeSetupLogo(context: android.content.Context, uri: Uri): SetupLogoPick? = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
    val largest = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val scaled = if (largest <= 1600) {
        bitmap
    } else {
        val ratio = 1600f / largest
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }
    val bytes = ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
    SetupLogoPick(scaled.asImageBitmap(), bytes)
}.getOrNull()
