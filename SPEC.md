# SopTNTRun Specification

## Overview

`SopTNTRun` is a fast arena minigame plugin for Paper/Spigot servers.

Players join a prepared arena, wait in a lobby, start together, and run across
multiple gameplay floors. When a player steps on a valid floor block, the
block disappears after a short delay. If the player falls through a hole, they
continue on the floor below. If they fall below the configured lose level,
they are eliminated and become a spectator. The last living player wins.

The plugin must support:

- multiple arenas
- multiple gameplay floors per arena
- thick floors
- delayed block disappearance
- random power-ups
- mandatory `SopParty` integration
- arena reset after each match
- player statistics

The plugin should fit the `Sop*` ecosystem style:

- clean config-driven behavior
- predictable admin commands
- warm but non-spammy messages
- modular runtime/state design

## Core Gameplay Model

### Match States

Each arena can be in one of the following states:

- `EDITING`
- `WAITING`
- `STARTING`
- `RUNNING`
- `ENDING`
- `RESETTING`
- `DISABLED`

### Basic Win Condition

- Players start alive.
- While alive, they run across destructible floor blocks.
- When an alive player falls below `lose-y`, they lose and become spectator.
- The last alive player wins.

### Multi-floor Arena Model

An arena contains:

- one waiting lobby area
- one gameplay area
- one or more gameplay floors
- one spectator spawn
- one post-game spawn
- one lose level

### Floors

Each floor is defined logically, not by hardcoded height assumptions.

Required floor properties:

- `id`
- `top-y`
- `materials`
- `destroy-delay-ticks`

Optional floor properties:

- `name`
- `enabled`

The arena must support multiple floors stacked vertically.

### Floor Thickness

We will intentionally mirror the simple upstream behavior:

- when a player steps on a valid floor block, the plugin removes:
  - the detected top block
  - the block directly below it

This means one gameplay floor can have thickness greater than one block.

There is no separate admin-facing `destroy-depth` setting in v1.
`SopTNTRun` should simply remove two layers per triggered block, just like the
upstream behavior the user wants to preserve.

### Lose Level

The arena has a single `lose-y`.

- If a living player falls below `lose-y`, they are eliminated.
- Falling between floors is allowed.
- Falling below the final threshold is the only fall-based elimination rule.

## Arena Editing

### Arena Creation

Command:

`/tntrun create <name> <mode> <minPlayers> <maxPlayers>`

Behavior:

- creates arena file
- enters edit mode
- registers arena as `EDITING`

### Mode

`mode` is a free-form arena tag used for matchmaking only.

Examples:

- `solo`
- `duo`
- `1x1`
- `draka`
- `fast`

Modes are not preset logic packages.
They are labels used by `/tntrun random`.

### Required Arena Points and Areas

Admin must define:

- gameplay area `pos1/pos2`
- lobby area `lobbypos1/lobbypos2`
- lobby spawn
- spectator spawn
- post-game spawn
- one or more player spawns
- one or more floors
- lose level

### Suggested Edit Commands

- `/tntrun edit <arena>`
- `/tntrun save`
- `/tntrun cancel`
- `/tntrun pos1`
- `/tntrun pos2`
- `/tntrun lobbypos1`
- `/tntrun lobbypos2`
- `/tntrun setlobbyspawn`
- `/tntrun setspectator`
- `/tntrun setendspawn`
- `/tntrun setspawn <slot>`
- `/tntrun setlosey`
- `/tntrun floor add <id> <topY>`
- `/tntrun floor remove <id>`
- `/tntrun floor setmaterials <id>`
- `/tntrun floor setdelay <id> <ticks>`

### Floor Material Selection

For each floor, admin must be able to define destructible materials.

Expected use case:

- upper block is `sand`, `red_sand`, `concrete_powder`, etc.
- lower support may be `tnt`, `bedrock`, or another block

The plugin must remove both blocks regardless of whether the lower block is
also listed as destructible material.

## Matchmaking

### Join Commands

- `/tntrun join <arena>`
- `/tntrun random`
- `/tntrun random <mode[,mode...]>`
- `/tntrun leave`

### Join Logic

`/tntrun join <arena>`

- joins that exact arena if it is available and has room

`/tntrun random`

- prefer waiting matches with room
- otherwise choose a random empty arena from any mode

`/tntrun random <mode[,mode...]>`

- prefer waiting matches with room among those modes
- otherwise choose a random empty arena among those modes

### SopParty Integration

`SopParty` support is mandatory.

Expected behavior:

- if a solo player joins, normal join flow applies
- if a party leader joins, the full party follows
- the party must be placed into the same arena together
- if a waiting arena has enough room for the full party, use it
- if not, try another arena
- if no arena has enough room for the full party, deny join

TNTRun does not need team logic in v1.
Party is only used for grouped entry into the same match.

### Recommended Integration Shape

`SopTNTRun` should depend on `SopParty` via service/API integration, not direct
hardcoded plugin assumptions where possible.

