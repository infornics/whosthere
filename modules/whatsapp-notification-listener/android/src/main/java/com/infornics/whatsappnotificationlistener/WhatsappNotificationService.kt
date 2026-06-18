package com.infornics.whatsappnotificationlistener

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class WhatsappNotificationService : NotificationListenerService() {
  private var tts: TextToSpeech? = null
  private var isTtsInitialized = false

  override fun onCreate() {
    super.onCreate()
    initializeTts()
  }

  private fun initializeTts() {
    tts = TextToSpeech(applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.getDefault()
        isTtsInitialized = true
      } else {
        Log.e("WhatsappService", "Failed to initialize TTS engine")
      }
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return

    val packageName = sbn.packageName
    if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
      return
    }

    // Read toggle from SharedPreferences
    val prefs = getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("announcement_enabled", true)
    if (!enabled) {
      return
    }

    val extras = sbn.notification.extras ?: return
    val title = extras.getCharSequence("android.title")?.toString() ?: ""
    val text = extras.getCharSequence("android.text")?.toString() ?: ""
    val conversationTitle = extras.getCharSequence("android.conversationTitle")?.toString() ?: ""

    val isGroup = conversationTitle.isNotEmpty()

    // Determine the text to announce
    val speakText = if (isGroup) {
      if (title.isNotEmpty() && title != conversationTitle) {
        "New message in group $conversationTitle from $title"
      } else {
        "New message in group $conversationTitle"
      }
    } else {
      if (title.isNotEmpty()) {
        "WhatsApp message from $title"
      } else {
        "New WhatsApp message"
      }
    }

    // Speak it
    if (isTtsInitialized) {
      tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "whatsapp_notif")
    } else {
      // Re-initialize and speak
      initializeTts()
    }

    // Send broadcast to App UI
    val intent = Intent("com.infornics.whosthere.WHATSAPP_NOTIFICATION")
    intent.putExtra("title", title)
    intent.putExtra("text", text)
    intent.putExtra("groupName", conversationTitle)
    intent.putExtra("isGroup", isGroup)
    intent.putExtra("packageName", packageName)
    intent.putExtra("timestamp", System.currentTimeMillis())
    intent.setPackage(this.packageName) // Ensure only our app receives this broadcast
    sendBroadcast(intent)
  }

  override fun onDestroy() {
    tts?.stop()
    tts?.shutdown()
    super.onDestroy()
  }
}
