## BuildCraft Renewed

BuildCraft for Minecraft 26.3, running on **Fabric**, **Forge** and **NeoForge** from one codebase. It is a port of
BuildCraft 7.99 (the Minecraft 1.12.2 version, "BuildCraft 8"), whose code is kept for reference in `legacy/`.

| Loader   | Version                                   |
|----------|-------------------------------------------|
| Fabric   | Loader 0.19.5, Fabric API 0.161.0+26.3    |
| Forge    | 66.0.9                                    |
| NeoForge | 26.3.0.48-beta                            |

Java 25 is needed to build and run it.

### What's in it

* **Core:** wrench, gears, paintbrush, list, volume markers, engines (redstone and creative), water and oil springs,
  power tester and decorated blocks.
* **Energy:** Stirling and combustion engines, oil and fuels, and oil wells, spouts and lakes in world generation.
* **Transport:** item, fluid and power pipes of every kind, pipe wires, painting, and pluggables (pipe plugs, power
  adapters, gates, pulsars, sensors, lenses, filters and facades).
* **Factory:** mining well, pump, flood gate, tank, chute, distiller, heat exchanger, auto workbench and water gel.
* **Silicon:** laser, assembly table, advanced crafting table, chipsets, and gates with their triggers and actions.
* **Builders:** quarry, filler (all 19 patterns, which gates can set), architect table, builder, electronic library,
  templates and blueprints. Quarries, fillers and builders keep the chunks they work in loaded.

Not ported (yet): the replacer, path markers, map locations, goggles, the zone planner and the guide book.

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
* `.github/smoke-test` is a datapack that CI uses to check, on every loader, that a dedicated server starts and that a
  few machines work.

### Licence

BuildCraft's code is under the Mozilla Public License 2.0 (`LICENSE-NEW`); older parts of the original code and assets
are under the Minecraft Mod Public License (`LICENSE`), as noted in the files themselves.
