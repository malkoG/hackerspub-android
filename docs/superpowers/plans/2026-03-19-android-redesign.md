# Hackers'Pub Android — Full UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Material Design 3 defaults with a custom Ivory-inspired design system (Deep Stone accent, near-monochrome, typography-first) across all screens.

**Architecture:** Pure UI-layer transformation. No changes to ViewModels, repositories, data layer, or navigation graph. New design tokens (colors, typography, shapes) defined as Compose objects with `CompositionLocal` access. M3 components replaced with custom composables where they deviate from the spec.

**Tech Stack:** Kotlin, Jetpack Compose, Material3 (selectively — keeping M3 under the hood where compatible), Coil for images.

**Design Spec:** `docs/superpowers/specs/2026-03-19-android-redesign-design.md`

---

## File Structure

### New Files
| File | Responsibility |
|------|---------------|
| `ui/theme/Colors.kt` | Custom color token objects (light/dark), `LocalAppColors` CompositionLocal |
| `ui/theme/AppTypography.kt` | Custom `TextStyle` definitions, `LocalAppTypography` CompositionLocal |
| `ui/theme/AppShapes.kt` | Shape token constants (radii, sizes) |
| `ui/components/LargeTitleHeader.kt` | Ivory-style large title header replacing CompactTopBar |
| `ui/components/BottomNavBar.kt` | Custom bottom navigation bar with notification dot |

### Modified Files
| File | What Changes |
|------|-------------|
| `ui/theme/Theme.kt` | Wire new color/typography CompositionLocals into HackersPubTheme |
| `ui/theme/Spacing.kt` | No changes needed (values already match spec) |
| `ui/HackersPubApp.kt` | Replace `NavigationBar` with `BottomNavBar`, update tab config |
| `ui/components/PostCard.kt` | Remove Card elevation, new spacing/colors/typography, update engagement bar |
| `ui/components/HtmlContent.kt` | Update link/mention colors to use `AppColors` |
| `ui/components/CompactTopBar.kt` | Delete (replaced by LargeTitleHeader) |
| `ui/components/MentionAutocomplete.kt` | Update colors/sizing to use design tokens |
| `ui/components/ReactionPicker.kt` | Update colors/sizing to use design tokens |
| `ui/screens/timeline/TimelineScreen.kt` | Replace CompactTopBar + FAB with LargeTitleHeader |
| `ui/screens/notifications/NotificationsScreen.kt` | New header, monochrome notification icons, new avatar sizing |
| `ui/screens/explore/ExploreScreen.kt` | New header, restyle tabs |
| `ui/screens/search/SearchScreen.kt` | New header, restyle search bar and results |
| `ui/screens/settings/SettingsScreen.kt` | New header, restyle list items |
| `ui/screens/profile/ProfileScreen.kt` | Centered layout, new sizing, pill buttons |
| `ui/screens/postdetail/PostDetailScreen.kt` | New header, restyle engagement stats/action bar/sheets |
| `ui/screens/compose/ComposeScreen.kt` | New navigation bar header, pill post button |
| `ui/screens/auth/SignInScreen.kt` | Restyle fields and buttons to spec |
| `MainActivity.kt` | Update status bar / system bars color for new background |

All paths are relative to `app/src/main/java/pub/hackers/android/`.

---

## Task 1: Design System — Colors

**Files:**
- Create: `ui/theme/Colors.kt`
- Modify: `ui/theme/Theme.kt`

- [ ] **Step 1: Create `Colors.kt` with light and dark color token objects**

```kotlin
package pub.hackers.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textBody: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentMuted: Color,
    val divider: Color,
    val buttonOutline: Color,
    val reaction: Color,
    val share: Color,
)

val LightAppColors = AppColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F7),
    textPrimary = Color(0xFF1C1C1E),
    textBody = Color(0xFF3A3A3C),
    textSecondary = Color(0xFFAEAEB2),
    accent = Color(0xFF44403C),
    accentMuted = Color(0xFF78716C),
    divider = Color(0xFFF2F2F7),
    buttonOutline = Color(0xFFD6D3D1),
    reaction = Color(0xFFE8453C),
    share = Color(0xFF34D399),
)

val DarkAppColors = AppColorScheme(
    background = Color(0xFF171717),
    surface = Color(0xFF262626),
    textPrimary = Color(0xFFFAFAFA),
    textBody = Color(0xFFD4D4D4),
    textSecondary = Color(0xFF737373),
    accent = Color(0xFFD6D3D1),
    accentMuted = Color(0xFFA8A29E),
    divider = Color(0xFF262626),
    buttonOutline = Color(0xFF525252),
    reaction = Color(0xFFE8453C),
    share = Color(0xFF34D399),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
```

