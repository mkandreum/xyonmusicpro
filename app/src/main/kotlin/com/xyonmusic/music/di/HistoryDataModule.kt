package com.xyonmusic.music.di

import com.xyonmusic.music.history.HistoryLocalDataSource
import com.xyonmusic.music.history.RoomHistoryLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryDataModule {
    @Binds
    @Singleton
    abstract fun bindHistoryLocalDataSource(
        source: RoomHistoryLocalDataSource,
    ): HistoryLocalDataSource
}
