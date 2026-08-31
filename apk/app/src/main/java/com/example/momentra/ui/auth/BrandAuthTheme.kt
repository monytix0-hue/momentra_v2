package com.example.momentra.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.MomentraBrandColors

/** Auth UI on the global brand canvas — mirrors `.auth-*` classes in `design/momentra_theme.css`. */
private val InputShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(100.dp)

@Composable
fun BrandAuthScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MomentraBrandColors.Brand)
            .testTag(MaestroIds.LOGIN_SCREEN),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun BrandPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MomentraBrandColors.Cta,
            contentColor = MomentraBrandColors.TextOnEmber,
            disabledContainerColor = MomentraBrandColors.Cta.copy(alpha = 0.5f),
            disabledContentColor = MomentraBrandColors.TextOnEmber.copy(alpha = 0.7f),
        ),
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BrandSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = PillShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MomentraBrandColors.TextOnDark,
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BrandTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        visualTransformation = visualTransformation,
        modifier = modifier
            .fillMaxWidth()
            .background(MomentraBrandColors.Indigo500, InputShape)
            .border(1.dp, MomentraBrandColors.Indigo100, InputShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        textStyle = TextStyle(
            color = MomentraBrandColors.TextOnDark,
            fontSize = 13.sp,
        ),
        cursorBrush = SolidColor(MomentraBrandColors.TextOnDark),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MomentraBrandColors.TextOnDark.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
fun BrandFieldLabel(text: String) {
    Text(
        text = text,
        color = MomentraBrandColors.TextOnDark.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun BrandOrDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .heightIn(min = 1.dp)
                .background(Color.White.copy(alpha = 0.15f)),
        )
        Text(
            text = "or",
            color = MomentraBrandColors.TextOnDark.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(MomentraBrandColors.Brand)
                .padding(horizontal = 8.dp),
        )
    }
}
