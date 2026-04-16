
# XML Serialization Architecture — Refactoring Documentation

## 1. Legacy Implementation: Critique

`XMLReader` and `XMLWriter` represent a classic example of procedural God-class design applied to a domain that fundamentally requires structured decomposition. Both classes exceed 2,700 lines of code and mix concerns indiscriminately: XML traversal, DOM manipulation, domain-object construction, strategy resolution, and model mutation are all co-located in monolithic static methods. Shared mutable state is managed through a set of `static` fields (e.g., `classes`, `stations`, `refStations`, `empiricalRouting`) that are re-initialized at the beginning of each parse cycle, making the code inherently non-reentrant and brittle under concurrent access. The writer compounds this with a proliferation of ad-hoc inner classes (`ForkStrategyWriter`, `JoinStrategyWriter`, `SemaphoreStrategyWriter`, etc.) that duplicate structural boilerplate and resist independent testing. The net result is a codebase with extremely high coupling, near-zero cohesion at the method level, no clear extension points, and a maintenance surface that scales poorly with the addition of new station types or strategies.

The same structural problems affected the GUI and results layers: `GuiXMLReader`/`GuiXMLWriter` and `XMLResultsReader`/`XMLResultsWriter` were similarly monolithic, mixing DOM traversal with domain-object mutation in long static methods with no intermediate representation.

Additionally, string constants were scattered and duplicated across `XMLReader`, `XMLWriter`, and other files, and lookup maps were built inside `static {}` initializer blocks — both patterns flagged by SonarQube as maintainability and code-smell issues.

---

## 2. The Two-Phase Transition

The refactoring replaces the direct XML-to-model coupling with a clean two-phase pipeline that introduces an intermediate representation layer. The same pattern is applied uniformly across all three XML sub-domains: simulation model (`sim/`), GUI metadata (`gui/`), and simulation results (`results/`).

### Phase 1 — Parsing: `XML → DTO`

The first phase is a pure structural transformation. An XML document (or DOM subtree) is traversed and its data is transcribed into a tree of plain Data Transfer Objects (DTOs). This phase is entirely decoupled from any model class. Parsers have no knowledge of domain logic, strategies, or model keys — they only read XML structure and populate fields. The output is a fully self-contained, serializable snapshot of the XML document.

### Phase 2 — Mapping: `DTO → Model`

The second phase interprets the DTO tree and populates the target model. This is where domain logic lives: station type resolution, strategy instantiation, key management, and cross-reference post-processing. Mappers operate on the already-validated DTO, never touching the DOM directly.

The same two phases run in reverse for the writing direction (`Model → DTO → XML`), making the architecture fully bidirectional.

This separation brings three immediate benefits:

- **Testability**: parsers and mappers can be unit-tested independently, without a real XML file or a real model.
- **Replaceability**: the serialization format (XML, JSON, binary) can be changed by replacing only the parser/serializer layer, with no impact on mappers.
- **Auditability**: the intermediate DTO provides a stable, inspectable snapshot of the model state at the boundary between the two phases.

---

## 3. Simulation Model Layer (`sim/`)

### 3.1 Overview

The architecture is organized into four distinct layers:

```
XML Document
     │
     ▼
[ Parser Layer ]        — XML → DTO
     │
     ▼
[ DTO Layer ]           — Intermediate representation
     │
     ▼
[ Mapper Layer ]        — DTO ↔ CommonModel (bidirectional)
     │
     ▼
[ Serializer Layer ]    — DTO → XML Document
```

Each layer has a single, well-defined responsibility and communicates with adjacent layers only through the DTO contract.

---

### 3.2 DTO Layer

The DTO layer is a hierarchy of plain Java objects with no behavior beyond field access. It mirrors the logical structure of the XML schema.

| Class | Responsibility |
|---|---|
| `SimDescriptor` | Root aggregate: holds global attributes, user classes, nodes, connections, measures, blocking regions, and preloads |
| `UserClassDescriptor` | Represents a job class (type, priority, population, reference source, deadlines) |
| `NodeDescriptor` | Represents a station node: name, ordered list of `SectionDescriptor`, node-level parameters (deadlines, quantum) |
| `SectionDescriptor` | Represents a section within a node (e.g., Queue, Server, Router): class name and list of `ParameterDescriptor` |
| `ParameterDescriptor` | Recursive parameter node: name, class path, value, ref-class tag, array/map flags, and nested sub-parameters |
| `ConnectionDescriptor` | A directed edge between two station names |
| `MeasureDescriptor` | A performance measure definition: type, reference node, reference class, statistical parameters |
| `BlockingRegionDescriptor` | A finite-capacity region: member nodes, global/class/group constraints, drop rules, weights, sizes |

All collections default to empty (never `null`), and setters perform defensive null-checks, ensuring that downstream consumers do not need null-guards on iteration.

