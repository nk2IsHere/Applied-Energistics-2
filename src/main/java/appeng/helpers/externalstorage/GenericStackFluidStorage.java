package appeng.helpers.externalstorage;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEKeyType;
import appeng.util.IVariantConversion;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

/**
 * Exposes a {@link GenericInternalInventory} as the platforms external fluid storage interface.
 */
public class GenericStackFluidStorage extends GenericStackInvStorage<FluidVariant> {
    public GenericStackFluidStorage(GenericInternalInventory inv) {
        super(IVariantConversion.FLUID, AEKeyType.fluids(), inv);
    }
}
