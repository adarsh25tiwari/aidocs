// All calls to the Spring Boot backend live here.
const USER = import.meta.env.VITE_API_USER ?? "dev";
const PASS = import.meta.env.VITE_API_PASSWORD ?? "dev123";
const BASE = import.meta.env.VITE_API_BASE ?? "";

const authHeaders = () => ({ Authorization: "Basic " + btoa(`${USER}:${PASS}`) });

// Backend wraps everything as { message, success, timestamp, data }
async function unwrap(res) {
  let body = null;
  try {
    body = await res.json();
  } catch {
    /* no json body */
  }
  if (!res.ok || (body && body.success === false)) {
    let msg;
    if (body?.message) {
      const extra =
        body.data && typeof body.data === "object" ? ` (${Object.values(body.data).join(", ")})` : "";
      msg = body.message + extra;
    } else if (res.status === 401) {
      msg = "Unauthorized - check VITE_API_USER / VITE_API_PASSWORD in .env";
    } else {
      msg = `Request failed (${res.status})`;
    }
    throw new Error(msg);
  }
  return body?.data;
}

async function http(path, options = {}) {
  try {
    return await fetch(BASE + path, {
      ...options,
      headers: { ...authHeaders(), ...(options.headers || {}) },
    });
  } catch (e) {
    if (e.name === "AbortError") throw e;
    throw new Error("Cannot reach the backend. Is Spring Boot running on port 8081?");
  }
}

export async function listDocuments() {
  return unwrap(await http("/api/v1/documents"));
}

export async function uploadDocument(file) {
  const form = new FormData();
  form.append("file", file);
  return unwrap(await http("/api/v1/documents/upload", { method: "POST", body: form }));
}

export async function deleteDocument(id) {
  return unwrap(await http(`/api/v1/documents/${id}`, { method: "DELETE" }));
}

export async function searchChunks({ query, documentId, topK = 5 }) {
  return unwrap(
    await http("/api/v1/chat/search/similarity", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ query, documentId: documentId || null, topK }),
    })
  );
}

// Streaming chat. Backend sends SSE events:  data:{"token":" word"} ... data:{"done":true}
export async function streamChat({ question, documentId, signal, onToken }) {
  const res = await http("/api/v1/chat/stream", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ question, documentId: documentId || null }),
    signal,
  });
  if (!res.ok) await unwrap(res);

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const handleEvent = (raw) => {
    const dataLines = raw
      .split(/\r?\n/)
      .filter((l) => l.startsWith("data:"))
      .map((l) => l.slice(5));
    if (!dataLines.length) return false;
    let msg;
    try {
      msg = JSON.parse(dataLines.join("\n")); // never trim: tokens carry their own spaces
    } catch {
      return false;
    }
    if (msg.error) throw new Error(msg.error);
    if (msg.token !== undefined) onToken(msg.token);
    return msg.done === true;
  };

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop();
    for (const ev of events) {
      if (handleEvent(ev)) return;
    }
  }
  if (buffer.trim()) handleEvent(buffer);
}
