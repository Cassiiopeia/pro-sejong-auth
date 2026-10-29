# API stability policy

[한국어](API_STABILITY.md) | English

An honest description, for developers who depend on this library, of what is public API, what the version number means, and what may change.

## The short answer

| Question | Answer |
|---|---|
| Do you guarantee backward compatibility? | **No.** The project does not aim for backward compatibility (a maintainer decision). Breaking changes can happen even within `1.x`. |
| Then what does the version number mean? | Release order. Whether a release breaks something is told by the **release notes**, not by the number. |
| How do I use it safely? | **Pin the exact version**, read the release notes before upgrading, and follow "Using it safely" below. |
| What if the school changes something? | A daily check runs against the live school servers; when something breaks an issue is opened and fixed quickly. There is no time commitment (SLA). |
| Are the three languages the same? | Features, error codes, result fields and value rules are kept identical and tests enforce it. Naming style and a few language-specific features differ. |

## Public API and internals

"Public API" is what gets a release-note line and a docs update (README, `spec/`) when it changes. "Internal" changes without notice.

| Language | Public API | Internal (do not use directly) |
|---|---|---|
| Python | The names in `__all__` of the `sejongpeia` package: `authenticate`, `authenticate_dhc`, `authenticate_sis`, `fetch_sis_business_periods`, `SejongAuthClient`, `SejongPublicData`, the result and model classes, `SejongAuthError`, `ErrorCode` | the modules and helpers in `sejongpeia.common.*`, `sejongpeia.auth.*`, `sejongpeia.public_data.*` (only the names imported with `from sejongpeia import ...` are public); names and arguments starting with an underscore (`_session_factory`, `_clock`) |
| Node | The functions, classes and types the package entry point (`pro-sejongpeia`) exports. The `exports` field of `package.json` opens only this one entry point | everything the entry point does not export; options starting with an underscore (`_sessionFactory`, `_nowMilliseconds`) |
| Java | The public methods of `auth.service.SuhSejongAuthEngine`, `auth.model.*`, `common.exception.*`, `publicdata.SejongPublicData` and `publicdata.model.*`, `common.util.SejongTlsSupport`, `common.config.SejongAuthProperties`, the property prefix `sejongpeia.*` | `common.util.CommonUtil`, the caller, mapper and fetcher classes in `publicdata` |

Java's `auth.client.*` and `auth.parser.*` are public because using the library without Spring means building and wiring them yourself, as in the [README](../README.en.md) example. Their **constructor shapes can still change** (semi-public). With Spring Boot auto-configuration you are not affected.

Even inside the public API, the following is **outside the guarantee**.

- `rawHtml` and `rawJson` returned by the raw option (`include_raw`, `includeRaw`, `authenticateWithRaw`): school text whose keys and structure follow the school.
- Keys and values of `extra`: they change when the school adds or removes a field. `extra` is only a way to reach fields the library has no name for.
- Error message wording: always branch on the error **code**.
- Log wording, request order, internal cache implementation (the cache defaults are configurable and documented, but the implementation may change).

## Version rule

