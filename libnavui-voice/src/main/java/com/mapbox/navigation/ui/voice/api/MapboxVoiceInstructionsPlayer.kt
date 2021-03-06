package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.AudioManager
import androidx.annotation.UiThread
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayer
import com.mapbox.navigation.ui.base.api.voice.VoiceInstructionsPlayerCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.ui.voice.options.VoiceInstructionsPlayerOptions
import java.lang.IllegalArgumentException
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Hybrid implementation of [VoiceInstructionsPlayer] combining [VoiceInstructionsTextPlayer] and
 * [VoiceInstructionsFilePlayer] speech players.
 * @property context Context
 * @property accessToken String
 * @property language [Locale] language (ISO 639)
 * @property options [VoiceInstructionsPlayerOptions] (optional)
 */
@UiThread
class MapboxVoiceInstructionsPlayer @JvmOverloads constructor(
    private val context: Context,
    private val accessToken: String,
    private val language: String,
    private val options: VoiceInstructionsPlayerOptions = VoiceInstructionsPlayerOptions.Builder()
        .build()
) : VoiceInstructionsPlayer {

    private val playCallbackQueue: Queue<PlayCallback> = ConcurrentLinkedQueue()
    private val filePlayer: VoiceInstructionsFilePlayer =
        VoiceInstructionsFilePlayerProvider.retrieveVoiceInstructionsFilePlayer(
            context,
            accessToken,
            language
        )
    private val textPlayer: VoiceInstructionsTextPlayer =
        VoiceInstructionsTextPlayerProvider.retrieveVoiceInstructionsTextPlayer(context, language)
    private var localCallback: VoiceInstructionsPlayerCallback =
        object : VoiceInstructionsPlayerCallback {
            override fun onDone(state: SpeechState.DonePlaying) {
                audioFocusDelegate.abandonFocus()
                val currentPlayCallback = playCallbackQueue.poll()
                val announcement = currentPlayCallback.announcement
                val clientCallback = currentPlayCallback.callback
                clientCallback.onDone(SpeechState.DonePlaying(announcement.announcement))
                play()
            }
        }
    private val audioFocusDelegate: AudioFocusDelegate =
        AudioFocusDelegateProvider.retrieveAudioFocusDelegate(
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
            options
        )

    /**
     * Given [SpeechState.ReadyToPlay] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    override fun play(state: SpeechState.ReadyToPlay, callback: VoiceInstructionsPlayerCallback) {
        playCallbackQueue.add(PlayCallback(state, callback))
        if (playCallbackQueue.size == 1) {
            play()
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * Volume is specified as a float ranging from 0 to 1
     * where 0 is silence, and 1 is the maximum volume (the default behavior).
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        if (state.level < MIN_VOLUME_LEVEL || state.level > MAX_VOLUME_LEVEL) {
            throw IllegalArgumentException(
                "Volume level needs to be a float ranging from 0 to 1."
            )
        }
        filePlayer.volume(state)
        textPlayer.volume(state)
    }

    /**
     * Clears any announcements queued.
     */
    override fun clear() {
        clean()
        filePlayer.clear()
        textPlayer.clear()
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        clean()
        filePlayer.shutdown()
        textPlayer.shutdown()
    }

    private fun play() {
        if (playCallbackQueue.isNotEmpty()) {
            audioFocusDelegate.requestFocus()
            val currentPlayCallback = playCallbackQueue.peek()
            val currentPlay = currentPlayCallback.announcement
            currentPlay.announcement.file?.let {
                filePlayer.play(currentPlay, localCallback)
            } ?: textPlayer.play(currentPlay, localCallback)
        }
    }

    private fun clean() {
        playCallbackQueue.clear()
    }

    private companion object {
        private const val MAX_VOLUME_LEVEL = 1.0f
        private const val MIN_VOLUME_LEVEL = 0.0f
    }
}
