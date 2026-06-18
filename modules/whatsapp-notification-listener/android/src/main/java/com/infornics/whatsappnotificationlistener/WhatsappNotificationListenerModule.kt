package com.infornics.whatsappnotificationlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationManagerCompat
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.util.Locale

class WhatsappNotificationListenerModule : Module() {
  private var tts: TextToSpeech? = null
  private var receiver: BroadcastReceiver? = null

  override fun definition() = ModuleDefinition {
    Name("WhatsappNotificationListener")

    Events("onNotificationReceived")

    OnCreate {
      val context = appContext.reactContext ?: return@OnCreate
      
      // Initialize TTS for test/app announcements
      tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale.getDefault()
        }
      }

      // Register Broadcast Receiver to receive events from our Notification Service
      receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          if (intent?.action == "com.infornics.whosthere.WHATSAPP_NOTIFICATION") {
            val title = intent.getStringExtra("title") ?: ""
            val text = intent.getStringExtra("text") ?: ""
            val groupName = intent.getStringExtra("groupName") ?: ""
            val isGroup = intent.getBooleanExtra("isGroup", false)
            val packageName = intent.getStringExtra("packageName") ?: ""
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

            sendEvent("onNotificationReceived", mapOf(
              "title" to title,
              "text" to text,
              "groupName" to groupName,
              "isGroup" to isGroup,
              "packageName" to packageName,
              "timestamp" to timestamp
            ))
          }
        }
      }

      val filter = IntentFilter("com.infornics.whosthere.WHATSAPP_NOTIFICATION")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        context.registerReceiver(receiver, filter)
      }
    }

    OnDestroy {
      val context = appContext.reactContext
      if (context != null && receiver != null) {
        try {
          context.unregisterReceiver(receiver)
        } catch (e: Exception) {
          // Ignore receiver errors
        }
      }
      tts?.stop()
      tts?.shutdown()
    }

    Function("isPermissionGranted") {
      val context = appContext.reactContext
      if (context != null) {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        packageNames.contains(context.packageName)
      } else {
        false
      }
    }

    Function("requestPermission") {
      val context = appContext.reactContext
      if (context != null) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      }
    }

    Function("setAnnouncementEnabled") { enabled: Boolean ->
      val context = appContext.reactContext
      if (context != null) {
        val prefs = context.getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("announcement_enabled", enabled).apply()
      }
    }

    Function("isAnnouncementEnabled") {
      val context = appContext.reactContext
      if (context != null) {
        val prefs = context.getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
        prefs.getBoolean("announcement_enabled", true)
      } else {
        true
      }
    }

    Function("speak") { text: String ->
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
  }
}
