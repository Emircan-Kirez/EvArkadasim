package com.emircankirez.mobileproject.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.activities.MyRequestsActivity

class RejectedNotificationService(
    private val context : Context
) {
    companion object{
        const val CHANNEL_ID = "rejected_notification"
    }

    fun showNotification(name: String, surname: String){
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MyRequestsActivity::class.java),
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_do_not_disturb_alt_24)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$name $surname adlı kullanıcıya gönderilen eşleşme talebiniz REDDEDİLDİ."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationID.id, notification)
    }
}