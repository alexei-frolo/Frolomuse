package com.frolo.muse.firebase

import com.google.android.gms.tasks.SuccessContinuation
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.Completable
import io.reactivex.Single
import java.util.concurrent.Executors


object FirebaseRemoteConfigUtil {

    private const val STRICT_ACTIVATION = false

    const val LYRICS_VIEWER_ENABLED = "lyrics_viewer_enabled"

    private val executor = Executors.newFixedThreadPool(2)

    fun fetchAndActivate(minimumFetchIntervalInSeconds: Long? = null): Completable {
        return Completable.create { emitter ->
            val configInstance = FirebaseRemoteConfig.getInstance()
            val task = if (minimumFetchIntervalInSeconds != null && minimumFetchIntervalInSeconds >= 0L) {
                val continuation = SuccessContinuation<Void, Boolean> { configInstance.activate() }
                configInstance.fetch(minimumFetchIntervalInSeconds).onSuccessTask(executor, continuation)
            } else {
                configInstance.fetchAndActivate()
            }

            task.addOnFailureListener {
                if (!emitter.isDisposed) {
                    emitter.onError(it)
                }
            }

            task.addOnCompleteListener { _task ->
                if (!emitter.isDisposed) {
                    if (_task.isSuccessful) {
                        val isActivated = _task.result
                        if (!STRICT_ACTIVATION || isActivated) {
                            emitter.onComplete()
                        } else {
                            val err: Exception = IllegalStateException("Failed to activate Firebase config instance")
                            emitter.onError(err)
                        }
                    } else {
                        val err: Exception = _task.exception ?: IllegalStateException("Task is not successful but the exception is null")
                        emitter.onError(err)
                    }
                }
            }
        }
    }

    fun getActivatedConfig(minimumFetchIntervalInSeconds: Long? = null): Single<FirebaseRemoteConfig> {
        return fetchAndActivate(minimumFetchIntervalInSeconds)
            .andThen(Single.fromCallable { FirebaseRemoteConfig.getInstance() })
    }

}