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

import com.google.gson.*;
import de.gerdiproject.store.datamodel.ResearchDataInputStream;
import de.gerdiproject.store.datamodel.StoreTask;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;

/**
 * This class represents a deserializer for incoming requests which are represented by {@linkplain StoreTask}
 *
 * @author Nelson Tavares de Sousa
 */
public class StoreTaskDeserializer implements JsonDeserializer<StoreTask> {

    @Override
    public StoreTask deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final StoreTask instance = new StoreTask();
        JsonElement userId = jsonObject.get("userId");
        if (userId == null) {
            throw new JsonParseException("userId must not be null.");
        }
        instance.setUserId(userId.getAsString());

        JsonElement bookmarkId = jsonObject.get("bookmarkId");
        if (bookmarkId == null) {
            throw new JsonParseException("bookmarkId must not be null.");
        }
        instance.setBookmarkId(bookmarkId.getAsString());

        JsonElement bookmarkName = jsonObject.get("bookmarkName");
        if (bookmarkName == null) {
            throw new JsonParseException("bookmarkName must not be null.");
        }
        instance.setBookmarkName(bookmarkName.getAsString());

        final JsonArray docs = jsonObject.get("docs").getAsJsonArray();
        for (JsonElement elem : docs) {
            try {
                final ResearchDataInputStream in = new ResearchDataInputStream(new URL(elem.getAsString()), instance);
                instance.addResearchDataInputStream(in);
            } catch (IOException e) {
                throw new JsonParseException("At least one element in docs is not a valid URL");
            }
        }

        return instance;
    }
}
