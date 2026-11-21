function clean(text, max = 320) {
  // PDF text has lots of repeated spaces/newlines
  const t = (text || "").replace(/\s+/g, " ").trim();
  return t.length > max ? t.slice(0, max) + "…" : t;
}

function Sources({ sources }) {
  if (!sources || sources.length === 0) return null;
  return (
    <details className="sources">
      <summary>📎 {sources.length} source{sources.length > 1 ? "s" : ""}</summary>
      {sources.map((s, i) => (
        <div className="source" key={i}>
          <div className="source-head">
            <b>{s.fileName}</b>
            {s.pageNumber != null && <span className="pill">page {s.pageNumber}</span>}
            {s.chunkIndex != null && <span className="pill">chunk {s.chunkIndex}</span>}
            {s.similarityScore != null && <span className="pill score">{Math.round(s.similarityScore * 100)}% match</span>}
          </div>
          <p>{clean(s.snippet)}</p>
        </div>
      ))}
    </details>
  );
}

export default function Message({ message }) {
  const { role, content, pending, streaming, error, sources } = message;
  const isUser = role === "user";
  return (
    <div className={"msg " + (isUser ? "user" : "assistant")}>
      <div className="avatar">{isUser ? "🧑" : "🧠"}</div>
      <div className="bubble">
        {pending && (
          <span className="typing"><i /><i /><i /></span>
        )}
        {content && <div className="text">{content}{streaming && <span className="cursor">▍</span>}</div>}
        {error && <div className="notice error">⚠ {error}</div>}
        <Sources sources={sources} />
      </div>
    </div>
  );
}
