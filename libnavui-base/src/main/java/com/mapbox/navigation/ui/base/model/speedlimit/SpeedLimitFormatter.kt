package com.mapbox.navigation.ui.base.model.speedlimit

import android.content.Context
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.ui.base.R
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.base.internal.speedlimit.SpeedLimitViewConstants
import kotlin.math.roundToInt

/**
 * Formats speed limit data to a string for displaying to a user.
 */
class SpeedLimitFormatter(
    context: Context
) : ValueFormatter<SpeedLimitState.UpdateSpeedLimit, String> {

    private val appContext = context.applicationContext

    /**
     * Formats speed limit data.
     *
     * @param state a state containing speed limit related data
     *
     * @return a formatted string
     */
    override fun format(state: SpeedLimitState.UpdateSpeedLimit): String {
        return getSpeedLimit(state.getSignFormat(), state.getSpeedUnit(), state.getSpeed())
    }

    private fun getSpeedLimit(
        sign: SpeedLimitSign,
        unit: SpeedLimitUnit,
        speedLimitKmph: Int
    ): String {
        return if (sign === SpeedLimitSign.VIENNA) {
            if (unit === SpeedLimitUnit.KILOMETRES_PER_HOUR) {
                appContext.getString(R.string.max_speed, speedLimitKmph)
            } else {
                val speed = (
                    5 * (speedLimitKmph * SpeedLimitViewConstants.KILO_MILES_FACTOR / 5)
                        .roundToInt()
                    ).toDouble()
                val formattedSpeed = String.format("%.0f", speed)
                appContext.getString(R.string.max_speed, formattedSpeed.toInt())
            }
        } else {
            if (unit === SpeedLimitUnit.KILOMETRES_PER_HOUR) {
                speedLimitKmph.toString()
            } else {
                val speed = (
                    5 * (speedLimitKmph * SpeedLimitViewConstants.KILO_MILES_FACTOR / 5)
                        .roundToInt()
                    ).toDouble()
                String.format("%.0f", speed)
            }
        }
    }
}
