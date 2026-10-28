#!/usr/bin/env python3
"""
Classes and Lines of Code Counter for JMT (Java Modelling Tools)
---------------------------------------------------------------
Analyzes Java source code and other project files to count:
  - Classes (top-level, inner/nested, anonymous classes, interfaces, enums)
  - Lines of code (physical SLOC/code, comments, blank lines, total)
  - Module breakdown (jmt-gui, jmt-engine, jmt-jmva-core, etc.)
  - Scope breakdown (src/main vs src/test)
  - Non-Java source/config files (XML, Python, XSLT, docs, etc.)

Output formats supported:
  - .txt / text / table (formatted plain text report)
  - .md / markdown (GitHub Flavored Markdown)
  - .json (machine-readable structured JSON)
  - all (generates .txt, .md, and .json simultaneously)

Can print to console (stdout) or generate files directly on disk.
Zero third-party dependencies (pure standard library).
Compatible with Python 3.7+.
"""

import argparse
import json
import os
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path


def is_binary_file(filepath):
    """Check if file is binary by inspecting first 1024 bytes for null bytes."""
    try:
        with open(filepath, "rb") as f:
            chunk = f.read(1024)
            return b"\0" in chunk
    except Exception:
        return True


def analyze_java_file(filepath):
    """
    Performs precise lexical analysis of a Java source file to count:
      - Line types: code, comment, blank, total
      - Java constructs:
          - Top-level: classes, interfaces, enums, records, annotations
          - Inner: classes, interfaces, enums, records, annotations
          - Anonymous inner classes
    """
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
    except Exception:
        return None

    lines = content.splitlines()
    total_lines = len(lines)
    blank_lines = 0
    comment_lines = 0
    code_lines = 0

    # 1. Line-by-line classification (standard cloc-style state machine)
    in_block_comment = False
    for line in lines:
        stripped = line.strip()
        if not stripped:
            blank_lines += 1
            continue

        i = 0
        has_code = False
        has_comment = False
        n = len(line)

        while i < n:
            if in_block_comment:
                has_comment = True
                if line[i : i + 2] == "*/":
                    in_block_comment = False
                    i += 2
                else:
                    i += 1
            else:
                if line[i : i + 2] == "/*":
                    has_comment = True
                    in_block_comment = True
                    i += 2
                elif line[i : i + 2] == "//":
                    has_comment = True
                    break
                elif line[i] == '"':
                    has_code = True
                    i += 1
                    while i < n and line[i] != '"':
                        if line[i] == "\\":
                            i += 2
                        else:
                            i += 1
                    i += 1
                elif line[i] == "'":
                    has_code = True
                    i += 1
                    while i < n and line[i] != "'":
                        if line[i] == "\\":
                            i += 2
                        else:
                            i += 1
                    i += 1
                elif not line[i].isspace():
                    has_code = True
                    i += 1
                else:
                    i += 1

        if has_code:
            code_lines += 1
        elif has_comment:
            comment_lines += 1
        else:
            blank_lines += 1

    # 2. Extract class/interface/enum/record/anonymous declarations
    # Strip comments and strings while keeping newlines to avoid false positives
    pattern = re.compile(
        r'(?P<STRING>"(?:\\.|[^"\\])*")|'
        r"(?P<CHAR>'(?:\\.|[^'\\])*')|"
        r'(?P<COMMENT_SINGLE>//[^\n]*)|'
        r'(?P<COMMENT_MULTI>/\*[\s\S]*?\*/)',
        re.DOTALL,
    )

    def repl(m):
        s = m.group(0)
        newlines = "\n" * s.count("\n")
        if m.group("STRING"):
            return ' "" ' + newlines
        elif m.group("CHAR"):
            return " '' " + newlines
        else:
            return " " + newlines + " "

    clean_code = pattern.sub(repl, content)

    # Tokenizer for Java declaration structures
    token_pattern = re.compile(
        r"(?P<AT_INTERFACE>@interface\b)|"
        r"(?P<KEYWORD>\b(?:class|interface|enum|record|new|extends|implements|public|protected|private|static|final|abstract)\b)|"
        r"(?P<IDENT>[A-Za-z_$][A-Za-z0-9_$]*)|"
        r"(?P<DOT>\.)|"
        r"(?P<LBRACE>\{)|"
        r"(?P<RBRACE>\})|"
        r"(?P<LPAREN>\()|"
        r"(?P<RPAREN>\))|"
        r"(?P<LBRACKET>\[)|"
        r"(?P<RBRACKET>\])|"
        r"(?P<SEMI>;)|"
        r"(?P<COMMA>,)|"
        r"(?P<OP>[<>&|!~?:=+\-*/%^]+)",
        re.UNICODE,
    )

    tokens = []
    for m in token_pattern.finditer(clean_code):
        for k, v in m.groupdict().items():
            if v is not None:
                tokens.append((k, v))
                break

    counts = {
        "top_level_classes": 0,
        "top_level_interfaces": 0,
        "top_level_enums": 0,
        "top_level_records": 0,
        "top_level_annotations": 0,
        "inner_classes": 0,
        "inner_interfaces": 0,
        "inner_enums": 0,
        "inner_records": 0,
        "inner_annotations": 0,
        "anonymous_classes": 0,
    }

    brace_depth = 0
    n = len(tokens)

    for i in range(n):
        tok_type, val = tokens[i]

        if val == "{":
            brace_depth += 1
        elif val == "}":
            if brace_depth > 0:
                brace_depth -= 1
        elif val == "class":
            prev = tokens[i - 1][1] if i > 0 else None
            if prev != ".":
                next_tok = tokens[i + 1] if i + 1 < n else None
                if next_tok and next_tok[0] == "IDENT":
                    if brace_depth == 0:
                        counts["top_level_classes"] += 1
                    else:
                        counts["inner_classes"] += 1
        elif val == "interface":
            prev = tokens[i - 1][1] if i > 0 else None
            if prev != "@":
                next_tok = tokens[i + 1] if i + 1 < n else None
                if next_tok and next_tok[0] == "IDENT":
                    if brace_depth == 0:
                        counts["top_level_interfaces"] += 1
                    else:
                        counts["inner_interfaces"] += 1
        elif val == "@interface":
            next_tok = tokens[i + 1] if i + 1 < n else None
            if next_tok and next_tok[0] == "IDENT":
                if brace_depth == 0:
                    counts["top_level_annotations"] += 1
                else:
                    counts["inner_annotations"] += 1
        elif val == "enum":
            prev = tokens[i - 1][1] if i > 0 else None
            if prev != ".":
                next_tok = tokens[i + 1] if i + 1 < n else None
                if next_tok and next_tok[0] == "IDENT":
                    if brace_depth == 0:
                        counts["top_level_enums"] += 1
                    else:
                        counts["inner_enums"] += 1
        elif val == "record":
            prev = tokens[i - 1][1] if i > 0 else None
            if prev != ".":
                next_tok = tokens[i + 1] if i + 1 < n else None
                if next_tok and next_tok[0] == "IDENT":
                    if brace_depth == 0:
                        counts["top_level_records"] += 1
                    else:
                        counts["inner_records"] += 1
        elif val == "new":
            # Match anonymous inner class creation: new [Type]<...> ( [args] ) { ... }
            j = i + 1
            while j < n and tokens[j][0] in ("IDENT", "DOT"):
                j += 1
            if j < n and tokens[j][1] == "<":
                angle_depth = 1
                j += 1
                while j < n and angle_depth > 0:
                    if tokens[j][1] == "<":
                        angle_depth += 1
                    elif tokens[j][1] == ">":
                        angle_depth -= 1
                    elif tokens[j][1] in (";", "{", "}"):
                        break
                    j += 1
            if j < n and tokens[j][1] == "(":
                paren_depth = 1
                j += 1
                while j < n and paren_depth > 0:
                    if tokens[j][1] == "(":
                        paren_depth += 1
                    elif tokens[j][1] == ")":
                        paren_depth -= 1
                    elif tokens[j][1] in (";", "}"):
                        break
                    j += 1
                if j < n and tokens[j][1] == "{":
                    counts["anonymous_classes"] += 1

    return {
        "total_lines": total_lines,
        "code_lines": code_lines,
        "comment_lines": comment_lines,
        "blank_lines": blank_lines,
        **counts,
    }


