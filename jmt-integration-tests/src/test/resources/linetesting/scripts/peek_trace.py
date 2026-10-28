#!/usr/bin/env python3
"""
Quick Trace Viewer for JMT .ndjson.gz and .ndjson trace files.

Usage:
    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py <folder-or-file-path> [num_events]

Examples:
    # Inspect latest trace file in a directory:
    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces

    # Inspect with 20 events:
    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces 20

    # Inspect a specific trace file:
    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces/my_model_run1_part1.ndjson.gz 15
"""

import gzip
import json
import os
import sys
import glob

def open_trace_file(file_path):
    if file_path.endswith(".gz"):
        return gzip.open(file_path, "rt", encoding="utf-8")
    return open(file_path, "r", encoding="utf-8")

def peek(file_path, num_events=5):
    if not os.path.exists(file_path):
        print(f"Error: File not found: {file_path}")
        sys.exit(1)

    file_size_mb = os.path.getsize(file_path) / (1024 * 1024)
    is_compressed = file_path.endswith(".gz")
    compression_label = "compressed" if is_compressed else "uncompressed"

    print("=" * 80)
    print(f"Viewing: {os.path.basename(file_path)} ({file_size_mb:.2f} MB {compression_label})")
    print(f"Path   : {os.path.abspath(file_path)}")
    print("=" * 80)

    with open_trace_file(file_path) as f:
        # Line 1: Metadata
        meta_line = f.readline().strip()
        if not meta_line:
            print("File is empty.")
            return

        try:
            meta = json.loads(meta_line)
            print("\n[LINE 1: NETWORK TOPOLOGY METADATA]")
            print(f"  Model Name : {meta.get('modelName')}")
            print(f"  Timestamp  : {meta.get('timestamp')}")
            nodes = [n.get("name", "") for n in meta.get("nodes", [])]
            print(f"  Stations ({len(nodes)}): {nodes}")
            classes = [c.get("name", "") for c in meta.get("classes", [])]
            print(f"  Job Classes ({len(classes)}): {classes}")
            connections = meta.get("connections", [])
            print(f"  Connections ({len(connections)} links):")
            for conn in connections[:6]:
                print(f"    - {conn.get('source')} -> {conn.get('target')}")
            if len(connections) > 6:
                print(f"    ... and {len(connections) - 6} more links")
        except Exception:
            print("  Raw Line 1:", meta_line[:140], "...")

        print(f"\n[NEXT {num_events} EVENTS]")
        events_shown = 0
        while events_shown < num_events:
            line = f.readline()
            if not line:
                break
            line = line.strip()
            if not line:
                continue

            try:
                ev = json.loads(line)
                record_type = ev.get("type", "event")

                if record_type == "metadata":
                    continue

                if record_type == "footer":
                    print("\n--- Summary Footer ---")
                    print(f"  Total Events  : {ev.get('totalEvents')}")
                    print(f"  Dropped Events: {ev.get('droppedEvents')}")
                    print(f"  Last Sim Time : {ev.get('lastSimTime')}")
                    print(f"  Duration (ms) : {ev.get('durationMs')}")
                    break

                events_shown += 1
                seq = ev.get("seq", "?")
                t = ev.get("t", 0.0)
                phase = ev.get("phase", "")
                event_name = ev.get("event", "UNKNOWN")
                event_id = ev.get("eventId", "")

                print(f"\n--- Event #{events_shown} (Seq: {seq}, SimTime: {t:.4f}, Phase: {phase}) ---")
                print(f"  Event   : {event_name} (ID: {event_id})")
                print(f"  Route   : {ev.get('srcDisplay')} -> {ev.get('dstDisplay')}")
                print(f"  Caller  : {ev.get('caller')}")

                if ev.get("jobId", -1) >= 0:
                    job_info = f"ID: {ev.get('jobId')}, Class: {ev.get('jobClass')} (Priority: {ev.get('priority')})"
                    if ev.get("enterTime", 0.0) > 0:
                        job_info += f", Entered: {ev.get('enterTime'):.4f}"
                    print(f"  Job     : {job_info}")

                state = []
                if "nodeJobs" in ev: state.append(f"nodeJobs={ev['nodeJobs']}")
                if "queueSize" in ev: state.append(f"queueSize={ev['queueSize']}")
                if "queueCap" in ev: state.append(f"queueCap={ev['queueCap']}")
                if "servers" in ev: state.append(f"servers={ev['servers']}")
                if state:
                    print(f"  State   : {', '.join(state)}")

                if "msg" in ev:
                    print(f"  Message : {ev.get('msg')}")
            except Exception:
                print(f"  Raw Line: {line[:120]}...")

def print_usage_and_exit():
    print("Error: Missing required folder or file path argument.")
    print("\nUsage:")
    print("    python peek_trace.py <folder-or-file-path> [num_events]")
    print("\nExamples:")
    print("    # Inspect the latest trace in traces/ folder:")
    print("    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces")
    print("\n    # Inspect 20 events from a specific folder:")
    print("    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py C:/Users/calog/traces 20")
    print("\n    # Inspect a specific compressed trace file:")
    print("    python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces/my_trace_run1_part1.ndjson.gz 15")
    sys.exit(1)

def main():
    if len(sys.argv) < 2:
        print_usage_and_exit()

    path_arg = sys.argv[1].strip()
    if path_arg in ("-h", "--help", "/?"):
        print_usage_and_exit()

    num_events = 5
    if len(sys.argv) > 2:
        try:
            num_events = int(sys.argv[2])
        except ValueError:
            print(f"Warning: Invalid event count '{sys.argv[2]}', defaulting to 5.")

    target_file = None

    if os.path.isdir(path_arg):
        # Look for *.ndjson.gz first, then *.ndjson
        pattern_gz = os.path.join(path_arg, "*.ndjson.gz")
        pattern_plain = os.path.join(path_arg, "*.ndjson")
        found = glob.glob(pattern_gz) + glob.glob(pattern_plain)

        if not found:
            print(f"Error: No trace files (.ndjson.gz or .ndjson) found in folder: {path_arg}")
            sys.exit(1)

        # Pick the most recently modified file
        target_file = sorted(found, key=os.path.getmtime, reverse=True)[0]
        if len(found) > 1:
            print(f"Found {len(found)} trace file(s) in '{path_arg}'. Showing the most recent:")
            print(f"-> {os.path.basename(target_file)}\n")
    elif os.path.isfile(path_arg):
        target_file = path_arg
    else:
        print(f"Error: Specified path does not exist: {path_arg}")
        sys.exit(1)

    peek(target_file, num_events)

if __name__ == "__main__":
    main()