The DTOs were originally located in a flat `dto/` package directly under `jmt.gui.common.xml`. As part of this sprint, they were reorganised into `sim/dto/` to make sub-domain ownership unambiguous, and four classes that were specific to the sim model (`BlockingRegionDescriptor`, `NodeDescriptor`, `SimDescriptor`, `UserClassDescriptor`) were rewritten to support the full bidirectional pipeline.

---

### 3.3 Parser Layer

Parsers implement the `XmlEntityParser<T>` interface:

```java
public interface XmlEntityParser<T> {
    T parse(Element element);
}
```

Each parser is responsible for exactly one DTO type and reads attributes and child elements from the DOM without any model interaction.

| Parser | Output |
|---|---|
| `SimParser` | `SimDescriptor` — dispatches child elements to specialized sub-parsers |
| `NodeParser` | `NodeDescriptor` — handles sections, soft deadlines, quantum parameters, and generic parameter fallback |
| `SectionParser` | `SectionDescriptor` — reads the section class name and delegates parameters to `ParameterParser` |
| `ParameterParser` | `ParameterDescriptor` — recursively parses `<subParameter>` nesting, `<value>`, and `<refClass>` elements |

`SimParser` acts as the top-level dispatcher. It iterates the direct children of the root `<sim>` element and routes each tag name to the appropriate parsing method: `<node>` → `NodeParser`, `<userClass>` → inline attribute reading, `<connection>` → `ConnectionDescriptor`, `<measure>` → `MeasureDescriptor`, `<region>` → blocking region parsing, `<preload>` → preload population parsing.

The `ParameterParser` deserves special attention: it reconstructs the `refClass` association correctly by pairing `<refClass>` text nodes with the immediately following `<subParameter>` element, preserving the ordering semantics of the original XML schema.

---

### 3.4 Mapper Layer

The mapper layer is the core of the architecture. It translates between the DTO representation and the `CommonModel`, in both directions.

#### 3.4.1 `ModelElementMapper` Interface

All mappers implement the `ModelElementMapper` interface, which defines the bidirectional contract:

```java
public interface ModelElementMapper extends XMLConstantNames {
    void mapDtoToCommonModel(SimDescriptor dto, MappingContext context);   // READ
    void mapCommonModelToDto(SimDescriptor dto, WritingContext context);    // WRITE
}
```

The interface also provides a rich set of `default` utility methods covering null-safe value extraction (`val`, `valD`, `valI`), typed parsing (`getInt`, `getDouble`, `getBoolean`), DTO factory methods (`makeParam`, `makeContainer`, `makeRefParam`), and DTO navigation helpers (`getParam`, `getSubParams`, `getRefClassMap`, `getFirst`, `getAt`). These helpers eliminate boilerplate in all concrete implementations and enforce consistency in how parameters are read and constructed.

#### 3.4.2 `MappingContext` and `WritingContext`

State that must be shared across multiple mappers within a single pipeline execution is encapsulated in context objects rather than static fields, eliminating the reentrancy issues of the legacy code.

**`MappingContext`** (read direction) holds:
- `CommonModel model` — the model being populated
- `Map<String, Object> classes` — maps class names to their model keys, populated by `ClassesMapper` and consumed by all section mappers
- `Map<String, Object> stations` — maps station names to their model keys, populated by `StationsMapper`
- `Map<String, Object> regions` — maps region names to their model keys, populated by `BlockingRegionsMapper`
- `Map<Object, String> refStations` — deferred reference source associations, resolved in post-processing
- Post-processing maps for empirical routing, load-dependent routing, WRR routing, fork strategies, and Petri net transition conditions
- Lazily initialized lookup caches: `engineToGuiDistr`, `engineToGuiRouting`, `engineToGuiFork`, `engineToGuiJoin`, `engineToGuiSemaphore` — populated on first access by scanning the available strategy registries

**`WritingContext`** (write direction) is intentionally minimal: it holds only a reference to the `CommonModel`. All information needed for writing is obtained directly from the model through its public API (`getStationName`, `getClassName`, `getClassKeys`, etc.), with no auxiliary state required.

#### 3.4.3 `SimDescriptorMapper` and the Pipeline

`SimDescriptorMapper` is the orchestrator for both directions. It constructs an ordered `List<ModelElementMapper>` — the pipeline — and executes each stage in sequence.

```
Pipeline (read):   GlobalSettingsMapper
                 → ClassesMapper
                 → StationsMapper
                 → ConnectionsMapper
                 → BlockingRegionsMapper
                 → MeasuresMapper
                 → PreloadMapper
                 → PostProcessingMapper
```

The pipeline order is not arbitrary: `ClassesMapper` must run before `StationsMapper` because section mappers require class keys to be available in the context; `StationsMapper` must run before `ConnectionsMapper` and `BlockingRegionsMapper` for the same reason; `PostProcessingMapper` runs last to resolve all forward-references accumulated during the earlier stages (empirical routing tables, fork strategy targets, Petri net conditions).

