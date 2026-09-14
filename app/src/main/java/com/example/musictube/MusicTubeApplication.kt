package com.example.musictube

import android.app.Application
import com.example.musictube.data.local.database.MusicTubeDatabase
import com.example.musictube.data.repository.MusicRepository
import com.example.musictube.playback.PlaybackManager

class MusicTubeApplication : Application() {

    lateinit var database: MusicTubeDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var playbackManager: PlaybackManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = MusicTubeDatabase.getInstance(this)
        repository = MusicRepository(database)
        playbackManager = PlaybackManager(repository, this)
    }

    companion object {
        lateinit var instance: MusicTubeApplication
            private set
    }
}
