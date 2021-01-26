package com.mapbox.navigation.ui.base.api.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.ui.base.model.speedlimit.SpeedLimitState

/**
 * An interface for implementing the speed limit API
 */
interface SpeedLimitApi {

    /**
     * Calculates a [SpeedLimitState.UpdateSpeedLimit] based on the provided [SpeedLimit].
     */
    fun updateSpeedLimit(speedLimit: SpeedLimit): SpeedLimitState.UpdateSpeedLimit

    /**
     * Hides the view component.
     */
    fun hide(): SpeedLimitState.Visibility.Hidden

    /**
     * Shows the view component.
     */
    fun show(): SpeedLimitState.Visibility.Visible
}
