import os
import csv
import json
import html
import sys

SOURCE_CODE_ROOT = os.path.join("..", "..", "..", "..", "..", "main", "java")

def read_csv_data(file_path):
    if not os.path.exists(file_path):
        print(f"Warning: {file_path} not found.")
        return []

    data = []
    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Basic stats
            try:
                i_cov = int(row.get('INSTRUCTION_COVERED', 0))
                i_miss = int(row.get('INSTRUCTION_MISSED', 0))
            except: 
                continue

            pkg = row.get('PACKAGE', '')
            cls_raw = row.get('CLASS', '')
            
            # --- ROBUST CLASS NAME EXTRACTION ---
            # Handle "jmt/pkg/MyClass" or "jmt.pkg.MyClass"
            cls_clean = cls_raw.replace('\\', '/').replace('.', '/')
            if '/' in cls_clean:
                cls_simple = cls_clean.split('/')[-1]
            else:
                cls_simple = cls_clean

            # Parse line numbers
            cov_str = row.get('SPECIFIC_LINES_COVERED', '')
            miss_str = row.get('SPECIFIC_LINES_MISSED', '')
            
            cov_lines = set()
            if cov_str and cov_str.strip():
                try: cov_lines = set(int(x) for x in cov_str.split())
                except ValueError: pass

            miss_lines = set()
            if miss_str and miss_str.strip():
                try: miss_lines = set(int(x) for x in miss_str.split())
                except ValueError: pass

            # Find existing entry for this test file or create new
            test_id = row.get('TEST_ID', 'Unknown')
            
            entry = next((item for item in data if item["name"] == test_id), None)
            if not entry:
                entry = {
                    "name": test_id,
                    "justification": row.get('OPTIMIZER_DECISION', 'N/A'),
                    "total_instructions": 0,
                    "classes_touched": 0,
                    "details": []
                }
                data.append(entry)
            
            entry["total_instructions"] += i_cov
            entry["classes_touched"] += 1
            
            # Read Source Code & Generate HTML View
            source_html = generate_source_view(pkg, cls_simple, cov_lines, miss_lines)

            entry["details"].append({
                "class_name": f"{pkg}.{cls_simple}",
                "inst_cov": i_cov,
                "inst_miss": i_miss,
                "source_html": source_html
            })
            
    # Sort by coverage contribution
    data.sort(key=lambda x: x["total_instructions"], reverse=True)
    return data

