# AiDocs Frontend (React + Vite)

Chat UI for the AiDocs Spring Boot backend: upload documents, list/select/delete them,
streaming chat with source citations, and a semantic "search chunks" mode.

## Run
1. Start Postgres + Ollama + the Spring Boot app (port **8081**).
2. In this folder:
   ```
   npm install
   npm run dev
   ```
3. Open http://localhost:5173

## How it connects
`vite.config.js` proxies every `/api/*` request to `http://localhost:8081`, so the browser
never makes a cross-origin call and **no CORS change is needed on the backend in dev**.
Basic-auth credentials come from `.env` (`VITE_API_USER` / `VITE_API_PASSWORD`, default dev/dev123).

## Backend endpoints used
| UI feature | Endpoint |
|---|---|
| Document list | GET /api/v1/documents |
| Upload | POST /api/v1/documents/upload |
| Delete | DELETE /api/v1/documents/{id} |
| Chat (streaming) | POST /api/v1/chat/stream |
| Sources + "Search chunks" | POST /api/v1/chat/search/similarity |

## Production notes
- Credentials in a frontend bundle are visible to anyone. Replace Basic auth with login + JWT before deploying.
- For a deployed build served from another origin, set `VITE_API_BASE` and enable CORS on the backend.
