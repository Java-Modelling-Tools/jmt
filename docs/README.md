# JMT Documentation Hub

Welcome to the central documentation directory for **Java Modelling Tools (JMT)**. This directory contains architectural specifications, testing guides, script references, and compatibility manuals for developers and contributors.

---

## Documentation Index

### 1. Testing & Quality Assurance

* **[Integration Testing Guide](integration_tests.md)**  
  Comprehensive manual on the headless integration test framework (`jmt-integration-tests`). Explains the headless execution architecture (`HeadlessSolver`), oracle verification via SMAPE (Symmetric Mean Absolute Percentage Error), JaCoCo runtime coverage tracking, and step-by-step instructions for running tests via Maven, IntelliJ IDEA, and the command line.

* **[Adding New Test Models & Oracles](integration_tests.md#9-adding-new-test-models--oracles)**  
  Step-by-step developer walkthrough for adding new simulation or analytical models to the integration test suite, establishing baseline oracles, and verifying numerical stability.

* **[Station Utilization Checker](utilization_checker.md)**  
  Automated diagnostic tool (`utilization_checker.py`) for scanning test models and simulation outputs to identify stations operating at 100% saturation ($U = 1.0$), bottlenecks, and heavy-load asymptotic limits.

* **[Regression Testing Diff Checker](jsim_diff_check.md)**  
  Automated tool for detecting numerical regressions between JMT versions. Compares performance metrics extracted from simulation runs and generates detailed stability reports (`comparison_detailed.csv` and `comparison_summary.csv`).

---

### 2. Test Suite Optimization

* **[Coverage Optimization Tool (Greedy Set Cover)](coverage_greedy_set_cover.md)**  
  Analysis pipeline that solves the Minimum Set Cover problem using JaCoCo coverage data. Identifies the "Champion" test suite needed to preserve 100% code coverage while discarding redundant test models, and generates an interactive HTML dashboard.

---

### 3. Codebase Utilities & Metrics

* **[Classes and Lines of Code Counter](classes_and_lines_counter.md)**  
  Documentation for the standalone metrics script (`scripts/classes_and_lines_counter/count_classes_and_lines.py`). Counts top-level, nested, and anonymous classes, interfaces, enums, physical SLOC, comments, and blank lines, with breakdowns by scope (`main` vs `test`), Maven module, and export formats (`.txt`, `.md`, `.json`).

---

### 4. Headless Execution & Compatibility

* **[Headless CLI Reference Guide](cli_reference.md)**  
  Full reference guide for `jmt.commandline.Jmt`. Details how to execute simulations and analytical MVA solvers without a GUI for cluster environments, automated scripts, parameter sweeps, and CI/CD pipelines.

* **[Legacy XML Compatibility Guide](legacy_xml_converter.md)**  
  Documentation for the `jmt-legacy-xml` tool (`legacyxml.LegacyXmlExporter`). Explains how JMT maintains backward and forward compatibility between modern modular releases and legacy JMT ($\le$ 1.4.0) formats by translating XML `classPath` attributes.

---

### 5. Architecture & Contribution

* **[Contributing Guide](../CONTRIBUTING.md)**  
  The main guide covering contribution workflows, coding conventions, Git branching practices, and the 16-module layered architecture contracts.
* **[Architecture Overview](contributing/architecture.png)**  
  High-level diagram illustrating the layered dependencies of JMT modules.
* **[Module Dependency Graph](contributing/dependency_graph.png)**  
  Complete compile-time dependency graph across all 16 Maven submodules.

---

## Quick Reference Table

| Document | Focus Area | Primary Audience |
| :--- | :--- | :--- |
| [`integration_tests.md`](integration_tests.md) | End-to-end headless testing & test execution | Developers, CI/CD Maintainers |
| [`cli_reference.md`](cli_reference.md) | Headless simulation & solver CLI (`Jmt.java`) | Researchers, Script Authors |
| [`coverage_greedy_set_cover.md`](coverage_greedy_set_cover.md) | Test suite minimization & coverage dashboards | Test Engineers, Core Contributors |
| [`jsim_diff_check.md`](jsim_diff_check.md) | Regression testing diff analysis | Release Managers, QA |
| [`classes_and_lines_counter.md`](classes_and_lines_counter.md) | Code metrics & line/class counting | Developers, Reviewers |
| [`utilization_checker.md`](utilization_checker.md) | Saturated station ($U = 1.0$) analysis & bottleneck detection | Performance Engineers, Testers |
| [`legacy_xml_converter.md`](legacy_xml_converter.md) | Bidirectional XML model compatibility | Core Developers, Integrators |
| [`../CONTRIBUTING.md`](../CONTRIBUTING.md) | Project architecture, setup & guidelines | All Contributors |
