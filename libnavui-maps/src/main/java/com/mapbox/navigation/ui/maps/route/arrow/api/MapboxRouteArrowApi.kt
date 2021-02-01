package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants.ARROW_BEARING
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants.MAX_DEGREES
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Responsible for displaying a maneuver arrow representing the next maneuver.
 * The maneuver arrow is calculated based on the route progress and the data returned should
 * be rendered on the map using the [MapRouteArrowView] class. Generally this class should be called
 * on each route progress update in order to ensure the arrow displayed is kept consistent
 * with the state of navigation.
 *
 * The two principal classes for the maneuver arrow are the [MapboxRouteArrowApi] and the
 * [MapboxRouteArrowView].
 *
 * Like the route line components the [MapboxRouteArrowApi] consumes data from the Navigation SDK,
 * specifically the [RouteProgress], and produces data for rendering on the map by the
 * [MapboxRouteArrowView]. While the route line has no dependency on the [MapboxRouteArrowApi],
 * the [MapboxRouteArrowApi] indirectly depends on the [MapboxRouteLineApi]. So the
 * [MapboxRouteLineApi] can be used without the [MapboxRouteArrowApi] the inverse is not currently
 * supported. Simple usage of the maneuver arrows would look like:
 *
 * ```java
 * RouteArrowOptions routeArrowOptions = new RouteArrowOptions.Builder(context)
 * MapboxRouteArrowApi routeArrow = new MapboxRouteArrowApi()
 * MapboxRouteArrowView routeArrowView = new MapboxRouteArrowView(routeArrowOptions)
 * ```
 *
 * or
 *
 * ```kotlin
 * val routeArrowOptions = RouteArrowOptions.Builder(context)
 * val routeArrow = MapboxRouteArrowApi()
 * val routeArrowView = MapboxRouteArrowView(routeArrowOptions)
 * ```
 *
 * In order for the [MapboxRouteArrowApi] to function it needs route progress updates.
 * An application should register a [RouteProgressObserver] with the [MapboxNavigation] class
 * instance and pass the route progress updates to the [MapboxRouteArrowApi] class. Be sure to
 * unregister this listener appropriately according to the lifecycle of your activity or Fragment
 * in order to prevent resource leaks.
 *
 * At a minimum an application should do the following with route progress updates:
 *
 * ```kotlin
 * override fun onRouteProgressChanged(routeProgress: RouteProgress) {
 * val updateState = routeArrow.updateUpcomingManeuverArrow(routeProgress)
 * routeArrowView.render(mapboxMap.getStyle(), updateState)
 * }
 * ```
 *
 */

class MapboxRouteArrowApi {
    private var maneuverPoints = listOf<Point>()
    private val arrows: CopyOnWriteArrayList<List<Point>> = CopyOnWriteArrayList()

    /**
     * @return all of the collections of points making up arrows that have been added. Each
     * collection of points represents a single arrow.
     */
    fun getArrows(): List<List<Point>> {
        return arrows.toList()
    }

    /**
     * Returns a state containing visibility modifications for hiding the maneuver arrow.
     *
     * @return the [UpdateRouteArrowVisibilityState] for rendering by the view.
     */
    fun hideManeuverArrow(): RouteArrowState.UpdateRouteArrowVisibilityState {
        return RouteArrowState.UpdateRouteArrowVisibilityState(getHideArrowModifications())
    }

    /**
     * Returns a state containing visibility modifications for showing the maneuver arrow.
     *
     * @return the UpdateRouteArrowVisibilityState for rendering by the view.
     */
    fun showManeuverArrow(): RouteArrowState.UpdateRouteArrowVisibilityState {
        return RouteArrowState.UpdateRouteArrowVisibilityState(getShowArrowModifications())
    }

    /**
     * Returns the data necessary to re-render or redraw the maneuver arrow based on the last
     * route progress received.
     *
     * @return the [ArrowAddedState] for rendering by the view.
     */
    fun redraw(): RouteArrowState.ArrowModificationState.ArrowAddedState {
        val shaftFeatureCollection = getShaftFeatureCollection()
        val arrowHeadFeatureCollection = getArrowHeadFeatureCollection()
        return RouteArrowState.ArrowModificationState.ArrowAddedState(
            shaftFeatureCollection,
            arrowHeadFeatureCollection
        )
    }

    /**
     * Calculates a maneuver arrow based on the route progress and returns a state that can
     * be used to render the arrow on the map.
     *
     * @param routeProgress a route progress generated by the core navigation system.
     *
     * @return the [UpdateManeuverArrowState] for rendering by the view.
     */
    fun addUpcomingManeuverArrow(routeProgress: RouteProgress):
        RouteArrowState.UpdateManeuverArrowState {
            val invalidUpcomingStepPoints = (
                routeProgress.upcomingStepPoints == null ||
                    routeProgress.upcomingStepPoints!!.size < RouteConstants.TWO_POINTS
                )
            val invalidCurrentStepPoints = routeProgress.currentLegProgress == null ||
                routeProgress.currentLegProgress!!.currentStepProgress == null ||
                routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints == null ||
                routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints!!.size <
                RouteConstants.TWO_POINTS

            val visibilityChanges = if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
                getHideArrowModifications()
            } else {
                getShowArrowModifications()
            }

