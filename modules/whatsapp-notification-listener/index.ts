import { EventEmitter } from 'expo';
import WhatsappNotificationListenerModule from './src/WhatsappNotificationListenerModule';
import { WhatsappNotificationListenerEvents } from './src/WhatsappNotificationListener.types';

const emitter = new EventEmitter<WhatsappNotificationListenerEvents>(WhatsappNotificationListenerModule);

export function addNotificationListener(listener: (event: any) => void) {
  return emitter.addListener('onNotificationReceived', listener);
}

export function isPermissionGranted(): boolean {
  return WhatsappNotificationListenerModule.isPermissionGranted();
}

export function requestPermission(): void {
  WhatsappNotificationListenerModule.requestPermission();
}

export function setAnnouncementEnabled(enabled: boolean): void {
  WhatsappNotificationListenerModule.setAnnouncementEnabled(enabled);
}

export function isAnnouncementEnabled(): boolean {
  return WhatsappNotificationListenerModule.isAnnouncementEnabled();
}

export function speak(text: string): void {
  WhatsappNotificationListenerModule.speak(text);
}

export async function getAvailableVoices(): Promise<any[]> {
  return await WhatsappNotificationListenerModule.getAvailableVoices();
}

export function setSelectedVoice(voiceName: string): void {
  WhatsappNotificationListenerModule.setSelectedVoice(voiceName);
}

export function getSelectedVoice(): string {
  return WhatsappNotificationListenerModule.getSelectedVoice();
}

export * from './src/WhatsappNotificationListener.types';
