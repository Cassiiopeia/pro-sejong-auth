# spec/: the contract between the school's services and the three implementations

Everything the library knows about the school's services lives here. Java, Python and Node follow it; when the school changes something, change this folder first, then all three implementations in the same PR.

## What goes where

| Kind | Location | Tracked | Rule |
|---|---|---|---|
| Request and response contract, field meanings, limits | `spec/<source>.md` (for example `PORTAL_FLOW.md`, `REFERENCE_DATA.md`) | yes | One document per source. Write only what was verified, and say what was not |
| Values shared by the implementations (URLs, error messages) | `spec/contract.json` | yes | The single source. Each language's contract test fails if its constants drift |
| Where the school serves what, dead ends, what was not checked | `spec/REFERENCE_DATA_SURVEY.md` | yes | Read it before searching again. Update it when you look somewhere new |
| Sample responses for tests | `spec/fixtures/` | yes | Synthetic only. Never copy a real response, and never keep personal data, session values or staff names |
| Shape drift detection | `scripts/probe-*.py` | yes | No credentials needed. One entry per source |
| Real responses, screenshots, raw dumps, working notes | your working folder | **no** | Keep them out of the repository |
| Issues, reports, design notes, audit results | `docs/projectops/`, `docs/superpowers/` | **no** (git-ignored) | Process records stay local. Durable knowledge goes into the rows above |

## Adding or changing a source

1. Write or update `spec/<source>.md` and the survey row. State the verified date and which account types were checked.
2. Add a synthetic fixture and a probe entry.
3. Implement it in Java, Python and Node in the same PR, with tests that run on the fixture only (no school server in unit tests).
4. Check the exposure list in [CONTRIBUTING](../CONTRIBUTING.md): README, package READMEs, the project site, package descriptions, release note line.

## Rules that keep it safe

- Raw school strings only: no value lists or code tables in the library, no guessing a code from a name.
- A failure in one source never blocks another (authentication in particular): return an empty value or the last cached value.
- Do not log or return internal server details (stack traces, session identifiers, IP addresses, password-state flags).
- Public pages and endpoints: be gentle (cache, spacing, a bounded number of requests per refresh).
- Each implementation keeps three layers: `common` (errors, transport, version), `auth` and `public_data` (`publicData`, `publicdata`). A source imports only `common` and never another source. A test per language enforces it (see [CONTRIBUTING](../CONTRIBUTING.md)).
