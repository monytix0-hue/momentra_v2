package com.example.momentra.ui.shell.empty.business

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CreateCompanyBody
import com.example.momentra.data.api.CreateLocationBody
import com.example.momentra.data.api.MintCompanyInviteBody
import com.example.momentra.data.repository.BusinessSliceRepository
import com.example.momentra.data.repository.MomentCreateRepository
import com.example.momentra.domain.CompanySummary
import com.example.momentra.ui.shell.business.shared.BusinessAudience
import com.example.momentra.ui.shell.business.shared.CompanyModules
import com.example.momentra.ui.shell.business.shared.IndustryTemplate
import com.example.momentra.ui.shell.business.shared.IndustryTemplateCatalog
import com.example.momentra.ui.shell.empty.business.BusinessSetupCatalog
import com.google.firebase.auth.FirebaseAuth
import com.example.momentra.ui.shell.empty.group.InviteSendChooserDialog
import com.example.momentra.ui.shell.empty.group.PendingInviteSend
import com.example.momentra.ui.shell.empty.group.inviteMessage
import com.example.momentra.ui.shell.empty.group.looksLikeInvitePhone
import com.example.momentra.ui.shell.empty.group.sendInviteSms
import com.example.momentra.ui.shell.empty.group.sendInviteWhatsApp
import java.util.UUID
import kotlinx.coroutines.launch

private val CoBg = Color(0xFF0C0F15)
private val CoAccent = Color(0xFF818CF8)
private val CoCard = Color(0xFF161B26)
private val CoBorder = Color(0xFF1E293B)
private val CoMuted = Color(0xFF94A3B8)
private val CoDim = Color(0xFF64748B)
private val CoGreen = Color(0xFF10B981)
private val CoAmber = Color(0xFFF59E0B)

private val CoIndustryOptions = listOf(
    "Retail / Kirana",
    "Pet store / Specialty",
    "Manufacturing / Workshop",
    "Restaurant / F&B",
    "Fashion / Apparel",
    "Services",
    "Wholesale",
    "Technology & Software",
    "E-commerce",
    "Other",
)

private val CoSizeOptions = listOf("Solo (1)", "Small (2-25)", "Medium (26-100)")

private val CoCurrencyOptions = listOf(
    "₹ INR — Indian Rupee",
    "$ USD — US Dollar",
    "€ EUR — Euro",
    "£ GBP — British Pound",
    "د.إ AED — UAE Dirham",
    "S$ SGD — Singapore Dollar",
)

private val CoTimezoneOptions = listOf(
    "IST (UTC+5:30)",
    "UTC (UTC+0)",
    "EST (UTC-5)",
    "PST (UTC-8)",
    "GST (UTC+4)",
)

private fun coTimezoneToIana(label: String): String = when {
    label.contains("IST", ignoreCase = true) -> "Asia/Kolkata"
    label.contains("EST", ignoreCase = true) -> "America/New_York"
    label.contains("PST", ignoreCase = true) -> "America/Los_Angeles"
    label.contains("GST", ignoreCase = true) -> "Asia/Dubai"
    else -> "UTC"
}

private data class CoLocation(
    val name: String,
    val area: String,
    val primary: Boolean,
    val accent: Color,
)

private fun signedInOwnerName(): String {
    val user = FirebaseAuth.getInstance().currentUser
    return user?.displayName?.trim().takeUnless { it.isNullOrEmpty() }
        ?: user?.email?.substringBefore("@")?.trim().takeUnless { it.isNullOrEmpty() }
        ?: "You"
}

private fun memberFromName(name: String) = CoMember(
    initials = name.take(2).uppercase(),
    name = name,
    role = "Member",
    scope = "All Locations",
    color = CoAccent,
)

private data class CoMember(
    val initials: String,
    val name: String,
    val role: String,
    val scope: String,
    val color: Color,
    val you: Boolean = false,
)

/**
 * Figma 695:4455 Company Setup — steps 692:38403 / 38453 / 38549 / 38635.
 */
