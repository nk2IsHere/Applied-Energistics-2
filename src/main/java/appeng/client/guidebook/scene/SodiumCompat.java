package appeng.client.guidebook.scene;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class SodiumCompat {
    private static final Logger LOG = LoggerFactory.getLogger(SodiumCompat.class);

    @Nullable
    private static final MethodHandle METHOD_HANDLE;

    static {
        MethodHandle handle = null;
        try {
            handle = MethodHandles.lookup().findStatic(
                    Class.forName("me.jellysquid.mods.sodium.client.render.texture.SpriteUtil"),
                    "markSpriteActive",
                    MethodType.methodType(void.class, TextureAtlasSprite.class));
            LOG.info("Loaded Sodium active sprite compat.");
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            if (FabricLoader.getInstance().isModLoaded("sodium")) {
                LOG.error("Failed to load Sodium active sprite compat.", e);
            }
        }

        METHOD_HANDLE = handle;
    }

    public static void markSpriteActive(TextureAtlasSprite sprite) {
        if (sprite != null && METHOD_HANDLE != null) {
            try {
                METHOD_HANDLE.invokeExact(sprite);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke SpriteUtil#markSpriteActive", e);
            }
        }
    }
}
