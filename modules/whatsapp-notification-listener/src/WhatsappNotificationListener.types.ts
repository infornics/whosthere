export type WhatsappNotificationEventPayload = {
  title: string;
  text: string;
  groupName: string;
  isGroup: boolean;
  packageName: string;
  timestamp: number;
};

export type WhatsappNotificationListenerEvents = {
  onNotificationReceived: (event: WhatsappNotificationEventPayload) => void;
};
