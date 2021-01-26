package com.mapbox.navigation.ui.base.model.speedlimit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeedLimitFormatterTest {

    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun format_VIENNA() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            SpeedLimitState.UpdateSpeedLimit(
                35,
                SpeedLimitUnit.KILOMETRES_PER_HOUR,
                SpeedLimitSign.VIENNA,
                formatter
            )
        )

        assertEquals("MAX\n35", result)
    }

    @Test
    fun format_VIENNA_MPH() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            SpeedLimitState.UpdateSpeedLimit(
                35,
                SpeedLimitUnit.MILES_PER_HOUR,
                SpeedLimitSign.VIENNA,
                formatter
            )
        )

        assertEquals("MAX\n20", result)
    }

    @Test
    fun format_MUTCD() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            SpeedLimitState.UpdateSpeedLimit(
                35,
                SpeedLimitUnit.KILOMETRES_PER_HOUR,
                SpeedLimitSign.MUTCD,
                formatter
            )
        )

        assertEquals("35", result)
    }

    @Test
    fun format_MUTCD_MPH() {
        val formatter = SpeedLimitFormatter(ctx)

        val result = formatter.format(
            SpeedLimitState.UpdateSpeedLimit(
                35,
                SpeedLimitUnit.MILES_PER_HOUR,
                SpeedLimitSign.MUTCD,
                formatter
            )
        )

        assertEquals("20", result)
    }
}
