package appeng.core.network;

import appeng.core.AppEng;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface CustomAppEngPayload extends CustomPacketPayload {
    static <T extends CustomPacketPayload> Type<T> createType(String name) {
        return new Type<>(AppEng.makeId(name));
    }
}
