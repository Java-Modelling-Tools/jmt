# JMT Visual Debug Tracer & Event Logging System

## 1. Overview & Architecture

The **Visual Debug Tracer** (`SimEventTracer`) is a native, high-performance event-logging engine embedded directly inside the JMT discrete-event simulation kernel (`jmt-engine`). It is specifically designed to power interactive, packet-level visual debuggers, timeline step-throughs, and network animation tools.

### Architectural Highlights

```
+------------------------------------------------------------------------------------+
|                                Simulation Hot Path                                 |
|                                                                                    |
|  [NetNode.send()] ----+                                                            |
|                       |                                                            |
|  [NetNode.dispatch()]-+--> [Hot-Path Guard 1: DEBUG_VISUAL_TRACE == false ? return]|
|                       |    [Hot-Path Guard 2: isVisualLifecycleEvent() == false ?] |
|  [Queue.process()] ---+            |                                               |
|                                    v (packet lifecycle event only)                 |
|                                                                                    |
|               [Push to SPSC Ring Buffer: 262,144 pre-allocated slots]              |
|               (Bitmask indexing, atomic write sequence, ZERO allocation/GC)        |
+------------------------------------------------------------------------------------+
                                     |
                                     | LockSupport / Async polling
                                     v
+------------------------------------------------------------------------------------+
|                            Background Writer Daemon                                |
|                                                                                    |
|  [WriterThread] ---> Drains SPSC Ring Buffer in 2,048-event batches                |
|                 ---> Serializes to Newline-Delimited JSON (NDJSON)                 |
|                 ---> Mandatory on-the-fly GZIP compression (GZIPOutputStream)      |
|                 ---> Automatic Rolling File Chunks (_part1.ndjson.gz, _part2...)   |
|                 ---> Buffered disk write (64 KB output buffer)                     |
+------------------------------------------------------------------------------------+
```

1. **Zero-Overhead Hot Path when Disabled**:
   - Guarded by a volatile boolean flag `SimEventTracer.DEBUG_VISUAL_TRACE`.
   - When set to `false` (default), the check is an inlined volatile read costing **~0–1 nanosecond**.
   - No object allocation, no stack trace walking, and no lock contention occur.

2. **Event Capture Filtering (High-Frequency Internal Tick Filter)**:
   - Simulations execute millions of internal scheduling ticks that provide no value to a visual canvas (e.g. `EVENT_TIMING`, `EVENT_KEEP_AWAKE`, `EVENT_CHECK_DELAY_OFF`, `EVENT_RESET_COOLSTART`, `EVENT_QUEUE_IDLE`, `EVENT_QUEUE_IDLE_ACK`).
   - The tracer filters these out **immediately on the hot path** before entering the ring buffer.
   - Only structural packet/job lifecycle events (`EVENT_JOB`, `EVENT_ACK`, `EVENT_PREEMPTED_JOB`, `DROP`, `EVENT_RENEGE`, `EVENT_JOIN`, etc.) are recorded.
   - This alone reduces trace volume by **50% to 75%** with zero GC overhead.

3. **Zero-Allocation SPSC Ring Buffer**:
   - Implements a Single-Producer Single-Consumer (SPSC) lock-free ring buffer with **262,144 pre-allocated slots** (`SimEventRecord`).
   - Slot indexing uses bitwise AND masking (`writeIndex & 0x3FFFF`), completely eliminating modulo division and object instantiations on the simulation thread.
   - Prevents Garbage Collector (GC) pressure and avoids distorting discrete-event simulation timings.

4. **Mandatory Streaming GZIP Compression (`.ndjson.gz`)**:
   - All trace files are compressed on-the-fly using `GZIPOutputStream`.
   - Because JSON is highly repetitive text (field names repeat on every line), GZIP achieves an **85% to 92% compression ratio**.
   - Compression runs entirely on the background `WriterThread` with a 64 KB buffer, introducing **zero slowdown** to simulation execution.

5. **Automatic Rolling File Chunks (`_part1`, `_part2`...)**:
   - Instead of producing one gigantic multi-gigabyte monolithic file, the tracer automatically splits runs into manageable chunk files.
   - Defaults to a reasonable PC-reader lowerbound (**500 GB uncompressed JSON text** / **850,000,000 events** per chunk).
   - Can be overridden via `-Djmt.trace.chunk.mb=...` or `-Djmt.trace.chunk.events=...`.
   - **Crucial**: Every chunk file repeats the Line 1 `metadata` header so each chunk is self-contained and independently readable by the visual debugger tool.

