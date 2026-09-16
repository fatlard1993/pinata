# Pinata

A server-side Fabric mod that adds a configurable Pinata block (visually a rainbow jeb_ sheep) that spills configured loot when hit enough times.

## Parties

There is no recipe. An operator can place a fully configured pinata with `/pinata spawn`, which is the way to make one worth queueing up for.

Otherwise, with [village-quests](https://github.com/fatlard1993/village-quests) installed, a village that thinks well of you (45 reputation) will occasionally offer to put one up. Nothing is charged. Reputation there buys belonging rather than goods, and a village throwing a party for its children is belonging in its plainest form.

It goes up outside, and every child in the village walks over and stands around it until somebody breaks it or the light starts to go. They head home before dusk, because a child who leaves at dusk is a child walking home in the dark. A hand-placed pinata, or one spawned without `contents=`, carries a modest default of sweets (six cookies, four sweet berries, three glow berries and an emerald), because a pinata that breaks open onto an empty floor is a sad thing.

## Features

- **Pinata Block**: Place it and hit it; after a configurable number of hits it breaks and sprays its contents outward as item entities
- **Configurable per-pinata**, via the `/pinata` command:
  - Number of hits required to break (`hits=`, default 5, 1 to 1000)
  - Item spread distance (`spread=`, default 0.5, 0 to 10)
  - One or more **content sets** (`contents=`, last on the line): comma-separated `item count` entries (count 1 to 64, default 1), with `|` between sets; unknown items are skipped
  - **Indestructible mode** (`indestructible`): the pinata doesn't break permanently; instead it resets after a configurable cooldown (`cooldown=`, in ticks, default 0, up to 72000), optionally advancing to the next content set each time (in order, or randomized with `randomize`)
- **`/pinata spawn [pos] <config>`**: places a fully configured pinata at the given position (or against the block you're looking at, within 5 blocks), e.g. `hits=5 spread=2.0 indestructible cooldown=60 randomize contents=minecraft:diamond 5, minecraft:gold_ingot 3 | minecraft:emerald 10`
- **`/pinata info <pos>`**: inspects an existing pinata's configuration and remaining state
- Both commands are op-only
- Hit/break feedback via particles and sounds, including distinct cooldown feedback for indestructible pinatas
- With [block-tip](https://github.com/fatlard1993/block-tip), the card names the pinata rather than the sheep inside it, and says whether an indestructible one is ready to smash or recovering

## How the visual works

The pinata block anchors a real, decorative sheep entity (NoAI, silent, persistent, custom-named `jeb_` with the name hidden). Every client, including completely vanilla ones, renders the rainbow wool cycle natively via the built-in jeb_ easter egg; no custom rendering code exists anywhere. Hits on the sheep are cancelled and routed to the pinata's hit logic, with the vanilla hurt flash as feedback. While an indestructible pinata is on cooldown, the sheep is shown sheared; the wool "regrows" when the cooldown ends.

## Pandorical

Pinata runs server-side, and Pandorical is required: the server will not load this mod without it. The pinata block and item are registered through Pandorical's content sync, along with their assets.

Clients are the optional half, and only for the block itself. The rainbow sheep needs nothing: it is a real vanilla sheep named `jeb_`, so every client already renders the wool cycle natively.

## Development

Installing is in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
