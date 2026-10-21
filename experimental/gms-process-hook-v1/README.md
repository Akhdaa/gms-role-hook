# GMS-process hook (v1.0 / v1.1), superseded

These hooked `RoleManager.getRoleHolders*` **inside Google Play services** (scope `com.google.android.gms`)
and returned an empty list on `SecurityException`.

- v1.0 hooked every GMS process. v1.1 skipped the `:unstable` process (DroidGuard / Play Integrity).
- Both fixed sign-in and token errors on the test device.
- **Both made Play Integrity drop to BASIC** while installed (DEVICE and STRONG lost). Reported by the device
  owner for v1.0 twice and for v1.1 once; removing the module restored the verdicts. Skipping `:unstable`
  did not help, so the cause is not a hook in that process alone (exact mechanism unknown).

Replaced by the system_server hook in the repository root (v2.0).
