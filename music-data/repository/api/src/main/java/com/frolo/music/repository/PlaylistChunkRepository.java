package com.frolo.music.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frolo.music.model.Playlist;
import com.frolo.music.model.Song;

import java.util.Collection;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface PlaylistChunkRepository extends SongRepository {
    /* *******************************************
     * ********************************************
     * ********************************************
     * ********************************************
     * **************   PLAYLIST   ****************
     * ********************************************
     * ********************************************
     * ********************************************
     * ***************************************** */

    Single<Boolean> isMovingAllowedForSortOrder(String sortOrder);

    Completable addToPlaylist(Playlist playlist, Collection<Song> items);

    Completable removeFromPlaylist(Playlist playlist, Song item);

    Completable removeFromPlaylist(Playlist playlist, Collection<Song> items);

    Completable moveItemInPlaylist(Playlist playlist, int fromPos, int toPos);

    Completable moveItemInPlaylist(MoveOp op);

    final class MoveOp {
        /**
         * Target song item that is being moved.
         */
        @NonNull
        public final Song target;
        /**
         * The song that will be 'previous' to the target after the movement completes.
         */
        @Nullable
        public final Song previous;
        /**
         * The song that will be 'next' to the target after the movement completes.
         */
        @Nullable
        public final Song next;

        public MoveOp(@NonNull Song target, @Nullable Song previous, @Nullable Song next) {
            this.target = target;
            this.previous = previous;
            this.next = next;
        }
    }
}
