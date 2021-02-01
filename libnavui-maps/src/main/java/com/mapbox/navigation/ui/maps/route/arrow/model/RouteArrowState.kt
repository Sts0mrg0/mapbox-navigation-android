package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.MapboxState

/**
 * A state object used for rendering maneuver arrow side effects.
 */
sealed class RouteArrowState : MapboxState {

    /**
     * A state object representing visibility side effects for rendering.
     */
    class UpdateRouteArrowVisibilityState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>
    ) : RouteArrowState() {
        /**
         * @return visibility modifications to be rendered
         */
        fun getVisibilityChanges(): List<Pair<String, Visibility>> = layerVisibilityModifications
    }

    /**
     * A state object representing an update to the maneuver arrow position and/or appearance.
     */
    class UpdateManeuverArrowState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>,
        private val arrowShaftFeature: Feature?,
        private val arrowHeadFeature: Feature?
    ) : RouteArrowState() {
        /**
         * @return visibility modifications to be rendered
         */
        fun getVisibilityChanges(): List<Pair<String, Visibility>> = layerVisibilityModifications

        /**
         * @return a map feature representing the arrow head or null
         */
        fun getArrowHeadFeature(): Feature? = arrowHeadFeature

        /**
         * @return a map feature representing the arrow shaft or null
         */
        fun getArrowShaftFeature(): Feature? = arrowShaftFeature
    }

    /**
     * A state object representing an arrow modification.
     */
    sealed class ArrowModificationState : RouteArrowState() {
        /**
         * A state representing an arrow addition to the map.
         *
         * @param arrowShaftFeatureCollection features for the arrow shafts
         * @param arrowHeadFeatureCollection features for the arrow heads
         */
        class ArrowAddedState(
            private val arrowShaftFeatureCollection: FeatureCollection,
            private val arrowHeadFeatureCollection: FeatureCollection
        ) : ArrowModificationState() {
            /**
             * @return a feature collection representing the arrow shaft(s)
             */
            fun getArrowShaftFeatureCollection() = arrowShaftFeatureCollection

            /**
             * @return a feature collection representing the arrow head(s)
             */
            fun getArrowHeadFeatureCollection() = arrowHeadFeatureCollection
        }
        /**
         * A state representing an arrow removal from the map.
         *
         * @param arrowShaftFeatureCollection features for the arrow shafts
         * @param arrowHeadFeatureCollection features for the arrow heads
         */
        class ArrowRemovedState(
            private val arrowShaftFeatureCollection: FeatureCollection,
            private val arrowHeadFeatureCollection: FeatureCollection
        ) : ArrowModificationState() {
            /**
             * @return a feature collection representing the arrow shaft(s)
             */
            fun getArrowShaftFeatureCollection() = arrowShaftFeatureCollection

            /**
             * @return a feature collection representing the arrow head(s)
             */
            fun getArrowHeadFeatureCollection() = arrowHeadFeatureCollection
        }
        /**
         * A state representing the removal of all arrows from the map.
         *
         * @param arrowShaftFeatureCollection features for the arrow shafts
         * @param arrowHeadFeatureCollection features for the arrow heads
         */
        class ClearArrowsState(
            private val arrowShaftFeatureCollection: FeatureCollection,
            private val arrowHeadFeatureCollection: FeatureCollection
        ) : ArrowModificationState() {
            /**
             * @return a feature collection representing the arrow shaft(s)
             */
            fun getArrowShaftFeatureCollection() = arrowShaftFeatureCollection

            /**
             * @return a feature collection representing the arrow head(s)
             */
            fun getArrowHeadFeatureCollection() = arrowHeadFeatureCollection
        }
        /**
         * A state indicating one or more of the points inputted were invalid for creating an arrow.
         *
         * @param message indicating the error that occurred
         */
        class InvalidPointErrorState(private val message: String) : ArrowModificationState() {
            /**
             * @return a message indicating the nature of the error
             */
            fun getMessage() = message
        }
        /**
         * A state indicating there was already an arrow added with one or more similar points
         */
        class AlreadyPresentErrorState : ArrowModificationState()
    }
}
