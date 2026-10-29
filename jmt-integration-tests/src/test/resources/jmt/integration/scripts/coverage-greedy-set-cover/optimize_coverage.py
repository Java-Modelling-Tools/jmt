import os
import csv
import sys

# Safely increase CSV field size limit
try:
    csv.field_size_limit(sys.maxsize)
except OverflowError:
    csv.field_size_limit(2147483647) # Fallback to standard 32-bit max integer

"""
SCRIPT: Exact Line-Level Coverage Optimizer (Set Cover)
-------------------------------------------------------
ALGORITHM: Greedy Set Cover
1. Identify every unique (Class, LineNumber) pair covered by the entire test suite.
2. Loop:
   a. Find the test file that covers the HIGHEST number of 'Not-Yet-Covered' lines.
   b. Add it to the 'KEEP' list.
   c. Mark those lines as covered.
3. Repeat until 0 lines remain uncovered.
4. Any file not in the 'KEEP' list is discarded because it adds NO unique value.
"""

def parse_csv_line_data(csv_path):
    """
    Parses the CSV to map TestIDs to the specific set of lines they cover.
    Returns:
        test_map: { 'TestID': set("pkg.Class:12", "pkg.Class:13") }
        universe: set("pkg.Class:12", ...) -> All unique lines covered by the suite.
    """
    test_map = {}
    universe = set()
    
    print(f"Phase 1: Parsing line-level data from {csv_path}...")
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        try:
            header = next(reader) # Skip Header
        except StopIteration:
            return {}, set()
            
        for row in reader:
            if not row or len(row) < 16: continue
            
            # --- PARSE IDENTIFIERS ---
            # Based on CSV structure:
            # ... | CLASS | ... | SPECIFIC_LINES_COVERED | SPECIFIC_LINES_MISSED
            
            # Specific Lines Covered is the 2nd to last column (Index -2)
            lines_str = row[-2] 
            
            # If no lines covered in this class by this test, skip logic
            if not lines_str.strip():
                continue
                
            clazz = row[-13] # Class Name
            pkg = row[-14]   # Package
            group_idx = -15
            
            # Test ID is everything before the Group column
            test_id = ",".join(row[:group_idx])
            
            full_class_name = f"{pkg}.{clazz}"
            
            # --- PARSE LINE NUMBERS ---
            # Convert "10 12 15" -> {10, 12, 15}
            try:
                line_ints = set(int(x) for x in lines_str.split())
            except ValueError:
                continue # Skip malformed data
            
            # --- BUILD SETS ---
            if test_id not in test_map:
                test_map[test_id] = set()
            
            # Create unique keys for every line: "jmt.MyClass:10"
            for line_num in line_ints:
                unique_key = f"{full_class_name}:{line_num}"
                test_map[test_id].add(unique_key)
                universe.add(unique_key)
                
    return test_map, universe

def run_set_cover_optimization(test_map, universe):
    """
    Performs the Greedy Set Cover optimization.
    """
    print(f"Phase 2: Optimizing. Goal: Cover {len(universe)} unique line-targets.")
    
    kept_tests = []
    covered_so_far = set()
    decision_reasons = {}
    
    # Loop until we have covered everything that is possible to cover
    while len(covered_so_far) < len(universe):
        
        remaining_targets = universe - covered_so_far
        
        best_test = None
        best_contribution = set()
        
        # Find the test that covers the MOST remaining targets
        for test_id, covered_lines in test_map.items():
            if test_id in kept_tests: continue 
            
            # Intersection: What lines does this test cover that we haven't hit yet?
            new_lines = covered_lines.intersection(remaining_targets)
            
            if len(new_lines) > len(best_contribution):
                best_contribution = new_lines
                best_test = test_id
        
        # specific safety check: if we found no test, but universe isn't full, 
        # it implies logic error or remaining items are unreachable (shouldn't happen here)
        if best_test is None:
            break
            
        kept_tests.append(best_test)
        covered_so_far.update(best_contribution)
        
        # Create a nice reason string
        count = len(best_contribution)
        # Grab a few examples for the report
        examples = list(best_contribution)[:2]
        ex_str = ", ".join([e.split(':')[-1] for e in examples]) # just line numbers
        decision_reasons[best_test] = f"Adds {count} unique lines not covered by others (e.g., lines {ex_str}...)"

    # Identify Discards
    all_tests = set(test_map.keys())
    kept_set = set(kept_tests)
    discarded_tests = all_tests - kept_set
    
    discard_reasons = {}
    for t in discarded_tests:
        total_lines = len(test_map[t])
        if total_lines == 0:
            discard_reasons[t] = "Empty: Covers 0 lines."
        else:
            discard_reasons[t] = "Redundant: All its lines are already covered by selected champions."
            
    return decision_reasons, discard_reasons

def write_reports(input_csv, output_dir, keep_map, discard_map, base_name):
    print("Phase 3: Writing reports...")
    
    # PREFIX OUTPUT FILES WITH THE SUITE NAME
    keep_csv = os.path.join(output_dir, f'{base_name}_optimized_suite_KEEP.csv')
    discard_csv = os.path.join(output_dir, f'{base_name}_optimized_suite_DISCARD.csv')
    
    # We copy the original CSV structure but add the Decision Column
    with open(input_csv, 'r') as fin, \
         open(keep_csv, 'w', newline='') as fkeep, \
         open(discard_csv, 'w', newline='') as fdiscard:
         
        reader = csv.reader(fin)
        writer_keep = csv.writer(fkeep)
        writer_discard = csv.writer(fdiscard)
        
        # Header
        header = next(reader)
        header.append("OPTIMIZER_DECISION")
        
        writer_keep.writerow(header)
        writer_discard.writerow(header)
        
        for row in reader:
            if len(row) < 15: continue
            
            # Reconstruct Test ID to match our map keys
            # Group is at -15
            test_id = ",".join(row[:-15])
            
            if test_id in keep_map:
                row.append(keep_map[test_id])
                writer_keep.writerow(row)
            elif test_id in discard_map:
                row.append(discard_map[test_id])
                writer_discard.writerow(row)
                
    print(f"Success! Reports written to:\n -> {keep_csv}\n -> {discard_csv}")

def main():
    if len(sys.argv) < 2:
        print("Error: Input CSV path must be provided as an argument.")
        sys.exit(1)

    input_csv = os.path.abspath(sys.argv[1])
    
    if not os.path.exists(input_csv):
        print(f"Error: Input file not found: {input_csv}")
        sys.exit(1)

    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, 'output')
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Extract base name (e.g., "jsim" from "jsim_coverage.csv")
    base_name = os.path.basename(input_csv).replace('_coverage.csv', '')

    test_map, universe = parse_csv_line_data(input_csv)
    
    if not test_map:
        print(f"No valid data or covered lines found in '{os.path.basename(input_csv)}'.")
        sys.exit(1)

    keep_reasons, discard_reasons = run_set_cover_optimization(test_map, universe)
    print(f"RESULTS: Kept {len(keep_reasons)} files. Discarded {len(discard_reasons)} files.")

    write_reports(input_csv, output_dir, keep_reasons, discard_reasons, base_name)

if __name__ == "__main__":
    main()
