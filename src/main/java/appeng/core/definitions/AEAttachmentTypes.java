package appeng.core.definitions;

import appeng.core.AppEng;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class AEAttachmentTypes {

    public static final AttachmentType<Boolean> HOLDING_CTRL = AttachmentRegistry
        .createDefaulted(AppEng.makeId("ctrl"), () -> false);
}
