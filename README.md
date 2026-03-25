# SpeedRadio 🎙️

An **Audio-First Social Feed** Android app built with Jetpack Compose, MVVM, Hilt, and ExoPlayer.  
Record short audio clips (max 30 seconds), browse them in a list, and enjoy a TikTok-style **Full-Screen Vertical Player** experience.

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
│   ├── AudioPlayerManager.kt ← Central singleton ExoPlayer wrapper (StateFlow)
│   └── PlaybackService.kt    ← MediaSessionService (Foreground Service)
├── di/
│   └── AppModule.kt          ← Hilt providers
├── viewmodel/
│   ├── FeedViewModel.kt      ← List state & card interactions
│   ├── PlayerViewModel.kt    ← Full-screen playback logic
│   └── RecordViewModel.kt    ← MediaRecorder lifecycle + timer
└── ui/
    ├── theme/Theme.kt        ← Aesthetic Dark Mode theme
    ├── navigation/AppNavGraph.kt
    ├── feed/
    │   ├── FeedScreen.kt     ← Scrolling list of audio clips
    │   └── WaveformAnimation.kt ← Animated visualizer bars
    ├── player/
    │   └── PlayerScreen.kt   ← Full-screen VerticalPager player
    └── record/
        └── RecordScreen.kt   ← Mic recording UI with permission handling
```

---

## Core Features & Architecture

### 1. Full-Screen Vertical Player
- **Trigger**: Tapping any item in the feed list opens the full-screen player.
- **Vertical Swipe**: Uses `VerticalPager` to switch between audio items effortlessly.
- **Single Playback**: Swiping to a new item automatically stops the previous playback and starts the new one via the `AudioPlayerManager` singleton.
- **Explicit Playback**: Scrolling the feed list itself does *not* auto-play. Playback only begins when a user explicitly enters the player mode.

### 2. Single Audio Entry Management
- **One Player to Rule Them All**: A centralized `@Singleton` `AudioPlayerManager` holds the only instance of `ExoPlayer`.
- **State Flow**: The player manager exposes a `PlaybackState` Flow. All UI components (the list cards and the full-screen items) observe this single source of truth to show synchronized play/pause/active states.

### 3. Lifecycle & Background Behavior
- **Intentional Foreground Service**: Implementation includes a `MediaSessionService` (`PlaybackService`). This ensures that audio playback is **intentional and persistent**:
  - **Going to Background**: Playback continues seamlessly because it is tied to an Android Foreground Service with the `mediaPlayback` type.
  - **Returning to Foreground**: The UI collect state via `collectAsStateWithLifecycle`, automatically re-reflecting the current player state without restarts, duplicate instances, or memory leaks.
- **Graceful Termination**: The player is released only when the `Application` or the `Service` is destroyed, preventing resource leaks.

---

## Technical Performance

- **Zero Glitch Scrolling**: The `VerticalPager` with `beyondViewportPageCount = 1` pre-loads the next audio file for instant playback on transition.
- **Compose Performance**: State management is handled via `snapshotFlow` within `LaunchedEffect` to react to page changes, ensuring minimum recompositions.
- **Permission Design**: `RECORD_AUDIO` is requested gracefully only when the user enters the record flow.

---

## Tradeoffs & Simplifications

| Decision | Rationale |
|---|---|
| No Feed Auto-Play | Prioritizes user intent and data savings; the full-screen player is the primary discovery tool. |
| In-Memory Repository | Simple and fast for an MVP/local file app. |
| Basic Progress Indicator | Focused on the "Audio-First Social" feel; aesthetic waveforms replace complex seek bars. |

---

## scaling & Future Scope

1. **Audio Focus Handling**: Implement ducking/pausing on phone calls or other media.
2. **Metadata Enrichment**: Support user avatars and descriptions in the vertical player.
3. **Remote Streaming**: Easily swappable to HLS/Dash streams via `MediaItem.fromUri` in the Player Manager.
4. **Offline Persistence**: Integrate Room to save the meta-data post-kill.

---

## Development Environment
- **Kotlin**: 2.0.21
- **Compose**: Foundation 1.7+ (via BOM 2024.09.03)
- **Hilt**: 2.51.1
- **Media3 (ExoPlayer)**: 1.4.1
- **Target SDK**: 35
- **Min SDK**: 26
