package com.gnaupitni

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Store.log("Pembaca notifikasi terputus, mencoba tersambung lagi", false)
        NotificationListenerService.requestRebind(ComponentName(this, NotifService::class.java))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Store.log("Pembaca notifikasi aktif", true)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val e = n.extras ?: return
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val body = listOf(title, big.ifBlank { text }).filter { it.isNotBlank() }.joinToString(" | ")
        if (body.isBlank()) return
        Dispatcher.handle(applicationContext, sbn.packageName, title, body, sbn.postTime)
    }
}
