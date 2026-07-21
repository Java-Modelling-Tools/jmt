# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Java Modelling Tools (JMT) is a Java Swing suite for performance evaluation and capacity
planning of queueing network models. It bundles six tools:

- **JSIMwiz** — wizard-driven discrete-event simulator.
- **JSIMgraph** — graphical (canvas) editor/front-end for the same simulation engine.
- **JMVA** — exact/approximate analytical MVA solver for product-form networks.
- **JMCH** — single-station Markov chain simulator/visualizer.
- **JABA** — bottleneck identification via convex hull algorithms.
- **JWAT** — workload characterization / clustering tool for log data (e.g. Apache logs).

## Build & test commands

Requires Maven 3.6.3+ and Java 8+ (compiler target is 1.8; a plain `mvn package`/`mvn compile`
works fine on newer JDKs too).

- `mvn package` — compile and build `JMT-singlejar-<version>.jar` under `target/`.
- `mvn install` / `mvn deploy` — also builds the IzPack installer (`JMT-installer-<version>.jar`).
- `mvn test` — run the JUnit suite. **GUI tests are excluded by default** (see `<excludes>` in
  the `full` profile of `pom.xml`): `JabaGuiTest`, `JmchGuiTest`, `JmvaGuiTest`,
  `JSIMgraphGuiTest`, `JSIMwizGuiTest`, `JwatGuiTest`, `MainGuiTest`, and several JMVA
  batch/data-collection helper classes (`AMVATest`, `AnalyticalTest`, `BatchTest`,
  `CommandLineTest`, `Jmva*ModelTest`, `PauseThreadTest`, `RandomMVAModelGenerator`,
  `ResultsCheck`) — these are long-running/FEST-Swing UI tests or data-generation scripts, not
  part of the regular test loop.
- `mvn test -Dtest=ClassName` — run a single test class (standard Surefire).
- `mvn test -Dtest=ClassName#methodName` — run a single test method.
- JaCoCo is wired into `mvn test` automatically (`prepare-agent` + `report` at the `test` phase);
  report lands at `target/site/jacoco/jacoco.xml`. `extract_coverage.py` at the repo root parses
  that report into a readable line/instruction coverage table
  (`python3 extract_coverage.py [xml_file] [output_file] [--all]`).
- Debug a running instance: the VS Code task "Maven: exec:java debug GraphStartScreen (8000)"
  launches the suite with a JDWP agent on port 8000
  (`MAVEN_OPTS='-agentlib:jdwp=...' mvn -Pfull -DskipTests -Dexec.mainClass=jmt.gui.common.startScreen.GraphStartScreen exec:java`);
  attach with the "Attach to Maven exec:java (8000)" launch config.
- Headless/CLI mode (no GUI): `java -cp <jar> jmt.commandline.Jmt [mva|sim|jsimw|jsimg] <modelfile.xml> [-seed N] [-maxtime N]`
  drives `jmt.jmva.engine.SolverDispatcher` or `jmt.engine.simDispatcher.DispatcherJSIMschema`
  directly against a model XML file — this is what the regression-test generator scripts below
  wrap.
- There's a separate `jmva` Maven profile (`-Pjmva -Dmode=jmva`) that builds a JMVA-only jar
  targeting Java 1.7 with a different main class (`jmt.jmva.gui.JMVAWizard`) — a legacy/standalone
  packaging path, distinct from the default `full` profile.

### Generating regression test fixtures

`JUnit-jsim.pl` and `JUnit-jmva.pl` are helper scripts for turning a hand-built model file into a
regression test: they run the model through `jmt.commandline.Jmt`, split the model file into
`<test>-input.xml` / `<test>-output.xml` fixtures, and stamp out a test method from
`JUnitTemplate-jsim.java` / `JUnitTemplate-jmva.java`. The printed instructions tell you where to
move the generated fixtures (a resource folder next to the test class) and which class to paste
the method into — this is the established way to add new solver/simulation correctness tests, not
something to reinvent by hand.

### `linetesting` integration suite

