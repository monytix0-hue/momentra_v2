package com.example.momentra.ui.shell.empty

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.ui.shell.empty.business.CompanyJoinCodeSheet
import com.example.momentra.analytics.MomentraAnalytics
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.CreateCompanyBody
import com.example.momentra.data.api.CreateLocationBody
import com.example.momentra.domain.CompanySummary
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

private data class CoLocation(
    val name: String,
    val area: String,
    val primary: Boolean,
    val accent: Color,
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
    var companyName by remember { mutableStateOf("Pureborn Ops") }
    var industry by remember { mutableStateOf("Technology & Software") }
    var companySize by remember { mutableStateOf("Small (2-25)") }
    var entityType by remember { mutableStateOf("Pvt Ltd") }
    var gstin by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("₹ INR — Indian Rupee") }
    var fyCycle by remember { mutableStateOf("Apr-Mar") }
    var timezone by remember { mutableStateOf("IST (UTC+5:30)") }
    var structure by remember { mutableStateOf("Multi-Location") }
    val locations = remember {
        mutableStateListOf(
            CoLocation("HQ — Mumbai", "Andheri East", true, CoGreen),
            CoLocation("Branch — Bangalore", "Koramangala", false, CoAmber),
            CoLocation("Branch — Delhi", "Connaught Place", false, Color(0xFFEF4444)),
        )
    }
    val members = remember {
        mutableStateListOf(
            CoMember("SM", "Sahil M.", "Owner", "All Locations", CoAccent, you = true),
            CoMember("AR", "Ananya R.", "Admin", "Bangalore Branch", CoAmber),
        )
    }
    var inviteText by remember { mutableStateOf("") }
    var activating by remember { mutableStateOf(false) }
    var showJoinCode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                onCompanyName = { companyName = it },
                industry = industry,
                companySize = companySize,
                onCompanySize = { companySize = it },
                entityType = entityType,
                onEntityType = { entityType = it },
                gstin = gstin,
                onGstin = { gstin = it },
                currency = currency,
                fyCycle = fyCycle,
                onFyCycle = { fyCycle = it },
                timezone = timezone,
                onContinue = { step = 3 },
                onBack = { step = 1 },
            )
            3 -> CoLocationsForm(
                structure = structure,
                onStructure = { structure = it },
                locations = locations,
                onContinue = { step = 4 },
                onBack = { step = 2 },
            )
            else -> CoLaunchForm(
                companyName = companyName,
                members = members,
                inviteText = inviteText,
                onInviteText = { inviteText = it },
                onAddInvite = {
                    val t = inviteText.trim()
                    if (t.isNotEmpty()) {
                        members.add(
                            CoMember(
                                initials = t.take(2).uppercase(),
                                name = t,
                                role = "Member",
                                scope = "All Locations",
                                color = CoAccent,
                            ),
                        )
                        inviteText = ""
                    }
                },
                activating = activating,
                onActivate = {
                    if (activating) return@CoLaunchForm
                    activating = true
                    scope.launch {
                        try {
                            val tz = when {
                                timezone.contains("IST", true) -> "Asia/Kolkata"
                                else -> "UTC"
                            }
                            val created = ApiClient.apiService.createCompany(
                                idempotencyKey = UUID.randomUUID().toString(),
                                body = CreateCompanyBody(
                                    displayName = companyName.ifBlank { "My Company" },
                                    legalName = companyName.ifBlank { "My Company" },
                                    timezone = tz,
                                    companyType = entityType,
                                    taxIdentifier = gstin.ifBlank { null },
                                    profileJson = mapOf(
                                        "industry" to industry,
                                        "companySize" to companySize,
                                        "currency" to currency,
                                        "financialYear" to fyCycle,
                                        "structure" to structure,
                                    ),
                                ),
                            ).data
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
                            onActivated(
                                CompanySummary(
                                    companyId = created.companyId,
                                    displayName = created.displayName,
                                ),
                            )
                        } catch (_: Exception) {
                            onClose()
                        } finally {
                            activating = false
                        }
                    }
                },
                onDraft = onClose,
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
                    text = "Get your company running on momentra in just a few steps.",
                    color = CoMuted,
                    fontSize = 14.sp,
                )
            }
        }
        CoReveal(delayMs = 200) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BenefitCard("2-minute setup", "Quick guided configuration", CoAccent.copy(alpha = 0.1f), "⏱")
                BenefitCard("Multi-location ready", "Support for branches & units", CoGreen.copy(alpha = 0.1f), "▣")
                BenefitCard("Edit anytime", "All settings adjustable later", CoAmber.copy(alpha = 0.1f), "✎")
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
    industry: String,
    companySize: String,
    onCompanySize: (String) -> Unit,
    entityType: String,
    onEntityType: (String) -> Unit,
    gstin: String,
    onGstin: (String) -> Unit,
    currency: String,
    fyCycle: String,
    onFyCycle: (String) -> Unit,
    timezone: String,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    CoStepStrip(active = 2)
    CoSectionCard("01", "COMPANY PROFILE") {
        CoFieldLabel("COMPANY NAME")
        CoTextField(companyName, onCompanyName)
        CoFieldLabel("INDUSTRY")
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(industry, color = Color.White, fontSize = 14.sp)
                Text("▼", color = CoDim, fontSize = 14.sp)
            }
        }
        CoFieldLabel("COMPANY SIZE")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Solo (1)", "Small (2-25)", "Medium (26-100)").forEach {
                CoPill(it, companySize == it) { onCompanySize(it) }
            }
        }
        CoFieldLabel("COMPANY LOGO")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CoBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Upload corporate logo", color = CoMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(currency, color = Color.White, fontSize = 14.sp)
                Text("▼", color = CoDim, fontSize = 14.sp)
            }
        }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(timezone, color = Color.White, fontSize = 14.sp)
                Text("▼", color = CoDim, fontSize = 14.sp)
            }
        }
    }
    CoPrimaryButton("Continue", onClick = onContinue)
    Text(
        "Back",
        color = CoDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(8.dp),
    )
}

