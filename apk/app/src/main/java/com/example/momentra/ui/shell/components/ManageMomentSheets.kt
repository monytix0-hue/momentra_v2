package com.example.momentra.ui.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.data.repository.MomentLifecycleRepository
import kotlinx.coroutines.launch

private val SheetBg = Color(0xFF1A1628)
private val RowBg = Color(0xFF14121B)
private val TextPrimary = Color(0xFFF5F2FC)
private val TextMuted = Color(0xFFABA3BA)
private val Purple = Color(0xFF7C5CFC)
private val Blue = Color(0xFF3B82F6)
private val Green = Color(0xFF10B981)
private val Red = Color(0xFFEF4444)
private val RedText = Color(0xFFFF5961)
private val DestructiveBg = Color(0xFF2A1A1A)

enum class ManageMomentPane {
    MENU,
    RENAME,
    DELETE,
    CONFIRM_PAUSE,
    CONFIRM_COMPLETE,
}

@Composable
fun ManageMomentSheet(
    momentId: String,
    momentTitle: String,
    onDismiss: () -> Unit,
    onEditSetup: () -> Unit,
    onLifecycleChanged: () -> Unit,
    repo: MomentLifecycleRepository = remember { MomentLifecycleRepository() },
) {
    var pane by remember { mutableStateOf(ManageMomentPane.MENU) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun runLifecycle(block: suspend (Long) -> Result<*>) {
        scope.launch {
            busy = true
            error = null
            val version = repo.getVersion(momentId).getOrElse {
                busy = false
                error = it.message ?: "Could not load moment"
                return@launch
            }
            block(version).fold(
                onSuccess = {
                    busy = false
                    onLifecycleChanged()
                    onDismiss()
                },
                onFailure = {
                    busy = false
                    error = it.message ?: "Request failed"
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.2f)),
        )
        Spacer(Modifier.height(16.dp))

        when (pane) {
            ManageMomentPane.MENU -> {
                SheetHeader(title = "Manage Moment", subtitle = momentTitle, onClose = onDismiss)
                Spacer(Modifier.height(16.dp))
                ManageRow(Icons.Filled.Settings, Purple, "Edit setup", "Revisit priorities and configuration") {
                    onDismiss()
                    onEditSetup()
                }
                Spacer(Modifier.height(4.dp))
                ManageRow(Icons.Filled.Edit, Blue, "Edit moment name", "Rename how this moment appears") {
                    pane = ManageMomentPane.RENAME
                }
                Spacer(Modifier.height(4.dp))
                ManageRow(Icons.Outlined.Pause, Blue, "Pause rhythm", "Pause tracking without losing your data") {
                    pane = ManageMomentPane.CONFIRM_PAUSE
                }
                Spacer(Modifier.height(4.dp))
                ManageRow(Icons.Filled.Check, Green, "Complete Chapter", "Mark this moment as finished") {
                    pane = ManageMomentPane.CONFIRM_COMPLETE
                }
                Spacer(Modifier.height(8.dp))
                DestructiveRow {
                    pane = ManageMomentPane.DELETE
                }
            }
            ManageMomentPane.RENAME -> {
                RenamePane(
                    momentTitle = momentTitle,
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onSave = { name ->
                        scope.launch {
                            busy = true
                            error = null
                            val version = repo.getVersion(momentId).getOrElse {
                                busy = false
                                error = it.message
                                return@launch
                            }
                            repo.rename(momentId, name, version).fold(
                                onSuccess = {
                                    busy = false
                                    onLifecycleChanged()
                                    onDismiss()
                                },
                                onFailure = {
                                    busy = false
                                    error = it.message
                                },
                            )
                        }
                    },
                )
            }
            ManageMomentPane.DELETE -> {
                DeletePane(
                    momentTitle = momentTitle,
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onDelete = { runLifecycle { repo.delete(momentId, it) } },
                )
            }
            ManageMomentPane.CONFIRM_PAUSE -> {
                ConfirmPane(
                    title = "Pause rhythm?",
                    body = "Pause tracking without losing your data.",
                    confirmLabel = "Pause",
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onConfirm = { runLifecycle { repo.archive(momentId, it) } },
                )
            }
            ManageMomentPane.CONFIRM_COMPLETE -> {
                ConfirmPane(
                    title = "Complete Chapter?",
                    body = "Mark this moment as finished.",
                    confirmLabel = "Complete",
                    busy = busy,
                    error = error,
                    onClose = { pane = ManageMomentPane.MENU },
                    onConfirm = { runLifecycle { repo.cancel(momentId, it) } },
                )
            }
        }

        error?.takeIf { pane == ManageMomentPane.MENU }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = RedText, fontSize = 12.sp)
        }
        if (busy && pane == ManageMomentPane.MENU) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Purple)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(subtitle, color = TextMuted, fontSize = 10.sp)
        }
        Icon(
            Icons.Filled.Close,
            contentDescription = "Close",
            tint = Purple,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onClose),
        )
    }
}

@Composable
private fun ManageRow(
    icon: ImageVector,
    well: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RowBg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(well),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun DestructiveRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DestructiveBg)
            .border(1.dp, Red, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Red),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Delete permanently", color = RedText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Removes this moment; analytics kept", color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun RenamePane(
    momentTitle: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(momentTitle) }
    SheetHeader(title = "Edit Moment Name", subtitle = momentTitle, onClose = onClose)
    Spacer(Modifier.height(16.dp))
    Text("MOMENT NAME", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Blue) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = RowBg,
            unfocusedContainerColor = RowBg,
            focusedBorderColor = Purple,
            unfocusedBorderColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(12.dp),
    )
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onSave(name.trim()) },
        enabled = !busy && name.trim().isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Purple),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("Save", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}

@Composable
private fun DeletePane(
    momentTitle: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    SheetHeader(title = "Delete Permanently?", subtitle = momentTitle, onClose = onClose)
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Red),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, null, tint = Color.White)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "This cannot be undone. The moment and operational data will be removed; analytics are kept.",
        color = TextMuted,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onDelete,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Red),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text("Delete", fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}

@Composable
private fun ConfirmPane(
    title: String,
    body: String,
    confirmLabel: String,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    SheetHeader(title = title, subtitle = "", onClose = onClose)
    Spacer(Modifier.height(12.dp))
    Text(body, color = TextMuted, fontSize = 13.sp)
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = RedText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onConfirm,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Purple),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(confirmLabel, fontWeight = FontWeight.Bold)
    }
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel", color = TextMuted)
    }
}
