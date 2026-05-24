# SopTNTRun

`SopTNTRun` is a Paper mini-game plugin for classic TNT Run style gameplay with:

- multi-arena support
- join blocks and command join flow
- party-aware queueing through `SopParty`
- random arena power-ups
- PlaceholderAPI support
- arena-specific winner fireworks

Players join an arena, wait on live arena spawns, and start running when the countdown ends.  
Blocks under players break with a delay, the last alive player wins, and eliminated players are restored and sent back to the global spawn.

## Features

- Classic TNT Run gameplay with delayed block breaking
- Multiple vertical layers supported naturally by the map itself
- Setup wizard for gameplay area and spawn points through hotbar items
- Join by command or by stepping on a configured join block
- Party join support through `SopParty`
- Power-ups:
  - `Feather`: one high double jump
  - `Dash`: one forward dash
  - `Knockback Snowball`
- Winner phase with flight, celebration position, and fireworks
- Player state restore on leave, finish, and plugin disable
- PlaceholderAPI expansion: `%soptntrun_*%`

## Requirements

- Paper `1.16.5+`
- Java `8+` for the plugin build/runtime target

Optional:

- `PlaceholderAPI`
- `SopParty`

## Commands

### Player

- `/tntrun join <arena>`
- `/tntrun random`
- `/tntrun leave`

### Admin

- `/tntrun create <name> <minPlayers> <maxPlayers>`
- `/tntrun edit <arena>`
- `/tntrun save`
- `/tntrun list`
- `/tntrun reload`
- `/tntrun setglobalspawn`
- `/tntrun pos1`
- `/tntrun pos2`
- `/tntrun setspawn [slot]`
- `/tntrun setjoinblock`
- `/tntrun removejoinblock`

## Permissions

- `soptntrun.player`
- `soptntrun.admin`

## Arena Setup

### 1. Create arena

```text
/tntrun create <name> <minPlayers> <maxPlayers>
```

This puts you into edit mode for that arena.

### 2. Set gameplay area

Use:

```text
/tntrun pos1
```

This starts the area setup wizard.

Hotbar during setup:

- slot `1`: `Set Point`
- slot `2`: `Back`
- slot `3`: `Next`

Set point `1`, then point `2`.

### 3. Set player spawns

Use:

```text
/tntrun setspawn [slot]
```

This starts the spawn setup wizard.

Hotbar during setup:

- slot `1`: `Set Point`
- slot `2`: `Back`
- slot `3`: `Next`

When you set a spawn, the wizard automatically advances to the next slot.

### 4. Set global spawn

```text
/tntrun setglobalspawn
```

Players return here when they:

- leave queue
- get eliminated
- finish a match
- are evacuated on plugin disable

### 5. Set join block

Stand on a block and run:

```text
/tntrun setjoinblock
```

Players can then join this arena by stepping onto that block.

### 6. Save arena

```text
/tntrun save
```

## Power-ups

Power-ups spawn randomly during a running match.

### Feather

- picked up from the arena
- grants one double jump

### Dash

- picked up from the arena
- grants one dash item
- right click to launch forward

### Knockback Snowball

- picked up from the arena
- grants a knockback snowball

## Winner Phase

When one player remains:

- winner enters ending phase
- winner gets forced flight
- winner is moved to the top of the arena zone
- winner is kept inside the arena during celebration
- fireworks spawn around the winner

Firework settings are stored per arena through arena settings:

- `winner-fireworks-interval-ticks`
- `winner-fireworks-radius`

If `winner-fireworks-radius <= 0`, the plugin uses half of the arena size automatically.

## Config Notes

Main config contains defaults such as:

- min/max players
- countdown
- block destroy delay
- power-up timing
- default winner fireworks interval/radius
- `global-spawn`

Each arena stores its own:

- gameplay area
- spawns
- join blocks
- arena settings

## Placeholders

- `%soptntrun_in_game%`
- `%soptntrun_game_status%`
- `%soptntrun_arena%`
- `%soptntrun_mode%`
- `%soptntrun_players_total%`
- `%soptntrun_alive_players%`
- `%soptntrun_countdown%`
- `%soptntrun_min_players%`
- `%soptntrun_max_players%`
- `%soptntrun_stats_games%`
- `%soptntrun_stats_wins%`
- `%soptntrun_stats_losses%`
- `%soptntrun_stats_falls%`
- `%soptntrun_stats_powerups%`
- `%soptntrun_stats_doublejumps%`

## Notes

- Old configs are not guaranteed to stay compatible.
- `SopParty` integration is used only for grouped queueing.
- Winner celebration and power-up logic are arena-runtime features, not lobby features.
