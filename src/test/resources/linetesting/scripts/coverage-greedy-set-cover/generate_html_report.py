import os
import csv
import json

KEEP_CSV_NAME = "detailed_suite_keep.csv"
DISCARD_CSV_NAME = "detailed_suite_discard.csv"
GOLDEN_CSV_NAME = "class_golden_standard.csv"
HTML_NAME = "coverage_dashboard.html"

def read_csv_detailed(file_path):
    if not os.path.exists(file_path):
        print(f"Warning: {file_path} not found.")
        return []

    grouped_data = {}

    with open(file_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            test_id = row['TEST_ID']
            pkg = row['PACKAGE']
            cls = row['CLASS']
            full_class_name = f"{pkg}.{cls}"
            
            if test_id not in grouped_data:
                grouped_data[test_id] = {
                    'name': test_id,
                    'justification': row.get('DECISION_JUSTIFICATION', 'N/A'),
                    'total_instructions': 0,
                    'classes_touched': 0,
                    'details': []
                }
            
            try:
                i_cov = int(row['INSTRUCTION_COVERED'])
                i_miss = int(row['INSTRUCTION_MISSED'])
                b_cov = int(row['BRANCH_COVERED'])
                b_miss = int(row['BRANCH_MISSED'])
                l_cov = int(row['LINE_COVERED'])
                l_miss = int(row['LINE_MISSED'])
                c_cov = int(row['COMPLEXITY_COVERED'])
                c_miss = int(row['COMPLEXITY_MISSED'])
                m_cov = int(row['METHOD_COVERED'])
                m_miss = int(row['METHOD_MISSED'])
            except ValueError:
                continue

            grouped_data[test_id]['classes_touched'] += 1
            grouped_data[test_id]['total_instructions'] += i_cov

            grouped_data[test_id]['details'].append({
                'class_name': full_class_name,
                'inst_cov': i_cov, 'inst_miss': i_miss,
                'br_cov': b_cov,   'br_miss': b_miss,
                'ln_cov': l_cov,   'ln_miss': l_miss,
                'cx_cov': c_cov,   'cx_miss': c_miss,
                'mt_cov': m_cov,   'mt_miss': m_miss
            })

    result_list = list(grouped_data.values())
    result_list.sort(key=lambda x: x['total_instructions'], reverse=True)
    return result_list

def read_golden_standard(file_path):
    if not os.path.exists(file_path):
        print(f"Warning: {file_path} not found.")
        return []

    golden_data = []
    with open(file_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                row['INSTRUCTION_COVERED'] = int(row['INSTRUCTION_COVERED'])
                row['INSTRUCTION_MISSED'] = int(row['INSTRUCTION_MISSED'])
                golden_data.append(row)
            except ValueError:
                continue
    
    golden_data.sort(key=lambda x: x['CLASS_FULL_NAME'])
    return golden_data

def generate_html(keep_data, discard_data, golden_data, output_path):
    total_keep = len(keep_data)
    total_discard = len(discard_data)
    total_files = total_keep + total_discard
    reduction_pct = (total_discard / total_files * 100) if total_files > 0 else 0
    
    json_keep = json.dumps(keep_data)
    json_discard = json.dumps(discard_data)
    json_golden = json.dumps(golden_data)

    html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>JMT Detailed Coverage Report</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        :root {{ --primary: #2c3e50; --success: #27ae60; --danger: #e74c3c; --bg: #f4f4f9; }}
        body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: var(--bg); margin: 0; padding: 20px; color: #333; }}
        .container {{ max-width: 1400px; margin: 0 auto; }}
        
        .stats-grid {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 30px; }}
        .card {{ background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); text-align: center; }}
        .card h2 {{ margin: 0; font-size: 2.5em; color: #3498db; }}
        
        .charts-row {{ display: flex; gap: 20px; margin-bottom: 30px; height: 350px; }}
        .chart-box {{ flex: 1; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }}

        .table-container {{ background: white; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); overflow: hidden; margin-top: 20px; }}
        
        table {{ width: 100%; border-collapse: collapse; }} 
        th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; word-wrap: break-word; vertical-align: middle; }}
        th {{ background-color: var(--primary); color: white; }}
        
        /* Tabs */
        .tab-nav {{ display: flex; margin-bottom: 20px; border-bottom: 2px solid #ddd; }}
        .tab-btn {{ padding: 10px 20px; cursor: pointer; border: none; background: none; font-size: 1.1em; color: #777; font-weight: 600; }}
        .tab-btn.active {{ color: var(--primary); border-bottom: 3px solid var(--primary); }}
        .tab-content {{ display: none; }}
        .tab-content.active {{ display: block; }}

        tr.parent-row {{ cursor: pointer; transition: background 0.1s; }}
        tr.parent-row:hover {{ background-color: #f1f1f1; }}
        tr.parent-row td {{ font-weight: 500; }}
        .expand-btn {{ font-weight: bold; margin-right: 10px; display: inline-block; width: 24px; height: 24px; line-height: 24px; text-align: center; background: #eee; border-radius: 50%; }}

        tr.child-row {{ background-color: #f9f9f9; display: none; }}
        tr.child-row td {{ padding: 0; }}
        
        .detail-table {{ margin: 15px; width: calc(100% - 30px); border: 1px solid #ddd; font-size: 0.9em; background: white; }}
        .detail-table th {{ background-color: #ecf0f1; color: #2c3e50; font-weight: 600; padding: 8px; }}
        .detail-table td {{ padding: 6px 8px; border-bottom: 1px solid #ddd; }}
        
        .highlight-row {{ background-color: #fffacd !important; }} 
        .cov-val {{ color: var(--success); font-weight: bold; }}
        .miss-val {{ color: #e67e22; font-size: 0.9em; }}
        .zero-val {{ color: #bdc3c7; }}
        .badge {{ padding: 4px 8px; border-radius: 4px; font-size: 0.85em; font-weight: bold; }}
        .badge-keep {{ background-color: #d5f5e3; color: var(--success); }}
        .badge-discard {{ background-color: #fadbd8; color: var(--danger); }}
        .search-box {{ float: right; padding: 8px; border: 1px solid #ddd; border-radius: 4px; width: 250px; }}
        h2.section-header {{ margin-top: 10px; border-bottom: 2px solid #ddd; padding-bottom: 10px; display: flex; justify-content: space-between; align-items: center; }}

        /* SPINNER */
        .spinner-overlay {{
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(255, 255, 255, 0.7);
            z-index: 9999;
            display: none; /* Hidden by default */
            justify-content: center; align-items: center; flex-direction: column;
        }}
        .spinner {{
            border: 8px solid #f3f3f3;
            border-top: 8px solid #3498db;
            border-radius: 50%;
            width: 60px; height: 60px;
            animation: spin 1s linear infinite;
        }}
        .loading-text {{ margin-top: 15px; font-weight: bold; color: #2c3e50; font-size: 1.2em; }}
        @keyframes spin {{ 0% {{ transform: rotate(0deg); }} 100% {{ transform: rotate(360deg); }} }}
    </style>
</head>
<body>

<div id="loadingSpinner" class="spinner-overlay">
    <div class="spinner"></div>
    <div class="loading-text">Searching...</div>
</div>

<div class="container">
    <h1 style="text-align:center;">Detailed Coverage Optimization Report</h1>
    
    <div class="stats-grid">
        <div class="card"><h2>{total_files}</h2><p>Total Files</p></div>
        <div class="card"><h2>{total_keep}</h2><p>Kept</p></div>
        <div class="card"><h2>{total_discard}</h2><p>Discarded</p></div>
        <div class="card"><h2>{reduction_pct:.1f}%</h2><p>Reduction</p></div>
    </div>

    <div class="charts-row">
        <div class="chart-box"><canvas id="splitChart"></canvas></div>
        <div class="chart-box"><canvas id="coverageChart"></canvas></div>
    </div>

    <div class="tab-nav">
        <button class="tab-btn active" onclick="openTab('tabFiles')">File Analysis</button>
        <button class="tab-btn" onclick="openTab('tabGolden')">Golden Standard (Per Class)</button>
    </div>

    <div id="tabFiles" class="tab-content active">
        <h2 class="section-header">
            Selected Test Suite (Keep)
            <input type="text" id="keepSearch" class="search-box" placeholder="Search file or class..." onkeyup="triggerSearch('keepTableBody', 'keepSearch', false)">
        </h2>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th style="width: 50px"></th>
                        <th style="width: 30%">Test File</th>
                        <th style="width: 30%">Role / Justification</th>
                        <th style="width: 15%">Classes</th>
                        <th style="width: 15%">Instr. Covered</th>
                    </tr>
                </thead>
                <tbody id="keepTableBody"></tbody>
            </table>
        </div>

        <h2 class="section-header">
            Redundant Files (Discard)
            <input type="text" id="discardSearch" class="search-box" placeholder="Search file or class..." onkeyup="triggerSearch('discardTableBody', 'discardSearch', false)">
        </h2>
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

    <div id="tabGolden" class="tab-content">
        <h2 class="section-header">
            Golden Standard Coverage Per Class
            <input type="text" id="goldenSearch" class="search-box" placeholder="Search class name..." onkeyup="triggerSearch('goldenTableBody', 'goldenSearch', true)">
        </h2>
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th style="width: 35%">Class Full Name</th>
                        <th style="width: 25%">Best Test File</th>
                        <th>Instructions (Cov/Miss)</th>
                        <th>Branches (Cov/Miss)</th>
                        <th>Lines (Cov/Miss)</th>
                    </tr>
                </thead>
                <tbody id="goldenTableBody"></tbody>
            </table>
        </div>
    </div>

</div>

<script>
    const keepData = {json_keep};
    const discardData = {json_discard};
    const goldenData = {json_golden};

    // --- RENDER FUNCTIONS ---
    function renderTable(tableBodyId, data, isKeep) {{
        const tbody = document.getElementById(tableBodyId);
        let html = "";
        
        data.forEach((file, index) => {{
            const uniqueId = tableBodyId + '_' + index;
            
            let childRowsHtml = "";
            file.details.forEach(det => {{
                childRowsHtml += `
                <tr class="inner-row" data-class-name="${{det.class_name.toLowerCase()}}">
                    <td title="${{det.class_name}}" style="word-break: break-all;">${{det.class_name}}</td>
                    <td><span class="cov-val">${{det.inst_cov}}</span> / <span class="${{det.inst_miss > 0 ? 'miss-val' : 'zero-val'}}">${{det.inst_miss}}</span></td>
                    <td><span class="cov-val">${{det.br_cov}}</span> / <span class="${{det.br_miss > 0 ? 'miss-val' : 'zero-val'}}">${{det.br_miss}}</span></td>
                    <td><span class="cov-val">${{det.ln_cov}}</span> / <span class="${{det.ln_miss > 0 ? 'miss-val' : 'zero-val'}}">${{det.ln_miss}}</span></td>
                    <td><span class="cov-val">${{det.cx_cov}}</span> / <span class="${{det.cx_miss > 0 ? 'miss-val' : 'zero-val'}}">${{det.cx_miss}}</span></td>
                    <td><span class="cov-val">${{det.mt_cov}}</span> / <span class="${{det.mt_miss > 0 ? 'miss-val' : 'zero-val'}}">${{det.mt_miss}}</span></td>
                </tr>`;
            }});

            html += `
            <tr class="parent-row" id="parent_${{uniqueId}}" data-file-name="${{file.name.toLowerCase()}}" onclick="toggleDetails('${{uniqueId}}', this)">
                <td style="text-align:center;"><span class="expand-btn">+</span></td>
                <td style="word-break: break-all;"><b>${{file.name}}</b></td>
                <td><span class="badge ${{isKeep ? 'badge-keep' : 'badge-discard'}}">${{file.justification}}</span></td>
                <td>${{file.classes_touched}}</td>
                <td>${{file.total_instructions.toLocaleString()}}</td>
            </tr>
            <tr id="${{uniqueId}}" class="child-row">
                <td colspan="5">
                    <table class="detail-table">
                        <thead>
                            <tr>
                                <th style="width: 30%">Class Name</th>
                                <th>Instructions</th>
                                <th>Branches</th>
                                <th>Lines</th>
                                <th>Complexity</th>
                                <th>Methods</th>
                            </tr>
                        </thead>
                        <tbody>${{childRowsHtml}}</tbody>
                    </table>
                </td>
            </tr>`;
        }});
        tbody.innerHTML = html;
    }}

    function renderGoldenTable(tableBodyId, data) {{
        const tbody = document.getElementById(tableBodyId);
        let html = "";
        data.forEach(item => {{
            html += `
            <tr>
                <td style="word-break: break-all;"><b>${{item.CLASS_FULL_NAME}}</b></td>
                <td style="word-break: break-all;">${{item.GOLDEN_TEST_FILE}}</td>
                <td><span class="cov-val">${{item.INSTRUCTION_COVERED}}</span> / <span class="${{item.INSTRUCTION_MISSED > 0 ? 'miss-val' : 'zero-val'}}">${{item.INSTRUCTION_MISSED}}</span></td>
                <td><span class="cov-val">${{item.BRANCH_COVERED}}</span> / <span class="${{item.BRANCH_MISSED > 0 ? 'miss-val' : 'zero-val'}}">${{item.BRANCH_MISSED}}</span></td>
                <td><span class="cov-val">${{item.LINE_COVERED}}</span> / <span class="${{item.LINE_MISSED > 0 ? 'miss-val' : 'zero-val'}}">${{item.LINE_MISSED}}</span></td>
            </tr>`;
        }});
        tbody.innerHTML = html;
    }}

    // --- UI LOGIC ---
    function toggleDetails(rowId, btnElement) {{
        const icon = btnElement.querySelector('.expand-btn');
        const row = document.getElementById(rowId);
        
        if (row.style.display !== "table-row") {{
            row.style.display = "table-row";
            icon.innerText = "-";
            icon.style.backgroundColor = "#27ae60";
            icon.style.color = "white";
        }} else {{
            row.style.display = "none";
            icon.innerText = "+";
            icon.style.backgroundColor = "#eee";
            icon.style.color = "#333";
        }}
    }}

    function expandRow(rowId) {{
        const row = document.getElementById(rowId);
        const parentId = "parent_" + rowId;
        const parentRow = document.getElementById(parentId);
        
        if (row.style.display !== "table-row") {{
            row.style.display = "table-row";
            const icon = parentRow.querySelector('.expand-btn');
            icon.innerText = "-";
            icon.style.backgroundColor = "#27ae60";
            icon.style.color = "white";
        }}
    }}

    function collapseRow(rowId) {{
        const row = document.getElementById(rowId);
        const parentId = "parent_" + rowId;
        const parentRow = document.getElementById(parentId);
        
        if (row.style.display === "table-row") {{
            row.style.display = "none";
            const icon = parentRow.querySelector('.expand-btn');
            icon.innerText = "+";
            icon.style.backgroundColor = "#eee";
            icon.style.color = "#333";
        }}
    }}

    // --- ASYNC SEARCH TRIGGER ---
    let searchTimeout;
    function triggerSearch(tbodyId, inputId, isGolden) {{
        // 1. Show Spinner
        document.getElementById('loadingSpinner').style.display = 'flex';

        // 2. Debounce & Async Execution
        clearTimeout(searchTimeout);
        
        // We use a small timeout to allow the UI thread to paint the spinner
        // before the heavy filtering loop starts.
        searchTimeout = setTimeout(() => {{
            if (isGolden) {{
                filterGoldenTable(tbodyId, inputId);
            }} else {{
                filterTable(tbodyId, inputId);
            }}
            
            // 3. Hide Spinner immediately after work is done
            document.getElementById('loadingSpinner').style.display = 'none';
        }}, 50); 
    }}

    function filterTable(tbodyId, inputId) {{
        const filter = document.getElementById(inputId).value.toLowerCase().trim();
        const tbody = document.getElementById(tbodyId);
        const rows = tbody.children; 
        
        for (let i = 0; i < rows.length; i += 2) {{
            const parentRow = rows[i];
            const childRow = rows[i+1];
            const uniqueId = childRow.id;
            const fileName = parentRow.getAttribute('data-file-name') || "";
            
            const innerRows = childRow.querySelectorAll('.inner-row');
            let hasClassMatch = false;

            innerRows.forEach(inner => {{
                const className = inner.getAttribute('data-class-name');
                if (filter) {{
                    if (className.includes(filter)) {{
                        inner.style.display = ""; 
                        inner.classList.add('highlight-row');
                        hasClassMatch = true;
                    }} else {{
                        inner.style.display = "none"; 
                        inner.classList.remove('highlight-row');
                    }}
                }} else {{
                    inner.style.display = ""; 
                    inner.classList.remove('highlight-row');
                }}
            }});
            
            if (!filter) {{
                parentRow.style.display = "";
                collapseRow(uniqueId);
            }} 
            else if (hasClassMatch) {{
                parentRow.style.display = "";
                expandRow(uniqueId);
            }} 
            else if (fileName.includes(filter)) {{
                // Reset inner rows to visible if file name matches
                innerRows.forEach(inner => inner.style.display = "");
                parentRow.style.display = "";
                collapseRow(uniqueId);
            }} 
            else {{
                parentRow.style.display = "none";
                collapseRow(uniqueId); // Ensure closed if hidden
            }}
        }}
    }}
    
    function filterGoldenTable(tbodyId, inputId) {{
        const filter = document.getElementById(inputId).value.toLowerCase();
        const rows = document.getElementById(tbodyId).rows;
        for (let i = 0; i < rows.length; i++) {{
            const txt = rows[i].innerText.toLowerCase();
            rows[i].style.display = txt.includes(filter) ? "" : "none";
        }}
    }}

    function openTab(tabName) {{
        document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
        document.getElementById(tabName).classList.add('active');
        event.currentTarget.classList.add('active');
    }}

    // --- INIT ---
    renderTable('keepTableBody', keepData, true);
    renderTable('discardTableBody', discardData, false);
    renderGoldenTable('goldenTableBody', goldenData);

    new Chart(document.getElementById('splitChart'), {{
        type: 'doughnut',
        data: {{ labels: ['Keep', 'Discard'], datasets: [{{ data: [{total_keep}, {total_discard}], backgroundColor: ['#2ecc71', '#e74c3c'] }}] }},
        options: {{ maintainAspectRatio: false, plugins: {{ title: {{ display: true, text: 'Test Suite Composition' }} }} }}
    }});
    
    const topFiles = keepData.slice(0, 10);
    new Chart(document.getElementById('coverageChart'), {{
        type: 'bar',
        data: {{ labels: topFiles.map(d => d.name.length > 20 ? d.name.substring(0, 20) + '...' : d.name), datasets: [{{ label: 'Instr. Covered', data: topFiles.map(d => d.total_instructions), backgroundColor: '#3498db' }}] }},
        options: {{ maintainAspectRatio: false, plugins: {{ title: {{ display: true, text: 'Top 10 Contributors' }} }} }}
    }});

</script>
</body>
</html>
    """
    
    with open(output_path, "w") as f:
        f.write(html_content)
    
    print(f"Success! Detailed Dashboard generated at: {os.path.abspath(output_path)}")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(script_dir, 'output')
    
    if not os.path.exists(output_dir):
        print(f"Error: Output directory not found at {output_dir}")
        return

    keep_path = os.path.join(output_dir, KEEP_CSV_NAME)
    discard_path = os.path.join(output_dir, DISCARD_CSV_NAME)
    golden_path = os.path.join(output_dir, GOLDEN_CSV_NAME)
    html_path = os.path.join(output_dir, HTML_NAME)
    
    print("Reading detailed CSV data...")
    keep_data = read_csv_detailed(keep_path)
    discard_data = read_csv_detailed(discard_path)
    golden_data = read_golden_standard(golden_path)
    
    if not keep_data and not discard_data:
        print("Error: No data found in CSV files.")
        return

    print("Generating HTML Report...")
    generate_html(keep_data, discard_data, golden_data, html_path)

if __name__ == "__main__":
    main()