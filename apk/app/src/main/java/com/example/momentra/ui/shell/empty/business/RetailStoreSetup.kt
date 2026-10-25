package com.example.momentra.ui.shell.empty.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.momentra.R
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.shell.business.shared.OperatingMomentCatalog
import com.example.momentra.ui.shell.business.shared.OperatingSubtype
import com.example.momentra.ui.shell.maestro.MaestroIds
import androidx.compose.ui.platform.testTag
import com.example.momentra.ui.shell.shared.PersonLabel
import com.example.momentra.analytics.AnalyticsScreens
import com.example.momentra.analytics.trackWidget
import com.example.momentra.ui.theme.PlusJakartaSans
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class SetupLook(
    val code: String,
    val bg: Color,
    val card: Color,
    val field: Color,
    val border: Color,
    val notice: Color,
    val brand: Color,
    val accent: Color,
    val number: Color,
    val secondary: Color,
    val text: Color,
    val statusFill: Color,
    val statusInk: Color,
    val draftFill: Color,
    val draftInk: Color,
    val inviteFill: Color,
    val actionInk: Color,
    val activateFill: Color,
    val pillBorder: Color,
    val chipOnFill: Color,
    val chipOnBorder: Color,
    val chipOnLabel: Color,
    val chipOffFill: Color,
    val chipOffLabel: Color,
    val channelOnFill: Color,
    val channelOnLabel: Color,
    val channelOffLabel: Color,
    val hero: Int,
    val mode: String,
    val title: String,
    val badge: String,
    val basicsLine: String,
    val nameHint: String,
    val descriptionHint: String,
    val locationHint: String,
    val ownerAccess: String,
    val leadTitle: String,
    val leadBody: String,
    val peopleNote: String,
    val channelPrompt: String,
    val channels: List<String>,
    val activate: String,
    val footer: String,
    val reviewKind: String,
    val tradeReach: List<String> = emptyList(),
    val partySection: String = "",
    val partyKinds: List<Pair<String, String>> = emptyList(),
    val steps: String = "3 steps · One simple setup",
    val intro: String = "Bring your business, people, and monthly plan together before you start.",
    val partyNote: String = "Optional. These are counterparties, not people who can sign in.",
    val channelHelp: String = "These are the places you sell or serve. They are not a different kind of business.",
    val targetNote: String = "Sales you record count toward the target. Money already collected does not. Purchases and expenses count once toward the budget.",
    val targetMetrics: List<String> = listOf("REVENUE"),
    val channelsOptional: Boolean = false,
)

private val sharedRoles = listOf(
    "Finance" to "Can see money records. Cannot approve unless you allow it.",
    "Approver" to "Can review and approve. Does not pay unless you allow it.",
    "Contributor" to "Can add updates for the work you assign.",
    "Team member" to "Can post updates. Money totals stay hidden.",
    "Observer" to "Can view what you assign. Money totals stay hidden.",
)

private fun campaignLook(
    code: String,
    title: String,
    badge: String,
    intro: String,
    basicsLine: String,
    channels: List<String>,
    partyKinds: List<Pair<String, String>>,
    partyNote: String,
    targetMetrics: List<String>,
    reviewKind: String,
) = SetupLook(
    code = code,
    bg = Color(0xFF09121F),
    card = Color(0xFF122235),
    field = Color(0xFF09121F),
    border = Color(0xFF2B4B63),
    notice = Color(0xFF16405C),
    brand = Color(0xFF6DDCFF),
    accent = Color(0xFF6DDCFF),
    number = Color(0xFF406582),
    secondary = Color(0xFFA5BBCE),
    text = Color(0xFFE5E0EE),
    statusFill = Color(0xFF16405C),
    statusInk = Color(0xFF6DDCFF),
    draftFill = Color(0xFF16405C),
    draftInk = Color(0xFF6DDCFF),
    inviteFill = Color(0xFF6DDCFF),
    actionInk = Color(0xFF09121F),
    activateFill = Color(0xFF6DDCFF),
    pillBorder = Color(0xFF2B4B63),
    chipOnFill = Color(0xFF16405C),
    chipOnBorder = Color(0xFF6DDCFF),
    chipOnLabel = Color(0xFFE5E0EE),
    chipOffFill = Color(0xFF09121F),
    chipOffLabel = Color(0xFFA5BBCE),
    channelOnFill = Color(0xFF16405C),
    channelOnLabel = Color(0xFF6DDCFF),
    channelOffLabel = Color(0xFFA5BBCE),
    hero = R.drawable.rs_setup_campaign,
    mode = "Marketing",
    title = title,
    badge = badge,
    basicsLine = basicsLine,
    nameHint = "Campaign name",
    descriptionHint = "Everyday notes about this campaign",
    locationHint = "Where this campaign runs",
    ownerAccess = "Full campaign access. Only the owner changes roles.",
    leadTitle = "Campaign lead",
    leadBody = "Runs day-to-day campaign work. Does not see full money records unless you allow it.",
    peopleNote = "You can start on your own. A campaign lead and finance person are optional.",
    channelPrompt = "Channels are optional.",
    channels = channels,
    activate = "Activate campaign",
    footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until the campaign is active.",
    reviewKind = reviewKind,
    partySection = "Network / Counterparties",
    partyKinds = partyKinds,
    steps = "4 steps · One simple setup",
    intro = intro,
    partyNote = partyNote,
    channelHelp = "These are the places this campaign runs. They are not a different kind of business.",
    targetNote = "Accepted results count toward the target. Money collected does not. Spend and vendor costs count once toward the budget.",
    targetMetrics = targetMetrics,
    channelsOptional = true,
)