In the write direction, the same pipeline runs in the same order but calls `mapCommonModelToDto` on each stage, progressively populating the `SimDescriptor`.

`SimDescriptorMapper` exposes two factory-style constructors:
- `SimDescriptorMapper(SimDescriptor dto, CommonModel model)` — for reading: takes an already-parsed DTO and maps it into the model
- `SimDescriptorMapper(CommonModel model)` — for writing: allocates a fresh `SimDescriptor` and populates it from the model

#### 3.4.4 Station Mapper and Section Mappers

`StationsMapper` implements a signature-based station type resolution strategy. A `STATION_SIGNATURES` map associates ordered lists of section class names (the "signature") with `CommonConstants` station type constants. For example, `[Queue, Server, Router]` resolves to `STATION_TYPE_SERVER`. An inverse `TYPE_TO_SIGNATURE` map supports the write direction.

Section-level behavior is delegated to `AbstractSectionMapper` subclasses:

```java
public abstract class AbstractSectionMapper implements XMLConstantNames {
    public abstract void mapDtoToCommonModel(...);
    public abstract void mapCommonModelToDto(...);
    // shared helpers: parseServiceStrategy, getDistributionParameter, etc.
}
```

Each section mapper handles exactly one section type (e.g., `SourceSectionMapper`, `ServerSectionMapper`, `QueueSectionMapper`, `RouterSectionMapper`, `JoinSectionMapper`, `ForkSectionMapper`, `StorageSectionMapper`, etc.). The `SECTION_MAPPERS` registry in `StationsMapper` maps section class names to their corresponding mapper instances. Multiple engine-level class names (e.g., `Server`, `PreemptiveServer`, `PSServer`) may share the same mapper instance where their mapping logic is equivalent.

---

### 3.5 Serializer Layer

The `XmlSerializer` is responsible for converting a fully populated `SimDescriptor` back into a DOM `Document`. It is the counterpart of the parser layer and, like the parsers, operates without any knowledge of the `CommonModel`. It traverses the DTO tree and emits XML elements and attributes according to the schema structure, using the constant names defined in `XMLConstantNames`.

The `XmlSerializer` is invoked exclusively by `XMLSimWriter.getDocument()`, which acts as the integration point between the mapper and serializer layers:

```java
// XMLSimWriter.getDocument()
SimDescriptorMapper mapper = new SimDescriptorMapper(model);
SimDescriptor dto = mapper.mapToDto();         // Mapper layer
return new XmlSerializer().serialize(dto);     // Serializer layer
```

---

### 3.6 Entry Points and Naming

| Class | Direction | Responsibility |
|---|---|---|
| `XMLSimReader` | Read | Delegates XML parsing to `SimParser` and DTO-to-model mapping to `SimDescriptorMapper.mapToModel()` |
| `XMLSimWriter` | Write | Calls `SimDescriptorMapper.mapToDto()` then `XmlSerializer.serialize()`, then pipes the DOM through a secure `Transformer` |
| `XMLLoader` | Read (I/O) | Handles document loading from storage: opens the file, optionally validates against an XSD schema, and returns the parsed DOM `Document` |
| `XMLArchiver` | Both | Orchestrates the full archive lifecycle: combines the sim document (from `XMLSimWriter`), the GUI document (from `gui/GuiXMLWriter`), and the results document (from `results/XMLResultsWriter`) into a single archive root element |

#### Naming rationale: `XMLReader` → `XMLSimReader` and the extraction of `XMLLoader`

The legacy `XMLReader` conflated two distinct responsibilities:

1. **Storage access** — opening the file from disk, validating it against the XSD schema, and constructing the DOM `Document`.
2. **Simulation-model parsing** — interpreting the `<sim>` element and populating the `CommonModel`.

These concerns are now separated into two dedicated classes. `XMLLoader` owns responsibility (1): it exposes `loadXML(String filename, String schemaSource)` and its no-schema overload, and returns a raw `Document` with no knowledge of any model. `XMLSimReader` owns responsibility (2): it calls `XMLLoader` internally when a full load-from-file workflow is required, but its `parseXML(Element, CommonModel)` entry point can also be invoked directly on an already-parsed DOM element (e.g., when `XMLArchiver` has already opened the archive and extracted the `<sim>` child). The `Sim` qualifier in the name makes explicit that this reader handles only the simulation-model section of the archive, distinguishing it from the GUI reader and the results reader that operate on the other two sections.

---

### 3.7 Package Structure

