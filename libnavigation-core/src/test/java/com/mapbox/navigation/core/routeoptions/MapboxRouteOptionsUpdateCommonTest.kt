package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.models.RouteOptions
import org.junit.Assert

object MapboxRouteOptionsUpdateCommonTest {

    fun checkUnmutableFields(routeOptions: RouteOptions, updated: RouteOptions) {
        Assert.assertEquals("Check Overview", routeOptions.overview(), updated.overview())
        Assert.assertEquals("Check Annotations", routeOptions.annotations(), updated.annotations())
        Assert.assertEquals("Check Uuid", routeOptions.requestUuid(), updated.requestUuid())
        Assert.assertEquals("Check Profile", routeOptions.profile(), updated.profile())
        Assert.assertEquals("Check Token", routeOptions.accessToken(), updated.accessToken())
        Assert.assertEquals(
            "Check Alternatives",
            routeOptions.alternatives(),
            updated.alternatives()
        )
        Assert.assertEquals("Check Steps", routeOptions.steps(), updated.steps())
        Assert.assertEquals(
            "Check BannerInstructions",
            routeOptions.bannerInstructions(),
            updated.bannerInstructions()
        )
        Assert.assertEquals("Check Base URL", routeOptions.baseUrl(), updated.baseUrl())
        Assert.assertEquals(
            "Check Continue Straight",
            routeOptions.continueStraight(),
            updated.continueStraight()
        )
        Assert.assertEquals("Check Exclude", routeOptions.exclude(), updated.exclude())
        Assert.assertEquals("Check Language", routeOptions.language(), updated.language())
        Assert.assertEquals("Check Geometries", routeOptions.geometries(), updated.geometries())
        Assert.assertEquals("Check User", routeOptions.user(), updated.user())
        Assert.assertEquals(
            "Check Roundabout Exits",
            routeOptions.roundaboutExits(),
            updated.roundaboutExits()
        )
        Assert.assertEquals(
            "Check Walking Options", routeOptions.walkingOptions(), updated.walkingOptions()
        )
        Assert.assertEquals(
            "Check Voice Instructions",
            routeOptions.voiceInstructions(),
            updated.voiceInstructions()
        )
    }
}