private fun productionLook(
    code: String,
    mint: Boolean,
    title: String,
    intro: String,
    basicsLine: String,
    partySection: String,
    partyKinds: List<Pair<String, String>>,
    partyNote: String,
    targetMetrics: List<String>,
    reviewKind: String,
    steps: String,
    tradeReach: List<String> = emptyList(),
) = SetupLook(
    code = code,
    bg = if (mint) Color(0xFF071014) else Color(0xFF09121F),
    card = if (mint) Color(0xFF15262C) else Color(0xFF122235),
    field = if (mint) Color(0xFF071014) else Color(0xFF09121F),
    border = if (mint) Color(0xFF345058) else Color(0xFF2B4B63),
    notice = Color(0xFF15262C).let { if (mint) it else Color(0xFF16405C) },
    brand = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    accent = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    number = if (mint) Color(0xFF66C7A4) else Color(0xFF406582),
    secondary = if (mint) Color(0xFFAFC1C3) else Color(0xFFA5BBCE),
    text = if (mint) Color(0xFFEFF5F2) else Color(0xFFE5E0EE),
    statusFill = if (mint) Color(0xFF15262C) else Color(0xFF16405C),
    statusInk = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    draftFill = if (mint) Color(0xFF15262C) else Color(0xFF16405C),
    draftInk = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    inviteFill = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    actionInk = if (mint) Color(0xFF071014) else Color(0xFF09121F),
    activateFill = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    pillBorder = if (mint) Color(0xFF345058) else Color(0xFF2B4B63),
    chipOnFill = if (mint) Color(0xFF15262C) else Color(0xFF16405C),
    chipOnBorder = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    chipOnLabel = if (mint) Color(0xFFEFF5F2) else Color(0xFFE5E0EE),
    chipOffFill = if (mint) Color(0xFF071014) else Color(0xFF09121F),
    chipOffLabel = if (mint) Color(0xFFAFC1C3) else Color(0xFFA5BBCE),
    channelOnFill = if (mint) Color(0xFF15262C) else Color(0xFF16405C),
    channelOnLabel = if (mint) Color(0xFF66C7A4) else Color(0xFF6DDCFF),
    channelOffLabel = if (mint) Color(0xFFAFC1C3) else Color(0xFFA5BBCE),
    hero = if (mint) R.drawable.rs_setup_jobwork else R.drawable.rs_setup_standard,
    mode = "Manufacturing",
    title = title,
    badge = "Setup draft · Not active",
    basicsLine = basicsLine,
    nameHint = "Factory name",
    descriptionHint = "Everyday notes about this factory",
    locationHint = "Factory location",
    ownerAccess = "Full manufacturing access. Only the owner changes roles.",
    leadTitle = "Production / Order Lead",
    leadBody = "Runs day-to-day production. Does not see full money records unless you allow it.",
    peopleNote = "You can start on your own. A production lead and finance person are optional.",
    channelPrompt = "Channels are optional.",
    channels = emptyList(),
    activate = "Activate Manufacturing Moment",
    footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until the manufacturing business is active.",
    reviewKind = reviewKind,
    tradeReach = tradeReach,
    partySection = partySection,
    partyKinds = partyKinds,
    steps = steps,
    intro = intro,
    partyNote = partyNote,
    channelHelp = "Channels are optional. They are the places finished goods leave the factory.",
    targetNote = "Validated sales count toward a revenue target. Money collected does not. Production, orders, and deliveries count only for the measure you choose. Purchases and expenses count once toward the budget.",
    targetMetrics = targetMetrics,
    channelsOptional = true,
)

private fun metricLabel(code: String, metrics: List<String> = emptyList()): String = when (code) {
    "REVENUE" -> if ("PRODUCTION_OUTPUT" in metrics) "Recognized sales" else if ("VALUE_DISPATCHED" in metrics) "Sales / Revenue" else "Revenue"
    "LEADS" -> "Leads"
    "CONVERSIONS" -> "Conversions"
    "REACH" -> "Reach"
    "ENGAGEMENT" -> "Engagement"
    "ORDERS" -> if ("PRODUCTION_OUTPUT" in metrics) "Order intake" else if ("DELIVERY_COUNT" in metrics) "Order count" else "Orders"
    "CUSTOM_COUNT" -> if ("DELIVERY_COUNT" in metrics) "Compatible units" else "Custom count"
    "CLIENT_BILLING" -> "Client billing"
    "PRODUCTION_OUTPUT" -> if ("DELIVERY_COUNT" in metrics) "Accepted units" else "Production output"
    "UNITS_PRODUCED" -> "Units Produced"
    "VALUE_DISPATCHED" -> "Value Dispatched"
    "DELIVERY_COUNT" -> if ("PRODUCTION_OUTPUT" in metrics) "Delivered units" else "Delivery count"
    else -> code
}

