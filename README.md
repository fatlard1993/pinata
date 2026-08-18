# Pinata

A server-side Fabric mod that adds a configurable Pinata block (visually a rainbow jeb_ sheep) that spills configured loot when hit enough times.

## Getting One

There is no recipe. An operator can place a fully configured pinata with `/pinata spawn`, which is the way to make one worth queueing up for.

For everyone else, with [village-quests](https://github.com/justfatlard/village-quests) installed, any villager who knows you will sell one for 8 emeralds. A hand-placed pinata carries a modest default of sweets, because a pinata that breaks open onto an empty floor is a sad thing.

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

## How the visual works

The pinata block anchors a real, decorative sheep entity (NoAI, silent, persistent, custom-named `jeb_` with the name hidden). Every client, including completely vanilla ones, renders the rainbow wool cycle natively via the built-in jeb_ easter egg; no custom rendering code exists anywhere. Hits on the sheep are cancelled and routed to the pinata's hit logic, with the vanilla hurt flash as feedback. While an indestructible pinata is on cooldown, the sheep is shown sheared; the wool "regrows" when the cooldown ends.

## Pandorical

Pinata runs server-side, and Pandorical is a hard dependency (`fabric.mod.json`): the server will not load this mod without it. The pinata block and item are registered through Pandorical's content sync, along with their assets.

Clients are the optional half, and only for the block itself. The rainbow sheep needs nothing: it is a real vanilla sheep named `jeb_`, so every client already renders the wool cycle natively.

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).

## License

MIT, see [LICENSE](LICENSE).
