import { useCallback, useState } from "react";

import { useStompSubscription } from "../hooks/useStompSubscription";
import type { ParsedStompMessage } from "../api/socket";

const DEFAULT_TOPIC = "/topic/user/1/widgets";

export default function WebSocketTest() {
  const [topic, setTopic] = useState(DEFAULT_TOPIC);
  const [activeTopic, setActiveTopic] = useState(DEFAULT_TOPIC);
  const [messages, setMessages] = useState<ParsedStompMessage[]>([]);

  const handleMessage = useCallback((payload: ParsedStompMessage) => {
    console.log("[WebSocketTest] Message received", payload);
    setMessages((prev) => [payload, ...prev].slice(0, 20));
  }, []);

  useStompSubscription(activeTopic, handleMessage);

  return (
    <section className="mx-auto flex w-full max-w-5xl flex-col gap-4 p-4 md:p-6">
      <div className="rounded-lg border border-slate-800 bg-slate-950 p-4">
        <h2 className="text-base font-bold text-white">WebSocket Test</h2>
        <div className="mt-4 flex flex-col gap-3 md:flex-row">
          <input
            value={topic}
            onChange={(event) => setTopic(event.target.value)}
            className="min-h-10 flex-1 rounded-lg border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100 outline-none focus:border-cyan-400"
          />
          <button
            type="button"
            onClick={() => setActiveTopic(topic)}
            className="rounded-lg bg-cyan-600 px-4 py-2 text-sm font-bold text-white transition-colors hover:bg-cyan-500"
          >
            구독
          </button>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Active topic: <span className="font-mono text-cyan-300">{activeTopic}</span>
        </p>
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-950 p-4">
        <h3 className="text-sm font-bold text-slate-100">Received Messages</h3>
        <pre className="mt-4 max-h-[560px] overflow-auto rounded-lg border border-slate-800 bg-slate-900 p-4 text-xs leading-relaxed text-slate-200">
          {messages.length > 0
            ? JSON.stringify(
                messages.map(({ body, rawBody, parseError }) => ({ body, rawBody, parseError })),
                null,
                2,
              )
            : "No messages received yet."}
        </pre>
      </div>
    </section>
  );
}
