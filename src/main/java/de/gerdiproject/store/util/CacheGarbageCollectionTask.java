package de.gerdiproject.store.util;

import de.gerdiproject.store.datamodel.CacheElement;
import de.gerdiproject.store.datamodel.ICredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.TimerTask;

public class CacheGarbageCollectionTask<E extends ICredentials> extends TimerTask {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CacheGarbageCollectionTask.class);
    private final Map<String, CacheElement<E>> map;

    public CacheGarbageCollectionTask(Map<String, CacheElement<E>> map) {
        this.map = map;
    }

    @Override
    public void run() {
        LOGGER.debug("CacheGarbageCollector is executed");
        int count = 0;
        final Instant halfAnHourAgo = Instant.now().minusSeconds(1800);
        for (Map.Entry<String, CacheElement<E>> entry : map.entrySet()) {
            if (entry.getValue().getTimespamp().isBefore(halfAnHourAgo)) {
                this.map.remove(entry.getKey());
                count++;
            }
        }
        LOGGER.info("Removed " + count + " old session from the cache.");
    }

}
