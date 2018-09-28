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

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a store task as requested by a user.
 *
 * @author Nelson Tavares de Sousa
 */
public @Data
class StoreTask {

    private String bookmarkId;
    private String bookmarkName;
    private List<ResearchDataInputStream> elements = new ArrayList<>();
    private String userId;
    private boolean started = false;

    /**
     * Adds a {@linkplain ResearchDataInputStream} to the list of data to be stored.
     *
     * @param inputStream The instance which will be added to the list
     */
    public void addResearchDataInputStream(final ResearchDataInputStream inputStream) {
        this.elements.add(inputStream);
    }
}
