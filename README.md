# L Client (Fabric, Minecraft 1.21.11)

Basehunting client with **Meteor Client**, **DupersUnited** and **UI Utils** built in (jar-in-jar).
One jar in `.minecraft/mods`, plus Fabric Loader >= 0.18.2 and Fabric API 0.141.6+1.21.11.

## Build
1. `gradle.properties` already targets 1.21.11. `yarn_mappings` uses `1.21.11+build.+` (newest build);
   if Gradle complains, pin it to the exact value shown on https://fabricmc.net/develop.
2. Wrapper files are not included (they are binary). Either open the folder in IntelliJ (it imports Gradle itself),
   or run `gradle wrapper --gradle-version 9.8.2` once, then `./gradlew build`.
3. Output: `build/libs/L Client-1.0.0+1.21.11.jar`.

## Use
- **Right Ctrl** opens the L Client GUI (rebind in Controls > L Client). Meteor still uses Right Shift, DupersUnited has its own key.
- Sidebar tabs: Basehunt / Movement / Player / Visual, then **BUNDLED MODS**: Meteor, DupersUnited, UI Utils.
  - Left click toggles a module, right click (or `+`) opens its settings and keybind. Type anywhere to search all modules.
  - Meteor and Dupers modules in the list are the real ones; toggling them toggles the real module.
  - "Open >" rows launch the other mods' own screens (Meteor Modules/Config/GUI/HUD/Friends/Macros/Profiles, Dupers ClickGui/Menu, UI Utils settings).
- Config: `.minecraft/config/L Client.json`. Finds log: `.minecraft/L Client/finds.txt`.

## Adding a module
Create a class extending `Module` in `module/impl`, add settings with `bool()` / `num()`, override `onTick()` / `onRender3D()`,
register it in `ModuleManager.init()`.

## Licences (matters if you share the jar)
- Meteor Client and DupersUnited are GPL-3.0: a jar that contains them must be distributed under GPL-3.0 with source available.
- UI Utils is "All Rights Reserved": fine for your own use, but you may not redistribute it inside your jar.
