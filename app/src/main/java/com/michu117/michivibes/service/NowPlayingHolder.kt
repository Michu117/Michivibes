package com.michu117.michivibes.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NowPlayingHolder {
    private val _nowPlaying = MutableStateFlow(NowPlayingInfo())
    val nowPlaying: StateFlow<NowPlayingInfo> = _nowPlaying.asStateFlow()

    fun update(info: NowPlayingInfo) {
        _nowPlaying.value = info
    }

    fun reset() {
        _nowPlaying.value = NowPlayingInfo()
    }
}
