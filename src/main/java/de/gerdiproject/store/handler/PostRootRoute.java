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
package de.gerdiproject.store.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import de.gerdiproject.store.datamodel.CacheElement;
import de.gerdiproject.store.datamodel.ICredentials;
import de.gerdiproject.store.datamodel.StoreTask;
import de.gerdiproject.store.util.StoreTaskDeserializer;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Map;
import java.util.UUID;

/**
 * This class represent a handler for a post request on the root URL
 *
 * @param <E> The type used to store the credentials. Must implement the ICredentials interface.
 */
public class PostRootRoute<E extends ICredentials> implements Route {

    private final Map<String, CacheElement<E>> cacheMap;
    private final Gson gson;

    /**
     * Just this class's constructor
     *
     * @param cacheMap The map which is used to cache the store requests
     */
    public PostRootRoute(final Map<String, CacheElement<E>> cacheMap) {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        final JsonDeserializer<StoreTask> jsonDeserializer = new StoreTaskDeserializer();
        gsonBuilder.registerTypeAdapter(StoreTask.class, jsonDeserializer);
        this.gson = gsonBuilder.create();
        this.cacheMap = cacheMap;
    }

    @Override
    public Object handle(final Request request, final Response response) throws Exception {
        final StoreTask input = gson.fromJson(request.body(), StoreTask.class);
        if (input.getElements().isEmpty() || input.getUserId() == null || input.getUserId().isEmpty()) {
            response.status(400);
            return null;
        }
        final String identifier = UUID.randomUUID().toString();
        cacheMap.put(identifier, new CacheElement(input));
        response.status(201);
        return "{ \"sessionId\": \"" + identifier + "\" }";
    }
}