def analyze_generic_file(filepath):
    """Counts lines, blank lines, and content lines for non-Java files."""
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()
    except Exception:
        return None

    total = len(lines)
    blank = sum(1 for line in lines if not line.strip())
    content = total - blank
    return {"total_lines": total, "blank_lines": blank, "code_lines": content}


def collect_project_files(root_path, use_git=True):
    """
    Collects project files either using `git ls-files` (if available and in git repo)
    or by directory traversal excluding build/cache artifacts.
    """
    root_path = os.path.abspath(root_path)

    if use_git:
        try:
            res = subprocess.run(
                ["git", "ls-files"],
                cwd=root_path,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                encoding="utf-8",
                errors="ignore",
            )
            if res.returncode == 0:
                files = [
                    os.path.normpath(os.path.join(root_path, f.strip()))
                    for f in res.stdout.splitlines()
                    if f.strip()
                ]
                return [f for f in files if os.path.isfile(f)]
        except Exception:
            pass

    # Fallback to filesystem walk
    ignored_dirs = {
        ".git",
        "target",
        ".idea",
        ".venv",
        "bin",
        "build",
        "node_modules",
        "__pycache__",
        ".settings",
    }
    collected = []
    for dirpath, dirnames, filenames in os.walk(root_path):
        dirnames[:] = [d for d in dirnames if d not in ignored_dirs]
        for f in filenames:
            collected.append(os.path.normpath(os.path.join(dirpath, f)))
    return collected


