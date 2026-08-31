package com.example.momentra.ui.shell.empty.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R

/** Figma 575:8894 — Group Create / Choose a Moment */
@Composable
fun GroupCreateMomentContent(
    onBack: () -> Unit,
    onSelectExperience: () -> Unit = {},
    onSelectPurchase: () -> Unit = {},
    onSelectLiving: () -> Unit = {},
    onJoinCode: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showScanner by remember { mutableStateOf(false) }
    Box(modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GeBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 16.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Back"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("‹", color = GeText, fontSize = 32.sp)
            Text("Choose a Moment", color = GeText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            GeAppear {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Plan life together, in one place.",
                        color = GeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    )
                    Text(
                        "Create a moment to plan, contribute, coordinate and stay in sync.",
                        color = GeSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    Image(
                        painter = painterResource(R.drawable.group_create_hero),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            GeAppear(delayMillis = 80) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BenefitChip(
                        iconRes = R.drawable.group_create_benefit_plan,
                        border = Color(0xFFF59E0B),
                        label = "Plan",
                        modifier = Modifier.weight(1f),
                    )
                    BenefitChip(
                        iconRes = R.drawable.group_create_benefit_contribute,
                        border = Color(0xFFFB7185),
                        label = "Contribute",
                        modifier = Modifier.weight(1f),
                    )
                    BenefitChip(
                        iconRes = R.drawable.group_create_benefit_coordinate,
                        border = Color(0xFF14B8A6),
                        label = "Coordinate",
                        modifier = Modifier.weight(1f),
                    )
                    BenefitChip(
                        iconRes = R.drawable.group_create_benefit_remember,
                        border = Color(0xFF8B5CF6),
                        label = "Remember",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            GeAppear(delayMillis = 140) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Choose what you want to do together",
                        color = GeText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 21.sp,
                    )
                    GeMomentTypeGrid(
                        onSelectExperience = onSelectExperience,
                        onSelectPurchase = onSelectPurchase,
                        onSelectLiving = onSelectLiving,
                    )
                    GeScanJoinButton(onClick = { showScanner = true })
                    Text(
                        "You can always add more moments later.",
                        color = GeSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (showScanner) {
        GroupJoinQrScanner(
            onCode = { code ->
                showScanner = false
                onJoinCode(code)
            },
            onDismiss = { showScanner = false },
        )
    }
    }
}

@Composable
private fun BenefitChip(
    iconRes: Int,
    border: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GeSurfaceHigh)
                .border(1.dp, border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(iconRes), null, Modifier.size(16.dp))
        }
        Text(label, color = GeSecondary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}
