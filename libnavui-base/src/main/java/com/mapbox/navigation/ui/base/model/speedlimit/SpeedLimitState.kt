package com.mapbox.navigation.ui.base.model.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.ui.base.MapboxState
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * A state representing speed limit related data
 */
sealed class SpeedLimitState : MapboxState {

    /**
     * A state for updating the speed limit view
     *
     * @param speedKPH a speed limit value in kilometers per hour
     * @param speedUnit a [SpeedLimitUnit]
     * @param signFormat a [SpeedLimitSign]
     * @param speedLimitFormatter an object that formats the speed limit data
     */
    class UpdateSpeedLimit(
        private val speedKPH: Int,
        private val speedUnit: SpeedLimitUnit,
        private val signFormat: SpeedLimitSign,
        private val speedLimitFormatter: ValueFormatter<UpdateSpeedLimit, String>
    ) : SpeedLimitState() {
        /**
         * @return the speed in kilometers per hour
         */
        fun getSpeed() = speedKPH

        /**
         * @return a [SpeedLimitUnit]
         */
        fun getSpeedUnit() = speedUnit

        /**
         * @return a [SpeedLimitSign]
         */
        fun getSignFormat() = signFormat

        /**
         * @return a speed limit formatter
         */
        fun getSpeedLimitFormatter() = speedLimitFormatter
    }

    /**
     * Indicates the [SpeedLimitState] visibility state
     */
    sealed class Visibility : SpeedLimitState() {
        /**
         * Indicates a visibility state of visible
         */
        class Visible : Visibility()
        /**
         * Indicates a visibility state of hidden
         */
        class Hidden : Visibility()
    }
}