def determine_module_and_scope(filepath, root_path):
    """
    Determines the Maven module name and scope (main vs test) from a file path.
    """
    rel = os.path.relpath(filepath, root_path).replace("\\", "/")
    parts = rel.split("/")
    module = parts[0] if len(parts) > 1 and not parts[0].startswith(".") else "root"

    if "/src/test/" in rel:
        scope = "test"
    elif "/src/main/" in rel:
        scope = "main"
    else:
        scope = "other"

    return module, scope


def run_analysis(root_path, include_non_java=False, use_git=True):
    """Runs complete analysis and returns dictionary of metrics."""
    files = collect_project_files(root_path, use_git=use_git)

    java_files = [f for f in files if f.endswith(".java")]

    overall_java = {
        "files": len(java_files),
        "total_lines": 0,
        "code_lines": 0,
        "comment_lines": 0,
        "blank_lines": 0,
        "top_level_classes": 0,
        "top_level_interfaces": 0,
        "top_level_enums": 0,
        "top_level_records": 0,
        "top_level_annotations": 0,
        "inner_classes": 0,
        "inner_interfaces": 0,
        "inner_enums": 0,
        "inner_records": 0,
        "inner_annotations": 0,
        "anonymous_classes": 0,
    }

    scope_stats = defaultdict(lambda: defaultdict(int))
    module_stats = defaultdict(lambda: defaultdict(int))

    for f in java_files:
        res = analyze_java_file(f)
        if not res:
            continue

        mod, sc = determine_module_and_scope(f, root_path)

        for k, v in res.items():
            overall_java[k] += v
            scope_stats[sc][k] += v
            module_stats[mod][k] += v

        scope_stats[sc]["files"] += 1
        module_stats[mod]["files"] += 1

    # Derived summary metrics
    named_classes = overall_java["top_level_classes"] + overall_java["inner_classes"]
    total_classes = named_classes + overall_java["anonymous_classes"]
    total_types = (
        total_classes
        + overall_java["top_level_interfaces"]
        + overall_java["top_level_enums"]
        + overall_java["top_level_records"]
        + overall_java["top_level_annotations"]
        + overall_java["inner_interfaces"]
        + overall_java["inner_enums"]
        + overall_java["inner_records"]
        + overall_java["inner_annotations"]
    )

    result = {
        "root_path": root_path,
        "java_summary": {
            **overall_java,
            "named_classes": named_classes,
            "total_classes": total_classes,
            "total_types": total_types,
        },
        "scopes": dict(scope_stats),
        "modules": dict(module_stats),
    }

    if include_non_java:
        non_java_stats = defaultdict(lambda: {"files": 0, "total_lines": 0, "blank_lines": 0, "code_lines": 0})
        for f in files:
            if f.endswith(".java") or is_binary_file(f):
                continue
            ext = os.path.splitext(f)[1].lower() or "[no ext]"
            res = analyze_generic_file(f)
            if res:
                non_java_stats[ext]["files"] += 1
                non_java_stats[ext]["total_lines"] += res["total_lines"]
                non_java_stats[ext]["blank_lines"] += res["blank_lines"]
                non_java_stats[ext]["code_lines"] += res["code_lines"]
        result["non_java"] = dict(non_java_stats)

    return result


