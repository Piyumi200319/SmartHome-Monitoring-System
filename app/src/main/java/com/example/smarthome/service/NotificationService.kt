package com.example.smarthome.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smarthome.models.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationService {

    private const val CHANNEL_ID =
        "SMART_HOME_NOTIFICATIONS"

    private const val CHANNEL_NAME =
        "Smart Home Notifications"

    private const val CHANNEL_DESCRIPTION =
        "Notifications from SmartHome devices"

    // ==================================================
    // NOTIFICATION LIST
    // ==================================================

    private val notifications =
        mutableListOf<Notification>()

    private var notificationId = 1000

    private lateinit var appContext: Context

    // ==================================================
    // INITIALIZE
    // ==================================================

    fun initialize(context: Context) {

        appContext =
            context.applicationContext

        createNotificationChannel()
    }

    // ==================================================
    // CREATE NOTIFICATION CHANNEL
    // ==================================================

    private fun createNotificationChannel() {

        // Notification channels are available from Android 8.0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        if (!::appContext.isInitialized) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        channel.description =
            CHANNEL_DESCRIPTION

        val manager =
            appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(
            channel
        )
    }

    // ==================================================
    // ADD NOTIFICATION
    // ==================================================

    fun addNotification(
        title: String,
        message: String
    ) {

        val time =
            SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        // Add to app notification list
        notifications.add(
            0,
            Notification(
                title = title,
                message = message,
                time = time
            )
        )

        // Show Android notification
        showSystemNotification(
            title,
            message
        )
    }

    // ==================================================
    // SHOW SYSTEM NOTIFICATION
    // ==================================================

    private fun showSystemNotification(
        title: String,
        message: String
    ) {

        if (!::appContext.isInitialized) {
            return
        }

        // Android 13+ requires POST_NOTIFICATIONS permission
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permission =
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permission !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notification =
            NotificationCompat.Builder(
                appContext,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(appContext)
            .notify(
                notificationId,
                notification
            )

        notificationId++
    }

    // ==================================================
    // GET NOTIFICATIONS
    // ==================================================

    fun getNotifications():
            MutableList<Notification> {

        return notifications
    }

    // ==================================================
    // CLEAR NOTIFICATIONS
    // ==================================================

    fun clearNotifications() {

        notifications.clear()
    }

    // ==================================================
    // GET NOTIFICATION COUNT
    // ==================================================

    fun getNotificationCount(): Int {

        return notifications.size
    }
}