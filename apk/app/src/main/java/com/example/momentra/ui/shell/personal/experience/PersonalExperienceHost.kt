package com.example.momentra.ui.shell.personal.experience

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.momentra.R
import com.example.momentra.data.api.PersonalActivityRowDto
import com.example.momentra.data.api.PersonalExperienceDto
import com.example.momentra.data.api.PersonalExpenseConfirmationDto
import com.example.momentra.data.api.PersonalExpensePhotoBody
import com.example.momentra.data.api.PersonalLifeExpenseBody
import com.example.momentra.data.api.PersonalLifeExpenseCorrectionBody
import com.example.momentra.data.api.PersonalMomentCardDto
import com.example.momentra.data.api.PersonalSignalDto
import com.example.momentra.data.api.PersonalSnapshotDto
import com.example.momentra.data.repository.PersonalExperienceRepository
import com.example.momentra.domain.BottomDestination
import com.example.momentra.ui.theme.generated.PersonalLifeArea
import com.example.momentra.ui.theme.generated.PersonalLifeLayer
import com.example.momentra.ui.theme.generated.PersonalLifeTaxonomy
import com.example.momentra.ui.theme.generated.PersonalMood
import com.example.momentra.ui.theme.generated.PersonalTheme
import com.example.momentra.ui.theme.generated.PersonalTokens
import com.example.momentra.ui.shell.personal.lifeops.create.LifeOpsQuickAddKind
import com.example.momentra.ui.shell.personal.lifeops.create.MoneyQuickAddKind
import com.example.momentra.ui.shell.personal.shared.PersonalQuickAddHub
import kotlin.math.abs
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ExpenseDraft(
    val amount: String = "",
    val merchant: String = "",
    val effectiveAt: String = OffsetDateTime.now().toString(),
    val areaCode: String? = null,
    val subcategoryCode: String? = null,
    val moodCode: String? = null,
    val note: String = "",
    val paymentSource: String = "",
    val showMore: Boolean = false,
    val editingId: String? = null,
    val photoPreview: ImageBitmap? = null,
    val photoBytes: ByteArray? = null,
)

private sealed class ExperienceFlow {
    data object Browse : ExperienceFlow()
    data class Sheet(val draft: ExpenseDraft) : ExperienceFlow()
    data class Area(val draft: ExpenseDraft) : ExperienceFlow()
    data class Subcategory(val draft: ExpenseDraft) : ExperienceFlow()
    data class Saved(val confirmation: PersonalExpenseConfirmationDto) : ExperienceFlow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalExperienceHost(
    destination: BottomDestination,
    hasActiveMoment: Boolean = false,
    momentTypeCode: String? = null,
    capabilityCodes: List<String> = emptyList(),
    onOpenLife: () -> Unit = {},
    onOpenPulse: () -> Unit = {},
    onOpenMoments: () -> Unit = {},
    onLifeOpsQuickAdd: (LifeOpsQuickAddKind) -> Unit = {},
    onMoneyQuickAdd: (MoneyQuickAddKind) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val repo = remember { PersonalExperienceRepository() }
    val scope = rememberCoroutineScope()
    var experience by remember { mutableStateOf<PersonalExperienceDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var flow by remember { mutableStateOf<ExperienceFlow>(ExperienceFlow.Browse) }
    var busy by remember { mutableStateOf(false) }
    var pendingMoodScroll by remember { mutableStateOf(false) }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    var areasY by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = experience == null
        error = null
        runCatching { repo.experience() }
            .onSuccess { experience = it; loading = false }
            .onFailure { error = it.message ?: "Could not load Personal."; loading = false }
    }

    fun openExpense(prefillArea: String? = null, editing: PersonalActivityRowDto? = null) {
        val draft = if (editing != null) {
            ExpenseDraft(
                amount = editing.amount,
                merchant = editing.merchantName,
                effectiveAt = editing.effectiveAt,
                areaCode = editing.areaCode,
                subcategoryCode = editing.subcategoryCode,
                moodCode = editing.moodCode,
                editingId = editing.expenseId,
            )
        } else {
            ExpenseDraft(areaCode = prefillArea)
        }
        flow = if (prefillArea != null && editing == null) ExperienceFlow.Subcategory(draft) else ExperienceFlow.Sheet(draft)
    }

    PersonalTheme {
        Box(
            modifier
                .fillMaxSize()
                .background(PersonalTokens.Colors.BgPrimary),
        ) {
            when (val current = flow) {
                ExperienceFlow.Browse, is ExperienceFlow.Sheet -> {
                    if (loading && experience == null) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = PersonalTokens.Colors.BrandAccent)
                    } else {
                        val data = experience
                        val scroll = rememberScrollState()
                        Column(
                            Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { viewportTop = it.positionInRoot().y }
                                .verticalScroll(scroll)
                                .padding(20.dp),
                        ) {
                            if (error != null) {
                                Text(error!!, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                                Spacer(Modifier.height(PersonalTokens.Space.Sm))
                            }
                            when (destination) {
                                BottomDestination.CREATE -> PersonalQuickAddHub(
                                    hasActiveMoment = hasActiveMoment,
                                    momentTypeCode = momentTypeCode,
                                    capabilityCodes = capabilityCodes.takeIf { it.isNotEmpty() },
                                    onSpend = { onMoneyQuickAdd(MoneyQuickAddKind.MASTER_EXPENSE) },
                                    onIncome = { onMoneyQuickAdd(MoneyQuickAddKind.INCOME) },
                                    onRecovery = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.RECOVERY) },
                                    onMood = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.MOOD) },
                                    onAttention = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.ATTENTION) },
                                    onAdjust = { onLifeOpsQuickAdd(LifeOpsQuickAddKind.ADJUST) },
                                    onTransfer = { onMoneyQuickAdd(MoneyQuickAddKind.TRANSFER) },
                                    onSavings = { onMoneyQuickAdd(MoneyQuickAddKind.SAVINGS) },
                                )
                                BottomDestination.PULSE -> PulseBody(
                                    data,
                                    scroll = scroll,
                                    onExpense = { openExpense() },
                                    onEdit = { openExpense(editing = it) },
                                    onOpenLife = onOpenLife,
                                    scrollToMood = pendingMoodScroll,
                                    onMoodScrolled = { pendingMoodScroll = false },
                                )
                                BottomDestination.MOMENTS -> MomentsBody(
                                    data,
                                    scroll = scroll,
                                    onExpense = { openExpense() },
                                    onOpenLife = onOpenLife,
                                )
                                BottomDestination.LIFE -> LifeBody(
                                    data,
                                    onExpense = { openExpense() },
                                    onArea = { openExpense(it) },
                                    onSeeAreas = { scope.launch { scroll.animateScrollTo(areasY) } },
                                    onCompare = onOpenPulse,
                                    onExploreMoods = {
                                        pendingMoodScroll = true
                                        onOpenPulse()
                                    },
                                    areasModifier = Modifier.onGloballyPositioned {
                                        areasY = (it.positionInRoot().y - viewportTop + scroll.value).toInt().coerceAtLeast(0)
                                    },
                                )
                                BottomDestination.MEMORY -> MemoryBody(data, onExpense = { openExpense() }, onOpenMoments = onOpenMoments)
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                    if (current is ExperienceFlow.Sheet) {
                        ModalBottomSheet(
                            onDismissRequest = { flow = ExperienceFlow.Browse },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = PersonalTokens.Colors.BgPrimary,
                            dragHandle = null,
                            shape = RoundedCornerShape(topStart = PersonalTokens.Radius.Sheet, topEnd = PersonalTokens.Radius.Sheet),
                        ) {
                            ExpenseSheet(
                    draft = current.draft,
                    busy = busy,
                    error = error,
                    onChange = { flow = ExperienceFlow.Sheet(it) },
                    onClose = { flow = ExperienceFlow.Browse },
                    onPickArea = { flow = ExperienceFlow.Area(current.draft) },
                    onSave = {
                        scope.launch {
                            busy = true
                            error = null
                            val draft = current.draft
                            val result = runCatching {
                                if (draft.editingId != null) {
                                    repo.correct(
                                        draft.editingId,
                                        PersonalLifeExpenseCorrectionBody(
                                            draft.areaCode!!,
                                            draft.subcategoryCode!!,
                                            draft.moodCode,
                                            draft.merchant.ifBlank { null },
                                            photo = draft.photoBytes?.let { expensePhoto(it) },
                                        ),
                                    )
                                } else {
                                    repo.create(
                                        PersonalLifeExpenseBody(
                                            amount = normalizedAmount(draft.amount),
                                            currencyCode = "INR",
                                            merchantName = draft.merchant.ifBlank { null },
                                            effectiveAt = draft.effectiveAt,
                                            note = draft.note.ifBlank { null },
                                            lifeAreaCode = draft.areaCode!!,
                                            lifeSubcategoryCode = draft.subcategoryCode!!,
                                            moodCode = draft.moodCode,
                                            paymentSource = draft.paymentSource.ifBlank { null },
                                            photo = draft.photoBytes?.let { expensePhoto(it) },
                                        ),
                                    )
                                }
                            }
                            busy = false
                            result.onSuccess {
                                reload += 1
                                flow = ExperienceFlow.Saved(it)
                            }.onFailure { error = it.message ?: "Could not save the expense." }
                        }
                    },
                    onRefund = {
                        val id = current.draft.editingId ?: return@ExpenseSheet
                        scope.launch {
                            busy = true
                            runCatching { repo.refund(id) }
                                .onSuccess { reload += 1; flow = ExperienceFlow.Browse }
                                .onFailure { error = it.message ?: "Could not record the refund." }
                            busy = false
                        }
                    },
                    onVoid = {
                        val id = current.draft.editingId ?: return@ExpenseSheet
                        scope.launch {
                            busy = true
                            runCatching { repo.voidExpense(id) }
                                .onSuccess { reload += 1; flow = ExperienceFlow.Browse }
                                .onFailure { error = it.message ?: "Could not void the expense." }
                            busy = false
                        }
                    },
                )
                        }
                    }
                }
                is ExperienceFlow.Area -> AreaPicker(
                    draft = current.draft,
                    onBack = { flow = ExperienceFlow.Sheet(current.draft) },
                    onClose = { flow = ExperienceFlow.Browse },
                    onSelect = { area ->
                        val cleared = if (current.draft.areaCode != area.code) current.draft.copy(areaCode = area.code, subcategoryCode = null) else current.draft.copy(areaCode = area.code)
                        flow = ExperienceFlow.Subcategory(cleared)
                    },
                )
                is ExperienceFlow.Subcategory -> SubcategoryPicker(
                    draft = current.draft,
                    onBack = { flow = ExperienceFlow.Area(current.draft) },
                    onClose = { flow = ExperienceFlow.Browse },
                    onChangeArea = { flow = ExperienceFlow.Area(current.draft) },
                    onSelect = { sub -> flow = ExperienceFlow.Sheet(current.draft.copy(areaCode = sub.areaCode, subcategoryCode = sub.code)) },
                )
                is ExperienceFlow.Saved -> SavedScreen(
                    confirmation = current.confirmation,
                    busy = busy,
                    error = error,
                    onPhoto = { bytes ->
                        val c = current.confirmation
                        scope.launch {
                            busy = true
                            error = null
                            runCatching {
                                repo.correct(
                                    c.expenseId,
                                    PersonalLifeExpenseCorrectionBody(
                                        lifeAreaCode = c.lifeAreaCode,
                                        lifeSubcategoryCode = c.lifeSubcategoryCode,
                                        moodCode = c.moodCode,
                                        merchantName = c.merchantName,
                                        photo = expensePhoto(bytes),
                                    ),
                                )
                            }.onSuccess {
                                reload += 1
                                flow = ExperienceFlow.Saved(it)
                            }.onFailure { error = it.message ?: "Could not save the photo." }
                            busy = false
                        }
                    },
                    onView = {
                        val c = current.confirmation
                        flow = ExperienceFlow.Sheet(
                            ExpenseDraft(
                                amount = c.amount,
                                merchant = c.merchantName.orEmpty(),
                                effectiveAt = c.effectiveAt,
                                areaCode = c.lifeAreaCode,
                                subcategoryCode = c.lifeSubcategoryCode,
                                moodCode = c.moodCode,
                                editingId = c.expenseId,
                            ),
                        )
                    },
                    onEdit = {
                        val c = current.confirmation
                        flow = ExperienceFlow.Area(
                            ExpenseDraft(
                                amount = c.amount,
                                merchant = c.merchantName.orEmpty(),
                                effectiveAt = c.effectiveAt,
                                areaCode = c.lifeAreaCode,
                                subcategoryCode = c.lifeSubcategoryCode,
                                moodCode = c.moodCode,
                                editingId = c.expenseId,
                            ),
                        )
                    },
                    onBack = {
                        flow = ExperienceFlow.Browse
                        onOpenPulse()
                    },
                )
            }
        }
    }
}