def generate_text_report(data, show_modules=True, show_non_java=False):
    """Generates plain text (.txt) formatted report with tables."""
    js = data["java_summary"]
    scopes = data["scopes"]
    main_sc = scopes.get("main", {})
    test_sc = scopes.get("test", {})

    out = []
    out.append("=" * 78)
    out.append("                      JMT CODEBASE METRICS SUMMARY")
    out.append("=" * 78)

    out.append("\n[1] CLASSES & TYPES SUMMARY")
    out.append("-" * 78)
    out.append(f"{'Category':<42} {'Main':>10} {'Test':>10} {'Total':>12}")
    out.append("-" * 78)
    out.append(f"{'Java Source Files (.java)':<42} {main_sc.get('files', 0):>10,d} {test_sc.get('files', 0):>10,d} {js['files']:>12,d}")
    out.append(f"{'Top-Level Classes':<42} {main_sc.get('top_level_classes', 0):>10,d} {test_sc.get('top_level_classes', 0):>10,d} {js['top_level_classes']:>12,d}")
    out.append(f"{'Top-Level Interfaces':<42} {main_sc.get('top_level_interfaces', 0):>10,d} {test_sc.get('top_level_interfaces', 0):>10,d} {js['top_level_interfaces']:>12,d}")
    out.append(f"{'Top-Level Enums':<42} {main_sc.get('top_level_enums', 0):>10,d} {test_sc.get('top_level_enums', 0):>10,d} {js['top_level_enums']:>12,d}")
    out.append(f"{'Inner / Nested Classes':<42} {main_sc.get('inner_classes', 0):>10,d} {test_sc.get('inner_classes', 0):>10,d} {js['inner_classes']:>12,d}")
    out.append(f"{'Inner Interfaces & Enums':<42} {(main_sc.get('inner_interfaces', 0) + main_sc.get('inner_enums', 0)):>10,d} {(test_sc.get('inner_interfaces', 0) + test_sc.get('inner_enums', 0)):>10,d} {(js['inner_interfaces'] + js['inner_enums']):>12,d}")
    out.append(f"{'Anonymous Inner Classes':<42} {main_sc.get('anonymous_classes', 0):>10,d} {test_sc.get('anonymous_classes', 0):>10,d} {js['anonymous_classes']:>12,d}")
    out.append("-" * 78)
    out.append(f"{'TOTAL Named Classes (Top + Inner)':<42} {(main_sc.get('top_level_classes', 0) + main_sc.get('inner_classes', 0)):>10,d} {(test_sc.get('top_level_classes', 0) + test_sc.get('inner_classes', 0)):>10,d} {js['named_classes']:>12,d}")
    out.append(f"{'TOTAL Classes (including Anonymous)':<42} {(main_sc.get('top_level_classes', 0) + main_sc.get('inner_classes', 0) + main_sc.get('anonymous_classes', 0)):>10,d} {(test_sc.get('top_level_classes', 0) + test_sc.get('inner_classes', 0) + test_sc.get('anonymous_classes', 0)):>10,d} {js['total_classes']:>12,d}")
    out.append(f"{'TOTAL Java Types (All definitions)':<42} {'-':>10} {'-':>10} {js['total_types']:>12,d}")

    out.append("\n[2] LINES OF CODE (JAVA)")
    out.append("-" * 78)
    out.append(f"{'Category':<32} {'Main':>10} {'Test':>10} {'Total':>12} {'Percent':>9}")
    out.append("-" * 78)
    tot = js['total_lines'] or 1
    out.append(f"{'Source Lines of Code (SLOC)':<32} {main_sc.get('code_lines', 0):>10,d} {test_sc.get('code_lines', 0):>10,d} {js['code_lines']:>12,d} {js['code_lines']/tot*100:>8.1f}%")
    out.append(f"{'Comment Lines':<32} {main_sc.get('comment_lines', 0):>10,d} {test_sc.get('comment_lines', 0):>10,d} {js['comment_lines']:>12,d} {js['comment_lines']/tot*100:>8.1f}%")
    out.append(f"{'Blank Lines':<32} {main_sc.get('blank_lines', 0):>10,d} {test_sc.get('blank_lines', 0):>10,d} {js['blank_lines']:>12,d} {js['blank_lines']/tot*100:>8.1f}%")
    out.append("-" * 78)
    out.append(f"{'TOTAL Raw Lines':<32} {main_sc.get('total_lines', 0):>10,d} {test_sc.get('total_lines', 0):>10,d} {js['total_lines']:>12,d} {'100.0%':>9}")

    if show_modules:
        out.append("\n[3] BREAKDOWN BY MAVEN MODULE")
        out.append("-" * 105)
        out.append(f"{'Module':<24} {'Files':>6} {'SLOC':>9} {'Comments':>9} {'Blanks':>8} {'Total':>9} {'TopCls':>7} {'InnCls':>7} {'AnonCls':>8}")
        out.append("-" * 105)
        sorted_mods = sorted(data["modules"].items(), key=lambda x: x[1]["code_lines"], reverse=True)
        for mod, st in sorted_mods:
            out.append(f"{mod:<24} {st['files']:>6,d} {st['code_lines']:>9,d} {st['comment_lines']:>9,d} {st['blank_lines']:>8,d} {st['total_lines']:>9,d} {st['top_level_classes']:>7,d} {st['inner_classes']:>7,d} {st['anonymous_classes']:>8,d}")
        out.append("-" * 105)

    if show_non_java and "non_java" in data:
        out.append("\n[4] OTHER SOURCE & CONFIG FILES")
        out.append("-" * 60)
        out.append(f"{'Extension':<16} {'Files':>8} {'Blank':>10} {'Total Lines':>14}")
        out.append("-" * 60)
        sorted_nj = sorted(data["non_java"].items(), key=lambda x: x[1]["total_lines"], reverse=True)
        for ext, st in sorted_nj:
            out.append(f"{ext:<16} {st['files']:>8,d} {st['blank_lines']:>10,d} {st['total_lines']:>14,d}")
        out.append("-" * 60)

    out.append("")
    return "\n".join(out)


