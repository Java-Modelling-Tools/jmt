#!/usr/bin/env python3
"""
JMT Station Utilization Checker
===============================
Scans JMT integration test models (JSIMgraph, JMVA, JSIMwiz) and their simulation
results to detect stations that reach utilization 1.0 (100% busy / saturated bottlenecks).

Default Tolerance: 5% (0.05), capturing all stations with utilization between 0.95 and 1.0.
Output Formats: JSON, Markdown (.md), Plain Text (.txt), and CSV.
Default Output Directory: jmt-integration-tests/src/test/resources/jmt/integration/scripts/utilization-checker/outputs/

Usage:
    python utilization_checker.py [options]

Examples:
    # Run scan with default 5% tolerance and print terminal table
    python utilization_checker.py

    # Export all formats (json, md, txt) to default outputs/ directory
    python utilization_checker.py --format all

    # Custom tolerance (e.g. 2% or 0.02) and custom output file
    python utilization_checker.py --tolerance 2% --md outputs/custom_report.md
"""

import os
import sys
import argparse
import csv
import json
import datetime
import xml.etree.ElementTree as ET
from pathlib import Path

# Configurable Defaults
DEFAULT_UTILIZATION_THRESHOLD = 1.0
DEFAULT_UTILIZATION_TOLERANCE = 0.05  # 5% default tolerance -> captures 0.95 to 1.0+


def parse_tolerance_value(val_str):
    """
    Parses tolerance input string into a decimal ratio.
    Supports '0.05', '5%', or '5' (interpreted as 5%).
    """
    if val_str is None:
        return DEFAULT_UTILIZATION_TOLERANCE
    if isinstance(val_str, (int, float)):
        val = float(val_str)
        return val / 100.0 if val > 1.0 else val

    cleaned = str(val_str).strip()
    if cleaned.endswith("%"):
        return float(cleaned[:-1].strip()) / 100.0
    val = float(cleaned)
    # If user passes 5 or 10 (common percentage integer notation), convert to ratio
    if val > 1.0:
        return val / 100.0
    return val


def resolve_tolerance():
    """
    Resolves utilization tolerance from environment variable or default constant.
    Checks JMT_UTILIZATION_TOLERANCE and UTILIZATION_TOLERANCE.
    """
    env_tol = os.environ.get("JMT_UTILIZATION_TOLERANCE") or os.environ.get("UTILIZATION_TOLERANCE")
    if env_tol:
        try:
            return parse_tolerance_value(env_tol)
        except ValueError:
            print(f"[WARN] Invalid environment tolerance '{env_tol}', falling back to default {DEFAULT_UTILIZATION_TOLERANCE * 100:.1f}%")
    return DEFAULT_UTILIZATION_TOLERANCE


def resolve_default_paths():
    """
    Dynamically locates input test models, Maven target output directories,
    and the default script outputs directory relative to script location.
    """
    script_dir = Path(__file__).resolve().parent

    # Candidate paths for input models
    input_candidates = [
        # From script dir: ../../../input
        script_dir.parents[2] / "input",
        # From repo root
        Path.cwd() / "jmt-integration-tests" / "src" / "test" / "resources" / "jmt" / "integration" / "input",
        # From module root
        Path.cwd() / "src" / "test" / "resources" / "jmt" / "integration" / "input",
    ]

    # Candidate paths for simulated output (Maven target)
    simulation_output_candidates = [
        # From script dir: ../../../../../../../target/jmt/integration/output
        script_dir.parents[6] / "target" / "jmt" / "integration" / "output",
        # From repo root
        Path.cwd() / "jmt-integration-tests" / "target" / "jmt" / "integration" / "output",
        # From module root
        Path.cwd() / "target" / "jmt" / "integration" / "output",
    ]

    resolved_input = None
    for cand in input_candidates:
        if cand.exists() and cand.is_dir():
            resolved_input = cand
            break

    resolved_sim_output = None
    for cand in simulation_output_candidates:
        if cand.exists() and cand.is_dir():
            resolved_sim_output = cand
            break

    # Default output directory for generated reports: .../utilization-checker/outputs/
    default_reports_dir = script_dir / "outputs"

    return resolved_input, resolved_sim_output, default_reports_dir