@Composable
fun CompanySetupContent(
    onClose: () -> Unit,
    onActivated: (CompanySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(1) }
    var companyName by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("Retail / Kirana") }
    var companySize by remember { mutableStateOf("Solo (1)") }
    var entityType by remember { mutableStateOf("Sole Prop") }
    var gstin by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("₹ INR — Indian Rupee") }
    var fyCycle by remember { mutableStateOf("Apr-Mar") }
    var timezone by remember { mutableStateOf("IST (UTC+5:30)") }
    var audience by remember { mutableStateOf(com.example.momentra.ui.shell.business.shared.BusinessAudience.SMALL_SHOP) }
    var industryTemplate by remember { mutableStateOf(IndustryTemplateCatalog.KIRANA) }
    var templateManuallyPicked by remember { mutableStateOf(false) }
    var sellWhat by remember { mutableStateOf("Physical items") }
    var billHow by remember { mutableStateOf("Counter billing") }
    var customMoney by remember { mutableStateOf(true) }
    var customDaily by remember { mutableStateOf(true) }
    var customTeam by remember { mutableStateOf(false) }
    var structure by remember { mutableStateOf("Single Location") }
    val locations = remember { mutableStateListOf<CoLocation>() }
    val members = remember {
        val signedIn = signedInOwnerName()
        mutableStateListOf(
            CoMember(
                initials = signedIn.take(2).uppercase(),
                name = signedIn,
                role = "Owner",
                scope = "All Locations",
                color = CoAccent,
                you = true,
            ),
        )
    }
    var inviteText by remember { mutableStateOf("") }
    var logoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var logoPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var logoError by remember { mutableStateOf<String?>(null) }
    var locationEditor by remember { mutableStateOf<Int?>(null) }
    var locationEditorOpen by remember { mutableStateOf(false) }
    var locationNameDraft by remember { mutableStateOf("") }
    var locationAreaDraft by remember { mutableStateOf("") }
    var memberEditor by remember { mutableStateOf<Int?>(null) }
    var memberNameDraft by remember { mutableStateOf("") }
    var activating by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var activateError by remember { mutableStateOf<String?>(null) }
    var activateWarning by remember { mutableStateOf<String?>(null) }
    var showJoinCode by remember { mutableStateOf(false) }
    var pendingActivation by remember { mutableStateOf<CompanySummary?>(null) }
    var pendingInviteSend by remember { mutableStateOf<PendingInviteSend?>(null) }
    var showAddPeople by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        MomentraAnalytics.get().onScreenEnter(AnalyticsScreens.COMPANY_SETUP)
        onDispose { MomentraAnalytics.get().onScreenExit(AnalyticsScreens.COMPANY_SETUP) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CoBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoHeader(step = step, onClose = onClose)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                if (forward) {
                    (slideInHorizontally(animationSpec = tween(320, easing = CoFigmaEase)) { it / 4 } +
                        fadeIn(tween(320, easing = CoFigmaEase))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(240, easing = CoFigmaEase)) { -it / 6 } +
                            fadeOut(tween(240)))
                } else {
                    (slideInHorizontally(animationSpec = tween(320, easing = CoFigmaEase)) { -it / 4 } +
                        fadeIn(tween(320, easing = CoFigmaEase))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(240, easing = CoFigmaEase)) { it / 6 } +
                            fadeOut(tween(240)))
                }
            },
            label = "companySetupStep",
            modifier = Modifier.fillMaxWidth(),
        ) { current ->
            when (current) {
            1 -> CoWelcome(
                onGetStarted = { step = 2 },
                onHaveCode = { showJoinCode = true },
            )
            2 -> CoCompanyForm(
                companyName = companyName,
                onCompanyName = {
                    companyName = it
                    if (nameError != null && it.isNotBlank()) nameError = null
                },
                nameError = nameError,
                industry = industry,
                onIndustry = { industry = it },
                companySize = companySize,
                onCompanySize = { companySize = it },
                entityType = entityType,
                onEntityType = { entityType = it },
                gstin = gstin,
                onGstin = { gstin = it },
                currency = currency,
                onCurrency = { currency = it },
                fyCycle = fyCycle,
                onFyCycle = { fyCycle = it },
                timezone = timezone,
                onTimezone = { timezone = it },
                audience = audience,
                onAudience = { next ->
                    audience = next
                    if (com.example.momentra.ui.shell.business.shared.BusinessAudience.isSmallShop(next)) {
                        if (structure == "Multi-Location" || structure == "Multi-Unit") {
                            structure = "Single Location"
                        }
                        industry = "Retail / Kirana"
                        companySize = "Solo (1)"
                        entityType = "Sole Prop"
                        industryTemplate = IndustryTemplateCatalog.KIRANA
                        templateManuallyPicked = false
                    } else {
                        industry = "Technology & Software"
                        companySize = "Small (2-25)"
                        entityType = "Pvt Ltd"
                    }
                },
                industryTemplate = industryTemplate,
                onIndustryTemplate = { tpl ->
                    industryTemplate = tpl
                    templateManuallyPicked = true
                    tpl.profileDefaults["industry"]?.toString()?.let { industry = it }
                },
                sellWhat = sellWhat,
                onSellWhat = { answer ->
                    sellWhat = answer
                    if (!templateManuallyPicked) {
                        industryTemplate = IndustryTemplateCatalog.suggest(answer, billHow)
                        industryTemplate.profileDefaults["industry"]?.toString()?.let { industry = it }
                    }
                },
                billHow = billHow,
                onBillHow = { answer ->
                    billHow = answer
                    if (!templateManuallyPicked) {
                        industryTemplate = IndustryTemplateCatalog.suggest(sellWhat, answer)
                        industryTemplate.profileDefaults["industry"]?.toString()?.let { industry = it }
                    }
                },
                customMoney = customMoney,
                onCustomMoney = { customMoney = it },
                customDaily = customDaily,
                onCustomDaily = { customDaily = it },
                customTeam = customTeam,
                onCustomTeam = { customTeam = it },
                logoPreview = logoPreview,
                logoError = logoError,
                onLogoPicked = { bytes, preview ->
                    logoBytes = bytes
                    logoPreview = preview
                    logoError = if (bytes != null && preview == null) "Could not open that photo" else null
                },
                onContinue = { step = 3 },
                onBack = { step = 1 },
            )
            3 -> CoLocationsForm(
                structure = structure,
                onStructure = { structure = it },
                audience = audience,
                locations = locations,
                currency = currency,
                onAddLocation = {
                    locationEditor = null
                    locationNameDraft = ""
                    locationAreaDraft = ""
                    locationEditorOpen = true
                },
                onEditLocation = { index ->
                    val loc = locations.getOrNull(index) ?: return@CoLocationsForm
                    locationEditor = index
                    locationNameDraft = loc.name
                    locationAreaDraft = loc.area
                    locationEditorOpen = true
                },
                onContinue = { step = 4 },
                onBack = { step = 2 },
            )
            else -> CoLaunchForm(
                companyName = companyName,
                members = members,
                inviteText = inviteText,
                onInviteText = { inviteText = it },
                onAddInvite = {
                    val typed = inviteText.trim()
                    if (typed.isNotEmpty()) {
                        members.add(memberFromName(typed))
                        inviteText = ""
                    } else {
                        showAddPeople = true
                    }
                },
                onEditMember = { index ->
                    val member = members.getOrNull(index) ?: return@CoLaunchForm
                    if (member.you) return@CoLaunchForm
                    memberEditor = index
                    memberNameDraft = member.name
                },
                onDeleteMember = { index ->
                    val member = members.getOrNull(index) ?: return@CoLaunchForm
                    if (!member.you) members.removeAt(index)
                },
                logoError = logoError,
                nameError = nameError,
                activateError = activateError,
                activateWarning = activateWarning,
                nextSteps = coLaunchNextSteps(
                    audience = audience,
                    template = industryTemplate,
                    customMoney = customMoney,
                    customDaily = customDaily,
                    customTeam = customTeam,
                ),
                activating = activating,
                onActivate = {
                    if (activating) return@CoLaunchForm
                    val trimmedName = companyName.trim()
                    if (trimmedName.isEmpty()) {
                        nameError = "Enter a company name to activate"
                        step = 2
                        return@CoLaunchForm
                    }
                    val smallShop = BusinessAudience.isSmallShop(audience)
                    val customMods = mapOf(
                        "money" to customMoney,
                        "dailyBusiness" to customDaily,
                        "teamOps" to customTeam,
                    )
                    if (smallShop && industryTemplate.isCustom) {
                        val kinds = IndustryTemplateCatalog.setupKindsFor(industryTemplate, customMods)
                        if (kinds.isEmpty()) {
                            activateError = "Turn on at least one moment (Money, Daily Business, or Team)"
                            return@CoLaunchForm
                        }
                    }
                    nameError = null
                    activateError = null
                    activateWarning = null
                    activating = true
                    scope.launch {
                        try {
                            val tz = coTimezoneToIana(timezone)
                            val template = if (smallShop) industryTemplate else null
                            val modules = template?.let {
                                IndustryTemplateCatalog.modulesFor(it, customMods)
                            } ?: mapOf(
                                "money" to true,
                                "dailyBusiness" to true,
                                "teamOps" to true,
                                "vendors" to true,
                            )
                            val profile = mutableMapOf<String, Any>(
                                "industry" to industry,
                                "companySize" to companySize,
                                "currency" to currency,
                                "financialYear" to fyCycle,
                                "structure" to structure,
                                "audience" to audience,
                                "settings" to mapOf("modules" to modules),
                            )
                            if (template != null) {
                                profile[IndustryTemplateCatalog.PROFILE_KEY] = template.id
                                template.profileDefaults.forEach { (k, v) ->
                                    if (k != "industry" || industry.isBlank()) {
                                        profile.putIfAbsent(k, v)
                                    }
                                }
                            }
                            val created = ApiClient.apiService.createCompany(
                                idempotencyKey = UUID.randomUUID().toString(),
                                body = CreateCompanyBody(
                                    displayName = trimmedName,
                                    legalName = trimmedName,
                                    timezone = tz,
                                    companyType = entityType,
                                    taxIdentifier = gstin.ifBlank { null },
                                    profileJson = profile,
                                ),
                            ).data
                            BusinessAudience.saveForCompany(
                                context,
                                created.companyId,
                                audience,
                            )
                            CompanyModules.saveModules(context, created.companyId, modules)
                            IndustryTemplateCatalog.hubHintFromProfile(profile)?.let {
                                IndustryTemplateCatalog.saveHubHint(context, created.companyId, it)
                            }
                            for (loc in locations) {
                                runCatching {
                                    ApiClient.apiService.createLocation(
                                        companyId = created.companyId,
                                        idempotencyKey = UUID.randomUUID().toString(),
                                        body = CreateLocationBody(
                                            name = loc.name,
                                            addressText = loc.area,
                                            timezone = tz,
                                        ),
                                    )
                                }
                            }
                            val softWarnings = mutableListOf<String>()
                            val bytes = logoBytes
                            if (bytes != null) {
                                val uploaded = BusinessSliceRepository().uploadCompanyLogo(
                                    companyId = created.companyId,
                                    bytes = bytes,
                                )
                                uploaded.fold(
                                    onSuccess = { mediaId ->
                                        runCatching {
                                            ApiClient.apiService.patchCompany(
                                                companyId = created.companyId,
                                                idempotencyKey = UUID.randomUUID().toString(),
                                                body = mapOf(
                                                    "expectedVersion" to created.version,
                                                    "profileJson" to mapOf("logoMediaId" to mediaId),
                                                ),
                                            )
                                        }.onFailure {
                                            softWarnings += "Logo could not be saved"
                                            logoError = it.message ?: "Could not save logo"
                                        }
                                    },
                                    onFailure = {
                                        softWarnings += "Logo could not be uploaded"
                                        logoError = it.message ?: "Could not upload logo"
                                    },
                                )
                            }
                            if (template != null) {
                                val kinds = IndustryTemplateCatalog.setupKindsFor(template, customMods)
                                val momentRepo = MomentCreateRepository()
                                val failedKinds = mutableListOf<String>()
                                for (kind in kinds) {
                                    val entry = BusinessSetupCatalog.forKind(kind)
                                    val prefs = BusinessSetupCatalog.defaultPreferences(
                                        kind,
                                        audience,
                                    ).toMutableMap()
                                    // Company-profile only — not a moment preference key.
                                    prefs.remove(IndustryTemplateCatalog.PROFILE_KEY)
                                    val allowed = BusinessSetupCatalog.allowedKeys(kind)
                                    val safePrefs = prefs.filterKeys { it in allowed }
                                    val result = momentRepo.createBusinessMoment(
                                        companyId = created.companyId,
                                        familyCode = kind.familyCode,
                                        momentTypeCode = entry.momentTypeCode,
                                        title = entry.defaultTitle,
                                        preferences = safePrefs,
                                        status = "ACTIVE",
                                    )
                                    if (result.isFailure) {
                                        failedKinds += entry.defaultTitle
                                    }
                                }
                                if (failedKinds.isNotEmpty()) {
                                    softWarnings +=
                                        "Company created; ${failedKinds.joinToString()} could not be set up—open Create to add"
                                }
                            }
                            if (softWarnings.isNotEmpty()) {
                                activateWarning = softWarnings.joinToString(" · ")
                            }
                            val summary = CompanySummary(
                                companyId = created.companyId,
                                displayName = created.displayName,
                            )
                            val inviteUrl = runCatching {
                                ApiClient.apiService.mintCompanyInvite(
                                    idempotencyKey = UUID.randomUUID().toString(),
                                    body = MintCompanyInviteBody(companyId = created.companyId),
                                ).data.let { dto ->
                                    dto.invitePath.ifBlank { CompanyJoinLink.displayPath(dto.inviteCode) }
                                }
                            }.getOrNull()
                            if (inviteUrl != null) {
                                val phoneHint = members
                                    .asReversed()
                                    .mapNotNull { m -> m.name.takeIf { looksLikeInvitePhone(it) } }
                                    .firstOrNull()
                                    ?: inviteText.takeIf { looksLikeInvitePhone(it) }
                                pendingInviteSend = PendingInviteSend(
                                    phone = phoneHint,
                                    message = inviteMessage(created.displayName, inviteUrl),
                                )
                                pendingActivation = summary
                            } else {
                                onActivated(summary)
                            }
                        } catch (e: Exception) {
                            activateError = e.message?.takeIf { it.isNotBlank() }
                                ?: "Could not activate company. Check your connection and try again."
                        } finally {
                            activating = false
                        }
                    }
                },
                onBack = { step = 3 },
                onCloseSetup = onClose,
            )
            }
        }
    }

    CompanyJoinCodeSheet(
        visible = showJoinCode,
        onDismiss = { showJoinCode = false },
        onJoined = { company ->
            showJoinCode = false
            onActivated(company)
        },
    )
    if (showAddPeople) {
        com.example.momentra.ui.shell.business.gap.BusinessAddPeopleSheet(
            onDismiss = { showAddPeople = false },
            onConfirm = { name ->
                members.add(
                    CoMember(
                        initials = name.take(2).uppercase(),
                        name = name,
                        role = "Member",
                        scope = "All Locations",
                        color = CoAccent,
                    ),
                )
                showAddPeople = false
            },
        )
    }

    if (locationEditorOpen) {
        CoLocationEditorDialog(
            title = if (locationEditor == null) "Add location" else "Edit location",
            name = locationNameDraft,
            onName = { locationNameDraft = it },
            area = locationAreaDraft,
            onArea = { locationAreaDraft = it },
            canRemove = locationEditor != null,
            onRemove = {
                val index = locationEditor
                if (index != null && index in locations.indices) {
                    val wasPrimary = locations[index].primary
                    locations.removeAt(index)
                    if (wasPrimary && locations.isNotEmpty() && locations.none { it.primary }) {
                        locations[0] = locations[0].copy(primary = true, accent = CoGreen)
                    }
                }
                locationEditorOpen = false
            },
            onDismiss = { locationEditorOpen = false },
            onSave = {
                val name = locationNameDraft.trim()
                if (name.isEmpty()) return@CoLocationEditorDialog
                val area = locationAreaDraft.trim()
                val index = locationEditor
                if (index != null && index in locations.indices) {
                    val current = locations[index]
                    locations[index] = current.copy(name = name, area = area)
                } else {
                    locations.add(
                        CoLocation(
                            name = name,
                            area = area,
                            primary = locations.isEmpty(),
                            accent = if (locations.isEmpty()) CoGreen else CoAmber,
                        ),
                    )
                }
                locationEditorOpen = false
            },
        )
    }
    memberEditor?.let { index ->
        CoMemberEditorDialog(
            name = memberNameDraft,
            onName = { memberNameDraft = it },
            onDismiss = { memberEditor = null },
            onSave = {
                val name = memberNameDraft.trim()
                val current = members.getOrNull(index)
                if (name.isNotEmpty() && current != null && !current.you) {
                    members[index] = current.copy(
                        name = name,
                        initials = name.take(2).uppercase(),
                    )
                }
                memberEditor = null
            },
        )
    }

    InviteSendChooserDialog(
        pending = pendingInviteSend,
        accent = CoAccent,
        onMessages = { pending ->
            sendInviteSms(context, pending.phone, pending.message)
            pendingInviteSend = null
            pendingActivation?.let { onActivated(it) }
            pendingActivation = null
        },
        onWhatsApp = { pending ->
            sendInviteWhatsApp(context, pending.phone, pending.message)
            pendingInviteSend = null
            pendingActivation?.let { onActivated(it) }
            pendingActivation = null
        },
        onDismiss = {
            pendingInviteSend = null
            pendingActivation?.let { onActivated(it) }
            pendingActivation = null
        },
    )
}