6. **Engine Lifecycle & Interception Hooks**:
   - `QueueNet.NetSystem.start()`: Writes the Line 1 network topology metadata header.
   - `QueueNet.NetNode.send()`: Intercepts all scheduled event sends (`SEND` phase).
   - `QueueNet.NetNode.dispatch()`: Intercepts all event deliveries at destination stations (`DISPATCH` phase).
   - `QueueNet.NetNode` unhandled messages: Intercepts routing failures and unhandled messages (`NOT_PROCESSED` phase).
   - `NodeSections.Queue.process()`: Intercepts packet/job drops when queue capacity is reached (`DROP` phase).
   - `QueueNet.NetSystem.run()` (`finally` block) / `terminate()`: Flushes remaining events, appends the footer summary, closes the file, and resets state.

---

## 2. Output File Specification (`.ndjson.gz`)

The tracer writes output in **Newline-Delimited JSON compressed with GZIP** (`.ndjson.gz`). Each line inside the decompressed stream is a self-contained, valid JSON object ending with `\n`.

### File Structure Summary

| Line Number | Record Type | Purpose |
| :--- | :--- | :--- |
| **Line 1** | `metadata` | Complete network canvas topology (stations, capacities, servers, job classes, connections) required to initialize and draw the visual debugger canvas. **Repeated at Line 1 of every chunk file**. |
| **Lines 2 .. N** | `event` | Chronological stream of simulation events with full packet/station state and source code locations. |
| **Line N+1** | `footer` | Simulation run completion summary (total events, duration, dropped count, last sim time, chunk part number). |

---

### Line 1: Network Topology Metadata (`"type": "metadata"`)

Emitted at the start of each simulation run and repeated at the start of each rolled chunk file. The visual debugger uses this header to layout nodes, configure queues, and draw connection wires before playback starts.

#### Example Line 1
```json
{
  "type": "metadata",
  "version": "1.0",
  "timestamp": 1730123456789,
  "modelName": "WebArchModel",
  "classes": [
    {"id": 0, "name": "HTTPRequests", "type": "OPEN", "priority": 1, "referenceNode": "SourceStation"},
    {"id": 1, "name": "BatchJobs", "type": "CLOSED", "priority": 0, "referenceNode": "QueueStation"}
  ],
  "nodes": [
    {
      "id": 1,
      "name": "SourceStation",
      "nodeType": "Source",
      "sections": {
        "input": {"type": "RandomSource"},
        "service": {"type": "NONE"},
        "output": {"type": "Router"}
      }
    },
    {
      "id": 2,
      "name": "AppServerQueue",
      "nodeType": "Queue",
      "sections": {
        "input": {"type": "Queue", "capacity": 50},
        "service": {"type": "Server", "servers": 4},
        "output": {"type": "Router"}
      }
    }
  ],
  "connections": [
    {"source": "SourceStation", "target": "AppServerQueue"}
  ]
}
```

#### Field-by-Field Dictionary: Metadata Record

| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | String | Fixed identifier: `"metadata"`. |
| `version` | String | Schema version: `"1.0"`. |
| `timestamp` | Long | Wall-clock UNIX epoch millisecond when the simulation started. |
| `modelName` | String | Name of the simulated queue network model. |
| `classes` | Array | List of all job classes defined in the network. |
| `classes[].id` | Integer | Unique identifier of the job class. |
| `classes[].name` | String | Human-readable name of the class (e.g., `"Class1"`, `"GoldUsers"`). |
| `classes[].type` | String | Workload type: `"OPEN"` (external arrivals) or `"CLOSED"` (fixed population). |
| `classes[].priority` | Integer | Scheduling priority (higher integer = higher priority). |
| `classes[].referenceNode`| String | Station name acting as reference/source for this class. |
| `nodes` | Array | All stations (nodes) present in the network canvas. |
| `nodes[].id` | Integer | Unique integer identifier of the station. |
| `nodes[].name` | String | Actual station name configured in JMT (e.g. `"Web-Server"`, `"DB-Queue"`). |
| `nodes[].nodeType` | String | Canvas categorization: `"Source"`, `"Queue"`, `"Server"`, `"Delay"`, `"Fork"`, `"Join"`, `"Sink"`, or `"Station"`. |
| `nodes[].sections` | Object | The three internal sections composing any JMT NetNode: `input`, `service`, `output`. |
| `nodes[].sections.input` | Object | Input section description (`type`: simple Java class name, `capacity`: station capacity or `-1` if infinite). |
| `nodes[].sections.service` | Object | Service section description (`type`: simple Java class name, `servers`: parallel server count). |
| `nodes[].sections.output` | Object | Output section description (`type`: simple Java class name, e.g. `"Router"`, `"Fork"`). |
| `connections` | Array | Directed topology links (wires) between stations. |
| `connections[].source` | String | Name of the source station. |
| `connections[].target` | String | Name of the destination station. |

