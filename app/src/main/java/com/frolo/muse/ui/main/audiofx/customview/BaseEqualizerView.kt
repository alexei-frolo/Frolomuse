package com.frolo.muse.ui.main.audiofx.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import androidx.core.view.forEach
import com.frolo.muse.R
import com.frolo.muse.engine.AudioFx
import com.frolo.muse.engine.AudioFxObserver
import com.frolo.muse.engine.SimpleAudioFxObserver
import java.util.*


@Suppress("UNCHECKED_CAST")
abstract class BaseEqualizerView<V> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.equalizerViewStyle,
    defStyleRes: Int = DEFAULT_STYLE_RES_ID
): FrameLayout(context, attrs, defStyleAttr) where V: View, V: BaseEqualizerView.BandView {

    /**
     * Special handler for setting the band level with some delay.
     * [Message.what] is the band index.
     * [Message.arg1] is the level value.
     */
    private inner class BandLevelHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            val bandIndex = msg.what.toShort()
            val bandLevel = msg.arg1.toShort()
            audioFx?.also { safeAudioFx ->
                val numberOfBands = safeAudioFx.numberOfBands.toInt()
                if (bandIndex in 0 until numberOfBands) {
                    safeAudioFx.setBandLevel(bandIndex, bandLevel)
                }
            }
        }
    }

    private val audioFxObserver: AudioFxObserver = object : SimpleAudioFxObserver() {
        override fun onBandLevelChanged(audioFx: AudioFx, band: Short, level: Short) {
            val container = getBandViewContainer()
            if (band >= 0 && band < container.childCount) {
                val bandView = container.getChildAt(band.toInt()) as V
                bandView.setLevel(level.toInt(), true)
            }
        }
    }

    private var audioFx: AudioFx? = null

    private val childContext: Context
    private val bandsContainer: LinearLayout by lazy {
        LinearLayout(context).also { layout ->
            layout.orientation = LinearLayout.HORIZONTAL
            addView(layout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    private val bandLevelHandler: BandLevelHandler

    private val drawVisuals: Boolean

    var gridLineThickness = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    @ColorInt
    var gridColor: Int = DEFAULT_GRID_COLOR
        set(value) {
            if (field != value) {
                field = value
                getBandViewContainer().forEach { child ->
                    (child as BandView).setTrackTint(value)
                }
                invalidate()
            }
        }

    @ColorInt
    var levelColor: Int = DEFAULT_LEVEL_COLOR
        set(value) {
            if (field != value) {
                field = value
                getBandViewContainer().forEach { child ->
                    (child as BandView).setThumbTint(value)
                }
                invalidate()
            }
        }

    // Background visual tools
    private val visualPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val visualNeutralPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val visualPaths: List<VisualPath>

    /**
     * Returns the current levels set by user.
     * @return current levels
     */
    val currentLevels: ShortArray
        get() {
            val container = getBandViewContainer()
            val numberOfBands = container.childCount
            val levels = ShortArray(numberOfBands)
            for (i in 0 until numberOfBands) {
                val bandView = container.getChildAt(i) as BandView
                levels[i] = bandView.actualLevel.toShort()
            }
            return levels
        }

    init {
        val styleId = attrs?.getAttributeIntValue(null, "style", DEFAULT_STYLE_RES_ID)
                ?: DEFAULT_STYLE_RES_ID
        childContext = ContextThemeWrapper(context, styleId)

        val a = context.theme
                .obtainStyledAttributes(attrs, R.styleable.BaseEqualizerView, defStyleAttr, defStyleRes)
        try {
            drawVisuals = a.getBoolean(R.styleable.BaseEqualizerView_drawVisuals, false)
            gridLineThickness = a.getDimension(R.styleable.BaseEqualizerView_gridLineThickness, 0f)
            gridColor = a.getColor(R.styleable.BaseEqualizerView_gridColor, DEFAULT_GRID_COLOR)
            levelColor = a.getColor(R.styleable.BaseEqualizerView_levelColor, DEFAULT_LEVEL_COLOR)
        } finally {
            a.recycle()
        }

        bandLevelHandler = BandLevelHandler(context.mainLooper)

        visualPaint.style = Paint.Style.STROKE
        visualPaint.strokeWidth = dpToPx(context, 2f)
        visualNeutralPaint.strokeWidth = dpToPx(context, 1.6f)

        visualPaths = ArrayList(3)
        visualPaths.add(VisualPath(ColorUtils.setAlphaComponent(levelColor, 102), dpToPx(context, 3f)))
        visualPaths.add(VisualPath(ColorUtils.setAlphaComponent(levelColor, 78), dpToPx(context, 1.2f)))
        visualPaths.add(VisualPath(ColorUtils.setAlphaComponent(levelColor, 48), dpToPx(context, 1f)))

        @Suppress("LeakingThis")
        setWillNotDraw(!drawVisuals)
    }

    private fun getBandViewContainer(): ViewGroup {
        return bandsContainer
    }

    /**
     * Setups the view with the given `audioFx`.
     * @param audioFx to bind with
     * @param animate if true, then the changes will be animated
     */
    @JvmOverloads
    fun setup(audioFx: AudioFx?, animate: Boolean = true) {

        val oldAudioFx = this.audioFx
        oldAudioFx?.unregisterObserver(audioFxObserver)

        this.audioFx = audioFx

        val container = getBandViewContainer()
        if (audioFx == null) {
            // No Audio Fx - no band views
            container.removeAllViews()
            return
        }

        if (isAttachedToWindow) {
            audioFx.registerObserver(audioFxObserver)
        }

        val numberOfBands = audioFx.numberOfBands.toInt()
        val minBandLevel = audioFx.minBandLevelRange.toInt()
        val maxBandLevel = audioFx.maxBandLevelRange.toInt()

        var addedBandCount = 0
        for (bandIndex in 0 until numberOfBands) {

            val bandView = if (bandIndex >= container.childCount) {
                val newBandView = onCreateBandView()
                val layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT)
                layoutParams.weight = 1f
                newBandView.layoutParams = layoutParams
                container.addView(newBandView, container.childCount)
                newBandView
            } else {
                @Suppress("UNCHECKED_CAST")
                container.getChildAt(bandIndex) as V
            }

            addedBandCount++

            val currentLevel = audioFx.getBandLevel(bandIndex.toShort()).toInt()
            val frequencyRange = audioFx.getBandFreqRange(bandIndex.toShort())

            val listener = object : BandListener {
                override fun onLevelChanged(bandView: BandView, level: Int) {
                    invalidate()
                    setBandLevelInternal(bandIndex, level)
                }

                override fun onAnimatedLevelChanged(bandView: BandView, animatedLevel: Int) {
                    invalidate()
                }
            }

            bandView.registerListener(listener)

            bandView.setLevelRange(minBandLevel, maxBandLevel)
            bandView.setLevel(currentLevel, animate)
            bandView.setLabel(getBandLabel(bandIndex, frequencyRange))
            bandView.setTrackTint(gridColor)
            bandView.setThumbTint(levelColor)
            bandView.setTag(R.id.tag_band_index, bandIndex)
        }

        // Removing views that are not bound to any band
        while (addedBandCount < container.childCount) {
            val childIndex = container.childCount - 1
            (container.getChildAt(childIndex) as V).unregisterAllListeners()
            container.removeViewAt(childIndex)
        }
    }

    /**
     * Creates a new band view.
     */
    abstract fun onCreateBandView(): V

    private fun setBandLevelInternal(bandIndex: Int, level: Int) {
        bandLevelHandler.removeMessages(bandIndex)
        val message = bandLevelHandler.obtainMessage(bandIndex, level, 0)
        bandLevelHandler.sendMessageDelayed(message, DEBOUNCE_SET_BAND_LEVEL)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val currAudioFx: AudioFx? = audioFx
        if (currAudioFx != null) {
            currAudioFx.registerObserver(audioFxObserver)
            val container = getBandViewContainer()
            val numberOfBands = currAudioFx.numberOfBands.toInt()
            val viewChildCount = container.childCount
            // Actually, the number of bands must be equal to the child count
            for (i in 0 until numberOfBands.coerceAtMost(viewChildCount)) {
                val level = currAudioFx.getBandLevel(i.toShort()).toInt()
                val bandView = container.getChildAt(i) as BandView
                bandView.setLevel(level, false)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        audioFx?.unregisterObserver(audioFxObserver)
        bandLevelHandler.removeCallbacksAndMessages(null)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (drawVisuals) {
            preDrawVisuals(canvas)
        }

        super.dispatchDraw(canvas)

        if (drawVisuals) {
            postDrawVisuals(canvas)
        }
    }

    /**
     * Draws visuals under the band views.
     */
    private fun preDrawVisuals(canvas: Canvas) {
        val container = getBandViewContainer()
        val bandViewCount = container.childCount
        if (bandViewCount < 1) {
            // Nothing to draw
            return
        }

        val firstBandView = container.getChildAt(0) as V

        // The y of the center
        val neutralY = container.top + container.paddingTop + firstBandView.centerY

        // The centered horizontal line
        visualNeutralPaint.strokeWidth = gridLineThickness
        visualNeutralPaint.color = gridColor
        canvas.drawLine(paddingLeft.toFloat(), neutralY - visualNeutralPaint.strokeWidth / 2,
                measuredWidth - paddingRight.toFloat(), neutralY - visualNeutralPaint.strokeWidth / 2, visualNeutralPaint)
    }

    /**
     * Draws visuals over the band views.
     */
    private fun postDrawVisuals(canvas: Canvas) {
        val container = getBandViewContainer()
        val bandViewCount = container.childCount
        if (bandViewCount < 1) {
            // Nothing to draw
            return
        }

        val firstBandView = container.getChildAt(0) as V

        // The y of the center
        val neutralY = container.top + container.paddingTop + firstBandView.centerY

        // Visual paths
        for (visualPath in visualPaths) {
            visualPath.path.reset()
            visualPath.tmpCx1 = paddingLeft.toFloat()
            visualPath.tmpCy1 = neutralY
            visualPath.path.moveTo(visualPath.tmpCx1, visualPath.tmpCy1)
        }
        for (i in 0..bandViewCount) {
            for (visualPathIndex in visualPaths.indices) {
                val visualPath = visualPaths[visualPathIndex]
                val yCoefficient = when (visualPathIndex) {
                    0 -> 0f
                    1 -> 0.2f
                    2 -> 0.3f
                    else -> 0.5f
                }
                if (i < bandViewCount) {
                    val bandView = container.getChildAt(i) as V
                    val centerY = bandView.thumbCenterY
                    visualPath.tmpCx2 = bandView.left + bandView.thumbCenterX
                    visualPath.tmpCy2 = bandView.top + centerY + (neutralY - centerY) * yCoefficient
                } else {
                    visualPath.tmpCx2 = measuredWidth - paddingRight.toFloat()
                    visualPath.tmpCy2 = neutralY
                }
                val x1 = visualPath.tmpCx1 + (visualPath.tmpCx2 - visualPath.tmpCx1) / 2f
                val y1 = visualPath.tmpCy1
                val x2 = visualPath.tmpCx1 + (visualPath.tmpCx2 - visualPath.tmpCx1) / 2f
                val y2 = visualPath.tmpCy2
                visualPath.path.cubicTo(x1, y1, x2, y2, visualPath.tmpCx2, visualPath.tmpCy2)
                visualPath.tmpCx1 = visualPath.tmpCx2
                visualPath.tmpCy1 = visualPath.tmpCy2
            }
        }

        for (visualPath in visualPaths) {
            visualPaint.color = visualPath.color
            visualPaint.strokeWidth = visualPath.strokeWidth
            // This offset helps to draw the path centered at its Y coors according to the stroke width
            visualPath.path.offset(0f, -visualPath.strokeWidth / 2f)
            canvas.drawPath(visualPath.path, visualPaint)
        }
    }

    /**
     * Internal state of a visual path.
     */
    private class VisualPath constructor(
        @ColorInt val color:
        Int, val strokeWidth: Float
    ) {
        val path: Path = Path()

        // tmp values used to calculate pixel positions while drawing
        var tmpCx1 = 0f
        var tmpCy1 = 0f
        var tmpCx2 = 0f
        var tmpCy2 = 0f
    }

    interface BandView {
        val actualLevel: Int
        val animatedLevel: Int

        val thumbCenterX: Float
        val thumbCenterY: Float
        val centerY: Float

        fun setLevelRange(minLevel: Int, maxLevel: Int)
        fun setLevel(level: Int, animate: Boolean)

        fun setLabel(label: CharSequence)

        fun setTrackTint(@ColorInt color: Int)
        fun setThumbTint(@ColorInt color: Int)

        fun registerListener(listener: BandListener)
        fun unregisterListener(listener: BandListener)
        fun unregisterAllListeners()
    }

    interface BandListener {
        /**
         * Called when the actual level of the band has been changed by the user.
         */
        fun onLevelChanged(bandView: BandView, level: Int)

        /**
         * Called when the animated level of the band has been changed due to animation update.
         * NOTE: [animatedLevel] is not the actual value of the band level.
         */
        fun onAnimatedLevelChanged(bandView: BandView, animatedLevel: Int)
    }

    companion object {
        private const val DEFAULT_STYLE_RES_ID = R.style.EqualizerView_Default
        private const val DEFAULT_GRID_COLOR = Color.TRANSPARENT
        private const val DEFAULT_LEVEL_COLOR = Color.LTGRAY

        private const val DEBOUNCE_SET_BAND_LEVEL = 300L

        private fun getBandLabel(bandIndex: Int, frequencyRange: IntArray): String {
            val freq: Int = frequencyRange.getOrNull(0) ?: 0
            return when {
                //freq > 1_000_000 -> (freq / 1_000_000).toString() + "\nkkHz"
                else -> (freq / 1000).toString() + "\nKHz"
            }
        }

        private fun dpToPx(context: Context, dp: Float): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }
}