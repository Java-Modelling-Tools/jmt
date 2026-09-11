---
name: Feature request
about: Propose an enhancement, new modeling capability, or architectural change
title: "[FEATURE] "
labels: enhancement
assignees: ''
---

## Problem

What problem are you trying to solve? Is it related to a limitation you've run into
(e.g. a modeling construct that JSIM/JMVA can't express, a missing routing/service strategy,
a workflow gap in the GUI, a performance bottleneck)? Describe the context.

## Proposed solution

Describe the change you'd like to see. Be as concrete as possible:

- Which tool(s) does this affect? (JSIMwiz / JSIMgraph / JMVA / JMCH / JABA / JWAT / commandline / all)
- Does it require changes to the model format? (`SIMmodeldefinition.xsd`, `JMTmodel.xsd`,
  `JModelGUI.xsd`, or the XSLT converters between them)
- Does it touch the simulation engine (`jmt.commandline`), the analytical solver (`jmt.jmva`),
  or both?
- Is this a GUI-only change (`jmt.gui.common` / a specific tool's GUI package), or does it
  need engine-level support too?

## Alternatives considered

What other approaches did you consider, and why is the proposed solution preferable?
(e.g. extending an existing `NetStrategies`/`NodeSections` class vs. adding a new one,
a wizard-only workaround vs. a schema change, etc.)

## Impact / compatibility

- Does this change the XML schema in a backwards-incompatible way? If so, note whether an
  XSLT migration for existing model files is needed.
- Any expected impact on existing tests (unit tests, `linetesting` integration coverage)?

## Additional context

Mockups, references to related work (e.g. LINE, published algorithms), or links to related issues.
