package com.mapbox.navigation.ui.speedlimit.view

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedLimitUnit
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.model.speedlimit.SpeedLimitFormatter
import com.mapbox.navigation.ui.base.model.speedlimit.SpeedLimitState
import com.mapbox.navigation.ui.speedlimit.R
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(RobolectricTestRunner::class)
class MapboxSpeedLimitViewTest {

    lateinit var ctx: Context

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.IODispatcher } returns coroutineRule.testDispatcher
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkObject(ThreadController)
    }

    @Test
    fun renderUpdateSpeedLimit_whenMPH_setsSpeedLimitText() = coroutineRule.runBlockingTest {
        val state = SpeedLimitState.UpdateSpeedLimit(
            35,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(state)
        }

        assertEquals("20", view.findViewById<TextView>(R.id.mapboxSpeedLimitText).text.toString())
    }

    @Test
    fun renderUpdateSpeedLimit_slow_MUTCD() = coroutineRule.runBlockingTest {
        val state = SpeedLimitState.UpdateSpeedLimit(
            5,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(state)
        }

        assertEquals("5", view.findViewById<TextView>(R.id.mapboxSpeedLimitText).text.toString())
    }

    @Test
    fun renderUpdateSpeedLimit_whenKPH_setsSpeedLimitText() = coroutineRule.runBlockingTest {
        val state = SpeedLimitState.UpdateSpeedLimit(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.VIENNA,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(state)
        }

        assertEquals(
            "MAX\n35",
            view.findViewById<TextView>(R.id.mapboxSpeedLimitText).text.toString()
        )
    }

    @Test
    fun renderUpdateSpeedLimit_whenMUTCD_setsBackground() = coroutineRule.runBlockingTest {
        val state = SpeedLimitState.UpdateSpeedLimit(
            35,
            SpeedLimitUnit.MILES_PER_HOUR,
            SpeedLimitSign.MUTCD,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(state)
        }

        assertNotNull(view.findViewById<ImageView>(R.id.mapboxSpeedLimitBackground).drawable)
    }

    @Test
    fun renderUpdateSpeedLimit_whenVienna_setsBackground() = coroutineRule.runBlockingTest {
        val state = SpeedLimitState.UpdateSpeedLimit(
            35,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitSign.VIENNA,
            SpeedLimitFormatter(ctx)
        )

        val view = MapboxSpeedLimitView(ctx).also {
            it.render(state)
        }

        assertNotNull(view.findViewById<ImageView>(R.id.mapboxSpeedLimitBackground).drawable)
    }

    @Test
    fun getFinalDrawable_when_MUTCD_hasCorrectNumChildren() {
        val view = MapboxSpeedLimitView(ctx)

        val drawable = view.getFinalDrawable(SpeedLimitSign.MUTCD)

        assertEquals(3, drawable.numberOfLayers)
    }

    @Test
    fun getFinalDrawable_when_VIENNA_hasCorrectNumChildren() {
        val view = MapboxSpeedLimitView(ctx)

        val drawable = view.getFinalDrawable(SpeedLimitSign.VIENNA)

        assertEquals(3, drawable.numberOfLayers)
    }

    @Test
    fun getSizeSpanStartIndex_MUTCD() {
        val view = MapboxSpeedLimitView(ctx)

        val result = view.getSizeSpanStartIndex(SpeedLimitSign.MUTCD, "whatever")

        assertEquals(0, result)
    }

    @Test
    fun getSizeSpanStartIndex_VIENNA() {
        val view = MapboxSpeedLimitView(ctx)

        val result = view.getSizeSpanStartIndex(SpeedLimitSign.VIENNA, "MAX\n35")

        assertEquals(4, result)
    }

    @Test
    fun renderHiddenState() {
        val view = MapboxSpeedLimitView(ctx)
        assertEquals(View.VISIBLE, view.visibility)

        view.render(SpeedLimitState.Visibility.Hidden())

        assertEquals(View.INVISIBLE, view.visibility)
    }

    @Test
    fun renderVisibleState() {
        val view = MapboxSpeedLimitView(ctx)
        view.visibility = View.INVISIBLE
        assertEquals(View.INVISIBLE, view.visibility)

        view.render(SpeedLimitState.Visibility.Visible())

        assertEquals(View.VISIBLE, view.visibility)
    }
}
