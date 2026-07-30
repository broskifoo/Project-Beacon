# Contributing to Project Beacon

Thank you for your interest in contributing to Project Beacon! We are building a disaster-resilient communications ecosystem and depend on community contributions to ensure robust, secure, and reliable software.

Please read the following guidelines to ensure a smooth contribution process.

---

## Issue Workflow

Before starting work on any code change, make sure a corresponding issue exists and is assigned to you.

1. **Find or Open an Issue:** Search the issue tracker. If your proposed feature or bug fix isn't represented, open an issue using the appropriate template.
2. **Engage in Discussion:** Discuss the implementation details on the issue.
3. **Claim the Issue:** Leave a comment requesting assignment, and wait for a maintainer to assign it to you.

---

## Branch Naming Conventions

All development branches should be branched from main (or a release branch) and follow this naming format:

```text
type/issue-number-short-description
```

### Allowed Types:
* `feature/` — New feature or functionality (e.g., `feature/102-mesh-routing`)
* `bugfix/` — Bug fix (e.g., `bugfix/45-memory-leak`)
* `docs/` — Documentation updates (e.g., `docs/22-api-spec`)
* `refactor/` — Code restructuring that doesn't change behavior (e.g., `refactor/64-mesh-encapsulation`)
* `chore/` — Build systems, dependencies, CI config, etc. (e.g., `chore/91-docs-workflow`)

---

## Commit Message Conventions

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification. This helps automate changelogs and release notes.

### Commit Format:
```text
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types:
* `feat`: A new feature
* `fix`: A bug fix
* `docs`: Documentation only changes
* `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc.)
* `refactor`: A code change that neither fixes a bug nor adds a feature
* `perf`: A code change that improves performance
* `test`: Adding missing tests or correcting existing tests
* `chore`: Changes to the build process or auxiliary tools/libraries

### Examples:
* `feat(mesh): add multi-hop routing table fallback`
* `fix(radio): resolve serial port lockup on reconnect`
* `docs(srs): update hardware protocol spec references`

---

## Pull Request Process

1. **Fork the Repo:** Create a fork and clone it locally.
2. **Keep it Synced:** Sync your fork with upstream main before submitting a PR.
3. **Tests & Lints:** Ensure your code meets linter guidelines and all tests pass.
4. **Draft PR:** Create a Pull Request using the provided PR template. If the work is incomplete, open it as a Draft.
5. **Link the Issue:** Explicitly state the issue number in the PR description (e.g., `Closes #102`).
6. **Review Iteration:** Respond to reviewer feedback and apply requested changes to your branch.

---

## Review Process

* **Required Reviewers:** Every PR must be approved by at least one CODEOWNER before merge.
* **Checks:** All CI builds, tests, lints, and security scan checks must pass.
* **Merges:** Maintainers will perform a squash-and-merge to keep git history clean.

---

## Documentation Standards

* All documentation files must be written in Markdown (`.md`).
* Document major architecture decisions using ADRs in the `adr/` directory (following the template format in `adr/ADR-0000-template.md`).
* Update the relevant specifications in `docs/` whenever APIs, protocols, hardware interfaces, or designs are modified.

---

## Coding Standards

* **Architecture Isolation:** Avoid coupling module logic. The separation between `beacon-core`, `beacon-mesh`, and `beacon-radio` must be strictly enforced via clean abstraction layers.
* **Testing Requirements:** All new code must be accompanied by relevant unit tests. Core packages require minimum 80% test coverage.
* **Zero Warnings:** All lint warnings and compiler warnings must be fixed or explicitly suppressed with explanation.