def parse_model_measures(file_path):
    """
    Parses an XML model or simulation result file (.jsimg, .jmva, .jsimw)
    and extracts all station utilization measures.
    """
    if not os.path.exists(file_path) or os.path.getsize(file_path) == 0:
        return []

    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
    except (ET.ParseError, OSError) as e:
        print(f"[WARN] Failed to parse XML file '{os.path.basename(file_path)}': {e}", file=sys.stderr)
        return []

    measures = []

    # Detect format by file extension or root element
    is_jmva = str(file_path).lower().endswith(".jmva") or root.find(".//stationresults") is not None

    if is_jmva:
        # JMVA XML schema: <solutions> -> <algorithm> -> <stationresults station="..."> -> <classresults customerclass="..."> -> <measure ...>
        for sr in root.iter("stationresults"):
            st_name = sr.get("station", "Unknown")
            for cr in sr.iter("classresults"):
                c_name = cr.get("customerclass", "All classes")
                for m in cr.iter("measure"):
                    m_type = m.get("measureType", "")
                    val_str = m.get("meanValue", "")
                    if not val_str:
                        continue
                    try:
                        val = float(val_str)
                    except ValueError:
                        continue

                    if "utilization" in m_type.lower():
                        measures.append({
                            "station": st_name,
                            "class": c_name,
                            "measure_name": f"{st_name}_{c_name}_Utilization",
                            "measure_type": "Utilization",
                            "node_type": "station",
                            "value": val
                        })
    else:
        # JSIM / JSIMw XML schema: <measure ...> elements inside <results> or model root
        for m in root.iter("measure"):
            m_name = m.get("name", "")
            m_type = m.get("type", m.get("measureType", ""))
            val_str = m.get("finalValue") or m.get("meanValue")
            
            if val_str is None or val_str.strip() == "":
                continue

            try:
                val = float(val_str)
            except ValueError:
                continue

            station = (
                m.get("referenceStation")
                or m.get("referenceNode")
                or m.get("station", "")
            )
            user_class = m.get("referenceClass") or m.get("referenceUserClass") or m.get("class", "All classes")
            node_type = m.get("nodeType", "station")

            # In JMT SimConstants:
            # UTILIZATION = 6, EFFECTIVE_UTILIZATION = 7
            is_util = (
                m_type in ("6", "7", "Utilization", "Effective Utilization")
                or "utilization" in m_name.lower()
                or "utilization" in m_type.lower()
            )

            if is_util:
                # Fallback if station name is not explicitly in attribute
                if not station and "_" in m_name:
                    station = m_name.split("_")[0]

                measures.append({
                    "station": station if station else "System/Unknown",
                    "class": user_class if user_class else "All classes",
                    "measure_name": m_name,
                    "measure_type": "Utilization" if m_type in ("6", "Utilization") else ("Effective Utilization" if m_type in ("7", "Effective") else m_type),
                    "node_type": node_type,
                    "value": val
                })

    return measures


def classify_utilization(val, threshold=1.0, tolerance=0.05):
    """
    Classifies a utilization metric:
    - EXACT_1.0: exactly 1.0 (within 1e-5)
    - STATISTICAL_SATURATION: near 1.0 (between threshold - tolerance and 1.0)
    - MULTI_SERVER_OVERLOAD: > 1.0 (delay stations or multi-server aggregate loads)
    - UNDER_UTILIZED: < threshold - tolerance
    """
    if abs(val - 1.0) < 1e-5:
        return "EXACT_1.0"
    elif val > 1.0:
        return "MULTI_SERVER_OVERLOAD"
    elif val >= (threshold - tolerance):
        return "STATISTICAL_SATURATION"
    else:
        return "UNDER_UTILIZED"


def scan_directory(directory_path, source_label, threshold=1.0, tolerance=0.05, show_all=False):
    """
    Recursively scans directory_path for JMT model files and filters station utilization.
    """
    results = []
    if not directory_path or not os.path.exists(directory_path):
        return results

    supported_extensions = {".jsimg", ".jmva", ".jsimw"}

    for root_dir, _, files in os.walk(directory_path):
        for f in sorted(files):
            ext = os.path.splitext(f)[1].lower()
            if ext not in supported_extensions:
                continue

            file_path = os.path.join(root_dir, f)
            rel_path = os.path.relpath(file_path, directory_path)
            measures = parse_model_measures(file_path)

            for m in measures:
                category = classify_utilization(m["value"], threshold=threshold, tolerance=tolerance)
                is_match = show_all or (category in ("EXACT_1.0", "STATISTICAL_SATURATION", "MULTI_SERVER_OVERLOAD"))

                if is_match:
                    results.append({
                        "source": source_label,
                        "file_name": f,
                        "relative_path": rel_path,
                        "station": m["station"],
                        "class": m["class"],
                        "measure_name": m["measure_name"],
                        "measure_type": m["measure_type"],
                        "node_type": m["node_type"],
                        "value": m["value"],
                        "category": category
                    })

    return results


