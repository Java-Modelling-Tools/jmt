# JMT Modularisation — Session Log

Complete record of the refactoring session: what was asked, what was done, what
failed, what was learned, and what remains open.

- **Repository:** `C:\Users\lmacera\Desktop\Claude\jmt\jmt`
- **Branch:** `hotfix/module-refactoring` (base: `main`, last commit `c399b874`)
- **Toolchain:** Maven 3.9.9 · Zulu JDK 21 · source/target 1.8
- **Final state:** 16 modules · `mvn compile test-compile` → BUILD SUCCESS · 52/53 integration tests

> Nothing in this session was committed. All work is in the working tree.

---

## Table of contents

1. [Objective](#1-objective)
2. [Phase log](#2-phase-log)
3. [Final module architecture](#3-final-module-architecture)
4. [All code changes](#4-all-code-changes)
5. [Failed approaches and dead ends](#5-failed-approaches-and-dead-ends)
6. [Incidents](#6-incidents)
7. [Test status](#7-test-status)
8. [Deliverables produced](#8-deliverables-produced)
9. [Corrections to claims made during the session](#9-corrections-to-claims-made-during-the-session)
10. [Open items](#10-open-items)
11. [Reproducing the verification](#11-reproducing-the-verification)

---

## 1. Objective

Original request: refactor the JMT monolith into a multi-module Maven project with
separate modules for the engine, shared GUI, and each tool (JSIMgraph, JSIMwiz, JMVA,
JABA, JWAT, JMCH), splitting each tool into `-core` (computation) and `-gui` layers.

Follow-on requests, in order received:

1. Make the Maven build work
2. Answer architecture questions (why cores depended on `jmt-gui`; why `jmt-jsimg-gui` was the composition root; empty modules)
3. Remove dead/unused classes and fields
4. Move hardcoded strings and magic numbers into config classes
5. Complete all outstanding `TODO`s
6. Write a run guide (Maven + IntelliJ)
7. Fix the failing integration tests — specifically Sec6 parametric and `2FJ_2tasks_nestedOK`
8. Merge `jmt-app` into the root module
9. Create `jmt-file-handler` to centralise readers/writers
10. Clean up obsolete files and empty directories
11. Produce a detailed change report with motivation for each change
12. Answer the questions in `../questions.md`

---

## 2. Phase log

### Phase 1 — Initial refactoring

Split the monolith into modules. Renamed packages to drop the `jmt.engine.` and
`jmt.gui.` prefixes: `jmt.engine.simDispatcher` → `simDispatcher`,
`jmt.gui.common` → `common`, `jmt.gui.jaba` → `jaba`, and so on.

Moved `XMLConstantNames` and `XMLResultsConstants` from `jmt-gui` into
`jmt-engine/simEngine/xml/`, since engine classes (`SimLoader`,
`XMLSimulationOutput`) consume them directly.

### Phase 2 — Getting the build green

Problems hit and resolved:

| Problem | Cause | Fix |
|---|---|---|
| UTF-8 BOM in 536 files | PowerShell `Set-Content -Encoding UTF8` writes a BOM | Read bytes, strip `0xEF 0xBB 0xBF` prefix |
| `${project.basedir}` wrong in submodules | Resolves per-module, not to the reactor root | `${maven.multiModuleProjectDirectory}` |
| Test imports failing | Tests still referenced the `.jmt` package layout | Rewrote imports |

### Phase 3 — Architecture questions

Answered why the core modules appeared to depend on `jmt-gui`. Investigation showed:

- `jmt-jmva-core`'s dependency was **test-scope only**, caused by a single unreferenced 2003 stub (`TestWizardPanel`)
- `jmt-jsimw-core` was an **empty module** — its only class, `JSIMModel`, extends `CommonModel` and had already been moved into `jmt-gui`

Both were cleaned up. See [§4.9](#49-dead-code-removal).

### Phase 4 — TODOs, dead code, config extraction

Implemented three empty `//TODO: implementation` stubs in `ModelChecker`. Removed
dead classes. Extracted magic numbers into `SimulationDefaults`.

> This phase is the origin of the `2FJ_2tasks_nestedOK` failure — see
> [§4.4](#44-correctness) and [§9](#9-corrections-to-claims-made-during-the-session).

### Phase 5 — Integration test debugging

The longest phase. Progression of failure counts across runs:

```
all failing  →  ~40/53  →  50/53  →  51/53  →  52/53
```

Root causes found, in the order they were discovered:

1. `SimLoader.createParameter()` commented out — broke **every** simulation
2. `JSIMWizMain.simulationFinished()` threw `UnsupportedOperationException`
3. Old-style classPath strings not recognised after the package rename
4. Empty XML attributes polluting oracle comparisons
5. Non-deterministic PA seeds causing Sec6 timeouts
6. Modal dialogs blocking the non-EDT test thread
7. `checkForForkJoinRoutingError()` false positive on nested fork-join

### Phase 6 — `jmt-app` merge attempt

Attempted, failed, fully reverted. See [§5.1](#51-merging-jmt-app-into-the-root-module).

### Phase 7 — `jmt-file-handler`

Created the serialisation module. One class (`XMLArchiver`) had to be moved back
to avoid a dependency cycle. See [§4.6](#46-architecture).

### Phase 8 — Cleanup

13 obsolete root files, the stale `src/test/` tree, and 27 empty directories removed.

### Phase 9 — Documentation

Four documents produced. See [§8](#8-deliverables-produced).

### Phase 10 — `questions.md` review

Verified 11 claimed new files (found 2 more), verified 6 claimed new functions
(found 3 were pre-existing stubs), diagnosed the IntelliJ `getUI()` error,
and assessed 9 layout/placement questions. Written to `../answer.md`.

---

## 3. Final module architecture

```
jmt-engine                        ← no JMT dependencies
    ↓
jmt-gui                           ← model definitions + Swing framework
    ↓
jmt-file-handler                  ← NEW: model serialisation
    ↓
jmt-jaba-core  jmt-jwat-core  jmt-jmva-core  jmt-jmch-core
    ↓
jmt-jaba-gui  jmt-jwat-gui  jmt-jmva-gui  jmt-jmch-gui  jmt-jsimw-gui
    ↓
jmt-jsimg-gui                     ← graph editor / integration layer
    ↓
jmt-app                           ← entry points + shade fat-JAR
    ↓
jmt-integration-tests
```

Verified dependency edges (compile scope, `jmt:*` only):

| Module | Depends on |
|---|---|
| `jmt-engine` | — |
| `jmt-gui` | `jmt-engine` |
| `jmt-file-handler` | `jmt-gui` |
| `jmt-jaba-core` | `jmt-engine` |
| `jmt-jwat-core` | `jmt-engine` |
| `jmt-jmva-core` | `jmt-engine` |
| `jmt-jmch-core` | `jmt-engine` |
| `jmt-jaba-gui` | `jmt-jaba-core`, `jmt-gui` |
| `jmt-jwat-gui` | `jmt-jwat-core`, `jmt-gui` |
| `jmt-jmva-gui` | `jmt-jmva-core`, `jmt-gui`, `jmt-jsimw-gui`, `jmt-file-handler` |
| `jmt-jmch-gui` | `jmt-jmch-core`, `jmt-gui`, `jmt-file-handler` |
| `jmt-jsimw-gui` | `jmt-gui` |
| `jmt-jsimg-gui` | `jmt-gui`, `jmt-jmva-gui`, `jmt-jsimw-gui`, `jmt-jaba-gui`, `jmt-file-handler` |
| `jmt-app` | all six tool GUIs |
| `jmt-integration-tests` | `jmt-app` |

---

## 4. All code changes

### 4.1 Critical bugs

**`SimLoader.createParameter()` restored**
`jmt-engine/src/main/java/simEngine/SimLoader.java:556`

```java
// was commented out:
initargs[i] = createParameter((Element) parameterList.item(i));
```

With the call disabled, `initargs` stayed all-null, so every node-section
constructor received null arguments and threw `NullPointerException` on the first
unboxing (typically `capacity.intValue()`). Symptom: *"Problem with section
constructor"* for every model. **This single line broke the entire test suite.**

**`JSIMWizMain.simulationFinished()` implemented**
`jmt-app/src/main/java/jsimwiz/JSIMWizMain.java:1103`

```java
@Override
public void simulationFinished() {
    changeSimActionsState(true, false, false);
}
```

Was an IDE-generated stub throwing `UnsupportedOperationException`.
`DispatcherThread.run()` calls it after every simulation, so the exception killed
the dispatcher thread *after* a successful run — results computed, UI stuck in
"running" state.

**Seed comparison fixed**
`jmt-engine/src/main/java/simEngine/SimLoader.java:170`

```java
// before: if (root.getAttribute(XML_A_ROOT_SEED) != "")
if (!root.getAttribute(XMLConstantNames.XML_A_ROOT_SEED).isEmpty()) {
```

`!= ""` compares object identity. `getAttribute()` returns a fresh String never
interned to the `""` literal, so the condition was always true — including for
absent attributes, making `Long.parseLong("")` throw.

### 4.2 Backwards compatibility

Model files store fully-qualified class names as XML *attribute values*, so
pre-refactoring files still reference the old package paths.

**`SimLoader.resolveClass()`** — new, `SimLoader.java:888–912`, 4 call sites
(563, 570, 610, 743). Tries the name as written, then retries with the legacy
prefix stripped. New-name-first ordering means current files pay no penalty.
Re-throws the *original* exception so error messages name the class as written
in the file.

**`XMLReader.normalizeClassPath()`** — new,
`jmt-file-handler/src/main/java/common/xml/XMLReader.java:2758–2764`, **12 call
sites**. This is string equality, not reflection: the reader compares the file's
`classPath` attribute against `SomeStrategy.getClassPath()`. Old files carry
old-style strings, code returns new-style ones, so comparisons silently failed.
Worst case was `parseServiceStrategy()` failing to recognise `LDStrategy`,
treating the section as a plain distribution, and throwing
`ArrayIndexOutOfBoundsException` downstream.

**`GuiXMLReader` inline normalisation** —
`jmt-jsimg-gui/src/main/java/common/xml/GuiXMLReader.java:254–263`. Same problem
in the parametric-analysis definition loader. Written inline rather than sharing
`normalizeClassPath` because the classes are in different modules and this is the
file's only `Class.forName` call.

**`JabaWizard.switchToSimulator()`** — `jmt-app/src/main/java/jaba/JabaWizard.java:531`.
A fully-qualified `jmt.gui.jsimgraph.mainGui.JSIMGraphMain` written inline in the
method body, missed by the bulk import rewrite.

### 4.3 Robustness and thread safety

The simulation path spans three threads: caller, `DispatcherThread`, and its inner
`PollerThread`.

| Change | File | Why |
|---|---|---|
| `volatile` on `dispatcher`, `batchThread` | `Mediator.java:169,175` | Written on one thread, read on another. Without `volatile` the JMM gives no visibility guarantee — reader could observe `null` indefinitely. Cause of intermittent *"dispatcherThread never initialized"*. |
| `forceFinished()` added | `DispatcherJSIMschema.java:354–361` | `PollerThread` loops on `!isFinished()`. A simulation failing during *initialisation* never set `simFinished`, so the poller spun until test timeout. |
| `forceFinished()` called in both catch blocks | `DispatcherThread.java:83–91` | `killSimulation()` only stops a simulation that *started*. Both handlers needed it — `OutOfMemoryError` is an `Error`, not caught by the `Exception` block. |
| Null guard on `dispatcher.simulator` | `DispatcherThread.java:192–195` | Poller starts before `simulator` is guaranteed non-null; construction failure meant an NPE on a background thread, invisible in the UI. |
| `gui.simulationFinished()` called directly | `DispatcherThread.java:99` | Removed an `instanceof AnimationPanel` downcast — a concrete JSIMgraph type referenced from code meant to work with any `GuiInterface`. |
| Null guard + `synchronized` | `ResultsModel.getMeasureNumber()` | `measures` is null between simulation start and first reported measure. Saving in that window threw NPE. |
| `logFiles != null` check | `Mediator.java:2237` | `File.listFiles()` returns `null` (not empty array) for a non-existent path. `2FJ_2tasks_nestedOK` has `logPath="C:\Users\Calogero\JMT"` baked in — a path on no other machine. |

### 4.4 Correctness

**`ModelChecker.findFirstJoinBFS()` — depth tracking**
`jmt-gui/src/main/java/common/controller/ModelChecker.java:1648–1684`

`checkForForkJoinRoutingError()` verifies all paths leaving a Fork converge on the
*same* Join. The first implementation returned the first Join reached on each
path, with no notion of nesting. For nested fork-join, path A reaches inner-Join-1
and path B reaches inner-Join-2 — they differ, so a valid model was flagged
`FORK_JOIN_ROUTING_ERROR`.

Downstream effect was severe and misleading: an *error* (not a warning) disables
the Continue button in `JModelProblemsWindow`, so `startSimulation()` returned
before constructing the dispatcher. The visible symptom was *"dispatcherThread
never initialized"* — which looks like a threading bug and sent the investigation
down the wrong path for a long time.

Fix: parallel node/depth queues. Inner Forks increment depth, inner Joins
decrement it, only a Join at depth 0 is returned.

> **Important:** this method was an empty `//TODO: implementation` stub in `HEAD`.
> The bug was in new code written during Phase 4, not a refactoring regression.
> See [§9](#9-corrections-to-claims-made-during-the-session).

### 4.5 Determinism

**`PADispatcherThread` seed propagation**
`jmt-jsimg-gui/src/main/java/common/controller/PADispatcherThread.java:190–193`

```java
simulator.setParametricStep(currentStep);
if (!simd.getUseRandomSeed().booleanValue()) {
    simulator.setSimulationSeed(simd.getSimulationSeed().longValue() + currentStep);
}
```

Each What-If step built a fresh `DispatcherJSIMschema` defaulting to a random
seed, ignoring the model setting. Runs weren't reproducible, and steps drawing an
unlucky seed converged slowly enough to exceed the timeout — the cause of
intermittent Sec6 failures.

Adding `currentStep` rather than reusing one seed is deliberate: identical seeds
across steps would correlate the runs and defeat the point of sampling different
parameter values.

### 4.6 Architecture

**`jmt-file-handler` created.** Six classes moved:

| Class | From |
|---|---|
| `XMLWriter` | `jmt-gui` |
| `XMLResultsWriter` | `jmt-gui` |
| `GuiXMLConstants` | `jmt-gui` |
| `PNMLConstants` | `jmt-gui` |
| `XMLReader` | `jmt-jsimg-gui` |
| `XMLResultsReader` | `jmt-jsimg-gui` |

Scope deliberately limited to classes touching only model definitions.
`GuiXMLReader`, `GuiXMLWriter`, `PNMLReader`, `PNMLWriter` and `ModelLoader`
stayed in `jmt-jsimg-gui` — they depend on graph-layout types
(`JSimGraphModel`, `JMTArc`) or Swing.

**`XMLArchiver` moved back.** Initially moved with the others, but it calls
`GuiXMLReader`/`GuiXMLWriter`, producing the cycle
`jmt-file-handler → jmt-jsimg-gui → jmt-file-handler`. Moving it back left one
dangling reference from `XMLReader`, resolved by inlining the one-line DOM lookup:

```java
// XMLReader.java:164 — was XMLArchiver.getSimFromArchiveDocument(doc)
parseXML((org.w3c.dom.Element) doc
    .getElementsByTagName(simEngine.xml.XMLConstantNames.XML_DOCUMENT_ROOT)
    .item(0), model);
```

**Controller interface extraction.** `JabaWizard implements JabaController`,
`JMVAWizard implements JMVAController`. The panels live in `jmt-jaba-gui` /
`jmt-jmva-gui` but take the wizard as a constructor argument, and the wizards live
in `jmt-app` — a module *above* them. The interface lives with the consumer;
the wizard implements it from above.

**`JabaWizard.updatePanels()` rewritten.** The old body called
`panels.get(i).setData(data)` and `.redraw()`, neither declared on `WizardPanel`.
Replaced with the contract JABA panels actually implement:

```java
for (int i = 0; i < panelCount; i++) {
    WizardPanel panel = panels.get(i);
    if (panel instanceof ForceUpdatablePanel) {
        ((ForceUpdatablePanel) panel).retrieveData();
    }
}
```

**`DispatcherThread implements SimulationRunner`** and moved from
`jsimgraph/controller/` to `common/controller/`, so both JSIMgraph and JSIMwiz
can use it.

### 4.7 Test harness

All test-only; none affect production behaviour.

| Change | Why |
|---|---|
| Skip empty attributes in `extractXmlAttributes` | `getAttribute()` returns `""` not `null` for absent attributes. Injected 18 phantom entries for Sec6 models, failing the size check — reported as a numerical failure when the values actually matched. |
| Empty-oracle guard in `checkResults` | Some models ship without saved results. Previously compared empty vs populated and failed on size. |
| Force `autoAppend = LOGGER_AR_REPLACE` | `LOGGER_AR_ASK` is the default and triggers a modal dialog when the log dir has CSVs. Blocked the calling thread with no user present. Fixed in the harness — changing the production default would alter real user behaviour to serve tests. |
| `startModelCheckerDialogWatcher()` + `dismissContinueButtonIfPresent()` | `startSimulation()` shows a modal `JModelProblemsWindow` on the calling thread (non-EDT in tests), so `show()` blocked and `continued()` — default `false` — was never reached. Daemon thread polls every 50 ms and clicks Continue. |
| `TEST_TIMEOUT` 5 → 10 min | PA models run 5 sequential simulations at 20M samples. Raised *after* the seed fix, so it didn't mask the real bug. |
| `THREAD_INITIALIZATION_WAIT_TIME` 50 → 200 (5 s → 20 s) | Tolerance increase, not a fix. Real causes were the `volatile` and fork-join issues. |

Three details in the dialog watcher are deliberate:

- Only clicks **enabled** buttons — Continue is disabled when real errors exist, so it dismisses warnings but never suppresses a genuine error
- **Daemon** thread — cannot keep the JVM alive if a test fails early
- Interrupted in a **`finally`** — does not leak across tests

### 4.8 Build configuration

| Change | Why |
|---|---|
| Root → `packaging=pom` + 16-module list | Structural core of the refactoring |
| `${basedir}` → `${maven.multiModuleProjectDirectory}` (3 sites) | `${basedir}` resolves per-module; once multi-module it pointed at `jmt-engine/` etc., breaking `globalsettings.conf` and the bundled repo URL |
| Shade `mainClass` → `common.startScreen.GraphStartScreen` | Manifest `Main-Class` is an unvalidated string — stale value builds fine and fails at launch |
| `<inherited>false</inherited>` on antrun + izpack | Otherwise all 15 children each try to build the installer staging area |
| `failonerror="false"` on the staging JAR copy | The shaded JAR is produced by `jmt-app`, not the root, so it may not exist when the root's antrun task runs |
| `xercesImpl` moved root → `jmt-engine` | Root deps are inherited by all 16 modules. Only the engine uses Xerces `DOMParser`/`SAXParser` directly |
| `jmt-file-handler` added to 3 modules | Found by grepping imports of the six moved classes, not by guessing |
| Removed `jmt-gui` (test) + `uispec4j` from `jmt-jmva-core` | `uispec4j` was a phantom dep — zero imports project-wide |
| Removed redundant `jmt-engine` from `jmt-jmch-gui` | Already transitive twice over |
| `<scope>test</scope>` on `fest-swing`, `commons-io` | Compile-scope in a module with only `src/test/java` |

> Note: the antrun copy tasks still use `${basedir}` — correctly, because
> `<inherited>false</inherited>` pins them to the root.

### 4.9 Dead code removal

| Item | Reason |
|---|---|
| `CachedBDMap` | Zero references project-wide |
| `DoubleArrayListOld` | Superseded copy kept beside its replacement |
| `sendBroadcast` overloads | No call sites |
| `TestWizardPanel.java` | 2003 stub, unreferenced, Surefire-excluded — yet the sole reason `jmt-jmva-core` pulled the GUI framework onto its test classpath |
| `jmt-jsimg-core/`, `jmt-jsimw-core/` | Directories containing only a `pom.xml`; contents already merged into `jmt-gui`; already absent from the reactor |

### 4.10 Repository cleanup

**13 root files removed:**

`build.properties` (Ant-era, named the pre-rename main class) ·
`global.properties` · `buildNumber.properties` (2020 plugin output) ·
`dependency-reduced-pom.xml` (shade artifact) · `JUnit-jmva.pl` ·
`JUnit-jsim.pl` (pre-JUnit Perl runners) · `JUnitTemplate-jmva.java` ·
`JUnitTemplate-jsim.java` (loose `.java` outside any source root) ·
`Documentation.html` · `Download.html` · `index.html` (old website) ·
`RUNNING.md` · `refactoring-report.html` (superseded by `docs/`)

Root now holds only `pom.xml`, `README.txt`, `License.txt`, `CONTRIBUTING.md`
(plus `docs/`, `build_util/`, `src/main/resources/` and the module dirs).

**`src/test/` removed** — 28 Java files with pre-rename imports, a stale duplicate
of the corrected copies now in `jmt-app/src/test/`. Because the root is
`pom`-packaged these were never compiled, so the two copies could silently drift.

**`src/main/java/` removed** — empty shell.
`src/main/resources/` deliberately kept: the POM reads `globalsettings.conf` and
the IzPack assets from it.

**27 empty package directories removed** across `jmt-engine`, `jmt-gui`,
`jmt-jmva-core`, `jmt-jsimg-gui`, `jmt-jwat-core`, `jmt-integration-tests`.
Git doesn't track directories, so emptied package folders persist after moves.

### 4.11 New classes created (13)

| Class | Module |
|---|---|
| `EngineNodeConstants` | `jmt-engine` |
| `SimulationDefaults` | `jmt-engine` |
| `SimulationThread` | `jmt-engine` |
| `SimulationRunner` | `jmt-gui` |
| `SimulationRunnerFactory` | `jmt-gui` |
| `GraphCursorController` | `jmt-gui` |
| `GraphRefreshable` | `jmt-gui` |
| `JSIMWizController` | `jmt-gui` |
| `JabaController` | `jmt-jaba-gui` |
| `JabaWizardPanel` | `jmt-jaba-gui` |
| `JMVALauncherService` | `jmt-jmva-core` |
| `JMVAController` | `jmt-jmva-gui` |
| `DispatcherThreadFactory` | `jmt-jsimg-gui` |

---

## 5. Failed approaches and dead ends

### 5.1 Merging `jmt-app` into the root module

**Requested, attempted, fully reverted.** Two independent Maven constraints make
it impossible:

**Constraint 1 — packaging.** Maven 3 requires any project declaring `<modules>`
to use `pom` packaging:

```
'packaging' with value 'jar' is invalid. Aggregator projects require 'pom' as packaging.
```

`pom` packaging compiles no sources and produces no JAR.

**Constraint 2 — dependency inheritance.** Parent `<dependencies>` are inherited
by every child. Declaring the tool GUIs at the root made each depend on itself:

```
'dependencies.dependency[jmt:jmt-jsimg-gui:latest]' for jmt:jmt-jsimg-gui:latest
is referencing itself.
```

Workarounds tried and rejected: explicit `compile`/`testCompile`/`jar` plugin
bindings on the `pom`-packaged root (doesn't solve constraint 2);
`<type>jar</type>` on the integration-tests dependency (same).

**Conclusion:** `jmt-app` as a *child* module is the only arrangement Maven
permits — as a child, its dependencies are not inherited by its sibling GUI
modules. Moving the files to the root directory changes nothing, because both
constraints are about the POM, not file location.

### 5.2 Moving all readers/writers into `jmt-file-handler`

The original plan was a "full serialization layer" including `GuiXMLReader`,
`GuiXMLWriter` and `XMLArchiver`. Blocked by the `XMLArchiver → GuiXMLReader`
cycle. Resolved by scoping the module to definition-only serialisers.

### 5.3 Misdiagnosis of `2FJ_2tasks_nestedOK`

Considerable time was spent on the wrong hypothesis. The symptom —
*"dispatcherThread never initialized"* — pointed at threading. Fixes attempted
in order:

1. `volatile` on the dispatcher fields (a real bug, but not this one)
2. Raised `THREAD_INITIALIZATION_WAIT_TIME` (tolerance, not a fix)
3. `autoAppend = REPLACE` to dismiss the log dialog (necessary, not sufficient)
4. Dialog watcher clicking Continue (necessary, not sufficient — the button was
   *disabled*, because the model reported an **error**, not a warning)
5. **Actual cause:** the new `checkForForkJoinRoutingError()` false-positived on
   nested fork-join

**Lesson:** the dialog watcher only clicks *enabled* buttons. When it didn't fire,
that was itself the diagnostic signal — a disabled Continue means a real error
was reported, which should have redirected attention to `ModelChecker` sooner.

---

## 6. Incidents

### 6.1 `jmt-app` deleted and recovered

During the revert of the root-merge attempt, `rm -rf jmt-app/` removed the
directory. Recovery via `git checkout -- jmt-app/` failed:

```
error: pathspec 'jmt-app/' did not match any file(s) known to git
```

**Cause:** `jmt-app/` was **untracked** — created during this session and never
committed. Git cannot restore untracked files.

**Recovery:** the source files existed in the git *index* under their original
pre-refactoring paths (staged as renames). Reconstructed by:

1. `git restore` on the staged paths in `jmt-gui/` and `src/`
2. Recreating the `jmt-app/` directory structure
3. Moving the restored files in
4. Rewriting their imports (the restored copies had pre-rename imports)
5. Re-applying the `simulationFinished()` fix, which had been lost
6. Re-adding `implements JabaController` / `implements JMVAController`
7. Rewriting `JabaWizard.updatePanels()`

**Lessons:**
- Untracked work is not recoverable through git — commit before destructive operations
- `git status` showing `RM`/`RD` entries means the content is in the index and recoverable from there
- Restored files carry their *original* content, so session fixes applied to them must be re-applied

### 6.2 Regex cleanup broke compilation

Removing debug logging with a multi-line regex stripped the
`System.err.println(` opening but left the string concatenation:

```java
public void startSimulation() {
        + " containsResults=" + model.containsSimulationResults() + " …");
```

Three sites in `Mediator.java`. Fixed manually.

**Lesson:** regex removal of multi-line statements is unsafe; match the full
statement including its terminator, or delete by line range.

---

## 7. Test status

Final run: **53 tests, 1 failure, 0 errors**

```bash
mvn test -pl jmt-integration-tests -am --no-transfer-progress
```

| Test | Status | Note |
|---|---|---|
| 50 of 53 | pass | Includes all Sec6 parametric tests and `2FJ_2tasks_nestedOK` |
| `NOTgoodSJF_SJF.JSIMG` | flaky | Pre-existing numerical mismatch; passed in the final run, failed in earlier ones |
| `SEED1test_3Job2Cl2CS2FJ_2NEWparameters.jsimg` | fail | Pre-existing numerical mismatch |

Both remaining failures predate this work and were explicitly excluded from scope
by the user ("The other two were already failing before").

> They are **not** known to be correct — they are known to be **unchanged**.
> Whether the stored oracles or the solver are at fault is still open.

Run duration is 23–34 minutes depending on machine load.

---

## 8. Deliverables produced

| File | Content |
|---|---|
| `docs/refactoring-report.html` | Architecture report: 6-layer dependency stack, SVG dependency graph with port spreading, all 16 module cards, key decisions, bug list, test status |
| `docs/running.md` | Run guide: prerequisites, build, fat JAR, per-tool main classes, IntelliJ setup, CLI flags, troubleshooting |
| `docs/code-changes.html` | 42 change entries, each with file, before/after code, and a dedicated motivation block |
| `../answer.md` | Answers to `questions.md` with verification method for every claim |
| `SESSION.md` | This file |

All HTML documents share one design system, support light/dark themes, and are
self-contained (no external dependencies).

---

## 9. Corrections to claims made during the session

Two statements made earlier in this session were later disproved by verification.
Both are recorded here rather than quietly dropped.

### 9.1 `DEBUG` fields were never removed

An earlier progress summary listed "removal of unused `DEBUG` fields" among the
dead-code cleanups. Verification found **14 `DEBUG` declarations still present and
still referenced**, including:

```
JabaWizard.java:81        (used at line 635)
JMVAWizard.java:72        (used at line 714)
ExactConstants.java:37    (= true)
SimSystem.java:38 · CircularEventQueue.java:35 · ResultsCheck.java:55
+ 8 solver classes
```

**No `DEBUG` field was removed.** Several are genuine debug-logging switches and
would need review before removal.

### 9.2 The fork-join check was new code, not a regression

`docs/code-changes.html` entry **D1** describes the nested fork-join bug as fixing
a rewrite that regressed during refactoring. That is wrong.

`HEAD` contained:

```java
private void checkForForkJoinRoutingError() {
    //TODO: implementation
}
```

The method existed but did nothing. It was implemented during Phase 4 in response
to the "complete all TODOs" request, and the first implementation was depth-blind.
The bug was in new code, not a regression.

The same applies to `checkForBcmpPsNonRationalDistributionWarning()` and
`checkForBcmpLcfsPrNonRationalDistributionWarning()` — both were empty stubs whose
bodies were written in this session.

**`docs/code-changes.html` entry D1 still needs this correction applied.**

---

## 10. Open items

### Known defects

| Item | Detail |
|---|---|
| `getBcmpServersWithNonRationalDistribution()` | Declared at `ModelChecker.java:1198`, **no call site anywhere**. Either wire it into `JModelProblemsWindow` to list offending stations, or delete it. |
| `docs/code-changes.html` entry D1 | Contains the incorrect framing described in §9.2 |
| Duplicate `CommandLineTest` | Same FQN in `jmt-app/src/test/java/jmt/jmva/` and `jmt-jmva-core/src/test/java/jmt/jmva/` |

### Layout inconsistencies

| Item | Detail |
|---|---|
| `jmt/jmch/distributions/distributions/` | Doubled directory — a mistake, not a decision |
| `jmt-jmva-gui` three-way split | `common/` (3 files), `jmt/` (27), `jmva/` (5). The latter two should merge into `jmva/` |
| `JMVAWizard` package | `jmt.jmva.gui` while every other wizard is prefix-free (`jaba`, `jsimwiz`, `common.startScreen`) |
| `jmt-jmva-core/jmt/jmva/analytical` | Three levels of nesting in a JMVA-only module. Flattening touches imports in 3 other modules |
| Two installer asset locations | `build_util/` and `src/main/resources/jmt/izpack/`. Only the latter is wired into the POM (`pom.xml:188`) |

### Test placement

| Proposal | Verdict |
|---|---|
| `jmt/engine` tests → `jmt-engine` | ❌ They import `jmt.commandline.Jmt` (in `jmt-app`) — end-to-end CLI tests, not unit tests |
| `jmt/gui` tests → `jmt-gui` | ❌ All 7 drive entry points in `jmt-app` |
| `jmt/jmva` tests → `jmt-jmva-core` | ✅ Viable once the duplicate `CommandLineTest` is resolved |
| `jmt/util` matchers | Already in `jmt-app/src/test/`; could flatten `jmt/util` → `util` for consistency |

### Environment

**IntelliJ `getUI()` error** in `JmtJGraph.java` — not a code defect:

- The file is byte-identical to `HEAD` apart from its `package` line
- It does not declare `getUI()`; it only does `extends JGraph`
- `org.jgraph.plaf.GraphUI extends javax.swing.plaf.ComponentUI`, so the
  covariant return is legal
- `mvn compile` succeeds on JDK 21

Cause is IDE configuration — most likely a stale index after 1,300+ file moves.
Remedy: Maven → Reload project, then Invalidate Caches → Restart. Unconfirmed,
as it cannot be reproduced outside the IDE.

### Not addressed

- The two pre-existing numerical test failures (§7) — excluded from scope
- Nothing in this session has been committed

---

## 11. Reproducing the verification

```bash
# Build
mvn compile test-compile --no-transfer-progress
# → BUILD SUCCESS, 16 modules

# Integration tests (23–34 min)
mvn test -pl jmt-integration-tests -am --no-transfer-progress
# → Tests run: 53, Failures: 1, Errors: 0

# Find genuinely new classes vs HEAD
git ls-tree -r --name-only HEAD | grep '\.java$' | sed 's|.*/||' | sort -u > /tmp/h.txt
find jmt-* -name '*.java' -not -path '*/target/*' | sed 's|.*/||' | sort -u > /tmp/n.txt
comm -13 /tmp/h.txt /tmp/n.txt
# → 13 classes

# Check whether a method pre-existed
git show HEAD:src/main/java/jmt/gui/common/controller/ModelChecker.java \
  | grep -A5 'private void checkForForkJoinRoutingError'

# Per-module jmt dependencies
for p in */pom.xml; do
  m=$(dirname "$p")
  deps=$(grep -o '<artifactId>jmt-[a-z-]*</artifactId>' "$p" \
         | sed 's/<[^>]*>//g' | grep -v "^$m$" | tr '\n' ' ')
  printf "%-24s %s\n" "$m" "${deps:-(none)}"
done

# Confirm no empty source directories
find . -path '*/target' -prune -o -type d -empty -print | grep src
# → (no output)
```

Environment note: `mvn` resolves to
`C:\Users\lmacera\.m2\wrapper\dists\apache-maven-3.9.9-bin\…\bin\mvn.cmd`
with `JAVA_HOME=C:\Program Files\Zulu\zulu-21`.
