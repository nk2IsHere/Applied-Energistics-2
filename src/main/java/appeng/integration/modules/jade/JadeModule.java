package appeng.integration.modules.jade;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.providers.*;
import appeng.integration.modules.igtooltip.TooltipProviders;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public class JadeModule implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        TooltipProviders.loadCommon(new CommonRegistration() {
            @Override
            public <T extends BlockEntity> void addBlockEntityData(ResourceLocation id,
                    Class<T> blockEntityClass,
                    ServerDataProvider<? super T> provider) {
                var adapter = new ServerDataProviderAdapter<>(id, provider, blockEntityClass);
                registration.registerBlockDataProvider(adapter, blockEntityClass);
            }
        });
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        TooltipProviders.loadClient(new ClientRegistration() {
            @Override
            public <T extends BlockEntity> void addBlockEntityBody(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, BodyProvider<? super T> provider,
                    int priority) {
                var adapter = new BodyProviderAdapter<>(id, priority, provider, blockEntityClass);
                registration.registerBlockComponent(adapter, blockClass);
            }

            @Override
            public <T extends BlockEntity> void addBlockEntityIcon(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, IconProvider<? super T> provider,
                    int priority) {
                var adapter = new IconProviderAdapter<>(id, priority, IElementHelper.get(), provider,
                        blockEntityClass);
                registration.registerBlockIcon(adapter, blockClass);
            }

            @Override
            public <T extends BlockEntity> void addBlockEntityName(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, NameProvider<? super T> provider,
                    int priority) {
                var adapter = new NameProviderAdapter<>(id, priority, provider, blockEntityClass);
                registration.registerBlockComponent(adapter, blockClass);
            }

            @Override
            public <T extends BlockEntity> void addBlockEntityModName(Class<T> blockEntityClass,
                    Class<? extends Block> blockClass, ResourceLocation id, ModNameProvider<? super T> provider,
                    int priority) {
                var adapter = new ModNameProviderAdapter<>(id, provider, blockEntityClass);
                registration.registerBlockComponent(adapter, blockClass);
            }
        });
    }

}
