package net.flytre.pipe.impl.item;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.flytre.flytre_lib.api.storage.connectable.ItemPipeConnectable;
import net.flytre.flytre_lib.api.storage.inventory.filter.ResourceFilter;
import net.flytre.pipe.api.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.ToLongFunction;

@SuppressWarnings("UnstableApiUsage")
public class FabricItemPipeLogic implements PipeLogic<ItemStack> {

    public static final ClientPathValidityChecker<ItemStack> FABRIC_VALIDITY_CHECKER = (world, current, next) -> {
        Direction dir = Direction.fromVector(next.subtract(current));
        if (dir == null)
            return false;
        return ItemStorage.SIDED.find(world, next, dir) != null;
    };
    private static final StorageFinder<ItemStack> FABRIC_STORAGE_FINDER = (world, pos, dir) -> {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, dir);
        return storage != null && storage.supportsInsertion();
    };
    private static final InsertionChecker<ItemStack> FABRIC_INSERTION_CHECKER = new InsertionChecker<>() {
        @Override
        public long getInsertionAmount(World world, ItemStack stack, BlockPos pos, Direction direction, boolean isStuck, long flowAmount) {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, direction);
            if (storage == null || !storage.supportsInsertion())
                return 0;

            try (Transaction transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
                long amountInserted = storage.insert(ItemVariant.of(stack), flowAmount + stack.getCount(), transaction);
                transaction.close();
                return amountInserted;
            }
        }
    };
    private static final StorageInserter<ItemStack> FABRIC_STORAGE_INSERTER = new StorageInserter<>() {
        @Override
        public boolean insert(World world, BlockPos pos, Direction direction, ItemStack stack) {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, direction);
            if (storage == null || !storage.supportsInsertion())
                return false;

            try (Transaction transaction = Transaction.openOuter()) {
                long amountInserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                if (amountInserted >= 1) {
                    transaction.commit();
                    stack.decrement((int) amountInserted);
                    return stack.isEmpty();
                }
            }
            return false;
        }
    };
    private static final StorageExtractor<ItemStack> FABRIC_STORAGE_EXTRACTOR = new StorageExtractor<>() {
        @Override
        public boolean extract(World world, BlockPos pipePosition, Direction direction, ResourceFilter<? super ItemStack> filter, ToLongFunction<ItemStack> pipeHandler, long maxExtractionAmount) {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pipePosition.offset(direction), direction.getOpposite());
            if (storage == null || !storage.supportsExtraction())
                return false;
            for (StorageView<ItemVariant> view : storage) {
                if (view.isResourceBlank())
                    continue;
                ItemVariant resource = view.getResource();
                if ((!filter.isEmpty() && !filter.passFilterTest(resource.toStack())))
                    continue;

                int extracted;
                try (Transaction transaction = Transaction.openOuter()) {
                    long amountExtracted = view.extract(resource, maxExtractionAmount, transaction);

                    if (amountExtracted == 0)
                        continue;

                    extracted = (int) pipeHandler.applyAsLong(resource.toStack((int) amountExtracted));
                }
                if (extracted > 0)
                    try (Transaction transaction = Transaction.openOuter()) {
                        view.extract(resource, extracted, transaction);
                        transaction.commit();
                        return true;
                    }
            }
            return false;
        }
    };
    private static final PipeConnectable<ItemStack> FABRIC_PIPE_CONNECTABLE = (world, pos, direction, block, entity) -> block instanceof ItemPipeConnectable || ItemStorage.SIDED.find(world, pos, direction) != null;
    public static FabricItemPipeLogic INSTANCE = new FabricItemPipeLogic();

    private FabricItemPipeLogic() {

    }

    @Override
    public InsertionChecker<ItemStack> getInsertionChecker() {
        return FABRIC_INSERTION_CHECKER;
    }

    @Override
    public PipeConnectable<ItemStack> getPipeConnectable() {
        return FABRIC_PIPE_CONNECTABLE;
    }

    @Override
    public StorageExtractor<ItemStack> getStorageExtractor() {
        return FABRIC_STORAGE_EXTRACTOR;
    }

    @Override
    public StorageFinder<ItemStack> getStorgeFinder() {
        return FABRIC_STORAGE_FINDER;
    }

    @Override
    public StorageInserter<ItemStack> getStorageInserter() {
        return FABRIC_STORAGE_INSERTER;
    }
}