@Composable
private fun ScreenIntro(kicker: String, title: String, subtitle: String, onExpense: (() -> Unit)?) {
    Text(kicker, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.StoryKicker)
    Spacer(Modifier.height(PersonalTokens.Space.Sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero, modifier = Modifier.weight(1f))
        if (onExpense != null) {
            Text(
                "+ Expense",
                color = PersonalTokens.Colors.TextOnAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
                    .background(PersonalTokens.Colors.BrandAccent)
                    .clickable(onClick = onExpense)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(subtitle, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
    Spacer(Modifier.height(PersonalTokens.Space.Compact))
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Text(title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section, modifier = modifier)
    if (subtitle != null) {
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
    }
    Spacer(Modifier.height(PersonalTokens.Space.Sm))
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PersonalTokens.Radius.Card))
            .background(PersonalTokens.Colors.SurfaceContainer)
            .border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Card))
            .padding(16.dp),
    ) { content() }
}


@Composable
private fun ExpenseSheet(
    draft: ExpenseDraft,
    busy: Boolean,
    error: String?,
    onChange: (ExpenseDraft) -> Unit,
    onClose: () -> Unit,
    onPickArea: () -> Unit,
    onSave: () -> Unit,
    onRefund: () -> Unit,
    onVoid: () -> Unit,
) {
    val context = LocalContext.current
    val area = draft.areaCode?.let { PersonalLifeTaxonomy.area(it) }
    val sub = draft.subcategoryCode?.let { PersonalLifeTaxonomy.subcategory(it) }
    val canSave = !busy && draft.amount.toDoubleOrNull()?.let { it > 0 } == true && area != null && sub != null
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val picked = uri?.let { decodePhoto(context, it) } ?: return@rememberLauncherForActivityResult
        onChange(draft.copy(photoPreview = picked.preview, photoBytes = picked.bytes))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                PersonalTokens.Colors.BorderHairline,
                RoundedCornerShape(topStart = PersonalTokens.Radius.Sheet, topEnd = PersonalTokens.Radius.Sheet),
            )
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
                .background(PersonalTokens.Colors.TextSubtle),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (draft.editingId == null) "Add expense" else "Edit life context",
                style = PersonalTokens.Type.Section,
                color = PersonalTokens.Colors.TextPrimary,
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
                    .background(PersonalTokens.Colors.SurfaceContainer)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Close", style = PersonalTokens.Type.Metadata, color = PersonalTokens.Colors.TextSecondary)
                Image(
                    painterResource(R.drawable.ic_pers_x),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary),
                )
            }
        }
        Text("AMOUNT · INR · REQUIRED", style = PersonalTokens.Type.Metadata, color = PersonalTokens.Colors.BrandPrimary)
        if (draft.editingId == null) {
            BasicTextField(
                value = draft.amount,
                onValueChange = { onChange(draft.copy(amount = sanitizeAmount(it))) },
                textStyle = PersonalTokens.Type.AmountXL.copy(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatSheetAmount(draft.amount), style = PersonalTokens.Type.AmountXL, color = PersonalTokens.Colors.TextPrimary)
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.width(2.dp).height(42.dp).background(PersonalTokens.Colors.BrandPrimary))
                        Box(Modifier.size(0.dp)) { inner() }
                    }
                },
            )
        } else {
            Text(formatSheetAmount(draft.amount), style = PersonalTokens.Type.AmountXL, color = PersonalTokens.Colors.TextPrimary)
        }
        SheetValueRow(icon = R.drawable.ic_pers_store, caption = "Paid to / For · recommended", trailing = R.drawable.ic_pers_chevron_down) {
            BasicTextField(
            value = draft.merchant,
            onValueChange = { onChange(draft.copy(merchant = it)) },
                textStyle = PersonalTokens.Type.Body.copy(color = PersonalTokens.Colors.TextPrimary),
                cursorBrush = SolidColor(PersonalTokens.Colors.BrandPrimary),
            modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box {
                        if (draft.merchant.isEmpty()) {
                            Text("Studio Fit · strength class", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextSubtle)
                        }
                        inner()
                    }
                },
            )
        }
        SheetValueRow(
            icon = R.drawable.ic_pers_calendar_days,
            caption = "Date & time · editable default",
            value = formatSheetWhen(draft.effectiveAt),
            trailing = R.drawable.ic_pers_chevron_down,
            onClick = {
                pickDateTime(context, draft.effectiveAt) { onChange(draft.copy(effectiveAt = it)) }
            },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PersonalTokens.Radius.TileSelected))
                .background(PersonalTokens.Colors.SurfaceHighlight)
                .border(1.dp, PersonalTokens.Colors.BrandPrimary, RoundedCornerShape(PersonalTokens.Radius.TileSelected))
                .clickable(onClick = onPickArea)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("One Life Area + its subcategory · required", style = PersonalTokens.Type.Caption, color = PersonalTokens.Colors.TextSubtle)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (area != null) {
                    Image(
                        painterResource(area.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextPrimary),
                    )
                }
                Text(area?.label ?: "Choose a Life Area", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextPrimary, modifier = Modifier.weight(1f))
                Image(painterResource(R.drawable.ic_pers_chevron_down), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sub?.label ?: "Then one subcategory", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.BrandPrimary, modifier = Modifier.weight(1f))
                Image(painterResource(R.drawable.ic_pers_chevron_down), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
            }
        }
        if (area != null) {
            Text("${area.layer.code} · derived from ${area.label}", style = PersonalTokens.Type.Caption, color = PersonalTokens.Colors.TextSubtle)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("How did it feel?", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextPrimary)
                Text("Optional", style = PersonalTokens.Type.Caption, color = PersonalTokens.Colors.TextSubtle)
            }
            Text(
                "Skip",
                style = PersonalTokens.Type.Metadata,
                color = PersonalTokens.Colors.BrandPrimary,
                modifier = Modifier.clickable { onChange(draft.copy(moodCode = null)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PersonalMood.entries.forEach { mood ->
                val selected = draft.moodCode == mood.code
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .clip(RoundedCornerShape(PersonalTokens.Radius.Tile))
                        .background(if (selected) PersonalTokens.Colors.BrandAccent else PersonalTokens.Colors.SurfaceContainer)
                        .border(
                            1.dp,
                            if (selected) PersonalTokens.Colors.BrandPrimary else PersonalTokens.Colors.BorderHairline,
                            RoundedCornerShape(PersonalTokens.Radius.Tile),
                        )
                        .clickable { onChange(draft.copy(moodCode = mood.code)) }
                        .padding(4.dp),
                ) {
                    Image(
                        painterResource(mood.iconRes),
                        contentDescription = mood.label,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(if (selected) PersonalTokens.Colors.TextOnAccent else PersonalTokens.Colors.TextSecondary),
                    )
                    Text(
                        if (selected) "${mood.label} ✓" else mood.label,
                        color = if (selected) PersonalTokens.Colors.TextOnAccent else PersonalTokens.Colors.TextSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
        SheetLinkRow(
            icon = R.drawable.ic_pers_paperclip,
            title = "More details",
            caption = "Note · payment source · receipt",
            onClick = { onChange(draft.copy(showMore = !draft.showMore)) },
        )
        if (draft.showMore) {
            SheetValueRow(icon = R.drawable.ic_pers_file_text, caption = "Note") {
                BasicTextField(
                value = draft.note,
                onValueChange = { onChange(draft.copy(note = it)) },
                    textStyle = PersonalTokens.Type.Body.copy(color = PersonalTokens.Colors.TextPrimary),
                    cursorBrush = SolidColor(PersonalTokens.Colors.BrandPrimary),
                modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (draft.note.isEmpty()) Text("Add a note", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextSubtle)
                            inner()
                        }
                    },
                )
            }
            SheetValueRow(icon = R.drawable.ic_pers_wallet, caption = "Payment source") {
                BasicTextField(
                value = draft.paymentSource,
                onValueChange = { onChange(draft.copy(paymentSource = it)) },
                    textStyle = PersonalTokens.Type.Body.copy(color = PersonalTokens.Colors.TextPrimary),
                    cursorBrush = SolidColor(PersonalTokens.Colors.BrandPrimary),
                modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (draft.paymentSource.isEmpty()) Text("UPI · card · cash", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextSubtle)
                            inner()
                        }
                    },
                )
            }
        }
        SheetLinkRow(
            icon = R.drawable.ic_pers_camera,
            title = "Add memory photo",
            caption = "Optional · saved with this expense",
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
        draft.photoPreview?.let { preview ->
            Image(
                bitmap = preview,
                contentDescription = "Memory photo",
            modifier = Modifier
                .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(PersonalTokens.Radius.Button)),
                contentScale = ContentScale.Crop,
            )
        }
        if (error != null) Text(error, style = PersonalTokens.Type.Caption, color = PersonalTokens.Colors.TextSecondary)
        Box(
            Modifier
                .fillMaxWidth()
                .height(PersonalTokens.Size.ButtonHeight)
                .then(
                    if (canSave) {
                        Modifier.shadow(20.dp, RoundedCornerShape(PersonalTokens.Radius.Button), ambientColor = PersonalTokens.Colors.FillGlow, spotColor = PersonalTokens.Colors.FillGlow)
                    } else {
                        Modifier
                    },
                )
                .clip(RoundedCornerShape(PersonalTokens.Radius.Button))
                .background(if (canSave) PersonalTokens.Colors.BrandAccent else PersonalTokens.Colors.SurfaceHigh)
                .clickable(enabled = canSave, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "Saving…" else "Save expense",
                color = PersonalTokens.Colors.TextOnAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Text(
            "No Moment required. Counted once.",
            style = PersonalTokens.Type.Caption,
            color = PersonalTokens.Colors.TextSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.editingId != null) {
            Text("Record a linked refund", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.BrandPrimary, modifier = Modifier.clickable(onClick = onRefund))
            Text("Void this expense", style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextSubtle, modifier = Modifier.clickable(onClick = onVoid))
        }
    }
}

@Composable
private fun SheetValueRow(
    icon: Int,
    caption: String,
    value: String? = null,
    trailing: Int? = null,
    onClick: (() -> Unit)? = null,
    field: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PersonalTokens.Radius.Button))
            .background(PersonalTokens.Colors.SurfaceContainer)
            .border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Button))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(caption, style = PersonalTokens.Type.Caption, color = PersonalTokens.Colors.TextSubtle)
            if (field != null) field() else Text(value.orEmpty(), style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.TextPrimary)
        }
        if (trailing != null) {
            Image(painterResource(trailing), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary))
        }
    }
}

