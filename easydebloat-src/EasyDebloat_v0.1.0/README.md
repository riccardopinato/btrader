# Easy Debloat — UTIL-051

Versione pilot Code Hygiene: **0.1.1**

Utility Android nativa Kotlin + Jetpack Compose per gestire app di sistema tramite Shizuku, senza root.

## Funzioni implementate

- rilevamento Shizuku e richiesta autorizzazione;
- Shizuku `UserService` reale con AIDL;
- executor strutturato delle operazioni privilegiate, senza catene `&&` ambigue;
- scansione pacchetti di sistema con `pm list packages`;
- rilevamento installato / disabilitato / rimosso per user 0;
- classificazione SAFE / CAUTION / DANGEROUS / UNKNOWN;
- hard-block dei package critici;
- ricerca e filtri;
- selezione multipla sicura;
- `pm disable-user --user 0`;
- `pm enable --user 0`;
- `pm uninstall --user 0`;
- `cmd package install-existing --user 0`;
- cronologia locale DataStore;
- undo rapido;
- export backup TXT tramite Storage Access Framework;
- impostazioni modalità predefinita e filtri;
- UI Material 3 light/dark/dynamic color.

## Sicurezza

Easy Debloat non espone una shell manuale. I package name sono validati prima di ogni comando e i componenti classificati `DANGEROUS` non possono essere modificati.

La rimozione usa esclusivamente `--user 0`: non modifica `/system` e non elimina fisicamente l'APK dalla ROM.

## Toolchain prevista

- JDK 17
- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- compileSdk 37
- targetSdk 36
- minSdk 26
- Compose BOM 2026.06.00
- Shizuku API/provider 13.1.5

## App Factory

Pilot allineato ad App-Factory-Core v26.2 / Golden Index v13.
`AGENTS.md` definisce il contratto operativo locale.

## Build

Il sorgente è ora materializzato come normali file Git. La compilazione Android
resta un gate separato e non va confusa con la sola validazione Kotlin pura.

Task previsti:

```bash
./gradlew test
./gradlew assembleDebug
```

APK atteso:

`app/build/outputs/apk/debug/app-debug.apk`

## Nota Play Store

L'app dichiara `QUERY_ALL_PACKAGES` perché la funzione principale richiede l'inventario delle app installate. La distribuzione Play Store richiederà una verifica specifica delle policy applicabili prima della pubblicazione.
