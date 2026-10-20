# gms-roleholders-fix (experimental, did NOT fix the problem)

A KernelSU module that adds a privapp-permissions allowlist entry:

    /product/etc/permissions/privapp-permissions-gms-roleholders.xml
      com.google.android.gms -> GET_ROLE_HOLDERS, OBSERVE_ROLE_HOLDERS

Kept here only as a record of an approach that was tried.

## Reported behaviour

Installed through KernelSU Manager and rebooted: Play Store worked for a short time, then
"Authentication is required" returned. During that window the logs still showed the permission as not
granted (`GET_ROLE_HOLDERS` absent from the install permissions, `SecurityException` still thrown), so the brief
success was most likely not caused by the allowlist (inference). A freshly logged-in account, not this module,
was what restored Play Store in the end.

A pre-built zip is attached to the `allowlist-v1.0` pre-release. It is kept for reference only.

## What happened

- The allowlist was loaded by the system. Verified with
  `cmd package get-privapp-permissions com.google.android.gms`, which listed both permissions.
- GMS was reinstalled in place (same version, `pm install-multiple -r`) to force a permission re-evaluation.
- `GET_ROLE_HOLDERS` was still not granted, `pm grant` is rejected ("not a changeable permission type"),
  and the `SecurityException` kept being thrown.

## Likely reason (inference, not verified in AOSP source here)

The updated GMS (26.40.x, in /data/app) requests `GET_ROLE_HOLDERS`, but the system-image GMS (25.15.x) does
not. An updated system app does not appear to receive new privileged permissions that the system-image
version never requested, regardless of the allowlist. The `dumpsys package` output was consistent with this:
the hidden system package had no `ROLE_HOLDERS` entries.

The Vector hook in the repository root avoids the permission entirely and is the approach that worked.

## Build

Zip the contents of this folder (forward-slash paths, LF line endings) and install it from KernelSU Manager.