@Composable
private fun SheetLinkRow(icon: Int, title: String, caption: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
        Column(Modifier.weight(1f)) {
            Text(title, style = PersonalTokens.Type.Body, color = PersonalTokens.Colors.BrandPrimary)
            Text(caption, style = PersonalTokens.Type.Caption, color = PersonalTokens.Colors.TextSubtle)
        }
        Image(painterResource(R.drawable.ic_pers_chevron_right), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary))
    }
}

@Composable
private fun AreaPicker(draft: ExpenseDraft, onBack: () -> Unit, onClose: () -> Unit, onSelect: (PersonalLifeArea) -> Unit) {
    var selected by remember(draft.areaCode) { mutableStateOf(draft.areaCode) }
    val chosen = selected?.let { PersonalLifeTaxonomy.area(it) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Lg),
    ) {
        PickerChrome("1 · Life Area", "2 · Subcategory", step = 1, amount = draft.amount, onBack = onBack, onClose = onClose)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What part of life is this for?", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero)
            Text("Choose one area. Then pick one of its six subcategories.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
        }
        AreaLayer("NOW · Everyday living", PersonalLifeTaxonomy.areas.filter { it.layer == PersonalLifeLayer.NOW }, selected) { selected = it.code }
        AreaLayer("BUILD · Longer horizons", PersonalLifeTaxonomy.areas.filter { it.layer == PersonalLifeLayer.BUILD }, selected) { selected = it.code }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(PersonalTokens.Size.ButtonVisual).clip(RoundedCornerShape(PersonalTokens.Radius.Button)).background(if (chosen == null) PersonalTokens.Colors.SurfaceHigh else PersonalTokens.Colors.BrandAccent).clickable(enabled = chosen != null) { chosen?.let(onSelect) },
                contentAlignment = Alignment.Center,
            ) {
                Text("Next: choose a subcategory", color = PersonalTokens.Colors.TextOnAccent, style = PersonalTokens.Type.Button)
            }
            if (chosen != null) {
                Text("${chosen.label} selected. ${formatSheetAmount(draft.amount)} draft kept.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            }
            Text("NOW / BUILD is automatic, not another decision.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        }
    }
}

private sealed class AreaSlot {
    data class Pair(val left: PersonalLifeArea, val right: PersonalLifeArea) : AreaSlot()
    data class Wide(val area: PersonalLifeArea, val selected: Boolean) : AreaSlot()
}

private fun areaSlots(areas: List<PersonalLifeArea>, selected: String?): List<AreaSlot> {
    val slots = mutableListOf<AreaSlot>()
    var index = 0
    while (index < areas.size) {
        val area = areas[index]
        val next = areas.getOrNull(index + 1)
        if (area.code == selected) {
            slots += AreaSlot.Wide(area, selected = true)
            index += 1
        } else if (next != null && next.code != selected) {
            slots += AreaSlot.Pair(area, next)
            index += 2
        } else {
            slots += AreaSlot.Wide(area, selected = false)
            index += 1
        }
    }
    return slots
}

@Composable
private fun AreaLayer(label: String, areas: List<PersonalLifeArea>, selected: String?, onSelect: (PersonalLifeArea) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
        areaSlots(areas, selected).forEach { slot ->
            when (slot) {
                is AreaSlot.Pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AreaTile(slot.left, wide = false, selected = false, Modifier.weight(1f)) { onSelect(slot.left) }
                    AreaTile(slot.right, wide = false, selected = false, Modifier.weight(1f)) { onSelect(slot.right) }
                }
                is AreaSlot.Wide -> AreaTile(slot.area, wide = true, selected = slot.selected, Modifier.fillMaxWidth()) { onSelect(slot.area) }
            }
        }
    }
}

@Composable
private fun AreaMark(area: PersonalLifeArea) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(PersonalTokens.Colors.FillIconWellQuiet), contentAlignment = Alignment.Center) {
        Image(painterResource(area.iconRes), contentDescription = null, modifier = Modifier.size(22.dp), colorFilter = ColorFilter.tint(area.tint))
    }
}

@Composable
private fun AreaCopy(area: PersonalLifeArea, selected: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(area.label, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
        Text(
            if (selected) "Selected · 6 subcategories" else "6 subcategories",
            color = if (selected) PersonalTokens.Colors.BrandPrimary else PersonalTokens.Colors.TextSubtle,
            style = PersonalTokens.Type.Caption,
        )
    }
}

@Composable
private fun AreaTile(area: PersonalLifeArea, wide: Boolean, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PersonalTokens.Radius.TileSelected)
    if (wide) {
        Row(
            modifier
                .heightIn(min = 108.dp)
                .clip(shape)
            .background(if (selected) PersonalTokens.Colors.SurfaceHighlight else PersonalTokens.Colors.SurfaceContainer)
                .border(if (selected) 1.5.dp else 1.dp, if (selected) PersonalTokens.Colors.BorderSelected else PersonalTokens.Colors.BorderHairline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AreaMark(area)
            Column(Modifier.weight(1f)) { AreaCopy(area, selected) }
            Image(
                painterResource(if (selected) R.drawable.ic_pers_circle_check else R.drawable.ic_pers_arrow_up_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(if (selected) PersonalTokens.Colors.BrandPrimary else PersonalTokens.Colors.TextSubtle),
            )
        }
    } else {
        Column(
            modifier
                .heightIn(min = 126.dp)
                .clip(shape)
                .background(PersonalTokens.Colors.SurfaceContainer)
                .border(1.dp, PersonalTokens.Colors.BorderHairline, shape)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AreaMark(area)
            AreaCopy(area, selected = false)
        }
    }
}

@Composable
private fun SubcategoryPicker(
    draft: ExpenseDraft,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onChangeArea: () -> Unit,
    onSelect: (com.example.momentra.ui.theme.generated.PersonalLifeSubcategory) -> Unit,
) {
    val area = draft.areaCode?.let { PersonalLifeTaxonomy.area(it) } ?: PersonalLifeTaxonomy.areas.first()
    var picked by remember(draft.areaCode, draft.subcategoryCode) { mutableStateOf(draft.subcategoryCode) }
    val chosen = area.subcategories.firstOrNull { it.code == picked }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Lg),
    ) {
        PickerChrome("✓ Life Area", "2 · Subcategory", step = 2, amount = draft.amount, onBack = onBack, onClose = onClose)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(PersonalTokens.Colors.FillIconWellQuiet), contentAlignment = Alignment.Center) {
                    Image(painterResource(area.iconRes), contentDescription = null, modifier = Modifier.size(30.dp), colorFilter = ColorFilter.tint(area.tint))
                }
            Spacer(Modifier.weight(1f))
                Text(
                    "${area.layer.code} · automatic",
                    color = PersonalTokens.Colors.TextSecondary,
                    style = PersonalTokens.Type.Metadata,
                    modifier = Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).background(PersonalTokens.Colors.SurfaceContainer).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Pill)).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Text(area.label, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero)
            Text(area.tagline, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
            Row(Modifier.clickable(onClick = onChangeArea), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Change Life Area", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
                Image(painterResource(R.drawable.ic_pers_arrow_up_right), contentDescription = null, modifier = Modifier.size(14.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose one subcategory", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
        area.subcategories.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { sub ->
                    val selected = picked == sub.code
                        val shape = RoundedCornerShape(PersonalTokens.Radius.TileSelected)
                    Column(
                            Modifier.weight(1f).heightIn(min = 142.dp).clip(shape).background(if (selected) PersonalTokens.Colors.SurfaceHighlight else PersonalTokens.Colors.SurfaceContainer).border(if (selected) 1.5.dp else 1.dp, if (selected) PersonalTokens.Colors.BorderSelected else PersonalTokens.Colors.BorderHairline, shape).clickable { picked = sub.code }.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(sub.label, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
                            Text(if (selected) "Selected" else sub.detail, color = if (selected) PersonalTokens.Colors.BrandPrimary else PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(PersonalTokens.Size.ButtonVisual).clip(RoundedCornerShape(PersonalTokens.Radius.Button)).background(if (chosen == null) PersonalTokens.Colors.SurfaceHigh else PersonalTokens.Colors.BrandAccent).clickable(enabled = chosen != null) { chosen?.let(onSelect) },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (chosen == null) "Choose a subcategory" else "Use ${chosen.label}", color = PersonalTokens.Colors.TextOnAccent, style = PersonalTokens.Type.Button)
            }
            Text("Returns to your expense form. Nothing is saved yet.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            Text("${area.layer.code} is derived from this Life Area. One area, one valid subcategory.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        }
    }
}

@Composable
private fun PickerChrome(left: String, right: String, step: Int, amount: String, onBack: () -> Unit, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).background(PersonalTokens.Colors.SurfaceContainer).clickable(onClick = onBack).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(painterResource(R.drawable.ic_pers_arrow_left), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextPrimary))
                Text("Back", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Metadata)
            }
            Text("Close ×", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Body, modifier = Modifier.clickable(onClick = onClose))
        }
        Text("EXPENSE CONTEXT · STEP $step OF 2", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PickerStepChip(left, step == 1)
            Box(Modifier.width(20.dp).height(1.dp).background(PersonalTokens.Colors.BrandPrimary))
            PickerStepChip(right, step == 2)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PersonalTokens.Colors.FillIconWellQuiet).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("EXPENSE DRAFT · NOT SAVED", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Metadata)
            Text(formatSheetAmount(amount), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
        }
    }
}