```
jmt.gui.common.xml
├── XMLLoader.java                      Document loading and XSD validation
├── XMLArchiver.java                    Archive orchestration
│
└── sim/
    ├── XMLSimReader.java               Entry point (read)
    ├── XMLSimWriter.java               Entry point (write)
    │
    ├── dto/
    │   ├── SimDescriptor.java
    │   ├── NodeDescriptor.java
    │   ├── SectionDescriptor.java
    │   ├── ParameterDescriptor.java
    │   ├── UserClassDescriptor.java
    │   ├── ConnectionDescriptor.java
    │   ├── MeasureDescriptor.java
    │   └── BlockingRegionDescriptor.java
    │
    ├── parsers/
    │   ├── SimParser.java
    │   ├── NodeParser.java
    │   ├── SectionParser.java
    │   └── ParameterParser.java
    │
    ├── mapper/
    │   ├── ModelElementMapper.java     Interface + default utilities
    │   ├── MappingContext.java
    │   ├── SimDescriptorMapper.java    Orchestrator (pipeline)
    │   ├── GlobalSettingsMapper.java
    │   ├── ClassesMapper.java
    │   ├── StationsMapper.java
    │   ├── ConnectionsMapper.java
    │   ├── BlockingRegionsMapper.java
    │   ├── MeasuresMapper.java
    │   ├── PreloadMapper.java
    │   ├── PostProcessingMapper.java
    │   └── sections/
    │       ├── AbstractSectionMapper.java
    │       ├── SourceSectionMapper.java
    │       ├── ServerSectionMapper.java
    │       ├── QueueSectionMapper.java
    │       ├── RouterSectionMapper.java
    │       ├── DelaySectionMapper.java
    │       ├── ForkSectionMapper.java
    │       ├── JoinSectionMapper.java
    │       ├── LoggerSectionMapper.java
    │       ├── ClassSwitchSectionMapper.java
    │       ├── SemaphoreSectionMapper.java
    │       ├── StorageSectionMapper.java
    │       ├── EnablingSectionMapper.java
    │       ├── TimingSectionMapper.java
    │       └── FiringSectionMapper.java
    │
    └── serializer/
        ├── WritingContext.java
        └── XmlSerializer.java
```

---

## 4. Results Layer (`results/`)

### 4.1 Overview

The results layer applies the same two-phase pipeline to simulation output documents. It handles two distinct XML formats produced at different points in the simulation lifecycle:

- **GUI format** (`Results.xsd`) — written by the refactored `results/XMLResultsWriter` after a simulation completes; the root element is `<results>`.
- **Engine format** (`SIMmodeloutput.xsd`) — written by the simulation engine directly; the root element is `<solutions>`.

Both formats are parsed by the same `ResultsParser` which dispatches to format-specific internal methods (`parseGuiResults` / `parseEngineResults`). The DTO and mapper layers are format-agnostic.

---

### 4.2 DTO Layer

| Class | Responsibility |
|---|---|
| `ResultsDescriptor` | Root aggregate: global metadata (polling interval, elapsed time, log decimal separator, log CSV delimiter) and ordered list of `ResultMeasureDescriptor` |
| `ResultMeasureDescriptor` | A single performance measure: name, numeric type code, station name, class name, node type, statistical parameters (alpha, precision), sample counts, convergence state, and list of `SampleDescriptor` |
| `SampleDescriptor` | A single statistical sample point: mean, lower/upper confidence bounds, last-interval average, simulation time, and validity flag (used only for parametric analysis) |

---

### 4.3 Parser Layer

`ResultsParser` implements `XmlEntityParser<ResultsDescriptor>`. Its `parse(Element root)` entry point inspects the root element name to select the appropriate internal parsing path:

- `<results>` → `parseGuiResults`: reads the GUI-format attributes (`pollingInterval`, `elapsedTime`, `logDecimalSeparator`, `logDelimiter`) and iterates `<measure>` children, each with nested `<sample>` elements.
- `<solutions>` → `parseEngineResults`: reads the engine-format structure and normalises it into the same `ResultsDescriptor` shape.

Both paths produce an identical DTO, so the mapper layer sees only one representation regardless of origin.

---

### 4.4 Mapper Layer

`ResultsDescriptorMapper` is bidirectional and handles both the `StoredResultsModel` (standard simulation) and `PAResultsModel` (parametric analysis) target types.

**Read direction** (`mapDtoToModel`): dispatches to `mapDtoToStoredModel` or `mapDtoToParametricModel` based on the runtime type of the target `MeasureDefinition`. Each path calls the appropriate `addMeasure` / `addSample` overloads on the model, preserving all statistical fields.

**Write direction** (`mapModelToDto`): iterates `MeasureDefinition.getMeasureNumber()` and builds one `ResultMeasureDescriptor` per measure. The numeric type code is produced by `EngineUtils.decodeMeasureType(model.getMeasureType(i))`, maintaining compatibility with the engine's type vocabulary. Confidence bounds of zero or infinity are suppressed (not written to the DTO), matching the original writer's behaviour.

---

### 4.5 Serializer Layer

`ResultsSerializer` converts a `ResultsDescriptor` into a DOM `Document` rooted at `<results>` (GUI format). It writes the global attributes on the root element, then emits one `<measure>` child per `ResultMeasureDescriptor`, each containing its `<sample>` children.

