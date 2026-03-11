# Module Developer Guide

This document describes the internals of the QIT module framework for developers
who need to extend, maintain, or understand the codebase. For a user-facing
overview, see [modules.md](modules.md).

## Architecture Overview

The module framework has four layers:

1. **Annotations** (`qit.base.annot.*`) — declarative metadata on classes and fields
2. **Module interface** (`qit.base.Module`) — the contract every module implements
3. **Reflection engine** (`qit.base.utils.ModuleUtils`) — discovers modules, extracts field metadata, validates, serializes
4. **CLI binding** (`qit.base.cli.*`) — wraps modules for command-line execution with auto-generated help

```
                 ┌──────────────┐
                 │   QitMain    │  entry point, module discovery
                 └──────┬───────┘
                        │
                 ┌──────▼───────┐
                 │  CliModule   │  CLI wrapper, arg parsing, I/O
                 └──────┬───────┘
                        │
                 ┌──────▼───────┐
                 │ ModuleUtils  │  reflection, validation, field access
                 └──────┬───────┘
                        │
              ┌─────────▼─────────┐
              │  Module + Annots  │  user-written module code
              └───────────────────┘
```

## Design Philosophy

QIT modules are **pure data transforms**: they read from input fields, compute a
result, and write to output fields. A module should not manage file discovery,
iterate over subjects, handle dependencies between processing steps, or
implement retry logic. These concerns belong to external orchestration tools.

This is a deliberate design choice. By keeping modules stateless and
single-purpose, they remain composable across many different execution contexts:

