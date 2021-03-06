package com.frolo.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public interface PlayerObserver {

    /**
     * Called when <code>player</code> has been prepared.
     * From this point until {@link PlayerObserver#onAudioSourceChanged(Player, AudioSource, int)} method is called,
     * calling the {@link Player#getProgress()}, {@link Player#getDuration()}, {@link Player#isPlaying()} methods really makes sense.
     * @param player player that is prepared
     * @param duration duration of the current audio source
     * @param progress current playback progress
     */
    void onPrepared(@NonNull Player player, int duration, int progress);

    /**
     * Called when <code>player</code> starts playing.
     * @param player that starts playing
     */
    void onPlaybackStarted(@NonNull Player player);

    /**
     * Called when <code>player</code> pauses playback.
     * @param player that pauses playback
     */
    void onPlaybackPaused(@NonNull Player player);

    /**
     * Called when the user has positioned the playback at the given <code>position</code>.
     * NOTE: called only when the user changes the position himself.
     * @param player on which the user has positioned the playback at the position
     */
    void onSoughtTo(@NonNull Player player, int position);

    /**
     * Called when the current audio source queue gets changed for the given <code>player</code>.
     * @param player for which the current queue is changed
     * @param queue new audio source queue
     */
    void onQueueChanged(@NonNull Player player, @NonNull AudioSourceQueue queue);

    /**
     * Called when the current audio source gets changed for the given <code>player</code>.
     * @param player for which the current audio source is changed
     * @param item new audio source
     * @param positionInQueue position of the new audio source in the current queue
     */
    void onAudioSourceChanged(@NonNull Player player, @Nullable AudioSource item, int positionInQueue);

    /**
     * Called when the current audio source item has been updated for the given <code>player</code>.
     * This is mainly due to calls to the {@link Player#update(AudioSource)} method.
     * @param player for which the current audio source has been updated
     * @param item updated audio source value
     */
    void onAudioSourceUpdated(@NonNull Player player, @NonNull AudioSource item);

    /**
     * Called when the position of the current audio source in queue gets changed for the given <code>player</code>.
     * @param player for which the current audio source is changed
     * @param positionInQueue new position of the current audio source in the queue
     */
    void onPositionInQueueChanged(@NonNull Player player, int positionInQueue);

    /**
     * Called when the current shuffle mode gets changed for the given <code>player</code>.
     * @param player for which the current shuffle mode is changed
     * @param mode new shuffle mode
     */
    void onShuffleModeChanged(@NonNull Player player, @Player.ShuffleMode int mode);

    /**
     * Called when the current repeat mode gets changed for the given <code>player</code>.
     * @param player for which the current repeat mode is changed
     * @param mode new repeat mode
     */
    void onRepeatModeChanged(@NonNull Player player, @Player.RepeatMode int mode);

    /**
     * Called when the given <code>player</code> is shutdown.
     * This is the termination state, no calls to the callback methods are expected after this.
     * @param player that is shutdown
     */
    void onShutdown(@NonNull Player player);

    /**
     * Called when the A-B status gets changed for the given <code>player</code>.
     * Normally <code>bPointed</code> should not be true if <code>aPointed</code> is false.
     * @param player for which the A-B status is changed
     * @param aPointed true if the A is pointed, false - otherwise
     * @param bPointed true if the B is pointed, false - otherwise.
     */
    void onABChanged(@NonNull Player player, boolean aPointed, boolean bPointed);

    /**
     * Called when the playback speed gets changed for the given <code>player</code>.
     * @param player for which the playback speed is changed
     * @param speed the new speed
     */
    void onPlaybackSpeedChanged(@NonNull Player player, float speed);

    /**
     * Called when the playback pitch gets changed for the given <code>player</code>.
     * @param player for which the playback pitch is changed
     * @param pitch the new pitch
     */
    void onPlaybackPitchChanged(@NonNull Player player, float pitch);

    /**
     * Called when an internal error has occurred in the given <code>player</code>.
     * This is not a terminate state, it is only a notification that something went wrong.
     * @param player with an internal error
     * @param error internal error
     */
    void onInternalErrorOccurred(@NonNull Player player, @NonNull Throwable error);

}