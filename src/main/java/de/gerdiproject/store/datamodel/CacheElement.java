package de.gerdiproject.store.datamodel;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.Instant;

public @Data class CacheElement<E extends Credentials> {

    private final Instant timespamp = Instant.now();
    private final StoreTask task;
    private E credentials;
    @Setter(AccessLevel.PRIVATE) private Progress progress;

    public CacheElement(StoreTask task) {
        this.task = task;
        this.progress = new Progress(this.task.getDocs());
    }
}
