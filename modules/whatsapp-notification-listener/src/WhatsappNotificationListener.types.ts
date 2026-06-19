export type WhatsappNotificationEventPayload = {
  title: string;
  text: string;
  groupName: string;
  isGroup: boolean;
  packageName: string;
  timestamp: number;
};

export type WhatsappVoice = {
  id: string;
  name: string;
  language: string;
  country: string;
  locale: string;
};

export type WhatsappNotificationListenerEvents = {
  onNotificationReceived: (event: WhatsappNotificationEventPayload) => void;
};
