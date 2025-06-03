package org.fossify.phone.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.phone.activities.CallActivity
import org.fossify.phone.helpers.ACCEPT_CALL
import org.fossify.phone.helpers.AudioNotificationHelper
import org.fossify.phone.helpers.CallManager
import org.fossify.phone.helpers.DECLINE_CALL
import org.fossify.phone.helpers.PLAY_NOTIFICATION_AND_ACCEPT

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACCEPT_CALL -> {
                context.startActivity(CallActivity.getStartIntent(context))
                CallManager.accept()
            }

            DECLINE_CALL -> CallManager.reject()
            
            PLAY_NOTIFICATION_AND_ACCEPT -> {
                // Accept the call first
                CallManager.accept()
                
                // Then play the notification audio
                val audioNotificationHelper = AudioNotificationHelper(context)
                audioNotificationHelper.playNotificationAudio {
                    // Audio playback completed or failed, nothing more to do
                }
            }
        }
    }
}
