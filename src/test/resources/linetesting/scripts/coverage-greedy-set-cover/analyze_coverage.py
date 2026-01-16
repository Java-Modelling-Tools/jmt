import os
import csv

"""
SCRIPT: Coverage Optimizer (Champion Logic)
-------------------------------------------
ALGORITHM EXPLANATION:

1. THE PHILOSOPHY: "The Gold Standard"
   Unlike simple coverage checks (Yes/No), this script treats coverage as a quantity 
   to be maximized.
   
   - Phase 1 (Benchmarking): It scans every test file to find the "Global Max" 
     instruction count for every single Java class.
     Example: If Test_A covers 20 lines of ClassX, and Test_B covers 100 lines, 
     the "Gold Standard" for ClassX becomes 100.

2. THE SELECTION: "Greedy Set Cover for Maximization"
   The goal is to find the smallest set of files such that EVERY class is covered 
   at its "Gold Standard" level.

   - Step 1: It scores every remaining test file.
     Score = The number of *Pending Classes* for which this file achieves the Global Max.
     (Note: If a file covers a class but only hits 20/100 lines, it gets 0 points 
     for that class. It is considered "Suboptimal".)
   
   - Step 2: It picks the "Champion" (Highest Score).
     This file is added to the 'Keep' list. The classes it championed are marked as 'Done'.
   
   - Step 3: Repeat until all classes are covered at their max observed level.

3. THE DISCARD PILE:
   Files end up here if:
   a) Redundant: They cover classes well, but a previously selected file already covered them.
   b) Suboptimal: They cover classes weakly (e.g., 20 lines vs 100 lines in another file).
"""

def get_coverage_counts(csv_path):
    """
    PASS 1: Scan the CSV to build the maps needed for logic.
    Returns:
        test_data (dict): { 'TestID': { 'full.class.Name': instruction_covered_count } }
        class_max_stats (dict): { 'full.class.Name': { 'max_cov': int, 'best_test': str, 'full_row_data': dict } }
    """

    test_data = {}
    class_max_stats = {} 

    print(f"Phase 1: Analyzing coverage logic from {csv_path}...")
    
    with open(csv_path, 'r') as f:
        header_line = f.readline()
        # Parse headers to map indices if needed, but we know the structure.
        
        for line in f:
            line = line.strip()
            if not line: continue
            parts = line.split(',')
            if len(parts) < 14: continue
            
            metrics = parts[-10:]
            clazz = parts[-11]
            pkg = parts[-12]
            group = parts[-13]
            test_id = ",".join(parts[:-13])
            
            try:
                i_cov = int(metrics[1]) # INSTRUCTION_COVERED
            except ValueError:
                continue

            full_class = f"{pkg}.{clazz}"

            if i_cov >= 0: # Even if 0, we might want to know it exists
                if test_id not in test_data:
                    test_data[test_id] = {}
                test_data[test_id][full_class] = i_cov
                
                # Check Global Max
                current_max = class_max_stats.get(full_class, {}).get('max_cov', -1)
                
                if i_cov > current_max:
                    # Found a new Golden Standard (or first one)
                    class_max_stats[full_class] = {
                        'max_cov': i_cov,
                        'best_test': test_id,
                        'metrics': metrics, # Store raw metrics for later output
                        'group': group,
                        'pkg': pkg,
                        'clazz': clazz
                    }

    return test_data, class_max_stats

def get_decision_maps(test_data, class_max_stats):
    keep_reasons = {}
    pending_classes = set(class_max_stats.keys())
    
    selected_tests = set()
    
    print("Running optimization logic...")
    
    while pending_classes:
        best_test = None
        best_contribution = set()
        
        for test_id, coverage_map in test_data.items():
            if test_id in selected_tests: continue
            
            contributions = set()
            for cls in pending_classes:
                if cls in coverage_map:
                    target_cov = class_max_stats[cls]['max_cov']
                    # Must match the Golden Standard
                    if coverage_map[cls] >= target_cov:
                        contributions.add(cls)
            
            if len(contributions) > len(best_contribution):
                best_contribution = contributions
                best_test = test_id
        
        if best_test is None:
            break
            
        selected_tests.add(best_test)
        pending_classes -= best_contribution
        
        unique_count = len(best_contribution)
        examples = list(best_contribution)[:3]
        ex_str = ", ".join([e.split('.')[-1] for e in examples])
        if len(best_contribution) > 3: ex_str += "..."
        keep_reasons[best_test] = f"Champion for {unique_count} classes (e.g. {ex_str})"

    all_tests = set(test_data.keys())
    discarded_tests = all_tests - selected_tests
    discard_reasons = {}
    
    for test in discarded_tests:
        touched = len(test_data[test])
        if touched == 0:
            discard_reasons[test] = "Useless (Covers 0 instructions)"
        else:
            discard_reasons[test] = "Redundant (Other files cover these classes better or equal)"
            
    return keep_reasons, discard_reasons

