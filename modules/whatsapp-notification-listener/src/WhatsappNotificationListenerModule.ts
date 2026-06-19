import { NativeModule, requireNativeModule } from 'expo';

import { WhatsappNotificationListenerEvents, WhatsappVoice } from './WhatsappNotificationListener.types';

declare class WhatsappNotificationListenerModule extends NativeModule<WhatsappNotificationListenerEvents> {
  isPermissionGranted(): boolean;
  requestPermission(): void;
  setAnnouncementEnabled(enabled: boolean): void;
  isAnnouncementEnabled(): boolean;
  speak(text: string): void;
  getAvailableVoices(): Promise<WhatsappVoice[]>;
  setSelectedVoice(voiceName: string): void;
  getSelectedVoice(): string;
}

export default requireNativeModule<WhatsappNotificationListenerModule>('WhatsappNotificationListener');
