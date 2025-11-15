import os
import csv
import xml.etree.ElementTree as ET
import shutil

def parse_measures(xml_path):
    """Return {measure_name: finalValue} for one XML-like .jsimg file."""
    if not os.path.exists(xml_path):
        return {}

    tree = ET.parse(xml_path)
    root = tree.getroot()

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

def compare_jsimg_folders(old_folder, new_folder, csv_output="diff_output.csv", output_folder=None):
    """Compare .jsimg files between old and new folders, write CSV, copy changed files."""
    if not os.path.exists(old_folder):
        raise FileNotFoundError(f"Old folder not found: {old_folder}")
    if not os.path.exists(new_folder):
        raise FileNotFoundError(f"New folder not found: {new_folder}")

    old_files = {f for f in os.listdir(old_folder) if f.endswith(".jsimg")}
    new_files = {f for f in os.listdir(new_folder) if f.endswith(".jsimg")}
    all_files = sorted(old_files.union(new_files))

    rows = []
    changed_files = set()

    for filename in all_files:
        old_path = os.path.join(old_folder, filename)
        new_path = os.path.join(new_folder, filename)

        old_measures = parse_measures(old_path) if filename in old_files else {}
        new_measures = parse_measures(new_path) if filename in new_files else {}

        all_measure_names = sorted(set(old_measures.keys()).union(new_measures.keys()))

        for name in all_measure_names:
            old_val = old_measures.get(name)
            new_val = new_measures.get(name)

            if old_val is None:
                rows.append([filename, name, "", new_val, "ADDED", ""])
                changed_files.add(filename)
            elif new_val is None:
                rows.append([filename, name, old_val, "", "REMOVED", ""])
                changed_files.add(filename)
            else:
                if old_val != new_val:
                    change = new_val - old_val
                    pct = (change / old_val * 100) if old_val != 0 else ""
                    rows.append([filename, name, old_val, new_val, change, pct])
                    changed_files.add(filename)

    # Write CSV
    with open(csv_output, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["Filename", "Measure Name", "Old Value", "New Value", "Change", "% Change"])
        writer.writerows(rows)

    print(f"\nCSV written to {csv_output}")
    print("\nFiles with at least one change:")
    for f in sorted(changed_files):
        print(f)

    # Copy changed files to output folders
    if output_folder:
        prev_out = os.path.join(output_folder, "previous")
        latest_out = os.path.join(output_folder, "latest")
        os.makedirs(prev_out, exist_ok=True)
        os.makedirs(latest_out, exist_ok=True)

        for f in changed_files:
            if f in old_files:
                shutil.copy2(os.path.join(old_folder, f), prev_out)
            if f in new_files:
                shutil.copy2(os.path.join(new_folder, f), latest_out)

        print(f"\nChanged files copied to:\nPrevious: {prev_out}\nLatest: {latest_out}")

def print_csv_clean(csv_path):
    """Print a CSV in a clean, aligned table format."""
    if not os.path.exists(csv_path):
        print(f"CSV file not found: {csv_path}")
        return

    with open(csv_path, newline='') as f:
        reader = csv.reader(f)
        rows = list(reader)

    if not rows:
        print("CSV is empty")
        return

    # Calculate max width for each column
    col_widths = [max(len(str(row[i])) for row in rows) for i in range(len(rows[0]))]

    # Print header
    header = rows[0]
    header_line = " | ".join(str(header[i]).ljust(col_widths[i]) for i in range(len(header)))
    print(header_line)
    print("-" * len(header_line))

    # Print rows
    for row in rows[1:]:
        print(" | ".join(str(row[i]).ljust(col_widths[i]) for i in range(len(row))))

if __name__ == "__main__":
    script_dir = os.path.dirname(os.path.abspath(__file__))

    previous_jmt_version_runs_folder_path = os.path.join(script_dir, "jmt-previous-runs")
    latest_jmt_version_runs_folder_path = os.path.join(script_dir, "jmt-1.3.0-stable-runs")
    output_file_path = os.path.join(script_dir, "comparison_report.csv")
    output_folder = os.path.join(script_dir, "changed_files")

    compare_jsimg_folders(
        previous_jmt_version_runs_folder_path,
        latest_jmt_version_runs_folder_path,
        csv_output=output_file_path,
        output_folder=output_folder
    )

    # print("\nCSV preview:\n")
    # print_csv_clean(output_file_path)
