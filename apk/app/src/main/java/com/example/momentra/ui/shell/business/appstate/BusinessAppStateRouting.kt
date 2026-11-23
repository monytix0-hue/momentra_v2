package com.example.momentra.ui.shell.business.appstate

import com.example.momentra.domain.BottomDestination
import com.example.momentra.domain.ShellContentState

/**
 * Maps shell destination + content state onto the Obsidian AppState kit.
 *
 * Keep in lockstep with `BusinessAppStateRouting.swift` on iOS.
 */
object BusinessAppStateRouting {

    fun screenFor(destination: BottomDestination, hasCompany: Boolean): BusinessAppStateScreen {
        if (!hasCompany) return BusinessAppStateScreen.NoCompany
        return when (destination) {
            BottomDestination.PULSE -> BusinessAppStateScreen.Pulse
            BottomDestination.MOMENTS -> BusinessAppStateScreen.Moments
            BottomDestination.LIFE -> BusinessAppStateScreen.Life
            BottomDestination.MEMORY -> BusinessAppStateScreen.Memory
            BottomDestination.CREATE -> BusinessAppStateScreen.Hub
        }
    }

    fun phaseFor(content: ShellContentState, hasCompany: Boolean): BusinessAppStatePhase {
        if (!hasCompany) return BusinessAppStatePhase.Setup
        return when (content) {
            ShellContentState.Loading, ShellContentState.Idle -> BusinessAppStatePhase.Loading
            ShellContentState.Offline -> BusinessAppStatePhase.Error
            is ShellContentState.Error, ShellContentState.Forbidden -> BusinessAppStatePhase.Error
            ShellContentState.Empty, is ShellContentState.Ready, ShellContentState.Deferred ->
                BusinessAppStatePhase.Empty
        }
    }

    /** Offline is a dedicated screen, not the destination's error variant. */
    fun resolveScreen(
        destination: BottomDestination,
        content: ShellContentState,
        hasCompany: Boolean,
    ): BusinessAppStateScreen {
        if (content is ShellContentState.Offline) return BusinessAppStateScreen.Offline
        return screenFor(destination, hasCompany)
    }

    fun loadingLabel(destination: BottomDestination): String =
        BusinessAppStateSpecs.spec(
            screenFor(destination, hasCompany = true),
            BusinessAppStatePhase.Loading,
        ).title

    fun titleFor(
        destination: BottomDestination,
        content: ShellContentState,
        hasCompany: Boolean,
    ): String {
        val screen = resolveScreen(destination, content, hasCompany)
        val phase = phaseFor(content, hasCompany)
        return if (phase == BusinessAppStatePhase.Loading) {
            loadingLabel(destination)
        } else {
            BusinessAppStateSpecs.spec(screen, phase).title
        }
    }
}
