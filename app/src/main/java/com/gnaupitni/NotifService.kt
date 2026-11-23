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
        val e = n.extras ?: return
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val sub = e.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString() }.orEmpty()
        val body = listOf(title, big.ifBlank { text }, lines, sub)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" | ")
        if (body.isBlank()) return
        val summary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val mode = Store.intipMode
        if (mode == 0 || (mode == 1 && Parser.parseAmount(body) != null)) {
            Store.addSeen(
                Seen(
                    System.currentTimeMillis(), sbn.packageName,
                    if (summary) "[Ringkasan grup, tidak diproses]" else title, body,
                ),
            )
        }
        if (summary) return
        Dispatcher.handle(applicationContext, sbn.packageName, title, body, sbn.postTime)
    }
}