            removeArrow(maneuverPoints)
            maneuverPoints = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)
            val arrowFeatureCollections: Pair<FeatureCollection?, FeatureCollection?> =
                addArrow(maneuverPoints).run {
                    when (this) {
                        is RouteArrowState.ArrowModificationState.InvalidPointErrorState -> {
                            Pair(null, null)
                        }
                        is RouteArrowState.ArrowModificationState.AlreadyPresentErrorState -> {
                            Pair(null, null)
                        }
                        is RouteArrowState.ArrowModificationState.ArrowAddedState -> {
                            Pair(
                                this.getArrowShaftFeatureCollection(),
                                this.getArrowHeadFeatureCollection()
                            )
                        }
                        else -> {
                            Pair(null, null)
                        }
                    }
                }
            val shaftFeature = arrowFeatureCollections.first?.features()?.firstOrNull()
            val headFeature = arrowFeatureCollections.second?.features()?.firstOrNull()

            return RouteArrowState.UpdateManeuverArrowState(
                visibilityChanges,
                shaftFeature,
                headFeature
            )
        }

    /**
     * Adds an arrow to the map made up of the points submitted. An arrow is made up of at least
     * two points. The direction of the arrow head is determined by calculating the bearing
     * between the last two points submitted. Each call will add a new arrow.
     *
     * @param points the points that make up the arrow to be drawn
     *
     * @return a [ArrowModificationState]
     */
    fun addArrow(points: List<Point>): RouteArrowState.ArrowModificationState {
        if (points.size < RouteConstants.TWO_POINTS) {
            return RouteArrowState.ArrowModificationState.InvalidPointErrorState(
                "An arrow must have at least 2 points."
            )
        }

        return if (arrows.flatten().intersect(points).isEmpty()) {
            arrows.add(points)
            return RouteArrowState.ArrowModificationState.ArrowAddedState(
                getShaftFeatureCollection(),
                getArrowHeadFeatureCollection()
            )
        } else {
            RouteArrowState.ArrowModificationState.AlreadyPresentErrorState()
        }
    }

    /**
     * Will remove any arrow having one or more points contained in the points submitted.
     * To remove a previously added arrow it isn't necessary to submit all of the points of the
     * previously submitted arrow(s). Instead it is necessary only to submit at least one point
     * for each previously added arrow that should be removed.
     *
     * @param points one or more points used as criteria for removing arrows from the map
     *
     * @return a [ArrowRemovedState]
     */
    fun removeArrow(points: List<Point>): RouteArrowState.ArrowModificationState.ArrowRemovedState {
        val arrowsToRemove = arrows.filter { it.intersect(points).isNotEmpty() }
        if (maneuverPoints.intersect(points).isNotEmpty()) {
            maneuverPoints = listOf()
        }
        arrows.removeAll(arrowsToRemove)

        return RouteArrowState.ArrowModificationState.ArrowRemovedState(
            getShaftFeatureCollection(),
            getArrowHeadFeatureCollection()
        )
    }

    /**
     * Clears all arrows from the map.
     *
     * @return a [ClearArrowsState]
     */
    fun clearArrows(): RouteArrowState.ArrowModificationState.ClearArrowsState {
        arrows.clear()
        maneuverPoints = listOf()
        return RouteArrowState.ArrowModificationState.ClearArrowsState(
            getShaftFeatureCollection(),
            getArrowHeadFeatureCollection()
        )
    }

    private fun getShaftFeatureCollection(): FeatureCollection {
        val shaftFeatures = arrows.map {
            LineString.fromLngLats(it)
        }.map {
            Feature.fromGeometry(it)
        }
        return FeatureCollection.fromFeatures(shaftFeatures)
    }

    private fun getArrowHeadFeatureCollection(): FeatureCollection {
        val arrowHeadFeatures = arrows.map {
            val azimuth = TurfMeasurement.bearing(it[it.size - 2], it[it.size - 1])
            Feature.fromGeometry(it[it.size - 1]).also { feature ->
                feature.addNumberProperty(
                    ARROW_BEARING,
                    wrap(azimuth, 0.0, MAX_DEGREES)
                )
            }
        }
        return FeatureCollection.fromFeatures(arrowHeadFeatures)
    }

    private fun getHideArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.NONE),
            Pair(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.NONE),
            Pair(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID, Visibility.NONE),
            Pair(RouteLayerConstants.ARROW_HEAD_LAYER_ID, Visibility.NONE)
        )
    }

    private fun getShowArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteLayerConstants.ARROW_HEAD_LAYER_ID, Visibility.VISIBLE)
        )
    }

    // This came from MathUtils in the Maps SDK which may have been removed.
    private fun wrap(value: Double, min: Double, max: Double): Double {
        val delta = max - min
        val firstMod = (value - min) % delta
        val secondMod = (firstMod + delta) % delta
        return secondMod + min
    }
}
