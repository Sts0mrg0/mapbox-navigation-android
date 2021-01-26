package com.mapbox.navigation.ui.speedlimit.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.speedlimit.SpeedLimitState
import com.mapbox.navigation.ui.speedlimit.R
import com.mapbox.navigation.ui.speedlimit.databinding.MapboxSpeedlimitLayoutBinding
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * A view component intended to consume data produced by the [MapboxSpeedLimitApi].
 */
class MapboxSpeedLimitView : FrameLayout, MapboxView<SpeedLimitState> {

    private var speedLimitBackgroundColor: Int = 0
    private var speedLimitViennaBorderColor: Int = 0
    private var speedLimitMutcdBorderColor: Int = 0
    private val binding: MapboxSpeedlimitLayoutBinding =
        MapboxSpeedlimitLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    companion object {
        private val RADIUS = 10f
        private const val VIENNA_MUTCD_BORDER_INSET = 4
        private const val MUTCD_INNER_BACKGROUND_INSET = 7
        private const val VIENNA_INNER_BACKGROUND_INSET = 16
        private const val VIENNA_MUTCD_OUTER_BACKGROUND_INSET = 0
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxSpeedLimitView
        )

        ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxSpeedLimitView_speedLimitTextColor,
                R.color.mapbox_speed_limit_text_color
            )
        ).let {
            binding.mapboxSpeedLimitText.setTextColor(it)
        }

        speedLimitBackgroundColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxSpeedLimitView_speedLimitBackgroundColor,
                R.color.mapbox_speed_limit_view_background
            )
        )

        speedLimitViennaBorderColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxSpeedLimitView_speedLimitViennaBorderColor,
                R.color.mapbox_speed_limit_view_vienna_border
            )
        )

        speedLimitMutcdBorderColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxSpeedLimitView_speedLimitMutcdBorderColor,
                R.color.mapbox_speed_limit_view_mutcd_border
            )
        )

        typedArray.recycle()
    }

    /**
     * Updates this view with speed limit related data.
     *
     * @param state a [SpeedLimitState]
     */
    override fun render(state: SpeedLimitState) {
        when (state) {
            is SpeedLimitState.UpdateSpeedLimit -> renderState(state)
            is SpeedLimitState.Visibility -> renderState(state)
        }
    }

    private fun renderState(state: SpeedLimitState.UpdateSpeedLimit) {
        ThreadController.getMainScopeAndRootJob().scope.launch {
            val layerDrawable = getFinalDrawable(state.getSignFormat())
            val speedLimitSpan = getSpeedLimitSpannable(state)

            binding.mapboxSpeedLimitBackground.setImageDrawable(layerDrawable)
            binding.mapboxSpeedLimitText.setText(speedLimitSpan, TextView.BufferType.SPANNABLE)
        }
    }

    private fun renderState(state: SpeedLimitState.Visibility) {
        when (state) {
            is SpeedLimitState.Visibility.Visible -> this.visibility = View.VISIBLE
            is SpeedLimitState.Visibility.Hidden -> this.visibility = View.INVISIBLE
        }
    }

    private fun getSpeedLimitSpannable(
        state: SpeedLimitState.UpdateSpeedLimit
    ): SpannableStringBuilder {
        val formattedSpeedLimit = state.getSpeedLimitFormatter().format(state)
        val sizeSpanStartIndex = getSizeSpanStartIndex(state.getSignFormat(), formattedSpeedLimit)

        return SpannableStringBuilder(formattedSpeedLimit).also {
            it.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                formattedSpeedLimit.length, SPAN_EXCLUSIVE_EXCLUSIVE
            )
            it.setSpan(
                RelativeSizeSpan(1.9f),
                sizeSpanStartIndex,
                formattedSpeedLimit.length,
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    internal fun getSizeSpanStartIndex(signFormat: SpeedLimitSign, formattedString: String): Int {
        return when (signFormat) {
            SpeedLimitSign.MUTCD -> 0
            SpeedLimitSign.VIENNA -> formattedString.indexOf("\n") + 1
        }
    }

    internal fun getFinalDrawable(signFormat: SpeedLimitSign): LayerDrawable {
        val drawableShape = getDrawableShape(signFormat)
        val backgroundDrawable = backgroundDrawable(drawableShape, speedLimitBackgroundColor)
        val borderDrawable = borderDrawable(
            drawableShape,
            speedLimitMutcdBorderColor,
            speedLimitViennaBorderColor
        )
        val outerBackgroundInset = getOuterBackgroundInset(signFormat)
        val borderInset = getBorderInset(signFormat)
        val innerBackgroundInset = getInnerBackgroundInset(signFormat)
        return LayerDrawable(
            arrayOf(
                backgroundDrawable,
                borderDrawable,
                backgroundDrawable
            )
        ).also {
            it.setLayerInset(
                0,
                outerBackgroundInset,
                outerBackgroundInset,
                outerBackgroundInset,
                outerBackgroundInset
            )
            it.setLayerInset(
                1,
                borderInset,
                borderInset,
                borderInset,
                borderInset
            )
            it.setLayerInset(
                2,
                innerBackgroundInset,
                innerBackgroundInset,
                innerBackgroundInset,
                innerBackgroundInset
            )
        }
    }

    private fun getOuterBackgroundInset(signFormat: SpeedLimitSign): Int {
        return when (signFormat) {
            SpeedLimitSign.VIENNA -> VIENNA_MUTCD_OUTER_BACKGROUND_INSET
            SpeedLimitSign.MUTCD -> VIENNA_MUTCD_OUTER_BACKGROUND_INSET
        }
    }

    private fun getBorderInset(signFormat: SpeedLimitSign): Int {
        return when (signFormat) {
            SpeedLimitSign.VIENNA -> VIENNA_MUTCD_BORDER_INSET
            SpeedLimitSign.MUTCD -> VIENNA_MUTCD_BORDER_INSET
        }
    }

    private fun getInnerBackgroundInset(signFormat: SpeedLimitSign): Int {
        return when (signFormat) {
            SpeedLimitSign.VIENNA -> MUTCD_INNER_BACKGROUND_INSET
            SpeedLimitSign.MUTCD -> VIENNA_INNER_BACKGROUND_INSET
        }
    }

    private fun getDrawableShape(signFormat: SpeedLimitSign): Int {
        return when (signFormat) {
            SpeedLimitSign.VIENNA -> GradientDrawable.RECTANGLE
            SpeedLimitSign.MUTCD -> GradientDrawable.OVAL
        }
    }

    private fun backgroundDrawable(shape: Int, speedLimitBackgroundColor: Int): GradientDrawable {
        val background = GradientDrawable()
        background.setColor(speedLimitBackgroundColor)
        background.shape = shape
        if (shape == GradientDrawable.RECTANGLE) {
            background.cornerRadius = RADIUS
        }
        return background
    }

    private fun borderDrawable(
        shape: Int,
        speedLimitMutcdBorderColor: Int,
        speedLimitViennaBorderColor: Int
    ): GradientDrawable {
        val border = GradientDrawable()
        border.shape = shape
        if (shape == GradientDrawable.RECTANGLE) {
            border.cornerRadius = RADIUS
            border.setColor(speedLimitMutcdBorderColor)
        } else {
            border.setColor(speedLimitViennaBorderColor)
        }
        return border
    }
}