- [ ] **Step 2: Update `Theme.kt` to provide `LocalAppColors`**

Keep the existing `MaterialTheme` wrapper (needed for M3 components that still exist like `ModalBottomSheet`, `Switch`, `Slider`) but add `CompositionLocalProvider` for `LocalAppColors`. Update the M3 `colorScheme` to map to the new tokens so M3 components pick up our colors automatically.

Key changes in `HackersPubTheme`:
- Select `LightAppColors` or `DarkAppColors` based on `isSystemInDarkTheme()`
- Map them into a `lightColorScheme()` / `darkColorScheme()` so M3 components still function
- Wrap `MaterialTheme` content in `CompositionLocalProvider(LocalAppColors provides colors)`
- Update status bar color to use `colors.background`

- [ ] **Step 3: Verify the app builds and runs with no visual regression**

Run: `./gradlew assembleDebug`

At this point colors are provided but nothing consumes them yet — the app should look identical.

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "feat: add custom color token system (AppColorScheme)"
```

---

## Task 2: Design System — Typography

**Files:**
- Create: `ui/theme/AppTypography.kt`
- Modify: `ui/theme/Theme.kt`

- [ ] **Step 1: Create `AppTypography.kt` with all text styles**

```kotlin
package pub.hackers.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppTextStyles(
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLargeSemiBold: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val caption: TextStyle,
    val tabLabel: TextStyle,
)

val AppTypographyDefaults = AppTextStyles(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    bodyLargeSemiBold = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    tabLabel = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
)

val LocalAppTypography = staticCompositionLocalOf { AppTypographyDefaults }
```

- [ ] **Step 2: Provide `LocalAppTypography` in `HackersPubTheme`**

Add `LocalAppTypography provides AppTypographyDefaults` to the existing `CompositionLocalProvider` block. Ensure the existing `LocalFontScale` mechanism is preserved — font scaling is applied at the composable level, not in the type definitions.

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "feat: add custom typography token system (AppTextStyles)"
```

---

## Task 3: Design System — Shapes

**Files:**
- Create: `ui/theme/AppShapes.kt`

- [ ] **Step 1: Create `AppShapes.kt` with size and radius constants**

