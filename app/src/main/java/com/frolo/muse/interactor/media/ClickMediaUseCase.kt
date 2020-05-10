package com.frolo.muse.interactor.media

import com.frolo.muse.engine.Player
import com.frolo.muse.engine.SongQueueFactory
import com.frolo.muse.navigator.Navigator
import com.frolo.muse.model.media.*
import com.frolo.muse.repository.GenericMediaRepository
import com.frolo.muse.rx.SchedulerProvider
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Function


class ClickMediaUseCase <E: Media> constructor(
        private val schedulerProvider: SchedulerProvider,
        private val player: Player,
        private val repository: GenericMediaRepository,
        private val navigator: Navigator,
        private val songQueueFactory: SongQueueFactory
) {

    private fun processPlay(target: Song, songs: List<Song>, toggleIfSameSong: Boolean) {
        val currentSong = player.getCurrent()
        if (toggleIfSameSong && currentSong?.id == target.id) {
            // if we've chosen the same song that is currently being played then toggle the playback
            player.toggle()
        } else {
            // otherwise, create new song queue and start playing it
            val songQueue = songQueueFactory.create(listOf(target), songs)
            player.prepare(songQueue, target, true)
        }
    }

    fun click(item: E, fromCollection: Collection<E>): Completable = when(item.kind) {
        Media.SONG -> {
            Single.fromCallable { fromCollection.filterIsInstance<Song>() }
                    .subscribeOn(schedulerProvider.computation())
                    .doOnSuccess { songs ->
                        processPlay(item as Song, songs, true)
                    }
                    .ignoreElement()
        }

        Media.ALBUM -> {
            Single.fromCallable { item as Album }
                    .observeOn(schedulerProvider.main())
                    .doOnSuccess { navigator.openAlbum(it) }
                    .ignoreElement()
        }

        Media.ARTIST -> {
            Single.fromCallable { item as Artist }
                    .observeOn(schedulerProvider.main())
                    .doOnSuccess { navigator.openArtist(it) }
                    .ignoreElement()
        }

        Media.GENRE -> {
            Single.fromCallable { item as Genre }
                    .observeOn(schedulerProvider.main())
                    .doOnSuccess { navigator.openGenre(it) }
                    .ignoreElement()
        }

        Media.PLAYLIST -> {
            Single.fromCallable { item as Playlist }
                    .observeOn(schedulerProvider.main())
                    .doOnSuccess { navigator.openPlaylist(it) }
                    .ignoreElement()
        }

        Media.MY_FILE -> {
            Single.fromCallable { item as MyFile }
                    .flatMapCompletable { myFile -> when {
                        myFile.isDirectory -> Completable.complete()
                                .observeOn(schedulerProvider.main())
                                .doOnComplete { navigator.openMyFile(myFile) }

                        myFile.isSongFile -> repository.collectSongs(myFile)
                            .subscribeOn(schedulerProvider.worker())
                            // Since the item is a song file itself then we create a collection of just 1 item
                            .map { songs -> songs.first() }
                            .flatMap { targetSong ->
                                val sources = fromCollection.filter { it is MyFile && it.isSongFile }
                                        .map { myFile ->
                                            repository.collectSongs(myFile).onErrorReturnItem(emptyList())
                                        }

                                Single.zip(
                                        sources,
                                        Function<Array<*>, List<Song>> { arr ->
                                            arr.filter { it is List<*> && it.size == 1 }
                                                    .map { (it as List<*>).first() as Song }
                                        })
                                        .onErrorResumeNext(Single.just(listOf(targetSong)))
                                        .map { songs -> targetSong to songs }
                            }
                            .doOnSuccess { pair ->
                                processPlay(pair.first, pair.second, true)
                            }
                            .ignoreElement()

                        else -> Completable.complete()
                    } }
        }

        else -> Completable.error(UnknownMediaException(item))
    }

}