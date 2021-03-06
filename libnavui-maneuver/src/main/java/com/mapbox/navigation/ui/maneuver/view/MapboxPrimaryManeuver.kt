package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.maneuver.DelimiterComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ManeuverState
import com.mapbox.navigation.ui.base.model.maneuver.PrimaryManeuver
import com.mapbox.navigation.ui.base.model.maneuver.RoadShieldComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.TextComponentNode
import com.mapbox.navigation.ui.maneuver.R

/**
 * Default view to render primary banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxPrimaryManeuver @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : MapboxView<ManeuverState.ManeuverPrimary>, AppCompatTextView(context, attrs, defStyleAttr) {

    private var leftDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_left
    )
    private var rightDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_right
    )
    private var exitBackground = ContextCompat.getDrawable(
        context, R.drawable.mapbox_exit_board_background
    )

    /**
     * Entry point for [MapboxPrimaryManeuver] to render itself based on a [ManeuverState].
     */
    override fun render(state: ManeuverState.ManeuverPrimary) {
        when (state) {
            is ManeuverState.ManeuverPrimary.Instruction -> {
                renderPrimaryManeuver(state.maneuver)
            }
            is ManeuverState.ManeuverPrimary.Show -> {
                updateVisibility(VISIBLE)
            }
            is ManeuverState.ManeuverPrimary.Hide -> {
                updateVisibility(GONE)
            }
        }
    }

    private fun renderPrimaryManeuver(maneuver: PrimaryManeuver) {
        val instruction = generateInstruction(maneuver, lineHeight)
        if (instruction.isNotEmpty()) {
            text = instruction
        }
    }

    private fun updateVisibility(visibility: Int) {
        this.visibility = visibility
    }

    private fun updateMaxLines(maxLines: Int) {
        this.maxLines = maxLines
    }

    private fun generateInstruction(
        maneuver: PrimaryManeuver,
        desiredHeight: Int
    ): SpannableStringBuilder {
        val instructionBuilder = SpannableStringBuilder()
        maneuver.componentList.forEach { component ->
            val node = component.node
            when (node) {
                is TextComponentNode -> {
                    instructionBuilder.append(node.text)
                    instructionBuilder.append(" ")
                }
                is ExitNumberComponentNode -> {
                    instructionBuilder.append(
                        styleAndGetExitView(
                            maneuver.modifier,
                            node,
                            desiredHeight
                        )
                    )
                    instructionBuilder.append(" ")
                }
                is RoadShieldComponentNode -> {
                    instructionBuilder.append(
                        styleAndGetRoadShield(
                            node,
                            desiredHeight
                        )
                    )
                    instructionBuilder.append(" ")
                }
                is DelimiterComponentNode -> {
                    instructionBuilder.append(node.text)
                    instructionBuilder.append(" ")
                }
            }
        }
        return instructionBuilder
    }

    private fun styleAndGetExitView(
        modifier: String?,
        exitNumber: ExitNumberComponentNode,
        desiredHeight: Int
    ): SpannableStringBuilder {
        val text = exitNumber.text
        val exitBuilder = SpannableStringBuilder(text)
        val exitView = MapboxExitText(context, attrs, defStyleAttr)
        exitView.setExitStyle(exitBackground, leftDrawable, rightDrawable)
        exitView.setExit(modifier, exitNumber)
        val exitBitmap = exitView.getViewAsBitmap()
        val drawable = exitView.styleExitWith(exitBitmap, desiredHeight)
        exitBuilder.setSpan(
            ImageSpan(drawable),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return exitBuilder
    }

    private fun styleAndGetRoadShield(
        roadShield: RoadShieldComponentNode,
        desiredHeight: Int
    ): SpannableStringBuilder {
        val icon = roadShield.shieldIcon
        val roadShieldBuilder = SpannableStringBuilder(roadShield.text)
        if (icon != null && icon.isNotEmpty()) {
            val svgBitmap = RoadShieldRenderer.renderRoadShieldAsBitmap(icon, desiredHeight)
            svgBitmap?.let { b ->
                val drawable: Drawable = BitmapDrawable(context.resources, b)
                val right = (desiredHeight * b.width.toDouble() / b.height.toDouble()).toInt()
                drawable.setBounds(0, 0, right, desiredHeight)
                roadShieldBuilder.setSpan(
                    ImageSpan(drawable),
                    0,
                    roadShield.text.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return roadShieldBuilder
    }
}
