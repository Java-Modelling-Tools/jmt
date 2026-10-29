# Station Utilization Checker Guide

## 1. Overview

The **JMT Station Utilization Checker** (`utilization_checker.py`) is an automated diagnostic and reporting utility for the Java Modelling Tools (JMT) integration test suite. It inspects test model definitions and simulation results across **JSIMgraph**, **JMVA**, and **JSIMwiz** to detect stations operating at or near saturation (**$U \ge 1.0$**).

By default, the checker evaluates stations against a **$1.0$ threshold with a $5\%$ tolerance** ($[0.95, 1.05]$), capturing:
- **Bottleneck Stations**: Stations operating at 100% capacity ($U = 1.0$) that constrain overall system throughput.
- **Statistical Saturation ($0.95 \le U < 1.0$)**: Stations that approach full saturation within stochastic simulation confidence intervals.
- **Multi-Server & Delay Station Overload ($U > 1.0$)**: Delay stations (infinite servers) and multi-server centers reporting aggregate load across all servers.

All generated reports are saved by default to:
`jmt-integration-tests/src/test/resources/jmt/integration/scripts/utilization-checker/outputs/`

Supported export formats include **JSON** (`.json`), **Markdown** (`.md`), and **Plain Text** (`.txt`) (as well as `.csv`).

---

## 2. Queueing Theory Context

In queueing network theory and operational analysis, utilization $U_i$ measures the fraction of time that resource $i$ is busy serving requests:

$$U_i = \frac{\lambda_i}{\mu_i} = X_i \cdot S_i = X \cdot D_i$$

Where:
- $\lambda_i$: Job arrival rate at station $i$
- $\mu_i$: Mean service rate ($\mu_i = 1 / S_i$)
- $X_i$: Station throughput
- $S_i$: Mean service time
- $D_i$: Total service demand ($D_i = V_i \cdot S_i$, where $V_i$ is the visit count)

### 2.1 Open Queueing Networks
In open networks, system stability requires that no station exceeds capacity:

$$\rho_i = \frac{\lambda_i}{c_i \mu_i} < 1$$

If $\rho_i \ge 1$ without capacity constraints, queue lengths grow unboundedly. In bounded systems (such as models utilizing **Finite Capacity Regions (FCR)**, drop policies, or soft deadlines), excess arrivals are blocked or dropped, and the server operates at **$U = 1.0$** (100% capacity).

### 2.2 Closed Queueing Networks
In closed networks with a fixed customer population $N$, the system is always stable. As the population increases ($N \to \infty$), the bottleneck station with maximum service demand $D_{max} = \max_i \{D_i\}$ saturates asymptotically:

$$\lim_{N \to \infty} U_{max}(N) = 1.0$$

The maximum achievable system throughput is bounded by:

$$X_{sat} = \frac{1}{D_{max}}$$

### 2.3 Statistical Variance in Simulation
Stochastic simulations compute utilization as an empirical time-average with statistical confidence intervals. A theoretical bottleneck station may record a measured utilization such as $0.985$ or $0.997$ depending on sample run length and warm-up discarded periods. A **$5\%$ tolerance window** ($0.95 \le U \le 1.0$) reliably captures these near-saturation bottleneck stations without false negatives.

---

## 3. How the Checker Works

### 3.1 Scanning Pipeline
1. **Dynamic Path Resolution**: Automatically locates test models (`input/`) and simulation results in the Maven build directory (`target/jmt/integration/output/`), regardless of the current working directory.
2. **Schema-Aware Parsing**:
   - **JSIMgraph / JSIMwiz**: Extracts `<measure>` elements inside `<results>` or model definitions matching type `6` (`SimConstants.UTILIZATION`), type `7` (`EFFECTIVE_UTILIZATION`), or measure names containing `Utilization`.
   - **JMVA**: Extracts analytical station solutions from `<stationresults station="...">` $\to$ `<classresults customerclass="...">` $\to$ `<measure measureType="Utilization" ...>`.
3. **Classification**:
   - **`EXACT_1.0`**: $|U - 1.0| < 10^{-5}$
   - **`STATISTICAL_SATURATION`**: $U \in [1.0 - \text{tolerance}, 1.0)$
   - **`MULTI_SERVER_OVERLOAD`**: $U > 1.0$
   - **`UNDER_UTILIZED`**: $U < 1.0 - \text{tolerance}$

---

## 4. Output Formats & Default Directory

All report files are saved into the `outputs/` directory inside the script folder:

```text
jmt-integration-tests/src/test/resources/jmt/integration/scripts/utilization-checker/
├── utilization_checker.py
└── outputs/
    ├── utilization_report.json   <-- Machine-readable structured JSON
    ├── utilization_report.md     <-- Formatted GitHub-style Markdown report
    ├── utilization_report.txt    <-- Clean plain-text ASCII summary table
    └── utilization_report.csv    <-- Spreadsheet-compatible tabular data
```

