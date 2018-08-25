# FastLeavesDecay
Speed up leaves decay in Minecraft

## Purpose
Leaves in Minecraft will decay when they are not connected to a log block directly or via other leaves by a distance of 6 blocks or less. The game will udpate their distance very quickly, but only gradually and very slowly arrange for their decay. This is where the **FastLeavesDecay** plugin comes in. It will realize whenever a leaves block gets disconnected from any nearby logs keeping it alive, and schedule a prompt decay. A variety of configuration options are included.

## Features
FastLeavesDecay works automatically on all leaves which are not player placed and does not require any permissions or configuration, although configuration is provided, see below.
- Decay leaves quickly after the tree stem was cut.
- Decay leaves quickly when natural decay happens.
- Player placed leaves (with the `persistent` flag) are never decayed.
- Limit decay to certain worlds or blacklist worlds in the configuration.
- Play a sound and animation for better player feedback. This can be disabled in the config.

## Installation
- Drop the `FastLeavesDecay.jar` into your `plugins` folder and restart the server or load the plugin manually.
- *(Optional)* Edit the `plugins/FastLeavesDecay/config.yml` file and restart the server or reload the plugin manually.

## Configuraton
Many features of FastLeavesDecay can be configured in the `config.yml` file. To apply your changes, either restart your server or reload the plugin manually. To revert to the plugin defaults, delete the file and a new one will be created next time the plugin gets loaded.
```
# Leave this string list empty to enable all worlds.
OnlyInWorlds: []
# List worlds you wish to exclude from sped up leaves decay.
ExcludeWorlds: []
# Delay in ticks to check around a broken blocks.
# Must be at least 5 to guarantee proper function.
BreakDelay: 5
# Delay in ticks to check around decaying leaves.
DecayDelay: 2
# Play additional effects
SpawnParticles: true
PlaySound: true
```