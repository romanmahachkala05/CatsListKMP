# Releasing

## One-time setup (only for whoever signs releases)

Create a keystore and a `keystore.properties` beside it in the repository root:

```properties
storeFile=catslist-release.jks
storePassword=…
keyAlias=catslist
keyPassword=…
```

Both files are gitignored and must stay that way. Without them the build still
works — `assembleRelease` just produces an unsigned APK, which is what CI and a
fresh clone get. Only a machine holding the key can produce a signed build.

CI can supply the same four values as `CATSLIST_STOREFILE`,
`CATSLIST_STOREPASSWORD`, `CATSLIST_KEYALIAS` and `CATSLIST_KEYPASSWORD`.

## Cutting a release

1. **Bump the version** — `catslist.version` in `gradle.properties`, the one
   number every app ships. Android's `versionCode` and `versionName` are derived
   from it ([ADR-0021](docs/DECISIONS.md#adr-0021)), it is the desktop
   installers' version, and the iOS build stamps it into `Info.plist`
   ([ADR-0040](docs/DECISIONS.md#adr-0040)), so there is no second number to
   remember.
2. **Merge to `dev`** as usual, with CI green.
3. **Merge `dev` into `main`** via a pull request, so the release commit is on
   `main` and has passed the same gate.
4. **Build and check the signature:**

   ```bash
   ./gradlew assembleRelease
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

   An `app-release-unsigned.apk` means the keystore was not picked up.

   The release build runs R8 (`isMinifyEnabled`, `isShrinkResources`), which
   took the Android-only 2.x APK from 13.6 MB to 2.3 MB. From 3.0.0 it is 7.4 MB, most of it
   the bundled SQLite's native library for four ABIs — the price of one SQLite on every
   platform ([ADR-0029](docs/DECISIONS.md#adr-0029)). There are no hand-written keep rules —
   Room, Koin, kotlinx.serialization, Ktor and Coil all ship their own, and
   a rule that is never exercised is worse than none. Because R8 failures show
   up at runtime rather than at build time, check the minified APK on a device
   (feed, favorites across a restart, download) before tagging.
5. **Tag `main`** and push the tag:

   ```bash
   git tag -a v2.0.0 -m "v2.0.0"
   git push origin v2.0.0
   ```
6. **Publish the draft release.** The tag runs `.github/workflows/release.yml`,
   which builds the desktop installers — MSI on Windows, DMG on macOS, DEB on
   Linux, each on its own runner, since jpackage cannot build for another OS —
   and the release APK, then drafts a GitHub release with all four attached.
   Check the notes and the files, replace the APK with the locally signed one
   if CI had no keystore, and publish.

   The desktop installers are unsigned: Windows SmartScreen and macOS
   Gatekeeper will warn on first launch. Signing them needs a code-signing
   certificate and an Apple Developer ID, neither of which this project has.

## Known limitations

- Nothing publishes to Play or to any desktop store. The GitHub release is the
  distribution point.
- iOS has no release artifact: without a paid Apple Developer account it is built
  from source ([ADR-0041](docs/DECISIONS.md#adr-0041)). Before tagging, run it on the
  simulator and on a phone, from Xcode on a Mac.
- Neither the release build nor the instrumented tests run in CI
  ([ADR-0018](docs/DECISIONS.md#adr-0018)).
