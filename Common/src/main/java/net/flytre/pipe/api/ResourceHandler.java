package net.flytre.pipe.api;

import net.flytre.flytre_lib.api.storage.inventory.filter.ResourceFilter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;


/**
 * This tells the pipe logic how to handle a custom resource in conjunction
 * with a custom PipeLogic and renderer
 *
 * @param <C> The type of resource, i.e. ItemStack
 * @param <F> The filter for this resource, i.e. FilterInventory
 */
public interface ResourceHandler<C, F extends ResourceFilter<? super C>> {

    C readNbt(NbtCompound compound);

    NbtCompound writeFilterNbt(NbtCompound compound, C resource);

    C copy(C in);

    //return a copy of the resource with just a single unit
    C copyWithSingleUnit(C in);

    boolean equals(C left, Object right);

    int getHashCode(C in);

    F getDefaultFilter();

    Class<C> getResourceClass();

    NbtCompound writeFilterNbt(F filter);

    F readFilterNbt(NbtCompound compound);

    void writeFilter(PacketByteBuf buf, F filter);

}
