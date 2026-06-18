import { NativeModule, requireNativeModule } from 'expo';

import { WhatsappNotificationListenerEvents } from './WhatsappNotificationListener.types';

declare class WhatsappNotificationListenerModule extends NativeModule<WhatsappNotificationListenerEvents> {
  isPermissionGranted(): boolean;
  requestPermission(): void;
  setAnnouncementEnabled(enabled: boolean): void;
  isAnnouncementEnabled(): boolean;
  speak(text: string): void;
}

export default requireNativeModule<WhatsappNotificationListenerModule>('WhatsappNotificationListener');
