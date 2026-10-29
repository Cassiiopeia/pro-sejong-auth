---
title: Issues and pull requests
description: How to open an issue first, what never to post, labels and Discussions, private security reports, pull request rules and the license.
---

Bug reports, fixes for portal changes and docs improvements are all welcome. Issues and PRs in English or Korean are fine.

## Start with an issue

- Bugs: include the error code (`SejongAuthException.getErrorCode()` and so on), the method you called, and the language and version.
- If authentication broke because the portal changed, first check whether the [live demo](../../#demo) reproduces it.
- For larger changes, agree on the direction in an issue before opening a PR.
- For a new school data source, use the `새 정보 출처 제안` (new data source proposal) template. The steps are in [Add a new data source](../add-data-source/).

:::danger
Never post student IDs, passwords, names, contact details, session values or raw real responses in an issue, PR or comment. Use field names and made-up values only.
:::

## Labels

| Label | Use |
|---|---|
| `good first issue` | Fits someone who is new |
| `help wanted` | Issues that need a hand |
| `data-source` | Proposals of new school data sources |

## Where to post

| You want to | Go to |
|---|---|
| Ask a usage question, suggest something, share a data source idea | [Discussions](https://github.com/Cassiiopeia/pro-sejongpeia/discussions) |
| Report a bug (a reproducible problem) | [Issues](https://github.com/Cassiiopeia/pro-sejongpeia/issues/new/choose) |
| Report a vulnerability | [Private reporting](https://github.com/Cassiiopeia/pro-sejongpeia/security/advisories/new), not a public issue |
| Fix or add something yourself | [CONTRIBUTING.en.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/CONTRIBUTING.en.md) |

Check the [FAQ](../../faq/) and the [error codes](../../auth/error-codes/) first. This is a personal open-source project and answers come from volunteers, so they can take time.

## Security reports

This library handles Sejong University portal passwords, so security reports are taken seriously. **Do not open a public issue.**

1. Go to the repository's [Security tab](https://github.com/Cassiiopeia/pro-sejongpeia/security).
2. Click **Report a vulnerability**.
3. Describe the issue, the affected version and the steps to reproduce. Never include real student IDs, passwords or other personal data.

You can expect a first response within 7 days. Only the latest released version receives security fixes. The scope is this library's code, for example leaking passwords or personal data through logs, exceptions or results, unsafe TLS handling, or session mix-ups between calls. Details are in [SECURITY.md](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/SECURITY.md).

## Pull request rules

- Branch from `main`.
- Add tests when behavior changes. Parser changes need a fixture in `spec/fixtures/` and the same expected values for all languages.
- When the portal changed, fix the three languages in the same PR. The steps are in [Repository structure](../structure/).
- Make sure no password or personal data ends up in logs or exception messages.
- For a new feature, go through the feature exposure checklist.

## Code of conduct and license

Participants follow the [Code of Conduct](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/CODE_OF_CONDUCT.en.md). Contributions are released under the [MIT License](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/LICENSE).
