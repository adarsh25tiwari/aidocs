import { useEffect, useRef, useState } from "react";
import Message from "./Message.jsx";

const SUGGESTIONS = ["Summarize this document", "What are the key points?", "List important dates and numbers"];

export default function ChatPanel({ messages, busy, scopeLabel, hasDocuments, onAsk, onStop, onClear }) {
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
          <h2>{mode === "chat" ? "Ask your documents" : "Semantic search"}</h2>
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
            <h3>{hasDocuments ? "What would you like to know?" : "Upload a document to get started"}</h3>
            <p>
              Answers are generated from your uploaded files, with the matching passages shown as sources.
              The first answer can be slow while the local model loads.
            </p>
            {hasDocuments && (
              <div className="chips">
                {SUGGESTIONS.map((s) => (
                  <button key={s} onClick={() => submit(s)} disabled={busy}>{s}</button>
                ))}
              </div>
            )}
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
          placeholder={mode === "chat" ? "Ask a question…  (Enter to send, Shift+Enter for new line)" : "Type text to find similar passages…"}
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
