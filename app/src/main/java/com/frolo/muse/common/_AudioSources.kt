package com.frolo.muse.common

import com.frolo.muse.engine.AudioSource
import com.frolo.muse.model.media.Song


val AudioSource.title: String? get() = metadata.title

val AudioSource.artistId: Long get() = metadata.artistId

val AudioSource.artist: String? get() = metadata.artist

val AudioSource.albumId: Long get() = metadata.albumId

val AudioSource.album: String? get() = metadata.album

val AudioSource.genre: String? get() = metadata.genre

val AudioSource.duration: Int get() = metadata.duration

val AudioSource.year: Int get() = metadata.year

val AudioSource.trackNumber: Int get() = metadata.trackNumber

fun AudioSource.toSong(): Song {
    return if (this is Song) this
    else Util.createSong(this)
}

fun List<AudioSource>.toSongs(): List<Song> = map { it.toSong() }