/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.di

import android.content.Context
import com.xyonmusic.storage.data.EncryptedNetworkServerStore
import com.xyonmusic.storage.data.NetworkMusicRepository
import com.xyonmusic.storage.domain.NetworkServerStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** SMB and WebDAV sources: one encrypted server store and one repository for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkStorageModule {

    @Provides
    @Singleton
    fun provideNetworkServerStore(@ApplicationContext context: Context): NetworkServerStore =
        EncryptedNetworkServerStore(context)

    @Provides
    @Singleton
    fun provideNetworkMusicRepository(store: NetworkServerStore): NetworkMusicRepository =
        NetworkMusicRepository(store)
}
