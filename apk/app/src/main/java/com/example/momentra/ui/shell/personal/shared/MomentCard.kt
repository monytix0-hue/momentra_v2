package com.example.momentra.ui.shell.personal.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.R
import com.example.momentra.ui.theme.PlusJakartaSans

private val CardText = Color(0xFFE5E0EE)
private val CardMuted = Color(0xFFC9C4D8)

@Composable
fun MomentCard(
    model: MomentCardModel,
    accent: Color = Color(0xFF7C5CFC),
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconRes(model.sourceIconKind)),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(accent),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    model.title,
                    color = CardText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PlusJakartaSans,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                model.amountLabel?.let {
                    Text(
                        it,
                        color = CardText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                model.contextLabel?.let { context ->
                    Text(
                        context,
                        color = CardMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                model.subtitle?.let {
                    Text(
                        it,
                        color = CardMuted,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                model.signalLabel?.let {
                    Text(
                        it,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    MomentCardTime.label(model.occurredAt),
                    color = CardMuted,
                    fontSize = 10.sp,
                    fontFamily = PlusJakartaSans,
                )
            }
        }
    }
}

private fun iconRes(kind: MomentSourceIconKind): Int = when (kind) {
    MomentSourceIconKind.SPEND -> R.drawable.ic_money_wallet
    MomentSourceIconKind.INCOME -> R.drawable.ic_qa_trending
    MomentSourceIconKind.MOOD -> R.drawable.ic_pulse_smile
    MomentSourceIconKind.RECOVERY, MomentSourceIconKind.WELLBEING -> R.drawable.ic_pulse_activity
    MomentSourceIconKind.ATTENTION, MomentSourceIconKind.MILESTONE -> R.drawable.ic_pulse_target
    MomentSourceIconKind.PROGRESS, MomentSourceIconKind.LEARNING -> R.drawable.ic_pulse_trending
    else -> R.drawable.ic_pulse_zap
}
