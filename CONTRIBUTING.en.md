# Contributing

[한국어](CONTRIBUTING.md) | English

Bug reports, fixes for portal changes and docs improvements are all welcome. Issues and PRs in English or Korean are fine.

Please do not report security vulnerabilities in public issues. Use the private reporting described in [SECURITY.md](SECURITY.md).

## Start with an issue

- Bugs: include the error code (`SejongAuthException.getErrorCode()`) and the method you called. Never post student IDs, passwords or other personal data.
- If authentication broke because the portal changed, first check whether the [live demo](https://cassiiopeia.github.io/pro-sejong-auth/#demo) reproduces it.
- For larger changes, agree on the direction in an issue before opening a PR.

## Repository layout

| Folder | What |
|---|---|
| `java/` | Java / Spring Boot library (`kr.suhsaechan:sejong-univ-auth`) |
| `python/` | Python package (`pro-sejong-auth`, import `sejong_auth`) |
| `node/` | Node.js package (`pro-sejong-auth`, TypeScript) |
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

## Pull requests

- Branch from `main`.
- Add tests when behavior changes. Parser changes need a fixture in `spec/fixtures/` and the same expected values for all languages.
- Make sure no password or personal data ends up in logs or exception messages.

## License

Contributions are released under the [MIT License](LICENSE).