---

### Lines 2 .. N: Event Records (`"type": "event"`)

Emitted for structural packet lifecycle transitions.

#### Example Line (Send Job)
```json
{
  "type": "event",
  "seq": 104,
  "t": 1.2504,
  "phase": "SEND",
  "event": "EVENT_JOB",
  "eventId": 3,
  "delay": 0.05,
  "schedTime": 1.3004,
  "srcStation": "ClientSource",
  "srcStationId": 1,
  "srcSection": "OUTPUT",
  "srcClass": "Router",
  "srcDisplay": "ClientSource [OUTPUT:Router]",
  "dstStation": "WebServerStation",
  "dstStationId": 2,
  "dstSection": "INPUT",
  "dstClass": "Queue",
  "dstDisplay": "WebServerStation [INPUT:Queue]",
  "src": "ClientSource",
  "dst": "WebServerStation",
  "caller": "NodeSections.Router.process(Router.java:142)",
  "nodeJobs": 5,
  "queueSize": 2,
  "queueCap": 50,
  "servers": 4,
  "jobId": 42,
  "jobClass": "HTTPRequests",
  "classId": 0,
  "priority": 1,
  "enterTime": 1.2001,
  "serviceTime": 0.0503,
  "queueTime": 0.0,
  "lastVisited": "ClientSource"
}
```

#### Example Line (Job Drop on Buffer Full)
```json
{
  "type": "event",
  "seq": 255,
  "t": 5.8102,
  "phase": "DROP",
  "event": "EVENT_JOB",
  "eventId": 3,
  "srcStation": "WebServerStation",
  "srcStationId": 2,
  "srcSection": "INPUT",
  "srcClass": "Queue",
  "srcDisplay": "WebServerStation [INPUT:Queue]",
  "dstStation": "WebServerStation",
  "dstStationId": 2,
  "dstSection": "NO_ADDRESS",
  "dstClass": "None",
  "dstDisplay": "WebServerStation [NO_ADDRESS:None]",
  "src": "WebServerStation",
  "dst": "WebServerStation",
  "caller": "NodeSections.Queue.process(Queue.java:558)",
  "queueSize": 50,
  "queueCap": 50,
  "jobId": 89,
  "jobClass": "HTTPRequests",
  "classId": 0,
  "msg": "Job dropped: queue capacity reached"
}
```

#### Field-by-Field Dictionary: Event Record

#### 1. Core Event Identifiers
| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | String | Fixed identifier: `"event"`. |
| `seq` | Long | Globally monotonic sequence number (1, 2, 3...) across the run. |
| `t` | Double | Current simulation clock time (`simTime`). |
| `phase` | String | Event lifecycle phase: `"SEND"` (scheduled), `"DISPATCH"` (executed/received), `"DROP"` (dropped job), `"NOT_PROCESSED"` (unrouted/unhandled). |
| `event` | String | Event name as defined in `NetEvent` (e.g. `"EVENT_JOB"`, `"EVENT_ACK"`, `"EVENT_PREEMPTED_JOB"`, `"EVENT_STOP"`, etc.). |
| `eventId` | Integer | Raw integer event code matching `NetEvent` constants (`0x0000` to `0x001D`). |
| `delay` | Double | *(Present if delay > 0)* Scheduled simulation delay before delivery. |
| `schedTime`| Double | *(Present if delay > 0)* Calculated delivery timestamp (`t + delay`). |

