# Remote Backup Agent (Android / Kotlin)

A sideloaded Android app that enables **remote browsing and backup of images and videos** via a **Cloudflare Worker WebSocket**.

---

## 🧠 Overview

The app runs in two modes:

1. **Background (WorkManager)**
   - Periodically calls an API endpoint.
   - If the endpoint returns `active=true`, it starts the Foreground Service.

2. **Foreground (RemoteBrowseService)**
   - Maintains a **persistent WebSocket** connection to a Cloudflare Worker.
   - Responds to commands (list, stat, read, thumbnail, etc.).
   - Provides remote file access for **images and videos**.
   - Displays a persistent notification (required for long-running background tasks).

---

## ⚙️ Architecture

```
com.yourapp.remotebackup
├─ service/
│  ├─ RemoteBrowseService.kt     // Foreground WebSocket service
│  ├─ NotificationHelper.kt
│  └─ NetWatcher.kt
├─ socket/
│  ├─ WsClient.kt                // OkHttp WebSocket client
│  └─ Protocol.kt                // Command/response schema
├─ media/
│  ├─ MediaScanner.kt            // MediaStore/FS enumerator
│  ├─ ThumbLoader.kt             // Thumbnail loader
│  └─ TransferEngine.kt          // File chunking, resume, hashing
├─ data/
│  ├─ AppDb.kt                   // Room database
│  ├─ Entities.kt
│  └─ MediaDao.kt
├─ work/
│  └─ ActivationCheckWorker.kt   // Periodic API poller
└─ MainActivity.kt               // Barebones entry (optional)
```

---

## 🔑 Permissions

All permissions are granted due to sideloading.

| Permission | Purpose |
|-------------|----------|
| `MANAGE_EXTERNAL_STORAGE` | Access all files (images/videos) |
| `FOREGROUND_SERVICE` | Run persistent service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Required on Android 14+ |
| `POST_NOTIFICATIONS` | Show service notification |

---

## 💬 WebSocket Protocol

### Incoming Commands (from Worker)
| Type | Description |
|------|--------------|
| `list` | List media files (images/videos) |
| `stat` | Metadata for one item |
| `read` | Stream file chunks |
| `thumb` | Return a thumbnail (previews) |
| `hash` | SHA-256 hash for deduplication |
| `find_new` | List items added since timestamp |

### Outgoing Messages (to Worker)
| Type | Description |
|------|--------------|
| `hello` | Identify device and SDK |
| `list_result` | Media items metadata |
| `read_chunk` | Metadata for file chunk |
| Binary | Raw bytes (file data or thumbnails) |

---

## 🗂️ Media Handling

- Uses **MediaStore** for efficient, indexed access.
- Filters to `Images` and `Videos` only.
- Returns `uri`, `mime`, `size`, `dateModified`, `width/height`.
- Thumbnails generated via `ContentResolver.loadThumbnail()` (API 29+).

---

## 🧩 Foreground Service → WorkManager

- `WorkManager` periodically checks API.
- When "active" flag is true:
  ```kotlin
  startForegroundService(Intent(context, RemoteBrowseService::class.java))
  ```
- Foreground Service handles all socket and file logic.
- Optional: Offload long CPU tasks (hashing/transcoding) into `OneTimeWorkRequest`.

---

## 🔄 Reliability

- **Reconnects** via exponential backoff.
- **Ping/pong** keep-alives every 20s.
- **Connectivity-aware** (reconnect on network regain).
- **Resumable transfers** using `offset` parameter.
- **Chunked reads** (256 KiB default).

---

## 🧠 Useful Features

| Feature | Description |
|----------|--------------|
| Incremental sync | Scan for files modified since last backup |
| Deduplication | SHA-256 per media item |
| ContentObserver | Detect new images/videos while active |
| Wifi-only mode | Optional toggle |
| HEIC handling | Preserve EXIF + rotation info |
| Thumbnail cache | LRU cache on disk |
| Resumable uploads | Server can request resume offset |
| Notification actions | Pause / Resume / Stop |

---

## 🚀 MVP Checklist

- [x] WorkManager periodic check  
- [x] Foreground WebSocket service  
- [x] MediaStore scanner for images/videos  
- [x] Thumbnail generation (`loadThumbnail`)  
- [x] Chunked binary streaming  
- [x] Automatic reconnects  
- [x] Basic protocol handling  
- [x] Minimal UI and notification

---

## 🧱 Next Steps

- Implement full JSON parser (e.g. `kotlinx.serialization`).
- Build Cloudflare Worker backend to accept commands.
- Add local DB (Room) for incremental sync.
- Add hash-based deduplication.
- Build remote web UI for browsing and initiating backups.

---

## 📦 Tech Stack

- **Language:** Kotlin  
- **Networking:** OkHttp WebSocket  
- **Background Tasks:** WorkManager  
- **Storage:** MediaStore + MANAGE_EXTERNAL_STORAGE  
- **Thumbnails:** `ContentResolver.loadThumbnail()`  
- **Persistence:** Room (planned)

---

## ⚡ Example Workflow

1. WorkManager polls `https://api.example.com/active`.
2. Returns `{ "active": true }`.
3. Starts `RemoteBrowseService`.
4. Service connects to `wss://your-worker.example/ws`.
5. Worker sends `list` command.
6. Service responds with media JSON.
7. Worker requests `read` or `thumb` to back up files.

---

## 🧩 Build Notes

- Min SDK: 29 (Android 10)  
- Target SDK: 35 (Android 15)  
- Build Tools: Gradle + Android Studio Koala+  
- Signed for sideloading (no Play Store restrictions)

---

## 📸 Screenshot (future)

*(Add status badge / notification example here once UI polish is done.)*

---

**Author:** You  
**License:** Proprietary (sideloaded internal use only)
