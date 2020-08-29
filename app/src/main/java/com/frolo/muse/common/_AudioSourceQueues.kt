package com.frolo.muse.common

import com.frolo.muse.engine.AudioSource
import com.frolo.muse.engine.AudioSourceQueue


fun AudioSourceQueue.indexOf(predicate: (item: AudioSource) -> Boolean): Int {
    for (i in 0 until length) {
        if (predicate(getItemAt(i))) {
            return i
        }
    }
    return -1
}

fun AudioSourceQueue.findFirstOrNull(predicate: (item: AudioSource) -> Boolean): AudioSource? {
    for (i in 0 until length) {
        val item = getItemAt(i)
        if (predicate(item)) {
            return item
        }
    }
    return null
}

@Throws(NoSuchElementException::class)
fun AudioSourceQueue.find(predicate: (item: AudioSource) -> Boolean): AudioSource {
    return findFirstOrNull(predicate) ?: throw NoSuchElementException()
}