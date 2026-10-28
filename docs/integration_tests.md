# JMT Integration Testing Guide

This guide provides a comprehensive overview of the integration test suite in the `jmt-integration-tests` module: how it is architected, how tests validate solver outputs, how code coverage is measured per test model, and all the ways to execute the tests via IntelliJ IDEA, Maven, and direct Java commands.

---

## Table of Contents
1. [Overview & Architecture](#1-overview--architecture)
2. [Test Structure & Supported Engines](#2-test-structure--supported-engines)
3. [How Verification Works (Oracles & SMAPE)](#3-how-verification-works-oracles--smape)
4. [Code Coverage Tracking (JaCoCo Integration)](#4-code-coverage-tracking-jacoco-integration)
5. [Prerequisites Before Running Tests](#5-prerequisites-before-running-tests)
6. [Execution Methods](#6-execution-methods)
   - [Method 1: Maven Command Line (Recommended)](#method-1-maven-command-line-recommended)
   - [Method 2: IntelliJ IDEA](#method-2-intellij-idea)
   - [Method 3: Direct Java Command Line (JUnit Console Launcher)](#method-3-direct-java-command-line-junit-console-launcher)
7. [Downstream Analysis & Optimization Tools](#7-downstream-analysis--optimization-tools)
8. [Troubleshooting & FAQs](#8-troubleshooting--faqs)
9. [Adding New Test Models & Oracles](#9-adding-new-test-models--oracles)

---

## 1. Overview & Architecture

The integration testing framework in JMT is located in the `jmt-integration-tests` module (package `linetesting`). Its goal is to perform end-to-end regression and verification testing by running hundreds of real simulation and analytical models through JMT solvers and verifying the numerical accuracy of their results against stored baselines (oracles).

### Key Architectural Characteristics

- **Headless Execution (`HeadlessSolver.java`)**:
  Historically, JMT tools (`JSIMGraphMain`, `JSIMWizMain`, `JMVAWizard`, `JabaWizard`) were wrapped in Swing `JFrame` instances. `HeadlessSolver` bypasses GUI window creation entirely, invoking the underlying XML parsers (`common.xml`), simulation engines (`simDispatcher`), and analytical algorithms directly. This makes the tests fast, deterministic, and runnable in headless environments such as CI/CD pipelines and servers without display servers (X11 / Wayland / Windows desktop).

- **Data-Driven Parameterized Tests (`IntegrationTest.java`)**:
  Tests are structured as JUnit 5 parameterized tests using `@MethodSource` test providers. Each provider scans an input directory (`src/test/resources/linetesting/input/<engine>`) and streams all model files into test instances.

- **Per-Test JaCoCo Coverage (`CoverageTracker.java`)**:
  Before each individual test model runs, execution data in the JaCoCo runtime agent is flushed. After the model finishes, the exact bytecode instructions and source lines executed are captured and recorded in coverage CSV files.

---

## 2. Test Structure & Supported Engines

The integration tests evaluate four core modeling paradigms in JMT:

| Engine / Component | Folder Name | Test Method | File Format | Description |
| :--- | :---: | :--- | :---: | :--- |
| **JSIMgraph** | `jsim` | `testJSIMGraph` | `.jsimg` / `.jsim` | Queueing network simulation models with arbitrary topologies, routing, fork/join, and stations |
| **JMVA** | `jmva` | `testJMVA` | `.jmva` / XML | Analytical Mean Value Analysis models (Exact, Approximate, Bard-Schweitzer, etc.) |
| **JSIMwiz** | `jsimw` | `testJSIMWiz` | `.jsimw` | Wizard-constructed queueing simulation models |
| **JABA** | `jaba` | `testJABA` | `.jaba` | Asymptotic Bound Analysis models |

### Directory Layout
```text
jmt-integration-tests/
├── src/test/java/linetesting/
│   ├── IntegrationTest.java           # Main JUnit 5 test entry point
│   ├── IntegrationTestConstants.java  # Constants, paths, thresholds, timeouts
│   ├── IntegrationTestUtil.java       # Model loaders, XML extractors, numerical comparator
│   ├── HeadlessSolver.java            # Headless solver orchestrator (no GUI)
│   ├── CoverageTracker.java           # JaCoCo runtime coverage extractor
│   └── FileUtil.java                  # File I/O helpers
└── src/test/resources/linetesting/
    ├── input/                         # Baseline test model files (oracles)
    │   ├── jsim/                      # JSIMgraph simulation models
    │   ├── jmva/                      # JMVA analytical models
    │   ├── jsimw/                     # JSIMwiz models
    │   └── jaba/                      # JABA models
    ├── output/                        # Test run outputs & generated coverage CSVs
    └── scripts/                       # Python analysis & optimization tools
```

---

## 3. How Verification Works (Oracles & SMAPE)

### Extraction
1. When a test model runs, `HeadlessSolver` saves the solved model to `src/test/resources/linetesting/output/<engine>/<model-name>`.
2. `IntegrationTestUtil` extracts numerical performance indices from both the original baseline model (oracle) and the newly generated output using XPath:
   - **Simulation Models (JSIMgraph / JSIMwiz)**: `//results/measure/@finalValue`
   - **Analytical Models (JMVA)**: `//solutions//measure/@meanValue`

### Comparison Metric: SMAPE
Because stochastic simulation models inherently produce minor statistical variations across runs, JMT does not use strict equality for floating-point results. Instead, it computes the **Symmetric Mean Absolute Percentage Error (SMAPE)** for every metric:

$$\text{SMAPE} = \frac{|\text{actual} - \text{expected}|}{\frac{|\text{actual}| + |\text{expected}|}{2}} \times 100\%$$

- **Stability Check**: A metric passes if $\text{SMAPE} < \text{STABILITY\_TRESHOLD}$ (default: **5.0%**).
- **Exact String Fast-Pass**: If the serialized strings match identically, calculation is skipped.
- **Functional Verification**: If a model file does not have pre-stored results, the test verifies that the solver executes to completion without crashing or throwing exceptions.

### Timeouts
To guard against infinite loops or hanging simulations (e.g. complex parametric analyses with millions of samples), each test is guarded by an assertion timeout (`TEST_TIMEOUT = 10` minutes). If a test times out, it is logged to `src/test/resources/linetesting/output/timeouts.csv`.

---

## 4. Code Coverage Tracking (JaCoCo Integration)

The test suite includes fine-grained coverage tracking capable of measuring which Java lines are hit by each individual test model:

1. **Modules Analyzed**: Non-GUI core modules (`jmt-engine`, `jmt-jmva-core`, `jmt-jaba-core`, `jmt-jwat-core`, `jmt-jmch-core`).
2. **Runtime Reflection**: `CoverageTracker` accesses `org.jacoco.agent.rt.RT.getAgent()` at runtime.
3. **Per-Model Reset**: Before each model executes, `CoverageTracker.reset()` clears the agent's counters.
4. **CSV Export**: When the model finishes, `CoverageTracker.calculateAndAppend()` records:
   - `INSTRUCTION_MISSED`, `INSTRUCTION_COVERED`
   - `BRANCH_MISSED`, `BRANCH_COVERED`
   - `LINE_MISSED`, `LINE_COVERED`
   - `SPECIFIC_LINES_COVERED`, `SPECIFIC_LINES_MISSED` (space-separated line numbers)
5. **Output Files**: Generated in `src/test/resources/linetesting/output/`:
   - `jsim_coverage.csv`
   - `jmva_coverage.csv`
   - `jsimwiz_coverage.csv`
   - `jaba_coverage.csv`

> [!IMPORTANT]
> The JaCoCo runtime agent must be attached to the JVM process during execution for coverage CSVs to be populated. If tests are run without the agent, tests will still verify model correctness, but coverage CSVs will contain only headers.

---

## 5. Prerequisites Before Running Tests

Before running the integration tests, ensure that all modules are compiled so that their `target/classes` directories exist. `IntegrationTestConstants` inspects these directories to analyze bytecode:

```bash
mvn compile -DskipTests
```

---

## 6. Execution Methods

### Method 1: Maven Command Line (Recommended)

Running via Maven is the standard approach because it automatically attaches the JaCoCo agent (`jacoco-maven-plugin:prepare-agent`) and resolves all module dependencies.

#### Run All Integration Tests
From the root repository directory:
```bash
mvn test -pl jmt-integration-tests -am
```
- `-pl jmt-integration-tests`: Target the integration tests module.
- `-am` (`--also-make`): Automatically compile and package required dependencies (`jmt-app`, `jmt-engine`, etc.).

#### Run Only the `IntegrationTest` Class
```bash
mvn test -Dtest=IntegrationTest -pl jmt-integration-tests -am
```

#### Run a Specific Engine / Category
To save time during development, you can target individual parameterized test methods:

```bash
# Only JSIMgraph simulation tests
mvn test -Dtest=IntegrationTest#testJSIMGraph -pl jmt-integration-tests -am

# Only JMVA analytical tests
mvn test -Dtest=IntegrationTest#testJMVA -pl jmt-integration-tests -am

# Only JSIMwiz wizard tests
mvn test -Dtest=IntegrationTest#testJSIMWiz -pl jmt-integration-tests -am

# Only JABA asymptotic bound tests
mvn test -Dtest=IntegrationTest#testJABA -pl jmt-integration-tests -am
```

#### Run from the Submodule Directory
If you navigate directly into the `jmt-integration-tests` directory:
```bash
cd jmt-integration-tests
mvn test -Dtest=IntegrationTest
```

---

### Method 2: IntelliJ IDEA

You can execute integration tests in IntelliJ IDEA using three different mechanisms:

#### Option A: "Run Anything" with Maven (Recommended for Coverage)
This runs tests with the full Maven build lifecycle and attaches JaCoCo properly:
1. Press `Ctrl` twice (`Ctrl` + `Ctrl`) on Windows/Linux, or `Cmd` + `Cmd` on macOS.
2. In the "Run Anything" popup, paste:
   ```bash
   mvn test -Dtest=IntegrationTest -pl jmt-integration-tests
   ```
3. Press **Enter**.

#### Option B: Maven Tool Window
1. Open the **Maven** sidebar on the right side of IntelliJ.
2. Expand `jmt` > `jmt-integration-tests` > `Lifecycle`.
3. Double-click `test` (or right-click `test` and select **Run 'jmt-integration-tests [test]'**).

#### Option C: Native JUnit Run Configuration (GUI Runner & Debugger)
This allows running and debugging tests with IntelliJ's visual test runner:

1. In the Project tool window, navigate to:
   `jmt-integration-tests/src/test/java/linetesting/IntegrationTest.java`
2. Right-click `IntegrationTest` (or a specific test method like `testJMVA`) and select **Run 'IntegrationTest'** or **Debug 'IntegrationTest'**.
3. **To enable JaCoCo coverage collection when using the native JUnit runner:**
   - Open **Run** > **Edit Configurations...**
   - Select the `IntegrationTest` JUnit configuration.
   - In **VM options** (or *Modify options* > *Add VM options*), add:
     ```text
     -javaagent:<USER_HOME>/.m2/repository/org/jacoco/org.jacoco.agent/0.8.13/org.jacoco.agent-0.8.13-runtime.jar=destfile=target/jacoco.exec
     ```
   - Ensure the **Working directory** is set to `$MODULE_WORKING_DIR$` (or the project root).
   - Click **Apply** and re-run.

---

### Method 3: Direct Java Command Line (JUnit Console Launcher)

For standalone script execution or custom CI runners without Maven installed, tests can be launched via the JUnit Platform Console Launcher JAR:

```bash
# 1. Download/locate junit-platform-console-standalone (e.g. version 1.10.x)
# 2. Execute with classpath pointing to all module target/classes and dependencies
java -javaagent:path/to/org.jacoco.agent-0.8.13-runtime.jar \
     -cp "jmt-integration-tests/target/test-classes;jmt-app/target/classes;jmt-engine/target/classes;jmt-jmva-core/target/classes;lib/*;target/dependency/*" \
     org.junit.platform.console.ConsoleLauncher \
     --select-class linetesting.IntegrationTest
```

---

## 7. Downstream Analysis & Optimization Tools

The outputs generated by the integration tests feed into analysis scripts located in `jmt-integration-tests/src/test/resources/linetesting/scripts/` (documented in `docs/`):

1. **Coverage Optimization Tool (`docs/coverage_greedy_set_cover.md`)**:
   - Location: `jmt-integration-tests/src/test/resources/linetesting/scripts/coverage-greedy-set-cover/`
   - Uses the generated `*_coverage.csv` files to solve the Minimum Set Cover problem.
   - Identifies redundant test models and finds the minimal "Champion" test suite that retains 100% code coverage.
   - Generates interactive HTML coverage dashboards (`*_dashboard.html`).

2. **Regression Testing Diff Checker (`docs/jsim_diff_check.md`)**:
   - Location: `jmt-integration-tests/src/test/resources/linetesting/scripts/jsim-diff-check/`
   - Compares performance metrics between previous stable versions and current version runs.
   - Produces detailed comparison reports (`comparison_detailed.csv`, `comparison_summary.csv`).

---

## 8. Troubleshooting & FAQs

### Error: `No module target/classes directory found. Run 'mvn compile' first.`
- **Cause**: JaCoCo requires compiled `.class` files to analyze instructions and line numbers.
- **Fix**: Run `mvn compile -DskipTests` from the root directory before running integration tests.

### Coverage CSV files are empty or contain only the header line
- **Cause**: Tests were run without the JaCoCo runtime agent attached (e.g. running via IntelliJ's native JUnit button without Maven or VM options).
- **Fix**: Execute tests via Maven (`mvn test -Dtest=IntegrationTest -pl jmt-integration-tests`) or configure the `-javaagent` VM option in your IDE run configuration.

### A test model fails with `Difference: X.XX% (exceeds threshold 5.0%)`
- **Cause**: The numerical result produced by the solver deviated by more than 5% from the oracle.
- **Investigation**:
  1. Inspect the test output in `src/test/resources/linetesting/output/<engine>/`.
  2. Compare the measure values with the oracle in `src/test/resources/linetesting/input/<engine>/`.
  3. If the solver algorithm was intentionally changed or improved, update the oracle baseline.

### How do I skip long-running integration tests during normal development builds?
- By default, standard builds do not execute the integration test suite unless explicitly requested. To run only standard unit tests across the repository, use:
  ```bash
  mvn test -pl !jmt-integration-tests
  ```

---

## 9. Adding New Test Models & Oracles

When developing new modeling features (e.g., custom scheduling strategies, blocking mechanisms, fork-join patterns, or distribution types) or fixing simulation/analytical bugs, you should add representative test models to the integration test suite.

### Step 1: Create or Save the Model File
Build your model using the appropriate JMT tool (e.g. JSIMgraph or JMVA):
- **Simulation Models**: Save as `.jsimg` (graphical) or `.jsimw` (wizard).
- **Analytical Models**: Save as `.jmva`.
- **Asymptotic Bound Models**: Save as `.jaba`.

Ensure the model has solved measures:
- If the model XML contains solved results (`<results>` with `<measure finalValue="...">` or `<solutions>` with `<measure meanValue="...">`), the harness uses them as an **oracle baseline**. Future test executions must produce results within the 5.0% SMAPE stability tolerance.
- If the model contains no pre-existing results, it will function as a **functional execution test** (verifying that the solver runs from start to finish without crashing).

### Step 2: Choose Test Structure & Place in Input Directory
Place the file under the corresponding engine folder inside `jmt-integration-tests/src/test/resources/linetesting/input/`:

- **Single Model Test (Standard)**:
  ```text
  src/test/resources/linetesting/input/<engine>/<model-name>.<ext>
  ```
  *(e.g., `src/test/resources/linetesting/input/jsim/my_new_fork_join_model.jsimg`)*

- **Paired Line Test (Model + Explicit Result File)**:
  Alternatively, you can create a dedicated subfolder with separate model and expected output files:
  ```text
  src/test/resources/linetesting/input/jsim/<my_test_folder>/
  ├── model.jsim                   # Input model definition
  └── model.jsim-result.jsim       # Expected baseline results oracle
  ```

### Step 3: Run the Integration Test
Execute only the relevant test method to verify your new model passes:

```bash
# Compile dependencies
mvn compile -DskipTests

# Run the test for your specific engine
mvn test -Dtest=IntegrationTest#testJSIMGraph -pl jmt-integration-tests -am
```

Check the test logs. If the model fails the numerical tolerance check, you will see:
```text
Unstable result at index X: Expected ~Y, Got Z (Difference: W.WW%)
```
Adjust simulation parameters (such as sample size or max simulation duration) to achieve statistical stability if necessary.

### Step 4: Verify Coverage Contribution
Run the Greedy Set Cover script to see whether your new model covers unique lines of code in the engine:
```bash
python jmt-integration-tests/src/test/resources/linetesting/scripts/coverage-greedy-set-cover/main.py
```
Open `output/*_dashboard.html` in your browser. If your new model exercises previously uncovered classes or instructions, the algorithm will automatically select it in the **KEEP** set!

