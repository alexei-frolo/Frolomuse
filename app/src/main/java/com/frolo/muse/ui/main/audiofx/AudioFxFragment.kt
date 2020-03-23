package com.frolo.muse.ui.main.audiofx

import android.Manifest
import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.frolo.muse.R
import com.frolo.muse.StyleUtil
import com.frolo.muse.Trace
import com.frolo.muse.arch.observeNonNull
import com.frolo.muse.ui.base.BaseFragment
import com.frolo.muse.ui.base.NoClipping
import com.frolo.muse.ui.main.audiofx.adapter.PresetAdapter
import com.frolo.muse.ui.main.audiofx.adapter.PresetReverbAdapter
import com.frolo.muse.ui.main.audiofx.preset.PresetSavedEvent
import com.frolo.muse.views.Anim
import com.frolo.muse.views.equalizer.EqualizerLayout
import com.frolo.muse.views.observeSelection
import com.frolo.muse.views.visualizer.SpectrumRenderer
import kotlinx.android.synthetic.main.fragment_audio_fx.*
import kotlinx.android.synthetic.main.include_audio_fx_content.*
import kotlinx.android.synthetic.main.include_message.*
import kotlinx.android.synthetic.main.include_preset_chooser.*
import kotlinx.android.synthetic.main.include_preset_reverb_chooser.*
import kotlinx.android.synthetic.main.include_seekbar_bass_boost.*
import kotlinx.android.synthetic.main.include_seekbar_visualizer.*
import kotlinx.android.synthetic.main.include_toolbar.*


class AudioFxFragment: BaseFragment(), NoClipping {

    companion object {

        // Factory
        fun newInstance() = AudioFxFragment()
    }

    private val viewModel: AudioFxViewModel by viewModel()

    private var trackingEqualizerBand = false
    private var trackingBass = false
    private var trackingVirtualizer = false

    private var enableStatusSwitchView: CompoundButton? = null
    private var visualizer: Visualizer? = null

    private lateinit var presetSaveEvent: PresetSavedEvent

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presetSaveEvent = PresetSavedEvent.register(context) { preset ->
            viewModel.onPresetSaved(preset)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_audio_fx, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(tb_actions)
            supportActionBar?.apply {
                setTitle(R.string.nav_equalizer)
                setDisplayShowTitleEnabled(true)
            }
        }

        initEqBars()

        initPresetChooser()

        initPresetReverbChooser()

        initBassBoostBar()

        initVirtualizer()