## Waiting Phase

### Waiting Lobby

Players who joined a match are teleported to the arena lobby.

In `WAITING` or `STARTING`:

- players are invulnerable
- players cannot escape the lobby area
- if a player leaves the lobby bounds, they are teleported back

### Autostart

Arena has:

- `min-players`
- `max-players`
- `countdown-seconds`

If `min-players` is met:

- countdown begins

If player count drops below `min-players` before game start:

- countdown is canceled

Once players are teleported into the match start positions, the game is
considered started.

## Running Match

### Player Spawns

For v1, the arena uses a flat list of player spawn points.

- each joined player gets one spawn slot
- no team grouping in TNTRun v1

### Block Disappearance

When a living player moves over a valid floor block:

- determine the active block under the player
- schedule destruction after floor delay
- remove the detected block
- remove the block directly below it

Important:

- blocks should not vanish instantly
- destruction must be delayed a small configurable number of ticks
- already scheduled blocks should not be scheduled again

### Trigger Detection

The trigger system should:

- scan downward from player feet
- find the first matching floor block
- ignore air and light blocks
- respect player movement and multi-floor gameplay

### Elimination

An alive player is eliminated when:

- they fall below `lose-y`
- they disconnect during `RUNNING`

After elimination:

- player becomes spectator
- stats record a loss
- winner check is run

### Spectators

Spectators:

- can fly
- cannot interfere
- stay inside gameplay area bounds
- are teleported back if they leave arena bounds

## Power-ups

### Power-up Philosophy

Power-ups should be simple, readable, and arena-safe.
No item should turn the mode into chaotic full PvP spam.

### Mandatory Power-up: Feather

Random feather spawn is required.

Behavior:

- a feather appears randomly on the arena
- player picks it up
- player receives one charged high double jump
- after one use, charge is consumed

Requirements:

- visible particles
- pickup sound
- short status message
- configurable boost strength
- configurable spawn interval
- configurable despawn time
- configurable maximum simultaneously active feathers

Implementation note:

- feather should spawn as a world item entity
- the charge should be stored in runtime player state, not as permanent flight

### Additional MVP Power-ups

Two extra power-ups should be designed into the system from the start:

- `Dash`
  - short forward burst
  - one-time use
- `Knockback Snowball`
  - gives one snowball with configured knockback effect

These should share a common power-up framework.

### Power-up Spawn Rules

Power-ups spawn only while a match is `RUNNING`.

Expected config:

- enabled power-up types
- per-type spawn chance or interval
- max active per arena
- despawn time

## Commands During Match

By default, commands during `RUNNING` should be blocked except for an allowlist.

Expected config:

- `allowed-commands`

Examples:

- `msg`
- `r`
- `tell`
- `tntrun leave`

Everything else is denied while alive in a running match.

Spectators may either:

- use the same allowlist
- or have a separate spectator allowlist later

v1 can use one common allowlist.

## Chat

Match chat should be isolated.

Suggested rules:

- waiting players see only waiting players in the same arena
- alive players in a running match see only that match
- spectators see match chat too
- outside players do not see match chat

No team chat is needed in v1.

## Reset and Regeneration

### Required Behavior

After a match ends:

- all removed floor blocks must be restored
- scheduled destruction state must be cleared
- temporary power-ups must be cleared
- players must be removed from runtime match state
- arena returns to `WAITING`

### Reset Strategy

For v1, we do not need a giant full-world snapshot system.

We only need to restore:

- removed floor blocks
- temporary entities created by the plugin
- temporary power-up items

This is enough because TNTRun gameplay mostly modifies arena floor blocks.

## Statistics

Required v1 stats:

- games played
- wins
- losses
- eliminations
- falls
- powerups picked
- double jumps used

Storage choice can be finalized later, but structure should assume stable
player UUID-based stats.

## Configuration

### Main Config

Main config should hold:

- default countdown
- default block destroy delay
- allowed commands
- power-up global settings
- message defaults
- stats settings

### Arena Files

Each arena file should store:

- name
- mode
- min/max players
- state metadata
- gameplay area
- lobby area
- lobby spawn
- spectator spawn
- post-game spawn override if any
- lose-y
- player spawns
- floors

## UX Notes

### Messages

Messages should be config-driven.
If a message path is empty, the plugin sends nothing.

### Sounds and Visuals

The plugin should use:

- countdown sounds
- pickup sounds
- elimination/win sounds
- feather particles

but all of these should be optional/configurable.

## MVP Scope

The first working version should include:

- arena create/edit/save
- lobby/waiting
- join/random/leave
- `SopParty` grouped join
- multi-floor gameplay
- two-layer destruction behavior
- lose-y elimination
- spectator mode
- winner detection
- arena reset
- feather power-up
- basic stats

Items explicitly not required for v1:

- cosmetics
- queue GUI
- team mode
- ranking system
- SQL-first storage
- cross-server routing

