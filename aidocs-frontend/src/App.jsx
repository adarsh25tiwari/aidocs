import { useCallback, useEffect, useRef, useState } from "react";
import Sidebar from "./components/Sidebar.jsx";
import ChatPanel from "./components/ChatPanel.jsx";
import AuthPage from "./components/AuthPage.jsx";
import {
  CHAT_MIN_SIMILARITY,
  deleteDocument,
  getStoredUser,
  hasValidSession,
  listDocuments,
  logout,
  searchChunks,
  setUnauthorizedHandler,
  streamChat,
  uploadDocument,
} from "./api.js";

let nextId = 1;
const newId = () => nextId++;

export default function App() {
  // null = logged out -> login page is shown
  const [user, setUser] = useState(() => (hasValidSession() ? getStoredUser() : null));
  const [documents, setDocuments] = useState([]);
  const [docsError, setDocsError] = useState("");
  const [selectedDocId, setSelectedDocId] = useState(null); // null = all documents
  const [uploads, setUploads] = useState([]);
  const [messages, setMessages] = useState([]);
  const [busy, setBusy] = useState(false);
  const abortRef = useRef(null);

  const refreshDocuments = useCallback(async () => {
    try {
      setDocuments(await listDocuments());
      setDocsError("");
    } catch (e) {
      setDocsError(e.message);
    }
  }, []);

  function resetState() {
    setDocuments([]);
    setDocsError("");
    setSelectedDocId(null);
    setUploads([]);
    setMessages([]);
    setBusy(false);
  }

  function handleLogout() {
    abortRef.current?.abort();
    logout();
    resetState();
    setUser(null);
  }

  // when the backend returns 401 (token expired) api.js calls this
  useEffect(() => {
    setUnauthorizedHandler(() => {
      resetState();
      setUser(null);
    });
  }, []);

  // load this user's documents once logged in
  useEffect(() => {
    if (user) refreshDocuments();
  }, [user, refreshDocuments]);

  // ---------- documents ----------
  async function handleUpload(files) {
    for (const file of files) {
      const uid = newId();
      setUploads((u) => [...u, { id: uid, name: file.name, status: "indexing" }]);
      try {
        await uploadDocument(file);
        setUploads((u) => u.map((x) => (x.id === uid ? { ...x, status: "done" } : x)));
        await refreshDocuments();
      } catch (e) {
        setUploads((u) => u.map((x) => (x.id === uid ? { ...x, status: "error", error: e.message } : x)));
      }
    }
    setTimeout(() => setUploads((u) => u.filter((x) => x.status === "indexing")), 6000);
  }

  async function handleDelete(id) {
    if (!window.confirm("Delete this document and its indexed chunks?")) return;
    try {
      await deleteDocument(id);
      if (selectedDocId === id) setSelectedDocId(null);
      await refreshDocuments();
    } catch (e) {
      setDocsError(e.message);
    }
  }

  // ---------- chat ----------
  const patchMessage = (id, fn) =>
      setMessages((ms) => ms.map((m) => (m.id === id ? { ...m, ...fn(m) } : m)));

  async function handleAsk(question, mode) {
    const userMsg = { id: newId(), role: "user", content: question };
    const aiId = newId();
    setMessages((ms) => [...ms, userMsg, { id: aiId, role: "assistant", content: "", pending: true }]);
    setBusy(true);

    if (mode === "search") {
      try {
        const res = await searchChunks({ query: question, documentId: selectedDocId });
        patchMessage(aiId, () => ({
          pending: false,
          content: res.totalMatches
              ? `Found ${res.totalMatches} matching chunk${res.totalMatches > 1 ? "s" : ""}:`
              : documents.length === 0
                  ? "You haven't uploaded any documents yet, so there is nothing to search. Upload one, or switch to 💬 Chat to ask me anything."
                  : "I couldn't find passages like that in your documents. Try different words, or switch to 💬 Chat to ask me directly.",
          sources: res.matches,
        }));
      } catch (e) {
        patchMessage(aiId, () => ({ pending: false, error: e.message }));
      } finally {
        setBusy(false);
      }
      return;
    }

    const controller = new AbortController();
    abortRef.current = controller;
    try {
      await streamChat({
        question,
        documentId: selectedDocId,
        signal: controller.signal,
        onToken: (t) => patchMessage(aiId, (m) => ({ pending: false, streaming: true, content: m.content + t })),
      });
      patchMessage(aiId, () => ({ pending: false, streaming: false }));
      // Show which chunks the answer was based on
      try {
        // same threshold the backend used for chat, so a plain "hi" shows no sources
        const res = await searchChunks({
          query: question,
          documentId: selectedDocId,
          similaritySearch: selectedDocId ? undefined : CHAT_MIN_SIMILARITY,
        });
        patchMessage(aiId, () => ({ sources: res.matches }));
      } catch {
        /* sources are optional */
      }
    } catch (e) {
      if (e.name === "AbortError") {
        patchMessage(aiId, (m) => ({ pending: false, streaming: false, content: m.content || "(stopped)" }));
      } else {
        patchMessage(aiId, () => ({ pending: false, streaming: false, error: e.message }));
      }
    } finally {
      abortRef.current = null;
      setBusy(false);
    }
  }

  const selectedDoc = documents.find((d) => d.id === selectedDocId);

  if (!user) {
    return <AuthPage onLoggedIn={setUser} />;
  }

  return (
      <div className="app">
        <Sidebar
            documents={documents}
            docsError={docsError}
            selectedDocId={selectedDocId}
            onSelect={setSelectedDocId}
            onUpload={handleUpload}
            onDelete={handleDelete}
            onRefresh={refreshDocuments}
            uploads={uploads}
            user={user}
            onLogout={handleLogout}
        />
        <ChatPanel
            messages={messages}
            busy={busy}
            scopeLabel={selectedDoc ? selectedDoc.fileName : "All documents"}
            hasDocuments={documents.length > 0}
            userName={user.username}
            onAsk={handleAsk}
            onStop={() => abortRef.current?.abort()}
            onClear={() => setMessages([])}
        />
      </div>
  );
}
