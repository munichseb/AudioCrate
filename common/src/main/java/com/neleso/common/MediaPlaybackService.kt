package com.neleso.common

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.StrictMode
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.neleso.common.data.Sound
import com.neleso.common.player.SimpleMediaPlayer
import com.neleso.common.player.SimpleMediaPlayerImpl
import com.neleso.common.player.SimpleMediaPlayerImpl.Companion.RESOURCE_ROOT_URI
import com.neleso.common.playlist.Playlist
import com.neleso.common.playlist.PlaylistImpl
import com.neleso.common.utils.MyNotificationManager
import java.net.URL
import java.util.*

class MediaPlaybackService : MediaBrowserServiceCompat() {

    private lateinit var myMediaSession: MediaSessionCompat
    private lateinit var simpleMediaPlayer: SimpleMediaPlayer
    private lateinit var playlist: Playlist<Sound>

    private var isForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        // https://trelp.datacar.io/datacar/debug.php

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val deviceModel = android.os.Build.MODEL

        playlist = PlaylistImpl(getSounds())
        myMediaSession = getMediaSession()
        sessionToken = myMediaSession.sessionToken
        simpleMediaPlayer = SimpleMediaPlayerImpl(
            player = MediaPlayer(),
            assetManager = resources.assets,
            notificationManager = MyNotificationManager(this),
            mediaSession = myMediaSession,
            playlist = playlist,
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
        myMediaSession.setCallback(simpleMediaPlayer.mediaSessionCallback)

        val userLocale = Locale.getDefault().getDisplayLanguage();
        val apiResponse = URL("https://trelp.datacar.io/callback.php?m=onCreate_AC_" + deviceModel + "_UC_" + userLocale).readText()

        setupForegroundService()
    }


    private fun setupForegroundService() {
        if (!isForeground) {
            ContextCompat.startForegroundService(
                applicationContext,
                Intent(applicationContext, this@MediaPlaybackService.javaClass)
            )
            isForeground = true
        }
    }

    private fun getMediaSession(): MediaSessionCompat {
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }

        return MediaSessionCompat(this, "MusicService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        val extras = Bundle().apply {
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
        }
        return BrowserRoot(MY_MEDIA_ROOT_ID, extras)
    }

    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentMediaId == MY_MEDIA_ROOT_ID) {
            result.sendResult(listOf(playlist.getRootItem(applicationContext)))
        } else {
            result.sendResult(playlist.toMediaItemList(applicationContext))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val apiResponse = URL("https://trelp.datacar.io/callback.php?m=onDestroy_AC").readText()
        simpleMediaPlayer.release()
    }

    private fun getSounds() = listOf(
        Sound(
            "ENGINE1",
             " -- Engine Reverb",
            getString(R.string.vehicle_sounds),
            RESOURCE_ROOT_URI + resources.getResourceEntryName(R.drawable.engine1),
            "soundscrate-car-enginerev1.mp3"
        ),
        Sound(
            "ENGINE2",
            "Engine Loop",
            getString(R.string.vehicle_sounds),
            RESOURCE_ROOT_URI + resources.getResourceEntryName(R.drawable.engine2),
            "soundscrate-engineloop2.mp3"
        ),
        Sound(
            "ENGINE3",
            "Engine V6",
            getString(R.string.vehicle_sounds),
            RESOURCE_ROOT_URI + resources.getResourceEntryName(R.drawable.engine3),
            "soundscrate-car-enginerev2.mp3"
        ),
        Sound(
            "HELI2",
            "Helicopter",
            getString(R.string.vehicle_sounds),
            RESOURCE_ROOT_URI + resources.getResourceEntryName(R.drawable.heli1),
            "soundscrate-helicopter-2.mp3"
        ),
        Sound(
            "HELILOOP2",
            "Helicopter Loop",
            getString(R.string.vehicle_sounds),
            RESOURCE_ROOT_URI + resources.getResourceEntryName(R.drawable.heli2),
            "soundscrate-helicopterloop2.mp3"
        )
    )

    companion object {
        private const val MY_MEDIA_ROOT_ID = "media_root_id_x"
        private const val CONTENT_STYLE_BROWSABLE_HINT =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_HINT =
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        // private const val CONTENT_STYLE_LIST = 2
        private const val CONTENT_STYLE_GRID = 2
    }
}