#### 2. Station & Section Identification
| Field | Type | Description |
| :--- | :--- | :--- |
| `srcStation` | String | **Actual network station name** of sender (e.g. `"ClientSource"`, `"WebServer"`, or `"SYSTEM"`). |
| `srcStationId` | Integer | Sender station integer ID (`-1` for system events). |
| `srcSection` | String | Sender station internal section: `"INPUT"`, `"SERVICE"`, `"OUTPUT"`, or `"NO_ADDRESS"`. |
| `srcClass` | String | Java class of sender section (e.g. `"Queue"`, `"Server"`, `"Delay"`, `"Router"`, `"RandomSource"`). |
| `srcDisplay` | String | Pre-formatted string: `StationName [Section:Class]` for immediate UI display. |
| `dstStation` | String | **Actual network station name** of receiver (e.g. `"WebServer"`, or `"SYSTEM"`). |
| `dstStationId` | Integer | Destination station integer ID (`-1` for system events). |
| `dstSection` | String | Receiver station internal section: `"INPUT"`, `"SERVICE"`, `"OUTPUT"`, or `"NO_ADDRESS"`. |
| `dstClass` | String | Java class of receiver section (e.g. `"Queue"`, `"Server"`). |
| `dstDisplay` | String | Pre-formatted string: `StationName [Section:Class]` for immediate UI display. |
| `src`, `dst` | String | Direct station names for quick visualization edge mapping. |

#### 3. Exact Source Code Origin
| Field | Type | Description |
| :--- | :--- | :--- |
| `caller` | String | **Mandatory** exact Java code location where the event was triggered: `Package.Class.method(FileName.java:LineNumber)`. |

#### 4. Station State Variables (at time of event)
| Field | Type | Description |
| :--- | :--- | :--- |
| `nodeJobs` | Integer | Total resident jobs currently inside the station (`jobsList.size()`). |
| `queueSize` | Integer | *(Present if section has a queue)* Current number of jobs waiting in the queue buffer. |
| `queueCap` | Integer | *(Present if section has a queue)* Configured capacity limit of the queue (`-1` if infinite). |
| `servers` | Integer | *(Present if section is a server)* Configured parallel server count. |

#### 5. Job / Packet Variables (if event carries a Job)
| Field | Type | Description |
| :--- | :--- | :--- |
| `jobId` | Integer | Unique integer ID of the customer/packet/job. |
| `jobClass` | String | Name of the job's class (e.g. `"HTTPRequests"`). |
| `classId` | Integer | Integer ID of the job's class. |
| `priority` | Integer | Scheduling priority level of the job. |
| `enterTime` | Double | Simulation time when the job originally entered the network. |
| `serviceTime` | Double | Accumulated time this job has spent receiving service across the network. |
| `queueTime` | Double | Accumulated time this job has spent waiting in queues across the network. |
| `isToken` | Boolean | `true` if this job acts as a synchronizing token (e.g. fork/join). |
| `lastVisited` | String | Station name visited immediately prior to the current station. |

#### 6. Non-Job Payload & Debugging Messages
| Field | Type | Description |
| :--- | :--- | :--- |
| `payloadType` | String | Java class name of non-job data object attached to the event. |
| `payload` | String | String representation of non-job payload data. |
| `msg` | String | Descriptive reason or warning (e.g. drop reason or unhandled message text). |

---

### Line N+1: Summary Footer (`"type": "footer"`)

Emitted into the active chunk file when the simulation completes or terminates.

#### Example Footer
```json
{
  "type": "footer",
  "totalEvents": 154200,
  "droppedEvents": 0,
  "lastSimTime": 1000.0,
  "durationMs": 412,
  "part": 1
}
```

#### Field-by-Field Dictionary: Footer Record
| Field | Type | Description |
| :--- | :--- | :--- |
| `type` | String | Fixed identifier: `"footer"`. |
| `totalEvents` | Long | Total number of simulation events recorded during this run. |
| `droppedEvents` | Long | Ring buffer overflows dropped (should be `0` under normal operation). |
| `lastSimTime` | Double | Final simulation clock timestamp when the run terminated. |
| `durationMs` | Long | Wall-clock execution time in milliseconds. |
| `part` | Integer | The final chunk part index. |

---

## 3. Multiple Simulations & Rolling File Chunks

### Output File Naming & Chunk Series

Each simulation run generates a series of chunked, compressed files ending with `_part1.ndjson.gz`, `_part2.ndjson.gz`, etc.

