package team.chisel.ctm;

import static team.chisel.ctm.CTM.MOD_ID;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.model.parsing.ModelLoaderCTM;
import team.chisel.ctm.client.newctm.json.CTMDefinitionManager;
import team.chisel.ctm.client.texture.type.TextureTypeRegistry;
import team.chisel.ctm.client.util.CTMPackReloadListener;
import team.chisel.ctm.client.util.TextureMetadataHandler;

@Mod(value = MOD_ID, dist = Dist.CLIENT)
public class CTM {
    
    public static final String MOD_ID = "ctm";
    public static final String MOD_NAME = "CTM";
    public static final String DOMAIN = MOD_ID;

    public static final Logger logger = LogManager.getLogger("CTM");
    
    public static CTM instance;
    @Getter
    private final CTMDefinitionManager definitionManager;
    @Getter
    private final CTMPackReloadListener reloadListener;

    public CTM(ModContainer modContainer, IEventBus modBus) {
    	instance = this;
        modBus.addListener(this::modelRegistry);
        modBus.addListener(this::imc);
        modBus.register(TextureMetadataHandler.INSTANCE);
        Configurations.register(modContainer, modBus);

        TextureTypeRegistry.scan();

        definitionManager = new CTMDefinitionManager();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            //TODO
            // Note: This can be null in datagen. We just skip adding the definition manager, as resources don't actually need
            // to be loaded in datagen. Once: https://github.com/neoforged/NeoForge/pull/1289 is merged, we should move to using
            // that instead
            ReloadableResourceManager resourceManager = (ReloadableResourceManager) minecraft.getResourceManager();
            resourceManager.registerReloadListener(definitionManager.getReloadListener());
        }
        reloadListener = new CTMPackReloadListener();
        modBus.addListener(this::reloadListenersLate);
    }

    private void modelRegistry(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "ctm"), ModelLoaderCTM.INSTANCE);
    }

    private void imc(InterModEnqueueEvent event) {
        InterModComms.sendTo(MOD_ID, "framedblocks", "add_ct_property", () -> AbstractCTMBakedModel.CTM_CONTEXT);
    }
    
    private void reloadListenersLate(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(reloadListener);
    }
}
