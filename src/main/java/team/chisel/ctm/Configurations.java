package team.chisel.ctm;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

public class Configurations {

    public static final Configurations INSTANCE = new Configurations();

    public static void register(ModContainer modContainer, IEventBus modBus) {
        modContainer.addConfig(new ModConfig(Type.CLIENT, INSTANCE.configSpec, modContainer, "ctm.toml"));
        // modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(Configurations::reloadEvent);
    }

    private static void reloadEvent(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(CTM.MOD_ID)) {
            //Only reload when our config changes
            AbstractCTMBakedModel.invalidateCaches();
            //Queue the change to occur on the render thread (as otherwise an exception will be thrown)
            RenderSystem.recordRenderCall(() -> Minecraft.getInstance().levelRenderer.allChanged());
        }
    }

    private final ForgeConfigSpec configSpec;

    public final ForgeConfigSpec.BooleanValue disableCTM;
    public final ForgeConfigSpec.BooleanValue connectInsideCTM;

    private Configurations() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        disableCTM = builder.translation("configuration.ctm.disable")
              .comment("Disable connected textures entirely")
              .define("disableCTM", false);
        connectInsideCTM = builder.translation("configuration.ctm.connect_inside")
              .comment("Choose whether the inside corner is disconnected on a CTM block - https://imgur.com/eUywLZ4")
              .define("connectInsideCTM", false);
        configSpec = builder.build();
    }

    public static boolean isDisabled() {
        return INSTANCE.disableCTM.get();
    }

    public static boolean connectInsideCTM() {
        if (INSTANCE.configSpec.isLoaded()) {
            return INSTANCE.disableCTM.get();
        }
        return INSTANCE.disableCTM.getDefault();
    }
}