@Composable
private fun CoLocationsForm(
    structure: String,
    onStructure: (String) -> Unit,
    locations: List<CoLocation>,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    CoStepStrip(active = 3)
    CoSectionCard("01", "BUSINESS STRUCTURE") {
        Text("How is your business organized?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        listOf(
            Triple("Single Location", "One office or store", "▢"),
            Triple("Multi-Location", "Multiple branches or offices", "▦"),
            Triple("Multi-Unit", "Different business units or brands", "☰"),
        ).forEach { (title, body, glyph) ->
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
        locations.forEach { loc ->
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
                Text("✎", color = CoDim, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp))
            }
        }
        Text(
            "+ Add another location",
            color = CoAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                "Currency: ₹ INR · Budget: Company default · Reporting: Consolidated",
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onBack).padding(8.dp),
    )
}

@Composable
private fun CoLaunchForm(
    companyName: String,
    members: List<CoMember>,
    inviteText: String,
    onInviteText: (String) -> Unit,
    onAddInvite: () -> Unit,
    activating: Boolean,
    onActivate: () -> Unit,
    onDraft: () -> Unit,
) {
    CoStepStrip(active = 4)
    CoSectionCard("01", "INVITE YOUR TEAM") {
        Text("Add team members to get started", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        members.forEach { m ->
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
                Text(if (m.you) "You" else "✎", color = CoDim, fontSize = 12.sp)
            }
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
            "3 free members included · Upgrade for more",
            color = CoDim,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    CoSectionCard("02", "WHAT HAPPENS NEXT") {
        Text(
            "After activation, three module wizards will guide you:",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        listOf(
            Triple("Team Operations", "Set review cycles, monitoring style, team pods", CoGreen),
            Triple("Business Runway", "Configure financials, cash tracking, burn alerts", CoAmber),
            Triple("Business Operations", "Define budgets, approval workflows, vendors", Color(0xFFA78BFA)),
        ).forEach { (title, body, color) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("●", color = color, fontSize = 10.sp)
                }
                Column {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(body, color = CoMuted, fontSize = 11.sp)
                }
            }
        }
        Text(
            "Each takes about 1 minute to configure.",
            color = CoDim,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("4 sections configured • ${members.size} team members added", color = CoDim, fontSize = 13.sp)
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
    CoPrimaryButton(
        label = "Activate ${companyName.ifBlank { "Company" }} →",
        onClick = onActivate,
        color = CoGreen,
        enabled = !activating,
    )
    Text(
        "Save as draft",
        color = CoDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !activating, onClick = onDraft).padding(8.dp),
    )
}
