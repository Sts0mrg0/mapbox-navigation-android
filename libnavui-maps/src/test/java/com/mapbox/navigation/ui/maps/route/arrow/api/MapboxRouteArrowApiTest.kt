package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapboxRouteArrowApiTest {

    @Test
    fun addUpcomingManeuverArrowStateWhenArrowPointsFromRouteProgressAreEmpty() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val routeStepPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = MapboxRouteArrowApi().addUpcomingManeuverArrow(routeProgress)

        assertNull(result.getArrowHeadFeature())
        assertNull(result.getArrowShaftFeature())
        assertEquals(4, result.getVisibilityChanges().size)
    }

    @Test
    fun addUpcomingManeuverArrowStateWhenUpComingPointsFromRouteProgressAreEmpty() {
        val upcomingPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = MapboxRouteArrowApi().addUpcomingManeuverArrow(routeProgress)

        assertNull(result.getArrowHeadFeature())
        assertNull(result.getArrowShaftFeature())
        assertEquals(4, result.getVisibilityChanges().size)
    }

    @Test
    fun hideManeuverArrow() {
        val result = MapboxRouteArrowApi().hideManeuverArrow()

        assertEquals(4, result.getVisibilityChanges().size)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.getVisibilityChanges()[0].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[0].second)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.getVisibilityChanges()[1].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[1].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.getVisibilityChanges()[2].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[2].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            result.getVisibilityChanges()[3].first
        )
        assertEquals(Visibility.NONE, result.getVisibilityChanges()[3].second)
    }

    @Test
    fun showManeuverArrow() {
        val result = MapboxRouteArrowApi().showManeuverArrow()

        assertEquals(4, result.getVisibilityChanges().size)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            result.getVisibilityChanges()[0].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[0].second)
        assertEquals(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            result.getVisibilityChanges()[1].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[1].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            result.getVisibilityChanges()[2].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[2].second)
        assertEquals(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            result.getVisibilityChanges()[3].first
        )
        assertEquals(Visibility.VISIBLE, result.getVisibilityChanges()[3].second)
    }

    @Test
    fun addArrow_doesNotAddDuplicates() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )

        val secondPoints = listOf(
            Point.fromLngLat(-122.528076, 37.969760),
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767)
        )

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstPoints)
        }
        arrows.addArrow(secondPoints)
            as RouteArrowState.ArrowModificationState.AlreadyPresentErrorState
        val redrawState = arrows.redraw()

        assertEquals(1, arrows.getArrows().size)
        assertEquals(1, redrawState.getArrowHeadFeatureCollection().features()!!.size)
        assertEquals(1, redrawState.getArrowShaftFeatureCollection().features()!!.size)
    }

    @Test
    fun addArrow() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )

        val state = MapboxRouteArrowApi().addArrow(firstPoints)
            as RouteArrowState.ArrowModificationState.ArrowAddedState

        assertEquals(1, state.getArrowHeadFeatureCollection().features()!!.size)
        assertEquals(1, state.getArrowShaftFeatureCollection().features()!!.size)
    }

    @Test
    fun removeArrow() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )
        val secondPoints = listOf(
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767)
        )
        val arrowToRemove = listOf(
            Point.fromLngLat(-122.526409, 37.968767),
            Point.fromLngLat(-122.525433, 37.968209,)
        )
        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstPoints)
            it.addArrow(secondPoints)
        }
        assertEquals(2, arrows.getArrows().size)

        val state = arrows.removeArrow(arrowToRemove)

        assertEquals(1, arrows.getArrows().size)
        assertEquals(1, state.getArrowHeadFeatureCollection().features()!!.size)
        assertEquals(1, state.getArrowShaftFeatureCollection().features()!!.size)
    }

    @Test
    fun clearArrows() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )
        val secondPoints = listOf(
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767)
        )
        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstPoints)
            it.addArrow(secondPoints)
        }
        assertEquals(2, arrows.getArrows().size)

        val state = arrows.clearArrows()

        assertEquals(0, arrows.getArrows().size)
        assertEquals(0, state.getArrowHeadFeatureCollection().features()!!.size)
        assertEquals(0, state.getArrowShaftFeatureCollection().features()!!.size)
    }

    @Test
    fun getArrows() {
        val points = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(points)
        }

        assertEquals(1, arrows.getArrows().size)
    }

    @Test
    fun getArrows_includesManeuverArrow() {
        val points = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )
        val route = getDirectionsRoute()
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns mockk {
                every { legIndex } returns 0
                every { currentStepProgress } returns mockk {
                    every { stepPoints } returns PolylineUtils.decode(
                        route.legs()!![0].steps()!![2].geometry()!!,
                        6
                    )
                    every { distanceTraveled } returns 0f
                    every { step } returns mockk {
                        every { distance() } returns route.legs()!![0].steps()!![2].distance()
                    }
                    every { stepIndex } returns 2
                }
            }
            every { upcomingStepPoints } returns PolylineUtils.decode(
                route.legs()!![0].steps()!![2].geometry()!!,
                6
            )
        }

        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(points)
            it.addUpcomingManeuverArrow(routeProgress)
        }

        assertEquals(2, arrows.getArrows().size)
    }

    @Test
    fun redraw() {
        val firstPoints = listOf(
            Point.fromLngLat(-122.528540, 37.971168),
            Point.fromLngLat(-122.528637, 37.970187),
            Point.fromLngLat(-122.528076, 37.969760)
        )
        val secondPoints = listOf(
            Point.fromLngLat(-122.527418, 37.969325),
            Point.fromLngLat(-122.526409, 37.968767)
        )
        val arrows = MapboxRouteArrowApi().also {
            it.addArrow(firstPoints)
            it.addArrow(secondPoints)
        }

        val state = arrows.redraw()

        assertEquals(2, state.getArrowHeadFeatureCollection().features()!!.size)
        assertEquals(2, state.getArrowShaftFeatureCollection().features()!!.size)
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = FileUtils.loadJsonFixture("vanish_point_test.txt")
            .replace("tokenHere", tokenHere)

        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}
