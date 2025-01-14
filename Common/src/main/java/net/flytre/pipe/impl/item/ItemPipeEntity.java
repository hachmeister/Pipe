package net.flytre.pipe.impl.item;

import net.flytre.flytre_lib.api.storage.inventory.filter.FilterInventory;
import net.flytre.flytre_lib.api.storage.inventory.filter.FilterSettings;
import net.flytre.flytre_lib.api.storage.inventory.filter.HasFilterSettings;
import net.flytre.flytre_lib.api.storage.inventory.filter.packet.FilterEventHandler;
import net.flytre.flytre_lib.loader.CustomScreenHandlerFactory;
import net.flytre.pipe.api.AbstractPipeEntity;
import net.flytre.pipe.api.PipeLogic;
import net.flytre.pipe.api.ResourceHandler;
import net.flytre.pipe.impl.network.ItemPipeHandler;
import net.flytre.pipe.impl.registry.Registry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;


public final class ItemPipeEntity extends AbstractPipeEntity<ItemStack,FilterInventory> implements CustomScreenHandlerFactory, HasFilterSettings, FilterEventHandler {


    private static PipeLogic<ItemStack> ITEM_PIPE_LOGIC = VanillaItemPipeLogic.INSTANCE;

    public ItemPipeEntity(BlockPos pos, BlockState state) {
        super(Registry.ITEM_PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.pipe.item_pipe");
    }

    /**
     * ScreenHandlers represent server side logic for a screen displayed client sde
     */
    @Override
    public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ItemPipeHandler(syncId, inv, this);
    }

    /**
     * Event that's automagically called when a filter update packet is received so the cache can be cleared.
     */
    @Override
    public void onPacketReceived() {
        clearNetworkCache(false);
    }

    @Override
    protected ResourceHandler<ItemStack, FilterInventory> getResourceHandler() {
        return ItemResourceHandler.INSTANCE;
    }

    @Override
    protected PipeLogic<ItemStack> getPipeLogic() {
        return ITEM_PIPE_LOGIC;
    }

    @Override
    protected int getBaseTicksBetweenPull() {
        Block block = getCachedState().getBlock();
        return block == Registry.FAST_PIPE.get() ? 4 : (block == Registry.LIGHTNING_PIPE.get() ? 1 : 10);
    }

    @Override
    protected int getBaseTicksThroughPipe() {
        Block block = getCachedState().getBlock();
        return block == Registry.FAST_PIPE.get() ? 8 : (block == Registry.LIGHTNING_PIPE.get() ? 3 : 20);
    }


    @Override
    protected long getAmountToExtract() {
        return 1L;
    }

    public static void setItemPipeLogic(PipeLogic<ItemStack> itemPipeLogic) {
        ITEM_PIPE_LOGIC = itemPipeLogic;
    }

    @Override
    public FilterSettings getFilterSettings() {
        return getFilter();
    }

    /**
     * Send data to the client about what to display on the pipe's screen.
     */
    @Override
    public void sendPacket(PacketByteBuf packetByteBuf) {
        packetByteBuf.writeBlockPos(pos);
        getResourceHandler().writeFilter(packetByteBuf, getFilter());
        packetByteBuf.writeBoolean(this.isRoundRobinMode());
    }
}
