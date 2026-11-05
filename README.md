<!-- Improved compatibility of back to top link: See: https://github.com/Java-Modelling-Tools/jmt/pull/73 -->
<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GPLv2 License][license-shield]][license-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/Java-Modelling-Tools/jmt">
    <img src="docs/readme/logo.png" alt="Logo" width="80" height="80">
  </a>

<h3 align="center">Java Modelling Tools (JMT)</h3>

  <p align="center">
    A free open source suite for performance evaluation, capacity planning and modelling of computer and communication systems.
    <br />
    <a href="/manuals/manual.pdf"><strong>Explore the docs »</strong></a>
    <br />
    <a href="https://java-modelling-tools.github.io/jmt/"><strong>Javadoc API »</strong></a>
    <br />
    <br />
    <a href="https://jmt.sourceforge.net/">View Site</a>
    &middot;
    <a href="https://github.com/Java-Modelling-Tools/jmt/issues/new?labels=bug&template=bug_report.md">Report Bug</a>
    &middot;
    <a href="https://github.com/Java-Modelling-Tools/jmt/issues/new?labels=enhancement&template=feature_request.md">Request Feature</a>
    &middot;
    <a href="https://github.com/Java-Modelling-Tools/jmt/issues/new?template=thesis_proposal.md">Propose a Thesis</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#architecture--modularization">Architecture & Modularization</a></li>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li>
      <a href="#usage">Usage</a>
      <ul>
        <li><a href="#run-the-full-application">Run the full application</a></li>
        <li><a href="#run-individual-tools">Run individual tools</a></li>
        <li><a href="#run-from-intellij-idea">Run from IntelliJ IDEA</a></li>
        <li><a href="#command-line-simulation-no-gui">Command-line simulation (no GUI)</a></li>
        <li><a href="#troubleshooting">Troubleshooting</a></li>
      </ul>
    </li>
    <li><a href="#reporting-issues">Reporting Issues</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

[![Product Name Screen Shot][product-screenshot]](https://example.com)

The Java Modelling Tools (JMT) is a free open source suite for performance evaluation, capacity planning and modelling of computer and communication systems. The suite implements numerous state-of-the-art algorithms for the exact, asymptotic and simulative analysis of queueing network models, either with or without product-form solution. Models can be described either through wizard dialogs or with a graphical user-friendly interface. The suite includes also a workload analysis tool based on clustering techniques. The suite incorporates an XML data layer that enables full reusability of the computational engines.

The JMT suite is composed of six tools that support different analyses frequently used in capacity planning studies. The main features of each tool follows:

* **JSIMwiz:** A discrete-event simulator for the analysis of queueing network models. An intuitive sequence of wizard windows helps specifying network properties. The simulation engine supports several probability distributions for characterizing service and inter-arrival times. Load-dependent strategies using arbitrary functions of the current queue-length can be specified. JSIM supports state-independent routing strategies, e.g., Markovian or round robin, as well as state-dependent strategies, e.g., routing to the server with minimum utilization, or with the shortest response time, or with minimum queue-length. The simulation engine supports several extended features not allowed in product-form models, namely, finite capacity regions (i.e., blocking), fork-join servers, and priority classes. The analysis of simulation results employs on-line transient detection techniques based on spectral analysis. What-if analyses, where a sequence of simulations is run for different values of parameters, are also possible.
* **JSIMgraph:** A graphical user-friendly interface for the simulator engine used by JSIMwiz. It integrates the same functionalities of JSIMwiz with an intuitive graphical workspace. This allows an easy description of network structure, as well as a simplified definition of the execution features like blocking regions. Network topologies can be exported in vectorial or raster image formats.
* **JMVA:** Meant for the exact analysis of single or multiclass product-form queueing network models, either processing open, closed or mixed workloads. The classic MVA solution algorithm is used. Network structure is specified by textual wizards, with conversion functions from probabilities to average visit ratios (and viceversa). What-if analyses are allowed.
* **JMCH:** It applies a simulation technique to solve a single station model, with finite (M/M/1/k) or infinite queue (M/M/1), and shows the underlying Markov Chain. It is also possible to dynamically change the arrival rate and service time of the system.
* **JABA:** A tool for the identification of bottlenecks in closed product-form networks using efficient convex hull algorithms. The tool supports models with up to three job classes. It is possible to identify potential bottlenecks corresponding to the different mixes of customer classes. Models with thousands of queues can be analyzed efficiently. The saturation sectors, i.e., the mixes of customer classes that saturate more than one resource simultaneously, are identified.
* **JWAT:** Supports the workload characterization phase, with emphasis on Web log data. Some standard formats for input file are provided (e.g., Apache HTTP log files), and customized formats may also be specified. The imported data can initially be analyzed using descriptive statistical techniques (e.g, means, correlations, pdf histograms, boxplots, scatterplots), either for univariate or multivariate data. Algorithms for data scaling, sample extraction, outlier filtering, k-means and fuzzy k-means clustering for identifying similarities in the input data are provided. These techniques allow to determine cluster centroids, and then estimate mean workload and service demands to be used for model parametrization. The tool includes also an interface to the similarity clustering tool CLUTO.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Architecture & Modularization

JMT has recently undergone a major refactoring, moving from a single monolithic JAR into a layered **16-module Maven project** with explicit dependency contracts. The packages were renamed to drop the old `jmt.commandline` and `jmt.gui` prefixes.

The modules are organized into 6 strict dependency layers. A module in layer *N* may only import from layers below it:
* **Layer 0 (`jmt-engine`):** Pure simulation engine (queue-network runtime, scheduling strategies, random distributions, XML schema constants). No GUI dependencies.
* **Layer 1 (`jmt-gui`):** Shared GUI framework and model definition interfaces (`CommonModel`, wizard framework, strategy interfaces). Depends on `jmt-engine`.
* **Layer 2 (`jmt-file-handler`):** Centralised model serialisation layer (`XMLWriter`, `XMLReader`, format constants).
* **Layer 3 (Core modules):** Pure analytical algorithms and computations (`jmt-jmva-core`, `jmt-jaba-core`, `jmt-jwat-core`, `jmt-jmch-core`). Depend only on `jmt-engine`.
* **Layer 4 (Tool GUIs):** Swing wizards, panels, and tool-specific I/O (`jmt-jmva-gui`, `jmt-jaba-gui`, `jmt-jwat-gui`, `jmt-jmch-gui`, `jmt-jsimw-gui`).
* **Layer 5 (`jmt-jsimg-gui`):** JModel graph editor (visual drag-and-drop network designer), `Mediator`, `DispatcherThread`.
* **Layer 6 (`jmt-app`):** Application composition root and top-level main windows. This structural module acts as the Maven aggregator so compile-scope dependencies do not create self-referencing cycles.

*Note: The `jmt-integration-tests` module handles end-to-end headless simulation tests outside of the main stack.*

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

* [![Java][Java.com]][Java-url]
* [![Maven][Maven.apache.org]][Maven-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

To get a local copy up and running follow these simple steps.

### Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | **Exactly 1.8 (Java 8)** | **IMPORTANT:** The project *must* be compiled with Java 1.8 due to `jmt-jsimg-gui/src/main/java/jsimgraph/JGraphMod/JmtJGraph.java`. This class heavily depends on legacy `org.freehep` and `org.jgraph` libraries (version 5.8) from 2006, which conflict with Java 9+ internal API encapsulation and module systems. Ensure your `JAVA_HOME` is set to JDK 8. |
| Maven | 3.6.3+ | Use the included wrapper (`mvnw`) or your own install. |
| RAM | ≥ 512 MB heap | Increase with `MAVEN_OPTS=-Xmx1g` for large models. |

### Installation

1. Clone the repo
```sh
  git clone https://github.com/Java-Modelling-Tools/jmt
```

2. To compile Java Modelling Tools and download dependencies (~200 MB on first run), use:
```sh
  mvn compile --no-transfer-progress
```

3. To create the fat JAR file (skipping tests), run on the command line from the JMT folder root:
```sh
  mvn package -DskipTests --no-transfer-progress
```

4. To create the JMT installer, run on the command line from the JMT folder root:
```sh
  mvn deploy
```

5. If the compilation is successful, the results will appear under the `target/` folder (`jmt-app/target/JMT-singlejar-<version>.jar`).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Usage

### Run the full application

**From the fat JAR (after `mvn package`)**

```bash
  java -jar jmt-app/target/JMT-singlejar-latest.jar
```

Opens the JMT start screen with all tools available.

**From Maven without building a JAR**

```bash
  mvn compile -DskipTests --no-transfer-progress
  mvn exec:java -pl jmt-app \
    -Dexec.mainClass="common.startScreen.GraphStartScreen" \
    -Dexec.classpathScope=runtime
```

**On Windows (PowerShell)**

```powershell
  $env:JAVA_HOME = "C:\Program Files\Java\jdk1.8.0_xxx"   # adjust to your JDK 8
  mvn package -DskipTests --no-transfer-progress
  java -jar jmt-app\target\JMT-singlejar-latest.jar
```

### Run individual tools

Each tool has its own main class. Use these with the fat JAR:

| Tool | Main class | What it opens |
| --- | --- | --- |
| **JMT Start Screen** | `common.startScreen.GraphStartScreen` | Main launcher (all tools) |
| **JSIMgraph** (visual editor) | `jsimgraph.mainGui.JSIMGraphMain` | Drag-and-drop queue network editor |
| **JSIMwiz** (textual editor) | `jsimwiz.JSIMWizMain` | Form-based queue network editor |
| **JMVA** (analytical) | `jmt.jmva.JMVAWizard` | Mean Value Analysis solver |
| **JABA** (bottleneck) | `jaba.JabaWizard` | Bottleneck analysis tool |
| **JWAT** (workload) | `jwat.MainJwatWizard` | Workload analysis tool |
| **JMCH** (Markov chains) | `jmt.jmch.wizard.MainWizard` | Markov chain animator |
| **CLI** | `jmt.commandline.Jmt` | Command-line simulation runner |

*Example — run JSIMgraph directly:*

```bash
  java -cp jmt-app/target/JMT-singlejar-latest.jar jsimgraph.mainGui.JSIMGraphMain
```

### Run from IntelliJ IDEA

**First-time setup**

1. **Open project**: File → Open → select the root `pom.xml` → Open as Project.
2. Wait for Maven import to finish (bottom status bar).
3. Set SDK: File → Project Structure → SDKs → add your JDK 1.8 installation.

**Run the full application**

1. Open `jmt-app/src/main/java/common/startScreen/GraphStartScreen.java`.
2. Click the green ▶ gutter icon next to the `main` method.
3. Or: Run → Edit Configurations → `+` → Application:
* **Module**: `jmt-app`
* **Main class**: `common.startScreen.GraphStartScreen`
* **VM options**: `-Xmx512m` (optional)

**Run integration tests**

1. Right-click `jmt-integration-tests/src/test/java/jmt/integration/IntegrationTest.java`.
2. Run → Run 'IntegrationTests'.
   *(Note: Integration tests open real Swing windows headlessly. Do not interact with them while tests are running — dialogs are auto-dismissed by the test framework.)*

### Command-line simulation (no GUI)

Run a simulation model file directly from the command line:

```bash
  java -cp jmt-app/target/JMT-singlejar-latest.jar \
    jmt.commandline.Jmt \
    --model path/to/model.jsim \
    --output path/to/results.jsim
```

| Flag | Description |
| --- | --- |
| `--model <file>` | Input `.jsim` model file |
| `--output <file>` | Output results file |
| `--seed <n>` | Fixed random seed for reproducibility |
| `--maxtime <seconds>` | Maximum simulation wall time |

### Troubleshooting

* **`java.lang.OutOfMemoryError: Java heap space`**: Increase the heap using `java -Xmx2g -jar jmt-app/target/JMT-singlejar-latest.jar`.
* **`UnsatisfiedLinkError` on startup**: The application is pure Java — no native libraries. This error means the wrong JDK is on `PATH`. Verify with `java -version` (must be 1.8).
* **`BUILD FAILURE` on first `mvn compile**`: Usually a missing local repository artifact. Run `mvn compile --no-transfer-progress -U`.
* **Tests hang / never complete**: If a simulation model is very large, increase the test timeout: `mvn test -pl jmt-integration-tests -am -DTEST_TIMEOUT_MINUTES=20`.
* **IntelliJ shows red imports after Maven import**: Right-click the root `pom.xml` → Maven → Reload project. If imports are still red, File → Invalidate Caches → Restart.

*For more examples, please refer to the [Documentation*](/manuals/manual.pdf)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- REPORTING ISSUES -->
## Reporting Issues

If you encounter a bug, have a new idea, or want to propose a project, we want to hear about it! To help us process requests efficiently, we use GitHub issue templates. 

When opening a new issue, please select the appropriate template and fill it out as completely as possible:

* **[Bug Report](https://github.com/Java-Modelling-Tools/jmt/issues/new?labels=bug&template=bug_report.md):** For reporting unexpected behavior, crashes, or incorrect calculations (template located at `.github/ISSUE_TEMPLATE/bug_report.md`).
* **[Feature Request](https://github.com/Java-Modelling-Tools/jmt/issues/new?labels=enhancement&template=feature_request.md):** For proposing new modelling capabilities, UI enhancements, or other features (template located at `.github/ISSUE_TEMPLATE/feature_request.md`).
* **[Thesis Proposal](https://github.com/Java-Modelling-Tools/jmt/issues/new?template=thesis_proposal.md):** For students proposing BSc/MSc thesis projects related to JMT (template located at `.github/ISSUE_TEMPLATE/thesis_proposal.md`).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

**For complete guidelines, architectural details, and workflow instructions, please see [CONTRIBUTING.md](CONTRIBUTING.md).**

When developing or modifying the suite, strict module boundaries must be respected:

* `jmt-engine` is the foundation and cannot contain JMT internal imports.

* `jmt-gui` must not know about specific tools.

* `jmt-file-handler` must remain independent of graph layout and concrete tool GUIs.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Top contributors:

<a href="https://github.com/Java-Modelling-Tools/jmt/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Java-Modelling-Tools/jmt" />
</a>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## License

Copyright (C) 2006-Present, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano, Italy.  
Copyright (C) 2010-Present, Department of Computing - Imperial College London, UK.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 

You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

For more information, please see the full [License.txt](License.txt) file.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Contact

Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano, Italy

Software Engineering Research Group (SWEN) - University of L’Aquila, Italy

Department of Computing - Imperial College London, UK (until 2025)

Project Link: [https://github.com/Java-Modelling-Tools/jmt](https://github.com/Java-Modelling-Tools/jmt)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Acknowledgments

Java Modelling Tools includes the following third-party software that can be freely distributed according to the licenses contained in the license folder:

* FreeHEP VectorGraphics package, released under GNU LGPL License.
* Apache Jakarta-ORO, released under Apache Software License.
* JFEP, released under the Apache License, Version 2.0
* JGoodies Looks, released under BSD open source License.
* JGraph, released under GNU LGPL License.
* Apache Log4j, released under Apache Software License.
* Apache Commons, released under Apache Software License.
* Ptolemy II Ptplot, released under BSD open source License.
* Apache Xerces Java Parser, released under Apache Software License.
* Pdf-renderer, released under GNU LGPL 2.1 License.
* Spearlib, released under BSD open source License.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/Java-Modelling-Tools/jmt.svg?style=for-the-badge
[contributors-url]: https://github.com/Java-Modelling-Tools/jmt/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/Java-Modelling-Tools/jmt.svg?style=for-the-badge
[forks-url]: https://github.com/Java-Modelling-Tools/jmt/network/members
[stars-shield]: https://img.shields.io/github/stars/Java-Modelling-Tools/jmt.svg?style=for-the-badge
[stars-url]: https://github.com/Java-Modelling-Tools/jmt/stargazers
[issues-shield]: https://img.shields.io/github/issues/Java-Modelling-Tools/jmt.svg?style=for-the-badge
[issues-url]: https://github.com/Java-Modelling-Tools/jmt/issues
[license-shield]: https://img.shields.io/github/license/Java-Modelling-Tools/jmt.svg?style=for-the-badge
[license-url]: https://github.com/Java-Modelling-Tools/jmt/blob/master/License.txt
[product-screenshot]: docs/readme/product-screenshot.png
[Java.com]: https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://www.java.com/
[Maven.apache.org]: https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white
[Maven-url]: https://maven.apache.org/
