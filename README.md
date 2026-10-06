## BuildCraft Renewed

BuildCraft for Minecraft 26.3, running on **Fabric**, **Forge** and **NeoForge** from one codebase. It is a port of
BuildCraft 7.99 (the Minecraft 1.12.2 version, "BuildCraft 8"), whose code is kept for reference in `legacy/`.

| Loader   | Version                                   |
|----------|-------------------------------------------|
| Fabric   | Loader 0.19.5, Fabric API 0.161.0+26.3    |
| Forge    | 66.0.9                                    |
| NeoForge | 26.3.0.48-beta                            |

Java 25 is needed to build and run it. If it isn't installed, Gradle downloads it for the build.

### What's in it

Everything in BuildCraft 7.99:

* **Core:** wrench, gears, paintbrush, list, map location, goggles, marker connector, volume and path markers, volume
  boxes, engines (redstone and creative), water and oil springs, power tester, fragile fluid shards, debugger and
  decorated blocks.
* **Energy:** Stirling and combustion engines, oil and fuels, and oil wells, spouts and lakes in world generation.
* **Transport:** item, fluid and power pipes of every kind, pipe wires, painting, and pluggables (pipe plugs, power
  adapters, gates, pulsars, sensors, timers, lenses, filters and facades).
* **Factory:** mining well, pump, flood gate, tank, chute, distiller, heat exchanger, auto workbench and water gel.
* **Silicon:** laser, assembly table, advanced crafting table, integration table, chipsets, gate copier, and gates with
  all their triggers and actions.
* **Builders:** quarry, filler (all 19 patterns, which gates can set), filler planner, architect table, builder (which
  can build along a path), electronic library, replacer, templates, blueprints and single schematics. Quarries, fillers
  and builders keep the chunks they work in loaded.
* **Robotics:** the zone planner. (BuildCraft 7.99 had no robots.)
* **Guide book:** describes every block, item, trigger and action, with recipes.

A few things work differently from 7.99:

* The zone planner's map is drawn from above, rather than in 3D.
* Markers are joined by clicking one and then the other with the marker connector, rather than by aiming at the line
  between them.

As in 7.99, the integration table comes with no recipes of its own: other mods add them (`IntegrationRecipes`).

Settings are in `config/buildcraft.properties`. The game writes it each time it starts, keeping the values set in it and
adding any missing settings with their defaults.

### Building

```
./gradlew build
```

The jars end up in `fabric/build/libs`, `forge/build/libs` and `neoforge/build/libs`. `./gradlew :<loader>:runClient`
and `./gradlew :<loader>:runServer` start the game with the mod.

### Layout

* `common/` holds nearly all the code and every asset. It only uses Minecraft's own classes (Mojang's names); each
  loader project compiles it into its own jar.
* `fabric/`, `forge/` and `neoforge/` hold the small part that differs between loaders: registration, item, fluid and
  energy transfer, world generation, and client setup. They implement `buildcraft.lib.platform.Platform`.
* `tools/gen_resources.py` generates the block states, models, item models, recipes, loot tables, tags and English
  names in `common/src/main/resources`. Edit it rather than the generated files, and run it after changing it.
* `tools/gen_guide.py` generates the guide book's pages (most converted from 7.99's guide), contents and recipes. Run it
  after `gen_resources.py`.
* `.github/smoke-test` is a datapack that CI uses to check, on every loader, that a dedicated server starts and that a
  few machines work.

### Licence

BuildCraft's code is under the Mozilla Public License 2.0 (`LICENSE-NEW`); older parts of the original code and assets
are under the Minecraft Mod Public License (`LICENSE`), as noted in the files themselves.
