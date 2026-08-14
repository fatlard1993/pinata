# Pinata

A Fabric mod that adds a configurable Pinata block (visually a rainbow jeb_ sheep) that spills configured loot when hit enough times.

## Features

- **Pinata Block**: Place it and hit it; after a configurable number of hits it breaks and sprays its contents outward as item entities
- **Configurable per-pinata**, via the `/pinata` command:
  - Number of hits required to break (`hits=`)
  - Item spread distance (`spread=`)
  - One or more **content sets**: lists of items/counts that spray out when the pinata breaks, configurable per spawn
  - **Indestructible mode**: the pinata doesn't break permanently; instead it resets after a configurable cooldown, optionally advancing to the next content set each time (in order, or randomized)
- **`/pinata spawn [pos] <config>`**: places a fully configured pinata at the given position (or wherever you're looking), e.g. `hits=5 spread=2.0 indestructible cooldown=60 randomize contents=minecraft:diamond 5, minecraft:gold_ingot 3 | minecraft:emerald 10`
- **`/pinata info <pos>`**: inspects an existing pinata's configuration and remaining state
- Hit/break feedback via particles and sounds, including distinct cooldown feedback for indestructible pinatas

## Requirements

- Targets the Minecraft, Fabric Loader, and Fabric API versions declared in this mod's `gradle.properties`. Check there for the exact currently-supported version
- Java version as declared in `fabric.mod.json`'s `depends` block

This mod ships its own client-side rendering code (a real client entrypoint with a custom `BlockEntityRenderer`) for the pinata's 3D model (the rainbow jeb_ sheep look) rather than going through Pandorical. This is a deliberate exception to the suite's usual Pandorical-first approach, since Pandorical does not currently support custom entity/block-entity models.

## Installation

Install alongside its declared dependencies (see `fabric.mod.json`). Since this mod's client rendering code ships in its own jar (not via Pandorical), connecting clients need this mod installed to see the pinata rendered correctly.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
