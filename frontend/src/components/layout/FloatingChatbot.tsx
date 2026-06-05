import type { FormEvent, MouseEvent as ReactMouseEvent } from "react";
import { useEffect, useRef, useState } from "react";

import { sendMcpChatMessage } from "../../api/client";

type ChatMessage = {
  id: string;
  role: "assistant" | "user";
  text: string;
  requiresConfirmation?: boolean;
  confirmationResolved?: boolean;
};

const INITIAL_CHAT_MESSAGES: ChatMessage[] = [
  {
    id: "welcome",
    role: "assistant",
    text: "설비 상태나 알림 내용을 물어보세요. 대시보드 상황을 빠르게 확인해드릴게요.",
  },
];

const MIN_CHAT_HEIGHT = 360;
const DEFAULT_CHAT_HEIGHT = 520;
const CHAT_VIEWPORT_MARGIN = 112;
const CHAT_STORAGE_KEY = "nexus-ai-chat-messages";

function loadStoredMessages(): ChatMessage[] {
  try {
    const raw = window.localStorage.getItem(CHAT_STORAGE_KEY);
    if (!raw) return INITIAL_CHAT_MESSAGES;

    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return INITIAL_CHAT_MESSAGES;

    const messages = parsed.filter((message): message is ChatMessage => (
      message &&
      typeof message === "object" &&
      typeof message.id === "string" &&
      (message.role === "assistant" || message.role === "user") &&
      typeof message.text === "string"
    ));

    return messages.length > 0 ? messages : INITIAL_CHAT_MESSAGES;
  } catch {
    return INITIAL_CHAT_MESSAGES;
  }
}

