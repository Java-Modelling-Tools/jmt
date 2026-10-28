import os
import sys
import csv
import xml.etree.ElementTree as ET
import shutil

def parse_measures(xml_path):
    """Return {measure_name: finalValue} for one XML-like .jsimg file."""
    if not os.path.exists(xml_path):
        return {}

    # Guard against 0-byte or unwritten files from aborted runs
    if os.path.getsize(xml_path) == 0:
        return {}

    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except (ET.ParseError, OSError) as e:
        print(f"Warning: Failed to parse XML in '{os.path.basename(xml_path)}': {e}", file=sys.stderr)
        return {}

    measures = {}
    for m in root.iter("measure"):
        name = m.get("name")
        final_value = m.get("finalValue")
        if name and final_value:
            try:
                measures[name] = float(final_value)
            except ValueError:
                pass

    return measures

def compare_jsimg_folders(old_folder, new_folder, csv_output="diff_output.csv", summary_output="summary_output.csv", stability_threshold=5, output_folder=None):
    """Compare .jsimg files, write detailed and summary CSVs, copy changed files."""
    if not os.path.exists(old_folder):
        raise FileNotFoundError(f"Old folder not found: {old_folder}")
    if not os.path.exists(new_folder):
        raise FileNotFoundError(f"New folder not found: {new_folder}")

    # Case-insensitive extension matching (.jsimg or .JSIMG)
    old_files = {f for f in os.listdir(old_folder) if f.lower().endswith(".jsimg")}
    new_files = {f for f in os.listdir(new_folder) if f.lower().endswith(".jsimg")}
    all_files = sorted(old_files.union(new_files))

    detailed_rows = []
    summary_rows = []
    changed_files = set()

    for filename in all_files:
        old_path = os.path.join(old_folder, filename)
        new_path = os.path.join(new_folder, filename)

        old_is_empty = (filename in old_files and os.path.getsize(old_path) == 0)
        new_is_empty = (filename in new_files and os.path.getsize(new_path) == 0)

        old_measures = parse_measures(old_path) if (filename in old_files and not old_is_empty) else {}
        new_measures = parse_measures(new_path) if (filename in new_files and not new_is_empty) else {}

        all_measure_names = sorted(set(old_measures.keys()).union(new_measures.keys()))

        # Summary stats for this file
        file_max_diff = 0.0
        file_issues_count = 0
        file_status = "Unchanged"

        file_has_diff = False

        if new_is_empty:
            detailed_rows.append([filename, "<FILE_STATE>", "Valid Model" if filename in old_files else "", "Empty (0 bytes)", "INCOMPLETE", False])
            file_has_diff = True
            file_status = "Incomplete (0 bytes)"
            file_issues_count += 1
        elif old_is_empty:
            detailed_rows.append([filename, "<FILE_STATE>", "Empty (0 bytes)", "Valid Model" if filename in new_files else "", "INCOMPLETE", False])
            file_has_diff = True
            file_status = "Old File Empty (0 bytes)"
            file_issues_count += 1

        for name in all_measure_names:
            old_val = old_measures.get(name)
            new_val = new_measures.get(name)

            if old_val is None:
                # Metric Added
                detailed_rows.append([filename, name, "", new_val, "ADDED", ""])
                file_has_diff = True
                if not new_is_empty and not old_is_empty:
                    file_status = "Metrics Added"
            elif new_val is None:
                # Metric Removed
                detailed_rows.append([filename, name, old_val, "", "REMOVED", ""])
                file_has_diff = True
                if not new_is_empty and not old_is_empty:
                    file_status = "Metrics Removed"
            else:
                if old_val != new_val:
                    denom = (new_val + old_val) / 2

                    if denom == 0:
                        # Avoid crash if both are 0.0 (though inequality check usually catches this)
                        ratio = 0.0 
                    else:
                        # 1. Calculate the decimal ratio (e.g., 0.048)
                        ratio = abs(new_val - old_val) / denom
                    
                    smape = ratio * 100
                    
                    # 2. Check against the decimal threshold (e.g., 0.05)
                    is_stable = smape < stability_threshold
                    
                    detailed_rows.append([filename, name, old_val, new_val, f"{smape:.6f}", is_stable])
                    
                    # Track max deviation (store as percentage or ratio, just be consistent)
                    file_max_diff = max(file_max_diff, smape)
                    file_has_diff = True
                    
                    if not is_stable:
                        file_issues_count += 1
                        file_status = "Unstable"

        if file_has_diff:
            changed_files.add(filename)
            if file_status == "Unchanged": # Caught by simple inequality but passed stable check
                file_status = "Changed (Stable)"
        
        # Add single row for the summary CSV
        summary_rows.append([filename, file_status, len(all_measure_names), f"{file_max_diff:.6f}", file_issues_count])

    # 1. Write Detailed CSV
    with open(csv_output, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Filename", "Measure Name", "Old Value", "New Value", "SMAPE (%)", f"Is Stable (< {stability_threshold}%)"])
        writer.writerows(detailed_rows)

    print(f"\nDetailed report written to {csv_output}")

    # 2. Write Summary CSV (One row per file)
    with open(summary_output, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Filename", "Overall Status", "Total Metrics", "Max SMAPE (%)", "Unstable Metrics Count"])
        writer.writerows(summary_rows)

    print(f"Summary report written to {summary_output}")

    # Copy changed files
    if output_folder:
        prev_out = os.path.join(output_folder, "previous")
        latest_out = os.path.join(output_folder, "latest")
        os.makedirs(prev_out, exist_ok=True)
        os.makedirs(latest_out, exist_ok=True)

        for f in changed_files:
            if f in old_files and os.path.exists(os.path.join(old_folder, f)):
                shutil.copy2(os.path.join(old_folder, f), prev_out)
            if f in new_files and os.path.exists(os.path.join(new_folder, f)):
                shutil.copy2(os.path.join(new_folder, f), latest_out)

        print(f"\nChanged files copied to:\nPrevious: {prev_out}\nLatest: {latest_out}")

if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))

    # Resolve latest test output folder (target/jmt/integration/output/jsim) with fallbacks
    target_jsim = os.path.normpath(os.path.join(script_dir, '..', '..', '..', '..', '..', '..', '..', 'target', 'jmt', 'integration', 'output', 'jsim'))
    legacy_jsim = os.path.normpath(os.path.join(script_dir, '..', '..', 'output', 'jsim'))
    default_latest = os.path.join(script_dir, "jmt-1.3.0-stable-runs")

    previous_jmt_runs = sys.argv[1] if len(sys.argv) > 1 else os.path.join(script_dir, "jmt-previous-runs")
    if len(sys.argv) > 2:
        latest_jmt_runs = sys.argv[2]
    elif os.path.exists(target_jsim):
        latest_jmt_runs = target_jsim
    elif os.path.exists(legacy_jsim):
        latest_jmt_runs = legacy_jsim
    else:
        latest_jmt_runs = default_latest

    # Output file paths
    detailed_report = os.path.join(script_dir, "comparison_detailed.csv")
    summary_report = os.path.join(script_dir, "comparison_summary.csv")
    output_folder = os.path.join(script_dir, "changed_files")

    compare_jsimg_folders(
        previous_jmt_runs,
        latest_jmt_runs,
        csv_output=detailed_report,
        summary_output=summary_report,
        # stability_threshold=3,
        output_folder=output_folder
    )