@Composable
private fun CoHeader(step: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClose)
                .semantics {
                    role = Role.Button
                    contentDescription = "Close"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("✕", color = CoDim, fontSize = 14.sp)
            Text("Close", color = CoDim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = "ONBOARDING $step/4",
            color = CoAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CoDropdownField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CoBg)
                .border(1.dp, CoBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(value, color = Color.White, fontSize = 14.sp)
                Text("▼", color = CoDim, fontSize = 14.sp)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CoCard),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == value) CoAccent else Color.White,
                            fontSize = 14.sp,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CoWelcome(onGetStarted: () -> Unit, onHaveCode: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        CoReveal(delayMs = 40, fromScale = 0.92f, fromY = 0f) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(CoCard)
                    .border(1.dp, CoAccent.copy(alpha = 0.2f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("M", color = CoAccent, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        CoReveal(delayMs = 120) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Set Up Your Business", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tell us a little about your business to get started.",
                    color = CoMuted,
                    fontSize = 14.sp,
                )
            }
        }
        CoReveal(delayMs = 200) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BenefitCard("Quick Setup", "Takes about 2 minutes", CoAccent.copy(alpha = 0.1f), "⏱")
                BenefitCard("Add locations anytime", "For shops, offices or branches", CoGreen.copy(alpha = 0.1f), "▣")
                BenefitCard("Change anytime", "You can update these details later", CoAmber.copy(alpha = 0.1f), "✎")
            }
        }
        CoReveal(delayMs = 280) {
            CoProgressDots(current = 1, label = "Current: Welcome Setup")
        }
        Spacer(Modifier.height(24.dp))
        CoReveal(delayMs = 360, fromY = 16f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CoPrimaryButton("Get Started →", onClick = onGetStarted)
                Text(
                    text = "I already have a company code",
                    color = CoDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onHaveCode)
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun BenefitCard(title: String, body: String, iconBg: Color, glyph: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CoCard)
            .border(1.dp, CoBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, color = Color.White, fontSize = 14.sp)
        }
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = CoMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CoProgressDots(current: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..4) {
                if (i == current) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$i", color = CoBg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CoBorder),
                    )
                }
                if (i < 4) {
                    Box(modifier = Modifier.width(40.dp).height(2.dp).background(CoBorder))
                }
            }
        }
        Text(label, color = CoAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CoStepStrip(active: Int) {
    val labels = listOf("Welcome", "Company", "Locations", "Launch")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { idx, label ->
            val n = idx + 1
            val done = n < active
            val current = n == active
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(when {
                            done -> CoGreen
                            current -> CoAccent
                            else -> CoCard
                        })
                        .then(
                            if (!done && !current) Modifier.border(1.dp, CoBorder, RoundedCornerShape(10.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (done) "✓" else "$n",
                        color = if (done || current) CoBg else CoMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = label,
                    color = if (current) Color.White else CoMuted,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CoSectionCard(number: String, title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CoCard)
            .border(1.dp, CoBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(number, color = CoAccent.copy(alpha = 0.12f), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = CoAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun CoFieldLabel(text: String) {
    Text(text, color = CoDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun CoTextField(value: String, onValueChange: (String) -> Unit, placeholder: String = "") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CoBg)
            .border(1.dp, CoBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, color = CoMuted, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(CoAccent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CoPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CoAccent else CoBg)
            .then(if (!selected) Modifier.border(1.dp, CoBorder, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) CoBg else CoMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CoModuleToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CoCard)
            .clickable { onChecked(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(
            if (checked) "On" else "Off",
            color = if (checked) CoGreen else CoDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CoPrimaryButton(label: String, onClick: () -> Unit, color: Color = CoAccent, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (color == CoGreen) Color.White else CoBg,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CoCompanyForm(
    companyName: String,
    onCompanyName: (String) -> Unit,
    nameError: String? = null,
    industry: String,
    onIndustry: (String) -> Unit,
    companySize: String,
    onCompanySize: (String) -> Unit,
    entityType: String,
    onEntityType: (String) -> Unit,
    gstin: String,
    onGstin: (String) -> Unit,
    currency: String,
    onCurrency: (String) -> Unit,
    fyCycle: String,
    onFyCycle: (String) -> Unit,
    timezone: String,
    onTimezone: (String) -> Unit,
    audience: String,
    onAudience: (String) -> Unit,
    industryTemplate: IndustryTemplate,
    onIndustryTemplate: (IndustryTemplate) -> Unit,
    sellWhat: String,
    onSellWhat: (String) -> Unit,
    billHow: String,
    onBillHow: (String) -> Unit,
    customMoney: Boolean,
    onCustomMoney: (Boolean) -> Unit,
    customDaily: Boolean,
    onCustomDaily: (Boolean) -> Unit,
    customTeam: Boolean,
    onCustomTeam: (Boolean) -> Unit,
    logoPreview: ImageBitmap?,
    logoError: String?,
    onLogoPicked: (ByteArray?, ImageBitmap?) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        val preview = bytes?.let { raw ->
            BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
        }
        onLogoPicked(bytes, preview)
    }
    val smallShop = BusinessAudience.isSmallShop(audience)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
    CoStepStrip(active = 2)
    CoSectionCard("01", "COMPANY PROFILE") {
        CoFieldLabel("WHO IS THIS FOR?")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CoPill(
                "Small shop / Retail",
                audience == BusinessAudience.SMALL_SHOP,
            ) {
                onAudience(BusinessAudience.SMALL_SHOP)
            }
            CoPill(
                "Growing business",
                audience == BusinessAudience.GROWING,
            ) {
                onAudience(BusinessAudience.GROWING)
            }
        }
        if (smallShop) {
            CoFieldLabel("WHAT DO YOU SELL?")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Physical items", "Made-to-order", "Services & repairs").forEach {
                    CoPill(it, sellWhat == it) { onSellWhat(it) }
                }
            }
            CoFieldLabel("HOW DO YOU BILL?")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Counter billing", "On-site / digital invoices").forEach {
                    CoPill(it, billHow == it) { onBillHow(it) }
                }
            }
            CoFieldLabel("STARTER TEMPLATE")
            Text(
                "Start with a template — change anytime in Company Settings.",
                color = CoDim,
                fontSize = 12.sp,
            )
            IndustryTemplateCatalog.all.forEach { tpl ->
                val selected = industryTemplate.id == tpl.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, if (selected) CoAccent else CoBorder, RoundedCornerShape(12.dp))
                        .background(if (selected) CoAccent.copy(alpha = 0.12f) else CoCard)
                        .clickable { onIndustryTemplate(tpl) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(tpl.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(tpl.subtitle, color = CoMuted, fontSize = 12.sp)
                }
            }
            if (industryTemplate.isCustom) {
                CoFieldLabel("WHICH MOMENTS TO START WITH?")
                CoModuleToggle("Money & Cash Flow", customMoney, onCustomMoney)
                CoModuleToggle("Daily Business", customDaily, onCustomDaily)
                CoModuleToggle("Team & Work", customTeam, onCustomTeam)
            }
        }
        CoFieldLabel("COMPANY NAME")
        CoTextField(companyName, onCompanyName, placeholder = "Your shop or company name")
        if (!nameError.isNullOrBlank()) {
            Text(nameError, color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        CoFieldLabel("INDUSTRY")
        CoDropdownField(value = industry, options = CoIndustryOptions, onSelect = onIndustry)
        CoFieldLabel("COMPANY SIZE")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CoSizeOptions.forEach {
                CoPill(it, companySize == it) { onCompanySize(it) }
            }
        }
        CoFieldLabel("COMPANY LOGO")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CoBorder, RoundedCornerShape(10.dp))
                .clickable {
                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center,
        ) {
            if (logoPreview != null) {
                Image(
                    bitmap = logoPreview,
                    contentDescription = "Company logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                val logoHint = if (com.example.momentra.ui.shell.business.shared.BusinessAudience.isSmallShop(audience)) {
                    "Upload shop logo"
                } else {
                    "Upload corporate logo"
                }
                Text(logoHint, color = CoMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (!logoError.isNullOrBlank()) {
            Text(logoError, color = Color(0xFFF87171), fontSize = 12.sp)
        }
    }
    CoSectionCard("02", "LEGAL & FINANCIAL") {
        CoFieldLabel("ENTITY TYPE")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Pvt Ltd", "LLP", "Partnership", "Sole Prop").forEach {
                CoPill(it, entityType == it) { onEntityType(it) }
            }
        }
        CoFieldLabel("GSTIN")
        CoTextField(gstin, onGstin, placeholder = "Enter 15-digit GSTIN")
        CoFieldLabel("PRIMARY CURRENCY")
        CoDropdownField(value = currency, options = CoCurrencyOptions, onSelect = onCurrency)
        CoFieldLabel("FINANCIAL YEAR CYCLE")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CoBg)
                .border(1.dp, CoBorder, RoundedCornerShape(10.dp))
                .padding(3.dp),
        ) {
            listOf("Jan-Dec", "Apr-Mar", "Custom").forEach { opt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (fyCycle == opt) CoCard else Color.Transparent)
                        .then(
                            if (fyCycle == opt) Modifier.border(1.dp, CoBorder, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable { onFyCycle(opt) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        opt,
                        color = if (fyCycle == opt) Color.White else CoMuted,
                        fontSize = 12.sp,
                        fontWeight = if (fyCycle == opt) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
        CoFieldLabel("TIMEZONE")
        CoDropdownField(value = timezone, options = CoTimezoneOptions, onSelect = onTimezone)
    }
    CoPrimaryButton("Continue", onClick = onContinue)
    Text(
        "Back",
        color = CoDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(8.dp),
    )
    }
}

@Composable
private fun CoLocationsForm(
    structure: String,
    onStructure: (String) -> Unit,
    audience: String,
    locations: List<CoLocation>,
    currency: String,
    onAddLocation: () -> Unit,
    onEditLocation: (Int) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val smallShop = com.example.momentra.ui.shell.business.shared.BusinessAudience.isSmallShop(audience)
    val structureOptions = buildList {
        add(Triple("Single Location", "One office or store", "▢"))
        if (!smallShop) {
            add(Triple("Multi-Location", "Multiple branches or offices", "▦"))
            add(Triple("Multi-Unit", "Different business units or brands", "☰"))
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
    CoStepStrip(active = 3)
    CoSectionCard("01", "BUSINESS STRUCTURE") {
        Text("How is your business organized?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        structureOptions.forEach { (title, body, glyph) ->
            val selected = structure == title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) CoCard else CoBg)
                    .border(if (selected) 1.5.dp else 1.dp, if (selected) CoAccent else CoBorder, RoundedCornerShape(10.dp))
                    .clickable { onStructure(title) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(glyph, color = if (selected) CoAccent else CoMuted, fontSize = 18.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    )
                    Text(body, color = CoMuted, fontSize = 11.sp)
                }
                if (selected) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(CoAccent))
                }
            }
        }
    }
    CoSectionCard("02", "YOUR LOCATIONS") {
        if (locations.isEmpty()) {
            Text("No locations yet", color = CoMuted, fontSize = 13.sp)
        }
        locations.forEachIndexed { index, loc ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CoBg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(4.dp).height(52.dp).background(loc.accent))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(loc.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(loc.area, color = CoMuted, fontSize = 11.sp)
                }
                if (loc.primary) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CoGreen.copy(alpha = 0.08f))
                            .border(1.dp, CoGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("Primary", color = CoGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "✎",
                    color = CoDim,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onEditLocation(index) }
                        .padding(end = 12.dp)
                        .semantics { role = Role.Button; contentDescription = "Edit location" },
                )
            }
        }
        Text(
            "+ Add another location",
            color = CoAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddLocation)
                .padding(vertical = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CoBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Locations inherit company defaults", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Currency: $currency · Budget: Company default · Reporting: Consolidated",
                color = CoMuted,
                fontSize = 10.sp,
            )
        }
    }
    CoPrimaryButton("Continue", onClick = onContinue)
    Text(
        "Back",
        color = CoDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(8.dp),
    )
    }
}

private data class CoLaunchNextStep(
    val title: String,
    val body: String,
    val color: Color,
)

private fun coLaunchNextSteps(
    audience: String,
    template: IndustryTemplate,
    customMoney: Boolean,
    customDaily: Boolean,
    customTeam: Boolean,
): Pair<String, List<CoLaunchNextStep>> {
    if (!BusinessAudience.isSmallShop(audience)) {
        return "After activation, open Create to add Money, Daily Business, or Team moments" to emptyList()
    }
    val customMods = mapOf(
        "money" to customMoney,
        "dailyBusiness" to customDaily,
        "teamOps" to customTeam,
    )
    val kinds = IndustryTemplateCatalog.setupKindsFor(template, customMods)
    if (kinds.isEmpty()) {
        return "Turn on at least one moment before activating" to emptyList()
    }
    val steps = kinds.map { kind ->
        when (kind) {
            BusinessSetupKind.BUSINESS_RUNWAY -> CoLaunchNextStep(
                "Money & Cash Flow",
                "Revenue, Khata, invoices, and spend tracking",
                CoAmber,
            )
            BusinessSetupKind.BUSINESS_OPERATIONS -> CoLaunchNextStep(
                "Daily Business",
                "Day-to-day ops, vendors, and routines",
                Color(0xFFA78BFA),
            )
            BusinessSetupKind.TEAM_OPERATIONS -> CoLaunchNextStep(
                "Team & Work",
                "Team rhythm, reviews, and collaboration",
                CoGreen,
            )
        }
    }
    val intro = if (steps.size == 1) {
        "After activation, we'll set up:"
    } else {
        "After activation, we'll set up these moments:"
    }
    return intro to steps
}

@Composable
private fun CoLaunchForm(
    companyName: String,
    members: List<CoMember>,
    inviteText: String,
    onInviteText: (String) -> Unit,
    onAddInvite: () -> Unit,
    onEditMember: (Int) -> Unit,
    onDeleteMember: (Int) -> Unit,
    logoError: String?,
    nameError: String? = null,
    activateError: String? = null,
    activateWarning: String? = null,
    nextSteps: Pair<String, List<CoLaunchNextStep>>,
    activating: Boolean,
    onActivate: () -> Unit,
    onBack: () -> Unit,
    onCloseSetup: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
    CoStepStrip(active = 4)
    CoSectionCard("01", "INVITE YOUR TEAM") {
        Text(
            "Add people to invite after activation — share the invite link when you’re ready",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        members.forEachIndexed { index, m ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(m.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(m.initials, color = CoBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(m.color.copy(alpha = 0.1f))
                                .border(1.dp, m.color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(m.role, color = m.color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(m.scope, color = CoMuted, fontSize = 11.sp)
                }
                if (m.you) {
                    Text("You", color = CoDim, fontSize = 12.sp)
                } else {
                    Text(
                        "✎",
                        color = CoDim,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onEditMember(index) }.semantics {
                            role = Role.Button
                            contentDescription = "Edit member"
                        },
                    )
                    Text(
                        "✕",
                        color = CoDim,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onDeleteMember(index) }.semantics {
                            role = Role.Button
                            contentDescription = "Remove member"
                        },
                    )
                }
            }
        }
        if (!logoError.isNullOrBlank()) {
            Text(logoError, color = Color(0xFFF87171), fontSize = 12.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoBg)
                    .border(1.dp, CoBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (inviteText.isEmpty()) {
                    Text("Enter email or name", color = CoMuted, fontSize = 13.sp)
                }
                BasicTextField(
                    value = inviteText,
                    onValueChange = onInviteText,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(CoAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoAccent)
                    .clickable(onClick = onAddInvite)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Add", color = CoBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Invite teammates after you activate",
            color = CoDim,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    CoSectionCard("02", "WHAT HAPPENS NEXT") {
        Text(
            nextSteps.first,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        nextSteps.second.forEach { step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(step.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("●", color = step.color, fontSize = 10.sp)
                }
                Column {
                    Text(step.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(step.body, color = CoMuted, fontSize = 11.sp)
                }
            }
        }
        if (nextSteps.second.isNotEmpty()) {
            Text(
                "You can customize each moment later from Create.",
                color = CoDim,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "4 sections configured • ${members.count { !it.you }} people to invite",
            color = CoDim,
            fontSize = 13.sp,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CoGreen.copy(alpha = 0.08f))
                .border(1.dp, CoGreen.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✓", color = CoGreen, fontSize = 12.sp)
            Text("Ready to activate", color = CoGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    if (!nameError.isNullOrBlank()) {
        Text(nameError, color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    if (!activateError.isNullOrBlank()) {
        Text(activateError, color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    if (!activateWarning.isNullOrBlank()) {
        Text(activateWarning, color = CoAmber, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    CoPrimaryButton(
        label = if (companyName.isBlank()) "Activate Company →" else "Activate $companyName →",
        onClick = onActivate,
        color = CoGreen,
        enabled = !activating,
    )
    Text(
        "Back",
        color = CoDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !activating, onClick = onBack)
            .padding(8.dp),
    )
    Text(
        "Close",
        color = CoDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !activating, onClick = onCloseSetup).padding(8.dp),
    )
    }
}

@Composable
private fun CoLocationEditorDialog(
    title: String,
    name: String,
    onName: (String) -> Unit,
    area: String,
    onArea: (String) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CoCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            CoFieldLabel("NAME")
            CoTextField(name, onName, placeholder = "Location name")
            CoFieldLabel("ADDRESS")
            CoTextField(area, onArea, placeholder = "Address")
            CoPrimaryButton("Save", onClick = onSave, enabled = name.isNotBlank())
            if (canRemove) {
                Text(
                    "Remove location",
                    color = Color(0xFFF87171),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onRemove).padding(8.dp),
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = CoDim)
            }
        }
    }
}

@Composable
private fun CoMemberEditorDialog(
    name: String,
    onName: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CoCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Edit member", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            CoFieldLabel("NAME")
            CoTextField(name, onName, placeholder = "Name or email")
            CoPrimaryButton("Save", onClick = onSave, enabled = name.isNotBlank())
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = CoDim)
            }
        }
    }
}