---

### 4.6 Entry Points

| Class | Direction | Responsibility |
|---|---|---|
| `results/XMLResultsReader` | Read | Extracts the `<results>` or `<solutions>` element, delegates to `ResultsParser`, then to `ResultsDescriptorMapper.mapDtoToModel()` |
| `results/XMLResultsWriter` | Write | Calls `ResultsDescriptorMapper.mapModelToDto()` then `ResultsSerializer.serialize()`, pipes DOM through a secure `Transformer` |

#### Legacy compatibility

The root package still contains `XMLResultsReader.java` and `XMLResultsWriter.java` as deprecated stubs. `XMLArchiver` imports the new implementations from the `results/` subpackage. The stubs exist only to avoid breaking any external call sites that reference the old fully-qualified names and will be removed in a subsequent cleanup.

---

### 4.7 Package Structure

```
jmt.gui.common.xml
└── results/
    ├── XMLResultsReader.java           Entry point (read)
    ├── XMLResultsWriter.java           Entry point (write)
    │
    ├── dto/
    │   ├── ResultsDescriptor.java
    │   ├── ResultMeasureDescriptor.java
    │   └── SampleDescriptor.java
    │
    ├── parsers/
    │   └── ResultsParser.java
    │
    ├── mapper/
    │   └── ResultsDescriptorMapper.java
    │
    └── serializer/
        └── ResultsSerializer.java
```

---

## 5. GUI Layer (`gui/`)

### 5.1 Overview

The GUI layer applies the same two-phase pipeline to the graphical metadata stored in the `<jmodel>` section of an archive (`JModelGUI.xsd`). This section contains information that is relevant only to the visual editor — class colours, station canvas positions, connection routing paths, and the parametric analysis configuration — and is entirely absent from archives generated by the command-line simulation engine.

---

### 5.2 DTO Layer

| Class | Responsibility |
|---|---|
| `GuiDescriptor` | Root aggregate: ordered list of `ClassColorDescriptor`, list of `StationPositionDescriptor`, list of `ConnectionShapeDescriptor`, and optional `ParametricAnalysisDescriptor` |
| `ClassColorDescriptor` | The ARGB hex colour string associated with a user class name |
| `StationPositionDescriptor` | Canvas position of a station node: name, x, y, rotate flag, rotation angle |
| `ConnectionShapeDescriptor` | Graphical shape of a directed edge: source station name, target station name, ordered list of `ArcDescriptor` forming the Bézier path |
| `ArcDescriptor` | A single arc segment: source endpoint, two Bézier control points, target endpoint (all as double pairs) |
| `ParametricAnalysisDescriptor` | Parametric analysis block: enabled flag, fully-qualified implementation class name, and a key/value property map |

---

### 5.3 Parser Layer

`GuiParser` implements `XmlEntityParser<GuiDescriptor>`. Its `parse(Element root)` entry point iterates the children of the `<jmodel>` root and delegates to four private methods:

- `parseClassColors` — reads `<userClass color="..." name="..."/>` elements
- `parseStationPositions` — reads `<station name="..."><position x="..." y="..." rotate="..." angle="..."/>` elements, treating `angle` as optional (defaults to `0.0`)
- `parseConnectionShapes` — reads `<connectionShape source="..." target="...">` elements with nested `<arc>` and their `<source>`, `<point>`, `<target>` children
- `parseParametricAnalysis` — reads the optional `<parametric classPath="..." enabled="...">` element with its `<field name="..." value="..."/>` children

---

### 5.4 Mapper Layer

`GuiDescriptorMapper` is bidirectional. The graphical sections (colours, positions, connection shapes) are applied only when the target model is an instance of `JSimGraphModel`; the parametric analysis block is applied to any `CommonModel`.

**Read direction** (`mapDtoToModel`): resolves class and station names against the keys already present in the model (which must have been populated by the simulation mapper first), then calls the appropriate setters (`setClassColor`, `setStationPosition`, `setConnectionShape`, `setParametricAnalysisModel`).

**Write direction** (`mapModelToDto`): iterates all class keys to extract colours (converting `Color.getRGB()` to a `#RRGGBBAA` hex string), all station keys to extract positions (including `angle`), all station-pair combinations to extract stored connection shapes, and the parametric analysis model to populate the `ParametricAnalysisDescriptor`. When parametric analysis is disabled, a descriptor with `enabled=false` is written explicitly so that the reader can restore the disabled state unambiguously.

---

### 5.5 Serializer Layer

`GuiSerializer` converts a `GuiDescriptor` into a DOM `Document` rooted at `<jmodel>`. It emits `<userClass>`, `<station>`, `<connectionShape>`, and `<parametric>` children in order, following the `JModelGUI.xsd` structure.

---

### 5.6 Entry Points

