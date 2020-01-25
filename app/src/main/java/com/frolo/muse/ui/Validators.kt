package com.frolo.muse.ui

import android.content.Context
import android.content.res.Resources
import android.text.format.DateUtils
import com.frolo.muse.R
import com.frolo.muse.model.Library
import com.frolo.muse.model.media.*
import java.text.SimpleDateFormat
import java.util.*

val DATE_FORMAT = SimpleDateFormat("MM.dd.yyyy", Locale.US)

fun Song.getNameString(res: Resources): String {
    return title.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Song.getArtistString(res: Resources): String {
    return artist.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Song.getAlbumString(res: Resources): String {
    return album.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Album.getNameString(res: Resources): String {
    return name.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Album.getArtistString(res: Resources): String {
    return artist.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Album.getNumberOfTracksString(res: Resources): String {
    return numberOfSongs.let { count ->
        when (count) {
            0 -> res.getString(R.string.no_tracks)
            1 -> res.getString(R.string.one_track)
            else -> res.getString(R.string.number_of_tracks, count.toString())
        }
    }
}

fun Artist.getNameString(res: Resources): String {
    return name.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Artist.getNumberOfAlbumsString(res: Resources): String {
    return numberOfAlbums.let { count ->
        when (count) {
            0 -> res.getString(R.string.no_albums)
            1 -> res.getString(R.string.one_album)
            else -> res.getString(R.string.number_of_albums, count.toString())
        }
    }
}

fun Artist.getNumberOfTracksString(res: Resources): String {
    return numberOfTracks.let { number ->
        when (number) {
            0 -> res.getString(R.string.no_tracks)
            1 -> res.getString(R.string.one_track)
            else -> res.getString(R.string.number_of_tracks, number.toString())
        }
    }
}

fun Genre.getNameString(res: Resources): String {
    return name.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Playlist.getNameString(res: Resources): String {
    return name.run {
        if (this.isNullOrBlank()) res.getString(R.string.placeholder_unknown) else this
    }
}

fun Playlist.getDateAddedString(res: Resources): String {
    return DATE_FORMAT.format(Date(dateAdded * 1000))
}

fun Playlist.getDateModifiedString(res: Resources): String {
    return DATE_FORMAT.format(Date(dateModified * 1000))
}

fun MyFile.getNameString(): String {
    return javaFile?.name ?: ""
}

fun MyFile.getNameAsRootString(): String {
    return "..." + (javaFile?.name ?: "")
}

fun Song.getDurationString(): String {
    return duration.asDurationInMs()
}

//region Relative time spans
private const val MIN_TIME_SPAN = (1000 * 60 * 60 * 24).toLong() // 1 day
private const val TIME_SPAN_FORMATS = DateUtils.FORMAT_ABBREV_ALL

fun SongWithPlayCount.getLastTimePlayedString(ctx: Context): CharSequence {
    val time = lastPlayTime ?: return ""
    val now = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(time, now, MIN_TIME_SPAN, TIME_SPAN_FORMATS)
}
//endregion

fun Int.asDurationInMs(): String {
    val totalSeconds = this / 1000
    val totalMinutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    //int totalHours = totalMinutes / 60;
    val format = if (seconds < 10) "%d:0%d" else "%d:%d"
    return String.format(format, totalMinutes, seconds)
}

fun Media.getName(): String {
    return when (this) {
        is Song ->  title
        is Album -> name
        is Artist -> name
        is Genre -> name
        is Playlist -> name
        is MyFile -> javaFile?.name ?: ""
        else -> ""
    }
}

fun Media.getTypeName(res: Resources): String {
    return when (kind) {
        Media.SONG -> res.getString(R.string.track)
        Media.ALBUM -> res.getString(R.string.album)
        Media.ARTIST -> res.getString(R.string.artist)
        Media.GENRE -> res.getString(R.string.genre)
        Media.PLAYLIST -> res.getString(R.string.playlist)
        Media.MY_FILE -> res.getString(R.string.file)
        else -> res.getString(R.string.placeholder_unknown)
    }
}

fun getSectionName(res: Resources, @Library.Section section: Int): String {
    return when(section) {
        Library.ALL_SONGS -> res.getString(R.string.all_songs)
        Library.ALBUMS -> res.getString(R.string.albums)
        Library.ARTISTS -> res.getString(R.string.artists)
        Library.GENRES -> res.getString(R.string.genres)
        Library.FAVOURITES -> res.getString(R.string.nav_favourite)
        Library.PLAYLISTS -> res.getString(R.string.playlists)
        Library.FOLDERS -> res.getString(R.string.folders)
        Library.RECENTLY_ADDED -> res.getString(R.string.recently_added)
        Library.MOST_PLAYED -> res.getString(R.string.most_played)
        else -> res.getString(R.string.placeholder_unknown)
    }
}

// Returns true if the media item may have several related to it songs
fun Media.mayHaveSeveralRelatedSongs(): Boolean {
    return when (kind) {
        Media.ALBUM,
        Media.ARTIST,
        Media.GENRE,
        Media.PLAYLIST -> true
        Media.MY_FILE -> (this as MyFile).isDirectory
        else -> false
    }
}

fun Context.getDeleteConfirmationMessage(item: Media): String {
    val msgResId = if (item.mayHaveSeveralRelatedSongs()
            && item.kind != Media.PLAYLIST) // Since we don't delete songs when deleting playlists
        R.string.confirmation_delete_item_and_related_song_files
    else R.string.confirmation_delete_item

    return getString(msgResId)
}

fun Context.getDeleteConfirmationMessage(items: List<Media>): String {
    val firstItem = items.firstOrNull() ?: return getString(R.string.confirmation_delete_items)

    val msgResId = if (firstItem.mayHaveSeveralRelatedSongs()
            && firstItem.kind != Media.PLAYLIST) // Since we don't delete songs when deleting playlists
        R.string.confirmation_delete_items_and_related_song_files
    else R.string.confirmation_delete_items

    return getString(msgResId)
}