def group_results_by_file(results):
    """
    Groups results by unique model file, aggregating the stations
    found for each file and computing summary statistics.
    Returns:
      unique_file_names: sorted list of unique file names
      unique_base_models: sorted list of unique base test model names (stripping intermediate .legacy. tags)
      files_summary: list of dicts with file_name, source, relative_path, stations, max_utilization
    """
    files_map = {}
    for r in results:
        key = (r["file_name"], r["source"])
        if key not in files_map:
            files_map[key] = {
                "file_name": r["file_name"],
                "source": r["source"],
                "relative_path": r["relative_path"],
                "stations": [],
                "max_utilization": r["value"],
                "primary_category": r["category"]
            }
        files_map[key]["stations"].append({
            "station": r["station"],
            "class": r["class"],
            "measure_name": r["measure_name"],
            "value": r["value"],
            "category": r["category"]
        })
        if r["value"] > files_map[key]["max_utilization"]:
            files_map[key]["max_utilization"] = r["value"]
            files_map[key]["primary_category"] = r["category"]

    unique_file_names = sorted(set(r["file_name"] for r in results))
    unique_base_models = sorted(set(r["file_name"].replace(".legacy.jsimg", ".jsimg").replace(".legacy.JSIMG", ".JSIMG") for r in results))
    files_summary = sorted(list(files_map.values()), key=lambda x: (x["source"], x["file_name"]))
    return unique_file_names, unique_base_models, files_summary


def format_text_table(results, threshold=1.0, tolerance=0.05):
    """
    Formats results as plain-text ASCII table, including a dedicated
    section listing all files that have at least one station in the utilization list.
    """
    lines = []
    lines.append("=" * 105)
    lines.append("                      JMT INTEGRATION TEST STATIONS AT UTILIZATION >= 1.0")
    lines.append("=" * 105)
    lines.append(f"Target Threshold: {threshold:.2f} | Tolerance: +/- {tolerance * 100:.1f}% (Range: [{threshold - tolerance:.4f}, {threshold + tolerance:.4f}])\n")

    if not results:
        lines.append("No stations found matching the specified utilization criteria.")
        lines.append("=" * 105)
        return "\n".join(lines)

    header_fmt = "{:<18} {:<32} {:<18} {:<15} {:<10} {:<12}"
    lines.append(header_fmt.format("Source", "Model File", "Station", "User Class", "Utilization", "Status"))
    lines.append("-" * 105)

    row_fmt = "{:<18} {:<32} {:<18} {:<15} {:<10.6f} {:<12}"

    exact_matches = [r for r in results if r["category"] == "EXACT_1.0"]
    stat_matches = [r for r in results if r["category"] == "STATISTICAL_SATURATION"]
    multi_matches = [r for r in results if r["category"] == "MULTI_SERVER_OVERLOAD"]
    other_matches = [r for r in results if r["category"] == "UNDER_UTILIZED"]

    if exact_matches:
        lines.append("[ EXACT 100% UTILIZATION (U = 1.0) ]")
        for r in exact_matches:
            display_name = (r["file_name"][:29] + "...") if len(r["file_name"]) > 32 else r["file_name"]
            lines.append(row_fmt.format(r["source"], display_name, r["station"][:17], r["class"][:14], r["value"], r["category"]))
        lines.append("-" * 105)

    if stat_matches:
        lines.append(f"[ STATISTICAL SATURATION ({threshold - tolerance:.4f} <= U < 1.0, Tolerance: {tolerance * 100:.1f}%) ]")
        for r in stat_matches:
            display_name = (r["file_name"][:29] + "...") if len(r["file_name"]) > 32 else r["file_name"]
            lines.append(row_fmt.format(r["source"], display_name, r["station"][:17], r["class"][:14], r["value"], r["category"]))
        lines.append("-" * 105)

    if multi_matches:
        lines.append("[ MULTI-SERVER / DELAY AGGREGATE LOAD (U > 1.0) ]")
        for r in multi_matches:
            display_name = (r["file_name"][:29] + "...") if len(r["file_name"]) > 32 else r["file_name"]
            lines.append(row_fmt.format(r["source"], display_name, r["station"][:17], r["class"][:14], r["value"], r["category"]))
        lines.append("-" * 105)

    if other_matches:
        lines.append("[ OTHER MEASURED STATIONS ]")
        for r in other_matches:
            display_name = (r["file_name"][:29] + "...") if len(r["file_name"]) > 32 else r["file_name"]
            lines.append(row_fmt.format(r["source"], display_name, r["station"][:17], r["class"][:14], r["value"], r["category"]))
        lines.append("-" * 105)

    # Dedicated Section: Files with at least one station in the utilization list
    unique_file_names, unique_base_models, files_summary = group_results_by_file(results)
    lines.append("\n" + "=" * 105)
    lines.append(f"         FILES WITH AT LEAST ONE STATION IN UTILIZATION LIST ({len(unique_base_models)} Base Models)")
    lines.append("=" * 105)
    lines.append(f"Unique Base Models ({len(unique_base_models)} files):")
    for bname in unique_base_models:
        lines.append(f"  - {bname}")

    lines.append("-" * 105)
    lines.append("Breakdown by Test Model Artifact & Saturated Station(s):")
    for idx, f_info in enumerate(files_summary, start=1):
        stations_desc = ", ".join([f"{st['station']} (U={st['value']:.4f})" for st in f_info["stations"]])
        lines.append(f"  {idx:2d}. {f_info['file_name']} [{f_info['source']}]")
        lines.append(f"      -> Saturated Station(s) ({len(f_info['stations'])}): {stations_desc}")

    lines.append("=" * 105)
    lines.append(f"Total Saturated Base Models: {len(unique_base_models)} | Total Saturated Station Measures: {len(results)}")
    lines.append("=" * 105)

    return "\n".join(lines)