| Class | Direction | Responsibility |
|---|---|---|
| `gui/GuiXMLReader` | Read | Delegates to `GuiParser`, then to `GuiDescriptorMapper.mapDtoToModel()`; must be called after the simulation model has been loaded |
| `gui/GuiXMLWriter` | Write | Calls `GuiDescriptorMapper.mapModelToDto()` then `GuiSerializer.serialize()`, pipes DOM through a secure `Transformer` |

#### Legacy compatibility

The root package still contains `GuiXMLReader.java` and `GuiXMLWriter.java` as deprecated stubs. `XMLArchiver` imports the new implementations from the `gui/` subpackage. The stubs will be removed in a subsequent cleanup.

---

### 5.7 Package Structure

```
jmt.gui.common.xml
└── gui/
    ├── GuiXMLReader.java               Entry point (read)
    ├── GuiXMLWriter.java               Entry point (write)
    │
    ├── dto/
    │   ├── GuiDescriptor.java
    │   ├── ClassColorDescriptor.java
    │   ├── StationPositionDescriptor.java
    │   ├── ConnectionShapeDescriptor.java
    │   ├── ArcDescriptor.java
    │   └── ParametricAnalysisDescriptor.java
    │
    ├── parsers/
    │   └── GuiParser.java
    │
    ├── mapper/
    │   └── GuiDescriptorMapper.java
    │
    └── serializer/
        └── GuiSerializer.java
```

---

## 6. Constant Consolidation — `XMLConstantNames`

### 6.1 Problem

Before the refactoring, XML constant strings (element names, attribute names, class-path literals, strategy name tokens) were scattered across `XMLReader`, `XMLWriter`, and other callers as `protected static final String` fields or inline literals. This caused:

- **Duplication**: the same string appeared in both the reader and the writer with no single authoritative definition.
- **SonarQube violations**: `static {}` initializer blocks in `XMLReader` and `XMLWriter` built lookup maps (drop-rule mappings, queue-put strategy mappings, PS strategy mappings, etc.) from scratch in each class, producing flagged complexity and duplication.
- **Tight coupling**: classes outside the XML package had to import `XMLReader` or `XMLWriter` merely to access a constant.

### 6.2 Changes

`XMLConstantNames` was significantly expanded from ~200 lines to ~470 lines. All constants previously defined as fields in `XMLReader` or `XMLWriter` were migrated here, and new ones introduced by the refactored mappers were added at the same time.

An inner class `XMLConstantNames.Maps` was introduced to host the immutable lookup tables that were previously built in `static {}` blocks:

```java
public interface XMLConstantNames {
    // ... string constants ...

    final class Maps {
        public static final Map<String, String> DROP_RULES;
        public static final Map<String, String> DROP_RULES_REVERSE;
        public static final Map<String, String> QUEUE_PUT_MAPPING;
        public static final Map<String, String> PRIORITY_QUEUE_PUT_MAPPING;
        public static final Map<String, String> PS_STRATEGIES_MAPPING;
        public static final Map<String, String> PRIORITY_PS_STRATEGIES_MAPPING;
        public static final Map<String, String> POLLING_TYPE_MAPPING;

        static { /* unmodifiableMap initializations */ }
    }
}
```

All maps are built once as `Collections.unmodifiableMap(...)` and referenced by both the reader-side (mappers) and writer-side (serializer) code, eliminating the duplication entirely.

### 6.3 Impact

Any class that implements (or `extends`) `XMLConstantNames` now automatically inherits all constants and has access to `XMLConstantNames.Maps.*` without additional imports. The interface is implemented by `ModelElementMapper`, `XmlSerializer`, `XMLSimReader`, `XMLSimWriter`, `XMLLoader`, and all three entry-point pairs in the `gui/` and `results/` subpackages.

---

## 7. SonarQube Fixes and Call-Site Migrations

### 7.1 Static Mutable State Elimination

The most significant SonarQube issue in the legacy code was the use of `static` mutable fields in `XMLReader` to carry parse state across method calls:

```java
// Legacy XMLReader — non-reentrant, SonarQube flagged
protected static TreeMap<String, Object> classes;
protected static TreeMap<String, Object> stations;
protected static HashMap<Object, String> refStations;
protected static HashMap<Object[], Map<String, Double>> empiricalRouting;
// ... and ~10 more static maps
```

These fields were re-initialized at the start of each `parseXML` invocation, making the class non-reentrant and the state lifecycle opaque. In the refactored code, all this state is encapsulated in `MappingContext`, an instance created per pipeline execution and passed explicitly to each mapper stage (see §3.4.2). The static fields are gone entirely.

### 7.2 Lookup Map Consolidation

`XMLReader` and `XMLWriter` each contained `static {}` blocks initialising mapping tables (drop rules, queue strategies, PS strategies, routing suffixes, etc.) from scratch. These were consolidated into `XMLConstantNames.Maps` (see §6.2), eliminating duplication and the SonarQube `S3010`/`S1192` violations.

