# SoundBuzz — Modern Android Music Discovery & Streaming App

A production-quality, responsive Android music application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Clean Architecture (MVVM)**. SoundBuzz enables users to discover, organize, and stream music content sourced from YouTube using YouTube's permitted APIs and player mechanisms.

---

## 📑 Table of Contents
1. [Project Overview](#project-overview)
2. [Key Features](#key-features)
3. [Architecture](#architecture)
4. [Technology Stack](#technology-stack)
5. [Project Structure](#project-structure)
6. [How YouTube Integration Works](#how-youtube-integration-works)
7. [Local Database (Room)](#local-database-room)
8. [Recommendation System](#recommendation-system)
9. [How to Configure API Keys](#how-to-configure-api-keys)
10. [How to Run](#how-to-run)
11. [Testing](#testing)
12. [Limitations & YouTube Compliance](#limitations--youtube-compliance)

---

## 1. Project Overview

SoundBuzz offers a Spotify-like user experience powered by YouTube's vast catalog of music videos and audio tracks. It runs 100% serverless: all personalization, playlist storage, playback history, favorites, search history, and recommendation scoring are computed and maintained locally on the user's Android device.

---

## 2. Key Features

### A. Home Screen
- **Horizontally scrollable curated carousels**:
  - *Recently Played* (automatically surfaced when playback history exists)
  - *Recommended For You* (powered by the local recommendation engine)
  - *Trending / Popular* (global music charts)
  - *New Releases*
  - *Popular Artists* (interactive artist avatars)
  - *Music Categories* (dynamic colored gradient category cards)
  - *Hit Songs*
  - *Regional Hits & Vibes* (Bangla, Bollywood, K-Pop, etc.)
- Quick playback directly from cards with cover art, duration badges, and titles.

### B. Search Experience
- Real-time debounced search input (400ms debounce).
- Quick category filter chips (Pop, Rock, Lo-Fi, EDM, Bangla, Bollywood, etc.).
- Search history stored locally in Room with individual item deletion and one-tap clear.
- 3-dot track overflow menu on each result:
  - Play Now
  - Play Next
  - Add to Queue
  - Add to Playlist
  - Toggle Favorites
  - View Artist Details
- Full handling of Loading (shimmer skeletons), Empty, and Error states with retry capability.

### C. Music Player & Persistent Mini-Player
- **Full-Screen Player**:
  - High-resolution artwork or toggleable permitted official YouTube video playback.
  - Interactive scrubbable seekbar with elapsed and total duration.
  - Controls: Play/Pause, Previous, Next, Shuffle, and Repeat (Off, Repeat All, Repeat One).
  - Quick Favorite heart button and Add to Playlist action.
  - Interactive Queue sheet: view upcoming tracks, remove individual items, shuffle queue, or clear queue.
- **Persistent Mini-Player**:
  - Appears seamlessly above the 4-tab bottom navigation bar across all main screens.
  - Displays thumbnail, title, artist, live playback progress indicator, and quick Play/Pause and Skip buttons.
  - Tap anywhere to expand the full-screen player.

### D. Library & Playlists
- **Unlimited Local Playlists**:
  - Create, rename, and delete custom playlists.
  - Add and remove tracks.
  - "Play All" and "Shuffle" full playlists.
  - Automatically derives playlist cover artwork from added tracks.
- **Favorites Screen**:
  - Offline-accessible metadata for all favorited songs.
  - Instant one-tap "Play All" or "Shuffle" favorites.
- **Playback History**:
  - Chronological playback history stored locally (newest first, capped at 50 items).
  - Re-playing a song updates the timestamp without duplicate entries.
  - One-tap history clearing.

### E. Artist Pages & Category Hubs
- **Artist Detail**:
  - Artist avatar, subscriber count, bio, and catalog of popular and related songs.
- **Category Hubs**:
  - Explores individual genres (Pop, Rock, Hip-Hop, Rap, Classical, EDM, Lo-Fi, Jazz, K-Pop, Bollywood, Bangla, English, Workout, Relax, Romantic, Sad, Party).
  - Categorized into Popular, Trending, and Latest songs.

### F. Settings & Customization
- **Appearance**: Dark Mode (primary obsidian theme), Light Mode, or Follow System.
- **Playback**: Autoplay toggle, shuffle toggle.
- **Storage Management**: Clear cached metadata and clear playback history.
- **API Key Configuration**: In-app configuration for YouTube Data API v3 key.
- **About**: Version information, open-source licenses, and YouTube API compliance notice.

---

## 3. Architecture

SoundBuzz follows standard Android Clean Architecture with MVVM:

```
UI Layer (Jetpack Compose Screens & Components)
       ▲
       │ StateFlow / Events
       ▼
ViewModel Layer (HomeViewModel, SearchViewModel, PlayerViewModel, etc.)
       ▲
       │ Domain Models & Use Cases
       ▼
Repository Layer (MusicRepository, PlaybackManager, RecommendationEngine)
       ▲
       ├─────────────────────────────────┐
       ▼                                 ▼
Local Data Layer (Room)        Remote Data Layer (Retrofit YouTube API)
 - TrackDao                    - YouTubeApiService
 - PlaylistDao                 - DefaultMusicCatalog (Fallback & Offline)
 - FavoriteDao
 - PlaybackHistoryDao
 - SearchHistoryDao
 - UserPreferenceDao
```

- **Package Structure**:
  ```
  com.example.musictube
  ├── data
  │   ├── local
  │   │   ├── dao (TrackDao, PlaylistDao, FavoriteDao, PlaybackHistoryDao, SearchHistoryDao, UserPreferenceDao)
  │   │   ├── database (MusicTubeDatabase)
  │   │   └── entity (TrackEntity, PlaylistEntity, PlaylistTrackEntity, FavoriteEntity, etc.)
  │   ├── remote
  │   │   ├── api (YouTubeApiService, NetworkClient)
  │   │   └── model (YouTubeDto, DefaultMusicCatalog)
  │   └── repository (MusicRepository)
  ├── domain
  │   ├── model (Track, Playlist, Artist, Category, PlayerState, PlaybackState)
  │   └── usecase (RecommendationEngine)
  ├── navigation (MusicTubeNavigation, Screen)
  ├── playback (PlaybackManager)
  ├── presentation
  │   ├── artist (ArtistDetailScreen, ArtistDetailViewModel)
  │   ├── category (CategoryDetailScreen, CategoryDetailViewModel)
  │   ├── components (MiniPlayer, TrackItemRow, TrackItemCard, TrackOptionsMenu, PlaylistDialogs, SkeletonLoading, StateViews, YouTubePlayerViewContainer)
  │   ├── home (HomeScreen, HomeViewModel)
  │   ├── library (LibraryScreen, LibraryViewModel)
  │   ├── player (PlayerScreen, PlayerViewModel)
  │   ├── playlist (PlaylistDetailScreen, PlaylistDetailViewModel)
  │   ├── search (SearchScreen, SearchViewModel)
  │   ├── settings (SettingsScreen, SettingsViewModel)
  │   └── theme (Color, Theme, Type)
  └── utils (DurationUtils)
  ```

---

## 4. Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Toolkit**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Database**: AndroidX Room 2.6.1 with KSP code generation
- **Networking**: Retrofit 2.11.0, OkHttp 4.12.0, Gson
- **Image Loading**: Coil Compose 2.7.0
- **YouTube Playback Integration**: `android-youtube-player:core:12.1.1` (Permitted official IFrame-based web-view embed)
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`) with Android Gradle Plugin 8.7.3

---

## 5. How YouTube Integration Works

1. **Discovery (YouTube Data API v3)**:
   - When a YouTube Data API v3 key is supplied, search queries and charts fetch directly from Google's endpoint (`https://www.googleapis.com/youtube/v3/`).
   - ISO-8601 durations (e.g. `PT3M45S`) are parsed into seconds for seekbar accuracy.
   - Metadata is cached in the local Room database to minimize network requests.
2. **Playback (Permitted IFrame Player)**:
   - Playback is achieved using the open-source `android-youtube-player` library, which embeds the official YouTube IFrame Embed Player.
   - **No scraping, audio ripping, or downloading** is performed, adhering 100% to YouTube's Developer Policies and Terms of Service.
   - The player supports play, pause, seek, duration updates, and track completion callbacks.
3. **Graceful Fallback & Offline Catalog**:
   - If no API key is supplied or when offline, SoundBuzz serves an extensive curated music catalog (`DefaultMusicCatalog`) across 17 genres with genuine YouTube video IDs so users can immediately hit play and enjoy music.

---

## 6. Local Database (Room)

SoundBuzz utilizes Room with 7 tables:
1. `tracks`: Cached track metadata (id, videoId, title, artist, channel, thumbnailUrl, duration, category, viewCount).
2. `playlists`: User-created playlist records (id, name, createdAt, updatedAt, thumbnail).
3. `playlist_tracks`: Relational items linking tracks to playlists with position ordering.
4. `favorites`: Favorited tracks with timestamps for instant offline retrieval.
5. `playback_history`: Play history tracking (latest 50 items, deduplicated by updating `playedAt`).
6. `search_history`: Recent search queries (latest 30 unique queries).
7. `user_preferences`: Key-value storage for settings (such as active API key).

---

## 7. Recommendation System

Since SoundBuzz operates without a centralized recommendation server, it runs an on-device weighted scoring engine (`RecommendationEngine`):

| Signal | Weight | Logic |
|---|---|---|
| **Artist Match** | `+5` | User has previously listened to this artist |
| **Genre Match** | `+4` | User has listened to this genre/category |
| **Favorite Artist** | `+5` | Artist exists in user's Favorites |
| **Recent Context** | `+2` | Artist or genre appears in recent top 5 history |
| **Top Category** | `+3` | Matches user's #1 most played category |
| **Popularity Bonus**| `+2` | Track has >500M views / high popularity |
| **Already Played** | `-1` | Soft penalty to introduce discovery and freshness |

The algorithm computes the cumulative score for available tracks and ranks the list in descending order.

---

## 8. How to Configure API Keys

You can configure your YouTube Data API v3 Key in either of two ways:

### Option A: In-App via Settings (Recommended)
1. Launch SoundBuzz on your device or emulator.
2. Navigate to the **Settings** tab.
3. Under **YouTube Data API v3 Key**, enter your API key and tap **Save Key**.
4. Search and trending charts will immediately begin querying YouTube Data API v3 live.

### Option B: Via `local.properties` or `build.gradle.kts`
Add your API key into `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "DEFAULT_YOUTUBE_API_KEY", "\"YOUR_YOUTUBE_API_KEY\"")
```

---

## 9. How to Run

### Requirements:
- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 35 (compileSdk 35, minSdk 24)

### Steps:
1. Open Android Studio.
2. Select **Open** and choose the `SoundBuzz` directory.
3. Allow Gradle to sync dependencies.
4. Select your connected Android device or emulator.
5. Click **Run 'app'** (or press Shift + F10).

---

## 10. Testing

SoundBuzz includes unit tests covering:
- **`RecommendationEngineTest`**: Validates weighted scoring rules (artist match +5, genre match +4, favorite artist +5, recent bonus +2, already played -1).
- **`DurationUtilsTest`**: Validates ISO-8601 duration parsing (`PT3M45S` -> 225s) and formatting (`225s` -> `"3:45"`).
- **`EntityMappingTest`**: Validates Room entity transformations to domain models and back.

To run tests:
```bash
./gradlew testDebugUnitTest
```

---

## 11. Limitations & YouTube Compliance

- **YouTube API Quotas**: YouTube Data API v3 enforces a default quota of 10,000 units per day. Search requests consume 100 units per call. SoundBuzz caches responses to reduce quota consumption. When the quota is exceeded or an API key is omitted, the app gracefully falls back to the curated local catalog without crashing.
- **Playback Policy**: As mandated by YouTube's Terms of Service, background audio playback with the screen off or outside the YouTube player embed is not permitted. SoundBuzz plays media using the permitted official YouTube IFrame player embed and does not rip or extract raw streams.
