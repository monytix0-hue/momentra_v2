package com.example.momentra.ui.shell.business.shared

import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.shell.business.retail.RetailCaptureCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OperatingToastHostTest {
    @Test
    fun show_setsCurrentSpec() {
        OperatingToastHost.dismiss()
        OperatingToastHost.show(
            MtoToastSpec(
                kind = MtoToastKind.Saved,
                title = "✓ Saved",
                body = "test",
                actionLabel = "Done",
                accent = Color(0xFF10B981),
            ),
        )
        assertEquals("✓ Saved", OperatingToastHost.current?.title)
        OperatingToastHost.dismiss()
        assertNull(OperatingToastHost.current)
    }

    @Test
    fun retailSavedToast_usesSharedHost() {
        OperatingToastHost.dismiss()
        OperatingToastHost.show(
            MtoToastSpec(
                kind = MtoToastKind.Saved,
                title = RetailCaptureCopy.SAVED_TOAST,
                body = "x",
                actionLabel = "Done",
                accent = Color(0xFF10B981),
            ),
        )
        assertNotNull(OperatingToastHost.current)
    }
}