### 7.3 Null-Check Ordering

`CommonModel` contained a potential `NullPointerException` in a reference-station type check:

```java
// Before — NPE if getStationType returns null
getStationType(refStation).equals(STATION_TYPE_SOURCE)

// After — null-safe
STATION_TYPE_SOURCE.equals(getStationType(refStation))
```

### 7.4 Call-Site Migrations

All production callers of the deprecated `XMLReader`, `XMLWriter`, and their related classes were updated to use the new pipeline. The following files were migrated:

| File | Change |
|---|---|
| `PADispatcherThread` | `XMLWriter.writeXML(...)` → `XMLSimWriter.writeXML(...)` ; `XMLReader.loadXML(...)` → `XMLLoader.loadXML(...)` |
| `ModelLoader` | `XMLReader.loadModel(...)` → `XMLSimReader.loadModel(...)` ; `XMLReader.loadXML(...)` → `XMLLoader.loadXML(...)` |
| `Mediator` | `XMLWriter.writeXML(...)` → `XMLSimWriter.writeXML(...)` |
| `JSIMWizMain` | `XMLWriter.writeXML(...)` → `XMLSimWriter.writeXML(...)` |
| `AnimationPanel` | `XMLWriter.writeXML(...)` → `XMLSimWriter.writeXML(...)` |
| `DispatcherJMVAschema` | `XMLWriter.getDocument(...)` + `XMLWriter.writeXML(...)` → `XMLSimWriter` equivalents |

`XMLArchiver` itself was updated to use `XMLLoader` (instead of `XMLReader.loadXML`), `XMLSimReader.parseXML` (instead of `XMLReader.parseXML`), `XMLSimWriter.getDocument` (instead of `XMLWriter.getDocument`), and the new `gui/GuiXMLReader`, `gui/GuiXMLWriter`, `results/XMLResultsReader`, `results/XMLResultsWriter` (instead of their deprecated root-package equivalents).

### 7.5 Import Organization

Several files had their import blocks reorganized (JDK standard library imports grouped separately from project imports, wildcard imports replaced with explicit ones) to comply with SonarQube `S1128` and code-style checks. This affects `PADispatcherThread`, `ModelLoader`, `Mediator`, `JSIMWizMain`, `AnimationPanel`, and `DispatcherJMVAschema`.

---

## 8. Legacy Compatibility

### 8.1 Deprecated Stubs

The following classes in the root `jmt.gui.common.xml` package are retained as `@Deprecated` stubs. They preserve their original implementations and are not delegating wrappers — they exist to prevent compilation failures in any external code that still references the old fully-qualified names:

| Deprecated class | Replaced by |
|---|---|
| `XMLReader` | `XMLSimReader` (parsing) + `XMLLoader` (I/O) |
| `XMLWriter` | `XMLSimWriter` |
| `GuiXMLReader` (root package) | `gui/GuiXMLReader` |
| `GuiXMLWriter` (root package) | `gui/GuiXMLWriter` |
| `XMLResultsReader` (root package) | `results/XMLResultsReader` |
| `XMLResultsWriter` (root package) | `results/XMLResultsWriter` |

All are annotated `@Deprecated` and carry a Javadoc `@deprecated` tag pointing to the replacement class.

### 8.2 `LegacyXMLArchiver`

`LegacyXMLArchiver` is a dedicated `@Deprecated` class that replicates the pre-refactoring `XMLArchiver` behaviour: it uses `XMLReader.loadXML`, `XMLReader.parseXML`, `XMLWriter.getDocument`, and the deprecated root-package `GuiXMLReader`, `GuiXMLWriter`, `XMLResultsReader`, `XMLResultsWriter`. Its primary purpose is to provide a clean interface for cross-verification tests that need to compare legacy and refactored output side by side (see §9). It must not be used in production code; use `XMLArchiver` instead.

---

## 9. Test Suite

### 9.1 Testing Approach

**`XMLSimReaderTest`** follows a branch-coverage strategy. Each test method targets a single, narrowly scoped behaviour of `XMLSimReader.parseXML` using an inline XML fragment or a dedicated resource file. Method names encode the behaviour under test (e.g., `test_parseJoin_branches_fullcoverage`, `test_parseImpatience_branches_fullcoverage`). This design makes regressions immediately identifiable — a failing test names the exact feature that broke.

**`XMLSimWriterTest`** uses a round-trip pattern. A `CommonModel` is loaded from a reference archive, written back to XML via `XMLSimWriter`, then re-loaded into a second model. Instead of a raw XML diff, both models are converted to `SimDescriptor` DTOs via `SimDescriptorMapper` and compared with `usingRecursiveComparison()`, verifying semantic equivalence while tolerating format-level noise.

**`XMLResultsReaderTest`** follows the same branch-coverage strategy as `XMLSimReaderTest`. Each test targets a single parsing path of `XMLResultsReader.parseXML` (DOM → DTO → model), covering both the standard `StoredResultsModel` and the `PAResultsModel` parametric analysis format.

