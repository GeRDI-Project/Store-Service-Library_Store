package de.gerdiproject.store.util;

import de.gerdiproject.store.datamodel.CacheElement;
import de.gerdiproject.store.datamodel.ICredentials;

import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.TimerTask;

public class CacheGarbageCollectionTask<E extends ICredentials> extends TimerTask {

    private final Map<String, CacheElement<E>> map;

    public CacheGarbageCollectionTask(Map<String, CacheElement<E>> map) {
        this.map = map;
    }

    @Override
    public void run() {
        final Instant halfAnHourAgo = Instant.now().minusSeconds(1800);
        for (Map.Entry<String, CacheElement<E>> entry : map.entrySet()) {
            if (entry.getValue().getTimespamp().isBefore(halfAnHourAgo)) {
                this.map.remove(entry.getKey());
            }
        }
    }

}