def generate_markdown_report(data, show_modules=True, show_non_java=False):
    """Generates GitHub Flavored Markdown (.md) formatted report."""
    js = data["java_summary"]
    scopes = data["scopes"]
    main_sc = scopes.get("main", {})
    test_sc = scopes.get("test", {})

    out = []
    out.append("# Codebase Metrics Report\n")
    out.append("### Classes and Types Summary\n")
    out.append("| Metric | Main (`src/main`) | Test (`src/test`) | Total |")
    out.append("| :--- | :---: | :---: | :---: |")
    out.append(f"| **Java Source Files (`.java`)** | {main_sc.get('files', 0):,d} | {test_sc.get('files', 0):,d} | **{js['files']:,d}** |")
    out.append(f"| **Top-Level Classes** | {main_sc.get('top_level_classes', 0):,d} | {test_sc.get('top_level_classes', 0):,d} | **{js['top_level_classes']:,d}** |")
    out.append(f"| **Top-Level Interfaces** | {main_sc.get('top_level_interfaces', 0):,d} | {test_sc.get('top_level_interfaces', 0):,d} | **{js['top_level_interfaces']:,d}** |")
    out.append(f"| **Top-Level Enums** | {main_sc.get('top_level_enums', 0):,d} | {test_sc.get('top_level_enums', 0):,d} | **{js['top_level_enums']:,d}** |")
    out.append(f"| **Inner / Nested Classes** | {main_sc.get('inner_classes', 0):,d} | {test_sc.get('inner_classes', 0):,d} | **{js['inner_classes']:,d}** |")
    out.append(f"| **Inner Interfaces & Enums** | {(main_sc.get('inner_interfaces', 0) + main_sc.get('inner_enums', 0)):,d} | {(test_sc.get('inner_interfaces', 0) + test_sc.get('inner_enums', 0)):,d} | **{(js['inner_interfaces'] + js['inner_enums']):,d}** |")
    out.append(f"| **Anonymous Inner Classes** | {main_sc.get('anonymous_classes', 0):,d} | {test_sc.get('anonymous_classes', 0):,d} | **{js['anonymous_classes']:,d}** |")
    out.append(f"| **Total Named Classes (Top + Inner)** | {(main_sc.get('top_level_classes', 0) + main_sc.get('inner_classes', 0)):,d} | {(test_sc.get('top_level_classes', 0) + test_sc.get('inner_classes', 0)):,d} | **{js['named_classes']:,d}** |")
    out.append(f"| **Total Classes (including Anonymous)** | {(main_sc.get('top_level_classes', 0) + main_sc.get('inner_classes', 0) + main_sc.get('anonymous_classes', 0)):,d} | {(test_sc.get('top_level_classes', 0) + test_sc.get('inner_classes', 0) + test_sc.get('anonymous_classes', 0)):,d} | **{js['total_classes']:,d}** |")
    out.append(f"| **Total Java Types (All definitions)** | - | - | **{js['total_types']:,d}** |\n")

    tot = js['total_lines'] or 1
    out.append("### Lines of Code (Java)\n")
    out.append("| Category | Main | Test | Total | Percentage |")
    out.append("| :--- | :---: | :---: | :---: | :---: |")
    out.append(f"| **Source Lines of Code (SLOC)** | {main_sc.get('code_lines', 0):,d} | {test_sc.get('code_lines', 0):,d} | **{js['code_lines']:,d}** | {js['code_lines']/tot*100:.1f}% |")
    out.append(f"| **Comment Lines** | {main_sc.get('comment_lines', 0):,d} | {test_sc.get('comment_lines', 0):,d} | **{js['comment_lines']:,d}** | {js['comment_lines']/tot*100:.1f}% |")
    out.append(f"| **Blank Lines** | {main_sc.get('blank_lines', 0):,d} | {test_sc.get('blank_lines', 0):,d} | **{js['blank_lines']:,d}** | {js['blank_lines']/tot*100:.1f}% |")
    out.append(f"| **Total Lines** | {main_sc.get('total_lines', 0):,d} | {test_sc.get('total_lines', 0):,d} | **{js['total_lines']:,d}** | 100.0% |\n")

    if show_modules:
        out.append("### Module Breakdown\n")
        out.append("| Module | Files | SLOC (Code) | Comments | Blanks | Total Lines | Top Classes | Inner Classes | Anon Classes |")
        out.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |")
        sorted_mods = sorted(data["modules"].items(), key=lambda x: x[1]["code_lines"], reverse=True)
        for mod, st in sorted_mods:
            out.append(f"| `{mod}` | {st['files']:,d} | {st['code_lines']:,d} | {st['comment_lines']:,d} | {st['blank_lines']:,d} | {st['total_lines']:,d} | {st['top_level_classes']:,d} | {st['inner_classes']:,d} | {st['anonymous_classes']:,d} |")
        out.append("")

    if show_non_java and "non_java" in data:
        out.append("### Other Source & Configuration Files\n")
        out.append("| Extension | Files | Blank Lines | Total Lines |")
        out.append("| :--- | :---: | :---: | :---: |")
        sorted_nj = sorted(data["non_java"].items(), key=lambda x: x[1]["total_lines"], reverse=True)
        for ext, st in sorted_nj:
            out.append(f"| `{ext}` | {st['files']:,d} | {st['blank_lines']:,d} | {st['total_lines']:,d} |")
        out.append("")

    return "\n".join(out)


