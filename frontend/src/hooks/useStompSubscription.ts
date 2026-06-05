import { useEffect, useRef } from "react";

import { createStompClient, parseStompMessage, type ParsedStompMessage } from "../api/socket";

export function useStompSubscription<T = unknown>(
  topic: string,
  onMessage: (payload: ParsedStompMessage<T>) => void,
) {
  const onMessageRef = useRef(onMessage);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!topic) {
      console.warn("[STOMP] Subscription skipped because topic is empty");
      return;
    }

    const client = createStompClient();

    client.onConnect = () => {
      console.info("[STOMP] Connected", { topic });
      client.subscribe(topic, (message) => {
        onMessageRef.current(parseStompMessage<T>(message));
      });
    };

    client.onWebSocketClose = (event) => {
      console.warn("[STOMP] WebSocket closed", {
        topic,
        code: event.code,
        reason: event.reason,
        wasClean: event.wasClean,
      });
    };

    client.activate();

    return () => {
      void client.deactivate();
    };
  }, [topic]);
}