```kotlin
package pub.hackers.android.ui.theme

import androidx.compose.ui.unit.dp

object AppShapes {
    // Avatar sizes
    val avatarTimeline = 42.dp
    val avatarProfile = 80.dp
    val avatarRepost = 16.dp
    val avatarSmall = 28.dp
    val avatarQuoted = 32.dp
    val avatarNotification = 42.dp

    // Button shapes
    val pillRadius = 20.dp
    val pillHeight = 36.dp
    val iconButtonSize = 28.dp

    // Content shapes
    val quotedPostRadius = 8.dp
    val mediaRadius = 8.dp
    val searchBarRadius = 12.dp
    val reactionPillRadius = 16.dp
    val tagRadius = 100.dp // full capsule

    // Navigation
    val bottomNavHeight = 56.dp
    val notificationDot = 8.dp

    // FAB
    val fabSize = 56.dp
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```
git add -A && git commit -m "feat: add shape token constants (AppShapes)"
```

---

## Task 4: Core Component — LargeTitleHeader

**Files:**
- Create: `ui/components/LargeTitleHeader.kt`
- Delete: `ui/components/CompactTopBar.kt` (after all screens migrated — defer deletion to final cleanup)

- [ ] **Step 1: Create `LargeTitleHeader.kt`**

A composable that renders:
- Large bold title (22sp/700) on the left
- Optional trailing content (icons, avatar) on the right
- No bottom border
- Background: `AppColors.background`
- Padding: 12dp vertical, 16dp horizontal

```kotlin
@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
)
```

Implementation:
- `Row` with `verticalAlignment = CenterVertically`
- `Text` with `LocalAppTypography.current.titleLarge` and `LocalAppColors.current.textPrimary`
- `Spacer(Modifier.weight(1f))` between title and trailing
- Trailing content in a `Row` with `horizontalArrangement = spacedBy(16.dp)`

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```
git add -A && git commit -m "feat: add LargeTitleHeader composable"
```

---

## Task 5: Core Component — BottomNavBar

**Files:**
- Create: `ui/components/BottomNavBar.kt`
- Modify: `ui/HackersPubApp.kt`

- [ ] **Step 1: Create `BottomNavBar.kt`**

Custom bottom navigation bar matching spec:
- Height: 56dp
- Background: `AppColors.background`
- Top border: 1dp `AppColors.divider`
- Each tab: icon (22dp) + label (tabLabel style), 3dp gap
- Active: `AppColors.accent`, Inactive: `AppColors.textSecondary`
- Support for notification dot overlay on Alerts tab

```kotlin
@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
)

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val hasNotificationDot: Boolean = false,
)
```

- [ ] **Step 2: Update `HackersPubApp.kt`**

Replace the M3 `NavigationBar` + `NavigationBarItem` block with `BottomNavBar`. Update tab configuration:
- Authenticated: Home, Explore, Alerts, Search (4 tabs, Settings removed from bottom nav — accessible from header or profile)
- Unauthenticated: keep existing pattern but with new component

Key changes:
- Remove `NavigationBar` / `NavigationBarItem` imports
- Define `BottomNavItem` list based on auth state
- Replace the `bottomBar` lambda in `Scaffold` with `BottomNavBar`
- Settings tab moves to a gear icon in the Timeline header trailing content

**Important:** The current app has 5 tabs (Timeline, Notifications, Explore, Search, Settings) when authenticated. The spec calls for 4 tabs (Home, Explore, Alerts, Search). Settings needs to be accessible via a gear icon in the header or from the profile screen. Add a settings gear icon to the Timeline header's trailing content.

- [ ] **Step 3: Verify the app builds, bottom nav renders with new styling**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "feat: replace M3 NavigationBar with custom BottomNavBar"
```

---

## Task 6: Core Component — PostCard Restyle

**Files:**
- Modify: `ui/components/PostCard.kt`

This is the largest single change. Break it into sub-steps.

- [ ] **Step 1: Remove Card wrapper, update container**

Replace the `Card` composable with a plain `Column`. Remove `CardDefaults.cardColors` and `CardDefaults.cardElevation`. The post is now just a `Column` with:
- 16dp horizontal padding, 12dp vertical padding
- No elevation, no background (inherits screen background)

- [ ] **Step 2: Update avatar sizing and author row**

- Avatar: change from 48dp to 42dp (`AppShapes.avatarTimeline`)
- Gap: keep 12dp
- Name: use `AppTypography.bodyLargeSemiBold` + `AppColors.textPrimary`
- Handle: remove from inline display (keep only timestamp)
- Timestamp: `AppTypography.labelMedium` + `AppColors.textSecondary`, prefix with "· "

- [ ] **Step 3: Update body text and hashtags**

- Body text: `AppTypography.bodyLarge` + `AppColors.textBody`
- Hashtags: `AppTypography.caption` + `AppColors.accentMuted`

- [ ] **Step 4: Restyle EngagementBar**

- Icons: 16dp size
- Gaps: 18dp between items
- Colors: `AppColors.textSecondary` default
- Active states:
  - Reply: `AppColors.accent`
  - Share: `AppColors.share`
  - Heart: `AppColors.reaction` (filled)
  - Quote: `AppColors.accent`
  - External share: always `AppColors.textSecondary`
- Counts: `AppTypography.labelMedium`

- [ ] **Step 5: Restyle QuotedPostPreview**

- Border: 1dp `AppColors.divider`, 8dp radius
- Padding: 8dp
- Avatar: 32dp (`AppShapes.avatarQuoted`)
- Remove `surfaceVariant` background, use transparent with border only

- [ ] **Step 6: Update MediaGrid**

- Border radius: 8dp (`AppShapes.mediaRadius`)
- Keep existing layout logic (1/2/3+ image grid)

- [ ] **Step 7: Update repost indicator**

