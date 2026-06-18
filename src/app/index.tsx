import React, { useState, useEffect } from "react";
import {
  Text,
  View,
  TextInput,
  Pressable,
  ScrollView,
  Switch,
  StatusBar,
  AppState,
  Platform,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import {
  addNotificationListener,
  isPermissionGranted,
  requestPermission,
  setAnnouncementEnabled,
  isAnnouncementEnabled,
  speak,
  WhatsappNotificationEventPayload,
} from "../../modules/whatsapp-notification-listener";

export default function Index() {
  const [permissionGranted, setPermissionGranted] = useState(false);
  const [announcementActive, setAnnouncementActive] = useState(true);
  const [testName, setTestName] = useState("");
  const [notifications, setNotifications] = useState<WhatsappNotificationEventPayload[]>([]);

  // Function to check permission and update state
  const checkPermissionState = () => {
    try {
      const granted = isPermissionGranted();
      setPermissionGranted(granted);
    } catch (e) {
      console.warn("Failed to check permission:", e);
    }
  };

  // Function to load the initial toggle state
  const loadToggleState = () => {
    try {
      const active = isAnnouncementEnabled();
      setAnnouncementActive(active);
    } catch (e) {
      console.warn("Failed to load toggle state:", e);
    }
  };

  useEffect(() => {
    // Check initial states
    checkPermissionState();
    loadToggleState();

    // Listen for app coming back to foreground (e.g. after user grants permission in settings)
    const subscription = AppState.addEventListener("change", (nextAppState) => {
      if (nextAppState === "active") {
        checkPermissionState();
      }
    });

    // Setup an event listener for live notifications
    let notificationSub: any = null;
    try {
      notificationSub = addNotificationListener((event: WhatsappNotificationEventPayload) => {
        setNotifications((prev) => {
          // Prevent duplicates by checking timestamp and title
          const isDuplicate = prev.some(
            (n) => n.timestamp === event.timestamp && n.title === event.title
          );
          if (isDuplicate) return prev;
          return [event, ...prev].slice(0, 50); // Keep last 50 notifications
        });
      });
    } catch (e) {
      console.warn("Could not register notification listener:", e);
    }

    // Interval to poll permissions as fallback
    const interval = setInterval(checkPermissionState, 2000);

    return () => {
      subscription.remove();
      if (notificationSub) {
        try {
          notificationSub.remove();
        } catch (err) {
          console.warn("Error removing notification listener sub:", err);
        }
      }
      clearInterval(interval);
    };
  }, []);

  const handleToggleAnnouncements = (value: boolean) => {
    try {
      setAnnouncementEnabled(value);
      setAnnouncementActive(value);
    } catch (e) {
      console.warn("Failed to set announcement enabled:", e);
    }
  };

  const handleTestSpeech = () => {
    if (!testName.trim()) {
      speak("Please enter a name to test.");
      return;
    }
    // Simulate announcement format
    const speechText = `WhatsApp message from ${testName}`;
    speak(speechText);
  };

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
  };

  return (
    <SafeAreaView className="flex-1 bg-slate-950 px-5 pt-4">
      <StatusBar barStyle="light-content" backgroundColor="#020617" />
      
      {/* Header Section */}
      <View className="mb-6 items-center">
        <View className="bg-emerald-500/10 px-4 py-1.5 rounded-full border border-emerald-500/20 mb-2">
          <Text className="text-emerald-400 text-xs font-semibold tracking-wider">ACTIVE BACKGROUND RUNNER</Text>
        </View>
        <Text className="text-white text-3xl font-black tracking-tight text-center">
          Who's <Text className="text-violet-500">There</Text>
        </Text>
        <Text className="text-slate-400 text-sm mt-1 text-center font-medium">
          WhatsApp Caller Name Announcer
        </Text>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} className="flex-1">
        
        {/* Permission Card */}
        <View className={`p-5 rounded-3xl border mb-5 ${
          permissionGranted 
            ? 'bg-slate-900/40 border-slate-800' 
            : 'bg-violet-950/20 border-violet-500/30'
        }`}>
          <View className="flex-row items-center mb-3">
            <View className={`w-8 h-8 rounded-full items-center justify-center mr-3 ${
              permissionGranted ? 'bg-emerald-500/10' : 'bg-violet-500/10'
            }`}>
              <Text className="text-base">{permissionGranted ? "🛡️" : "⚠️"}</Text>
            </View>
            <View className="flex-1">
              <Text className="text-slate-400 text-xs uppercase font-bold tracking-wider">Service Permission</Text>
              <Text className="text-white font-bold text-base mt-0.5">
                {permissionGranted ? "Notification Access Granted" : "Notification Access Required"}
              </Text>
            </View>
            <View className={`w-2.5 h-2.5 rounded-full ${permissionGranted ? 'bg-emerald-400' : 'bg-amber-400 animate-pulse'}`} />
          </View>

          <Text className="text-slate-400 text-sm leading-relaxed mb-4">
            {permissionGranted 
              ? "The background service is running and ready to read incoming WhatsApp messages."
              : "This app needs notification access permissions to run in the background. Please click grant and toggle the option for Who's There."}
          </Text>

          {!permissionGranted && (
            <Pressable 
              onPress={requestPermission}
              className="bg-violet-600 active:bg-violet-700 py-3.5 px-5 rounded-2xl items-center justify-center border border-violet-500"
            >
              <Text className="text-white font-bold text-sm tracking-wide">Grant Notification Access</Text>
            </Pressable>
          )}
        </View>

        {/* Controller Card */}
        <View className="bg-slate-900/40 border border-slate-800 p-5 rounded-3xl mb-5">
          <View className="flex-row items-center justify-between pb-4 border-b border-slate-800/80 mb-4">
            <View className="flex-row items-center flex-1 pr-4">
              <View className="w-8 h-8 rounded-full items-center justify-center bg-violet-500/10 mr-3">
                <Text className="text-base">🎙️</Text>
              </View>
              <View>
                <Text className="text-white font-bold text-base">Announce Callers</Text>
                <Text className="text-slate-400 text-xs mt-0.5">Enable background text-to-speech</Text>
              </View>
            </View>
            <Switch
              value={announcementActive}
              onValueChange={handleToggleAnnouncements}
              trackColor={{ false: "#1e293b", true: "#8b5cf6" }}
              thumbColor={announcementActive ? "#c084fc" : "#64748b"}
            />
          </View>

          {/* Test TTS Utility */}
          <Text className="text-white font-bold text-sm mb-2">Test Voice Output</Text>
          <View className="flex-row items-center gap-2">
            <TextInput
              value={testName}
              onChangeText={setTestName}
              placeholder="e.g. John Doe, Coding Group"
              placeholderTextColor="#64748b"
              className="flex-1 bg-slate-950 border border-slate-800 text-white px-4 py-3 rounded-2xl font-medium text-sm focus:border-violet-500"
            />
            <Pressable 
              onPress={handleTestSpeech}
              className="bg-slate-800 active:bg-slate-700 h-[46px] px-5 rounded-2xl items-center justify-center border border-slate-700"
            >
              <Text className="text-violet-400 font-bold text-sm">Test</Text>
            </Pressable>
          </View>
        </View>

        {/* Recent Notifications Section */}
        <View className="mb-6">
          <View className="flex-row items-center justify-between mb-3 px-1">
            <Text className="text-white font-bold text-lg tracking-tight">Recent Interceptions</Text>
            {notifications.length > 0 && (
              <Pressable onPress={() => setNotifications([])}>
                <Text className="text-slate-500 text-xs font-semibold hover:text-violet-400">Clear History</Text>
              </Pressable>
            )}
          </View>

          {notifications.length === 0 ? (
            <View className="bg-slate-900/20 border border-dashed border-slate-800/80 p-8 rounded-3xl items-center justify-center">
              <Text className="text-3xl mb-2">💬</Text>
              <Text className="text-slate-400 text-sm font-medium text-center">
                Waiting for incoming notifications...
              </Text>
              <Text className="text-slate-600 text-xs mt-1 text-center px-4">
                Keep the app running and send a test WhatsApp message to this device to watch it live update.
              </Text>
            </View>
          ) : (
            <View className="gap-3">
              {notifications.map((item, index) => (
                <View key={index} className="bg-slate-900/40 border border-slate-800/60 p-4 rounded-2xl flex-row items-start">
                  <View className={`w-9 h-9 rounded-full items-center justify-center mr-3 ${
                    item.isGroup ? 'bg-violet-500/10' : 'bg-emerald-500/10'
                  }`}>
                    <Text className="text-sm">{item.isGroup ? "👥" : "👤"}</Text>
                  </View>
                  <View className="flex-1">
                    <View className="flex-row items-center justify-between">
                      <Text className="text-white font-bold text-sm" numberOfLines={1}>
                        {item.title}
                      </Text>
                      <Text className="text-slate-500 text-xs">
                        {formatTime(item.timestamp)}
                      </Text>
                    </View>
                    {item.isGroup && (
                      <Text className="text-violet-400 text-xs font-semibold mt-0.5">
                        Group: {item.groupName}
                      </Text>
                    )}
                    <Text className="text-slate-400 text-xs mt-1 leading-normal" numberOfLines={2}>
                      {item.text}
                    </Text>
                  </View>
                </View>
              ))}
            </View>
          )}
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}
