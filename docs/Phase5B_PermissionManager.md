# Phase 5B – Extract PermissionManager

## 1. Purpose of the extraction
- Isolate Android runtime permission handling from `MainActivity` into a dedicated manager.
- Keep `MainActivity` focused on orchestration while preserving existing behavior.

## 2. Responsibilities moved from `MainActivity`
- Location permission check (`ACCESS_FINE_LOCATION`).
- Location permission request (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, request code `100`).
- App settings intent creation/opening for permission recovery flows.

## 3. Public API of `PermissionManager`
- `fun hasLocationPermission(): Boolean`
- `fun requestLocationPermission()`
- `fun openAppSettings()`

## 4. List of methods moved
Moved from `MainActivity.kt` to `PermissionManager.kt`:
- `hasLocationPermission`
- `requestLocationPermission`
- `openAppSettings`

## 5. Files modified
- `app/src/main/java/com/healthbridge/MainActivity.kt`
- `app/src/main/java/com/healthbridge/PermissionManager.kt` (new)
- `docs/Phase5B_PermissionManager.md` (new)

## 6. Dependencies
- Android framework: `Activity`, `Intent`, `Settings`, `Uri`, `Manifest`, `PackageManager`
- AndroidX:
  - `androidx.core.content.ContextCompat`
  - `androidx.core.app.ActivityCompat`

## 7. Testing checklist verifying no behavior change
- [ ] App still starts and initializes UI, speech, messaging, map, and role loading.
- [ ] Patient role still requests location permission when not granted.
- [ ] If location permission is already granted, telemetry still starts as before.
- [ ] Viewer role path remains unchanged (map + messaging listeners still start).
- [ ] Speech recognition flow remains unchanged.
- [ ] Message send/receive/ack flows remain unchanged.
- [ ] Build compiles with no manual edits required.
