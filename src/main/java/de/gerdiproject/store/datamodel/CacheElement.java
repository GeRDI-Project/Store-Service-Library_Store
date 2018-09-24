/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.datamodel;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

public @Data
class CacheElement<E extends ICredentials> {

    private final Instant timespamp = Instant.now();
    private final StoreTask task;
    private final Map<String, CacheElement<E>> cacheMap;
    private final String sessionId;
    @Setter(AccessLevel.PRIVATE)
    private boolean finished = false;
    private E credentials;
    @Setter(AccessLevel.PRIVATE)
    private Progress<E> progress;

    public CacheElement(final String sessionId, final StoreTask task, final Map<String, CacheElement<E>> cacheMap) {
        this.sessionId = sessionId;
        this.cacheMap = cacheMap;
        this.task = task;
        this.progress = new Progress(this.task.getDocs(), this);
    }

    /**
     * Will be called if all files are completely copied
     */
    void notifyAllFinished() { // NOPMD
        this.setFinished(true);
    }
}
