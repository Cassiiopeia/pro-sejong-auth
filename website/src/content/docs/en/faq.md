---
title: FAQ
description: Answers on the Sejong login API, password storage, requirements, authentication failures and locked accounts, what an empty list means and the cache period.
---

## Is there a Sejong University login API?

Yes. pro-sejongpeia is a Java, Python and Node.js library that handles the Sejong University portal login (SSO) for you and also provides school data that needs no login. With a student ID and password it returns enrollment status, major, year, classic-reading certification and contact info. See the [introduction](../start/introduction/) and the [quick start](../start/quickstart/).

## Is my password stored?

No. The library only uses the password for the portal login request. The live demo server also returns the result without storing input. In your own service, never log or persist passwords.

## Is this an official Sejong University library?

No. It is a personal open-source project by a Sejong student and is not affiliated with the university. If the portal changes it may stop working.

## What are the requirements?

| Language | Requirements |
|---|---|
| Java | Java 17 or later, Spring Boot 3.x (it can also be used without Spring) |
| Python | Python 3.9 or later, no dependencies beyond the standard library |
| Node.js | Node 18 or later, zero dependencies, TypeScript types included, ESM and CommonJS |

## Are the results the same in all three languages? Which should I use?

Features, error codes, result fields and value rules are the same and tests enforce it. Only the naming style (`snake_case` / `camelCase`) and a few language-specific features differ. Pick the language that matches your stack. Details are in [API stability](../api-stability/).

## Authentication suddenly fails

Try the [live demo](../#demo) first. If the demo also fails, the portal likely changed. Check the error **code**, not the message ([Error codes](../auth/error-codes/)), and report it in an issue with the error code. Never post a student ID or password. The steps are in [Issues and pull requests](../contribute/issues-and-prs/).

## My portal account is locked

After several wrong passwords the portal locks the account and the library returns `ACCOUNT_LOCKED`. Reset the password at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> after identity verification. Waiting does not unlock it, and the library cannot unlock it for you. The library never retries a rejected login and stops at the first failure. If anyone can type a student ID into your service, limit failed attempts per student ID. ([Login verdict and account lock protection](../understand/login-verdict/))

## An empty list came back: is there no data, or is the school having a problem?

They cannot be told apart. Public data does not throw when one source fails: it returns the last cached value or an empty list. Show a message such as "could not load" next to the result. ([Failure isolation and cache](../understand/failure-isolation-cache/))

## How often is school data refreshed?

Results are cached in memory, 24 hours by default (`cache_ttl_seconds` defaults to 86400). A fresh cache entry means no request to the school, and identical concurrent requests share one call. Create one `SejongPublicData` per process. ([Common rules](../data/overview/))

## Can I call it from a browser or an app?

No. Call it from a server only. Calling it directly from a browser or app exposes the password to third-party code. The Node.js package is server-side only too.

## I think the portal changed. How do I report it?

A check logs in to the real portal every day and opens an issue automatically when something is wrong. If you noticed first, report it in an [issue](https://github.com/Cassiiopeia/pro-sejongpeia/issues/new/choose) with the error code and the method you called. ([Daily live portal check](../understand/daily-live-check/))

## Where do I report a security problem?

Use GitHub's [private reporting](https://github.com/Cassiiopeia/pro-sejongpeia/security/advisories/new), not a public issue. Never include real student IDs, passwords or other personal data. Details are in [Issues and pull requests](../contribute/issues-and-prs/).

## What is the license?

[MIT](https://github.com/Cassiiopeia/pro-sejongpeia/blob/main/LICENSE). The rights to the Sejong University seal belong to Sejong University.
