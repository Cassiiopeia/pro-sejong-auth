# AGENTS.md

Rules for AI coding agents working **on** this repository. If you are helping someone **use** the library, read the generated [`llms.txt`](https://cassiiopeia.github.io/pro-sejongpeia/llms.txt) and [`api.json`](https://cassiiopeia.github.io/pro-sejongpeia/api.json) (build output of the docs site, not files in this repo) instead (page: [Using with coding agents](https://cassiiopeia.github.io/pro-sejongpeia/en/guides/agents/)).

## What this is

pro-sejongpeia is a Sejong University library with the **same behavior in three languages**: Java/Spring (`java/`), Python stdlib-only (`python/`), Node.js/TypeScript zero-dependency (`node/`). It does portal login verification (`auth`) and exposes 15 login-free public school data methods (`public_data`). One version number covers all three (`version.yml`, set by the release workflow).

## Layout

| Path | Content |
|---|---|
| `python/src/sejongpeia/{common,auth,public_data}/` | Python package (import `sejongpeia`) |
| `node/src/{common,auth,publicData}/`, `node/src/index.ts` | Node package, tests in `node/test/` |
| `java/src/main/java/kr/suhsaechan/sejongpeia/{common,auth,publicdata,config}/` | Java library (Gradle project `:sejongpeia`) |
| `spec/` | Contract: `PUBLIC_DATA.md`, `PORTAL_FLOW.md`, `RESULT_SCHEMA.md`, `contract.json`, `api-index.json`, `fixtures/` |
| `scripts/` | Fixture generator, probes, docs and API-index checks, live checks |
| `docs/assets/` | Brand images (`sejong-seal.png`, `og.png`): the one original; the docs site copies them at build, never edit copies under `website/` |
| `website/` | Astro Starlight docs site (Korean default, English under `/en/`), deployed by `DOCS-PAGES` when merged to `main` |
| root `*.md`, `docs/` | README, CONTRIBUTING (Korean + `.en.md`), integration examples, API stability |

Public data lives in one file per language: `python/.../public_data/client.py` + `models.py`, `node/src/publicData/client.ts` + `models.ts`, `java/.../publicdata/SejongPublicData.java` + `PublicDataMapper.java` + `model/`.

## Dependency rule (three layers)

`common` imports no source. `auth` and `public_data` import only `common`, never each other. Entry points (`__init__.py`, `index.ts`, Java `config/`) only assemble. If two sources need to share something, move it to `common`.
Enforced by `python/tests/test_dependency_rules.py`, `node/test/dependencyRules.test.mjs`, Java `DependencyRulesTest`.

## Commands (run what you touched; all must pass before you say done)

```bash
cd python && PYTHONPATH=src python3 -m pytest -q              # Python (3.9+)
cd node && npm ci && npm run build && npm test                 # Node (18+); tests import from dist (npm test builds too)
./gradlew :sejongpeia:test :sejongpeia:javadoc --offline       # Java (17+), Javadoc must build too
python3 scripts/check-docs.py                                  # broken relative links, language-switch links in *.md
python3 scripts/check-api-index.py                             # spec/api-index.json vs all three implementations
python3 scripts/probe-public-data.py --offline                 # shape probe on fixtures (no network)
cd website && npm ci && npm run build && node scripts/check-links.mjs   # docs site + link check
```

## Add a new public data source (7 steps, details in `CONTRIBUTING.md` and `website/.../contribute/add-data-source`)

1. Propose it as an issue (source location, login needed or not, request shape, fields, personal data). HTML-only sources are not a base source.
2. Contract first: put the address, request keys and fixed values under `publicData` in `spec/contract.json` (each language keeps its own constants, its contract test compares them), then add a row to the Methods table in `spec/PUBLIC_DATA.md` (request, field mapping, date checked).
3. Synthetic fixture and expected output: extend `FIXTURES` and `map_all()` in `scripts/generate-public-data-fixtures.py`, then run it (`python3 scripts/generate-public-data-fixtures.py`).
4. Implement Java, Python and Node **in the same PR**, using the shared helpers of `SejongPublicData` for request, cache and failure isolation.
5. Add one test per language that compares fixture output to the expected JSON. No test calls the school.
6. Add a line to `SOURCES` in `scripts/probe-public-data.py`.
7. Update `spec/api-index.json` (methods, parameters, Java signatures, return model), then everything in the "exposure" list below. `scripts/check-api-index.py` fails until all three languages and the spec table agree.

## Single source of truth

- `spec/api-index.json` is the one hand-maintained method list. `api.json` (build output, see below) is generated from it plus the Python models at docs build time (`website/scripts/generate-llms.mjs`, hooked as `prebuild`).
- `llms.txt`, `llms-full.txt`, `llms-full.en.txt`, `api.json` are build output (written to `website/public` at docs build, git-ignored, absent from a fresh clone) and generated. Never edit or commit them. Docs order and titles come from `website/site.config.mjs` (`SIDEBAR`) and the docs frontmatter.
- Brand images live only in `docs/assets/` (README uses them directly). `website/scripts/sync-brand-assets.mjs` copies them into `website/` at dev/build time, and the copies are git-ignored.
- `spec/fixtures/public_data/` is generated by `scripts/generate-public-data-fixtures.py`. `CHANGELOG.*`, README version lines and `version.yml` are generated by workflows.
- Docs exposure for a new feature: README (ko/en), `python/README.md`, `node/README.md`, `spec/`, `website/` (ko and en), package keywords.

## Invariants (tests rely on these)

- Java, Python and Node behave identically: same methods, same fields (snake_case in Python, camelCase in Node/Java), same values.
- Values are the school's raw strings. A missing value is an empty string, never null. `views`/`total` are integers, `latitude`/`longitude` numbers. Unused school fields go to `extra`.
- Failure isolation: upstream problems return the last cached value or an empty value and never raise. Only invalid input raises `INVALID_INPUT` (no request sent).
- Fixtures are synthetic and contain no personal data. Never copy a real response. Staff names never appear in `extra`.
- Never log or print a password, a session cookie or a token. Log only what the existing `[sejong ...]` messages log.
- No backward-compatibility promise (maintainer decision): do not add compatibility shims or deprecated aliases.
- Java: Javadoc cannot see Lombok builders, so never name builder types in signatures or `{@link}`. Models use `@Getter @Builder`.
- Comments and docstrings are short Korean one-liners (summary, `input > output`, `A > B` flow). No narrative paragraphs, no change history. Logs are English with a `[feature]` prefix.

## Do not

- Do not call the real portal with credentials in tests or scripts you add. Live checks are `scripts/live-check.sh` / `verify-live.sh`, run by the maintainer's workflow with secrets. Never try wrong passwords (account lock).
- Do not hand-edit generated files (list above), `node/src/common/version.ts`, or lock files without reason.
- Do not mention former project names in code, docs, issues or commit messages.
- Do not change only one language, one doc language (ko + en move together), or only the code without `spec/`.
- Do not use `git add -A`, force push, or `git reset --hard` (other sessions may share the checkout). Stage explicit paths.

## Git

- Branch from `main` (branch name `<date>_#<issue>_<type>_<title>`), PR against `main`. A merge to `main` that touches `website/` deploys the docs.
- Commit message: `<issue title> : <type> : <summary> <issue URL>` in Korean. Do not add co-author trailers. Commit and push only when asked.
