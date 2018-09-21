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

import java.util.*;

public class Progress implements Iterable<TaskElement> {

    private final Map<String, TaskElement> map = new HashMap<>();

    Progress(List<String> docs) {
        for (String it : docs) {
            this.map.put(it, new TaskElement(it));
        }
    }

    @Override
    public Iterator<TaskElement> iterator() {
        return map.values().iterator();
    }

    public Collection<TaskElement> values() {
        return this.map.values();
    }
}
