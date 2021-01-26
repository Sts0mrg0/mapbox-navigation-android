package com.mapbox.navigation.ui.speedlimit.api

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.ui.base.api.speedlimit.SpeedLimitApi
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.base.model.speedlimit.SpeedLimitState
import com.mapbox.navigation.ui.speedlimit.SpeedLimitAction
import com.mapbox.navigation.ui.speedlimit.SpeedLimitProcessor
import com.mapbox.navigation.ui.speedlimit.SpeedLimitResult

/**
 * A Mapbox implementation of the [SpeedLimitApi] for formatting speed limit view related data.
 *
 * @param formatter formats the speed limit data into a string for displaying in the UI
 * @param processor an instance of a [SpeedLimitProcessor]
 */
class MapboxSpeedLimitApi internal constructor(
    var formatter: ValueFormatter<SpeedLimitState.UpdateSpeedLimit, String>,
    private val processor: SpeedLimitProcessor
) : SpeedLimitApi {

    /**
     * Returns the current visibility state of the API
     */
    var visibility: SpeedLimitState.Visibility = SpeedLimitState.Visibility.Visible()
        private set

    /**
     * @param formatter formats the speed limit data into a string for displaying in the UI
     */
    constructor(formatter: ValueFormatter<SpeedLimitState.UpdateSpeedLimit, String>) : this(
        formatter,
        SpeedLimitProcessor()
    )

    /**
     * Evaluates the [SpeedLimit] data into a state that can be rendered by the view.
     *
     * @param speedLimit a [speedLimit] instance
     * @return an updated state for rendering in the view
     */
    override fun updateSpeedLimit(speedLimit: SpeedLimit): SpeedLimitState.UpdateSpeedLimit {
        val action = SpeedLimitAction.CalculateSpeedLimitUpdate(speedLimit)
        val result = processor.process(action) as SpeedLimitResult.SpeedLimitCalculation

        return SpeedLimitState.UpdateSpeedLimit(
            result.speedKPH,
            result.speedUnit,
            result.signFormat,
            formatter
        )
    }

    /**
     * Indicates the component should be in a hidden state.
     *
     * @return a state for rendering in the view
     */
    override fun hide(): SpeedLimitState.Visibility.Hidden {
        visibility = SpeedLimitState.Visibility.Hidden()
        return visibility as SpeedLimitState.Visibility.Hidden
    }

    /**
     * Indicates the component should be in a visible state.
     *
     * @return a state for rendering in the view
     */
    override fun show(): SpeedLimitState.Visibility.Visible {
        visibility = SpeedLimitState.Visibility.Visible()
        return visibility as SpeedLimitState.Visibility.Visible
    }
}
