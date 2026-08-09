# Trace Visualizer

**Status:** Planned

The trace visualizer will turn executable simulator histories into
first-principles learning tools.

## Views

- Node topology and network links.
- Message and timer timeline.
- Selected node state.
- Protocol-specific logs, task tables, deduplication tables, or shard ownership.
- Invariant status at the selected event.

Static Mermaid diagrams will explain fixed structure. Interactive HTML and SVG
will be used only when stepping through changing state materially improves
understanding.

Visual scenarios will be backed by named automated tests so teaching material
does not drift away from implementation behavior.
