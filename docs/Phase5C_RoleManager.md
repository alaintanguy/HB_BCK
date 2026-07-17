# Phase 5C – RoleManager

## Purpose

Phase 5C extracts all member role management logic from `MainActivity.kt` into a dedicated `RoleManager.kt` class, preserving the exact application behavior that existed after Phase 5B (PermissionManager).

The goal is to keep `MainActivity` as a thin coordinator while concentrating role-resolution responsibility in a single, well-scoped class.

---

## Responsibilities Moved

The following responsibilities have been moved from `MainActivity` to `RoleManager`:

| Responsibility | Before (MainActivity) | After (RoleManager) |
|---|---|---|
| Firebase role read | `loadRole()` private method | `RoleManager.loadRole()` |
| Role string → boolean resolution | `isPublisher = (role == "patient")` | Inside `RoleManager.loadRole()` callback |
| Firebase path `groups/family_001/members/<id>/profile/role` | Hardcoded in `loadRole()` | Encapsulated in `RoleManager` |
| Persistent role change listener | `addValueEventListener` in `MainActivity` | `addValueEventListener` in `RoleManager` |

---

## Public API

### `RoleManager(memberId: String)`

Constructor. Accepts the member identifier (e.g. `"M1"` or `"M2"`) whose role will be loaded.

### `fun loadRole(onRoleResolved: (isPublisher: Boolean) -> Unit)`

Reads the member's role from Firebase at:

```
groups/family_001/members/<memberId>/profile/role
```

Registers a **persistent** `ValueEventListener` so that any server-side role change is immediately reflected.

Calls `onRoleResolved` with:
- `isPublisher = true` when `role == "patient"`
- `isPublisher = false` for any other value (defaults to `"caregiver"` when absent)

`MainActivity` is responsible for storing the returned `isPublisher` value and calling `startAccordingToRole()`.

---

## Methods Moved

| Method | Source file | Destination |
|---|---|---|
| `loadRole()` | `MainActivity.kt` | `RoleManager.loadRole()` |

---

## Files Modified

| File | Change |
|---|---|
| `app/src/main/java/com/healthbridge/RoleManager.kt` | **Created** – contains extracted role loading logic |
| `app/src/main/java/com/healthbridge/MainActivity.kt` | **Updated** – delegates role loading to `RoleManager`; removed `loadRole()` private method |

---

## Dependencies

`RoleManager` depends on:
- `com.healthbridge.firebase.FirebaseManager` – for `memberReference(memberId)` 
- `com.google.firebase.database` – `DataSnapshot`, `DatabaseError`, `ValueEventListener`
- `android.util.Log`

`RoleManager` does **not** depend on any `Activity` context, making it easy to test or reuse.

---

## Testing Checklist

- [ ] App compiles without errors or warnings related to the refactor.
- [ ] On startup, the role is loaded from Firebase and `startAccordingToRole()` is called.
- [ ] When `role == "patient"`, `isPublisher` is `true` and telemetry engine starts.
- [ ] When `role == "caregiver"` (or absent), `isPublisher` is `false` and viewer mode starts.
- [ ] Both M1 and M2 markers appear on the map in both roles.
- [ ] Message listener starts correctly after role resolution.
- [ ] `MainActivity` no longer contains any Firebase code reading `profile/role`.
- [ ] `authenticateFirebase()` (commented-out fallback path) correctly delegates to `RoleManager`.

---

## Future Extension Ideas

- Add a `Role` enum (`PATIENT`, `CAREGIVER`) instead of a raw `Boolean` to make the API more expressive.
- Support multiple families by injecting the family ID into `RoleManager`.
- Cache the last known role locally (SharedPreferences) so the app can start offline.
- Expose a `LiveData<Boolean>` or `StateFlow<Boolean>` for reactive role updates.
- Add unit tests with a Firebase emulator or mock `FirebaseManager`.
