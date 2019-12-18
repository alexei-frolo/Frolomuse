package com.frolo.muse.ui.main.library.favourites

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.frolo.muse.arch.observeNonNull
import com.frolo.muse.model.media.Song
import com.frolo.muse.ui.main.AlbumArtUpdateHandler
import com.frolo.muse.ui.main.library.base.SimpleMediaCollectionFragment
import com.frolo.muse.ui.main.library.base.SongAdapter


class FavouriteSongListFragment: SimpleMediaCollectionFragment<Song>() {

    companion object {
        // Factory
        fun newIntent() = FavouriteSongListFragment()
    }

    override val viewModel: FavouriteSongListViewModel by viewModel()

    override val adapter by lazy {
        SongAdapter(Glide.with(this)).apply {
            setHasStableIds(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlbumArtUpdateHandler.attach(this) { _, _ ->
            adapter.forceResubmit()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observerViewModel(viewLifecycleOwner)
    }

    private fun observerViewModel(owner: LifecycleOwner) {
        viewModel.apply {
            isPlaying.observeNonNull(owner) { isPlaying ->
                adapter.setPlayingState(isPlaying)
            }

            playingPosition.observeNonNull(owner) { playingPosition ->
                val isPlaying = isPlaying.value ?: false
                adapter.setPlayingPositionAndState(playingPosition, isPlaying)
            }
        }
    }
}