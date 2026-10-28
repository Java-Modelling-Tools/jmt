# JMT Headless Command-Line Interface (CLI) Reference

The `jmt.commandline.Jmt` class provides a headless command-line interface for running discrete-event simulations and analytical Mean Value Analysis (MVA) models without opening any graphical user interface (GUI). 

This interface is designed for:
- Automated batch simulations and parameter sweeps
- High-Performance Computing (HPC) clusters and remote compute nodes
- Continuous Integration / Continuous Deployment (CI/CD) pipelines
- Scripts written in Bash, Python, or PowerShell

---

## Table of Contents
1. [Syntax and Invocation](#1-syntax-and-invocation)
2. [Subcommands](#2-subcommands)
   - [`sim` — Discrete-Event Simulation](#sim--discrete-event-simulation)
   - [`mva` — Analytical MVA Solver](#mva--analytical-mva-solver)
   - [GUI Launcher Modes (`jsimg`, `jsimw`, `jmva`)](#gui-launcher-modes-jsimg-jsimw-jmva)
3. [Options Reference](#3-options-reference)
4. [Output Files and Format](#4-output-files-and-format)
5. [Practical Examples](#5-practical-examples)
6. [Exit Codes & Error Handling](#6-exit-codes--error-handling)

---

## 1. Syntax and Invocation

### General Command Syntax
```bash
java -cp <classpath> jmt.commandline.Jmt <subcommand> <modelfile> [options]
```

### Running from the Fat JAR (Recommended)
After building JMT via `mvn package -DskipTests`, the fat JAR contains all dependencies:
```bash
java -cp jmt-app/target/JMT-singlejar-latest.jar jmt.commandline.Jmt <subcommand> <modelfile> [options]
```

### Running via Maven
You can execute directly through Maven without building the standalone fat JAR:
```bash
mvn exec:java -pl jmt-app \
  -Dexec.mainClass="jmt.commandline.Jmt" \
  -Dexec.args="<subcommand> <modelfile> [options]"
```

---

## 2. Subcommands

### `sim` — Discrete-Event Simulation

Executes the discrete-event queueing network simulation engine on the provided model file.

- **Supported Input Formats**: `.jsimg` (JSIMgraph canvas models) or `.jsim` / `.jsimw` (JSIMwiz models).
- **Execution Engine**: `simDispatcher.DispatcherJSIMschema`
- **Output File**: Automatically creates `<modelfile>-result.jsim` in the same directory as the input model.
- **Syntax**:
  ```bash
  java -cp jmt-app/target/JMT-singlejar-latest.jar jmt.commandline.Jmt sim <model-path> [options]
  ```

### `mva` — Analytical MVA Solver

Executes the analytical Mean Value Analysis solver on the provided product-form queueing network.

- **Supported Input Formats**: `.jmva` (JMVA XML model files).
- **Execution Engine**: `solvers.dispatchers.SolverDispatcher`
- **Output File**: Automatically creates `<modelfile>-result.jmva` containing the solved metrics under `<solutions>`.
- **Syntax**:
  ```bash
  java -cp jmt-app/target/JMT-singlejar-latest.jar jmt.commandline.Jmt mva <model-path>
  ```

### GUI Launcher Modes (`jsimg`, `jsimw`, `jmva`)

For convenience on desktop environments with display servers, `Jmt` can also launch GUI editors with a model preloaded:
- `jsimg <model.jsimg>`: Opens the visual drag-and-drop editor (`JSIMGraphMain`).
- `jsimw <model.jsimw>`: Opens the wizard-based simulation editor (`JSIMWizMain`).
- `jmva <model.jmva>`: Opens the analytical MVA wizard editor (`JMVAWizard`).

---

## 3. Options Reference

The `sim` subcommand supports the following runtime flags:

| Flag | Argument | Description | Default |
| :--- | :--- | :--- | :--- |
| `-seed` | `<number>` (long) | Explicit pseudorandom generator seed. Use this to ensure deterministic, reproducible simulation runs. | Random seed |
| `-maxtime` | `<seconds>` (long) | Maximum allowed simulation duration in wall-clock seconds. If the simulation exceeds this time, it halts and reports current intermediate statistics. | Unlimited |

*Note: Options are case-insensitive (`-seed` or `-SEED`).*

---

## 4. Output Files and Format

### Simulation Results (`*-result.jsim`)
When `sim` completes, it writes an XML document formatted according to `SIMmodeloutput.xsd`:
```xml
<results>
    <measure station="Queue 1" class="Class 1" type="0" measureType="Number of Customers"
             meanValue="3.412" finalValue="3.412" lowerLimit="3.350" upperLimit="3.474">
    </measure>
    <measure station="Queue 1" class="Class 1" type="1" measureType="Response Time"
             meanValue="0.085" finalValue="0.085" lowerLimit="0.083" upperLimit="0.087">
    </measure>
</results>
```

### Analytical Results (`*-result.jmva`)
When `mva` completes, it writes an XML document formatted according to `JMTmodel.xsd`:
```xml
<solutions>
    <algorithm name="Exact MVA"/>
    <measure station="Station 1" class="Class 1" type="0" meanValue="1.2500000000"/>
    <measure station="Station 1" class="Class 1" type="1" meanValue="0.0250000000"/>
</solutions>
```

---

## 5. Practical Examples

### Example 1: Run a Headless Simulation with a Fixed Seed
```bash
java -cp jmt-app/target/JMT-singlejar-latest.jar jmt.commandline.Jmt sim ./models/ecommerce_system.jsimg -seed 42
```
Generates `./models/ecommerce_system.jsimg-result.jsim`.

### Example 2: Set Maximum Duration to Prevent Long Runs
```bash
java -cp jmt-app/target/JMT-singlejar-latest.jar jmt.commandline.Jmt sim ./models/large_network.jsimg -maxtime 120
```
Restricts simulation to 120 seconds.

### Example 3: Solve an Analytical MVA Model Headless
```bash
java -cp jmt-app/target/JMT-singlejar-latest.jar jmt.commandline.Jmt mva ./models/closed_mva_network.jmva
```
Generates `./models/closed_mva_network.jmva-result.jmva`.

### Example 4: Batch Script for Parameter Sweeps (Bash)
```bash
#!/usr/bin/env bash
JAR="jmt-app/target/JMT-singlejar-latest.jar"

for SEED in {1001..1005}; do
    echo "Running simulation with seed $SEED..."
    java -cp "$JAR" jmt.commandline.Jmt sim "./models/model.jsimg" -seed "$SEED"
    mv "./models/model.jsimg-result.jsim" "./results/run_seed_${SEED}.jsim"
done
```

---

## 6. Exit Codes & Error Handling

| Exit Code | Meaning | Description |
| :---: | :--- | :--- |
| `0` | Success | Simulation or analytical solution completed normally, or help message printed |
| `1` | Input Error | Missing or invalid model file path, or non-numeric argument passed to `-seed` or `-maxtime` |
| Exception | Solver Error | XML parsing error, model topology error, or simulation engine exception |