```
                              [Configured Output Target]
                                          |
                      +-------------------+-------------------+
                      |                                       |
                Target is Directory                    Target is File
             (ends with / or is a dir)         (e.g. visual_trace.ndjson.gz)
                      |                                       |
                      v                                       v
         <dir>/<model>_run<M>_part1.ndjson.gz      Run 1: visual_trace_part1.ndjson.gz
         <dir>/<model>_run<M>_part2.ndjson.gz      Run 1: visual_trace_part2.ndjson.gz
         <dir>/<model>_run<M>_part3.ndjson.gz      Run 2: visual_trace_<model>_run2_part1.ndjson.gz
```

### Rolling File Chunk Thresholds & Overrides

By default, chunks roll when either limit is reached:
* **Default Size Lowerbound**: **500 GB of uncompressed JSON text** (`CHUNK_MAX_MB = 512,000`).
* **Default Event Count Lowerbound**: **850,000,000 events** (which represents ~500 GB of uncompressed JSON text).

You can override these thresholds either via JVM properties or programmatically in code:

| Property | Java Variable | Default | Description |
| :--- | :--- | :--- | :--- |
| `-Djmt.trace.chunk.mb=5120000` | `SimEventTracer.CHUNK_MAX_MB` | `500` | Max compressed MB per chunk file before rolling (`0` = disabled). |
| `-Djmt.trace.chunk.events=100000` | `SimEventTracer.CHUNK_MAX_EVENTS` | `850000000` | Max event count per chunk file before rolling (`0` = disabled). |
| `-Djmt.trace.filter=true` | `SimEventTracer.FILTER_INTERNAL_EVENTS` | `true` | Filter out internal scheduler wakeups (`EVENT_TIMING`, `EVENT_KEEP_AWAKE`). |

---

## 4. How to Run and Enable the Tracer

### 4.1. End-User Activation & JAR Archive Requirement

> [!IMPORTANT]
> The Visual Debug Tracer mode is designed for production execution from a packaged JMT JAR archive (`JMT.jar`). End users can only activate the mode if they run the application from a packaged JAR file. If an attempt is made to activate it from loose class files (e.g., when launched inside an IDE without packaging), the activation is prevented and an explicit warning is displayed.

Simulation traces are saved into the `traces/` folder (`traces/<modelName>_run<M>_part1.ndjson.gz`), which is automatically created if it does not already exist.

---

### 4.2. Activation in the GUI (JSIMGraph)

Users have two ways to toggle the Visual Debug Tracer mode in the GUI:

#### Method 1: The Solve Menu or Keyboard Shortcut
1. Open or build a model in **JSIMGraph**.
2. Go to the menu: **Solve** $\to$ **Visual Debug Tracer Mode** (or press `Alt+Shift+V`).
3. If running from the packaged JAR, the mode toggles ON with a confirmation dialog:
   > *"Visual Debug Tracer mode activated. Simulation traces will be recorded in the 'traces/' folder."*
4. Run your simulation via **Solve $\to$ Simulate** (or the Play button). The trace chunks will appear inside the `traces/` folder.

#### Method 2: The Debug Solver Dialog
1. Go to **Solve** $\to$ **Debug Solver...**.
2. In the configuration dialog, inspect the **Visual Debug Tracer (JSONL.GZ)** section:
   - When running from a JAR archive: The checkbox is enabled and reads `Enable Visual Debug Tracer (logs to traces/ folder)`.
   - When running without a JAR archive: The checkbox is disabled and reads `Enable Visual Debug Tracer (requires packaged JMT JAR)`.
3. Check the box and click **Enable debug logging**.

---

### 4.3. Activation via Command Line Interface (CLI)

Pass the `-trace` (or `-visual-trace`, `-visual-debug-tracer`) command-line flag when invoking `sim`:

```bash
java -jar JMT.jar sim my_model.jsimg -trace
```

#### Additional Options:
* Combine with seed and duration:
  ```bash
  java -jar JMT.jar sim my_model.jsimg -trace -seed 42 -maxtime 120
  ```
* Output files will be stored directly in `traces/<modelName>_run1_part1.ndjson.gz`.
* If `-trace` is passed when running outside of a packaged JAR, the CLI will output:
  ```text
  Error: Visual-debug-tracer mode can only be activated when running from a packaged JMT JAR archive.
  ```
  and exit with error code `1`.

---

### 4.4. Developer Options: JVM Properties & Programmatic API

For development, testing, and advanced customization, developers can also use JVM arguments or programmatic methods:

#### A. Running in IDE via VM Options (IntelliJ IDEA / Eclipse)
Developers can run visual debug tracing directly in the IDE without packaging a JAR by configuring VM options in their Run/Debug Configuration:
```text
-Djmt.trace.visual=true -Djmt.trace.file=traces/
```
*(or `-Djmt.trace.visual=true -Djmt.trace.dir=traces/`)*

When launched with `-Djmt.trace.visual=true`:
- Tracing is enabled immediately and permitted without requiring a packaged `.jar` archive.
- The traces are written to the specified directory (defaults to `traces/`).
- In JSIMGraph, the Debug Solver panel displays `Enable Visual Debug Tracer (active via VM option)` and the checkbox is selected.

#### B. Command Line JVM System Properties
```bash
# Force enable visual tracer and specify folder
java -Djmt.trace.visual=true -Djmt.trace.file=traces/ -jar JMT.jar sim my_model.jsimg

# Custom chunk thresholds (e.g. 10 MB or 100k events per chunk)
java -Djmt.trace.visual=true -Djmt.trace.chunk.mb=10 -Djmt.trace.chunk.events=100000 -jar JMT.jar sim my_model.jsimg
```

#### C. Programmatic Activation in Code
```java
import log.SimEventTracer;

public class MySimulationRunner {
    public static void main(String[] args) throws Exception {
        // Activate visual debug tracer mode (logs into traces/ folder)
        // Pass false if invoking directly in development without JAR checks:
        SimEventTracer.enableVisualDebugTracer(false);

        // Run simulation
        NetSystem netSystem = new NetSystem();
        // ... configure model ...
        netSystem.start();

        // Tracer automatically stops at end of simulation, or stop manually:
        SimEventTracer.stop();
    }
}
```

---

## 5. How to Consume `.ndjson.gz` Chunks in the Visual Debugger

In web browsers or desktop applications:

1. **Streaming Decompression in JavaScript/Web Browser**:
   Modern browsers support native gzip decompression on the fly without writing uncompressed files to disk:
   ```javascript
   const response = await fetch("traces/model_run1_part1.ndjson.gz");
   const decompressedStream = response.body.pipeThrough(new DecompressionStream("gzip"));
   const reader = decompressedStream.getReader();
   // Stream NDJSON lines into your canvas parser
   ```

2. **Multi-Chunk Playback**:
   Because each chunk file (`part1`, `part2`, etc.) begins with Line 1 `metadata`, the visual debugger can open any chunk independently, draw the network topology, and replay the packet animations for that slice of simulation time.

---

## 6. Inspecting Trace Contents with `peek_trace.py`

### 6.1. Purpose & Capabilities

`peek_trace.py` is a dedicated command-line inspection utility created to analyze, verify, and debug JMT simulation trace files.

#### Key Features:
* **Zero-Extraction In-Memory Streaming**: Reads `.ndjson.gz` files on the fly using Python's streaming `gzip` module. It stops immediately after reading the requested number of events, never decompressing gigabytes of data to disk.
* **Dual Format Compatibility**: Seamlessly reads both compressed (`.ndjson.gz`) and uncompressed (`.ndjson`) files.
* **Auto-Discovery of Latest Run**: When given a folder path, it automatically scans for all trace files in that directory, sorts them by last-modified timestamp, and previews the **most recent simulation run**.
* **Human-Readable Terminal Formatting**: Parses minified JSON into structured, color-neutral terminal blocks showing:
  - Network canvas metadata (model name, stations, classes, topology connections).
  - Packet routing transitions with formatted station names and section types.
  - Java caller stack traces (`Package.Class.method(File.java:line)`).
  - Station queue lengths, capacities, and active parallel servers.
  - Job/customer attributes (IDs, classes, priorities, arrival times).
* **Summary Footer Display**: If reaching the end of a trace, it prints the completion summary (`totalEvents`, `droppedEvents`, `durationMs`).

---

### 6.2. Location

The tool is located in the integration test resources:
```text
jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py
```

---

### 6.3. Command Syntax & Parameters

```bash
python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py <folder-or-file-path> [num_events]
```

#### Parameters:

| Parameter | Mandatory? | Description |
| :--- | :---: | :--- |
| `<folder-or-file-path>` | **Yes** | Path to the directory containing traces (e.g. `traces`) **OR** direct path to a `.ndjson.gz` / `.ndjson` file. If omitted, the script exits with an error and usage instructions. |
| `[num_events]` | No | Number of simulation events to display (default: `5`). |
| `-h`, `--help` | No | Displays the help message and command examples. |