- The current line is `1.x`. The three languages (Java, Python, Node) share **one version number**.
- The release workflow **bumps the patch number (third digit) automatically.** Minor and major are bumped by the maintainer by hand.
- The number does **not** follow the [SemVer](https://semver.org/) promise (patch is compatible, only major breaks). Behavior-changing changes can ship in an automatic patch release.
- Being at `1` does not mean a stability declaration.
- How breaking changes are announced: every release gets a one-line summary on [GitHub Releases](https://github.com/Cassiiopeia/pro-sejongpeia/releases) and in [CHANGELOG.md](../CHANGELOG.md), and a rename or signature change is stated in that line.

Renames and signature changes (method or field names, package paths, configuration keys) can land at once, with no compatibility alias. Expect that kind of change to be possible again.

### What we still do

Compatibility is not promised, but we try not to surprise you.

- New features prefer **adding** (result fields, methods) over changing existing fields.
- When a name or shape must change, the three languages and the docs change **in one PR**.
- Changes that follow the school aim to keep your code working, but we cannot prevent the school data itself from changing.

## What the three languages keep identical

These must be the same in every language, and tests fail when they drift.

| Kept identical | How it is enforced |
|---|---|
| Request URLs, portal verdict to error code mapping and other shared values | [spec/contract.json](../spec/contract.json) is the single source; each language's contract test compares its constants |
| The 10 error codes (`AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, ...) and their meaning | [spec/RESULT_SCHEMA.md](../spec/RESULT_SCHEMA.md), contract tests |
| Authentication result fields and meaning (only the spelling differs) | RESULT_SCHEMA, expected-value tests on the sanitized sample responses in `spec/fixtures/` |
| The 14 public-data methods, their result fields and value rules (missing value is an empty string, failure isolation, cache) | [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md), one shared expected-result file (`expected_public_data.json`) compared in all three languages |
| Authentication and public data do not depend on each other | dependency-rule tests (Python, Node, Java each) |
| Results against the real portal | a daily live login check in all three languages, field-by-field hash comparison (`scripts/verify-live.sh`) |

**What differs by language** is intentional.

- Naming: Python `snake_case`, Node and Java `camelCase` (for example `authenticate_dhc` / `authenticateDhc` / `authenticateWithDHC`; Java names a few methods differently). The feature table in the README maps them.
- Call style: Python and Java are synchronous, Node returns a `Promise`.
- Language-specific: Spring Boot auto-configuration and the `sejongpeia.*` settings (Java), `SejongTlsSupport` (Java), the old code `INVALID_CREDENTIALS` (kept only in Java, never thrown).
- Date-time type: the authentication time uses each language's own type.

## Deprecation

- A name we want to remove is kept for a while where possible, written up in the docs and release notes, and marked `@Deprecated` in Java. Example: `SejongAuthErrorCode.INVALID_CREDENTIALS` is no longer thrown but remains so existing code compiles.
- **No period is promised.** Something can disappear or change without a deprecation marker. The release notes (see "Version rule") are the only official signal.

## School data is the school's own text

School fields in public data and authentication results are **the strings the school returned, as is.** The library carries no code tables or name mappings.

- Values such as department codes, organization names, cafeteria numbers, schedule titles and enrollment status labels **change when the school changes them.** Do not write code that branches on a list of values (for example `parentUnitName` is a label that changes each academic year and must not be used to decide authentication).
- Date formats also differ per school source (the academic calendar uses `yyyy-MM-dd`, the graduate calendar `MM.dd`). Each format is written in [spec/PUBLIC_DATA.md](../spec/PUBLIC_DATA.md).
- A missing value is an empty string (public data never has `null`; the SIS fields of authentication are `null` when that lookup failed).
- School fields the library does not map land in `extra`; when the school adds a field it can show up in `extra` without a new release.

### When the school changes something

1. A check runs every day at 09:00 KST. The three languages really log in with a correct password (no wrong-password attempts), and the response shapes of the public data sources that need no account are verified.
2. When it detects a problem an issue is opened automatically.
3. The contract documents (`spec/`) and the three implementations are fixed in one PR and released.

Public data does not throw when one source fails: it returns the last cached value or an empty list. So **an empty list cannot tell "no data" from "a problem on the school side".** Decide what it means on the calling side if that matters.

## Using it safely

- **Pin the exact version** of the dependency (for example `pro-sejongpeia==1.2.49`, `"pro-sejongpeia": "1.2.49"`, `kr.suhsaechan:sejongpeia:1.2.49`; 1.2.49 is the latest at the time of writing). A range (`^`, `~=`, `+`) takes breaking changes automatically.
- Read the [release notes](https://github.com/Cassiiopeia/pro-sejongpeia/releases) before upgrading and run your tests first.
- Wrap the library in one thin adapter inside your app, so there is one place to fix when something changes.
- Branch on error **codes**, not messages.
- Do not hang core application logic on `extra`, raw responses or school code values.
- Usage examples are in [INTEGRATION_EXAMPLES.en.md](INTEGRATION_EXAMPLES.en.md).

## Changing this policy

If the policy moves toward promising backward compatibility, this document is updated first and the release notes announce it. Share opinions in [Discussions](https://github.com/Cassiiopeia/pro-sejongpeia/discussions) or an issue.
