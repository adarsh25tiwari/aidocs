// All calls to the Spring Boot backend live here. Auth = JWT (Bearer token).
const BASE = import.meta.env.VITE_API_BASE ?? "";

// Must match app.rag.chat-similarity-threshold in application-dev.properties.
// Used so the "sources" list under a chat answer matches what the backend actually fed to the model.
export const CHAT_MIN_SIMILARITY = 0.5;

// ---------- token / session storage ----------
const TOKEN_KEY = "aidocs.token";
const USER_KEY = "aidocs.user";

export const getToken = () => localStorage.getItem(TOKEN_KEY);

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

function saveSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

// true if there is a token that has not expired yet (reads the "exp" claim of the JWT)
export function hasValidSession() {
  const token = getToken();
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return !payload.exp || payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

// App.jsx registers a callback here; it is called when the backend answers 401 (token expired/invalid)
let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => {
  onUnauthorized = fn;
};

// ---------- http helpers ----------
// Documents endpoints answer { message, success, timestamp, data }.
// /auth/login answers { accessToken, user } and /auth/signup answers UserDto (not wrapped).
async function unwrap(res, { raw = false, isAuthCall = false } = {}) {
  let body = null;
  try {
    body = await res.json();
  } catch {
    /* no json body */
  }

  if (res.status === 401 && !isAuthCall) {
    clearSession();
    onUnauthorized();
    throw new Error("Session expired. Please log in again.");
  }

  if (!res.ok || (body && body.success === false)) {
    let msg;
    if (body?.message) {
      const extra =
          body.data && typeof body.data === "object" ? ` (${Object.values(body.data).join(", ")})` : "";
      msg = body.message + extra;
    } else if (res.status === 401) {
      msg = "Invalid username or password";
    } else if (res.status === 403) {
      msg = "You do not have permission to do this";
    } else {
      msg = `Request failed (${res.status})`;
    }
    throw new Error(msg);
  }
  return raw ? body : body?.data;
}

async function http(path, options = {}, { auth = true } = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (auth && token) headers.Authorization = `Bearer ${token}`;
  try {
    return await fetch(BASE + path, { ...options, headers });
  } catch (e) {
    if (e.name === "AbortError") throw e;
    throw new Error("Cannot reach the backend. Is Spring Boot running on port 8081?");
  }
}

const jsonPost = (path, payload, extra = {}, opts) =>
    http(path, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload), ...extra }, opts);

// ---------- auth ----------
export async function login(username, password) {
  const res = await jsonPost("/api/v1/auth/login", { username, password }, {}, { auth: false });
  const body = await unwrap(res, { raw: true, isAuthCall: true });
  saveSession(body.accessToken, body.user);
  return body.user;
}

export async function signup(username, email, password) {
  const res = await jsonPost("/api/v1/auth/signup", { username, email, password }, {}, { auth: false });
  return unwrap(res, { raw: true, isAuthCall: true });
}

export function logout() {
  clearSession();
}

// ---------- documents ----------
// NOTE: GET /api/v1/documents is ADMIN-only. Normal users must use /documents/user.
export async function listDocuments() {
  return unwrap(await http("/api/v1/documents/user"));
}

export async function uploadDocument(file) {
  const form = new FormData();
  form.append("file", file);
  return unwrap(await http("/api/v1/documents/upload", { method: "POST", body: form }));
}

export async function deleteDocument(id) {
  return unwrap(await http(`/api/v1/documents/${id}`, { method: "DELETE" }));
}

// ---------- chat ----------
export async function searchChunks({ query, documentId, topK = 5, similaritySearch }) {
  return unwrap(
      await jsonPost("/api/v1/chat/search/similarity", {
        query,
        documentId: documentId || null,
        topK,
        similaritySearch: similaritySearch ?? null,
      })
  );
}

// Streaming chat. Backend sends SSE events:  data:{"token":" word"} ... data:{"done":true}
export async function streamChat({ question, documentId, signal, onToken }) {
  const res = await jsonPost(
      "/api/v1/chat/stream",
      { question, documentId: documentId || null },
      { signal, headers: { "Content-Type": "application/json", Accept: "text/event-stream" } }
  );
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
