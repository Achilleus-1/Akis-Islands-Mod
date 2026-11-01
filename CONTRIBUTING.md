# Contributing

Use a focused branch and pull request. Explain the problem, resulting behavior, and verification. Build with Java 21, run `python tools/check_repository.py`, and run `./gradlew build runGameTestServer --no-daemon` before submitting. See [DEVELOPMENT.md](DEVELOPMENT.md).

Preserve the `akis_islands` mod ID, generator codecs, old-save defaults, per-world settings, and deterministic shape behavior. Optional climate must stay opt-in and retain native humidity and biome-selection rules. Keep client classes isolated from dedicated-server initialization.

Treat compatibility checks and actual gameplay tests separately. Do not describe successful loading as proof of village placement, every stream route, or universal compatibility.

Do not commit worlds, modpack jars, build caches, credentials, private server addresses, local IDE files, or machine-specific logs. Use [SECURITY.md](SECURITY.md) for private reports. Contribution acceptance does not change the existing project license.