private fun setupLook(code: String): SetupLook = when (code) {
    "CLIENT_CAMPAIGN" -> campaignLook(
        code = code,
        title = "Marketing Campaign Setup",
        badge = "Campaign draft · Not active",
        intro = "Run a marketing campaign with clear roles, counterparties and measurable targets before you activate.",
        basicsLine = "A home for this client campaign.",
        channels = listOf("Search Ads", "Paid Social", "Display / Video Ads", "Organic Social", "SEO / Content", "Email", "SMS / Messaging", "Cold Calling", "Field Sales / Visits", "Newspaper / Magazine", "Flyers / Brochures", "Radio / Podcast", "Billboard / Transit", "Exhibition / Roadshow", "Influencer", "Affiliate / Referral", "Custom Channel"),
        partyKinds = listOf("CLIENT" to "Client contact", "VENDOR" to "Vendor, creator, agency, or platform"),
        partyNote = "Client identity and optional vendors, creators, agencies, and platforms are separate from invited team roles.",
        targetMetrics = listOf("LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT", "CLIENT_BILLING"),
        reviewKind = "Client Campaign",
    )
    "OWN_BUSINESS_CAMPAIGN" -> campaignLook(
        code = code,
        title = "Own Business Campaign Setup",
        badge = "Draft · Not active",
        intro = "You run campaigns for your own business or brand",
        basicsLine = "A home for this brand campaign.",
        channels = listOf("Search Ads", "Paid Social", "Display / Video Ads", "Organic Social", "SEO / Content", "Email", "SMS / Messaging", "Cold Calling", "Field Sales / Visits", "Newspaper / Magazine", "Flyers / Brochures", "Radio / Podcast", "Billboard / Transit", "Exhibition / Roadshow", "Influencer", "Affiliate / Referral", "Custom Channel"),
        partyKinds = listOf("VENDOR" to "Vendor, creator, agency, or platform"),
        partyNote = "Vendors, creators, agencies and platforms are separate from invited team roles. No client is required.",
        targetMetrics = listOf("LEADS", "CONVERSIONS", "REVENUE", "ORDERS", "REACH", "ENGAGEMENT", "CUSTOM_COUNT"),
        reviewKind = "Own Business Campaign",
    )
    "STANDARD_PRODUCTION" -> productionLook(
        code = code,
        mint = false,
        title = "Standard Production Setup",
        intro = "Set up your manufacturing business, people, suppliers, buyers and monthly plan before activation.",
        basicsLine = "A home for this factory.",
        partySection = "Material Suppliers & Buyers",
        partyKinds = listOf("SUPPLIER" to "Material supplier", "BUYER" to "Buyer or group"),
        partyNote = "Material suppliers and buyers are optional. Counterparties stay separate from invited team roles.",
        targetMetrics = listOf("REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"),
        reviewKind = "Standard Production",
        steps = "4 steps · Factory, people, suppliers, buyers and monthly plan",
    )
    "MADE_TO_ORDER" -> productionLook(
        code = code,
        mint = true,
        title = "Made-to-Order Production Setup",
        intro = "Set up your job-work business, people, suppliers, customers and monthly plan.",
        basicsLine = "A home for this workshop.",
        partySection = "Material Suppliers & Customers",
        partyKinds = listOf("SUPPLIER" to "Material supplier", "CUSTOMER" to "Customer"),
        partyNote = "Material suppliers and customers are optional. They are counterparties, not people who can sign in.",
        targetMetrics = listOf("REVENUE", "PRODUCTION_OUTPUT", "DELIVERY_COUNT", "ORDERS"),
        reviewKind = "Made-to-Order Production",
        steps = "4 steps · Factory, people, suppliers, customers and monthly plan",
        tradeReach = listOf("Local", "Domestic", "International"),
    )
    "TRADING_WHOLESALE" -> SetupLook(
        code = code,
        bg = Color(0xFF09121F),
        card = Color(0xFF122235),
        field = Color(0xFF09121F),
        border = Color(0xFF2B4B63),
        notice = Color(0xFF16405C),
        brand = Color(0xFF6DDCFF),
        accent = Color(0xFF6DDCFF),
        number = Color(0xFF406582),
        secondary = Color(0xFFA5BBCE),
        text = Color(0xFFE5E0EE),
        statusFill = Color(0xFF16405C),
        statusInk = Color(0xFF6DDCFF),
        draftFill = Color(0xFF16405C),
        draftInk = Color(0xFF6DDCFF),
        inviteFill = Color(0xFF6DDCFF),
        actionInk = Color(0xFF09121F),
        activateFill = Color(0xFF6DDCFF),
        pillBorder = Color(0xFF2B4B63),
        chipOnFill = Color(0xFF16405C),
        chipOnBorder = Color(0xFF6DDCFF),
        chipOnLabel = Color(0xFFE5E0EE),
        chipOffFill = Color(0xFF09121F),
        chipOffLabel = Color(0xFFA5BBCE),
        channelOnFill = Color(0xFF16405C),
        channelOnLabel = Color(0xFF6DDCFF),
        channelOffLabel = Color(0xFFA5BBCE),
        hero = R.drawable.rs_setup_trading,
        mode = "Trading",
        title = "Trading / Wholesale Setup",
        badge = "Not started yet",
        basicsLine = "A home for this trading business.",
        nameHint = "Business name",
        descriptionHint = "Everyday notes about this trade",
        locationHint = "Warehouse or business location",
        ownerAccess = "Full trading business access. Only the owner changes roles.",
        leadTitle = "Trading manager",
        leadBody = "Runs day-to-day work. Does not see full money records unless you allow it.",
        peopleNote = "You can start on your own. You can invite people after this starts.",
        channelPrompt = "Where do you sell? Choose at least one.",
        channels = listOf("Wholesale", "Direct", "Dealer", "Marketplace", "Export", "Other"),
        activate = "Activate trading moment",
        footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
        reviewKind = "Trading / Wholesale",
        tradeReach = listOf("Local", "Domestic", "International"),
        partySection = "Suppliers, Buyers & Channels",
        partyKinds = listOf("SUPPLIER" to "Supplier", "BUYER" to "Buyer"),
        steps = "4 steps · One simple setup",
        intro = "Bring your business, people and monthly plan together. Make it yours before you activate.",
    )
    "DISTRIBUTION_DEALERSHIP" -> SetupLook(
        code = code,
        bg = Color(0xFF071014),
        card = Color(0xFF0C171B),
        field = Color(0xFF0C171B),
        border = Color(0xFF345058),
        notice = Color(0xFF15262C),
        brand = Color(0xFF66C7A4),
        accent = Color(0xFF66C7A4),
        number = Color(0xFF66C7A4),
        secondary = Color(0xFF66C7A4),
        text = Color(0xFFEFF5F2),
        statusFill = Color(0xFF15262C),
        statusInk = Color(0xFF66C7A4),
        draftFill = Color(0xFF15262C),
        draftInk = Color(0xFF66C7A4),
        inviteFill = Color(0xFF66C7A4),
        actionInk = Color(0xFF071014),
        activateFill = Color(0xFF66C7A4),
        pillBorder = Color(0xFF345058),
        chipOnFill = Color(0xFF15262C),
        chipOnBorder = Color(0xFF66C7A4),
        chipOnLabel = Color(0xFFEFF5F2),
        chipOffFill = Color(0xFF0C171B),
        chipOffLabel = Color(0xFF66C7A4),
        channelOnFill = Color(0xFF15262C),
        channelOnLabel = Color(0xFF66C7A4),
        channelOffLabel = Color(0xFF66C7A4),
        hero = R.drawable.rs_setup_distribution,
        mode = "Distribution",
        title = "Distribution / Dealership Setup",
        badge = "Not started yet",
        basicsLine = "A home for this distribution business.",
        nameHint = "Business name",
        descriptionHint = "Everyday notes about this distribution",
        locationHint = "Depot or service territory",
        ownerAccess = "Full distribution business access. Only the owner changes roles.",
        leadTitle = "Distribution manager",
        leadBody = "Operations lead. Does not see full money records unless you allow it.",
        peopleNote = "You can start on your own. A distribution manager and finance person are optional.",
        channelPrompt = "Where do you sell? Choose at least one.",
        channels = listOf("Retailer", "Dealer", "Sub-distributor", "Institutional", "Direct", "Other"),
        activate = "Activate distribution moment",
        footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until the distribution business is active.",
        reviewKind = "Distribution / Dealership",
        tradeReach = listOf("Local", "Domestic", "International"),
        partySection = "Principals & Counterparties",
        partyKinds = listOf("PRINCIPAL" to "Principal or supplier", "DEALER" to "Dealer or buyer group"),
        steps = "4 steps · One simple setup",
        intro = "Bring your business, people and key counterparties together. Make it yours before you activate.",
    )
    "RESTAURANT_FOOD" -> SetupLook(
        code = code,
        bg = Color(0xFF101412),
        card = Color(0xFF202A24),
        field = Color(0xFF1A211D),
        border = Color(0xFF344238),
        notice = Color(0xFF202A24),
        brand = Color(0xFFF2B544),
        accent = Color(0xFFF2B544),
        number = Color(0xFFF2B544),
        secondary = Color(0xFFB9C5BA),
        text = Color(0xFFF4F3EB),
        statusFill = Color(0xFF202A24),
        statusInk = Color(0xFFF2B544),
        draftFill = Color(0xFF202A24),
        draftInk = Color(0xFFF2B544),
        inviteFill = Color(0xFFF2B544),
        actionInk = Color(0xFF101412),
        activateFill = Color(0xFFF2B544),
        pillBorder = Color(0xFF344238),
        chipOnFill = Color(0xFF202A24),
        chipOnBorder = Color(0xFFF2B544),
        chipOnLabel = Color(0xFFF4F3EB),
        chipOffFill = Color(0xFF1A211D),
        chipOffLabel = Color(0xFFB9C5BA),
        channelOnFill = Color(0xFFF2B544),
        channelOnLabel = Color(0xFF101412),
        channelOffLabel = Color(0xFFF4F3EB),
        hero = R.drawable.rs_setup_restaurant,
        mode = "Restaurant",
        title = "Restaurant / Food Setup",
        badge = "Not started yet",
        basicsLine = "A home for this restaurant.",
        nameHint = "Restaurant name",
        descriptionHint = "Everyday notes about this restaurant",
        locationHint = "Add your restaurant location",
        ownerAccess = "Only the owner can change who has access.",
        leadTitle = "Restaurant manager",
        leadBody = "Runs day-to-day work. Does not see full money records unless you allow it.",
        peopleNote = "You can start on your own. You can invite people and add suppliers after this starts.",
        channelPrompt = "Where do you sell? Choose at least one.",
        channels = listOf("Dine-in", "Direct", "Zomato", "Takeaway", "Swiggy"),
        activate = "Start this restaurant",
        footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
        reviewKind = "Restaurant / Food",
    )
    "CONSUMER_SERVICE" -> SetupLook(
        code = code,
        bg = Color(0xFF080F0E),
        card = Color(0xFF0F2320),
        field = Color(0xFF0B1917),
        border = Color(0xFF1E3530),
        notice = Color(0xFF0F2320),
        brand = Color(0xFF0ECBB0),
        accent = Color(0xFF0ECBB0),
        number = Color(0xFF0ECBB0),
        secondary = Color(0xFF8FBFBA),
        text = Color(0xFFD8F0ED),
        statusFill = Color(0xFF0F2320),
        statusInk = Color(0xFFD8F0ED),
        draftFill = Color(0xFF0F2320),
        draftInk = Color(0xFFD8F0ED),
        inviteFill = Color(0xFF0ECBB0),
        actionInk = Color(0xFF080F0E),
        activateFill = Color(0xFF0ECBB0),
        pillBorder = Color(0xFF1E3530),
        chipOnFill = Color(0xFF0ECBB0),
        chipOnBorder = Color(0xFF0ECBB0),
        chipOnLabel = Color(0xFF080F0E),
        chipOffFill = Color(0xFF0B1917),
        chipOffLabel = Color(0xFF8FBFBA),
        channelOnFill = Color(0xFF0ECBB0),
        channelOnLabel = Color(0xFF080F0E),
        channelOffLabel = Color(0xFF8FBFBA),
        hero = R.drawable.rs_setup_service,
        mode = "Service",
        title = "Consumer Service Setup",
        badge = "Not started yet",
        basicsLine = "A home for this service.",
        nameHint = "Service name",
        descriptionHint = "Everyday notes about this service",
        locationHint = "Add your service location",
        ownerAccess = "Only the owner can change who has access.",
        leadTitle = "Service manager",
        leadBody = "Runs day-to-day work. Does not see full money records unless you allow it.",
        peopleNote = "You can start on your own. Managers and finance people are optional. You can invite them after this starts.",
        channelPrompt = "How do customers reach you? Choose at least one.",
        channels = listOf("Walk-in", "Direct booking", "Platform", "Home service"),
        activate = "Start this service",
        footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
        reviewKind = "Consumer Service",
    )
    else -> SetupLook(
        code = "RETAIL_STORE",
        bg = Color(0xFF0B1020),
        card = Color(0xFF161B2E),
        field = Color(0xFF0B1020),
        border = Color(0xFF2D3554),
        notice = Color(0xFF252654),
        brand = Color(0xFF5B5CEB),
        accent = Color(0xFFA5A7FF),
        number = Color(0xFF565A91),
        secondary = Color(0xFFA1A7C1),
        text = Color.White,
        statusFill = Color(0xFF102C30),
        statusInk = Color(0xFF10B981),
        draftFill = Color(0xFF252654),
        draftInk = Color(0xFFA5A7FF),
        inviteFill = Color(0xFF1E243D),
        actionInk = Color.White,
        activateFill = Color(0xFF5B5CEB),
        pillBorder = Color.Transparent,
        chipOnFill = Color(0xFF252654),
        chipOnBorder = Color(0xFF5B5CEB),
        chipOnLabel = Color(0xFFA5A7FF),
        chipOffFill = Color(0xFF1A2036),
        chipOffLabel = Color(0xFFA1A7C1),
        channelOnFill = Color(0xFF252654),
        channelOnLabel = Color(0xFFA5A7FF),
        channelOffLabel = Color(0xFFA1A7C1),
        hero = R.drawable.rs_setup_store,
        mode = "Retail store",
        title = "Retail Store Setup",
        badge = "Not started yet",
        basicsLine = "A home for this store.",
        nameHint = "Store name",
        descriptionHint = "Everyday notes about this store",
        locationHint = "Add your store location",
        ownerAccess = "Only the owner can change who has access.",
        leadTitle = "Store manager",
        leadBody = "Runs day-to-day work. Does not see full money records unless you allow it.",
        peopleNote = "You can start on your own. Managers and finance people are optional. You can invite them after this starts.",
        channelPrompt = "Where do you sell? Choose at least one.",
        channels = listOf("Store", "Website", "Marketplace", "Social", "Direct"),
        activate = "Start this store",
        footer = "This stays private to your business. Only the owner can change who has access. Nothing is counted until you start.",
        reviewKind = "Retail Store",
    )
}

