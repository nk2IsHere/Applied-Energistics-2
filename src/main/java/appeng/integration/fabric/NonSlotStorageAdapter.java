package appeng.integration.fabric;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class NonSlotStorageAdapter<T> implements SingleSlotStorage<T> {

    private final Storage<T> storage;

    public NonSlotStorageAdapter(Storage<T> storage) {
        this.storage = storage;
    }

    @Override
    public long insert(
        T resource,
        long maxAmount,
        TransactionContext transaction
    ) {
        return storage.insert(resource, maxAmount, transaction);
    }

    @Override
    public long extract(
        T resource,
        long maxAmount,
        TransactionContext transaction
    ) {
        return storage.extract(resource, maxAmount, transaction);
    }

    @Override
    public boolean isResourceBlank() {
        return !storage.nonEmptyIterator().hasNext();
    }

    @Override
    public T getResource() {
        for (var storageView : storage) {
            return storageView.getResource();
        }

        return null;
    }

    @Override
    public long getAmount() {
        var iterator = storage.nonEmptyIterator();

        var count = 0L;
        while(iterator.hasNext()) {
            count += iterator.next().getAmount();
        }

        return count;
    }

    @Override
    public long getCapacity() {
        for (var storageView : storage) {
            return storageView.getCapacity();
        }

        return 0;
    }
}
