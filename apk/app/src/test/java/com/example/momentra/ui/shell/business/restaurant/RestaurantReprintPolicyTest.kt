package com.example.momentra.ui.shell.business.restaurant

import org.junit.Assert.assertFalse
import org.junit.Test

class RestaurantReprintPolicyTest {
    @Test
    fun restaurantReprint_doesNotPostUpdate() {
        assertFalse(RestaurantReprintPolicy.postsEvent)
    }
}