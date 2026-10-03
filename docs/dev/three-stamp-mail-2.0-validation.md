# 3-Stamp Mail 2.0 validation

Validated on 2026-10-03. Version 2.0.0, build 75; package
`io.github.supermonster003.autojs6.plugin.three.stamp.mail`, plugin id `three-stamp-mail`.
The generic `mail` engine, `default` variant, discovery categories and Binder protocol remain
unchanged. New installations require AutoJs6 6.8.0 build 5316. Old installations and their data
are separate; when both plugins are enabled, Plugin Center's existing mail priority selects one.

## Build and repository checks

- Mail-core JVM: 275 tests, 273 passed and 2 optional probes skipped.
- Application JVM: 88 passed.
- Host API: 10 passed; host mail/settings JVM: 74 passed; AppDebug assembly passed.
- Debug, instrumentation and signed R8 release APKs assembled successfully. The first build
  after the directory move required cleaning module outputs that retained the old absolute path.
- Lint: 0 errors, 67 warnings. This is not a zero-warning result.
- Ten-language Markdown generation: 36 artifacts verified. Launcher generation: 14 resources
  verified, with three Python tests for source detail, alpha, safe-circle geometry and determinism.
- Official index generator: 46 tests passed. Final release admission is produced from the signed
  APK receipt and the release's actual source commit in the index repository.
- The two release host AARs were built together from AutoJs6 commit
  `5b5841bfbf60c0000b525ca69dd906ade642e5ee`; their SHA-256 values are pinned in
  `locks/host-api-aars.lock`. The generic mail protocol floor stays at host build 5282 so that
  changing the official installation identity does not invalidate other mail implementations.

## Devices and OAuth

- API 24 x86 emulator: the complete build-74 instrumentation run reported 55 tests, with 41 passed and
  14 assumption skips (real-account, optional performance or explicitly requested setup cases).
  Build 75 then passed all 11 targeted appearance, icon and metadata regression cases; a new light-mode
  screenshot confirmed readable white navigation buttons on the dark Android 7 system bar.
- Sony XQ-AT72, API 31 arm64-v8a: 22 relevant tests passed and one real-account case skipped.
  A remaining old display-name expectation in the metadata test was corrected and that test
  rerun successfully. Coverage includes discovery, Binder metadata, transparent UI resources,
  adaptive/legacy launcher icons, selection persistence, appearance dialogs, asynchronous host
  appearance, on-device MIME handlers and OAuth callback handling.
- OAuth checks used the new Google client and the existing Microsoft client with its new
  registered redirect. Browser launch, foreign-state rejection, matching-state callback handling,
  HTTPS exchange of deliberately invalid codes, encrypted test records and refusal/revocation
  paths passed. These tests do not establish a new live-account authorization or provider review.
- The maintainer confirmed both provider-side registration updates. The local configuration check
  confirmed a changed, validly formatted Google client id and unchanged Microsoft client/tenant.
  The final APK's Google scheme matches that configuration; Microsoft uses the new package scheme.
- No live-account send/receive or refreshed Google consent session was repeated for this rename.
  Existing Google project audience restrictions still apply. Tests restored temporary settings;
  the old application was not uninstalled and its data was not cleared.

## Artwork and release receipt

Both supplied 1092 x 1092 RGBA originals are retained byte-for-byte. Their alpha channels match.
UI width ratio is 0.66, adaptive width ratio 0.42, with zero optical offset. The artwork's tonal
folds are preserved: replacing its RGB with a single color would erase the envelope details.
Transparent UI bitmaps are separate from system adaptive resources, whose backgrounds are
fixed light `#FAFAFA` and dark `#212121`. The final antialiased foreground fits the 66 dp safe circle.

The signed release APK contains no native libraries. Its certificate SHA-256 is
`31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
The local APK receipt and the published checksum file record its exact size, SHA-256 and CRC32.
The release tag, CI runs and index admission bind the final published commit; GitHub results are
reported with the release handoff instead of being inferred from these local checks.

The final light/dark screenshots also exposed the Android 7 light-navigation contrast issue.
Build 75 keeps that system bar dark on API 24/25, where dark navigation buttons are unavailable.
