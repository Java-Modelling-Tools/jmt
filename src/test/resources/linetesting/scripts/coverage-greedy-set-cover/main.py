import os
import subprocess
import sys

OPTIMIZER_SCRIPT = "optimize_coverage.py"   # The Set Cover Logic script
REPORT_SCRIPT    = "generate_html_report.py" # The HTML Generator script

def run_script(script_name):
    # Get the directory where this main.py lives
    current_dir = os.path.dirname(os.path.abspath(__file__))
    script_path = os.path.join(current_dir, script_name)
    
    if not os.path.exists(script_path):
        print(f"ERROR: Could not find '{script_name}' in: {current_dir}")
        return False

    print(f"--- Running {script_name} ---")
    try:
        # sys.executable ensures we use the same python interpreter
        subprocess.run([sys.executable, script_path], check=True)
        print(f"--- {script_name} Finished Successfully ---\n")
        return True
    except subprocess.CalledProcessError as e:
        print(f"!!! ERROR: {script_name} failed with exit code {e.returncode}.")
        return False

def main():
    print("="*60)
    print("      JMT COVERAGE OPTIMIZATION SUITE      ")
    print("="*60)
    print("This tool will:")
    print("  1. Analyze your 'full_detailed_coverage.csv'.")
    print("  2. Select the optimal set of tests (Set Cover Algorithm).")
    print("  3. Generate an interactive HTML Dashboard.")
    print("-" * 60 + "\n")

    # Step 1: Analyze and Optimize
    if not run_script(OPTIMIZER_SCRIPT):
        print("Aborting process due to error in Step 1.")
        sys.exit(1)

    # Step 2: Generate Report
    if not run_script(REPORT_SCRIPT):
        print("Aborting process due to error in Step 2.")
        sys.exit(1)

    print("="*60)
    print("      PROCESS COMPLETE!      ")
    print("="*60)
    
    # Construct path to the result for display
    # Note: We used 'output' in the previous scripts
    output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'output')
    html_path = os.path.join(output_dir, 'coverage_dashboard.html')
    
    print(f"Open the following file in your browser:\n")
    print(f"   file://{html_path}")
    print("\n")

if __name__ == "__main__":
    main()