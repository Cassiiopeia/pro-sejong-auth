---
title: Add a new data source
description: The 7-step procedure for adding new public school data to all three languages, with code locations, dependency rules and safety rules.
---

How to add new data the school serves publicly (course lists, menus, other public data). Sources are independent and share only the common layer (`transport`, `errors`), so you can build yours without touching the others.

## Procedure

1. **Propose it in an issue.** Use the `새 정보 출처 제안` (new data source proposal) issue template. Describe where it is served, whether login is needed, the request form (JSON or HTML), the fields, and whether it contains personal data. Information that exists only as HTML is not used as a primary source.
2. **Write the contract first.** Add the method, request and field mapping to `spec/PUBLIC_DATA.md`, one line each, with the date checked and the account type you used.
3. **Create synthetic fixtures and expected results.** Add them to `FIXTURES` and `map_all()` in `scripts/generate-public-data-fixtures.py` and run it. Never copy a real response, and never put in values that identify a person, such as a staff name.
4. **Implement Java, Python and Node in the same PR.** Reuse the common helpers of `SejongPublicData` for requests, cache and failure isolation, and follow the contract's value rules (empty string when missing, `extra`).
5. **Write only the tests you need.** One test that the fixture result equals the expected result is enough. Do not add tests that call the school's servers.
6. **Add a shape check.** Add a row to `SOURCES` in `scripts/probe-public-data.py`. It runs without an account.
7. Go through the [feature exposure checklist](../structure/).

## Where the code goes

For a public-data source. One method maps to three files.

| Language | Method and request | Result model | Test |
|---|---|---|---|
| Python | `python/src/sejongpeia/public_data.py` | `public_data_models.py` | `python/tests/test_public_data.py` |
| Node | `node/src/publicData.ts` | `publicDataModels.ts` | `node/test/publicData.test.mjs` |
| Java | `publicdata/SejongPublicData.java`, mapping in `PublicDataMapper.java` | `publicdata/model/` | `SejongPublicDataTest.java` |

- Method names are `snake_case` in Python and `camelCase` in Node and Java. School fields you do not map go into `extra`.
- Login-required sources are not covered by this procedure. They are entangled with the authentication code (`client`, `parsers`), so agree on the direction in an issue first.
- When you add or change a public method or result field, check that the "Public API" list in [API stability](../../api-stability/) and the [integration examples](../../guides/student-verification/) are still correct.

## Dependency rule

Authentication code and public data do not import each other. Tests enforce it.

| Language | Rule test |
|---|---|
| Python | `python/tests/test_dependency_rules.py` |
| Node | `node/test/dependencyRules.test.mjs` |
| Java | `DependencyRulesTest` |

If a rule fails, move what needs sharing into `transport` or `errors`.

## What goes in spec/

The contract between the school's services and the three implementations lives in `spec/`. When the school changes something, change `spec/` first, then all three implementations in the same PR.

| Kind | Location | Rule |
|---|---|---|
| Request and response contract, field meanings, limits | `spec/<source>.md` | One document per source. Write only what was verified, and say what was not |
| Values shared by the implementations (URLs, error messages) | `spec/contract.json` | The single source. Each language's contract test fails if its constants drift |
| Where the school serves what, dead ends, what was not checked | `spec/REFERENCE_DATA_SURVEY.md` | Read it before searching again. Update it when you look somewhere new |
| Sample responses for tests | `spec/fixtures/` | Synthetic only. Never real responses, personal data, session values or staff names |
| Shape drift detection | `scripts/probe-*.py` | Runs without an account, one entry per source |

## Rules that keep it safe

- Return the school's raw strings. No value lists or code tables in the library, no guessing a code from a name.
- A failure in one source never blocks another, authentication in particular. Return an empty value or the last cached value.
- Do not log or return internal server details (stack traces, session identifiers, IP addresses, password-state flags).
- Be gentle with public pages and endpoints: cache, space requests out, and bound the number of requests per refresh.

See also [Failure isolation and cache](../../understand/failure-isolation-cache/) and [Common rules](../../data/overview/).
