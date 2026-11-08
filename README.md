# Example Mod (Minecraft 26.3 Forge)

A **Minecraft: Java Edition 26.3** mod built with **Minecraft Forge (`26.3-66.1.0`)** and **Java 25**.

## Features
- **Permanent Snowfall:** Ensures the world starts with active snowfall and continues snowing indefinitely across all biomes.
- **Example Content:** Includes a sample block (`examplemod:example_block`), food item (`examplemod:example_item`), creative mode tab (`examplemod:example_tab`), and configuration (`Config`).

## Requirements
- **Minecraft: Java Edition:** `26.3`
- **Minecraft Forge:** `26.3-66.1.0`
- **Java SDK:** **Java 25** (e.g., Eclipse Adoptium JDK 25)

## Quick Start & Commands

Make sure `JAVA_HOME` points to your Java 25 installation before running Gradle commands in PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
```

### Start the Game Client
```powershell
.\gradlew.bat runClient
```

### Start the Dedicated Server
```powershell
.\gradlew.bat runServer
```

### Compile & Build the Mod JAR
```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
```

