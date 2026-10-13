# App Factory v26.2 — Easy Debloat Code Hygiene Pilot

Date: 2026-10-14

## Baselines

- App Factory: `e183de8f811873a146eaa78bb1daa2617e11f466`
  - Master v26.2
  - Golden Index v13
  - Agent Governance v1 CERTIFIED PROCESS
  - Code Hygiene v1 COPY-READY PROCESS
- Easy Debloat packed source:
  `riccardopinato/btrader:easydebloat-build @ 10ffcd4e86bb88807703d0fcaa65017a4c16b505`

## Source normalization

The prior branch stored Easy Debloat only as five base64 tar chunks plus a
Dockerfile. The exact tarball was materialized into normal Git-tracked source
before functional modification.

## Audit findings

### NOT FOUND — no fake cleanup

No active manual ADB or Wireless ADB implementation exists in v0.1.0.
Privilege operations already route through Shizuku UserService. Therefore no
"legacy ADB path" is deleted just to satisfy the pilot.

### REPLACE — compound command path

The repository built state-changing actions with raw `&&` shell composition.
This mixed best-effort process-control with the required package-state change.

Concrete diagnostic bug:
- ENABLE ran `pm enable ... && am force-stop ...`;
- the app could already be enabled while a later force-stop failure made the
  whole action appear failed.

Replacement:
- `PackageOperationExecutor` owns an explicit ordered step plan;
- required and best-effort steps are distinct;
- invalid package names never reach privileged shell;
- restore stops after a failed install-existing and surfaces enable failure;
- `DebloatRepository` no longer owns raw shell composition.

### FIX — Shizuku duplicate bind path

Shizuku's sticky binder listener can invoke immediately when the binder is
already available. The previous `start()` then called `refreshAndBind()`
again unconditionally, allowing duplicate UserService bind attempts while the
first connection was still pending.

Fix:
- register dead/permission listeners before the sticky listener;
- only explicit-probe when the binder is not alive;
- add a `binding` gate;
- clear the gate on connect/disconnect/binder death/bind failure;
- manual retry explicitly resets a stuck pending bind.

## REMOVE / KEEP

REMOVE after source normalization:
- packed `easydebloat-ci/source.part00..04`;
- obsolete `easydebloat-ci/Dockerfile`;
- temporary materialization workflow.

KEEP:
- Shizuku API/provider;
- AIDL UserService;
- explicit user-0 package commands;
- force-stop only as best-effort pre-step for disable/remove;
- QUERY_ALL_PACKAGES pending separate Play-policy review;
- existing DataStore/history/catalog/UI.

## Validation

Required:
- command-plan regression tests;
- Android unit tests;
- assembleDebug / assembleRelease;
- physical Shizuku permission/binder/action validation before runtime
  certification.

Pilot certification remains BLOCKED if Android build/unit gates are unavailable.
