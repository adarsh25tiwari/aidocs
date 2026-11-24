import { useEffect, useRef, useState } from "react";
import Message from "./Message.jsx";

const DOC_SUGGESTIONS = ["Summarize this document", "What are the key points?", "List important dates and numbers"];
const GENERAL_SUGGESTIONS = ["Hi! What can you do?", "Explain how RAG works", "Write a short email asking for a meeting"];

export default function ChatPanel({ messages, busy, scopeLabel, hasDocuments, userName, onAsk, onStop, onClear }) {
  const [text, setText] = useState("");
  const [mode, setMode] = useState("chat"); // chat | search
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const submit = (value) => {
    const q = (value ?? text).trim();
    if (!q || busy) return;
    setText("");
    onAsk(q, mode);
  };

  const onKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  };

  return (
      <main className="chat">
        <header className="chat-header">
          <div>
            <h2>{mode === "chat" ? "AiDocs assistant" : "Semantic search"}</h2>
            <p>Scope: <b>{scopeLabel}</b></p>
          </div>
          <div className="header-actions">
            <div className="tabs">
              <button className={mode === "chat" ? "on" : ""} onClick={() => setMode("chat")}>💬 Chat</button>
              <button className={mode === "search" ? "on" : ""} onClick={() => setMode("search")}>🔎 Search chunks</button>
            </div>
            {messages.length > 0 && <button className="link" onClick={onClear}>Clear</button>}
          </div>
        </header>

        <div className="messages">
          {messages.length === 0 ? (
              <div className="welcome">
                <div className="big">🧠</div>
                <h3>{userName ? `Hi ${userName}, how can I help?` : "How can I help?"}</h3>
                <p>
                  Ask me anything. If your question matches your uploaded documents I answer from them and show the
                  sources; otherwise I answer like a normal assistant. Upload files any time from the sidebar.
                  The first answer can be slow while the local model loads.
                </p>
                <div className="chips">
                  {(hasDocuments ? [...DOC_SUGGESTIONS, ...GENERAL_SUGGESTIONS.slice(0, 1)] : GENERAL_SUGGESTIONS).map((x) => (
                      <button key={x} onClick={() => submit(x)} disabled={busy || mode !== "chat"}>{x}</button>
                  ))}
                </div>
              </div>
          ) : (
              messages.map((m) => <Message key={m.id} message={m} />)
          )}
          <div ref={endRef} />
        </div>

        <div className="composer">
        <textarea
            rows={1}
            value={text}
            placeholder={mode === "chat" ? "Ask anything…  (Enter to send, Shift+Enter for new line)" : "Type text to find similar passages…"}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={onKeyDown}
        />
          {busy && mode === "chat" ? (
              <button className="send stop" onClick={onStop}>■ Stop</button>
          ) : (
              <button className="send" onClick={() => submit()} disabled={busy || !text.trim()}>Send ➤</button>
          )}
        </div>
      </main>
  );
}