def export_markdown(results, file_path, threshold=1.0, tolerance=0.05):
    """
    Exports results as a structured Markdown (.md) document with tables,
    including a dedicated section listing all files that have at least one station in the list.
    """
    os.makedirs(os.path.dirname(os.path.abspath(file_path)), exist_ok=True)
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    exact_matches = [r for r in results if r["category"] == "EXACT_1.0"]
    stat_matches = [r for r in results if r["category"] == "STATISTICAL_SATURATION"]
    multi_matches = [r for r in results if r["category"] == "MULTI_SERVER_OVERLOAD"]

    unique_files = len(set(r["file_name"] for r in exact_matches + stat_matches))
    unique_stations = len(set((r["file_name"], r["station"]) for r in exact_matches + stat_matches))

    unique_file_names, unique_base_models, files_summary = group_results_by_file(results)

    lines = [
        "# JMT Station Utilization Report",
        "",
        f"- **Generated At**: `{timestamp}`",
        f"- **Utilization Threshold**: `{threshold:.2f}`",
        f"- **Tolerance Range**: `+/- {tolerance * 100:.1f}%` (Captures: `[{threshold - tolerance:.4f}, {threshold + tolerance:.4f}]`)",
        f"- **Total Saturated Models**: `{unique_files}`",
        f"- **Total Saturated Stations**: `{unique_stations}`",
        f"- **Unique Base Models**: `{len(unique_base_models)}`",
        f"- **Unique Matching Artifacts**: `{len(unique_file_names)}`",
        "",
        "---",
        "",
        "## 1. Files with at least one Station in Utilization List",
        "",
        f"The following **{len(unique_base_models)} unique base model files** contain at least one station matching the utilization criteria:",
        "",
        "### 1.1 Unique Base Models Index",
        "",
    ]

    for bname in unique_base_models:
        lines.append(f"- `{bname}`")

    lines.extend([
        "",
        "### 1.2 Breakdown by Model File Artifact",
        "",
        "| # | Model File | Source | Saturated Station(s) | Max Utilization | Primary Category |",
        "| :-: | :--- | :--- | :--- | :---: | :--- |",
    ])

    for idx, f_info in enumerate(files_summary, start=1):
        stations_desc = ", ".join([f"`{st['station']}` ({st['value']:.4f})" for st in f_info["stations"]])
        lines.append(
            f"| {idx} | `{f_info['file_name']}` | {f_info['source']} | {stations_desc} | **{f_info['max_utilization']:.6f}** | {f_info['primary_category']} |"
        )

    lines.extend([
        "",
        "---",
        "",
        "## 2. Exact 100% Saturation ($U = 1.0$)",
        "",
        "| Source | Model File | Station | User Class | Measure Type | Utilization |",
        "| :--- | :--- | :--- | :--- | :--- | :---: |",
    ])

    if exact_matches:
        for r in exact_matches:
            lines.append(f"| {r['source']} | `{r['file_name']}` | `{r['station']}` | `{r['class']}` | {r['measure_type']} | **{r['value']:.6f}** |")
    else:
        lines.append("| - | *No exact 1.0 saturation matches* | - | - | - | - |")

    lines.extend([
        "",
        f"## 3. Statistical Saturation ({threshold - tolerance:.4f} <= U < 1.0, {tolerance * 100:.1f}% Tolerance)",
        "",
        "| Source | Model File | Station | User Class | Measure Type | Utilization |",
        "| :--- | :--- | :--- | :--- | :--- | :---: |",
    ])

    if stat_matches:
        for r in stat_matches:
            lines.append(f"| {r['source']} | `{r['file_name']}` | `{r['station']}` | `{r['class']}` | {r['measure_type']} | **{r['value']:.6f}** |")
    else:
        lines.append("| - | *No near-saturation matches* | - | - | - | - |")

    lines.extend([
        "",
        "## 4. Multi-Server & Delay Station Aggregate Load ($U > 1.0$)",
        "",
        "| Source | Model File | Station | User Class | Measure Type | Utilization |",
        "| :--- | :--- | :--- | :--- | :--- | :---: |",
    ])

    if multi_matches:
        for r in multi_matches:
            lines.append(f"| {r['source']} | `{r['file_name']}` | `{r['station']}` | `{r['class']}` | {r['measure_type']} | **{r['value']:.6f}** |")
    else:
        lines.append("| - | *No multi-server overload matches* | - | - | - | - |")

    lines.append("")
    with open(file_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"[INFO] Markdown report exported to: {file_path}")