`src/test/java/linetesting/IntegrationTest.java` is a separate, parameterized JUnit 5 integration
suite (distinct from the regular unit tests) that runs every model file under
`src/test/resources/linetesting/input/{jsim,jsimw,jmva,jaba,jwat}` through the corresponding
engine, tracks per-line bytecode coverage via `linetesting.CoverageTracker`, and writes
`*_coverage.csv` / `timeouts.csv` reports to `src/test/resources/linetesting/output/`. It requires
`target/classes` to already exist (`mvn compile` first) and excludes GUI packages from coverage
(`jmt/framework/gui`, `jmt/gui`, `jmt/jmva/gui`, `jmt/commandline`) since it drives engines
directly rather than through the UI.

## Architecture

### XML is the integration layer between GUI and engines

There is no shared in-memory model between the GUI processes and the solver engines — everything
crosses that boundary as XML validated against schemas in `src/main/resources/jmt/common/xml/`
(loaded via `jmt.common.xml.XSDSchemaLoader`):

- `SIMmodeldefinition.xsd` / `SIMmodeloutput.xsd` — JSIM model + simulation results.
- `JMTmodel.xsd` — JMVA model.
- `JModelGUI.xsd` — JSIMgraph canvas layout.
- `Archive.xsd`, `Results.xsd` — combined project archives / GUI-side results.
- `MVAtoSIM.xslt`, `SIMtoMVA.xslt`, `OUTtoMVA.xslt` — XSLT converters between the JMVA and JSIM
  model formats.

`jmt.gui.common.xml` (`XMLReader`/`XMLWriter`/`GuiXMLReader`/`GuiXMLWriter`/`XMLArchiver`) does the
actual (de)serialization of the shared GUI model to/from these formats. Because a model is just an
XML document, any GUI (wizard or graphical) can produce a file that any engine — or the headless
`jmt.commandline.Jmt` — can consume, and vice versa for results. When changing model semantics,
the schema, the reader/writer, and the engine-side transformer usually all need to move together.

### Two independent solver engines

- **`jmt.engine`** — the discrete-event simulator. `simEngine` (`SimSystem`, `Simulation`,
  `SimEvent`, `EventQueue`) is a generic event-scheduling kernel; `QueueNet` supplies the domain
  model (`NetSystem`, `NetNode`, `QueueNetwork`, `JobClass`/`Job`, `NodeSection`); `NodeSections`
  implements concrete station behaviors (`Queue`, `Server`, `Fork`, `Join`, `Router`, `Delay`,
  `Storage`, `Cache`, ...); `NetStrategies` supplies pluggable policies (routing, service, forking,
  joining, queue put/get, vacation, caching). `simDispatcher.DispatcherJSIMschema` is the bridge
  that reads a `SIMmodeldefinition.xsd` file, builds and runs the network, and writes results back
  per `SIMmodeloutput.xsd`.
- **`jmt.jmva`** — the analytical MVA solver (`jmva/analytical/solvers`, `jmva/engine`), entirely
  separate machinery for exact/approximate product-form analysis. It does reuse some low-level
  `jmt.engine` utilities (math, random engine, logging) and, in `DispatcherJMVAschema`, can invoke
  the simulation engine directly for hybrid solves — but the two are otherwise parallel systems
  connected only through shared XML formats and XSLT conversion, not shared runtime objects.

Don't assume a fix in one engine applies to the other, and don't assume `jmt.engine` changes are
visible to `jmt.jmva` (the dependency mostly runs one direction).

### GUI structure

`jmt.gui.common` is the shared GUI framework — model classes (`definitions/`), probability
distributions, strategy UI mirroring `jmt.engine.NetStrategies`, the XML layer described above,
shared panels/editors, and `startScreen/` (the suite launcher). **JSIMwiz** and **JSIMgraph** both
build heavily on `jmt.gui.common`, editing the same underlying model through a wizard vs. a
graphical-canvas front-end respectively. `jmva`, `jaba`, `jwat`, and `table` are largely
self-contained per-tool GUI packages under `jmt.gui`.

### Entry points

`jmt.gui.common.startScreen.GraphStartScreen` (the default `mainClass` in `pom.xml`) is the suite
launcher screen; clicking a tool icon instantiates that tool's own main class: `JSIMWizMain`,
`JSIMGraphMain`, `JMVAWizard`, `JabaWizard`, `MainJwatWizard` (jwat), `MainWizard` (jmch). Each of
these also has a standalone `main()`. `jmt.commandline.Jmt` is the headless entry point (see
above) for running solves from scripts/CI without any GUI involved.
