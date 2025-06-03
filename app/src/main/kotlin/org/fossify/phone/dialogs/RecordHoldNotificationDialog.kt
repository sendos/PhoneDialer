package org.fossify.phone.dialogs

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.fossify.commons.activities.BaseSimpleActivity
//import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.phone.R
import org.fossify.phone.databinding.DialogRecordHoldNotificationBinding
import org.fossify.phone.helpers.AudioNotificationHelper
import java.io.IOException

class RecordHoldNotificationDialog(
    val activity: BaseSimpleActivity,
    val callback: () -> Unit
) {
    private val recordAudioPermissionRequestCode = 101
    private val binding = DialogRecordHoldNotificationBinding.inflate(activity.layoutInflater)
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingTimer = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private val audioNotificationHelper = AudioNotificationHelper(activity)

    init {
        binding.apply {
            recordButton.setOnClickListener {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
            }

            deleteButton.setOnClickListener {
                deleteRecording()
            }

            playButton.setOnClickListener {
                playRecording()
            }

            updateUI()
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { _, _ -> callback() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.record_hold_notification) { dialog ->
                    dialog.setOnCancelListener { cleanup() }
                    dialog.setOnDismissListener { cleanup() }
                }
            }
    }

    private fun updateUI() {
        val hasRecording = audioNotificationHelper.hasNotificationAudio()
        
        binding.apply {
            deleteButton.beVisibleIf(hasRecording)
            playButton.beVisibleIf(hasRecording)
            
            if (isRecording) {
                recordButton.text = activity.getString(R.string.stop_recording)
                recordButton.setTextColor(activity.resources.getColor(R.color.md_red_700, activity.theme))
                statusText.text = activity.getString(R.string.recording_in_progress)
                timerText.beVisible()
            } else {
                recordButton.text = activity.getString(R.string.start_recording)
                recordButton.setTextColor(activity.getProperPrimaryColor())
                statusText.text = if (hasRecording) {
                    activity.getString(R.string.recording_exists)
                } else {
                    activity.getString(R.string.no_recording)
                }
                timerText.beGone()
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activity.handlePermission(recordAudioPermissionRequestCode) { granted ->
                if (granted) {
                    startRecording()
                } else {
                    activity.toast("Audio recording permission is required")
                }
            }
            return
        }

        try {
            val outputFile = audioNotificationHelper.getNotificationAudioFile()
            
            // Delete existing file if it exists
            if (outputFile.exists()) {
                outputFile.delete()
            }

            mediaRecorder = MediaRecorder(activity).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration(30000) // 30 seconds max
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }
                
                prepare()
                start()
            }

            isRecording = true
            recordingTimer = 0
            startTimer()
            updateUI()
            
        } catch (e: IOException) {
            activity.toast(R.string.recording_failed)
            cleanup()
        } catch (e: Exception) {
            activity.toast(R.string.recording_failed)
            cleanup()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            stopTimer()
            updateUI()
            activity.toast(R.string.recording_saved)
        } catch (e: Exception) {
            activity.toast(R.string.recording_failed)
            cleanup()
        }
    }

    private fun deleteRecording() {
        val audioFile = audioNotificationHelper.getNotificationAudioFile()
        if (audioFile.exists()) {
            audioFile.delete()
            updateUI()
            activity.toast(R.string.file_deleted)
        }
    }

    private fun playRecording() {
        // Show file info for debugging
        val fileInfo = audioNotificationHelper.getNotificationAudioFileInfo()
        activity.toast("Playing audio...\n$fileInfo")
        
        audioNotificationHelper.playNotificationAudio {
            // Playback completed
            activity.toast("Playback completed")
        }
    }

    private fun startTimer() {
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    private val timerRunnable = object : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (isRecording) {
                recordingTimer++
                val minutes = recordingTimer / 60
                val seconds = recordingTimer % 60
                binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
        stopTimer()
        audioNotificationHelper.release()
    }
}
