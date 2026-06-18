import { registerWebModule, NativeModule } from 'expo';

import { WhatsappNotificationListenerEvents } from './WhatsappNotificationListener.types';

class WhatsappNotificationListenerModule extends NativeModule<WhatsappNotificationListenerEvents> {
  isPermissionGranted(): boolean {
    return false;
  }
  requestPermission(): void {
    console.warn('Notification listener is not supported on web.');
  }
  setAnnouncementEnabled(enabled: boolean): void {
    // No-op on web
  }
  isAnnouncementEnabled(): boolean {
    return true;
  }
  speak(text: string): void {
    if ('speechSynthesis' in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      window.speechSynthesis.speak(utterance);
    }
  }
}

export default registerWebModule(WhatsappNotificationListenerModule, 'WhatsappNotificationListener');
