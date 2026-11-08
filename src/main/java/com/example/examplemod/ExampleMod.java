package com.example.examplemod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public final class ExampleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "examplemod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register for custom BiomeModifier codecs
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);

    public static final RegistryObject<MapCodec<AlwaysSnowBiomeModifier>> ALWAYS_SNOW_BIOME_MODIFIER =
        BIOME_MODIFIER_SERIALIZERS.register("always_snow", () -> AlwaysSnowBiomeModifier.CODEC);

    private static final int PERMANENT_SNOW_DURATION = Integer.MAX_VALUE;
    private static final int MIN_SNOW_DURATION_THRESHOLD = 24000;
    private static final float SNOW_BIOME_TEMPERATURE = -0.5F;

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("example_block"))
            .mapColor(MapColor.STONE)
        )
    );
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
        () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().setId(ITEMS.key("example_block")))
    );

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("example_item"))
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(1)
                .saturationModifier(2f)
                .build()
            )
        )
    );

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((_, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    public ExampleMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modBusGroup);
        // Register the Deferred Register for biome modifier codecs
        BIOME_MODIFIER_SERIALIZERS.register(modBusGroup);

        // Register the item to a creative tab
        BuildCreativeModeTabContentsEvent.BUS.addListener(ExampleMod::addCreative);

        // Ensure snow starts immediately when the world/server starts and continues indefinitely
        ServerAboutToStartEvent.BUS.addListener(ExampleMod::onServerAboutToStart);
        LevelEvent.Load.BUS.addListener(ExampleMod::onLevelLoad);
        TickEvent.ServerTickEvent.Pre.BUS.addListener(ExampleMod::onServerTick);
        TickEvent.LevelTickEvent.Post.BUS.addListener(ExampleMod::onLevelTick);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ensurePermanentPrecipitation(event.getServer());
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        var levelAccessor = event.getLevel();

        // Ensure all biomes in this level's registry (including ClientLevel synced from KnownPacks) are set to snow
        levelAccessor.registryAccess().lookup(Registries.BIOME).ifPresent(biomeRegistry ->
            biomeRegistry.listElements().forEach(biomeHolder -> {
                Biome biome = biomeHolder.value();
                if (biome.modifiableBiomeInfo().getModifiedBiomeInfo() == null) {
                    biome.modifiableBiomeInfo().applyBiomeModifiers(biomeHolder, List.of(AlwaysSnowBiomeModifier.INSTANCE));
                }
            })
        );

        if (levelAccessor instanceof ServerLevel serverLevel && serverLevel.canHaveWeather()) {
            ensurePermanentPrecipitation(serverLevel.getServer());
            serverLevel.setRainLevel(1.0F);
            serverLevel.setThunderLevel(0.0F);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent.Pre event) {
        ensurePermanentPrecipitation(event.server());
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level() instanceof ServerLevel serverLevel && serverLevel.canHaveWeather()) {
            ensurePermanentPrecipitation(serverLevel.getServer());
            if (serverLevel.getRainLevel(1.0F) < 1.0F) {
                serverLevel.setRainLevel(1.0F);
            }
        }
    }

    private static void ensurePermanentPrecipitation(MinecraftServer server) {
        WeatherData weatherData = server.getWeatherData();
        if (!weatherData.isRaining()
                || weatherData.getClearWeatherTime() > 0
                || weatherData.getRainTime() < MIN_SNOW_DURATION_THRESHOLD) {
            server.setWeatherParameters(0, PERMANENT_SNOW_DURATION, true, false);
        }
    }

    public record AlwaysSnowBiomeModifier() implements BiomeModifier {
        public static final AlwaysSnowBiomeModifier INSTANCE = new AlwaysSnowBiomeModifier();
        public static final MapCodec<AlwaysSnowBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase == Phase.MODIFY) {
                builder.climateSettings().setHasPrecipitation(true);
                builder.climateSettings().setTemperature(SNOW_BIOME_TEMPERATURE);
                builder.climateSettings().setTemperatureModifier(Biome.TemperatureModifier.NONE);
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return CODEC;
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
