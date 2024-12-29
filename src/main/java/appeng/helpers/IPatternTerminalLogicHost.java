package appeng.helpers;

import appeng.parts.encoding.PatternEncodingLogic;
import net.minecraft.world.level.Level;

public interface IPatternTerminalLogicHost {
    PatternEncodingLogic getLogic();

    Level getLevel();

    void markForSave();
}
