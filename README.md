# gms-role-hook

A tiny Vector/LSPosed (legacy Xposed API) module that stops the Google sign-in screen from crashing on
ROMs where the updated Google Play services cannot be granted `GET_ROLE_HOLDERS`.

## Symptoms

- Play Store shows **"Authentication is required"** and Google apps cannot load.
- Signing in again or adding a Google account shows **"no internet connection"** while Wi-Fi and mobile data work.
- logcat shows `AuthSecurityError`, `GmsAccountAuthenticatorImpl ... error status while fetching token`,
  a `FATAL EXCEPTION` in `com.google.android.gms.ui`, and
  `SecurityException: getRoleHoldersAsUser: Neither user ... has at least one of [MANAGE_ROLE_HOLDERS, GET_ROLE_HOLDERS]`.

## Cause

Two separate layers were observed.

1. **Permission denial (observed in logs and decompiled bytecode).** Google Play services (GMS) 26.40.x calls
   `RoleManager.getRoleHolders("android.app.role.SUPERVISION")`, a supervised-device / Family Link check.
   `GET_ROLE_HOLDERS` is `signature|privileged`. The ROM's system-image GMS (25.15.x) never requested it, so the
   updated GMS is not granted it and `system_server` throws `SecurityException`. In GMS `dbsu.b()` one branch
   (selected by a flag) calls it without a `try/catch`, so the exception reaches the sign-in UI process and the
   account flow fails with a misleading "no internet" message.
2. **Missing credentials (observed).** Clearing GMS data removes the stored credentials of every account, after
   which logs show `BadAuthentication: Long live credential not available`, and Play Store reports
   `AuthFailureError: User needs to (re)enter credentials`. That is the "Authentication is required" message.
   Signing in again restores them, but layer 1 stops the sign-in screen from completing.

Not verified: the exact value and origin of the GMS flag, and why an updated system app cannot be granted the
permission (see `experimental/`).

## What the hook does

In `com.google.android.gms` processes only, it wraps `RoleManager.getRoleHolders`, `getRoleHoldersAsUser` and
`addOnRoleHoldersChangedListenerAsUser`. A `SecurityException` is swallowed and an empty list is returned, the
same value GMS' own guarded branch returns. Nothing else is changed.

## When to use it

Only while signing in, adding an account, or after clearing GMS data. Install, enable with scope
`com.google.android.gms`, finish signing in, then remove it. Once credentials are stored it was not needed for
normal token refresh. Leaving it installed is not recommended: it was not proven harmless next to other Zygisk
modules (for example Play Integrity Fix).

## Tested on

- Device: POCO F4 (`munch`, 22021211RG)
- ROM: Project Infinity-X **3.12** (UNOFFICIAL, Android 16, build 25.08.2026)
- Framework: Vector 2.2
- Google Play services: 26.40.32

Behaviour on other devices, ROMs or GMS versions is unknown.

## Build (Windows + Git Bash)

Needs a JDK and the Android SDK (`build-tools`, `platforms/android-36`). No Gradle.

    ANDROID_HOME=/path/to/Sdk ./build.sh

`build.sh` runs javac, d8, aapt2, zipalign and apksigner and writes `gms-role-hook.apk`, signed with a
generated debug key. `_stubs/` holds compile-only copies of the Xposed API that Vector provides at runtime.

## Enable

    vector-cli modules enable io.xxru.gmsrolehook
    vector-cli scope set io.xxru.gmsrolehook com.google.android.gms/0

then force-stop Google Play services.

## Experimental

`experimental/gms-roleholders-fix` is an allowlist-based module that was tried first. It did **not** work; see
its README.

## License

MIT, see `LICENSE`. Provided as-is, no warranty.
