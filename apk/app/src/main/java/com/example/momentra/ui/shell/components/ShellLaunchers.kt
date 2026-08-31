package com.example.momentra.ui.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.momentra.domain.AppContext
import com.example.momentra.domain.ShellContentState
import com.example.momentra.domain.ShellIdentity
import com.example.momentra.ui.shell.policy.ShellScreenResolver
import com.example.momentra.ui.shell.policy.ShellScreenSlot
import com.example.momentra.ui.theme.ShellTokens
import com.example.momentra.ui.theme.shell.MomentThemes

/** Shell-owned Quick Add presentation port — action catalogs stay in S2–S4. */
@Composable
fun QuickAddLauncher(
    context: AppContext,
    momentTypeCode: String?,
    capabilities: List<String>,
    enabled: Boolean,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canLaunch = enabled && capabilities.isNotEmpty()
    Button(
        onClick = onLaunch,
        enabled = canLaunch,
        modifier = modifier.semantics {
            contentDescription = if (canLaunch) "Open Quick Add" else "Quick Add unavailable"
        },
    ) {
        Text("Quick Add")
    }
    // Accent binding for callers: MomentThemes.resolve(context, momentTypeCode).primary
    @Suppress("UNUSED_VARIABLE")
    val accent = MomentThemes.resolve(context, momentTypeCode).primary
}

/** Global Create Moment — separate from Quick Add. */
@Composable
fun CreateMomentLauncher(
    enabled: Boolean,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onLaunch,
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = "Create moment" },
    ) {
        Text("Create Moment")
    }
}

@Composable
fun ShellProfileSheet(
    identity: ShellIdentity,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = identity.displayName ?: "Member", fontSize = 16.sp)
        identity.email?.let { Text(text = it, fontSize = 14.sp) }
        Button(
            onClick = onSignOut,
            modifier = Modifier.semantics { contentDescription = "Sign out" },
        ) {
            Text("Sign out")
        }
        TextButton(onClick = onClose) { Text("Close") }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Reusable shell-level state containers (not domain empty artwork). */
@Composable
fun ShellGlobalStateHost(
    content: ShellContentState,
    life360Open: Boolean,
    profileOpen: Boolean,
    onRetry: () -> Unit,
    product: @Composable () -> Unit,
    empty: @Composable () -> Unit,
    deferred: @Composable () -> Unit = {
        ShellMessageState(title = "Coming soon", body = "This surface is not available yet.")
    },
) {
    when (ShellScreenResolver.resolve(content, life360Open, profileOpen)) {
        ShellScreenSlot.LOADING -> ShellLoadingState()
        ShellScreenSlot.OFFLINE -> ShellMessageState(
            title = "You're offline",
            body = "Check your connection, then retry.",
            actionLabel = "Retry",
            onAction = onRetry,
        )
        ShellScreenSlot.UNAUTHORIZED -> ShellMessageState(
            title = "Session expired",
            body = "Sign in again to continue.",
        )
        ShellScreenSlot.ERROR -> {
            val err = content as? ShellContentState.Error
            ShellMessageState(
                title = "Something went wrong",
                body = err?.message ?: "Please try again.",
                actionLabel = "Retry",
                onAction = onRetry,
            )
        }
        ShellScreenSlot.DEFERRED -> deferred()
        ShellScreenSlot.EMPTY -> empty()
        ShellScreenSlot.PRODUCT -> product()
        ShellScreenSlot.LIFE360_GLOBAL, ShellScreenSlot.PROFILE -> product()
    }
}

@Composable
fun ShellLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = ShellTokens.ContextSelectedPersonal)
    }
}

@Composable
fun ShellMessageState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = ShellTokens.EmptyBody)
        Text(
            text = body,
            fontSize = 14.sp,
            color = ShellTokens.EmptyBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) {
                Text(actionLabel)
            }
        }
    }
}
