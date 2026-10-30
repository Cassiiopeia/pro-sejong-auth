# Contributing

[한국어](CONTRIBUTING.md) | English

Bug reports, fixes for portal changes and docs improvements are all welcome. Issues and PRs in English or Korean are fine.

Please do not report security vulnerabilities in public issues. Use the private reporting described in [SECURITY.md](SECURITY.md).

Ask questions and share ideas in Discussions rather than issues; [SUPPORT.en.md](SUPPORT.en.md) explains what goes where. Participants follow the [Code of Conduct](CODE_OF_CONDUCT.en.md). Look for the `good first issue` label if you are new, `help wanted` for issues that need a hand, and `data-source` for proposals of new school data sources.

If you work with a coding agent (Claude Code, Cursor, Codex and so on), have it read [AGENTS.md](AGENTS.md) first. It collects the layout, dependency rule, test commands and what not to do.

## Start with an issue

- Bugs: include the error code (`SejongAuthException.getErrorCode()`) and the method you called. Never post student IDs, passwords or other personal data.
- If authentication broke because the portal changed, first check whether the [live demo](https://cassiiopeia.github.io/pro-sejongpeia/#demo) reproduces it.
- For larger changes, agree on the direction in an issue before opening a PR.

## Repository layout

| Folder | What |
|---|---|
| `java/` | Java / Spring Boot library (`kr.suhsaechan:sejongpeia`) |
| `python/` | Python package (`pro-sejongpeia`, import `sejongpeia`) |
| `node/` | Node.js package (`pro-sejongpeia`, TypeScript) |
| `spec/` | Shared contract: portal flow, result schema, sanitized response samples used by every language's tests |

All three languages share one version number (`version.yml`, updated by the release workflow).

## Development setup

```bash
./gradlew build                                   # Java (JDK 17+, Gradle Wrapper included)
cd python && python -m pip install pytest && python -m pytest   # Python 3.9+
cd node && npm ci && npm test                     # Node 18+
```

## Tests against the real portal

Live tests log in to the real Sejong portal, so they only run when you provide an account.

- Python and Node: set `SEJONG_TEST_STUDENT_ID` and `SEJONG_TEST_PASSWORD`, then run the tests as above
- Java: create `java/src/test/resources/application-dev.yml` (in `.gitignore`)

  ```yaml
  sejong:
    auth:
      test:
        student-id: "your student ID"
        password: "your password"
  ```

  The `실제인증_*` tests in `SuhSejongAuthEngineTest` are `@Disabled`; remove it locally and restore before committing.
- All three at once, comparing results field by field (no personal data printed):

  ```bash
  SEJONG_TEST_STUDENT_ID=... SEJONG_TEST_PASSWORD=... scripts/verify-live.sh
  ```

- Correct password only (no wrong-password attempts): `scripts/live-check.sh`. The `PROJECT-SEJONG-LIVE-CHECK` workflow runs it daily and opens an issue when it fails.

> Several wrong passwords in a row can lock the portal account. If a run fails, find the cause before running it again.

## When the portal changes

1. Update `spec/PORTAL_FLOW.md` and `spec/contract.json` (and `spec/fixtures/` if the response shape changed, with personal data removed). Each language's contract test fails until its constants match `spec/contract.json`
2. Fix Java, Python and Node in the **same PR**
3. Run each language's tests and `scripts/verify-live.sh`

## Documentation languages

- The default document is Korean `X.md`; translations are `X.<language code>.md` (for example `README.en.md`). Add a new language the same way.
- The language line at the top of each document links every language of that document (for example `[한국어](README.md) | English | [日本語](README.ja.md)`).
- When you change a Korean document, update its translations in the same PR. If you cannot, say so in the PR description.
- The `## 최신 버전 :` line in `README.md` is updated by the release workflow; do not edit it by hand.
- Check with `python3 scripts/check-docs.py` (broken relative links, missing language links). `PROJECT-SEJONG-DOCS-CHECK` also runs on pull requests.

## Adding a new information source

How to add new data the school serves (course lists, menus, other public data). Sources are independent and share only the common layer (`common`: errors, HTTP transport, version). Authentication (`auth`) is one source just like public data, so you can build yours without touching the others.

1. **Propose it in an issue** (use the `새 정보 출처 제안` template): where it is served, whether login is needed, the request form (JSON or HTML), the fields, and whether it contains personal data. Information that exists only as HTML is not used as a primary source.
2. **Write the contract first**: put the address, request keys and fixed values under `publicData` in `spec/contract.json` (each language keeps its own constants and its contract test compares them), then add the method, request and field mapping to `spec/PUBLIC_DATA.md`, one line each, with the date checked and the account type you used. Add the same method to the agent-facing method list `spec/api-index.json` too (parameters, return model, Java signatures). `python3 scripts/check-api-index.py` fails if it drifts from the three implementations.
3. **Create synthetic fixtures and expected results**: add them to `FIXTURES` and the per-source `map_*()` functions (merged by `map_all()`) in `scripts/generate-public-data-fixtures.py` and run it. Never copy a real response, and never put in values that identify a person, such as a staff name.
4. **Implement Java, Python and Node in the same PR.** Put the request building and mapping in one file per source (`<source>_source.py`, `<source>Source.ts`, `<source>Source.java`) and add only a one-line public method to `SejongPublicData`. Reuse the shared file (`fetcher`) for cache and failure isolation, and follow the contract's value rules (empty string when missing, `extra`).
5. **Write only the tests you need**: one test that the fixture result equals the expected result is enough. Do not add tests that call the school's servers.
6. **Add a shape check** (add a row to `SOURCES` in `scripts/probe-public-data.py`, runs without an account).
7. Go through the exposure list in `When you add a feature` below.

Tests enforce the dependency rule, which has three layers: common (`common`) imports no source, and each source (`auth`, `public_data`) imports only common and never another source (`python/tests/test_dependency_rules.py`, `node/test/dependencyRules.test.mjs`, Java `DependencyRulesTest`). Inside public data, source files (`*_source`) never import each other or the entry point (`client`); they use only the shared files (`fetcher`, `row_reader`, `models`). If a rule fails, move what needs sharing into `common`. The top-level entry points (`__init__.py`, `index.ts`, Spring `config`) only assemble, so they may import every layer.

**Folder layout** (same shape in all three languages)

| Layer | Python `python/src/sejongpeia/` | Node `node/src/` | Java `java/src/main/java/kr/suhsaechan/sejongpeia/` |
|---|---|---|---|
| Common | `common/` (`errors`, `transport`, `version`) | `common/` (`errors`, `transport`, `version`) | `common/` (`exception`, `util`, `http`, `config`) |
| Authentication | `auth/` (`client`, `parsers`, `portal_main_parser`, `models`) | `auth/` (`client`, `parsers`, `portalMainParser`, `models`) | `auth/` (`client`, `parser`, `model`, `service`) |
| Public data | `public_data/` (`client`, `fetcher`, `row_reader`, `models`, `*_source`) | `publicData/` (`client`, `fetcher`, `rowReader`, `queries`, `models`, `*Source`) | `publicdata/` (`SejongPublicData`, `*Source`, `PublicDataFetcher`, `model`) |
| Assembly | `__init__.py` | `index.ts` | `config/` (Spring auto-configuration, TLS setup) |

**Where the code goes** (for a public-data source; one method maps to three files)

| Language | Method and request | Result model | Test |
|---|---|---|---|
| Python | `python/src/sejongpeia/public_data/<source>_source.py` (request, mapping), the public method line in `client.py` | `public_data/models.py` | `python/tests/test_public_data.py` |
| Node | `node/src/publicData/<source>Source.ts` (request, mapping), the public method line in `client.ts`, argument objects in `queries.ts` | `publicData/models.ts` | `node/test/publicData.test.mjs` |
| Java | `publicdata/<source>Source.java` (request, mapping), the public methods in `SejongPublicData.java` | `publicdata/model/` | `SejongPublicDataTest.java` |

- Method names are `snake_case` in Python and `camelCase` in Node and Java. School fields you do not map go into `extra`.
- Login-required sources are not covered by this procedure. Sources cannot import each other, so how a login session would be shared needs a design; agree on the direction in an issue first.
- When you add or change a public method or result field, check that the "Public API" list in the [API stability policy](docs/API_STABILITY.en.md) and the [integration examples](docs/INTEGRATION_EXAMPLES.en.md) are still correct.

## When you add a feature

Building a feature is not the end: it has to show up everywhere people look. Check this list before opening the PR (the PR template has the same checklist).

- README in Korean and English: the feature and a usage example, and the `Result fields` table if fields were added
- `python/README.md`, `node/README.md`: the PyPI and npm package pages
- `spec/`: the contract document, `spec/contract.json`, synthetic fixtures
- The project site (`website/`, Korean and English docs): feature intro, reference, guides. It is deployed automatically when merged to `main`, so update it in the same PR
- Package descriptions and keywords: `python/pyproject.toml`, `node/package.json`, `java/build.gradle`
- A one-line summary for the release notes (the commit message)
- Only promote what is actually supported

## AI and agent contributions

- PRs written with a coding agent are welcome. The person submitting the PR must have run the commands in [AGENTS.md](AGENTS.md) and confirmed they pass, and the PR description should say which agent was used (for example Claude Code or Codex).
- Never put personal data or real school responses in code, fixtures, issues or PRs. Use synthetic values only.
- The review bar is the same as for human-written PRs. The submitter must be able to explain the change.

## Pull requests

- Branch from `main`.
- Add tests when behavior changes. Parser changes need a fixture in `spec/fixtures/` and the same expected values for all languages.
- Make sure no password or personal data ends up in logs or exception messages.

## License

Contributions are released under the [MIT License](LICENSE).