private val LocalLook = staticCompositionLocalOf { setupLook("RETAIL_STORE") }

private val Bg: Color @Composable get() = LocalLook.current.bg
private val Card: Color @Composable get() = LocalLook.current.card
private val Border: Color @Composable get() = LocalLook.current.border
private val Selected: Color @Composable get() = LocalLook.current.notice
private val Brand: Color @Composable get() = LocalLook.current.brand
private val Accent: Color @Composable get() = LocalLook.current.accent
private val Number: Color @Composable get() = LocalLook.current.number
private val Secondary: Color @Composable get() = LocalLook.current.secondary
private val Success: Color @Composable get() = LocalLook.current.statusInk
private val SuccessBg: Color @Composable get() = LocalLook.current.statusFill
private val Elevated: Color @Composable get() = LocalLook.current.inviteFill
private val Ink: Color @Composable get() = LocalLook.current.text
private val Field: Color @Composable get() = LocalLook.current.field
private val DraftFill: Color @Composable get() = LocalLook.current.draftFill
private val DraftInk: Color @Composable get() = LocalLook.current.draftInk

private val channels: List<String> @Composable get() = LocalLook.current.channels
private val roleCopy: List<Pair<String, String>>
    @Composable get() {
        val look = LocalLook.current
        return listOf(
            "Owner / Admin" to look.ownerAccess,
            look.leadTitle to look.leadBody,
        ) + sharedRoles
    }
