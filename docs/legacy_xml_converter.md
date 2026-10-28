# JMT Legacy XML Compatibility Guide

This document describes the `jmt-legacy-xml` tool and how JMT handles backward and forward compatibility for XML model files across versions.

---

## Table of Contents
1. [The Compatibility Problem](#1-the-compatibility-problem)
2. [How the Converter Works](#2-how-the-converter-works)
3. [Command-Line Usage](#3-command-line-usage)
4. [Automatic Legacy Saves in the GUI](#4-automatic-legacy-saves-in-the-gui)
5. [Developer Maintenance](#5-developer-maintenance)

---

## 1. The Compatibility Problem

During the architectural refactoring into a 16-module Maven project, Java packages were modernized and streamlined:
- Classes previously located under `jmt.engine.*` were moved to top-level packages (e.g., `NetStrategies.*`, `NodeSections.*`, `QueueNet.*`, `random.*`, `math.*`, `simEngine.*`, `dataAnalysis.*`).
- Classes previously located under `jmt.gui.common.*` were moved to `common.*`.

Because JMT serializes model configurations directly to XML by writing fully qualified Java class names into `classPath` attributes (for routing strategies, queue disciplines, random distributions, and node sections), **models saved by modern JMT cannot be directly deserialized by legacy JMT releases (version $\le$ 1.4.0)**. Furthermore, older XML schemas reject the modern `jmtVersion` root attribute.

The `jmt-legacy-xml` module provides a lightweight, JDK-only bidirectional translation layer to solve this problem.

---

## 2. How the Converter Works

The converter (`legacyxml.LegacyXmlExporter`) parses the XML DOM and rewrites every `classPath` attribute:

| Package Group | Modern Format (`current`) | Legacy Format (`1.4.0`) |
| :--- | :--- | :--- |
| **Engine Strategies & Queues** | `NetStrategies.RoutingStrategy` | `jmt.engine.NetStrategies.RoutingStrategy` |
| **Node Sections** | `NodeSections.Queue` | `jmt.engine.NodeSections.Queue` |
| **Distributions** | `random.Exponential` | `jmt.engine.random.Exponential` |
| **Shared GUI Definitions** | `common.definitions.CommonModel` | `jmt.gui.common.definitions.CommonModel` |

### Attribute Handling
- When converting to `1.4.0`, the `jmtVersion` attribute is removed from the root element (to satisfy older XSD schemas) and preserved inside an XML header comment:
  ```xml
  <!-- Saved by JMT latest in the format of releases up to 1.4.0 -->
  ```
- Unrecognized or third-party class names (e.g. `java.lang.*`) are passed through untouched.

---

## 3. Command-Line Usage

### Syntax
```bash
java -cp <classpath> legacyxml.LegacyXmlExporter <input-file> <output-file> [version]
```
- `<input-file>`: Source XML model (`.jsimg`, `.jsimw`, `.jsim`, or `.jmva`).
- `<output-file>`: Destination file path.
- `[version]`: Target format version: `1.4.0` (default) or `current`.

### Converting to Legacy Format (for JMT $\le$ 1.4.0)
From the repository root (after compiling with `mvn compile -DskipTests`):
```bash
java -cp jmt-legacy-xml/target/classes legacyxml.LegacyXmlExporter \
     ./models/my_model.jsimg \
     ./models/my_model_legacy.jsimg \
     1.4.0
```

### Upgrading a Legacy Model to Modern Modular Format
```bash
java -cp jmt-legacy-xml/target/classes legacyxml.LegacyXmlExporter \
     ./models/old_model.jsimg \
     ./models/old_model_upgraded.jsimg \
     current
```

---

## 4. Automatic Legacy Copies in the GUI

To ensure seamless collaboration between users running modern JMT and colleagues using legacy institutional installations, the visual graph editor automatically saves a secondary legacy copy whenever you save a model.

- When saving `network.jsimg`, `LegacyXmlExporter.saveLegacyCopy` creates `network.legacy.jsimg` in the same directory.
- This operation runs on an in-memory clone of the document and is safe: if writing the legacy copy fails, the primary save operation is never interrupted.

---

## 5. Developer Maintenance

If you introduce a new Java package whose classes are serialized into model XML files via `classPath` attributes, you must register its root prefix in `LegacyXmlExporter.java`:

- Engine classes: Add the package prefix to `ENGINE_ROOTS` in `LegacyXmlExporter.java`:
  ```java
  private static final List<String> ENGINE_ROOTS = Arrays.asList(
      "NetStrategies.", "NodeSections.", "QueueNet.", "random.", "math.",
      "simEngine.", "dataAnalysis.", "yourNewPackage."
  );
  ```
- GUI classes: Add the package prefix to `GUI_ROOTS`.
