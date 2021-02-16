package com.neleso.audiocrate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // TODO(developer): Handle FCM messages here.
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val payload = remoteMessage.data
            Log.d(TAG, "Message data payload: ${payload.getValue("command")}")

            if (payload.containsKey("command")) { // For long-running tasks (10 seconds or more up to maximum of 10 minutes) use background job
                val workerClass: Class<out Worker>
                if (payload.get("command").equals("push_car_data")) {
                    workerClass = CmdPushCarData::class.java
                } else {
                    workerClass = MyWorker::class.java
                }
                scheduleJob(workerClass)
            } else {
                // Handle message within 10 seconds
                handleNow()
            }
        } else {
            Log.d(TAG, getString(R.string.msg_no_data))
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val body = it.body
            val title = it.title
            Log.d(TAG, "Message Title: ${title} -> Notification Body: ${body}")
            if (title != null && body != null) {
                this.sendNotification(title, body)
            }
        }

        // here you can show notification, if sendNotification method below is fired.
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // send the FCM registration token to our app server
        sendRegistrationToServer(token)
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private fun scheduleJob(serviceClass: Class<out Worker>) {
        // [START dispatch_job]
        val work = OneTimeWorkRequest.Builder(serviceClass).build()

        WorkManager.getInstance(this).beginWith(work).enqueue()
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    /**
     * Persist token to backend server is crucial for accessing app via FCM
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {

        // TODO (Sebastian): Implement this method to send token to our backend server.
        var superU = "----";

        if (PreferenceManager.getDefaultSharedPreferences(this).contains("uuid")) {
            superU =  PreferenceManager.getDefaultSharedPreferences(this)
                .getString("uuid", "error").toString()
        }

        val apiResponse =
            URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + superU + "&t=sendRegistrationToServer&m=" + token).readText()

        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    /**
     * Create and show a simple notification containing the received FCM message, if you want!
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(title: String?, messageBody: String) {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId,
            "readable title",
            NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0 /* ID */, notificationBuilder.build())
    }

    companion object {

        private const val TAG = "MyFirebaseMsgService"
    }
}
