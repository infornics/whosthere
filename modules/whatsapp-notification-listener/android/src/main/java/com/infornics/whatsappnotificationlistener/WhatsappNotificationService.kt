package com.infornics.whatsappnotificationlistener

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class WhatsappNotificationService : NotificationListenerService() {
  private var tts: TextToSpeech? = null
  private var isTtsInitialized = false

  // Maps sbn.key to the timestamp of the last announced message in that thread
  private val lastAnnouncedTimestampMap = ConcurrentHashMap<String, Long>()
  // Maps sbn.key to the fallback string key of the last announced message
  private val lastAnnouncedFallbackMap = ConcurrentHashMap<String, String>()

  override fun onCreate() {
    super.onCreate()
    initializeTts()
  }

  private fun initializeTts() {
    tts = TextToSpeech(applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.getDefault()
        isTtsInitialized = true

        // Apply selected voice
        try {
          val prefs = getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
          val selectedVoice = prefs.getString("selected_voice", "") ?: ""
          if (selectedVoice.isNotEmpty()) {
            val voices = tts?.voices
            val voice = voices?.find { it.name == selectedVoice }
            if (voice != null) {
              tts?.voice = voice
            } else {
              // Fallback to setting language by parsing locale string
              val locale = Locale.forLanguageTag(selectedVoice.replace("_", "-"))
              tts?.language = locale
            }
          }
        } catch (e: Exception) {
          Log.e("WhatsappService", "Failed to apply voice in service: ${e.message}")
        }
      } else {
        Log.e("WhatsappService", "Failed to initialize TTS engine")
      }
    }
  }

  override fun onListenerConnected() {
    super.onListenerConnected()
    try {
      val activeNotifications = activeNotifications
      if (activeNotifications != null) {
        for (sbn in activeNotifications) {
          if (sbn == null) continue
          val packageName = sbn.packageName
          if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            // Check if it's a summary notification
            if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
              continue
            }
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
            val latestMessage = messagingStyle?.messages?.lastOrNull()
            if (latestMessage != null) {
              lastAnnouncedTimestampMap[sbn.key] = latestMessage.timestamp
            } else {
              val extras = sbn.notification.extras
              if (extras != null) {
                val rawTitle = extras.getCharSequence("android.title")?.toString() ?: ""
                val rawText = extras.getCharSequence("android.text")?.toString() ?: ""
                lastAnnouncedFallbackMap[sbn.key] = "${sbn.key}|$rawTitle|$rawText"
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e("WhatsappService", "Failed to pre-populate announced maps: ${e.message}")
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val key = sbn.key
    lastAnnouncedTimestampMap.remove(key)
    lastAnnouncedFallbackMap.remove(key)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return

    val packageName = sbn.packageName
    if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
      return
    }

    // Ignore group summary notifications
    if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
      return
    }

    // Read toggle from SharedPreferences
    val prefs = getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("announcement_enabled", true)
    if (!enabled) {
      return
    }

    val extras = sbn.notification.extras ?: return

    // Ignore generic WhatsApp titles
    val rawTitle = extras.getCharSequence("android.title")?.toString() ?: ""
    if (rawTitle.equals("WhatsApp", ignoreCase = true) || rawTitle.equals("WhatsApp Business", ignoreCase = true)) {
      return
    }

    val title: String
    val text: String
    val conversationTitle: String
    val isGroup: Boolean
    val timestamp: Long

    val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
    val latestMessage = messagingStyle?.messages?.lastOrNull()

    if (messagingStyle != null && latestMessage != null) {
      val msgTimestamp = latestMessage.timestamp
      val lastTimestamp = lastAnnouncedTimestampMap[sbn.key] ?: 0L
      if (msgTimestamp <= lastTimestamp) {
        // Already announced this message
        return
      }

      // Update last announced timestamp
      lastAnnouncedTimestampMap[sbn.key] = msgTimestamp

      // Extract details
      isGroup = messagingStyle.isGroupConversation
      conversationTitle = messagingStyle.conversationTitle?.toString() ?: ""

      @Suppress("DEPRECATION")
      val sender = latestMessage.person?.name?.toString()
        ?: latestMessage.sender?.toString()
        ?: ""

      title = sender
      text = latestMessage.text?.toString() ?: ""
      timestamp = msgTimestamp
    } else {
      // Fallback logic for non-MessagingStyle notifications
      val rawText = extras.getCharSequence("android.text")?.toString() ?: ""
      val rawConvTitle = extras.getCharSequence("android.conversationTitle")?.toString() ?: ""

      // Construct a unique key for deduplication of fallback notifications
      val fallbackKey = "${sbn.key}|$rawTitle|$rawText"
      val lastFallback = lastAnnouncedFallbackMap[sbn.key]
      if (fallbackKey == lastFallback) {
        return
      }
      lastAnnouncedFallbackMap[sbn.key] = fallbackKey

      title = rawTitle
      text = rawText
      conversationTitle = rawConvTitle
      isGroup = conversationTitle.isNotEmpty()
      timestamp = sbn.postTime
    }

    // Double check that we don't announce "WhatsApp" as the sender title
    if (title.isEmpty() || title.equals("WhatsApp", ignoreCase = true) || title.equals("WhatsApp Business", ignoreCase = true)) {
      return
    }

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
    intent.putExtra("timestamp", timestamp)
    intent.setPackage(this.packageName) // Ensure only our app receives this broadcast
    sendBroadcast(intent)
  }

  override fun onDestroy() {
    tts?.stop()
    tts?.shutdown()
    super.onDestroy()
  }
}