def write_split_csvs(input_csv, keep_csv, discard_csv, keep_map, discard_map):
    print("Phase 2: Writing detailed Keep/Discard files...")
    
    base_headers = ["TEST_ID", "GROUP", "PACKAGE", "CLASS", 
                    "INSTRUCTION_MISSED", "INSTRUCTION_COVERED", 
                    "BRANCH_MISSED", "BRANCH_COVERED", 
                    "LINE_MISSED", "LINE_COVERED", 
                    "COMPLEXITY_MISSED", "COMPLEXITY_COVERED", 
                    "METHOD_MISSED", "METHOD_COVERED"]
    final_headers = base_headers + ["DECISION_JUSTIFICATION"]

    with open(input_csv, 'r') as fin, \
         open(keep_csv, 'w', newline='') as fkeep, \
         open(discard_csv, 'w', newline='') as fdiscard:
        
        reader = csv.reader(fin)
        writer_keep = csv.writer(fkeep)
        writer_discard = csv.writer(fdiscard)
        
        writer_keep.writerow(final_headers)
        writer_discard.writerow(final_headers)
        
        next(reader, None) # Skip header
        
        for row in reader:
            if not row or len(row) < 14: continue
            
            metrics_start_idx = len(row) - 10
            group_idx = metrics_start_idx - 3
            test_id_parts = row[:group_idx]
            test_id = ",".join(test_id_parts)
            
            if test_id in keep_map:
                row.append(keep_map[test_id])
                writer_keep.writerow(row)
            elif test_id in discard_map:
                row.append(discard_map[test_id])
                writer_discard.writerow(row)

def write_golden_standard_csv(class_max_stats, output_path):
    print(f"Phase 3: Writing Golden Standard Map to {output_path}...")
    
    headers = ["CLASS_FULL_NAME", "GOLDEN_TEST_FILE", 
               "INSTRUCTION_MISSED", "INSTRUCTION_COVERED", 
               "BRANCH_MISSED", "BRANCH_COVERED", 
               "LINE_MISSED", "LINE_COVERED", 
               "COMPLEXITY_MISSED", "COMPLEXITY_COVERED", 
               "METHOD_MISSED", "METHOD_COVERED"]
    
    with open(output_path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        
        # Sort by class name for readability
        sorted_classes = sorted(class_max_stats.keys())
        
        for full_class in sorted_classes:
            stats = class_max_stats[full_class]
            
            row = [
                full_class,
                stats['best_test']
            ]
            # Add the metrics list (already strictly ordered from parsing)
            row.extend(stats['metrics'])
            
            writer.writerow(row)

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # Input CSV location
    input_csv = os.path.join(script_dir, '..', '..', 'output', 'full_detailed_coverage.csv')
    input_csv = os.path.abspath(input_csv)
    
    # Output Directory
    output_dir = os.path.join(script_dir, 'output')
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        print(f"Created output directory: {output_dir}")

    output_keep = os.path.join(output_dir, 'detailed_suite_keep.csv')
    output_discard = os.path.join(output_dir, 'detailed_suite_discard.csv')
    output_golden = os.path.join(output_dir, 'class_golden_standard.csv')

    if not os.path.exists(input_csv):
        print(f"Error: {input_csv} not found.")
        return

    # Analyze & Benchmark
    test_data, class_max_stats = get_coverage_counts(input_csv)
    
    # Optimize
    keep_reasons, discard_reasons = get_decision_maps(test_data, class_max_stats)
    print(f"Decided to KEEP {len(keep_reasons)} files and DISCARD {len(discard_reasons)} files.")

    # Write Keep/Discard Reports
    write_split_csvs(input_csv, output_keep, output_discard, keep_reasons, discard_reasons)
    
    # Write Golden Standard Report
    write_golden_standard_csv(class_max_stats, output_golden)
    
    print(f"Done!\n -> {output_keep}\n -> {output_discard}\n -> {output_golden}")

if __name__ == "__main__":
    main()