### 4.1 JSON (`.json`)
Contains full structured metadata, unique model file arrays, and raw result records:
```json
{
  "generated_at": "2026-10-29T02:04:00",
  "threshold": 1.0,
  "tolerance": 0.05,
  "tolerance_percentage": "5.0%",
  "lower_bound": 0.95,
  "upper_bound": 1.05,
  "total_records": 133,
  "unique_base_models_count": 18,
  "unique_base_models": [ "2cl_3stat_whatif_popmix.jmva", "FCR bugPerfInd1FCR.jsimg", ... ],
  "files_summary": [ ... ],
  "results": [ ... ]
}
```

### 4.2 Markdown (`.md`)
Renders:
1. **Files with at least one Station in Utilization List**: An index of all unique base models and a summary table breakdown per file.
2. **Categorized Tables**: Grouped by Exact 1.0, Statistical Saturation (5% tolerance), and Multi-Server Overload.

### 4.3 Plain Text (`.txt`) & Console Output
A clean ASCII table for terminal inspection or log files, followed by:
1. **Alphabetical Index of Unique Models**: Quick reference list of every model file with saturated stations.
2. **Breakdown by Model Artifact**: Detailed listing showing each model file, its source (Input Oracle vs Simulated Output), and each saturated station with its utilization value.

---

## 5. Usage & Command-Line Reference

### 5.1 Basic Execution
Run the checker with default settings (threshold $1.0$, tolerance $5\%$, prints summary table to console):

```bash
python jmt-integration-tests/src/test/resources/jmt/integration/scripts/utilization-checker/utilization_checker.py
```

### 5.2 Configuring Tolerance
You can specify tolerance as a decimal ratio (`0.05`), a percentage string (`5%`), or an integer percentage (`5`):

```bash
# 5% tolerance (captures 0.95 <= U <= 1.05) - Default
python utilization_checker.py --tolerance 5%

# Strict tolerance (e.g., 1% -> captures 0.99 <= U <= 1.01)
python utilization_checker.py --tolerance 0.01

# Wide tolerance (e.g., 10% -> captures 0.90 <= U <= 1.10)
python utilization_checker.py -t 10%
```

Alternatively, set the environment variable:
```bash
# In Bash:
export JMT_UTILIZATION_TOLERANCE=0.05

# In PowerShell:
$env:JMT_UTILIZATION_TOLERANCE = "0.05"
```

### 5.3 Scanning Scope (Inputs vs. Outputs)
By default, the script scans **both** input test models (`src/test/resources/.../input/`) and Maven simulation results (`target/.../output/`). You can restrict the scan using:

```bash
# Scan ONLY input oracle models (skip simulation outputs)
python utilization_checker.py --inputs-only

# Scan ONLY simulated output models (skip input oracles)
python utilization_checker.py --outputs-only
```

### 5.4 Exporting Reports

```bash
# Export all formats (json, md, txt, csv) to outputs/
python utilization_checker.py --format all

# Export specific formats
python utilization_checker.py --format json,md,txt

# Export to a custom file path
python utilization_checker.py --json custom_output.json --md custom_report.md
```

### 5.5 Command-Line Options Reference

| Option | Shorthand | Default | Description |
| :--- | :--- | :--- | :--- |
| `--threshold` | | `1.0` | Target utilization threshold value. |
| `--tolerance` | `-t` | `0.05` ($5\%$) | Tolerance window ratio or percentage (`0.05`, `5%`, or `5`). |
| `--inputs-only` | | `False` | Scan only input test models in `input/` (skip simulation outputs). |
| `--outputs-only` | | `False` | Scan only simulated output models in `output/` (skip input models). |
| `--format` | `-f` | `None` | Comma-separated output formats (`json`, `md`, `txt`, `csv`, or `all`). |
| `--json` | | `None` | Export JSON report (`[PATH]` or default `outputs/utilization_report.json`). |
| `--md` | | `None` | Export Markdown report (`[PATH]` or default `outputs/utilization_report.md`). |
| `--txt` | | `None` | Export Plain Text report (`[PATH]` or default `outputs/utilization_report.txt`). |
| `--csv` | | `None` | Export CSV report (`[PATH]` or default `outputs/utilization_report.csv`). |
| `--outputs-dir` | | `outputs/` | Target directory for generated output reports. |
| `--input-dir` | | *Auto* | Custom path to integration test input models directory. |
| `--sim-dir` | | *Auto* | Custom path to Maven simulation target output directory. |
| `--all` | | `False` | Display all measured stations regardless of utilization value. |
| `--help` | `-h` | | Show CLI help message and exit. |

---

## 6. Directory Layout

```text
jmt/
├── docs/
│   ├── utilization_checker.md          <-- THIS DOCUMENT
│   ├── integration_tests.md
│   └── README.md
│
└── jmt-integration-tests/
    ├── target/jmt/integration/output/  <-- Maven test simulation outputs
    │   ├── jsim/
    │   ├── jmva/
    │   └── ...
    │
    └── src/test/resources/jmt/integration/
        ├── input/                      <-- Model input oracles
        │   ├── jsim/
        │   ├── jmva/
        │   ├── jsimw/
        │   └── jaba/
        │
        └── scripts/
            └── utilization-checker/    <-- SCRIPT LOCATION
                ├── utilization_checker.py
                └── outputs/            <-- GENERATED REPORTS
                    ├── utilization_report.json
                    ├── utilization_report.md
                    ├── utilization_report.txt
                    └── utilization_report.csv
```