@Composable
private fun PickerStepChip(label: String, selected: Boolean) {
    Text(
        label,
        color = if (selected) PersonalTokens.Colors.TextOnAccent else PersonalTokens.Colors.TextSecondary,
        style = PersonalTokens.Type.Metadata,
        modifier = Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).background(if (selected) PersonalTokens.Colors.BrandAccent else PersonalTokens.Colors.SurfaceContainer).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Pill)).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SavedScreen(
    confirmation: PersonalExpenseConfirmationDto,
    busy: Boolean,
    error: String?,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onPhoto: (ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val picked = decodePhoto(context, uri) ?: return@rememberLauncherForActivityResult
        onPhoto(picked.bytes)
    }
    val month = monthUpdatedLabel(confirmation.effectiveAt)
    val coverage = confirmation.moodCoverage?.let { String.format(Locale.US, "%.1f%%", it * 100) } ?: "—"
    val icon = PersonalLifeTaxonomy.subcategory(confirmation.lifeSubcategoryCode)?.iconRes ?: R.drawable.ic_pers_receipt
    val title = confirmation.merchantName?.takeIf { it.isNotBlank() } ?: confirmation.lifeSubcategoryLabel
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Lg),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(112.dp).clip(RoundedCornerShape(56.dp)).background(PersonalTokens.Colors.FillGlow))
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(PersonalTokens.Colors.SurfaceHighlight).border(1.dp, PersonalTokens.Colors.BrandPrimary, RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_pers_check), contentDescription = null, modifier = Modifier.size(28.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
                }
            }
            Text("A little more life, recorded.", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero, textAlign = TextAlign.Center)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PersonalTokens.Colors.SurfaceContainer).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(PersonalTokens.Colors.FillIconWellQuiet), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_pers_check), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.SignalPositive))
                }
                Spacer(Modifier.width(12.dp))
                Text("Expense saved · counted once", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body, modifier = Modifier.weight(1f))
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceHighlight).border(1.dp, PersonalTokens.Colors.BorderDivider, RoundedCornerShape(PersonalTokens.Radius.Card)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(inr(confirmation.amount, confirmation.currencyCode), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.AmountCard, modifier = Modifier.weight(1f))
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(PersonalLifeTaxonomy.subcategory(confirmation.lifeSubcategoryCode)?.tint ?: PersonalTokens.Colors.BrandPrimary))
            }
            Text(title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
            Text(formatSheetWhen(confirmation.effectiveAt), color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            Box(Modifier.fillMaxWidth().height(1.dp).background(PersonalTokens.Colors.BorderDivider))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onEdit)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${confirmation.lifeAreaLabel} › ${confirmation.lifeSubcategoryLabel}", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
                    Text("Edit Life context", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Caption)
                }
                Image(painterResource(R.drawable.ic_pers_pencil), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(R.drawable.ic_pers_smile), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.SignalPositive))
        Text(
                    confirmation.moodLabel?.let { "Mood: $it · chosen by you" } ?: "No mood recorded",
                    color = PersonalTokens.Colors.TextPrimary,
                    style = PersonalTokens.Type.Metadata,
                )
            }
        }
        val hasPhoto = !confirmation.photoUploadId.isNullOrBlank()
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.TileSelected)).dashedBorder(PersonalTokens.Colors.BrandPrimary, PersonalTokens.Radius.TileSelected).clickable(enabled = !hasPhoto && !busy) {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(width = 48.dp, height = 60.dp).clip(RoundedCornerShape(12.dp)).background(PersonalTokens.Colors.FillIconWellQuiet), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_pers_camera), contentDescription = null, modifier = Modifier.size(26.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (hasPhoto) "Photo added · separate from receipt" else "Add memory photo", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Body)
                if (!hasPhoto) {
                    Text("Optional · Upload a photo to remember this", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                    Text("No photo added · separate from receipt", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                }
            }
            if (!hasPhoto) {
                Image(painterResource(R.drawable.ic_pers_plus), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
            }
        }
        if (!error.isNullOrBlank()) Text(error, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
            LifeHeading("$month, updated", null)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceContainer).border(1.dp, PersonalTokens.Colors.BorderDivider, RoundedCornerShape(PersonalTokens.Radius.Card)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$month total", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                        Text(inr(confirmation.monthTotal, confirmation.currencyCode), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
                        Text(expenseCountLabel(confirmation.monthExpenseCount), color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(confirmation.lifeAreaLabel, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                        Text(inr(confirmation.areaMonthTotal, confirmation.currencyCode), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
                        Text(expenseCountLabel(confirmation.areaMonthCount), color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(PersonalTokens.Colors.BorderDivider))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mood · ${confirmation.moodResponses} / ${confirmation.monthExpenseCount} responses", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body, modifier = Modifier.weight(1f))
                    Text(coverage, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.fillMaxWidth().height(PersonalTokens.Size.ButtonVisual).clip(RoundedCornerShape(PersonalTokens.Radius.Button)).background(PersonalTokens.Colors.BrandAccent).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("Back to Pulse", color = PersonalTokens.Colors.TextOnAccent, style = PersonalTokens.Type.Button)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable(onClick = onView)) {
                Text("View expense", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Body)
                Image(painterResource(R.drawable.ic_pers_arrow_up_right), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
            }
            Text("One expense, counted once. No separate Moment payment.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption, textAlign = TextAlign.Center)
        }
    }
}

private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)),
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
    )
}

@Composable
private fun PulseBody(
    data: PersonalExperienceDto?,
    scroll: ScrollState,
    onExpense: () -> Unit,
    onEdit: (PersonalActivityRowDto) -> Unit,
    onOpenLife: () -> Unit,
    scrollToMood: Boolean,
    onMoodScrolled: () -> Unit,
) {
    val pulse = data?.pulse
    val cold = data?.cold != false
    val intro = pulse?.intro
    val scope = rememberCoroutineScope()
    var moodY by remember { mutableIntStateOf(0) }
    LaunchedEffect(scrollToMood, moodY, cold) {
        if (!scrollToMood) return@LaunchedEffect
        if (cold) {
            onMoodScrolled()
        } else if (moodY > 0) {
            scroll.animateScrollTo(moodY)
            onMoodScrolled()
        }
    }
    ScreenIntro(
        intro?.kicker?.ifBlank { null } ?: if (cold) "A FRESH START" else "YOUR PULSE",
        intro?.title?.ifBlank { null } ?: if (cold) "Your everyday starts here." else "Your Pulse",
        intro?.subtitle?.ifBlank { null } ?: if (cold) "Start with one expense. Add a little life context." else "A little context. A clearer everyday.",
        if (cold) null else onExpense,
    )
    if (cold) ColdPulse(data, onExpense) else PopulatedPulse(
        data,
        onEdit = onEdit,
        onOpenLife = onOpenLife,
        onOpenMood = { scope.launch { scroll.animateScrollTo(moodY) } },
        moodModifier = Modifier.onGloballyPositioned { moodY = it.positionInParent().y.toInt() },
    )
}

