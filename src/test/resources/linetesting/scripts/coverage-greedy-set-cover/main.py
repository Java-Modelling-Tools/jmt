import os
import subprocess
import sys
import time

def run_script(script_name):
    script_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), script_name)
    
    if not os.path.exists(script_path):
        print(f"Error: Could not find '{script_name}' in the current directory.")
        return False

    print(f"Running {script_name}...")
    try:
        # Run the script and capture output to pass it through to the console
        result = subprocess.run([sys.executable, script_path], check=True)
        print(f"{script_name} completed successfully.\n")
        return True
    except subprocess.CalledProcessError as e:
        print(f"Error: {script_name} failed with exit code {e.returncode}.")
        return False

def main():
    print("="*60)
    print("      JMT COVERAGE OPTIMIZATION SUITE      ")
    print("="*60)
    print("This tool will:")
    print("  1. Analyze your Java coverage CSV.")
    print("  2. Select the best test files (Champion Logic).")
    print("  3. Generate an interactive HTML Dashboard.")
    print("-" * 60 + "\n")

    # Step 1: Analyze and Optimize
    if not run_script("analyze_coverage.py"):
        print("Aborting process due to error in Step 1.")
        sys.exit(1)

    # Step 2: Generate Report
    if not run_script("generate_html_report.py"):
        print("Aborting process due to error in Step 2.")
        sys.exit(1)

    print("="*60)
    print("      PROCESS COMPLETE!      ")
    print("="*60)
    
    # Construct path to the result for display
    output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'output')
    html_path = os.path.join(output_dir, 'coverage_dashboard.html')
    
    print(f"Open the following file in your browser:\n")
    print(f"   file://{html_path}")
    print("\n")

if __name__ == "__main__":
    main()