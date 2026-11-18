package com.example.momentra.ui.shell.business.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.ui.shell.business.mto.MtoToastAction
import com.example.momentra.ui.shell.business.mto.MtoToastCatalog
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay

private val ToastPanel = Color(0xFF16181F)
private val ToastInk = Color(0xFFF8F9FD)
private val ToastMuted = Color(0xFF9496A8)
private val ToastBtn = Color(0xFF1C1F28)

/** Process-wide operating toast host — shared across business kits. */
object OperatingToastHost {
    var current by mutableStateOf<MtoToastSpec?>(null)
        private set

    var onAction: ((MtoToastAction) -> Unit)? = null

    fun show(spec: MtoToastSpec) {
        current = spec
    }

    fun show(kind: MtoToastKind, entity: String = "this job", businessLabel: String = "Make to order") {
        show(MtoToastCatalog.of(kind, entity, businessLabel))
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun OperatingToastOverlay(
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 4200L,
) {
    val spec = OperatingToastHost.current
    AnimatedVisibility(
        visible = spec != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val toast = spec ?: return@AnimatedVisibility
        LaunchedEffect(toast) {
            delay(autoDismissMs)
            if (OperatingToastHost.current == toast) OperatingToastHost.dismiss()
        }
        OperatingToastPanel(
            spec = toast,
            onAction = {
                val action = toast.action
                OperatingToastHost.dismiss()
                if (action != MtoToastAction.Dismiss) {
                    OperatingToastHost.onAction?.invoke(action)
                }
            },
        )
    }
}

@Composable
fun OperatingToastPanel(
    spec: MtoToastSpec,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, shape, ambientColor = spec.accent.copy(alpha = 0.12f), spotColor = spec.accent.copy(alpha = 0.12f))
                .clip(shape)
                .background(ToastPanel)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(spec.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = spec.accent, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(spec.title, color = ToastInk, fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(spec.body, color = ToastMuted, fontFamily = PlusJakartaSans, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ToastBtn)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    spec.actionLabel,
                    color = spec.accent,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(spec.accent.copy(alpha = 0.6f)),
        )
    }
}