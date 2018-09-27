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

import lombok.Data;

import java.time.Instant;

/**
 * This class depicts a element which is cached by the library. It stores the task and further relevant data, such as the creation time and the corresponding credentials.
 *
 * @param <E> The type of the class which implements the {@linkplain ICredentials} interface and is used as credentials storage.
 */
public @Data
class CacheElement<E extends ICredentials> {

    private final Instant timespamp = Instant.now();
    private final StoreTask task;
    private E credentials;

    /**
     *  Constructor for this class
     *
     * @param task The task to be cached
     */
    public CacheElement(final StoreTask task) {
        this.task = task;
    }

}
