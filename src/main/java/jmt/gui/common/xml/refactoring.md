# XML Serialization Architecture — Refactoring Documentation

## 1. Legacy Implementation: Critique

`OLDXMLReader` and `OLDXMLWriter` represent a classic example of procedural God-class design applied to a domain that fundamentally requires structured decomposition. Both classes exceed 2,700 lines of code and mix concerns indiscriminately: XML traversal, DOM manipulation, domain-object construction, strategy resolution, and model mutation are all co-located in monolithic static methods. Shared mutable state is managed through a set of `static` fields (e.g., `classes`, `stations`, `refStations`, `empiricalRouting`) that are re-initialized at the beginning of each parse cycle, making the code inherently non-reentrant and brittle under concurrent access. The writer compounds this with a proliferation of ad-hoc inner classes (`ForkStrategyWriter`, `JoinStrategyWriter`, `SemaphoreStrategyWriter`, etc.) that duplicate structural boilerplate and resist independent testing. The net result is a codebase with extremely high coupling, near-zero cohesion at the method level, no clear extension points, and a maintenance surface that scales poorly with the addition of new station types or strategies.

---

## 2. The Two-Phase Transition

The refactoring replaces the direct XML-to-model coupling with a clean two-phase pipeline that introduces an intermediate representation layer.

### Phase 1 — Parsing: `XML → DTO`

The first phase is a pure structural transformation. An XML document (or DOM subtree) is traversed and its data is transcribed into a tree of plain Data Transfer Objects (DTOs): `SimDescriptor` as the root, with nested `NodeDescriptor`, `SectionDescriptor`, `ParameterDescriptor`, `UserClassDescriptor`, `MeasureDescriptor`, `ConnectionDescriptor`, and `BlockingRegionDescriptor`. This phase is entirely decoupled from `CommonModel`. Parsers have no knowledge of domain logic, strategies, or model keys — they only read XML structure and populate fields. The output is a fully self-contained, serializable snapshot of the XML document.

### Phase 2 — Mapping: `DTO → CommonModel`

The second phase interprets the DTO tree and populates the `CommonModel`. This is where domain logic lives: station type resolution, strategy instantiation, key management, and cross-reference post-processing. Mappers operate on the already-validated DTO, never touching the DOM directly.

The same two phases run in reverse for the writing direction (`CommonModel → DTO → XML`), making the architecture fully bidirectional.

This separation brings three immediate benefits:

- **Testability**: parsers and mappers can be unit-tested independently, without a real XML file or a real `CommonModel`.
- **Replaceability**: the serialization format (XML, JSON, binary) can be changed by replacing only the parser/serializer layer, with no impact on mappers.
- **Auditability**: the intermediate DTO provides a stable, inspectable snapshot of the model state at the boundary between the two phases.

---

## 3. New Architecture

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

The `XmlSerializer` is invoked exclusively by `XMLWriter.getDocument()`, which acts as the integration point between the mapper and serializer layers:

```java
// XMLWriter.getDocument()
SimDescriptorMapper mapper = new SimDescriptorMapper(model);
SimDescriptor dto = mapper.mapToDto();         // Mapper layer
return new XmlSerializer().serialize(dto);     // Serializer layer
```

---

### 3.6 Entry Points

| Class | Direction | Responsibility |
|---|---|---|
| `XMLReader` | Read | Loads and validates the XML document, extracts the root element, delegates to `SimParser` and then to `SimDescriptorMapper.mapToModel()` |
| `XMLWriter` | Write | Calls `SimDescriptorMapper.mapToDto()` then `XmlSerializer.serialize()`, then pipes the DOM through a secure `Transformer` |
| `XMLArchiver` | Both | Orchestrates the full archive lifecycle: combines the sim document (from `XMLWriter`) with the GUI document (from `GuiXMLWriter`) and the results document (from `XMLResultsWriter`) into a single archive root element |

`XMLArchiver` is not refactored in this phase and continues to delegate to `XMLReader`, `XMLWriter`, `GuiXMLReader`/`GuiXMLWriter`, and `XMLResultsReader`/`XMLResultsWriter` as before. The refactoring of the GUI and results layers follows the same architectural pattern and is described in subsequent documentation.

---

### 3.7 Package Structure

```
jmt.gui.common.xml
├── XMLReader.java                  Entry point (read)
├── XMLWriter.java                  Entry point (write)
├── XMLArchiver.java                Archive orchestration
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
│   ├── XmlEntityParser.java        Interface
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