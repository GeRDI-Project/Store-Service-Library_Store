/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    /**
     * This class represents a collector which removes obsolete cache elements from the list
     *
     * @param map The cache map used to store the requests
     */
    public CacheGarbageCollectionTask(Map<String, CacheElement<E>> map) {
        this.map = map;
    }

    @Override
    public void run() {
        LOGGER.debug("CacheGarbageCollector is executed");
        int count = 0;
        final Instant halfAnHourAgo = Instant.now().minusSeconds(1800);
        for (Map.Entry<String, CacheElement<E>> entry : map.entrySet()) {
            if (entry.getValue().getTimespamp().isBefore(halfAnHourAgo) || entry.getValue().isFinished()) {
                this.map.remove(entry.getKey());
                count++;
            }
        }
        LOGGER.info("Removed " + count + " old session from the cache.");
    }

}
