package com.example.momentra.ui.shell.business.retail

import com.example.momentra.ui.shell.business.mto.MtoToastHost
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.theme.RetailRose

/** Retail quick-add / capture save toast via shared host (same copy as hub paths). */
internal fun showRetailCaptureSavedToast() {
    MtoToastHost.show(
        MtoToastSpec(
            kind = MtoToastKind.Saved,
            title = RetailCaptureCopy.SAVED_TOAST,
            body = RetailCaptureCopy.TAGLINE,
            actionLabel = "Done",
            accent = RetailRose.positive,
        ),
    )
}