package com.infornics.whatsappnotificationlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.util.Locale

class WhatsappNotificationListenerModule : Module() {
  private var tts: TextToSpeech? = null
  private var receiver: BroadcastReceiver? = null
  @Volatile private var isTtsInitialized = false

  private fun applyVoice(voiceName: String) {
    if (isTtsInitialized && !voiceName.isEmpty()) {
      try {
        val voices = tts?.voices
        val voice = voices?.find { it.name == voiceName }
        if (voice != null) {
          tts?.voice = voice
        } else {
          // Fallback to setting language by parsing locale string
          val locale = Locale.forLanguageTag(voiceName.replace("_", "-"))
          tts?.language = locale
        }
      } catch (e: Exception) {
        Log.e("WhatsappModule", "Failed to apply voice: ${e.message}")
      }
    }
  }

  override fun definition() = ModuleDefinition {
    Name("WhatsappNotificationListener")

    Events("onNotificationReceived")

    OnCreate {
      val context = appContext.reactContext ?: return@OnCreate
      
      // Initialize TTS for test/app announcements
      tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
          tts?.language = Locale.getDefault()
          isTtsInitialized = true

          // Apply selected voice
          val prefs = context.getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
          val selectedVoice = prefs.getString("selected_voice", "") ?: ""
          if (selectedVoice.isNotEmpty()) {
            applyVoice(selectedVoice)
          }
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

    AsyncFunction("getAvailableVoices") { promise: Promise ->
      var retries = 30
      while (!isTtsInitialized && retries > 0) {
        try {
          Thread.sleep(100)
        } catch (e: InterruptedException) {
          // Ignore
        }
        retries--
      }
      
      if (!isTtsInitialized) {
        promise.reject("TTS_NOT_INITIALIZED", "TTS Engine is not initialized yet", null)
        return@AsyncFunction
      }

      val voicesList = mutableListOf<Map<String, Any>>()
      try {
        val voices = tts?.voices
        if (voices != null && !voices.isEmpty()) {
          for (voice in voices) {
            val locale = voice.locale
            val displayName = "${locale.getDisplayName(Locale.getDefault())} (${voice.name})"
            voicesList.add(mapOf(
              "id" to voice.name,
              "name" to displayName,
              "locale" to locale.toString(),
              "language" to locale.language,
              "country" to locale.country
            ))
          }
        } else {
          // Fallback to checking all available locales
          val locales = Locale.getAvailableLocales()
          if (locales != null) {
            for (locale in locales) {
              val status = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
              if (status == TextToSpeech.LANG_AVAILABLE || 
                  status == TextToSpeech.LANG_COUNTRY_AVAILABLE || 
                  status == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                val displayName = locale.getDisplayName(Locale.getDefault())
                val localeStr = locale.toString()
                if (voicesList.none { it["locale"] == localeStr }) {
                  voicesList.add(mapOf(
                    "id" to localeStr,
                    "name" to displayName,
                    "locale" to localeStr,
                    "language" to locale.language,
                    "country" to locale.country
                  ))
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        promise.reject("FETCH_VOICES_ERROR", "Failed to fetch voices: ${e.message}", e)
        return@AsyncFunction
      }
      promise.resolve(voicesList)
    }

    Function("getSelectedVoice") {
      val context = appContext.reactContext
      if (context != null) {
        val prefs = context.getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
        prefs.getString("selected_voice", "") ?: ""
      } else {
        ""
      }
    }

    Function("setSelectedVoice") { voiceName: String ->
      val context = appContext.reactContext
      if (context != null) {
        val prefs = context.getSharedPreferences("com.infornics.whosthere.preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_voice", voiceName).apply()
        applyVoice(voiceName)
      }
    }
  }
}
