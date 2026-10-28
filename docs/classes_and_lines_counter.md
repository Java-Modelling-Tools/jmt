# Code Metrics Counter: Classes & Lines of Code

A standalone Python utility for analyzing Java codebases and multi-module Maven projects (such as JMT). It counts:

1. **Classes and Types:**
   - Top-level classes, interfaces, and enums
   - Inner/nested classes, interfaces, and enums
   - Anonymous inner classes (e.g. Swing listeners, event adapters, runners)
   - Total named classes vs total classes including anonymous definitions
2. **Lines of Code (SLOC):**
   - Source code lines (excluding comments and blanks)
   - Comment lines (Javadoc, multi-line block comments, single-line comments)
   - Blank lines
   - Total raw lines
3. **Scope & Module Breakdown:**
   - Production (`src/main`) vs Test (`src/test`)
   - Per-module statistics across all Maven submodules (`jmt-gui`, `jmt-engine`, etc.)
4. **Other Project Files (Optional):**
   - XML, XSD, XSLT, Python, HTML, Markdown, and other text/configuration files

---

## Supported Output Formats

| Format | Extension | Flag | Description |
| :--- | :---: | :--- | :--- |
| **Plain Text Report** | `.txt` | `--format txt` *(or `text`, `table`)* | Formatted ASCII table report (default) |
| **Markdown** | `.md` | `--format markdown` *(or `md`)* | GitHub Flavored Markdown tables |
| **JSON** | `.json` | `--format json` | Structured JSON for CI/CD or automation |
| **All Formats** | `.txt`, `.md`, `.json` | `--format all` | Simultaneously generates all three files |

---

## Requirements

- **Python 3.7+**
- **Standard Library only** (no external packages or `pip install` required)
- Works seamlessly on Windows, Linux, and macOS

---

## Quick Start

### 1. View report in terminal (default Plain Text format):
```bash
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py
```

### 2. Generate and save report files to disk:
```bash
# Generate code_metrics.txt in ./scripts/classes_and_lines_counter/output/
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --generate

# Generate code_metrics.txt, code_metrics.md, and code_metrics.json simultaneously
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --format all

# Save directly to a custom file path
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py -o ./my_report.txt
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py -o ./my_report.md
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py -o ./my_report.json
```

---

## Command-Line Usage and Options

```
usage: count_classes_and_lines.py [-h] [--path PATH]
                                  [--format {txt,text,table,md,markdown,json,all}]
                                  [--output OUTPUT] [--output-dir OUTPUT_DIR]
                                  [--generate] [--prefix PREFIX]
                                  [--no-modules] [--include-non-java]
                                  [--no-git] [--quiet]
```

### Options

| Flag | Description | Default |
| :--- | :--- | :--- |
| `-f`, `--format` | Output format: `txt`, `md`, `json`, or `all` | `txt` |
| `-g`, `--generate` | Generate and write output file(s) to disk | `False` |
| `-o`, `--output FILE` | Explicit output file path (auto-detects format from extension) | `None` |
| `-d`, `--output-dir DIR` | Target directory for generated reports | `./scripts/classes_and_lines_counter/output` |
| `--prefix NAME` | Filename prefix when saving files | `code_metrics` |
| `-q`, `--quiet` | Suppress console output when saving files | `False` |
| `-p`, `--path PATH` | Project root directory to scan | Repository root directory |
| `--no-modules` | Hide the detailed per-module breakdown table | `False` (breakdown shown) |
| `--include-non-java` | Include counts for XML, Python, and config files | `False` |
| `--no-git` | Walk the filesystem directly instead of using `git ls-files` | `False` (uses git if available) |

---

## Detailed Examples

### Example 1: Generate `.txt` report file
```bash
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --generate --format txt
```
Writes `output/code_metrics.txt`.

### Example 2: Generate all three formats at once (`.txt`, `.md`, `.json`) silently
```bash
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --format all --quiet
```
Writes:
- `output/code_metrics.txt`
- `output/code_metrics.md`
- `output/code_metrics.json`

### Example 3: Save to a specific custom directory
```bash
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --generate --output-dir ./docs/metrics
```

### Example 4: Output Markdown to terminal (e.g. for PR or issue descriptions)
```bash
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --format markdown
```

### Example 5: Include non-Java files (XML, Python, configs)
```bash
python ./scripts/classes_and_lines_counter/count_classes_and_lines.py --include-non-java -o ./full_inventory.txt
```

---

## Counting Methodology

### Line Counting
The script uses a character-by-character lexical state machine matching the standard `cloc` counting algorithm:
- **Blank Lines:** Lines consisting exclusively of whitespace.
- **Comment Lines:** Lines containing comments (single-line `//`, block `/* ... */`, or Javadoc `/** ... */`) and no executable source code.
- **Code Lines (SLOC):** Lines containing actual source code. If a line has both code and an inline comment (e.g. `int x = 1; // comment`), it is counted as a **Code Line**.
- **Total Lines:** `SLOC + Comment Lines + Blank Lines`.

### Class Counting
To prevent false matches (e.g., inside strings, comments, or class literals like `Foo.class`), the script strips comments and string literals, then tokenizes Java declarations:
- **Top-Level Classes:** Classes declared outside any enclosing class (`brace_depth == 0`). Note that in Java, a single `.java` source file can define multiple package-private top-level classes.
- **Inner / Nested Classes:** Static or member classes declared within another class or interface.
- **Anonymous Classes:** Anonymous inner classes instantiated via `new Type(...) { ... }` (frequent in Swing event handlers, adapters, and worker threads).
- **Interfaces & Enums:** Tracked separately at both top-level and inner levels.
- **Total Named Classes:** `Top-Level Classes + Inner / Nested Classes`.
- **Total Classes:** `Total Named Classes + Anonymous Inner Classes`.
- **Total Java Types:** All class, interface, enum, record, and annotation definitions.
