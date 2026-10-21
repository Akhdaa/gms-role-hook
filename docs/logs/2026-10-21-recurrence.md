# 2026-10-21 recurrence (no hook installed)

Device: POCO F4 (munch), Project Infinity-X 3.12, GMS 26.40.32. Logs read over adb about 5 minutes
after boot (boot ~09:14, read 09:19 local time). Account identifiers are omitted.

The previous day (2026-10-20, ~12:29) Play Store worked without any hook: role-holder denials still
appeared in the log but Play Store sync completed (`Sync complete, number failed: 0`). About 21 hours
later, after a reboot, the same denial became fatal again.

## Counts in logcat since boot

| Pattern | Count |
|---|---|
| `SecurityException: getRoleHoldersAsUser: Neither user 10290 ... GET_ROLE_HOLDERS` | 93 |
| `AuthSecurityError` | 20 |
| `BadAuthentication` | 0 |
| `AuthFailureError` (Play Store) | 1 |
| `FATAL EXCEPTION` | 0 |

First denial 09:16:49, last 09:17:53.

## Token requests that failed (`ChimeraGetToken`, app = com.google.android.gms)

| Scope | Count |
|---|---|
| `oauth2:https://www.googleapis.com/auth/webhistory` | 12 |
| `oauth2:https://www.googleapis.com/auth/googleplay` | 3 |
| `oauth2:https://www.googleapis.com/auth/cryptauth` | 2 |
| `oauth2:https://www.googleapis.com/auth/onegoogle.api` | 1 |

Play Store: `com.android.volley.AuthFailureError: Error while retrieving auth token`.

## Interpretation

- `BadAuthentication = 0` and 3 Google accounts present: stored credentials were intact. The failure was
  the role-holder denial alone.
- Same denial harmless one day and fatal the next. GMS `dbsu.b()` has a guarded branch (catches
  `SecurityException`, returns an empty list) and an unguarded one, chosen by a flag. A flag change is the
  likely explanation; its value and source were not verified. A reboot happened in between, so a
  reboot-triggered path cannot be ruled out.
- `GET_ROLE_HOLDERS` was not granted; the allowlist module was not installed at the time.
