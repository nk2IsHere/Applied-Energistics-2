/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.menu.implementations;

import java.util.function.Function;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import appeng.core.AppEng;
import appeng.init.InitMenuTypes;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.locator.MenuLocators;

/**
 * Builder that allows creation of menu types which can be opened from multiple types of hosts.
 */
public final class MenuTypeBuilder<M extends AEBaseMenu, I> {

    @Nullable
    private ResourceLocation id;

    private final Class<I> hostInterface;

    private final MenuFactory<M, I> factory;

    private Function<I, Component> menuTitleStrategy = this::getDefaultMenuTitle;

    @Nullable
    private InitialDataSerializer<I> initialDataSerializer;

    @Nullable
    private InitialDataDeserializer<M, I> initialDataDeserializer;

    private MenuType<M> menuType;

    private MenuTypeBuilder(Class<I> hostInterface, TypedMenuFactory<M, I> typedFactory) {
        this.hostInterface = hostInterface;
        this.factory = (containerId, playerInv, accessObj) -> typedFactory.create(menuType, containerId, playerInv,
                accessObj);
    }

    private MenuTypeBuilder(Class<I> hostInterface, MenuFactory<M, I> factory) {
        this.hostInterface = hostInterface;
        this.factory = factory;
    }

    public static <C extends AEBaseMenu, I> MenuTypeBuilder<C, I> create(MenuFactory<C, I> factory,
            Class<I> hostInterface) {
        return new MenuTypeBuilder<>(hostInterface, factory);
    }

    public static <C extends AEBaseMenu, I> MenuTypeBuilder<C, I> create(TypedMenuFactory<C, I> factory,
            Class<I> hostInterface) {
        return new MenuTypeBuilder<>(hostInterface, factory);
    }

    /**
     * Specifies a custom strategy for obtaining a custom menu name.
     * <p>
     * The strategy should return {@link Component#empty()} if there's no custom name.
     */
    public MenuTypeBuilder<M, I> withMenuTitle(Function<I, Component> menuTitleStrategy) {
        this.menuTitleStrategy = menuTitleStrategy;
        return this;
    }

    /**
     * Sets a serializer and deserializer for additional data that should be transmitted from server->client when the
     * menu is being first opened.
     */
    public MenuTypeBuilder<M, I> withInitialData(InitialDataSerializer<I> initialDataSerializer,
            InitialDataDeserializer<M, I> initialDataDeserializer) {
        this.initialDataSerializer = initialDataSerializer;
        this.initialDataDeserializer = initialDataDeserializer;
        return this;
    }

