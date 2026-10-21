# gms-role-hook

A small Vector/LSPosed (legacy Xposed API) module that fixes Google sign-in and Play Store
**"Authentication is required"** on ROMs where the updated Google Play services cannot be granted
`GET_ROLE_HOLDERS`.

> **Status: v2.0 is a pre-release in testing.** It fixed the errors and kept Play Integrity STRONG on the test
> device, but one unexplained `system_server` restart was seen during the first test, and a 24-hour / reboot
> test is still pending. Read *Known issues* before using it.

## Symptoms

- Play Store shows "Authentication is required"; Google apps cannot load or sign in.
- Signing in again or adding a Google account shows "no internet connection" while the network works.
- logcat: `AuthSecurityError`, `GmsAccountAuthenticatorImpl ... error status while fetching token`,
  sometimes a `FATAL EXCEPTION` in `com.google.android.gms.ui`, and
  `SecurityException: getRoleHoldersAsUser: Neither user ... has at least one of [MANAGE_ROLE_HOLDERS, GET_ROLE_HOLDERS]`.
- It can come and go: working one day, broken the next (see `docs/logs/2026-10-21-recurrence.md`).

## Cause

Google Play services (GMS) 26.40.x calls `RoleManager.getRoleHolders("android.app.role.SUPERVISION")` to
check whether the device is supervised (Family Link). `GET_ROLE_HOLDERS` is `signature|privileged`. The ROM's
system-image GMS (25.15.x) never requested it, so the updated GMS is not granted it, `pm grant` refuses it
("not a changeable permission type") and a privapp allowlist does not help (see `experimental/`).

`system_server` then throws `SecurityException`. In GMS `dbsu.b()` one branch (selected by a flag) catches it
and another does not. When the unguarded branch runs:

- the sign-in UI process (`com.google.android.gms.ui`) crashes, shown as "no internet";
- token requests fail with `AuthSecurityError`, so Play Store reports "Authentication is required".

Clearing GMS data additionally removes stored credentials (`BadAuthentication: Long live credential not
available`), which then requires signing in again through the crashing UI.

Not verified: the flag's value and source, and the exact AOSP rule that blocks the grant.

## How v2.0 works

Scope: **`android` (system_server) only**. Nothing is injected into Google Play services.

It hooks `com.android.role.RoleService$Stub.enforceCallingOrSelfAnyPermissions(String[], String)`. When the
required permissions include `GET_ROLE_HOLDERS` or `OBSERVE_ROLE_HOLDERS` **and** the calling UID is
`com.google.android.gms`, the check is skipped. `getRoleHoldersAsUser` then returns the real role holders
(normally none). Every other caller and every other permission is checked as usual. Any failure inside the
hook falls back to the original check.

This **relaxes one permission check** in the system: GMS may read the role-holder list without the
permission. It is narrow (one app, read-only role-holder queries) but it is a real security relaxation.

## Results on the test device

| Version | Where it hooks | Sign-in / tokens | Play Integrity |
|---|---|---|---|
| none | - | broken when the unguarded branch runs | STRONG |
| v1.0 | all GMS processes | fixed | **BASIC only** |
| v1.1 | GMS processes except `:unstable` | fixed | **BASIC only** |
| **v2.0** | system_server | fixed: 0 `AuthSecurityError`, `allowed GMS` x3, Play Store sync OK | **STRONG** (owner report) |

Integrity results are the device owner's reports from a checker app; they were not read from logs.

## Known issues

- During the first v2.0 test `system_server` restarted once (new pid at 09:33:58 on 2026-10-21) and the
  device later dropped off adb. The cause is **not known yet**; it may or may not be this module.
- A bug in a system_server hook can cause framework restarts or a bootloop. Recovery: KernelSU safe mode
  (hold Volume Down while booting) disables Vector modules.
- Only tested on one device. Behaviour on other ROMs or GMS versions is unknown.

## Install

1. Install `gms-role-hook.apk` (package `io.munch.gmsrolehook`). Remove the old `io.xxru.gmsrolehook` if present.
2. Enable with scope `android`:

       vector-cli modules enable io.munch.gmsrolehook
       vector-cli scope set io.munch.gmsrolehook android/0

3. Reboot. Check logcat for `RoleHolderHook: hooked 1 method(s) in android`.

## Tested on

- Device: POCO F4 (`munch`, 22021211RG)
- ROM: Project Infinity-X 3.12 (UNOFFICIAL, Android 16, build 25.08.2026)
- Framework: Vector 2.2, Zygisk Next 1.5.2, KernelSU
- Google Play services: 26.40.32

## Build (Windows + Git Bash)

Needs a JDK and the Android SDK (`build-tools`, `platforms/android-36`). No Gradle.

    ANDROID_HOME=/path/to/Sdk ./build.sh

`_stubs/` holds compile-only copies of the Xposed API that Vector provides at runtime.

## Repository layout

- `src/` - v2.0 system_server hook
- `experimental/gms-process-hook-v1/` - v1.0 / v1.1 GMS-process hooks (lose DEVICE/STRONG)
- `experimental/gms-roleholders-fix/` - KernelSU allowlist module (did not grant the permission)
- `docs/logs/` - recorded evidence

## License

MIT, see `LICENSE`. Provided as-is, no warranty.
