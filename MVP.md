# SopTNTRun MVP

## Goal

Build the first production-viable version of `SopTNTRun` with:

- multi-arena support
- multi-floor TNTRun gameplay
- `SopParty` integration
- one mandatory power-up: feather

## MVP Features

### 1. Arena Editor

- create arena
- edit arena
- save arena
- set gameplay bounds
- set lobby bounds
- set lobby spawn
- set spectator spawn
- set end spawn
- set lose-y
- add/remove floors
- define floor materials
- define floor destroy delay
- set player spawns

### 2. Match Flow

- join arena
- random join
- leave arena
- waiting lobby
- countdown
- start match
- run match
- spectator on elimination
- winner detection
- reset arena

### 3. SopParty Integration

- if solo, normal join
- if party leader joins, whole party follows
- only join arenas with enough space for whole party
- fail cleanly if no arena can fit the party

### 4. TNTRun Core

- detect valid block under player
- schedule block disappearance
- remove triggered block
- remove block directly below it
- support multiple floors
- eliminate only below lose-y

### 5. Power-ups

- random feather spawns in arena
- pickup grants one high double jump
- charge is consumed on use

### 6. Stats

- played
- wins
- losses
- falls
- powerups picked
- jumps used

## Post-MVP

- dash power-up
- knockback snowball power-up
- better spectator tools
- scoreboards
- placeholders
- cosmetics