        initVisualizer()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel(viewLifecycleOwner)
        viewModel.onOpened()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_audio_fx, menu)
        menu.findItem(R.id.action_playback_params).also { menuItem ->
            // this option is visible only for android versions MARSHMALLOW and higher
            menuItem.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }
        menu.findItem(R.id.action_switch_audio_fx).also { menuItem ->
            val shouldBeEnabled = viewModel.audioFxEnabled.value ?: false
            val switchView = menuItem.actionView as CompoundButton
            if (switchView.isChecked != shouldBeEnabled) {
                switchView.isChecked = shouldBeEnabled
            }
            switchView.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onEnableStatusChanged(isChecked)
            }
            enableStatusSwitchView = switchView
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_playback_params -> viewModel.onPlaybackParamsOptionSelected()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        layout_audio_fx_hint.visibility = View.GONE
    }

    override fun onDestroyView() {
        visualizer?.also { safeVisualizer ->
            safeVisualizer.runCatching {
                release()
            }.onFailure {
                Trace.e(it)
            }
        }
        visualizer = null
        super.onDestroyView()
    }

    override fun onDetach() {
        presetSaveEvent.unregister(requireContext())
        super.onDetach()
    }

    private fun showWaveForm() {
        visualizer_view.visibility = View.VISIBLE

        val context = visualizer_view.context
        //val extinctColor = ContextCompat.getColor(context, R.color.ghost)
        val gainedColor = StyleUtil.getVisualizerColor(context)

        visualizer_view.renderer = SpectrumRenderer().apply {
            color = gainedColor
        }

        runCatching {
            if (visualizer == null) {
                visualizer = viewModel.audioSessionId.value?.let { Visualizer(it) }
            }
        }.onFailure {
            Trace.e(it)
            toastError(it)
        }

        visualizer?.also { safeVisualizer ->
            safeVisualizer.runCatching {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        // we ignore fft
                    }
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        visualizer_view?.setData(waveform)
                    }
                }, Visualizer.getMaxCaptureRate(), true, false)
                enabled = true
            }.onFailure {
                Trace.e(it)
            }
        }
    }

    private fun initVisualizer() {
        btn_show_visualizer.setOnClickListener {
            requestRxPermissions(Manifest.permission.RECORD_AUDIO) { granted ->
                if (granted) {
                    btn_show_visualizer.visibility = View.GONE
                    showWaveForm()
                }
            }
        }

        if (isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
            btn_show_visualizer.visibility = View.GONE
            showWaveForm()
        }
    }

    private fun initEqBars() {
        layout_eq_bars.setOnBandLevelChangeListener(object : EqualizerLayout.OnBandLevelChangeListener {
            override fun onStartTrackingBandLevel(band: Short, level: Short) {
                trackingEqualizerBand = true
            }
            override fun onBandLevelChange(band: Short, level: Short) {
                viewModel.onBandLevelChanged(band, level)
            }
            override fun onStopTrackingBandLevel(band: Short, level: Short) {
                trackingEqualizerBand = false
            }
        })
    }

    private fun initBassBoostBar() {
        sb_bass_boost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewModel.onBassStrengthChanged(seekBar.progress.toShort())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                trackingBass = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                trackingBass = false
            }
        })
    }

    private fun initVirtualizer() {
        sb_virtualizer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewModel.onVirtStrengthChanged(seekBar.progress.toShort())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                trackingVirtualizer = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                trackingVirtualizer = false
            }
        })
    }

    private fun initPresetChooser() {
        sp_presets.observeSelection { adapterView, position ->
            val adapter = adapterView.adapter as? PresetAdapter
            if (adapter != null) {
                val item = adapter.getItem(position)
                viewModel.onPresetSelected(item)
            }
        }

        btn_save_preset.setOnClickListener {
            val currentBandLevels = layout_eq_bars.getLevels()
            viewModel.onSavePresetButtonClicked(currentBandLevels)
        }
    }

    private fun initPresetReverbChooser() {
        sp_preset_reverbs.observeSelection { adapterView, position ->
            val adapter = adapterView.adapter as? PresetReverbAdapter
            if (adapter != null) {
                val item = adapter.getItem(position)
                viewModel.onPresetReverbSelected(item.first)
            }
        }
    }

    private fun observeViewModel(owner: LifecycleOwner) = with(viewModel) {
        error.observeNonNull(owner) { err ->
            toastError(err)
        }

        audioFxAvailable.observeNonNull(owner) { available ->
            if (available) {
                layout_audio_fx_content.setOnTryTouchingListener {
                    if (layout_audio_fx_hint.visibility != View.VISIBLE) {
                        Anim.fadeIn(layout_audio_fx_hint, duration = 300)
                        runDelayedOnUI({
                            Anim.fadeOut(layout_audio_fx_hint, 300)
                        }, 1800)
                    }
                }
                layout_player_placeholder.visibility = View.GONE
            } else {
                tv_message.setText(R.string.current_playlist_is_empty)
                layout_player_placeholder.setOnClickListener { } // to make all view under overlay non-clickable
                layout_player_placeholder.visibility = View.VISIBLE
            }
        }

        equalizerAvailable.observeNonNull(owner) { available ->
            layout_eq_bars.visibility = if (available) View.VISIBLE else View.GONE
            ll_preset_chooser.visibility = if (available) View.VISIBLE else View.GONE
        }

        bassBoostAvailable.observeNonNull(owner) { available ->
            ll_bass_boost.visibility = if (available) View.VISIBLE else View.GONE
        }

        virtualizerAvailable.observeNonNull(owner) { available ->
            ll_virtualizer.visibility = if (available) View.VISIBLE else View.GONE
        }

        presetReverbAvailable.observeNonNull(owner) { available ->
            ll_preset_reverb_chooser.visibility = if (available) View.VISIBLE else View.GONE
        }

        audioFxEnabled.observeNonNull(owner) { enabled ->
            layout_audio_fx_content.isEnabled = enabled

            if (layout_audio_fx_hint.visibility == View.VISIBLE) {
                Anim.fadeOut(layout_audio_fx_hint)
            }

            val toAlpha = if (enabled) 1.0f else 0.3f
            Anim.alpha(layout_audio_fx_content, toAlpha)

            enableStatusSwitchView?.isChecked = enabled
        }

        bandLevels.observeNonNull(owner) { audioFx ->
            layout_eq_bars.bindWith(audioFx, true)
        }

        presets.observeNonNull(owner) { presets ->
            val adapter = PresetAdapter(presets) { item ->
                onDeletePresetClicked(item)
            }
            sp_presets.adapter = adapter
            val position = viewModel.currentPreset.value?.let { adapter.indexOf(it) } ?: -1
            if (position >= 0 && position < adapter.count) {
                sp_presets.setSelection(position, false)
            }
        }

        currentPreset.observeNonNull(owner) { preset ->
            val adapter = sp_presets.adapter as? PresetAdapter
            if (adapter != null) {
                val position = adapter.indexOf(preset)
                if (position >= 0 && position < adapter.count) {
                    sp_presets.setSelection(position, false)
                }
            }
        }

        bassStrengthRange.observeNonNull(owner) { range ->
            sb_bass_boost.max = range.second.toInt()
        }

        bassStrength.observeNonNull(owner) { strength ->
            sb_bass_boost.progress = strength.toInt()
        }

        virtStrengthRange.observeNonNull(owner) { range ->
            sb_virtualizer.max = range.second.toInt()
        }

        virtStrength.observeNonNull(owner) { strength ->
            sb_virtualizer.progress = strength.toInt()
        }

        presetReverbs.observeNonNull(owner) { reverbs ->
            sp_preset_reverbs.adapter = PresetReverbAdapter(reverbs)
            val position = presetReverbIndex.value.let { index ->
                reverbs.indexOfFirst { it.first == index }
            }
            sp_preset_reverbs.setSelection(position, false)
        }

        presetReverbIndex.observeNonNull(owner) { index ->

        }
    }

    override fun removeClipping(left: Int, top: Int, right: Int, bottom: Int) {
        view?.also { safeView ->
            if (safeView is ViewGroup) {
                safeView.setPadding(left, top, right, bottom)
                safeView.clipToPadding = false
            }
        }
    }
}