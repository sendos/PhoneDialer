package org.fossify.phone.helpers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File

class AudioNotificationHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val NOTIFICATION_AUDIO_FILENAME = "hold_notification.m4a"
        private const val NOTIFICATION_AUDIO_DIR = "notification_audio"
    }

    fun playNotificationAudio(onComplete: (() -> Unit)? = null) {
        try {
            val audioFile = getNotificationAudioFile()
            if (audioFile.exists()) {
                if (audioFile.length() > 0) {
                    playAudioFile(audioFile, onComplete)
                } else {
                    // File exists but is empty
                    android.util.Log.w("AudioNotificationHelper", "Audio file is empty: ${audioFile.absolutePath}")
                    onComplete?.invoke()
                }
            } else {
                // If no custom audio file exists, just call the completion callback immediately
                android.util.Log.w("AudioNotificationHelper", "Audio file does not exist: ${audioFile.absolutePath}")
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            // If there's any error, just call the completion callback
            android.util.Log.e("AudioNotificationHelper", "Error playing audio", e)
            onComplete?.invoke()
        }
    }

    private fun playAudioFile(audioFile: File, onComplete: (() -> Unit)?) {
        try {
            stopCurrentPlayback()
            
            android.util.Log.d("AudioNotificationHelper", "Starting playback of: ${audioFile.absolutePath}")
            
            // Get audio manager to ensure proper audio routing
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val originalMode = audioManager.mode
            
            mediaPlayer = MediaPlayer().apply {
                // Set audio attributes to play through speaker/media stream
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                
                setDataSource(audioFile.absolutePath)
                setOnCompletionListener {
                    android.util.Log.d("AudioNotificationHelper", "Playback completed")
                    // Restore original audio mode
                    try {
                        audioManager.mode = originalMode
                    } catch (e: Exception) {
                        android.util.Log.w("AudioNotificationHelper", "Failed to restore audio mode", e)
                    }
                    stopCurrentPlayback()
                    onComplete?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("AudioNotificationHelper", "MediaPlayer error: what=$what, extra=$extra")
                    // Restore original audio mode
                    try {
                        audioManager.mode = originalMode
                    } catch (e: Exception) {
                        android.util.Log.w("AudioNotificationHelper", "Failed to restore audio mode", e)
                    }
                    stopCurrentPlayback()
                    onComplete?.invoke()
                    true
                }
                prepareAsync()
                setOnPreparedListener { player ->
                    android.util.Log.d("AudioNotificationHelper", "MediaPlayer prepared, starting playback")
                    // Set audio mode to normal for media playback
                    audioManager.mode = AudioManager.MODE_NORMAL
                    // Set volume to a reasonable level
                    setVolume(1.0f, 1.0f)
                    player.start()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioNotificationHelper", "Exception in playAudioFile", e)
            stopCurrentPlayback()
            onComplete?.invoke()
        }
    }

    private fun stopCurrentPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        } finally {
            mediaPlayer = null
        }
    }

    fun getNotificationAudioFile(): File {
        val audioDir = File(context.filesDir, NOTIFICATION_AUDIO_DIR)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return File(audioDir, NOTIFICATION_AUDIO_FILENAME)
    }

    fun hasNotificationAudio(): Boolean {
        return getNotificationAudioFile().exists()
    }

    fun getNotificationAudioFileInfo(): String {
        val file = getNotificationAudioFile()
        return if (file.exists()) {
            "File exists: ${file.absolutePath}\nSize: ${file.length()} bytes\nReadable: ${file.canRead()}"
        } else {
            "File does not exist: ${file.absolutePath}"
        }
    }

    fun release() {
        stopCurrentPlayback()
        handler.removeCallbacksAndMessages(null)
    }
}