@Composable
private fun ColdPulse(data: PersonalExperienceDto?, onExpense: () -> Unit) {
    val pulse = data?.pulse
    val currency = data?.currencyCode
    SectionTitle("Recent Activity")
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("0 EXPENSES, ALL POSSIBILITY", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Micro)
                Spacer(Modifier.height(4.dp))
                Text("The little things count.", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
            }
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(PersonalTokens.Colors.FillIconWell),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_pers_receipt), contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(PersonalTokens.Space.Sm))
        Text("No expenses yet. Begin with the last thing you spent on.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
        Spacer(Modifier.height(PersonalTokens.Space.Compact))
            Text(
            "+ Add your first expense",
            color = PersonalTokens.Colors.TextOnAccent,
            style = PersonalTokens.Type.Button,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).background(PersonalTokens.Colors.BrandAccent).clickable(onClick = onExpense).padding(vertical = 12.dp),
        )
        Spacer(Modifier.height(PersonalTokens.Space.Sm))
        Text("Amount → Life Area → subcategory. Mood is optional.", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
    }
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    SectionTitle("Needs Attention")
    QuietLine(R.drawable.ic_pers_circle_check, "0 issues. Nothing waiting for review.")
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    SectionTitle("Spending Snapshot")
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text((pulse?.snapshot?.periodLabel ?: "This month").uppercase(), color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Micro, modifier = Modifier.weight(1f))
            Text(
                "0 expenses",
                color = PersonalTokens.Colors.TextSecondary,
                style = PersonalTokens.Type.Chip,
                modifier = Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).border(1.dp, PersonalTokens.Colors.BorderDivider, RoundedCornerShape(PersonalTokens.Radius.Pill)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(PersonalTokens.Space.Sm))
        Text(inr("0", currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero)
        Text("No prior period to compare", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
    }
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    SectionTitle("Life Signals")
    QuietLine(R.drawable.ic_pers_sparkles, pulse?.signalsMessage ?: "No signals yet. Patterns need enough supporting activity.")
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    SectionTitle("Mood Pulse")
        Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_pers_smile), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("0 responses / 0 expenses", color = PersonalTokens.Colors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Coverage unavailable", color = PersonalTokens.Colors.TextSubtle, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(pulse?.mood?.message ?: "Only feelings you choose to record. Never inferred.", color = PersonalTokens.Colors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun PopulatedPulse(
    data: PersonalExperienceDto?,
    onEdit: (PersonalActivityRowDto) -> Unit,
    onOpenLife: () -> Unit,
    onOpenMood: () -> Unit,
    moodModifier: Modifier,
) {
    val pulse = data?.pulse
    val currency = data?.currencyCode
    var withMood by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    val recentAll = pulse?.recent.orEmpty()
    val recent = if (withMood) recentAll.filter { it.moodCode != null } else recentAll
    val visible = if (showAll) recent else recent.take(4)
    val total = data?.expenseCount ?: recentAll.size
    SectionTitle("Recent Activity")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip("All activity", !withMood) { withMood = false }
        FilterChip("With mood", withMood) { withMood = true }
        Text(
            "$total expenses",
            color = PersonalTokens.Colors.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).border(1.dp, PersonalTokens.Colors.BorderDivider, RoundedCornerShape(PersonalTokens.Radius.Pill)).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    if (recent.isEmpty()) {
        Text("No expenses with a mood in this list.", color = PersonalTokens.Colors.TextSecondary)
    } else {
        visible.forEachIndexed { index, row ->
            ActivityRow(row, emphasized = index == 0 && !withMood, onClick = { onEdit(row) })
        }
    }
    if (recent.size > 4 || total > visible.size) {
        ExploreRow(if (showAll) "Show recent expenses" else "View all $total expenses") { showAll = !showAll }
    }
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    SectionTitle("Needs Attention")
    val attention = pulse?.attention
    if (attention?.quiet != false) {
        QuietLine(R.drawable.ic_pers_circle_check, attention?.title?.ifBlank { null } ?: "All clear.", attention?.body?.ifBlank { null })
    } else {
        QuietLine(R.drawable.ic_pers_circle_check, attention.title.ifBlank { attention.message }, attention.body.ifBlank { null })
    }
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    val snap = pulse?.snapshot
    SectionTitle("Spending Snapshot", snap?.rangeLabel?.takeIf { it.isNotBlank() })
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(inr(snap?.total ?: "0", currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero, modifier = Modifier.weight(1f))
            Image(
                painterResource(R.drawable.ic_pers_arrow_up_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp).clickable(onClick = onOpenLife),
            )
        }
        Text(deltaLine(snap, currency), color = PersonalTokens.Colors.TextSecondary, fontSize = 13.sp)
        if (snap?.comparable == true) {
            Spacer(Modifier.height(12.dp))
            CompareTrack(snap.total, snap.previousTotal)
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Tile)).background(PersonalTokens.Colors.SurfaceHighlight).padding(14.dp),
            ) {
                Text(inr(snap.previousTotal, currency), color = PersonalTokens.Colors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(
                    listOfNotNull(snap.previousRangeLabel, "${snap.previousExpenseCount} expenses").joinToString(" · "),
                    color = PersonalTokens.Colors.TextSubtle,
                    fontSize = 12.sp,
                )
            }
        }
        if (!snap?.comparisonNote.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(snap?.comparisonNote ?: "", color = PersonalTokens.Colors.TextSecondary, fontSize = 13.sp)
        }
        ExploreRow("Explore spending in Life", onOpenLife)
    }
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    val signals = pulse?.signals.orEmpty()
    SectionTitle("Life Signals", if (signals.isEmpty()) null else pulse?.signalsPrompt)
    if (signals.isEmpty()) {
        QuietLine(R.drawable.ic_pers_sparkles, pulse?.signalsMessage ?: "No signals yet. Patterns need enough supporting activity.")
    } else {
        Card {
        signals.forEach { signal ->
                Text(signal.subcategoryLabel.ifBlank { signal.areaLabel }, color = PersonalTokens.Colors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(signal.areaLabel, color = PersonalTokens.Colors.TextSubtle, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                SignalBar(signal.previousShort(snap), inr(signal.previous, currency), signal.previous, signal.current, muted = true)
                SignalBar(signal.currentShort(snap), inr(signal.current, currency), signal.current, signal.previous, muted = false)
                Text(signal.sentence, color = PersonalTokens.Colors.TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
            }
            ExploreRow("View supporting expenses", onOpenLife)
        }
    }
    Spacer(Modifier.height(PersonalTokens.Space.Md))
    val mood = pulse?.mood
    SectionTitle("Mood Pulse", "The feelings you chose to record", moodModifier)
    Card {
        val coverage = mood?.coverage
        Row {
            Text("${mood?.responseCount ?: 0} / ${mood?.expenseCount ?: 0} expenses", color = PersonalTokens.Colors.TextSubtle, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                if (coverage == null) "Coverage unavailable" else "${(coverage * 100).toInt()}% coverage",
                color = PersonalTokens.Colors.BrandPrimary,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        val maxCount = (mood?.counts?.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            mood?.counts.orEmpty().forEach { count ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${count.count}", color = PersonalTokens.Colors.TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height((12 + 72 * count.count / maxCount).dp)
                            .background(PersonalTokens.Colors.BrandSecondary, RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(count.label, color = PersonalTokens.Colors.TextSubtle, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(mood?.message ?: "Only feelings you choose to record. Never inferred.", color = PersonalTokens.Colors.TextSecondary, fontSize = 13.sp)
        ExploreRow("View mood context", onOpenMood)
    }
}

@Composable
private fun ActivityRow(row: PersonalActivityRowDto, emphasized: Boolean, onClick: () -> Unit) {
    val icon = PersonalLifeTaxonomy.subcategory(row.subcategoryCode)?.iconRes ?: R.drawable.ic_pers_receipt
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(PersonalTokens.Radius.Row))
            .background(if (emphasized) PersonalTokens.Colors.SurfaceHighlight else PersonalTokens.Colors.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PersonalTokens.Colors.FillIconWell),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(row.merchantName, color = PersonalTokens.Colors.TextPrimary, modifier = Modifier.weight(1f))
            Text(inr(row.amount, row.currencyCode), color = PersonalTokens.Colors.TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        val whenLine = listOfNotNull(formatPulseWhen(row.effectiveAt), row.moodLabel?.let { "Mood: $it" }).joinToString(" · ")
        Text(whenLine, color = PersonalTokens.Colors.TextSubtle, fontSize = 12.sp)
        Text("${row.areaLabel} · ${row.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun QuietLine(icon: Int, title: String, body: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(PersonalTokens.Colors.FillIconWellQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
            if (!body.isNullOrBlank()) Text(body, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) PersonalTokens.Colors.TextOnAccent else PersonalTokens.Colors.TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
            .background(if (selected) PersonalTokens.Colors.BrandAccent else PersonalTokens.Colors.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun ExploreRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PersonalTokens.Colors.BrandPrimary, modifier = Modifier.weight(1f))
        Image(painterResource(R.drawable.ic_pers_arrow_up_right), contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CompareTrack(current: String, previous: String?) {
    val cur = current.toDoubleOrNull() ?: 0.0
    val prev = previous?.toDoubleOrNull() ?: 0.0
    val max = maxOf(cur, prev, 1.0)
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)).background(PersonalTokens.Colors.SurfaceHigh)) {
        Box(
            Modifier.fillMaxWidth((prev / max).toFloat().coerceIn(0f, 1f)).height(10.dp).background(PersonalTokens.Colors.TextSubtle.copy(alpha = 0.45f)),
        )
        Box(
            Modifier.fillMaxWidth((cur / max).toFloat().coerceIn(0f, 1f)).height(10.dp).background(PersonalTokens.Colors.BrandSecondary),
        )
    }
}

@Composable
private fun SignalBar(label: String, amount: String, value: String, other: String, muted: Boolean) {
    val mine = value.toDoubleOrNull() ?: 0.0
    val theirs = other.toDoubleOrNull() ?: 0.0
    val max = maxOf(mine, theirs, 1.0)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Text(label, color = PersonalTokens.Colors.TextSubtle, fontSize = 12.sp, modifier = Modifier.width(36.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(PersonalTokens.Colors.SurfaceHigh)) {
            Box(
                Modifier.fillMaxWidth((mine / max).toFloat().coerceIn(0.05f, 1f)).height(8.dp).background(
                    if (muted) PersonalTokens.Colors.TextSubtle else PersonalTokens.Colors.BrandSecondary,
                ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(amount, color = PersonalTokens.Colors.TextSecondary, fontSize = 12.sp)
    }
}

private fun PersonalSignalDto.previousShort(snap: PersonalSnapshotDto?): String = snap?.previousShort ?: "Prev"

private fun PersonalSignalDto.currentShort(snap: PersonalSnapshotDto?): String = snap?.currentShort ?: "Now"

private fun deltaLine(snap: PersonalSnapshotDto?, currency: String?): String {
    val count = "${snap?.expenseCount ?: 0} expenses"
    val delta = snap?.deltaAmount?.toDoubleOrNull()
    val ratio = snap?.deltaRatio
    if (snap?.comparable != true || delta == null || ratio == null) return count
    val sign = if (delta >= 0) "+" else "−"
    val pct = String.format(Locale.US, "%.1f", abs(ratio) * 100)
    return "$count · $sign${inr(abs(delta).toString(), currency)} / $pct%"
}

@Composable
private fun MomentsBody(
    data: PersonalExperienceDto?,
    scroll: ScrollState,
    onExpense: () -> Unit,
    onOpenLife: () -> Unit,
) {
    val moments = data?.moments
    val active = moments?.active.orEmpty()
    val emerging = moments?.emerging.orEmpty()
    val recent = moments?.recent.orEmpty()
    val cold = active.isEmpty() && emerging.isEmpty() && recent.isEmpty()
    val intro = moments?.intro
    val scope = rememberCoroutineScope()
    var storiesY by remember { mutableIntStateOf(0) }
    MomentsIntro(
        kicker = intro?.kicker?.ifBlank { null } ?: if (cold) "A FRESH START" else "MOMENTS",
        title = intro?.title?.ifBlank { null } ?: if (cold) "Life happens. Stories follow." else "Moments",
        subtitle = intro?.subtitle?.ifBlank { null }
            ?: if (cold) "Your meaningful expenses can connect into Moments automatically."
            else "Meaningful stories, taking shape in your expenses. Found automatically, not created.",
        onExpense = if (cold) null else onExpense,
    )
    if (cold) {
        ColdMoments(moments, onExpense)
    } else {
        PopulatedMoments(
            moments,
            active,
            emerging,
            recent,
            onOpenLife = onOpenLife,
            onCompare = { scope.launch { scroll.animateScrollTo(storiesY) } },
            storiesModifier = Modifier.onGloballyPositioned { storiesY = it.positionInParent().y.toInt() },
        )
    }
}

@Composable
private fun MomentsIntro(kicker: String, title: String, subtitle: String, onExpense: (() -> Unit)?) {
    Text(kicker, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
    Spacer(Modifier.height(PersonalTokens.Space.Sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero, modifier = Modifier.weight(1f))
        if (onExpense != null) {
            Text(
                "+ Expense",
                color = PersonalTokens.Colors.TextOnAccent,
                style = PersonalTokens.Type.Chip,
                modifier = Modifier
                    .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
                    .background(PersonalTokens.Colors.BrandAccent)
                    .clickable(onClick = onExpense)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(subtitle, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
    Spacer(Modifier.height(if (onExpense == null) PersonalTokens.Space.Lg else PersonalTokens.Space.Section))
}

@Composable
private fun ColdMoments(moments: com.example.momentra.data.api.PersonalMomentsExperienceDto?, onExpense: () -> Unit) {
    SectionTitle("Active Moments")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PersonalTokens.Radius.Card))
            .background(PersonalTokens.Colors.SurfaceHighlight)
            .padding(12.dp),
    ) {
        Image(
            painterResource(R.drawable.art_pers_open_beginning),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(PersonalTokens.Radius.Card)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(PersonalTokens.Space.Compact))
        Text("NO ACTIVE MOMENTS YET", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.StoryKicker)
        Spacer(Modifier.height(6.dp))
        Text("Room for your next chapter.", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
        Spacer(Modifier.height(6.dp))
        Text(
            "Start with an expense. Let meaningful connections take shape.",
            color = PersonalTokens.Colors.TextSecondary,
            style = PersonalTokens.Type.Caption,
        )
        Spacer(Modifier.height(6.dp))
        Text(moments?.heroMonth ?: "A fresh start", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
        Text(
            moments?.heroEvidence ?: "0 stories · no supporting activity",
            color = PersonalTokens.Colors.BrandPrimary,
            style = PersonalTokens.Type.Caption,
        )
    }
    Spacer(Modifier.height(PersonalTokens.Space.Compact))
    Text(
        "+ Add your first expense",
        color = PersonalTokens.Colors.TextOnAccent,
        style = PersonalTokens.Type.Button,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(PersonalTokens.Size.ButtonVisual)
            .clip(RoundedCornerShape(PersonalTokens.Radius.Button))
            .background(PersonalTokens.Colors.BrandAccent)
            .clickable(onClick = onExpense)
            .padding(vertical = 14.dp),
    )
    Spacer(Modifier.height(PersonalTokens.Space.Lg))
    SectionTitle("Emerging Moments")
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_pers_receipt), contentDescription = null, modifier = Modifier.size(22.dp))
            Box(Modifier.padding(horizontal = 8.dp).width(24.dp).height(1.dp).background(PersonalTokens.Colors.BrandPrimary))
            Image(painterResource(R.drawable.ic_pers_link), contentDescription = null, modifier = Modifier.size(22.dp))
            Box(Modifier.padding(horizontal = 8.dp).width(24.dp).height(1.dp).background(PersonalTokens.Colors.BrandPrimary))
            Image(painterResource(R.drawable.ic_pers_layers), contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(PersonalTokens.Space.Inner))
        Text("No connections yet.", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
        Text(
            "Related expenses need shared meaning. Repeating the same merchant alone isn’t a story.",
            color = PersonalTokens.Colors.TextSecondary,
            style = PersonalTokens.Type.Body,
        )
    }
    Spacer(Modifier.height(PersonalTokens.Space.Lg))
    SectionTitle("Moment Progress")
    QuietLine(R.drawable.ic_pers_route, "No story activity yet. Progress appears with supporting expenses—not a completion score.")
    Spacer(Modifier.height(PersonalTokens.Space.Lg))
    SectionTitle("Recent Moments")
    QuietLine(R.drawable.ic_pers_book_open, "0 completed stories. Confirmed chapters will be here to revisit.")
    Spacer(Modifier.height(PersonalTokens.Space.Lg))
    SectionTitle("Moment Insights")
    QuietLine(R.drawable.ic_pers_lightbulb, "No insights yet. Observations need meaningful stories first.")
}

@Composable
private fun PopulatedMoments(
    moments: com.example.momentra.data.api.PersonalMomentsExperienceDto?,
    active: List<PersonalMomentCardDto>,
    emerging: List<PersonalMomentCardDto>,
    recent: List<PersonalMomentCardDto>,
    onOpenLife: () -> Unit,
    onCompare: () -> Unit,
    storiesModifier: Modifier,
) {
    SectionTitle("Active Moments", moments?.activeSubtitle, storiesModifier)
    active.forEachIndexed { index, card ->
        if (index == 0) FeaturedMoment(card, onOpenLife) else StoryCard(card, onOpenLife, showArtwork = false)
        if (index < active.lastIndex) Spacer(Modifier.height(PersonalTokens.Space.Inner))
    }
    Spacer(Modifier.height(PersonalTokens.Space.Section))
    SectionTitle("Emerging Moments", moments?.emergingSubtitle)
    if (emerging.isEmpty()) {
        QuietLine(R.drawable.ic_pers_link, "No connections yet.")
    } else emerging.forEach { card ->
        StoryCard(card, onOpenLife, showArtwork = true)
        Spacer(Modifier.height(PersonalTokens.Space.Inner))
    }
    Spacer(Modifier.height(PersonalTokens.Space.Section))
    val progress = moments?.progress
    SectionTitle("Moment Progress", moments?.progressSubtitle)
    if (progress == null) {
        QuietLine(R.drawable.ic_pers_route, "No story activity yet. Progress appears with supporting expenses—not a completion score.")
    } else {
    Card {
            Text(progress.title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
            if (progress.areaLabel.isNotBlank()) {
                Text("${progress.areaLabel} › ${progress.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            }
            Spacer(Modifier.height(PersonalTokens.Space.Inner))
            ProgressBars(progress.bars.orEmpty())
            if (progress.cumulativeLabel.isNotBlank()) {
                Spacer(Modifier.height(PersonalTokens.Space.Compact))
                Text(progress.cumulativeLabel, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
            }
            val note = progress.footnote.ifBlank { progress.message }
            if (note.isNotBlank()) Text(note, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        }
    }
    Spacer(Modifier.height(PersonalTokens.Space.Section))
    SectionTitle("Recent Moments", moments?.recentSubtitle)
    if (recent.isEmpty()) {
        QuietLine(R.drawable.ic_pers_book_open, "0 completed stories. Confirmed chapters will be here to revisit.")
    } else recent.forEach { StoryCard(it, onOpenLife, showArtwork = false) }
    Spacer(Modifier.height(PersonalTokens.Space.Section))
    SectionTitle("Moment Insights")
    val insight = moments?.insight
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PersonalTokens.Radius.Row))
            .background(PersonalTokens.Colors.SurfaceContainer)
            .padding(18.dp),
    ) {
        if (!insight?.title.isNullOrBlank()) {
            Text(insight.title, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Section)
            Spacer(Modifier.height(PersonalTokens.Space.Sm))
        }
        Text(
            insight?.text?.ifBlank { null } ?: "Insights appear when more than one story has enough evidence.",
            color = PersonalTokens.Colors.TextSecondary,
            style = PersonalTokens.Type.Body,
        )
    }
    if (!insight?.actionLabel.isNullOrBlank()) ExploreRow(insight.actionLabel, onCompare)
    if (!insight?.footnote.isNullOrBlank()) {
        Spacer(Modifier.height(PersonalTokens.Space.Sm))
        Text(insight.footnote, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
    }
}

@Composable
private fun FeaturedMoment(card: PersonalMomentCardDto, onOpenLife: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PersonalTokens.Radius.Card))
            .background(PersonalTokens.Colors.SurfaceHighlight),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            val art = artwork(card.artworkKey) ?: R.drawable.art_pers_open_beginning
            Image(
                painterResource(art),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(PersonalTokens.Radius.Card)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(PersonalTokens.Space.Compact))
            StoryCopy(card)
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text("${card.areaLabel} › ${card.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            ExploreRow("Explore ${card.activityCount} supporting expenses", onOpenLife)
        }
    }
}

@Composable
private fun StoryCard(card: PersonalMomentCardDto, onOpenLife: () -> Unit, showArtwork: Boolean) {
    Card {
        if (showArtwork) {
            artwork(card.artworkKey)?.let { res ->
                Image(
                    painterResource(res),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(PersonalTokens.Radius.Row)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(PersonalTokens.Space.Inner))
            }
        }
        StoryCopy(card)
        Text("${card.areaLabel} › ${card.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        ExploreRow("Explore supporting expenses", onOpenLife)
    }
}

@Composable
private fun StoryCopy(card: PersonalMomentCardDto) {
    if (card.statusLabel.isNotBlank()) {
        Text(card.statusLabel, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
        Spacer(Modifier.height(6.dp))
    }
    Text(card.title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
    if (card.summary.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(card.summary, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
    }
    Spacer(Modifier.height(6.dp))
    Text("${card.activityCount} expenses · ${inr(card.spend, "INR")}", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
    if (card.rangeLabel.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(card.rangeLabel, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
    }
    Spacer(Modifier.height(PersonalTokens.Space.Sm))
}

@Composable
private fun ProgressBars(bars: List<com.example.momentra.data.api.PersonalProgressBarDto>) {
    val max = bars.maxOfOrNull { it.amount.toDoubleOrNull() ?: 0.0 }?.coerceAtLeast(1.0) ?: 1.0
    Row(
        Modifier.fillMaxWidth().height(134.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val ratio = ((bar.amount.toDoubleOrNull() ?: 0.0) / max).toFloat().coerceIn(0.08f, 1f)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(inr(bar.amount, "INR"), color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .width(42.dp)
                        .height(72.dp * ratio)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PersonalTokens.Colors.BrandPrimary),
                )
                Spacer(Modifier.height(8.dp))
                Text(bar.label.ifBlank { formatPulseWhen(bar.effectiveAt) }, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            }
        }
    }
}

@Composable
private fun MomentCardView(card: PersonalMomentCardDto) {
    Card(Modifier.padding(bottom = 8.dp)) {
        artwork(card.artworkKey)?.let { res ->
            Image(painterResource(res), card.title, modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(8.dp))
        }
        Text(card.title, color = PersonalTokens.Colors.TextPrimary, fontSize = 18.sp)
        Text("${card.areaLabel} · ${card.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary)
        Text("${card.activityCount} expenses · ${card.spend}", color = PersonalTokens.Colors.TextSubtle, fontSize = 12.sp)
    }
}

@Composable
private fun LifeBody(
    data: PersonalExperienceDto?,
    onExpense: () -> Unit,
    onArea: (String) -> Unit,
    onSeeAreas: () -> Unit,
    onCompare: () -> Unit,
    onExploreMoods: () -> Unit,
    areasModifier: Modifier = Modifier,
) {
    val life = data?.life
    val cold = data?.cold != false
    val currency = data?.currencyCode
    val gap = if (cold) PersonalTokens.Space.Lg else PersonalTokens.Space.Section
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        LifeIntro(life, cold, onExpense)
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact), modifier = areasModifier) {
            LifeHeading("Your Life", if (cold) null else life?.rangeLabel)
            Text(inr(if (cold) "0" else life?.total, currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.AmountL)
            Text(
                if (cold) "0 expenses · ${life?.monthLabel?.ifBlank { null } ?: "this month"}" else expenseCountLabel(life?.expenseCount ?: 0),
                color = PersonalTokens.Colors.TextSecondary,
                style = PersonalTokens.Type.Body,
            )
            if (!cold) {
                Text(life?.coverageLabel?.ifBlank { null } ?: "Fully classified", color = PersonalTokens.Colors.SignalPositive, style = PersonalTokens.Type.Caption)
            }
            LifeLayerGrid("NOW · Everyday living", PersonalLifeLayer.NOW, life, cold, currency, onArea)
            LifeLayerGrid("BUILD · Longer horizons", PersonalLifeLayer.BUILD, life, cold, currency, onArea)
            if (!cold) {
                Text(
                    "Tap an area to explore activity and subcategories. Bars show its share of ${inr(life?.total, currency)}.",
                    color = PersonalTokens.Colors.TextSubtle,
                    style = PersonalTokens.Type.Caption,
                )
            }
        }
        if (cold) {
            Box(
                Modifier.fillMaxWidth().height(PersonalTokens.Size.ButtonVisual).clip(RoundedCornerShape(PersonalTokens.Radius.Button)).background(PersonalTokens.Colors.BrandAccent).clickable(onClick = onExpense),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Add your first expense", color = PersonalTokens.Colors.TextOnAccent, style = PersonalTokens.Type.Button)
            }
        }
        LifeBalanceCard(life, cold, currency)
        LifeMoneySection(life, cold, currency, onSeeAreas)
        LifeTrendsSection(life, cold, currency, onCompare)
        LifeMoodSection(life, cold, onExploreMoods)
    }
}

@Composable
private fun LifeIntro(
    life: com.example.momentra.data.api.PersonalLifeExperienceDto?,
    cold: Boolean,
    onExpense: () -> Unit,
) {
    val intro = life?.intro
    Text(
        intro?.kicker?.ifBlank { null } ?: if (cold) "A FRESH START" else "YOUR LIFE",
        color = PersonalTokens.Colors.BrandPrimary,
        style = PersonalTokens.Type.Metadata,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            intro?.title?.ifBlank { null } ?: if (cold) "All the parts of you." else "Your money, in life",
            color = PersonalTokens.Colors.TextPrimary,
            style = PersonalTokens.Type.Hero,
            modifier = Modifier.weight(1f),
        )
        if (!cold) {
            Text(
                "+ Expense",
                color = PersonalTokens.Colors.TextOnAccent,
                style = PersonalTokens.Type.Chip,
                modifier = Modifier
                    .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
                    .background(PersonalTokens.Colors.BrandAccent)
                    .clickable(onClick = onExpense)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
    Text(
        intro?.subtitle?.ifBlank { null } ?: if (cold) "Eight areas of life. Explore any one, even before your first expense." else "Eight areas. One connected picture.",
        color = PersonalTokens.Colors.TextSecondary,
        style = PersonalTokens.Type.Body,
    )
}

@Composable
private fun LifeHeading(title: String, subtitle: String?) {
    Text(title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
    if (!subtitle.isNullOrBlank()) Text(subtitle, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
}

@Composable
private fun LifeLayerGrid(
    label: String,
    layer: PersonalLifeLayer,
    life: com.example.momentra.data.api.PersonalLifeExperienceDto?,
    cold: Boolean,
    currency: String?,
    onArea: (String) -> Unit,
) {
    Text(label, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
    val areas = PersonalLifeTaxonomy.areas.filter { it.layer == layer }
    Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Sm)) {
        areas.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Sm)) {
                row.forEach { area ->
                    val spend = life?.areas.orEmpty().find { it.areaCode == area.code }
                    LifeAreaTile(
                        area = area,
                        spend = if (cold) "0" else spend?.spend ?: "0",
                        count = if (cold) 0 else spend?.activityCount ?: 0,
                        share = if (cold) 0.0 else spend?.share ?: 0.0,
                        highlighted = !cold && life?.highlightedAreaCode == area.code,
                        cold = cold,
                        currency = currency,
                        modifier = Modifier.weight(1f),
                        onClick = { onArea(area.code) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LifeAreaTile(
    area: PersonalLifeArea,
    spend: String,
    count: Int,
    share: Double,
    highlighted: Boolean,
    cold: Boolean,
    currency: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(PersonalTokens.Radius.TileSelected)
    Column(
        modifier
            .heightIn(min = if (cold) 134.dp else 156.dp)
            .clip(shape)
            .background(if (highlighted) PersonalTokens.Colors.SurfaceHighlight else PersonalTokens.Colors.SurfaceContainer)
            .border(1.dp, if (highlighted) PersonalTokens.Colors.BorderSelected else PersonalTokens.Colors.BorderHairline, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(area.iconRes), contentDescription = null, modifier = Modifier.size(18.dp), colorFilter = ColorFilter.tint(area.tint))
            Spacer(Modifier.weight(1f))
            Image(painterResource(R.drawable.ic_pers_arrow_up_right), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSubtle))
        }
        Column {
            Text(area.label, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
            Spacer(Modifier.height(4.dp))
            if (cold) {
                Text("₹0 · 0 expenses", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(inr(spend, currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Metadata, modifier = Modifier.weight(1f))
                    Text(expenseCountLabel(count), color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(PersonalTokens.Colors.SurfaceHigh)) {
                    Box(Modifier.fillMaxWidth(share.toFloat().coerceIn(0f, 1f)).height(4.dp).background(PersonalTokens.Colors.BrandPrimary))
                }
            }
        }
    }
}

@Composable
private fun LifeBalanceCard(
    life: com.example.momentra.data.api.PersonalLifeExperienceDto?,
    cold: Boolean,
    currency: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Sm)) {
        LifeHeading("NOW ↔ BUILD", null)
    Card {
            if (cold) {
                Row {
                    LifeBalanceColumn("NOW", "Everyday living", "0", currency, Modifier.weight(1f))
                    LifeBalanceColumn("BUILD", "Longer horizons", "0", currency, Modifier.weight(1f))
                }
                Spacer(Modifier.height(PersonalTokens.Space.Sm))
                Text("Shares unavailable at ₹0. Derived from Life Areas, never a separate choice.", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
            } else {
                val nowShare = life?.nowBuild?.nowShare ?: 0.0
                val buildShare = life?.nowBuild?.buildShare ?: 0.0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LifeShareRing(nowShare, buildShare)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LifeShareLine("NOW", nowShare, life?.nowBuild?.now, currency, PersonalTokens.Colors.BrandPrimary)
                        LifeShareLine("BUILD", buildShare, life?.nowBuild?.build, currency, PersonalTokens.Colors.BrandTertiary)
                    }
                }
                Spacer(Modifier.height(PersonalTokens.Space.Sm))
                Text("Derived from Life Areas. Allocation is context, not a score. Percentages rounded.", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
            }
        }
    }
}

@Composable
private fun LifeBalanceColumn(kicker: String, title: String, amount: String, currency: String?, modifier: Modifier) {
    Column(modifier) {
        Text(kicker, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
        Text(title, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
        Text(inr(amount, currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
    }
}

@Composable
private fun LifeShareRing(nowShare: Double, buildShare: Double) {
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 6.dp.toPx()
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Butt)
            val topLeft = Offset(inset, inset)
            val oval = Size(size.width - inset * 2, size.height - inset * 2)
            val nowSweep = (nowShare * 360.0).toFloat().coerceIn(0f, 360f)
            val buildSweep = (buildShare * 360.0).toFloat().coerceIn(0f, 360f - nowSweep)
            if (nowSweep > 0f) {
                drawArc(PersonalTokens.Colors.BrandPrimary, -90f, nowSweep, false, topLeft, oval, style = stroke)
            }
            if (buildSweep > 0f) {
                drawArc(PersonalTokens.Colors.BrandTertiary, -90f + nowSweep, buildSweep, false, topLeft, oval, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("100%", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
            Text("of spend", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
        }
    }
}

@Composable
private fun LifeShareLine(label: String, share: Double, amount: String?, currency: String?, color: Color) {
    Column {
        Text("$label · ${sharePercent(share)}", color = color, style = PersonalTokens.Type.Metadata)
        Text(inr(amount, currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
    }
}

@Composable
private fun LifeMoneySection(
    life: com.example.momentra.data.api.PersonalLifeExperienceDto?,
    cold: Boolean,
    currency: String?,
    onSeeAreas: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Sm)) {
        LifeHeading("Where Your Money Goes", null)
        if (cold) {
            QuietLine(R.drawable.ic_pers_wallet, "No classified spend yet.", "Subcategories appear with your first expense.")
        } else {
            Card {
                life?.moneyGoes.orEmpty().forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(slice.label, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
                            if (slice.areaLabel.isNotBlank()) Text(slice.areaLabel, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(inr(slice.spend, currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Metadata)
                            Text(sharePercent(slice.share), color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(PersonalTokens.Colors.SurfaceHigh)) {
                        Box(Modifier.fillMaxWidth(slice.share.toFloat().coerceIn(0f, 1f)).height(6.dp).background(PersonalTokens.Colors.BrandAccent))
                    }
                    Spacer(Modifier.height(PersonalTokens.Space.Compact))
                }
                if ((life?.otherSpend?.toDoubleOrNull() ?: 0.0) > 0.0) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Tile)).background(PersonalTokens.Colors.FillIconWellQuiet).padding(12.dp),
                    ) {
                        Text("${life?.otherCount ?: 0} other subcategories", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
                        Text("${inr(life?.otherSpend, currency)} · ${sharePercent(life?.otherShare ?: 0.0)}", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
                    }
                    Spacer(Modifier.height(PersonalTokens.Space.Sm))
                }
                Text(
                    "Top five · ${inr(life?.topFiveSpend, currency)} · ${sharePercent(life?.topFiveShare ?: 0.0)} of this period.",
                    color = PersonalTokens.Colors.TextSubtle,
                    style = PersonalTokens.Type.Caption,
                )
                Text(
                    "See all subcategories",
                    color = PersonalTokens.Colors.BrandPrimary,
                    style = PersonalTokens.Type.Body,
                    modifier = Modifier.padding(top = 8.dp).clickable(onClick = onSeeAreas),
                )
            }
        }
    }
}

@Composable
private fun LifeTrendsSection(
    life: com.example.momentra.data.api.PersonalLifeExperienceDto?,
    cold: Boolean,
    currency: String?,
    onCompare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Sm)) {
        LifeHeading("Life Trends", null)
    val trends = life?.trends.orEmpty()
        if (cold || trends.isEmpty()) {
            QuietLine(
                R.drawable.ic_pers_calendar_days,
                life?.trendsMessage?.ifBlank { null } ?: "No comparable periods yet. There’s no trend to show.",
            )
        } else {
            Card {
                trends.take(2).forEach { trend ->
                    Text(trend.label, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
                    if (trend.areaLabel.isNotBlank()) Text(trend.areaLabel, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
                    Spacer(Modifier.height(8.dp))
                    LifeTrendBar(trend.previousShort, trend.previous, trend.current, currency, PersonalTokens.Colors.SurfaceHigh)
                    Spacer(Modifier.height(6.dp))
                    LifeTrendBar(trend.currentShort, trend.current, trend.previous, currency, PersonalTokens.Colors.BrandAccent)
                    Spacer(Modifier.height(6.dp))
        Text(
                        "${expenseCountLabel(trend.previousCount)} · ${expenseCountLabel(trend.currentCount)}",
            color = PersonalTokens.Colors.TextSecondary,
                        style = PersonalTokens.Type.Caption,
                    )
                    Spacer(Modifier.height(PersonalTokens.Space.Compact))
                }
                life?.trendsFootnote?.let { Text(it, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption) }
                Text(
                    "Compare period activity",
                    color = PersonalTokens.Colors.BrandPrimary,
                    style = PersonalTokens.Type.Body,
                    modifier = Modifier.padding(top = 8.dp).clickable(onClick = onCompare),
                )
            }
        }
    }
}

@Composable
private fun LifeTrendBar(month: String, amount: String, other: String, currency: String?, color: Color) {
    val value = amount.toDoubleOrNull() ?: 0.0
    val max = maxOf(value, other.toDoubleOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(month.ifBlank { "—" }, color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption, modifier = Modifier.width(36.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(PersonalTokens.Colors.FillIconWell)) {
            Box(Modifier.fillMaxWidth((value / max).toFloat().coerceIn(0f, 1f)).height(8.dp).background(color))
        }
        Spacer(Modifier.width(8.dp))
        Text(inr(amount, currency), color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifeMoodSection(
    life: com.example.momentra.data.api.PersonalLifeExperienceDto?,
    cold: Boolean,
    onExploreMoods: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Sm)) {
        LifeHeading("Life & Mood", null)
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    life?.moodSummary?.ifBlank { null } ?: if (cold) "0 responses / 0 expenses" else "",
                    color = PersonalTokens.Colors.TextPrimary,
                    style = PersonalTokens.Type.Body,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    life?.moodCoverageLabel?.ifBlank { null } ?: "Coverage unavailable",
                    color = PersonalTokens.Colors.BrandPrimary,
                    style = PersonalTokens.Type.Caption,
                )
            }
            Spacer(Modifier.height(PersonalTokens.Space.Sm))
            if (!cold) {
                life?.moodByArea.orEmpty().forEach { area ->
                    Text(area.areaLabel, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Metadata)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        area.responses.orEmpty().forEach { mood ->
                            val recorded = mood != "No mood"
                            Text(
                                mood,
                                color = if (recorded) PersonalTokens.Colors.BrandPrimary else PersonalTokens.Colors.TextSecondary,
                                style = PersonalTokens.Type.Chip,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(PersonalTokens.Radius.Chip))
                                    .background(if (recorded) PersonalTokens.Colors.SurfaceHighlight else PersonalTokens.Colors.SurfaceHigh)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(PersonalTokens.Space.Sm))
                }
            }
            Text(
                life?.moodFootnote?.ifBlank { null } ?: "Optional, explicitly recorded feelings. Area-level patterns need enough responses.",
                color = PersonalTokens.Colors.TextSubtle,
                style = PersonalTokens.Type.Caption,
            )
            if (!cold) {
                Text(
                    "Explore recorded moods",
                    color = PersonalTokens.Colors.BrandPrimary,
                    style = PersonalTokens.Type.Body,
                    modifier = Modifier.padding(top = 8.dp).clickable(onClick = onExploreMoods),
                )
            }
        }
    }
}

@Composable
private fun MemoryBody(data: PersonalExperienceDto?, onExpense: () -> Unit, onOpenMoments: () -> Unit) {
    val memory = data?.memory
    val currency = data?.currencyCode
    val hasHistory = memory?.timeline.orEmpty().isNotEmpty() || memory?.completed.orEmpty().isNotEmpty() || memory?.monthly != null || memory?.lookBack != null
    var currentOnly by remember { mutableStateOf(false) }
    val gap = if (hasHistory) PersonalTokens.Space.Section else PersonalTokens.Space.Lg
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        MemoryIntro(memory, hasHistory, onExpense)
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
            LifeHeading("Life Timeline", if (hasHistory) "Meaningful events, not a transaction feed" else null)
            if (!hasHistory) {
                MemoryOpening(memory, onExpense)
                MemoryRail(
                    memory?.timelineOpenKicker?.ifBlank { null } ?: "OPEN",
                    "Your timeline begins when a meaningful story takes shape. No events yet.",
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("All chapters", !currentOnly) { currentOnly = false }
                    FilterChip(memory?.currentFilterLabel?.ifBlank { null } ?: "This month · current", currentOnly) { currentOnly = true }
                }
                val events = memory?.timeline.orEmpty().filter { !currentOnly || it.period == "current" }
                if (events.isEmpty()) {
                    Text("No events in this month yet.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
                } else {
                    events.forEach { MemoryRail(it.kicker.ifBlank { formatWhen(it.effectiveAt) }, it.detail) }
                }
                MemoryLabel("Explore the timeline", onOpenMoments)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
            LifeHeading("Completed Moments", if (hasHistory) "Finished stories with confirmed context" else null)
    val completed = memory?.completed.orEmpty()
            if (completed.isEmpty()) {
                QuietLine(R.drawable.ic_pers_book_check, "0 completed stories. Confirmed chapters will stay here.")
            } else {
                completed.forEach { MemoryCompletedCard(it, currency, onOpenMoments) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
            LifeHeading("Monthly Memories", if (hasHistory) "Closed months, ready to revisit" else null)
            val monthly = memory?.monthly
            if (monthly == null) MemoryOpenMonth(memory) else MemoryClosedMonth(monthly, memory?.openMonthNote, currency, onOpenMoments)
        }
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
            LifeHeading("Milestones", if (hasHistory) "Significant outcomes, confirmed by you" else null)
    val milestones = memory?.milestones.orEmpty()
            if (milestones.isEmpty()) {
                QuietLine(R.drawable.ic_pers_flag, "No confirmed outcomes yet. Significant completed chapters belong here—not routine payments.")
            } else {
                milestones.forEach { MemoryMilestone(it, onOpenMoments) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
            LifeHeading("Look Back", if (hasHistory) "Something worth remembering" else null)
    val look = memory?.lookBack
            if (look == null) {
                QuietLine(R.drawable.ic_pers_history, memory?.lookBackMessage?.ifBlank { null } ?: "No earlier chapter to revisit. Memories will grow from meaningful history.")
            } else {
                MemoryLookBack(look, onOpenMoments)
            }
        }
    }
}

@Composable
private fun MemoryIntro(
    memory: com.example.momentra.data.api.PersonalMemoryExperienceDto?,
    hasHistory: Boolean,
    onExpense: () -> Unit,
) {
    val intro = memory?.intro
    Text(
        intro?.kicker?.ifBlank { null } ?: if (hasHistory) "HISTORY" else "A FRESH START",
        color = PersonalTokens.Colors.BrandPrimary,
        style = PersonalTokens.Type.Metadata,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            intro?.title?.ifBlank { null } ?: if (hasHistory) "What stays with you" else "A story worth keeping.",
            color = PersonalTokens.Colors.TextPrimary,
            style = PersonalTokens.Type.Hero,
            modifier = Modifier.weight(1f),
        )
        if (hasHistory) {
            Text(
                "+ Expense",
                color = PersonalTokens.Colors.TextOnAccent,
                style = PersonalTokens.Type.Chip,
                modifier = Modifier
                    .clip(RoundedCornerShape(PersonalTokens.Radius.Pill))
                    .background(PersonalTokens.Colors.BrandAccent)
                    .clickable(onClick = onExpense)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
    Text(
        intro?.subtitle?.ifBlank { null } ?: if (hasHistory) "The chapters behind your expenses." else "There's room here for the chapters that will matter.",
        color = PersonalTokens.Colors.TextSecondary,
        style = PersonalTokens.Type.Body,
    )
}

@Composable
private fun MemoryOpening(memory: com.example.momentra.data.api.PersonalMemoryExperienceDto?, onExpense: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceHighlight).border(1.dp, PersonalTokens.Colors.BorderDivider, RoundedCornerShape(PersonalTokens.Radius.Card)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(
                painterResource(R.drawable.art_pers_open_beginning),
                contentDescription = null,
                modifier = Modifier.size(112.dp).clip(RoundedCornerShape(56.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text(memory?.intro?.kicker?.substringBefore(" ·")?.ifBlank { null } ?: memory?.openMonthShort.orEmpty(), color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
                Text("The first page is yours.", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero)
            }
        }
        Text("No meaningful history yet. Ordinary expenses stay in activity; meaningful events build this timeline.", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
        Box(
            Modifier.fillMaxWidth().height(PersonalTokens.Size.ButtonVisual).clip(RoundedCornerShape(PersonalTokens.Radius.Button)).background(PersonalTokens.Colors.BrandAccent).clickable(onClick = onExpense),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ Add your first expense", color = PersonalTokens.Colors.TextOnAccent, style = PersonalTokens.Type.Button)
        }
    }
}

@Composable
private fun MemoryRail(kicker: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 4.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(PersonalTokens.Colors.BrandPrimary))
            Box(Modifier.padding(top = 4.dp).width(1.dp).height(36.dp).background(PersonalTokens.Colors.BorderHairline))
        }
        Column(Modifier.weight(1f)) {
            Text(kicker, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
            Text(body, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
        }
    }
}

@Composable
private fun MemoryLabel(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata, modifier = Modifier.weight(1f))
        Image(painterResource(R.drawable.ic_pers_arrow_up_right), contentDescription = null, modifier = Modifier.size(16.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
    }
}

@Composable
private fun MemoryCompletedCard(card: com.example.momentra.data.api.PersonalMomentCardDto, currency: String?, onOpenMoments: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceHighlight).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Card)),
    ) {
        Image(
            painterResource(artwork(card.artworkKey) ?: R.drawable.art_pers_open_beginning),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Illustrated keepsake · not a personal photo", color = PersonalTokens.Colors.TextSubtle, style = PersonalTokens.Type.Caption)
            if (card.statusLabel.isNotBlank()) Text(card.statusLabel, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
            Text(card.title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero.copy(fontSize = 26.sp, lineHeight = 32.sp))
            Text("${card.areaLabel} › ${card.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            Text("${expenseCountLabel(card.activityCount)} · ${inr(card.spend, currency)}", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
            if (card.rangeLabel.isNotBlank()) Text(card.rangeLabel, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            MemoryLabel("Open supporting story", onOpenMoments)
        }
    }
}

@Composable
private fun MemoryOpenMonth(memory: com.example.momentra.data.api.PersonalMemoryExperienceDto?) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceHighlight).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(memory?.openMonthShort?.ifBlank { null } ?: "—", color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.AmountL, modifier = Modifier.weight(1f))
            Text(
                memory?.openMonthChip?.ifBlank { null } ?: "still open",
                color = PersonalTokens.Colors.TextSecondary,
                style = PersonalTokens.Type.Metadata,
                modifier = Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Pill)).background(PersonalTokens.Colors.SurfaceContainer).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Pill)).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Text("Let the month unfold.", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Body)
        Text(
            memory?.openRecap?.ifBlank { null } ?: "No closed-month history yet.",
            color = PersonalTokens.Colors.TextSecondary,
            style = PersonalTokens.Type.Caption,
        )
    }
}

@Composable
private fun MemoryClosedMonth(
    monthly: com.example.momentra.data.api.PersonalMonthlyMemoryDto,
    openNote: String?,
    currency: String?,
    onOpenMoments: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PersonalTokens.Space.Compact)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceHighlight).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Card)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(monthly.shortLabel.ifBlank { monthly.label }, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.AmountL)
                    Text(monthly.closedLabel, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
                }
                Image(painterResource(R.drawable.ic_pers_book_open), contentDescription = null, modifier = Modifier.size(38.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
            }
            Text(monthly.title.ifBlank { monthly.label }, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero.copy(fontSize = 26.sp, lineHeight = 32.sp))
            Column(Modifier.clip(RoundedCornerShape(PersonalTokens.Radius.Tile)).background(PersonalTokens.Colors.FillIconWell).padding(14.dp)) {
                Text(inr(monthly.spend, currency), color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Metric)
                Text("${expenseCountLabel(monthly.expenseCount)} · ${monthly.periodCaption}", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            }
            Row {
                Column(Modifier.weight(1f)) {
                    Text("${monthly.completedCount}", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
                    Text("completed Moments", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                }
                Column(Modifier.weight(1f)) {
                    Text("${monthly.moodCount}", color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.StoryTitle)
                    Text("moods · ${(monthly.moodCoverage * 100).toInt()}% coverage", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
                }
            }
            Text(monthly.summary, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            MemoryLabel(monthly.openLabel.ifBlank { "Open memory" }, onOpenMoments)
        }
        if (!openNote.isNullOrBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_pers_clock_3), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.TextSecondary))
                Text(openNote, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MemoryMilestone(item: com.example.momentra.data.api.PersonalMilestoneDto, onOpenMoments: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, PersonalTokens.Colors.BorderHairline).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(PersonalTokens.Colors.FillIconWell),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_pers_award), contentDescription = null, modifier = Modifier.size(28.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Section)
            if (item.confirmedLabel.isNotBlank()) Text(item.confirmedLabel, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata)
            if (item.areaLabel.isNotBlank()) Text("${item.areaLabel} › ${item.subcategoryLabel}", color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            Text(item.detail, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Caption)
            MemoryLabel("View the completed Moment", onOpenMoments)
        }
    }
}

@Composable
private fun MemoryLookBack(look: com.example.momentra.data.api.PersonalLookBackDto, onOpenMoments: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PersonalTokens.Radius.Card)).background(PersonalTokens.Colors.SurfaceContainer).border(1.dp, PersonalTokens.Colors.BorderHairline, RoundedCornerShape(PersonalTokens.Radius.Card)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(look.kicker, color = PersonalTokens.Colors.BrandPrimary, style = PersonalTokens.Type.Metadata, modifier = Modifier.weight(1f))
            Image(painterResource(R.drawable.ic_pers_history), contentDescription = null, modifier = Modifier.size(20.dp), colorFilter = ColorFilter.tint(PersonalTokens.Colors.BrandPrimary))
        }
        Text(look.title, color = PersonalTokens.Colors.TextPrimary, style = PersonalTokens.Type.Hero)
        Text(look.detail, color = PersonalTokens.Colors.TextSecondary, style = PersonalTokens.Type.Body)
        MemoryLabel("Return to that chapter", onOpenMoments)
    }
}

private fun artwork(key: String?): Int? = when (key) {
    "art_pers_design_course" -> R.drawable.art_pers_design_course
    "art_pers_goa_sunset" -> R.drawable.art_pers_goa_sunset
    "art_pers_pottery_keepsake" -> R.drawable.art_pers_pottery_keepsake
    "art_pers_open_beginning" -> R.drawable.art_pers_open_beginning
    else -> null
}

private fun normalizedAmount(raw: String): String {
    val trimmed = raw.trimEnd('.')
    val dot = trimmed.indexOf('.')
    val capped = if (dot >= 0 && trimmed.length - dot - 1 > 4) trimmed.substring(0, dot + 5) else trimmed
    return capped.ifBlank { "0" }
}

private fun expenseCountLabel(count: Int): String = if (count == 1) "1 expense" else "$count expenses"

private fun sharePercent(share: Double): String = String.format(Locale.US, "%.1f%%", share * 100)

private fun inr(amount: String?, currency: String?): String {
    val number = amount?.toDoubleOrNull() ?: 0.0
    val formatted = String.format(Locale.US, "%,.0f", number)
    return if (currency == null || currency == "INR") "₹$formatted" else "$currency $formatted"
}

private fun formatPulseWhen(iso: String): String = runCatching {
    val time = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
    time.format(DateTimeFormatter.ofPattern("dd MMM · h:mm a", Locale.US))
}.getOrDefault(iso)

private fun formatWhen(iso: String): String = runCatching {
    val time = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
    time.format(DateTimeFormatter.ofPattern("dd MMM yyyy · h:mm a", Locale.US))
}.getOrDefault(iso)

private fun formatSheetWhen(iso: String): String =
    formatWhen(iso).replace(" AM", " am").replace(" PM", " pm")

private fun formatSheetAmount(raw: String): String {
    if (raw.isBlank()) return "₹0"
    val dot = raw.indexOf('.')
    val whole = if (dot >= 0) raw.substring(0, dot) else raw
    val fraction = if (dot >= 0) raw.substring(dot) else ""
    val grouped = whole.toLongOrNull()?.let { String.format(Locale.US, "%,d", it) } ?: whole
    return "₹$grouped$fraction"
}

private fun sanitizeAmount(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    if (dot < 0) return cleaned.trimStart('0').ifEmpty { if (cleaned.isEmpty()) "" else "0" }
    val whole = cleaned.substring(0, dot).trimStart('0').ifEmpty { "0" }
    val fraction = cleaned.substring(dot + 1).filter { it.isDigit() }.take(2)
    return "$whole.$fraction"
}

private fun pickDateTime(context: android.content.Context, iso: String, onPicked: (String) -> Unit) {
    val zoned = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()) }
        .getOrElse { java.time.ZonedDateTime.now() }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val local = java.time.LocalDate.of(year, month + 1, day).atTime(hour, minute)
                    onPicked(local.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString())
                },
                zoned.hour,
                zoned.minute,
                false,
            ).show()
        },
        zoned.year,
        zoned.monthValue - 1,
        zoned.dayOfMonth,
    ).show()
}

private data class PickedPhoto(val preview: ImageBitmap, val bytes: ByteArray)

private fun decodePhoto(context: android.content.Context, uri: Uri): PickedPhoto? = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
    val largest = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val scaled = if (largest <= 1600) bitmap else {
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
    PickedPhoto(scaled.asImageBitmap(), bytes)
}.getOrNull()

private fun expensePhoto(bytes: ByteArray) = PersonalExpensePhotoBody(
    contentType = "image/jpeg",
    dataBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
)

private fun monthUpdatedLabel(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMMM", Locale.US))
}.getOrDefault("This month")