export default function FloatingChatbot() {
  const [isOpen, setIsOpen] = useState(false);
  const [messageText, setMessageText] = useState("");
  const [isSendingMessage, setIsSendingMessage] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>(loadStoredMessages);
  const [chatHeight, setChatHeight] = useState(DEFAULT_CHAT_HEIGHT);
  const messageListRef = useRef<HTMLDivElement | null>(null);
  const resizeStateRef = useRef<{
    startY: number;
    startHeight: number;
  } | null>(null);

  const resetChat = () => {
    setMessages(INITIAL_CHAT_MESSAGES);
    window.localStorage.removeItem(CHAT_STORAGE_KEY);
    setMessageText("");
    setIsSendingMessage(false);
  };

  useEffect(() => {
    window.localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
  }, [messages]);

  useEffect(() => {
    if (!isOpen) return;

    messageListRef.current?.scrollTo({
      top: messageListRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [isOpen, messages, isSendingMessage]);

  useEffect(() => {
    const handleMouseMove = (event: MouseEvent) => {
      if (!resizeStateRef.current) return;

      const maxHeight = Math.max(
        MIN_CHAT_HEIGHT,
        window.innerHeight - CHAT_VIEWPORT_MARGIN,
      );
      const nextHeight =
        resizeStateRef.current.startHeight +
        (resizeStateRef.current.startY - event.clientY);

      setChatHeight(Math.min(maxHeight, Math.max(MIN_CHAT_HEIGHT, nextHeight)));
    };

    const handleMouseUp = () => {
      resizeStateRef.current = null;
      document.body.style.userSelect = "";
      document.body.style.cursor = "";
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
      document.body.style.userSelect = "";
      document.body.style.cursor = "";
    };
  }, []);

  useEffect(() => {
    const handleResize = () => {
      const maxHeight = Math.max(
        MIN_CHAT_HEIGHT,
        window.innerHeight - CHAT_VIEWPORT_MARGIN,
      );

      setChatHeight((prev) => Math.min(prev, maxHeight));
    };

    handleResize();
    window.addEventListener("resize", handleResize);

    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const startResize = (event: ReactMouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    resizeStateRef.current = {
      startY: event.clientY,
      startHeight: chatHeight,
    };
    document.body.style.userSelect = "none";
    document.body.style.cursor = "ns-resize";
  };

  const sendChatCommand = (text: string, options?: {
    showUserMessage?: boolean;
    resolveMessageId?: string;
  }) => {
    const nextText = text.trim();
    if (!nextText || isSendingMessage) return;

    const now = Date.now();
    if (options?.resolveMessageId) {
      setMessages((prev) =>
        prev.map((message) =>
          message.id === options.resolveMessageId
            ? { ...message, confirmationResolved: true }
            : message,
        ),
      );
    }

    if (options?.showUserMessage ?? true) {
      setMessages((prev) => [
        ...prev,
        {
          id: `user-${now}`,
          role: "user",
          text: nextText,
        },
      ]);
    }

    setIsSendingMessage(true);

    void sendMcpChatMessage({ message: nextText })
      .then((result) => {
        setMessages((prev) => [
          ...prev,
          {
            id: `assistant-${now}`,
            role: "assistant",
            text: result.reply,
            requiresConfirmation: result.requiresConfirmation,
          },
        ]);
      })
      .catch((error) => {
        setMessages((prev) => [
          ...prev,
          {
            id: `assistant-error-${now}`,
            role: "assistant",
            text: error instanceof Error
              ? `MCP 요청에 실패했습니다.\n${error.message}`
              : "MCP 요청에 실패했습니다.",
          },
        ]);
      })
      .finally(() => setIsSendingMessage(false));
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextText = messageText.trim();
    if (!nextText || isSendingMessage) return;

    setMessageText("");
    sendChatCommand(nextText);
  };

  return (
    <div className="fixed bottom-5 right-5 z-[70] flex flex-col items-end gap-3 sm:bottom-6 sm:right-6">
      {isOpen && (
        <section
          className="relative flex max-h-[calc(100vh-7rem)] w-[min(calc(100vw-2.5rem),380px)] flex-col overflow-hidden rounded-lg border border-slate-700/80 bg-[#0D1117] shadow-2xl shadow-black/50"
          aria-label="AI 챗봇 대화창"
          style={{ height: chatHeight }}
        >
          <button
            type="button"
            onMouseDown={startResize}
            className="group flex h-3 shrink-0 cursor-ns-resize items-center justify-center border-b border-slate-800 bg-slate-950/80"
            aria-label="챗봇 높이 조절"
            title="드래그해서 높이 조절"
          >
            <span className="h-1 w-12 rounded-full bg-slate-700 transition-colors group-hover:bg-cyan-400" />
          </button>

          <div className="flex h-14 shrink-0 items-center justify-between border-b border-slate-800 bg-slate-950/70 px-4">
            <div className="flex min-w-0 items-center gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-cyan-500 text-slate-950 shadow-lg shadow-cyan-500/20">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M12 2a7 7 0 0 0-7 7v2.1A4 4 0 0 0 6 19h1.4a2.5 2.5 0 0 0 4.8.7h1.4A4.4 4.4 0 0 0 18 15.3V9a6 6 0 0 0-6-7Zm-3.5 9.2a1.2 1.2 0 1 1 0-2.4 1.2 1.2 0 0 1 0 2.4Zm7 0a1.2 1.2 0 1 1 0-2.4 1.2 1.2 0 0 1 0 2.4ZM9.8 15h4.4a2.2 2.2 0 0 1-4.4 0Z" />
                </svg>
              </div>
              <div className="min-w-0">
                <h2 className="truncate text-sm font-black text-white">Nexus AI</h2>
                <p className="truncate text-[10px] font-semibold text-emerald-400">Online</p>
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-1">
              <button
                type="button"
                onClick={resetChat}
                disabled={isSendingMessage}
                className="rounded-md px-2 py-1 text-[10px] font-bold text-slate-400 transition-colors hover:bg-slate-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
                aria-label="새 대화 시작"
                title="새 대화"
              >
                새 대화
              </button>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="flex h-8 w-8 items-center justify-center rounded-md text-slate-400 transition-colors hover:bg-slate-800 hover:text-white"
                aria-label="챗봇 닫기"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2" fill="none" aria-hidden="true">
                  <path d="M18 6 6 18M6 6l12 12" strokeLinecap="round" />
                </svg>
              </button>
            </div>
          </div>

          <div
            ref={messageListRef}
            className="flex-1 space-y-3 overflow-y-auto bg-[#0B0F1A] px-4 py-4 text-xs"
          >
            {messages.map((message) => {
              const isUser = message.role === "user";

              return (
                <div
                  key={message.id}
                  className={[
                    "flex w-full",
                    isUser ? "justify-end" : "justify-start",
                  ].join(" ")}
                >
                  <div className="max-w-[86%]">
                    <div
                      className={[
                        "whitespace-pre-wrap break-words rounded-lg px-3 py-2 leading-5",
                        isUser
                          ? "rounded-tr-sm bg-cyan-500 font-semibold text-slate-950"
                          : "rounded-tl-sm border border-slate-700/70 bg-slate-900 text-slate-200",
                      ].join(" ")}
                    >
                      {message.text}
                    </div>

                    {!isUser && message.requiresConfirmation && !message.confirmationResolved && (
                      <div className="mt-2 flex gap-2">
                        <button
                          type="button"
                          onClick={() => sendChatCommand("적용해줘", {
                            showUserMessage: false,
                            resolveMessageId: message.id,
                          })}
                          disabled={isSendingMessage}
                          className="rounded-md bg-cyan-500 px-3 py-1.5 text-[11px] font-black text-slate-950 transition-colors hover:bg-cyan-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-500"
                        >
                          적용
                        </button>
                        <button
                          type="button"
                          onClick={() => sendChatCommand("취소해줘", {
                            showUserMessage: false,
                            resolveMessageId: message.id,
                          })}
                          disabled={isSendingMessage}
                          className="rounded-md border border-slate-700 px-3 py-1.5 text-[11px] font-bold text-slate-300 transition-colors hover:border-slate-500 hover:bg-slate-800 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
                        >
                          취소
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
            {isSendingMessage && (
              <div className="flex justify-start">
                <div className="max-w-[86%] rounded-lg rounded-tl-sm border border-slate-700/70 bg-slate-900 px-3 py-2 text-slate-400">
                  MCP가 요청을 처리하는 중입니다...
                </div>
              </div>
            )}
          </div>

          <form
            className="flex shrink-0 items-end gap-2 border-t border-slate-800 bg-slate-950/70 p-3"
            onSubmit={handleSubmit}
          >
            <textarea
              value={messageText}
              disabled={isSendingMessage}
              onChange={(event) => setMessageText(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  event.currentTarget.form?.requestSubmit();
                }
              }}
              placeholder="예: 현재 대시보드 세팅 상태 점검해줘"
              rows={1}
              className="max-h-28 min-h-10 min-w-0 flex-1 resize-none rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-xs leading-5 text-slate-100 outline-none transition-colors placeholder:text-slate-600 focus:border-cyan-400"
              aria-label="AI 챗봇 메시지 입력"
            />
            <button
              type="submit"
              disabled={!messageText.trim() || isSendingMessage}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-cyan-500 text-slate-950 transition-colors hover:bg-cyan-400 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-500"
              aria-label="메시지 보내기"
            >
              <svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M3.4 20.4 21 12 3.4 3.6 3 10l10 2-10 2 .4 6.4Z" />
              </svg>
            </button>
          </form>
        </section>
      )}

      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex h-14 w-14 items-center justify-center rounded-full bg-cyan-500 text-slate-950 shadow-xl shadow-cyan-500/25 ring-1 ring-cyan-300/50 transition-all hover:-translate-y-0.5 hover:bg-cyan-400 focus:outline-none focus:ring-2 focus:ring-cyan-200"
        aria-label={isOpen ? "챗봇 접기" : "챗봇 열기"}
        aria-expanded={isOpen}
      >
        {isOpen ? (
          <svg width="22" height="22" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.2" fill="none" aria-hidden="true">
            <path d="M18 6 6 18M6 6l12 12" strokeLinecap="round" />
          </svg>
        ) : (
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M4 5.8A3.8 3.8 0 0 1 7.8 2h8.4A3.8 3.8 0 0 1 20 5.8v6.9a3.8 3.8 0 0 1-3.8 3.8h-4.8L7 20.4v-3.9A3.8 3.8 0 0 1 4 12.7V5.8Zm5 4.1a1.1 1.1 0 1 0 0-2.2 1.1 1.1 0 0 0 0 2.2Zm3 0a1.1 1.1 0 1 0 0-2.2 1.1 1.1 0 0 0 0 2.2Zm3 0a1.1 1.1 0 1 0 0-2.2 1.1 1.1 0 0 0 0 2.2Z" />
          </svg>
        )}
      </button>
    </div>
  );
}
