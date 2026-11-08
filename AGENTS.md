# Agent Guidelines & Project Knowledge Base

## Project Overview
This project is a **Minecraft: Java Edition** mod built with **Minecraft Forge**.
- **Mod ID:** `examplemod`
- **Base Package:** `com.example.examplemod`
- **Mod Entry Point:** `com.example.examplemod.ExampleMod`
- **Configuration:** `com.example.examplemod.Config` (`ForgeConfigSpec`)

## Environment & Version Stack
- **Minecraft: Java Edition:** `26.3` (version range `[26.3,26.4)`)
- **Minecraft Forge:** `26.3-66.1.0` (Loader version `[66,)`)
- **ForgeGradle:** `[7.0.17,8)`
- **EventBus:** `net.minecraftforge:eventbus:7.0.6` & `net.minecraftforge:eventbus-validator:7.0.6`
- **Java Version:** **Java 25** (`JavaLanguageVersion.of(25)`)
- **Local JDK 25 Installation Path:**
  `C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot`
  *(Note: `JAVA_HOME` is not set by default in fresh PowerShell sessions; always set `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"` before running Gradle or invoke binaries from `C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot\bin\`.)*

---

## Strict Version Compatibility & Documentation Policy
1. **Verify Before Using Any Code:** Always verify that any code snippet, class, method, or modding pattern is strictly compatible with **Minecraft 26.3**, **Forge 26.3-66.1.0**, and **Java 25** before applying it. Only use code once compatibility with version `26.3` is confirmed.
2. **Context7 + Local 26.3 Source Inspection:**
   - Use **Context7** (`/websites/minecraftforge_net_en`, `/minecraftforge/minecraftforge`) for general Forge concepts, but note that online docs primarily cover up to `1.21.x`.
   - Because **Minecraft 26.3** and **Forge 66.x** introduce major breaking changes compared to `1.21.x`, **always verify package names, class names, and method signatures against the local 26.3 Mavenizer cache jars** listed below.

---

## Local Package Search & Source Inspection (Minecraft 26.3 / Forge 66.1.0)
When searching for classes, methods, patches, or vanilla/Forge implementations in `26.3`, inspect these local jars in the Gradle cache:

1. **Full Decompiled & Patched Minecraft + Forge 26.3 Sources (`injected-sources.jar`):**
   `C:\Users\totha\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\forge\net\minecraftforge\forge\26.3-66.1.0\injected-sources.jar`
   - Contains both `net/minecraft/...` and `net/minecraftforge/...` `.java` source files with all Forge patches applied.
2. **Compiled Minecraft + Forge 26.3 Bytecode (`recompiled.jar`):**
   `C:\Users\totha\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\forge\net\minecraftforge\forge\26.3-66.1.0\official\26.3\recompiled.jar`
   - Ideal for `javap.exe -cp` inspection and direct `javac.exe` compilation checks.
3. **Forge-Only 26.3 Sources & Patches (`forge-26.3-66.1.0-sources.jar`):**
   `C:\Users\totha\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\maven\forge\net\minecraftforge\forge\26.3-66.1.0\forge-26.3-66.1.0-sources.jar`

### Useful PowerShell Snippets for Package/Source Search
- **List classes matching a pattern in `injected-sources.jar`:**
  ```powershell
  & "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot\bin\jar.exe" tf "C:\Users\totha\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\forge\net\minecraftforge\forge\26.3-66.1.0\injected-sources.jar" | Select-String -Pattern "ClassName"
  ```
- **Read a specific `.java` source file from `injected-sources.jar`:**
  ```powershell
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zip = [System.IO.Compression.ZipFile]::OpenRead("C:\Users\totha\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\forge\net\minecraftforge\forge\26.3-66.1.0\injected-sources.jar")
  $entry = $zip.GetEntry("net/minecraft/server/level/ServerLevel.java")
  $reader = New-Object System.IO.StreamReader($entry.Open())
  Write-Output $reader.ReadToEnd()
  $reader.Close()
  $zip.Dispose()
  ```
- **Fast Sandboxed Compilation Check (without Gradle Daemon):**
  If the Gradle daemon cannot bind loopback sockets inside the sandbox, verify compilation directly with `javac.exe`:
  ```powershell
  $allJars = Get-ChildItem -Path "$env:USERPROFILE\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\maven" -Filter "*.jar" -Recurse | Where-Object { $_.Name -notmatch "sources|javadoc|natives|26\.1" } | Select-Object -ExpandProperty FullName
  $cp = (@("C:\Users\totha\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\forge\net\minecraftforge\forge\26.3-66.1.0\official\26.3\recompiled.jar") + $allJars) -join ";"
  New-Item -ItemType Directory -Force -Path "$env:TEMP\mod_classes" | Out-Null
  & "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot\bin\javac.exe" -encoding UTF-8 --release 25 -cp $cp -d "$env:TEMP\mod_classes" (Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName)
  ```

---

## Key Minecraft 26.3 & Forge 66.1.0 API Conventions
1. **Resource Identifiers:**
   - `ResourceLocation` has been replaced by `net.minecraft.resources.Identifier` (e.g., `Identifier.tryParse(...)`, `Identifier.parse(...)`, `Identifier.withDefaultNamespace(...)`).
2. **Explicit Registry IDs on Block & Item Properties:**
   - Both `BlockBehaviour.Properties` and `Item.Properties` require `.setId(...)` before constructing a `Block` or `Item`:
     - `BlockBehaviour.Properties.of().setId(BLOCKS.key("name"))`
     - `new Item.Properties().setId(ITEMS.key("name"))`
3. **EventBus 7 Architecture (`net.minecraftforge.eventbus:7.0.6`):**
   - Legacy `MinecraftForge.EVENT_BUS` and `context.getModEventBus()` no longer exist.
   - Obtain the mod bus group via `var modBusGroup = context.getModBusGroup();` from `FMLJavaModLoadingContext`.
   - Register `DeferredRegister` instances to `modBusGroup`: `BLOCKS.register(modBusGroup);`.
   - Mod lifecycle events use `EventClass.getBus(modBusGroup).addListener(...)` (e.g., `FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup)`).
   - Forge/game events expose a static `BUS` field directly on the event class or record subtype:
     - `BuildCreativeModeTabContentsEvent.BUS.addListener(...)`
     - `ServerAboutToStartEvent.BUS.addListener(...)`
     - `LevelEvent.Load.BUS.addListener(...)`
     - `TickEvent.ServerTickEvent.Pre.BUS.addListener(...)` / `Post.BUS.addListener(...)`
     - `TickEvent.LevelTickEvent.Pre.BUS.addListener(...)` / `Post.BUS.addListener(...)`
   - `TickEvent` is a `sealed interface` with `record` subtypes implementing `RecordEvent`. Use record accessors (`event.server()`, `event.level()`, `event.side()`), whereas `LevelEvent` subtypes use `event.getLevel()`.
   - `@SubscribeEvent` is located at `net.minecraftforge.eventbus.api.listener.SubscribeEvent`.
4. **Weather System (`WeatherData` & `MinecraftServer`):**
   - Weather state is stored in `net.minecraft.world.level.saveddata.WeatherData` (retrieved via `server.getWeatherData()` or `serverLevel.getWeatherData()`), not in `ServerLevelData`.
   - `setWeatherParameters(int clearTime, int rainTime, boolean raining, boolean thundering)` is a method on `MinecraftServer` (`server.setWeatherParameters(...)`), not `ServerLevel`.
   - `Level#setRainLevel(float)` and `Level#setThunderLevel(float)` control immediate precipitation intensity on a `Level`.