def export_text(results, file_path, threshold=1.0, tolerance=0.05):
    """Exports results to a plain-text (.txt) file."""
    os.makedirs(os.path.dirname(os.path.abspath(file_path)), exist_ok=True)
    table_content = format_text_table(results, threshold=threshold, tolerance=tolerance)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(table_content)
    print(f"[INFO] Text report exported to: {file_path}")


def export_json(results, file_path, threshold=1.0, tolerance=0.05):
    """Exports results to a structured JSON (.json) file."""
    os.makedirs(os.path.dirname(os.path.abspath(file_path)), exist_ok=True)
    unique_file_names, unique_base_models, files_summary = group_results_by_file(results)
    data = {
        "generated_at": datetime.datetime.now().isoformat(),
        "threshold": threshold,
        "tolerance": tolerance,
        "tolerance_percentage": f"{tolerance * 100:.1f}%",
        "lower_bound": threshold - tolerance,
        "upper_bound": threshold + tolerance,
        "total_records": len(results),
        "unique_base_models_count": len(unique_base_models),
        "unique_base_models": unique_base_models,
        "unique_artifacts_count": len(unique_file_names),
        "unique_artifacts": unique_file_names,
        "files_summary": files_summary,
        "results": results
    }
    with open(file_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"[INFO] JSON report exported to: {file_path}")