---

### 6.4. Usage Examples

#### A. Inspect the Latest Simulation in a Folder (Default: 5 Events)
```bash
python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces
```

#### B. Inspect More Events (e.g., 20 or 50 Events)
```bash
python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces 20
```

#### C. Inspect an Absolute Folder Path (Windows)
```bash
python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py "C:/Users/calog/Documents/JMT Workspace/jmt/traces" 10
```

#### D. Inspect a Specific Rolled Chunk File Directly
```bash
python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces/JSIM_simulation___JModelSimulation2635063350737723356.xml_run1_part1.ndjson.gz 15
```

---

### 6.5. Detailed Output Walkthrough

Running `python jmt-integration-tests/src/test/resources/linetesting/scripts/peek_trace.py traces 3` produces output structured into three primary areas:

```text
================================================================================
Viewing: JSIM_simulation___JModelSimulation2635063350737723356.xml_run1_part1.ndjson.gz (93.66 MB compressed)
Path   : C:\Users\calog\Documents\JMT Workspace\jmt\traces\JSIM_simulation___JModelSimulation2635063350737723356.xml_run1_part1.ndjson.gz
================================================================================

[LINE 1: NETWORK TOPOLOGY METADATA]
  Model Name : JSIM simulation: ~JModelSimulation2635063350737723356.xml
  Timestamp  : 1793198698085
  Stations (2): ['CPU', 'FlowEq']
  Job Classes (1): ['Class0']
  Connections (2 links):
    - CPU -> FlowEq
    - FlowEq -> CPU

[NEXT 3 EVENTS]

--- Event #1 (Seq: 1, SimTime: 0.0000, Phase: SEND) ---
  Event   : EVENT_START (ID: 2)
  Route   : CPU [NO_ADDRESS:None] -> CPU [INPUT:Queue]
  Caller  : QueueNet.NetSystem.start(NetSystem.java:128)
  State   : nodeJobs=3, queueSize=0, queueCap=0, servers=0

--- Event #2 (Seq: 2, SimTime: 0.0000, Phase: SEND) ---
  Event   : EVENT_START (ID: 2)
  Route   : CPU [NO_ADDRESS:None] -> CPU [SERVICE:Server]
  Caller  : QueueNet.NetSystem.start(NetSystem.java:129)
  State   : nodeJobs=3, queueSize=0, queueCap=0, servers=0

--- Event #3 (Seq: 3, SimTime: 0.0000, Phase: SEND) ---
  Event   : EVENT_START (ID: 2)
  Route   : CPU [NO_ADDRESS:None] -> CPU [OUTPUT:Router]
  Caller  : QueueNet.NetSystem.start(NetSystem.java:130)
  State   : nodeJobs=3, queueSize=0, queueCap=0, servers=0
```

#### Output Fields Explained:
* **Header Block**: Shows the active file name, file size on disk (e.g. `93.66 MB compressed`), compression status, and resolved absolute filesystem path.
* **`[LINE 1: NETWORK TOPOLOGY METADATA]`**:
  - `Model Name`: The JMT model identifier.
  - `Timestamp`: Simulation start UNIX timestamp.
  - `Stations`: List of all stations in the queue network.
  - `Job Classes`: Workload classes defined in the network.
  - `Connections`: Directed link topology representing packet routes.
* **`[EVENT BLOCKS]`**:
  - `Event #N`: Incremental displayed index with globally monotonic `Seq`, exact simulation clock `SimTime`, and lifecycle `Phase` (`SEND`, `DISPATCH`, `DROP`, `NOT_PROCESSED`).
  - `Event`: Event name constant (e.g. `EVENT_JOB`, `EVENT_ACK`, `EVENT_START`) and numerical ID.
  - `Route`: Formatted path showing `srcStation [srcSection:srcClass] -> dstStation [dstSection:dstClass]`.
  - `Caller`: Exact Java code source location `Class.method(File.java:line)`.
  - `Job`: *(If present)* Job ID, class name, priority, and entering time.
  - `State`: Real-time station state (`nodeJobs`: total resident jobs, `queueSize`: waiting buffer, `queueCap`: capacity, `servers`: parallel servers).
  - `Message`: *(If present)* Direct diagnostic message (e.g. drop reason or warning).
