# AGENTS

This repository keeps its project guidance in two documents:

- Read [README.md](./README.md) first for product scope, build steps, and required checks.
- Read [CONVENTION.md](./CONVENTION.md) for reviewer-enforced coding rules under `app/src/main/`.

Minimum expectations for any change:

- Keep changes consistent with `README.md` and `CONVENTION.md`.
- Run `./gradlew :app:lintDebug` before pushing.
- Do not introduce new `!!` in production code.
- For paged post feeds, keep using the existing Paging overlay and deduplication patterns.
