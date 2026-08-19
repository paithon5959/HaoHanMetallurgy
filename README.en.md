<div align="center">

<img src="image.png" alt="HaoHan Metallurgy banner" width="100%">

# HaoHan Metallurgy

A custom metallurgy plugin for HaoHan SMP, built around the Ancient Forge system.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-API-222222?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io/)
[![Purpur](https://img.shields.io/badge/Purpur-Compatible-8A4FFF?style=for-the-badge)](https://purpurmc.org/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![Gson](https://img.shields.io/badge/Gson-JSON-2E7D32?style=for-the-badge&logo=google&logoColor=white)](https://github.com/google/gson)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)

Language: [Tiếng Việt](README.md) | English

</div>

## Overview

HaoHan Metallurgy is a Minecraft plugin for HaoHan SMP. It provides a custom metallurgy system that handles machine logic, forge recipes, GUI interactions, custom items, and persistent machine data on the server.

## Tech Stack

| Toolkit | Role |
| --- | --- |
| Paper API | Main server API used for plugin development. |
| Purpur | Recommended server runtime for deployment. |
| Java 21 | Main programming language and runtime. |
| Gradle | Dependency management and `.jar` build pipeline. |
| Gson | JSON handling for recipe and configuration data. |
| JUnit 5 | Unit testing framework. |

## Requirements

- Minecraft server running Paper or Purpur.
- Java 21 or newer.
- No separate Gradle installation is required; the Gradle Wrapper is included.
- Resource pack included if the server uses custom textures/models. Datapack is no longer required.

## Installation

1. Build or download the plugin `.jar` file.
2. Copy the `.jar` file into the server `plugins/` directory.
3. Install the resource pack on the client, or configure the server to prompt players to download it when joining.
4. Restart the server.

On first startup, the plugin creates its configuration file at `plugins/HaoHanMetallurgy/config.yml`.

## Build From Source

Run this command in the plugin project root:

```bash
.\gradlew clean build
```

The built `.jar` file will be generated in the `build/libs/` directory.

For a faster build without running tests:

```bash
.\gradlew clean assemble
```

## Commands

Administrative commands require the `haohansmp.metallurgy.admin` permission. Server operators receive this permission by default.

| Command | Description |
| --- | --- |
| `/metallurgy info` | Displays plugin information. |
| `/metallurgy reload` | Reloads configuration and recipes. |
| `/metallurgy debug` | Toggles debug mode. |
| `/metallurgy list` | Displays the list of managed metallurgy machines. |
| `/metallurgy give <player> <item_id> [amount]` | Gives a custom item to a player. |

Main command aliases: `/met`, `/forge`.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `haohansmp.metallurgy.admin` | OP | Allows access to administrative commands. |
| `haohansmp.metallurgy.use` | All players | Allows interaction with metallurgy machines. |

## Recipe Structure

Metallurgy recipes are stored in `src/main/resources/recipes/`. Each recipe defines its ingredients, output, and Ancient Forge processing parameters.

Reference example available at:

```text
src/main/resources/recipes/example_forge.json
```

### Temperature and Purity

Each recipe specifies two distinct temperature thresholds:

- `min_temperature`: Minimum temperature required for ingredients to start melting and the recipe to run.
- `purification_temperature`: Temperature required for stable impurity removal.
- `underheat_fail_chance`: Slag output chance right at the melting threshold.
- `fail_chance`: Base slag output chance when reaching the refining temperature.

Between `min_temperature` and `purification_temperature`, the slag output chance decreases linearly based on the average temperature of the entire batch. `max_temperature` remains the overheating threshold that ruins the batch.

Special recipes can declare required additives:

```json
"additives": ["QUARTZ", "GLOWSTONE_DUST"],
"additive_amount": 2,
"additive_clean_output_bonus": 0.25
```

Only one type from the list is needed, but in sufficient quantity. `additive_clean_output_bonus: 0.25` adds 25 percentage points to the clean ore output chance. Recipes that do not declare a bonus use `additives.default-clean-output-bonus` in `config.yml`.

Environmental requirements for special alloys:

```json
"requires_cold_quench": true,
"requires_soul_fire": true
```

Cold quench accepts a cold biome or Ice/Packed Ice/Blue Ice near the forge. Soul Fire accepts Soul Fire, Soul Campfire, or Soul Lantern within a two-block radius of the forge.

The forge accepts vanilla fuels along with additional categories such as fresh vegetation, wool/beds, and Nether fire items. `fuel-values`, `fuel-groups`, `temperature.fuel-limits`, and `temperature.ignition-boosts` control burn duration, temperature caps, and initial heat spikes.

### Alloy Progression

| Tier | Alloy | Main Ingredients | Conditions |
| --- | --- | --- | --- |
| 0 | Copper | Raw Copper | 700–800°C |
| 1 | Iron | Raw Iron | 900–1000°C |
| 2 | EmberSteel | 2 Iron + Blaze Powder; Coal in FLUX | 1200–1300°C |
| 3 | Mithril | EmberSteel + Mithril Shard | 1400°C and cold quench |
| 4 | SoulSteel | Mithril + Ghast Tear; Soul Sand/Soil in FLUX | 1600°C and Soul Fire |
| 5 | Netherite | SoulSteel + Netherite Scrap; Gold in FLUX | 2000°C |

When upgrading from older configurations to `config-version: 4`, the plugin creates `config.before-v4.yml` and updates the temperature balancing sections. Default recipes following the old schema are also backed up as `*.json.before-v4.bak` before replacement; custom recipes with different names are left untouched.

## Operational Notes

- The plugin manages recipes, Netherite locks, and progression data automatically; the datapack is only an optional compatibility package for displaying legacy advancements.
- HaoHanMetallurgy depends on `HaoHanItemCore` (version 1.0.0). Install `HaoHanItemCore.jar` before installing this plugin.
- Avoid editing runtime server data directly when changes can be managed from source.
- After updating the plugin or recipe files on a running server, verify the changes with `/metallurgy reload`.
