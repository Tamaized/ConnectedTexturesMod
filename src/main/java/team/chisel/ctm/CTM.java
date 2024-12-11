package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.newctm.json.CTMDefinitionManager;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;
import team.chisel.ctm.client.util.CTMPackReloadListener;
import team.chisel.ctm.client.util.TextureMetadataHandler;

@Mod(value = MOD_ID)
public class CTM {
    
    public static final String MOD_ID = "ctm";
    public static final String MOD_NAME = "CTM";
    public static final String DOMAIN = MOD_ID;

    public static final Logger logger = LogManager.getLogger("CTM");
    
    public static CTM instance;
    @Getter
    private CTMDefinitionManager definitionManager;
    @Getter
    private CTMPackReloadListener reloadListener;

    public CTM() {
        instance = this;

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            modBus.addListener(this::modelRegistry);
            modBus.addListener(this::imc);
            modBus.register(TextureMetadataHandler.INSTANCE);

            Configurations.register(ModList.get().getModContainerById(MOD_ID).get(), modBus);

            TextureTypeRegistry.scan();

            definitionManager = new CTMDefinitionManager();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                // Note: This can be null in datagen. We just skip adding the definition manager, as resources don't actually need
                // to be loaded in datagen. Once: https://github.com/neoforged/NeoForge/pull/1289 is merged, we should move to using
                // that instead
                ReloadableResourceManager resourceManager = (ReloadableResourceManager) minecraft.getResourceManager();
                resourceManager.registerReloadListener(definitionManager.getReloadListener());
            }
            reloadListener = new CTMPackReloadListener();
            modBus.addListener(this::reloadListenersLate);
        });

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void modelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register("ctm", ModelLoaderCTM.INSTANCE);
    }

    private void imc(InterModEnqueueEvent event) {
        InterModComms.sendTo(MOD_ID, "framedblocks", "add_ct_property", () -> AbstractCTMBakedModel.CTM_CONTEXT);
    }
    
    private void reloadListenersLate(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(reloadListener);
    }
}
