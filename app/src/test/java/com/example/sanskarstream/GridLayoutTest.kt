package com.example.sanskarstream

import com.example.sanskarstream.domain.GridLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class GridLayoutTest {
    @Test
    fun allLayout_16Streams_is4x4LikeWebsite() {
        assertEquals(4 to 4, GridLayout.All.dims(16))
    }

    @Test
    fun allLayout_autoGrid() {
        assertEquals(1 to 1, GridLayout.All.dims(0))
        assertEquals(1 to 1, GridLayout.All.dims(1))
        assertEquals(2 to 1, GridLayout.All.dims(2))
        assertEquals(3 to 2, GridLayout.All.dims(5))
        assertEquals(3 to 3, GridLayout.All.dims(8))
    }

    @Test
    fun presets() {
        assertEquals(2 to 2, GridLayout.Four.dims(20))
        assertEquals(4 to 2, GridLayout.Eight.dims(20))
        assertEquals(6 to 6, GridLayout.Custom(9, 9).dims(20))
    }

    @Test
    fun fromKey_roundTrip() {
        assertEquals(GridLayout.Eight, GridLayout.fromKey("8", 3, 3))
        assertEquals(GridLayout.Custom(3, 2), GridLayout.fromKey("custom", 3, 2))
        assertEquals(GridLayout.All, GridLayout.fromKey(null, 3, 3))
    }
}