5. **Biomes, `BiomeModifier`, and Client KnownPack Sync:**
   - Custom `BiomeModifier` codecs are registered to `ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS`, with data files placed at `src/main/resources/data/<modid>/forge/biome_modifier/<name>.json`.
   - Server-side `BiomeModifier`s are applied in `ServerLifecycleHooks.runModifiers(server)` before levels load.
   - **Client KnownPack Caveat:** In Minecraft 26.3, `RegistrySynchronization` skips sending full biome payloads for vanilla `KnownPack` entries (`canSkipContents`), so `ClientLevel` loads vanilla biomes from the local client pack without server `BiomeModifier`s applied. When a `BiomeModifier` changes client-visible biome data (such as `ClimateSettings` temperature/precipitation used by `WeatherEffectRenderer`), also apply the modifier in `LevelEvent.Load` to any biome in `level.registryAccess().lookup(Registries.BIOME)` where `biome.modifiableBiomeInfo().getModifiedBiomeInfo() == null`.
6. **Java 25 Language Features:**
   - Use modern Java 25 features where appropriate (unnamed variables `_`, pattern matching for `instanceof`, records, sealed interfaces, switch expressions).

---

## Project Structure & Common Gradle Commands
- `build.gradle` — Build configuration, ForgeGradle `7.0.x` setup, Java 25 toolchain, and run configurations.
- `src/main/java/com/example/examplemod/` — Primary mod Java source files.
- `src/main/resources/META-INF/mods.toml` — Mod metadata and dependency declarations.
- `src/main/resources/data/examplemod/` — Mod datapack files (such as `forge/biome_modifier/always_snow.json`).
- `src/generated/resources/` — Output directory for generated data/assets.

Always set `JAVA_HOME` before running Gradle wrapper commands:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
.\gradlew.bat compileJava
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
.\gradlew.bat build
```