**`XMLResultsWriterTest`** uses the same round-trip pattern as `XMLSimWriterTest`. A results model (standard or parametric) is written via `XMLResultsWriter`, reloaded, and the two `ResultsDescriptor` DTOs are compared recursively, verifying that all statistical fields survive the write–reload cycle without loss.

**`GuiXMLReaderTest`** follows the same branch-coverage strategy. Each test targets a single parsing path of `GuiXMLReader.parseXML`, covering the four `<jmodel>` sub-sections: class colours, station canvas positions, Bézier connection shapes, and parametric-analysis configuration.

**`GuiXMLWriterTest`** uses the same round-trip pattern. A `JSimGraphModel` is saved via `GuiXMLWriter`, reloaded, and the two `GuiDescriptor` DTOs are compared recursively, confirming that all visual metadata (colours, positions, connection paths, parametric analysis state) is preserved faithfully.

**`XMLArchiverTest`** is a systematic cross-pipeline equivalence suite. It dynamically discovers every `.xml` file in `src/test/resources/jmt/gui/common/xml/` and runs one parameterized test case per file. Both pipelines (`LegacyXMLArchiver` and `XMLArchiver`) load each archive independently, and three recursive assertions are performed via DTOs:

1. **Sim model** — both models are mapped to `SimDescriptor` DTOs via `SimDescriptorMapper` and compared field-by-field with `usingRecursiveComparison()`, ignoring purely dynamic metadata (`archiveName`, `timestamp`, `name`) and applying the same `alignZeroStrategies` alignment used in `XMLSimWriterTest` to tolerate legitimate ghost-data differences inherited from the legacy format.
2. **GUI metadata** — both models are mapped to `GuiDescriptor` DTOs via `GuiDescriptorMapper` and compared recursively; for `JSimGraphModel` instances this additionally covers class colours, station canvas positions, and connection routing paths.
3. **Simulation results** — the `containsSimulationResults()` flag is asserted equal between pipelines; when both models carry embedded results the `SimulationResultsModel` objects are compared recursively, ignoring runtime-only listener and progress-hook fields.

**`XMLArchiverModelRecursiveParityTest`** is a direct-object recursive parity suite that targets the JSIM graphical model corpus. It dynamically discovers every `.jsimg` file in `src/test/resources/linetesting/input/jsim/` and runs one parameterized test case per file. Both pipelines load each archive into independent `JSimGraphModel` instances, and three recursive assertions are performed directly on the live model objects — rather than on DTOs:

1. **CommonModel** — both models are compared field-by-field with `usingRecursiveComparison()`, verifying that all simulation-model state (classes, stations, connections, measures, blocking regions, preloads) is identical between the two load paths.
2. **GUI model** — the full `JSimGraphModel` object graph (which additionally carries class colours, canvas positions, and Bézier connection shapes) is compared recursively, confirming that visual metadata is preserved faithfully.
3. **Simulation results** — the `containsSimulationResults()` flag is checked first; when both models carry embedded results the `MeasureDefinition` objects are compared recursively, ignoring runtime-only log fields.

All three comparisons exclude fields matching `.*[Ll]istener.*`, `.*[Pp]rogress.*`, and `.*[Tt]imestamp.*` (or `.*[Ll]og.*` for results), which carry volatile runtime state irrelevant to semantic equivalence. The key distinction from `XMLArchiverTest` is the comparison strategy: `XMLArchiverTest` converts both models to DTOs before comparing; this suite compares live model objects directly, catching discrepancies that DTO serialisation might silently absorb. The two suites are therefore complementary: any deviation between the legacy and refactored load paths will be caught by at least one of them.

---

### 9.2 Linetesting Integration

In addition to the XML-pipeline suites described above, the pre-existing `linetesting.IntegrationTest` suite was used to validate the refactoring from an end-to-end simulation perspective. This suite, which predates this refactoring work, exercises the full JMT engine pipeline: it loads model files (`.jsimg`, `.jmva`, `.jsimw`, `.jaba`) from `src/test/resources/linetesting/input/`, runs the simulation through the engine under a 5-minute timeout, and asserts that the numerical results match expected reference outputs. It also measures per-test JaCoCo line and branch coverage against the production class files in `target/classes/`, emitting one CSV row per test run to a structured coverage report.

The `.jsimg` files under `src/test/resources/linetesting/input/jsim/` serve a dual purpose: they are the same corpus consumed by `XMLArchiverModelRecursiveParityTest` (see §9.1) and the model files loaded by `IntegrationTest.testJSIMGraph`. Running both suites over the same corpus provides two orthogonal views of correctness — the parity test confirms that the XML load pipeline produces identical model state between the legacy and refactored paths; the integration test confirms that the engine produces numerically correct simulation output when driven by models loaded through the refactored pipeline.
