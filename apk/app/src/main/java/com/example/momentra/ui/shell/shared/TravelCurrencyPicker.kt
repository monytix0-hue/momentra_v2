package com.example.momentra.ui.shell.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.shell.group.shared.TravelCurrencyCatalog
import com.example.momentra.ui.theme.PlusJakartaSans

@Composable
fun TravelCurrencyPickerRow(
    selectedCode: String,
    onSelected: (String) -> Unit,
    preferredCodes: List<String>,
    modifier: Modifier = Modifier,
    label: String = "Currency",
    textColor: Color = Color(0xFFE5E0EE),
    secondaryColor: Color = Color(0xFFC9C4D8),
    accentColor: Color = Color(0xFF14B8A6),
    labelFontSize: TextUnit = 12.sp,
    valueFontSize: TextUnit = 14.sp,
    symbolFontSize: TextUnit = 28.sp,
    cornerRadius: Dp = 12.dp,
    showLabel: Boolean = true,
) {
    val options = remember(preferredCodes) { MomentCurrencyResolver.pickerOptions(preferredCodes) }
    var menuOpen by remember { mutableStateOf(false) }
    val symbol = TravelCurrencyCatalog.symbol(selectedCode)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menuOpen = true }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = if (showLabel) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showLabel) {
                Text(label, color = secondaryColor, fontSize = labelFontSize, fontFamily = PlusJakartaSans)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    symbol,
                    color = accentColor,
                    fontSize = symbolFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
                Text(
                    selectedCode,
                    color = textColor,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_biz_create_chevron),
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            options.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            TravelCurrencyCatalog.display(code),
                            fontFamily = PlusJakartaSans,
                        )
                    },
                    onClick = {
                        onSelected(code)
                        menuOpen = false
                    },
                )
            }
        }
    }
}
