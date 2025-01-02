package appeng.integration.modules.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AttunementDisplay extends BasicDisplay {

    public AttunementDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Component... description) {
        super(inputs, outputs);

        for (var input : inputs) {
            for (var entry : input) {
                entry.tooltip(description);
            }
        }
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return AttunementCategory.ID;
    }
}
