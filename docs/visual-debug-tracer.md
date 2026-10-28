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
|  [NetNode.dispatch()]-+--> [Hot-Path Guard: DEBUG_VISUAL_TRACE == false ? return]  |
|                       |            |                                               |
|  [Queue.process()] ---+            v (if enabled)                                  |
|                                    |                                               |
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
|                 ---> Buffered disk write (64 KB output buffer)                     |
|                 ---> Writes to isolated, non-colliding files                       |
+------------------------------------------------------------------------------------+
```

1. **Zero-Overhead Hot Path when Disabled**:
   - Guarded by a volatile boolean flag `SimEventTracer.DEBUG_VISUAL_TRACE`.
   - When set to `false` (default), the check is an inlined volatile read costing **~0–1 nanosecond**.
   - No object allocation, no stack trace walking, and no lock contention occur.

2. **Zero-Allocation SPSC Ring Buffer**:
   - Implements a Single-Producer Single-Consumer (SPSC) lock-free ring buffer with **262,144 pre-allocated slots** (`SimEventRecord`).
   - Slot indexing uses bitwise AND masking (`writeIndex & 0x3FFFF`), completely eliminating modulo division and object instantiations on the simulation thread.
   - Prevents Garbage Collector (GC) pressure and avoids distorting discrete-event simulation timings.

3. **Asynchronous Background Disk I/O**:
   - A dedicated daemon thread (`WriterThread`) continuously drains the ring buffer in batches of up to 2,048 records.
   - I/O operations are buffered with a 64 KB write buffer and encoded in UTF-8.
   - If the ring buffer fills during an extreme burst, the writer thread keeps pace while overflow events are tracked via `DROPPED_EVENTS`.

4. **Engine Lifecycle & Interception Hooks**:
   - `QueueNet.NetSystem.start()`: Writes the Line 1 network topology metadata header.
   - `QueueNet.NetNode.send()`: Intercepts all scheduled event sends (`SEND` phase).
   - `QueueNet.NetNode.dispatch()`: Intercepts all event deliveries at destination stations (`DISPATCH` phase).
   - `QueueNet.NetNode` unhandled messages: Intercepts routing failures and unhandled messages (`NOT_PROCESSED` phase).
   - `NodeSections.Queue.process()`: Intercepts packet/job drops when queue capacity is reached (`DROP` phase).
   - `QueueNet.NetSystem.run()` (`finally` block) / `terminate()`: Flushes remaining events, appends the footer summary, closes the file, and resets state.

---

## 2. Output File Specification (NDJSON)

The tracer writes output in **Newline-Delimited JSON** (`.ndjson`). Each line is a self-contained, valid JSON object ending with `\n`.

### File Structure Summary

| Line Number | Record Type | Purpose |
| :--- | :--- | :--- |
| **Line 1** | `metadata` | Complete network canvas topology (stations, capacities, servers, job classes, connections) required to initialize and draw the visual debugger canvas. |
| **Lines 2 .. N** | `event` | Chronological stream of simulation events with full packet/station state and source code locations. |
| **Line N+1** | `footer` | Simulation run completion summary (total events, duration, dropped count, last sim time). |

---

### Line 1: Network Topology Metadata (`"type": "metadata"`)

Emitted exactly once at the start of each simulation run. The visual debugger uses this header to layout nodes, configure queues, and draw connection wires before playback starts.

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

Emitted for every discrete event transition occurring in the simulation kernel.

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

Emitted when the simulation completes or terminates.

#### Example Footer
```json
{
  "type": "footer",
  "totalEvents": 154200,
  "droppedEvents": 0,
  "lastSimTime": 1000.0,
  "durationMs": 412
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

---

## 3. Multiple Simulations & What-If File Isolation

When running multiple simulations in the same JVM session—such as parametric sweeps, what-if analyses, multi-seed replications, or multiple batch models—**runs must never overwrite each other**.

### Resolution Rules

`SimEventTracer` automatically segregates trace outputs based on whether you specify a **directory** or a **file path**:

```
                              [Configured Output Target]
                                          |
                      +-------------------+-------------------+
                      |                                       |
                Target is Directory                    Target is File
             (ends with / or is a dir)           (e.g. visual_trace.ndjson)
                      |                                       |
                      v                                       v
         <dir>/<model>_step<N>_run<M>.ndjson       Run 1: visual_trace.ndjson
                                                   Run 2: visual_trace_<model>_run2.ndjson
                                                   Run 3: visual_trace_<model>_run3.ndjson
```

1. **Target is a Directory** (e.g., `-Djmt.trace.dir=traces/` or `-Djmt.trace.file=C:\traces\`):
   - Automatically creates the directory if it does not exist.
   - Generates a cleanly named file per run:
     ```
     <modelName>_run1.ndjson
     <modelName>_run2.ndjson
     <modelName>_step1_run3.ndjson   (if parametric step > 0)
     ```

2. **Target is a Specific File** (e.g., `-Djmt.trace.file=my_trace.ndjson`):
   - Run 1 writes directly to `my_trace.ndjson`.
   - Run 2 automatically writes to `my_trace_<modelName>_run2.ndjson`.
   - Run 3 automatically writes to `my_trace_<modelName>_run3.ndjson`.
   - Subsequent runs never truncate or overwrite prior files.

3. **Lifecycle Reset Between Runs**:
   - When a simulation ends, `SimEventTracer.stop()` closes the current file stream, clears the ring buffer, and resets the sequence numbers.
   - When the next simulation starts, `SimEventTracer.initTrace(network)` assigns a new file, emits Line 1 metadata for the new model, and restarts event sequencing from `seq: 1`.

---

## 4. How to Run and Enable the Tracer

### Option 1: Via Command Line (CMD / PowerShell / Bash)

To enable tracing when running JMT from the command line, pass the JVM system property `-Djmt.trace.visual=true`.

#### A. Basic Run (Default File `visual_trace.ndjson`)
```bash
java -Djmt.trace.visual=true -jar jmt.jar sim my_model.jsimg
```

#### B. Output to a Specific File
```bash
java -Djmt.trace.visual=true -Djmt.trace.file="C:\Users\calog\traces\trace_run.ndjson" -jar jmt.jar sim my_model.jsimg
```

#### C. Output to a Dedicated Folder (Ideal for Batch / Multiple Runs)
Specify a folder path ending with a slash (`/` or `\`):
```bash
java -Djmt.trace.visual=true -Djmt.trace.dir="./traces" -jar jmt.jar sim my_model.jsimg
```
Or via `-Djmt.trace.file`:
```bash
java -Djmt.trace.visual=true -Djmt.trace.file="./traces/" -jar jmt.jar sim my_model.jsimg
```

---

### Option 2: Using IntelliJ IDEA

You can enable visual tracing directly in your IntelliJ Run/Debug configurations without modifying source code.

#### Step-by-Step Instructions

1. Open your project in **IntelliJ IDEA**.
2. In the top toolbar, click the **Run Configuration** dropdown (next to the green Play button) and select **Edit Configurations...**.
3. Select your Application or JUnit test configuration on the left panel (e.g., `JMT-GUI`, `JMT-Engine`, or a test class).
4. Look for the **VM options** field:
   - *If VM options is not visible*: Click **Modify options** (or `Alt+M`) and check **Add VM options**.
5. In the **VM options** field, enter:
   ```text
   -Djmt.trace.visual=true -Djmt.trace.dir=./traces
   ```
   *(Or specify an exact file: `-Djmt.trace.visual=true -Djmt.trace.file=visual_trace.ndjson`)*
6. Click **Apply** and **OK**.
7. Run or Debug the application. The trace files will be automatically generated inside the `./traces` directory in your project root.

```
+-------------------------------------------------------------------------------+
| Run/Debug Configurations                                                      |
+-------------------------------------------------------------------------------+
| Name: RunSimulation                                                           |
| Main class: jmt.commandline.Jmt                                               |
| VM options: -Djmt.trace.visual=true -Djmt.trace.dir=./traces                  |
| Working directory: C:\Users\calog\Documents\JMT Workspace\jmt                 |
+-------------------------------------------------------------------------------+
```

---

### Option 3: Programmatic Activation in Code

You can also control tracing directly from Java code (useful in integration tests or custom simulation harnesses):

```java
import log.SimEventTracer;

public class MySimulationRunner {
    public static void main(String[] args) throws Exception {
        // 1. Enable tracing to a folder or file before simulation starts
        SimEventTracer.enable("./traces");

        // 2. Run simulation normally
        NetSystem netSystem = new NetSystem();
        // ... configure model ...
        netSystem.start();

        // 3. Stop tracing manually (if not relying on shutdown hooks)
        SimEventTracer.stop();
    }
}
```

#### Available API Methods

| Method | Description |
| :--- | :--- |
| `SimEventTracer.enable()` | Enables tracing using default file (`visual_trace.ndjson`). |
| `SimEventTracer.enable(String path)` | Enables tracing pointing to a specific file or output directory. |
| `SimEventTracer.disable()` | Disables tracing. Hot-path calls immediately bypass recording. |
| `SimEventTracer.setOutputFile(String path)` | Sets the default output destination path. |
| `SimEventTracer.getCurrentOutputFile()` | Returns the `File` currently being written to for the active run. |
| `SimEventTracer.stop()` | Flushes all buffered events, writes the footer, and closes the active file. |