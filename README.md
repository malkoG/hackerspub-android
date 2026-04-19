# Hackers' Pub Android

An Android client for [Hackers' Pub](https://hackers.pub), a fediverse-compatible social network for developers.

## Features

- **Timeline**: View your personal home timeline with posts from people you follow
- **Explore**: Browse local and global/federated timelines
- **Notifications**: In-app and background push for follows, mentions, replies, quotes, shares, and reactions (polled by a WorkManager worker)
- **Search**: Find posts and users across the fediverse
- **Compose**: Create posts with Markdown support, mention autocomplete, and visibility controls
- **Drafts**: Save and resume in-progress posts
- **Profiles**: View user profiles and their posts; recommended-actor discovery
- **Post Details**: See full posts with replies and reactions
- **Translation**: On-device post translation via ML Kit
- **In-app Browser**: Open external links in Custom Tabs
- **Authentication**: Secure login via email verification code

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt (with `hilt-work` for WorkManager)
- **Networking**: Apollo GraphQL Client
- **Caching**: Apollo Normalized Cache with SQLite
- **Image Loading**: Coil
- **State Management**: StateFlow + ViewModel
- **Navigation**: Navigation Compose
- **Local Storage**: DataStore Preferences
- **Background Work**: WorkManager (notification polling)
- **On-device ML**: ML Kit (Translate + Language ID)
- **Crash & Analytics**: Firebase Crashlytics, Firebase Analytics
- **Auth APIs**: AndroidX Credentials (Passkey-ready, gated by `FeatureFlags`)
- **External Links**: AndroidX Browser (Custom Tabs)

## Project Structure

```
app/src/main/java/pub/hackers/android/
├── data/
│   ├── auth/           # PasskeyManager (Credentials API)
│   ├── local/          # SessionManager, PreferencesManager, NotificationStateManager
│   ├── paging/         # CursorPagingSource, PostOverlay
│   ├── repository/     # HackersPubRepository
│   └── worker/         # NotificationWorker (WorkManager)
├── di/                 # Hilt modules
├── domain/
│   └── model/          # Domain models (@Immutable)
├── navigation/         # HackersPubUrlRouter + sealed HackersPubRoute
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Screen composables & ViewModels
│   ├── theme/          # Theme, LocalAppColors
│   ├── AppViewModel.kt
│   └── HackersPubApp.kt
├── FeatureFlags.kt
├── HackersPubApplication.kt
└── MainActivity.kt
```

## Building

### Prerequisites

- A recent stable Android Studio (the project targets `compileSdk = 36`)
- JDK 17
- Android SDK 36 (`minSdk = 26`, `targetSdk = 36`)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/hackerspub-android.git
   cd hackerspub-android
   ```

2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```

3. Or open in Android Studio and run on a device/emulator.

4. (Optional) Generate GraphQL code separately:
   ```bash
   ./gradlew generateApolloSources
   ```

### Before pushing

Run lint after finishing each task and again right before `git push` to catch any new errors you introduced:

```bash
./gradlew :app:lintDebug
```

Target is **0 errors**. For a genuine false positive (e.g., AGP bug), suppress narrowly at the single site with a comment or `tools:ignore="..."` — never disable the check globally. See [CONVENTION.md §10.5](./CONVENTION.md).

## GraphQL

The app uses Apollo GraphQL to communicate with the Hackers' Pub API at `https://hackers.pub/graphql`.

- Schema: `app/src/main/graphql/pub/hackers/android/schema.graphqls`
- Operations: `app/src/main/graphql/pub/hackers/android/operations.graphql`

To update the schema:
```bash
./gradlew downloadHackerspubApolloSchemaFromIntrospection
```

## Architecture

The app follows Clean Architecture principles with three main layers:

1. **Data Layer**: Repository, GraphQL client, local storage
2. **Domain Layer**: Business models and use cases
3. **Presentation Layer**: ViewModels and Composables

### Data Flow

```
UI (Compose) → ViewModel → Repository → Apollo Client → GraphQL API
                              ↓
                        Apollo Cache (SQLite)
```

For the rules reviewers enforce during PRs — null-safety, Paging config, threading, Compose stability — see [CONVENTION.md](./CONVENTION.md).

## Screens

| Screen | Description |
|--------|-------------|
| Timeline | Personal home feed with pull-to-refresh and infinite scroll |
| Explore | Local/Global timeline tabs |
| Notifications | All notification types with appropriate icons |
| Search | Post and user search with query input |
| Settings | User info, sign out, cache management |
| Sign In | Two-step email verification flow |
| Compose | New post creation with visibility options and mention autocomplete |
| Drafts | Saved drafts list with resume/delete |
| Post Detail | Full post view with replies and reactions |
| Profile | User profile with bio and posts |
| Recommended Actors | Suggested accounts to follow |
| WebView | In-app `WebView` for opening URLs without leaving the app |

## License

[AGPL-3.0-only](./LICENSE)