private val currencies = listOf(
    "INR" to "INR · Indian Rupee (₹)",
    "USD" to "USD · US Dollar",
    "EUR" to "EUR · Euro",
    "GBP" to "GBP · British Pound",
    "AED" to "AED · UAE Dirham",
)
private val policies = listOf(
    "NONE" to "No approval needed",
    "APPROVAL_REQUIRED" to "Someone must approve",
)
private val dayFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private data class StoreInvite(val name: String, val role: String)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RetailStoreSetup(
    subtypes: List<OperatingSubtype>,
    subtype: OperatingSubtype,
    onSubtype: (OperatingSubtype) -> Unit,
    ownerName: String,
    submitting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onActivate: (Map<String, Any>, String) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialCompanyName: String = "",
    initialGstin: String = "",
) {
    val owner = PersonLabel.personName(ownerName, fallback = "You")
    var name by remember { mutableStateOf(initialName) }
    var companyName by remember { mutableStateOf(initialCompanyName.ifBlank { initialName }) }
    var companyEdited by remember { mutableStateOf(initialCompanyName.isNotBlank()) }
    var gstin by remember { mutableStateOf(initialGstin) }
    var currency by remember { mutableStateOf("INR") }
    var currencyOpen by remember { mutableStateOf(false) }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var invites by remember { mutableStateOf(listOf<StoreInvite>()) }
    var inviting by remember { mutableStateOf(false) }
    var inviteName by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("OPERATIONS_LEAD") }
    var policy by remember { mutableStateOf("NONE") }
    var policyOpen by remember { mutableStateOf(false) }
    var selectedChannels by remember { mutableStateOf(setOf<String>()) }
    var tradeReach by remember { mutableStateOf(setOf<String>()) }
    var parties by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var partyName by remember { mutableStateOf("") }
    var partyKind by remember { mutableStateOf("SUPPLIER") }
    val look = setupLook(subtype.code)
    var budget by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf(subtype.targetMetrics.firstOrNull() ?: "REVENUE") }
    var metricOpen by remember { mutableStateOf(false) }
    var clientFee by remember { mutableStateOf("") }
    var offering by remember { mutableStateOf("") }
    var creativeApproval by remember { mutableStateOf(false) }
    var cadence by remember { mutableStateOf("") }
    var conversionNumerator by remember { mutableStateOf("") }
    var conversionDenominator by remember { mutableStateOf("") }
    val marketingSetup = look.code == "CLIENT_CAMPAIGN" || look.code == "OWN_BUSINESS_CAMPAIGN"
    val clientCampaign = look.code == "CLIENT_CAMPAIGN"
    var localError by remember { mutableStateOf<String?>(null) }
    var howItWorks by remember { mutableStateOf(false) }
    var gstOpen by remember { mutableStateOf(false) }
    var gstTracking by remember { mutableStateOf("NOT_NOW") }
    var gstRegistration by remember { mutableStateOf("REGULAR") }
    var gstState by remember { mutableStateOf("") }
    var salePriceIncludesGst by remember { mutableStateOf("NO") }
    var purchasePriceIncludesGst by remember { mutableStateOf("NO") }
    var gstEffectiveFrom by remember { mutableStateOf("") }
    var howItWorksViaCreate by remember { mutableStateOf(false) }
    val needsApprover = policy == "APPROVAL_REQUIRED" && invites.none { it.role == "APPROVER" }
    val ready = name.isNotBlank() &&
        companyName.isNotBlank() &&
        (look.channelsOptional || selectedChannels.isNotEmpty()) &&
        budget.toDoubleOrNull()?.let { it > 0 } == true &&
        (marketingSetup && target.isBlank() || target.toDoubleOrNull()?.let { it > 0 } == true) &&
        !needsApprover
    LaunchedEffect(name) {
        if (!companyEdited) companyName = name
    }
    LaunchedEffect(initialName, initialCompanyName, initialGstin) {
        if (name.isBlank() && initialName.isNotBlank()) name = initialName
        if (!companyEdited && initialCompanyName.isNotBlank()) {
            companyName = initialCompanyName
            companyEdited = true
        }
        if (gstin.isBlank() && initialGstin.isNotBlank()) gstin = initialGstin
    }
    LaunchedEffect(subtype.code) {
        selectedChannels = selectedChannels.intersect(look.channels.toSet())
        tradeReach = tradeReach.intersect(look.tradeReach.toSet())
        if (look.partyKinds.none { it.first == partyKind }) {
            partyKind = look.partyKinds.firstOrNull()?.first ?: "SUPPLIER"
        }
        if (metric !in look.targetMetrics) metric = look.targetMetrics.firstOrNull() ?: "REVENUE"
        if (look.code != "CLIENT_CAMPAIGN") {
            clientFee = ""
            cadence = ""
        }
    }

    CompositionLocalProvider(LocalLook provides look) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClose)) {
                SetupIcon(R.drawable.rs_setup_close, 14.dp)
                Text("Close", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
            Text(look.mode, color = Accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BoxEmblem()
            Text(look.title, color = Ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, textAlign = TextAlign.Center)
            Text(
                look.intro,
                color = Secondary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            Pill(look.badge, DraftFill, DraftInk)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(18.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Kind of business", color = Secondary, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                subtypes.forEach { item ->
                    val on = item.code == subtype.code
                    Column(
                        Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) look.chipOnFill else look.chipOffFill)
                            .border(1.dp, if (on) look.chipOnBorder else Border, RoundedCornerShape(10.dp))
                            .clickable { onSubtype(item) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                    ) {
                        SetupIcon(modelIcon(item.code), 20.dp)
                        Text(item.label, color = if (on) look.chipOnLabel else look.chipOffLabel, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(look.steps, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            SetupIcon(R.drawable.rs_setup_arrow_down, 16.dp)
        }
        Pill(look.badge, DraftFill, DraftInk)
        SectionCard("01", "Business Basics") {
            Text(look.basicsLine, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 22.sp)
            FieldEditor(look.nameHint, name, look.nameHint, R.drawable.rs_setup_pencil) { name = it }
            FieldEditor("Company name", companyName, "Registered company name", R.drawable.rs_setup_pencil) {
                companyEdited = true
                companyName = it
            }
            FieldEditor("GSTIN", gstin, "15-digit GSTIN, if you have one", R.drawable.rs_setup_pencil) {
                gstin = it.take(15)
            }
            ChoiceRow("Currency", currencies.firstOrNull { it.first == currency }?.second ?: currency, R.drawable.rs_setup_chevron) { currencyOpen = !currencyOpen }
            if (currencyOpen) {
                currencies.forEach { (code, label) ->
                    Text(label, color = if (code == currency) Accent else Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.clickable { currency = code; currencyOpen = false }.padding(vertical = 6.dp))
                }
            }
            ChoiceRow("Start date", start.format(dayFormat), R.drawable.rs_setup_calendar) { dateTarget = "start" }
            ChoiceRow("End date", end?.format(dayFormat) ?: "Still open", R.drawable.rs_setup_calendar) { dateTarget = "end" }
            FieldEditor("Description", description, look.descriptionHint, R.drawable.rs_setup_pencil) { description = it }
            FieldEditor("Location", location, look.locationHint, R.drawable.rs_setup_pin) { location = it }
            if (look.tradeReach.isNotEmpty()) {
                Text("Trade reach", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    look.tradeReach.forEach { reach ->
                        val on = reach in tradeReach
                        Text(
                            if (on) "✓ $reach" else reach,
                            color = if (on) look.channelOnLabel else look.channelOffLabel,
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) look.channelOnFill else Color.Transparent).border(1.dp, if (on) look.chipOnBorder else Border, RoundedCornerShape(8.dp)).clickable {
                                tradeReach = if (on) tradeReach - reach else tradeReach + reach
                            }.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Text("You can fill in the extra details later.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp)
        }
        if (look.partySection.isNotBlank()) {
            SectionCard("03", look.partySection) {
                Text(look.partyNote, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp)
                parties.forEach { (kind, label) ->
                    Text("$label · ${look.partyKinds.firstOrNull { it.first == kind }?.second ?: kind}", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                }
                FieldEditor("Name", partyName, "Counterparty name", R.drawable.rs_setup_pencil) { partyName = it }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    look.partyKinds.forEach { (code, label) ->
                        val on = code == partyKind
                        Text(label, color = if (on) look.channelOnLabel else look.channelOffLabel, fontFamily = PlusJakartaSans, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) look.channelOnFill else Color.Transparent).border(1.dp, if (on) look.chipOnBorder else Border, RoundedCornerShape(999.dp)).clickable { partyKind = code }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Text("Add", color = Ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                    val trimmed = partyName.trim()
                    if (trimmed.isNotEmpty()) {
                        parties = parties + (partyKind to trimmed)
                        partyName = ""
                    }
                })
            }
        }
        SectionCard("02", "People & Roles") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("$owner · You", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 22.sp)
                    Text("Owner", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
                Text(initials(owner), color = Accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Pill("You are the owner", SuccessBg, Success)
            invites.forEach { person ->
                Text("${person.name} · ${roleLabel(person.role)}", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
            }
            Text(
                "+ Invite or add people",
                color = look.actionInk,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Elevated).border(1.dp, if (look.inviteFill == look.activateFill) look.inviteFill else Border, RoundedCornerShape(14.dp)).clickable { inviting = !inviting }.padding(14.dp),
            )
            if (inviting) {
                FieldEditor("Name", inviteName, "Person name", R.drawable.rs_setup_pencil) { inviteName = it }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OperatingMomentCatalog.roles.filter { it.first != "OWNER_ADMIN" }.forEach { (code, label) ->
                        val on = code == inviteRole
                        Text(label, color = if (on) Accent else Secondary, fontFamily = PlusJakartaSans, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) Selected else Color.Transparent).border(1.dp, if (on) Brand else Border, RoundedCornerShape(999.dp)).clickable { inviteRole = code }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Text("Add", color = Ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                    val trimmed = inviteName.trim()
                    if (trimmed.isNotEmpty()) {
                        invites = invites + StoreInvite(trimmed, inviteRole)
                        inviteName = ""
                        inviting = false
                    }
                })
            }
            Text(look.peopleNote, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp)
            Text("Who can do what", color = Ink, fontFamily = PlusJakartaSans, fontSize = 16.sp, lineHeight = 24.sp)
            roleCopy.forEach { (title, body) ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(title, color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                    Text(body, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                }
            }
            ChoiceRow("Who needs to approve before this starts", policies.first { it.first == policy }.second, R.drawable.rs_setup_chevron) { policyOpen = !policyOpen }
            if (policyOpen) {
                policies.forEach { (code, label) ->
                    Text(label, color = if (code == policy) Accent else Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.clickable { policy = code; policyOpen = false }.padding(vertical = 6.dp))
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Selected).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (needsApprover) "Someone needs to approve" else "$owner can start this",
                    color = Accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    if (needsApprover) "Add a person who can approve, or choose no approval needed."
                    else "You only need someone else to approve if you turn that on. A manager or finance person is not required to start.",
                    color = Secondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
            Text("Only the owner can change who has access.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp)
        }
        SectionCard(if (look.partySection.isBlank()) "03" else "04", "Channels & Monthly Plan") {
            Text(look.channelPrompt, color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 22.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                channels.forEach { channel ->
                    val on = channel in selectedChannels
                    Text(
                        if (on) "✓ $channel" else channel,
                        color = if (on) look.channelOnLabel else look.channelOffLabel,
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = if (on) 11.sp else 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(if (on) 999.dp else 8.dp)).background(if (on) look.channelOnFill else Color.Transparent).border(if (on) 0.dp else 1.dp, Border, RoundedCornerShape(if (on) 999.dp else 8.dp)).clickable {
                            selectedChannels = if (on) selectedChannels - channel else selectedChannels + channel
                        }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Text(
                when {
                    look.channelsOptional && selectedChannels.isEmpty() -> look.channelHelp
                    selectedChannels.isEmpty() -> "Choose at least one."
                    else -> "${selectedChannels.size} selected. ${look.channelHelp}"
                },
                color = Secondary,
                fontFamily = PlusJakartaSans,
                fontSize = 12.sp,
            )
            ChoiceRow("Month", start.format(monthFormat), R.drawable.rs_setup_calendar) { dateTarget = "start" }
            FieldEditor("Monthly budget", budget, "Amount", R.drawable.rs_setup_pencil) { budget = it.filter { ch -> ch.isDigit() || ch == '.' } }
            FieldEditor(
                if (marketingSetup) "Target (optional until you choose a measure)" else "Monthly sales target",
                target,
                "Amount",
                R.drawable.rs_setup_pencil,
            ) { target = it.filter { ch -> ch.isDigit() || ch == '.' } }
            if (look.targetMetrics.size > 1) {
                ChoiceRow("How the target is measured", metricLabel(metric, look.targetMetrics), R.drawable.rs_setup_chevron) { metricOpen = !metricOpen }
                if (metricOpen) {
                    look.targetMetrics.forEach { code ->
                        Text(metricLabel(code, look.targetMetrics), color = if (code == metric) Accent else Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.clickable { metric = code; metricOpen = false }.padding(vertical = 6.dp))
                    }
                }
            }
            if (marketingSetup) {
                FieldEditor("Offering", offering, "Optional product or service", R.drawable.rs_setup_pencil) { offering = it }
                ChoiceRow("Creative approval", if (creativeApproval) "Required" else "Not required", R.drawable.rs_setup_chevron) {
                    creativeApproval = !creativeApproval
                }
                if (metric == "CONVERSIONS") {
                    FieldEditor("Conversion numerator", conversionNumerator, "What counts as a conversion", R.drawable.rs_setup_pencil) { conversionNumerator = it }
                    FieldEditor("Conversion denominator", conversionDenominator, "What it is divided by", R.drawable.rs_setup_pencil) { conversionDenominator = it }
                }
            }
            if (clientCampaign) {
                FieldEditor("Client fee", clientFee, "Stored only. This is not an invoice.", R.drawable.rs_setup_pencil) { clientFee = it.filter { ch -> ch.isDigit() || ch == '.' } }
                ChoiceRow("Reporting cadence", cadence.ifBlank { "None" }, R.drawable.rs_setup_chevron) {
                    cadence = when (cadence) {
                        "" -> "WEEKLY"
                        "WEEKLY" -> "MONTHLY"
                        "MONTHLY" -> "CUSTOM"
                        else -> ""
                    }
                }
                Text("A cadence adds one due activity. It does not file a report.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Selected).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How your target is counted", color = Accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(look.targetNote, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp)
            }
            if (ready) Pill("Budget and sales target look good", SuccessBg, Success)
        }
        if (look.code == "TRADING_WHOLESALE" || look.code == "DISTRIBUTION_DEALERSHIP") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(18.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GST & Tax Details", color = Ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Set up GST once to make buying and selling easier.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                Text(
                    if (gstOpen) "Hide GST setup" else "Set up GST (optional)",
                    color = Accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { gstOpen = !gstOpen },
                )
                if (gstOpen) {
                    Text("Do you want to track GST in this Moment?", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("YES" to "Yes", "NOT_NOW" to "Not now").forEach { (code, label) ->
                            Text(label, color = if (gstTracking == code) Accent else Ink, modifier = Modifier.clickable { gstTracking = code }.padding(vertical = 6.dp))
                        }
                    }
                    Text("Skipping means tax tracking is disabled in Momentra. It does not mean the business is legally GST-exempt.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                    if (gstTracking == "YES") {
                        Text("How is your business registered for GST?", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                        listOf("REGULAR" to "Regular", "COMPOSITION" to "Composition", "UNREGISTERED" to "Unregistered", "NOT_SURE" to "Not sure").forEach { (code, label) ->
                            Text(label, color = if (gstRegistration == code) Accent else Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.clickable { gstRegistration = code }.padding(vertical = 4.dp))
                        }
                        FieldEditor("Which state is your business registered in?", gstState, "State", R.drawable.rs_setup_pin) { gstState = it }
                        Text("How do you usually enter selling prices?", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                        listOf("YES" to "Prices include GST", "NO" to "GST is added separately").forEach { (code, label) ->
                            Text(label, color = if (salePriceIncludesGst == code) Accent else Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.clickable { salePriceIncludesGst = code }.padding(vertical = 4.dp))
                        }
                        Text("How do you usually enter purchase prices?", color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                        listOf("YES" to "Prices include GST", "NO" to "GST is added separately").forEach { (code, label) ->
                            Text(label, color = if (purchasePriceIncludesGst == code) Accent else Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp, modifier = Modifier.clickable { purchasePriceIncludesGst = code }.padding(vertical = 4.dp))
                        }
                        Text("Your GSTIN stays the one entered above. It is stored as a reference only.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
                        FieldEditor("Effective from (optional)", gstEffectiveFrom, "YYYY-MM-DD", R.drawable.rs_setup_pin) { gstEffectiveFrom = it.take(10) }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Selected).border(1.dp, Border, RoundedCornerShape(18.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Review your setup", color = Ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)
            Column {
                Text(name.ifBlank { look.nameHint }, color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                Text(
                    "${look.reviewKind} · $currency · From ${start.format(dayFormat)}${end?.let { " to ${it.format(dayFormat)}" } ?: " · Still open"}",
                    color = Secondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                )
            }
            ReviewLine("Company", companyName.ifBlank { "Not set" })
            ReviewLine("GSTIN", gstin.ifBlank { "Not added" })
            if (tradeReach.isNotEmpty()) ReviewLine("Trade reach", tradeReach.joinToString())
            if (parties.isNotEmpty()) ReviewLine("Counterparties", parties.joinToString { it.second })
            ReviewLine("People", (listOf("$owner · Owner") + invites.map { "${it.name} · ${roleLabel(it.role)}" }).joinToString(", "))
            Text("Sales channels · ${selectedChannels.joinToString().ifBlank { "None yet" }}", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            ReviewLine("${start.format(monthFormat)} budget", money(currency, budget))
            ReviewLine("${start.format(monthFormat)} sales target", money(currency, target))
            if (look.targetMetrics.size > 1) ReviewLine("Target measure", metricLabel(metric, look.targetMetrics))
        }
        if (ready) {
            Pill("Ready to start", SuccessBg, Success)
            Text("Name, currency, owner, and sales channels are set. Budget and sales target are filled in.", color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
        }
        val shown = plainSetupError(localError ?: error)
        if (!shown.isNullOrBlank()) Text(shown, color = Color(0xFFF87171), fontFamily = PlusJakartaSans, fontSize = 13.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(look.activateFill.copy(alpha = if (submitting) 0.45f else 1f))
                .testTag(MaestroIds.BUSINESS_SETUP_SUBMIT)
                .clickable(enabled = !submitting) {
                    val problem = when {
                        name.isBlank() -> "Add a name."
                        companyName.isBlank() -> "Add a company name."
                        !look.channelsOptional && selectedChannels.isEmpty() -> "Choose at least one."
                        budget.toDoubleOrNull()?.let { it > 0 } != true -> "Enter a monthly budget above zero."
                        !(marketingSetup && target.isBlank()) && target.toDoubleOrNull()?.let { it > 0 } != true -> "Enter a monthly sales target above zero."
                        needsApprover -> "Add someone who can approve, or choose no approval needed."
                        else -> null
                    }
                    localError = problem
                    if (problem != null) return@clickable
                    val trimmed = name.trim()
                    onActivate(
                        mapOf(
                            "subtype" to look.code,
                            "identityName" to trimmed,
                            "partyName" to trimmed,
                            "description" to description.trim(),
                            "currency" to currency,
                            "startDate" to start.toString(),
                            "endDate" to (end?.toString() ?: ""),
                            "location" to location.trim(),
                            "activationPolicy" to policy,
                            "participants" to invites.map { mapOf("displayName" to it.name, "role" to it.role) },
                            "counterparties" to parties.map { mapOf("name" to it.second, "kind" to it.first) },
                            "channels" to selectedChannels.toList(),
                            "tradeReach" to tradeReach.toList(),
                            "monthlyBudget" to budget.trim(),
                            "monthlyTarget" to target.trim(),
                            "targetMetric" to metric,
                            "clientFee" to if (clientCampaign) clientFee.trim() else "",
                            "offering" to if (marketingSetup) offering.trim() else "",
                            "creativeApprovalRequired" to if (marketingSetup && creativeApproval) "YES" else "",
                            "reportingCadence" to if (clientCampaign) cadence else "",
                            "conversionNumerator" to if (marketingSetup) conversionNumerator.trim() else "",
                            "conversionDenominator" to if (marketingSetup) conversionDenominator.trim() else "",
                            "conversionDefinitionVersion" to if (marketingSetup && conversionNumerator.isNotBlank()) "1" else "",
                            "campaignStatus" to "OPEN",
                            "companyName" to companyName.trim(),
                            "gstin" to gstin.trim(),
                            "gstTracking" to if (look.code == "TRADING_WHOLESALE" || look.code == "DISTRIBUTION_DEALERSHIP") gstTracking else "",
                            "gstRegistration" to gstRegistration,
                            "gstState" to gstState.trim(),
                            "salePriceIncludesGst" to salePriceIncludesGst,
                            "purchasePriceIncludesGst" to purchasePriceIncludesGst,
                            "gstEffectiveFrom" to gstEffectiveFrom.trim(),
                        ),
                        trimmed,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (submitting) "Starting…" else look.activate,
                color = look.actionInk,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        val howCopy = TradingHowItWorksCopy.forSubtype(look.code)
        if (howCopy != null) {
            Text(
                howCopy.context,
                color = Secondary,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Text(
                "See how it works",
                color = look.brand,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        howItWorksViaCreate = false
                        howItWorks = true
                        trackWidget(AnalyticsScreens.BUSINESS_SETUP_RUNWAY, "how_it_works_opened", look.code)
                    }
                    .padding(vertical = 8.dp)
                    .semantics { role = Role.Button },
            )
        }
        Text(look.footer, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp, lineHeight = 18.sp)
    }

    if (dateTarget != null) {
        val editingEnd = dateTarget == "end"
        val state = rememberDatePickerState(
            initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(if (editingEnd) end ?: start else start),
        )
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val picked = SetupDateTimeUtils.millisToLocalDate(it)
                        if (editingEnd) end = picked else start = picked
                    }
                    dateTarget = null
                }) { Text("Done") }
            },
            dismissButton = {
                if (editingEnd) {
                    TextButton(onClick = { end = null; dateTarget = null }) { Text("Clear") }
                } else {
                    TextButton(onClick = { dateTarget = null }) { Text("Cancel") }
                }
            },
        ) { DatePicker(state = state) }
    }
    if (howItWorks) {
        TradingHowItWorksCopy.forSubtype(look.code)?.let { copy ->
            TradingHowItWorksSheet(
                copy = copy,
                accent = look.brand,
                ink = look.text,
                actionInk = look.actionInk,
                onExpand = {
                    trackWidget(AnalyticsScreens.BUSINESS_SETUP_RUNWAY, "how_it_works_expanded", look.code)
                },
                onCreate = {
                    howItWorksViaCreate = true
                    trackWidget(AnalyticsScreens.BUSINESS_SETUP_RUNWAY, "how_it_works_create_clicked", look.code)
                    howItWorks = false
                },
                onClose = {
                    if (!howItWorksViaCreate) {
                        trackWidget(AnalyticsScreens.BUSINESS_SETUP_RUNWAY, "how_it_works_closed", look.code)
                    }
                    howItWorksViaCreate = false
                    howItWorks = false
                },
            )
        }
    }
    }
}

@Composable
private fun BoxEmblem() {
    Column(
        Modifier.size(88.dp).clip(RoundedCornerShape(44.dp)).background(Selected).border(1.dp, Border, RoundedCornerShape(44.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { SetupIcon(LocalLook.current.hero, 38.dp) }
}

@Composable
private fun SectionCard(number: String, title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(18.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(number, color = Number, fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp)
            Text(title, color = Ink, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)
        }
        content()
    }
}

@Composable
private fun FieldRow(label: String, value: String, filled: Boolean, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Field).border(1.dp, Border, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            Text(value, color = if (filled) Ink else Secondary, fontFamily = PlusJakartaSans, fontSize = 14.sp, lineHeight = 22.sp)
        }
        SetupIcon(icon, 16.dp)
    }
}

@Composable
private fun ChoiceRow(label: String, value: String, icon: Int, onClick: () -> Unit) {
    FieldRow(label, value, true, icon, onClick)
}

@Composable
private fun FieldEditor(label: String, value: String, placeholder: String, icon: Int, onValue: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Field).border(1.dp, Border, RoundedCornerShape(8.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            BasicTextField(
                value = value,
                onValueChange = onValue,
                textStyle = TextStyle(color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                cursorBrush = SolidColor(Brand),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, color = Secondary, fontFamily = PlusJakartaSans, fontSize = 14.sp)
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SetupIcon(icon, 16.dp)
    }
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Ink, fontFamily = PlusJakartaSans, fontSize = 14.sp)
        Text(value, color = Accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun Pill(text: String, background: Color, foreground: Color) {
    val stroke = LocalLook.current.pillBorder
    Text(
        text,
        color = foreground,
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(if (stroke.alpha == 0f) 0.dp else 1.dp, stroke, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun SetupIcon(res: Int, size: Dp) {
    Image(painter = painterResource(res), contentDescription = null, modifier = Modifier.size(size))
}

private fun modelIcon(code: String): Int = when (code) {
    "RESTAURANT_FOOD" -> R.drawable.rs_setup_utensils
    "CONSUMER_SERVICE" -> R.drawable.rs_setup_handshake
    "TRADING_WHOLESALE" -> R.drawable.rs_setup_trading
    "DISTRIBUTION_DEALERSHIP" -> R.drawable.rs_setup_distribution
    "CLIENT_CAMPAIGN", "OWN_BUSINESS_CAMPAIGN" -> R.drawable.rs_setup_campaign
    "STANDARD_PRODUCTION" -> R.drawable.rs_setup_standard
    "MADE_TO_ORDER" -> R.drawable.rs_setup_jobwork
    else -> R.drawable.rs_setup_store_chip
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    val letters = parts.take(2).map { it.first().uppercaseChar() }.joinToString("")
    return letters.ifBlank { "Y" }
}

private fun roleLabel(code: String): String = OperatingMomentCatalog.roles.firstOrNull { it.first == code }?.second ?: code

private fun plainSetupError(message: String?): String? {
    if (message.isNullOrBlank()) return null
    val hasCode = Regex("\\b[A-Z][A-Z0-9_]{2,}\\b").containsMatchIn(message)
    return if (message.contains("\\n") || message.contains("{") || message.contains("/") || hasCode) {
        "We couldn't start this yet. Check the name, budget, and sales target, then try again."
    } else {
        message
    }
}

private fun money(currency: String, amount: String): String {
    if (amount.isBlank()) return "—"
    return if (currency == "INR") "₹$amount" else "$currency $amount"
}
