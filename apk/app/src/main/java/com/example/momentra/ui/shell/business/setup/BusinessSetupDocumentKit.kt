package com.example.momentra.ui.shell.business.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import java.time.LocalDate
import com.example.momentra.ui.setup.SetupDateTimeUtils
import com.example.momentra.ui.shell.business.appstate.BusinessGlassModalBottomSheet
import com.example.momentra.ui.shell.business.appstate.BusinessGlassSheetChrome
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DatePicker
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans
import com.example.momentra.ui.theme.generated.BusinessTokens

/** Shared atoms for Business setup documents. Obsidian chrome — lockstep with iOS. */
object BusinessSetupDocumentKit {

    @Composable
    fun HeroCard(
        theme: BusinessSetupTheme,
        eyebrow: String,
        title: String,
        subtitle: String,
        logoBitmap: ImageBitmap? = null,
        logoUrl: String? = null,
        logoUploading: Boolean = false,
        onLogoClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(theme.canvasTop.copy(alpha = 0.95f), theme.surface.copy(alpha = 0.88f)),
                    ),
                )
                .border(1.dp, theme.accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    theme.accent.copy(alpha = 0.55f),
                                    theme.accentSoft.copy(alpha = 0.22f),
                                    theme.canvas.copy(alpha = 0f),
                                ),
                            ),
                            CircleShape,
                        ),
                )
                val logoClick = onLogoClick
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(theme.inset)
                        .border(1.5.dp, theme.accent.copy(alpha = 0.45f), CircleShape)
                        .then(
                            if (logoClick != null) {
                                Modifier
                                    .semantics {
                                        contentDescription = "Upload logo"
                                        role = Role.Button
                                    }
                                    .clickable(onClick = logoClick)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        logoUploading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = theme.accent,
                                strokeWidth = 2.dp,
                            )
                        }
                        logoBitmap != null -> {
                            Image(
                                bitmap = logoBitmap,
                                contentDescription = "Upload logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        // No Coil / remote image helper on classpath — glyph for blank or remote URL.
                        !logoUrl.isNullOrBlank() -> {
                            Text("▣", color = theme.accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                        else -> {
                            Text("▣", color = theme.accent, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Text(
                text = eyebrow.uppercase(),
                color = theme.accentSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                fontFamily = PlusJakartaSans,
            )
            Text(
                text = title,
                color = BusinessTokens.Colors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
            Text(
                text = subtitle,
                color = theme.hint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = PlusJakartaSans,
            )
        }
    }

    /** Figma `Kind of business` panel (`5:71178`) — equal tiles with glyph + border select. */
    @Composable
    fun SubtypeChips(
        theme: BusinessSetupTheme,
        chips: List<BusinessSetupChip>,
        selected: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.surface.copy(alpha = 0.88f))
                .border(1.dp, theme.accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Kind of business",
                color = theme.hint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { chip ->
                    val isOn = chip.typeCode == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.surface.copy(alpha = 0.95f))
                            .border(
                                width = if (isOn) 1.5.dp else 1.dp,
                                color = if (isOn) theme.accent else theme.accent.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelect(chip.typeCode) }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "◈",
                            color = if (isOn) BusinessTokens.Colors.TextPrimary else theme.hint,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        Text(
                            text = chip.label,
                            color = if (isOn) BusinessTokens.Colors.TextPrimary else theme.hint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SectionCard(
        theme: BusinessSetupTheme,
        index: Int,
        title: String,
        subtitle: String?,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(theme.surface.copy(alpha = 0.78f))
                .border(1.dp, theme.accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "%02d".format(index),
                    color = theme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = PlusJakartaSans,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        color = BusinessTokens.Colors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = theme.hint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                }
            }
            content()
        }
    }

    @Composable
    fun FieldRow(
        theme: BusinessSetupTheme,
        label: String,
        placeholder: String,
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        helper: String = "",
        error: String? = null,
        forceUppercase: Boolean = false,
    ) {
        val showError = BusinessSetupFieldChrome.hasError(error)
        val focusManager = LocalFocusManager.current
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                color = theme.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.inset)
                    .border(
                        1.dp,
                        if (showError) BusinessTokens.Colors.SignalDanger else theme.accent.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = theme.hint, fontSize = 15.sp, fontFamily = PlusJakartaSans)
                }
                BasicTextField(
                    value = value,
                    onValueChange = { raw ->
                        val next = when {
                            forceUppercase -> BusinessSetupDateFormatting.forceUppercaseInput(raw)
                            BusinessSetupAmountFormatting.isAmountHelper(helper) ->
                                BusinessSetupAmountFormatting.formatIndianAmount(raw)
                            else -> raw
                        }
                        onValueChange(next)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (forceUppercase) {
                            KeyboardCapitalization.Characters
                        } else {
                            KeyboardCapitalization.None
                        },
                        imeAction = BusinessSetupFieldInputPolicy.imeAction(),
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (BusinessSetupFieldInputPolicy.CLEAR_FOCUS_ON_SUBMIT) {
                                focusManager.clearFocus()
                            }
                        },
                    ),
                    textStyle = TextStyle(
                        color = BusinessTokens.Colors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    ),
                    cursorBrush = SolidColor(theme.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showError) {
                Text(
                    text = error.orEmpty(),
                    color = BusinessTokens.Colors.SignalDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            } else if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = theme.hint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }

    @Composable
    fun PanelHeader(
        theme: BusinessSetupTheme,
        title: String,
        modifier: Modifier = Modifier,
    ) {
        Text(
            text = title,
            color = theme.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            modifier = modifier.padding(top = 2.dp),
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ChoiceChips(
        theme: BusinessSetupTheme,
        options: List<String>,
        selected: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isOn = option == selected
                Text(
                    text = option,
                    color = if (isOn) theme.onAccent else theme.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isOn) theme.accent else theme.inset)
                        .border(1.dp, theme.accent.copy(alpha = if (isOn) 0.5f else 0.2f), CircleShape)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun MultiChoiceChips(
        theme: BusinessSetupTheme,
        options: List<String>,
        selectedPipe: String,
        onChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        error: String? = null,
        helper: String = "",
    ) {
        val selected = selectedPipe.split("|").filter { it.isNotBlank() }.toSet()
        val showError = BusinessSetupFieldChrome.hasError(error)
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (showError) {
                            Modifier
                                .border(1.dp, BusinessTokens.Colors.SignalDanger, RoundedCornerShape(12.dp))
                                .padding(6.dp)
                        } else {
                            Modifier
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    val isOn = option in selected
                    Text(
                        text = option,
                        color = if (isOn) theme.onAccent else theme.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isOn) theme.accent else theme.inset)
                            .border(1.dp, theme.accent.copy(alpha = if (isOn) 0.5f else 0.2f), CircleShape)
                            .clickable {
                                val next = selected.toMutableSet()
                                if (isOn) next.remove(option) else next.add(option)
                                onChange(options.filter { it in next }.joinToString("|"))
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (showError) {
                Text(
                    text = error.orEmpty(),
                    color = BusinessTokens.Colors.SignalDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            } else if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = theme.hint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }

    @Composable
    fun ToggleRow(
        theme: BusinessSetupTheme,
        label: String,
        value: String,
        onChange: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val isOn = value != "No" && !value.equals("false", ignoreCase = true)
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = theme.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (isOn) "Yes" else "No",
                color = if (isOn) theme.onAccent else theme.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isOn) theme.accent else theme.inset)
                    .border(1.dp, theme.accent.copy(alpha = 0.35f), CircleShape)
                    .clickable { onChange(if (isOn) "No" else "Yes") }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DateRow(
        theme: BusinessSetupTheme,
        label: String,
        placeholder: String,
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        helper: String = "",
        error: String? = null,
        fieldKey: String = "",
    ) {
        val mode = BusinessSetupDateFormatting.modeForKey(fieldKey)
        val allowClear = BusinessSetupDateFormatting.allowClearForKey(fieldKey)
        val showError = BusinessSetupFieldChrome.hasError(error)
        var showPicker by remember { mutableStateOf(false) }
        val display = BusinessSetupDateFormatting.formatDisplay(value, mode, placeholder)

        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                color = theme.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.inset)
                    .border(
                        1.dp,
                        if (showError) BusinessTokens.Colors.SignalDanger else theme.accent.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { showPicker = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = display,
                    color = if (value.isBlank()) theme.hint else BusinessTokens.Colors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Text("▼", color = theme.hint, fontSize = 12.sp)
            }
            if (showError) {
                Text(
                    text = error.orEmpty(),
                    color = BusinessTokens.Colors.SignalDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            } else if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = theme.hint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }

        if (showPicker) {
            val initial = BusinessSetupDateFormatting.parseToLocalDate(value) ?: LocalDate.now()
            val state = rememberDatePickerState(
                initialSelectedDateMillis = SetupDateTimeUtils.localDateToMillis(initial),
            )
            // Solid calendar backing (#101623) — glass sheets are too transparent to read days.
            val calendarSurface = Color(0xFF101623)
            BusinessGlassModalBottomSheet(onDismissRequest = { showPicker = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(calendarSurface),
                ) {
                    Text(
                        text = label,
                        color = theme.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    DatePicker(
                        state = state,
                        colors = androidx.compose.material3.DatePickerDefaults.colors(
                            containerColor = calendarSurface,
                            titleContentColor = theme.label,
                            headlineContentColor = BusinessTokens.Colors.TextPrimary,
                            weekdayContentColor = theme.hint,
                            subheadContentColor = theme.hint,
                            yearContentColor = BusinessTokens.Colors.TextPrimary,
                            currentYearContentColor = theme.accent,
                            selectedYearContentColor = Color.White,
                            selectedYearContainerColor = theme.accent.copy(alpha = 0.55f),
                            dayContentColor = BusinessTokens.Colors.TextPrimary,
                            selectedDayContentColor = Color.White,
                            selectedDayContainerColor = theme.accent.copy(alpha = 0.70f),
                            todayContentColor = theme.accent,
                            todayDateBorderColor = theme.accent.copy(alpha = 0.70f),
                            disabledDayContentColor = theme.hint.copy(alpha = 0.40f),
                        ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (allowClear) {
                            TextButton(onClick = {
                                onValueChange("")
                                showPicker = false
                            }) { Text("Clear") }
                        }
                        TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            state.selectedDateMillis?.let { millis ->
                                val picked = SetupDateTimeUtils.millisToLocalDate(millis)
                                onValueChange(BusinessSetupDateFormatting.toStorage(picked, mode))
                            }
                            showPicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    @Composable
    fun PickerRow(
        theme: BusinessSetupTheme,
        label: String,
        value: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        placeholder: String = "Select",
        helper: String = "",
        error: String? = null,
    ) {
        val showError = BusinessSetupFieldChrome.hasError(error)
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                color = theme.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.inset)
                    .border(
                        1.dp,
                        if (showError) BusinessTokens.Colors.SignalDanger else theme.accent.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    color = if (value.isBlank()) theme.hint else BusinessTokens.Colors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                    modifier = Modifier.weight(1f),
                )
                Text("▼", color = theme.hint, fontSize = 12.sp)
            }
            if (showError) {
                Text(
                    text = error.orEmpty(),
                    color = BusinessTokens.Colors.SignalDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            } else if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = theme.hint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }

    @Composable
    fun CurrencyDropdown(
        theme: BusinessSetupTheme,
        label: String,
        options: List<String>,
        selected: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier,
        helper: String = "",
        error: String? = null,
    ) {
        val showError = BusinessSetupFieldChrome.hasError(error)
        var expanded by remember { mutableStateOf(false) }
        val shown = selected.ifBlank { options.firstOrNull().orEmpty() }
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                color = theme.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
            )
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.inset)
                        .border(
                            1.dp,
                            if (showError) BusinessTokens.Colors.SignalDanger else theme.accent.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = shown.ifBlank { "Select currency" },
                        color = if (shown.isBlank()) theme.hint else BusinessTokens.Colors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.weight(1f),
                    )
                    Text("▼", color = theme.hint, fontSize = 12.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontFamily = PlusJakartaSans) },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
            if (showError) {
                Text(
                    text = error.orEmpty(),
                    color = BusinessTokens.Colors.SignalDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                )
            } else if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = theme.hint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }

    @Composable
    fun RowAction(
        theme: BusinessSetupTheme,
        label: String,
        action: String,
        onTap: () -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.inset)
                .border(1.dp, theme.accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                .clickable(onClick = onTap)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = BusinessTokens.Colors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = action,
                color = theme.onAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(theme.accent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }

    @Composable
    fun MemberList(
        theme: BusinessSetupTheme,
        members: List<String>,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            members.forEach { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.inset.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.accent.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = member.take(1),
                            color = theme.onAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Text(
                        text = member,
                        color = BusinessTokens.Colors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }

    @Composable
    fun ModuleList(
        theme: BusinessSetupTheme,
        modules: List<BusinessSetupModuleRow>,
        modifier: Modifier = Modifier,
        helper: String = "",
    ) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            modules.forEach { module ->
                val isAction = module.title.trimStart().startsWith("+")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.inset)
                        .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.accent.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isAction) "+" else "▣",
                            color = theme.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = module.title,
                            color = if (isAction) theme.accent else BusinessTokens.Colors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                        )
                        if (module.subtitle.isNotBlank()) {
                            Text(
                                text = module.subtitle,
                                color = theme.hint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = PlusJakartaSans,
                            )
                        }
                    }
                    Text(
                        text = "›",
                        color = theme.accentSoft,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            if (helper.isNotBlank()) {
                Text(
                    text = helper,
                    color = theme.hint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }

    @Composable
    fun ReviewSummary(
        theme: BusinessSetupTheme,
        keys: List<String>,
        values: Map<String, String>,
        labelsByKey: Map<String, String>,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.inset.copy(alpha = 0.85f))
                .border(1.dp, theme.accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            keys.forEach { key ->
                val label = labelsByKey[key] ?: key
                val raw = values[key]?.trim().orEmpty()
                val display = if (raw.isEmpty()) "—" else raw.replace("|", ", ")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        color = theme.hint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = display,
                        color = BusinessTokens.Colors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }

    @Composable
    fun StickyActivateBar(
        theme: BusinessSetupTheme,
        label: String,
        enabled: Boolean,
        submitting: Boolean,
        onActivate: () -> Unit,
        modifier: Modifier = Modifier,
        draftEnabled: Boolean = false,
        savingDraft: Boolean = false,
        onSaveDraft: (() -> Unit)? = null,
        onScheduleLater: (() -> Unit)? = null,
    ) {
        Column(modifier = modifier.fillMaxWidth().background(theme.canvas).imePadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(theme.canvas.copy(alpha = 0f), theme.canvas),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onSaveDraft != null || onScheduleLater != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (onSaveDraft != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.inset)
                                    .border(1.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable(
                                        enabled = draftEnabled && !savingDraft && !submitting,
                                        onClick = onSaveDraft,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                if (savingDraft) {
                                    CircularProgressIndicator(
                                        color = theme.accent,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.width(16.dp).height(16.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (savingDraft) "Saving…" else "Save draft",
                                    color = theme.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                        }
                        if (onScheduleLater != null) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .clickable(
                                        enabled = !savingDraft && !submitting,
                                        onClick = onScheduleLater,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "Schedule later",
                                    color = BusinessTokens.Colors.TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = PlusJakartaSans,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(51.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (enabled && !submitting) {
                                Brush.horizontalGradient(listOf(theme.accent, theme.accentSoft))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(theme.accent.copy(alpha = 0.35f), theme.accentSoft.copy(alpha = 0.25f)),
                                )
                            },
                        )
                        .clickable(enabled = enabled && !submitting && !savingDraft, onClick = onActivate),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            color = theme.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.width(18.dp).height(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (submitting) "Activating…" else label,
                        color = theme.onAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
        }
    }
}
