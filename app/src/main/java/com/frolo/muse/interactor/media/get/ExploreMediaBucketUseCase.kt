package com.frolo.muse.interactor.media.get

import com.frolo.muse.engine.AudioSource
import com.frolo.muse.model.Library
import com.frolo.muse.model.media.MediaBucket
import com.frolo.muse.model.media.MediaFile
import com.frolo.muse.repository.MediaFileRepository
import com.frolo.muse.repository.Preferences
import com.frolo.muse.rx.SchedulerProvider
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Flowable
import io.reactivex.Single


class ExploreMediaBucketUseCase @AssistedInject constructor(
    private val schedulerProvider: SchedulerProvider,
    private val repository: MediaFileRepository,
    private val preferences: Preferences,
    @Assisted private val bucket: MediaBucket
): GetAllMediaUseCase<MediaFile>(Library.FOLDERS, schedulerProvider, repository, preferences) {

    fun getBucket(): Flowable<MediaBucket> {
        return Flowable.just(bucket)
    }

    fun detectPlayingPosition(list: List<MediaFile>?, audioSource: AudioSource?): Single<Int> {
        return if (list != null && audioSource != null) {
            return Single.fromCallable {
                list.indexOfFirst { mediaFile -> mediaFile.id == audioSource.id }
            }.subscribeOn(schedulerProvider.computation())
        } else {
            Single.just(-1)
        }
    }

    override fun getSortedCollection(sortOrder: String): Flowable<List<MediaFile>> {
        return repository.getSortedAudioFiles(bucket, sortOrder)
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(bucket: MediaBucket): ExploreMediaBucketUseCase
    }

    companion object {
        private const val PATH_SEPARATOR = "/"
    }


}