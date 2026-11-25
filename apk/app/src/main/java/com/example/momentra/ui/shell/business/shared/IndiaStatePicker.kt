package com.example.momentra.ui.shell.business.shared

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.theme.PlusJakartaSans

/**
 * The one state picker used by every Business surface.
 *
 * The list is the canonical India States & UTs ([IndiaStateCatalog.all]) bundled into the app, so
 * it opens instantly offline. States come first, union territories after, each row carrying its
 * two-digit GST code — the same code the backend uses to decide CGST + SGST versus IGST.
 *
 * A row writes back the canonical full name, never the code, so existing display code is
 * unchanged and a saved value reads the same as it always did.
 */
@Composable
fun IndiaStateSheet(
    selected: String,
    theme: OperatingSheetTheme,
    onDismiss: () -> Unit,
    onPick: (IndiaState) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query) { IndiaStateCatalog.search(query) }
    val chosen = IndiaStateCatalog.codeOf(selected)

    OperatingSheetScrim(
        onDismiss = onDismiss,
        accent = theme.accent,
        surface = theme.canvas,
        border = theme.border,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
            }
            Text(
                "State",
                color = theme.ink,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = theme.ink, fontFamily = PlusJakartaSans, fontSize = 14.sp),
                cursorBrush = SolidColor(theme.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.inset)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(
                            "Search a state or code",
                            color = theme.body.copy(alpha = 0.65f),
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                },
            )
            if (matches.isEmpty()) {
                Text(
                    "No state matches \"$query\".",
                    color = theme.body,
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                )
            }
            var lastGroup: Boolean? = null
            matches.forEach { state ->
                if (lastGroup != state.isUnionTerritory) {
                    lastGroup = state.isUnionTerritory
                    Text(
                        if (state.isUnionTerritory) "Union territories" else "States",
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                val on = chosen.isNotEmpty() && state.code == chosen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) theme.accent.copy(alpha = 0.14f) else theme.inset)
                        .border(
                            1.dp,
                            if (on) theme.accent else theme.border,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onPick(state) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.name,
                        color = theme.ink,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        state.code,
                        color = theme.body,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                    )
                    if (on) Text("●", color = theme.accent, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A form row that opens a picker instead of a keyboard, matching the master form's fields. */
@Composable
fun OperatingPickerRow(
    label: String,
    value: String,
    theme: OperatingSheetTheme,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String = "Pick one",
    onClick: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = theme.body, fontFamily = PlusJakartaSans, fontSize = 11.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.inset)
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value.ifBlank { placeholder },
                color = if (value.isBlank()) theme.body.copy(alpha = 0.65f) else theme.ink,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("›", color = theme.body, fontFamily = PlusJakartaSans, fontSize = 16.sp)
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, color = theme.body.copy(alpha = 0.65f), fontFamily = PlusJakartaSans, fontSize = 11.sp)
        }
    }
}
