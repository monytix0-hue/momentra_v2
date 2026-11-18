package com.example.momentra.ui.shell.business.shared

import androidx.compose.ui.graphics.Color
import com.example.momentra.ui.shell.business.mto.MtoToastHost
import com.example.momentra.ui.shell.business.mto.MtoToastKind
import com.example.momentra.ui.shell.business.mto.MtoToastSpec
import com.example.momentra.ui.shell.business.restaurant.RestaurantCaptureCopy
import com.example.momentra.ui.shell.business.restaurant.showRestaurantCaptureSavedToast
import com.example.momentra.ui.shell.business.retail.RetailCaptureCopy
import com.example.momentra.ui.shell.business.retail.showRetailCaptureSavedToast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    fun retailSavedToast_usesSharedHostViaMtoToastHost() {
        OperatingToastHost.dismiss()
        showRetailCaptureSavedToast()
        assertEquals(RetailCaptureCopy.SAVED_TOAST, MtoToastHost.current?.title)
        assertSame(MtoToastHost.current, OperatingToastHost.current)
        assertNotNull(OperatingToastHost.current)
        OperatingToastHost.dismiss()
    }

    @Test
    fun restaurantCaptureSavedToast_delegatesThroughMtoToastHost() {
        OperatingToastHost.dismiss()
        showRestaurantCaptureSavedToast()
        assertEquals(RestaurantCaptureCopy.SAVED_TOAST, MtoToastHost.current?.title)
        assertSame(MtoToastHost.current, OperatingToastHost.current)
        OperatingToastHost.dismiss()
    }
}