- **Command line** — chain modules with shell scripts or Makefiles
- **LONI Pipeline** — QIT modules integrate directly with the
  [LONI Pipeline](http://pipeline.loni.usc.edu) workflow engine, which handles
  execution graphs, parallelism, and provenance tracking
- **Workflow managers** — tools like Nextflow, Snakemake, or Nipype can invoke
  `qit <Module> ...` as a process step, managing dependencies and scheduling
- **qitview** — the interactive viewer wraps modules in a GUI, letting users
  apply transforms to loaded data without writing scripts

Workflow orchestration tools evolve independently and have their own strengths
(DAG scheduling, cluster support, checkpointing, provenance). By not
reimplementing these capabilities inside QIT, modules stay simple, testable, and
durable — they will work with whatever orchestration tools exist today or in the
future.

**Rule of thumb:** if you find yourself adding loops over subjects, file
globbing, or conditional branching inside a module, that logic should live in an
external script or workflow definition instead.

## Annotations Reference

All annotations are in `qit/base/annot/`. All use `@Retention(RUNTIME)` and
`@Inherited`.

### Class-level annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@ModuleDescription("...")` | Class or Field | Plain-text description shown in help |
| `@ModuleAuthor("...")` | Class | Author attribution |
| `@ModuleCitation("...")` | Class | Publication to cite when using the module |
| `@ModuleUnlisted` | Class | Hides the module from `--list` and help output |

### Field-level annotations

Every annotated field must have exactly one role annotation:

| Annotation | Role | Typical types |
|---|---|---|
| `@ModuleInput` | Data consumed by the module | Dataset subclasses: `Volume`, `Mask`, `Curves`, `Mesh`, `Vects`, `Table`, `Matrix`, `Affine`, `Gradients`, `Deformation`, `Neuron`, `Solids` |
| `@ModuleOutput` | Data produced by the module | Same Dataset subclasses |
| `@ModuleParameter` | Configuration value | `Double`, `Integer`, `String`, `Boolean`, `boolean`, enums |

Field modifiers (combinable with a role annotation):

| Annotation | Effect |
|---|---|
| `@ModuleOptional` | Field may be null; not required on the command line |
| `@ModuleAdvanced` | Grouped separately in help output; not typically modified |
| `@ModuleExpert` | Only visible when `--expert` flag is passed |

### Validation rules

`ModuleUtils.validate()` enforces:

- Each annotated field has exactly one role (`@ModuleInput` XOR `@ModuleParameter` XOR `@ModuleOutput`)
- `@ModuleOutput` fields are not marked `@ModuleAdvanced`
- Input/output field types are valid Dataset subclasses

## Module Interface

```java
public interface Module {
    Module run() throws IOException;
}
```

Modules must:

- Implement a public no-argument constructor
- Declare annotated `public` fields for inputs, parameters, and outputs
- Implement `run()` which reads from input fields, writes to output fields, and returns `this`

The framework populates input and parameter fields before calling `run()`, and
reads output fields after it returns.

## How Module Discovery Works

`QitMain.main()` → `ModuleUtils.list()`:

1. Uses `org.reflections.Reflections` to scan the `"qit"` package at runtime
2. Finds all classes implementing `Module`
3. Filters out inner classes and `@ModuleUnlisted` classes
4. Maps simple class names (e.g. `VolumeThreshold`) to classes
5. Also discovers `CliMain` implementations for batch processing tools

A user invokes a module by name:
```
qit VolumeThreshold --input in.nii.gz --output out.nii.gz
```

`QitMain` looks up `VolumeThreshold`, wraps it in `CliModule`, and calls `run()`.

## CLI Binding Flow

`CliModule` is the adapter between `Module` and the command line:

### 1. Build CLI specification (`cli()` method)

For each field returned by `ModuleUtils.fields(module)`:

- Check `@ModuleExpert` visibility (skip if not in expert mode)
- Determine role from `@ModuleInput` / `@ModuleParameter` / `@ModuleOutput`
- Read `@ModuleDescription` for help text
- Read `@ModuleOptional` / `@ModuleAdvanced` for categorization
- For `boolean` fields: create a flag (no argument value needed)
- For `enum` fields: auto-generate option list from enum constants
- For Dataset fields: use the type name as the argument placeholder
- Build a `CliOption` and add it to `CliSpecification`

### 2. Parse arguments

`CliValues` parses `--key value` pairs and positional arguments from `String[] args`.

### 3. Read inputs (`read()` method)

For each input/parameter field:

- Get the string value from `CliValues`
- For Dataset types: call the type's static `read(filename)` method via reflection
- For primitives: parse from string (`Double.valueOf()`, etc.)
- For enums: `Enum.valueOf()`
- For booleans: presence of the flag means `true`

### 4. Execute

Call `module.run()`.

### 5. Write outputs (`write()` method)

For each `@ModuleOutput` field:

- Get the filename from `CliValues`
- Call the Dataset's `write(filename)` method via reflection

## Writing a New Module

### Minimal example

```java
@ModuleDescription("Scale a volume by a constant factor")
@ModuleAuthor("Your Name")
public class VolumeScale implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleDescription("scale factor")
    public Double factor = 1.0;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    @Override
    public VolumeScale run()
    {
        this.output = this.input.copy();
        // ... scale voxel values by this.factor ...
        return this;
    }
}
```

This automatically provides:

- `qit VolumeScale --help` with formatted help text
- `qit VolumeScale --input in.nii.gz --factor 2.0 --output out.nii.gz`
- Availability in `qitview` module panel
- JSON serialization of parameters via `--save`/`--load`

### Conventions

- Place modules in `qit/data/modules/<category>/` (e.g. `volume/`, `mask/`, `curves/`)
- Class name should be `<DataType><Action>` (e.g. `VolumeThreshold`, `MaskInvert`, `CurvesTransform`)
- Always set default values for `@ModuleParameter` fields
- Mark non-essential inputs with `@ModuleOptional`
- Return `this` from `run()`

### Using @ModuleOptional inputs

```java
@ModuleInput
@ModuleOptional
@ModuleDescription("optional mask to restrict processing")
public Mask mask;

public MyModule run()
{
    for (Sample sample : this.input.getSampling())
    {
        if (this.input.valid(sample, this.mask))  // mask can be null
        {
            // process voxel
        }
    }
    return this;
}
```

### Using enum parameters

```java
public enum InterpolationType { NEAREST, LINEAR, CUBIC }

@ModuleParameter
@ModuleDescription("interpolation method")
public InterpolationType interp = InterpolationType.LINEAR;
```

The CLI will automatically show: `--interp <InterpolationType> (Options: NEAREST, LINEAR, CUBIC) (Default: LINEAR)`

## Batch Processing Tools

For operations across multiple subjects/files, create a `CliMain` implementation
in `qit/main/`:

```java
@ModuleDescription("Run VolumeThreshold on many subjects")
@ModuleAuthor("Your Name")
public class VolumeThresholdBatch implements CliMain
{
    public void execute(CliValues args) throws Exception
    {
        // Custom batch logic using args.keyed("--input"), etc.
    }
}
```

These are discovered alongside Modules and invoked the same way:
`qit VolumeThresholdBatch --help`

## Module Serialization

Modules can be saved and loaded as JSON:

```
qit VolumeThreshold --input in.nii.gz --threshold 0.3 --output out.nii.gz --save params.json
qit VolumeThreshold --load params.json --input other.nii.gz --output other_out.nii.gz
```

`ModuleUtils.write()` serializes annotated field values to JSON.
`ModuleUtils.read()` deserializes and populates fields.

## Key Source Files

| File | Role |
|---|---|
| `qit/base/Module.java` | Module interface (10 lines) |
| `qit/base/annot/*.java` | All annotation definitions |
| `qit/base/utils/ModuleUtils.java` | Reflection engine: discovery, validation, field access, serialization |
| `qit/base/cli/CliModule.java` | CLI adapter: arg parsing, file I/O, help generation |
| `qit/base/cli/CliSpecification.java` | Help text and Markdown generation |
| `qit/base/cli/CliOption.java` | Single CLI option metadata |
| `qit/base/cli/CliValues.java` | Parsed command-line arguments |
| `qit/main/QitMain.java` | Entry point, module discovery and dispatch |
