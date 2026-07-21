# Contributing to JMT

Thanks for your interest in contributing to Java Modelling Tools (JMT)! This document covers how
to propose changes, the expected workflow, and the conventions used in this repository.

## Ways to contribute

- **Report a bug** using the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Propose an enhancement** using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md).
- **Propose a BSc/MSc thesis project** using the [thesis proposal template](.github/ISSUE_TEMPLATE/thesis_proposal.md).
- **Submit a pull request** fixing a bug, implementing an approved feature, or improving
  documentation/tests.

For anything non-trivial (new modeling capabilities, schema changes, or anything touching both
`jmt.engine` and `jmt.jmva`), please open an issue first to discuss the approach before writing
code — see [`CLAUDE.md`](CLAUDE.md) for an overview of the architecture and how the pieces fit
together.

## Getting started

1. Fork the repository and clone your fork.
2. Requires **Maven 3.6.3+** and **JDK 8+**.
3. Build: `mvn package` (or `mvn install` to also build the installer).
4. Run the test suite: `mvn test`. See `CLAUDE.md` for how single tests, GUI tests, and the
   `linetesting` integration suite are run.
5. Launch a specific tool from source, e.g.:
   ```
   mvn -Pfull -DskipTests -Dexec.mainClass=jmt.gui.common.startScreen.GraphStartScreen exec:java
   ```

## Development workflow

1. Create a topic branch off `main`, named descriptively (e.g. `fix/jsim-fork-join-deadlock`,
   `feature/priority-routing-strategy`).
2. Keep changes focused — one logical change per pull request. Unrelated cleanups make review
   harder and should be a separate PR.
3. Match the existing code style in the file/package you're editing (indentation, brace style,
   naming). There is no enforced formatter/linter configuration in this repo, so consistency with
   surrounding code is the standard to follow.
4. Add or update tests for any behavior change:
   - Unit tests under `src/test/java`.
   - For simulation/analytical solver correctness fixes, use `JUnit-jsim.pl` / `JUnit-jmva.pl` to
     generate input/output fixtures and a test stub from a model file (see `CLAUDE.md` for
     details), rather than hand-writing new fixture formats.
   - If your change affects the model XML format, keep `src/main/resources/jmt/common/xml`
     schemas and any XSLT converters in sync, and check whether the `linetesting` integration
     suite needs new sample models.
5. Run `mvn test` locally before opening a PR. GUI tests (`*GuiTest`) are excluded from the
   default run — if your change affects GUI behavior, note in the PR description how you tested
   it (running the excluded GUI tests locally, or manual testing steps).
6. Update documentation as needed — this includes user-facing docs (`manuals/`, in-app help)
   as well as `CLAUDE.md` if you change the architecture or build/test workflow it describes.

## Commit messages

Write clear, descriptive commit messages in the imperative mood (e.g. "Fix null pointer in
fork-join station", not "Fixed" or "Fixes"). Reference the related issue number when applicable
(e.g. `Fixes #123`).

## Pull requests

- Describe **what** the PR changes and **why**, and which tool(s)/package(s) are affected
  (JSIMwiz, JSIMgraph, JMVA, JMCH, JABA, JWAT, or shared code in `jmt.engine` / `jmt.gui.common` /
  `jmt.common.xml`).
- Link the issue(s) the PR addresses, if any.
- Include a test plan: what you ran (`mvn test`, specific test classes, manual GUI testing) and
  the results.
- Keep the PR scoped to a single concern; large, mixed-purpose PRs take longer to review and are
  more likely to be asked to split.
- Be responsive to review feedback. A maintainer will merge once the PR is approved and CI
  (if configured) passes.

## Reporting bugs and requesting features

Please use the issue templates linked above rather than opening a blank issue — they collect the
environment details (JDK version, build method, affected tool) needed to reproduce problems
efficiently.

## License

By contributing, you agree that your contributions will be licensed under the
[GNU General Public License v2 (or later)](License.txt), the same license that covers this
project.

## Questions

If you're unsure where to start, open an issue describing what you'd like to work on, or check
existing open issues labeled `good first issue` / `help wanted` if available.
