package com.frolo.muse.android

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat


val Context.windowManager: WindowManager?
    get() {
        return getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

val Context.clipboardManager: ClipboardManager?
    get() {
        return getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

val Context.notificationManager: NotificationManager?
    get() {
        return getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

val Context.notificationManagerCompat: NotificationManagerCompat
    get() {
        return NotificationManagerCompat.from(this)
    }

val Context.displayCompat: Display?
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: windowManager?.defaultDisplay
        } else {
            windowManager?.defaultDisplay
        }
    }