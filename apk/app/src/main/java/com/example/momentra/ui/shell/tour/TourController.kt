package com.example.momentra.ui.shell.tour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.momentra.data.local.AppPreferences
import com.example.momentra.domain.BottomDestination

class TourController(
    private val prefs: AppPreferences,
) {
    var activeTour by mutableStateOf<TourId?>(null)
        private set
    var stepIndex by mutableIntStateOf(0)
        private set
    var steps by mutableStateOf<List<TourStep>>(emptyList())
        private set
    /** One-off tip after Quick Add (not part of a script). */
    var oneOffHint by mutableStateOf<WhereToLookHint?>(null)
        private set

    val currentStep: TourStep?
        get() = steps.getOrNull(stepIndex)

    val isActive: Boolean
        get() = activeTour != null && currentStep != null || oneOffHint != null

    fun startPersonal(hasActiveMoment: Boolean, force: Boolean = false) {
        if (!force && prefs.isTourPersonalDone()) return
        if (!force && activeTour == TourId.PERSONAL) return
        if (force) {
            prefs.setTourPersonalDone(false)
            prefs.setTourPostQaHintSeen(false)
        }
        oneOffHint = null
        activeTour = TourId.PERSONAL
        steps = TourScripts.personal(hasActiveMoment)
        stepIndex = 0
    }

    fun startGroupMini(force: Boolean = false) {
        if (!force && prefs.isTourGroupMiniDone()) return
        if (activeTour != null && !force) return
        oneOffHint = null
        activeTour = TourId.GROUP_MINI
        steps = TourScripts.groupMini()
        stepIndex = 0
    }

    fun startBusinessMini(force: Boolean = false) {
        if (!force && prefs.isTourBusinessMiniDone()) return
        if (activeTour != null && !force) return
        oneOffHint = null
        activeTour = TourId.BUSINESS_MINI
        steps = TourScripts.businessMini()
        stepIndex = 0
    }

    fun next(onNavigate: (BottomDestination) -> Unit = {}) {
        oneOffHint?.let { hint ->
            onNavigate(hint.goTo)
            dismissOneOff()
            return
        }
        val step = currentStep ?: return
        step.goToDestination?.let(onNavigate)
        if (stepIndex >= steps.lastIndex) {
            complete()
        } else {
            stepIndex += 1
        }
    }

    fun skip() {
        oneOffHint = null
        complete()
    }

    fun complete() {
        when (activeTour) {
            TourId.PERSONAL -> prefs.setTourPersonalDone(true)
            TourId.GROUP_MINI -> prefs.setTourGroupMiniDone(true)
            TourId.BUSINESS_MINI -> prefs.setTourBusinessMiniDone(true)
            null -> Unit
        }
        activeTour = null
        steps = emptyList()
        stepIndex = 0
        oneOffHint = null
    }

    fun onSignal(signal: TourSignal) {
        val step = currentStep ?: return
        if (signal in step.advanceOn) {
            if (stepIndex >= steps.lastIndex) {
                complete()
            } else {
                stepIndex += 1
            }
        }
    }

    fun showWhereToLook(hint: WhereToLookHint) {
        val step = currentStep
        if (step != null && TourSignal.QUICKADD_SAVED in step.advanceOn) {
            onSignal(TourSignal.QUICKADD_SAVED)
        }
        // Skip the generic "where to look" script step — the specific tip replaces it.
        while (currentStep?.id?.contains("where") == true && stepIndex < steps.lastIndex) {
            stepIndex += 1
        }
        if (!prefs.isTourPostQaHintSeen() || activeTour != null) {
            oneOffHint = hint
            prefs.setTourPostQaHintSeen(true)
        }
    }

    fun dismissOneOff() {
        oneOffHint = null
        prefs.setTourPostQaHintSeen(true)
        when {
            currentStep?.id?.contains("where") == true && stepIndex >= steps.lastIndex -> complete()
            currentStep?.id?.contains("where") == true -> stepIndex += 1
            activeTour != null && currentStep == null -> complete()
            // After QA tip during personal tour, continue to moment switcher if present.
            activeTour != null && currentStep != null -> Unit
            activeTour != null -> complete()
        }
    }
}

@Composable
fun rememberTourController(prefs: AppPreferences): TourController =
    remember(prefs) { TourController(prefs) }
