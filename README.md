# SpeedRadio 🎙️

An **Audio-First Social Feed** Android app built with Jetpack Compose, MVVM, Hilt, and ExoPlayer.  
Record short audio clips (max 30 seconds), browse them in a scrollable feed, and play them with seamless single-audio enforcement.

---

## Project Structure

```
app/src/main/java/com/speedradio/app/
├── SpeedRadioApp.kt          ← @HiltAndroidApp entry point
├── MainActivity.kt           ← Single activity, sets up NavGraph
├── data/
│   └── AudioRepository.kt    ← In-memory + file-backed store (StateFlow)
├── domain/
│   └── AudioPost.kt          ← Core data model
├── player/
│   ├── AudioPlayerManager.kt ← Central singleton ExoPlayer wrapper
│   └── PlaybackService.kt    ← MediaSessionService (foreground playback)
├── di/
│   └── AppModule.kt          ← Hilt providers
├── viewmodel/
│   ├── FeedViewModel.kt      ← Feed state + playback delegation
│   └── RecordViewModel.kt    ← MediaRecorder lifecycle + timer
└── ui/
    ├── theme/Theme.kt        ← Dark color scheme
    ├── navigation/AppNavGraph.kt
    ├── feed/
    │   ├── FeedScreen.kt     ← LazyColumn feed + AudioPostCard
    │   └── WaveformAnimation.kt ← Animated bars on active card
    └── record/
        └── RecordScreen.kt   ← Permission, timer, pulsing mic button
```

---

## Architecture Decisions

### Why MVVM?
MVVM is the recommended pattern for Jetpack Compose apps. ViewModels survive configuration changes, expose state via `StateFlow` that Compose collects efficiently, and keep screens stateless and testable.

### Why a Central AudioPlayerManager?
A single `@Singleton` wrapping one `ExoPlayer` instance is the simplest way to guarantee the **one-audio-at-a-time** invariant across the whole app — regardless of which screen is currently visible. A distributed approach (one player per feed item) makes stopping previous audio complex and error-prone.

---

## How Key Problems Are Solved

### Single Audio Playback
`AudioPlayerManager.play(postId, filePath)`:
1. If the same post is already playing → **pause** (toggle).
2. If a *different* post is playing → `stop()` + `clearMediaItems()` then load and play the new one.
3. `PlaybackState(currentPostId, isPlaying)` is emitted via `StateFlow` — the UI reacts automatically.

### Scroll + Playback Interaction
ExoPlayer lives in `AudioPlayerManager`, which is a **singleton that outlives any Composable**. When a `LazyColumn` item scrolls off-screen its Composable is destroyed, but the player keeps running. The `FeedViewModel` (scoped to the NavBackStackEntry) holds the `playbackState` `StateFlow` — every visible card observes this shared state, so the correct card shows the "playing" indicator whenever it re-enters composition.

### Lifecycle Events
- **Background**: `AudioPlayerManager.pause()` is called from `FeedViewModel` which is observed inside the `RecordScreen`/`FeedScreen` with `collectAsStateWithLifecycle`. `Lifecycle.State.STARTED` gating means the flow stops when the app is backgrounded—but the simpler guarantee is that `PlaybackService` (a `MediaSessionService`) keeps the player process alive and the OS can present a media notification.
- **Return**: Audio remains paused; the user resumes manually (or you can hook `onResume` to call `playerManager.resume()`). This is the expected TikTok-like UX.

---

## Tradeoffs & Simplifications

| Decision | Rationale |
|---|---|
| In-memory repository | No database needed for MVP; adding Room is a 1-file change |
| No seek bar | Keeps feed card UI minimal; trivial to add with a `Slider` bound to `ExoPlayer.currentPosition` polled via a coroutine |
| Simple progress indicator (text) | Circular `Canvas`-drawn arc was deprioritised; the text `"N s remaining"` communicates the same info |
| `MediaRecorder` → M4A/AAC | Best quality-to-size ratio, universally supported on Android |
| No audio-focus management | Handled implicitly via `MediaSession`; explicit `AudioFocusRequest` is the next step |

---

## What Would Come Next (More Time)

1. **Audio focus handling** — `AudioFocusRequest` + ducking on incoming calls
2. **Seek bar + waveform thumbnail** — scrub position in feed card
3. **Room persistence** — survive app kill; migrate `AudioRepository` to a DAO
4. **Swipe-to-delete** — `SwipeToDismiss` on each LazyColumn item
5. **Record screen visualization** — live amplitude meter using `MediaRecorder.maxAmplitude`
6. **Playback speed control** — trivial with ExoPlayer `setPlaybackParameters`
7. **Share sheet** — `FileProvider` + `Intent.ACTION_SEND`

---

## Scaling Toward a Backend

| Layer | Current | With Backend |
|---|---|---|
| Storage | `context.filesDir` | S3 / GCS pre-signed upload |
| Feed data | `MutableStateFlow<List>` | Paging 3 + REST/gRPC |
| Playback | Local file URI | HLS stream URI (ExoPlayer supports natively) |
| Auth | None | Firebase Auth / JWT |
| Caching | None | `OkHttp` disk cache + offline fallback |

ExoPlayer's `DefaultDataSource` handles local files and HTTP streams identically — switching is a URI change, not an architecture change.

---

## Building & Running

```bash
# Open in Android Studio, or from CLI:
./gradlew assembleDebug

# Install on connected device / emulator
./gradlew installDebug

# Grant mic permission on first launch when prompted
```

**Requirements**
- Android Studio Ladybug (2024.2+) or Gradle 8.7
- JDK 17+
- Android SDK 35, minSdk 26

---

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Microphone access for recording |
| `FOREGROUND_SERVICE` | Keep `PlaybackService` alive in background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Required for `mediaPlayback` foreground service type on API 34+ |
| `POST_NOTIFICATIONS` | Media playback notification on Android 13+ |
