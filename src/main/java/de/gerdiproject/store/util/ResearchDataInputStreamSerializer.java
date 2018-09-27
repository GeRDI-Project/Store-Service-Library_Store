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
package de.gerdiproject.store.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.gerdiproject.store.datamodel.ResearchDataInputStream;

import java.lang.reflect.Type;

/**
 * This class represents a serializer for the {@linkplain ResearchDataInputStream}.
 *
 * @author Nelson Tavares de Sousa
 */
public class ResearchDataInputStreamSerializer implements JsonSerializer<ResearchDataInputStream> {

    @Override
    public JsonElement serialize(final ResearchDataInputStream src, final Type typeOfSrc, final JsonSerializationContext context) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("fileName", src.getName());
        obj.addProperty("progressInPercent", src.getProgressInPercent());
        obj.addProperty("state", src.getStatus().toString());
        return obj;
    }
}
