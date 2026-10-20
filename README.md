# gms-role-hook

A tiny Vector/LSPosed (legacy Xposed API) module that stops the Google sign-in screen from
crashing on ROMs where the updated Google Play services cannot be granted `GET_ROLE_HOLDERS`.

## Problem

Google Play services (GMS) 26.40.x calls
`RoleManager.getRoleHolders("android.app.role.SUPERVISION")` (a supervised-device / Family Link
check). The privileged permission `GET_ROLE_HOLDERS` is `signature|privileged`, and the ROM's
system-image GMS (25.15.x) never requested it, so the updated GMS is not granted it and
`system_server` throws `SecurityException`. In GMS `dbsu.b()` one branch (selected by a flag)
calls it without a `try/catch`, so the exception reaches the sign-in UI process
(`com.google.android.gms.ui`) and the account flow fails with a misleading "no internet" message.

## What the hook does

In `com.google.android.gms` processes only, it wraps `RoleManager.getRoleHolders`,
`getRoleHoldersAsUser` and `addOnRoleHoldersChangedListenerAsUser`. A `SecurityException` is
swallowed and an empty list is returned, the same value GMS' own guarded branch returns.
Nothing else is changed.

## When to use it

Only while signing in, adding an account, or after clearing GMS data. Install, enable with scope
`com.google.android.gms`, finish signing in, then remove it. It was observed to be unnecessary
for normal token refresh once credentials are stored. Leaving it installed is not recommended:
it was not proven harmless next to other Zygisk modules.

## Build (Windows + Git Bash)

Needs a JDK, the Android SDK (`build-tools`, `platforms/android-36`). No Gradle.

    ANDROID_HOME=/path/to/Sdk ./build.sh

`build.sh` runs javac, d8, aapt2, zipalign and apksigner and writes `gms-role-hook.apk`
signed with a generated debug key. `_stubs/` holds compile-only copies of the Xposed API that
the Vector framework provides at runtime.

## Enable

    vector-cli modules enable io.xxru.gmsrolehook
    vector-cli scope set io.xxru.gmsrolehook com.google.android.gms/0

then force-stop Google Play services.

## Status and caveats

Tested only on one device (POCO F4, custom Android 16 ROM, Vector 2.2, GMS 26.40.32).
Behaviour on other GMS versions is unknown. If the account has lost its credentials, signing in
again is still required. Provided as-is, no warranty.
