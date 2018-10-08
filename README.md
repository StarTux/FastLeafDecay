# FastLeafDecay
Speed up leaf decay in Minecraft

![Leaves decaying instantaneously](https://i.imgur.com/d56fuXl.gif)

## Purpose
Leaves in Minecraft will decay when they are not connected to a log block directly or via other leaves by a distance of 6 blocks or less. The game will udpate their distance very quickly, but only gradually and very slowly arrange for their decay. This is where the **FastLeafDecay** plugin comes in. It will realize whenever a leaves block gets disconnected from any nearby logs keeping it alive, and schedule a prompt decay. A variety of configuration options are included.

## Features
FastLeafDecay works automatically on all leaves which are not player placed and does not require any permissions or configuration, although configuration is provided, see below.
- Leaves decay quickly after the tree stem was cut.
- Leaves decay quickly around natural decay events.
- Natural drops, such as saplings and apples, are unaffected.
- Player placed leaves (with the `persistent` flag) are unaffected.
- Limit decay to certain worlds or blacklist worlds in the configuration.
- Play a sound and animation for better player feedback. This can be disabled in the config.
- Devs: This plugin calls `LeavesDecayEvent` before taking action and respects its cancellation state.

## Compatibility
FastLeafDecay uses Spigot's modern `BlockData` and `Tag` frameworks and will therefore only work on Bukkit or Spigot **1.13** or above.
Builds compatible with Bukkit **1.12** or **1.8** can be found in the *Links* section below.

## Installation
- Drop the `FastLeafDecay.jar` into your `plugins` folder and restart the server or load the plugin manually.
- *(Optional)* Edit the `plugins/FastLeafDecay/config.yml` file and restart the server or reload the plugin manually.

## Configuraton
Many features of FastLeafDecay can be configured in the `config.yml` file. To apply your changes, either restart your server or reload the plugin manually. To revert to the plugin defaults, delete the file and a new one will be created next time the plugin gets loaded.
```
# Leave this string list empty to enable all worlds.
OnlyInWorlds: []
# List worlds you wish to exclude from sped up leaves decay.
ExcludeWorlds: []
# Delay in ticks to check around broken blocks.
# Must be at least 5 to guarantee proper function.
BreakDelay: 5
# Delay in ticks to check around decaying leaves.
DecayDelay: 2
# Play additional effects
SpawnParticles: true
PlaySound: true
```

## Links
- [Source code](https://github.com/StarTux/FastLeafDecay) on Github
- [BukkitDev plugin page](https://dev.bukkit.org/projects/fastleafdecay)
- [SpigotMC resource page](https://www.spigotmc.org/resources/fastleafdecay.60237/)
- [1.12 compatibility version](https://github.com/StarTux/FastLeafDecay/releases/tag/1.12-compat-1.0)
- [1.8 compatibility version](https://github.com/StarTux/FastLeafDecay/releases/tag/1.8-compat-1.0)
