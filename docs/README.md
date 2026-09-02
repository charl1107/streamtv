# 📺 Stream TV

**Android TV streaming app — Stremio-style addon architecture**

A school project that builds an Android TV client consuming HTTP JSON addons, plus a Node.js addon server with Puppeteer-based video scraping.

---

## Architecture

```
┌─────────────────────┐      HTTPS (Stremio Protocol)      ┌──────────────────────┐
│   Android TV App    │ ◄──────────────────────────────────►│   Node.js Addon      │
│   Kotlin + Compose  │                                     │   Express + Puppeteer│
│   ExoPlayer (HLS)   │                                     │   ngrok tunnel       │
└─────────────────────┘                                     └──────────────────────┘
```

**Stremio Addon Protocol** — 4 JSON endpoints:

| Endpoint | Purpose |
|----------|---------|
| `GET /manifest.json` | Describes addon capabilities and catalogs |
| `GET /catalog/:type/:id.json` | Returns a list of content items |
| `GET /meta/:type/:id.json` | Returns full metadata for one item |
| `GET /stream/:type/:id.json` | Returns playable stream URLs |

---

## Backend Server

### Quick Start

```bash
cd backend
npm install
npm start
# Server runs on http://localhost:7000
```

### Development

```bash
npm run dev   # Uses nodemon for auto-reload
```

### Endpoints

```bash
# Manifest
curl http://localhost:7000/manifest.json

# Catalog (all anime)
curl http://localhost:7000/catalog/series/popular.json

# Catalog (filtered by genre)
curl http://localhost:7000/catalog/series/popular.json?genre=action

# Metadata for a specific anime
curl http://localhost:7000/meta/series/kitsu:1.json

# Stream URLs for an episode
curl http://localhost:7000/stream/series/kitsu:1:1.json

# Health check
curl http://localhost:7000/health
```

### ngrok Tunnel (for TV app access)

```bash
# Install ngrok: https://ngrok.com/download
ngrok config add-authtoken YOUR_TOKEN
ngrok http --domain=YOUR_DOMAIN.ngrok-free.app 7000
```

---

## Android TV App

**Status:** Not yet started

### Tech Stack
- Kotlin + Jetpack Compose TV (1.0.0-alpha10)
- ExoPlayer / Media3 for video playback
- Retrofit + OkHttp for networking
- Koin for dependency injection

---

## Project Structure

```
streamtv-project/
├── android-tv/           # Android TV Client (Phase 2+)
├── backend/              # Node.js Addon Server
│   ├── server.js         # Express server + 4 Stremio endpoints
│   ├── scraper.js        # Puppeteer video extraction
│   ├── package.json
│   ├── .env
│   └── .env.example
└── docs/
    └── README.md         # This file
```

---

## Sample Data

The server ships with 4 sample anime titles:

| ID | Title | Genre |
|----|-------|-------|
| `kitsu:1` | Attack on Titan | Action, Drama, Fantasy |
| `kitsu:2` | Demon Slayer | Action, Fantasy, Supernatural |
| `kitsu:3` | One Piece | Action, Adventure, Fantasy |
| `kitsu:4` | My Hero Academia | Action, Comedy, Superhero |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port conflict | Change PORT in `.env` |
| Puppeteer fails | Check system has Chrome/Chromium installed |
| CORS errors | Already handled via `cors({ origin: '*' })` |
| Cache stale | Restart server (1h TTL) or hit `/health` to check |
