import { useRef, useState } from "react";

const ACCEPT = ".pdf,.doc,.docx,.txt,.md,.csv,.html,.rtf,.xlsx,.pptx";

function formatSize(bytes) {
  if (bytes == null) return "";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1024 / 1024).toFixed(1) + " MB";
}

export default function Sidebar({
  documents, docsError, selectedDocId, onSelect, onUpload, onDelete, onRefresh, uploads,
}) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);

  const pick = (fileList) => {
    const files = Array.from(fileList || []);
    if (files.length) onUpload(files);
  };

  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="logo">🧠</span>
        <div>
          <h1>AiDocs</h1>
          <p>Chat with your documents</p>
        </div>
      </div>

      <div
        className={"dropzone" + (dragging ? " dragging" : "")}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => { e.preventDefault(); setDragging(false); pick(e.dataTransfer.files); }}
      >
        <div className="dz-icon">⬆</div>
        <strong>Upload documents</strong>
        <span>Drag &amp; drop or click · PDF, DOCX, TXT, MD…</span>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept={ACCEPT}
          hidden
          onChange={(e) => { pick(e.target.files); e.target.value = ""; }}
        />
      </div>

      {uploads.length > 0 && (
        <ul className="uploads">
          {uploads.map((u) => (
            <li key={u.id} className={u.status}>
              <span className="u-name">{u.name}</span>
              {u.status === "indexing" && <span className="u-state"><i className="spinner" /> indexing…</span>}
              {u.status === "done" && <span className="u-state ok">✓ indexed</span>}
              {u.status === "error" && <span className="u-state err" title={u.error}>✗ {u.error}</span>}
            </li>
          ))}
        </ul>
      )}

      <div className="docs-header">
        <h2>Documents ({documents.length})</h2>
        <button className="link" onClick={onRefresh}>Refresh</button>
      </div>

      {docsError && <div className="notice error">{docsError}</div>}

      <div className="doc-list">
        <button className={"doc-item" + (selectedDocId === null ? " active" : "")} onClick={() => onSelect(null)}>
          <span className="doc-icon">📚</span>
          <span className="doc-meta"><b>All documents</b><small>Search across everything</small></span>
        </button>

        {documents.map((d) => (
          <div key={d.id} className={"doc-item" + (selectedDocId === d.id ? " active" : "")} onClick={() => onSelect(d.id)}>
            <span className="doc-icon">📄</span>
            <span className="doc-meta">
              <b title={d.fileName}>{d.fileName}</b>
              <small>
                {formatSize(d.fileSize)}
                {d.totalChunks != null && ` · ${d.totalChunks} chunks`}
                {` · `}
                <em className={"status " + (d.status || "").toLowerCase()}>{d.status}</em>
              </small>
            </span>
            <button
              className="icon-btn"
              title="Delete"
              onClick={(e) => { e.stopPropagation(); onDelete(d.id); }}
            >🗑</button>
          </div>
        ))}

        {documents.length === 0 && !docsError && <p className="empty">No documents yet. Upload one to begin.</p>}
      </div>
    </aside>
  );
}