def export_csv(results, file_path):
    """Exports results to a CSV file."""
    os.makedirs(os.path.dirname(os.path.abspath(file_path)), exist_ok=True)
    with open(file_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Source", "File Name", "Relative Path", "Station", "User Class", "Measure Name", "Measure Type", "Node Type", "Utilization", "Category"])
        for r in results:
            writer.writerow([
                r["source"], r["file_name"], r["relative_path"], r["station"],
                r["class"], r["measure_name"], r["measure_type"], r["node_type"],
                f"{r['value']:.6f}", r["category"]
            ])
    print(f"[INFO] CSV report exported to: {file_path}")


def main():
    default_tolerance = resolve_tolerance()

    parser = argparse.ArgumentParser(
        description="Scans JMT integration test models to find stations operating at utilization 1.0 (saturation/bottlenecks)."
    )
    parser.add_argument(
        "--threshold", type=float, default=DEFAULT_UTILIZATION_THRESHOLD,
        help=f"Utilization threshold (default: {DEFAULT_UTILIZATION_THRESHOLD})"
    )
    parser.add_argument(
        "--tolerance", "-t", type=str, default=str(default_tolerance),
        help=f"Utilization tolerance as ratio or percentage (e.g. '0.05' or '5%%', default: {default_tolerance * 100:.1f}%%)"
    )
    parser.add_argument("--input-dir", type=str, default=None, help="Path to input test models directory")
    parser.add_argument("--sim-dir", type=str, default=None, help="Path to simulated output directory")
    parser.add_argument(
        "--outputs-dir", type=str, default=None,
        help="Directory to save generated output reports (defaults to script's outputs/ directory)"
    )
    parser.add_argument("--inputs-only", action="store_true", help="Scan only input oracle models (skip simulation outputs)")
    parser.add_argument("--outputs-only", action="store_true", help="Scan only simulated output models (skip input models)")
    parser.add_argument("--all", action="store_true", help="Include all measured stations regardless of utilization")
    parser.add_argument(
        "--format", "-f", type=str, default=None,
        help="Comma-separated output formats to generate (options: json, md, txt, csv, or 'all')"
    )
    parser.add_argument("--json", nargs="?", const="default", default=None, help="Export JSON report (optional custom path)")
    parser.add_argument("--md", nargs="?", const="default", default=None, help="Export Markdown report (optional custom path)")
    parser.add_argument("--txt", nargs="?", const="default", default=None, help="Export Plain Text report (optional custom path)")
    parser.add_argument("--csv", nargs="?", const="default", default=None, help="Export CSV report (optional custom path)")

    args = parser.parse_args()

    # Parse and validate tolerance
    tolerance = parse_tolerance_value(args.tolerance)

    default_input, default_sim_output, default_reports_dir = resolve_default_paths()
    input_dir = Path(args.input_dir).resolve() if args.input_dir else default_input
    sim_dir = Path(args.sim_dir).resolve() if args.sim_dir else default_sim_output
    reports_dir = Path(args.outputs_dir).resolve() if args.outputs_dir else default_reports_dir

    scan_inputs = not args.outputs_only
    scan_outputs = not args.inputs_only

    input_results = []
    if scan_inputs:
        print(f"[INFO] Scanning input models in: {input_dir}")
        input_results = scan_directory(input_dir, "Input Oracle", threshold=args.threshold, tolerance=tolerance, show_all=args.all)

    sim_results = []
    if scan_outputs:
        print(f"[INFO] Scanning simulated outputs in: {sim_dir}")
        sim_results = scan_directory(sim_dir, "Simulated Output", threshold=args.threshold, tolerance=tolerance, show_all=args.all)

    combined_results = input_results + sim_results

    # Print summary table to stdout
    print(format_text_table(combined_results, threshold=args.threshold, tolerance=tolerance))

    # Parse format flags
    requested_formats = set()
    if args.format:
        fmt_tokens = [t.strip().lower() for t in args.format.split(",")]
        if "all" in fmt_tokens:
            requested_formats.update(["json", "md", "txt", "csv"])
        else:
            requested_formats.update(fmt_tokens)

    # Process individual format arguments
    if args.json is not None:
        requested_formats.add("json")
    if args.md is not None:
        requested_formats.add("md")
    if args.txt is not None:
        requested_formats.add("txt")
    if args.csv is not None:
        requested_formats.add("csv")

    # Export requested formats
    if "json" in requested_formats:
        path = args.json if (args.json and args.json != "default") else str(reports_dir / "utilization_report.json")
        export_json(combined_results, path, threshold=args.threshold, tolerance=tolerance)

    if "md" in requested_formats:
        path = args.md if (args.md and args.md != "default") else str(reports_dir / "utilization_report.md")
        export_markdown(combined_results, path, threshold=args.threshold, tolerance=tolerance)

    if "txt" in requested_formats:
        path = args.txt if (args.txt and args.txt != "default") else str(reports_dir / "utilization_report.txt")
        export_text(combined_results, path, threshold=args.threshold, tolerance=tolerance)

    if "csv" in requested_formats:
        path = args.csv if (args.csv and args.csv != "default") else str(reports_dir / "utilization_report.csv")
        export_csv(combined_results, path)


if __name__ == "__main__":
    main()
