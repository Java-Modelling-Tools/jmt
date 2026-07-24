# Contributing to JMT

Thanks for your interest in contributing to Java Modelling Tools (JMT)! This document covers how to propose changes, the expected workflow, the conventions used in this repository, and an overview of the architecture.

Java Modelling Tools (JMT) is a Java Swing suite for performance evaluation and capacity planning of queueing network models. It bundles six tools:
*   **JSIMwiz**: wizard-driven discrete-event simulator.
*   **JSIMgraph**: graphical (canvas) editor/front-end for the same simulation engine.
*   **JMVA**: exact/approximate analytical MVA solver for product-form networks.
*   **JMCH**: single-station Markov chain simulator/visualizer.
*   **JABA**: bottleneck identification via convex hull algorithms.
*   **JWAT**: workload characterization / clustering tool for log data (e.g. Apache logs).

## Ways to Contribute

*   **Report a bug** using the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md).
*   **Propose an enhancement** using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md).
*   **Propose a BSc/MSc thesis project** using the [thesis proposal template](.github/ISSUE_TEMPLATE/thesis_proposal.md).
*   **Submit a pull request** fixing a bug, implementing an approved feature, or improving documentation/tests.

For anything non-trivial (new modeling capabilities, schema changes, or anything touching both `jmt.engine` and `jmt.jmva`), please open an issue first to discuss the approach before writing code. Please refer to the [Architecture](#architecture) section below for an overview of how the pieces fit together. 

## Getting Started: Build & Test Commands

1.  Fork the repository and clone your fork.
2.  Requires **Maven 3.6.3+** and **Java 8+** (compiler target is 1.8; a plain `mvn package`/`mvn compile` works fine on newer JDKs too).
3.  **Build:** 
    *   `mvn package` - compile and build `JMT-singlejar-<version>.jar` under `target/`.
    *   `mvn install` / `mvn deploy` - also builds the IzPack installer (`JMT-installer-<version>.jar`).
4.  **Run the test suite:** 
    *   `mvn test` - run the JUnit suite. **GUI tests are excluded by default** (see `<excludes>` in the `full` profile of `pom.xml`): `JabaGuiTest`, `JmchGuiTest`, `JmvaGuiTest`, `JSIMgraphGuiTest`, `JSIMwizGuiTest`, `JwatGuiTest`, `MainGuiTest`, and several JMVA batch/data-collection helper classes. These are long-running/FEST-Swing UI tests or data-generation scripts, not part of the regular test loop.
    *   `mvn test -Dtest=ClassName` - run a single test class (standard Surefire).
    *   `mvn test -Dtest=ClassName#methodName` - run a single test method.
    *   JaCoCo is wired into `mvn test` automatically (`prepare-agent` + `report` at the `test` phase); report lands at `target/site/jacoco/jacoco.xml`. `extract_coverage.py` at the repo root parses that report into a readable line/instruction coverage table (`python3 extract_coverage.py [xml_file] [output_file] [--all]`).
5.  **Launch a specific tool from source:** 
    *   `mvn -Pfull -DskipTests -Dexec.mainClass=jmt.gui.common.startScreen.GraphStartScreen exec:java`
6.  **Debug a running instance:** 
    *   The VS Code task "Maven: exec:java debug GraphStartScreen (8000)" launches the suite with a JDWP agent on port 8000 (`MAVEN_OPTS='-agentlib:jdwp=...' mvn -Pfull -DskipTests -Dexec.mainClass=jmt.gui.common.startScreen.GraphStartScreen exec:java`); attach with the "Attach to Maven exec:java (8000)" launch config.
7.  **Headless/CLI mode (no GUI):** 
    *   `java -cp <jar> jmt.commandline.Jmt [mva|sim|jsimw|jsimg] <modelfile.xml> [-seed N] [-maxtime N]` drives `jmt.jmva.engine.SolverDispatcher` or `jmt.engine.simDispatcher.DispatcherJSIMschema` directly against a model XML file.
8.  **Legacy packaging:** 
    *   There's a separate `jmva` Maven profile (`-Pjmva -Dmode=jmva`) that builds a JMVA-only jar targeting Java 1.7 with a different main class (`jmt.jmva.gui.JMVAWizard`) - a legacy/standalone packaging path, distinct from the default `full` profile.

## Development Workflow

1.  Create a topic branch off `main`, named descriptively (e.g. `fix/jsim-fork-join-deadlock`, `feature/priority-routing-strategy`).
2.  Keep changes focused - one logical change per pull request. Unrelated cleanups make review harder and should be a separate PR.
3.  Match the existing code style in the file/package you're editing (indentation, brace style, naming). There is no enforced formatter/linter configuration in this repo, so consistency with surrounding code is the standard to follow.
4.  Add or update tests for any behavior change:
    *   Unit tests under `src/test/java`.
    *   For simulation/analytical solver correctness fixes, use `JUnit-jsim.pl` / `JUnit-jmva.pl` to generate input/output fixtures and a test stub from a model file, rather than hand-writing new fixture formats. These helper scripts turn a hand-built model file into a regression test by running the model through `jmt.commandline.Jmt`, splitting the file into `<test>-input.xml` / `<test>-output.xml` fixtures, and stamping out a test method from `JUnitTemplate-jsim.java` / `JUnitTemplate-jmva.java`. 
    *   If your change affects the model XML format, keep `src/main/resources/jmt/common/xml` schemas and any XSLT converters in sync.
    *   Check whether the `linetesting` integration suite needs new sample models.
5.  Run `mvn test` locally before opening a PR. GUI tests (`*GuiTest`) are excluded from the default run - if your change affects GUI behavior, note in the PR description how you tested it (running the excluded GUI tests locally, or manual testing steps).
6.  Update documentation as needed - this includes user-facing docs (`manuals/`, in-app help) as well as updating this contributing guide if you change the architecture or build/test workflow it describes.

## Architecture

### XML is the integration layer between GUI and engines

There is no shared in-memory model between the GUI processes and the solver engines - everything crosses that boundary as XML validated against schemas in `src/main/resources/jmt/common/xml/` (loaded via `jmt.common.xml.XSDSchemaLoader`):

*   `SIMmodeldefinition.xsd` / `SIMmodeloutput.xsd` - JSIM model + simulation results.
*   `JMTmodel.xsd` - JMVA model.
*   `JModelGUI.xsd` - JSIMgraph canvas layout.
*   `Archive.xsd`, `Results.xsd` - combined project archives / GUI-side results.
*   `MVAtoSIM.xslt`, `SIMtoMVA.xslt`, `OUTtoMVA.xslt` - XSLT converters between the JMVA and JSIM model formats.

`jmt.gui.common.xml` (`XMLReader`/`XMLWriter`/`GuiXMLReader`/`GuiXMLWriter`/`XMLArchiver`) does the actual (de)serialization of the shared GUI model to/from these formats. Because a model is just an XML document, any GUI (wizard or graphical) can produce a file that any engine - or the headless `jmt.commandline.Jmt` - can consume, and vice versa for results. When changing model semantics, the schema, the reader/writer, and the engine-side transformer usually all need to move together.

### Two independent solver engines

*   **`jmt.engine`** - the discrete-event simulator. `simEngine` (`SimSystem`, `Simulation`, `SimEvent`, `EventQueue`) is a generic event-scheduling kernel; `QueueNet` supplies the domain model (`NetSystem`, `NetNode`, `QueueNetwork`, `JobClass`/`Job`, `NodeSection`); `NodeSections` implements concrete station behaviors (`Queue`, `Server`, `Fork`, `Join`, `Router`, `Delay`, `Storage`, `Cache`, ...); `NetStrategies` supplies pluggable policies (routing, service, forking, joining, queue put/get, vacation, caching). `simDispatcher.DispatcherJSIMschema` is the bridge that reads a `SIMmodeldefinition.xsd` file, builds and runs the network, and writes results back per `SIMmodeloutput.xsd`.
*   **`jmt.jmva`** - the analytical MVA solver (`jmva/analytical/solvers`, `jmva/engine`), entirely separate machinery for exact/approximate product-form analysis. It does reuse some low-level `jmt.engine` utilities (math, random engine, logging) and, in `DispatcherJMVAschema`, can invoke the simulation engine directly for hybrid solves - but the two are otherwise parallel systems connected only through shared XML formats and XSLT conversion, not shared runtime objects.

Don't assume a fix in one engine applies to the other, and don't assume `jmt.engine` changes are visible to `jmt.jmva` (the dependency mostly runs one direction).

### GUI structure

`jmt.gui.common` is the shared GUI framework - model classes (`definitions/`), probability distributions, strategy UI mirroring `jmt.engine.NetStrategies`, the XML layer described above, shared panels/editors, and `startScreen/` (the suite launcher). **JSIMwiz** and **JSIMgraph** both build heavily on `jmt.gui.common`, editing the same underlying model through a wizard vs. a graphical-canvas front-end respectively. `jmva`, `jaba`, `jwat`, and `table` are largely self-contained per-tool GUI packages under `jmt.gui`.

### Entry points

`jmt.gui.common.startScreen.GraphStartScreen` (the default `mainClass` in `pom.xml`) is the suite launcher screen; clicking a tool icon instantiates that tool's own main class: `JSIMWizMain`, `JSIMGraphMain`, `JMVAWizard`, `JabaWizard`, `MainJwatWizard` (jwat), `MainWizard` (jmch). Each of these also has a standalone `main()`. `jmt.commandline.Jmt` is the headless entry point for running solves from scripts/CI without any GUI involved.

### Integration Testing (`linetesting`)

`src/test/java/linetesting/IntegrationTest.java` is a separate, parameterized JUnit 5 integration suite (distinct from the regular unit tests) that runs every model file under `src/test/resources/linetesting/input/{jsim,jsimw,jmva,jaba,jwat}` through the corresponding engine, tracks per-line bytecode coverage via `linetesting.CoverageTracker`, and writes `*_coverage.csv` / `timeouts.csv` reports to `src/test/resources/linetesting/output/`. It requires `target/classes` to already exist (`mvn compile` first) and excludes GUI packages from coverage (`jmt/framework/gui`, `jmt/gui`, `jmt/jmva/gui`, `jmt/commandline`) since it drives engines directly rather than through the UI.

## Commit Messages

Write clear, descriptive commit messages in the imperative mood (e.g. "Fix null pointer in fork-join station", not "Fixed" or "Fixes"). Reference the related issue number when applicable (e.g. `Fixes #123`).

## Pull Requests

*   Describe **what** the PR changes and **why**, and which tool(s)/package(s) are affected (JSIMwiz, JSIMgraph, JMVA, JMCH, JABA, JWAT, or shared code in `jmt.engine` / `jmt.gui.common` / `jmt.common.xml`).
*   Link the issue(s) the PR addresses, if any.
*   Include a test plan: what you ran (`mvn test`, specific test classes, manual GUI testing) and the results.
*   Keep the PR scoped to a single concern; large, mixed-purpose PRs take longer to review and are more likely to be asked to split.
*   Be responsive to review feedback. A maintainer will merge once the PR is approved and CI (if configured) passes.

## Reporting Bugs and Requesting Features

Please use the issue templates linked above rather than opening a blank issue - they collect the environment details (JDK version, build method, affected tool) needed to reproduce problems efficiently.

## License

By contributing, you agree that your contributions will be licensed under the [GNU General Public License v2 (or later)](License.txt), the same license that covers this project.

## Questions

If you're unsure where to start, open an issue describing what you'd like to work on, or check existing open issues labeled `good first issue` / `help wanted` if available.