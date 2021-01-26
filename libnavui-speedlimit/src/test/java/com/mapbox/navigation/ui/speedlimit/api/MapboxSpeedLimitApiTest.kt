package com.mapbox.navigation.ui.speedlimit.api

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.ui.base.api.speedlimit.SpeedLimitApi
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.base.model.speedlimit.SpeedLimitState
import com.mapbox.navigation.ui.speedlimit.SpeedLimitProcessor
import com.mapbox.navigation.ui.speedlimit.SpeedLimitResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxSpeedLimitApiTest {

    @Test
    fun updateSpeedLimit() {
        val processorResult = SpeedLimitResult.SpeedLimitCalculation(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )
        val formatter = mockk<ValueFormatter<SpeedLimitState.UpdateSpeedLimit, String>>()
        val processor = mockk<SpeedLimitProcessor> {
            every { process(any()) } returns processorResult
        }
        val api: SpeedLimitApi = MapboxSpeedLimitApi(formatter, processor)
        val speedLimit = SpeedLimit(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.MUTCD
        )

        val result = api.updateSpeedLimit(speedLimit)

        assertEquals(35, result.getSpeed())
        assertEquals(SpeedLimitSign.MUTCD, result.getSignFormat())
        assertEquals(SpeedLimitUnit.KILOMETRES_PER_HOUR, result.getSpeedUnit())
        assertEquals(formatter, result.getSpeedLimitFormatter())
    }
}