def generate_source_view(pkg, cls_simple, cov_lines, miss_lines):
    """
    Reads the .java file and returns an HTML string with colored lines.
    """
    pkg_path = pkg.replace('.', os.sep).replace('/', os.sep)
    rel_path = os.path.join(pkg_path, f"{cls_simple}.java")
    full_path = os.path.join(SOURCE_CODE_ROOT, rel_path)
    full_path = os.path.normpath(full_path)
    
    if not os.path.exists(full_path):
        return f"<div style='padding:15px; color:#999; font-style:italic;'>Source file not found.<br><small>{full_path}</small></div>"

    html_lines = []
    try:
        with open(full_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        html_lines.append("<table class='code-table'>")
        for i, raw_line in enumerate(lines, start=1):
            line_content = html.escape(raw_line.rstrip())
            
            row_class = ""
            if i in cov_lines:
                row_class = "cov-line"
            elif i in miss_lines:
                row_class = "miss-line"
                
            html_lines.append(
                f"<tr class='{row_class}'>"
                f"<td class='line-num'>{i}</td>"
                f"<td class='code-text'><pre>{line_content}</pre></td>"
                f"</tr>"
            )
        html_lines.append("</table>")
        
    except Exception as e:
        return f"Error reading source: {str(e)}"

    return "".join(html_lines)

def generate_html(keep_data, discard_data, output_path, base_name):
    total_keep = len(keep_data)
    total_discard = len(discard_data)
    total_files = total_keep + total_discard
    reduction_pct = (total_discard / total_files * 100) if total_files > 0 else 0
    
    json_keep = json.dumps(keep_data)
    json_discard = json.dumps(discard_data)
    
    # Chart Data
    top_files = keep_data[:10]
    chart_labels = json.dumps([f["name"][:20] + "..." for f in top_files])
    chart_values = json.dumps([f["total_instructions"] for f in top_files])

    title = f"{base_name.upper()} Coverage Dashboard"

    html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title}</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        :root {{ --primary: #2c3e50; --success: #27ae60; --danger: #e74c3c; --bg: #f4f4f9; --green-bg: #d4edda; --red-bg: #f8d7da; }}
        body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: var(--bg); margin: 0; padding: 20px; color: #333; }}
        .container {{ max-width: 1400px; margin: 0 auto; }}
        
        /* CARDS */
        .stats-grid {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 30px; }}
        .card {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); text-align: center; }}
        .card h2 {{ margin: 0; font-size: 2.5em; color: #3498db; }}
        .card p {{ margin: 5px 0 0; color: #7f8c8d; font-weight: 600; }}
        
        /* CHARTS */
        .charts-row {{ display: flex; gap: 20px; margin-bottom: 30px; height: 350px; }}
        .chart-box {{ flex: 1; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); position: relative; }}

        /* TABS */
        .tab-nav {{ display: flex; margin-bottom: 20px; border-bottom: 2px solid #ddd; }}
        .tab-btn {{ padding: 12px 24px; cursor: pointer; border: none; background: none; font-size: 1.1em; color: #7f8c8d; font-weight: 600; transition: color 0.2s; }}
        .tab-btn:hover {{ color: var(--primary); }}
        .tab-btn.active {{ color: var(--primary); border-bottom: 3px solid var(--primary); }}
        .tab-content {{ display: none; }}
        .tab-content.active {{ display: block; }}

        /* TABLES & LISTS */
        .table-container {{ background: white; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); overflow: hidden; }}
        
        /* SEARCH */
        .search-box {{ float: right; padding: 8px; border: 1px solid #ddd; border-radius: 4px; width: 250px; margin-bottom: 10px; }}
        .section-header {{ margin-bottom: 15px; overflow: hidden; }}

        /* HIERARCHY STYLES */
        table {{ width: 100%; border-collapse: collapse; table-layout: fixed; }}
        th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; word-wrap: break-word; vertical-align: middle; }}
        th {{ background-color: var(--primary); color: white; }}
        
        /* Level 1: File Row */
        tr.parent-row {{ cursor: pointer; background: white; }}
        tr.parent-row:hover {{ background-color: #f1f1f1; }}
        .expand-btn {{ font-weight: bold; display: inline-block; width: 24px; height: 24px; line-height: 24px; text-align: center; background: #eee; border-radius: 50%; margin-right: 10px; }}

        /* Level 2: Classes List */
        tr.child-row {{ display: none; background-color: #f9f9f9; }}
        .inner-table {{ width: 95%; margin: 10px auto; border: 1px solid #ddd; background: white; }}
        .inner-table th {{ background: #ecf0f1; color: #2c3e50; font-size: 0.9em; }}
        
        /* Level 3: Source Code */
        tr.code-row {{ display: none; }}
        .code-container {{ padding: 0; background: #fff; border-top: 1px solid #eee; }}
        
        /* CODE VIEW STYLING */
        .code-table {{ width: 100%; font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; border-collapse: collapse; }}
        .code-table td {{ padding: 2px 5px; border: none; }}
        .line-num {{ width: 40px; text-align: right; color: #999; border-right: 1px solid #ddd; background: #fafafa; user-select: none; }}
        .code-text pre {{ margin: 0; tab-size: 4; white-space: pre-wrap; }}
        
        .cov-line {{ background-color: var(--green-bg); }}
        .miss-line {{ background-color: var(--red-bg); }}

        .badge {{ padding: 4px 8px; border-radius: 4px; font-size: 0.85em; font-weight: bold; }}
        .badge-keep {{ background-color: #d5f5e3; color: var(--success); }}
        .badge-discard {{ background-color: #fadbd8; color: var(--danger); }}
        
        .cov-val {{ color: var(--success); font-weight: bold; }}
        .miss-val {{ color: #e67e22; font-size: 0.9em; }}
    </style>
</head>
<body>

<div class="container">
    <h1 style="text-align:center; margin-bottom: 5px;">Coverage Optimization Dashboard</h1>

    <p style="text-align:center; color:#7f8c8d; font-size:1.2em; margin-top:0; margin-bottom:30px;">
        Generated from: <strong style="color:var(--primary);">{base_name}</strong>
    </p>
    
    <div class="stats-grid">
        <div class="card"><h2>{total_files}</h2><p>Total Files</p></div>
        <div class="card"><h2>{total_keep}</h2><p>Kept</p></div>
        <div class="card"><h2>{total_discard}</h2><p>Discarded</p></div>
        <div class="card"><h2>{reduction_pct:.1f}%</h2><p>Reduction</p></div>
    </div>

    <div class="charts-row">
        <div class="chart-box"><canvas id="splitChart"></canvas></div>
        <div class="chart-box"><canvas id="barChart"></canvas></div>
    </div>

    <div class="tab-nav">
        <button class="tab-btn active" onclick="openTab('tabKeep')">Optimized Suite (Keep)</button>
        <button class="tab-btn" onclick="openTab('tabDiscard')">Redundant Files (Discard)</button>
    </div>

    <div id="tabKeep" class="tab-content active">
        <div class="section-header">
            <input type="text" id="searchKeep" class="search-box" placeholder="Search file..." onkeyup="filterTable('keepTableBody', 'searchKeep')">
        </div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th style="width: 50px"></th>
                        <th style="width: 30%">Test File</th>
                        <th style="width: 30%">Decision / Justification</th>
                        <th style="width: 15%">Classes</th>
                        <th style="width: 15%">Instr. Covered</th>
                    </tr>
                </thead>
                <tbody id="keepTableBody"></tbody>
            </table>
        </div>
    </div>

    <div id="tabDiscard" class="tab-content">
        <div class="section-header">
            <input type="text" id="searchDiscard" class="search-box" placeholder="Search file..." onkeyup="filterTable('discardTableBody', 'searchDiscard')">
        </div>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th style="width: 50px"></th>
                        <th style="width: 30%">Test File</th>
                        <th style="width: 30%">Reason</th>
                        <th style="width: 15%">Classes</th>
                        <th style="width: 15%">Instr. Covered</th>
                    </tr>
                </thead>
                <tbody id="discardTableBody"></tbody>
            </table>
        </div>
    </div>

</div>

<script>
    const keepData = {json_keep};
    const discardData = {json_discard};

    // --- RENDER TABLE ---
    function renderTable(tbodyId, data, isKeep) {{
        const tbody = document.getElementById(tbodyId);
        let html = "";
        
        data.forEach((file, fIdx) => {{
            const fileId = tbodyId + '_f_' + fIdx;
            
            // Build Inner Classes Rows
            let classesHtml = "";
            file.details.forEach((det, cIdx) => {{
                const codeId = fileId + '_c_' + cIdx;
                classesHtml += `
                <tr style="cursor:pointer;" onclick="toggleDisplay('${{codeId}}')">
                    <td style="width:30px; text-align:center; color:#7f8c8d;">&#9654;</td>
                    <td><b>${{det.class_name}}</b></td>
                    <td colspan="3"><span class="cov-val">${{det.inst_cov}}</span> / <span class="miss-val">${{det.inst_miss}}</span> (Instructions)</td>
                </tr>
                <tr id="${{codeId}}" class="code-row">
                    <td colspan="5">
                        <div class="code-container">
                            ${{det.source_html}}
                        </div>
                    </td>
                </tr>
                `;
            }});

            // Build Parent File Row
            html += `
            <tr class="parent-row" onclick="toggleFile('${{fileId}}', this)">
                <td style="text-align:center;"><span class="expand-btn">+</span></td>
                <td title="${{file.name}}">${{file.name}}</td>
                <td><span class="badge ${{isKeep ? 'badge-keep' : 'badge-discard'}}">${{file.justification}}</span></td>
                <td>${{file.classes_touched}}</td>
                <td>${{file.total_instructions}}</td>
            </tr>
            <tr id="${{fileId}}" class="child-row">
                <td colspan="5" style="padding:10px 20px;">
                    <table class="inner-table">
                        ${{classesHtml}}
                    </table>
                </td>
            </tr>
            `;
        }});
        tbody.innerHTML = html;
    }}

    // --- UI INTERACTIONS ---
    function toggleFile(id, row) {{
        const el = document.getElementById(id);
        const btn = row.querySelector('.expand-btn');
        const isHidden = (el.style.display === "none" || el.style.display === "");
        
        el.style.display = isHidden ? "table-row" : "none";
        btn.innerText = isHidden ? "-" : "+";
        btn.style.backgroundColor = isHidden ? "#2c3e50" : "#eee";
        btn.style.color = isHidden ? "white" : "#333";
    }}

    function toggleDisplay(id) {{
        const el = document.getElementById(id);
        el.style.display = (el.style.display === "none" || el.style.display === "") ? "table-row" : "none";
    }}

    function filterTable(tbodyId, inputId) {{
        const filter = document.getElementById(inputId).value.toLowerCase();
        const rows = document.getElementById(tbodyId).querySelectorAll('.parent-row');
        
        rows.forEach(row => {{
            const txt = row.innerText.toLowerCase();
            const childRow = row.nextElementSibling;
            
            if (txt.includes(filter)) {{
                row.style.display = "";
            }} else {{
                row.style.display = "none";
                if(childRow) childRow.style.display = "none";
            }}
        }});
    }}

    function openTab(tabName) {{
        document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
        document.getElementById(tabName).classList.add('active');
        event.currentTarget.classList.add('active');
    }}

    // --- CHARTS ---
    new Chart(document.getElementById('splitChart'), {{
        type: 'doughnut',
        data: {{ labels: ['Keep', 'Discard'], datasets: [{{ data: [{total_keep}, {total_discard}], backgroundColor: ['#2ecc71', '#e74c3c'] }}] }},
        options: {{ maintainAspectRatio: false, plugins: {{ title: {{ display: true, text: 'Test Suite Composition' }} }} }}
    }});

    new Chart(document.getElementById('barChart'), {{
        type: 'bar',
        data: {{ labels: {chart_labels}, datasets: [{{ label: 'Instr. Covered', data: {chart_values}, backgroundColor: '#3498db' }}] }},
        options: {{ maintainAspectRatio: false, indexAxis: 'y', plugins: {{ title: {{ display: true, text: 'Top 10 Contributors' }} }} }}
    }});

    // INIT
    renderTable('keepTableBody', keepData, true);
    renderTable('discardTableBody', discardData, false);

</script>
</body>
</html>
    """
    
    with open(output_path, "w", encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"Success! Dashboard generated at: {os.path.abspath(output_path)}")

def main():
    if len(sys.argv) < 2:
        print("Error: Input CSV path must be provided as an argument.")
        sys.exit(1)
        
    target_csv = sys.argv[1]
    
    # This automatically grabs "jsim" from "jsim_coverage.csv", etc.
    base_name = os.path.basename(target_csv).replace('_coverage.csv', '')

    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, 'output')
    
    if not os.path.exists(output_dir):
        print(f"Error: Output directory not found at {output_dir}")
        sys.exit(1)

    keep_path = os.path.join(output_dir, f"{base_name}_optimized_suite_KEEP.csv")
    discard_path = os.path.join(output_dir, f"{base_name}_optimized_suite_DISCARD.csv")
    html_path = os.path.join(output_dir, f"{base_name}_dashboard.html")
    
    print(f"Reading data for {base_name}...")
    k_data = read_csv_data(keep_path)
    d_data = read_csv_data(discard_path)
    
    if not k_data and not d_data:
        print(f"Error: No optimized data found for {base_name} (KEEP/DISCARD CSVs missing or empty).")
        sys.exit(1)

    print("Generating HTML Report...")
    # Passing base_name directly to generate_html
    generate_html(k_data, d_data, html_path, base_name)

if __name__ == "__main__":
    main()