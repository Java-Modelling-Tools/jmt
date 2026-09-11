---
name: Thesis proposal
about: Propose a BSc/MSc thesis project built around JMT
title: "[THESIS] "
labels: academic
assignees: ''
---

## Student info

- **Name**:
- **Degree**: (BSc / MSc)
- **University / department**:
- **Supervisor(s)**:
- **Expected start / end date**:

## Scope

One-paragraph summary of the thesis topic and the research or engineering question it addresses.

## Motivation

Why is this problem interesting or worth solving? Reference any related literature, an
existing JMT limitation, or a gap in current modeling/solving capabilities.

## Proposed technical contribution

Describe what will actually be built or investigated, e.g.:

- A new simulation strategy/station type (`jmt.commandline.NetStrategies` / `NodeSections`)
- A new or extended analytical solver (`jmt.jmva.analytical` / `jmva.engine`)
- A new GUI workflow or editor (`jmt.gui.common`, or a specific tool's package)
- Changes to the model XML format/schema (`src/main/resources/jmt/common/xml`) and any
  required XSLT conversion
- An evaluation/benchmarking study using the existing tools without core changes

## Impacted packages / modules

List the Java packages expected to be touched (add/remove as appropriate):

- [ ] `jmt.commandline` (simulation core: `QueueNet`, `NodeSections`, `NetStrategies`, `simEngine`, `simDispatcher`)
- [ ] `jmt.jmva` (analytical solver: `jmva/analytical`, `jmva/engine`)
- [ ] `jmt.gui.common` (shared GUI/model framework)
- [ ] `jmt.gui.jsimwiz` / `jmt.gui.jsimgraph`
- [ ] `jmt.gui.jmva` / `jmt.gui.jaba` / `jmt.gui.jwat`
- [ ] `jmt.common.xml` / model schemas (`src/main/resources/jmt/common/xml`)
- [ ] `jmt.commandline`
- [ ] Other:

## Timeline

Rough milestone breakdown (adjust to the actual thesis duration):

| Milestone | Target date | Notes |
|---|---|---|
| Literature review / background study | | |
| Design / architecture proposal | | |
| Implementation phase 1 | | |
| Implementation phase 2 | | |
| Evaluation / experiments | | |
| Thesis writing | | |
| Defense | | |

## Evaluation plan

How will success be measured? (e.g. correctness tests against known analytical results,
performance benchmarks, comparison with existing tools such as LINE, user study, etc.)

## Additional context

Any prior discussion, related issues/PRs, or external references (papers, datasets) relevant
to this proposal.
