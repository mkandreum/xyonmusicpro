package com.xyonmusic.music.history

import com.xyonmusic.music.db.entities.EventWithSong
import kotlinx.coroutines.flow.Flow

interface HistoryLocalDataSource {
    suspend fun loadEvents(limit: Int, offset: Int, hideVideoSongs: Boolean, query: String): List<EventWithSong>
    fun observeEventCount(): Flow<Int>
}