- Left padding: 54dp (42dp avatar + 12dp gap, aligning with post content)
- Avatar: 16dp
- Text: `AppTypography.caption` + `AppColors.textSecondary`

- [ ] **Step 8: Verify build and visual output**

Run: `./gradlew assembleDebug`

Check: Timeline posts render with new styling, no Card shadows, dividers between posts.

- [ ] **Step 9: Commit**

```
git add -A && git commit -m "feat: restyle PostCard with Ivory-inspired design"
```

---

## Task 7: Supporting Components — HtmlContent, MentionAutocomplete, ReactionPicker

**Files:**
- Modify: `ui/components/HtmlContent.kt`
- Modify: `ui/components/MentionAutocomplete.kt`
- Modify: `ui/components/ReactionPicker.kt`

- [ ] **Step 1: Update HtmlContent**

- Replace hardcoded link color `Color(0xFF2563EB)` with `AppColors.accent`
- Replace dark mode link color `Color(0xFF60A5FA)` with `AppColors.accent` (it's already different per theme)
- Mention background: `AppColors.accent.copy(alpha = 0.10f)`
- Code background: `AppColors.surface`
- Default text color: `AppColors.textBody` instead of `MaterialTheme.colorScheme.onSurface`
- Text style: `AppTypography.bodyLarge` instead of `MaterialTheme.typography.bodyMedium`

- [ ] **Step 2: Update MentionAutocomplete**

- Surface background: `AppColors.background`
- Border: 1dp `AppColors.divider`
- Avatar: 36dp (keep existing)
- Name text: `AppTypography.bodyLargeSemiBold` + `AppColors.textPrimary`
- Handle text: `AppTypography.labelMedium` + `AppColors.textSecondary`
- Shadow: remove, use border instead

- [ ] **Step 3: Update ReactionPicker**

- Title: `AppTypography.bodyLargeSemiBold` + `AppColors.textPrimary`
- Grid cell background: `AppColors.surface`
- Active cell: `AppColors.accent.copy(alpha = 0.15f)`
- Count labels: `AppTypography.labelSmall` + `AppColors.textSecondary`
- Close button background: `AppColors.surface`
- Divider: `AppColors.divider`

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```
git add -A && git commit -m "feat: restyle HtmlContent, MentionAutocomplete, ReactionPicker"
```

---

## Task 8: Timeline Screen

**Files:**
- Modify: `ui/screens/timeline/TimelineScreen.kt`

- [ ] **Step 1: Replace header and FAB**

- Remove `CompactTopBar` usage
- Add `LargeTitleHeader(title = "Timeline")` with trailing content:
  - Settings gear icon (28dp circle, `AppColors.surface` background, `AppColors.accent` icon)
  - Compose + icon (28dp circle, `AppColors.surface` background, `AppColors.accent` icon)
  - User avatar (28dp circle)
- Remove `FloatingActionButton` — compose is now in the header
- Update `Scaffold` to remove `floatingActionButton` parameter

- [ ] **Step 2: Update dividers**

- `HorizontalDivider` with `color = AppColors.divider`, `thickness = 1.dp`
- Add 16dp horizontal padding to dividers: `Modifier.padding(horizontal = 16.dp)`

- [ ] **Step 3: Verify build and visual output**

Run: `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "feat: restyle Timeline screen with LargeTitleHeader"
```

---

## Task 9: Notifications Screen

**Files:**
- Modify: `ui/screens/notifications/NotificationsScreen.kt`

- [ ] **Step 1: Replace header and FAB**

- Replace `CompactTopBar` with `LargeTitleHeader(title = "Notifications")`
- Remove `FloatingActionButton`

- [ ] **Step 2: Update NotificationItem styling**

- Avatar: 42dp (`AppShapes.avatarNotification`)
- Remove hardcoded per-type colors (`Color(0xFF4CAF50)`, `Color(0xFF2196F3)`, etc.)
- All notification type icons: `AppColors.textSecondary` (monochrome)
- Actor name: `AppTypography.bodyLargeSemiBold` + `AppColors.textPrimary`
- Action text: `AppTypography.bodyLarge` + `AppColors.textBody`
- Timestamp: `AppTypography.labelMedium` + `AppColors.textSecondary`
- Dividers: `AppColors.divider` with 16dp horizontal padding

- [ ] **Step 3: Update empty state**

- Text: `AppTypography.bodyLarge` + `AppColors.textSecondary`, centered

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```
git add -A && git commit -m "feat: restyle Notifications screen with monochrome icons"
```

---

## Task 10: Explore Screen

**Files:**
- Modify: `ui/screens/explore/ExploreScreen.kt`

- [ ] **Step 1: Replace header and FAB**

- Replace `CompactTopBar` with `LargeTitleHeader(title = "Explore")`
- Remove `FloatingActionButton`

- [ ] **Step 2: Restyle tabs**

- Replace `PrimaryTabRow` + `Tab` with a custom `Row` implementation
- Active tab text: `AppTypography.bodyLargeSemiBold` + `AppColors.accent`
- Inactive tab text: `AppTypography.bodyLarge` + `AppColors.textSecondary`
- Active underline indicator: 2dp thick, `AppColors.accent`, below text
- Tab labels: "Local", "Global"

- [ ] **Step 3: Update dividers**

Same as timeline: `AppColors.divider` with 16dp horizontal padding.

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```
git add -A && git commit -m "feat: restyle Explore screen with custom tab styling"
```

---

## Task 11: Search Screen

**Files:**
- Modify: `ui/screens/search/SearchScreen.kt`

- [ ] **Step 1: Replace header**

- Replace `CompactTopBar` with `LargeTitleHeader(title = "Search")`

- [ ] **Step 2: Restyle search bar**

- Replace `OutlinedTextField` with a custom text field:
  - Background: `AppColors.surface`
  - Border-radius: 12dp (`AppShapes.searchBarRadius`)
  - No outline border
  - Search icon: `AppColors.textSecondary`
  - Text: `AppTypography.bodyLarge` + `AppColors.textBody`
  - Placeholder: `AppTypography.bodyLarge` + `AppColors.textSecondary`
  - 16dp horizontal margin

- [ ] **Step 3: Restyle result items**

- Replace `ListItem` with custom rows
- Actor rows: 40dp avatar, name (`bodyLargeSemiBold`, `textPrimary`), handle (`labelMedium`, `textSecondary`)
- Post results: reuse PostCard component
- Dividers: `AppColors.divider`

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```
git add -A && git commit -m "feat: restyle Search screen with custom search bar"
```

---

## Task 12: Profile Screen

**Files:**
- Modify: `ui/screens/profile/ProfileScreen.kt`

- [ ] **Step 1: Update header**

- Back arrow: `AppColors.accent`
- Ellipsis menu: `AppColors.accent`
- No title text (transparent header)

- [ ] **Step 2: Restyle ProfileHeader — centered layout**

- Avatar: 80dp (`AppShapes.avatarProfile`)
- Name: `AppTypography.titleMedium` + `AppColors.textPrimary`, centered
- Handle: `AppTypography.bodyMedium` + `AppColors.textSecondary`, centered
- Bio: `AppTypography.bodyMedium` + `AppColors.textBody`, centered, 24dp horizontal margin
- Stats row: only display if data available (see spec note). 24dp gap between items.
  - Count: `AppTypography.bodyMedium` with `FontWeight.Bold` + `AppColors.textPrimary`
  - Label: `AppTypography.labelSmall` + `AppColors.textSecondary`

- [ ] **Step 3: Restyle action buttons**

- Follow button: pill shape (20dp radius), `AppColors.accent` background, white text, 28dp horizontal padding
- Unfollow/Remove: pill with `AppColors.buttonOutline` border, `AppColors.accent` text
- Share button: pill, transparent, `AppColors.buttonOutline` border, share icon in `AppColors.accent`

- [ ] **Step 4: Restyle RelationshipTags**

- "Follows you": capsule shape, `AppColors.accentMuted.copy(alpha = 0.10f)` background, `AppColors.accentMuted` text, `AppTypography.caption`
- "Blocked": capsule, `AppColors.reaction.copy(alpha = 0.10f)` background, `AppColors.reaction` text

- [ ] **Step 5: Update post list dividers**

Same as timeline.

- [ ] **Step 6: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 7: Commit**

```
git add -A && git commit -m "feat: restyle Profile screen with centered layout and pill buttons"
```

---

## Task 13: Post Detail Screen

**Files:**
- Modify: `ui/screens/postdetail/PostDetailScreen.kt`

- [ ] **Step 1: Update header**

- Back arrow: `AppColors.accent`
- Title: "Post" centered, `AppTypography.bodyLargeSemiBold` + `AppColors.textPrimary`
- Ellipsis menu: `AppColors.accent`

- [ ] **Step 2: Update post content styling**

- Author row: same as PostCard (42dp avatar, bodyLargeSemiBold name)
- Body: `AppTypography.bodyLarge` + `AppColors.textBody`
- Timestamp: absolute date/time, `AppTypography.labelMedium` + `AppColors.textSecondary`
- Article badge: capsule, `AppColors.accentMuted.copy(alpha = 0.10f)` background, `AppTypography.caption`
- Article title: `AppTypography.titleMedium` + `AppColors.textPrimary`

- [ ] **Step 3: Restyle engagement stats row**

- Counts: `AppTypography.bodyMedium`, count value bold + `textPrimary`, label + `textSecondary`
- Separator dots between items

- [ ] **Step 4: Restyle action bar**

- Icons: 20dp, `AppColors.textSecondary` default
- Active states per spec (reply/accent, share/green, heart/red, quote/accent)
- Delete icon: `AppColors.reaction`
- Hairline dividers above and below

- [ ] **Step 5: Restyle reaction groups**

- Emoji pills: `AppColors.surface` background, 16dp radius, 8dp horizontal padding
- Active pill: `AppColors.accent.copy(alpha = 0.10f)` background, `AppColors.accent` text

- [ ] **Step 6: Restyle bottom sheets (ReactionPicker, Shares, Quotes)**

- Background: `AppColors.background`
- Actor rows: 40dp avatar, `bodyLargeSemiBold` name, `labelMedium` handle
- Title: `AppTypography.bodyLargeSemiBold`
- Load more button: `AppColors.accent` text
- Empty state: `AppColors.textSecondary`

- [ ] **Step 7: Update Reply FAB**

- 56dp circle, `AppColors.accent` background, white reply icon

- [ ] **Step 8: Restyle reply thread dividers**

Same as timeline.

- [ ] **Step 9: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 10: Commit**

```
git add -A && git commit -m "feat: restyle Post Detail screen with new engagement bar and sheets"
```

---

## Task 14: Compose Screen

**Files:**
- Modify: `ui/screens/compose/ComposeScreen.kt`

- [ ] **Step 1: Restyle header**

- Left: "Cancel" text button, `AppTypography.bodyLarge` + `AppColors.accent`
- Center: "Compose" / "Reply" / "Quoting" title, `AppTypography.bodyLargeSemiBold` + `AppColors.textPrimary`
- Right: "Post" pill button, `AppColors.accent` background, white text, 20dp radius. Disabled at 40% opacity.

- [ ] **Step 2: Restyle editor area**

- Text input: `AppTypography.bodyLarge` + `AppColors.textBody`
- Placeholder: `AppTypography.bodyLarge` + `AppColors.textSecondary`
- Reply indicator: "Replying to @handle", `AppTypography.labelMedium` + `AppColors.textSecondary`
- Quote indicator: same styling as QuotedPostPreview from PostCard

- [ ] **Step 3: Restyle tab row (Edit/Preview)**

- Active tab: `AppColors.accent`
- Inactive tab: `AppColors.textSecondary`

- [ ] **Step 4: Restyle bottom toolbar**

- Visibility/language icons: `AppColors.textSecondary`
- Dropdown menus: `AppColors.background` background, items in `AppColors.textBody`

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**

```
git add -A && git commit -m "feat: restyle Compose screen with pill post button"
```

---

## Task 15: Settings Screen

**Files:**
- Modify: `ui/screens/settings/SettingsScreen.kt`

- [ ] **Step 1: Replace header**

- Replace `CompactTopBar` with `LargeTitleHeader(title = "Settings")`

- [ ] **Step 2: Restyle list sections**

- Section headers: `AppTypography.labelMedium`, uppercase, `AppColors.textSecondary`, 16dp horizontal padding
- Replace `ListItem` with custom rows:
  - Label: `AppTypography.bodyLarge` + `AppColors.textPrimary`
  - Value/description: `AppTypography.bodyMedium` + `AppColors.textSecondary`
  - 16dp padding all sides
- Dividers: `AppColors.divider`

- [ ] **Step 3: Restyle interactive controls**

- `Switch`: keep M3 but tint with accent colors via `SwitchDefaults.colors(checkedThumbColor = AppColors.accent, ...)`
- `Slider`: tint with `AppColors.accent` via `SliderDefaults.colors(thumbColor = AppColors.accent, activeTrackColor = AppColors.accent)`
- Logout button icon: `AppColors.reaction`
- Dialog buttons: `AppColors.accent`

- [ ] **Step 4: Restyle app info section**

- Logo: 80dp, 16dp corner radius (keep existing)
- App name: `AppTypography.titleMedium` + `AppColors.textPrimary`
- Version: `AppTypography.labelMedium` + `AppColors.textSecondary`

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**

```
git add -A && git commit -m "feat: restyle Settings screen with grouped sections"
```

---

## Task 16: Sign-In Screen

**Files:**
- Modify: `ui/screens/auth/SignInScreen.kt`

- [ ] **Step 1: Restyle layout**

- Remove `CompactTopBar` (sign-in doesn't need a top bar per spec)
- Center content vertically
- Logo: 120dp, centered
- Title: `AppTypography.titleLarge` + `AppColors.textPrimary`, centered
- Subtitle: `AppTypography.bodyMedium` + `AppColors.textSecondary`, centered

- [ ] **Step 2: Restyle text fields**

- Replace `OutlinedTextField` with custom styled `BasicTextField`:
  - Background: `AppColors.surface`
  - Border-radius: 12dp
  - No outline border
  - Text: `AppTypography.bodyLarge` + `AppColors.textBody`
  - Placeholder: `AppTypography.bodyLarge` + `AppColors.textSecondary`
  - 32dp horizontal margin

- [ ] **Step 3: Restyle buttons**

- Primary button: full-width pill, `AppColors.accent` background, white text, 20dp radius
- "Try a different username" link: `AppTypography.bodyMedium` + `AppColors.accent`
- Error text: `AppTypography.labelMedium` + `AppColors.reaction`

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```
git add -A && git commit -m "feat: restyle Sign-In screen with pill button and custom fields"
```

---

## Task 17: Update MainActivity & Cleanup

**Files:**
- Modify: `MainActivity.kt`
- Delete: `ui/components/CompactTopBar.kt`

- [ ] **Step 1: Update MainActivity**

- Replace `MaterialTheme.colorScheme.background` in `Surface` with `LocalAppColors.current.background`
- System bars: ensure status bar and navigation bar colors match `AppColors.background`

- [ ] **Step 2: Delete CompactTopBar.kt**

All screens now use `LargeTitleHeader`. Remove the file and verify no imports reference it.

Run: `grep -r "CompactTopBar" app/src/main/java/` — should return nothing.

- [ ] **Step 3: Full build verification**

Run: `./gradlew assembleDebug`

Manually verify on device/emulator:
- Light mode timeline, profile, notifications, explore, search, settings, compose, post detail, sign-in
- Dark mode: same screens
- Font scaling at 75%, 100%, 200%

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "chore: cleanup — remove CompactTopBar, update MainActivity"
```

---

## Summary

| Task | Component | Files Changed | Complexity |
|------|-----------|---------------|------------|
| 1 | Colors | 2 | Low |
| 2 | Typography | 2 | Low |
| 3 | Shapes | 1 | Low |
| 4 | LargeTitleHeader | 1 | Low |
| 5 | BottomNavBar + App | 2 | Medium |
| 6 | PostCard | 1 | High |
| 7 | HtmlContent + Supporting | 3 | Medium |
| 8 | Timeline | 1 | Low |
| 9 | Notifications | 1 | Medium |
| 10 | Explore | 1 | Low |
| 11 | Search | 1 | Medium |
| 12 | Profile | 1 | Medium |
| 13 | Post Detail | 1 | High |
| 14 | Compose | 1 | Medium |
| 15 | Settings | 1 | Medium |
| 16 | Sign-In | 1 | Low |
| 17 | Cleanup | 2 | Low |

**Total:** 17 tasks, ~22 files touched. Tasks 1-5 are foundation (must be done first, in order). Tasks 6-7 are core components. Tasks 8-16 are independent screen updates (can be parallelized). Task 17 is final cleanup.