def generate_json_report(data):
    """Generates structured JSON string."""
    return json.dumps(data, indent=2)


def write_file_safe(filepath, content):
    """Writes content to filepath, creating parent directories if necessary."""
    p = Path(filepath).resolve()
    p.parent.mkdir(parents=True, exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        f.write(content)
    return str(p)


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    default_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    if not os.path.isdir(os.path.join(default_root, "jmt-engine")):
        default_root = os.getcwd()

    default_output_dir = os.path.join(script_dir, "output")

    parser = argparse.ArgumentParser(
        description="Counts classes and lines of code in JMT modules, with file generation support."
    )
    parser.add_argument(
        "--path",
        "-p",
        default=default_root,
        help=f"Project root directory to scan (default: {default_root})",
    )
    parser.add_argument(
        "--format",
        "-f",
        choices=["txt", "text", "table", "md", "markdown", "json", "all"],
        default="txt",
        help="Report format: 'txt' (default), 'markdown', 'json', or 'all'",
    )
    parser.add_argument(
        "--output",
        "-o",
        default=None,
        help="Path to specific output file to generate (e.g. ./metrics.txt, ./report.md)",
    )
    parser.add_argument(
        "--output-dir",
        "-d",
        default=None,
        help=f"Directory to write generated reports into (default: {default_output_dir})",
    )
    parser.add_argument(
        "--generate",
        "-g",
        action="store_true",
        help="Generate and save report file(s) to disk",
    )
    parser.add_argument(
        "--prefix",
        default="code_metrics",
        help="Filename prefix for generated files when saving (default: 'code_metrics')",
    )
    parser.add_argument(
        "--no-modules",
        action="store_true",
        help="Omit per-module breakdown table",
    )
    parser.add_argument(
        "--include-non-java",
        action="store_true",
        help="Include metrics for non-Java source and config files (XML, Python, etc.)",
    )
    parser.add_argument(
        "--no-git",
        action="store_true",
        help="Do not use git ls-files; traverse filesystem directly (skips target/ and .git/)",
    )
    parser.add_argument(
        "--quiet",
        "-q",
        action="store_true",
        help="Suppress printing report content to stdout when saving files",
    )

    args = parser.parse_args()

    # Normalize format alias
    fmt = args.format.lower()
    if fmt in ("table", "text"):
        fmt = "txt"
    elif fmt == "md":
        fmt = "markdown"

    # If --output is given, deduce format from extension if not explicitly specified
    if args.output:
        args.generate = True
        ext = os.path.splitext(args.output)[1].lower()
        if ext in (".txt", ".text") and fmt not in ("markdown", "json", "all"):
            fmt = "txt"
        elif ext in (".md", ".markdown") and fmt not in ("txt", "json", "all"):
            fmt = "markdown"
        elif ext == ".json" and fmt not in ("txt", "markdown", "all"):
            fmt = "json"

    # If format is 'all', generate is automatically implied
    if fmt == "all":
        args.generate = True

    # If --output-dir is specified, generate is implied
    if args.output_dir:
        args.generate = True

    target_output_dir = args.output_dir or default_output_dir

    # Run analysis
    data = run_analysis(
        root_path=args.path,
        include_non_java=args.include_non_java,
        use_git=not args.no_git,
    )

    show_modules = not args.no_modules
    show_non_java = args.include_non_java

    # Build report contents
    text_content = generate_text_report(data, show_modules=show_modules, show_non_java=show_non_java)
    md_content = generate_markdown_report(data, show_modules=show_modules, show_non_java=show_non_java)
    json_content = generate_json_report(data)

    generated_files = []

    # Handle file generation
    if args.generate:
        if args.output:
            # Single file explicit target
            if fmt == "markdown":
                content = md_content
            elif fmt == "json":
                content = json_content
            else:
                content = text_content
            saved = write_file_safe(args.output, content)
            generated_files.append(saved)
        else:
            # Target directory generation
            if fmt in ("txt", "all"):
                filepath = os.path.join(target_output_dir, f"{args.prefix}.txt")
                saved = write_file_safe(filepath, text_content)
                generated_files.append(saved)
            if fmt in ("markdown", "all"):
                filepath = os.path.join(target_output_dir, f"{args.prefix}.md")
                saved = write_file_safe(filepath, md_content)
                generated_files.append(saved)
            if fmt in ("json", "all"):
                filepath = os.path.join(target_output_dir, f"{args.prefix}.json")
                saved = write_file_safe(filepath, json_content)
                generated_files.append(saved)

    # Output to stdout if not quiet
    if not args.quiet:
        if fmt == "json":
            print(json_content)
        elif fmt == "markdown":
            print(md_content)
        elif fmt == "all":
            # For 'all', show text table on stdout
            print(text_content)
        else:
            print(text_content)

    # Print confirmation of generated files
    if generated_files:
        print("\n" + "=" * 78)
        print("Generated file(s):")
        for gf in generated_files:
            print(f"  -> {gf}")
        print("=" * 78)


if __name__ == "__main__":
    main()
