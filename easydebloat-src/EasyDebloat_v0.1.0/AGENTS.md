# Easy Debloat Agent Contract

Factory baseline for this pilot:
`riccardopinato/App-Factory-Core @ e183de8f811873a146eaa78bb1daa2617e11f466`
(Master v26.2 / Golden Index v13).

For later Factory-dependent work, resolve `CURRENT.yaml` again only when a new
session/pilot requires it.

## Product / architecture invariants

- Native Android Kotlin + Jetpack Compose. Do not rewrite to Flutter without a
  concrete product benefit.
- Shizuku UserService is the single privilege transport.
- Do not add a parallel manual ADB, root, wireless-debug or arbitrary-shell path
  unless Product Truth explicitly introduces it.
- No manual shell UI.
- Package names must be validated before interpolation into privileged commands.
- DANGEROUS / hard-blocked packages remain non-actionable.
- Remove/restore is user-0 scoped; never mutate /system.

## Code hygiene

- Replace rather than stack parallel privilege/command paths.
- Compound shell strings that hide partial state changes are not allowed.
- Optional helper steps must not convert a successful required state transition
  into a false failure.
- Re-run relevant tests/build after final cleanup.
- Keep cleanup bounded to Easy Debloat; do not refactor the unrelated bTrader
  project sharing this temporary repository.

## Validation

At minimum for privilege/command changes:
- pure command-plan regression tests;
- Android unit tests;
- debug/release build when the Android toolchain is available;
- physical Shizuku validation before claiming runtime certification.

Build PASS != Shizuku runtime PASS.
