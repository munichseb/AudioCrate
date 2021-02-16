package com.neleso.common.player

import android.content.res.AssetManager
import android.media.MediaPlayer
import com.neleso.common.data.Sound

fun MediaPlayer.loadSound(assetManager: AssetManager, sound: Sound) {
    val songFileDescriptor = assetManager.openFd(sound.fileName)
    stop()
    reset()
    setDataSource(
        songFileDescriptor.fileDescriptor,
        songFileDescriptor.startOffset,
        songFileDescriptor.length
    )
    songFileDescriptor.close()
    prepare()
}