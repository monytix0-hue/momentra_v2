package com.example.momentra.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.CreateExpenseOutcome
import com.example.momentra.ui.shell.maestro.MaestroIds
import com.example.momentra.ui.theme.MomentraBrandColors
import com.example.momentra.ui.theme.ShellTokens

/**
 * Personal Master Expense — amount-first compact form.
 * Contracted fields only (no Paid From / category catalog / splits).
 */
@Composable
fun ExpenseCreateContent(
    momentId: String,
    momentTitle: String?,
    viewModel: ExpenseCreateViewModel,
    onBack: () -> Unit,
    onCreated: (CreateExpenseOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ShellTokens.SurfaceContent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .semantics {
                    role = Role.Button
                    contentDescription = "Back"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", color = MomentraBrandColors.TextOnDark, fontSize = 28.sp)
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Column {
                Text("Add expense", color = MomentraBrandColors.TextOnDark, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                if (!momentTitle.isNullOrBlank()) {
                    Text(momentTitle, color = ShellTokens.EmptyBody, fontSize = 13.sp)
                }
            }
        }

        Text("Amount", color = ShellTokens.EmptyBody, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = state.amount,
            onValueChange = viewModel::updateAmount,
            textStyle = TextStyle(
                color = MomentraBrandColors.TextOnDark,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = SolidColor(ShellTokens.ContextSelectedPersonal),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Expense amount" }
                .testTag(MaestroIds.PERSONAL_EXPENSE_AMOUNT),
            decorationBox = { inner ->
                if (state.amount.isEmpty()) {
                    Text("0.00", color = ShellTokens.EmptyBody.copy(alpha = 0.4f), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                inner()
            },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Currency", color = ShellTokens.EmptyBody, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = state.currencyCode,
                onValueChange = viewModel::updateCurrency,
                textStyle = TextStyle(color = MomentraBrandColors.TextOnDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(ShellTokens.ContextSelectedPersonal),
                singleLine = true,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1926))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics { contentDescription = "Currency code" },
            )
        }

        Text(
            "More details",
            color = ShellTokens.ContextSelectedPersonal,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable { viewModel.setMoreDetailsOpen(!state.moreDetailsOpen) }
                .testTag(MaestroIds.PERSONAL_EXPENSE_MORE)
                .semantics {
                    role = Role.Button
                    contentDescription = if (state.moreDetailsOpen) "Hide more details" else "Show more details"
                },
        )

        if (state.moreDetailsOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1926))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField("Merchant", state.merchantName, viewModel::updateMerchant)
                LabeledField("Note", state.description, viewModel::updateDescription)
            }
        }

        if (state.error != null) {
            Text(state.error!!, color = Color(0xFFFF8A80), fontSize = 13.sp)
        }

        Button(
            onClick = { viewModel.submit(momentId, onCreated) },
            enabled = !state.submitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(MaestroIds.PERSONAL_EXPENSE_SUBMIT)
                .semantics { contentDescription = "Save expense" },
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Save Expense", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = ShellTokens.EmptyBody, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = MomentraBrandColors.TextOnDark, fontSize = 15.sp),
            cursorBrush = SolidColor(ShellTokens.ContextSelectedPersonal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
