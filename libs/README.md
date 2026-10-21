# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifact named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- `mail-api.aar` (host module `plugin-api/mail-api`, the Binder contract of roadmap P1.1; staged when that phase lands)

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

Both artifacts are release builds of the host modules; `mail-api.aar` depends on
`common-plugin-api.aar` (the `PluginInfo` parcelable), so the two must come from the same host
commit. When the host contract changes, restage both and extend the lock file in the same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.

Current pair: AutoJs6 6.8.0/build 5697, commit `8be99cba4fa2dfc888d76eaa0e8ebb13b9ece751`. Both release artifacts are built together. The mail contract adds the separate packaged service action and normal permission; existing Binder transaction order is unchanged.