    /**
     * Opens a menu that is based around a single block entity. The block entity's position is encoded in the packet
     * buffer.
     */
    private M fromNetwork(int containerId, Inventory inv, ByteBuf data) {
        var registryBuf = new RegistryFriendlyByteBuf(data, inv.player.registryAccess());
        var locator = MenuLocators.readFromPacket(registryBuf);
        I host = locator.locate(inv.player, hostInterface);
        if (host == null) {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new ServerboundContainerClosePacket(containerId));
            }
            throw new IllegalStateException("Couldn't find menu host at " + locator + " for " + this.id
                    + " on client. Closing menu.");
        }
        M menu = factory.create(containerId, inv, host);
        menu.setReturnedFromSubScreen(data.readBoolean());
        if (initialDataDeserializer != null) {
            initialDataDeserializer.deserializeInitialData(host, menu, registryBuf);
        }
        return menu;
    }

    private boolean open(Player player, MenuHostLocator locator, boolean fromSubMenu) {
        if (!(player instanceof ServerPlayer)) {
            // Cannot open menus on the client or for non-players
            // FIXME logging?
            return false;
        }

        var accessInterface = locator.locate(player, hostInterface);

        if (accessInterface == null) {
            return false;
        }

        Component title = menuTitleStrategy.apply(accessInterface);
        player.openMenu(new HandlerFactory(locator, title, accessInterface, initialDataSerializer, fromSubMenu));

        return true;
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, ByteBuf> PACKET_IDENTITY_CODEC = StreamCodec
            .of(
                    (encoded, decoded) -> encoded.writeBytes(decoded),
                    (encoded) -> {
                        var decoded = new FriendlyByteBuf(Unpooled.buffer());
                        decoded.writeBytes(encoded);
                        return decoded;
                    });

    private class HandlerFactory implements ExtendedScreenHandlerFactory {

        private final MenuHostLocator locator;

        private final I accessInterface;

        private final Component title;

        private final InitialDataSerializer<I> initialDataSerializer;

        private final boolean fromSubMenu;

        public HandlerFactory(MenuHostLocator locator, Component title, I accessInterface,
                InitialDataSerializer<I> initialDataSerializer, boolean fromSubMenu) {
            this.locator = locator;
            this.title = title;
            this.accessInterface = accessInterface;
            this.initialDataSerializer = initialDataSerializer;
            this.fromSubMenu = fromSubMenu;
        }

        @Override
        public Component getDisplayName() {
            return title;
        }

        @Override
        public boolean shouldCloseCurrentScreen() {
            return false; // Stops the cursor from re-centering
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int wnd, Inventory inv, Player p) {
            M m = factory.create(wnd, inv, accessInterface);
            // Set the original locator on the opened server-side menu for it to more
            // easily remember how to re-open after being closed.
            m.setLocator(locator);
            return m;
        }

        @Override
        public Object getScreenOpeningData(ServerPlayer player) {
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            MenuLocators.writeToPacket(buf, locator);
            buf.writeBoolean(fromSubMenu);
            if (initialDataSerializer != null) {
                initialDataSerializer.serializeInitialData(accessInterface, buf);
            }

            return buf;
        }
    }

    public MenuType<M> build(String id) {
        return build(AppEng.makeId(id));
    }

    /**
     * Creates a menu type that uses this helper as a factory and network deserializer.
     */
    public MenuType<M> buildUnregistered(ResourceLocation id) {
        Preconditions.checkState(menuType == null, "build was already called");
        Preconditions.checkState(this.id == null, "id should not be set");

        this.id = id;
        menuType = new ExtendedScreenHandlerType<>(this::fromNetwork, PACKET_IDENTITY_CODEC);
        InitMenuTypes.queueRegistration(this.id, menuType);
        MenuOpener.addOpener(menuType, this::open);
        return menuType;
    }

    /**
     * Creates a menu type that uses this helper as a factory and network deserializer, and queues it for registration
     * with Vanilla.
     */
    public MenuType<M> build(ResourceLocation id) {
        var menuType = buildUnregistered(id);
        InitMenuTypes.queueRegistration(this.id, menuType);
        return menuType;
    }

    @FunctionalInterface
    public interface MenuFactory<C, I> {
        C create(int containerId, Inventory playerInv, I menuHost);
    }

    @FunctionalInterface
    public interface TypedMenuFactory<C extends AbstractContainerMenu, I> {
        C create(MenuType<C> type, int containerId, Inventory playerInv, I accessObj);
    }

    /**
     * Strategy used to serialize initial data for opening the menu on the client-side into the packet that is sent to
     * the client.
     */
    @FunctionalInterface
    public interface InitialDataSerializer<I> {
        void serializeInitialData(I host, RegistryFriendlyByteBuf buffer);
    }

    /**
     * Strategy used to deserialize initial data for opening the menu on the client-side from the packet received by the
     * server.
     */
    @FunctionalInterface
    public interface InitialDataDeserializer<C, I> {
        void deserializeInitialData(I host, C menu, RegistryFriendlyByteBuf buffer);
    }

    private Component getDefaultMenuTitle(I accessInterface) {
        if (accessInterface instanceof Nameable nameable) {
            if (nameable.hasCustomName()) {
                return nameable.getCustomName();
            }
        }

        return Component.empty();
    }

}
