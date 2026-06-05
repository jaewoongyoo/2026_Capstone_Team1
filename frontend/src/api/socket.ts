import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import { getAccessToken } from "../utils/Auth";

const configuredWsUrl = import.meta.env.VITE_WS_URL?.trim();

export const WS_URL = configuredWsUrl || "/ws-stomp";
export const DEFAULT_USER_ID = import.meta.env.VITE_USER_ID ?? "1";
export const DEFAULT_EQUIPMENT_ID = import.meta.env.VITE_EQUIPMENT_ID;
export const DEFAULT_EQUIPMENT_ENTITY_ID = import.meta.env.VITE_EQUIPMENT_ENTITY_ID;
export const EQUIPMENT_TOPIC_MODE =
  import.meta.env.VITE_EQUIPMENT_TOPIC_MODE ?? "user-widgets";
export const EQUIPMENT_TOPIC = import.meta.env.VITE_EQUIPMENT_TOPIC;

export type EquipmentTopicMode =
  | "user-widgets"
  | "equipment-widgets"
  | "equipment-entity-widgets"
  | "equipment";

export type EquipmentTopicOptions = {
  topic?: string;
  mode?: EquipmentTopicMode;
  userId?: string | number;
  equipmentId?: string | number;
  equipmentEntityId?: string | number;
};

export type ParsedStompMessage<T = unknown> = {
  body: T | string;
  rawBody: string;
  parseError?: unknown;
  message: IMessage;
};

export function parseStompMessage<T = unknown>(message: IMessage): ParsedStompMessage<T> {
  try {
    return {
      body: JSON.parse(message.body) as T,
      rawBody: message.body,
      message,
    };
  } catch (parseError) {
    console.warn("[STOMP] Failed to parse message body as JSON", {
      body: message.body,
      parseError,
    });

    return {
      body: message.body,
      rawBody: message.body,
      parseError,
      message,
    };
  }
}

export function createStompClient() {
  const accessToken = getAccessToken();

  return new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
    reconnectDelay: 5000,
    debug: (message) => {
      console.debug("[STOMP]", message);
    },
    onWebSocketError: (error) => {
      console.error("[STOMP] WebSocket transport error. Check endpoint, CORS, or network policy.", error);
    },
    onStompError: (frame) => {
      console.error("[STOMP] Broker error", {
        message: frame.headers.message,
        body: frame.body,
        headers: frame.headers,
      });
    },
    onDisconnect: () => {
      console.info("[STOMP] Disconnected");
    },
  });
}

export const equipmentTopics = {
  userWidgets: (userId: string | number) => `/topic/user/${userId}/widgets`,
  equipmentWidgets: (equipmentId: string | number) => `/topic/equipment/${equipmentId}/widgets`,
  equipmentEntityWidgets: (equipmentEntityId: string | number) =>
    `/topic/equipment-id/${equipmentEntityId}/widgets`,
  equipment: (equipmentId: string | number) => `/topic/equipment/${equipmentId}`,
};

export function resolveEquipmentTopic(options: EquipmentTopicOptions = {}) {
  if (options.topic) return options.topic;
  if (EQUIPMENT_TOPIC) return EQUIPMENT_TOPIC;

  const mode = options.mode ?? (EQUIPMENT_TOPIC_MODE as EquipmentTopicMode);

  switch (mode) {
    case "equipment-widgets": {
      const equipmentId = options.equipmentId ?? DEFAULT_EQUIPMENT_ID;
      if (!equipmentId) {
        throw new Error("Missing equipmentId for /topic/equipment/{equipmentId}/widgets");
      }
      return equipmentTopics.equipmentWidgets(equipmentId);
    }
    case "equipment-entity-widgets": {
      const equipmentEntityId = options.equipmentEntityId ?? DEFAULT_EQUIPMENT_ENTITY_ID;
      if (!equipmentEntityId) {
        throw new Error("Missing equipmentEntityId for /topic/equipment-id/{equipmentEntityId}/widgets");
      }
      return equipmentTopics.equipmentEntityWidgets(equipmentEntityId);
    }
    case "equipment": {
      const equipmentId = options.equipmentId ?? DEFAULT_EQUIPMENT_ID;
      if (!equipmentId) {
        throw new Error("Missing equipmentId for /topic/equipment/{equipmentId}");
      }
      return equipmentTopics.equipment(equipmentId);
    }
    case "user-widgets":
    default:
      return equipmentTopics.userWidgets(options.userId ?? DEFAULT_USER_ID);
  }
}

export function subscribeToTopic<T = unknown>(
  topic: string,
  onMessage: (payload: ParsedStompMessage<T>) => void,
): { client: Client; unsubscribe: () => void } {
  const client = createStompClient();
  let subscription: StompSubscription | undefined;

  client.onConnect = () => {
    console.info("[STOMP] Connected", { topic });
    subscription = client.subscribe(topic, (message) => {
      onMessage(parseStompMessage<T>(message));
    });
  };

  client.onWebSocketClose = (event) => {
    console.warn("[STOMP] WebSocket closed", {
      code: event.code,
      reason: event.reason,
      wasClean: event.wasClean,
    });
  };

  client.activate();

  return {
    client,
    unsubscribe: () => {
      subscription?.unsubscribe();
      void client.deactivate();
    },
  };
}

export function subscribeToEquipmentGateway<T = unknown>(
  onMessage: (payload: ParsedStompMessage<T>) => void,
  options: EquipmentTopicOptions = {},
) {
  const topic = resolveEquipmentTopic(options);
  return subscribeToTopic<T>(topic, onMessage);
}
